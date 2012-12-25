package org.vishia.zbnf.example.billOfMaterial;

import java.io.File;
import java.text.ParseException;

import org.vishia.mainCmd.MainCmd;
import org.vishia.mainCmd.MainCmd_ifc;
import org.vishia.zbnf.ZbnfJavaOutput;


public class Main
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
  
    /**Cmd-line-argument, set on -i: option. Inputfile. */
    String sFileIn = null;
  
  }



  /*---------------------------------------------------------------------------------------------*/
  /** main started from java*/
  public static void main(String [] args)
  { Main.Args cmdlineArgs = new Main.Args();     //holds the command line arguments.
    CmdLine mainCmdLine = new CmdLine(args, cmdlineArgs); //the instance to parse arguments and others.
    boolean bOk = true;
    try{ mainCmdLine.parseArguments(); }
    catch(Exception exception)
    { mainCmdLine.setExitErrorLevel(MainCmd_ifc.exitWithArgumentError);
      bOk = false;
    }
    if(bOk)
    { /**Now instantiate the main class. 
       * It is possible to create some aggregates (final references) first outside depends on args.
       * Therefore the main class is created yet here.
       */
      Main main = new Main(mainCmdLine, cmdlineArgs);
      /** The execution class knows the Main class in form of the MainCmd super class
          to hold the contact to the command line execution.
      */
      try
      { main.executeStoreViaMethods(cmdlineArgs); 
        main.executeStoreFields(cmdlineArgs); 
      }
      catch(Exception exception)
      { //catch the last level of error. No error is reported direct on command line!
        main.console.report("Uncatched Exception on main level:", exception);
        main.console.setExitErrorLevel(MainCmd_ifc.exitWithErrors);
      }
    }
    mainCmdLine.exit();
  }

  
  
  /**The inner class CmdLine helps to evaluate the command line arguments
   * and show help messages on command line.
   * This class also implements the {@link MainCmd_ifc}, for some cmd-line-respective things
   * like error level, output of texts, exit handling.  
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
  
      if(cmdlineArgs.sFileIn == null)            { bOk = false; writeError("ERROR argument -i is obligat."); }
      else if(cmdlineArgs.sFileIn.length()==0)   { bOk = false; writeError("ERROR argument -i without content.");}
  
      if(!bOk) setExitErrorLevel(exitWithArgumentError);
    
      return bOk;
    
    }
  }//class CmdLine
  
  
  
  Main(MainCmd_ifc console, Args args)
  {
    this.console = console;
  }
  
  
  /**This method reads the bill of material given in textual form with ZBNF-parsing, 
   * stores all positions in the Java-class {@link BillOfMaterialData_Fields} 
   * and outputs it than in another format. It is an example of parsing and processing.
   * @param args The class Args holds input arguments.
   */
  void executeStoreFields(Args args)
  {
    /**The instance to store the data of parsing result is created locally and private visible: */
    BillOfMaterialZbnf_Fields bill = new BillOfMaterialZbnf_Fields();
    /**This call processes the whole parsing and storing action in the instance 'bill', using the type of bill.
     * It is the simple form.
     * In this basic example the data won't be used never, therefore please set a breakpoint here
     * and check the data manually.
     */ 
    String sError = ZbnfJavaOutput.parseFileAndFillJavaObject(bill.getClass(), bill, new File(args.sFileIn), new File("./billOfMaterial.zbnf"), console, 1200);
    if(sError != null)
    { /**there is any problem while parsing, report it: */
      console.writeError("ERROR Parsing bill of material, file: " + args.sFileIn + "\n" + sError);
    }
    else
    {
      /**now output the result... at first and simple it is able to debug and visit in an ECLIPSE environment. 
       * Do nothing yet else output toString(): .
       */
      System.out.println(bill.toString());
    }  
    
  }
  
  /**This method reads the bill of material given in textual form with ZBNF-parsing, 
   * stores all positions in the Java-class {@link BillOfMaterialData_Fields} 
   * and outputs it than in another format. It is an example of parsing and processing.
   * @param args The class Args holds input arguments.
   */
  void executeStoreViaMethods(Args args)
  {
    /**The instance to store the data of parsing result is created locally and private visible,
     * but it is referenced with an interface. It is possible that the instance is created somewhere other. */
    BillOfMaterial_Zbnf_ifc.ZbnfStore_BillOfMaterial bill = new BillofMaterialData_Methods();
    /**This call processes the whole parsing and storing action in the instance 'bill', using the type of bill.
     * It is the qualification form. The instance is referenced via an interface. The construction of the instance' data is not depending on the syntax form.
     * Some more checks and data processing may be done in the implementation of the interface' methods.
     * In this basic example the data won't be used never, therefore please set a breakpoint here
     * and check the data manually.
     */ 
    String sError = ZbnfJavaOutput.parseFileAndFillJavaObject(BillOfMaterial_Zbnf_ifc.ZbnfStore_BillOfMaterial.class, bill, new File(args.sFileIn), new File("./billOfMaterial.zbnf"), console, 1200);
    if(sError != null)
    { /**there is any problem while parsing, report it: */
      console.writeError("ERROR Parsing bill of material, file: " + args.sFileIn + "\n" + sError);
    }
    else
    {
      /**now output the result... at first and simple it is able to debug and visit in an ECLIPSE environment. 
       * Do nothing yet else output toString(): .
       */
      System.out.println(bill.toString());
    }  
    
  }
  
}