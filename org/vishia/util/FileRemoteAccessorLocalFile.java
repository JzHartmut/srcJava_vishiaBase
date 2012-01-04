package org.vishia.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

/**Implementation for a standard local file.
 */
public class FileRemoteAccessorLocalFile implements FileRemoteAccessor
{
  
  /**Version and history.
   * <ul>
   * <li>2011-12-31 Hartmut new {@link #execCopy(org.vishia.util.FileRemoteAccessor.Commission)}. 
   *   TODO: copy file trees started from a given directory
   * <li>2011-12-31 Hartmut new {@link #runCommissions} as extra thread.  
   * <li>2011-12-10 Hartmut creation: See {@link FileRemoteAccessor}.
   * </ul>
   */
  public static final int version = 0x20111210;

  private static FileRemoteAccessor instance = new FileRemoteAccessorLocalFile();
  
  /**State of execution commissions.
   * '?': not started. 'w': waiting for commission, 'b': busy, 'x': should finish, 'z': finished
   */
  private char commissionState = '?';
  
  
  /**List of all commissions to do. */
  private final ConcurrentLinkedQueue<Commission> commissions = new ConcurrentLinkedQueue<Commission>();
  
  
  /**List of files to handle between {@link #checkCopy(org.vishia.util.FileRemoteAccessor.Commission)}
   * and {@link #execCopy(org.vishia.util.FileRemoteAccessor.Commission)}. */
  private final List<File> listFiles = new LinkedList<File>();
  
  private File currentFile;

  long zBytes, zFiles;
  
  
  /**The thread to run all commissions. */
  private Runnable runCommissions = new Runnable(){
    @Override public void run(){
      runCommissions();  
    }
  };
  
  
  Thread thread = new Thread(runCommissions, "vishia.FileLocal");
  
  { thread.start(); }
  
  public static FileRemoteAccessor getInstance(){
    return instance;
  }
  
  
  @Override public Object createFileObject(FileRemote file)
  { Object oFile = new File(file.path, file.name);
    return oFile;
  }
  
  
  
  @Override public boolean getFileProperties(FileRemote file)
  { return false; //file.super.lastModified();
  }

  
  @Override public ReadableByteChannel openRead(FileRemote file, long passPhase)
  { try{ 
      FileInputStream stream = new FileInputStream(file);
      return stream.getChannel();
    } catch(FileNotFoundException exc){
      return null;
    }
  }

  
  @Override public WritableByteChannel openWrite(FileRemote file, long passPhase)
  { try{ 
    FileOutputStream stream = new FileOutputStream(file);
    return stream.getChannel();
    } catch(FileNotFoundException exc){
      return null;
    }
  }

  
  @Override public boolean isLocalFileSystem()
  {  return true;
  }
  
  
  @Override public void addCommission(Commission com){ 
    commissions.add(com);
    synchronized(this){
      if(commissionState == 'w'){
        notify();
      } else {
        commissionState = 'c';
      }
    }
  }
  
  
  
  void runCommissions(){
    commissionState = 'r';
    while(commissionState != 'x'){
      Commission commission;
      if( (commission = commissions.poll()) !=null){
        commissionState = 'b';
        execCommission(commission);    
      } else {
        synchronized(this){
          if(commissionState != 'c'){
            commissionState = 'w';
            try{ wait(1000); } catch(InterruptedException exc){}
            commissionState = 'r';
          }
        }
      }
    }
  }
  
  
  void execCommission(Commission commission){
    switch(commission.cmd){
    case Commission.kCheckFile: checkCopy(commission); break;
    case Commission.kCopy: execCopy(commission); break;
    case Commission.kDel:  execDel(commission); break;
    
    }
  }
  
  
  
  void checkCopy(Commission co){
    this.currentFile= co.src;
    listFiles.clear();
    if(currentFile.isDirectory()){ 
      zBytes = 0;
      zFiles = 0;
      checkDir(currentFile, 1);
    } else {
      listFiles.add(currentFile);
      zBytes = co.src.length();
      zFiles = 1;
    }
    co.callBack.data3 = zBytes;  //number between 0...1000
    co.callBack.data4 = zFiles;  //number between 0...1000
    co.callBack.id = FileRemoteAccessor.kNrofFilesAndBytes;
    co.callBack.dst.processEvent(co.callBack);
  }
  
  
  
  void checkDir(File dir, int recursion){
    //try{
      File[] files = dir.listFiles();
      for(File file: files){
        if(file.isDirectory()){
          if(recursion < 100){ //prevent loop with itself
            checkDir(file, recursion+1);  //recursively
          }
        } else {
          listFiles.add(file);
          zFiles +=1;
          zBytes += file.length();
        }
      }
    //}catch(IOException exc){
      
    //}
  }
  
  
  
  void execCopy(Commission co){
    FileInputStream in = null;
    FileOutputStream out = null;
    final long zBytesMax = co.src.length();
    long zBytesCum = 0;
    try{
      long timestart = System.currentTimeMillis();
      in = new FileInputStream(co.src);
      out = new FileOutputStream(co.dst);
      byte[] buffer = new byte[0x4000];  //16 kByte buffer
      boolean bContCopy;
      do {
        int zBytes = in.read(buffer);
        if(zBytes > 0){
          bContCopy = true;
          zBytesCum += zBytes;
          out.write(buffer, 0, zBytes);
          long time = System.currentTimeMillis();
          //
          //feedback of progression after about 0.3 second. 
          if(time > timestart + 300){
            co.callBack.data1 = (int)((float)zBytesCum / zBytesMax * 1000);  //number between 0...1000
            co.callBack.id = FileRemoteAccessor.kOperation;
            co.callBack.dst.processEvent(co.callBack);
            timestart = time;
          }
        } else if(zBytes == -1){
          bContCopy = false;
          out.close();
          co.callBack.id = zBytesCum == zBytesMax ? FileRemoteAccessor.kFinishOk : FileRemoteAccessor.kFinishNok;
          co.callBack.dst.processEvent(co.callBack);
        } else {
          //0 bytes ?
          bContCopy = true;
        }
      }while(bContCopy);
    } catch(IOException exc){
      co.callBack.data1 = (int)((float)zBytesCum / zBytesMax * 1000);  //number between 0...1000
      co.callBack.id = FileRemoteAccessor.kFinishError;
      co.callBack.dst.processEvent(co.callBack);
    }
    try{
      if(in !=null) { in.close(); }
      if(out !=null) { out.close(); }
    }catch(IOException exc){}
  }
  
  
  void execDel(Commission commission){
    
  }
  
  
  
}
