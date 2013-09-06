package org.vishia.zbatch;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.vishia.cmd.JbatchExecuter;
import org.vishia.cmd.JbatchScript;
import org.vishia.mainCmd.MainCmd;
import org.vishia.mainCmd.MainCmdLogging_ifc;
import org.vishia.mainCmd.MainCmd_ifc;
import org.vishia.util.Assert;
import org.vishia.util.FileSystem;
import org.vishia.util.StringPart;
import org.vishia.util.StringPartFromFileLines;
import org.vishia.util.UnexpectedException;
import org.vishia.xmlSimple.SimpleXmlOutputter;
import org.vishia.xmlSimple.XmlException;
import org.vishia.xmlSimple.XmlNode;
import org.vishia.zbnf.ZbnfJavaOutput;
import org.vishia.zbnf.ZbnfParser;


public class Zbatch
{
  
  public static class Args{
    
    /**Cmdline-argument, set on -i option. Inputfile to to something. :TODO: its a example.*/
    String sFileScript;
    
    String sFileTextOut;
    
    File fileTestXml;
    
    List<String> userArgs = new ArrayList<String>();
  }
  

  protected final MainCmdLogging_ifc log;
  
  protected final Args args;
  
  protected final JbatchExecuter executer;
  
  
  public Zbatch(Args args, MainCmdLogging_ifc log){
    this.log = log;
    this.args = args;
    this.executer = new JbatchExecuter(log);
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
        Zbatch main = new Zbatch(args, mainCmdLine);     //the main instance
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
      executer.setScriptVariable("$" + nrArg, argu);
      nrArg +=1;
    }
    sError = generate(executer, fileIn, out, true, args.fileTestXml);
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
  public String generate(JbatchExecuter executer, File fileScript, Appendable out, boolean accessPrivate, File testOut){
    String sError = null;
    executer.setOutfile(out);
    executer.setScriptVariable("text", executer.outFile());
    JbatchScript genScript = null; //gen.parseGenScript(fileGenCtrl, null);
    try { genScript = translateAndSetGenCtrl(fileScript, testOut);
    } catch (Exception exc) {
      sError = exc.getMessage();
    }
    //genScript = parseGenScript(fileScript, testOut);
    if(sError == null) { // && out !=null){
      try{
        sError = executer.genContent(genScript, null, accessPrivate, out);
        //out.close();
      } catch(IOException exc){
        System.err.println(Assert.exceptionInfo("", exc, 0, 4));
      }
    }
    //String sError = generator.generate(zbnfResultData, fileScript, fOut, true);
    return sError;
  }
  
  

  public JbatchScript translateAndSetGenCtrl(File fileZbnf4GenCtrl, File fileGenCtrl, File checkXmlOut) 
  throws FileNotFoundException, IOException
    , ParseException, XmlException, IllegalArgumentException, IllegalAccessException, InstantiationException
  { log.writeInfoln("* Zbatch: parsing gen script \"" + fileZbnf4GenCtrl.getAbsolutePath() 
    + "\" with \"" + fileGenCtrl.getAbsolutePath() + "\"");

    int lengthBufferSyntax = (int)fileZbnf4GenCtrl.length();
    StringPart spSyntax = new StringPartFromFileLines(fileZbnf4GenCtrl, lengthBufferSyntax, "encoding", null);

    int lengthBufferGenctrl = (int)fileGenCtrl.length();
    StringPart spGenCtrl = new StringPartFromFileLines(fileGenCtrl, lengthBufferGenctrl, "encoding", null);

    File fileParent = FileSystem.getDir(fileGenCtrl);
    return translateAndSetGenCtrl(new StringPart(ZbatchSyntax.syntax), new StringPart(spGenCtrl), checkXmlOut, fileParent);
  }
  

  public JbatchScript translateAndSetGenCtrl(File fileGenCtrl) 
  throws FileNotFoundException, IllegalArgumentException, IllegalAccessException, InstantiationException, IOException, ParseException, XmlException 
  {
    return translateAndSetGenCtrl(fileGenCtrl, null);
  }
  
  
  public JbatchScript translateAndSetGenCtrl(File fileGenCtrl, File checkXmlOut) 
  throws FileNotFoundException, IllegalArgumentException, IllegalAccessException, InstantiationException, IOException, ParseException, XmlException 
  {
    int lengthBufferGenctrl = (int)fileGenCtrl.length();
    StringPart spGenCtrl = new StringPartFromFileLines(fileGenCtrl, lengthBufferGenctrl, "encoding", null);
    File fileParent = FileSystem.getDir(fileGenCtrl);
    return translateAndSetGenCtrl(new StringPart(ZbatchSyntax.syntax), new StringPart(spGenCtrl), checkXmlOut, fileParent);
  }
  
  
  public JbatchScript translateAndSetGenCtrl(String spGenCtrl) 
  throws IllegalArgumentException, IllegalAccessException, InstantiationException, ParseException 
  {
    try{ 
      return translateAndSetGenCtrl(new StringPart(ZbatchSyntax.syntax), new StringPart(spGenCtrl), null, null);
    } catch(IOException exc){ throw new UnexpectedException(exc); }
  }
  
  
  public JbatchScript translateAndSetGenCtrl(StringPart spGenCtrl) 
  throws ParseException, IllegalArgumentException, IllegalAccessException, InstantiationException 
  {
    try { 
      return translateAndSetGenCtrl(new StringPart(ZbatchSyntax.syntax), spGenCtrl, null, null);
    }catch(IOException exc){ throw new UnexpectedException(exc); }
  }
  
  
  
  /**Translate the generation control file - core routine.
   * It sets the {@link #zTextGenCtrl} aggregation. This routine must be called before  the script can be used.
   * There are some routines without the parameter sZbnf4GenCtrl, which uses the internal syntax. Use those if possible:
   * {@link #translateAndSetGenCtrl(File)}, {@link #translateAndSetGenCtrl(String)}
   * <br><br>
   * This routine will be called recursively if scripts are included.
   * 
   * @param sZbnf4GenCtrl The syntax. This routine can use a special syntax. The default syntax is {@link ZbatchSyntax#syntax}.
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
  public JbatchScript translateAndSetGenCtrl(StringPart sZbnf4GenCtrl, StringPart spGenCtrl, File checkXmlOutput, File fileParent) 
  throws ParseException, IllegalArgumentException, IllegalAccessException, InstantiationException, FileNotFoundException, IOException 
  { boolean bOk;
    ZbnfParser parserGenCtrl = new ZbnfParser(log); //console);
    parserGenCtrl.setSyntax(sZbnf4GenCtrl);
    if(log.getReportLevel() >= MainCmdLogging_ifc.fineInfo){
      log.reportln(MainCmdLogging_ifc.fineInfo, "== Syntax GenCtrl ==");
      parserGenCtrl.reportSyntax(log, MainCmdLogging_ifc.fineInfo);
    }
    log.writeInfo(" ... ");
    return translateAndSetGenCtrl(parserGenCtrl, spGenCtrl, checkXmlOutput, fileParent);
  }
    
    
    
    
  private JbatchScript translateAndSetGenCtrl(ZbnfParser parserGenCtrl, StringPart spGenCtrl
      , File checkXmlOutput, File fileParent) 
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

    JbatchScript script = new JbatchScript(null, log);
    JbatchScript.ZbnfMainGenCtrl zbnfGenCtrl = script.new ZbnfMainGenCtrl();
    parserGenCtrl2Java.setContent(JbatchScript.ZbnfMainGenCtrl.class, zbnfGenCtrl, parserGenCtrl.getFirstParseResult());
    
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
        StringPart spGenCtrlSub = new StringPartFromFileLines(fileInclude, lengthBufferGenctrl, "encoding", null);
        translateAndSetGenCtrl(parserGenCtrl, spGenCtrlSub, checkXmlOutput, fileIncludeParent);
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
      System.out.println(ZbatchSyntax.syntax);
    }

  }
  
  
}
