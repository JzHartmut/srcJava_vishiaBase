package org.vishia.zbnf.example.vhdlScript;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.ParseException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.vishia.mainCmd.MainCmd;
import org.vishia.mainCmd.MainCmd_ifc;
import org.vishia.zbnf.ZbnfJavaOutput;


public class VhdlScript2Header
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
    /**Cmdline-argument, set on -i: option. Inputfile to to something. */
    String sFileIn = null;
  
    /**Cmdline-argument, set on -y: option. Outputfile to output something. */
    String sFileOut = null;

    /**Cmdline-argument, set on -s: option. Zbnf-file to output something. */
    String sFileZbnf = null;
  }



  /*---------------------------------------------------------------------------------------------*/
  /** main started from java*/
  public static void main(String [] args)
  { VhdlScript2Header.Args cmdlineArgs = new VhdlScript2Header.Args();     //holds the command line arguments.
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
      VhdlScript2Header main = new VhdlScript2Header(mainCmdLine, cmdlineArgs);
      /** The execution class knows the Main class in form of the MainCmd super class
          to hold the contact to the command line execution.
      */
      try
      { main.execute(cmdlineArgs); 
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
      super.addAboutInfo("Conversion Vhdl-Address-script to header");
      super.addAboutInfo("made by Hartmut Schorrig, 2009-12-01");
      super.addHelpInfo("Conversion Vhdl-Address-script to header V 2009-12-01");
      super.addHelpInfo("param: -i:INPUT -s:ZBNF -o:OUTPUT");
      super.addHelpInfo("-i:INPUT    inputfilepath, this file is testing.");
      super.addHelpInfo("-s:ZBNF     syntaxfilepath, this file is written.");
      super.addHelpInfo("-y:OUTPUT   outputfilepath, this file is written.");
      super.addStandardHelpInfo();
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
      else if(arg.startsWith("-y:")) cmdlineArgs.sFileOut  = getArgument(3);
      else if(arg.startsWith("-s:")) cmdlineArgs.sFileZbnf  = getArgument(3);
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
  
  
  /**This class gets and stores the results from a parsed ZBNF-component <code>constant</code>.
   * 
   */
  public static class VhdlConstant
  {
    /**From ZBNF: <$?@name>. */
    public String name;
    
    /**From ZBNF: <""?@addr>. */
    public String addr;
    
    /**From ZBNF: <""?@comment>. */
    public String comment= "";  //it is optional.
    
  }
  
  
  public static class ZbnfResultData
  {
    private List<VhdlConstant> constants = new LinkedList<VhdlConstant>();
    
    
    public VhdlConstant new_constant()
    { return new VhdlConstant();
    }
    
    public void add_constant(VhdlConstant value)
    { 
      constants.add(value);
    }
    
  }
  
  
  
  VhdlScript2Header(MainCmd_ifc console, Args args)
  {
    this.console = console;
  }
  
  
  void execute(Args args) throws IOException
  {
    boolean bOk;
    ZbnfResultData zbnfResultData = parseAndStoreInput(args);
    if(zbnfResultData != null){
      
      File fHeader = new File(args.sFileOut);
      Writer output1 = new FileWriter(fHeader);
      BufferedWriter output = new BufferedWriter(output1);
      
      GenerateHeader generateHeader = new GenerateHeader(fHeader.getName(), zbnfResultData, output);
      generateHeader.generateHeader();
      console.writeInfoln("SUCCESS headefile: " + fHeader.getAbsolutePath());
      output.close();
    }
  }
  
  
  
  
  /**This method reads the input VHDL-script, parses it with ZBNF, 
   * stores all results in the Java-class {@link ZbnfResultData} 
   */
  private ZbnfResultData parseAndStoreInput(Args args)
  {
    /**The instance to store the data of parsing result is created locally and private visible: */
    ZbnfResultData zbnfResultData = new ZbnfResultData();
    /**This call processes the whole parsing and storing action: */
    File fileIn = new File(args.sFileIn);
    File fileSyntax = new File(args.sFileZbnf);
    String sError = ZbnfJavaOutput.parseFileAndFillJavaObject(zbnfResultData.getClass(), zbnfResultData, fileIn, fileSyntax, console, 1200);
    if(sError != null)
    { /**there is any problem while parsing, report it: */
      console.writeError("ERROR Parsing file: " + fileIn.getAbsolutePath() + "\n" + sError);
      return null;
    }
    else
    { console.writeInfoln("SUCCESS parsed: " + fileIn.getAbsolutePath());
      return zbnfResultData;
    }
  
  }
  
  /**This is a inner class to encapsulate the conversion of the parsed VHDL-Data to the header file.
   */
  private class GenerateHeader
  {
  
    final static String sNewline = "\r\n";
    
    /**50 spaces to generate fix tab position. */
    final static String spaces = "                                                  ";
    
    final String sNameHeader; 
    
    final ZbnfResultData zbnfResultData; 
    
    final Writer output;
    
    Iterator<VhdlConstant> iterVhdlConstant;
    
    int lastPortOffset = -20000;
      
    VhdlConstant vhdlConstantCurrent;
    
    VhdlConstant vhdlConstantNext;

    StringBuilder sbDefines = new StringBuilder(10000);
    
    StringBuilder sbWholeStruct = new StringBuilder(10000);
    
    StringBuilder sbWholeStructInit = new StringBuilder(10000);
    
    /**
     * @param nameHeader
     * @param zbnfResultData
     * @param output
     */
    public GenerateHeader(String nameHeader, ZbnfResultData zbnfResultData, Writer output)
    {
      sNameHeader = nameHeader;
      this.zbnfResultData = zbnfResultData;
      this.output = output;
    }
    
    /**Generates the header from parsed results. 
     * @reads this.{@link #zbnfResultData}
     * @uses this
     * @writes this.output.*
     * @calls {@link #generateStructure()}
     * @return true if success
     * @throws IOException
     */
    private boolean generateHeader() throws IOException
    {
      output.write("/*Generated header by VhdlScript2Header, made by Hartmut Schorrig, Version 2009-11-26, source org.vishia.zbnf.example.vhdlScript.java */");
      output.write(sNewline + "#ifndef __" + sNameHeader + "_h__");
      output.write(sNewline + "#define __" + sNameHeader + "_h__");
      
      sbWholeStruct.append(sNewline + "typedef struct FPGA_References_t {");
      
      iterVhdlConstant = zbnfResultData.constants.iterator();
      if(iterVhdlConstant.hasNext()){
        vhdlConstantCurrent = iterVhdlConstant.next();
        boolean bCont;
        do{
          bCont = generateStructure();
        }
        while(bCont);
      }
      
      sbWholeStruct.append(sNewline + "} FPGA_References; "+ sNewline);
      
      output.write(sbWholeStruct.toString());
      
      output.write(sNewline + sNewline + sNewline);
      output.write(sbWholeStructInit.toString());
      output.write(sNewline + sNewline + sNewline);
      
      output.write(sNewline + sNewline + "#endif __" + sNameHeader + "_h__"+ sNewline);
      return true;
    }
    
    
    boolean getNextVhdlContant()
    {
      boolean hasNextData;
      
      hasNextData = iterVhdlConstant.hasNext();
      if(hasNextData){
        vhdlConstantNext = iterVhdlConstant.next();
      } else {
        vhdlConstantNext = null;
      }
      return hasNextData;
    }
    
    
    
    boolean generateStructure() throws IOException
    {
      boolean hasNextData = getNextVhdlContant();
        
      String sNameStruct = "FPGA_" + vhdlConstantCurrent.name;
      
      if(vhdlConstantNext != null && vhdlConstantNext.addr.equals(vhdlConstantCurrent.addr)){
        /**The vhdlConstantCurrent is the title of some ports. */
        vhdlConstantCurrent = vhdlConstantNext;  //start with next.
        hasNextData = getNextVhdlContant();     
      }
      
      lastPortOffset = Integer.parseInt(vhdlConstantCurrent.addr, 16);
          
      output.write(sNewline + sNewline + sNewline + "typedef struct " + sNameStruct + "_t {");
      sbWholeStruct.append(sNewline + "  " + sNameStruct + "* " + sNameStruct+ ";" );
      sbWholeStructInit.append(sNewline + "  fPGA_References." + sNameStruct 
           + " = (" + sNameStruct + "*)(0x" + vhdlConstantCurrent.addr + " + FPGA_BASEMEMADDR);"
           );
      boolean bCont;
      do{
        int additionalSpaces = 50 - vhdlConstantCurrent.name.length();
        if(additionalSpaces < 1){ additionalSpaces = 1; }
        output.write(sNewline + "  int32 " + vhdlConstantCurrent.name + ";" 
                    + spaces.substring(0, additionalSpaces) 
                    + " //" + vhdlConstantCurrent.addr 
                    + " " + vhdlConstantCurrent.comment
                    );
        if(vhdlConstantNext != null){
          vhdlConstantCurrent = vhdlConstantNext;
          int portOffset = Integer.parseInt(vhdlConstantCurrent.addr, 16);
          int portOffsDiff = portOffset - lastPortOffset;
          bCont = portOffsDiff < 0x400;
          if(bCont){
            if(portOffsDiff > 4){
              int nrofDummies = (portOffsDiff -4) /4; //sizeof(int32)
              output.write(sNewline + "  int32 dummy" + vhdlConstantCurrent.addr + "["+ nrofDummies + "];");
            } else {
              assert(portOffsDiff == 4);
            }
            hasNextData = getNextVhdlContant();     
          }  
          lastPortOffset = portOffset;
        }
        else{ 
          bCont = false; 
        }
      }while(bCont);
      output.write(sNewline + "} " + sNameStruct + "; \n\n");
      return hasNextData;
    }




  }//class GenerateHeader
  
}