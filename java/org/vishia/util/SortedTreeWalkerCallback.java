package org.vishia.util;


/**A Callback interface for walking through any tree with more as one children per node.
 * The concept is similar the {@link java.nio.file.Files#walkFileTree(java.nio.file.Path, java.nio.file.FileVisitor)} callback interface.
 * but with a more universal approach and concept.
 * @author Hartmut Schorrig
 *
 * @param <TypeNode> The type of a node which may have children or it is a leaf.
 * @param <TypeStartInfo> this is a specific type of data which describes the condition of walking 
 */
public interface SortedTreeWalkerCallback<TypeNode, TypeStartInfo>
{
  
  /**Version, history and license.
   * <ul>
   * <li>2023-07-15 Hartmut enhanced with TypeStartInfo and Object data for {@link #offerLeafNode(Object, Object)} etc.
   *   It is used for {@link org.vishia.fileRemote.FileRemoteWalkerCallback} to inform about the implementation data java.nio.file.Path
   * <li>2014-12-24 Hartmut created from the more special interface {@link org.vishia.fileRemote.FileRemoteWalkerCallback}.                  
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
   */
  public static final String sVersion = "2023-07-15";

  
  /**It is similar {@link java.nio.file.FileVisitResult}.
   * This class is defined here because it runs with Java-6 too.
   */
  public enum Result
  {
    cont
    , terminate
    , skipSiblings
    , skipSubtree
  }

  
  /**This class contains the number of files etc. for callback.
   * @author hartmut
   *
   */
//  public static class Counters
//  { /**Any number of internal data. */
//    public long nrofBytes;
//    
//    /**Number of parent nodes and number of leaf nodes which are processed. */
//    public int nrofParents, nrofLeafss;
//  
//    /**Number of parent nodes and number of leaf nodes which are selected. */
//    public int nrofParentSelected, nrofLeafSelected;
//    
//    public void clear(){ nrofBytes = 0; nrofParents = nrofLeafss = nrofParentSelected = nrofLeafSelected = 0; }
//    
//  }
  
  /**Invoked before start of a walk through the tree.
   */
  void start(TypeNode startNode, TypeStartInfo startInfo);
  
  /**Invoked on start on walking through a parent node which have children. It may be a directory inside a file tree. 
   * If this method is invoked for a node, the {@link #offerLeafNode(TypeNode)} is not invoked.
   * @param parentNode
   * @param data Specific data presentation of the node
   * @param oWalkInfo internal possible information about walking, depending on usage.
   * @return information to abort, maybe boolean.
   */
  Result offerParentNode(TypeNode parentNode, Object data, Object oWalkInfo);
  
  /**Invoked on end of walking through a parent node.
   * @param parentNode the node which was walked through
   * @param data Specific data presentation of the node
   * @param oWalkInfo internal possible information about walking, depending on usage.
   */
  Result finishedParentNode(TypeNode parentNode, Object data, Object oWalkInfo);
  
  /**Invoked for any node which has no children or which is not processed because the depth of walking through the tree is reached.
   * It is called in opposite to {@link #offerLeafNode(Object)}, only one of both is called for a node.
   * For example it is invoked for a sub directory only if the depth is reached and {@link #offerParentNode(TypeNode)} is not called.
   * @param leafNode
   * @param data Specific data presentation of the node
   * @return information to abort or continue.
   */
  Result offerLeafNode(TypeNode leafNode, Object leafNodeData);
  
  /**Invoked after finishing the walking through.
   */
  void finished(TypeNode startNode);
  
  
  /**Returns true if the tree walking should be terminated respectively aborted.
   * This routine should be called in any walk tree routine.
   * The aborted state should be set from outside in the implementation classes.
   * @return true if should terminated.
   */
  boolean shouldAborted();

  
  
  /**Use this as template for anonymous implementation. Frequently without 'static'.*/
  static SortedTreeWalkerCallback<Object, Object> callbackTemplate = new SortedTreeWalkerCallback<Object, Object>()
  {

    /**It is called on start of walking. 
     *
     */
    @Override public void start(Object startDir, Object info) {  }
    
    @Override public void finished(Object parentNode) {  }

    @Override public Result offerParentNode(Object file, Object oPath, Object filter) {
      return Result.cont;      
    }
    
    @Override public Result finishedParentNode(Object file, Object oData, Object oInfo) {
      return Result.cont;      
    }
    
    

    @Override public Result offerLeafNode(Object file, Object info) {
      return Result.cont;
    }

    
    @Override public boolean shouldAborted(){
      return false;
    }
    
  };
  
  

  
}
