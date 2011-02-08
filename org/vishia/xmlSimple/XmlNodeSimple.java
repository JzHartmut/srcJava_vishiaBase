/****************************************************************************
 * Copyright/Copyleft:
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
 * 2008-01-15: JcHartmut www.vishia.de creation
 * 2008-04-02: JcHartmut some changes
 *
 ****************************************************************************/
package org.vishia.xmlSimple;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


/**This is a simple variant of processing XML.*/

/**Representation of a XML node. It contains a tree of nodes or text content. */ 
public class XmlNodeSimple implements XmlNode
{ 
  /**The tag name of the node or the text if namespaceKey is "$". */
  String name;
  
  /**The namespace-key. If it is "$", the node is a terminate text node. */
  String namespaceKey;

  /**Sorted attributes. */
  TreeMap<String, String> attributes;

  /**The List of child nodes and text in order of adding. 
   * Because the interface reference is used, it is possible that a node is another instance else XmlNodeSimple. 
   */
  List<XmlNode> content;

  /**Sorted child nodes. The nodes are sorted with tag-names inclusive name-space.*/
  TreeMap<String, List<XmlNode>> sortedChildNodes;
  
  /**List of namespace declaration, typical null because only top elements has it. */
  TreeMap<String, String> namespaces;

  
  /**The parent node. */
  XmlNode parent;
  
  public XmlNodeSimple(String name)
  { this.name = name;
  }
  
  public XmlNodeSimple(String name, String namespaceKey)
  { this.name = name;
    this.namespaceKey = namespaceKey;
  }
  
  public XmlNodeSimple(String name, String namespaceKey, String namespace)
  { this.name = name;
    this.namespaceKey = namespaceKey;
  }
  
  
  public XmlNode createNode(String name, String namespaceKey)
  { return new XmlNodeSimple(name,namespaceKey);
  }

  
  
  public void setAttribute(String name, String value)
  { if(attributes == null){ attributes = new TreeMap<String, String>(); }
    attributes.put(name, value);
  }
  
  public void addNamespaceDeclaration(String name, String value)
  { if(namespaces == null){ namespaces = new TreeMap<String, String>(); }
    namespaces.put(name, value);
  }
  
  public XmlNode addContent(String text)
  { if(content == null){ content = new LinkedList<XmlNode>(); }
    content.add(new XmlNodeSimple(text, "$"));
    return this;
  }
  

  public XmlNode addNewNode(String name, String namespaceKey) throws XmlException
  { XmlNode node = new XmlNodeSimple(name, namespaceKey);
    addContent(node);
    return node;
  }
  
  
  
  public XmlNode addContent(XmlNode node) 
  throws XmlException
  { if(content == null){ content = new LinkedList<XmlNode>(); }
    //XmlNodeSimple node = (XmlNodeSimple)nodeP;
    if(node.getParent() != null)
      throw new XmlException("node has always a parent");
    node.setParent(this);
    content.add(node);
    
    String key;  //build the key namespace:tagname or tagname
    if(node.getNamespaceKey() != null) { key =  node.getNamespaceKey() + ":" + node.getName(); }
    else { key = node.getName(); }
    
    List<XmlNode> listKeyNode;
    if(sortedChildNodes == null)
    { //the node hasn't any children yet:
      sortedChildNodes = new TreeMap<String, List<XmlNode>>();
      listKeyNode = null; 
    }
    else
    { //search children with same key, may be found or not:
      listKeyNode = sortedChildNodes.get(key);
    }
    if(listKeyNode == null)
    { //no tag with key contained yet:
      listKeyNode = new LinkedList<XmlNode>();
      sortedChildNodes.put(key, listKeyNode);
    }
    listKeyNode.add(node);  //to the list to the correct key.
    return this;
  }

  
  public void removeChildren()
  { if(content != null) for(XmlNode child: content)
    { if(!child.isTextNode())
      { child.setParent(null);  //
      }
    }
    content = null;
    sortedChildNodes = null;
  }
  
  
  
  public void setParent(XmlNode parent)
  { this.parent = parent;
  }

  
  public XmlNode getParent(){ return parent; }
  
  
  public boolean isTextNode(){ return namespaceKey != null && namespaceKey.equals("$"); }
  
  /**Returns the text of the node. If it isn't a text node, the tagName is returned. */
  public String getText()
  { if(namespaceKey.equals("$"))
    { //it is a text node.
      return name;
    }
    else
    { String sText = "";
      if(content != null) for(XmlNode child: content)
      { if(child.getNamespaceKey().equals("$"))
        { //a text content as child.
          sText += child.getName();
        }
      }
      return sText;
    }
  }
  
  /**Returns the text if it is a text node. If it isn't a text node, the tagName is returned. */
  public String getName(){ return name; }
  
  public String getNamespaceKey(){ return namespaceKey; }
  
  public String getAttribute(String name)
  {
    if(attributes != null)
    { return attributes.get(name);
    }
    else return null;
  }

  public Map<String, String> getAttributes()
  {
    return attributes;
  }

  public Map<String, String> getNamespaces()
  {
    return namespaces;
  }

  public String removeAttribute(String name)
  {
    if(attributes != null)
    { return attributes.remove(name);
    }
    else return null;
  }

  public XmlNode getChild(String key)
  {
    if(sortedChildNodes != null)
    { List<XmlNode> children = sortedChildNodes.get(key);
      if(children != null) return children.get(0);
      else return null;
    }
    else return null;
  }

  public Iterator<XmlNode> iterChildren()
  {
    if(content != null) return content.iterator();
    else return null;
  }

  public Iterator<XmlNode> iterChildren(String key)
  {
    if(sortedChildNodes != null)
    { List<XmlNode> children = sortedChildNodes.get(key);
      if(children != null) return children.iterator();
      else return null;
    }
    else return null;
  }

  public List<XmlNode> listChildren()
  {
    // TODO Auto-generated method stub
    return content;
  }

  public List<XmlNode> listChildren(String key)
  {
    if(sortedChildNodes != null)
    { List<XmlNode> children = sortedChildNodes.get(key);
      return children;  //may be null if key not found.
    }
    else return null;
  }
  
  public String toString()
  { return "<" + name + ">";
  }
  
}


