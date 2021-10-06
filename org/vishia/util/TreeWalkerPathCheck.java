package org.vishia.util;

/**This class is a tree walker which checks the names of nodes with a mask path.
 * @author Hartmut Schorrig
 *
 * @param <Type>
 */
public class TreeWalkerPathCheck implements SortedTreeWalkerCallback<String>
{
  /**Version, history and license.
   * <ul>
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

  
  
  /**This class contains the number of files etc. for callback.
   * @author hartmut
   *
   */
  public static class Counters
  { /**Any number of internal data. */
    public long nrofBytes;
    
    /**Number of parent nodes and number of leaf nodes which are processed. */
    public int nrofParents, nrofLeafss;
  
    /**Number of parent nodes and number of leaf nodes which are selected. */
    public int nrofParentSelected, nrofLeafSelected;
    
    public void clear(){ nrofBytes = 0; nrofParents = nrofLeafss = nrofParentSelected = nrofLeafSelected = 0; }
    
  }
  

  
  /**Data chained from a first parent to deepness of dir tree for each level.
   * This data are created while {@link FileAccessorLocalJava7#walkFileTree(FileRemote, FileFilter, int, FileRemoteCallback)} runs.
   * It holds the gathered children from the walker. The children are stored inside the {@link #dir}
   * only on {@link WalkFileTreeVisitor#postVisitDirectory(Path, IOException)}
   */
  private class CurrDirChildren{
    /**The directory of the level. */
    String dir;
    
    final Counters cnt = new Counters();
    
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
    
    @Override public String toString(){ return dir + ": " + pathCheck; }
  }

  
  
  //final SortedTreeWalkerCallback<Type> callback;
  
  final PathCheck check;
  
  private CurrDirChildren curr;

  
  public TreeWalkerPathCheck(String sPathCheck) {
    //this.callback = callback;
    this.check = new PathCheck(sPathCheck);
  }
  
  public void start(String startNode){ } //callback.start(startNode); }

  public SortedTreeWalkerCallback.Result offerParentNode(String sName)
  {
    //String sName = node instanceof TreeNodeNamed_ifc ? ((TreeNodeNamed_ifc)node).getName() : node.toString();
    PathCheck use;
    if(curr != null){ use = curr.pathCheck; }
    else { use = this.check; }  //the first level.
    PathCheck ret = use.check(sName, true);
    if(ret == null){ return Result.skipSubtree; }
    else {
      curr = new CurrDirChildren(sName, ret, curr);
      return Result.cont; //callback.offerParentNode(node);
    }
  }

  
  
  public SortedTreeWalkerCallback.Result finishedParentNode(String parentNode,
      org.vishia.util.SortedTreeWalkerCallback.Counters cnt)
  {
    //checkRet[0] = check.bAllTree ? check : check.parent;
    curr = curr.parent;
    return Result.cont; //callback.finishedParentNode(parentNode, cnt);
  }

  public SortedTreeWalkerCallback.Result offerLeafNode(String sName, Object info)
  {
    //String sName = leafNode instanceof TreeNodeNamed_ifc ? ((TreeNodeNamed_ifc)leafNode).getName() : leafNode.toString();;
    assert(curr !=null);  //it is set in offerParentNode
    PathCheck use =  curr.pathCheck;
    PathCheck ret = use.next !=null ? null : use.check(sName, false); //it should be the last.
    if(ret == null){ return Result.skipSubtree; }
    else return Result.cont; //callback.offerLeafNode(leafNode);
  }

  public void finished(String startNode, org.vishia.util.SortedTreeWalkerCallback.Counters cnt)
  {
    //callback.finished(startNode, cnt);
    
  }

  public boolean shouldAborted()
  {
    return false;
    //return callback.shouldAborted();
  }
  
}
