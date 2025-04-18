package org.vishia.xmlReader;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.vishia.genJavaOutClass.GenJavaOutClass;
import org.vishia.mainCmd.MainCmd;
import org.vishia.mainCmd.MainCmdLogging_ifc;
import org.vishia.msgDispatch.LogMessage;
import org.vishia.util.DataAccess;
import org.vishia.util.Debugutil;
import org.vishia.util.StringFunctions_B;
import org.vishia.xmlReader.XmlCfg.AttribDstCheck;
import org.vishia.xmlReader.XmlCfg.XmlCfgNode;



/**Generates Source.java adequate the given XML cfg data to store the results there.
 * This should only be invoked if an unknown XML file should be analyzed.
 * 
 */
public class GenXmlCfgJavaData {
  
  
  /**Version, history and license.
   * <ul>
   * <li>2024-05-09: new {@link #exec(XmlCfg)} with a given non XML read XmlCfg
   * <li>2024-05-09: Using {@link LogMessage}, MainCmd is deprecated.
   * <li>2024-05-09: {@link WrClassXml#evaluateChildren(String, XmlCfgNode, SubClassXml, boolean, int)} and also
   *   {@link WrClassXml#wrVariable(SubClassXml, String, String, String, org.vishia.util.DataAccess.DatapathElement, boolean, boolean, boolean)} 
   *   now with argument sOuterClass, this was missing till now, (manual adjusted in generated sources)
   * <li>2022-06-23: set type_ns to produce _Zbnf suffix ("<:if:typeNs>_Zbnf<.if>" in GenJavaOutClass) 
   * <li>2022-02-18 only adapted to {@link GenJavaOutClass}, argument typeNs should be null. 
   * <li>201x: Hartmut www.vishia.org creation
   * </ul>
   * <ul>
   * <li>new: new functionality, downward compatibility.
   * <li>fnChg: Change of functionality, no changing of formal syntax, it may be influencing the functions of user,
   *            but mostly in a positive kind. 
   * <li>chg: Change of functionality, it should be checked syntactically, re-compilation necessary.
   * <li>adap: No changing of own functionality, but adapted to a used changed module.
   * <li>corr: correction of a bug, it should be a good thing.
   * <li>bug123: correction of a tracked bug.
   * <li>nice: Only a nice correction, without changing of functionality, without changing of syntax.
   * <li>descr: Change of description of elements.
   * </ul> 
   * <br><br>
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
   */
  public static final String sVersion = "2022-02-18";

  
  
  /**Instances of this class describe a sub class in generated code.
   */
  static class SubClassXml extends GenJavaOutClass.SubClassJava{

    
    XmlCfgNode subItem;
    
    
    SubClassXml(String semantic, String className)
    { super(semantic, className, null);  //superType
    }
  
  }


  protected final GenJavaOutClass.CmdArgs cmdArgs;
  
  protected final LogMessage log;
  
  private final GenJavaOutClass genJava;
  
  
  
  protected final GenJavaOutClass genClass;
  

  protected Map<String, XmlCfgNode> subtrees;
  
  public GenXmlCfgJavaData(GenJavaOutClass.CmdArgs cmdArgs, LogMessage log) {
    this.cmdArgs = cmdArgs;
    this.genJava = new GenJavaOutClass(cmdArgs, log);
    this.genClass = new GenJavaOutClass(cmdArgs, log);
    this.log = log;
  }
  
  
  public void exec() throws IOException {
    String fInName = this.cmdArgs.fileInput.getName();
    final XmlCfg xmlCfg;
    if(fInName.endsWith(".xml")) {
      XmlJzReader xmlReader = new XmlJzReader();
      xmlReader.readCfg(this.cmdArgs.fileInput);
      xmlCfg = xmlReader.cfg;
    } else {
      xmlCfg = new XmlCfg(true);
      xmlCfg.readCfgFile(this.cmdArgs.fileInput, this.log);
    }
    exec(xmlCfg);
  }
    
    
  public void exec(XmlCfg xmlCfg) {

    this.subtrees = xmlCfg.subtrees;
    Debugutil.stop();
    
    this.genClass.setupWriter();
    WrClassXml wrClass = this.new WrClassXml();  //the main class to write
    try {
      XmlCfgNode rootNode = xmlCfg.rootNode;
      SubClassXml classDataRoot = new SubClassXml("root", "root");
      classDataRoot.subItem = rootNode;
      String sOuterClass = this.cmdArgs.sJavaClass + ".";
      wrClass.evaluateChildren(sOuterClass, rootNode, classDataRoot, false, 1);
      wrClass.wrClassJava.writeOperations();
      //
      //
      //
      int ixCmpn = 0;
      while(this.genClass.listCmpn.size() > ixCmpn) { //possible to add on end in loop
        SubClassXml classData = (SubClassXml)this.genClass.listCmpn.get(ixCmpn++);
        wrClass = new WrClassXml();
        wrClass.wrClassJava.wrClassCmpn(classData, null);
        //
        //
        wrClass.evaluateChildren(sOuterClass, classData.subItem, classData, false, 0);
        //
        wrClass.wrClassJava.writeOperations();
        //
        this.genClass.finishCmpnWrite();

      }
      //
      this.genClass.finishClassWrite();
      //
    } catch (Exception e1) {
      // TODO Auto-generated catch block
      System.err.println(e1.getMessage());
    }
    this.genClass.closeWrite();    

    
  }

  
  
  
  
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
    String sRet;
    if(sArgs.length == 0) {
      System.out.println("java -cp .... org.vishia.xmlReader.GenXmlCfgJavaData -cfg:INFILE -dirJava:PATH -pkg:PKG -class:CLASS\n"
          + "  -cfg:INFILE: The config.xml file as config file for XmlJzReader\n"
          + "  -dirJava:path/to/javaSrcRoot to create\n"
          + "  -pkg:my.pkg.path The package path\n"
          + "  -class:MyClass without .java, class to create");
      sRet = "";
    } else {

      GenJavaOutClass.CmdArgs args = new GenJavaOutClass.CmdArgs();
      CmdLine mainCmdLine = new CmdLine(args, sArgs); //the instance to parse arguments and others.
      try{
        mainCmdLine.addCmdLineProperties();
        boolean bOk;
        try{ bOk = mainCmdLine.parseArguments(); }
        catch(Exception exception)
        { mainCmdLine.report("Argument error:", exception);
          mainCmdLine.setExitErrorLevel(MainCmdLogging_ifc.exitWithArgumentError);
          bOk = false;
        }
        if(bOk)
        { GenXmlCfgJavaData main = new GenXmlCfgJavaData(args, mainCmdLine);     //the main instance
          /* The execution class knows the SampleCmdLine Main class in form of the MainCmd super class
             to hold the contact to the command line execution.
          */
          try{ main.exec(); }
          catch(Exception exc)
          { //catch the last level of error. No error is reported direct on command line!
            System.err.println(exc.getMessage());
          }
        }
        sRet = "";
      } catch(Exception exc){
        sRet = exc.getMessage();
      }
      if(shouldExitVM) { mainCmdLine.exit(); }
    }
    return sRet;
  }

  

  
  private class WrClassXml {

    
    final GenJavaOutClass.WrClassJava wrClassJava;
    
    
    WrClassXml() {
      this.wrClassJava = GenXmlCfgJavaData.this.genClass.new WrClassJava();
    }
    
    
    private SubClassXml getRegisterSubclass(String name, XmlCfgNode cfgItem) {
      SubClassXml classData = (SubClassXml)genClass.idxRegisteredCmpn.get(name);
      if(classData == null) {
        classData = new SubClassXml(name, GenJavaOutClass.firstUppercaseIdentifier(name));
        classData.sDbgIdent = "xxx";
        classData.subItem = cfgItem;
        genClass.idxRegisteredCmpn.put(name, classData);
        genClass.listCmpn.add(classData);
        ////
      }
      return classData;
    }

    /**This routine is called for <code>&lt;cmpnSyntax...></code>.
     * <ul>
     * <li>The component is searched in {@link #idxSubSyntax}. It should be found, elsewhere it is an IllegalArgumentException
     * <li>The semantic is taken either from the component's definition if it is not given on the item.
     *   In this case it is written in source like <code>&lt;component></code> and item[@link #sSemantic} contains "@"-
     * <li>In that case the semantic is usual the syntax identifier.
     * <li>In that case it can be abbreviating because <code>component::=&lt;?semantic></code> is given.
     * <li>In that case the semantic can be null if <code>component::=&lt;?></code> is given. See next List.
     * <li>The semantic is taken from the item if <code>&lt;component?semantic></code> is given.
     * <li>The semantic is null if <code>&lt;component?></code> is given.
     * </ul>
     * Depending on semantic == null:
     * <ul>
     * <li>If the semantic is null, then the component's syntax definition is used
     * and the component's data are created in this class.  
     * <li>If the semantic is given then a container for the component's data is created in this class 
     *   via {@link #wrVariable(String, String, boolean, boolean)}
     *   and the component's name is {@link #registerCmpn(String)} to create a class for it later if not created already. 
     * </ul>  
     * @param item The calling item of the component
     * @param bList true if the syntax is part of a repetition
     * @param level
     * @throws IOException
     */
    private void evaluateSubCmpn(XmlCfgNode item, boolean bList, int level) throws Exception {
      
      
    }
    
    
    
    void evaluateChildren(String sOuterClass, XmlCfgNode cfgNode, SubClassXml classData, boolean bList, int level) throws Exception {
      System.out.println(cfgNode.tag);
//      if(cfgNode.tag.equals("section"))
//        Debugutil.stop();
      if(cfgNode.attribs !=null) for(Map.Entry<String, AttribDstCheck> eattrib: cfgNode.attribs.entrySet()) {
        AttribDstCheck attrib = eattrib.getValue();
        //if(attrib.daccess !=null) {
        String sName = StringFunctions_B.replaceNonIdentifierChars(attrib.name, '-').toString();
        wrVariable(classData, null, "String", sName, attrib.daccess, "Attribute " + attrib.name, true, false, false);
      }
      if(cfgNode.subnodes !=null) for(Map.Entry<String, XmlCfgNode> eNode: cfgNode.subnodes.entrySet()) {
        XmlCfgNode childNode = eNode.getValue();
//        if(childNode.tag.equals("section"))
//          Debugutil.stop();
        if(childNode.dstClassName !=null) {
          String sType = GenJavaOutClass.firstUppercaseIdentifier(childNode.dstClassName);
          if(GenXmlCfgJavaData.this.genClass.idxStdTypes.get(sType) !=null) {
            sType += "__";  //It must not be a Standard type.
          }
          SubClassXml metaClass = getRegisterSubclass(sType, childNode); //idxMetaClass.get(semantic1);
          String sName = StringFunctions_B.replaceNonIdentifierChars(childNode.tag, '-').toString();
          wrVariable(classData, sOuterClass, sType, sName, childNode.elementStorePath, "Complex node " + childNode.tag, false, childNode.bList, true);  //write the create routine and access
        } else {
          String sName = StringFunctions_B.replaceNonIdentifierChars(childNode.tag, '-').toString();
          wrVariable(classData, null, "String", sName, childNode.elementStorePath, "Simple node " + childNode.tag, true, false, false);
            
        }
      }
      this.wrClassJava.writeListAllNodes(null);            // if at least one list node exists, then create a NameObject list which contains all nodes in natural order:
    }
    
    
    

    /**
     * It calls {@link org.vishia.genJavaOutClass.GenJavaOutClass.WrClassJava#wrVariable(org.vishia.genJavaOutClass.GenJavaOutClass.SubClassJava, String, String, String, String, boolean, boolean, boolean, List)} 
     * 
     * @param classData
     * @param typeNs Either null for a standard type, or the name of the environment class where all types are defined as sub type, then with trainling "."
     * @param type
     * @param varName
     * @param storePath
     * @param bStdType
     * @param bList
     * @param bCmpn
     * @throws Exception
     */
    protected void wrVariable(SubClassXml classData, String sOuterClass, String type, String varName, DataAccess.DatapathElement storePath, String sDocu, boolean bStdType, boolean bList, boolean bCmpn
    ) throws Exception {
      if(varName !=null && varName.length() >0) { //else: do not write, parsed without data
        if(varName.startsWith("ST"))
          Debugutil.stop();
        if(type.equals("Section_A"))
          Debugutil.stop();
        String sTypeExist = wrClassJava.variables.get(varName);
        if(sTypeExist !=null) {
          if(! sTypeExist.equals(type)) {
            throw new IllegalArgumentException("Semantic " + varName + " with different types");
          }
        } else {
          if(type.equals("Integer")) { 
            type = "int"; 
          }
          if(varName.equals("FBType")) {  //a required Attribute in XML
            Debugutil.stop();
          }
          if(varName.indexOf("@")>=0) {  //a required Attribute in XML
            Debugutil.stop();
          }
          List<String> args = null;
          if(bCmpn) {
            int nrArgs = storePath.nrArgNames();
            if(nrArgs >0) {
              args = new LinkedList<String>();
              for(int ixArg = 0; ixArg < nrArgs; ++ixArg) {
                String operArg = storePath.argName(ixArg);
                args.add(GenJavaOutClass.firstLowercaseIdentifier(operArg));
              }
            }
          }
          //semantic = semantic.replace("@!", "");
          this.wrClassJava.wrVariable(classData, varName, sOuterClass, type, type, sDocu, bStdType, bList, bCmpn, args); 
          
        }
      }
    }

    private void registerCmpn(String name) {
      if(genClass.idxRegisteredCmpn.get(name) == null) {
        XmlCfgNode cmpn = subtrees.get(name);
        if(cmpn == null) {
          throw new IllegalArgumentException("syntax component not found: " + name);
        }
        SubClassXml classData = new SubClassXml(name, GenJavaOutClass.firstUppercaseIdentifier(cmpn.tag.toString()));
        classData.sDbgIdent = cmpn.tag.toString();
        classData.subItem = null; //cmpn;
        genClass.idxRegisteredCmpn.put(name, classData);
        genClass.listCmpn.add(classData);
        ////
      }
    }
    
  }
  
  
  
  /**The inner class CmdLine helps to evaluate the command line arguments
   * and show help messages on command line.
   */
  private static class CmdLine extends MainCmd
  { 
  
    
    
    public final MainCmd.Argument[] defArguments =
    { new MainCmd.Argument("-cfg", ":<fileCfg.xml>    Xml cfg file", new MainCmd.SetArgument(){ 
      @Override public boolean setArgument(String val){ 
        argData.fileInput = new File(val);  return true;
      }})
      , new MainCmd.Argument("-struct", ":<fileStruct.txt> out file to show the struct, optional", new MainCmd.SetArgument(){ 
        @Override public boolean setArgument(String val){ 
          argData.fileOutStruct = new File(val);  return true;
        }})
      , new MainCmd.Argument("-dirJava", ":<dirJava>    directory for Java output", new MainCmd.SetArgument(){ 
        @Override public boolean setArgument(String val){ 
          argData.dirJava = new File(val);  return true;
        }})    
      , new MainCmd.Argument("-pkg", ":<pkg.path>    directory for Java output", new MainCmd.SetArgument(){ 
        @Override public boolean setArgument(String val){ 
          argData.sJavaPkg = val;  return true;
        }})    
      , new MainCmd.Argument("-class", ":<class>     name and file <class>.java for Java output", new MainCmd.SetArgument(){ 
        @Override public boolean setArgument(String val){ 
          argData.sJavaClass = val;  return true;
    }})    
    };

    public final GenJavaOutClass.CmdArgs argData;
    
    /*---------------------------------------------------------------------------------------------*/
    /**Constructor of the cmdline handling class.
    The command line arguments are parsed here. After them the execute class is created as composition of SampleCmdLine.
*/
    protected CmdLine(GenJavaOutClass.CmdArgs argData, String[] sCmdlineArgs)
    { super(sCmdlineArgs);
      this.argData = argData;
    }
    

    void addCmdLineProperties(){
      super.addAboutInfo("Generate Java source code as destination class from XML cfg file");
      super.addAboutInfo("made by HSchorrig, 2019-08-16..2019-08-29");
      super.addArgument(defArguments);
      super.addHelpInfo("==Standard arguments of MainCmd==");
      super.addStandardHelpInfo();
    }
    
  
  
  
    /*---------------------------------------------------------------------------------------------*/
    /**Checks the cmdline arguments relation together.
       If there is an inconsistents, a message should be written. It may be also a warning.
       @return true if successfull, false if failed.
    */
    @Override
    protected boolean checkArguments()
    { boolean bOk = true;
  
      if(this.argData.fileInput == null)            { bOk = false; writeError("ERROR argument Syntaxfile is obligat."); }
      else if(!this.argData.fileInput.exists())     { bOk = false; writeError("ERROR argument Syntaxfile not found " + argData.fileInput.getAbsolutePath()); }
      else if(this.argData.fileInput.length()==0)   { bOk = false; writeError("ERROR argument Syntaxfile without content.");}
  
      return bOk;
  
   }
  }//class CmdLine


  
  
}
