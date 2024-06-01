package org.vishia.xmlReader;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**This is an universal data container as output for {@link XmlJzReader}
 * without semantic sorting of attributes and nodes.
 * The semantic sorting should be done by evaluation of the content.
 * This instance represents all nodes and refers all sub nodes.
 * If it is the root of a read XML file, it is the sum of all data.
 * @author Hartmut Schorrig
 * @copyrigth LPGL license, do not remove this copyright
 * @since 2024-05-26
 *
 */
public class XmlDataNode implements XmlAddData_ifc{

  protected final XmlDataNode parent;
  
  public final String tag;
  
  public String text;
  
  public final Map<String, String> attribs;
  
  public final Map<String, XmlDataNode> singleNodes;
  
  public final Map<String, List<XmlDataNode>> multiNodes;
  
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

  
  @Override public void addText ( String text ) {
    if(this.allNodes.size()==0) {
      this.text = text;                                    // store a singular simple text immediately in the node.
    } else {
      XmlDataNode nodeText = new XmlDataNode(this, "$", null);
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
  
  
  public String getText () {
    if(this.text !=null) { return this.text; }
    else if(this.singleNodes !=null) {
      XmlDataNode nodeText = this.singleNodes.get("$");
      if(nodeText !=null) { return nodeText.getText(); }
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
  
  
  
  @Override public String toString() { return "<" + this.tag + (this.text ==null ? ">" : ">" + this.text); }
}
