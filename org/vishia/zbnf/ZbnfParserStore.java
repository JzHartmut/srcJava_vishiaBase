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

import org.vishia.util.SortedTreeNode;
import org.vishia.util.StringPart;


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
    private ZbnfParseResultItem parent;
    
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

    
    /**The syntax identifications which has produced the stored result.
     * With this information a re-using of result can be do if the same syntax is detect 
     * in another path of the syntax graph, it spares calculation time. 
     */
    String syntaxIdent;
    
    /**  */
    SortedTreeNode<ZbnfParseResultItem> treeNodeRepresentation = null;
    
    
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
      this.parent = parent;
      this.syntaxIdent = syntax;
    }


    /**Gets the semantic of the item.
     *
     */
    public String getSemantic()
    { return (sSemantic != null) ? sSemantic : "Null-Semantic";
    }

    public String getParsedText()
    { return sInput;
    }

    public double getParsedFloat()
    { return parsedFloatNumber;
    }

    public long getParsedInteger()
    { return parsedIntegerNumber;
    }

    public String getParsedString()
    { return parsedString;
    }

    public int getInputColumn()
    { return nColumn;
    }
    
    public boolean isComponent()
    { return (offsetAfterEnd > 1) || kind == kComponent;
    }

    public boolean isInteger()
    { return kind == kIntegerNumber;
    }

    public boolean isFloat()
    { return kind == kFloatNumber;
    }

    public boolean isString()
    { return kind == kString;
    }

    public boolean isTerminalSymbol()
    { return kind == kTerminalSymbol;
    }

    //public boolean isSingleOption()
    //{ return kind == kSingleOption;
    //}

    public boolean isIdentifier()
    { return kind == kIdentifier;
    }

    public boolean isOnlySemantic()
    { return kind == kOnlySemantic;
    }

    public int isRepeat()
    { return (kind < 0 && kind > -kMaxRepetition ? -kind : -1);
    }

    public int isRepetition()
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

    public String getDescription()
    { String sSemantic = getSemantic();
      if(sSemantic.equals("doubleNumber"))
        stop();
      String sRet = " [" + idxOwn;
      if(offsetAfterEnd > 1)
      { sRet += ".." + (idxOwn + offsetAfterEnd -1);
      }

      sRet += "]";
      
      sRet += "<...?" + sSemantic + ">";

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
      return sRet;
    }

    
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
    
    public String toString()
    { return getDescription();
    }

    /** Implements from ParseResultItem */
    public ZbnfParseResultItem nextSkipIntoComponent(/*Parser.ParserStore store, */ZbnfParseResultItem parent)
    { return next(parent, 1);
    }


    /** Implements from ParseResultItem */
    public ZbnfParseResultItem next(/*Parser.ParserStore store, */ZbnfParseResultItem parent)
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
          return (ZbnfParseResultItem) store.items.get(idxNew);
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
    public int getNrofAlternative()
    { return nrofAlternative;
    }


    public boolean isOption()
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
      { return (ZbnfParseResultItem) store.items.get(idxNew);
      }
      else
      { stop();
        return null;
      }
    }



  }




  /** The last item, useable for set... */
  ParseResultItemImplement item;

  /** List of items to store, instaceof ParseResultItem */
  final ArrayList<ParseResultItemImplement> items;

  /** The index to read out the parse results. See getNextParseResult().*/
  //int idxParserStore;


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
      return (ZbnfParseResultItem)items.get(0);
    }
    else return null;
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
  private int add(String sSemantic, String sInput, int nAlternative, long start, long end, int nLine, int nColumn, ZbnfParseResultItem parent)
  { item = new ParseResultItemImplement(this, sSemantic, parent, "?");
    item.sInput = sInput;
    if(item.parsedString == null){ //it is not null if it was set in constructor, especially on sSemantic = "name=value".
      item.parsedString = sInput;
    }
    if(item.kind ==0){  //it is not 0 if it was set in constructor, especially on sSemantic = "name=value".
    	item.kind = nAlternative;
    }
    item.start = start;
    item.end = end;
    item.nLine = nLine;
    item.nColumn = nColumn;
    item.idxOwn = items.size();
    if(item.idxOwn == 221)
      stop();
    items.add(item);
    if(parent != null)
    { ((ParseResultItemImplement)(parent)).offsetAfterEnd +=1; 
    }
    return items.size() -1;  //position of the entry
  }


  int addAlternative(String sSemantic, int type, ZbnfParseResultItem parent, StringPart input)
  { return add(sSemantic, null, type, 0,0, input.getLineCt(), input.getCurrentColumn(), parent);
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
    ParseResultItemImplement item = (ParseResultItemImplement)items.get(idxStore);
    item.nrofAlternative = alternative;
    item.offsetAfterEnd = items.size() - idxStore;
  }

  /** Sets the src of the parsing. It is the return value from getParsedText();
   * 
   * @param sInput the parsed text
   */
  void setParsedText(int idxStore, String sInput)
  {
    ParseResultItemImplement item = (ParseResultItemImplement)items.get(idxStore);
    item.sInput = sInput;
  }
  
  void setParsedString(int idxStore, String ss)
  {
    ParseResultItemImplement item = (ParseResultItemImplement)items.get(idxStore);
    item.parsedString = ss;
  }
  
  
  int addRepetition(int countRepetition, String sSemantic, long start, long end, int nLine, int nColumn, ZbnfParseResultItem parent )
  { return add(sSemantic, null, countRepetition, start, end, nLine, nColumn, parent);
  }


  int addRepetitionRepeat(int countRepetition, String sSemantic, long start, long end, int nLine, int nColumn, ZbnfParseResultItem parent )
  { return add(sSemantic, null, -countRepetition, start, end, nLine, nColumn, parent);
  }


  int addConstantSyntax(String sInput, long start, long end, int nLine, int nColumn, ZbnfParseResultItem parent )
  { return add(null, sInput, kTerminalSymbol, start, end, nLine, nColumn, parent);
  }


  int xxxaddString(String sInput, long start, long end, int nLine, int nColumn, ZbnfParseResultItem parent )
  { return add(null, sInput, kTerminalSymbol, start, end, nLine, nColumn, parent);
  }


  int addSemantic(String sSemantic, ZbnfParseResultItem parent)
  { return add(sSemantic, null, kOnlySemantic, 0,0,0,0, parent);
  }


  /** Adds a founded string to the parsers store. It is called at as the issue of
   * parsing some special string tests, such as &lt;!regex?..> or &lt;""?...>.
   * @param spInput
   * @return
   */
  int addString(StringPart spInput, String sSemantic, ZbnfParseResultItem parent)
  { long start = spInput.getCurrentPosition();
    long end   = start + spInput.length();
    int nLine = spInput.getLineCt();
    int nColumn = 0;
    return add(sSemantic, spInput.getCurrentPart(), kString, start, end, nLine, nColumn, parent);
  }

  /** Adds a founded string to the parsers store. It is called at as the issue of
   * parsing some special string tests, such as &lt;!regex?..> or &lt;""?...>.
   * @param spInput
   * @return
   */
  int addString(String src, String sSemantic, ZbnfParseResultItem parent)
  { return add(sSemantic, src, kString, -1, -1, -1, -1, parent);
  }

  void addIdentifier(String sSemantic, String sIdent, ZbnfParseResultItem parent)
  { item = new ParseResultItemImplement(this, sSemantic, parent, "$");
    item.sInput = null;
    item.kind = kIdentifier;
    item.parsedString = sIdent;
    item.idxOwn = items.size();
    if(item.idxOwn == 221)
      stop();
    items.add(item);
    if(parent != null)
    { ((ParseResultItemImplement)(parent)).offsetAfterEnd +=1; 
    }
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
  int add(ZbnfParserStore addStore, ZbnfParseResultItem parent)
  { int idx = items.size(); //actual position
    if(addStore.items.size() >0)
    { Iterator<ParseResultItemImplement> iter = addStore.items.iterator();
      while(iter.hasNext())
      { ParseResultItemImplement item = (ParseResultItemImplement)(iter.next());
        if(item.isAdded)
        { //the item is used onetime, therefore clone it.
          item = item.clone();
        }
        if(!item.isAdded)
        { item.parent = parent;
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
      { ParseResultItemImplement item = (ParseResultItemImplement)(iter.next());
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
  { ParseResultItemImplement fromWhere = (ParseResultItemImplement) items.get(idxFromwhere);
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
        ((ParseResultItemImplement)(items.get(ii))).isAdded = false;
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

  