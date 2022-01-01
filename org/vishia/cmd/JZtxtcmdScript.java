package org.vishia.cmd;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptException;

import org.vishia.jztxtcmd.JZtxtcmdSyntax;
import org.vishia.mainCmd.MainCmdLoggingStream;
import org.vishia.mainCmd.MainCmdLogging_ifc;
import org.vishia.util.CalculatorExpr;
import org.vishia.util.DataAccess;
import org.vishia.util.Debugutil;
import org.vishia.util.FileFunctions;
import org.vishia.util.FilePath;
import org.vishia.util.FileSet;
import org.vishia.util.FileSystem;
import org.vishia.util.GetTypeToUse;
import org.vishia.util.IndexMultiTable;
import org.vishia.util.SetLineColumn_ifc;
import org.vishia.util.StringFunctions_B;
import org.vishia.util.StringPartFromFileLines;
import org.vishia.util.StringPartScan;
import org.vishia.xmlSimple.SimpleXmlOutputter;
import org.vishia.xmlSimple.XmlNode;
import org.vishia.zbnf.ZbnfJavaOutput;
import org.vishia.zbnf.ZbnfParseResultItem;
import org.vishia.zbnf.ZbnfParser;



/**This class contains the internal representation of a JZcmd script. 
 * The translator is contained in {@link org.vishia.jztxtcmd.JZtxtcmd} in the srcJava_Zbnf source package. 
 * This class is independent of ZBNF. It is used for working in srcJava_vishiaBase environment.
 * It means, without the srcJava_Zbnf source package all sources of that are able to compile, but
 * this class have not data, respectively it is not instantiated. It may be possible to instantiate
 * and fill by direct invocation of the new_semantic(...) and add_semantic(...) operations, for example
 * for simple scripts.
 * 
 * @author Hartmut Schorrig
 *
 */
public class JZtxtcmdScript extends CompiledScript 
{
  /**Version, history and license.
   * 
   * <ul>
   * <li>2021-12-30 Hartmut Enhancement: {@link #setScriptFromString(StringPartScan, ZbnfJZcmdScript, File, File)}: 
   *   Now it is possible to read an include script from an operation,
   *   which is hence stored in the jar file. Syntay is: include <code>%<#?backlevel>:pkg.path.Class.operation()</code>.
   *   The <code>backlevel</code> is to built a reference current directory for further includes. 
   *   It should be relative in the same working tree, usual on the root of a working tree.  
   * <ul>2021-09-15 Hartmut chg: {@link JZcmditem#new_textExpr(ZbnfParseResultItem)}:
   *   If this new text expression has no semantic specification for attribute "indent" written as (exmpl) <code>?.indent=-3</code>
   *   then the indent of the parent is valid. It is better for generated text.  
   * <li>2021-06-08 Hartmut: Now the <::> can be used for a text start without newline written with ident designation
   * * usage: subtext(args) <::>textexpr<.> possible and proposed
   * * intern: ZbnfItemAttribute ?.skipFirstnl= and ?.ident= used.
   * <li>2021-06-08 Hartmut: Some simplifications for inhibit repeat same programming:
   *   common use of {@link JZcmditem#new_textExpr(ZbnfParseResultItem)} 
   *   also in {@link StatementList#new_textExpr(ZbnfParseResultItem)}
   * <li>2020-01-28 Hartmut bugfix {@link #createScriptFromString(StringPartScan, MainCmdLogging_ifc, File, File)}
   *   with null as fileScript arg now runs as described.
   * <li>2018-09-22 Hartmut new: important change: Now the compilation of the script is part of this class.
   *   {@link #createScriptFromFile(File, MainCmdLogging_ifc, File)} and {@link #createScriptFromString(StringPartScan, MainCmdLogging_ifc, File, File)}
   *   added. The adequate routines in {@link org.vishia.jztxtcmd.JZtxtcmd#translateAndSetGenCtrl(StringPartScan, MainCmdLogging_ifc, File, File)}
   *   is obsolete yet. This change was possible with joining srcJava_Zbnf with srcJava_vishiaBase because the {@link ZbnfParser} is necessary for that.
   *   The motivation was: Re-translated a script in a graphical environment (build with JZtxtcmd). It should be simple. It is a refactoring.
   * <li>2017-07-12 Hartmut new: debugOp
   * <li>2017-03-25 Hartmut chg: Handling of indentation improved with {@link org.vishia.zbnf.ZbnfParser} capability for define attributes on a syntax item
   *   and the capability of {@link StatementList#new_textExpr(ZbnfParseResultItem)} with the result item. 
   * <li>2017-01-13 Hartmut chg: A {@link Subroutine} now has the reference {@link Subroutine#theScript} to get the script 
   *   for a {@link JZtxtcmdExecuter#execSub(Subroutine, List, boolean, Appendable, File, CmdExecuter)} invocation.  
   * <li>2016-12-27 Hartmut adapt: {@link JZtxtcmdExecuter.ExecuteLevel#jzcmdMain}
   * <li>2016-12-26 Hartmut adapt: {@link JZcmdClass#listClassesAndSubroutines}  
   * <li>2016-12-18 Hartmut new: {@link Subroutine#new_List()} necessary in any zbnf syntax (where...). 
   * <li>2016-01-09 Hartmut new: {@link DefContainerVariable}: A List is accepted as container for constant elements now, changed in {@link org.vishia.jztxtcmd.JZtxtcmdSyntax}.
   *   DefSubtext now supported.
   * <li>2015-12-12 Hartmut chg: {@link StatementList#new_hasNext()} returns a {@link StatementList} instead {@link JZcmditem}
   *   because there may be not only one const text element between <:hasNext>...<.hasNext>. No change in the {@link JZtxtcmdExecuter} necessary. 
   * <li>2015-08-30 Hartmut chg: The functionality to remove indentation is moved from JZcmdExecuter 
   *   to {@link StatementList#set_plainText(String)} because it is done only one time on preparation of the script, not more as one in any loop of execution.
   *   It is changed in functionality: <code><:s></code> to skip over white spaces and the next line indentation.
   *   The {@link JZscriptSettings} is created as composite one time in this class and referenced in 
   *   {@link StatementList#jzSettings} for usage in that functionality.
   * <li>2015-05-24 Hartmut chg: copy, move, del with options, uses {@link FileOpArg} 
   * <li>2015-05-17 Hartmut new: syntax "File : <textValue>" is now able as start path of a DataPath.
   *   Therefore it's possible to write <code>File: "myFile".exists()</code> or adequate. A relative filename
   *   is related to the {@link JZtxtcmdExecuter.ExecuteLevel#currdir()}. 
   * <li>2015-05-17 Hartmut new: mkdir
   * <li>2015-05-17 Hartmut new: <code>text = path/to/output</code> is able to set in the script yet too.
   * <li>2014-12-14 Hartmut adapt: uses GetTypeToUse, adapted to changed ZbnfJavaOutput.
   * <li>2014-12-14 Hartmut new: supports <+out:n> to write a newline on start of that output.
   * <li>2014-11-16 Hartmut chg: enhancement of capability of set text column: <:@ nr> nr can be an expression. Not only a constant.   
   * <li>2014-11-15 Hartmut new: instanceof as compare operator.   
   * <li>2014-10-19 Hartmut chg: {@link JZcmditem#add_dataStruct(StatementList)} with 'M' instead 'X' adequate to change in JZcmdExecuter.
   * <li>2014-08-10 Hartmut new: <:>...<.> as statement writes into the current out Buffer. Before: calculates an textExpression which is never used then.
   *   In opposite:<+>...<.> writes to the main text output always, not to the current buffer. 
   * <li>2014-08-10 Hartmut new: !checkXmlFile = filename; 
   * <li>2014-06-12 Hartmut new: {@link JZcmditem#scriptException(String)} 
   * <li>2014-06-07 Hartmut new: {@link DefClassVariable} with {@link DefClassVariable#loader}:
   *   Syntax: Class var = :loader: package.path.Class; with a loader. 
   * <li>2014-06-07 Hartmut new: {@link JZcmdDatapathElementClass#dpathClass}: 
   *   Using a Class variable instead constant package.path.Class possible. 
   *   Syntax: new &datapath.Classvar:(args) and java &datapath.Classvar:method(args):
   * <li>2014-06-01 Hartmut chg: "File :" as conversion type for any objExpr, not in a dataPath.TODO: Do the same for Filepath, Fileset with accessPath
   * <li>2014-05-18 Hartmut chg: DefFilepath now uses a textValue and divides the path to its components
   *   at runtime, doesn't use the prepFilePath in ZBNF. Reason: More flexibility. The path can be assmebled
   *   on runtime.  
   * <li>2014-05-18 Hartmut new: try to implement javax.script interfaces, not ready yet
   * <li>2014-03-07 Hartmut new: All capabilities from Zmake are joined here. Only one concept!
   * <li>2014-02-22 Hartmut new: Bool and Num as variable types.
   * <li>2014-02-16 Hartmut: new {@link #fileScript} stored here. 
   * <li>2014-01-01 Hartmut re-engineering: {@link JZcmditem} has one of 4 active associations for its content.
   * <li>2013-12-26 Hartmut re-engineering: Now the Statement class is obsolete. Instead all statements have the base class
   *   {@link JZcmditem}. That class contains only elements which are necessary for all statements. Some special statements
   *   have its own class with some more elements, especially for the ZBNF parse result. Compare it with the syntax
   *   in {@link org.vishia.jztxtcmd.JZtxtcmdSyntax}.    
   * <li>2013-07-30 Hartmut chg {@link #translateAndSetGenCtrl(File)} returns void.
   * <li>2013-07-20 Hartmut chg Some syntactical changes.
   * <li>2013-07-14 Hartmut tree traverse enable because {@link Argument#parentList} and {@link StatementList#parentStatement}
   * <li>2013-06-20 Hartmut new: Syntax with extArg for textual Arguments in extra block
   * <li>2013-03-10 Hartmut new: <code><:include:path></code> of a sub script is supported up to now.
   * <li>2013-10-09 Hartmut new: <code><:scriptclass:JavaPath></code> is supported up to now.
   * <li>2013-01-13 Hartmut chg: The {@link ZbatchExpressionSet#ascertainValue(Object, Map, boolean, boolean, boolean)} is moved
   *   and adapted from TextGenerator.getContent. It is a feauture from the Expression to ascertain its value.
   *   That method and {@link ZbatchExpressionSet#text()} can be invoked from a user script immediately.
   *   The {@link ZbatchExpressionSet} is used in {@link org.vishia.zmake.ZmakeUserScript}.
   * <li>2013-01-02 Hartmut chg: localVariableScripts removed. The variables in each script part are processed
   *   in the order of statements of generation. In that kind a variable can be redefined maybe with its own value (cummulative etc.).
   *   A ZText_scriptVariable is valid from the first definition in order of generation statements.
   * <li>2012-12-24 Hartmut chg: Now the 'ReferencedData' are 'namedArgument' and it uses 'dataAccess' inside. 
   *   The 'dataAccess' is represented by a new {@link XXXXXXStatement}('e',...) which can have {@link ZbatchExpressionSet#constValue} 
   *   instead a {@link ZbatchExpressionSet#datapath}. 
   * <li>2012-12-24 Hartmut chg: {@link ZbnfDataPathElement} is a derived class of {@link DataAccess.DatapathElement}
   *   which contains destinations for argument parsing of a called Java-subroutine in a dataPath.  
   * <li>2012-12-23 Hartmut chg: A {@link XXXXXXStatement} and a {@link Argument} have the same usage aspects for arguments
   *   which represents values either as constants or dataPath. Use Argument as super class for ScriptElement.
   * <li>2012-12-23 Hartmut new: formatText in the {@link ZbatchExpressionSet#textArg} if a data path is given, use for formatting a numerical value.   
   * <li>2012-12-22 Hartmut new: Syntax as constant string inside. Some enhancements to set control: {@link #translateAndSetGenCtrl(StringPartBase)} etc.
   * <li>2012-12-22 Hartmut chg: <:if:...> uses {@link CalculatorExpr} for expressions.
   * <li>2012-11-24 Hartmut chg: @{@link XXXXXXStatement#datapath} with {@link DataAccess.DatapathElement} 
   * <li>2012-11-25 Hartmut chg: Now Variables are designated starting with $.
   * <li>2012-10-19 Hartmut chg: <:if...> works.
   * <li>2012-10-19 Hartmut chg: Renaming: {@link XXXXXXStatement} instead Zbnf_ScriptElement (shorter). The Scriptelement
   *   is the component for the genContent-Elements now instead Zbnf_genContent. This class contains attributes of the
   *   content elements. Only if a sub content is need, an instance of Zbnf_genContent is created as {@link XXXXXXStatement#statementlist}.
   *   Furthermore the {@link XXXXXXStatement#statementlist} should be final because it is only created if need for the special 
   *   {@link XXXXXXStatement#elementType}-types (TODO). This version works for {@link org.vishia.stateMGen.StateMGen}.
   * <li>2012-10-11 Hartmut chg Syntax changed of ZmakeGenCtrl.zbnf: dataAccess::={ <$?path>? \.}. 
   *   instead dataAccess::=<$?name>\.<$?elementPart>., it is more universal. adapted. 
   * <li>2012-10-10 new: Some enhancements, it is used for {@link org.vishia.zbatch.ZbatchExecuter} now too.
   * <li>2011-03-00 created.
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
  static final public String version = "2020-01-28";

  final MainCmdLogging_ifc console;

  /**The file which has contained the script. It is used only to provide the variables
   * 'scriptdir' and 'scriptfile' for execution. The file is not evaluated. It means, it does not need
   * to exist. */
  final File fileScript;
  
  /**The JSR-223-conform engine for this script. 
   * It is used for {@link #getEngine()}. */
  final JZtxtcmdEngine scriptEngine;
  
  
  /**All subroutines of this script with name class.sub in alphabetic order to search. */
  final Map<String, Subroutine> subroutinesAll = new TreeMap<String, Subroutine>();
  
  final Map<String, JZcmdClass> classesAll = new IndexMultiTable<String, JZcmdClass>(IndexMultiTable.providerString);
  
  
  
  /**The script element for the whole file. It shall contain calling of <code><*subtext:name:...></code> 
   */
  Subroutine mainRoutine;
  
  /**Text expression for a file name where the script content is written to. */
  protected JZcmditem checkJZcmdFile;
  
  /**Text expression for a file name where the parsed script in XML is written to. */
  JZcmditem checkJZcmdXmlFile;
  
  /**Only set if checkJZcmdXmlFile is not null, removed after processing. */
  XmlNode xmlSrc;
  
  /**The class which presents the script level. */
  JZcmdClass scriptClass;
  
  //public String scriptclassMain;

  /**Creates.
   * @param console
   * @param fileScript The file which has contained the script. It is used only to provide the variables
   *   'scriptdir' and 'scriptfile' for execution. The file is not evaluated. It means, it does not need
   *   to exist.

   */
  public JZtxtcmdScript(MainCmdLogging_ifc console, File fileScript, JZtxtcmdEngine scriptEngine)
  { this.console = console;
    this.fileScript = fileScript;
    this.scriptEngine = scriptEngine;
  }
  
  
  
  
  
  /**Translates with given script in text format.
   * @param fileScript The file which has contained the script. It is used only to provide the variables
   *   'scriptdir' and 'scriptfile' for execution. The file is not evaluated. It means, it does not need
   *   to exist.<br>
   *   The fileScript's parent is the directory which is the anchor for included scripts. 
   *   Maybe null if included scripts are not used and the variable 'scriptfile' and 'scriptdir' are not used in the script.s 
   * @param sourceScript The content of fileScript or from any other source. This is the script source. 
   *   Use new {@link StringPartScan#StringPartScan(CharSequence)} to create one with a String given syntax.
   * @param log if null then the {@link MainCmdLoggingStream} is used with System.out.
   * @param checkXmlOutput may be null, for output the script in XML form.
   * @return
   * @throws ScriptException
   */
  public static JZtxtcmdScript createScriptFromFile(File fileScript,  MainCmdLogging_ifc log, File checkXmlOutput) 
  throws ScriptException {
    int lengthBufferGenctrl = (int)fileScript.length();
    StringPartScan sourceScript;
    try { 
      sourceScript = new StringPartFromFileLines(fileScript, lengthBufferGenctrl, "encoding", null);
    } catch(IOException exc){
      String sError = exc.getMessage();
      throw new ScriptException("JZcmd - Error script file not found; " + fileScript.getAbsolutePath() + "; " + sError); 
    }
    return createScriptFromString(sourceScript, log, checkXmlOutput, fileScript);
  }
  
  
  
  
  /**Translates with given script in text format.
   * @param fileScript The file which has contained the script. It is used only to provide the variables
   *   'scriptdir' and 'scriptfile' for execution. The file is not evaluated. It means, it does not need
   *   to exist.<br>
   *   The fileScript's parent is the directory which is the anchor for included scripts. 
   *   Maybe null if included scripts are not used and the variable 'scriptfile' and 'scriptdir' are not used in the script.s 
   * @param sourceScript The content of fileScript or from any other source. This is the script source. 
   *   Use new {@link StringPartScan#StringPartScan(CharSequence)} to create one with a String given syntax.
   * @param log if null then the {@link MainCmdLoggingStream} is used with System.out.
   * @param checkXmlOutput may be null, for output the script in XML form.
   * @return
   * @throws ScriptException
   */
  public static JZtxtcmdScript createScriptFromString(StringPartScan sourceScript, 
      MainCmdLogging_ifc log, File checkXmlOutput, File fileScript) 
  throws ScriptException
  //throws ParseException, IllegalArgumentException, IllegalAccessException, InstantiationException, FileNotFoundException, IOException 
  { //MainCmdLogging_ifc log1;
    JZtxtcmdScript thiz = new JZtxtcmdScript(log, fileScript, null);
    File dirIncludeBase = fileScript==null ? null : FileSystem.getDir(fileScript);
    JZtxtcmdScript.ZbnfJZcmdScript zbnfDstScript = new JZtxtcmdScript.ZbnfJZcmdScript(thiz);
    thiz.setScriptFromString(sourceScript, zbnfDstScript, dirIncludeBase, checkXmlOutput);
    return thiz;
  }

  
  
  
  /**Translates, internally and recursively for included scripts
   */
  private void setScriptFromString(StringPartScan sourceScript, JZtxtcmdScript.ZbnfJZcmdScript zbnfDstScript
      , File dirIncludeBase 
      , File checkXmlOutput) 
  //throws ParseException, IllegalArgumentException, IllegalAccessException, InstantiationException, FileNotFoundException, IOException 
  throws ScriptException
  { boolean bOk;
    final ZbnfParser parserGenCtrl = new ZbnfParser(this.console);
    try{ parserGenCtrl.setSyntax(JZtxtcmdSyntax.syntax);
    } catch(ParseException exc){ throw new ScriptException("JZcmd.ctor - internal syntax error; " + exc.getMessage()); }
    //
    parserGenCtrl.setXmlSrcline(checkXmlOutput !=null);
    bOk = parserGenCtrl.parse(sourceScript);
    if(!bOk){
      String sError = parserGenCtrl.getSyntaxErrorReport();
      throw new ScriptException("\n" + sError, sourceScript.getInputfile(), -1, -1);
    }
    if(checkXmlOutput !=null){  //may be used if probles with parserGenCtrl2Java.setContent are given.
      //XmlNodeSimple<?> xmlParseResult = parserGenCtrl.getResultTree();
      XmlNode xmlParseResult = parserGenCtrl.getResultTree();
      SimpleXmlOutputter xmlOutputter = new SimpleXmlOutputter();
      try{
        OutputStreamWriter xmlWriter = new OutputStreamWriter(new FileOutputStream(checkXmlOutput));
        xmlOutputter.write(xmlWriter, xmlParseResult);
        xmlWriter.close();
      } catch(IOException exc){ throw new ScriptException(exc); }
    }
    //if(log.getReportLevel() >= MainCmdLogging_ifc.fineInfo){
    //  parserGenCtrl.reportStore((Report)log, MainCmdLogging_ifc.fineInfo, "Zmake-GenScript");
    //}
    //write into Java classes:
    /**Helper to transfer parse result into the java classes {@link ZbnfJZcmdScript} etc. */
    final ZbnfJavaOutput parserGenCtrl2Java = new ZbnfJavaOutput(console);
    //
    //create a new instance of script-file for any file, especially for included.
    zbnfDstScript.scriptfile = new JZtxtcmdScript.Scriptfile();
    
    try {
      parserGenCtrl2Java.setContent(JZtxtcmdScript.ZbnfJZcmdScript.class, zbnfDstScript, parserGenCtrl.getFirstParseResult());
    } catch (Exception exc) { throw new ScriptException(exc); }
    if(zbnfDstScript.isXmlSrcNecessary()){
      zbnfDstScript.setXmlSrc(parserGenCtrl.getResultTree());  //to output XML from Script executer.
    }
    //
    //get the main routine from the first parsed file, store it, set it after processing includefiles.
    //
    JZtxtcmdScript.Subroutine mainRoutine = zbnfDstScript.scriptfile.getMainRoutine();
    //
    if(zbnfDstScript.scriptfile.includes !=null){
      //parse includes after processing this file, because the zbnfDstScript.includes are not set before.
      //If one include contain a main, use it. But override the main after them, see below.
      for(JZtxtcmdScript.JZcmdInclude include: zbnfDstScript.scriptfile.includes){
        String sFileInclude;
        File fileIncludeParent = null;
        StringPartScan sourceScriptIncluded;
        if(include.bJop) {  
          try {
            String sInclScript = (String)DataAccess.access("%" + include.path, null, false, false, false, null);
            sourceScriptIncluded = new StringPartScan(sInclScript);
            fileIncludeParent = dirIncludeBase.getAbsoluteFile();
            int parents = include.parentLevel;
            while(--parents >=0) {
              fileIncludeParent = fileIncludeParent.getParentFile();
            }
            sourceScriptIncluded.setInputfile(FileFunctions.getCanonicalPath(fileIncludeParent) + "/" + include.path);  //It is only for error messages, show it is from a Class.
          }
          catch(Exception exc) { throw new ScriptException(exc); }
        }
        else {
          if(include.envVar !=null){
            String sEnv = System.getenv(include.envVar);
            if(sEnv == null) throw include.scriptException("JZcmd.include - cannot find environment variable;" + include.envVar);          sFileInclude = sEnv + '/' + include.path;
          } else {
            sFileInclude = include.path;
          }
          final File fileInclude;
          if(FileSystem.isAbsolutePath(sFileInclude)){
            fileInclude= new File(sFileInclude);
          } else {
            fileInclude= new File(dirIncludeBase, sFileInclude);
          }
          if(!fileInclude.exists()){
            System.err.printf("TextGenScript - translateAndSetGenCtrl, included file not found; %s\n", fileInclude.getAbsolutePath());
            throw new ScriptException("JZcmd.compile - included file not found: ", fileInclude.getAbsolutePath(), -1, -1);
          }
          fileIncludeParent = FileSystem.getDir(fileInclude);
          int lengthBufferGenctrl = (int)fileInclude.length();
          try {
            sourceScriptIncluded = new StringPartFromFileLines(fileInclude, lengthBufferGenctrl, "encoding", null);
          } catch (Exception exc) { throw new ScriptException(exc); }
          //
        }
        //included script, call recursively.
        setScriptFromString(sourceScriptIncluded, zbnfDstScript, fileIncludeParent, checkXmlOutput);
        sourceScriptIncluded.close();
      }
    }
    //
    //set the main routine from the first parsed file if existing:
    //
    if(mainRoutine !=null){  //the main routine of the main file wins against includes if exists.
      zbnfDstScript.setMainRoutine(mainRoutine);
    }
  }
  

  
  
  
  
  
  /**Executes the main routine of the script. Before that the script variables will be created.
   * @see javax.script.CompiledScript#eval(javax.script.ScriptContext)
   */
  @Override
  public Object eval(ScriptContext context) throws ScriptException
  {
    if(context instanceof JZtxtcmdExecuter.ExecuteLevel){
      JZtxtcmdExecuter.ExecuteLevel level = (JZtxtcmdExecuter.ExecuteLevel) context;
      try{ 
        Subroutine main = getMain();
        level.exec_Subroutine(main, null, null, 0);
      } catch(Throwable exc){ 
        if(exc instanceof Exception){
          throw new ScriptException((Exception)exc); 
        } else {
          throw new RuntimeException("JZcmdScript.eval - unexpected Throwable", exc);
        }
      }
    } else {
      throw new ScriptException("faulty context");
    }
    return null;
  }


  /* (non-Javadoc)
   * @see javax.script.CompiledScript#getEngine()
   */
  @Override public JZtxtcmdEngine getEngine() { return this.scriptEngine; }
  
  

  
  public JZcmdClass scriptClass(){ return this.scriptClass; }
  
  
  
  public final Subroutine getMain(){ return this.mainRoutine; }
  
  
  public Subroutine getSubroutine(CharSequence name){ return subroutinesAll.get(name.toString()); }
  
  public JZcmdClass getClass(CharSequence name){ return classesAll.get(name); }
  
  
  public void writeStruct(Appendable out) throws IOException{
    //mainRoutine.writeStruct(0, out);
    scriptClass.writeStruct(0, out);
  }
  
  
  
  public static interface AddSub2List {
    void clear();
    void add2List(Subroutine jzsub, int level);
    void add2List(JZcmdClass jzclass, int level);
  }
  
  /**Adds the content of the translated script to any list or map which contains all subroutines of the script,
   * maybe a list for selection. 
   */
  public void addContentToSelectContainer(AddSub2List list){
    addSubOfJZcmdClass(scriptClass, list, 1);  
  }
  
  /**Core and recursively called routine.
   * @param jzcmdClass firstly the script class, nested the sub classes.
   * @param level firstly 1, nested 2...
   */
  private void addSubOfJZcmdClass(JZtxtcmdScript.JZcmdClass jzcmdClass, AddSub2List list, int level){
    for(Object classOrSub: jzcmdClass.listClassesAndSubroutines()) { // = e.getValue();
      if(classOrSub instanceof JZtxtcmdScript.Subroutine) {
        JZtxtcmdScript.Subroutine subRoutine = (JZtxtcmdScript.Subroutine) classOrSub;
        if(!subRoutine.name.startsWith("_")) { //ignore internal subroutines!
          list.add2List(subRoutine, level);
        }
      } else {
        assert(classOrSub instanceof JZtxtcmdScript.JZcmdClass);  //what else!
        JZtxtcmdScript.JZcmdClass jzCmdclass = (JZtxtcmdScript.JZcmdClass) classOrSub;
        list.add2List((JZtxtcmdScript.JZcmdClass)classOrSub, level);
        //call recursive for content of class.
        addSubOfJZcmdClass(jzCmdclass, list, level+1);
        
      }
    }
  }
  
  
 
  
  
  
  public static class JZscriptSettings
  {
    String sLinefeed = "\r\n";
    int srcTabsize = 8;
    //boolean bOldIndentBefore2017;
  }
  
  JZscriptSettings jzScriptSettings = new JZscriptSettings();
  
  /**Common Superclass for a JZcmd script item.
   * A script item is either a statement maybe with sub statements, or an expression, or an access to data
   * or a constant text. Therefore only one of the associations {@link #statementlist}, {@link #dataAccess},
   * {@link #expression} or {@link #textArg} is set.
   *
   */
  public static class JZcmditem implements SetLineColumn_ifc
  {
    /**Designation what presents the element.
     * 
     * <table><tr><th>c</th><th>what is it</th></tr>
     * <tr><td>t</td><td>simple constant text</td></tr>
     * <tr><td>n</td><td>simple newline text</td></tr>
     * <tr><td>T</td><td>textual output to any variable or file</td></tr>
     * <tr><td>l</td><td>add to list</td></tr>
     * <tr><td>i</td><td>content of the input, {@link #textArg} describes the build-prescript, 
     *                   see {@link ZmakeGenerator#getPartsFromFilepath(org.vishia.zmake.ZmakeUserScript.Filepath, String)}</td></tr>
     * <tr><td>o</td><td>content of the output, {@link #textArg} describes the build-prescript, 
     *                   see {@link ZmakeGenerator#getPartsFromFilepath(org.vishia.zmake.ZmakeUserScript.Filepath, String)}</td></tr>
     * <tr><td>e</td><td>A datatext, from <*expression> or such.</td></tr>
     * <tr><td>XXXg</td><td>content of a data path starting with an internal variable (reference) or value of the variable.</td></tr>
     * <tr><td>s</td><td>call of a subtext by name. {@link #textArg}==null, {@link #statementlist} == null.</td></tr>
     * <tr><td>j</td><td>call of a static java method. {@link #identArgJbat}==its name, {@link #statementlist} == null.</td></tr>
     * <tr><td>c</td><td>cmd line invocation.</td></tr>
     * <tr><td>d</td><td>cd change current directory.</td></tr>
     * <tr><td>J</td><td>Object variable {@link #identArgJbat}==its name, {@link #statementlist} == null.</td></tr>
     * <tr><td>P</td><td>Pipe variable, {@link #textArg} contains the name of the variable</td></tr>
     * <tr><td>U</td><td>Buffer variable, {@link #textArg} contains the name of the variable</td></tr>
     * <tr><td>S</td><td>String variable, {@link #textArg} contains the name of the variable</td></tr>
     * <tr><td>K</td><td>Numeric variable, {@link #textArg} contains the name of the variable</td></tr>
     * <tr><td>Q</td><td>Bool variable, {@link #textArg} contains the name of the variable</td></tr>
     * <tr><td>L</td><td>Container variable, a list</td></tr>
     * <tr><td>W</td><td>Opened file, a Writer in Java</td></tr>
     * <tr><td>F</td><td>FilePath</td></tr>
     * <tr><td>G</td><td>FileSet</td></tr>
     * <tr><td>=</td><td>assignment of an expression to a variable.</td></tr>
     * <tr><td>B</td><td>statement block</td></tr>
     * <tr><td>C</td><td><:for:path> {@link #statementlist} contains build.script for any list element,</td></tr>
     * <tr><td>E</td><td><:else> {@link #statementlist} contains build.script for any list element,</td></tr>
     * <tr><td>?? F</td><td><:if:condition:path> {@link #statementlist} contains build.script for any list element,</td></tr>
     * <tr><td>?? G</td><td><:elsif:condition:path> {@link #statementlist} contains build.script for any list element,</td></tr>
     * <tr><td>w</td><td>while(cond) {@link #statementlist} contains build.script for any list element,</td></tr>
     * <tr><td>b</td><td>break</td></tr>
     * <tr><td>?</td><td><:if:...?gt> compare-operation in if</td></tr>
     * <tr><td>D</td><td>debug: break at a defined break point in {@link JZtxtcmdExecuter}</td></tr>
     * <tr><td>H</td><td>debugOp: break on invocation of an operation (method) in {@link DataAccess} before evaluating the arguments or if operation not found.</td></tr>
     * 
     * <tr><td>Z</td><td>a target,</td></tr>
     * <tr><td>Y</td><td>the file</td></tr>
     * <tr><td>xxxX</td><td>a subtext definition</td></tr>
     * </table> 
     */
    protected char elementType;    
    
    
    /**Designation of a conversion from given value to a destination instance.
     * old: 'E' to java.io.File
     * '~' complete a CharSequence with the current directory if it is relative.
     * 'F' to {@link FilePath}
     * 'G' to {@link UserFileset}
     */
    protected char conversion = 0;
    
    /**Hint to the source of this parsed argument or statement. */
    int srcLine, srcColumn;
    
    String srcFile = "";
    
    
    
    /**Necessary for throwing exceptions with the {@link StatementList#srcFile} in its text. */
    final StatementList parentList;
    
    /**If need, sub statements, maybe null. An argument may need a StatementList
     * to calculate the value of the argument if is is more complex. Alternatively
     * an Argument can be calculated with the {@link #expression} or with {@link #dataAccess}
     * or it is a simple {@link #textArg}.*/
    public StatementList statementlist;
    
    /**Any access to an Object, maybe content of a variable, maybe access to any Java data,
     * maybe invocation of a Java routine. */
    public JZcmdDataAccess dataAccess;
  
    /**Any calculation of data. */
    public CalculatorExpr expression;

    
    /**Any special sub item if necessary, see conversion. */
    JZcmditem subitem;
    
    /**From Zbnf <""?textInStatement>, constant text, null if not used. */
    public String textArg; 
    
    
    JZcmditem(StatementList parentList, char whatisit){
      if(parentList == null) {
        Debugutil.stop();
      } 
      this.parentList = parentList;
      this.elementType = whatisit;
    }
    
    
    @Override public void setLineColumnFile(int line, int column, String sFile){
      if(srcFile !=null && !srcFile.equals("")){
        Debugutil.stop();
      }
      //if(line == 388 ){ //&& column == 18){
      //  Debugutil.stop();
      //}
      srcLine = line; srcColumn = column; srcFile = sFile; 
    }

    /**Returns wheter only the line or only the column should be set.
     * It can save calculation time if one of the components are not necessary.
     * @return 'c' if only column, 'l' if only line, any other: set both.
     */
    @Override public int setLineColumnFileMode(){ return mLine | mColumn | mFile; }

    
    
    /*package private*/ char elementType(){ return elementType; }
    
    public StatementList statementlist(){ return statementlist; }

    
    private void checkEmpty() {
      if(!isEmpty()) {
        throw new IllegalArgumentException("JZcmdItem with more as one content type.");
      }
    }
    
    
    protected boolean isEmpty() {
      return statementlist == null && dataAccess == null && expression == null && textArg == null;
    }
    
    
    public JZcmdDataAccess new_dataAccess() { 
      checkEmpty();
      return new JZcmdDataAccess(); 
    }
    
    public void add_dataAccess(JZcmdDataAccess val){ 
      dataAccess = val;
    }
    

    public void set_plainText(String text) { if(text.length() >0) set_text(text); } 


    public void set_text(String text) { 
      checkEmpty();
      CharSequence cText;
      if(text.contains("\n=")){   //= on start of line, remove it
        StringBuilder u = new StringBuilder(text);
        cText = u;
        int pos = 0;
        while( (pos = u.indexOf("\n=",pos))>=0){
          u.replace(pos+1, pos+2, "");
        }
      } else {
        cText = text;
      }
      textArg = cText.toString(); //StringSeq.create(cText, true);  //let the text inside the StringBuilder.
      //if(statementlist == null){ statementlist = new StatementList(this); }
      //statementlist.set_text(text);
    }
    
    /**For ZbnfJavaOutput: Creates a statementlist for a dataStruct.
     * <pre>
     * objExpr::= \\{ <dataStruct> \\} | <\"\"?text> | ....
     * dataStruct::= { <DefVariable?> ; }.\n"
     * </pre>
     */
    public StatementList new_dataStruct(){
      checkEmpty();
      statementlist = new StatementList(this);
      if(elementType != 'M') {
        elementType = 'M';     //should be a Map
      }
      return statementlist;
    }
    
    public void add_dataStruct(StatementList val){ }
    
    
    /**For ZbnfJavaOutput: All block statements in if, while etc.
     * <pre>
     * ifBlock::= ( <condition> ) \\{ <statementBlock> \\} .
     * ...
     * statementBlock::= { <statement?> }."
     * </pre>
     */
    public StatementList new_statementBlock(){
      checkEmpty();
      statementlist = new StatementList(this);
      return statementlist;
    }
    
    public void add_statementBlock(StatementList val){ }
    
    

    /**Define the whole item as a statement block which is a text expression.
     * See also {@link StatementList#new_textExpr(ZbnfParseResultItem)}: There such an item
     * is declared inside a longer statement list. The difference is less, both have the same effect. 
     * This list does only contain text expression statements.
     */
    public StatementList new_textExpr(ZbnfParseResultItem zbnfItem) { 
      //checkEmpty();  //don't check empty, on <+:n> there is stored a newline
      if(this.statementlist == null){                      // it is a JZcmdItem either subtext, or textEcpr in other expr 
        this.statementlist = new StatementList(this);      // Should have its own sub statements.
      }
      //The input column counts from 1 for left. identDiff for ex. -3 ist from syntax <syntax?semantic|-3>
      String sIndentDiff = zbnfItem.syntaxItem().getAttribute("indent");
      int indent;                                          // some special designations for text expr.
      if(sIndentDiff !=null && sIndentDiff.length() >0) {
        try{ 
          if(sIndentDiff.charAt(0) == '-') {
            int indentDiff = Integer.parseInt(sIndentDiff.substring(1));
            indent = zbnfItem.getInputColumn() -1 - indentDiff;
          } else if(sIndentDiff.charAt(0) == '+') {
            int indentDiff = Integer.parseInt(sIndentDiff.substring(1));
            indent = zbnfItem.getInputColumn() -1 + indentDiff;
          } else {
            indent = Integer.parseInt(sIndentDiff);
          }
        } catch(NumberFormatException exc) {
          throw new RuntimeException("faulty number for indentdiff", exc);
        }
      } else {
        indent = this.statementlist.parentStatement.statementlist.nIndentInScript; //.ident not given, then from parent. //zbnfItem.getInputColumn() -1;
      }
      this.statementlist.nIndentInScript = indent;   // valid for all textExpr-statements (all statements)
      String sSkipFirstnl = zbnfItem.syntaxItem().getAttribute("skipFirstnl");
      if(sSkipFirstnl !=null) {
        this.statementlist.bSetSkipSpaces = true;    //Note: valid for the first line because set to false after first line 
      }
      return this.statementlist;
    }
    
    public void add_textExpr(StatementList val){}

    public JZcmdCalculatorExpr new_numExpr() { 
      checkEmpty();
      return new JZcmdCalculatorExpr(this); 
    }

    public void add_numExpr(JZcmdCalculatorExpr val){ 
      DataAccess dataAccess1 = val.onlyDataAccess();
      if(dataAccess1 !=null){
        this.dataAccess = (JZcmdDataAccess)dataAccess1;
      } else {
        val.closeExprPreparation();
        this.expression = val.expr(); 
      }
    }
    
    
    public JZcmdCalculatorExpr new_boolExpr() { 
      checkEmpty();
      return new JZcmdCalculatorExpr(this); 
    }

    public void add_boolExpr(JZcmdCalculatorExpr val){ 
      DataAccess dataAccess1 = val.onlyDataAccess();
      if(dataAccess1 !=null){
        this.dataAccess = (JZcmdDataAccess)dataAccess1;
      } else {
        val.closeExprPreparation();
        this.expression = val.expr(); 
      }
    }
    
    
    /**For ZbnfJavaOutput: An < objExpr> is designated as "File : < textValue?File>"
     * @return this, the conversion is set.
     */
    public JZcmditem XXXnew_File(){ 
      conversion = 'E';
      return this;
    }
    
    public void XXXadd_File(JZcmditem val){} //do nothing. 
    
    public JZcmditem new_Filepath(){ conversion = 'F'; return this; }
    
    public void add_Filepath(JZcmditem val){} //do nothing. conversion was set.
    
    public AccessFilesetname new_filesetAccess(){ return new AccessFilesetname(parentList); }
    
    public void add_filesetAccess(AccessFilesetname val){ conversion = 'G'; subitem = val; };
    

    
    static String sindentA = "                                                                               "; 
    
    
    /**Builds a ScriptException with the given text and the srcFile, line, column of this item. 
     * @param text message of the ScriptException
     */
    public ScriptException scriptException(String text) {
      return new ScriptException(text, srcFile, srcLine, srcColumn);
    }
    
    /**Writes a complete readable information about this item with all nested information.
     * @param indent
     * @param out
     * @throws IOException
     */
    final void writeStruct(int indent, Appendable out) throws IOException{
      String sIndent= (2*indent < sindentA.length()-2) ? sindentA.substring(0, 2*indent) : sindentA;
      out.append(sIndent);
      writeStructLine(out);
      writeStructAdd(indent, out);
      if(textArg !=null){
        out.append("\"").append(textArg).append("\"");
      }
      if(dataAccess !=null){
        dataAccess.writeStruct(out);
      }
      if(expression !=null){
        String sExpr = expression.toString();
        out.append(sExpr);
      }
      out.append("\n");
      if(statementlist !=null){
        for(JZcmditem item: statementlist.statements){
          item.writeStruct(indent+1, out);
        }
      }
    }
    
    /**Prepares information in following lines if necessary. It is possible to append in the only one line too.
     * This routine have to be append a newline at last.
     * @param indent
     * @param out
     * @throws IOException
     */
    void writeStructAdd(int indent, Appendable out) throws IOException{  }
    
    
    /**Prepares the information about the JZcmditem in one line. A newline is not appended here! 
     * This routine is called in {@link #toString()} and in {@link #writeStruct(int, Appendable)}.
     * It should be called in all overridden routines with super.writeStructLine
     * for the derived statement types. 
     * @param u 
     * @throws IOException
     */
    void writeStructLine(Appendable u) {
      try{
        switch(elementType){
          case 't': u.append(" text \"").append(textArg).append("\""); break;
          /*
          case 'S': u.append("String " + identArgJbat;
          case 'O': u.append("Obj " + identArgJbat;
          case 'P': u.append("Pipe " + identArgJbat;
          case 'U': u.append("Buffer " + identArgJbat;
          case 'o': u.append("(?outp." + textArg + "?)";
          case 'i': u.append("(?inp." + textArg + "?)";
          */
          case 'e': u.append(" <*)"); break;  //expressions.get(0).dataAccess
          //case 'g': u.append("<$" + path + ">";
          case 'B': u.append(" { statementblock }"); break;
          case 'D': u.append(" debug"); break;
          case 'E': u.append(" else "); break;
          case 'F': u.append(" Filepath "); break;
          case 'G': u.append(" Fileset "); break;
          case 'H': u.append(" debugOp"); break;
          case 'I': u.append(" (?forInput?)...(/?)"); break;
          case 'M': u.append(" Map "); break;
          case 'N': u.append(" <:hasNext> content <.hasNext>"); break;
          case 'O': u.append(" Obj"); break;
          case 'L': u.append(" List"); break;
          case 'W': u.append(" Openfile"); break;
          case 'Z': u.append(" zmake"); break;
          case 'Y': u.append(" <:file> "); break;
          case 'b': u.append(" break; "); break;
          case 'c': u.append(" cmd "); break;
          case 'd': u.append(" cd "); break;
          case 'f': u.append(" for "); break;
          case 'g': u.append(" elsif "); break;
          case 'i': u.append(" if "); break;
          case 'm': u.append(" move "); break;
          case 'n': u.append(" newline "); break;
          case 'o': u.append(" createTextOut "); break;
          case 'q': u.append(" appendTextOut "); break;
          case 'r': u.append(" throw "); break;
          //case 's': u.append("call " + identArgJbat;
          case 's': u.append(" call "); break;
          case 'v': u.append(" throw on error "); break;
          case 'w': u.append(" while "); break;
          case 'x': u.append(" thread "); break;
          case 'y': u.append(" copy "); break;
          case 'z': u.append(" exit "); break;
          case '9': u.append(" mkdir "); break;
          case ':': u.append(" <:> ... <.>"); break;
          case '!': u.append(" flush "); break;
          case '{': u.append(" statements "); break;
          case '[': u.append(" List-container "); break;
          case '*': u.append(" Map-container "); break;
          case '_': u.append(" close "); break;
          case ' ': u.append(" skipWhitespace "); break;
          case ',': u.append(" errortoOutput "); if(textArg == null){ u.append("off "); } break;
          default: //do nothing. Fo in overridden method.
        }
        u.append("-").append(elementType).append(" @").append(Integer.toString(srcLine)).append(",").append(Integer.toString(srcColumn)).append(" ").append(srcFile).append(':');
        
      } catch(IOException exc){
        throw new RuntimeException(exc); //unexpected.
      }
    }
    
    
    @Override public String toString(){
      StringBuilder u = new StringBuilder();
      writeStructLine(u); //append on StringBuilder has not a IOException!
      return u.toString();
    }

  }
  
  
  
  
  
  
  /**Argument for a subroutine. It has a name. 
   *
   */
  public static class Argument extends JZcmditem  { //CalculatorExpr.Datapath{
    
    /**Name of the argument. It is the key to assign calling argument values. */
    public String identArgJbat;
    
    /**Set whether the argument is a filepath. The the references of JZcmditem are null.*/
    //protected FilePath filepath;
    
    //protected AccessFilesetname accessFileset;
   
    public Argument(StatementList parentList){
      super(parentList, '.');
    }
    
    public void set_name(String name){ this.identArgJbat = name; }
    
    public String getIdent(){ return identArgJbat; }
    
    

    
  }
  
 

  
  /**This class wraps the {@link DataAccess.DataAccessSet} to support JZcmd-specific enhancements.
   * All methods for ZBNF java output of the super class are used too.
   */
  public static class JZcmdDataAccess extends DataAccess.DataAccessSet 
  implements GetTypeToUse
  {
    //protected String filepath;
    
    public JZcmdDataAccess(){ super(); }
    
    @Override public Class<?> getTypeToUse(){ return JZcmdDataAccess.class; }
    

    public JZcmditem new_File(){ return new JZcmditem(null, 'A'); } 
    
    public void set_File(JZcmditem val){ 
      JZcmdDatapathElementClass elem = new JZcmdDatapathElementClass();
      elem.set_Class(java.io.File.class);
      elem.set_whatisit("+");  //new File, add current dir
      val.conversion = '~';    //set a relative to an absolute path.
      elem.add_argument(val);
      add_newJavaClass(elem);
    }
    
    
    @Override public SetDatapathElement new_startDatapath(){ return new JZcmdDatapathElement(); }
    
    /**Returns the derived instance of {@link DataAccess.DatapathElement} which supports JZcmd.
     * It is invoked by semantic datapathElement.
     * @see org.vishia.util.DataAccess.DataAccessSet#new_datapathElement()
     */
    @Override public final JZcmdDatapathElement new_datapathElement(){ return new JZcmdDatapathElement(); }

    /**For ZbnfJavaOutput: It should add a type JZcmdDatapathElement 
     *   instead {@link DataAccess.DataAccessSet#add_datapathElement(DatapathElement)}.
     * @param val
     */
    public final void add_datapathElement(JZcmdDatapathElement val){ 
      super.add_datapathElement(val); 
    }
    
    /**Returns the derived instance of {@link DataAccess.DatapathElementClass} which supports JZcmd.
     * It is invoked by semantic newJavaClass and staticJavaMethod,
     * see {@link DataAccess.DataAccessSet#new_newJavaClass()}, {@link DataAccess.DataAccessSet#new_staticJavaMethod()}. 
     * @see org.vishia.util.DataAccess.DataAccessSet#new_datapathElementClass()
     */
    @Override public final JZcmdDatapathElementClass newDatapathElementClass(){ return new JZcmdDatapathElementClass(); }


  }
  
  
  
  
  /**This class extends the {@link DatapathElement} respectively {@link SetDatapathElement}
   * with more references which contains the method arguments which should be evaluated with the JZcmd
   * capability.
   * @author Hartmut
   *
   */
  public static class JZcmdDatapathElement extends DataAccess.SetDatapathElement 
  implements GetTypeToUse 
  {
    /**For ZbnfJavaOutput: Expressions to calculate the {@link #fnArgs} for a method or constructor arguments.
     * Maybe null if not arguments are necessary.
     */
    protected List<JZcmditem> fnArgsExpr;

    /**For ZbnfJavaOutput: A datapath which accesses the identifier for this element.
     * The identifier is indirect (referenced). */
    protected JZcmdDataAccess indirectDatapath;
    
    public JZcmdDatapathElement(){ super(); }
    
    @Override public Class<?> getTypeToUse(){ return JZcmdDatapathElement.class; }
    
    /**For ZbnfJavaOutput: Creates a new item for an argument of a method or constructor. */
    public JZcmditem new_argument(){ return new JZcmditem(null, 'A'); }
    
    /**For ZbnfJavaOutput: Adds argument to {@link #fnArgsExpr}. */
    public void add_argument(JZcmditem val){ 
      if(fnArgsExpr == null){ fnArgsExpr = new ArrayList<JZcmditem>(); }
      fnArgsExpr.add(val);
    } 

    
    /**For ZbnfJavaOutput: Creates a datapath which accesses the identifier for this element.
     * The identifier may be indirect (referenced). The syntax is:
     * <pre>
     *   startDatapath::= [ & ( <dataPath> ) | <$-?ident> ] ....
     * </pre>
     * Write 
     * <pre>
     * 
     * </pre>
     * */
    public JZcmdDataAccess new_dataPath(){
      return new JZcmdDataAccess();
    }

    
    public void add_dataPath(JZcmdDataAccess val){
      indirectDatapath = val;
    }
    
    public void writeStruct(int indent, Appendable out) throws IOException {
      out.append(ident).append(':').append(whatisit);
      if(fnArgsExpr!=null){
        //String sep = "(";
        for(JZcmditem arg: fnArgsExpr){
          arg.writeStruct(indent+1, out);
        }
      }
    }
  }
  

  
  
  public static class JZcmdDatapathElementClass extends DataAccess.DatapathElementClass 
  implements GetTypeToUse {

    /**The name of that variable which is used as Loader for classes. null if not used (it is optional). 
     * It should refer instanceof {@link java.lang.Classloader}*/
    JZcmdDataAccess dpathLoader;
    
    /**For ZbnfJavaOutput: Expressions to calculate the {@link #fnArgs} for a method or constructor arguments.
     * Maybe null if not arguments are necessary.
     */
    protected List<JZcmditem> fnArgsExpr;

    /**Access to data which describes the class. null if not used. 
     * It should refer instanceof {@link java.lang.Class}. */
    protected JZcmdDataAccess dpathClass;
    
    @Override public Class<?> getTypeToUse(){ return JZcmdDatapathElementClass.class; }
    
    /**For ZbnfJavaOutput: Creates a new item for an argument of a method or constructor. */
    public JZcmditem new_argument(){ return new JZcmditem(null, 'A'); }
    
    /**For ZbnfJavaOutput: Adds argument to {@link #fnArgsExpr}. */
    public void add_argument(JZcmditem val){ 
      if(fnArgsExpr == null){ fnArgsExpr = new ArrayList<JZcmditem>(); }
      fnArgsExpr.add(val);
    } 

    
    public JZcmdDataAccess new_Class_Var(){ return new JZcmdDataAccess(); }
    
    public void add_Class_Var(JZcmdDataAccess val){ dpathClass = val; }
    
    /**For ZbnfJavaOutput: Creates a datapath for a specific ClassLoader. */
    public JZcmdDataAccess new_Classpath_Var(){ return new JZcmdDataAccess(); }
    
    /**For ZbnfJavaOutput: Sets the datapath for a specific ClassLoader. */
    public void add_Classpath_Var(JZcmdDataAccess val){ dpathLoader = val; }
    

  }
  
  
  
  public static class JZcmdCalculatorExpr extends CalculatorExpr.SetExprBase.SetExpr 
  implements GetTypeToUse
  {
    
    JZcmdCalculatorExpr(Object dbgParent){ new CalculatorExpr.SetExprBase(true, dbgParent).super(); }
    
    @Override public Class<?> getTypeToUse(){ return JZcmdCalculatorExpr.class; }
    


    /**It creates an {@link JZcmdDataAccess} because overridden {@link #newDataAccessSet()}
     * of {@link CalculatorExpr.SetExpr#newDataAccessSet()} 
     */
    @Override public JZcmdDataAccess new_dataAccess(){ 
      return (JZcmdDataAccess)super.new_dataAccess();  
    }

    public void add_dataAccess(JZcmdDataAccess val){ }

    @Override protected JZcmdDataAccess newDataAccessSet(){ return new JZcmdDataAccess(); }
    
    
    public JZcmdInstanceofExpr new_instanceof() { return new JZcmdInstanceofExpr(); }
    public void add_instanceof(JZcmdInstanceofExpr val) {  }
    
  }
  
  
  
  
  public static class JZcmdInstanceofExpr
  {
    
    
    
    
  }
  
  
  
  
  /**A < fileset> in the ZmakeStd.zbnf. It is assigned to a script variable if it was created by parsing the ZmakeUserScript.
   * If the fileset is used in a target, it is associated to the target to get the absolute paths of the files
   * temporary while processing that target.
   * <br><br>
   * The Zbnf syntax for parsing is defined as
   * <pre>
   * fileset::= { basepath = <file?basepath> | <file> ? , }.
   * </pre>
   * The <code>basepath</code> is a general path for all files which is the basepath (in opposite to localpath of each file)
   * or which is a pre-basepath if any file is given with basepath.
   * <br><br>
   * Uml-Notation see {@link org.vishia.util.Docu_UML_simpleNotation}:
   * <pre>
   *               UserFileset
   *                    |------------commonBasepath-------->{@link Filepath}
   *                    |
   *                    |------------filesOfFileset-------*>{@link Filepath}
   *                                                        -drive:
   *                                                        -absPath: boolean
   *                                                        -basepath
   *                                                        -localdir
   *                                                        -name
   *                                                        -someFiles: boolean
   *                                                        -ext
   * </pre>
   * 
   */
  public static class UserFileset extends DefVariable
  {
    
    final JZtxtcmdScript script;
    
    public final FileSet fileset = new FileSet();
    
    UserFileset(StatementList parentList, JZtxtcmdScript script){
      super(parentList, 'G');
      this.script = script;
    }
    
    
    public UserFileset(StatementList parentList){
      super(parentList, 'G');
      this.script = null;
    }
    
    
    public void set_name(String name){
      JZcmdDataAccess dataAccess1 = new JZcmdDataAccess();
      this.dataAccess = dataAccess1;
      dataAccess1.set_startVariable(name);
    }
    
    
    /**From Zbnf: srcpath = <""?!prepSrcpath>.
     * It sets the base path for all files of this fileset. This basepath is usually relative.
     * @return ZBNF component.
     */
    //public FilePath.ZbnfFilepath new_commonpath(){ return new FilePath.ZbnfFilepath(); }  //NOTE: it has not a parent. this is not its parent!
    //public void set_commonpath(FilePath.ZbnfFilepath val){ commonBasepath = val.filepath; }
    
    public void set_commonPath(String val){ fileset.set_commonPath(val); }
    
    public void set_filePath(String val){ fileset.add_filePath(val); }
    
    public void add_addFileset(String val) { }
    
  }
  
  

  
  /**A Filepath variable. Note that the {@link Filepath} is defined in org.vishia.util independent of JZcmd.
   * It is aggreagated here. */
  public static class DefFilepath extends DefVariable
  {
    FilePath filepath;

    DefFilepath(StatementList parentList){
      super(parentList, 'F');
    }
  
    
  }
 
  
  
  
  

  public static class Zmake extends CallStatement {

    
    JZcmditem jzoutput;
    
    String name;
    
    List<AccessFilesetname> input = new ArrayList<AccessFilesetname>();
    
    Zmake(StatementList parentList)
    { super(parentList, 'Z');
    }
    
    
    
    public void set_name(String name){ this.name = name; }
    
    
    
    public JZcmditem new_zmakeOutput(){ return new JZcmditem(parentList, '.'); }
    
    public void add_zmakeOutput(JZcmditem val){ jzoutput = val; }
    
    @Override
    public AccessFilesetname new_filesetAccess(){ return new AccessFilesetname(parentList); }
    
    @Override
    public void add_filesetAccess(AccessFilesetname val){ input.add(val); };
    
    
  }
  
  
  
  /**This class contains the fileset-variable name and maybe an FilePath as accessPath.
   * The class is similar like {@link JZtxtcmdAccessFileset} but this class 
   * contains the name of the fileset instead the fileset-reference itself
   * and it contains a {@link FilePath} instead a {@link JZtxtcmdFilepath}. 
   * It is the form which is gotten from a textual script by translating the script.
   * The {@link JZtxtcmdAccessFileset} is build on running time in the adequate execution level 
   * of the @link {@link JZtxtcmdExecuter.ExecuteLevel}
   */
  public static class AccessFilesetname extends JZcmditem{
    
    /**From Zbnf, if null then {@link #zmakeFilepathName} may be the fileset. */
    String XXXfilesetVariableName;
    
    
    JZcmdDataAccess filesetVariable;
    
    /**The filepath for the fileset. */
    //FilePath accessPath;
    
    public AccessFilesetname(StatementList list){
      super(list, 'G');
    }
    
    /**For ZbnfJavaOutput: <""?accessPath>. Note that the access path can be given with an text expression,
     * then it is stored in {@link JZcmditem#statementlist}.
     * 
     */
    public void set_accessPath(String val){
      textArg = val;
      //accessPath = new FilePath(val);  //prepare it with its parts.
    }

    /**For ZbnfJavaOutput: The first occurrence of &name is checked whether it is a accesspath or a fileset.
     */
    /*
    public void set_accessPathOrFilesetVariable(String val){ 
      if(val.startsWith("&") && StringFunctions.indexOfAnyChar(val, 0, val.length(), "\\/:") <0){
        //contains only &name
        filesetVariableName = val.substring(1);  //Note: nevertheless it may be an accessPath, check in set_zmakeFilesetVariable  
      } else {
        textArg = val;
        //accessPath = new FilePath(val);  //prepare it with its parts.
      }
    }
    */
    
    /**For ZbnfJavaOutput. The second occurrence of &name is supposed as the fileset anyway.
     * An accessPath is really an accessPath. If the {@link #filesetVariableName} is set already,
     * it is an accessPath with a variable.
     * @param val
     */
    public void XXXset_zmakeFilesetVariable(String val){
      if(XXXfilesetVariableName !=null){
        //assert(accessPath == null);
        //a simple accessPath "&name" was recognized as the filesetVariablebame,
        //but it is an accessPath really.
        textArg = "&" + XXXfilesetVariableName;
        //accessPath = new FilePath("&" + filesetVariableName);  //create afterward.
      }
      this.XXXfilesetVariableName = val; 
    }
    
    
    
    public JZcmdDataAccess new_zmakeFilesetVariable() { 
      return new JZcmdDataAccess(); 
    }
    
    public void add_zmakeFilesetVariable(JZcmdDataAccess val){ 
      this.filesetVariable = val; 
    }
    

  }
  
  
  
  /**In ZBNF: <*dataAccess:formatString>
   */
  public static class DataText extends JZcmditem
  {
    public DataText(StatementList parentList)
    { super(parentList, 'e');
    }

    public String format;
    
    public void set_formatText(String text){ this.format = text; }
    
    public String errorText;
    
    public void set_errorText(String text){ this.errorText = text; }
    

  }
  
  

  
  
  
  public static class DefVariable extends JZcmditem
  {
    
    /**The variable which should be created. 
     * The variable maybe build with name.subname.subname. 
     * It is possible to add an element to an internal container in Java data. 
     */
    public DataAccess defVariable;
    
    String typeVariable;
    
    boolean bConst;
    
    boolean bAppendToFile;
    
    DefVariable(StatementList parentList, char type){
      super(parentList, type);
    }
    
    
    /**From Zbnf: [ const <?const>] */
    public void set_const(){ bConst = true; } 
    
    /**Only used for Openfile += name;
     * 
     */
    public void set_appendToFile() { bAppendToFile = true; }
    
    
    public void set_type(String val){ typeVariable = val; }
    
    public void set_name(String val){  }
    
    
    /**From Zbnf: < variable?defVariable> inside a DefVariable::=...
     */
    public JZcmdDataAccess new_defVariable(){ return new JZcmdDataAccess(); }
    
    public void add_defVariable(JZcmdDataAccess val){   
      int whichStatement =     "SPULOKQWMCJFG*{[\0".indexOf(elementType);
      char whichVariableType = "SPULOKQAMCJFG*X[\0".charAt(whichStatement);  //from elementType to variable type.
      if(bConst){
        whichVariableType = Character.toLowerCase(whichVariableType);  //see DataAccess.access
      }
      val.setTypeToLastElement(whichVariableType);
      defVariable = val;
    }

    //public void new_statementBlock(){}
    
    //public void set_name(String val){}
    
    /**Returns the simple variable name if the variable is on one level only.
     * Returns name.name for more levels.
     * @return
     */
    public String getVariableIdent(){
      final String name; 
      List<DataAccess.DatapathElement> path = defVariable.datapath();
      int zpath = path.size();
      if(path == null || zpath ==0){
        name = null;
      }
      else if(path.size() == 1){
        name = defVariable.datapath().get(0).ident();
      } else {
        name = null;  //TODO name.name
      }
      return name;
    }
    
    
    @Override void writeStructLine(Appendable out) {
      super.writeStructLine(out);
      try{ out.append(" Defvariable ").append(defVariable !=null ? defVariable.toString(): "no_Variable");
      
      }catch(IOException exc){ throw new RuntimeException(exc); }  //unexpected
    }
    
  };
  
  
  
  
  
  
  /**It is a List, either as constant container or as any java.util.List-instance.
   * <ul>
   * <li>The variable name is stored in the super class {@link DefVariable#defVariable}.
   * <li>A constant container is contained in {@link #elements}. If the Zbnf: <?element> was found, then a Map in {@link #element} is created and added to {@link #elements}.
   * <li>If a Zbnf: <$?name> was found, it is stored only yet. If a Zbnf: <<""?text> was found, then the text with the name is added to the element.
   * <li>If a Zbnf <?element> was not found, then a <$?name> or <""?text> cannot be found because syntax.
   * <li>If a < objExpr?> was found, it is stored in {@link JZcmditem#expression} etc. like other variable definitions.
   * </ul>  
   */
  public static class DefContainerVariable extends DefVariable
  {
    
    //ArrayList<Map<String, String>> elements;
    
    /**Name of that variable which value builds the key.*/
    public String keyVariableName;
    
    boolean bFirst = true;
  
    DefContainerVariable(StatementList parentList, char whatIsit)
    {
      super(parentList, whatIsit);
    }
    
    
    /**From Zbnf: <?element>. Creates an {@link IndexMultiTable} in {@link #element} and adds it to {@link #elements}.
     * @return this
     */
    public DefListElement new_element(){  
      if(statementlist == null){ statementlist = new StatementList(this); }
      return new DefListElement();
    }
    
    public void add_element(DefListElement val){ 
      statementlist.statements.add(val);
    }
    
    public DefVariable new_textVariable(){ 
      if(statementlist == null){ statementlist = new StatementList(this); }
      return new DefVariable(parentList, 'S');    
    }
   
    public void add_textVariable(DefVariable val){
      statementlist.statements.add(val);
    }
    
    public DefVariable XXXnew_DefSubtext(){
      return new DefVariable(statementlist, '\0');  
    } 

    public void XXXadd_DefSubtext(DefVariable val){ 
      if(val.statementlist !=null) {
        val.elementType = '{';  //a code block, statement block which should be executed on used time.
      } else {
        val.elementType = 'O';  //an expression which should be evaluated on build-time of the container
      }
      if(statementlist == null){ statementlist = new StatementList(this); }
      statementlist.statements.add(val);  
    }
    
    

    public Subroutine new_DefSubtext(){
      StatementList parent = parentList;
      while(parent !=null && !(parent instanceof JZcmdClass)){
        parent = parent.parentStatement.parentList;
      }
      JZcmdClass jzClass = parent == null ? null : (JZcmdClass)parent;
      return new Subroutine(jzClass);  
    } 

    public void add_DefSubtext(Subroutine val){ 
      if(val.statementlist !=null) {
        val.elementType = '{';  //a code block, statement block which should be executed on used time.
      } else {
        val.elementType = 'O';  //an expression which should be evaluated on build-time of the container
      }
      if(statementlist == null){ statementlist = new StatementList(this); }
      statementlist.statements.add(val);  
    }
    
    

    
    public JZcmditem new_objElement() {
      if(bFirst && isEmpty()) { return this; //first objElement, the only one is the value which is the list.
      } else {
        if(bFirst) {
          bFirst = false;
          //it is the second invocation
          JZcmditem element1 = new JZcmditem(statementlist, '\0');
          element1.dataAccess = this.dataAccess; this.dataAccess = null;
          element1.textArg = this.textArg; this.textArg = null;
          element1.expression = this.expression; this.expression = null;
          element1.statementlist = this.statementlist; 
          statementlist = new StatementList(this);
          statementlist.statements.add(element1);  //add the copied first content.
        }    
        JZcmditem element = new JZcmditem(statementlist, '\0');
        return element;
      }
    }
    
    public void add_objElement(JZcmditem val) {
      if(val !=this) {
        statementlist.statements.add(val);
      } //else: this is the first or only one. It is the initial value for the list.
    }
    
  }
  
  
  
  
  /**A list element is a statement in the {@link JZcmditem#statementlist} of the {@link DefContainerVariable} item.
   * It can be a DefVariable 
   */
  public static class DefListElement extends DefVariable
  {
    //private Map<String, String> element;
    
    /**From Zbnf: stores the name for an element. */
    //public String elementName;
    
  
    DefListElement(){
      super(null, '\0');  //'*'
    }
    
    public StatementList new_dataSet() {
      //element = new IndexMultiTable<String, String>(IndexMultiTable.providerString);
      super.elementType = '*';
      return super.statementlist = new StatementList(this);
    }

    
    public void add_dataSet(StatementList val) { }
    
    
    public DefVariable XXXnew_textVariable(){ 
      super.elementType = 'S';
      return this; 
    }
    
    public void XXXadd_textVariable(DefVariable val){}
    
    /**Puts a new element with previous stored {@link #elementName}.
     * @param text
     */
    public void XXXset_elementText(String text) {
      //element.put(elementName, text); 
    }
    
    
    
  }
  
  public static class DefClassVariable extends DefVariable
  {
    
    JZcmdDataAccess loader;
    
    DefClassVariable(StatementList parentList)
    {
      super(parentList, 'C');
    }
    
    
    public JZcmdDataAccess new_loader(){ return new JZcmdDataAccess(); }
    
    public void add_loader(JZcmdDataAccess val){ loader = val; };
    
    
  }
  
  
  
  
  
  public static class DefClasspathVariable extends DefVariable
  {
    
    List<AccessFilesetname> jarpaths = new ArrayList<AccessFilesetname>();

    /**Name of the Classpath variable which refers the possible parent ClassLoader.
     * If null, then the ClassLoader from JZcmdExecuter is used. */
    String nameParentClasspath;
    
    DefClasspathVariable(StatementList parentList)
    {
      super(parentList, 'J');
    }
    
    
    @Override
    public AccessFilesetname new_filesetAccess(){ return new AccessFilesetname(parentList); }
    
    @Override
    public void add_filesetAccess(AccessFilesetname val){ jarpaths.add(val); };
    

    public void set_parentClasspath(String val){
      nameParentClasspath = val;
    }
    
  }
  
  
  
  public static class FileOpArg extends JZcmditem
  {
    Argument src, dst;
    
    boolean bNewTimestamp, bOverwrite, bOverwriteReadonly;
    
    FileOpArg(StatementList parentList, char whatisit) {
      super(parentList, whatisit);
    }

    public Argument new_src(){ return src = new Argument(parentList); }
    
    public void set_src(Argument val) {} 
    
    public Argument new_dst(){ return dst = new Argument(parentList); }
    
    public void set_dst(Argument val) {}
    
    public void set_newTimestamp(){ bNewTimestamp = true; }
    
    public void set_overwr(){ bOverwrite = true; }
    
    public void set_overwro(){ bOverwriteReadonly = true; }
    
  }  
  
  public static class AssignExpr extends JZcmditem
  {

    /**Any variable given by name or java instance  which is used to assign to it.
     * A variable is given by the start element of the data path. An instance is given by any more complex datapath
     * null if not used. */
    public List<JZcmdDataAccess> assignObjs;
    
    
    /**The variable which should be created or to which a value is assigned to. */
    public JZcmdDataAccess variable;
    
    AssignExpr(StatementList parentList, char elementType)
    { super(parentList, elementType);
    }
    
    /**From Zbnf: [{ <dataAccess?-assign> = }] 
     */
    public JZcmdDataAccess new_assign(){ return new JZcmdDataAccess(); }
    
    public void add_assign(JZcmdDataAccess val){ 
      if(variable == null){ variable = val; }
      else {
        if(assignObjs == null){ assignObjs = new LinkedList<JZcmdDataAccess>(); }
        assignObjs.add(val); 
      }
    }

    
    public void set_append(){
      if(elementType == '='){ elementType = '+'; }
      else throw new IllegalArgumentException("JZcmdScript - unexpected set_append");
    }
    
    
    @Override void writeStructLine(Appendable out) {
      super.writeStructLine(out);
      try{
        if(variable !=null){
          out.append(" assign ");
          variable.writeStruct(out);
          out.append(" = ");        
        } else {
          out.append(" invoke ");
        }
      }catch(IOException exc){ throw new RuntimeException(exc); }  //unexpected
    }

    
  }
  
  
  public static class TextOut extends JZcmditem
  {

    /**The variable which should be created or to which a value is assigned to. 
     * If it is left null, then the text will be output to the current channel, the output file on main level. */
    public JZcmdDataAccess variable;
    
    //int indent;
    
    TextOut(StatementList parentList, char elementType)
    { super(parentList, elementType);
    }
    
    
    
    /**Sets the column where the syntax component TextOut has its start in the source.
     * See {@link org.vishia.zbnf.ZbnfJavaOutput}, it is a special feature of them.
     * @param col countered from 1 as first character in line.
     */
    //public void set_inputColumn_(int col){ this.indent = col; } 
    
    /**From Zbnf: [{ <dataAccess?-assign> = }] 
     */
    public JZcmdDataAccess new_assign(){ return new JZcmdDataAccess(); }
    
    public void add_assign(JZcmdDataAccess val){ 
      variable = val; 
    }

    public void set_newline(){
      if(statementlist == null){ statementlist = new StatementList(this); }
      statementlist.statements.add(new JZcmditem(parentList, 'n'));  
    }

    public void set_flush(){
      if(statementlist == null){ statementlist = new StatementList(this); }
      statementlist.statements.add(new JZcmditem(parentList, '!'));  
    }

    public void set_close(){
      if(statementlist == null){ statementlist = new StatementList(this); }
      statementlist.statements.add(new JZcmditem(parentList, '_'));  
    }

  }
  
  
  
  /**JZcmditem which contain a column for text positioning.
   * Created on < @pos> statement. 
   */
  public static class TextColumn extends JZcmditem
  {
    
    /**If given then at least this number of spaces are added on setColumn. 
     * The column is not be exact than, but existing text won't be not overridden.
     * If -1 then the setPosition may override existing text, but the column is exact.
     */
    CalculatorExpr minSpaces;
    
    JZcmdDataAccess minSpacesDataAccess;
    
    TextColumn(StatementList parentList)
    { super(parentList, '@');
    }

    
    public JZcmdCalculatorExpr new_minSpaces(){ return new JZcmdCalculatorExpr(this); }
    
    public void add_minSpaces(JZcmdCalculatorExpr val){ 
      DataAccess dataAccess1 = val.onlyDataAccess();
      if(dataAccess1 !=null){
        this.minSpacesDataAccess = (JZcmdDataAccess)dataAccess1;
      } else {
        val.closeExprPreparation();
        this.minSpaces = val.expr(); 
      }
    }
    
    
    @Override
    void writeStructAdd(int indent, Appendable out) throws IOException{ out.append(" setColumn ").append(Integer.toString(0x7777777)); }

  }
  
  
  
  
  public static class IfStatement extends JZcmditem
  {

    IfStatement(StatementList parentList, char whatisit)
    { super(parentList, whatisit);
    }
    
    public IfCondition new_ifBlock()
    { //StatementList subGenContent = new StatementList(this);
      IfCondition statement = new IfCondition(parentList, 'g');
      //statement.statementlist = subGenContent;  //The statement contains a genContent. 
      statementlist.statements.add(statement);
      statementlist.onerrorAccu = null; statementlist.withoutOnerror.add(statement);
      return statement;
    }
    
    public void add_ifBlock(IfCondition val){}



    public StatementList new_elseBlock()
    { JZcmditem statement = new JZcmditem(parentList, 'E');
      statement.statementlist = new StatementList(this);  //The statement contains a genContent. 
      statementlist.statements.add(statement);
      statementlist.onerrorAccu = null; statementlist.withoutOnerror.add(statement);
      return statement.statementlist;  //The else sub statementlist.
    }
    
    public void add_elseBlock(StatementList val){}

    
  }
  
  
  
  
  
  
  public static class CondStatement extends JZcmditem
  {
    
    public JZcmditem condition;

    //DataAccess conditionValue;
    
    CondStatement(StatementList parentList, char type){
      super(parentList, type);
    }

    /**From Zbnf: < condition>. A condition is an expression. It is the same like {@link #new_numExpr()}
     */
    public JZcmdCalculatorExpr new_condition(){  
      condition = new JZcmditem(statementlist, '.');
      return condition.new_numExpr();
    }
    
    public void add_condition(JZcmdCalculatorExpr val){ 
      condition.add_numExpr(val);
    }
    
  };
  
  
  
  public static class IfCondition extends CondStatement
  {
    
    public boolean bElse;
    
    IfCondition(StatementList parentList, char whatis){
      super(parentList, whatis);
    }
    
  }






  public static class ForStatement extends CondStatement
  {
    
    String forVariable, checkForVariable;
    
    
    JZcmdDataAccess forContainer;
    
    ForStatement(StatementList parentList, char type){
      super(parentList, type);
    }
    
    
    public void set_forVariable(String name){ this.forVariable = name; }

    
    public void set_checkForVariable(String name){ this.checkForVariable = name; }
    
    public JZcmdDataAccess new_forContainer() { 
      return new JZcmdDataAccess(); 
    }
    
    public void add_forContainer(JZcmdDataAccess val){ 
      forContainer = val;
    }
    


    
  };
  
  
  
  
  public static class Subroutine extends JZcmditem
  {
    public String name;
    
    /**If set the subroutine gets all local variables as copy. */
    public boolean addLocals;
    
    /**If set the subroutine uses the local variables of the calling level immediately. It does not use an extra calling level. 
     * The the subroutine has no formalArgs. */
    public boolean useLocals;
    
    public List<DefVariable> formalArgs;
    
    final JZtxtcmdScript theScript;
    
    /**
     * @param parentList It is neccessary that parentList is instanceof JZcmdClass, for {@link JZtxtcmdExecuter}
     */
    Subroutine(JZcmdClass parentList) {
      super(parentList, 'X');
      this.theScript = parentList.theScript;
    }
    
    public void set_name(String name){ this.name = name; }

    public void set_useLocals(){ useLocals = true; }

    public void set_addLocals(){ addLocals = true; }
    
    public Subroutine new_formalArgument(){ return this; } //new Argument(parentList); }
    
    /**Set from ZBNF:  \<*subtext:name: { <namedArgument> ?,} \> */
    public void add_formalArgument(Subroutine val){}
    
    public DefVariable new_DefObjVar(){
      return new DefVariable(parentList, 'O'); 
    }
    
    public void add_DefObjVar(DefVariable val) {
      if(formalArgs == null){ formalArgs = new ArrayList<DefVariable>(); }
      formalArgs.add(val);
    }

    /**Defines a variable which is able to use as container.
     */
    public DefContainerVariable new_List(){ 
      return new DefContainerVariable(parentList, 'L'); 
    } 

    public void add_List(DefContainerVariable val){ 
      if(formalArgs == null){ formalArgs = new ArrayList<DefVariable>(); }
      formalArgs.add(val);
    }
    
    public DefVariable new_ClassObjVar(){
      return new DefVariable(parentList, 'C'); 
    }
    
    public void add_DefClassVar(DefVariable val) {
      if(formalArgs == null){ formalArgs = new ArrayList<DefVariable>(); }
      formalArgs.add(val);
    }

    public DefVariable new_DefNumVar(){
      return new DefVariable(parentList, 'K'); 
    }
    
    public void add_DefNumVar(DefVariable val) {
      if(formalArgs == null){ formalArgs = new ArrayList<DefVariable>(); }
      formalArgs.add(val);
    }

    
    public DefVariable new_DefBoolVar(){
      return new DefVariable(parentList, 'Q'); 
    }
    
    public void add_DefBoolVar(DefVariable val) {
      if(formalArgs == null){ formalArgs = new ArrayList<DefVariable>(); }
      formalArgs.add(val);
    }

    
    public DefVariable new_textVariable(){
      return new DefVariable(parentList, 'S'); 
    }
    
    public void add_textVariable(DefVariable val) {
      if(formalArgs == null){ formalArgs = new ArrayList<DefVariable>(); }
      formalArgs.add(val);
    }

    
    public DefVariable new_DefMapVar(){
      return new DefVariable(parentList, 'M'); 
    }
    
    public void add_DefMapVar(DefVariable val) {
      if(formalArgs == null){ formalArgs = new ArrayList<DefVariable>(); }
      formalArgs.add(val);
    }

    
    public DefVariable new_DefFilepath(){ return new DefVariable(this.parentList, 'F'); } 

    public void add_DefFilepath(DefVariable val){ 
      if(formalArgs == null){ formalArgs = new ArrayList<DefVariable>(); }
      formalArgs.add(val);
    }
    
    public UserFileset new_DefFileset(){
      return new UserFileset(this.parentList); 
    } 

    public void add_DefFileset(UserFileset val){ 
      if(formalArgs == null){ formalArgs = new ArrayList<DefVariable>(); }
      formalArgs.add(val);
    }
    
    


    
    /**Defines or changes an environment variable with value. set NAME = TEXT;
     * Handle in the same kind like a String variable
     */
    public DefVariable new_setEnvVar(){ 
      return new DefVariable(parentList, 'S'); 
    } 

    /**Defines or changes an environment variable with value. set NAME = TEXT;
     * Handle in the same kind like a String variable but appends a '$' to the first name.
     */
    public void add_setEnvVar(DefVariable val){ 
      if(formalArgs == null){ formalArgs = new ArrayList<DefVariable>(); }
      //change the first identifier to $name
      val.defVariable.datapath().get(0).setIdent("$" + val.defVariable.datapath().get(0).ident());
      //val.identArgJbat = "$" + val.identArgJbat;
      formalArgs.add(val); 
    } 

    
    
    
    @Override void writeStructAdd(int indent, Appendable out) throws IOException{
      if(formalArgs !=null){
        for(DefVariable item: formalArgs){
          item.writeStruct(indent+1, out);
        }
      }
      out.append(")\n");
    }
    
    
    @Override void writeStructLine(Appendable out) {
      super.writeStructLine(out);
      try{ 
        if(name==null){
          out.append(" main(");
        } else {
          out.append(" sub ").append(name).append("(");
        }
      }catch(IOException exc){ throw new RuntimeException(exc); }  //unexpected
    }

    
  };
  
  
  
  public static class CallStatement extends AssignExpr
  {
    
    JZcmditem call_Name;
    
    /**Argument list either actual or formal if this is a subtext call or subtext definition. 
     * Maybe null if the subtext has not argument. It is null if it is not a subtext call or definition. */
    public List<Argument> actualArgs;
    

    
    CallStatement(StatementList parentList, char elementType){
      super(parentList, elementType);
    }
    
    public JZcmditem new_callName(){ return call_Name = new Argument(parentList); }
    
    public void set_callName(JZcmditem val){}
    
    
    /**Set from ZBNF:  \<*subtext:name: { <namedArgument> ?,} \> */
    public Argument new_actualArgument(){ return new Argument(parentList); }
    
    /**Set from ZBNF:  \<*subtext:name: { <namedArgument> ?,} \> */
    public void add_actualArgument(Argument val){ 
      if(actualArgs == null){ actualArgs = new ArrayList<Argument>(); }
      actualArgs.add(val); }
    
    
    @Override void writeStructAdd(int indent, Appendable out) throws IOException{
      call_Name.writeStruct(0, out);
      if(actualArgs !=null){
        for(Argument item: actualArgs){
          item.writeStruct(indent+1, out);
        }
      }
      out.append("\n");
    }
    

  };
  
  
  
  
  /**class for a cmd execution (operation system cmd process invocation).
   * The assignExpr is used for stdout capturing.
   * @author hartmut
   *
   */
  public static class CmdInvoke extends AssignExpr
  {

    /**Argument list either actual or formal if this is a subtext call or subtext definition. 
     * Maybe null if the subtext has not argument. It is null if it is not a subtext call or definition. */
    public List<JZcmditem> cmdArgs;
    
    /**Any variable given by name or java instance  which is used to assign to it.
     * A variable is given by the start element of the data path. An instance is given by any more complex datapath
     * null if not used. */
    public List<DataAccess> errorPipes;
    
    
    /**The variable which should be created or to which a value is assigned to. */
    public DataAccess errorPipe;
    

    /**The variable which should be created or to which a value is assigned to. */
    public DataAccess inputPipe;
    
    boolean bCmdCheck;

    public boolean bShouldNotWait;

    CmdInvoke(StatementList parentList, char elementType)
    { super(parentList, elementType);
    }
    
    /**Set from ZBNF:  \<*subtext:name: { <namedArgument> ?,} \> */
    public JZcmditem new_actualArgument(){ return new JZcmditem(parentList, '.'); }
     
    /**Set from ZBNF:  \<*subtext:name: { <namedArgument> ?,} \> */
    public void add_actualArgument(JZcmditem val){ 
      if(cmdArgs == null){ cmdArgs = new ArrayList<JZcmditem>(); }
      cmdArgs.add(val); 
    }
    
    
    /**Set from ZBNF: */
    public JZcmdDataAccess new_argList(){ 
      JZcmditem statement = new JZcmditem(parentList, 'L');
      JZcmdDataAccess dataAccess1 = new JZcmdDataAccess(); 
      statement.dataAccess = dataAccess1; 
      if(cmdArgs == null){ cmdArgs = new ArrayList<JZcmditem>(); }
      cmdArgs.add(statement); 
      return dataAccess1;
    }
 
    

    
    public void add_argList(JZcmdDataAccess val){ 
    }
    
    
    
    public void set_argsCheck(){ bCmdCheck = true; }

  }
  
  
  
  public static class ExitStatement extends JZcmditem
  {
    
    int exitValue;
    
    ExitStatement(StatementList parentList, int exitValue){
      super(parentList, 'z');
      this.exitValue = exitValue;
    }
  };
  
  
  
  public static class ThreadBlock extends JZcmditem
  {
    
    DataAccess threadVariable;
    
    ThreadBlock(StatementList parentList){
      super(parentList, 'x');
    }

    
    /**From Zbnf: [{ Thread <dataAccess?defThreadVar> = }] 
     */
    public JZcmdDataAccess new_defThreadVar(){ 
      return new JZcmdDataAccess(); 
    }
    
    public void add_defThreadVar(JZcmdDataAccess val){ 
      val.setTypeToLastElement('T');
      threadVariable = val;
      //identArgJbat = "N";  //Marker for a new Variable.
    }

    
    /**From Zbnf: [{ Thread <dataAccess?assignThreadVar> = }] 
     */
    public JZcmdDataAccess new_assignThreadVar(){ 
      return new JZcmdDataAccess(); 
    }
    
    public void add_assignThreadVar(JZcmdDataAccess val){ 
      threadVariable = val;
    }

    


  
  
  }
  
  
  
  /**This class contains expressions for error handling.
   * The statements in this sub-ScriptElement were executed if an exception throws
   * or if a command line invocation returns an error level greater or equal the {@link Iferror#errorLevel}.
   * If it is null, no exception handling is done.
   * <br><br>
   * This block can contain any statements as error replacement. If they fails too,
   * the iferror-Block can contain an iferror too.
   * 
 */
  public final static class Onerror extends JZcmditem
  {
    /**From ZBNF. If not changed then it is not a cmd error type.*/
    public int errorLevel = Integer.MIN_VALUE;
    
    
    /**
     * <ul>
     * <li>'n' for notfound
     * <li>'f' file error
     * <li>'i' any internal exception.
     * <li>'?' any exception.
     * </ul>
     * 
     */
    public char errorType = '?';
    
    public void set_errortype(String type){
      errorType = type.charAt(0); //n, i, f
    }
 
    Onerror(StatementList parentList){
      super(parentList, '?');
    }
    
    /**Sets the statement to a cmd error execution.
     * See {@link JZtxtcmdExecuter.ExecuteLevel#execCmdError(Onerror)}.
     * This method is called in {@link StatementList#add_onerror(Onerror)}.
     */
    void setCmdError(){ elementType = '#'; }
    
 }
  
  
  
  
  
  /**Organization class for a list of script elements inside another Scriptelement.
   *
   * A statement, an element of the script, maybe a simple text, an condition etc.
   * A statement may have sub statements , see aggregation {@link JZcmditem#statementlist}. 
   * <br>
   * UML-Notation see {@link org.vishia.util.Docu_UML_simpleNotation}:
   * <pre>
   *  StatementList         JZcmditem
   *       |              !The statement           StatementList
   *       |                  |                    !Sub statements
   *       |----statements--*>|                         |
   *                          |-----statementlist------>|
   * </pre> 
   */
  public static class StatementList implements SetLineColumn_ifc
  {
    //JZcmditem currStatement;
    
    /**Hint to the source of this parsed argument or statement. */
    String srcFile = "srcFile-yet-unknown";
    
    /**For debug and error message, set by compiler. */
    int srcLine, srcColumn;
    
    /**Only used for debug, to see which is the parent. */
    final JZcmditem parentStatement;
    
    public String cmpnName;
    
    public final List<JZcmditem> statements = new ArrayList<JZcmditem>();
    

    /**List of currently onerror statements.
     * This list is referenced in the appropriate {@link XXXXXXStatement#onerror} too. 
     * If an onerror statement will be gotten next, it is added to this list using this reference.
     * If another statement will be gotten next, this reference is cleared. So a new list will be created
     * for a later getting onerror statement. 
     */
    List<Onerror> onerrorAccu;

    
    List<JZcmditem> withoutOnerror = new LinkedList<JZcmditem>();
    
    
    /**True if the block {@link Argument#statementlist} contains at least one variable definition.
     * In this case the execution of the ScriptElement as a block should be done with an separated set
     * of variables because new variables should not merge between existing of the outer block.
     */
    public boolean bContainsVariableDef;

    /**Set with <code><:s></code> in a textExpression to enforce skipping white spaces. 
     * Also true if a text expression starts with &lt;::>, then the first line feed should not be generated 
     * if the text before is empty. 
     * It is valid for the first statements till a statement with content is found. 
     * Then this variable is set to false.
     */
    boolean bSetSkipSpaces;
    

    //int indentText;
    
    //int indentTextOut;
    
    final JZscriptSettings jzSettings;
    
    /**Number of indent whitespace characters which should be skipped for an text expression.
     * If not set, it is -1 to designate, not set.
     */
    int nIndentInScript = 0;
    
    /**indent characters which should be skipped for an text expression.*/
    String sIndentChars = "=+:";
    
    /**Scripts for some local variable. This scripts where executed with current data on start of processing this genContent.
     * The generator stores the results in a Map<String, String> localVariable. 
     * 
     */
    //private final List<ScriptElement> localVariableScripts = new ArrayList<ScriptElement>();
    
    public final List<StatementList> addToList = new ArrayList<StatementList>();
    
    //public List<String> datapath = new ArrayList<String>();
    
    public StatementList(JZscriptSettings jzSettings)
    { this.parentStatement = null;
      this.jzSettings = jzSettings;
      //this.isContentForInput = false;
    }
        
    public StatementList(JZcmditem parentStatement)
    { this.parentStatement = parentStatement;
      if(parentStatement == null || parentStatement.parentList == null){
        this.jzSettings = null;
      } else {
        this.jzSettings = parentStatement.parentList.jzSettings;
        this.nIndentInScript = parentStatement.parentList.nIndentInScript;  //default value, can be changed in expression.
        this.sIndentChars = parentStatement.parentList.sIndentChars;
      }
    }
        
    public JZcmditem new_createTextOut(){
      JZcmditem statement = new JZcmditem(this, 'o');
      statements.add(statement);
      onerrorAccu = null; withoutOnerror.add(statement);
      return statement;
    }
    
    
    public void add_createTextOut(JZcmditem val){}
    
    

    public JZcmditem new_appendTextOut(){
      JZcmditem statement = new JZcmditem(this, 'q');
      statements.add(statement);
      onerrorAccu = null; withoutOnerror.add(statement);
      return statement;
    }
    
    
    public void add_appendTextOut(JZcmditem val){}
    
    

    
    @Override public void setLineColumnFile(int line, int column, String sFile){
      srcLine = line; srcColumn = column; srcFile = sFile; 
    }

    /**Returns wheter only the line or only the column should be set.
     * It can save calculation time if one of the components are not necessary.
     * @return 'c' if only column, 'l' if only line, any other: set both.
     */
    @Override public int setLineColumnFileMode(){ return mLine | mColumn | mFile; }


    
    public StatementList new_statementBlock(){
      this.bSetSkipSpaces = false;
      JZcmditem statement = new JZcmditem(this, 'B');
      statements.add(statement);
      onerrorAccu = null; withoutOnerror.add(statement);
      return statement.statementlist = new StatementList(statement);
    }
    
    public void add_statementBlock(StatementList val){}

    

    
    
    /**Defines or changes an environment variable with value. set NAME = TEXT;
     * Handle in the same kind like a String variable
     */
    public JZcmditem new_debugOp(){
      return new JZcmditem(this, 'H'); 
    } 

    /**Defines or changes an environment variable with value. set NAME = TEXT;
     * Handle in the same kind like a String variable but appends a '$' to the first name.
     */
    public void add_debugOp(JZcmditem val){ 
      statements.add(val); 
      onerrorAccu = null; withoutOnerror.add(val);
    } 
    
    /**Defines or changes an environment variable with value. set NAME = TEXT;
     * Handle in the same kind like a String variable
     */
    public JZcmditem new_debug(){
      return new JZcmditem(this, 'D'); 
    } 

    /**Defines or changes an environment variable with value. set NAME = TEXT;
     * Handle in the same kind like a String variable but appends a '$' to the first name.
     */
    public void add_debug(JZcmditem val){ 
      statements.add(val); 
      onerrorAccu = null; withoutOnerror.add(val);
    } 
    
    
    public void set_debug(){
      statements.add(new JZcmditem(this, 'D'));
    }
    
    public void set_scriptdir(){
      this.bSetSkipSpaces = false;
      JZcmditem textOut = new TextOut(this, 't');
      int posscriptdir = srcFile.lastIndexOf('/');
      textOut.textArg = srcFile.substring(0, posscriptdir);
      statements.add(textOut);
    }
    
    
    /**Gathers a text which is assigned to any variable or output. <+ name>text<.+>
     */
    public TextOut new_textOut(){ 
      //indentTextOut = indentText;
      return new TextOut(this, 'T'); 
    }

    public void add_textOut(TextOut val){ 
      statements.add(val); 
    } 
    
    
    public void set_nIndent(long value){ nIndentInScript= (int)value; }
    
    public void set_cIndent(String value){ sIndentChars= value; }  //it has only one character.
    
    
    
    
    
    /**Gathers statements to build a text which is assigned to the out-instance.
     * It is a sub statement list wrapped by a JZcmdItem as text expr in the current statements.
     * See also {@link JZcmditem#new_textExpr(ZbnfParseResultItem)}: It is invoked here as sub statement list
     * for this <code>new JZcmditem(this, ':');</code>. The difference is less, both have the same effect. 
     * This StatementList contains some sub statements as text expression with this item. 
     * The text is assembled and outputted to the standard output. 
     */
    public StatementList new_textExpr(ZbnfParseResultItem zbnfItem){ 
      this.bSetSkipSpaces = false;  //nested textExpr switches off leading space skipping. Depending check from nesting text is too complex.
      if(zbnfItem.syntaxItem().bDebugParsing){
        Debugutil.stop();
      }
      JZcmditem textExpr = new JZcmditem(this, ':');
      this.statements.add(textExpr);                         // add to the superior statements
      
      textExpr.statementlist = new StatementList(textExpr);  // the statementList of this textExpr as sub statements
      return textExpr.new_textExpr(zbnfItem);                // the statementlist is a textExpr. Use the same as for subtext.
    }

    public void add_textExpr(StatementList val){ } 
    
    /**Gathers a text which is assigned to the out-instance
     */
    public StatementList new_textExprTEST(ZbnfParseResultItem zbnfItem){ 
      JZcmditem textExpr = new JZcmditem(this, ':');
      statements.add(textExpr);
      textExpr.statementlist = new StatementList(textExpr);
      return textExpr.statementlist;
    }

    public void add_textExprTEST(StatementList val){ } 
    
    /**Defines a variable with initial value. <= <variableAssign?textVariable> \<\.=\>
     */
    public DefVariable new_textVariable(){
      bContainsVariableDef = true; 
      return new DefVariable(this, 'S'); 
    } 

    public void add_textVariable(DefVariable val){ statements.add(val); onerrorAccu = null; withoutOnerror.add(val);} 
    
    
    /**Defines a variable which is able to use as pipe.
     */
    public DefVariable new_Pipe(){
      bContainsVariableDef = true; 
      return new DefVariable(this, 'P'); 
    } 

    public void add_Pipe(DefVariable val){ statements.add(val); onerrorAccu = null; withoutOnerror.add(val); }
    
    /**Defines a variable which is able to use as String buffer.
     */
    public DefVariable new_Stringjar(){
      bContainsVariableDef = true; 
      return new DefVariable(this, 'U'); 
    } 

    public void add_Stringjar(DefVariable val){ statements.add(val);  onerrorAccu = null; withoutOnerror.add(val);}
    
        
    /**Defines a variable which is able to use as container.
     */
    public DefContainerVariable new_List(){ 
      bContainsVariableDef = true; 
      return new DefContainerVariable(this, 'L'); 
    } 

    public void add_List(DefContainerVariable val){ statements.add(val);  onerrorAccu = null; withoutOnerror.add(val);}
    
    /**Defines a variable which is able to use as container.
     */
    public DefVariable new_DefMapVar(){ 
      bContainsVariableDef = true; 
      DefVariable statement = new DefVariable(this, 'M'); 
      statements.add(statement);  onerrorAccu = null; withoutOnerror.add(statement);
      return statement;
    } 

    public void add_DefMapVar(DefVariable val){ }
    
    /**Defines a variable which is able to use as Appendable, it is a Writer.
     */
    public DefVariable new_Openfile(){
      bContainsVariableDef = true; 
      return new DefVariable(this, 'W'); 
    } 

    public void add_Openfile(DefVariable val){ 
      statements.add(val);  
      onerrorAccu = null; 
      withoutOnerror.add(val);
    }
    
    public DefVariable new_DefFilepath(){
      bContainsVariableDef = true; 
      return new DefVariable(this, 'F'); 
    } 

    public void add_DefFilepath(DefVariable val){ 
      statements.add(val);  
      onerrorAccu = null; 
      withoutOnerror.add(val);
    }
    
    
    public UserFileset new_DefFileset(){
      bContainsVariableDef = true; 
      return new UserFileset(this); 
    } 

    public void add_DefFileset(UserFileset val){ 
      statements.add(val);  
      onerrorAccu = null; 
      withoutOnerror.add(val);
    }
    
    
    public Zmake new_zmake(){
      bContainsVariableDef = true; 
      return new Zmake(this); 
    }
    
    public void add_zmake(Zmake val){ 
      statements.add(val);  
      onerrorAccu = null; 
      withoutOnerror.add(val);
    }
    
    
    /**Defines a variable with initial value. <= <$name> : <obj>> \<\.=\>
     */
    public DefVariable new_DefObjVar(){ 
      bContainsVariableDef = true; 
      return new DefVariable(this, 'O'); 
    } 

    public void add_DefObjVar(DefVariable val){ statements.add(val); onerrorAccu = null; withoutOnerror.add(val);}
    
    public DefVariable XXXnew_DefSubtext(){
      return new DefVariable(this, 'C');  
    } 

    public void XXXadd_DefSubtext(DefVariable val){ 
      if(val.statementlist !=null) {
        val.elementType = '{';  //a code block, statement block which should be executed on used time.
      } else {
        val.elementType = 'O';  //an expression which should be evaluated on build-time of the container
      }
      statements.add(val);  
    }
    
    
    public Subroutine new_DefSubtext(){
      StatementList parent = this;
      while(parent !=null && !(parent instanceof JZcmdClass)){
        parent = parent.parentStatement.parentList;
      }
      JZcmdClass jzClass = parent == null ? null : (JZcmdClass)parent;
      return new Subroutine(jzClass);  
    } 

    public void add_DefSubtext(Subroutine val){ 
      if(val.statementlist !=null) {
        val.elementType = '{';  //a code block, statement block which should be executed on used time.
      } else {
        val.elementType = 'O';  //an expression which should be evaluated on build-time of the container
      }
      statements.add(val);  
    }
    
    

    
    public DefClasspathVariable new_DefClasspath(){ 
      bContainsVariableDef = true; 
      return new DefClasspathVariable(this); 
    } 

    public void add_DefClasspath(DefClasspathVariable val){ statements.add(val); onerrorAccu = null; withoutOnerror.add(val);}
    
    
    public DefClassVariable new_DefClassVar(){ 
      bContainsVariableDef = true; 
      return new DefClassVariable(this); 
    } 

    public void add_DefClassVar(DefClassVariable val){ statements.add(val); onerrorAccu = null; withoutOnerror.add(val);}
    
    
    public DefVariable new_DefNumVar(){ 
      bContainsVariableDef = true; 
      return new DefVariable(this, 'K'); 
    } 

    public void add_DefNumVar(DefVariable val){ statements.add(val); onerrorAccu = null; withoutOnerror.add(val);}
    
    
    public DefVariable new_DefBoolVar(){ 
      bContainsVariableDef = true; 
      return new DefVariable(this, 'Q'); 
    } 

    public void add_DefBoolVar(DefVariable val){ statements.add(val); onerrorAccu = null; withoutOnerror.add(val);}
    
    

    /**Defines or changes an environment variable with value. set NAME = TEXT;
     * Handle in the same kind like a String variable
     */
    public DefVariable new_setEnvVar(){ 
      return new DefVariable(this, 'S'); 
    } 

    /**Defines or changes an environment variable with value. set NAME = TEXT;
     * Handle in the same kind like a String variable but appends a '$' to the first name.
     */
    public void add_setEnvVar(DefVariable val){ 
      //change the last identifier to $name
      List<DataAccess.DatapathElement> datapath = val.defVariable.datapath();
      int ix = datapath.size()-1;  //usual 0 if 1 element for simple set
      DataAccess.DatapathElement lastElement = datapath.get(ix);
      lastElement.setIdent("$" + lastElement.ident());
      statements.add(val); 
      onerrorAccu = null; withoutOnerror.add(val);
    } 
    

    //public List<ScriptElement> getLocalVariables(){ return localVariableScripts; }
    
    /**Set from ZBNF:  (\?*<$?dataText>\?) */
    public DataText new_dataText(){ 
      this.bSetSkipSpaces = false;     //spaces after this item are not skipped.
      return new DataText(this); 
    }
    
    /**Set from ZBNF:  (\?*<*dataText>\?) */
    public void add_dataText(DataText val){ 
      statements.add(val);
      onerrorAccu = null; withoutOnerror.add(val);
    }
    
    /**Sets the textReplLf From ZBNF. 
     * Inside a <code>textExpr::=...<*|\<:|\<+|\<=|\<*|\<\.?textReplLf></code>.
     * The text is written in the source file, but the line feed character sequence may be another
     * int the generated text. Additional a left indent can be removed.
     * @param text
     */
    public void set_textReplLf(String text){
    }
    
    
    
    public TextColumn new_setColumn(){
      return new TextColumn(this);
    }
    
    
    public void add_setColumn(TextColumn val){ 
      statements.add(val);
    }
    
    
    /**Sets the plainText From ZBNF. invokes {@link #set_textReplLf(String)} if the text contains
     * other characters as white spaces. 
     * It regards {@link #bSetSkipSpaces}.
     */
    public void set_plainText(String text){
      int zText = text.length();
      CharSequence text1 = text;
      if(zText >0){
        if(srcLine == 24)
          Debugutil.stop();
        //if(parentStatement instanceof TextOut) {
          //TextOut textOut = (TextOut)parentStatement;
        if(jzSettings !=null) {
          final String sIndentChars1;
          final int nIndentScript1;
          sIndentChars1 = this.sIndentChars;
          nIndentScript1 = this.nIndentInScript;
          text1 = StringFunctions_B.removeIndentReplaceNewline(text
              , nIndentScript1, sIndentChars1, jzSettings.srcTabsize, jzSettings.sLinefeed, this.bSetSkipSpaces);
          zText = text1.length();                          // used text length
          if( zText>0) {  //it is 0 if all spaces are skipped
            this.bSetSkipSpaces = false;  //it is valid for the following text only, it is used.
          }
        }
      }
      if(zText >0) {                             // don't add a not given text
        JZcmditem statement = new JZcmditem(this, 't');
        statement.textArg = text1.toString();
        statements.add(statement);
        onerrorAccu = null; withoutOnerror.add(statement);
      }
    }
    
    
    public void set_transliteration(String val){
      this.bSetSkipSpaces = false;
      char cc = val.charAt(0);
      if(cc != 'x') {  //the <:x> means a non existing char, can break a white space sequence
        JZcmditem statement = new JZcmditem(this, '\\');
        switch(cc){
          case 'n': statement.textArg = "\n"; break;
          case 'r': statement.textArg = "\r"; break;
          case 't': statement.textArg = "\t"; break;
          case 'b': statement.textArg = "\b"; break;
          case '<': case '\"': case '#': statement.textArg = val; break;
        }
        statements.add(statement);
        onerrorAccu = null; withoutOnerror.add(statement);
      }
    }
    
    public void set_utf16code(long val){
      this.bSetSkipSpaces = false;
      JZcmditem statement = new JZcmditem(this, 't');
      statement.textArg = "" + (char)val; //Character.toChars(val);
      statements.add(statement);
      onerrorAccu = null; withoutOnerror.add(statement);
    }
    
    public void set_newline(){
      this.bSetSkipSpaces = false;
      JZcmditem statement = new JZcmditem(this, 'n');
      statements.add(statement);
      onerrorAccu = null; withoutOnerror.add(statement);
    }
    
    
    public void set_skipWhiteSpaces(){
      bSetSkipSpaces = true;
    }
    
    
    public AssignExpr new_assignExpr(){ 
      return new AssignExpr(this, '='); 
    } 

    public void add_assignExpr(AssignExpr val){ 
      statements.add(val);  
      onerrorAccu = null; 
      withoutOnerror.add(val);
    }
    
    
    
    public JZcmditem new_throw(){ 
      return new JZcmditem(this, 'r'); 
    } 

    public void add_throw(JZcmditem val){ 
      statements.add(val);  
      onerrorAccu = null; 
      withoutOnerror.add(val);
    }
    
    public void set_throwonerror(int val){ 
      Onerror statement = new Onerror(this);
      statement.elementType = 'v';
      statement.errorLevel = val;
      statements.add(statement);
    } 

    
    public void set_errorToOutput(String val){
      JZcmditem statement = new JZcmditem(this, ',');
      if(val.equals("1")){ statement.textArg = val; }
      statements.add(statement);
    }
    
    
    public Onerror new_onerror(){
      return new Onerror(this);
    }
    

    public void add_onerror(Onerror val){
      if(val.errorLevel != Integer.MIN_VALUE){
        val.setCmdError();
      }
      statements.add(val);
      /*
      if(statementlist.onerrorAccu == null){ statementlist.onerrorAccu = new LinkedList<Onerror>(); }
      for( JZcmditem previousStatement: statementlist.withoutOnerror){
        previousStatement.onerror = onerror;  
        //use the same onerror list for all previous statements without error designation.
      }
      */
      withoutOnerror.clear();  //remove all entries, they are processed.
    }

    

    public Onerror new_iferrorlevel(){
      return new Onerror(this);
    }
    
    public void add_iferrorlevel(Onerror val){
      val.setCmdError();
      statements.add(val);
    }


    public void set_breakBlock(){ 
      JZcmditem statement = new JZcmditem(this, 'b');
      statements.add(statement);
    }
    
 

    
    
    public IfStatement new_ifCtrl ( ) {
      this.bSetSkipSpaces = false;
      StatementList subGenContent = new StatementList(parentStatement);
      IfStatement statement = new IfStatement(this, 'i');
      statement.statementlist = subGenContent;  //The statement contains a genContent. 
      return statement;

    }

    
    public void add_ifCtrl(IfStatement val){
      statements.add(val);
      onerrorAccu = null; withoutOnerror.add(val);
      
    }

    
    
    public StatementList new_hasNext()  
    { JZcmditem statement = new JZcmditem(this, 'N');
      statements.add(statement);
      onerrorAccu = null; withoutOnerror.add(statement);
      if(statement.statementlist == null){
        statement.statementlist = new StatementList(statement); 
      }
      return statement.statementlist;
    }
    
    public void add_hasNext(StatementList val){}
    
    

    
    

    /**for(name: iterable).
     * It builds a DefVariable, because it is similar. Variable is the for-variable.
     * @return 
     */
    public ForStatement new_forCtrl()
    { this.bSetSkipSpaces = false;
      ForStatement statement = new ForStatement(this, 'f');
      statements.add(statement);
      onerrorAccu = null; withoutOnerror.add(statement);
      return statement;
    }
    
    public void add_forCtrl(ForStatement val){}


    public CondStatement new_whileCtrl()
    { this.bSetSkipSpaces = false;
      CondStatement statement = new CondStatement(this, 'w');
      statements.add(statement);
      onerrorAccu = null; withoutOnerror.add(statement);
      return statement;
    }
    
    public void add_whileCtrl(CondStatement val){}


    public CondStatement new_dowhileCtrl()
    { this.bSetSkipSpaces = false;
      CondStatement statement = new CondStatement(this, 'u');
      statements.add(statement);
      onerrorAccu = null; withoutOnerror.add(statement);
      return statement;
    }
    
    public void add_dowhileCtrl(CondStatement val){}


    public ThreadBlock new_threadBlock()
    { ThreadBlock statement = new ThreadBlock(this);
      statements.add(statement);
      onerrorAccu = null; withoutOnerror.add(statement);
      return statement;
    }
    
    public void add_threadBlock(ThreadBlock val){}

    
    
    
    public CallStatement new_call()
    { this.bSetSkipSpaces = false;
      CallStatement statement = new CallStatement(this, 's');
      statements.add(statement);
      onerrorAccu = null; withoutOnerror.add(statement);
      return statement;
    }
    
    public void add_call(CallStatement val){}

    

    public CmdInvoke new_cmdWait()
    { CmdInvoke statement = new CmdInvoke(this, 'c');
      statements.add(statement);
      onerrorAccu = null; withoutOnerror.add(statement);
      return statement;
    }
    
    public void add_cmdWait(CmdInvoke val){}

    
    public CmdInvoke new_cmdStart()
    { CmdInvoke statement = new CmdInvoke(this, 'c');
      statement.bShouldNotWait = true;
      statements.add(statement);
      onerrorAccu = null; withoutOnerror.add(statement);
      return statement;
    }
    
    public void add_cmdStart(CmdInvoke val){}

    


    public FileOpArg new_move()
    { FileOpArg statement = new FileOpArg(this, 'm');
      statements.add(statement);
      onerrorAccu = null; withoutOnerror.add(statement);
      return statement;
    }
    
    public void add_move(FileOpArg val){}


    public FileOpArg new_copy()
    { FileOpArg statement = new FileOpArg(this, 'y');
      statements.add(statement);
      onerrorAccu = null; withoutOnerror.add(statement);
      return statement;
    }
    
    public void add_copy(FileOpArg val){}


    public FileOpArg new_del()
    { FileOpArg statement = new FileOpArg(this, 'l');
      statements.add(statement);
      onerrorAccu = null; withoutOnerror.add(statement);
      return statement;
    }
    
    public void add_del(FileOpArg val){}


    /*
    public void set_cd(String val)
    { JZcmditem statement = new JZcmditem(this, 'd');
      statement.textArg = StringSeq.create(val);
      statements.add(statement);
      onerrorAccu = null; withoutOnerror.add(statement);
    }
    */
    
    public JZcmditem new_cd(){
      JZcmditem statement = new JZcmditem(this, 'd');
      statements.add(statement);
      onerrorAccu = null; withoutOnerror.add(statement);
      return statement;
    }
    
    
    public void add_cd(JZcmditem val){}
    
    
    public JZcmditem new_mkdir(){
      JZcmditem statement = new JZcmditem(this, '9');
      statements.add(statement);
      onerrorAccu = null; withoutOnerror.add(statement);
      return statement;
    }
    
    
    public void add_mkdir(JZcmditem val){}
    
    
    
    public void set_name(String name){
      cmpnName = name;
    }
    
    
    public void set_exitScript(int val){
      ExitStatement statement = new ExitStatement(this, val);
      statements.add(statement);
    }  
    
  }
  

  
  
  /**A class in the JZcmd syntax.
   * The class can contain statements, which are variable definitions of the class variable. 
   * Therefore this class extends the StatementList.
   */
  public static class JZcmdClass extends StatementList
  {
    /**Sub classes of this class. */
    List<JZcmdClass> classes;
    
    /**All subroutines of this class. */
    final Map<String, JZtxtcmdScript.Subroutine> subroutines = new TreeMap<String, JZtxtcmdScript.Subroutine>();
    
    /**All classes and subroutines in order of the script to present it in a list for choice. */
    final List<Object> listClassesAndSubroutines = new ArrayList<Object>();
   
    final JZtxtcmdScript theScript;
    
    protected JZcmdClass(JZtxtcmdScript theScript, JZscriptSettings jzScriptSettings){
      super(/*JZcmdScript.this.*/jzScriptSettings);
      this.theScript = theScript;
    }
    
    
    public final List<JZcmdClass> classes(){ return classes; }
    
    public final Map<String, JZtxtcmdScript.Subroutine> subroutines(){ return subroutines; }
    
    public final List<Object> listClassesAndSubroutines(){ return listClassesAndSubroutines; }
    
    public JZcmdClass new_subClass(){ return new JZcmdClass(theScript, theScript.jzScriptSettings); }
    
    public void add_subClass(JZcmdClass val){ 
      if(classes == null){ classes = new ArrayList<JZcmdClass>(); }
      classes.add(val); 
      listClassesAndSubroutines.add(val);
      String sName = val.cmpnName;
      String nameGlobal = cmpnName == null ? sName : cmpnName + "." + sName;
      theScript.classesAll.put(nameGlobal, val); 
   
    }
    
    public Subroutine new_subroutine(){ return new Subroutine(this); }
    
    public void add_subroutine(Subroutine val){ 
      if(val.name == null){
        val.name = "main";
      }
      String sName = val.name.toString();
      subroutines.put(sName, val); 
      listClassesAndSubroutines.add(val);
      String nameGlobal = cmpnName == null ? sName : cmpnName + "." + sName;
      theScript.subroutinesAll.put(nameGlobal, val); 
    }
    
    
    
    public void writeStruct(int indent, Appendable out) throws IOException{
      
      if(statements !=null){
        for(JZcmditem item: statements){
          item.writeStruct(indent+1, out);
        }
      }
      for(Map.Entry<String, Subroutine> entry: subroutines.entrySet()){
        Subroutine sub = entry.getValue();
        sub.writeStruct(0, out);
      }
      //for(Map.Entry<String, JZcmdClass> entry: classes.entrySet()){
      if(classes !=null){
        for(JZcmdClass class1: classes){
          class1.writeStruct(indent+1, out);
        }
      }
    }

    
    
  }
  
  
  public final static class JZcmdInclude extends JZcmditem
  {
    JZcmdInclude(StatementList parentList)
    {
      super(parentList, '.');
    }
    public String path;
    /**If given name of an environment variable for the path. */
    public String envVar;
    
    /**True then &path.to.java.operation given as path.*/
    public boolean bJop;
    
    public int parentLevel;
    
  }
  
  
  
  /**Main class for ZBNF parse result.
   * This class has the enclosing class to store {@link ZbatchGenScript#subroutinesAll}, {@link ZbatchGenScript#listScriptVariables}
   * etc. while parsing the script. The <code><:file>...<.file></code>-script is stored here locally
   * and used as the main file script only if it is the first one of main or included script. The same behaviour is used  
   * <pre>
   * ZmakeGenctrl::= { <target> } \e.
   * </pre>
   */
  public final static class ZbnfJZcmdScript extends JZcmdClass
  {

    private final JZtxtcmdScript compiledScript;
    
    public Scriptfile scriptfile;    
    
    public ZbnfJZcmdScript(JZtxtcmdScript compiledScript){
      super(compiledScript, compiledScript.jzScriptSettings);   //JZcmdClass is non-static, enclosing is outer.
      this.compiledScript = compiledScript;
      compiledScript.scriptClass = this; //outer.new JZcmdClass();
    }
    
    
    public JZcmdInclude new_include(){ return new JZcmdInclude(this); }
    
    public void add_include(JZcmdInclude val){
      if(scriptfile.includes ==null){ scriptfile.includes = new ArrayList<JZcmdInclude>(); }
      scriptfile.includes.add(val); 
    }
    
    
    
    /**Any script file gets its own mainRoutine because the {@link #scriptfile} is one instance per parsed script file.
     * The lastly valid {@link JZtxtcmdScript#scriptFile} is set from the last processes file.
     * @return
     */
    public StatementList new_mainRoutine(){ 
      scriptfile.mainRoutine = new Subroutine(compiledScript.scriptClass); 
      scriptfile.mainRoutine.statementlist = new StatementList(compiledScript.jzScriptSettings);
      scriptfile.mainRoutine.srcFile = this.srcFile;
      return scriptfile.mainRoutine.statementlist;
    }
    
    public void add_mainRoutine(StatementList val){  }
    
    
    
    public JZcmditem new_checkJZcmdFile(){ return new JZcmditem(this, '\0'); } 

    public void add_checkJZcmdFile(JZcmditem val){ compiledScript.checkJZcmdFile = val; }

    
    public JZcmditem new_checkXmlFile(){ return new JZcmditem(this, '\0'); } 

    public void add_checkXmlFile(JZcmditem val){ compiledScript.checkJZcmdXmlFile = val; }

    
    public void setMainRoutine(Subroutine mainRoutine){
      compiledScript.mainRoutine = mainRoutine;
    }
    
    public boolean isXmlSrcNecessary(){ return compiledScript.checkJZcmdXmlFile !=null; }

    public void setXmlSrc(XmlNode xmlSrc){
    	compiledScript.xmlSrc = xmlSrc;	
    }
    
    
  }
  

  
  /**For one scriptfile, on include use extra instance per include.
   */
  public static class Scriptfile {
    public List<JZcmdInclude> includes;
    
    /**The script element for the whole file of this script. 
     * It is possible that it is from a included script.
     * It shall contain calling of <code><*subtext:name:...></code> 
     */
    Subroutine mainRoutine;
    
    /**Returns the main routine which may be parsed in this maybe included script. */
    public Subroutine getMainRoutine(){ return mainRoutine; }
    
    
    
  }




}
