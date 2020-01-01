package org.vishia.xmlReader.test;

import java.io.File;
import java.io.IOException;

import org.vishia.util.Debugutil;
import org.vishia.xmlReader.XmlCfg;
import org.vishia.xmlReader.XmlJzReader;


public class Test_Office_odtData {
  
  
  public static void main(String[] args) {
    Test_Office_odtData main = new Test_Office_odtData();
    try {
      main.readXmlMap();
    } catch (Exception e) {
      System.err.println("Unexpected: " + e.getMessage());
      e.printStackTrace(System.err);
    }
  }
  
  
  XmlJzReader xmlReader = new XmlJzReader();
  
  
  
  private void readXmlMap() throws IOException {
    XmlCfg cfg = xmlReader.readCfg(new File("Office_odt.cfg.xml"));
    if(cfg !=null) {
      Office_odtData dst = new Office_odtData_Zbnf();
      File fileToRead = new File("XmlJzReader_de.xml");
      String error = xmlReader.readXml(fileToRead, dst);
      if(error !=null) {
        System.err.println(error);
      }
      Debugutil.stop(); 
    }
  }

}
