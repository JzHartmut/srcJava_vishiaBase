package org.vishia.xmlReader;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**This is an universal data container as output for {@link XmlJzReader}
 * without semantic sorting of attributes and nodes.
 * The {@link XmlJzReader} can now be used adequate a DOM XML reader, reads all what is found.
 * This is the destination. 
 * The semantic sorting should be done by evaluation of the content.
 * This instance represents all nodes and refers all sub nodes.
 * If it is the root of a read XML file, it is the sum of all data.
 * <br>
 *   The concept of {@link XmlJzReader} till before was only: Only store adequate a given config file.
 *   Now using this class the config file should only contain namespace declarations, no more.
 * @author Hartmut Schorrig
 * @copyrigth LPGL license, do not remove this copyright
 * @since 2024-05-26
 *
 */
public class XmlDataNode implements XmlAddData_ifc{
  
  /**Version, history and license.
    * <ul>
    * <li>2024-09-19 new {@link #getTextSub()} for special cases, for example LibreOffc span is sometimes deep nested without necessity. 
    * <li>2024-06-23 new {@link #iterNodes(String)} and {@link OnlyOneNode} to evaluate a dedicated tag of nodes. 
    * <li>2024-05-25 Hartmut created because the structure of an XML file should not be explored on first usage,
    *   but {@link XmlJzReader} should be used adequate a DOM XML reader, reads all what is found.
    *   This is the destination. 
    * </ul>
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
    */
   public final static String version = "2024-09-19"; 


  protected final XmlDataNode parent;
  
  public final String tag;
  
  /**If this element is set, the node has only one text.*/
  public String text;
  
  /**Index of all attributes sorted by name.*/
  public final Map<String, String> attribs;
  
  /**Index of sub nodes with a defined key as &lt;name ...
   * but only with one member in the node. 
   * This allows simple select a sub node with a defined &lt;name 
   */
  public final Map<String, XmlDataNode> singleNodes;
  
  /**As alternative to members in {@link #singleNodes}: 
   * Index of sub nodes with a defined key as &lt;name ...
   * with more as one member in the node. 
   * This allows simple select all sub nodes with a defined &lt;name in order of their appearance in the node. 
   */
  public final Map<String, List<XmlDataNode>> multiNodes;
  
  /**List of all sub nodes in order of appearance in the node.
   * To evaluate in given order.
   */
  public final List<XmlDataNode> allNodes;
  
  
  
  
  public XmlDataNode ( XmlDataNode parent, String tag, Map<String, String> attribs ) {
    this.parent = parent;
    this.tag = tag;
    this.attribs = attribs;
    this.allNodes = new LinkedList<>();
    this.singleNodes = new TreeMap<String, XmlDataNode>();
    this.multiNodes = new TreeMap<String, List<XmlDataNode>>();
  }

  @Override public XmlAddData_ifc newNode ( String tag, Map<String, String> attribs ) {
    return new XmlDataNode(this, tag, attribs);
  }

  @Override public void addNode ( XmlAddData_ifc node ) {
    XmlDataNode dataNode = (XmlDataNode) node;
    if(this.text !=null) {  //------------------------------- if only a singular text is stored in the node
      XmlDataNode nodeText = new XmlDataNode(this, "$", null);   // it should be added as first as text node.
      nodeText.text = this.text;
      this.text = null;
      addNode(nodeText);
    }
    this.allNodes.add(dataNode);
    List<XmlDataNode> listTaggedSubnode;
    XmlDataNode dataNode1;
    if( (listTaggedSubnode = this.multiNodes.get(dataNode.tag)) != null) {
      listTaggedSubnode.add(dataNode);                     // an multiNode is already given, store there
    } else if( (dataNode1 = this.singleNodes.get(dataNode.tag)) == null) {
      this.singleNodes.put(dataNode.tag, dataNode);        // first occurrence of this tagged node
    } else {  //--------------------------------------------- any other dataNode already stored in singleNodes
      List<XmlDataNode> listTaggedNodes = new LinkedList<>();
      listTaggedNodes.add(dataNode1);                      // add the first found node
      listTaggedNodes.add(dataNode);                           // add current node
      this.singleNodes.remove(dataNode.tag);               // remove the first found node as single node
      this.multiNodes.put(dataNode.tag, listTaggedNodes);  // up to now add nodes with this tag in 
    }                                                      
  }

  
  /**Add a plain text to the node.
   * If the node is empty yet, the text is stored in {@link #text} for a simple text.
   * If the node is not empty, then all texts are stored in order of all sub nodes in the List {@link #allNodes}.
   * A simple text before stored is added also there, but on first position because it was stored as first.
   */
  @Override public void addText ( String text ) {
    if(this.allNodes.size()==0) {
      this.text = text;                                    // store a singular simple text immediately in the node.
    } else {                                               // a given text is stored as node also in addNode. text = null.
      XmlDataNode nodeText = new XmlDataNode(this, "$", null); // store the text always as node.
      nodeText.text = text;
      addNode(nodeText);
    }
  }

  
  
  
  public String getAttrib ( String key) {
    return this.attribs == null ? null : this.attribs.get(key);
  }
  
  
  
  public XmlDataNode getFirstNode ( String key) {
    XmlDataNode node = this.singleNodes ==null ? null : this.singleNodes.get(key);
    if(node == null && this.multiNodes !=null) {
      List<XmlDataNode> node1 = this.multiNodes.get(key);
      if(node1 !=null) { node = node1.get(0); }            // return first member if more as one
    }
    return node;
  }
  
  
  
  /**Iterator over all nodes of a defined tag, also applicable if no node is given.
   * @param tag
   * @return
   * @since 2024-06-23
   */
  public Iterable<XmlDataNode> iterNodes(String tag) {
    List<XmlDataNode> listNodes =  this.multiNodes.get(tag);
    if(listNodes !=null) return listNodes;
    else {
      XmlDataNode nd = this.singleNodes.get(tag);          // nd maybe null also
      return new OnlyOneNode(nd);
    }
  }
  
  
  
  
  /**Get the whole plain text inside the node. 
   * Either there is exact one text part, it is stored in {@link #text} and returned.
   * Or there are some more text parts, stored as node with tag="$" in {@link #multiNodes}.
   * Then all these text parts are concatenated and returned as one.
   * 
   * @return
   */
  public String getText () {
    if(this.text !=null) { 
      assert(this.allNodes.size()==0);                     // then no nodes should exist.
      return this.text;                                    // only this text 
    } else if(this.singleNodes !=null) {
      XmlDataNode nodeText = this.singleNodes.get("$");
      if(nodeText !=null) { 
        return nodeText.getText(); 
      }
    }  
    if(this.multiNodes !=null) {
      List<XmlDataNode> nodesText = this.multiNodes.get("$");
      if(nodesText !=null) {
        String sText = "";
        for(XmlDataNode nodeText1 : nodesText) {
          sText += nodeText1.getText();
        }
        return sText;
      }
      else { return null; }   //--------------------------- no text found. 
    }
    else { return null; }       //--------------------------- no text found. 
  }

  
  /**Get the whole plain text inside the node and from all sub nodes. 
   * Either there is exact one text part, it is stored in {@link #text} and returned.
   * Or there are some more text parts, either stored as node with tag="$" in {@link #allNodes}.
   * or also in sub nodes with any tag in #allNodes.
   * In the last case the tags of the deeper sub nodes are ignored for getting the whole texts.
   * 
   * On return all these text parts are concatenated and returned as one.
   * 
   * @return null if no texts found, else the sum of all texts appended in order in XML node and all sub nodes.
   */
  public String getTextSub () {
    if(this.text !=null) { 
      assert(this.allNodes.size()==0);                     // then no nodes should exist.
      return this.text;                                    // only this text 
    } else if(this.allNodes !=null) {
      StringBuilder sbText = new StringBuilder();
      for(XmlDataNode subNode: this.allNodes) {
        if(subNode.tag.equals("$")) {
          sbText.append(subNode.getText());                // summarize given more texts.
        } else {
          sbText.append(subNode.getTextSub());             // summarize all texts from deeper nodes, also recursively
        }
      }
      return sbText.length() ==0 ? null : sbText.toString();
    }
    else { return null; }       //--------------------------- no text found. 
  }
  
  
  @Override public String toString() { return "<" + this.tag + (this.text ==null ? ">" : ">" + this.text); }



  /**Dummy class to offer an iterator with one or zero nodes
   * @since 2024-06-23
   */
  class OnlyOneNode implements Iterator<XmlDataNode>, Iterable<XmlDataNode> {

    private XmlDataNode nd;
    
    
    /**Constructor with the given one node or without node.
     * @param nd can be null, then the first call of {@link #hasNext()} delivers false.
     */
    OnlyOneNode(XmlDataNode nd){
      this.nd = nd;
    }
    
    @Override public boolean hasNext () {
      return this.nd !=null;
    }

    @Override public XmlDataNode next () {
      XmlDataNode nd = this.nd;
      this.nd = null;                    // following hasNext() returns false
      return nd;
    }

    @Override public Iterator<XmlDataNode> iterator () {
      return this;
    }
    
  }





}
