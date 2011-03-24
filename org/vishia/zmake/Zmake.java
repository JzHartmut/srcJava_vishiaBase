package org.vishia.zmake;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.util.Properties;


import org.vishia.mainCmd.MainCmd;
import org.vishia.mainCmd.MainCmd_ifc;
import org.vishia.mainCmd.Report;
import org.vishia.util.StringPart;
import org.vishia.util.StringPartFromFileLines;
//import org.vishia.util.StringPartFromFile;
import org.vishia.xmlSimple.XmlException;
import org.vishia.zbnf.ZbnfJavaOutput;
import org.vishia.zbnf.ZbnfParseResultItem;
import org.vishia.zbnf.ZbnfParser;
import org.vishia.zbnf.ZbnfXmlOutput;

public class Zmake
{

	/**
	 * 2011-03-07: cmdline arguments removed -zbnf4ant, -tmpAntXml, new -o=OUT -zbnf=
	 * 
	 */
	public final static int version = 0x20110307;
	
	
	private static class CallingArgs
	{
	
	  private String input = null;
	  
	  /**String path to the XML_TOOLBASE Directory. */
	  private String zbnfjax_PATH;
	  
	  /**String path to the current dir from calling. */
	  private String curDir = null;  //default: actual dir
	  
	  /**String path fromcurDir to a tmp dir. */
	  private String tmp = "../tmp";  
	  
	  
	  /**Path of ZBNF script to generate the ant.xml*/
	  private String sZbnf4ant = "xsl/ZmakeStd.zbnf";
	  
	  /**Path of ZBNF script to read the genctrl4ant*/
	  private String sZbnfGenCtrl = "xsl/ZmakeGenctrl.zbnf";
	  
	  /**Path of ZBNF script to generate the ant.xml*/
	  private String sGenCtrl4ant = "xsl/ZmakeStd2Ant.genctrl";
	  
	  /**Path of XSL script to generate the ant.xml*/
	  //private String sXslt4ant = "xsl/ZmakeStd.xslp";
	  
	  /**Path of the input.xml*/
	  private String sInputXml = null;
	  
	  /**Path of the ant.xml*/
	  private String sOutput = null;
	};

	
  /** Aggregation to the Console implementation class.*/
  private MainCmd_ifc console;

  private final ZmakeGenScript antGenCtrl;
  
  /**String path for the absolute tmp dir. */
  private String tmpAbs;  
  

	final CallingArgs args;
  
  /*---------------------------------------------------------------------------------------------*/
  /**Invocation from command line. Up to now the ant file will produced, but ant will not started here. 
   * <br>code in constructor to generate help info:<pre>
     Zmake organizer                                               
     made by JcHartmut, 2007-07-06 - 2007-10-18                    
     invoke>%JAX_EXE% org.vishia.zbnfXml.Zmake [INPUT] [{OPTIONS}]
     * pathes to files or dirs are absolute or relativ from cmd line invocation.               
     * TPATH means a path started from given -ZBNFJAX_HOME:PATH or ZBNFJAX_HOME in environment.
     * WPATH means a path started from given -tmp directory (WorkPATH).
     * INPUTFILE is the only filename without path and without extension dissolved from INPUT                        
     INPUT              The first argument without - is the input file with path and extension.
     -i:INPUT           path to the input file alternatively to INPUT.                         
     -curdir:PATH       sets the current dir alternatively to command line invocation path.     
     -ZBNFJAX_HOME:PATH path to the ZBNFJAX_HOME, default it is getted from environment.       
     -tmp:PATH          path of tmp dir, will be created if not exists, default=\"../tmp\".    
     -tmpinputxml:WPATH name of the temporary file parsed from input, default=INPUTFILE.xml   
     -o=PATH            output-file for generate the target                        
     -zbnf=TPATH        zbnf-file to parse the input                                           
     -genCtrl:TPATH    xslt-file to generate the ant.xml                                      
     </pre>
   *
   */
  public static void main(String [] args)
  { //creates the args-class before creating the main class and fills it:
  	Zmake.CallingArgs cmdArgs = new Zmake.CallingArgs();
  	CmdLine mainCmdLine = new CmdLine(args, cmdArgs); //the instance to parse arguments and others.
    boolean bOk = true;
    try{ mainCmdLine.parseArguments(); }
    catch(Exception exception)
    { mainCmdLine.setExitErrorLevel(MainCmd_ifc.exitWithArgumentError);
      bOk = false;
    }
    Zmake main = new Zmake(cmdArgs, mainCmdLine);     //the main instance
    if(bOk)
    { /** The execution class knows the SampleCmdLine Main class in form of the MainCmd super class
          to hold the contact to the command line execution.
      */
      String sExecuteError = null;
      try{ sExecuteError = main.execute(); }
      catch(Exception exception)
      { //catch the last level of error. No error is reported direct on command line!
        main.console.report("Uncatched Exception on main level:", exception);
        exception.printStackTrace(System.err);
        main.console.setExitErrorLevel(MainCmd_ifc.exitWithErrors);
      }
      if(sExecuteError != null)
      { 
        main.console.reportln(0, sExecuteError);
        main.console.setExitErrorLevel(MainCmd_ifc.exitWithErrors);
      }
    }
    main.console.writeInfoln("* ");
    mainCmdLine.exit();
  }


  public Zmake(CallingArgs args, MainCmd_ifc console)
  { this.args = args;
    this.console = console;
    antGenCtrl = new ZmakeGenScript(console);
  }
  
  
  private static class CmdLine extends MainCmd
  {
  	private final CallingArgs callingArgs;
  	
    /*---------------------------------------------------------------------------------------------*/
    /** Constructor of the CmdLine class.
        The command line arguments are parsed here. After them the execute class is created as composition of SampleCmdLine.
    */
    private CmdLine(String[] args, CallingArgs callingArgs)
    { super(args);
      this.callingArgs = callingArgs;
      //:TODO: user, add your help info!
      //super.addHelpInfo(getAboutInfo());
      super.addAboutInfo("Zmake organizer");
      super.addAboutInfo("made by JcHartmut, 2007-07-06 - 2007-10-18");
      super.addHelpInfo("invoke>%JAX_EXE% org.vishia.zbnfXml.Zmake [INPUT] [{OPTIONS}]");
      super.addHelpInfo("* pathes to files or dirs are absolute or relativ from cmd line invocation.");
      super.addHelpInfo("* TPATH means a path started from given -ZBNFJAX_HOME:PATH or ZBNFJAX_HOME in environment.");
      super.addHelpInfo("* WPATH means a path started from given -tmp directory (WorkPATH).");
      super.addHelpInfo("* INPUTFILE is the only filename without path and without extension dissolved from INPUT");
      super.addHelpInfo("INPUT              The first argument without - is the input file with path and extension.");
      super.addHelpInfo("-i:INPUT           path to the input file alternatively to INPUT.");
      super.addHelpInfo("-curdir:PATH       sets the current dir alternatively to command line invocation path.");
      super.addHelpInfo("-ZBNFJAX_HOME:PATH path to the ZBNFJAX_HOME, default it is getted from environment.");
      super.addHelpInfo("-tmp:PATH          path of tmp dir, will be created if not exists, default=\"../tmp\".");
      super.addHelpInfo("-tmpinputxml:WPATH name of the temporary file parsed from input, default=INPUTFILE.xml");
      super.addHelpInfo("-o=PATH            output-file to generate");
      super.addHelpInfo("-zbnf=TPATH        zbnf-file to parse the input");
      super.addHelpInfo("-genCtrl:TPATH     file which describes the generation for the ant.xml");
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
  
      if(nArg==0 && !arg.startsWith("-"))      { callingArgs.input = getArgument(0); }
      else if(arg.startsWith("-i:"))           { callingArgs.input = getArgument(3); }
      else if(arg.startsWith("-i="))           { callingArgs.input = getArgument(3); }
      else if(arg.startsWith("-i"))            { callingArgs.input = getArgument(2); }
      else if(arg.startsWith("-curdir:"))      { callingArgs.curDir = getArgument(8) + "/"; }
      else if(arg.startsWith("-curdir="))      { callingArgs.curDir = getArgument(8) + "/"; }
      else if(arg.startsWith("-ZBNFJAX_HOME:")){ callingArgs.zbnfjax_PATH = getArgument(14); }
      else if(arg.startsWith("-XML_TOOLBASE:")){ callingArgs.zbnfjax_PATH = getArgument(14); }  //older version, compatibility
      else if(arg.startsWith("-XML_TOOLBASE=")){ callingArgs.zbnfjax_PATH = getArgument(14); }  //older version, compatibility
      else if(arg.startsWith("-tmp:"))         { callingArgs.tmp = getArgument(5); }
      else if(arg.startsWith("-tmp="))         { callingArgs.tmp = getArgument(5); } //older version, compatibility
      else if(arg.startsWith("-tmpinputxml:")) { callingArgs.sInputXml = getArgument(13); }
      else if(arg.startsWith("-o="))           { callingArgs.sOutput = getArgument(3); }
      else if(arg.startsWith("-zbnf="))        { callingArgs.sZbnf4ant = getArgument(6); }  //older version, compatibility
      else if(arg.startsWith("-genCtrl="))     { callingArgs.sGenCtrl4ant = getArgument(9); }
      //else if(arg.startsWith("-xslt4ant="))    { sXslt4ant = getArgument(10); }  //older version, compatibility
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
  
	    if(callingArgs.input == null)              
	    { bOk = false; 
	      writeError("ERROR argument -i=INP is obligate."); 
	    }
	
	    if(callingArgs.sOutput == null)              
	    { bOk = false; 
	      writeError("ERROR argument -o=OUT is obligate."); 
	    }
	
	    if(!bOk) setExitErrorLevel(exitWithArgumentError);
    
      return bOk;
    
    }
  }//class CmdLine

    
  
  /** Execute the task of the class. 
   * @throws ParseException 
   * @throws XmlException 
   * @throws InstantiationException 
   * @throws IllegalAccessException 
   * @throws IllegalArgumentException 
   * @throws IOException
   */
  String execute() throws ParseException, XmlException, IllegalArgumentException, IllegalAccessException, InstantiationException, IOException
  { boolean bOk = true;
    String sError = null;
    //the followed line maybe unnecessary because the java cmd line interpretation always cuts the quotion  marks,
    //Such quotion marks appeares if a double click from commandline is happened. 
    if(args.input.startsWith("\"") && args.input.length()>=2){ args.input = args.input.substring(1, args.input.length()-1); }

    /*Separate input path file ext.*/
    String inputFile, inputExt;
    { int pos1 = args.input.lastIndexOf(('/'));
      int pos2 = args.input.lastIndexOf(('\\'));
      int pos3 = args.input.lastIndexOf((':'));
      if(pos2 > pos1){ pos1 = pos2; }
      if(pos3 > pos1){ pos1 = pos3; }
      if(pos1 < 0){ pos1 = -1; }
      int pos9 = args.input.lastIndexOf('.');
      if(pos9 < pos1) { pos9 = args.input.length(); }
      inputFile = args.input.substring(pos1 + 1, pos9); //, pos9);
      inputExt =  args.input.substring(pos9);
      
      if(args.curDir == null) 
      { args.curDir = args.input.substring(0, pos1 +1);  //"" if no path before filename is given.
      	args.input = inputFile + inputExt;
      } 
      else
      { //input is the full named file, but it is used relative to current dir.
        //curDir is given from command line.
      }
    }
    
    tmpAbs = args.curDir +args.tmp;
    
    if(args.zbnfjax_PATH==null) { args.zbnfjax_PATH = System.getenv("ZBNFJAX_HOME"); }
    if(args.zbnfjax_PATH==null) { args.zbnfjax_PATH = System.getenv("XML_TOOLBASE"); }
    if(args.zbnfjax_PATH == null)
    { throw new ParseException("ZBNFJAX_HOME is not set. Either you should set a environment variable with this name or you should use the -ZBNFJAX_HOME: cmdline-Argument.", 0);
    }
    if(args.zbnfjax_PATH.startsWith("\"") && args.zbnfjax_PATH.length()>=2){ 
    	args.zbnfjax_PATH = args.zbnfjax_PATH.substring(1, args.zbnfjax_PATH.length()-1); 
    }
    args.zbnfjax_PATH += "/";
    
    console.writeInfoln("* Zmake: " + args.input);
    
    File tmpDir = new File(tmpAbs);
    if(!tmpDir.exists()) { tmpDir.mkdir(); }
    
    if(args.sInputXml == null)
    { args.sInputXml = inputFile + inputExt + ".xml"; 
    }
    File fileZbnfXml = new File(tmpAbs + "/" + args.sInputXml);
    fileZbnfXml.setWritable(true); 
    fileZbnfXml.delete();

    File fileOut = new File(args.sOutput);
    fileOut.setWritable(true); 
    fileOut.delete();
    
    File fileZbnf4GenCtrl = new File(args.zbnfjax_PATH + args.sZbnfGenCtrl);
    if(!fileZbnf4GenCtrl.exists()) throw new IllegalArgumentException("cannot find -zbnf4GenCtrl=" + fileZbnf4GenCtrl.getAbsolutePath());
    
    final String sFileGenCtrl = args.sGenCtrl4ant.startsWith(".") ? args.sGenCtrl4ant
    	                        : (args.zbnfjax_PATH + args.sGenCtrl4ant);
    File fileGenCtrl = new File(sFileGenCtrl);
    if(!fileGenCtrl.exists()) throw new IllegalArgumentException("cannot find -genCtrl=" + fileGenCtrl.getAbsolutePath());
    
    //Build the data for ANT-generation control:
    antGenCtrl.parseAntGenCtrl(fileZbnf4GenCtrl, fileGenCtrl);
    
    console.writeInfoln("* Zmake: parsing user.zmake \"" + args.curDir + args.input + "\" with \"" 
    	+ args.zbnfjax_PATH + args.sZbnf4ant + "\" to \""  + fileZbnfXml.getAbsolutePath() + "\"");
    //call the parser from input, it produces a temporary xml file.
    String sInputAbs = args.curDir + args.input;
    String sZbnf = args.zbnfjax_PATH + args.sZbnf4ant;
    
    String sInputAbs_xml = tmpAbs + "/" + args.sInputXml;
    StringPart spInput = new StringPartFromFileLines(new File(sInputAbs));
    ZbnfParser parser = new ZbnfParser(console);
    parser.setSyntax(new File(sZbnf));
    console.writeInfo(" ... ");
    bOk = parser.parse(spInput);
    if(!bOk){
    	sError = parser.getSyntaxErrorReport();
    	throw new ParseException(sError,0);
    }
    spInput.close();
    if(console.getReportLevel() >= Report.fineInfo){
    	parser.reportStore(console, Report.fineInfo, "User-ZmakeScript");
    }
    console.writeInfo(" ok, set result ... ");
    ZbnfParseResultItem parseResult = parser.getFirstParseResult();
    //write XML output only to check:
    if(sInputAbs_xml !=null){
	    ZbnfXmlOutput xmlOutput = new ZbnfXmlOutput();
	    xmlOutput.write(parser, sInputAbs_xml);
    }
    //write into Java classes:
    ZmakeUserScript.UserScript zmakeInput = new ZmakeUserScript.UserScript();
    ZbnfJavaOutput parser2Java = new ZbnfJavaOutput(console);
    parser2Java.setContent(zmakeInput.getClass(), zmakeInput, parseResult);
    //evaluate
    console.writeInfoln("* generate script \"" + fileOut.getAbsolutePath() + "\"\n");
    ZmakeGenerator mng = new ZmakeGenerator(fileOut, zmakeInput, antGenCtrl, console);
    mng.gen_ZmakeOutput();
    console.writeInfoln("* done");
    
        
    return sError;
  }
  
  

}
