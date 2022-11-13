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
 * @author JcHartmut = hartmut.schorrig@vishia.de
 *
 ****************************************************************************/

package org.vishia.mainCmd;

//import org.jdom;
import java.io.*;
import java.util.*;  //List
import java.text.*;  //ParseException

import org.vishia.util.FileSystem;
import org.vishia.util.StringFunctions;
import org.vishia.bridgeC.OS_TimeStamp;
import org.vishia.bridgeC.Va_list;
import org.vishia.cmd.CmdExecuter;
import org.vishia.msgDispatch.LogMessage;
import org.vishia.msgDispatch.LogMessageBase;
import org.vishia.msgDispatch.MsgDispatcher;
import org.vishia.msgDispatch.MsgRedirectConsole;


/**This is an abstract superclass to support cmd line applications. The class contains the following features:
    <ul><li>Support parsing of command line arguments. </li>
        <li>Management of the system output</li>
        <li>Provides an interface for reporting to a file.</li>
        <li>Gathering of the maximum of the exitErrorLevel, see setExitErrorLevel().</li>
        <li>Provides a method to execute external cmd lines in a simple way for the user, see executeCmdLine()</li>
        <li>store information about the application see {@link #writeAboutInfo()} and {@link #writeHelpInfo()}.
    </ul>
    You can find an application example in {@link SampleCmdLine}.
 * <br>
 * <br>
 * <hr>
 * <b>Output to console or to the command line pipes or?</b>:<br>
 * There are some approaches for outputs from an application:
 * <ul>
 * <li>Simple command line invocation: The output is expected on the console respectively shell or cmd window. 
 *   This kind of output is possible on Graphical Applications too if the application was started in a command line window.
 * <li>Command line invocation with redirected output: One can write <code>cmd args >myOutput</code> or one can use a pipe
 *   construction like <code>cmd args | cmd2 args</code>. In that cases the outputs written with {@link java.lang.System#out}
 *   are written to the given channel (usual a file) respectively they are stored in a pipe to provide the input for the
 *   next command. In this cases usual the standard output {@link java.lang.System#out} is redirected but not the error output.
 * <li>Graphical Applications: The graphical application may have an output window which can present the System.out.
 * </ul>
 * What is need? It is nice to have some information while running an application and while occurring errors. But sometime
 * detail information are need, and sometime the flood of information is not desired.
 * <br>
 * <b>The report approach: </b>: Therefore since the first version of this class a output dispatching system are implemented with the routines
 * {@link #report(int, String)} etc.
 * <ul>
 * <li>For usual output requests the routine {@link #report(int, String)} or {@link #reportln(int, String)} can be used
 *   from an application instead <code>System.out.println(....)</code>.
 *   The first parameter controls whether the output is written to console channel or to the report file. With the
 *   command line arguments <code>--report=FILE</code> and <code>--rlevel=WDR</code> parsed as standard arguments
 *   the usage of levels and the file is controlled without additional programming effort. 
 *   With that approach a output file 'report file' is created anyway in a command line application. The report file 
 *   contains the same or more information than the console output. A command line application doesn't require the
 *   redirection in a file to get its output persistently. A persistent output is available anyway with the report file.  
 * <li>The routines {@link #writeInfo(String)}, {@link #writeWarning(String)} and {@link #writeError(String)} can be used
 *   to output some things as information, warning or error in the application. The output is categorized to one of this
 *   three levels.
 *   This information is written to the console output anyway.
 *   It depends on the 3. position 'W' of the <code>--relevl=WDR</code> whether this output is written in the report file.
 * <li>To output an exception message one can use {@link #writeError(String, Throwable)}.  
 * </ul>
 * This is the older system to output something. The interface {@link MainCmdLogging_ifc} (older version {@link Report})
 * supports this system. 
 * <br><br>
 * The problem of the older output management system is: A simple application can distinguish between the 6 levels 
 * {@link MainCmdLogging_ifc#error} to {@link MainCmdLogging_ifc#fineDebug}. But in complex software a module may output
 * something independent of the necessity of output of information of another module. 
 * <br><br>
 * The second problem is: The modules which should output something may not know or use the {@link MainCmdLogging_ifc} 
 * because it may be realized as a special interface.
 * <br>
 * <br>
 * <b>The LogMessage interface or MsgDispatcher</b>: For embedded applications a message dispatching system is known. 
 * The message system writes messages with numbers and timeStamp. Such a message system was implemented in C-language
 * applications too. That system uses Numbers for any output information. The output text can be determined with the 
 * identification number of a output message in different languages. Such capability is done by special software. 
 * The messages can be stored in a simple way in log archives or alarm state storages using only the number and additional
 * measurement values without the message text.
 * <br><br>
 * This message output system with its identification number for any message can be used to dispatch any information
 * to several output channels by using its number. With them any module can output its stuff with a number range. 
 * Whether it is output to display, to a log file or the output is suppressed, it can be controlled by the dispatching
 * control file. That control file associates numbers or number ranges to output channels, 
 * see {@link org.vishia.msgDispatch.MsgDispatcher#setOutputRange(int, int, int, int, int)}
 * <br><br>
 * The interface for this dispatcher is given with the {@link LogMessage} interface. The MainCmd 
 * implements the {@link LogMessage} interface. It is a super interface of {@link MainCmdLogging_ifc}.
 * The implementation of the {@link LogMessage} in MainCmd can redirect the message to the message dispatcher or any
 * other implementor given with {@link #setLogMessageDestination(LogMessage)}. The default behaviour is writing
 * the message via {@link #reportln(int, String)} to console and the report file. Wherby the ident number is taken from
 * the message indent number if it is 1..6 ({@link MainCmdLogging_ifc#error} to {@link MainCmdLogging_ifc#fineDebug}).
 * All other ident numbers are written with <code>reportln({@link MainCmdLogging_ifc#info}, line)</code>. 
 * The line contains the message ident number and the time of creation of the message.
 * That is the simple adaption from the {@link LogMessage} approach without using of a message dispatching system.
 * <br><br>
 * Without change in the user's software the {@link org.vishia.msgDispatch.MsgDispatcher} can be established. One can
 * invoke {@link #setLogMessageDestination(LogMessage)} and use the channels {@link #getLogMessageErrorConsole()},
 * {@link #getLogMessageOutputConsole()} and {@link #getLogMessageOutputFile()} from this class as destination channels
 * for the MessageDispatcher. 
 * <br>
 * <br>
 * <b>Message dispatching without numbers using System.out and System.err</b>: 
 * The problem of the numbers is: Any log output or message should have
 * a unified number. The programmer should handle this numbers in a global way. That number actuation is a pain in the neck
 * if the programmer does not need the numbers at that time.
 * <br><br>
 * To simple to way of life the {@link org.vishia.msgDispatch.MsgPrintStream} can be used to create numbers automatically.
 * With them the simple <code>System.out.printf("message text\n", parameter)</code> is converted to a {@link LogMessage} invocation.
 * The ident numbers of the messages are created automatically in a given number range or it can be set by a control String.
 * <br><br>
 * With them a simple <code>System.out.printf("message text\n", parameter)</code> is sufficient to output something 
 * and to work with the message dispatching and output management system. The dispatching is support with 
 * <br>
 * <br>
 * <b>Conversion from the report output to message outputs</b>: See {@link ReportWrapperLog}. This is the adaption from
 * the older report approach to the message system. 
 * <br><br>
 * <b>What should the user do</b>:
 * <ol>
 * <li>If the user is intent to work with the MainCmd class, one should use the {@link MainCmd_ifc} or its super interface,
 *   the {@link MainCmdLogging_ifc}. One can use both, the {@link MainCmd_ifc#report(int, String)} invocation and the 
 *   {@link LogMessage#sendMsg(int, String, Object...)} approach.
 * <li>If the user should only work with the Message Dispatcher System independent of this MainCmd class, 
 *   one should associate this class or the {@link org.vishia.msgDispatch.MsgDispatcher} class as {@code LogMessage} reference. 
 *   One should organize and use the message ident numbers. 
 * <li>As a independent common approach the user can use the <code>System.out.print...</code>. Therewith independent
 *   sources from any vishia classes can be written. Using a format <br>
 *   <code>System.out.printf("Module - message; text %d %f\n", value1, value2);</code><br>
 *   is a proper format independent of any vishia approaches. That system matches to the redirection and conversion
 *   to the vishia message system with output channels of this class.
 * </ol>   
 * It seems to be a good choice to use the second approach, {@link LogMessage} if the software is specialized for
 * message output, and to use the third approach, System.out for any other independent modules. The organization of both
 * approaches to use the message system can be done at a centralized point in software. The example {@link SampleCmdLine}
 * presents example outputs with that approaches.    
 * <hr>                  
 * <br><br>
    <b>Extension to GUI application</b>:<br>
    This class is extended for example by {@link org.vishia.gral.area9.GralArea9MainCmd} to a class which implements
    the outputs on a GUI window. The methods supporting command line things
    are adapted there for a GUI (Graphical User Interface) -style application. In the GUI frame the user used the same
    interface MainCmd_Ifc to do something with the applications frame.
    <br><br>
    It is an old documentation:    
    The user should write his application class in the followed way:<br/>
    <pre>
    ---|> means an inherition
    <>--> means a composition or aggregation
    ----> means a association


    +-------------+        +-------------+        +-------------+
    |             |------|>| MainCmd     |------|>| MainCmd_Ifc |
    |UserMainClass|        +-------------+        +-------------+
    |             |                                      ^
    |             |        +-------------+    main       |
    |             |<>----->| UserClasses |---------------+
    +-------------+        +-------------+

    in cmdlines:

    class UserMainClass extends MainCmd
    { final UserClasses userClasses = new UserClasses(this);  //... the user's classes
    }

    class UserClasses
    { final MainCmd_Ifc main;        //the aggregation to Main
      UserClasses(MainCmd_Ifc main)
      { this.main = main;
      }
    }
    </pre>
    The UserMainClass based on MainCmd as superclass, MainCmd implements MainCmd_Ifc.
    All composite classes from UserMainclass may known the MainCmd instance respective the UserMainClass instance
    via the interface MainCmd_Ifc.
    With this interface the user can access at all capabilities of support of command line things,
    implemented cardinally in MainCmd, but overwriteable from the user in his UserMainClass.<br/>
    The user should realized the evaluation of the command line arguments, help messages for command line etc. in
    the UserMainClass. But the functionality of the user's application should be realized not in the UserMainClass,
    but in the UserClass and the underlying classes. The functionality should be separated from the command line invoke,
    because this application's functionality can be invoked also from a comprising application, at example by a GUI
    (Graphical Users Interface).
 */

public abstract class MainCmd implements MainCmd_ifc
{

  /**Version, history and license.
   * Changes:
   * <ul>
   * <li>2019-12-05 Hartmut new: $$ENV$: replacement of environment variables able to use both in cmd line and in --@file 
   * <li>2015-11-21 Hartmut bugfix: close() was missing after reading an --@args.file 
   * <li>2014-05-31 Hartmut new: option --help:outfile 
   * <li>2014-05-31 Hartmut chg: default value for nReportLevel = 0, only output if --rlevel= is given. 
   *   Do not create unnecessary files. 
   * <li>2014-05-31 Hartmut chg: {@link #writeError(String, Throwable)} instead Exception etc.
   * <li>2014-05-28 Hartmut new: {@link #currdir()} and {@link #currdir}. It can be used by an application.
   *   Note that the System.setProperty("user.dir", value) affects the whole JVM. A MainCmd may be invoked 
   *   only as a part of an Java application. Therefore the currdir is local valid. Note that the currdir
   *   and the System.setProperty("user.dir",...) both don't affects the operation system.   
   * <li>2014-01-08 Hartmut new: {@link #getLogging_ifc()}, usage MainCmd in any Java class without forwarded argument.
   * <li>2014-01-08 Hartmut bugfix: {@link #sendMsgTimeToAppendableDst(Appendable, int, int, OS_TimeStamp, String, Object...)}
   *   catches a IllegalFormatException 
   * <li>2013-12-30 Hartmut chg: {@link #testArgument(String, int)} emptyArg supported, was commented.
   * <li>2013-12-15 Hartmut new: Argument --msgcfg adds the {@link MsgRedirectConsole} to the main instance.
   *   Therewith the Conversion of System.out.println(...) and System.err... can be used in all applications.
   *   The first advantage: All messages have a time stamp. 
   * <li>2013-07-14 Hartmut chg rule for the {@link #addArgument(Argument[])}, see description of {@link Argument}
   * <li>2013-03-10 Hartmut chg Some adjustments in {@link #addArgument(Argument[])},especially old: setArguments(..)
   *   is renamed to addArgument(). It is prepared that an inherited class's main(...) can add some more arguments
   *   than the base class. 
   * <li>2013-02-24 Hartmut improved {@link #getLogMessageOutputConsole()} etc.
   * <li>2013-02-09 Hartmut new {@link #addArgument(Argument[])} as new method to bundle argument query and help text.
   * <li>2012-03-30 Hartmut new {@link #getLogMessageErrorConsole()}
   * <li>2011-10-11 Hartmut new {@link #setOutputChannels(Appendable, Appendable)}. All outputs are redirect-able now.
   *   Used for output in a graphical text box.
   * <li>2011-07-10 JcHartmut bugfix: The method {@link #executeCmdLine(ProcessBuilder, String, String, int, Appendable, Appendable)}
   *   has produced a problem because 2 spaces are given in the args instead of one. There was an empty argument therefore,
   *   which has had a negative effect to a called command (it was "bzr add", an empty argument forces addition of all files
   *   though the next arguments had contain some named files). The problem is solved with using a more complex algorithm
   *   to split arguments as a simple String.split(" "). See {@link #splitArgs(String)}.  
   * <li>2007..2011 JcHartmut Some changes
   * <li>2007-03-07 JcHartmut  Method getReportFileName
   * <li>2007-03-07 JcHartmut  callWithoutArgument do not exit but throws an exception likewise by an error of arguments.
   * <li>2006-04-02 JcHartmut  writeInfo__ new methods write__Directly, solution of concurrency to write to report.
   * <li>2006-01-17 JcHartmut  testArgument() may thrown an ParseException.
   * <li>2006-01-21 JcHartmut  bugfix callWithoutArguments() should be protected, not package private.
   * <li>2004-06-00 JcHartmut  initial revision
   * </ul>
   * Copyright/Copyleft:
   *
   * For this source the LGPL Lesser General Public License,
   * published by the Free Software Foundation is valid.
   * It means:
   * 1) You can use this source without any restriction for any desired purpose.
   * 2) You can redistribute copies of this source to everybody.
   * 3) Every user of this source, also the user of redistribute copies
   *    with or without payment, must accept this license for further using.
   * 4) But the LPGL is not appropriate for a whole software product,
   *    if this source is only a part of them. It means, the user
   *    must publish this part of source,
   *    but don't need to publish the whole source of the own product.
   * 5) You can study and modify (improve) this source
   *    for own using or for redistribution, but you have to license the
   *    modified sources likewise under this LGPL Lesser General Public License.
   *    You mustn't delete this Copyright/Copyleft inscription in this source file.
   *
   * @author JcHartmut = hartmut.schorrig@vishia.de
   */
  public static String sVersion = "2014-06-17";
  
  
  
  
  /**Interface for implementation of setting arguments.
   * The implementation can be written with an anonymous implementation with the simple form:
   * <pre>
   * MainCmd.SetArgument setArgxy = new MainCmd.SetArgument(){
   *   @Override public boolean setArgument(String val){
   *     args.argxy = val;
   *     return true;
   * } }
   * <pre>
   * The implementation should be an instance inside the user's class. This example shows an argument instance
   * named 'args' where the values of the given arguments are stored.
   * <br><br>
   * The implementation method can test the admissibility of the argument's value. It can return false
   * to designate that the value is not valid. For example the existence of a file can be checked.
   */
  public interface SetArgument{ 
    boolean setArgument(String val); 
  }
  
  /**Class to describe one argument. One can be create static instances with constant content. Example:
   * <pre>
   * Argument[] argList =
   * { new Argument("", " argument without key on first position", setmethod)
   * , new Argument("-arg", ":keyed argument", setmethod)
   * , new Argument("-x", ":the help text", setx)
   * , new Argument("", " argument without key on any position", setx)
   * };
   * </pre>
   * <ul>
   * <li>If the {@link #arg} is empty and it is not on the last position, this is a non keyed argument
   * which is expect on this position in the argument list.
   * <li>If the {@link #arg} is not empty, it is the key for the argument. Usual it starts with "-"
   *   but that is not necessary for the algorithm of argument detection. It is only a style of guide
   *   to give arguments. After the argument one of the character '=' or ':' are possible to follow.
   *   But that is not necessary too. If that characters follow, the argument key is taken.
   *   Elsewhere the longest key is detected which is matching. It is possible to request:
   *   "key1value" and "key11value", the longest key detection wins.
   * <li>If the {@link #arg} is empty and it is the last Argument[] which is added with {@link MainCmd#addArgument(Argument[])}
   *   respectively it is the last entry in {@link MainCmd#argList}, then any argument which does not start 
   *   with the given keys are recognized on any position. In this case the distinction between arguments
   *   should be done at user level. For example it may be usual to write "key value key2 value".  
   * </ul>    
   *
   */
  public static class Argument{ 
    final String arg; 
    final String help; 
    final SetArgument set;
    
    public Argument(String arg, String help, SetArgument set){
      this.arg = arg; 
      this.help = help; 
      this.set = set;
    }
    @Override public String toString(){ return arg + help + '\n'; }
  }
  
  
  
  protected final List<MainCmd.Argument> argList = new ArrayList<MainCmd.Argument>();
  
  
  /**Because this class is used as superclass for a command line Java invocation,
   * it is possible to store the only one instance for a static getter method.
   * In this kind any Java routine which is attempt to use the {@link MainCmdLogging_ifc}
   * or {@link MainCmd_ifc} it can use this instance without a forwarded argument. 
   */
  private static MainCmd singleton;
  
  /** The report file. This attribute is set to null, if no report is desired by missing the argument -r REPORTFILE
  */
  public FileWrite fReport;

  /** All reports with a level less than or equal this level will be reported. Other report calls has non effect.*/
  public int nLogLevel = 0; //MainCmdLogging_ifc.error;  //default: reports only errors, old: warnings and info, but no debug

  private String sFileLog = "report.txt";
  
  public String currdir = System.getProperty("user.dir"); 

  /** All reports with a level less than or equal this level will be written on display.
   *  Note: using {@link Report#writeInfo(String)} etc. writes to display also if this attribute is 0.
   */
  public int nReportLevelDisplay = 0;   //default: don't write reports to display

  /** writeError(), writeWarning(), writeInfo()  are also reported if the level is equal or greater than 1,2,3*/
  public int nLevelDisplayToReport = MainCmdLogging_ifc.info;

  /** List of strings contents help Info in form of lines. The list should be filled with method addHelpInfo(String);*/
  public List<String> listHelpInfo = new LinkedList<String>();

  /** List of strings contents about Info in form of lines. The list should be filled with method addHelpInfo(String);*/
  public List<String> listAboutInfo = new LinkedList<String>();

  /** The maximum of the value given with setExitErrorLevel */
  private int nMaxErrorLevel = MainCmdLogging_ifc.exitSuccessfull;


  /** array of arguments from command line*/
  protected String[] cmdLineArgs;

  /** number of the argument is parsing*/
  private int iArgs;

  protected boolean bHelpIswritten;
  
  /** access to some spaces via Report.spaces.substring(0,n)*/
  private static final String report_spaces = "                                                                                ";

  
  private LogMessage redirectLogMessage;
  
  /**Preserve the original System.out and System.err on construction. It is the cmdline output and error output
   * of the commmand line invocation. */
  public final PrintStream outCmdline, errCmdline;
  
  /**Channels for output and error output of the main program. */
  private Appendable outConsole,errConsole;
  
  
  /**A standard variant of the {@link MsgDispatcher} for all application,
   * which redirects System.out and System.err. */
  protected MsgRedirectConsole msgDisp;


  protected SimpleDateFormat dateFormatMsg = new SimpleDateFormat("MMM-dd HH:mm:ss.SSS: ");

  



  /** Empty Constructor of the main class. 
   * Note: One can use this constructor and {@link #setArguments(Argument[])} and {@link #parseArguments(String[])}
   * instead of {@link #MainCmd(Argument[], String[])}.
   * <br><br>
   * On construction the referenced Printstreams of {@link java.lang.System#out} and {@link java.lang.System#err}
   * are preserved inside this class in {@link #outCmdline} and {@link #errCmdline}. 
   * One can redirect {@link java.lang.System#setOut(PrintStream)} and ...setErr(...) without changing of the MainCmd
   * outputs after construction of this. Especially the System.out and System.err can be redirected to the Message system
   * using {@link org.vishia.msgDispatch.MsgPrintStream} and {@link org.vishia.msgDispatch.MsgDispatcher} with usage of
   * the console outputs preserved in this class using {@link #getLogMessageErrorConsole()}, {@link #getLogMessageOutputConsole()}
   * and {@link #getLogMessageOutputFile()} of this class.
   * 
   */
  protected MainCmd()
  { MainCmd.singleton = this;
    outConsole = outCmdline = System.out;
    errConsole = errCmdline = System.err;
    this.redirectLogMessage = logMessageImplReport;
  }

  /** Constructor of the main class.
  */
  protected MainCmd(String[] args)
  { this();
    this.cmdLineArgs = args;
  }


  protected MainCmd(Argument[] argList)
  { this();
    for(Argument arg: argList){
      this.argList.add(arg);
    }
  }

  /** Constructor of the main class.
  */
  protected MainCmd(Argument[] argList, String[] args)
  { this(argList);
    this.cmdLineArgs = args;
  }

  
  
  
  /**Redirects the LogMessage implementation of this class to the given implementor.
   * Usual it may be the {@link org.vishia.msgDispatch.MsgDispatcher}. By default the logmessage capability
   * of this class is implemented in that kind that all messages with the ident numbers from 1 to 6 are output
   * in the same form as {@link #reportln(int, String)} and all other ident numbers are written with
   * <code>reportln({@link MainCmdLogging_ifc#info}, messageString)</code>. The message dispatcher can be help
   * to select the output channels of the messages.
   * @param log
   */
  public void setLogMessageDestination(LogMessage log){
    redirectLogMessage = log;
  }
  
  
  
  /**Adds arguments inclusively help info for this application. 
   * This method should be called in order of {@link #addHelpInfo(String)}
   * to the correct sequence.
   * @param list see {@link Argument}
   */
  public void addArgument(Argument[] list){ 
    for(Argument arg: list){
      this.argList.add(arg);
      listHelpInfo.add(arg.arg + arg.help);
      
    }
  }

  /**Adds the help info for standard arguments. The text is the followed:<pre>
    addHelpInfo("--about          show the about info");
    addHelpInfo("--help[=helpfile]  show the help info");
    addHelpInfo("--currdir=PATH   Set the currdir variable.");
    addHelpInfo("--log=FILE       write the report (log) into the given file, create or clear the file.");
    addHelpInfo("-+log=FILE      add to the end of given file or create the report file.");
    addHelpInfo("--loglevel=R     set the level of report, R is number from 1 to 6.");
    addHelpInfo("--loglevel=DR    also write reports upto level D on display, sample: ..-rlevel:24");
    addHelpInfo("--loglevel=WDR   write output also in report, W is nr from 1 to 3 (error, warning, info");
    </pre>
  */
  protected void addStandardHelpInfo()
  {
    addHelpInfo("--arg:value    Use = or : as separator between arg and its value. Don't use space!");
    addHelpInfo("--about        show the help info");
    addHelpInfo("--help[=helpfile]  show the help info");
    addHelpInfo("---arg         ignore this argument");
    addHelpInfo("--@file        use file for further arguments, one argument per line.");
    addHelpInfo("--currdir=PATH Set the currdir variable.");
    addHelpInfo("--log=FILE     write the log (report) into the given file, create or clear the file.");
    addHelpInfo("-+log=FILE     add to the end of given file or create the log file.");
    addHelpInfo("--loglevel=R   set the level of log, R is number from 1 to 6.");
    addHelpInfo("--loglevel=DR  also write logs up to level D on display, sample: ..-rlevel:24");
    addHelpInfo("--loglevel=WDR write output also in log, W is nr from 1 to 3 (error, warning, info");
  }





  /** Adds a help info-line to the internal list. This method should be called from the user on startup to set the
      help info. The help info is used by <a href="#writeHelpInfo()"><code>writeHelpInfo()</code>
      called if the argument --help is given or without arguments. </a>.
      @param info String contains 1 line of a help Info.
  */
  protected void addHelpInfo(String sInfo)
  { listHelpInfo.add(sInfo);
  }

  /** Adds a about info-line to the internal list. This method should be called from the user on startup to set the
  about info. 
  @param info String contains 1 line of a help Info.
  */
  protected void addAboutInfo(String sInfo)
  { listAboutInfo.add(sInfo);
  }

  
  
  

  /** prints the help info to the console output. This Method may be overloaded in MainCmdWin to show the contents in a window.
  */
  public void writeHelpInfo(File outfile)
  { Appendable out= null;
    FileWriter wr = null;
    boolean shouldClose = outfile !=null;
    if(outfile !=null){
      try{ 
        wr = new FileWriter(outfile);
        out = wr;
      } catch(IOException exc){
        out = System.out;
        shouldClose = false;
        try{ out.append("MainCmd.writeHelpInfo - ERROR cannot open help output file; " + outfile.getAbsolutePath());
        }catch(IOException exc2){ System.err.println("MainCmd.writeHelpInfo - ERROR unexpected IOException; "); }
      }
    } else {
      out = System.out;
    }
    writeAboutInfo(out);
    Iterator<String> iInfo = listHelpInfo.iterator();
    try{
      while(iInfo.hasNext())
      {
        out.append((iInfo.next())).append('\n');
      }
    }catch(IOException exc){ System.err.println("MainCmd.writeAboutInfo - ERROR unexpected IOException; "); }
    if(shouldClose){
      try{ ((Writer)out).close();
      }catch(IOException exc){ System.err.println("MainCmd.writeAboutInfo - ERROR unexpected IOException; "); }
    }
    if(wr !=null) { try{ wr.close();} catch(Exception exc) {/*do nothing if close fails*/}}
  
    bHelpIswritten = true;
  }



  /** prints the help info to the console output. This Method may be overloaded in MainCmdWin to show the contents in a window.
  */
  public void writeAboutInfo(Appendable out)
  { 
    Iterator<String> iInfo = listAboutInfo.iterator();
    try{
      while(iInfo.hasNext())
      {
        out.append((iInfo.next())).append('\n');
      }
    }catch(IOException exc){ System.err.println("MainCmd.writeAboutInfo - ERROR unexpected IOException; "); }
  }


  /**Parses the cmdLine-arguments. The args should be set in constructor.
   * @see parseArguments(String [] args)
   * 
   * @throws ParseException
   */
  public final boolean parseArguments()
  throws ParseException
  {
    return parseArguments(cmdLineArgs);
  }

  /**Parses the cmdLine-arguments. The user should invoked this method in his static main method
   * with the following pattern:
   * <pre>
  public static void main(String[] args){
    Args argData = new Args();
    Cmdline cmd = new Cmdline(argData);
    boolean bOk;
    try{ bOk = cmd.parseArguments(args);
    } catch(ParseException exc){
      cmd.setExitErrorLevel(MainCmd.exitWithArgumentError);
      bOk = false;
    }
    if(bOk) {
      UserClass main = new UserClass(argData.x, argData.y);
      main.exec(argData.z);
    }
    cmd.exit();
  }
   * </pre>
   * Within this pattern the parsing of the arguments is independent of the user class. 
   * It may be an advantage of architecture of the user class. Some argument values
   * can be used as initial values on construction of the user class. They can be stored there in 'final' Associations. 
   * <br><br>
   * In that pattern the Argument values are stored in an independent class Args, which is an independent mediator
   * between the MainCmd and the UserClass.
   * <br><br>
   * One can use the {@link #setArguments(Argument[])} to define which arguments are used in which kind.
   * In the older form (till 2012), the user was requested to override this method.
   * <br><br>
   * <b>Standard arguments</b>
   * See {@link #addStandardHelpInfo()}.
   * <ul>
   * <li>--about show the help infos
   * <li>--help[=helpfile]  show the help infos");
   * <li>--currdir=PATH Set the currdir variable.
   * <li>--log=FILE  write the report (log) into the given file, create or clear the file.");
   * <li>--log+=FILE add to the end of given file or create the report file.");
   * <li>--loglevel=R     set the level of report, R is number from 1 to 6.");
   * <li>--loglevel=DR    also write reports upto level D on display, sample: ..-rlevel:24");
   * <li>--loglevel=WDR   write output also in report, W is nr from 1 to 3 (error, warning, info");
   * <li>--@ARGFiLE     The ARGFILE contains one argument per line. Especially it is possible to use spaces 
   *   in the argument values without "".
   *   For example a cmd line invocation <code>>cmd -a arg1 --@file -x argx</code> results in the followed arguments:<br/>
   *  <code>-a arg1 arguments-from-file-lines -x argx</code>
   * </ul>
   * <br><br>
   * <b>What does this method</b>:<br>
   * On every not-standard argument this method calles {@link #testArgument(String, int)} inside.
   * That method checks any argument, see there. If that method returns false,
   * a {@link #writeError(String)} with a matched text is called and a ParseException("", N) ist thrown, N is the
   * number of the argument.
   * Standard arguments are parsed here, see <a href="#addStandardHelpInfo()"><code> addStandardHelpInfo()</code></a>.
   * On end the method {@link #checkArguments()} is called if other then standard arguments are given.
   * If it returns false, an ParseException("", 0) is thrown.
   * The user should had write an output message with writeError() inside its <code>checkArguments()</code><br/>
   * 
   * @see #addArgument(Argument[])
   * @param arg The given arguments from command line invocation.
   * @return true if other then the standard arguments are given. Only then {@link #checkArguments()} was called.
   *  
   */
  public final boolean parseArguments(String [] args)
  throws ParseException
  { cmdLineArgs = args;
    boolean hasArguments = false;
    MainCmd main = this;
        if(cmdLineArgs.length == 0)
    { //call without parameters:
      callWithoutArguments();
    }
    boolean bAppendReport = false;
    { iArgs = 0;
      while(iArgs < cmdLineArgs.length)
      { if(cmdLineArgs[iArgs].startsWith("--@")){
          String sArgFile = getArgument(3);
          File fileArg = new File(sArgFile);
          List<String> listArgs = new LinkedList<String>();
          try{
            @SuppressWarnings("unused")
            int lenFile = (int)fileArg.length();
            BufferedReader inp = new BufferedReader(new FileReader(fileArg));
            while(inp.ready()){
              String sParam = inp.readLine().trim();  
              if(sParam.startsWith("==Args==")) {
                listArgs.clear(); //from this line args starts.
              }
              else if(sParam.length()>0 && sParam.charAt(0) != '#'){
                listArgs.add(replaceEnv(sParam));
              }  
            }
            inp.close();
            //add existing cmdLineArgs and the new args:
            String[] argsNew = new String[cmdLineArgs.length -1 + listArgs.size()];
            System.arraycopy(cmdLineArgs, 0, argsNew, 0, iArgs);
            int iArgNew = iArgs-1;
            for(String arg: listArgs){
              argsNew[++iArgNew] = arg;
            }
            System.arraycopy(cmdLineArgs, iArgs+1, argsNew, iArgNew+1, cmdLineArgs.length - iArgs-1);
            //now all args from file and the existing cmdLineArgs are stored in argsNew, use it instead:
            cmdLineArgs = argsNew;
            iArgs -=1;  //the --@ is removed and replaced with the first argument from file!
          } 
          catch(FileNotFoundException exc){ throw new ParseException("argfile not found: " + fileArg.getAbsolutePath(),0); }
          catch(IOException exc){ throw new ParseException("argfile read error: " + fileArg.getAbsolutePath(),0); }
          
        }
        else if (cmdLineArgs[iArgs].startsWith("--loglevel"))
        { int level;
          try{ level = Integer.parseInt(getArgument(11)); } //;cmdLineArgs[iArgs].substring(7,8)); }
          catch(Exception e)
          { throw new ParseException("ERROR on argument --rlevel=" + cmdLineArgs[iArgs], iArgs);
          }
          main.nLogLevel          = level % 10;
          main.nReportLevelDisplay   = (level / 10) % 10;  //0: no report on display.
          main.nLevelDisplayToReport = (level / 100) % 10;  //0: no display to report.
        }
        else if (cmdLineArgs[iArgs].startsWith("--rlevel"))
        { int level;
          try{ level = Integer.parseInt(getArgument(9)); } //;cmdLineArgs[iArgs].substring(7,8)); }
          catch(Exception e)
          { throw new ParseException("ERROR on argument --rlevel=" + cmdLineArgs[iArgs], iArgs);
          }
          main.nLogLevel          = level % 10;
          main.nReportLevelDisplay   = (level / 10) % 10;  //0: no report on display.
          main.nLevelDisplayToReport = (level / 100) % 10;  //0: no display to report.
        }
        else if(cmdLineArgs[iArgs].startsWith("--currdir=")) { currdir = getArgument(10); }
        else if(cmdLineArgs[iArgs].startsWith("--currdir:")) { currdir = getArgument(10); }
        else if(cmdLineArgs[iArgs].startsWith("--log:")) { sFileLog = getArgument(6); }
        else if(cmdLineArgs[iArgs].startsWith("--log=")) { sFileLog = getArgument(6); }
        else if(cmdLineArgs[iArgs].startsWith("-+log:")) { sFileLog = getArgument(6); bAppendReport = true; }
        else if(cmdLineArgs[iArgs].startsWith("-+log=")) { sFileLog = getArgument(6); bAppendReport = true; }
        else if(cmdLineArgs[iArgs].startsWith("--log+=")) { sFileLog = getArgument(7); bAppendReport = true; }
        else if(cmdLineArgs[iArgs].startsWith("--report+=")) { sFileLog = getArgument(10); bAppendReport = true; }
        else if(cmdLineArgs[iArgs].startsWith("--report:")) { sFileLog = getArgument(9); }
        else if(cmdLineArgs[iArgs].startsWith("--report=")) { sFileLog = getArgument(9); }
        else if(cmdLineArgs[iArgs].startsWith("--about")) { writeAboutInfo(null); }
        else if(cmdLineArgs[iArgs].startsWith("--help:")) { writeHelpInfo(new File(getArgument(7))); }
        else if(cmdLineArgs[iArgs].startsWith("--help=")) { writeHelpInfo(new File(getArgument(7))); }
        else if(cmdLineArgs[iArgs].startsWith("--help")) { writeHelpInfo(null); }
        else if(cmdLineArgs[iArgs].startsWith("--msgcfg:")) { addMsgConfig(cmdLineArgs[iArgs].substring(9)); }
        else if(cmdLineArgs[iArgs].startsWith("--msgcfg=")) { addMsgConfig(cmdLineArgs[iArgs].substring(9)); }
        else if(cmdLineArgs[iArgs].startsWith("---")) { /*ignore it*/ }
        else
        { hasArguments = main.testArgument(cmdLineArgs[iArgs], iArgs); //formal style guide: Use extra statement to assign
          if( !  hasArguments ) { 
            main.writeError("failed argument: " + cmdLineArgs[iArgs]);
            throw new ParseException("failed argument:" + cmdLineArgs[iArgs], iArgs);  //ParseException used from java.text
          }
        }
        iArgs +=1;
      }
    }
    //debug System.out.println("report level=" + main.nReportLevel + " file:" + sFileReport);
    /** open reportfile: */
    if(nLogLevel > 0)
    { try{ openReportfile(sFileLog, bAppendReport); }
      catch(FileNotFoundException exc)
      { writeError("ERROR creating reportfile-path: " +sFileLog);
        throw new ParseException("ERROR creating reportfile-path: " +sFileLog, 0);
      /*if a requested reportfile is not createable, the programm can't be run. The normal problem reporting fails.
        That's why the program is aborted here.
      */
      }
    }
    if(hasArguments && !checkArguments())
    {
      throw new ParseException("arguments not consistent", 0);  //ParseException used from java.text
    }
    return hasArguments;
  }


  
  
  public void openReportfile(String sFileReport, boolean bAppendReport) 
  throws FileNotFoundException
  { 
    boolean bMkdir= false; 
    this.sFileLog = sFileReport;
    if(fReport != null)
    { try{ fReport.close(); } catch(IOException exc){}
    }
    try{ this.fReport = new FileWrite(sFileReport, bAppendReport); }
    catch(IOException exception)
    { //it is possible that the path not exist.
      bMkdir = true;
    }
    if(bMkdir)
    { { FileSystem.mkDirPath(sFileReport); 
        this.fReport = new FileWrite(sFileReport, bAppendReport);
      }
      
    }  
    
  }
  
  
  
  /** Checks the arguments after parsing from command line, test of consistence. This method must overwrite from
   * the user, may be with a return true. The method is ocalled at last inside parseArguments().
   * If this method returns false, parseArguments throws an exception in the same manner if on argument errors.
   *
   * @return true if all is ok. If false,
   */
  protected abstract boolean checkArguments();




  /** Invoked from parseArguments if no argument is given. In the default implementation a help info is written
   * and the application is terminated. The user should overwrite this method if the call without comand line arguments
   * is meaningfull.
   *
   */
  protected void callWithoutArguments()
  throws ParseException
  { writeHelpInfo(null);
    //throw new ParseException("no cmdline Arguments", 0);
  }

  /** Returns the argument contents. The user should invoke this method in his testArgument()-method.
      If the actually argument is longer as pos, the contents is the substring starts on pos.
      If the actually argument is no longer as pos( in praxis equal pos), the contents is the contents of the next argument.
      In this way the getArgument()-method supplies both argument methods:
      <ul><li>-kcontents</li>
          <li>-k contents</li>
      </ul>
      In this sample -k is the key to recognize the argument and contens is the appendant content.
      @param pos position of the beginning of the information in a argument. In the sample above use the value 2.
  */
  protected String getArgument(int pos)
  { String sRet;
    if(cmdLineArgs[iArgs].length() > pos) sRet = cmdLineArgs[iArgs].substring(pos);
    else
    { iArgs+=1;
      if(iArgs < cmdLineArgs.length)
      { sRet = cmdLineArgs[iArgs];
      }
      else
      { sRet = "";
      }
    }
    return sRet;
  }




  /** Tests one argument, called from parseArguments() on every argument excluding standard arguments -r and -rlevel.
      The user can overwrite this method to test the application-specific parameters.
      This non-overridden method uses the {@link #setArgumentList(Argument[])}
      @param argc The argument to test
      @param nArg number of argument, the first argument is numbered with 1.
      @return false, if the argument doesn't match, true if ok.
              if the method returns false, an exception in parseArgument is thrown to abort the argument test
              or to abort the program. @see <a href="#parseArguments()">parseArgument()</a>.<br/>

      @exception ParseException The method may thrown this exception if a conversion error or such other is thrown.
  */
  protected boolean testArgument(String argc, int nArg)
  {
    if(argList !=null){
      Argument emptyArg = null;
      Argument argFound = null;
      int argLenFound = 0;
      int argclen = argc.length();
      int ixArglist = 0;
      int lastIxArglist = argList.size()-1;
      Iterator<Argument> iter = argList.iterator();
      while(argFound == null && iter.hasNext()){  //break while if found.
        Argument argTest = iter.next();
        int argLen = argTest.arg.length();
        if(argLen == 0){
          emptyArg = argTest;  //possible argument if nothing met.
        } else {
          boolean bSeparator = false;
          if((argc.startsWith(argTest.arg)                //correct prefix 
               && (  argclen == argLen                      //only the prefix
                  || (bSeparator = ":=".indexOf(argc.charAt(argLen))>=0))  //or prefix ends with the separator characters.
                  || (argLen == 0 && (ixArglist == nArg || ixArglist == lastIxArglist))                         //argument without key characters
                  )
               && argLen >= argLenFound   
            ){ //then the argument is a candidat
            argLenFound = bSeparator ? argLen +1 : argLen;
            argFound = argTest;
          }
        }
        ixArglist +=1;
      }
      if(argFound !=null){
        //then the argument is correct and associated to this argTest.
        String argval = argclen == argLenFound //no additional value, use argument 
                        //|| argLen == 0      //argument without prefix (no option)
                        ? argc              //then use the whole argument as value.
                        : argc.substring(argLenFound);  //use the argument after the separator as value.
        argval = replaceEnv(argval);
        boolean bOk = argFound.set.setArgument(argval);   //call the user method for this argument.
        //if(!bOk) throw new ParseException("Argument value error: " + argc, nArg);
        return bOk;
      } 
      else if(emptyArg !=null){ //argument start string not found but an empty choice possible:
        //set the empty arg.
        return emptyArg.set.setArgument(argc);
      } 
      else {
        //argument not found (not returned in for-loop):
        return false;
      }
    } else {
      System.err.println("MainCmd- Software design error - MainCmd.setArgumentList(...) should be called or the method testArgument(...) should be overridden.");
      return false;
    }
  }


  
  
  public String replaceEnv(String argval) {
    int posEnv;
    while( (posEnv=argval.indexOf("$$")) >=0) {
      int posEnvEnd = argval.indexOf('$', posEnv+2);
      String nameEnv = argval.substring(posEnv+2, posEnvEnd);
      String env = System.getenv(nameEnv);
      if(env == null) throw new IllegalArgumentException("Environment variable " + nameEnv + "expected, not found");
      argval = argval.substring(0, posEnv) + env + argval.substring(posEnvEnd+1);
    }
    return argval;
  }
  
  
  

  /**Returns that current directory which is given by argument "--currdir=value" or which was {@link #setcurrdir(String)}. 
   * Note that the currdir is not checked whether it is valid and existing.
   * It should be an existing valid normalized absolute path. The application should check it
   * because the path may be existing and removed than. There is no guarantee possible.
   * <br><br>
   * Use that currdir to create File instances independent of the operation system's current dir
   * which is present on command line by start the application:
   * <pre>
   *   File myFile = new File(currdir(), "relative/path");
   * </pre>  
   * Note that the
   * <pre>
   *   System.setProperty("user.dir", currdir);
   * </pre>
   * has a global effect for the running JVM, but it has no effect to the operation system. It means
   * <pre>
   *   System.setProperty("user.dir", "/My/Workingplace");
   *   File myFile = new File("relative.path");
   *   String abspath = myFile.getAbsolutePath();
   * </pre>
   * Does not work proper! If the relative File is used to open anything, the open process is done on the
   * operation system level. But the operation system does not know anything about a changed current dir.
   * But the <code> File.getAbsolutePath() </code> works with the JVM-global setting of "user.dir". That is confusion,
   * which depends on the history and technique.
   * <br><br>
   * Therefore the only one way to work with changed current directories is: Handle it in the application.
   * This routine is a helper of that.    
   * */
  @Override public String currdir(){ return currdir; }

  
  /**Sets the Property {@link #currdir()} for this instance.
   * @param newDir any String which should contain an absolute normalized valid path to an existing directory.
   *   The path is not tested here, it should be tested in the user's application because it may be depending
   *   on any user's condition. 
   * @return The old value of {@link #currdir()}.
   */
  public String setcurrdir(String newDir){
    String ret = currdir;
    currdir = newDir;
    return ret;
  }


  /*--------------------------------------------------------------------------------------------------------*/
  /** set the exitErrorLevel of the maximum of given level of every call.
      @param level Errorlevel how defined in Report, 0 is the lowest level (successfull), >0 is worse.
  */
  public void setExitErrorLevel(int level)
  { if(level > nMaxErrorLevel)
    {  nMaxErrorLevel = level;  }
  }



  /** get the maximum of errorLevel setted with setExitErrorLevel().
      @return the errorlevel
  */
  public int getExitErrorLevel()
  { return nMaxErrorLevel; 
  }


  
  /**Sets destinations for output and error output.
   * Per default the PrintStreams which are referenced from {@link java.lang.System#out} and {@link java.lang.System#err}
   * on construction of this class are set. That is the console output if this class is construct during processing
   * a public main(String[] args)-routine, this routine is invoked froma cmd line call and the output and error
   * channels of the command line are not redirected.
   * <br><br>
   * This method allows to redirect the output and error used from {@link #writeDirectly(String, short)} and from the
   * implementations of {@link #getLogMessageOutputConsole()} and {@link #getLogMessageErrorConsole()}.
   * If the 
   * 
   * 
   * It is recommended that this channels 
   * shall be unchanged, it means this method should not used. It is reasonable to use this method only for tests.
   * <br>
   * Carefull should be taken: If System.setOut or System.setErr is redirected using {@link org.vishia.msgDispatch.MsgPrintStream}
   * and that output is directed to this class, and then setOuputChannels(System.out, System.err) is set,
   * it escalates in a self-calling loop with stack overflow.
   *  
   * @param outP  Destination for output. If null, current output isn't change.
   * @param errP Destination for error output. If null, current output isn't change.
   */
  @Override public void setOutputChannels(Appendable outP, Appendable errP)
  {
    if(outP !=null) { outConsole = outP; }
    if(errP !=null) { errConsole = errP; }
  }
  
  
  

  /*--------------------------------------------------------------------------------------------------------*/
  /**Exits the cmdline application with the maximum of setted exit error level.
     This method should be called only on end of the application, never inside. If the user will abort
     the application from inside, he should throw an exception instead. So an comprising application
     may catch the exception and works beyond.
     This method is not member of MainCmd_Ifc and is setted protected, because
     the users Main class (UserMain in the introcuction) only should call exit.
  */
  public void exit()
  { System.exit(getExitErrorLevel());
  }



  /*--------------------------------------------------------------------------------------------------------*/
  /*--------------------------------------------------------------------------------------------------------*/

  /** Execute a command invoke a cmdline call, implements MainCmd_Ifc.
      @deprecated: since Java 1.5 a ProcessBuilder is available. 
      The new form {@link #executeCmdLine(String, ProcessBuilder, int, Appendable, String)} use it.
  */
  public int executeCmdLine(String cmd, int nReportLevel, Appendable output, String input)
  {
    String[] cmdArray = CmdExecuter.splitArgs(cmd);
    return executeCmdLine(cmdArray, nReportLevel, output, input);
  }





  /** Execute a command invoke a cmdline call, implements MainCmd_Ifc.
      The call must not be needed any input.
      The output is written with a separate thread, using the internal (private) class ShowCmdOutput.
      This class use the method writeInfoln() from here. The writeInfoln-Method writes to console for MainCmd,
      but it may be overloaded, to example for MainCmdWin it may be writed to a box in the GUI.
      @deprecated: since Java 1.5 a ProcessBuilder is available. 
      The new form {@link #executeCmdLine(String[], ProcessBuilder, int, Appendable, String)} use it.
  */
  @Deprecated public int executeCmdLine(String[] cmd, int nReportLevel, Appendable output, String input)
  {
    int exitErrorLevel; // = 0;  //default unused, only because throw
    { StringBuilder sOut = new StringBuilder();
      int iCmd = 0;
      while(iCmd < cmd.length)
      { sOut.append(cmd[iCmd]); iCmd +=1;
        sOut.append(" ");
      }
      sOut.append(":");
      reportln(MainCmdLogging_ifc.debug, "MainCmd.executeCmdLine():" + sOut);
      writeInfoln("execute>" + sOut.toString());
    }
    BufferedReader outputReader = null, errorReader = null;
    try
    { Process process = Runtime.getRuntime().exec(cmd);
      InputStream processOutput = process.getInputStream();  //reads the output from exec.
      InputStream processError  = process.getErrorStream();  //reads the error from exec.

      Runnable cmdOutput = new ShowCmdOutput(output, nReportLevel, outputReader = new BufferedReader(new InputStreamReader(processOutput) ));
      Thread threadOutput = new Thread(cmdOutput,"cmdline-out");
      threadOutput.start();  //the thread reads the processOutput and disposes it to the infoln

      Runnable cmdError = new ShowCmdOutput(null, nReportLevel < 0 ? -MainCmdLogging_ifc.error: MainCmdLogging_ifc.error, errorReader = new BufferedReader(new InputStreamReader(processError) ));
      Thread threadError = new Thread(cmdError,"cmdline-error");
      threadError.start();   //the thread reads the processError and disposes it to the infoln
      writeInfoln("process ...");
      process.waitFor();
      exitErrorLevel = process.exitValue();
    }
    catch(IOException exception)
    { writeInfoln( "Problem \n" + exception);
      if(errorReader !=null) { try{ errorReader.close();} catch(Exception exc) {/*do nothing if close fails*/}}
      if(outputReader !=null) { try{ outputReader.close();} catch(Exception exc) {/*do nothing if close fails*/}}
      throw new RuntimeException("IOException on commandline");
    }
    catch ( InterruptedException ie )
    {
      writeInfoln( ie.toString() );
      if(errorReader !=null) { try{ errorReader.close();} catch(Exception exc) {/*do nothing if close fails*/}}
      if(outputReader !=null) { try{ outputReader.close();} catch(Exception exc) {/*do nothing if close fails*/}}
      throw new RuntimeException("cmdline interrupted");
    }
    if(errorReader !=null) { try{ errorReader.close();} catch(Exception exc) {/*do nothing if close fails*/}}
    if(outputReader !=null) { try{ outputReader.close();} catch(Exception exc) {/*do nothing if close fails*/}}
    return exitErrorLevel;
  }

  
  
  /**Execute a command invoke a cmdline call, implements MainCmd_Ifc.
    The call must not be needed any input.
    The output is written with a separate thread, using the internal (private) class ShowCmdOutput.
    This class use the method writeInfoln() from here. The writeInfoln-Method writes to console for MainCmd,
    but it may be overloaded, to example for MainCmdWin it may be writed to a box in the GUI.
   * @deprecated
  */
  @Deprecated
  public int executeCmdLine
  ( String cmd
  , ProcessBuilder processBuilder
  , int nReportLevel
  , Appendable output, String input
  )
  {
    String[] cmdArray = CmdExecuter.splitArgs(cmd);  //split arguments in the array form
    return executeCmdLine(cmdArray, processBuilder, nReportLevel, output, input);
  
  }

  
  /**
   * @param cmd
   * @param processBuilder
   * @param nReportLevel
   * @param output
   * @param input
   * @return
   * @deprecated
   */
  @Deprecated
  public int executeCmdLine
  ( String[] cmd
  , ProcessBuilder processBuilder
  , int nReportLevel
  , Appendable output, String input
  ){
    return executeCmdLine(processBuilder, cmd, input, nReportLevel, output, output);
  }
  
  
  
  /**Executes a command line call maybe as pipe, waiting for finishing..
   * The output is written with a separate thread, using the internal (private) class ShowCmdOutput.
   * @param processBuilder The ProcessBuilder. There may be assigned environment variables and a current directory.
   * @param cmd The cmd and arguments. If it is null, the command assigened to the processBuilder is used.
   *   The command can contain arguments separated with spaces (usual for command lines) or white spaces.
   *   The method {@link #splitArgs(String)} is used.
   * @param input Any pipe-input. It may be null.
   * @param nReportLevel The report level which is used for output. 
   *        If it is 0, then the output isn't written
   * @param output The output pipe.
   * @param sAnswer The error pipe. If it is null, then errors are written in the output pipe.
   * @return
   */
  @Override public int executeCmdLine
  ( ProcessBuilder processBuilder
      , String cmd
      , String input
      , int nReportLevel
      , Appendable output
      , Appendable errOut
  ){
    String[] cmdArray = CmdExecuter.splitArgs(cmd);  //split arguments in the array form
    
    return executeCmdLine(processBuilder, cmdArray, input, nReportLevel, output, errOut);

  }


  
  /**Executes a command line call maybe as pipe, waiting for finishing..
   * The output is written with a separate thread, using the internal (private) class ShowCmdOutput.
   * @param processBuilder The ProcessBuilder. There may be assigned environment variables and a current directory.
   * @param cmd The cmd and arguments. If it is null, the command assigend to the processBuilder is used.
   * @param input Any pipe-input. It may be null.
   * @param nReportLevel The report level which is used for output. 
   *        If it is 0, then the output isn't written
   * @param output The output pipe.
   * @param sAnswer The error pipe. If it is null, then errors are written in the output pipe.
   * @return
   */
  @Override public int executeCmdLine
  ( ProcessBuilder processBuilder
      , String[] cmd
      , String input
      , int nReportLevel
      , Appendable output
      , Appendable errOut
  )
  { int exitErrorLevel;
  BufferedReader outputReader = null, errorReader = null;
  try
  {
    processBuilder.command(cmd);
    Process process = processBuilder.start();
    InputStream processOutput = process.getInputStream();  //reads the output from exec.
    InputStream processError  = process.getErrorStream();  //reads the error from exec.
    Runnable cmdOutput = new ShowCmdOutput(output, nReportLevel, outputReader = new BufferedReader(new InputStreamReader(processOutput) ));
    Thread threadOutput = new Thread(cmdOutput,"cmdline-out");
    threadOutput.start();  //the thread reads the processOutput and disposes it to the infoln

    Runnable cmdError = new ShowCmdOutput(errOut, nReportLevel, errorReader = new BufferedReader(new InputStreamReader(processError) ));
    Thread threadError = new Thread(cmdError,"cmdline-error");
    threadError.start();   //the thread reads the processError and disposes it to the infoln
    //boolean bCont = true;
    writeInfoln("process ...");
    //String sOut = null;
    //String sError = null;
    //int nTime = 0;
    //while(bCont)
    { //if(++nTime > 100000){ writeInfo("."); nTime = 0; }
      //if(in.ready())
      //{ writeInfoln(in   .readLine()); }
      //if(error.ready()) { writeInfoln(error.readLine()); }
      //if(sOut == null || sError == null)
      { //writeInfoln("******* finish **********");
        //bCont = false;
      }
    }

    process.waitFor();
    exitErrorLevel = process.exitValue();
  }
  catch(IOException exception)
  { writeInfoln( "Problem \n" + exception);
       exitErrorLevel = 255;
  //throw new RuntimeException("IOException on commandline");
  }
  catch ( InterruptedException ie )
  {
    writeInfoln( ie.toString() );
    exitErrorLevel = 255;
    //throw new RuntimeException("cmdline interrupted");
  }
  if(errorReader !=null) { try{ errorReader.close();} catch(Exception exc) {/*do nothing if close fails*/}}
  if(outputReader !=null) { try{ outputReader.close();} catch(Exception exc) {/*do nothing if close fails*/}}
  return exitErrorLevel;
  }
  


  /**Starts a command invocation for a independent window.
   * This command does not have any input or output. The command will be started,
   * the finishing isn't await. This command line invocation is proper for commands,
   * which create a new window in the operation system. The new window has its own live cycle then,
   * independent of the invocation.
   * @param cmd The command. Some arguments are possible, they should be separated by space.
   * @param processBuilder The processBuilder.
   * @return
   */
  @Override public int startCmdLine(ProcessBuilder processBuilder, String cmd)
  {
    String[] cmdArray = CmdExecuter.splitArgs(cmd);  //split arguments in the array form
    return startCmdLine(processBuilder, cmdArray);
  
  }
  
  
  /**Starts a command invocation for a independent window.
   * This command does not have any input or output. The command will be started,
   * the finishing isn't await. This command line invocation is proper for commands,
   * which create a new window in the operation system. The new window has its own live cycle then,
   * independent of the invocation.
   * @param cmd The command and some arguments.
   * @param processBuilder The processBuilder.
   * @return 0 on success, 255 if any start error.
   */
  @Override public int startCmdLine(ProcessBuilder processBuilder, String[] cmd)
  { int exitErrorLevel = 0;
    try
    {
      processBuilder.command(cmd);
      @SuppressWarnings("unused")
      Process process = processBuilder.start();
    
    }
    catch(IOException exception)
    { writeError("", exception);
      exitErrorLevel = 255;
      //throw new RuntimeException("IOException on commandline");
    }
    return exitErrorLevel;
  }
  

  @Override public int switchToWindowOrStartCmdline(ProcessBuilder processBuilder, String sCmd, String sWindowTitle)
  {
    throw new IllegalArgumentException("only available in graphical-systems.");
  }



  /*--------------------------------------------------------------------------------------------------------*/
  /*--------------------------------------------------------------------------------------------------------*/
  /** Writes an information, implementation of MainCmd_Ifc.
      The info is written also to report depended on command line arguments --rlevel.
      If the user will overwrite the kind of output, overwrite writeInfoDirectly, it is called here.
  */
  public final void writeInfo(CharSequence sInfo)
  { writeDirectly(sInfo, kInfo_writeInfoDirectly);
    if(nLevelDisplayToReport >= MainCmdLogging_ifc.info)
    { report(MainCmdLogging_ifc.info | MainCmdLogging_ifc.mNeverOutputToDisplay, sInfo );
    }
  }

  /** Writes an information line, implementation of MainCmd_Ifc.
      The info is written also to report depended on command line arguments --rlevel.
      If the user will overwrite the kind of output, overwrite writeInfolnDirectly, it is called here.
  */
  public final void writeInfoln(CharSequence sInfo)  //##a
  { writeDirectly(sInfo, kInfoln_writeInfoDirectly);
    if(nLevelDisplayToReport >= MainCmdLogging_ifc.info)
    { reportln(MainCmdLogging_ifc.info | MainCmdLogging_ifc.mNeverOutputToDisplay, sInfo );
    }
  }


  /** Writes an warning line, implementation of MainCmd_Ifc.
      The info is written also to report depended on command line arguments --rlevel.
      If the user will overwrite the kind of output, overwrite writeWarningDirectly, it is called here.
  */
  public final void writeWarning(CharSequence sInfo)
  { writeDirectly(sInfo, kWarning_writeInfoDirectly);
    if(nLevelDisplayToReport >= MainCmdLogging_ifc.warning)
    { reportln(MainCmdLogging_ifc.warning | MainCmdLogging_ifc.mNeverOutputToDisplay, "WARNING:" + sInfo );
    }
  }

  /** Writes an error line, implementation of MainCmd_Ifc.
      The info is written also to report depended on command line arguments --rlevel.
      If the user will overwrite the kind of output, overwrite writeErrorDirectly, it is called here.
  */
  public final void writeError(CharSequence sInfo)
  { writeDirectly(sInfo, kError_writeInfoDirectly);
    if(nLevelDisplayToReport >= MainCmdLogging_ifc.error)
    { reportln(MainCmdLogging_ifc.error | MainCmdLogging_ifc.mNeverOutputToDisplay, "ERROR:" + sInfo );
    }
  }


  /** Writes an error line with exception info, implementation of MainCmd_Ifc.
      The info is written also to report depended on command line arguments --rlevel.
      If the user will overwrite the kind of output, overwrite writeErrorDirectly, it is called here.
  */
  public final void writeError(String sInfo, Throwable exception)
  { writeErrorDirectly(sInfo, exception);
    if(nLevelDisplayToReport >= MainCmdLogging_ifc.error)
    { report(sInfo, exception, true );
    }
  }

  
  

  /** write out the stacktrace from a exception. Writes into reportfile and on screen
  */
  public void writeStackTrace(Exception exception)
  { exception.printStackTrace(new PrintStream(fReport, true));
  }



  /** Some bits to mark the kind of output*/
  protected static final short kInfo_writeInfoDirectly     = (short)0x1;
  public static final short kInfoln_writeInfoDirectly   = (short)0x3;
  protected static final short kWarning_writeInfoDirectly  = (short)0x6;
  protected static final short kError_writeInfoDirectly    = (short)0xa;

  /** If this bit is setted in param kind of writeDirectly
   * a newline should be inserted at begin of output
   */
  public static final short mNewln_writeInfoDirectly   = (short)0x2;

  /** If this bit is setted in param kind of writeDirectly
   * a warning is to be outputted
   */
  protected static final short mWarning_writeInfoDirectly = (short)0x4;

  /** If this bit is setted in param kind of writeDirectly
   * an error is to be outputted
   */
  protected static final short mError_writeInfoDirectly   = (short)0x8;


  /**Writes the given sInfo to the output channels {@link #out} or {@link #err}. That channels are gotten from
   * System.out and System.err on construction of this class. Usual that channels are non-overriden, so that it is
   * the console output. 
   * <br><br>
   * If {@link java.lang.System#setErr(PrintStream)} or {@link java.lang.System#setOut(PrintStream)} was invoked,
   * this routine is not influenced. 
   * <br><br>
   * This method is overrideable espscially to redirect the output to a graphical window instead to console.
   * Another possibility to change the output is {@link #setOutputChannels(Appendable, Appendable)}.
   *  @param sInfo The String to write
   *  @param kind Combination of bits {@link #mWarning_writeInfoDirectly}, {@link #mError_writeInfoDirectly}
   *    or {@link #mNewln_writeInfoDirectly}.
   */
  public void writeDirectly(CharSequence sInfo, short kind)  //##a
  {
    try{
      if((kind & mWarning_writeInfoDirectly) != 0)
      {
        errConsole.append("\n");
        errConsole.append( "WARNING: ").append(sInfo ).append("\n");
      }
      else if((kind & mError_writeInfoDirectly) != 0)
      {
        errConsole.append("\n");
        errConsole.append( "ERROR: " ).append(sInfo).append("\n");
      }
      else
      { if( (kind & mNewln_writeInfoDirectly) != 0) outConsole.append("\n"); //finishes the previous line
        int posStart = 0;
        int posEol;
        while( posStart < sInfo.length() && (posEol = StringFunctions.indexOf(sInfo,'\n', posStart)) >=0)
        { outConsole.append(sInfo.subSequence(posStart, posEol)).append("|");
          posStart = posEol + 1;
        }
        if(posStart < sInfo.length()) outConsole.append(sInfo.subSequence(posStart, sInfo.length()));
      }
    } catch(Exception exc){
      System.err.println("Exception while output in MainCmd.writeDirectly(" + sInfo + ")");
    }
  }




  /** Writes an error line by console application directly to System.err.println
      with the String "EXCEPTION: " before and the exception message.
      The user can overwrite this method in the derived class of MainCmd to change to kind of output.
      @param sInfo Text to write in the new line after "EXCEPTION: ".
      @param exception Its getMessage will be written.
  */
  public void writeErrorDirectly(CharSequence sInfo, Throwable exception)
  {
    System.err.println("");
    System.err.println( "EXCEPTION: " + sInfo); // + exception.getMessage());
    exception.printStackTrace(System.err);  //the exception message will be printed here.
  }

  /*--------------------------------------------------------------------------------------------------------*/
  /*--------------------------------------------------------------------------------------------------------*/
  /* implements Report.*/


  /** report appending on exist line.
      If the report contains an newline char \n, a char '|' will be written.
      This newline is not interpreted as newline for the report file, but only as stochastic contents in the string.
    * @param nLevel write the report only if the demand level is greater or equal. Use {@link MainCmdLogging_ifc#error}
    * etc.
    * @param ss String to write.
  */
  public void report(int nLevel, CharSequence ss)
  { if( (nLevel & mReportLevel) <= nLogLevel && fReport != null)
    { int posStart = 0;
      //int posEol;
      /*
      while( posStart < ss.length() && (posEol = ss.indexOf('\n', posStart)) >=0)
      { fReport.write(ss.substring(posStart, posEol) + "|");
        posStart = posEol + 1;
      }
      */
      if(posStart < ss.length()){ fReport.write(ss.toString().substring(posStart)); }
    }
    if( (nLevel & mReportLevel) <= nReportLevelDisplay && (nLevel & mNeverOutputToDisplay) == 0)
    { //writes also an error as info on display.
      writeDirectly(ss, kInfo_writeInfoDirectly);
    }

  }

  /** report begins at a new a line with left margin
    * @param nLevel write the report only if the demand level is greater or equal.
    * @param nLeftMargin determins a left margin. First a new line is outputted, followed by '*' and spaces.
    * @param ss String to write.
  */
  public void reportln(int nLevel, int nLeftMargin, CharSequence ss)
  { if( (nLevel & MainCmdLogging_ifc.mReportLevel) <= nLogLevel && fReport != null)
    { fReport.writeln("");
      int nLeftMargin1 = nLeftMargin;
      if(nLeftMargin > report_spaces.length()/2){ nLeftMargin1 = report_spaces.length()/2; }
      fReport.write("*" + (nLevel & MainCmdLogging_ifc.mReportLevel) + ":" + report_spaces.substring(0, 2*nLeftMargin1));
    }
    if((nLevel & MainCmdLogging_ifc.mReportLevel) <= nReportLevelDisplay && (nLevel & MainCmdLogging_ifc.mNeverOutputToDisplay) == 0 )
    { //writes also an error as info on display.
      writeDirectly(ss, kInfoln_writeInfoDirectly);
    }
    report((nLevel | MainCmdLogging_ifc.mNeverOutputToDisplay), ss);
  }

  public void reportln(int nLevel, CharSequence ss){ reportln(nLevel, 0, ss); }


  /** Reports an exception. This report is written unconditional, like MainCmdLogging_ifc.error.
      On Display, this report is only written if display output is enabled for the level MainCmdLogging_ifc.error.
      @param sInfo Text to write in the new line after "EXCEPTION: ".
      @param exception Exception info to write
  */
  public void report(CharSequence sInfo, Throwable exception)
  {
    report(sInfo, exception, false); //not written on display yet.
  }

  /** Internal method to write an exception to report. A third parameter idicates,
   * wether or not the writing to display is always done.
   * @param sInfo Text to write in the new line after "EXCEPTION: "
   *        and before the exception.getMessage is written.<br/>
   * @param exception Exception info to write
   * @param bWrittenOnDisplay true, than the writing to display is always done.
   */
  private void report(CharSequence sInfo, Throwable exception, boolean bWrittenOnDisplay)
  { if(fReport == null){
      if( !bWrittenOnDisplay){
        writeErrorDirectly(sInfo, exception);
        //exception.printStackTrace(System.out);
      }
    } else { 
      fReport.writeln("\nEXCEPTION: " + sInfo + "  " + exception.getMessage());
      exception.printStackTrace(new PrintStream(fReport, true));
      if(MainCmdLogging_ifc.error <= nReportLevelDisplay && !bWrittenOnDisplay)
      { //writes also an error as info on display.
        writeErrorDirectly(sInfo, exception);
      }
    }
  }


  /** Test wether the report is in the level.
  */
  public int getReportLevel(){ return nLogLevel; }
  
  /** Set another level inside programming. It is advisable to restore the old level
   * after the designated operation.
   * @param newLevel The level to be set, use one of the defines MainCmdLogging_ifc.info to MainCmdLogging_ifc.fineDebug
   * @return the current level, usefull to restore it.
   */
  public int setReportLevel(int newLevel)
  { int oldLevel = nLogLevel;
    nLogLevel = newLevel;
    return oldLevel;
  }


  public void flushReport()
  { if(fReport != null)
    { try{ fReport.flush();}
      catch(IOException exc)
      { //do noting because the report is damaged at all.
      }
    }
  }

  
  
  
  public void setReportLevelToIdent(int nLevel, int nLevelActive)
  { throw new RuntimeException("report idents are not supported here.");
    
  }
  
  
  public int getReportLevelFromIdent(int ident)
  { if(ident >=MainCmdLogging_ifc.error && ident <=MainCmdLogging_ifc.fineDebug)
    { return ident;
    }
    else throw new RuntimeException("report idents are not supported here.");
  }

  
  
  
  public String getReportFileName()
  { return sFileLog;
  }
  

  
  protected void addMsgConfig(String sFileName){
    msgDisp = new MsgRedirectConsole(this, 0, null);
    if(sFileName.length() >0){
      File fileCfg = new File(sFileName);
      if(fileCfg.exists()){
        msgDisp.readConfig(fileCfg);
      } else {
        System.err.println("MainCmd - msgCfg file does not exits; " + fileCfg.getAbsolutePath());
      }
    }
    System.out.println("MainCmd - msgConfig installed;");
  }
  
  

  /**Sends a message. The timestamp of the message is build with the system time. 
   * All other parameter are identically see {@link #sendMsg(int, OS_TimeStamp, String, Object...)}.
   * @param identNumber of the message. If it is negative, it is the same message as positive number,
   *                    but with information 'going state', where the positive number is 'coming state'.
   * @param text The text representation of the message, format string, see java.lang.String.format(..).
   *             @pjava2c=zeroTermString.
   * @param args 0, 1 or more arguments of any type. 
   *             The interpretation of the arguments is controlled by param text.
   * @return true if the message will be dispatched, false if it is suppressed
   */  
  @Override public boolean sendMsg(int identNumber, String text, Object... args)
  { return redirectLogMessage.sendMsg(identNumber, text, args);
  }

  /**Sends a message.
   * 
   * @param identNumber of the message. If it is negative, it is the same message as positive number,
   *                    but with information 'going state', where the positive number is 'coming state'.
   * @param creationTime absolute time stamp. @Java2C=perValue.
   * @param text The text representation of the message, format string, see java.lang.String.format(..).
   *             @pjava2c=zeroTermString.
   * @param args 0, 1 or more arguments of any type. 
   *             The interpretation of the arguments is controlled by param text.
   * @return true if the message will be dispatched, false if it is suppressed
   */
  @Override public boolean sendMsgTime(int identNumber, OS_TimeStamp creationTime, String text, Object... args)
  { return redirectLogMessage.sendMsgTime(identNumber, creationTime, text, args);
  }
  

  
  /**Sends a message. The functionality and the calling parameters are identically 
   * with {@link #sendMsg(int, OS_TimeStamp, String, Object...)}, but the parameter args is varied:  
   * @param identNumber
   * @param creationTime
   * @param text
   * @param typeArgs Type chars, ZCBSIJFD for boolean, char, byte, short, int, long, float double. 
   *        @java2c=zeroTermString.
   * @param args Reference to a buffer which contains the values for a variable argument list.
   *             <br>
   *             In C implementation it is a reference either to the stack, or to a buffer elsewhere,
   *             but the reference type is appropriate to provide the values in stack
   *             for calling routines with variable argument list such as 
   *             <code>vprintf(buffer, text, args)</code>.
   *             The referenced instance shouldn't accepted as persistent outside processing time 
   *             of the called routine. Therefore stack content is able to provide.
   *             <br>
   *             In Java it is a special class wrapping a Object[] tantamount to a Object...
   *             as variable argument list. Using of this wrapper class is only a concession
   *             to C-programming, because in Java an Object[] would adequate.
   * @return true than okay. It is possible, that a destination for dispatching is not available yet.
   *         Than the routine returns false. That is for special outputs of message dispatcher. 
   *         Normally the user shouldn't realize false here and react anywise. 
   *         If a message isn't able to transport, it is not visible in the creating thread. 
   *         It is possible that a message is lost anywhere in transportation way. In Generally,
   *         to secure a complex systems functionality, any timeouts, repeats 
   *         and backup strategies are necessary 
   *         in the supervise software above sending a single message.           
   */
  @Override public boolean sendMsgVaList(int identNumber, OS_TimeStamp creationTime, String text, Va_list args){
    return redirectLogMessage.sendMsgVaList(identNumber, creationTime, text, args);
  }

  @Override
  public Appendable append(CharSequence csq) throws IOException {
    redirectLogMessage.sendMsg(0, csq.toString());
    return this;
  }

  @Override
  public Appendable append(CharSequence csq, int start, int end) throws IOException {
    redirectLogMessage.sendMsg(0, csq.subSequence(start, end).toString());
    return this;
  }

  @Override
  public Appendable append(char c) throws IOException {
    String s = "" + c;
    redirectLogMessage.sendMsg(0, s);
    return this;
  }

  
  
  
  
  
  
  /**Only preliminary, because Java2C doesn't support implementation of interfaces yet.
   * This method is implemented in C in another kind.
   * @param src 
   * @return the src.
   */
  //public final static LogMessage convertFromMsgDispatcher(LogMessage src){ return src; }
  
  /**A call of this method closes the devices, which processed the message. It is abstract. 
   * It depends from the kind of device, what <code>close</code> mean. 
   * If the device is a log file writer it should be clearly.
   * <code>close</code> may mean, the processing of messages is finite temporary. 
   * An <code>open</code> occurs automatically, if a new message is dispatched. 
   */
  public void close(){
    redirectLogMessage.close();
  }
  
  /**A call of this method causes an activating of transmission of all messages since last flush. 
   * It is abstract. It depends from the kind of device, what <code>flush</code> mean. 
   * If the device is a log file writer it should be clearly.
   * <code>flush</code> may mean, the processing of messages is ready to transmit yet. 
   */
  public void flush(){
    redirectLogMessage.flush();
  }
  
  /**Checks whether the message output is available. */
  public boolean isOnline(){
    return redirectLogMessage.isOnline();
  }



    
  final void sendMsgTimeToAppendableDst(Appendable dst, int identNumber, int reportLevel, OS_TimeStamp creationTime,
      String text, Object... args) {
    Object[] argsArg = args;  
    String line;
    if(argsArg.length == 0){
      line = dateFormatMsg.format(creationTime) + "; " + identNumber + "; " + text;
    } else {
      try{
        if(argsArg[0] instanceof Object[]){
          argsArg = (Object[])argsArg[0];
        }
        line = dateFormatMsg.format(creationTime) + "; " + identNumber + "; " + String.format(text,argsArg);
      } catch(IllegalFormatException exc){
        line = dateFormatMsg.format(creationTime) + "; FORMATEXCEPTION " + identNumber + "; " + text;
      }
    }
    try{ 
      dst.append(line);
      if(!line.endsWith("\n")){
        dst.append("\n");
      }
    } catch(IOException exc){
      //the exception may be unexpected. Write it to the original System.err on construction of this class,
      //all other channels may be redirected.
      exc.printStackTrace(this.errCmdline);
    }
  }

    

  
  /**Instance to send a LogMessage to the Log interface of maincmd.
   */
  protected LogMessage logMessageImplReport = new LogMessageBase()
  {

    @Override
    public void close() {}
  
    @Override
    public void flush() {}
  
    @Override
    public boolean isOnline() { return true; }

    @Override
    public boolean sendMsgVaList(int identNumber, OS_TimeStamp creationTime, String text, Va_list args) {
      Object oArgs = args.get();
      return sendMsgTime(identNumber, creationTime, text, oArgs);
    }

    @Override
    public boolean sendMsg(int identNumber, String text, Object... args) {
      return sendMsgTime(identNumber, OS_TimeStamp.os_getDateTime(), text, args);
    }

    @Override
    public boolean sendMsgTime(int identNumber, OS_TimeStamp creationTime, String text, Object... args) {
      final int reportLevel = identNumber == 0 ? 
                               MainCmdLogging_ifc.info :
                               identNumber <= MainCmdLogging_ifc.fineDebug ? identNumber : MainCmdLogging_ifc.info;

      final String line;
      if(args.length == 0){
        line = dateFormatMsg.format(creationTime) + "; " + identNumber + "; " + text;
      } else {
        line = dateFormatMsg.format(creationTime) + "; " + identNumber + "; " + String.format(text, args);
      }
      reportln(reportLevel, line);
      ///
      return true;
    }
    
    
    @Override
    public Appendable append(CharSequence csq) throws IOException {
      sendMsg(0, csq.toString());
      return this;
    }

    @Override
    public Appendable append(CharSequence csq, int start, int end) throws IOException {
      sendMsg(0, csq.subSequence(start, end).toString());
      return this;
    }

    @Override
    public Appendable append(char c) throws IOException {
      String s = "" + c;
      sendMsg(0, s);
      return this;
    }

    public @Override String toString(){ return "MainCmd.logMessageImplReport"; }
  };
  


  
  
  
  
  /**Inner class to send a LogMessage to the {@link MainCmd#outConsole}.
   * The last one is originally System.out from main invocation but may be redirected, 
   * see {@link MainCmd#setOutputChannels(Appendable, Appendable)}. 
   */
  private class LogMessageImplConsole extends LogMessageBase
  {

    @Override
    public void close() {}
  
    @Override
    public void flush() {}
  
    @Override
    public boolean isOnline() { return true; }

    @Override
    public boolean sendMsgVaList(int identNumber, OS_TimeStamp creationTime,
      String text, Va_list args) {
      if(args.size() ==0){
        return sendMsgTime(identNumber, creationTime, text);
      } else {
        Object oArgs = args.get();
        return sendMsgTime(identNumber, creationTime, text, oArgs);
      }
    }

    @Override
    public boolean sendMsg(int identNumber, String text, Object... args) {
      return sendMsgTime(identNumber, OS_TimeStamp.os_getDateTime(), text, args);
    }

    @Override
    public boolean sendMsgTime(int identNumber, OS_TimeStamp creationTime, String text, Object... args) {
      final int reportLevel = identNumber == 0 ? 
                               MainCmdLogging_ifc.info :
                               identNumber <= MainCmdLogging_ifc.fineDebug ? identNumber : MainCmdLogging_ifc.info;
      sendMsgTimeToAppendableDst(MainCmd.this.outConsole, identNumber, reportLevel, creationTime, text, args); 
      return true;
    }

    public @Override String toString(){ return "MainCmd.LogMessageImplConsole"; }

    @Override
    public Appendable append(CharSequence csq) throws IOException {
      sendMsg(0, csq.toString());
      return this;
    }

    @Override
    public Appendable append(CharSequence csq, int start, int end) throws IOException {
      sendMsg(0, csq.subSequence(start, end).toString());
      return this;
    }

    @Override
    public Appendable append(char c) throws IOException {
      String s = "" + c;
      sendMsg(0, s);
      return this;
    }

  }
  

  
  
  class LogMessageImplErrConsole extends LogMessageBase
  {

    @Override
    public void close() {}
  
    @Override
    public void flush() {}
  
    @Override
    public boolean isOnline() { return true; }

    @Override
    public boolean sendMsgVaList(int identNumber, OS_TimeStamp creationTime,
        String text, Va_list args) {
      Object oArgs = args.get();
      return sendMsgTime(identNumber, creationTime, text, oArgs);
    }

    @Override
    public boolean sendMsg(int identNumber, String text, Object... args) {
      return sendMsgTime(identNumber, OS_TimeStamp.os_getDateTime(), text, args);
    }

    @Override
    public boolean sendMsgTime(int identNumber, OS_TimeStamp creationTime, String text, Object... args) {
      final int reportLevel = identNumber == 0 ? 
                               MainCmdLogging_ifc.info :
                               identNumber <= MainCmdLogging_ifc.fineDebug ? identNumber : MainCmdLogging_ifc.info;
      sendMsgTimeToAppendableDst(MainCmd.this.errConsole, identNumber, reportLevel, creationTime, text, args); 
      return true;
    }

    public @Override String toString(){ return "MainCmd.LogMessageImplErrConsole"; }

    @Override public Appendable append(CharSequence csq) throws IOException {
      sendMsg(0, csq.toString());
      return this;
    }

    @Override public Appendable append(CharSequence csq, int start, int end) throws IOException {
      sendMsg(0, csq.subSequence(start, end).toString());
      return this;
    }

    @Override public Appendable append(char c) throws IOException {
      String s = "" + c;
      sendMsg(0, s);
      return this;
    }

  }
  
  class LogMessageImplFile extends LogMessageBase
  {
    final private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM-dd HH:mm:ss.SSS: ");

    @Override
    public void close() {}
  
    @Override
    public void flush() {}
  
    @Override
    public boolean isOnline() { return true; }

    @Override
    public boolean sendMsgVaList(int identNumber, OS_TimeStamp creationTime,
        String text, Va_list args) {
      String line = dateFormat.format(creationTime) + "; " + identNumber + "; " + String.format(text,args.get());
      reportln(MainCmdLogging_ifc.info, line);
      return false;
    }

    @Override
    public boolean sendMsg(int identNumber, String text, Object... args) {
      return sendMsgTime(identNumber, OS_TimeStamp.os_getDateTime(), text, args);
    }

    @Override
    public boolean sendMsgTime(int identNumber, OS_TimeStamp creationTime,
        String text, Object... args) {
      if(fReport != null)
      { String line = "*" + identNumber + "; " + dateFormat.format(creationTime) + "; ";
        fReport.writeln("");
        fReport.write(line);
        if(args.length == 0){
          //no arguments, no formatting!
          line = text;  //may be more as one line, can contain %-character.
        } else {
          line = String.format(text,args);
        }
        fReport.write(line);  //may be more as one line.
      }
      return false;
    }

    public @Override String toString(){ return "MainCmd.LogMessageImplFile"; }

    @Override public Appendable append(CharSequence csq) throws IOException {
      sendMsg(0, csq.toString());
      return this;
    }

    @Override public Appendable append(CharSequence csq, int start, int end) throws IOException {
      sendMsg(0, csq.subSequence(start, end).toString());
      return this;
    }

    @Override public Appendable append(char c) throws IOException {
      String s = "" + c;
      sendMsg(0, s);
      return this;
    }


  }
  
  LogMessageImplConsole logMessageConsole = new LogMessageImplConsole();
  
  LogMessageImplErrConsole logMessageErrConsole = new LogMessageImplErrConsole();
  
  LogMessageImplFile logMessageFile = new LogMessageImplFile();
  
  @Override public LogMessage getLogMessageOutputConsole(){ return logMessageConsole; }
  
  @Override public LogMessage getLogMessageErrorConsole(){ return logMessageErrConsole; }
  
  @Override public LogMessage getLogMessageOutputFile(){ return logMessageFile; }

  /**Gets the singleton MainCmd instance as Logging interface or returns a {@link MainCmdLoggingStream}
   * instance if this class is not instanciated.
   */
  public static MainCmdLogging_ifc getLogging_ifc(){ 
    if(singleton !=null) return singleton; 
    else return new MainCmdLoggingStream(System.out);
  }
  
  
  /**Its a helper to set a breakpoint for assert
   * @param condition
   */
  public static void XXXassertion(boolean condition){
    if(!condition){
      throw new RuntimeException ("assertion");
    }
  }

  /*===========================================================================================================*/
  /** Class to write any readed stream to the output, running in a separate thread.
   *  Used especially by command invokation
  */
  private class ShowCmdOutput implements Runnable
  {
 
    /** The reader to read the contents*/
    final BufferedReader reader;

    /** Captures the output if not null, see constructor*/
    final Appendable uOutput;

    /**control of the output medium. See constructor, value nCtrl*/
    final int nCtrl;


    /** Creates a new ShowCmdOutput
        The output is directed in the followed manner with the value of nCtrl
        <table border=1>
          <tr><th>nCtrl</th><th>meaning</th></tr>
          <tr><td>MainCmdLogging_ifc.error .. MainCmdLogging_ifc.fineDebug</td>  <td>Writes the output to the report</td></tr>
          <tr><td>-MainCmdLogging_ifc.error (negativ)         </td>   <td>Writes the output via writeError()</td></tr>
          <tr><td>-MainCmdLogging_ifc.warning (negativ)       </td><td>Writes the output via writeWarning()</td></tr>
          <tr><td>-MainCmdLogging_ifc.info (negativ)          </td><td>Writes the output via writeInfo()
                                                           usw.                            </td></tr>
          <tr><td>other value</td>               <td>cause an exception, it is wrong.</td></tr>
        </table>
        @param nCtrl Controls the output channel: 1..6 using MainCmdLogging_ifc.error to MainCmdLogging_ifc.fineDebug writes to the report;
                     -1..-3 (negative) using -MainCmdLogging_ifc.error to -MainCmdLogging_ifc.info  writes via writeInfo
        @param in The input stream to read.
    */
    ShowCmdOutput(Appendable output, int nCtrl, BufferedReader in)
    { this.uOutput = output;
      this.nCtrl   = nCtrl;
      this.reader  = in;
    }


    /** runnable in a separate thread, reads the reader and write to*/
    public void run()
    { String sOut;
      try
      { while( (sOut = reader.readLine()) != null)
        { if(uOutput!=null) uOutput.append(sOut + "\n");

          if     (nCtrl == -MainCmdLogging_ifc.error)  writeError(sOut);
          else if(nCtrl == -MainCmdLogging_ifc.warning)writeWarning(sOut);
          else if(nCtrl == -MainCmdLogging_ifc.info)   writeInfoln(sOut);
          else if(nCtrl == 0);   //no output
          else reportln(nCtrl, sOut);
        }
      }
      catch(IOException exception){ System.out.println(exception.toString()); }
      //System.out.println("***** ende **********");
    }
  }
  /*===========================================================================================================*/

}













                           