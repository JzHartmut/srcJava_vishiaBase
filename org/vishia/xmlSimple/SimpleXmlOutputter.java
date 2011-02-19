package org.vishia.xmlSimple;

import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.Map;



public class SimpleXmlOutputter
{

  String newline = "\r\n";
  
  String sIdent="\r\n                                                                                            ";
  
  
  public void write(Writer out, XmlNode xmlNode) 
  throws IOException
  { out.write("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>" + newline);
    out.write("<!-- written with org.vishia.xmlSimple.SimpleXmlOutputter -->");
    writeNode(out, xmlNode, 0);
  }
  
  protected void writeNode(Writer out, XmlNode xmlNode, int level) 
  throws IOException
  { out.write(sIdent.substring(0, 2+level*2));
    out.write(elementStart(xmlNode.name));
    if(xmlNode.attributes != null)
    { Iterator<Map.Entry<String, String>> iterAttrib = xmlNode.attributes.entrySet().iterator();
      while(iterAttrib.hasNext())
      { Map.Entry<String, String> entry = iterAttrib.next();
        String name = entry.getKey();
        String value = entry.getValue();
        out.write(attribute(name, value));
      }
    }  
    if(xmlNode.content != null)
    { out.write(elementTagEnd());
      Iterator<XmlContent> iterContent = xmlNode.content.iterator();
      while(iterContent.hasNext())
      { XmlContent content = iterContent.next();
        if(content.text != null)
        { out.write(convert(content.text) );
        }
        if(content.xmlNode != null)
        { writeNode(out, content.xmlNode, level+1);
        }
      }
      out.write(elementEnd(xmlNode.name));
    }  
    else
    { out.write(elementShortEnd());
    }
  }
  
  
  public static String elementStart(String name)
  { return "<" + name + " ";
  }

  public static String elementTagEnd()
  { return ">";
  }

  public static String elementShortEnd()
  { return "/>";
  }

  public static String elementEnd(String name)
  { return "</" + name + ">";
  }

  public static String attribute(String name, String value)
  { return name + "=\"" + convert(value) + "\" ";
  }
  

  private static String convert(String textP)
  { int pos2;
    StringBuffer[] buffer = new StringBuffer[1]; 
    String[] text = new String[1];
    text[0] = textP;
    if((pos2 = text[0].indexOf('&')) >=0)         //NOTE: convert & first!!!
    { convert(buffer, text, pos2, '&', "&amp;");
    }
    if( (pos2 = text[0].indexOf('<')) >=0)
    { convert(buffer, text, pos2, '<', "&lt;");
    }
    if((pos2 = text[0].indexOf('>')) >=0)
    { convert(buffer, text, pos2, '>', "&gt;");
    }
    if(  (pos2 = text[0].indexOf('\"')) >=0)
    { convert(buffer, text, pos2, '\"', "&quot;");
    }
    if(buffer[0] == null) return textP; //directly and fast if no special chars.
    else return buffer[0].toString();  //only if special chars were present.
  }  
    
  private static void convert(StringBuffer[] buffer, String text[], int pos1, char cc, String sNew)
  {
    if(buffer[0] == null) 
    { buffer[0] = new StringBuffer(text[0]);
    }
    do
    { buffer[0].replace(pos1, pos1+1, sNew);
      text[0] = buffer[0].toString();
      pos1 = text[0].indexOf(cc, pos1+sNew.length());
    } while(pos1 >=0);  
  }
}