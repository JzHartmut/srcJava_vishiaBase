package org.vishia.genJavaOutClass;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.vishia.mainCmd.MainCmdLogging_ifc;
import org.vishia.util.Debugutil;
import org.vishia.util.FileSystem;
import org.vishia.util.OutTextPreparer;




/**Basic functionality to generate a Java class proper for storing results from Zbnf parser and from XmlJzReader. 
 * This class is used from {@link org.vishia.zbnf.GenZbnfJavaData} and from {@link org.vishia.xmlReader.GenXmlCfgJavaData}.
 * For generating the source the {@link OutTextPreparer} is used. It means the sources are given as templates, 
 * one for each part. 
 * <br><br>
 * Comparison {@link OutTextPreparer} with the JZtxtcmd script executer:<br> 
 * The {@link OutTextPreparer} is similar the {@link org.vishia.jztxtcmd.JZtxtcmd} but more simple in execution. 
 * For JZtxtcmd the template for the sources is really given in a textual file. 
 * Here the template for the sources are contained in this class as Strings. 
 * It may be possible to read the Strings from a file, for more flexibility to change the sources. 
 * */
public class GenJavaOutClass {

  /**Version, history and license.
   * <ul>
   * <li>2019-08-17 Hartmut only comments
   * <li>2019-08-17 Hartmut creation copied and reduced from {@link org.vishia.zbnf.GenZbnfJavaData}. It is compare able
   *   with the last version from this class. Some changes made. Tested. 
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
   * 
   */
  public static final String sVersion = "2019-08-17";

  
  
  
  
  /**Command line args */
  public static class CmdArgs{
    
    /**The input file which determines the structure. */
    public File fileInput = null;
    
    public File dirJava;
    
    public String sJavaPkg;
    
    public String sJavaClass;
  
  }

  public static class SubClassField {
    final String type;
    final String varName;
    
    final String semantic;
    
    public SubClassField(String type, String name, String semantic)
    { this.type = type;
      this.varName = name;
      this.semantic = semantic;
    }
    
  }
  
  
  
  /**Instances of this class describe a sub class in generated code.
   */
  public static class SubClassJava {
    protected final String className;
    
    /**This Map is filled if any [&lt;?semantic> or [&lt;?semantic/@attr] are found.  
     * It produces a String field with the semantic respecitvely the attr name. 
     */
    public TreeMap<String, SubClassField> fieldsFromSemanticAttr;
    
    /**The semantic identifier how given in parse script. */
    public final String semantic;
    
    public String sDbgIdent;
    
    public SubClassJava(String semantic, String className)
    { this.semantic = semantic;
      this.className = className;
    }
    
    
  }
  
  

  
  
  /**Command line args */
  private final CmdArgs cmdArgs;

  private final MainCmdLogging_ifc log;
  
  
  /**Writer for the base data class and the Zbnf JavaOut class.
   * 
   */
  private Writer wr, wrz;

  /**StandardTypes. */
  public final TreeMap<String, String> idxStdTypes = new TreeMap<String, String>();

  
  
  
  
  /**The syntax components which are to process yet (are used for parse result storing).  */
  public List<SubClassJava> listCmpn = new LinkedList<SubClassJava>();
  
  /**Index of already registered components to add in {@link #listCmpn} only one time. */
  public Map<String, SubClassJava> idxRegisteredCmpn = new TreeMap<String, SubClassJava>();
  
  private final static Map<String, String> reservedNames = new TreeMap<String, String>();
  
  /**Text for Java header. */
  private final OutTextPreparer sJavaHead = new OutTextPreparer("sJavaHead", null, "pkgpath, javaclass", 
      "package <&pkgpath>;\n"
    + "\n"
    + "import java.util.LinkedList;\n"
    + "import java.util.List;\n"
    + "\n"
    + "/**This file is generated by genJavaOut.jzTc script. */\n"
    + "public class <&javaclass> {\n"
    + "\n");
  
  /**Text for Java header for Zbnf writer class. */
  private final OutTextPreparer sJavaHeadZbnf = new OutTextPreparer("sJavaHeadZbnf", null, "pkgpath, javaclass", 
      "package <&pkgpath>;\n"
    + "\n"
    + "import java.util.LinkedList;\n"
    + "import java.util.List;\n"
    + "\n"
    + "/**This file is generated by genJavaOut.jzTc script. \n"
    + " * It is the derived class to write Zbnf result. */\n"
    + "public class <&javaclass>_Zbnf extends <&javaclass>{\n"
    + "\n");
  
  /**Text for class header for syntax component data storing. */
  private final OutTextPreparer sJavaCmpnClass = new OutTextPreparer( "sJavaCmpnClass", null, "cmpnclass, dataclass, semantic",
      "\n"
    + "\n"
    + "\n"
    + "  /**Class for Component <&semantic>. */\n"
    + "  public static class <&cmpnclass> {\n"
    + "  \n");
  
  /**Text for class header for syntax component to write from zbnf. */
  private final OutTextPreparer sJavaCmpnClassZbnf = new OutTextPreparer( "sJavaCmpnClassZbnf", null, "cmpnclass, dataclass, semantic",
      "\n"
    + "\n"
    + "\n"
    + "  /**Class for Component <&semantic>.*/\n"
    + "  public static class <&cmpnclass>_Zbnf extends <&dataclass>.<&cmpnclass> {\n"
    + "  \n");
  
  private static final String sJavaCmpnEnd = 
      "  \n"
    + "  }\n"
    + "\n";
  
  private static final  String sJavaEnd = 
      "\n"
    + "}\n"
    + "\n";
  
  private final OutTextPreparer sJavaSimpleVar = new OutTextPreparer(  "sJavaSimpleVar", null, "typeGeneric, varName, name, type, typeZbnf, args",
      "    \n"
    + "    protected <&type> <&varName>;\n"
    + "    \n"
    + "    \n");
  
  private final OutTextPreparer sJavaListVar = new OutTextPreparer(  "sJavaListVar", null, "typeGeneric, varName, name, type, typeZbnf, args",
      "    \n"
    + "    protected List<<&typeGeneric>> <&varName>;\n"
    + "    \n"
    + "    \n");
  
  private final OutTextPreparer sJavaSimpleVarOper = new OutTextPreparer( "sJavaSimpleVarOper", null, "typeGeneric, varName, name, type, typeZbnf, args",
      "    \n    \n"
    + "    /**Access to parse result.*/\n"
    + "    public <&type> get_<&name>() { return <&varName>; }\n"
    + "    \n"
    + "    \n");
  
  private final OutTextPreparer sJavaListVarOper = new OutTextPreparer( "sJavaListVarOper", null, "typeGeneric, varName, name, type, typeZbnf, args",
      "    \n    \n"
    + "    /**Access to parse result, get the elements of the container <&name>*/\n"
    + "    public Iterable<<&typeGeneric>> get_<&name>() { return <&varName>; }\n"
    + "    \n"
    + "    /**Access to parse result, get the size of the container <&name>.*/\n"
    + "    public int getSize_<&name>() { return <&varName> ==null ? 0 : <&varName>.size(); }\n"
    + "    \n"
    + "    \n");
  
  
  private final OutTextPreparer sJavaSimpleVarZbnf = new OutTextPreparer( "sJavaSimpleVarZbnf", null, "typeGeneric, varName, name, type, typeZbnf, args",
      "    /**Set routine for the singular component &lt;<&type>?<&name>>. */\n"
    + "    public void set_<&name>(<&type> val) { super.<&varName> = val; }\n"
    + "    \n"
    + "    \n");
  
  private final OutTextPreparer sJavaListVarZbnf = new OutTextPreparer( "sJavaListVarZbnf", null, "typeGeneric, varName, name, type, typeZbnf, args",
      "    /**Set routine for the singular component &lt;<&type>?<&name>>. */\n"
    + "    public void set_<&name>(<&type> val) { \n"
    + "      if(super.<&varName>==null) { super.<&varName> = new LinkedList<<&typeGeneric>>(); }\n"
    + "      super.<&varName>.add(val); \n"
    + "    }\n"
    + "    \n"
    + "    \n");
  
  
  private final OutTextPreparer sJavaCmpnZbnf = new OutTextPreparer( "sJavaCmpnZbnf", null, "typeGeneric, varName, name, type, typeZbnf, args",
      "    /**Creates an instance for the result Zbnf <:if:args> (not Xml) <.if>. &lt;<&typeZbnf>?<&name>&gt; for ZBNF data store*/\n"
    + "    public <&typeZbnf>_Zbnf new_<&name>() { \n"
    + "      <&typeZbnf>_Zbnf val = new <&typeZbnf>_Zbnf();\n"
    + "      super.<&varName> = val;\n"
    + "      return val; //Note: needs the derived Zbnf-Type.\n"
    + "    }\n"
    + "    \n"  //<:debug:name:FBType>"
    + "<:if:args>"
    + "    /**Creates an instance for the Xml data storage with default attibutes. &lt;<&typeZbnf>?<&name>&gt;  */\n"
    + "    public <&typeZbnf>_Zbnf new_<&name>(<:for:arg:args>String <&arg><:if:arg_next>,<.if> <.for>) { \n"
    + "      <&typeZbnf>_Zbnf val = new <&typeZbnf>_Zbnf();\n"
    + "      <:for:arg:args>val.<&arg> = <&arg>;\n"
    + "      <.for>//\n"
    + "      super.<&varName> = val;\n"
    + "      return val; //Note: needs the derived Zbnf-Type.\n"
    + "    }\n"
    + "    \n"
    + "<.if>"
    + "    /**Set the result. &lt;<&typeZbnf>?<&name>&gt;*/\n"
    + "    public void set_<&name>(<&typeZbnf>_Zbnf val) { /*already done: super.<&varName> = val; */ }\n"
    + "    \n"
    + "    \n");
  
  private final OutTextPreparer sJavaListCmpnZbnf = new OutTextPreparer( "sJavaListCmpnZbnf", null, "typeGeneric, varName, name, type, typeZbnf, args",
      "    /**create and add routine for the list component <<&typeZbnf>?<&name>>. */\n"
    + "    public <&typeZbnf>_Zbnf new_<&name>() { \n"
    + "      <&typeZbnf>_Zbnf val = new <&typeZbnf>_Zbnf(); \n"
    + "      if(super.<&varName>==null) { super.<&varName> = new LinkedList<<&typeZbnf>>(); }\n"
    + "      super.<&varName>.add(val); \n"
    + "      return val; \n"
    + "    }\n"
    + "    \n"
    + "<:if:args>"
    + "    /**Creates an instance for the Xml data storage with default attibutes. &lt;<&typeZbnf>?<&name>&gt;  */\n"
    + "    public <&typeZbnf>_Zbnf new_<&name>(<:for:arg:args>String <&arg><:if:arg_next>,<.if> <.for>) { \n"
    + "      <&typeZbnf>_Zbnf val = new <&typeZbnf>_Zbnf();\n"
    + "      <:for:arg:args>val.<&arg> = <&arg>;\n"
    + "      <.for>//\n"
    + "      if(super.<&varName>==null) { super.<&varName> = new LinkedList<<&typeZbnf>>(); }\n"
    + "      super.<&varName>.add(val);\n"
    + "      return val; //Note: needs the derived Zbnf-Type.\n"
    + "    }\n"
    + "    \n"
    + "<.if>"
    + "    /**Add the result to the list. &lt;<&typeZbnf>?<&name>&gt;*/\n"
    + "    public void add_<&name>(<&typeZbnf>_Zbnf val) {\n"
    + "      //already done: \n"
    + "      //if(super.<&varName>==null) { super.<&varName> = new LinkedList<<&typeZbnf>>(); }\n"
    + "      //super.<&varName>.add(val); \n"
    + "    }\n"
    + "    \n"
    + "    \n");
  
  private final OutTextPreparer sJavaMetaClass = new OutTextPreparer( "sJavaMetaClass", null, "attrfield",
      "    <&attrfield.type> <&attrfield.varName>;  \n"
    + "  \n");
  

  private final OutTextPreparer sJavaMetaClassOper = new OutTextPreparer( "sJavaMetaClass", null, "attrfield",
      "    <&attrfield.type> get_<&attrfield.semantic>() { return <&attrfield.varName>; }  \n"
    + "  \n");

  
  
  private final OutTextPreparer sJavaMetaClassZbnf = new OutTextPreparer( "sJavaMetaClassZbnf", null, "attrfield",
      "    public void set_<&attrfield.semantic>(<&attrfield.type> <&attrfield.varName>) { super.<&attrfield.varName> = <&attrfield.varName>; }  \n\n");
  
  
  

  
  
  
  
  
  public GenJavaOutClass(CmdArgs args, MainCmdLogging_ifc log)
  { this.cmdArgs = args;
    this.log = log;
    idxStdTypes.put("boolean","Boolean");
    idxStdTypes.put("float","Float");
    idxStdTypes.put("int","Integer");
    idxStdTypes.put("String","String");
    idxStdTypes.put("double","Double");
    idxStdTypes.put("long","Long");
    idxStdTypes.put("Boolean","Boolean");
    idxStdTypes.put("Float","Float");
    idxStdTypes.put("Integer","Integer");
    idxStdTypes.put("Double","Double");
    idxStdTypes.put("Long","Long");
    
    reservedNames.put("const", "const___");
    reservedNames.put("final", "final___");
    reservedNames.put("class", "class___");
    reservedNames.put("interface", "interface___");
  }
  
  public void setupWriter() {
    String sJavaOutputDir = cmdArgs.dirJava.getAbsolutePath() + "/" + cmdArgs.sJavaPkg.replace(".","/") + "/";
    File sJavaOutputFile = new File(sJavaOutputDir + cmdArgs.sJavaClass + ".java");
    File sJavaOutputFileZbnf = new File(sJavaOutputDir + cmdArgs.sJavaClass + "_Zbnf.java");
    try {
      FileSystem.mkDirPath(sJavaOutputDir);
      wr = new FileWriter(sJavaOutputFile);
      wrz = new FileWriter(sJavaOutputFileZbnf);
    } catch (IOException e) {
      System.err.println("cannot create: " + sJavaOutputFile.getAbsolutePath());
    }
    try {
      //Map<String, Object> argstxt = new TreeMap<String, Object>();
      OutTextPreparer.DataTextPreparer argsJavaHead = sJavaHead.createArgumentDataObj();
      OutTextPreparer.DataTextPreparer argsJavaHeadZbnf = sJavaHeadZbnf.createArgumentDataObj();
      argsJavaHead.setArgument("pkgpath", cmdArgs.sJavaPkg);
      argsJavaHeadZbnf.setArgument("pkgpath", cmdArgs.sJavaPkg);
      argsJavaHead.setArgument("javaclass", cmdArgs.sJavaClass);
      argsJavaHeadZbnf.setArgument("javaclass", cmdArgs.sJavaClass);
      sJavaHead.exec(wr, argsJavaHead);
      sJavaHeadZbnf.exec(wrz, argsJavaHeadZbnf);
      //
      //
      //
    } catch (Exception e1) {
      // TODO Auto-generated catch block
      System.err.println(e1.getMessage());
    }
  }
  
  
  
  public void closeWrite() {
    try {
      if(wr!=null) { wr.close(); }
      if(wrz!=null) { wrz.close(); }
    } catch (IOException e) {
      System.err.println("internal error cannot close Files: " + cmdArgs.dirJava);
    }

  }
  
  public static String firstUppercase(String src) {
    char cc = src.charAt(0);
    if(Character.isUpperCase(cc)) return src;
    else return Character.toUpperCase(cc) + src.substring(1);
  }

  public static String firstLowercase(String src) {
    char cc = src.charAt(0);
    if(Character.isLowerCase(cc)) return src;
    else return Character.toLowerCase(cc) + src.substring(1);
  }

  public void finishCmpnWrite() throws IOException {
    wr.append(sJavaCmpnEnd);
    wrz.append(sJavaCmpnEnd);
  }
  
  
  public void finishClassWrite() throws IOException {
    wr.append(sJavaEnd);
    wrz.append(sJavaEnd);
  }
  
  
  
  public class WrClassJava {
    public Map<String, String> variables = new TreeMap<String, String>();

    StringBuilder wrOp = new StringBuilder(1000);
    
    public WrClassJava() {}
    
    /**Writes a Class for a syntax Component.
     * @param cmpn
     * @throws IOException
     */
    public void wrClassCmpn(SubClassJava classData) throws Exception {
      if(classData.sDbgIdent.equals("add_expression"))
        Debugutil.stop();
      //Map<String, Object> argstxt = new TreeMap<String, Object>();
      OutTextPreparer.DataTextPreparer argsJavaCmpnClass = sJavaCmpnClass.createArgumentDataObj();
      OutTextPreparer.DataTextPreparer argsJavaCmpnClassZbnf = sJavaCmpnClassZbnf.createArgumentDataObj();
      argsJavaCmpnClass.setArgument("cmpnclass", classData.className); //firstUppercase(cmpn.sDefinitionIdent));
      argsJavaCmpnClass.setArgument("dataclass", cmdArgs.sJavaClass);
      argsJavaCmpnClass.setArgument("semantic", classData.semantic);
      argsJavaCmpnClassZbnf.setArgument("cmpnclass", classData.className); //firstUppercase(cmpn.sDefinitionIdent));
      argsJavaCmpnClassZbnf.setArgument("dataclass", cmdArgs.sJavaClass);
      argsJavaCmpnClassZbnf.setArgument("semantic", classData.semantic);
      sJavaCmpnClass.exec(wr, argsJavaCmpnClass);
      sJavaCmpnClassZbnf.exec(wrz, argsJavaCmpnClassZbnf);
      //
      TreeMap<String, SubClassField> elems = classData.fieldsFromSemanticAttr;
      if(elems !=null) {
        OutTextPreparer.DataTextPreparer argsJavaMetaClass = sJavaMetaClass.createArgumentDataObj();
        OutTextPreparer.DataTextPreparer argsJavaMetaClassOper = sJavaMetaClassOper.createArgumentDataObj();
        OutTextPreparer.DataTextPreparer argsJavaMetaClassZbnf = sJavaMetaClassZbnf.createArgumentDataObj();
        
        for(Map.Entry<String, SubClassField> e : elems.entrySet()) {
          SubClassField attrfield = e.getValue();
          //TreeMap<String, Object> argstxt2 = new TreeMap<String, Object>();
          argsJavaMetaClass.setArgument("attrfield", attrfield);
          argsJavaMetaClassOper.setArgument("attrfield", attrfield);
          argsJavaMetaClassZbnf.setArgument("attrfield", attrfield);
          sJavaMetaClass.exec(wr, argsJavaMetaClass);
          sJavaMetaClassOper.exec(wrOp, argsJavaMetaClassOper);
          sJavaMetaClassZbnf.exec(wrz, argsJavaMetaClassZbnf);
          
        }
      }
    }

    
    
    public void wrVariable(String varName, String varType, boolean bStdType, boolean bList, boolean bCmpn, List<String> args) throws IOException {
      if(varName.equals("operator"))
        Debugutil.stop();
      String varTypeStored = this.variables.get(varName);
      if(varTypeStored !=null) {
        if(!varTypeStored.equals(varType)){
          throw new IllegalArgumentException("same variable twice with different types");
        }
      } 
      else { //varName not found
        this.variables.put(varName, varType);
        String varNameJava = firstLowercase(varName);
        String varNameReplReserved = reservedNames.get(varNameJava);
        if(varNameReplReserved !=null) { varNameJava = varNameReplReserved; }
        String sTypeGeneric = idxStdTypes.get(varType);
        if(sTypeGeneric == null) { 
          sTypeGeneric = varType;
          assert(bStdType == false);
        } else {
          if(!bStdType) { //stdType found, 
            varType += "__";  //it should not be a standard Java type.
          }
        }
        Map<String, Object> argstxt = new TreeMap<String, Object>();
        OutTextPreparer.DataTextPreparer argsJavaListVarOper = sJavaListVarOper.createArgumentDataObj();
        argsJavaListVarOper.setArgument("typeGeneric", sTypeGeneric);
        argsJavaListVarOper.setArgument("varName", varNameJava);
        argsJavaListVarOper.setArgument("name", varName);
        argsJavaListVarOper.setArgument("type", varType);
        argsJavaListVarOper.setArgument("typeZbnf", varType);
        argsJavaListVarOper.setArgument("args", args);
        
        OutTextPreparer.DataTextPreparer argsJavaListVar = sJavaListVar.createArgumentDataObj();
        argsJavaListVar.setArgument("typeGeneric", sTypeGeneric);
        argsJavaListVar.setArgument("varName", varNameJava);
        argsJavaListVar.setArgument("name", varName);
        argsJavaListVar.setArgument("type", varType);
        argsJavaListVar.setArgument("typeZbnf", varType);
        argsJavaListVar.setArgument("args", args);
        
        OutTextPreparer.DataTextPreparer argsJavaListVarZbnf = sJavaListVarZbnf.createArgumentDataObj();
        argsJavaListVarZbnf.setArgument("typeGeneric", sTypeGeneric);
        argsJavaListVarZbnf.setArgument("varName", varNameJava);
        argsJavaListVarZbnf.setArgument("name", varName);
        argsJavaListVarZbnf.setArgument("type", varType);
        argsJavaListVarZbnf.setArgument("typeZbnf", varType);
        argsJavaListVarZbnf.setArgument("args", args);
        
        OutTextPreparer.DataTextPreparer argsJavaListCmpnZbnf = sJavaListCmpnZbnf.createArgumentDataObj();
        argsJavaListCmpnZbnf.setArgument("typeGeneric", sTypeGeneric);
        argsJavaListCmpnZbnf.setArgument("varName", varNameJava);
        argsJavaListCmpnZbnf.setArgument("name", varName);
        argsJavaListCmpnZbnf.setArgument("type", varType);
        argsJavaListCmpnZbnf.setArgument("typeZbnf", varType);
        argsJavaListCmpnZbnf.setArgument("args", args);
        
        OutTextPreparer.DataTextPreparer argsJavaSimpleVarOper = sJavaSimpleVarOper.createArgumentDataObj();
        argsJavaSimpleVarOper.setArgument("typeGeneric", sTypeGeneric);
        argsJavaSimpleVarOper.setArgument("varName", varNameJava);
        argsJavaSimpleVarOper.setArgument("name", varName);
        argsJavaSimpleVarOper.setArgument("type", varType);
        argsJavaSimpleVarOper.setArgument("typeZbnf", varType);
        argsJavaSimpleVarOper.setArgument("args", args);
        
        OutTextPreparer.DataTextPreparer argsJavaSimpleVarZbnf = sJavaSimpleVarZbnf.createArgumentDataObj();
        argsJavaSimpleVarZbnf.setArgument("typeGeneric", sTypeGeneric);
        argsJavaSimpleVarZbnf.setArgument("varName", varNameJava);
        argsJavaSimpleVarZbnf.setArgument("name", varName);
        argsJavaSimpleVarZbnf.setArgument("type", varType);
        argsJavaSimpleVarZbnf.setArgument("typeZbnf", varType);
        argsJavaSimpleVarZbnf.setArgument("args", args);
        
        OutTextPreparer.DataTextPreparer argsJavaCmpnZbnf = sJavaCmpnZbnf.createArgumentDataObj();
        argsJavaCmpnZbnf.setArgument("typeGeneric", sTypeGeneric);
        argsJavaCmpnZbnf.setArgument("varName", varNameJava);
        argsJavaCmpnZbnf.setArgument("name", varName);
        argsJavaCmpnZbnf.setArgument("type", varType);
        argsJavaCmpnZbnf.setArgument("typeZbnf", varType);
        argsJavaCmpnZbnf.setArgument("args", args);
        
        OutTextPreparer.DataTextPreparer argsJavaSimpleVar = sJavaSimpleVar.createArgumentDataObj();
        argsJavaSimpleVar.setArgument("typeGeneric", sTypeGeneric);
        argsJavaSimpleVar.setArgument("varName", varNameJava);
        argsJavaSimpleVar.setArgument("name", varName);
        argsJavaSimpleVar.setArgument("type", varType);
        argsJavaSimpleVar.setArgument("typeZbnf", varType);
        argsJavaSimpleVar.setArgument("args", args);
        
        //because of debugging write firstly to a StringBuilder:
        StringBuilder wrb = new StringBuilder();
        StringBuilder wrzb = new StringBuilder();
        
        if(bList) {
          sJavaListVar.exec(wrb, argsJavaListVar);
          sJavaListVarOper.exec(wrOp, argsJavaListVarOper);
          if(bStdType) {
            sJavaListVarZbnf.exec(wrzb, argsJavaListVarZbnf);
          }
          else if(bCmpn) {
            sJavaListCmpnZbnf.exec(wrzb, argsJavaListCmpnZbnf);
          } 
          else {
            sJavaListVarZbnf.exec(wrzb, argsJavaListVarZbnf);
          }
        } else {
          sJavaSimpleVar.exec(wrb, argsJavaSimpleVar);
          sJavaSimpleVarOper.exec(wrOp, argsJavaSimpleVarOper);
          if(bStdType) {
            sJavaSimpleVarZbnf.exec(wrzb, argsJavaSimpleVarZbnf);
          }
          else if(bCmpn) {
            sJavaCmpnZbnf.exec(wrzb, argsJavaCmpnZbnf);
          } 
          else {
            sJavaSimpleVarZbnf.exec(wrzb, argsJavaSimpleVarZbnf);
          }
        }
        wr.append(wrb); //now append to output, remove wrb as stack local ref 
        wrz.append(wrzb);
      }
    }
    
    

    public void writeOperations() throws IOException {
      wr.append(wrOp);
      wrOp.setLength(0);
    }
    
  }
 

  
  
}
