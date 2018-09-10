package org.vishia.xmlparser;

import java.util.Map;

public class Xml2J_template
{
  /**Elements with this tagName are processed at the appropriate level. */
  String tagName;
  
  /**The element should contain that attributes (key of map) with the specified values (key of element) or with all values (null as value of this map. */
  Map<String, Map<String,String>> reqAttribs;

  
  /**The element can contain that attributes (key of map). The value of the map is the variable where the attribute value will be stored. */
  Map<String, String> possibleAttribs;

  
  /**The element can contain that sub elements (key of map is the tag name). 
   * The value of the map is the variable where the element content will be stored. */
  Map<String, String> possibleElements;

  
  
  
  
}
