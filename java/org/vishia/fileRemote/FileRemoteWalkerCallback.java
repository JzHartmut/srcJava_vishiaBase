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
  
  /**Files with a lesser difference in time (2 sec) are seen as equal in time stamp.*/
  protected long minDiffTimestamp = 2000; 
  
 
  protected boolean aborted = false;
  
  public FileRemoteWalkerCallback(FileRemote dir1, FileRemote dir2, FileRemoteWalkerCallback callbackUser, EventWithDst<FileRemoteProgressEvData,?> evBack) { //FileRemote.CallbackEvent evCallback){
    //this.evCallback = evCallback;
    //this.evWalker2 = new FileRemoteWalkerEvent("", dir2.device(), null, null, 0);
    this.evBack = evBack;
    this.progress = evBack == null ? null : evBack.data();
    this.callbackUser = callbackUser;
    this.dir1Base = dir1; 
    this.dir2Base = dir2;
    this.basepath1 = FileFunctions.normalizePath(dir1.getAbsolutePath()).toString();
    this.zBasePath1 = this.basepath1.length();
  }

  
  protected void prepareDirs (FileRemote dir, boolean bCreateDir2) {
    this.dir1Curr = dir;
    if(this.dir2Base !=null) {                   //========vv get or create dir2Curr from existing dir2Curr and dir.
      CharSequence path = FileFunctions.normalizePath(dir.getAbsolutePath());
      if(path.length() <= this.zBasePath1){
        this.dir2Curr = this.dir2Base;                             // the first offerParentNode with dir as same as dir2Base
        //it should be file == dir1, but there is a second instance of the start directory.
        //System.err.println("FileRemoteCallbackCmp - faulty FileRemote; " + path);
        //return Result.cont;
      } else {
        if(bCreateDir2) {
          //Build dir2sub with the local path from dir1:
          CharSequence localPath = path.subSequence(this.zBasePath1+1, path.length());
          //if(StringFunctions.equals(localPath, "functionBlocks")) Debugutil.stopp();
          //System.out.println("FileRemoteCallbackCmp - dir; " + localPath);
          this.dir2Curr = this.dir2Base.subdir(localPath);
        } else {                                           // TODO do not create the sub dir if it is not physically existing, with an additonal parameter
          CharSequence localPath = path.subSequence(this.zBasePath1+1, path.length());
          this.dir2Curr = this.dir2Base.subdir(localPath);
//          this.dir2Curr = this.dir2Curr.getChild(dir.getName());  // it is null if not refreshed and existing.
        }
      }
    }
    if(this.progress !=null) {
      this.progress.currDir = dir;
    }
  }

  
  protected void restoreDirs () {
    this.dir1Curr = this.dir1Curr.getParentFile();
    if(this.dir2Curr !=null) {
      this.dir2Curr = this.dir2Curr.getParentFile();
    }
  }

  
  /**Get the second file for given file from walker
   * in the given {@link #dir2Curr}.
   * @param file from walker
   * @return null if the file2 is not given as children. 
   */
  protected FileRemote getFile2(FileRemote file, boolean bCreate) {
    String name = file.getName();
    FileRemote file2;
    if(bCreate) {
       file2 = this.dir2Curr.child(name);                  //creates the child if not given;
    } else {
      file2 = this.dir2Curr.getChild(name);                // null if not exists
    }
    return file2;
  }
  
  
  @Override public boolean shouldAborted(){
    return this.aborted;
  }

  @Override public void finished(FileRemote startDir)
  {
    if(this.evBack !=null) {
      this.progress.done(null, null); 
      //progress.show(FileRemote.CallbackCmd.done, null);
    }
    /*
    if(evCallback !=null && evCallback.occupyRecall(500, null, true) !=0){
      evCallback.sendEvent(FileRemote.CallbackCmd.done);
    }
    */
  }



}
