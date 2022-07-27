/****************************************************************************/
/* Copyright/Copyleft:
 *
 * For this source the LGPL Lesser General Public License,
 * published by the Free Software Foundation is valid.
 * It means:
 * 1) You can use this source without any restriction for any desired purpose.
 * 2) You can redistribute copies of this source to everybody.
 * 3) Every user of this source, also the user of redistribute copies
 *    with or without payment, must accept this license for further using.
 * 4) But the LPGL ist not appropriate for a whole software product,
 *    if this source is only a part of them. It means, the user
 *    must publish this part of source,
 *    but don't need to publish the whole source of the own product.
 * 5) You can study and modify (improve) this source
 *    for own using or for redistribution, but you have to license the
 *    modified sources likewise under this LGPL Lesser General Public License.
 *    You mustn't delete this Copyright/Copyleft inscription in this source file.
 *
 * @author JcHartmut = hartmut.schorrig@vishia.de
 * @version 2006-06-15  (year-month-day)
 * list of changes:
 * 2012-07-22: Hartmut documentation, creation of Node4Tree and comparison with this implementation.
 * 2008-04-02: JcHartmut some changes
 * 2007-09-15: JcHartmut www.vishia.de creation
 *
 ****************************************************************************/
package org.vishia.util;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;


/**Commonly implementation of a Node in a sorted tree. The node is implemented with a TreeMap with the key 
 * and a LinkedList as Object in the TreeMap node, containing all Object with the same key.
 * This construction enables the fast searching of more as one child nodes with same key. 
 * The found children with the same key may be sorted
 * in its originally order on input by building the tree.
 * 
 * UML-Diagram, presentation style see {@link Docu_UML_simpleNotation}:
 * <pre>
 * 
 * SortedTreeNode
 *     -key
 *       |-------obj------>Type (data)
 *       |
 *       |-------------key*>-------------*>|
 *       |                                Type (children)
 *       |-------------------------------*>|
 *                                         |--------------> SortedTreeNode
 * </pre>
 * The node refers to some Instances from Type. The instances can be selected by a key, whereby more as one instance
 * with the same key is possible. 
 * <br><br>
 * If the Type contains a reference to a SortedTree in turn, then a tree of nodes is built.
 * <br><br>
 * See {@link TreeNodeUniqueKey}, it is another implementation of a node for a tree.
 * 
 * This class is designated as deprecated. The problem of this class is: children are referenced
 *   directly with there data. Instead the child nodes should be referenced.
 *   For example the {@link org.vishia.xmlSimle.XmlNodeSimple} has contained extra references to its
 *   children if this class was used as subclass. Now this associations are contained in the
 *   used subclass {@link TreeNodeBase}.
 * 
 * @author Hartmut Schorrig
 *
 * @param <Type> Type of the children.
 * @deprecated. Use {@link TreeNodeBase} instead. 
 */
public class SortedTreeNode<Type extends SortedTree<Type>> implements SortedTree<Type>
{
  String sKey;
  
  Type obj;
  
  TreeMap<String, LinkedList<Type>> sortedChildren;
  
  List<Type> unsortedChildren;
  
  
  /**adds a child.
   * 
   * @param itsKey The key may be free defined outside, independent of the content of the child. 
   *        This key is used to find out children.
   * @param newElement The child to add.
   */
  public void add(String itsKey, Type newElement)
  { 
    LinkedList<Type> childrenWithKey;
    if(sortedChildren == null)
    { sortedChildren = new TreeMap<String, LinkedList<Type>>();
      childrenWithKey = null;
    }
    else
    {
      childrenWithKey = sortedChildren.get(itsKey);
    }
    if(childrenWithKey == null)
    { childrenWithKey = new LinkedList<Type>();
      sortedChildren.put(itsKey, childrenWithKey);
    }
    childrenWithKey.add(newElement);
    //
    if(unsortedChildren == null){
      unsortedChildren = new LinkedList<Type>(); }
    unsortedChildren.add(newElement);
  }
  
  @Override public Type getParent(){
    return null;
  }

  
  /**implements the interface method from {@link org.vishia.util.SortedTree}.
   */
  @Override public Type getChild(String key) 
  {
    List<Type> childrenNode = sortedChildren.get(key);
    if(childrenNode == null)
    { return null;
    }
    else
    { return childrenNode.get(0);
    }
  }
  
  /**implements the interface method from {@link org.vishia.util.SortedTree}.
   */
  @Override public Iterator<Type> iterChildren() 
  {
    if(unsortedChildren == null)
    { return null;
    }
    else
    { return unsortedChildren.iterator();
    }
  }
  
  /**implements the interface method from {@link org.vishia.util.SortedTree}.
   */
  @Override public Iterator<Type> iterChildren(String key) 
  {
    List<Type> childrenNode = sortedChildren.get(key);
    if(childrenNode == null)
    { return null;
    }
    else
    { return childrenNode.iterator();
    }
  }

  /**implements the interface method from {@link org.vishia.util.SortedTree}.
   */
  @Override public List<Type> listChildren() 
  {
    if(unsortedChildren == null)
    { return null;
    }
    else
    { return unsortedChildren;
    }
  }
  
  /**implements the interface method from {@link org.vishia.util.SortedTree}.
   */
  @Override public List<Type> listChildren(String key) 
  {
    return sortedChildren.get(key);  //null if key is not found.
  }

}
