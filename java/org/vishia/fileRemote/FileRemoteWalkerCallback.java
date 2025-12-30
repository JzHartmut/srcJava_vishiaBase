package org.vishia.fileRemote;

import java.nio.file.Path;

import org.vishia.event.EventWithDst;
import org.vishia.util.FileFunctions;
import org.vishia.util.SortedTreeWalkerCallback;


/**This interface is used as callback for 
 * {@link FileRemoteAccessor#walkFileTreeCheck(FileRemote, boolean, boolean, boolean, String, long, int, FileRemoteCallback)}
 * It is similar like the concept of {@link java.nio.file.Files#walkFileTree(java.nio.file.Path, java.nio.file.FileVisitor)}
 * with its visitor interface. But it is implemented for Java6-usage too, usable as common approach.
 */
public abstract class FileRemoteWalkerCallback implements SortedTreeWalkerCallback<FileRemote, FileRemoteCmdEventData> {

  /**The given start directories for both trees. */
  protected final FileRemote dir1Base, dir2Base;
  
  /**The given current directories set in {@link #offerParentNode(FileRemote, Object, Object)},
   * used in {@link #offerLeafNode(FileRemote, Object)}, 
   * set to null in {@link #offerLeafNode(FileRemote, Object)} for both trees. */
  protected FileRemote dir1Curr, dir2Curr;
  
  /**Only stored for debug, set in ctor*/
  protected final String basepath1;
  
  /**Length of the base path given with ctor dir1, to build the relative path
   * from dir2 with the same substring startet here.
   */
  protected final int zBasePath1;
  
  /**Event instance for user callback. */
  protected final EventWithDst<FileRemoteProgressEvData,?> evBack;  //FileRemote.CallbackEvent evCallback;
  
  protected final FileRemoteProgressEvData progress;
  
  /**Event to walk through the second tree.
   * 
   */
  //private final FileRemoteWalkerEvent evWalker2;
  
  protected final FileRemoteWalkerCallback callbackUser;
  
  /**one of the bits
   * <br> {@link FileCompare#onlyTimestamp} = {@value FileCompare#onlyTimestamp}
   * <br> {@link FileCompare#withoutLineend} = {@value FileCompare#withoutLineend}
   * <br> {@link FileCompare#withoutEndlineComment} = {@value FileCompare#withoutEndlineComment}
   * <br> {@link FileCompare#withoutComment} = {@value FileCompare#withoutComment}
   * used for the comparison itself.
   */
  protected int mode;
  
  /**Files with a lesser difference in time (2 sec) are seen as equal in time stamp.*/
  protected long minDiffTimestamp = 2000; 
  
 
  protected boolean aborted = false;
  
  public FileRemoteWalkerCallback(FileRemote dir1, FileRemote dir2, int cmpMode, FileRemoteWalkerCallback callbackUser, EventWithDst<FileRemoteProgressEvData,?> evBack) { //FileRemote.CallbackEvent evCallback){
    //this.evCallback = evCallback;
    //this.evWalker2 = new FileRemoteWalkerEvent("", dir2.device(), null, null, 0);
    this.evBack = evBack;
    this.progress = evBack == null ? null : evBack.data();
    this.callbackUser = callbackUser;
    this.dir1Base = dir1; this.dir2Base = dir2;
    this.mode = cmpMode;
    this.basepath1 = FileFunctions.normalizePath(dir1.getAbsolutePath()).toString();
    this.zBasePath1 = this.basepath1.length();
  }

}
