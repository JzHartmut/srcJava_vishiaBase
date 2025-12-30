package org.vishia.fileLocalAccessor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.spi.FileSystemProvider;

import org.vishia.event.EventWithDst;
import org.vishia.fileRemote.FileRemote;
import org.vishia.fileRemote.FileRemoteCmdEventData;
import org.vishia.fileRemote.FileRemoteProgressEvData;
import org.vishia.fileRemote.FileRemoteWalkerCallback;
import org.vishia.util.Debugutil;

public class FileCallbackLocalMove  implements FileRemoteWalkerCallback {

  
  /**Version, history and license.
   * <ul>
   * <li>2025-12-27 created due to the schema of copy. Move was never supported by The.file.Commander in the past.
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
  static final public String sVersion = "2025-12-27";
  
  

  private FileRemote dirDst;
  
//  private boolean first;
  
  //private final String basepath1;
  //private final int zBasePath1;
  
  /**Event instance for user callback. */
  private final EventWithDst<FileRemoteProgressEvData,?> evBack;

  private final FileRemoteProgressEvData progress;
  
  private final FileRemoteWalkerCallback callbackUser;

  
  /**Constructs an instance to execute copy of files in a directory trees.
   * @param dirDstStart Destination directory due to the given first FileRemote source directory on start walking.
   * @param callbackUser usual null, possible as callback after move of one file.
   * @param evBack The back event for progress and finish.
   */
  public FileCallbackLocalMove(FileRemote dirDstStart, FileRemoteWalkerCallback callbackUser, EventWithDst<FileRemoteProgressEvData,?> evBack) { //FileRemote.CallbackEvent evCallback){
    this.evBack = evBack;
    this.progress = evBack.data();
    this.callbackUser = callbackUser;
    this.dirDst = dirDstStart;
  }

  
  
  @Override public void start ( FileRemote startDir, FileRemoteCmdEventData co) {  }
  
  
  
  @Override public Result offerParentNode ( FileRemote dir, Object oPath, Object filter) {
//    if(this.first){
//      this.first = false;  //first level: don't change dirDst. It matches to the first source dir.
//    } else {
      String name = dir.getName();
      this.dirDst = FileRemote.getDir(this.dirDst.getPathChars() + "/" + name);
      this.dirDst.mkdir();
      if(this.progress !=null) {
        this.progress.currDir = dir;
      }
//    }
    return Result.cont;
  }
  
  /**Checks whether all files are compared or whether there are alone files.
   */
  @Override public Result finishedParentNode(FileRemote file, Object oPath, Object oWalkInfo){
    this.dirDst = this.dirDst.getParentFile();
    return Result.cont;      
  }
  
  
  /**This method creates a new file in the current dst directory with the same name as the given file.
   * Note: because the FileRemote concept the file can be located on any remote device.
   * @see org.vishia.util.SortedTreeWalkerCallback#offerLeafNode(java.lang.Object)
   */
  @Override public Result offerLeafNode(FileRemote file, Object info) {
//    int repeat = 5;
    FileRemote fileDst = this.dirDst.child(file.getName());
    Path pathSrc = file.path();
    Path pathDst = fileDst.path();
    try{
//      FileStore fstoreSrc = Files.getFileStore(pathSrc);
//      FileStore fstoreDst = Files.getFileStore(pathDst.getParent());
      @SuppressWarnings("resource") FileSystemProvider provSrc = pathSrc.getFileSystem().provider();
      @SuppressWarnings("resource") FileSystemProvider provDst = pathDst.getParent().getFileSystem().provider();
      if(this.progress !=null) {
        this.progress.nrofBytesFile = file.length();
      }
      if(provSrc == provDst) {                   //--------vv files on the same device, the copy is faster than manually copy.
        Files.move(pathSrc, pathDst); //, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
        if(this.progress !=null) {
          this.progress.nrofBytesFileCopied = this.progress.nrofBytesFile;
        }
      } else {
        Debugutil.stopp();
      }                                                    // after copy tune also the date in FileRemote fileDst.
      fileDst.internalAccess().setLengthAndDate(file.length(), file.lastModified(), -1, -1);
      if(this.callbackUser !=null) {                       // it is a callback in the callback, usual not used.
        this.callbackUser.offerLeafNode(file, info);
      }
    } catch(IOException exc) {
      System.err.println(exc.toString());
    } finally {
    }
    return Result.cont;
  }

  
  
  @Override public boolean shouldAborted(){
    return false; 
  }

  
  
  
  
  
  
  @Override public void finished ( FileRemote startDir) { }


  

}
