package org.vishia.xmlReader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.vishia.util.Debugutil;
import org.vishia.util.ExcUtil;
import org.vishia.util.StringPart;
import org.vishia.util.StringPartScan;

/**This class reads data not in XML format, it is experience. Not using Yaml, too sophisticated ....
 * not ready yet.
 * @author hartmut
 *
 */
public class ZmlReader {

  
  /**The declared name spaces found in all nodes. */
  Map<String, String> nameSpacesAll = new TreeMap<String, String>();

  
  /**Stores the cfg subtree in the usage order. */
  Map<String, ZmlNode> idxNodes = new TreeMap<String, ZmlNode>();
  
  
  
  public void readZml(File fin) {
    BufferedReader rd = null;
    try {
      rd = new BufferedReader(new InputStreamReader(new FileInputStream(fin), "UTF-8"));
      String sLine;
      while( (sLine = rd.readLine()) !=null) {
        sLine = sLine.trim();                    // indentation is only for viewing
        if(sLine.length() == 0) {}               // ignore empty line
        else if(sLine.startsWith("#")) {}        // ignore comment line
        else if(sLine.startsWith("@xmlns:")) {
          int sep = sLine.indexOf(" = ");
          this.nameSpacesAll.put(sLine.substring(7, sep), sLine.substring(sep+3));
        }
        else if(sLine.startsWith("<")) {
          ZmlNode node = readNode(rd, sLine);
          this.idxNodes.put(node.tag, node);
        } 
        else {
          System.err.println("faulty line: " + sLine);
        }
        
      }
    } catch(Exception exc) {
      CharSequence error = ExcUtil.exceptionInfo("unexpected", exc, 0, 100);
      System.err.append(error);
    } finally {
      if(rd !=null) {
        try { rd.close(); } catch(IOException exc) { System.err.append("cannot close" + fin.getAbsolutePath());}
      }
    }
  }

  
  
  private ZmlNode readNode ( BufferedReader rd, String sLine1) throws IOException {
    String sLine = sLine1 != null ? sLine1 : rd.readLine();
    sLine = sLine.trim();
    StringPartScan scLine = new StringPartScan(sLine);
    assert(sLine.startsWith("<"));
    scLine.seekPos(1).lento(' ');                           // tag ends with space, all characters admissible for the tag.
    String sTag = scLine.getCurrentPart().toString();
    ZmlNode node = new ZmlNode(null, sTag);
    scLine.fromEnd().seekNoWhitespace();
    readContent(scLine, node);
    while( (sLine= rd.readLine()) !=null) {
      scLine = new StringPartScan(sLine.trim());
      if(scLine.startsWith(">")) {                // end of node
        break;
      }
      else if(scLine.startsWith("<")) {
        ZmlNode subNode = readNode(rd, sLine);
        node.putSubnode(subNode);
      }
      else {
        readContent(scLine, node);
      }
    }
    return node;
  }

  
  void readContent(StringPart scLine, ZmlNode node) {
    while(scLine.length() >0) {
      char c0 = scLine.charAt(0);
      if(c0 =='#') {
        break;                                   // endline comment as end of line
      }
      else if(c0 =='@') {
        readAttribute(scLine, node);
      }
      else {
        final String value = scLine.contentTillSpaceEndOrQuotation("\"\"", '\\');
        scLine.fromEnd().seekNoWhitespace();
        node.addAttributeUnnamed(value);
      }
    }

  }
  
  
  
  void readAttribute(StringPart scLine, ZmlNode node) {
    scLine.lento('=');
    String sAttribName = scLine.getCurrentPart().toString().trim();
    scLine.fromEnd().seekPos(1).seekNoWhitespace();
    final String sAttrValue = scLine.contentTillSpaceEndOrQuotation("\"\"", '\\');
    node.addAttribute(sAttribName, sAttrValue);
  }
  
  
  
  public static class AttribRead {
    String namespace;
    String name;
    /**storage type or access routine */
    String value;
  }


  
  
  public static class ZmlNode {
    /**Tag name of the element. */
    final String tag;
    
    final ZmlNode parent;
    
    /**Found sub nodes. The list is supplemented if new sub nodes are found on further occurrences of elements. 
     * It contains the same tag element only one time. that is tested in {@link #addElement(String)}.
     */
    Map<String, ZmlNode> nodes;
    
    /**Found attributes. The list is supplemented if new attribute names are found on further occurrences of elements. */
    Map<String, AttribRead> attribs;
    
    
    /**Set if new attributes are found. Then the */
    boolean bNewAttributes = false; 

    /**The declared name spaces for this node. */
    Map<String, String> nameSpaces;

    List<String> attribsUnnamed;
    
    ZmlNode(ZmlNode parent, String tag){ 
      this.parent = parent; 
      this.tag = tag;
    }

    /**It is invoked via reflection from {@link XmlJzCfgAnalyzer#newCfgReadStruct()}
     * @param tag
     * @return
     */
    public void addAttribute(String namespacename, String attribValue) { 
      AttribRead attrib = new AttribRead();
      //replaces the ':' between namespace:name with _
      attrib.value = attribValue;
      int posNamespace = namespacename.indexOf(':');
      if(posNamespace >=0) {
        attrib.namespace = namespacename.substring(0, posNamespace);
        attrib.name = namespacename.substring(posNamespace+1);
      } else {
        attrib.name = namespacename;
      }
      if(this.attribs == null) { 
        this.attribs = new TreeMap<String, AttribRead>(); 
        this.bNewAttributes = true;
        this.attribs.put(namespacename, attrib);  //the first attrin
      }
      else if(this.attribs.get(namespacename) ==null) {
        this.bNewAttributes = true;
        this.attribs.put(namespacename, attrib);  //a new attrib
      }
    }

    /**Adds a value String found in the data presentation, which is not designated by a name.
     * It may be designated by its distinguished content or by its position.
     * The post preparation should decide about usage.
     * @param value value to add to {@link #attribsUnnamed}.
     */
    public void addAttributeUnnamed(String value) {
      if(this.attribsUnnamed == null) { this.attribsUnnamed = new LinkedList<String>(); }
      this.attribsUnnamed.add(value);
    }
    
    void putSubnode(ZmlNode subNode) {
      if(this.nodes == null) { this.nodes = new TreeMap<String, ZmlNode>(); }
      this.nodes.put(subNode.tag, subNode);
    }
    

  }

  
}
