package org.vishia.fileRemote;

public class FileRemoteTestCallback  implements FileRemoteWalkerCallback {

  @Override public void start ( FileRemote startNode, FileRemoteCmdEventData co ) {
    System.out.println("start callback test: " + startNode.toString());
  }

  @Override public Result offerParentNode ( FileRemote parentNode, Object oPath, Object oWalkInfo ) {
    System.out.println("callback test: dir = " + parentNode.toString());
    return Result.cont;
  }

  @Override public Result finishedParentNode ( FileRemote parentNode, Object oPath, Object oWalkInfo ) {
    System.out.println("callback test finishDir: " + parentNode.toString());
    return Result.cont;
  }

  @Override public Result offerLeafNode ( FileRemote leafNode, Object info ) {
    System.out.println("start callback file: " + leafNode.toString());
    return Result.cont;
  }

  @Override public void finished ( FileRemote startNode ) {
    System.out.println("finish callback test: " + startNode.toString());
  }

  @Override public boolean shouldAborted () {
    System.out.println("callback test should aborted?");
    return false;
  }

}
