package org.vishia.zgen;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.vishia.cmd.ZGenExecuter;
import org.vishia.cmd.ZGenScript;
import org.vishia.mainCmd.MainCmd;
import org.vishia.mainCmd.MainCmdLoggingStream;
import org.vishia.mainCmd.MainCmdLogging_ifc;
import org.vishia.mainCmd.MainCmd_ifc;
import org.vishia.util.Assert;
import org.vishia.util.DataAccess;
import org.vishia.util.FileSystem;
import org.vishia.util.StringPartScan;
import org.vishia.util.StringPartFromFileLines;
import org.vishia.util.StringPartScan;
import org.vishia.util.UnexpectedException;
import org.vishia.xmlSimple.SimpleXmlOutputter;
import org.vishia.xmlSimple.XmlException;
import org.vishia.xmlSimple.XmlNode;
import org.vishia.zbnf.ZbnfJavaOutput;
import org.vishia.zbnf.ZbnfParser;


public class ZGen
{
  
  /**Version and history
   * <ul>
   * <li>2013-10-27 Hartmut chg: {@link #jbatch(String, org.vishia.cmd.ZGenExecuter.ExecuteLevel)}
   * <li>2012-10-03 created. Backgorund was the {@link org.vishia.zmake.Zmake} generator, but that is special for make problems.
   *   A generator which converts ZBNF-parsed data from an Java data context to output texts in several form, documenation, C-sources
   *   was need.
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
  //@SuppressWarnings("hiding")
  static final public int version = 20131029;

  
  public static class Args{
    
    /**path to the script file for the generation or execution script of ZGen.*/
    String sFileScript;
    
    /**path to the text output file which is generated by ZGen. May be null, then no text output. */
    String sFileTextOut;
    
    /**path to some output files for debugging, maybe left null. */
    File fileTestXml;
    
    /**User arguments, stored as script variable "$1" till "$n" */
    List<String> userArgs = new ArrayList<String>();
  }
  

  protected final MainCmdLogging_ifc log;
  
  protected final Args args;
  
  protected final ZGenExecuter executer;
  
  
  public ZGen(Args args, MainCmdLogging_ifc log){
    this.log = log;
    this.args = args;
    this.executer = new ZGenExecuter(log);
  }
  
  
  /**Does not support {@link #execute()}, only {@link #translateAndSetGenCtrl(File, File)}.
   * @param log
   */
  public ZGen(MainCmdLogging_ifc log){
    this.log = log;
    this.args = null;
    this.executer = null;
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
      if(args.sFileScript !=null){
        ZGen main = new ZGen(args, mainCmdLine);     //the main instance
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


  
  
  /**Parses the script and executes it independent from any other inputs.
   * It executes the script like a batch with access to any Java classes.
   * Therefore it is a 'Java batch executer'.
   * @param script Path to the script.
   * @return any error string. null if successfull.
   * @throws IllegalAccessException 
   */
  public static String jbatch(String script) throws IllegalAccessException{
    
    Args args = new Args();
    args.sFileScript = script;
    MainCmdLogging_ifc log = new MainCmdLoggingStream(System.out);
    ZGen zGen = new ZGen(args, log);
    return zGen.execute();
  }
  
  
  
  /**Parses the script and executes it with a given ZGen ExecuterLevel.
   * That is the possibility to start a independent ZGen execution in a ZGen script itself.
   * @param script Path to the script.
   * @return TODO ? any error string. null if successfull.
   */
  public static String jbatch(String script, ZGenExecuter.ExecuteLevel zgenExecuteLevel){
    boolean bWaitForThreads = false;
    StringBuilder u = new StringBuilder();
    //ZGenScript genScript = null; //gen.parseGenScript(fileGenCtrl, null);
    MainCmdLogging_ifc log = zgenExecuteLevel.log();
    Args args = new Args();
    args.sFileScript = script;
    ZGen zgen = new ZGen(args, log);
    //Copy all local variables of the calling level as script variables.
    try { 
      File fileScript = new File(script);
      ZGenScript genScript = translateAndSetGenCtrl(fileScript, null, log);
      //the script variables are build from the local ones of the calling script:
      zgen.executer.genScriptVariables(genScript, true, zgenExecuteLevel.localVariables);
      zgen.executer.execute(genScript, true, bWaitForThreads, u);
      //zgenExecuteLevel.execute(genScript.getMain().subContent, u, false);
    } catch (Exception exc) {
      String sError = exc.getMessage();
      System.err.println(sError);
    }
    return u.toString();
  }
  
  
  
  
  public String execute() throws IllegalAccessException{
    String sError = null;
    Appendable out = null;
    if(args.sFileTextOut !=null){
      try{
        File fileOut = new File(args.sFileTextOut);
        out = new FileWriter(fileOut);
      } catch(IOException exc){
        sError = "Jbat - cannot create output text file;"; 
      }
    } else {
      out = System.out;
    }
    File fileIn = new File(args.sFileScript);
    char nrArg = '1';
    for(String argu: args.userArgs){
      executer.setScriptVariable("$" + nrArg, 'S', argu, true);
      nrArg +=1;
    }
    sError = execute(executer, fileIn, out, true, args.fileTestXml, log);
    return sError;
  }
  
  
  
  /**Generates a text described with a file given script from any data. This is the main routine of this class.
   * @param userData The data pool where all data are stored
   * @param fileScript The script to generate the text context
   * @param out output for the text
   * @param accessPrivate if true then private data are accessed too. The accessing of private data may be helpfull
   *  for debugging. It is not recommended for general purpose! The access mechanism is given with 
   *  {@link java.lang.reflect.Field#setAccessible(boolean)}.
   * @param testOut if not null then outputs a data tree of the generate script.
   * @return null if no error or an error string.
   */
  public static String execute(ZGenExecuter executer, File fileScript, Appendable out, boolean accessPrivate
      , File testOut, MainCmdLogging_ifc log) {
    String sError = null;
    ZGenScript genScript = null; //gen.parseGenScript(fileGenCtrl, null);
    try { genScript = translateAndSetGenCtrl(fileScript, testOut, log);
    } catch (ParseException exc){
      sError = exc.getMessage();
      System.err.println("ZGen - Error parsing genscript; " + fileScript.getAbsolutePath() + "; " + sError); 
    } catch (Exception exc) {
      sError = exc.getMessage();
      System.err.println(Assert.exceptionInfo("ZGen - exception execute parsing;", exc,0,10));
    }
    //genScript = parseGenScript(fileScript, testOut);
    if(sError == null) { // && out !=null){
      try{
        executer.execute(genScript, accessPrivate, true, out);
        //out.close();
      } catch(Exception exc){
        System.err.println(Assert.exceptionInfo("", exc, 0, 4));
      }
    }
    //String sError = generator.generate(zbnfResultData, fileScript, fOut, true);
    return sError;
  }
  
  

  public static ZGenScript translateAndSetGenCtrl(File fileZbnf4GenCtrl, File fileGenCtrl, File checkXmlOut, MainCmdLogging_ifc log) 
  throws FileNotFoundException, IOException
    , ParseException, XmlException, IllegalArgumentException, IllegalAccessException, InstantiationException
  { log.writeInfoln("* Zbatch: parsing gen script \"" + fileZbnf4GenCtrl.getAbsolutePath() 
    + "\" with \"" + fileGenCtrl.getAbsolutePath() + "\"");

    int lengthBufferSyntax = (int)fileZbnf4GenCtrl.length();
    StringPartScan spSyntax = new StringPartFromFileLines(fileZbnf4GenCtrl, lengthBufferSyntax, "encoding", null);

    int lengthBufferGenctrl = (int)fileGenCtrl.length();
    StringPartScan spGenCtrl = new StringPartFromFileLines(fileGenCtrl, lengthBufferGenctrl, "encoding", null);

    File fileParent = FileSystem.getDir(fileGenCtrl);
    return translateAndSetGenCtrl(new StringPartScan(ZGenSyntax.syntax), new StringPartScan(spGenCtrl), checkXmlOut, fileParent, log);
  }
  

  public static ZGenScript translateAndSetGenCtrl(File fileGenCtrl, MainCmdLogging_ifc log) 
  throws FileNotFoundException, IllegalArgumentException, IllegalAccessException, InstantiationException, IOException, ParseException, XmlException 
  {
    return translateAndSetGenCtrl(fileGenCtrl, null, log);
  }
  
  
  public static ZGenScript translateAndSetGenCtrl(File fileGenCtrl, File checkXmlOut, MainCmdLogging_ifc log) 
  throws FileNotFoundException, IllegalArgumentException, IllegalAccessException, InstantiationException, IOException, ParseException, XmlException 
  {
    int lengthBufferGenctrl = (int)fileGenCtrl.length();
    StringPartScan spGenCtrl = new StringPartFromFileLines(fileGenCtrl, lengthBufferGenctrl, "encoding", null);
    File fileParent = FileSystem.getDir(fileGenCtrl);
    return translateAndSetGenCtrl(new StringPartScan(ZGenSyntax.syntax), new StringPartScan(spGenCtrl), checkXmlOut, fileParent, log);
  }
  
  
  public static ZGenScript translateAndSetGenCtrl(String spGenCtrl, MainCmdLogging_ifc log) 
  throws IllegalArgumentException, IllegalAccessException, InstantiationException, ParseException 
  {
    try{ 
      return translateAndSetGenCtrl(new StringPartScan(ZGenSyntax.syntax), new StringPartScan(spGenCtrl), null, null, log);
    } catch(IOException exc){ throw new UnexpectedException(exc); }
  }
  
  
  public static ZGenScript translateAndSetGenCtrl(StringPartScan spGenCtrl, MainCmdLogging_ifc log) 
  throws ParseException, IllegalArgumentException, IllegalAccessException, InstantiationException 
  {
    try { 
      return translateAndSetGenCtrl(new StringPartScan(ZGenSyntax.syntax), spGenCtrl, null, null, log);
    }catch(IOException exc){ throw new UnexpectedException(exc); }
  }
  
  
  
  /**Translate the generation control file - core routine.
   * It sets the {@link #zTextGenCtrl} aggregation. This routine must be called before  the script can be used.
   * There are some routines without the parameter sZbnf4GenCtrl, which uses the internal syntax. Use those if possible:
   * {@link #translateAndSetGenCtrl(File)}, {@link #translateAndSetGenCtrl(String)}
   * <br><br>
   * This routine will be called recursively if scripts are included.
   * 
   * @param sZbnf4GenCtrl The syntax. This routine can use a special syntax. The default syntax is {@link ZGenSyntax#syntax}.
   * @param spGenCtrl The input file with the genCtrl statements.
   * @param checkXmlOut If not null then writes the parse result to this file, only for check of the parse result.
   * @param fileParent directory of the used file as start directory for included scripts. 
   *   null possible, then the script should not contain includes.
   * @return a new instance of {@link ZbnfMainGenCtrl}. This method returns null if there is an error. 
   * @throws ParseException
   * @throws IllegalArgumentException
   * @throws IllegalAccessException
   * @throws InstantiationException
   * @throws IOException only if xcheckXmlOutput fails
   * @throws FileNotFoundException if a included file was not found or if xcheckXmlOutput file not found or not writeable
   */
  public static ZGenScript translateAndSetGenCtrl(StringPartScan sZbnf4GenCtrl, StringPartScan spGenCtrl
      , File checkXmlOutput, File fileParent, MainCmdLogging_ifc log) 
  throws ParseException, IllegalArgumentException, IllegalAccessException, InstantiationException, FileNotFoundException, IOException 
  { final MainCmdLogging_ifc log1;
    if(log == null){
      log1 = new MainCmdLoggingStream(System.out);
    } else { log1 = log; }
    ZbnfParser parserGenCtrl = new ZbnfParser(log1); //console);
    parserGenCtrl.setSyntax(sZbnf4GenCtrl);
    if(log1.getReportLevel() >= MainCmdLogging_ifc.fineInfo){
      log1.reportln(MainCmdLogging_ifc.fineInfo, "== Syntax GenCtrl ==");
      parserGenCtrl.reportSyntax(log1, MainCmdLogging_ifc.fineInfo);
    }
    //log1.writeInfo(" ... ");
    return translateAndSetGenCtrl(parserGenCtrl, spGenCtrl, checkXmlOutput, fileParent, log1);
  }
    
    
    
    
  private static ZGenScript translateAndSetGenCtrl(ZbnfParser parserGenCtrl, StringPartScan spGenCtrl
      , File checkXmlOutput, File fileParent, MainCmdLogging_ifc log) 
  throws ParseException, IllegalArgumentException, IllegalAccessException, InstantiationException, FileNotFoundException, IOException 
  { boolean bOk;
    
    bOk = parserGenCtrl.parse(spGenCtrl);
    if(!bOk){
      String sError = parserGenCtrl.getSyntaxErrorReport();
      throw new ParseException(sError,0);
    }
    if(checkXmlOutput !=null){
      //XmlNodeSimple<?> xmlParseResult = parserGenCtrl.getResultTree();
      XmlNode xmlParseResult = parserGenCtrl.getResultTree();
      SimpleXmlOutputter xmlOutputter = new SimpleXmlOutputter();
      OutputStreamWriter xmlWriter = new OutputStreamWriter(new FileOutputStream(checkXmlOutput));
      xmlOutputter.write(xmlWriter, xmlParseResult);
      xmlWriter.close();
    }
    //if(log.getReportLevel() >= MainCmdLogging_ifc.fineInfo){
    //  parserGenCtrl.reportStore((Report)log, MainCmdLogging_ifc.fineInfo, "Zmake-GenScript");
    //}
    //write into Java classes:
    /**Helper to transfer parse result into the java classes {@link ZbnfMainGenCtrl} etc. */
    final ZbnfJavaOutput parserGenCtrl2Java = new ZbnfJavaOutput(log);

    ZGenScript script = new ZGenScript(null, log);
    ZGenScript.ZbnfMainGenCtrl zbnfGenCtrl = new ZGenScript.ZbnfMainGenCtrl(script);
    parserGenCtrl2Java.setContent(ZGenScript.ZbnfMainGenCtrl.class, zbnfGenCtrl, parserGenCtrl.getFirstParseResult());
    
    //if(this.scriptclassMain ==null){
    //this.scriptclassMain = zbnfGenCtrl.scriptclass;
    //}
    if(zbnfGenCtrl.includes !=null){
      //parse includes after processing this file, because the zbnfGenCtrl.includes are not set before.
      //If one include contain a main, use it. But override the main after them, see below.
      for(String sFileInclude: zbnfGenCtrl.includes){
        File fileInclude = new File(fileParent, sFileInclude);
        if(!fileInclude.exists()){
          System.err.printf("TextGenScript - translateAndSetGenCtrl, included file not found; %s\n", fileInclude.getAbsolutePath());
          throw new FileNotFoundException("TextGenScript - translateAndSetGenCtrl, included file not found: " + fileInclude.getAbsolutePath());
        }
        File fileIncludeParent = FileSystem.getDir(fileInclude);
        int lengthBufferGenctrl = (int)fileInclude.length();
        StringPartScan spGenCtrlSub = new StringPartFromFileLines(fileInclude, lengthBufferGenctrl, "encoding", null);
        translateAndSetGenCtrl(parserGenCtrl, spGenCtrlSub, checkXmlOutput, fileIncludeParent, log);
      }
    }
    script.setFromIncludedScript(zbnfGenCtrl);
    return script;
  }
  
  

  
  
  protected static class CmdLine extends MainCmd
  {

    public final Args argData;

    protected final MainCmd.Argument[] argList =
    { new MainCmd.Argument("", "<INPUT>    jbat-File to execute"
        , new MainCmd.SetArgument(){ @Override public boolean setArgument(String val){ 
          if(argData.sFileScript == null){ argData.sFileScript = val; }
          else {argData.userArgs.add(val); }
          return true; 
        }})
    , new MainCmd.Argument("-t", ":<OUTEXT> text-File for output"
        , new MainCmd.SetArgument(){ @Override public boolean setArgument(String val){ 
          argData.sFileTextOut = val; return true;
        }})
    , new MainCmd.Argument("-debug", ":<TEST.xml> Test-File"
        , new MainCmd.SetArgument(){ @Override public boolean setArgument(String val){ 
          argData.fileTestXml = new File(val); return true;
        }})
    , new MainCmd.Argument("-u", "userargs"
        , new MainCmd.SetArgument(){ @Override public boolean setArgument(String val){ 
          argData.userArgs.add(val); 
          return true; 
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
      System.out.println(ZGenSyntax.syntax);
    }

  }
  
  
}
