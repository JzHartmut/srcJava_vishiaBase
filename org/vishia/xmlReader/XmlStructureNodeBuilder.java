package org.vishia.xmlReader;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.vishia.util.IndexMultiTable;

/**This class stores a reading content from a XML file via {@link XmlJzReader}
 * in a form that the structure of the xml file is stored to use as Configuration in a next step.
 * This class builds the root node and any child node too.
 * <pre>
 * XmlCfgNodeBuilder<*>--nodes-->XmlCfgNodeBuilder
 *                                  <*> 
 *                                   |
 *                                   +--attribs--->String
 * </pre>
 * @author Hartmut Schorrig
 *
 */
public class XmlStructureNodeBuilder
{
  final String tag;
  
  Map<String, XmlStructureNodeBuilder> nodes = new IndexMultiTable<String, XmlStructureNodeBuilder>(IndexMultiTable.providerString);
  
  List<String> attribs = new ArrayList<String>();
  
  public XmlStructureNodeBuilder(String tag){ this.tag = tag; }
  
  public XmlStructureNodeBuilder addElement(String tag) { 
    XmlStructureNodeBuilder ret = nodes.get(tag); //use existent one with same tag to strore further content.
    if(ret == null) {
      ret = new XmlStructureNodeBuilder(tag); 
      nodes.put( tag, ret);
    }
    return ret; 
  }
  
  
  public void setAttribute(String name) { attribs.add(name); }

}
