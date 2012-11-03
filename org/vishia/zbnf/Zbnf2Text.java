package org.vishia.zbnf;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.vishia.mainCmd.MainCmd_ifc;
import org.vishia.textGenerator.TextGenerator;
import org.vishia.xmlSimple.XmlNodeSimple;

public class Zbnf2Text extends Zbnf2Xml
{

  /** Aggregation to the Console implementation class.*/
  final MainCmd_ifc console;


  //final TreeNodeUniqueKey<ZbnfParseResultItem> data = 
    //new TreeNodeUniqueKey<ZbnfParseResultItem>("root", null);
  
  
  
  /*---------------------------------------------------------------------------------------------*/
  /** main started from java*/
  public static void main(String [] sArgs)
  { Args arg = new Args();
    CmdLineText mainCmdLine = new CmdLineText(arg, sArgs); //the instance to parse arguments and others.
    Zbnf2Text main = new Zbnf2Text(arg, mainCmdLine);     //the main instance
    boolean bOk = true;
    try{ mainCmdLine.parseArguments(); }
    catch(Exception exception)
    { main.report.report("Argument error:", exception);
      main.report.setExitErrorLevel(MainCmd_ifc.exitWithArgumentError);
      bOk = false;
    }
    if(bOk)
    { /** The execution class knows the SampleCmdLine Main class in form of the MainCmd super class
          to hold the contact to the command line execution.
      */
      try{ main.execute(arg); }
      catch(Exception exception)
      { //catch the last level of error. No error is reported direct on command line!
        main.report.report("Uncatched Exception on main level:", exception);
        exception.printStackTrace(System.out);
        main.report.setExitErrorLevel(MainCmd_ifc.exitWithErrors);
      }
    }
    mainCmdLine.exit();
  }

  
  Zbnf2Text(Args args, MainCmd_ifc mainCmdLine){
    super(args, mainCmdLine);
    this.console = mainCmdLine;  //it may be also another instance based on MainCmd_ifc
    
  }
  
  
  
  
  public boolean execute(Args args) throws IOException
  { boolean bOk = true;
    bOk = super.execute();
    
    XmlNodeSimple<ZbnfParseResultItem> resultTree = parser.getResultTree(); 
    
    
    if(bOk){
      FileWriter outData;
      if(args.sScriptCheck !=null){
        outData = new FileWriter(args.sScriptCheck);
      } else {
        outData = null;
      }
      for(Zbnf2Text.Out out: args.listOut){
        File fOut = new File(out.sFileOut);
        File fileScript = new File(out.sFileScript);
        TextGenerator generator = new TextGenerator(console);
        if(outData !=null) {
          outData.append("===================").append(out.sFileScript);
        }
        String sError = generator.generate(resultTree, fileScript, fOut, true, outData);
        if(sError !=null){
          console.writeError(sError);
        } else {
          console.writeInfoln("SUCCESS outfile: " + fOut.getAbsolutePath());
        }
      }
      if(outData !=null) {
        outData.close();
      }
    }
    return bOk;
  }
  
  
  
  /**This class holds the got arguments from command line. 
   * It is possible that some more operations with arguments as a simple get process.
   * The arguments may got also from configuration files named in cmd line arguments or other.
   * The user of arguments should know (reference) only this instance, not all other things arround MainCmd,
   * it it access to arguments. 
   */
  public static class Args extends Zbnf2Xml.Args
  {
    public String sScriptCheck = null;
    
    public List<Out> listOut = new LinkedList<Out>();
    
    public Out lastOut;
  }



  
  
  public static class Out{
    
    /**Cmdline-argument, set on -y: option. Outputfile to output something. */
    public String sFileOut = null;

    public String sFileScript = null;
  }



  /**The inner class CmdLineText helps to evaluate the command line arguments
   * and show help messages on command line.
   */
  static class CmdLineText extends Zbnf2Xml.CmdLine
  {

    //Args cmdlineArgs;  
    
    
    protected CmdLineText(Args arg, String[] args) {
      super(arg, args);
      //cmdlineArgs = new Args();
      super.addHelpInfo("-c:<OUTCTRL> name of a file to control the output");
      super.addHelpInfo("-t:<TEXTOUT> name of the output file to generate");
      super.addHelpInfo("-c:<TEXTOUT> -t:<TEXTOUT> more as one output file with this pair of arguments");
    } 
    
    
    public boolean testArgument(String argc, int nArg)
    { boolean bOk = super.testArgument(argc, nArg);  //set to false if the argc is not passed
      if(!bOk){ 
        bOk = testArgsZbnf2Text(argc, nArg);
      }
      return bOk;
    }
    
    
    public boolean testArgsZbnf2Text(String argc, int nArg){
      boolean bOk = true;
      if(argc.startsWith("-t:")) {
        Args cmdlineArgs = (Args)super.args;
        if(cmdlineArgs.lastOut == null){ cmdlineArgs.lastOut = new Out(); cmdlineArgs.listOut.add(cmdlineArgs.lastOut); }
        cmdlineArgs.lastOut.sFileOut  = getArgument(3);
        if(cmdlineArgs.lastOut.sFileScript !=null){ cmdlineArgs.lastOut = null; } //filled. 
      }
      else if(argc.startsWith("-c:")) {
        Args cmdlineArgs = (Args)super.args;
        if(cmdlineArgs.lastOut == null){ cmdlineArgs.lastOut = new Out(); cmdlineArgs.listOut.add(cmdlineArgs.lastOut); }
        cmdlineArgs.lastOut.sFileScript  = getArgument(3);
        if(cmdlineArgs.lastOut.sFileOut !=null){ cmdlineArgs.lastOut = null; } //filled. 
      }
      else bOk = false;
      return bOk;
    }
  }


}
