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
import org.vishia.util.Writer_Appendable;




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
   * <li>2022-03-28 Hartmut: Now line and column is written in own super class SrcInfo. This can be used by applications
   *   to get line and column info independent of the parse result. Very important. 
   *   It is changed in this generator. The class SrcInfo is new.  
   * <li>2022-02-28 Hartmut: Now creates three files. Docu is todo. 
   *   The third file is especially user-changable, it is only a template. 
   * <li>2022-02-20 Hartmut: in {@link CmdArgs#fileOutStruct}: for argument output the struct.
   *   See {@link org.vishia.parseJava.JavaParser} and {@link org.vishia.zbnf.ZbnfParser#writeSyntaxStruct(Appendable)}.
   * <li>2022-02-18 Hartmut: adding the SetLineColumn_ifc to the output classes. 
   *   This is important to get the source information while using the parse result.
   *   It is a general property from now. The basics to write the information 
   *   are contained in {@link org.vishia.zbnf.ZbnfJavaOutput} since 2014-05, used for manual written files.
   *   Now also for the generated ones. For other outputs (XML) it should be supplemented.
   * <li>2022-02-18 Hartmut: Improvements for basic types, now proper without manual corrections:
   *   The typeNs argument should be null for basic types. It is tested. The value of typeNs is yet not used,
   *   here some improvement opportunities TODO.  
   * <li>2022-02-11 Hartmut Generally change: Now no inheritance between writing class (..._Zbnf)
   *   instead referencing of the data class. In this kind a writing class can be also derived
   *   from another writing class as well as the data class can have its own inheritance. 
   *   The inheritance of data classes is a new feature also introduced in the last version, 
   *   but it was not sufficient without this change. Some else experience in this code.
   * <li>2022-02-10 Hartmut 
   *   <ul><li>option -all, then writes classes from all syntax components. (Else, compatible version, writes only from used ones). 
   *     This is necessary if super components are used, which are not written elsewhere. Usually there should not exist unnecessary non used components
   *  <li>Using flush, hence output can be checked. It is very helpfully.
   *  <li>Some fine tuning for super components, should be compatible with older usages. (compare it before use) 
   * </ul>    
   * <li>2022-02-07 Hartmut identifier-name replaced by identifier_name. Used for {@link org.vishia.parseJava.JavaContent}
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
  public static final String sVersion = "2022-03-28";

  
  
  
  
  /**Command line args */
  public static class CmdArgs{
    
    /**The input file which determines the structure. */
    public File fileInput = null;
    
    /**A file which should show the inner syntax structure from fileInput given. */
    public File fileOutStruct = null;
    
    public File dirJava;
    
    public String sJavaPkg;
    
    public String sJavaClass;
  
    public boolean bAll;
    
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
    
    public final String sSuperItemType;
    
    public String sDbgIdent;
    
    public SubClassJava(String semantic, String className, String sSuperItemType)
    { this.semantic = semantic;
      this.className = className;
      this.sSuperItemType = sSuperItemType;
    }
    
    
  }
  
  

  
  
  /**Command line args */
  private final CmdArgs cmdArgs;

  private final MainCmdLogging_ifc log;
  
  
  /**Writer for the base data class and the Zbnf JavaOut class.
   * 
   */
  private Writer wrz, wru;

  private Writer wr;
  
  private String sJavaOutputDir;
  
  Map<String, String> superTypes = new TreeMap<String, String>();
  
  /**StandardTypes. */
  public final TreeMap<String, String> idxStdTypes = new TreeMap<String, String>();

  
  
  
  
  /**The syntax components which are to process yet (are used for parse result storing).  */
  public List<SubClassJava> listCmpn = new LinkedList<SubClassJava>();
  
  /**Index of already registered components to add in {@link #listCmpn} only one time. */
  public Map<String, SubClassJava> idxRegisteredCmpn = new TreeMap<String, SubClassJava>();
  
  private final static Map<String, String> reservedNames = new TreeMap<String, String>();
  
  /**Text for Java header. */
  private final OutTextPreparer tJavaHeadBase = new OutTextPreparer("tJavaHead", null, "pkgpath, dataClass", 
      "package <&pkgpath>;\n"
    + "\n"
    + "import java.util.LinkedList;\n"
    + "import java.util.List;\n"
    + "import org.vishia.genJavaOutClass.SrcInfo;\n"
    + "\n"
    + "/**This file is generated by genJavaOut.jzTc script. */\n"
    + "public class <&dataClass>_Base extends SrcInfo {\n"
    + "\n");
  
  /**Text for Java usage header. */
  private final OutTextPreparer tJavaHeadUsg = new OutTextPreparer("tJavaHeadUsg", null, "pkgpath, dataClass", 
      "package <&pkgpath>;\n"
    + "\n"
    + "//import java.util.LinkedList;\n"
    + "//import java.util.List;\n"
    + "\n"
    + "/**This file is generated by {@link org.vishia.genJavaOutClass.GenJavaOutClass}.\n"
    + " * It defines the usage level for the data class for the parse results. \n"
    + " * This usage classes can be adapted. */\n"
    + "public class <&dataClass> extends <&dataClass>_Base {\n"
    + "\n");
  
  /**Text for Java header for Zbnf writer class. */
  private final OutTextPreparer sJavaHeadZbnf = new OutTextPreparer("sJavaHeadZbnf", null, "pkgpath, dataClass", 
      "package <&pkgpath>;\n"
    + "\n"
    + "import java.util.LinkedList;\n"
    + "import java.util.List;\n"
    + "import org.vishia.util.SetLineColumn_ifc;\n"
    + "\n"
    + "/**This file is generated by genJavaOut.jzTc script. \n"
    + " * It is the derived class to write Zbnf result. */\n"
    + "public class <&dataClass>_Zbnf {\n"
    + "  \n"
    + "  /**Referenced working instance will be filled.*/\n"
    + "  public final <&dataClass> data<&dataClass>;\n"
    + "  \n"
    + "  /**Default ctor for non-inherit instance. */\n"
    + "  public <&dataClass>_Zbnf ( ) {\n"
    + "    this.data<&dataClass> = new <&dataClass>();\n"
    + "  }\n"
    + "  \n"
    + "  \n"
    + "  \n");
  
  /**Text for class header for syntax component data storing. */
  private final OutTextPreparer tJavaSuperTypeClass = new OutTextPreparer( "tJavaSuperTypeClass", null, "className, dataClass",
      "\n"
    + "\n"
    + "\n"
    + "  /**Class for Super Types. */\n"
    + "  public abstract static class <&className> {\n"
    + "    //left empty, implementors contains all ...\n"
    + "  }\n"
    + "  \n");
  
  /**Text for class header for syntax component data storing. */
  private final OutTextPreparer tJavaCmpnClass_Base = new OutTextPreparer( "tJavaCmpnClass_Base", null, "cmpnClass, superClass, dataClass, semantic",
      "\n"
    + "\n"
    + "\n"
    + "  /**Class for Component <&semantic>. */\n"
    + "  public static class <&cmpnClass>_Base extends <:if:superClass>JavaSrc.<&superClass><:else>SrcInfo<.if> {\n"
//    + "    <:if:superClass==null>\n"
//    + "    protected int _srcColumn_, _srcLine_; protected String _srcFile_;\n"
//    + "    \n"
//    + "    public String getSrcInfo ( int[] lineColumn) {\n"
//    + "      if(lineColumn !=null) {\n"
//    + "        if(lineColumn.length >=1) { lineColumn[0] = this._srcLine_; }\n"
//    + "        if(lineColumn.length >=2) { lineColumn[1] = this._srcColumn_; }\n"
//    + "      }\n"
//    + "      return this._srcFile_;\n"
//    + "    }\n<.if>"
    + "  \n"
    + "  \n");
  
  /**Text for class header for syntax component to write from zbnf. */
  private final OutTextPreparer tJavaCmpnClassZbnf = new OutTextPreparer( "tJavaCmpnClassZbnf", null, "cmpnClass, superClass, dataClass, semantic",
      "\n"
    + "\n"
    + "\n"
    + "  /**Class for Writing the Component <&semantic>.*/\n"
    + "  public static class <&cmpnClass>_Zbnf<:if:superClass> extends <&superClass>_Zbnf<:else> implements SetLineColumn_ifc<.if> {\n"
    + "    /**Referenced working instance will be filled.*/\n"
    + "    final <&dataClass>.<&cmpnClass> data<&cmpnClass>;\n"
    + "    \n"
    + "    /**Default ctor for non-inherit instance. */\n"
    + "    public <&cmpnClass>_Zbnf ( ) {\n"
    + "      <:if:superClass>super(new <&dataClass>.<&cmpnClass>());\n"
    + "      this.data<&cmpnClass> = (<&dataClass>.<&cmpnClass>) super.data<&superClass>;\n"
    + "      <:else>this.data<&cmpnClass> = new <&dataClass>.<&cmpnClass>();\n<.if>"
    + "    }\n"
    + "    \n"
    + "    /**ctor called as super ctor possible, not in any case for a inherited instance. */\n"
    + "    public <&cmpnClass>_Zbnf ( <&dataClass>.<&cmpnClass> data) {\n"
    + "      <:if:superClass>super(data);\n<.if>"
    + "      this.data<&cmpnClass> = data;\n"
    + "    }\n"
    + "    \n"
    + "    <:if:superClass==null>@Override public int setLineColumnFileMode ( ) {\n"
    + "      return SetLineColumn_ifc.mLine + SetLineColumn_ifc.mColumn + SetLineColumn_ifc.mFile; }\n"
    + "    \n"
    + "    @Override public void setLineColumnFile ( int line, int column, String sFile) { \n"
    + "      this.data<&cmpnClass>._srcLine_ = line; \n"
    + "      this.data<&cmpnClass>._srcColumn_ = column; \n"
    + "      this.data<&cmpnClass>._srcFile_ = sFile; \n"
    + "    }\n<.if>"
    + "  \n"
    + "  \n"
    + "  \n"
    + "  \n");
  
  /**Text for class header for syntax component to write from zbnf. */
  private final OutTextPreparer tJavaCmpnClassUsg = new OutTextPreparer( "tJavaCmpnClassUsg", null, "cmpnClass, superClass, dataClass, semantic",
      "\n"
    + "\n"
    + "\n"
    + "  /**Class for Writing the Component <&semantic>.*/\n"
    + "  public static class <&cmpnClass> extends JavaSrc_Base.<&cmpnClass>_Base {\n"
    + "  \n"
    + "  \n"
    + "  \n"
    + "  \n"
    + "  \n"
    + "    @Override public String toString ( ) { \n"
    + "      return \"TODO toString\";\n"
    + "    }\n"
    );
  
  private static final String tJavaCmpnEnd = 
      "  \n"
    + "  }\n"
    + "\n";
  
  private static final  String tJavaEnd = 
      "\n"
    + "}\n"
    + "\n";
  
  private final OutTextPreparer tJavaSimpleVar = new OutTextPreparer(  "sJavaSimpleVar", null, "typeNs, cmpnClass, typeGeneric, dataClass, varName, name, type, typeZbnf, args",
      "    \n"
    + "    protected <:if:typeNs>JavaSrc.<.if><&type> <&varName>;\n"
    + "    \n"
    + "    \n");
  
  private final OutTextPreparer tJavaListVar = new OutTextPreparer(  "sJavaListVar", null, "typeNs, cmpnClass, typeGeneric, dataClass, varName, name, type, typeZbnf, args",
      "    \n"
    + "    protected List<<:if:typeNs>JavaSrc.<.if><&typeGeneric>> <&varName>;\n"
    + "    \n"
    + "    \n");
  
  private final OutTextPreparer tJavaSimpleVarOper = new OutTextPreparer( "sJavaSimpleVarOper", null, "typeNs, cmpnClass, typeGeneric, dataClass, varName, name, type, typeZbnf, args",
      "    \n    \n"
    + "    /**Access to parse result.*/\n"
    + "    public <:if:typeNs>JavaSrc.<.if><&type> get_<&name>() { return <&varName>; }\n"
    + "    \n"
    + "    \n");
  
  private final OutTextPreparer tJavaListVarOper = new OutTextPreparer( "sJavaListVarOper", null, "typeNs, cmpnClass, typeGeneric, dataClass, varName, name, type, typeZbnf, args",
      "    \n    \n"
    + "    /**Access to parse result, get the elements of the container <&name>*/\n"
    + "    public Iterable<<:if:typeNs>JavaSrc.<.if><&typeGeneric>> get_<&name>() { return <&varName>; }\n"
    + "    \n"
    + "    /**Access to parse result, get the size of the container <&name>.*/\n"
    + "    public int getSize_<&name>() { return <&varName> ==null ? 0 : <&varName>.size(); }\n"
    + "    \n"
    + "    \n");
  
  
  /**
   * <ul>
   * <li>typeNs: null on simple types as String, int.   
   * <li>cmpnClass: The name of the syntax component. It builts the name of the data... variable too. 
   * </ul>
   */
  private final OutTextPreparer tJavaSimpleVarZbnf = new OutTextPreparer( "tJavaSimpleVarZbnf", null, "typeGeneric, dataClass, cmpnClass, varName, name, typeNs, type, typeZbnf, args",
      "    /**Set routine for the singular component &lt;<&type>?<&name>>. */\n"
//    + "     * <:if:typeNs>Component for argument: <&typeNs><.if>*/\n"
    + "    public void set_<&name>(<&type><:if:typeNs>_Zbnf<.if> val) { this.data<&cmpnClass>.<&varName> = val<:if:typeNs>.data<&type><.if>; }\n"
    + "    \n"
    + "    \n");
  
  private final OutTextPreparer tJavaListVarZbnf = new OutTextPreparer( "tJavaListVarZbnf", null, "cmpnClass, typeGeneric, dataClass, varName, name, typeNs, type, typeZbnf, args",
      "    /**Set routine for the singular component &lt;<&type>?<&name>>. */\n"
    + "    public void set_<&name>(<&type><:if:typeNs>_Zbnf<.if> val) { \n"
    + "      if(data<&cmpnClass>.<&varName>==null) { data<&cmpnClass>.<&varName> = new LinkedList<<&typeGeneric>>(); }\n"
    + "      data<&cmpnClass>.<&varName>.add(val<:if:typeNs>.data<&type><.if>); \n"
    + "    }\n"
    + "    \n"
    + "    \n");
  
  
  private final OutTextPreparer tJavaCmpnZbnf = new OutTextPreparer( "tJavaCmpnZbnf", null, "cmpnClass, typeGeneric, dataClass, superType, varName, name, typeNs, type, typeZbnf, args",
      "    /**Creates an instance for the result Zbnf <:if:args> (not Xml) <.if>. &lt;<&typeZbnf>?<&name>&gt; for ZBNF data store*/\n"
    + "    <:if:xxxsuperType>@Override <.if>public <&typeZbnf>_Zbnf new_<&name>() { \n"
    + "      <&typeZbnf>_Zbnf val = new <&typeZbnf>_Zbnf();\n"
    + "      return val; //Note: needs the derived Zbnf-Type.\n"
    + "    }\n"
    + "    \n"  //<:debug:name:FBType>"
    + "<:if:args>"
    + "    /**Creates an instance for the Xml data storage with default attibutes. &lt;<&typeZbnf>?<&name>&gt;  */\n"
    + "    public <&typeZbnf>_Zbnf new_<&name>(<:for:arg:args>String <&arg><:if:arg_next>,<.if> <.for>) { \n"  //< :if:superType>@Override < .if>
    + "      <&typeZbnf>_Zbnf val = new <&typeZbnf>_Zbnf();\n"
    + "      <:for:arg:args>val.data<&type>.<&arg> = <&arg>;\n"
    + "      <.for>//\n"
    + "      return val; //Note: needs the derived Zbnf-Type.\n"
    + "    }\n"
    + "    \n"
    + "<.if>"
    + "    /**Set the result. &lt;<&typeZbnf>?<&name>&gt;*/\n"
    + "    public void set_<&name>(<&type><:if:typeNs>_Zbnf<.if> val) { \n"
    + "      data<&cmpnClass>.<&varName> = val<:if:typeNs>.data<&type><.if>;\n"
    + "    }\n"
    + "    \n"
    + "    \n");
  
  private final OutTextPreparer tJavaListCmpnZbnf = new OutTextPreparer( "tJavaListCmpnZbnf", null, "typeGeneric, dataClass, cmpnClass, superType, varName, name, typeNs, type, typeZbnf, args",
      "    /**create and add routine for the list component <<&typeZbnf>?<&name>>. */\n"
    + "    <:if:xxxsuperType>@Override <.if>public <&typeZbnf>_Zbnf new_<&name>() { \n"
    + "      <&typeZbnf>_Zbnf val = new <&typeZbnf>_Zbnf(); \n"
    + "      return val; \n"
    + "    }\n"
    + "    \n"
    + "<:if:args>"
    + "    /**Creates an instance for the Xml data storage with default attibutes. &lt;<&typeZbnf>?<&name>&gt;  */\n"
    + "    public <&typeZbnf>_Zbnf new_<&name>(<:for:arg:args>String <&arg><:if:arg_next>,<.if> <.for>) { \n"  //<:if:superType>@Override <.if>
    + "      <&typeZbnf>_Zbnf val = new <&typeZbnf>_Zbnf();\n"
    + "      <:for:arg:args>val.data<&type>.<&arg> = <&arg>;\n"
    + "      <.for>//\n"
    + "      return val; //Note: needs the derived Zbnf-Type.\n"
    + "    }\n"
    + "    \n"
    + "<.if>"
    + "    /**Add the result to the list. &lt;<&typeZbnf>?<&name>&gt;*/\n"
    + "    public void add_<&name>(<&type><:if:typeNs>_Zbnf<.if> val) {\n"
    + "      if(data<&cmpnClass>.<&varName>==null) { data<&cmpnClass>.<&varName> = new LinkedList<<&typeGeneric>>(); }\n"
    + "      data<&cmpnClass>.<&varName>.add(val<:if:typeNs>.data<&type><.if>); \n"
    + "    }\n"
    + "    \n"
    + "    \n");
  
  private final OutTextPreparer sJavaMetaClass = new OutTextPreparer( "sJavaMetaClass", null, "cmpnClass, attrfield, dataClass",
      "    JavaSrc.<&attrfield.type> <&attrfield.varName>;  \n"
    + "  \n");
  

  private final OutTextPreparer sJavaMetaClassOper = new OutTextPreparer( "sJavaMetaClass", null, "cmpnClass, attrfield, dataClass",
      "    JavaSrc.<&attrfield.type> get_<&attrfield.semantic>() { return <&attrfield.varName>; }  \n"
    + "  \n");

  
  
  private final OutTextPreparer tJavaMetaClassZbnf = new OutTextPreparer( "tJavaMetaClassZbnf", null, "attrfield, dataClass, cmpnClass",
      "    public void set_<&attrfield.semantic>(JavaSrc.<&attrfield.type> <&attrfield.varName>) { data<&cmpnClass>.<&attrfield.varName> = <&attrfield.varName>; }  \n\n");
  
  
  

  
  
  
  
  
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
    this.sJavaOutputDir = cmdArgs.dirJava.getAbsolutePath() + "/" + cmdArgs.sJavaPkg.replace(".","/") + "/";
    File fJavaOutputFileZbnf = new File(sJavaOutputDir + cmdArgs.sJavaClass + "_Zbnf.java");
    File fJavaOutputFile = new File(this.sJavaOutputDir + this.cmdArgs.sJavaClass + "_Base.java");
    File fJavaOutputFileUsg = new File(this.sJavaOutputDir + this.cmdArgs.sJavaClass + ".java");
    try {
      FileSystem.mkDirPath(sJavaOutputDir);
      wr = new FileWriter(fJavaOutputFile);  //new StringBuilder(); //
      wrz = new FileWriter(fJavaOutputFileZbnf);
      wru = new FileWriter(fJavaOutputFileUsg);
    } catch (IOException e) {
      System.err.println("cannot create: " + fJavaOutputFileZbnf.getAbsolutePath());
    }
    try {
      //Map<String, Object> argstxt = new TreeMap<String, Object>();
      OutTextPreparer.DataTextPreparer argsJavaHeadBase = this.tJavaHeadBase.createArgumentDataObj();
      OutTextPreparer.DataTextPreparer argsJavaHeadZbnf = this.sJavaHeadZbnf.createArgumentDataObj();
      OutTextPreparer.DataTextPreparer argsJavaHeadUsg = this.tJavaHeadUsg.createArgumentDataObj();
      argsJavaHeadBase.setArgument("pkgpath", this.cmdArgs.sJavaPkg);
      argsJavaHeadZbnf.setArgument("pkgpath", this.cmdArgs.sJavaPkg);
      argsJavaHeadUsg.setArgument("pkgpath", this.cmdArgs.sJavaPkg);
      argsJavaHeadBase.setArgument("dataClass", this.cmdArgs.sJavaClass);
      argsJavaHeadZbnf.setArgument("dataClass", this.cmdArgs.sJavaClass);
      argsJavaHeadUsg.setArgument("dataClass", this.cmdArgs.sJavaClass);

      this.tJavaHeadBase.exec(this.wr, argsJavaHeadBase);
      this.sJavaHeadZbnf.exec(this.wrz, argsJavaHeadZbnf);
      this.tJavaHeadUsg.exec(this.wru, argsJavaHeadUsg);
      this.wr.flush();
      this.wrz.flush();
      this.wru.flush();
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
      if(this.wr!=null) { 
//        File fJavaOutputFile = new File(this.sJavaOutputDir + this.cmdArgs.sJavaClass + ".java");
//        Writer fwr = new FileWriter(fJavaOutputFile);
//        fwr.append(this.wr);
        wr.close(); 
      }
      if(this.wrz!=null) { this.wrz.close(); }
      if(this.wru!=null) { this.wru.close(); }
    } catch (IOException e) {
      System.err.println("internal error cannot close Files: " + this.cmdArgs.dirJava);
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
    this.wr.append(tJavaCmpnEnd);
    this.wrz.append(tJavaCmpnEnd);
    this.wru.append(tJavaCmpnEnd);
    this.wr.flush();
    this.wrz.flush();
    this.wru.flush();
  }
  
  
  public void finishClassWrite() throws IOException {
    this.wr.append(tJavaEnd);
    this.wrz.append(tJavaEnd);
    this.wru.append(tJavaEnd);
    this.wr.flush();
    this.wrz.flush();
    this.wru.flush();
  }
  
  
  
  public class WrClassJava {
    public Map<String, String> variables = new TreeMap<String, String>();

    Writer_Appendable wrOp = new Writer_Appendable(new StringBuilder(1000));
    
    public WrClassJava() {}
    
    /**Writes a Class for a syntax Component.
     * @param cmpn
     * @throws IOException
     */
    public void wrClassCmpn(SubClassJava classData, String sSuperType) throws Exception {
      if(classData.sDbgIdent !=null && classData.sDbgIdent.equals("add_expression"))
        Debugutil.stop();
      if(sSuperType !=null && GenJavaOutClass.this.superTypes.get(sSuperType) ==null) {
        GenJavaOutClass.this.superTypes.put(sSuperType, sSuperType);           // store, only ones.
        OutTextPreparer.DataTextPreparer argsJavaSuperTypeClass = GenJavaOutClass.this.tJavaSuperTypeClass.createArgumentDataObj();
        argsJavaSuperTypeClass.setArgument("className", sSuperType); 
//        tJavaSuperTypeClass.exec(wr, argsJavaSuperTypeClass);           not necessary
//        GenJavaOutClass.this.wr.flush();
      }
      //Map<String, Object> argstxt = new TreeMap<String, Object>();
      OutTextPreparer.DataTextPreparer argsJavaCmpnClass = tJavaCmpnClass_Base.createArgumentDataObj();
      OutTextPreparer.DataTextPreparer argsJavaCmpnClassZbnf = tJavaCmpnClassZbnf.createArgumentDataObj();
      OutTextPreparer.DataTextPreparer argsJavaCmpnClassUsg = tJavaCmpnClassUsg.createArgumentDataObj();
      argsJavaCmpnClass.setArgument("cmpnClass", classData.className); //firstUppercase(cmpn.sDefinitionIdent));
      argsJavaCmpnClass.setArgument("superClass", sSuperType);
      argsJavaCmpnClass.setArgument("dataClass", cmdArgs.sJavaClass);
      argsJavaCmpnClass.setArgument("semantic", classData.semantic);
      argsJavaCmpnClassZbnf.setArgument("cmpnClass", classData.className); //firstUppercase(cmpn.sDefinitionIdent));
      argsJavaCmpnClassZbnf.setArgument("superClass", sSuperType);
      argsJavaCmpnClassZbnf.setArgument("dataClass", cmdArgs.sJavaClass);
      argsJavaCmpnClassZbnf.setArgument("semantic", classData.semantic);
      argsJavaCmpnClassUsg.setArgument("cmpnClass", classData.className); //firstUppercase(cmpn.sDefinitionIdent));
      argsJavaCmpnClassUsg.setArgument("superClass", sSuperType);
      argsJavaCmpnClassUsg.setArgument("dataClass", cmdArgs.sJavaClass);
      argsJavaCmpnClassUsg.setArgument("semantic", classData.semantic);
      tJavaCmpnClass_Base.exec(wr, argsJavaCmpnClass);
      tJavaCmpnClassZbnf.exec(wrz, argsJavaCmpnClassZbnf);
      tJavaCmpnClassUsg.exec(wru, argsJavaCmpnClassUsg);
      GenJavaOutClass.this.wr.flush();
      GenJavaOutClass.this.wrz.flush();
      GenJavaOutClass.this.wru.flush();
      //
      TreeMap<String, SubClassField> elems = classData.fieldsFromSemanticAttr;
      if(elems !=null) {
        OutTextPreparer.DataTextPreparer argsJavaMetaClass = sJavaMetaClass.createArgumentDataObj();
        OutTextPreparer.DataTextPreparer argsJavaMetaClassOper = sJavaMetaClassOper.createArgumentDataObj();
        OutTextPreparer.DataTextPreparer argsJavaMetaClassZbnf = tJavaMetaClassZbnf.createArgumentDataObj();
        
        for(Map.Entry<String, SubClassField> e : elems.entrySet()) {
          SubClassField attrfield = e.getValue();
          //TreeMap<String, Object> argstxt2 = new TreeMap<String, Object>();
          argsJavaMetaClass.setArgument("attrfield", attrfield);
          argsJavaMetaClassOper.setArgument("attrfield", attrfield);
          argsJavaMetaClassZbnf.setArgument("attrfield", attrfield);
          argsJavaMetaClassZbnf.setArgument("cmpnClass", classData.className);
          argsJavaCmpnClassZbnf.setArgument("dataClass", cmdArgs.sJavaClass);
          sJavaMetaClass.exec(wr, argsJavaMetaClass);
          sJavaMetaClassOper.exec(wrOp, argsJavaMetaClassOper);
          tJavaMetaClassZbnf.exec(wrz, argsJavaMetaClassZbnf);
          GenJavaOutClass.this.wr.flush();
          GenJavaOutClass.this.wrz.flush();

        }
      }
    }

    
    
    public void wrVariable(SubClassJava classData, String varNameArg, String typeNs, String varTypeRef, String varTypeObj
        , boolean bStdType, boolean bList, boolean bCmpn, List<String> args) throws IOException {

      if(typeNs ==null || typeNs.length()==0)
        Debugutil.stop();
      ///typeNs = "";
      
      if(varTypeRef.equals("Test_description"))
        Debugutil.stop();
      
      String varName = varNameArg.replace('-', '_');
      if(varName.length()==0) {
        varName="xxxx";
      }
      
      if(varName.equals("type"))
        Debugutil.stop();
      String varTypeStored = this.variables.get(varName);
      if(varTypeStored !=null) {
        if(!varTypeStored.equals(varTypeRef)){
          throw new IllegalArgumentException("same variable twice with different types");
        }
      } 
      else { //varName not found
        this.variables.put(varName, varTypeRef);
        String varNameJava = firstLowercase(varName);
        String varNameReplReserved = reservedNames.get(varNameJava);
        if(varNameReplReserved !=null) { varNameJava = varNameReplReserved; }
        String sTypeGeneric = idxStdTypes.get(varTypeRef);
        if(sTypeGeneric == null) { 
          sTypeGeneric = varTypeRef;
          //assert(bStdType == false);  //?? todo
        } else {
          if(!bStdType) { //stdType found, 
            varTypeRef += "__";  //it should not be a standard Java type.
          }
        }
        final String sTypeNsGeneric = typeNs == null ? sTypeGeneric : typeNs + sTypeGeneric;
        String sSuperType = classData.sSuperItemType;
        Map<String, Object> argstxt = new TreeMap<String, Object>();
        OutTextPreparer.DataTextPreparer argsJavaListVarOper = tJavaListVarOper.createArgumentDataObj();
        if(varTypeRef.contains("Expression"))
          Debugutil.stop();
        if(varTypeRef.contains("ExprPart"))
          Debugutil.stop();
        
        argsJavaListVarOper.setArgument("typeNs", typeNs);
        argsJavaListVarOper.setArgument("typeGeneric", sTypeGeneric);
        argsJavaListVarOper.setArgument("varName", varNameJava);
        argsJavaListVarOper.setArgument("name", varName);
        argsJavaListVarOper.setArgument("type", varTypeRef);
        argsJavaListVarOper.setArgument("typeZbnf", varTypeObj);
        argsJavaListVarOper.setArgument("args", args);
        argsJavaListVarOper.setArgument("dataClass", GenJavaOutClass.this.cmdArgs.sJavaClass);
        argsJavaListVarOper.setArgument("cmpnClass", classData.className); //firstUppercase(cmpn.sDefinitionIdent));
        
        OutTextPreparer.DataTextPreparer argsJavaListVar = tJavaListVar.createArgumentDataObj();
        argsJavaListVar.setArgument("typeNs", typeNs);
        argsJavaListVar.setArgument("typeGeneric", sTypeGeneric);
        argsJavaListVar.setArgument("varName", varNameJava);
        argsJavaListVar.setArgument("name", varName);
        argsJavaListVar.setArgument("type", varTypeRef);
        argsJavaListVar.setArgument("typeZbnf", varTypeObj);
        argsJavaListVar.setArgument("args", args);
        argsJavaListVar.setArgument("dataClass", GenJavaOutClass.this.cmdArgs.sJavaClass);
        argsJavaListVar.setArgument("cmpnClass", classData.className); //firstUppercase(cmpn.sDefinitionIdent));
        
        OutTextPreparer.DataTextPreparer argsJavaListVarZbnf = tJavaListVarZbnf.createArgumentDataObj();
        argsJavaListVarZbnf.setArgument("typeGeneric", sTypeNsGeneric);
        argsJavaListVarZbnf.setArgument("varName", varNameJava);
        argsJavaListVarZbnf.setArgument("name", varName);
        argsJavaListVarZbnf.setArgument("type", varTypeRef);
        argsJavaListVarZbnf.setArgument("typeNs", typeNs);
        argsJavaListVarZbnf.setArgument("typeZbnf", varTypeObj);
        argsJavaListVarZbnf.setArgument("args", args);
        argsJavaListVarZbnf.setArgument("dataClass", GenJavaOutClass.this.cmdArgs.sJavaClass);
        argsJavaListVarZbnf.setArgument("cmpnClass", classData.className); //firstUppercase(cmpn.sDefinitionIdent));
        
        OutTextPreparer.DataTextPreparer argsJavaListCmpnZbnf = GenJavaOutClass.this.tJavaListCmpnZbnf.createArgumentDataObj();
        argsJavaListCmpnZbnf.setArgument("typeGeneric", sTypeNsGeneric);
        argsJavaListCmpnZbnf.setArgument("superType", classData.sSuperItemType);
        argsJavaListCmpnZbnf.setArgument("cmpnClass", classData.className); //firstUppercase(cmpn.sDefinitionIdent));
        argsJavaListCmpnZbnf.setArgument("varName", varNameJava);
        argsJavaListCmpnZbnf.setArgument("name", varName);
        argsJavaListCmpnZbnf.setArgument("type", varTypeRef);
        argsJavaListCmpnZbnf.setArgument("typeNs", typeNs);
        argsJavaListCmpnZbnf.setArgument("typeZbnf", varTypeObj);
        argsJavaListCmpnZbnf.setArgument("args", args);
        argsJavaListCmpnZbnf.setArgument("dataClass", GenJavaOutClass.this.cmdArgs.sJavaClass);
        
        OutTextPreparer.DataTextPreparer argsJavaSimpleVarOper = tJavaSimpleVarOper.createArgumentDataObj();
        argsJavaSimpleVarOper.setArgument("typeNs", typeNs);
        argsJavaSimpleVarOper.setArgument("typeGeneric", sTypeGeneric);
        argsJavaSimpleVarOper.setArgument("varName", varNameJava);
        argsJavaSimpleVarOper.setArgument("name", varName);
        argsJavaSimpleVarOper.setArgument("type", varTypeRef);
        argsJavaSimpleVarOper.setArgument("typeZbnf", varTypeObj);
        argsJavaSimpleVarOper.setArgument("args", args);
        argsJavaSimpleVarOper.setArgument("dataClass", GenJavaOutClass.this.cmdArgs.sJavaClass);
        argsJavaSimpleVarOper.setArgument("cmpnClass", classData.className); //firstUppercase(cmpn.sDefinitionIdent));
        
        OutTextPreparer.DataTextPreparer argsJavaSimpleVarZbnf = tJavaSimpleVarZbnf.createArgumentDataObj();
        argsJavaSimpleVarZbnf.setArgument("typeGeneric", sTypeNsGeneric);
        argsJavaSimpleVarZbnf.setArgument("cmpnClass", classData.className);
        argsJavaSimpleVarZbnf.setArgument("varName", varNameJava);
        if(varName.contains("ExprPart"))
          Debugutil.stop();
        if(varNameJava.contains("RegularExpression"))
          Debugutil.stop();
        argsJavaSimpleVarZbnf.setArgument("name", varName);
        argsJavaSimpleVarZbnf.setArgument("type", varTypeRef);
        argsJavaSimpleVarZbnf.setArgument("typeNs", typeNs);
        argsJavaSimpleVarZbnf.setArgument("typeZbnf", varTypeObj);
        argsJavaSimpleVarZbnf.setArgument("args", args);
        argsJavaSimpleVarZbnf.setArgument("dataClass", GenJavaOutClass.this.cmdArgs.sJavaClass);
        
        OutTextPreparer.DataTextPreparer argsJavaCmpnZbnf = GenJavaOutClass.this.tJavaCmpnZbnf.createArgumentDataObj();
        argsJavaCmpnZbnf.setArgument("typeGeneric", sTypeNsGeneric);
        argsJavaCmpnZbnf.setArgument("superType", classData.sSuperItemType);
        argsJavaCmpnZbnf.setArgument("varName", varNameJava);
        argsJavaCmpnZbnf.setArgument("name", varName);
        argsJavaCmpnZbnf.setArgument("type", varTypeRef);
        argsJavaCmpnZbnf.setArgument("typeNs", typeNs);
        argsJavaCmpnZbnf.setArgument("typeZbnf", varTypeObj);
        argsJavaCmpnZbnf.setArgument("args", args);
        argsJavaCmpnZbnf.setArgument("dataClass", GenJavaOutClass.this.cmdArgs.sJavaClass);
        argsJavaCmpnZbnf.setArgument("cmpnClass", classData.className); //firstUppercase(cmpn.sDefinitionIdent));
        
        OutTextPreparer.DataTextPreparer argsJavaSimpleVar = tJavaSimpleVar.createArgumentDataObj();
        argsJavaSimpleVar.setArgument("typeNs", typeNs);
        argsJavaSimpleVar.setArgument("typeGeneric", sTypeGeneric);
        argsJavaSimpleVar.setArgument("varName", varNameJava);
        argsJavaSimpleVar.setArgument("name", varName);
        argsJavaSimpleVar.setArgument("type", varTypeRef);
        argsJavaSimpleVar.setArgument("typeZbnf", varTypeObj);
        argsJavaSimpleVar.setArgument("args", args);
        argsJavaSimpleVar.setArgument("dataClass", GenJavaOutClass.this.cmdArgs.sJavaClass);
        argsJavaSimpleVar.setArgument("cmpnClass", classData.className); //firstUppercase(cmpn.sDefinitionIdent));
        
        //because of debugging write firstly to a StringBuilder:
        Writer_Appendable wrb = new Writer_Appendable(new StringBuilder());
        Writer_Appendable wrzb = new Writer_Appendable(new StringBuilder());
        
        if(bList) {
          tJavaListVar.exec(wrb, argsJavaListVar);
          tJavaListVarOper.exec(wrOp, argsJavaListVarOper);
          if(bStdType) {
            tJavaListVarZbnf.exec(wrzb, argsJavaListVarZbnf);
          }
          else if(bCmpn) {
            tJavaListCmpnZbnf.exec(wrzb, argsJavaListCmpnZbnf);
          } 
          else {
            tJavaListVarZbnf.exec(wrzb, argsJavaListVarZbnf);
          }
        } else {
          tJavaSimpleVar.exec(wrb, argsJavaSimpleVar);
          tJavaSimpleVarOper.exec(wrOp, argsJavaSimpleVarOper);
          if(bStdType) {
            tJavaSimpleVarZbnf.exec(wrzb, argsJavaSimpleVarZbnf);
          }
          else if(bCmpn) {
            tJavaCmpnZbnf.exec(wrzb, argsJavaCmpnZbnf);
          } 
          else {
            tJavaSimpleVarZbnf.exec(wrzb, argsJavaSimpleVarZbnf);
          }
        }
        wr.append(wrb.getContent()); //now append to output, remove wrb as stack local ref 
        wr.flush();
        wrz.append(wrzb.getContent());
        wrz.flush();
      }
    }
    
    

    public void writeOperations() throws IOException {
      wr.append(wrOp.getContent());
      wrOp.clear();
      wr.flush();
    }
    
  }
 

  
  
}
