/****************************************************************************/
/* Copyright/Copyleft: 
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
 * @author www.vishia.de/Java
 * @version 2006-06-15  (year-month-day)
 * list of changes: 
 * 2006-05-00: www.vishia.de creation
 *
 ****************************************************************************/
package org.vishia.zbnf;

//import java.io.File;

//import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Iterator;
import java.util.TreeMap;


import org.vishia.xmlSimple.WikistyleTextToSimpleXml;
import org.vishia.xmlSimple.XmlException;
import org.vishia.xmlSimple.XmlNode;
import org.vishia.xmlSimple.SimpleXmlOutputter;
import org.vishia.xmlSimple.XmlNodeSimple;
import org.vishia.zbnf.ZbnfParseResultItem;
import org.vishia.zbnf.ZbnfParser;


/** Class to write the parsers store from vishia.stringScan.Parser to xml.
 * Special semantic ussages:
 * <ul><li><code>name1/name2</code>: creates first the xml-element name1 as child, and additional a child element name2</li>
 * <li><code>.</code>: adds the content of child into the actual xml-element</li>
 * <li><code>name/.</code>: adds the content to child, same as simple <code>name1</code></li>
 * <li><code>@name</code>: Writes to the attribute</li>
 * <li><code>name1/@name2</code>: Create the child name1 and write into the attribute name2  </li>
 * <li><code>name+</code>: Expands the content with {@link vishia.xml.ConverterWikistyleTextToXml}</li>
 * <li><code>name%</code>: Writes also the parsed syntax as content, if no other content is stored. 
 *      Especially for options with no deeper semantic statements like <code>[<?option> a|b|c]</code></li>
 * </ul>  
 * 
 *
 */
class ZbnfXmlOutput
{

  //final Namespace xhtml = Namespace.getNamespace("http://www.w3.org/1999/xhtml");;
  
  ZbnfParser parser;
  
  TreeMap<String, String> xmlnsList;
  
  ZbnfXmlOutput()
  {
  }
  
  //void write(ParseResultItem item, String sFileOut)
  /** writes the parsers store to an xml file
   * @throws XmlException 
   * 
   */
  void write(ZbnfParser parser, String sFileOut) 
  throws FileNotFoundException, XmlException
  { this.parser = parser;
    xmlnsList = parser.getXmlnsFromSyntaxPrescript();
    
    ZbnfParseResultItem item = parser.getFirstParseResult();
    XmlNode xmlTop = addToXmlNode(item, null);  //builds the whole XML tree recursively.
    { TreeMap<String, String> xmlnsList = parser.getXmlnsFromSyntaxPrescript();
      if(xmlnsList != null)
      { Iterator<String> iter = xmlnsList.keySet().iterator();
        while(iter.hasNext())
        { String nsKey = iter.next();
          String nsVal = xmlnsList.get(nsKey);
          //xmlTop.addNamespaceDeclaration(Namespace.getNamespace(nsKey, nsVal));
          xmlTop.addNamespaceDeclaration(nsKey, nsVal);
        }
      }      
    }
    
    WikistyleTextToSimpleXml toWikistyle = new WikistyleTextToSimpleXml(); 
    toWikistyle.testXmlTreeAndConvert(xmlTop);
    try
    {
      Writer out = new FileWriter(sFileOut);
      SimpleXmlOutputter outputter = new SimpleXmlOutputter();
      outputter.write(out, xmlTop);
      out.flush();
    } 
    catch (IOException e)
    {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }    
    //cc ConverterWikistyleTextToXml toWikistyle = new ConverterWikistyleTextToXml(); 
    //cc toWikistyle.testXmlTreeAndConvert(xmlTop);
    
    /*cc
    XmlExtensions.XmlMode mode = new XmlExtensions.XmlMode();
    mode.setXmlIso8859();
    if(true) //mode.isIndent())
    { xmlTop = XmlExtensions.beautificationBewareTextContent(xmlTop);
    }
    try
    { XmlExtensions.writeXmlFile(xmlTop, new File(sFileOut), mode);
    }
    catch (XmlException e)
    {
      // TODO Auto-generated catch block
    }
    */
  }
  
  /*
  private XmlNode writeXmlNode(ParseResultItem parent)
  { 
    if(parent == null)
    { //The top xml element.
      
    }
    String sSemantic = parent.getSemantic();
    XmlNode xmlOut = new XmlNode(sSemantic);
    //writeAttributes(xmlOut, parent);
    ParseResultItem item = parent.nextSkipIntoComponent(parent);
    while(item != null)
    { if(item.isComponent())
      { //recursively call:
        xmlOut.addContent(writeXmlNode(item));
      }
      else
      { if(!item.isOption() || item.getNrofAlternative() >0 )
        { addToXmlNode(item, xmlOut);
        }
      }
      item = item.next(parent);
    }
    return xmlOut;
  }
  */
  
  
  /**adds the content of the parse result item to the xml tree or creates the toplevel element.
   * 
   * @param item The current item. If it is a component, this routine will be called
   *         recursively.
   * @param xmlOut The parent element to be add the content. It may be null, than
   *         the created element will be the toplevel element. In this case the semantic of item
   *         should be contain only a simple identifier to create one element.
   * @return the deepest created element. It is the toplevel element if param xmlOut
   *         is null, and only one level of elements is created. Only in this constellation
   *         the returned element is necessary and usefull.
   * @throws XmlException 
   */
  @SuppressWarnings("deprecation")
  private XmlNode addToXmlNode(ZbnfParseResultItem item, XmlNode xmlOut) 
  throws XmlException
  { int posSeparator;
    String sSemantic = item.getSemantic();
    if(sSemantic.equals("elseConditionBlock"))
    	stop();
    {
      while( (posSeparator = sSemantic.indexOf('/')) > 0)
      { String sName = sSemantic.substring(0, posSeparator);
        sSemantic = sSemantic.substring(posSeparator+1);

        XmlNode xmlChild = newChild(sName, null);
        if(xmlOut == null)
        { xmlOut = xmlChild; 
        }
        else
        { xmlOut.addContent(xmlChild);
          xmlOut = xmlChild;
        } 
      }

      if(sSemantic.length() == 0)
      { //the semantic had end with "/", the rest is empty, no text output inside the element.
      }
      else if(sSemantic.startsWith("@"))
      { int posValue = sSemantic.indexOf('=');              //an attribute can defined in form @name=value
        String sNameAttribute;
        if(posValue >=0){ sNameAttribute = sSemantic.substring(1, posValue); }
        else{ sNameAttribute = sSemantic.substring(1); }
        if(sNameAttribute.length() >0)                      //the given =value is stored if neccessary.
        { String sValue = posValue >=0 ? sSemantic.substring(posValue +1) : getValue(item, true);  
          xmlOut.setAttribute(sNameAttribute, sValue);
        }
      }
      else if(sSemantic.equals("text()") || sSemantic.equals("."))
      { xmlOut.addContent(getValue(item, true));
      }
      else
      { XmlNode xmlChild = newChild(sSemantic, item);
        if(xmlOut != null)
        { xmlOut.addContent(xmlChild);
        } 
        xmlOut = xmlChild; 
      }  
      
      if(item.isComponent())
      { ZbnfParseResultItem childItem;
        childItem = item.nextSkipIntoComponent(item);
        while(childItem != null)
        { //TODO what were this for a question?
        	//if(!item.isOption() || item.getNrofAlternative() >0 )
          { addToXmlNode(childItem, xmlOut);
          }
          childItem = childItem.next(item);
        }
      }
    }
    return xmlOut;
  }

  
  
  
  
  
  XmlNode newChild(String sName, ZbnfParseResultItem item)
  { XmlNode xmlChild;
  
    int idxNs;
    String sNamespaceKey;
    String sNamespaceVal;
    if( (idxNs = sName.indexOf(':')) >0)
    { sNamespaceKey = sName.substring(0, idxNs);
      sNamespaceVal = (String)xmlnsList.get(sNamespaceKey);
      sName = sName.substring(idxNs+1);
    }
    else
    { sNamespaceKey = null;
      sNamespaceVal = null;
    }
    
    boolean bExpandWikistyle = sName.endsWith("+");
    boolean bSetParsedText = sName.endsWith("&");
    if(bExpandWikistyle || bSetParsedText)
    { sName = sName.substring(0, sName.length()-1);
    }

    if(sNamespaceKey != null)
    { xmlChild = new XmlNodeSimple(sName, sNamespaceKey, sNamespaceVal);
    }
    else
    { xmlChild = new XmlNodeSimple(sName);
    }
    
    if(bExpandWikistyle){ xmlChild.setAttribute("expandWikistyle", "yes"); }
    
    if(item != null) 
    { String sValue;
      if(bSetParsedText)
      { sValue = item.getParsedText();  //TODO: mostly no text is stored.
      }
      else
      { sValue = getValue(item, bExpandWikistyle);
      }
      if(sValue != null && sValue.length() >0)
      { xmlChild.addContent(sValue);
      }
    }
    return xmlChild;
  }  
  
  
  
  
  /*
  void writeAttributes(XmlNode xmlOut, ParseResultItem item)
  { boolean bWriteSrc = false;
    if(item.isFloat())
    { xmlOut.setAttribute("float", "" + item.getParsedFloat());
      bWriteSrc = true;
    }
    else if(item.isInteger())
    { xmlOut.setAttribute("int", "" + item.getParsedInteger());
      bWriteSrc = true;
    }
    else if(item.isIdentifier())
    { xmlOut.setAttribute("ident", "" + item.getParsedString());
    }
    else if(item.isString())
    { xmlOut.setAttribute("string", "" + item.getParsedString());
    }
    
    if(item.isRepeat()>0)
    { xmlOut.setAttribute("repeat", "" + item.isRepeat());
    }
    
    if(item.isRepetition()>0)
    { xmlOut.setAttribute("repetition", "" + item.isRepetition());
    }
    
    { String sParsedText = item.getParsedString();
      if(sParsedText == null)
      { sParsedText = item.getParsedText();
      }
      if(sParsedText != null)
      { xmlOut.setAttribute("src", sParsedText);
      }
    }
    { int alternative = item.getNrofAlternative();
      if(alternative >=0) // && alternative !=1)
      { xmlOut.setAttribute("alternative", "" + alternative);
      }
    }
  }
  */
  
  private String getValue(ZbnfParseResultItem item, boolean bGetParsedString)
  { if(item.isFloat())
    { return "" + item.getParsedFloat();
    }
    else if(item.isInteger())
    { return "" + item.getParsedInteger();
    }
    else if(item.isIdentifier())
    { return "" + item.getParsedString();
    }
    else if(item.isRepeat()>0)
    { return "" + item.isRepeat();
    }
    else if(item.isRepetition()>0)
    { return "" + item.isRepetition();
    }
    else if(item.isString())
    { return item.getParsedString();
    }
    else if(bGetParsedString)
    { //not ones of, test wether a string is saved or the parsed text.
      String sRet = item.getParsedString();
      if(sRet == null){ sRet = item.getParsedText(); }
      if(sRet == null){ sRet = ""; }
      return sRet;
    }
    else return null;  //nothing
  }

  
  /**It's a debug helper. The method is empty, but it is a mark to set a breakpoint. */
  void stop()
  {
    //only for debugging
  }
  
}
