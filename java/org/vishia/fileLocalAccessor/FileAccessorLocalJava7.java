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
import java.nio.file.FileSystem;
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
   * <li>2026-03-18 Inside {@link WalkFileTreeVisitor} many changed to prevent conflicts on circular linked directories.
   *   Circular situations can be occur any time. General not prevented. Then the walking should not enter twice and more in a circular directory tree.
   *   Changes in coordination with {@link FileRemote#realFile} etc. 
   *   But yet not complete thought trough, because usage of the {@link FileRemote.Properties#idUsage} is not reentrant on multi thread walking. 
   *   TODO think about algorithm in thw walker itself, a HashMap for dir entries with the {@link #realFile} as entry.
   * <li>2026-03-17 {@link #execCmd(FileRemoteCmdEventData, EventWithDst)}: call of {@link FileCallbackLocalSearch} on {@link FileRemoteCmdEventData.Cmd#walkSearch} 
   * <li>2024-04-02 {@link WalkFileTreeVisitor#preVisitDirectory(Path, BasicFileAttributes)}:
   *   If the parent directory is marked with {@link FileMark#cmpAlone} and this bit is part of the select mask in the command (commision),
   *   then the directory is marked with {@link FileRemoteCmdEventData#markSet()}, means the bits to set for selection. 
   *   This allows copy also an alone standing directory. But yet todo it does not copy .... the files internally. 
   * <li>2024-02-26 {@link WalkFileTreeVisitor#visitFile(Path, BasicFileAttributes)}: Possibility to reset mark bits if the file is non selected.
   *   This is important to clean marking from the.File.commander. 
   *   Second: bugfix for comparison files, if cmpTimeGreater or Lesser is set but the files are marked as cmpContentEqual, 
   *   then they are not selected if cmpContentNotEqual flag is required 
   * <li>2024-02-12 Now respects symbolic link as JUNCTION in Windows:
   *   The {@link Files#isSymbolicLink(Path)} in Java original does not detect a JUNCTION as symbolic link, that is not nice.
   *   The solution is also tested in {@link WalkFileTreeVisitor#preVisitDirectory(Path, BasicFileAttributes)}:
   *   With {@link Path#toRealPath(LinkOption...)} the path resulting from JUNCTION is detected, and by comparison with {@link Path#toAbsolutePath()}
   *   it is detected that this is very probably a JUNCTION.
   *   It is important that also Junctions are not entered if {@link FileMark#ignoreSymbolicLinks} is set. 
   *   This is firstly for updating and comparison for source file trees. The symbolic linked directories (via Junction) should not be handled then,
   *   because they should be handled in there own working tree"
   *   Also the mode is set from outside, change argument list of {@link FileCallbackLocalCmp#FileCallbackLocalCmp(FileRemote, FileRemote, int, FileRemoteWalkerCallback, EventWithDst)}.
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
  public static final String sVersion = "2024-02-12";

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
    this.singleThreadForCommission = new EventTimerThread("FileAccessor-local", this);
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
  
  /**Returns a unique absolute path for the given file from medium. 
   * It does not regard resolve environment variables etc. 
   * but accepts starting with '~/...' for the home path 
   * and starting with '/tmp/...' for the tmp folder also on windows. 
   * It uses {@link FileFunctions#absolutePath(String, File, boolean)} with last argument false to fulfill all.
   * @param path given path
   * @return path to get the file. 
   * @since 2026-03: Do not resolve the environment variables, it has no sense here. Because the sPath comes always from the file system itself.
   *   Resolving environment variables in path makes only sense on given arguments from outside. 
   *   If this is necessary, call {@link FileFunctions#absolutePath(String, File, boolean)} for your own. 
   */
  @Override public CharSequence completeFilePath(CharSequence sPath) {
    return FileFunctions.absolutePath(sPath.toString(), null, false);
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
  protected static void setAttributes(FileRemote fileRemote, Path pathArg, BasicFileAttributes attribs){
    Path path = pathArg;
    fileRemote.internalAccess().setPath(path);
    FileTime fileTime = attribs.lastModifiedTime();
    long dateLastModified = fileTime.toMillis();
    long dateCreation = attribs.creationTime().toMillis();
    long dateLastAccess = attribs.lastAccessTime().toMillis();
    long length = attribs.size();
    int flags = FileRemote.mExist | FileRemote.mTested;
    //if(fileRemote.getName().contains("build")) Debugutil.stopp();
//
// cc-2025-08:
// This following lines were a experience why attribs do not contain the information about directory if pathArg is a symbolic linked dir.
// But it was not sufficient, but Problematic of get toRealPath() may be interesting also for other situations.
// That's why this commented lines should be left here.
// The solution of the problem was: 'options.add(FileVisitOption.FOLLOW_LINKS);' to arg of 'java.nio.file.Files.walkFileTree(..., options, ...)'
// changed in 'walkFileTreeExecInThisThread (...)'    
//    if(attribs.isSymbolicLink()) {
//      try {
//        path = path.toRealPath();
//        FileSystem fs = path.getFileSystem();
//        //Files.getAttribute(path, sVersion);
//      } catch (IOException e) {
//        // TODO Auto-generated catch block
//        
//      }
//      Debugutil.stop();
//    }
//    if(Files.isDirectory(pathArg)) {
//      flags |= FileRemote.mDirectory;
//    }
    if(attribs.isDirectory()){ 
      flags |= FileRemote.mDirectory;
      length = -1; // do not set, set in postVisitDirectory.
    }
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
// symbolicLink is already detected by toRealPath, inclusively the Windows JUNCTION which are not regard by isSymbolicLink()
// cc-2024-02
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
      FileFunctions.mkDirPath(file);
      FileOutputStream stream = new FileOutputStream(file);
      return stream;
    } catch(FileNotFoundException exc){
      return null;
    }
    
  }
  

  
  @Override public WritableByteChannel openWrite(FileRemote file, long passPhase)
  { try{ 
      FileFunctions.mkDirPath(file);
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

  
  
  
  
  
  
  
  /**
   * @param co commission data what should be done, especially {@link FileRemoteCmdEventData#callback} describes what should be done with a file.
   * @param evBack
   * @return
   */
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

  
  /**
   * @param co commission data what should be done, especially {@link FileRemoteCmdEventData#callback} describes what should be done with a file.
   * @param evBack
   * @return
   */
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
   * @param co commission data what should be done, especially {@link FileRemoteCmdEventData#callback} describes what should be done with a file.
   * @param evBack
   * @return
   */
  protected String execCmd ( FileRemoteCmdEventData co, EventWithDst<FileRemoteProgressEvData,?> evBack) {
    String ret = null;
    FileRemoteCmdEventData.Cmd cmd = co.cmd();
    //cmd = FileRemoteCmdEventData.Cmd.abortAll;
    switch(cmd){
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
      co.setCallback(new FileCallbackLocalCopy(co.filesrc(), co.filedst(), null, evBack));  //evCallback);
      FileAccessorLocalJava7.this.walkFileTreeExecInThisThread(co, false, evBack , false); 
      break;
    case walkMoveDirTree:
      assert(co.callback() == null);
      co.setCallback(new FileCallbackLocalMove(co.filedst(), null, evBack));  //evCallback);
      FileAccessorLocalJava7.this.walkFileTreeExecInThisThread(co, false, evBack , false); 
      break;
    case walkDelete:
      assert(co.callback() == null);
      co.setCallback(new FileCallbackLocalDelete(evBack));  //evCallback);
      FileAccessorLocalJava7.this.walkFileTreeExecInThisThread(co, false, evBack , false); 
      break;
    case walkCompare:
      assert(co.callback() == null);
      co.setCallback(new FileCallbackLocalCmp(co.filesrc(), co.filedst(), co.modeCmpOper, null, evBack));
      FileAccessorLocalJava7.this.walkFileTreeExecInThisThread(co, true, evBack , false); 
      break;
    case walkSearch:
      assert(co.callback() == null);
      co.setCallback(new FileCallbackLocalSearch(co.filesrc(), co.modeCmpOper, co.sText, null, evBack));
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
   * @param co commission data what should be done, especially {@link FileRemoteCmdEventData#callback} describes what should be done with a file.
   */
  @Override public String cmd(boolean bWait, FileRemoteCmdEventData co, EventWithDst<FileRemoteProgressEvData,?> evBack) {
    if(bWait) {
      return execCmd(co, evBack);                       // execute in this thread.
    } else {
      String ret = "no thread free";
      for(WalkerThread th : this.walkerThread) {
        if(th.isFree()) {       
          //======>>>>    =====thread found ================= set break point in operation above!
          if(! th.setOrder(co, evBack)) {                  
            ret = "cannot set order, evBack is null";
          } else {
            ret = null;                                    // and it's all done in this thread
          }
          break;                                           // the order will be executed in the other thread
        }
      }
      return ret;
    }
  }
  
  
  /**
   * @param co commission data what should be done, especially {@link FileRemoteCmdEventData#callback} describes what should be done with a file.
   * @param evBack
   */
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
  
  
  /**
   * @param co commission data what should be done, especially {@link FileRemoteCmdEventData#callback} describes what should be done with a file.
   * @param evBack
   */
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
  
  
  
  /**
   * @param co commission data what should be done, especially {@link FileRemoteCmdEventData#callback} describes what should be done with a file.
   * @param evBack
   */
  private void execCountLength(FileRemoteCmdEventData co, EventWithDst<FileRemoteProgressEvData, ?> evBack){
    long length = countLengthDir(co.filesrc(), 0, 0);    
    FileRemoteProgressEvData.ProgressCmd cmd;
    FileRemoteProgressEvData progress = evBack.data();
    if(length >=0){
      cmd = FileRemoteProgressEvData.ProgressCmd.done; 
      progress.nrofBytesUsed = length;
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
   * @param co commission data what should be done, especially {@link FileRemoteCmdEventData#callback} describes what should be done with a file.
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
   * <ul>
   * <li>_C_: First {@link FileRemoteWalkerCallback#start(FileRemote, FileRemoteCmdEventData)} is called
   *   from 'co' if {@link FileRemoteCmdEventData#callback} is given.
   * <li>_D_: If 'bRefreshChildren' is true, then all children from {@link FileRemoteCmdEventData#filesrc} 
   *   are marked with child.flags |= mRefreshChildPending. After walking either this attribute bit is reseted, or the child will be deleted
   *   because the appropriate file in the physical file system is not found while walking.  
   * <li>_E_: if 'evBack' is given, {@link FileRemoteProgressEvData#clean()} is called
   *   and the {@link FileRemoteProgressEvData#answerToCmd} is set from 'co' {@link FileRemoteCmdEventData#cmd}
   * <li>_F_: {@link java.nio.file.Files#walkFileTree(Path, Set, int, FileVisitor))} is called, with 
   *   <ul><li>'Path' from {@link FileRemoteCmdEventData#filesrc}
   *   <li>'Set' options always with {@link FileVisitOption#FOLLOW_LINKS}, to check the links.
   *   <li>'int' from {@link FileRemoteCmdEventData#depthWalk}, 1 for one level, 0: set to MAX_VALUE for all levels
   *   <li>'FileVisitor' see next:
   *   </ul>
   * <li>_G_: The {@link FileVisitor} is always an instance of {@link WalkFileTreeVisitor} (inner class here).
   *   This class is enough for refresh only. 
   *   If more should be done, it uses {@link WalkFileTreeVisitor#callback} from 'co' {@link FileRemoteCmdEventData#callback}.
   *   'callback' is intrinsically a command execution instance, not a so named callback.
   *   But the term 'callback' is usual used, because this 'callback' instance is given 
   *   with the calling instance 'co' in {@link FileRemoteCmdEventData#callback}
   *   Candidates for this execution command ('callback') instance are {@link FileCallbackLocalCmp}, 
   *   {@link FileCallbackLocalCopy}, {@link FileCallbackLocalMove} and {@link FileCallbackLocalDelete}. 
   * <li>_H_: After walking {@link FileRemoteWalkerCallback#finished(FileRemote)} is called
   *   from 'co' if {@link FileRemoteCmdEventData#callback} is given.
   * <li>_J_: Then  {@link FileRemoteProgressEvData#done(org.vishia.fileRemote.FileRemoteCmdEventData.Cmd, String)}   
   *   is called from given 'evBack' {@link EventWithDst#d} (the payload) 
   * <li>_K_: At least the given 'evBack' is sent to its sender instance (queue) via {@link EventWithDst#sendEvent(Object)}.  
   * </ul>
   * @param co commission data what should be done, especially {@link FileRemoteCmdEventData#callback} describes what should be done with a file.
   * @param bRefreshChildren true then reads the properties of all children from the original file system, 
   *   refreshes also the {@link FileRemote#children()} if {@link FileRemoteCmdEventData#depthWalk} reaches this sub level.
   * @param evBack a progress event, also usable for quests with answer via the {@link EventWithDst#getOpponent()} prepared before call.
   * @param debugOut
   */
  protected void walkFileTreeExecInThisThread (
    FileRemoteCmdEventData co
  , boolean bRefreshChildren
  , EventWithDst<FileRemoteProgressEvData, ?> evBack
  , boolean debugOut
  ) {
    int progressFinish = EventConsumer.mEventConsumFinished;
    String sError = null;                        // for unexpected exception message
    try{ 
//      if(evWalker.progress !=null && evWalker.progress.timeOrder !=null) {
//        evWalker.progress.timeOrder.activateCyclic();     // timeOrder back event to inform
//      }
      if(co.callback() !=null) {                           //_C_: start()
        co.callback().start(co.filesrc(), co); 
      }
      if(bRefreshChildren) {                               //_D_: refreshChildren is for children in FileRemote instance
        co.filesrc().internalAccess().pendingChildren();       // it marks all children with child.flags |= mRefreshChildPending,
      }                                                    // does not create a new instance.
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
      options.add(FileVisitOption.FOLLOW_LINKS);
      long idCircular = FileRemote.getIdUsage();           // count the idUsage, but should not necessary here ??? 
      //======>>>>                ----------------- call of the java.nio-walker
      //==========                ----------------- set breakpoints in visitFile etc. in the following class WalkFileTreeVisitor
      java.nio.file.Files.walkFileTree(co.filesrc().path(), options, depth1, visitor);  
      //
      if(visitor.timeOrderProgress !=null ) { 
        visitor.timeOrderProgress.deactivate(); 
      }
    } catch(IOException exc){
      sError = org.vishia.util.ExcUtil.exceptionInfo("FileAccessorLocalJava7.walkFileTree - unexpected Exception; ", exc, 0, 20).toString();
      progressFinish = EventConsumer.mEventConsumerException;
    }
    if(co.callback() !=null) { 
      co.callback().finished(co.filesrc());               // callback for finish 
    }
    if(evBack !=null ) {                       // back event for finish
      FileRemoteProgressEvData progress = evBack.data();
      if(progress.abort()) { 
        sError = sError ==null ? "aborted" : sError + " aborted"; 
      }
      progress.done(co.cmd(), sError);                        // set done for this calling command, to consider one destination for several actions.
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
    
    
    /**true then reads the properties of all children from the original file system, 
     *   refreshes also the {@link FileRemote#children()} if {@link FileRemoteCmdEventData#depthWalk} reaches this sub level.
     * It is set only in the ctor {@link WalkFileTreeVisitor#WalkFileTreeVisitor(FileCluster, boolean, FileRemoteCmdEventData, EventWithDst, boolean)}
     * from the second argument 'refreshChildern'.  
     */
    final boolean bRefresh;
    
    /**Commission data what should be done, especially {@link FileRemoteCmdEventData#callback} describes what should be done with a file.
     */
    final FileRemoteCmdEventData co;
    
    final SortedTreeWalkerCallback<FileRemote, FileRemoteCmdEventData> callback;
    
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
    
    /**This info are written during visit and shown during and after visit. 
     * Number of files etc.
     */
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
    
    
    /**This id is set on start walking (ctor). 
     * All files seen on walking are marked with this id: {@link FileRemote#checkIdUsage(long)}. 
     * So it is detected whether a file is attempt to handle twice. 
     * This is a circular tree because of a symbolic link backward, maybe via different links.
     * If this occurs, then the sub dir is skipped, it does not follow the circular entry. 
     */
    final long idUsageFiles;
    
    /**Constructs the instance.
     * @param fileCluster The cluster where all FileRemote are able to found by its path.
     * @param refreshChildren true then reads the properties of all children from the original file system, 
     *   refreshes also the {@link FileRemote#children()} if {@link FileRemoteCmdEventData#depthWalk} reaches this sub level.
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
      this.idUsageFiles = FileRemote.getIdUsage();          // to check whether files are used twice because circular symbolic links.
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
      if(co.cycleProgress() >0 && evBack !=null) {         // progress only in cycles, presumed evBack is given
        @SuppressWarnings("resource") EventThread_ifc timer = this.evBack.getDstThread();
        assert(timer instanceof EventTimerThread_ifc);     //should refer a timer
        this.timeOrderProgress = new TimeOrder("progress", (EventTimerThread_ifc)timer, this.evBack);
      } else {
        this.timeOrderProgress = null;
      }
      reset();
    }

    /**Translates between finer gradual return values of {@link SortedTreeWalkerCallback} results
     * and the necessary results for the {@link FileVisitor}.
     * All cont... {@link SortedTreeWalkerCallback.Result#contUnused} etc. results in {@link FileVisitResult#CONTINUE}.
     * All others adequate.
     * @param result from the {@link SortedTreeWalkerCallback} operations
     * @return necessary result for the {@link FileVisitor}.
     */
    private FileVisitResult translateResult(FileRemoteWalkerCallback.Result result){
      FileVisitResult ret;
      switch(result){
        case cont: case contUnused: case contUsed: case contMarked: case contMarkedOlder: case contReadError: ret = FileVisitResult.CONTINUE; break;
        case skipSiblings: ret = FileVisitResult.SKIP_SIBLINGS; break;
        case skipSubtree: ret = FileVisitResult.SKIP_SUBTREE; break;
        case terminate: ret = FileVisitResult.TERMINATE; break;
        default: assert(false); ret = FileVisitResult.TERMINATE;
      }
      return ret;      
    }
    
    
    private void reset(){ } //if(this.progress !=null) { this.progress.clear(); } }
    
    
    
    /**Invoked from {@link java.nio.file.FileTreeWalker} if the depths does not reached the end of directory deepness, 
     * even called for empty directories.
     * It implements {@link java.nio.file.FileVisitor#preVisitDirectory(java.lang.Object, java.nio.file.attribute.BasicFileAttributes)}
     * <ul>
     * <li>_A_: if {@link #callback} is given and {@link SortedTreeWalkerCallback#shouldAborted()} returns true, 
     *   then walking is aborted with {@link FileVisitResult#TERMINATE} because it is a command from outside to stop this doing. 
     * <li>_B_: Some internals are set: 
     *   <ul>
     *   <li>'selectMask' = {@link #co} -> {@link FileRemoteCmdEventData#selectMask()}. 
     *     {@link #co} is given on ctor {@link WalkFileTreeVisitor#WalkFileTreeVisitor(FileCluster, boolean, FileRemoteCmdEventData, EventWithDst, boolean)}
     *     for this walk. The selectMask is set in {@link #co} in preparing of this walk via     
     *     {@link FileRemoteCmdEventData#setCmdWalkLocal(FileRemote, org.vishia.fileRemote.FileRemoteCmdEventData.Cmd, FileRemote, int, int, String, int, int, SortedTreeWalkerCallback, int)}
     *     or even {@link FileRemoteCmdEventData#setCmdWalkRemote(FileRemote, org.vishia.fileRemote.FileRemoteCmdEventData.Cmd, FileRemote, String, int, int, int)}
     *     <br>
     *     This 'selectMask' contain bits even for selecting in cohesion with {@link FileRemote#mark()} bits 
     *     as even command bits to set and reset select bits.  
     *   <li>'namePath' = {@link Path#getFileName()},   
     *   <li>'dirAbs' = {@link Path#toAbsolutePath()} but only if {@link Path#isAbsolute()} is not set. On walking this should be always set, but tested here.
     *     Usage of the absolute path is essential. 
     *     <br>It will be presumed that this path is canonical, but not resolved to the real path on symbolic links.
     *     Last one are handled in _D_:  
     *   </ul>
     * <li>_C_: It tests whether a symbolic link is given. Whereby the original {@link Files#isSymbolicLink(Path)} does not work
     *   for JUNCTIONs in Windoes, but JUNCTION is a really symbolic link. 
     *   As solution for he JUNCTION problem the {@link Path#toRealPath(LinkOption...)} is gotten (anyway necessary) 
     *   and compared with the given path 'dirAbs'. 
     *   If both paths are equals then it is NOT a symbolic link, else it is.
     * <li>_D_: If {@link FileMark#ignoreSymbolicLinks} is set in the 'selectMask'
     *   then symbolic linked directories are general skipped, but not the selected start directory.
     *   This helps to prevent processing non substantial files. Often symbolic links are used for additional access possibilities,
     *   not for original sources. Then this flag can be set. But the determining selected first level is always used. 
     *   <br>TODO it may be interesting to walk through the real path tree if the first level is a symbolic link 
     *   and the flag {@link FileMark#ignoreSymbolicLinks} is set, or better another flag is set' 'useFirstlevelSymbolicLink'
     * <li>_E_: If a {@link #walkInfo} -> {@link FileRemoteWalker.WalkInfo#fileFilter} is given, then the 'childFilter' is gotten
     *   via {@link FilepathFilterM#check(String, boolean)} with true as second argument because it is an directory.   
     *   This file filter has the structure 'path/** /*.mask' with some more nuances, and hence should skip forward for a directory.
     *   If the result is null, then this directory is not selected, the directory is skipped.
     *   If it returns then this is the new filter used for the {@link #walkInfo} (3th argument of 
     *   {@link FileRemoteWalker.WalkInfo#WalkInfo(FileRemote, org.vishia.fileRemote.FileRemoteWalker.WalkInfo, FilepathFilterM)}
     *   for the new directory level of walking used in _M_:
     * <li>_F_: Working is only continued (else return {@link FileVisitResult#SKIP_SUBTREE}) if one of the following conditions are met:
     *   <ul>
     *   <li>The file is the root level for this walker.
     *   <li>The file is selected by textual mask described on _E_:
     *   <li>The bit {@link FileMark#orWithSelectString} is set in the 'selectMask' given.
     *     Then select bits in the FileRemote instance should be regarded, hence the FileRemote instance is necessary to continue.
     *     The usage of this entry then depends of this bits.
     *   <li>Any bit in {@link FileRemoteCmdEventData#markSet} is set, means there is some stuff to do with set bits, see _K_:
     *   <li>{@link #bRefresh} is set, it means the FileRemote instances of this directory should be all refreshed.   
     *   </ul>   
     * <li>_G_: Only on continue, the instance of FileRemote for the directory entry is obtained:
     *   <ul>
     *   <li>The appropriated {@link FileRemote} instance to the directory entry 'dir1' is searched 
     *     in the standard {@link FileRemote#clusterOfApplication} calling {@link FileRemote#getDir(CharSequence, CharSequence)}
     *     maybe with the realpath as second argument on symbolic linked directories.
     *     This arranges the given directory in a given {@link FileRemote} parent instance even for the symbolic linked instance
     *     as for the real path instance.  
     *   <li>Or it is searched or created as child in the given non symbolic linked {@link #walkInfo} -> {@link FileRemoteWalker.WalkInfo#dir}.   
     *     This is the same, only a faster way for non symbolic linked FileRemote instances.
     *   <li>If the 'dir' is a symbolic linked one, the new FileRemote#ctor(givenDir, realDir) constructs the {@link FileRemote#realFile}
     *     as link to the original directory. Hence the real path is knwon also in the FileRemote instances.  
     *     This is necessary to get the same mask() for both instances.
     *   <li>The FileRemote instance is refreshed by the properties of the real file.  
     *   <li>{@link FileRemote#checkIdUsage(long)} is invoked with the {@link #idUsageFiles} of this walking action. 
     *     If the directory was entered twice, then the usage is prevented with return {@link FileVisitResult#SKIP_SUBTREE}.
     *     This prevents unendingly running of walking. This is the important new feature @since 2026-03.
     *   </ul>
     * <li>_H_: The FileRemote instance 'dir' is refreshed with the given file information (length, time stamp). 
     *   If {@link #bRefresh} is set (second argument of ctor of this class, 'refreshChildren'), then 
     *   the {@link FileRemote#flags} bit {@link FileRemote#mRefreshChildPending} is cleared, because this child, the directory entry, is refreshed.
     *   But {@link FileRemote.InternalAccess#pendingChildren()} is called. This marks all stored children in {@link FileRemote#children}
     *   with this flag bit {@link FileRemote#mRefreshChildPending}. This is important because in {@link #postVisitDirectory(Path, IOException)}
     *   all non refreshed children are removed from this list {@link FileRemote#children}.
     * <li>_J_: Second selection with special bits in the FileRemote#mask() instance:
     *   <br>The refresh action are all done before. The selection is related to the last processed _K_.
     *   Working is only continued if
     *   <ul>
     *   <li>On top level
     *   <li>A select mask is not given. 
     *   <li>If one of the bits of {@link FileMark#mSelectMarkBits} are given in the 'selectMask' given for this walk, 
     *     then this bits are tuned with the given bits in {@link FileRemote#mark()}.
     *     Either {@link FileMark#orWithSelectString} are given, then this bits are used to additional select this file,
     *     which may be non selected by the given mask.
     *     Or this bits are used as AND condition. This is the usual case, if all files would be selected else, because no textual selection is given. 
     *     This is used especially if a mark is done before, typical on handling in 'The.file.Commander', first it is selected
     *     by comparison, searching etc. Then the pre selected files, with bits in {@link FileRemote#mark()} are used
     *     by a given mask there similar as '?#^+' for selection changed files with new time stamp and additional files.
     *   </ul>
     * <li>_K_: If any bit of {@link FileRemoteCmdEventData#markSet} are given in the {@link #co}
     *     given on ctor {@link FileRemoteCmdEventData#setCmdWalkLocal(FileRemote, org.vishia.fileRemote.FileRemoteCmdEventData.Cmd, FileRemote, int, int, String, int, int, SortedTreeWalkerCallback, int)}
     *     as argument of this walk, then this bits are set or cleared depending on given FileMark#resetMark 
     * <li>_L_: If {@link #progress} is given it is now updated.
     * <li>_M_: Builds a new {@link FileRemoteWalker.WalkInfo#WalkInfo(FileRemote, org.vishia.fileRemote.FileRemoteWalker.WalkInfo, FilepathFilterM)} 
     *   for this directory level, use it for _N_: and store it for the entered new directory in {@link #walkInfo}.
     *   The {@link #walkInfo} before is restored in {@link #postVisitDirectory(Path, IOException)} again from {@link FileRemoteWalker.WalkInfo#parent}. 
     * <li>_N_: If {@link #co} -> {@link FileRemoteCmdEventData#callback}  is given, then the {@link FileRemoteWalkerCallback#offerParentNode(FileRemote, Object, Object)}
     *   is now called with the FileRemote instance of this directory. This can do specific work with the FileRemote dir entry.
     *   If callback is not given, all other operations before have done a proper work (mark update). 
     *   It means for only updating the FileRemote entries the callback is not necessary.
     *   <br>
     *   In older versions the following comment was written: 
     *   <i>The {@link #co} -> {@link FileRemoteCmdEventData#callback} -> {@link FileRemoteWalkerCallback#offerParentNode(FileRemote, Object, Object)}
     *   is not called for the first level (!) because the first level is the original source directory which should not handled by itself,
     *   only its content should be handled. Also the {@link FileRemoteCmdEventData#selectFilter} is valid only from the second level.
     *   The first level is intrinsic selected because it is the calling source directory.
     *   This is detected by evaluating {@link FileRemote.WalkInfo#parent} which is null for the first level.</i>
     *   But meanwhile since 2025-12-30 this is no more true. Here TODO test, it is a new change ??? 
     * </ul>
     */
    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
        throws IOException
    {
      final FileVisitResult ret;
      if(this.callback !=null && this.callback.shouldAborted()) { //_A_: abort on command from outside
        return FileVisitResult.TERMINATE; 
      }
      int mSelectMask = this.co.selectMask();               //_B_: set some internals.
      Path namepath = dir.getFileName();                   // NOTE namepath is null if for ex. D:/ is dir
      String name = namepath == null ? "/" : namepath.toString();
      //if(name.equals("data")) Debugutil.stopp();
      SortedTreeWalkerCallback.Result result;
      boolean selected;
      final FilepathFilterM childFilter;
      boolean bIgnoreSymbolicLinks = (mSelectMask & FileMark.ignoreSymbolicLinks)!=0;
      //if(bIgnoreSymbolicLinks) Debugutil.stopp();
      final Path dirAbs;
      if(!dir.isAbsolute()) {                   // when does it occure?
        dirAbs = dir.toAbsolutePath();
      } else {
        dirAbs = dir;                           // normal case
      }                                //------------------_C_: detect a symbolic link, also a JUNCTION in windows
      Path dirRealPath = dirAbs.toRealPath();               // In windows this works also for JUNCTION
      boolean isSymbolicLink = dirRealPath.compareTo(dirAbs)!=0;  // compare both is a longer way but correct.
      boolean isSymbolicLinkByFilesystem = Files.isSymbolicLink(dir);  //Note: this does not detect JUNCTION in Windows.
      if(isSymbolicLinkByFilesystem) { //------------------^^ isSymbolicLink is set.
        Debugutil.stop();
      }                                //------------------vv childFilter from the given walkInfo
      if(this.walkInfo.parent ==null) {                    //_D_: on the first level of preVisistDirectory:
        selected = true;                                   // it is always selected (elsewhere the operation will no t be called)
        childFilter = this.walkInfo.fileFilter;            // the fileFilter is effective from the next level
      } else if((mSelectMask & FileMark.ignoreSymbolicLinks) !=0 &&  isSymbolicLink) {
        selected = false;                        //_D_: skip a directory which is a symbolic link if desired
        childFilter = null;
      } else if(this.fileFilter == null) {       //_E_: do not skip if no fileFilter given, because files may be marked
        selected = true; result = SortedTreeWalkerCallback.Result.cont;
        childFilter = null;
      } else {                                   //_E_: evaluate fileFilter, skip if no file is selected.
        childFilter = this.walkInfo.fileFilter.check(name, true); 
        selected = (childFilter != null); 
      }                                //------------------^^ childFilter
      int markSet = this.co.markSet();                     // any bit is set: info what to do to mark or reset mark:
      int selectMask = this.co.selectMask();               // given bit mask for selection from commission
      if( !selected                           //_F_: not selected, vv also not with a selectMask which ORs selection
       && ( selectMask == 0 || (selectMask & FileMark.orWithSelectString) ==0 )
       && markSet == 0                        // nothing else to do with the dir 
       && !this.bRefresh                      // and also not to refresh
        ) {
        return FileVisitResult.SKIP_SUBTREE;               // ====>> return skipSubtree, nothing more to do.  
      }
      //========^^^^^^^====================================== return skipSubtree if not selected and no more to do
      //
      //===================================================vv either selected or some to do:
      final FileRemote dir1;                     //--------vv get the FileRemote instance for the directory proper to this path
      if(this.walkInfo.dir ==null || isSymbolicLink) {     // null only on first entry 
        String sDir = dir.toString();
        String sDirReal = isSymbolicLink ? dirRealPath.toString() : null;              // get directory from nio.file.Path
        //if(sDirReal !=null) Debugutil.stopp();
        dir1 = FileRemote.getDir(sDir, sDirReal);          //_G_: and gets the directory instance from file cluster
      } else {                                             // not first time:
        dir1 = this.walkInfo.dir.subdir(name);             //_G_: get or create a child in FileRemote, it is a faster way with same result.
      }
      if(!dir1.checkIdUsage(this.idUsageFiles)) {
        //Debugutil.stopp();
        return FileVisitResult.SKIP_SUBTREE;               //_G_:
      }
      setAttributes(dir1, dir, attrs);           //--------<< copy the file attributes from nio.file..Path to FileRemote also if not bRefresh
      if(this.bRefresh && this.walkInfo !=null){           //_H_: for this dir, mRefreshChildPending no more pending
        dir1.internalAccess().clrFlagBit(FileRemote.mRefreshChildPending);
        dir1.internalAccess().pendingChildren();               // but the children are set with mRefreshChildPending
      }
      //------------------------------------------- _J_: If a co.selectMask is given, then the subdir should contain one of the bit.
           
      if(this.walkInfo.parent !=null && (selectMask & FileMark.mSelectMarkBits) !=0) {  // one of the relevant bits are set? 0x3fffffff
        boolean bMarkSelect = (dir1.getMark() & FileMark.mSelectMarkBits & selectMask) !=0; // true then selected with bits
        if( (selectMask & FileMark.orWithSelectString) !=0) { 
          selected |= bMarkSelect;                         // additional selection with this bits beside string mask
        } else {
          selected &= bMarkSelect;                         // necessary selection with this bits beside string mask
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
        return FileVisitResult.SKIP_SUBTREE;               // ====>> return skipSubtree, nothing more to do.  
      }
      //========^^^^^^^====================================== return skipSubtree if not selected
      //
      //===================================================vv selected:
      ret = FileVisitResult.CONTINUE;                    // enter in directory always if curr.levelProcessMarked !=1
      if(this.walkInfo.parent !=null && markSet !=0) {   //_K_: anything to do here?
        if( (markSet & FileMark.resetMark) !=0) {        // reset a mark also for a directory
          dir1.resetMarked(markSet);
        } else {
          boolean bMarkDir = (dir1.getMark() & FileMark.cmpAlone & selectMask) !=0;
          if(bMarkDir) {
            dir1.setMarked(this.co.markSet());           // set the directory mark with the bits from the command because cmpAlone is detected and relevant
          }                                              // to copy the directory content. 
        }
      }
      
      if(this.progress !=null) { //----------------------- _L_:  creates or updates a time order for the state. 
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
      //======================================================_M_: Build the currentInfo of this directory level
      FileRemoteWalker.WalkInfo currInfo = new FileRemoteWalker.WalkInfo(dir1, this.walkInfo, childFilter);
      //
      if(/*this.walkInfo.parent !=null && */this.callback !=null) {  // not for the entry level ??
        //---------------------vvvv======                   //_N_: call the offerParentNode
        result = this.callback.offerParentNode(dir1, dir, currInfo, this.walkInfo.parent ==null);  //<<<<======
        //
      } else {
        result = SortedTreeWalkerCallback.Result.cont;
      }
      if(result == SortedTreeWalkerCallback.Result.cont){
        this.walkInfo = currInfo;                           //_M_: Store this currInfo because a new level is given, removed on postVisistDirectory
        if(this.debugOut) System.out.println("FileRemoteAccessorLocalJava7.walker - pre dir; " + this.walkInfo.dir.getAbsolutePath());
      } else {                                           
        // currInfo will be garbaged, not necessary
        if(this.debugOut) System.out.println("FileRemoteAccessorLocalJava7.walker - pre dir don't entry; " + this.walkInfo.dir.getAbsolutePath());
      }
      return this.callback !=null && this.callback.shouldAborted() ? FileVisitResult.TERMINATE : translateResult(result);
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
      if(this.callback !=null && this.callback.shouldAborted()) {
        return FileVisitResult.TERMINATE; 
      }
      if(this.bRefresh){  
        //no: curr.dir.internalAccess().setChildren(curr.children);  //Replace the map.
        //thread safety: The children which are marked with mRefreshChildPending are removed.
        //If this mark is set in another thread too because the same directory should be refreshed in another thread
        //then children are removed which are existing and not to remove.
        //Only one thread should do this action.
        //The setChildrenRefreshed() is called yet (2015-11-13) before  the callback.finishedParentNode(...) is called
        //because that call invokes refresh the second time.
        this.walkInfo.dir.timeChildren = System.currentTimeMillis();
        this.walkInfo.dir.internalAccess().setChildrenRefreshed();  // first called before callback.finishedParentNode see above
        
        if(this.co.depthWalk() ==0) {             //--------vv update length and date only for walk till full deepness, else the values are not correct.
          this.walkInfo.dir.internalAccess().setNrofFilesInTree(this.walkInfo.nrofFilesInSubtree, this.walkInfo.nrBytesInDir);
          this.walkInfo.dir.internalAccess().setLengthAndDate(this.walkInfo.nrBytesInDir, -1, -1, System.currentTimeMillis());
        }
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
        this.progress.nrDirVisited +=1;
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
        this.walkInfo.parent.nrBytesInDir += this.walkInfo.nrBytesInDir;
        this.walkInfo.nrofFilesInSubtree += this.walkInfo.nrofFilesInSubtree;
      }
      this.walkInfo = this.walkInfo.parent;   
      return translateResult(result);
    }

    
    
    /**Invoked for any file entry.
     * This method is invoked for directories instead {@link #preVisitDirectory(Path, BasicFileAttributes)}
     * if the depth of the tree is reached. Only then the Path is a directory. 
     * This method is not invoked if {@link #preVisitDirectory(Path, BasicFileAttributes)} is invoked for the Path. 
     * See {@link java.nio.file.FileVisitor#visitFile(java.lang.Object, java.nio.file.attribute.BasicFileAttributes)}
     * <br><br>
     * TODO: The following list is only copied and adapted as necessary from {@link #preVisitDirectory(Path, BasicFileAttributes)}
     *   but not completely checked. The steps are similar but not the same. It is yet only a raw docu.
     * <ul>
     * <li>_A_: if {@link #callback} is given and {@link SortedTreeWalkerCallback#shouldAborted()} returns true, 
     *   then walking is aborted with {@link FileVisitResult#TERMINATE} because it is a command from outside to stop this doing. 
     * <li>_B_: Some internals are set: 
     *   <ul>
     *   <li>'selectMask' = {@link #co} -> {@link FileRemoteCmdEventData#selectMask()}. 
     *     {@link #co} is given on ctor {@link WalkFileTreeVisitor#WalkFileTreeVisitor(FileCluster, boolean, FileRemoteCmdEventData, EventWithDst, boolean)}
     *     for this walk. The selectMask is set in {@link #co} in preparing of this walk via     
     *     {@link FileRemoteCmdEventData#setCmdWalkLocal(FileRemote, org.vishia.fileRemote.FileRemoteCmdEventData.Cmd, FileRemote, int, int, String, int, int, SortedTreeWalkerCallback, int)}
     *     or even {@link FileRemoteCmdEventData#setCmdWalkRemote(FileRemote, org.vishia.fileRemote.FileRemoteCmdEventData.Cmd, FileRemote, String, int, int, int)}
     *     <br>
     *     This 'selectMask' contain bits even for selecting in cohesion with {@link FileRemote#mark()} bits 
     *     as even command bits to set and reset select bits.  
     *   <li>'namePath' = {@link Path#getFileName()},   
     *   <li>'dirAbs' = {@link Path#toAbsolutePath()} but only if {@link Path#isAbsolute()} is not set. On walking this should be always set, but tested here.
     *     Usage of the absolute path is essential. 
     *     <br>It will be presumed that this path is canonical, but not resolved to the real path on symbolic links.
     *     Last one are handled in _D_:  
     *   </ul>
     * <li>_E_: If a {@link #walkInfo} -> {@link FileRemoteWalker.WalkInfo#fileFilter} is given, then the 'childFilter' is gotten
     *   via {@link FilepathFilterM#check(String, boolean)} with true as second argument because it is an directory.   
     *   This file filter has the structure 'path/** /*.mask' with some more nuances, and hence should skip forward for a directory.
     *   If the result is null, then this directory is not selected, the directory is skipped.
     *   If it returns then this is the new filter used for the {@link #walkInfo} (3th argument of 
     *   {@link FileRemoteWalker.WalkInfo#WalkInfo(FileRemote, org.vishia.fileRemote.FileRemoteWalker.WalkInfo, FilepathFilterM)}
     *   for the new directory level of walking used in _M_:
     * <li>_F_: Working is only continued (else return {@link FileVisitResult#SKIP_SUBTREE}) if one of the following conditions are met:
     *   <ul>
     *   <li>The file is selected by textual mask described on _E_:
     *   <li>The bit {@link FileMark#orWithSelectString} is set in the 'selectMask' given.
     *     Then select bits in the FileRemote instance should be regarded, hence the FileRemote instance is necessary to continue.
     *     The usage of this entry then depends of this bits.
     *   <li>Any bit in {@link FileRemoteCmdEventData#markSet} is set, means there is some stuff to do with set bits, see _K_:
     *   <li>{@link #bRefresh} is set, it means the FileRemote instances of this directory should be all refreshed.   
     *   </ul>   
     * <li>_G_: Only on continue, the instance of FileRemote for the directory entry is obtained:
     *   <ul>
     *   <li>The appropriated {@link FileRemote} instance to the directory entry 'dir1' is searched 
     *     in the standard {@link FileRemote#clusterOfApplication} calling {@link FileRemote#getDir(CharSequence, CharSequence)}
     *     maybe with the realpath as second argument on symbolic linked directories.
     *     This arranges the given directory in a given {@link FileRemote} parent instance even for the symbolic linked instance
     *     as for the real path instance.  
     *   <li>Or it is searched or created as child in the given non symbolic linked {@link #walkInfo} -> {@link FileRemoteWalker.WalkInfo#dir}.   
     *     This is the same, only a faster way for non symbolic linked FileRemote instances.
     *   <li>If the 'dir' is a symbolic linked one, the new FileRemote#ctor(givenDir, realDir) constructs the {@link FileRemote#realFile}
     *     as link to the original directory. Hence the real path is knwon also in the FileRemote instances.  
     *     This is necessary to get the same mask() for both instances.
     *   <li>The FileRemote instance is refreshed by the properties of the real file.  
     *   <li>{@link FileRemote#checkIdUsage(long)} is invoked with the {@link #idUsageFiles} of this walking action. 
     *     If the directory was entered twice, then the usage is prevented with return {@link FileVisitResult#SKIP_SUBTREE}.
     *     This prevents unendingly running of walking. This is the important new feature @since 2026-03.
     *   </ul>
     * <li>_H_: The FileRemote instance 'dir' is refreshed with the given file information (length, time stamp). 
     *   If {@link #bRefresh} is set (second argument of ctor of this class, 'refreshChildren'), then 
     *   the {@link FileRemote#flags} bit {@link FileRemote#mRefreshChildPending} is cleared, because this child, the directory entry, is refreshed.
     *   But {@link FileRemote.InternalAccess#pendingChildren()} is called. This marks all stored children in {@link FileRemote#children}
     *   with this flag bit {@link FileRemote#mRefreshChildPending}. This is important because in {@link #postVisitDirectory(Path, IOException)}
     *   all non refreshed children are removed from this list {@link FileRemote#children}.
     * <li>_J_: Second selection with special bits in the FileRemote#mask() instance:
     *   <br>The refresh action are all done before. The selection is related to the last processed _K_.
     *   Working is only continued if
     *   <ul>
     *   <li>A select mask is not given. 
     *   <li>If one of the bits of {@link FileMark#mSelectMarkBits} are given in the 'selectMask' given for this walk, 
     *     then this bits are tuned with the given bits in {@link FileRemote#mark()}.
     *     Either {@link FileMark#orWithSelectString} are given, then this bits are used to additional select this file,
     *     which may be non selected by the given mask.
     *     Or this bits are used as AND condition. This is the usual case, if all files would be selected else, because no textual selection is given. 
     *     This is used especially if a mark is done before, typical on handling in 'The.file.Commander', first it is selected
     *     by comparison, searching etc. Then the pre selected files, with bits in {@link FileRemote#mark()} are used
     *     by a given mask there similar as '?#^+' for selection changed files with new time stamp and additional files.
     *   </ul>
     * <li>_K_: If any bit of {@link FileRemoteCmdEventData#markSet} are given in the {@link #co}
     *     given on ctor {@link FileRemoteCmdEventData#setCmdWalkLocal(FileRemote, org.vishia.fileRemote.FileRemoteCmdEventData.Cmd, FileRemote, int, int, String, int, int, SortedTreeWalkerCallback, int)}
     *     as argument of this walk, then this bits are set or cleared depending on given FileMark#resetMark 
     * <li>_L_: If {@link #progress} is given it is now updated.
     * <li>_N_: If {@link #co} -> {@link FileRemoteCmdEventData#callback}  is given, then the {@link FileRemoteWalkerCallback#offerParentNode(FileRemote, Object, Object)}
     *   is now called with the FileRemote instance of this directory. This can do specific work with the FileRemote dir entry.
     *   If callback is not given, all other operations before have done a proper work (mark update). 
     *   It means for only updating the FileRemote entries the callback is not necessary.
     *   <br>
     *   In older versions the following comment was written: 
     *   <i>The {@link #co} -> {@link FileRemoteCmdEventData#callback} -> {@link FileRemoteWalkerCallback#offerParentNode(FileRemote, Object, Object)}
     *   is not called for the first level (!) because the first level is the original source directory which should not handled by itself,
     *   only its content should be handled. Also the {@link FileRemoteCmdEventData#selectFilter} is valid only from the second level.
     *   The first level is intrinsic selected because it is the calling source directory.
     *   This is detected by evaluating {@link FileRemote.WalkInfo#parent} which is null for the first level.</i>
     *   But meanwhile since 2025-12-30 this is no more true. Here TODO test, it is a new change ??? 
     * </ul>
     */
    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
        throws IOException
    {
      if(this.callback !=null && this.callback.shouldAborted()) {
        return FileVisitResult.TERMINATE;                   //_A_:
      }
      try {
        final FileVisitResult ret;
        String name = file.getFileName().toString();        //_B_:
        //if(name.equals("data")) Debugutil.stopp();
        int selectMask = this.co.selectMask();
        //if(name.startsWith("constant-values.html")) Debugutil.stopp();
        boolean isSymbolicLink;
        boolean bDirectory = Files.isDirectory(file); //  attrs.isDirectory();
        if(bDirectory) {
        }
        if(this.progress !=null) {
          if(bDirectory) {
            this.progress.nrDirVisited +=1;
          } else {
            this.progress.nrFilesVisited +=1;
            this.progress.nrofBytesVisited += file.toFile().length();
          }
        }
        long size = attrs.size();
        this.walkInfo.nrBytesInDir += size;
        this.walkInfo.nrofFilesInSubtree +=1;
        boolean selected = (this.fileFilter == null)         //_E_: check selection via String, fileFilter: 
                        || this.walkInfo.fileFilter.check(name, bDirectory) !=null;
        if( !selected                                        //_F_: not selected via String
         && this.co.markSet() == 0                                // and no set mark operation necessary 
         && ( selectMask == 0                            // AND no select mask given,
           || (selectMask & FileMark.orWithSelectString) ==0 //OR no OR-selectmask given,
          ) ) {                                              // it means not selected and no more to do
          return FileVisitResult.CONTINUE;                   // ====>> return but does nothing with the file,  
        }
        //----------------------------------------------------- continue get the file
        FileRemote fileRemote;                               //_G_:
        if(this.walkInfo.dir !=null) { 
          if(bDirectory) {                                  // visitFile comes also on directory entries
            final Path dirAbs;
            if(!file.isAbsolute()) {                   // when does it occure?
              dirAbs = file.toAbsolutePath();
            } else {
              dirAbs = file;                           // normal case
            }                                //------------------vv detect a symbolic link, also a JUNCTION in windows
            Path dirRealPath = dirAbs.toRealPath();               // In windows this works also for JUNCTION
            isSymbolicLink = dirRealPath.compareTo(dirAbs)!=0;  // compare both is a longer way but correct.
            if(isSymbolicLink) {     // null only on first entry 
              String sDir = file.toString();
              String sDirReal = dirRealPath.toString();              // get directory from nio.file.Path
              //if(sDirReal !=null) Debugutil.stopp();
              fileRemote = FileRemote.getDir(sDir, sDirReal);                    // and gets the directory instance from file cluster
            } else {                                             // not first time:
              fileRemote = this.walkInfo.dir.subdir(name);             // get or create a child in FileRemote, it is a faster way with same result.
            }
            //fileRemote = this.walkInfo.dir.subdir(name);    // get or create a sub directory in given dir
          } else {
            fileRemote = this.walkInfo.dir.child(name);     // get or create a file in given dir
          }
        } else {     // only a file is selected.            // get the file immediately.
          //assert(false);                                  // NO: starts always with a directory!
          String sDir = file.getParent().toString();        // get directory from nio.file.Path
          this.walkInfo.dir = FileRemote.getFile(sDir, null);
          fileRemote = FileRemote.getFile(sDir, name); // and gets a new directory
        }
        if(!fileRemote.checkIdUsage(this.idUsageFiles)) {
          return FileVisitResult.SKIP_SUBTREE;
        }
//----------------------------------------------------- If a co.selectMask is given, then the subdir should contain one of the bit.
        int markFile = fileRemote.getMark();                //_K_:
        if( (markFile & FileMark.cmpContentEqual) !=0        // if the file is equal after comparison.
         && (selectMask & FileMark.cmpContentNotEqual) !=0) {// and for comparison non equals files should be regarded
          markFile &= ~ (FileMark.cmpTimeGreater | FileMark.cmpTimeLesser);  // then ignore marks of its time stamp
        }                                                    // it means if 'non equal' is the command, the file should be non equal or equality is not tested.
        else if( (markFile & FileMark.cmpContentNotEqual) !=0        // if the file is non equal after comparison.
         && (markFile & (FileMark.cmpTimeGreater | FileMark.cmpTimeLesser)) !=0 // and it has also the bits for lesser and greater
         && (selectMask & (FileMark.cmpTimeGreater | FileMark.cmpTimeLesser)) !=0 // and the select mask has this bits too:
          ) {
          markFile &= ~ FileMark.cmpContentNotEqual;  // then remove mark for contentNotEqual to prevent selecting the other, greater of lesser time
        }                                                    // it means if 'non equal' is the command, the file should be non equal or equality is not tested.
        if( (selectMask & FileMark.cmpTimeGreater)!=0 )
          Debugutil.stop();                                  // stop here to debug file mark with ^ given
        // Now evaluation of the select bits to select, markFile is tuned before for time stamp comparison.
        if((selectMask & FileMark.mSelectMarkBits) !=0) {    // general: that are all bits excl. orWithSelectString and ignoreSymbolicLinks
          boolean bMarkSelect = (markFile & FileMark.mSelectMarkBits & selectMask) !=0;
          if( (this.co.selectMask() & FileMark.orWithSelectString) !=0) {
            selected |= bMarkSelect;
          } else {
            selected &= bMarkSelect;
          }
        }                                          // if co.selectMask does not contain mSelectMarkBits, do nothing with it.
        if(!selected) {                                     //_J_:
          if(this.co.markSet() !=0) {
            if( (this.co.markSet() & FileMark.resetNonMarked) !=0) {
              fileRemote.resetMarked(this.co.markSet());
            }
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
              this.progress.nrofFilesSelected +=1;
            }
          }
          //
          setAttributes(fileRemote, file, attrs);            //_H_: copy the file attributes from nio.file..Path to FileRemote
          //org.vishia.util.ExcUtil.check(this.walkInfo.dir == fileRemote.getParentFile());
          
          this.walkInfo.nrBytesInDirSelected += size;
          this.walkInfo.nrofFilesSelected +=1;
          if(this.debugOut) System.out.println("FileRemoteAccessorLocalJava7.walker - file; " + name);
          FileRemoteWalkerCallback.Result result;
          //                                                 //vv check shouldAborted important for debug, if abort is set manual
          if(this.callback !=null && this.callback.shouldAborted()) {
            //only if a manual abort comes from the callback.
            result = SortedTreeWalkerCallback.Result.terminate;
          } else {
            if(this.bRefresh){
              //if(curr.children !=null) { curr.children.put(name, fileRemote); }
              fileRemote.internalAccess().clrFlagBit(FileRemote.mRefreshChildPending);
              fileRemote.internalAccess().setRefreshed();
      
            }
            if(this.callback !=null) {                      //_N_:
              //if(bDirectory) Debugutil.stopp();
              //----------------------------------------------------------------<<<<====== offerLeaveNode
              result = this.callback.offerLeafNode(fileRemote, file);         //<<<<======
              //
            } else { 
              result = SortedTreeWalkerCallback.Result.contUsed;      // File used by file mask, but don't know what to do .
            }
          }
          if(this.progress !=null) {                        //_L_:
            //--------------------------------------- creates or updates a time order for the state. 
            if(this.timeOrderProgress !=null) { this.timeOrderProgress.hold(); }
            this.progress.progressCmd = FileRemoteProgressEvData.ProgressCmd.refreshFile;
            this.progress.nrofFilesUsed +=1;
            this.progress.nrofBytesUsed += size;
            if(result == FileRemoteWalkerCallback.Result.contMarkedOlder) {
              this.progress.nrofFilesChangedOlder +=1;
              this.progress.nrofBytesChangedOlder += size;
            }
            else if(result == FileRemoteWalkerCallback.Result.contMarked) {
              this.progress.nrofFilesMarked +=1;
              this.progress.nrofBytesMarked += size;
            }
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
          ret = this.callback !=null && this.callback.shouldAborted() ? FileVisitResult.TERMINATE : translateResult(result);
        }
        return this.callback !=null && this.callback.shouldAborted() ? FileVisitResult.TERMINATE : ret;
      } catch(Exception exc ) {                  //--------vv anything is wrong with this file.
        // log output?
        return this.callback.shouldAborted() ? FileVisitResult.TERMINATE  // terminate if abort is given from outside. 
               : FileVisitResult.SKIP_SUBTREE;  // same as CONTINUE, ignore this file, cannot do anything. 
      }
      //try { Thread.sleep(1); } catch (InterruptedException e) { }
    }

    
    
    
    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc)
        throws IOException
    {
      if(this.callback !=null && this.callback.shouldAborted()) {
        return FileVisitResult.TERMINATE; 
      }
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

 

  
}
