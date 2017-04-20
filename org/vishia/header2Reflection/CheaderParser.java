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
   * <li>2016-10-18 JzHartmut chg: The UnionVariante is syntactically identically with a struct definition. 
   *   Therefore the same class {@link StructDefinition} is used for {@link HeaderBlock#new_undefDefinition()} instead the older extra class UnionVariante.
   *   the {@link StructDefinition#isUnion} designates that it is a union in semantic. Using scripts should be changed.
   *   Some new definitions {@link StructDefinition#set_implicitUnion()} and {@link StructDefinition#set_implicitStruct()} are added. 
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
  static final public String sVersion = "2014-10-25";

  
  
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
    
    
    public void XXset_HeaderEntry(String val) {}
    public void XXset_HeaderEntryDef(String val) {}
   
    public IncludeDef XXnew_includeDef(){ return new IncludeDef(); }
    public void XXadd_includeDef(IncludeDef val){  }

    
    public ClassC new_CLASS_C() {
      ClassC classC = new ClassC();
      return classC;
    }
    
    public void add_CLASS_C(ClassC val){ 
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
    public String file;
    public String ext;
    public String path;
    public boolean sysInclude;
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
  
    HeaderBlock(){ super("{}"); }
    
    /**Type of the HeaderBlockEntry as String, checked in JZcmd. */
    HeaderBlock(String whatisit){ super(whatisit); }
  
    List<HeaderBlockEntry> entries = new LinkedList<HeaderBlockEntry>();
  
    
    
    
    
    public Define new_undefDefinition(){ return new Define(); }
    public void add_undefDefinition(Define val){ entries.add(val); }
    
    public ConditionBlock new_conditionBlock(){ return new ConditionBlock(); }
    public void add_conditionBlock(ConditionBlock val){ entries.add(val); }
    
    
    public DefineDefinition new_defineDefinition(){ return new DefineDefinition(); }
    public void add_defineDefinition(DefineDefinition val){ entries.add(val); }
  
    
    public StructDecl new_structDecl(){ return new StructDecl(); }
    public void add_structDecl(StructDecl val){ entries.add(val); }
    
    
    public StructDefinition new_structDefinition(){ return new StructDefinition("structDefinition", false); }
    public void add_structDefinition(StructDefinition val){ entries.add(val); }
    
    public StructDefinition new_structContentInsideCondition(){ return new StructDefinition("structDefinition", false); }
    public void add_structContentInsideCondition(StructDefinition val){ entries.add(val); }
    
    public StructDefinition new_unionDefinition(){ return new StructDefinition("unionDefinition", true); }
    public void add_unionDefinition(StructDefinition val){ entries.add(val); }
    
    public AttributeOrTypedef new_typedef(){ return new AttributeOrTypedef("typedef"); }
    public void add_typedef(AttributeOrTypedef val){ entries.add(val); }
  
  
    public EnumDefinition new_enumDefinition(){ return new EnumDefinition(); }
    public void add_enumDefinition(EnumDefinition val){ entries.add(val); }
  
  
    public AttributeOrTypedef new_attribute(){ return new AttributeOrTypedef(); }
    public void add_attribute(AttributeOrTypedef val){ entries.add(val); }
  
    public MethodDef new_methodDef(){ return new MethodDef(); }
    public void add_methodDef(MethodDef val){ entries.add(val); }
  
    public MethodDef new_inlineMethod(){ return new MethodDef(); }
    public void add_inlineMethod(MethodDef val){ val.inline = true; entries.add(val); }
  
    public Description new_implementDescription(){ return new Description("implementDescription"); }
    public void add_implementDescription(Description val){ entries.add(val); }
  

    
    public void set_modifier(String val){}
    
  }
  
  
  
  public static class HeaderBlockEntry
  {
    public String whatisit;
    //public String data;
    
    public Description description;
    
    public List<Description> implementDescriptions;
  
    HeaderBlockEntry(String whatisit){ this.whatisit = whatisit; }
    
    public Description new_implementDescription(){ return new Description("implementDescription"); }
    public void add_implementDescription(Description val){ 
      if(implementDescriptions == null) { implementDescriptions = new LinkedList<Description>(); }
      implementDescriptions.add(val);
    }

    

  }
  
  
  
  
  public static class Description extends HeaderBlockEntry
  {
    Description(String whatisit){ super(whatisit); }
    public Description(){ super("description"); }
    public String text = "";
    public String simulinkTag;
    
    public ParamDescription returnDescription;
    
    public List<ParamDescription> paramDescriptions;
    
    public List<ParamDescription> auxDescriptions;
    
    
    public void set_text(String text){ this.text += text; }
    
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
    
    
    public Value new_condition(){ return new Value(); }
    public void add_condition(Value val){ conditionValue = val; }
  }
  
  
  
  
  public static class ConditionBlock extends HeaderBlock
  {
    ConditionBlock(){ super("#ifdef"); }
    
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
    
    public String elseConditionBlock;

    public void conditionDef(String val) { not = false; conditionDef = val; }
    
    public HeaderBlock elseBlock;
    
    
    public void set_conditionDefNot(String val) { not = true; conditionDef = val; }
    
    
    public OrCondition new_OrCondition(){ return new OrCondition(); }
    public void add_OrCondition(OrCondition val) { 
      if(orConditions == null) {orConditions = new LinkedList<OrCondition>(); } 
      orConditions.add(val); 
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
    
    public Description description;
    
    public String valueDef = "";
    
    public int intvalue, hexvalue;
    
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
  
  


  
  public static class StructDecl extends HeaderBlockEntry
  { StructDecl(){ super("structDecl"); }
    public String name;
  }
  
  
  public static class StructDefinition extends HeaderBlock
  { 
    StructDefinition(String whatisit, boolean isUnion){ super(whatisit); this.isUnion = isUnion; }
  
    public final boolean isUnion;
    
    public String tagname, name;
    
    public String conditionDef;
  
    public StructDefinition new_implicitStructAttribute() { return new StructDefinition("unnamedStructAttr", false); }

    public void add_implicitStructAttribute(StructDefinition val) { entries.add(val); }

    public StructDefinition new_implicitUnionAttribute() { return new StructDefinition("unnamedUnionAttr", true); }

    public void add_implicitUnionAttribute(StructDefinition val) { entries.add(val); }

    public void set_implicitStruct() { name = "?"; }
    
    public void set_variante(String val) {} //only formally necessary because [<?variante>...] it stores the text.

    public StructDefinition new_variante() { return this; }

    public void add_variante(StructDefinition  val){} //already added.

    @Override public String toString(){ return name; }

  
  }

  
  
  public static class EnumDefinition extends HeaderBlockEntry
  {
    EnumDefinition() { super("enumDef"); }
    
    public String tagname, name;
    public Map<String, EnumElement> values = new TreeMap<String, EnumElement>();
    
    public EnumElement new_enumElement(){ return new EnumElement(); }
    public void add_enumElement(EnumElement val){ values.put(val.name, val); }
    
  }
  
  public static class EnumElement
  {
    public String name;
    
    public long intnumber, hexnumber;
  }
  
  
  
  
  
  
  /**An attribute in a struct (variable definition).
   * Note: the parser creates via syntax more as one instance for <pre>
   *  Type name1, name2; </pre>
   * The Type information is duplicated in the parser's result already. 
   */
  public static class AttributeOrTypedef extends HeaderBlockEntry
  { AttributeOrTypedef(String whatisit){ super(whatisit); }
    AttributeOrTypedef(){ super("Attribute"); }
  
    
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
    
    /**Used only on conditionArgument. */
    public String conditionDef;
    
    public Arraysize arraysize;
  
  
    public Description new_description() { return description = new Description(); }
    public void add_description(Description val){ /*already added.*/ } 
  
    @Override public String toString(){ return name + ": " + type; }
  
  
  }
  
  
  
  
  
  
  
  public static class Type
  {
    public String name;
    
    /**Will be set if a type is given with a macro <code>MACRO(argument)</code>.
     * This is usefully for special cases only.
     */
    public String macro;
    
    public String forward;
    
    public boolean pointer;
    
    public boolean pointer2;
    
    public String modifier;
    
    public String macroArg;
    
    /**Modifier const* and const**. */
    public boolean constPointer, constPointer2; 
    
    public void set_macro_arg(String arg) {
      macroArg = arg;
      macro = name;
      name = null;
    }
    
    @Override public String toString(){ return name; }
  }
  
  
  
  
  public static class Arraysize
  {
    public int value;
    
    public String symbolValue;
    
    public Arraysize new_arraysize() { return this; }
    public void add_arraysize(Arraysize val) {}
  }
  
  
  
  
  public static class MethodDef extends HeaderBlockEntry
  { MethodDef(){ super("method"); }

    public Type type;
    
    public String name;
    
    public boolean inline;
    
    public List<AttributeOrTypedef> args;
    
    
    StatementBlock body;
    
    
     
    
    public AttributeOrTypedef new_conditionalArgument(){ return new AttributeOrTypedef(); }
    public void add_conditionalArgument(AttributeOrTypedef val){ 
      if(args == null) { args = new LinkedList<AttributeOrTypedef>(); }
      args.add(val);
    }

    public AttributeOrTypedef new_typedParameter(){ return new AttributeOrTypedef(); }
    public void add_typedParameter(AttributeOrTypedef val){ 
      if(args == null) { args = new LinkedList<AttributeOrTypedef>(); }
      args.add(val);
    }

    
    public StatementBlock new_statementBlock(){ return body = new StatementBlock(); }
    public void add_statementBlock(StatementBlock val){ }
    
    
    
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
  
    public Value value;
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
      catch(Exception exc){ throw new IllegalArgumentException("CheaderParser - file ERROR; " + fileIn.getAbsolutePath()); }
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
