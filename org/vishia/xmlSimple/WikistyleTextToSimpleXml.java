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
 * @author www.vishia.de/Java
 * @version 2006-06-15  (year-month-day)
 * list of changes: 
 * 2006-05-00 JcHartmut: creation
 *
 ****************************************************************************/
package org.vishia.xmlSimple;

import java.util.ListIterator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.vishia.xmlSimple.XmlException;
import org.vishia.xmlSimple.XmlNode;
import org.vishia.util.SpecialCharStrings;

import org.vishia.mainCmd.Report;



/** This class converts a string or the text content of a XML element
 * inside a XML tree according to part of the rules used in Wikipedia-online encyclopaedia.
 * <br>
 * Therewith, if an text field in pure ASCII is converted to XML inside some tools, 
 * it is able to write a formatted text in the wikipedia-style and get the XML-formatted text as result. 
 * <br>
 * Pay attention regarding JDOM: This software works prosperously only with a newer version of JDOM since Jan-2007.
 * With older versions (the actual version in 12/2006) use a corrected version of org/jdom/ContentList.java
 * inside the vishia-Download. This version is modified slightly, 
 * but important by using <code>anyElement.getChildren().listIterator().add(newContent);</code>.
 * <br>
 * The followed rules are considered:<br>
 * <br><u>paragraph format rules:</u>
 * <table border="1" width="100%">
 * <tr><th>Writing</th>               <th>XML-result</th>                    <th>explanation</th></tr>
 * <tr><td><code>text<br><br>next paragraph</code></td>
 *                                    <td><code>&lt;p>text&lt;/p>&lt;p>next paragraph...</code></td>             
 *                                      <td>Two linefeeds forces separated paragraphs. Only one linefeed will be 
 *                                          interpreted as white space. A carrige return (hexa 0d) 
 *                                          before linefeed (hexa 0a)will be ignored.</td></tr>
 * <tr><td><code>*text</code></td>    <td><code>&lt;ul>&lt;li>&lt;p>text...</code></td>             
 *                                      <td>An asterix at beginning of a line (after a linefeed) determines the beginning of a
 *                                          list item. The &lt;ul> will be created if it is the first list item.
 *                                          One space after the asterix char will be ignored.</td></tr>
 * <tr><td><code>**text</code></td>   <td><code>&lt;ul>&lt;li>...<br>
 *                                              ..&lt;ul>&lt;li>&lt;p>text...</code></td>             
 *                                      <td>Creates inside a list item a new list, or a next list item,
 *                                          if two list level are already present.</td></tr>
 * <tr><td><code>*:text</code></td>   <td><code>&lt;ul>&lt;li>...<br>
 *                                              ..&lt;dd>text...&lt;/dd></code></td>             
 *                                      <td>In Wikipedia Software it is used often to create an second paragraph 
 *                                          in a list item. But in reality it is a &lt;dd>-element of a definition list.
 *                                          Thats why an indentation is shown using normal CSS styles.</td>
 * <tr><td><code>*+text</code></td>   <td><code>&lt;ul>&lt;li>...<br>
 *                                              ..&lt;p>text...</code></td>             
 *                                      <td>This is not standard in wikipedia, but necessary for exact XHTML image of the content.
 *                                          It will produced a second paragraph inside a list item.    
 *                                          It will not create a new List item AND an indentation like using *: in Wikipedia, 
 *                                          in Wikipedia editing there is no exactly possibility to add a second paragraph
 *                                          inside a list item.
 *                                          </td></tr>
 * <tr><td><code>;text</code></td>    <td><code>&lt;dl>&lt;dt>text...</code></td>             
 *                                      <td>A definition text of a definition list.
 *                                         The &lt;dl> will be created if it is the first list item. 
 *                                      </td></tr>
 * <tr><td><code>:text</code></td>    <td><code>&lt;dl>&lt;dd>text...</code></td>             
 *                                      <td>A definition data content of a definition list.
 *                                         The &lt;dl> will be created if it is the first list item. 
 *                                      </td></tr>
 * <tr><td><code>*;text</code></td>    <td><code>&lt;li>...&lt;dl>&lt;dd>text...</code></td>             
 *                                      <td>All list item types may be used nested, the correct nested XHTML content will be produced.
 *                                      See also examples below! 
 *                                      </td></tr>
 * <tr><td><code>>text</code></td>    <td><code>&lt;div id="indentation1">&lt;p>text...</code></td>             
 *                                      <td>A paragraph with indentation. It results in an < div>-Tag.
 *                                          The &lt;div>-tag is only created at first such paragraph.
 *                                          It is not possible on wikipedia editing.</td></tr>
 * <tr><td><code>>>text</code></td>   <td><code>&lt;div id="indentation2">&lt;p>text...</code></td>             
 *                                      <td>Two level indentation.</td></tr>
 * <tr><td><code> text</code></td><td><code>&lt;pre>text...</code></td>             
 *                                      <td>preserved text, if one space is present after any line feed.</td></tr>
 * <tr><td><code>, text</code></td><td><code>&lt;pre>text...</code></td>             
 *                                      <td>preserved text, if a colon followed by one space is present after any line feed.</td></tr>
 * <tr><td><code>{| text</code></td><td><code>&lt;table>...</code></td>             
 *                                      <td>Start of a table. See example below.</td></tr>
 * <tr><td><code>!! text</code></td><td><code>&lt;th>...</code></td>             
 *                                      <td>Start of a table head cell. 
 *                                      This string may be also placed inside a table-row-line, not only at start of line.</td></tr>
 * <tr><td><code>|| text</code></td><td><code>&lt;td>...</code></td>             
 *                                      <td>Start of a table cell. 
 *                                      This string may be also placed inside a table-row-line, not only at start of line.</td></tr>
 * <tr><td><code>|- text</code></td><td><code>&lt;tr>...</code></td>             
 *                                      <td>Start of a table row.</td></tr>
 * <tr><td><code>|} text</code></td><td><code>&lt;table>...</code></td>             
 *                                      <td>Start of a table.</td></tr>
 * </table> 
 *
 * <br><u>character format rules:</u>
 * <table border="1" width="100%">
 * <tr><th>Writing</th>                    <th>XML-result</th>                    <th>explanation</th></tr>
 * <tr><td><code>''text''</code></td>      
 *   <td><code>&lt;i>text&lt;/i></code></td>             
 *   <td>italic text</td></tr>
 * <tr><td><code>'''text'''</code></td>    
 *   <td><code>&lt;b>text&lt;/b></code></td>             
 *   <td>bold text</td></tr>
 * <tr><td><code>'''''text'''''</code></td>
 *   <td><code>&lt;b>&lt;i>text&lt;/i>&lt;/b></code></td>
 *   <td>bold and italic text</td></tr>
 * <tr><td><code>,,text,,</code></td>      
 *   <td><code>&lt;code>text&lt;/code></code></td>       
 *   <td>Text for representation coding.
 *     This is not defined in Wikipedia. It is an enhancement.</td></tr>
 * <tr><td><code>[[link|text]]</code></td> 
 *   <td><code>&lt;a href="#link">text&lt;/a></code></td>             
 *   <td>a link to a internal label, adequate to a link to a article in wikipedia.</td></tr>
 * <tr><td><code>[[link]]</code></td>      
 *   <td><code>&lt;a href="#link">link&lt;/a></code></td>             
 *   <td>a link to a internal label, adequate to a link to a article in wikipedia.</td></tr>
 * <tr><td><code>[[!link|text]]</code></td>      
 *   <td><code>&lt;a href="link">text&lt;/a></code></td>             
 *   <td>a link to a absolute label.</td></tr>
 * </table> 
 *
 *<hr>Examples:<br>
 *<pre>
{|!!column-title                 !!column-title2
|-||cell 11                      ||cell 12
/-||cell 21 may be have a longer text
||thats why cell 22 is written in a new line, but cell 22 contains
 ** A list
 ** sendond item

its furthermore content of cell 22, a new paragraph.
|-|| and this is the third row.
|}
</pre>
A '''simple table''' may be shown very simple, but a complex table is not readable simply,
but itsn't worse as using the html tags &lt;table>&lt;tr> and so on.
 
 */ 
@SuppressWarnings("unchecked")
public class WikistyleTextToSimpleXml
{
  final Report report;
  
  /** The end of the last line, starts with -1, the position before 0.*/
  int end = -1;
  
  /** The start of the actual line.*/
  int start = 0;
  
  /** A list if list items are present. Its an array for nested lists.*/
  XmlNode[] xmlNesting = new XmlNode[20];

  /** A list item is present.*/
  //XmlNode[] xmlNestingItem = new XmlNode[6];
  
  ListIterator iterBaseElement; XmlNode dstElement; String dstNamespace;
  
  /** The iterator for the List to add something*/
  XmlNode xmlChild = null;
  
  /** The index of xmlNesting if the conversion is inside a structure with end label (table). */
  int idxNesting = -1;
  
  /** The content of the class attributes for nesting levels. In the first level
   * it is equal to the sClass attribute.
   */
  String sClassNesting;
  
  /** A current < pre>-Block*/
  XmlNode xmlPre = null;
  
  /** treemap of elements for attributes */
  TreeMap elementsWithAttrib;
  
  /** Constructor only connect the class to a report system.
   * 
   */
  public WikistyleTextToSimpleXml()
  { this.report = null;
  }
  
  /** Constructor only connect the class to a report system.
   * 
   * @param report may be null.
   */
  public WikistyleTextToSimpleXml(Report report)
  { this.report = report;
  }
  
  /** Tests in the tree of xml elements all Elements wether they are appropriate &lt;p>-tags. 
   * A appropriate &lt;p>-Tag is a tag which contains the attribute "expandWikistyle".
   * <br>
   * If such &lt;ü>-Tag is found, its textual content is read. Child Elements are ignored. It means, no
   * xml-formatted &lt;b>- or adequate tags are considered. Its content is ignored. 
   * Only the the textual content directly held under this element is used.
   * The &lt;p>-XmlNode will be removed, and the created new Elements are placed into this position instead,
   * see Method {@link insertInIterator(String, ListIterator, List, String)}.  
   * The new Elements are &lt;p>-Elements with XHTML-formatting inside, &lt;ul>-Elements and so on, 
   * see the general description of this class. All attributes of the original &lt;p>-XmlNode are copied 
   * to the created &lt;p-Elements inside.
   * 
   * @param xmlTree The tree to convert.
   * @throws XmlException 
   */
  public void testXmlTreeAndConvert(XmlNode xmlTree) throws XmlException
  {
    List<XmlNode> listParaElements = xmlTree.listChildren();
    if(listParaElements != null)
    { ListIterator<XmlNode> iterElements = listParaElements.listIterator();
      int idx = 0;
      while(iterElements.hasNext())
      { XmlNode xmlTest = iterElements.next();
        String attrExpand;
        //if(xmlTest.getChildren().size()==0) //only if there are no xml formatting inside paragraph
        if(xmlTest.getName().equals("p") && (attrExpand = xmlTest.getAttribute("expandWikistyle"))!=null)
        { String sText = xmlTest.getText();
          if(report!=null)
          { int sMax = sText.length(); if(sMax > 30){ sMax = 30;}
            report.reportln(Report.fineInfo, 0, "ConverterWikiStyle:" + sText.substring(0, sMax));
          }
          xmlTest.removeAttribute("expandWikistyle");
          Map attrib = xmlTest.getAttributes();
          attrib = null; 
          String sClass = xmlTest.getAttribute("class");  //the style attribute
          //remove the founded element:
          iterElements.remove(); 
          //add new elements instead using the sText, all elements inherit the attrrib:
          insertInIterator(sText, iterElements, null, attrib, sClass);
        }
        else if( (xmlTest.getAttribute("expandWikistyle"))!=null)
        { String sText = xmlTest.getText();
          if(report!=null)
          { int sMax = sText.length(); if(sMax > 30){ sMax = 30;}
            report.reportln(Report.fineInfo, 0, "ConverterWikiStyle:" + sText.substring(0, sMax));
          }
          xmlTest.removeAttribute("expandWikistyle");
          Map attrib = xmlTest.getAttributes();
          attrib = null; 
          xmlTest.removeChildren();
          String sClass = xmlTest.getAttribute("class");  //the style attribute
          insertInIterator(sText, null, xmlTest, attrib, sClass);
                  
          
        }
        else
        { //call recursively to entry in tree bough
          testXmlTreeAndConvert(xmlTest);
        }
        idx +=1;
      }
    }     
  }
  
  
  
  /**Converts a text content in wikipedia-style to a tree of textual elements. The created new elements
   * are added as childs to a xml-XmlNode, from which the iterator is present. It is possible to add some elements other,
   * and between it some elements converted from a wikipedia style text. A sample of using is shown downside. 
   * In this sample the ListIterator iter referenced to an empty list first, but with the add method some elements are added
   * to the list, and with the paradigmas of jdom also to the element.
   * <pre>
   * XmlNode parent = new XmlNode("parent");
   * ListIterator iter = parent.getChildren().listIterator();
   * doSomethingAddingOtherElments(iter);
   * String sText = getAnyTextWithWikipediaFormattingStyle();
   * insertInIterator(sText, iter, null);
   * </pre>  
   * Another sample of using is implemented in the method {@link testXmlTreeAndConvert(XmlNode)} of this class,
   * xmlTree is the input argument of this method:
   * <pre>
   * List listParaElements = xmlTree.getChildren();
   * ListIterator iterElements = listParaElements.listIterator();
   * while(iterElements.hasNext())
   * { XmlNode xmlTest = (XmlNode)(iterElements.next());
   *   if(xmlTest.getName().equals("p") && xmlTest.getAttribute("expandWikistyle")!=null)
   *   { String sText = xmlTest.getText();
   *     List attrib = xmlTest.getAttributes();
   *     String sClass = xmlTest.getAttributeValue("class");  //the style attribute
   *     iterElements.remove(); 
   *     insertInIterator(sText, iterElements, attrib, sClass);
   *   }
   *   else
   *   { testXmlTreeAndConvert(xmlTest);
   *   }
   * }
   * </pre> 
   * In this sample the actual element is replaced (removed and the new Elements are inserted) 
   * if it is a &lt;p>-XmlNode with wikipedia-style-formatting. The Method testXmlTreeAndConvert()
   * is called recursively for all elements. The attributes from the original &lt;p>-XmlNode are copied to
   * the new generated Elements. 
   * 
   * @param sInput A String may be containing wikipedia style elements.
   * @param iter If not null, Iterator of Elements, getted via anyElement.getChildren().listIterator(). 
   *        The resulting elements are added at the current position of the iterator.
   * @param dstElement If not null, add new Elements as child into this given XmlNode.       
   * @param attributes List of attributes, type org.jdom.Attribute. All attributes are setted to
   *        all created &lt;p>-Elements.
   * @param sClass Value of the class attribute from the original element.
   * @throws XmlException 
   *        
   */
  @SuppressWarnings("unchecked")
  public void insertInIterator(String sInput, ListIterator iter, XmlNode dstElement, Map attributes, String sClass) throws XmlException
  { 
  	iterBaseElement = iter; 
  	this.dstElement = dstElement;

    sClassNesting = sClass; //may be setted from previous call.
  	
  	start = 0; end = -1;
  	idxNesting = -1;
  	xmlChild = initNesting();
  	elementsWithAttrib = null;
  	xmlPre = null;
  	
  	if(dstElement != null){ dstNamespace = dstElement.getNamespaceKey(); }
    else{ dstNamespace = null; }
    
    xmlChild = initNesting();  
    
    
    /** Searches in the given text paragraphs.*/
    while(start < (sInput.length()))
    { String sLine;
      char cFirst = sInput.charAt(start);
      if(cFirst == ' ' || cFirst == ',')
      { end = sInput.indexOf("\n", start);
        if(end <0){ end = sInput.length(); }   //last line is empty
        //while(++start == ' ' && start < end);  //skip over spaces.
        if(start < end)
        { sLine = sInput.substring(start, end);
          start = end+1;
          //pre Format
          xmlChild = initNesting(); 
          if(xmlPre == null)
          { xmlPre = dstElement.createNode("pre", dstNamespace); //new XmlNode("pre", dstNamespace);
            if(iter != null){ iter.add(xmlPre);}
            if(xmlChild != null){ xmlChild.addContent(xmlPre); }
            if(elementsWithAttrib != null)
            { TreeMap attribs = (TreeMap)elementsWithAttrib.get("pre");
              if(attribs != null)
              { Iterator iterAttr = attribs.keySet().iterator();
                while(iterAttr.hasNext())
                { String sAttrib = (String)iterAttr.next();
                  String value = (String)attribs.get(sAttrib);
                  xmlPre.setAttribute(sAttrib, value);
            } } }
          }
          else
          { //xmlPre.addContent("&x0a;");
            //xmlPre.addContent("\n");
          }
          xmlPre.addContent(sLine.substring(1)+"\n");
        }  
      }
      else
      { sLine = getLineSpecial(sInput); //reads more as one line or until special || and !!
        if(sLine.length()>0)
        { cFirst = sLine.charAt(0);
          //char c2 = sLine.length()>=2 ? sLine.charAt(1) : ' ';
          //char c3 = sLine.length()>=3 ? sLine.charAt(2) : ' ';
          //switch(cFirst)
          /* The first character of a line determines wether it may be a new simple paragraph or a special textblock
           * like a list, table, pre text. If it is a special textblock, {@link nestingLevel()} is called.
           * nestinglevel() creates a new XmlNode xmlChild, but it also creates the superior textblock elements
           * like &lt;ul>. ,,xmlChild,, is type of the container for paragraphs, at example &lt;li>. 
           * The followed text will produces a paragraph inside xmlChild.
           *  
           * class attribute: The content of class attribute for next paragraphs inside a special textblock
           * are built inside nestingLevel() derivated from the input content in sClass, added the textblock
           * label. It is stored in the class-wide- attribute sClassNesting.   
           * 
           */
          switch(cFirst)
          { case '*': case ';': case '#': case ':': case '>': case '{': case '|': case '!':
            { //any nested block
              sLine = nestingLevel(sLine, idxNesting+1, iter, dstElement, dstNamespace, sClass); //##1
              if(sLine.length()>0)
              { if(xmlChild.getName().equals("dt"))
  	            { convertLine(sLine, xmlChild, dstNamespace);
  	            }
  	            else
  	            { writeParagraphInElement(sLine, xmlChild, dstNamespace, attributes, sClassNesting == null ? null : sClassNesting + "_p");
  	            }
              }
            }break;
            case '@':
            { readAttributes(sLine);
            } break;
            default:
            { //a new line not beginning with a special char, it is a paragraph at basic level. 
              xmlChild = initNesting();
              xmlPre = null;
              writeParagraphInIter(sLine, iter, xmlChild, dstNamespace, attributes, sClass);
            }          //special char at start of paragraphs line:
          }//switch
        }
      }  
    }//while end
    { //clear all aggregations
	    idxNesting = -1;
	    initNesting();
	  	xmlChild = null;
	  	elementsWithAttrib = null;
	  	iterBaseElement = null;
	  	dstElement = null;
    	xmlPre = null;
    } 	

  }

  
  
 
  private void convertLine(String sInput, XmlNode xmlRet, String dstNamespace) throws XmlException
	{ //test the next appearances of some special chars:
	  final int kNothing = 0, 
	            kBold=1, 
	            kItalic=2, 
	            kBoldItalic=3, 
	            kHyperlink=4, 
	            kHyperlinkAbsolute=5, 
	            kCode=6,
	            //kPreserve=7,
              kImage=8,
              kInset=9
	            //kCitiation=8
	            ;
	
	  
	  int start = 0;
	  
	  /** The actual end for looking for somewhat.*/
	  
	  /** If it is set, a text after end is followed.*/
	  int startAfter = -1;
	  
	  if(sInput.startsWith(",,%%%,,code"))
	    stop();
	  
	  while(start < sInput.length())
	  { //Test the nearest appearance of a control chars, end is always limited: 
	    int kind = kNothing;
	    { int posCtrlChars = sInput.length();
		    int posCtrlChars1;
		
		    /**search the nearest special char sequence,
		     * test it one after another, use the nearest position of any of them.
		     * The end of a prosperous search is the limit for the next search.
		     * It is a :TRICKY: to save calculation time by limiting the search 
		     * from start unil actual end determined from search before
		     * and it is twice the algorithm: a nearest prosperous search gets a indexOf
		     * > 0 in this area of text.
		     */
		    if( (posCtrlChars1 = sInput.indexOf("'''''", start)) >=0)
		    { //search first the long variant of '''''
		    	kind = kBoldItalic; 
		      posCtrlChars = posCtrlChars1; 
		      startAfter = posCtrlChars + 5; 
		    }
		    if( (posCtrlChars1 = sInput.substring(start,posCtrlChars).indexOf("'''")) >=0)
		    { kind = kBold; 
		      posCtrlChars = start + posCtrlChars1; 
		      startAfter = posCtrlChars + 3; 
		    }
		    if( (posCtrlChars1 = sInput.substring(start,posCtrlChars).indexOf("''")) >=0)
		    { kind = kItalic; 
		      posCtrlChars = start + posCtrlChars1; 
		      startAfter = posCtrlChars + 2; 
		    }
		    if( (posCtrlChars1 = sInput.substring(start,posCtrlChars).indexOf(",,")) >=0)
		    { kind = kCode; 
		      posCtrlChars = start + posCtrlChars1; 
		      startAfter = posCtrlChars + 2; 
		    }
        if( (posCtrlChars1 = sInput.substring(start,posCtrlChars).indexOf("[[Inset:")) >=0)
        { kind = kInset; 
          posCtrlChars = start + posCtrlChars1; 
          startAfter = posCtrlChars + 8; 
        }
        if( (posCtrlChars1 = sInput.substring(start,posCtrlChars).indexOf("[[Image:")) >=0)
        { kind = kImage; 
          posCtrlChars = start + posCtrlChars1; 
          startAfter = posCtrlChars + 8; 
        }
        if( (posCtrlChars1 = sInput.substring(start,posCtrlChars).indexOf("[[!")) >=0)
        { kind = kHyperlinkAbsolute; 
          posCtrlChars = start + posCtrlChars1; 
          startAfter = posCtrlChars + 3; 
        }
		    if( (posCtrlChars1 = sInput.substring(start,posCtrlChars).indexOf("[[")) >=0)
		    { kind = kHyperlink; 
		      posCtrlChars = start + posCtrlChars1; 
		      startAfter = posCtrlChars + 2; 
		    }
	      
		    /**Copy the part before before the founded control chars: */
		    if(posCtrlChars > start)
		    { String sBefore = sInput.substring(start, posCtrlChars);
          String sAdd = SpecialCharStrings.resolveCircumScription(sBefore);
          xmlRet.addContent(sAdd);
		    }
	    }	    
	    /**Executes the concern of control chars: */
	    { int endCtrledPart;
	    	int startNextPart;  //::TRICKY:: The compiler tests wether a value is assigned in every case.
	      switch(kind)
		    { case kBoldItalic:
		      { endCtrledPart = sInput.indexOf("'''''", startAfter); 
		        if(endCtrledPart >= 0){ startNextPart = endCtrledPart +5; }
		        else { endCtrledPart = sInput.length(); startNextPart = endCtrledPart;}
		        if(endCtrledPart > startAfter)
		        { XmlNode xmlBold = xmlRet.addNewNode("b", dstNamespace);
		          XmlNode xmlNew = xmlBold.addNewNode("i", dstNamespace);
		          convertLine(sInput.substring(startAfter, endCtrledPart), xmlNew, dstNamespace);  
		        }
		      } break;
		      case kBold:
		      { endCtrledPart = sInput.indexOf("'''", startAfter); 
		        if(endCtrledPart >= 0){ startNextPart = endCtrledPart +3; }
		        else { endCtrledPart = sInput.length(); startNextPart = endCtrledPart;}
		        if(endCtrledPart > startAfter)
		        { XmlNode xmlNew = xmlRet.addNewNode("b", dstNamespace);
		          convertLine(sInput.substring(startAfter, endCtrledPart), xmlNew, dstNamespace);  
		        }
		      } break;
		      case kItalic:
		      { endCtrledPart = sInput.indexOf("''", startAfter); 
		        if(endCtrledPart >= 0){ startNextPart = endCtrledPart +2; }
		        else { endCtrledPart = sInput.length(); startNextPart = endCtrledPart;}
		        if(endCtrledPart > startAfter)
		        { XmlNode xmlNew = xmlRet.createNode("i", dstNamespace);
		          xmlRet.addContent(xmlNew);
		          convertLine(sInput.substring(startAfter, endCtrledPart), xmlNew, dstNamespace);  
		        }
		      } break;
		      case kCode:
		      { endCtrledPart = sInput.indexOf(",,", startAfter);
		        while(endCtrledPart >=3 && sInput.substring(endCtrledPart-3,endCtrledPart).equals("%%%"))
		        { //the end char sequence is invalide because %%% is found before:
		        	endCtrledPart = sInput.indexOf(",,", endCtrledPart +2);  //search after them
		        }
		        if(endCtrledPart >= 0){ startNextPart = endCtrledPart +2; }
		        else { endCtrledPart = sInput.length(); startNextPart = endCtrledPart;}
		        if(endCtrledPart > startAfter)
		        { XmlNode xmlNew = xmlRet.createNode("code", dstNamespace);
		          xmlRet.addContent(xmlNew);
		          convertLine(sInput.substring(startAfter, endCtrledPart), xmlNew, dstNamespace);  
		        }
		      } break;
		      case kHyperlink: case kHyperlinkAbsolute:
		      { int startText, endCtrledPartHref;
		        endCtrledPartHref = sInput.indexOf("|", startAfter); 
		        endCtrledPart = sInput.indexOf("]]", startAfter); 
		        if(endCtrledPart >= 0)
		        { startNextPart = endCtrledPart +2; }
		        else 
		        { endCtrledPart = sInput.length(); startNextPart = endCtrledPart;
		        }
		        if(endCtrledPartHref <0 || endCtrledPartHref > endCtrledPart)
		        { startText = startAfter;
		          endCtrledPartHref = endCtrledPart;
		        }
		        else
		        { startText = endCtrledPartHref+1;
		        }
		        if(endCtrledPart > startAfter)
		        { XmlNode xmlNew = xmlRet.createNode("a", dstNamespace);
		          xmlRet.addContent(xmlNew);
		          String sHref = sInput.substring(startAfter, endCtrledPartHref);
		          if(kind == kHyperlinkAbsolute)
		          { xmlNew.setAttribute("href", sHref);
		          }
		          else
		          { xmlNew.setAttribute("href", "#" + sHref);
		          }
		          convertLine(sInput.substring(startText, endCtrledPart), xmlNew, dstNamespace);  
		        }
		      } break;
          case kImage:
          { endCtrledPart = sInput.indexOf("]]", startAfter); 
            if(endCtrledPart >= 0)
            { startNextPart = endCtrledPart +2; }
            else 
            { endCtrledPart = sInput.length(); startNextPart = endCtrledPart;
            }
            String sImage = sInput.substring(startAfter, endCtrledPart);
            String[] sTokens = sImage.split("\\|");
            XmlNode xmlImage = xmlRet.createNode("img", dstNamespace);
            xmlRet.addContent(xmlImage);
            xmlImage.setAttribute("src", sTokens[0]);
            if(sTokens.length > 1)
            { //the last token is the title.
              xmlImage.setAttribute("title", sTokens[sTokens.length-1]);
            }
            for(int iToken = 1; iToken < sTokens.length-1; iToken++)
            { String sToken = sTokens[iToken];
              int pos;
              if(sToken.equals("left") || sToken.equals("right") 
                || sToken.equals("center")
                )
              { xmlImage.setAttribute("align", sToken); 
              }
              else if( (pos=sToken.indexOf("px"))>0)
              { int posX = sToken.indexOf('x');
                int height=-1, width=-1;
                if(posX >0 && posX < pos)
                { //"80x100px"
                  xmlImage.setAttribute("width", sToken.substring(0, posX));
                }
                else if(posX>=pos)
                { //"100px"
                  xmlImage.setAttribute("width", sToken.substring(0, pos));
                }
                else //posX==0
                { //"x100px", only height will be set.
                }
                if(posX >=0 && posX < pos)
                {  xmlImage.setAttribute("height", sToken.substring(posX+1, pos));
                }
              }
            }
          } break;
          case kInset:
          { endCtrledPart = sInput.indexOf("]]", startAfter); 
            if(endCtrledPart >= 0)
            { startNextPart = endCtrledPart +2; }
            else 
            { endCtrledPart = sInput.length(); startNextPart = endCtrledPart;
            }
            String sImage = sInput.substring(startAfter, endCtrledPart);
            String[] sTokens = sImage.split("\\|");
            XmlNode xmlInset = xmlRet.createNode("span", dstNamespace);
            xmlRet.addContent(xmlInset);
            xmlInset.setAttribute("class", "inset");
            xmlInset.setAttribute("title", sTokens[0]);
            if(sTokens.length > 1)
            { //the last token is the title.
              xmlInset.addContent(sTokens[sTokens.length-1]);
            }
          } break;
          case kNothing:
		      { //no control chars found, the text before is copied yet, its the end of line 
		      	startNextPart = sInput.length();
		      } break;
		      default:
		      { throw new RuntimeException("unexpected case on switch");
		      }
		    }  
		    start = startNextPart;  //continous at this position.
	    }
	  }
	}

  
  
  /**Gets a line 
   * 
   * @param sInput
   * @return The line
   */
  private String getLineSpecial(String sInput)
  {
    /** Accumulator for the lines content.*/
    String sLine = "";
    boolean continuationParagraphInNextline = true;
    boolean bSpecialEnd = false;
    while(!bSpecialEnd && continuationParagraphInNextline)
    {      
      end = sInput.indexOf("\n", start);
      int end2 = sInput.indexOf("!!", start);  //table: some th in 1 line
      int end3 = sInput.indexOf("||", start);  //table: some td in 1 line
      if(end < 0)
      { end = sInput.length();
        continuationParagraphInNextline = false;
      }
      if(end2 >= 0 && end2 < end){ end = end2; bSpecialEnd = true; }
      if(end3 >= 0 && end3 < end){ end = end3; bSpecialEnd = true; }
      { /*Searches the end of text in the actual line, considers white spaces on end.*/
        int lineEnd = end;
        while(lineEnd > 0 && " \t\r".indexOf(sInput.charAt(lineEnd-1)) >= 0){ lineEnd -=1; }
        if(sLine.length()>0){ sLine += " ";}  //1 space instead new line before append next text
        sLine += sInput.substring(start, lineEnd);  //the text exclusively appended white spaces.
      }  
      if(bSpecialEnd)
      { start = end +=1; //NOTE: bSpecialEnd: the next text info starts at 1 ! or |, not at both.
      }
      else 
      { start = end +1;
      }
      continuationParagraphInNextline
       =  start < sInput.length()                  //there is a next line
       && " ,>+*#;:@{!|\r\n".indexOf(sInput.charAt(start)) < 0  //it doesn't start with this chars.
       ;  
    }  
    return sLine;  	
  }
  
  
  
  
  
  /** process a nested element, may be a list item or div recursively in max 6 levels.
   * The sLine starts with any char of "*" or "#" or ";" or ":" or ">" and may contain more such chars after
   * or a character "+" after.<br>
   *  
   * If more characters "*" or "#" or ";" or ":" or ">" after are detect, the method are called recursively
   * to process the next level.<br>
   * 
   * If a next character "+" is detect, this is a continuation of the current nested level, from there
   * it isn't created a new element like < li>. Only if it is the first line
   * of such a element like < li>, the non existing level-element is created yet.
   * <br>
   * If no "+" or "*" or "#" or ";" or ":" or ">" is detected after the first char, a new level element like < li> will be created,
   * also if there is a actual item of this level.
   * <br>
   * The class field <code>xmlChild</code> is setted to the correct level-XmlNode.
   * 
   * @param sLine The line beginning with "*" or "#" or ";" or ":" or ">".
   *        On recursive call it is not the first char of the absolute line, but the second, third, ...
   *        adequate to the level.
   * @param level The level, the user should set always 0. Only incremented if recursively called here. 
   * @param iterBaseElement If not null, the base level to insert level 0.
   * @param dstElement If not null, the base level to insert level 0.
   * @return The string after the last "*" or a followed ":" or some followed spaces,
   *         that is the content of the paragraph in < li>. It may be beginn 
   *         with ":", that is a indentation inside the < li>-Block.
   * @throws XmlException 
   */
  private String nestingLevel(String sLine, int level, ListIterator iterBaseElement, XmlNode dstElement, String dstNamespace, String sClass) throws XmlException
  { char cFirst = sLine.charAt(0);
    char cNext = sLine.length()>=2 ? sLine.charAt(1) : ' ';
    int nrofPreChars = 1;
    { //new list item
      String sTagNesting = null, sTagListItem = null;
      switch(cFirst)
      { case '*': sTagNesting = "ul"; sTagListItem = "li"; break;
        case '#': sTagNesting = "ol"; sTagListItem = "li"; break;
        case ';': sTagNesting = "dl"; sTagListItem = "dt"; break;
        case ':': sTagNesting = "dl"; sTagListItem = "dd"; break;
        case '>': sTagNesting = "div"; sTagListItem = null; break;
        case '{': 
        { if(cNext=='|')
        	{ idxNesting = level;
        	  nrofPreChars = 2;
        	  newChild("table");
        	}
          else{ sTagListItem=null; }
        } break;
        case '|': case '!': 
        { if(cNext=='-')
	      	{ sTagListItem = null; 
        	  nrofPreChars = 2;
        	  if(getTagNesting(idxNesting -1).equals("tr"))
        	  { //td was before
        	  	idxNesting -=1;
        	  }
        	  else if(getTagNesting(idxNesting).equals("table"))
        	  { //td was before
        	  	idxNesting +=1;
        	  }
        	  newChild("tr");
	      	}
          else if(cNext=='}')
	      	{ sTagListItem = null; 
        	  nrofPreChars = 2;
        	  boolean bSearchTableLevel = true;
	      	  for(int idx = idxNesting; bSearchTableLevel && idx >= (idxNesting-2); idx--)
        	  { if(getTagNesting(idx).equals("table"))
		      	  { idxNesting = idx -1;  //before table, close it
		      	    bSearchTableLevel = false;
		      	    xmlChild = initNesting();
		      	  }
        	  }
        	  level = idxNesting;  //table end.
	      	}
	        else
          { 
        	  if(getTagNesting(idxNesting).equals("table"))
        	  { idxNesting +=1;
        	  	newChild("tr");
        	  	idxNesting +=1;
        	  }
        	  else if(getTagNesting(idxNesting).equals("tr"))
        	  { idxNesting +=1;
        	  }
            if(idxNesting >=0)
            {
              newChild(cFirst == '!' ? "th" : "td"); 
            }
          }
        } break;
        default:  sTagListItem = "xdiv"; break;
      }  
      if(sTagNesting != null)
      { boolean bNewListType = (xmlNesting[level] == null)  //no outer Tag, it is the first list item 
		                         || !(xmlNesting[level].getName().equals(sTagNesting)); 
      	if( bNewListType)  //another tag type
	      { //the first element in this list, create the < ul>-XmlNode.
	        XmlNode xmlContainer = dstElement.createNode(sTagNesting, dstNamespace); 
	        //add it to the previous level, first to iterBaseElement.
	        addToParentList(level-1, xmlContainer);
	        if(sClass != null)
	        { sClassNesting = sClass + "_" + sTagNesting;
	        	xmlContainer.setAttribute("class", sClassNesting);
	        }
	        xmlNesting[level] = xmlContainer;
	      }
		    if( sTagListItem != null)
		    { level += 1; //need 2 level. 
			    if( bNewListType     //first item of a list or such
			    	|| (xmlNesting[level] == null)	
			      || ("*#;:+>".indexOf(cNext) <0)  //new item, because new text at this level, no further nesting or additional text to last item (+).
			      )  
		      { //second ary element
		        xmlChild = xmlNesting[level] = dstElement.createNode(sTagListItem, dstNamespace);
		        addToParentList(level-1, xmlNesting[level]);
		        if(sClass != null)
		        { sClassNesting = sClass + "_" + sTagListItem;
	        	  xmlChild.setAttribute("class", sClassNesting);
		        }
		        if(elementsWithAttrib != null)
		        { TreeMap attribs = (TreeMap)elementsWithAttrib.get(sTagListItem);
		          if(attribs != null)
		          { Iterator iter = attribs.keySet().iterator();
		            while(iter.hasNext())
		            { String sAttrib = (String)iter.next();
		              String value = (String)attribs.get(sAttrib);
		              xmlChild.setAttribute(sAttrib, value);
		              if(sAttrib.equals("class"))
		              { sClassNesting = value;
		              }
		        } } }
		      }
		      xmlChild = xmlNesting[level];
		    }  
      }
    }
    
    if("*#;:".indexOf(cNext)>=0 && level < 6)
    { //a next list level, nested list
      return nestingLevel(sLine.substring(1), level+1, iterBaseElement, dstElement, dstNamespace, sClassNesting);
    }
    else
    { //the level of list nesting is reached. clear deeper list levels.
      for(int ii = level+1; ii < xmlNesting.length; ii++)
      { xmlNesting[ii] = null; 
        xmlPre = null;
      }

      if(cNext == '+'){ nrofPreChars +=1; }
      while(sLine.length() > nrofPreChars && sLine.charAt(nrofPreChars) == ' ')
      { nrofPreChars +=1;
      }
      return sLine.substring(nrofPreChars);
    }
  }  

  
  /**Gets the name of the element or "" to test wether it is*/
  private String getTagNesting(int idx)
  { String sTag;
  	if(idx < 0) sTag = "";
  	else
  	{ XmlNode xml = xmlNesting[idx];
  	  if(xml != null) sTag = xml.getName();
  	  else sTag = "";
  	}
    return sTag;  	
  }
  
  
  private void newChild(String sTag) throws XmlException
  { xmlChild = dstElement.createNode(sTag, dstNamespace);
    xmlNesting[idxNesting] = xmlChild;
    addToParentList(idxNesting -1, xmlChild);
	  initNesting();
  }
  
  
  /**Adds either into the iterator or in the topLevel element if the level is <0
   * or add to the xmlNesting[level]
   * @param level The level, -1 means add to topLevel
   * @param toAdd XmlNode to add, in most case it is xmlNesting[level+1]
   * @param iterBaseElement top levels iterator if dst XmlNode = null
   * @param dstElement topLevel element.
   * @param dstNamespace
   * @throws XmlException 
   */
  private void addToParentList(int level, XmlNode toAdd) throws XmlException
  { if(level < 0)
    { //to base level, new list
      if(iterBaseElement != null){ iterBaseElement.add(toAdd); }
      if(dstElement != null){ dstElement.addContent(toAdd); }
    }
    else
    { //it should be exist a < li>, create it if text start with more as one **
      xmlNesting[level].addContent(toAdd);
    }
  }
  
  /** Reads attributes for followed elements.
   * example: @dt.class="value" @dd.class="value" <br>
   * reads an attribute name class applicable to dt and another attribute applicable to dd.
   * @param sLine
   */
  void readAttributes(String sLine)
  { int pos = 0;
    while(pos >=0 && sLine.length() > pos && sLine.charAt(pos) == '@')
    { int pos2 = sLine.indexOf('.', pos);
      if(pos2 > 0)
      { String sElement = sLine.substring(pos + 1, pos2);
        int pos3 = sLine.indexOf('=', pos2);
        if(pos3 > 0)
        { String sAttribute = sLine.substring(pos2 + 1, pos3);
          int pos4 = sLine.indexOf('\"', pos3+2);
          int pos5 = sLine.indexOf('@', pos3+1);
          String sValue = (pos4 > 0 && (pos5<0 || pos4 < pos5) )
                        ? sLine.substring(pos3 + 2, pos4)
                        : null;
          if(elementsWithAttrib == null)
          { elementsWithAttrib = new TreeMap();
          }
          TreeMap attrib = (TreeMap)elementsWithAttrib.get(sElement);
          if(attrib == null)
          { attrib = new TreeMap();
            elementsWithAttrib.put(sElement, attrib);
          }
          if(sValue == null){ attrib.remove(sAttribute); }
          else { attrib.put(sAttribute, sValue); }
        }  
      }
      pos = sLine.indexOf('@', pos+1);
    }
  }
  
  
  
  /** Sets all variables for assignment to nested levels after idxNesting to null,
   * no nesting is existing yet.
   * @xmlTopLevel the toplevel element for output if the decision is to toplevel.
   * @return the actual element. NOTE: It may be always set to xmlChild, but the management of xmlChild is no job of this method.
   */
  private XmlNode initNesting()
  { for(int ii = idxNesting+1; ii < xmlNesting.length; ii++)
    { xmlNesting[ii] = null;
    }
    return idxNesting < 0 ? dstElement : xmlNesting[idxNesting];
  }
  
  
  private void writeParagraphInIter(String sLine, ListIterator iterParent, XmlNode dstElement, String dstNamespace, Map attributes, String sClass) throws XmlException  
  { XmlNode xmlParagraph = dstElement.createNode("p", dstNamespace);
    if(iterParent != null){ iterParent.add(xmlParagraph);}
    if(dstElement != null){ dstElement.addContent(xmlParagraph); }
    if(attributes != null)
    { 
      Iterator<Map.Entry<String, String>> iterAttrib = attributes.entrySet().iterator();
      while(iterAttrib.hasNext())
      { Map.Entry<String, String> entry = iterAttrib.next();
        String name = entry.getKey();
        String value = entry.getValue();
        if(!name.equals("expandWikistyle"))
        { xmlParagraph.setAttribute(name, value);
        }
      }      
    }
    if(sClass != null)
    { xmlParagraph.setAttribute("class", sClass);
    }
    convertLine(sLine, xmlParagraph, dstNamespace);
  }
  
  
  
  private void writeParagraphInElement(String sLine, XmlNode xmlParent, String dstNamespace, Map attributes, String sClass) throws XmlException  
  { XmlNode xmlParagraph = xmlParent.addNewNode("p", dstNamespace);
    if(attributes != null)
    { 
      Iterator<Map.Entry<String, String>> iterAttrib = attributes.entrySet().iterator();
      while(iterAttrib.hasNext())
      { Map.Entry<String, String> entry = iterAttrib.next();
        String name = entry.getKey();
        String value = entry.getValue();
        if(!name.equals("expandWikistyle"))
        { xmlParagraph.setAttribute(name, value);
        }
      }      
    }
    if(sClass != null)
    { xmlParagraph.setAttribute("class", sClass);
    }
    convertLine(sLine, xmlParagraph, dstNamespace);
  }
  
  /**Only for debug. */
  private void stop()
  { //only for debug
  }

}
