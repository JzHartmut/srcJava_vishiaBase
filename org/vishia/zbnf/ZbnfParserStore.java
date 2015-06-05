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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.vishia.util.Assert;
import org.vishia.util.Debugutil;
import org.vishia.util.SortedTreeNode;
import org.vishia.util.StringPart;
import org.vishia.xmlSimple.WikistyleTextToSimpleXml;
import org.vishia.xmlSimple.XmlException;
import org.vishia.xmlSimple.XmlNode;
import org.vishia.xmlSimple.XmlNodeSimple;
import org.vishia.zbnf.ZbnfParser.ParseResultlet;


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
class ZbnfParserStore
{
  /**Version, history and license.
   * <ul>
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
  public static final String sVersion = "2014-06-17";

  
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
    private ZbnfParserStore store;
    
    /**The item which is the parent (contains a offsetAfterEnd with includes this position). */
    private ParseResultItemImplement parent;
    
    /** The action to invoke.*/
    final String sSemantic;
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
     * =1 if it is no complex morphem.
     */
    int offsetAfterEnd = 1;

    /** The position from-to of the associated input*/
    long start, end;
    /** The line and column nr for debugging*/
    int nLine, nColumn;

    /**The file or adequate ressource from which this result comes from. */
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
    
    
    ParseResultItemImplement(ZbnfParserStore store, String sSemantic, ZbnfParseResultItem parent, String syntax)
    { this.store = store;
      int posValue = sSemantic.indexOf('='); 
      if(posValue>0){
        String value = sSemantic.substring(posValue+1);
        sSemantic = sSemantic.substring(0, posValue);
        this.parsedString = value;
        this.kind = kString;
      }
      this.sSemantic = sSemantic;
      this.parent = (ParseResultItemImplement)parent;
      this.syntaxIdent = syntax;
    }


    /**Gets the semantic of the item.
     *
     */
    @Override public String getSemantic()
    { return (sSemantic != null) ? sSemantic : "Null-Semantic";
    }

    @Override public String getParsedText()
    { return sInput;
    }

    @Override public double getParsedFloat()
    { return parsedFloatNumber;
    }

    @Override public long getParsedInteger()
    { return parsedIntegerNumber;
    }

    @Override public String getParsedString()
    { return parsedString;
    }

    @Override public int getInputLine()
    { return nLine;
    }
    
    @Override public int getInputColumn()
    { return nColumn;
    }
    
    @Override public String getInputFile(){ return sFile; }

    
    @Override public boolean isComponent()
    { return (offsetAfterEnd > 1) || kind == kComponent;
    }

    @Override public boolean isInteger()
    { return kind == kIntegerNumber;
    }

    @Override public boolean isFloat()
    { return kind == kFloatNumber;
    }

    @Override public boolean isString()
    { return kind == kString;
    }

    @Override public boolean isTerminalSymbol()
    { return kind == kTerminalSymbol;
    }

    //public boolean isSingleOption()
    //{ return kind == kSingleOption;
    //}

    @Override public boolean isIdentifier()
    { return kind == kIdentifier;
    }

    @Override public boolean isOnlySemantic()
    { return kind == kOnlySemantic;
    }

    @Override public int isRepeat()
    { return (kind < 0 && kind > -kMaxRepetition ? -kind : -1);
    }

    @Override public int isRepetition()
    { return (kind > 0 && kind < kMaxRepetition ? kind : -1);
    }

    public int xxxisAlternative()
    { return -1;
      //return (kind < 0 && kind > kMaxAlternate ? -kind : -1);
    }

    public String getKindOf()
    { String sRet;
      switch(kind)
      { case kComponent: sRet = "complex"; break;
        case kTerminalSymbol: sRet = "constant"; break;
        case kFloatNumber: sRet = "float"; break;
        case kIntegerNumber: sRet = "integer"; break;
        case kOnlySemantic: sRet = "semantic"; break;
        //case kRepetitionRepeat: sRet = "repeat"; break;
        //case kAlternative: sRet = "choice"; break;
        default:
        { if(kind >=0) sRet = "repetition nr "+ kind;
          else              sRet = "choose nr " + kind;
        }
      }
      return sRet;
    }

    @Override public String getDescription()
    { String sSemantic = getSemantic();
      if(sSemantic.equals("doubleNumber"))
        stop();
      String sRet = " [" + idxOwn;
      if(offsetAfterEnd > 1)
      { sRet += ".." + (idxOwn + offsetAfterEnd -1);
      }

      sRet += "]";
      
      sRet += "<...?" + sSemantic + ">";

      if(sSemantic.equals("operator"))
        Assert.stop();
      
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

      sRet += " syntax=" + syntaxIdent;
      
      sRet += " input=" + start + ".." + end + "(" + nLine + ", " + nColumn + ")";
      
      String sParsedText = getParsedText();
      if(sParsedText != null)
      { sRet += " read: \"" + sParsedText + "\"";
      }
      if(treeNodeXml !=null){ sRet += " xmlNode=" + treeNodeXml; }
      return sRet;
    }

    
    
    /**Returns a text information proper to use for XML if it is a leaf node.
     * Returns null if it is a component.
     */
    String getText(){
      String ret = null;
      if(isComponent()){ ret = null; }
      else{
        switch(kind){
          case kTerminalSymbol: ret = parsedString; break;
          case kIntegerNumber: ret = Long.toString(parsedIntegerNumber); break;
          case kFloatNumber: ret = Double.toString(parsedFloatNumber); break;
          case kIdentifier: ret = parsedString; break;
          case kOption: ret = parsedString!=null ? parsedString : ""; break;
          case kString: ret = parsedString; break;
          case kOnlySemantic: ret = null; break;
          default: ret = "??unknown kind of node = " + Integer.toHexString(kind) + "=" + Integer.toHexString(-kind) + ".?"; break;
        }
      }
      return ret;
    }
    
    
    @Override
    protected ParseResultItemImplement clone()
    { ParseResultItemImplement item = new ParseResultItemImplement(null, sSemantic, null, null);
      item.kind = kind;
      item.nrofAlternative = nrofAlternative;
      item.parsedFloatNumber = parsedFloatNumber;
      item.parsedIntegerNumber = parsedIntegerNumber;
      item.parsedString = parsedString;
      item.isAdded = false;
      item.offsetAfterEnd = offsetAfterEnd;
      item.syntaxIdent = syntaxIdent;
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
    { return next(parent, offsetAfterEnd);
    }


    /** Implements from ParseResultItem */
    private ZbnfParseResultItem next(/*Parser.ParserStore store, */ZbnfParseResultItem parent, int offset)
    { int idxNew = idxOwn + offset;
      int idxEnd;
      if(parent != null)
      { ParseResultItemImplement parent1 = (ParseResultItemImplement)(parent);
        idxEnd = parent1.idxOwn + parent1.offsetAfterEnd;
      }
      else idxEnd = store.items.size();

      if( idxNew < idxEnd)
      { if(idxNew < store.items.size())
          return store.items.get(idxNew);
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
      return idxOwn;
    }

    protected int getIdxAfterComponent()
    {
      return idxOwn + offsetAfterEnd;
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


    @Override public boolean isOption()
    { return kind == kOption;
    }

    static class IteratorImpl implements Iterator<ZbnfParseResultItem>
    {
      final int idxBegin;
      final int idxEnd;
      int idx;
      final ArrayList<ParseResultItemImplement> items;
      
      IteratorImpl(ParseResultItemImplement parent)
      { idxBegin = parent.idxOwn +1;
        idxEnd = parent.idxOwn + parent.offsetAfterEnd;
        items = parent.store.items;
        idx = idxBegin;
      }
      
      public boolean hasNext()
      {
        return idx < idxEnd;
      }

      public ZbnfParseResultItem next()
      { 
        if(hasNext())
        { ParseResultItemImplement item =items.get(idx);
          idx += item.offsetAfterEnd;
          return item;
        }  
        else return null;
      }

      public void remove()
      { throw new RuntimeException("remove not expected"); 
      }
      
    }
    
    public Iterator<ZbnfParseResultItem> iteratorChildren()
    { return new IteratorImpl(this);
    }

    
    
    private void buildTreeNodeRepresentation()
    { treeNodeRepresentation = new SortedTreeNode<ZbnfParseResultItem>();
      Iterator<ZbnfParseResultItem> iter = iteratorChildren();
      while(iter.hasNext())
      { ZbnfParseResultItem item =iter.next(); 
        treeNodeRepresentation.add(item.getSemantic(), item);
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
    public ZbnfParseResultItem getChild(String key)
    { if(offsetAfterEnd == 1){ return null; }
      else if(key == null || key.length()==0)
      {
        return store.items.get(idxOwn +1);
      }
      else
      { if(treeNodeRepresentation == null){ buildTreeNodeRepresentation(); }
        int posSep = key.indexOf('/');
        final String key2 = posSep >=0 ? key.substring(0, posSep) : key; 
        ZbnfParseResultItem zbnfChild = treeNodeRepresentation.getChild(key2);
        if(zbnfChild !=null && posSep >=0){
          final String key3 = key.substring(posSep+1);
          zbnfChild = zbnfChild.getChild(key3);
        }
        return zbnfChild;
      }  
    }

    
    public String getChildString(String key)
    {
      ZbnfParseResultItem child = getChild(key);
      if(child != null) return child.getParsedString();
      else return null;
    }
    
    
    

    public Iterator<ZbnfParseResultItem> iterChildren()
    { return iteratorChildren();
    }


    public Iterator<ZbnfParseResultItem> iterChildren(String key)
    { if(offsetAfterEnd == 1){ return null; }
      else
      { if(treeNodeRepresentation == null){ buildTreeNodeRepresentation(); }
        return treeNodeRepresentation.iterChildren(key);
      }
    }


    public List<ZbnfParseResultItem> listChildren()
    { if(offsetAfterEnd == 1){ return null; }
      else
      { if(treeNodeRepresentation == null){ buildTreeNodeRepresentation(); }
        return treeNodeRepresentation.listChildren();
      }
    }


    public List<ZbnfParseResultItem> listChildren(String key)
    { if(offsetAfterEnd == 1){ return null; }
      else
      { if(treeNodeRepresentation == null){ buildTreeNodeRepresentation(); }
        return treeNodeRepresentation.listChildren(key);
      }
    }


    public ZbnfParseResultItem firstChild()
    { return getChild(null);
      //if(offsetAfterEnd > 1)
      //{ return store.items.get(idxOwn +1);
      //}
      //else return null;
    }


    public ZbnfParseResultItem next()
    { int idxNew = idxOwn + offsetAfterEnd;
      if(idxNew < store.items.size())
      { return store.items.get(idxNew);
      }
      else
      { stop();
        return null;
      }
    }

    /*package private*/ void setSrcLineColumnFileInParent(int srcLine, int srcColumn, String srcFile)
    { ParseResultItemImplement parent1 = this;
      do {  
        if(parent1.sFile ==null){
          parent1.sFile = srcFile;
          parent1.nLine = srcLine;
          parent1.nColumn = srcColumn;
        }
      } while( (parent1 = parent1.parent) != null); 
    }


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


  
  
  private void addInParent(ZbnfParseResultItem parent, int srcLine, int srcColumn, String srcFile)
  {
    if(parent != null)
    { ParseResultItemImplement parent1 = ((ParseResultItemImplement)(parent));
      parent1.offsetAfterEnd +=1;
      if(parent1.sFile ==null && srcFile !=null){
        parent1.sFile = srcFile;
        parent1.nLine = srcLine;
        parent1.nColumn = srcColumn;
      }
      //correct also all offsetAfterEnd from all parents. If the parents are not ready parsed yet,
      //its offsetAfterEnd is not correct, but it is set correctly after parsing the component.
      //in this case the changing here is not necessary but also not interfering.
      while( (parent1 = (ParseResultItemImplement)parent1.parent) != null 
           && parent1.offsetAfterEnd >0
           )
      { parent1.offsetAfterEnd +=1;
        if(parent1.sFile ==null && srcFile !=null){
          parent1.sFile = srcFile;
          parent1.nLine = srcLine;
          parent1.nColumn = srcColumn;
        }
      }
    }
  }
  
  
  /** Adds a new item
   *
   * @param sSemantic
   * @param sInput
   * @param nAlternative
   * @param start
   * @param end
   * @param nLine
   * @param nColumn
   * @return The position of this entry, using for rewind(posititon);
   */
  private ParseResultItemImplement add(String sSemantic, CharSequence sInput, int nAlternative, long start, long end, int srcLine, int srcColumn, String srcFile, ZbnfParseResultItem parent)
  { if(sSemantic.equals("textOut"))
      stop();
    if(srcLine == 47 && srcColumn == 12)
      stop();
    item = new ParseResultItemImplement(this, sSemantic, parent, "?");
    item.sInput = sInput == null ? null : sInput.toString();
    if(item.parsedString == null){ //it is not null if it was set in constructor, especially on sSemantic = "name=value".
      item.parsedString = item.sInput;
    }
    if(item.kind ==0){  //it is not 0 if it was set in constructor, especially on sSemantic = "name=value".
      item.kind = nAlternative;
    }
    item.start = start;
    item.end = end;
    item.nLine = srcLine;
    item.nColumn = srcColumn;
    item.sFile = srcFile;
    item.idxOwn = items.size();
    if(item.idxOwn == 221)
      stop();
    items.add(item);
    addInParent(parent, srcLine, srcColumn, srcFile);
    /*
    if(parent != null)
    { 
      ((ParseResultItemImplement)(parent)).offsetAfterEnd +=1; 
    }
    */
    return item;
    //return items.size() -1;  //position of the entry
  }


  ParseResultItemImplement addAlternative(String sSemantic, int type, ZbnfParseResultItem parent, StringPart input)
  { if(input ==null){
      return add(sSemantic, null, type, 0,0, -1, -1, null, parent);
    } else {
      return add(sSemantic, null, type, 0,0, input.getLineAndColumn(column), column[0], input.getInputfile(), parent);
    }
  }

  /** Sets the number of the alternative into a existing item.
   * The item is to be added on begin of parsing the part, and the
   * number of alternative is to be added at the end of it.
   * The number of alternative is ascertained only at the end. Also at the end
   * it is possible to add the offsetAfterEnd-offset. It is done here.
   * If the item isn't a type of alternative, a RuntimeException is thrown.
   *
   * @param idxStore The index where find the alternative item in the store
   * @param alternative The number of founded, alternative from 1, 0 if it is the empty option.
   */
  void setAlternativeAndOffsetToEnd(int idxStore, int alternative)
  {
    ParseResultItemImplement item = items.get(idxStore);
    item.nrofAlternative = alternative;
    item.offsetAfterEnd = items.size() - idxStore;
  }

  /** Sets the src of the parsing. It is the return value from getParsedText();
   * 
   * @param sInput the parsed text
   */
  void XXXsetParsedText(int idxStore, String sInput)
  {
    ParseResultItemImplement item = items.get(idxStore);
    item.sInput = sInput;
  }
  
  void XXXsetParsedString(int idxStore, String ss)
  {
    ParseResultItemImplement item = items.get(idxStore);
    item.parsedString = ss;
  }
  
  
  ParseResultItemImplement addRepetition(int countRepetition, String sSemantic, long start, long end, int nLine, int nColumn, String sFile, ZbnfParseResultItem parent )
  { return add(sSemantic, null, countRepetition, start, end, nLine, nColumn, sFile, parent);
  }


  ParseResultItemImplement addRepetitionRepeat(int countRepetition, String sSemantic, long start, long end, int nLine, int nColumn, String sFile, ZbnfParseResultItem parent )
  { return add(sSemantic, null, -countRepetition, start, end, nLine, nColumn, sFile, parent);
  }


  ParseResultItemImplement addConstantSyntax(String sInput, long start, long end, int nLine, int nColumn, String sFile, ZbnfParseResultItem parent )
  { return add(null, sInput, kTerminalSymbol, start, end, nLine, nColumn, sFile, parent);
  }


  ParseResultItemImplement addSemantic(String sSemantic, ZbnfParseResultItem parent, int srcLine, int srcColumn, String srcFile)
  { return add(sSemantic, null, kOnlySemantic, 0,0, srcLine, srcColumn, srcFile, parent);
  }


  /** Adds a founded string to the parsers store. It is called at as the issue of
   * parsing some special string tests, such as &lt;!regex?..> or &lt;""?...>.
   * @param spInput
   * @return
   */
  ParseResultItemImplement addString(StringPart spInput, String sSemantic, ZbnfParseResultItem parent)
  { long start = spInput.getCurrentPosition();
    long end   = start + spInput.length();
    int nLine = spInput.getLineAndColumn(column);
    String sFile = spInput.getInputfile();
    return add(sSemantic, spInput.getCurrentPart().toString(), kString, start, end, nLine, column[0], sFile, parent);
  }

  /** Adds a founded string to the parsers store. It is called at as the issue of
   * parsing some special string tests, such as &lt;!regex?..> or &lt;""?...>.
   * @param spInput
   * @return
   */
  ParseResultItemImplement addString(CharSequence src, String sSemantic, StringPart spInput, ZbnfParseResultItem parent, int srcLine, int srcColumn, String srcFile)
  { return add(sSemantic, src, kString, -1, -1, srcLine, srcColumn, srcFile, parent);
  }

  void addIdentifier(String sSemantic, String sIdent, ZbnfParseResultItem parent, int srcLine, int srcColumn, String srcFile)
  { item = new ParseResultItemImplement(this, sSemantic, parent, "$");
    item.sInput = null;
    item.kind = kIdentifier;
    item.parsedString = sIdent;
    item.idxOwn = items.size();
    if(item.idxOwn == 221)
      stop();
    items.add(item);
    addInParent(parent, srcLine, srcColumn, srcFile);
    /*
    if(parent != null)
    { ((ParseResultItemImplement)(parent)).offsetAfterEnd +=1; 
    }*/
  }


  void addIntegerNumber(String sSemantic, long number, ZbnfParseResultItem parent)
  { item = new ParseResultItemImplement( this, sSemantic, parent, "#");
    item.sInput = null;
    item.kind = kIntegerNumber;
    item.parsedIntegerNumber = number;
    item.idxOwn = items.size();
    if(item.idxOwn == 221)
      stop();
    items.add(item);
    if(parent != null)
    { ((ParseResultItemImplement)(parent)).offsetAfterEnd +=1; 
    }
  }


  void addFloatNumber(String sSemantic, double number, ZbnfParseResultItem parent)
  { item = new ParseResultItemImplement( this, sSemantic, parent, "#f");
    item.sInput = null;
    item.kind = kFloatNumber;
    item.parsedFloatNumber = number;
    item.idxOwn = items.size();
    if(item.idxOwn == 221)
      stop();
    items.add(item);
    if(parent != null)
    { ((ParseResultItemImplement)(parent)).offsetAfterEnd +=1; 
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
  int xxxadd(ZbnfParserStore addStore, ZbnfParseResultItem parent)
  { int idx = items.size(); //actual position
    if(addStore.items.size() >0)
    { Iterator<ParseResultItemImplement> iter = addStore.items.iterator();
      while(iter.hasNext())
      { ParseResultItemImplement item = (iter.next());
        if(item.isAdded)
        { //the item is used onetime, therefore clone it.
          item = item.clone();
        }
        if(!item.isAdded)
        { item.parent = (ParseResultItemImplement)parent;
          item.isAdded = true;
          item.idxOwn = items.size();
          item.store = this;
          if(item.idxOwn == 80)
            stop();
          items.add(item);
          if(parent != null)
          { ((ParseResultItemImplement)(parent)).offsetAfterEnd +=1; 
          }
        }
      }
    }
    //items.addAll(addStore.items);
    return idx;
  }


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
            parent1.offsetAfterEnd +=1;
            //correct also all offsetAfterEnd from all parents. If the parents are not ready parsed yet,
            //its offsetAfterEnd is not correct, but it is set correctly after parsing the component.
            //in this case the changing here is not necessary but also not interfering.
            while( (parent1 = (ParseResultItemImplement)parent1.parent) != null 
                 && parent1.offsetAfterEnd >0
                 )
            { parent1.offsetAfterEnd +=1;
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
  int xxxaddComponent(String sSemantic, ZbnfParseResultItem parent, String syntaxIdent)
  { item = new ParseResultItemImplement(this, sSemantic, parent, syntaxIdent);
    item.kind = kComponent;
    item.idxOwn = items.size();
    if(item.idxOwn == 221)
      stop();
    items.add(item);
    if(parent != null)
    { ((ParseResultItemImplement)(parent)).offsetAfterEnd +=1; 
    }
    return items.size() -1;
  }

  /** Sets the offset to the end
   *
   * @param idxFromwhere
   */
  void xxx_setOffsetToEnd(int idxFromwhere)
  { ParseResultItemImplement fromWhere = items.get(idxFromwhere);
    fromWhere.offsetAfterEnd = items.size() - idxFromwhere;
  }


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
        ((items.get(ii))).isAdded = false;
        items.remove(ii);
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
                Assert.check(false);
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
      if(bXmlSrcline && parseResult.nLine >0){
        xmlNode.setAttribute("src-line", Integer.toString(parseResult.nLine));
      }
      if(bXmlSrcline && parseResult.nColumn >0){
        xmlNode.setAttribute("src-col", Integer.toString(parseResult.nColumn));
      }
      return xmlNode;
    }
  
    //static XmlNodeSimple<ZbnfParseResultItem> createXmlNode
    XmlNode createXmlNode
    ( XmlNode xmlParentP
    , ParseResultItemImplement parseResult
    ){
      XmlNode xmlNode = xmlParentP;
      String semantic = parseResult.getSemantic();
      int sep;
      do { //loop for semantic/child/... : builds the node and children.
        sep = semantic.indexOf('/');
        if(sep >=0){ //should build a child
          String sLeftSemantic = semantic.substring(0, sep);
          XmlNode xmlMeta = xmlNode == null ? null : xmlNode.getChild(sLeftSemantic);
          //XmlNodeSimple<ZbnfParseResultItem> xmlMeta = xmlNode == null ? null : (XmlNodeSimple<ZbnfParseResultItem>)xmlNode.getChild(sLeftSemantic);
          if(xmlMeta ==null){
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
            if(!parseResult.isComponent()){
              //add the textual parse result to a leaf node.
              String sText = parseResult.getText();
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

  