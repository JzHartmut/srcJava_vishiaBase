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
 * @author Hartmut Schorrig, Pinzberg, Germany www.vishia.org
 * @version 2009-08-02  (year-month-day)
 * list of changes: 
 * 2009-08-02: Hartmut change of visibility
 * 2008-03-00: Hartmut creation
 *
 ****************************************************************************/
package org.vishia.zbnf;

//import java.io.File;

//import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
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
public class ZbnfXmlOutput
{

  //final Namespace xhtml = Namespace.getNamespace("http://www.w3.org/1999/xhtml");;
  
  //ZbnfParser parser;
  
  private TreeMap<String, String> xmlnsList;
  
  public ZbnfXmlOutput()
  {
  }
  
  /** writes the parsers store to an xml file
   * @throws XmlException 
   * 
   */
  public void write(ZbnfParser parser, String sFileOut) 
  throws FileNotFoundException, XmlException
  {
    xmlnsList = parser.getXmlnsFromSyntaxPrescript();
    ZbnfParseResultItem zbnfTop = parser.getFirstParseResult();
    Charset encoding = Charset.forName("UTF-8");
    FileOutputStream fileOut = new FileOutputStream(sFileOut);
    OutputStreamWriter out = new OutputStreamWriter(fileOut, encoding);
    //Writer out = new FileWriter(sFileOut);
    write(zbnfTop, xmlnsList, out);
  }
  
  /**Writes the XML Tree.
   * @param zbnfTop
   * @param xmlnsList
   * @param out
   * @throws FileNotFoundException
   * @throws XmlException
   */
  public void write(ZbnfParseResultItem zbnfTop, TreeMap<String, String> xmlnsList, OutputStreamWriter out) 
  throws FileNotFoundException, XmlException
  { //this.parser = parser;
    this.xmlnsList = xmlnsList;
    String sTopLevelSemantic = zbnfTop.getSemantic();
    
    /**Converts the whole parser result. */
    XmlNode xmlTop = newChild(sTopLevelSemantic);  //a new node.
    processComponent(zbnfTop, xmlTop);

    /**Adds the namespace declarations if exists: */
    { if(xmlnsList != null)
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
      SimpleXmlOutputter outputter = new SimpleXmlOutputter();
      outputter.write(out, xmlTop);
      out.flush();
    } 
    catch (IOException e)
    {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }    
  }
  
  
  
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
  private void addToXmlNode(ZbnfParseResultItem item, final XmlNode xmlOut) 
  throws XmlException
  { int posSeparator;
    XmlNode xmlRet = null;
    String sSemantic = item.getSemantic();
    if(sSemantic.equals("elseConditionBlock"))
    	stop();
    if( (posSeparator = sSemantic.indexOf('/')) > 0)
    { 
      String sName = sSemantic.substring(0, posSeparator); //name of xmlNode to search/create
      sSemantic = sSemantic.substring(posSeparator+1);     //the rest

      /**Search all given XML-nodes and add the items content, if exists: */
      Iterator<XmlNode> iter = xmlOut.iterChildren(sName);
      if(iter != null)
      { //maybe only one child.
        while(iter.hasNext())
        { XmlNode xmlAddTo = iter.next();
          xmlRet = xmlAddTo;
          addToXmlNodeFinal(sSemantic, item, xmlAddTo);
        }
      }
      /**create the XML-node because it isn't exists. */
      if(xmlRet == null)
      { XmlNode xmlChild = newChild(sName);
        if(xmlOut != null)
        { xmlOut.addContent(xmlChild);
        }
        xmlRet = xmlChild; 
        addToXmlNodeFinal(sSemantic, item, xmlChild);
      }
    } 
    else
    { addToXmlNodeFinal(sSemantic, item, xmlOut);
      
    }
  }
  



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
  private void addToXmlNodeFinal(final String sTagAttrName, ZbnfParseResultItem item, final XmlNode xmlOut) 
  throws XmlException
  { final XmlNode xmlNew;
    boolean bZbnfComponent = item.isComponent();
    { //Test only
      if(sTagAttrName.equals("visibility"))
        stop();
    }
    if(sTagAttrName.length() == 0)
    { /**the semantic had end with "/", than a text output shouldn't be store.
       * Either the XML-node, which is created before this method is called, 
       * is the parent of the components content, or a node without content should be cretated.
       */
      xmlNew = xmlOut;
    }
    else if(sTagAttrName.startsWith("@"))
    { /**Attribute should be set. */
      int posValue = sTagAttrName.indexOf('=');              //an attribute can defined in form @name=value
      String sNameAttribute;
      if(posValue >=0){ sNameAttribute = sTagAttrName.substring(1, posValue); }
      else{ sNameAttribute = sTagAttrName.substring(1); }
      if(sNameAttribute.length() >0)                      //the given =value is stored if neccessary.
      { String sValue = posValue >=0 ? sTagAttrName.substring(posValue +1) : getValue(item, true);  
        xmlOut.setAttribute(sNameAttribute, sValue);
      }
      xmlNew = xmlOut;  //no new node, because attribute is added
    }
    else
    { String sValue = null;
      if(sTagAttrName.equals("text()") || sTagAttrName.equals("."))
      { /**The last part of the semantic <code>tag/last()</code> is a routine ... TODO */
        sValue = getValue(item, true);
        if(sValue != null && sValue.length() >0)
        { xmlOut.addContent(sValue);
        }
        xmlNew = xmlOut;  //no new node, because text is added
      }
      else
      { /**The last part of the semantic <code>path/tag</code> or the whole semantic is an XML-indentifier.
         * Create a child element with this tagname and add it to the output. 
         * This child, xmlNew, is the parent of the content.*/
        final String sTagName;
        boolean bExpandWikistyle = sTagAttrName.endsWith("+");
        boolean bSetParsedText = sTagAttrName.endsWith("&");
        if(bExpandWikistyle || bSetParsedText)
        { sTagName = sTagAttrName.substring(0, sTagAttrName.length()-1);
        }
        else
        { sTagName = sTagAttrName;
        }
        xmlNew = newChild(sTagName);  //a new node.
        xmlOut.addContent(xmlNew);          //add it to the given parent node.
        //if(bExpandWikistyle){ xmlNew.setAttribute("expandWikistyle", "yes"); }
        if(item != null && (!bZbnfComponent || bSetParsedText)) 
        { sValue = getValue(item, true);
          if(sValue != null && sValue.length() >0)
          { 
            if(bExpandWikistyle)
            { convertWikiStyle.prepareXmlNode(xmlNew, sValue);
            }
            else 
            { xmlNew.addContent(sValue);
            }  
          }
        }
      }
    }  
    
    if(bZbnfComponent)
    { processComponent(item, xmlNew);
    }
    else
    { /**Note: the content is added already above, if it is a component, its done also.*/
    }
  }

  
  
  
  /**Adds the content of the ZBNF-component item to the xmlParent.
   * If the component hasn't own content, the parsed text is try to add.
   * This situation is typical if <code>[<?choice> A| B | C]</code> is processed here.
   * @param item The component
   * @param xmlParent The XML-Node to which the components content should be added.
   * @throws XmlException
   */
  private void processComponent( ZbnfParseResultItem item, final XmlNode xmlParent) 
  throws XmlException
  {
    Iterator<ZbnfParseResultItem> iterZbnf = item.iteratorChildren();
    if(iterZbnf != null && iterZbnf.hasNext())
    { while(iterZbnf.hasNext())
      { ZbnfParseResultItem childItem = iterZbnf.next();
        addToXmlNode(childItem, xmlParent);
      }
    }
    
  }
  
  
  
  
  
  
  protected XmlNode newChild(String sName)
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
    
    
    if(sNamespaceKey != null)
    { xmlChild = new XmlNodeSimple(sName, sNamespaceKey, sNamespaceVal);
    }
    else
    { xmlChild = new XmlNodeSimple(sName);
    }
    
    return xmlChild;
  }  
  
  
  
  
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
  
  
  
  private static class ConvertWikiStyle implements Zbnf2Xml.PrepareXmlNode
  {
    WikistyleTextToSimpleXml wikistyleText2SimpleXml = new WikistyleTextToSimpleXml();

    public void prepareXmlNode(XmlNode xmlDst, String text) throws XmlException
    {
      // TODO Auto-generated method stub
      wikistyleText2SimpleXml.setWikistyleFormat(text, xmlDst, null, null);
    }
    
  }
  
  ConvertWikiStyle convertWikiStyle = new ConvertWikiStyle(); 
  
  
}
