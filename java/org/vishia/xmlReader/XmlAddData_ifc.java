package org.vishia.xmlReader;

import java.util.Map;

public interface XmlAddData_ifc {

  
  /**Creates a new node with all given attrib values.
   * @param tag
   * @param attribs
   * @return data for this node, whereas the attribs are already stored.
   */
  XmlAddData_ifc newNode(String tag, Map<String, String> attribs);
  
  /**Adds a new parsed node to the current list of nodes.
   * @param node
   */
  void addNode(XmlAddData_ifc node);
  
  void addText(String text);
}
