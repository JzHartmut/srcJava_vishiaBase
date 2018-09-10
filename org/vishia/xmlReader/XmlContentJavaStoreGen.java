package org.vishia.xmlReader;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import org.vishia.cmd.JZtxtcmdTester;
import org.vishia.util.Assert;
import org.vishia.util.Debugutil;
import org.vishia.xmlSimple.SimpleXmlOutputter;
import org.vishia.xmlSimple.XmlException;
import org.vishia.xmlSimple.XmlNode;
import org.vishia.xmlSimple.XmlNodeSimple;

public class XmlContentJavaStoreGen
{
  /**Version, License and History:
   * <ul>
   * <li>2018-09-09 created.
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
  public static final String version = "2018-09-09";

  XmlStructureNodeBuilder data = new XmlStructureNodeBuilder("root");  //the root node for reading config

  
  public void readXmlStruct(File fXmlIn) {
    XmlJzReader xmlReader = new XmlJzReader();
    xmlReader.readXml(fXmlIn, data, XmlCfg.newCfgReadStruct());
    
    Debugutil.stop();
  }

  
  public static void main(String[] args) {
    XmlContentJavaStoreGen main = new XmlContentJavaStoreGen();
    main.readXmlStruct(new File("D:/ML/SULtrcCurve/SULtrc.xml"));
    Debugutil.stop();
    
    try {
      JZtxtcmdTester.dataHtml(main.data, new File("T:/datashow.html"));
    } catch (NoSuchFieldException | IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  
 
}
