package org.vishia.blockConnect;

import java.io.File;
import java.io.IOException;

import org.vishia.util.Debugutil;
import org.vishia.xmlReader.XmlJzReader;

public class ExampleSimulink
{


  public static class Data {
  
  
  }



  public static void main(String[] args) {
  
    XmlJzReader main = new XmlJzReader();
    try {
      main.readCfg(new File("D:\\vishia\\graphDesign\\Smlk\\blockdiagram.cfg.xml"));
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    Data data = new Data();
    File slx = new File("D:\\vishia\\graphDesign\\Smlk\\ex_model.slx");
    main.readZipXml(slx, "simulink/blockdiagram.xml", data);
    Debugutil.stop();
  
  
  }

}
