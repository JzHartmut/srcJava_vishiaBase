/****************************************************************************
 * Copyright/Copyleft:
 *
 * For this source the LGPL Lesser General Public License,
 * published by the Free Software Foundation is valid.
 * It means:
 * 1) You can use this source without any restriction for any desired purpose.
 * 2) You can redistribute copies of this source to everybody.
 * 3) Every user of this source, also the user of redistribute copies
 *    with or without payment, must accept this license for further using.
 * 4) But the LPGL ist not appropriate for a whole software product,
 *    if this source is only a part of them. It means, the user
 *    must publish this part of source,
 *    but don't need to publish the whole source of the own product.
 * 5) You can study and modify (improve) this source
 *    for own using or for redistribution, but you have to license the
 *    modified sources likewise under this LGPL Lesser General Public License.
 *    You mustn't delete this Copyright/Copyleft inscription in this source file.
 *
 * @author JcHartmut = hartmut.schorrig@vishia.de
 * @version 2006-06-15  (year-month-day)
 * list of changes:
 * 2008-01-15: JcHartmut www.vishia.de creation
 * 2008-04-02: JcHartmut some changes
 *
 ****************************************************************************/
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
  
  protected void writeNode(Writer out, XmlNode xmlNode, int nIndent) 
  throws IOException
  { if(nIndent >=0 && nIndent < sIdent.length()/2)
    { out.write(sIdent.substring(0, 2+nIndent*2));
    }
    String sTagName;
    if(xmlNode.getNamespaceKey() != null)
    { sTagName = xmlNode.getNamespaceKey() + ":" + xmlNode.getName();
    }
    else
    { sTagName = xmlNode.getName();
    }
    out.write(elementStart(sTagName));
    if(xmlNode.getAttributes() != null)
    { Iterator<Map.Entry<String, String>> iterAttrib = xmlNode.getAttributes().entrySet().iterator();
      while(iterAttrib.hasNext())
      { Map.Entry<String, String> entry = iterAttrib.next();
        String name = entry.getKey();
        String value = entry.getValue();
        out.write(attribute(name, value));
      }
    }  
    if(xmlNode.getNamespaces() != null)
    { Iterator<Map.Entry<String, String>> iterNameSpaces = xmlNode.getNamespaces().entrySet().iterator();
      while(iterNameSpaces.hasNext())
      { Map.Entry<String, String> entry = iterNameSpaces.next();
        String name = entry.getKey();
        String value = entry.getValue();
        out.write(attribute("xmlns:" + name, value));
      }
    }  
    Iterator<XmlNode> iterContent = xmlNode.iterChildren();
    if(iterContent != null) 
    { out.write(elementTagEnd());
      while(iterContent.hasNext())
      { XmlNode content = iterContent.next();
        if(content.isTextNode())
        { out.write(convert(content.getText()) );
          nIndent = -1;  //no indentation, write the rest and all subnodes in one line.
        }
        else
        { //if nIndent<0, write no indent in next node level.
          writeNode(out, content, nIndent >=0 ? nIndent+1 : -1);
        }
      }
      out.write(elementEnd(sTagName));
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