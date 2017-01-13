package org.vishia.zcmd;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;

import org.vishia.cmd.CmdExecuter;
import org.vishia.cmd.CmdStore;
import org.vishia.cmd.JZcmdEngine;
import org.vishia.cmd.JZcmdExecuter;
import org.vishia.cmd.JZcmdScript;
import org.vishia.mainCmd.MainCmd;
import org.vishia.mainCmd.MainCmdLoggingStream;
import org.vishia.mainCmd.MainCmdLogging_ifc;
import org.vishia.util.DataAccess;
import org.vishia.util.FileSystem;
import org.vishia.util.StringPartScan;
import org.vishia.util.StringPartFromFileLines;
import org.vishia.xmlSimple.SimpleXmlOutputter;
import org.vishia.xmlSimple.XmlNode;
import org.vishia.zbnf.ZbnfJavaOutput;
import org.vishia.zbnf.ZbnfParser;


/**This is the Script executer and text generator started form command line.
 * The simplest form is:
 * <pre>
 * java path/to/zbnf.jar org.vishia.zcmd.JZcmd path/to/scriptFile
 * </pre>
 * This class contains the translator which uses the ZBNF parser. The core executer is {@link JZcmdExecuter}.
 * The translated script is stored in an instance of {@link JZcmdScript} which are both parts of the component
 * <code>srcJava_vishiaBase</code>.  
 * <br>
 * <b>Execution from command line or from Java with String[]-args:</b>
 * <br><br>
 * {@link #main(String[])} is the command line start. Arguments:
 * {@link #smain(String[])} is the same as {@link #main(String[])} but without exit the VM. 
 * It can be used to invoke main from inside another Java program. 
 * <br>
 * <br>
 * <b>Execution in a java context</b>
 * <br><br>
 * There are some differenz possibilities. The motivation to execute a part of a algorithm in a JZcmd script instead in Java pure may be:
 * <ul>
 * <li>Text generation: The text is better able to write in the script form. 
 * <li>Flexibility in application: It does not need to write or change Java source code for any changes
 * <li>It is possible to create data in the script and use it in Java after invocation.
 * <li>It is possible to output data in the script which are prepared in java before, especially for code generation.
 * <li>It is possible to prepare and complete data in the script.
 * </ul>
 * The translation of the script without immediate execution can be done calling one of the static routine. 
 * <ul><li>{@link #translateAndSetGenCtrl(File, MainCmdLogging_ifc)}: simplest form with a file as input
 * <li>{@link #translateAndSetGenCtrl(String, MainCmdLogging_ifc)}: The script is given as String
 * <li>{@link #translateAndSetGenCtrl(StringPartScan, MainCmdLogging_ifc, File, File)}: This is the core routine.
 * </ul>
 * To execute a compiled script see {@link JZcmdExecuter}.
 * <br><br>  
 * This class contains 'execute' routines only for textual given scripts, in different variants:
 * <ul>
 * <li>Simple execution with own JZcmdExecuter, without special data exchange. But inside the script static Java data can be used:
 *   <ul>
 *   <li>{@link #execute(String)}: for simple algorithms given as String, for example text preparation.
 *   <li>{@link #execute(File, MainCmdLogging_ifc)}: translate and execute a given script. 
 *   </ul>
 * <li>Execution with a given JZcmdExecuter instance:
 *   <ul>
 *   <li>{@link #execute(JZcmdExecuter, File, Appendable, String, boolean, File, MainCmdLogging_ifc)}: It reuses a given instance of JZcmdExecuter.
 *   </ul>
 * </ul>      
 * If the script is already compiled and stored in {@link JZcmdScript} and an execution environment {@link JZcmdExecuter} 
 * is established, one can invoke a subroutine of the script with 
 * {@link JZcmdExecuter#execSub(org.vishia.cmd.JZcmdScript.Subroutine, Map, boolean, Appendable, File)}
 * or run the main routine of the script with {@link JZcmdExecuter#execute(JZcmdScript, boolean, boolean, Appendable, String)}.
 * See there.
 * <br><br>
 * This class is only necessary if a script is still to compile. Note that from a compiled script the routine
 * {@link JZcmdScript#getEngine()}, defined in {@link CompiledScript#getEngine()} returns this instance
 * if the JZcmdScript was created with this.
 * <ul>
 * <li>{@link #execute(File, org.vishia.cmd.JZcmdExecuter.ExecuteLevel)} 
 *   compiles and executes a script given with file, whereby the given ExecuteLevel is used
 * <li>{@link #execSub(File, String, Map, org.vishia.cmd.JZcmdExecuter.ExecuteLevel)}
 * </ul>  
 * <br>
 * <br>
 * <b>Execution with the {@link ScriptEngine} interface</b>
 * <br>
 * That feature is not ready and not tested yet.
 * <br>
 * <br>
 * <b>Exceptionhandling</b>
 * <br>
 * On any exception outside main a {@link ScriptException} is thrown. That exception should be caught
 * by the caller.
 * The ScriptException contains 
 * <ul>
 * <li>an error text with the file, line and column if the error can be associated of a file-line,
 * <li>or an error text and the file name on file error, especially file not found.
 * <li>or an wrapped exception if any other usual unexpected exception was thrown.
 * </ul>
 * The main routine writes a line or more as one line in the {@link MainCmdLogging_ifc#writeError(String)}
 * if the exception has a file info. Unexpected Exceptions were written with 
 * {@link MainCmdLogging_ifc#writeError(String, Throwable)} with the unwrapped inner exception. That exceptions
 * should be shown in detail.
 * 
 * @author Hartmut Schorrig
 *
 */
public class JZcmd implements JZcmdEngine, Compilable
{
  
  /**Version, history and license.
   * <ul>
   * <li>2017-01-01 Hartmut chg adaption to {@link JZcmdExecuter}- 
   *   Now {@link #execute(JZcmdExecuter, File, Appendable, List, String, boolean, File, MainCmdLogging_ifc)}
   *   with List-given additional variables are available. Note that {@link JZcmdExecuter#setScriptVariable(String, char, Object, boolean)}
   *   is deprecated now.
   * <li>2017-01-01 Hartmut new {@link #readJZcmdCfg(org.vishia.cmd.JZcmdScript.AddSub2List, File, MainCmdLogging_ifc, CmdExecuter)} 
   * <li>2014-08-10 Hartmut bugfix: Now {@link #translateAndSetGenCtrl(File, File, MainCmdLogging_ifc)} : close() will be invoked.
   * <li>2014-08-10 Hartmut new: message "JZcmd - cannot create output text file" with the output file path.
   * <li>2014-08-10 Hartmut new: !checkXmlFile = filename; 
   * <li>2014-06-10 Hartmut chg: improved Exception handling of the script.
   * <li>2014-05-18 Hartmut new: try to implement javax.script interfaces, not ready yet
   * <li>2014-02-16 Hartmut new: {@link #jbatch(File, org.vishia.cmd.JZcmdExecuter.ExecuteLevel)} is deprecated now,
   *   instead {@link #execSub(File, String, Map, org.vishia.cmd.JZcmdExecuter.ExecuteLevel)} used.
   *   The difference: No scriptlevel created for the new compiled script, with given scriptlevel
   *   the subroutine is executed. 
   * <li>2014-02-16 Hartmut chg: execute(... String sCurrdir) now with current directory from outside.
   *   translateAndSetGenCtrl( File fileScript, ...) with the script file.
   *   Argument -currdir=PATH for command line invocation {@link #main(String[])}
   *   Build of script variable currdir, scriptfile, scriptdir with them in {@link JZcmdExecuter#initialize(JZcmdScript, boolean, Map, String, boolean)}.
   * <li>2013-10-27 Hartmut chg: {@link #jbatch(String, org.vishia.cmd.JZcmdExecuter.ExecuteLevel)}
   * <li>2012-10-03 created. Backgorund was the {@link org.vishia.zmake.Zmake} generator, but that is special for make problems.
   *   A generator which converts ZBNF-parsed data from an Java data context to output texts in several form, documenation, C-sources
   *   was need.
   * </ul>
   * 
   * <b>Copyright/Copyleft</b>:
   * For this source the LGPL Lesser General Public License,
   * published by the Free Software Foundation is valid.
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
  static final public String version = "2017-01-13";

  
  private static class Args{
    
    /**path to the script file for the generation or execution script of JZcmd.*/
    String sFileScript;
    
    /**path to the text output file which is generated by JZcmd. May be null, then no text output. */
    String sFileTextOut;
    
    /**path to some output files for debugging, maybe left null. */
    File fileTestXml;
    
    /**User arguments, stored as script variable "$1" till "$n" */
    List<String> userArgs = new ArrayList<String>();
  }
  

  
  /**The organization class for command line invocation.
   */
  private static class CmdLine extends MainCmd
  {
  
    public final Args argData;
  
    protected final MainCmd.Argument[] argList =
    { new MainCmd.Argument("", "INPUT    pathTo JZcmd-File to execute"
        , new MainCmd.SetArgument(){ @Override public boolean setArgument(String val){ 
          if(argData.sFileScript == null){ argData.sFileScript = val; }
          else {argData.userArgs.add(val); }
          return true; 
        }})
    , new MainCmd.Argument("-t", ":OUTEXT pathTo text-File for output"
        , new MainCmd.SetArgument(){ @Override public boolean setArgument(String val){ 
          argData.sFileTextOut = val; return true;
        }})
    , new MainCmd.Argument("-debug", ":INPUT.xml pathTo XML output of parsed script"
        , new MainCmd.SetArgument(){ @Override public boolean setArgument(String val){ 
          argData.fileTestXml = new File(val); 
          try { FileSystem.mkDirPath(argData.fileTestXml);
          } catch (FileNotFoundException e) {
            System.err.println("JZcmd.main - faulty path for -debug;" + val);
            return false;
          }
          return true;
        }})
    , new MainCmd.Argument("-u", ":userArgs"
        , new MainCmd.SetArgument(){ @Override public boolean setArgument(String val){ 
          argData.userArgs.add(val); 
          return true; 
        }})
    };
  
  
    protected CmdLine(Args argData, String[] sCmdlineArgs){
      super(sCmdlineArgs);
      this.argData = argData;
      super.addAboutInfo("Compilation and Execution of JZcmd-Files");
      super.addAboutInfo("made by HSchorrig, Version 1.0, 2013-07-11..2014-06-09");
      super.addHelpInfo("args JZcmd_SCRIPTFILE [-t:OUTEXT] [-debug:SCRIPTFILE.xml]");
      super.addArgument(argList);
      super.addHelpInfo("==Standard arguments of MainCmd==");
      super.addStandardHelpInfo();
      super.addHelpInfo("==Syntax of a JZcmd script==");
      super.addHelpInfo(JZcmdSyntax.syntax);

      
    }
    
    @Override protected void callWithoutArguments()
    { //overwrite with empty method - it is admissible.
    }
  
    
    @Override protected boolean checkArguments()
    {
      if(argData.sFileScript ==null) return false;
      else return true;
    } 
    
  
  }


  /**main started from java command line.
   * <pre>
INPUT          pathTo JZcmd-File to execute
-t:OUTEXT      pathTo text-File for output
-debug:TEST.xml pathTo XML output of parsed script
-u:userArgs
--help         show the help for command line and the syntax
--currdir=PATH Set the currdir variable.
   * </pre>
   */
  public static void main(String [] sArgs)
  { 
    try{ //for unexpected exceptions
      int exitlevel = smain(sArgs);
      System.exit(exitlevel);
    } catch(Exception exc) {
      //catch the last level of error. No error is reported direct on command line!
      Throwable exc1 = exc.getCause();
      if(exc1 == null){ exc1 = exc; }
      System.err.println("JZcmd.main() - uncaught ERROR; "); // + exc1.getMessage());
      exc1.printStackTrace(System.err);
      System.exit(MainCmdLogging_ifc.exitWithErrors);
    }
  }


  /**Invocation from another java program without exit the JVM
   * @param sArgs same like {@link #main(String[])}
   * @return the exit level 0 - successful 1..6 see {@link MainCmdLogging_ifc#exitWithArgumentError} etc.
   */
  public static int smain(String[] sArgs) throws ScriptException {
    String sError = null;
    Args args = new Args();
    CmdLine mainCmdLine = new CmdLine(args, sArgs); //the instance to parse arguments and others.
    mainCmdLine.setReportLevel(0);  //over-write if argument given. Don't use a report.txt by default.
    try{ mainCmdLine.parseArguments(); }
    catch(Exception exception)
    { sError = "JZcmd - Argument error ;" + exception.getMessage();
      mainCmdLine.report(sError, exception);
      mainCmdLine.setExitErrorLevel(MainCmdLogging_ifc.exitWithArgumentError);
    }
    if(args.sFileScript ==null){
      mainCmdLine.writeHelpInfo(null);
    } else if(sError ==null) {
      File fileIn = new File(args.sFileScript);
      int nrArg = 1;
      JZcmdExecuter executer = new JZcmdExecuter(mainCmdLine);
      Map<String, DataAccess.Variable<Object>> data = args.userArgs.size() >0 ? new TreeMap<String, DataAccess.Variable<Object>>() :null;
      try{
        for(String argu: args.userArgs){
          DataAccess.createOrReplaceVariable(data, "$" + nrArg, 'S', argu, true);  //User arguments as $
          nrArg +=1;
        }
      } catch(IllegalAccessException exc){ 
        throw new ScriptException("JZcmd.smain - cannot set user argument; " + nrArg);
      }
      FileWriter fout = null;
      Appendable out = null;
      if(args.sFileTextOut !=null){
        File fileOut = null;
        try{
          fileOut = new File(args.sFileTextOut);
          out = fout = new FileWriter(fileOut);
        } catch(IOException exc){
          sError = "JZcmd - cannot create output text file," + fileOut.getAbsolutePath(); 
        }
      } else {
        out = System.out;
      }
      if(sError == null){
        String sCurrdir = mainCmdLine.currdir();
        execute(executer, fileIn, out, data, sCurrdir, true, args.fileTestXml, mainCmdLine);
      }
      if(fout !=null){
        try { fout.close(); } catch(IOException exc){ throw new RuntimeException(exc); }
      }
      if(sError !=null){
        mainCmdLine.writeError(sError);
      }
    }
    return mainCmdLine.getExitErrorLevel();
  }


  
  
  /**Instantiates. The console output of the parser will be send to System.out.
   * It invokes {@link #JZcmd(MainCmdLogging_ifc)} with null as argument.
   * @throws ScriptException
   */
  public JZcmd() throws ScriptException{
    this(null);    
  }


  /**Instantiates with maybe given MainCmd logging.
   * @param log if null then the {@link MainCmdLoggingStream} is used with System.out.
   * @throws ScriptException
   */
  public JZcmd(MainCmdLogging_ifc log) throws ScriptException{
    if(log == null){
      this.log = new MainCmdLoggingStream(System.out);
    } else { this.log = log; }
    parserGenCtrl = new ZbnfParser(this.log); //console);
    try{ parserGenCtrl.setSyntax(JZcmdSyntax.syntax);
    } catch(ParseException exc){ throw new ScriptException("JZcmd.ctor - internal syntax error; " + exc.getMessage()); }
  }


  
  
  /**Parses the script and executes it in an own environment but with usage of the given local variables,
   * the current directory and the log of a given JZcmd ExecuterLevel.
   * That is the possibility to start a independent JZcmd execution in a JZcmd script itself.
   * @param script Path to the script.
   * @param execLevel subroutine level where this is called.
   *   All variables of this level are used as script variables for the script to execute.
   *   Especially the currdir of the level is used as script level currdir
   * @return The text which are created in the script using <:>text<.>
   */
  public static CharSequence execute(
      File script
    , JZcmdExecuter.ExecuteLevel execLevel
  ) throws IllegalAccessException{
    //return execute(script, execLevel.log());
    
    boolean bWaitForThreads = false;
    StringBuilder u = new StringBuilder();
    //JZcmdScript genScript = null; //gen.parseGenScript(fileGenCtrl, null);
    MainCmdLogging_ifc log = execLevel.log();
    JZcmdExecuter executer = new JZcmdExecuter(log);
    //Copy all local variables of the calling level as script variables.
    try { 
      JZcmdScript genScript = translateAndSetGenCtrl(script, null, log);
      //the script variables are build from the local ones of the calling script:
      executer.execute(genScript, true, bWaitForThreads, u, execLevel.localVariables, execLevel.currdir());
      //zgenExecuteLevel.execute(genScript.getMain().subContent, u, false);
    } catch (Throwable exc) {
      String sError = exc.getMessage();
      System.err.println(sError);
    }
    return u.toString();
  }
  
  
  
  /**Executes a sub routine in a special script, but uses a given execution environment.
   * The script level of the special script is not used. The script variables are taken from the existing level.
   * It means the script should only contain some subroutine codes.
   * This routine can be called inside another script with invocation:
   * <pre>
   * { ## any JZcmd script
   *     Map args;
   *     String args.name  = value;
   *     java org.vishia.zcmd.JZcmd.execSub(File:"path/JZcmdscript.jzcmd", "class.subroutine-Name", args, jzcmdsub);
   * }
   * </pre>
   * @param fileScript The file which contains the script
   * @param subroutine name of the subroutine in the script.
   * @param args Arguments for this subroutine.
   * @param execLevel Execution level where this routine from where it is called.
   * @return
   */
  public static CharSequence execSub(File fileScript, String subroutine
      , Map<String, DataAccess.Variable<Object>> args
      , JZcmdExecuter.ExecuteLevel execLevel)
  {
    
    MainCmdLogging_ifc log = execLevel.log();
    //boolean bWaitForThreads = false;
    //Copy all local variables of the calling level as script variables.
    try { 
      JZcmdScript jzscript = translateAndSetGenCtrl(fileScript, null, log);
      JZcmdScript.Subroutine substatement = jzscript.getSubroutine(subroutine);
      //the script variables are build from the local ones of the calling script:
      execLevel.exec_Subroutine(substatement, args, null, -1);
      //executer.execute(genScript, true, bWaitForThreads, null, null);
      //zgenExecuteLevel.execute(genScript.getMain().subContent, u, false);
    } catch (Exception exc) {
      String sError = exc.getMessage();
      System.err.println(sError);
    }
    
    return "";    
    
  }
  
  
  
  /**Executes a sub routine in a special script, but uses a given execution environment.
   * The script level of the special script is not used. The script variables are taken from the existing level.
   * It means the script should only contain some subroutine codes.
   * This routine can be called inside another script with invocation:
   * <pre>
   * { ## any JZcmd script
   *     Map args;
   *     String args.name  = value;
   *     java org.vishia.zcmd.JZcmd.execSub(File:"path/JZcmdscript.jzcmd", "class.subroutine-Name", args, jzcmdsub);
   * }
   * </pre>
   * @param fileScript The file which contains the script
   * @param subroutine name of the subroutine in the script.
   * @param args Arguments for this subroutine.
   * @param execLevel Execution level where this routine from where it is called.
   * @return
   */
  public Object evalSub(File fileScript, String subroutine
      , Map<String, DataAccess.Variable<Object>> args
      , JZcmdExecuter.ExecuteLevel execLevel)
  throws ScriptException
  {
    
    //boolean bWaitForThreads = false;
    //Copy all local variables of the calling level as script variables.
      
      
    JZcmdScript jzscript = compile(fileScript, null);
    JZcmdScript.Subroutine substatement = jzscript.getSubroutine(subroutine);
    //the script variables are build from the local ones of the calling script:
    return execLevel.evalSubroutine(substatement, args, null, -1);    
  }
  
  
  
  /**Executes a JZcmd script.
   * A log output using the <code>console</code> script variable is written to {@link System#out}.
   * 
   * @param script the script in ASCII-format, syntax see {@link JZcmdSyntax}
   * @return An error text or null.
   */
  public static void execute(String script) throws ScriptException {
    StringPartScan spScript = new StringPartScan(script);
    MainCmdLogging_ifc log = new MainCmdLoggingStream(System.out);
    JZcmdExecuter zgenExecuter = new JZcmdExecuter(log);
    execute(zgenExecuter, null, spScript, null, null, null, true, null, log);
  }
  
  
  /**Executes a JZcmd script.
   * @param script the script in ASCII-format, syntax see {@link JZcmdSyntax}
   * @param log A given log output
   * @return An error text or null.
   */
  public static void execute(File script, MainCmdLogging_ifc log)
  throws ScriptException 
  { execute(null, script, null, null, true, null, log);
  }
  
  
  
  
  /**Translates and executes a JZcmd script.
   * @param executer A given instance of the executer. 
   * @param fileScript the script in ASCII-format, syntax see {@link JZcmdSyntax}
   * @param out Output channel for <+text>...<.+>
   * @param sCurrdir The start value for currdir
   * @param accessPrivate if true then private data are accessed too. The accessing of private data may be helpfull
   *  for debugging. It is not recommended for general purpose! The access mechanism is given with 
   *  {@link java.lang.reflect.Field#setAccessible(boolean)}.
   * @param testOut if not null then outputs a data tree of the generate script.
   * @param log A given log output
   * @return The text which are created in the script using <:>text<.>
   * @throws ScriptException 
   */
  public static void execute(JZcmdExecuter executer, File fileScript, Appendable out, Map<String, DataAccess.Variable<Object>> data
      , String sCurrdir, boolean accessPrivate
      , File testOut, MainCmdLogging_ifc log) 
  throws ScriptException {
    int lengthBufferGenctrl = (int)fileScript.length();
    StringPartScan sourceScript = null;
    try { 
      sourceScript = new StringPartFromFileLines(fileScript, lengthBufferGenctrl, "encoding", null);
    } catch(IOException exc){
      String sError = exc.getMessage();
      throw new ScriptException("JZcmd - Error script file not found; " + fileScript.getAbsolutePath() + "; " + sError); 
    }
    if(sourceScript !=null){
      execute(executer, fileScript, sourceScript, out, data, sCurrdir, accessPrivate, testOut, log);
    }
  }  


  
  
  /**Translates and executes a JZcmd script.
   * @param executer A given instance of the executer. 
   * @param fileScript the script in ASCII-format, syntax see {@link JZcmdSyntax}
   * @param out Output channel for <+text>...<.+>
   * @param sCurrdir The start value for currdir
   * @param accessPrivate if true then private data are accessed too. The accessing of private data may be helpfull
   *  for debugging. It is not recommended for general purpose! The access mechanism is given with 
   *  {@link java.lang.reflect.Field#setAccessible(boolean)}.
   * @param testOut if not null then outputs a data tree of the generate script.
   * @param log A given log output
   * @return The text which are created in the script using <:>text<.>
   * @throws ScriptException 
   */
  public static void execute(JZcmdExecuter executer, File fileScript, Appendable out, List<DataAccess.Variable<Object>> data
      , String sCurrdir, boolean accessPrivate
      , File testOut, MainCmdLogging_ifc log) 
  throws ScriptException {
    int lengthBufferGenctrl = (int)fileScript.length();
    StringPartScan sourceScript = null;
    try { 
      sourceScript = new StringPartFromFileLines(fileScript, lengthBufferGenctrl, "encoding", null);
    } catch(IOException exc){
      String sError = exc.getMessage();
      throw new ScriptException("JZcmd - Error script file not found; " + fileScript.getAbsolutePath() + "; " + sError); 
    }
    if(sourceScript !=null){
      //MainCmdLogging_ifc log1 = log == null ? new MainCmdLoggingStream(System.out) : log;
      JZcmdScript genScript = null; //gen.parseGenScript(fileGenCtrl, null);
      genScript = translateAndSetGenCtrl(sourceScript, log, testOut, fileScript);
      JZcmdExecuter executer1 = executer == null ? new JZcmdExecuter(log) : executer;
      executer1.execute(genScript, accessPrivate, true, out, data, sCurrdir);
    }
  }  


  
  
  /**Translates and executes a JZcmd script.
   * @param executer A given instance of the executer. 
   * @param fileScript the script in ASCII-format, syntax see {@link JZcmdSyntax}
   * @param out Output channel for <+text>...<.+>
   * @param sCurrdir The start value for currdir
   * @param accessPrivate if true then private data are accessed too. The accessing of private data may be helpfull
   *  for debugging. It is not recommended for general purpose! The access mechanism is given with 
   *  {@link java.lang.reflect.Field#setAccessible(boolean)}.
   * @param testOut if not null then outputs a data tree of the generate script.
   * @param log A given log output
   * @return The text which are created in the script using <:>text<.>
   * @throws ScriptException 
   */
  public static void execute(JZcmdExecuter executer, File fileScript, Appendable out
      , String sCurrdir, boolean accessPrivate
      , File testOut, MainCmdLogging_ifc log) 
  throws ScriptException {
    int lengthBufferGenctrl = (int)fileScript.length();
    StringPartScan sourceScript = null;
    try { 
      sourceScript = new StringPartFromFileLines(fileScript, lengthBufferGenctrl, "encoding", null);
    } catch(IOException exc){
      String sError = exc.getMessage();
      throw new ScriptException("JZcmd - Error script file not found; " + fileScript.getAbsolutePath() + "; " + sError); 
    }
    if(sourceScript !=null){
      //MainCmdLogging_ifc log1 = log == null ? new MainCmdLoggingStream(System.out) : log;
      JZcmdScript genScript = null; //gen.parseGenScript(fileGenCtrl, null);
      genScript = translateAndSetGenCtrl(sourceScript, log, testOut, fileScript);
      JZcmdExecuter executer1 = executer == null ? new JZcmdExecuter(log) : executer;
      executer1.execute(genScript, accessPrivate, true, out, sCurrdir);
    }
  }  


  
  
  
  /**Executes a textual given script in a existing instance of a {@link JZcmdExecuter}. 
   * 
   * @param executer
   * @param fileScript This file is used only as information to support the <&scriptdir>.
   * @param script The input which contains the JZcmd script
   * @param out output for the text
   * @param sCurrdir
   * @param accessPrivate if true then private data are accessed too. The accessing of private data may be helpfull
   *  for debugging. It is not recommended for general purpose! The access mechanism is given with 
   *  {@link java.lang.reflect.Field#setAccessible(boolean)}.
   * @param testOut if not null then outputs a data tree of the generate script.
   * @param log
   * @throws ScriptException on any non-caught exception in the script
   */
  public static void execute(
      JZcmdExecuter executer
    , File fileScript  
    , StringPartScan script
    , Appendable out
    , Map<String, DataAccess.Variable<Object>> data
    , String sCurrdir
    , boolean accessPrivate
    , File testOut
    , MainCmdLogging_ifc log
  ) throws ScriptException 
  {
    //MainCmdLogging_ifc log1 = log == null ? new MainCmdLoggingStream(System.out) : log;
    JZcmdScript genScript = null; //gen.parseGenScript(fileGenCtrl, null);
    genScript = translateAndSetGenCtrl(script, log, testOut, fileScript);
    JZcmdExecuter executer1 = executer == null ? new JZcmdExecuter(log) : executer;
    executer1.execute(genScript, accessPrivate, true, out, data, sCurrdir);
  }
  
  
  
  /**Translates a script into its internal form.
   * @param fileScript
   * @param log
   * @return
   * @throws ScriptException
   *    
   * @see #translateAndSetGenCtrl(String, MainCmdLogging_ifc)
   * , {@link #translateAndSetGenCtrl(StringPartScan, MainCmdLogging_ifc, File, File)}
   * , {@link #translateAndSetGenCtrl(File, File, MainCmdLogging_ifc)}
   */
  public static JZcmdScript translateAndSetGenCtrl(File fileScript, MainCmdLogging_ifc log) 
  throws ScriptException
  //throws FileNotFoundException, IllegalArgumentException, IllegalAccessException, InstantiationException, IOException, ParseException, XmlException 
  {
    return translateAndSetGenCtrl(fileScript, null, log);
  }
  
  
  public static JZcmdScript translateAndSetGenCtrl(File fileGenCtrl, File checkXmlOut, MainCmdLogging_ifc log) 
  throws ScriptException
  //throws FileNotFoundException, IllegalArgumentException, IllegalAccessException, InstantiationException, IOException, ParseException, XmlException 
  {
    int lengthBufferGenctrl = (int)fileGenCtrl.length();
    StringPartScan sourceScript = null;
    try {
      sourceScript = new StringPartFromFileLines(fileGenCtrl, lengthBufferGenctrl, "encoding", null);
    } catch (IllegalCharsetNameException e) {
      if(sourceScript !=null) { sourceScript.close(); }
      throw new ScriptException("JZcmd.translate - illegal CharSet in file; ", fileGenCtrl.getAbsolutePath(), -1);
    } catch (UnsupportedCharsetException e) {
      if(sourceScript !=null) { sourceScript.close(); }
      throw new ScriptException("JZcmd.translate - illegal CharSet in file; ", fileGenCtrl.getAbsolutePath(), -1);
    } catch (FileNotFoundException e) {
      throw new ScriptException("JZcmd.translate - file not found; ", fileGenCtrl.getAbsolutePath(), -1);
    } catch (IOException e) {
      throw new ScriptException("JZcmd.translate - any file error; ", fileGenCtrl.getAbsolutePath(), -1);
    }
    JZcmdScript scr = translateAndSetGenCtrl(sourceScript, log, checkXmlOut, fileGenCtrl);
    sourceScript.close();
    return scr;
  }
  
  
  public JZcmdScript compile(File fileGenCtrl, File checkXmlOut) 
  throws ScriptException
  //throws FileNotFoundException, IllegalArgumentException, IllegalAccessException, InstantiationException, IOException, ParseException, XmlException 
  {
    int lengthBufferGenctrl = (int)fileGenCtrl.length();
    StringPartScan sourceScript;
    try {
      sourceScript = new StringPartFromFileLines(fileGenCtrl, lengthBufferGenctrl, "encoding", null);
    } catch (IllegalCharsetNameException e) {
      throw new ScriptException("JZcmd.translate - illegal CharSet in file; ", fileGenCtrl.getAbsolutePath(), -1);
    } catch (UnsupportedCharsetException e) {
      throw new ScriptException("JZcmd.translate - illegal CharSet in file; ", fileGenCtrl.getAbsolutePath(), -1);
    } catch (FileNotFoundException e) {
      throw new ScriptException("JZcmd.translate - file not found; ", fileGenCtrl.getAbsolutePath(), -1);
    } catch (IOException e) {
      throw new ScriptException("JZcmd.translate - any file error; ", fileGenCtrl.getAbsolutePath(), -1);
    }
    return compile(sourceScript, checkXmlOut, fileGenCtrl);
  }
  
  
  public static JZcmdScript translateAndSetGenCtrl(String sourceScript, MainCmdLogging_ifc log) 
  throws ScriptException
  //throws IllegalArgumentException, IllegalAccessException, InstantiationException, ParseException 
  { return translateAndSetGenCtrl(new StringPartScan(sourceScript), log, null, null);
  }
  
  
  /**Translates with a new Parser and the given script in text format.
   * @param fileParent The directory which is the anchor for included scripts. Maybe null if included scripts does not exists.
   * @param sourceScript The script in form of StringPartScan. User new {@link StringPartScan#StringPartScan(CharSequence)}
   *   to create one with a String given syntax.
   * @param checkXmlOutput may be null, for output the script in XML form.
   * @param log
   * @return
   * @throws ParseException
   * @throws IllegalArgumentException
   * @throws IllegalAccessException
   * @throws InstantiationException
   * @throws FileNotFoundException
   * @throws IOException
   * 
   * @see #translateAndSetGenCtrl(String, MainCmdLogging_ifc), {@link #translateAndSetGenCtrl(File, MainCmdLogging_ifc)}
   * , {@link #translateAndSetGenCtrl(File, File, MainCmdLogging_ifc)}
   */
  public static JZcmdScript translateAndSetGenCtrl(StringPartScan sourceScript, MainCmdLogging_ifc log) 
  //throws ParseException, IllegalArgumentException, IllegalAccessException, InstantiationException 
  throws ScriptException
  { return translateAndSetGenCtrl(sourceScript, log, null, null);
  }
  
  


  /**Reads a scriptfile and stores the subroutines, nested in classes too, to the given CmdStore.
   * The content of the CmdStore can be presented in a choice list or commands can be selected by name later to execute there.
   * @param dst The command store. Its content won't be cleared, this file is added. invoke {@link CmdStore#clear()} before.
   * @param jzScriptFile
   * @param log
   * @param executerToInit The executer will be initialized with the script variables of the parsed script-
   * @return null if successfully. Elsewhere an error text. 
   */
  public static String readJZcmdCfg(JZcmdScript.AddSub2List dst, File jzScriptFile, MainCmdLogging_ifc log, CmdExecuter execToInit) {
    String error = null;
    try{ 
      JZcmdScript script = translateAndSetGenCtrl(jzScriptFile, new File(jzScriptFile.getParentFile(), jzScriptFile.getName() + ".check.xml"), log);
      script.addContentToSelectContainer(dst);
      if(execToInit !=null) {
        execToInit.initJZcmdExecuter(script, null, log);  //NOTE: currdir is not determined.
      }
        //main.cmdSelector.initExecuter(script);
    } catch(Throwable exc){
      
      log.writeError("JZcmdScript error,", exc);
      error = "JZcmdScript error," + exc.getMessage();
    }
    return error;
  }
  
  

  
  /**The parser knows the correct syntax already. One should use
   *   {@link JZcmdSyntax#syntax} to set {@link ZbnfParser#setSyntax(String)}. One should use an 
   *   abbreviating syntax for experience.
   * 
   */
  final ZbnfParser parserGenCtrl;
  
  
  final MainCmdLogging_ifc log;
  
  /**Translates with a new Parser and the given script in text format.
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
   * @throws ParseException
   * @throws IllegalArgumentException
   * @throws IllegalAccessException
   * @throws InstantiationException
   * @throws FileNotFoundException
   * @throws IOException
   */
  public static JZcmdScript translateAndSetGenCtrl(StringPartScan sourceScript, 
      MainCmdLogging_ifc log, File checkXmlOutput, File fileScript) 
  throws ScriptException
  //throws ParseException, IllegalArgumentException, IllegalAccessException, InstantiationException, FileNotFoundException, IOException 
  { //MainCmdLogging_ifc log1;
    final JZcmd jzcmd = new JZcmd(log); 
    JZcmdScript compiledScript = new JZcmdScript(log, fileScript, jzcmd);
    File dirIncludeBase = FileSystem.getDir(fileScript);
    JZcmdScript.ZbnfJZcmdScript zbnfDstScript = new JZcmdScript.ZbnfJZcmdScript(compiledScript);
    jzcmd.translateAndSetGenCtrl(sourceScript, zbnfDstScript, dirIncludeBase, checkXmlOutput);
    return compiledScript;
  }
    
  
  
  
  /**Translates with a new Parser and the given script in text format.
   * @param fileScript The file which has contained the script. It is used only to provide the variables
   *   'scriptdir' and 'scriptfile' for execution. The file is not evaluated. It means, it does not need
   *   to exist.<br>
   *   The fileScript's parent is the directory which is the anchor for included scripts. 
   *   Maybe null if included scripts are not used and the variable 'scriptfile' and 'scriptdir' are not used in the script.s 
   * @param sourceScript The content of fileScript or from any other source. This is the script source. 
   *   Use new {@link StringPartScan#StringPartScan(CharSequence)} to create one with a String given syntax.
   * @param log
   * @param checkXmlOutput may be null, for output the script in XML form.
   * @return
   * @throws ParseException
   * @throws IllegalArgumentException
   * @throws IllegalAccessException
   * @throws InstantiationException
   * @throws FileNotFoundException
   * @throws IOException
   */
  private JZcmdScript compile(StringPartScan sourceScript, File checkXmlOutput, File fileScript) 
  throws ScriptException
  //throws ParseException, IllegalArgumentException, IllegalAccessException, InstantiationException, FileNotFoundException, IOException 
  { //MainCmdLogging_ifc log1;
    JZcmdScript compiledScript = new JZcmdScript(log, fileScript, this);
    File dirIncludeBase = FileSystem.getDir(fileScript);
    JZcmdScript.ZbnfJZcmdScript zbnfDstScript = new JZcmdScript.ZbnfJZcmdScript(compiledScript);
    this.translateAndSetGenCtrl(sourceScript, zbnfDstScript, dirIncludeBase, checkXmlOutput);
    return compiledScript;
  }
    
  
  
  
  
  /**Translates one file, called recursively for included files.
   * @param sourceScript The script in form of StringPartScan. User new {@link StringPartScan#StringPartScan(CharSequence)}
   *   to create one with a String given syntax.
   * @param zbnfDstScript Instance to store the parse result, it refers the compiled script. 
   * @param dirIncludeBase The directory which is the anchor for included scripts. Maybe null if included scripts does not exists.
   * @param checkXmlOutput may be null, for output the script in XML form.
   * @throws ParseException
   * @throws IllegalArgumentException
   * @throws IllegalAccessException
   * @throws InstantiationException
   * @throws FileNotFoundException
   * @throws IOException
   */
  private void translateAndSetGenCtrl(StringPartScan sourceScript, JZcmdScript.ZbnfJZcmdScript zbnfDstScript
      , File dirIncludeBase 
      , File checkXmlOutput) 
  //throws ParseException, IllegalArgumentException, IllegalAccessException, InstantiationException, FileNotFoundException, IOException 
  throws ScriptException
  { boolean bOk;
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
    final ZbnfJavaOutput parserGenCtrl2Java = new ZbnfJavaOutput(log);
    //
    //create a new instance of script-file for any file, especially for included.
    zbnfDstScript.scriptfile = new JZcmdScript.Scriptfile();
    
    try {
      parserGenCtrl2Java.setContent(JZcmdScript.ZbnfJZcmdScript.class, zbnfDstScript, parserGenCtrl.getFirstParseResult());
    } catch (Exception exc) { throw new ScriptException(exc); }
    if(zbnfDstScript.isXmlSrcNecessary()){
      zbnfDstScript.setXmlSrc(parserGenCtrl.getResultTree());  //to output XML from Script executer.
    }
    //
    //get the main routine from the first parsed file, store it, set it after processing includefiles.
    //
    JZcmdScript.Subroutine mainRoutine = zbnfDstScript.scriptfile.getMainRoutine();
    //
    if(zbnfDstScript.scriptfile.includes !=null){
      //parse includes after processing this file, because the zbnfDstScript.includes are not set before.
      //If one include contain a main, use it. But override the main after them, see below.
      for(JZcmdScript.JZcmdInclude include: zbnfDstScript.scriptfile.includes){
        String sFileInclude;
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
        File fileIncludeParent = FileSystem.getDir(fileInclude);
        int lengthBufferGenctrl = (int)fileInclude.length();
        StringPartScan sourceScriptIncluded;
        try {
          sourceScriptIncluded = new StringPartFromFileLines(fileInclude, lengthBufferGenctrl, "encoding", null);
        } catch (Exception exc) { throw new ScriptException(exc); }
        //
        //included script, call recursively.
        translateAndSetGenCtrl(sourceScriptIncluded, zbnfDstScript, fileIncludeParent, checkXmlOutput);
      }
    }
    //
    //set the main routine from the first parsed file if existing:
    //
    if(mainRoutine !=null){  //the main routine of the main file wins against includes if exists.
      zbnfDstScript.setMainRoutine(mainRoutine);
    }
  }
  
  

  
  
  /**JSR-223-conform method to compile. This method does not support included scripts with relative path.
   * @see javax.script.Compilable#compile(java.lang.String)
   */
  @Override
  public CompiledScript compile(String script) throws ScriptException
  {
    StringPartScan spSource = new StringPartScan(script);
    JZcmdScript compiledScript = new JZcmdScript(log, null, this);
    JZcmdScript.ZbnfJZcmdScript zbnfDstScript = new JZcmdScript.ZbnfJZcmdScript(compiledScript);
    try{ translateAndSetGenCtrl(spSource, zbnfDstScript, null, null);
    } catch(Exception exc){
      //cannot compile
      throw new ScriptException(exc);
    }
    return compiledScript;
  }


  /**JSR-223-conform method to compile. This method does not support included scripts with relative path.
   * @see javax.script.Compilable#compile(java.lang.String)
   */
  @Override
  public CompiledScript compile(Reader script) throws ScriptException
  { //NOTE: unoptimized, use StringPartScan(Reader)
    int size = 8192;
    int length;
    char[] buffer;
    try{
      do{
        size = 2*size;
        buffer = new char[size];
        length = script.read(buffer);
      } while(length == size && size < 5000000);
      if(length == size) throw new ScriptException("script to long; " + length);
    } catch(IOException exc){ throw new ScriptException(exc); }
    String source = new String(buffer, 0, length);
    return compile(source);
  }


  @Override
  public Bindings createBindings()
  {
    // TODO Auto-generated method stub
    return null;
  }


  @Override
  public Object eval(String script) throws ScriptException
  {
    // TODO Auto-generated method stub
    return null;
  }


  @Override
  public Object eval(Reader reader) throws ScriptException
  {
    // TODO Auto-generated method stub
    return null;
  }


  @Override
  public Object eval(String script, ScriptContext context)
      throws ScriptException
  {
    // TODO Auto-generated method stub
    return null;
  }


  @Override
  public Object eval(Reader reader, ScriptContext context)
      throws ScriptException
  {
    // TODO Auto-generated method stub
    return null;
  }


  @Override
  public Object eval(String script, Bindings n) throws ScriptException
  {
    // TODO Auto-generated method stub
    return null;
  }


  @Override
  public Object eval(Reader reader, Bindings n) throws ScriptException
  {
    // TODO Auto-generated method stub
    return null;
  }


  @Override
  public Object get(String key)
  {
    // TODO Auto-generated method stub
    return null;
  }


  @Override
  public Bindings getBindings(int scope)
  {
    // TODO Auto-generated method stub
    return null;
  }


  @Override
  public ScriptContext getContext()
  {
    JZcmdExecuter executer = new JZcmdExecuter();
    return executer.scriptLevel();
  }


  @Override
  public ScriptEngineFactory getFactory()
  {
    // TODO Auto-generated method stub
    return null;
  }


  @Override
  public void put(String key, Object value)
  {
    // TODO Auto-generated method stub
    
  }


  @Override
  public void setBindings(Bindings bindings, int scope)
  {
    // TODO Auto-generated method stub
    
  }


  @Override
  public void setContext(ScriptContext context)
  {
    // TODO Auto-generated method stub
    
  }
  
  
}
