package org.vishia.zmake;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.ParseException;
import java.util.Map;


import org.vishia.mainCmd.MainCmd;
import org.vishia.mainCmd.MainCmd_ifc;
import org.vishia.mainCmd.Report;
import org.vishia.textGenerator.OutputDataTree;
import org.vishia.textGenerator.TextGenScript;
import org.vishia.textGenerator.TextGenerator;
import org.vishia.util.DataAccess;
import org.vishia.util.StringPart;
import org.vishia.util.StringPartFromFileLines;
//import org.vishia.util.StringPartFromFile;
import org.vishia.xmlSimple.SimpleXmlOutputter;
import org.vishia.xmlSimple.XmlException;
import org.vishia.xmlSimple.XmlNodeSimple;
import org.vishia.zbnf.ZbnfJavaOutput;
import org.vishia.zbnf.ZbnfParseResultItem;
import org.vishia.zbnf.ZbnfParser;



/**The Zmake translator translates between a Zmake user script and the ready-to-run make-script. 
 * The zmake user script describes what's how to make in a more simple syntax than most of the other known 
 * make scripts. The outputted make-script may be an ANT.xml-make file, an old-style make file, 
 * a simple batch or script file or any other control file for a make process. 
 * The translator is able for universal usage, it  controls which text is produced for the output.
 * <pre>
  Zmake
  user    =====Z===translator=====> make script
  script       ^        ^           ready to run
               |        |
               |        |  (user level)
          - - -|- - - - |- - - - - - -
               |        |  (admin level)
          zmake.zbnf    |
                        |
   ZmakeGenctrl.zbnf--->Z
                        |
               translation script

 * </pre>
 * The Zmake translator works in two stages. Adequate, two scripts control the working of the translator:
 * <ul>
 * <li> Parsing the Zmake user script: A Zmake.zbnf syntax script describes the syntax of the zmake user script 
 *   and controls the ZBNF-parser.
 * <li> Generate the output script: The translation script controls it. The translation script is parsed too 
 * before it is used. The [[zbnfjax/xsl/ZmakeGenctrl.zbnf]] contains the syntax for it.
 * </ul>
 * The three scripts below are designated as ',,admin level,,' in the figure above. 
 * Where the end user writes its Zmake script and starts the make process for its sources, a person 
 * which administrates the make-tools edits and determines the ''translation script''. 
 * The ''translation script'' can contain some more options for the compiler or other translator tools, 
 * which are usage specific, but without influence possibility of the end-user. 
 * For example some standard include paths or compiling options may be determined from the administrator only. 
 * The translation script may be usage-unspecific also, whereby all options are supplied in the user script.
 * <br><br>
 * The [[zbnfjax:zmake/ZmakeStd.zbnf]] script determines the syntax and semantic of the user script. 
 * The given script containing in ,,zbnfjax/zmake/zmake.zbnf,, contains enough possibilities to formulate 
 * the users requests. It is adjusted with the Java-algorithm of the translator. 
 * But some enhancements are possible without changing the translator-program.
 * <br><br>
 * The [[zbnfjax:xsl/ZmakeGenctrl.zbnf]] controls the syntax of the translation script. 
 * It is adjusted with the internals of the translator. Only for special cases an adaption may be sensitive.
 * The translator itself, a Java-program, is not specialized to any input or output form.
 *
 * @author Hartmut Schorrig
 *
 */
public class Zmake
{

	/**Changes
	 * <ul>
	 * <li>2011-08-13: {@link ZmakeGenerator} regards < ?expandFiles>, see there.
	 * <li>2011-03-07: cmdline arguments removed -zbnf4ant, -tmpAntXml, new -o=OUT -zbnf=
	 * <li>2011-02-01: Hartmut creation. Before that, the {@link org.vishia.zbnf.Zmake} exists (in the zbnf-package)
	 *   which produces only ANT files. This solution bases on them, but it is more universal.
	 * </ul>
	 */
	public final static int version = 0x20110813;
	
	
	private static class CallingArgs
	{
  
    String input = null;
    
    /**String path to the XML_TOOLBASE Directory. */
    String zbnfjax_PATH;
    
    /**String path to the current dir from calling. */
    String curDir = null;  //default: actual dir
    
    /**String path fromcurDir to a tmp dir. */
    String tmp = "../tmp";  
    
    
    /**Path of ZBNF script for the input zmake script file*/
    String sZbnfInput = "zmake/ZmakeStd.zbnf";
    
    /**Path of ZBNF script to read the sGenCtrl*/
    String sZbnfGenCtrl = "zmake/ZmakeGenctrl.zbnf";
    
    /**Path of script to control the generation of the output file. */
    String sGenCtrl = "xsl/ZmakeStd2Ant.genctrl";
    
    /**Path of XSL script to generate the ant.xml*/
    //String sXslt4ant = "xsl/ZmakeStd.xslp";
    
    /**Path of the input.xml*/
    String sInputXml = null;
    
    /**Path of the ant.xml*/
    String sOutput = null;
    
    CallingArgs(){}
  };

  
  /** Aggregation to the Console implementation class.*/
  private final MainCmd_ifc console;

  /**
   * 
   */
  private final TextGenScript genScript;
  
  /**String path for the absolute tmp dir. */
  private String tmpAbs;  
  

  final CallingArgs args;
  
  /*---------------------------------------------------------------------------------------------*/
  /**Invocation from command line. 
   * <br>code in constructor to generate help info:<pre>
     Zmake generator                                               
     invoke>java -cp zbnf.jar org.vishia.zmake.Zmake [INPUT] [{OPTIONS}]
     + pathes to files or dirs are absolute or relativ from cmd line invocation.               
     + TPATH means a path started from given -ZBNFJAX_HOME:PATH or ZBNFJAX_HOME in environment.
       But if the path starts with . it is from current dir, 
     + WPATH means a path started from given -tmp directory (WorkPATH).
     + INPUTFILE is the only filename without path and without extension dissolved from INPUT                        
     INPUT              The first argument without - is the input file with path and extension.
     -i:INPUT           path to the input file alternatively to INPUT.                         
     -curdir:PATH       sets the current dir alternatively to command line invocation path.     
     -ZBNFJAX_HOME:PATH path to the ZBNFJAX_HOME, default it is gotten from environment.       
     -tmp:PATH          path of tmp dir, will be created if not exists, default=\"../tmp\".    
     -tmpinputxml:WPATH name of the temporary file parsed from input, default=INPUTFILE.xml   
     -o=PATH            output-file for generate the target                        
     -zbnf=TPATH        zbnf-file to parse the input                                           
     -genCtrl=TPATH     Zmake-genScript-file to generate the output file
     One can use either '=' or ':' as separator between option key and value.                                      
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
    genScript = new TextGenScript(console);
  }
  
  
  private static class CmdLine extends MainCmd
  {
    private final CallingArgs callingArgs;
    
    /*---------------------------------------------------------------------------------------------*/
    /** Constructor of the CmdLine class.
        The command line arguments are parsed here. After them the execute class is created as composition of SampleCmdLine.
    */
    CmdLine(String[] args, CallingArgs callingArgs)
    { super(args);
      this.callingArgs = callingArgs;
      //:TODO: user, add your help info!
      //super.addHelpInfo(getAboutInfo());
      super.addAboutInfo("Zmake generator");
      super.addAboutInfo("made by JcHartmut, 2007-07-06 - 2012-05-24");
      super.addHelpInfo("invoke>java -cp zbnf.jar org.vishia.zmake.Zmake [INPUT] [{OPTIONS}]");
      super.addHelpInfo("* pathes to files or dirs are absolute or relativ from cmd line invocation.");
      super.addHelpInfo("* TPATH means a path started from given -ZBNFJAX_HOME:PATH or ZBNFJAX_HOME in environment.");
      super.addHelpInfo("  But if the path starts with . it is from current dir. ");
      super.addHelpInfo("* WPATH means a path started from given -tmp directory (WorkPATH).");
      super.addHelpInfo("* INPUTFILE is the only filename without path and without extension dissolved from INPUT");
      super.addHelpInfo("INPUT              The first argument without - is the input file with path and extension.");
      super.addHelpInfo("-i:INPUT           path to the input file alternatively to INPUT.");
      super.addHelpInfo("-genCtrl:TPATH     file which describes the generation for the output file");
      super.addHelpInfo("-o=PATH            output-file to generate");
      super.addHelpInfo("-curdir:PATH       sets the current dir alternatively to command line invocation path.");
      super.addHelpInfo("-ZBNFJAX_HOME:PATH path to the ZBNFJAX_HOME, default it is getted from environment.");
      super.addHelpInfo("-tmp:PATH          path of tmp dir, will be created if not exists, default=\"../tmp\".");
      super.addHelpInfo("-tmpinputxml:WPATH name of the temporary file parsed from input, default=INPUTFILE.xml");
      super.addHelpInfo("-zbnf=TPATH        zbnf-file to parse the input, default is zmake/ZmakeStd.zbnf");
      super.addHelpInfo("-zGen:TPATH        zbnf-file to parse the genCtrl file, default is zmake/ZmakeGenctrl.zbnf");
      super.addHelpInfo("One can use either '=' or ':' as separator between option key and value.");
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
    @Override protected boolean testArgument(String arg, int nArg)
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
      else if(arg.startsWith("-o:"))           { callingArgs.sOutput = getArgument(3); }
      else if(arg.startsWith("-zbnf="))        { callingArgs.sZbnfInput = getArgument(6); }  //older version, compatibility
      else if(arg.startsWith("-zbnf:"))        { callingArgs.sZbnfInput = getArgument(6); }  //older version, compatibility
      else if(arg.startsWith("-zGen="))        { callingArgs.sZbnfGenCtrl = getArgument(6); }  //older version, compatibility
      else if(arg.startsWith("-zGen:"))        { callingArgs.sZbnfGenCtrl = getArgument(6); }  //older version, compatibility
      else if(arg.startsWith("-genCtrl="))     { callingArgs.sGenCtrl = getArgument(9); }
      else if(arg.startsWith("-genCtrl:"))     { callingArgs.sGenCtrl = getArgument(9); }
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
    @Override
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
    @Override
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
    
    /*
    if(args.sInputXml == null)
    { args.sInputXml = inputFile + inputExt + ".xml"; 
    }
    File fileZbnfXml = new File(tmpAbs + "/" + args.sInputXml);
    fileZbnfXml.setWritable(true); 
    fileZbnfXml.delete();
    */

    File fileOut = new File(args.sOutput);
    fileOut.setWritable(true); 
    fileOut.delete();
    
    File fileZbnf4GenCtrl = new File(args.zbnfjax_PATH + args.sZbnfGenCtrl);
    if(!fileZbnf4GenCtrl.exists()) throw new IllegalArgumentException("cannot find -zbnf4GenCtrl=" + fileZbnf4GenCtrl.getAbsolutePath());
    
    final String sFileGenCtrl = args.sGenCtrl.startsWith(".") ? args.sGenCtrl
                              : (args.zbnfjax_PATH + args.sGenCtrl);
    File fileGenCtrl = new File(sFileGenCtrl);
    if(!fileGenCtrl.exists()) throw new IllegalArgumentException("cannot find -genCtrl=" + fileGenCtrl.getAbsolutePath());
    
    //Build the data for ANT-generation control:
    genScript.parseGenCtrl(fileZbnf4GenCtrl, fileGenCtrl);
    
    console.writeInfoln("* Zmake: parsing user.zmake \"" + args.curDir + args.input + "\" with \"" 
      + args.zbnfjax_PATH + args.sZbnfInput + "\" to \""  + fileOut.getAbsolutePath() + "\"");
    //call the parser from input, it produces a temporary xml file.
    String sInputAbs = args.curDir + args.input;
    String sZbnf = args.zbnfjax_PATH + args.sZbnfInput;
    
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
    //
    XmlNodeSimple<ZbnfParseResultItem> xmlTop = parser.getResultTree();
    if(args.sInputXml !=null){
      //write XML output only to check the input script.
      OutputStreamWriter wrXml = new OutputStreamWriter(new FileOutputStream(sInputAbs_xml)); 
      SimpleXmlOutputter xmlOut = new SimpleXmlOutputter();
      xmlOut.write(wrXml, xmlTop);
      wrXml.close();
      
      //ZbnfXmlOutput xmlOutput = new ZbnfXmlOutput();
      //xmlOutput.write(parser, sInputAbs_xml + "2.xml");
    }
    //write into Java classes:
    ZmakeUserScript.UserScript zmakeInput = new ZmakeUserScript.UserScript();
    ZbnfJavaOutput parser2Java = new ZbnfJavaOutput(console);
    parser2Java.setContent(zmakeInput.getClass(), zmakeInput, parseResult);
    
        
    if(args.sInputXml !=null){
      FileWriter outData = new FileWriter(sInputAbs_xml + ".javadat");
      OutputDataTree outputterData = new OutputDataTree();
      outputterData.output(0, zmakeInput, outData, false);
      outData.close();
    }

    
    //evaluate
    console.writeInfoln("* generate script \"" + fileOut.getAbsolutePath() + "\"\n");
    TextGenerator gen = new TextGenerator(console);
    Writer out = new FileWriter(fileOut);
    TextGenScript genScript = gen.parseGenScript(fileGenCtrl, null);
    Map<String, Object> scriptVariables;
    try{ 
      scriptVariables = gen.genScriptVariables();
    } catch(IOException exc){
      System.err.println("Zmake - unexpected IOexception while generation; " + exc.getMessage());
      scriptVariables = null;
    }
    
    //The gen script may contain a currDir variable. Set the current directory of Zmake therewith.
    //the variable <=currDir><.=> may exist. Get it:
    Object oCurrDir = scriptVariables.get("currDir");
    if(oCurrDir != null){
      //Set the current dir in the user script. It is needed there for file path building.
      zmakeInput.setCurrentDir(null, DataAccess.getStringFromObject(oCurrDir, null));
    }
    
    try{ 
      sError = gen.genContent(genScript, zmakeInput, true, out);
    } catch(IOException exc){
      System.err.println("Zmake - unexpected IOexception while generation; " + exc.getMessage());
    }
    out.close();
    //ZmakeGenerator mng = new ZmakeGenerator(fileOut, zmakeInput, genScript, console);
    //mng.gen_ZmakeOutput();
    console.writeInfoln("* done");
    
        
    return sError;
  }
  
  

}
