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
    public List<IncludeDef> includeDef = new LinkedList<IncludeDef>();
    
    public String headerEntry;
    public String headerEntryDef;
    

    public List<ClassC> listClassC;
    
    
    public void XXset_HeaderEntry(String val) {}
    public void XXset_HeaderEntryDef(String val) {}
   
    public IncludeDef XXnew_includeDef(){ return new IncludeDef(); }
    public void XXadd_includeDef(IncludeDef val){  }

    
    public ClassC new_outside() {
      ClassC classC = new ClassC();
      classC.name = "--outside--";
      return classC;
    }
    
    public void add_outside(ClassC val){ 
      if(listClassC == null) { listClassC = new LinkedList<ClassC>(); }
      listClassC.add(val);
    }

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
    
    public Attribute new_attribute(){ return new Attribute(); }
    public void add_attribute(Attribute val){ entries.add(val); }
  
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
    
    HeaderBlockEntry(String whatisit){ this.whatisit = whatisit; }
  }
  
  
  
  
  public static class Description extends HeaderBlockEntry
  {
    Description(String whatisit){ super(whatisit); }
    public Description(){ super("description"); }
    public String text;
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
    
    
    public void conditionDefNot(String val) { not = true; conditionDef = val; }
    
    
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
    public String valueDef = "";
    
    public void set_value(String val){ valueDef += val; }
    
  }
  
  
  public static class StructDecl extends HeaderBlockEntry
  { StructDecl(){ super("structDecl"); }
    public String name;
  }
  
  
  public static class StructDefinition extends StructContent
  { StructDefinition(){ super("structDefinition"); }
    public String tagname, name;
  }

  
  
  
  
  public static class StructContent extends HeaderBlock
  { StructContent(String whatisit){ super(whatisit); }
    
  
  }
  
  
  
  /**An attribute in a struct (variable definition).
   * Note: the parser creates via syntax more as one instance for <pre>
   *  Type name1, name2; </pre>
   * The Type information is duplicated in the parser's result already. 
   */
  public static class Attribute extends HeaderBlockEntry
  { Attribute(){ super("Attribute"); }
    
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
    
  }
  
  
  
  
  public static class Arraysize
  {
    public int value;
    
    public String symbolValue;
  }
  
  
  
  
  public static class MethodDef extends HeaderBlockEntry
  { MethodDef(){ super("method"); }

    public Description description;
    
    public List<Description> implementDescriptions;
  
    public Type type;
    
    public String name;
    
    public boolean inline;
    
    public List<Attribute> args;
    
    
    StatementBlock body;
    
    
    public Description new_implementDescription(){ return new Description("implementDescription"); }
    public void add_implementDescription(Description val){ 
      if(implementDescriptions == null) { implementDescriptions = new LinkedList<Description>(); }
      implementDescriptions.add(val);
    }

    
    
    
    public Attribute new_typedParameter(){ return new Attribute(); }
    public void add_typedParameter(Attribute val){ 
      if(args == null) { args = new LinkedList<Attribute>(); }
      args.add(val);
    }

    
    public StatementBlock new_statementBlock(){ return body = new StatementBlock(); }
    public void add_statementBlock(StatementBlock val){ }
    
    
    
  }
  
  
  
  public static class StatementBlock
  {
    
  }
  
  
  
  
  public static class Value
  {
    public List<ValueEntry> entries = new LinkedList<ValueEntry>(); 
    
    public void set_variable(java.lang.String val){}
    
    public Variable new_variable(){ return new Variable(); }
    public void add_variable(Variable val){ entries.add(val); }
    
    public void set_binaryOperator(String val){ entries.add(new ValueEntry('b', val)); }
    
  }
  
  
  public static class ValueEntry
  {
    public char whatisit;
    
    public String data;

    public ValueEntry(char whatisit, String data)
    { this.whatisit = whatisit;
      this.data = data;
    }
    
    public ValueEntry(char whatisit)
    { this.whatisit = whatisit;
    }
  
    
  }
  
  
  
  public static class Variable extends ValueEntry
  { Variable(){ super('V'); }
    public boolean preDecrement, preIncrement, postDecrement, postIncrement;
    public String simpleVariable;
  }
  
  
  
  
  
  
  
  private final MainCmd_ifc console;
  
  
  public CheaderParser(MainCmd_ifc console) {
    this.console = console;
  }
  
  
  
  public void execute(Args args) {
    parseAndStoreInput(args);
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
        ZbnfResultFile resultFile = new ZbnfResultFile();
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
