package org.vishia.zcmd;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.vishia.cmd.JZcmdExecuter;
import org.vishia.cmd.JZcmdScript;
import org.vishia.mainCmd.MainCmd;
import org.vishia.mainCmd.MainCmdLoggingStream;
import org.vishia.mainCmd.MainCmd_ifc;
import org.vishia.util.Assert;
import org.vishia.util.DataAccess;
import org.vishia.xmlSimple.SimpleXmlOutputter;
import org.vishia.xmlSimple.XmlException;
import org.vishia.xmlSimple.XmlNode;
import org.vishia.zbnf.Zbnf2Xml;
import org.vishia.zbnf.ZbnfJavaOutput;
import org.vishia.zbnf.ZbnfParseResultItem;
import org.vishia.zbnf.ZbnfParser;

public class Zbnf2Text extends Zbnf2Xml
{
  
  /**Version, history and license.
   * <ul>
   * <li>2014-02-16 Hartmut chg: Argument -currdir=PATH for command line invocation {@link #main(String[])}
   * <li>2013-10-27 Hartmut chg: {@link #jbatch(String, org.vishia.cmd.JZcmdExecuter.ExecuteLevel)}
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
  static final public String sVersion = "2014-02-16";

  public interface PreparerParsedData{
    void prepareParsedData(XmlNode xmlResult, ZbnfParseResultItem zbnfResult, JZcmdScript zgenscript, JZcmdExecuter zgen);
  }
  

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
      mainCmdLine.writeHelpInfo();
      bOk = false;
    }
    if(bOk)
    { /** The execution class knows the SampleCmdLine Main class in form of the MainCmd super class
          to hold the contact to the command line execution.
      */
      try{ main.parseAndTranslate(arg, null, main.setZbnfResult); }
      catch(Exception exception)
      { //catch the last level of error. No error is reported direct on command line!
        main.report.report("Uncatched Exception on main level:", exception);
        exception.printStackTrace(System.out);
        main.report.setExitErrorLevel(MainCmd_ifc.exitWithErrors);
      }
    }
    mainCmdLine.exit();
  }

  
  public Zbnf2Text(Args args, MainCmd_ifc mainCmdLine){
    super(args, mainCmdLine);
    this.console = mainCmdLine;  //it may be also another instance based on MainCmd_ifc
    
  }
  
  
  PreparerParsedData setZbnfResult = new PreparerParsedData(){
    @Override public void prepareParsedData(XmlNode xmlResult, ZbnfParseResultItem zbnfResult, JZcmdScript zgenscript, JZcmdExecuter zgen)
    { try{ 
        Zbnf2Text.Args argsZtext = (Zbnf2Text.Args)Zbnf2Text.this.argsx;
        zgen.genScriptVariables(zgenscript, true, null, argsZtext.sCurrdir);
        zgen.setScriptVariable("data", 'O', xmlResult, true);
      } catch(Throwable exc){
        System.err.println("Zbnf2Text - unexpected IOexception while generation; " + exc.getMessage());
        throw new RuntimeException(exc);
      }
    }
  };
  
  
  /**Executes the parsing and translation to the destination files
   * @param args Zbnf2Text command line calling arguments.
   * @param userData If null then the xmlResultTree of {@link ZbnfParser#getResultTree()} is used as data to produce the output texts.
   *   If given then {@link ZbnfJavaOutput} is used to set its content with the parsers result. 
   *   If this instance is instance of {@link java.lang.Runnable} then its run method will be execute after filling.
   * @param preparerParsedData This is a callback which is called after generating the script variable
   *   and before execution the script. The user can set additional script variables before execution
   *   or can use script variables to get its content for any userData evaluation. 
   * @return
   * @throws IOException
   * @throws IllegalArgumentException
   * @throws IllegalAccessException
   * @throws InstantiationException
   * @throws ParseException
   * @throws XmlException
   */
  public boolean parseAndTranslate(Args args, Object userData, PreparerParsedData preparerParsedData) 
  throws IOException, IllegalArgumentException, IllegalAccessException, InstantiationException, ParseException, XmlException
  { boolean bOk = true;
    bOk = super.parseAndWriteXml();
    
    if(bOk){

      //XmlNodeSimple<ZbnfParseResultItem> resultTree = parser.getResultTree(); 
      XmlNode resultTree = parser.getResultTree(); 
      ZbnfParseResultItem zbnfResult = parser.getFirstParseResult();
      if(userData != null){
        ZbnfJavaOutput parser2Java = new ZbnfJavaOutput(console);
        parser2Java.setContent(userData.getClass(), userData, zbnfResult);
      }
    
      File outData;
      if(args.sScriptCheck !=null){
        outData = new File(args.sScriptCheck);
      } else {
        outData = null;
      }
      for(Zbnf2Text.Out outArgs: args.listOut){
        File fOut = new File(outArgs.sFileOut);
        File fileScript = new File(outArgs.sFileScript);
        JZcmd.Args argsZ = new JZcmd.Args();
        //JZcmd zbatch = new JZcmd(argsZ, console);
        JZcmdExecuter generator = new JZcmdExecuter(console);
        if(outData !=null) {
          //outData.append("===================").append(outArgs.sFileScript);
        }
        File checkXmlOutGenCtrl = args.sCheckXmlOutput == null ? null : new File(args.sCheckXmlOutput + "_check.genctrl");
        //The generation script:
        //
        JZcmdScript genScript = JZcmd.translateAndSetGenCtrl(fileScript, checkXmlOutGenCtrl, console);
        //
        //preparation as callback:
        //
        preparerParsedData.prepareParsedData(resultTree, zbnfResult, genScript, generator);
        //
        Writer out = new FileWriter(fOut);
        try{ 
          //
          generator.execute(genScript, true, true, out, args.sCurrdir);
          //
        } catch(Throwable exc){
          CharSequence sMsg = Assert.exceptionInfo("Zbnf2Text - Exception; ", exc, 0, 10);
          System.err.println(sMsg);
        }
        out.close();
      }
      if(outData !=null) {
        //outData.close();
      }
    }
    return bOk;
  }
  
  
  
  /**This class holds the gotten arguments from command line. 
   * It is possible that some more operations with arguments as a simple get process.
   * The arguments may got also from configuration files named in cmd line arguments or other.
   * The user of arguments should know (reference) only this instance, not all other things arround MainCmd,
   * it it access to arguments. 
   */
  public static class Args extends Zbnf2Xml.Args
  {
    
    /**File name for a file to check the script. It contains the content of the script after parsing. */
    public String sScriptCheck = null;
    
    /**Path without extension of a file which outputs the text generation scripts parse result and the Zmake input parse result. 
     * */
    public String sCheckXmlOutput = null;

    
    /**If given the absolute or maybe relative path for the start currdir.
     * This value is used as absolute start path for a <code>currdir = < objExpr?path></code> - assignment in the script.
     */
    public String sCurrdir;
    
    /**List of pairs of scripts and output files. */
    public List<Out> listOut = new LinkedList<Out>();
    
    /**Only the current pair of script and output. It is added to listOut if filled. */
    public Out lastOut;
    
    /**Adds a pair of JZcmd generation files.
     * @param sFileScript The script to generate
     * @param sFileOut the produced outfile.
     */
    public void addJZcmd(String sFileScript, String sFileOut){
      Out out = new Out();
      out.sFileOut = sFileOut;
      out.sFileScript = sFileScript;
      listOut.add(out);
    }
    
    
  }



  
  
  public static class Out{
    
    /**Cmdline-argument, set on -y: option. Outputfile to output something. */
    public String sFileOut = null;

    public String sFileScript = null;

    @Override public String toString(){ return sFileScript + "==>" + sFileOut; }
  }



  /**The inner class CmdLineText helps to evaluate the command line arguments
   * and show help messages on command line.
   */
  public static class CmdLineText extends Zbnf2Xml.CmdLine
  {

    //Args cmdlineArgs;  
    
    protected final MainCmd.Argument[] argumentsZbnf2Text =
    { new MainCmd.Argument("-currdir", ":<PATH> path of the current dir. If file-path, then it's dir."
        , new MainCmd.SetArgument(){ @Override public boolean setArgument(String val){ 
          Args cmdlineArgs = (Args)CmdLineText.super.argData;
          cmdlineArgs.sCurrdir = val;
          return true; }})
    , new MainCmd.Argument("-checkzcmd", "=CHECK  if given then 3 files for debugging will be written"
        , new MainCmd.SetArgument(){ @Override public boolean setArgument(String val){ 
          Args cmdlineArgs = (Args)CmdLineText.super.argData;
          cmdlineArgs.sCheckXmlOutput = val; 
          return true; }})
    , new MainCmd.Argument("-t", ":<TEXTOUT> name of the output file to generate"
        , new MainCmd.SetArgument(){ @Override public boolean setArgument(String val){ 
          Args cmdlineArgs = (Args)CmdLineText.super.argData;
          if(cmdlineArgs.lastOut == null){ cmdlineArgs.lastOut = new Out(); cmdlineArgs.listOut.add(cmdlineArgs.lastOut); }
          cmdlineArgs.lastOut.sFileOut  = val;
          if(cmdlineArgs.lastOut.sFileScript !=null){ cmdlineArgs.lastOut = null; } //filled. 
          return true; }})
    , new MainCmd.Argument("-c", ":<OUTCTRL> name of the generate script for the output file"
        , new MainCmd.SetArgument(){ @Override public boolean setArgument(String val){ 
          Args cmdlineArgs = (Args)CmdLineText.super.argData;
          if(cmdlineArgs.lastOut == null){ cmdlineArgs.lastOut = new Out(); cmdlineArgs.listOut.add(cmdlineArgs.lastOut); }
          cmdlineArgs.lastOut.sFileScript  = val;
          if(cmdlineArgs.lastOut.sFileOut !=null){ cmdlineArgs.lastOut = null; } //filled. 
          return true; }})
    , new MainCmd.Argument("  ", "-c... -t... more as one output file with this pair of arguments", null)
    };
    
    
    protected CmdLineText(Args arg, String[] args) {
      super(arg, args);
      super.addArgument(argumentsZbnf2Xml);
      super.addArgument(argumentsZbnf2Text);
      //cmdlineArgs = new Args();
      //super.addHelpInfo("-c:<OUTCTRL> name of a file to control the output");
      //super.addHelpInfo("-t:<TEXTOUT> name of the output file to generate");
      //super.addHelpInfo("-c:<TEXTOUT> -t:<TEXTOUT> more as one output file with this pair of arguments");
    } 
    
    
    
    
    public boolean XXXtestArgsZbnf2Text(String argc, int nArg){
      boolean bOk = true;
      if(argc.startsWith("-t:")) {
        Args cmdlineArgs = (Args)super.argData;
        if(cmdlineArgs.lastOut == null){ cmdlineArgs.lastOut = new Out(); cmdlineArgs.listOut.add(cmdlineArgs.lastOut); }
        cmdlineArgs.lastOut.sFileOut  = getArgument(3);
        if(cmdlineArgs.lastOut.sFileScript !=null){ cmdlineArgs.lastOut = null; } //filled. 
      }
      else if(argc.startsWith("-c:")) {
        Args cmdlineArgs = (Args)super.argData;
        if(cmdlineArgs.lastOut == null){ cmdlineArgs.lastOut = new Out(); cmdlineArgs.listOut.add(cmdlineArgs.lastOut); }
        cmdlineArgs.lastOut.sFileScript  = getArgument(3);
        if(cmdlineArgs.lastOut.sFileOut !=null){ cmdlineArgs.lastOut = null; } //filled. 
      }
      else bOk = false;
      return bOk;
    }
  }


}
