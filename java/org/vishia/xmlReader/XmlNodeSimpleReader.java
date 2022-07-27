package org.vishia.xmlReader;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;

import org.vishia.util.DataAccess;
import org.vishia.xmlSimple.XmlNodeSimple;

public class XmlNodeSimpleReader {

  
  
  
  
  /**Read the configuration file for this instance. The configuration contains
   * how elements of a read xml file are processed and stored.
   * @param file from this file
   * @return A new XmlCfg instance which is automatically set as referenced and used in {@link #cfg}.
   *   But you can store this reference and used for {@link #readXml(File, Object, XmlCfg)} later too.
   * @throws IOException
   */
  public XmlNodeSimple<Object> readXml(File file) throws IOException {
    XmlNodeSimple<Object> rootroot = new XmlNodeSimple<Object>("rootroot");
    XmlCfg xmlCfg = newCfgXmlNodeSimple();
    XmlJzReader xmlReader = new XmlJzReader();
    xmlReader.readXml(file, rootroot, xmlCfg);
    return rootroot;
  }


  
  /**Creates the configuration to read any file and store the content to a {@link org.vishia.xmlSimple.XmlNodeSimple} data tree.
   * @return instance
   */
  public static XmlCfg newCfgXmlNodeSimple()
  { XmlCfg cfgCfg = new XmlCfg();
    cfgCfg.rootNode = new XmlCfg.XmlCfgNode(null, cfgCfg, "CFG-ROOT");  //The rootnode of the cfg is only formalistic.
    
    //The next XmlCfgNode is used for all subnodes of the <xmlinput.cfg...
    XmlCfg.XmlCfgNode nodes = new XmlCfg.XmlCfgNode(null, cfgCfg, "?"); //Any unspecific nodes.
    cfgCfg.rootNode.addSubnode("?", nodes);    //2. level add to root node too.
    nodes.addSubnode("?", nodes);      //add this node itself as subnode, then all nodes are the same, infinite.
    
    
//    cfgNode.subNodeUnspec = nodes; //it is set to use for all nodes with 
//    nodes.subNodeUnspec = nodes;  //recursively, all children are unspec.
    
    try {
      nodes.setNewElementPath("!createNode(tag, null)");
      nodes.setContentStorePath("!addContent(text)");
      nodes.attribsUnspec = new DataAccess.DatapathElement("!setAttribute(name, value)");  //use addAttributeStorePath in the dst node to add.
      nodes.setNameSpaceStorePath("!setAttribute(name, value)");
    }
    catch(ParseException exc) {
      throw new RuntimeException(exc); //it is unexpected
    }
    return cfgCfg;
  }

}
