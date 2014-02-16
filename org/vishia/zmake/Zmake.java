package org.vishia.zmake;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.ParseException;
import java.util.Map;



import org.vishia.mainCmd.MainCmd;
import org.vishia.mainCmd.MainCmdLogging_ifc;
import org.vishia.mainCmd.MainCmd_ifc;
import org.vishia.mainCmd.Report;
import org.vishia.util.Assert;
import org.vishia.util.DataAccess;
import org.vishia.util.FileSystem;
import org.vishia.util.StringPartScan;
import org.vishia.util.StringPartFromFileLines;
//import org.vishia.util.StringPartFromFile;
import org.vishia.xmlSimple.SimpleXmlOutputter;
import org.vishia.xmlSimple.XmlException;
import org.vishia.xmlSimple.XmlNode;
import org.vishia.cmd.ZGenExecuter;
import org.vishia.cmd.ZGenScript;
import org.vishia.zbnf.ZbnfJavaOutput;
import org.vishia.zbnf.ZbnfParseResultItem;
import org.vishia.zbnf.ZbnfParser;
import org.vishia.zgen.OutputDataTree;
import org.vishia.zgen.ZGen;
import org.vishia.zgen.ZGenSyntax;
import org.vishia.zgen.Zbnf2Text;



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
public class Zmake extends Zbnf2Text
{

  /**Version and history
   * <ul>
   * <li>2013-03-10: Hartmut chg: <:scriptclass:JavaPath> is supported up to now. Usage for docu generation with other/more capability.
   * <li>2011-08-13: {@link ZmakeGenerator} regards < ?expandFiles>, see there.
   * <li>2011-03-07: cmdline arguments removed -zbnf4ant, -tmpAntXml, new -o=OUT -zbnf=
   * <li>2011-02-01: Hartmut creation. Before that, the {@link org.vishia.zbnf.Zmake} exists (in the zbnf-package)
   *   which produces only ANT files. This solution bases on them, but it is more universal.
   * </ul>
   * 
   * <b>Copyright/Copyleft</b>:
   * For this source the LGPL Lesser General Public License,
   * published by the Free Software Foundation is valid.
   * It means:
   * <ol>
   * <li> You can use this source without any restriction for any desired purpose.
   * <li> You can redistribute copies of this source to everybody.
   * <li> Every user of this source, also the user of redistribute copies
   *    with or without payment, must accept this license for further using.
   * <li> But the LPGL ist not appropriate for a whole software product,
   *    if this source is only a part of them. It means, the user
   *    must publish this part of source,
   *    but don't need to publish the whole source of the own product.
   * <li> You can study and modify (improve) this source
   *    for own using or for redistribution, but you have to license the
   *    modified sources likewise under this LGPL Lesser General Public License.
   *    You mustn't delete this Copyright/Copyleft inscription in this source file.
   * </ol>
   * If you are intent to use this sources without publishing its usage, you can get
   * a second license subscribing a special contract with the author. 
   * 
   * @author Hartmut Schorrig = hartmut.schorrig@vishia.de
   * 
   * 
   */
  @SuppressWarnings("hiding")
  static final public int version = 20131029;
	
	
	private static class CallingArgs extends Zbnf2Text.Args
	{
  
    //String input = null;
    
    /**String path to the XML_TOOLBASE Directory. */
    String zbnfjax_PATH;
    
    /**String path to the current dir from calling. */
    //File currdir = null;  //default: actual dir
    
    /**String path from currdir to a tmp dir. */
    String tmp = "../tmp";  
    
    /**Path of ZBNF script for the input zmake script file*/
    //String sZbnfInput = "zmake/ZmakeStd.zbnf";
    
    /**Path of ZBNF script to read the sGenCtrl*/
    String sZbnfGenCtrl = "zmake/ZmakeGenctrl.zbnf";
    
    /**Path of script to control the generation of the output file. */
    String sGenCtrl = "xsl/ZmakeStd2Ant.genctrl";
    
    /**Output file for help with syntax information. */
    String sHelpOut;
    
    /**Path of XSL script to generate the ant.xml*/
    //String sXslt4ant = "xsl/ZmakeStd.xslp";
    
    /**Path of the ant.xml*/
    String sOutput = null;
    
    CallingArgs(){}
  };

  
  /** Aggregation to the Console implementation class.*/
  private final MainCmd_ifc console;

  
  private final ZmakeUserScript.UserScript zmakeInput;
  
  final ZGenExecuter gen;
  
  /**
   * 
   */
  /**String path for the absolute tmp dir. */
  //private String tmpAbs;  
  

  final CallingArgs args;
  
  
  File fileInput;
  
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
     -currdir:PATH       sets the current dir alternatively to command line invocation path.     
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
    cmdArgs.sFileSyntax = "zmake/ZmakeStd.zbnf";
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
      try{ 
        if(cmdArgs.sHelpOut !=null){
          FileWriter helpOut = new FileWriter(new File(cmdArgs.sHelpOut));
          for(String sLine: mainCmdLine.listHelpInfo){
            helpOut.append(sLine).append("\n");
          }
          helpOut.append("\n\n\n");
          helpOut.append("==Syntax of the ZmakeGenCtrlScript================================================================================\n");
          helpOut.append(ZGenSyntax.syntax);
          helpOut.append("==================================================================================================================\n");
          helpOut.close();
        }
        if(cmdArgs.sFileIn != null){
          main.execMake();
          //sExecuteError = main.parseAndTranslate();
        }
      }
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
  { super(args, console);
    this.args = args;
    this.console = console;
    gen = new ZGenExecuter(console);
    zmakeInput = new ZmakeUserScript.UserScript(gen);
  }
  
  
  private static class CmdLine extends Zbnf2Text.CmdLineText
  {
    private final CallingArgs callingArgs;
    
    /*---------------------------------------------------------------------------------------------*/
    /** Constructor of the CmdLine class.
        The command line arguments are parsed here. After them the execute class is created as composition of SampleCmdLine.
    */
    CmdLine(String[] args, CallingArgs callingArgs)
    { super(callingArgs, args);
      this.callingArgs = callingArgs;
      //:TODO: user, add your help info!
      //super.addHelpInfo(getAboutInfo());
      super.addAboutInfo("Zmake generator");
      super.addAboutInfo("made by JcHartmut, 2007-07-06 - 2012-05-24");
      super.addHelpInfo("invoke>java -cp zbnf.jar org.vishia.zmake.Zmake [INPUT] [{OPTIONS}]");
      super.addHelpInfo("* PATH to files or dirs are absolute or relativ from cmd line invocation.");
      super.addHelpInfo("* TPATH means a path started from given -ZBNFJAX_HOME:PATH or ZBNFJAX_HOME in environment.");
      super.addHelpInfo("  But if the path starts with . it is from current dir. ");
      super.addHelpInfo("* WPATH means a path started from given -tmp directory (WorkPATH).");
      //super.addHelpInfo("INPUT              The first argument without - is the input file with path and extension.");
      //super.addHelpInfo("-i:INPUT           path to the input file alternatively to INPUT.");
      //super.addHelpInfo("-genCtrl:TPATH     file which describes the generation for the output file");
      //super.addHelpInfo("-o=PATH            output-file to generate");
      //super.addHelpInfo("-currdir:PATH       sets the current dir alternatively to command line invocation path.");
      //super.addHelpInfo("-ZBNFJAX_HOME:PATH path to the ZBNFJAX_HOME, default it is getted from environment.");
      //super.addHelpInfo("-tmp:PATH          path of tmp dir, will be created if not exists, default=\"../tmp\".");
      //super.addHelpInfo("-tmpinputxml:WPATH name of the temporary file parsed from input, default=INPUTFILE.xml");
      //super.addHelpInfo("-zbnf:TPATH        zbnf-file to parse the input, default is zmake/ZmakeStd.zbnf");
      ////super.addHelpInfo("-zGen:TPATH        zbnf-file to parse the genCtrl file, default is the internal syntax");
      //super.addHelpInfo("-syntax:PATH       Write an information file which contains the help and the syntax.");
      super.addHelpInfo("One can use either '=' or ':' as separator between option key and value.");
      super.addArgument(argumentsZbnf2Xml);
      super.addArgument(argumentsZmake);
      super.addStandardHelpInfo();
      
    }
  
    private final MainCmd.Argument[] argumentsZmake =
    { new MainCmd.Argument("", "INPUT         The first argument without - is the input file with path and extension."
        , new MainCmd.SetArgument(){ @Override public boolean setArgument(String val){ 
          callingArgs.sFileIn = val; return true; }})
   /*
    , new MainCmd.Argument("-i", "=INPUT    path to the input file."
        , new MainCmd.SetArgument(){ @Override public boolean setArgument(String val){ 
          callingArgs.input = val; return true; }})
    */      
    , new MainCmd.Argument("-genCtrl", "=TPATH     file which describes the generation for the output file"
      , new MainCmd.SetArgument(){ @Override public boolean setArgument(String val){ 
          callingArgs.sGenCtrl = val; return true; }})
    , new MainCmd.Argument("-helpsyntax", "=PATH       Write an information file which contains the help and the syntax.", new MainCmd.SetArgument(){ @Override public boolean setArgument(String val){ 
          callingArgs.sHelpOut = val; return true; }})
    /*
    , new MainCmd.Argument("-zbnf", "=TPATH        zbnf-file to parse the input, default is zmake/ZmakeStd.zbnf", new MainCmd.SetArgument(){ @Override public boolean setArgument(String val){ 
          callingArgs.sZbnfInput = val; return true; }})
    */
    , new MainCmd.Argument("-o", "=PATH            output-file to generate", new MainCmd.SetArgument(){ @Override public boolean setArgument(String val){ 
          callingArgs.sOutput = val; return true; }})
    , new MainCmd.Argument("-ZBNFJAX_HOME", "=PATH path to the ZBNFJAX_HOME, default it is getted from environment.", new MainCmd.SetArgument(){ @Override public boolean setArgument(String val){ 
      callingArgs.zbnfjax_PATH = val; return true; }})
    , new MainCmd.Argument("-tmp", "=PATH of tmp dir, will be created if not exists, default=\"../tmp\"", new MainCmd.SetArgument(){ @Override public boolean setArgument(String val){ 
          callingArgs.tmp = val; return true; }})
    , new MainCmd.Argument("-checkxml", "=CHECK  if given then 3 files for debugging will be written"
        , new MainCmd.SetArgument(){ @Override public boolean setArgument(String val){ 
          callingArgs.sCheckXmlOutput = val; return true; }})
    };
 
  
    /** Invoked from parseArguments if no argument is given. In the default implementation a help info is written
     * and the application is terminated. The user should overwrite this method if the call without comand line arguments
     * is meaningfull.
     * @throws ParseException 
     *
     */
    @Override
    protected void callWithoutArguments()
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
  
      if(callingArgs.sHelpOut ==null){
        if(callingArgs.sFileIn == null)              
        { bOk = false; 
          writeError("ERROR argument -i=INP is obligate."); 
        }
    
        //if(callingArgs.sOutput == null)              
        if(callingArgs.listOut.size() ==0)
        { bOk = false; 
          writeError("Zmake: arguments -c=genctrl -t=textOut is obligate.");
          //writeError("ERROR argument -o=OUT is obligate."); 
        }
      }
      if(!bOk) setExitErrorLevel(exitWithArgumentError);
    
      return bOk;
    
    }
  }//class CmdLine

    
  
  
  
  
  
  
  
  /**Parse a user's zmake-script and provide a Java data view to its content.
   * @param fileZmakeUserscript The user script
   * @param sZbnf The syntax file for Zmake user script.
   * @param console 
   * @param jbatExecuter It is used to get the current dir and to help evaluate expressions.
   * @param sCheckXmlOutput if not null, then intermedia info are outputted
   * @return
   * @throws IllegalArgumentException
   * @throws IllegalAccessException
   * @throws InstantiationException
   * @throws FileNotFoundException
   * @throws IOException
   * @throws ParseException
   */
  public static ZmakeUserScript.UserScript parseUserScript(File fileZmakeUserscript
  , String sZbnf, MainCmdLogging_ifc console, ZGenExecuter jbatExecuter, String sCheckXmlOutput) 
  throws IllegalArgumentException, IllegalAccessException, InstantiationException, FileNotFoundException, IOException, ParseException 
  {
    ZmakeUserScript.UserScript zmakeInput;

    try{
      StringPartScan spInput = new StringPartFromFileLines(fileZmakeUserscript);
      ZbnfParser parser = new ZbnfParser(console);
      parser.setSyntax(new File(sZbnf));
      console.writeInfo(" ... ");
      if(!parser.parse(spInput)){
        String sError = parser.getSyntaxErrorReport();
        throw new ParseException(sError,0);
      }
      spInput.close();
      if(console.getReportLevel() >= Report.fineInfo){
        parser.reportStore(console, Report.fineInfo, "User-ZmakeScript");
      }
      console.writeInfo(" ok, set result ... ");
      ZbnfParseResultItem parseResult = parser.getFirstParseResult();
      //
      if(sCheckXmlOutput !=null){
        //write ZmakeUserScript into XML output only to check the input script.
        XmlNode xmlTop = parser.getResultTree();
        OutputStreamWriter wrXml = new OutputStreamWriter(new FileOutputStream(sCheckXmlOutput + "_zmake.test.xml")); 
        SimpleXmlOutputter xmlOut = new SimpleXmlOutputter();
        xmlOut.write(wrXml, xmlTop);
        wrXml.close();
        
      }
      //write ZmakeUserScript into Java classes:
      /*
      String scriptclass = this.genScript.getScriptclass();
      if(scriptclass !=null){
        try{
          @SuppressWarnings("unchecked")
          Class<ZmakeUserScript.UserScript> classZmake = (Class<ZmakeUserScript.UserScript>)Class.forName(scriptclass);
          zmakeInput = classZmake.newInstance();
        } catch (Exception e) {
          System.err.printf("Zmake - UserScriptClass faulty: %s\n", scriptclass);
          throw new IllegalArgumentException("error");
        }
      } else */{
        zmakeInput = new ZmakeUserScript.UserScript(jbatExecuter);
      }
      ZbnfJavaOutput parser2Java = new ZbnfJavaOutput(console);
      parser2Java.setContent(zmakeInput.getClass(), zmakeInput, parseResult);
      
      //TODO File currDir = (File)jbatExecuter.getScriptVariable("currDir");
      //zmakeInput.setCurrentDir(currDir);
      
      if(sCheckXmlOutput !=null){
        //Write the data structure of the ZmakeUserScript into a file to check.
        FileWriter outData = new FileWriter(sCheckXmlOutput + "_zmake.javadat.test");
        OutputDataTree outputterData = new OutputDataTree();
        outputterData.output(0, zmakeInput, outData, false);
        outData.close();
        
      }
    } catch(Exception exc){
      Assert.throwCompleteExceptionMessage("Zmake - parseUserScript;", exc);
      zmakeInput = null;
    }
    return zmakeInput;
    
  }
  

  
  PreparerParsedData prepareZmake = new PreparerParsedData(){
    @Override public void prepareParsedData(XmlNode xmlResult, ZbnfParseResultItem zbnfResult, ZGenScript zgenscript, ZGenExecuter zgen) { 
      prepareZmake(zgenscript, zgen);
    }
  };
  

  /*
  private void generateVariablesInZmakeUserscript(ZGenExecuter zgen) throws Exception{
    for(Map.Entry<String, ZmakeUserScript.ScriptVariable> entry: zmakeInput.var.entrySet()){
      ZmakeUserScript.ScriptVariable var = entry.getValue();
      CharSequence value = zgen.scriptLevel().evalString(var);
    }
  }
  */
  

  private void prepareZmake(ZGenScript zgenscript, ZGenExecuter zgen) { 
    try{ 
      //generateVariablesInZmakeUserscript(zgen);
      Zbnf2Text.Args argsZtext = (Zbnf2Text.Args)Zmake.this.argsx;
      String sCurrdir;
      if(argsZtext.sCurrdir !=null){
        sCurrdir = argsZtext.sCurrdir;
      } else {
        sCurrdir = (new File(argsZtext.sFileIn)).getAbsoluteFile().getParent(); 
      }
      ZmakeUserScript.ScriptVariable varCurrdir = zmakeInput.var.get("currdir");
      if(varCurrdir !=null) {
        CharSequence sCurrdirZmakescript = zgen.scriptLevel().evalString(varCurrdir);
        sCurrdir += "/" + sCurrdirZmakescript;
      }
      zgen.genScriptVariables(zgenscript, true, null, sCurrdir);
      zgen.setScriptVariable("zmake", 'O', zmakeInput, true);
      File currdir = (File)zgen.getScriptVariable("currdir").value();  //set in script level
      zmakeInput.setCurrentDir(currdir); //use for build absolute paths. 
    } catch(Exception exc){
      throw new RuntimeException(exc);
    }
    
  }
  
  
  
  
  private void execMake() throws IllegalArgumentException, IllegalAccessException, InstantiationException, IOException, ParseException, XmlException{
    if(args.zbnfjax_PATH==null) { args.zbnfjax_PATH = System.getenv("ZBNFJAX_HOME"); }
    if(args.zbnfjax_PATH==null) { args.zbnfjax_PATH = System.getenv("XML_TOOLBASE"); }
    if(args.zbnfjax_PATH == null)
    { throw new ParseException("ZBNFJAX_HOME is not set. Either you should set a environment variable with this name or you should use the -ZBNFJAX_HOME: cmdline-Argument.", 0);
    }
    if(args.zbnfjax_PATH.startsWith("\"") && args.zbnfjax_PATH.length()>=2){ 
      args.zbnfjax_PATH = args.zbnfjax_PATH.substring(1, args.zbnfjax_PATH.length()-1); 
    }
    args.zbnfjax_PATH += "/";
    if(args.sFileSyntax !=null && !FileSystem.isAbsolutePath(args.sFileSyntax) && !args.sFileSyntax.startsWith("./")){
      args.sFileSyntax = args.zbnfjax_PATH + args.sFileSyntax;
    }

    
    console.writeInfoln("* Zmake: " + args.sFileIn);
    super.parseAndTranslate(args, zmakeInput, prepareZmake);
  }
  
  
  /** Execute the task of the class. 
   * @throws ParseException 
   * @throws XmlException 
   * @throws InstantiationException 
   * @throws IllegalAccessException 
   * @throws IllegalArgumentException 
   * @throws IOException
   */
  String parseAndTranslate() throws ParseException, XmlException, IllegalArgumentException, IllegalAccessException, InstantiationException, IOException
  { String sError = null;
    
    //the followed line maybe unnecessary because the java cmd line interpretation always cuts the quotion  marks,
    //Such quotion marks appeares if a double click from commandline is happened. 
    if(args.sFileIn.startsWith("\"") && args.sFileIn.length()>=2){ args.sFileIn = args.sFileIn.substring(1, args.sFileIn.length()-1); }
    ////
    /*Separate input path file ext.*/
    //String inputFile, inputExt;
    { int pos1 = args.sFileIn.lastIndexOf(('/'));
      int pos2 = args.sFileIn.lastIndexOf(('\\'));
      int pos3 = args.sFileIn.lastIndexOf((':'));
      if(pos2 > pos1){ pos1 = pos2; }
      if(pos3 > pos1){ pos1 = pos3; }
      if(pos1 < 0){ pos1 = -1; }
      int pos9 = args.sFileIn.lastIndexOf('.');
      if(pos9 < pos1) { pos9 = args.sFileIn.length(); }
      //inputFile = args.sFileIn.substring(pos1 + 1, pos9); //, pos9);
      //inputExt =  args.sFileIn.substring(pos9);
      
    }
    
    //tmpAbs = args.currdir +args.tmp;
    
    if(args.zbnfjax_PATH==null) { args.zbnfjax_PATH = System.getenv("ZBNFJAX_HOME"); }
    if(args.zbnfjax_PATH==null) { args.zbnfjax_PATH = System.getenv("XML_TOOLBASE"); }
    if(args.zbnfjax_PATH == null)
    { throw new ParseException("ZBNFJAX_HOME is not set. Either you should set a environment variable with this name or you should use the -ZBNFJAX_HOME: cmdline-Argument.", 0);
    }
    if(args.zbnfjax_PATH.startsWith("\"") && args.zbnfjax_PATH.length()>=2){ 
      args.zbnfjax_PATH = args.zbnfjax_PATH.substring(1, args.zbnfjax_PATH.length()-1); 
    }
    args.zbnfjax_PATH += "/";
    
    console.writeInfoln("* Zmake: " + args.sFileIn);
    
    File fileOut = new File(args.sOutput);
    fileOut.setWritable(true); 
    fileOut.delete();
    //
    //Parses the genctrl
    //
    final String sFileGenCtrl = args.sGenCtrl.startsWith(".") ? args.sGenCtrl
                              : (args.zbnfjax_PATH + args.sGenCtrl);
    File fileGenCtrl = new File(sFileGenCtrl);
    if(!fileGenCtrl.exists()) throw new IllegalArgumentException("cannot find -genCtrl=" + fileGenCtrl.getAbsolutePath());
    
    ///
    //ZGen.Args argsZ = new ZGen.Args();
    //ZGen zbatch = new ZGen(argsZ, console);
    //ZGenScript genScript; // = new JbatchScript(textGen, console);
    
    //File checkXmlGenctrl = args.sCheckXmlOutput==null ? null : new File(args.sCheckXmlOutput + "_ZText.xml");
    //genScript = zbatch.translateAndSetGenCtrl(fileGenCtrl, checkXmlGenctrl);
    
    console.writeInfoln("* Zmake: parsing user.zmake \"" + args.sCurrdir + args.sFileIn + "\" with \"" 
      + args.zbnfjax_PATH + args.sFileSyntax + "\" to \""  + fileOut.getAbsolutePath() + "\"");
    //call the parser from input, it produces a temporary xml file.
    String sZbnf = args.zbnfjax_PATH + args.sFileSyntax;
    
    
    //String sInputAbs_xml = tmpAbs + "/" + args.sInputXml;
    fileInput = new File(args.sFileIn);
    
    //Parse the users zmake script:
    //evaluate
    console.writeInfoln("* generate script \"" + fileOut.getAbsolutePath() + "\"\n");
    //JbatchScript genScript = new JbatchScript(gen, console); //gen.parseGenScript(fileGenCtrl, null);
    File checkXmlOutGenCtrl = args.sCheckXmlOutput == null ? null : new File(args.sCheckXmlOutput + "_check.genctrl");
    
    //The generation script:
    ZGenScript genScript = ZGen.translateAndSetGenCtrl(fileGenCtrl, checkXmlOutGenCtrl, console);
    
    //The users make file:
    final ZmakeUserScript.UserScript zmakeInput = parseUserScript(fileInput, sZbnf, console, gen, args.sCheckXmlOutput);

    Writer out = new FileWriter(fileOut);
    Map<String, DataAccess.Variable<Object>> scriptVariables;
    try{ 
      scriptVariables = gen.genScriptVariables(genScript, true, null, args.sCurrdir);
    } catch(IOException exc){
      System.err.println("Zmake - unexpected IOexception while generation; " + exc.getMessage());
      scriptVariables = null;
    }
    
    setScriptVariablesCurrDir(zmakeInput, scriptVariables);
    
    try{ 
      gen.setScriptVariable("zmake", 'O', zmakeInput, true);
      gen.execute(genScript, true, true, out, args.sCurrdir);
    } catch(Exception exc){
      CharSequence sMsg = Assert.exceptionInfo("Zmake - Exception; ", exc, 0, 10);
      System.err.println(sMsg);
    }
    out.close();
    //ZmakeGenerator mng = new ZmakeGenerator(fileOut, zmakeInput, genScript, console);
    //mng.gen_ZmakeOutput();
    console.writeInfoln("* done");
    
        
    return sError;
  }

  
  
  void setScriptVariablesCurrDir(ZmakeUserScript.UserScript zmakeInput, Map<String, DataAccess.Variable<Object>> scriptVariables) 
  throws FileNotFoundException{
    /*
    File currdir = args.currdir;
    String sCurrdir;
    if(args.currdir == null){
      File dirInput = FileSystem.getDirectory(fileInput);
      //The gen script may contain a currdir variable. Set the current directory of Zmake therewith.
      //the variable <=currdir><.=> may exist. Get it:
      Object oCurrDir = scriptVariables.get("currdir");
      if(oCurrDir != null){
        //Set the current dir in the user script. It is needed there for file path building.
        sCurrdir = DataAccess.getStringFromObject(oCurrDir, null);
        currdir = new File(dirInput, sCurrdir);
      } else {
        currdir = dirInput;
      }
    }
    if(!currdir.exists()){
      throw new FileNotFoundException("Zmake - currdir does not exist; " + currdir.getAbsolutePath());
    } else {
      zmakeInput.setCurrentDir(currdir);
    }
    */
  }

}
