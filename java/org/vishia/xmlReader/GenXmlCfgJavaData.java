package org.vishia.xmlReader;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.vishia.genJavaOutClass.GenJavaOutClass;
import org.vishia.mainCmd.MainCmd;
import org.vishia.mainCmd.MainCmdLogging_ifc;
import org.vishia.util.DataAccess;
import org.vishia.util.Debugutil;
import org.vishia.util.StringFunctions_B;
import org.vishia.xmlReader.XmlCfg.AttribDstCheck;
import org.vishia.xmlReader.XmlCfg.XmlCfgNode;



public class GenXmlCfgJavaData {
  
  
  /**Version, history and license.
   * <ul>
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


  
  
  private final GenJavaOutClass genJava;
  
  
  XmlJzReader xmlReader = new XmlJzReader();
  
  private final GenJavaOutClass genClass;
  

  Map<String, XmlCfgNode> subtrees;
  
  GenXmlCfgJavaData(GenJavaOutClass.CmdArgs cmdArgs, MainCmdLogging_ifc log) {
    this.genJava = new GenJavaOutClass(cmdArgs, log);
    this.genClass = new GenJavaOutClass(cmdArgs, log);
  }
  
  
  public void exec(File fileXmlCfg) throws IOException {
    xmlReader.readCfg(fileXmlCfg);
    XmlCfg xmlCfg = xmlReader.cfg;
    subtrees = xmlCfg.subtrees;
    Debugutil.stop();
    
    genClass.setupWriter();
    WrClassXml wrClass = this.new WrClassXml();  //the main class to write
    try {
      XmlCfgNode rootNode = xmlCfg.rootNode;
      SubClassXml classDataRoot = new SubClassXml("root", "root");
      classDataRoot.subItem = rootNode;
      wrClass.evaluateChildren(rootNode, classDataRoot, false, 1);
      wrClass.wrClassJava.writeOperations();
      //
      //
      //
      int ixCmpn = 0;
      while(genClass.listCmpn.size() > ixCmpn) { //possible to add on end in loop
        SubClassXml classData = (SubClassXml)genClass.listCmpn.get(ixCmpn++);
        wrClass = new WrClassXml();
        wrClass.wrClassJava.wrClassCmpn(classData, null);
        //
        //
        wrClass.evaluateChildren(classData.subItem, classData, false, 0);
        //
        wrClass.wrClassJava.writeOperations();
        //
        genClass.finishCmpnWrite();

      }
      //
      genClass.finishClassWrite();
      //
    } catch (Exception e1) {
      // TODO Auto-generated catch block
      System.err.println(e1.getMessage());
    }
    genClass.closeWrite();    

    
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
          try{ main.exec(args.fileInput); }
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
      wrClassJava = genClass.new WrClassJava();
    }
    
    
    private SubClassXml getRegisterSubclass(String name, XmlCfgNode cfgItem) {
      SubClassXml classData = (SubClassXml)genClass.idxRegisteredCmpn.get(name);
      if(classData == null) {
        classData = new SubClassXml(name, GenJavaOutClass.firstUppercase(name));
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
    
    
    
    void evaluateChildren(XmlCfgNode cfgNode, SubClassXml classData, boolean bList, int level) throws Exception {
      System.out.println(cfgNode.tag);
//      if(cfgNode.tag.equals("section"))
//        Debugutil.stop();
      if(cfgNode.attribs !=null) for(Map.Entry<String, AttribDstCheck> eattrib: cfgNode.attribs.entrySet()) {
        AttribDstCheck attrib = eattrib.getValue();
        //if(attrib.daccess !=null) {
        String sName = StringFunctions_B.replaceNonIdentifierChars(attrib.name, '-').toString();
        wrVariable(classData, "String", sName, attrib.daccess, true, false, false);
      }
      if(cfgNode.subnodes !=null) for(Map.Entry<String, XmlCfgNode> eNode: cfgNode.subnodes.entrySet()) {
        XmlCfgNode childNode = eNode.getValue();
//        if(childNode.tag.equals("section"))
//          Debugutil.stop();
        if(childNode.dstClassName !=null) {
          String sType = GenJavaOutClass.firstUppercase(childNode.dstClassName);
          if(GenXmlCfgJavaData.this.genClass.idxStdTypes.get(sType) !=null) {
            sType += "__";  //It must not be a Standard type.
          }
          SubClassXml metaClass = getRegisterSubclass(sType, childNode); //idxMetaClass.get(semantic1);
          String sName = StringFunctions_B.replaceNonIdentifierChars(childNode.tag, '-').toString();
          wrVariable(classData, sType, sName, childNode.elementStorePath, false, childNode.bList, true);  //write the create routine and access
        
        } else {
          String sName = StringFunctions_B.replaceNonIdentifierChars(childNode.tag, '-').toString();
          wrVariable(classData, "String", sName, childNode.elementStorePath, true, false, false);
            
        }
      }
    }
    
    
    

    protected void wrVariable(SubClassXml classData, String type, String varName, DataAccess.DatapathElement storePath, boolean bStdType, boolean bList, boolean bCmpn
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
                args.add(GenJavaOutClass.firstLowercase(operArg));
              }
            }
          }
          //semantic = semantic.replace("@!", "");
          String typeNs = "Type__Ns";
          wrClassJava.wrVariable(classData, varName, typeNs, type, type, bStdType, bList, bCmpn, args); 
          
        }
      }
    }

    private void registerCmpn(String name) {
      if(genClass.idxRegisteredCmpn.get(name) == null) {
        XmlCfgNode cmpn = subtrees.get(name);
        if(cmpn == null) {
          throw new IllegalArgumentException("syntax component not found: " + name);
        }
        SubClassXml classData = new SubClassXml(name, GenJavaOutClass.firstUppercase(cmpn.tag.toString()));
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
