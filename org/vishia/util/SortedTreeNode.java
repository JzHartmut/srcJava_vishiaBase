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
 * @author JcHartmut
 *
 * @param <Type> Type of the children.
 */
public class SortedTreeNode<Type> implements SortedTree<Type>
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
    //unsortedChildren.add(newElement);
  }
  
  /**implements the interface method from {@link org.vishia.util.SortedTree}.
   */
  public Type getChild(String key) 
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
  public Iterator<Type> getChildren() 
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
  public Iterator<Type> getChildren(String key) 
  {
    List<Type> childrenNode = sortedChildren.get(key);
    if(childrenNode == null)
    { return null;
    }
    else
    { return childrenNode.iterator();
    }
  }

}
