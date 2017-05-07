package org.vishia.docuGen;

//==JZcmd==
//JZcmd currdir = "D:/vishia/ZBNF/examples_XML/DocuGenerationViaXML/docuSrc";
//JZcmd java org.vishia.docuGen.DocuGen.main("D:/vishia/ZBNF/examples_XML/DocuGenerationViaXML/docuSrc/_genDocu_docu.bat", "-zbnfjax:D:/vishia/ZBNF/zbnfjax/");
//==endJZcmd==

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.script.ScriptException;

import org.vishia.cmd.JZtxtcmdExecuter;
import org.vishia.jztxtcmd.JZtxtcmd;
import org.vishia.mainCmd.MainCmd;
import org.vishia.mainCmd.MainCmd_ifc;
import org.vishia.util.DataShow;
import org.vishia.util.FilePath;
import org.vishia.util.FileSystem;
import org.vishia.zbnf.ZbnfJavaOutput;
import org.vishia.zcmd.Zbnf2Text;





public class DocuGen
{
  /**Version, history and license.
   * <ul>
   * <li>2015-12-05 Hartmut new: Created, with schema of StateMcHgen. It should be the main class for documentation generation
   *   using JZcmd instead XSLT and ANT.
   * </ul>
   * 
   * <b>Copyright/Copyleft</b>:
   * For this source the LGPL Lesser General Public License, published by the Free Software Foundation is valid.
   * It means:
   * <ol>
   * <li> You can use this source without any restriction for any desired purpose.
   * <li> You can redistribute copies of this source to everybody.
   * <li> Every user of this source, also the user of redistribute copies
   *    with or without payment, must accept this license for further using.
   * <li> But the LPGL is not appropriate for a whole software product,
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
  static final public String sVersion = "2015-12-05";
  
  /** Aggregation to the Console implementation class.*/
  final MainCmd_ifc console;

  
  public static class Args {
    public String sDirZbnfJax;
    public String sCurrDir;
    public String sCtrlFile;
    public String sCheckDataHtml;
  }
  
  
  public static class CmdLine extends MainCmd
  {

    Args cmdlineArgs;  
    
    protected final MainCmd.Argument[] arglist1 =
    { new MainCmd.Argument("-checkdata", "=CHECK.html if given then a html file for debugging will be written"
        , new MainCmd.SetArgument(){ @Override public boolean setArgument(String val){ 
          cmdlineArgs.sCheckDataHtml = val; 
          return true; }})
    , new MainCmd.Argument("-zbnfjax", ":<Path> to the script directory zbnfjax"
        , new MainCmd.SetArgument(){ @Override public boolean setArgument(String val){ 
          cmdlineArgs.sDirZbnfJax = val; 
          return true; }})
    , new MainCmd.Argument("", " <Path> to the docu generation control file. Its directory is the current directory"
        , new MainCmd.SetArgument(){ @Override public boolean setArgument(String val){ 
          String path = FileSystem.absolutePath(val, null);
          int posFile = path.lastIndexOf('/');
          cmdlineArgs.sCurrDir = path.substring(0, posFile);
          cmdlineArgs.sCtrlFile = path.substring(posFile+1);
          return true; }})
    };
    
    
    public CmdLine(Args arg, String[] args) {
      super(args);  //arg, args);
      cmdlineArgs = arg;
      for(Argument arg2: arglist1){
        this.argList.add(arg2);
      }
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
  
      if(cmdlineArgs.sCtrlFile == null)            { bOk = false; writeError("ERROR argument -iInputfile is obligate."); }
      if(!bOk) setExitErrorLevel(exitWithArgumentError);
  
      return bOk;
  
   }

  }
  

  /** main started from java*/
  public static void main(String [] args)
  { Args cmdlineArgs = new Args();     //holds the command line arguments.
    CmdLine mainCmdLine = new CmdLine(cmdlineArgs, args); //the instance to parse arguments and others.
    boolean bOk = true;
    try{ mainCmdLine.parseArguments(); }
    catch(Exception exception)
    { mainCmdLine.setExitErrorLevel(MainCmd_ifc.exitWithArgumentError);
      bOk = false;
    }
    if(bOk)
    { /**Now instantiate the main class. 
       * It is possible to create some aggregates (final references) first outside depends on args.
       * Therefore the main class is created yet here.
       */
      DocuGen main = new DocuGen(mainCmdLine);
      /** The execution class knows the Main class in form of the MainCmd super class
          to hold the contact to the command line execution.
      */
      try
      { main.execute(cmdlineArgs); 
      }
      catch(Exception exception)
      { //catch the last level of error. No error is reported direct on command line!
        main.console.report("Uncatched Exception on main level:", exception);
        main.console.setExitErrorLevel(MainCmd_ifc.exitWithErrors);
      }
    }
    mainCmdLine.exit();
  }

  
  public DocuGen(MainCmd_ifc console)
  {
    this.console = console;
  }
  
  

   
  void execute(Args args) throws IOException, IllegalAccessException
  {
    ZbnfDocuGenCtrl zsrcData = parseAndStoreInput(args);  //parsed and converts into Java data presentation
    if(zsrcData != null){
      prepareDocuGenCtrl(zsrcData);
      if(args.sCheckDataHtml!=null) {
        Writer out  = new FileWriter(args.sCheckDataHtml);
        DataShow.outHtml(zsrcData, out);
        out.close();
      }
      

    } else {
      console.writeInfoln("ERROR");
      
    }
  }
  
  
  private void prepareDocuGenCtrl(ZbnfDocuGenCtrl zsrcData)
  {
    
  }
      
  
  
  
  
  /**This method reads the input script, parses it with ZBNF, 
   * stores all results in the Java-class {@link ZbnfResultData} 
   */
  private ZbnfDocuGenCtrl parseAndStoreInput(Args args)
  {
    /**The instance to store the data of parsing result is created locally and private visible: */
    ZbnfDocuGenCtrl zbnfResultData = new ZbnfDocuGenCtrl();
    /**This call processes the whole parsing and storing action: */
    File fileIn = new File(args.sCurrDir + "/" + args.sCtrlFile);
    File fileSyntax = new File(args.sDirZbnfJax + "/zdocu/DocuGenCtrl.zbnf");
    String sError = ZbnfJavaOutput.parseFileAndFillJavaObject(zbnfResultData.getClass(), zbnfResultData, fileIn, fileSyntax, console, 1200);
    if(sError != null)
    { /**there is any problem while parsing, report it: */
      console.writeError("ERROR Parsing file: " + fileIn.getAbsolutePath() + "\n" + sError);
      return null;
    }
    else
    { console.writeInfoln("SUCCESS parsed: " + fileIn.getAbsolutePath());
      return zbnfResultData;
    }
  
  }
  
  /**This is the root class for the Zbnf parsing result for the docuGenCtrl script file.
   * It refers to the main syntax component.
   */
  public static class ZbnfDocuGenCtrl //extends ZbnfState
  {
  
    /**ZBNF: genCtrl: <*\ ;?@file> ; */ 
    public String file;
    
    /**ZBNF: [<?hrefCtrl> hrefCtrl:  ; */ 
    public String hrefCtrl;
    
    
    
    public List<Target> target = new ArrayList<Target>();
    
    public Map<String, Document> documents = new TreeMap<String, Document>();
    
    public Map<String, HyperlinkAssociation> hyperlinkAssociations;
 
    public void set_hrefCtrl(String val) {
      hrefCtrl = val;
    }
    
    public Document new_document(){ return new Document(); }
    
    public void add_document(Document val){  
      String key = val.ident !=null ? val.ident : "???";
      documents.put(key, val);
    }

    public HyperlinkAssociation new_HyperlinkAssociation(){ return new HyperlinkAssociation(); }
    
    public void add_HyperlinkAssociation(HyperlinkAssociation val) { 
      if(hyperlinkAssociations == null){ hyperlinkAssociations = new TreeMap<String, HyperlinkAssociation>(); }
      hyperlinkAssociations.put(val.href, val); 
    }
    

  }

  
  
  
  public static class Target
  {
    public String target;
    
    public String translator;
    
    public FilePath output;
    
    public FilePath srcpath;
    
    public List<FilePath> inputs = new ArrayList<FilePath>();
    
    public void set_output(String val){
      output = new FilePath(val);
    }
    
    
    public void set_srcpath(String val){
      srcpath = new FilePath(val);
    }
    
    
    public void set_inputfile(String val) {
      FilePath input = new FilePath(val);
      inputs.add(input);
    }
    
  }

  
  
  
  public static class Document 
  {
    public String title;
    
    public String ident;
    
    public FilePath outHtml, outWord, cssHtml;
    
    public List<DocuInput> inputs = new ArrayList<DocuInput>();
    
    public List<Content> content = new ArrayList<Content>();

    public String crossRef;
    
    /**ZBNF: set from file::=<""?filePath>|<*\ \r\n,)?filePath> after new_OutHtml() etc. */
    public String filePath;
    
    public Map<String, HyperlinkAssociation> hyperlinkAssociations;
 
    public Map<String, Inset> insets;
 
    
    
    public Document new_outHtml(){ return this; }
    
    public void set_outHtml(Document val){ outHtml = new FilePath(filePath); }
    
    public Document new_outWord(){ return this; }
    
    public void set_outWord(Document val){ outWord = new FilePath(filePath); }
    
    public void set_cssHtml(String val){ cssHtml = new FilePath(val); }
    
    public DocuInput new_input(){ return new DocuInput(); }
    
    public void add_input(DocuInput val){ inputs.add(val); }
    
    public Topic new_topictree(){ Topic ret = new Topic(); ret.bTree = true; return ret; }
    
    public void add_topictree(Topic val){ content.add(val); }
       
    public Topic new_topic(){ Topic ret = new Topic(); ret.bTree = false; return ret; }
    
    public void add_topic(Topic val){ content.add(val); }
       
    public void set_crossRef(String val){ crossRef = val; }
    
    public HyperlinkAssociation new_HyperlinkAssociation(){ return new HyperlinkAssociation(); }
    
    public void add_HyperlinkAssociation(HyperlinkAssociation val) { 
      if(hyperlinkAssociations == null){ hyperlinkAssociations = new TreeMap<String, HyperlinkAssociation>(); }
      hyperlinkAssociations.put(val.href, val); 
    }
    
    public Inset new_inset(){ return new Inset(); }
    
    public void add_inset(Inset val) { 
      if(insets == null){ insets = new TreeMap<String, Inset>(); }
      insets.put(val.label, val); 
    }
    
  }
  
  
  
  public static class DocuInput
  {
    
    FilePath inputFile;
    
    public void set_inputfile(String val){ inputFile = new FilePath(val); }
    
  }
 
  
  
  
  public static class Content
  {
    
  }
  
  
  
  public static class Topic extends Content
  {
    boolean bTree = false;
    
    /**ZBNF: <*,)\ ?@select>. It is the selection String in all input topics. */
    public String select;
  }
  
  
  
  
  public static class Inset
  {
    public String label, topic;
  }
 
  
  public static class HyperlinkAssociation
  {
    public String href, dst, content;
  }
  
  
  
  
}
