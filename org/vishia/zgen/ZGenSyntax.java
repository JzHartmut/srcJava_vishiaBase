package org.vishia.zgen;

public final class ZGenSyntax {

  
  /**Version, history and license.
   * <ul>
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
      "$comment=(?...?).\n"
    + "$endlineComment=\\#\\#.  ##The ## is the start chars for an endline-comment or commented line in the generator script.\n"
    + "$keywords= new | java | cmd | start | debug | stdout | stdin | stderr \n" 
    + "  | subtext | sub | main | call | cd \n"
    + "  | Pipe | StringBuffer | Stringjar | String | List | Openfile | Obj | Set | set | include | zbatch \n"
    + "  | break | XXXreturn | exit | onerror | for | while | if | elsif | else . \n"
    + "\n"
    + "ZGen::= \n"
    + "[<*|==ZGen==?>==ZGen== ]\n"
    //+ "{ \\<:scriptclass : <$\\.?scriptclass> \\> \n"
    + "[{ include [<\"\"?include> | <*\\ ?include>] ; }] \n"
    + "{ <DefVariable?> ; \n"
    + "| subtext  <subtext?subroutine> \n"
    + "| sub <subroutine> \n"
    + "| class <subClass> \n"
    + "| main ( ) \\{ <statementBlock?mainScript> \\} \n"
    + "| //<*\\n?> ##line comment in C style\n"
    + "| /*<*|*/?>*/ ##block commment in C style\n"
    + "| ==endZGen==<*\\e?> \n"
    + "} \\e.\n"
    + "\n"
    + "\n"
    + "subClass::= <$?name> \\{ \n"
    //+ "{ <DefVariable?> ; \n"
    + "{ subtext  <subtext?subroutine> \n"
    + "| sub <subroutine> \n"
    + "| class <subClass> \n"
    + "} \\}. \n"
    + "\n"
    + "\n"
    + "subroutine::= [<?type> String | Append | Openfile | Map | List | Obj |] <$?name> ( [{ <DefVariable?formalArgument> ? , }] ) \\{ <statementBlock> \\}. \n"
    + "\n"
    + "subtext::= <$?name> ( [ { <DefVariable?formalArgument> ? , }] ) <textExpr>.\n"
    + "\n"
    + "\n"
    + "statementBlock::= [{ <statement?> }].\n"
    + "\n"
    + "statement::=\n"
    + "  \\{ <statementBlock> \\} \n"
    + "| REM <*\\n?> ##Remark like in batch files\n"
    + "| //<*\\n?> ##line commment in C style\n"
    + "| /*<*|*/?>*/ ##block commment in C style\n"
    + "| <DefVariable?> ; \n"
    + "| for <forScript?forContainer> \n"
    + "| <callSubroutine?call> \n"
    + "| cd [<textValue?cd> | <*\\ ;?cd> ; ]  ##change current directory \n"
    + "| if <ifScript?if> \n"
    + "| while <whileScript?> \n"
    + "| <threadBlock> \n"
    + "| <textOut> \n"
    + "| <cmdLineWait?cmdWait> \n"  ///
    + "| start <cmdLine?cmdStart> \n"
    + "| move <srcdst?move> ; \n"
    + "| copy <srcdst?copy> ; \n"
    //+ "| [{ <datapath?-assign> = }] java <staticJavaMethod?+?> \n"
    + "| <assignExpr> \n"
    + "| break <?breakBlock> ;\n"
//    + "| return <objExpr?return> ;\n"
    + "| exit <#?exitScript> ;\n"
    + "| onerror <onerror> \n"
    + "| debug <textValue?debug> ; \n"
    + "| ; \n"
    + ".\n"
    + "\n"
    + "\n"
    + "DefVariable::=String\\  <DefStringVar?textVariable> \n"  //note: without ; because used in sub argument list
    + "| StringBuffer\\  <DefObjVar?Stringjar> \n"
    + "| Stringjar\\  <DefObjVar?Stringjar> \n"
    + "| Pipe\\  <DefObjVar?Pipe> \n"
    + "| List\\  <DefObjVar?List> \n"
    + "| Map\\  <DefMapVar> \n"
    + "| Obj\\  <DefObjVar?objVariable> \n"
    + "| Openfile\\  <Openfile> \n"
    + "| Set\\  <DefStringVar?setEnvVar> \n"
    + "| set\\  <DefStringVar?setEnvVar> \n"
    + "| SET\\  <DefStringVar?setEnvVar> \n"
    + ".\n" ///
    + "\n"
    + "DefObjVar::= <variable?defVariable>  [ = <objExpr?>].\n"  //a text or object or expression
    + "\n"
    + "DefStringVar::= <variable?defVariable> [ = <textDatapath?>].\n"  //[{ <variable?assign> = }] <textDatapath?> .\n"
    + "\n"
    + "DefMapVar::= <variable?defVariable>.\n"  //[{ <variable?assign> = }] <textDatapath?> .\n"
    + "\n"
    + "Openfile::= <variable?defVariable> = <textDatapath?> .\n"
    + "\n"
    + "variable::= <$@-?startVariable>[ [?\\. \\>] \\.{ <datapathElement> ? [?\\. \\>] \\.}].\n"
    + "\n"
    + "\n"
    + "datapathElement::=[<$@-?ident>|<\"\"?ident>] [( [{ <objExpr?argument> ? ,}])<?whatisit=(>].\n"  
    + "\n"
    + "datapath::= \n"
    + "[ $$<$?envVariable> \n" 
    + "| [<?startVariable> $<![1-9]?>| $<$?>]    ## $1 .. $9 are the arguments of Jbatch, $name for environment \n"
    + "| <variable?> \n" 
    + "| new <newJavaClass> \n" 
    + "| java <staticJavaMethod> \n" 
    + "| %<staticJavaMethod> \n" 
    + "].\n"
    + "\n"
    + "\n"
    + "newJavaClass::= <$\\.?javapath> [ ( [{ <objExpr?argument> ? , }] )].\n" 
    + "staticJavaMethod::= <$\\.?javapath> [( [ { <objExpr?argument> ? , } ] )].\n"
    + "##a javapath is the full package path and class [.staticmetod] separated by dot. \n"
    + "\n"
    
    + "envVar::= <$?name> = <textDatapath?>.\n"
    + "\n"
    + "\n"
    + "\n"
    + "objExpr::= <textDatapath?> \n"         //either a text or a datapath, which may be any object, 
    + "  | <expression?>.\n"              //or an expression
    + "\n"
    + "\n"
    + "textDatapath::=  <\"\"?text> | \\<:\\><textExprNoWhiteSpaces?textExpr>\\<\\.\\> | <datapath> .\n"
    + "\n"
    + "textValue::=  <\"\"?text> | \\<:\\><textExprNoWhiteSpaces?textExpr>\\<\\.\\> | * <datapath> | <*;(\\ \\r\\n?text> .\n"
    + "\n"
    + "conditionInText::=<andExprInText?> [{\\|\\| <andExprInText?boolOrOperation>}].\n"  // || of <andExpr> 
    + "\n"
    + "andExprInText::= <boolExprInText?> [{ && <boolExprInText?boolAndOperation>}].\n"    // && of <boolExpr>
    + "\n"  
    + "boolExprInText::= [<?boolNot> ! | not|]\n"
    + "[ ( <conditionInText?parenthesisCondition> ) \n"                //boolean in paranthesis
    + "| <expression?> [<cmpOperationInText>]\n"  //simple boolean
    + "].\n"  
    + "\n"
    + "cmpOperationInText::=[ \\?[<?cmpOperator>gt|ge|lt|le|eq|ne] |  [<?cmpOperator> != | == ]] <expression?>.\n"
    + "\n"
    + "\n"
    + "\n"
    + "condition::=<andExpr?> [{\\|\\| <andExpr?boolOrOperation>}].\n"  // || of <andExpr> 
    + "\n"
    + "andExpr::= <boolExpr?> [{ && <boolExpr?boolAndOperation>}].\n"    // && of <boolExpr>
    + "\n"  
    + "boolExpr::= [<?boolNot> ! | not|]\n"
    + "[ ( <condition?parenthesisCondition> ) \n"                //boolean in paranthesis
    + "| <expression?> [<cmpOperation>]\n"  //simple boolean
    + "].\n"  
    + "\n"
    + "cmpOperation::=[ \\?[<?cmpOperator>gt|ge|lt|le|eq|ne] |  [<?cmpOperator> != | == | \\>= | \\> | \\<= | \\< ]] <expression?>.\n"
    + "\n"
    + "expression::= \\<:\\><textExprNoWhiteSpaces?textExpr>\\<\\.\\> \n"
    + "            | bool ( <boolExpr> ) \n"
    + "            | <multExpr?> [{ + <multExpr?addOperation> | - <multExpr?subOperation>}].\n"
    + "\n"
    + "\n"
    + "multExpr::= <value?> [{ * <value?multOperation> | / <value?divOperation> }].\n"
    + "\n"
    + "value::= <#?intValue> | <#f?floatValue> |   ##unary - associated to value.\n"
    + "[{[<?unaryOperator> ! | ~ | - | + ]}]        ##additional unary operators.\n"
    + "[<#?intValue> | 0x<#x?intValue> | <#f?floatValue> ##ones of kind of value:\n"
    + "| '<!.?charValue>' | <\"\"?textValue> \n"
    + "| ( <expression?parenthesisExpr> ) \n" 
    + "| <datapath> \n"
    + "].\n"
    + "\n"
    //+ "objvalue::=\n"
    + "\n"
    
    + "textExpr::=\n"
    + "{ [?\\<\\.\\>]              ##abort on <.> \n"
    + "[ \\<:for:<forContainer>\n"
    + "| \\<:if: <if>\n"
    + "| \\<:hasNext\\> <textExpr?hasNext> \\<\\.hasNext\\>\n"
    + "| \\<*subtext : <callSubtext?call>\n"
    + "| \\<*<dataText>\n"
    + "| \\<:n\\><?newline>\n"
    + "| \\<:\\><textExprNoWhiteSpaces?>\\<\\.\\>"               //flat nesting
    + "| <*|\\<:|\\<+|\\<=|\\<*|\\<\\.?nonEmptyText>    ##non-empty text inclusive leading and trailing whitespaces\n"
    + "]\n"
    + "}.\n"
    + "\n"
    
    + "textExprNoWhiteSpaces::=<$NoWhiteSpaces>\n"
    + "{ [?\\<\\.\\>]              ##abort on <.> \n"
    + "[ \\<:for:<forContainer>\n"
    + "| \\<:if: <if>\n"
    + "| \\<:hasNext\\> <textExpr?hasNext> \\<\\.hasNext\\>\n"
    + "| \\<*subtext : <callSubtext?call>\n"
    + "| \\<*<dataText>\n"
    + "| \\<:n\\><?newline>\n"
    + "| \\<:\\><textExpr?>\\<\\.\\>"               //flat nesting
    + "| <*|\\<:|\\<+|\\<=|\\<*|\\<\\.?textReplLf>    ##text inclusive leading and trailing whitespaces\n"
    + "]\n"
    + "}.\n"
    + "\n"
    + "dataText::=<datapath>[ : <\"\"?formatText>] \\>.     ##<*expr: format>\n"
    + "\n"
      //Note: the for-variable is adequate a DefVariable
    + "forScript::= ( <$?forVariable> : <datapath?forContainer> )  \\{ <statementBlock> \\} .\n"
    + "\n"
    + "forContainer::= <$?forVariable> : <datapath?forContainer> \\> <textExpr> \\<\\.for[ : <$?@name> ]\\>. ##name is the name of the container element data reference\n"
    + "\n"
    + "if::= <ifBlock> [{ \\<:elsif : <ifBlock>  }][ \\<:else\\> <textExpr?elseBlock> ] \\<\\.if\\>.\n"
    + "ifBlock::= <conditionInText> \\> <textExpr>.\n"
    + "\n"
    + "assignExpr::= [{ <variable?assign> [ = | += <?append>] }] <objExpr?> ;.\n"
    + "\n"
    + "threadBlock::= Thread <variable?defThreadVar> [;| = [thread] \\{ <statementBlock> \\}] | [<variable?assignThreadVar> =] thread \\{ <statementBlock> \\}.\n"
    + "\n"
    + "srcdst::= [src=] <textValue?actualArgument> [dst=] <textValue?actualArgument> .\n"
    + "\n"
    + "onerror::= [<#?errorLevel> | [<?errortype> notfound | file | internal | exit [<#?errorLevel>] ]|] \\{ <statementBlock> \\}.\n"
    + "\n"

    + "\n"
    + "ifScript::= <ifScriptBlock?ifBlock> [{ elsif <ifScriptBlock?ifBlock>  }][ else \\{ <statementBlock?elseBlock> \\} ].\n"
    + "\n"
    + "whileScript::= <ifScriptBlock?whileBlock> .\n"
    + "\n"
    + "ifScriptBlock::= [ ( <datapath> ) | ( <condition> ) ] \\{ <statementBlock> \\} .\n"
    + "\n"
    //Note: the for-variable is adequate a DefVariable
    + "callSubroutine::= [{ <variable?assign> [ = | += <?append>] }] call <textValue?callName> ( [{ <namedArgument?actualArgument> ? , }] ) ; .\n"
    + "callSubtext::=[<\"\"?callName>|<textValue?callNameExpr>] [ : { <namedArgument?actualArgument> ? , }] \\>.\n"
    + "\n"
    + "namedArgument::= [<?name>[$]<$?>|xxx][ = <objExpr?>].\n"
    + "\n"
    + "textOut::= \\<+ <variable?assign> \\> <textExpr>[ \\<\\.+\\> | \\<\\.n+\\><?newline>].\n"
    + "\n"
    + "cmdLineWait::=[{ <variable?assign> += }] cmd <cmdLine?>.\n"
    + "\n"
    + "cmdLine::= <textValue?> [{[?;[\\ |\\n|\\r]] [ \\<\\:arg\\><textExprNoWhiteSpaces?actualArgument>\\<\\.arg\\> |<textValue?actualArgument>] }] \n"
    + "  [ \\<:stdout:[ pipe<?pipe>| [$]<$?stdoutVariable>] \\>] ;.\n"
    + "\n"
       + "\n";
 
  


  
}
