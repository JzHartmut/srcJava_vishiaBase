package org.vishia.header2Reflection;

import java.io.File;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.vishia.mainCmd.MainCmd_ifc;
import org.vishia.stateMGen.StateMGen.CompositeState;
import org.vishia.stateMGen.StateMGen.NameValue;
import org.vishia.stateMGen.StateMGen.SimpleState;
import org.vishia.stateMGen.StateMGen.XXXStateStructure;
import org.vishia.stateMGen.StateMGen.ZbnfState;
import org.vishia.util.FileSystem;
import org.vishia.util.StringPart;
import org.vishia.zbnf.ZbnfJavaOutput;
import org.vishia.zbnf.ZbnfParseResultItem;
import org.vishia.zbnf.ZbnfParser;

/**This class parses C-files and builds a result tree, which can be proceed.
 * @author Hartmut Schorrig
 *
 */
public class CheaderParser {

  
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
    
    public HeaderBlock new_HeaderBlock(){ return this; }
    public void add_HeaderBlock(HeaderBlock val){}
  }
  
  
  
  public static class HeaderBlock extends HeaderBlockEntry
  { HeaderBlock(){ super("{}"); }
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
    
    
    public StructDefinition new_structDefinition(){ return new StructDefinition(); }
    public void add_structDefinition(StructDefinition val){ entries.add(val); }
    
    public AttributeOrTypedef new_typedef(){ return new AttributeOrTypedef("typedef"); }
    public void add_typedef(AttributeOrTypedef val){ entries.add(val); }
  
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
    public String text;
    public String simulinkTag;
    
    public ParamDescription returnDescription;
    
    public List<ParamDescription> paramDescriptions;
    
    public List<ParamDescription> auxDescriptions;
    
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
  
  
  public static class StructDefinition extends StructContent
  { StructDefinition(){ super("structDefinition"); }
    public String tagname, name;
    public Description description;
  }

  
  
  
  
  public static class StructContent extends HeaderBlock
  { StructContent(String whatisit){ super(whatisit); }
    
  
  }
  
  
  
  /**An attribute in a struct (variable definition).
   * Note: the parser creates via syntax more as one instance for <pre>
   *  Type name1, name2; </pre>
   * The Type information is duplicated in the parser's result already. 
   */
  public static class AttributeOrTypedef extends HeaderBlockEntry
  { AttributeOrTypedef(String whatisit){ super(whatisit); }
    AttributeOrTypedef(){ super("Attribute"); }
  
    public Description description;
  
    public Type type;
    
    public String name;
    
    public int bitField;
    
    public Arraysize arraysize;
  }
  
  
  
  
  
  
  
  public static class Type
  {
    public String name;
    
    public String forward;
    
    public boolean pointer;
    
    public boolean pointer2;
    
    /**Modifier const* and const**. */
    public boolean constPointer, constPointer2; 
    
  }
  
  
  
  
  public static class Arraysize
  {
    public int value;
    
    public String symbolValue;
  }
  
  
  
  
  public static class MethodDef extends HeaderBlockEntry
  { MethodDef(){ super("method"); }

    public Type type;
    
    public String name;
    
    public boolean inline;
    
    public List<AttributeOrTypedef> args;
    
    
    StatementBlock body;
    
    
     
    
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
    
    
  }
  
  
  
  public static class Statement extends ValueEntry
  { public Statement(char whatisit)
    { super(whatisit);
    }
    
    
  }
  
  
  
  public static class Assignment extends Statement
  { public Assignment(){ super('='); }
    
    public Variable variable;
    
    public String assignOperator;
    
    public Value value;
  }
  
  
  
  
  
  public static class VariableDefinition extends Statement
  { public VariableDefinition(){ super('d'); }
    
    public Description description;
    
    public Type type;
    
    public String name;
    
    public int bitField;
    
    public Arraysize arraysize;
  
    public Value value;
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
    
    public void set_variable(java.lang.String val){}
    
    public Variable new_variable(){ return new Variable(); }
    public void add_variable(Variable val){ entries.add(val); }
    
    public Value new_value(){ Value entry = new Value(); entry.unaryOperator = this.unaryOperator; this.unaryOperator = null; return entry; }
    public void add_value(Value val){ entries.add(val); }
    
    
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
    
    
    
    
    public void set_binaryOperator(String val){ entries.add(new ValueEntry('b', val)); }
    
  }
  
  
  public static class ValueEntry
  {
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
        ZbnfResultFile resultFile = new ZbnfResultFile(src.name, src.path);
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
