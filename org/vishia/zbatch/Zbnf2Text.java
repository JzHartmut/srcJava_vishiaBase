package org.vishia.zbatch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;

import org.vishia.mainCmd.MainCmd;
import org.vishia.mainCmd.MainCmd_ifc;
import org.vishia.xmlSimple.SimpleXmlOutputter;
import org.vishia.xmlSimple.XmlNode;
import org.vishia.zbnf.Zbnf2Xml;
import org.vishia.zbnf.ZbnfJavaOutput;
import org.vishia.zbnf.ZbnfParser;

public class Zbnf2Text extends Zbnf2Xml
{

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
      try{ main.parseAndTranslate(arg, null); }
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
   */
  public boolean parseAndTranslate(Args args, Object userData) 
  throws IOException, IllegalArgumentException, IllegalAccessException, InstantiationException
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
        ZbatchExecuter generator = new ZbatchExecuter(console);
        if(outData !=null) {
          //outData.append("===================").append(outArgs.sFileScript);
        }
        Writer out = new FileWriter(fOut);
        String sError = generator.generate(resultTree, fileScript, out, true, outData);
        out.close();
        if(sError !=null){
          console.writeError(sError);
        } else {
          console.writeInfoln("SUCCESS outfile: " + fOut.getAbsolutePath());
        }
      }
      if(outData !=null) {
        //outData.close();
      }
    }
    return bOk;
  }
  
  
  
  /**This class holds the got arguments from command line. 
   * It is possible that some more operations with arguments as a simple get process.
   * The arguments may got also from configuration files named in cmd line arguments or other.
   * The user of arguments should know (reference) only this instance, not all other things arround MainCmd,
   * it it access to arguments. 
   */
  public static class Args extends Zbnf2Xml.Args
  {
    public String sScriptCheck = null;
    
    public List<Out> listOut = new LinkedList<Out>();
    
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
    
    private final MainCmd.Argument[] arguments =
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
    , new MainCmd.Argument("", "-c... -t... more as one output file with this pair of arguments", null)
    };
    
    
    protected CmdLineText(Args arg, String[] args) {
      super(arg, args);
      super.addArgument(arguments);
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
