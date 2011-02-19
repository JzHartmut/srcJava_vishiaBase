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
 * 2006-12-15 JcHartmut: regular expressions should be handled after white spaces trimming, error correction.
 * 2006-06-00 JcHartmut: a lot of simple problems in developemnt.
 * 2006-05-00 JcHartmut: creation
 *
 ****************************************************************************/

package org.vishia.zbnf;

//import java.io.InputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//import org.vishia.util.SortedTreeNode;
import org.vishia.util.StringPart;

//import vishia.mainCmd.Report;
import org.vishia.mainCmd.Report;


/**An instance of ZbnfParser contains a syntax prescript inside and is able to parse a text, test the syntax and output
 * a tree of information given in the input text.<br/>
 * The invocation is in followed manner:<pre>
 * ZbnfParser parser = new ZbnfParser(reportConsole);
 * try{ parser.setSyntax(syntaxString);}
 * catch(ParseException exception)
 * { writeError("parser reading syntax error: " + exception.getMessage();
 *   return;
 * }
 * if(!parser.parse(inputString))
 * { writeError(parser.getSyntaxErrorReport());
 * }
 * else
 * { ParseResultItem resultItem = parser.getFirstParseResult();
 *   while( resultItem != null)
 *   { evaluateResult(resultItem);
 *     resultItem = resultItem.next(null))
 *   }
 * }</pre>
 *
 * <h2>The syntax</h2>
 * The Syntax given as argument of {@link setSyntax(StringPart)} is to be defined in the Semantic Backus Naur-Form 
 * (ZBNF, Z is a reverse S for Semantic). 
 * It is given as a String or {@link StringPart}.
 * The method setSyntax, reads the string and convert it in internal data. The input string (mostly readed from a file)
 * may be consist of a sequence of <b>variables</b> beginning with $ and <b>syntax terms</b>. A syntax term is described 
 * on the class {@link ZbnfSyntaxPrescript}, because this class converts a syntax term in an internal tree of syntax nodes.
 * Downside it is shown an example of a syntax file or string with all variables.
 * <pre>
 * &lt;?ZBNF-www.vishia.org version="1.0" encoding="iso-8859-1" ?&gt;  ##this first line is not prescribed but possible.
 * $setLinemode.                                 ##if set, than the newline char \n is not overwritten as whitespace
 * $endlineComment=##.                           ##defines the string introducing a comment to eol, default is //
 * $comment=[*...*].                             ##... between [* ... *] all chars are ignored, default is /*...* /
 * $keywords=if|else.                            ##that identifiers are not accepted as identifiers parsing by &lt;$?...&gt;
 * $inputEncodingKeyword="encoding".             ##it helps to define the encoding of the input file via a keyword input-file
 * $inputEncoding="UTF-8".                       ##it helps to define the encoding of the input file (useable outside parser core)
 * $xmlns:nskey="value".                         ##defines a namespace key for XML output (useable outside parser core)
 * 
 * component::=<$?name>=<#?number> { &lt;value> , }. ##The first syntax term is the toplevel syntax.
 * value::= val = [<?option> a | b | c].         ##another syntax term
 * </pre>
 * 
 *
 * <h2>White space and comment handling when parsing</h2>
 * The whitespaces and/or comments may be skipped over while parsing or not. The following rules ar valid:
 * <ul><li>The comment start/end characters defined in the syntax prescript are valid, if a calling of
 *   {@link setSkippingComment(String, String, boolean)}, {@link setSkippingEndlineComment(String, boolean)}, 
 *   {@link setWhiteSpaces(String)}, {@link setLinemode(boolean)} is not occured after {@link setSyntax(String)}.</li>
 * <li>Whitespaces and comments are skipped before any matching test occurs, but only if the syntax term in the syntax prescript
 *   has at least one whitespace at this position.</li>
 * <li>The consideration of whitespaces in syntax terms are switchable off by using the <code><$NoWhiteSpaces></code>-construct,
 *   see {@link ZbnfSyntaxPrescript}.  
 * <li>But if constant symbols are tested, first a comment is not skipped but tested. If the comment start with this constants,
 *   it is recognized as content.
 *   So it is possible to include comments in the parsing process . If the constant are not matched to a start of comment,
 *   the comment is skipped over and the test is repeated.</li>
 * </ul>  
 *
 * <h2>Evaluate the parsers result</h2>
 * By calling Parser.parse() a new result buffer is created. 
 * The result buffer contains entries with the parsed informations 
 * appropriate to the semantic semantic named in the syntax prescript. 
 * The evaluation of result starts with {@link getFirstParseResult()} to get the toplevel item. 
 *
 *
 */

public class ZbnfParser
{

  /** Helpfull empty string to build some spaces in strings. */
  static private final String sEmpty = "                                                                                ";


  
  
  /** Class to organize parsing of a component with a own prescript.
   *  It is a outer class from the working class SubParser.  
   */
  private class PrescriptParser
  {
    
     /**
       * The actual input stream. 
       * By calling parse recursively, a new SubParser instance is created, 
       * but the references to the same input and parse result are assigend. 
       * By using the Parser for another parsing execution, a new input and parseResult is used.
       */
    protected StringPart input;
  
    final PrescriptParser parentPrescriptParser;

    /**The parse result buffer is a own reference in each subParser.  If the parsed component is to be added in the main stream (the normal case), this pointer points to the parents parseResultBuffer identical with the parse result buffer at first level, the main buffer. But if the component parsed here is to be added later, with syntax prescript <code>&lt;...?-...&gt;</code>, the parseResultBuffer is a local instance here.
     */
    final ZbnfParserStore parserStoreInPrescript;

    /**List of typical one, but possible more as one parse results, which are stored in another component  in the parent level (using parseResultToOtherComponent there). The typical construct is: <code>  syntax::=... <somewhat?-semantic> ... <other?+semantic></code> In this sample the parse result from 'somewhat' is stored inside 'other'. But there may be more complexly constructs: <pre> syntax::=... <somewhat?-semantic>{ [ <other?+semantic> ] }.</pre> The above example shows the transportation in a repetition or alternatively level,  all levels use an instance of SubParser.; <pre>  syntax::=... <somewhat?-semantic>{ [ <other?*semantic> ] }. other::=.... <onother?-semantic> ... <inner?+semantic>. </pre> This above example shows, that at the one hand an additional result are transported from outside, and at the other hand an additional result is produced inside, both are added.  But both result must not merge, because on syntax error using 'other', the outside stored result  should be used furthermore, but the inside 'other' produced result have to be trashed. <br> This list is null, if no addParseResult is given.   
     */
    List<ZbnfParserStore> parseResultsFromOuterToInnerLevels;

    /**An additional parser store to accumulate parse results, which are transfered into the next deeper level of any next component. If it is used first, it is initialized.
     */
    ZbnfParserStore parseResultToOtherComponent = null;

    int idxOfidxWholeParserStoreForInsertionsFromOuterToInnerLevels = 0;
    
    /**This array contains some indices to insert the parseResultsFromOuterToInnerLevels for successfull parsed components.
     */
    int[] idxWholeParserStoreForInsertionsFromOuterToInnerLevels = new int[100];  //more as 10 positions are unexpected.
    
    ZbnfParseResultItem[] parentInWholeParserStoreForInsertionsFromOuterToInnerLevels = new ZbnfParseResultItem[100];
    
    final String sSemanticIdent;
    
    public PrescriptParser(PrescriptParser parent, String sSemantic, ZbnfParserStore parseResult, List<ZbnfParserStore> parseResultsFromOuterLevel)
    { parentPrescriptParser = parent;
      this.parserStoreInPrescript = parseResult;
      if(parseResultsFromOuterLevel != null) 
      { //build a new LinkedList, do not use the given reference, because inner results will be added too.
        parseResultsFromOuterToInnerLevels = new LinkedList<ZbnfParserStore>();
        for(ZbnfParserStore parserStore : parseResultsFromOuterLevel)
        { parseResultsFromOuterToInnerLevels.add(parserStore);  //copy all.
        }
      }
      
      sSemanticIdent = sSemantic;
    }
  
    
    void setparseResultsFromOuterLevels(ZbnfParserStore result)
    { if(parseResultsFromOuterToInnerLevels == null)
      { parseResultsFromOuterToInnerLevels = new LinkedList<ZbnfParserStore>();
      }
      parseResultsFromOuterToInnerLevels.add(result);
    }
  
    
    void insertParseResultFromOuterToInnerLevels()
    { for(int ii = 0; ii < idxOfidxWholeParserStoreForInsertionsFromOuterToInnerLevels; ii++)
      { int idxInStore = idxWholeParserStoreForInsertionsFromOuterToInnerLevels[ii];
        int idxDelta = 0; 
        ZbnfParseResultItem parent = parentInWholeParserStoreForInsertionsFromOuterToInnerLevels[ii];
        for(ZbnfParserStore addParseResult : parseResultsFromOuterToInnerLevels) //kk2
        { idxDelta += parserStoreInPrescript.insert(addParseResult, idxInStore, parent);  //will be ignored by rewind on error inside this component.
        }
        for(int i2 = ii+1; i2 < idxOfidxWholeParserStoreForInsertionsFromOuterToInnerLevels; i2++)
        { if(idxWholeParserStoreForInsertionsFromOuterToInnerLevels[i2] > idxInStore)
          //correct the positions because it is inserted anything before:
          idxWholeParserStoreForInsertionsFromOuterToInnerLevels[i2] += idxDelta;
        }
      }
    }
    
    
    /** To parse nested syntax, for every level a subparser is created.
     *
     */
    private class SubParser
    {
  
      /** The Prescript of the syntax.
       * The parser instance is useable for more as one parsing execution with the same syntax prescript.
       * */
      private final ZbnfSyntaxPrescript syntaxPrescript;
  
      /** The index of the current treated item.*/
      private int idxPrescript;
  
      /** Recursionscounter */
      private final int nRecursion;
  
      /** Counter of tested alternatives. On succesfull parsing it is the number of matched alternative.*/
      int xxxidxAlternative;
      
      /** List of the current alternativ syntax in the prescript.
       * Setted inside parseSub, used in some called methods.
       */
      List<ZbnfSyntaxPrescript> listPrescripts;
  
      /** Indixes inside syntaxList*/
      //int idxSyntaxList, idxSyntaxListEnd;
      
  
      /** The semantic for getExpectedSyntaxOnError.
       * @see getExpectedSyntaxOnError()
       */
      String sSemanticForError;
      
      /** Pointer to parent, used for build expected syntax on error:*/
      final SubParser parentParser;
      
      /**Reference to the parent parse result item of the parent, given on constructor. */
      final ZbnfParseResultItem parentOfParentResultItem;
  
      /**Reference to the parent parse result item. */
      ZbnfParseResultItem parentResultItem;
  
      /** Constructs a new Parser with a given parsed syntaxPrescript and a given input and output buffer.
       * This constructor is used by recursively calling.
       * @param syntax The prescript is a child of the parents' prescript.
       * @param parent Parent level of parsing, for debug and transform parseResultToOtherComponent.
       * @param parserStoreInPrescript typically the same result buffer as parent, but an other buffer, typically
       *        identically with parent.parssResultToOtherComponent, if the result shouldn't be stored in main stream,
       *        used to transform semantic results to a followed item (&lt;..?->-Syntax).
       *        It may be null, than create a new temporary parser result buffer.
       * */
      protected SubParser(ZbnfSyntaxPrescript syntax, SubParser parent, ZbnfParseResultItem parentResultItem )
      { syntaxPrescript = syntax;
        parentParser = parent;
        this.parentOfParentResultItem = parentResultItem;
        nRecursion = parent == null ? 0 : parent.nRecursion +1;
      }
  
      /**call if reused. */
      private void init()
      { if(parseResultToOtherComponent != null)
        { parseResultToOtherComponent = null;
          parseResultsFromOuterToInnerLevels.remove(parseResultToOtherComponent);
        }
      }
  
      /**Parses a syntax-prescript assigned with this class. This routine should be called 
       * only one time with a new instance of SubParser. But some instances of SubParser may be created in nested levels.
       *   
       * The input is used from the outer class Parser, aggregation 'input'.
       * It is running in a while()-loop. The loop breaks at end of syntax-prescript or
       * if no path in syntax prescript matches to the input. Inside the method 'parseComplexSyntax()'
       * this method may be called recursively, if the syntax prescript contains &lt;<i>syntax</i>...&gt;.<br/>
       *
       * If a semantic for storing is given as input argument sSemanticForStoring, 
       * or the syntax prescript have a semantic itself,
       * a parse result is stored as a component for this sub parsing. 
       * A semantic as input argument is given if this call results of <code>&lt;<i>syntax</i>...&gt;</code>. 
       * A semantic itself is given if the prescript starts with <code>&lt;?<i>syntax</i>&gt;</code> at example in 
       * option brackets <code>[&lt;?<i>syntax</i>&gt;...</code> or repeat brackets <code>{&lt;?<i>syntax</i>&gt;...</code> 
       * or also possible at start of an nested prescript <code>::=&lt;?<i>syntax</i>&gt;...</code>    
       * 
       * Everytime at first the whitespaces and comments are skipped over before the matching of the input
       * to the syntax prescript is tested. The mode of skipping whitespace and comments may be setted
       * by calling setSkippingComment(), setSkippingWhitespace() and setSkippingEndlineComment(), see there.<br>
       *
       * If the syntax prescript contains a semantic, this is at example by [&lt;?semantic&gt;...],
       * a parse result is written, containing the semantic and the nr of choice if there are some alternatives.
       * Also an empty option is considered.
       *
       * 
       * @param input The input to parse,it is a reference to the same instance as in parent.
       * @param sSemanticForErrorP The semantic superior semantic. This semantic is used for error report.<br/>
       * @param resultType type of the result item of this component. It may one of ParserStore.kOption etc or
       *        a less positiv or negativ number used for repetition and backward repetition. 
       * @param sSemanticForStoring The semantic given at the calling item. It may be 
       * <ul><li>null: No parse result item is created for this level of syntax component. In ZBNF this behavior is forced
       *   with noting <code>&lt;component?&gt;</code> by using the component.</li> 
       * <li>an identifier: A parse result item is created in the parsers result with this given semantic. 
       *   </li>
       * <li>an identifier started with an @: The same as a normal identifier, but in XML output a attribute is created
       *   instead an element.</li>
       * <li>The string "@": The own semantic of the component is used. 
       *   The semantic of the component is either identical with the name of the component, or it is defined in ZBNF
       *   with the construct <code>component::=&lt;?semantic&gt;</code>. This is the regulary case for mostly calls 
       *   of syntax component, especially in ZBNF via the simple <code>&lt;syntax&gt;</code>. 
       *   <br/>
       *   It is possible that the components semantic is null, in this case no parse result item is created for this level.
       *   A null-semantic of the component is given in ZBNF via construct <code>component::=&lt;?&gt;</code>.
       *   For transforming ZBNF a SyntaxPrescript see @see SyntaxPrescript.convertSyntaxDefinition(StringPart spInput)
       *   If not own semantic of the component is defined, </li> 
       * </ul>
       * @param bSkipSpaceAndComment  if true, than white spaces or comments are possible at actual input positions
       *        and should be skipped before test a non-terminate syntax
       *        and should be skipped after test a terminate syntax if its failed.<br>
       * @param parseResultsFromOuterLevel
       * @param addParseResultsFromOuterLevel List of Buffers with a outer parse result, it should be written 
       *        in the parseResult buffer after insertion of parseResult.addAlternative() for this component.<br/>        
      
       * @return true if successfully, false on error.<br/>
       */
      public boolean parseSub
      ( StringPart input
      , String sSemanticForErrorP
      , int resultType
      , String sSemanticForStoring
      , boolean bSkipSpaceAndComment
      , boolean addParseResultsFromOuterLevel
      )
      { PrescriptParser.this.input = input;
        boolean bFound = false;
        sSemanticForError = sSemanticForErrorP;
        @SuppressWarnings("unused")
        String sSemanticIdent1 = sSemanticIdent; //only debug
        if(sSemanticForStoring!= null && sSemanticForStoring.equals("@"))
        { //on calling its written like <name> without semantic, than:
          sSemanticForStoring = syntaxPrescript.getSemantic();  //use it from 
        }
        else
        {  /*the semantic from calling is determinant, it may be also null.*/ 
        }
  
        if(syntaxPrescript.getSemantic()!=null)
        { sSemanticForError = sSemanticForErrorP = syntaxPrescript.getSemantic();
        }
        
        /** Index in parse Result to rewind on error*/
        int idxCurrentStore = -1;
        
        /**Index to restore */
        int idxOfidxWholeParserStoreForInsertionsFromOuterToInnerLevels_save = idxOfidxWholeParserStoreForInsertionsFromOuterToInnerLevels;
        
        /** Index in parseResult of the item of the alternative like [<?semantic>...*/
        int idxStoreAlternativeAndOffsetToEnd;
        if(sSemanticForStoring != null && sSemanticForStoring.equals("@"))
        { //##d
          idxStoreAlternativeAndOffsetToEnd = -1;
        }
        else if(sSemanticForStoring != null && sSemanticForStoring.length()>0)
        { idxCurrentStore = idxStoreAlternativeAndOffsetToEnd = parserStoreInPrescript.addAlternative(sSemanticForStoring, resultType, parentOfParentResultItem);
          parentResultItem = parserStoreInPrescript.getItem(idxCurrentStore);
        }
        else 
        { idxStoreAlternativeAndOffsetToEnd = -1;
          parentResultItem = parentOfParentResultItem;
        }
        
        report.reportln(Report.fineInfo, "parse " + input.getCurrentPosition()+ " " + input.getCurrent(20) + sEmpty.substring(0, nRecursion) + " parseSub semantic=" + sSemanticForStoring + " errormsg=" + sSemanticForError );
        
        //if(parseResultsFromOuterToInnerLevels != null && addParseResultsFromOuterLevel) 
        if(addParseResultsFromOuterLevel) 
        { if(idxOfidxWholeParserStoreForInsertionsFromOuterToInnerLevels < idxWholeParserStoreForInsertionsFromOuterToInnerLevels.length)
          { int idxParserStoreCurrent = parserStoreInPrescript.getNextPosition();
            idxWholeParserStoreForInsertionsFromOuterToInnerLevels[idxOfidxWholeParserStoreForInsertionsFromOuterToInnerLevels]
            = idxParserStoreCurrent;
            parentInWholeParserStoreForInsertionsFromOuterToInnerLevels[idxOfidxWholeParserStoreForInsertionsFromOuterToInnerLevels]
            = parentResultItem;
            idxOfidxWholeParserStoreForInsertionsFromOuterToInnerLevels +=1;
          }
          else
          { stop();   //to many insertions.
          }
        }
        
        if( false && parseResultsFromOuterToInnerLevels != null) 
        { //the parseResultToOtherComponent will be added also, if it will be used.
          if(addParseResultsFromOuterLevel)
          { for(ZbnfParserStore addParseResult : parseResultsFromOuterToInnerLevels) //kk2
            {
              report.reportln(Report.fineInfo, "parse " + input.getCurrentPosition()+ " " + input.getCurrent(20) + sEmpty.substring(0, nRecursion) + " add parseResult " + addParseResult.items.size() + "items, to parseResult-items=" + parserStoreInPrescript.items.size() );
              int idxAddParseResult = parserStoreInPrescript.add(addParseResult, null);  //will be ignored by rewind on error inside this component.
              if(idxCurrentStore == -1)
              { idxCurrentStore = idxAddParseResult;
              }
            }
          }
          report.report(Report.fineInfo, " result: " + parserStoreInPrescript.items.size() + " items.");
        }
  
        int posParseResult = parserStoreInPrescript.getNextPosition();
        long posInput  = input.getCurrentPosition();
        //int nLineInput = input.getLineCt();
        
        int idxAlternative = 0;
        if(syntaxPrescript == null)
          stop();
        //while( !bFound && (syntaxList = syntaxPrescript.getPrescript(idxAlternative)) != null)

        listPrescripts = syntaxPrescript.getListPrescripts();
        boolean bOk = false;
        if(syntaxPrescript.isAlternative())
        { Iterator<ZbnfSyntaxPrescript> iter = listPrescripts.iterator();
          while(!bOk && iter.hasNext())
          { ZbnfSyntaxPrescript alternativePrescript = iter.next();
            SubParser alternativParser = new SubParser(alternativePrescript, this, parentResultItem); //false);
            bOk = alternativParser.parseSub(input, "..|..|.."/*sSemanticForError*/, ZbnfParserStore.kOption, "@", bSkipSpaceAndComment, false);
          }
          
        }
        else
        { if(nReportLevel >= Report.debug) report.reportln(Report.debug, "parse " + input.getCurrentPosition()+ " " + input.getCurrent(20) + sEmpty.substring(0, nRecursion) + " parse(" + nRecursion +") alternative=" + idxAlternative);
          //idxSyntaxList = 0;
          //idxSyntaxListEnd = listPrescripts.size();
          if(listPrescripts == null)
            stop();
          bOk = parsePrescript(bSkipSpaceAndComment); //, parseResult);
        }  
        
        if(bOk)
        { bFound = true;
        }
        else
        {
          input.setCurrentPosition(posInput);
          parserStoreInPrescript.setCurrentPosition(posParseResult);
          idxAlternative +=1;
          if(nReportLevel >= Report.fineDebug)
          { report.reportln(Report.fineDebug, nRecursion
              , "parse Error, reset to: " + input.getCurrent(20)
              + "...... idxResult = " + posParseResult
              + " idxAlternative = " + idxAlternative
              );
          }
        }
        
        if(!bFound)
        { //remove added entries
          parserStoreInPrescript.setCurrentPosition(idxCurrentStore);
          idxOfidxWholeParserStoreForInsertionsFromOuterToInnerLevels = idxOfidxWholeParserStoreForInsertionsFromOuterToInnerLevels_save;
        }

        if(!bFound && syntaxPrescript.isPossibleEmptyOption())
        { bFound = true;
          idxAlternative = -1;  //:TRICKY: to store 0 in parseResult.setAlternativeAndOffsetToEnd
        }
        else if(bFound && idxStoreAlternativeAndOffsetToEnd >=0)
        { if(idxAlternative == 0 && !syntaxPrescript.hasAlternatives())
          { idxAlternative = -1;  //info: there is no alternative.
          }
          else
          { idxAlternative +=1;  //to store 1..n instead 0..n-1, -1 => 0 on empty option.
          }
          parserStoreInPrescript.setAlternativeAndOffsetToEnd(idxStoreAlternativeAndOffsetToEnd, idxAlternative);
          if(resultType == ZbnfParserStore.kOption)
          { String parsedInput = input.substring((int)posInput, (int)input.getCurrentPosition());
            parserStoreInPrescript.setParsedText(idxStoreAlternativeAndOffsetToEnd, parsedInput);
            parserStoreInPrescript.setParsedString(idxStoreAlternativeAndOffsetToEnd, parsedInput.trim());
          }
        }
  
        if(!bFound && idxCurrentStore >=0)
        { report.reportln(Report.fineInfo, "parse " + input.getCurrentPosition()+ " " + input.getCurrent(20) + sEmpty.substring(0, nRecursion) + " error parsing, remove items from result buffer, before=" + parserStoreInPrescript.items.size() );
          report.report(Report.fineInfo, " after=" + parserStoreInPrescript.items.size());  
        }
        return bFound;
      }
  
      
  
      /**This routine is only called inside parseSub at one position. It is used to test if the input is matched 
       * to the current prescript associated with this SubParser in a straigth forward kind. 
       * 
       * @param bSkipSpaceAndComment from level before.
       * @return
       */
      private boolean parsePrescript(boolean bSkipSpaceAndComment) //, ParserStore parseResultP)
      { boolean bOk = true;
        idxPrescript = 0;  //start from first element. Regard that a SubParser instance is used more as one if it is a repetition.
        while(bOk && idxPrescript < listPrescripts.size()) //iterItems.hasNext())
        { 
          if(testSkipSpaceAndComment()){ bSkipSpaceAndComment = true; }
          if(idxPrescript < listPrescripts.size())  //consider spaces on end of prescript.
          { 
            bOk = parseItem(bSkipSpaceAndComment); //, thisParseResult, addParseResult);  //##s
            bSkipSpaceAndComment = false; 
          }  
          if(bOk)
          { idxPrescript +=1; 
          }
        }
        if(bSkipSpaceAndComment)
        { /*:TRICKY: a last possible whitespace is a inaccuracy in the syntax prescript.
            ignore this whitespace. The continuation of the parsing outside
            will might be needed the whitespaceAndComment-Test in a own way.
          */   
        }
        return bOk;
      }    
      
      
      
      /**increments the idxPrescript over all Prescript items of type kSkipSpaces.
       * 
       * @return true if at least one prescript item of type kSkipSpaces is found.
       */
      boolean testSkipSpaceAndComment()
      { boolean bSkipSpaceAndComment = false;
        while(  (idxPrescript < listPrescripts.size())
             && listPrescripts.get(idxPrescript).getType() == ZbnfSyntaxPrescript.kSkipSpaces
             )
        { idxPrescript +=1;
          bSkipSpaceAndComment = true;
        }
        return bSkipSpaceAndComment;
      }
      
      
      
      
      
      /** parses one item. In this method a switch-case to the type of syntaxitem is done.
       * Because a terminal symbol or regular expression may be test with including comment parts, 
       * the input is provided with leading white spaces and comments.  
       * @param iterItems Iterator through the items only used for parseOptions to check the items after the option if set bParseFirstAfterOption
       * @param syntaxItem The syntax item to check with the input position inside this.
       * @param bSkipSpaceAndComment true if white spaces and comments are to skip. 
      */
      /**
       * @param syntaxItem
       * @param bSkipSpaceAndComment
       * @return
       */
      private boolean parseItem(boolean bSkipSpaceAndComment) //, ParserStore parseResult, ParserStore addParseResult)
      { boolean bOk;
        ZbnfSyntaxPrescript syntaxItem = listPrescripts.get(idxPrescript);
        String sConstantSyntax = syntaxItem.getConstantSyntax();
        String sDefinitionIdent = syntaxItem.getDefinitionIdent();
        String sSemanticForStoring = syntaxItem.getSemantic();
        
        @SuppressWarnings("unused")
        String sInput = input.getCurrent(80); //only test.
        
        if(sSemanticForStoring != null && sSemanticForStoring.length()==0)
        { sSemanticForStoring = null; 
        }
        if(sSemanticForStoring != null)
        { sSemanticForError = sSemanticForStoring;//##a
        }
        int maxNrofChars = syntaxItem.getMaxNrofCharsFromComplexItem();
        if(maxNrofChars < -1)
          stop();
        int nType = syntaxItem.getType();
        
        { /*Only for debugging:*/
          if(input.getCurrentPosition()==20352)
            stop();
          
          //if(input.startsWith("\n\r\n/** Konstantedefinitionen"))
          //if(input.startsWith("\r\n#include \"CRuntimeJavalike.h")
          if(input.startsWith("\r\n#include")
            //&& sConstantSyntax != null && sConstantSyntax.equals("#define")
            )
            stop();
        }  
        /** white space and comments are not skipped to provide it to terminal symbols.
         * All sub-syntaxtests are called here in the same kind.
         */
        switch(nType)
        { /*complex syntax constructions: do not parse spaces or comments before,
           *because it is possible that such text parts are used as terminal symbols
           *inside the syntax constructions. 
           */ 
          case ZbnfSyntaxPrescript.kSyntaxDefinition:
          case ZbnfSyntaxPrescript.kAlternative:
          case ZbnfSyntaxPrescript.kSimpleOption:
          case ZbnfSyntaxPrescript.kAlternativeOption:
          case ZbnfSyntaxPrescript.kAlternativeOptionCheckEmptyFirst:
          {
            //bOk = parseOptions(iterItems, syntaxItem, bSkipSpaceAndComment, parseResult);
            bOk = parseOptions(bSkipSpaceAndComment);
          } break;
          case ZbnfSyntaxPrescript.kNegativVariant:
          { bOk = parseNegativVariant(syntaxItem, bSkipSpaceAndComment);
          } break;
          case ZbnfSyntaxPrescript.kRepetition:
          { bOk = parseRepetition(syntaxItem, syntaxItem.getRepetitionBackwardPrescript(), bSkipSpaceAndComment, parserStoreInPrescript);
          } break;
          case ZbnfSyntaxPrescript.kTerminalSymbol:
          { if(bSkipSpaceAndComment)
            { String sTerminalSymbol = syntaxItem.getConstantSyntax();
              bOk = parseWhiteSpaceAndCommentOrTerminalSymbol(sTerminalSymbol, parserStoreInPrescript);
              if(nReportLevel >= Report.debug) report.report(Report.debug, (bOk? " ok: :" : " error: :") + sTerminalSymbol);
            }
            else
            { bOk = parseTerminalSymbol(syntaxItem, parserStoreInPrescript);
            }
          } break;
          case ZbnfSyntaxPrescript.kOnlySemantic:
          { bOk = true;
            if(nReportLevel >= Report.fineInfo) report.reportln(Report.fineInfo, "parse " + input.getCurrentPosition()+ " " + input.getCurrent(20) + sEmpty.substring(0, nRecursion) + " parseSemantic(" + nRecursion + ") <?" + sSemanticForError + ">");
            parserStoreInPrescript.addSemantic(sSemanticForStoring, parentResultItem);
          } break; //do nothing
          case ZbnfSyntaxPrescript.kSyntaxComponent:
          { bOk = parseComponent
                  ( input
                  , sDefinitionIdent
                  , sSemanticForStoring
                  , bSkipSpaceAndComment
                  , syntaxItem.isResultToAssignIntoNextComponent()
                  , syntaxItem.isToTransportOuterResults()
                  , syntaxItem.isToAddOuterResults()
                  );
          } break;
          default:
          {
            //simple Symbols, parse spaces before.
            int posParseResult = parserStoreInPrescript.getNextPosition();
            long posInput  = input.getCurrentPosition();
            if(bSkipSpaceAndComment)
            {
              parseWhiteSpaceAndComment(parserStoreInPrescript);
            }
            String sSrc = null;  //parsed string
            switch(nType)
            {
              case ZbnfSyntaxPrescript.kIdentifier:
              { bOk = parseIdentifier( sConstantSyntax, sSemanticForStoring, parserStoreInPrescript);
              } break;
              case ZbnfSyntaxPrescript.kPositivNumber:
              { bOk = parsePositiveInteger( sSemanticForStoring, maxNrofChars, parserStoreInPrescript);
              } break;
              case ZbnfSyntaxPrescript.kHexNumber:
              { bOk = parseHexNumber( sSemanticForStoring, maxNrofChars, parserStoreInPrescript);
              } break;
              case ZbnfSyntaxPrescript.kIntegerNumber:
              { bOk = parseInteger( sSemanticForStoring, maxNrofChars, parserStoreInPrescript);
              } break;
              case ZbnfSyntaxPrescript.kFloatNumber:
              { bOk = parseFloatNumber( sSemanticForStoring, maxNrofChars, parserStoreInPrescript);
              } break;
              case ZbnfSyntaxPrescript.kStringUntilEndchar:
              { if(nReportLevel >= Report.fineInfo) report.reportln(Report.fineInfo, "parse " + input.getCurrentPosition()+ " " + input.getCurrent(20) + sEmpty.substring(0, nRecursion) + " parse(" + nRecursion + ") <*" + sConstantSyntax + "?" + sSemanticForError + ">");
                if(sConstantSyntax.length() >0)
                { bOk = input.lentoAnyChar(sConstantSyntax, maxNrofChars).found();
                }
                else
                { int maxLen = input.length();
                  if(maxNrofChars < maxLen)
                  { input.lento(maxNrofChars);  //NOTE: if maxNrofChars > length(), an exception will be thrown in StringPart.
                  }
                  bOk = true;  //whole length
                }
                if(bOk)
                { bOk = addResultOrSubsyntax(sSemanticForStoring, syntaxItem.getSubSyntax());
                  /* NOTE: is it necessary to store the line numbers etc?
                  if(input.length()>0 && sSemanticForStoring != null)
                  { parseResult.addString(input, sSemanticForStoring);
                  }
                  */
                  input.fromEnd();
                }
                else
                { input.setLengthMax();
                  saveError("ones of terminate chars \"" + sConstantSyntax + "\" not found <?" + sSemanticForError + ">");
                }
                
              } break;
              case ZbnfSyntaxPrescript.kStringUntilRightEndchar:
              { bOk = parseStringUntilRightEndchar(sConstantSyntax, false, maxNrofChars, sSemanticForStoring, syntaxItem, parserStoreInPrescript);
              } break;
              case ZbnfSyntaxPrescript.kStringUntilRightEndcharInclusive:
              { bOk = parseStringUntilRightEndchar(sConstantSyntax, true, maxNrofChars, sSemanticForStoring, syntaxItem, parserStoreInPrescript);
              } break;
              case ZbnfSyntaxPrescript.kStringUntilEndString: //kk
              { bOk = parseStringUntilEndString(sConstantSyntax, false, maxNrofChars, sSemanticForStoring, syntaxItem, parserStoreInPrescript);
              } break;
              case ZbnfSyntaxPrescript.kStringUntilEndStringInclusive: //kk
              { bOk = parseStringUntilEndString(sConstantSyntax, true, maxNrofChars, sSemanticForStoring, syntaxItem, parserStoreInPrescript);
              } break;
              case ZbnfSyntaxPrescript.kStringUntilEndStringWithIndent:
              { if(nReportLevel >= Report.fineInfo) report.reportln(Report.fineInfo, "parse " + input.getCurrentPosition()+ " " + input.getCurrent(20) + sEmpty.substring(0, nRecursion) + " parse(" + nRecursion + ") <*" + sConstantSyntax + "?" + sSemanticForError + ">");
                sSrc = input.setLengthMax().lentoAnyStringWithIndent(syntaxItem.getListStrings(), syntaxItem.getIndentChars() , maxNrofChars);
                bOk = input.found();
                if(bOk)
                { input.fromEnd();
                
                  if(sSrc.startsWith("Sets the reflection class."))
                    stop();
    
                }
                else
                { input.setLengthMax();  //not found: length() was setted to 0
                  saveError("ones of terminate strings not found" + " <?" + sSemanticForError + ">");
                }
              } break;
              case ZbnfSyntaxPrescript.kStringUntilEndcharOutsideQuotion:
              { bOk = parseNoOrSomeCharsOutsideQuotion(sConstantSyntax, maxNrofChars, sSemanticForStoring, syntaxItem, parserStoreInPrescript);
              } break;
              case ZbnfSyntaxPrescript.kQuotedString:
              { bOk = parseSimpleStringLiteral(sConstantSyntax, maxNrofChars, sSemanticForStoring, syntaxItem, parserStoreInPrescript);
              } break;
              case ZbnfSyntaxPrescript.kRegularExpression:
              { 
                bOk = parseRegularExpression(syntaxItem, sSemanticForStoring, bSkipSpaceAndComment, parserStoreInPrescript);
              } break;
              default:
              {
                if(nReportLevel >= Report.fineInfo) report.reportln(Report.fineInfo, "parse " + input.getCurrentPosition()+ " " + input.getCurrent(20) + sEmpty.substring(0, nRecursion) + " parseSemantic(" + nRecursion + ") <?" + sSemanticForError + ">");
                //parseResult.addSemantic(sSyntax);
                bOk = false;
              }
            }
            if(sSrc != null)  //##ss
            { //something parsed as string, may be also an empty string:
              if(syntaxItem.getSubSyntax()!= null)
              { //##i
                StringPart partOfInput = new StringPart(sSrc);
                bOk = parseComponent(partOfInput, syntaxItem.getSubSyntax(), null, false, false, false, false);
              }
              else if( sSemanticForStoring != null)
              { parserStoreInPrescript.addString(sSrc, sSemanticForStoring, parentResultItem);
              }
            }                
            if(!bOk)
            { //position before parseWhiteSpaceAndComment().
              input.setCurrentPosition(posInput);
              parserStoreInPrescript.setCurrentPosition(posParseResult);
            }
          }
  
        }
        return bOk;
      }
  
      
      
      /**
       * checks a terminal symbol.
       * @param syntaxItem The syntaxitem of a terminal symbol, containing the constant syntax.
       * @param parseResult 
       * @return
       * ##s
       */
      private boolean parseTerminalSymbol(ZbnfSyntaxPrescript syntaxItem, ZbnfParserStore parseResult)
      { boolean bOk;
        long nStart = input.getCurrentPosition();
        int nLineInput = input.getLineCt();
        String sConstantSyntax = syntaxItem.getConstantSyntax();
        //if(sConstantSyntax.equals(";") && input.getCurrentPosition()==28925)
          //stop();
        //if(input.scan(sConstantSyntax).scanOk())
        if(input.startsWith(sConstantSyntax))
        { bOk = true;
          input.seek(sConstantSyntax.length());
          if(bConstantSyntaxAsParseResult)
          { parseResult.addConstantSyntax(sConstantSyntax, nStart, input.getCurrentPosition(), nLineInput, 0, parentResultItem);
          }
          if(nReportLevel >= Report.fineInfo) report.reportln(Report.debug, "parse " + input.getCurrentPosition()+ " " + input.getCurrent(20) + sEmpty.substring(0, nRecursion) + " parse Ok Terminal:" + sConstantSyntax);
        }
        else
        { bOk = false;
          saveError("\"" + sConstantSyntax + "\"");
        }
        return bOk;
      }
  
  
  
      private boolean parseRegularExpression(ZbnfSyntaxPrescript syntaxItem, String sSemanticForStoring, boolean bSkipSpaceAndComment, ZbnfParserStore parseResult)
      { boolean bOk;
        Pattern pattern = syntaxItem.getRegexPatternFromComplexItem(); //Pattern.compile(sSyntax);
        String sSyntax = pattern.pattern();
        if(nReportLevel >= Report.fineInfo) report.reportln(Report.fineInfo, "parse " + input.getCurrentPosition()+ " " + input.getCurrent(20) + sEmpty.substring(0, nRecursion) + " parseRegex(" + nRecursion + ") <!" + sSyntax + "?" + sSemanticForError + ">");
        String sInput = input.getCurrentPart();
        Matcher matcher = pattern.matcher(sInput);
        bOk = true;
        matcher.lookingAt();
        int posEnd = -1;
        try { posEnd = matcher.end();}
        catch(IllegalStateException  exception)
        { bOk = false;
          String sRegexException = exception.getMessage();
          saveError("regex: <!" + sSyntax + "?" + sSemanticForError + "> illegalStateException:" + sRegexException);
        }
        if(bOk)
        { input.lento(posEnd);
          if(sSemanticForStoring != null)
          { parseResult.addString(input, sSemanticForStoring, parentResultItem);
          }
          input.fromEnd();
        }
        return bOk;
      }
  
  
      private boolean parseIdentifier(String addChars, String sSemanticForStoring, ZbnfParserStore parseResult)
      { boolean bOk;
  
        if(nReportLevel >= Report.fineInfo) report.reportln(Report.fineInfo, "parse " + input.getCurrentPosition()+ " " + input.getCurrent(20) + sEmpty.substring(0, nRecursion) + " parseIdentifier(" + nRecursion + ") <$" + "" + "?" + sSemanticForError + ">");
        input.lentoIdentifier(null, addChars);
        if( input.length() > 0)
        { String sIdentifier = input.getCurrentPart();
          if(listKeywords.get(sIdentifier)!=null)
          {
            bOk = false;
            input.setLengthMax();
          }
          else
          { bOk = true;
            if(sSemanticForStoring != null)
            { parseResult.addIdentifier(sSemanticForStoring, sIdentifier, parentResultItem);
            }
            input.fromEnd();
          }
        }
        else
        { bOk = false;
          input.fromEnd();
          saveError("identifier <?" + sSemanticForError + ">");
        }
        return bOk;
      }
  
  
      private boolean parsePositiveInteger(String sSemanticForStoring, int maxNrofChars, ZbnfParserStore parseResult)
      { boolean bOk;
        if(nReportLevel >= Report.fineInfo) report.reportln(Report.fineInfo, "parse " + input.getCurrentPosition()+ " " + input.getCurrent(20) + sEmpty.substring(0, nRecursion) + " parsePosNumber(" + nRecursion + ") <#?" + sSemanticForError + ">");
        if(input.scanPositivInteger().scanOk())
        { bOk = true;
          if(sSemanticForStoring != null)
          { parseResult.addIntegerNumber(sSemanticForStoring, input.getLastScannedIntegerNumber(), parentResultItem);
          }
        }
        else
        { bOk = false;
          saveError("positiv number <?" + sSemanticForError + ">");
        }
        return bOk;
      }
  
  
      private boolean parseHexNumber(String sSemanticForStoring, int maxNrofChars, ZbnfParserStore parseResult)
      { boolean bOk;
        if(nReportLevel >= Report.fineInfo) report.reportln(Report.fineInfo, "parse " + input.getCurrentPosition()+ " " + input.getCurrent(20) + sEmpty.substring(0, nRecursion) + " parseHex(" + nRecursion + ") <#x?" + sSemanticForError + ">");
        if(input.scanHex(maxNrofChars).scanOk())
        { bOk = true;
          if(sSemanticForStoring != null)
          { parseResult.addIntegerNumber(sSemanticForStoring, input.getLastScannedIntegerNumber(), parentResultItem);
          }
        }
        else
        { bOk = false;
          saveError("hex number" + " <?" + sSemanticForError + ">");
        }
        return bOk;
      }
  
  
      private boolean parseInteger(String sSemanticForStoring, int maxNrofChars, ZbnfParserStore parseResult)
      { boolean bOk;
        if(nReportLevel >= Report.fineInfo) report.reportln(Report.fineInfo, "parse " + input.getCurrentPosition()+ " " + input.getCurrent(20) + sEmpty.substring(0, nRecursion) + " parseInt(" + nRecursion + ") <#-?" + sSemanticForError + ">");
        if(input.scanInteger().scanOk())
        {
          bOk = true;
          if(sSemanticForStoring != null)
          { parseResult.addIntegerNumber(sSemanticForStoring, input.getLastScannedIntegerNumber(), parentResultItem);
          }
        }
        else
        { bOk = false;
          saveError("integer number" + " <?" + sSemanticForError + ">");
        }
        return bOk;
      }
  
  
      private boolean parseFloatNumber(String sSemanticForStoring, int maxNrofChars, ZbnfParserStore parseResult)
      { boolean bOk;
        if(nReportLevel >= Report.fineInfo) report.reportln(Report.fineInfo, "parse " + input.getCurrentPosition()+ " " + input.getCurrent(20) + sEmpty.substring(0, nRecursion) + " parseFloat(" + nRecursion + ") <#f?" + sSemanticForError + ">");
        if(input.scanFloatNumber().scanOk())
        {
          bOk = true;
          if(sSemanticForStoring != null)
          { parseResult.addFloatNumber(sSemanticForStoring, input.getLastScannedFloatNumber(), parentResultItem);
          }
        }
        else
        { bOk = false;
          saveError("float number" + " <?" + sSemanticForError + ">");
        }
        return bOk;
      }
  
  
      private boolean parseNoOrSomeCharsOutsideQuotion(String sSyntax, int maxNrofChars, String sSemanticForStoring, ZbnfSyntaxPrescript syntaxItem, ZbnfParserStore parseResult)
      { boolean bOk;
        if(nReportLevel >= Report.fineInfo) report.reportln(Report.fineInfo, "parse " + input.getCurrentPosition()+ " " + input.getCurrent(20) + sEmpty.substring(0, nRecursion) + " parse(" + nRecursion + ") <*\"\"" + sSyntax + "?" + sSemanticForError + ">");
        int len = input.indexOfAnyCharOutsideQuotion(sSyntax, 0, maxNrofChars);
        bOk = input.found();
        if(bOk)
        { if(len >=0)
          { input.lento(len);
            bOk = addResultOrSubsyntax(sSemanticForStoring, syntaxItem.getSubSyntax());
            //if(sSemanticForStoring != null)
            //{ parseResult.addString(input, sSemanticForStoring);
            //}
            input.fromEnd();
          }  
        }
        else
        { saveError("ones of terminate chars \"" + sSyntax + "\" not found <?" + sSemanticForError + ">");
        }
        return bOk;
      }
  
  
      /**parses until one of some endchars from right.
       * 
       * @param sConstantSyntax contains the possible end chars
       * @param bInclusive If true, the end char is parsed inclusive, if false, than exclusive.
       * @param maxNrofChars possible given at left position <123....> or Integer.MAX_VALUE
       * @param sSemanticForStoring The semantic, null if no result should be stored.
       * @param sSubSyntax not null, the name of the inner syntax prescript if there is one.
       * @param parseResult Buffer to store the result. It may be a special buffer or the main buffer.
       * @return
       */
      private boolean parseStringUntilRightEndchar(String sConstantSyntax, boolean bInclusive, int maxNrofChars, String sSemanticForStoring, ZbnfSyntaxPrescript syntaxItem, ZbnfParserStore parseResult)
      { boolean bOk;
        if(nReportLevel >= Report.fineInfo) report.reportln(Report.fineInfo, "parse " + input.getCurrentPosition()+ " " + input.getCurrent(20) + sEmpty.substring(0, nRecursion) + " parse(" + nRecursion + ") <stringtolastinclChar?" + sSemanticForError + ">");
        input.lentoAnyChar(sConstantSyntax, maxNrofChars, StringPart.seekBack);
        bOk = input.found();
        if(bOk)
        { if(bInclusive) 
          { input.lento(input.length()+1);  //inclusive termintated char.
          } 
          if(input.length() >0)
          { bOk = addResultOrSubsyntax(sSemanticForStoring, syntaxItem.getSubSyntax());
          }
          input.fromEnd();
        }
        else
        { input.setLengthMax();
          saveError("ones of terminate chars \"" + sConstantSyntax + "\" not found <?" + sSemanticForError + ">");
        }
        return bOk;
      }
  
      
      /**parses until one of some endchars from right.
       * 
       * @param sConstantSyntax contains the possible end chars
       * @param bInclusive If true, the end char is parsed inclusive, if false, than exclusive.
       * @param maxNrofChars possible given at left position <123....> or Integer.MAX_VALUE
       * @param sSemanticForStoring The semantic, null if no result should be stored.
       * @param sSubSyntax not null, the name of the inner syntax prescript if there is one.
       * @param parseResult Buffer to store the result. It may be a special buffer or the main buffer.
       * @return
       */ //kk
      private boolean parseStringUntilEndString(String sConstantSyntax, boolean bInclusive, int maxNrofChars, String sSemanticForStoring, ZbnfSyntaxPrescript syntaxItem, ZbnfParserStore parseResult)
      { boolean bOk;
        if(nReportLevel >= Report.fineInfo) report.reportln(Report.fineInfo, "parse " + input.getCurrentPosition()+ " " + input.getCurrent(20) + sEmpty.substring(0, nRecursion) + " parse(" + nRecursion + ") <*" + sConstantSyntax + "?" + sSemanticForError + ">");
        { int mode = bInclusive ? StringPart.seekEnd : StringPart.seekNormal;
          input.setLengthMax().lentoAnyString(syntaxItem.getListStrings(), maxNrofChars, mode);
        }  
        bOk = input.found();
        if(bOk)
        { if(bInclusive) 
          { input.lento(input.length()+1);  //inclusive termintated char.
          } 
          bOk = addResultOrSubsyntax(sSemanticForStoring, syntaxItem.getSubSyntax());
          input.fromEnd();
        }
        else
        { input.setLengthMax();  //not found: length() was setted to 0
          String sTerminateStrings = "";
          Iterator<String> iter = syntaxItem.getListStrings().iterator();
          while(iter.hasNext()){ sTerminateStrings += "|" + (String)(iter.next());}
          saveError("ones of terminate strings:"+ sTerminateStrings + " not found" + " <?" + sSemanticForError + ">");
        }
        return bOk;
      }
  

      /**parses a component. This method creates a new SubParser instances and works
       * with an own syntax definition, in ZBNF written as "<code>sDefinitionIdent::=.... .</code>".
       * It is possible to parse in mainstream input, in this case this.input should be given as
       * parameter sInputP, or it is possible to parse a detected part of input, escpecially
       * after parsing "<code>&lt;*...?!sDefinitionIdent&gt;</code>".<br>
       * It is also possible to write the result in the mainstream parse result buffer, or in a extra buffer.
       * @param sInputP input to parse. This may be the mainstream input from this.
       * @param sDefinitionIdent Identifier of the Syntax prescript, written in ZBNF at "sDefinitionIdent::=..."
       * @param sSemanticForStoring If the semantic is specified outside, it is its identifier. Otherwise it is null 
       * @param bSkipSpaceAndComment True if there are spaces in the prescript before this component.
       * @param parserStoreInPrescript Buffer to store the result. It is the buffer of the parent in normally (the main stream buffer),
       *        but it may be also a separate buffer, if the component in the prescript contains <code>&lt;...?-...&gt></code>
       * @param addParseResult If not null, than this Parseresult should be added at start of this component inside the component.
       *        It is a result of pervious parsing with <code>&lt;...?-...&gt></code>, 
       *        stored now because the components prescript has the form <code>&lt;...?+...&gt></code>.
       * @return true if succesfull, false if the input not matches to the prescript. 
       */
      private boolean parseComponent
      ( StringPart sInputP
      , String sDefinitionIdent
      , String sSemanticForStoring
      , boolean bSkipSpaceAndComment
      , boolean bResultToAssignIntoNextComponent
      , boolean bTransferParseResultFromPrevious
      , boolean bAddParseResultFromPrevious
      )
      { boolean bOk;
        if(nReportLevel >= Report.fineInfo) report.reportln(Report.fineInfo, "parse " + input.getCurrentPosition()+ " " + input.getCurrent(20) + sEmpty.substring(0, nRecursion) + " parseComponent(" + nRecursion + ") <" + sDefinitionIdent + "?" + sSemanticForError + ">");
        if(sDefinitionIdent.equals("defineDefinition"))
        { stop();
        
        }
        ZbnfSyntaxPrescript complexItem = searchSyntaxPrescript(sDefinitionIdent);
        if(complexItem != null)
        { /**Create the SubParser-instace: */
          PrescriptParser componentsPrescriptParser;
          SubParser itemParser;
          { //create an own store for results, or use the given store.
            ZbnfParserStore store1;
            ZbnfParseResultItem parentResultItem1;
            if(bResultToAssignIntoNextComponent)
            { /**the parse result of the sub parser should be stored not in main parse result, to transfer to deeper levels. */
              if(parseResultToOtherComponent == null)
              { parseResultToOtherComponent = new ZbnfParserStore();
                if(parseResultsFromOuterToInnerLevels == null)
                { parseResultsFromOuterToInnerLevels = new LinkedList<ZbnfParserStore>();
                }    
                parseResultsFromOuterToInnerLevels.add(parseResultToOtherComponent);
              }
              store1 = parseResultToOtherComponent;
              parentResultItem1 = null;
            }
            else
            { store1 = parserStoreInPrescript;
              parentResultItem1 = parentResultItem;
            }
            componentsPrescriptParser = new PrescriptParser
            ( PrescriptParser.this
            , sDefinitionIdent
            , store1
            , bTransferParseResultFromPrevious? parseResultsFromOuterToInnerLevels : null  //break the transport to components.
            );
            itemParser = componentsPrescriptParser.new SubParser(complexItem, this, parentResultItem1); //bOwnParserStore);
          } 
          
          if(sDefinitionIdent.equals("attribute"))
            stop();
          bOk = itemParser.parseSub
                ( sInputP
                , "<...>" //sSemanticForError
                , ZbnfParserStore.kComponent
                , sSemanticForStoring
                , bSkipSpaceAndComment
                //, parseResult
                , bAddParseResultFromPrevious 
                  //if isToTransport given in syntax (written with <?*..>) than donot bAddParseResultFromPrevious in this level!
                  && !itemParser.syntaxPrescript.isToTransportOuterResults()
                );  //bAddParseResultFromPrevious ? parseResultToOtherComponent : null);
          if(bOk)
          { //parseResult.setOffsetToEnd(posResult); 
            if(nReportLevel >= Report.fineInfo)
            { report.reportln(Report.fineInfo, "parse " + input.getCurrentPosition()+ " " + input.getCurrent(20) + sEmpty.substring(0, nRecursion) + " parseComponent-ok(" + nRecursion + ") <?" + sSemanticForError + ">");
            }
            if(  componentsPrescriptParser.idxOfidxWholeParserStoreForInsertionsFromOuterToInnerLevels >0
              && parseResultsFromOuterToInnerLevels != null
              )
            { componentsPrescriptParser.insertParseResultFromOuterToInnerLevels();
            }
          }
          else
          { if(nReportLevel >= Report.fineInfo)
            { report.reportln(Report.fineInfo, "parse " + input.getCurrentPosition()+ " " + input.getCurrent(20) + sEmpty.substring(0, nRecursion) + " parseComponent-error(" + nRecursion + ") <?" + sSemanticForError + ">");
            }
          }
        }
        else
        { bOk = false;
          report.reportln(Report.error, "parse - Syntaxprescript not found:" + sDefinitionIdent);
          saveError("prescript: " + sDefinitionIdent + "not found. <?" + sSemanticForError + ">");
        }
        return bOk;
      }
  
    
    
    
      
      
      
      private boolean addResultOrSubsyntax(String sSemanticForStoring, String subSyntax)
      { //##ss
        boolean bOk;
        String sSrc = input.getCurrentPart();
        if(subSyntax!= null)
        { //##i
          StringPart partOfInput = new StringPart(sSrc);
          bOk = parseComponent(partOfInput, subSyntax, null, false, false, false, false);
        }
        else if( sSemanticForStoring != null)
        { parserStoreInPrescript.addString(sSrc, sSemanticForStoring, parentResultItem);
          bOk = true;
        }
        else { bOk = true; }
        return bOk;        
      }
      
      
  
      /**
       *
       * @param sQuotionMarks  left and right quotion mark char, typical "" or '', also possible <> or ><
       * @param maxNrofChars
       * @param sSemantic
       * @return
       */
      private boolean parseSimpleStringLiteral(String sQuotionMarks, int maxNrofChars, String sSemanticForStoring, ZbnfSyntaxPrescript syntaxItem, ZbnfParserStore parseResult)
      { boolean bOk;
        if(nReportLevel >= Report.fineInfo) report.reportln(Report.fineInfo, "parse " + input.getCurrentPosition()+ " " + input.getCurrent(20) + sEmpty.substring(0, nRecursion) + " parse(" + nRecursion + ") <\"\"\"?" + sSemanticForError + ">");
        if(input.getCurrentChar() == sQuotionMarks.charAt(0))
        { int len = input.indexEndOfQuotion(sQuotionMarks.charAt(1), 0, maxNrofChars);
          if(len >=2)
          { bOk = true;
            input.seek(1);       //without start quotion mark
            input.lento(len-2);  //without end quotion mark
            bOk = addResultOrSubsyntax(sSemanticForStoring, syntaxItem.getSubSyntax());
            //if(sSemanticForStoring != null)
            //{ parseResult.addString(input, sSemanticForStoring);
            //}
            input.fromEnd().seek(1);  //behind right quotion mark
          }
          else
          { bOk = false;
            saveError("right quotion mark <" + sQuotionMarks + "?" + sSemanticForError + ">");
          }
        }
        else
        { bOk = false;
          saveError("" + sQuotionMarks.charAt(0) + "StingLiteral" + sQuotionMarks.charAt(1) + " <" + sQuotionMarks + "?" + sSemanticForError + ">");
        }
        return bOk;
      }
  
  
      /**
       * If a syntaxItem is given, the terminal symbol (constant syntax) must be found.
       * Otherwise the current position of input is rewind to the start before this
       * method is calling (no whitespaces and comments are skipped!).
       * @param sConstantSyntax The string to test as terminal symbol or null. 
       * @return true if the constant syntax given by syntaxItem is found. 
       *         false if it is not found or syntaxItem == null.
       ##s
       */
      private boolean parseWhiteSpaceAndCommentOrTerminalSymbol(String sConstantSyntax, ZbnfParserStore parseResult)
      { int posParseResult = parseResult.getNextPosition();
        long posInput  = input.getCurrentPosition();
        
        long posCurrent = input.getCurrentPosition();
        boolean bFoundConstantSyntax;  
        boolean bFoundAnySpaceOrComment;
        do  //if once of whitespace, comment or endlinecomment is found, try again.
        { bFoundAnySpaceOrComment = false;
          if(  sConstantSyntax != null 
            && sConstantSyntax.charAt(0) == StringPart.cEndOfText
            && input.length()==0
            )
          { bFoundConstantSyntax = true;
            bFoundAnySpaceOrComment = false;
            if(nReportLevel >= Report.debug) report.reportln(Report.debug, "parse " + input.getCurrentPosition()+ " " + input.getCurrent(20) + sEmpty.substring(0, nRecursion) + " parse Ok EndOfText:");
          }
          else if(sConstantSyntax != null && input.startsWith(sConstantSyntax))
          { bFoundConstantSyntax = true;
            bFoundAnySpaceOrComment = false;
            long nStart = input.getCurrentPosition();
            int nLineInput = input.getLineCt();
            input.seek(sConstantSyntax.length());
            if(bConstantSyntaxAsParseResult)
            { parseResult.addConstantSyntax(sConstantSyntax, nStart, input.getCurrentPosition(), nLineInput, 0, parentResultItem);
            }
            if(nReportLevel >= Report.debug) report.reportln(Report.debug, "parse " + input.getCurrentPosition()+ " " + input.getCurrent(20) + sEmpty.substring(0, nRecursion) + " parse Ok Terminal:" + sConstantSyntax);
          }
          else
          { bFoundConstantSyntax = false;
            input.seekNoChar(sWhiteSpaces);
            long posNew = input.getCurrentPosition();
            if(posNew > posCurrent)
            { bFoundAnySpaceOrComment = true;
              posCurrent = posNew;
            }
            if(!bFoundAnySpaceOrComment && sCommentStringStart != null && input.startsWith(sCommentStringStart))
            { input.lento(sCommentStringEnd, StringPart.seekEnd);
              if(input.length()>0)
              { bFoundAnySpaceOrComment = true;
      
                input.fromEnd();
                posCurrent = input.getCurrentPosition();
              }
            }
      
            if(!bFoundAnySpaceOrComment && sEndlineCommentStringStart != null && input.startsWith(sEndlineCommentStringStart))
            { input.lento("\n", StringPart.seekEnd);
              if(input.length()>0)
              { bFoundAnySpaceOrComment = true;
      
                input.fromEnd();
                posCurrent = input.getCurrentPosition();
              }
            }
          }
        }while(bFoundAnySpaceOrComment);
        if(sConstantSyntax != null && !bFoundConstantSyntax)
        { saveError("\"" + sConstantSyntax + "\"");
          input.setCurrentPosition(posInput);
          parseResult.setCurrentPosition(posParseResult);
        }
        return bFoundConstantSyntax;
      }

      /** Parses at start of an option.
       * 
       * @param options The current syntaxprescript item, a option item.
       * @return
       */
      //private boolean parseOptions(Iterator<ZbnfSyntaxPrescript> iterItems, ZbnfSyntaxPrescript options, boolean bSkipSpaceAndComment, ParserStore parseResult)
      private boolean parseOptions(boolean bSkipSpaceAndComment)
      { boolean bOk = true;
        //boolean bNotFound = true;
        if(nReportLevel >= Report.debug) report.reportln(Report.debug, "parse " + input.getCurrentPosition()+ " " + input.getCurrent(20) + sEmpty.substring(0, nRecursion) + " parse option:" + sSemanticForError);
        ZbnfSyntaxPrescript optionPrescript = listPrescripts.get(idxPrescript);
        int optionType = optionPrescript.getType();
        boolean bParseFirstAfterOption 
        = (optionType == ZbnfSyntaxPrescript.kAlternativeOptionCheckEmptyFirst)
        ;
        
        if(bParseFirstAfterOption)
        {
          int posParseResult = parserStoreInPrescript.getNextPosition();
          long posInput  = input.getCurrentPosition();
          int idxItemOption = idxPrescript;
          idxPrescript +=1;  //after the option
          //if(input.startsWith(")=0;")
            //&& sConstantSyntax.equals("#include")
          //  )
          //  stop();
          
          
          //first try behind option:
          //while(bOk && idxSyntaxList < idxSyntaxListEnd)
          while(bOk && idxPrescript < listPrescripts.size()) //iterItems.hasNext())
          {
            //int idxSyntaxListOnOption = idxSyntaxList; 
            //ZbnfSyntaxPrescript syntaxItem = listPrescripts.get(idxItem); //iterItems.next();
            //ZbnfSyntaxPrescript syntaxItem = listPrescripts.get(idxSyntaxList++);
            if(testSkipSpaceAndComment()){ bSkipSpaceAndComment = true; }
            if(idxPrescript < listPrescripts.size())  //consider spaces on end of prescript.
            { bOk = parseItem(bSkipSpaceAndComment); //, parseResult, null);  //##s
              bSkipSpaceAndComment = false; 
            }  
            if(bOk)
            { //continue parsing after the option prescript item.
              idxPrescript +=1; 
            }
            else
            { //idxSyntaxList = idxSyntaxListOnOption;
            }
          }
          if(bOk)
          { //the prescript is tested until its end without any problem, it means, the option is not used.
            if(nReportLevel >= Report.fineInfo) report.reportln(Report.fineInfo, "parse " + input.getCurrentPosition()+ " " + input.getCurrent(20) + sEmpty.substring(0, nRecursion) + " parse FirstAfterOption -ok:");
          }
          else
          { //error in parsing after option, try it regarding the option!
            input.setCurrentPosition(posInput);
            parserStoreInPrescript.setCurrentPosition(posParseResult);
            idxPrescript = idxItemOption;
            if(nReportLevel >= Report.fineInfo) report.reportln(Report.fineInfo, "parse " + input.getCurrentPosition()+ " " + input.getCurrent(20) + sEmpty.substring(0, nRecursion) + " parse FirstAfterOption -error:");
          }
        }
  
        if(!bParseFirstAfterOption || !bOk)  //##cc
        { //now try the option.
          if(nReportLevel >= Report.fineInfo) report.reportln(Report.fineInfo, "parse " + input.getCurrentPosition()+ " " + input.getCurrent(20) + sEmpty.substring(0, nRecursion) + " try options:");
                    
          SubParser optionParser = new SubParser(optionPrescript, this, parentResultItem); //false);
          bOk = optionParser.parseSub(input, "[...]"/*sSemanticForError*/, ZbnfParserStore.kOption, "@", bSkipSpaceAndComment, false);
          if(!bOk)
          { saveError(" [...]<?" + sSemanticForError + ">");
          }
        } 
        return bOk;
      }
  
  
      private boolean parseNegativVariant(ZbnfSyntaxPrescript options, boolean bSkipSpaceAndComment)
      { boolean bOk = true;
        //boolean bNotFound = true;
        if(nReportLevel >= Report.fineInfo) report.reportln(Report.fineInfo, "parse " + input.getCurrentPosition()+ " " + input.getCurrent(20) + sEmpty.substring(0, nRecursion) + " [?" + sSemanticForError);
        
        //TODO: use a own buffer and trash it. //true/*use own buffer*/);
        //or be careful that the negativParser don't save any parse results.
        SubParser negativParser = new SubParser(options, this, parentResultItem); 
        long posInput  = input.getCurrentPosition();
        bOk = negativParser.parseSub(input, "[?..]"/*sSemanticForError*/, ZbnfParserStore.kOption, "@", bSkipSpaceAndComment, false);
        //always set the current position back to the originator at begin of this parse test.
        input.setCurrentPosition(posInput);

        return !bOk;  //negation, it is not ok if the result matches.
      }
  
  
      private boolean parseRepetition(ZbnfSyntaxPrescript forwardSyntax, ZbnfSyntaxPrescript backwardSyntax, boolean bSkipSpaceAndComment, ZbnfParserStore parseResult)
      { boolean bOk = true;
        boolean bShouldRepeat = true;
        boolean bRepeatContinue;
        int countRepetition = 0;
  
        String sForwardSemantic = forwardSyntax.getSemantic();
        if(sForwardSemantic != null) sSemanticForError = sForwardSemantic;
  
        //String sBackwardSemantic = null;
        //if(backwardSyntax != null) { sBackwardSemantic = backwardSyntax.getSemantic(); }
        if(sForwardSemantic != null) sSemanticForError = sForwardSemantic;
  
        SubParser repeatForwardParser = new SubParser(forwardSyntax, this, parentResultItem); //false);
        SubParser repeatBackwardParser = new SubParser(backwardSyntax, this, parentResultItem); //, false);
  
        long nStartLast = -1;
        if(nReportLevel >= Report.debug) report.reportln(Report.debug, "parse " + input.getCurrentPosition()+ " " + input.getCurrent(20) + sEmpty.substring(0, nRecursion) + " parse repetition:");
        do
        { countRepetition +=1;
          
          long nStart = input.getCurrentPosition();

          if(nStart == 594)
            stop();
          boolean bOkForward;
          
          /*every loop will start with empty parseResultToOtherComponent,
           * otherwise the result is added multiple.
           */
            
          
          if(nStart == nStartLast)
          { bOkForward = false;
            if(nReportLevel >= Report.fineInfo) report.reportln(Report.fineInfo, "parse " + input.getCurrentPosition()+ " " + input.getCurrent(20) + sEmpty.substring(0, nRecursion) + "   parse no repetition because no progress on input");
          }
          else
          { nStartLast = nStart;
            if(nReportLevel >= Report.fineInfo) report.reportln(Report.fineInfo, "parse " + input.getCurrentPosition()+ " " + input.getCurrent(20) + sEmpty.substring(0, nRecursion) + " { parse repetition nr:" + countRepetition);
            repeatForwardParser.init();  //because re-using
            if(parseResultsFromOuterToInnerLevels!=null)
              stop();
            bOkForward = repeatForwardParser.parseSub(input, "{...}"/*sSemanticForError*/, countRepetition, sForwardSemantic, bSkipSpaceAndComment, false);
          }
  
          if(!bOkForward && bShouldRepeat)
          { bOk = false;
            saveError("repetition required because backward-continue is matched."); // + " <?" + sSemanticForError + ">");
          }
  
          if(input.getCurrentPosition() == 1688)
            stop();
          
          if(bOkForward)
          { 
            if(repeatBackwardParser.syntaxPrescript != null)
            { if(nReportLevel >= Report.debug) report.reportln(Report.debug, "parse " + input.getCurrentPosition()+ " " + input.getCurrent(20) + sEmpty.substring(0, nRecursion) + " parse test repetition back:");
              repeatBackwardParser.init();  //because re-using
              bShouldRepeat = bRepeatContinue = repeatBackwardParser.parseSub(input, "{?...}"/*sSemanticForError*/, -countRepetition, "@", bSkipSpaceAndComment, false);
            }
            else
            { bShouldRepeat = false;  //may be or not repeated
              bRepeatContinue = true;       //test the repeat possibility.
              if(nReportLevel >= Report.debug) report.reportln(Report.debug, "parse " + input.getCurrentPosition()+ " " + input.getCurrent(20) + sEmpty.substring(0, nRecursion) + " parse repetition test repeat:");
            }
          }
          else bRepeatContinue = false;
  
          switch(nRecursion)
          { case 0:
             stop(); break;
            case 1:
             stop(); break;
            case 2:
             stop(); break;
            case 3:
             stop(); break;
            case 4:
             stop(); break;
            case 5:
             stop(); break;
            default:
             stop(); break;
          }
        } while(bRepeatContinue);
        if(nReportLevel >= Report.fineInfo) report.reportln(Report.fineInfo, "parse " + input.getCurrentPosition()+ " " + input.getCurrent(20) + sEmpty.substring(0, nRecursion) + " } parse repetition finished, nr:" + countRepetition);
        return bOk;
      }
  
  
      private boolean parseWhiteSpaceAndComment(ZbnfParserStore parseResult)
      { return parseWhiteSpaceAndCommentOrTerminalSymbol(null, parseResult);
      }
  
      /** Sets the rightest position of matched input. Usefull to support the
       * methods getFoundedInputOnError() and getExpectedSyntaxOnError().
       * <br>
       * This method should be called always in inner routine if a test failed.
       * The expected syntax will be added in a string if the same position is tested more as one time. 
       * So the user gets a synopsis of expected syntax at the rightest error position.
       *
       * @param sSyntax The expected syntax.
       */
      private void saveError(String sSyntax)
      { if(input.length() < input.lengthMaxPart())
        { report.reportln(Report.error," saveError: actual length of input is to less");
        }
        if(posRightestError < input.getCurrentPosition())
        { posRightestError = input.getCurrentPosition();
          sRightestError = input.getCurrentPart(80);
          if(input.length()< 80){sRightestError += "<<<<end of file"; }
          sExpectedSyntax = "";
          if(listParseResultOnError != null)
          { int idxStoreEnd = parserStoreInPrescript.items.size();
            int idxStore = idxStoreEnd - maxParseResultEntriesOnError;
            if(idxStore < 0) idxStore = 0;
            listParseResultOnError.clear();
            while(idxStore < idxStoreEnd)
            { listParseResultOnError.add( parserStoreInPrescript.items.get(idxStore) );
              idxStore +=1;
            }
          }  
        }
        if(posRightestError <= input.getCurrentPosition())
        { //the same position, but it is a improvement in syntax prescript.
          sExpectedSyntax += "\n " + sSyntax + " in " + getSemanticTreeForErrorMsg();
        }
        if(nReportLevel >= Report.debug) report.reportln(Report.debug, "parse " + input.getCurrentPosition()+ " " + input.getCurrent(20) + sEmpty.substring(0, nRecursion) + " parseError");
      }
  
      /** gets the tree of semantic while parsing
       *  
       * @return Tree of semantic, every sSemanticForError is divide by a dot. 
       */
      private String getSemanticTreeForErrorMsg()
      { 
        StringBuffer sbSemantic = new StringBuffer(100);
        
        PrescriptParser syntaxPreScript1 = PrescriptParser.this;
        while(syntaxPreScript1 != null)
        {
          { sbSemantic.insert(0, '.');
            sbSemantic.insert(0, syntaxPreScript1.sSemanticIdent);
            syntaxPreScript1 = syntaxPreScript1.parentPrescriptParser;
          }
        }
        
        SubParser subParser = null; //this;
        if(sSemanticForError.startsWith("stringvalue"))
          stop();
        
        while(subParser != null)
        { if(  subParser.sSemanticForError != null
            && !subParser.sSemanticForError.equals("@")
            )
          { sbSemantic.insert(0, '.');
            sbSemantic.insert(0, subParser.sSemanticForError);
          }
          subParser = subParser.parentParser;
        }
        return sbSemantic.toString();        
      }
      
      
      /** Gets the parser Store
       * 
       * @return The parser Store
       */
      public ZbnfParserStore xxxgetParserStore()
      { return parserStoreInPrescript;
      }
  
      public String getDefinitionIdent()
      { return syntaxPrescript.getDefinitionIdent();
      }
  
    }
  
  
  }//class PrescriptParser


  /**To Report something.*/
  protected final Report report;

  protected int nReportLevel = Report.debug;

  /** The list of some sub syntax definitons.*/
  protected final TreeMap<String,ZbnfSyntaxPrescript> listSubPrescript;

  /** Keywords*/
  TreeMap<String,String> listKeywords = new TreeMap<String,String>();

  /** xmlns */
  TreeMap<String,String> xmlnsList = null;
  
  /** Set if constant syntax (terminate morphems) also should stored. See setStoringConstantSyntax()*/
  protected boolean bConstantSyntaxAsParseResult = false;

  protected final PrescriptParser prescriptParserTopLevel;

  protected PrescriptParser.SubParser subParserTopLevel;

  /** The string and position found on the rightest position of an parse fault.
   * It is necessarry to report a parsing error.
   */
  protected String sRightestError = "--noError--";

  /**Required syntax on rightest parsing error position*/
  protected String sExpectedSyntax = "--noError--";
  
  /**founded syntax on rightest parsing error position*/
  protected String xxxsFoundedSyntax = "--noError--";

  /**Maximum number of shown parsing results on error. */
  private int maxParseResultEntriesOnError = 0; 
  
  /**founded content on rightest parsing error position. This list will be filled with current parse result
   * if it is the rightest position. 
   */
  ArrayList<ZbnfParseResultItem> listParseResultOnError  = null;

  /** The string and position found on the rightest position of an parse fault.
   * It is necessarry to report a parsing error.
   */
  protected long posRightestError = 0;

  /** Some mode bits, see m...Mode */
  //private int bitMode;

  /** The start of a comment string, if null than no comment is known. The default value is "/ *" like Java or C.*/
  String sCommentStringStart = "/*";

  /** The end of a comment string, it shoult be set if sCommentStringStart is not null. The default value is "* /" like Java or C.*/
  String sCommentStringEnd   = "*/";

  /** If it is true, the comment is stored in the ParserStore and is supplied by calling
   * getFirstParseResult() and from there calling next().
   */
  boolean bStoreComment = false;

  /** The start of a comment string, if null than no comment is known.*/
  String sEndlineCommentStringStart = "//";

  /**If the syntax prescript contains <code>$inputEncodingKeyword="...".</code> this variable is set.
   * The content are not used inside the parser itself, but may be requested outside.
   */  
  protected String sInputEncodingKeyword;
        
  /**If the syntax prescript contains <code>$inputEncoding="...".</code> this variable is set.
   * The content are not used inside the parser itself, but may be requested outside.
   */  
  protected String sInputEncoding;
        
  /** If it is true, the end-line-comment is stored in the ParserStore and is supplied by calling
   * getFirstParseResult() and from there calling next().
   */
  boolean bStoreEndlineComment = false;

  /** Chars there are detect as white spaces: */
  String sWhiteSpaces = " \t\r\f\n";
  
  /** If it is true, a newline is stored in the ParserStore and is supplied by calling
   * getFirstParseResult() and from there calling next().
   */
  boolean bStoreNewline = false;

  /** If it is true, one space is stored on whitespaces in the ParserStore and is supplied by calling
   * getFirstParseResult() and from there calling next().
   */
  boolean bStoreOneSpaceOnWhitespaces = false;

  /** If it is true, the complete white spaces are stored in the ParserStore and is supplied by calling
   * getFirstParseResult() and from there calling next().
   */
  boolean bStoreWhiteSpaces = false;

  private Charset charsetInput;

  /** Bit in mode. If this bit ist set, all whitespace are overreaded
   * before calling any scan method.
   */
  //protected static final int mSkipOverWhitespace_mode = 0x1;

  /** Bit in mode. If this bit ist set, all comments are overreaded
   * before calling any scan method.
   */
  //protected static final int mSkipOverCommentInsideText_mode = 0x2;

  /** Bit in mode. If this bit ist set, all comments are overreaded
   * before calling any scan method.
   */
  //protected static final int mSkipOverCommentToEol_mode = 0x4;


  /** The actual parse result buffer.*/
  private final ZbnfParserStore parserStore; //parseResult;

  
  /**Creates a empty parser instance. 
   * @param report A report output
   * */
  public ZbnfParser( Report report)
  { this(report, 0);
  }
  
  
  /**Creates a empty parser instance. 
   * @param report A report output
   * @param maxParseResultEntriesOnError if 0 than no parse result is stored.
   *        If >0, than the last founded parse result is stored to support better analysis of syntax errors,
   *        but the parser is slower.
   */
  public ZbnfParser( Report report, int maxParseResultEntriesOnError)
  { this.report = report;
    //parserStore = new ParserStore();
    listSubPrescript = new TreeMap<String,ZbnfSyntaxPrescript>(); //ListPrescripts();
    parserStore = new ZbnfParserStore();
    prescriptParserTopLevel = new PrescriptParser(null, "topLevelSyntax", parserStore, null); 
    this.maxParseResultEntriesOnError = maxParseResultEntriesOnError;
    listParseResultOnError = maxParseResultEntriesOnError >0 
                           ? new ArrayList<ZbnfParseResultItem>(maxParseResultEntriesOnError)
                           : null;
  }


  
  /** Sets the syntax from given string.
   * Further expanations, see <a href="#setSyntax(vishia.StringScan.StringPart)">setSyntax(vishia.StringScan.StringPart)</a>
   * @param syntax The ZBNF-Syntax.
   */
  public void setSyntax(String syntax)
  throws ParseException
  { //StringPart spSyntax = ;
    setSyntax(new StringPart(syntax));
    //listSubPrescript = new ListPrescripts();
    //syntaxPrescript = new SyntaxPrescript(syntax);
  }


  protected void setSyntax(InputStream input)
  throws ParseException
  { 
  }
  
  
  
  /** Sets the syntax from given String.
   * The String should contain the syntax in ZBNF-Format. The string is parsed
   * and converted into a tree of objects of class <code>SyntaxPrescript</code>.
   * The class <code>SyntaxPrescript</code> is private inside the Parser, but its matter of principle may be
   * explained here. <br>
   * The class <code>SyntaxPrescript</code> contains a list of elements (<code>listSyntaxElements</code>)
   * or a list of such <code>listSyntaxElements</code>.
   * The list of <code>listSyntaxElements</code> is used if there are some alternatives. <br>
   * The <code>listSyntaxElements</code> contains
   * objects of type <code>String</code>, <code>SyntaxPrescript</code>,
   * <code>Component</code> or <code>Repetition</code>. It is the sequence of
   * syntax elements of one syntax-path in ZBNF. An object of type <code>String</code> represents a
   * terminal symbol (constant string).
   * An element of <code>SyntaxPrescript</code> is an option construction <code>[...|..|..]</code>
   * or also a simple option <code>[...]</code>. The <code>Repetition</code> represents the
   * <code>{...?...}</code>-construction. A <code>Repetition</code> contains one or two objects
   * of type <code>SyntaxPrescript</code> for the forward and optional backward syntax. This syntax-prescripts
   * may be build complexly in the same way.<br>
   * An object of type <code>Component</code> in the <code>listSyntaxElements</code> represents a construction
   * <code>&lt;...?...></code>. It may contained the semantic information, it may containded a reference
   * to another <code>SyntaxPrescript</code> if there is required in the wise <code>&lt;<i>syntax</i>...</code>.
   * It is also built if a construction of kind <code>&lt;!<i>regex</i>...</code>,
   * <code>&lt;$...</code>, <code>&lt;#...</code> or such else is given.<br>
   * The tree of <code>SyntaxPrescript</code> is passed by syntax test, the right way is searched,
   * see method <a href="#parse(vishia.stringScan.StringPart)">parse()</a>
   *
   * @param syntax The syntax in ZBNF-Format.
   * @throws ParseException If any wrong syntax is containing in the ZBNF-string. A string-wise information
   * of the error location is given.
   */
  public void setSyntax(StringPart syntax)
  throws ParseException
  { ZbnfSyntaxPrescript mainScript = null;

    if(syntax.startsWith("<?SBNF") || syntax.startsWith("<?ZBNF"))
    { syntax.seek("?>", StringPart.seekEnd); 
    }
    while(syntax.seekNoWhitespace().length()>0)
    { String sCurrentInput = syntax.getCurrent(30);
      syntax.scanStart();  //NOTE: sets the restore position for scan error on current position.
      if(sCurrentInput.startsWith("$keywords"))
      { syntax.seek(9);  //TODO skip also over ::=
        if(syntax.startsWith("::=")){ syntax.seek(3);}
        else if(syntax.startsWith("=")){ syntax.seek(1);}
        else throw new ParseException("expected \"=\" behind \"$keywords\"", syntax.getLineCt());
        char cc;
        do
        { syntax.seekNoWhitespace().lentoIdentifier();
          if(syntax.length()>0)
          { //listKeywords.addNew(syntax.getCurrentPart());
            String sKeyword = syntax.getCurrentPart();
            listKeywords.put(sKeyword, sKeyword);
          }
          cc = syntax.fromEnd().seekNoWhitespace().getCurrentChar();
          syntax.seek(1);
        }while(cc == '|');
        if(cc != '.') throw new ParseException("expected \".\" on end of \"$keywords\"", syntax.getLineCt());
      }
      else if(sCurrentInput.startsWith("$setLinemode")) //##s
      { syntax.seek(12); 
        if(syntax.getCurrentChar() == '.')
        { syntax.seek(1);
          setLinemode(true);
        }
        else throw new ParseException("expected \".\" on end of \"$keywords\"", syntax.getLineCt());
      }
      else if(sCurrentInput.startsWith("$endlineComment=")) //##s
      { syntax.seek(16); 
        sEndlineCommentStringStart = syntax.getCircumScriptionToAnyChar(".");
        if(sEndlineCommentStringStart.length()==0){ sEndlineCommentStringStart = null; }
        else if(sEndlineCommentStringStart.length()>5) throw new ParseException("more as 5 chars as $endlineComment unexpected", syntax.getLineCt());
        syntax.seek(1);
      }
      else if(sCurrentInput.startsWith("$comment=")) //##s
      { syntax.seek(9); 
        sCommentStringStart = syntax.getCircumScriptionToAnyChar(".");
        if(sCommentStringStart.length()==0){ sCommentStringStart = null; }
        else if(sCommentStringStart.length()>5) throw new ParseException("more as 5 chars as $endlineComment unexpected", syntax.getLineCt());
        else
        { if(!syntax.startsWith("...")) throw new ParseException("$comment, must have ... betwenn comment strings.", syntax.getLineCt());
          syntax.seek(3);
          sCommentStringEnd = syntax.getCircumScriptionToAnyChar(".");
          if(sCommentStringEnd.length()==0) throw new ParseException("$comment: no endchars found.", syntax.getLineCt());
          else if(sCommentStringEnd.length()>5) throw new ParseException("SyntaxPrescript: more as 5 chars as $endlineComment-end unexpected", syntax.getLineCt());
          syntax.seek(1);  //skip "."
        }
      }
      else if(syntax.scan("$inputEncodingKeyword=").scanOk()) //##s
      { String[] result = new String[1];
        if(  syntax.scanQuotion("\"", "\"", result).scan(".").scanOk()
          //|| syntax.scanIdentifier(result).scanOk().scan(".")
          )
        { sInputEncodingKeyword = result[0];
        }
        else throw new ParseException("$inputEncodingKeyword=",0);
      }
      else if(syntax.scan("$inputEncoding=").scanOk()) //##s
      { String[] result = new String[1];
        if(  syntax.scanQuotion("\"", "\"", result).scan(".").scanOk()
          //|| syntax.scanIdentifier(result).scanOk().scan(".")
          )
        { sInputEncoding = result[0];
          charsetInput = Charset.forName(result[0]); 
        }
        else throw new ParseException("$inputEncodingKeyword=",0);
      }
      else if(sCurrentInput.startsWith("##")) //##s
      { syntax.seek('\n', StringPart.seekEnd); 
      }
      else if(sCurrentInput.startsWith("$main=")) //##s
      { syntax.seek(6); 
        //overwrites a older mainscript, especially the first prescript.
        mainScript = ZbnfSyntaxPrescript.createWithSyntax(syntax, report);
        listSubPrescript.put(mainScript.getDefinitionIdent(), mainScript);
      }
      else if(sCurrentInput.startsWith("$xmlns:")) //##s
      { syntax.seek(7); 
        //overwrites a older mainscript, especially the first prescript.
        String sNamespaceKey = syntax.lento("=").getCurrentPart();
        String sNamespace = syntax.fromEnd().seek(1).lentoQuotionEnd('\"', Integer.MAX_VALUE).getCurrentPart();
        if(sNamespaceKey.length() > 0 && sNamespace.length()>2)
        { if(xmlnsList == null){ xmlnsList = new TreeMap<String, String>(); }
          //NOTE: sNamespace should be have " left and right, do not save it in xmlnsList.
          xmlnsList.put(sNamespaceKey, sNamespace.substring(1, sNamespace.length()-1));
        }
        else throw new ParseException("SyntaxPrescript: $xmlns:ns:\"string\". :failed syntax.", syntax.getLineCt());
        if(syntax.fromEnd().getCurrentChar() == '.')
        { syntax.seek(1);
        }
        else throw new ParseException("SyntaxPrescript: $xmlns:ns:\"string\". :no dot on end.", syntax.getLineCt());
      }
      else
      {
        ZbnfSyntaxPrescript subScript = ZbnfSyntaxPrescript.createWithSyntax(syntax, report);
        if(mainScript == null)
        { mainScript = subScript;  //the first prescript may be the main.
        }
        listSubPrescript.put(subScript.getDefinitionIdent(), subScript);
      }
    }
    subParserTopLevel = prescriptParserTopLevel.new SubParser(mainScript, null, null);  //true);
  }


  /** Set the mode of skipping comments.
   * It it is set, comments are always skipped on every parse operation.
   * This mode may or should be combinded with setIgnoreWhitespace.<br/>
   * @param sCommentStringStart The start chars of comment string, at example '/ *'
   * @param sCommentStringEnd The end chars of comment string, at example '* /'
   * @param bStoreComment If it is true, the comment string will be stored in the ParserStrore
   *                      and can be evaluated from the user.
   */
  public void setSkippingComment
  ( String sCommentStringStart
  , String sCommentStringEnd
  , boolean bStoreComment
  )
  { this.sCommentStringStart = sCommentStringStart;
    this.sCommentStringEnd   = sCommentStringEnd;
    this.bStoreComment = bStoreComment;
  }

  /** Set the mode of skipping comments to end of line.
   * It it is set, comments to end of line are always skipped on every parse operation.
   * This mode may or should be combinded with setIgnoreWhitespace.<br/>
   * @param sCommentStringStart The start chars of comment string to end of line, at example '/ /'
   * @param bStoreComment If it is true, the comment string will be stored in the ParserStrore
   *                      and can be evaluated from the user.
   */
  public void setSkippingEndlineComment
  ( String sCommentStringStart
  , boolean bStoreComment
  )
  { this.sEndlineCommentStringStart = sCommentStringStart;
    this.bStoreComment = bStoreComment;
  }

  /** Sets the chars which are recognized as white spaces. 
   * The default without calling this method is " \t\r\n\f", 
   * that is: space, tab, carrige return, new line, form feed.
   * This mehtod is equal to the using of the syntaxprescript variable $Whitespaces,
   * @see setSyntax(String).
   * @param sWhiteSpaces Chars there are recognize as white space. 
   */ 
  public void setWhiteSpaces(String sWhiteSpaces)
  {
    this.sWhiteSpaces = sWhiteSpaces; 
  }
  
  
  /** Sets the line mode or not. The line mode means, a new line character
   * is not recognize as whitespace, it must considered in syntax prescript
   * as a signifying element.
   * This mehtod is equal to the using of the syntaxprescript variable $setLinemode,
   * @see setSyntax(String).
   * @parameter bTrue if true than set the linemode, false, ignore line structure of the input.
   */  
  public void setLinemode(boolean bTrue)
  { int posNewline = sWhiteSpaces.indexOf('\n');
    if(bTrue && posNewline >= 0)
    { sWhiteSpaces = sWhiteSpaces.substring(0, posNewline) + sWhiteSpaces.substring(posNewline +1); 
    }
    else if(!bTrue && posNewline <0)
    { sWhiteSpaces += '\n'; 
    }
  }
  
  
  
  /**Parsed a given Input and produces a parse result.
   * See <a href="#parse(vishia.StringScan.StringPart)">parse(StringPart)</a>.
   *
   * @param input
   * @return
   */

  public boolean parse(String input)
  {
    StringPart spInput = new StringPart(input);
    return parse(spInput);
  }




  /**parses a given Input and produces a parse result.
   * The method <a href="#setSyntax(vishia.StringScan.StringPart)">setSyntax(vishia.StringScan.StringPart)</a>
   * should be called before.
   * While parsing the pathes in the tree of <code>SyntaxPrescript</code> are tested. If a matching path
   * is found, the method returns true, otherwise false. The result of parsing is stored inside the parser
   * (private internal class ParserStore).
   * To evaluate the parse result see <a href="#getFirstParseResult()">getFirstParseResult()</a>.<br>
   *
   * @param input The source to be parsed.
   * @return true if the input is matched to the syntax, otherwise false.
   */
  public boolean parse(StringPart input)
  { return parse(input, null);
  }
   
  
  /**parses a given Input, see [{@link parse(StringPart)}, but write additional semantic informations
   * into the first parse result (into the top level component).
   * @param input The text to parse
   * @param additionalInfo Pairs of semantic idents and approriate information content. 
   *        The elements [0], [2] etc. contains the semantic identifier 
   *        whereas the elements [1], [3] etc. contains the information content.
   * @return true if the input is matched to the syntax, otherwise false.
   */
  public boolean parse(StringPart input, List<String> additionalInfo)
  { //TODO setIgnore... are not necessary because StringPart.skipWhitespaceAndComment is not used.
    //input.setIgnoreComment( (bitMode & mSkipOverCommentInsideText_mode) != 0);
    //input.setIgnoreEndlineComment( (bitMode & mSkipOverCommentToEol_mode) != 0);
    //input.setIgnoreWhitespaces( (bitMode & mSkipOverWhitespace_mode) != 0);
  	
  	parserStore.item = null;
  	parserStore.items.clear();
  	posRightestError = 0;
  	sExpectedSyntax = null;
  	sRightestError = input.getCurrentPart(80); 
  	
    if(additionalInfo != null)
    { ZbnfParserStore addParseResult = new ZbnfParserStore();
      Iterator<String> iterAdditionalInfo = additionalInfo.iterator();
      while(iterAdditionalInfo.hasNext())
      { String addSemantic = iterAdditionalInfo.next();
        String addContent = iterAdditionalInfo.hasNext() ? iterAdditionalInfo.next() : "";
        addParseResult.addString(addContent, addSemantic, null);
      }
      prescriptParserTopLevel.setparseResultsFromOuterLevels(addParseResult);
    }  
  	String sSemantic = subParserTopLevel.getDefinitionIdent();
    //also create a component of top level.
    boolean bOk = subParserTopLevel.parseSub(input, sSemantic, ZbnfParserStore.kComponent, sSemantic, false, true);
    return bOk;
  }




  private ZbnfSyntaxPrescript searchSyntaxPrescript(String sSyntax)
  { ZbnfSyntaxPrescript foundItem;
    foundItem = (ZbnfSyntaxPrescript)listSubPrescript.get(sSyntax);
    return foundItem;
  }



  /** Reports the syntax.*/
  public void reportSyntax(Report report)
  {
    subParserTopLevel.syntaxPrescript.reportContent(report, 0);
    Iterator<String> iter = listSubPrescript.keySet().iterator();
    while(iter.hasNext())
    { String sName = (String)iter.next();
      ZbnfSyntaxPrescript subSyntax = (ZbnfSyntaxPrescript)listSubPrescript.get(sName);
      report.reportln(Report.fineInfo, 0, "");
      subSyntax.reportContent(report, 0);
    }
  }

  
  
  

  /** Reports the whole content of the parse result. The report is grouped into components.
   * A component is represented by an own syntax presript, written in the current syntax prescript
   * via &lt;ident...>. A new nested component forces a deeper level.<br/>
   * The output is written in the form:<pre>
   * parseResult:  &lt;?semanticIdent> Component
   * parseResult:   &lt;?semanticIdent> ident="foundedString"
   * parseResult:   &lt;?semanticIdent> number=foundedNumber
   * parseResult:  &lt;/?semanticIdent> Component
   * </pre>
   * Every line is exactly one entry in the parsers store. Every component may be evaluated by calling
   * <pre>
   * ParseResultItem item = componentsHeadItem.skipIntoComponent();
   * while( item.isInsideComponent(itemParent)
   * { ...
   * }</pre>
   * and skipping by calling
   * <pre>
   * componentsHeadItem.skipOverComponent();</pre>
   * @see <a href="ParseResultItem.html">interface ParseResultItem</a>.
   * */
  public void reportStore(Report report)
  { report.reportln(Report.fineInfo, 0, "== Report ParserStore ==");
    reportStoreComponent(getFirstParseResult(), report, 1, null);
  }





  /** Inner method to report the content of the parse result
   * @param parseResultItem The first item to report, it is the next item behind componentes first (head-) item, if it is a component.
   * @param report The report system.
   * @param level Level of nested componentes
   * @param parent If not null, the inner items of parent component are reported.
   * @return The number of written lines.
   * */
  @SuppressWarnings("deprecation")
  private int reportStoreComponent(ZbnfParseResultItem parseResultItem, Report report, int level, ZbnfParseResultItem parent)
  { int countLines = 0;
    while(parseResultItem != null)
    { countLines +=1;
      report.reportln(Report.fineInfo, 0, "parseResult: " + sEmpty.substring(0, level) + parseResultItem.getDescription());
      if(parseResultItem.isComponent())
      { //int nLines = 
        reportStoreComponent(parseResultItem.nextSkipIntoComponent(parseResultItem), report, level+1, parseResultItem);
        //if(nLines >1) report.reportln(Report.info, 0, "parseResult: " + sEmpty.substring(0, level) + "</?" + "> Component");
      }
      //parseResultItem = parseResultItem.next();
      parseResultItem = parseResultItem.next(parent);
    }

    return countLines;
  }

  /**Returns the setting of <code>$inputEncodingKeyword="...".</code> in the syntax prescript or null it no such entry is given.
   * @return
   */
  public String getInputEncodingKeyword()
  { return sInputEncodingKeyword;
  }
  
  
  

  

  /**Returns the setting of <code>$inputEncoding="...".</code> in the syntax prescript or null it no such entry is given.
   * @return
   */
  public Charset getInputEncoding()
  { return charsetInput;
  }
  
  
      
      
  
  

  /** Returns the expected syntax on error position. This position is matched
   * to the report of getFoundenInputOnError(). Because the syntax may be differently,
   * much more as a deterministic string is possible, the returned syntax are
   * only one possibility and don't may be non-ambiguous. It may be only a help to detect the error.
   * It is the same problem as error messages by compilers.
   * @return A possible expected syntax.
   */
  public String getExpectedSyntaxOnError()
  { return sExpectedSyntax;
  }

  
  /** Returns the up to now founded result on error position. This position is matched
   * to the report of getFoundenInputOnError() and getExpectedSyntaxOnError().
   * @return A possible founded result or null if this feature is not switched on. 
   */
  public String getLastFoundedResultOnError()
  { String sRet = null;
    if(listParseResultOnError != null)
    { sRet = "";
      Iterator<ZbnfParseResultItem> iter = listParseResultOnError.iterator();
      while(iter.hasNext())
      { ZbnfParseResultItem item = iter.next(); 
        sRet += "\n" + item.getDescription();
      }
    }  
    return sRet;
  }
  

  /** Returns about 50 chars of the input string founded at the parsing
   * error position. If the error position is the end of file or near them,
   * this string ends with the chars "<<<end of file".
   *
   * @return The part of input on error position.
   */
  public String getFoundedInputOnError()
  { return sRightestError;
  }

  
  /** Returns the position of error in input string. 
   * It is the same number as in report.
   */
  public long getInputPositionOnError()
  { return posRightestError;
  }

  
  /**throws a ParseException with the infos of syntax error from last parsing.
   * This method is simple callable if a routine should be aborted on syntax error.
   * Inside a string via @see getSyntaxErrorReport() is build.
   * @param text leading text
   * @throws ParseException immediate.
   */
  protected void throwSyntaxErrorException(String text)
  throws ParseException
  { throw new ParseException(text + getSyntaxErrorReport(),(int)getInputPositionOnError());
  }
  
  
  /** assembles a string with a user readable syntax error message.
   * This method is useable if the user should be inform about the error 
   * and the application should be controlled by the users directives.  
   * 
   * @return String with syntax error message.
   */
  public String getSyntaxErrorReport()
  { String sLastFoundedResultOnError = getLastFoundedResultOnError();
    return "Parse ERROR at input:" 
        + getInputPositionOnError()
        + "(0x" + Long.toString(getInputPositionOnError(),16) + ")"
        + " >>>>" + getFoundedInputOnError()
        + "\nexpected: ----------------------------------------------" + getExpectedSyntaxOnError()
        + ( sLastFoundedResultOnError == null 
          ? "" 
          : "\nfounded before: ----------------------------------------\n" + getLastFoundedResultOnError()
          )
        ;    
  }
  
  
  
  
  
  
  /** Returns the first parse result item to start stepping to the results.
   * See samples at interface ParseResultItem.
   *
   * @return The first parse result item.
   */
  public ZbnfParseResultItem getFirstParseResult()
  {
    if(parserStore.items.size()>0)
    { //parseResult.idxParserStore = 0;
      return (ZbnfParseResultItem)parserStore.items.get(0);
    }
    else return null;
  }


  /** Returns a TreeMap of all xmlns keys and strings.
   * This is the result of detecting $xmlns:ns="string". -expressions in the syntax prescript.
   */
  public TreeMap<String, String> getXmlnsFromSyntaxPrescript()
  { return xmlnsList;
  }
  
  

  /** Determines wether or not constant syntax (teminal syntax items or terminal morphes)
   * should also strored in the result buffer.
   * @param bStore true if they should strored, false if not.
   * @return The old value of this setting.
   */
  public boolean setStoringConstantSyntax(boolean bStore)
  { boolean bOld = bConstantSyntaxAsParseResult;
    bConstantSyntaxAsParseResult = bStore;
    return bOld;
  }


  private void stop()
  {
  }

}
