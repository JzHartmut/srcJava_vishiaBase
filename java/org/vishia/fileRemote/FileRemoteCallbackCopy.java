package org.vishia.fileRemote;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.LinkedList;
import java.util.List;

import org.vishia.util.Debugutil;
import org.vishia.util.FileSystem;
import org.vishia.util.SortedTreeWalkerCallback;
import org.vishia.util.StringFunctions;


/**This class contains the callback operations used for 
 * {@link FileRemoteAccessor#walkFileTreeCheck(FileRemote, boolean, boolean, boolean, String, long, int, FileRemoteWalkerCallback)}
 * to copy the content of a directory tree called in {@link FileRemote#copyDirTreeTo(FileRemote, int, String, int, FileRemoteWalkerCallback, FileRemoteProgressTimeOrder)}.
 * 
 * @author Hartmut Schorrig
 *
 */
public class FileRemoteCallbackCopy implements FileRemoteWalkerCallback
{
  /**Version, history and license.
   * <ul>
   * <li>2014-12-12 Hartmut new: {@link CompareCtrl}: Comparison with suppressed parts especially comments. 
   * <li>2013-09-19 created. Comparison in callback routine of walkThroughFiles instead in the graphic thread.
   * </ul>
   * 
   * <b>Copyright/Copyleft</b>:
   * For this source the LGPL Lesser General Public License,
   * published by the Free Software Foundation is valid.
   * It means:
   * <ol>
   * <li> You can use this source without any restriction for any desired purpose.
   * <li> You can redistribute copies of this source to everybody.
   * <li> Every user of this source, also the user of redistribute copies
   *    with or without payment, must accept this license for further using.
   * <li> But the LPGL is not appropriate for a whole software product,
   *    if this source is only a part of them. It means, the user
   *    must publish this part of source,
   *    but don't need to publish the whole source of the own product.
   * <li> You can study and modify (improve) this source
   *    for own using or for redistribution, but you have to license the
   *    modified sources likewise under this LGPL Lesser General Public License.
   *    You mustn't delete this Copyright/Copyleft inscription in this source file.
   * </ol>
   * If you are intent to use this sources without publishing its usage, you can get
   * a second license subscribing a special contract with the author. 
   * 
   * @author Hartmut Schorrig = hartmut.schorrig@vishia.de
   * 
   * 
   */
  //@SuppressWarnings("hiding")
  static final public String sVersion = "2014-12-14";
  
  

  private FileRemote dirDst;
  
  private boolean first;
  
  //private final String basepath1;
  //private final int zBasePath1;
  
  /**Event instance for user callback. */
  private final FileRemoteProgressTimeOrder timeOrderProgress;  //FileRemote.CallbackEvent evCallback;
  
  private final FileRemoteWalkerCallback callbackUser;
  
  int mode;
  
  //byte[] buffer = new byte[16384]; 
  
  byte[] buffer = new byte[16384];
  
  boolean aborted = false;
  
  /**Constructs an instance to execute a comparison of directory trees.
   * @param dir1
   * @param dirDst
   * @param evCallback maybe null, if given, this event will be sent to show the progression of the comparison
   */
  FileRemoteCallbackCopy(FileRemote dirDstStart, FileRemoteWalkerCallback callbackUser, FileRemoteProgressTimeOrder timeOrderProgress) { //FileRemote.CallbackEvent evCallback){
    //this.evCallback = evCallback;
    this.timeOrderProgress = timeOrderProgress;
    this.callbackUser = callbackUser;
    this.dirDst = dirDstStart;
  }
  
  
  
  @Override public void start(FileRemote startDir)
  {
    first = true;
  }
  
  
  
  @Override public Result offerParentNode(FileRemote dir){
    if(first){
      first = false;  //first level: don't change dirDst. It matches to the first source dir.
    } else {
      String name = dir.getName();
      dirDst = FileRemote.getDir(dirDst.getPathChars() + "/" + name);
      if(this.timeOrderProgress !=null) {
        this.timeOrderProgress.currDir = dir;
      }
    }
    return Result.cont;
    
  }
  
  /**Checks whether all files are compared or whether there are alone files.
   */
  @Override public Result finishedParentNode(FileRemote file){
    dirDst = dirDst.getParentFile();
    return Result.cont;      
  }
  
  
  /**This method creates a new file in the current dst directory with the same name as the given file.
   * Note: because the FileRemote concept the file can be located on any remote device.
   * @see org.vishia.util.SortedTreeWalkerCallback#offerLeafNode(java.lang.Object)
   */
  @Override public Result offerLeafNode(FileRemote file, Object info)
  {
    FileRemote fileDst = dirDst.child(file.getName());
    InputStream inp = null;
    OutputStream wr = null;
    try{
      long nrofBytesSum0 = 0;
      if(this.timeOrderProgress !=null) {
        nrofBytesSum0 = this.timeOrderProgress.nrofBytesAll;
        this.timeOrderProgress.nrofBytesFile = file.length();
        this.timeOrderProgress.nrofBytesFileCopied = 0;
        this.timeOrderProgress.nrFilesProcessed +=1;
        this.timeOrderProgress.currFile = file;
      }

      inp = file.openInputStream(0);
      wr = fileDst.openOutputStream(0);
      if(inp == null || wr == null) {
        
      } else {
        int bytes, sum = 0;
        while( (bytes = inp.read(this.buffer)) >0){
          wr.write(this.buffer, 0, bytes);
          sum += bytes;
          if(this.timeOrderProgress !=null) {
            this.timeOrderProgress.nrofBytesFileCopied = sum;
            this.timeOrderProgress.nrofBytesAll = nrofBytesSum0 + sum;
          }
        }
        if(this.callbackUser !=null) {
          this.callbackUser.offerLeafNode(file, info);
        }
      }
    } catch(IOException exc) {
      System.err.println(exc.toString());
    } finally {
      try{
        if(inp !=null) inp.close();
        if(wr !=null) wr.close();
      } catch(IOException exc){
        throw new RuntimeException(exc);
      }
    }
    return Result.cont;
  }

  
  
  @Override public boolean shouldAborted(){
    return aborted;
  }

  
  
  
  
  
  
  @Override public void finished(FileRemote startDir)
  {
    if(this.timeOrderProgress !=null){
      this.timeOrderProgress.activateDone(); 
    }
    /*
    if(evCallback !=null && evCallback.occupyRecall(500, null, true) !=0){
      evCallback.sendEvent(FileRemote.CallbackCmd.done);
    }
    */
  }


  
  
  
  
}
