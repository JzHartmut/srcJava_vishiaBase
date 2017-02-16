package org.vishia.zcmd;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;

import javax.script.ScriptException;

import org.vishia.cmd.JZtExecuter;
import org.vishia.cmd.JZtScript;
import org.vishia.jztcmd.JZtcmd;
import org.vishia.mainCmd.MainCmd;
import org.vishia.mainCmd.MainCmdLogging_ifc;
import org.vishia.mainCmd.MainCmd_ifc;
import org.vishia.util.Assert;
import org.vishia.xmlSimple.XmlException;
import org.vishia.xmlSimple.XmlNode;
import org.vishia.zbnf.Zbnf2Xml;
import org.vishia.zbnf.ZbnfJavaOutput;
import org.vishia.zbnf.ZbnfParseResultItem;
import org.vishia.zbnf.ZbnfParser;

/**This class reads any input text file with given syntay and creates a translated file from that data.
 * It uses a {@link JZtScript} to describe the text generation and the {@link JZtExecuter} to generate the text.
 * The translator from the generate control script is parsed with {@link JZtcmd} contained in this package.
 * The translation process is full script controlled. Therewith the user can define and change the translation 
 * without study the complexly parsing and translation algorithm in Java, only adapt the maybe given scripts or writing 
 * own scripts based on examples and the knowledge of the script syntax.
 * <pre>
 User                                       Internal                             User 
 input data --------> ZBNF parser --------> Java data -----> JZcmdExecuter ----> output data
 (textual)                 ^                 image               ^               usual textual
 ==========                |                                     |               =============s
                      ZBNF syntax                            JZcmdScript
                      script                                     ^
                      ==========                                 |
                                                                 |
                                            JZcmd   -------> JZcmd using
                                            generation       ZBNF parser
                                            script
                                            ==========
 * </pre>
 * @author hartmut
 *
 */
public class Zbnf2Text extends Zbnf2Xml
{
  
  /**Version, history and license.
   * <ul>
   * <li>2014-02-16 Hartmut chg: Argument -currdir=PATH for command line invocation {@link #main(String[])}
   * <li>2013-10-27 Hartmut chg: {@link #jbatch(String, org.vishia.cmd.JZtExecuter.ExecuteLevel)}
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
  @SuppressWarnings("hiding")
  static final public String sVersion = "2014-02-16";

  public interface PreparerParsedData{
    void prepareParsedData(XmlNode xmlResult, ZbnfParseResultItem zbnfResult, JZtScript zgenscript, JZtExecuter zgen);
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
    boolean bOk;
    try{ bOk = mainCmdLine.parseArguments(); }
    catch(Exception exception)
    { mainCmdLine.report("Argument error:", exception);
      mainCmdLine.setExitErrorLevel(MainCmdLogging_ifc.exitWithArgumentError);
      mainCmdLine.writeHelpInfo(null);
      bOk = false;
    }
    if(bOk)
    { /** The execution class knows the SampleCmdLine Main class in form of the MainCmd super class
          to hold the contact to the command line execution.
      */
      Zbnf2Text main = new Zbnf2Text(arg, mainCmdLine);     //the main instance
      try{ main.parseAndTranslate(arg, null, main.setZbnfResult); }
      catch(Exception exception)
      { //catch the last level of error. No error is reported direct on command line!
        main.logmaincmd.report("Uncatched Exception on main level:", exception);
        exception.printStackTrace(System.out);
        main.logmaincmd.setExitErrorLevel(MainCmdLogging_ifc.exitWithErrors);
      }
    }
    mainCmdLine.exit();
  }

  
  public Zbnf2Text(Args args, MainCmd_ifc mainCmdLine){
    super(args, mainCmdLine);
    this.console = mainCmdLine;  //it may be also another instance based on MainCmd_ifc
    
  }
  
  
  PreparerParsedData setZbnfResult = new PreparerParsedData(){
    @Override public void prepareParsedData(XmlNode xmlResult, ZbnfParseResultItem zbnfResult, JZtScript zgenscript, JZtExecuter zgen)
    { try{ 
        zgen.initialize(zgenscript, true, console.currdir());
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
        JZtExecuter generator = new JZtExecuter(console);
        if(outData !=null) {
          //outData.append("===================").append(outArgs.sFileScript);
        }
        File checkXmlOutGenCtrl = args.sCheckXmlOutput == null ? null : new File(args.sCheckXmlOutput + "_check.genctrl");
        Writer out = new FileWriter(fOut);
        try{ 
          //The generation script:
          //
          JZtScript genScript = JZtcmd.translateAndSetGenCtrl(fileScript, checkXmlOutGenCtrl, console);
          //
          //preparation as callback:
          //
          preparerParsedData.prepareParsedData(resultTree, zbnfResult, genScript, generator);
          //
          //
          generator.execute(genScript, true, true, out, console.currdir());
          //
        } catch(ScriptException exc){
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
    
    /**If this file is given the parsed data will be preseted by either a text, html or xml output file
     * how they are parsed, without the interpretation of the text generation.
     */
    public String sFileSrcData = null;
    

    
    /**Path without extension of a file which outputs the text generation scripts parse result and the Zmake input parse result. 
     * */
    public String sCheckXmlOutput = null;

    
    /**List of pairs of scripts and output files. */
    public List<Out> listOut = new LinkedList<Out>();
    
    /**Only the current pair of script and output. It is added to listOut if filled. */
    public Out lastOut;
    
    /**Adds a pair of JZcmd generation files.
     * @param sFileScript The script to generate
     * @param sFileOut the produced outfile.
     */
    public void addGenOut(String sFileScript, String sFileOut){
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
    { new MainCmd.Argument("-checkdata", "=CHECK.html if given then a html file for debugging will be written"
        , new MainCmd.SetArgument(){ @Override public boolean setArgument(String val){ 
          Args cmdlineArgs = (Args)CmdLineText.super.argData;
          cmdlineArgs.sFileSrcData = val; 
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
    
    
    public CmdLineText(Args arg, String[] args) {
      super(arg, args);
      super.addHelpInfo("args: -i:<INPUT> -s:<SYNTAX> [-[x|y|z]:<OUTPUT>] [{-t:<TEXTFILE> -c<JZcmd_CTRLFILE>}]");  //[-w[+|-|0]]
      super.addHelpInfo("==Arguments of Zbnf2Xml==");
      super.addArgument(argumentsZbnf2Xml);
      super.addHelpInfo("==Additional arguments of Zbnf2Text==");
      super.addArgument(argumentsZbnf2Text);
      super.addHelpInfo("Syntax of a JZcmd_CTRLFILE: It is the JZcmd syntax, see JZcmd --help or www.vishia.org/ZBNF/sf/docu/JZcmd.html");
      super.addHelpInfo("==Standard arguments of MainCmd==");
      super.addStandardHelpInfo();
    } 
    
    
        /**Checks the cmdline arguments relation together.
       If there is an inconsistents, a message should be written. It may be also a warning.
       @return true if successfull, false if failed.
    */
    @Override
    protected boolean checkArguments()
    { boolean bOk = true;
  
      if(argData.sFileIn == null)            { bOk = false; writeError("ERROR argument -iInputfile is obligat."); }
      else if(argData.sFileIn.length()==0)   { bOk = false; writeError("ERROR argument -iInputfile without content.");}
  
      if(argData.sFileSyntax == null)            { bOk = false; writeError("ERROR argument -sSyntaxfile is obligat."); }
      else if(argData.sFileSyntax.length()==0)   { bOk = false; writeError("ERROR argument -sSyntaxfile without content.");}
  
      if(!bOk) setExitErrorLevel(exitWithArgumentError);
  
      return bOk;
  
   }

    
  }


}
