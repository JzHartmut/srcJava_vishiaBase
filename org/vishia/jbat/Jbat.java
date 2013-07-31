package org.vishia.jbat;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.vishia.mainCmd.MainCmd;
import org.vishia.mainCmd.MainCmdLogging_ifc;
import org.vishia.mainCmd.MainCmd_ifc;

public class Jbat
{
  
  protected static class Args{
    
    /**Cmdline-argument, set on -i option. Inputfile to to something. :TODO: its a example.*/
    String sFileIn;
    
    String sFileOut;
    
    File fileTestOut;
  }
  

  protected final MainCmdLogging_ifc log;
  
  protected final Args args;
  
  protected final JbatExecuter executer;
  
  
  public Jbat(Args args, MainCmdLogging_ifc log){
    this.log = log;
    this.args = args;
    this.executer = new JbatExecuter(log);
  }
  
  
  /** main started from java*/
  public static void main(String [] sArgs)
  { 
    smain(sArgs, true);
  }


  /**Invocation from another java program without exit the JVM
   * @param sArgs same like {@link #main(String[])}
   * @return "" or an error String
   */
  public static String smain(String[] sArgs){ return smain(sArgs, false); }

  
  private static String smain(String[] sArgs, boolean shouldExitVM){
    String sRet = null;
    Args args = new Args();
    CmdLine mainCmdLine = new CmdLine(args, sArgs); //the instance to parse arguments and others.
    try{
      try{ mainCmdLine.parseArguments(); }
      catch(Exception exception)
      { sRet = "Jbat - Argument error ;" + exception.getMessage();
        mainCmdLine.report(sRet, exception);
        mainCmdLine.setExitErrorLevel(MainCmd_ifc.exitWithArgumentError);
      }
      if(args.sFileIn !=null){
        Jbat main = new Jbat(args, mainCmdLine);     //the main instance
        if(sRet == null)
        { /** The execution class knows the SampleCmdLine Main class in form of the MainCmd super class
              to hold the contact to the command line execution.
          */
          try{ 
            sRet = main.execute();
            if(sRet !=null){
              mainCmdLine.writeError(sRet);
            }
          }
          catch(Exception exception)
          { //catch the last level of error. No error is reported direct on command line!
            sRet = "Jbat - Any internal error;" + exception.getMessage();
            mainCmdLine.report(sRet, exception);
            exception.printStackTrace(System.out);
            mainCmdLine.setExitErrorLevel(MainCmd_ifc.exitWithErrors);
          }
        }
      } else {
        mainCmdLine.writeHelpInfo();
      }
    } catch(Exception exc){
      sRet = exc.getMessage();
    }
    
    if(shouldExitVM) { mainCmdLine.exit(); }
    return sRet;
  }


  
  public String execute(){
    String sError = null;
    Appendable out = null;
    if(args.sFileOut !=null){
      try{
        File fileOut = new File(args.sFileOut);
        out = new FileWriter(fileOut);
      } catch(IOException exc){
        sError = "Jbat - cannot create output text file;"; 
      }
    } else {
      out = System.out;
    }
    File fileIn = new File(args.sFileIn);
    sError = executer.generate(null, fileIn, out, true, args.fileTestOut);
    return sError;
  }
  
  
  
  protected static class CmdLine extends MainCmd
  {

    public final Args argData;

    protected final MainCmd.Argument[] argList =
    { new MainCmd.Argument("", "<INPUT>    jbat-File to execute"
        , new MainCmd.SetArgument(){ @Override public boolean setArgument(String val){ 
          argData.sFileIn = val; return true;
        }})
    , new MainCmd.Argument("-t", ":<OUTEXT> text-File for output"
        , new MainCmd.SetArgument(){ @Override public boolean setArgument(String val){ 
          argData.sFileOut = val; return true;
        }})
    , new MainCmd.Argument("-debug", ":<TEST.xml> Test-File"
        , new MainCmd.SetArgument(){ @Override public boolean setArgument(String val){ 
          argData.fileTestOut = new File(val); return true;
        }})
    };


    protected CmdLine(Args argData, String[] sCmdlineArgs){
      super(sCmdlineArgs);
      this.argData = argData;
      super.addAboutInfo("Execution and Generation of jbat-Files");
      super.addAboutInfo("made by HSchorrig, Version 1.0, 2013-07-11..2013-07-11");
      super.addArgument(argList);
      super.addStandardHelpInfo();
      
    }
    
    @Override protected void callWithoutArguments()
    { //overwrite with empty method - it is admissible.
    }

    
    @Override protected boolean checkArguments()
    {
      return true;
    } 
    
    @Override public void writeHelpInfo(){
      super.writeHelpInfo();
      System.out.println("=== Syntax of a jbat script===");
      System.out.println(JbatSyntax.syntax);
    }

  }
  
  
}
