package org.vishia.odg;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.vishia.cmd.JZtxtcmdTester;
import org.vishia.odg.data.XmlForOdg;
import org.vishia.odg.data.XmlForOdg_Zbnf;
import org.vishia.util.Arguments;
import org.vishia.util.Debugutil;
import org.vishia.xmlReader.GenXmlCfgJavaData;
import org.vishia.xmlReader.XmlCfg;
import org.vishia.xmlReader.XmlJzCfgAnalyzer;
import org.vishia.xmlReader.XmlJzReader;
import org.vishia.xmlReader.XmlNodeSimpleReader;
import org.vishia.xmlSimple.XmlNodeSimple;

public class ReadFbcl_odgData {
  
  /**Version, License and History:
   * <ul>
   * <li>TODO first example, output yet only to console, not in -o:file
   * <li>2022-06-29 created.
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
   */
  public static final String version = "2022-05-29";

  XmlJzCfgAnalyzer cfgAnalyzer = new XmlJzCfgAnalyzer();  //the root node for reading config

  
  /**Analyzes the given XML file example and creates a proper xmlcfg.xml file from that.
   * @param fXmlIn The example file. The xmlCfg will be write beside as xmlcfg.xml
   * @throws IOException
   */
  public void analyzeXmlStruct(File fXmlIn) throws IOException {
    File fXmlCfg = new File("T:/xmlcfg.xml");
    this.cfgAnalyzer.readXmlStruct(fXmlIn);
    this.cfgAnalyzer.writeCfgTemplate(fXmlCfg);
    Debugutil.stop();
  }

  
  
  /**Generates Java source files to store the data due to xmlcfg.xml.
   * It calls immediately {@link GenXmlCfgJavaData#smain(String[])}
   * with here set arguments.
   */
  public void genJavaData() {
    String[] args = 
      { "-cfg:D:/vishia/spe/SPE-card/FPGA/src/main/oodg/xmlcfg.xml"
      , "-dirJava:" + Arguments.replaceEnv("$(TMP)/test_XmlOdg/Java")
      , "-pkg:org.vishia.odg.data"
      , "-class:XmlForOdg"
      };
      
    GenXmlCfgJavaData.smain(args);
  }
  
  
  
  
  public void parseExample(Args args) {

    
//    File testFileodg = new File("D:/vishia/spe/SPE-card/FPGA/src/main/oodg/moduls_SpeA_a.odg");
//    File fout1 = new File("T:/testXmlRead-a.xml");
//    File foutData = new File("T:/testXmlRead-data.html");
    
    
      try {
        
        XmlNodeSimpleReader xmlNodeSimpleReader = new XmlNodeSimpleReader();
        XmlNodeSimple<Object> rootroot = new XmlNodeSimple<Object>("rootroot");
        XmlCfg xmlCfg = XmlNodeSimpleReader.newCfgXmlNodeSimple();
        XmlJzReader xmlReader = new XmlJzReader();
        xmlReader.setNamespaceEntry("xml", "XML");         // it is missing
//        xmlReader.openXmlTestOut(fout1);
//        xmlReader.readXml(testFile, rootroot, xmlCfg);

        //xmlReader.setDebugStopTag("xmlinput:subtree");        
        XmlCfg cfg = xmlReader.readCfgFromJar(XmlForOdg.class, "odgxmlcfg.xml"); //(new File("D:/vishia/spe/SPE-card/FPGA/src/main/oodg/xmlcfg.xml"));
        JZtxtcmdTester.dataHtml(cfg, new File("T:/oodg_xmlcfg.html"));
        xmlReader.setNamespaceEntry("xml", "XML");         // it is missing
        XmlForOdg_Zbnf data = new XmlForOdg_Zbnf();
        xmlReader.setDebugStopTag("text:span");
        xmlReader.openXmlTestOut(args.fOutStruct); //fout1);
        xmlReader.readZipXml(args.fIn, "content.xml", data);
        if(args.fOutDataHtml !=null) {
          JZtxtcmdTester.dataHtml(data.dataXmlForOdg, args.fOutDataHtml);
        }
        XmlForOdg odg = data.dataXmlForOdg;
        odg.prepareData();
        for(Map.Entry<String, XmlForOdg.Module> emdl: odg.idxAllModule.entrySet()) {
          System.out.print(emdl.getValue().showMdlAggregations());
        }
        Debugutil.stop();
        //System.out.println(data.get_BillOfMaterial().toString()); //set breakpoint here to view data
      } catch (IOException | NoSuchFieldException e) {
        e.printStackTrace();
      }

  }
  
  
  
  
  
  public static void main(String[] sArgs) {
    Args args = new Args();
    try {
      if(sArgs.length ==0) {
        args.showHelp(System.out);
        System.exit(1);                // no arguments, help is shown.
      }
      if(  ! args.parseArgs(sArgs, System.err)
        || ! args.testArgs(System.err)
        ) { 
        System.exit(2);                // argument error
      }
      ReadFbcl_odgData main = new ReadFbcl_odgData();
//      main.analyzeXmlStruct(new File("d:\\vishia\\spe\\SPE-card\\FPGA\\src\\main\\oodg\\content.xml"));
//      main.genJavaData();
      main.parseExample(args);
    } catch (Exception e) {
      System.err.println("Unexpected: " + e.getMessage());
      e.printStackTrace(System.err);
    }
    Debugutil.stop();
    
  }

  
  public static class Args extends Arguments {

    public File fIn, fOutStruct, fOutDataHtml;

    
    
    Arguments.SetArgument setInput = new Arguments.SetArgument(){ @Override public boolean setArgument(String val){ 
      Args.this.fIn = new File(val);
      return true;
    }};
    
    
    Arguments.SetArgument setOutput = new Arguments.SetArgument(){ @Override public boolean setArgument(String val){ 
      Args.this.fOutStruct = new File(val);
      return true;
    }};
    
    
    Arguments.SetArgument setOutDataHtml = new Arguments.SetArgument(){ @Override public boolean setArgument(String val){ 
      Args.this.fOutDataHtml = new File(val);
      return true;
    }};
    
    
    Args(){
      super.aboutInfo = "...Reader content from odg for FunctionBlockGrafic";
      super.helpInfo="obligate args: -o:... ...input";
      addArg(new Argument("-o", ":path/to/output.file", this.setOutput));
      addArg(new Argument("-datahtml", ":path/to/data.html", this.setOutput));
      addArg(new Argument("", "path/to/input.odg", this.setInput));
    }

    @Override
    public boolean testArgs(Appendable msg) throws IOException {
      boolean bOk = true;
      if(this.fOutStruct == null) { msg.append("-o:outfile obligate\n"); bOk = false; }
      if(!bOk) {
        super.showHelp(msg);
      }
      return bOk;
    }
    
  }



}
