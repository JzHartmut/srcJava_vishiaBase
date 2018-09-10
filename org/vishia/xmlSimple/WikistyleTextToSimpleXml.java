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
 * @author Hartmut Schorrig, www.vishia.org
 * @version 2009-07-02  (year-month-day)
 * list of changes:
 * 2012-09-09 Hartmut: new: now empty lines in pre-Blocks are regarded.
 * 2012-09-09 Hartmut: new: now @p.class="format" works.
 * 2009-12-14 Hartmut: new: [[&name]] builds an anchor. 
 * 2009-07-02 Hartmut: new: A ~ is replaced by the value of attribute expandLabelOwn, if it is found.
 *                     This feature is used in conclusion with XmiDocu.xslp to shorten the name of the own class. 
 * 2006-05-00 Hartmut: creation
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
import org.vishia.util.Debugutil;
import org.vishia.util.SpecialCharStrings;

import org.vishia.mainCmd.Report;



/** This class converts a string or the text content of a XML element
 * inside a XML tree according to part of the rules used in Wikipedia-online encyclopaedia.
 * <br>
 * Therewith, if an text field in pure ASCII is converted to XML inside some tools, 
 * it is able to write a formatted text in the wikipedia-style and get the XML-formatted text as result. 
 * <br><br>
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
|-||cell 21 may be have a longer text
||thats why cell 22 is written in a new line, but cell 22 contains
 ** A list
 ** sendond item

its furthermore content of cell 22, a new paragraph.
|-|| and this is the third row.
|}
</pre>
A '''simple table''' may be shown very simple, but a complex table is not readable simply,
but itsn't worse as using the html tags &lt;table>&lt;tr> and so on.
 * @author Hartmut Schorrig
 * @since 2006
 * last changes:
 * 2007: JDOM was used, but now this version uses the {@link org.vishia.xmlSimple.SimpleXmlOutputter}
 * 2008..2010 some changes
 * 2010-05-13 Tabs are recognize instead spaces too, but there are converted to 2 spaces. Method {@link #replaceTabs(String)}.
 */ 
public class WikistyleTextToSimpleXml
{
  
    /**Version history and license.
   * <ul>
   * <li>2018-01-07 warning free
   * <li>2018-01-07 bugfix the '+' from "*+" and a second '*' was not removed in the text for *+list item and **deeper list.  
   * <li>2017-08-31 bugfix table short form was not recognized.
   * <li>2017-05-06 bugfix  
   *   bug: if an chapter ends with a list item, the next usage of this instance was damaged.
   *   reason: {@link #deepnessList} was not reseted, was 1 for next usage.
   *   fix: cleanup improved. 
   * <li>2016-11-06 Hartmut: new Now writes paragraphs in 2 or more  columns of a table if "..............." is found on start of line.
   *   The end is "^^^^^^^^^^^^^^^^^^^^^^^^" 
   * <li>2016-11-06 Hartmut: chg Gardening of the algorithm. Line ending with \r\n or only \r are supported too, see {@link #endline(String)}  
   * <li>2015-09-29 Hartmut: chg An attribute like <code>@p.class="style"</code> can contained at start of a line with more content now.
   *   Note that a line in this meaning can consist of more as one line with one carriage return inside in the source. It was a problem
   *   writing an attribute with only one carriage return following with a text. Now you can write:
   *   <pre>
   *     @p.class="style"
   *     The line following after a single carriage return.
   *     
   *     @p.class="style" line immediately following
   *   </pre>  
   * <li> 2012-09-09 Hartmut: new: now empty lines in pre-Blocks are regarded.
   * <li> 2012-09-09 Hartmut: new: now @p.class="format" works.
   * <li> 2009-12-14 Hartmut: new: [[&name]] builds an anchor. 
   * <li> 2009-07-02 Hartmut: new: A ~ is replaced by the value of attribute expandLabelOwn, if it is found.
   *   This feature is used in conclusion with XmiDocu.xslp to shorten the name of the own class. 
   * <li> 2006-05-00 Hartmut: creation
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
   * If you are indent to use this sources without publishing its usage, you can get
   * a second license subscribing a special contract with the author. 
   * 
   * @author Hartmut Schorrig = hartmut.schorrig@vishia.de
   * 
   * 
   */
  public final static String version = "2017-05-06";

  final Report report;
  
  /** The end of the last line, starts with -1, the position before 0.*/
  int end = -1;
  
  /**The start of the next line. */
  int startNext = 0;
  
  char cnl1 = '\n', cnl2 = '\r';
  
  /** The start of the actual line.*/
  int start = 0;
  
  /** A list if list items are present. Its an array for nested lists.*/
  XmlNode[] xmlNesting = new XmlNode[20];

  XmlNode currElem;

  /** A list item is present.*/
  //XmlNode[] xmlNestingItem = new XmlNode[6];
  
  ListIterator<XmlNode> iterBaseElement; XmlNode dstElement; String dstNamespace;
  
  /** The index of xmlNesting if the conversion is inside a structure with end label (table). */
  int ixChild = -1;
  
  /**If ** was written before, it is 2: List in list. */
  int deepnessList;
  
  char kindList;
  
  /** The content of the class attributes for nesting levels. In the first level
   * it is equal to the sClass attribute.
   */
  String sClassNesting;
  
  /** A current < pre>-Block*/
  XmlNode xmlPre = null;
  
  int preEmptylines = 0;
  
  /** treemap of elements for attributes */
  TreeMap<String, TreeMap<String, String>> elementsWithAttrib;
  
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
  
  /** Tests all Elements of the XML tree whether their content should be expand.
   * The children of the current level are tested with an iterator, for all child elements
   * this routine is called recursively.
   * If an element contains the attribute "expandWikistyle", it should be expand.
   * The plain text of such an element is taken as input to expand. 
   * Child Elements are ignored. It means, no xml-formatted &lt;b>- or adequate tags are considered. 
   * Only the the textual content directly held under this element is used.
   * <br>
   * If it is a &lt;p>-element, it will be removed, and the created new Elements are placed into this position instead,
   * If it is another element, its content is removed and the content is replaced by the expanding result
   * <br>
   * The new Elements are &lt;p>-Elements with XHTML-formatting inside, &lt;ul>-Elements and so on, 
   * see the general description of this class. All attributes of the original &lt;p>-XmlNode are copied 
   * to the created &lt;p-Elements inside.
   * <br>
   * For preparation the expanding result see 
   * {@link #insertAndConvertText(String, ListIterator, XmlNode, Map, String)}.  
   * @param xmlTree The tree to convert.
   * @throws XmlException 
   */
  public void testXmlTreeAndConvert(XmlNode xmlTree) 
  throws XmlException
  {
    List<XmlNode> listParaElements = xmlTree.listChildren();
    if(listParaElements != null)
    { ListIterator<XmlNode> iterElements = listParaElements.listIterator();
      //int idx = 0;
      while(iterElements.hasNext())
      { XmlNode xmlTest = iterElements.next();
        @SuppressWarnings("unused")
        String attrExpand;
        //if(xmlTest.getChildren().size()==0) //only if there are no xml formatting inside paragraph
        if(xmlTest.getName().equals("p") && (attrExpand = xmlTest.getAttribute("expandWikistyle"))!=null)
        { String sText = xmlTest.text();
          String sLabelOwn1 = xmlTest.getAttribute("expandLabelOwn");
          if(report!=null)
          { int sMax = sText.length(); if(sMax > 30){ sMax = 30;}
            report.reportln(Report.fineInfo, 0, "ConverterWikiStyle:" + sText.substring(0, sMax));
          }
          xmlTest.removeAttribute("expandWikistyle");
          Map<String, String> attrib = xmlTest.getAttributes();
          attrib = null; 
          String sClass = xmlTest.getAttribute("class");  //the style attribute
          //remove the founded element:
          iterElements.remove(); 
          //add new elements instead using the sText, all elements inherit the attrrib:
          insertAndConvertText(sText, iterElements, null, attrib, sClass, sLabelOwn1);
        }
        else if( (xmlTest.getAttribute("expandWikistyle"))!=null)
        { String sText = xmlTest.text();
          String sLabelOwn1 = xmlTest.getAttribute("expandLabelOwn");
          if(report!=null)
          { int sMax = sText.length(); if(sMax > 30){ sMax = 30;}
            report.reportln(Report.fineInfo, 0, "ConverterWikiStyle:" + sText.substring(0, sMax));
          }
          xmlTest.removeAttribute("expandWikistyle");
          Map<String, String> attrib = xmlTest.getAttributes();
          attrib = null; 
          xmlTest.removeChildren();
          String sClass = xmlTest.getAttribute("class");  //the style attribute
          insertAndConvertText(sText, null, xmlTest, attrib, sClass, sLabelOwn1);
                  
          
        }
        else
        { //call recursively to entry in tree bough
          testXmlTreeAndConvert(xmlTest);
        }
        //idx +=1;
      }
    }     
  }
  
  
  
  
  
  public void setWikistyleFormat(String sInput, XmlNode dstElement, Map<String, String> attributes, String sClass) 
  throws XmlException
  {
    cnl1 = '\n'; cnl2 = '\r';
    insertAndConvertText(sInput, null, dstElement, attributes, sClass, null);
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
   * @param iter If not null, it is an ListIterator of XML-children. 
   *        The resulting elements are added at the current position of the iterator.
   * @param dstElement If not null, add new Elements as children into this given XmlNode.       
   * @param attributes List of attributes, type org.jdom.Attribute. All attributes are setted to
   *        all created &lt;p>-Elements.
   * @param sClass Value of the class attribute from the original element.
   * @param sLabelOwn transported in nested level, set from an Attribute expandLabelOwn.
   *        It replaces a <code>~</code>-character in a label.
   * @throws XmlException 
   *        
   */
  private void insertAndConvertText(String sInput, ListIterator<XmlNode> iter, XmlNode dstElement
      , Map<String, String> attributes, String sClass, String sLabelOwn
      ) throws XmlException
  { 
    cleanup(); 
    iterBaseElement = iter; 
    this.dstElement = dstElement;
    sClassNesting = sClass; //may be setted from previous call.
    
    start = 0; end = -1;
    if(dstElement !=null) {
      this.ixChild = 0;
      currElem = xmlNesting[ixChild] = dstElement;
    } else {
      assert(ixChild == -1);
    }
    elementsWithAttrib = null;
    xmlPre = null;
    
    if(dstElement != null){ dstNamespace = dstElement.getNamespaceKey(); }
    else{ dstNamespace = null; }
    
    
    
    /** Searches in the given text paragraphs.*/
    while(start < (sInput.length()))
    { //String sLine;
      char cFirst = sInput.charAt(start);
      if(xmlPre !=null && (cFirst == '\n' || cFirst == '\r')) { //empty line inside pre-block
        //empty line inside a pre block. count it and add it to the pre block if lines are following
        preEmptylines +=1;
        endline(sInput);
        start = startNext;
      }
      else if(cFirst == ' ' || cFirst == '\t' || cFirst == ',') { 
        //<pre>, the line starts with a space it is translated to a <pre>..</pre>*/
        endline(sInput);
        if(start < end)
        { String sLineTest = sInput.substring(start, end);
          String sLine = replaceTabs(sLineTest);
          start = startNext;
          //pre Format
          if(xmlPre == null) {
            this.xmlPre = newChild("pre");
            if(iter != null){ iter.add(xmlPre);}
            if(elementsWithAttrib != null)
            { TreeMap<String, String> attribs = (TreeMap<String, String>)elementsWithAttrib.get("pre");
              if(attribs != null)
              { Iterator<String> iterAttr = attribs.keySet().iterator();
                while(iterAttr.hasNext())
                { String sAttrib = (String)iterAttr.next();
                  String value = (String)attribs.get(sAttrib);
                  xmlPre.setAttribute(sAttrib, value);
            } } }
          }
          while(preEmptylines > 0){
            //xmlPre.addContent("&x0a;");
            xmlPre.addContent("\n");
            preEmptylines-=1;
          }
          xmlPre.addContent(sLine.substring(1)+"\n");
        } else { //empty line.
          start = startNext;
        }  
      } //cFirst == ' ' || cFirst == ','
      else {
        //not a pre block, not empty lines. 
        String sLineTest = getLineSpecial(sInput); //reads more as one line or until special || and !!
        //if(sLineTest.contains("Man soll also statt dem in C++ speziell"))
        //  Debugutil.stop();
        if(this.xmlPre !=null) {
          closeChild(); xmlPre = null; //end of a pre text.  
          preEmptylines = 0;
        }
        if(sLineTest.length()>0)
        { String sLine = replaceTabs(sLineTest);
          while(sLine !=null) {
            cFirst = sLine.charAt(0);
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
            { case '*': case ';': case '#': case ':': case '>': case '{': case '.': case '/': case '^': case '|': case '!':
              { //any nested block
                if(cFirst != kindList) {  ////
                  //end of list or another list
                  while(deepnessList >0 && ixChild >=2) { closeChild(); closeChild(); deepnessList -=1; } //close the list.
                  kindList = '\0';
                }
                sLine = nestingLevel(cFirst, sLine, iter, dstElement, dstNamespace, sClass); //##1
                if(sLine.length()>0)
                { if(currElem.getName().equals("dt"))
                  { convertLine(sLine, currElem, dstNamespace, sLabelOwn);
                  }
                  else
                  { writeParagraphInElement(sLine, currElem, dstNamespace, attributes, sClassNesting == null ? null : sClassNesting, sLabelOwn); // + "_p");
                  }
                }
                sLine = null;
              }break;
              case '@':
              { sLine = readAttributes(sLine);
              } break;
              default:
              { //a new line not beginning with a special char, it is a paragraph at basic level. 
                while(deepnessList >0) {  //not a list item, then close the list.
                  if(ixChild >=2) {       //it should be! assert(ixChild >=2)
                    closeChild(); closeChild(); deepnessList -=1;  //close the list.
                  }
                  else break;
                }
                kindList = '\0';
                if(sLine.contains("Man soll also statt dem in C++ speziell"))
                  Debugutil.stop();
                writeParagraphInIter(sLine, iter, currElem, dstNamespace, attributes, sClass, sLabelOwn);
                sLine = null;
              }          //special char at start of paragraphs line:
            }//switch
          }//while(sLine !=null
        }
      }  
    }//while end
    cleanup();  //cleanup for next usage:

  }

  
  
  
  private void cleanup()
  {
    while(ixChild >=0) {
      xmlNesting[ixChild--] = null;
    }
    deepnessList = 0;  //it is not 0 if the last line is a list item.
    currElem = null;
    elementsWithAttrib = null;
    iterBaseElement = null;
    this.dstElement = null;
    xmlPre = null;
    
  }
  
  
  
  /**It sets end and startNext.
   * @param sLine
   */
  private void endline(String sLine) {
  
    end = sLine.indexOf(cnl1, start);
    if(cnl2 != '\0'){
      int end2 = sLine.indexOf(cnl2, start);
      if(end2 == -1) { cnl2 = '\0';  startNext = end +1; }  //there is not a combination of \r \n
      else if(end2 == end -1) { startNext = end+1; end = end2; }   // \r\n
      else if(end2 == end +1) { startNext = end2+1; }   // \n\r
      else if (end == -1 && end2 >=0) { cnl1 = cnl2; cnl2 = '\0'; end = end2; startNext = end2 +1; }  //found \r not \n
    } else if(end <0) {
      end = sLine.length();
      startNext = end;
    } else {
      startNext = end+1; //only 1 separation character.
    }
    while(end > start && " \t\f".indexOf(sLine.charAt(end-1)) >=0) { end -=1; }   //remove trailing white spaces. 
  }
  
  
  
  
  
  
  
  
  /**Replace tabs in the line with 2 spaces.
   * @param src The line
   * @return src, if it doesn't contain tabs, elsewhere new String with replaced tabs.
   */
  private final String replaceTabs(String src)
  { int posStart = 0;
    int posTab = src.indexOf('\t');
    if(posTab >=0){
      final StringBuilder u = new StringBuilder(2 * src.length()); //if all chars are tabs, double size.
      //u.append(src);
      while(posTab >=0){
        u.append(src.substring(posStart, posTab))
         .append("  ");  //replace with 2 spaces.
        posStart = posTab +1;
        posTab = src.indexOf('\t', posStart);
      }
      u.append(src.substring(posStart)); //rest of line.
      return u.toString();
    } else {
      /**The line doesn't contain any tab: */
      return src;
    }
  }
  
  
 
  /**converts a line
   * @param sInput
   * @param xmlRet
   * @param dstNamespace
   * @param sLabelOwn transported in nested level, set from an Attribute expandLabelOwn.
   *        It replaces a <code>~</code>-character in a label.
   * @throws XmlException
   * 
   */
  private void convertLine(String sInput, XmlNode xmlRet, String dstNamespace, String sLabelOwn) throws XmlException
  { //test the next appearances of some special chars:
    final int kNothing = 0, 
              kBold=1, 
              kItalic=2, 
              kBoldItalic=3, 
              kHyperlink=4, 
              kHyperlinkAbsolute=5,
              kAnchor=6,
              kImage=8,
              kInset=9,
              kCode=10
              //kPreserve=7,
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
        if( (posCtrlChars1 = sInput.substring(start,posCtrlChars).indexOf("[[")) >=0)
        { posCtrlChars = start + posCtrlChars1; 
          if( sInput.substring(posCtrlChars).startsWith("[[Inset:"))
          { kind = kInset; 
            startAfter = posCtrlChars + 8; 
          }
          else if( sInput.substring(posCtrlChars).startsWith("[[Image:"))
          { kind = kImage; 
            startAfter = posCtrlChars + 8; 
          }
          else if( sInput.substring(posCtrlChars).startsWith("[[http:"))
          { kind = kHyperlinkAbsolute; 
            startAfter = posCtrlChars + 2; 
          }
          else if( sInput.substring(posCtrlChars).startsWith("[[!"))
          { kind = kHyperlinkAbsolute; 
            startAfter = posCtrlChars + 3; 
          }
          else if( sInput.substring(posCtrlChars).startsWith("[[&"))
          { kind = kAnchor; 
            startAfter = posCtrlChars + 3; 
          }
          else
          { kind = kHyperlink; 
            startAfter = posCtrlChars + 2; 
          }
        }
        /**Copy the part before before the founded control chars: */
        if(posCtrlChars > start)
        { String sBefore = sInput.substring(start, posCtrlChars);
          CharSequence sAdd = SpecialCharStrings.resolveCircumScription(sBefore);
          xmlRet.addContent(sAdd.toString());
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
            { XmlNode xmlBold = xmlRet.addNewNode("stroke", dstNamespace);
              XmlNode xmlNew = xmlBold.addNewNode("em", dstNamespace);
              convertLine(sInput.substring(startAfter, endCtrledPart), xmlNew, dstNamespace, sLabelOwn);  
            }
          } break;
          case kBold:
          { endCtrledPart = sInput.indexOf("'''", startAfter); 
            if(endCtrledPart >= 0){ startNextPart = endCtrledPart +3; }
            else { endCtrledPart = sInput.length(); startNextPart = endCtrledPart;}
            if(endCtrledPart > startAfter)
            { XmlNode xmlNew = xmlRet.addNewNode("stroke", dstNamespace);
              convertLine(sInput.substring(startAfter, endCtrledPart), xmlNew, dstNamespace, sLabelOwn);  
            }
          } break;
          case kItalic:
          { endCtrledPart = sInput.indexOf("''", startAfter); 
            if(endCtrledPart >= 0){ startNextPart = endCtrledPart +2; }
            else { endCtrledPart = sInput.length(); startNextPart = endCtrledPart;}
            if(endCtrledPart > startAfter)
            { XmlNode xmlNew = xmlRet.createNode("em", dstNamespace);
              xmlRet.addContent(xmlNew);
              convertLine(sInput.substring(startAfter, endCtrledPart), xmlNew, dstNamespace, sLabelOwn);  
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
              convertLine(sInput.substring(startAfter, endCtrledPart), xmlNew, dstNamespace, sLabelOwn);  
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
              int posSubst;
              /**replaces a ~ with the attribute value of expandLabelOwn: */
              if(sLabelOwn != null && (posSubst=sHref.indexOf('~')) >=0)
              { sHref = sHref.substring(0, posSubst) + sLabelOwn + sHref.substring(posSubst+1);
              }
              if(kind == kHyperlinkAbsolute)
              { xmlNew.setAttribute("href", sHref);
              }
              else
              { xmlNew.setAttribute("href", "#" + sHref);
              }
              convertLine(sInput.substring(startText, endCtrledPart), xmlNew, dstNamespace, sLabelOwn);  
            }
          } break;
          case kAnchor: 
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
            { XmlNode xmlNew = xmlRet.createNode("span", dstNamespace);
              xmlRet.addContent(xmlNew);
              String sHref = sInput.substring(startAfter, endCtrledPartHref);
              int posSubst;
              /**replaces a ~ with the attribute value of expandLabelOwn: */
              if(sLabelOwn != null && (posSubst=sHref.indexOf('~')) >=0)
              { sHref = sHref.substring(0, posSubst) + sLabelOwn + sHref.substring(posSubst+1);
              }
              xmlNew.setAttribute("class", "anchor");
              xmlNew.setAttribute("id", sHref);
              convertLine(sInput.substring(startText, endCtrledPart), xmlNew, dstNamespace, sLabelOwn);  
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
                @SuppressWarnings("unused")
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
          { startNextPart = setInset(sInput, startAfter, xmlRet, "span");
          } break;
          case kNothing:
          { //no control chars found, the text before is copied yet, its the end of line 
            startNextPart = sInput.length();
          } break;
          default:
          { throw new RuntimeException("unexpected case on switch");
          }
      }//switch  
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
    while(continuationParagraphInNextline)
    { endline(sInput);     
      
      int end2 = sInput.indexOf("!!", start);  //table: some th in 1 line
      int end3 = sInput.indexOf("||", start);  //table: some td in 1 line
      if(end2 >= 0 && end2 < end) { 
        end = end2; bSpecialEnd = true; startNext = end +1;  //+2  //A !! is a separation for table col instead ! at begin of a line. 
      }                                                      //start the next line with on ! (not 2)
      if(end3 >= 0 && end3 < end) { 
        end = end3; bSpecialEnd = true; startNext = end +1; 
      }
      if(end > start) {
        /*Searches the end of text in the actual line, considers white spaces on end.*/
        int lineEnd = end;
        while(lineEnd > start && " \t\r".indexOf(sInput.charAt(lineEnd-1)) >= 0){ lineEnd -=1; }  //without spaces on end.
        sLine += sInput.substring(start, lineEnd);  //the text exclusively appended white spaces.
      }
      start = startNext;  
      continuationParagraphInNextline
       =  !bSpecialEnd
       && sLine.length() >0 
       && start < sInput.length()                  //there is a next line
       && " \t,>+*#;:@{!|./^\r\n".indexOf(sInput.charAt(start)) < 0  //it doesn't start with this chars.
       ;
      if(continuationParagraphInNextline) {
        sLine += ' ';  //instead linefeed insert one space.
      }
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
   * @param iterBaseElement_a If not null, the base level to insert level 0.
   * @param dstElement_a If not null, the base level to insert level 0.
   * @return The string after the last "*" or a followed ":" or some followed spaces,
   *         that is the content of the paragraph in < li>. It may be beginn 
   *         with ":", that is a indentation inside the < li>-Block.
   * @throws XmlException 
   */
  private String nestingLevel(char cFirst, String sLine, ListIterator<XmlNode> iterBaseElement_a, XmlNode dstElement_a, String dstNamespace_a, String sClass) throws XmlException
  { char cNext = sLine.length()>=2 ? sLine.charAt(1) : ' ';
    int nrofPreChars = 1;
    { //new list item
      String sTagNesting = null, sTagListItem = null; 
      //String sTagAttribSetting = null;  //TODO: use it.
      switch(cFirst)
      { case '*': 
          sTagNesting = "ul"; sTagListItem = "li"; kindList = cFirst; break;
        case '#': sTagNesting = "ol"; sTagListItem = "li"; kindList = cFirst; break;
        case ';': sTagNesting = "dl"; sTagListItem = "dt"; kindList = cFirst; break;
        case ':': sTagNesting = "dl"; sTagListItem = "dd"; kindList = cFirst; break;
        case '>': 
        { sTagNesting = null; sTagListItem = null;
          nrofPreChars = checkInsertNesting_div(sLine, sClass);
        } break;
        case '.': {
          if(cNext == '.') {
            nrofPreChars = 2;
            while(sLine.length() > nrofPreChars && sLine.charAt(nrofPreChars) == '.') { nrofPreChars +=1; }
            if(checkCurrelem("td")){
              newSibling("td");
            } else {
              XmlNode table = newChild("table");
              table.setAttribute("class", "textColumns");
              newChild("tr");
              newChild("td");
            }
          }
        } break;
        case '/': {
          if(cNext == '.') {
            if(checkCurrelem("td")){
              closeChild();  //td
              closeChild("tr");  //tr
              closeChild("table");  //table
            }
          } else {
            Debugutil.stop();
          }
        } break;
        case '^': {
          if(cNext == '^') {
            while(sLine.length() > nrofPreChars && sLine.charAt(nrofPreChars) == '^') { nrofPreChars +=1; }
            if(checkCurrelem("td")){
              closeChild();  //td
              closeChild("tr");  //tr
              closeChild("table");  //table
            } else {
              Debugutil.stop();
            }
          }
        } break;
        case '{': 
        { if(cNext=='|')
          { nrofPreChars = 2;
            newChild("table");
            newChild("tr");
          }
          else{ sTagListItem=null; }
        } break;
        case '|': case '!': 
        { if(cNext=='-')
          { sTagListItem = null; 
            nrofPreChars = 2;
            if(checkCurrelem("td") || checkCurrelem("th")) {
              closeChild();
            }
            newSibling("tr");
          }
          else if(cNext=='}')
          { sTagListItem = null; 
            nrofPreChars = 2;
            if(checkCurrelem("td") || checkCurrelem("th")) {
              closeChild();
            }
            if(checkCurrelem("tr")) {
              closeChild();
            }
            if(checkCurrelem("table")) {
              closeChild();
            }
          }
          else
          { 
            if(checkCurrelem("td") || checkCurrelem("th")) {
              closeChild();
            }
            newChild(cFirst == '!' ? "th" : "td"); 
          }
        } break;
        default:  sTagListItem = "xdiv"; break;
      }  
      if(sTagNesting != null) {
        int deepnessList1 = 1;
        XmlNode newChild = null;;
        while(sLine.length() > deepnessList1 && sLine.charAt(deepnessList1) == cFirst) { deepnessList1 +=1; nrofPreChars +=1; }
        while(deepnessList > deepnessList1){ //deepnessList is the current one. close it if deeper
          closeChild();
          closeChild();
          deepnessList -=1;
        }
        if(deepnessList == deepnessList1) {
          if(sLine.length() > deepnessList1 && sLine.charAt(deepnessList1) == '+') { //new paragraph in this list
            //+ after List chars, it is not a new list item but an paragraph in this list item.
            nrofPreChars +=1;  //skip over the '+'
          } else {
            newChild = newSibling(sTagListItem);
          }
        }
        else { 
          while(deepnessList < deepnessList1) {
            newChild(sTagNesting);
            newChild = newChild(sTagListItem);
            deepnessList +=1;
        } }
        if(newChild !=null) {
          if(sClass != null)
          { sClassNesting = sClass; // + "_" + sTagListItem;
            newChild.setAttribute("class", sClassNesting);
          }
          if(elementsWithAttrib != null)
          { TreeMap<String, String> attribs = (TreeMap<String, String>)elementsWithAttrib.get(sTagListItem);
            if(attribs != null)
            { Iterator<String> iter = attribs.keySet().iterator();
              while(iter.hasNext())
              { String sAttrib = (String)iter.next();
                String value = (String)attribs.get(sAttrib);
                newChild.setAttribute(sAttrib, value);
                if(sAttrib.equals("class"))
                { sClassNesting = value;
                }
          } } }
        }  
      }
    }
    
    while(sLine.length() > nrofPreChars && sLine.charAt(nrofPreChars) == ' ')
    { nrofPreChars +=1;
    }
    return sLine.substring(nrofPreChars);
  }  

  
  
  /**Checks whether the given tag is an new tag or it is the same like the last line.
   * If it is a new tag a new nesting level is created.
   * At example:
   * <pre>
   * *listitem
   * *listitem
   * </pre>
   * Because the type of list is the same, no new list is inserted.
   * @param sTagNesting The tag, if list, than the whole list, not the list item.
   * @param level The new level of nesting.
   * @param sClass
   * @throws XmlException
   */
  XmlNode checkInsertNestingTag(String sTagNesting, String sClass) 
  throws XmlException
  {
    boolean bNewListType = (ixChild < 0)  //no outer Tag, it is the first list item 
      || !(xmlNesting[ixChild].getName().equals(sTagNesting)); 
    if( bNewListType)  //another tag type
    { //the first element in this list, create the < ul>-XmlNode.
      
      XmlNode xmlContainer = newChild(sTagNesting);
      if(sClass != null)
      { sClassNesting = sClass; // + "_" + sTagNesting;
      xmlContainer.setAttribute("class", sClassNesting);
      }
    }
    return xmlNesting[ixChild];
  }
  
  
  /**Processing a paragraph with beginning >
   * @param sLine
   * @param level
   * @param sClass
   * @return
   * @throws XmlException 
   */
  int checkInsertNesting_div(String sLine, String sClass) 
  throws XmlException
  {
    XmlNode xmlDiv = checkInsertNestingTag("div", sClass);
    int posContent = 1;
    int lengthLine = sLine.length();
    if(sLine.startsWith(">@")){
      //sTagAttribSetting = readAttributeSetting(sLine, posContent);
      //posContent += sTagAttribSetting.length();
    }
    if(lengthLine > posContent +2){
      if(sLine.substring(posContent, lengthLine).startsWith("[[inset:")){
        //The content of the devision is determined by another generated document part.
        posContent = setInset(sLine, posContent+8, xmlDiv, "p");
      }
    }
    return posContent;
  }  
  
  
  
  private XmlNode newChild(String sTag) throws XmlException
  { XmlNode xmlChild = dstElement.createNode(sTag, dstNamespace);
    xmlNesting[++ixChild] = xmlChild;
    addToParentList(ixChild -1, xmlChild);
    return currElem = xmlChild;
  }
  
  
  private XmlNode newSibling(String sTag) throws XmlException
  { XmlNode xmlChild = dstElement.createNode(sTag, dstNamespace);
    xmlNesting[ixChild] = xmlChild;
    addToParentList(ixChild -1, xmlChild);
    return currElem = xmlChild;
  }
  
  
  private XmlNode closeChild()
  { xmlNesting[ixChild] = null;
    ixChild -=1;
    return currElem = (ixChild >=0 ? xmlNesting[ixChild] : null);
  }
  
  private XmlNode closeChild(String sTag)
  { if(xmlNesting[ixChild].getName().equals(sTag)) {
      xmlNesting[ixChild] = null;
      ixChild -=1;
    } else {
      Debugutil.stop();
    }
    return currElem = (ixChild >=0 ? xmlNesting[ixChild] : null);
  }
  
  
  private boolean checkCurrelem(String tag) {
    return ixChild >=0 && xmlNesting[ixChild].getName().equals(tag); 
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
  
  
  
  int setInset(String sInput, int startAfter, XmlNode xmlRet, String sTag) 
  throws XmlException
  { int endCtrledPart, startNextPart;
    endCtrledPart = sInput.indexOf("]]", startAfter); 
    if(endCtrledPart >= 0)
    { startNextPart = endCtrledPart +2; }
    else 
    { endCtrledPart = sInput.length(); startNextPart = endCtrledPart;
    }
    String sImage = sInput.substring(startAfter, endCtrledPart);
    String[] sTokens = sImage.split("\\|");
    XmlNode xmlInset = xmlRet.createNode(sTag, dstNamespace);
    xmlRet.addContent(xmlInset);
    xmlInset.setAttribute("class", "inset");
    xmlInset.setAttribute("title", sTokens[0]);
    if(sTokens.length > 1)
    { //the last token is the title.
      xmlInset.addContent(sTokens[sTokens.length-1]);
    }
    return startNextPart;
  }
  
  
  /** Reads an simple attribute setting.
   * example: @dt.class="value" @class=value<br>
   * @param sLine The line
   * @param pos should be the position of the @.
   * @return The attribute setting. It is postprocessed with 
   *         White spaces are not admissible.
   */
  String readAttributeSetting(String sLine, int pos)
  { return null;
  }
  
  /**Gets the name of attribute from a setting returned by {@link #readAttributeSetting(String, int)}.
   * example: @dt.class="value" @class=value<br>
   * @param sLine The Setting
   * @return The attribute name. It is the identifier after @
   */
  String getAttributeName(String sLine)
  { return null;
  }
  
  
  /**Gets the value of attribute from a setting returned by {@link #readAttributeSetting(String, int)}.
   * example: @dt.class="value" @class=value<br>
   * @return The attribute value. It is the part of String after the = maybe in "".
   *         If the value isn't set in "", the value is built from the identifier after =.
   *         White spaces are not admissible.
   */
  String getAttributeValue(String sAttribSetting)
  { return null;
  }
  
  /** Reads the attribute for one elements.
   * example: @dt.class="value"<br>
   * Note: More as one attributes are read in the outer loop of line content.
   * reads an attribute name class applicable to dt and another attribute applicable to dd.
   * @param sLine The line starting with '@'
   * @return the line after the attribute after spaces.
   */
  String readAttributes(String sLine)
  { int pos = 0;
    final String sElement;
    int pos2 = sLine.indexOf('.', pos);
    if(pos2 > 0) { 
      sElement = sLine.substring(pos + 1, pos2);
    } else {
      sElement = "p";  //default. TODO use for this type of paragraph! Or for all (extra list)
      pos2 = pos;
    }
    int pos3 = sLine.indexOf('=', pos2);
    if(pos3 > 0)
    { String sAttribute = sLine.substring(pos2 + 1, pos3).trim();
      int pos4 = sLine.indexOf('\"', pos3);
      if(pos4 <0){ 
        pos4 = pos3-1; //" not found.
      }
      int pos5 = sLine.indexOf('\"', pos4+1);
      if(pos5 < 0){
        pos5 = sLine.length(); //till end. 
      }
      final String sValue = sLine.substring(pos4+1, pos5);  //between "..."
      /*
      int pos6 = sLine.indexOf('@', pos3+1);
      String sValue = (pos4 > 0 && (pos5<0 || pos4 < pos5) )
                    ? sLine.substring(pos3 + 2, pos4)
                    : null;
      */
      if(elementsWithAttrib == null)
      { elementsWithAttrib = new TreeMap<String, TreeMap<String, String>>();
      }
      TreeMap<String, String> attrib = (TreeMap<String, String>)elementsWithAttrib.get(sElement);
      if(attrib == null)
      { attrib = new TreeMap<String, String>();
        elementsWithAttrib.put(sElement, attrib);
      }
      if(sValue == null){ attrib.remove(sAttribute); }
      else { attrib.put(sAttribute, sValue); }
      while(++pos5 < sLine.length() && sLine.charAt(pos5) == ' ');  //skip over space
      pos = pos5;
    } 
    else {
      //= not found. ignore it.
      pos = sLine.length(); //ignore till end of line.
    }
    return pos < sLine.length() ? sLine.substring(pos) : null;
  }
  
  
  
  
  
  private void writeParagraphInIter(String sLine, ListIterator<XmlNode> iterParent, XmlNode dstElement
  , String dstNamespace, Map<String, String> attributes, String sClass, String sLabelOwn
  ) throws XmlException  
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
    if(elementsWithAttrib !=null)
    { TreeMap<String, String> attribs = (TreeMap<String, String>)elementsWithAttrib.get("p");
      if(attribs != null)
      { Iterator<String> iterAttr = attribs.keySet().iterator();
        while(iterAttr.hasNext())
        { String sAttrib = (String)iterAttr.next();
          String value = (String)attribs.get(sAttrib);
          xmlParagraph.setAttribute(sAttrib, value);
    } } }
    convertLine(sLine, xmlParagraph, dstNamespace, sLabelOwn);
  }
  
  
  
  private void writeParagraphInElement(String sLine, XmlNode xmlParent, String dstNamespace
  , Map<String, String> attributes, String sClass, String sLabelOwn
  ) throws XmlException  
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
    convertLine(sLine, xmlParagraph, dstNamespace, sLabelOwn);
  }
  
  /**Only for debug. */
  private void stop()
  { //only for debug
  }

}
