package org.vishia.fileRemote;

import org.vishia.event.EventWithDst;
import org.vishia.event.Payload;

/**This callback operations are used to copy files from walking to the FileRemote instances
 * using {@link FileRemote#walkLocal(org.vishia.fileRemote.FileRemoteCmdEventData.Cmd, FileRemote, int, int, String, int, int, FileRemoteWalkerCallback, FileRemoteCmdEventData, int, org.vishia.event.EventWithDst)}
 * An instance of this class is used for callback.
 * For the copy itself it calls an operations of FileRemoteAccessor to copy on the device.
 * Either one file is copied, or the directory tree is created which are designated, and then children should be copied.
 * 
 * @author hartmut
 *
 */
public class FileRemoteWalkerCallbackCopy  extends FileRemoteWalkerCallback {

  final FileRemoteProgressEventConsumer evConsumer;
  
  FileRemote srcRootDir, dstRootDir, dstDir;
  
  public FileRemoteWalkerCallbackCopy ( ) {
    this.evConsumer = new FileRemoteProgressEventConsumer("FileRemoteWalkerCallbackCopy", null, null);
  }

  public void cleanSetDstDir(FileRemote dstDir) {
    this.dstRootDir = dstDir;
    this.dstDir = dstDir;
  }
  
  @Override public void start ( FileRemote startNode, FileRemoteCmdEventData startInfo ) {
    this.srcRootDir = startNode;
  }

  @Override public Result offerParentNode ( FileRemote parentNode, Object data, Object oWalkInfo ) {
    String name = parentNode.getName();
    this.dstDir = this.dstDir.subdir(name);
    this.dstDir.mkdir(true, this.evConsumer.evBack);
    this.evConsumer.awaitExecution(0, true);
    return Result.cont;   // all is deleted, do not look for content. 
  }

  @Override public Result finishedParentNode ( FileRemote parentNode, Object data, Object oWalkInfo ) {
    this.dstDir = this.dstDir.getParentFile();
    return Result.cont;
  }

  @Override public Result offerLeafNode ( FileRemote leafNode, Object leafNodeData ) {
    String sName = leafNode.getName();
    FileRemote dst = this.dstDir.child(sName);
    leafNode.copyTo(dst, this.evConsumer.evBack);
    this.evConsumer.awaitExecution(0, true);
    return Result.cont;
  }

  @Override public void finished ( FileRemote startNode ) {
    // left empty
  }

  @Override public boolean shouldAborted () {
    return false;
  }



}
