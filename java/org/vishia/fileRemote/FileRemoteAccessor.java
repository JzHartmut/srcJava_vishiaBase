package org.vishia.fileRemote;

import java.io.Closeable;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.List;

//import org.vishia.event.EventCmdtypeWithBackEvent;
import org.vishia.event.EventConsumer;
import org.vishia.event.EventConsumerAwait;
import org.vishia.event.EventSource;
import org.vishia.event.EventThread_ifc;
import org.vishia.event.EventWithDst;

/**Interface for instances, which organizes a remote access to files.
 * One instance per transfer protocol are need.
 * 
 * @author Hartmut Schorrig
 *
 */
public abstract class FileRemoteAccessor implements EventConsumer 
{
  private static final long serialVersionUID = 1589913596618865454L;
  
  /**Version, history and license.
   * <ul>
   * <li>2023-03-15 Hartmut chg: no more derived from Closeable, instead {@link #close()} defined here,
   *   because there are anytime again unnecessary warnings "resource" on access to this instance.  
   * <li>2012-05-30 Hartmut new: {@link #openOutputStream(FileRemote, long)}
   *   Note: it may be that the {@link #openRead(FileRemote, long)} and {@link #openWrite(FileRemote, long)}
   *   is not proper for some requirements, working with the traditional streams may be better.
   * <li>2015-05-30 Hartmut new: {@link #walkFileTreeCheck(FileRemote, boolean, boolean, boolean, String, long, int, FileRemoteWalkerCallback)}
   * <li>2012-09-14 Hartmut new: {@link CallbackFile}, {@link #walkFileTree(FileRemote, FileFilter, int, CallbackFile)}. 
   * <li>2012-08-12 Hartmut chg: Now it is an interface, not an abstract class, only formal.
   * <li>2012-08-12 Hartmut new: {@link #setLastModified(FileRemote, long)}. 
   * <li>2012-08-12 Hartmut bugfix: {@link #getChildren(FileRemote, FileFilter)} here only abstract.
   * <li>2012-08-12 Hartmut new: {@link #openInputStream(FileRemote, long)}
   * <li>2012-08-12 Hartmut new: {@link #getChildren(FileRemote, FileFilter)} implemented here.
   * <li>2012-08-12 Hartmut chg: up to now this is not an interface but an abstract class. It contains common method implementation.
   *   An derivation (or implementation of the interface before that change) may not need other base classes.
   * <li>2012-08-03 Hartmut chg: Usage of Event in FileRemote. 
   *   The FileRemoteAccessor.Commission is removed yet. The same instance FileRemote.Callback, now named FileRemote.FileRemoteEvent is used for forward event (commision) and back event.
   * <li>2012-07-28 Hartmut new: Concept of remote files enhanced with respect to {@link FileAccessZip},
   *   see {@link FileRemote}
   * <li>2012-03-10 Hartmut new: {@link Commission#newDate} etc. 
   *   for {@link FileRemote#chgProps(String, int, int, long, org.vishia.fileRemote.FileRemote.CallbackEvent)}.
   * <li>2012-01-09 Hartmut new: This class extends from Closeable, because an implementation 
   *  may have an running thread which is need to close. A device should be closeable any time.
   * <li>2012-01-06 Hartmut new {@link #refreshFileProperties(FileRemote)}. 
   * <li>2011-12-31 Hartmut new {@link Commission} and {@link #addCommission(Commission)}. It is used
   *   to add commissions to the implementation class to do in another thread/via communication.
   * <li>2011-12-10 Hartmut creation: Firstly only the {@link FileRemoteAccessorLocalFile} is written.
   *   
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
  public static final int version = 20120310;
  
  //public final static int kOperation = 0xd00000, kFinishOk = 0xf10000, kFinishNok = 0xf10001
  //, kFinishError = 0xf1e3303, kNrofFilesAndBytes = 0xd00001, kCopyDir = 0xd0cd13;


  protected FileRemoteAccessor ( ) { //String name, EventSource source, EventConsumer consumer, EventThread_ifc evThread ) {
    
    //super(name, source, consumer, evThread);
  }
  
  
  /**Activates working of the devide, thread starting, communication establishing etc. */
  public abstract void activate();
  
  
  public abstract void close();
  
  /**Returns a unique absolute path for the file regarding maybe tmp, home, environment variables etc.
   * @param path given path
   * @return path to get the file.
   */
  public abstract CharSequence completeFilePath(CharSequence path);
  
  
  /**Gets the properties of the file from the physical file.
   * @param file the destination file object.
   * @param callback If null then the method should access the file system immediately in this thread.
   *   It means it is possible that this needs a longer time for wait for response 
   *   from a maybe remote file system with a suitable timeout.
   *   <br> 
   *   If the callback is given, the implementor should install a temporary thread
   *   which does the access and calls the callback on finishing. 
   *   This operation returns immediately. 
   *   That is especially proper if the operation is called in an event procedure in a graphic system. 
   *   Prevent scrolling wheel on the screen. 
   */
  public abstract void refreshFileProperties(FileRemote file, EventWithDst<FileRemoteProgressEvData,?> evBack);

  
  /**Common form of a command with the file.
   * @param bWait true then wait for fulfill. If it is a local file (depends on {@link FileRemote#device()},
   *   then the execution is immediately done in the same thread.
   *   If it is especially a file on a remote device only able to reach via communication 
   *   ( for example on an embedded device), the operation should wait for success.
   * @param co Command description. It is the payload of an command event. 
   * @param evBack prepared event for back information or also for progress.
   *   The event knows a destination where the back event is processed. 
   *   If this argument is null, no back information will be sent. This is sensible if bWait is true,
   *   and if the caller can check what is happen. 
   *   Hint: Usual an instance of {@link FileRemoteProgress} can be used to execute the event, 
   *   and also to wait for success on user level.
   * @return null if executed in another thread (bWait = false), else return null if no error.
   * @since 2023-03, the new concept.
   *   
   */
  public abstract String cmd(boolean bWait, FileRemote.CmdEventData co, EventWithDst<FileRemoteProgressEvData,?> evBack);
  
  

  
  /**Gets files and sub directories of a directory. This method uses the {@link java.io.File} access methods to get the children of this file.
   * @param file parent directory.
   * @param filter maybe a filter
   * @return The list of that file's children with given filter.
   */
  public abstract List<File> getChildren(FileRemote file, FileFilter filter);
  
  
  
  protected abstract boolean setLastModified(FileRemote file, long time);
  
  
  public abstract boolean createNewFile(FileRemote file, EventWithDst<FileRemoteProgressEvData,?> evBack) throws IOException;

  
  //public abstract String moveFile(FileRemote src, FileRemote dst, EventWithDst<FileRemoteProgressEvData,?> evBack);
  
  //public abstract String copyFile(FileRemote src, FileRemote dst, EventWithDst<FileRemoteProgressEvData,?> evBack);
  
  /**Try to delete the file.
   * @param callback
   * @return If the callback is null, the method returns if the file is deleted or it can't be deleted.
   *   The it returns true if the file is deleted successfully. If the callback is not null, it returns true.
   */
  public abstract boolean delete(FileRemote file, EventWithDst<FileRemoteProgressEvData,?> evBack);
  
  /**
   * @param file
   * @param subdirs true then create all necessary dirs on the path
   * @param evBack It will be called on any directory which was made, to update the local instance of FileRemote.
   * @return
   */
  public abstract boolean mkdir(FileRemote file, boolean subdirs, EventWithDst<FileRemoteProgressEvData,?> evBack);
  
  
  /**Copies all files which are checked before.
   * @param fileSrc dir or file as root for copy to the given pathDst
   * @param pathDst String given destination for the copy
   * @param nameModification Modification for each name. null then no modification. TODO
   * @param mode One of the bits {@link FileRemote#modeCopyCreateYes} etc.
   * @param callbackUser Maybe null, elsewhere on every directory and file which is finished to copy a callback is invoked.
   * @param timeOrderProgress may be null, to show the progress of copy.
   */
  public abstract void copyChecked(FileRemote fileSrc, String pathDst, String nameModification, int mode, FileRemoteWalkerCallback callbackUser, FileRemoteProgressEvData timeOrderProgress);
  
  
  /**Search in all files.
   * @param fileSrc dir or file as root for search the given byte[]
   * @param callbackUser Maybe null, elsewhere on every directory and file which is finished to copy a callback is invoked.
   * @param timeOrderProgress may be null, to show the progress of copy.
   */
  public abstract void search(FileRemote fileSrc, byte[] search, FileRemoteWalkerCallback callbackUser, FileRemoteProgressEvData timeOrderProgress);
  
  
  public abstract ReadableByteChannel openRead(FileRemote file, long passPhase);
  
  /**Creates an InputStream with this fileRemote instance.
   * @param file
   * @param passPhase
   * @return
   */
  public abstract InputStream openInputStream(FileRemote file, long passPhase);
  
  public abstract WritableByteChannel openWrite(FileRemote file, long passPhase);
 
  /**Creates an OutputStream with this fileRemote instance.
   * @param file
   * @param passPhase
   * @return
   */
  public abstract OutputStream openOutputStream(FileRemote file, long passPhase);
  
  //FileRemote[] listFiles(FileRemote parent);
  
  /**Creates or prepares a CmdEvent to send to the correct destination. The event is ready to use but not  occupied yet. 
   * If the evBack contains a CmdEvent as its opponent, it is used. In that way a non-dynamic event management
   * is possible. */
//  public abstract FileRemote.CmdEvent prepareCmdEvent(int timeout, EventCmdtypeWithBackEvent<?, FileRemote.CmdEvent> evBack);

  
  public abstract boolean isLocalFileSystem();

  
  public abstract CharSequence getStateInfo();
  
  /**Abort currently running and saved copy, check etc. actions. */
  public abstract void abortAll ( );
  
  
  /**This class offers a Thread especially for {@link FileRemoteAccessor#walkFileTree(FileRemote, boolean, FileFilter, int, CallbackFile)}
   * which can be use for devices which can evaluate the files by immediately system calls without communication but with maybe waiting for response.
   * It is for the PC's file system especially. 
   */
  public static class FileWalkerThread extends Thread
  {
    final protected FileRemote startDir; 
    //final protected FileFilter filter; 
    final protected int markSet, markSetDir;
    final protected String sMask;
    final protected long bMarkCheck;
    final protected FileRemoteWalkerCallback callback;
    final protected boolean bRefresh;
    final protected int depth;
    final protected boolean debugOut;
    
    public FileWalkerThread(FileRemote startDir, boolean bRefreshChildren, int depth, int markSet, int markSetDir
        , String sMask, long bMarkCheck, FileRemoteWalkerCallback callback, boolean debugOut)
    { super("FileRemoteRefresh");
      this.startDir = startDir;
      this.bRefresh = bRefreshChildren;
      this.markSet = markSet;
      this.markSetDir = markSetDir;
      this.depth = depth;
      //this.filter = filter;
      this.sMask = sMask;
      this.bMarkCheck = bMarkCheck;
      this.callback = callback;
      this.debugOut = debugOut;
    }
    
  }
  
  


}
