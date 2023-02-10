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
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.EventObject;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.vishia.event.EventCmdtypeWithBackEvent;
import org.vishia.event.EventConsumer;
import org.vishia.event.EventSource;
import org.vishia.event.EventTimerThread;
import org.vishia.event.EventWithDst;
import org.vishia.fileRemote.FileCluster;
import org.vishia.fileRemote.FileMark;
import org.vishia.fileRemote.FileRemote;
import org.vishia.fileRemote.FileRemoteAccessor;
import org.vishia.fileRemote.FileRemote.Cmd;
import org.vishia.fileRemote.FileRemoteWalkerCallback;
import org.vishia.fileRemote.FileRemoteProgressEvent;
import org.vishia.util.Assert;
import org.vishia.util.Debugutil;
import org.vishia.util.FileFunctions;
import org.vishia.util.FilepathFilter;
import org.vishia.util.FilepathFilterM;
import org.vishia.util.FileSystem;
import org.vishia.util.SortedTreeWalkerCallback;
import org.vishia.util.TreeWalkerPathCheck;

/**This is the implementation of the FileRemoteAccessor working with {@link FileRemote}
 * which uses the java.nio.files startegy (new from Java-7)
 * 
 * <br>
 * German description for the java.nio.file:
 * <br><a href="https://entwickler.de/java/javaniofile-hoher-weiter-schneller">https://entwickler.de/java/javaniofile-hoher-weiter-schneller</a>
 *
 * @author hartmut
 *
 */
@SuppressWarnings("synthetic-access") 
public final class FileAccessorLocalJava7 extends FileRemoteAccessor {
  
  /**Version, history and license.
   * <ul>
   * <li>2023-02-03 Hartmut chg: experience with Thread priority. 
   *   It seems to be that the walker has generally a higher priority,  it is not proper interuptable by the SWT graphic thread ??
   *   Yet wait(10) after each directory in {@link WalkFileTreeVisitor#postVisitDirectory(Path, IOException)}
   *   to allow the graphic thread working. 
   * <li>2023-02-03 Hartmut chg: the  {@link #walkFileTreeExecInThisThread(FileRemote, boolean, boolean, String, long, int, FileRemoteWalkerCallback, FileRemoteProgressEvent)}
   *   is called recursively by {@link org.vishia.fileRemote.FileRemoteCallbackCmp#offerParentNode(FileRemote)}.
   *   Hence it is bad to set <code>progress.bDone = true;</code> in this operation, it kills the progress visibility
   *   because it sets to bDone after a sub directory. It is shifted to {@link #walkFileTree(FileRemote, boolean, boolean, boolean, String, long, int, FileRemoteWalkerCallback, FileRemoteProgressEvent)}
   *   done after really finished. 
   * <li>2023-02-03 Hartmut refactoring, the WalkFileTreeVisitorCheck is removed respectively merge to the {@link WalkFileTreeVisitor}.
   *   It was a new feature: check with a duplicated implementation instead refactored implementation. Now it is refactored. 
   *   Test: {@link org.vishia.commander.Fcmd} runs, {@link org.vishia.fileRemote.test.TestFileRemote} used for test. 
   * <li>2015-11-13 Hartmut bugfix: {@link WalkFileTreeVisitor#postVisitDirectory(Path, IOException)}: 
   *   The same directory was walked twice because the callback was called firstly. The callback forces a {@link #walkFileTree(FileRemote, boolean, boolean, boolean, String, long, int, FileRemoteWalkerCallback)}
   *   started in another thread. This marks all child files with {@link FileRemote#mRefreshChildPending} while the other thread has removed the FileRemote child instances
   *   which are marked with that. Therefore FileRemote instances were removed and created new, there are existing more as one for the same file after them.
   *   The order of execution is changed yet only, so the bug is not forced. The core of the bug is a thread safety. While a walkFileTree for a directory runs,
   *   another thread should wait for it or skip it because the other thread refreshes already in the near time.  
   * <li>2015-03-27 Hartmut now children in {@link WalkFileTreeVisitor.CurrDirChildren} is deactivate because not used before.
   *   A seldom error of twice instances for the same children of a directory was watched.  
   * <li>2014-12-21 Hartmut chg: The {@link WalkFileTreeVisitor.CurrDirChildren#children} is not used any more, the refreshing of children is done
   *   in the Map instance of {@link FileRemote#children()} with marking the children with {@link FileRemote#mRefreshChildPending} as flag bit
   *   while refreshing is pending and removing the files which's mark is remain after refresh. With them a new instance of a Map is not necessary.
   * <li>2013-09-21 Hartmut creation: Derived from {@link FileAccessorLocalJava7}
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
  public static final String sVersion = "2014-12-21";

  /**Some experience possible: if true, then store File objects in {@link FileRemote#children} instead
   * {@link FileRemote} objects. The File objects may be replaces by FileRemote later if necessary. This may be done
   * in applications. The problem is: Wrapping a File with FileRemote does not change the reference in {@link FileRemote#children}
   * automatically. It should be done by any algorithm. Therefore this compiler switch is set to false yet.
   */
  private final static boolean useFileChildren = false;
  
  
  private static FileRemoteAccessor instance;
  
  
  /**Type of the attributes of files. Set on constructor depending on the operation system.
   * 
   */
  protected final Class<? extends BasicFileAttributes> systemAttribtype;
  
  
  /**May be set to true from outside, maybe with inspector, then System.out on file walking. */
  public boolean debugout = false;
  
  /**The state machine for executing over some directory trees is handled in this extra class.
   * Note: the {@link Copy#Copy(FileAccessorLocalJava7)} needs initialized references
   * of {@link #singleThreadForCommission} and {@link #executerCommission}.
   */
//  protected final FileLocalAccessorCopyStateM states = new FileLocalAccessorCopyStateM();  
  
  EventSource evSrc = new EventSource("FileLocalAccessor"){
    @Override public void notifyDequeued(){}
    @Override public void notifyConsumed(int ctConsumed){}
    @Override public void notifyRelinquished(int ctConsumed){}
    @Override public void notifyShouldSentButInUse(){ throw new RuntimeException("event usage error"); }

    @Override public void notifyShouldOccupyButInUse(){throw new RuntimeException("event usage error"); }

  };

  

  
  /**This thread runs after creation. Only one thread for all events to access the file system
   * separated by the user thread. */
  EventTimerThread singleThreadForCommission = new EventTimerThread("FileAccessor-local");
  
  
  
  
  /**Destination for all events which forces actions in the execution thread.
   * 
   */
  EventConsumer executerCommission = new EventConsumer(){
    @Override public int processEvent(EventObject ev) {
//      if(ev instanceof FileLocalAccessorCopyStateM.EventInternal){ //internal Event
//        return FileAccessorLocalJava7.this.states.statesCopy.processEvent(ev);
//      } else 
      if(ev instanceof FileRemote.CmdEvent){  //event from extern
        return execCommission((FileRemote.CmdEvent)ev);
      } else {
        return 0;
      }
    }
    
    @Override public String toString(){ return "FileRemoteAccessorLocal - executerCommision"; }

  };
  

  
  /**The state machine for executing over some directory trees is handled in this extra class.
   * Note: the {@link Copy#Copy(FileAccessorLocalJava7)} needs initialized references
   * of {@link #singleThreadForCommission} and {@link #executerCommission}.
   */
  //private final FileRemoteCopy_NEW copy = new FileRemoteCopy_NEW();  
  
  private FileRemote workingDir;
  
  public FileAccessorLocalJava7() {
    //singleThreadForCommission.startThread();
    systemAttribtype = DosFileAttributes.class;
  }
  
  public void activate() {
    this.singleThreadForCommission.start();
  }
  
  
  
  /**Returns the singleton instance of this class.
   * Note: The instance will be created and the thread will be started if this routine was called firstly.
   * @return The singleton instance.
   */
  public static FileRemoteAccessor getInstance(){
    if(instance == null){
      ClassLoader classLoader = ClassLoader.getSystemClassLoader();
      try{ classLoader.loadClass("java.nio.file.Files");
        instance = new FileAccessorLocalJava7();
      } catch(ClassNotFoundException exc){
        //instance = new FileAccessorLocalJava6();  //use fallback strategy
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
  
  
  
  
  protected static void setAttributes(FileRemote fileRemote, Path path, BasicFileAttributes attribs){
    FileTime fileTime = attribs.lastModifiedTime();
    long dateLastModified = fileTime.toMillis();
    long dateCreation = attribs.creationTime().toMillis();
    long dateLastAccess = attribs.lastAccessTime().toMillis();
    long length = attribs.size();
    int flags = FileRemote.mExist | FileRemote.mTested;
    if(attribs.isDirectory()){ flags |= FileRemote.mDirectory; }
    String sAbsPath = fileRemote.getAbsolutePath();
    try {
      Path linkedPath = path.toRealPath();
      boolean isSymbolicLink = linkedPath.compareTo(path)!=0;
      if(isSymbolicLink) {
        fileRemote.setSymbolicLinkedPath(linkedPath.toAbsolutePath().toString());
      } else {
        fileRemote.setCanonicalAbsPath(fileRemote.getAbsolutePath());
      }
    }catch(IOException exc){
      System.err.println("FileAccessorLocalJava7 - Problem on toRealPath; " + fileRemote.getAbsolutePath());
    }
    //symbolicLink is already detected by toRealPath, inclusively the Windows JUNCTION which are not regard by isSymbolicLink()
//    if(attribs.isSymbolicLink()){
//      try{
//        Path target = Files.readSymbolicLink(path);
//        fileRemote.setSymbolicLinkedPath(target.toAbsolutePath().toString());
//      }catch(IOException exc){
//        System.err.println("FileAccessorLocalJava7 - Problem on SymbolicLinkPath; " + fileRemote.getAbsolutePath());
//        fileRemote.setCanonicalAbsPath(fileRemote.getAbsolutePath());
//      }
//    } else {
//      fileRemote.setCanonicalAbsPath(fileRemote.getAbsolutePath());
//    }
    int flagMask = FileRemote.mExist | FileRemote.mTested | FileRemote.mDirectory;
    if(attribs instanceof DosFileAttributes){
      DosFileAttributes dosAttribs = (DosFileAttributes)attribs;
      flagMask |= FileRemote.mHidden | FileRemote.mCanWrite| FileRemote.mCanRead; 
      if(dosAttribs.isHidden()){ flags |= FileRemote.mHidden; }
      if(!dosAttribs.isReadOnly()){ flags |= FileRemote.mCanWrite; }
      if(attribs.isRegularFile()){ flags |= FileRemote.mCanRead; }
      //if(dosAttribs.canExecute()){ flags |= FileRemote.mExecute; }
    }
    fileRemote.internalAccess().setFlagBits(flagMask, flags);
    fileRemote.internalAccess().setLengthAndDate(length, dateLastModified, dateCreation, dateLastAccess);
  }
  
  
  

  
  /**Sets the file properties from the existing file on the device.
   * checks whether the file exists and set the {@link FileRemote#mTested} flag any time.
   * If the file exists, the properties of the file were set, elsewhere they were set to 0.
   * <br>
   * This operation creates a temporary thread to do this action if callback is given,
   * callback is invoked in this thread.
   * 
   * @see {@link org.vishia.fileRemote.FileRemoteAccessor#refreshFileProperties(org.vishia.fileRemote.FileRemote)}
   */
  @Override public void refreshFileProperties(final FileRemote fileRemote, final FileRemote.CallbackEvent callback) { 
    //Strategy: use an inner private routine which is encapsulated in a Runnable instance.
    // either run it locally or run it in an extra thread.
    // The new instance is necessary because it should store the both given references.
    // It is a cheap operation in Java inclusively the garbage of the instance.
    //
    Runnable thread = new RunRefresh(fileRemote, callback);
    if(callback == null){
      thread.run(); //run direct
    } else {
      Thread threadObj = new Thread(thread);    // the threadObj and thread is garbaged if run is finished.
      threadObj.start();                        //run in an extra thread, the caller doesn't wait.
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

  
  /* (non-Javadoc)
   * @see org.vishia.fileRemote.FileRemoteAccessor#getChildren(org.vishia.fileRemote.FileRemote, java.io.FileFilter)
   */
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
      FileSystem.mkDirPath(file);
      FileOutputStream stream = new FileOutputStream(file);
      return stream;
    } catch(FileNotFoundException exc){
      return null;
    }
    
  }
  

  
  @Override public WritableByteChannel openWrite(FileRemote file, long passPhase)
  { try{ 
      FileSystem.mkDirPath(file);
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
      return fileLocal.delete();                           // access immediately the file system in this thread
    } else {
      boolean bOk = fileLocal.delete();          // also access immediately the file system in this thread
      if(bOk) {
        file._setProperties(0, 0, 0, 0, 0, null);;                          // file is no more existing, all clean
      }
      callback.occupy(evSrc, true);
      callback.sendEvent(bOk ? FileRemote.CallbackCmd.done : FileRemote.CallbackCmd.errorDelete );
      return bOk;
    }
  }

  
  
  @Override public void copyChecked(FileRemote fileSrc, String pathDst, String nameModification, int mode, FileRemoteWalkerCallback callbackUser, FileRemoteProgressEvent timeOrderProgress)
  {
    //states.copyChecked(fileSrc, pathDst, nameModification, mode, callbackUser, timeOrderProgress);
    
  }

  
  
  
  
  
  
  
  @Override public String copyFile(FileRemote src, FileRemote dst, FileRemote.CallbackEvent callback) {
    String sError = null;
    if(callback == null) {
      try {
        Files.copy(src.path, dst.path, StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
      } 
      catch(Exception exc) {
        sError = org.vishia.util.ExcUtil.exceptionInfo("copyFile", exc, 0, 10).toString();
      }
    } else {
      FileRemote.CmdEvent evCmd = callback.getOpponent();
      if(evCmd.occupy(this.evSrc, this.execCopyFile, this.singleThreadForCommission, true)) {
        evCmd.filesrc = src;
        evCmd.filedst = dst;
        evCmd.sendEvent();
      } else {
        sError = "unexpected: evCmd is in use";
      }
    }
    return sError;
  }

  
  EventConsumer execCopyFile = new EventConsumer() {
    @Override public int processEvent ( EventObject evP ) {
      FileRemote.CmdEvent ev = (FileRemote.CmdEvent)evP;
      String sError = copyFile(ev.filesrc, ev.filedst, null);  // action and back event.    
      FileRemote.CallbackEvent callback = ev.getOpponent();
      callback.errorMsg = sError;
      callback.sendEvent(sError == null? FileRemote.CallbackCmd.done : FileRemote.CallbackCmd.error);
      return mEventConsumed;
    }
    
  };
  

  
  
  @Override public String moveFile(FileRemote src, FileRemote dst, FileRemote.CallbackEvent callback) {
    if(callback == null) {
      String sError;
      try {
        Files.move(src.path, dst.path, StandardCopyOption.REPLACE_EXISTING);
        sError = null;
      } 
      catch(Exception exc) {                  // not moved
        sError = org.vishia.util.ExcUtil.exceptionInfo("moveFile", exc, 0, 10).toString();
      }
      return sError;                          // return null if success
    } else {
      Runnable run = new Runnable() {
        @Override public void run () {
          String sError = moveFile(src, dst, null);
          callback.errorMsg = sError;
          callback.sendEvent(sError == null? FileRemote.CallbackCmd.done : FileRemote.CallbackCmd.error);
        }
      };
      Thread thread = new Thread(run, "FileAcc-moveFile " + src.getName());
      thread.start();
      return null;
    }
  }

  
  @Override public void search(FileRemote fileSrc, byte[] search, FileRemoteWalkerCallback callbackUser, FileRemoteProgressEvent timeOrderProgress) {
    //TODO
  }


  
  @Override public boolean isLocalFileSystem()
  {  return true;
  }

  @Override public CharSequence getStateInfo(){ return "no stateInfo"; } //states.getStateInfo(); }
  
  @Override public void abortAll ( ) {
//    this.states.abortAllOrders();
  }
  
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
      cmdEvent1 = new FileRemote.CmdEvent("FileLocalAccessor-cmd-temp", this.evSrc, this.executerCommission, this.singleThreadForCommission, (FileRemote.CallbackEvent)evBack);
    }
    return  cmdEvent1; 
  }
  
  
  /**Executes the given event as commission.
   * @param commission
   * @return Some bits defined in {@link StateSimple}, 
   *   especially from here {@link StateSimple#mEventConsumed} and {@link StateSimple#mEventDonotRelinquish}.
   *   The last one is identically with  {@link EventConsumer.mEventDonotRelinquish}
   *   and is set, if this event is forwarded to the #theThreaad of this state machine.    
   */
  int execCommission(FileRemote.CmdEvent commission){
    int ret = 0;
    FileRemote.Cmd cmd = commission.getCmd();
    switch(cmd){
      case check: //copy.checkCopy(commission); break;
      case abortAll:     //should abort the state machine!
      case delChecked:
      case moveChecked:
//      case copyChecked: 
//        ret = this.states.statesCopy.processEvent(commission); break;
//      case move: ret = 0; this.states.execMove(commission); break;  //TODO this was never run.
      case chgProps:  execChgProps(commission); break;
      case chgPropsRecurs:  execChgPropsRecurs(commission); break;
      case countLength:  execCountLength(commission); break;
      case delete:  execDel(commission); break;
      case mkDir: mkdir(false, commission); break;
      case mkDirs: mkdir(true, commission); break;
    }
    return ret;
  }
  
  
  
  
  
  private void execChgProps(FileRemote.CmdEvent co){
    FileRemote dst;
    //FileRemote.FileRemoteEvent callBack = co;  //access only 1 time, check callBack. co may be changed from another thread.
    boolean ok = co !=null;
    if(co.newName() !=null && ! co.newName().equals(co.filesrc().getName())){
      File fileRenamed = new File(co.filesrc.getParent(), co.newName());
      ok &= co.filesrc.renameTo(fileRenamed);
      dst = FileRemote.fromFile(co.filesrc.itsCluster, fileRenamed);
      dst.refreshProperties(null);
    } else {
      dst = co.filesrc;
    }
    ok = chgFile(dst, co.maskFlags(), co.newFlags(), ok);
    long date =co.newDate();
    if(date !=0) {
      ok &= dst.setLastModified(date);
    }
    FileRemote.CallbackCmd cmd;
    if(ok){
      cmd = FileRemote.CallbackCmd.done; 
    } else {
      cmd = FileRemote.CallbackCmd.nok; 
    }
    FileRemote.CallbackEvent evback = co.getOpponent();    // the back event should be occupied already.
    if(!evback.isOccupied()) {
      evback.occupy(this.evSrc, dst, true);                // but then the action is not clarified....
    }
    evback.setFileSrc(dst);
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
      if((chg & mask & maskFlags)!=0){ 
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
  
  
  /**Uses the java.io.File
   * @param file
   * @param sum
   * @param recursion
   * @return
   */
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
  { this.singleThreadForCommission.close();
//    this.states.close();  
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
    
    public void run() {///
      String sPath = fileRemote.getAbsolutePath();
      Path pathfile = Paths.get(sPath);
      try{
        BasicFileAttributes attribs = Files.readAttributes(pathfile, systemAttribtype);
        setAttributes(fileRemote, pathfile, attribs);
      }catch(IOException exc){
        fileRemote.internalAccess().clrFlagBit(FileRemote.mExist);
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
          if(debugout) System.out.println("FileAccessorLocalJava7.refreshFilePropertiesAndChildren - start listFiles; dt=" + (time1 - time));
          
          File[] files = fileLocal.listFiles();
          time1 = System.currentTimeMillis();
          if(debugout) System.out.println("FileAccessorLocalJava7.refreshFilePropertiesAndChildren - ok listFiles; dt=" + (time1 - time));
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
                  child = fileRemote.internalAccess().newChild(name1, 0, 0,0,0, flags, file1); 
                  //child.refreshProperties(null);    //should show all sub files with its properties, but not files in sub directories.
                } else {
                  if(!child.isTested(time - 1000)){
                    //child.refreshProperties(null);    //should show all sub files with its properties, but not files in sub directories.
                  }
                }
                fileRemote.internalAccess().putNewChild(child);
              }
              //oldChildren contains yet removed files.
              if(debugout) System.out.println("FileAccessorLocalJava7.refreshFilePropertiesAndChildren - ok refresh; " + files.length + " files; dt=" + (System.currentTimeMillis() - time));
            }
          }
        }
        fileRemote.timeChildren = System.currentTimeMillis();
        if(callback !=null){
          callback.occupy(evSrc, true);
          long time1 = System.currentTimeMillis();
          if(debugout) System.out.println("FileAccessorLocalJava7.refreshFilePropertiesAndChildren - callback listFiles; dt=" + (time1 - time));
          callback.sendEvent(FileRemote.CallbackCmd.done);
          time1 = System.currentTimeMillis();
          if(debugout) System.out.println("FileAccessorLocalJava7.refreshFilePropertiesAndChildren - finish listFiles; dt=" + (time1 - time));
        }
        fileRemote.internalAccess().clrFlagBit(FileRemote.mThreadIsRunning);
      }
      catch(Exception exc){
        System.err.println("FileAccessorLocalJava7.refreshFilePropertiesAndChildren - Thread Excpetion;" + exc.getMessage());
      }
    }
  }
    

  
  
  /**Routine for walk through all really files of the file system for PC file systems and Java7 or higher. 
   * It calls {@link Files#walkFileTree(Path, Set, int, FileVisitor)} in an extra thread.
   * defined in {@link FileRemoteAccessor#walkFileTree(FileRemote, boolean, boolean, boolean, String, long, int, FileRemoteWalkerCallback)} 
   */
  @Override public void walkFileTree(FileRemote startDir, final boolean bWait, boolean bRefreshChildren, boolean resetMark
      , String sMask, long bMarkCheck, int depth, FileRemoteWalkerCallback callback, FileRemoteProgressEvent progress)
  { if(bWait){
      //execute it in this thread, therewith wait for success.
      walkFileTreeExecInThisThread(startDir, bRefreshChildren, resetMark, sMask, bMarkCheck, depth, callback, progress);
    } else {
      //creates a new Thread with instance of FileWalkerThread for the run routine and the arguments saving:
      FileRemoteAccessor.FileWalkerThread thread = new FileRemoteAccessor.FileWalkerThread(startDir, bRefreshChildren, resetMark, depth, sMask, bMarkCheck, callback) {
        @Override public void run() {
          try{
            FileAccessorLocalJava7.this.walkFileTreeExecInThisThread(startDir, bRefresh, resetMark, sMask, bMarkCheck, depth, callback, progress);
              if(progress !=null && progress.getDstThread() !=null) {
                progress.bDone = true;
                progress.sendEvent(); //activateDone();                 // remove the progress event from the timer queue, sends done to the same thread.
              }
            } 
          catch(Exception exc){
            CharSequence text = Assert.exceptionInfo("FileAccessorLocalJava7 - RefreshThread Exception; ", exc, 0, 20, true);
            System.err.println(text);
          }
        }
      };
      thread.setPriority(Thread.MIN_PRIORITY +1);
      thread.start();
    }
  }


  
  
  /**See {@link #walkFileTree(FileRemote, boolean, boolean, boolean, String, long, int, FileRemoteWalkerCallback)}, inner routine.
   * @param startDir
   * @param bRefreshChildren if true than gets all files in a directory and builds the {@link FileRemote#children()} newly.
   * @param resetMark true than removes all mark bits in {@link FileRemote#mark}
   * @param sMask selection mask.
   * @param markMask Bit to be set to mark a file in its 
   * @param depth Depth of walking through the directory tree. If <=0 then walk through all levels. >0 limited walk.
   *   if <0 then only marked files and directories with {@link FileMark#select} or {@link FileMark#selectSomeInDir} 
   *   in the first sub directory level are processed and checked. This is to handle pre-selected files of one level.
   * @param callback invoked for any directory entry and finsih and for any file.
   */
  protected void walkFileTreeExecInThisThread(FileRemote startDir, boolean bRefreshChildren, boolean resetMark, String sMask, long bMarkCheck, int depth
      , FileRemoteWalkerCallback callback, FileRemoteProgressEvent progress)
  {
    if(callback !=null) { callback.start(startDir); }
//    String sPath = startDir.getAbsolutePath();
//    if(FileSystem.isRoot(sPath))
//      Assert.stop();
//    java.nio.file.Path pathdir = java.nio.file.Paths.get(sPath);
    if(bRefreshChildren) { // && filter == null) {
      startDir.internalAccess().newChildren();
    }
    int depth1;
    if(depth ==0){ depth1 = Integer.MAX_VALUE; }
    else if(depth < 0){ depth1 = -depth; }
    else { depth1 = depth; }

    WalkFileTreeVisitor visitor = new WalkFileTreeVisitor(startDir.itsCluster, bRefreshChildren, resetMark, sMask
        , bMarkCheck, callback, progress);
    Set<FileVisitOption> options = new TreeSet<FileVisitOption>();
    try{ 
      java.nio.file.Files.walkFileTree(startDir.path, options, depth1, visitor);  
    } catch(IOException exc){
      System.err.println("FileAccessorLocalData.walkFileTree - unexpected IOException; " + exc.getMessage() );
    }
    if(callback !=null) { callback.finished(startDir); }
    
  }
  
  
  

  /**This class is the general FileVisitor for the adaption layer to FileRemote.
   * It will be created on demand if any request is proceeded with the given {@link FileRemoteWalkerCallback} callback interface.
   * The callback {@link FileRemoteWalkerCallback#offerLeafNode(FileRemote)} and {@link FileRemoteWalkerCallback#offerParentNode(FileRemote)} 
   * is processed only for selected files and directories, 
   * see 4. and 5. parameter of {@link WalkFileTreeVisitor#WalkFileTreeVisitor(FileCluster, boolean, boolean, String, int, FileRemoteWalkerCallback)}
   * <br><br>
   * <b>FileRemote instance delivered</b>:<br>
   * On callback anytime a FileRemote instance is delivered which wraps the operation systems file. 
   * The instance of FileRemote is gotten or created and stored from/to the {@link FileCluster}. 
   * If any parent of this file will be found in the FileCluster the FileRemote is stored in the {@link FileRemote#children()}. 
   * The FileRemote instance is refreshed with the information from the file on the operation system. The {@link FileRemote#getParent()} is set
   * and the instance is added as child of the parent. Anyway the same instance of FileRemote is used for the same file path. 
   * Therefore the FileRemote instance can be used to mark something on this file for this application.
   * 
   *
   */
  protected class WalkFileTreeVisitor implements FileVisitor<Path>
  {
    
    
    
    /**Data chained from a first parent to deepness of dir tree for each level.
     * This data are created while {@link FileAccessorLocalJava7#walkFileTree(FileRemote, FileFilter, int, FileRemoteWalkerCallback)} runs.
     * It holds the gathered children from the walker. The children are stored inside the {@link #dir}
     * only on {@link WalkFileTreeVisitor#postVisitDirectory(Path, IOException)}
     */
    private class CurrDirChildren{
      /**The directory of the level. */
      FileRemote dir;
      
      /**This is the sum of all files length independent of mask seen on refresh.
       * It will be stored in {@link FileRemote#length()} for the {@link #dir}.*/
      long nrBytesInDir;
      
      /**This is the sum of all files length which are selected.
       * It will be stored in {@link FileRemote#mark()} for the {@link #dir}, there in {@link FileMark#nrofBytesSelected}. */
      long nrBytesInDirSelected;
      
      int nrofFilesSelected;
      
      /**Current level of the file path filter. */
      FilepathFilterM fileFilter;
      
      int levelProcessMarked;
      
      /**parallel structure of all children.
       * The child entries are gotten from the dir via {@link FileCluster#getFile(CharSequence, CharSequence, boolean)}. It means, existing children
       * are gotten from the existing {@link FileRemote} instances. They are written in this map while walking through the directory.
       * After walking, in {@link WalkFileTreeVisitor#postVisitDirectory(Path, IOException)}, the {@link #dir}.{@link FileRemote#children()}
       * are replaced by this instance because it contains only existing children. {@link FileRemote} instances for non existing children are removed then.
       */
      //Map<String,FileRemote> children;
      /**The parent. null on first parent. */
      CurrDirChildren parent;
      
      CurrDirChildren(FileRemote dir, CurrDirChildren parent){
        this.dir = dir; this.parent = parent;
        this.levelProcessMarked = (parent == null) ? 0: parent.levelProcessMarked -1;
        if(refresh){
          //children = FileRemote.createChildrenList(); //new TreeMap<String,FileRemote>();
        }
      }
    }
    
    final FileCluster fileCluster;
    final boolean refresh, resetMark;
    final FileRemoteWalkerCallback callback;
    
    /**Information to the current level of walking. 
     * 
     */
    private CurrDirChildren curr;
    
    final int markCheck;
    
    //FilepathFilter mask;
    
    final FileRemoteProgressEvent progress;
    
    final TreeWalkerPathCheck checker;
    
    
    /**Constructs the instance.
     * @param fileCluster The cluster where all FileRemote are able to found by its path.
     * @param refreshChildren true then refreshes the FileRemote which are processed 
     * @param resetMark true then resets a {@link FileRemote#resetMarked(int)} of any processed file and directory.
     * @param sMask A mask "path/ ** /subdir/pre*post"
     * @param bMarkCheck Bits 31..0 to select marked files, Bits 63..32: Number of levels to process this check, 
     *   especially 2 (0x200000000L) if marked files in a directory should be checked.
     * @param levelProcessMarked
     * @param markCheck
     * @param callback Callback interface to the user.
     */
    public WalkFileTreeVisitor(FileCluster fileCluster, boolean refreshChildren, boolean resetMark, String sMask
        , long bMarkCheck
        , FileRemoteWalkerCallback callback, FileRemoteProgressEvent progress)
    {
      this.fileCluster = fileCluster;
      this.refresh = refreshChildren;
      this.resetMark = resetMark;
      this.markCheck = (int)(bMarkCheck & 0xffffffff);
      this.progress = progress;
      this.checker = sMask == null ? null : new TreeWalkerPathCheck(sMask);
      this.callback = callback;
      curr = new CurrDirChildren(null, null);  //starts without parent.
      curr.levelProcessMarked = (int)(bMarkCheck >>32); // levelProcessMarked;
      //mask = new FilepathFilter(sMask);
      reset();
    }

    private FileVisitResult translateResult(FileRemoteWalkerCallback.Result result){
      FileVisitResult ret;
      switch(result){
        case cont: ret = FileVisitResult.CONTINUE; break;
        case skipSiblings: ret = FileVisitResult.SKIP_SIBLINGS; break;
        case skipSubtree: ret = FileVisitResult.SKIP_SUBTREE; break;
        case terminate: ret = FileVisitResult.TERMINATE; break;
        default: ret = FileVisitResult.TERMINATE;
      }
      return ret;      
    }
    
    
    private void reset(){ } //if(this.progress !=null) { this.progress.clear(); } }
    
    
    
    /**Invoke if the depths does not reached the end on any directory, independent whether it is empty or not.
     * @see java.nio.file.FileVisitor#preVisitDirectory(java.lang.Object, java.nio.file.attribute.BasicFileAttributes)
     */
    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
        throws IOException
    {
      Path namepath = dir.getFileName();
      String name = namepath == null ? "/" : namepath.toString();
      SortedTreeWalkerCallback.Result result;
      if( this.checker !=null && (result = this.checker.offerParentNode(name)) != SortedTreeWalkerCallback.Result.cont) {
        return translateResult(result);  //usual skipSubtree.
      }
      final FileRemote dir1;
      if(this.curr.dir !=null) { 
        dir1 = this.curr.dir.subdir(name);              // get or create a child in FileRemote
      } else {                                             // first time:
        String sDir = dir.toString();         // get directory from nio.file.Path
        dir1 = this.fileCluster.getDir(sDir);      // and gets a new directory
      }

      if(this.curr.levelProcessMarked ==1 && (dir1.getMark() & this.markCheck)==0) {  //If it is the check level, check directories too.
        return FileVisitResult.SKIP_SUBTREE;      
      } else { //enter in directory always if curr.levelProcessMarked !=1
        if(this.resetMark && this.curr.levelProcessMarked <= 0){ 
          dir1.resetMarked(0xffffffff); 
        }
        setAttributes(dir1, dir, attrs);                   // copy the file attributes from nio.file..Path to FileRemote
        if(this.refresh && this.curr !=null){                        // yet mRefreshChildPending no more pending
          dir1.internalAccess().clrFlagBit(FileRemote.mRefreshChildPending);
          //curr.children.put(name, dir1);
        }
        result = (this.callback !=null) ? this.callback.offerParentNode(dir1) : SortedTreeWalkerCallback.Result.cont;
        if(result == SortedTreeWalkerCallback.Result.cont){
          this.curr = new CurrDirChildren(dir1, this.curr);
          if(FileAccessorLocalJava7.this.debugout) System.out.println("FileRemoteAccessorLocalJava7 - callback - pre dir; " + this.curr.dir.getAbsolutePath());
        } else {
          if(FileAccessorLocalJava7.this.debugout) System.out.println("FileRemoteAccessorLocalJava7 - callback - pre dir don't entry; " + this.curr.dir.getAbsolutePath());
        }
        return translateResult(result);
      }
    }

    
    
    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc)
        throws IOException
    { 
      if( this.checker !=null) {
        this.checker.finishedParentNode(this.curr.dir.getName()) ;
      }
      if(this.refresh){  
        //no: curr.dir.internalAccess().setChildren(curr.children);  //Replace the map.
        //thread safety: The children which are marked with mRefreshChildPending are removed.
        //If this mark is set in another thread too because the same directory should be refreshed in another thread
        //then children are removed which are existing and not to remove.
        //Only one thread should done this action.
        //The setChildrenRefreshed() is called yet (2015-11-13) before  the callback.finishedParentNode(...) is called
        //because that call invokes refresh the second time.
        this.curr.dir.timeChildren = System.currentTimeMillis();
        this.curr.dir.internalAccess().setChildrenRefreshed();  // first called before callback.finishedParentNode see above
        this.curr.dir.internalAccess().setLengthAndDate(this.curr.nrBytesInDir, -1, -1, System.currentTimeMillis());
      }
      if(this.curr.nrofFilesSelected >0) {
        FileMark mark = this.curr.dir.getCreateMark();
        mark.nrofBytesSelected = this.curr.nrBytesInDirSelected;
        mark.nrofFilesSelected = this.curr.nrofFilesSelected;
      }
//      if(curr.cnt.nrofParentSelected == curr.cnt.nrofParents && curr.cnt.nrofLeafSelected == curr.cnt.nrofLeafss){
//        if(curr.parent !=null) { curr.parent.cnt.nrofParentSelected +=1; }
//        cntTotal.nrofParentSelected +=1;
//      }
      
      if(this.progress !=null) {
        this.progress.nrDirProcessed +=1;
        this.progress.nrofBytesAll += this.curr.nrBytesInDir;
        this.progress.nrFilesProcessed += this.curr.dir.children().size();
      }
      synchronized(this) { try{ wait(10);} catch(InterruptedException exc1) {}}

      FileRemoteWalkerCallback.Result result = (this.callback !=null) ? 
                                         this.callback.finishedParentNode(this.curr.dir) 
                                       : SortedTreeWalkerCallback.Result.cont;
      if(FileAccessorLocalJava7.this.debugout) System.out.println("FileRemoteAccessorLocalJava7 - callback - post dir; " + this.curr.dir.getAbsolutePath());
      this.curr = this.curr.parent;
//      if(curr !=null) { curr.cnt.nrofParents +=1; } 
      return translateResult(result);
    }

    
    
    /**This method is invoked for directories instead {@link #preVisitDirectory(Path, BasicFileAttributes)}
     * if the depth of the tree is reached. Only then the Path is a directory. 
     * This method is not invoked if {@link #preVisitDirectory(Path, BasicFileAttributes)} is invoked for the Path. 
     * @see java.nio.file.FileVisitor#visitFile(java.lang.Object, java.nio.file.attribute.BasicFileAttributes)
     */
    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
        throws IOException
    {
      String name = file.getFileName().toString();
      if(name.startsWith("Byte"))
        Debugutil.stop();
      boolean selected = //mask.checkName(name);
        this.checker ==null ? true : this.checker.offerLeafNode(name, null) == SortedTreeWalkerCallback.Result.cont;
      if(FileAccessorLocalJava7.this.debugout) System.out.println("FileRemoteAccessorLocalJava7 - callback - file; " + name);
      FileRemote fileRemote;
      if(attrs.isDirectory()) { //only if the depth is reached, don't use it. 
//        curr.cnt.nrofParents +=1; 
//        cntTotal.nrofParents +=1;
      } else { 
//        curr.cnt.nrofLeafss +=1; 
//        cntTotal.nrofLeafss +=1;
      }
      if(this.curr.dir !=null) { 
        if(attrs.isDirectory()) { 
          fileRemote = this.curr.dir.subdir(name);              // get or create a child in FileRemote
        } else {
          fileRemote = this.curr.dir.child(name);
        }
      } else {                                             // first time:
        assert(false);  //starts always with a directory!
        String sDir = file.getParent().toString();         // get directory from nio.file.Path
        fileRemote = this.fileCluster.getFile(sDir, name);      // and gets a new directory
      }
      if(this.curr.levelProcessMarked >0 && (fileRemote.getMark() & this.markCheck)==0) {
        //If only marked files should be processed, but this file is not marked:
        //do nothing with the file, but continue:
        return FileVisitResult.CONTINUE;  //but does nothing with the file.      
      } else {
        if(this.resetMark){ 
          fileRemote.resetMarked(0xffffffff); }
        setAttributes(fileRemote, file, attrs);            // copy the file attributes from nio.file..Path to FileRemote
        long size = attrs.size();
        assert(this.curr.dir == fileRemote.getParentFile());
        this.curr.nrBytesInDir += size;
        if(selected) {
          this.curr.nrBytesInDirSelected += size;
          this.curr.nrofFilesSelected +=1;
        }
        FileRemoteWalkerCallback.Result result;
        if(this.callback !=null && this.callback.shouldAborted()){
          //only if a manual abort comes from the callback.
          result = SortedTreeWalkerCallback.Result.terminate;
        } else {
          if(this.refresh){
            //if(curr.children !=null) { curr.children.put(name, fileRemote); }
            fileRemote.internalAccess().clrFlagBit(FileRemote.mRefreshChildPending);
            fileRemote.internalAccess().setRefreshed();
    
          }
          if(this.callback !=null) {
            //check mask:
            if(selected) {
              if(attrs.isDirectory()) { 
//                curr.cnt.nrofParentSelected +=1; 
//                cntTotal.nrofParentSelected +=1;
              } else { 
//                curr.cnt.nrofLeafSelected +=1; 
//                cntTotal.nrofLeafSelected +=1;
              }
//              curr.cnt.nrofBytes += size;
//              cntTotal.nrofBytes += size;
              result = this.callback.offerLeafNode(fileRemote, null);
            } else {
              result = SortedTreeWalkerCallback.Result.cont;
            }
          } else { 
            result = SortedTreeWalkerCallback.Result.cont;
          }
        }
        return translateResult(result);
      }
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc)
        throws IOException
    {
      return FileVisitResult.CONTINUE;
    }
 
  }
  
  
  
  /**Access selector which uses {@link FileAccessorLocalJava7} for any path.
   * It is the standard for normal PC programs.
   * 
   */
  public static FileRemote.FileRemoteAccessorSelector selectLocalFileAlways = new FileRemote.FileRemoteAccessorSelector() {
    @Override public FileRemoteAccessor selectFileRemoteAccessor(String sPath) {
      return FileAccessorLocalJava7.getInstance();
    }
  };

  
}
