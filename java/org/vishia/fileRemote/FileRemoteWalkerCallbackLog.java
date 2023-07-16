package org.vishia.fileRemote;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

import org.vishia.msgDispatch.LogMessageStream;
import org.vishia.util.OutputTwice;
import org.vishia.util.SortedTreeWalkerCallback;
import org.vishia.util.TreeNodeCallback;

/**This class can be used to log all activity on walking through a file tree.
 * It can be used also as template for own similar walkers, or to set for breakpoints.
 * @author hartmut Schorrig
 *
 */
public class FileRemoteWalkerCallbackLog implements FileRemoteWalkerCallback {

  
  final LogMessageStream logout;
  
  final boolean bAnyFile;
  //OutputTwice out2;
  
  
  public FileRemoteWalkerCallbackLog(OutputStream out1, OutputStream out2, Appendable out3, boolean bAnyFile) {
    this.logout = new LogMessageStream(out1, out2, out3, true, java.nio.charset.Charset.forName("UTF-8"));
    this.bAnyFile = bAnyFile;
  }
  
  public FileRemoteWalkerCallbackLog(File fOut, OutputStream out2, Appendable out3, boolean bAnyFile) throws IOException {
    this.bAnyFile = bAnyFile;
    @SuppressWarnings("resource")           //note: closed in LogMessageStream because of closeOnClose
    OutputStream out1 = new FileOutputStream(fOut);
    boolean closeOnClose = true;
    this.logout = new LogMessageStream(out1, out2, out3, closeOnClose, Charset.forName("UTF-8"));
  }
  
  
  @Override public void start ( FileRemote startNode, FileRemote.CmdEventData co ) {
    this.logout.sendMsg(1, "start: %s", startNode.getAbsolutePath());
  }

  @Override public Result offerParentNode ( FileRemote parentNode, Object oPath, Object walkInfo ) {
    this.logout.sendMsg(1, "start: %s", parentNode.getAbsolutePath());
    return SortedTreeWalkerCallback.Result.cont;
  }

  @Override public Result finishedParentNode ( FileRemote parentNode, Object oPath, Object oWalkInfo ) {
    this.logout.sendMsg(1, "finish: %s", parentNode.getAbsolutePath());
    return SortedTreeWalkerCallback.Result.cont;
  }

  @Override public Result offerLeafNode ( FileRemote leafNode, Object info ) {
    if(bAnyFile) {
      this.logout.sendMsg(1, "file: %s", leafNode.getAbsolutePath());
    }
    return SortedTreeWalkerCallback.Result.cont;
  }

  @Override public void finished ( FileRemote startNode ) {
    this.logout.sendMsg(1, "done: %s", startNode.getAbsolutePath());
  }

  @Override public boolean shouldAborted () {
    // TODO Auto-generated method stub
    return false;
  }

}
