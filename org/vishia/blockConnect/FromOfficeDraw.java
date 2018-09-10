package org.vishia.blockConnect;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.vishia.util.Debugutil;
import org.vishia.xmlReader.XmlJzReader;

public class FromOfficeDraw
{


  static class Connection {
    public String styleName, textStyle, layer, line;

    public String srcBlock = "?", dstBlock = "", srcPort = "?", dstPort = ""; 
    @Override public String toString(){ return "Connection " + srcBlock + "." + srcPort + " --> " + dstBlock + "." + dstPort; }
  }



  static class Port {

    public String portName;
    
    @Override public String toString(){ return "Port " + portName; }
  }

  static class Block {
    public List<Port> ports = new ArrayList<Port>();
    public String styleName, textStyleName, xmlid, drawid, layout;
    
    Port port(){ Port val = new Port(); ports.add(val); return val; } 
    @Override public String toString(){ return "Block " + drawid; }
  }

  static class Page {
    List<Block> blocks = new ArrayList<Block>();
    
    public List<Connection> connections = new ArrayList<Connection>();

    Block newBlock(){ Block val = new Block(); blocks.add(val); return val; }

    Connection newConnection(){ Connection val = new Connection(); connections.add(val); return val; } 
  
    
  }


  static class Data {
    Data main = this;
    
    List<Page> pages = new ArrayList<Page>();
    
    Page page(){ Page page = new Page(); pages.add(page); return page; } 
  
  }
  



  public static void main(String[] args)
  { XmlJzReader main = new XmlJzReader();
    main.readCfg(new File("D:\\vishia\\graphDesign\\draw.tplxml"));
    Data data = new Data();    
    main.readXml(new File("D:\\vishia\\graphDesign\\test1.fodg"), data);
    Debugutil.stop();
  }



}
