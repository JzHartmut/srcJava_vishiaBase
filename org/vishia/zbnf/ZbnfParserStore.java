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
 * 2008-03-28 JcHartmut: The ParserStore is not cleared, only the reference is assigned new.
 *                       So outside the ParserStore can be used from an older parsing.
 * 2006-12-15 JcHartmut: regular expressions should be handled after white spaces trimming, error correction.
 * 2006-06-00 JcHartmut: a lot of simple problems in developemnt.
 * 2006-05-00 JcHartmut: creation
 *
 ****************************************************************************/
package org.vishia.zbnf; 

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.vishia.util.CheckVs;
import org.vishia.util.Debugutil;
import org.vishia.util.SortedTreeNode;
import org.vishia.util.StringFormatter;
import org.vishia.util.StringPart;
import org.vishia.xmlSimple.WikistyleTextToSimpleXml;
import org.vishia.xmlSimple.XmlException;
import org.vishia.xmlSimple.XmlNode;
import org.vishia.xmlSimple.XmlNodeSimple;


/** This class stores an syntax tested item.
 * <table border=1><tr><th>Syntax</th><th>SyntaxPrescript.</th><th>ParserStore.</th><th>add...</th></tr>
 * <tr><td>Terminal Symbols</td><td>String         </td><td>kTerminalSymbol</td><td>add??</td></tr>
 * <tr><td>[&lt;?Semantic> </td><td>         &nbsp;</td><td>&lt;kMaxAlternate</td><td>addOption</td></tr>
 * <tr><td>&lt;syntax?semantic></td><td>     &nbsp;</td><td>kComponent</td><td>add??</td></tr>
 * <tr><td>&lt;?semantic>  </td><td>         &nbsp;</td><td>kOnlySemantic</td><td>add??</td></tr>
 * <tr><td>[...]           </td><td>         &nbsp;</td><td>kSingleOption</td><td>add??</td></tr>
 * <tr><td>{<b>...</b>     </td><td>         &nbsp;</td><td>kRepetition</td><td>add??</td></tr>
 * <tr><td>{..?<b>...</b>} </td><td>         &nbsp;</td><td>kRepetitionRepeat</td><td>add??</td></tr>
 * <tr><td>&lt;#?semantic> </td><td>         &nbsp;</td><td>kIntegerNumber</td><td>add??</td></tr>
 * <tr><td>&lt;#-?semantic></td><td>         &nbsp;</td><td>kIntegerNumber</td><td>add??</td></tr>
 * <tr><td>&lt;#x?semantic></td><td>         &nbsp;</td><td>kIntegerNumber</td><td>add??</td></tr>
 * <tr><td>&lt;#f?semantic></td><td>         &nbsp;</td><td>kFloatNumber</td><td>add??</td></tr>
 * <tr><td>&lt;$?semantic> </td><td>         &nbsp;</td><td>kIdentifier</td><td>add??</td></tr>
 * <tr><td>&lt;*endchars?semantic></td><td>  &nbsp;</td><td>kString</td><td>add??</td></tr>
 * <tr><td>&lt;!regex?semantic></td><td>     &nbsp;</td><td>kString</td><td>add??</td></tr>
 * <tr><td>&lt;""?semantic></td><td>         &nbsp;</td><td>kString</td><td>add??</td></tr>
 * <tr><td>&lt;''?semantic></td><td>         &nbsp;</td><td>kString</td><td>add??</td></tr>
 * <tr><td>&lt;*""endchars?semantic></td><td>         &nbsp;</td><td>kString</td><td>add??</td></tr>
 * </table>
 * */
//@SuppressWarnings("synthetic-access") 
class ZbnfParserStore
{
  /**Version, history and license.
   * <ul>
   * <li>2022-04-28 Hartmut {@link #addIdentifier(String, ZbnfSyntaxPrescript, String, ZbnfParseResultItem, int, int, String)}
   *  and {@link #writeContentAsList(Appendable)}: enhanced to store the source of numeric values.
   * <li>2022-02-08 Hartmut
   *   <ul><li>{@link ParseResultItemImplement#elementSyntax} renamed from "parentSyntaxElement". It is not the parent. 
   *   <li>componentSyntax removed, because it is referenced in the elementSyntax (improved in {@link ZbnfSyntaxPrescript}
   *   <li>{@link #add(String, ZbnfSyntaxPrescript, ZbnfSyntaxPrescript, CharSequence, int, long, long, int, int, String, ZbnfParseResultItem)}
   *     and other add: Change of {@link ParseResultItemImplement#offsetAfterEnd} in the parent's items is removed. 
   *     This was an old concept, no more necessary, and confusing. 
   *   <li>in {@link #setAlternativeAndOffsetToEnd(int, int)}: Here important change, see description.
   *   </ul>
   * <li>2022-02-08 Hartmut new: {@link ParseResultItemImplement#componentSyntax} now references immediately from the result
   *   to the component for syntax. This gives more information. The data are not too much 
   *   because there are lesser Syntax definitions as result items.
   * <li>% renaming {@link ParseResultItemImplement#parentSyntaxElement} from syntaxElement to prevent confusion. 
   *   The meaning of that element was originally, before 2019, that of {@link ParseResultItemImplement#componentSyntax}
   *   but with lesser usage. It was a inconsequently change.
   * <li>% {@link ParseResultItemImplement#getSuperItemType()} uses the change in {@link ZbnfSyntaxPrescript#sSuperItemType}
   * <li>% {@link #writeContentAsList(Appendable)} as output to check (usual data debug).     
   * <li>2019-05-22 Hartmut chg: {@link ParseResultItemImplement#getText()} improved while testing ZBNF/testAllConecpts (from 2009).<br>
   *   {@link BuilderTreeNodeXml#createXmlNode(XmlNode, ParseResultItemImplement)}: On repetition item an XmlNode for <code>{&lt;?semantic>...</code>
   *   should be created anywhere, because elsewhere only one node exists. 
   * <li>2017-03-25 Hartmut chg: The syntax item for a parse result is stored in the {@link ParseResultItemImplement#syntaxItem()}.
   * <li>2014-06-17 Hartmut chg: new {@link BuilderTreeNodeXml} with attributes {@link BuilderTreeNodeXml#bXmlSrcline}
   *   and bXmlSrctext controls whether srcline="xx" and srctext="text" will be written to a XML output. 
   * <li>2014-05-23 Hartmut chg: use {@link StringPart#getLineAndColumn(int[])} instead getLineCt() and {@link StringPart#getCurrentColumn()}
   *   because it is faster. 
   * <li>2014-05-22 Hartmut new: Save srcFile in {@link ZbnfParserStore.ParseResultItemImplement#sFile},
   *   for information in written results, especially with {@link ZbnfJavaOutput}. 
   * <li>2014-04-21 Hartmut new: {@link #createXmlNode(XmlNode, ParseResultItemImplement)}: writes src-line, src-col
   *   and start of src in the xml element. 
   * <li>2013-09-12 Hartmut chg: {@link #buildTreeNodeRepresentationXml(XmlNode, ParseResultItemImplement, boolean)} now
   *   returns and works with the interface reference {@link XmlNode} instead the implementation instance reference 
   *   {@link XmlNodeSimple}. The implementation is the same. All references are adapted.
   * <li>2013-09-11 Hartmut new {@link PrepareXmlNode}, {@link ConvertWikiStyle}, 
   *   {@link #buildTreeNodeRepresentationXml(XmlNode, ParseResultItemImplement, boolean)}: All capabilities
   *   which were located in {@link ZbnfXmlOutput} are implemented here. The class {@link ZbnfXmlOutput} is not necessary
   *   any more because the intrinsic format of data stored as parse result is XML now.
   * <li>2013-09-12 Hartmut chg 
   * <li>2012-11-02 Hartmut new {@link ParseResultItemImplement#getText()} to get a textual projection of the content. used for XML presentation.
   * <li>2012-11-02 Hartmut: The ParseResultItem has gotten an element {@link ParseResultItemImplement#treeNodeXml}.
   *   It refers to an tree-like result store, whereby the {@link XmlNodeSimple} is used as node.
   *   Therewith the conversion to XML is obviously. As well too, the access to treed data able to use for direct
   *   text conversion using {@link org.vishia.zbatch.ZbatchExecuter}.
   *   A new method {@link #buildTreeNodeRepresentationXml(XmlNodeSimple, ParseResultItemImplement, boolean)} is offered to use.    
   * <li>2008-03-28 JcHartmut: The ParserStore is not cleared, only the reference is assigned new.
   *   So outside the ParserStore can be used from an older parsing.
   * <li>2006-12-15 JcHartmut: regular expressions should be handled after white spaces trimming, error correction.
   * <li>2006-06-00 JcHartmut: fixed a lot of simple problems in development.
   * <li>2006-05-00 JcHartmut: creation
   * </ul>
   * <br><br>
   * <b>Copyright/Copyleft</b>:
   * For this source the LGPL Lesser General Public License,
   * published by the Free Software Foundation is valid.
   * It means:
   * <ol>
   * <li> You can use this source without any restriction for any desired purpose.
   * <li> You can redistribute copies of this source to everybody.
   * <li> Every user of this source, also the user of redistribute copies
   *    with or without payment, must accept this license for further using.
   * <li> But the LPGL ist not appropriate for a whole software product,
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
   * @author Hartmut Schorrig = hartmut.schorrig@vishia.de, www.vishia.org
   * 
   */
  public static final String sVersion = "2022-04-28";

  
  /** Constant to detect the entry describes a terminate symbol. -32767*/
  final static int kTerminalSymbol = -0x7fff;

  /** Constant to detect the entry describes a component assigned to a non-teminal syntax symbol. -32766*/
  final static int kComponent = -0x7ffe;

  /** Constant to detect the entry describes a single option. -32764*/
  final static int kOnlySemantic = -0x7ffc;

  /** Constant to detect the entry describes a only semantiv entry. -32763*/
  final static int kOption = -0x7ffb;

  /** Constant to detect the entry describes a repetition forward. -32761*/
  //private final static int kRepetitionForward = -0x7ff9;

  /** Constant to detect the entry describes a repetition repeat. -32760*/
  //private final static int kRepetitionRepeat = -0x7ff8;

  /** Constant to detect an integer number. -32759*/
  final static int kIntegerNumber = -0x7ff7;

  /** Constant to detect an integer number. -32758*/
  final static int kFloatNumber = -0x7ff6;

  /** Constant to detect an identifier. -32757*/
  final static int kIdentifier = -0x7ff5;

  /** Constant to detect an identifier. -32756*/
  final static int kString = -0x7ff4;

  /** A number between 1 and this constant detects, it is a repetition.*/
  //private final static int kMaxAlternate = -0x7fe0;

  /** A number between this constant  an -1 detects, it is a repetition.*/
  final static int kMaxRepetition = 0x7fe0;


  /** One item to store in the list.
   *  It is a static inner class because it has a nearly coherence 
   *  to its outer class ParserStore.
   * */
  static class ParseResultItemImplement implements ZbnfParseResultItem
  {
    /** The explicit reference to the store to which the item is member of.
     *  The store is used to skip to next items by evaluation, see 
     *  method next() or nextSkipIntoComponent(). 
     *  <br>
     *  A temporary ParserStore is used to store a result outside a component 
     *  to add to the desired component later.
     *  In this case this reference is changed, {@link #ParserStore.add(ZbnfParserStore)}.
     *  Because it should be to changeable, a non static class ParseResultItemImplement
     *  with its implicit reference to the outer class ParserStore
     *  is a failure, in this case the implicit reference to the outer class
     *  can't be changed. 
     * */
    ZbnfParserStore store;
    
    /**The item which is the parent (contains a offsetAfterEnd with includes this position). */
    ParseResultItemImplement parent;
    
    /** The action to invoke.*/
    final String sSemantic;
    
    /**Reference to the syntax which has determined this element, used for semantic naming etc. */
    final ZbnfSyntaxPrescript elementSyntax;
    
    
    /**This is the reference to the components syntax is a component is used. Else null. */
//    final ZbnfSyntaxPrescript componentSyntax;
    
    /**Replacement for a non existing parentSyntaxElement instead null-reference. */
    final static ZbnfSyntaxPrescript null_syntaxElement = new ZbnfSyntaxPrescript(null, ZbnfSyntaxPrescript.EType.kNotDefined, 0);
    
    /** The text from input at the position*/
    String sInput;


    /** Kind of entry. If kind is >0 and < kMaxRepetition, it is the count of the repetition.
     * If kind is <0 and > -kMaxRepetition, it is the count of the repetition.
     * If the kind is one of the k___ constants of the outer class, it is such an entry.
     * Someone else is undefined.
    */
    int kind;

    /** Nr of the founded alternative. It is -1 if there is no alternative.
     * 0 means an empty option, alternatives count from 1.*/
    int nrofAlternative = -1;

    /** The string matching to the parsed */
    String parsedString;

    long parsedIntegerNumber;

    double parsedFloatNumber;

    /** The index of the element itself inside the outer->items - arraylist.*/
    int idxOwn;

    /** Set to true if the item is added from a additional list to the main list.*/
    boolean isAdded = false;
    
    /** The offset to the next item behind the items of this complex morphem.
     * =1 if it is no syntax component or "complex morphem"
     * The correct value is set with call of {@link ZbnfParserStore#setAlternativeAndOffsetToEnd(int, int)}.
     * This is done on end of each nested parsing for the appropriate component. 
     * Recursively then all values are correct on finishing the parent components. 
     */
    int offsetAfterEnd = 1;

    /** The position from-to of the associated input*/
    long start, end;
    /** The line and column and the position in the source nr for debugging*/
    int srcLine, srcColumn;
    long srcPos;

    /**The file or adequate resource from which this result comes from. */
    String sFile;
    
    /**The syntax identifications which has produced the stored result.
     * With this information a re-using of result can be do if the same syntax is detect 
     * in another path of the syntax graph, it spares calculation time. 
     */
    String syntaxIdent;
    
    /**  */
    SortedTreeNode<ZbnfParseResultItem> treeNodeRepresentation = null;
    
    
    //XmlNodeSimple<ZbnfParseResultItem> treeNodeXml = null;
    XmlNode treeNodeXml = null;
    
    
    ParseResultItemImplement(ZbnfParserStore store, String sSemantic, ZbnfParseResultItem parent
        , String syntax, ZbnfSyntaxPrescript elementSyntax, ZbnfSyntaxPrescript componentSyntax) { 
      this.store = store;
      if(sSemantic.equals("content"))
        Debugutil.stop();
      if(elementSyntax !=null && elementSyntax.sDefinitionIdent !=null && elementSyntax.sDefinitionIdent.equals("real_literal"))
        Debugutil.stop();
      if(elementSyntax !=null && elementSyntax.sDefinitionIdent !=null && elementSyntax.sDefinitionIdent.equals("real_type_name"))
        Debugutil.stop();
      int posValue = sSemantic.indexOf('='); 
      if(posValue>0){  //set a value with the given semantic in form "semantic=value"
        String value = sSemantic.substring(posValue+1);
        this.sSemantic = sSemantic.substring(0, posValue);
        this.parsedString = value;
        this.kind = kString;
      } else {
        this.sSemantic = sSemantic;
      }
      this.parent = (ParseResultItemImplement)parent;
      this.syntaxIdent = syntax;
      this.elementSyntax = elementSyntax;
//      this.componentSyntax = componentSyntax;
      if(this.sSemantic.equals("alternateText"))
        Debugutil.stop();
    }


    /**Gets the semantic of the item.
     *
     */
    @Override public String getSemantic()
    { return (this.sSemantic != null) ? this.sSemantic : "Null-Semantic";
    }

    @Override public String getParsedText()
    { return this.sInput;
    }

    @Override public double getParsedFloat()
    { return this.parsedFloatNumber;
    }

    @Override public long getParsedInteger()
    { return this.parsedIntegerNumber;
    }

    @Override public String getParsedString()
    { return this.parsedString;
    }

    @Override public int getInputLine()
    { return this.srcLine;
    }
    
    @Override public int getInputColumn()
    { return this.srcColumn;
    }
    
    @Override public String getInputFile(){ return this.sFile; }

    
    /**For example also {<?*semantic> is a component, though it has not a componentSyntax.
     * The componentSyntax is the following syntax straigtforward. 
     */
    @Override public boolean isComponent()
    { //return this.elementSyntax !=null &&  this.elementSyntax.componentSyntax !=null;
      return (this.offsetAfterEnd > 1) || this.kind == kComponent;
    }

    
    @Override public ZbnfSyntaxPrescript getComponentSyntax ( ) {
      if(this.elementSyntax ==null) return null;
      else return this.elementSyntax.componentSyntax;  //may be null
    }
    
    
    @Override public boolean isInteger()
    { return this.kind == kIntegerNumber;
    }

    @Override public boolean isFloat()
    { return this.kind == kFloatNumber;
    }

    @Override public boolean isString()
    { return this.kind == kString;
    }

    @Override public boolean isTerminalSymbol()
    { return this.kind == kTerminalSymbol;
    }

    //public boolean isSingleOption()
    //{ return kind == kSingleOption;
    //}

    @Override public boolean isIdentifier()
    { return this.kind == kIdentifier;
    }

    @Override public boolean isOnlySemantic()
    { return this.kind == kOnlySemantic;
    }

    @Override public int isRepeat()
    { return (this.kind < 0 && this.kind > -kMaxRepetition ? -this.kind : -1);
    }

    @Override public int isRepetition()
    { return (this.kind > 0 && this.kind < kMaxRepetition ? this.kind : -1);
    }

    public int xxxisAlternative()
    { return -1;
      //return (kind < 0 && kind > kMaxAlternate ? -kind : -1);
    }

    public String getKindOf()
    { String sRet;
      switch(this.kind)
      { case kComponent: sRet = "complex"; break;
        case kTerminalSymbol: sRet = "constant"; break;
        case kFloatNumber: sRet = "float"; break;
        case kIntegerNumber: sRet = "integer"; break;
        case kOnlySemantic: sRet = "semantic"; break;
        //case kRepetitionRepeat: sRet = "repeat"; break;
        //case kAlternative: sRet = "choice"; break;
        default:
        { if(this.kind >=0) sRet = "repetition nr "+ this.kind;
          else              sRet = "choose nr " + this.kind;
        }
      }
      return sRet;
    }

    @Override public String getDescription()
    { String sSemantic = getSemantic();
      if(sSemantic.equals("doubleNumber"))
        stop();
      String sRet = " [" + this.idxOwn;
      if(this.offsetAfterEnd > 1)
      { sRet += ".." + (this.idxOwn + this.offsetAfterEnd -1);
      }

      sRet += "]";
      
      sRet += "<...?" + sSemantic + ">";

      if(sSemantic.equals("operator"))
        Debugutil.stop();
      
      sRet += " ";
      int nrofAlternative = getNrofAlternative();
      if(nrofAlternative>=0 && nrofAlternative!=1)
      { sRet += "alternative:" + getNrofAlternative();
      }

      if(isInteger())        { sRet += "int:" + getParsedInteger(); }
      else if(isFloat())     { sRet += "float:" + getParsedFloat(); }
      else if(isIdentifier()){ sRet += "identifier:" + getParsedString(); }
      else if(isString())    { sRet += "string:\"" + getParsedString() + "\""; }

      if(isRepetition()>=0)
      { sRet += "{" + isRepetition() + "}";
      }
      else if(isRepeat()>=0)
      { sRet += "{?" + isRepeat() + "}";
      }

      sRet += " syntax=" + this.syntaxIdent;
      
      sRet += " input=" + this.start + ".." + this.end + "(" + this.srcLine + ", " + this.srcColumn + ")";
      
      String sParsedText = getParsedText();
      if(sParsedText != null)
      { sRet += " read: \"" + sParsedText + "\"";
      }
      if(this.treeNodeXml !=null){ sRet += " xmlNode=" + this.treeNodeXml; }
      return sRet;
    }

    
    
    /**Returns a text information proper to use for XML if it is a leaf node.
     * Returns null if it is a component.
     */
    String getText(){
      String ret = null;
      if(this.sSemantic.equals("line"))
        Debugutil.stop();
      if(isComponent()){ 
        ret = this.parsedString;  //it is null if only the component's item is stored.
        //it is not null if [<?semantic> ....] was written.
      }  
      else{  //an item
        switch(this.kind){
          case kTerminalSymbol: ret = this.parsedString; break;
          case kIntegerNumber: ret = Long.toString(this.parsedIntegerNumber); break;
          case kFloatNumber: ret = Double.toString(this.parsedFloatNumber); break;
          case kIdentifier: ret = this.parsedString; break;
          case kOption: ret = this.parsedString!=null ? this.parsedString : /*sInput !=null ? sInput :*/ ""; break;
          case kString: ret = this.parsedString; break;
          case kOnlySemantic: ret = null; break;
          default:
            //ret = "??unknown kind of node = " + Integer.toHexString(kind) + "=" + Integer.toHexString(-kind) + ".?"; 
            //it is especially on options or repetitions
            ret = this.parsedString;
//            if(parsedString !=null) {
//              ret = parsedString;
//            //} else if(sInput !=null) {
//            //  ret = sInput; 
//            } else if(kind >=0) {
//              ret = "?repeat=" + kind;
//            } else {
//              ret= "?repeat=" + (-kind);
//            }
        }    
      }
      return ret;
    }
    
    
    @Override
    protected ParseResultItemImplement clone()
    { ParseResultItemImplement parseResultItemImplement = this;
    ParseResultItemImplement item = new ParseResultItemImplement(null, parseResultItemImplement.sSemantic, null, null, parseResultItemImplement.elementSyntax, null);
      item.kind = this.kind;
      item.nrofAlternative = this.nrofAlternative;
      item.parsedFloatNumber = this.parsedFloatNumber;
      item.parsedIntegerNumber = this.parsedIntegerNumber;
      item.parsedString = this.parsedString;
      item.isAdded = false;
      item.offsetAfterEnd = this.offsetAfterEnd;
      item.syntaxIdent = this.syntaxIdent;
      
      return item;
    }
    
    @Override
    public String toString()
    { return getDescription();
    }

    /** Implements from ParseResultItem */
    @Override public ZbnfParseResultItem nextSkipIntoComponent(/*Parser.ParserStore store, */ZbnfParseResultItem parent)
    { return next(parent, 1);
    }


    /** Implements from ParseResultItem */
    @Override public ZbnfParseResultItem next(/*Parser.ParserStore store, */ZbnfParseResultItem parent)
    { return next(parent, this.offsetAfterEnd);
    }


    /** Implements from ParseResultItem */
    private ZbnfParseResultItem next(/*Parser.ParserStore store, */ZbnfParseResultItem parent, int offset)
    { int idxNew = this.idxOwn + offset;
      int idxEnd;
      if(parent != null)
      { ParseResultItemImplement parent1 = (ParseResultItemImplement)(parent);
        idxEnd = parent1.idxOwn + parent1.offsetAfterEnd;
      }
      else idxEnd = this.store.items.size();

      if( idxNew < idxEnd)
      { if(idxNew < this.store.items.size())
          return this.store.items.get(idxNew);
        else
        { stop();
          return null;
        }
      }
      else return null;
    }


    /**It's a debug helper. The method is empty, but it is a mark to set a breakpoint. */
    private void stop()
    { 
    }


    protected int getIdx()
    {
      return this.idxOwn;
    }

    protected int getIdxAfterComponent()
    {
      return this.idxOwn + this.offsetAfterEnd;
    }

    /*
    public boolean xxxisInsideComponent(ParseResultItem parentP)
    { ZbnfParseResultItem parent = (ParseResultItemImplement)parentP;
      return (  idxOwn >= parent.idxOwn
             && idxOwn < (parent.idxOwn + parent.offsetAfterEnd)
             );
    }
    */

    /**Gets the number of the alternative. If there was no alternative to test,
     * the method returns -1. If there was an possible empty option like [<?semantic> testit ],
     * and the option doesn't match, it returns 0. If there were any alternatives
     * to test, the method returns the founded alternative, count from 1.
     */
    @Override public int getNrofAlternative()
    { return nrofAlternative;
    }

    
    
    /**This is the super type to store the result. 
     * @return null if not given, on simple items. 
     */
    public String getSuperItemType() {
      if(this.elementSyntax !=null) {
        if(this.elementSyntax.componentSyntax !=null && this.elementSyntax.componentSyntax.sSuperItemType !=null) {
          return this.elementSyntax.componentSyntax.sSuperItemType;
        } 
        else if(this.elementSyntax.sSuperItemType !=null) {
          return this.elementSyntax.sSuperItemType;
        }
        else { 
          return null;
        }
      }
      else {
        return null;
      }
    }

    @Override public boolean isOption()
    { return this.kind == kOption;
    }

    static class IteratorImpl implements Iterator<ZbnfParseResultItem>
    {
      final int idxBegin;
      final int idxEnd;
      int idx;
      final ArrayList<ParseResultItemImplement> items;
      
      IteratorImpl(ParseResultItemImplement parent)
      { this.idxBegin = parent.idxOwn +1;
        this.idxEnd = parent.idxOwn + parent.offsetAfterEnd;
        this.items = parent.store.items;
        this.idx = this.idxBegin;
      }
      
      @Override
      public boolean hasNext()
      {
        return this.idx < this.idxEnd;
      }

      @Override
      public ZbnfParseResultItem next()
      { 
        if(hasNext())
        { ParseResultItemImplement item =this.items.get(this.idx);
          this.idx += item.offsetAfterEnd;
          return item;
        }  
        else return null;
      }

      @Override
      public void remove()
      { throw new RuntimeException("remove not expected"); 
      }
      
    }
    
    @Override
    public Iterator<ZbnfParseResultItem> iteratorChildren()
    { return new IteratorImpl(this);
    }

    
    
    private void buildTreeNodeRepresentation()
    { this.treeNodeRepresentation = new SortedTreeNode<ZbnfParseResultItem>();
      Iterator<ZbnfParseResultItem> iter = iteratorChildren();
      while(iter.hasNext())
      { ZbnfParseResultItem item =iter.next(); 
        this.treeNodeRepresentation.add(item.getSemantic(), item);
      }
    }

    
    @Override public ZbnfParseResultItem getParent(){
      return null;
    }

    
    

    /**Gets the named child or null.
     * @param key Name or path. The key can address more as one key in tree-depth,
     *            separated with slash. '/' This feature is since 2010-06-02
     * implements {@link org.vishia.util.SortedTree#getChild(java.lang.String)}
     */
    @Override
    public ZbnfParseResultItem getChild(String key)
    { if(this.offsetAfterEnd == 1){ return null; }
      else if(key == null || key.length()==0)
      {
        return this.store.items.get(this.idxOwn +1);
      }
      else
      { if(this.treeNodeRepresentation == null){ buildTreeNodeRepresentation(); }
        int posSep = key.indexOf('/');
        final String key2 = posSep >=0 ? key.substring(0, posSep) : key; 
        ZbnfParseResultItem zbnfChild = this.treeNodeRepresentation.getChild(key2);
        if(zbnfChild !=null && posSep >=0){
          final String key3 = key.substring(posSep+1);
          zbnfChild = zbnfChild.getChild(key3);
        }
        return zbnfChild;
      }  
    }

    
    @Override
    public String getChildString(String key)
    {
      ZbnfParseResultItem child = getChild(key);
      if(child != null) return child.getParsedString();
      else return null;
    }
    
    
    

    @Override
    public Iterator<ZbnfParseResultItem> iterChildren()
    { return iteratorChildren();
    }


    @Override
    public Iterator<ZbnfParseResultItem> iterChildren(String key)
    { if(this.offsetAfterEnd == 1){ return null; }
      else
      { if(this.treeNodeRepresentation == null){ buildTreeNodeRepresentation(); }
        return this.treeNodeRepresentation.iterChildren(key);
      }
    }


    @Override
    public List<ZbnfParseResultItem> listChildren()
    { if(this.offsetAfterEnd == 1){ return null; }
      else
      { if(this.treeNodeRepresentation == null){ buildTreeNodeRepresentation(); }
        return this.treeNodeRepresentation.listChildren();
      }
    }


    @Override
    public List<ZbnfParseResultItem> listChildren(String key)
    { if(this.offsetAfterEnd == 1){ return null; }
      else
      { if(this.treeNodeRepresentation == null){ buildTreeNodeRepresentation(); }
        return this.treeNodeRepresentation.listChildren(key);
      }
    }


    @Override
    public ZbnfParseResultItem firstChild()
    { return getChild(null);
      //if(offsetAfterEnd > 1)
      //{ return store.items.get(idxOwn +1);
      //}
      //else return null;
    }


    @Override
    public ZbnfParseResultItem next()
    { int idxNew = this.idxOwn + this.offsetAfterEnd;
      if(idxNew < this.store.items.size())
      { return this.store.items.get(idxNew);
      }
      else
      { stop();
        return null;
      }
    }

    /**Sets information about the source of parsing in the parent result item, that is a component item usually.
     * This is called especially if a constant (terminate morphem) was parsed which does not generate a result item.
     * It is similar to {@link #addInParent(long, long, int, int, String)} to the source information.
     * The information are only set if the parent contains null for the source file. Elsewhere the information
     * was set already by any of the previous result items of this component, more exactly the first one.
     * @param parent
     * @param begin The begin position of the parsed String for this component.
     * @param end The end of parsed String
     * @param srcLine The line in source on begin of parsed String.
     * @param srcColumn The column in source on begin of parsed String.
     * @param srcFile The path to the source file.
     */
    /*package private*/ void setSrcLineColumnFileInParent(long begin, long end, int srcLine, int srcColumn, String srcFile)
    { ParseResultItemImplement parent1 = this;
      do {  
        if(parent1.sFile ==null){
          parent1.start = begin;
          parent1.end = end;
          parent1.sFile = srcFile;
          parent1.srcLine = srcLine;
          parent1.srcColumn = srcColumn;
        }
      } while( (parent1 = parent1.parent) != null); 
    }


    /**Adds an item in the parents, that is a component item usually. This method is called for the immediately parent
     * of a new item added to the store.
     * It sets also information about the source of parsing in the parent result item, similar to {@link #setSrcLineColumnFileInParent(long, long, int, int, String)}. 
     * The information are only set if the parent contains null for the source file. Elsewhere the information
     * was set already by any of the previous result items of this component, more exactly the first one.
     * @param begin The begin position of the parsed String for this component.
     * @param end The end of parsed String
     * @param srcLine The line in source on begin of parsed String.
     * @param srcColumn The column in source on begin of parsed String.
     * @param srcFile The path to the source file.
     */
//    private void addInParent(long begin, long end, int srcLine, int srcColumn, String srcFile)
//    {
//      ParseResultItemImplement parent1 = this;
//        //parent1.offsetAfterEnd +=1;
//        if(parent1.sFile ==null && srcFile !=null){
//          parent1.sFile = srcFile;
//          parent1.srcLine = srcLine;
//          parent1.srcColumn = srcColumn;
//        }
//        //correct also all offsetAfterEnd from all parents. If the parents are not ready parsed yet,
//        //its offsetAfterEnd is not correct, but it is set correctly after parsing the component.
//        //in this case the changing here is not necessary but also not interfering.
//        while( (parent1 = (ParseResultItemImplement)parent1.parent) != null 
//             && parent1.offsetAfterEnd >0
//             )
//        { parent1.offsetAfterEnd +=1;
//          if(parent1.sFile ==null && srcFile !=null){
//            parent1.sFile = srcFile;
//            parent1.srcLine = srcLine;
//            parent1.srcColumn = srcColumn;
//          }
//        }
//      }

    
    @Override
    public ZbnfSyntaxPrescript syntaxItem ( ) { return this.elementSyntax == null ? null_syntaxElement : this.elementSyntax; }

    public ZbnfSyntaxPrescript componentSyntax ( ) { return this.elementSyntax ==null ? null : this.elementSyntax.componentSyntax; }

  }


  
  public interface PrepareXmlNode
  {
    
    void prepareXmlNode(XmlNode xmlDst, String text) throws XmlException;
  }
  

  
  
  
  private static class ConvertWikiStyle implements PrepareXmlNode
  {
    WikistyleTextToSimpleXml wikistyleText2SimpleXml = new WikistyleTextToSimpleXml();

    public void prepareXmlNode(XmlNode xmlDst, String text) throws XmlException
    {
      // TODO Auto-generated method stub
      wikistyleText2SimpleXml.setWikistyleFormat(text, xmlDst, null, null);
    }
    
  }
  


  /** The last item, useable for set... */
  ParseResultItemImplement item;

  /** List of items to store, instaceof ParseResultItem */
  final ArrayList<ParseResultItemImplement> items;

  /**Temporary store for column. */
  private final int[] column = new int[1];
  
  /** The index to read out the parse results. See getNextParseResult().*/
  //int idxParserStore;

  
  static ConvertWikiStyle convertWikiformat = new ConvertWikiStyle(); 


  /** Constructs a new empty ParserStore.*/
  public ZbnfParserStore()
  { items = new ArrayList<ParseResultItemImplement>();
  }

  
  /** Returns the first parse result item to start stepping to the results.
   * See samples at interface ParseResultItem.
   *
   * @return The first parse result item.
   */
  public ZbnfParseResultItem getFirstParseResult()
  {
    if(items.size()>0)
    { //parseResult.idxParserStore = 0;
      return items.get(0);
    }
    else return null;
  }


  
  
  /**Adds a new item
   * If this is a components item, on end {@link #setAlternativeAndOffsetToEnd(int, int)} should be called.  
   * @param sSemantic
   * @param sInput
   * @param nAlternative
   * @param start
   * @param end
   * @param nLine
   * @param nColumn
   * @return The position of this entry, using for rewind(posititon);
   */
  ParseResultItemImplement add(String sSemantic, ZbnfSyntaxPrescript elementSyntax, ZbnfSyntaxPrescript componentSyntax
      , CharSequence sInput, int nAlternative, long start, long end, int srcLine, int srcColumn, String srcFile
     , ZbnfParseResultItem parent)
  { //if(sSemantic.equals("textExprTEST"))
    //  stop();
    //if(srcLine == 726 ) //&& srcColumn == 7)
    //  stop();
    if(sSemantic.equals("plainText"))
      Debugutil.stop();
    if(sSemantic.equals("alternateText"))
      Debugutil.stop();
    this.item = new ParseResultItemImplement(this, sSemantic, parent, "?", elementSyntax, componentSyntax);
    this.item.sInput = sInput == null ? null : sInput.toString();
    if(this.item.parsedString == null){ //it is not null if it was set in constructor, especially on sSemantic = "name=value".
      this.item.parsedString = item.sInput;
    }
    if(item.kind ==0){  //it is not 0 if it was set in constructor, especially on sSemantic = "name=value".
      item.kind = nAlternative;
    }
    item.start = start;
    item.end = end;
    item.srcLine = srcLine;
    item.srcColumn = srcColumn;
    item.sFile = srcFile;
    item.idxOwn = items.size();
    if(item.idxOwn == 221)
      stop();
    items.add(item);
    if(item.parent !=null){
      item.parent.setSrcLineColumnFileInParent(start, end, srcLine, srcColumn, srcFile);
      //item.parent.addInParent(start, end, srcLine, srcColumn, srcFile);
    }
    //Only the immediately parent.
    if(parent != null)
    { 
      //((ParseResultItemImplement)(parent)).offsetAfterEnd +=1; 
    }
    return item;
    //return items.size() -1;  //position of the entry
  }

  
  
//  void XXXadd(ParseResultItemImplement item) {
//    
//  }
  

  /** Sets the spread of the item (@link {@link ParseResultItemImplement#offsetAfterEnd} 
   * and the number of the alternative into a existing item.
   * The item is to be added on begin of parsing the part, and the
   * number of alternative is to be added at the end of it.
   * The number of alternative is ascertained only at the end. Also at the end
   * it is possible to add the offsetAfterEnd-offset. It is done here.
   * If the item isn't a type of alternative, a RuntimeException is thrown.
   * <br>
   * New since 2022-02: If a component is finished, and the parent component 
   * has the same super type and contains only this only one element,
   * then the parent is removed.   
   *
   * @param idxStore The index where find the alternative item in the store
   * @param alternative The number of founded, alternative from 1, 0 if it is the empty option.
   */
  void setAlternativeAndOffsetToEnd(int idxStore, int alternative)
  { int zItems = this.items.size();
    int nrItems = zItems - idxStore;
    ParseResultItemImplement itemStart = this.items.get(idxStore);
    String sT1 = itemStart.getSuperItemType();
    if(sT1 !=null) { 
      ParseResultItemImplement itemNext = this.items.get(idxStore+1);
      if(itemNext.getIdxAfterComponent() == zItems) {      // only one child in the component
        String sT2 = itemNext.getSuperItemType();
        if(sT2 !=null && sT2.equals(sT1)) {                // it is the same superType
          this.items.remove(idxStore);                     // Then remove this parent component
          zItems -=1;
          for(int ix = idxStore; ix < zItems; ++ix) {
            this.items.get(ix).idxOwn = ix;
          }
          nrItems -=1;                                     //the child is the correct presentation.
          itemStart = itemNext;
          assert(itemStart.offsetAfterEnd == nrItems);
        }
      }
    }
//    if(nrItems ==1) {
//      int ixLast = this.items.size()-1;
//      ParseResultItemImplement itemCurr = this.items.get(ixLast);
//      String sSuperItemTypeCurr = itemCurr.getSuperItemType();
//      String sSuperItemTypePrev = itemStart.getSuperItemType();
//      if(sSuperItemTypePrev !=null || sSuperItemTypeCurr !=null) { // && sSuperItemTypeCurr.equals(sSuperItemTypePrev)) {
//        this.items.set(idxStore, itemCurr);
//        this.items.remove(ixLast);
//      }
//    }
    itemStart.nrofAlternative = alternative;
    
    itemStart.offsetAfterEnd = nrItems;
  }

  /** Sets the src of the parsing. It is the return value from getParsedText();
   * 
   * @param sInput the parsed text
   */
//  void XXXsetParsedText(int idxStore, String sInput)
//  {
//    ParseResultItemImplement item = items.get(idxStore);
//    item.sInput = sInput;
//  }
  
//  void XXXsetParsedString(int idxStore, String ss)
//  {
//    ParseResultItemImplement item = items.get(idxStore);
//    item.parsedString = ss;
//  }
  
  
  ParseResultItemImplement addRepetition(int countRepetition, String sSemantic, ZbnfSyntaxPrescript elementSyntax, long start, long end, int nLine, int nColumn, String sFile, ZbnfParseResultItem parent )
  { return add(sSemantic, elementSyntax, null, null, countRepetition, start, end, nLine, nColumn, sFile, parent);
  }


  ParseResultItemImplement addRepetitionRepeat(int countRepetition, String sSemantic, ZbnfSyntaxPrescript elementSyntax, long start, long end, int nLine, int nColumn, String sFile, ZbnfParseResultItem parent )
  { return add(sSemantic, elementSyntax, null, null, -countRepetition, start, end, nLine, nColumn, sFile, parent);
  }


  ParseResultItemImplement addConstantSyntax(ZbnfSyntaxPrescript elementSyntax, String sInput, long start, long end, int nLine, int nColumn, String sFile, ZbnfParseResultItem parent )
  { return add(null, elementSyntax, null, sInput, kTerminalSymbol, start, end, nLine, nColumn, sFile, parent);
  }


  ParseResultItemImplement addSemantic(String sSemantic, ZbnfSyntaxPrescript elementSyntax, ZbnfParseResultItem parent, int srcLine, int srcColumn, String srcFile)
  { return add(sSemantic, elementSyntax, null, null, kOnlySemantic, 0,0, srcLine, srcColumn, srcFile, parent);
  }


  /** Adds a founded string to the parsers store. It is called at as the issue of
   * parsing some special string tests, such as &lt;!regex?..> or &lt;""?...>.
   * @param spInput
   * @return
   */
  ParseResultItemImplement addString(StringPart spInput, String sSemantic, ZbnfSyntaxPrescript elementSyntax, ZbnfParseResultItem parent)
  { long start = spInput.getCurrentPosition();
    long end   = start + spInput.length();
    int nLine = spInput.getLineAndColumn(column);
    String sFile = spInput.getInputfile();
    return add(sSemantic, elementSyntax, null, spInput.getCurrentPart().toString(), kString, start, end, nLine, column[0], sFile, parent);
  }

  /** Adds a founded string to the parsers store. It is called at as the issue of
   * parsing some special string tests, such as &lt;!regex?..> or &lt;""?...>.
   * @param spInput
   * @return
   */
  ParseResultItemImplement addString(CharSequence src, String sSemantic, ZbnfSyntaxPrescript elementSyntax, StringPart spInput, ZbnfParseResultItem parent, int srcLine, int srcColumn, String srcFile)
  { return add(sSemantic, elementSyntax, null, src, kString, -1, -1, srcLine, srcColumn, srcFile, parent);
  }

  void addIdentifier(String sSemantic, ZbnfSyntaxPrescript elementSyntax, String sIdent, ZbnfParseResultItem parent, int srcLine, int srcColumn, String srcFile)
  { item = new ParseResultItemImplement(this, sSemantic, parent, "$", elementSyntax, null);
    item.sInput = null;
    item.kind = kIdentifier;
    item.parsedString = sIdent;
    item.idxOwn = items.size();
    if(item.idxOwn == 221)
      stop();
    items.add(item);
    if(item.parent !=null){
      item.parent.setSrcLineColumnFileInParent(-1, -1, srcLine, srcColumn, srcFile);
      //item.parent.addInParent(-1, -1, srcLine, srcColumn, srcFile);
    }
    if(parent != null) { //only the immediate parent.
      //((ParseResultItemImplement)(parent)).offsetAfterEnd +=1; 
    }
  }


  void addIntegerNumber(String sSemantic, ZbnfSyntaxPrescript elementSyntax, long number, String[] sNumber, ZbnfParseResultItem parent)
  { item = new ParseResultItemImplement( this, sSemantic, parent, "#", elementSyntax, null);
    item.sInput = sNumber ==null ? null :  sNumber[0];
    item.kind = kIntegerNumber;
    item.parsedIntegerNumber = number;
    item.idxOwn = items.size();
    if(item.idxOwn == 221)
      stop();
    items.add(item);
    if(parent != null)
    { //((ParseResultItemImplement)(parent)).offsetAfterEnd +=1; 
    }
  }


  void addFloatNumber(String sSemantic, ZbnfSyntaxPrescript elementSyntax, double number, ZbnfParseResultItem parent)
  { item = new ParseResultItemImplement( this, sSemantic, parent, "#f", elementSyntax, null);
    item.sInput = null;
    item.kind = kFloatNumber;
    item.parsedFloatNumber = number;
    item.idxOwn = items.size();
    if(item.idxOwn == 221)
      stop();
    items.add(item);
    if(parent != null)
    { //((ParseResultItemImplement)(parent)).offsetAfterEnd +=1; 
    }
  }

  
  
  
  ZbnfParseResultItem getItem(int idx)
  { return items.get(idx);
  }
  
  
  /** adds items form another ParserStore. This is used especially to transfer parse results to another component.
   * 
   * @param addStore
   * @return position of the added content. 
   */
//  int xxxadd(ZbnfParserStore addStore, ZbnfParseResultItem parent)
//  { int idx = items.size(); //actual position
//    if(addStore.items.size() >0)
//    { Iterator<ParseResultItemImplement> iter = addStore.items.iterator();
//      while(iter.hasNext())
//      { ParseResultItemImplement item = (iter.next());
//        if(item.isAdded)
//        { //the item is used onetime, therefore clone it.
//          item = item.clone();
//        }
//        if(!item.isAdded)
//        { item.parent = (ParseResultItemImplement)parent;
//          item.isAdded = true;
//          item.idxOwn = items.size();
//          item.store = this;
//          if(item.idxOwn == 80)
//            stop();
//          items.add(item);
//          if(parent != null)
//          { ((ParseResultItemImplement)(parent)).offsetAfterEnd +=1; 
//          }
//        }
//      }
//    }
//    //items.addAll(addStore.items);
//    return idx;
//  }


  /** adds items form another ParserStore. This is used especially to transfer parse results to another component.
   * 
   * @param addStore
   * @return nrof items added. 
   */
  int insert(ZbnfParserStore addStore, final int idxStore, ZbnfParseResultItem parent)
  { int idx = idxStore;
    if(addStore.items.size() >0)
    { Iterator<ParseResultItemImplement> iter = addStore.items.iterator();
      while(iter.hasNext())
      { ParseResultItemImplement item = (iter.next());
        if(item.isAdded)
        { //the item is used already, therefore clone it.
          item = item.clone();
        }
        if(!item.isAdded)
        { item.isAdded = true;
          item.idxOwn = idx;
          item.store = this;
          if(item.idxOwn == 80)
            stop();
          items.add(idx, item);
          if(parent != null)
          { ParseResultItemImplement parent1 = ((ParseResultItemImplement)(parent));
            //parent1.offsetAfterEnd +=1;
            //correct also all offsetAfterEnd from all parents. If the parents are not ready parsed yet,
            //its offsetAfterEnd is not correct, but it is set correctly after parsing the component.
            //in this case the changing here is not necessary but also not interfering.
            while( (parent1 = (ParseResultItemImplement)parent1.parent) != null 
                 && parent1.offsetAfterEnd >0
                 )
            { //parent1.offsetAfterEnd +=1;
            }
          }
          idx +=1;
        }
      }
      while(idx < items.size())
      { ParseResultItemImplement item = items.get(idx);
        item.idxOwn = idx;
        idx+=1;
      }
    }
    //items.addAll(addStore.items);
    return idx - idxStore;
  }


  /** adds the information, it is an metamorphem.
   *
   * @param sSemantic The given semantic, may be null
   * @return the position of the item in array list.
   */
//  int xxxaddComponent(String sSemantic, ZbnfParseResultItem parent, String syntaxIdent)
//  { item = new ParseResultItemImplement(this, sSemantic, parent, syntaxIdent, null, null);
//    item.kind = kComponent;
//    item.idxOwn = items.size();
//    if(item.idxOwn == 221)
//      stop();
//    items.add(item);
//    if(parent != null)
//    { ((ParseResultItemImplement)(parent)).offsetAfterEnd +=1; 
//    }
//    return items.size() -1;
//  }

  /** Sets the offset to the end
   *
   * @param idxFromwhere
   */
//  void xxx_setOffsetToEnd(int idxFromwhere)
//  { ParseResultItemImplement fromWhere = items.get(idxFromwhere);
//    fromWhere.offsetAfterEnd = items.size() - idxFromwhere;
//  }


  /** Gets the position for the next entry, useable for rewind().
   *
   * @return The size of content equals index of next position.
   */
  int getNextPosition()
  { return items.size();
  }

  /**Removes all entries start from position.
   *
   * @param pos following position after last valid result. If pos is negative, no action is done. 
   */
  void setCurrentPosition(int pos)
  { if(pos >=0)  
    { int ii = items.size()-1;
      while(ii >= pos)
      { /* isAdded is only used in adding store, the items will be labeled there#
         * if there are added to the main store, but they must stay there for further using.
         * 
        */
        ParseResultItemImplement item9 = items.get(ii); 
        item9.isAdded = false;
        //((items.get(ii))).isAdded = false;
        items.remove(ii);
        /*//They may be another bug, some addResult methods does not increment the offsetAfterEnd. If this routine cleans it, it seams to be faulty.
        { ParseResultItemImplement parent = item9.parent;
          while(parent !=null){
            if(parent.offsetAfterEnd + parent.idxOwn > ii ){
              parent.offsetAfterEnd = ii - parent.idxOwn;
            }
            parent = parent.parent;
          }
          
        }
        */
        //item9.parent = null;
        ii-=1;
      }
    }
  }


  /** Gets the next item to step through the items.
   *
   * @return Iterator, usind like List.iterator().
   */
  public Iterator<ParseResultItemImplement> getIterator()
  { return items.iterator();
  }

  
  
  /*package private*/ static class BuilderTreeNodeXml 
  {
    
    
    
    /**Will be set in {@link ZbnfParser#setXmlSrcline(boolean)}, {@link ZbnfParser#setXmlSrctext(boolean)}. */
    /*package private*/ boolean bXmlSrcline, bXmlSrctext;
    
    
    BuilderTreeNodeXml(){}
    
    
  
    /**Builds an XML tree node representation for the current element and its children of a parser result item.
     * @param xmlParent null if a  {@link ZbnfParser.ParseResultlet#xmlResult} is built 
     *   in {@link ZbnfParser.PrescriptParser#parsePrescript1(String, ZbnfParseResultItem, ZbnfParserStore, ZbnfParserStore, boolean, int)}.
     *   in this case the node has not a parent. The parent if this method is called recursively internally.
     * @param cmpnResult The Zbnf result component in the Zbnf Arraylist presentation.
     * @param bRecursive true then for all children of children.
     * @return
     */
    XmlNode buildTreeNodeRepresentationXml(XmlNode xmlParent
        , ParseResultItemImplement cmpnResult, boolean bRecursive) 
    {
      //long time = System.nanoTime();
      XmlNode xmlNode = cmpnResult.treeNodeXml;
      if(xmlNode ==null){
        if(cmpnResult.sSemantic.equals("description"))
          Debugutil.stop();
        cmpnResult.treeNodeXml = xmlNode = createXmlNode(xmlParent, cmpnResult);
        if(cmpnResult.isComponent()){
          Iterator<ZbnfParseResultItem> iter = cmpnResult.iteratorChildren();
          while(iter.hasNext()) { 
            ZbnfParseResultItem item =iter.next(); 
            ParseResultItemImplement childResult = (ParseResultItemImplement)item;
            XmlNode xmlChild = childResult.treeNodeXml;
            if( xmlChild !=null){
              //The child component has a treeNodeRepresentation already.
              //It is because it is a component, any component has gotten its treeNodeRepresentation already.
              assert(xmlNode !=null);
              if(xmlChild.getParent() !=null){
//                CheckVs.check(false);
              }
              try{ xmlNode.addContent(xmlChild); } catch(XmlException exc){ throw new IllegalArgumentException(exc); }
            } else {
              //assert(!childResult.isComponent());
              //No component. Build a leaf and add it.
              if(bRecursive){
                buildTreeNodeRepresentationXml(xmlNode, childResult, bRecursive);
                xmlChild = childResult.treeNodeXml;
              } else {
                createXmlNode(xmlParent, childResult);
              }
            }
          }
        }
      }
      //time = System.nanoTime() - time;
      //System.out.println("buildTreeNodeRepresentationXml; " + time + "; " + cmpnResult.sSemantic);
      return xmlNode;
    }
    
    
    
    private XmlNode createXmlNodeIntern(String sTagName, XmlNode xmlParent
    , ParseResultItemImplement parseResult
    ){
      XmlNode xmlNode = new XmlNodeSimple<ZbnfParseResultItem>(sTagName, parseResult);
      if(xmlParent !=null){
        try { 
          xmlParent.addContent(xmlNode);
        } catch (XmlException e) {
          throw new IllegalArgumentException(e);
        }
      }
      if(bXmlSrctext && parseResult.sInput !=null){
        String srctext;
        if(parseResult.sInput.length()>30){
          srctext = parseResult.sInput.substring(0,30);
        } else {
          srctext = parseResult.sInput;
        }
        xmlNode.setAttribute("src-text", srctext);
      }
      if(bXmlSrcline && parseResult.srcLine >0){
        xmlNode.setAttribute("src-line", Integer.toString(parseResult.srcLine));
      }
      if(bXmlSrcline && parseResult.srcColumn >0){
        xmlNode.setAttribute("src-col", Integer.toString(parseResult.srcColumn));
      }
      return xmlNode;
    }
  
    
    
    //static XmlNodeSimple<ZbnfParseResultItem> createXmlNode
    private XmlNode createXmlNode
    ( XmlNode xmlParentP
    , ParseResultItemImplement parseResult
    ){
      XmlNode xmlNode = xmlParentP;
      String semantic = parseResult.getSemantic();
//      if(semantic.startsWith("line")) 
//        Debugutil.stop();
      int sep;
      do { //loop for semantic/child/... : builds the node and children.
        sep = semantic.indexOf('/');
        if(sep >=0){ //should build a child
          String sLeftSemantic = semantic.substring(0, sep);
          XmlNode xmlMeta = xmlNode == null ? null : xmlNode.getChild(sLeftSemantic);
          //XmlNodeSimple<ZbnfParseResultItem> xmlMeta = xmlNode == null ? null : (XmlNodeSimple<ZbnfParseResultItem>)xmlNode.getChild(sLeftSemantic);
          if(parseResult.isRepetition() >0   //If this item is for repetition, any repeat need one extra node.
              //if it is not a repetition, it may be [<?semantic> or <syntax?semantic>, an existing node can be used.
            || xmlMeta ==null){ //create if not exists.
            xmlNode = createXmlNodeIntern(sLeftSemantic, xmlNode, parseResult);  //create new node as child of xmlNode
          } else { //child already existent.
            assert(xmlMeta instanceof XmlNodeSimple<?>);
            xmlNode = xmlMeta;
          }
          semantic = semantic.substring(sep +1);
        } else {
          //no children, or last child:
          if((xmlNode !=null) && semantic.startsWith("@")){
            //write result as attribute of the given node.
            int posValue = semantic.indexOf('=');              //an attribute can defined in form @name=value
            String sNameAttribute;
            if(posValue >=0){ sNameAttribute = semantic.substring(1, posValue); }
            else{ sNameAttribute = semantic.substring(1); }
            if(sNameAttribute.length() >0)                      //the given =value is stored if neccessary.
            { String sValue = posValue >=0 ? semantic.substring(posValue +1) : parseResult.getText();  
              xmlNode.setAttribute(sNameAttribute, sValue);
            }
          } else if(xmlNode !=null && (semantic.equals("text()") || semantic.equals("."))) { //write result as text.
              /**The last part of the semantic <code>tag/last()</code> is a routine ... TODO */
              String sValue = parseResult.getText();
              if(sValue != null && sValue.length() >0)
              { xmlNode.addContent(sValue);
              }
          }
          else
          { /**The last part of the semantic <code>path/tag</code> or the whole semantic is an XML-indentifier.
             * Create a child element with this tagname and add it to the output. 
             * This child, xmlNew, is the parent of the content.*/
            final String sTagName;
            boolean bExpandWikistyle = semantic.endsWith("+");
            boolean bSetParsedText = semantic.endsWith("&");
            if(bExpandWikistyle || bSetParsedText)
            { sTagName = semantic.substring(0, semantic.length()-1);
            }
            else
            { sTagName = semantic;
            }
            xmlNode = createXmlNodeIntern(sTagName, xmlNode, parseResult);  //create new node as child of xmlNode
            if(parseResult.kind != kComponent) { //add a text to {<?semantic>. Write {<?semantic=> if you do not whish the text.  //if(!parseResult.isComponent()){
              //NOTE: on a kComponent there is not a parsedString. But there may be a parsed input. Do not write here.
              //add the textual parse result to a leaf node.
              String sText = parseResult.getText();   //get number too!
              if(sText != null && sText.length() >0)
              { 
                if(bExpandWikistyle)
                { try{ convertWikiformat.prepareXmlNode(xmlNode, sText);
                  
                  } catch(XmlException exc){
                    System.err.println("ZbnfParserStore - XmlException convert wikiformat; " + exc.getMessage());
                  }
                }
                else 
                { xmlNode.addContent(sText);
                }  
              }
            }
          }
        }
      } while(sep >=0 && semantic.length() >0);
      return xmlNode;
    }
    
  }//class BuilderTreeNodeXml

  
  
  
  /**This is called from {@link ZbnfParser#writeResultAsTextList(Appendable)} as wrapper.
   * @param out
   * @throws IOException
   */
  void writeContentAsList(Appendable out) throws IOException {
    int indent = 16;
    int nrItems = 0;
    int ixNesting = -1;
    int[] nesting = new int[1000];
    StringFormatter format = new StringFormatter(out, false, "\n", 200);
    for(ParseResultItemImplement item: this.items) {
      while( ixNesting >=0 && nesting[ixNesting] == item.idxOwn) {
        ixNesting -=1;
        indent -=1;
      }
      format.addint(item.idxOwn, "2221");
      if(item.offsetAfterEnd !=1) {
        format.addint(item.idxOwn+item.offsetAfterEnd, "..1111");
        format.addint(item.offsetAfterEnd, " +1 ");
      }
      format.pos(indent);
      format.add(item.sSemantic);
      ZbnfSyntaxPrescript cmpnSyntax = item.componentSyntax();
      if(cmpnSyntax !=null) {
        String sIdent = cmpnSyntax.sDefinitionIdent;
        if(sIdent.equals(item.sSemantic)) {
          sIdent = "%";
        }
        format.add("=::").add(sIdent);
      }
      String superItemType = item.getSuperItemType();
      if(superItemType !=null) {
        format.add(':').add(superItemType);
      }
      format.pos(80);
      String info;
      if(item.isInteger()) { info = Long.toString(item.parsedIntegerNumber); }
      else if(item.isFloat()) { info = Double.toString(item.parsedFloatNumber); }
      else if(item.parsedString !=null) { info = item.parsedString; }
      else  { info = item.syntaxIdent; }
      format.add("  ").add(info);
      if(item.sInput !=null && (item.sInput != item.parsedString || item.parsedString ==null)) { 
        format.add("  >").add(item.getParsedText()).add('<'); 
      }
      format.flushLine("\n");
      if(item.offsetAfterEnd >1) {
        indent +=1;
        nesting[++ixNesting] = item.idxOwn + item.offsetAfterEnd;
      }
      if(indent >40) {
        indent = 41;
      }
    }
  }
  
  
  
  
  @Override
  public String toString()
  { String ret = "size=" + items.size();
    if(item!=null) ret+=" lastItem=" + item.sSemantic;
    return ret;
  }
  
  
  /**It's a debug helper. The method is empty, but it is a mark to set a breakpoint. */
  void stop()
  { //to test, set here a breakpoint.
  }
}

  