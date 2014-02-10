package org.vishia.zgen;

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

import org.vishia.cmd.ZGenExecuter;
import org.vishia.cmd.ZGenScript;
import org.vishia.mainCmd.MainCmd;
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
  
  
  public interface PreparerParsedData{
    void prepareParsedData(XmlNode zbnfResult, ZGenExecuter zgen);
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
    @Override public void prepareParsedData(XmlNode zbnfResult, ZGenExecuter zgen)
    { try{ zgen.setScriptVariable("xml", 'O', zbnfResult, true);
      } catch(Exception exc){
        throw new RuntimeException(exc);
      }
    }
  };
  
  
  /**Executes the parsing and translation to the destination files
   * @param args
   * @param userData If null then the xmlResultTree of {@link ZbnfParser#getResultTree()} is used as data to produce the output texts.
   *   If given then {@link ZbnfJavaOutput} is used to set its content with the parsers result. 
   *   If this instance is instance of {@link java.lang.Runnable} then its run method will be execute after filling.
   * @return
   * @throws IOException
   * @throws IllegalArgumentException
   * @throws IllegalAccessException
   * @throws InstantiationException
   * @throws XmlException 
   * @throws ParseException 
   */
  public boolean parseAndTranslate(Args args, Object userData, PreparerParsedData preparerParsedData) 
  throws IOException, IllegalArgumentException, IllegalAccessException, InstantiationException, ParseException, XmlException
  { boolean bOk = true;
    bOk = super.parseAndWriteXml();
    
    //XmlNodeSimple<ZbnfParseResultItem> resultTree = parser.getResultTree(); 
    XmlNode resultTree = parser.getResultTree(); 
    if(userData == null){
      userData = resultTree;
    } else {
      ZbnfJavaOutput parser2Java = new ZbnfJavaOutput(console);
      parser2Java.setContent(userData.getClass(), userData, parser.getFirstParseResult());
    }
    
    
    if(args.sFileOut !=null){
      OutputStreamWriter wrXml = new OutputStreamWriter(new FileOutputStream(args.sFileOut + "2.xml")); 
      SimpleXmlOutputter xmlOut = new SimpleXmlOutputter();
      xmlOut.write(wrXml, resultTree);
      wrXml.close();
    }
    
    if(bOk){
      File outData;
      if(args.sScriptCheck !=null){
        outData = new File(args.sScriptCheck);
      } else {
        outData = null;
      }
      for(Zbnf2Text.Out outArgs: args.listOut){
        File fOut = new File(outArgs.sFileOut);
        File fileScript = new File(outArgs.sFileScript);
        ZGen.Args argsZ = new ZGen.Args();
        //ZGen zbatch = new ZGen(argsZ, console);
        ZGenExecuter generator = new ZGenExecuter(console);
        if(outData !=null) {
          //outData.append("===================").append(outArgs.sFileScript);
        }

        File checkXmlOutGenCtrl = args.sCheckXmlOutput == null ? null : new File(args.sCheckXmlOutput + "_check.genctrl");
        //The generation script:
        ZGenScript genScript = ZGen.translateAndSetGenCtrl(fileScript, checkXmlOutGenCtrl, console);
        
        Writer out = new FileWriter(fOut);
        Map<String, DataAccess.Variable<Object>> scriptVariables;
        try{ 
          scriptVariables = generator.genScriptVariables(genScript, true, null);
        } catch(IOException exc){
          System.err.println("Zbnf2Text - unexpected IOexception while generation; " + exc.getMessage());
          scriptVariables = null;
        }
        preparerParsedData.prepareParsedData(resultTree, generator);
        try{ 

          CharSequence sError = generator.execute(genScript, true, true, out);
        } catch(Exception exc){
          CharSequence sMsg = Assert.exceptionInfo("Zmake - Exception; ", exc, 0, 10);
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

    /**List of pairs of scripts and output files. */
    public List<Out> listOut = new LinkedList<Out>();
    
    /**Only the current pair of script and output. It is added to listOut if filled. */
    public Out lastOut;
  }



  
  
  public static class Out{
    
    /**Cmdline-argument, set on -y: option. Outputfile to output something. */
    public String sFileOut = null;

    public String sFileScript = null;
  }



  /**The inner class CmdLineText helps to evaluate the command line arguments
   * and show help messages on command line.
   */
  public static class CmdLineText extends Zbnf2Xml.CmdLine
  {

    //Args cmdlineArgs;  
    
    protected final MainCmd.Argument[] argumentsZbnf2Text =
    { new MainCmd.Argument("-t", "<TEXTOUT> name of the output file to generate"
        , new MainCmd.SetArgument(){ @Override public boolean setArgument(String val){ 
          Args cmdlineArgs = (Args)CmdLineText.super.argData;
          if(cmdlineArgs.lastOut == null){ cmdlineArgs.lastOut = new Out(); cmdlineArgs.listOut.add(cmdlineArgs.lastOut); }
          cmdlineArgs.lastOut.sFileOut  = val;
          if(cmdlineArgs.lastOut.sFileScript !=null){ cmdlineArgs.lastOut = null; } //filled. 
          return true; }})
    , new MainCmd.Argument("-c", "<OUTCTRL> name of the output file to generate"
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
