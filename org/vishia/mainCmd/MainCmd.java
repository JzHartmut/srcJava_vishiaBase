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
 * @version 2006-06-15  (year-month-day)
 * list of changes:
 * 2006-05-00: JcHartmut www.vishia.de creation
 * 2008-04-02: JcHartmut some changes
 *
 ****************************************************************************/

package org.vishia.mainCmd;

//import org.jdom;
import java.io.*;
import java.util.*;  //List
import java.text.*;  //ParseException
import org.vishia.util.FileSystem;

/**
<h1>class MainCmd - Description</h1>
<font color="0x008000">
    Diese abstrakte Klasse dient als Basisklasse für alle cmdline-Applikationen. Diese Klasse enthält folgende Leistungseigenschaften:
    <ul><li>Erfassen der Kommandozeilenargumente: Als Erweiterung ist geplant ::TODO:: bei Angabe @filepath werden
            die Argumente aus einem File gelesen, und zwar pro Zeile ein Argument auch mit Leerzeichen. Der Anwender
            braucht dies selbst nicht zu berücksichtigen, siehe parseArguments() und getArgument()</li>
        <li>Relalisieren aller System-Ausgaben auf das CmdLine-Fenster oder anders geeignet:
            Der User ruft die entsprechenden Methoden writeHelpInfo(), writeInfo() usw. auf und nicht direkt
            System.out.println... Damit ist es in dieser Klasse möglich, die Ausgabe geeignet umzuleiten, beispielsweise
            für Windows-Applikationen.
        <li>Bereitstellen einer Schnittstelle für Report in eine Datei, es wird das Interface Report implementiert.</li>
        <li>Erfassen des Maximum des ExitErrorLevel, siehe setExitErrorLevel().</li>
        <li>Bereitstellen einer Methode, um auf einfache Weise andere Kommandozeilen auszuführen, siehe executeCmdLine()</li>
        <li>Bereitstellen eines Systems für die Applikation (abstrakte Methoden), wie Informationen nach außen gegeben werden
            sollen. Diese Methoden werden unter anderem in der MainCmdWin benutzt und sollen zu einer Einheitlichkeit führen.
            Siehe writeHelpInfo(), writeAboutInfo().
    </ul>
    </font>
    This is a abstract superclass to support cmd line applications. The class contents the followed features:
    <ul><li>Parsing of cmd line arguments: </li>
        <li>Realising of the system outputs</li>
        <li>Provides an interface for reporting to a file.</li>
        <li>Gathering of the maximum of the exitErrorLevel, see setExitErrorLevel().</li>
        <li>Provides a method to execute external cmd lines in a simple way for the user, see executeCmdLine()</li>
        <li>Defining of a system for the application (abstract methods), for the way to give informations from the application.
            see <a href=#writeAboutInfo()>writeAboutInfo()</a>, <a href=#writeHelpInfo()>writeHelpInfo()</a>.
    </ul>
    This class is extended by <a href="MainCmdWin">MainCmdWin</a>, the methods supporting command line things
    are adapted there for a GUI (Graphical User Interface) -style application. In the GUI frame the user used the same
    interface MainCmd_Ifc to do something with the applications frame.<br/>
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
<hr/>
<pre>
date       who        what
2007-03-07 JcHartmut  Method getReportFileName
2007-03-07 JcHartmut  callWithoutArgument do not exit but throws an exception likewise by an error of arguments.
2006-04-02 JcHartmut  writeInfo__ new methods write__Directly, solution of concurrency to write to report.
2006-01-17 JcHartmut  testArgument() may thrown an ParseException.
2006-01-21 JcHartmut  bugfix callWithoutArguments() should be protected, not package private.
2004-06-00 JcHartmut  initial revision

*
</pre>
<hr/>
*/

public abstract class MainCmd implements MainCmd_ifc
{
  /** The report file. This attribute is set to null, if no report is desired by missing the argument -r REPORTFILE
  */
  private FileWrite fReport;

  /** All reports with a level less than or equal this level will be reported. Other report calls has non effect.*/
  private int nReportLevel = Report.info;  //default: reports errors, warnings and info, but no debug

  private String sFileReport = "report.txt";

  /** All reports with a level less than or equal this level will be written on display.*/
  private int nReportLevelDisplay = 0;   //default: don't write reports to display

  /** writeError(), writeWarning(), writeInfo()  are also reported if the level is equal or less than 1,2,3*/
  private int nLevelDisplayToReport = 0;

  /** List of strings contents help Info in form of lines. The list should be filled with method addHelpInfo(String);*/
  private List<String> listHelpInfo = new LinkedList<String>();

  /** List of strings contents about Info in form of lines. The list should be filled with method addHelpInfo(String);*/
  private List<String> listAboutInfo = new LinkedList<String>();

  /** The maximum of the value given with setExitErrorLevel */
  private int nMaxErrorLevel = MainCmd_ifc.exitSuccessfull;


  /** array of arguments from command line*/
  protected String[] cmdLineArgs;

  /** number of the argument is parsing*/
  private int iArgs;

  /** access to some spaces via Report.spaces.substring(0,n)*/
  private static final String report_spaces = "                                                                                ";


  /*===========================================================================================================*/
  /** Class to write any readed stream to the output, running in a separate thread.
   *  Used especially by command invokation
  */
  private class ShowCmdOutput implements Runnable
  {
    /** To response the outer class outer may be used.*/
    //final MainCmd outer = (MainCmd)(this);  //:TODO: why is that an error?

    /** The reader to read the contents*/
    final BufferedReader reader;

    /** Captures the output if not null, see constructor*/
    final StringBuffer sOutput;

    /**control of the output medium. See constructor, value nCtrl*/
    final int nCtrl;


    /** Creates a new ShowCmdOutput
        The output is directed in the followed manner with the value of nCtrl
        <table border=1>
          <tr><th>nCtrl</th><th>meaning</th></tr>
          <tr><td>Report.error .. Report.fineDebug</td>  <td>Writes the output to the report</td></tr>
          <tr><td>-Report.error (negativ)         </td>   <td>Writes the output via writeError()</td></tr>
          <tr><td>-Report.warning (negativ)       </td><td>Writes the output via writeWarning()</td></tr>
          <tr><td>-Report.info (negativ)          </td><td>Writes the output via writeInfo()
                                                           usw.                            </td></tr>
          <tr><td>other value</td>               <td>cause an exception, it is wrong.</td></tr>
        </table>
        @param nCtrl Controls the output channel: 1..6 using Report.error to Report.fineDebug writes to the report;
                     -1..-3 (negative) using -Report.error to -Report.info  writes via writeInfo
        @param in The input stream to read.
    */
    ShowCmdOutput(StringBuffer output, int nCtrl, BufferedReader in)
    { this.sOutput = output;
      this.nCtrl   = nCtrl;
      this.reader  = in;
    }


    /** runnable in a separate thread, reads the reader and write to*/
    public void run()
    { String sOut;
      try
      { while( (sOut = reader.readLine()) != null)
        { if(sOutput!=null) sOutput.append(sOut + "\n");

          if     (nCtrl == -Report.error)  writeError(sOut);
          else if(nCtrl == -Report.warning)writeWarning(sOut);
          else if(nCtrl == -Report.info)   writeInfoln(sOut);
          else if(nCtrl == 0);   //no output
          else reportln(nCtrl, sOut);
        }
      }
      catch(IOException exception){ System.out.println(exception.toString()); }
      //System.out.println("***** ende **********");
    }
  }
  /*===========================================================================================================*/



  /** Empty Constructor of the main class. The command line args should be
   * set in the derivated class.
  */
  protected MainCmd()
  { 
  }

  /** Constructor of the main class.
  */
  protected MainCmd(String[] args)
  { this.cmdLineArgs = args;
  }


  /** Adds the help info for standard arguments. The text is the followed:<pre>
    addHelpInfo("--about show the help infos");
    addHelpInfo("--help  show the help infos");
    addHelpInfo("--report:FILE  write the report (log) into the given file, create or clear the file.");
    addHelpInfo("--report+:FILE add to the end of given file or create the report file.");
    addHelpInfo("--rlevel:R     set the level of report, R is number from 1 to 6.");
    addHelpInfo("--rlevel:DR    also write reports upto level D on display, sample: ..-rlevel:24");
    addHelpInfo("--rlevel:WDR   write output also in report, W is nr from 1 to 3 (error, warning, info");
    </pre>
  */
  protected void addStandardHelpInfo()
  {
    addHelpInfo("--about show the help infos");
    addHelpInfo("--help  show the help infos");
    addHelpInfo("--report:FILE  write the report (log) into the given file, create or clear the file.");
    addHelpInfo("--report+:FILE add to the end of given file or create the report file.");
    addHelpInfo("--rlevel:R     set the level of report, R is number from 1 to 6.");
    addHelpInfo("--rlevel:DR    also write reports upto level D on display, sample: ..-rlevel:24");
    addHelpInfo("--rlevel:WDR   write output also in report, W is nr from 1 to 3 (error, warning, info");
  }





  /** Addes a helpinfo-line to the internal list. This method should be called from the user on startup to set the
      help infos. The help info is used by <a href="#writeHelpInfo()"><code>writeHelpInfo()</code>
      called if the argument --help is given or without arguments. </a>.
      @param info String contains 1 line of a help Info.
  */
  protected void addHelpInfo(String info)
  { listHelpInfo.add(info);
  }

  /** Addes a helpinfo-line to the internal list. This method should be called from the user on startup to set the
  help infos. The help info is used by evaluating command line arguments.
  @param info String contains 1 line of a help Info.
  */
  protected void addAboutInfo(String info)
  { listAboutInfo.add(info);
  }


  /** prints the help info to the console output. This Method may be overloaded in MainCmdWin to show the contents in a window.
  */
  public void writeHelpInfo()
  { writeAboutInfo();
    Iterator<String> iHelpInfo = listHelpInfo.iterator();
    while(iHelpInfo.hasNext())
    {
      System.out.println((String)(iHelpInfo.next()));
    }
  }



  /** prints the help info to the console output. This Method may be overloaded in MainCmdWin to show the contents in a window.
  */
  public void writeAboutInfo()
  {
    Iterator<String> iInfo = listAboutInfo.iterator();
    while(iInfo.hasNext())
    {
      System.out.println((String)(iInfo.next()));
    }
  }


  /**Parses the cmdLine-arguments. The args should be set in constructor.
   * @see parseArguments(String [] args)
   * 
   * @throws ParseException
   */
  public void parseArguments()
  throws ParseException
  {
    parseArguments(cmdLineArgs);
  }

  /**Parses the cmdLine-arguments. The user should invoked this method in his static main method
   * or in the main-method often named execute().<br/>
     On every not-standard argument this method calles <code>testArgument(String, int)</code> inside.
     That is a abstract methode, the user must overwrite it, see there. If this method returns false,
     a <code>writeError()</code> with a matched text is called and a ParseException("", N) ist thrown, N is the
     number of the argument.
     Standard arguments are parsed here, see <a href="#addStandardHelpInfo()"><code> addStandardHelpInfo()</code></a>.
     On end the methode <code>checkArguments()</code> is called.
     If it returns false, an ParseException("", 0) is thrown.
     The user should had write an output message with writeError() inside its <code>checkArguments()</code><br/>
     :TODO: It should be helpfull to read arguments also from file. An cmd line argument in the form @@filepath
     should be evaluated in this form, that every line of the readed file is used as one argument additionaly.
     At example a cmd line invoke like <code>>cmd -a arg1 @@file -x argx</code> results in the followed argument array:<br/>
     <code>-a arg1 arguments-from-file-lines -x argx</code>
  */
  public void parseArguments(String [] args)
  throws ParseException
  { cmdLineArgs = args;
    MainCmd main = this;
    /* create main instance:*/
    //MainCmd main = createMainInstance();    //1 instance of main class

    //if(cmdLineArgs.length < 1) throw new ParseException("no cmdline Arguments", 0);

    
    if(cmdLineArgs.length == 0)
    { //call without parameters:
      callWithoutArguments();
    }
    boolean bAppendReport = false;
    { iArgs = 0;
      while(iArgs < cmdLineArgs.length)
      { if     (cmdLineArgs[iArgs].startsWith("--rlevel:"))
        { int level;
          try{ level = Integer.parseInt(getArgument(9)); } //;cmdLineArgs[iArgs].substring(7,8)); }
          catch(Exception e)
          { throw new ParseException("ERROR on argument --rlevel:" + cmdLineArgs[iArgs], iArgs);
          }
          main.nReportLevel          = level % 10;
          main.nReportLevelDisplay   = (level / 10) % 10;  //0: no report on display.
          main.nLevelDisplayToReport = (level / 100) % 10;  //0: no display to report.
        }
        else if(cmdLineArgs[iArgs].startsWith("--report+:")) { sFileReport = getArgument(10); bAppendReport = true; }
        else if(cmdLineArgs[iArgs].startsWith("--report:")) { sFileReport = getArgument(9); }
        else if(cmdLineArgs[iArgs].startsWith("--about")) { writeAboutInfo(); }
        else if(cmdLineArgs[iArgs].startsWith("--help")) { writeHelpInfo(); }
        else
        { if(!main.testArgument(cmdLineArgs[iArgs], iArgs))
          { main.writeError("failed argument:" + cmdLineArgs[iArgs]);
            throw new ParseException("failed argument:" + cmdLineArgs[iArgs], iArgs);  //ParseException used from java.text
          }
        }
        iArgs +=1;
      }
    }
    //debug System.out.println("report level=" + main.nReportLevel + " file:" + sFileReport);
    /** open reportfile: */
    if(nReportLevel > 0)
    { boolean bMkdir= false; 
      try{ main.fReport = new FileWrite(sFileReport, bAppendReport); }
      catch(IOException exception)
      { //it is possible that the path not exist.
        bMkdir = true;
      }
      if(bMkdir)
      { try
        { FileSystem.mkDirPath(sFileReport); 
          main.fReport = new FileWrite(sFileReport, bAppendReport);
        }
        catch(FileNotFoundException exc)
        { writeError("ERROR creating reportfile-path: " +sFileReport);
          throw new ParseException("ERROR creating reportfile-path: " +sFileReport, 0);
        /*if a requested reportfile is not createable, the programm can't be run. The normal problem reporting fails.
          That's why the program is aborted here.
        */
        }
      }  

    }
    if(!checkArguments())
    {
      throw new ParseException("arguments not consistent", 0);  //ParseException used from java.text
    }
  }


  /** Checks the arguments after parsing from command line, test of consistence. This method must overwrite from
   * the user, may be with a return true. The method is called at last inside parseArguments().
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
  { writeAboutInfo();
    writeHelpInfo();
    throw new ParseException("no cmdline Arguments", 0);
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
      The user must overwrite this method to test the application-specific paramters.
      @param argc The argument to test
      @param nArg number of argument, the first argument is numbered with 1.
      @return false, if the argument doesn't match, true if ok.
              if the method returns false, an exception in parseArgument is thrown to abort the argument test
              or to abort the programm. @see <a href="#parseArguments()">parseArgument()</a>.<br/>

      @exception ParseException The Methode may thrown this excpetion if a conversion error or such other is thrown.
  */
  protected abstract boolean testArgument(String argc, int nArg)
  throws ParseException;







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
  */
  public int executeCmdLine(String cmd, int nReportLevel, StringBuffer output, String input)
  {
    String[] cmdArray = cmd.split(" ",0);
    return executeCmdLine(cmdArray, nReportLevel, output, input);
  }





  /** Execute a command invoke a cmdline call, implements MainCmd_Ifc.
      The call must not be needed any input (:TODO:?).
      The output is written with a separate thread, using the internal (private) class ShowCmdOutput.
      This class use the method writeInfoln() from here. The writeInfoln-Method writes to console for MainCmd,
      but it may be overloaded, to example for MainCmdWin it may be writed to a box in the GUI.
  */
  public int executeCmdLine(String[] cmd, int nReportLevel, StringBuffer output, String input)
  {
    int exitErrorLevel; // = 0;  //default unused, only because throw
    { StringBuffer sOut = new StringBuffer();
      int iCmd = 0;
      while(iCmd < cmd.length)
      { sOut.append(cmd[iCmd]); iCmd +=1;
        sOut.append(" ");
      }
      sOut.append(":");
      reportln(Report.debug, "MainCmd.executeCmdLine():" + sOut);
      writeInfoln("execute>" + sOut.toString());
    }
    try
    { Process process = Runtime.getRuntime().exec(cmd);
      InputStream processOutput = process.getInputStream();  //reads the output from exec.
      InputStream processError  = process.getErrorStream();  //reads the error from exec.

      Runnable cmdOutput = new ShowCmdOutput(output, nReportLevel, new BufferedReader(new InputStreamReader(processOutput) ));
      Thread threadOutput = new Thread(cmdOutput);
      threadOutput.start();  //the thread reads the processOutput and disposes it to the infoln

      Runnable cmdError = new ShowCmdOutput(null, nReportLevel < 0 ? -Report.error: Report.error, new BufferedReader(new InputStreamReader(processError) ));
      Thread threadError = new Thread(cmdError);
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
      throw new RuntimeException("IOException on commandline");
    }
    catch ( InterruptedException ie )
    {
      writeInfoln( ie.toString() );
      throw new RuntimeException("cmdline interrupted");
    }
    return exitErrorLevel;
  }



  /*--------------------------------------------------------------------------------------------------------*/
  /*--------------------------------------------------------------------------------------------------------*/
  /** Writes an information, implementation of MainCmd_Ifc.
      The info is written also to report depended on command line arguments --rlevel.
      If the user will overwrite the kind of output, overwrite writeInfoDirectly, it is called here.
  */
  public final void writeInfo(String sInfo)
  { writeDirectly(sInfo, kInfo_writeInfoDirectly);
    if(nLevelDisplayToReport >= Report.info)
    { report(Report.info | Report.mNeverOutputToDisplay, sInfo );
    }
  }

  /** Writes an information line, implementation of MainCmd_Ifc.
      The info is written also to report depended on command line arguments --rlevel.
      If the user will overwrite the kind of output, overwrite writeInfolnDirectly, it is called here.
  */
  public final void writeInfoln(String sInfo)  //##a
  { writeDirectly(sInfo, kInfoln_writeInfoDirectly);
    if(nLevelDisplayToReport >= Report.info)
    { reportln(Report.info | Report.mNeverOutputToDisplay, sInfo );
    }
  }


  /** Writes an warning line, implementation of MainCmd_Ifc.
      The info is written also to report depended on command line arguments --rlevel.
      If the user will overwrite the kind of output, overwrite writeWarningDirectly, it is called here.
  */
  public final void writeWarning(String sInfo)
  { writeDirectly(sInfo, kWarning_writeInfoDirectly);
    if(nLevelDisplayToReport >= Report.warning)
    { reportln(Report.warning | Report.mNeverOutputToDisplay, "WARNING:" + sInfo );
    }
  }

  /** Writes an error line, implementation of MainCmd_Ifc.
      The info is written also to report depended on command line arguments --rlevel.
      If the user will overwrite the kind of output, overwrite writeErrorDirectly, it is called here.
  */
  public final void writeError(String sInfo)
  { writeDirectly(sInfo, kError_writeInfoDirectly);
    if(nLevelDisplayToReport >= Report.error)
    { reportln(Report.error | Report.mNeverOutputToDisplay, "ERROR:" + sInfo );
    }
  }


  /** Writes an error line with exception info, implementation of MainCmd_Ifc.
      The info is written also to report depended on command line arguments --rlevel.
      If the user will overwrite the kind of output, overwrite writeErrorDirectly, it is called here.
  */
  public final void writeError(String sInfo, Exception exception)
  { writeErrorDirectly(sInfo, exception);
    if(nLevelDisplayToReport >= Report.error)
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
  protected static final short kInfoln_writeInfoDirectly   = (short)0x3;
  protected static final short kWarning_writeInfoDirectly  = (short)0x6;
  protected static final short kError_writeInfoDirectly    = (short)0xa;

  /** If this bit is setted in param kind of writeDirectly
   * a newline should be inserted at begin of output
   */
  protected static final short mNewln_writeInfoDirectly   = (short)0x2;

  /** If this bit is setted in param kind of writeDirectly
   * a warning is to be outputted
   */
  protected static final short mWarning_writeInfoDirectly = (short)0x4;

  /** If this bit is setted in param kind of writeDirectly
   * an error is to be outputted
   */
  protected static final short mError_writeInfoDirectly   = (short)0x8;


  /** Writes out the Info, Warning or Error in the users way.

   * This method is overwriteable to form or configure the users requests.
     @param sInfo The String to write
     @param kind Ones of the kWriteOut__
   */
  protected void writeDirectly(String sInfo, short kind)  //##a
  {
    if((kind & mWarning_writeInfoDirectly) != 0)
    {
      System.err.println("");
      System.err.println( "WARNING: " + sInfo );
    }
    else if((kind & mError_writeInfoDirectly) != 0)
    {
      System.err.println("");
      System.err.println( "WARNING: " + sInfo );
    }
    else
    { if( (kind & mNewln_writeInfoDirectly) != 0) System.out.println(""); //finishes the previous line
      int posStart = 0;
      int posEol;
      while( posStart < sInfo.length() && (posEol = sInfo.indexOf('\n', posStart)) >=0)
      { System.out.print(sInfo.substring(posStart, posEol) + "|");
        posStart = posEol + 1;
      }
      if(posStart < sInfo.length()) System.out.print(sInfo.substring(posStart));
    }
  }




  /** Writes an error line by console application directly to System.err.println
      with the String "EXCEPTION: " before and the exception message.
      The user can overwrite this method in the derived class of MainCmd to change to kind of output.
      @param sInfo Text to write in the new line after "EXCEPTION: ".
      @param exception Its getMessage will be written.
  */
  protected void writeErrorDirectly(String sInfo, Exception exception)
  {
    System.err.println("");
    System.err.println( "EXCEPTION: " + sInfo + exception.getMessage());
    exception.printStackTrace(System.err);
  }

  /*--------------------------------------------------------------------------------------------------------*/
  /*--------------------------------------------------------------------------------------------------------*/
  /* implements Report.*/


  /** report appending on exist line.
      If the report contains an newline char \n, a char '|' will be written.
      This newline is not interpreted as newline for the report file, but only as stochastic contents in the string.
    * @param nLevel write the report only if the demand level is greater or equal.
    * @param ss String to write.
  */
  public void report(int nLevel, String ss)
  { if( (nLevel & mReportLevel) <= nReportLevel && fReport != null)
    { int posStart = 0;
      int posEol;
      while( posStart < ss.length() && (posEol = ss.indexOf('\n', posStart)) >=0)
      { fReport.write(ss.substring(posStart, posEol) + "|");
        posStart = posEol + 1;
      }
      if(posStart < ss.length()){ fReport.write(ss.substring(posStart)); }
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
  public void reportln(int nLevel, int nLeftMargin, String ss)
  { if( (nLevel & Report.mReportLevel) <= nReportLevel && fReport != null)
    { fReport.writeln("");
      if(nLeftMargin > report_spaces.length()/2) nLeftMargin = report_spaces.length()/2;
      fReport.write("*" + (nLevel & Report.mReportLevel) + ":" + report_spaces.substring(0, 2*nLeftMargin));
    }
    if((nLevel & Report.mReportLevel) <= nReportLevelDisplay && (nLevel & Report.mNeverOutputToDisplay) == 0 )
    { //writes also an error as info on display.
      writeDirectly(ss, kInfoln_writeInfoDirectly);
    }
    report((nLevel | Report.mNeverOutputToDisplay), ss);
  }

  public void reportln(int nLevel, String ss){ reportln(nLevel, 0, ss); }


  /** Reports an exception. This report is written unconditional, like Report.error.
      On Display, this report is only written if display output is enabled for the level Report.error.
      @param sInfo Text to write in the new line after "EXCEPTION: ".
      @param exception Exception info to write
  */
  public void report(String sInfo, Exception exception)
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
  private void report(String sInfo, Exception exception, boolean bWrittenOnDisplay)
  { if(fReport == null && !bWrittenOnDisplay)
    { writeErrorDirectly(sInfo, exception);
      //exception.printStackTrace(System.out);
    }
    else
    { fReport.writeln("\nEXCEPTION: " + sInfo + "  " + exception.getMessage());
      exception.printStackTrace(new PrintStream(fReport, true));
      if(Report.error <= nReportLevelDisplay && !bWrittenOnDisplay)
      { //writes also an error as info on display.
        writeErrorDirectly(sInfo, exception);
      }
    }
  }


  /** Test wether the report is in the level.
  */
  public int getReportLevel(){ return nReportLevel; }
  
  /** Set another level inside programming. It is advisable to restore the old level
   * after the designated operation.
   * @param newLevel The level to be set, use one of the defines Report.info to Report.fineDebug
   * @return the current level, usefull to restore it.
   */
  public int setReportLevel(int newLevel)
  { int oldLevel = nReportLevel;
    nReportLevel = newLevel;
    return oldLevel;
  }


  public String getReportFileName()
  { return sFileReport;
  }
  
  
  
}













                           