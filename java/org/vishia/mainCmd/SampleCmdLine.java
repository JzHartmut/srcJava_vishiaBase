package org.vishia.mainCmd;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;


import org.vishia.msgDispatch.MsgPrintStream;



/**Sample for a main-class for a application using {@link MainCmd}-super-class.....<br/>
   This is a sample and an example. The user should do the followed to use the class as a template:
   <ul><li>copy this file under the requested name and package</li>
       <li>adapt the package line</li>
       <li>replace 'CmdLineSample' with the user's main class name</li>
       <li>look on all :TODO: -location and change/complete the source there</li>
   </ul>
   You can adapt and change one after another.
   <br><br>
   The toplevel class contains three static sub classes and the main routine. The sub classes may be written in extra files
   in the same package instead. Because there are static, it's easy.
   <br><br>
   The three inner classes are:
   <ul>
   <li>The class {@link UserMain} is an example for the user's class. It contains some less statements to show what's work
     but it does not contain essential code.
   <li>A {@link Args} class to hold the data from the cmd line invocation in a proper form or some other values too.
      This class may be an independent mediator between an independent {@link UserMain} and the {@link MainCmd} implementor.
   <li>The class {@link CmdLine} is derived from {@link MainCmd} and implements the command line invocation necessities
     of this example.
   </ul>        
            
 */

public class SampleCmdLine
{

  
  /**Version, history and license.
   * <ul>
   * <li>2013-02-23 Hartmut chg: {@link UserMain} as extra inner class, LogMessage, commented.
   * <li>2009-03-21: Hartmut The parsed arguments are written in an extra static class Args, it is better
   *                     and it is more structured. The application can read arguments via knowledge only of this class,
   *                     without dependencies of the other code. The arguments may be supplied or corrected
   *                     also with other algorithm.
   * <li>2009-03-21: Hartmut The class CmdLine based on MainCmd is static. This class is only a definition wrapper.
   *                     The application can/should use this class non-depended from arguments etc. via MainCmd_ifc.
   * <li>2009-03-21: Hartmut The main-method instantiates at first only the classes 
   *                     {@link SampleCmdLine.CmdLine} and the {@link SampleCmdLine.Args} but not the main class itself,
   *                     because the Main-class of the application may got some other aggregates, 
   *                     which may created first and outside (dependency injection, inside using final).
   *                     It is also possible to create more as one classes in {@link SampleCmdLine#reflexAccessSfc}, 
   *                     especially {@link SampleCmdLine} may only be an empty wrapper 
   *                     for organization of main, Args and MainCmd, without deeper dependencies to the core application.                                             
   * </ul>
   * 
   * <b>Copyright/Copyleft</b>:<br>
   * Because this class is a template only, you can use a copy without any license notification.
   * For the original code the LGPL Lesser General Public License,
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
   *    but doesn't need to publish the whole source of the own product.
   * <li> You can study and modify (improve) this source
   *    for own using or for redistribution, but you have to license the
   *    modified sources likewise under this LGPL Lesser General Public License.
   *    You mustn't delete this Copyright/Copyleft inscription in this source file.
   * </ol>
   * 
   * @author Hartmut Schorrig = hartmut.schorrig@vishia.de
   */
  public static final int version = 20130223;

  
  
  
  
  /*---------------------------------------------------------------------------------------------*/
  /** main started from java*/
  public static void main(String [] cmdlineargs)
  { Args arguments = new Args();     //holds the command line arguments.
    CmdLine mainCmdline = new CmdLine(cmdlineargs, arguments); //the instance to parse arguments and others.
    boolean bOk = true;
    try{ mainCmdline.parseArguments(); }
    catch(Exception exception)
    { mainCmdline.setExitErrorLevel(MainCmd_ifc.exitWithArgumentError);
      bOk = false;
    }
    if(bOk)
    { //With this two lines the System.err PrintStream is redirected to the LogMessage interface of MainCmd.
      //Any System.err.println("Identifier - message; information") is redirected.
      //The usage of the MessageDispatcher is recommended too, but it is not part of this package.
      MsgPrintStream redirectSystemErr = new MsgPrintStream(mainCmdline, 15000, 5000, 100);
      System.setErr (redirectSystemErr.getPrintStreamLog("err."));
      
      /**Now instantiate the main class. 
       * It is possible to create some aggregates (final references) first outside, depends on args.
       * Therefore the main class is created yet only here.
       */
      UserMain main = new UserMain(mainCmdline);     //the main instance
      /** The execution class knows the SampleCmdLine Main class in form of the MainCmd super class
          to hold the contact to the command line execution.
      */
      try{ main.execute(arguments); }
      catch(Exception exception)
      { //catch the last level of error. No error is reported direct on command line!
        mainCmdline.report("Uncatched Exception on main level:", exception);
        mainCmdline.setExitErrorLevel(MainCmd_ifc.exitWithErrors);
      }
    }
    mainCmdline.exit();
  }

  
  /**This class holds the got arguments from command line. 
   * It is possible that some more operations with arguments as a simple get process.
   * The arguments may got also from configuration files named in cmd line arguments or other.
   * The user of arguments should know (reference) only this instance, not all other things arround MainCmd,
   * it it access to arguments. 
   */
  static class Args
  {
    /*---------------------------------------------------------------------------------------------*/
    /*::TODO:: for every argument of command line at least one variable should be existed.
      The variable is to set in testArgument.
      It is possible that one variable can used in several command line arguments.
    */
  
    /**Cmdline-argument, set on -i: option. Inputfile to to something. :TODO: its a example.*/
    String sFileIn = null;
  
    /**Cmdline-argument, set on -o: option. Outputfile to output something. :TODO: its a example.*/
    String sFileOut = null;
  }



  
  /**The inner class CmdLine helps to evaluate the command line arguments
   * and show help messages on command line.
   */
  static class CmdLine extends MainCmd
  {
    final Args cmdlineArgs;
    
  
    /*---------------------------------------------------------------------------------------------*/
    /** Constructor of the main class.
        The command line arguments are parsed here. After them the execute class is created as composition of SampleCmdLine.
    */
    private CmdLine(String[] argsInput, Args cmdlineArgs)
    { super(argsInput);
      this.cmdlineArgs = cmdlineArgs;
      //:TODO: user, add your help info!
      //super.addHelpInfo(getAboutInfo());
      super.addAboutInfo("Sample cmdLine");
      super.addAboutInfo("made by JcHartmut, 2006-01-06");
      super.addHelpInfo("Sample of a java programm.");
      super.addHelpInfo("param: -i:INPUT -o:OUTPUT");
      super.addStandardHelpInfo();
      super.addHelpInfo("-i:INPUT    inputfilepath, this file is testing.");
      super.addHelpInfo("-o:OUTPUT   outputfilepath, this file is written.");
  
    }
  
  
  
  
  
  
    /*---------------------------------------------------------------------------------------------*/
    /** Tests one argument. This method is invoked from parseArgument. It is abstract in the superclass MainCmd
        and must be overwritten from the user.
        :TODO: user, test and evaluate the content of the argument string
        or test the number of the argument and evaluate the content in dependence of the number.
  
        @param argc String of the actual parsed argument from cmd line
        @param nArg number of the argument in order of the command line, the first argument is number 1.
        @return true is okay,
                false if the argument doesn't match. The parseArgument method in MainCmd throws an exception,
                the application should be aborted.
    */
    @Override
    protected boolean testArgument(String arg, int nArg)
    { boolean bOk = true;  //set to false if the argc is not passed
  
      if(arg.startsWith("-i:"))      cmdlineArgs.sFileIn   = getArgument(3);
      else if(arg.startsWith("-o:")) cmdlineArgs.sFileOut  = getArgument(3);
      else bOk=false;
  
      return bOk;
    }
  
    /**Invoked from parseArguments if no argument is given. In the default implementation a help info is written
     * and the application is terminated. The user should overwrite this method if the call without command line arguments
     * is meanfull.
     * @throws ParseException 
     *
     */
    @Override protected void callWithoutArguments() throws ParseException
    { writeAboutInfo(null);
      writeHelpInfo(null);
      //throw new ParseException("no cmdline Arguments", 0);
    }
  
  
  
  
    /*---------------------------------------------------------------------------------------------*/
    /**Checks the cmdline arguments relation together.
       If there is an inconsistents, a message should be written. It may be also a warning.
       :TODO: the user only should determine the specific checks, this is a sample.
       @return true if successfull, false if failed.
    */
    @Override
    protected boolean checkArguments()
    { boolean bOk = true;
  
      if(cmdlineArgs.sFileIn == null)            { bOk = false; writeError("ERROR argument -i: is obligat."); }
      else if(cmdlineArgs.sFileIn.length()==0)   { bOk = false; writeError("ERROR argument -i: without content.");}
  
      if(cmdlineArgs.sFileOut == null)           { writeWarning("argument -o: no outputfile is given, use default"); cmdlineArgs.sFileOut = "out.txt";}
      else if(cmdlineArgs.sFileOut.length()==0)  { bOk = false; writeError("argument -o: without content"); }
  
      if(!bOk) setExitErrorLevel(exitWithArgumentError);
      bOk = true;  //experience: Example without arguments
      return bOk;
    
    }
  }//class CmdLine
  

  
  
  
  
/**This class represents a user's class. It is independent of the MainCmd and may need some values from the 
 * {@link Args} for construction.
 * <br><br>
 * In this example the UserMain uses the {@link MainCmdLogging_ifc} to write some informations either to a log file
 * or to console or both. Additionally there is used a redirect of {@link java.lang.System#out} and System.err 
 * to the logging mechanism of MainCmd. See {@link org.vishia.msgDispatch.MsgDispatchSystemOutErr}. 
 * With that capability the user can invoke the standard java mechanism for System.out.println("any message") only
 * and nevertheless the message and log capability of MainCmd is used.. 
 * 
 */
public static class UserMain
{
  
  /*------------------------------------------------------------------------------------------------*/
  /* Start of definitions of the SampleCmdLine class. 
   * The classes above are static, it may be assigned outside also.
   */
  
  /* NOTE: It may be better, to havn't an aggregation of Args, because it is better able to control,
   * which methods use args.
   */
  
  /** Aggregation to the Console implementation class.*/
  final MainCmd_ifc console;


  /**Constructs the Sample class. */
  public UserMain(MainCmd_ifc console)
  {
    this.console = console;
  }
  
  
  
  
  
  /** Executes the task of this class.
   * @param cmdLineArgs The args originally from command line call, 
   *                    but it is possible to patch this args with some other operations.
   *                    It should not be the pure originally.
   */
  void execute(Args cmdLineArgs)
  {
    /** sample to write something as info:
    */
    console.writeInfoln("Info: read it please!");
    console.writeInfo(" ... this is added to the last info.");
    console.writeInfoln("Info: second line!");
    console.writeInfo(" ... this is added to the second line.");
    System.err.println("SampleCmdLine - testmessage; any information");
    System.err.printf("SampleCmdLine - testmessage; some more information %d\n", 624);

    try  //it may be problematic
    { executeFiles(cmdLineArgs); // sample to handle with the files:
    }
    catch(IOException exception)
    {
      //do nothing, ignore the exception.
    }
    /** sample to use executeCmdLine. Note, that a cmdline call "dir c:" isn't possible,
        because "dir" is an internal cmd from the cmd.exe. Use instead the shown notation.
        The output will written to the sDirectoryContent and also to the reportfile with the given level.
        The output will written also on Display by using Report.infoDisplay
    */
    StringBuffer sDirectoryContent = new StringBuffer(2000);
    console.executeCmdLine("cmd /C dir", Report.info, sDirectoryContent, null);
    System.out.println("TEST DIRECT PRINTLN:" + sDirectoryContent.toString());

    /** other sample: the notepad from windows is invoked. The process blocks until the notepad is closed.
        Note, that a parallel invoke of a process is possibel by using several threads in java.
    */
    console.executeCmdLine("notepad.exe " + cmdLineArgs.sFileOut, Report.error, null, null);

    /** use of the report system:
    */
    console.reportln(Report.info, 2, "Output on report");

    /** set an error level: it is possible to set any error level and continue the process.
        The maximum of error level is saved and returned by getExitErrorLevel().
    */
    console.setExitErrorLevel(MainCmd_ifc.exitWithErrors);
  }


  /** Sample to handle with files 
   * @param cmdLineArgs cmdline args, see {@link #execute(org.vishia.mainCmd.SampleCmdLine.Args)}.
   *                    It may be a good idea to provide this args in the thread, instead as class members.
   * @throws IOException
   */
  void executeFiles(Args cmdLineArgs)
  throws IOException
  { BufferedWriter writer = null;  //import java.io.*
    BufferedReader reader = null;  // only the BufferedReader supports readLine.

    try    //Exceptionhandling: what is if the file can't open.
    { reader = new BufferedReader(new FileReader(cmdLineArgs.sFileIn));
    }
    catch(IOException exception)
    { console.writeError("can't open " + cmdLineArgs.sFileIn, exception);
      console.setExitErrorLevel(MainCmd_ifc.exitWithFileProblems);
      if(reader !=null) { reader.close();}
      throw new IOException("executeFiles");  //commonly exception of this routine.
    }

    try{ writer = new BufferedWriter(new FileWriter(cmdLineArgs.sFileOut)); }
    catch(IOException exception)
    { console.writeError("can't create " + cmdLineArgs.sFileOut, exception);
      console.setExitErrorLevel(MainCmd_ifc.exitWithFileProblems);
      if(reader !=null) { reader.close();}
      if(writer !=null) { writer.close(); }
      throw exception;  //it's also able to forward the exception
    }

    try
    { int nLineNr = 1;

      boolean bCont = true;
      while(bCont)
      { String sLine= reader.readLine();
        if(sLine == null) bCont = false;
        else
        { writer.write(nLineNr + ": " + sLine);
          writer.newLine();
          nLineNr +=1;
        }
      }
      reader.close();
      writer.close();
    }
    catch(IOException exception)
    { console.writeError("error reading or writing " + cmdLineArgs.sFileIn + " / "+ cmdLineArgs.sFileOut, exception);
      console.setExitErrorLevel(MainCmd_ifc.exitWithFileProblems);
    }

  }

}  
}//SampleCmdLine


