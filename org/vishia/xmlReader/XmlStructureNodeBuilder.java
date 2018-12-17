package org.vishia.xmlReader;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.vishia.util.Debugutil;
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
  
  /**Set if at least one of the occurences has a text content.*/
  boolean bText = false;
  
  /**Found sub nodes. The list is supplemented if new sub nodes are found on further occurrences of elements. */
  Map<String, XmlStructureNodeBuilder> nodes = new IndexMultiTable<String, XmlStructureNodeBuilder>(IndexMultiTable.providerString);
  
  /**Found attributes. The list is supplemented if new attribute names are found on further occurrences of elements. */
  Map<String, String> attribs = new TreeMap<String, String>();
  
  
  int nrofAttributes = 0;
  
  /**Set if new attributes are found. Then the 
   * 
   */
  boolean bNewAttributes = false; 
  
  
  public XmlStructureNodeBuilder(String tag){ this.tag = tag; }
  
  /**Returns an instance to store the occurrence of a XML-element.
   * If more XML-elements with the same tag name are found, only one occurrence of this element type is stored.
   * If an element with the same tag name is given, which is already registered, then the same instance is returned from the found element before.
   * With them maybe new occurring attributes or new occurring elements are stored. 
   * It is invoked via reflection from {@link XmlCfg#newCfgReadStruct()}
   * @param tag
   * @return
   */
  public XmlStructureNodeBuilder addElement(String tag) { 
    XmlStructureNodeBuilder ret = nodes.get(tag); //use existent one with same tag to strore further content.
    if(ret == null) {
      ret = new XmlStructureNodeBuilder(tag); 
      nodes.put( tag, ret);
    } else {
      if(ret.attribs.size() >0) {
        Debugutil.stop();
      }
    }
    return ret; 
  }
  
  
  /**It is invoked via reflection from {@link XmlCfg#newCfgReadStruct()}
   * @param tag
   * @return
   */
  public void setAttribute(String name) { 
    if(attribs.get(name) ==null) {
      bNewAttributes = true;
      attribs.put(name, name);
    }
  }

  
  public void setTextOccurrence() { bText = true; }
  
}
