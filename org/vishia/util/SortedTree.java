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
 * 2007-09-15: JcHartmut www.vishia.de creation
 * 2008-04-02: JcHartmut some changes
 *
 ****************************************************************************/
package org.vishia.util;

import java.util.Iterator;
import java.util.List;

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
  Iterator<Type> iterChildren();
  
  /**Returns an iterator through the list of all children with the given key.
   * 
   * @param sKey The key of the demanded child. The implementation may have extra possibilities, see getChild(String).
   * @return A iterator through the list of all children with the given key or null if there is no such child.
   */
  Iterator<Type> iterChildren(String sKey);

  /**Returns a List of all children of the node.
   * 
   * @return The List or null if there isn't any children.
   */
  List<Type> listChildren();
  
  /**Returns a List of all children with the given key.
   * 
   * @param sKey The key of the demanded child. The implementation may have extra possibilities, see getChild(String).
   * @return A List of all children with the given key or null if there is no such child.
   */
  List<Type> listChildren(String sKey);
}
