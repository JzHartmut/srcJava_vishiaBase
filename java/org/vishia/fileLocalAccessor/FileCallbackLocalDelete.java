package org.vishia.fileLocalAccessor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.vishia.event.EventWithDst;
import org.vishia.fileRemote.FileRemote;
import org.vishia.fileRemote.FileRemoteCmdEventData;
import org.vishia.fileRemote.FileRemoteProgressEvData;
import org.vishia.fileRemote.FileRemoteWalker;
import org.vishia.fileRemote.FileRemoteWalkerCallback;
import org.vishia.util.FilepathFilterM;

public class FileCallbackLocalDelete implements FileRemoteWalkerCallback{

  /**Version, history and license.
   * <ul>
   * <li>2023-04-09 improve: #offerLeafNode: Use {@link Files#copy(Path, Path, CopyOption...) if possible.
   *   It is faster if both files are in network on the same drive. No network data transmission clarified by OS level. 
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
  static final public String sVersion = "2023-07-15";
  
  
  private final EventWithDst<FileRemoteProgressEvData,?> evBack;

  public FileCallbackLocalDelete(EventWithDst<FileRemoteProgressEvData,?> evBack) { //FileRemote.CallbackEvent evCallback){
    //this.evCallback = evCallback;
    this.evBack = evBack;
  }

  @Override public void start ( FileRemote startNode, FileRemoteCmdEventData co ) { }

  @Override public Result offerParentNode ( FileRemote parentNode, Object data, Object oWalkInfo ) {
    return Result.cont;
  }

  @Override public Result offerLeafNode ( FileRemote fileRemote, Object oPath ) {
    try{ 
      Path path = (Path)oPath;                               // The FileLocalAccessor offers the java.nio.file.Path
      Files.delete(path);
      fileRemote.internalAccess().setDeleted(); 
    }
    catch(IOException exc) {
      if(this.evBack !=null) {
        FileRemoteProgressEvData data = this.evBack.data();
        //data.answerToCmd
        data.currFile = fileRemote;
        data.setDone(false, exc.getMessage());
        data.progressCmd = FileRemoteProgressEvData.ProgressCmd.error;
        this.evBack.sendEvent(this);
      }
    }
    return Result.cont;
  }

  @Override public Result finishedParentNode ( FileRemote dirRemote, Object oPath, Object oWalkInfo ) {
    FileRemoteWalker.WalkInfo walkInfo = (FileRemoteWalker.WalkInfo) oWalkInfo;
    FilepathFilterM filter = walkInfo.fileFilter;
    if(filter == null || filter.selAllDirEntries() && filter.selAllFilesInDir()) {
      try{ 
        Path path = (Path)oPath;                           // The FileLocalAccessor offers the java.nio.file.Path
        Files.delete(path); 
        dirRemote.internalAccess().setDeleted(); 
      }
      catch(IOException exc) {                             // especially if the dir is not empty.
        if(this.evBack !=null) {
          FileRemoteProgressEvData data = this.evBack.data();
          //data.answerToCmd
          data.currFile = dirRemote;
          data.setDone(false, exc.getMessage());
          data.progressCmd = FileRemoteProgressEvData.ProgressCmd.error;
          this.evBack.sendEvent(this);
        }
      }
    }
    return Result.cont;
  }

  @Override public void finished ( FileRemote startNode ) { }

  @Override public boolean shouldAborted () {
    return false;
  }

}
