package org.vishia.util;




/**This interface is used as callback for any walk action through a tree structure.
 * It is similar like the concept of {@link java.nio.file.Files#walkFileTree(java.nio.file.Path, java.nio.file.FileVisitor)}
 * with its visitor interface. But it is used for more as file access.
 */
public interface TreeNodeCallback<DerivedNode extends TreeNodeBase<DerivedNode, ?, ?>>
{
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

  /**Invoked before start of {@link java.nio.file.Files#walkFileTree(java.nio.file.Path, java.util.Set, int, java.nio.file.FileVisitor)}.
   * or an adequate communication.
   */
  void start();
  
  /**Invoked for any parent which has children.
   * @param node a node which has children.
   * @return information to abort
   */
  Result offerParent(DerivedNode node);
  
  /**Invoked on end of walking through children of its parent.
   * @param node a node which has children.
   * @return information to abort
   */
  Result finishedChildren(DerivedNode node);
  
  /**Invoked for any node which has no children.
   * @param node a node which has children.
   * @return information to abort
   */
  Result offerLeaf(DerivedNode node);
  
  /**Invoked after finishing a .
   */
  void finished();
  
  
  /**Checks whether the tree walking should be terminated respectively aborted.
   * This routine is called in any walk tree routine.
   * The aborted state should be set from outside in the implementation classes.
   * @return true if should terminated.
   */
  boolean shouldAborted();
}


class CallbackTemplate<DerivedNode  extends TreeNodeBase<DerivedNode, ?, ?>>  implements TreeNodeCallback<DerivedNode>
{

  @Override public void start() {  }
  @Override public void finished() {  }

  @Override public Result offerParent(DerivedNode node) {
    return Result.cont;      
  }
  
  @Override public Result finishedChildren(DerivedNode node) {
    return Result.cont;      
  }
  
  

  @Override public Result offerLeaf(DerivedNode node) {
    return Result.cont;
  }

  
  @Override public boolean shouldAborted(){
    return false;
  }
  
};

