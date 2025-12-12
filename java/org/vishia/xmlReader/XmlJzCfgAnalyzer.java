package org.vishia.xmlReader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

import org.vishia.genJavaOutClass.GenJavaOutClass;
import org.vishia.msgDispatch.LogMessage;
import org.vishia.msgDispatch.LogMessageStream;
import org.vishia.util.ApplMain;
import org.vishia.util.Arguments;
import org.vishia.util.Debugutil;
import org.vishia.util.ExcUtil;
import org.vishia.util.FileFunctions;
import org.vishia.util.FileFunctions.FilePathnameExt;
import org.vishia.util.IndexMultiTable;
import org.vishia.util.StringFunctions_B;
import org.vishia.xmlSimple.SimpleXmlOutputter;
import org.vishia.xmlSimple.XmlException;
import org.vishia.xmlSimple.XmlNode;
import org.vishia.xmlSimple.XmlNodeSimple;


/**This class reads any XML file and writes its structure to a XmlCfg format
 * or also can be used to create the Java classes for storing the {@link XmlJzReader} parse results.
 * <ul>
 * <li>It has a main operation to call from command line. See description on {@link https://vishia.org/Java/html/RWTrans/XmlJzReader.html}
 *   or see documentation on {@link CmdArgs}.
 * <li>How does the analysing of XML files work: See description on 
 * </ul>  
 * @author Hartmut Schorrig
 *
 */
public class XmlJzCfgAnalyzer
{
  /**Version, License and History:
   * <ul>
   * <li>2025-12-11 Hartmut: enhancement: Supports re-read XML files with given XmlCfg from before and supplements the configuration
   *   with the new elements in the read XML files: 
   *   <ul><li>{@link #checkStructTree()} recognises given subtrees in {@link #cfgGiven}.
   *   <li>~ also offers all nodes from all trees to check whether the nodes are twice, should build a subtree. 
   *   </ul>
   * <li>2024-05-25 Hartmut: {@link #XXXreadNode(BufferedReader, String)} etc. seems to be unnecessary.
   * <li>2024-05-25 Hartmut: Criteria to build a SUBTREE: Not if all have the same parent tag.
   *   This is only closed sensible, if all parent node occurrences or its parents have the same SUBTREE. 
   *   But this should be regarded if given XmlCfg is really used. 
   * <li>2024-05-25 Hartmut: Now do no more deal with the "!accessPath", this is only for the XML form of XmlCfg
   * <li>2024-05-21 Hartmut: Reads a given XmlCfg, uses Arguments for {@link CmdArgs} now. Not ready for all. 
   * <li>2022-06-06 Hartmut: produces the "xmlinput:finish" entry, see {@link XmlCfg}.
   * <li>2022-06-06 Hartmut: regards all namespace entries
   * <li>2019-08-18 improvements with namespace and usage for {@link GenXmlCfgJavaData}.
   * <li>2018-09-10 writes cfg attributes.
   * <li>2018-08-15 created.
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
  public static final String version = "2018-08-15";

  

  private static class CmdArgs extends Arguments {

    List<FileFunctions.FilePathnameExt> listfIn = new LinkedList<>();;
    
    File fLog;

    /**A given config file to supplement */
    File fInCfg;
    
    
    /**Maybe null, data file to write. */
    File fdOut;
    
    /**Structure file to write. */
    File fWriteXmlConfigText;
    
    /**Structure XML file to write. */
    File fxOut;
    
    File dirDbg;
    
    /**If true then call {@link GenXmlCfgJavaData} with the used existing XmlCfg. */
    File dirCreateCfgJavaData;
    
    /**package path and base class name for the java data.
     * form: <code>"org.package.path.Classname"</code>
     */
    String sPkgJava, sClassJava;
    
    Argument[] argsXmlJzCfgAnalyzer =
    { new Argument("-iXML", ":D:/path/to/file.xml to analyze"
        , new SetArgument() { @Override public boolean setArgument (String val) {
          File fIn = new File(val); CmdArgs.this.listfIn.add(new FileFunctions.FilePathnameExt(fIn, null)); return fIn.exists(); 
        } })
    , new Argument("-iXml", ":\"D:/path/to/file.xml\" or \"...zip:content.xml\" to analyze a stored XML in a zip format"
        , new SetArgument() { @Override public boolean setArgument (String val) throws FileNotFoundException {
          FileFunctions.FilePathnameExt fzip =  FileFunctions.FilePathnameExt.parseZipPath(null, val, ':');
          CmdArgs.this.listfIn.add(fzip); 
          return fzip.file.exists(); 
        } })
    , new Argument("-iCfg", ":D:/path/to/file.cfg a given config file to supplement or for genJavaClass"
        , new SetArgument() { @Override public boolean setArgument (String val) {
          CmdArgs.this.fInCfg = new File(val); return CmdArgs.this.fInCfg.exists(); 
        } })
    , new Argument("-oCfg", ":D:/path/to/file.xml or file.txt to write the config as cfg.xml or cfg.txt"
        , new SetArgument() { @Override public boolean setArgument (String val) {
          File fOut = new File(val); 
          if( !fOut.getParentFile().exists()) {
            return false;
          }
          if(val.endsWith(".xml")) {
            CmdArgs.this.fxOut = new File(val);
          } else {
            CmdArgs.this.fWriteXmlConfigText = new File(val); 
          }
          return true;
        } })
    , new Argument("-genJavaData", ":path/to/JavaDir:pkg.path.Classname - Generates new versions of Java data in pkg path", new SetArgument(){ 
      @Override public boolean setArgument(String val) throws IOException { 
        FileFunctions.FilePathnameExt filePkg = FileFunctions.FilePathnameExt.parseZipPath(null, val, ':');
        File dirJava = filePkg.file;
        if(!dirJava.exists() || !dirJava.isDirectory()) {
          return CmdArgs.this.errMsg("-genJavaData:%s ERROR not found as directory", dirJava );
        } else {
          CmdArgs.this.dirCreateCfgJavaData = dirJava;
          int posClass = filePkg.sNamePath.lastIndexOf('.');
          if(posClass <0) CmdArgs.this.errMsg("\"-genJavaData pathToDir:pkg.Class\" not correct, \".Class\" not found: %s ", filePkg.sNamePath );
          CmdArgs.this.sPkgJava = filePkg.sNamePath.substring(0, posClass); 
          CmdArgs.this.sClassJava = filePkg.sNamePath.substring(posClass+1); 
          return true;
      }}})
    , new Argument("-log", ":D:/path/to/logfile.txt", new SetArgument() {
      @Override public boolean setArgument(String val) throws FileNotFoundException { 
        CmdArgs.this.fLog = new File(val) ; 
        return CmdArgs.this.fLog.getParentFile().exists();
      }})
    , new Argument("-dirDebug", ":D:/path/to/xmlCfg.txt in given write debug info", new SetArgument() {
      @Override public boolean setArgument(String val) throws FileNotFoundException { 
        CmdArgs.this.dirDbg = new File(val) ;  
        //FileFunctions.mkDir(CmdArgs.this.dirDbg);
        return CmdArgs.this.dirDbg.exists();
      }})
    };
    
    
    CmdArgs () {
      super.aboutInfo = "...your about info";
      super.helpInfo="obligate args: -o:...";
      //super.addArgs(this.argsXmlJzCfgAnalyzer);
      for(Argument arg: this.argsXmlJzCfgAnalyzer) { addArg(arg); }
    }

    /**This operation checks the consistence of all operations. */
    @Override
    public boolean testConsistence(Appendable msg) throws IOException {
      boolean bOk = true;
      //if(this.fWriteXmlConfigText == null) { msg.append("-o:outfile obligate\n"); bOk = false; }
      if(!bOk) {
        super.showHelp(msg);
      }
      return bOk;
    }
  
    
    
    
  }

  
  private CmdArgs cmdArgs;
  
  File dirDbg;
  
  public static void main ( String[] sArgs) {
    try {
      int exitCode = smain(sArgs, System.out, System.err);
      System.exit(exitCode);
    } catch (Exception e) {
      System.err.println("Unexpected: " + e.getMessage());
      e.printStackTrace(System.err);
      System.exit(255);
    }
  }

  
  /**main gets the arguments as String, 
   * but does not catch unexpected exceptions and does not System.exit(...), use it to execute in a Java environment. 
   * @param sArgs
   * @return 0 if all is ok
   * @throws IOException 
   * @throws Exception if unexpected.
   */
  public static int smain ( String[] sArgs, Appendable logHelp, Appendable logError) throws IOException {
    CmdArgs args = new CmdArgs();
    if(sArgs.length ==0) {
      args.showHelp(logHelp);
      return(1);                // no arguments, help is shown.
    }
    if(  ! args.parseArgs(sArgs, logError)
      || ! args.testConsistence(logError)
      ) { 
      return(2);                    // argument error
    }
    //LogMessageStream log = new LogMessageStream(System.out);
    int exitCode = amain(args);
    return exitCode;
  }
  
  
  
  
  /**Reads a XML file and writes its structure as cfg.xml to help creating a config for the XmlJzReader
   * @param args pathfileIn.xml pathfileOutcfg.xml 
   */
  public static int amain(CmdArgs args) {
    int error = 0;
    XmlJzCfgAnalyzer main = new XmlJzCfgAnalyzer();
    main.cmdArgs = args;
    main.setDirDebug(args.dirDbg);
    try {
      if(args.fInCfg !=null) {   //=========================== read a given cfg file to accordingly in subtree organization.
        main.cfgGiven = new XmlCfg(true);
        main.cfgGiven.readCfgFile(args.fInCfg, main.log);
        if(args.dirDbg !=null) {
          main.cfgGiven.writeToText(new File(args.dirDbg, "cfgGiven.read.back.txt"), main.log);  // only for test.
        }
      }
      if(args.fWriteXmlConfigText !=null || args.fxOut !=null) {    //========vv option -analyzeXmlStruct:path/to/fWriteCfgTxt
        main.analyzeXml(args);    
      } else {
        main.cfgData = main.cfgGiven;
      }
      if(args.dirCreateCfgJavaData !=null && main.cfgData !=null) {
        main.writeJavaClasses();
      }
    } catch (Exception e) {
      System.err.println("Unexpected: " + e.getMessage());
      e.printStackTrace(System.err);
      error = 255;
    }
    return error;
  }

  
  
  /**Creates the configuration to read any xml file to store its structure especially in {@link XmlJzCfgAnalyzer.XmlStructureNode}.
   * @return instance of XmlCfg to do so.
   */
  public static XmlCfg newCfgReadStruct()
  { XmlCfg cfgCommon = new XmlCfg(false);
    cfgCommon.rootNode = new XmlCfg.XmlCfgNode(null, cfgCommon, null);  //The rootnode of the cfg is only formalistic.
    
    XmlCfg.XmlCfgNode rootNode = new XmlCfg.XmlCfgNode(null, cfgCommon, "?");  //<xmlinput:cfg as node 2. level
    cfgCommon.rootNode.addSubnode(rootNode.tag.toString(), rootNode);        //The cfg file should start with a <xmlinput:root
    rootNode.addSubnode(rootNode.tag.toString(), rootNode);        //The cfg file should start with a <xmlinput:root
    
    try {
      //On any element the 'addElement(tag)' is invoked via Reflection. 
      rootNode.setNewElementPath("!addElement(tag)");  //executed in the data destination instance.
      rootNode.addAttribStorePath("?", "!setAttribute(name)"); 
      rootNode.setNameSpaceStorePath("!addNamespace(name, value)");
      rootNode.setContentStorePath("!setTextOccurrence()");
    }
    catch(ParseException exc) {
      throw new RuntimeException(exc); //it is unexpected
    }
    return cfgCommon;
  }




  int debugStopLineXmlInp = -1;
  
  LogMessage log;
  
  /**The common structure data of the read XML file. */
  final XmlStructureData xmlStructData = new XmlStructureData(this);
  
  /**Contains for each tag as key all occurrences of nodes with this tag.
   * An element is added to this root node and then to all current nodes in {@link XmlStructureNode#addElement(String)}. */
  XmlStructureNode xmlStructTree = new XmlStructureNode(null, "root", this.xmlStructData);  //the root node for reading config

  /**Contains one element for each tag name or node type.
   * The {@link XmlStructureData.CfgSubtreeType#occurrence} of the element 
   * contains only one instance of {@link XmlStructureNode} if the node with this tag is only found in one constellation,
   * but maybe more as one time in the same parent.
   * <br>
   * If the node with this tag name is found in different constellations (with different parent nodes) 
   * then each constellation occurrence of {@link XmlStructureNode} ia stored in {@link XmlStructureData.CfgSubtreeType#occurrence}.
   * <br><br>
   * {@link #checkStructTree()} checks, if the occurrences are substantial equal, (yet always),
   * then a 'SUBTREE' is created to prevent effort in the configuration,
   * respectively allow recursively using of a node type.
   * 
   */
  TreeMap<String, XmlStructureData.CfgSubtreeType> allElementTypes = new TreeMap<String, XmlStructureData.CfgSubtreeType>();
  


  XmlCfg cfgData = new XmlCfg(true);
  
  XmlCfg cfgGiven;
  
  XmlCfg cfgGiven1;
  
  
  public XmlJzCfgAnalyzer () {
    this.log = new LogMessageStream(null, null, System.out, System.err, false, Charset.forName("UTF-8"));
    
  }
  
  
  public void setDirDebug(File dir) {
    this.dirDbg = dir;
  }
  
  /**Only for internal debug. See implementation. There is a possibility to set a break point if the parser reaches the line.
   * @param line
   */
  public void setDebugStop(int line) {
    debugStopLineXmlInp = line;
  }
 
  
  
  
  /**Reads given XML file(s) and writes its structure as cfg.xml or cfg.txt to help creating a config for the XmlJzReader.
   * <ul>
   * <li>For explanation of the analyse algorithm see the called {@link #analyzeXmlFile(List)}.
   * <li>Write the XML config is done if {@link CmdArgs#fxOut} is given, it calls {@link #writeCfgTemplate(File)}.
   * <li>The textual configuration is written on given {@link CmdArgs#fWriteXmlConfigText}
   *   with the capability in {@link XmlCfg}: {@link XmlCfg#writeToText(File, LogMessage)}.
   * </ul>  
   * @param args from main invocation, also from any other Java call with this args as super class.
   *   <ul>
   *   <li>uses {@link CmdArgs#listfIn} either zip with xml or xml file, more as one possible.
   *   <li>uses {@link CmdArgs#fdOut} to write with {@link #writeData(File)} currently not used and tested.
   *   <li>uses {@link CmdArgs#fWriteXmlConfigText} to write the content of analysed configuration to a config.txt file
   *     usable for {@link XmlCfg#readCfgFile(File, LogMessage)} later for parsing.
   *   <li>uses {@link CmdArgs#fxOut} to write the content of analysed configuration to a config.xml file.
   *     This is meanwhile (2025-12) no more recommended, use the textual config presentation. 
   *     It is usable for {@link XmlJzReader#se} later for parsing.
   *   </li>
   * 
   * pathfileIn.xml pathfileOutcfg.xml 
   */
  void analyzeXml (CmdArgs args) throws IOException {
    //
    //======>>>> 
    this.analyzeXmlFile(args.listfIn);
    if(args.fdOut !=null) {
      this.writeData(args.fdOut);
    }
    //JZtxtcmdTester.dataHtmlNoExc(this.data, new File("T:/datashow.html"), true);
    if(args.fxOut !=null) {
      this.writeCfgTemplate(args.fxOut);
      this.log.writef("\nconfig XML template written to: %s", args.fxOut.getAbsolutePath());
      log.writef("");
    } 
    if(args.fWriteXmlConfigText !=null) {
      this.cfgData.writeToText(args.fWriteXmlConfigText, this.log);
      this.log.writef("\nconfig text written to: %s", args.fWriteXmlConfigText.getAbsolutePath());
    }
  }


  /**Reads any XML file and stores the structure in the returned {@link XmlCfg}.
   * The found configuration should be writtem out after them with {@link XmlCfg#writeToText(File, LogMessage)}
   * to use it to configure the {@link XmlJzReader#setCfg(XmlCfg)} with a proper configuration,
   * which reads the here created textual configuration file via
   * {@link XmlCfg#readCfgFile(File, LogMessage)} or {@link XmlCfg#readFromJar(Class, String, LogMessage)}. 
   * Whereby, for further usage, the configuration file and also the possible created Java classes
   * can be manual adapted before further usage.
   * <br>
   * It calls the following operations per node and attribute in the read XML file, 
   * set a breakpoint there to see what's happen. 
   * This operations are called inside {@link XmlJzReader#readXml(File, Object)}
   * due to the set config with {@link #newCfgReadStruct()}.
   * <ul>
   * <li>{@link XmlStructureNode#addElement(String)} on a new node on '<tag...'
   * <li>{@link XmlStructureNode#setAttribute(String)} on a attribute
   * <li>{@link XmlStructureNode#addNamespace(String, String)} on a 'xmlns:...' attribute
   * <li>{@link XmlStructureNode#setTextOccurrence()} on a text.
   * </ul>
   * This operations accumulate the occurrence of the given tags and attributes in 
   * <ul>
   * <li>{@link #xmlStructData}:  
   * Note: renamed from readXmlStructZip(...)
   * @param fXmlIn The file, immediately the XML file or a zip file containing the XML file.
   * @param pathInZip null for reading immediately XML, else the path in the zip file to the XML file to read.
   * @return
   * @throws IOException
   */
  public XmlCfg analyzeXmlFile(List<FileFunctions.FilePathnameExt> fIn) throws IOException {
    XmlJzReader xmlReader = new XmlJzReader();
    xmlReader.setCfg(newCfgReadStruct());        //<<<<------ this is the essential one, analyse as SAX parser any node.
    if(this.debugStopLineXmlInp >0) {
      xmlReader.setDebugStop(this.debugStopLineXmlInp);
    }
    for(FileFunctions.FilePathnameExt fIn1: fIn) {
      String sInName = fIn1.file.getName();
      this.log.writef("\nanalyses XML for: %s", fIn1.file.getAbsolutePath());
      if(this.dirDbg !=null) {
        xmlReader.openXmlTestOut( new File( this.dirDbg, sInName + "-back.xml")); //fout1);
      }
      xmlReader.readZipXml(fIn1.file, fIn1.sNamePath, this.xmlStructTree);
    }
    //
    checkStructTree();
    // this.cfgData has now already created but yet empty subtrees. The root node is also empty.
    //this.xmlStructData.checkCfgSubtree(this.cfgGiven);   //removeSingleEntries();
    storeInCfg(xmlReader);
    return this.cfgData;
  }


  /**Writes the config file for the XmlJzReader with config file.
   * @param wrCfg the output file.
   */
  public void writeCfgTemplate(File wrCfg) {
    FileWriter writer = null;
    try {
      //Output it as cfg.xml
      XmlNodeSimple<?> root = new XmlNodeSimple<>("xmlinput:root");
      root.addNamespaceDeclaration("xmlinput", "www.vishia.org/XmlReader-xmlinput");
      for(Map.Entry<String, String> en: this.cfgData.xmlnsAssign.entrySet()) {
        root.addNamespaceDeclaration(en.getValue(), en.getKey() );
      }
      for(Map.Entry<String, XmlStructureNode> estructnode: this.xmlStructData.cfgSubtreeList.entrySet()) {
        XmlStructureNode structnode = estructnode.getValue();
        //add one subtree node for each tag type in its context:
        assert(structnode.sSubtreenode !=null);  //it should be designated.
        XmlNode wrCfgsubtreenode = root.addNewNode("subtree", "xmlinput"); //second node "cfg"
        if(structnode.sSubtreenode.equals("ObjectList_A"))
          Debugutil.stop();
        wrCfgsubtreenode.setAttribute("name", "xmlinput", structnode.sSubtreenode);
        String sClass = StringFunctions_B.replaceNonIdentifierChars(structnode.sSubtreenode, '-').toString();
        wrCfgsubtreenode.setAttribute("class", "xmlinput", sClass);
        if(structnode.attribs !=null)for(Map.Entry<String, AttribRead> e_attrib: structnode.attribs.entrySet()) {
          AttribRead attrib = e_attrib.getValue();
//          if(attrib.name.equals("idref"))
//            Debugutil.stop();
          wrCfgsubtreenode.setAttribute(attrib.name, attrib.namespace, attrib.value);
        }
        wrSetAddContentAttrib(structnode, wrCfgsubtreenode);
        //
        if(structnode.nodes !=null) for(Map.Entry<String, XmlStructureNode> e_srcSubnode: structnode.nodes.entrySet()) {
          XmlStructureNode srcSubnode = (XmlStructureNode)e_srcSubnode.getValue();
          XmlNodeSimple<?> xmlNodeSub = new XmlNodeSimple<>(srcSubnode.tag);
          wrCfgsubtreenode.addContent(xmlNodeSub);
          addWrNode(xmlNodeSub, srcSubnode, 999);
        }
      }
      //add the root cfg node with all its subnodes:
      XmlNode cfgnode = root.addNewNode("cfg", "xmlinput"); //second node "cfg"
      //
      //add all nodes from data, recursively called.
      addWrNode(cfgnode, xmlStructTree, 999);  
      //
      SimpleXmlOutputter oXml = new SimpleXmlOutputter();
      writer = new FileWriter(wrCfg);
      oXml.write(writer, root);
      writer.close();
    } catch(XmlException | IOException exc) {
      CharSequence error = ExcUtil.exceptionInfo("unexpected", exc, 0, 100);
      System.err.append(error);
    } finally {
      if(writer !=null) {
        try { writer.close(); } catch(IOException exc) { System.err.append("cannot close"+wrCfg.getAbsolutePath());}
      }
    }
    
    Debugutil.stop();
  }


  
  
  /**Writes the config file for the XmlJzReader with config file.
   * @param wrCfg the output file.
   */
  public void writeCfgText(File fout) {
    Writer out = null;
    try {
      out = new OutputStreamWriter(new FileOutputStream(fout), "UTF-8");
      //Output it as cfg.xml
      out.append("\n\n##== Namespaces ==\n");
      for(Map.Entry<String, String> en: this.cfgData.xmlnsAssign.entrySet()) {
        ApplMain.outNewlineIndent(out,0);
        out.append("@xmlns:").append(en.getValue()).append(" = ").append(en.getKey());
      }
      out.append("\n\n##== Subtrees ==\n");
      for(Map.Entry<String, XmlStructureNode> estructnode: this.xmlStructData.cfgSubtreeList.entrySet()) {
        XmlStructureNode structnode = estructnode.getValue();
        //add one subtree node for each tag type in its context:
        if(structnode.sSubtreenode ==null)
          Debugutil.stop();
        //assert(structnode.sSubtreenode !=null);  //it should be designated.
        structnode.writeData(out, 0);
        //
//        if(structnode.nodes !=null) for(Map.Entry<String, XmlStructureNode> e_srcSubnode: structnode.nodes.entrySet()) {
//          XmlStructureNode srcSubnode = e_srcSubnode.getValue();
//          srcSubnode.writeData(out, 2);          // calls recursively more subnodes
//          ApplMain.outNewlineIndent(out, 0);
//        }
      }
      out.append("\n\n##== The root struct ==\n");
      if(false) {
        for(Map.Entry<String, XmlStructureNode> enode: this.xmlStructTree.nodes.entrySet()) {
          XmlStructureNode node = (XmlStructureNode)enode.getValue();
          node.writeData(out,0);
        }
      } else {
        this.xmlStructTree.writeData(out, 0);
      }
      ApplMain.outNewlineIndent(out, 0);
      
      out.close();
    } catch(Exception exc) {
      CharSequence error = ExcUtil.exceptionInfo("unexpected", exc, 0, 100);
      System.err.append(error);
    } finally {
      if(out !=null) {
        try { out.close(); } catch(IOException exc) { System.err.append("cannot close" + fout.getAbsolutePath());}
      }
    }
  }


  private void writeJavaClasses () throws IllegalCharsetNameException, UnsupportedCharsetException {
    GenJavaOutClass.CmdArgs genXmlJavaDataArgs = new GenJavaOutClass.CmdArgs();
    genXmlJavaDataArgs.dirJava = this.cmdArgs.dirCreateCfgJavaData;
    genXmlJavaDataArgs.sJavaClass = this.cmdArgs.sClassJava; //"XmlDataLOffcStyles";
    genXmlJavaDataArgs.sJavaPkg = this.cmdArgs.sPkgJava; //"org.vishia.libOffc.styles";
    GenXmlCfgJavaData genXmlJavaData = new GenXmlCfgJavaData(genXmlJavaDataArgs, this.log);
    genXmlJavaData.exec(this.cfgData);
  }


  
  
  @SuppressWarnings("static-method") 
  private void wrSetAddContentAttrib(XmlStructureNode structNode, XmlNode wrCfgXmlNode) throws XmlException {
    CharSequence sArg;
    if(structNode.attribs !=null) {
      StringBuilder uArg= new StringBuilder(100);
      sArg = uArg;
      char sep = '(';
      for(Map.Entry<String,AttribRead> e: structNode.attribs.entrySet()) {
        AttribRead attrib = e.getValue();
        wrCfgXmlNode.setAttribute(attrib.name, attrib.namespace, attrib.value);  //transfer to cfg too.
        if(attrib.value.startsWith("@")) {  //use only attributes which should be used as arguments for the set/add operation
          uArg.append(sep).append(attrib.value.substring(2));
          sep = ',';
        }
      }
      uArg.append(')');
    } else {
      sArg = "()";
    }                                            // writes the xmlinput:data="!new_Tag(...)" to create data for the sub nodes and attribs
    wrCfgXmlNode.setAttribute("data", "xmlinput", "!new_" + structNode.tagIdent + sArg);
    final String setAdd;
    if(structNode.onlySingle) {                 // writes the xmlinput:finish="!set_Tag(data)" to set the data. 
      setAdd = "!set_";
    } else {
      setAdd = "!add_";
      wrCfgXmlNode.setAttribute("list", "xmlinput", "");
    }
    wrCfgXmlNode.setAttribute("finish", "xmlinput", setAdd + structNode.tagIdent + "(value)");
    
  }
  
  
  
  
  /**Adds the node and recursively all sub nodes from {@link XmlStructureNode}
   * It is invoked after a XML file war read with {@link XmlCfg#newCfgReadStruct()} 
   * @param wrCfgXmlNode The xml node for output to add.
   * @param structNode The node from the structure of the read XML file, 
   * @param recursion decremented, exception on <=0
   * @throws XmlException on XML error, IllegalArgumentException on recursion error.
   */
  private void addWrNode(XmlNode wrCfgXmlNode, XmlStructureNode structNode, int recursion) throws XmlException {
    if(recursion <0) throw new IllegalArgumentException();
    if(structNode.nodes !=null || structNode.attribs !=null) {
      
      wrSetAddContentAttrib(structNode, wrCfgXmlNode);  //sets the xmlinput:data="..." and xmlinput:list=""
      if(structNode.sSubtreenode !=null) {
        //The tag type occurs more as one time in different situations, but with the same meaning. Use subtree in cfg.
        String sSubtreeName = structNode.sSubtreenode;
        wrCfgXmlNode.setAttribute("subtree", "xmlinput", sSubtreeName);
      } else {
        //Only one time tag type: tree inside.
        if(structNode.nodes !=null || structNode.attribs !=null && (structNode.bText || structNode.attribs.size() >1)) {
          //sub nodes or more as one (attribute or text content) needs a sub class to store the content for this node. 
          String sClass = StringFunctions_B.replaceNonIdentifierChars(structNode.tagIdent, '-').toString();
          wrCfgXmlNode.setAttribute("class", "xmlinput", sClass  );
        }
        if(structNode.nameSpaces !=null) {
          Debugutil.stop();
          for(Map.Entry<String, String> e: structNode.nameSpaces.entrySet()) {
            wrCfgXmlNode.addNamespaceDeclaration(e.getKey(), e.getValue());
          }
        }
        if(structNode.nodes !=null) { //has subnodes
          for(Map.Entry<String, XmlStructureNode> e: structNode.nodes.entrySet()) {
            XmlStructureNode subnode = (XmlStructureNode)e.getValue();
            String tag = e.getKey();
            XmlNodeSimple<?> xmlNodeSub = new XmlNodeSimple<>(tag);
            wrCfgXmlNode.addContent(xmlNodeSub);
            addWrNode(xmlNodeSub, subnode, recursion-1);
        } }
      }
      if(structNode.bText) {
        wrCfgXmlNode.addContent("!set_text(text)");
      }
    } else { //no attribs, no sub tree, only the text content should be stored.
      if(structNode.onlySingle) {
        wrCfgXmlNode.addContent("!set_" + structNode.tag + "(text)");  //set_TAG(String text)
      } else {
        wrCfgXmlNode.addContent("!add_" + structNode.tag + "(text)");
      }
    }
  }


  /**Reads and analyses any XML file and stores the structure in the {@link #XmlCfg}.
   * Description see {@link #analyzeXmlStructZip(File, String)}
   * Note: compatible form calls {@link #analyzeXmlFile(List)}
   * @param fXmlIn
   * @throws IOException 
   */
  public XmlCfg analyzeXmlStruct(File fXmlIn) throws IOException {
    LinkedList<FileFunctions.FilePathnameExt> list = new LinkedList<>();
    list.add(new FileFunctions.FilePathnameExt(fXmlIn, null));
    return analyzeXmlFile(list);
  }

  
  
  /**Compatible form
   * @param fXmlIn
   * @return
   * @throws IOException
   * @Deprecated use {@link #analyzeXmlStruct(File)}
   */
  public XmlCfg readXmlStruct(File fXmlIn) throws IOException {
    return analyzeXmlStruct(fXmlIn);
  }

  /**Reads any XML file and stores the structure in the {@link #XmlCfg}.
   * Description see {@link #analyzeXmlStructZip(File, String)}
   * Note: compatible form calls {@link #analyzeXmlFile(List)}
   * @param fIn zip file
   * @param sPathInZip null then fIn should be an XML file, else path of the XML file in the zip
   * @throws IOException 
   */
  public XmlCfg readXmlStructZip(File fIn, String sPathInZip) throws IOException {
    LinkedList<FileFunctions.FilePathnameExt> list = new LinkedList<>();
    list.add(new FileFunctions.FilePathnameExt(fIn, sPathInZip));
    return analyzeXmlFile(list);
  }

  
  
  /**Checks the read XML structure tree.
   * All the occurrences of nodes with the same tag are stored in {@link #allElementTypes}.
   * That entries knows all XmlStructNode occurrences.
   * If a XmlStructNode is used in several constellations (more as one time found, different parent),
   * then an entry in {@link #cfgData} {@link XmlCfg#subtrees} will be created to mark, it is an candidate for SUBTREE.
   * The SUBTREE in {@link #cfgData} is then filled in {@link #storeInCfg(XmlJzReader)}.
   * 
   */
  private void checkStructTree () {
    if(this.cfgGiven !=null) {
      this.cfgData = this.cfgGiven;
    }
    //                                           ==========vv build a map with all existing nodes, key is tag name
    Map<String, XmlCfg.XmlCfgNode> idxNodesGiven = new TreeMap<>(); // to check whether tags are twice now.
    fillNodesGivenForCheckSubtree(this.cfgGiven.rootNode, idxNodesGiven, 0); // from root below root
    for(XmlCfg.XmlCfgNode ndSub: this.cfgGiven.subtrees.values()) {
      fillNodesGivenForCheckSubtree(ndSub, idxNodesGiven, 0); // as also from all child nodes from subtree
    }                                                      
    for(XmlStructureData.CfgSubtreeType cfgSubtreeOccurrences: this.allElementTypes.values()) {
      String sTag = cfgSubtreeOccurrences.occurrence.get(0).tag;
      //if(sTag.equals("style:list-level-properties")) Debugutil.stopp();
      //                                                   //vv if cfgGiven has here a subtree, use it.
      XmlCfg.XmlCfgNode ndGivenSubtree = this.cfgGiven == null ? null : this.cfgGiven.subtrees.get(sTag);
      XmlCfg.XmlCfgNode ndGivenSingle = this.cfgGiven == null ? null : idxNodesGiven.get(sTag);
      if ( ndGivenSubtree !=null   //------------------------ the node is already found as SUBTREE in given cfg 
        || ndGivenSingle !=null   //------------------------- the node is also found in any tree of given cfg
        || cfgSubtreeOccurrences.occurrence.size() >1      // or more as one occurrence 
         ) {
        String sTagParent = null;
        if(ndGivenSingle !=null) {
          //if(cfgSubtreeOccurrences.occurrence.size() ==1) Debugutil.stopp();
          sTagParent = ndGivenSingle.parent.tag.toString();
        }
        boolean bCreateSubtree = ndGivenSubtree !=null;
        for(XmlStructureNode structNode: cfgSubtreeOccurrences.occurrence ) {
          structNode.onlySingle &= (ndGivenSingle == null && ndGivenSubtree == null);
          if(sTagParent == null) { sTagParent = structNode.parent.tag; } // first initialize
          else if(!sTagParent.equals(structNode.parent.tag) ) {
            bCreateSubtree = true;
            break;      // one different parent is enough
          } else {
            Debugutil.stop(); //Twice the same parent. 
            //That occurs if the child is in different branches where the parent is used different,
            //But this child node is only occuring in context of this parent. 
            // Then the parent should be build a SUBTREE entry, and not this child.
          }
        }
        if(!bCreateSubtree) {
          Debugutil.stop();      // this is exact if the subNode is used more as one, but anyway in the same parent constellation.
        }
        // If !cCreateSubtree: The same node type occurs in several positions in tree, 
        // but anytime in the same parent. If the parent will be a subtree member, it is only one. 
        // If the parent will not be a subtree member and also all grand parent won't, 
        // then it is a node in a specific situation, do not build a subtree because the parents don't. 
        if(bCreateSubtree) {
          //Build a SUBTREE
          XmlCfg.XmlCfgNode ndSub = this.cfgData.addSubTree(sTag);                     // creates a SUBTREE entry, then the content will be associated in this SUBTREE in #storeInCfg
          if(ndGivenSubtree == null && ndGivenSingle !=null) {
            if(ndGivenSingle.attribs !=null) {
              ndSub.attribs = new TreeMap<>();;
              Iterator<XmlCfg.AttribDstCheck> iterAttribs =  ndGivenSingle.attribs.values().iterator();
              while(iterAttribs.hasNext()) {
                XmlCfg.AttribDstCheck attr = iterAttribs.next();
                if(!attr.bUseForCheck) {
                  iterAttribs.remove();
                }
                ndSub.attribs.put(attr.name, attr);
              }
            }
            ndSub.subnodes = ndGivenSingle.subnodes;
            ndGivenSingle.subnodes = null;
            ndGivenSingle.cfgSubtreeName = sTag;
            ndSub.dstClassName = ndGivenSingle.dstClassName;
            ndSub.elementCreatePath = ndGivenSingle.elementCreatePath;
            ndSub.contentStorePath = ndGivenSingle.contentStorePath;
          }
          
          for(XmlStructureNode structNode: cfgSubtreeOccurrences.occurrence ) {
            structNode.sSubtreenode = sTag;                // mark only the node, the content will transferred then in storeInCfg(...)
          }
        }
      }
    }
  }

  
  
  private final void fillNodesGivenForCheckSubtree(XmlCfg.XmlCfgNode node, Map<String, XmlCfg.XmlCfgNode> idxNodesGiven, int recursion) {
    if(recursion >100) return;
    if(node.subnodes !=null) {
      for(XmlCfg.XmlCfgNode ndChild : node.subnodes.values()) {
        idxNodesGiven.put(ndChild.tag.toString(), ndChild);
        fillNodesGivenForCheckSubtree(ndChild, idxNodesGiven, recursion+1);
    } }
  }
  
  
  
  
  private void storeInCfg ( XmlJzReader xmlReader) {
    this.cfgData.transferNamespaceAssignment(xmlReader.namespaces);
    try {                              //==================vv first search all subtrees:
      for(XmlStructureData.CfgSubtreeType cfgSubtreeOccurrences: this.allElementTypes.values()) {
        for(XmlStructureNode structNode: cfgSubtreeOccurrences.occurrence ) {
          if(structNode.sSubtreenode !=null) {             // each occurence marked with sSubtreenode contributes to the content of the sub tree:
            XmlCfg.XmlCfgNode subtree = this.cfgData.subtrees.get(structNode.sSubtreenode); // subtrees was already filled by first empty subtree nodes.
            //if(structNode.sSubtreenode.equals("style:style")) Debugutil.stopp();
            storeCfgNode(subtree, structNode, true, 100);  // add the content of the read node in structNode to the content of the node as subtree
          }
        }            
      }
//      for(Map.Entry<String, XmlStructureNode> eSubtree: this.xmlStructData.cfgSubtreeByName.entrySet()) {
//        String nameSubtree = eSubtree.getKey();
//        XmlStructureNode treeRoot = eSubtree.getValue();
//        XmlCfg.XmlCfgNode subroot = this.cfgData.addSubTree(nameSubtree);
//        storeCfgNode(subroot, treeRoot, 100);
//      }
      storeCfgNode(this.cfgData.rootNode, this.xmlStructTree, true, 100);
    } catch(ParseException exc) {
      this.log.writeError("ERROR unexpected: ", exc);
    }
    
  }
  
  
  /**Copy or add the content of the read {@link XmlStructureData} 'srx' to the XmlCfg 'dst'.
   * This can be a simple inner child node which is created with 'dst' in its parent,
   * or it can be also the first node of a {@link XmlCfg#subtrees}
   * @param dst the destination where info from 'src' are stored
   * @param src read data from evaluated XML
   * @param bRoot true then ignore {@link XmlStructureNode#sSubtreenode} because it is the name of the subtree itself,
   *   and not the reference to a subtree.
   * @param recursion called recursively for all sub nodes
   * @throws ParseException
   */
  private void storeCfgNode(XmlCfg.XmlCfgNode dst, XmlStructureNode src, boolean bRoot, int recursion) throws ParseException {
    if(recursion <=0) { assert(false); return; }
    XmlStructureNode srcx = src;
    //if(src.tag.equals("style:list-level-properties")) Debugutil.stopp();
    //if(src.tag.equals("style:style")) Debugutil.stopp();
    dst.bList |= !src.onlySingle;
    String sClass1 = StringFunctions_B.replaceNonIdentifierChars(srcx.tagIdent, '-').toString();
    String sClass = sClass1;
    char c1 = sClass1.charAt(0);
    if(!Character.isUpperCase(c1)) {
      sClass = Character.toUpperCase(c1) + sClass1.substring(1);
    }
    assert(dst.dstClassName == null || dst.dstClassName.equals(sClass) || dst.dstClassName.equals(sClass1));
    dst.dstClassName = sClass;
    if(!bRoot || src.sSubtreenode ==null) {      // bRoot && sSubtreeNode: In a subtree root definition do not write SET or ADD.
      // Note for condition: it is any node with or without sSubtreeNode, if it is not the root.
      StringBuilder sNewElement = new StringBuilder(20 + (src.attribs == null ? 0 : src.attribs.size() * 10));
      sNewElement.append("new_").append(src.tagIdent).append('(');  // new creates the instance and sets the reference.
      if(src.attribs !=null) {
        String sep = "";
        for(AttribRead attrib: src.attribs.values()) {     // all attribs with "@argname" are used as argument names.
          if(attrib.value.startsWith("@")) {               // for all attributes for any node occurence.
            sNewElement.append(sep).append(attrib.value.substring(1));
            sep = ",";
          }
        }
      }
      sNewElement.append(')');                             // the new_...() operation can also store the new instance of the data for the node
      dst.setNewElementPath(sNewElement.toString() );      // inside the parent node, hence should be written in the parent node.
            if(dst.bList) {                            // write ADD:"operation" always in the original node, not in the subtree node
        String sAddPath = "add_" + src.tagIdent + "(value)";
        dst.setFinishElementPath(sAddPath);      //... because it is different how to store in the parent node, List or No List.
      } else {
        String sAddPath = "set_" + src.tagIdent + "(value)";
        dst.setFinishElementPath(sAddPath);      //If no LIST, then use set_... due to code generation. 
    } }
    if(!bRoot && src.sSubtreenode !=null) {      //--------vv if bRoot then the 'sSubtreenode' is only the name of the sub tree
      dst.cfgSubtreeName = src.sSubtreenode;               // sets the reference to the subtree node to use.
    } else {                                     //--------vv SUBTREE is not given, or it is the start node of a subtree
      if(src.bText) {
        dst.setContentStorePath("set_Text(text)");         // write the necessities.
      }
      //dst.setNameSpaceStorePath("!ns");
      dst.bCheckAttributeNode = false;
      //dst.attribsForCheck;
      //dst.allArgNames;
      dst.bStoreAttribsInNewContent = false;
      if(src.attribs !=null) { //============================ Transfer all attribs
        for(AttribRead attrib: src.attribs.values()) {
          String key = attrib.namespace + ':' + attrib.name;
          //if(key.equals("draw:fit-to-size")) Debugutil.stopp();
          dst.addAttribStorePath(key, attrib.value);
        }
      }
      if(src.nodes !=null) {   //============================ Transfer all nodes
        if(dst.subnodes == null) { dst.subnodes = new TreeMap<>(); }
        for(XmlStructureNode nodez: src.nodes.values()) {
          //if(nodez.tag.equals("text:list-style")) Debugutil.stopp();
          XmlCfg.XmlCfgNode nodeDst = dst.subnodes.get(nodez.tag);
          if(nodeDst == null) {
            nodeDst = new XmlCfg.XmlCfgNode(dst, this.cfgData, nodez.tag);
            dst.addSubnode(nodeDst.tag.toString(), nodeDst);   // a new till now not existing new node
          } else {
            mergeNode(nodeDst, nodez);
          }
          storeCfgNode(nodeDst, nodez, false, recursion -1);  // store and merge
          Debugutil.stop();
        }
      }
    }
  }
  
  public void mergeNode(XmlCfg.XmlCfgNode ndDst, XmlStructureNode ndSrc) {
    Debugutil.stop();
  }


  
  public void writeData ( File fout) throws IOException {
    Writer out = new OutputStreamWriter(new FileOutputStream(fout), "UTF-8");
    this.xmlStructTree.writeData(out, 0);
    ApplMain.outNewlineIndent(out, 0);
    out.close();
  }    


  
  static final class XmlStructureData {
    
    final XmlJzCfgAnalyzer this0;
    
    /**Contains all elements with its {@link #occurrence}.
     * Instances of this class are built to store occurrences of each tag in {@link XmlJzCfgAnalyzer#allElementTypes}
     */
    static class CfgSubtreeType {
      
      List<XmlStructureNode> occurrence = new ArrayList<XmlStructureNode>();
      
      public CfgSubtreeType(){}
      
      @Override public String toString() { return "" + occurrence.size() + " * " + occurrence.get(0).toString(); }
    }
    
    /**Contains info about equals element types.
     */
    static class CfgSubtreeType2 {
      
      final String tag;
      //XmlStructureNodeBuilder node;
      
      /**Contains all nodes in all levels which are detect as same type.*/
      List<XmlStructureNode> occurrence = new ArrayList<XmlStructureNode>();
      
      /**The representative attributes and sub nodes of all associated nodes. */
      XmlStructureNode representative;
      
      List<CfgSubtreeType2> dependings = new ArrayList<CfgSubtreeType2>();
      
      boolean bSort;
      
      Map<String, String> attributeNames = new TreeMap<String, String>();
      Map<String, String> nodeNames = new TreeMap<String, String>();
      
      public CfgSubtreeType2(XmlStructureNode structNode){
        this.tag = structNode.tag;
        this.representative = structNode; //the first
        this.occurrence.add(structNode);
      }
      
      
      
      /**Adds the content of a new occurring node to the subtree {@link #representative} node.
       * @param node new found node in user XML tree.
       */
      protected void addContentOfFoundNode(XmlStructureNode node){
        if(node.attribs !=null) for(Map.Entry<String, AttribRead> e: node.attribs.entrySet()) {
          String key = e.getKey();
          if(key.equals("draw:fit-to-size")) Debugutil.stopp();
          if(this.attributeNames.get(key) ==null) {
            this.attributeNames.put(key, key);     //an attribute non detected as yet, add it in representative.;
            if(this.representative.attribs == null) {
              this.representative.attribs = new TreeMap<String, AttribRead>();
            }
            this.representative.attribs.put(key, e.getValue());
          }
        }
        if(node.nodes !=null) for(Map.Entry<String, XmlStructureNode> e: node.nodes.entrySet()) {
          String key = e.getKey();
          //nonsense on the old source position: foundNodeNames.put(key, key);
          if(this.nodeNames.get(e.getKey()) ==null) {
            this.nodeNames.put(key, key);    //a node non detected as yet, add it in representative.
            if(this.representative.nodes ==null) {
              //this.representative.addElement(key);
              this.representative.nodes = new TreeMap<String, XmlStructureNode>();
            }
            this.representative.nodes.put(key, e.getValue());
          }
        }
        this.representative.bText |= node.bText;
      }
      
      
   
      
      @Override public String toString() { return "" + this.occurrence.size() + " * " + occurrence.get(0).toString(); }
    }

    
    
    /**Stores all node types per tagName, with its occurrence in the structure file. 
     * */
    /**Stores all node types with occurrence more as one time, with tag name 
     * but with extra entry for any different content (really different type with same tag). 
     */
    IndexMultiTable<String, CfgSubtreeType2> allElementTypes2 = new IndexMultiTable<String, CfgSubtreeType2>(IndexMultiTable.providerString);
   
    
    
    /**Stores all node types for cfg subtree with the subtree name as key. */
    Map<String, XmlStructureNode> cfgSubtreeByName = new TreeMap<String, XmlStructureNode>();
    
    /**Stores the cfg subtree in the usage order. @since 2024-05 It seems to be that this is only
     * the order of output in the XML file by usage, but this output is not sensible. TODO  */
    Map<String, XmlStructureNode> cfgSubtreeList = new TreeMap<String, XmlStructureNode>();
    
    
    
    
    
    public XmlStructureData(XmlJzCfgAnalyzer this0) {
      this.this0 = this0;
    }


    private void createCfgSubtree(XmlStructureNode node, char nameModif) {
      CfgSubtreeType2 cfgSubtreeType = new CfgSubtreeType2(node);
      String key;
      if(nameModif < 'A') {
        key = node.tag;
      } else {
        key = node.tag + '_' + nameModif;    // if a node structure with same tag is used in a different semantic.
      }
      //do not so: node.sSubtreenode = key;
      if(node.attribs !=null) for(Map.Entry<String, AttribRead> e: node.attribs.entrySet()) {
        cfgSubtreeType.attributeNames.put(e.getKey(), e.getKey());  // first time, first attributes
      }
      if(node.nodes !=null) for(Map.Entry<String, XmlStructureNode> e: node.nodes.entrySet()) {
        cfgSubtreeType.nodeNames.put(e.getKey(), e.getKey());
      }
      if(allElementTypes2.get(node.tag) !=null) {
        Debugutil.stop();
      }
      if(allElementTypes2.get(node.tag) !=null) {
        Debugutil.stop();
      }
      allElementTypes2.add(node.tag, cfgSubtreeType);  //more as one with same tag name possible, store only for comparison (check)
      cfgSubtreeByName.put(key, node);   //unique cfg-subtree key, for usage.

    }
    
    
    /**Adds the occurrence of a sub node in the struct data. This is independent whether a cfg-subtree should be created. It is input for build a cfg-subtree.
     * It is possible that a config-subtree should be built though this node has no sub nodes.
     * @param node
     */
    void addStructureNodeOccurence(XmlStructureNode node) {
      CfgSubtreeType cfgSubtreeWithAllOccurences = this.this0.allElementTypes.get(node.tag); 
      if(cfgSubtreeWithAllOccurences == null) {
        cfgSubtreeWithAllOccurences = new CfgSubtreeType();
        this.this0.allElementTypes.put(node.tag, cfgSubtreeWithAllOccurences);
      }
      cfgSubtreeWithAllOccurences.occurrence.add(node);
    }
    
    
    
    /**Checks whether a element type is already existing with a score. 
     * 3/4 of all found attributes and sub elements are identically with another existing element
     * with the same tag name. 
     * Invoked for all element types found in the source XML tree.
     * If another node is found, the appropriated {@link CfgSubtreeType2} in {@link #allElementTypes2}
     * is supplemented ({@link CfgSubtreeType2#representative}.
     * If the node is a new one, {@link #createCfgSubtree(XmlStructureNode, char)} is called.
     * @param node
     */
    private void checkStructureNodeOccurence(XmlStructureNode node, List<XmlStructureNode> occurrences) {
      if(node.nodes == null && node.attribs == null) {
        return; //without sub nodes: does not need to store as subtree.
      }
      Iterator<CfgSubtreeType2> iterCfgSubtrees = allElementTypes2.iterator(node.tag);
      CfgSubtreeType2 cfgSubtreeType;
      char nameModif = 'A'-1;
      boolean found = false;
      if(node.tag.equals("AttributeList"))
        Debugutil.stop();
      if(!iterCfgSubtrees.hasNext() || !(cfgSubtreeType = iterCfgSubtrees.next()).tag.equals(node.tag)) {
        createCfgSubtree(node, '\0');                      //first occurrence with this tag.
      }
      else { //Same tag more as one.
        do {
          nameModif +=1;
          //check whether the found cfgSubtree seems to be the same type, because it has the same children:
          int nrfound=0, nrcount=0;
          if(node.attribs !=null) for(Map.Entry<String, AttribRead> e: node.attribs.entrySet()) {
            String key = e.getKey();
            if(cfgSubtreeType.attributeNames.get(key) !=null) {
              nrfound +=1;
            }
            nrcount +=1;
          }
          float nrOtherAttribsIncfgSubtree = cfgSubtreeType.attributeNames.size() - nrfound;
          float nrNewAttribs = nrcount - nrfound;
          float nrSameAttribs = nrfound;
          nrfound = nrcount = 0;
          if(node.nodes !=null) for(Map.Entry<String, XmlStructureNode> e: node.nodes.entrySet()) {
            String key = e.getKey();
            if(cfgSubtreeType.nodeNames.get(key) !=null) {
              nrfound +=1;
            }
            nrcount +=1;
          }
          int nrOtherSubnodesIncfgSubtree = cfgSubtreeType.nodeNames.size() - nrfound;
          int nrNewSubnodes = nrcount - nrfound;
          int nrNodes = nrfound;
          //================================================= Check whether a group of nodes can be found which has the same sub nodes
          //                                                  and have no relations to the other groups (they have other sub nodes)
          //List<XmlStructureNode> listFellow = new LinkedList<>();
          //List<XmlStructureNode> listNoFellow = new LinkedList<>();
          for(XmlStructureNode maybeFellow: occurrences) {
            nrfound = nrcount = 0;
            int mask = 1;
            if(node.nodes !=null) for(Map.Entry<String, XmlStructureNode> e: node.nodes.entrySet()) {
              String key = e.getKey();
              if(maybeFellow.nodes !=null && maybeFellow.nodes.get(key) !=null) {
//                nrfound +=1;
                node.mGroup |= mask;                        // mark the fellows in the mGroup
              }
//              nrcount +=1;
            }
//            if(nrfound ==0) {          //-------------------- no common sub nodes
//              listNoFellow.add(maybeFellow);
//            } else if(nrcount == nrfound) {  //-------------- at least one common sub node
//              listFellow.add(maybeFellow);
//            }
          }
          boolean otherNodeType = false; //nrSameAttribs  
          if(otherNodeType) { //most (3/4) nodes or attributes are non-identical: It is another cfgSubtree type.
            createCfgSubtree(node, nameModif);
          } else {
            found = true; //>= 3/4 all attrib and nodes are identically:
            cfgSubtreeType.occurrence.add(node);           // add the node instance as occurrence
            node.sSubtreenode = cfgSubtreeType.representative.sSubtreenode;  // mark this node occurrence with the subtree name.
            //assert(node.sSubtreenode !=null);
            cfgSubtreeType.addContentOfFoundNode(node);
            break;
          }
        } while(!found && iterCfgSubtrees.hasNext() && (cfgSubtreeType = iterCfgSubtrees.next()).tag.equals(node.tag));
        if(!found) {
        }
      }
    }
    
    
    
    
    /**Checks whether all occurrences of node types have the same meaning (semantic), should represent by one sub tree structure.
     * If the occurrences of one node have other inner sub node structure as the others, (building groups)
     * different sub tree structures results. 
     * TODO It is not ready yet for writing text nodes, and the XML output is too complex for repeat it.
     * Should be finished with
     * * Compare with cfgGiven
     * * take over NEW, ADD, TEXT from cfgGiven if existing
     * * clarify what are the two more for loops.  
     * This operation is not call yet. It was originally thought for sophisticated XML files in any automation designer. 
     * Do not remove it, test it if similar XML files are given.
     */
    protected void checkCfgSubtree ( XmlCfg cfgGiven ) {
      // TODO for the case more as one cfgGiven subtree build a Map with occurences of the config variants.
      for(CfgSubtreeType cfgSubtreeOccurrences: this.this0.allElementTypes.values()) {
        String sTag = cfgSubtreeOccurrences.occurrence.get(0).tag;
        XmlCfg.XmlCfgNode nodeGiven = cfgGiven == null ? null : cfgGiven.subtrees.get(sTag);
        if ( nodeGiven !=null   //--------------------------- the node is already found as SUBTREE in given cfg 
          || cfgSubtreeOccurrences.occurrence.size() >1) { // or more as one occurrence 
          if(nodeGiven !=null && cfgSubtreeOccurrences.occurrence.size() ==1)
            Debugutil.stop();
          for(XmlStructureNode structNode: cfgSubtreeOccurrences.occurrence ) {
            checkStructureNodeOccurence(structNode, cfgSubtreeOccurrences.occurrence);
          }            
        }
      }
      //TODO check all mGroup whether they are exclusively. 
      Debugutil.stop();
      //detect all dependencies in cfg-subtree, add to CfgSubtreeType2#dependings
      for(Map.Entry<String, CfgSubtreeType2> e: this.allElementTypes2.entrySet()) {
        CfgSubtreeType2 cfgSubtree = e.getValue();
        XmlStructureNode node= e.getValue().representative;
        if(!node.bDependencyChecked) {
          node.bDependencyChecked = true;
          checkUsageSubtreenode(cfgSubtree, node, 99);
        }
      }
      Debugutil.stop();
      for(Map.Entry<String, CfgSubtreeType2> e: this.allElementTypes2.entrySet()) {
        CfgSubtreeType2 cfgSubtree = e.getValue();
        processDependingCfgSubtree(cfgSubtree, 99);
      }
    }
    
    
    
    
    private void processDependingCfgSubtree(CfgSubtreeType2 cfgSubtree, int recursionCt) {
      if(recursionCt < 50)
        Debugutil.stop();
      assert(recursionCt >=0);
      if(!cfgSubtree.bSort ) {
        if(cfgSubtree.dependings.size()==0) {
          if(this.cfgSubtreeList.get(cfgSubtree.tag) !=null) {
            System.out.println("processDependingCfgSubtree twice: " + cfgSubtree.tag);
          }
          this.cfgSubtreeList.put(cfgSubtree.tag, cfgSubtree.representative);
          cfgSubtree.bSort = true;
        } else {
          cfgSubtree.bSort = true;  //prevent recursion on own depending 
          for(CfgSubtreeType2 dep: cfgSubtree.dependings) {
            processDependingCfgSubtree(dep, recursionCt-1);
          }
          //all dependencies processed.
          if(this.cfgSubtreeList.get(cfgSubtree.tag) !=null) {
            System.out.println("processDependingCfgSubtree-2 twice: " + cfgSubtree.tag);
          }
          this.cfgSubtreeList.put(cfgSubtree.tag, cfgSubtree.representative);
        }
      }
    }
    
    
    
    
    /**Adds all the {@link CfgSubtreeType2#dependings}
     * @param cfgSubtreeNeeds
     * @param node
     * @param recursiveCt
     */
    private void checkUsageSubtreenode(CfgSubtreeType2 cfgSubtreeNeeds, XmlStructureNode node, int recursiveCt) {
      assert(recursiveCt >= 0);
      if(node.nodes !=null) for(Map.Entry<String,XmlStructureNode> e_subnode: node.nodes.entrySet()) {
        XmlStructureNode subnode = (XmlStructureNode)e_subnode.getValue();
        if(subnode.sSubtreenode !=null) { //need as subtree
          if(!subnode.bDependencyChecked) {
            subnode.bDependencyChecked = true;
            //cfgSubtreeList.add(0, subnode);  //add before the evaluated node.
            CfgSubtreeType2 cfgSubtreeSub;
            Iterator<CfgSubtreeType2> iterSearch = allElementTypes2.iterator(subnode.tag);
            while(iterSearch.hasNext() && (cfgSubtreeSub = iterSearch.next()).tag.equals(subnode.tag)) {
              if(cfgSubtreeSub.representative.sSubtreenode.equals(subnode.sSubtreenode)) {
                cfgSubtreeNeeds.dependings.add(cfgSubtreeSub);
                checkUsageSubtreenode(cfgSubtreeSub, cfgSubtreeSub.representative, recursiveCt -1);  //firstly check this usage.
                break;
              }
            }
        } }
        else {
          checkUsageSubtreenode(cfgSubtreeNeeds, subnode, recursiveCt -1);  //not in cfg-subtree, process it.
        }
      }
    }
    
    
    
    
    
    
    
    
  }
  
  
  
  public static class AttribRead {
    String namespace;
    String name;
    /**storage type or access routine */
    String value;
  }

  

  /**This class is one node for one element to store the structure of a XML file via {@link XmlJzReader}.
   * <ul>
   * <li>An instance of this class should be created as root node. The root node from the XML file is stored there as the first level.
   * It means, this root instance is not presented in the XML file, and the root instance has one member: The root node of the XML file.
   * <li>Instances are created for any found element with different tags in the XML tree. This instances are stored in the internal {@link #nodes} tree.
   * <li>If elements with the same tag name are found on one level of the XML file, only one instance of this is stored in {@link #nodes}.
   *   It means an instance presents not the content of the XML file but the structure. An instance of this contains the sum of all found attributes.
   * <li>If an element with a known tag is found in a deeper level of the XML file,    
   * </ul> 
   * The information of the built tree of this class can be used to 
   * <ul>
   * <li>Write a config file template, using {@link XmlJzCfgAnalyzer#writeCfgTemplate(File)}
   * </ul>
   * This class builds the root node and any child node too.
   * <pre>
   * XmlCfgNodeBuilder<*>--nodes-->XmlCfgNodeBuilder
   *                                  <*> 
   *                                   |
   *                                   +--attribs--->String
   * </pre>
   * @author Hartmut Schorrig
   *
   */
  static class XmlStructureNode
  {
    
    /**Tag name of the element. */
    final String tag;
    
    final XmlStructureNode parent;
    
    /**Found sub nodes. The list is supplemented if new sub nodes are found on further occurrences of elements. 
     * It contains the same tag element only one time. that is tested in {@link #addElement(String)}.
     */
    Map<String, XmlStructureNode> nodes;
    
    /**Found attributes. The list is supplemented if new attribute names are found on further occurrences of elements. */
    Map<String, AttribRead> attribs;
    
    
    /**Set if new attributes are found. Then the */
    boolean bNewAttributes = false; 

    /**The declared name spaces for this node. */
    Map<String, String> nameSpaces;

    List<String> attribsUnnamed;
    

    /**Tag name of the element as written in Java code, only identifier chars. */
    final String tagIdent;
    
    /**Name of the representation of the node structure in a subtree. 
     * After recognizing (selection) of the node, in the {@link XmlCfg}, 
     * the {@link org.vishia.xmlReader.XmlCfg.XmlCfgNode} can be substituted with the subtree content with this name.*/
    String sSubtreenode;
    
    /**Set if at least one of the occurrences has a text content.*/
    boolean bText = false;
    
    boolean XXXbDetermineWithParent = false;
    
    boolean bDependencyChecked;
    
    
    //boolean bUseSubtree;
    
    /**Set if the node is only one time in all found trees. 
     * It means that a variable is sufficient to store the content of all attributes and the text if no subNodes exists.
     */
    boolean onlySingle = true;
    
    
    /**Only for test whether or not a node is found twice. If twice, {@link #onlySingle} is set to false for this node. */
    Map<String, String> nodesLocal;
    
    int nrofAttributes = 0;
    
    
    
    final XmlStructureData xmlStructData;
    
    /**This bit mask is set by one bit per occurrence of a node with same tag 
     * to characteristic groups with same sub nodes.
     */
    long mGroup;
    
    XmlStructureNode(XmlStructureNode parent, String tag, XmlStructureData xmlStructData){ 
      this.parent = parent; 
      this.tag = tag;
      this.tagIdent = StringFunctions_B.replaceNonIdentifierChars(tag, '-').toString();
      
      this.xmlStructData = xmlStructData; 
    }
    
    
    
    
    
    /**It is invoked via reflection from {@link XmlJzCfgAnalyzer#newCfgReadStruct()}
     * @param tag
     * @return
     */
    public void addAttribute(String namespacename, String attribValue) { 
      AttribRead attrib = new AttribRead();
      //replaces the ':' between namespace:name with _
      attrib.value = attribValue;
      int posNamespace = namespacename.indexOf(':');
      if(posNamespace >=0) {
        attrib.namespace = namespacename.substring(0, posNamespace);
        attrib.name = namespacename.substring(posNamespace+1);
      } else {
        attrib.name = namespacename;
      }
      //if(namespacename.equals("draw:fit-to-size")) Debugutil.stopp();
      if(this.attribs == null) { 
        this.attribs = new TreeMap<String, AttribRead>(); 
        this.bNewAttributes = true;
        this.attribs.put(namespacename, attrib);  //the first attrin
      }
      else if(this.attribs.get(namespacename) ==null) {
        this.bNewAttributes = true;
        this.attribs.put(namespacename, attrib);  //a new attrib
      }
    }

    /**Adds a value String found in the data presentation, which is not designated by a name.
     * It may be designated by its distinguished content or by its position.
     * The post preparation should decide about usage.
     * @param value value to add to {@link #attribsUnnamed}.
     */
    public void addAttributeUnnamed(String value) {
      if(this.attribsUnnamed == null) { this.attribsUnnamed = new LinkedList<String>(); }
      this.attribsUnnamed.add(value);
    }

    void putSubnode(XmlStructureNode subNode) {
      if(this.nodes == null) { this.nodes = new TreeMap<String, XmlStructureNode>(); }
      this.nodes.put(subNode.tag, subNode);
    }

    /**Returns the instance to store the occurrence of a XML-element for the given tag name.
     * If more XML-elements with the same tag name are found in any node, only one occurrence for this element tag is stored.
     * With them maybe new occurring attributes or new occurring elements are stored. 
     * <br>Additionally via {@link XmlStructureData#addStructureNodeOccurence(XmlStructureNode)} 
     *   all elements in the whole xml file with the same tag are registered any later evaluated, if there are semantically also the same. 
     * <br>This operation is invoked via reflection from {@link XmlJzCfgAnalyzer#newCfgReadStruct()}
     * @param tag
     * @return
     */
    public XmlStructureNode addElement(String tag) { 
      //if(tag.equals("style:style")) Debugutil.stopp();
      //if(tag.contains("   ")) Debugutil.stopp();
      //if(tag.equals("Document")) Debugutil.stopp();
      if(this.nodes == null) {
        this.nodes = new TreeMap<String, XmlStructureNode>();
      }
      if(this.nodesLocal == null) {
        this.nodesLocal = new TreeMap<String, String>();
      }
      //Note: this is a node inside the yet read XML tree, see this.parent
      //      this.xmlStructData is the access to common data from the cfgAnalyzer.
      XmlStructureNode subNode = (XmlStructureNode)this.nodes.get(tag); //use existent one with same tag to strore further content.
      if(subNode == null) {
        //if(tag.equals("style:style")) Debugutil.stopp();
        subNode = new XmlStructureNode(this, tag, this.xmlStructData); 
        this.nodes.put( tag, subNode);
        this.xmlStructData.addStructureNodeOccurence(subNode); //it helps to find subtrees, register each node type
      } else { 
        //the node is known already, use it, supplement new elements and attributes.
        subNode.onlySingle = false;
        if(subNode.parent != this) Debugutil.stopp();
      }
      if(this.nodesLocal.get(tag)!=null) { 
        subNode.onlySingle = false; //at least twice in this tree.
      }
      this.nodesLocal.put(tag, tag); //to detect whether it occurs a second one
      subNode.nodesLocal = null;
      return subNode; 
    }
    
    
    
    public void addNamespace(String key, String value) {
      if(this.nameSpaces == null) { this.nameSpaces = new TreeMap<String, String>(); }
      this.nameSpaces.put(key, value);
//TODO      if(nameSpacesAll == null) { this.nameSpacesAll = new TreeMap<String, String>(); }
//      this.nameSpacesAll.put(key, value);
    }
    
    
    
    /**It is invoked via reflection from {@link XmlJzCfgAnalyzer#newCfgReadStruct()}
     * @param tag
     * @return
     */
    public void setAttribute(String namespacename) { 
      //replaces the ':' between namespace:name with _
      String value = "set_" +  // operation in java to set the attribute value. 'value' is the internal variable in XmlJzReader
                      StringFunctions_B.replaceNonIdentifierChars(namespacename, '-').toString()
                   + "(value)";  ;
      addAttribute(namespacename, value);
    }

    
    
    public void setTextOccurrence() { this.bText = true; }
   
    
    
    void writeNodeData ( Appendable out, int indent) throws IOException {
      ApplMain.outIndent(out, indent);
      out.append('<').append(this.tag).append(' ').append(this.tagIdent).append(' ').append(this.sSubtreenode).append(' ');
      if(this.bText) { out.append(" TEXT "); }
      if(this.bDependencyChecked) { out.append(" DEPCHECKED "); }
      if(this.onlySingle) { out.append(" SINGLE "); }
      out.append('\n');
      if(this.attribs !=null) {
        for(Map.Entry<String, AttribRead> e_attrib : this.attribs.entrySet()) {
          ApplMain.outIndent(out, indent +2);
          AttribRead attrib = e_attrib.getValue();
          out.append('@');
          if(attrib.namespace !=null) { out.append(attrib.namespace).append(':'); }
          out.append(attrib.name).append(" = ").append(attrib.value).append("\n");
        }
      }
      
    }
    
    void writeData ( Appendable out, int indent) throws IOException {
      writeNodeData(out, indent);
      if(this.nodes !=null) {
        for(Map.Entry<String, XmlStructureNode> e_node : this.nodes.entrySet()) {
          XmlStructureNode node = (XmlStructureNode)e_node.getValue();
          node.writeData(out, indent +2);
        }
      }
      ApplMain.outIndent(out, indent);
      out.append(">\n");
    }
    
    @Override public String toString(){ return this.tag + (this.attribs != null ? " attr:" + this.attribs.toString():"") + (this.nodes !=null ? " nodes:" + this.nodes.toString() : ""); }
    
  }

  
  
}
