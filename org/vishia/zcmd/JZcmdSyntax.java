package org.vishia.zcmd;

import org.vishia.cmd.JZcmdExecuter;
import org.vishia.cmd.JZcmdScript;

/**This class contains the syntax as ZBNF string for a JZcmd script.
 * See {@link JZcmd}, {@link JZcmdScript}, {@link JZcmdExecuter}.
 * The syntax can be shown by command line
 * <pre>
 * java path/to/zbnf.jar org.vishia.zcmd.JZcmd --help
 * </pre>
 * @author Hartmut Schorrig
 *
 */
public final class JZcmdSyntax {

  
  /**Version, history and license.
   * <ul> 
   * <li>2015-08-30 Hartmut chg: textValue ends on a ':' too, important for <subtext:name:...
   * <li>2015-08-30 Hartmut new: The simple syntax <code>text = newFile;</code> to change the <code><+>output<.+></code>
   *   has a less semantic effect. Therefore it is replaced by <code><+:create>newFile<.+></code> or <code><+:append>...</code>
   *   with the possibility of append to an existing file.  
   * <li>2014-12-06 Hartmut new: //JZcmd will be ignored, possibility to write a JZcmd script in the comment of a Java source file. 
   * <li>2014-12-06 Hartmut chg: The definition of a return type of a subroutine was never used in the execution. Other concept. removed. 
   * <li>2014-08-10 Hartmut chg: endlineComment with // only till <+ to support start of text output in an endline comment line.
   * <li>2014-08-10 Hartmut chg: < :@ columnPos : minSpaces> uses : instead + as separator because + is a part of the columnpos expression.  
   * <li>2014-08-10 Hartmut new: !checkXmlFile = filename; 
   * <li>2014-07-27 Hartmut new: ## in text expression don't skip over newline. Write a <: > before to prevent newline.
   * <li>2014-06-15 Hartmut new: zmake &filePath := ... output as Filepath instance
   * <li>2014-06-15 Hartmut new: Obj name : type possible, not used yet, see {@link org.vishia.util.DataAccess.Variable#clazz}
   * <li>2014-06-07 Hartmut new: Class var = :loader: package.path.Class; with a loader. 
   * <li>2014-06-07 Hartmut new: new &datapath.Classvar:(args) and java &datapath.Classvar:method(args):
   *   Using a Class variable instead constant package.path.Class possible. 
   * <li>2014-05-18 Hartmut chg: DefFilepath now uses a textValue and divides the path to its components
   *   at runtime, doesn't use the prepFilePath in ZBNF. Reason: More flexibility. The path can be assmebled
   *   on runtime.  
   * <li>2014-04-24 Hartmut some changes, especially datapath uses a regular expression for second identifiers,
   *   not a ZBNF identifier (< $?>), therefore the JZcmd keywords are possible to use for internal data identifier.
   *   There should not be restrictions.
   * <li>2014-03-07 Hartmut new:  currdir without definition, Filepath as type of a named argument.
   * <li>2014-03-07 Hartmut new: All capabilities from Zmake are joined here. Only one concept!
   * <li>2014-02-22 Hartmut new: Bool and Num as variable types.
   * <li>2013-12-01 Hartmut new debug 
   * <li>2013-07-07 Hartmut improved: The older text generation view is now removed.
   *   <ul>
   *   <li>include "file" instead <:include:file>
   *   <li>subtext name(args) <:>...text...<.> instead <:subtext:name:args>...text...<.subtext>
   *   <li><:file>...<.file> removed, use main(){...}
   *   <li><=variabledef...> removed, use newer variabledef
   *   <li>genContent::= with whitespaces removed, instead <code>genContentNowWhiteSpaces::=</code> now exists
   *     as <code>textExpr::=</code> and contains <code><:for...> and <:if...></code>
   *   <li><code>textExpr::=</code> in older version now <code>textValue::=</code> with admissible 
   *     <code>"text", <:>...<.>, new ..., java ...</code>.
   *   <li><code>info</code> as syntactical unit removed, available as method <code>debug.info(...)</code>
   *   <li>  
   *   </ul>
   * <li>2013-07-07 Hartmut new: Now syntax as Java batch, invocation of command lines.
   * <li>2013-06-20 Hartmut new: Syntax with extArg for textual Arguments in extra block
   * <li>2013-06-29 Hartmut chg: Now <=var:expr.> should be terminated with .>
   * <li>2013-03-10 <code><:include:path> and <:scriptclass:JavaPath></code> is supported up to now.
   * <li>2013-01-05 Hartmut new: A expression is a concatenation of strings or + or - of numerics. It is used for all value expressions.
   *   In this kind an argument of <*path.method("text" + $$eNV_VAR + dataPath) is possible.
   *   Also <*path + path2> is possible whereby its the same like <*$path><*$path2> in that case.
   * <li>2012-12-26 Hartmut creation of this class: The syntax should be in a separate file, better for navigation.
   * <li>2012-12-10 Hartmut chg: The syntax is now stored in a static String variable. 
   * <li>2012-10-00 Hartmut new TextGenerator-syntax in a text file.
   * <li>2011-05-00 Hartmut creation of the syntax as Zmake syntax in a text file.
   * </ul>
   * 
   * <b>Copyright/Copyleft</b>:
   * For this source the LGPL Lesser General Public License, published by the Free Software Foundation is valid.
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
   * If you are intent to use this sources without publishing its usage, you can get
   * a second license subscribing a special contract with the author. 
   * 
   * @author Hartmut Schorrig = hartmut.schorrig@vishia.de
   * 
   * 
   */
  //@SuppressWarnings("hiding")
  static final public int version = 20131201;

  
  public final static String syntax =
      " $comment=(?...?).\n"
    + " $endlineComment=\\#\\#.  ##The ## is the start chars for an endline-comment or commented line in the generator script.\n"
    + " $keywords= new | cmd | cmd_check | start \n" 
    + "   | debug | java \n" 
    + "   | stdout | stdin | stderr \n" 
    + "   | subtext | sub | main | call | cd | CD | REM | Rem | rem \n"
    + "   | Pipe | StringBuffer | Stringjar | String | List | Openfile | Fileset | Obj | Set | set | include | zbatch \n"
    + "   | break | XXXreturn | exit | onerror | instanceof | for | while | do | if | elsif | else | throw . \n"
    + " \n"
    + " JZcmd::= \n"
    + " [<*|==ZGen==?>==ZGen== ]\n"
    + " [<*|==JZcmd==?>==JZcmd== ]\n"
    //+ " [<*|//JZcmd?>//JZcmd ]\n"
    //+ " { \\<:scriptclass : <$\\.?scriptclass> \\> \n"
    + " [{ ! checkJZcmd = <textValue?checkJZcmdFile> ; \n"
    + "  | ! checkXml = <textValue?checkXmlFile> ; \n"
    + " }]\n"
    + " [{ [REM|Rem|rem] <*\\n\\r?> ##Remark like in batch files\n"
    + "  | include <include> ; \n"
    + "  | currdir = <textDatapath?cd> ;\n"
    + " }] \n"
    + " { [//] ==endJZcmd==<*\\e?> \n"
    //+ " | //endJZcmd<*\\e?> \n"
    + " | [REM|Rem|rem] <*\\n\\r?> ##Remark like in batch files\n"
    + " | //JZcmd      ##ignore //JZcmd, it may be a comment for another language\n"
    + " | //<*\\n\\r?> ##line comment in C style\n"
    + " | /*<*|*/?>*/ ##block commment in C style\n"
    + " | <DefVariable?> ; \n"
    + " | <statement?>\n"
    + " | subtext  <subtext?subroutine> \n"
    + " | sub <subroutine> \n"
    + " | class <subClass> \n"
    + " | main ( ) \\{ <statementBlock?mainRoutine> \\} \n"
    + " } \\e.\n"
    + " \n"
    + " \n"
    + " include::= [$<$?envVar>[/|\\\\]][ <\"\"?path> | <*;\\ ?path>].\n"
    + " \n"
    + " \n"
    + " subClass::= <$?name> \\{ \n"
    //+ " { <DefVariable?> ; \n"
    + " { subtext  <subtext?subroutine> \n"
    + " | sub <subroutine> \n"
    + " | class <subClass> \n"
    + " } \\}. \n"
    + " \n"
    + " \n"
    + " subroutine::= <$?name> ( [ use-locals<?useLocals> | { add-locals<?addLocals> | <DefVariable?formalArgument> ? , }|] ) \\{ [<statementBlock>] \\}. \n"
    + " \n"
    + " subtext::= <$?name> ( [ use-locals<?useLocals> | { add-locals<?addLocals> | <DefVariable?formalArgument> ? , }|] ) \\<:\\><textExpr>\\<\\.\\>.\n"
    + " \n"
    + " \n"
    + " statementBlock::= { <statement?> }.\n"
    + " \n"
    + " statement::=\n"
    + "   \\{ [<statementBlock>] \\} \n"
    + " | REM <*\\n\\r?> ##Remark like in batch files\n"
    + " | ::{:}                ##Skip over :::\n"
    + " | //JZcmd      ##ignore //JZcmd, it may be a comment for another language\n"
    + " | //<*|\\n|\\r|\\<+?>     ##line commment in C style but only till <+\n"
    + " | /*<*|*/?>*/          ##block commment in C style\n"
    + " | text = <textValue?createTextOut> ;    ##set text output\n"
    + " | currdir = <textDatapath?cd> ;   ##set current directory\n"
    + " | [cd|CD] [<textValue?cd> | <*\\ ;?cd> ; ]  ##change current directory \n"
    + " | mkdir <textValue?mkdir> ;                 ##create any directory if not exists \n"
    + " | <DefVariable?> ; \n"
    + " | for <forCtrl> \n"
    + " | if <ifCtrl> \n"
    + " | while <whileCtrl> \n"
    + " | do <dowhileCtrl> \n"
    + " | start <cmdLine?cmdStart> \n"
    + " | zmake <zmake> \n"
    + " | move <srcdst?move> ; \n"
    + " | copy <srcdst?copy> ; \n"
    + " | [rm|del] <oneArg?del> ; \n"
    + " | break <?breakBlock> ; \n" 
    + " | return <?return> ; \n" 
    + " | exit <#?exitScript> ;\n"
    + " | throw on errorlevel <#?throwonerror> \n"
    + " | throw <textDatapath?throw> \n" 
    + " | onerror <onerror> \n"
    + " | errortoOutput off <?errorToOutput=0> \n"
    + " | errortoOutput <?errorToOutput=1> \n"
    + " | if errorlevel <iferrorlevel> \n"
    + " | debug [<textValue?debug>| <?debug>] ; \n"
    + " | <callSubroutine?call> \n"
    + " | <threadBlock> \n"
    + " | \\<+:create\\><textExpr?createTextOut>\\<\\.+\\> \n" ////
    + " | \\<+:append\\><textExpr?appendTextOut>\\<\\.+\\> \n" ////
    + " | <textOut> \n"  //Note: The srcLine should be set on start of <+ therefore it is checked in the syntax component. 
    + " | \\<:\\><textExpr>\\<\\.\\> [;] \n"
    + " | <cmdLineWait?cmdWait> \n"  
    + " | <assignExpr> \n"
    + " | ; \n"
    + " .\n"
    + " \n"
    + " \n"
    + " srcdst::= [{ -n<?newTimestamp>| -w<?overwr> | -r<overwro>}] [ src=] <textValue?src> [ dst=] <textValue?dst> .\n"
    + " oneArg::= <textValue?src> .\n"
    + " \n"
    + " \n"
    + " DefVariable::=\n"
    + "   String\\  <DefStringVar?textVariable> \n"  //note: without ; because used in sub argument list
    + " | Stringjar\\  <DefSpecVar?Stringjar> \n"
    + " | Num\\  <DefNumVar> \n"
    + " | Bool\\  <DefBoolVar> \n"
    + " | Pipe\\  <DefSpecVar?Pipe> \n"
    + " | List\\  <DefSpecVar?List> \n"
    + " | Map\\  <DefMapVar> \n"
    + " | Obj\\  <DefObjVar> \n"
    + " | Class\\  <DefClassVar> \n"
    + " | Classpath\\  <DefClasspath> \n"
    + " | Openfile\\  <Openfile> \n"
    + " | Fileset\\  <DefFileset> \n"
    + " | Filepath\\ <DefFilepath> \n"
    + " | Set\\  <DefStringVar?setEnvVar> \n"
    + " | set\\  <DefStringVar?setEnvVar> \n"
    + " | SET\\  <DefStringVar?setEnvVar> \n"
    + " .\n" 
    + " \n"
    + " DefNumVar::= [const <?const>] <definePath?defVariable>  [ = <numExpr>].\n"  //a text or object or expression
    + " \n"
    + " DefBoolVar::= [const <?const>] <definePath?defVariable>  [ = <boolExpr>].\n"  //a text or object or expression
    + " \n"
    + " DefSpecVar::= [const <?const>] <definePath?defVariable>  [ = <objExpr?>].\n"  //a text or object or expression
    + " \n"
    + " DefObjVar::= [const <?const>] <definePath?defVariable> [ : <$\\.?type>]  [ = <objExpr?>].\n"  //a text or object or expression
    + " \n"
    + " DefClassVar::= [const] <definePath?defVariable>  = \n"  //a text or object or expression
    + "   [: <dataAccess?loader> : ]  ## a datapath to a ClassLoader instance, a Classpath variable. \n"  
    + "   <textValue?>.               ## The package path maybe contained in any expression\n"  
    + " \n"
    + " DefClasspath::= [const] <definePath?defVariable>  = [ : <$?parentClasspath> : ] { <filesetAccess> ? , }.\n"  //a text or object or expression
    + " \n"
    + " DefStringVar::= [const <?const>] <definePath?defVariable> [ = <textDatapath?>].\n"  //[{ <definePath?assign> = }] <textDatapath?> .\n"
    + " \n"
    + " DefMapVar::= [const <?const>] <definePath?defVariable> [ = \\{ <dataStruct> \\}  ].\n" 
    + " \n"
    + " Openfile::= [const <?const>] <definePath?defVariable> = <textDatapath?> .\n"
    + " \n"
    + " \n"
    + " definePath::= <$-?startVariable>[ [?\\. \\>] \\.{ <defineSubelement?datapathElement> ? [?\\. \\>] \\.}].\n"
    + " \n"
    + " defineSubelement::= <$-?ident> [( [{ <objExpr?argument> ? ,}])<?whatisit=(>].\n"  
    + " \n"
    + " \n"
    + " DefFileset::= <definePath?defVariable> [ =  ( \n"
    + " [ commonpath = [<\"\"?commonPath>|<*;,)(\\ \\r\\n?commonPath>] , ] \n"
    + " { [{ //JZcmd | //<*\\n\\r?>}] [<\"\"?filePath>|<*;,)(\\ \\r\\n?filePath>] [{ //JZcmd | //<*\\n\\r?>}] ? , } \n"
    + " ) ] .\n"
    + " \n"
    + " DefFilepath::= <definePath?defVariable> [ = <textValue?> ]. \n"
    + " \n"
    + " \n"
    + " XXXFilepath::=<\"\"?!prepFilePath>|<*;\\ \\r\\n,)?!prepFilePath>. \n"
    + " \n"
    + " prepFilePath::=<$NoWhiteSpaces><! *?>\n"
    + " [ &$<$?@envVariable> [\\\\|/|]      ##path can start with a environment variable's content\n"
    + " | &<$?@scriptVariable> [\\\\|/|]    ##path can start with a scriptvariable's content\n"
    + " | [<!.?@drive>:]                  ## only 1 char with followed : is the drive letter\n"
    + "   [ [/|\\\\]<?@absPath>]            ## starting with / maybe after d: is absolute path\n"
    + " |]\n"
    + " [ <*:?@pathbase>[?:=]:]           ## all until : is pathbase, but not till a :=\n"
    + " [ <toLastChar:/\\\\?@path>[\\\\|/|]] ## all until last \\ or / is path\n"
    + " [ <toLastChar:.?@name>              ## all until exclusive dot is the name\n"
    + "   <*\\e?@ext>                       ## from dot to end is the extension\n"
    + " | <*\\e?@name>                      ## No dot is found, all is the name.\n"
    + " ] ."
    + " \n"
    + " \n"
    + " \n"
    + " \n"
    + " \n"
    + " \n"
    + " textDatapath::=  <\"\"?text> | \\<:\\><textExpr>\\<\\.\\> | [& [?(] ] <dataAccess> .\n"
    + " \n"
    //NOTE: a textvalue cannot end on a ':' because command line arguments cannont be parsed with then.
    + " textValue::=  <\"\"?text> | \\<:\\><textExpr>\\<\\.\\> | & <dataAccess> | <*;,)(\\ \\r\\n\\>?text> .\n"
    + " \n"
    //NOTEtextvalue for a text expression in <subtext:name:...> can end on a ':' .
    + " textValueTextExpr::=  <\"\"?text> | \\<:\\><textExpr>\\<\\.\\> | & <dataAccess> | <*:;,)(\\ \\r\\n\\>?text> .\n"
    + " \n"
    + " \n"
    + " objExpr::= \n"
    //+ "   File : <textValue?File>         ## A textValue which builds a java.lang.File in the currdir \n"
    + "   Filepath : <textValue?Filepath> ## A textValue which builds a Filepath in the currdir \n"
    + " | Fileset : <filesetAccess>  \n"
    + " | \\{ <dataStruct> \\}              ## It is a Map of Variables. \n"
    + " | <\"\"?text>                       ## It is a constant text. \n"
    + " | \\<:\\><textExpr>\\<\\.\\>           ## It is a text assembled in runtime. \n"
    + " | <numExpr>.                      ## special detection of a simple dataAccess.\n"
    + " \n"
    + " dataStruct::= { <DefVariable?> ; }.\n"
    + " \n"
    + " \n"
    + " \n"
    + " \n"
    + " dataAccess::= \n"
    + " [ $<$?envVariable> \n" 
    //+ " | [<?startVariable> $<![1-9]?>| $<$?>]    ## $1 .. $9 are the arguments of Jbatch, $name for environment \n"
    + " | [<?startVariable> $<#?>| $<$?>]    ## $1 .. $999 are the arguments of JZcmd, $name for environment \n"
    + " | [|java\\ ] new\\  <staticJavaAccess?newJavaClass> \n" 
    + " | [%|java\\ ] <staticJavaAccess?staticJavaMethod> \n" 
    + " | <dataPath?> \n" 
    + " ].\n"
    + " \n"
    + " \n"
    + " ## Access to a Java class constructor or static method or field\n"
    + " staticJavaAccess::=\n"
    + "   [ & <dataAccess?Class_Var> : [<$\\.$?javapath>]       ## access via Class variable .element\n"
    + "   | [: <dataAccess?Classpath_Var> : ] <$\\.$?javapath>  ## [Classpath] package.path.Class.element\n"
    + "   ] [( [ { <objExpr?argument> ? , } ])].               ## arguments\n"
    + " \n"
    //+ " dataPath::= <$-?startVariable>[ [?\\. \\>] \\.{ <datapathElement> ? [?\\. \\>] \\.}].\n"
    + " dataPath::= \n"
    + " [ File : <textValue?File>     ##creates a file object with given path\n"
    + " | <startDatapath> \n" 
    + " ] [ [?\\. \\>] \\.{ <datapathElement> ? [?\\. \\>] \\.}].\n"
    + " \n"
    + " ## A datapath cannot start with an JZcmd keyword! \n"
    + " startDatapath::= \n"
    + " [ & ( <dataPath> ) \n"
    + " | <$-?ident> ] <?whatisit=@> [( [{ <objExpr?argument> ? ,}])<?whatisit=+>]\n"
    + " .\n"  
    + " datapathElement::= \n"
    + " [ & ( <dataPath> ) \n"
    + "   ##Field or method identifier, use regex for the second datapath element, it can be a JZcmd keyword too! \n"
    + " | [<?ident>[@]<![\\\\w-]+?>]] \n"
    + "     [( [{ <objExpr?argument> ? ,}]) <?whatisit=(> ]  ##a method\n"
    + " .\n"  
    + " \n"
    + " \n"
    + " \n"
    + " \n"
    + " condition::=<andExpr?> [{\\|\\| <?boolCheckOrOperation> <andExpr?boolOrOperation>}].\n"  // || of <andExpr> 
    + " \n"
    + " andExpr::= <boolExpr?> [{ && <?boolCheckAndOperation> <boolExpr?boolAndOperation>}].\n"    // && of <boolExpr>
    + " \n"  
    + " boolExpr::= [<?boolNot> ! | not| NOT|]\n"
    + " [ ( <condition?parenthesisCondition> ) \n"                //boolean in paranthesis
    //+ " | <instanceof> \n"                //boolean in paranthesis
    + " | <numExpr?> [<cmpOperation>]\n"  //simple boolean
    + " ].\n"  
    + " \n"
    + " cmpOperation::=[ \\?[<?cmpOperator>gt|ge|lt|le|eq|ne|instanceof] |  [<?cmpOperator> != | == | \\>= | \\> | \\<= | \\< ]] <numExpr?>.\n"
    + " \n"
    + " instanceof::=<objExpr> instanceof <staticJavaAccess>.\n"
    + " \n"
    + " conditionInText::=<andExprInText?> [{\\|\\| <?boolCheckOrOperation> <andExprInText?boolOrOperation>}].\n"  // || of <andExpr> 
    + " \n"
    + " andExprInText::= <boolExprInText?> [{ && <?boolCheckAndOperation> <boolExprInText?boolAndOperation>}].\n"    // && of <boolExpr>
    + " \n"  
    + " boolExprInText::= [<?boolNot> ! | not|]\n"
    + " [ ( <conditionInText?parenthesisCondition> ) \n"                //boolean in paranthesis
    + " | <numExpr?> [<cmpOperationInText?cmpOperation>]\n"  //simple boolean
    + " ].\n"  
    + " \n"
    + " cmpOperationInText::=[ \\?[<?cmpOperator>gt|ge|lt|le|eq|ne] |  [<?cmpOperator> != | == ]] <numExpr?>.\n"
    + " \n"
    + " \n"
    + " numExpr::=  bool ( <boolExpr?> ) \n"
    //+ " | File : <textValue?File>  ##creates a file object with given path \n"    
    + " | <multExpr?> [{ + <multExpr?addOperation> | - <multExpr?subOperation>}]\n"
    + " .\n"
    + " \n"
    + " \n"
    + " multExpr::= <value?> [{ * <value?multOperation> | / <value?divOperation> }].\n"
    + " \n"
    + " value::= 0x<#x?intValue> | <#?intValue>[?\\.] | <#f?doubleValue> |    ##unary - associated to value.\n"
    + " [{[<?unaryOperator> ! | ~ | - | + ]}]        ##additional unary operators.\n"
    + " [ 0x<#x?intValue> | <#?intValue>  ##ones of kind of value:\n"
    + " | '<!.?charValue>' | <\"\"?textValue> \n"
    + " | ( <numExpr?parenthesisExpr> ) \n" 
    + " | [& [?(] ] <dataAccess>   ## & is optional, don't confuse with &(variable) \n"
    + " ].\n"
    + " \n"
    + " \n"
    + " textExpr::=<$NoWhiteSpaces>\n"
    + " { [?\\<\\.\\>]                             ##abort on <.> \n"
    + " [ \\<&-<*\\>?>\\>                          ##<&- it is comment> \n"
    + " | \\<:---<*|---\\>?>---\\> ##<:--- comment ---> \n"
    + " | \\<:-<*\\>?>\\><textExpr?>\\<\\.-<*\\>?>\\> ##<:-comment> comment <.- > \n"
    + " | \\#\\#<*\\r\\n?>   ##comment to eol in a text Expression\n"
    + " | \\<:for:<forInText?forCtrl>\n"
    + " | \\<:if: <ifInText?ifCtrl>\n"
    + " | \\<:hasNext\\> <textExpr?hasNext> \\<\\.hasNext\\>\n"
    + " | \\<:subtext : <callSubtext?call>\n"
    + " | \\<:scriptdir<?scriptdir>\\>\n"
    + " | \\<:debug[:<textDatapath?debug>| <?debug>]\\>\n"
    + " | \\<&<dataText>\n"
    //+ " | \\<: [<?transliteration> n | r | t | \\< | # | \\\" ] \\>\n"
    + " | \\<: [<?transliteration>n|r|t|b|[\\<|#|\\\"]<*\\>?>] \\>\n"
    + " | \\<:[<#?utf16code>|x<#x?utf16code>]\\>\n"
    + " | \\<:lf\\><?newline>\n"
    + " | \\<:\\ \\><!\\\\s*?> [ \\#\\#<*\\r\\n?> <!\\\\s*?> ]\n"      //skip all whitespaces and endlinecomment
    + " | \\<:s\\><?skipWhiteSpaces>\n"      //skip all whitespaces and endlinecomment
    + " | \\<:@<setColumn>\\>  \n"               //set column 
    + " | \\<:\\><textExpr?>\\<\\.\\>\n"               //flat nesting
    + " | <*|\\<:|\\<=|\\<&|\\#\\#|\\<\\.?plainText>\n"  //Note: beginning "<" of "?plainText>" is left!
    + " ]\n"
    + " }.\n"
    + " \n"
    + " \n"
    + " dataText::=<dataAccess>[ \\: [<\"\"?formatText>|<*\\>?formatText>]] \\>.     ##<*expr: format>\n" 
    + " \n"  ////
    + " textOut::=\\<+ [<dataPath?assign>] [:n<?newline>] \\> \n"
    + "   <textExpr>\n"
    + "   [ \\<\\.+\\>                     ## end text variants: \n"
    + "   | \\<\\.n+\\><?newline>  \n"  
    + "   | \\<\\.+n\\><?newline> \n"
    + "   | \\<\\.+n+flush\\><?newline><?flush>  \n"
    + "   | \\<\\.+flush\\><?flush>\n"
    + "   | \\<\\.+n+close\\><?close>  \n"
    + "   | \\<\\.+close\\><?close> \n"
    + "   ].\n"
    + " \n"      //<?posIndent>
    + " setColumn::=<numExpr> [ : <numExpr?minSpaces>] | : <numExpr?minSpaces>.\n"  
    + " \n"
    + " \n"
    + " \n"
      //Note: the for-variable is adequate a DefVariable
    + " forCtrl::= ( <$?forVariable> : <dataAccess?forContainer> [ && <condition> ] )  \\{ <statementBlock> \\} .\n"
    + " \n"
    + " forInText::= <$?forVariable> : <dataAccess?forContainer> [ && <condition> ] \\> <textExpr> \\<\\.for[ : <$?@checkForVariable> ]\\>. \n"
    + " ##name is the name of the container element data reference\n"
    + " \n"
    + " ifCtrl::= <ifBlock> [{ elsif <ifBlock>  }][ else \\{ [<statementBlock?elseBlock>] \\} ].\n"
    + " \n"
    + " ifBlock::= ( <condition> ) \\{ <statementBlock> \\} .\n"
    + " \n"
    + " ifInText::= <ifBlockInText?ifBlock> [{ \\<:elsif : <ifBlockInText?ifBlock>  }][ \\<:else\\> <textExpr?elseBlock> ] \\<\\.if\\>.\n"
    + " \n"
    + " ifBlockInText::= <conditionInText?condition> \\> <textExpr>.\n"
    + " \n"
    + " whileCtrl::= ( <condition> ) \\{ [<statementBlock>] \\} .\n"
    + " \n"
    + " dowhileCtrl::=  \\{ [<statementBlock>] \\} while ( <condition> ) ; .\n"
    + " \n"
    + " \n"
    + " onerror::= [ <#?errorLevel> \n" 
    + "            | [<?errortype> notfound | cmd | file | internal | exit ]\n" 
    + "            |]\n" 
    + "            \\{ [<statementBlock>] \\}.\n"
    + " \n"
    + " \n"
    + " callSubroutine::= [{ <dataPath?assign> [ = | += <?append>] }] call <textValue?callName> ( [{ <namedArgument?actualArgument> ? , }] ) ; .\n"
    + " \n"
    //+ " callSubtext::=[<\"\"?callName>|<textValue?callNameExpr>] [ : { <namedArgument?actualArgument> ? , }] \\>.\n"
    + " callSubtext::=<textValueTextExpr?callName> [ : { <namedArgument?actualArgument> ? , }] \\>.\n"
    + " \n"
    + " namedArgument::= <$?name> = <objExpr?>.\n"
    + " \n"
    + " \n"
    + " zmake::= [ : <$-?name> :[?=] ] <textValue?zmakeOutput> := <textValue?callName> ( { <namedArgument?actualArgument> | <filesetAccess> ? ,} ).\n"
    + " \n"
    //+ " output::=<*|\\ |\\r|\\n|:=?!prepFilePath>.\n"
    + " \n"
    + " \n"
    + " ## An accessPath is a Filepath, see prepFilepath::=, but analyzed on Java level. \n"
    + " filesetAccess::= [ \n"
    + "                    <\"\"?accessPath> | \\<:\\><textExpr>\\<\\.\\> \n"
    + "                  | [<?accessPathOrFilesetVariable> [&]<*\\ \\r\\n,)&;?>] \n"
    + "                  ] [ & <$?zmakeFilesetVariable>] . \n"
    + " \n"
    + " \n"
    + " cmdLineWait::=[{ <dataPath?assign> += }] cmd\\  <cmdLine?>.\n"
    + " \n"
    + " cmdLine::= [\\!argsCheck!<?argsCheck>] <textValue?> [{[?;[\\ |\\n|\\r]] [ \\<\\:arg\\><textExpr?actualArgument>\\<\\.arg\\> | \\<\\:args:<dataAccess?argList>\\> |<textValue?actualArgument>] }] \n"
    + "   [ \\<:stdout:[ pipe<?pipe>| [$]<$?stdoutVariable>] \\>] ;.\n"
    + " \n"
    + " iferrorlevel::= <#?errorLevel> \\{ [<statementBlock>] \\}.\n"
    + " \n"
    + " \n"
    + " \n"
    + " assignExpr::= [{ <dataPath?assign> [ = | += <?append>] }] <objExpr?> ;.\n"
    + " \n"
    + " \n"
    + " threadBlock::= Thread <dataPath?defThreadVar> = [thread] \\{ <statementBlock> \\} \n"
    + "              | thread \\{ <statementBlock> \\}.\n"
    + " \n"
    ;  
 
  


  
}
