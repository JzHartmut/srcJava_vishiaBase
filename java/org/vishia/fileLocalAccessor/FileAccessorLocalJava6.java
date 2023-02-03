package org.vishia.fileLocalAccessor;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.EventObject;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.vishia.event.EventCmdtype;
import org.vishia.event.EventCmdtypeWithBackEvent;
import org.vishia.event.EventConsumer;
import org.vishia.event.EventSource;
import org.vishia.event.EventTimerThread;
import org.vishia.fileRemote.FileAccessZip;
import org.vishia.fileRemote.FileRemote;
import org.vishia.fileRemote.FileRemoteAccessor;
import org.vishia.fileRemote.FileRemote.CallbackEvent;
import org.vishia.fileRemote.FileRemote.Cmd;
import org.vishia.fileRemote.FileRemoteCallback;
import org.vishia.fileRemote.FileRemoteProgressTimeOrder;
import org.vishia.util.Assert;
import org.vishia.util.FileSystem;


/**Implementation for a standard local file.
 */
public class FileAccessorLocalJava6 extends FileRemoteAccessor
{
  
  /**Version, history and license.
   * <ul>
   * <li>2013-03-31 Hartmut bugfix: number of percent in backevent while copy
   * <li>2012-11-17 Hartmut chg: review of {@link #execChgProps(org.vishia.fileRemote.FileRemote.CmdEvent)} etc. It should not work before.
   *   yet not all is tested. 
   * <li>2012-10-01 Hartmut chg: Some adaption because {@link FileRemote#listFiles()} returns File[] and not FileRemote[].
   * <li>2012-10-01 Hartmut experience {@link #useFileChildren}
   * <li>2012-10-01 Hartmut new: {@link #refreshFilePropertiesAndChildren(FileRemote, org.vishia.fileRemote.FileRemote.CallbackEvent)} time measurement
   * <li>2012-09-26 Hartmut new: {@link #refreshFileProperties(FileRemote, org.vishia.fileRemote.FileRemote.CallbackEvent)} 
   *   thread with exception msg.
   * <li>2012-08-05 Hartmut new: If the oFile reference is null, the java.io.File instance for the local file will be created anyway.
   * <li>2012-08-03 Hartmut chg: Usage of Event in FileRemote. 
   *   The FileRemoteAccessor.Commission is removed yet. The same instance FileRemote.Callback, now named FileRemote.FileRemoteEvent is used for forward event (commision) and back event.
   * <li>2012-07-30 Hartmut new: execution of {@link #refreshFileProperties(FileRemote, EventCmdPingPongType)} and {@link #refreshFilePropertiesAndChildren(FileRemote, EventCmdPingPongType)}
   *   in an extra thread if a callback is given. It is substantial for a fluently working with files, if an access
   *   for example in network hangs.
   * <li>2012-07-28 Hartmut new: Concept of remote files enhanced with respect to {@link FileAccessZip},
   *   see {@link FileRemote}
   * <li>2012-03-10 Hartmut new: implementation of the {@link FileRemote#chgProps(String, int, int, long, org.vishia.fileRemote.FileRemote.CallbackEvent)} etc.
   * <li>2012-02-02 Hartmut chg: {@link #refreshFileProperties(FileRemote, File)}: There was an faulty recursive loop,
   *   more checks. 
   * <li>2012-01-09 Hartmut new: {@link #close()} terminates the thread.
   * <li>2012-01-06 Hartmut new: {@link #refreshFileProperties(FileRemote)} etc.
   * <li>2012-01-04 Hartmut new: copy file trees started from a given directory
   * <li>2011-12-31 Hartmut new {@link #execCopy(org.vishia.util.FileRemoteAccessor.Commission)}. 
   * <li>2011-12-31 Hartmut new {@link #runCommissions} as extra thread.  
   * <li>2011-12-10 Hartmut creation: See {@link FileRemoteAccessor}.
   * </ul>
   * <br><br>
   * <b>Copyright/Copyleft</b>:
   * For this source the LGPL Lesser General Public License,
   * published by the Free Software Foundation is valid.
   * It means:
   * <ol>
   * <li> You can use this source without any restriction for any desired purpose.
   * <li> You can redistribute copies of this source to everybody.
   * <li> Every user of this source, also the user of redistribute copies
   *    with or without payment, must accept this license for further using.
   * <li> But the LPGL ist not appropriate for a whole software product,
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
   */
  public static final int version = 20130331;

  /**Some experience possible: if true, then store File objects in {@link FileRemote#children} instead
   * {@link FileRemote} objects. The File objects may be replaces by FileRemote later if necessary. This may be done
   * in applications. The problem is: Wrapping a File with FileRemote does not change the reference in {@link FileRemote#children}
   * automatically. It should be done by any algorithm. Therefore this compiler switch is set to false yet.
   */
  private final static boolean useFileChildren = false;
  
  
  private static FileRemoteAccessor instance;
  
  /**The state machine for executing over some directory trees is handled in this extra class.
   * Note: the {@link Copy#Copy(FileAccessorLocalJava7)} needs initialized references
   * of {@link #singleThreadForCommission} and {@link #executerCommission}.
   */
  protected final FileLocalAccessorCopyStateM states = new FileLocalAccessorCopyStateM();  

  
  EventSource evSrc = new EventSource("FileLocalAccessor"){
    @Override public void notifyDequeued(){}
    @Override public void notifyConsumed(int ctConsumed){}
    @Override public void notifyRelinquished(int ctConsumed){}
    @Override public void notifyShouldSentButInUse(){ throw new RuntimeException("event usage error"); }

    @Override public void notifyShouldOccupyButInUse(){throw new RuntimeException("event usage error"); }

  };

  

  
  EventTimerThread singleThreadForCommission = new EventTimerThread("FileAccessor-local");
  
  /**Destination for all events which forces actions in the execution thread.
   * 
   */
  EventConsumer executerCommission = new EventConsumer(){
    @Override public int processEvent(EventObject ev) {
      /*if(ev instanceof Copy_FileLocalAccJava6.EventCpy){ //internal Event
        //copy.stateCopy.processEvent(ev);
        return 1;
      } else*/ if(ev instanceof FileRemote.CmdEvent){  //event from extern
            execCommission((FileRemote.CmdEvent)ev);
        return 1;
      } else {
        return 0;
      }
    }
    
    @Override public String toString(){ return "FileRemoteAccessorLocal - executerCommision"; }

  };
  

  
  /**The state machine for executing over some directory trees is handled in this extra class.
   * Note: the {@link Copy#Copy(FileRemoteAccessorLocalFile)} needs initialized references
   * of {@link #singleThreadForCommission} and {@link #executerCommission}.
   */
  //private final Copy_FileLocalAccJava6 copy = new Copy_FileLocalAccJava6(this);  
  
  private FileRemote workingDir;
  
  public FileAccessorLocalJava6() {
    //singleThreadForCommission.startThread();
  }
  
  
  
  /**Returns the singleton instance of this class.
   * Note: The instance will be created and the thread will be started if this routine was called firstly.
   * @return The singleton instance.
   */
  public static FileRemoteAccessor getInstance(){
    if(instance == null){
      ClassLoader classLoader = ClassLoader.getSystemClassLoader();
      try{ 
        classLoader.loadClass("java.nio.file.Files");
        @SuppressWarnings("unchecked")
        Class<? extends FileRemoteAccessor> clazz = 
          (Class<? extends FileRemoteAccessor>)classLoader.loadClass("org.vishia.fileLocalAccessor.FileAccessorLocalJava7");
          instance = clazz.newInstance(); //new FileAccessorLocalJava7();
      } catch(Exception exc){
        instance = new FileAccessorLocalJava6();  //use fallback strategy
      }
    }
    return instance;
  }
  

  
  
  private File getLocalFile(FileRemote fileRemote){
    //NOTE: use the superclass File only as interface, use a second instance.
    //the access to super methods does not work. Therefore access to non-inherited File.methods.
    if(fileRemote.oFile() == null){
      String path = fileRemote.getPath();
      fileRemote.setFileObject(new File(path));
    }
    return (File)fileRemote.oFile();
  }
  
  
  /*
  @Override public Object createFileObject(FileRemote file)
  { Object oFile = new File(file.path, file.name);
    return oFile;
  }
  */
  
  
  /**Sets the file properties from the local file.
   * checks whether the file exists and set the {@link FileRemote#mTested} flag any time.
   * If the file exists, the properties of the file were set, elsewhere they were set to 0.
   * @see {@link org.vishia.fileRemote.FileRemoteAccessor#refreshFileProperties(org.vishia.fileRemote.FileRemote)}
   */
  @Override public void refreshFileProperties(final FileRemote fileRemote, final FileRemote.CallbackEvent callback)
  { 
  
    
    /**Strategy: use an inner private routine which is encapsulated in a Runnable instance.
     * either run it locally or run it in an extra thread.
     */
    Runnable thread = new RunRefresh(fileRemote, callback);
  
    //the method body:
    if(callback == null){
      thread.run(); //run direct
    } else {
      Thread threadObj = new Thread(thread);
      threadObj.start(); //run in an extra thread, the caller doesn't wait.
    }
  
  }  
    

  
  //@Override 
  public void XXXXrefreshFilePropertiesAndChildren(final FileRemote fileRemote, final FileRemote.CallbackEvent callback){
    //a temporary instance for the thread routine.
    RunRefreshWithChildren thread = new RunRefreshWithChildren(fileRemote, callback);
    //the method body:
    if(callback == null){
      thread.run(); //run direct
    } else {
      if((fileRemote.getFlags() & FileRemote.mThreadIsRunning) ==0) { //check whether another thread is running with this file.
        fileRemote.internalAccess().setFlagBit(FileRemote.mThreadIsRunning);
        Thread threadObj = new Thread(thread);
        thread.time = System.currentTimeMillis();
        threadObj.start(); //run in an extra thread, the caller doesn't wait.
      } else {
        System.err.println("FileRemoteAccessLocalFile.refreshFilePropertiesAndChildren - double call, ignored;");
        callback.relinquish(); //ignore it.
      }
    }
  }

  
  @Override
  public List<File> getChildren(FileRemote file, FileFilter filter){
    File data = (File)file.oFile();
    File[] children = data.listFiles(filter);
    List<File> list = new LinkedList<File>();
    if(children !=null){
      for(File file1: children){
        list.add(file1);
      }
    }
    return list;
  }
  
  
  /**Variant of getChildren for non-Java-7. Firstly all children without its properties are gotten
   * from the operation system using {@link java.io.File#list()}. Therefore {@link #refreshFilePropertiesAndChildren(FileRemote, CallbackEvent)}
   * will be called. Then this list is iterated and the file properties are gotten using 
   * {@link #refreshFileProperties(FileRemote, CallbackEvent)}. In any iteration step the file
   * is offered to the application calling {@link FileRemoteCallback#offerLeafNode(FileRemote)}.
   * 
   * @see org.vishia.fileRemote.FileRemoteAccessor#walkFileTree(org.vishia.fileRemote.FileRemote, java.io.FileFilter, int, org.vishia.fileRemote.FileRemoteCallback)
   */
  @Override public void walkFileTree(FileRemote startDir, boolean bWait, boolean bRefreshChildren, boolean resetMark, String sMask, long bMarkCheck, int depth, FileRemoteCallback callback)
  {
    callback.start(startDir);
    walkSubTree(startDir, null, depth, callback);
    callback.finished(startDir, null);
  }
    
  public FileRemoteCallback.Result walkSubTree(FileRemote file, FileFilter filter, int depth, FileRemoteCallback callback)
  {
    XXXXrefreshFilePropertiesAndChildren(file, null);
    Map<String, FileRemote> children = file.children();
    FileRemoteCallback.Result result = FileRemoteCallback.Result.cont;
    if(children !=null){
      result = callback.offerParentNode(file);
      if(result == FileRemoteCallback.Result.cont){ //only walk through subdir if cont
        Iterator<Map.Entry<String, FileRemote>> iter = children.entrySet().iterator();
        while(result == FileRemoteCallback.Result.cont && iter.hasNext()) {
          Map.Entry<String, FileRemote> file1 = iter.next();
          FileRemote file2 = file1.getValue();
          refreshFileProperties(file2, null);
          if(file2.isDirectory()){
            if(depth >1){
              result = walkSubTree(file2, filter, depth-1, callback);  
            } else {
              result = callback.offerLeafNode(file2, null);  //show it as file instead walk through tree
            }
          } else {
            result = callback.offerLeafNode(file2, null);
          }
        }
      } 
    }
    if(result != FileRemoteCallback.Result.terminate){
      //continue with parent. Also if offerDir returns skipSubdir or any file returns skipSiblings.
      result = FileRemoteCallback.Result.cont;
    }
    return result;  //maybe terminate
  }


  
  /**Routine for walk through all really files of the file system for PC file systems and Java7 or higher. 
   * It calls {@link Files#walkFileTree(Path, Set, int, FileVisitor)} in an extra thread.
   * defined in {@link FileRemoteAccessor#walkFileTree(FileRemote, boolean, boolean, boolean, String, long, int, FileRemoteCallback)} 
   */
  @Override public void walkFileTreeCheck(FileRemote startDir, final boolean bWait, boolean bRefreshChildren, boolean resetMark, String sMask, long bMarkCheck, int depth, FileRemoteCallback callback)
  {
    //TODO
  }
  
  
  @Override public boolean setLastModified(FileRemote file, long time)
  { File ffile = (File)file.oFile();
    if(ffile !=null){ return ffile.setLastModified(time); }
    else return false;
  }

  
  
  @Override public ReadableByteChannel openRead(FileRemote file, long passPhase)
  { try{ 
      @SuppressWarnings("resource") //will be closed on ReadableByteChannel.close();
      FileInputStream stream = new FileInputStream(file);
      return stream.getChannel();
    } catch(FileNotFoundException exc){
      return null;
    }
  }

  
  
  @Override public InputStream openInputStream(FileRemote file, long passPhase){
    try{ 
      FileInputStream stream = new FileInputStream(file);
      return stream;
    } catch(FileNotFoundException exc){
      return null;
    }
    
  }
  

  @Override public OutputStream openOutputStream(FileRemote file, long passPhase){
    try{ 
      FileOutputStream stream = new FileOutputStream(file);
      return stream;
    } catch(FileNotFoundException exc){
      return null;
    }
    
  }
  


  @Override public WritableByteChannel openWrite(FileRemote file, long passPhase)
  { try{ 
    @SuppressWarnings("resource") //will be closed on WriteableByteChannel.close();
    FileOutputStream stream = new FileOutputStream(file);
    return stream.getChannel();
    } catch(FileNotFoundException exc){
      return null;
    }
  }

  
  
  @Override public boolean createNewFile(FileRemote file, FileRemote.CallbackEvent callback) throws IOException{
    File file1;
    if(file.oFile() == null){
      file.setFileObject(file1 = new File(file.getAbsolutePath()));
    } else {
      file1 = (File) file.oFile();
    }
    return file1.createNewFile();
  }



  
  @Override public boolean mkdir(FileRemote file, boolean subdirs, FileRemote.CallbackEvent evback){
    File file1 = (File)file.oFile();
    if(file1 == null){ 
      file1 = new File(file.getAbsolutePath());
      file.setFileObject(file1);
    }
    if(evback == null){ 
      if(subdirs){ return file1.mkdirs(); }
      else { return file1.mkdir(); }
    } else {
      FileRemote.CmdEvent ev = prepareCmdEvent(500, evback);
      ev.filesrc = file;
      ev.filedst = null;
      ev.sendEvent(subdirs ? Cmd.mkDirs: Cmd.mkDir);
      return true;
    }
  }

  
  private void mkdir(boolean recursively, FileRemote.CmdEvent ev) {
    boolean bOk = mkdir(ev.filesrc, recursively, null);  //call direct
    FileRemote.CallbackEvent evback = ev.getOpponent();
    if(evback.occupy(evSrc, true)){
      evback.sendEvent(bOk ? FileRemote.CallbackCmd.done : FileRemote.CallbackCmd.nok);
    }
  }

  
  

  
  
  @Override public boolean delete(FileRemote file, FileRemote.CallbackEvent callback){
    File fileLocal = getLocalFile(file);
    if(callback == null){
      return fileLocal.delete();
    } else {
      boolean bOk = fileLocal.delete();
      callback.occupy(evSrc, true);
      callback.sendEvent(bOk ? FileRemote.CallbackCmd.done : FileRemote.CallbackCmd.errorDelete );
      return bOk;
    }
  }

  
  
  @Override public void copyChecked(FileRemote fileSrc, String pathDst, String nameModification, int mode, FileRemoteCallback callbackUser, FileRemoteProgressTimeOrder timeOrderProgress)
  {
    states.copyChecked(fileSrc, pathDst, nameModification, mode, callbackUser, timeOrderProgress);
  }

  
  @Override public void search(FileRemote fileSrc, byte[] search, FileRemoteCallback callbackUser, FileRemoteProgressTimeOrder timeOrderProgress) {
    //TODO
  }


  
  @Override public boolean isLocalFileSystem()
  {  return true;
  }

  
  @Override public CharSequence getStateInfo(){ return ""; } //states.getStateInfo(); }
  

  
  /**Creates an CmdEvent if necessary, elsewhere uses the opponent of the given evBack and occupies it.
   * While occupying the Cmdevent is completed with the destination, it is {@link #executerCommission}.
   * @see org.vishia.fileRemote.FileRemoteAccessor#prepareCmdEvent(org.vishia.fileRemote.FileRemote.CallbackEvent)
   */
  @Override public FileRemote.CmdEvent prepareCmdEvent(int timeout, EventCmdtypeWithBackEvent<?, FileRemote.CmdEvent>  evBack){
    FileRemote.CmdEvent cmdEvent1;
    if(evBack !=null && (cmdEvent1 = (FileRemote.CmdEvent)evBack.getOpponent()) !=null){
      if(!cmdEvent1.occupy(timeout, evSrc, executerCommission, singleThreadForCommission)){
        return null;
      }
    } else {
      cmdEvent1 = new FileRemote.CmdEvent(evSrc, executerCommission, singleThreadForCommission, (FileRemote.CallbackEvent)evBack);
    }
    return  cmdEvent1; 
  }
  
  
  void execCommission(FileRemote.CmdEvent commission){
    FileRemote.Cmd cmd = commission.getCmd();
    switch(cmd){
      case check: //copy.checkCopy(commission); break;
      case abortAll:     //should abort the state machine!
      case delChecked:
      case moveChecked:
      //case copyChecked: 
        //copy.stateCopy.processEvent(commission); break;
      //case move: copy.execMove(commission); break;
      case chgProps:  execChgProps(commission); break;
      case chgPropsRecurs:  execChgPropsRecurs(commission); break;
      case countLength:  execCountLength(commission); break;
      case delete:  execDel(commission); break;
      case mkDir: mkdir(false, commission); break;
      case mkDirs: mkdir(true, commission); break;
 
      
    }
  }
  
  
  
  
  
  
  private void execChgProps(FileRemote.CmdEvent co){
    FileRemote dst;
    //FileRemote.FileRemoteEvent callBack = co;  //access only 1 time, check callBack. co may be changed from another thread.
    boolean ok = co !=null;
    if(co.newName() !=null && ! co.newName().equals(co.filesrc().getName())){
      File fileRenamed = new File(co.filesrc.getParent(), co.newName());
      ok &= co.filesrc.renameTo(fileRenamed);
      dst = FileRemote.fromFile(co.filesrc.itsCluster, fileRenamed);
    } else {
      dst = co.filesrc;
    }
    ok = chgFile(dst, co.maskFlags(), co.newFlags(), ok);
    FileRemote.CallbackCmd cmd;
    if(ok){
      cmd = FileRemote.CallbackCmd.done; 
    } else {
      cmd = FileRemote.CallbackCmd.nok; 
    }
    FileRemote.CallbackEvent evback = co.getOpponent();
    
    evback.occupy(evSrc, true);
    evback.sendEvent(cmd );
  }
  
  
  private void execChgPropsRecurs(FileRemote.CmdEvent co){
    FileRemote dst;
    boolean ok = co !=null;
    if(co.newName() !=null && ! co.newName().equals(co.filesrc.getName())){
      FileRemote fileRenamed = co.filesrc.getParentFile().child(co.newName());
      ok &= co.filesrc.renameTo(fileRenamed);
      dst = fileRenamed;
    } else {
      dst = co.filesrc;
    }
    ok &= chgPropsRecursive(dst, co.maskFlags(), co.newFlags(), ok, 0);
    FileRemote.CallbackCmd cmd;
    if(ok){
      cmd = FileRemote.CallbackCmd.done ; 
    } else {
      cmd = FileRemote.CallbackCmd.error ; 
    }
    FileRemote.CallbackEvent evback = co.getOpponent();
    evback.occupy(evSrc, true);
    evback.sendEvent(cmd);
  }
  
  
  
  private boolean chgPropsRecursive(File dst, int maskFlags, int newFlags, boolean ok, int recursion){
    if(recursion > 100){
      throw new IllegalArgumentException("FileRemoteAccessorLocal.chgProsRecursive: too many recursions ");
    }
    if(dst.isDirectory()){
      File[] filesSrc = dst.listFiles();
      for(File fileSrc: filesSrc){
        ok = chgPropsRecursive(fileSrc, maskFlags, newFlags, ok, recursion +1);
      }
    } else {
      ok = chgFile(dst, maskFlags, newFlags, ok);
    }
    return ok;
  }
  

  
  private boolean chgFile(File dst, int maskFlags, int newFlags, boolean ok){
    //if(dst instanceof FileRemote)
    //int flagsNow = dst.getFlags();
    //int chg = (flagsNow ^ newFlags) & maskFlags;  //changed and masked
    int chg = maskFlags;
    int mask = 1;
    while(mask !=0){
      if((chg & mask)!=0){ 
        if(!chgFile1(dst, mask, newFlags)){
          ok = false;
        }
      }
      mask <<=1;
    }
    return ok;
  }
  
  
  private boolean chgFile1(File dst, int maskFlags, int newFlags){
    boolean bOk;
    boolean set = (newFlags & maskFlags ) !=0;
    switch(maskFlags){
      case FileRemote.mCanWrite:{ bOk = dst.setWritable(set); } break;
      case FileRemote.mCanWriteAny:{ bOk = dst.setWritable(set, true); } break;
      default: { bOk = true; }   //TODO only writeable supported yet, do rest
    }//switch
    if(bOk && dst instanceof FileRemote){
      FileRemote dst1 = (FileRemote)dst;
      dst1.internalAccess().setOrClrFlagBit(maskFlags, set);
    }
    return bOk;
  }
  
  
  
  private void execCountLength(FileRemote.CmdEvent co){
    long length = countLengthDir(co.filesrc, 0, 0);    
    FileRemote.CallbackEvent evback = co.getOpponent();
    evback.occupy(evSrc, true);
    FileRemote.CallbackCmd cmd;
    if(length >=0){
      cmd = FileRemote.CallbackCmd.done; 
      evback.nrofBytesAll = length;
    } else {
      cmd = FileRemote.CallbackCmd.nok; 
    }
    evback.sendEvent(cmd );
  }
  
  
  private long countLengthDir(File file, long sum, int recursion){
    if(recursion > 100){
      throw new IllegalArgumentException("FileRemoteAccessorLocal.chgProsRecursive: too many recursions ");
    }
    if(file.isDirectory()){
      File[] filesSrc = file.listFiles();
      for(File fileSrc: filesSrc){
        sum = countLengthDir(fileSrc, sum, recursion+1);
      }
    } else {
      sum += file.length();
    }
    return sum;
  }
  
  
  
  void execDel(FileRemote.CmdEvent co){
    System.err.println("FileRemoteLocal - execDel not implemented yet.");
  }


  @Override public void close() throws IOException
  { singleThreadForCommission.close();
  }
  
  
  
  /**A thread which gets all file properties independent of a caller of the #re
   */
  private class RunRefresh implements Runnable{
    final FileRemote fileRemote;
    
    final FileRemote.CallbackEvent callback;
    
    RunRefresh(final FileRemote fileRemote, final FileRemote.CallbackEvent callback){
      this.fileRemote= fileRemote;
      this.callback = callback;
    }
    
    public void run(){
      File fileLocal = getLocalFile(fileRemote);
      String path = fileRemote.getPath();
      if(fileLocal.exists()){
        String canonicalPath = FileSystem.getCanonicalPath(fileLocal);
        long date = fileLocal.lastModified();
        long length = fileLocal.length();
        int flags = FileRemote.mExist | FileRemote.mTested;
        if(fileLocal.isDirectory()){ flags |= FileRemote.mDirectory; }
        if(fileLocal.isHidden()){ flags |= FileRemote.mHidden; }
        if(fileLocal.canWrite()){ flags |= FileRemote.mCanWrite; }
        if(fileLocal.canRead()){ flags |= FileRemote.mCanRead; }
        if(fileLocal.canExecute()){ flags |= FileRemote.mExecute; }
        if(fileLocal.isDirectory()){ flags |= FileRemote.mDirectory; }
        if(fileLocal.isDirectory()){ flags |= FileRemote.mDirectory; }
        fileRemote._setProperties(length, date, 0, 0, flags, fileLocal);
        if(fileLocal.isAbsolute()){
          String pathCleaned = FileSystem.cleanAbsolutePath(path);
          if(!canonicalPath.startsWith(pathCleaned)){
            fileRemote.setSymbolicLinkedPath(canonicalPath);
          } else {
            fileRemote.setCanonicalAbsPath(canonicalPath);
          }
        } else { //relative path
          Assert.stop();
          /*
          if(workingDir == null){
            workingDir = new FileRemote(FileSystem.getCanonicalPath(new File(".")));  //NOTE: should be absolute
          }
          fileRemote.setReferenceFile(workingDir);
          */  
        }
      } else { //fileLocal not exists:
        //designate it as tested, mExists isn't set.
        fileRemote._setProperties(0, 0, 0, 0, FileRemote.mTested, fileLocal);
      }
      fileRemote.timeRefresh = System.currentTimeMillis();
      if(callback !=null){
        callback.occupy(evSrc, true);
        callback.sendEvent(FileRemote.CallbackCmd.done);
      }
    }
    
    
  }
  
  
  /**A thread which gets all file properties inclusive children independent of a caller of the #re
   */
  private class RunRefreshWithChildren implements Runnable{
    long time;
    
    final FileRemote fileRemote;
    
    final FileRemote.CallbackEvent callback;
    
    RunRefreshWithChildren(final FileRemote fileRemote, final FileRemote.CallbackEvent callback){
      this.fileRemote= fileRemote;
      this.callback = callback;
    }
    
    public void run(){  ////
      try{
        time = System.currentTimeMillis();
        refreshFileProperties(fileRemote, null);
        File fileLocal = getLocalFile(fileRemote);
        //fileRemote.flags |= FileRemote.mChildrenGotten;
        if(fileLocal.exists()){
          long time1 = System.currentTimeMillis();
          System.out.println("FileAccessorLocalJava6.refreshFilePropertiesAndChildren - start listFiles; dt=" + (time1 - time));
          
          File[] files = fileLocal.listFiles();
          time1 = System.currentTimeMillis();
          System.out.println("FileAccessorLocalJava6.refreshFilePropertiesAndChildren - ok listFiles; dt=" + (time1 - time));
          if(files !=null){
            if(useFileChildren){
              //fileRemote.children = files;
            } else {
              //re-use given children because they may have additional designation in flags.
              Map<String, FileRemote> oldChildren = fileRemote.children();
              //but create a new list to prevent keeping old files.
              fileRemote.internalAccess().newChildren();
              int iFile = -1;
              for(File file1: files){
                String name1 = file1.getName();
                FileRemote child = null;   
                if(oldChildren !=null){ child = oldChildren.remove(name1); }
                if(child == null){ 
                  int flags = file1.isDirectory() ? FileRemote.mDirectory : 0;
                  child = fileRemote.internalAccess().newChild(name1, 0, 0, 0, 0, flags, file1); 
                  //child.refreshProperties(null);    //should show all sub files with its properties, but not files in sub directories.
                } else {
                  if(!child.isTested(time - 1000)){
                    //child.refreshProperties(null);    //should show all sub files with its properties, but not files in sub directories.
                  }
                }
                fileRemote.internalAccess().putNewChild(child);
              }
              //oldChildren contains yet removed files.
              System.out.println("FileAccessorLocalJava6.refreshFilePropertiesAndChildren - ok refresh; " + files.length + " files; dt=" + (System.currentTimeMillis() - time));
            }
          }
        }
        fileRemote.timeChildren = System.currentTimeMillis();
        if(callback !=null){
          callback.occupy(evSrc, true);
          long time1 = System.currentTimeMillis();
          System.out.println("FileAccessorLocalJava6.refreshFilePropertiesAndChildren - callback listFiles; dt=" + (time1 - time));
          callback.sendEvent(FileRemote.CallbackCmd.done);
          time1 = System.currentTimeMillis();
          System.out.println("FileAccessorLocalJava6.refreshFilePropertiesAndChildren - finish listFiles; dt=" + (time1 - time));
        }
        fileRemote.internalAccess().clrFlagBit(FileRemote.mThreadIsRunning);
      }
      catch(Exception exc){
        System.err.println("FileAccessorLocalJava6.refreshFilePropertiesAndChildren - Thread Excpetion;" + exc.getMessage());
      }
    }
  }
    

  
  
  
  /**Access selector which uses {@link FileAccessorLocalJava6} for any path.
   * It is the standard for normal PC programs.
   * 
   */
  public static FileRemote.FileRemoteAccessorSelector selectLocalFileAlways = new FileRemote.FileRemoteAccessorSelector() {
    @Override public FileRemoteAccessor selectFileRemoteAccessor(String sPath) {
      return FileAccessorLocalJava6.getInstance();
    }
  };

  @Override public void abortAll () {
    // TODO Auto-generated method stub
    
  }



  @Override public String copyFile ( FileRemote src, FileRemote dst, CallbackEvent callback ) {
    // TODO Auto-generated method stub
    return "not implemented";

  }



  @Override public String moveFile ( FileRemote src, FileRemote dst, CallbackEvent callback ) {
    // TODO Auto-generated method stub
    return "not implemented";
  }



  @Override public void activate () {
    // TODO Auto-generated method stub
    
  }

  
}
