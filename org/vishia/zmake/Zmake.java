package org.vishia.zmake;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.util.Map;



import org.vishia.mainCmd.MainCmd;
import org.vishia.mainCmd.MainCmdLogging_ifc;
import org.vishia.mainCmd.MainCmd_ifc;
import org.vishia.util.FileSystem;
import org.vishia.xmlSimple.XmlException;
import org.vishia.xmlSimple.XmlNode;
import org.vishia.cmd.ZGenExecuter;
import org.vishia.cmd.ZGenScript;
import org.vishia.zbnf.ZbnfParseResultItem;
import org.vishia.zgen.ZGenSyntax;
import org.vishia.zgen.Zbnf2Text;



/**The Zmake translator translates between a Zmake user script and the ready-to-run make-script. 
 * The Zmake user script describes what's how to make in a more simple syntax than most of the other known 
 * make scripts. The output make-script may be an ANT.xml-make file, an old-style make file, 
 * a simple batch or script file or any other control file for a make process. 
 * The Zmake translator is sufficient for universal usage, it  controls which text is produced for the output.
 * <pre>
  Zmake
  user    ====ZBNF====ZGen========> make script
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
 * <li> Generate the output script: Therefore the {@link ZGenExecuter} is used. The translation script controls it. 
 *   The translation script is parsed too before it is used. The Syntax for ZGen is determined by the 
 *   {@link org.vishia.zgen.ZGenSyntax}. The usage of ZGen and the syntax is described in 
 *   {@linkplain www.vishia.org/ZBNF/sf/ZBNF/docu/Zgen.html}.
 * </ul>
 * The three scripts below are designated as ',,admin level,,' in the figure above. 
 * Where the end user writes its Zmake script and starts the make process for its sources, a person 
 * which administered the make-tools edits and determines the ''translation script''. 
 * The ''translation script'' can contain some more options for the compiler or other translator tools, 
 * which are usage specific, but without influence possibility of the end-user. 
 * For example some standard include paths or compiling options may be determined from the administrator only. 
 * The translation script may be usage-unspecific also, whereby all options are supplied in the user script.
 * <br><br>
 * The syntax of the Zmake user script, named <code>zmake.zbnf</code> in the figure above, can be set 
 * by the command line option <code>-s:zmake.zbnf</code>. If that option is not given the <code>zbnfjax/ZmakeStd.zbnf</code>
 * is used. The directory where <code>zbnfjax</code> is found have to be determined either by an environment variable
 * <code>ZBNFJAX_HOME</code> or by the command line option <code>-ZBNFJAX_HOME:PATH</code>.  
 * The given <code>zbnfjax/ZmakeStd.zbnf</code> contains enough possibilities to formulate 
 * the users requests. It is adjusted with the class {@link ZmakeUserScript} which holds the parsed data 
 * (using {@link org.vishia.zbnf.ZbnfJavaOutput}). 
 * But some enhancements are possible without changing the translator-program.
 * <br><br>
 * The newness of a file which forces a compilation is not tested in the Zmake class. The Zmake is only 
 * a management of translation. The newness can be tested with the capability of {@link org.vishia.checkDeps_C.CheckDeps}
 * which is written for C and C++ files. That class can be used inside the translation script. 
 * <br><br>
 * All components including the Zmake are bundled in a download zip file with this sources, documentation, 
 * examples.
 * <ul>
 * <li>{@linkplain sf.net/ZBNF} Project on sourceforge
 * <li>{@linkplain www.vishia.org/indexDownload.html} vishia download page
 * <li>{@linkplain www.vishia.org/ZBNF/sf/ZBNF/Docu/readme_ZBNF.html} The main description in the download component
 * <li>{@linkplain www.vishia.org/ZBNF/sf/ZBNF/Docu/Zmake.html}
 * <li>{@linkplain www.vishia.org/ZBNF/sf/ZBNF/Docu/ZGen.html}
 * </ul> 
 * @author Hartmut Schorrig
 *
 */
public class Zmake extends Zbnf2Text
{

  /**Version and history
   * <ul>
   * <li>2014-02-16: Hartmut chg: conjunction with {@link Zbnf2Text}, redesign of ZGen concept. 
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
  static final public String sVersion = "2014-02-16";
	
	
	private static class CallingArgs extends Zbnf2Text.Args
	{
    /**String path to the XML_TOOLBASE Directory. */
    String zbnfjax_PATH;
    
    /**Output file for help with syntax information. */
    String sHelpOut;
    
    CallingArgs(){}
  };

  
  /** Aggregation to the Console implementation class.*/
  private final MainCmd_ifc console;

  
  private final ZmakeUserScript.UserScript zmakeInput;
  
  /**ZGen in the user script is executed by another ZGenExecuter than the execution of the translation script.
   * The both scripts should be independently.
   */
  final ZGenExecuter zgenUserScript;
  

  final CallingArgs argsZmake;
  
  
  /**Invocation from command line. Invoke without arguments to get help.
   * All options of {@link org.vishia.zbnf.Zbnf2Xml} and {@link Zbnf2Text} all valid too because there are
   * super classes of this. The command line argument <code>-s:Syntax</code> for the parsing of the 
   * Zmake user script is set to <code>zmake/ZmakeStd.zbnf</code> per default. This option is not necessary.
   */
  public static void main(String [] args)
  { //creates the args-class before creating the main class and fills it:
    Zmake.CallingArgs cmdArgs = new Zmake.CallingArgs();
    cmdArgs.sFileSyntax = "zmake/ZmakeStd.zbnf";
    CmdLine mainCmdLine = new CmdLine(args, cmdArgs); //the instance to parse arguments and others.
    boolean bOk = true;
    try{ mainCmdLine.parseArguments(); }
    catch(Exception exception)
    { mainCmdLine.setExitErrorLevel(MainCmdLogging_ifc.exitWithArgumentError);
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
        main.console.setExitErrorLevel(MainCmdLogging_ifc.exitWithErrors);
      }
      if(sExecuteError != null)
      { 
        main.console.reportln(0, sExecuteError);
        main.console.setExitErrorLevel(MainCmdLogging_ifc.exitWithErrors);
      }
    }
    main.console.writeInfoln("* ");
    mainCmdLine.exit();
  }


  public Zmake(CallingArgs args, MainCmd_ifc console)
  { super(args, console);
    this.argsZmake = args;
    this.console = console;
    zgenUserScript = new ZGenExecuter(console);
    zmakeInput = new ZmakeUserScript.UserScript(zgenUserScript);
  }
  
  
  private void prepareZmake(ZGenScript zgenscript, ZGenExecuter zgen) { 
    try{ 
      Zbnf2Text.Args argsZtext = (Zbnf2Text.Args)Zmake.this.argsx;
      String sCurrdir;
      if(argsZtext.sCurrdir !=null){
        sCurrdir = argsZtext.sCurrdir;
      } else {
        sCurrdir = (new File(argsZtext.sFileIn)).getAbsoluteFile().getParent(); 
      }
      ZmakeUserScript.ScriptVariable varCurrdir = zmakeInput.var.get("currdir");
      if(varCurrdir !=null) {
        CharSequence sCurrdirZmakescript = varCurrdir.text(); //zgen.scriptLevel().evalString(varCurrdir);
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
    if(argsZmake.zbnfjax_PATH==null) { argsZmake.zbnfjax_PATH = System.getenv("ZBNFJAX_HOME"); }
    if(argsZmake.zbnfjax_PATH==null) { argsZmake.zbnfjax_PATH = System.getenv("XML_TOOLBASE"); }
    if(argsZmake.zbnfjax_PATH == null)
    { throw new ParseException("ZBNFJAX_HOME is not set. Either you should set a environment variable with this name or you should use the -ZBNFJAX_HOME: cmdline-Argument.", 0);
    }
    if(argsZmake.zbnfjax_PATH.startsWith("\"") && argsZmake.zbnfjax_PATH.length()>=2){ 
      argsZmake.zbnfjax_PATH = argsZmake.zbnfjax_PATH.substring(1, argsZmake.zbnfjax_PATH.length()-1); 
    }
    argsZmake.zbnfjax_PATH += "/";
    if(argsZmake.sFileSyntax !=null && !FileSystem.isAbsolutePath(argsZmake.sFileSyntax) && !argsZmake.sFileSyntax.startsWith("./")){
      argsZmake.sFileSyntax = argsZmake.zbnfjax_PATH + argsZmake.sFileSyntax;
    }
    for(Zbnf2Text.Out item: argsZmake.listOut){
      if(item.sFileScript !=null && !FileSystem.isAbsolutePath(item.sFileScript) && !item.sFileScript.startsWith("./")){
        item.sFileScript = argsZmake.zbnfjax_PATH + item.sFileScript;
      }
    }
    
    
    console.writeInfoln("* Zmake: " + argsZmake.sFileIn);
    super.parseAndTranslate(argsZmake, zmakeInput, prepareZmake);
  }


  PreparerParsedData prepareZmake = new PreparerParsedData(){
    @Override public void prepareParsedData(XmlNode xmlResult, ZbnfParseResultItem zbnfResult, ZGenScript zgenscript, ZGenExecuter zgen) { 
      prepareZmake(zgenscript, zgen);
    }
  };


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
    , new MainCmd.Argument("-helpsyntax", "=PATH       Write an information file which contains the help and the syntax.", new MainCmd.SetArgument(){ @Override public boolean setArgument(String val){ 
          callingArgs.sHelpOut = val; return true; }})
    /*
    , new MainCmd.Argument("-zbnf", "=TPATH        zbnf-file to parse the input, default is zmake/ZmakeStd.zbnf", new MainCmd.SetArgument(){ @Override public boolean setArgument(String val){ 
          callingArgs.sZbnfInput = val; return true; }})
    */
    , new MainCmd.Argument("-ZBNFJAX_HOME", "=PATH path to the ZBNFJAX_HOME, default it is getted from environment.", new MainCmd.SetArgument(){ @Override public boolean setArgument(String val){ 
      callingArgs.zbnfjax_PATH = val; return true; }})
    , new MainCmd.Argument("-checkxml", "=CHECK  if given then 3 files for debugging will be written"
        , new MainCmd.SetArgument(){ @Override public boolean setArgument(String val){ 
          callingArgs.sCheckXmlOutput = val; return true; }})
    };
 
  
  
  
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
  
  
}
