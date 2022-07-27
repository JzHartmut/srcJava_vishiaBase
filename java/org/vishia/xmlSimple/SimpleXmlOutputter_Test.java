package org.vishia.xmlSimple;

import java.io.FileWriter;

/**This class contains an example respectively a test for the {@link SimpleXmlOutputter}
 * 
 * @author Hartmut Schorrig
 *
 */
public class SimpleXmlOutputter_Test
{

  public static void main(String[] args) {
    
    try {
      XmlNode root = new XmlNodeSimple("xmlSimpleRoot");
      XmlNode node2 = root.addNewNode("tag2", null);
      //node2.addContent("This  text\nwith more as one line\r\nthird line.");
      
      SimpleXmlOutputter oXml = new SimpleXmlOutputter();
      FileWriter writer = new FileWriter("Example_SimpleXmlOutputter.xml");
      oXml.write(writer, root);
    } catch(Exception exc) {
      System.err.println(exc.getMessage());
    }
  }
  
  
}
