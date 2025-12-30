package org.vishia.fileRemote;

import org.vishia.fileRemote.FileRemoteCmdEventData;

/**This callback worker works together with {@link FileRemoteWalker} to handle some selected files in a tree.
 * The tree starts with the directory on {@link #start(FileRemote, FileRemoteCmdEventData)} which is the not copied src root dir
 * adequate to the not copied given destination dir in 
 * @author hartmut
 *
 */
public class FileRemoteCallbackCopyDispersedFiles extends FileRemoteWalkerCallback
{
  
  /**Version, history and license.
   * <ul>
   * <li>2023-07-19 Hartmut created. It is used by the new FileRemoteWalker to copy some selected files.
   *   The idea handle only a few files is old, but never before consequently put into practice.
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
  static final public String sVersion = "2023-02-10";

  @Override public void start ( FileRemote startNode, FileRemoteCmdEventData startInfo ) {
    // TODO Auto-generated method stub
    
  }

  @Override public Result offerParentNode ( FileRemote parentNode, Object data, Object oWalkInfo ) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override public Result finishedParentNode ( FileRemote parentNode, Object data, Object oWalkInfo ) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override public Result offerLeafNode ( FileRemote leafNode, Object leafNodeData ) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override public void finished ( FileRemote startNode ) {
    // TODO Auto-generated method stub
    
  }

  @Override public boolean shouldAborted () {
    // TODO Auto-generated method stub
    return false;
  }
 

}
