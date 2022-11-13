package org.vishia.cmd;


import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.Flushable;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptException;
import org.vishia.cmd.CmdExecuter;
import org.vishia.cmd.JZtxtcmdScript.Subroutine;
import org.vishia.fileRemote.FileRemote;
import org.vishia.mainCmd.MainCmd;
import org.vishia.mainCmd.MainCmdLoggingStream;
import org.vishia.mainCmd.MainCmdLogging_ifc;
import org.vishia.util.Assert;
import org.vishia.util.CalculatorExpr;
import org.vishia.util.Conversion;
import org.vishia.util.DataAccess;
import org.vishia.util.Debugutil;
import org.vishia.util.FilePath;
import org.vishia.util.FileFunctions;
//import org.vishia.util.IndexMultiTable;
import org.vishia.util.StringFormatter;
import org.vishia.util.StringFunctions;
import org.vishia.util.StringPartAppend;
import org.vishia.util.StringSeq;
import org.vishia.util.CalculatorExpr.Value;
import org.vishia.util.DataAccess.Variable;
import org.vishia.xmlSimple.SimpleXmlOutputter;



/**This class is the executer of JZcmd. The translated JZscript is contained in an instance of {@link JZtxtcmdScript}. 
 * It can be executed with a given script:
 * <ul>
 * <li>{@link #execute(JZtxtcmdScript, boolean, boolean, Appendable, String)} executes a given translated script.
 * <li>{@link #execSub(org.vishia.cmd.JZtxtcmdScript.Subroutine, Map, boolean, Appendable, File)} executes one subroutine
 *   from a translated script. It is possible to translate a script one time, and then invoke any subroutine 
 *   from a java context on demand.
 * <li>{@link #initialize(JZtxtcmdScript, boolean, Map, String, boolean)} cleans the instance and generates all script variables.
 *   It prepares usage for {@link #execSub(org.vishia.cmd.JZtxtcmdScript.Subroutine, Map, boolean, Appendable, File)}.
 * <li>{@link #reset()} cleans the instance. The scriptvariables will be generate 
 *   by first call of {@link #execute(JZtxtcmdScript, boolean, boolean, Appendable, String)}
 * </ul>
 * To use the Java platform's {@link javax.script.CompiledScript} eval method start {@link JZtxtcmdScript#eval(ScriptContext)}
 * with an instance of {@link #scriptLevel()}.
 * <br><br> 
 * An instance of this class is used while {@link #execute(JZcmdScript, boolean, boolean, Appendable))} is running.
 * You should not use the instance concurrently in more as one thread. But you can use this instance
 * for one after another call of {@link #execute(JZtxtcmdScript, boolean, boolean, Appendable)}.
 * Note that Threads can be created in the script.
 * <br><br>
 * @author Hartmut Schorrig
 *
 */
@SuppressWarnings("synthetic-access") 
public class JZtxtcmdExecuter {
  
  
  /**Version, history and license.
   * <ul>
   * <li>2022-11-13 Hartmut meaningful change: {@link ExecuteLevel#changeCurrDir(CharSequence)} now sets the "user.dir" Java system's property.
   *   With them it is possible generally in Java to open a file via new File(System.getProperty("user.dir"), sRelativePath),
   *   then this is effective. It does not effect the operation system's PWD, but usable via this property.
   * <li>2022-01-31 Hartmut feature: Now the order of statements from included files are proper. 
   *   In the versions before script statements of included files were placed
   *   after the statements of the current file, due to processing the includes after wards.
   *   Now in the {@link JZtxtcmdScript#createScriptFromString(org.vishia.util.StringPartScan, MainCmdLogging_ifc, File, File)}
   *   the included script statements are stored in the {@link JZtxtcmdScript.JZcmdInclude} item itself,
   *   and executed in this order. Additonal case '.' in {@link ExecuteLevel#execute(org.vishia.cmd.JZtxtcmdScript.StatementList, StringFormatter, int, Map, int)}.
   * <li>2021-06-21 Hartmut new jztc.envar A container which can be given to hold variables from the calling environment
   *   (a Java program) This variables are not changed if a new {@link JZtxtcmdScript} is translated and started.
   *   Used firstly for the Stimuli Selector GUI {@linkplain https://vishia.org/StimuliSel/html/StimuliSel.html}
   * <li>2021-06-12 Hartmut now write to System.err if inside a thread there is an error. Before (bug) it was silent,
   *   an error was not seen.
   * <li>2021-06-12 Hartmut capability of {@link JzTcMain#envar} for simple access to Java data from the calling environment.
   *   {@link #initialize(JZtxtcmdScript, boolean, List, Map, CharSequence)} enhanced for that.
   * <li>2021-06-11 Hartmut refactoring: Only formally replacements, but possible side effects, not supposed but not excluded.
   * Change IndexMultiTable to TreeMap for the variables. 
   * The IndexMultiTable is also a Map, but not supported from Eclipse debug view "Show Logical Structure".
   * It is more complex handling for debugging. Hence replaced.
   * <br>The difference TreeMap, IndexMultiTable is: last one should not have unique keys.
   * But for that application the keys are unique (the variables). Hence there should not be side effects.
   * Only one passage in {@link ExecuteLevel#exec_cmdline(org.vishia.cmd.JZtxtcmdScript.CmdInvoke), see comment there.
   * <li>2021-06-08 Hartmut improved: throw on missing main is a better error messaging.
   * <li>2021-02-07 Hartmut new: <code>List ... [ @ <$?keyVariableName> ]</code> then the List is stored 
   *   with a Map part to access an element via the value in the named variable. Access possible via get(name)
   * <li>2021-02-06 Hartmut bugfix: Now variables scriptfile and scriptdir are correct.
   * <li>2020-07-22 Hartmut bugfix: now invoke level.close(): {@link ExecuteLevel#close()}, 
   *   it invokes this.cmdExecuter.close() {@link CmdExecuter#close() }which closes the out and error Thread.
   *   Note: The {@link ExecuteLevel#cmdExecuter} will remain open, not removed, so long the level runs. 
   *   For this time the output and error thread of the CmdExecuter remain open. It is a proven concept since few years.
   *   Do not close the CmdExcuter every time when finished. Held open for the next action may be in a loop.
   * <li>2020-03-14 Hartmut bugfix: {@link ExecuteLevel#ctNesting} should be counted if internal execute levels are created, 
   *   elsewhere the {@link ExecuteLevel#cmdExecuter} is closed on inner levels. fixed. 
   * <li>2018-10-16 Hartmut bugfix: The empty statement list in script part <code>onerror{}</code> was syntactically admissible but the execution crashes. fixed. 
   * <li>2018-09-22 Hartmut new in {@link ExecuteLevel#exec_OpenTextOut(org.vishia.cmd.JZtxtcmdScript.JZcmditem, StringFormatter, boolean)} :
   *   <code>text = appendableObj</code> can be used yet, it is especially to assign the text output to an GUI output box widget. 
   * <li>2018-09-17 new {@link JzTcMain#sleep(int)} 
   * <li>2018-09-10 Hartmut bugfix. The script variable text = path/to/textout cannot be set. 
   *   Now the argument -t=out wins against the script variable, but if -t is not set, the scriptVariable text = ... is used.
   *   bugfix: setting text = ... in the script should be used immediately after that. Changing of {@link ExecuteLevel#exec_OpenTextOut(org.vishia.cmd.JZtxtcmdScript.JZcmditem, StringFormatter, boolean)}.
   * <li>2018-08-29 Hartmut new: {@link ExecuteLevel#exec_DefVariable(Map, org.vishia.cmd.JZtxtcmdScript.DefVariable, char, Object, boolean)}:
   *   Converts to Numeric value on a Num variable ('K'). TODO: do same for Bool ('Q') 
   * <li>2018-08-29 Hartmut new: true, false, Num as script variable. 
   * <li>2018-08-29 Hartmut new: Conversion Bool to boolean for java operation args, see {@link DataAccess}. 
   * <li>2018-01-07 Hartmut new: mkdir with ' at least for path/to/file, see documentation. 
   * <li>2017-09-09 Hartmut chg: The zmake uses a datapath yet instead a String-named Fileset variable. It is more universal in application
   *   and more clear in syntax. There may be used 2 datapath, 1 for the accessPath and the Fileset. If only one datapath is used, 
   *   the type of the gotten instance decides whether it is only one file given with access path (not a Fileset) 
   *   or the only one datapath is the Fileset. Writing <code>&access.path&myfileset</code> or <code>&access.onefile</code> or <code>&myFileset</code>
   *   The older scripts are running unregarded of this change. The description in 
   *   <a href="http://www.vishia.org/docuZBNF/JZtxtcmd.html#Topic.JZtxtcmd.statements.zmake.">www.vishia.org/.../JZtxtcmd.html</a> is renewed.
   * <li>2017-09-04 Hartmut bugfix: {@link ExecuteLevel#evalCondition(org.vishia.cmd.JZtxtcmdScript.JZcmditem)}: A Integer value will be convert
   *   to bool now. Before: Bool val = 0 is interpret as true because 0 is an Integer object which exists. Now Integer.intValue() !=0 is checked. 
   * <li>2017-08-30 Hartmut new: implementation of the feature <code>Obj name :type = ....</code>, 
   *   checking of the type now on assigning of the value. This feature was prepared in syntax but not implemented till now.
   * <li>2017-08-30 Hartmut new: uses {@link JzScriptException} extends {@link ScriptException} only because the line:col should be able to read
   *   on start of the next line, better able to found on error messages. 
   * <li>2017-07-12 Hartmut new: debugOp
   * <li>2017-06-13 Hartmut chg: for(entry: numvalue) works now for 0 to numvalue-1, especially for array indices. 
   * <li>2017-01-13 Hartmut chg: On any call of execute(...) or execSub(..) the given script will be used as current and replaces an old one. 
   *   If it is another script as the last one, the new script will be established. 
   *   It means, the existing script variables are deleted and the script variables of the new given script are calculated and stored.
   *   If the script is the same, the variables won't be replaced. It is a spare of time, but also additional variables will be kept.
   *   The new rule is: the JZcmdExecuter can be used for several scripts one after another (in one thread). The current script wins. 
   * <li>2016-12-27 Hartmut chg: {@link #execSub(org.vishia.cmd.JZtxtcmdScript.Subroutine, List, boolean, Appendable, File)} now works 
   *   with a List of arguments instead Map. The arguments are checked 
   *   in {@link ExecuteLevel#exec_Subroutine(org.vishia.cmd.JZcmdScript.Subroutine, ExecuteLevel, List, List, StringFormatter, int, int).}  
   *   Thus argument type conversion is done especially argument type {@link FilePath} for the.File.commander - execution.
   *   The {@link #execSub(org.vishia.cmd.JZtxtcmdScript.Subroutine, Map, boolean, Appendable, File)} 
   *   with Map supports a Variable Map, but it is not necessary for example for the.File:commander execution. 
   *   The {@link ExecuteLevel#levelForSubroutine(org.vishia.cmd.JZtxtcmdScript.Subroutine)} now 
   *   centralizes getting a new level (gardening, before more a one time the adquate algorithm). 
   * <li>2016-12-27 Hartmut new: {@link #abortCmdExecution()} necessary if an execution hangs. 
   * <li>2016-12-27 Hartmut new: class {@link JzTcMain} renamed from JZcmd and set package private. Because possible confusion with {@link org.vishia.jztxtcmd.JZtxtcmd} 
   * <li>2016-03-06 Hartmut new: 
   *   <ul><li>Create a codeBlock variable with {@link ExecuteLevel#exec_DefCodeblockVariable(Map, org.vishia.cmd.JZtxtcmdScript.Subroutine, boolean) on statement <code>Subtext name = ...</code>,
   *   <li>Create a codeBlock content in {@link ExecuteLevel#exec_DefList(org.vishia.cmd.JZtxtcmdScript.DefContainerVariable, Map)} on {@link Subroutine} as content
   *   <li>execute the codeBlock in {@link ExecuteLevel#exec_Call(org.vishia.cmd.JZtxtcmdScript.CallStatement, List, StringFormatter, int, int)} on found codeBlock variable as name
   *   <li>execute the codeBlock in  {@link ExecuteLevel#exec_Datatext(org.vishia.cmd.JZtxtcmdScript.DataText, StringFormatter, int, int)} on found codeBlock variable (type 'X') with local given variables.
   *   <li>evaluating of the code block with
   *   </ul> 
   * <li>2016-02-20 Hartmut new: ExecuteLevel#exec_Datatext(...): On exception an replacement text can be written, Syntax is <&datapath:?replacement:format> 
   * <li>2016-02-20 Hartmut new: Check statementList on if, while, for because an empty list is admissible. The syntax is changed.
   * <li>2016-02-20 Hartmut chg: The for variable is set to null on end of for-loop without break. Therewith it can be tested whether a for has broken on found element. The description is enhanced with that feature with example.  
   *   It is admissible to write <code> for(variable:container && !variable.check()); </code> especially to search somewhat in a container. 
   * <li>2016-02-20 Hartmut gardening in check of break in while loops.
   * <li>2016-02-20 Hartmut bugfix: ExecuteLevel.isBreak was set permanently and has broken the next for-loop. 
   *   fix: The break statement forces return {@link #kBreak}. That is used in while loops already. Now used in for loop too. gardening.
   * <li>2016-01-10 Hartmut new: {@link #execute_Scriptclass(String)}
   * <li>2016-01-10 Hartmut chg: {@link #execSub(org.vishia.cmd.JZtxtcmdScript.Subroutine, Map, boolean, Appendable, File)} now returns the "return" Map, not the localVariables. This is a more consequent concept.
   *   To assemble some user Parameter use {@link #execute_Scriptclass(org.vishia.cmd.JZtxtcmdScript.JZcmdClass)}, whereby a class can define some variables. 
   * <li>2016-01-09 Hartmut {@link ExecuteLevel#exec_Call(org.vishia.cmd.JZtxtcmdScript.CallStatement, List, StringFormatter, int, int)} Execution of a <:subtext:&var> which contains a Subtext is prepared, 
   *   but the maybe better variant is only <&var> and detection, it is a subtext.
   * <li>2016-01-09 Hartmut 'e': {@link ExecuteLevel#exec_Datatext(org.vishia.cmd.JZtxtcmdScript.DataText, StringFormatter, int, int)} Execution of a Subtext variable which's statements are evaluated in the given environment.
   * <li>2016-01-09 Hartmut 'L': {@link ExecuteLevel#exec_DefList(org.vishia.cmd.JZtxtcmdScript.DefContainerVariable, Map)}: Execution of a List, it is filled on creation of a list variable with content which is evaluated in the given environment.
   * <li>2016-01-06 Hartmut functionality enhancing: {@link #execSub(org.vishia.cmd.JZtxtcmdScript.Subroutine, Map, boolean, Appendable, File)} now returns all variable of the subroutine level.
   * <li>2016-01-06 Hartmut functionality enhancing: {@link #execSub(org.vishia.cmd.JZtxtcmdScript.Subroutine, Map, boolean, Appendable, File)} can work on the script level variables.
   *   That is proper for initializing routines or routines for parameter.
   * <li>2015-12-24 Hartmut chg: <&datatext>: On a {@link CalculatorExpr.Value} it invokes {@link CalculatorExpr.Value#stringValue()} to convert in a String representation.
   *   The {@link CalculatorExpr.Value#toString()} is not proper for that. Commonly toString() is invoked on any Object.
   * <li>2015-08-30 Hartmut chg: The functionality to remove indentation is moved to the JZcmdScript
   *   because it is done only one time on preparation of the script, not more as one in any loop of execution.
   *   It is changed in functionality: <code><:s></code> to skip over white spaces and the next line indentation.
   * <li>2015-08-30 Hartmut bugfix: The {@link JzTcMain#currdir()} and {@link JzTcMain#calctime()} should be moved from the environment class
   *   because there should be able to access as <code>jzcmd.currdir()</code> in the script.   
   * <li>2015-08-30 Hartmut new: The simple syntax <code>text = newFile;</code> to change the <code><+>output<.+></code>
   *   has a less semantic effect. Therefore it is replaced by <code><+:create>newFile<.+></code> or <code><+:append>...</code>
   *   with the possibility of append to an existing file.  
   * <li>2015-08-30 Hartmut bug: Closing System.out, <+out>text... does not work if a new text output will be used to replace the given one
   *   by <+:create>newFile<.+>. Fix:  System.out is used for the text output via the argument out for {@link #execute(JZtxtcmdScript, boolean, boolean, Appendable, String)}.
   *   That is done if the {@link org.vishia.jztxtcmd.JZtxtcmd} script was called without an -text argument. The System.out should not closed.
   *   It is checked now.
   * <li>2015-08-30 Hartmut new: flush an output in {@link ExecuteLevel#exec_TextAppendToVar(org.vishia.cmd.JZtxtcmdScript.TextOut, int)}.
   * <li>2015-07-18 Hartmut chg: In {@link #execSub(org.vishia.cmd.JZtxtcmdScript.Subroutine, Map, boolean, Appendable, File)}: 
   *   An exception should not be thrown forward, rather than an ScriptException with the file, line and column should be thrown
   *   because it is not an exception in deep Java algorithm mostly but it is usual an error in a script. That exception
   *   is a better hint to find out the problem.   
   * <li>2015-06-04 Hartmut bugfix and improve: in {@link ExecuteLevel#exec_Text(org.vishia.cmd.JZtxtcmdScript.JZcmditem, Appendable, int)}:
   *   The processing of insertion characters was not according to the description. It is fixed, enhanced and described. 
   *   Now '+++' and '===' are possible as insertion text marking characters too.
   * <li>2015-05-24 Hartmut chg: copy, move, del with options, uses {@link FileOpArg} 
   * <li>2015-05-23 Hartmut new: {@link JzTcMain#getstdin()} 
   * <li>2015-05-23 Hartmut chg: Divide the class JZcmdExecuter in a part which is available for execution:
   *   {@link JzTcMain}. While execution some methods from the main class should not in focus!
   * <li>2015-05-17 Hartmut new: syntax "File : <textValue>" is now able as start path of a DataPath.
   *   Therefore it's possible to write <code>File: "myFile".exists()</code> or adequate. A relative filename
   *   is related to the {@link JZtxtcmdExecuter.ExecuteLevel#currdir()}. 
   * <li>2015-05-17 Hartmut new: mkdir
   * <li>2015-05-17 Hartmut new: <code>text = path/to/output</code> is able to set in the script yet too.
   * <li>2015-05-10 Hartmut chg: {@link #execSub(org.vishia.cmd.JZtxtcmdScript.Subroutine, Map, boolean, Appendable, File)}
   *   uses the argumet out as textout for <code><+>text output<.+></code> now. It is adequate like 
   *   {@link #execute(JZtxtcmdScript, boolean, boolean, Appendable, String)} 
   * <li>2015-04-25 Hartmut chg: scriptdir is the really script dir on included scripts. 
   * <li>2014-12-14 Hartmut new: {@link ExecuteLevel#jzClass} stores the current class statement. Not call of own class methods without the class name is possible. TODO instances  
   * <li>2014-12-12 Hartmut new: If a subroutine's argument has the type 'F': {@link JZtxtcmdFilepath}, then the argument is converted into this type if necessary.
   *   TODO: do so for all non-Obj argument types. It is probably to work with a simple String as argument if a Filepath is expected. Therefore new {@link ExecuteLevel#convert2FilePath(Object)}. 
   * <li>2014-12-06 Hartmut new: don't need a main routine, writes only a warning on System.out. 
   * <li>2014-11-28 Hartmut chg: {@link ExecuteLevel#bForHasNext} instead calling argument for all execute...(...), it was forgotten in one level.
   * <li>2014-11-21 Hartmut chg: Character ::: can be used as indentation characters
   * <li>2014-11-16 Hartmut chg: enhancement of capability of set text column: <:@ nr> nr can be an expression. Not only a constant.   
   * <li>2014-10-20 Hartmut chg: break in a condition should break a loop, forwarding {@link #kBreak} from execution level in {@link ExecuteLevel#exec_IfStatement(org.vishia.cmd.JZtxtcmdScript.IfStatement, StringFormatter, int, int)}.
   * <li>2014-10-20 Hartmut new: Map name = { String variable = "value"; Obj next; } now works. Changed 'M' instead 'X' in {@link JZtxtcmdScript.JZcmditem#add_dataStruct(org.vishia.cmd.JZtxtcmdScript.StatementList)}.
   *   {@link ExecuteLevel#evalObject(org.vishia.cmd.JZtxtcmdScript.JZcmditem, boolean)} has accepted such dataStruct already (used in argumentlist), now with 'M' instead 'X'.
   *   {@link ExecuteLevel#exec_DefMapVariable(org.vishia.cmd.JZtxtcmdScript.DefVariable, Map)} newly.  
   * <li>2014-10-20 Hartmut bufgix: some parameter in call of {@link ExecuteLevel#execute(org.vishia.cmd.JZtxtcmdScript.StatementList, StringFormatter, int, boolean, Map, int)}
   *   were set faulty. Last change bug from 2014-07-27. 
   * <li>2014-08-10 Hartmut new: <:>...<.> as statement writes into the current out Buffer. Before: calculates an textexpression which is never used then.
   *   In opposite:<+>...<.> writes to the main text output always, not to the current buffer. 
   * <li>2014-08-10 Hartmut new: !checkXmlFile = filename; 
   * <li>2014-07-27 Hartmut bugfix: {@link ExecuteLevel#exec_hasNext(org.vishia.cmd.JZtxtcmdScript.JZcmditem, StringFormatter, int, boolean, int)}
   * <li>2014-07-27 Hartmut chg: save one level for recursive execution, less stack, better able to view
   *   by calling {@link ExecuteLevel#execute(org.vishia.cmd.JZtxtcmdScript.StatementList, StringFormatter, int, boolean, Map, int)}immediately.
   * <li>2014-06-15 Hartmut chg: improved {@link ExecuteLevel#exec_zmake(org.vishia.cmd.JZtxtcmdScript.Zmake, StringFormatter, int, int)}:
   *   works with a Filepath for output.
   * <li>2014-06-15 Hartmut chg: improved {@link ExecuteLevel#exec_Copy(org.vishia.cmd.JZtxtcmdScript.CallStatement)}:
   *   works with a Filepath. TODO for execMove!
   * <li>2014-06-14 Hartmut chg: {@link ExecuteLevel} implements {@link FilePath.FilePathEnvAccess} now,
   *   therewith a {@link JZtxtcmdFileset#listFiles(List, JZtxtcmdFilepath, boolean, org.vishia.util.FilePath.FilePathEnvAccess)}
   *   does not need an accessPath, it may be empty respectively null.
   * <li>2014-06-10 Hartmut chg: improved Exception handling of the script.
   * <li>2014-06-01 Hartmut chg: {@link #genScriptVariables(JZtxtcmdScript, boolean, Map, CharSequence)} and
   *   {@link #initialize(JZtxtcmdScript, boolean, Map, String, boolean)} throws on any error.
   * <li>2014-06-01 Hartmut chg: "File :" as conversion type for any objExpr, not in a dataPath.TODO: Do the same for Filepath, Fileset with accessPath
   * <li>2014-06-01 Hartmut chg: uses {@value #kException} as return value of all called 
   *   {@link ExecuteLevel#execute(org.vishia.cmd.JZtxtcmdScript.StatementList, StringFormatter, int, boolean, Map, int)}
   *   instead a new ForwardException. Reason: Stacktrace of Exception is not disturbed.
   *   The ForwardException has put its Stacktrace only. Too complex to read the causer. 
   * <li>2014-05-18 Hartmut chg: DefFilepath now uses a textValue and divides the path to its components
   *   at runtime, doesn't use the prepFilePath in ZBNF. Reason: More flexibility. The path can be assmebled
   *   on runtime.  
   * <li>2014-05-18 Hartmut new: try to implement javax.script interfaces, not ready yet
   * <li>2014-05-18 Hartmut chg: {@link ExecuteLevel#exec_Subroutine(org.vishia.cmd.JZtxtcmdScript.Subroutine, Map, StringFormatter, int)}
   *   called from {@link org.vishia.jztxtcmd.JZtxtcmd#execSub(File, String, Map, ExecuteLevel)} for a sub routine in a new translated script.
   * <li>2014-05-18 Hartmut chg: Handling of {@link ExecuteLevel#currdir}   
   * <li>2014-05-10 Hartmut new: File: supported as conversion with currdir. See {@link JZtxtcmdScript.JZcmdDataAccess#filepath}.
   * <li>2014-05-10 Hartmut new: {@link ExecuteLevel#cmdExecuter} instantiated in a whole subroutine. It is more faster
   *   instead creating a new instance for any cmd invocation.
   * <li>2014-04-24 Hartmut chg: {@link #execute(JZtxtcmdScript, boolean, boolean, Appendable, String)} 
   *   returns nothing, No confusion between argument out and return value! If out is not given,
   *   then <:>text<.> is not possible. It causes an NullPointerException which may be thrown.
   * <li>2014-04-24 Hartmut new: {@link #textline} the same for all threads, used synchronized. 
   * <li>2014-04-25 Hartmut new: {@link NextNr#start()} etc. more capability.
   * <li>2014-03-08 Hartmut new: debug_dataAccessArgument() able to call from outside, to force breakpoint.
   * <li>2014-03-08 Hartmut new: Filepath as type of a named argument regarded on call, see syntax
   * <li>2014-03-07 Hartmut new: All capabilities from Zmake are joined here. Only one concept!
   * <li>2014-03-01 Hartmut new: {@link ExecuteLevel#execForContainer(org.vishia.cmd.JZtxtcmdScript.ForStatement, Appendable, int)}
   *   now supports arrays as container too.
   * <li>2014-03-01 Hartmut new: !argsCheck! functionality.  
   * <li>2014-02-22 Hartmut new: Bool and Num as variable types.
   * <li>2014-02-16 Hartmut chg: Build of script variable currdir, scriptfile, scriptdir with them in {@link JZtxtcmdExecuter#genScriptVariables(JZtxtcmdScript, boolean, Map, CharSequence)}.
   *   {@link #execute(JZtxtcmdScript, boolean, boolean, Appendable, String)} and {@link #execSub(org.vishia.cmd.JZtxtcmdScript.Subroutine, Map, boolean, Appendable, File)}
   *   with sCurrDir.
   * <li>2014-02-01 Hartmut new: onerror errorlevel for cmd now works as statement. {@link ExecuteLevel#cmdErrorlevel}. 
   * <li>2014-02-01 Hartmut chg: now {@link ExecuteLevel#execute(org.vishia.cmd.JZtxtcmdScript.StatementList, Appendable, int, boolean)}
   *   returns the exit designation (break, return) 
   * <li>2014-01-12 Hartmut chg: now uses a Stringjar {@link StringPartAppend} instead StringBuffer in Syntax and execution.
   * <li>2014-01-09 Hartmut new: If the "text" variable or any other Appendable variable has a null-value,
   *   a StringBuilder is instantiated therefore and stored in this variable. It is possible therewith
   *   to create an text only if necessary. We don't need a StringBuilder instance if it is never used. 
   * <li>2014-01-06 Hartmut new: {@link ThreadData} has the error Variable, test thread 
   * <li>2013-12-26 Hartmut new: subroutine returns a value and assigns to any variable.
   * <li>2013-12-26 Hartmut re-engineering: Now the Statement class is obsolete. Instead all statements have the base class
   *   {@link JZcmditem}. That class contains only elements which are necessary for all statements. Some special statements
   *   have its own class with some more elements, especially for the ZBNF parse result. Compare it with the syntax
   *   in {@link org.vishia.jztxtcmd.JZtxtcmdSyntax}.    
   * <li>2013-10-27 Hartmut chg: Definition of a String name [= value] is handled like assign. Stored with 
   *   {@link DataAccess#storeValue(List, Map, Object, boolean)} with special designation in {@link DataAccess.DatapathElement#whatisit}
   * <li>2013-10-20 Hartmut chg: The {@link #scriptVariables} and the {@link ExecuteLevel#localVariables} are of type
   *   {@link DataAccess.Variable} and not only Object. Advantage: The variable references can be changed in the
   *   instance of {@link DataAccess.Variable#val}, this changing is valid for all references to that variable.
   *   In this kind a script-variable can be changed in a subroutine. This may be necessary and should be supported.
   *   Nevertheless a concept of non-changeable script variables may be proper. It should realized in runtime.
   * <li>2013-10-13 Hartmut chg: onerror: only the coherent statements in one block are checked for onerror. See description.
   *   onerror exit treaded.
   * <li>2013-01-13 Hartmut chg: The method getContent is moved and adapted to {@link ZbatchGenScript.ZbatchExpressionSet#ascertainValue(Object, Map, boolean, boolean, boolean)}.
   * <li>2013-01-12 Hartmut chg: improvements while documentation. Some syntax details. Especially handling of visibility of variables.
   * <li>2013-01-02 Hartmut chg: The variables in each script part are processed
   *   in the order of statements of generation. In that kind a variable can be redefined maybe with its own value (cummulative etc.).
   *   A ZText_scriptVariable is valid from the first definition in order of generation statements.
   *   But a script-global variable referred with {@link #listScriptVariables} is defined only one time on start of text generation
   *   with the routine {@link ZbatchExecuter#genScriptVariables(ZbatchGenScript, Object, boolean)}.  
   * <li>2012-12-23 Hartmut chg: {@link #getContent(org.vishia.zbatch.ZbatchGenScript.ZbatchExpressionSet, Map, boolean)} now uses
   *   an {@link ZbatchGenScript.ZbatchExpressionSet} instead a List<{@link DataAccess.DatapathElement}>. Therewith const values are able to use
   *   without extra dataPath, only with a ScriptElement.
   * <li>2012-12-23 Hartmut new: formatText in the {@link ZbatchGenScript.ZbatchExpressionSet#textArg} if a data path is given, use for formatting a numerical value.
   * <li>2012-12-08 Hartmut new: <:subtext:name:formalargs> has formal arguments now. On call it will be checked and
   *   maybe default values will be gotten.
   * <li>2012-12-08 Hartmut chg: {@link #parseGenScript(File, Appendable)}, {@link #genScriptVariables()}, 
   *   {@link #genContent(ZbatchGenScript, Object, boolean, Appendable)} able to call extra especially for Zmake, currDir.
   *   It is possible to define any script variables in the generating script and use it then to control getting data 
   *   from the input data.
   * <li>2012-11-25 Hartmut chg: Now Variables are designated starting with $.
   * <li>2012-11-04 Hartmut chg: adaption to DataAccess respectively distinguish between getting a container or an simple element.
   * <li>2012-10-19 Hartmut chg: <:if...> works.
   * <li>2012-10-10 Usage of {@link ZbatchGenScript}.
   * <li>2012-10-03 created. Backgorund was the {@link org.vishia.zmake.Zmake} generator, but that is special for make problems.
   *   A generator which converts ZBNF-parsed data from a Java data context to output texts in several form, documentation, C-sources
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
  static final public String version = "2022-11-13";

  /**This class is the jzcmd main level from a script.
   * @author Hartmut Schorrig
   *
   */
  public static class JzTcMain
  {
    final JZtxtcmdExecuter jzCmdExecuter;
  
    public final MainCmdLogging_ifc log;
    /**The newline char sequence. */
    public String newline = "\r\n";
    /**The width or size of a tab in the script file is used for detect and ignore tab indentation in the script. */ 
    public int tabsize = 4;
    /**Used for formatting Numbers. 
     * Problem in Germany: The numbers are written with , instead with a decimal point. 
     * Using Locale.ENGLISH produces the well used decimal point.
     * Numbers with comma are used only in the german banking sector, not in engineering.
     */
    protected Locale locale = Locale.ENGLISH;
    
    /**It is set from argument outside. Used in initialization (execution of script level). Set to null on start of execution. 
     * If it is set and it is not System.out, the assignment of a <code>text = path/to/textout;</code> is suppressed in script level.
     * It means, a textout can be set in script level, but a argument -t=textout on invocation wins.
     * On runtime a <code>text = path/to/newTextOut;</code> can be set because this variable is ==null then.
     * @since 2018-09*/
    Appendable outFromCmdlineArg;
    
    
    /**The text output, the same for all threads. It refers System.out if an other output was not defined yet. 
     * It is a StringFormatter associated with an Appendable. 
     * @since 2018-09 It is the <code>text</code> script variable */
    StringFormatter textline;
    
    /**The time stamp from {@link System#currentTimeMillis()} on start of script. */
    public long startmilli;
    
    /**The time stamp from {@link System#nanoTime()} on start of script. */
    public long startnano;
    
    private int nextNr_ = 0;
    /**Set it to true if private and protected fields and methods should be used
     * by data access.
     */
    public boolean bAccessPrivate;
    
    /**Instance for the main script part. */
    //Gen_Content genFile;
    
    /**The java prepared generation script. */
    JZtxtcmdScript jzcmdScript;
    
    Queue<CmdExecuter> runningCmdExecuter = new ConcurrentLinkedQueue<CmdExecuter>();
    
    
    public final Queue<JZtxtcmdThreadData> threads = new ConcurrentLinkedQueue<JZtxtcmdThreadData>();
    
    public final JZtxtcmdThreadData scriptThread;

    
    public final ExecuteLevel scriptLevel;
    
    
    /**possible environment variables from a calling Java environment. 
     * null if no environment variables are given on calling {@link JZtxtcmdExecuter#initialize(JZtxtcmdScript, boolean, List, Map, CharSequence)}
     * @since 2021-06
     */
    public Map<String, DataAccess.Variable<Object>> envar;

    JzTcMain(MainCmdLogging_ifc log, JZtxtcmdExecuter jzCmdExecuter){
      this.log = log;
      this.jzCmdExecuter = jzCmdExecuter;
      this.scriptThread = new JZtxtcmdThreadData();
      this.scriptLevel = new ExecuteLevel(this, this.scriptThread);
    }

        
        
    /**Returns the association to all script variables. The script variables can be changed
     * via this association. Note that change of script variables is a global action, which should not
     * be done for special requests in any subroutine.
     */
    public Map<String, DataAccess.Variable<Object>> scriptVariables(){ return this.scriptLevel.localVariables; }
 
    
    /**Returns the log interface from the environment class. */
    public MainCmdLogging_ifc log(){ return this.log; }
    
    public String getstdin() {
      String ret = null;
      try{
        byte[] line = new byte[100];
        int zline = System.in.read(line);
        ret = new String(line, 0, zline);
      } catch(IOException exc){
        throw new RuntimeException(exc);
      }
      return ret;
    }

    
    /**Capability to delay an execution, maybe for polling.
     * @param millisec
     * @return false if interrupted, true if full waiting
     * @since 2018-09 
     */
    public boolean sleep(int millisec)
    { boolean breaked = false;
      try{ Thread.sleep(millisec);} 
      catch (InterruptedException e){ breaked = true; }
      return !breaked;
    }

    
    
    public void setScriptVariable(String name, char type, Object content, boolean bConst) 
    throws IllegalAccessException{
      DataAccess.createOrReplaceVariable(this.scriptLevel.localVariables, name, type, content, bConst);
    }

    public DataAccess.Variable<Object> getScriptVariable(String name) throws NoSuchFieldException
    { return DataAccess.getVariable(this.scriptLevel.localVariables, name, true); }

    
    
    public Subroutine sub(CharSequence name){ return this.jzcmdScript.getSubroutine(name); }
    
    public String nextNr(){
      return Integer.toString(++this.nextNr_); 
    }

    public DataAccess.Variable<Object> removeScriptVariable(String name)
    { return this.scriptLevel.localVariables.remove(name);
      
    }

    

    public CharSequence currdir(){ return this.scriptLevel.currdir(); }

    public long calctime(){ return System.currentTimeMillis() - this.startmilli; }


    private Map<String, DataAccess.Variable<Object>> new_Variables(){
      return new TreeMap<String, DataAccess.Variable<Object>>();
      //return new IndexMultiTable<String, DataAccess.Variable<Object>>(IndexMultiTable.providerString);
    }

  } //class JzTcMain
  
  
  /**It is a List but also with Map
   * @author Hartmut Schorrig
   *
   */
  public static class ListMap extends ArrayList<Object> {
    /**
     * 
     */
    private static final long serialVersionUID = -8641598689693432170L;
    
    public Map<String, Object> valueMap;
    
    void add ( String name, Object data) {
      this.add(data);
      if(name !=null) {
        if(this.valueMap == null) { valueMap = new TreeMap<String, Object>(); }
        this.valueMap.put(name, data);
      }
    }
    
    //public Iterable<Object> foreach ( ) { return this.valueList; }
    
    /**The get is additional to the List, it accesses the valueMap with the given name as key.
     * @param name key for access.
     * @return proper entry to the name or null.
     * @throws If the get operation is called on a ListMap which has not a valueMap, an Exception is thrown. 
     *         It is a faulty in programming.
     */
    public Object get ( String name ) { return this.valueMap.get(name); }
    
  }
  
  
  
  
  private final JzTcMain acc;
  
  /**Variable for any exception while accessing any java resources. It is the $error variable of the script. */
  protected String accessError = null;
  
  /**Should break a loop, break statement. */
  public static final short kBreak = -1;
  
  /**Should return from subroutine, return statement. */ 
  public static final short kReturn = -2;
  
  /**Only internal designation.*/
  private static final short kFalse = -3;
  
  /**Has thrown an not catch exception. onerror statement not found. */
  public static final short kException = -5;
  
  /**Should continue. */
  public static final short kSuccess = 0;
  
  
  
  
  /**Instance for the main script part. */
  //Gen_Content genFile;

  //private boolean bScriptVariableGenerated;
  
  /**This instance is used to mark a return object as exception return. */
  private static CharSequence retException = new String("Exception");
  
  
  /**This is an instance used as marker for {@link #execSub(org.vishia.cmd.JZtxtcmdScript.Subroutine, Map, boolean, Appendable, File)}
   * for the argument args to use the script level in the sub routine variable instead.  
   */
  public static Map<String, DataAccess.Variable<Object>> useScriptLevel = new Map<String, DataAccess.Variable<Object>>(){

    @Override public int size(){  return 0; }

    @Override public boolean isEmpty() { return true; }

    @Override public boolean containsKey(Object key) {  return false; }

    @Override public boolean containsValue(Object value) { return false; }

    @Override public Variable<Object> get(Object key) { return null; }

    @Override public Variable<Object> put(String key, Variable<Object> value) { return null; }

    @Override public Variable<Object> remove(Object key) { return null; }

    @Override public void putAll(Map<? extends String, ? extends Variable<Object>> m) {}
   
    @Override public void clear() { }

    @Override public Set<String> keySet() { return null; }

    @Override public Collection<Variable<Object>> values() { return null; }

    @Override public Set<java.util.Map.Entry<String, Variable<Object>>> entrySet() { return null; }
    
  };
  
  /**Creates a JZcmdExecuter with possible writing exceptions in the output text.
   * 
   * @param log maybe null
   */
  public JZtxtcmdExecuter(MainCmdLogging_ifc log){
    this.acc = new JzTcMain(log, this);
  }
  
  
/**Creates a JZcmdExecuter with possible writing exceptions in the output text.
 */
public JZtxtcmdExecuter(){
  MainCmdLogging_ifc log = MainCmd.getLogging_ifc();  //maybe started with MainCmd
  if(log == null){
    log = new MainCmdLoggingStream(System.out);
  }
  this.acc = new JzTcMain(log, this);
}

  
/**Initializes the standard script variables and maybe executes the script level. All content before is removed.
 * Especially script variables from a previous usage of the instance are removed.
 * If you want to use a JZcmdExecuter more as one time with different scripts
 * but with the same script variables, one should call this routine one time on start,
 * and then {@link #execute(JZtxtcmdScript, boolean, boolean, Appendable)} with maybe several scripts,
 * which should not contain script variables, or one should call 
 * {@link #execSub(org.vishia.cmd.JZtxtcmdScript.Subroutine, Map, boolean, Appendable)}
 * with one of the subroutines in the given script.
 * 
 * @param genScriptArg Generation script in java-prepared form. It contains the building prescript
 *   for the script variables.
 * @param accessPrivate decision whether private and protected members from Java instances can be accessed.   
 * @param srcVariables
 * @param sCurrdir
 * @param bExecuteScriptLevel
 * @throws ScriptException
 */
public void initialize
( JZtxtcmdScript script
, boolean accessPrivate
, Map<String, DataAccess.Variable<Object>> srcVariables
, CharSequence sCurrdirArg
) 
throws ScriptException //, IllegalAccessException
{ 
  this.acc.scriptLevel.localVariables.clear();
  if(srcVariables !=null){
    for(Map.Entry<String, DataAccess.Variable<Object>> entry: srcVariables.entrySet()){
      DataAccess.Variable<Object> var = entry.getValue();
      try{ DataAccess.createOrReplaceVariable(this.acc.scriptLevel.localVariables, var.name(), var.type(), var.value(), var.isConst());
      } catch(IllegalAccessException exc){
        throw new JzScriptException("JZcmdExecuter.genScriptVariable - IllegalAccessException; " + exc.getMessage());
      }
    }
  }
  initialize_i(script, accessPrivate, sCurrdirArg);
}






/**Initializes the standard script variables and maybe executes the script level. All content before is removed.
 * Especially script variables from a previous usage of the instance are removed.
 * If you want to use a JZcmdExecuter more as one time with different scripts
 * but with the same script variables, one should call this routine one time on start,
 * and then {@link #execute(JZtxtcmdScript, boolean, boolean, Appendable)} with maybe several scripts,
 * which should not contain script variables, or one should call 
 * {@link #execSub(org.vishia.cmd.JZtxtcmdScript.Subroutine, Map, boolean, Appendable)}
 * with one of the subroutines in the given script.
 * 
 * @param script jzTc script in java-prepared form. It contains the building prescript
 *   for the script variables.
 * @param accessPrivate decision whether private and protected members from Java instances can be accessed.   
 * @param srcVariables
 * @param envar Variables from the Java environment used by reference. Preserve content on new initialized script.
 *              The new initialized script can use the before set content. 
 * @param sCurrdir
 * @param bExecuteScriptLevel
 * @throws ScriptException
 */
public void initialize
( JZtxtcmdScript script
, boolean accessPrivate
, List<DataAccess.Variable<Object>> srcVariables
, Map<String, DataAccess.Variable<Object>> envar
, CharSequence sCurrdirArg
) 
throws ScriptException //, IllegalAccessException
{ 
  this.acc.scriptLevel.localVariables.clear();
  if(srcVariables !=null){
    for(DataAccess.Variable<Object> var: srcVariables){
      try{ DataAccess.createOrReplaceVariable(this.acc.scriptLevel.localVariables, var.name(), var.type(), var.value(), var.isConst());
      } catch(IllegalAccessException exc){
        throw new JzScriptException("JZcmdExecuter.genScriptVariable - IllegalAccessException; " + exc.getMessage());
      }
    }
  }
  this.acc.envar = envar != null ? envar                                // use it immediately from the caller
                 : new TreeMap<String, DataAccess.Variable<Object>>();  // or create an empty one as container
  initialize_i(script, accessPrivate, sCurrdirArg);
}




/**Initializes the standard script variables and maybe executes the script level. All content before is removed.
 * Especially script variables from a previous usage of the instance are removed.
 * If you want to use a JZcmdExecuter more as one time with different scripts
 * but with the same script variables, one should call this routine one time on start,
 * and then {@link #execute(JZtxtcmdScript, boolean, boolean, Appendable)} with maybe several scripts,
 * which should not contain script variables, or one should call 
 * {@link #execSub(org.vishia.cmd.JZtxtcmdScript.Subroutine, Map, boolean, Appendable)}
 * with one of the subroutines in the given script.
 * 
 * @param genScriptArg Generation script in java-prepared form. It contains the building prescript
 *   for the script variables.
 * @param accessPrivate decision whether private and protected members from Java instances can be accessed.   
 * @param srcVariables
 * @param sCurrdir
 * @param bExecuteScriptLevel
 * @throws ScriptException
 */
public void initialize
( JZtxtcmdScript script
, boolean accessPrivate
, List<DataAccess.Variable<Object>> srcVariables
, CharSequence sCurrdirArg
) 
throws ScriptException //, IllegalAccessException
{ initialize(script, accessPrivate, srcVariables, null, sCurrdirArg);
}






/**Initializes the standard script variables and maybe executes the script level. All content before is removed.
 * Especially script variables from a previous usage of the instance are removed.
 * If you want to use a JZcmdExecuter more as one time with different scripts
 * but with the same script variables, one should call this routine one time on start,
 * and then {@link #execute(JZtxtcmdScript, boolean, boolean, Appendable)} with maybe several scripts,
 * which should not contain script variables, or one should call 
 * {@link #execSub(org.vishia.cmd.JZtxtcmdScript.Subroutine, Map, boolean, Appendable)}
 * with one of the subroutines in the given script.
 * 
 * @param genScriptArg Generation script in java-prepared form. It contains the building prescript
 *   for the script variables.
 * @param accessPrivate decision whether private and protected members from Java instances can be accessed.   
 * @param srcVariables
 * @param sCurrdir
 * @param bExecuteScriptLevel
 * @throws ScriptException
 */
public void initialize
( JZtxtcmdScript script
, boolean accessPrivate
, CharSequence sCurrdirArg
) 
throws ScriptException //, IllegalAccessException
{ 
  this.acc.scriptLevel.localVariables.clear();
  initialize_i(script, accessPrivate, sCurrdirArg);
}






private void initialize_i
( JZtxtcmdScript script
, boolean accessPrivate
, CharSequence sCurrdirArg
) 
throws ScriptException //, IllegalAccessException
{ 
  
  this.acc.bAccessPrivate = accessPrivate;
  this.acc.jzcmdScript = script;
  ExecuteLevel scriptLevel = this.acc.scriptLevel;
  if(this.acc.envar == null) {
    this.acc.envar = new TreeMap<String, DataAccess.Variable<Object>>();  // create an empty one as container
  }
  try{
    if(sCurrdirArg == null && scriptLevel.currdir == null){
      //get from the JVM environment respectively from the operation system.
      scriptLevel.currdir = new File("").getAbsoluteFile();  
      scriptLevel.sCurrdir = FileFunctions.getCanonicalPath(scriptLevel.currdir);
    } else if(sCurrdirArg !=null) {
      scriptLevel.changeCurrDir(sCurrdirArg);
    }
    File filescript = script == null ? null : script.fileScript;
    if(filescript !=null) { 
      String scriptfile = filescript.getName();
      CharSequence scriptdir = FileFunctions.normalizePath(FileFunctions.getDir(filescript));
      //File dirscript = FileFunctions.getDirectory(filescript).getCanonicalFile();
      DataAccess.createOrReplaceVariable(acc.scriptLevel.localVariables, "scriptfile", 'S', scriptfile, true);
      DataAccess.createOrReplaceVariable(acc.scriptLevel.localVariables, "scriptdir", 'S', scriptdir, true);
    }
    //do not replace variables which are set from outside.
    //if(scriptLevel.localVariables.get("error") == null){ DataAccess.createOrReplaceVariable(scriptLevel.localVariables, "error", 'A', accessError, true); }
    if(scriptLevel.localVariables.get("console") == null){ DataAccess.createOrReplaceVariable(scriptLevel.localVariables, "console", 'O', this.acc.log, true); }
    //DataAccess.createOrReplaceVariable(scriptLevel.localVariables, "nrElementInContainer", 'O', null);
    //Note: "text" will be replaced by the text output if given, argument -t:FILE
    
    
    { //creates a outText in any case. For harmonic software. It may be unused. Then garbage it. 
      DataAccess.Variable<Object> outText = scriptLevel.localVariables.get("text");
      Appendable outTextA = null;
      if(outText != null)  {
        Object outTextO = outText.value();
        if(outTextO instanceof StringFormatter) {
          this.acc.textline = (StringFormatter) outTextO;
        } else if(outTextO instanceof Appendable) {
          outTextA = (Appendable) outTextO;
        } else {
          throw new IllegalArgumentException("a given text variable should be either a StringFormatter or a Appendable");
        }
      } else {
        outTextA = this.acc.outFromCmdlineArg;
        if(outTextA == null) { outTextA = System.out; }
      }
      if(outTextA !=null) {
        boolean shouldClose = outTextA != System.out && outTextA instanceof Closeable;
        this.acc.textline = new StringFormatter(outTextA, shouldClose, "\n", 200);
      }
      DataAccess.createOrReplaceVariable(scriptLevel.localVariables, "text", 'A', this.acc.textline, true); 
    }
    
    
    if(scriptLevel.localVariables.get("out") == null)  {DataAccess.createOrReplaceVariable(scriptLevel.localVariables, "out", 'A', System.out, true); }
    if(scriptLevel.localVariables.get("err") == null)  {DataAccess.createOrReplaceVariable(scriptLevel.localVariables, "err", 'A', System.err, true); }
    if(scriptLevel.localVariables.get("null") == null) {DataAccess.createOrReplaceVariable(scriptLevel.localVariables, "null", 'O', null, true); }
    if(scriptLevel.localVariables.get("true") == null) {DataAccess.createOrReplaceVariable(scriptLevel.localVariables, "true", 'Q', true, true); }
    if(scriptLevel.localVariables.get("false") == null) {DataAccess.createOrReplaceVariable(scriptLevel.localVariables, "false", 'Q', false, true); }
    if(scriptLevel.localVariables.get("jzcmd") == null) {DataAccess.createOrReplaceVariable(scriptLevel.localVariables, "jzcmd", 'O', this.acc, true); }
    if(scriptLevel.localVariables.get("jztc") == null) {DataAccess.createOrReplaceVariable(scriptLevel.localVariables, "jztc", 'O', this.acc, true); }
    if(scriptLevel.localVariables.get("test") == null) {DataAccess.createOrReplaceVariable(scriptLevel.localVariables, "test", 'O', new JZtxtcmdTester(), true); }
    if(scriptLevel.localVariables.get("conv") == null) {DataAccess.createOrReplaceVariable(scriptLevel.localVariables, "conv", 'O', new Conversion(), true); }
    DataAccess.createOrReplaceVariable(scriptLevel.localVariables, "Math", 'C', Class.forName("java.lang.Math"), true);
    DataAccess.createOrReplaceVariable(scriptLevel.localVariables, "Num", 'C', Class.forName("org.vishia.util.Num"), true);
    DataAccess.createOrReplaceVariable(scriptLevel.localVariables, "System", 'C', Class.forName("java.lang.System"), true);
    //Hint: from view of JZtxtcmd it should be named "FileSystem". Only from view of the java class it is FileFunctions:
    DataAccess.createOrReplaceVariable(scriptLevel.localVariables, "FileSystem", 'C', Class.forName("org.vishia.util.FileFunctions"), true);
    DataAccess.createOrReplaceVariable(scriptLevel.localVariables, "StringFunctions", 'C', Class.forName("org.vishia.util.StringFunctions"), true);
    if(scriptLevel.localVariables.get("nextNr") == null){DataAccess.createOrReplaceVariable(scriptLevel.localVariables, "nextNr", 'O', new NextNr(), true); }
    Method mCurrdir = this.acc.getClass().getMethod("nextNr");
    DataAccess.ObjMethod objMethod = new DataAccess.ObjMethod(mCurrdir, this.acc);
    DataAccess.createOrReplaceVariable(scriptLevel.localVariables, "nextnr", 'M', objMethod, true);
    //
  } catch(IllegalAccessException exc){
    throw new JzScriptException("JZcmdExecuter.genScriptVariable - IllegalAccessException; " + exc.getMessage());
  } catch(Exception exc){
    throw new JzScriptException("JZcmdExecuter.genScriptVariable - unexpected exception; " + exc.getMessage());
  }
  if(script !=null) {
    //generate all variables in this script:
    executeScriptLevel(script, sCurrdirArg);
  }
  //setCurrdirScript(sCurrdirArg);
  //return scriptLevel.localVariables;
}

  
  
/**Stores the script and executes the script level to generate the script level variables, especially the script variables were calculated yet. 
 * The {@link #initialize(JZtxtcmdScript, boolean, Map, CharSequence)} may had invoked before, then the standard variables are created already.
 * Any additional user variable can be stored also. If the {@link ExecuteLevel#localVariables} are empty, 
 * then {@link #initialize(JZtxtcmdScript, boolean, Map, CharSequence)} is called with the script instead.
 * <br><br>
 * This method should be used. For example the script level can be used to {@link #execSub(org.vishia.cmd.JZtxtcmdScript.Subroutine, Map, boolean, Appendable, File)}
 * for a sub routine which does not use script level variables, for example to set parameter.
 * Note: The standard script variables are set with {@link #initialize(JZtxtcmdScript, boolean, Map, String, boolean)} already.
 * 
 * @param script The given script
 * @throws ScriptException
 */
public void  executeScriptLevel(JZtxtcmdScript script, CharSequence sCurrdir) throws ScriptException //, IllegalAccessException
{ boolean bscriptInitialized = checkInitialize(script, true, sCurrdir);
  if(!bscriptInitialized && sCurrdir !=null) {
    try{ this.acc.scriptLevel.changeCurrDir(sCurrdir);
    } catch(IllegalAccessException exc) { throw new JzScriptException(exc); }
  }

  short ret = this.acc.scriptLevel.execute(script.scriptClass, this.acc.textline, 0, this.acc.scriptLevel.localVariables, -1);
  if(ret == kException){
    Throwable cause = this.acc.scriptThread.exception.getCause();
    String sException;
    if(cause == null) { sException = this.acc.scriptThread.exception.toString(); }
    else { sException = cause.toString() + " => s" + this.acc.scriptThread.exception.toString(); }
    throw new JzScriptException(sException, this.acc.scriptThread.excSrcfile, this.acc.scriptThread.excLine, this.acc.scriptThread.excColumn);
  }
} 



/**Initializes respectively calculates a class newly.
 * @param sClazz Name
 * @return The executeLevel, able to use for subroutines or to get the variables of the class.
 * @throws ScriptException
 */
public ExecuteLevel execute_Scriptclass(String sClazz) throws ScriptException 
{
  JZtxtcmdScript.JZcmdClass clazz1 = this.acc.jzcmdScript.getClass(sClazz);
  if(clazz1 == null) throw new IllegalArgumentException("class in script not found: " + sClazz);
  ExecuteLevel level = new ExecuteLevel(acc, null, this.acc.jzcmdScript.scriptClass, this.acc.scriptThread, this.acc.scriptLevel, null);
  short ret = level.execute(clazz1, this.acc.textline, 0, level.localVariables, -1);
  if(ret == kException){
    if(acc.scriptThread.exception instanceof ScriptException){
      throw (ScriptException)acc.scriptThread.exception; 
    } else {
      CharSequence text = Assert.exceptionInfo("Exception in the script, ", this.acc.scriptThread.exception, 0, 20);
      throw new JzScriptException(text.toString(), this.acc.scriptThread.excSrcfile, this.acc.scriptThread.excLine, this.acc.scriptThread.excColumn);
    }
  }
  return level;
}
  
/**Executes the code of a class in a script without checking the whole script.
 * If the executer was not initialized or initialized with another script, it is possible. 
 * The script class should only define simple variables then without dependencies.
 * @param clazz the class in any Script
 * @return The executeLevel, able to use for subroutines or to get the variables of the class.
 * @throws ScriptException
 */
public ExecuteLevel execute_Scriptclass(JZtxtcmdScript.JZcmdClass clazz) throws ScriptException 
{
  ExecuteLevel level = new ExecuteLevel(acc, null, clazz, this.acc.scriptThread, this.acc.scriptLevel, null);
  short ret = level.execute(clazz, this.acc.textline, 0, level.localVariables, -1);
  if(ret == kException){
    if(acc.scriptThread.exception instanceof ScriptException){
      throw (ScriptException)acc.scriptThread.exception; 
    } else {
      CharSequence text = Assert.exceptionInfo("Exception in the script, ", this.acc.scriptThread.exception, 0, 20);
      throw new JzScriptException(text.toString(), this.acc.scriptThread.excSrcfile, this.acc.scriptThread.excLine, this.acc.scriptThread.excColumn);
    }
  }
  return level;
}
  
  /**Returns the association to all script variables. The script variables can be changed
   * via this association. Note that change of script variables is a global action, which should not
   * be done for special requests in any subroutine.
   */
  public Map<String, DataAccess.Variable<Object>> scriptVariables(){ return this.acc.scriptLevel.localVariables; }
  
  /**Returns the script level instance. This instance is created with the constructor of this class (a composite).
   * The {@link ExecuteLevel#localVariables} of the script level are the script variables. 
   * They are filled with the given script on call of {@link #initialize(JZtxtcmdScript, boolean, Map, String, boolean)}
   * or if {@link #execute(JZtxtcmdScript, boolean, boolean, Appendable, String)} was called.
   */
  public ExecuteLevel scriptLevel(){ return this.acc.scriptLevel; }
  
  
  /**Clears the instance. A newly use will create the script variables in any case from the given script.
   * 
   */
  public void reset(){
    //bScriptVariableGenerated = false;
    this.acc.scriptLevel.localVariables.clear();
    this.acc.jzcmdScript = null;
  }
  
  
  /**Aborts all {@link CmdExecuter} for operation system calls in all threads of this.
   * @return true if at least one was aborted.
   */
  public boolean abortCmdExecution() {
    boolean bAborted = false;
    for(CmdExecuter cmd: this.acc.runningCmdExecuter) {
      if(cmd.abortCmd()) {
        bAborted = true;
      }
    }
    return bAborted;
  }
  
  
  
  
  private boolean checkInitialize(JZtxtcmdScript script, boolean accessPrivate, Map<String, DataAccess.Variable<Object>> args, CharSequence sCurrdir) 
  throws ScriptException //Throwable
  { boolean bscriptInitialized = script !=null && this.acc.jzcmdScript != script; //another script given.
    if(bscriptInitialized){
      initialize(script, accessPrivate, args, sCurrdir);
    }
    return bscriptInitialized;
  }
  
  
  private boolean checkInitialize(JZtxtcmdScript script, boolean accessPrivate, List< DataAccess.Variable<Object>> args, CharSequence sCurrdir) 
  throws ScriptException //Throwable
  { boolean bscriptInitialized = script !=null && this.acc.jzcmdScript != script; //another script given.
    if(bscriptInitialized){
      initialize(script, accessPrivate, args, sCurrdir);
    }
    return bscriptInitialized;
  }
  
  
  private boolean checkInitialize(JZtxtcmdScript script, boolean accessPrivate, CharSequence sCurrdir) 
  throws ScriptException //Throwable
  { boolean bscriptInitialized = script !=null && this.acc.jzcmdScript != script; //another script given.
    if(bscriptInitialized){
      initialize(script, accessPrivate, sCurrdir);
    }
    return bscriptInitialized;
  }
  
  
  private boolean checkInitialize(JZtxtcmdScript script, boolean accessPrivate, Map<String, DataAccess.Variable<Object>> args, File currdir) 
    throws ScriptException //Throwable
  { String sCurrdir = currdir == null ? null: currdir.getPath();
    return checkInitialize(script, accessPrivate, args, sCurrdir);
  }
  
  
  
  
  /**Executes the given script. See {@link #execute(JZtxtcmdScript, boolean, boolean, Appendable, String)}
   * @param given data as input.
   */
  public void execute(
      JZtxtcmdScript script
      , boolean accessPrivate
      , boolean bWaitForThreads
      , Appendable outFromCmdlineArg
      , Map<String, DataAccess.Variable<Object>> data
      , String sCurrdir
      ) 
  throws ScriptException //, IllegalAccessException //, Throwable
  { //boolean bScriptLevelShouldExecuted = checkScript(script);
    this.acc.outFromCmdlineArg = outFromCmdlineArg;
    boolean bscriptInitialized = this.acc.jzcmdScript != script;
    if(bscriptInitialized){
      checkInitialize(script, true, data, sCurrdir);
    }
    execute_i(script, accessPrivate, bWaitForThreads, sCurrdir, bscriptInitialized);
  }    
    
 
  
  
  /**Executes the given script. See {@link #execute(JZtxtcmdScript, boolean, boolean, Appendable, String)}
   * @param data data as input.
   */
  public void execute(
      JZtxtcmdScript script
      , boolean accessPrivate
      , boolean bWaitForThreads
      , Appendable outFromCmdlineArg
      , List<DataAccess.Variable<Object>> data
      , String sCurrdir
      ) 
  throws ScriptException //, IllegalAccessException //, Throwable
  { //boolean bScriptLevelShouldExecuted = checkScript(script);
    this.acc.outFromCmdlineArg = outFromCmdlineArg;
    boolean bscriptInitialized = this.acc.jzcmdScript != script;
    if(bscriptInitialized){
      checkInitialize(script, true, data, sCurrdir);
    }
    execute_i(script, accessPrivate, bWaitForThreads, sCurrdir, bscriptInitialized);
  }    
    
  
  /**Executes the given script.
   * @param script The script. If this instance was used with the same script before, the calculated script variables are not refreshed.
   *   That may be important if script variables are produced by running sub routines before. Use {@link #reset()} if that is not desired.
   *   If the script is another and after {@link #reset()}, previous script variables are removed firstly, and the script variables 
   *   and script statements are firstly processed.  
   * @param accessPrivate 
   * @param bWaitForThreads should set to true if it is a command line invocation of Java,
   *   the exit should wait for all threads. May set to false if calling inside a long running application.
   * @param out Any output for text generation using <code><+>text output<.>. 
   *   It is used also for direct text output <:>text<.>.
   * @param sCurrdir maybe null, the current directory. If given, it is set in any case for the script level.
   * @throws IOException only if out.append throws it.
   * @throws IllegalAccessException if a const scriptVariable are attempt to modify.
   */
  public void execute(
      JZtxtcmdScript script
      , boolean accessPrivate
      , boolean bWaitForThreads
      , Appendable outFromCmdlineArg
      , String sCurrdir
      ) 
  throws ScriptException //, IllegalAccessException //, Throwable
  { //boolean bScriptLevelShouldExecuted = checkScript(script);
    this.acc.outFromCmdlineArg = outFromCmdlineArg;
    boolean bscriptInitialized = this.acc.jzcmdScript != script;
    if(bscriptInitialized){
      checkInitialize(script, true, sCurrdir);
    }
    execute_i(script, accessPrivate, bWaitForThreads, sCurrdir, bscriptInitialized);
  }    
    
  
  
  
  /**Inner routine to process the execution after initializing the script. This routine is called from all other.
   * @param script The compiled script.
   * @param accessPrivate true if private access to any given Java data inside a script.
   * @param bWaitForThreads true then wait for finish all threads to exit
   * @param outFromCmdline If {@link System#out} then it can be overridden by internal given <code>text = path/to/file</code>
   *   It is set by cmd line invocation with -o=file with opend file. 
   *   If <code>text = path/to/file</code> is given by initializing the script, the given file will be closed and the script determined 
   * @param sCurrdir
   * @param bscriptInitialized
   * @throws ScriptException
   */
  void execute_i(
      JZtxtcmdScript script
      , boolean accessPrivate
      , boolean bWaitForThreads
      , String sCurrdir
      , boolean bscriptInitialized
      ) 
  throws ScriptException //, IllegalAccessException //, Throwable
  {  
    
    this.acc.bAccessPrivate = accessPrivate;
    //this.data = userData;
    short ret;
    //try
    {
      assert(acc.textline !=null); //because it is handled in initialize
      if(acc.textline == null) { //that is if a 'text = path' is not part of script variables.
        //then a 'text = path' is not given in the script variables.
        if(acc.outFromCmdlineArg == null) {
          this.acc.outFromCmdlineArg = System.out; //default
        }
        boolean bShouldCloseOutFromCmdline = (acc.outFromCmdlineArg != System.out) && (acc.outFromCmdlineArg instanceof Closeable);
        StringFormatter outFormatter = new StringFormatter(acc.outFromCmdlineArg, bShouldCloseOutFromCmdline, null, 200);
        this.acc.textline = outFormatter;
        try{
          this.acc.setScriptVariable("text", 'A', this.acc.textline, true);  //NOTE: out maybe null
        } catch(IllegalAccessException exc){ throw new JzScriptException("JZcmd.executer - IllegalAccessException; " + exc.getMessage()); }
        
      }
      this.acc.outFromCmdlineArg = null; //further setting of 'text =' closes the current textline and opens a new one.
      
    }
    if(!bscriptInitialized && sCurrdir !=null) {
      try {acc.scriptLevel.changeCurrDir(sCurrdir);
      } catch(IllegalAccessException exc) { throw new JzScriptException(exc); }
    }
    
    
    JZtxtcmdScript.Subroutine mainRoutine = this.acc.jzcmdScript.getMain();
    if(mainRoutine == null) {
      throw new IllegalArgumentException("main routine not found"); 
    }
    //needs all scriptVariable:
    JZtxtcmdScript.JZcmdClass mainClass = (JZtxtcmdScript.JZcmdClass)mainRoutine.parentList;

    ExecuteLevel execFile = new ExecuteLevel(acc, mainRoutine, mainClass/*this.acc.jzcmdScript.scriptClass*/, this.acc.scriptThread, this.acc.scriptLevel, null);
    if(acc.jzcmdScript.checkJZcmdXmlFile !=null) {
      CharSequence sFilecheckXml;
      try { sFilecheckXml = this.acc.scriptLevel.evalString(acc.jzcmdScript.checkJZcmdXmlFile);
      } catch (Exception exc) { throw new JzScriptException("JZcmd.execute - String eval error on checkJZcmd; "
          , this.acc.jzcmdScript.checkJZcmdXmlFile.srcFile, this.acc.jzcmdScript.checkJZcmdXmlFile.srcLine, this.acc.jzcmdScript.checkJZcmdXmlFile.srcColumn ); 
      }
      SimpleXmlOutputter xmlOutputter = new SimpleXmlOutputter();
      try{
        OutputStreamWriter xmlWriter = new OutputStreamWriter(new FileOutputStream(sFilecheckXml.toString()));
        xmlOutputter.write(xmlWriter, this.acc.jzcmdScript.xmlSrc);
        xmlWriter.close();
        this.acc.jzcmdScript.xmlSrc = null;  //can be garbaged.
      } catch(IOException exc){ throw new JzScriptException(exc); }
      
    }
    if(acc.jzcmdScript.checkJZcmdFile !=null){
      CharSequence sFilecheck;
      try { sFilecheck = execFile.evalString(acc.jzcmdScript.checkJZcmdFile);
      } catch (Exception exc) { throw new JzScriptException("JZcmd.execute - String eval error on checkJZcmd; "
          , this.acc.jzcmdScript.checkJZcmdFile.srcFile, this.acc.jzcmdScript.checkJZcmdFile.srcLine, this.acc.jzcmdScript.checkJZcmdFile.srcColumn ); 
      }
      File filecheck = new File(sFilecheck.toString());
      try{
        Writer writer = new FileWriter(filecheck);
        this.acc.jzcmdScript.writeStruct(writer);
        writer.close();
      } catch(IOException exc){ throw new JzScriptException("JZcmd.execute - File error on checkJZcmd; " + filecheck.getAbsolutePath()); }
    }
    //return execute(execFile, contentScript, true);
    this.acc.startmilli = System.currentTimeMillis();
    this.acc.startnano = System.nanoTime();
    if(mainRoutine !=null) {
      ret = execFile.execute(mainRoutine.statementlist, this.acc.textline, 0, execFile.localVariables, -1);
    } else {
      System.out.println("JZcmdExecuter - main routine not found.");
      ret = 0;
    }
    if(bWaitForThreads){
      boolean bWait = true;
      while(bWait){
        synchronized(acc.threads){
          bWait = this.acc.threads.size() !=0;
          if(bWait){
            try{ this.acc.threads.wait(1000); }
            catch(InterruptedException exc){}
          }
        }
      }
    }
    if(acc.textline !=null) {
      try{ this.acc.textline.close(); } 
      catch(IOException exc){ throw new RuntimeException("unexpected exception on close", exc); }
    }
    
    //catch(Exception exc){
    //  ret = kException;
    //}
    if(ret == kException){
      if(acc.scriptThread.exception instanceof ScriptException){
        throw (ScriptException)acc.scriptThread.exception; 
      } else {
        CharSequence text = Assert.exceptionInfo("Exception in the script, ", this.acc.scriptThread.exception, 0, 20);
        throw new JzScriptException(text.toString(), this.acc.scriptThread.excSrcfile, this.acc.scriptThread.excLine, this.acc.scriptThread.excColumn);
      }
    }
  }
  
  
  
  
  /**Executes a subroutine. See {@link #execSub(org.vishia.cmd.JZtxtcmdScript.Subroutine, List, boolean, Appendable, File, CmdExecuter)}.
   * This variation should be used only if the args are given for example and especially as Variables in a Map.
   * @param script maybe null. If null the last given script will be used. If not null and not equal the given script this script
   * will be newly used. {@link #initialize(JZtxtcmdScript, boolean, CharSequence)} will be invoked then.
   * @param args given as Map. It is converted to a list firstly. 
   */
  public Map<String, DataAccess.Variable<Object>> execSub(JZtxtcmdScript script, String name, Map<String, DataAccess.Variable<Object>> args
    , boolean accessPrivate, Appendable out, File currdir) 
  throws ScriptException //Throwable
  { checkInitialize(script, true, null, currdir);
    JZtxtcmdScript.Subroutine statement = this.acc.jzcmdScript.getSubroutine(name);
    if( statement == null) throw new JzScriptException("Subroutine not found: " + name, this.acc.jzcmdScript.fileScript.getAbsolutePath(), 0,0); 
    return execSub(statement, args, accessPrivate, out, currdir);
  }
    
    
  /**Executes a subroutine. It invokes {@link #execSub(org.vishia.cmd.JZtxtcmdScript.Subroutine, List, boolean, Appendable, File, CmdExecuter)}
   *   if the subroutine is found per name in the given script. 
   * @param script The script. If this instance was used with the same script before, the calculated script variables are not refreshed.
   *   That may be important if script variables are produced by running sub routines before. Use {@link #reset()} if that is not desired.
   *   If the script is another and after {@link #reset()}, previous script variables are removed firstly, and the script variables 
   *   and script statements are firstly processed.  
   * @param name of the sub routine in the given script. Maybe in form "theclass.thesub"-  
   * @param args given as Map. It is converted to a list firstly. 
   */
  public Map<String, DataAccess.Variable<Object>> execSub(JZtxtcmdScript script, String name, List<DataAccess.Variable<Object>> args
    , boolean accessPrivate, Appendable out, File currdir) 
  throws ScriptException //Throwable
  { checkInitialize(script, true, null, currdir);
    JZtxtcmdScript.Subroutine statement = this.acc.jzcmdScript.getSubroutine(name);
    if( statement == null) throw new JzScriptException("Subroutine not found: " + name, script.fileScript.getAbsolutePath(), 0,0); 
    return execSub(statement, args, accessPrivate, out, currdir, null);
  }
    
    
  
  
  /**Executes a subroutine. See {@link #execSub(org.vishia.cmd.JZtxtcmdScript.Subroutine, List, boolean, Appendable, File, CmdExecuter)}.
   * This variation should be used only if the args are given for example and especially as Variables in a Map.
   * @param args given as Map. It is converted to a list firstly. 
   */
  public Map<String, DataAccess.Variable<Object>> execSub(JZtxtcmdScript.Subroutine statement, Map<String, DataAccess.Variable<Object>> args
      , boolean accessPrivate, Appendable out, File currdir) ////
  throws ScriptException //Throwable
  {
    final List<DataAccess.Variable<Object>> arglist;
    if(args !=null){
      arglist = new LinkedList<DataAccess.Variable<Object>>();
      for(Map.Entry<String, DataAccess.Variable<Object>> entry: args.entrySet()){
        arglist.add(entry.getValue());
      }
    } else {
      arglist = null;
    }
    return execSub(statement, arglist, accessPrivate, out, currdir, null);
  }
  
  
  
  
  /**Executes the given sub routine invoked from any user application. 
   * The time measurements {@link #startmilli} and {@link #startnano} starts newly. This is the core routine.
   * @param statement The subroutine in a script. The script of this subroutine is used as new script.
   *   If this instance was used with the same script before, the calculated script variables are not refreshed.
   *   That may be important if script variables are produced by running sub routines before. Use {@link #reset()} if that is not desired.
   *   If the script is another and after {@link #reset()}, previous script variables are removed firstly, and the script variables 
   *   and script statements are firstly processed.  
   * @param args Some variables which are stored as argument values. Use {@link #useScriptLevel} to advertise that no extra level should be used.
   *   Then all changed and created variables are part of the script level.
   * @param accessPrivate flag for access private Java data.
   * @param out Any output for text generation using <code><+>text output<.>. 
   *   It is used also for direct text output <:>text<.>.
   * @param currdir if not null, then this directory is used as {@link ExecuteLevel#changeCurrDir(CharSequence)} for this subroutine.
   * @return the variables which are stored in a definition of return variables in the sub routine, or null if no such variables were built.
   * @throws Throwable 
   * @throws IOException
   */
  public Map<String, DataAccess.Variable<Object>> execSub(JZtxtcmdScript.Subroutine statement, List<DataAccess.Variable<Object>> arglist
      , boolean accessPrivate, Appendable out, File currdir, CmdExecuter cmdExecuter) ////
  throws ScriptException //Throwable
  {
    //if(acc.jzcmdScript == null) throw new IllegalArgumentException("jzcmdScript missing, you should invoke \"initialize(script, false, null, null);\" before call execSub(...)");
    boolean bscriptInitialized = checkInitialize(statement.theScript, true, null, currdir);
    final ExecuteLevel level = this.acc.scriptLevel.levelForSubroutine(statement);  //uses the script variable if subroutine uses the locals. //new ExecuteLevel(acc, this.acc.jzcmdScript.scriptClass, this.acc.scriptThread, this.acc.scriptLevel, null);
    if(cmdExecuter !=null) {
      level.setCmdExecuter(cmdExecuter);
      if(currdir !=null) {
        cmdExecuter.setCurrentDir(currdir);
      }
      //TODO what about environment variables of the current level. Test it!
    }
    if(out !=null) {
      StringFormatter outFormatter = new StringFormatter(out, out instanceof Closeable, "\n", 200);
      this.acc.textline = outFormatter;
      try{ this.acc.setScriptVariable("text", 'A', out, true); //It is the default output for <:>...<.>
      } catch(IllegalAccessException exc) { throw new JzScriptException(exc); }
    }
    if(!bscriptInitialized && currdir !=null){
      try { level.changeCurrDir(currdir.getPath());
      } catch(IllegalAccessException exc) { throw new JzScriptException(exc); }
    }
    //Executes the statements of the sub routine:
    this.acc.startmilli = System.currentTimeMillis();
    this.acc.startnano = System.nanoTime();
    short ret;
    try{ 
      ret = this.acc.scriptLevel.exec_Subroutine(statement, level, null, arglist, this.acc.textline, 1, 0);
    } catch(Exception exc) {
      throw new JzScriptException(exc.getMessage(), this.acc.scriptThread.excSrcfile, this.acc.scriptThread.excLine, this.acc.scriptThread.excColumn);
    }
    if(acc.textline !=null) {
      try{ this.acc.textline.close(); } 
      catch(IOException exc){ throw new RuntimeException("unexpected exception on close", exc); }
    }
    if(ret == kReturn || ret == kBreak){ ret = kSuccess; }
    if(ret == kException){
      //The primary exception is not the point of interest because a script is executed.
      //To get the detail message set a breakpoint here.
      //wrong: throw this.acc.scriptThread.exception;
      throw new JzScriptException(acc.scriptThread.exception.getMessage(), this.acc.scriptThread.excSrcfile, this.acc.scriptThread.excLine, this.acc.scriptThread.excColumn);
    }
    DataAccess.Variable<Object> ret2 = level.localVariables.get("return");
    if(ret2 !=null && ret2.value() instanceof Map) {
      @SuppressWarnings("unchecked") 
      Map<String, DataAccess.Variable<Object>> ret3 = (Map<String, DataAccess.Variable<Object>>) ret2.value();
      return ret3;
    } else {
      return null;  //no return statement.
    }
  }
  
  
  
  
  /**Sets a script variable for a given initialized script.
   * The routine {@link DataAccess.createOrReplaceVariable()} will be called 
   * with the given {@link JzTcMain#scriptLevel}.{@link ExecuteLevel#localVariables}  
   * @param name Name of the script variable
   * @param type One of {@link DataAccess.Variable#type()}
   * @param content
   * @param bConst
   * @throws IllegalAccessException
   * @deprecated It is deprecated now. On start of a script or a subroutine from a script a List or Map of additional script variables
   *   can be provided. This is a safe but other way. The script variable created with this routine may be removed if the script
   *   was newly initialized. Only if the following order of execution is given, it is proper: <pre>
   *   initialize(script, accessPrivate, currDir);  //will initialize
   *   setScriptVariable(name, type, content, const);
   *   execute(script, ....);
   *   </pre>
   *   The JZcmdExecuter instance should be firstly initialize with the same script.  
   */
  @Deprecated public void setScriptVariable(String name, char type, Object content, boolean bConst) 
  throws IllegalAccessException{
    DataAccess.createOrReplaceVariable(acc.scriptLevel.localVariables, name, type, content, bConst);
  }

  
  
  public DataAccess.Variable<Object> getScriptVariable(String name) throws NoSuchFieldException
  { return DataAccess.getVariable(acc.scriptLevel.localVariables, name, true); }

  
  public DataAccess.Variable<Object> removeScriptVariable(String name)
  { return this.acc.scriptLevel.localVariables.remove(name);
    
  }

  
  
  


  
  
  /**Wrapper to generate a script with specified localVariables.
   * A new Wrapper is created on any subroutine level. It is used in a {@link CalculatorExpr#calcDataAccess(Map, Object...)} 
   * to generate an expression independent of an environment.
   *
   */
  public final static class ExecuteLevel implements ScriptContext, FilePath.FilePathEnvAccess, Closeable
  {
    final JzTcMain jzcmdMain;
    /**Not used yet. Only for debug! */
    public final ExecuteLevel parent;
    
    /**Counts nesting of {@link #execute(org.vishia.cmd.JZtxtcmdScript.StatementList, StringFormatter, int, boolean, Map, int)}
     * 1->0: leaf execution of this level, close {@link #cmdExecuter}    */
    int ctNesting = 0;
    
    final JZtxtcmdThreadData threadData;
    
    
    final JZtxtcmdScript.JZcmdClass jzClass;
    
    final JZtxtcmdScript.Subroutine subRoutine;
    
    /**The current directory of this level. It is an absolute normalized but not Unix-canonical path. 
     * Note that a Unix-canonical path have to resolved symbolic links. */
    public File currdir;
    
    /**The current directory of this level. It is an absolute normalized but not Unix-canonical path. 
     * Note that a Unix-canonical path have to resolved symbolic links. */
    String sCurrdir;
    
    /**Flag, write error in the current output if set to true. */
    public boolean bWriteErrorInOutput;
    

    /**Generated content of local variables in this nested level including the {@link JzTcMain#scriptLevel}.{@link ExecuteLevel#localVariables}.
     * The variables are type invariant on language level. The type is checked and therefore 
     * errors are detected on runtime only. */
    public final Map<String, DataAccess.Variable<Object>> localVariables;
    
    
    /**Initialize firstly on demand. Don't close to speed up following cmd invocations.
     * It is necessary to have one executer per level because different environment variables.
     */
    public CmdExecuter cmdExecuter;

    //private boolean bSetSkipSpaces;
    
    /**Used while a for-container loop runs. */
    private boolean bForHasNext;
    
    private boolean debug_dataAccessArguments;
    
    /**The error level which is returned from an operation system cmd invocation.
     * It is used for the {@link #execCmdError(org.vishia.cmd.JZtxtcmdScript.Onerror)}.
     */
    public int cmdErrorlevel = 0;
    
    
    /**Constructs data for a local execution level.
     * @param parentVariables if given this variable are copied to the local ones.
     *   They contains the script variables too. If not given (null), only the script variables
     *   are copied into the {@link #localVariables}. Note that subroutines do not know the
     *   local variables of its calling routine! This argument is only set if nested statement blocks
     *   are to execute. 
     */
    protected ExecuteLevel(JzTcMain acc, JZtxtcmdScript.Subroutine subRoutine, JZtxtcmdScript.JZcmdClass jzClass, JZtxtcmdThreadData threadData, ExecuteLevel parent
        , Map<String, DataAccess.Variable<Object>> parentVariables)
    { this.jzcmdMain = acc;
      this.parent = parent;
      this.ctNesting = parent == null ? 0 : parent.ctNesting+1;  //should not close #cmdExecuter if not the root.
      this.jzClass = jzClass;
      this.subRoutine = subRoutine;
      this.threadData = threadData;
      if(parent !=null) {
        this.currdir = parent.currdir;
        this.sCurrdir = parent.sCurrdir;
      }
      this.localVariables = acc.new_Variables();
      if(parentVariables != null) {
        this.localVariables.putAll(parentVariables);  //use the same if it is not a subText, only a 
      } else if(acc.scriptLevel !=null) {
        for(Map.Entry<String, DataAccess.Variable<Object>> e: acc.scriptLevel.localVariables.entrySet()){
          DataAccess.Variable<Object> var = e.getValue();
          String key = e.getKey();
          if(key.equals("scriptdir")){           //scriptfile either from subroutine if given, or ...
            File scriptFile = subRoutine !=null ? new File(subRoutine.srcFile) : 
                              jzClass !=null ? new File(jzClass.srcFile) : acc.jzcmdScript.fileScript ;
            CharSequence scriptdir = FileFunctions.normalizePath(FileFunctions.getDir(scriptFile));
            //create a new scriptdir and scriptfile variable
            DataAccess.Variable<Object> var2 = new DataAccess.Variable<Object>('S', "scriptdir", scriptdir, true);
            this.localVariables.put("scriptdir", var2);
            //
            String scriptname = scriptFile.getName();
            DataAccess.Variable<Object> varFile = new DataAccess.Variable<Object>('S', "scriptfile", scriptname, true);
            this.localVariables.put("scriptfile", varFile);
          }
          else if(key.equals("scriptfile")){
            //do nothing, already done with scriptdir handling
          } 
          else if(var.isConst()){
            //Scriptvariables which are designated as const cannot be changed in the sub level.
            //Therefore it is enough to refer it.
            this.localVariables.put(key, var);
          } 
          else {
            //build a new independent variable, which can be changed in the sub level.
            DataAccess.Variable<Object> var2 = new DataAccess.Variable<Object>(var);
            this.localVariables.put(key, var2);
          }
        }
      }
      try{ 
        //create a new variable to refer jzcmdsub:
        DataAccess.createOrReplaceVariable(this.localVariables,  "jzcmdsub", 'O', this, true);
        DataAccess.createOrReplaceVariable(this.localVariables,  "jztcsub", 'O', this, true);
        //use the existent variable threadData.error to refer here:
        this.localVariables.put("error", threadData.error);
      } catch(IllegalAccessException exc){ throw new RuntimeException(exc); }
    }

    
    
    /**Constructs data for the script execution level.
     */
    protected ExecuteLevel(JzTcMain acc, JZtxtcmdThreadData threadData)
    { this(acc, null, null, threadData, null, null);
    }
    
    
    /**Use the given CmdExecuter especially for a {@link JZtxtcmdExecuter#execSub(org.vishia.cmd.JZtxtcmdScript.Subroutine, List, boolean, Appendable, File)}
     * after {@link #levelForSubroutine(org.vishia.cmd.JZtxtcmdScript.Subroutine)}.
     * @param executer may be opened, should not be in process, will be used then.
     */
    public void setCmdExecuter(CmdExecuter executer) {
      if(this.cmdExecuter !=null) { this.cmdExecuter.abortCmd(); this.cmdExecuter.close(); }
      this.cmdExecuter = executer;
    }

    public JzTcMain executer(){ return this.jzcmdMain; }
    
    
    public JZtxtcmdEngine scriptEngine(){ return this.jzcmdMain.jzcmdScript.getEngine(); }
    
    
    /**Returns the log interface from the environment class. */
    public MainCmdLogging_ifc log(){ return this.jzcmdMain.log; }
    
    
    
    public void setLocalVariable(String name, char type, Object content, boolean isConst) 
    throws IllegalAccessException {
      DataAccess.createOrReplaceVariable(this.localVariables, name, type, content, isConst);
    }
    
    
    
    /**Executes an inner script part maybe with a new level of nested local variables.
     * If the contentScript does not contain any variable definition 
     * (see @link {@link JZtxtcmdScript.StatementList#bContainsVariableDef}) then this level is used,
     * it prevents a non necessary instantiation of an {@link ExecuteLevel} and copying of local variables. 
     * If new variables are to build in the statements, a new instance of {@link ExecuteLevel} is created
     * and all variables of this.{@link #localVariables} are copied to it. It means the nested level
     * knows the same variables but it can add new variables without effect to this level. Changing
     * of existing variables effects all outer levels. That is necessary, an outer-defined variable can be changed
     * in an inner level. That is guaranteed because only the reference is changed.
     * @param contentScript of the inner level.
     * @param out The main output
     * @param bContainerHasNext only used for for-statement
     * @return an error hint.
     * @throws IOException
     */
    public short executeNewlevel ( JZtxtcmdScript.StatementList contentScript, final StringFormatter out, int indentOut
        , int nDebug) 
    throws Exception { 
      short result;
      if(contentScript.bContainsVariableDef){
        final ExecuteLevel level = new ExecuteLevel(this.jzcmdMain, this.subRoutine, this.jzClass, this.threadData, this, this.localVariables);
        result = level.execute(contentScript, out, indentOut, level.localVariables, nDebug);
        level.close();
      } else {
        result = this.execute(contentScript, out, indentOut, this.localVariables, nDebug);
      }
      return result;
    }

  

    /**Processes the statement of the current node in the JZcmditem.
     * @param statementList 
     * @param outText The current output. Either it is a special output channel for <+channel>...<.+>
     *   or it is the threadData.out or it is null if threadData.out is not initialized yet.
     * @param indentOutArg The indentation in the script.
     * @param bContainerHasNext Especially for <:for:element:container>SCRIPT<.for> to implement <:hasNext>
     * @param newVariables Destination instance for newly created variables. It is {@link #localVariables} usual
     *   but in special cases new variables are stored in an own Map, especially on { <dataStruct> }.
     * @param nDebugP
     * @return {@link JZtxtcmdExecuter#kSuccess} ==0, {@link JZtxtcmdExecuter#kBreak}, {@link JZtxtcmdExecuter#kReturn} or {@link JZtxtcmdExecuter#kException} 
     * @throws Exception
     */
    short execute(JZtxtcmdScript.StatementList statementList, StringFormatter outText, int indentOutArg
        , Map<String, DataAccess.Variable<Object>> newVariables, int nDebugP) 
    //throws Exception 
    {
      this.ctNesting +=1;
      //Generate direct requested output. It is especially on inner content-scripts.
      int indentOut = indentOutArg;
      int ixStatement = -1;
      short ret = 0;
      //Note: it is admissible that statementList is null. for empty blocks. @since 2018-10-16 
      int ixStatementEnd = statementList !=null && statementList.statements !=null ? statementList.statements.size() : 0;
      //Note: don't use an Iterator, use ixStatement because it will be incremented onError.
      while(ret == 0 && ++ixStatement < ixStatementEnd) { //iter.hasNext() && sError == null){
        JZtxtcmdScript.JZcmditem statement = statementList.statements.get(ixStatement); //iter.next();
        int nDebug1 = 0; //TODO nDebug>0 || debugNext >=0;
        if(statement.elementType() == 'D'){
          nDebug1 = debug(statement);  //debug
          if(++ixStatement < statementList.statements.size()) { //iter.hasNext() && sError == null){
            statement = statementList.statements.get(ixStatement);
          } else {
            //debug was the last statement, it is unecessary.
          }
        } else {
          nDebug1 = nDebugP;
        }
        if(nDebug1 >=0){
          //TODO print a debug text
          Debugutil.stop();  //NOTE: set the local variable nDebug1 to 0: stop debugging for sub levels.
        }
        //for(TextGenScript.ScriptElement statement: contentScript.content){
        try{    
          switch(statement.elementType()){
          //case ' ': bSetSkipSpaces = true; break;
          case 't': exec_Text(statement, outText, indentOut);break; //<:>...textexpression <.>
          case '@': exec_SetColumn((JZtxtcmdScript.TextColumn)statement, outText);break; //<:@23>
          case 'n': outText.append(this.jzcmdMain.newline);  break;   //<.n+>
          case '!': outText.flush();  break;   //<.n+>
          case '_': outText.close();  outText = null; break;   //<.n+>
          case '\\': outText.append(statement.textArg);  break;   //<:n> transcription
          case 'T': ret = exec_TextAppendToVar((JZtxtcmdScript.TextOut)statement, --nDebug1); break; //<+text>...<.+> 
          case ':': ret = exec_TextAppendToOut(statement, outText, --nDebug1); break; //<+text>...<.+> 
          case 'A': break;  //used for Argument
          //case 'X': break;  //unused for dataStruct in Argument
          case 'U': ret = defineExpr(newVariables, (JZtxtcmdScript.DefVariable)statement); break; //setStringVariable(statement); break; 
          case 'S': ret = defineExpr(newVariables, (JZtxtcmdScript.DefVariable)statement); break; //setStringVariable(statement); break; 
          case 'P': { //create a new local variable as pipe
            StringBuilder uBufferVariable = new StringBuilder();
            exec_DefVariable(newVariables, (JZtxtcmdScript.DefVariable)statement, 'P', uBufferVariable, true);
          } break;
          case 'L': ret = exec_DefList((JZtxtcmdScript.DefContainerVariable)statement, newVariables); break; 
          case 'M': ret = exec_DefMapVariable((JZtxtcmdScript.DefVariable)statement, newVariables); break;
          case 'W': ret = exec_Openfile(newVariables, (JZtxtcmdScript.DefVariable)statement); break;
          case 'C': ret = exec_DefClassVariable((JZtxtcmdScript.DefClassVariable) statement, newVariables); break; 
          case 'J': ret = exec_addClassLoader((JZtxtcmdScript.DefClasspathVariable)statement, newVariables); break;
          case 'O': {
            Object value = evalObject(statement, false);
            if(value == JZtxtcmdExecuter.retException){ ret = kException; }
            else {
              exec_DefVariable(newVariables, (JZtxtcmdScript.DefVariable)statement, 'O', value, false);
            }
          } break;
          case 'K': {
            Object value = evalValue(statement, false);
            if(value == JZtxtcmdExecuter.retException){ ret = kException; }
            else {
              exec_DefVariable(newVariables, (JZtxtcmdScript.DefVariable)statement, 'K', value, false);
            }
          } break;
          case 'Q': {
            Object cond = new Boolean(evalCondition(statement));
            if(cond == JZtxtcmdExecuter.retException){ ret = kException; }
            else {
              exec_DefVariable(newVariables, (JZtxtcmdScript.DefVariable)statement, 'Q', cond, false);
            }
          } break;
          case '{': {  //a Subtext or codeblock variable:  
            //don't evaluate the statements of the variable yet. 
            //JZcmdScript.DefVariable statementDef = (JZcmdScript.DefVariable)statement;
            //exec_DefVariable(newVariables, statementDef, '{', statementDef.statementlist, false);
            exec_DefCodeblockVariable(newVariables, (JZtxtcmdScript.Subroutine)statement, true); 
          } break;
          case 'e': ret = exec_Datatext((JZtxtcmdScript.DataText)statement, outText, indentOut, --nDebug1); break; 
          case 's': ret = exec_Call((JZtxtcmdScript.CallStatement)statement, null, outText, indentOut, --nDebug1); break;  //sub
          case 'x': ret = exec_Thread(newVariables, (JZtxtcmdScript.ThreadBlock)statement); break;             //thread
          case 'm': exec_Move((JZtxtcmdScript.FileOpArg)statement); break;             //move
          case 'y': exec_Copy((JZtxtcmdScript.FileOpArg)statement); break;             //copy
          case 'l': exec_Delete((JZtxtcmdScript.FileOpArg)statement); break;             //copy
          case 'c': exec_cmdline((JZtxtcmdScript.CmdInvoke)statement); break;              //cmd
          case 'd': ret = exec_ChangeCurrDir(statement); break;                              //cd
          case '9': ret = exec_MkDir(statement); break;                              //mkdir
          case 'f': ret = exec_forContainer((JZtxtcmdScript.ForStatement)statement, outText, indentOut, --nDebug1); break;  //for
          case 'B': ret = exec_NestedLevel(statement, outText, indentOut, --nDebug1); break;              //statementBlock
          case 'i': ret = exec_IfStatement((JZtxtcmdScript.IfStatement)statement, outText, indentOut, --nDebug1); break;
          case 'w': ret = exec_whileStatement((JZtxtcmdScript.CondStatement)statement, outText, indentOut, --nDebug1); break;
          case 'u': ret = exec_dowhileStatement((JZtxtcmdScript.CondStatement)statement, outText, indentOut, --nDebug1); break;
          case 'N': ret = exec_hasNext(statement, outText, indentOut, --nDebug1); break;
          case '=': ret = assignStatement(statement); break;
          case '+': ret = appendExpr((JZtxtcmdScript.AssignExpr)statement); break;        //+=
          case '?': break;  //don't execute a onerror, skip it.  //onerror
          case 'z': throw new JZtxtcmdExecuter.ExitException(((JZtxtcmdScript.ExitStatement)statement).exitValue);  
          case 'r': exec_Throw(statement); break;
          case 'v': exec_Throwonerror((JZtxtcmdScript.Onerror)statement); break;
          case ',': bWriteErrorInOutput = statement.textArg !=null; break;
          case 'b': ret = JZtxtcmdExecuter.kBreak; break;
          case '#': ret = exec_CmdError((JZtxtcmdScript.Onerror)statement, outText, indentOut); break;
          case 'F': ret = exec_createFilepath(newVariables, (JZtxtcmdScript.DefVariable) statement); break;
          case 'G': ret = exec_createFileSet(newVariables, (JZtxtcmdScript.UserFileset) statement); break;
          case 'o': { StringFormatter outTextnew = exec_OpenTextOut(statement, outText, false); //changes the outText 
                      if(outTextnew ==null && outText !=null) { ret = kException;} else {ret = kSuccess; outText = outTextnew; } } break;
          case 'q': { StringFormatter outTextnew = exec_OpenTextOut(statement, outText, true); //changes the outText 
                      if(outTextnew ==null && outText !=null) { ret = kException;} else {ret = kSuccess; outText = outTextnew; } } break;
          case 'Z': ret = exec_zmake((JZtxtcmdScript.Zmake) statement, outText, indentOut, --nDebug1); break;
          case 'D': break; // a second debug statement one after another or debug on end is ignored.
          case 'H': exec_DebugOp(statement); break; // debugOp
          case '.': ret = execute(statement.statementlist, outText, indentOutArg, newVariables, nDebugP); break;  //recursively because inlined included statements.
          default: throw new IllegalArgumentException("JZcmd.execute - unknown statement; ");
          }//switch
          
        } catch(Exception exc){
          //any statement has thrown an exception.
          
          CharSequence errortext;
          if(exc instanceof InvocationTargetException){
            this.threadData.exception = exc.getCause();
          } else {
            this.threadData.exception = exc;
          }
          this.threadData.excStatement = statement;
          this.threadData.excLine = statement.srcLine;          //TODO: prepare all calling levels!
          this.threadData.excColumn = statement.srcColumn;
          this.threadData.excSrcfile = statement.srcFile;
          StringBuilder u = new StringBuilder(1000); 
          u.append(this.threadData.exception.toString()).append("; in statement: ");
          statement.writeStructLine(u);
          this.threadData.error.setValue(u);
          errortext = u;
          if(this.bWriteErrorInOutput){
            try{ outText.append("<?? ").append(errortext).append(" ??>");
            } catch(IOException exc1){ throw new RuntimeException(exc1); }
            this.threadData.error.setValue(null);  //clear for next usage.
            this.threadData.exception = null;
            this.threadData.excStatement = null;
          } else {
            ret = kException;
          }
        } //catch
        //
        //handle onerror
        //
        if(ret == kException){
          //check onerror with proper error type anywhere after this statement, it is stored in the statement.
          //continue there.
          boolean found = false;
          char excType;   //NOTE: the errortype in an onerror statement is the first letter of error keyword in syntax; notfound, file, internal, exit
          int errLevel = 0;
          final Throwable exc1 = this.threadData.exception;
          if(exc1 instanceof ExitException){ excType = 'e'; errLevel = ((ExitException)exc1).exitLevel; }
          else if(exc1 instanceof IOException){ excType = 'f'; }
          else if(exc1 instanceof CmdErrorLevelException){ excType = 'c'; }
          else if(exc1 instanceof NoSuchFieldException || exc1 instanceof NoSuchMethodException){ excType = 'n'; }
          else { excType = 'i'; }
          //Search the block of onerror after this statement.
          //Maybe use an index in any statement, to prevent search time.
          JZtxtcmdScript.JZcmditem onerrorStatement = null;
          while(++ixStatement < statementList.statements.size() && (onerrorStatement = statementList.statements.get(ixStatement)).elementType() != '?');
          if(ixStatement < statementList.statements.size()){
            assert(onerrorStatement !=null);  //because it is found in while above.
            //onerror-block found.
            do { //search the appropriate error type:
              char onerrorType;
              JZtxtcmdScript.Onerror errorStatement = (JZtxtcmdScript.Onerror)onerrorStatement;
              if( ((onerrorType = errorStatement.errorType) == excType
                || (onerrorType == '?' && excType != 'e')   //common onerror is valid for all excluding exit 
                )  ){
                found = excType != 'e' || errLevel >= errorStatement.errorLevel;  //if exit exception, then check errlevel
              }
            } while(!found && ++ixStatement < statementList.statements.size() && (statement = statementList.statements.get(ixStatement)).elementType() == '?');
          }
          if(found){ //onerror found:
            assert(onerrorStatement !=null);  //because it is found in while above.
            ret = execute(onerrorStatement.statementlist, outText, indentOut, this.localVariables, -1);  //executes the onerror block
            //maybe throw exception too, Exception in onerror{...}
            if(ret != kException) {
              this.threadData.error.setValue(null);  //clear for next usage.
              this.threadData.exception = null;
              this.threadData.excStatement = null;
            }
          } else {
            ret = kException;  //terminates this level.
            assert(this.threadData.exception !=null);
          }
        }
      }//while
      /*
      DataAccess.Variable<Object> retVar = localVariables.get("return");
      if(retVar !=null){
        @SuppressWarnings("unchecked")
        Map<String, DataAccess.Variable<Object>> ret =  (Map<String, DataAccess.Variable<Object>>)retVar.value(); 
        return ret;
      }
      else { return null; }
      */
      endExecution();
      return ret;
    }
    
    
    
    private void endExecution(){
      if(--this.ctNesting <=0){
        assert(this.ctNesting ==0);
        //close this level.
        if(this.cmdExecuter !=null){
          this.cmdExecuter.close();
          this.cmdExecuter = null;
        }
      }
    }
    
    
    /**Outputs the given constant text
     * @param statement contains the text
     * @param out out channel to append
     * @param indentOut Number of characters in a new line which are skipped from begin. Indentation in the script, no indentation in outputted text
     * @throws IOException
     */
    void exec_Text(JZtxtcmdScript.JZcmditem statement, Appendable out, int indentOut) throws IOException{
      if(statement.textArg.startsWith("|+"))
        Debugutil.stop();
      out.append(statement.textArg);
    }
    
    
    void exec_SetColumn(JZtxtcmdScript.TextColumn statement, StringFormatter out) throws Exception{
      int column = -1;
      int minChars = -1;
      if(statement.minSpaces !=null) {
        CalculatorExpr.Value value = calculateExpression(statement.minSpaces); //.calcDataAccess(localVariables);
        minChars = value.intValue();
      }
      if(statement.expression !=null) {
        CalculatorExpr.Value value = calculateExpression(statement.expression); //.calcDataAccess(localVariables);
        column = value.intValue();
      }
      
      out.pos(column, minChars);
    }
    
    

    /**
     * @param newVariables
     * @param statement {@link JZtxtcmdScript.DefVariable#defVariable} contains the type.
     * @param type unused, see statement
     * @param value
     * @param isConst
     * @throws Exception
     */
    void exec_DefVariable(Map<String, DataAccess.Variable<Object>> newVariables, JZtxtcmdScript.DefVariable statement, char type, Object valueArg, boolean isConst) 
    throws Exception {
      Object value = valueArg; //default: without conversion.
      CalculatorExpr.Value valueCalc = value instanceof CalculatorExpr.Value ? (CalculatorExpr.Value) valueArg : null;
      switch(statement.elementType) {
      case 'K': //numeric Value
        if(valueCalc !=null) { valueCalc.toNumValue(); }
        else if(!(value instanceof Number)) { } //TODO convert, improve StringFunctions_C
        break;
      default:
      }

      if(statement.typeVariable !=null) {
        if(!DataAccess.istypeof(value, statement.typeVariable)) {
          throw new IllegalArgumentException("execSubroutine - argument type faulty, " + value.getClass().getName() + " ");
        }
      }
      if(statement.defVariable.datapath().get(0).ident().equals("return") && !newVariables.containsKey("return")) {
        //
        //creates the local variable return on demand:
        DataAccess.Variable<Object> ret = new DataAccess.Variable<Object>('M', "return", this.jzcmdMain.new_Variables());
        this.localVariables.put("return", ret); //.add("return", ret);
      }
      //statement.defVariable is type of DataAccess and contains the type. 
      storeValue(statement.defVariable, newVariables, value, this.jzcmdMain.bAccessPrivate);
    }

    
    
    /**It defines a variable type 'X' and stores the given subroutine reference to execute that statements on evaluating the variable 
     * @param newVariables
     * @param statement Subroutine reference
     * @param type not used, type is 'C'
     * @param value 
     * @param isConst
     * @throws Exception
     */
    void exec_DefCodeblockVariable(Map<String, DataAccess.Variable<Object>> newVariables, JZtxtcmdScript.Subroutine statement, boolean isConst) 
    throws Exception {
      DataAccess defVariable = new DataAccess(statement.name, 'X');
      storeValue(defVariable, newVariables, statement, this.jzcmdMain.bAccessPrivate);
    }

    
    
    protected short exec_DefList(JZtxtcmdScript.DefContainerVariable statement,  Map<String, DataAccess.Variable<Object>> newVariables)
    throws Exception   
    { short ret = 0;
      Object value;
      if(statement.statementlist !=null) {
        //the list variable should be build with this statements:
        ListMap valueList = new ListMap();
        for(JZtxtcmdScript.JZcmditem elementStm: statement.statementlist.statements) {
          char elementType = elementStm.elementType();
          if(elementType == '*') {
            //A container with variable definition adequate 'M' but not as Map variable
            final ExecuteLevel level = new ExecuteLevel(this.jzcmdMain, this.subRoutine, this.jzClass, this.threadData, this, this.localVariables);
            Map<String, DataAccess.Variable<Object>> elementValue = 
              new TreeMap<String, DataAccess.Variable<Object>>(); 
              //new IndexMultiTable<String, DataAccess.Variable<Object>>(IndexMultiTable.providerString); 
            //fill the dataStruct with its values:
            ret = level.execute(elementStm.statementlist, null, 0, elementValue, -1); //Note: extra newVariables
            level.close();
            if(ret == kException) 
              return ret;
            else {
              String key = null;
              if(statement.keyVariableName !=null) {       //do add as Map only if keyVariableName is given, it is List name @keyVariableName = 
                Object okey = elementValue.get(statement.keyVariableName);
                if(okey instanceof DataAccess.Variable<?>) {
                  key = ((DataAccess.Variable<?>)okey).value().toString();
                }
                else if(okey !=null) {
                  key = okey.toString();
                }
              }
              valueList.add(key, elementValue); 
            }
          } else if (elementStm instanceof JZtxtcmdScript.Subroutine) {
            JZtxtcmdScript.Subroutine stm1 = (JZtxtcmdScript.Subroutine) elementStm;
            DataAccess.Variable<Object> variable = new DataAccess.Variable<Object>('X', stm1.name, stm1);  //create a codeblock (eXectable) variable
            valueList.add(stm1.name, variable);
          } else if (elementStm instanceof JZtxtcmdScript.DefVariable) {
            //A variable:
            JZtxtcmdScript.DefVariable stm1 = (JZtxtcmdScript.DefVariable) elementStm;
            String name = stm1.getVariableIdent();
            final Object elementValue;
            if(stm1.elementType() == '{'){
              assert(false);
              elementValue = stm1.statementlist;
            } else {
              elementValue = evalObject(elementStm, true);
            }
            DataAccess.Variable<Object> variable = new DataAccess.Variable<Object>(elementType, name, elementValue);
            valueList.add(name, variable);
          } else {
            //Any other expression
            Object elementValue = evalObject(elementStm, true);
            valueList.add(null, elementValue);
          }
        }
        value = valueList;
      } else {
        value = evalObject(statement, true);  //any list from user.
      }
      if(value == JZtxtcmdExecuter.retException){ ret = kException; }
      else {
        if(value !=null && !(value instanceof Iterable<?>)) 
          throw new NoSuchFieldException("JZcmdExecuter - exec variable must be of type Iterable ;" + ((JZtxtcmdScript.DefVariable)statement).defVariable);
        if(value ==null){ //initialize the list
          value = new ArrayList<Object>();
        }
        exec_DefVariable(newVariables, statement, 'L', value, true);
      }
      return ret;
    }

    
    
    protected short exec_DefMapVariable(JZtxtcmdScript.DefVariable statement,  Map<String, DataAccess.Variable<Object>> newVariables) 
    throws Exception   
    { short ret = kSuccess;
      Object value = evalObject(statement, false);
      if(value == JZtxtcmdExecuter.retException) {
        ret = kException;
      } else {
        if(value == null) { //no initialization
          value = new TreeMap<String, DataAccess.Variable<?>>();
        }
        exec_DefVariable(newVariables, statement, 'M', value, true); 
      }
      return ret;
    }



    protected short exec_DefClassVariable(JZtxtcmdScript.DefClassVariable statement, Map<String, DataAccess.Variable<Object>> newVariables) 
    throws Exception   
    { //Class name = java.path
      short ret = kSuccess;
      CharSequence value = evalString(statement);
      Class<?> clazz;
      if(statement.loader !=null){
        Object oLoader = dataAccess(statement.loader, this.localVariables, this.jzcmdMain.bAccessPrivate, false, false, null);  //get the loader
        if(!(oLoader instanceof ClassLoader)) throw new IllegalArgumentException("JZcmd.exec_DefClassVariable - faulty ClassLoader");
        ClassLoader loader = (ClassLoader)oLoader;
        clazz = loader.loadClass(value.toString());
      } else {
        clazz = Class.forName(value.toString()); 
      }
      exec_DefVariable(newVariables, statement, 'C', clazz, false);
      return ret; 
    }



    protected short exec_addClassLoader(JZtxtcmdScript.DefClasspathVariable statement, Map<String, DataAccess.Variable<Object>> newVariables) 
    throws Exception {
      List<File> filesjar = new LinkedList<File>();
      for(JZtxtcmdScript.AccessFilesetname fileset: statement.jarpaths){
        if(fileset.filesetVariable !=null){  //fileset variable
          JZtxtcmdAccessFileset zjars = new JZtxtcmdAccessFileset(fileset, this);
          List<JZtxtcmdFilepath> jars = zjars.listFilesExpanded();
          for(JZtxtcmdFilepath jfilejar: jars){
            File filejar = new File(jfilejar.absfile().toString());
            if(!filejar.exists()) throw new IllegalArgumentException("JZcmd.addClasspath - file does not exist; " + filejar.getAbsolutePath());
            filesjar.add(filejar);
          }
        } else {
          final FilePath accessPath;
          //if(fileset.accessPath ==null){
            CharSequence sAccesspath = evalString(fileset);
            accessPath = new FilePath(sAccesspath.toString());
          //} else {
          //  accessPath = fileset.accessPath;
          //}
          JZtxtcmdFilepath jfilejar = new JZtxtcmdFilepath(this, accessPath);
          File filejar = new File(jfilejar.absfile().toString());
          if(!filejar.exists()) throw new IllegalArgumentException("JZcmd.addClasspath - file does not exist; " + filejar.getAbsolutePath());
          filesjar.add(filejar);
        }
      }
      URL[] urls = new URL[filesjar.size()];
      int ixurl = -1;
      for(File filejar: filesjar){
        URI uri = filejar.toURI();
        urls[++ixurl] = uri.toURL();
          
      }
      ClassLoader parentLoader = this.getClass().getClassLoader(); //classLoader from this class
      URLClassLoader loader = new URLClassLoader(urls, parentLoader);
      exec_DefVariable(newVariables, statement, 'J', loader, true);
      loader.close();
      return kSuccess;
    }



    short exec_createFilepath(Map<String, DataAccess.Variable<Object>> newVariables, JZtxtcmdScript.DefVariable statement) throws Exception {
      CharSequence sPath = evalString(statement);
      if(sPath == JZtxtcmdExecuter.retException){ return kException; }
      else {
      
        JZtxtcmdFilepath filepath = new JZtxtcmdFilepath(this, sPath.toString());
        storeValue(statement.defVariable, newVariables, filepath, false);
        return kSuccess;
      }
    }
    
      
    short exec_createFileSet(Map<String, DataAccess.Variable<Object>> newVariables, JZtxtcmdScript.UserFileset statement) throws Exception {
      JZtxtcmdFileset filepath = new JZtxtcmdFileset(this, statement);
      storeValue(statement.defVariable, newVariables, filepath, false);
      return kSuccess;
    }
    
      
    private short exec_forContainer(JZtxtcmdScript.ForStatement statement, StringFormatter out, int indentOut, int nDebug) 
    throws Exception
    {
      JZtxtcmdScript.StatementList subContent = statement.statementlist();  //The same sub content is used for all container elements.
      //Note: don't use an extra ExecuteLevel to save calculation time. Especially the forVariable and some inner defined variables
      //      are existing outside of the for loop body namely, but that property is defined in the JZcmd language description.
      //ExecuteLevel forExecuter = this; //do not do so: new ExecuteLevel(threadData, this, localVariables);
      //creates the for-variable in the executer level. Use an existing variable and set it to null.
      DataAccess.Variable<Object> forVariable = DataAccess.createOrReplaceVariable(this.localVariables, statement.forVariable, 'O', null, false);
      //a new level for the for... statements. It contains the foreachData and maybe some more variables.
      Object container = dataAccess(statement.forContainer, this.localVariables, this.jzcmdMain.bAccessPrivate, false, false, null);
      //Object container = statement.forContainer.getDataObj(localVariables, bAccessPrivate, true);
      //DataAccess.Dst dst = new DataAccess.Dst();
      //DataAccess.access(statement.defVariable.datapath(), null, localVariables, bAccessPrivate,false, true, dst);
      short cont = kSuccess;
      boolean bForHasNextOld = this.bForHasNext;  //to restore. Note: bForHasNext is a instance variable to check it in hasNext()
      //
      if(container instanceof String && ((String)container).startsWith("<?")){
        throw new IllegalArgumentException("JZcmd.execFor - faulty container type;" + (String)container);
      }
      else if(container !=null && container instanceof Iterable<?>){
        Iterator<?> iter = ((Iterable<?>)container).iterator();
        this.bForHasNext = iter.hasNext();
        while(cont == kSuccess && this.bForHasNext){
          Object foreachData = iter.next();
          forVariable.setValue(foreachData);
          this.bForHasNext = iter.hasNext();  //an element after it?
          if(statement.condition !=null && ! evalCondition(statement.condition)) {
            cont = kBreak;
          } else if(subContent !=null) {
            cont = this.execute(subContent, out, indentOut, this.localVariables, nDebug);
          }
        }//while of for-loop
      }
      else if(container !=null && container instanceof Map<?,?>){
        Map<?,?> map = (Map<?,?>)container;
        Set<?> entries = map.entrySet();
        Iterator<?> iter = entries.iterator();
        this.bForHasNext = iter.hasNext();
        while(cont == kSuccess && this.bForHasNext){
          Map.Entry<?, ?> foreachDataEntry = (Map.Entry<?, ?>)iter.next();
          Object foreachData = foreachDataEntry.getValue();
          forVariable.setValue(foreachData);
          this.bForHasNext = iter.hasNext();  //an element after it?
          if(statement.condition !=null && ! evalCondition(statement.condition)) {
            cont = kBreak;
          } else if(subContent !=null) {
            cont = this.execute(subContent, out, indentOut, this.localVariables, nDebug);
          }
        }
      }
      else if(container !=null && container.getClass().isArray()){
        Object[] aContainer = (Object[])container;
        int zContainer = aContainer.length;
        int iContainer = 0;
        this.bForHasNext = iContainer < zContainer;
        while(cont == kSuccess && this.bForHasNext){
          Object foreachData = aContainer[iContainer];
          forVariable.setValue(foreachData);
          this.bForHasNext = ++iContainer < zContainer;
          if(statement.condition !=null && ! evalCondition(statement.condition)) {
            cont = kBreak;
          } else if(subContent !=null) {
            cont = this.execute(subContent, out, indentOut, this.localVariables, nDebug);
          }
        }
      }
      else if(container instanceof CalculatorExpr.Value) {
        int endValue = (int)((CalculatorExpr.Value)container).longValue();
        for(int ix = 0; ix < endValue; ++ix) {
          forVariable.setValue(new CalculatorExpr.Value(ix));
          if(statement.condition !=null && ! evalCondition(statement.condition)) {
            cont = kBreak;
          } else if(subContent !=null) {
            cont = this.execute(subContent, out, indentOut, this.localVariables, nDebug);
          }
          
        }
      }
      else if(container !=null) { //only for the simple variable:
        forVariable.setValue(container);
        if(statement.condition !=null && ! evalCondition(statement.condition)) {
          cont = kBreak;
        } else if(subContent !=null) {
          cont = this.execute(subContent, out, indentOut, this.localVariables, nDebug);
        }
      }
      if(cont == kSuccess && !this.bForHasNext) {  //on break it is not completed. 
        forVariable.setValue(null); //on any completed loop. On break the variable remain its content.
      }
      //
      this.bForHasNext = bForHasNextOld;  //restore for nested for loops.
      if(cont == kBreak){ cont = kSuccess; } //break in while does not break at calling level. It breaks only the own one.
      return cont; //maybe kException
    }
    
    
    
    /**Executes the statements or output the text if in textArg if a for-container has a next element.
     * 
     * @param statement the hasnext-Statement. 
     * @param out
     * @param indentOut
     * @param nDebug
     * @return
     * @throws Exception
     */
    short exec_hasNext(JZtxtcmdScript.JZcmditem statement, StringFormatter out, int indentOut, int nDebug) 
    throws Exception{
      short cont = kSuccess;
      if(this.bForHasNext){
        JZtxtcmdScript.StatementList statementList = statement.statementlist();
        if(statementList !=null){
          cont = execute(statement.statementlist(), out, indentOut, this.localVariables, nDebug);
        } 
        else if(statement.textArg !=null){
          exec_Text(statement, out, indentOut);
        }
      }
      return cont;
    }
    
    
    
    /**it contains maybe more as one if block and else. 
     * @return {@link #kBreak} if a break statement was found.
     * @throws Exception 
     */
    short exec_IfStatement(JZtxtcmdScript.IfStatement ifStatement, StringFormatter out, int indentOut, int nDebug) 
    throws Exception{
      short cont = kFalse;
      Iterator<JZtxtcmdScript.JZcmditem> iter = ifStatement.statementlist.statements.iterator();
      //boolean found = false;  //if block found
      while(iter.hasNext() && cont == kFalse ){ //check which if branch should be executed
        JZtxtcmdScript.JZcmditem statement = iter.next();
        switch(statement.elementType()){
          case 'g': { //if-block
            JZtxtcmdScript.IfCondition ifBlock = (JZtxtcmdScript.IfCondition)statement;
            boolean bCheck = evalCondition(ifBlock.condition); //.calcDataAccess(localVariables);
            if(bCheck){
              if(ifBlock.statementlist !=null) {
                cont = execute(ifBlock.statementlist, out, indentOut, this.localVariables, nDebug);
              }
            } else {
              cont = kFalse;
            }
          } break;
          case 'E': { //elsef
            if(statement.statementlist !=null) {
              cont = execute(statement.statementlist, out, indentOut, this.localVariables, nDebug);
            }
          } break;
          default:{
            throw new IllegalArgumentException("JZcmd.execIf - unknown statement; " + statement.elementType());
          }
        }//switch
      }//for
      if(cont == kFalse){ cont = kSuccess; }  //no if block found, though success.
      return cont;  //if a break statement was found, kBreak is returned.
    }
    
    
    
    /**Executes a while statement. 
     * @throws Exception */
    short exec_whileStatement(JZtxtcmdScript.CondStatement whileStatement, StringFormatter out, int indentOut, int nDebug) 
    throws Exception 
    {
      short cont = kSuccess;
      boolean cond;
      do{
        cond = evalCondition(whileStatement.condition); //.calcDataAccess(localVariables);
        if(cond){
          if(whileStatement.statementlist !=null) {
            cont = execute(whileStatement.statementlist, out, indentOut, this.localVariables, nDebug);
          }
        }
      } while(cond && cont == kSuccess);  //break on kBreak;  
      if(cont == kBreak){ cont = kSuccess; } //break in while does not break at calling level.
      return cont;
    }
    
    
    
    /**Executes a dowhile statement. 
     * @throws Exception */
    short exec_dowhileStatement(JZtxtcmdScript.CondStatement whileStatement, StringFormatter out, int indentOut, int nDebug) 
    throws Exception 
    { short cont;
      boolean cond;
      do{
        cont = execute(whileStatement.statementlist, out, indentOut, this.localVariables, nDebug);
        cond = evalCondition(whileStatement.condition); //.calcDataAccess(localVariables);
      } while(cond && cont == kSuccess);  //if executed, check cond again.  
      if(cont == kBreak){ cont = kSuccess; } //break in while does not break at calling level.
      return cont;
    }
    
    
    
    
    
    
    /**Invocation for <:>text<.+>.
     * It gets the Appendable from the assign variable
     * and executes {@link #execute(JZtxtcmdScript.StatementList, StringFormatter, int, Map, int)}
     * with it. The arg 'indentOutArg' is set from the {@link JZtxtcmdScript.JZcmditem#srcColumn} of the statement. 
     * @param statement the statement
     * @throws Exception 
     */
    short exec_TextAppendToOut(JZtxtcmdScript.JZcmditem statement, StringFormatter out, int nDebug) throws Exception
    { short ret;
      if(statement.statementlist !=null){
        //executes the statement, use the Appendable to output immediately
        synchronized(out){
          ret = execute(statement.statementlist, out, statement.srcColumn-1, this.localVariables, nDebug);
        }
      } else {
        //Any other text expression
        CharSequence text = evalString(statement);
        ret = text == JZtxtcmdExecuter.retException ? kException : kSuccess;
        if(text !=null){
          synchronized(out){
            out.append(text);
          }
        }
      }
      //if(bShouldClose){
      //  out1.close();
      //}
      return ret;
    }
    
    

    
    
    
    
    
    /**Invocation for <+name>text<.+>.
     * It gets the Appendable from the assign variable
     * and executes {@link #execute(JZtxtcmdScript.StatementList, StringFormatter, int, Map, int)}
     * with it. The arg 'indentOutArg' is set from the {@link JZtxtcmdScript.JZcmditem#srcColumn} of the statement. 
     * @param statement the statement
     * @throws Exception 
     */
    short exec_TextAppendToVar(JZtxtcmdScript.TextOut statement, int nDebug) throws Exception
    { StringFormatter out1;
      //boolean bShouldClose;
      short ret;
      if(statement.variable !=null){
        Object chn;
        //Object oVar = DataAccess.access(statement.variable.datapath(), null, localVariables, bAccessPrivate, false, true, null);
        Object oVar = dataAccess(statement.variable, this.localVariables, this.jzcmdMain.bAccessPrivate, false, true, null);
        if(oVar instanceof DataAccess.Variable<?>){
          @SuppressWarnings("unchecked")
          DataAccess.Variable<Object> var = (DataAccess.Variable<Object>) oVar;
          chn = var.value();
          if(chn == null && var.type() == 'A'){
            chn = new StringPartAppend();
            var.setValue(chn);            //Creates a new StringPartAppend for an uninitialized variable. Stores there.
          }
        } else {
          //it is not a variable, can be direct stored Appendable:
          chn = oVar;
        }
        if(chn instanceof StringFormatter){
          out1 = (StringFormatter)chn;
          //bShouldClose = false;
        } else if((chn instanceof Appendable)) {
          out1 = new StringFormatter((Appendable)chn, chn instanceof Closeable, "\n", 200);  //append, it may be a StringPartAppend.
          //bShouldClose = true;
        } else {
          throwIllegalDstArgument("variable should be Appendable", statement.variable, statement);
          out1 = new StringFormatter();  //NOTE: it is a dummy because the statement above throws.
          //bShouldClose = false;
        }
      } else {
        out1 = jzcmdMain.textline;  //output to the text output.
        //bShouldClose = false;
      }
      if(statement.statementlist !=null){
        //executes the statement, use the Appendable to output immediately
        synchronized(out1){
          ret = execute(statement.statementlist, out1, statement.srcColumn-1, localVariables, nDebug);
          if(out1 instanceof Flushable){
            ((Flushable)out1).flush();
          }
        }
      } else {
        //Any other text expression
        CharSequence text = evalString(statement);
        ret = text == JZtxtcmdExecuter.retException ? kException : kSuccess;
        if(text !=null){
          synchronized(out1){
            out1.append(text);
          }
        }
      }
      //if(bShouldClose){
      //  out1.close();
      //}
      return ret;
    }
    
    

    
    
    
    
    
    private short exec_Call(JZtxtcmdScript.CallStatement callStatement, List<DataAccess.Variable<Object>> additionalArgs
        , StringFormatter out, int indentOut, int nDebug) 
    throws IllegalArgumentException, Exception
    { short success = kSuccess;
      final CharSequence nameSubtext;
      JZtxtcmdScript.Subroutine subroutine = null;
      /*
      if(statement.name == null){
        //subtext name gotten from any data location, variable name
        Object oName = ascertainText(statement.expression, localVariables); //getContent(statement, localVariables, false);
        nameSubtext = DataAccess.getStringFromObject(oName, null);
      } else {
        nameSubtext = statement.name;
      }*/
      if(callStatement.call_Name.dataAccess !=null) {
        Object o = dataAccess(callStatement.call_Name.dataAccess, this.localVariables, this.jzcmdMain.bAccessPrivate, false, false, null);
        //Object o = arg.dataAccess.getDataObj(localVariables, this.acc.bAccessPrivate, false);
        if(o instanceof JZtxtcmdScript.Subroutine) {
          subroutine = (JZtxtcmdScript.Subroutine)o;  //eval codeblock
          nameSubtext = null;
        } else if(o instanceof DataAccess.Variable && ((DataAccess.Variable<?>)o).type() == 'X'){ 
          //This possibility is not full tested yet, <:subtext:&variable>
          nameSubtext = null; 
          subroutine = null;  
          @SuppressWarnings("unchecked") 
          DataAccess.Variable<JZtxtcmdScript.StatementList> var = (DataAccess.Variable<JZtxtcmdScript.StatementList>)o;
          JZtxtcmdScript.StatementList statements = var.value();
          //The exec_subroutine is not invoked here, execute it without extra level and without arguments.
          success = execute(statements, out, indentOut, this.localVariables, nDebug);
        } else if(o ==null){ 
          throw new NoSuchElementException("JZcmdExecuter - subroutine variable emtpy; " );
        } else {
          nameSubtext = o.toString(); 
        }

      } else {
        nameSubtext = evalString(callStatement.call_Name);
        if(nameSubtext ==null) {
          throw new NoSuchElementException("JZcmdExecuter - subroutine name emtpy; " );
        }
      }
      if(subroutine == null && nameSubtext !=null) {
        subroutine = this.jzClass.subroutines.get(nameSubtext);
        if(subroutine == null) { //not found in this class:    
          subroutine = this.jzcmdMain.jzcmdScript.getSubroutine(nameSubtext);  //the subtext script to call
        }
      }
      if(subroutine == null){
          throw new NoSuchElementException("JZcmdExecuter - subroutine not found; " + nameSubtext);
      } else {  //subroutine !=null)  
        //TODO use execSubroutine, same code!
        final ExecuteLevel sublevel = levelForSubroutine(subroutine);
        success = exec_Subroutine(subroutine, sublevel, callStatement.actualArgs, additionalArgs, out, indentOut, nDebug);
        if(success == kSuccess){
          if(callStatement.variable !=null || callStatement.assignObjs !=null){
            DataAccess.Variable<Object> retVar = sublevel.localVariables.get("return");
            Object value = retVar !=null ? retVar.value() : null;
            assignObj(callStatement, value, false);
          }
          
        }
        sublevel.close();
      }
      return success;
    }
    

    
    
    /**This routine is called only from org.vishia.zcmd.JZcmd#evalSub(....) which may be not used in praxis?.
     * @param substatement
     * @param args
     * @param out
     * @param indentOut
     * @return
     * @throws ScriptException
     */
    public Object evalSubroutine(JZtxtcmdScript.Subroutine substatement
        , Map<String, DataAccess.Variable<Object>> args
        , StringFormatter out, int indentOut
    ) throws ScriptException 
    {
      short ok;
      try { 
        ok = exec_Subroutine(substatement, args, null, -1);
        //executer.execute(genScript, true, bWaitForThreads, null, null);
        //zgenExecuteLevel.execute(genScript.getMain().subContent, u, false);
      } catch (Exception exc) {
        throw new JzScriptException(exc);
      }
      if(ok != JZtxtcmdExecuter.kSuccess){
        throw new JzScriptException(this.threadData.exception.getMessage(), this.threadData.excSrcfile, this.threadData.excLine, this.threadData.excColumn);
      }
      return null;
    }    
    
    
    
    /**Executes a subroutine invoked from user space, with given {@link ExecuteLevel} as this.
     * This routine should be used if args are given as Map of Variable.
     * The routine creates an own level to execute the sub routine unless the subroutine is marked with {@link JZtxtcmdScript.Subroutine#useLocals}.
     * It calls {@link #levelForSubroutine(org.vishia.cmd.JZtxtcmdScript.Subroutine)} therefore.
     * Internally the private {@link #exec_Subroutine(org.vishia.cmd.JZtxtcmdScript.Subroutine, ExecuteLevel, List, List, StringFormatter, int, int)} is called.
     * @param substatement Statement of the subroutine
     * @param args Any given arguments in form of a map. It is proper to build a Map with JZcmd script features better than a list.
     *   The execution needs a List of Variables, the Variables of the map are copied to a temporary List instance.
     * @param out output
     * @param indentOut
     * @return null on success, an error message on parameter error
     * @throws Exception
     */
    public short exec_Subroutine(JZtxtcmdScript.Subroutine subroutine
        , Map<String, DataAccess.Variable<Object>> args
        , StringFormatter out, int indentOut
    )  
    {
      final List<DataAccess.Variable<Object>> arglist;
      if(args !=null){
        arglist = new LinkedList<DataAccess.Variable<Object>>();
        for(Map.Entry<String, DataAccess.Variable<Object>> entry: args.entrySet()){
          arglist.add(entry.getValue());
        }
      } else {
        arglist = null;
      }
      short success;
      final ExecuteLevel sublevel = levelForSubroutine(subroutine);
      try{
        success = exec_Subroutine(subroutine, sublevel, null, arglist, out, indentOut, -1);
      } catch(Exception exc){
        success = kException;
      }
      sublevel.close();
      return success;
    }
    
    
    
    /**Creates a new level for the subroutine with this given level. 
     * If the subroutine designates {@link JZtxtcmdScript.Subroutine#useLocals} then a new level is not created,
     * instead this is returned.
     * @param subroutine
     * @return
     */
    public final ExecuteLevel levelForSubroutine(JZtxtcmdScript.Subroutine subroutine) {
      final ExecuteLevel sublevel;
      JZtxtcmdScript.JZcmdClass subClass = (JZtxtcmdScript.JZcmdClass)subroutine.parentList;
      if(subroutine.useLocals) {  //TODO check whether the subClass == this.jzclass 
        sublevel = this; 
      } else { 
        sublevel = new ExecuteLevel(this.jzcmdMain, subroutine, subClass, this.threadData, this, null); 
      }
      return sublevel;
    }
    
    
    
    
    
    
    /**Core routine to execute a sub routine.
     * @param statement
     * @param sublevel
     * @param actualArgs
     * @param additionalArgs
     * @param out
     * @param indentOut
     * @param nDebug
     * @return
     * @throws Exception
     */
    private short exec_Subroutine(JZtxtcmdScript.Subroutine statement
        , ExecuteLevel sublevel
        , List<JZtxtcmdScript.Argument> actualArgs
        , List<DataAccess.Variable<Object>> additionalArgs
        , StringFormatter out, int indentOut, int nDebug  ////
    ) throws Exception
    {
      //String error = null;
      short success = kSuccess;
      if(statement.formalArgs !=null){
        //
        //build a Map temporary to check which arguments are used:
        //
        TreeMap<String, JZtxtcmdScript.DefVariable> check = new TreeMap<String, JZtxtcmdScript.DefVariable>();
        for(JZtxtcmdScript.DefVariable formalArg: statement.formalArgs) {
          check.put(formalArg.getVariableIdent(), formalArg);
        }
        //
        //process all actual arguments:
        //
        if(actualArgs !=null){
          for( JZtxtcmdScript.Argument actualArg: actualArgs){  //process all actual arguments
            Object ref;
            ref = evalObject(actualArg, false);
            JZtxtcmdScript.DefVariable checkArg = check.remove(actualArg.getIdent());      //is it a requested argument (per name)?
            if(checkArg == null){
              throw new IllegalArgumentException("\nError: execSubroutine: " + statement.name + "( ... "  + actualArg.identArgJbat + "=...) - unexpected argument; ");
            } else {
              if(checkArg.typeVariable !=null) {
                if(!DataAccess.istypeof(ref, checkArg.typeVariable)) {
                  throw new IllegalArgumentException("execSubroutine - argument type faulty, " +ref.getClass().getName() + " "  + actualArg.identArgJbat);
                }
              }
              char cType = checkArg.elementType();
              //creates the argument variable with given actual value and the requested type in the sub level.
              switch(cType){
                case 'F': ref = convert2FilePath(ref); break;
              }
              DataAccess.createOrReplaceVariable(sublevel.localVariables, actualArg.identArgJbat, cType, ref, false);
            }
          }
        }
        //
        //process additional arguments
        //
        if(additionalArgs !=null){
          for(DataAccess.Variable<Object> arg: additionalArgs){
            String name = arg.name();
            JZtxtcmdScript.DefVariable checkArg = check.remove(name);      //is it a requested argument (per name)?
            if(checkArg == null){
              throw new IllegalArgumentException("execSubroutine - unexpected additial argument; "  + name);
            } else {
              if(checkArg.typeVariable !=null) {
                if(!DataAccess.istypeof(arg.value(), checkArg.typeVariable)) {
                  throw new IllegalArgumentException("execSubroutine - argument type faulty, " +arg.getClass().getName() + " "  + arg.name());
                }
              }
              char cType = checkArg.elementType();
              //creates the argument variable with given actual value and the requested type in the sub level.
              Object argVal = arg.value();
              switch(cType){
                case 'F': argVal = convert2FilePath(argVal); break;
                default: argVal = arg.value();
              }
              //creates the argument variable with given actual value and the requested type in the sub level.
              DataAccess.createOrReplaceVariable(sublevel.localVariables, name, cType, argVal, false);
            }
          }
        }
        //check whether all formal arguments are given with actual args or get its default values.
        //if not all variables are correct, write error.
        for(Map.Entry<String, JZtxtcmdScript.DefVariable> checkArg : check.entrySet()){
          JZtxtcmdScript.DefVariable arg = checkArg.getValue();
          //Generate on this.acc.scriptLevel (classLevel) because the formal parameter list should not know things of the calling environment.
          Object ref = this.jzcmdMain.scriptLevel.evalObject(arg, false);
          String name = arg.getVariableIdent();
          char cType = arg.elementType();
          if(cType == 'F' && !(ref instanceof JZtxtcmdFilepath) ){
            ref = new JZtxtcmdFilepath(this, ref.toString());
          }
          //creates the argument variable with given default value and the requested type.
          DataAccess.createOrReplaceVariable(sublevel.localVariables, name, cType, ref, false);
        }
      } else if(actualArgs !=null){
        throw new IllegalArgumentException("execSubroutine -  not expected arguments");
      }
      success = sublevel.execute(statement.statementlist, out, indentOut, sublevel.localVariables, nDebug);
      return success;
    }
    
    
    
    /**Executes a Zmake subroutine call. Additional to {@link #exec_Call(org.vishia.cmd.JZtxtcmdScript.CallStatement, List, StringFormatter, int, int)}
     * a {@link ZmakeTarget} will be prepared and stored as 'target' in the localVariables of the sublevel.
     * @param statement
     * @param out
     * @param indentOut
     * @throws IllegalArgumentException
     * @throws Exception
     */
    private short exec_zmake(JZtxtcmdScript.Zmake statement, StringFormatter out, int indentOut, int nDebug) 
    throws IllegalArgumentException, Exception {
      ZmakeTarget target = new ZmakeTarget(this, statement.name);
      Object oOutput = evalObject(statement.jzoutput, false);
      target.output = convert2FilePath(oOutput); 
      //target.output = new JZcmdFilepath(this, statement.output);  //prepare to ready-to-use form.
      for(JZtxtcmdScript.AccessFilesetname input: statement.input){
        JZtxtcmdAccessFileset zinput = new JZtxtcmdAccessFileset(input, this);
        if(target.inputs ==null){ target.inputs = new ArrayList<JZtxtcmdAccessFileset>(); }
        target.inputs.add(zinput);
      }
      //Build a temporary list only with the 'target=target' as additionalArgs for the subroutine call
      List<DataAccess.Variable<Object>> args = new LinkedList<DataAccess.Variable<Object>>();
      DataAccess.Variable<Object> targetV = new DataAccess.Variable<Object>('O',"target", target, true);
      args.add(targetV);
      //
      //same as a normal subroutine.
      return exec_Call(statement, args, out, indentOut, nDebug);
    }
    
    
    
    
    
    
    
    
    
    /**executes statements in another thread.
     * @throws Exception 
     */
    private short exec_Thread(Map<String, DataAccess.Variable<Object>> newVariables, JZtxtcmdScript.ThreadBlock statement) 
    throws Exception
    { final JZtxtcmdThread thread;
      final String name;
      if(statement.threadVariable !=null){
        try{
          thread = new JZtxtcmdThread();
          name = statement.threadVariable.idents().toString();
          storeValue(statement.threadVariable, newVariables, thread, this.jzcmdMain.bAccessPrivate);
        } catch(Exception exc){
          throw new IllegalArgumentException("JZcmd - thread assign failure; path=" + statement.threadVariable.toString());
        }
      } else {
        thread = new JZtxtcmdThread();  //without assignment to a variable.
        name = "JZcmd";
      }
      ExecuteLevel threadLevel = new ExecuteLevel(this.jzcmdMain, this.subRoutine, this.jzClass, thread, this, this.localVariables);
      synchronized(this.jzcmdMain.threads){
        this.jzcmdMain.threads.add(thread);
      }
      thread.startThread(name, threadLevel, statement);
      //it does not wait on finishing this thread.
      return kSuccess;
    }

    
    
    
    /**Executes an internal statement block. If the Statementlist has local variables, a new
     * instance of this is created. Elsewhere the same instance is used.
     * 
     * @param script
     * @param out
     * @return continue information for the calling level, 
     *   see return of {@link #execute(org.vishia.cmd.JZtxtcmdScript.StatementList, Appendable, int, boolean)}
     *   A {@link JZtxtcmdExecuter#kBreak} inside the statementlist is not returned. The break is only valid
     *   inside the block. All other return values of execute are returned.
     * @throws IOException
     */
    private short exec_NestedLevel(JZtxtcmdScript.JZcmditem script, StringFormatter out, int indentOut, int nDebug) 
    throws Exception
    {
      ExecuteLevel genContent;
      /*
      if(false && script.statementlist.bContainsVariableDef){
        genContent = new ExecuteLevel(jzcmdMain, jzClass, threadData, this, localVariables);
      } else */{
        genContent = this;  //don't use an own instance, save memory and calculation time.
      }
      short ret = genContent.execute(script.statementlist, out, indentOut, genContent.localVariables, nDebug);
      if(ret == kBreak){ 
        ret = kSuccess; 
      }
      return ret;
    }

    
    
    private void exec_cmdline(JZtxtcmdScript.CmdInvoke statement) 
    throws IllegalArgumentException, Exception
    {
      //boolean ok = true;
      final CharSequence sCmd;
      if(statement.textArg == null){
        //cmd gotten from any data location, variable name
        sCmd = evalString(statement); 
      } else {
        sCmd = statement.textArg;
      }
      List<String> args = new ArrayList<String>();
      args.add(sCmd.toString());
      if(statement.cmdArgs !=null){
        //int iArg = 1;
        for(JZtxtcmdScript.JZcmditem arg: statement.cmdArgs){
          if(arg.elementType == 'L'){
            Object oVal = dataAccess(arg.dataAccess, localVariables, jzcmdMain.bAccessPrivate, false, false, null);
            if(oVal instanceof List<?>){
              @SuppressWarnings("unchecked")
              List<Object> arglist = (List<Object>)oVal;
              for(Object oArg: arglist){
                args.add( oArg.toString());
              }
            } else {
              
            }
          } else {
            String sArg = evalString(arg).toString(); 
            args.add(sArg);
          }
        }
      }
      if(statement.bCmdCheck){
        setLocalVariable("argsCheck", 'L', args, true);
      }
      //
      //The standard output of the cmd line invocation is written into destination variable.
      //Gather a list of Appendable for the execution. 
      //
      List<Appendable> outCmd;
      if(statement.variable !=null){
        outCmd = new LinkedList<Appendable>();
        Object oOutCmd = dataAccess(statement.variable, localVariables, jzcmdMain.bAccessPrivate, false, false, null);
        //Object oOutCmd = statement.variable.getDataObj(localVariables, this.acc.bAccessPrivate, false);
        if(oOutCmd instanceof Appendable){
          outCmd.add((Appendable)oOutCmd);
        } else { throwIllegalDstArgument("variable should be Appendable", statement.variable, statement);
        }
        if(statement.assignObjs !=null){
          for(JZtxtcmdScript.JZcmdDataAccess assignObj1 : statement.assignObjs){
            oOutCmd = dataAccess(assignObj1, localVariables, jzcmdMain.bAccessPrivate, false, false, null);
            //oOutCmd = assignObj1.getDataObj(localVariables, this.acc.bAccessPrivate, false);
            if(oOutCmd instanceof Appendable){
              outCmd.add((Appendable)oOutCmd);
            } else { throwIllegalDstArgument("variable should be Appendable", statement.variable, statement);
            }
        } }
      } else {
        outCmd = null;
      }
      
      if(cmdExecuter == null){ 
        cmdExecuter = new CmdExecuter(); 
        Map<String,String> env = cmdExecuter.environment();
        //Iterator<DataAccess.Variable<Object>> iter = localVariables.iterator("$");
        Iterator<Map.Entry<String, DataAccess.Variable<Object>>> iter = localVariables.entrySet().iterator(); //("$");
        
        boolean cont = true;
        //
        //gather all variables starting with "$" as environment variable. 
        //Note: Here usage of IndexMultiTable may be more proper, it can pre-select starting with "$".
        //But for this few variables if-checking is also proper. 
        //
        while(cont && iter.hasNext()){
          //DataAccess.Variable<Object> variable = iter.next();
          Map.Entry<String, DataAccess.Variable<Object>> e = iter.next();
          DataAccess.Variable<Object> variable = e.getValue();
          String name = variable.name();
          Object oValue = variable.value(); 
          if(name.startsWith("$") && oValue !=null){
            String value = oValue.toString();
            env.put(name.substring(1), value);
          } else {
            //removed cont=false, only applicable for IndexMultiTable. 
            //On TreeMap it would break on beginning too.
            //cont = false;  //only valid for IndexMultiTable
          }
        }
        cmdExecuter.setCurrentDir(this.currdir);  //from this executer level.
      }
      
      //
      //currdir
      //
      //
      //execute, run other process on operation system. With or without wait.
      //
      String[] sArgs = new String[args.size()]; 
      args.toArray(sArgs);
      jzcmdMain.runningCmdExecuter.offer(cmdExecuter);
      this.cmdErrorlevel = cmdExecuter.execute(sArgs, statement.bShouldNotWait, null, outCmd, null, null);
      
      jzcmdMain.runningCmdExecuter.remove(cmdExecuter);
      //
      //don't close for following commands.
      //
    }
    

    short exec_CmdError(JZtxtcmdScript.Onerror statement, StringFormatter out, int indentOut) throws Exception {
      short ret = 0;
      if(this.cmdErrorlevel >= statement.errorLevel){
        ret = execute(statement.statementlist, out, indentOut, localVariables, -1);
      }
      return ret;
    }


    short exec_DebugOp(JZtxtcmdScript.JZcmditem statement) throws Exception {
      short ret = 0;
      Object val = evalObject(statement, false);
      if(val instanceof String) {
        DataAccess.debugMethod((String)val);
      }
      return ret;
    }



    short exec_ChangeCurrDir(JZtxtcmdScript.JZcmditem statement)
    throws Exception
    {
      /*if(statement.dataAccess !=null){
        Object oDir = dataAccess(statement.dataAccess, localVariables, this.acc.bAccessPrivate, false, false, null);
        if(oDir instanceof File){
          
        }
      } else */{
        CharSequence arg = evalString(statement);
        if(arg == JZtxtcmdExecuter.retException){ return kException; }
        else {
          changeCurrDir(arg); 
          return kSuccess;
        }
      }
    }

    
    short exec_MkDir(JZtxtcmdScript.JZcmditem statement)
    throws Exception
    {
      CharSequence arg = evalString(statement);
      if(arg == JZtxtcmdExecuter.retException){ return kException; }
      else {
        if(!FileFunctions.isAbsolutePath(arg)){
          arg = this.currdir() + "/" + arg;
        }
        String arg1;
        if(StringFunctions.endsWith(arg, "'")) {  //should regard arg as filename, mk the directory of the file
          arg1 = arg.toString().substring(0,  arg.length()-1);  //mkdirpath ignores a file name 
        } else if(!StringFunctions.endsWith(arg, "/")){
          arg1 = arg + "/"; // mkDirPath needs a / on end to regard it as directory.
        } else {
          arg1 = arg.toString();  //it has a / on end.
        }
        FileFunctions.mkDirPath(arg1);  //note: if ends with / then it is a directory, else with filename.
        return kSuccess;
      }
    }

    
    /**Closes an existing text out and opens a new one with the given name in the current directory.
     * It is suppressed in initializing phase if another {@link JzTcMain#outFromCmdlineArg} is given than System.out.
     * Elsewhere it closes the given output and opens the new one if possible.
     * @param statement builds a path
     * @param outTextprev previously used textOut, it is returned if it is not changed. Because it is used locally in the script execution.
     * @param bAppend for <code>&lt;+:append>path/to...</code>
     * @return null on exception of {@link #evalString(org.vishia.cmd.JZtxtcmdScript.JZcmditem)} of the statement.
     *   elsewhere the new outText or the given outTextprev
     * @throws Exception
     * @since 2018-09 returns the new outText to use immediately
     */
    StringFormatter exec_OpenTextOut(JZtxtcmdScript.JZcmditem statement, StringFormatter outTextprev, boolean bAppend)
    throws Exception
    { if(jzcmdMain.outFromCmdlineArg != null && jzcmdMain.outFromCmdlineArg != System.out) {
        System.out.println("Info: exec_OpenTextOut skipped because outFromCmdLine is given.");
        return outTextprev;     //ignore the setting of textout in the script setting phase.
      } else {
        //CharSequence arg = evalObject(statement, false);  //evalString(statement);
        Object oarg = evalObject(statement, false);  //evalString(statement);
        Appendable out = null;
        if(oarg == JZtxtcmdExecuter.retException){ return null; }
        else if(oarg instanceof Appendable) {
          out = (Appendable) oarg;
        }
        else if(oarg instanceof CharSequence){
          CharSequence arg = (CharSequence)oarg;
          if(!FileFunctions.isAbsolutePath(arg)){
            arg = this.currdir() + "/" + arg;
          }
          out = new FileWriter(arg.toString(), bAppend);
        } else {
          throw new IllegalArgumentException("assignment to text should either an Appendable or a text which is the filename.");  
        }
        if(out !=null) {
          if(jzcmdMain.textline !=null) {
            jzcmdMain.textline.close();
          }
          jzcmdMain.textline =  new StringFormatter(out, true, null, 200);
          jzcmdMain.setScriptVariable("text", 'A', jzcmdMain.textline, true); 
          //return jzcmdMain.textline;
        }
        return jzcmdMain.textline;
      }
    }
    
    /**Executes the cd command: changes the directory in this execution level.
     * @param arg maybe a relative path. If it is a StringBuilder, it will be changed on normalizePath.
     * @throws IllegalAccessException 
     */
    protected void changeCurrDir(CharSequence arg) throws IllegalAccessException 
    {
      final CharSequence arg1;
      boolean absPath = FileFunctions.isAbsolutePathOrDrive(arg);
      if(absPath){
        //Change the content of the currdir to the absolute directory.
        arg1 = FileFunctions.normalizePath(arg);
      } else {
        if(this.currdir == null){
          //only on startup, start with operation system's current directory.
          this.currdir = new File("").getAbsoluteFile();
        }
        StringBuilder sCurrdir = new StringBuilder();
        sCurrdir.append(currdir.getPath()).append('/').append(arg);
        arg1 = FileFunctions.normalizePath(sCurrdir);
      }
      this.sCurrdir = arg1.toString();
      this.currdir = new File(this.sCurrdir);
      if(!currdir.exists() || !currdir.isDirectory()){
        throw new IllegalArgumentException("JZcmdExecuter - cd, dir not exists; " + arg);
      }
      setLocalVariable("currdir", 'O', this.currdir, true);
      //NOTE: don't do so, because it is global for the JVM (not desired!) but does not affect the operation system.
      //it has not any sense.
      //String sOldDir = System.setProperty("user.dir", currdir.getAbsolutePath());
      //No it has sense.
      //use new File(System.getProperty("user.dir"), sRelativePath) whenever a file is necessary.
      System.setProperty("user.dir", this.currdir.getAbsolutePath());
    }
    
     
    
    
    
    
    
    /**Inserts <&a_Datapath> in the out.
     * @param statement The statement which contains the access path etc.
     * @param out
     * @throws IllegalArgumentException
     * @throws Exception
     */
    private short exec_Datatext(JZtxtcmdScript.DataText statement, StringFormatter out, int indentOut, int nDebug)  //<*datatext>
    throws IllegalArgumentException, Exception
    { short success = kSuccess;
      CharSequence text = null;
      Object obj = null;
      try{ obj = dataAccess(statement.dataAccess, localVariables, jzcmdMain.bAccessPrivate, false, false, null);
      } catch(Exception exc) {
        if(statement.errorText !=null){
          text = statement.errorText;
        } else {
          throw exc;
        }
      }
      if(text !=null) { //set already.
      } else if(obj == JZtxtcmdExecuter.retException){ 
        success = kException; 
      }
      else {
        if(statement.format !=null){ //it is a format string:
            if(obj instanceof CalculatorExpr.Value){
              obj = ((CalculatorExpr.Value)obj).objValue();  
              //converted to boxed numeric if numeric.
              //boxed numeric is necessary for format
            }
            
            text = String.format(jzcmdMain.locale,  statement.format, obj);
        } else if(obj==null){ 
          text = null; //don't append if obj hasn't a content. 
        } else if (obj instanceof CharSequence){
          text = (CharSequence)obj;
        } else if(obj instanceof DataAccess.Variable) {
          DataAccess.Variable<?> variable = (DataAccess.Variable<?>) obj;
          if(variable.type() == 'X') { 
            //a Subtext or Statement block  
            @SuppressWarnings("unchecked") 
            DataAccess.Variable<JZtxtcmdScript.Subroutine> var = (DataAccess.Variable<JZtxtcmdScript.Subroutine>)obj;
            JZtxtcmdScript.Subroutine subroutine = var.value();
            if(!subroutine.useLocals) {
              throw new IllegalArgumentException("Subroutine as <&dataText> can only used without arguments.");
            }
            final ExecuteLevel sublevel = levelForSubroutine(subroutine);
            try{
              success = exec_Subroutine(subroutine, sublevel, null, null, out, indentOut, -1);
            } catch(Exception exc){
              success = kException;
            }
            //executes the statement of the Subtext and appends the content to out.
            //success = execute(statements, out, indentOut, localVariables, nDebug);
            text = null; //because it is appended already, text = null.
          } else {
            text = variable.value().toString();
          }
        } else if(obj instanceof CalculatorExpr.Value) {
          text = ((CalculatorExpr.Value)obj).stringValue();
        } else  {
          text = obj.toString();
        }
        if(text == null)
          Debugutil.stop();
        success = kSuccess;
      }
      if(text!=null){ out.append(text); }
      return success;
    }

    
    
    void exec_Move(JZtxtcmdScript.FileOpArg statement) 
    throws IllegalArgumentException, Exception
    {
      CharSequence s1 = evalString(statement.src);
      CharSequence s2 = evalString(statement.dst);
      File fileSrc = FileFunctions.isAbsolutePath(s1) ? new File(s1.toString()) : new File(currdir, s1.toString());
      File fileDst = FileFunctions.isAbsolutePath(s2) ? new File(s2.toString()) : new File(currdir, s2.toString());
      boolean bOk = fileSrc.renameTo(fileDst);
      if(!bOk) throw new IOException("JZcmd - move not successfully; " + fileSrc.getAbsolutePath() + " to " + fileDst.getAbsolutePath());;
    }
    
 
    
    void exec_Copy(JZtxtcmdScript.FileOpArg statement) 
    throws Exception
    { String ssrc, sdst;
      Object osrc = evalObject(statement.src, false);
      if(osrc instanceof JZtxtcmdFilepath){
        ssrc = ((JZtxtcmdFilepath)osrc).absfile().toString();
      } else {
        ssrc = osrc.toString();
      }
      Object odst = evalObject(statement.dst, false);
      if(odst instanceof JZtxtcmdFilepath){
        sdst = ((JZtxtcmdFilepath)odst).absfile().toString();
      } else {
        sdst = odst.toString();
      }
      int posWildcard = ssrc.indexOf('*');
      if(posWildcard>=0) {
        int posSep = ssrc.lastIndexOf('/', posWildcard);
        String sDirSrc = ssrc.substring(0, posSep);
        if(!FileFunctions.isAbsolutePath(sDirSrc)){
          sDirSrc = sCurrdir + '/' + sDirSrc;
        }
        if(!FileFunctions.isAbsolutePath(sdst)){
          sdst = sCurrdir + '/' + sdst;
        }
        String sMask = ssrc.substring(posSep +1);
        FileRemote dirSrc = FileRemote.getDir(sDirSrc);
        FileRemote dirDst = FileRemote.getDir(sdst);
        //====>
        dirSrc.copyDirTreeTo(dirDst, 0, sMask, 0, null, null);
        //
      } else {
        File src = FileFunctions.isAbsolutePath(ssrc) ? new File(ssrc.toString()) : new File(currdir, ssrc.toString());
        File dst = FileFunctions.isAbsolutePath(sdst) ? new File(sdst.toString()) : new File(currdir, sdst.toString());
        
        //CharSequence s1 = evalString(statement.actualArgs.get(0));
        //CharSequence s2 = evalString(statement.actualArgs.get(1));
        int nrofBytes = FileFunctions.copyFile(src, dst, statement.bNewTimestamp, statement.bOverwrite, statement.bOverwriteReadonly);
        if(nrofBytes <0) throw new FileNotFoundException("JbatchExecuter - copy src not found; " + src.getAbsolutePath() + " to " + dst.getAbsolutePath());
      }
    }
    
    
    
    
    void exec_Delete(JZtxtcmdScript.FileOpArg statement) 
    throws Exception
    {
      CharSequence s1 = evalString(statement.src);
      String fileSrc = FileFunctions.isAbsolutePath(s1) ? s1.toString() : currdir() + "/" + s1;
      boolean isDeleted = FileFunctions.delete(fileSrc);
      if(!isDeleted) throw new FileNotFoundException("JbatchExecuter - del not possible; " + s1);;
    }
    
    /**Creates a new FileWriter with the given name {@link #evalString(org.vishia.cmd.JZtxtcmdScript.Argument)}
     * of the statement. If the file name is local, the actual value of $CD is set as pre-path.
     * The current directory of the file system is not proper to use because the current directory of this 
     * execution level should be taken therefore. If the path is faulty or another exception is thrown,
     * the exception is forwarded to the execution level (onerror-statement).
     * @param statement
     * @throws IllegalArgumentException
     * @throws Exception
     */
    short exec_Openfile(Map<String, DataAccess.Variable<Object>> newVariables, JZtxtcmdScript.DefVariable statement) 
    throws IllegalArgumentException, Exception
    {
      CharSequence sqFilename = evalString(statement);
      if(sqFilename == JZtxtcmdExecuter.retException){ return kException; }
      else {
        String sFilename = sqFilename.toString();
        Writer writer;
        if(!FileFunctions.isAbsolutePath(sFilename)){
          //build an absolute filename with $CD, the current directory of the file system is not proper to use.
          
          File fWriter = new File(currdir, sFilename);
          writer = new FileWriter(fWriter, statement.bAppendToFile);
        } else {
          writer = new FileWriter(sFilename, statement.bAppendToFile);  //given absolute path
        }
        storeValue(statement.defVariable, newVariables, writer, jzcmdMain.bAccessPrivate);
        //setLocalVariable(statement.identArgJbat, 'A', writer);
        return kSuccess;
      }
    }
    
    
    
    private short assignStatement(JZtxtcmdScript.JZcmditem statement) throws IllegalArgumentException, Exception{
      //Object val = evalObject(statement, false);
      return assignObj((JZtxtcmdScript.AssignExpr)statement, null, true); //val); 
    }
    
    
    
    
    
    /**Executes a <code>assignment::= [{ < datapath?assign > = }] < expression > ;.</code>.
     * If the datapath to assign is only a localVariable (one simple name), then the expression
     * is assigned to this local variable, a new variable will be created.
     * If the datapath to assign is more complex, the object which is described with it
     * will be gotten. Then an assignment will be done depending on its type:
     * <ul>
     * <li>Appendable: appends the gotten expression.toString(). An Appendable may be especially
     * <li>All others cause an error. 
     * </ul>
     * @param statement
     * @param val value to assign. If not null then bEval should be false.
     * @param bEval if true then the value will be evaluate from the statement.
     * @throws IllegalArgumentException
     * @throws Exception
     */
    private short assignObj(JZtxtcmdScript.AssignExpr statement, Object val, boolean bEval) 
    throws IllegalArgumentException, Exception
    {
      CalculatorExpr.Value value = null;
      Object oVal = val;
      short ret = kSuccess;
      Boolean cond = null;
      JZtxtcmdScript.JZcmdDataAccess assignObj1 = statement.variable;
      if(assignObj1 ==null){
        //no assignment, only an expression which is a procedure call:
        Object oRet = evalObject(statement, false);
        if(oRet == JZtxtcmdExecuter.retException){ ret = kException; }
        else { ret = kSuccess; }
      }
      Iterator<JZtxtcmdScript.JZcmdDataAccess> iter1 = statement.assignObjs == null ? null : statement.assignObjs.iterator();
      while(assignObj1 !=null) {
        //Object dst = assignObj1.getDataObj(localVariables, this.acc.bAccessPrivate, false);
        DataAccess.Dst dstField = new DataAccess.Dst();
        //List<DataAccess.DatapathElement> datapath = assignObj1.datapath(); 
        //Object dst = DataAccess.access(datapath, null, localVariables, this.acc.bAccessPrivate, false, true, dstField);
        Object dst = dataAccess(assignObj1, localVariables, jzcmdMain.bAccessPrivate, false, true, dstField);
        if(dst instanceof DataAccess.Variable<?>){
          @SuppressWarnings("unchecked")
          DataAccess.Variable<Object> var = (DataAccess.Variable<Object>) dst; //assignObj1.accessVariable(localVariables, this.acc.bAccessPrivate);
          char vartype = var.type();
          switch(vartype){
            case 'K': if(value == null){ value = evalValue(statement, false); } break;
            case 'Q': if(cond == null){ cond = new Boolean(evalCondition(statement)); } break;
            default: if(oVal == null){ oVal = evalObject(statement, false); }
          }
          
          switch(var.type()){
            case 'A': {
              throwIllegalDstArgument("assign to appendable faulty", assignObj1, statement); 
            } break;
            case 'U': {
              dst = var.value();
              assert(dst instanceof StringPartAppend);            
              StringPartAppend u = (StringPartAppend) dst;
              u.clear();
              Object ocVal = oVal == null ? "--null--" : oVal;
              if(!(ocVal instanceof CharSequence)){
                ocVal = ocVal.toString();
              }
              u.append((CharSequence)ocVal);
            } break;
            case 'S':{
              if(oVal == null || val instanceof String || val instanceof StringSeq && ((StringSeq)val).isUnmated()){
                var.setValue(oVal);
              } else {
                var.setValue(oVal.toString());
              }
            } break;
            case 'K': var.setValue(value); break;
            case 'Q': var.setValue(cond); break;
            default:{
              var.setValue(oVal);   //sets the value to the variable.
            }
          }//switch
        } else {
          //check whether the field is compatible with val
          if(oVal == null){ 
            oVal = evalObject(statement, false); 
            if(oVal == JZtxtcmdExecuter.retException){ ret = kException; }
          }
          dstField.set(oVal);
          //DataAccess.storeValue(assignObj1.datapath(), newVariables, val, this.acc.bAccessPrivate);
        }
        if(iter1 !=null && iter1.hasNext()){
          assignObj1 = iter1.next();
        } else {
          assignObj1 = null;
        }
      }
      return ret;
    }
    
    
    
    
    
    
    /**Executes a <code>DefObjVar::= < variable?defVariable>  [ = < objExpr?>]</code>.
     * If the datapath to assign is only a localVariable (one simple name), then the expression
     * is assigned to this local variable, a new variable will be created.
     * If the datapath to assign is more complex, the object which is described with it
     * will be gotten. Then an assignment will be done depending on its type:
     * <ul>
     * <li>Appendable: appends the gotten expression.toString(). An Appendable may be especially
     * <li>All others cause an error. 
     * </ul>
     * @param statement
     * @throws IllegalArgumentException
     * @throws Exception
     */
    short defineExpr(Map<String, DataAccess.Variable<Object>> newVariables, JZtxtcmdScript.DefVariable statement) 
    throws IllegalArgumentException, Exception
    {
      Object init = evalObject(statement, false);  //maybe null
      if(init == JZtxtcmdExecuter.retException){
        return kException;
      } else {
        Object val;
        switch(statement.elementType()){ //type of container to store, StringJar or String
          case 'U': {
            if(init == null){
              val = new StringPartAppend();
            } else {
              CharSequence init1 = init instanceof CharSequence ? (CharSequence)init : init.toString();
              StringPartAppend u = new StringPartAppend();
              u.append(init1);
              val = u;
            }
          } break;
          case 'S':{
            if(init == null || init instanceof String /*|| init instanceof StringSeq && ((StringSeq)init).isUnmated()*/){
              val = init;
            } else {
              val = init.toString();
            }
          } break;
          default: val = init;
        }
        //DataAccess.Variable var = (DataAccess.Variable)DataAccess.access(statement.defVariable.datapath(), null, localVariables, this.acc.bAccessPrivate, false, true, null);
        //var.setValue(val);
        List<DataAccess.DatapathElement> datapath = statement.defVariable.datapath();  //datapath of the variable to store.
        if(datapath.get(0).ident().equals("return") && !localVariables.containsKey("return")) {
          //
          //creates the local variable return on demand:
          DataAccess.Variable<Object> ret = new DataAccess.Variable<Object>('M', "return", jzcmdMain.new_Variables());
          localVariables.put("return", ret); //.add("return", ret);
        }
        storeValue(statement.defVariable, newVariables, val, jzcmdMain.bAccessPrivate);
        if(cmdExecuter !=null){
          String name = statement.defVariable.datapath().get(0).ident();
          if(name.startsWith("$")){
            //an environment variable
            cmdExecuter.environment().put(name,val.toString());
        } }
        return kSuccess;
      }
    }
    
    
    
    /**Executes a <code>appendExpr::= [{ < datapath?assign > += }] < expression > ;.</code>.
     * @param statement
     * @throws IllegalArgumentException
     * @throws Exception
     */
    short appendExpr(JZtxtcmdScript.AssignExpr statement) 
    throws IllegalArgumentException, Exception
    {
      short ret;
      Object val = evalObject(statement, false);
      if(val == JZtxtcmdExecuter.retException){ ret = kException; }
      else { ret = kSuccess; }
      JZtxtcmdScript.JZcmdDataAccess assignObj1 = statement.variable;
      Iterator<JZtxtcmdScript.JZcmdDataAccess> iter1 = statement.assignObjs == null ? null : statement.assignObjs.iterator();
      while(assignObj1 !=null) {
        Object dst = dataAccess(assignObj1, localVariables, jzcmdMain.bAccessPrivate, false, false, null);
        //Object dst = assignObj1.getDataObj(localVariables, this.acc.bAccessPrivate, false);
        if(dst instanceof Appendable){
          if(!(val instanceof CharSequence)){
            val = val.toString();
          }
          ((Appendable) dst).append((CharSequence)val);
        } else if(dst instanceof List<?>){
          @SuppressWarnings("unchecked")
          List<Object> list = (List<Object>)dst; 
          if(val instanceof List<?>){
            @SuppressWarnings("unchecked")
            List<Object> listval = (List<Object>)val;
            for(Object entry: listval){
              list.add(entry);
            }
          } else {
            list.add(val);
          }
        } else {
          throwIllegalDstArgument("dst should be Appendable", assignObj1, statement);
        }
        if(iter1 !=null && iter1.hasNext()){
          assignObj1 = iter1.next();
        } else {
          assignObj1 = null;
        }
      }
      return ret;
    }
    
    
    
    void exec_Throw(JZtxtcmdScript.JZcmditem statement) throws Exception {
      CharSequence msg = evalString(statement);
      throw new JZcmdThrow(msg.toString());
    }
    
    
    
    /**Throws an {@link CmdErrorLevelException} if the errorlevel is >= statement.errorLevel.
     * @param statement
     * @throws CmdErrorLevelException
     */
    void exec_Throwonerror(JZtxtcmdScript.Onerror statement) throws CmdErrorLevelException{
      if(cmdErrorlevel >= statement.errorLevel){
        throw new CmdErrorLevelException(cmdErrorlevel);
      }
    }
    
    
    /**Checks either the {@link JZtxtcmdScript.Argument#dataAccess} or, if it is null,
     * the {@link JZtxtcmdScript.Argument#expression}. Returns either the Object which is gotten
     * by the {@link DataAccess#access(Map, boolean, boolean)} or which is calculated
     * by the expression. Returns an instance of {@link CalculatorExpr.Value} if it is 
     * a result by an expression.
     * @param arg
     * @return
     * @throws Exception
     */
    public Object evalDatapathOrExpr(JZtxtcmdScript.Argument arg) throws Exception{
      if(arg.dataAccess !=null){
        Object o = dataAccess(arg.dataAccess, localVariables, jzcmdMain.bAccessPrivate, false, false, null);
        //Object o = arg.dataAccess.getDataObj(localVariables, this.acc.bAccessPrivate, false);
        if(o==null){ return "null"; }
        else {return o; }
      } else if(arg.expression !=null){
        CalculatorExpr.Value value = calculateExpression(arg.expression); //.calcDataAccess(localVariables);
        if(value.isObjValue()){ return value.objValue(); }
        else return value;
      } else throw new IllegalArgumentException("JZcmd - unexpected, faulty syntax");
    }
    
    
    

    
    public CharSequence evalString(JZtxtcmdScript.JZcmditem arg) 
    throws Exception 
    {
      if(arg.textArg !=null) return arg.textArg;
      else if(arg.dataAccess !=null){
        Object o = dataAccess(arg.dataAccess, localVariables, jzcmdMain.bAccessPrivate, false, false, null);
        //Object o = arg.dataAccess.getDataObj(localVariables, this.acc.bAccessPrivate, false);
        if(o==null){ return "null"; }
        else if(o instanceof CharSequence){ return (CharSequence)o; }  //maybe retException
        else {return o.toString(); }
      } else if(arg.statementlist !=null){
        StringFormatter u = new StringFormatter();
        //StringPartAppend u = new StringPartAppend();
        short ret = executeNewlevel(arg.statementlist, u, 0, -1);
        if(ret == kException){ return JZtxtcmdExecuter.retException; }
        else { return u.getBuffer(); }
      } else if(arg.expression !=null){
        CalculatorExpr.Value value = calculateExpression(arg.expression); //.calcDataAccess(localVariables);
        return value.stringValue();
      } else return null;  //it is admissible.
      //} else throw new IllegalArgumentException("JZcmd - unexpected, faulty syntax");
    }
    
    
    
    /**Accesses the data. Before execution all arguments of methods inside the {@link #datapath()}
     * are calculated with the capability of JZcmd.
     * @param dataPool
     * @param accessPrivate
     * @param bContainer
     * @param bVariable
     * @param dst
     * @return
     * @throws Exception
     */
    Object dataAccess(JZtxtcmdScript.JZcmdDataAccess dataAccess
    , Map<String, DataAccess.Variable<Object>> dataPool
    , boolean accessPrivate
    , boolean bContainer
    , boolean bVariable
    , DataAccess.Dst dst
    ) throws Exception {
      /*if(dataAccess.filepath !=null){
        //Special case in JZcmd: a "File: "
        if(FileFunctions.isAbsolutePath(dataAccess.filepath)){
          return new File(dataAccess.filepath);
        } else {
          return new File(currdir, dataAccess.filepath);
        }
      } else */{
        calculateArguments(dataAccess);  //resolves an indirectDatapath too.
        return DataAccess.access(dataAccess.datapath(), dataPool, accessPrivate, bContainer, null, null, bVariable, dst);
      }
    }
      
    
    
    void storeValue(DataAccess dataAccess
    , Map<String, DataAccess.Variable<Object>> dataPool
    , Object value
    , boolean accessPrivate
    )throws Exception {
      calculateArguments(dataAccess);
      dataAccess.storeValue(dataPool, value, accessPrivate);
    }

    
    
    /**This routine invokes {@link CalculatorExpr#calcDataAccess(Map, Object...)}, but before
     * the arguments of all operations will be calculated if necessary.
     * All operations are gotten with {@link CalculatorExpr#listOperations()}.
     * with that dataPath  {@link #calculateArguments(DataAccess)} is called.
     * with 
     * @param expr
     * @return
     * @throws Exception
     */
    private CalculatorExpr.Value calculateExpression(CalculatorExpr expr) 
    throws Exception {
      for(CalculatorExpr.Operation operation: expr.listOperations()){
        DataAccess datapath = operation.datapath();
        if(datapath !=null){
          calculateArguments(datapath);
        }
      }
      return expr.calcDataAccess(localVariables);
    }
    
    
    
    
    private void calculateArguments(DataAccess dataAccess) throws Exception {
      if(debug_dataAccessArguments){
        debug(); //set breakpoint into!
        debug_dataAccessArguments = false;
      }
      for(DataAccess.DatapathElement dataElement : dataAccess.datapath()){  //loop over all elements of the path with or without arguments.
        //check all datapath elements whether they have method calls with arguments:
        List<JZtxtcmdScript.JZcmditem> fnArgsExpr = null;

        if(  dataElement instanceof JZtxtcmdScript.JZcmdDatapathElementClass){
          JZtxtcmdScript.JZcmdDatapathElementClass jzcmdDataElement = (JZtxtcmdScript.JZcmdDatapathElementClass)dataElement;
          if(jzcmdDataElement.dpathLoader !=null){
            Object oLoader = dataAccess(jzcmdDataElement.dpathLoader, localVariables, jzcmdMain.bAccessPrivate, false, false, null);
            assert(oLoader instanceof ClassLoader);
            jzcmdDataElement.set_loader((ClassLoader)oLoader);
          }
          if(jzcmdDataElement.dpathClass !=null){
            Object o = dataAccess(jzcmdDataElement.dpathClass, localVariables, jzcmdMain.bAccessPrivate, false, false, null);
            assert(o instanceof Class<?>);
            jzcmdDataElement.set_Class((Class<?>)o);
          }
          fnArgsExpr = jzcmdDataElement.fnArgsExpr;
        }
        if(  dataElement instanceof JZtxtcmdScript.JZcmdDatapathElement ){
          JZtxtcmdScript.JZcmdDatapathElement jzcmdDataElement = (JZtxtcmdScript.JZcmdDatapathElement)dataElement;
          if(jzcmdDataElement.indirectDatapath !=null){
            Object oIdent = dataAccess(jzcmdDataElement.indirectDatapath, localVariables, jzcmdMain.bAccessPrivate, false, false, null);
            dataElement.setIdent(oIdent.toString());
          }
          fnArgsExpr = jzcmdDataElement.fnArgsExpr;
        }
        if(fnArgsExpr !=null) {
          int nrofArgs = fnArgsExpr.size();
          Object[] args = new Object[nrofArgs];
          int iArgs = -1;
          for(JZtxtcmdScript.JZcmditem expr: fnArgsExpr){
            Object arg = evalObject(expr, false);
            args[++iArgs] = arg;
          }
          dataElement.setActualArgumentArray(args);
        }
      }

    }
    
    
    
    /**Gets the value of the given Argument. Either it is a 
     * <ul>
     * <li>String from {@link JZtxtcmdScript.JZcmditem#textArg}
     * <li>Object from {@link JZtxtcmdScript.JZcmditem#dataAccess}
     * <li>String from {@link JZtxtcmdScript.JZcmditem#statementlist} if not 'M'
     * <li>Map<String, Object> from statementlist if the arg.{@link JZtxtcmdScript.JZcmditem#elementType} == 'M'.
     *   it is d 'dataStruct'. 
     * <li>Object from {@link JZtxtcmdScript.JZcmditem#expression}.
     * <li>If the arg is instanceof {@link JZtxtcmdScript.Argument} - an argument of a call subroutine, then
     *   <ul>
     *   <li>If the {@link JZtxtcmdScript.Argument#filepath} is set, then that {@link FilePath} is transfered
     *     to a {@link JZtxtcmdFilepath} and returned. 
     *     A {@link JZtxtcmdFilepath} contains the reference to this execution level.
     *   <li>If the {@link JZtxtcmdScript.Argument#accessFileset} is set, then that {@link JZtxtcmdScript.AccessFilesetname}
     *     is transfered to an instance of {@link JZtxtcmdAccessFileset} and that instance is returned.   
     * </ul>
     * @param arg
     * @return Object adequate the arg, maybe null
     * @throws Exception any Exception while evaluating.
     */
    public Object evalObject(JZtxtcmdScript.JZcmditem arg, boolean bContainer) throws Exception{
      Object obj;
      short ret = 0;
      if(arg.textArg !=null) obj = arg.textArg;
      else if(arg.dataAccess !=null){
        //calculate arguments firstly:
        obj = dataAccess(arg.dataAccess, localVariables, jzcmdMain.bAccessPrivate, false, false, null);
        //obj = arg.dataAccess.getDataObj(localVariables, this.acc.bAccessPrivate, false);
      } else if(arg.statementlist !=null){
        if(arg.elementType == 'M') {  
          //a dataStruct
          final ExecuteLevel level = new ExecuteLevel(jzcmdMain, this.subRoutine, this.jzClass, threadData, this, localVariables);
          Map<String, DataAccess.Variable<Object>> newVariables = 
            new TreeMap<String, DataAccess.Variable<Object>>(); 
            //new IndexMultiTable<String, DataAccess.Variable<Object>>(IndexMultiTable.providerString); 
          //fill the dataStruct with its values:
          ret = level.execute(arg.statementlist, null, 0, newVariables, -1); //Note: extra newVariables
          obj = ret == kException ? JZtxtcmdExecuter.retException: newVariables;
        } else {
          //A statementlist as object can be only a text expression.
          //its value is returned as String.
          StringFormatter u = new StringFormatter();
          ret = executeNewlevel(arg.statementlist, u, 0, -1);
          obj = ret == kException ? JZtxtcmdExecuter.retException:  u.toString();
        }
      } else if(arg.expression !=null){
        CalculatorExpr.Value value = calculateExpression(arg.expression); //.calcDataAccess(localVariables);
        obj = value.objValue();
      } else {
        obj = null;  //throw new IllegalArgumentException("JZcmd - unexpected, faulty syntax");
      }
      if(obj !=null && ret != kException && arg.conversion !=0){
        switch(arg.conversion){
          case '~': {
            if(!FileFunctions.isAbsolutePath(obj.toString())) {
              obj = currdir() + "/" + obj;
            }
          } break;
          case 'E': { //TODO not used:
            String value = obj.toString();
            if(FileFunctions.isAbsolutePath(value)){
              obj = new File(value);
            } else {
              obj = new File(currdir, value);
            }
          } break;
          case 'F': {
            String value = obj.toString();
            obj = new JZtxtcmdFilepath(this, value);
          } break;
          case 'G': {
            assert(arg.subitem instanceof JZtxtcmdScript.AccessFilesetname);
            JZtxtcmdScript.AccessFilesetname arg1 = (JZtxtcmdScript.AccessFilesetname) arg.subitem;
            obj = new JZtxtcmdAccessFileset(arg1, this);
          } break;
          default: assert(false);
        }
      }
      return obj;
    }
    
    
    
    
    /**Gets the value of the given Argument. It is a {@link JZtxtcmdScript.JZcmditem#expression}
     * @param arg
     * @return
     * @throws Exception
     */
    public CalculatorExpr.Value evalValue(JZtxtcmdScript.JZcmditem arg, boolean bContainer) throws Exception{
      if(arg.textArg !=null){
        return null;  //TODO
      }
      else if(arg.dataAccess !=null){
        CalculatorExpr.Value value;
        Object obj = dataAccess(arg.dataAccess, localVariables, jzcmdMain.bAccessPrivate, false, false, null);
        if(obj instanceof Float){
          value = new Value(((Float)obj).floatValue());
        } else if(obj instanceof Double){
          value = new Value(((Double)obj).doubleValue());
        } else if(obj instanceof Long){
          value = new Value(((Long)obj).longValue());
        } else if(obj instanceof Integer){
          value = new Value(((Integer)obj).intValue());
        } else if(obj instanceof Short){
          value = new Value(((Short)obj).shortValue());
        } else if(obj instanceof Byte){
          value = new Value(((Byte)obj).byteValue());
        } else if(obj instanceof Boolean){
          value = new Value(((Boolean)obj).booleanValue());
        } else if(obj instanceof CalculatorExpr.Value) {
          value = (CalculatorExpr.Value) obj;  //the obj itself is the value.
        } else {
          value = new Value(obj);
        }
        return value;
      } else if(arg.expression !=null) {
        return calculateExpression(arg.expression); 
      } else {
        return null;  //no value given.
      }
    }
    
    
    
    
    public boolean evalCondition(JZtxtcmdScript.JZcmditem arg) throws Exception{
      boolean ret;
      if(arg.textArg !=null) return true;
      else if(arg.dataAccess !=null){
        try{
          Object obj = dataAccess(arg.dataAccess, localVariables, jzcmdMain.bAccessPrivate, false, false, null);
          //if(obj instanceof Number){
          //  ret = ((Number)obj).intValue() !=0;
          //} else 
          if(obj instanceof Boolean) {
            ret = ((Boolean)obj).booleanValue();
          } else if(obj instanceof Integer) {  //conversion from int to boolean
            ret = ((Integer)obj).intValue() != 0;
          } else {
            ret = obj !=null;          //else: conversion null or not null to boolean
          }
        } catch(NoSuchElementException exc){
          ret = false;
        } catch(NoSuchFieldException exc){
          ret = false;
        } catch(NoSuchMethodException exc){
          ret = false;
        }
      } else if(arg.statementlist !=null){
        throw new IllegalArgumentException("JZcmdExecuter - unexpected, faulty syntax");
      } else if(arg.expression !=null){
        CalculatorExpr.Value value = calculateExpression(arg.expression); //.calcDataAccess(localVariables);
        ret = value.booleanValue();
      } else throw new IllegalArgumentException("JZcmdExecuter - unexpected, faulty syntax");
      return ret;
      
    }
    

    
    
    /**Check whether osrc is a filepath, or create one.
     * @param osrc
     * @return
     */
    JZtxtcmdFilepath convert2FilePath(Object osrc)
    { JZtxtcmdFilepath ret;
      if(osrc == null){
        ret = null;
      }
      else if(osrc instanceof JZtxtcmdFilepath){
        ret = (JZtxtcmdFilepath)osrc;
      } else {
        String src = osrc.toString();
        ret = new JZtxtcmdFilepath(this, new FilePath(src));
      }
      return ret;
    }
    
    
    protected void runThread(ExecuteLevel executeLevel, JZtxtcmdScript.ThreadBlock statement, JZtxtcmdThreadData threadVar){
      try{
        executeLevel.execute(statement.statementlist, jzcmdMain.textline, 0, executeLevel.localVariables, -1);
      } 
      catch(Exception exc){
        threadVar.exception = exc;
        //finishes the thread.
      }
    }
    

    protected void finishThread(JZtxtcmdThreadData thread){
      if(thread.exception !=null) {
        CharSequence excInfo = excStacktraceinfo();
        System.err.println(excInfo);   //show it anywhere, should be visible
      }
      synchronized(thread){
        thread.notifyAll();   //any other thread may wait for join
      }
      synchronized(jzcmdMain.threads){  //remove this thread from the list of threads.
        boolean bOk = jzcmdMain.threads.remove(thread);
        assert(bOk);
        if(jzcmdMain.threads.size() == 0){
          jzcmdMain.threads.notify();    //notify the waiting main thread to finish.
        }
      }

    }


    

    
    /**Returns the absolute canonical path of the currdir variable. currdir is a {@link File}.
     * The file separator is '/'.
     */
    public String currdir(){ return currdir.getPath().replace('\\', '/'); }
    
    
    int debug(JZtxtcmdScript.JZcmditem statement) //throws Exception
    { try{ 
        CharSequence text = evalString(statement);
        if(text !=null ){
          System.out.println(text.toString());
        }
      } catch(Exception exc){ //unexpected
      }
      Assert.stop();
      return 1;
    }
    
    
    public void debug_dataAccessArguments(){ debug_dataAccessArguments = true; }
    
    
    void debug(){
      Assert.stop();
    }
    

    
    /**Returns a one-line-information about the last Exception with Stacktrace information (max 20 levels).
     */
    public CharSequence excStacktraceinfo(){    
      CharSequence sExc = Assert.exceptionInfo("JZcmd.execute - exception at;" + threadData.excStatement.toString() + ";", threadData.exception, 0, 20);
      return sExc;
    }
    
    
    void throwIllegalDstArgument(CharSequence text, DataAccess dst, JZtxtcmdScript.JZcmditem statement)
    throws IllegalArgumentException
    {
      StringBuilder u = new StringBuilder(100);
      u.append("JZcmd - ").append(text).append(";").append(dst);
      u.append("; in file ").append(statement.parentList.srcFile);
      u.append(", line ").append(statement.srcLine).append(" col ").append(statement.srcColumn);
      throw new IllegalArgumentException(u.toString());
    }



    @Override
    public Object getAttribute(String name)
    { DataAccess.Variable<Object> var = localVariables.get(name);
      if(var == null) return null;
      else return var.value();
    }



    @Override
    public Object getAttribute(String name, int scope)
    { switch(scope){
        case ScriptContext.ENGINE_SCOPE: return getAttribute(name); 
        case ScriptContext.GLOBAL_SCOPE: return jzcmdMain.scriptLevel.getAttribute(name); 
        default: throw new IllegalArgumentException("JZcmdExecuter.getAttribute - failed scope;" + scope);
      } //switch
    }



    @Override
    public int getAttributesScope(String name)
    {
      // TODO
      return ScriptContext.ENGINE_SCOPE;
    }



    @Override
    public Bindings getBindings(int scope)
    { switch(scope){
      case ScriptContext.ENGINE_SCOPE: return new JZcmdBindings(localVariables); 
      case ScriptContext.GLOBAL_SCOPE: return new JZcmdBindings(jzcmdMain.scriptLevel.localVariables); 
      default: throw new IllegalArgumentException("JZcmdExecuter.getBindings - failed scope;" + scope);
    } //switch
  }



    @Override
    public Writer getErrorWriter()
    {
      // TODO Auto-generated method stub
      return null;
    }



    @Override
    public Reader getReader()
    {
      // TODO Auto-generated method stub
      return null;
    }



    @Override
    public List<Integer> getScopes()
    {
      // TODO Auto-generated method stub
      return null;
    }



    @Override
    public Writer getWriter()
    {
      // TODO Auto-generated method stub
      return null;
    }



    @Override
    public Object removeAttribute(String name, int scope)
    {
      // TODO Auto-generated method stub
      return null;
    }



    @Override
    public void setAttribute(String name, Object value, int scope)
    {
      // TODO Auto-generated method stub
      
    }



    @Override
    public void setBindings(Bindings bindings, int scope)
    {
      // TODO Auto-generated method stub
      
    }



    @Override
    public void setErrorWriter(Writer writer)
    {
      // TODO Auto-generated method stub
      
    }



    @Override
    public void setReader(Reader reader)
    {
      // TODO Auto-generated method stub
      
    }



    @Override
    public void setWriter(Writer writer)
    {
      // TODO Auto-generated method stub
      
    }
    
    
    /* (non-Javadoc)
     * @see org.vishia.util.FilePath.FilePathEnvAccess#getCurrentDir()
     */
    @Override public CharSequence getCurrentDir()
    {
      return currdir();
    }




    /* (non-Javadoc)
     * @see org.vishia.util.FilePath.FilePathEnvAccess#getValue(java.lang.String)
     */
    @Override public Object getValue(String variable) throws NoSuchFieldException
    { Object oValue;
      DataAccess.Variable<Object> varV = localVariables.get(variable);
      if(varV == null){
        if(variable.startsWith("$")){
          oValue = System.getenv(variable.substring(1)).replace('\\', '/');  
        } else {
          oValue = null;
        }
        if(oValue == null) {
          throw new NoSuchFieldException("JZcmdFilepath.getValue() - variable not found; " + variable);
        } 
      } else {
        oValue = varV.value();
        if(oValue instanceof JZtxtcmdFilepath){
          oValue = ((JZtxtcmdFilepath)oValue).data;  //the FilePath instance.
        } 
      }
      return oValue;
    }

    
    @Override public void close() {
      if(this.cmdExecuter !=null) {
        this.cmdExecuter.close();  //close its threads, or continue and close threads itself.
        this.cmdExecuter = null;
      }
    }

    
  }  
  
  
  /**Simple class to build a countered number. 
   * toString() increments and returns the number.
   */
  public static class NextNr {
    private int nr = 0;
    
    /**Start method, next access returns 1. */
    void start(){ nr = 0; }
    
    /**Set method, next access returns value. */
    void set(int value){ nr = value -1; }  //NOTE: pre-increment.
    
    /**Increments and returns the number. */
    @Override public String toString(){
      return "" + ++nr;
    }
  };
  
  
  
  
  
  
  void stop(){
    
    
  }

  
  
  /**A thread instance of JZcmd.
   */
 // protected class JZcmdThread  
  
  public static class CmdErrorLevelException extends Exception
  {
    private static final long serialVersionUID = 7785185972638755384L;
    
    public int errorLevel;
    
    public CmdErrorLevelException(int errorLevel){
      super("cmd error level = " + errorLevel);
      this.errorLevel = errorLevel;
    }
  }
  
  
  public static class JZcmdThrow extends Exception
  {
    private static final long serialVersionUID = 1L;

    public JZcmdThrow(String text){
      super(text);
    }
  }
  
  
  public static class ExitException extends Exception
  {
    private static final long serialVersionUID = 1L;
    
    public int exitLevel;
    
    public ExitException(int exitLevel){
      this.exitLevel = exitLevel;
    }
  }
  
  
  
  protected static class JzScriptException extends ScriptException
  {
    JzScriptException( String message, String fileName, int lineNumber, int columnNumber) 
    { super(message, fileName, lineNumber, columnNumber); }
  
    JzScriptException( Exception exc) 
    { super(exc); }
  
    JzScriptException( String exc) 
    { super(exc); }
  
    public final String getMessage(){
      String sMessage = super.getMessage(); //hint: toString() causes recursion.
      sMessage += "\n@line:col " + super.getLineNumber() + ':' + super.getColumnNumber() + " in " + super.getFileName();
      return sMessage;
    }
  
  }
  
  
  
  /**
  *
  */
 protected static class JZcmdBindings implements Bindings 
 { 
   private final Map<String, DataAccess.Variable<Object>> vars;

   public JZcmdBindings(Map<String, DataAccess.Variable<Object>> vars)
   { this.vars = vars;
   }

   @Override
   public Object put(String name, Object value)
   {
     DataAccess.Variable<Object> var = new DataAccess.Variable<Object>('O', name, value, false);
     return vars.put(name, var);
   }

    @Override
    public boolean containsKey(Object key){ return vars.containsKey(key); }
  
    @Override
    public Object get(Object key)
    { DataAccess.Variable<Object> var = vars.get(key);
      return var == null ? null : var.value();
    }
  
    @Override
    public void putAll(Map<? extends String, ? extends Object> toMerge)
    {
      // TODO Auto-generated method stub
      
    }
  
    @Override
    public Object remove(Object key)
    {
      // TODO Auto-generated method stub
      return null;
    }
  
    @Override
    public void clear()
    {
      // TODO Auto-generated method stub
      
    }
  
    @Override
    public boolean containsValue(Object value)
    {
      // TODO Auto-generated method stub
      return false;
    }
  
    @Override
    public Set<java.util.Map.Entry<String, Object>> entrySet()
    {
      // TODO Auto-generated method stub
      return null;
    }
  
    @Override
    public boolean isEmpty()
    {
      // TODO Auto-generated method stub
      return false;
    }
  
    @Override
    public Set<String> keySet()
    {
      // TODO Auto-generated method stub
      return null;
    }
  
    @Override
    public int size()
    {
      // TODO Auto-generated method stub
      return 0;
    }
  
    @Override
    public Collection<Object> values()
    {
      // TODO Auto-generated method stub
      return null;
    }
     
  }
 
 
  
  
  
  
}
