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

import org.vishia.event.EventCmdtypeWithBackEvent;

/**Interface for instances, which organizes a remote access to files.
 * One instance per transfer protocol are need.
 * 
 * @author Hartmut Schorrig
 *
 */
public abstract class FileRemoteAccessor implements Closeable
{
  /**Version, history and license.
   * <ul>
   * <li>2012-05-30 Hartmut new: {@link #openOutputStream(FileRemote, long)}
   *   Note: it may be that the {@link #openRead(FileRemote, long)} and {@link #openWrite(FileRemote, long)}
   *   is not proper for some requirements, working with the traditional streams may be better.
   * <li>2015-05-30 Hartmut new: {@link #walkFileTreeCheck(FileRemote, boolean, boolean, boolean, String, long, int, FileRemoteCallback)}
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


  protected FileRemoteAccessor(){
    
  }
  
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
  public abstract void refreshFileProperties(FileRemote file, FileRemote.CallbackEvent callback);

  /**Gets the properties and the children of the file from the physical file.
   * <br><br>
   * The properties of the children are not gotten for the Standard-PC-Filesystem using Java-6. 
   * It may use too many calculation time.
   * Use {@link #refreshFileProperties(FileRemote, org.vishia.fileRemote.FileRemote.CallbackEvent)}
   * in a loop for any file only if it is necessary. Check the {@link FileRemote#isTested(long)} therefore.
   * <br><br>
   * For Java-7 the main properties of all children are gotten too, because the system call deliver it.
   * To document that, the {@link FileRemote#timeRefresh} is set to the new time.
   * For other remote file systems it should be also do so. Usual the main properties of the children
   * should be present if the name of the children is gotten. The main properties are length, 
   * timestamp last modified, read or write able.
   * 
   * @param file the destination file object.
   * @param callback If null then the method waits for response from the maybe remote file system
   *   with a suitable timeout. 
   *   If not null then the method may return immediately without any waiting
   *   and the callback method in the {@link EventCmdPingPongType#callback()} is invoked maybe in another thread
   *   if the answer is gotten. 
   */
  public abstract void refreshFilePropertiesAndChildren(FileRemote file, FileRemote.CallbackEvent callback);

  
  /**Gets files and sub directories of a directory. This method uses the {@link java.io.File} access methods to get the children of this file.
   * @param file parent directory.
   * @param filter maybe a filter
   * @return The list of that file's children with given filter.
   */
  public abstract List<File> getChildren(FileRemote file, FileFilter filter);
  
  /**Walks through all children of the given file with given filter on the storage medium, maybe refreshes the files 
   * and inform the user on any directory entry and and file or directory via callback.
   * The callback may be done in another thread, because it may be a result of communication.
   * This routine may return immediately. It does not block if a communication is necessary.
   * <ul>
   * <li>{@link CallbackFile#start()} is called firstly. 
   * <li>{@link CallbackFile#offerParentNode(FileRemote)} is called on a new directory which is entered.
   * <li>{@link CallbackFile#offerLeafNode(FileRemote)} is called for any found entry in a directory. It may be a file or sub directory.
   * <li>{@link CallbackFile#finished()} is the last action of that.
   * </ul> 
   *  
   * @param startDir The start directory.
   * @param bWait true then waits for success. On return the walk through is finished and all callback routines are invoked already.
   *   false then this method may return immediately. The callback routines are not invoked. The walk is done in another thread.
   *   Note: Whether or not another thread is used for communication it is not defined with them. It is possible to start another thread
   *   and wait for success, for example if communication with a remote device is necessary. 
   * @param bRefresh if true then refreshes all entries in file and maybe found children. If false then let file unchanged.
   *   If filter is not null, only the filtered children will be updated,
   *   all other children remain unchanged. It means it is possible that non exists files are remain as children.
   * @param sMaskCheck Any filter which files will be accepted.
   * @param bMarkCheck Bits 31..0 which are used to select files to mark if this mark bits are set. Additional (AND) to the sMask.
   *    If 0 all files, marked or non marked are checked. The Bits 63..32 contains the number of levels to process. 
   *    It is especially 2 (0x200000000 in argument) if the first level of a directory should be checked. 
   * @param depth at least 1 for enter in the first directory. Use 0 if all levels should enter.
   *   If negative then the absolute is number of levels (maybe Integer.MAXVALUE) but uses the first level to enter only marked files.
   * @param callback this callback will be invoked on any file or directory.
   */
  public abstract void walkFileTree(FileRemote startDir, boolean bWait, boolean bRefreshChildren, boolean resetMark, String sMaskCheck, long bMarkCheck, int depth, FileRemoteCallback callback);
  
  
  public abstract void walkFileTreeCheck(FileRemote startDir, final boolean bWait, boolean bRefreshChildren, boolean resetMark, String sMask, long bMarkCheck, int depth, FileRemoteCallback callback);

  protected abstract boolean setLastModified(FileRemote file, long time);
  
  
  public abstract boolean createNewFile(FileRemote file, FileRemote.CallbackEvent callback) throws IOException;

  
  public abstract String moveFile(FileRemote src, FileRemote dst, FileRemote.CallbackEvent callback);
  
  public abstract void copyFile(FileRemote src, FileRemote dst, FileRemote.CallbackEvent callback);
  
  /**Try to delete the file.
   * @param callback
   * @return If the callback is null, the method returns if the file is deleted or it can't be deleted.
   *   The it returns true if the file is deleted successfully. If the callback is not null, it returns true.
   */
  public abstract boolean delete(FileRemote file, FileRemote.CallbackEvent callback);
  
  public abstract boolean mkdir(FileRemote file, boolean subdirs, FileRemote.CallbackEvent callback);
  
  
  /**Copies all files which are checked before.
   * @param fileSrc dir or file as root for copy to the given pathDst
   * @param pathDst String given destination for the copy
   * @param nameModification Modification for each name. null then no modification. TODO
   * @param mode One of the bits {@link FileRemote#modeCopyCreateYes} etc.
   * @param callbackUser Maybe null, elsewhere on every directory and file which is finished to copy a callback is invoked.
   * @param timeOrderProgress may be null, to show the progress of copy.
   */
  public abstract void copyChecked(FileRemote fileSrc, String pathDst, String nameModification, int mode, FileRemoteCallback callbackUser, FileRemoteProgressTimeOrder timeOrderProgress);
  
  
  /**Search in all files.
   * @param fileSrc dir or file as root for search the given byte[]
   * @param callbackUser Maybe null, elsewhere on every directory and file which is finished to copy a callback is invoked.
   * @param timeOrderProgress may be null, to show the progress of copy.
   */
  public abstract void search(FileRemote fileSrc, byte[] search, FileRemoteCallback callbackUser, FileRemoteProgressTimeOrder timeOrderProgress);
  
  
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
  public abstract FileRemote.CmdEvent prepareCmdEvent(int timeout, EventCmdtypeWithBackEvent<?, FileRemote.CmdEvent> evBack);

  
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
    final protected String sMask;
    final protected long bMarkCheck;
    final protected FileRemoteCallback callback;
    final protected boolean bRefresh, resetMark;
    final protected int depth;
    
    public FileWalkerThread(FileRemote startDir, boolean bRefreshChildren, boolean resetMark, int depth, String sMask, long bMarkCheck, FileRemoteCallback callback)
    { super("FileRemoteRefresh");
      this.startDir = startDir;
      this.bRefresh = bRefreshChildren;
      this.resetMark = resetMark;
      this.depth = depth;
      //this.filter = filter;
      this.sMask = sMask;
      this.bMarkCheck = bMarkCheck;
      this.callback = callback;
    }
    
  }
  
  


}
