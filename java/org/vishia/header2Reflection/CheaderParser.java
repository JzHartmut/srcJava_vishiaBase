package org.vishia.header2Reflection;

import java.io.File;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.vishia.cmd.JZtxtcmdExecuter;
import org.vishia.jztxtcmd.JZtxtcmd;
import org.vishia.mainCmd.MainCmd_ifc;
import org.vishia.util.Debugutil;
import org.vishia.util.FileSystem;
import org.vishia.zbnf.ZbnfJavaOutput;
import org.vishia.zbnf.ZbnfParseResultItem;
import org.vishia.zbnf.ZbnfParser;


/**This class parses C-files and builds a result tree, which can be proceed especially with a JZcmd script.
 * Therewith output files from headers can be generated with a control script without hard-core Java programming.
 * This class should be matched stronly to the parser script <code>Cheader.zbnf</code> in the software component 
 * {@linkplain www.vishia.org/ZBNF/zbnfjax}.
 * <br><br>
 * This class contains some <code>public static class</code> Subclasses which are the complements to the parsers result component.
 * The structure of the syntax of a header file may be able to recognized with them. See the syntax definition.
 * <br><br>
 * The class representing the root of the parse result for each header file is {@link ZbnfResultFile}. This class contains new_... and add_.. methods
 * for the parser's result components of the header file.
 * <br>
 * All headerfile's results are stored in an instance of {@link ZbnfResultData} which is returned by the {@link #execute(Args)} method of this class.
 * <br><br>
 * To invoke this class from a JZcmd script write a script with content:<pre>
##A ZZcmd script:
sub ExampleGen(Obj target: org.vishia.cmd.ZmakeTarget)  ##a zmake target
{ 
  Obj headerTranslator = java new org.vishia.header2Reflection.CheaderParser(console);  ##create instance of this class

  Obj args = java new org.vishia.header2Reflection.CheaderParser$Args();     ##arguments have to be filled firstly
  List inputsExpanded = target.allInputFilesExpanded();
  for(input:inputsExpanded)
  { args.addSrc(input.absfile(), input.namext());                            ##input files, here from zmake target.
  }
  args.setDst(target.output.absfile());                                      ##the output file
  args.setZbnfHeader(<:><&scriptdir>/../../zbnfjax/zbnf/Cheader.zbnf<.>);    ##the Zbnf syntax control script. 
  //
  Obj headers = headerTranslator.execute(args);              ##executes
 * </pre> 
 * The result tree <code>headers</code> is contained in the instance of {@link ZbnfResultData}. That result can be evaluated with JZcmd for example:
 * <pre>
  ##A ZZcmd script:
  for(headerfile: headers.files){
    for(classC: headerfile.listClassC) {
      for(entry: classC.entries) {
        if(entry.whatisit == "structDefinition") {
          call genStruct(struct=entry, headerName = &headerfile.fileName);
        }
      }
    }
  }

 * </pre>
 * 
 * 
 * For Zmake see {@linkplain www.vishia.org/ZBNF/sf/docu/Zmake.html}.
 * <br>
 * This class can be downloaded from {@linkplain www.sf.net/projects/zbnf}
 * @see JZtxtcmd
 * @see JZtxtcmdExecuter
 * @author Hartmut Schorrig
 *
 */
public class CheaderParser {

  /**Version, history and license.
   * <ul>
   * <li>2018-10-10 JzHartmut change; handling of macro because comment is necessary.
   * <li>2018-10-10 JzHartmut {@link StructDefinition#new_attribMacro()}, the HeaderBlock.add_macro is removed. It is more systematic. Cheader.zbnf adequate changed.
   * <li>2018-10-10 JzHartmut Some adaption to jzTc generation script, especially up to now all Type_s produces reflection_Type without suffix _s.
   *   It is necessary because it should be given a simple rule to deduce from the TYPE_t tagname to the type. The rule is:
   *   <pre>struct TYPE_t{....} TYPE_s; class TYPE : TYPE_s{...}; reflection_TYPE </pre>
   *   The reflection_TYPE is for a struct TYPE_s which is presented by a class TYPE without own reflection. 
   * <li>2018-09-02 JzHartmut first super element for non-ObjectJc superclass, see {@link StructDefinition#add_attribute(AttributeOrTypedef)}
   * <li>2018-09-02 JzHartmut <code>structNameTypeOffs</code> removed, see {@link StructDefinition#add_innerStructAttribute(StructDefinition)}
   * <li>2018-08-28 JzHartmut {@link HeaderBlock#set_const()}, {@link HeaderBlock#new_structContentInsideCondition()} should return a {@link ConditionBlock}.
   *   {@link HeaderBlock#add_macro(AttribAsMacro)} and {@link StructDefinition#add_macro(AttribAsMacro)}: The OS_HandlePtr should be accepted as attribute.
   *   Some more container for semantics from CHeader.zbnf
   * <li>2018-08-12 JzHartmut inner named or unnamed struct handled, named struct are stored as extra type..
   * <li>2018-01-03 JzHartmut new Some enhancements adequate to syntax in Cheader.zbnf, while testing reflection.
   * <li>2017-12-20 JzHartmut new {@link StructDefinition} with attributes and isBasedOnObjectJc. Used for Sfunction not based on ObjectJc.
   * <li>2017-05-29 JzHartmut {@link Type#pointer_} with {@link Pointer} designation  
   * <li>2017-05-10 JzHartmut adapt to Cheader.zbnf 
   * <li>2016-10-18 JzHartmut chg: The UnionVariante is syntactically identically with a struct definition. 
   *   Therefore the same class {@link StructDefinition} is used for {@link HeaderBlock#new_undefDefinition()} instead the older extra class UnionVariante.
   *   the {@link StructDefinition#isUnion} designates that it is a union in semantic. Using scripts should be changed.
   *   Some new definitions {@link StructDefinition#set_innerUnion()} and {@link StructDefinition#set_innerStruct()} are added. 
   * <li>2014-10-18 JzHartmut created. 
   * In the past either the parse result was used immediately with a Java programm as generator, for example for {@link CmdHeader2Reflection},
   * or the generation of some things are done via the XML output from the parser via an XSLT translator.
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
  static final public String sVersion = "2018-10-11";

  /**All yet parsed struct or class.
   * The key is the basename on a struct, to detect it for a forward declared type usage.
   */
  static Map<String, StructOrClassDef> allClasses = new TreeMap<String, StructOrClassDef>(); 
  
  
  {
    StructDefinition objectJc = new StructDefinition(null, "structDefinition", false, false);
    objectJc.name = "ObjectJc";
    objectJc.tagname = "ObjectJc_T";
    allClasses.put("ObjectJc", objectJc);
  }
  
  
  public static class SrcFile
  {
    public final String name;
    
    public final String path;

    public SrcFile(CharSequence name, CharSequence path)
    { this.name = name.toString();
      this.path = path.toString();
    }

  }
  
  
  public static class Args
  {
    List<SrcFile> srcFiles = new LinkedList<SrcFile>();
    
    String sFileDst;
    
    String sFileZbnf;
    
    public void addSrc(String path, String name) {
      SrcFile src = new SrcFile(name, path);
      srcFiles.add(src);
    }
    
    
    public void setZbnfHeader(String path) { sFileZbnf = path; }
    
    public void setDst(String path) { sFileDst = path; }
  }
  
  
  
  
  public static class ZbnfResultData
  {
    public List<ZbnfResultFile> files = new LinkedList<ZbnfResultFile>();
    
  }
  

  public static class ZbnfResultFile 
  {
    public final String fileName, filePath;
    
    
    
    public List<IncludeDef> includeDef = new LinkedList<IncludeDef>();
    
    public String headerEntry;
    public String headerEntryDef;
    

    public List<ClassC> listClassC;
    
    
    public ZbnfResultFile(String fileName, String filePath)
    { this.fileName = fileName;
      this.filePath = filePath;
    }
    
    
    public void set_HeaderEntry(String val) {}
    public void set_HeaderEntryDef(String val) {}
   
    public IncludeDef new_includeDef(){ return new IncludeDef(); }
    public void add_includeDef(IncludeDef val){  }

    
    public ClassC new_CLASS_C() {
      ClassC classC = new ClassC();
      return classC;
    }
    
    public void add_CLASS_C(ClassC val){ 
      if(listClassC == null) { listClassC = new LinkedList<ClassC>(); }
      listClassC.add(val);
    }

    public ClassC new_CLASS_CPP() {
      ClassC classC = new ClassC();
      return classC;
    }
    
    public void add_CLASS_CPP(ClassC val){ 
      if(listClassC == null) { listClassC = new LinkedList<ClassC>(); }
      listClassC.add(val);
    }

    public ClassC new_DEFINE_C() {
      ClassC classC = new ClassC();
      return classC;
    }
    
    public void add_DEFINE_C(ClassC val){ 
      if(listClassC == null) { listClassC = new LinkedList<ClassC>(); }
      listClassC.add(val);
    }

    public ClassC new_ARRAY() {
      ClassC classC = new ClassC();
      return classC;
    }
    
    public void add_ARRAY(ClassC val){ 
      if(listClassC == null) { listClassC = new LinkedList<ClassC>(); }
      listClassC.add(val);
    }

    public ClassC new_INLINE() {
      ClassC classC = new ClassC();
      return classC;
    }
    
    public void add_INLINE(ClassC val){ 
      if(listClassC == null) { listClassC = new LinkedList<ClassC>(); }
      listClassC.add(val);
    }

    public ClassC new_outside() {
      ClassC classC = new_CLASS_C();
      classC.name = "--outside--";
      return classC;
    }
    
    public void add_outside(ClassC val){ add_CLASS_C(val); }

  }
  
  
  
  
  public static class IncludeDef
  {
    public String includeGuard;
    public String file;
    public String ext;
    public String path;
    
    public boolean sysInclude;
    
    public Description implementDescription;
    
    //public Description new_implementDescription(){ return new Description("implementDescription"); }
    //public void add_implementDescription(Description val){ val.visibility = true; entries.add(val); }

  }
  
  
  
  
  
  public static class ClassC extends HeaderBlock
  {
    public String name;
    
    public List<IncludeDef> includeDef = new LinkedList<IncludeDef>();

    
    public HeaderBlock new_HeaderBlock(){ return this; }
    public void add_HeaderBlock(HeaderBlock val){}
    
    
    //IncludeDef new_includeDef(){ return new IncludeDef(); }
    
    //void add_includeDef(IncludeDef  src){  }
    
    @Override public String toString(){ return name; }

    
  }
  
  
  
  public static class HeaderBlock extends HeaderBlockEntry
  { 
  
    final HeaderBlock parent;
    
    HeaderBlock(){ super("{}"); parent = null;}
    
    /**Type of the HeaderBlockEntry as String, checked in JZcmd. */
    HeaderBlock(HeaderBlock parent, String whatisit){ super(whatisit); this.parent = parent; }
  
    public List<HeaderBlockEntry> entries = new LinkedList<HeaderBlockEntry>();
  
    /**The last created entry in entries, to set something in. */
    HeaderBlockEntry currEntry;
    
    /**An invalid block is stored here, but overwritten from the next found invalidBlock. It should not be used. */
    HeaderBlock invalidBlock_;
    
    /**From the devision in the header for example /*@DEFINE name... */
    public String headerBlockName;
    
    /**It is the identifier given after:
     * <pre>
     * DEFINE_C::=/*@DEFINE[_C] [&lt;$?@name>]<*|* /?>* /
     * /**&lt;DescriptionDEFINE_C?>* /
     * ....
     * DescriptionDEFINE_C::=&lt;$?@headerBlockNameDescr> : &lt;description>.</pre>
     */
    public String headerBlockNameDescr;
    
    
    /**That is for C++ classDefinition. The visibility is set for any {@link ClassDefinition#new_classVisibilityBlock()}.
     * It is used for any entry. */
    String visibity;

    
    boolean isConst;
    
    public void set_const() { isConst = true; }
    
    
    public String compilerError;
    
    public IncludeDef new_includeDef(){ return new IncludeDef(); }
    public void add_includeDef(IncludeDef val){  }

    
    public Define new_undefDefinition(){ return new Define(); }
    public void add_undefDefinition(Define val){ val.visibility = visibility; entries.add(val); }
    
    public ConditionBlock new_conditionBlock(){ return new ConditionBlock(this); }
    public void add_conditionBlock(ConditionBlock val){ val.visibility = visibility; entries.add(val); }
    
    
    public DefineDefinition new_defineDefinition(){ return new DefineDefinition(); }
    public void add_defineDefinition(DefineDefinition val){ entries.add(val); }
  
    public DefineDefinition new_null_initializer(){ return new DefineDefinition(); }
    public void add_null_initializer(DefineDefinition val){ val.specialDefine = "NULL"; entries.add(val); }
  
    public DefineDefinition new_const_initializer(){ return new DefineDefinition(); }
    public void add_const_initializer(DefineDefinition val){ val.specialDefine = "CONST"; entries.add(val); }
  
    
    public StructClassDecl new_enumDecl(){ return new StructClassDecl("enumDecl"); }
    public void add_enumDecl(StructClassDecl val){ val.visibility = visibility; entries.add(val); }
    
    public StructClassDecl new_structDecl(){ return new StructClassDecl("structDecl"); }
    public void add_structDecl(StructClassDecl val){ val.visibility = visibility; entries.add(val); }
    
    public StructClassDecl new_classDecl(){ return new StructClassDecl("classDecl"); }
    public void add_classDecl(StructClassDecl val){ val.visibility = visibility; entries.add(val); }
    
    public FriendClass new_friendClassDef(){ return new FriendClass(); }
    public void add_friendClassDef(FriendClass val){ val.visibility = visibility; entries.add(val); }
    
    
    public StructDefinition new_structDefinition(){ return new StructDefinition(this, "structDefinition", false, false); }
    public void add_structDefinition(StructDefinition val){ 
      if(val.name !=null && val.name.equals("ARRAYJC"))
        Debugutil.stop();
      val.visibility = visibility; entries.add(val); 
    }
    
    public ClassDefinition new_classDef(){ return new ClassDefinition(this, "classDef"); }
    public void add_classDef(ClassDefinition val){ val.visibility = visibility; entries.add(val); }
    
    public ConditionBlock new_structContentInsideCondition(){ return new ConditionBlock(parent); }
    public void add_structContentInsideCondition(ConditionBlock val){ val.visibility = visibility; entries.add(val); }
    
    public StructDefinition new_unionDefinition(){ return new StructDefinition(this, "unionDefinition", true, false); }
    public void add_unionDefinition(StructDefinition val){ val.visibility = visibility; entries.add(val); }
    
    public AttributeOrTypedef new_typedef(){ return new AttributeOrTypedef("typedef"); }
    public void add_typedef(AttributeOrTypedef val){ val.visibility = visibility; entries.add(val); }
  
  
    public EnumDefinition new_enumDefinition(){ return new EnumDefinition(); }
    public void add_enumDefinition(EnumDefinition val){ val.visibility = visibility; entries.add(val); }
  
    
    public FnPointer new_fnPointer() { return new FnPointer(); }
    public void add_fnPointer(FnPointer val) { entries.add(val); }
    
  
    public AttributeOrTypedef new_attribute(){ return new AttributeOrTypedef(); }
    public void add_attribute(AttributeOrTypedef val){ 
//      if(val.name.equals("waitTxSerial"))
//        Debugutil.stop();
      val.visibility = this.visibility; this.entries.add(val); 
    }
  
    public AttributeOrTypedef new_constDef(){ return new AttributeOrTypedef(); }
    public void add_constDef(AttributeOrTypedef val){ val.visibility = this.visibility; this.entries.add(val); }
  
    
    public MethodDef new_methodDef(){ return new MethodDef(); }
    public void add_methodDef(MethodDef val){ val.visibility = this.visibility; this.entries.add(val); }

    public MethodDef new_virtualMethod(){ return new MethodDef(); }
    public void add_virtualMethod(MethodDef val){ val.visibility = this.visibility; val.virtual_ = true; this.entries.add(val); }

    public MethodDef new_abstractMethod(){ return new MethodDef(); }
    public void add_abstractMethod(MethodDef val){ val.visibility = this.visibility; val.virtual_ = true; this.entries.add(val); }

    public MethodDef new_staticMethod(){ return new MethodDef(); }
    public void add_staticMethod(MethodDef val){ val.visibility = this.visibility; val.static_ = true; this.entries.add(val); }

    public MethodTypedef new_methodTypedef(){ return new MethodTypedef(); }
    public void add_methodTypedef(MethodTypedef val){ val.visibility = this.visibility; this.entries.add(val); }
  
    public MethodTypedef new_methodPtrTypedef(){ return new MethodTypedef(); }
    public void add_methodPtrTypedef(MethodTypedef val){ val.visibility = this.visibility; val.bPointerType = true; this.entries.add(val); }
  
    public MethodDef new_inlineMethod(){ return new MethodDef(); }
    public void add_inlineMethod(MethodDef val){ val.inline = true; val.visibility = this.visibility; this.entries.add(val); }
  
    @Override
    public Description new_implementDescription(){ return new Description("implementDescription"); }
    @Override
    public void add_implementDescription(Description val){ val.visibility = this.visibility; this.entries.add(val); }
  
    public HeaderBlock new_invalidBlock() { return new HeaderBlock(); }
    public void add_invalidBlock(HeaderBlock val) { } //don't add, forget it.
    
    public void set_modifier(String val){}
    
    
    public void set_constDef(String val) {}
    
    
  }
  
  
  
  public static class HeaderBlockEntry
  {
    public String whatisit;
    //public String data;
    
    public String visibility;
    
    public Description description;
    
    public boolean static_;
    
    public boolean virtual_;
    
    public List<Description> implementDescriptions;
  
    HeaderBlockEntry(String whatisit){ this.whatisit = whatisit; }
    
    
    
    
    public Description new_implementDescription(){ return new Description("implementDescription"); }
    public void add_implementDescription(Description val){ 
      if(implementDescriptions == null) { implementDescriptions = new LinkedList<Description>(); }
      implementDescriptions.add(val);
    }



  }
  
  
  
  
  
  public static class SizeofDescription 
  {
    public int sizeof;
    
    public String text;
  }
  
  
  
  
//  public static class SimulinkTag
//  {
//    
//  }
  
  
  public static class Description extends HeaderBlockEntry
  {
    Description(String whatisit){ super(whatisit); }
    public Description(){ super("description"); }
    public String text = "";
    
    /**set in #set_acclevel */
    public int accLevel, chgLevel;
    
    public String simulinkTag = "";
    
    /**For Simulink Sfn generation: Variable for function call. */
    //public String fnCallVar, fnCallMask;
    
    /**True then add the name of the ClassC to fnVallMask. */
    //public boolean fnCallMaskAddClass;

    /** <code>@vtbl <$?vtbl></code> name of the virtual table for reflection generation. */
    public String vtbl;
    
    public String refl;
    
    public SizeofDescription sizeof;
    
    public boolean noReflection;
    
    public ParamDescription returnDescription;
    
    public List<ParamDescription> paramDescriptions;
    
    public List<ParamDescription> auxDescriptions;
    
    public String containerType, containerElementType;
    
    public boolean referencedContainerElement;
    
    public boolean noRefl;
    
    /**It is possible to set a type which is used for the reflection presentation.
     * Especially int32 or void const* for a handle. The type is not used for C, but for the reflection generation.
     */
    public Type reflType;
    
    /**Type of the variable arguments given in Description. */
    public AttributeOrTypedef varg;
    
    public void set_text(String text){ this.text += text; }
    
    /**Add more as one simulinkTag with ":value:", get with simulinkTag.contains(":value:") */
    public void set_simulinkTag(String val) {
      simulinkTag = simulinkTag + ":" + val + ":";
    }
    
    
    public void set_acclevel(String val){ accLevel = val.charAt(0) - '0'; }
    
    public void set_chglevel(String val){ chgLevel = val.charAt(0) - '0'; }
    
    
    
    public AttributeOrTypedef new_varg() { return varg = new AttributeOrTypedef(); }
    
    public void add_varg(AttributeOrTypedef val) {}
    
    
    public final ParamDescription new_paramDescription(){ return new ParamDescription();}
    public final void add_paramDescription(ParamDescription val){ 
      if(paramDescriptions == null) { paramDescriptions = new LinkedList<ParamDescription>(); }
      paramDescriptions.add(val);
    }

    public final ParamDescription new_returnDescription(){ return new ParamDescription();}
    public final void add_returnDescription(ParamDescription val){ 
      val.name = "return";
      returnDescription = val;
    }

    public final ParamDescription new_auxDescription(){ return new ParamDescription();}
    public final void add_auxDescription(ParamDescription val){ 
      if(auxDescriptions == null) { auxDescriptions = new LinkedList<ParamDescription>(); }
      auxDescriptions.add(val);
    }
    
    
    /**Only because |<?auxDescription> ... is written in zbnf file. Not to use.
     * @param val
     */
    public final void set_auxDescription(String val){ } 
   
   
    @Override public String toString(){ return text !=null ? text :  ""; }

   
  }
  
  public static class ParamDescription
  { public String name, text;
  }
  
  

  
  public static class ConditionDef
  {
    public String conditionDef;
    public boolean not;
    
    public Value conditionValue;
    
    public void set_conditionDefNot(String val) { not = true; conditionDef = val; }

    public Value new_condition(){ return new Value(); }
    public void add_condition(Value val){ conditionValue = val; }
  }
  
  
  
  
  public static class ConditionBlock extends HeaderBlock
  {
    ConditionBlock(HeaderBlock parent){ super(parent, "#ifdef"); }
    
    public static class OrCondition extends ConditionDef {
      public String conditionDef;
      public boolean not;
      
      public List<ConditionDef> andConditions;

      public ConditionDef new_AndCondition(){ return new ConditionDef(); }
      public void add_AndCondition(ConditionDef val) { 
        if(andConditions == null) {andConditions = new LinkedList<ConditionDef>(); } 
        andConditions.add(val); 
      }

    }
    
    public String conditionDef;
    
    public boolean not;
    
    public List<OrCondition> orConditions;
    
    /**Ignore &lt;?elseConditionBlock&gt; as String, add it only as component, see {@link #new_elseConditionBlock()}. 
     * The set-Routine should be existing, elsewhere it is an error. But the routine does nothing. */
    public void set_elseConditionBlock(String val) {}

    public void conditionDef(String val) { not = false; conditionDef = val; }
    
    
    public List<ConditionBlock> elifBlocks;
    
    
    public HeaderBlock elseBlock;
    
    
    public void set_conditionDefNot(String val) { not = true; conditionDef = val; }
    
    
    public OrCondition new_OrCondition(){ return new OrCondition(); }
    public void add_OrCondition(OrCondition val) { 
      if(orConditions == null) {orConditions = new LinkedList<OrCondition>(); } 
      orConditions.add(val); 
    }
    
    public ConditionBlock new_elif(){ return new ConditionBlock(parent); }
    public void add_elif(ConditionBlock val){ 
      if(elifBlocks == null) { elifBlocks = new LinkedList<ConditionBlock>(); }
      elifBlocks.add(val); 
    }
    
    
    public HeaderBlock new_elseConditionBlock(){ return new HeaderBlock(); }
    public void add_elseConditionBlock(HeaderBlock val){ elseBlock = val; }
    
    
    
    
    
    
  }
  
  
  
  
  public static class Define extends HeaderBlockEntry
  { Define(){ super("xxdefine"); }
    public String name;
  }


  public static class DefineDefinition extends HeaderBlockEntry
  {
    DefineDefinition(){ super("#define"); }
    public String name;
    
    public String specialDefine;
    
    public Description description;
    
    public String valueDef = "";
    
    public int intvalue, hexvalue;
    
    public String stringvalue;
    
    public int fractPart;
    
    public void set_value(String val){ valueDef += val; }
   
    public List<String> args;
    
    
    public DefineParameter new_parameter(){ return new DefineParameter(); }
    public void add_parameter(DefineParameter val){ 
      if(args == null) { args = new LinkedList<String>(); }
      args.add(val.name);
    }
    
    
    
  }
  
  
  public static class DefineParameter
  { public String name;
  }
  
  


  
  public static class StructClassDecl extends HeaderBlockEntry
  { StructClassDecl(String whatisit){ super(whatisit); } 
    public String name;
  }
  
  
  public static class FriendClass extends HeaderBlockEntry
  { FriendClass(){ super("friend"); } 
    public String name;
  }
  
  
  
  
  public static abstract class StructOrClassDef extends HeaderBlock {
    StructOrClassDef(HeaderBlock parent, String whatisit){ super(parent, whatisit); }
    
    public String name;

    /**If this is set, the struct is a inner one. The {@link #name} is the type name of the struct as well as,  
     * this element is the name of the element with this struct.
     * It is necessary to address the elements in the struct from view of the parent struct.
     * For deeper nested inner struct: This name has the outerparent.parent element name
     */
    public String innerName;

    List<AttributeOrTypedef> attribs = new LinkedList<AttributeOrTypedef>();
    
    /**Set always true if either the struct itself or a base struct has ObjectJc as first element. */
    public String sBasedOnObjectJc;
    

  }  
  
  
  
  public static class StructDefinition extends StructOrClassDef
  { 
    
    static int ctUnifiedImplicitelyStructName = 0;
    
    /**If set the struct is conditionally defined in the header file. */
    public String conditionDef, conditionDefNot;
    
    StructDefinition(HeaderBlock parent, String whatisit, boolean isUnion, boolean isInnerStruct){ 
      super(parent, whatisit);
      this.isUnion = isUnion; 
      this.isInnerStruct = isInnerStruct;
    }
  
    public final boolean isUnion;
    
    public final boolean isInnerStruct;
    
    /**In a struct a superclass is a struct on start of the struct (nested). */
    public AttributeOrTypedef superclass;

    public String tagname;
    
    /**Used in an innerStructAttribute. */
    public Arraysize arraysize;
    
    public void set_name ( String val ) { 
      if(val.equals("Test_T1CtrlClass_s"))
        Debugutil.stop();
      this.name = val;
      CheaderParser.allClasses.put(this.baseName("_s"), this);
      CheaderParser.allClasses.put(this.name, this);  //with full name.
    }
    
    public void set_tagname ( String val ) { 
      if(val.equals("Test_T1CtrlClass_T"))
        Debugutil.stop();
      this.tagname = val; 
    }
    
    
    /**It is a struct definition maybe with tag name but without type name, because it is inner. */
    public StructDefinition new_innerStructAttribute() { return new StructDefinition(this, "unnamedStructAttr", false, true); }

    /**Called from ZBNF: <code> structContent:: ... struct\W &lt;structDefinition?+innerStructAttribute></code>
     * @param val The parsed inner struct
     * @since 2018-09-02: <code>structNameTypeOffs</code> is removed. It is faulty for <code>struct type_t {...} type_s;</code>
     *   because it can't be deduce from the tag name to the struct name. The last one is need for reflection generation.
     *   The Reflection generator Cheader2Refl.jztxt.cmd was improved. The element <code>structNameTypeOffs</code> is no more necessary.  
     */
    public void add_innerStructAttribute(StructDefinition val) { 
      if(val.name == null) {                     //If the struct itself is anonymous, only the member are relevant.  
        for(AttributeOrTypedef mem : val.attribs) {  //add all attributes of the inner union r struct
          attribs.add(mem);
        }
      } else {                                   //An named inner struct. It is rcognized as type.
        //Note: this.name is not set yet because it is set on end of typedef struct{   } <?name> only.
        String structName;                       //Build a type name
        
        if(val.tagname !=null) {                 //If the inner struct has a tag name, it is used as reflection name.
          structName = val.tagname;     
        } else {
          structName = val.name;                 //Reflection name is Instance name_ParentName
          String baseNameParent = this.baseName("_", "_T", "_t", "_s");  //get always from the tagname because name is not given yet.
          if(baseNameParent !=null) {                //this.tagname is the name of the parent (wrapping) struct.
            structName += "_" +  baseNameParent;
          } else {                               //The parent struct has no tagname, what to do:
            structName += "_" +  "_InnerStruct" + (++ctUnifiedImplicitelyStructName) + "_";
          }
        }
        //
        //                                       //The struct itself is a type, creaate an attribute as member of the parent. 
        AttributeOrTypedef attr = new AttributeOrTypedef();
        attr.name = val.name;
        attr.type = new Type();
        attr.type.name = structName;
        attribs.add(attr);
        if(parent !=null) {
          if(this.innerName !=null) {
            val.innerName = this.innerName + "." + val.name; //deeper nested inner struct.
          } else {
            val.innerName = val.name;            //innerName is necessary to address the elements in the superior struct.
          }
          val.name = structName;                 //It is the type name of the StructDefinition, as well as on a not inner struct.
          val.whatisit = "structDefinition";
          parent.entries.add(val);             
        }
      }
      //entries.add(val); 
    }

    public StructDefinition new_innerUnionAttribute() { return new StructDefinition(this, "unnamedUnionAttr", true, true); }

    /**An innerUnionAttribute is a fully parsed union with or without a name.
     * If no attributes are stored before, it may be the definition of a super class in form:
     * <pre>
     * typedef struct MyType_t {
     *   union( MyBaseType super; ObjectJc object; MyInterface ifxy; }; 
     * </pre> 
     * The superclass should be the first member of the union because const initalizing with <code>{ ... }</code> does not work elsewhere,
     * it is from C language. 
     * Because that the first element of the union can be detected as the super class. 
     * <br>
     * Another inner union is an attribute. Maybe named or unnamed.
     *  
     * Checks whether it is the first member of this struct and one member is ObjectJc, then it is the base class.
     * @param val
     */
    public void add_innerUnionAttribute(StructDefinition val) { 
      entries.add(val); 
      boolean bIsSuper = false;
      if(attribs.size()==0 && (val.name == null || val.name.equals("base") || val.name.equals("baseX") || val.name.equals("super"))) { //first one
        this.sBasedOnObjectJc = val.sBasedOnObjectJc == null ? null : val.name + "." + val.sBasedOnObjectJc;  //first attribute basedOnObject, than this too!
        if(val.superclass !=null) {  //use the first element in the union. Maybe "ObjectJc" too.
          superclass = val.superclass;
          superclass.description = val.description;  //may be given or null
          bIsSuper = true;
        }
        //check the attributes for the first union element, whether it contains ObjectJc or a superclass
        for(AttributeOrTypedef mem : val.attribs) {  //attributes of the inner union:
          //Only if ObjectJc is ecplicitely here, this struct based on Object.
          if( mem.type.name.equals("ObjectJc") && mem.type.pointer_==null) {
            this.sBasedOnObjectJc = (val.name == null ? "" : (val.name + ".")) + mem.name;
          } 
          if(superclass == null && mem.type.pointer_==null) {
            superclass = mem;   //The first member is the superclass. It is ObjectJc if it is the first one.
            superclass.description = val.description;  //may be given or null
            bIsSuper = true;
          }
        }
      } 
      if(!bIsSuper) {
        add_innerStructAttribute(val);
      }
    }

     /**Adds the attribute in the struct additional to {@link HeaderBlock#entries}.
     * Invokes super.HeaderBlock{@link #add_attribute(AttributeOrTypedef)}.
     * @since 2018-09: A first attribute named "super" is the superclass. But then the struct does not based on Object. 
     *   Hint: Use an inner <code>union{ Type super; ObjectJc object;};</code> to express it.
     * @see org.vishia.header2Reflection.CheaderParser.HeaderBlock#add_attribute(org.vishia.header2Reflection.CheaderParser.AttributeOrTypedef)
     */
    @Override public void add_attribute(AttributeOrTypedef val){ 
      super.add_attribute(val);
      if(attribs.size() == 0 && superclass == null) {
        //The first element with type Object or with name super is the super class.
        //Note: this is used in a inner union { .... } base. The base unit itself is added with add_innerUnionAttribute
        if((val.type.name.equals("ObjectJc") || val.name.equals("super")) && val.type.pointer_==null) {
          superclass = val;
          if(val.name !=null && val.name.endsWith("X"))
            Debugutil.stop();
          if(val.type.name.equals("ObjectJc")) {  //hint to designate this based on Object use a union definiton in C.
            this.sBasedOnObjectJc = val.name;
          }
          else if(val.type.typeClass !=null) {
            this.sBasedOnObjectJc = val.type.typeClass.sBasedOnObjectJc == null ? null : val.name + "." + val.type.typeClass.sBasedOnObjectJc;
          }
        } else {
          //the first as  normal attribute, the first one. This struct may not based on ObjectJc or has an inner union.
          attribs.add(val);
        }
      } else {
        //a second attribute.
        attribs.add(val);
      }
    }
    
    
    
    public AttribAsMacro new_attribMacro(){ 
      AttribAsMacro macro = new AttribAsMacro();
      entries.add(macro );
      return macro;
    }

    public void add_attribMacro(AttribAsMacro val){
      if(val.macro.equals("OS_HandlePtr") || val.macro.equals("HandlePtr_emC")) {
        val.type.pointer_ = new LinkedList<Pointer>();
        val.type.pointer_.add(new Pointer());
        val.bOS_HandlePointer = true;
        attribs.add(val);
      }
      
    } 
    

    
    /**Returns name without suffix if a suffix is recognized.
     * @param name
     * @param maybesuffix
     * @return
     */
    private static String checkNameSuffix(String name, String ... maybesuffix) {
      for(String suffix: maybesuffix) {
        if(name.endsWith(suffix)) { 
          return name.substring(0, name.length() - suffix.length()); 
      } }
      return name;
    }
    
    
    
    public String baseName ( String ... maybesuffix) {
      String tagnameBase = this.tagname ==null ? null : checkNameSuffix(this.tagname, maybesuffix);
      if(this.name !=null) {
        if(tagnameBase !=null && this.name.equals(tagnameBase)) { return this.name; }  //struct MyName_s_T { ...} MyName_s
        else {
          return checkNameSuffix(this.name, maybesuffix);  //without suffix
        }
      }
      else {
        return tagnameBase;  //name not given. mayne null if tagname not given. 
      }
    } 
    
    
    
    /**Returns a name of the struct. It is the tagname for a type definition <code>struct tagname { ...}; </code>*/
    public String name() { return name == null ? tagname : name; }
    
    

    
    
        
    @Override public String toString(){ return name ==null ? tagname : name; }

  
  }


  /**Form parsing it is only an entry with given name. 
   * To access properties the underlying struct is necessary. 
   * */
  public static class Superclass {
  
    public String name;
    
    public Superclass new_visibility() { return this; }
    public void set_visibility(Superclass val) {}
    
    public String accessRight;
    
    public boolean isVirtual;
    
    //public StructOrClassDef typeClass;
    /**It is adequate an attribute, same structure. */
    public Type type;
    
    public void set_name(String val) {
      this.name = val;
      StructOrClassDef typeClass = CheaderParser.allClasses.get(val);
      if(typeClass == null && val.endsWith("_s")) {
        typeClass = CheaderParser.allClasses.get(val.substring(0, val.length()-2));  //basename
      }
      Type supertype = new Type();
      supertype.typeClass = typeClass;
      this.type = supertype;
    }

  }




  public static class InitializationInCtor
  {
    public String ident;
    
    public Value value;
  
  }



  public static class Constructor extends MethodDef
  {
    private Constructor(){ super("Constructor"); }
  
    public List<InitializationInCtor> init= new LinkedList<InitializationInCtor>();
  
    public String initialization;
    public InitializationInCtor new_initialization() { return new InitializationInCtor(); }
    public void add_initialization(InitializationInCtor val) { init.add(val); }
  
  }


  public static class Destructor extends MethodDef
  {
    private Destructor(){ super("Destructor"); }
    
    public String className;
    
    public boolean abstract_, virtual;
    
    public void set_abstract() { abstract_ = true; }
  
  }


  public static class Operator extends MethodDef
  {
    private Operator(){ super("Operator"); }
    
    public String operator;
    
    public Operator new_operator() { return this; }
    public void add_operator(Operator val) {}
    
    public String assignOperator;
    
    public boolean parenthesis;
    
    public Type typeConversion;
    
    public String unaryOperator;
    public String binaryOperator;
  
  }



  public static class ClassDefinition extends StructOrClassDef
  { 
    ClassDefinition(HeaderBlock parent, String whatisit){ super(parent, whatisit); }
  
    //public List<MethodDef> constructors = new LinkedList<MethodDef>();
    
    public Superclass superclass;

    public void set_name ( String val ) { 
      this.name = val;
      Type type = new Type();
      type.name = type.basename = val;
      if(CheaderParser.allClasses.get(this.name) ==null) {
        //do not replace a struct with the adequate NAME_s. The basename is stored. 
        CheaderParser.allClasses.put(this.name, this);
      }
    }

    public Superclass new_superclass ( ) { return new Superclass(); }
    
    public void add_superclass ( Superclass val ) {
      this.superclass = val;
      if(val.type.typeClass !=null) {
        this.sBasedOnObjectJc = val.type.typeClass.sBasedOnObjectJc;
      }
    }
    
    
    /**Adds the attribute in the struct additional to {@link HeaderBlock#entries}.
     * Invokes super.HeaderBlock{@link #add_attribute(AttributeOrTypedef)}.
     * @since 2018-09: A first attribute named "super" is the superclass. But then the struct does not based on Object. 
     *   Hint: Use an inner <code>union{ Type super; ObjectJc object;};</code> to express it.
     * @see org.vishia.header2Reflection.CheaderParser.HeaderBlock#add_attribute(org.vishia.header2Reflection.CheaderParser.AttributeOrTypedef)
     */
    @Override public void add_attribute(AttributeOrTypedef val){ 
      super.add_attribute(val);
      attribs.add(val);
    }

    
    
    public ClassDefinition new_classVisibilityBlock() { return this; }
    public void add_classVisibilityBlock(ClassDefinition value) {} 
  
    public Constructor new_Constructor(){ return new Constructor(); }
    public void add_Constructor(Constructor val){ val.visibility = visibility; entries.add(val); }
  
    public Destructor new_Destructor(){ return new Destructor(); }
    public void add_Destructor(Destructor val){ val.visibility = visibility; entries.add(val); }
  
    public Operator new_operator(){ return new Operator(); }
    public void add_operator(Operator val){ val.visibility = visibility; entries.add(val); }
  
    public Operator new_virtualOperator(){ return new Operator(); }
    public void add_virtualOperator(Operator val){ val.visibility = visibility; entries.add(val); }
  

    /**Returns true on a construct: <pre>
     * class MyClass : MyClass_s {... </pre>
     * In this case the class should only be used as a wrapper around struct data without own reflection.
     * @return
     */
    public boolean isClassOfStruct ( ) {
      return superclass !=null && superclass.name.endsWith("_s") 
          && this.name.equals(superclass.name.substring(0, superclass.name.length()-2));
    }
    
    /**Returns a name of the class. */
    public String name() { return name; }
    
    /**Should be offered, as for struct, but no content. Returns always the name.
     * @param maybesuffix ignored.
     * @return The name of the class
     */
    public String baseName(String ... maybesuffix) {
      return name;
    } 

  }

  
  
  public static class EnumDefinition extends HeaderBlockEntry
  {
    EnumDefinition() { super("enumDef"); }
    
    public String tagname, name;
    public Map<String, EnumElement> values = new TreeMap<String, EnumElement>();
    
    /**Defines inside an enum definition block are defines for the enum.*/
    public Map<String, DefineDefinition> defines;
    
    public EnumElement new_enumElement(){ return new EnumElement(); }
    public void add_enumElement(EnumElement val){ values.put(val.name, val); }
    
    
    public DefineDefinition new_defineDefinition(){ return new DefineDefinition(); }
    public void add_defineDefinition(DefineDefinition val){ 
      if(defines == null) { defines = new TreeMap<String, DefineDefinition>(); }
      defines.put(val.name, val);
      
    } //entries.add(val); }
  

  }
  
  public static class EnumElement extends HeaderBlockEntry
  {
    EnumElement() { super("enumElement"); }
    
    public String name;
    
    public long intnumber, hexnumber;
    
    public String symbol;
  }
  
  
  
  
  
  
  /**An attribute in a struct (variable definition) or in an argument list.
   * Note: the parser creates via syntax more as one instance for <pre>
   *  Type name1, name2; </pre>
   * The Type information is duplicated in the parser's result already. 
   */
  public static class AttributeOrTypedef extends HeaderBlockEntry
  { 
      
    /**Use on component conditionalArgument.
     * @return this not an extra container.
     */
    public AttributeOrTypedef new_typedParameter(){ return this; }
    
    public void set_typedParameter(AttributeOrTypedef data){ }
    
    public Description description;
  
    public Type type;
    
    public String name;
    
    public String text;
    
    public int bitField;
    
    /**Not parsed but possible for postprocessing. Any index in a list. */
    public int ix;
    
    /**Not parsed but possible for postprocessing. Any index in a list. */
    public long addr;
    
    /**Not parsed but possible for postprocessing. Any index in a list. */
    public String sTstep;
    
    /**Not parsed but possible for postprocessing. Any index in a list. */
    public boolean bOutput;
    
    /**Not parsed but possible for postprocessing. Any index in a list. */
    public boolean bParam;
    
    /**Not parsed but possible for postprocessing. Any index in a list. */
    public int ixInList;
    
    public boolean staticConst;
    
    public boolean bOS_HandlePointer;
    
    /**Used only on conditionArgument. */
    public String conditionDef;
    
    public Arraysize arraysize;
  
    public Value defaultValue; 
    
    /**It is only for comptatibility for symbolic access from JZtxtcmd, as in {@link CheaderParser.Superclass} */
    //public final AttributeOrTypedef typeClass;
    
    /**Any index value maybe used in an evaluating routine via JZtxtcmd. It is not used by the parser, remain 0. 
     * For Simulink Sfunction -wrapper-generator: It is used for the ixParam to access <code>ssGetSFcnParam(simstruct, ixArg)</code>*/
    public int ixArg;
  
    public AttributeOrTypedef(String whatisit){ super(whatisit); } //typeClass = this; }
    
    AttributeOrTypedef(){ super("Attribute"); } //typeClass = this; }
  
    public Description new_description() { return description = new Description(); }
    public void add_description(Description val){ /*already added.*/ } 
  
    /**This routine is used especially for {@link Description#varg} to store a single attribute where this class is used in syntax.
     * Inside the syntax <code>{&lt;?+attribute&gt; ...  }</code> is contained. That forces the storing of the attribute inside its container.
     * If this class is used as container for an single attribute, this operation is used. 
     * @return
     */
    public AttributeOrTypedef new_attribute() { return this; }
    public void set_attribute(AttributeOrTypedef val) {}
    
    
    public String baseName ( String ... maybesuffix) { return name; }
    
    public Type type ( ) { return type; }
    
    @Override public String toString(){ return name + ": " + type; }
  
  
  }
  
  
  /**An attribute in a struct (variable definition).
   * Note: the parser creates via syntax more as one instance for <pre>
   *  Type name1, name2; </pre>
   * The Type information is duplicated in the parser's result already. 
   */
  public static class AttribAsMacro extends AttributeOrTypedef
  { AttribAsMacro(String macro){ super(); this.macro = macro; }
    AttribAsMacro(){ super();  }
  
    public String macro; 
    
    
  }  
  
  
  
  
  
  /**Designation whether a type is a pointer type.
   * Used inside {@link Type}
   */
  public static class Pointer
  {
    public boolean constPointer;
    
    public boolean volatilePointer;
    
    public boolean cppRef;
  
  
  }
  
  
  
  
  
  
  /**This class presents the properties of a type.
   * ZBNF: <pre>
type::= [<?@modifier>volatile|const|] 
  [<?@forward>struct\W|class\W|union\W|] 
  [ [unsigned<?unsigend>|signed<?signed>]  ##signed and unsigned notification      
    [ int<!\\s?> <?@name=int32>
    | short<!\\s?> int <?@name=int16>
    | short<!\\s?> <?@name=int16>
    | long long<!\\s?> <?@name=int64>
    | long int<!\\s?> <?@name=int32>
    | long<!\\s?>     <?@name=int32>
    | char<!\\s?>     <?@name=int8>
    | <?@name=int>                        ##unsigned or signed allone
    ]
  | [{ <$?environmentClass> ::}] 
    [ int<!\\s?> <?@name=int32>           ##notification without first signed or unsigend
    | short int<?@name=int16>             ##special int types assigned to int16 etc.
    | short<!\\s?> <?@name=int16> 
    | long long<!\\s?><?@name=int64> 
    | long int<!\\s?><?@name=int32> 
    | long<!\\s?><?@name=int32> 
    | char<!\\s?>     <?@name=int8>
    | long double<?@long_double>
    | <$?@name>                           ##any other type name, also float, double, or a user type 
    ##  [ ( { <* |,|)?macro_arg> ? , } ) ]         ##MACRO(arg) also admissible.
    ] 
    [ \< <type?templateType> \> ]        ##<templatetype> 
  ]
  [<typeRefModifier?>]                   ##such as *, const *, volatile *
  [<?@modifier2>volatile|const|].        ##last one is const or volatile for the variable.
   * </pre>
   */
  public static class Type
  {
    /**It is the name without forward suffix (_, _T, _t or _* admissible, * = any char)
     * or the name witout "_s" for struct type definitions. 
     */
    public String basename;
    
    
    /**If {@link #forward} is not null, it is the struct type name. */
    public String name;
    
    /**Will be set if a type is given with a macro <code>MACRO(argument)</code>.
     * This is usefully for special cases only.
     */
    //public String macro;
    
    public String forward;
    
    public boolean constVar, volatileVar, signed, unsigned;
    
    @Deprecated public boolean pointer;
    
    @Deprecated public boolean pointer2;
    
    @Deprecated public String modifier;
    
    //public List<String> macroArg;
    
    /**Modifier const* and const**. */
    @Deprecated public boolean constPointer, constPointer2; 
    
    /**It is possible that a deeper pointer level is given. Any pointer type can be volatile or const. */
    List<Pointer> pointer_;
    
    
    /**Reference to the before parsed type found by name. 
     * It presumes that the header are parsed in the correct order. 
     * Elsewhere this is null.
     */
    StructOrClassDef typeClass;

    public void set_name(String val) { 
      name = val; 
      int len = val.length();
      if(forward !=null) {
        if(val.endsWith("_")) { len -=1;}  //for struct NAME_*
        else { len -=2; } //for struct NAME_t* or struct NAME_T*
      } else if(val.endsWith("_s")) {
        len -=2;
      }; 
      basename = val.substring(0,len); 
      typeClass = CheaderParser.allClasses.get(basename); //remain null if type is unknown.
    }

    public void set_Pointer() { set_pointer(); }

    
    public void set_pointer()
    {
      if(pointer_ == null) { 
        pointer_ = new LinkedList<Pointer>(); 
        if(this.constVar) { this.constPointer = true; } else { this.pointer = true; } 
      } else { 
        if(this.constVar) { this.constPointer = false; this.constPointer2 = true; } else { this.pointer = false; this.pointer2 = true; } 
      }
      Pointer pointerEntry = new Pointer();
      //a const or volatile designation before a reference designator (*) is valid for the reference!
      if(constVar) { pointerEntry.constPointer = true; constVar = false; }
      if(volatileVar) { pointerEntry.volatilePointer = true; volatileVar = false; }
      pointer_.add(pointerEntry);
    
    }
    
    public void set_cppRef()
    {
      if(pointer_ == null) { 
        pointer_ = new LinkedList<Pointer>(); 
      }
      Pointer pointerEntry = new Pointer();
      pointerEntry.cppRef = true;
      //a const or volatile designation before a reference designator (*) is valid for the reference!
      if(constVar) { pointerEntry.constPointer = true; constVar = false; }
      if(volatileVar) { pointerEntry.volatilePointer = true; volatileVar = false; }
      pointer_.add(pointerEntry);
    
    }
    
    
    public String typeString() { 
      String ret;
      if(forward != null) { ret = forward + " " + name; }
      else { ret = name; }
      if(pointer_!=null) {
        for(Pointer entry : pointer_) {
          if(entry.constPointer) { ret += " const"; }
          if(entry.volatilePointer) { ret += " volatile"; }
          ret += "*";
        }
      }
      return ret; 
    }
    
    /**Returns the base name of the type. This is usually the name of the reflection.
     * @param maybesuffix An expected suffix which is not part of the base name
     * @param maybeForwardSuffix An expected suffix for forward declaration
     * @return
     */
    public String baseName ( String maybesuffix, String maybeForwardSuffix) {
      if(forward !=null && name.endsWith(maybeForwardSuffix)) { return name.substring(0, name.length()- maybeForwardSuffix.length()); }
      else if(name.endsWith(maybesuffix)) { return name.substring(0, name.length()-maybesuffix.length()); }
      else return name;
    } 
    
    
    /**depth of pointer. 0: no pointer.
     */
    public int pointerDepth() { return pointer_ !=null ? pointer_.size() : 0; }
    
    public boolean constPointer() { return pointer_ ==null ? false : pointer_.get(0).constPointer; }
    
    public StructOrClassDef typeClass ( ) { return typeClass; }
    
    @Override public String toString(){ return name; }
  }
  
  
  
  
  public static class Arraysize
  {
    /**Simple constant integer value, typically for immediate arrays in a struct. 
     * If -1 is stored here, the array is given with unknown size with [] */
    public int value;
    
    /**A symbol value, defined per #define, typically for C but difficult to evaluate. */
    public String symbolValue;
    
    /**A value as expression. Difficult to evaluate. Needs an expression interpreter. Needs knowledge of defines because variables are defines */
    public Value exprValue;
    
    /**Array with [] as pointer or unknown determined by initializer. */
    public void set_unknown(){ value = -1; }
    
    public Arraysize new_arraysize() { return this; }
    public void add_arraysize(Arraysize val) {}
  }
  
  
  
  
  public static class MethodDef extends HeaderBlockEntry
  { MethodDef(){ super("method"); }
    MethodDef(String whatisit){ super(whatisit); }

    public Type type;
    
    public String name;
    
    public boolean inline;
    
    public boolean abstract_;
    
    public List<AttributeOrTypedef> args;
    
    public Map<String, AttributeOrTypedef> name_args;
    
    public boolean variableArgs;
    
    /**If true then body==null; */
    public boolean declaration;
    
    /**If empty then this is a declaration. */
    StatementBlock body;
    
    public String modifier;
     
    
    public AttributeOrTypedef new_conditionalArgument(){ return new AttributeOrTypedef(); }
    public void add_conditionalArgument(AttributeOrTypedef val){ 
      if(args == null) { args = new LinkedList<AttributeOrTypedef>(); name_args = new TreeMap<String, AttributeOrTypedef>(); }
      args.add(val);
      if(val.name !=null) { name_args.put(val.name, val); }
    }

    public AttributeOrTypedef new_typedParameter(){ return new AttributeOrTypedef(); }
    public void add_typedParameter(AttributeOrTypedef val){ 
      if(args == null) { args = new LinkedList<AttributeOrTypedef>(); name_args = new TreeMap<String, AttributeOrTypedef>(); }
      args.add(val);
      if(val.name !=null) { name_args.put(val.name, val); }
    }

    
    public StatementBlock new_statementBlock(){ return body = new StatementBlock(); }
    public void add_statementBlock(StatementBlock val){ }
    
    public void set_abstract() { abstract_=true; }
    
  }
  


  public static class MethodTypedef extends MethodDef
  { 
    boolean bPointerType;
    String class_;
    MethodTypedef(){ super("typedef_method"); }

    public void set_class(String name) { class_ = name; }
  
  }
  
  
  /**A C like funktion pointer definition. 
   * Written like type (*name)(args);
   * It contains the same elements like {@link MethodDef}
   */
  public static class FnPointer extends MethodDef
  { FnPointer(){ super("def_fnPointer"); }
  }
  
  
  /**A StatementBlock contains statements. It contains a <pre> 
   * List<Statement> statements; </pre>
   * in the order of the source.
   * A StatementBlock is a {@link Statement} itself because a StatementBlock can contain an inner Block. 
   *
   */
  public static class StatementBlock extends Statement
  {
    List<Statement> statements = new LinkedList<Statement>();
    
    
    String conditionDef;
    
    public String sTACKTRC_ENTRY;
    
    public StatementBlock(){ super('{'); }
    
    /**A statement is a syntax component which contains any of the statements. This syntax component is not necessary for extra storing.
     * Therefore returns this. 
     * @return this
     */
    public StatementBlock new_statement(){ return this; }
    public void add_statement(StatementBlock val){ }
    
    public StatementBlock new_statementBlock(){ return new StatementBlock(); }
    public void add_statementBlock(StatementBlock val){ statements.add(val); }
    
    public MethodCall new_simpleMethodCall(){ return new MethodCall(); }
    public void add_simpleMethodCall(MethodCall val){ statements.add(val); }
    
    public Assignment new_assignment(){ return new Assignment(); }
    public void add_assignment(Assignment val){ statements.add(val); }
    
    public VariableDefinition new_variabledefinition(){ return new VariableDefinition(); }
    public void add_variabledefinition(VariableDefinition val){ statements.add(val); }
    
    
    public ConditionalStatement new_if_statement(){ return new ConditionalStatement('i'); }
    public void add_if_statement(ConditionalStatement val){ statements.add(val); }
    
    
    public Value new_returnAssignment(){ 
      Assignment ret = new Assignment();
      ret.assignOperator = "return";
      statements.add(ret);
      return ret.value = new Value();
    }
    public void add_returnAssignment(Value val){}
    
    public void set_conditionDef(String arg) { conditionDef = arg;}

  }
  
  
  
  public static class Statement extends ValueEntry
  { public Statement(char whatisit)
    { super(whatisit);
    }
    
    
  }
  
  
  
  public static class Assignment extends Statement
  { public Assignment(){ super('='); }
    
    public Variable variable;
    
    public int nrofRefLevels_variable = 0;
    
    public String assignOperator;
    
    public Value value;
    
    public Variable new_Refvariable(){ nrofRefLevels_variable = 1; return variable = new Variable(); }
    
    public void add_Refvariable(Variable src){}
  }
  
  
  
  
  
  public static class VariableDefinition extends Statement
  { public VariableDefinition(){ super('d'); }
    
    public Description description;
    
    public Type type;
    
    public String name;
    
    public int bitField;
    
    public Arraysize arraysize;
  
    public Value defaultValue;
    
    List<VariableDefinition> moreVariable;
    
    public VariableDefinition new_name() { 
      if(name == null) { return this; }
      else {
        if(moreVariable == null) { moreVariable = new LinkedList<VariableDefinition>(); }
        VariableDefinition ret = new VariableDefinition();
        ret.type = type; //same type
        moreVariable.add(ret);
        return ret;
      }
    }
    
    /**Add modifier to the existing type. */
    public Type new_typeRefModifier() { return type; }
    public void add_typeRefModifier(Type val) { }
    
    
    
    @Override public String toString(){ return name + ": " + type; }
  }
  
  
  public static class ConditionalStatement extends Statement
  {
    ConditionalStatement(char whatisit){ super(whatisit); }
    
    public Value condition;
    
    public StatementBlock statementBlock;
    
    public StatementBlock new_statement(){ return statementBlock = new StatementBlock(); }
    public void set_statement(StatementBlock val){  }
    
    
    
  }
  
  
  
  public static class MethodCall extends Statement
  { public MethodCall(){ super('m'); }
  
    /**The Object which's method is called. */
    public ExternObject externObject;

    public String methodname;
    
    List<Value> args; 
    
    public Value new_actualParameter(){ return new Value(); }
    public void add_actualParameter(Value val){ 
      if(args == null){ args = new LinkedList<Value>(); }
      args.add(val);
    }
    
    public MethodCall new_simpleMethodCall(){ return this; }
    public void add_simpleMethodCall(MethodCall val){ }
    
  }
  
  
  
  /**A <code>value</code> is syntactically an expression. It is designated as <code>value</code> in the Cheader.zbnf syntax definition
   * in opposite to usual naming because it is semantically a <code>value</code>. The ZBNF forces the semantic aspect of parsing. 
   */
  public static class Value extends ValueEntry
  {
    Value(char whatisit){ super(whatisit); }
    public Value(){ super('v'); }
    
    public List<ValueEntry> entries = new LinkedList<ValueEntry>(); 
    
    /**The current entry used for some post-information. */
    ValueEntry entry1;
    
    /**That is parsed before the next entry, use and remove it on entry. */
    public String unaryOperator;
    
    public String simpleStringLiteral;
    
    /**Expressions for true and false in an construct <code>condition ? value_true : value_false</code>
     * the condition is this value itself with its entries.
     */
    public Value value_true, value_false;
    
    public String conditionDef;
    
    public void set_variable(java.lang.String val){}
    
    public Variable new_variable(){ return new Variable(); }
    public void add_variable(Variable val){ entries.add(val); }
    
    public Value new_value(){ Value entry = new Value(); entry.unaryOperator = this.unaryOperator; this.unaryOperator = null; return entry; }
    public void add_value(Value val){ entries.add(val); }
    
    
    public Value new_referenceAddress(){ return new Value(); }
    public void add_referenceAddress(Value val){ val.whatisit = '&'; entries.add(val); }
    
    
    public Assignment new_assignment(){ Assignment entry = new Assignment(); entry.unaryOperator = this.unaryOperator; this.unaryOperator = null; return entry; }
    public void add_assignment(Assignment val){ entries.add(val); }
    
    
    public RefCastingValue new_refCastingValue(){ return new RefCastingValue(); }
    public void add_refCastingValue(RefCastingValue val){ entries.add(val); }
 
    //public void set_floatNumber(String val){ entry1 = new Number('f'); entry1.data = val; entries.add(entry1);  }
    
    /**Adds a string representing the parsed char.
     * @param chars
     */
    public void set_simpleCharLiteral(String chars) {
      entry1 = new ValueEntry('c', chars);
      entries.add(entry1);
    }
    
    
    public Number set_number(int val){ 
      Number number = new Number('f'); 
      entry1 = number;
      entries.add(entry1);
      return number; 
    }
    
    public Number set_floatNumber(int val){ 
      Number number = new Number('f'); 
      entry1 = number;
      entries.add(entry1);
      return number; 
    }
    
    public void set_floatModifier(String val) {
      ((Number)entry1).floatModifier = val;
    }
    
    public void set_longModifier(String val) {
      ((Number)entry1).floatModifier = val;
    }
    
    
    //public void add_floatNumber(Number val){ entries.add(val);}
    
    public Number new_number(){ Number number = new Number('f'); entry1 = number; return number; }
    public void add_number(Number val){ entries.add(val);}
    
    public MethodCall new_methodCall(){ return new MethodCall(); }
    public void add_methodCall(MethodCall val){ entries.add(val); }
    
    public void set_conditionDef(String arg) { conditionDef = arg;}
    
    
    public void set_binaryOperator(String val){ entries.add(new ValueEntry('b', val)); }
    
  }
  
  
  public static class ValueEntry
  {
    
    
    /**
     * <ul>
     * <li>& reference value
     * </ul>
     */
    public char whatisit;
    
    public String data;

    
    public String unaryOperator;
    
    public ValueEntry(char whatisit, String data)
    { this.whatisit = whatisit;
      this.data = data;
    }
    
    public ValueEntry(char whatisit)
    { this.whatisit = whatisit;
    }
  
    
  }
  
  
  
  
  public static class Number extends ValueEntry
  {
    Number(char whatisit){ super(whatisit); }
    public Number(){ super('n'); }
    
    public double number;
    
    public String floatModifier;
    public String longModifier;
    
  }
  
  
  
  
  public static class ExternObject
  {
    public List<Value> entries;
    
    public Value index;
    
    public void set_association(String val) { 
      Value value = new Value(); 
      value.data = val; 
      addEntry(value); 
    }
    
    private void addEntry(Value val){
      if(entries == null) { entries = new LinkedList<Value>(); }
      entries.add(val);
    }
    
    
  }
  
  
  
  
  public static class Variable extends ValueEntry
  { public Variable(){ super('V'); }
    public boolean preDecrement, preIncrement, postDecrement, postIncrement;
    public String simpleVariable;
    
    /**The Object which's variable is accessed. */
    public ExternObject externObject;
    
    public Value index;
  }
  
  
  
  public static class RefCastingValue extends Value
  { RefCastingValue(){ super('c'); }
  
    public Type type;
  }
  
  
  
  
  
  
  
  private final MainCmd_ifc console;
  
  
  public CheaderParser(MainCmd_ifc console) {
    this.console = console;
  }
  
  
  
  public ZbnfResultData execute(Args args) {
    ZbnfResultData res = parseAndStoreInput(args);
    return res;
  }
  
  
  
  /**Prepares a name for a variable, may be longer than 30 chars or may contain "." for sub structure.
   * @param name
   * @return
   */
  public static String prepareReflName(String name) {
    String ret = name.replace('.', '_');
    int zName = name.length();
    if(zName > 30) {
      name = name.substring(0, 15) + "_" + name.substring(zName - 14, zName);
    }
    return ret;
  }
  
  
  
  /**This method reads the input script, parses it with ZBNF, 
   * stores all results in the Java-class {@link ZbnfResultData} 
   */
  private ZbnfResultData parseAndStoreInput(Args args)
  { ZbnfResultData zbnfResultData = new ZbnfResultData();
    ZbnfParser  parser = new ZbnfParser(console);
    File fileSyntax = new File(args.sFileZbnf);
    if(!fileSyntax.exists()) throw new IllegalArgumentException("CheaderParser - syntax file not found; " + fileSyntax.getAbsolutePath());
    try{ parser.setSyntax(FileSystem.readFile(fileSyntax)); }
    catch(ParseException exc)
    { String sError = "CheaderParser - ERROR in syntax prescript; " + exc.getMessage();
      throw new IllegalArgumentException(sError);
    }
      /**The instance to store the data of parsing result is created locally and private visible: */
    /**This call processes the whole parsing and storing action: */
    for(SrcFile src: args.srcFiles) {
      System.out.println(src.name);
      File fileIn = new File(src.path);
      boolean bOk = false;
      try { bOk = parser.parseFile(fileIn); } 
      catch(Exception exc){ throw new IllegalArgumentException("CheaderParser - file ERROR; " + fileIn.getAbsolutePath() + ":" + exc.getMessage() ); }
      ZbnfParseResultItem resultItem = parser.getFirstParseResult();
      if(!bOk) {
        String sError = parser.getSyntaxErrorReport();
        System.err.println("ERROR Parsing file: " + fileIn.getAbsolutePath() + "\n" + sError);
      }
      else {
        ZbnfResultFile resultFile = new ZbnfResultFile(src.name, src.path);   //Container for the parsed file.
        try{ ZbnfJavaOutput.setOutput(resultFile, resultItem, console); }
        catch(Exception exc) {
          throw new IllegalStateException("CheaderParser - internal ERROR storing parse result; " + exc.getMessage());
        }
        zbnfResultData.files.add(resultFile);
      }    
      
    }
    return zbnfResultData;
  }
  

  
  
  
}
