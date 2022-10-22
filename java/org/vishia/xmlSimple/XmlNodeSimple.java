package org.vishia.xmlSimple;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.vishia.util.TreeNodeBase;


/**This is a simple variant of processing XML.*/

/**Representation of a XML node. It contains a tree of nodes or text content. 
 * Hint: The possibility to reference data is additional to XML concepts, not usable for the XML output but for management of data.
 * The referenced data are present if the appropriate constructors are used: 
 * {@link #XmlNodeSimple(String, Object)} and {@link #XmlNodeSimple(String, String, Object)}. 
 * Elsewhere the {@link TreeNodeBase#nd_data} is left empty. This is for ordinary XML data.
 * 
 * */ 
public class XmlNodeSimple<UserData> extends TreeNodeBase<XmlNodeSimple<UserData>, UserData, XmlNode> implements XmlNode
{ 
  /**Version, history and license.
   * <ul>
   * <li>2018-09-10: Hartmut new: {@link #setAttribute(String, String, String)} with namespace
   * <li>2013-03-21 Hartmut bugfix: store data in {@link XmlNodeSimple#XmlNodeSimple(String, Object)} if given.
   * <li>2013-03-21 Hartmut chg: {@link XmlNodeSimple#XmlNodeSimple(String, Object)} etc. If "namespace:name" is given as parameter name
   *   the namespace is used.
   * <li>2012-12-26 Hartmut chg: {@link #toString()} returns the pure text if the node contains only text or it is an attribute.
   *   It is helpful for output data.
   * <li>2012-12-26 Hartmut chg: If the node has a simple text as content, not more, then the text is stored in the {@link #text}
   *   and no extra child is created for that. If the node has another children and the text is dispersed between them therefore,
   *   the text is stored as child node with "$"-key like before.
   * <li>2012-11-24 Hartmut chg: The attributes are stored as children in the same tree as other content.
   *   It means that the attributes are visible too if the tree is evaluated as {@link TreeNodeBase} reference.
   * <li>2012-11-24 Hartmut new: {@link #addContent(XmlNode)} checks whether the child is an attribute node.
   *   Then it is added as attribute. Such nodes are created from {@link org.vishia.zbnf.ZbnfParser} now. 
   *   and the {@link XmlNodeSimple#attributes} may be removed.
   * <li>2012-11-03 Hartmut Now this class is derived from TreeNodeBase directly. It is a TreeNode by itself.
   * <li>2012-11-01 Hartmut The {@link TreeNodeBase} is used for the node structure. 
   *   Reference {@link #node}. The algorithm to manage the node structure is deployed in the
   *   TreeNodeBase yet. 
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


  
  /**The tag name of the node or the text if namespaceKey is "$". 
   * Note: It is not the key in the {@link TreeNodeBase}. The key there is namespaceKey:name. */
  final String name;
  
  /**The namespace-key. If it is "$", the node is a terminate text node. */
  final String namespaceKey;
  
  /**Text of an attribute or text of a text node. */
  String text;
  
  /**List of namespace declaration, typical null because only top elements has it. */
  TreeMap<String, String> namespaces;

  
  /**The parent node. */
  //XmlNode parent;
  
  /**Creates a XML node.
   * @param name The tag name. If it contains a "namespace:name", the namespace will be separated.
   */
  public XmlNodeSimple(String name)
  { this(name, null, (UserData)null);
  }
  
  
  
  /**Creates a XML node.
   * @param name The tag name. If it contains a "namespace:name", the namespace will be separated.
   * @param data Additional data stored in the {@link TreeNodeBase#nd_data}
   */
  public XmlNodeSimple(String name, UserData data)
  { this(name, null, data);
  }
  
  
  
  /**Creates a XML node with text.
   * @param key
   * @param text
   * @param isText not used. It is a marker only to select this constructor.
   */
  protected XmlNodeSimple(String key, String text, boolean isText)
  { super(key, null);
    this.name = key;
    this.text = text;
    this.namespaceKey = key;
  }
  
  
  
  /**Creates a XML node.
   * @param name The tag name. If it contains a "namespace:name", the namespace will be separated.
   * @param namespaceKey A given namespace if not contained in name. This argument wins if "namespace:name"
   * @param data Additional data stored in the {@link TreeNodeBase#nd_data}
   */
  public XmlNodeSimple(String name, String namespaceKey, UserData data)
  { super(calcKey(name, namespaceKey), data);  //TreeNodeBase, key is namespace:name
    int posNamespace = name.indexOf(':');
    if(posNamespace >=0){
      if(namespaceKey == null || namespaceKey.length() ==0) { namespaceKey = name.substring(0, posNamespace); }
      //else: let namespaceKey unchanged, ignore it in name.
      name = name.substring(posNamespace +1);
    }
    this.name = name;
    //if(name.startsWith("@"))
      //Assert.stop();
    this.namespaceKey = namespaceKey;
  }
  
  
  
  /**Creates a XML node.
   * @param name The tag name. If it contains a "namespace:name", the namespace will be separated.
   * @param namespaceKey A given namespace if not contained in name. This argument wins if "namespace:name"
   */
  public XmlNodeSimple(String name, String namespaceKey)
  { this(name, namespaceKey, (UserData)null);
  }
  
  
  
  /**Creates a XML node.
   * @param name The tag name. If it contains a "namespace:name", the namespace will be separated.
   * @param namespaceKey A given namespace if not contained in name. This argument wins if "namespace:name"
   * @param namespace the string associated to the namespaceKey
   */
  public XmlNodeSimple(String name, String namespaceKey, String namespace)
  { this(name, namespaceKey, (UserData)null);
    if(namespaces == null){ namespaces = new TreeMap<String, String>(); }
    namespaces.put(namespaceKey, namespace);    
  }
  
  
  private static String calcKey(String name, String namespaceKey){
    String key;  //build the key namespace:tagname or tagname
    if(namespaceKey != null) { key =  namespaceKey + ":" + name; }
    else { key = name; }
    return key;
  }
  
  @Override protected XmlNodeSimple<UserData> newNode(String key, UserData data){
    XmlNodeSimple<UserData> newNode = new XmlNodeSimple<UserData>(key, this.namespaceKey, data);
    return newNode;
  }

  
  @Override public XmlNode createNode(String name, String namespaceKey)
  { return new XmlNodeSimple<UserData>(name,namespaceKey);
  }

  
  
  /**Creates an independent new XmlNode without data. 
   * It is static, does not presume existing nodes, in opposite to {@link #createNode(String, String)}
   * @param name Tag name or "namespacekey:name" if argument namespaceKey is null
   * @param namespaceKey extra argument as name space key. 
   *   This is not the namespace itself, it is the key defined in xml with an attribute:
   *   <code>xmlns:namespaceKey="www.url/of/namespace"</code>
   * @return the created node.
   * @since 2022-06 as supplement to {@link #createNode(String, String)} which needs an instance.
   */
  public static XmlNode createNewNode(String name, String namespaceKey)
  { return new XmlNodeSimple<Object>(name,namespaceKey);
  }
  
  
  @SuppressWarnings("unchecked")
  @Override public XmlNode setAttribute(String name, String value)
  { String aname = "@" + name;
    XmlNodeSimple<UserData> attribute = (XmlNodeSimple<UserData>)getChild(aname);
    if(attribute ==null){
      attribute = new XmlNodeSimple<UserData>(aname);
      addNode(attribute);
    }
    attribute.text = value;
    return this;
  }
  
  @SuppressWarnings("unchecked")
  @Override public void setAttribute(String name, String namespaceKey, String value)
  { String aname = (namespaceKey !=null? namespaceKey + ":" : "") + "@" + name;
    XmlNodeSimple<UserData> attribute = (XmlNodeSimple<UserData>)getChild(aname);
    if(attribute ==null){
      attribute = new XmlNodeSimple<UserData>(aname, namespaceKey);
      addNode(attribute);
    }
    attribute.text = value;
  }
  
  public void addNamespaceDeclaration(String name, String value)
  { if(namespaces == null){ namespaces = new TreeMap<String, String>(); }
    namespaces.put(name, value);
  }
  
  /* (non-Javadoc)
   * @see org.vishia.xmlSimple.XmlNode#addContent(java.lang.String)
   */
  @Override public XmlNode addContent(String text)
  { if(!hasChildren()){ 
      if(this.text == null){ //the first text
        this.text = text;
      } else {
        //one text is existing, add it and the second one as child.
        XmlNodeSimple<UserData> child = new XmlNodeSimple<UserData>("$", this.text, true);
        addNode(child);
        this.text = null;
        XmlNodeSimple<UserData> child2 = new XmlNodeSimple<UserData>("$", text, true);
        addNode(child2);
      }
    } else {
      XmlNodeSimple<UserData> child = new XmlNodeSimple<UserData>("$", text, true);
      addNode(child);
    }
    return this;
  }
  

  /* (non-Javadoc)
   * @see org.vishia.xmlSimple.XmlNode#addNewNode(java.lang.String, java.lang.String)
   */
  @Override public XmlNode addNewNode(String name, String namespaceKey) throws XmlException
  { XmlNode node = new XmlNodeSimple<UserData>(name, namespaceKey);
    addContent(node);
    return node;
  }
  
  
  
  /* (non-Javadoc)
   * @see org.vishia.xmlSimple.XmlNode#addNewNode(java.lang.String, java.lang.String)
   */
  @Override public XmlNode addNewNode(String name) throws XmlException
  { XmlNode node = new XmlNodeSimple<UserData>(name);
    addContent(node);
    return node;
  }
  
  
  
  /* (non-Javadoc)
   * @see org.vishia.xmlSimple.XmlNode#addContent(org.vishia.xmlSimple.XmlNode)
   */
  @SuppressWarnings("unchecked")
  @Override public XmlNode addContent(XmlNode child) 
  throws XmlException 
  { if(!hasChildren() && text !=null){
      //add a text as child because the node has more as one child.
      XmlNodeSimple<UserData> textchild = new XmlNodeSimple<UserData>("$", this.text, true);
      addNode(textchild);
    
    }
    //String nameChild = child.getName();
    if(child instanceof XmlNodeSimple<?>){
      XmlNodeSimple<UserData> child1 = (XmlNodeSimple<UserData>) child;
      addNode(child1);
    } else {
      //Because the child node from another implementation type of XmlNodeSimple,
      //a wrapper node should be created.
      //TODO regard all children in tree? - test with XmlNodeJdom.
      String name = child.getName();
      String namespaceKey = child.getNamespaceKey();
      String key = calcKey(name, namespaceKey);
      //TreeNodeBase<XmlNode,XmlNode> childnode = new TreeNodeBase<XmlNode,XmlNode>(key, child);
      XmlNodeSimple<UserData> childnode = new XmlNodeSimple<UserData>(name, key);
      addNode(childnode);
    }
    return this;
  }

  
  
  
  @Override public boolean isTextNode(){ return namespaceKey != null && namespaceKey.equals("$"); } // || !name.startsWith("@") && text !=null; }
  
  /**Returns the text of the node. If it isn't a text node, the tagName is returned. */
  @Override public String text()
  { if(text !=null) 
    { //it is a text node.
      return text;
    }
    else
    { List<XmlNode> textNodes = listChildren("$");
      String sText = "";
      if(textNodes !=null){
        for(XmlNode textNode: textNodes){
          sText += textNode.text();
        }
      }
      return sText;
    }
  }
  
  /**Returns the text if it is a text node. If it isn't a text node, the tagName is returned. */
  @Override public String getName(){ return name; }
  
  @Override public String getNamespaceKey(){ return namespaceKey; }
  
  @Override public String getAttribute(String name)
  { XmlNodeSimple<UserData> attribute = getNode("@" + name, "/");
    if(attribute !=null){
      assert(attribute.text !=null);
      return attribute.text;
    }
    else return null;
  }

  /**Prepares a Map for attributes from given children. 
   * Invokes {@link #listChildren()} and checks whether the name starts with "@".
   * @see org.vishia.xmlSimple.XmlNode#getAttributes()
   * @since 2018-09: regard nameSpace for attributes
   */
  @Override public Map<String, String> getAttributes()
  {
    Map<String, String> mapAttributes = new TreeMap<String, String>();
    List<XmlNode> attributes = listChildren();
    if(attributes !=null){
      for(XmlNode attrib: attributes){
        String name = attrib.getName();
        if(name.startsWith("@")){
          String nameAttr;
          String namespace = attrib.getNamespaceKey();
          if( namespace !=null) {
            nameAttr = namespace + ":" + name.substring(1);
          } else {
            nameAttr = name.substring(1);
          }
          mapAttributes.put(nameAttr, attrib.text());
        }
      }
    }
    return mapAttributes;
  }

  
  
  /**Prepares a List for attributes from given children. 
   * Invokes {@link #listChildren()} and checks whether the name starts with "@".
   * @see org.vishia.xmlSimple.XmlNode#getAttributes()
   * @since 2019-01: because attributes should be sorted in given order, not in alphabetic one.
   */
  @Override public List<String[]> getAttributeList()
  {
    List<String[]> listAttributes = new LinkedList<String[]>();
    List<XmlNode> attributes = listChildren();
    if(attributes !=null){
      for(XmlNode attrib: attributes){
        String name = attrib.getName();
        if(name.startsWith("@")){
          String nameAttr;
          String namespace = attrib.getNamespaceKey();
          if( namespace !=null) {
            nameAttr = namespace + ":" + name.substring(1);
          } else {
            nameAttr = name.substring(1);
          }
          String[] sAttrib = new String[2];
          sAttrib[0] = nameAttr;
          sAttrib[1] = attrib.text();
          listAttributes.add(sAttrib);
        }
      }
    }
    return listAttributes;
  }

  @Override public Map<String, String> getNamespaces()
  {
    return namespaces;
  }

  @Override public String removeAttribute(String name)
  {
    XmlNodeSimple<UserData> attribute = getNode("@" + name, "/");
    if(attribute !=null){
      attribute.detach();
      return attribute.text;
    }
    else return null;
  }



  /**Returns either the simple text which is associated to the node (for attributes or simple text nodes)
   * or returns "<name>...</> or returns <name/> for empty nodes.
   * This operation is need for output the tree especially in the {@link org.vishia.zbatch.ZbatchExecuter}.
   * @see org.vishia.util.TreeNodeBase#toString()
   */
  @Override public String toString()
  { if(text !=null){ return text; }
    else return "<" + name + (nd_idxChildren !=null ? ">...</>" : ">"); //any container
  }

  
  public String toString1()
  { if(namespaceKey !=null && namespaceKey.equals("$")) return text;  //it is the text
    else if(name.startsWith("@")) return name + "=" + text; 
    else return "<" + name + (nd_idxChildren !=null ? ">...</>" : ">"); //any container
  }

  
}


