package org.vishia.xmlReader;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import org.vishia.util.Assert;
import org.vishia.util.Debugutil;
import org.vishia.xmlSimple.SimpleXmlOutputter;
import org.vishia.xmlSimple.XmlException;
import org.vishia.xmlSimple.XmlNode;
import org.vishia.xmlSimple.XmlNodeSimple;

/**This class reads any XML file and writes its structure to a XmlCfg format. 
 * With it the xmlCfg to read that file to data can be prepared.
 * @author Hartmut Schorrig
 *
 */
public class XmlContentCfgWriter
{
  /**Version, License and History:
   * <ul>
   * <li>2018-09-10 writes cfg attributes.
   * <li>2018-08-15 created.
   * </ul>
   * 
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
   * 
   */
  public static final String version = "2018-08-15";

  
  
  
  
  
  
  public void readXmlStruct_writeCfgTemplate(File fXmlIn, File wrCfg) {
    XmlJzReader xmlReader = new XmlJzReader();
    XmlStructureNodeBuilder data = new XmlStructureNodeBuilder("root");  //the root node for reading config
    xmlReader.readXml(fXmlIn, data, XmlCfg.newCfgReadStruct());
    FileWriter writer = null;
    try {
      //Output it as cfg.xml
      XmlNodeSimple<?> root = new XmlNodeSimple<>("xmlinput:root");
      root.addNamespaceDeclaration("xmlinput", "www.vishia.org/XmlReader-xmlinput");
      XmlNode node2 = root.addNewNode("cfg", "xmlinput"); //second node "cfg"
      //
      //add all nodes from data, recursively called.
      addWrNode(node2, data, 999);  
      //
      SimpleXmlOutputter oXml = new SimpleXmlOutputter();
      writer = new FileWriter(wrCfg);
      oXml.write(writer, root);
      writer.close();
    } catch(XmlException | IOException exc) {
      CharSequence error = Assert.exceptionInfo("unexpected", exc, 0, 100);
      System.err.append(error);
    } finally {
      if(writer !=null) {
        try { writer.close(); } catch(IOException exc) { System.err.append("cannot close"+wrCfg.getAbsolutePath());}
      }
    }
    
    Debugutil.stop();
  }

  
  /**Adds the node and recursively all sub nodes from the configuration 
   * @param xmlNode The xml node for output to add.
   * @param node The node from the structure of the read XML file, 
   * @param recursion decremented, exception on <=0
   * @throws XmlException on XML error, IllegalArgumentException on recursion error.
   */
  private void addWrNode(XmlNode xmlNode, XmlStructureNodeBuilder node, int recursion) throws XmlException {
    if(recursion <0) throw new IllegalArgumentException();
    for(String name: node.attribs) {
      xmlNode.setAttribute(name, "!@"+name);
    }
    for(Map.Entry<String, XmlStructureNodeBuilder> e: node.nodes.entrySet()) {
      XmlStructureNodeBuilder subnode = e.getValue();
      String tag = e.getKey();
      XmlNodeSimple<?> xmlNodeSub = new XmlNodeSimple<>(tag);
      xmlNode.addContent(xmlNodeSub);
      xmlNodeSub.setAttribute("data", "xmlinput", "!add_" + tag + "()");
      //if(subnode.)
      //TODO problem with beautification
      //xmlNodeSub.addContent("!content(text)");
      addWrNode(xmlNodeSub, subnode, recursion-1);
    }
  }
  

  
  
  
}
