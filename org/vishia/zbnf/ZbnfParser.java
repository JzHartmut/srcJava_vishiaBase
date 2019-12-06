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
 * @author Hartmut Schorrig www.vishia.org
 * @version See variable sVersion
 *
 ****************************************************************************/
package org.vishia.zbnf;

//import java.io.InputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;




//import org.vishia.util.SortedTreeNode;
import org.vishia.util.Assert;
import org.vishia.util.Debugutil;
import org.vishia.util.StringFunctions;
import org.vishia.util.StringPart;
import org.vishia.util.StringPartScan;
import org.vishia.util.TreeNode_ifc;
import org.vishia.util.TreeNodeBase;
import org.vishia.util.StringPartFromFileLines;
import org.vishia.util.StringFormatter;
import org.vishia.xmlSimple.XmlNode;
import org.vishia.xmlSimple.XmlNodeSimple;
import org.vishia.zbnf.ZbnfParser.PrescriptParser.SubParser;
import org.vishia.zbnf.ZbnfParserStore.ParseResultItemImplement;
import org.vishia.zbnf.ZbnfSyntaxPrescript.EType;
import org.vishia.mainCmd.MainCmdLogging_ifc;


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
 * It is given as a String or {@link StringPartScan}.
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
  
  /**Version, history and license.
   * <ul>
   * <li>2019-10-10: new {@link #setSyntax(CharSequence)} formally with CharSequence instead String, more common useable, especially from new {@link org.vishia.util.FileSystem#readInJar(Class, String, String)}
   * <li>2019-10-10: new {@link #setMainSyntax(String)} not only the first entry can be the the main rule. Used to parse inner Syntax in XML (for IEC 61499).
   * <li>2019-07-07: Some debug possibilities moved or commented.  
   * <li>2019-07-06 Hartmut bugfix: conspicuously on JTtxtcmd-Script some indents where missing (Reflection Generation). The cause was the missing <code>.indent=-3</code>-attribute.
   *   The primary error was using the faulty {@link ZbnfSyntaxPrescript} instance to store the parse result in <code>parseSub(...)</code>.
   *   Because of change in 2019-05 accidentally or experimentally the current used prescript are used for the parse result instead the calling prescript item.
   *   It is now tested with {@link GenZbnfJavaData} and FBCL, that it is not comprehensible for necessity. 
   *   The <code>parentSyntaxItem</code> (calling level) contains the correct semantic (often same as current syntax prescript but not in any case)
   *   and especially for JZtxtcmd indent on texts the attribute for indent, or some more information.   
   * <li>2019-06-29 Hartmut bugfix: If &lt;syntax?"!"textSemantic> is used, the stored text has contained skipped comment and spaces. 
   *   Often only spaces were contained in the parse result, so trim() was a workarround. But comments are contained too, that is not proper.
   *   Yet the order of checking white spaces inclusively parse {@link ZbnfSyntaxPrescript.EType#kTerminalSymbolInComment} is moved before checking
   *   some nested syntax to set the posInput (local variable).    
   * <li>2019-05-29 Hartmut changes for correctly processing &lt;...?"!"@>, The semantic=="@" should be used to get 
   *   the semantic from the syntaxComponent. To do it it is explicitly programmed before calling of {@link PrescriptParser.SubParser#parseSub(ZbnfSyntaxPrescript, String, int, ZbnfSyntaxPrescript, String, ParseResultItemImplement, boolean, ZbnfParserStore)}
   *   Some gardening in arguments.  
   * <li>2019-05-25 Hartmut new possibilities of parsing number: &lt;#8 for any radix, separatorChars in number. 
   * <li>2019-05-22 Hartmut new syntaxItem.bStoreAsString first test success, not ready.
   * <li>2019-05-22 Hartmut improved: Storing parse result with <code>[&lt;?result>...</code> It was faulty in some cases in comparison with ZBNF/testAllConcepts - test.
   *   The result should be stored as {@link ZbnfParserStore.ParseResultItemImplement#parsedString} and not as sInput. 
   *   See changes in {@link ZbnfParserStore.ParseResultItemImplement#getText()}. It should not be stored if a result string is set already 
   *   with <code>{&lt;?semantic=> ...</code> for an empty element per repetition, for example.<br> 
   *   Storing parsed Input on Component: It was done in any case but selten used. It needs memory space. Now the parsed input is only stored 
   *   if new instance {@link #args} {@link Args#bStoreInputForComponent} is true. it can be set either immediately in a JZtxtcmd Script or with public access,
   *   or {@link Args} should be used in a command line environment and set via ctor (TODO) 
   * <li>2019-03-20 Hartmut new: #dbgPosFrom etc {@link #setDebugPosition(int, int, int)} for low level source debugging (Eclipse) on special problems. 
   * <li>2018-09-09 Hartmut only formalistic: instead int kSyntaxDefinition etc. now {@link EType} as enum. It is not a functional change
   * <li>2017-08-27 Hartmut new: {@link #getResultNode()}. To evaluate the result with JZtxtcmd immediately without interim store.
   * <li>2017-03-25 Hartmut new: The {@link ZbnfSyntaxPrescript} syntax item is stored in the {@link ZbnfParserStore.ParseResultItemImplement}. 
   * <li>2017-03-25 Hartmut chg: The line and column of a component parse result is stored immediately. See parseSub(...). 
   *   The change from 2015-06-07: {@link PrescriptParser#srcLineOption} is never used now. The idea in 2015 was: supply position for text indentation
   *   though the < subtext> item was written after a <:> (JZcmd, jzTc), the position of the options may be usefully. But that is a non-simple coherence.
   *   Now the position of a syntax item in the parsed text is written immediately in the parse result. The correction of the indentation is defined by an attribute
   *   in the syntax. See {@link org.vishia.jztxtcmd.JZtxtcmdSyntax} element <code>\\<:\\>< textExpr?.indent=-3></code>
   * <li>2017-01-07 Hartmut bugfix: missing {@link StringPartScan#scanStart()} in {@link PrescriptParser.SubParser#parseTerminalSymbol(ZbnfSyntaxPrescript, org.vishia.zbnf.ZbnfParserStore.ParseResultItemImplement)}.
   *   In follow of that an error in the terminal text shows an faulty position (the position from any scanStart() before). 
   *   An syntax error in the parsed text was not shown with the exact position. 
   * <li>2016-12-02 Hartmut new: The syntax element \W in the syntax script is considered via the StringFunction capability already:
   *    check whether a terminal string ends with a non-identifier character. Check in StringPartScan.scan(text)
   * <li>2015-12-29 Hartmut new: Possibility for debug: Write <code> <...?%...> in Syntax, then {@link ZbnfSyntaxPrescript#bDebugParsing} is set for this item.
   *  It can be tested here to set a specific debug breakpoint for parsing this element. Only for special debugging problems. 
   * <li>2015-07-04 Hartmut bugfix of change on 2015-06-14: It should check kTerminalSymbolInComment if such an symbol is parsed
   *   inside a part with <code><$NoWhiteSpaces></code> 
   * <li>2015-06-14 Hartmut chg: Writes the start of option parsing in log, "Opti" on level 5. Writes the recursion depths in log.
   *   Note: The level {@link #nLevelReportParsing} respectively all source parts "report.report..." outside of {@link LogParsing}
   *   should be removed. They are not reviewed, the usage of {@link LogParsing} is better.  
   * <li>2015-06-14 Hartmut new: distinguishs between {@link ZbnfSyntaxPrescript#kTerminalSymbolInComment} and {@link ZbnfSyntaxPrescript#kTerminalSymbol}.
   * <li>2015-06-07 Hartmut chg: {@link PrescriptParser#srcLineOption} etc. created and filled. If given it is the src position for a component.
   * <li>2015-06-07 Hartmut chg: Improved setting line, column and position in parse result items.
   * <li>2015-06-06 Hartmut chg: Showing components in logfile now from left to right root to special, may be better to read.
   * <li>2015-06-06 Hartmut chg: showing position in String on error additional to line and column, need for error analyzing.
   * <li>2015-06-06 Hartmut chg: {@link SubParser#parseSub(ZbnfSyntaxPrescript, String, int, String, boolean, ZbnfParserStore)}
   *   gets the syntaxPrescript as argument, not a instance variable. Therewith it is not necessary to have an own instance for parsing
   *   some alternative options. Because that is the most frequently parse task, it should save calculation time to do so.
   *   Furthermore {@link ZbnfSyntaxPrescript#kAlternativeOptionCheckEmptyFirst} is handled in the core parser routine
   *   in {@link SubParser#parseSub(ZbnfSyntaxPrescript, String, int, String, boolean, ZbnfParserStore)}
   *   respectively in the new sub routine in parseSub(...) {@link SubParser#parsePrescript(List, boolean)}.
   *   {@link SubParser#idxPrescript} not as instance variable but as stack local variable.
   * <li>2015-06-04 Hartmut chg: Sets line and column of a component from the first read item insert the component, after whiltespaces. 
   *   That is important for JZcmd to detect the indent position of texts.
   * <li>2014-12-14 Hartmut chg: Now returns the line and column and the name of the input file on error if that information are available.
   *   There are available for a {@link org.vishia.util.StringPartFromFileLines} which is used usual as input for the parser.
   *   Changed routines: {@link #getSyntaxErrorReport()}, {@link #getFoundedInputOnError()}, new: {@link #buildFoundedInputOnError()}.
   * <li>2014-06-17 Hartmut new: {@link #setXmlSrcline(boolean)} and {@link #setXmlSrctext(boolean)} to control 
   *   whether srcline="xx" and srctext="text" will be written to a XML output  
   * <li>2014-05-23 Hartmut chg: use {@link StringPart#getLineAndColumn(int[])} instead getLineCt() and {@link StringPart#getCurrentColumn()}
   *   because it is faster. 
   * <li>2014-05-22 Hartmut new: Save srcFile in {@link ZbnfParserStore.ParseResultItemImplement#sFile},
   *   for information in written results, especially with {@link ZbnfJavaOutput}. 
   * <li>2014-03-21 Hartmut new: {@link #setSyntaxFile(File)} and {@link #setSyntaxString(String)}
   *   for ambiguous names called from a JZcmd script.
   * <li>2014-03-21 Hartmut bugfix: Parsing kStringUntilEndStringWithIndent and regular expression: There was a check
   *   'if(sSemanticForStoring != null)' before calling addResultOrSubsyntax(...), therefore 
   *   <code><*{ * }|* /?!test_description></code> has not write the result of the sub syntax.
   *   It is not correct. Originally there was set sSrc and addResultOrSubsyntax(...) was invoked if(sSrc !=null).
   *   That code is reconstructed again. 
   * <li>2014-01-23 Hartmut chg: {@link SubParser#parseSub(StringPartScan, String, int, String, boolean, ZbnfParserStore)}:
   *   Whitespaces skipped before <code>parserStoreInPrescript.addAlternative(...)</code> is called because the position in input
   *   should be stored in the alternative ParseResultItem after the whitespaces. Especially the correct line
   *   should be noted. 
   * <li>2014-01-23 Hartmut chg: {@link SubParser#parseWhiteSpaceAndCommentOrTerminalSymbol(String, ZbnfParserStore)}:
   *   the <code>parseResult</code> argument is used only if <code>sConstantSyntax</code> is not null. Changed consequently.
   *   Now it is possible to invoke this routine with <code>parseWhiteSpaceAndCommentOrTerminalSymbol(null, null)</code>
   *   to only skip white spaces and comments. Therefore {@link SubParser#parseWhiteSpaceAndComment()} is possible
   *   without the 'parseResult' argument. 
   * <li>2014-01-23 Hartmut bugfix: The <code><*{ }></code> for indented lines does not work. Testing and fixing. 
   * <li>2014-01-01 Hartmut new: Line number transfer to parse result items. Idea TODO: transfer the line numbers only
   *   on finish of parsing, store position in input file while parsing: There are some more items stored in the parse process
   *   than remain on finish. Getting line numbers form {@link org.vishia.util.StringPartFromFileLines#getLineAndColumn(column)}
   *   is a binary search process of association position to line numbers. It should only be done on end only for the
   *   remaining parse result items. Time measurement: Parsing of about 30 Headerfiles with line numbers: 15 seconds,
   *   without line numbers: 14 second. 
   * <li>2013-12-06 Hartmut nice fix: trim spaces in $comment and $endlineComment. A user may write white spaces, it didn't recognize comments.
   *   Now white spaces are admissable.
   * <li>2013-09-02 Hartmut TODO forex "[{ <datapath?-assign> = }] cmd " saves the {@link PrescriptParser#parseResultToOtherComponent} of "assign"
   *   because that {@link SubParser#parse_Component(StringPartScan, int, String, String, boolean, boolean, boolean)} is ok. But the outer level "{ ... = }"
   *   fails because the "=" is not present. In this case the {@link PrescriptParser#parseResultToOtherComponent} should be removed if it comes
   *   from an inner SubParser which is not used. The solution should be: The parseResultToOtherComponent should be an attribute of {@link SubParser}
   *   instead the {@link PrescriptParser}, the {@link PrescriptParser} should know it via a List<ParserStore> and all levels of SubParser should 
   *   have a List<ParserStore> for its own or inner Result items for other component. If a SubParser's syntax does not match, all ParserStores, 
   *   inclusive the inner ones, can and should be removed. 
   * <li>2013-02-26 Hartmut bugfix: {@link PrescriptParser#parsePrescript1(String, ZbnfParseResultItem, ZbnfParserStore, ZbnfParserStore, boolean, int)}
   *   while storing {@link ParseResultlet#xmlResult} in {@link #alreadyParsedCmpn}: If the result is empty, the resultlet
   *   should be stored with an xmlResult=null (nothing was created), but the syntax is ok. There are some syntax checks
   *   without result possible.
   * <li>2013-02-12 Hartmut chg: {@link #getResultTree()} returns now the interface reference {@link XmlNode} instead the
   *   implementation instance reference {@link org.vishia.xmlSimple.XmlNodeSimple}. The implementation is the same.
   *   All references are adapted, especially {@link ParseResultlet#xmlResult}
   * <li>2013-01-18 Hartmut chg, new: Log-output improved. New inner class {@link LogParsing}.
   * <li>2013-01-04 Hartmut new {@link #alreadyParsedCmpn}. It may be speed up the parsing process but only if the same component
   *   is requested  at the same position inside another component. It is not used yet. 
   *   Todo: position of text for <syntax?!subsyntax) starts with position 0, it is faulty for that.
   * <li>2012-11-02 Hartmut new local class {@link ParseResultlet}, the {@link PrescriptParser} contains a reference to it.
   *   The resultlet is the first action to save gotten parse results though the result is not convenient in the current context.
   *   This result may be re-used later in another context (not programmed yet, only prepared).
   *   In that context any component's result is converted to an XML tree presentation. This may be the new strategy for parse result storing.
   * <li>2012-10-23 Hartmut Supports <* |endstring: The parse result is trimmed without leading and trailing white spaces.
   * <li>2011-10-10 Hartmut bugfix: scanFloatNumber(true). The parser had an exception because more as 5 floats are parsed and not gotten calling {@link StringPartScan#getLastScannedFloatNumber()}.
   * 
   * <li>2011-01-09 Hartmut corr: Improvement of report of parsing: Not the report level {@link #nLevelReportBranchParsing}
   *     (set with MainCmdLogging_ifc.debug usualy) writes any branch of parsing with ok or error. In that way the working of the parser
   *     in respect to the syntax prescript is able to view. It is if some uncertainty about the correctness of the given syntax is in question. 
   * <li>2011-01-09 Creation of this variable to show the changes in the javadoc.    
   * <li>2010-05-04 Hartmut: corr: sEndlineCommentStringStart: The \n is not included, it will be skipped either as whitespace or it is necessary for the linemode.
   * <li>2009-12-30 Hartmut: corr: Output info: subParserTopLevel == null, no syntax is now removed.
   * <li>2009-08-02 Hartmut: new: parsing with subSyntax now also available in options writing [<?!subSyntax> ...]. 
   * <li>2009-08-02 Hartmut: new: parseExpectedVariant writing [!...] now available. It tests but doesn't processed the content.
   * <li>2009-08-02 Hartmut: new: $Whitespaces= now accepted (it was declared in documentation but not implement). 
   * <li>2009-05-31 Hartmut: corr: some changes of report and error output: In both cases the syntax path is written from inner to root,
   *                           separated with a +. 
   *                           In reportlevel 5 (nLevelReportComponentParsing) also the success of parsing terminal symbols are reported,
   *                           in the same line after 'ok/error Component'. The reporting of parsing process should be improved furthermore.
   * <li>2009-01-16 Hartmut: new: ZbnfSyntaxPrescript.kFloatWithFactor: Writing <#f*Factor?...> is working now..
   *                     corr: Some non-active code parts deleted.
   *                     corr: Processing of parse(... additionalInfo) corrected. It was the only one position, where setparseResultsFromOuterLevels() was used. But more simple is: 
   *                     chg: pass of  ParseResult to components is simplified, in the kind like programmed in 2007. The pass of parse-Results through some components in deeper levels ins't able now, 
   *                          but that feature causes falsity by using.
   * <li>2008-03-28 JcHartmut: The ParserStore is not cleared, only the reference is assigned new.
   *                       So outside the ParserStore can be used from an older parsing.
   * <li>2006-12-15 JcHartmut: regular expressions should be handled after white spaces trimming, error correction.
   * <li>2006-06-00 JcHartmut: a lot of simple problems in developemnt.
   * <li>2006-05-00 JcHartmut: creation
   * </ul>
   */
  public static final String sVersion = "2019-07-07";

  /** Helpfull empty string to build some spaces in strings. */
  static private final String sEmpty = "                                                                                                                                                                                                                                                                                                                          ";

  
  public static class Args {
    boolean bStoreInputForComponent;
  }
  
  public final Args args;

  /*package private*/ final static int mXmlSrcline_xmlWrmode = 0x1, mXmlSrctext_xmlWrmode = 0x2;

  int dbgPosFrom, dbgPosTo, dbgLineSyntax;
  
  
  /** Class to organize parsing of a component with a own prescript.
   *  It is the outer class of the working class {@link ZbnfParser.PrescriptParser.SubParser}.  
   */
  class PrescriptParser
  {
    final ParseResultlet resultlet;
     /**
       * The actual input stream. 
       * By calling parse recursively, a new SubParser instance is created, 
       * but the references to the same input and parse result are assigend. 
       * By using the Parser for another parsing execution, a new input and parseResult is used.
       */
    protected final StringPartScan input;
    
    /**Position where the input String is located in the whole input. */
    protected final int posInputbase;
    
    /**Position in source on the first parsed item in an option, used if a component is used inside the option
     * and only a constant character item is given before.
     */
    protected int srcLineOption = -1;
    protected final int[] srcColumnOption = new int[1];
    protected long srcPosOption;
    
    final PrescriptParser parentPrescriptParser;

    /**The parse result buffer is a own reference in each subParser.  
     * If the parsed component is to be added in the main stream (the normal case), 
     * this pointer points to the parents parseResultBuffer which is the parse result buffer 
     * at first level, the main buffer. 
     * But if the component parsed here is to be added later, 
     * with syntax prescript <code>&lt;...?-...&gt;</code>, 
     * the parseResultBuffer is a local instance here.
     */
    /*cc080318: final*/ ZbnfParserStore parserStoreInPrescript;

    /**An additional parser store to accumulate parse results, which are transfered into the next deeper level of any next component. 
     * If it is used first, it is initialized.
     */
    ZbnfParserStore parseResultToOtherComponent = null;

    /**This array contains some indices to insert the parseResultsFromOuterToInnerLevels for successfull parsed components.
     */
    int[] idxWholeParserStoreForInsertionsFromOuterToInnerLevels = new int[100];  //more as 10 positions are unexpected.
    
    ZbnfParseResultItem[] parentInWholeParserStoreForInsertionsFromOuterToInnerLevels = new ZbnfParseResultItem[100];
    
    final String sSemanticIdent;
    
    
    /**A string representing the parent components. */ 
    final String sReportParentComponents;
    
    protected PrescriptParser(PrescriptParser parent
        , ZbnfSyntaxPrescript syntax
        , String sSemantic, StringPartScan input, int posInputbase /*, cc080318 ZbnfParserStore parseResult*//*, List<ZbnfParserStore> parseResultsFromOuterLevel*/)
    { 
//      if(syntax.sDefinitionIdent.equals("test_description"))
//        Debugutil.stop();
      resultlet = new ParseResultlet(syntax, input.getCurrentPosition());
      //System.out.println("ZbnfParser - PrescriptPaser; " + input.debugString() + syntax.sSemantic);
      parentPrescriptParser = parent;
      
      this.input = input;
      this.posInputbase = posInputbase;
      sSemanticIdent = sSemantic;
      //sReportParentComponents = sSemantic + "+" + (parent != null ? parent.sReportParentComponents : "");
      sReportParentComponents = (parent != null ? parent.sReportParentComponents : "") + "+" + sSemantic;
    }
  
    
    
    
    
    
    /**Parses the syntax-prescript assigned with this class. This routine should be called 
     * only one time with a new instance of SubParser. But some instances of SubParser may be created in nested levels.
     *   
     * The input is given in the constructor, calld from the outer class {@link ZbnfParser}.
     * It is running in a while()-loop. The loop breaks at end of syntax-prescript or
     * if no path in syntax prescript matches to the input. Inside the method 
     * {@link ZbnfParser.PrescriptParser.SubParser#parse_Component(StringPartScan, int, String, String, boolean, boolean, boolean)}
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
     * @throws ParseException 
     */
    public boolean parsePrescript1 //##a
    ( //StringPart input
      String sSemanticForStoring
    , ZbnfSyntaxPrescript parentSyntaxItem  //which invokes the component
    , ZbnfParserStore.ParseResultItemImplement parentResultItem
    , ZbnfParserStore parserStoreInPrescriptP  //either main store, or a temporary if <..?->
    , ZbnfParserStore parseResultsFromOtherComponents          //null in main, or if <..?+..>
    , boolean bSkipSpaceAndComment
    , boolean bDoNotStoreData
    , int nRecursion
    ) throws ParseException
    { //this.input = input;
      this.parserStoreInPrescript = parserStoreInPrescriptP;
      int ixStoreStart = parserStoreInPrescript.items.size();
      int idxRewind = -1; //unused yet.
      if(false && parseResultsFromOtherComponents != null)
      { idxRewind = parserStoreInPrescript.getNextPosition(); 
        parserStoreInPrescript.insert(parseResultsFromOtherComponents, idxRewind, null);
      }
       
      SubParser subParser = new SubParser(null, parentResultItem, bDoNotStoreData, nRecursion); //bOwnParserStore);
      boolean bOk = subParser.parseSub
            ( resultlet.syntaxPrescript
            , "::=" //sSemanticForError
            , ZbnfParserStore.kComponent
            , parentSyntaxItem
            , sSemanticForStoring
            , parentResultItem
            , bSkipSpaceAndComment
            , parseResultsFromOtherComponents
            , nRecursion
            );  
      if(!bOk)
      { //report.reportln(idReportComponentParsing, "                                          --");
        
        if(idxRewind >=0)
        { parserStoreInPrescript.setCurrentPosition(idxRewind);
        }
      }
      if(nReportLevel >= nLevelReportComponentParsing)  
      { log.reportParsing("parseComp ", idReportComponentParsing, resultlet.syntaxPrescript, sReportParentComponents, input, (int)input.getCurrentPosition(), nRecursion, bOk);
      }
      if(bOk){
        if(ixStoreStart < parserStoreInPrescript.items.size()){
          ZbnfParserStore.ParseResultItemImplement parseResultStart = parserStoreInPrescript.items.get(ixStoreStart);
          //Build a part of the XML tree from the start parse result without parent.
          resultlet.xmlResult = builderTreeNodeXml.buildTreeNodeRepresentationXml(null, parseResultStart, true);
        } else {
          //it is possible that the parsing is ok but a parse result is not produced because it is a check only
          //or it has empty options.
          resultlet.xmlResult = null;
        }
        resultlet.endPosText = input.getCurrentPosition();
        String key = String.format("%9d", resultlet.startPosText) + resultlet.syntaxPrescript.sDefinitionIdent;
        alreadyParsedCmpn.put(key, resultlet);
      }
      return bOk;
    }




    /** To parse nested syntax, for every level a subparser is created.
     *
     */
    class SubParser
    {
  
      /** The Prescript of the syntax.
       * The parser instance is useable for more as one parsing execution with the same syntax prescript.
       * */
      //final ZbnfSyntaxPrescript syntaxPrescript;

      /** The Prescript of the syntax.
       * The parser instance is useable for more as one parsing execution with the same syntax prescript.
       * */
      //final ZbnfSyntaxPrescript syntaxPrescript;
  
      /** The index of the current treated item.*/
      //private int idxPrescript;
  
      /** Recursionscounter */
      private final int nRecursion;
  
      /** Counter of tested alternatives. On succesfull parsing it is the number of matched alternative.*/
      int xxxidxAlternative;
      
      /** List of the current alternativ syntax in the prescript.
       * Setted inside parseSub, used in some called methods.
       */
      //List<ZbnfSyntaxPrescript> listPrescripts;
  
      /** Indixes inside syntaxList*/
      //int idxSyntaxList, idxSyntaxListEnd;
      
  
      /** The semantic for getExpectedSyntaxOnError.
       * @see getExpectedSyntaxOnError()
       */
      String sSemanticForError;
      
      final boolean bDoNotStoreData;
      
      /** Pointer to parent, used for build expected syntax on error:*/
      final SubParser parentParser;
      
      /**Reference to the parent parse result item of the parent, given on constructor. */
      final ZbnfParserStore.ParseResultItemImplement parentOfParentResultItem;
  
          
      /**last parsed column in an item. */
      private final int[] srcColumn = new int[1];

      /**last parsed line in an item. */
      private int srcLine;
      
      /** Constructs a new Parser with a given parsed syntaxPrescript and a given input and output buffer.
       * This constructor is used by recursively calling.
       * @param syntax The prescript is a child of the parents' prescript.
       * @param parent Parent level of parsing, for debug and transform parseResultToOtherComponent.
       * @param parserStoreInPrescript typically the same result buffer as parent, but an other buffer, typically
       *        identically with parent.parssResultToOtherComponent, if the result shouldn't be stored in main stream,
       *        used to transform semantic results to a followed item (&lt;..?->-Syntax).
       *        It may be null, than create a new temporary parser result buffer.
       * */
      protected SubParser(/*ZbnfSyntaxPrescript syntax, */SubParser parent
                         , ZbnfParserStore.ParseResultItemImplement parentResultItem, boolean bDoNotStoreData, int nRecursion )
      { //syntaxPrescript = syntax;
        this.parentParser = parent;
        this.bDoNotStoreData = bDoNotStoreData;
        this.parentOfParentResultItem = parentResultItem;
        this.nRecursion = nRecursion; //parent == null ? 0 : parent.nRecursion +1;
      }
  
      /**call if reused. */
      private void init()
      { if(parseResultToOtherComponent != null)
        { parseResultToOtherComponent = null;
        }
      }
  
      /**Parses a syntax-prescript assigned with this class. The prescript may be a syntax component, but also an option or repetition.
       * This routine should be called only one time with a new instance of SubParser. 
       * Some instances of SubParser may be created in nested levels.
       * <br><br>  
       * The input is used from the outer class Parser, aggregation 'input'.
       * It is running in a while()-loop. The loop breaks at end of syntax-prescript or
       * if no path in syntax prescript matches to the input. Inside the method 'parseComplexSyntax()'
       * this method may be called recursively, if the syntax prescript contains &lt;<i>syntax</i>...&gt;.<br/>
       * <br><br>
       * If a semantic for storing is given as input argument sSemanticForStoring, 
       * or the syntax prescript have a semantic itself,
       * a parse result is stored as a component for this sub parsing. 
       * A semantic as input argument is given if this call results of <code>&lt;<i>syntax</i>...&gt;</code>. 
       * A semantic itself is given if the prescript starts with <code>&lt;?<i>syntax</i>&gt;</code> at example in 
       * option brackets <code>[&lt;?<i>syntax</i>&gt;...</code> or repeat brackets <code>{&lt;?<i>syntax</i>&gt;...</code> 
       * or also possible at start of an nested prescript <code>::=&lt;?<i>syntax</i>&gt;...</code>    
       * <br><br>
       * Every time at first the whitespaces and comments are skipped over before the matching of the input
       * to the syntax prescript is tested. The mode of skipping whitespace and comments may be setted
       * by calling setSkippingComment(), setSkippingWhitespace() and setSkippingEndlineComment(), see there.<br>
       * <br><br>
       * If the syntax prescript contains a semantic, this is at example by [&lt;?semantic&gt;...],
       * a parse result is written, containing the semantic and the nr of choice if there are some alternatives.
       * Also an empty option is considered.
       * <br><br>
       * 
       * @param input The input to parse,it is a reference to the same instance as in parent.
       * @param sSemanticForErrorP The semantic superior semantic. This semantic is used for error report.<br/>
       * @param resultType type of the result item of this component. It may one of ParserStore.kOption etc or
       *        a less positiv or negativ number used for repetition and backward repetition. 
       * @param sSemanticForStoring1 The semantic given at the calling item. It may be 
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
       * @param input The input String.
       * @param sSemanticForErrorDefault It is used only if the component has not a semantic.
       * @param resultType The type of Parser Store result, see {@link ZbnfParserStore.kTerminalSymbol} etc.
       *        A positiv number 1... is the count of repetition.
       * @param sSemanticForStoring
       * @param parentResultItem the parent parse result item for a new child.
       * @param bSkipSpaceAndComment  if true, than white spaces or comments are possible at actual input positions
       *        and should be skipped before test a non-terminate syntax
       *        and should be skipped after test a terminate syntax if its failed.<br>
       * @return true if successfully, false on error.<br/>
       * @throws ParseException 
       */
      public boolean parseSub
      ( ZbnfSyntaxPrescript syntaxPrescript
      , String sSemanticForErrorDefault
      , int resultType
      , ZbnfSyntaxPrescript parentSyntaxItem  //which invokes the component
      , String sSemanticForStoring
      , ZbnfParserStore.ParseResultItemImplement parentResultItem
      , boolean bSkipSpaceAndComment
      , ZbnfParserStore parseResultsFromOtherComponents          //if <..?+..>
      , int recursion
      ) throws ParseException
      { boolean bFound = false;
        sSemanticForError = sSemanticForErrorDefault;
        @SuppressWarnings("unused")
        String sSemanticIdent1 = sSemanticIdent; //only debug
//        if(resultType == ZbnfParserStore.kOption)
//          stop();
        final String sSemanticForStoring1;
        if(sSemanticForStoring!= null && sSemanticForStoring.equals("@"))
        { //on calling its written like <name> without semantic, than:
          sSemanticForStoring1 = syntaxPrescript.getSemantic();  //use it from an semantic for option if given.
        }
        else
        {  /*the semantic from calling is determinant, it may be also null.*/
          sSemanticForStoring1 = sSemanticForStoring;
        }
  
        if(syntaxPrescript.getSemantic()!=null)
        { sSemanticForError = syntaxPrescript.getSemantic();
        }

//        if(sSemanticForStoring1 != null && sSemanticForStoring1.equals("CLASS_C"))
//          Assert.stop();

        //int nLineInput = input.getLineCt();
        long posInputForStore;
        long posInput  = input.getCurrentPosition();
//        if(syntaxPrescript !=null && syntaxPrescript.sDefinitionIdent !=null) {
//          if(syntaxPrescript.sDefinitionIdent.equals("textExpr") && posInput == 5933)
//            Debugutil.stop();
//          if(syntaxPrescript.sDefinitionIdent.equals("real_type_name"))
//          Debugutil.stop();
//        }
        if(  bSkipSpaceAndComment 
          && resultType == ZbnfParserStore.kOption  //if another one, it is possible to parse a comment inside the component.
          && sSemanticForStoring1 != null
          )
        { //only if a syntax [<?semantic>x|y|z] is given:
          parseWhiteSpaceAndComment();
          posInputForStore = input.getCurrentPosition();  //it is after whitespaces
          input.setCurrentPosition(posInput); //back because other variants may need the comments
        } else {
          posInputForStore = posInput;
        }
        
        /** Index in parse Result to rewind on error*/
        int idxCurrentStore = -1;
        
        /** Index in parseResult of the item of the alternative like [<?semantic>...*/
        int idxStoreAlternativeAndOffsetToEnd;
        final ZbnfParserStore.ParseResultItemImplement resultItem;  //for the item's result.
        if(parentSyntaxItem !=null && parentSyntaxItem.bDebugParsing) {
          Debugutil.stop();
        }
        if(sSemanticForStoring1 != null && sSemanticForStoring1.length()>0 && ! bDoNotStoreData)
        { 
          srcLine = input.getLineAndColumn(srcColumn);
          String srcFile = input.getInputfile();
          
          //Add the component result item to the parser store. It is not tested yet whether the component is valid.
          //But the elements of the component are stored as child of the component.
          //If the component is not valid, this parse result will be deleted then.
          //The content is stored via parentResultItem later after parsing ok of this component.
          //on 2019-05-30 was written:: The associated syntaxPrescript is necessary to evaluate the parsers result, not the parent.
          //but that is faulty, because the calling item should determine the semantic for storing, not the prescript itself.
          //faulty: resultItem = parserStoreInPrescript.add(sSemanticForStoring1, syntaxPrescript, null, resultType, 0,0, srcLine, srcColumn[0], srcFile, parentOfParentResultItem);
          //Use parentSyntaxItem, it contains the necessary semantic for storing, maybe also especially attributes for indent or other.
          resultItem = parserStoreInPrescript.add(sSemanticForStoring1, parentSyntaxItem, null, resultType, 0,0, srcLine, srcColumn[0], srcFile, parentOfParentResultItem);
          idxCurrentStore = idxStoreAlternativeAndOffsetToEnd = parserStoreInPrescript.items.size() -1; 
          parentResultItem = resultItem; //parserStoreInPrescript.getItem(idxCurrentStore);
        }
        else 
        { idxStoreAlternativeAndOffsetToEnd = -1;
          resultItem = null;  //component without semantic, it has not a resultitem.
          parentResultItem = parentOfParentResultItem;
        }
        
        if(parseResultsFromOtherComponents != null  && ! bDoNotStoreData)
        { int idx1 = parserStoreInPrescript.getNextPosition(); 
          parserStoreInPrescript.insert(parseResultsFromOtherComponents, idx1, parentResultItem);
          if(idxCurrentStore == -1){ idxCurrentStore = idx1; }
        }
        
        if( (log.mLogParse & LogParsing.mLogParseCmpn) !=0  ) { //nLevelReportParsing <= report.getReportLevel()) { 
          String sLog = "parseSub                " + input.getCurrentPosition()+ " " + input.getCurrent(30) + sEmpty.substring(0, nRecursion) + " semantic=" + sSemanticForStoring1 + " errormsg=" + sSemanticForError;
          report.reportln(nLevelReportParsing, sLog );
        }
        int idxAlternative = -1;
        
        //Either the List of all syntax items one after another of this node
        // or List of all alternatives if this is an alternative syntax node.
        boolean bOk = false;
        if(syntaxPrescript.isAlternative()) { //not for [x|y], only for component::= x | y.
          List<ZbnfSyntaxPrescript> listPrescripts = syntaxPrescript.getListPrescripts();
          Iterator<ZbnfSyntaxPrescript> iter = listPrescripts.iterator();
          while(!bOk && iter.hasNext())
          { idxAlternative +=1;
            ZbnfSyntaxPrescript alternativePrescript = iter.next();
            String semanticOfAlternative = alternativePrescript.getSemantic();
            if(semanticOfAlternative !=null) {
              SubParser alternativParser = new SubParser(this, parentResultItem, this.bDoNotStoreData || syntaxPrescript.bDonotStoreData,  nRecursion+1); //false);
              bOk = alternativParser.parseSub(alternativePrescript, "..|..|.."/*sSemanticForError*/, ZbnfParserStore.kOption, null, "@", parentResultItem, bSkipSpaceAndComment, null, recursion+1);
            } else {
              //the alternative has not a special semantic. Parse it without extra level.
              bOk = parsePrescript(alternativePrescript, parentResultItem, bSkipSpaceAndComment, bDoNotStoreData, recursion);
              //SubParser alternativParser = new SubParser(this, parentResultItem, nRecursion+1); //false);
              //bOk = alternativParser.parseSub(alternativePrescript, "..|..|.."/*sSemanticForError*/, ZbnfParserStore.kOption, "@", bSkipSpaceAndComment, null);
            }
            if(nReportLevel >= MainCmdLogging_ifc.fineDebug)
            { report.reportln(MainCmdLogging_ifc.fineDebug
                , "parse Error, reset to:  " + input.getCurrent(30)
                + "...... idxResult = " + parserStoreInPrescript.getNextPosition()
                + " idxAlternative = " + idxAlternative
                );
            }
          }
          int inputPos = (int)input.getCurrentPosition();
          if(dbgPosFrom > 0 && (inputPos = (int)posInput) >= dbgPosFrom && inputPos < dbgPosTo
            //&& (dbgLineSyntax == 0 || dbgLineSyntax == syntaxItem.lineFile)
              ) {
            Debugutil.stop();
          }

        }//isAlternative()
        else
        { /* parse the current sub-prescript: 
          */
          if(nReportLevel >= nLevelReportParsing) report.reportln(idReportParsing, "parse subPrescript;     " + input.getCurrentPosition()+ " " + input.getCurrent(30) + sEmpty.substring(0, nRecursion) + " parse(" + nRecursion +") alternative=" + idxAlternative);
          bOk = parsePrescript(syntaxPrescript, parentResultItem, bSkipSpaceAndComment, bDoNotStoreData, recursion);
        }  
        if(bOk)
        { bFound = true;
        }
        else
        {
        }
        final StringPart.Part parsedInput;
        if(!bFound)
        { //remove the added component if added
          if(idxCurrentStore >=0){ 
            parserStoreInPrescript.setCurrentPosition(idxCurrentStore);
          }
          parsedInput = null;
        } else {
          long posInput1 = input.getCurrentPosition();
          if(  resultItem !=null   //resultitem is existing on any tree node for result, on <syntax> or [<?semantic> 
            && posInputForStore <= posInput1    //any text parsed:
            ) { 
            /* If it is a construct [<?semantic> ...], the parsed input is stored. 
             * An option result item has no more essential informations. 
             * This information helps to evaluate such constructs as [<?semantic>green|red|yellow].
             */
            parsedInput = input.getPart((int)posInputForStore, (int)(input.getCurrentPosition()-posInputForStore));
//            if(resultItem.sSemantic.startsWith("line"))
//              Debugutil.stop();
            if(  resultItem.kind != ZbnfParserStore.kComponent
              && resultItem.parsedString == null  //it is not a constant String stored. 
              ) {
              //it is [<?semantic>....
              resultItem.parsedString = parsedInput.toString();
            } 
            //
            //store parsed String for a component only if wished.
            if(  args.bStoreInputForComponent ) {
              resultItem.sInput = parsedInput.toString();
            }
          } else {
            parsedInput = null;
          }
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
          if( ! bDoNotStoreData) {
            parserStoreInPrescript.setAlternativeAndOffsetToEnd(idxStoreAlternativeAndOffsetToEnd, idxAlternative);
            
            if(resultType == ZbnfParserStore.kOption && parsedInput !=null)
            { /* If it is a construct [<?semantic> ...], the parsed input is stored. 
               * An option result item has no more essential informations. 
               * This information helps to evaluate such constructs as [<?semantic>green|red|yellow].
               */
              //parserStoreInPrescript.setParsedText(idxStoreAlternativeAndOffsetToEnd, parsedInput);
              //parserStoreInPrescript.setParsedString(idxStoreAlternativeAndOffsetToEnd, parsedInput.trim());
              assert(resultItem !=null);  //elsewhere the syntax does not match
//              if(resultItem.sSemantic.equals("line"))
//                Debugutil.stop();
            }
          }
        }
        
        if( syntaxPrescript.sSubSyntax != null)
        { /**If <code>[<?!subsyntax> ... ]</code> is given, execute it! */
          //String parsedInput = input.substring((int)posInputForStore, (int)input.getCurrentPosition());
          int srcLine = parentResultItem.srcLine;
          int srcColumn = parentResultItem.srcColumn;
          long srcPos = parentResultItem.start;
          String srcFile = parentResultItem.sFile;
          bFound = addResultOrSubsyntax(parsedInput, srcPos, srcLine,srcColumn, srcFile, sSemanticForStoring1, parentResultItem, syntaxPrescript.sSubSyntax);
          //bFound = addResultOrSubsyntax(parsedInput, posInputForStore, sSemanticForStoring1, syntaxPrescript.sSubSyntax, input);
        }
        
        if(!bFound && idxCurrentStore >=0)
        { report.reportln(idReportParsing, "parseSub not found;     " + input.getCurrentPosition()+ " " + input.getCurrent(30) + sEmpty.substring(0, nRecursion) + " error parsing, remove items from result buffer, before=" + parserStoreInPrescript.items.size() );
          report.report(idReportParsing, " after=" + parserStoreInPrescript.items.size());  
        }
        return bFound;
      }
  
      
  
      
      
      /**Parse one linear prescript maybe with options to test first after.
       * @param listPrescripts list of the syntax sub prescripts
       * @param bSkipSpaceAndCommentForward true then outside a space skip is set
       * @return true on success.
       * @throws ParseException
       */
      boolean parsePrescript(ZbnfSyntaxPrescript prescriptItem, ZbnfParserStore.ParseResultItemImplement parentResultItem
          , boolean bSkipSpaceAndCommentForward, boolean bDoNotStoreData, int recursion) 
      throws ParseException 
      { List<ZbnfSyntaxPrescript> listPrescripts = prescriptItem.getListPrescripts();
        boolean bSkipSpaceAndComment = bSkipSpaceAndCommentForward;
        boolean bOk = false;
        //save the current positions in input and result to restore if the prescript does not match:
        long posInput  = input.getCurrentPosition();
        int posParseResult = parserStoreInPrescript.getNextPosition();
        //
        //list for fork points only used for right aligned parsing.
        List<ForkPoint> forkPoints = null;  //for option
        boolean backToFork = false;
//        if(parentOfParentResultItem !=null && parentOfParentResultItem.sSemantic.equals("CLASS_C") && sSemanticIdent.equals("headerBlock"))
//          Debugutil.stop();
        int idxPrescript = 0;  //start from first element. Regard that a SubParser instance is used more as one if it is a repetition.
        do { //loop for forkpoint 
          bOk = true;
          while(bOk && idxPrescript < listPrescripts.size()) { //all items of the prescipt
            while(  (idxPrescript < listPrescripts.size())
                 && listPrescripts.get(idxPrescript).getType() == ZbnfSyntaxPrescript.EType.kSkipSpaces
                 )
            { idxPrescript +=1;
              bSkipSpaceAndComment = true;
            }
            if(idxPrescript < listPrescripts.size()) { //consider spaces on end of prescript.
//              if(input.getCurrentPosition()>=6834) 
//                if(StringFunctions.equals(prescriptItem.sDefinitionIdent, "binaryOperator"))
//                  Debugutil.stop();
              ZbnfSyntaxPrescript syntaxItem = listPrescripts.get(idxPrescript); 
              if(syntaxItem.bDebugParsing) {
                Debugutil.stop();
              }
              ZbnfSyntaxPrescript.EType nType = syntaxItem.getType();
              if(nType == ZbnfSyntaxPrescript.EType.kAlternativeOptionCheckEmptyFirst && !backToFork){
                //check whether it matches if this option is ignored.
                //to restore the fork position, store it.
                if(forkPoints == null){ forkPoints = new LinkedList<ForkPoint>(); }
                ForkPoint forkPoint = new ForkPoint(input.getCurrentPosition(), idxPrescript, parserStoreInPrescript.getNextPosition(), bSkipSpaceAndComment);
                forkPoints.add(forkPoint);
                idxPrescript +=1;  //first try skip over this option.
              } else {
                bOk = parseItem(syntaxItem, parentResultItem, bSkipSpaceAndComment, recursion); //, thisParseResult, addParseResult);  //##s
                if(nType == ZbnfSyntaxPrescript.EType.kAlternativeOptionCheckEmptyFirst){
                  backToFork = false;  //for following fork points.
                  if(bOk)
                    Debugutil.stop();
                }
                bSkipSpaceAndComment = false; //after a parsed item, maybe spaces or not depending on syntax 
                if(bOk)
                { idxPrescript +=1; 
                }
              }
            }  
          }
          if(bSkipSpaceAndComment)
          { /*:TRICKY: a last possible whitespace is a inaccuracy in the syntax prescript.
              ignore this whitespace. The continuation of the parsing outside
              will might be needed the whitespaceAndComment-Test in a own way.
            */   
          }
          
          if(!bOk && forkPoints !=null) {
            ForkPoint forkPoint = forkPoints.remove(forkPoints.size()-1);  //remove last
            if(forkPoints.size() == 0){ forkPoints = null; } //no next
            input.setCurrentPosition(forkPoint.posInput);
            parserStoreInPrescript.setCurrentPosition(forkPoint.ixParseResult);
            idxPrescript = forkPoint.ixPrescript;
            bSkipSpaceAndComment = forkPoint.bSkipSpacesAndComment;
            backToFork = true;
          } else {
            backToFork = false; //also if bOk
          }
        } while(backToFork);  
        if(!bOk) {
          input.setCurrentPosition(posInput);
          parserStoreInPrescript.setCurrentPosition(posParseResult);
        }
        return bOk;
      }
      
      
      
      
      /** parses one item. In this method a switch-case to the type of syntaxitem is done.
       * Because a terminal symbol or regular expression may be test with including comment parts, 
       * the input is provided with leading white spaces and comments.  
       * 
       * @param parentResultItem the parent parse result item for a new child.
       * @param bSkipSpaceAndComment true if the calling level determines that white spaces and comments are to skip.
       * @return
       * @throws ParseException
       */
      private boolean parseItem(ZbnfSyntaxPrescript syntaxItem, ZbnfParserStore.ParseResultItemImplement parentResultItem
          , boolean bSkipSpaceAndComment, int recursion) throws ParseException //, ParserStore parseResult, ParserStore addParseResult)
      { boolean bOk;
        String sConstantSyntax = syntaxItem.getConstantSyntax();
        String sDefinitionIdent = syntaxItem.getDefinitionIdent();
        String sSemanticForStoring = syntaxItem.getSemantic();
        
        if(syntaxItem.bDebugParsing) {
          Debugutil.stop();   //possible to set a breakpoint here.
        }
        @SuppressWarnings("unused")
        CharSequence sInput = input.getCurrent(80); //only test.
        if(sSemanticForStoring != null && sSemanticForStoring.length()==0)
        { sSemanticForStoring = null; 
        }
        if(sSemanticForStoring != null)
        { sSemanticForError = sSemanticForStoring;
          if(sSemanticForStoring.equals("semantic"))
            stop();
        }
        int maxNrofChars = syntaxItem.getMaxNrofCharsFromComplexItem();
        if(maxNrofChars < -1)
          stop();
        ZbnfSyntaxPrescript.EType nType = syntaxItem.getType();
        
        final boolean bTerminalFoundInComment;
        
        long posInput  = input.getCurrentPosition();
        String sReport = null;
        if(bSkipSpaceAndComment)
        { final String sConstantText;
          if(nType == ZbnfSyntaxPrescript.EType.kTerminalSymbolInComment){
            sConstantText = syntaxItem.getConstantSyntax();
            sReport = nReportLevel < nLevelReportBranchParsing ? ""
              : "parsSpace " + input.getCurrentPosition()+ " " + inputCurrent(input); 
          } else {
            sConstantText = null;
          }
          bOk = bTerminalFoundInComment = parseWhiteSpaceAndCommentOrTerminalSymbol(sConstantText, parentResultItem);
        } else { 
          bOk = bTerminalFoundInComment = false; 
        }
        if(!bTerminalFoundInComment) {
	        {  /*Only for debugging:*/
	          if(input.getCurrentPosition()==704)
	            stop();
	          
	          //if(input.startsWith("\n\r\n/** Konstantedefinitionen"))
	          //if(input.startsWith("\r\n#include \"CRuntimeJavalike.h")
	          if(input.startsWith("\r\n#include")
	            //&& sConstantSyntax != null && sConstantSyntax.equals("#define")
	            )
	            stop();
	        }  
	        long posInput2  = input.getCurrentPosition();  //after space and comment
	        //Set the current position before the skipped comment, because the following sub syntax may expect kTerminalSymbolInComment.
	        //It should be able to found. 
	        input.setCurrentPosition(posInput);
	        posInput = posInput2; 
	        { int inputPos;
	          if(dbgPosFrom > 0 && (inputPos = (int)posInput) >= dbgPosFrom && inputPos <= dbgPosTo
	            && (dbgLineSyntax == 0 || dbgLineSyntax == syntaxItem.lineFile)
	              ) {
	            Debugutil.stop();
	          }
	        }
	        if((log.mLogParse & LogParsing.mLogParseItem)!=0) {
	          String sLog = "" + input.getLineAndColumn(null) + ":"+ input.getCurrent(20) + "-?-" + syntaxItem.lineFile;
	          ZbnfSyntaxPrescript syntaxParent = syntaxItem;
	          do {
	            sLog +=  " - " + syntaxParent.sDefinitionIdent;
	            syntaxParent = syntaxParent.parent;
	          } while(syntaxParent !=null && sLog.length() < 120);
	          report.reportln(3, sLog);
	        }
	        
	        /** white space and comments are not skipped to provide it to terminal symbols.
	         * All sub-syntaxtests are called here in the same kind.
	         */
	        switch(nType)
	        { /*complex syntax constructions: do not parse spaces or comments before,
	           *because it is possible that such text parts are used as terminal symbols
	           *inside the syntax constructions. 
	           */ 
	          case kSyntaxDefinition:
	          case kAlternative:
	          case kSimpleOption:
	          case kAlternativeOption:
	          case kAlternativeOptionCheckEmptyFirst:
	          { if(nReportLevel >= nLevelReportBranchParsing){ 
	              log.reportParsing("Opti " + input.getCurrentPosition()+ " " + inputCurrent(input), idReportBranchParsing, syntaxItem, sReportParentComponents, input, (int)input.getCurrentPosition(), nRecursion, true); 
	            }
	            bOk = parseOptions(syntaxItem, parentResultItem, bSkipSpaceAndComment, recursion);
	          } break;
	          case kNegativVariant:
	          { bOk = parseNegativVariant(syntaxItem, bSkipSpaceAndComment, recursion);
	          } break;
	          case kUnconditionalVariant:
	          { bOk = parseUnconditionalVariant(syntaxItem, parentResultItem, bSkipSpaceAndComment, recursion);
	          } break;
	          case kExpectedVariant:
	          { bOk = parseExpectedVariant(syntaxItem, bSkipSpaceAndComment, recursion);
	          } break;
	          case kRepetition:
	          { bOk = parseRepetition(syntaxItem, syntaxItem.getRepetitionBackwardPrescript(), parentResultItem, bSkipSpaceAndComment, recursion);
	          } break;
	          case kOnlySemantic:
	          { bOk = true;
	            if(nReportLevel >= nLevelReportParsing) report.reportln(idReportParsing, "parseItem only Semantic;" + input.getCurrentPosition()+ " " + input.getCurrent(30) + sEmpty.substring(0, nRecursion) + " parseSemantic(" + nRecursion + ") <?" + sSemanticForError + ">");
	            if( ! bDoNotStoreData ) {
	              srcLine = input.getLineAndColumn(srcColumn);  //TODO optimize input position, usual from component start.
	              String srcFile = input.getInputfile();
	              parserStoreInPrescript.addSemantic(sSemanticForStoring, syntaxItem, parentResultItem, srcLine, srcColumn[0], srcFile);
	            }
	          } break; //do nothing
	          case kSyntaxComponent:
	          { 
	            ZbnfSyntaxPrescript syntaxCmpn = searchSyntaxPrescript(sDefinitionIdent);
	            if(syntaxCmpn == null) {
	              bOk = false;
	              report.reportln(MainCmdLogging_ifc.error, "parse - Syntaxprescript not found:" + sDefinitionIdent);
	              String sError = "prescript for : <" + sDefinitionIdent 
	              + ((!sSemanticForError.equals("@") && !sSemanticForError.equals("?") ) ? "?" + sSemanticForError : "")
	              + "> not found.";
	              saveError(sError);
	            } else {
	              if(sSemanticForStoring !=null && sSemanticForStoring.equals("@")) {
	                //replace single @ with semantic of component. 
	                sSemanticForStoring = syntaxCmpn.getSemantic();
	              }
	              
	              
	              bOk = parse_Component
	                    ( input
	                    , posInputbase    
	                    , syntaxCmpn
	                    , sSemanticForStoring
	                    , parentResultItem
	                    , bSkipSpaceAndComment
	                    , this.bDoNotStoreData || syntaxItem.bDonotStoreData
	                    , syntaxItem       //which invokes the component
	                    , syntaxItem.isResultToAssignIntoNextComponent()
	                    , syntaxItem.isToAddOuterResults()
	                    );
	            }
	          } break;
	          default:
	          {
	            //simple Symbols
	            //Because kTerminalSymbolInComment is already tested, the skipped spaces and comment should be skipped really:
	            input.setCurrentPosition(posInput);  //Use the stored input position after space and comment.
	            int posParseResult = parserStoreInPrescript.getNextPosition();
	//Old code shifted, todo remove here:            
	//            final boolean bTerminalFoundInComment;
	//            
	//            String sReport = null;
	//            if(bSkipSpaceAndComment)
	//            { final String sConstantText;
	//              if(nType == ZbnfSyntaxPrescript.EType.kTerminalSymbolInComment){
	//                sConstantText = syntaxItem.getConstantSyntax();
	//                sReport = nReportLevel < nLevelReportBranchParsing ? ""
	//                  : "parsSpace " + input.getCurrentPosition()+ " " + inputCurrent(input); 
	//              } else {
	//                sConstantText = null;
	//              }
	//              bOk = bTerminalFoundInComment = parseWhiteSpaceAndCommentOrTerminalSymbol(sConstantText, parentResultItem);
	//            } else { 
	//              bOk = bTerminalFoundInComment = false; 
	//            }
	//            if(!bTerminalFoundInComment){
	              assert(!bOk);  //it is set to false in branch above.
	              CharSequence sSrc = null;  //parsed string
	              //either set bOk to true or sSrc to !=null if successfully parsed.
	              //
	              sReport = nReportLevel < nLevelReportBranchParsing ? ""
	                : "item " + input.getCurrentPosition()+ " " + inputCurrent(input); // + sEmpty.substring(0, nRecursion); 
	
	              //int posSrc = -1;     //position of the string
	              switch(nType)
	              {
	              case kTerminalSymbolInComment:  //Note: important if bSkipSpaceAndComment = false. 
	              case kTerminalSymbol: 
	              { bOk = parseTerminalSymbol(syntaxItem, parentResultItem);
	              } break;
	              case kIdentifier:
	              { bOk = parseIdentifier( sConstantSyntax, sSemanticForStoring, parentResultItem);
	              } break;
	              case kPositivNumber:
	              { bOk = parsePositiveInteger( syntaxItem, parentResultItem, maxNrofChars);
	              } break;
	              case kHexNumber:
	              { bOk = parseHexNumber( syntaxItem, parentResultItem, maxNrofChars);
	              } break;
	              case kNumberRadix:
	              { bOk = parseNumberRadix( syntaxItem, parentResultItem, maxNrofChars);
	              } break;
	              case kIntegerNumber:
	              { bOk = parseInteger( syntaxItem, parentResultItem, maxNrofChars);
	              } break;
	              case kFloatNumber:
	              case kFloatWithFactor:
	              { bOk = parseFloatNumber( syntaxItem, maxNrofChars, parentResultItem);
	              } break;
	              case kStringUntilEndchar:
	              { if(sSemanticForStoring!=null && sSemanticForStoring.equals("TESTrest"))
	                stop();
	                if(nReportLevel >= nLevelReportParsing) report.reportln(idReportParsing, "parseItem StrTilEndchar;" + input.getCurrentPosition()+ " " + input.getCurrent(30) + sEmpty.substring(0, nRecursion) + " parse(" + nRecursion + ") <*" + sConstantSyntax + "?" + sSemanticForError + ">");
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
	                { sSrc = input.getCurrentPart();
	                }
	                else
	                { input.setLengthMax();
	                  saveError("ones of terminate chars \"" + sConstantSyntax + "\" not found <?" + sSemanticForError + ">");
	                }
	
	              } break;
	              case kStringUntilRightEndchar:
	              { bOk = parseStringUntilRightEndchar(sConstantSyntax, false, maxNrofChars, sSemanticForStoring, parentResultItem, syntaxItem);
	              } break;
	              case kStringUntilRightEndcharInclusive:
	              { bOk = parseStringUntilRightEndchar(sConstantSyntax, true, maxNrofChars, sSemanticForStoring, parentResultItem, syntaxItem);
	              } break;
	              case kStringUntilEndString: //kk
	              { bOk = parseStringUntilTerminateString(sConstantSyntax, false, false, maxNrofChars, sSemanticForStoring, parentResultItem, syntaxItem);
	              } break;
	              case kStringUntilEndStringTrim: //kk
	              { bOk = parseStringUntilTerminateString(sConstantSyntax, false, true, maxNrofChars, sSemanticForStoring, parentResultItem, syntaxItem);
	              } break;
	              case kStringUntilEndStringInclusive: //kk
	              { bOk = parseStringUntilTerminateString(sConstantSyntax, true, false, maxNrofChars, sSemanticForStoring, parentResultItem, syntaxItem);
	              } break;
	              case kStringUntilEndStringWithIndent:
	              { if(nReportLevel >= nLevelReportParsing) report.reportln(idReportParsing, "parseItem-StrTilEndInde;" + input.getCurrentPosition()+ " " + input.getCurrent(30) + sEmpty.substring(0, nRecursion) + " parse(" + nRecursion + ") <*" + sConstantSyntax + "?" + sSemanticForError + ">");
	                StringBuilder buffer = new StringBuilder();
	                input.setLengthMax().lentoAnyStringWithIndent(syntaxItem.getListStrings().toArray(new String[1]), syntaxItem.getIndentChars() , maxNrofChars, buffer);
	                if(input.found()) {
	                  sSrc = buffer;
	                }
	                else
	                { input.setLengthMax();  //not found: length() was setted to 0
	                  saveError("ones of terminate strings not found" + " <?" + sSemanticForError + ">");
	                }
	              } break;
	              case kStringUntilEndcharOutsideQuotion:
	              { bOk = parseNoOrSomeCharsOutsideQuotion(sConstantSyntax, maxNrofChars, sSemanticForStoring, parentResultItem, syntaxItem);
	              } break;
	              case kQuotedString:
	              { bOk = parseSimpleStringLiteral(sConstantSyntax, maxNrofChars, sSemanticForStoring, parentResultItem, syntaxItem);
	              } break;
	              case kRegularExpression:
	              { 
	                bOk = parseRegularExpression(syntaxItem, sSemanticForStoring, parentResultItem, bSkipSpaceAndComment);
	              } break;
	              default:
	              {
	                if(nReportLevel >= nLevelReportParsing) {
	                  report.reportln(idReportParsing, "parseItem-default;      " + input.getCurrentPosition()+ " " + input.getCurrent(30) + sEmpty.substring(0, nRecursion) + " parseSemantic(" + nRecursion + ") <?" + sSemanticForError + ">");
	                }
	                //parseResult.addSemantic(sSyntax);
	                bOk = false;
	              }
	              }//switch
	              //
	              //check whether anything was parsed stored in sSrc:
	              //then input refers to the start position of the parsed one with its current part.
	              //
	            	if(sSrc != null)
	              { //something parsed as string, may be also an empty string:
	                if(StringFunctions.startsWith(sSrc,"First line"))
	                  stop();
	                long posResult = input.getCurrentPosition();
	                int srcLine = input.getLineAndColumn(srcColumn);  //, srcLine, srcColumn[0], input.getInputfile()
	                bOk = addResultOrSubsyntax(sSrc, posResult, srcLine, srcColumn[0], input.getInputfile(), sSemanticForStoring, parentResultItem, syntaxItem.getSubSyntax());
	                //posSrc = (int)input.getCurrentPosition();
	                input.fromEnd();
	              }                
	//            }//if bTerminalFound
	            if(!bOk)
	            { //position before parseWhiteSpaceAndComment().
	              input.setCurrentPosition(posInput);
	              parserStoreInPrescript.setCurrentPosition(posParseResult);
	            }
	            if(nReportLevel >= nLevelReportBranchParsing)    
	            { log.reportParsing(sReport, idReportBranchParsing, syntaxItem, sReportParentComponents, input, (int)posInput, nRecursion, bOk);
	            }
	          }//default
	        }//switch
        } //bTerminalFoundInComment, bOk is true
        if(bOk) {
          if(syntaxItem.bStoreAsString && ! bDoNotStoreData) {
            srcLine = input.getLineAndColumn(srcColumn);
            String srcFile = input.getInputfile();
            int zChars = (int)(input.getCurrentPosition() - posInput);
            CharSequence sParsedString = input.getPart((int)posInput, zChars);
            //Add the component result item to the parser store. It is not tested yet whether the component is valid.
            //But the elements of the component are stored as child of the component.
            //If the component is not valid, this parse result will be deleted then.
            parserStoreInPrescript.add(sSemanticForStoring, syntaxItem, sParsedString, ZbnfParserStore.kString, 0,0, srcLine, srcColumn[0], srcFile, parentOfParentResultItem);
           ////
          }
        }
        
        if((log.mLogParse & LogParsing.mLogParseItem)!=0) {
          String sLog = bOk ? " ok " : " ERROR";
          report.report(3, sLog);
        }
        if(dbgPosFrom > 0 && posInput >= dbgPosFrom && posInput <= dbgPosTo
            && (dbgLineSyntax == 0 || dbgLineSyntax == syntaxItem.lineFile)
              ) {
            Debugutil.stop();
        }
        return bOk;
      }
  
      
      private boolean parseWhiteSpaceAndComment() //ZbnfParserStore parseResult)
      { return parseWhiteSpaceAndCommentOrTerminalSymbol(null, null);
      }

      /**Skips white spaces and parses a given constant syntax before checking and skipping comments.
       * Other comment parts are skipped.
       * If the sConstantSyntax is given, it have to be found. 
       * Otherwise the current position of input is rewind to the start before this
       * method is calling (no whitespaces and comments are skipped!).
       * If the constant syntax is not given, all comments and white spaces are skipped.
       * @param sConstantSyntax The string to test as terminal symbol or null. 
       * @param parentResultItem the parent parse result item for a new child, can be null if sConstantSyntax is null.
       * @return true if the constant syntax given by syntaxItem is found. 
       *         false if it is not found or syntaxItem == null.
       */
      private boolean parseWhiteSpaceAndCommentOrTerminalSymbol(String sConstantSyntax, ZbnfParserStore.ParseResultItemImplement parentResultItem)
      { long posInput  = input.getCurrentPosition();
        
        long posCurrent = input.getCurrentPosition();
        CharSequence test = input.getCurrent(40);
//        if(StringFunctions.startsWith(test, "  //NOTE all 24 low-bits are 0")){
//          Debugutil.stop();
//        }
        boolean bFoundConstantSyntax;  
        boolean bFoundAnySpaceOrComment;
        do  //if once of whitespace, comment or endlinecomment is found, try again.
        { bFoundAnySpaceOrComment = false;
          if(  sConstantSyntax != null 
            && sConstantSyntax.charAt(0) == StringFunctions.cEndOfText
            && input.length()==0
            )
          { bFoundConstantSyntax = true;
            bFoundAnySpaceOrComment = false;
            if(nReportLevel >= nLevelReportParsing) report.reportln(idReportParsing, "parseWhiteSpace;        " + input.getCurrentPosition()+ " " + input.getCurrent(30) + sEmpty.substring(0, nRecursion) + " parse Ok EndOfText:");
          }
          else if(sConstantSyntax != null && input.startsWith(sConstantSyntax))
          { bFoundConstantSyntax = true;
            bFoundAnySpaceOrComment = false;
            if(sConstantSyntax.equals("<:>")){
              stop();
            }
            long nStart = input.getCurrentPosition();
            srcLine = input.getLineAndColumn(srcColumn);
            String sFileInput = input.getInputfile();
            input.seek(sConstantSyntax.length());
            if(bConstantSyntaxAsParseResult && ! bDoNotStoreData) {
              srcLineOption = -1;
              parserStoreInPrescript.addConstantSyntax(null, sConstantSyntax, nStart, input.getCurrentPosition(), srcLine, srcColumn[0], sFileInput, parentResultItem);
            }
            else {
              if(srcLineOption == -1) {  //it is the first item in an option, without result item.
                srcLineOption = srcLine; srcColumnOption[0] = srcColumn[0]; srcPosOption = nStart; 
              }
              if(parentResultItem !=null) {
                //especially if it is the first item in component.
                parentResultItem.setSrcLineColumnFileInParent(nStart, input.getCurrentPosition(), srcLine, srcColumn[0], sFileInput);  
              }
            }
            if(nReportLevel >= nLevelReportParsing) report.reportln(idReportParsing, "parseConstInComment;    " + input.getCurrentPosition()+ " " + input.getCurrent(30) + sEmpty.substring(0, nRecursion) + " parse Ok Terminal:" + sConstantSyntax);
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
            { input.lento(sCommentStringEnd, StringPartScan.seekEnd);
              if(input.length()>0)
              { bFoundAnySpaceOrComment = true;
      
                input.fromEnd();
                posCurrent = input.getCurrentPosition();
              }
            }
      
            if(!bFoundAnySpaceOrComment && sEndlineCommentStringStart != null && input.startsWith(sEndlineCommentStringStart))
            { input.lento("\n"); //, StringPart.seekEnd);  //the \n should not included, it will skip either as whitespace or it is necessary for the linemode.
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
          int posParseResult = parserStoreInPrescript.getNextPosition();
          parserStoreInPrescript.setCurrentPosition(posParseResult);
        }
        return bFoundConstantSyntax;
      }

      /**
       * checks a terminal symbol.
       * @param syntaxItem The syntaxitem of a terminal symbol, containing the constant syntax.
       * @param parentResultItem the parent parse result item for a new child.
       * @return
       * ##s
       */
      private boolean parseTerminalSymbol(ZbnfSyntaxPrescript syntaxItem, ZbnfParserStore.ParseResultItemImplement parentResultItem)
      { boolean bOk;
        long nStart = input.getCurrentPosition();
//        if(nStart == 6532)
//          { Debugutil.stop();}
        srcLine = input.getLineAndColumn(srcColumn);
        String sFile = input.getInputfile();
        String sConstantSyntax = syntaxItem.getConstantSyntax();
        if(input.scanStart().scan(sConstantSyntax).scanOk())
        { bOk = true;
          //input.seek(sConstantSyntax.length());
          if(bConstantSyntaxAsParseResult && ! bDoNotStoreData) {
            srcLineOption = -1;
            parserStoreInPrescript.addConstantSyntax(syntaxItem, sConstantSyntax, nStart, input.getCurrentPosition(), srcLine, srcColumn[0], sFile, parentResultItem);
          } else {
            if(srcLineOption == -1){  //it is the first item in an option, without result item.
              srcLineOption = srcLine; srcColumnOption[0] = srcColumn[0]; srcPosOption = nStart; 
            }
            if(parentResultItem !=null) {
              parentResultItem.setSrcLineColumnFileInParent(nStart, input.getCurrentPosition(), srcLine, srcColumn[0], sFile);  //especially if it is the first item in component.
            }
          }
          if(nReportLevel >= nLevelReportParsing) report.reportln(idReportParsing, "parseTerminalSymbol;    " + input.getCurrentPosition()+ " " + input.getCurrent(30) + sEmpty.substring(0, nRecursion) + " parse Ok Terminal:" + sConstantSyntax);
        }
        else
        { bOk = false;
          saveError("\"" + sConstantSyntax + "\"");
        }
        return bOk;
      }
  
  
  
      /**
       * @param syntaxItem
       * @param sSemanticForStoring
       * @param parentResultItem the parent parse result item for a new child.
       * @param bSkipSpaceAndComment
       * @return
       * @throws ParseException
       */
      private boolean parseRegularExpression(ZbnfSyntaxPrescript syntaxItem, String sSemanticForStoring, ZbnfParserStore.ParseResultItemImplement parentResultItem, boolean bSkipSpaceAndComment) 
      throws ParseException
      { boolean bOk;
        Pattern pattern = syntaxItem.getRegexPatternFromComplexItem(); //Pattern.compile(sSyntax);
        String sSyntax = pattern.pattern();
        if(nReportLevel >= nLevelReportParsing) report.reportln(idReportParsing, "parseRegex;             " + input.getCurrentPosition()+ " " + input.getCurrent(30) + sEmpty.substring(0, nRecursion) + " parseRegex(" + nRecursion + ") <!" + sSyntax + "?" + sSemanticForError + ">");
        CharSequence sInput = input.getCurrentPart();
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
          //if(sSemanticForStoring != null)
          { CharSequence sResult = input.getCurrentPart();
            long posResult = input.getCurrentPosition();
            int srcLine = input.getLineAndColumn(srcColumn);
            bOk = addResultOrSubsyntax(sResult, posResult, srcLine, srcColumn[0], input.getInputfile(), sSemanticForStoring, parentResultItem, syntaxItem.getSubSyntax());
            //parseResult.addString(input, sSemanticForStoring, parentResultItem);
          }
          if(bOk){ input.fromEnd(); }
        }
        return bOk;
      }
  
  
      private boolean parseIdentifier(String addChars, String sSemanticForStoring, ZbnfParserStore.ParseResultItemImplement parentResultItem)
      { boolean bOk;
  
        if(nReportLevel >= nLevelReportParsing) report.reportln(idReportParsing, "parseIdentifier;        " + input.getCurrentPosition()+ " " + input.getCurrent(30) + sEmpty.substring(0, nRecursion) + " parseIdentifier(" + nRecursion + ") <$" + "" + "?" + sSemanticForError + ">");
        input.lentoIdentifier(null, addChars);
        if( input.length() > 0)
        { String sIdentifier = input.getCurrentPart().toString();
          if(sIdentifier.equals("way1Sensor") && parentPrescriptParser.parseResultToOtherComponent !=null)
            stop();
          if(listKeywords.get(sIdentifier)!=null)
          {
            bOk = false;
            input.setLengthMax();
          }
          else
          { bOk = true;
            if(sSemanticForStoring != null && ! bDoNotStoreData)
            { parserStoreInPrescript.addIdentifier(sSemanticForStoring, null, sIdentifier, parentResultItem, srcLine = input.getLineAndColumn(srcColumn), srcColumn[0], input.getInputfile());
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
  
  
      private boolean parsePositiveInteger(ZbnfSyntaxPrescript syntaxItem, ZbnfParserStore.ParseResultItemImplement parentResultItem, int maxNrofChars) throws ParseException
      { boolean bOk;
        if(nReportLevel >= nLevelReportParsing) report.reportln(idReportParsing, "parsePositivIntg;       " + input.getCurrentPosition()+ " " + input.getCurrent(30) + sEmpty.substring(0, nRecursion) + " parsePosNumber(" + nRecursion + ") <#?" + sSemanticForError + ">");
        if(input.scanDigits(10, Integer.MAX_VALUE, syntaxItem.getConstantSyntax()).scanOk())  //Note: constantSyntax is separator chars.
        { bOk = true;
          if(syntaxItem.getSemantic() != null && ! this.bDoNotStoreData)
          { parserStoreInPrescript.addIntegerNumber(syntaxItem.getSemantic(), null, input.getLastScannedIntegerNumber(), parentResultItem);
          }
        }
        else
        { bOk = false;
          saveError("positive number <?" + sSemanticForError + ">");
        }
        return bOk;
      }
  
  
      private boolean parseHexNumber(ZbnfSyntaxPrescript syntaxItem, ZbnfParserStore.ParseResultItemImplement parentResultItem, int maxNrofChars) throws ParseException
      { boolean bOk;
        if(nReportLevel >= nLevelReportParsing) report.reportln(idReportParsing, "parseHexNumber;         " + input.getCurrentPosition()+ " " + input.getCurrent(30) + sEmpty.substring(0, nRecursion) + " parseHex(" + nRecursion + ") <#x?" + sSemanticForError + ">");
        if(input.scanDigits(16, maxNrofChars, syntaxItem.getConstantSyntax()).scanOk())
        { bOk = true;
          if(syntaxItem.getSemantic() != null && ! this.bDoNotStoreData)
          { parserStoreInPrescript.addIntegerNumber(syntaxItem.getSemantic(), null, input.getLastScannedIntegerNumber(), parentResultItem);
          }
        }
        else
        { bOk = false;
          saveError("hex number" + " <?" + sSemanticForError + ">");
        }
        return bOk;
      }
  
  
      /**Any desired radix, 2, 8, 16, 24 etc.
       * @param syntaxItem
       * @param parentResultItem
       * @param maxNrofChars
       * @return
       * @throws ParseException
       */
      private boolean parseNumberRadix(ZbnfSyntaxPrescript syntaxItem, ZbnfParserStore.ParseResultItemImplement parentResultItem, int maxNrofChars) throws ParseException
      { boolean bOk;
        if(nReportLevel >= nLevelReportParsing) report.reportln(idReportParsing, "parseHexNumber;         " + input.getCurrentPosition()+ " " + input.getCurrent(30) + sEmpty.substring(0, nRecursion) + " parseHex(" + nRecursion + ") <#x?" + sSemanticForError + ">");
        if(input.scanDigits((int)syntaxItem.nFloatFactor, maxNrofChars, syntaxItem.getConstantSyntax()).scanOk())
        { bOk = true;
          if(syntaxItem.getSemantic() != null && ! this.bDoNotStoreData)
          { parserStoreInPrescript.addIntegerNumber(syntaxItem.getSemantic(), null, input.getLastScannedIntegerNumber(), parentResultItem);
          }
        }
        else
        { bOk = false;
          saveError("hex number" + " <?" + sSemanticForError + ">");
        }
        return bOk;
      }
  
  
      private boolean parseInteger(ZbnfSyntaxPrescript syntaxItem, ZbnfParserStore.ParseResultItemImplement parentResultItem, int maxNrofChars) throws ParseException
      { boolean bOk;
        if(nReportLevel >= nLevelReportParsing) report.reportln(idReportParsing, "parseInteger;           " + input.getCurrentPosition()+ " " + input.getCurrent(30) + sEmpty.substring(0, nRecursion) + " parseInt(" + nRecursion + ") <#-?" + sSemanticForError + ">");
        if(input.scanInteger(syntaxItem.getConstantSyntax()).scanOk())  //constantSyntax may contain additional separator chars or null. 
        {
          bOk = true;
          if(syntaxItem.getSemantic() != null && ! this.bDoNotStoreData)
          { parserStoreInPrescript.addIntegerNumber(syntaxItem.getSemantic(), null, input.getLastScannedIntegerNumber(), parentResultItem);
          }
        }
        else
        { bOk = false;
          saveError("integer number" + " <?" + sSemanticForError + ">");
        }
        return bOk;
      }
  
  
      private boolean parseFloatNumber(ZbnfSyntaxPrescript syntaxItem, int maxNrofChars, ZbnfParserStore.ParseResultItemImplement parentResultItem) 
      throws ParseException
      { boolean bOk;
        if(nReportLevel >= nLevelReportParsing) report.reportln(idReportParsing, "parseFloat;             " + input.getCurrentPosition()+ " " + input.getCurrent(30) + sEmpty.substring(0, nRecursion) + " parseFloat(" + nRecursion + ") <#f?" + sSemanticForError + ">");
        if(input.scanFloatNumber(true).scanOk())
        {
          bOk = true;
          if(syntaxItem.getSemantic() != null && ! this.bDoNotStoreData)
          { double result = input.getLastScannedFloatNumber();
            if(syntaxItem.getType() == ZbnfSyntaxPrescript.EType.kFloatWithFactor)
            { result *= syntaxItem.getFloatFactor();
            }
            srcLineOption = -1;
            parserStoreInPrescript.addFloatNumber(syntaxItem.getSemantic(), null, result, parentResultItem);
          }
        }
        else
        { bOk = false;
          saveError("float number" + " <?" + sSemanticForError + ">");
        }
        return bOk;
      }
  
  
      private boolean parseNoOrSomeCharsOutsideQuotion(String sSyntax, int maxNrofChars, String sSemanticForStoring, ZbnfParserStore.ParseResultItemImplement parentResultItem, ZbnfSyntaxPrescript syntaxItem) throws ParseException
      { boolean bOk;
        if(nReportLevel >= nLevelReportParsing) report.reportln(idReportParsing, "parseCharsOutsideQuot;  " + input.getCurrentPosition()+ " " + input.getCurrent(30) + sEmpty.substring(0, nRecursion) + " parse(" + nRecursion + ") <*\"\"" + sSyntax + "?" + sSemanticForError + ">");
        int len = input.indexOfAnyCharOutsideQuotion(sSyntax, 0, maxNrofChars);
        bOk = input.found();
        if(bOk)
        { if(len >=0)
          { input.lento(len);
            CharSequence sResult = input.getCurrentPart();
            long posResult = input.getCurrentPosition();
            int srcLine = input.getLineAndColumn(srcColumn); 
            bOk = addResultOrSubsyntax(sResult, posResult, srcLine, srcColumn[0], input.getInputfile(), sSemanticForStoring, parentResultItem, syntaxItem.getSubSyntax());
            //if(sSemanticForStoring != null)
            //{ parseResult.addString(input, sSemanticForStoring);
            //}
            if(bOk){ input.fromEnd(); }
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
       * @throws ParseException 
       */
      private boolean parseStringUntilRightEndchar(String sConstantSyntax, boolean bInclusive, int maxNrofChars, String sSemanticForStoring, ZbnfParserStore.ParseResultItemImplement parentResultItem, ZbnfSyntaxPrescript syntaxItem) throws ParseException
      { boolean bOk;
        if(nReportLevel >= nLevelReportParsing) report.reportln(idReportParsing, "parseStrTilRightChar;   " + input.getCurrentPosition()+ " " + input.getCurrent(30) + sEmpty.substring(0, nRecursion) + " parse(" + nRecursion + ") <stringtolastinclChar?" + sSemanticForError + ">");
        input.lentoAnyChar(sConstantSyntax, maxNrofChars, StringPartScan.seekBack);
        bOk = input.found();
        if(bOk)
        { if(bInclusive) 
          { input.lento(input.length()+1);  //inclusive termintated char.
          } 
          if(input.length() >0)
          { CharSequence sResult = input.getCurrentPart();
            long posResult = input.getCurrentPosition();
            srcLine = input.getLineAndColumn(srcColumn);
            int srcLine = input.getLineAndColumn(srcColumn);
            bOk = addResultOrSubsyntax(sResult, posResult, srcLine, srcColumn[0], input.getInputfile(), sSemanticForStoring, parentResultItem, syntaxItem.getSubSyntax());
          }
          input.fromEnd();
        }
        else
        { input.setLengthMax();
          saveError("ones of terminate chars \"" + sConstantSyntax + "\" not found <?" + sSemanticForError + ">");
        }
        return bOk;
      }
  
      
      /**parses until one of some one of the  end strings.
       * 
       * @param sConstantSyntax now unused.
       * @param bInclusive If true, the end char is parsed inclusive, if false, than exclusive.
       * @param maxNrofChars possible given at left position <123....> or Integer.MAX_VALUE
       * @param sSemanticForStoring The semantic, null if no result should be stored.
       * @param syntaxItem The syntax item, contains info for substrings, see {@link ZbnfSyntaxPrescript#getListStrings()}.
       * @param parseResult Buffer to store the result. It may be a special buffer or the main buffer.
       * @return
       * @throws ParseException 
       */
      private boolean parseStringUntilTerminateString
      ( String sConstantSyntax
      , boolean bInclusive
      , boolean bTrim
      , int maxNrofChars
      , String sSemanticForStoring
      , ZbnfParserStore.ParseResultItemImplement parentResultItem
      , ZbnfSyntaxPrescript syntaxItem
      ) throws ParseException
      { boolean bOk;
        if(nReportLevel >= nLevelReportParsing) report.reportln(idReportParsing, "parseStrTilTermString;  " + input.getCurrentPosition()+ " " + input.getCurrent(30) + sEmpty.substring(0, nRecursion) + " parse(" + nRecursion + ") <*" + sConstantSyntax + "?" + sSemanticForError + ">");
        { int mode = bInclusive ? StringPartScan.seekEnd : StringPartScan.seekNormal;
          input.setLengthMax().lentoAnyString(syntaxItem.getListStrings().toArray(new String[1]), maxNrofChars, mode);
        }  
        bOk = input.found();
        if(bOk)
        { if(bInclusive) 
          { input.lento(input.length()+1);  //inclusive termintated char.
          } 
          StringPart.Part sResult = input.getCurrentPart();
          if(bTrim){ 
            sResult = sResult.trim();
          }
          long posResult = input.getCurrentPosition();
          int srcLine = input.getLineAndColumn(srcColumn);
          bOk = addResultOrSubsyntax(sResult, posResult, srcLine, srcColumn[0], input.getInputfile(), sSemanticForStoring, parentResultItem, syntaxItem.getSubSyntax());
          if(bOk){ input.fromEnd(); }
        }
        else
        { input.setLengthMax();  //not found: length() was setted to 0
          String sTerminateStrings = "";
          Iterator<String> iter = syntaxItem.getListStrings().iterator();
          while(iter.hasNext()){ sTerminateStrings += "|" + (iter.next());}
          saveError("ones of terminate strings:"+ sTerminateStrings + " not found" + " <?" + sSemanticForError + ">");
        }
        return bOk;
      }
  

      /**parses a component. This method creates a new SubParser instances and works
       * with an own syntax definition, in ZBNF written as "<code>sDefinitionIdent::=.... .</code>".
       * It is possible to parse in mainstream input, in this case this.input should be given as
       * parameter sInputP, or it is possible to parse a detected part of input, escpecially
       * after parsing "<code>&lt;*...?!sDefinitionIdent&gt;</code>".<br>
       * It is also possible to write the result in the main parse result buffer, or in a extra buffer.
       * @param sInputP input to parse. This may be the main input from this.
       * @param posInputbase The position of the given sInputP in the originally input. Note that this parse routine
       *   does not use the {@link #input} but parses the sInputP. This is the offset information.
       * @param sDefinitionIdent Identifier of the Syntax prescript, written in ZBNF at "sDefinitionIdent::=..."
       * @param sSemanticForStoring If the semantic is specified outside, it is its identifier. Otherwise it is null 
       * @param bSkipSpaceAndComment True if there are spaces in the prescript before this component.
       * @param bResultToAssignIntoNextComponent If true, than the second Buffer {@link #parseResultToOtherComponent} 
       *        is used to store the parse result. That is, if the calling syntax/semantic syntax contains <code>&lt;...?-...&gt></code>.
       * @param parentSyntaxItem That syntax element which invokes parsing this component. It contains for example
       *        the {@link ZbnfSyntaxPrescript#bDebugParsing} for a manual break while debugging.
       * @param bAddParseResultFromPrevious If true, than this Parseresult containing in 
       *        {@link #parseResultToOtherComponent} should be added at start of this components result associated to this component.
       *        It is a result of previous parsing with <code>&lt;...?-...&gt></code>, 
       *        stored now because the components prescript has the form <code>&lt;...?+...&gt></code>.
       * @return true if succesfull, false if the input not matches to the prescript. 
       * @throws ParseException 
       */
      private boolean parse_Component
      ( StringPartScan sInputP
      , int posInputbase
      , ZbnfSyntaxPrescript syntaxCmpn
      , String sSemanticForStoring
      , ZbnfParserStore.ParseResultItemImplement parentResultItem
      , boolean bSkipSpaceAndComment
      , boolean bDoNotStoreData
      , ZbnfSyntaxPrescript parentSyntaxItem  //which invokes the component.
      , boolean bResultToAssignIntoNextComponent
      , boolean bAddParseResultFromPrevious
      ) throws ParseException
      { boolean bOk;
        String sKeySearchAlreadyParsed = String.format("%9d", input.getCurrentPosition()) + syntaxCmpn.sDefinitionIdent;
        ParseResultlet resultlet = alreadyParsedCmpn.get(sKeySearchAlreadyParsed);
        if(resultlet !=null){
          Assert.stop();
        }
          
        if(nReportLevel >= nLevelReportParsing) report.reportln(idReportParsing, "parseComponent;         " + input.getCurrentPosition()+ " " + input.getCurrent(30) + sEmpty.substring(0, nRecursion) + " parseComponent(" + nRecursion + ") <" + syntaxCmpn.sDefinitionIdent + "?" + sSemanticForError + ">");
        if(syntaxCmpn.sDefinitionIdent.equals("headerBlock") && parentResultItem.sSemantic.equals("CLASS_C"))
        { stop();
        
        }
        /**Create the SubParser-instace: */
          final PrescriptParser componentsPrescriptParser;
          
//          if(sDefinitionIdent.equals("variableDefinition"))
//            stop();
          //create an own store for results, or use the given store.
          { final ZbnfParserStore store1;
            ZbnfParserStore.ParseResultItemImplement parentResultItem1;
            if(bResultToAssignIntoNextComponent)
            { /**the parse result of the sub parser should be stored not in main parse result, to transfer to deeper levels. */
              if(parseResultToOtherComponent == null)
              { parseResultToOtherComponent = new ZbnfParserStore();
              }
              store1 = parseResultToOtherComponent;
              parentResultItem1 = null;
            }
            else
            { store1 = PrescriptParser.this.parserStoreInPrescript;
              parentResultItem1 = parentResultItem;
            }
            componentsPrescriptParser = new PrescriptParser
            ( PrescriptParser.this
            , syntaxCmpn 
            , syntaxCmpn.sDefinitionIdent
            , sInputP, posInputbase
            );
            
            bOk = componentsPrescriptParser.parsePrescript1
                  ( sSemanticForStoring, parentSyntaxItem, parentResultItem1, store1
                  , bAddParseResultFromPrevious ? parseResultToOtherComponent : null
                  , bSkipSpaceAndComment
                  , bDoNotStoreData
                  , nRecursion +2
                  );
            stop();
          }
          if(bOk)
          { //parseResult.setOffsetToEnd(posResult); 
//            if(complexItem.sDefinitionIdent.equals("test_description"))
//              Debugutil.stop();
//      
//            if(bResultToAssignIntoNextComponent)
//              Debugutil.stop();
//            if(sSemanticForStoring !=null && sSemanticForStoring.equals("description"))
//              Debugutil.stop();
            if(nReportLevel >= nLevelReportParsing)
            { report.reportln(idReportParsing, "parseCompOk;            " + input.getCurrentPosition()+ " " + input.getCurrent(30) + sEmpty.substring(0, nRecursion) + " parseComponent-ok(" + nRecursion + ") <?" + sSemanticForError + ">");
            }
          }
          else
          { if(nReportLevel >= nLevelReportParsing)
            { report.reportln(idReportParsing, "parseCompError;         " + input.getCurrentPosition()+ " " + input.getCurrent(30) + sEmpty.substring(0, nRecursion) + " parseComponent-error(" + nRecursion + ") <?" + sSemanticForError + ">");
            }
          }
        return bOk;
      }
  
    
    
      
        
      
      
      private boolean addResultOrSubsyntax(CharSequence sResult, long srcBegin, int srcLine, int srcColumn, String srcFile, String sSemanticForStoring, ZbnfParserStore.ParseResultItemImplement parentResultItem, String subSyntax) throws ParseException
      { boolean bOk;
//        if(sSemanticForStoring !=null && sSemanticForStoring.equals("ST/@Text"))
//          Debugutil.stop();
        if(subSyntax!= null)
        { //The source string of the parsed component is used for further parsing of them by a sub syntax:
          if(sSemanticForStoring !=null){
            assert(false);  //it is excluded by the syntax definition.
          }
          ZbnfSyntaxPrescript syntaxCmpn = searchSyntaxPrescript(subSyntax);
          if(syntaxCmpn == null) {
            bOk = false;
            report.reportln(MainCmdLogging_ifc.error, "parse - Syntaxprescript not found:" + subSyntax);
            String sError = "prescript for : <" + subSyntax 
            + ((!sSemanticForError.equals("@") && !sSemanticForError.equals("?") ) ? "?" + sSemanticForError : "")
            + "> not found.";
            saveError(sError);
          } else {
            StringPartScan partOfInput = new StringPartScan(sResult);
            bOk = parse_Component(partOfInput, (int)srcBegin, syntaxCmpn, sSemanticForStoring, parentResultItem, false, false, null, false, false);
          }
        }
        else if( sSemanticForStoring != null && ! bDoNotStoreData)
        { srcLineOption = -1;
          parserStoreInPrescript.addString(sResult, sSemanticForStoring, null, null, parentResultItem, srcLine, srcColumn, srcFile);
          bOk = true;
        } else {
          if(srcLineOption == -1) {  //it is the first item in an option, without result item.
            srcLineOption = srcLine; srcColumnOption[0] = srcColumn; srcPosOption = srcBegin; 
          }
          if(parentResultItem !=null) {
            parentResultItem.setSrcLineColumnFileInParent(srcBegin, srcBegin, srcLine, srcColumn, srcFile);  //especially if it is the first item in component.
          }
          bOk = true; 
        }
        return bOk;        
      }
      
      
  
      /**
       *
       * @param sQuotionMarks  left and right quotion mark char, typical "" or '', also possible <> or ><
       * @param maxNrofChars
       * @param sSemantic
       * @return
       * @throws ParseException 
       */
      private boolean parseSimpleStringLiteral(String sQuotionMarks, int maxNrofChars, String sSemanticForStoring, ZbnfParserStore.ParseResultItemImplement parentResultItem, ZbnfSyntaxPrescript syntaxItem) throws ParseException
      { boolean bOk;
        char transcriptChar = sQuotionMarks.length() >=3 ? sQuotionMarks.charAt(2) : '\\';
        if(nReportLevel >= nLevelReportParsing) report.reportln(idReportParsing, "parseSimpleStringLit;   " + input.getCurrentPosition()+ " " + input.getCurrent(30) + sEmpty.substring(0, nRecursion) + " parse(" + nRecursion + ") <\"\"\"?" + sSemanticForError + ">");
        if(input.getCurrentChar() == sQuotionMarks.charAt(0))
        { int len = input.indexEndOfQuotation(sQuotionMarks.charAt(1), transcriptChar, 0, maxNrofChars);
          if(len >=2)
          { bOk = true;
            input.seekPos(1);       //without start quotion mark
            input.lentoPos(len-2);  //without end quotion mark
            CharSequence sResult = input.getCurrentPart();
            long posResult = input.getCurrentPosition();
            int srcLine = input.getLineAndColumn(srcColumn);  //, srcLine, srcColumn[0], input.getInputfile()
            bOk = addResultOrSubsyntax(sResult, posResult, srcLine, srcColumn[0], input.getInputfile(), sSemanticForStoring, parentResultItem, syntaxItem.getSubSyntax());
            //if(sSemanticForStoring != null)
            //{ parseResult.addString(input, sSemanticForStoring);
            //}
            if(bOk){
              input.fromEnd().seekPos(1);  //behind right quotion mark
            }
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
  
  
      /** Parses at start of an option.
       * 
       * @param options The current syntaxprescript item, a option item.
       * @return
       * @throws ParseException 
       */
      //private boolean parseOptions(Iterator<ZbnfSyntaxPrescript> iterItems, ZbnfSyntaxPrescript options, boolean bSkipSpaceAndComment, ParserStore parseResult)
      private boolean parseOptions(ZbnfSyntaxPrescript optionPrescript, ZbnfParserStore.ParseResultItemImplement parentResultItem, boolean bSkipSpaceAndComment, int recursion) throws ParseException
      { boolean bOk = true;
        //boolean bNotFound = true;
        if(nReportLevel >= nLevelReportParsing) report.reportln(idReportParsing, "parseOptions;           " + input.getCurrentPosition()+ " " + input.getCurrent(30) + sEmpty.substring(0, nRecursion) + " parse option:" + sSemanticForError);
        //ZbnfSyntaxPrescript optionPrescript = listPrescripts.get(idxPrescript);
        /*
        int optionType = optionPrescript.getType();
        boolean bParseFirstAfterOption 
        = (optionType == ZbnfSyntaxPrescript.EType.kAlternativeOptionCheckEmptyFirst)
        ;
        
        if(bParseFirstAfterOption)
        {
          int posParseResult = parserStoreInPrescript.getNextPosition();
          long posInput  = input.getCurrentPosition();
          int idxItemOption = idxPrescript;
          idxPrescript +=1;  //after the option
          while(bOk && idxPrescript < listPrescripts.size()) //iterItems.hasNext())
          {
            if(testSkipSpaceAndComment(listPrescripts)){ bSkipSpaceAndComment = true; }
            if(idxPrescript < listPrescripts.size())  //consider spaces on end of prescript.
            { bOk = parseItem(listPrescripts.get(idxPrescript), bSkipSpaceAndComment); //, parseResult, null);  //##s
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
            if(nReportLevel >= nLevelReportParsing) report.reportln(idReportParsing, "parseOptionOk;          " + input.getCurrentPosition()+ " " + input.getCurrent(30) + sEmpty.substring(0, nRecursion) + " parse FirstAfterOption -ok:");
          }
          else
          { //error in parsing after option, try it regarding the option!
            input.setCurrentPosition(posInput);
            parserStoreInPrescript.setCurrentPosition(posParseResult);
            idxPrescript = idxItemOption;
            if(nReportLevel >= nLevelReportParsing) report.reportln(idReportParsing, "parseOptionError;       " + input.getCurrentPosition()+ " " + input.getCurrent(30) + sEmpty.substring(0, nRecursion) + " parse FirstAfterOption -error:");
          }
        }
  
        if(!bParseFirstAfterOption || !bOk)  //##cc
        */
        { //now try the option.
          //if(nReportLevel >= nLevelReportParsing) report.reportln(idReportParsing, "parseOptionFirstAfter;  " + input.getCurrentPosition()+ " " + input.getCurrent(30) + sEmpty.substring(0, nRecursion) + " try options:");
          srcLineOption = -1;  //clear          
          SubParser optionParser = new SubParser(this, parentResultItem, this.bDoNotStoreData || optionPrescript.bDonotStoreData, nRecursion+1); //false);
          bOk = optionParser.parseSub(optionPrescript, "[...]"/*sSemanticForError*/, ZbnfParserStore.kOption, null, "@", parentResultItem, bSkipSpaceAndComment,  null, recursion+1);
          if(!bOk)
          { /* The isPossibleEmptyOption will be checked in parseSub
            if(optionPrescript.alsoEmptyOption) {
              bOk = true;    //the option should not be match
            } else */{
              saveError(" [...]<?" + sSemanticForError + ">");
            }
          }
        } 
        return bOk;
      }
  
  
      private boolean parseNegativVariant(ZbnfSyntaxPrescript syntaxNegativ, boolean bSkipSpaceAndComment, int recursion) throws ParseException
      { boolean bOk = true;
        //boolean bNotFound = true;
        if(nReportLevel >= nLevelReportParsing) report.reportln(idReportParsing, "parseNegativVariante;   " + input.getCurrentPosition()+ " " + input.getCurrent(30) + sEmpty.substring(0, nRecursion) + " [?" + sSemanticForError);
        
        //TODO: use a own buffer and trash it. //true/*use own buffer*/);
        //or be careful that the negativParser don't save any parse results.
        SubParser negativParser = new SubParser(this, null, false, nRecursion+1); 
        int posParseResult = parserStoreInPrescript.getNextPosition();
        long posInput  = input.getCurrentPosition();
        bOk = negativParser.parseSub(syntaxNegativ, "[?..]"/*sSemanticForError*/, ZbnfParserStore.kOption, null, "@", null, bSkipSpaceAndComment, null, recursion+1);
        /**always set the current position back to the originator at begin of this parse test. */
        input.setCurrentPosition(posInput);
        parserStoreInPrescript.setCurrentPosition(posParseResult);
        if(nReportLevel >= nLevelReportBranchParsing)    
        { log.reportParsing("parseNegV ", idReportBranchParsing, syntaxNegativ, sReportParentComponents, input, (int)input.getCurrentPosition(), nRecursion, bOk);
        }  
        return !bOk;  //negation, it is not ok if the result matches.
      }
  
  
      private boolean parseExpectedVariant(ZbnfSyntaxPrescript syntaxPrescript, boolean bSkipSpaceAndComment, int recursion) 
      throws ParseException
      { final boolean bOk;
        if(nReportLevel >= nLevelReportParsing) report.reportln(idReportParsing, "parseExpectVariante;    " + input.getCurrentPosition()+ " " + input.getCurrent(30) + sEmpty.substring(0, nRecursion) + " [!" + sSemanticForError);
        
        SubParser expectedParser = new SubParser(this, null, this.bDoNotStoreData || syntaxPrescript.bDonotStoreData, nRecursion+1); 
        int posParseResult = parserStoreInPrescript.getNextPosition();
        long posInput  = input.getCurrentPosition();
        bOk = expectedParser.parseSub(syntaxPrescript, "[!..]"/*sSemanticForError*/, ZbnfParserStore.kOption, null, "@", null, bSkipSpaceAndComment, null, recursion+1);
        if(bOk)
        { /**it is okay, but ignore the result there. */
          input.setCurrentPosition(posInput);
          parserStoreInPrescript.setCurrentPosition(posParseResult);
        }
        return bOk;  //negation, it is not ok if the result matches.
      }

      private boolean parseUnconditionalVariant(ZbnfSyntaxPrescript syntaxPrescript, ZbnfParserStore.ParseResultItemImplement parentResultItem, boolean bSkipSpaceAndComment, int recursion) 
      throws ParseException
      { boolean bOk = true;
        //boolean bNotFound = true;
        if(nReportLevel >= nLevelReportParsing) report.reportln(idReportParsing, "parseUncondVariante;    " + input.getCurrentPosition()+ " " + input.getCurrent(30) + sEmpty.substring(0, nRecursion) + " [>" + sSemanticForError);
        
        SubParser positiveParser = new SubParser(this, parentResultItem, this.bDoNotStoreData || syntaxPrescript.bDonotStoreData, nRecursion+1); 
        bOk = positiveParser.parseSub(syntaxPrescript, "[>..]"/*sSemanticForError*/, ZbnfParserStore.kOption, null, "@", parentResultItem, bSkipSpaceAndComment, null, recursion+1);
        if(!bOk) 
          throw new ParseException("unconditional Syntax failes", 0);
        return bOk;  //negation, it is not ok if the result matches.
      }
  
  
      private boolean parseRepetition(ZbnfSyntaxPrescript forwardSyntax, ZbnfSyntaxPrescript backwardSyntax
      , ZbnfParserStore.ParseResultItemImplement parentResultItem, boolean bSkipSpaceAndComment, int recursion
      ) throws ParseException
      { boolean bOk = true;
        boolean bShouldRepeat = true;
        boolean bRepeatContinue;
        int countRepetition = 0;
  
        String sForwardSemantic = forwardSyntax.getSemantic();
        if(sForwardSemantic != null) sSemanticForError = sForwardSemantic;
  
        //String sBackwardSemantic = null;
        //if(backwardSyntax != null) { sBackwardSemantic = backwardSyntax.getSemantic(); }
        if(sForwardSemantic != null) sSemanticForError = sForwardSemantic;
  
        SubParser repeatForwardParser = new SubParser(this, parentResultItem, this.bDoNotStoreData || forwardSyntax.bDonotStoreData, nRecursion+1); //false);
        SubParser repeatBackwardParser = new SubParser(this, parentResultItem, this.bDoNotStoreData || (backwardSyntax !=null && backwardSyntax.bDonotStoreData), nRecursion+1); //, false);
  
        long nStartLast = -1;
        if(nReportLevel >= nLevelReportParsing) report.reportln(idReportParsing, "parseRepetition;        " + input.getCurrentPosition()+ " " + input.getCurrent(30) + sEmpty.substring(0, nRecursion) + " parse repetition:");
        do
        { countRepetition +=1;
          
          long nStart = input.getCurrentPosition();

          boolean bOkForward;
          
          /*every loop will start with empty parseResultToOtherComponent,
           * otherwise the result is added multiple.
           */
            
          
          if(nStart == nStartLast)
          { bOkForward = false;
            if(nReportLevel >= nLevelReportParsing) report.reportln(idReportParsing, "parseRep-nonRepeat;     " + input.getCurrentPosition()+ " " + input.getCurrent(30) + sEmpty.substring(0, nRecursion) + "   parse no repetition because no progress on input");
          }
          else
          { nStartLast = nStart;
            if(nReportLevel >= nLevelReportParsing) report.reportln(idReportParsing, "parseRep-repeat;        " + input.getCurrentPosition()+ " " + input.getCurrent(30) + sEmpty.substring(0, nRecursion) + " { parse repetition nr:" + countRepetition);
            if(false) repeatForwardParser.init();  //because re-using
            bOkForward = repeatForwardParser.parseSub(forwardSyntax, "{...}"/*sSemanticForError*/, countRepetition, null, sForwardSemantic, parentResultItem, bSkipSpaceAndComment, null, recursion+1);
          }
  
          switch(recursion) //helper for break points in several level 
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
          if(!bOkForward && bShouldRepeat)
          { bOk = false;
            saveError("repetition required because backward-continue is matched."); // + " <?" + sSemanticForError + ">");
          }
  
          
          if(bOkForward)
          { 
            if(backwardSyntax != null)
            { if(nReportLevel >= nLevelReportParsing) report.reportln(idReportParsing, "parseRep-backCheck;     " + input.getCurrentPosition()+ " " + input.getCurrent(30) + sEmpty.substring(0, nRecursion) + " parse test repetition back:");
              if(false) repeatBackwardParser.init();  //because re-using
              bShouldRepeat = bRepeatContinue = repeatBackwardParser.parseSub(backwardSyntax, "{?...}"/*sSemanticForError*/, -countRepetition, null, "@", parentResultItem, bSkipSpaceAndComment, null, recursion);
            }
            else
            { bShouldRepeat = false;  //may be or not repeated
              bRepeatContinue = true;       //test the repeat possibility.
              if(nReportLevel >= nLevelReportParsing) report.reportln(idReportParsing, "parseRep-backUncond;    " + input.getCurrentPosition()+ " " + input.getCurrent(30) + sEmpty.substring(0, nRecursion) + " parse repetition test repeat:");
            }
          }
          else bRepeatContinue = false;
  
        } while(bRepeatContinue);
        if(nReportLevel >= nLevelReportParsing) report.reportln(idReportParsing, "parseRep-finish;        " + input.getCurrentPosition()+ " " + input.getCurrent(30) + sEmpty.substring(0, nRecursion) + " } parse repetition finished, nr:" + countRepetition);
        return bOk;
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
        { report.reportln(MainCmdLogging_ifc.error," saveError: actual length of input is to less");
        }
        int posInput = (int)input.getCurrentPosition() + posInputbase;
        if(posRightestError < posInput)
        { posRightestError = posInput;
          int[] column1 = new int[1];
          lineError = input.getLineAndColumn(column1);
          sFileError = input.getInputfile();
          columnError = column1[0];
          sRightestError = input.getCurrentPart(80);
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
        if(nReportLevel >= nLevelReportParsing) report.reportln(idReportParsing, "parse-saveError;        " + input.getCurrentPosition()+ " " + input.getCurrent(30) + sEmpty.substring(0, nRecursion) + " parseError");
      }
  
      /** gets the tree of semantic while parsing
       *  
       * @return Tree of semantic, every sSemanticForError is divide by a dot. 
       */
      private String getSemanticTreeForErrorMsg()
      { 
        StringBuilder sbSemantic = new StringBuilder(200);
        
        PrescriptParser syntaxPreScript1 = PrescriptParser.this;
        String sep = "";
        while(syntaxPreScript1 != null)
        {
          { //sbSemantic.insert(0, '.');
            //sbSemantic.insert(0, syntaxPreScript1.sSemanticIdent);
            sbSemantic.append(sep);
            sbSemantic.append(syntaxPreScript1.sSemanticIdent);
            syntaxPreScript1 = syntaxPreScript1.parentPrescriptParser;
            sep = " <- ";
          }
        }
        
        SubParser subParser = null; //this;
//        if(sSemanticForError.startsWith("stringvalue"))
//          stop();
        
        while(subParser != null)
        { if(  subParser.sSemanticForError != null
            //&& !subParser.sSemanticForError.equals("@")
            )
          { sbSemantic.append('+');
            sbSemantic.append(subParser.sSemanticForError);
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
  
      public String XXXgetDefinitionIdent()
      { return null; //syntaxPrescript.getDefinitionIdent();
      }
  
    }
  

  
  }//class PrescriptParser

  
  
  
  /**To MainCmdLogging_ifc something.*/
  protected final MainCmdLogging_ifc report;

  /**The current report level. 
   * This value is used to compare wether the report arguments are prepared or not.
   * The test of the level before calling report(...) saves calculation time.
   * It is set on starting of parse().
   */
  protected int nReportLevel;
  
  protected int nLevelReportParsing, nLevelReportComponentParsing, nLevelReportInfo, nLevelReportError;

  protected int nLevelReportBranchParsing = MainCmdLogging_ifc.error; //debug;
  /**The ident to report the progress of parsing. */
  protected int idReportParsing = MainCmdLogging_ifc.fineDebug;
  protected int idReportComponentParsing = MainCmdLogging_ifc.debug;
  protected int idReportBranchParsing = MainCmdLogging_ifc.debug;
  protected int idReportInfo = MainCmdLogging_ifc.info;
  protected int idReportError = MainCmdLogging_ifc.error;
  
  
  /** The list of some all syntax definitons (syntax components).*/
  protected final TreeMap<String,ZbnfSyntaxPrescript> listSubPrescript;

  /** Keywords*/
  TreeMap<String,String> listKeywords = new TreeMap<String,String>();

  /** xmlns */
  TreeMap<String,String> xmlnsList = null;
  
  /** Set if constant syntax (terminate morphems) also should stored. See setStoringConstantSyntax()*/
  protected boolean bConstantSyntaxAsParseResult = false;

  /**The main syntax prescript set from {@link setSyntax(StringPart)}. */
  private ZbnfSyntaxPrescript mainScript;
  
  protected PrescriptParser prescriptParserTopLevel;

  //protected PrescriptParser.SubParser subParserTopLevel;

  /** The string and position found on the rightest position of an parse fault.
   * It is necessary to report a parsing error.
   */
  protected CharSequence sRightestError = "--noError--";

  //protected int nRightestLineError;
  
  /**Required syntax on rightest parsing error position*/
  protected String sExpectedSyntax = "--noError--";
  
  /**founded syntax on rightest parsing error position*/
  protected String xxxsFoundedSyntax = "--noError--";

  /**Maximum number of shown parsing results on error. */
  private int maxParseResultEntriesOnError = 0; 
  
  private final LogParsing log; 
  
  /**founded content on rightest parsing error position. This list will be filled with current parse result
   * if it is the rightest position. 
   */
  ArrayList<ZbnfParseResultItem> listParseResultOnError  = null;

  /**The position of the most right parse fault. The information will be set newly any time if the parser founds 
   * a non matching position more right than the last one.
   */
  protected long posRightestError = 0;
  
  /**The lineError and columnError will be set if the input supports it, see {@link StringPart#getLineAndColumn(int[])}. 
   * It is necessary to report a parsing error.
   */
  protected int lineError, columnError;

  /**The file or name of the {@link StringPart#getInputfile()} which was parsed on the rightest error position. */
  protected String sFileError;
  
  
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

  
  protected Map<String, String> idxMissingPrescripts;
  
  /** The actual parse result buffer.*/
  private ZbnfParserStore parserStoreTopLevel; //parseResult;

  
  private final ZbnfParserStore.BuilderTreeNodeXml builderTreeNodeXml = new ZbnfParserStore.BuilderTreeNodeXml();
  
  
  /**Temporary store for column. */
  //private final int[] column = new int[1];

 
  /**Already parsed components with the same input text which should be requested in another context. 
   * The usage of the already detected parse result speeds up the parsing process. 
   * The syntax may be designed with such reused parts especially. 
   * The key contains the component syntax name and the position in the input.
   * */
  final TreeMap<String, ParseResultlet> alreadyParsedCmpn = new TreeMap<String, ParseResultlet>();
  
  /**Creates a empty parser instance. 
   * @param report A report output
   * */
  public ZbnfParser( MainCmdLogging_ifc report)
  { this(report, 20);
  }
  
  
  /**Creates a empty parser instance. 
   * @param report A report output
   * @param maxParseResultEntriesOnError if 0 than no parse result is stored.
   *        If >0, than the last founded parse result is stored to support better analysis of syntax errors,
   *        but the parser is slower.
   */
  public ZbnfParser( MainCmdLogging_ifc report, int maxParseResultEntriesOnError)
  { this.report = report;
    this.args = new Args();   //Default values.
    //parserStore = new ParserStore();
    listSubPrescript = new TreeMap<String,ZbnfSyntaxPrescript>(); //ListPrescripts();
    //cc080318 create it at start of parse(): parserStore = new ZbnfParserStore();
    //prescriptParserTopLevel = new PrescriptParser(null, "topLevelSyntax"/*cc080318 , parserStore, null*/); 
    log = new LogParsing(report);
    this.maxParseResultEntriesOnError = maxParseResultEntriesOnError;
    listParseResultOnError = maxParseResultEntriesOnError >0 
                           ? new ArrayList<ZbnfParseResultItem>(maxParseResultEntriesOnError)
                           : null;
  }


  
  /** Sets the syntax from given string.
   * @param syntax The ZBNF-Syntax.
   */
  public void setSyntax(CharSequence syntax)
  throws ParseException
  { setSyntax(new StringPartScan(syntax));
  }

 
  
  /** Sets the syntax from given string. This method should be used in an JZcmd script to distinguish
   * between {@link #setSyntax(File)} and {@link #setSyntax(String)}.
   * @param syntax The ZBNF-Syntax.
   */
  public void setSyntaxString(CharSequence syntax)
  throws ParseException
  { setSyntax(new StringPartScan(syntax));
  }

 
  
  /**Sets the syntax from a file. This method should be used in an JZcmd script to distinguish
   * between {@link #setSyntax(File)} and {@link #setSyntax(String)}.
   * @param fileSyntax The file which contains the syntax prescription.
   * @throws IllegalCharsetNameException
   * @throws UnsupportedCharsetException
   * @throws FileNotFoundException
   * @throws IOException
   * @throws ParseException
   */
  public void setSyntaxFile(File fileSyntax) 
  throws IllegalCharsetNameException, UnsupportedCharsetException, FileNotFoundException, IOException, ParseException 
  { setSyntax(fileSyntax); }
  
  
  public void setSyntax(File fileSyntax) 
  throws IllegalCharsetNameException, UnsupportedCharsetException, FileNotFoundException, IOException, ParseException
  {
    StringPartScan spSyntax = null;
    int lengthFile = (int)fileSyntax.length();
    spSyntax = new StringPartFromFileLines(fileSyntax, lengthFile, "encoding", null);
    String sDirParent = fileSyntax.getParent();
    setSyntax(spSyntax, sDirParent);
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
  public void setSyntax(StringPartScan syntax)
  throws ParseException
  { try{ setSyntax(syntax, null); }
    catch(FileNotFoundException exc){ throw new ParseException("import in ZBNF-script is not supported here.",0); }
    catch(IOException exc){ throw new ParseException("import in ZBNF-script is not supported here.",0); }
  }
  

  
  /**Sets the syntax
   * @param syntax The syntax, may be read from any file or from a String, use new StringPart(...)
   * @param sDirImport If the syntax contains a $import statement, use this directory as current dir to search the file.
   * @throws ParseException
   * @throws IllegalCharsetNameException
   * @throws UnsupportedCharsetException
   * @throws FileNotFoundException
   * @throws IOException
   */
  public void setSyntax(StringPartScan syntax, String sDirImport)
  throws ParseException, IllegalCharsetNameException, UnsupportedCharsetException, FileNotFoundException, IOException
  { List<String> listImports = null;
    /**Temporary store for column. */
    final int[] column = new int[1];
    boolean bSetMainscript = false;
    if(syntax.startsWith("<?SBNF") || syntax.startsWith("<?ZBNF"))
    { syntax.seek("?>", StringPartScan.seekEnd); 
    }
    while(syntax.seekNoWhitespace().length()>0)
    { CharSequence sCurrentInput = syntax.getCurrent(30);
      syntax.scanStart();  //NOTE: sets the restore position for scan error on current position.
      if(StringFunctions.startsWith(sCurrentInput, "$keywords"))
      { syntax.seek(9);  //TODO skip also over ::=
        if(syntax.startsWith("::=")){ syntax.seek(3);}
        else if(syntax.startsWith("=")){ syntax.seek(1);}
        else throw new ParseException("expected \"=\" behind \"$keywords\"", syntax.getLineAndColumn(column));
        char cc;
        do
        { syntax.seekNoWhitespace().lentoIdentifier();
          if(syntax.length()>0)
          { //listKeywords.addNew(syntax.getCurrentPart());
            String sKeyword = syntax.getCurrentPart().toString();
            listKeywords.put(sKeyword, sKeyword);
          }
          cc = syntax.fromEnd().seekNoWhitespace().getCurrentChar();
          syntax.seek(1);
        }while(cc == '|');
        if(cc != '.') throw new ParseException("expected \".\" on end of \"$keywords\"", syntax.getLineAndColumn(column));
      }
      else if(StringFunctions.startsWith(sCurrentInput, "$Whitespaces=")) //##s
      { syntax.seek(12); 
        String sWhitespaces = syntax.getCircumScriptionToAnyChar(".").toString();
        if(sWhitespaces.length()==0 || sWhitespaces.indexOf('\n')>=0){ 
          throw new ParseException("expected \".\" on end of \"$Whitespaces=\"", syntax.getLineAndColumn(column));
        }
        syntax.seek(1);
        setWhiteSpaces(sWhiteSpaces);
      }
      else if(StringFunctions.startsWith(sCurrentInput, "$setLinemode")) //##s
      { syntax.seek(12); 
        if(syntax.getCurrentChar() == '.')
        { syntax.seek(1);
          setLinemode(true);
        }
        else throw new ParseException("expected \".\" on end of \"$setLinemode\"", syntax.getLineAndColumn(column));
      }
      else if(StringFunctions.startsWith(sCurrentInput, "$setXmlSrcline")) //##s
      { syntax.seek(16); 
        if(syntax.getCurrentChar() == '.')
        { syntax.seek(1);
          setXmlSrcline(true);
        }
        else throw new ParseException("expected \".\" on end of \"$setXmlSrcline\"", syntax.getLineAndColumn(column));
      }
      else if(StringFunctions.startsWith(sCurrentInput, "$setXmlSrctext")) //##s
      { syntax.seek(16); 
        if(syntax.getCurrentChar() == '.')
        { syntax.seek(1);
          setXmlSrctext(true);
        }
        else throw new ParseException("expected \".\" on end of \"$setXmlSrctext\"", syntax.getLineAndColumn(column));
      }
      else if(StringFunctions.startsWith(sCurrentInput, "$endlineComment=")) //##s
      { syntax.seek(16); 
        syntax.seekNoWhitespace();
        sEndlineCommentStringStart = syntax.getCircumScriptionToAnyChar(".").toString().trim();
        if(sEndlineCommentStringStart.length()==0){ sEndlineCommentStringStart = null; }
        else if(sEndlineCommentStringStart.length()>5) throw new ParseException("more as 5 chars as $endlineComment unexpected", syntax.getLineAndColumn(column));
        syntax.seek(1);
      }
      else if(StringFunctions.startsWith(sCurrentInput, "$comment=")) //##s
      { syntax.seek(9);
        syntax.seekNoWhitespace();
        sCommentStringStart = syntax.getCircumScriptionToAnyChar(".").toString().trim();
        if(sCommentStringStart.length()==0){ sCommentStringStart = null; }
        else if(sCommentStringStart.length()>5) throw new ParseException("more as 5 chars as $endlineComment unexpected", syntax.getLineAndColumn(column));
        else
        { if(!syntax.startsWith("...")) throw new ParseException("$comment, must have ... betwenn comment strings.", syntax.getLineAndColumn(column));
          syntax.seek(3);
          syntax.seekNoWhitespace();
          sCommentStringEnd = syntax.getCircumScriptionToAnyChar(".").toString().trim();
          if(sCommentStringEnd.length()==0) throw new ParseException("$comment: no endchars found.", syntax.getLineAndColumn(column));
          else if(sCommentStringEnd.length()>5) throw new ParseException("SyntaxPrescript: more as 5 chars as $endlineComment-end unexpected", syntax.getLineAndColumn(column));
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
      else if(StringFunctions.startsWith(sCurrentInput, "##")) //##s
      { syntax.seek('\n', StringPartScan.seekEnd); 
      }
      else if(StringFunctions.startsWith(sCurrentInput, "$main=")) //##s
      { syntax.seek(6); 
        //overwrites a older mainscript, especially the first prescript.
        mainScript = ZbnfSyntaxPrescript.createWithSyntax(syntax, sEndlineCommentStringStart, sCommentStringStart, report);
        listSubPrescript.put(mainScript.getDefinitionIdent(), mainScript);
      }
      else if(StringFunctions.startsWith(sCurrentInput, "$xmlns:")) //##s
      { syntax.seek(7); 
        //overwrites a older mainscript, especially the first prescript.
        String sNamespaceKey = syntax.lento("=").getCurrentPart().toString();
        String sNamespace = syntax.fromEnd().seek(1).lentoQuotionEnd('\"', Integer.MAX_VALUE).getCurrentPart().toString();
        if(sNamespaceKey.length() > 0 && sNamespace.length()>2)
        { if(xmlnsList == null){ xmlnsList = new TreeMap<String, String>(); }
          //NOTE: sNamespace should be have " left and right, do not save it in xmlnsList.
          xmlnsList.put(sNamespaceKey, sNamespace.substring(1, sNamespace.length()-1));
        }
        else throw new ParseException("SyntaxPrescript: $xmlns:ns:\"string\". :failed syntax.", syntax.getLineAndColumn(column));
        if(syntax.fromEnd().getCurrentChar() == '.')
        { syntax.seek(1);
        }
        else throw new ParseException("SyntaxPrescript: $xmlns:ns:\"string\". :no dot on end.", syntax.getLineAndColumn(column));
      }
      else if(syntax.scan("$import").scanOk()) //##s
      { String[] result = new String[1];
        if(  syntax.seekNoWhitespace().scan().scanQuotion("\"", "\"", result).scan(".").scanOk())
        { if(listImports == null){ listImports = new LinkedList<String>(); }
          //listImports.add(result[0]);
          importScript(result[0], sDirImport);
        }
        else throw new ParseException("$import \"importfile\".",0);
      } 
      else
      {
        ZbnfSyntaxPrescript subScript = ZbnfSyntaxPrescript.createWithSyntax(syntax, sEndlineCommentStringStart, sCommentStringStart, report);
        //if(mainScript == null)
        if(!bSetMainscript)
        { bSetMainscript = true;   //first script of this level. A setted mainScript of imported scripts will be overwritten.
          mainScript = subScript;  //the first prescript may be the main.
        }
        String sDefinitionIdent = subScript.getDefinitionIdent();
        if(sDefinitionIdent != null)
        { //may be null, especially if found: ?semantic ::= "explaination". 
          listSubPrescript.put(sDefinitionIdent, subScript);
        }
      }
    }
    if(listImports != null)
    { //this text contains imports, it should be files with absolute or relativ path.
      for(String sFile: listImports)
      { importScript(sFile, sDirImport);
      }
    }
  }


  
  /**Sets another syntax rule as the first entry in the given syntax.
   * This routine should be invoked only with a given syntax, one of the {@link #setSyntax(File)} routines should be called before.
   * @param ident syntax rule, <identifier>::=
   * @throws ParseException
   */
  public void setMainSyntax(String ident) throws ParseException {
    ZbnfSyntaxPrescript rule = this.listSubPrescript.get(ident);
    if(rule == null) throw new ParseException("syntax rule not found: " + ident, 0);
    this.mainScript = rule;
  }
  
  
  
  public ZbnfSyntaxPrescript mainScript() { return mainScript; }
  
  
  /**Returns the index of all sub prescripts for checking. */
  public TreeMap<String,ZbnfSyntaxPrescript> subPrescripts() { return listSubPrescript; }
  
  
  
  private void importScript(String sFile, String sDirParent) 
  throws IllegalCharsetNameException, UnsupportedCharsetException, FileNotFoundException, IOException, ParseException
  {
    String sFileAbs;
    sFileAbs = sDirParent + "/" + sFile;
    File fileImport = new File(sFileAbs);
    setSyntax(fileImport);
  }
  
  
  
  /**Sets info for debug break, see using of {@link #dbgPosFrom} etc.
   * @param from The absolute char positon, not the line, it is outputted on error reports
   * @param to if the current position is between from and to, the break condition met.
   * @param lineSyntax Additional condition: Only if the semantic item on this line is used. It is the definition::= line in the zbnf script.
   *   Use 0 if it should be inactive.
   */
  public void setDebugPosition(int from, int to, int lineSyntax) {
    this.dbgPosFrom = from; this.dbgPosTo = to; this.dbgLineSyntax = lineSyntax;
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
  
  
  /**Sets the line mode or not. The line mode means, a new line character
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
  
  
  
  /**Sets the mode of output source line and column in XML. 
   * This method is equal to the using of the syntax-prescript variable $setSrclineInXml,
   * but after invocation of setSyntax(...) the mode can be changed.
   * @see setSyntax(String).
   * @parameter bValue if true than set the mode of output source line and column in XML, false, then No source line an column output.
   */  
  public void setXmlSrcline(boolean bValue){
    builderTreeNodeXml.bXmlSrcline = bValue;
  }
  
  
  
  /**Sets the mode of output source line and column in XML. 
   * This method is equal to the using of the syntax-prescript variable $setSrctextInXml,
   * but after invocation of setSyntax(...) the mode can be changed.
   * @see setSyntax(String).
   * @parameter bValue if true than set the mode of output source line and column in XML, false, then No source line an column output.
   */  
  public void setXmlSrctext(boolean bValue){
    builderTreeNodeXml.bXmlSrctext = bValue;
  }

  
  
  /**sets the ident number for report of the progress of parsing. 
   * If the idents are  >0 and < MainCmdLogging_ifc.fineDebug, theay are used directly as report level.
   * @param identError ident for error and warning outputs.
   * @param identInfo ident for progress information output.
   * @param identComponent ident for output if a component is parsing
   * @param identFine ident for fine parsing outputs.
   */
  public void setReportIdents(int identError, int identInfo, int identComponent, int identFine)
  {
    idReportParsing = identFine;
    idReportComponentParsing = identComponent;
    idReportInfo = identInfo;
    idReportError = identError;
  }
  
  
  
  /**Parses a given file with standard encoding, produces a parse result.
   * @param fInput
   * @return true if successfully parsed, false then use {@link #getSyntaxErrorReport()}
   * @throws IOException 
   * @throws FileNotFoundException 
   * @throws UnsupportedCharsetException 
   * @throws IllegalCharsetNameException 
   */

  public boolean parseFile(File fInput, int maxBuffer, String sEncodingDetect, Charset charset) throws IllegalCharsetNameException, UnsupportedCharsetException, FileNotFoundException, IOException
  {
    StringPartScan spInput = new StringPartFromFileLines(fInput, maxBuffer, sEncodingDetect, charset); 
    return parse(spInput);
  }



  /**Parses a given file with standard encoding, produces a parse result.
   * @param fInput
   * @return true if successfully parsed, false then use {@link #getSyntaxErrorReport()}
   * @throws IOException 
   * @throws FileNotFoundException 
   * @throws UnsupportedCharsetException 
   * @throws IllegalCharsetNameException 
   */

  public boolean parseFile(File fInput) throws IllegalCharsetNameException, UnsupportedCharsetException, FileNotFoundException, IOException 
  {
    int maxBuffer = 0;  //auto detect, use file size.
    StringPartScan spInput = new StringPartFromFileLines(fInput, maxBuffer, null, null); 
    return parse(spInput);
  }



  /**Parses a given Input and produces a parse result.
   * See {@link #parse(StringPartScan)}</a>.
   *
   * @param input
   * @return
   */

  public boolean parse(String input)
  {
    StringPartScan spInput = new StringPartScan(input);
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
  public boolean parse(StringPartScan input)
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
  public boolean parse(StringPartScan input, List<String> additionalInfo) {
//    nLevelReportParsing = report.getReportLevelFromIdent(idReportParsing);  
//    nLevelReportComponentParsing = report.getReportLevelFromIdent(idReportComponentParsing);  
//    nLevelReportInfo = report.getReportLevelFromIdent(idReportInfo);  
//    nLevelReportError = report.getReportLevelFromIdent(idReportError);  
    nReportLevel = report.getReportLevel(); //the current reportlevel from the users conditions.
    
    //the old parserStore may be referenced from the evaluation, use anyway a new one!
    parserStoreTopLevel = new ZbnfParserStore(); 
    posRightestError = 0; lineError = 0; columnError = 0; sFileError = null;
    sExpectedSyntax = null;
    alreadyParsedCmpn.clear();
    sRightestError = input.getCurrentPart(80); 
    //nRightestLineError = input.getLineAndColumn(null);
    prescriptParserTopLevel = new PrescriptParser(null, mainScript, "topLevelSyntax", input, 0/*cc080318 , parserStore, null*/); 
    //subParserTopLevel = prescriptParserTopLevel.new SubParser(mainScript, null, null, 0);  //true);
    final ZbnfParserStore addParseResult;
    if(additionalInfo != null)
    { addParseResult = new ZbnfParserStore();
      Iterator<String> iterAdditionalInfo = additionalInfo.iterator();
      while(iterAdditionalInfo.hasNext())
      { String addSemantic = iterAdditionalInfo.next();
        String addContent = iterAdditionalInfo.hasNext() ? iterAdditionalInfo.next() : "";
        addParseResult.addString(addContent, addSemantic, null, input, null, -1, -1, null);
        /**NOTE: it is false to : parserStoreTopLevel.addString(addContent, addSemantic, null);
         * because the first item should be a syntax components with all content inside (like XML-toplevel argument).
         * instead use addParseResult as argument for prescriptParserTopLevel.parsePrescript1(...).  
         */
      }
    }
    else
    { addParseResult = null; 
    }
    String sSemantic = mainScript.getDefinitionIdent();
    try
    { boolean bOk = prescriptParserTopLevel.parsePrescript1
                  (sSemantic, null, null, parserStoreTopLevel, addParseResult, false, false, 0);
      return bOk;
    }
    catch(ParseException exc)
    {
      return false;
    }
  }




  protected ZbnfSyntaxPrescript searchSyntaxPrescript(String sSyntax)
  { ZbnfSyntaxPrescript foundItem;
    foundItem = listSubPrescript.get(sSyntax);
    if(foundItem ==null){
      if(idxMissingPrescripts ==null){ idxMissingPrescripts = new TreeMap<String, String>(); }
      idxMissingPrescripts.put(sSyntax, sSyntax);
    }
    return foundItem;
  }



  /** Reports the syntax.*/
  public void reportSyntax(MainCmdLogging_ifc report, int reportLevel)
  {
      mainScript.reportContent(report, reportLevel);
      Iterator<String> iter = listSubPrescript.keySet().iterator();
      while(iter.hasNext())
      { String sName = iter.next();
        ZbnfSyntaxPrescript subSyntax = listSubPrescript.get(sName);
        report.reportln(reportLevel, 0, "");
        subSyntax.reportContent(report, reportLevel);
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
   * Every line is exactly one entry in the parsers store. 
   * 
   * @param report The report output instance
   * @param reportLevel level of report. This level is shown in output. 
   *        If the current valid reportLevel of report is less than this parameter, no action is done.
   */
  public void reportStore(MainCmdLogging_ifc report, int reportLevel, String sTitle)
  { if(report.getReportLevel()>=reportLevel)
    { report.reportln(reportLevel, 0, "== MainCmdLogging_ifc ParserStore " + sTitle + " ==");
      reportStoreComponent(getFirstParseResult(), report, 1, null, reportLevel);
      report.flushReport();
    }  
  }

  public void reportStore(MainCmdLogging_ifc report, int reportLevel)
  { reportStore(report, reportLevel, "");
  }

  /**Reports the whole content of the parse result in the MainCmdLogging_ifc.fineInfo-level. 
   * @see {@link reportStore(MainCmdLogging_ifc report, int reportLevel)}.
   * @param report The report output instance.
   */
  public void reportStore(MainCmdLogging_ifc report)
  { reportStore(report, MainCmdLogging_ifc.fineInfo);
  }

  

  /** Inner method to report the content of the parse result
   * @param parseResultItem The first item to report, it is the next item behind componentes first (head-) item, if it is a component.
   * @param report The report system.
   * @param level Level of nested componentes
   * @param parent If not null, the inner items of parent component are reported.
   * @return The number of written lines.
   * */
  @SuppressWarnings("deprecation")
  private int reportStoreComponent(ZbnfParseResultItem parseResultItem, MainCmdLogging_ifc report, int level, ZbnfParseResultItem parent, int reportLevel)
  { int countLines = 0;
    while(parseResultItem != null)
    { countLines +=1;
      report.reportln(reportLevel, 0, "parseResult: " + sEmpty.substring(0, level) + parseResultItem.getDescription());
      if(parseResultItem.isComponent())
      { //int nLines = 
        reportStoreComponent(parseResultItem.nextSkipIntoComponent(parseResultItem), report, level+1, parseResultItem, reportLevel);
        //if(nLines >1) report.reportln(MainCmdLogging_ifc.info, 0, "parseResult: " + sEmpty.substring(0, level) + "</?" + "> Component");
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
  public StringBuilder buildFoundedInputOnError()
  { StringBuilder u = new StringBuilder(120);
    int column = getInputColumnOnError();
    int line = getInputLineOnError();
    CharSequence sFile = getInputFileOnError();
    u.append("ZbnfParser ERROR ");
    if(sFile !=null){
      u.append(" in file ").append(sFile);
    }
    u.append(" @char-pos: "); 
      u.append(getInputPositionOnError());
      u.append("=0x" + Long.toString(getInputPositionOnError(),16) + " ");
    if(line >0 || column > 0){
      u.append(" @line, col: ").append(line).append(", ").append(column);
    }
    
    u.append(" >>>>>").append(sRightestError);
    if(u.length() < 80){u.append("<<<<end of file"); }
    for(int i=0; i < u.length(); ++i) {
      char cc = u.charAt(i);
      if(cc == '\n' || cc == '\r'){ 
        u.setCharAt(i, '|'); 
      } 
    }
    return u;
  }

  /**Invokes {@link #buildFoundedInputOnError()} with String as return value.
   * If possible use only {@link #buildFoundedInputOnError()} if a CharSequence is sufficient, which are processed in this time.
   */
  public String getFoundedInputOnError(){ return buildFoundedInputOnError().toString(); }
  
  /** Returns the position of error in input string. 
   * It is the same number as in report.
   */
  public long getInputPositionOnError()
  { return posRightestError;
  }

  public int getInputLineOnError(){ return lineError; }
  
  
  public int getInputColumnOnError(){ return columnError; }
  
  public String getInputFileOnError(){ return sFileError; }
  
  
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
    StringBuilder u = buildFoundedInputOnError();
    u.append("\n expected: ----------------------------------------------"); 
    u.append(getExpectedSyntaxOnError());
    u.append("\n found before: ----------------------------------------------"); 
    u.append(( sLastFoundedResultOnError == null 
          ? "-nothing-" 
          : sLastFoundedResultOnError
          ));
    u.append("\n");
    if(idxMissingPrescripts !=null){
      for(Map.Entry<String, String> entry: idxMissingPrescripts.entrySet()){
        u.append("missing prescript: ").append(entry.getValue()).append("\n");
      }
    }
    return u.toString();
  }
  
  
  
  
  
  
  /** Returns the first parse result item to start stepping to the results.
   * See samples at interface ParseResultItem.
   *
   * @return The first parse result item.
   */
  public ZbnfParseResultItem getFirstParseResult()
  {
    if(parserStoreTopLevel.items.size()>0)
    { //parseResult.idxParserStore = 0;
      return parserStoreTopLevel.items.get(0);
    }
    else return null;
  }

  
  
  /**Returns the XML-like result tree.
   * Note that the {@link XmlNodeSimple} can be written as XML textfile or converted to a Java-XML-format (TODO)
   * using {@link @org.vishia.xmlSimple.SimpleXmlOutputter}
   * */
  //public XmlNodeSimple<ZbnfParseResultItem> getResultTree(){
  public XmlNode getResultTree(){
    if(parserStoreTopLevel.items.size()>0)
    { //parseResult.idxParserStore = 0;
    ZbnfParserStore.ParseResultItemImplement firstItem = parserStoreTopLevel.items.get(0);
    //ZbnfParserStore.buildTreeNodeRepresentationXml(null, firstItem, true);
    return firstItem.treeNodeXml;
    }
    else return null;
  }


  public TreeNode_ifc<XmlNodeSimple<ZbnfParseResultItem>, ZbnfParseResultItem> getResultNode(){
    if(parserStoreTopLevel.items.size()>0) { 
      ZbnfParserStore.ParseResultItemImplement firstItem = parserStoreTopLevel.items.get(0);
      assert(firstItem.treeNodeXml instanceof TreeNodeBase);
      @SuppressWarnings("unchecked")
      TreeNodeBase<XmlNodeSimple<ZbnfParseResultItem>, ZbnfParseResultItem, XmlNode> treeNode = 
          (TreeNodeBase<XmlNodeSimple<ZbnfParseResultItem>, ZbnfParseResultItem, XmlNode>)firstItem.treeNodeXml;
      return treeNode;
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


  /**It's a debug helper. The method is empty, but it is a mark to set a breakpoint. */
  private void stop()
  {
  }



  static CharSequence inputCurrent(StringPartScan input)
  {
    StringBuilder u = new StringBuilder(input.getCurrent(40));
    char c;
    for(int i=0; i<u.length(); ++i){
      c=u.charAt(i);
      if(c=='\n'){ u.replace(i, i+1, "|");}
      if(c=='\r'){ u.replace(i, i+1, "-");}
    }
    return u;
  }
  
  
  /**This class contains some information to create a log output which logs the parsing process.
   * @since 2013-01-19, improvement possible.
   *
   */
  static class LogParsing
  { ///
    
    /**Mask of logging activities, @since 2019-05 */
    public int mLogParse;
    /**It logs on any component per one line.*/
    public static int mLogParseCmpn = 0x4;
    /**It logs on any item per one line.*/
    public static int mLogParseItem = 0x8;
    
  
    final StringFormatter line = new StringFormatter(250);
  
    final MainCmdLogging_ifc logOut;
  
    LogParsing(MainCmdLogging_ifc logOut){ this.logOut = logOut;}
    
    void reportParsing(String sWhat, int nReport, ZbnfSyntaxPrescript syntax, String sReportParentComponents
        , StringPartScan input, int posInput, int nRecursion, boolean bOk){
      
      int nrofCharsParsed = (int)(input.getCurrentPosition() - posInput);
      line.reset().add(sWhat).pos(10)
      .addint(nRecursion, "221")
      .addint(posInput,"22221").add('+').addint(nrofCharsParsed, "221").add(": ")
      .addReplaceLinefeed(input.getPart(posInput, 30), "|-||", 30)
      .pos(50)
      .add(bOk ?  "  ok    " : "  error ")
      .add(syntax.toString()).pos(70)
      .add( " in ").add(sReportParentComponents);
      ;
      
      
      logOut.reportln(nReport, line.getContent());
      
    }
  
  
  }




  /**Element of a Parse result for a part of the syntax.
   * It is possible to reuse an instance of a ParseResultlet though the result was 
   * dedicated as false before in a given content. 
   * If another bough in the syntax tree uses the same
   * sub syntax with the same syntax prescript on the same position in text,
   * it is parsed successfully already.
   */
  static class ParseResultlet
  {
    /** The Prescript of the syntax.
     * The parser instance is useable for more as one parsing execution with the same syntax prescript.
     * */
    final ZbnfSyntaxPrescript syntaxPrescript;

    /**The start and the end position (character in parsed Character input) */
    final long startPosText;
    long endPosText;
    
    XmlNode xmlResult;
    
    ParseResultlet(ZbnfSyntaxPrescript syntaxPrescript, long startPosText){
      this.syntaxPrescript = syntaxPrescript;
      this.startPosText = startPosText;
    }

    
    @Override public final String toString(){ return syntaxPrescript.sDefinitionIdent; }
  }
  
  
  
  /**Position where an option should be parsed. 
   * Used only for {@link ZbnfSyntaxPrescript#kAlternativeOptionCheckEmptyFirst}
   *
   */
  static class ForkPoint
  {
    final long posInput;
    
    final int ixPrescript;
    
    final int ixParseResult;
    
    final boolean bSkipSpacesAndComment;
    
    ForkPoint(long posInput, int ixPrescript, int ixParseResult, boolean bSkipSpacesAndComment) { 
      this.posInput = posInput; 
      this.ixPrescript = ixPrescript;
      this.ixParseResult = ixParseResult; 
      this.bSkipSpacesAndComment = bSkipSpacesAndComment;
    }
  }
  
}
