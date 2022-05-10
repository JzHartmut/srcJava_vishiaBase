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
 * @author Hartmut Schorrig www.vishia.org
 *
 ****************************************************************************/
package org.vishia.zbnf;
import java.text.ParseException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.vishia.util.Debugutil;
import org.vishia.util.IndexMultiTable;
import org.vishia.util.StringFunctions;
import org.vishia.util.StringPart;
import org.vishia.util.StringPartScan;

import org.vishia.mainCmd.MainCmdLogging_ifc;

/** This class is a node of the syntax tree. 
 *  <h2>Structure of a node</h2>
 *  A node contains:
 *  <ul><li><code>sSemantic</code>: The semantic of the node. It may be identical with the <code>sDefinitionIdent</code>.</li>
 *  <li><code>sDefinitionIdent</code>: The name of the syntax component if it is any, 
 *    or a formal name for documentation.</li>
 *  <li>Some flags and detail info</li>
 *  </ul>
 *  This class has some inner classes. The coherence with this inner classes is shown in the followed 
 *  object model diagram, <code> <+>---> </code>is a composition,
 * <code> <>---> </code>is a aggregation, <code> <|---- </code>is a inherition.
 *  <pre>
 *
 *
 *           +-----------------------------+  outer     +-----------------------+
 *  from     | SyntaxPrescript             |<---------<>| SyntaxPrescipt.Syntax |
 *  Parser   |-----------------------------|* childs    |-----------------------|
 *  -------->| sSemantic:String            |<-----------|           t           |
 *           | sDefinitionIdent:String     | syntaxList |                       |
 *           |                             |<+>-------->|                       |
 *           |                             |            +-----------------------+
 *           |                             |
 *           |                             |   (super)       +------------------+
 *           |                             |<|-----------+---| ComplexSyntax    |
 *           +-----------------------------+             |   +------------------+
 *                                                       |
 *                                                       |   +------------------+
 *                                                       +---| RepetitionSyntax |
 *                                                           +------------------+
 *</pre>
 * A SyntaxPrescript instance is either a simple instance without childs, than it doesn't contain
 * an instance of its inner class Syntax. This is a terminating node. Or the SyntaxPrescript instance 
 * is a componentwise instance with childs, than a composite instance of Syntax is created and final referenced with
 * <code>syntaxList</code>. A Child may be also one of the inherited classes ComplexSyntax or
 * RepititionSyntax, both based either on a simple or a componentwise instance of SyntaxPrescript.
 * In this manner all of the variants of syntax nodes are implementable.
 * <br>
 * <br>
 * <h2>The possible Syntax given at public method {@link setSyntax(StringPart)}</h2>
 * The method setSyntax creates a tree of such instances by conversion the string given syntax. The syntax
 * processed here is one Syntax term of SBNF, beginning with the identifier and ending with the point:
 * <br>
 * The basic form of syntax is the followed:<br/><br/>
 * <code><i>syntaxident[</i>:<i>[superType]</i>::=</code><i>syntaxprescript</i><code>.</code><br><br/>
 * Pay attention to the point on end of the syntax definition. The <code><i>syntaxident</i></code> is a identifier,
 * like in java or C, written here in italic monospaced font, but the <i>syntaxprescript</i> is a complex expression, written here
 * in italic standard font.<br>
 * The <i>syntaxprescript</i>. may built in a complex way with the followed elements:
 * <table border=1>
 * <tr><td><code>Terminalsymbol</code></td><td>Fix text is written directly, without quotions. The backslash
 *                                             character \ is used to transcript control chars \r \n \t \f
 *                                             like java/C <b>and</b> to transcript the special chars of syntax
 *                                             definition: <code>\[ \] \| \{ \} \. \? \# \&lt; \&gt;</code>. 
 *                                             \a \e means the start and end of text, \s is a space char.</td></tr>
 * <tr><td><i>syntax1</i><code>|</code><i>syntax2</i></td><td>They are alternatives.</td></tr>
 * <tr><td><code>[</code><i>syntax</i><code>]</code></td><td>It is a single option.</td></tr>
 * <tr><td><code>[</code><i>syntax1</i><code>|</code><i>syntax2</i><code>]</code></td><td>It is a choiceable option,
 *                                                              either syntax1 or syntax2 (or more choice possibilities</td></tr>
 * <tr><td><code>[</code><i>syntax1</i><code>|</code><i>syntax2</i><code>|]</code></td><td>It is a choiceable option, but also the empty choice is possible</td></tr>
 * <tr><td><code>[|</code><i>syntax1</i><code>|</code><i>syntax2</i><code>]</code></td><td>First it is tested the syntax behind the option, only if it is not matched, the options are tested.</td></tr>
 * <tr><td><code>[></code><i>syntax1</i><code>|</code><i>syntax2</i><code>]</code></td><td>Ones of the alternatives should match, otherwise the parsing process fails.</td></tr>
 * <tr><td><code>[?</code><i>syntax1</i><code>|</code><i>syntax2</i><code>]</code></td><td>Test whether it is <b>not</b> matched. This is usefull to abort repetitions.</td></tr>
 * <tr><td><code>{</code><i>syntax</i><code>}</code></td><td>Repetition of the <i>syntax</i>, at least one time.</td></tr>
 * <tr><td><code>{</code><i>syntax1</i><code>|</code><i>syntax2</i><code>}</code></td><td>Alternatives in repetition.</td></tr>
 * <tr><td><code>{|</code><i>syntax1</i><code>|</code><i>syntax2</i><code>}</code></td><td>Alternatives in repetition but each alternative only one time. This is not tested while parsing. It creates only a scalar, not a list, for each result item in {@link GenZbnfJavaData}</td></tr>
 * <tr><td><code>{</code><i>syntax</i><code>?</code><i>syntaxBackward</i></code>}</code></td><td>A requested repeat syntax. It is a novum BNF-likely, but a require of praxis.
 *                                       If the <i>syntaxBackward</i> is found, the repetition is required.
 *                                       If the <i>syntaxBackward</i> failed, no repetition is required.
 *                                       <code><i>syntaxBackward</i></code> is the separate-syntax between some repeated morphems.</td></tr>
 * <tr><td><code>&lt;<i>syntaxident</i>&gt;</code></td><td>It is a complex syntax element (morphem). It is defined in an extra definition.</td></tr>
 * <tr><td><code>&lt;<i>syntaxident</i>?<i>semantic</i>&gt;</code></td><td>The semantic ident is defined here, 
 *                                             it is possible to define various semantics by using the same syntax components.</td></tr>
 * <tr><td><code>&lt;</code><i>special</i><code>?</code><i>semantic</i><code>&gt;</code></td><td>Some syntax elements are standard definitions and don't need defined at users level.
 *                                       At example numbers, float numbers, identifier, quotion strings
 *                                       or strings till terminated chars, but not this chars in a quotion.
 *                                       Here are some special symbols, see downside. In generally the semanticident
 *                                       can be absent, than only the syntax ist tested, no output is produced.</td></tr>
 * <tr><td><code>&lt;</code><i>maxnrof</i> <i>special</i><code>?<i>semantic</i>&gt;</code></td><td>The maximal number of chars of a special syntax morphem is limited,
 *                                       at example <code>&lt;32$?NameOfVariable> for an identifier with maximal 32 chars.</td></tr>
 * </table>
   <br/>
 * The <i>special</i> element is one of the followed:
 *
 * <table border = 1 width=100%>
 * <tr><td><code>$</code></td><td>It is a identifier. Its semantic is given with the semanticident.</td></tr>
 * <tr><td><code>$</code><i>AddChars</i></td><td>It is a identifier with additional chars. At example in XML, an identifier
 *                                       may have also the char '<code>-</code>' inside. If the '<code>?</code>' is a additional char, it must be written as '<code>\?</code>'.
 *                                       The transcription with '<code>\</code>' is also active here.</td></tr>
 * <tr><td><code>#</code></td><td>It is a positiv number only with the digits 0..9.</td></tr>
 * <tr><td><code>#-</code></td><td>It is a number with a negative sign or not.</td></tr>
 * <tr><td><code>#x</code></td><td>It is a hexadecimal number</td></tr>
 * <tr><td><code>#f</code></td><td>It is a float number</td></tr>
 * <tr><td><code>""</code><i>endchars</i></td><td>A string in quotion marks. Like in Java or C,
 *                                       inside the string a sequence <code>\"</code> isn't interpreted as end of quotion. </td></tr>
 * <tr><td><code>''</code><i>endchars</i></td><td>A string in single-quotion marks.
 *                                       Inside the string a sequence <code>\'</code> isn't interpreted as end of quotion. </td></tr>
 * <tr><td><code>*</code><i>endchars</i></td><td>It is a string of any chars, but not containing the <i>endchars</i>.
 *                                       Regarding the <i>endchars</i> the same rule for transcriptions with '<code>\</code>' is valid.
 *                                       At example the newline char is '<code>\n</code>' and the char '<code>?</code>'
 *                                       must be written as '<code>\?</code>' because the '<code>?</code>' has an extra meaning in this
 *                                       syntax definition, also the '<code>></code>', written as '<code>\></code>'.</td></tr>
 * <tr><td><code>*|</code><i>endstring</i><code>|</code><i>endstring</i></td><td>between | to ? there are some strings as end strings.
 *                                       All chars are matching until one of the endstring.
 *                                       </td></tr>
 * <tr><td><code>* |</code><i>endstring</i><code>|</code><i>endstring</i></td><td>between | to ? there are some strings as end strings.
 *                                       All chars are matching until one of the endstring. The parse result is trimmed without leading and trailing white spaces.
 *                                       </td></tr>
 * <tr><td><code>*""</code><i>endchars</i></td><td>Any chars exclusively <i>endchars</i>, but if any of the <i>endchars</i> is inside
 *                                       a quution, it is claimed as a valid char, not an endchar.</td></tr>
 * <tr><td><code>*{</code><i>indent</i><code>}|</code><i>endstring</i></td><td>It is a special construct to parse text with indentation.
 *                                       All indentation chars until the column position of the first line are overreaded.
 *                                       See {@link StringPart#lentoAnyStringWithIndent(CharSequence[], CharSequence, int, StringBuilder)} </td></tr>
 * <tr><td><code>!<i>regex</i></code></td><td>Regular expression using java.util.regex.Matcher.lookingAt().</td></tr>
 * </table>
 * <br/>
 *There are also some possibilities to use the result of parsing:
 * <table border = 1 width=100%>
 * <tr><td><code>&lt;...?-...&gt;</code></td>
 *   <td>Store the result in a temporary buffer to assing to a followed component.</td>
 * </tr><tr><td><code>&lt;...?+!...&gt;</code></td>
 *   <td>Position of the parsed before item, into the component. It is not used for parsing itself.
 *   It is especially for the {@link GenZbnfJavaData} because a data element in the destination class is necessary to store the parsed result.</td>
 * </tr><tr><td><code>&lt;...?+...&gt;</code></td>
 *   <td>Assign a stored result, parsed before, into the component.</td>
 * </tr><tr><td><code>&lt;...?*...&gt;</code></td>
 *   <td>Transport to deeper levels, but not assign a stored result into the component.</td>
 * </tr><tr><td><code>&lt;...?&gt;</code></td>
 *   <td>Do not produce a parse result to this components level.</td>
 * </tr><tr><td><code>&lt;...??&gt;</code></td>
 *   <td>The semantic is used from the components definition. It is the same as <code>&lt;...&gt;</code> </td>
 * </tr><tr><td><code>&lt;...?@<i>semantic</i>&gt;</code></td>
 *   <td>Its the same like normal semantic, but in XML it is stored in an attribute.</td>
 * </tr><tr><td><code>::=&lt;?<i>semantic</i>&gt;</code> <code>[&lt;?<i>semantic</i>&gt;</code> <code>{&lt;?<i>semantic</i>&gt;</code></td>
 *   <td>If <code>::=&lt;?</code> is written immediately at start of a syntax term, also inside options and so on,
 *     the given semantic and control possibilities is assigned to this term part. So a shorter form is writeable, at ex:
 *     <code>[&lt;?ident&gt; option]</code> is the same as <code>[&lt;ident&gt;]</code> were <code>ident::= option.</code>
 *     is defined in an extra syntax term.</td>
 * </tr></table>
 * 
 * 
 * 
 * On syntax definition a semantic of the syntax component or a part of syntax may be also given in form<br>
 * <code><i>syntaxident</i>::=&lt;?<i>semantic</i>&gt;</code> <i>syntaxprescript</i>.</code>
 * , also possible on parts of the prescript like
 * <code>[&lt;?<i>semantic[</i>:<i>superType]</i>&gt;... </code>.<br>
 * The construct <code><i>syntaxident</i>::=&lt;?&gt;</code> <i>syntaxprescript</i>
 * means that the syntax component don't create a parse result item, if no special semantic is given at using by <code>&lt;<i>syntax</i>&gt;</code>.
 * 
 * <h2>Examples for syntax</h2>
 * The parser have been chosen for complex users textual data to parse it. The examples are given for the well known
 * programmin language C language, to explain the syntax.<br/>
 * <pre>
   enumExpression::=enum \{ { <$name> [ = [<#-?value>|0x<#x?value|<$symbolnr>] ] | , }.
   typedefinition::=typedef [&lt;enumExpression>|&lt;structuredefinition>|int|float] &lt;$typeident>;. </pre>
 * It is the definition of a enum expression in C. The name of the syntax component is "<code>enumExpression</code>".
 * The string "<code>enum</code>" is a constant text (terminal symbol), also the "<code>{</code>" written as "<code>\{</code>".
 * The followed <code>{</code> is the beginning of a repetition. Inside the repetition there is an identifier
 * with the semantic '<code>name</code>', behind them it may be followed (optional) by a value specification,
 * beginning with the terminate symbol '<code>=</code>', behind them either a number, may be negativ, or the
 * terminate symbols '<code>0x</code>', followed by a hexadecimal number, or an identifier with the semantic
 * '<code>symbolnr</code>'. One of the choice should be given. If behind them a char <code>,</code> is found, the
 * repetition is necessary, on the other hand it is not. The point '<code>.</code>' means, that is it.</br>
 * The next line shows the syntax of a named '<code>typedefinition</code>', it isn't entire.
 * The <code>&lt;enumExpression></code> is used here.
 *
 * <pre>
  adress::= [&lt;title>\s ] [&lt;$firstname>] \s &lt;$lastname>\n
  {&lt;$street_or_house>\s} \s [&lt;#numberOfApartment>] \n
  &lt;5#postalCode> \s &lt;$townOrVillage>\n.</pre>
 * It is a sample of a adress specification. At this no whitespaces are tolerated, otherwise the newline detection
 * is necessary to separate the parts of the adress. Some space charactes, in the syntax definition written as
 * '<code>\s</code>' are possible between the parts of names. But the '<code>\n</code>' is tested to separate the lines
 * of the adress. '<code>&lt;title></code>'  and '<code>&lt;word></code>' are defined in extra definitions.
 *
 * <h2>White space and comment handling in the syntax prescript</h2>
 * As first princip whitespaces in syntax prescript are skipped. The user can write a formular free style. 
 * But every whitespace position in syntax prescript enables a whitespace or comment in the input text to parse. 
 * If no whitespace in the syntax prescript exists, whitespaces in the input text ar not accepted.
 * <br>
 * A Special element <code><$NoWhiteSpaces></code> at start of a syntactical expression supressed the recognizing of
 * whitespaces in the syntax term. So no whitespaces are accepted in input text. But in this case spaces may be tested 
 * with some special constructs especially regular expressions.
 * <br>
 * Comments can be written as
 * end-line-comments with <code>##</code> as start chars. The double <code>##</code> let it able to request a single
 * <code>#</code> as normal terminate char. If in special two ## immediate followed are necessearry as terminal char,
 * the user can write a char sequence of \#\# to prevent that it is interpreted as comment start.
 *
 *       
 */
public class ZbnfSyntaxPrescript
{
  /**Version, history and license.
   * list of changes:
   * <ul>
   * <li>2022-02-10 Hartmut new: {@link #componentSyntax} here existing and set for all syntax rules which uses another syntax component.
   *   It is set in the parser instead searching the syntax component every time again, saves time. 
   *   Secondly it contains some important information, especially the implementation type of a result item.
   *   It is used in {@link ZbnfJavaOutput}.
   * <li>2022-02-10 Hartmut chg since last new: The <code>{<?*semantic:type> </code> writes in {@link #sDefinitionIdent},
   *   because it is the implementation type of this part of repetition. Used for {@link ZbnfJavaOutput}.   
   * <li>2022-02-08 Hartmut new: {@link #sSuperItemType} as a generalizing of a type if items. 
   *   This is set either for a component:superType::=definition or for {<?innerComponent?semantic:Type>....
   *   In the next version TODO: if only one item with the same type is stored in the outer level, it is replaced. 
   * <li>2022-02-07 Hartmut new: regarding {| only as marker for generating {@link GenZbnfJavaData}
   * <li>2022-02-07 Hartmut new: regarding ?+! then this item is {@link EType#kOnlyMarker} and not regarded for parsing. 
   * <li>2019-07-06 Hartmut new: {@link #objid} for debugging only. No functional changes.  
   * <li>2019-05-30 Hartmut new: {@link #childsHasSemantic()} for evaluation in {@link GenZbnfJavaData}
   * <li>2019-05-29 Hartmut new: prepared for &lt;...?*...> for {@link #bEntryComponentContainer}
   * <li>2019-05-25 Hartmut new: possibilities of parsing number: &lt;#8 for any radix, separatorChars in number. 
   * <li>2019-05-22 Hartmut new: {@link #bStoreAsString} and {@link #bDonotStoreData} written with <code>&lt;...?"!"semantic></code>
   * <li>2018-09-09 Hartmut new: {@link #lineFile} element in all Prescripts, using for {@link ZbnfParser#setDebugPosition(int, int, int)}.
   * <li>2018-09-09 Hartmut only formalistic: instead int kSyntaxDefinition etc. now {@link EType} as enum. It is not a functional change
   *   but the EType can contain some more information. It are used for generating a Java Output.java from the syntax proper for {@link ZbnfJavaOutput}.
   * <li>2017-03-25 Hartmut new: Possibility to add attributes to the syntax item. See ZBNF-core-Documentation, using <...?.name=value?...>
   *   new: {@link #getAttribute(String)}. 
   *   Another approach, indent for text, is processed in jzTc. It may be possible to do that in the parser already?
   * <li>2015-12-29 Hartmut new: Possibility for debug: Write <code> <...?%...> in Syntax, then {@link ZbnfSyntaxPrescript#bDebugParsing} is set for this item.
   *  It can be tested here to set a specific debug breakpoint for parsing this element. Only for special debugging problems. 
   * <li>2013-09-07 Hartmut new: Now it distinguishs between terminal symbols which are searched in the comment
   *   and normal terminal symbols. All terminal symbols which are to search in comment starts with one of the comment chars
   *   because elsewhere there won't be found because the comment is skipped. Therefore a terminal symbols is checked
   *   whether it starts with a comment string. The comment string is given by the parser script in the first lines.
   *   There are two types now: {@link #kTerminalSymbolInComment} and {@link #kTerminalSymbol}. 
   * <li>2013-09-07 Hartmut chg: usage of {@link StringPartScan#scanTranscriptionToAnyChar(CharSequence[], String, char, char, char)}
   *   for constant syntax. There was a bug if a non-end character was transcript, especially "\\<\\:" 
   * <li> 2012-10-23 Supports <* |endstring: The parse result is trimmed without leading and trailing white spaces.
   * <li>New_1.10.005 Hartmut 2011-0118: The ZBNF-syntax supports now a semantic ident 
   * in the construct with inner syntax, in the form ,,<""?!innerSyntax?semantic>,,. 
   * See the ZBNF-description. 
   * In the past there should be a own pseudo syntax-component-definition to give a semantic 
   * in form ,,<""?!pseudoComp>...   pseudoComp::=<innerSyntax?semantic>.,, Now it is more easy to apply.
   * <li> 2009-08-20: Hartmut bugfix: "toLastCharIncl:" were skipped over 1 char additionally. fixed.
   * <li> 2009-08-02: Hartmut new: parseExpectedVariant writing [!...] now available. It tests but doesn't processed the content.
   * <li> 2009-03-16: Hartmut new: kFloatWithFactor: noted as <#f*Factor?...> now works.                                                   
   * <li> 2009-03-16: Hartmut new: <toLastChar:chars?...> is an alternative notation of <stringtolastExclChar or <*<<, more simple to read.    
   * <li> 2009-03-16: Hartmut new: <toLastCharIncl:chars?...> is an alternative notation of <stringtolastInclChar oder <+<<, more simple to read.
   * <li> 2009-03-16: Hartmut chg: <...?*... is not supported anymore, it isn't admissible in syntax scripts up to now. 
   * <li>                     It was the functionality isToTransportOuterResults(). But this functionality is too complex, difficult to understand and able to handle.                                 
   * <li> 2006-05-00: Hartmut creation
   * </ul>
   */
  public static final String version = "2022-02-10";
  
  static int objId_ = 1000;
  
  final int objid;
  
  /**For debugging and error report: The line in the syntax file. */
  final int lineFile;

  /** Kind of syntay type of the item */
  EType eType;

  /**To go back for syntax path on error. */
  final ZbnfSyntaxPrescript parent;
  
  /**Identificator of the sub-syntax. It is the part before ::=
   * <ul>
   * <li>If this is a top-level syntax item,
   * it is the identification disposed at <code><i>sDefinitionIdent</i>:==...</code>.</li>
   * <li>If this is a superclass of ComplexSyntax, and the eType == kComplexItem,
   * it is the name of the required syntax definition,
   * disposed at <code>&lt;<i>syntax</i>?...&gt;</code>.</li>
   * <li>If this is a special ComplexSyntax, it is set to a reportable string,
   * not used for process.</li>
   * </ul>
   */
  protected String sDefinitionIdent;

  /**The semantic of this syntax item or null. It is usually the String after: <code>&lt;...?semantic></code>.
   * <ul>
   * <li>If the item has not a special semantic but the name of the syntax should be used
   * (written <code>&lt;cmpn></code>), this field contains "@".
   * <li>If the item has a special semantic <code>&lt;cmpn?semantic></code> it is contained here.
   * <li>If the item is written <code>&lt;cmpn?></code> this field contains null. 
   *   In this case an own data container for this component should not be produced, 
   *   the data of the component is written in the calling component's data. .
   * <li>for an option or repetition it may be the semantic of the parsed string of this whole item (with children):
   * <code>[&lt;?semantic> ... ]</code> or {&lt;?semantic> ... }
   * <li>If this is an syntax definition for a component <code>cmpn::=...</code> either the semantic
   *   is equal the components name
   * <li>or it is a given semantic with <code>cmpn::=&lt;?semantic>...</code> 
   *   Then this semantic should be used instead the semantic in the calling item if it contains only "@"
   * <li>or it is null if <code>cmpn::=&lt;?>...</code> is given for the component. 
   *   In this case an own data container for this component should not be produced, 
   *   the data of the component is written in the calling component's data.
   * </ul>    
   */
  protected String sSemantic;
  
  
  
  /**This element is only set for component syntax call and also on {&lt;?semantic:component>
   * Only if the element is used, then one time searched. 
   * It means for all used {@link ZbnfParserStore.ParseResultItemImplement#parentSyntaxElement}
   * it is referenced it it is a component. (since 2022-02)
   */
  protected ZbnfSyntaxPrescript componentSyntax;
  
  /**If set the parse result and all sub results are not stored as data. 
   * They may be usually stored as {@link #bStoreAsString}*/
  boolean bDonotStoreData;
  
  /**If set the parse result for this item is stored as String from the source immediately. */
  boolean bStoreAsString;

  /** if it is set, the semantic of this component should be assigned into the next component
   * of the outer prescript.
   */
  protected boolean bAssignIntoNextComponent = false;

  
  /**If it is set this result is stored in a container (List). 
   * If the component is successfully parsed the component is stored multiple for each result in this container.  */
  protected boolean bEntryComponentContainer = false;
  
  
  /** see quest method.*/
  protected boolean bAddOuterResults = false;

  /**Either List of all syntax items one after another of this node
   * or List of all alternatives if this is an alternative syntax node.
   * It is an alternative syntax node if( (eType & (kAlternative | kAlternativOption...) !=0).
   *  */
  List<ZbnfSyntaxPrescript> childSyntaxPrescripts;

  /**If it is set, the childSyntaxPrescripts contains some alternative syntax prescripts,
   * otherwise childSyntaxPrescripts contain the items one after another of this syntax prescript.
   * alternative syntax prescripts are produced if some ...|...|... are present in source.
   */  
  boolean bChildSyntaxAreAlternatives = false;
  
  /** List of alternatives of this node, the list contains ArrayList of SyntaxPrescrips.
   * If the alternatives is not null, all childs are inside the alternatives.
   * The attribute childs than holds
   * only the last scanned alternative. If the attribute is null, there aren't
   * alternatives. Therefor the childs hold the only one syntaxprescript of this node.
   */
  //List<ZbnfSyntaxPrescript> listAlternatives;

  /** If it is true, it is also matching if no prescript matches to the parsed input.*/
  private boolean alsoEmptyOption;

  /** A string accordingly to the syntax. The meaning depends on the type of prescript,
   * for example the constant string for terminal characters.
   */
  protected String sConstantSyntax;

  /**A string accordingly to the syntax. 
   * If it is set then it is the name of a super class of the stored data.
   * Data with the same super class can be changed together. 
   */
  protected String sSuperItemType;

  /** List of strings used by kStringUntilEndString or null if not used. */
  protected List<String> listStrings;

  /** Float-Factor see attribute kFloatToInt */
  double nFloatFactor = 1.0;

  /** Identification number, auto generated, to store in the founded users syntax tree.*/
  protected int nodeIdent;

  /**0 or a usual negative number to define the indentation for a parsed text.
   * For example in jzTc:
   * \\<:\\><textExpr?|-3> The indentation is -3 shift left from <textExpr>
   */
  //int indentDiff;
  
  /**If not null, than attributes of this item. */
  Map<String, String> attributes = null;

  /**
   * Required sub-syntax of a syntax component with string result.
   * If this is a special ComplexSyntax with a string result,
   * it is the name of the required syntax definition to parse implicitly the string,
   * disposed at <code>&lt;*...?!<i>syntax</i>&gt;</code>.
   */
  protected String sSubSyntax = null;

  /** Indent chars if it is a &lt;*{...}?...>-construct.*/
  protected String sIndentChars = null;

  /**Comment start to check whether parse terminal syntax as comment. */
  private final String sCommentStart1, sCommentStart2;
  
  /**Set to stop parsing on this item. Write <...?%...> to set it. Set breakpoint where bDebugParsing was set or checked.  */
  public boolean bDebugParsing;
  
  /** The syntax of this element.*/
  //final Syntax syntaxLists;

  /** MainCmdLogging_ifc something*/
  final MainCmdLogging_ifc report;


  enum EType {
    kNotDefined (0)
    /** Top level item of a syntaxdefinition.*/
  , kSyntaxDefinition(1)
    /**constant text in source. */
  , kTerminalSymbol(2)
  /**constant text in source in a comment part. */
  , kTerminalSymbolInComment('/')
  , kSkipSpaces ( 3 ) //' '

  /** One alternative of the syntax prescript must be matched.*/
  , kAlternative   ( 4 )  //'|'

  /** No or one alternative of the syntax prescript must be matched.*/
  , kAlternativeOption   ( 5 )  //'['

  /** First it is to be tested wether the followed syntax behind this prescript
   *  does match, only if not, this syntaxprescript is to be tested.
   */
  , kAlternativeOptionCheckEmptyFirst   ( 6 ) //']'

  /** Simple option is to be tested, if it is not matched, it is ok.*/
  , kSimpleOption   ( 7 )  //'o'

  /** one or more negative variants. If there are matched, it is false.
   *  The use of this possibility is able especially to break in repetitions.
   */
  , kNegativVariant ( 8 )  //'?'

  /**Designation of a option written as <code>[>....]</code>
   * If the syntax inside square brackets doesn't match, the whole parsing process is aborted.
   */
  , kUnconditionalVariant ( '>' )  //60 ( 0x3c
  
  /**Designation of a option written as <code>[!....]</code>
   * The syntax inside is expected, but not converted.
   */
  , kExpectedVariant ( '!' )  //33 ( 0x21
  
  , kRepetition ( 9, '{' )  //'{'

  , kRepetitionRepeat ( 10 )  //'}'

  /** This enum marks, that this item is nor a syntax item.
   * Only a semantic is defined. Written with <code>&lt;?semantic&gt;</code> but not on start of option
   * (not for <code>[&lt;?semantic&gt; ... ]</code>) 
   * The last one is stored as semantic on {@value #kAlternativeOption} or {@link #kSimpleOption}
   */
  , kOnlySemantic  ( 11 )  //'?'  <?semantic> not for [<?semenatic>...]

  /** This enum marks, that the syntax is defined with another definition.
   * The identifier of the definition is got with getSyntaxFromComplexItem(Object) )
   */
  , kSyntaxComponent   ( 12, '=' ),    //'='

  
   /**Number with any radix
   * 
   */
   kNumberRadix  (14, 'I')
  
  /** This enum marks, that the syntax of the token should be a float number,
   * but it should be converted to an integer.
   * The syntax of this is [-]<#?integer>[\.<#?fractional>][[E|e][+|-|]<#?exponent>].
   */
  , kFloatWithFactor ( 15, 'F' )    //'F'

  /** This enum marks, that the syntax of the token should be a positive number.
   * It is a string only with characters '0' to '9'.
   */
  , kPositivNumber ( 16, 'I' )    //'d'
  /** This enum marks, that the syntax of the token should be a positive or negative number.
   * At first a '-' is optional. The rest is a string only with characters '0' to '9'.
   */
  , kIntegerNumber ( 17, 'I' )  //'i'
  /** This enum marks, that the syntax of the token should be a hexadecimal number.
   * It is a string only with characters '0' to '9', 'A' to 'F' or 'a' to 'f'.
   */
  , kHexNumber ( 18, 'I' )  //'x'
  /** This enum marks, that the syntax of the token should be a float number.
   * The syntax of this is [-]<#?integer>[\.<#?fractional>][[E|e][+|-|]<#?exponent>].
   */
  , kFloatNumber ( 19, 'F' )  //'f'

  /** This enum marks, that the syntax of the token should be an identifiert.
   * The method getSyntaxFromComplexItem(Object) supplies a list of extra characters
   * there should be also accepted as identifier characters )
   */
  , kIdentifier    ( 20, 's' )  //0x14  //'$'

  /**Some constants used in switch-case to mark the search type.
   */
  , kStringUntilEndString   ( 0x15, 's' ) //'S'
  , kStringUntilEndchar     ( 0x16, 's' ) //'c'
  , kStringUntilEndcharOutsideQuotion  ( 0x17, 's' ) //'+'
  , kStringUntilEndcharWithIndent     ( 0x18, '\t' ) //'t'
  , kStringUntilEndStringWithIndent ( 0x19, '\t' ) //'T'
  , kQuotedString ( 0x1a, 's' ) //'\"'
  , kStringUntilRightEndchar     ( 0x1b, 's' ) //'E'
  , kRegularExpression ( 0x1c, 's' ) //'%'
  , kStringUntilEndStringTrim ( 0x25, 's' ) //'R'
  , kStringUntilEndStringInclusive   ( 0x35, 's' ) //'I'
  , kStringUntilEndcharInclusive     ( 0x36, 's' ) //'C'
  , kStringUntilRightEndcharInclusive     ( 0x3b, 's' ) //'H' 
  , kOnlyMarker ( 0x3f, '-')
  ;
    int k;
    /**The type of the item for storing.
     * <ul>
     * <li> 0 only syntax control, no result.
     * <li>FIs for Float, Integer, String
     * <li>'{' for repetition start
     * <li>'=' syntax component
     * <li>'\t' tab 0x09 for indented String
     * </ul>
     */
    char type;
    EType(int k, char type){
      this.k = k;
      this.type = type;
    }

    EType(int k){
      this.k = k;
      this.type = '\0';
    }
  }
  
  

  /** The class contains the execution method */
  private static class ComplexSyntax extends ZbnfSyntaxPrescript
  {
    /** Maximal number of chars representing the item.*/
    int nMaxChars = Integer.MAX_VALUE;

    /**If it is a Regular Expression, the compiled regex is available here. */
    Pattern regex = null;

    ComplexSyntax(ZbnfSyntaxPrescript parent, MainCmdLogging_ifc report, boolean bWithSyntaxList, int linefile)
    { super(parent, report, bWithSyntaxList, linefile);
    }


    /** Sets the content of the syntax prescript for a Syntax Component
     * with a string given syntax. The syntax of the syntax given in SBNF is <pre>
     * \&lt;[<#?maxNrOfChars>]
     * [ #[-|x|X|f|]?nrFormat><?number>
     * | $<?identifier>
     * | !<*\??regExpression>
     * | *<*\??charsToEndchar>
     * | *""<*\??charsToEndcharButNotInQuotions>
     * | ""<*\??Literal>
     * | <$?otherPrescript>
     * | <?onlySemantic>
     * ]
     * [\?[+|-|][!]<$?semantic>]
     * \&gt;
     * </pre>
     * where is
     * <table>
     * <tr><td>maxNrOfChars</td>The first digits represent the maximum nr of chararcters
     *                          in source they are considered on scanning.</td></tr>
     * <tr><td>...</td><td>...</td></tr>
     * </table>
     * @param spInput syntaxPrescript in textual form
     * @throws ParseException
     */
    void convertSyntaxComponent(StringPartScan spInput)
    throws ParseException
    { char cc;
      if(spInput.getCurrentPosition() == 2957 )
        Debugutil.stop();
      { //TRICKY: sDefinitionIdent and eType blanketed the class variable, to test assignment from compiler in all branches.
        String sDefinitionIdent;
        EType eType;
        cc = spInput.getCurrentChar();
        if(cc == '+') {
          //this.bRequired = true;
          //It is a possible special designation, yet without function.
          cc = spInput.seekPos(1).getCurrentChar();
        }
        if( cc >='0' && cc <='9')
        { nMaxChars = 0;
          do
          { nMaxChars = 10* nMaxChars + (cc-'0');
            spInput.seek(1);
            cc = spInput.getCurrentChar();
          } while( cc >='0' && cc <='9');
        }
        { spInput.lentoAnyChar("?>");  //For syntax detection only the String till ? or till > is relevant. (change 2019-05-15, save memory and calc time).  
          String sTest = spInput.getCurrentPart().toString();
          spInput.setLengthMax();
          cc = spInput.getCurrentChar();
          if(cc == '#') { 
            cc = spInput.seekPos(1).getCurrentChar();  //may read the following ?
            if(cc >='0' && cc <='9') {
              if(spInput.scanStart().scanPositivInteger().scanOk()) {
                eType = EType.kNumberRadix; sDefinitionIdent = "i-NumberRadix"; 
                //spInput.(1); 
                this.nFloatFactor = spInput.getLastScannedIntegerNumber();
              }
            }
            switch(cc)
            { case 'X': eType = EType.kHexNumber;     sDefinitionIdent = "i-HexNumber"; spInput.seekPos(1); break;
              case 'x': eType = EType.kHexNumber;     sDefinitionIdent = "i-HexNumber"; spInput.seekPos(1); break;
              case '-': eType = EType.kIntegerNumber; sDefinitionIdent = "i-IntegerNumber"; spInput.seekPos(1); break;
              case 'f': 
              { eType = EType.kFloatNumber;   
                sDefinitionIdent = "i-FloatNumber"; 
                spInput.seekPos(1);
                if(spInput.scanStart().scan("*").scanFloatNumber().scanOk())
                { nFloatFactor = spInput.getLastScannedFloatNumber();
                  eType = EType.kFloatWithFactor;
                  sDefinitionIdent = "i-FloatFactor(" + nFloatFactor + ")"; 
                }
                else
                { eType = EType.kFloatNumber;   
                  sDefinitionIdent = "i-FloatNumber"; 
                }  
              } break;
              default:  eType = EType.kPositivNumber; sDefinitionIdent = "i-PositivNumber"; break;
            }
            spInput.lentoAnyChar("?>");  //after the num syntax
            if(spInput.length() >0) { //any additional characters as separator in num
              this.sConstantSyntax = spInput.getCurrent().toString();
              spInput.fromEnd();
            }
            spInput.setLengthMax();  //necessary for following scan!
          }
          else if(cc == '$')
          { eType = EType.kIdentifier;
            sDefinitionIdent = "i-Identifier";
            spInput.seek(1);
            sConstantSyntax = spInput.getCircumScriptionToAnyChar("?>").toString();
          }
          else if(cc == '!')
          { eType = EType.kRegularExpression;
            sDefinitionIdent = "i-RegularExpression";
            spInput.seek(1);
            sConstantSyntax = spInput.getCircumScriptionToAnyChar("?>").toString();
            try{ regex = Pattern.compile(sConstantSyntax); }
            catch(PatternSyntaxException exc)
            { throw new ParseException("failed regex syntax:" + sConstantSyntax,0);
            }
          }
          else if(sTest.startsWith("*|"))
          { eType = EType.kStringUntilEndString;
            sDefinitionIdent = "i-StringUntilEndString";
            spInput.seek(2); //read sConstantSyntax from "|"
            listStrings = new LinkedList<String>();
            boolean bContinue = true;
            while(bContinue)
            { sConstantSyntax = spInput.getCircumScriptionToAnyChar("|?>").toString();
              listStrings.add(sConstantSyntax);
              if(spInput.getCurrentChar() == '|')
              { spInput.seek(1);
              }
              else bContinue = false;
            }
          }
          else if(sTest.startsWith("* |"))
          { eType = EType.kStringUntilEndStringTrim;
            sDefinitionIdent = "i-StringUntilEndStringTrim";
            spInput.seek(3); //read sConstantSyntax from "|"
            listStrings = new LinkedList<String>();
            boolean bContinue = true;
            while(bContinue) { 
              CharSequence[] dst = new CharSequence[1];
              if(spInput.scanStart().scanToAnyChar(dst, "|?>", '\\', '\0', '\0').scanOk()){
                sConstantSyntax = dst[0].toString();
                //sConstantSyntax = spInput.getCircumScriptionToAnyChar("|?>").toString();
                listStrings.add(sConstantSyntax);
                if(spInput.getCurrentChar() == '|')
                { spInput.seek(1);
                }
                else bContinue = false;
              } else {
                throwParseException(spInput, "ZbnfSyntaxPrescript.convertSyntaxComponent - <* |...argument error");
              }
            }
          }
          else if(sTest.startsWith("+|"))
          { eType = EType.kStringUntilEndStringInclusive;
            sDefinitionIdent = "i-StringUntilEndStringInclusive";
            spInput.seek(2); //read sConstantSyntax from "|"
            listStrings = new LinkedList<String>();
            boolean bContinue = true;
            while(bContinue)
            { sConstantSyntax = spInput.getCircumScriptionToAnyChar("|?>").toString();
              listStrings.add(sConstantSyntax);
              if(spInput.getCurrentChar() == '|')
              { spInput.seek(1);
              }
              else bContinue = false;
            }
          }
          
          else if(sTest.startsWith("*<<"))
          { eType = EType.kStringUntilRightEndchar;
            sDefinitionIdent = "i-StringUntilRightEndChar";
            spInput.seek(3); //read sConstantSyntax from "|"
            sConstantSyntax = spInput.getCircumScriptionToAnyChar("?>").toString();
          }
          else if(sTest.startsWith("stringtolastExclChar"))
          { eType = EType.kStringUntilRightEndchar;
            sDefinitionIdent = "i-StringUntilRightEndChar";
            spInput.seek(20); //read sConstantSyntax from "|"
            sConstantSyntax = spInput.getCircumScriptionToAnyChar("?>").toString();
          }
          else if(sTest.startsWith("toLastChar:"))
          { eType = EType.kStringUntilRightEndchar;
            sDefinitionIdent = "i-StringUntilRightEndChar";
            spInput.seek(11); //read sConstantSyntax from "|"
            sConstantSyntax = spInput.getCircumScriptionToAnyChar("?>").toString();
            //spInput.seek(1);  //skip over ), ?> will be accept later.
          }
          
          else if(sTest.startsWith("+<<"))
          { eType = EType.kStringUntilRightEndcharInclusive;
            sDefinitionIdent = "i-StringUntilRightEndCharInclusive";
            spInput.seek(3); //read sConstantSyntax from "|"
            sConstantSyntax = spInput.getCircumScriptionToAnyChar("?>").toString();
          }
          else if(sTest.startsWith("stringtolastinclChar"))
          { eType = EType.kStringUntilRightEndcharInclusive;
            sDefinitionIdent = "i-StringUntilRightEndCharInclusive";
            spInput.seek(20); //read sConstantSyntax from "|"
            sConstantSyntax = spInput.getCircumScriptionToAnyChar("?>").toString();
          }
          else if(sTest.startsWith("toLastCharIncl:"))
          { eType = EType.kStringUntilRightEndcharInclusive;
            sDefinitionIdent = "i-StringUntilRightEndCharInclusive";
            spInput.seek(15); //read sConstantSyntax from "|"
            sConstantSyntax = spInput.getCircumScriptionToAnyChar("?>").toString();
            //spInput.seek(1);  //skip over ), ?> will be accept later.
          }
          
          else if(sTest.startsWith("*{"))
          { spInput.seek(2); //read sConstantSyntax from "|"
            sIndentChars = spInput.getCircumScriptionToAnyChar("}").toString();
            if(!spInput.found()) throwParseException(spInput, "\"}\"expected");
            spInput.seek(1);
            cc=spInput.getCurrentChar();
            if(cc == '|')
            { eType = EType.kStringUntilEndStringWithIndent; //##k
              sDefinitionIdent = "i-StringUntilEndStringWithIndent";
              spInput.seek(1); //read sConstantSyntax from "|"
              listStrings = new LinkedList<String>();
              boolean bContinue = true;
              while(bContinue)
              { sConstantSyntax = spInput.getCircumScriptionToAnyChar("|?>").toString();
                listStrings.add(sConstantSyntax);
                if(spInput.getCurrentChar() == '|')
                { spInput.seek(1);
                }
                else bContinue = false;
              }
            }
            else
            { eType = EType.kStringUntilEndcharWithIndent; //##k
              sDefinitionIdent = "i-StringUntilEndcharWithIndent";
              spInput.seek(1);
              sConstantSyntax = spInput.getCircumScriptionToAnyChar("?>").toString();
            }
          }
          else if(sTest.startsWith("*\"\""))
          { eType = EType.kStringUntilEndcharOutsideQuotion;
            sDefinitionIdent = "i-StringUntilEndcharOutsideQuotion";
            spInput.seek(3);
            sConstantSyntax = spInput.getCircumScriptionToAnyChar("?>").toString();
          }
          else if(cc == '*')
          { eType = EType.kStringUntilEndchar;
            sDefinitionIdent = "i-StringUntilEndChar";
            spInput.seek(1);
            sConstantSyntax = spInput.getCircumScriptionToAnyChar("?>").toString();
          }
          else if(cc == '+')
          { eType = EType.kStringUntilEndcharInclusive;
            sDefinitionIdent = "i-StringUntilEndCharInclusive";
            spInput.seek(1);
            sConstantSyntax = spInput.getCircumScriptionToAnyChar("?>").toString();
          }
          else if(sTest.startsWith("\"\"")) ////
          { eType = EType.kQuotedString;
            sConstantSyntax = sTest;  //till excl. ? or > with transcript char, @since 2019-05-15  "\"\"";
            sDefinitionIdent = "i-QuotedString";
            spInput.seekPos(sTest.length());
          }
          else if(sTest.startsWith("\'\'"))
          { eType = EType.kQuotedString;
            sDefinitionIdent = "i-QuotedString";
            sConstantSyntax = sTest;  //till excl. ? or > with transcript char, @since 2019-05-15  "\'\'";
            spInput.seekPos(sTest.length());
          }
          else if(cc == '?')
          { eType = EType.kOnlySemantic;
            sDefinitionIdent = "i-Semantic";
          }
          else
          { spInput.lentoIdentifier();
            if(spInput.length()>0)
            { eType = EType.kSyntaxComponent;
              sDefinitionIdent = spInput.getCurrentPart().toString();
              spInput.fromEnd();
            }
            else
            { sDefinitionIdent = null;
              eType = EType.kOnlySemantic;
            }
          }
        }
        this.sDefinitionIdent = sDefinitionIdent;
        this.eType = eType;
      }

      sSemantic = "@";  //default: use the semantic of the component if no special setting behind ? (<...?Semantic>)
      while( (cc = spInput.getCurrentChar()) == '?')  //following semantic
      { spInput.seekPos(1);
        if(spInput.getCurrentChar() == '.') {
          getAttribute(spInput);
        } else {
          getSemantic(spInput);  //Note: more as one semantic, the last wins.
        }
      }
      
      if( (cc = spInput.getCurrentChar()) == '>')
      { spInput.seek(1);
      }
      else
      { throwParseException(spInput, "\">\" expected");
      }
    }


  }


  public static class RepetitionSyntax extends ZbnfSyntaxPrescript
  { /** Syntax of the forward path is this*/
    
    /**Set on {| not tested in zbnf, only for Store in Java*/
    boolean bOnlyOneEachOption;
    /** Syntax of the repetition path */
    ZbnfSyntaxPrescript backward;

    RepetitionSyntax(ZbnfSyntaxPrescript parent, MainCmdLogging_ifc report, boolean bWithSyntaxList, int linefile)
    { super(parent, report, bWithSyntaxList, linefile);
    }
  }

  /** Constructor only fills the data.*/
  private ZbnfSyntaxPrescript(ZbnfSyntaxPrescript parent, MainCmdLogging_ifc report, boolean bWithSyntaxList, int linefile)
  { this.objid = ++objId_;
//    if(objid == 2813)
//      Debugutil.stop();
    this.report = report;
    this.lineFile = linefile;
    this.sCommentStart1 = parent.sCommentStart1;
    this.sCommentStart2 = parent.sCommentStart2;
    
    this.parent =parent;
    //syntaxLists = bWithSyntaxList ? new Syntax() : null;
  }

  /** Constructor only fills the data.*/
  private ZbnfSyntaxPrescript(MainCmdLogging_ifc report, boolean bWithSyntaxList, String sCommentStart1, String sCommentStart2, int linefile)
  { this.objid = ++objId_;
//    if(objid == 2813)
//      Debugutil.stop();
    this.report = report;
    this.lineFile = linefile;
    this.sCommentStart1 = sCommentStart1;
    this.sCommentStart2 = sCommentStart2;
    
    this.parent = null;
    //syntaxLists = bWithSyntaxList ? new Syntax() : null;
  }

  /** Constructor only fills the data.*/
  ZbnfSyntaxPrescript(ZbnfSyntaxPrescript parent, EType type, int linefile)
  { this.objid = ++objId_;
//    if(objid == 2813)
//      Debugutil.stop();
    eType = type;
    this.lineFile = linefile;
    this.sCommentStart1 = parent ==null ? null : parent.sCommentStart1;
    this.sCommentStart2 = parent ==null ? null : parent.sCommentStart2;
    this.parent =parent;
    //syntaxLists = null;
    report = null;
  }
  

  /*
  public static SyntaxPrescript createWithSyntax(String sInput, MainCmdLogging_ifc report)
  throws ParseException
  { return createWithSyntax(new StringPart(sInput), report);
  }
  */
  
  /** Creates an tree of instance with the given syntax. If any parse error occurs
   * by converting the syntax, a ParseException is thrown. 
   * The input is typically a part of a longer text, the position should be
   * the beginning of the indent of the syntax term. The position in the Stringpart
   * is setted on finishing behind the ending point of the syntax term. So next parts
   * may be parsed outside this method, like other syntax terms or variables.
   * <br> 
   * @param spInput The syntax as part of a longer text. See {@link Parser.setSyntax(StringPart)}.
   * @param report To report something.
   * @return the created root instance
   * @throws ParseException on error of input syntax. The message of the exception
   *         contains a information about the error position.
   */  
  static ZbnfSyntaxPrescript createWithSyntax(StringPartScan spInput, String sCommentStart1, String sCommentStart2, MainCmdLogging_ifc report)
  throws ParseException
  {
    ZbnfSyntaxPrescript ret = new ZbnfSyntaxPrescript(report, true, sCommentStart1, sCommentStart2,0);
    ret.convertSyntaxDefinition(spInput);
    return ret;
  }
  
  
  
  /** Converts the semantic behind a ? in a <..?..>-Expression. 
   * @param spInput The input string, the actual position is behind the '?'
   *                in a expression <..?...:...>. 
   *                The actual position after work is at the '>'.
   * */
  void getSemantic(StringPartScan spInput)
  { //TRICKY: sSemantic blanketed the class variable, to test assignment from compiler in all branches.
    String sSemantic;
    boolean bSemantic = true;
    char cc = spInput.getCurrentChar();

//    if(sDefinitionIdent !=null && sDefinitionIdent.equals("identifierWithPrefix"))
//      Debugutil.stop();
    
    
    if( cc == '%')
    { this.bDebugParsing = true;
      cc = spInput.seekPos(1).getCurrentChar();
    }
    //
    if( cc == '*') { 
      this.bEntryComponentContainer = true;
      cc = spInput.seekPos(1).getCurrentChar();
    } else if( cc == '-') { 
      this.bAssignIntoNextComponent = true;
      cc = spInput.seekPos(1).getCurrentChar();
    } else if(cc == '+') { 
      cc = spInput.seekPos(1).getCurrentChar();
      if(cc == '!') {
        cc = spInput.seekPos(1).getCurrentChar();
        eType = EType.kOnlyMarker;
      } else {
        this.bAddOuterResults = true;
      }
      //bTransportOuterResults = true;
    } else if(cc == '"') {                          
      //<Syntax?"!"semantic>: store the input which is parsed.
      this.bStoreAsString = true;
      cc = spInput.seekPos(1).getCurrentChar();
      if(cc == '!') { 
        this.bDonotStoreData = true;
        cc = spInput.seekPos(1).getCurrentChar();
      }
      if(cc == '\"') { //skip over a second ", it is only used formally.
        cc = spInput.seekPos(1).getCurrentChar();
      }
    }
    //
    if( cc  == '!')
    { //call of an inner parsing
      spInput.seek(1).lentoIdentifier();
      if(spInput.length()>0)
      { this.sSubSyntax = spInput.getCurrentPart().toString();
      }
      else ; //no sDefintionIdent and no Semantic
      spInput.fromEnd();
      cc = spInput.getCurrentChar();
      if(cc=='?'){ cc=spInput.seek(1).getCurrentChar();  //the first char of semantic
      } else { bSemantic = false; }  //no second ?, cc should be '>'
    }
    if(bSemantic){
      if( cc == '?')   //this is a second ?, written as <syntax??>. Then is is equal <syntax?@> 
      { spInput.seek(1);
        sSemantic = "@";  // use the semantic of the component if no special setting behind ? (<...?Semantic>)
      }
      else
      { //behind ? the semantic is defined. It may be a null-Semantic.
        spInput.lentoAnyChar(":?>");
        if(spInput.length()>0)
        { sSemantic = spInput.getCurrentPart().toString();
//          if(sSemantic.equals("@")) {   TODO yet a problem with XML output see ZBNF/testAllFeatures
//            sSemantic = this.sDefinitionIdent;  //same as syntax.
//          }
        }
        else
        { sSemantic = null;  //<..?>: without semantic
        }
        spInput.fromEnd();
      }
    } else {
      sSemantic = null;  //<...?!subSyntax>
    }
    this.sSemantic = sSemantic;
    if(sSemantic != null && sSemantic.equals("return"))
      stop();
    char cNext = spInput.getCurrentChar();
    if(cNext==':') {
      spInput.seekPos(1).lento(">");
      this.sDefinitionIdent = spInput.getCurrentPart().toString();
      spInput.fromEnd();
    }
  }
  
  
  
  void getAttribute(StringPartScan spInput)
  throws ParseException
  {
    spInput.seekPos(1).lentoIdentifier();
    String name = spInput.getCurrentPart().toString();
    if(name.length() ==0) { throwParseException(spInput, "semantic attribute identifier expected"); }
    char cc = spInput.fromEnd().getCurrentChar();
    if(cc !='='){ throwParseException(spInput, "attribute value expected, note: no spaces between @name=value"); }
    spInput.seekPos(1).lentoAnyChar("?>");
    if(!spInput.found()){ throwParseException(spInput, "> or ? missed for attribute value"); }
    CharSequence svalue = spInput.getCurrentPart();
    spInput.fromEnd();
    String value;
    if(svalue.length() >=2 && svalue.charAt(0) == '\"') {
      value = svalue.subSequence(1,  svalue.length()-1).toString();  //assume " on end, yet not checked.
    } else {
      value = svalue.toString();
    }
    if(attributes == null) {
      attributes = new IndexMultiTable<String, String>(IndexMultiTable.providerString);
    }
    attributes.put(name, value);
  }
  
  
  
  /**It's a debug helper. The method is empty, but it is a mark to set a breakpoint. */
  void stop()
  {
    
  }
  
  /** Inner class contains the complex syntax */
  //class Syntax
  //{



  //  Syntax()
  //  {
  //    alsoEmptyOption = false;

  //  }


    /** Converts the whole syntax definition from identifier to the end point.
     *
     * @param spInput The SBNF string with start position at syntax definition ident.
     * @throws ParseException
     */
    private void convertSyntaxDefinition(StringPartScan spInput)
    throws ParseException
    { spInput.setIgnoreWhitespaces(true);
      //spInput.setIgnoreComment("/*", "*/");
      spInput.setIgnoreEndlineComment("##");
      spInput.seekNoWhitespaceOrComments();
      if(spInput.getCurrentChar()=='?')
      { //a semantic explanation
        //skip until dot. It is uninteresting here.
        spInput.getCircumScriptionToAnyCharOutsideQuotion(".");
        spInput.seekPos(1);  //skip dot.
      }
      else
      { spInput.lentoIdentifier();
        if(spInput.length()>0)
        { sDefinitionIdent = spInput.getCurrentPart().toString();
          this.sSemantic = sDefinitionIdent;  //default if no <?Semantic> follows immediately
          eType = EType.kSyntaxDefinition;
          spInput.fromEnd();
        }
        else throwParseException(spInput, "ZbnfSyntaxPrescript - identifier for prescript expected;");
        if(spInput.seekNoWhitespace().scan().scan(":").scanIdentifier().scanOk()) {
          this.sSuperItemType = spInput.getLastScannedString();      // component:superSemantic::=
        }
        if(!spInput.seekNoWhitespace().scan().scan("::=").scanOk())
        { throwParseException(spInput, "::= expected");
        }
        convertTheStringGivenSyntax(spInput, ".",true, spInput.getCurrent(20));
      }  
    }


    /** Converts a associated semantic to the current syntax component.
     * A associated semantic is given by <code>&lt;?<i>semantic</i>&gt;</code> on start
     * of a syntax component. It may be also <code>[&lt;?&gt;</code>, in this case it is determined
     * that the component has no semantic, sSemantic is set to null.
     * <br>
     * semantic may be also "@identifier" or "text()", usefull for XML conversion.
     *
     * @param spInput
     * @return the semantic identifier.
     * @throws ParseException
     */
    boolean convertAssociatedSemantic(StringPartScan spInput) //aa
    throws ParseException
    {
      if(spInput.startsWith("<?"))  //##a
      { spInput.seek(2);
        if(false && spInput.startsWith("text()"))
        { spInput.seek(6);
          sSemantic = "text()";
          spInput.fromEnd();
        }
        else
        { //spInput.lentoIdentifier("@", ".");
          getSemantic(spInput);
          /*
          spInput.lentoAnyChar(">");
          if(spInput.length()>0)
          { sSemantic = spInput.getCurrentPart();
          }
          else sSemantic = null; //<?> without semantic
          spInput.fromEnd();
          */
        }
        if(spInput.getCurrentChar() == '>') { spInput.seek(1); }
        else throwParseException(spInput, "expected \">\" behind semantic" );

        return true;
      }
      else return false;
    }


    /** Converts a part of a ZBNF string to given and chars.
     * Called on top level and also nested.
     *
     * @param spInput  SBNF string at the start position
     * @param charsEnd one or some end chars.
     * @param bWhiteSpaces true: than whitespaces in syntax prescript causes whitespaces parsing in input text
     *                     false: whitespaces in syntax prescript have no effect.
     * @return The founded end char.
     * @throws ParseException if the content do not match.
     */
    char convertTheStringGivenSyntax(StringPartScan spInput, final String charsEnd, boolean bWhiteSpaces, CharSequence sSyntaxOnStartForErrorNothingFound)
    throws ParseException
    {
      
      CharSequence sCheck = spInput.getCurrent(30);
      if(StringFunctions.startsWith(sCheck, "\\#\\#"))
        stop();
     
      //first test wether <?semantic> is written:
      convertAssociatedSemantic(spInput);
      if(spInput.startsWith("<$NoWhiteSpaces>"))
      { bWhiteSpaces = false;
        spInput.seek(16);
      }
      char cEnd = 0;
      childSyntaxPrescripts = null;
      int catastrophicCt = 0;
      while(cEnd == 0)
      { if(++catastrophicCt > 1000000)
          stop();
        spInput.seekNoWhitespaceOrComments();
        if(spInput.found() && bWhiteSpaces)
        {
          childsAdd(new ZbnfSyntaxPrescript(this, EType.kSkipSpaces, spInput.getLineAndColumn(null)));
        }
        CharSequence sSyntaxOnStartForErrorNothingFoundChild = spInput.getCurrent(20);
        char cc;
        if(spInput.length() >0)
        { cc = spInput.getCurrentChar();
          if( charsEnd.indexOf(cc) >=0)
          { spInput.seek(1);
            cEnd = cc;
          }
          else if(cc == StringFunctions.cEndOfText)
          { cEnd = cc;
          }
        }
        else
        { cEnd = StringFunctions.cEndOfText;
          cc = 0;
        }
        if(cEnd == 0)
        { sSyntaxOnStartForErrorNothingFound = null;
          switch(cc)
          { //first test the special chars, it are the cases:
            case '|':
            { //alternative in syntax-expression maybe outside [...] or inside.
              spInput.seek(1);
              { CharSequence sInput = spInput.getCurrent(30);
                if(StringFunctions.startsWith(sInput, "|<?return"))
                  stop();
              }
              if(childSyntaxPrescripts == null)
              { //special case '|'without any other information before, it doesn't may be an alternative:
                alsoEmptyOption = true;
              }
              else
              { do
                { //convert the string given syntax of all alternatives and add it to this.
                  char cNext = spInput.getCurrentChar();
                  if(cNext==']')
                  { //it is nothing found between '|' and a terminate char, also maybe "|]"
                    alsoEmptyOption = true;
                    spInput.seek(1);
                    cEnd = ']';
                  }
                  else  
                  { ZbnfSyntaxPrescript alternative = new ZbnfSyntaxPrescript(this, report, true, spInput.getLineAndColumn(null));
                    cEnd = alternative.convertTheStringGivenSyntax(spInput, charsEnd + "|", bWhiteSpaces, sSyntaxOnStartForErrorNothingFoundChild);
                    //add the founded syntax as alternative.
                    if(!bChildSyntaxAreAlternatives)
                    { //it is the second alternative,the first one 
                      //founded syntax before, that is the first alternative.
                      //transfer it to a new ZbnfsyntaxPrescript:
                      ZbnfSyntaxPrescript firstAlternative = new ZbnfSyntaxPrescript(this, report, true, spInput.getLineAndColumn(null));
                      firstAlternative.childSyntaxPrescripts = childSyntaxPrescripts; 
                      childSyntaxPrescripts = new ArrayList<ZbnfSyntaxPrescript>();
                      bChildSyntaxAreAlternatives = true;
                      childSyntaxPrescripts.add(firstAlternative);
                    }  
                    childSyntaxPrescripts.add(alternative);
                  }
                } while(cEnd == '|');
                //cEnd is set with one of the charsEnd, it is the end.
              }
            } break;

            case '<':
            { childsAdd(convertSyntaxComponent(spInput));
            }break;
            case '[':
            { childsAdd(convertOptionSyntax(spInput, bWhiteSpaces, sSyntaxOnStartForErrorNothingFoundChild));
            }break;
            case '{':
            { childsAdd(convertRepetitionSyntax(spInput, bWhiteSpaces, sSyntaxOnStartForErrorNothingFoundChild));
            }break;
            case '.':
            case '?':
            case '}':
            case ']': case '>':
            { throwParseException(spInput, "unexpected \"" + cc + "\", expected endChars are " + charsEnd );
            }break;
            default: //childsAdd(convertConstantSyntax(spInput));
            { //no special char found, it is a terminal syntax!
              char cFirst = spInput.getCurrentChar();
              //String sTerminateChars;
              CharSequence[] sqTerminateChars = new CharSequence[1];
              char quotationChar = cFirst == '\"' ? cFirst: 0;  //scan after a quotation if a " is the current one.
              boolean bOk = spInput.scanStart().scanToAnyChar(sqTerminateChars
                  , "[|]{?}<>. \r\n\t\f"+StringFunctions.cEndOfText+StringFunctions.cNoCidentifier, '\\', quotationChar, quotationChar).scanOk();
              assert(bOk);  //because scan to end of text.
              /*
              if(cFirst == '\"') 
              { sTerminateChars = spInput.getCircumScriptionToAnyCharOutsideQuotion("[|]{?}<>. \r\n\t\f");
                int length = sTerminateChars.length();
                if(length >=2 && sTerminateChars.charAt(length-1) == '\"')
                { //The constant string is in "", it is valid without the "":
                  sTerminateChars = sTerminateChars.substring(1, length-1);
                }
              }
              else
              { sTerminateChars = spInput.getCircumScriptionToAnyChar("[|]{?}<>. \r\n\t\f"+StringPart.cEndOfText);
              }
              */
              if(sqTerminateChars[0].length() >0){
                String sTerminateString = sqTerminateChars[0].toString();
                ZbnfSyntaxPrescript terminateSyntax = new ZbnfSyntaxPrescript(this, report, false, spInput.getLineAndColumn(null));
                terminateSyntax.eType = sTerminateString.startsWith(sCommentStart1) || sTerminateString.startsWith(sCommentStart2) 
                    ? EType.kTerminalSymbolInComment : EType.kTerminalSymbol;
                terminateSyntax.sDefinitionIdent = "i-text";
                terminateSyntax.sConstantSyntax = sTerminateString; //sTerminateChars;
                childsAdd(terminateSyntax );
              } else {
                throwParseException(spInput, "internal error: any constant syntax expected." );
              }
            }
          }
        }
      }
      /*
      if(listAlternatives != null)
      { //It may be there are some alternatives,
        //than copy the last converted syntax in the list of alternatives.
        if(childs == null)
        { 
          alsoEmptyOption = true;  //set to false if further content is given.
        }
        else 
        { ZbnfSyntaxPrescript lastAlternative = new ZbnfSyntaxPrescript(report, true);
          lastAlternative.childs = childs; 
          listAlternatives.add(lastAlternative);
          childs = null;
        }
      }
      */
      if(sSyntaxOnStartForErrorNothingFound != null) 
        throw new ParseException("the syntax in this element is empty, it isn't correct:" + sSyntaxOnStartForErrorNothingFound
            + spInput.getCurrent(30), 0);
      return cEnd;
    }


    
    
    
    

    /**Reads the syntax of an &lt;action>.
     * Syntax of this element::=
     * <pre>\&lt;[&lt;#?numberOfChars>]&lt;syntax>[?&lt;$?semantic>]\></pre></br>
     * numberOfChars: max number of chars representing this element.<br/>
     * <pre>
     * syntax ::= [ #-&lt;?negativeNumber>
     *            | #X&lt;?hexNumber>
     *            | #&lt;?posNumber>
     *            | $&lt;?identifier>[&lt;\.?additionalChars>
     *            | \.&lt;?anyChars>[^\.&lt;exclusiveChars]>
     *            ].</pre>
     * It means (examlples)
     * <table><tr><th>sample</th><th>meaning</th></tr>
     * <tr><td><code>32$?nameOfSomething>     </code></td><td>Identifier with max 32 chars, it is the name of something</td></tr>
     * <tr><td><code>#?theNumberOfSomething>  </code></td><td>A positiv number, it is the number of something</td></tr>
     * <tr><td><code>#-?theNumberOfSomething> </code></td><td>A positiv or negative number, it is the number of something</td></tr>
     * <tr><td><code>5#-?theNumberOfSomething></code></td><td>A positiv or negative number with max 5 chars, it is the number of something</td></tr>
     * <tr><td><code>.^}?description>         </code></td><td>All chars exclusive }, a description</td></tr>
     * <tr><td><code>10$\$&%-?ident>          </code></td><td>An identifier with max 10 chars, also $&%- are identifier chars</td></tr>
     * <tr><td><code>""?description>          </code></td><td>A description written in quotions</td></tr>
     * </table>
     * @param spInput string to parse
     * @return The neu ComplexSyntax item
     * @throws ParseException
     */
    private ComplexSyntax convertSyntaxComponent(StringPartScan spInput)
    throws ParseException
    {
      ComplexSyntax actionItem = new ComplexSyntax(this, report, false, spInput.getLineAndColumn(null));
      spInput.seek(1);
      actionItem.convertSyntaxComponent(spInput);
      return actionItem;
    }


    private ZbnfSyntaxPrescript convertOptionSyntax(StringPartScan spInput, boolean bWhiteSpaces, CharSequence sSyntaxOnStartForErrorNothingFoundChild)
    throws ParseException
    {
      ZbnfSyntaxPrescript optionItem = new ZbnfSyntaxPrescript(this, report, true, spInput.getLineAndColumn(null));
      //optionItem = optionItem.new Syntax();
      spInput.seek(1);
      optionItem.convertAssociatedSemantic(spInput);
      if(spInput.startsWith("?"))
      { spInput.seek(1);
        optionItem.convertTheStringGivenSyntax(spInput, "]", bWhiteSpaces, sSyntaxOnStartForErrorNothingFoundChild);
        optionItem.eType = EType.kNegativVariant;
      }
      else if(spInput.startsWith(">"))
      { spInput.seek(1);
        optionItem.convertTheStringGivenSyntax(spInput, "]", bWhiteSpaces, sSyntaxOnStartForErrorNothingFoundChild);
        optionItem.eType = EType.kUnconditionalVariant;
      }
      else if(spInput.startsWith("!"))
      { spInput.seek(1);
        optionItem.convertTheStringGivenSyntax(spInput, "]", bWhiteSpaces, sSyntaxOnStartForErrorNothingFoundChild);
        optionItem.eType = EType.kExpectedVariant;
      }
      else if(spInput.startsWith("|"))
      { spInput.seek(1);
        optionItem.convertTheStringGivenSyntax(spInput, "]", bWhiteSpaces, sSyntaxOnStartForErrorNothingFoundChild);
        optionItem.eType = EType.kAlternativeOptionCheckEmptyFirst;
      }
      else
      {
        optionItem.convertTheStringGivenSyntax(spInput, "]", bWhiteSpaces, sSyntaxOnStartForErrorNothingFoundChild);
        //if(optionItem.listAlternatives == null)  //it means, only one alternative.
        //if(optionItem.childs.size() == 1)  //it means, only one alternative.
        if(!optionItem.isAlternative())  //it means, only one alternative.
        { optionItem.alsoEmptyOption = true;
          optionItem.sDefinitionIdent = "i-simpleOption";
          optionItem.eType = EType.kSimpleOption;
        }
        else
        { if(optionItem.alsoEmptyOption)
          { optionItem.sDefinitionIdent = "i-alternativeOption";
            optionItem.eType = EType.kAlternativeOption;
          }
          else
          { optionItem.sDefinitionIdent = "i-alternative";
            optionItem.eType = EType.kAlternative;
          }
        }
      }
      return optionItem;
    }


    private RepetitionSyntax convertRepetitionSyntax(StringPartScan spInput, boolean bWhiteSpaces, CharSequence sSyntaxOnStartForErrorNothingFoundChild)
    throws ParseException
    {
      RepetitionSyntax repetitionItem = new RepetitionSyntax(this, report, true, spInput.getLineAndColumn(null));
      spInput.seekPos(1);
      if(spInput.startsWith("|"))
      { spInput.seekPos(1);
        repetitionItem.bOnlyOneEachOption = true;
      }
      //repetitionItem = repetitionItem.new Syntax();
      repetitionItem.sDefinitionIdent = "i-Repetition";
      repetitionItem.eType = EType.kRepetition;
      char cEnd = repetitionItem.convertTheStringGivenSyntax(spInput, "?}", bWhiteSpaces, sSyntaxOnStartForErrorNothingFoundChild);
      if(cEnd == '?')
      { repetitionItem.backward = new ZbnfSyntaxPrescript(this, report, true, spInput.getLineAndColumn(null));
        repetitionItem.backward.eType = EType.kRepetitionRepeat;
        //repetitionItem.backward = repetitionItem.backward.new Syntax();
        repetitionItem.backward.sDefinitionIdent = "i-RepetitionRepeat";
        repetitionItem.backward.convertTheStringGivenSyntax(spInput, "}", bWhiteSpaces, sSyntaxOnStartForErrorNothingFoundChild);
      }
      return repetitionItem;
    }


    private void childsAdd(ZbnfSyntaxPrescript child)  //Object child)
    { if(childSyntaxPrescripts == null)
      { childSyntaxPrescripts = new ArrayList<ZbnfSyntaxPrescript>();//
      }
      childSyntaxPrescripts.add(child);
    }

    /** Gets an item from list.
     *
     * @param idx Index, starts with 0. T xxx
     * @return
     */
    Object xxxgetItem(int idx)
    { if(idx >= childSyntaxPrescripts.size()) return null; //end of list
      else return childSyntaxPrescripts.get(idx); //cause IndexOutOfBoundsException on negative index.
    }




  //}//class Syntax

  String getDefinitionIdent()
  { return sDefinitionIdent;
  }

  String getSubSyntax()
  { return sSubSyntax;
  }



  /** Gets the list of one alternative prescript inside this instance.
   * This method is used from the parser to test a syntax.
   * @param idx Nr of the alternative, 0 for first
   * @return null if no prescript exists for the alternative, otherwise
   *         the list of syntax elements in the prescript.
   */
  public List<ZbnfSyntaxPrescript> getListPrescripts()
  { return childSyntaxPrescripts;
  }

  
  
  /**Checks whether any child produces data.  
   * @return false if no child has a {@link #getSemantic()}
   */
  public boolean childsHasSemantic() {
    if(childSyntaxPrescripts == null) return false;
    else {
      for(ZbnfSyntaxPrescript child: childSyntaxPrescripts) {
        if(child.sSemantic !=null) return true;
        if(child.childsHasSemantic()) return true;
      }
    }
    return false; //all checked, nowhere semantic
  }
  
  
  /** Returns true if there are more as one alternative.*/
  boolean hasAlternatives()
  { return (eType == EType.kAlternative || eType == EType.kAlternativeOption || eType == EType.kAlternativeOptionCheckEmptyFirst);
  }



  void reportContent(MainCmdLogging_ifc report, int nLevel)
  {
    reportItem(nLevel, "+-", false, report);
  }


  private void reportItem(int nLevel, String sIndent, boolean bHasNext, MainCmdLogging_ifc report)
  {
    {
      String sReport;
      //String sSyntax = getConstantSyntax();
      sReport = toString();
      report.reportln(nLevel, 0, "SyntaxPrescript:" + sIndent  + sReport);
      if(childSyntaxPrescripts != null)
      { String sIndentNew = sIndent.substring(0, sIndent.length()-2)
                          + (bHasNext ? "| " : "  ");
        /*
        int idxPrescript = 0;
        Iterator iterPrescript;
        Iterator iterPrescriptNext = getPrescriptIterator(idxPrescript);
        boolean bAlternatives = true;
        while( (iterPrescript = iterPrescriptNext) != null)
        { iterPrescriptNext = getPrescriptIterator(++idxPrescript);
          String sIndentAdd = "*-";
          if(idxPrescript == 1 && iterPrescriptNext == null)
          { bAlternatives = false;
            sIndentAdd = "";
          }
          while(iterPrescript.hasNext())
          { ZbnfSyntaxPrescript itemChild = (ZbnfSyntaxPrescript)iterPrescript.next();
            itemChild.reportItem(nLevel, sIndentNew + sIndentAdd + "+-", iterPrescript.hasNext() , report);
            if(bAlternatives)
            { //from second line of item in alternative.
              sIndentAdd = (iterPrescriptNext == null) ? "  " : "! ";
            }
          }
          if(iterPrescriptNext != null)
          { //report.reportln(MainCmdLogging_ifc.info, 0, "SyntaxPrescript:" + sIndent + "................");
          }
        }
        */
        Iterator<ZbnfSyntaxPrescript> iter = childSyntaxPrescripts.iterator();
        while(iter.hasNext())
        { ZbnfSyntaxPrescript syntax = iter.next();
          syntax.reportItem(nLevel, sIndentNew + "+-", false , report);
          
        }
      }
    }
  }






  String getConstantSyntax()
  { return sConstantSyntax;
  }


  List<String> getListStrings()
  { return listStrings;
  }

  /** Returns the semantic of the item.
   *
   * @return String with semantic.
   */
  String getSemantic()
  { return sSemantic;
  }

  /** Returns the possible indent chars if the type is k..WithIndent
   * or null.
   *
   * @return String with the indent chars
   */
  String getIndentChars()
  { return sIndentChars;
  }

  /**Returns the factor to multiply for syntax <#f*factor?...> */
  double getFloatFactor()
  { return nFloatFactor;
  }
  
  
  /**Returns true, if the result of the parsing with this Syntaxprescript
   * is to assigned into the next component of the outer prescript
   */
  boolean isResultToAssignIntoNextComponent()
  { return bAssignIntoNextComponent;
  }

  /**Returns true, if outer result of parsing with the outer prescript
   * are to assigned into this component.
   */
  boolean isToAddOuterResults()
  { return bAddOuterResults;
  }



  /**Returns true if the Syntax item contains some alternatives getted by getListPrescripts. 
   * 
   * @return false if getListPrescripts supplies the one after another prescripts of the syntax term.
   */
  boolean isAlternative()
  { return bChildSyntaxAreAlternatives;
  }
  
  
  /** Gets the really max number of chars. If the max number is not defined
   * by textual syntax input, this method supplies the maximal integer value.
   * @param item The syntax item.
   * @return Max number, -1 if it is not a ComplexSyntax.
   */
  int getMaxNrofCharsFromComplexItem()
  { if(this instanceof ComplexSyntax)
    { int max = ((ComplexSyntax)this).nMaxChars;
      //unneccessary now: if(max <=0) max = Integer.MAX_VALUE;  //it means it.
      return max;
    }
    else return -1;  //NOTE: it isnot used anywhere. 
  }


  /** Gets the really max number of chars. If the max number is not defined
   * by textual syntax input, this method supplies the maximal integer value.
   * @param item The syntax item.
   * @return Max number, -1 if it is not a ComplexSyntax.
   */
  Pattern getRegexPatternFromComplexItem()
  { if(this instanceof ComplexSyntax)
    { return ((ComplexSyntax)this).regex;
    }
    else return null;   
  }


  EType getType()
  { return eType;
  }



  ZbnfSyntaxPrescript xxxgetSyntaxPrescript()
  { return null; //syntax;
  }

  /** Returns true if it is also valid if no input matches to this prescript,
   * defined by [option1|option2|] or [option]
   * */
  boolean isPossibleEmptyOption()
  { return alsoEmptyOption;
  }


/**
  static boolean isPossibleEmptyOption(Object item)
  { if(item instanceof SyntaxPrescript.OptionSyntax)
    { return ((SyntaxPrescript.OptionSyntax)item).alsoEmptyOption;
    }
    else return false;  //the user may be check if it is an OptionSyntax.
  }





  static SyntaxPrescript getOptionNextItem(Iterator iter)
  { if(iter.hasNext())
    { return (SyntaxPrescript)(iter.next());
    }
    else return null;  //the user may be check if it is an OptionSyntax.
  }
*/

  ZbnfSyntaxPrescript xxxgetRepetitionForwardPrescript()
  { return null; //syntax;
  }

  ZbnfSyntaxPrescript getRepetitionBackwardPrescript()
  { if(this instanceof RepetitionSyntax)
    { return ((ZbnfSyntaxPrescript.RepetitionSyntax)this).backward;
    }
    else return null;  //the user may be check if it is an OptionSyntax.
  }

  
  /**Reads an attribute of this syntax item. An attribute is given by syntax:
   * <code><...?.name=value?...></code>. Write a dot after the asterisk.
   * The attribute can be used in evaluating a parse result by the application.
   * @param name
   * @return
   */
  public String getAttribute(String name){ return attributes == null ? null: attributes.get(name); }

  /**Shows the content in a readable format for debugging. */
  @Override
  public String toString()
  { StringBuilder u = new StringBuilder(50);
    { String sWhat; // = "Syntax:" + getDefinitionIdent();
      if(eType == null) { 
        sWhat = "?-0-?"; 
      } else {
      
        switch(eType)
        { case kSyntaxDefinition:
          { sWhat = "!" + getDefinitionIdent() + "::=";
          } break;
          case kTerminalSymbol: sWhat = ":" + sConstantSyntax; break;
          case kSimpleOption:
          { sWhat = "[...]";
          } break;
          case kAlternativeOption:
          { sWhat = "[...|...|]";
          } break;
          case kAlternativeOptionCheckEmptyFirst:
          { sWhat = "[|...|...]";
          } break;
          case kNegativVariant:
          { sWhat = "[?...|...]";
          } break;
          case kUnconditionalVariant:
          { sWhat = "[>...|...]";
          } break;
          case kExpectedVariant:
          { sWhat = "[!...|...]";
          } break;
          case kAlternative:
          { sWhat = "...|...";
          } break;
          case kRepetition:
          { sWhat = ((RepetitionSyntax)this).bOnlyOneEachOption ? "{|...}" : "{...}";
          } break;
          case kOnlySemantic:
          { sWhat = "<?";
          } break;
          case kSyntaxComponent:
          { sWhat = "<" + getDefinitionIdent();
          } break;
          case kIdentifier  :                     sWhat="<$" + getConstantSyntax(); break;
          case kRegularExpression :               sWhat="<!" + getConstantSyntax();  break;
          case kStringUntilEndchar:               sWhat="<*C " + getConstantSyntax();  break;
          case kStringUntilEndcharOutsideQuotion: sWhat = "<*\"\" " + getConstantSyntax(); break;
          case kStringUntilEndStringInclusive:    sWhat = "<*+| " + getConstantSyntax(); break;
          case kStringUntilRightEndchar:          sWhat = "<stringtolastExclChar" + getConstantSyntax(); break;
          case kStringUntilRightEndcharInclusive: sWhat = "<stringtolastinclChar" + getConstantSyntax(); break;
          case kQuotedString  : sWhat = "<\"\" " + getConstantSyntax(); break;
          case kStringUntilEndString: sWhat = "<*| " + sConstantSyntax; break;
          case kStringUntilEndStringWithIndent: sWhat = "<+++* " + sConstantSyntax; break;
          case kPositivNumber : sWhat = "<#";  break;
          case kIntegerNumber : sWhat = "<#-"; break;
          case kHexNumber :     sWhat = "<#x"; break;
          case kFloatNumber :   sWhat = "<#f"; break;
          case kFloatWithFactor :   sWhat = "<#f*" +nFloatFactor; break;
          case kSkipSpaces :    sWhat = "\\n\\t"; break;
          case kNotDefined :    sWhat = "?-0-?"; break;
          default: sWhat = "?-?-?";
        }
      }
      u.append(sWhat);
    }
    String sSemantic = getSemantic();
    if(sSemantic != null)
    { if(u.charAt(0) != '<'){ u.append("<"); }
      u.append("?").append(sSemantic).append(">");
    }
    else
    { if(u.charAt(0) == '<'){ u.append( ">"); }
    }
    for(int i=0; i <u.length(); ++i){  //change \r etc in constant text to readable output
      char cc = u.charAt(i);
      if(cc == '\n'){ u.replace(i, i+1, "\\n");}
      if(cc == '\r'){ u.replace(i, i+1, "\\r");}
    }

    return u.toString();
  }




  /** Central method to invoke excpetion, usefull to set a breakpoint in debug
   * or to add some standard informations.
   * @param sMsg
   * @throws IndexOutOfBoundsException
   */
  protected void throwParseException(StringPartScan spInput, String sMsg)
  throws ParseException
  { //reportContent(report, 0);
    throw new ParseException(sMsg + ", found: " + spInput.getCurrent(60), spInput.getLineAndColumn(null));
  }




}
