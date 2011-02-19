package org.vishia.util;

import java.util.Iterator;

/**Interface to access data which are organized in a sorted tree.
 * The sorting key is of type String. The interface should be implemented from a class
 * which presents a node in a tree.
 * 
 * @author JcHartmut
 *
 * @param <Type> The type of elements.
 */
public interface SortedTree<Type> 
{ 
  /**Searches the first child with given key from the given node this.
   * 
   * @param sKey The key of the demanded child. The implementation may have extra possibilities in addressing children
   *        with strings. The sKey can be representing more as a simple string as a attribute in children. 
   *        It may be comparable with the ability of XPATH in XML.
   *        But it will be a feature of Implementation and is not defined as a basic feature of this interface.
   * @return The first child with the given key or null if there is no such child.
   */
  Type getChild(String sKey);
  
  /**Returns an iterator through the list of all children of the node.
   * 
   * @return The iterator or null if there isn't any children.
   */
  Iterator<Type> getChildren();
  
  /**Returns an iterator through the list of all children with the given key.
   * 
   * @param sKey The key of the demanded child. The implementation may have extra possibilities, see getChild(String).
   * @return A iterator through the list of all children with the given key or null if there is no such child.
   */
  Iterator<Type> getChildren(String sKey);
}
