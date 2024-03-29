package org.vishia.util;

/**This class is a tree walker which checks the names of nodes with a mask path.
 * @author Hartmut Schorrig
 *
 * @param <Type>
 */
public class TreeWalkerPathCheck implements SortedTreeWalkerCallback<String, Object>
{
  /**Version, history and license.
   * <ul>
   * <li>2023-07-15 adapted to changed SortedTreeWalkerCallback
   * <li>2015-05-25 Hartmut created for walking through a file tree but with universal approach.                  
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
  public static final String sVersion = "2015-05-25";


  
  /**Data chained from a first parent to deepness of dir tree for each level.
   * This data are created while {@link FileAccessorLocalJava7#walkFileTree(FileRemote, FileFilter, int, FileRemoteCallback)} runs.
   * It holds the gathered children from the walker. The children are stored inside the {@link #dir}
   * only on {@link WalkFileTreeVisitor#postVisitDirectory(Path, IOException)}
   */
  private class CurrDirChildren{
    /**The directory of the level. */
    String dir;
    
    
    int levelProcessMarked;
    
    PathCheck pathCheck;
    
    /**parallel structure of all children.
     * The child entries are gotten from the dir via {@link FileCluster#getFile(CharSequence, CharSequence, boolean)}. It means, existing children
     * are gotten from the existing {@link FileRemote} instances. They are written in this map while walking through the directory.
     * After walking, in {@link WalkFileTreeVisitor#postVisitDirectory(Path, IOException)}, the {@link #dir}.{@link FileRemote#children()}
     * are replaced by this instance because it contains only existing children. {@link FileRemote} instances for non existing children are removed then.
     */
    //Map<String,FileRemote> children;
    /**The parent. null on first parent. */
    CurrDirChildren parent;
    
    CurrDirChildren(String dir, PathCheck check, CurrDirChildren parent){
      this.dir = dir; this.parent = parent; this.pathCheck = check;
      this.levelProcessMarked = (parent == null) ? 0: parent.levelProcessMarked -1;
    }
    
    @Override public String toString(){ return this.dir + ": " + this.pathCheck; }
  }

  
  
  //final SortedTreeWalkerCallback<Type> callback;
  
  final PathCheck check;
  
  private CurrDirChildren curr;

  
  public TreeWalkerPathCheck(String sPathCheck) {
    //this.callback = callback;
    this.check = new PathCheck(sPathCheck);
  }
  
  @Override public void start(String startNode, Object info){ } //callback.start(startNode); }

  @Override public SortedTreeWalkerCallback.Result offerParentNode(String sName, Object data, Object walkInfo)
  {
    //String sName = node instanceof TreeNodeNamed_ifc ? ((TreeNodeNamed_ifc)node).getName() : node.toString();
    PathCheck use;
    if(this.curr != null){ use = this.curr.pathCheck; }
    else { use = this.check; }  //the first level.
    PathCheck ret = use.check(sName, true);
    if(ret == null){ return Result.skipSubtree; }
    else {
      this.curr = new CurrDirChildren(sName, ret, this.curr);
      return Result.cont; //callback.offerParentNode(node);
    }
  }

  
  
  @Override public SortedTreeWalkerCallback.Result finishedParentNode(String parentNode, Object oPath, Object oWalkInfo)
  {
    //checkRet[0] = check.bAllTree ? check : check.parent;
    this.curr = this.curr.parent;
    return Result.cont; //callback.finishedParentNode(parentNode, cnt);
  }

  @Override public SortedTreeWalkerCallback.Result offerLeafNode(String sName, Object info) {
    if(this.curr ==null)
      Debugutil.stop();
    assert(this.curr !=null);  //it is set in offerParentNode
    PathCheck use =  this.curr.pathCheck;
    PathCheck ret = use.next !=null ? null : use.check(sName, false); //it should be the last.
    if(ret == null){ return Result.skipSubtree; }
    else return Result.cont; //callback.offerLeafNode(leafNode);
  }

  @Override public void finished(String startNode)
  {
    //callback.finished(startNode, cnt);
    
  }

  @Override public boolean shouldAborted()
  {
    return false;
    //return callback.shouldAborted();
  }
  
}
