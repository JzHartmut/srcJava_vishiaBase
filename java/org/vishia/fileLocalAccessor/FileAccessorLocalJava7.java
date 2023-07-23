package org.vishia.fileLocalAccessor;

import java.io.Closeable;
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
import java.nio.file.LinkOption;
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

//import org.vishia.event.EventCmdtypeWithBackEvent;
import org.vishia.event.EventConsumer;
import org.vishia.event.EventConsumerAwait;
import org.vishia.event.EventSource;
import org.vishia.event.EventThread_ifc;
import org.vishia.event.EventTimerThread;
import org.vishia.event.EventTimerThread_ifc;
import org.vishia.event.EventWithDst;
import org.vishia.event.TimeOrder;
import org.vishia.fileRemote.FileCluster;
import org.vishia.fileRemote.FileMark;
import org.vishia.fileRemote.FileRemote;
import org.vishia.fileRemote.FileRemoteAccessor;
import org.vishia.fileRemote.FileRemoteCmdEventData;
import org.vishia.fileRemote.FileRemoteWalkerCallback;
import org.vishia.fileRemote.FileRemoteProgressEvData;
import org.vishia.fileRemote.FileRemoteTestCallback;
import org.vishia.fileRemote.FileRemoteWalker;
import org.vishia.util.Debugutil;
import org.vishia.util.FileFunctions;
import org.vishia.util.FilepathFilterM;
import org.vishia.util.FileSystem;
import org.vishia.util.SortedTreeWalkerCallback;

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
public final class FileAccessorLocalJava7 extends FileRemoteAccessor {
  
  /**Version, history and license.
   * <ul>
   * <li>2023-07-22 ctor is private, should never called directly, only singleton. 
   * <li>2023-07-22 experience with mkdir, refactored. 
   * <li>2023-07-18 rename and using {@link FileRemoteWalker.WalkInfo} instead CurrDirChildren, the content is the same.
   * <li>2023-07-16 change of {@link #delete(FileRemote, EventWithDst)} but this is now obsolete. 
   *   Improve {@link #execDel(org.vishia.fileRemote.FileRemoteCmdEventData, EventWithDst)}. 
   * <li>2023-07-16 {@link WalkInfo} is now subclass ot this, used for walkInfo in {@link FileCallbackLocalDelete},
   *   but should be renamed to WalkInfo.
   * <li>2023-07-16 {@link WalkFileTreeVisitor#preVisitDirectory(Path, BasicFileAttributes)} now set the first level always to selected
   *   but does not call callback. The first level is the entry, not to handle for the functionality. See comment there.
   *   Adequate for {@link WalkFileTreeVisitor#postVisitDirectory(Path, IOException)}.  
   * <li>2023-04-06 Hartmut new: chg: {@link #execCmd(org.vishia.fileRemote.FileRemoteCmdEventData, EventWithDst)} now also with copyFile, moveFile
   * <li>2023-02-21 some fine tuning 
   * <li>2023-02-13 Hartmut new: {@link WalkFileTreeVisitor#debugOut } as helper.
   * <li>2023-02-12 {@link #walkFileTree(FileRemote, boolean, boolean, int, int, String, long, int, FileRemoteWalkerCallback, FileRemoteProgressEvData)}
   *   with selection via mask, used for copy of selected files. Additional: mark during walk. 
   * <li>2023-02-03 Hartmut chg: experience with Thread priority. 
   *   It seems to be that the walker has generally a higher priority,  it is not proper interuptable by the SWT graphic thread ??
   *   Yet wait(10) after each directory in {@link WalkFileTreeVisitor#postVisitDirectory(Path, IOException)}
   *   to allow the graphic thread working. 
   * <li>2023-02-03 Hartmut chg: the  {@link #walkFileTreeExecInThisThread(FileRemote, boolean, boolean, String, long, int, FileRemoteWalkerCallback, FileRemoteProgressEvData)}
   *   is called recursively by {@link org.vishia.fileLocalAccessor.FileCallbackLocalCmp#offerParentNode(FileRemote)}.
   *   Hence it is bad to set <code>progress.bDone = true;</code> in this operation, it kills the progress visibility
   *   because it sets to bDone after a sub directory. It is shifted to {@link #walkFileTree(FileRemote, boolean, boolean, boolean, String, long, int, FileRemoteWalkerCallback, FileRemoteProgressEvData)}
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
   * <li>2015-03-27 Hartmut now children in {@link WalkFileTreeVisitor.WalkInfo} is deactivate because not used before.
   *   A seldom error of twice instances for the same children of a directory was watched.  
   * <li>2014-12-21 Hartmut chg: The {@link WalkFileTreeVisitor.WalkInfo#children} is not used any more, the refreshing of children is done
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
  public static final String sVersion = "2023-07-16";

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
  EventTimerThread singleThreadForCommission;
  
  final WalkerThread[] walkerThread = new WalkerThread[3];
  
  
  
  /**Destination for all events which forces actions in the execution thread.
   * 
   */
  EventConsumer executerCommission = new EventConsumer(){
    @Override public int processEvent(EventObject ev) {
//      if(ev instanceof FileLocalAccessorCopyStateM.EventInternal){ //internal Event
//        return FileAccessorLocalJava7.this.states.statesCopy.processEvent(ev);
//      } else 
      if(ev instanceof EventWithDst){  //event from extern
        return execCommission((EventWithDst<FileRemoteCmdEventData, FileRemoteProgressEvData>)ev);
      } else {
        return 0;
      }
    }
    
    @Override public boolean awaitExecution ( long timeout, boolean cleanDone ) { return false; }

    @Override public String toString(){ return "FileRemoteAccessorLocal - executerCommision"; }

    @Override public EventThread_ifc evThread () {
      // TODO Auto-generated method stub
      return null;
    }

  };
  

  
  /**The state machine for executing over some directory trees is handled in this extra class.
   * Note: the {@link Copy#Copy(FileAccessorLocalJava7)} needs initialized references
   * of {@link #singleThreadForCommission} and {@link #executerCommission}.
   */
  //private final FileRemoteCopy_NEW copy = new FileRemoteCopy_NEW();  
  
  private FileRemote workingDir;
  
  /**Use {@link #getInstance()} to get the singleton instance.
   * 
   */
  private FileAccessorLocalJava7() {
    //super("FileAccessorLoacalJava7", null, null, null);
    //singleThreadForCommission.startThread();
    this.systemAttribtype = DosFileAttributes.class;
    this.singleThreadForCommission = new EventTimerThread("FileAccessor-local");
    this.singleThreadForCommission.start();
    for(int ix = 0; ix < this.walkerThread.length; ++ix) {
      this.walkerThread[ix] = new WalkerThread();
      //new Thread(this.walkerThread[ix], "walkerThread" + ix);
    }
  }
  
  
  @Override public void finalize ( ) {    //does not been called because #instance
    this.close();
  }
  
  public void activate() {
    //this.singleThreadForCommission.start();
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
  
  /**Returns a unique absolute path for the file regarding maybe tmp, home, environment variables etc.
   * It uses {@link FileFunctions#absolutePath(String, File)} to fulfill all.
   * @param path given path
   * @return path to get the file. 
   */
  @Override public CharSequence completeFilePath(CharSequence sPath) {
    return FileFunctions.absolutePath(sPath.toString(), null);
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
  
  
  
  
  /**Sets the real attributs.
   * @param fileRemote
   * @param path should be gotten as existing path, 
   * @param attribs
   */
  protected static void setAttributes(FileRemote fileRemote, Path path, BasicFileAttributes attribs){
    fileRemote.internalAccess().setPath(path);
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
  @Override public void refreshFileProperties(final FileRemote fileRemote, EventWithDst<FileRemoteProgressEvData,?> evBack) { 
    //Strategy: use an inner private routine which is encapsulated in a Runnable instance.
    // either run it locally or run it in an extra thread.
    // The new instance is necessary because it should store the both given references.
    // It is a cheap operation in Java inclusively the garbage of the instance.
    //
    Runnable thread = new RunRefresh(fileRemote, evBack);
    if(evBack == null){
      thread.run(); //run direct
    } else {
      Thread threadObj = new Thread(thread);    // the threadObj and thread is garbaged if run is finished.
      threadObj.start();                        //run in an extra thread, the caller doesn't wait.
    }
  }  
    

  
  //@Override 
//  public void XXXXrefreshFilePropertiesAndChildren(final FileRemote fileRemote, final FileRemoteProgressEvent callback){
//    //a temporary instance for the thread routine.
//    RunRefreshWithChildren thread = new RunRefreshWithChildren(fileRemote, callback);
//    //the method body:
//    if(callback == null){
//      thread.run(); //run direct
//    } else {
//      if((fileRemote.getFlags() & FileRemote.mThreadIsRunning) ==0) { //check whether another thread is running with this file.
//        fileRemote.internalAccess().setFlagBit(FileRemote.mThreadIsRunning);
//        Thread threadObj = new Thread(thread);
//        thread.time = System.currentTimeMillis();
//        threadObj.start(); //run in an extra thread, the caller doesn't wait.
//      } else {
//        System.err.println("FileRemoteAccessLocalFile.refreshFilePropertiesAndChildren - double call, ignored;");
////        callback.relinquish(); //ignore it.
//      }
//    }
//  }

  
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

  
  
  @Override public boolean createNewFile(FileRemote file, EventWithDst<FileRemoteProgressEvData,?> evBack) throws IOException{
    File file1;
    if(file.oFile() == null){
      file.setFileObject(file1 = new File(file.getAbsolutePath()));
    } else {
      file1 = (File) file.oFile();
    }
    return file1.createNewFile();
  }



  
  @Override public boolean mkdir(FileRemote dir, boolean subdirs, EventWithDst<FileRemoteProgressEvData,?> evBack){
    String sDir = dir.getAbsolutePath();
    Path pathdir = Paths.get(sDir);
    String sError;
    try {
      if(subdirs) {
        Files.createDirectories(pathdir);
      } else {
        Files.createDirectory(pathdir);
      }
      sError = null;
    } catch (IOException e) {
      sError = e.getMessage();
    }
    if(evBack != null){ 
      FileRemoteProgressEvData progress = evBack.data();
      //FileRemote.CmdEvent ev = prepareCmdEvent(500, evBack);
      progress.done(FileRemoteCmdEventData.Cmd.mkDir, sError);
      evBack.sendEvent("mkdir");
    }
    return sError == null;
  }

  

  
  

  
  
  @Override public boolean delete(FileRemote file, EventWithDst<FileRemoteProgressEvData,?> evBack){
    
    //Path path7 = file.path();
    File fileLocal = getLocalFile(file);
    final boolean bOk;
    if(fileLocal.exists()) {
//      if(fileLocal.isDirectory()) {
//        bOk = FileFunctions.rmdir(fileLocal);  // this is illegal, it is not defined for the File.delete() operation!
//      } else {
        bOk = fileLocal.delete();
//      }
    } else {
      bOk = true;
    }
    evBack.relinquish();  // not used yet.
    return bOk; 
    //Files.delete(path);
//    if(callback == null){
//      return fileLocal.delete();                           // access immediately the file system in this thread
//    } else {
//      boolean bOk = fileLocal.delete();          // also access immediately the file system in this thread
//      if(bOk) {
//        file._setProperties(0, 0, 0, 0, 0, null);;                          // file is no more existing, all clean
//      }
//      callback.occupy(evSrc, true);
//      callback.sendEvent(bOk ? FileRemote.CallbackCmd.done : FileRemote.CallbackCmd.errorDelete );
//      return bOk;
//    }
  }

  
  
  @Override public void copyChecked(FileRemote fileSrc, String pathDst, String nameModification, int mode, FileRemoteWalkerCallback callbackUser, FileRemoteProgressEvData timeOrderProgress)
  {
    //states.copyChecked(fileSrc, pathDst, nameModification, mode, callbackUser, timeOrderProgress);
    
  }

  
  
  
  
  
  
  
  protected static String copyFile(FileRemoteCmdEventData co, EventWithDst<FileRemoteProgressEvData,?> evBack) {
    String sError = null;
    try {
      Files.copy(co.filesrc().path(), co.filedst().path(), StandardCopyOption.COPY_ATTRIBUTES, StandardCopyOption.REPLACE_EXISTING);
    } 
    catch(Exception exc) {
      sError = org.vishia.util.ExcUtil.exceptionInfo("copyFile", exc, 0, 10).toString();
    }
    if(evBack != null) {
      evBack.data().done(FileRemoteCmdEventData.Cmd.copyFile, sError);
      evBack.sendEvent("copy");
    }
    return sError;
  }

  
  protected static String moveFile(FileRemoteCmdEventData co, EventWithDst<FileRemoteProgressEvData,?> evBack) {
    String sError = null;
    try {
      Files.move(co.filesrc().path(), co.filedst().path(), StandardCopyOption.REPLACE_EXISTING);
    } 
    catch(Exception exc) {
      sError = org.vishia.util.ExcUtil.exceptionInfo("copyFile", exc, 0, 10).toString();
    }
    if(evBack != null) {
      evBack.data().done(FileRemoteCmdEventData.Cmd.moveFile, sError);
      evBack.sendEvent("move");
    }
    return sError;
  }

  
  EventConsumer execCopyFile = new EventConsumerAwait(null) {
    @Override public int processEvent ( EventObject evP ) {
//      FileRemote.CmdEvent ev = (FileRemote.CmdEvent)evP;
//      String sError = copyFile(ev.filesrc, ev.filedst, null);  // action and back event.    
//      FileRemoteProgressEvent callback = ev.getOpponent();
//      callback.errorMsg = sError;
//      callback.sendEvent(sError == null? FileRemote.CallbackCmd.done : FileRemote.CallbackCmd.error);
      return mEventConsumed;
    }

    
  };
  

  
  
  
  @Override public void search(FileRemote fileSrc, byte[] search, FileRemoteWalkerCallback callbackUser, FileRemoteProgressEvData timeOrderProgress) {
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
   * @see org.vishia.fileRemote.FileRemoteAccessor#prepareCmdEvent(org.vishia.fileRemote.FileRemoteProgressEvent)
   */
//  @Override public FileRemote.CmdEvent prepareCmdEvent(int timeout, EventWithDst<?, FileRemote.CmdEvent>  evBack){
//    FileRemote.CmdEvent cmdEvent1;
//    if(evBack !=null && (cmdEvent1 = (FileRemote.CmdEvent)evBack.getOpponent()) !=null){
//      if(!cmdEvent1.occupy(timeout, evSrc, executerCommission, singleThreadForCommission)){
//        return null;
//      }
//    } else {
//      cmdEvent1 = new FileRemote.CmdEvent("FileLocalAccessor-cmd-temp", this.evSrc, this.executerCommission, this.singleThreadForCommission, (FileRemoteProgressEvent)evBack);
//    }
//    return  cmdEvent1; 
//  }
  
  
  /**Executes the given event as commission.
   * @param commission
   * @return Some bits defined in {@link StateSimple}, 
   *   especially from here {@link StateSimple#mEventConsumed} and {@link StateSimple#mEventDonotRelinquish}.
   *   The last one is identically with  {@link EventConsumer.mEventDonotRelinquish}
   *   and is set, if this event is forwarded to the #theThreaad of this state machine.    
   */
  int execCommission(EventWithDst<FileRemoteCmdEventData, FileRemoteProgressEvData> commission){
    int ret = 0;
    FileRemoteCmdEventData cmdData = commission.data();
    FileRemoteCmdEventData.Cmd cmd = cmdData.cmd();
    EventWithDst<FileRemoteProgressEvData, ?> evBack = commission.getOpponent();    // the back event should be occupied already.
    switch(cmd){
      case check: //copy.checkCopy(commission); break;
      case abortAll:     //should abort the state machine!
      case delChecked:
      case moveChecked:
//      case copyChecked: 
//        ret = this.states.statesCopy.processEvent(commission); break;
//      case move: ret = 0; this.states.execMove(commission); break;  //TODO this was never run.
      case chgProps:  execChgProps(cmdData, evBack); break;
      case chgPropsRecurs:  execChgPropsRecurs(cmdData, evBack); break;
      case countLength:  execCountLength(cmdData, evBack); break;
      case delete:  execDel(cmdData, evBack); break;
      case mkDir: mkdir(cmdData.filesrc(), false, evBack); break;
      case mkDirs: mkdir(cmdData.filesrc(), true, evBack); break;
    }
    return ret;
  }
  
  
  /**This is called in the {@link WalkerThread} or immediately from {@link #cmd(boolean, org.vishia.fileRemote.FileRemoteCmdEventData, EventWithDst)}
   * if first argument is true.
   * @param co
   * @param evBack
   * @return
   */
  protected String execCmd ( FileRemoteCmdEventData co, EventWithDst<FileRemoteProgressEvData,?> evBack) {
    String ret = null;
    switch(co.cmd()){
    case check: break; //copy.checkCopy(commission); break;
    case abortAll: break;      //should abort the state machine!
    case delChecked: break; 
    case moveChecked: break; 
//    case copyChecked: 
//      ret = this.states.statesCopy.processEvent(commission); break;
//    case move: ret = 0; this.states.execMove(commission); break;  //TODO this was never run.
    case chgProps:  execChgProps(co, evBack); break;
    case chgPropsRecurs:  execChgPropsRecurs(co, evBack); break;
    case countLength:  execCountLength(co, evBack); break;
    case delete:  execDel(co, evBack); break;
    case mkDir: mkdir(co.filesrc(), false, evBack); break;
    case mkDirs: mkdir(co.filesrc(), true, evBack); break;
    case copyFile: copyFile(co, evBack); break;
    case moveFile: moveFile(co, evBack); break;
    case walkRefresh: //also refreshs with selection, and mark functionality.
      assert(co.callback() == null);
      FileAccessorLocalJava7.this.walkFileTreeExecInThisThread(co, true, evBack , false); 
      break;
    case walkCopyDirTree:
      assert(co.callback() == null);
      co.setCallback(new FileCallbackLocalCopy(co.filedst(), null, evBack));  //evCallback);
      FileAccessorLocalJava7.this.walkFileTreeExecInThisThread(co, false, evBack , false); 
      break;
    case walkDelete:
      assert(co.callback() == null);
      co.setCallback(new FileCallbackLocalDelete(evBack));  //evCallback);
      FileAccessorLocalJava7.this.walkFileTreeExecInThisThread(co, false, evBack , false); 
      break;
    case walkCompare:
      assert(co.callback() == null);
      co.setCallback(new FileCallbackLocalCmp(co.filesrc(), co.filedst(), null, evBack));
      FileAccessorLocalJava7.this.walkFileTreeExecInThisThread(co, true, evBack , false); 
      break;
    case walkTest:
      assert(co.callback() == null);
      co.setCallback(new FileRemoteTestCallback());
      FileAccessorLocalJava7.this.walkFileTreeExecInThisThread(co, true, evBack , false); 
      break;
    default:
    }//switch
    return ret;
  }
  
  
  
  /**See {@link FileRemoteAccessor#cmd(boolean, org.vishia.fileRemote.FileRemoteCmdEventData, EventWithDst)}.
   * Hint: Set breakpoint to {@link #execCmd(org.vishia.fileRemote.FileRemoteCmdEventData, EventWithDst)}
   * to stop in the execution thread.
   */
  @Override public String cmd(boolean bWait, FileRemoteCmdEventData co, EventWithDst<FileRemoteProgressEvData,?> evBack) {
    if(bWait) {
      return execCmd(co, evBack);                       // execute in this thread.
    } else {
      String ret = "no thread free";
      for(WalkerThread th : this.walkerThread) {
        if(th.isFree()) {       
          //======>>>>    thread found, set break point in operation above!
          if(! th.setOrder(co, evBack)) {                 // set the order
            ret = "cannot set order, evBack is null";
          } else {
            ret = null;                                    // and it's all done in this thread
          }
          break;                                           // the order is executed
        }
      }
      return ret;
    }
  }
  
  
  private void execChgProps(FileRemoteCmdEventData co, EventWithDst<FileRemoteProgressEvData, ?> evBack){
    FileRemote dst;
    //FileRemote.FileRemoteEvent callBack = co;  //access only 1 time, check callBack. co may be changed from another thread.
    boolean ok = co !=null;
    if(co.newName() !=null && ! co.newName().equals(co.filesrc().getName())){
      dst = co.filesrc().getParentFile().child(co.newName());   // new file in the same directory
      //File fileRenamed = new File(co.filesrc.getParent(), co.newName());
      ok &= co.filesrc().renameTo(dst);              // call File#renameTo
      //dst = FileRemote.fromFile(co.filesrc.itsCluster, fileRenamed);
      dst.refreshProperties(null);
    } else {
      dst = co.filesrc();
    }
    ok = chgFile(dst, co.maskFlags(), co.newFlags(), ok);
    long date =co.newDate();
    if(date !=0) {
      ok &= dst.setLastModified(date);
    }
    FileRemoteProgressEvData.ProgressCmd cmd;
    if(ok){
      cmd = FileRemoteProgressEvData.ProgressCmd.done; 
    } else {
      cmd = FileRemoteProgressEvData.ProgressCmd.nok; 
    }
    if(!evBack.isOccupied()) {
      evBack.occupy(this.evSrc, true);                // but then the action is not clarified....
    }
    FileRemoteProgressEvData progress = evBack.data();
    progress.currFile = dst;
    progress.setAnswer(cmd);
    evBack.sendEvent("execChgProps");
  }
  
  
  private void execChgPropsRecurs(FileRemoteCmdEventData co, EventWithDst<FileRemoteProgressEvData, ?> evBack){
    FileRemote dst;
    boolean ok = co !=null;
    if(co.newName() !=null && ! co.newName().equals(co.filesrc().getName())){
      FileRemote fileRenamed = co.filesrc().getParentFile().child(co.newName());
      ok &= co.filesrc().renameTo(fileRenamed);
      dst = fileRenamed;
    } else {
      dst = co.filesrc();
    }
    ok &= chgPropsRecursive(dst, co.maskFlags(), co.newFlags(), ok, 0);
    FileRemoteProgressEvData.ProgressCmd cmd;
    if(ok){
      cmd = FileRemoteProgressEvData.ProgressCmd.done ; 
    } else {
      cmd = FileRemoteProgressEvData.ProgressCmd.error ; 
    }
    FileRemoteProgressEvData progress = evBack.data();
    progress.currFile = dst;
    progress.setAnswer(cmd);
    evBack.sendEvent("execChgPropsRecurs");
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
  
  
  
  private void execCountLength(FileRemoteCmdEventData co, EventWithDst<FileRemoteProgressEvData, ?> evBack){
    long length = countLengthDir(co.filesrc(), 0, 0);    
    FileRemoteProgressEvData.ProgressCmd cmd;
    FileRemoteProgressEvData progress = evBack.data();
    if(length >=0){
      cmd = FileRemoteProgressEvData.ProgressCmd.done; 
      progress.nrofBytesAll = length;
    } else {
      cmd = FileRemoteProgressEvData.ProgressCmd.nok; 
    }
    progress.currFile = co.filesrc();
    progress.setAnswer(cmd);
    evBack.sendEvent("execCountLength");
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
  
  
  
  /**Executes delete file maybe in an extra thread or really remote
   * @param co
   * @param evBack 
   */
  void execDel(FileRemoteCmdEventData co, EventWithDst<FileRemoteProgressEvData,?> evBack) {
    Path path7 = co.filesrc().path();
    String sError = null;
    try{ 
      Files.delete(path7);
      co.filesrc().internalAccess().setDeleted();
    } catch(IOException exc) {
      sError = exc.getMessage();
    }
    if(evBack !=null) {
      FileRemoteProgressEvData data = evBack.data();
      //data.answerToCmd
      data.currFile = co.filesrc();
      data.done(FileRemoteCmdEventData.Cmd.noCmd, sError);
      evBack.sendEvent(this);
    }
  }


  @Override public void close()
  { if(this.singleThreadForCommission !=null) { this.singleThreadForCommission.close(); }
    for(WalkerThread th: this.walkerThread) {
      if(th !=null) { th.bRun = false; }
    }
//    this.states.close();  
  }
  
  
  
  /**A thread which gets all file properties independent of a caller of the #re
   */
  private class RunRefresh implements Runnable{
    final FileRemote fileRemote;
    
    final EventWithDst<FileRemoteProgressEvData, ?> evBack;
    
    RunRefresh(final FileRemote fileRemote, EventWithDst<FileRemoteProgressEvData, ?> evBack){
      this.fileRemote= fileRemote;
      this.evBack = evBack;
    }
    
    public void run() {///
      String sPath = fileRemote.getAbsolutePath();
      String name = fileRemote.getName();
      Path pathfile = Paths.get(sPath);
      int x = 1;
//      try{
//        Path pDir = pathfile.getParent();        // yet not clarified whether it exists
        //useless: Path pFile = pDir.resolve(name);
//Path path = Paths.get(pDir);
      try {
        Path pathFileExists = pathfile.toRealPath(LinkOption.NOFOLLOW_LINKS);
        boolean bExists = Files.exists(pathFileExists);
        //        FileRemote rDir = FileRemote.get(pDir.toString());
//        if(!rDir.isTested()) {
//          rDir.refreshPropertiesAndChildren(true, null);
//        }
//        FileRemote rFile = rDir.getChild(name);  // it is completely refreshed because refreshing the parent.
        BasicFileAttributes attribs = Files.readAttributes(pathFileExists, FileAccessorLocalJava7.this.systemAttribtype);
        setAttributes(fileRemote, pathFileExists, attribs);
      }catch(IOException exc){
        fileRemote.internalAccess().clrFlagBit(FileRemote.mExist);
      }
      fileRemote.timeRefresh = System.currentTimeMillis();
      if(evBack !=null){
        FileRemoteProgressEvData progress = evBack.data();
        progress.setAnswer(FileRemoteProgressEvData.ProgressCmd.done);
        evBack.occupy(evSrc, true);
        evBack.sendEvent("RunFrefresh");
      }
    }
    
    
  }
  
    

  
  


  
  
  //tag::walkFileTreeExecInThisThread[]
  /**Executes walk file tree. Usual called in the {@link #execCmd(org.vishia.fileRemote.FileRemoteCmdEventData, EventWithDst)}
   * either in one of the {@link WalkerThread} or immediately in the caller thread.  
   * @param co data what should be done, especially {@link FileRemoteCmdEventData#callback} describes what should be done with a file.
   * @param bRefreshChildren true then reads the properties of all children from the original file system.
   * @param evBack a progress event, also usable for quests with answer via the {@link EventWithDst#getOpponent()} prepared before call.
   * @param debugOut
   */
  protected void walkFileTreeExecInThisThread(
      FileRemoteCmdEventData co
      , boolean bRefreshChildren
      , EventWithDst<FileRemoteProgressEvData, ?> evBack
      , boolean debugOut)
  {
    int progressFinish = EventConsumer.mEventConsumFinished;
    String sError = null;                        // for unexpected exception message
    try{ 
//      if(evWalker.progress !=null && evWalker.progress.timeOrder !=null) {
//        evWalker.progress.timeOrder.activateCyclic();     // timeOrder back event to inform
//      }
      if(co.callback() !=null) { co.callback().start(co.filesrc(), co); }
      if(bRefreshChildren) {                               // refreshChildren is for children in FileRemote instance
        co.filesrc().internalAccess().newChildren(); 
      }
      int depth1;
      if(co.depthWalk() ==0){ depth1 = Integer.MAX_VALUE; }
      else if(co.depthWalk() < 0){ depth1 = -co.depthWalk(); }
      else { depth1 = co.depthWalk(); }
      if(evBack !=null) {
        FileRemoteProgressEvData progress = evBack.data();
        progress.clean();       //cleans the payload for cummulate
        progress.answerToCmd = co.cmd();
        
      }
      WalkFileTreeVisitor visitor = new WalkFileTreeVisitor(co.filesrc().itsCluster, bRefreshChildren
          , co, evBack, debugOut);
      Set<FileVisitOption> options = new TreeSet<FileVisitOption>();
      //======>>>>                ----------------- call of the java.nio-walker
      //==========                ----------------- set breakpoints in visitFile etc. in the following class
      java.nio.file.Files.walkFileTree(co.filesrc().path(), options, depth1, visitor);  
      if(visitor.timeOrderProgress !=null ) { visitor.timeOrderProgress.deactivate(); }
    } catch(IOException exc){
      sError = org.vishia.util.ExcUtil.exceptionInfo("FileAccessorLocalJava7.walkFileTree - unexpected Exception; ", exc, 0, 20).toString();
      progressFinish = EventConsumer.mEventConsumerException;
    }
    if(co.callback() !=null) { 
      co.callback().finished(co.filesrc());               // callback for finish 
    }
    if(evBack !=null ) {                       // back event for finish
      FileRemoteProgressEvData progress = evBack.data();
      progress.done(co.cmd(), sError);
      evBack.sendEvent("walkFileTreeExecInThisThread-done");
    }
  }
  //end::walkFileTreeExecInThisThread[]
  
  


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
    
    
    
    
    /**Can be used internally to control outputs for debugging (printf). */
    public boolean debugOut;
    
    final FileCluster fileCluster;
    final boolean bRefresh; //, bResetMark;
    
    final FileRemoteCmdEventData co;
    
    final FileRemoteWalkerCallback callback;
    
    /**Information to the current level of walking. 
     * 
     */
    private FileRemoteWalker.WalkInfo walkInfo;
    
    /**If 0 do nothing. If not 0 check whether one of the bits are set in {@link FileRemote#mark()}
     * for selecting the file. 
     */
    //final int markCheck;
    
    /**If 0 do nothing. Else set or reset this bits in the {@link FileRemote#mark} of the file
     * Whether set or reset is controlled by {@link #bResetMark};
     */
    //final int markSet, markSetDir;
    
    //FilepathFilter mask;
    
    /**Received event for this action with some parameter. 
     * The event is hold till end of walking.
     */
    //final FileRemoteWalkerEvent ev;
    
    /**It is also aggregated in {@link #ev} */
    final EventWithDst<FileRemoteProgressEvData, ?> evBack;
    
    final FileRemoteProgressEvData progress;
    
    /**The time order is used to transmit a progress event after a given time,
     * to prevent too much traffic for fast walking. 
     * The current directory or file or progress in file is noted in this time order. 
     * If a new information comes, and the time order is not expired,
     * then the time order is hold and the informations are replaced with the new ones. 
     * If the progress event is in processing, the second progress event is used with a new timeOrder.
     * If both progress events are in processing, it means the processing hangs, 
     * then no more progress events are send. 
     */
    final TimeOrder timeOrderProgress;
    
    //final TreeWalkerPathCheck checker;
    final FilepathFilterM fileFilter;
    
    long startTime, lastTimeProgress;
    
    /**Constructs the instance.
     * @param fileCluster The cluster where all FileRemote are able to found by its path.
     * @param refreshChildren true then refreshes the FileRemote which are processed 
     * @param co data for the commission especially also the callback for each dir and file
     * @param evBack given event to be used for messages to the caller, free for use 
     * @param bDbg
     */
    public WalkFileTreeVisitor(FileCluster fileCluster, boolean refreshChildren
        , FileRemoteCmdEventData co
        , EventWithDst<FileRemoteProgressEvData, ?> evBack, boolean bDbg) {
      this.debugOut = bDbg;
      this.fileCluster = fileCluster;
      this.bRefresh = refreshChildren;
      this.co = co;
      //this.markSet = markSet;
      //this.markSetDir = markSetDir;
      this.fileFilter = co.selectFilter() == null ? null : FilepathFilterM.createWildcardFilter(co.selectFilter());
      //this.markCheck = (int)(bMarkCheck & 0xffffffff);
      this.callback = co.callback();
      //this.ev = ev;
      this.evBack = evBack;
      this.progress = evBack == null ? null : evBack.data();
      this.walkInfo = new FileRemoteWalker.WalkInfo(null, null, this.fileFilter);  //starts without parent.
      this.walkInfo.levelProcessMarked = 0; //(int)(bMarkCheck >>32); // levelProcessMarked;
      this.startTime = System.currentTimeMillis();
      //this.lastTimeProgress = this.startTime - evProgress.delay;
      if(co.cycleProgress() >0) {                  // progress only in cycles
        @SuppressWarnings("resource") EventThread_ifc timer = this.evBack.getDstThread();
        assert(timer instanceof EventTimerThread_ifc); //should refer a timer
        this.timeOrderProgress = new TimeOrder("progress", (EventTimerThread_ifc)timer, this.evBack);
      } else {
        this.timeOrderProgress = null;
      }
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
    
    
    
    /**Invoked from {@link java.nio.file.FileTreeWalker} if the depths does not reached the end of directory deepness, 
     * also called for empty directories.
     * It implements {@link java.nio.file.FileVisitor#preVisitDirectory(java.lang.Object, java.nio.file.attribute.BasicFileAttributes)}
     * <br><br>
     * It does create a level of {@link FileRemote.WalkInfo} as walk info for the current level.
     * This {@link FileRemote.WalkInfo} are nested or stacked via {@link FileRemote.WalkInfo#parent}, any level has its own
     * and go back to the first level on {@link #postVisitDirectory(Path, IOException)}.
     * <br><br>
     * The {@link #co} -> {@link FileRemoteCmdEventData#callback} -> {@link FileRemoteWalkerCallback#offerParentNode(FileRemote, Object, Object)}
     * is not called for the first level (!) because the first level is the original source directory which should not handled by itself,
     * only its content should be handled. Also the {@link FileRemoteCmdEventData#selectFilter} is valid only from the second level.
     * The first level is intrinsic selected because it is the calling source directory.
     * This is detected by evaluating {@link FileRemote.WalkInfo#parent} which is null for the first level.  
     */
    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
        throws IOException
    {
      final FileVisitResult ret;
      Path namepath = dir.getFileName();
      String name = namepath == null ? "/" : namepath.toString();
      SortedTreeWalkerCallback.Result result;
      boolean selected;
      final FilepathFilterM childFilter;
      if(this.walkInfo.parent ==null) {                        // the first level of preVisistDirectory, the given one
        selected = true;                                   // is always selected (elsewhere the operation will no t be called)
        childFilter = this.walkInfo.fileFilter;                // the fileFilter is effective from the next level
      } else if((this.co.selectMask() & FileMark.ignoreSymbolicLinks) !=0 &&  Files.isSymbolicLink(namepath)) {
        selected = false;                        // skip a directory which is a symbolic link if desired
        childFilter = null;
      } else if(this.fileFilter == null) {       // do not skip if no fileFilter given, because files may be marked
        selected = true; result = SortedTreeWalkerCallback.Result.cont;
        childFilter = null;
      } else {                                   // evaluate fileFilter, skip if no file is selected.
        childFilter = this.walkInfo.fileFilter.check(name, true); 
        selected = (childFilter != null); 
      }
      if( !selected && this.co.markSet() == 0      // somewhat to do else with the dir? 
       && ( this.co.selectMask() == 0 || (this.co.selectMask() & FileMark.orWithSelectString) ==0 )
        ) {
        return FileVisitResult.SKIP_SUBTREE;     // ====>> return skipSubtree, if not selected and no more to do 
      }
      //------------------------------------------- either selected or some to do:
      final FileRemote dir1;                     // get the FileRemote instance for the directory proper to this path
      if(this.walkInfo.dir !=null) { 
        dir1 = this.walkInfo.dir.subdir(name);       // get or create a child in FileRemote
      } else {                                   // first time:
        String sDir = dir.toString();            // get directory from nio.file.Path
        dir1 = FileRemote.getDir(sDir);          // and gets the root directory from file cluster
      }
      //------------------------------------------- If a co.selectMask is given, then the subdir should contain one of the bit.
      if(this.walkInfo.parent !=null && (this.co.selectMask() & FileMark.mSelectMarkBits) !=0) {
        boolean bMarkSelect = (dir1.getMark() & FileMark.mSelectMarkBits & this.co.selectMask()) !=0;
        if( (this.co.selectMask() & FileMark.orWithSelectString) !=0) {
          selected |= bMarkSelect;
        } else {
          selected &= bMarkSelect;
        }
      }                                          // if co.selectMask does not contain mSelectMarkBits, do nothing with it.
      //
      if(!selected) {                            // after this.co.selectMask still not selected
//        if(this.co.markSet !=0) {
//          if( (this.co.markSet & FileMark.resetMark) !=0) {
//            dir1.setMarked(this.co.markSet);
//          } else {
//            dir1.resetMarked(this.co.markSet);
//          }
//        }
        ret =  FileVisitResult.SKIP_SUBTREE;  //but does nothing with the file.      
      } else {
        ret = FileVisitResult.CONTINUE;
        //enter in directory always if curr.levelProcessMarked !=1
        if(this.walkInfo.parent !=null && this.co.markSet() !=0) {                // only reset a mark here, set only if files are marked.
          if( (this.co.markSet() & FileMark.resetMark) !=0) {
            dir1.resetMarked(this.co.markSet());
          } else {
            //NO: dir1.setMarked(this.co.markSet);
          }
        }
        setAttributes(dir1, dir, attrs);         // copy the file attributes from nio.file..Path to FileRemote
        if(this.bRefresh && this.walkInfo !=null){   // yet mRefreshChildPending no more pending
          dir1.internalAccess().clrFlagBit(FileRemote.mRefreshChildPending);
          //curr.children.put(name, dir1);
        }
        
        if(this.progress !=null) {                         
          //--------------------------------------- creates or updates a time order for the state. 
          if(this.timeOrderProgress !=null) { this.timeOrderProgress.hold(); }
          this.progress.progressCmd = FileRemoteProgressEvData.ProgressCmd.refreshDirPre;
          this.progress.nrDirProcessed +=1;
          this.progress.currDir = dir1;          // all information about the FileRemote will be proper serialized if remote
          if(this.co.cycleProgress() ==0) {        // send back event on any file or dir entry:
            this.evBack.sendEvent(this);             // evBack is associated to the progress
          } else {                               // send cyclically only informations about progress
            long timeEvent = System.currentTimeMillis() + this.co.cycleProgress();
            this.timeOrderProgress.activateAt(timeEvent, timeEvent); // activate a time order with delay, not too much traffic
            //this.progress.nrofBytesAll += this.curr.nrBytesInDir;
            //this.progress.nrFilesProcessed += this.curr.dir.children().size();
          }
        }
        FileRemoteWalker.WalkInfo currInfo = new FileRemoteWalker.WalkInfo(dir1, this.walkInfo, childFilter);
        if(this.walkInfo.parent !=null && this.callback !=null) {
          result = this.callback.offerParentNode(dir1, dir, currInfo);  // not for the entry level
        } else {
          result = SortedTreeWalkerCallback.Result.cont;
        }
        if(result == SortedTreeWalkerCallback.Result.cont){
          this.walkInfo = currInfo;                  // only store this currInfo if a new level is given, removed on postVisistDirectory
          if(this.debugOut) System.out.println("FileRemoteAccessorLocalJava7.walker - pre dir; " + this.walkInfo.dir.getAbsolutePath());
        } else {                                           
          // currInfo will be garbaged, not necessary
          if(this.debugOut) System.out.println("FileRemoteAccessorLocalJava7.walker - pre dir don't entry; " + this.walkInfo.dir.getAbsolutePath());
        }
        return translateResult(result);
      }
      return ret;
    }

    
    
    /**Invoked from {@link java.nio.file.FileTreeWalker} on end of walking through a directory. 
     * It implements {@link java.nio.file.FileVisitor#postVisitDirectory(Object, IOException)}
     * <br><br>
     * It does remove the current level of {@link FileRemote.WalkInfo} as walk info because it's the end of this level.
     * <br><br>
     * The {@link #co} -> {@link FileRemoteCmdEventData#callback} -> {@link FileRemoteWalkerCallback#finishedParentNode(FileRemote, Object, Object)}
     * is not called for the first level (!) (it's the last call) because the first level is the original source directory which should not handled by itself,
     * see adequate in preVisitDirectory(...).
     */
    @Override public FileVisitResult postVisitDirectory ( Path dir, IOException exc)
        throws IOException
    { 
      if(this.bRefresh){  
        //no: curr.dir.internalAccess().setChildren(curr.children);  //Replace the map.
        //thread safety: The children which are marked with mRefreshChildPending are removed.
        //If this mark is set in another thread too because the same directory should be refreshed in another thread
        //then children are removed which are existing and not to remove.
        //Only one thread should done this action.
        //The setChildrenRefreshed() is called yet (2015-11-13) before  the callback.finishedParentNode(...) is called
        //because that call invokes refresh the second time.
        this.walkInfo.dir.timeChildren = System.currentTimeMillis();
        this.walkInfo.dir.internalAccess().setChildrenRefreshed();  // first called before callback.finishedParentNode see above
        this.walkInfo.dir.internalAccess().setLengthAndDate(this.walkInfo.nrBytesInDir, -1, -1, System.currentTimeMillis());
      }
      if(this.walkInfo.nrofFilesSelected >0 && this.co.markSetDir() !=0 && (this.co.markSetDir() & FileMark.resetMark) ==0) {
        FileMark mark = this.walkInfo.dir.getCreateMark();
        mark.nrofBytesSelected = this.walkInfo.nrBytesInDirSelected;
        mark.nrofFilesSelected = this.walkInfo.nrofFilesSelected;
        mark.setMarked(this.co.markSetDir(), null);
      }
      
      synchronized(this) { try{ wait(10);} catch(InterruptedException exc1) {}}
      final FileRemoteWalkerCallback.Result result;
      if(this.walkInfo.parent !=null && this.walkInfo.parent.parent !=null //do not callback not for the exit level (first level directory) 
          && this.callback !=null
        ) { 
        result = this.callback.finishedParentNode(this.walkInfo.dir, dir, this.walkInfo);
      } else {
        result = SortedTreeWalkerCallback.Result.cont;
      }
      if(this.progress !=null) {                         
        //--------------------------------------- creates or updates a time order for the state. 
        if(this.timeOrderProgress !=null) { this.timeOrderProgress.hold(); }
        this.progress.progressCmd = FileRemoteProgressEvData.ProgressCmd.refreshDirPost;
        this.progress.currFile = this.walkInfo.dir;          // all information about the FileRemote will be proper serialized if remote
        if(this.co.cycleProgress() ==0) {        // send back event on any file or dir entry:
          this.evBack.sendEvent(this);             // evBack is associated to the progress
        } else {                               // send cyclically only informations about progress
          long timeEvent = System.currentTimeMillis() + this.co.cycleProgress();
          this.timeOrderProgress.activateAt(timeEvent, timeEvent); // activate a time order with delay, not too much traffic
          //this.progress.nrofBytesAll += this.curr.nrBytesInDir;
          //this.progress.nrFilesProcessed += this.curr.dir.children().size();
        }
      }
      if(this.debugOut) System.out.println("FileRemoteAccessorLocalJava7.walker - post dir; " + this.walkInfo.dir.getAbsolutePath());
      if(this.walkInfo.parent !=null) {
        this.walkInfo.parent.nrBytesInDirSelected += this.walkInfo.nrBytesInDirSelected;
        this.walkInfo.parent.nrofFilesSelected += this.walkInfo.nrofFilesSelected;
      }
      this.walkInfo = this.walkInfo.parent;   
      return translateResult(result);
    }

    
    
    /**Invoked for any file entry.
     * This method is invoked for directories instead {@link #preVisitDirectory(Path, BasicFileAttributes)}
     * if the depth of the tree is reached. Only then the Path is a directory. 
     * This method is not invoked if {@link #preVisitDirectory(Path, BasicFileAttributes)} is invoked for the Path. 
     * See {@link java.nio.file.FileVisitor#visitFile(java.lang.Object, java.nio.file.attribute.BasicFileAttributes)}
     */
    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
        throws IOException
    {
      final FileVisitResult ret;
      String name = file.getFileName().toString();
//      if(name.startsWith("docs"))
//        Debugutil.stop();
      boolean bDirectory = attrs.isDirectory();
      if(this.progress !=null) {
        if(bDirectory) {
          this.progress.nrDirVisited +=1;
        } else {
          this.progress.nrFilesVisited +=1;
        }
      }
      boolean selected = (this.fileFilter == null)         // check selection via String, fileFilter: 
                      || this.walkInfo.fileFilter.check(name, bDirectory) !=null;
      if( !selected                                        // not selected via String
       && this.co.markSet() == 0                                // and no set mark operation necessary 
       && ( this.co.selectMask() == 0                            // AND no select mask given,
         || (this.co.selectMask() & FileMark.orWithSelectString) ==0 //OR no OR-selectmask given,
        ) ) {                                              // it means not selected and no more to do
        return FileVisitResult.CONTINUE;                   // ====>> return but does nothing with the file,  
      }
      //----------------------------------------------------- continue get the file
      FileRemote fileRemote;
      if(this.walkInfo.dir !=null) { 
        if(bDirectory) {                                   // visitFile comes also on directory entries
          fileRemote = this.walkInfo.dir.subdir(name);         // get or create a sub directory in given dir
        } else {
          fileRemote = this.walkInfo.dir.child(name);          // get or create a file in given dir
        }
      } else {     // only a file is selected.             // get the file immediately.
        //assert(false);                                   // NO: starts always with a directory!
        String sDir = file.getParent().toString();         // get directory from nio.file.Path
        this.walkInfo.dir = FileRemote.getFile(sDir, null);
        fileRemote = FileRemote.getFile(sDir, name); // and gets a new directory
      }
      //----------------------------------------------------- If a co.selectMask is given, then the subdir should contain one of the bit.
      if((this.co.selectMask() & FileMark.mSelectMarkBits) !=0) {
        boolean bMarkSelect = (fileRemote.getMark() & FileMark.mSelectMarkBits & this.co.selectMask()) !=0;
        if( (this.co.selectMask() & FileMark.orWithSelectString) !=0) {
          selected |= bMarkSelect;
        } else {
          selected &= bMarkSelect;
        }
      }                                          // if co.selectMask does not contain mSelectMarkBits, do nothing with it.
      if(!selected) {
        if(this.co.markSet() !=0) {
//          if( (this.co.markSet & FileMark.alternativeFunction) !=0) {
//            fileRemote.setMarked(this.co.markSet);
//          } else {
//            fileRemote.resetMarked(this.co.markSet);
//          }
        }
        ret = FileVisitResult.CONTINUE;  //but does nothing with the file.      
      } 
      else {  //--------------------------------------------- The file is selected.
        if(this.co.markSet() !=0) {                             // setMark activity necessary: do it here
          if( (this.co.markSet() & FileMark.resetMark) !=0) {
            fileRemote.resetMarked(this.co.markSet());
          } else {
            fileRemote.setMarked(this.co.markSet());
          }
          if(this.progress !=null) {
            this.progress.nrofFilesMarked +=1;
          }
        }
        //
        setAttributes(fileRemote, file, attrs);            // copy the file attributes from nio.file..Path to FileRemote
        long size = attrs.size();
        assert(this.walkInfo.dir == fileRemote.getParentFile());
        this.walkInfo.nrBytesInDir += size;
        this.walkInfo.nrBytesInDirSelected += size;
        this.walkInfo.nrofFilesSelected +=1;
        if(this.progress !=null) {                         
          //--------------------------------------- creates or updates a time order for the state. 
          if(this.timeOrderProgress !=null) { this.timeOrderProgress.hold(); }
          this.progress.progressCmd = FileRemoteProgressEvData.ProgressCmd.refreshFile;
          this.progress.nrofFilesSelected +=1;
          this.progress.nrofBytesAll += size;
          this.progress.currFile = fileRemote;          // all information about the FileRemote will be proper serialized if remote
          if(this.co.cycleProgress() ==0) {        // send back event on any file or dir entry:
            this.evBack.sendEvent(this);             // evBack is associated to the progress
          } else {                               // send cyclically only informations about progress
            long timeEvent = System.currentTimeMillis() + this.co.cycleProgress();
            this.timeOrderProgress.activateAt(timeEvent, timeEvent); // activate a time order with delay, not too much traffic
            //this.progress.nrofBytesAll += this.curr.nrBytesInDir;
            //this.progress.nrFilesProcessed += this.curr.dir.children().size();
          }
        }
        if(this.debugOut) System.out.println("FileRemoteAccessorLocalJava7.walker - file; " + name);
        FileRemoteWalkerCallback.Result result;
        if(this.callback !=null && this.callback.shouldAborted()){
          //only if a manual abort comes from the callback.
          result = SortedTreeWalkerCallback.Result.terminate;
        } else {
          if(this.bRefresh){
            //if(curr.children !=null) { curr.children.put(name, fileRemote); }
            fileRemote.internalAccess().clrFlagBit(FileRemote.mRefreshChildPending);
            fileRemote.internalAccess().setRefreshed();
    
          }
          if(this.callback !=null) {
            if(bDirectory)
              Debugutil.stop();
            //check mask:
            result = this.callback.offerLeafNode(fileRemote, file);
          } else { 
            result = SortedTreeWalkerCallback.Result.cont;
          }
        }
        ret = translateResult(result);
      }
      //try { Thread.sleep(1); } catch (InterruptedException e) { }
      return ret;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc)
        throws IOException
    {
      if(this.progress !=null) {                         
        //--------------------------------------- creates or updates a time order for the state. 
        if(this.timeOrderProgress !=null) { this.timeOrderProgress.hold(); }
        this.progress.progressCmd = FileRemoteProgressEvData.ProgressCmd.refreshFileFaulty;
        if(this.co.cycleProgress() ==0) {        // send back event on any file or dir entry:
          this.evBack.sendEvent(this);             // evBack is associated to the progress
        } else {                               // send cyclically only informations about progress
          long timeEvent = System.currentTimeMillis() + this.co.cycleProgress();
          this.timeOrderProgress.activateAt(timeEvent, timeEvent); // activate a time order with delay, not too much traffic
          //this.progress.nrofBytesAll += this.curr.nrBytesInDir;
          //this.progress.nrFilesProcessed += this.curr.dir.children().size();
        }
      }
      return FileVisitResult.CONTINUE;
    }
 
  }
  
  
  
  class WalkerThread implements Runnable {
    
    Thread thread;
    boolean bRun = false;
    
    EventWithDst<FileRemoteProgressEvData,?> evBack;
    
    //FileRemoteWalkerEvent ev = new FileRemoteWalkerEvent("walker", FileAccessorLocalJava7.this, null, null, 0);
    
    FileRemoteCmdEventData co;
    
    void start() {
      if(this.thread ==null) {
        this.thread = new Thread(this, "walkerThread");
        this.bRun = true;
        this.thread.start();
      } else {
        synchronized (this) {
          this.notify();
        }
      }
    }
    
    
    
    @Override public void run() {
      while(this.bRun) {
        if(this.evBack !=null) {
          if(this.co !=null) {
            execCmd(this.co, this.evBack);
          } else {
//            FileAccessorLocalJava7.this.walkFileTreeExecInThisThread(this.ev.startDir, this.ev.bRefresh, this.ev.markSet, this.ev.markSetDir
//                , this.ev.sMask, this.ev.bMarkCheck, this.ev.depth, this.ev.callback, this.evBack , this.ev, false);
          }
          this.evBack = null;
          this.co = null;
        } else {
          synchronized(this) {
            try {
              this.wait(100);
            } catch (InterruptedException e) {
              // TODO Auto-generated catch block
              e.printStackTrace();
            }
          }
        }
      }
    } //run
  
    synchronized boolean setOrder( FileRemoteCmdEventData co, EventWithDst<FileRemoteProgressEvData,?> evBack) {
      if(this.evBack !=null) return false;  //not usable
      else {
        this.co = co;
        this.evBack = evBack;
        start();
        return true;
      }
    }
    
    /**Check without mutex, search free order.
     * if free then return of {@link #setOrder(org.vishia.fileRemote.FileRemoteCmdEventData, EventWithDst)}
     * should be checked also.
     * @return true if free.
     */
    boolean isFree ( ) { return this.evBack ==null && this.co == null; }
  
  }
  
  
  
  
  /**Access selector which uses {@link FileAccessorLocalJava7} for any path.
   * It is the standard for normal PC programs.
   * 
   */
  public static FileRemote.FileRemoteAccessorSelector selectLocalFileAlways = new FileRemote.FileRemoteAccessorSelector() {
    @Override public FileRemoteAccessor selectFileRemoteAccessor(CharSequence sPath) {
      return FileAccessorLocalJava7.getInstance();
    }
  };

  /**
   * @return null because it has not a typical event thread. See {@link #processEvent(EventObject)}
   */
  @Override public EventThread_ifc evThread () {
    return null;
  }

  /**Processes immediately an event,
   * but delegate to a free {@link #walkerThread}
   * It means the processing is finished in this thread, 
   * but the event is not relinquished yet immediately.
   */
  @Override public int processEvent ( EventObject ev ) {
    return 0;
  }

  @Override public boolean awaitExecution ( long timeout, boolean clearDone ) {
    // TODO Auto-generated method stub
    return false;
  }


  
}
