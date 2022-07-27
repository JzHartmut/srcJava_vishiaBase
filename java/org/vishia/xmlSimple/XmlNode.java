package org.vishia.xmlSimple;

import java.util.List;
import java.util.Map;

import org.vishia.util.SortedTree;

/**Interface to Access to a XML node. It contains a tree of nodes or text content. 
 * This class is a simple way using XML, some features will not supported yet:
 * <ul>
 * <li>Namespaces on attributes.
 * <li>Check of namespace correctness: The user works with the aliases (keys). They will be written in the node.
 *     if the aliases are incorrect, the XML-tree is incorrect.
 * </ul>
 * Type of children returned from {@link SortedTree#listChildren()}:
 * <ul>
 * <li>Children which are XML elements.
 * <li>Children which are plain texts. Use {@link #isTextNode()} to differ it.
 * <li>Note that the attributes are not returned as children. Use {@link #getAttributes()}. 
 * <ul>    
 */ 
public interface XmlNode extends SortedTree<XmlNode>
{  /**Version, history and license.
   * <ul>
   * <li>2018-09-10: Hartmut new: {@link #setAttribute(String, String, String)} with namespace
   * <li>2012-12-26: Hartmut chg: {@link #text()} instead getText(). It is compatible with XPATH-expressions.
   * <li>2012-10-05: The {@link XmlNodeSimple} is removed as dependency, so this class is the same like in the past.
   * <li>2012-10-04: The mainly usage class {@link XmlNodeSimple} is derived from 
   *   {@link org.vishia.util.TreeNodeBase} yet. Because both inherit from {@link SortedTree},
   *   the generic type have to be the same. Up to now it is not possible to use
   *   a indirect related type, the {@link XmlNodeSimple} is used here.  It is not able yet
   *   to mix different implementations of XmlNode in one tree (is it necessary?)
   * <li>2008-04-02: Hartmut some changes
   * <li>2008-01-15: Hartmut www.vishia.org creation
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
   * @author Hartmut Schorrig = hartmut.schorrig@vishia.de
   * 
   */
  public static final int version = 20121104;

  /**creates a XmlNode from the same implementation type as this, but without adding to the XML-tree.
   * The node can be added later, using {@link #addContent(XmlNode)}, because it may be necessary to built
   * a sub tree with the new node first, and than add, if there are multi threads.
   * @param name
   * @param namespaceKey The this-node should know the key, otherwise an XmlException is thrown.
   * @return
   */ 
  public XmlNode createNode(String name, String namespaceKey) throws XmlException;

  /**Sets an attribute by name.
   * 
   * @param name
   * @param value
   * @return this
   */
  public XmlNode setAttribute(String name, String value);
  
  /**Sets an attribute by name.
   * 
   * @param name
   * @param value
   */
  public void setAttribute(String name, String namespaceKey, String value) throws XmlException;
  
  /**Adds namespace declaration on the specified node. 
   * 
   * @param name namespace alias (key)
   * @param value namespace uri.
   */
  public void addNamespaceDeclaration(String name, String value);
  
  /**Gets all namespace declaration at this node. */
  public Map<String, String> getNamespaces();

  /**Adds textual content. */
  public XmlNode addContent(String text);
  
  /**Adds a child node. 
   * If the child nodes {@link #getName()} starts with "@", it is added as an attribute to this with the {@link #text()}
   * as attribute value. This opens the possibility to create a XmlNode independent of a parent and independent of checking
   * whether it should be an attribute, and convert it to an attribute later.
   * 
   * @param node to add
   * @return this itself to add something else.
   * @throws XmlException
   */
  public XmlNode addContent(XmlNode node) throws XmlException;
  
  /**adds and returns a new child node.
   * 
   * @param name
   * @param namespaceKey
   * @return the new created node.
   * @throws XmlException
   */
  public XmlNode addNewNode(String name, String namespaceKey) throws XmlException;
  
  /**adds and returns a new child node.
   * 
   * @param name
   * @param namespaceKey
   * @return the new created node.
   * @throws XmlException
   */
  public XmlNode addNewNode(String name) throws XmlException;
  
  /**removes all children, also textual content, but not attributes. */
  public void removeChildren();  
  
  /**returns true if the node is a text, not a XML element. A textual content is also represent as node. 
   * 
   * @return
   */ 
  public boolean isTextNode();
  
  /**Returns the text of the node. 
   * If it isn't a text node, the summary text of all child text-nodes is returned. 
   * It is similar like XPATH, access to text with "path/text()"
   */
  public String text();
  
  /**Returns the tagname of the node. 
   * <br>Special cases:
   * <ul>
   * <li>If it is a text-node, the behavior is not determined. To check whether it is a text node, call {@link #isTextNode()}.
   * <li>The name can start with a "@". Then it should be esteemed as an XML attribute. 
   * </ul>
   */
  public String getName();
  
  /**returns the alias or key of the namespace. */
  public String getNamespaceKey();
  
  /**returns the requested attribute or null if it is not existing. */
  public String getAttribute(String name);

  /**returns all attributes Sorted by "namespace:name" or "name". */
  public Map<String, String> getAttributes();
  
  /**Returns a list of attributes in its given order. 
   * @return List of : element[0] is the namespace:name, element[1] is the value.
   */
  List<String[]> getAttributeList();

  /**removes the requested attribute. If the attribute is not existing, this method does nothing. */
  public String removeAttribute(String name);

  /**returns the first child with the given name.
   * @param name If the child has a namespace, its key have to be given in form "key:name"
   * @return the node or null.
   * @see org.vishia.util.SortedTree#getChild(java.lang.String)
   */
  //public XmlNode getChild(String name);

  /**gets the parent of the node. */
  //public XmlNode getParent();

  
  
}


