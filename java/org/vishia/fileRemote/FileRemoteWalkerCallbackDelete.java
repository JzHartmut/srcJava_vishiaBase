package org.vishia.fileRemote;

import org.vishia.util.SortedTreeWalkerCallback;

/**This callback operations are used to delete files from walking to the FileRemote instances
 * using {@link FileRemote#walkLocal(org.vishia.fileRemote.FileRemoteCmdEventData.Cmd, FileRemote, int, int, String, int, int, FileRemoteWalkerCallback, FileRemoteCmdEventData, int, org.vishia.event.EventWithDst)}
 * An instance of this class is used for callback.
 * For the deletion itself it calls an operations of FileRemoteAccessor to delete on the device.
 * Either one file is deleted, or a directory, and then with all children should be deleted.
 * 
 * @author hartmut
 *
 */
public class FileRemoteWalkerCallbackDelete implements SortedTreeWalkerCallback<FileRemote, FileRemoteCmdEventData> { //extends FileRemoteWalkerCallback {

  final FileRemoteProgressEventConsumer evConsumer;

  
  protected FileRemoteWalkerCallbackDelete() {
    this.evConsumer = new FileRemoteProgressEventConsumer("FileRemoteWalkerCallbackDelete", null, null);
  }

  @Override public void start ( FileRemote startNode, FileRemoteCmdEventData startInfo ) {
    // left empty
  }

  @Override public Result offerParentNode ( FileRemote parentNode, Object data, Object oWalkInfo ) {
    parentNode.deleteFilesDirTree(false, 0, "**/*", FileMark.ignoreSymbolicLinks, this.evConsumer.evBack);
    this.evConsumer.awaitExecution(0, true);
    return Result.skipSubtree;   // all is deleted, do not look for content. 
  }

  @Override public Result finishedParentNode ( FileRemote parentNode, Object data, Object oWalkInfo ) {
    return Result.cont;
  }

  @Override public Result offerLeafNode ( FileRemote leafNode, Object leafNodeData ) {
    leafNode.delete(this.evConsumer.evBack);
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
