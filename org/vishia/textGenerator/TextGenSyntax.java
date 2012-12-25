package org.vishia.textGenerator;

public final class TextGenSyntax {

  
  /**Version, history and license.
   * <ul>
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
  static final public int version = 20121226;

  
  public final static String syntax =
    "$comment=(?...?).\n"
    + "$endlineComment=\\#\\#.  ##The ## is the start chars for an endline-comment or commented line in the generator script.\n"
    + "\n"
    + "ZmakeGenctrl::= \n"
    + "{ <ZmakeTarget> \n"
    + "| <subtext> \n"
    + "| <genFile> \n"
    + "| \\<= <variableAssign>\n"
    + "} \\e.\n"
    + "\n"
    + "##A Zmake-Script contains of targets, which are to do.\n"
    + "##The genControl Script determines the textual representation of some targets with the several use-able names.\n"
    + "##All named targets in the control script can be used as Zmake-target-name (translator name) in the users Zmake script.\n"
    + "##This file describes how a genControl script should be build.\n"
    + "\n"
    + "\n"
    + "ZmakeTarget::= \\<:target = <$?@name> \\> \n"
    +   "<genContent?>\n"
    + "\\<\\.target\\>.\n"
    + "\n"
    + "\n"
    + "subtext::= \\<:subtext : <$?name> [ : { <namedArgument?formalArgument> ? , }] \\><genContent?> \\<\\.subtext\\>.\n"
    + "\n"
    + "\n"
    + "\n"
    + "##A genControl script should have a part <:file>....<.file> which describes how the whole file should build.\n"
    + "\n"
    + "genFile::= \\<:file\\>\n"
    +   "<genContent?>\n"
    + "\\<\\.file\\>.\n"
    +   "\n"
    + "  \n"
    + "\n"
    + "\n"
    + "##The textual content of any target, file, variable etc.\n"
    + "\n"
    + "genContent::=\n"
    + "{ \\<= <variableAssign>                       ##Possibility to have local variables. It is constant text.\n"
    //+ "| \\<= <namedArgument>                                           ##A reference to user data\n"
    + "| \\<:for:<forContainer>\n"
    + "| \\<:if: <if>\n"
    + "| \\<:hasNext\\> <genContent?hasNext> \\<\\.hasNext\\>\n"
    //+ "| \\<+<variableAssignment?addToList>\n"
    + "| \\<*subtext : <callSubtext>\n"
    + "| \\<*<dataText>\\>\n"
    + "| \\<:\\><genContentNoWhitespace?>\\<\\.\\>\n"
    + "| <*|\\<:|\\<*|\\<\\.?text>                        ##text after whitespace but inclusive trailing whitespaces till next control <: <* <.\n"
    + "}.\n"
    + "\n"
    + "\n"
    + "\n"
    + "callSubtext::=[<\"\"?name>|<datapath>] [ : { <namedArgument?actualArgument> ? , }] \\>.\n"
    + "\n"
    + "dataText::=<dataAccess?>[ : <\"\"?formatText>].\n"
    + "\n"
    + "dataAccess::= <#?intValue> | 0x<#x?intValue> | <#f?floatValue> | '<!.?charValue>' | <\"\"?textValue> \n"
    + "              | $new <newJavaClass> | $$<staticJavaMethod> |<datapath>.\n"
    + "\n"
    + "newJavaClass::= <$\\.?javapath> [ ({ <dataAccess?actualArgument> ? , } )].\n" ///
    + "staticJavaMethod::= <$\\.?javapath> ( [ { <dataAccess?actualArgument> ? , } ] ).\n"
    + "##a javapath is the full package path and class [.staticmetod] separated by dot. \n"
    + "\n"
    + "datapath::=<?>{ <datapathElement> ? \\.}.  ##path elements can start with $ or @ and can contain -\n"
    + "\n"
    + "datapathElement::=[<?ident>[$|@|][<$-?>]] [( [{ <dataAccess?actualArgument> ? ,}<?whatisit=r>])].\n"  
    + "\n"
    + "genContentNoWhitespace::=<$NoWhiteSpaces>\n"
    + "{ [?\\<\\.\\>]              ##abort on <.> \n"
    + "[ \\<*<dataText>\\>\n"
    + "| <*|\\<:|\\<*|\\<\\.?text>           ##text inclusive leading and trailing whitespaces\n"
    + "]\n"
    + "}.\n"
    + "\n"
    + "variableAssign::=<?> <textVariable> | <objVariable>.\n"
    + "textVariable::= <$?name> \\> <genContent?>  \\<\\.=\\>.\n"
    + "objVariable::= <$?name> : <dataAccess?> \\>.\n"
    + "\n"
    + "namedArgument::= <$?name>[ = <dataAccess?>].\n"
    + "\n"
    + "forContainer::= <$?@name> : <datapath> \\> <genContent?> \\<\\.for[ : <$?@name> ]\\>. ##name is the name of the container element data reference\n"
    + "\n"
    + "if::= <ifBlock> [{ \\<:elsif : <ifBlock>  }][ \\<:else\\> <genContent?elseBlock> ] \\<\\.if\\>.\n"
    + "ifBlock::= <condition> \\> <genContent?>.\n"
    + "\n"
    + "condition::=<?><dataAccess?> [<cmpOperation>].\n"  //NOTE: condition and dataAccess is stored in the ifBlock.
    + "\n"
    + "cmpOperation::=[ \\?[<?name>gt|ge|lt|le|eq|ne] |  [<?name> != | == ]] <dataAccess?>\n"
    + "\n"
    + "\n"
    + "\n"
    + "\n"
    + "\n";
 
  


  
}
