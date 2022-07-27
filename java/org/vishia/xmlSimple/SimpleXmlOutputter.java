package org.vishia.xmlSimple;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.vishia.util.Assert;
import org.vishia.util.Debugutil;



/**This class writes any {@link XmlNode} to a textual output. Because the interface {@link XmlNode} may be used 
 * for several Xml tree representation, 
 * for example via {@link org.vishia.xml.XmlNodeJdom} for a {@linkplain www.jdom.org} XML tree, it is universal to use.
 * @author Hartmut Schorrig, www.vishia.org
 *
 */
public class SimpleXmlOutputter
{

  /**Version, history and license.
   * <ul>
   * <li>2020-02-12 new {@link #write(Writer, CharSequence, XmlNode)} especially for test outputs 
   *   which should stored only locally without a File using.
   * <li>2013-07-28: Hartmut chg: Uses a {@link BufferedWriter} internally. The {@link BufferedWriter#newLine()}
   *   is not invoked therefore the platform depending line separator is not used. The line separator is still 
   *   a simple '\n'. But the output may be faster, or not because the Writer outside may have a buffer too.
   *   That BufferedWriter is included as debug helper primary. The output String should be seen. 
   *   Some comments and test features added.
   * <li>2013-07-28: Hartmut new The {@link #convertString(String)} is now public.
   * <li>2009-05-24: Hartmut The out arg for write is a OutputSteamWriter, not a basic Writer. 
   *             Because: The charset of the writer is got and written in the head line.
   *             The write routine should used to write a byte stream only.
   * <li>2008-04-02: Hartmut some changes
   * <li>2008-01-15: Hartmut www.vishia.de creation
   * </ul>
   * known bugs and necessary features:
   * <ul>
   * <li>2008-05-24: Hartmut if US-ASCII-encoding is used, '?' is written on unknown chars yet.
   * </ul>
   * 
   * <b>Copyright/Copyleft</b>:
   * For this source the LGPL Lesser General Public License, published by the Free Software Foundation is valid.
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
   * 
   */
  @SuppressWarnings("hiding")
  static final public int version = 20121130;


  enum ModeBits {
    writeNl(0x1)
  , writeNlAftertext(0x2)
  
  , writeAttribsInAlphabeticOrder(0x10)
  ;  
    
    final int maskbit;
    ModeBits(int value){ maskbit = value; }
  }
  
  
  int mode;
  
  
  
  String newline = "\r\n";
  
  String sIdent="\r\n                                                                                            ";
  
  
  /**Writes the XML tree into a byte stream and closes after them. Uses the encoding from the given OutputStreamWriter
   * to show the encoding in the file.
   * @param out A Writer to convert in a Byte stream.
   * @param xmlNode The top level node
   * @throws IOException
   */
  public void write(OutputStreamWriter out, XmlNode xmlNode) 
  throws IOException
  { String sEncodingCanonicalName = out.getEncoding();
    Charset charset = Charset.forName(sEncodingCanonicalName);
    String sEncoding = charset.displayName();
    try{
      write(out, sEncoding, xmlNode);
    }
    finally {
      out.close();
    }
  }
  
  
  
  /**Writes the XML tree into a character stream and closes after them. Uses the given encoding
   * to show the encoding in the file.
   * @param out A Writer better for String output in comparison to the Stream (byte, encoded) output.
   * @param sEncoding the encoding String stored in the XML file. Note: It should be matching to the Writer.
   * @param xmlNode The top level node
   * @throws IOException
   */
  public void write(Writer out, CharSequence sEncoding, XmlNode xmlNode) 
  throws IOException
  { BufferedWriter bout = new BufferedWriter(out);
    try {
      out.write("<?xml version=\"1.0\" encoding=\"" + sEncoding + "\"?>" + newline);
      out.write("<!-- written with org.vishia.xmlSimple.SimpleXmlOutputter -->");
      writeNode(bout, xmlNode, 0);
      bout.write(newline); //finsih with a new line.
    }
    finally {
      bout.close();
    }
  }
  
  
  
  
  protected void writeNode(Writer out, XmlNode xmlNode, int nIndent) 
  throws IOException
  { if(nIndent >=0 && nIndent < sIdent.length()/2)
    { out.write(sIdent.substring(0, 2+nIndent*2));
    }
    final String sElementName = xmlNode.getName();
    if(sElementName.equals("em")){
      Assert.stop();
    }
    final String sTagName;
    
    if(xmlNode.getNamespaceKey() != null)
    { sTagName = xmlNode.getNamespaceKey() + ":" + sElementName;
    }
    else
    { sTagName = sElementName;
    }
    Assert.check(!sTagName.startsWith("@"));
    out.write(elementStart(sTagName));      //out: <ns:tag
    if((mode & ModeBits.writeAttribsInAlphabeticOrder.maskbit) !=0) {
      Map<String, String> attribs = xmlNode.getAttributes();
      if(attribs !=null) {
        Iterator<Map.Entry<String, String>> iterAttrib = attribs.entrySet().iterator();
        while(iterAttrib.hasNext())
        { Map.Entry<String, String> entry = iterAttrib.next();
          String name = entry.getKey();
          String value = entry.getValue();
          out.write(attribute(name, value));
        }
      }
    } else {
      /*
      List<XmlNode> children = xmlNode.listChildren();
      if(children !=null){
        for(XmlNode xmlAttr: children){
          String sName = xmlAttr.getName();
          if(sName.startsWith("@")){
            String value = xmlAttr.getText();
            out.write(attribute(sName.substring(1), value));
          }
        }
      }
      */
      List<String[]> listAttribs = xmlNode.getAttributeList();
      if(listAttribs != null) {
        for(String[] elem: listAttribs) {
          out.write(attribute(elem[0], elem[1]));
        }
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
    boolean bContent= false;  //set to true if </endTag> is necessary
    boolean bSubNode = false;
    if(iterContent != null)    //at least one child node is present: 
    { while(iterContent.hasNext()) {
        XmlNode content = iterContent.next();
        //String sName = content.getName();
        String textContent;
        if(content.isTextNode()) { // && (textContent = content.text()).length() >0){ 
          textContent = content.text();
          if(!bContent){
            out.write(elementTagEnd());
            bContent = true;
          }
          out.write(convertString(textContent).toString());
          nIndent = -1;  //no indentation, write the rest and all subnodes in one line.
        } else if(!content.getName().startsWith("@")){ 
          if(!bContent){
            out.write(elementTagEnd());
            bContent = true;
          }
          //if nIndent<0, write no indent in next node level.
          writeNode(out, content, nIndent >=0 ? nIndent+1 : -1);
          bSubNode = true;
        }
      }
    } else {
      //NOTE: get the text() only if there is no children. Otherwise the summary of text nodes are gotten, that were wrong.
      String text = xmlNode.text(); //the node has not children, but may have text
      if(text !=null && text.length() >0){
        out.write(elementTagEnd());
        bContent = true;
        out.write(convertString(text).toString());
      }
    }
    if(bContent) {
      if(bSubNode && nIndent >=0 && nIndent < sIdent.length()/2)
      { out.write(sIdent.substring(0, 2+nIndent*2));
      }
      out.write(elementEnd(sTagName));
    }
    else { 
      out.write(elementShortEnd());
    }
  }
  
  
  public static String elementStart(String name)
  { return "<" + name;
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
  { return " " + name + "=\"" + convertString(value) + "\" ";
  }
  

  /**Replaces XML special character.
   * @param textP
   * @return textP without effort if such special character are not present,
   *   elsewhere a prepared string in a StringBuilder.
   */
  public static CharSequence convertString(String textP)
  { int pos2;
    StringBuilder[] buffer = new StringBuilder[1]; 
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
    if(  (pos2 = text[0].indexOf('\'')) >=0)
    { convert(buffer, text, pos2, '\'', "&apos;");
    }
    if((pos2 = text[0].indexOf('\n')) >=0)
    { convert(buffer, text, pos2, '\n', "&#x0a;");
    }
    if((pos2 = text[0].indexOf('\r')) >=0)
    { convert(buffer, text, pos2, '\n', "&#x0d;");
    }
    if(buffer[0] == null) return textP; //directly and fast if no special chars.
    else return buffer[0]; //.toString();  //only if special chars were present.
  }  
    
  private static void convert(StringBuilder[] buffer, String text[], int pos1, char cc, String sNew)
  {
    if(buffer[0] == null) 
    { buffer[0] = new StringBuilder(text[0]);
    }
    do
    { buffer[0].replace(pos1, pos1+1, sNew);
      text[0] = buffer[0].toString();
      pos1 = text[0].indexOf(cc, pos1+sNew.length());
    } while(pos1 >=0);  
  }
}
