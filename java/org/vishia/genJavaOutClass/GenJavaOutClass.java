package org.vishia.genJavaOutClass;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.vishia.msgDispatch.LogMessage;
import org.vishia.util.Debugutil;
import org.vishia.util.FileFunctions;
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
public final class GenJavaOutClass {

  /**Version, history and license.
   * <ul>
   * <li>2025-12-14 tJava... some 'this.' completed.
   * <li>{@link #tJavaSimpleVarZbnf}, {@link #tJavaCmpnZbnf}, {@link #tJavaListCmpnZbnf}: 
   *  new argument 'nameOp' as name for the operation to create for storing.
   *  Hence the really defined operation is yet used, not a presumed 'set_NAME'.
   * <li>{@link WrClassJava#wrVariable(SubClassJava, String, String, String, String, boolean, String, String, boolean, boolean, boolean, List)} 
   *   wrVariable(..., sAccess, bAccessOp, ...): The access oper should be given. 
   * <li>2024-05-24 Warning-free without changes 
   * <li>2024-05-08 In all otx scripts now generates a comment line in the Java src which script is used there. Starts with //otx-Template
   * <li>2024-05-08: new {@link #convertToIdentifier(String)} because in odt files text:s is used etc. , the name space was not regarded till now.
   * <li>2024-05-08 in {@link #tJavaCmpnClassUsg}: use "&lt:&dataClass>_Base" instead "JavaSrc_Base", old manual after generation fixed mistake
   * <li>2024-05-08 some formally changes, using this. etc. 
   * <li>2022-06-01 Hartmut: instead "JavaSrc" (special usage) generate name of the correct frame class. 
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
   *   here some improvement opportunities.  
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
  public static final String sVersion = "2025-12-14";

  
  
  
  
  /**Command line args */
  public static class CmdArgs{
    
    /**The input file which determines the structure. */
    public File fileInput = null;
    
    /**A file which should show the inner syntax structure from fileInput given. */
    public File fileOutStruct = null;
    
    /**Directory where the root of the package path is to write the generated class.java files. 
     * This directory should be existing. */
    public File dirJava;
    
    /**The package where the classes are created into, also determining the path starting from {@link #dirJava}.
     * The package dirs should be separated with dot, as in a Java package definition. */
    public String sJavaPkg;
    
    /**Name of the User Java class. 
     * Additional a class _Base and a class _Zbnf will be created in {@link #sJavaPkg}. */
    public String sJavaClass;
  
    /**TODO description
     * 
     */
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
  protected final CmdArgs cmdArgs;

  protected final LogMessage log;
  
  
  /**Writer for the base data class and the Zbnf JavaOut class.
   * 
   */
  protected Writer wrz, wru;

  protected Writer wr;
  
  protected String sJavaOutputDir;
  
  Map<String, String> superTypes = new TreeMap<String, String>();
  
  /**StandardTypes. */
  public final TreeMap<String, String> idxStdTypes = new TreeMap<String, String>();

  
  
  
  
  /**The syntax components which are to process yet (are used for parse result storing).  */
  public List<SubClassJava> listCmpn = new LinkedList<SubClassJava>();
  
  /**Index of already registered components to add in {@link #listCmpn} only one time. */
  public Map<String, SubClassJava> idxRegisteredCmpn = new TreeMap<String, SubClassJava>();
  
  protected final static Map<String, String> reservedNames = new TreeMap<String, String>();
  
  /**Text for Java header. */
  protected final OutTextPreparer tJavaHeadBase = new OutTextPreparer("tJavaHead", null, "pkgpath, dataClass", 
      "package <&pkgpath>;\n"
    + "//otx-Template org.vishia.genJavaOutClass.GenJavaOutClass#tJavaHeadBase\n"
    + "\n"
    + "import java.util.LinkedList;\n"
    + "import java.util.List;\n"
    + "import org.vishia.genJavaOutClass.SrcInfo;\n"
    + "import org.vishia.util.NameObj;\n"
    + "\n"
    + "\n"
    + "/**This file is generated by genJavaOut.jzTc script. */\n"
    + "public class <&dataClass>_Base extends SrcInfo {\n"
    + "\n");
  
  /**Text for Java usage header. */
  private final OutTextPreparer tJavaHeadUsg = new OutTextPreparer("tJavaHeadUsg", null, "pkgpath, dataClass", 
      "package <&pkgpath>;\n"
    + "//otx-Template org.vishia.genJavaOutClass.GenJavaOutClass#tJavaHeadUsg\n"
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
    + "//otx-Template org.vishia.genJavaOutClass.GenJavaOutClass#tJavaHeadZbnf\n"
    + "\n"
    + "import java.util.LinkedList;\n"
    + "import java.util.List;\n"
    + "import org.vishia.util.NameObj;\n"
    + "import org.vishia.util.SetLineColumn_ifc;\n"
    + "\n"
    + "/**This file is generated by genJavaOut.jzTc script. \n"
    + " * It is the derived class to write Zbnf result. */\n"
    + "public class <&dataClass>_Zbnf {\n"
    + "  \n"
    + "  /**Referenced working instance will be filled.*/\n"
    + "  public final <&dataClass> dataroot;\n"
    + "  \n"
    + "  /**Default ctor for non-inherit instance. */\n"
    + "  public <&dataClass>_Zbnf ( ) {\n"
    + "    this.dataroot = new <&dataClass>();\n"
    + "  }\n"
    + "  \n"
    + "  \n"
    + "  \n");
  
  /**Text for class header for syntax component data storing. */
  protected final OutTextPreparer tJavaSuperTypeClass = new OutTextPreparer( "tJavaSuperTypeClass", null, "className, dataClass",
      "\n"
    + "\n"
    + "\n"
    + "  //otx-Template org.vishia.genJavaOutClass.GenJavaOutClass#tJavaSuperTypeClass\n"
    + "  /**Class for Super Types. */\n"
    + "  public abstract static class <&className> {\n"
    + "    //left empty, implementors contains all ...\n"
    + "  }\n"
    + "  \n");
  
  /**Text for class header for syntax component data storing. */
  protected final OutTextPreparer tJavaCmpnClass_Base = new OutTextPreparer( "tJavaCmpnClass_Base", null, "cmpnClass, superClass, dataClass, semantic",
      "\n"
    + "\n"
    + "\n"
    + "  //otx-Template org.vishia.genJavaOutClass.GenJavaOutClass#tJavaCmpnClass_Base\n"
    + "  /**Class for Component <&semantic>. */\n"
    + "  public static class <&cmpnClass>_Base extends <:if:superClass><&dataClass>.<&superClass><:else>SrcInfo<.if> {\n"
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
  protected final OutTextPreparer tJavaCmpnClassZbnf = new OutTextPreparer( "tJavaCmpnClassZbnf", null, "cmpnClass, superClass, dataClass, semantic",
      "\n"
    + "\n"
    + "\n"
    + "  //otx-Template org.vishia.genJavaOutClass.GenJavaOutClass#tJavaCmpnClassZbnf\n"
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
  protected final OutTextPreparer tJavaCmpnClassUsg = new OutTextPreparer( "tJavaCmpnClassUsg", null, "cmpnClass, superClass, dataClass, semantic",
      "\n"
    + "\n"
    + "\n"
    + "  //otx-Template org.vishia.genJavaOutClass.GenJavaOutClass#tJavaCmpnClassUsg\n"
    + "  /**Class for Writing the Component <&semantic>.*/\n"
    + "  public static class <&cmpnClass> extends <&dataClass>_Base.<&cmpnClass>_Base {\n"
    + "  \n"
    + "  \n"
    + "  \n"
    + "  \n"
    + "  \n"
    + "    @Override public String toString ( ) { \n"
    + "      return \"TODO toString\";\n"
    + "    }\n"
    );
  
  protected static final String tJavaCmpnEnd = 
      "  \n"
    + "  }  //otx-Template org.vishia.genJavaOutClass.GenJavaOutClass#tJavaCmpnEnd\n"
    + "\n";
  
  protected static final  String tJavaEnd = 
      "\n"
    + "}   //otx-Template org.vishia.genJavaOutClass.GenJavaOutClass#tJavaEnd\n"
    + "\n"
    + "\n";
  
  protected final OutTextPreparer tJavaSimpleVar = new OutTextPreparer(  "sJavaSimpleVar", null, "typeNs, cmpnClass, typeGeneric, dataClass, varName, name, type, typeZbnf, docu, args",
      "    \n"
    + "    //otx-Template org.vishia.genJavaOutClass.GenJavaOutClass#tJavaSimpleVar\n"
    + "<:if:docu>    /**<&docu> */\n<.if>" 
    + "    protected <:if:typeNs><&typeNs><.if><&type> <&varName>;\n"
    + "    \n"
    + "    \n");
  
  protected final OutTextPreparer tJavaListVar = new OutTextPreparer(  "sJavaListVar", null, "typeNs, cmpnClass, typeGeneric, dataClass, varName, name, type, typeZbnf, docu, args",
      "    \n"
    + "    //otx-Template org.vishia.genJavaOutClass.GenJavaOutClass#tJavaListVar\n"
    + "<:if:docu>    /**<&docu> */\n<.if>" 
    + "    protected List<<:if:typeNs><&typeNs><.if><&typeGeneric>> <&varName>;\n"
    + "    \n"
    + "    \n");
  
  protected final OutTextPreparer tJavaListAllVar = new OutTextPreparer(  "sJavaListAllVar", null, "docu",
      "    \n"
    + "    //otx-Template org.vishia.genJavaOutClass.GenJavaOutClass#tJavaListAllVar\n"
    + "    /**All nodes in natural order. <:if:docu>\n     * <&docu> <.if>*/\n" 
    + "    protected List<:<>NameObj> listAllNodes = new LinkedList<:<>NameObj>();\n"
    + "    \n"
    + "    \n");
  
  protected final OutTextPreparer tJavaSimpleVarOper = new OutTextPreparer( "sJavaSimpleVarOper", null, "typeNs, cmpnClass, typeGeneric, dataClass, varName, name, type, typeZbnf, args",
      "    \n    \n"
    + "    //otx-Template org.vishia.genJavaOutClass.GenJavaOutClass#tJavaSimpleVarOper\n"
    + "    /**Access to parse result.*/\n"
    + "    public <:if:typeNs><&typeNs><.if><&type> get_<&name>() { return this.<&varName>; }\n"
    + "    \n"
    + "    \n");
  
  protected final OutTextPreparer tJavaListVarOper = new OutTextPreparer( "sJavaListVarOper", null, "typeNs, cmpnClass, typeGeneric, dataClass, varName, name, type, typeZbnf, args",
      "    \n    \n"
    + "    //otx-Template org.vishia.genJavaOutClass.GenJavaOutClass#tJavaListVarOper\n"
    + "    /**Access to parse result, get the elements of the container <&name>*/\n"
    + "    public Iterable<<:if:typeNs><&typeNs><.if><&typeGeneric>> get_<&name>() { return this.<&varName>; }\n"
    + "    \n"
    + "    /**Access to parse result, get the size of the container <&name>.*/\n"
    + "    public int getSize_<&name>() { return this.<&varName> ==null ? 0 : this.<&varName>.size(); }\n"
    + "    \n"
    + "    \n");
  
  
  /**
   * <ul>
   * <li>typeNs: null on simple types as String, int.   
   * <li>cmpnClass: The name of the syntax component. It builts the name of the data... variable too. 
   * </ul>
   */
  protected final OutTextPreparer tJavaSimpleVarZbnf = new OutTextPreparer( "tJavaSimpleVarZbnf", null, "typeGeneric, dataClass, cmpnClass, varName, nameOp, name, typeNs, type, typeZbnf, args",
      "    //otx-Template org.vishia.genJavaOutClass.GenJavaOutClass#tJavaSimpleVarZbnf\n"
    + "    /**Set routine for the singular component &lt;<&type>?<&name>>. */\n"
//    + "     * <:if:typeNs>Component for argument: <&typeNs><.if>*/\n"
    + "    public void <&nameOp>(<&type><:if:typeNs>_Zbnf<.if> val) { this.data<&cmpnClass>.<&varName> = val<:if:typeNs>.data<&type><.if>; }\n"
    + "    \n"
    + "    \n");
  
  protected final OutTextPreparer tJavaListVarZbnf = new OutTextPreparer( "tJavaListVarZbnf", null, "bListAll, cmpnClass, typeGeneric, dataClass, varName, nameOp, name, typeNs, type, typeZbnf, args",
      "    //otx-Template org.vishia.genJavaOutClass.GenJavaOutClass#tJavaListVarZbnf\n"
    + "    /**Set routine for the singular component &lt;<&type>?<&name>>. */\n"
    + "    public void <&nameOp>(<&type><:if:typeNs>_Zbnf<.if> val) { \n"
    + "      if(this.data<&cmpnClass>.<&varName>==null) { this.data<&cmpnClass>.<&varName> = new LinkedList<<&typeGeneric>>(); }\n"
    + "      this.data<&cmpnClass>.<&varName>.add(val<:if:typeNs>.data<&type><.if>); \n"
    + "<:if:bListAll>      this.data<&cmpnClass>.listAllNodes.add(new NameObj(\"<&name>\", val<:if:typeNs>.data<&type><.if>)); \n<.if>"
    + "    }\n"
    + "    \n"
    + "    \n");
  
  
  protected final OutTextPreparer tJavaCmpnZbnf = new OutTextPreparer( "tJavaCmpnZbnf", null, "cmpnClass, typeGeneric, dataClass, superType, varName, name, nameOp, typeNs, type, typeZbnf, args",
      "    //otx-Template org.vishia.genJavaOutClass.GenJavaOutClass#tJavaCmpnZbnf\n"
    + "    /**Creates an instance for the result Zbnf <:if:args> (not Xml) <.if>. &lt;<&typeZbnf>?<&name>&gt; for ZBNF data store*/\n"
    + "    <:if:xxxsuperType>@Override <.if>public <&typeZbnf>_Zbnf new_<&name>() { \n"
    + "      <&typeZbnf>_Zbnf val = new <&typeZbnf>_Zbnf();\n"
    + "      return val; //Note: needs the derived Zbnf-Type.\n"
    + "    }\n"
    + "    \n"  //<:debug:name:FBType>"
    + "<:if:args>"
    + "    /**Creates an instance for the Xml data storage with default attibutes. &lt;<&typeZbnf>?<&name>&gt;  */\n"
    + "    public <&typeZbnf>_Zbnf new_<&name>(<:for:arg:args>String <&arg><:if:arg_next>,<.if> <.for>) {\n"
    + "      <&typeZbnf>_Zbnf val = new <&typeZbnf>_Zbnf();\n"
    + "      <:for:arg:args>val.data<&type>.<&arg> = <&arg>;\n"
    + "      <.for>//\n"
    + "      return val; //Note: needs the derived Zbnf-Type.\n"
    + "    }\n"
    + "    \n"
    + "<.if>"
    + "    /**Set the result. &lt;<&typeZbnf>?<&name>&gt;*/\n"
    + "    public void <&nameOp>(<&type><:if:typeNs>_Zbnf<.if> val) {\n"
    + "      this.data<&cmpnClass>.<&varName> = val<:if:typeNs>.data<&type><.if>;\n"
    + "    }\n"
    + "    \n"
    + "    \n");
  
  protected final OutTextPreparer tJavaListCmpnZbnf = new OutTextPreparer( "tJavaListCmpnZbnf", null, "bListAll, typeGeneric, dataClass, cmpnClass, superType, varName, nameOp, name, typeNs, type, typeZbnf, args",
      "    //otx-Template org.vishia.genJavaOutClass.GenJavaOutClass#tJavaListCmpnZbnf\n"
    + "    /**create and add routine for the list component <<&typeZbnf>?<&name>>. */\n"
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
    + "    public void <&nameOp>(<&type><:if:typeNs>_Zbnf<.if> val) {\n"
    + "      if( this.data<&cmpnClass>.<&varName>==null ) { this.data<&cmpnClass>.<&varName> = new LinkedList<<&typeGeneric>>(); }\n"
    + "<:if:bListAll>      this.data<&cmpnClass>.listAllNodes.add(new NameObj(\"<&varName>\", val<:if:typeNs>.data<&type><.if>));\n<.if>"
    + "      this.data<&cmpnClass>.<&varName>.add(val<:if:typeNs>.data<&type><.if>); \n"
    + "    }\n"
    + "    \n"
    + "    \n");
  
  protected final OutTextPreparer sJavaMetaClass = new OutTextPreparer( "sJavaMetaClass", null, "cmpnClass, attrfield, dataClass",
      "    //otx-Template org.vishia.genJavaOutClass.GenJavaOutClass#sJavaMetaClass\n"
    + "    <&dataClass>.<&attrfield.type> <&attrfield.varName>;  \n"
    + "  \n");
  

  protected final OutTextPreparer sJavaMetaClassOper = new OutTextPreparer( "sJavaMetaClass", null, "cmpnClass, attrfield, dataClass",
      "    //otx-Template org.vishia.genJavaOutClass.GenJavaOutClass#sJavaMetaOper\n"
    + "    <&dataClass>.<&attrfield.type> get_<&attrfield.semantic>() { return <&attrfield.varName>; }  \n"
    + "  \n");

  
  
  protected final OutTextPreparer tJavaMetaClassZbnf = new OutTextPreparer( "tJavaMetaClassZbnf", null, "attrfield, dataClass, cmpnClass",
      "    //otx-Template org.vishia.genJavaOutClass.GenJavaOutClass#tJavaMetaClassZbnf\n"
    + "    public void set_<&attrfield.semantic>(<&dataClass>.<&attrfield.type> <&attrfield.varName>) { "
    + "data<&cmpnClass>.<&attrfield.varName> = <&attrfield.varName>; }  \n\n");
  
  
  

  
  
  
  
  
  public GenJavaOutClass(CmdArgs args, LogMessage log)
  { this.cmdArgs = args;
    this.log = log;
    this.idxStdTypes.put("boolean","Boolean");
    this.idxStdTypes.put("float","Float");
    this.idxStdTypes.put("int","Integer");
    this.idxStdTypes.put("String","String");
    this.idxStdTypes.put("double","Double");
    this.idxStdTypes.put("long","Long");
    this.idxStdTypes.put("Boolean","Boolean");
    this.idxStdTypes.put("Float","Float");
    this.idxStdTypes.put("Integer","Integer");
    this.idxStdTypes.put("Double","Double");
    this.idxStdTypes.put("Long","Long");
    
    reservedNames.put("const", "const___");
    reservedNames.put("final", "final___");
    reservedNames.put("class", "class___");
    reservedNames.put("interface", "interface___");
  }
  
  public void setupWriter() {
    this.sJavaOutputDir = this.cmdArgs.dirJava.getAbsolutePath() + "/" + this.cmdArgs.sJavaPkg.replace(".","/") + "/";
    File fJavaOutputFileZbnf = new File(this.sJavaOutputDir + this.cmdArgs.sJavaClass + "_Zbnf.java");
    File fJavaOutputFile = new File(this.sJavaOutputDir + this.cmdArgs.sJavaClass + "_Base.java");
    File fJavaOutputFileUsg = new File(this.sJavaOutputDir + this.cmdArgs.sJavaClass + ".java");
    try {
      FileFunctions.mkDirPath(this.sJavaOutputDir);
      this.wr = new FileWriter(fJavaOutputFile);  //new StringBuilder(); //
      this.wrz = new FileWriter(fJavaOutputFileZbnf);
      this.wru = new FileWriter(fJavaOutputFileUsg);
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
      this.log.writeError("Unexpected EXCEPTION ", e1);
    }
  }
  
  
  
  public void closeWrite() {
    try {
      if(this.wr!=null) { 
//        File fJavaOutputFile = new File(this.sJavaOutputDir + this.cmdArgs.sJavaClass + ".java");
//        Writer fwr = new FileWriter(fJavaOutputFile);
//        fwr.append(this.wr);
        this.wr.close(); 
      }
      if(this.wrz!=null) { this.wrz.close(); }
      if(this.wru!=null) { this.wru.close(); }
    } catch (IOException e) {
      System.err.println("internal error cannot close Files: " + this.cmdArgs.dirJava);
    }

  }
  
  
  public static String convertToIdentifier(String src) {
    StringBuilder sb = null;
    for(int ix = 0; ix < src.length(); ++ix) {
      char c1 = src.charAt(ix);
      if(c1 >='0' && c1 <= '9' || c1 >= 'A' && c1 <= 'Z' || c1 >= 'a' && c1 <= 'z' || c1 == '_') {
      } else {
        if(sb == null) { sb = new StringBuilder(src); }
        sb.setCharAt(ix, '_');                             // replace a non identifier char with '_', it is often unique. 
      }
    }
    return sb !=null ? sb.toString() : src;
  }
  
  
  
  
  public static String firstUppercaseIdentifier(String src) {
    String src1 = convertToIdentifier(src);
    char cc = src1.charAt(0);
    if(Character.isUpperCase(cc)) return src1;
    else return Character.toUpperCase(cc) + src1.substring(1);
  }

  public static String firstLowercaseIdentifier(String src) {
    String src1 = convertToIdentifier(src);
    char cc = src1.charAt(0);
    if(Character.isLowerCase(cc)) return src1;
    else return Character.toLowerCase(cc) + src1.substring(1);
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

    /**This StringBuilder as Appendable for generation is necessary because the variables and operations are created in the same {@link #wrVariable(SubClassJava, String, String, String, String, String, boolean, boolean, boolean, List)}
     * but write to file afterwards. It is the temporary buffer. */
    Writer_Appendable wrOp = new Writer_Appendable(new StringBuilder(1000));
    
    protected boolean bWriteAllListNodes = false; 
    
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
      OutTextPreparer.DataTextPreparer argsJavaCmpnClass = GenJavaOutClass.this.tJavaCmpnClass_Base.createArgumentDataObj();
      OutTextPreparer.DataTextPreparer argsJavaCmpnClassZbnf = GenJavaOutClass.this.tJavaCmpnClassZbnf.createArgumentDataObj();
      OutTextPreparer.DataTextPreparer argsJavaCmpnClassUsg = GenJavaOutClass.this.tJavaCmpnClassUsg.createArgumentDataObj();
      argsJavaCmpnClass.setArgument("cmpnClass", classData.className); //firstUppercase(cmpn.sDefinitionIdent));
      argsJavaCmpnClass.setArgument("superClass", sSuperType);
      argsJavaCmpnClass.setArgument("dataClass", GenJavaOutClass.this.cmdArgs.sJavaClass);
      argsJavaCmpnClass.setArgument("semantic", classData.semantic);
      argsJavaCmpnClassZbnf.setArgument("cmpnClass", classData.className); //firstUppercase(cmpn.sDefinitionIdent));
      argsJavaCmpnClassZbnf.setArgument("superClass", sSuperType);
      argsJavaCmpnClassZbnf.setArgument("dataClass", GenJavaOutClass.this.cmdArgs.sJavaClass);
      argsJavaCmpnClassZbnf.setArgument("semantic", classData.semantic);
      argsJavaCmpnClassUsg.setArgument("cmpnClass", classData.className); //firstUppercase(cmpn.sDefinitionIdent));
      argsJavaCmpnClassUsg.setArgument("superClass", sSuperType);
      argsJavaCmpnClassUsg.setArgument("dataClass", GenJavaOutClass.this.cmdArgs.sJavaClass);
      argsJavaCmpnClassUsg.setArgument("semantic", classData.semantic);
      GenJavaOutClass.this.tJavaCmpnClass_Base.exec(GenJavaOutClass.this.wr, argsJavaCmpnClass);
      GenJavaOutClass.this.tJavaCmpnClassZbnf.exec(GenJavaOutClass.this.wrz, argsJavaCmpnClassZbnf);
      GenJavaOutClass.this.tJavaCmpnClassUsg.exec(GenJavaOutClass.this.wru, argsJavaCmpnClassUsg);
      GenJavaOutClass.this.wr.flush();
      GenJavaOutClass.this.wrz.flush();
      GenJavaOutClass.this.wru.flush();
      //
      TreeMap<String, SubClassField> elems = classData.fieldsFromSemanticAttr;
      if(elems !=null) {
        OutTextPreparer.DataTextPreparer argsJavaMetaClass = GenJavaOutClass.this.sJavaMetaClass.createArgumentDataObj();
        OutTextPreparer.DataTextPreparer argsJavaMetaClassOper = GenJavaOutClass.this.sJavaMetaClassOper.createArgumentDataObj();
        OutTextPreparer.DataTextPreparer argsJavaMetaClassZbnf = GenJavaOutClass.this.tJavaMetaClassZbnf.createArgumentDataObj();
        
        for(Map.Entry<String, SubClassField> e : elems.entrySet()) {
          SubClassField attrfield = e.getValue();
          //TreeMap<String, Object> argstxt2 = new TreeMap<String, Object>();
          argsJavaMetaClass.setArgument("attrfield", attrfield);
          argsJavaMetaClassOper.setArgument("attrfield", attrfield);
          argsJavaMetaClassZbnf.setArgument("attrfield", attrfield);
          argsJavaMetaClassZbnf.setArgument("cmpnClass", classData.className);
          argsJavaCmpnClassZbnf.setArgument("dataClass", GenJavaOutClass.this.cmdArgs.sJavaClass);
          GenJavaOutClass.this.sJavaMetaClass.exec(GenJavaOutClass.this.wr, argsJavaMetaClass);
          GenJavaOutClass.this.sJavaMetaClassOper.exec(this.wrOp, argsJavaMetaClassOper);
          GenJavaOutClass.this.tJavaMetaClassZbnf.exec(GenJavaOutClass.this.wrz, argsJavaMetaClassZbnf);
          GenJavaOutClass.this.wr.flush();
          GenJavaOutClass.this.wrz.flush();

        }
      }
    }

    
    
    /**Writes all necessities to one variable of the 'classData':
     * <ul>
     * <li>
     * </ul>
     * @param classData The class where this variable is member of, it accesses here {@link SubClassJava#className} and {@link SubClassJava#sSuperItemType}
     * @param varNameArg
     * @param sOuterClass Either null for a standard type, or the name of the environment class where all types are defined as sub type, then with trainling "."
     * @param varTypeRefArg
     * @param sAccess The name of the operation as should be used in the Zbnf Writer operation
     * @param bAccOper usual true (not used yet) true then sAccess is an operation.
     * @param varTypeObj
     * @param sDocu
     * @param bStdType
     * @param bList
     * @param bCmpn
     * @param args
     * @throws IOException
     */
    public void wrVariable(SubClassJava classData, String varNameArg, String sOuterClass, String varTypeRefArg, String sAccess, boolean bAccOper, String varTypeObj, String sDocu
        , boolean bStdType, boolean bList, boolean bCmpn, List<String> args) throws IOException {

      //if(sOuterClass ==null || sOuterClass.length()==0) Debugutil.stopp();
      //if(varNameArg.equals("_textContent")) Debugutil.stopp();
      ///typeNs = "";
      this.bWriteAllListNodes |= bList;
      if(varTypeRefArg.equals("Test_description"))
        Debugutil.stop();
      
      String varName = varNameArg.replace('-', '_');
      if(varName.length()==0) {
        varName="xxxx";
      }
      
      if(varName.equals("type"))
        Debugutil.stop();
      String varTypeStored = this.variables.get(varName);
      if(varTypeStored !=null) {
        if(!varTypeStored.equals(varTypeRefArg)){
          throw new IllegalArgumentException("same variable twice with different types");
        }
      } 
      else { //varName not found
        this.variables.put(varName, varTypeRefArg);
        String varNameJava = firstLowercaseIdentifier(varName);
        String varNameReplReserved = reservedNames.get(varNameJava);
        if(varNameReplReserved !=null) { varNameJava = varNameReplReserved; }
        String sTypeGeneric = GenJavaOutClass.this.idxStdTypes.get(varTypeRefArg);
        final String varTypeRef;
        if(sTypeGeneric == null) { 
          sTypeGeneric = varTypeRefArg;
          varTypeRef = varTypeRefArg;
          //assert(bStdType == false);  //?? todo
        } else {
          if(!bStdType) { //stdType found, 
            varTypeRef = varTypeRefArg + "__";  //it should not be a standard Java type.
          } else {
            varTypeRef = varTypeRefArg;
          }
        }
        final String sTypeNsGeneric = sOuterClass == null ? sTypeGeneric : sOuterClass + sTypeGeneric;
        //String sSuperType = classData.sSuperItemType;
        //Map<String, Object> argstxt = new TreeMap<String, Object>();
        if(varTypeRef.contains("Expression"))
          Debugutil.stop();
        if(varTypeRef.contains("ExprPart"))
          Debugutil.stop();
        //                             --------------------vv the variable definition
        OutTextPreparer.DataTextPreparer argsJavaListVarOper = GenJavaOutClass.this.tJavaListVarOper.createArgumentDataObj();
        argsJavaListVarOper.setArgument("typeNs", sOuterClass);
        argsJavaListVarOper.setArgument("typeGeneric", sTypeGeneric);
        argsJavaListVarOper.setArgument("varName", varNameJava);
        argsJavaListVarOper.setArgument("name", varName);
        argsJavaListVarOper.setArgument("type", varTypeRef);
        argsJavaListVarOper.setArgument("typeZbnf", varTypeObj);
        argsJavaListVarOper.setArgument("args", args);
        argsJavaListVarOper.setArgument("dataClass", GenJavaOutClass.this.cmdArgs.sJavaClass);
        argsJavaListVarOper.setArgument("cmpnClass", classData.className); //firstUppercase(cmpn.sDefinitionIdent));
        
        OutTextPreparer.DataTextPreparer argsJavaListVar = GenJavaOutClass.this.tJavaListVar.createArgumentDataObj();
        argsJavaListVar.setArgument("typeNs", sOuterClass);
        argsJavaListVar.setArgument("typeGeneric", sTypeGeneric);
        argsJavaListVar.setArgument("varName", varNameJava);
        argsJavaListVar.setArgument("name", varName);
        argsJavaListVar.setArgument("type", varTypeRef);
        argsJavaListVar.setArgument("typeZbnf", varTypeObj);
        argsJavaListVar.setArgument("docu", sDocu);
        argsJavaListVar.setArgument("args", args);
        argsJavaListVar.setArgument("dataClass", GenJavaOutClass.this.cmdArgs.sJavaClass);
        argsJavaListVar.setArgument("cmpnClass", classData.className); //firstUppercase(cmpn.sDefinitionIdent));
        
        if(this.bWriteAllListNodes)
          Debugutil.stop();
        
        OutTextPreparer.DataTextPreparer argsJavaListVarZbnf = GenJavaOutClass.this.tJavaListVarZbnf.createArgumentDataObj();
        argsJavaListVarZbnf.setArgument("bListAll", this.bWriteAllListNodes);
        argsJavaListVarZbnf.setArgument("typeGeneric", sTypeNsGeneric);
        argsJavaListVarZbnf.setArgument("varName", varNameJava);
        argsJavaListVarZbnf.setArgument("nameOp", sAccess);
        argsJavaListVarZbnf.setArgument("name", varName);
        argsJavaListVarZbnf.setArgument("type", varTypeRef);
        argsJavaListVarZbnf.setArgument("typeNs", sOuterClass);
        argsJavaListVarZbnf.setArgument("typeZbnf", varTypeObj);
        argsJavaListVarZbnf.setArgument("args", args);
        argsJavaListVarZbnf.setArgument("dataClass", GenJavaOutClass.this.cmdArgs.sJavaClass);
        argsJavaListVarZbnf.setArgument("cmpnClass", classData.className); //firstUppercase(cmpn.sDefinitionIdent));
        
        OutTextPreparer.DataTextPreparer argsJavaListCmpnZbnf = GenJavaOutClass.this.tJavaListCmpnZbnf.createArgumentDataObj();
        argsJavaListCmpnZbnf.setArgument("bListAll", this.bWriteAllListNodes);
        argsJavaListCmpnZbnf.setArgument("typeGeneric", sTypeNsGeneric);
        argsJavaListCmpnZbnf.setArgument("superType", classData.sSuperItemType);
        argsJavaListCmpnZbnf.setArgument("cmpnClass", classData.className); //firstUppercase(cmpn.sDefinitionIdent));
        argsJavaListCmpnZbnf.setArgument("varName", varNameJava);
        argsJavaListCmpnZbnf.setArgument("nameOp", sAccess);
        argsJavaListCmpnZbnf.setArgument("name", varName);
        argsJavaListCmpnZbnf.setArgument("type", varTypeRef);
        argsJavaListCmpnZbnf.setArgument("typeNs", sOuterClass);
        argsJavaListCmpnZbnf.setArgument("typeZbnf", varTypeObj);
        argsJavaListCmpnZbnf.setArgument("args", args);
        argsJavaListCmpnZbnf.setArgument("dataClass", GenJavaOutClass.this.cmdArgs.sJavaClass);
        
        OutTextPreparer.DataTextPreparer argsJavaSimpleVarOper = GenJavaOutClass.this.tJavaSimpleVarOper.createArgumentDataObj();
        argsJavaSimpleVarOper.setArgument("typeNs", sOuterClass);
        argsJavaSimpleVarOper.setArgument("typeGeneric", sTypeGeneric);
        argsJavaSimpleVarOper.setArgument("varName", varNameJava);
        argsJavaSimpleVarOper.setArgument("name", varName);
        argsJavaSimpleVarOper.setArgument("type", varTypeRef);
        argsJavaSimpleVarOper.setArgument("typeZbnf", varTypeObj);
        argsJavaSimpleVarOper.setArgument("args", args);
        argsJavaSimpleVarOper.setArgument("dataClass", GenJavaOutClass.this.cmdArgs.sJavaClass);
        argsJavaSimpleVarOper.setArgument("cmpnClass", classData.className); //firstUppercase(cmpn.sDefinitionIdent));
        
        OutTextPreparer.DataTextPreparer argsJavaSimpleVarZbnf = GenJavaOutClass.this.tJavaSimpleVarZbnf.createArgumentDataObj();
        argsJavaSimpleVarZbnf.setArgument("typeGeneric", sTypeNsGeneric);
        argsJavaSimpleVarZbnf.setArgument("cmpnClass", classData.className);
        argsJavaSimpleVarZbnf.setArgument("varName", varNameJava);
        argsJavaSimpleVarZbnf.setArgument("nameOp", sAccess);
        if(varName.contains("ExprPart"))
          Debugutil.stop();
        if(varNameJava.contains("RegularExpression"))
          Debugutil.stop();
        argsJavaSimpleVarZbnf.setArgument("name", varName);
        argsJavaSimpleVarZbnf.setArgument("type", varTypeRef);
        argsJavaSimpleVarZbnf.setArgument("typeNs", sOuterClass);
        argsJavaSimpleVarZbnf.setArgument("typeZbnf", varTypeObj);
        argsJavaSimpleVarZbnf.setArgument("args", args);
        argsJavaSimpleVarZbnf.setArgument("dataClass", GenJavaOutClass.this.cmdArgs.sJavaClass);
        
        OutTextPreparer.DataTextPreparer argsJavaCmpnZbnf = GenJavaOutClass.this.tJavaCmpnZbnf.createArgumentDataObj();
        argsJavaCmpnZbnf.setArgument("typeGeneric", sTypeNsGeneric);
        argsJavaCmpnZbnf.setArgument("superType", classData.sSuperItemType);
        argsJavaCmpnZbnf.setArgument("varName", varNameJava);
        argsJavaCmpnZbnf.setArgument("nameOp", sAccess);
        argsJavaCmpnZbnf.setArgument("name", varName);
        argsJavaCmpnZbnf.setArgument("type", varTypeRef);
        argsJavaCmpnZbnf.setArgument("typeNs", sOuterClass);
        argsJavaCmpnZbnf.setArgument("typeZbnf", varTypeObj);
        argsJavaCmpnZbnf.setArgument("args", args);
        argsJavaCmpnZbnf.setArgument("dataClass", GenJavaOutClass.this.cmdArgs.sJavaClass);
        argsJavaCmpnZbnf.setArgument("cmpnClass", classData.className); //firstUppercase(cmpn.sDefinitionIdent));
        
        OutTextPreparer.DataTextPreparer argsJavaSimpleVar = GenJavaOutClass.this.tJavaSimpleVar.createArgumentDataObj();
        argsJavaSimpleVar.setArgument("typeNs", sOuterClass);
        argsJavaSimpleVar.setArgument("typeGeneric", sTypeGeneric);
        argsJavaSimpleVar.setArgument("varName", varNameJava);
        argsJavaSimpleVar.setArgument("name", varName);
        argsJavaSimpleVar.setArgument("type", varTypeRef);
        argsJavaSimpleVar.setArgument("docu", sDocu);
        argsJavaSimpleVar.setArgument("typeZbnf", varTypeObj);
        argsJavaSimpleVar.setArgument("args", args);
        argsJavaSimpleVar.setArgument("dataClass", GenJavaOutClass.this.cmdArgs.sJavaClass);
        argsJavaSimpleVar.setArgument("cmpnClass", classData.className); //firstUppercase(cmpn.sDefinitionIdent));
        
        //because of debugging write firstly to a StringBuilder:
        Writer_Appendable wrb = new Writer_Appendable(new StringBuilder());
        Writer_Appendable wrzb = new Writer_Appendable(new StringBuilder());
        
        if(bList) {
          GenJavaOutClass.this.tJavaListVar.exec(wrb, argsJavaListVar);
          GenJavaOutClass.this.tJavaListVarOper.exec(this.wrOp, argsJavaListVarOper);
          if(bStdType) {
            GenJavaOutClass.this.tJavaListVarZbnf.exec(wrzb, argsJavaListVarZbnf);
          }
          else if(bCmpn) {
            GenJavaOutClass.this.tJavaListCmpnZbnf.exec(wrzb, argsJavaListCmpnZbnf);
          } 
          else {
            GenJavaOutClass.this.tJavaListVarZbnf.exec(wrzb, argsJavaListVarZbnf);
          }
        } else {
          GenJavaOutClass.this.tJavaSimpleVar.exec(wrb, argsJavaSimpleVar);
          GenJavaOutClass.this.tJavaSimpleVarOper.exec(this.wrOp, argsJavaSimpleVarOper);
          if(bStdType) {
            GenJavaOutClass.this.tJavaSimpleVarZbnf.exec(wrzb, argsJavaSimpleVarZbnf);
          }
          else if(bCmpn) {
            GenJavaOutClass.this.tJavaCmpnZbnf.exec(wrzb, argsJavaCmpnZbnf);
          } 
          else {
            GenJavaOutClass.this.tJavaSimpleVarZbnf.exec(wrzb, argsJavaSimpleVarZbnf);
          }
        }
        GenJavaOutClass.this.wr.append(wrb.getContent()); //now append to output, remove wrb as stack local ref 
        wrb.close();
        GenJavaOutClass.this.wr.flush();
        GenJavaOutClass.this.wrz.append(wrzb.getContent());
        wrzb.close();
        GenJavaOutClass.this.wrz.flush();
      }
    }
    
    
    
    public void writeListAllNodes(String sDocu) throws IOException {
      if(this.bWriteAllListNodes) {
        OutTextPreparer.DataTextPreparer argsJavaListAllVar = GenJavaOutClass.this.tJavaListAllVar.createArgumentDataObj();
        argsJavaListAllVar.setArgument("docu", sDocu);
        GenJavaOutClass.this.tJavaListAllVar.exec(GenJavaOutClass.this.wr, argsJavaListAllVar);
      }
    }

    public void writeOperations() throws IOException {
      GenJavaOutClass.this.wr.append(this.wrOp.getContent());
      this.wrOp.clear();
      GenJavaOutClass.this.wr.flush();
    }
    
  }
 

  
  
}
