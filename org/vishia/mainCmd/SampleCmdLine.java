package org.vishia.mainCmd;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;



/**Sample for a main-class for a application using {@link MainCmd}-super-class.....<br/>
   This is a sample and an example. The user should do the followed to use the class as a template:
   <ul><li>copy this file under the requested name and package</li>
       <li>correct the package line</li>
       <li>replace 'CmdLineSample' with the user's main class name</li>
       <li>look on all :TODO: -location and change/complete the source there</li>
   </ul>
   The top level class may be also extends the Eclipse-ANT frame, it is implemented
   in this sample some times later (TODO). To support main cmd line invoke,
   the static main routine is available here, but the detail features of evaluating
   the arguments of command line is delegated in the inner class CmdLine.<br>
   The pattern is the following:
   <ul>
   <li>The top level class organizes the processing of the users concept.</li>
   <li>The inner class CmdLine organizes the command line environment.<li>
   <li>The top level class knows the main arguments (also may be given in a Eclipse-ANT environment).</li>
   <li>The inner class CmdLine knows the outer class (not static) to set the main arguments
       given at command line.</li>
   <li>The inner class CmdLine is only instantiated if static main() is invoked,
       but not if an Eclipse-ANT-call is invoked.</li>
   <ul>    
   <li>look on all :TODO: -location and change/complete the source there</li>
   </ul>
   The top level class may be also extends the Eclipse-ANT frame, it is implemented
   in this sample some times later (TODO). To support main cmd line invoke,
   the static main routine is available here, but the detail features of evaluating
   the arguments of command line is delegated in the inner class CmdLine.<br>
   The pattern is the following:
   <ul><li>The top level class organizes the processing of the users concept.</li>
   <li>The inner class CmdLine organizes the commmand line environent.<li>
   <li>The top level classe knows the main arguments (also may be given in a Eclipse-ANT environment).</li>
   <li>The inner class CmdLine knows the outer class (not static) to set the main arguments
       given at command line.</li>
   <li>The inner class CmdLine is only instanciated if static main() is invoked,
       but not if an Eclipse-ANT-call is invoked.</li>
   <ul>    
            
   @author Hartmut Schorrig, last change 2009-03-19

 * last changes / concept ideas:         
 * 2009-03-21: Hartmut The parsed arguments are written in an extra static class Args, it is better
 *                     and it is more structured. The application can read arguments via knowledge only of this class,
 *                     without dependencies of the other code. The arguments may be supplied or corrected
 *                     also with other algorithm.
 * 2009-03-21: Hartmut The class CmdLine based on MainCmd is static. This class is only a definition wrapper.
 *                     The application can/should use this class non-depended from arguments etc. via MainCmd_ifc.
 * 2009-03-21: Hartmut The main-method instantiates at first only the classes 
 *                     {@link SampleCmdLine.CmdLine} and the {@link SampleCmdLine.Args} but not the main class itself,
 *                     because the Main-class of the application may got some other aggregates, 
 *                     which may created first and outside (dependency injection, inside using final).
 *                     It is also possible to create more as one classes in {@link SampleCmdLine#reflexAccessSfc}, 
 *                     especially {@link SampleCmdLine} may only be an empty wrapper 
 *                     for organization of main, Args and MainCmd, without deeper dependencies to the core application.                                             
 */

public class SampleCmdLine
{

  /** Aggregation to the Console implementation class.*/
  final MainCmd_ifc console;


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



  /*---------------------------------------------------------------------------------------------*/
  /** main started from java*/
  public static void main(String [] args)
  { SampleCmdLine.Args cmdlineArgs = new SampleCmdLine.Args();     //holds the command line arguments.
    CmdLine mainCmdLine = new CmdLine(args, cmdlineArgs); //the instance to parse arguments and others.
    boolean bOk = true;
    try{ mainCmdLine.parseArguments(); }
    catch(Exception exception)
    { mainCmdLine.setExitErrorLevel(MainCmd_ifc.exitWithArgumentError);
      bOk = false;
    }
    if(bOk)
    { /**Now instantiate the main class. 
       * It is possible to create some aggregates (final references) first outside, depends on args.
       * Therefore the main class is created yet only here.
       */
      SampleCmdLine main = new SampleCmdLine((MainCmd_ifc)mainCmdLine);     //the main instance
      /** The execution class knows the SampleCmdLine Main class in form of the MainCmd super class
          to hold the contact to the command line execution.
      */
      try{ main.execute(cmdlineArgs); }
      catch(Exception exception)
      { //catch the last level of error. No error is reported direct on command line!
        mainCmdLine.report("Uncatched Exception on main level:", exception);
        mainCmdLine.setExitErrorLevel(MainCmd_ifc.exitWithErrors);
      }
    }
    mainCmdLine.exit();
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
    protected boolean testArgument(String arg, int nArg)
    { boolean bOk = true;  //set to false if the argc is not passed
  
      if(arg.startsWith("-i:"))      cmdlineArgs.sFileIn   = getArgument(3);
      else if(arg.startsWith("-o:")) cmdlineArgs.sFileOut  = getArgument(3);
      else bOk=false;
  
      return bOk;
    }
  
    /** Invoked from parseArguments if no argument is given. In the default implementation a help info is written
     * and the application is terminated. The user should overwrite this method if the call without comand line arguments
     * is meaningfull.
     * @throws ParseException 
     *
     */
    protected void callWithoutArguments() throws ParseException
    { //:TODO: overwrite with empty method - if the calling without arguments
      //having equal rights than the calling with arguments - no special action.
      super.callWithoutArguments();  //it needn't be overwritten if it is unnecessary
    }
  
  
  
  
    /*---------------------------------------------------------------------------------------------*/
    /**Checks the cmdline arguments relation together.
       If there is an inconsistents, a message should be written. It may be also a warning.
       :TODO: the user only should determine the specific checks, this is a sample.
       @return true if successfull, false if failed.
    */
    protected boolean checkArguments()
    { boolean bOk = true;
  
      if(cmdlineArgs.sFileIn == null)            { bOk = false; writeError("ERROR argument -i: is obligat."); }
      else if(cmdlineArgs.sFileIn.length()==0)   { bOk = false; writeError("ERROR argument -i: without content.");}
  
      if(cmdlineArgs.sFileOut == null)           { writeWarning("argument -o: no outputfile is given, use default"); cmdlineArgs.sFileOut = "out.txt";}
      else if(cmdlineArgs.sFileOut.length()==0)  { bOk = false; writeError("argument -o: without content"); }
  
      if(!bOk) setExitErrorLevel(exitWithArgumentError);
    
      return bOk;
    
    }
  }//class CmdLine
  

  /*------------------------------------------------------------------------------------------------*/
  /* Start of definitions of the SampleCmdLine class. 
   * The classes above are static, it may be assigned outside also.
   */
  
  /* NOTE: It may be better, to havn't an aggregation of Args, because it is better able to control,
   * which methods use args.
   */
  
  /**Constructs the Sample class. */
  public SampleCmdLine(MainCmd_ifc console)
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


  /** Sample to handle with files */
  /**
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
      throw new IOException("executeFiles");  //commonly exception of this routine.
    }

    try{ writer = new BufferedWriter(new FileWriter(cmdLineArgs.sFileOut)); }
    catch(IOException exception)
    { console.writeError("can't create " + cmdLineArgs.sFileOut, exception);
      console.setExitErrorLevel(MainCmd_ifc.exitWithFileProblems);
      throw exception;  //it's also able to forward the exception
    }

    try{ reader = new BufferedReader(new FileReader(cmdLineArgs.sFileIn)); }
    catch(IOException exception)
    { console.writeError("can't open " + cmdLineArgs.sFileIn, exception);
      console.setExitErrorLevel(MainCmd_ifc.exitWithFileProblems);
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


