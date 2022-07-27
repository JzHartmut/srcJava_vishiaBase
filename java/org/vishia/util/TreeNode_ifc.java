package org.vishia.util;

import java.util.Iterator;
import java.util.List;

/**Interface for instances, which are organized with the {@link TreeNodeBase} class.
 * This interface can be used as super interface of an interface of such instances.
 * @author Hartmut Schorrig
 *
 * @param <DerivedNode> The instance type which is the implementor of this.
 * @param <Data> The data type which are referred with a that tree node.
 */
public interface TreeNode_ifc
<DerivedNode extends TreeNode_ifc<DerivedNode,Data>  // & SortedTree<IfcType>
, Data
//, IfcType extends SortedTree<IfcType> 
> 
//extends SortedTree<TreeNode_ifc>
{
  
  /**Version, history and license.
   * <ul>
   * <li>2017-08-27 Hartmut enhanced: {@link #iteratorChildren()} for all children in given order
   * <li>2013-11-11 Hartmut created. The class {@link TreeNodeBase} was existing already.
   *   The reason for this interface:
   *   <ul>
   *   <li>Overview over methods
   *   <li>super interface if any tree node has an interface.
   *   </ul>
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
   * <li> But the LPGL ist not appropriate for a whole software product,
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
   * @author Hartmut Schorrig = hartmut.schorrig@vishia.de, www.vishia.org
   * 
   */
  public static final String version = "2017-08-27";

  
  
  DerivedNode addNode(String itsKey, Data leaf);
  
  
  void addNode(DerivedNode childNode);
  
  
  void addNodeFirst(DerivedNode childNode);
  
  
  void addSiblingNext(DerivedNode childNode);
  
  
  void detach();
  
  
  void removeChildren();
  
  
  String getKey();
  
  
  Data getParentData();
  
  
  DerivedNode getNode(String path, String separator);
  
  
  DerivedNode getOrCreateNode(String path, String separator);
  
  
  DerivedNode parent();
  
  /**Checks whether the parent is a known node. This test can be done for example to check whether the parent
   * is the root node. 
   * Note that especially the root node may be from another type. Therefore parent() fails because a
   * ClassCastException to the DerivedNode.
   * 
   * @param cmpr The node to compare with
   * @return true if the parent node is the same instance as the given node.
   */
  boolean parentEquals(TreeNode_ifc<DerivedNode, Data> cmpr);
  
  DerivedNode nextSibling();
  
  
  
  DerivedNode prevSibling();
  
  boolean hasChildren();
  
  
  int nrofChildren();
  
  
  
  DerivedNode firstChild();
  
  
  
  DerivedNode lastChild();
  
  
  IterableIterator<DerivedNode> iteratorChildren(String keyP);
  

  /**Returns an iterator through the list of all children of the node.
   * 
   * @return The iterator or null if there isn't any children.
   */
  IterableIterator<DerivedNode> iteratorChildren();
  
  
}
