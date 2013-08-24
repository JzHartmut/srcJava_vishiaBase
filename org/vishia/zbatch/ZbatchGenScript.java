package org.vishia.zbatch;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.vishia.checkDeps_C.AddDependency_InfoFileDependencies;
import org.vishia.mainCmd.MainCmdLogging_ifc;
import org.vishia.util.Assert;
import org.vishia.util.CalculatorExpr;
import org.vishia.util.DataAccess;
import org.vishia.util.FileSystem;
import org.vishia.util.StringFunctions;
import org.vishia.util.StringPart;
import org.vishia.util.StringPartFromFileLines;
import org.vishia.util.UnexpectedException;
import org.vishia.xmlSimple.SimpleXmlOutputter;
import org.vishia.xmlSimple.XmlException;
import org.vishia.xmlSimple.XmlNode;
import org.vishia.zbnf.ZbnfJavaOutput;
import org.vishia.zbnf.ZbnfParser;

/**This class contains control data and sub-routines to generate output texts from internal data.
 * 
 * @author Hartmut Schorrig
 *
 */
public class ZbatchGenScript {
  /**Version, history and license.
   * <ul>
   * <li>2014-07-30 Hartmut chg {@link #translateAndSetGenCtrl(File)} returns void.
   * <li>2014-07-20 Hartmut chg Some syntactical changes.
   * <li>2013-07-14 Hartmut tree traverse enable because {@link Argument#parentList} and {@link StatementList#parentStatement}
   * <li>2013-06-20 Hartmut new: Syntax with extArg for textual Arguments in extra block
   * <li>2013-03-10 Hartmut new: <code><:include:path></code> of a sub script is supported up to now.
   * <li>2013-10-09 Hartmut new: <code><:scriptclass:JavaPath></code> is supported up to now.
   * <li>2013-01-13 Hartmut chg: The {@link Expression#ascertainValue(Object, Map, boolean, boolean, boolean)} is moved
   *   and adapted from TextGenerator.getContent. It is a feauture from the Expression to ascertain its value.
   *   That method and {@link Expression#text()} can be invoked from a user script immediately.
   *   The {@link Expression} is used in {@link org.vishia.zmake.ZmakeUserScript}.
   * <li>2013-01-02 Hartmut chg: localVariableScripts removed. The variables in each script part are processed
   *   in the order of statements of generation. In that kind a variable can be redefined maybe with its own value (cummulative etc.).
   *   A ZText_scriptVariable is valid from the first definition in order of generation statements.
   * <li>2012-12-24 Hartmut chg: Now the 'ReferencedData' are 'namedArgument' and it uses 'dataAccess' inside. 
   *   The 'dataAccess' is represented by a new {@link Statement}('e',...) which can have {@link Expression#constValue} 
   *   instead a {@link Expression#datapath}. 
   * <li>2012-12-24 Hartmut chg: {@link ZbnfDataPathElement} is a derived class of {@link DataAccess.DatapathElement}
   *   which contains destinations for argument parsing of a called Java-subroutine in a dataPath.  
   * <li>2012-12-23 Hartmut chg: A {@link Statement} and a {@link Argument} have the same usage aspects for arguments
   *   which represents values either as constants or dataPath. Use Argument as super class for ScriptElement.
   * <li>2012-12-23 Hartmut new: formatText in the {@link Expression#textArg} if a data path is given, use for formatting a numerical value.   
   * <li>2012-12-22 Hartmut new: Syntax as constant string inside. Some enhancements to set control: {@link #translateAndSetGenCtrl(StringPart)} etc.
   * <li>2012-12-22 Hartmut chg: <:if:...> uses {@link CalculatorExpr} for expressions.
   * <li>2012-11-24 Hartmut chg: @{@link Statement#datapath} with {@link DataAccess.DatapathElement} 
   * <li>2012-11-25 Hartmut chg: Now Variables are designated starting with $.
   * <li>2012-10-19 Hartmut chg: <:if...> works.
   * <li>2012-10-19 Hartmut chg: Renaming: {@link Statement} instead Zbnf_ScriptElement (shorter). The Scriptelement
   *   is the component for the genContent-Elements now instead Zbnf_genContent. This class contains attributes of the
   *   content elements. Only if a sub content is need, an instance of Zbnf_genContent is created as {@link Statement#subContent}.
   *   Furthermore the {@link Statement#subContent} should be final because it is only created if need for the special 
   *   {@link Statement#elementType}-types (TODO). This version works for {@link org.vishia.stateMGen.StateMGen}.
   * <li>2012-10-11 Hartmut chg Syntax changed of ZmakeGenCtrl.zbnf: datapath::={ <$?path>? \.}. 
   *   instead dataAccess::=<$?name>\.<$?elementPart>., it is more universal. adapted. 
   * <li>2012-10-10 new: Some enhancements, it is used for {@link org.vishia.zbatch.ZbatchExecuter} now too.
   * <li>2011-03-00 created.
   *   It is the concept of specialized {@link GralWidget}.
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
  static final public int version = 20130310;

  final MainCmdLogging_ifc console;

  /**Helper to transfer parse result into the java classes {@link ZbnfMainGenCtrl} etc. */
  final ZbnfJavaOutput parserGenCtrl2Java;

  /**Mirror of the content of the zmake-genctrl-file. Filled from ZBNF-ParseResult*/
  //ZbnfMainGenCtrl zTextGenCtrl;
  
  //final Map<String, Statement> zmakeTargets = new TreeMap<String, Statement>();
  
  final Map<String, Statement> subtextScripts = new TreeMap<String, Statement>();
  
  
  
  /**List of the script variables in order of creation in the jbat script file and all includes.
   * The script variables can contain inputs of other variables which are defined before.
   * Therefore the order is important.
   * This list is stored firstly in the {@link StatementList#content} in an instance of 
   * {@link ZbnfMainGenCtrl} and then transferred from all includes and from the main script 
   * to this container because the {@link ZbnfMainGenCtrl} is only temporary and a ensemble of all
   * Statements should be present from all included files. The statements do not contain
   * any other type of statement than script variables because only ScriptVariables are admissible
   * in the syntax. Outside of subroutines and main there should only exist variable definitions. 
   */
  private final List<Statement> listScriptVariables = new ArrayList<Statement>();

  /**The script element for the whole file. It shall contain calling of <code><*subtext:name:...></code> 
   */
  Statement scriptFile;
  
  
  final ZbatchExecuter executer;

  //public String scriptclassMain;

  public ZbatchGenScript(ZbatchExecuter executer, MainCmdLogging_ifc console)
  { this.console = console;
    this.executer = executer;
    this.parserGenCtrl2Java = new ZbnfJavaOutput(console);

  }

  
  public void translateAndSetGenCtrl(File fileZbnf4GenCtrl, File fileGenCtrl, File checkXmlOut) 
  throws FileNotFoundException, IOException
    , ParseException, XmlException, IllegalArgumentException, IllegalAccessException, InstantiationException
  { console.writeInfoln("* Zmake: parsing gen script \"" + fileZbnf4GenCtrl.getAbsolutePath() 
    + "\" with \"" + fileGenCtrl.getAbsolutePath() + "\"");

    int lengthBufferSyntax = (int)fileZbnf4GenCtrl.length();
    StringPart spSyntax = new StringPartFromFileLines(fileZbnf4GenCtrl, lengthBufferSyntax, "encoding", null);

    int lengthBufferGenctrl = (int)fileGenCtrl.length();
    StringPart spGenCtrl = new StringPartFromFileLines(fileGenCtrl, lengthBufferGenctrl, "encoding", null);

    File fileParent = FileSystem.getDir(fileGenCtrl);
    translateAndSetGenCtrl(new StringPart(ZbatchSyntax.syntax), new StringPart(spGenCtrl), checkXmlOut, fileParent);
  }
  

  public void translateAndSetGenCtrl(File fileGenCtrl) 
  throws FileNotFoundException, IllegalArgumentException, IllegalAccessException, InstantiationException, IOException, ParseException, XmlException 
  {
    translateAndSetGenCtrl(fileGenCtrl, null);
  }
  
  
  public void translateAndSetGenCtrl(File fileGenCtrl, File checkXmlOut) 
  throws FileNotFoundException, IllegalArgumentException, IllegalAccessException, InstantiationException, IOException, ParseException, XmlException 
  {
    int lengthBufferGenctrl = (int)fileGenCtrl.length();
    StringPart spGenCtrl = new StringPartFromFileLines(fileGenCtrl, lengthBufferGenctrl, "encoding", null);
    File fileParent = FileSystem.getDir(fileGenCtrl);
    translateAndSetGenCtrl(new StringPart(ZbatchSyntax.syntax), new StringPart(spGenCtrl), checkXmlOut, fileParent);
  }
  
  
  public void translateAndSetGenCtrl(String spGenCtrl) 
  throws IllegalArgumentException, IllegalAccessException, InstantiationException, ParseException 
  {
    try{ 
      translateAndSetGenCtrl(new StringPart(ZbatchSyntax.syntax), new StringPart(spGenCtrl), null, null);
    } catch(IOException exc){ throw new UnexpectedException(exc); }
  }
  
  
  public void translateAndSetGenCtrl(StringPart spGenCtrl) 
  throws ParseException, IllegalArgumentException, IllegalAccessException, InstantiationException 
  {
    try { 
      translateAndSetGenCtrl(new StringPart(ZbatchSyntax.syntax), spGenCtrl, null, null);
    }catch(IOException exc){ throw new UnexpectedException(exc); }
  }
  
  
  
  /**Translate the generation control file - core routine.
   * It sets the {@link #zTextGenCtrl} aggregation. This routine must be called before  the script can be used.
   * There are some routines without the parameter sZbnf4GenCtrl, which uses the internal syntax. Use those if possible:
   * {@link #translateAndSetGenCtrl(File)}, {@link #translateAndSetGenCtrl(String)}
   * <br><br>
   * This routine will be called recursively if scripts are included.
   * 
   * @param sZbnf4GenCtrl The syntax. This routine can use a special syntax. The default syntax is {@link ZbatchSyntax#syntax}.
   * @param spGenCtrl The input file with the genCtrl statements.
   * @param checkXmlOut If not null then writes the parse result to this file, only for check of the parse result.
   * @param fileParent directory of the used file as start directory for included scripts. 
   *   null possible, then the script should not contain includes.
   * @return a new instance of {@link ZbnfMainGenCtrl}. This method returns null if there is an error. 
   * @throws ParseException
   * @throws IllegalArgumentException
   * @throws IllegalAccessException
   * @throws InstantiationException
   * @throws IOException only if xcheckXmlOutput fails
   * @throws FileNotFoundException if a included file was not found or if xcheckXmlOutput file not found or not writeable
   */
  public void translateAndSetGenCtrl(StringPart sZbnf4GenCtrl, StringPart spGenCtrl, File checkXmlOutput, File fileParent) 
  throws ParseException, IllegalArgumentException, IllegalAccessException, InstantiationException, FileNotFoundException, IOException 
  { boolean bOk;
    ZbnfParser parserGenCtrl = new ZbnfParser(console);
    parserGenCtrl.setSyntax(sZbnf4GenCtrl);
    if(console.getReportLevel() >= MainCmdLogging_ifc.fineInfo){
      console.reportln(MainCmdLogging_ifc.fineInfo, "== Syntax GenCtrl ==");
      parserGenCtrl.reportSyntax(console, MainCmdLogging_ifc.fineInfo);
    }
    console.writeInfo(" ... ");
    translateAndSetGenCtrl(parserGenCtrl, spGenCtrl, checkXmlOutput, fileParent);
  }
    
    
    
    
  private void translateAndSetGenCtrl(ZbnfParser parserGenCtrl, StringPart spGenCtrl
      , File checkXmlOutput, File fileParent) 
  throws ParseException, IllegalArgumentException, IllegalAccessException, InstantiationException, FileNotFoundException, IOException 
  { boolean bOk;
    
    bOk = parserGenCtrl.parse(spGenCtrl);
    if(!bOk){
      String sError = parserGenCtrl.getSyntaxErrorReport();
      throw new ParseException(sError,0);
    }
    if(checkXmlOutput !=null){
      //XmlNodeSimple<?> xmlParseResult = parserGenCtrl.getResultTree();
      XmlNode xmlParseResult = parserGenCtrl.getResultTree();
      SimpleXmlOutputter xmlOutputter = new SimpleXmlOutputter();
      OutputStreamWriter xmlWriter = new OutputStreamWriter(new FileOutputStream(checkXmlOutput));
      xmlOutputter.write(xmlWriter, xmlParseResult);
      xmlWriter.close();
    }
    //if(console.getReportLevel() >= MainCmdLogging_ifc.fineInfo){
    //  parserGenCtrl.reportStore((Report)console, MainCmdLogging_ifc.fineInfo, "Zmake-GenScript");
    //}
    //write into Java classes:
    ZbnfMainGenCtrl zbnfGenCtrl = new ZbnfMainGenCtrl();
    parserGenCtrl2Java.setContent(ZbnfMainGenCtrl.class, zbnfGenCtrl, parserGenCtrl.getFirstParseResult());
    
    //if(this.scriptclassMain ==null){
    //this.scriptclassMain = zbnfGenCtrl.scriptclass;
    //}
    if(zbnfGenCtrl.includes !=null){
      //parse includes after processing this file, because the zbnfGenCtrl.includes are not set before.
      //If one include contain a main, use it. But override the main after them, see below.
      for(String sFileInclude: zbnfGenCtrl.includes){
        File fileInclude = new File(fileParent, sFileInclude);
        if(!fileInclude.exists()){
          System.err.printf("TextGenScript - translateAndSetGenCtrl, included file not found; %s\n", fileInclude.getAbsolutePath());
          throw new FileNotFoundException("TextGenScript - translateAndSetGenCtrl, included file not found: " + fileInclude.getAbsolutePath());
        }
        File fileIncludeParent = FileSystem.getDir(fileInclude);
        int lengthBufferGenctrl = (int)fileInclude.length();
        StringPart spGenCtrlSub = new StringPartFromFileLines(fileInclude, lengthBufferGenctrl, "encoding", null);
        translateAndSetGenCtrl(parserGenCtrl, spGenCtrlSub, checkXmlOutput, fileIncludeParent);
      }
    }
    if(zbnfGenCtrl.scriptFileSub !=null){
      //use the last found main, also from a included script but firstly from main.
      scriptFile = zbnfGenCtrl.scriptFileSub;   
    }
    if(zbnfGenCtrl.content !=null){
      listScriptVariables.addAll(zbnfGenCtrl.content);
    }
  }
  
  
  /**Searches the Zmake-target by name (binary search. TreeMap.get(name).
   * @param identArgJbat The name of given < ?translator> in the end-users script.
   * @return null if the Zmake-target is not found.
  public final StatementList searchZmakeTaget(String name){ 
    Statement target = zmakeTargets.get(name);
    return target == null ? null : target.subContent;
  }
   */
  
  
  //public final String getScriptclass(){ return scriptclassMain; }
  
  public final Statement getFileScript(){ return scriptFile; }
  
  
  public Statement getSubtextScript(CharSequence name){ return subtextScripts.get(name.toString()); }
  
  
  List<Statement> getListScriptVariables(){ return listScriptVariables; }



  public static class XXXZbnfDataPathElement extends CalculatorExpr.DataPathItem
  {
    final Argument parentStatement;
    
    protected List<Argument> paramArgument;

    //List<ZbnfDataPathElement> actualArguments;
    
    List<Expression> XXXactualValue;
    
    boolean bExtArgs;
    
    
    public XXXZbnfDataPathElement(Argument statement){
      this.parentStatement = statement;
    }
    
    
    /**Set if the arguments are listed outside of the element
     * 
     */
    public void XXXset_extArgs(){
      //if(actualValue == null){ actualValue = new ArrayList<Expression>(); }
      bExtArgs = true;
      //Expression actualArgument = new Expression();
      //actualValue.add(actualArgument);
    }
    
    
    public Expression XXXnew_argument(){
      Expression actualArgument = new Expression(parentStatement);
      //ScriptElement actualArgument = new ScriptElement('e', null);
      //ZbnfDataPathElement actualArgument = new ZbnfDataPathElement();
      return actualArgument;
    }

    
    /**From Zbnf.
     * The Arguments of type {@link Statement} have to be resolved by evaluating its value in the data context. 
     * The value is stored in {@link DataAccess.DatapathElement#addActualArgument(Object)}.
     * See {@link #add_datapathElement(org.vishia.util.DataAccess.DatapathElement)}.
     * @param val The Scriptelement which describes how to get the value.
     */
    public void XXXadd_argument(Expression val){ 
      if(paramExpr == null){ paramExpr = new ArrayList<CalculatorExpr>(); }
      paramExpr.add(val);
    } 
    
    public Argument new_argument(){
      Argument actualArgument = new Argument(parentStatement.parentList);
      //ScriptElement actualArgument = new ScriptElement('e', null);
      //ZbnfDataPathElement actualArgument = new ZbnfDataPathElement();
      return actualArgument;
    }

    
    /**From Zbnf.
     * The Arguments of type {@link Statement} have to be resolved by evaluating its value in the data context. 
     * The value is stored in {@link DataAccess.DatapathElement#addActualArgument(Object)}.
     * See {@link #add_datapathElement(org.vishia.util.DataAccess.DatapathElement)}.
     * @param val The Scriptelement which describes how to get the value.
     */
    public void add_argument(Argument val){ 
      if(paramArgument == null){ paramArgument = new ArrayList<Argument>(); }
      paramArgument.add(val);
    } 
    
    public void set_javapath(String text){ this.ident = text; }
    


  }
  
  

  /**
  *
  */
  public static class Expression extends ZbatchZbnfExpression
  {
  
    final Argument parentStatement;
    
    /**If need, a sub-content, maybe null.*/
    public StatementList genString;
    
    List<XXXZbnfValue> XXXvalues = new ArrayList<XXXZbnfValue>();
  
  
    public Expression(Argument statement){
      this.parentStatement = statement;  
    }
    
    //public ZbnfValue new_value(){ return new ZbnfValue(parentStatement); }
    
    //public void add_value(ZbnfValue val){ values.add(val); }
  
    
    /**From Zbnf, a part <:>...<.> */
    public StatementList new_genString(){ return genString = new StatementList(); }
    
    public void add_genString(StatementList val){}
    
    
    @Override public CalculatorExpr.Value calcDataAccess(Map<String, Object> javaVariables, Object... args) 
    throws Exception{
      if(genString !=null){
        ZbatchExecuter.ExecuteLevel executer = (ZbatchExecuter.ExecuteLevel)javaVariables.get("jbatExecuteLevel");
        StringBuilder u = new StringBuilder();
        executer.executeNewlevel(genString, u, false);
        return new CalculatorExpr.Value(u.toString());
      } else {
        return super.calcDataAccess(javaVariables, args);
      }
    }

  }
  
  
  
  
  public static class XXXXZbnfOperation extends CalculatorExpr.Operation
  {
    final Argument parentStatement;
    
    XXXXZbnfOperation(String operator, Argument parentStatement){ 
      super(operator); 
      this.parentStatement = parentStatement;
    }

  
    /**Set a integer (long) argument of a access method. From Zbnf <#?intArg>. */
    public void set_intValue(long val){ this.value = new CalculatorExpr.Value(val); }
    
    /**Set a integer (long) argument of a access method. From Zbnf <#?intArg>. */
    public void set_floatValue(double val){ this.value = new CalculatorExpr.Value(val); }
    
    /**Set a integer (long) argument of a access method. From Zbnf <#?intArg>. */
    @Override
    public void set_textValue(String val){ this.value = new CalculatorExpr.Value(val); }
    
    /**Set a integer (long) argument of a access method. From Zbnf <#?intArg>. */
    public void set_charValue(String val){ this.value = new CalculatorExpr.Value(val.charAt(0)); }
    
    
    public XXXZbnfDataPathElement new_datapathElement(){ return new XXXZbnfDataPathElement(parentStatement); }
    
    public void add_datapathElement(XXXZbnfDataPathElement val){ 
      super.add_datapathElement(val); 
    }
    
    public void set_envVariable(String ident){
      DataAccess.DatapathElement element = new DataAccess.DatapathElement();
      element.whatisit = 'e';
      element.ident = ident;
      add_datapathElement(element);
    }
    

    public void set_startVariable(String ident){
      DataAccess.DatapathElement element = new DataAccess.DatapathElement();
      element.whatisit = 'v';
      element.ident = ident;
      add_datapathElement(element);
    }
    
    
    public XXXZbnfDataPathElement new_newJavaClass()
    { XXXZbnfDataPathElement value = new XXXZbnfDataPathElement(parentStatement);
      value.whatisit = 'n';
      //ScriptElement contentElement = new ScriptElement('J', null); ///
      //subContent.content.add(contentElement);
      return value;
    }
    
    public void add_newJavaClass(XXXZbnfDataPathElement val) { add_datapathElement(val); }


    public XXXZbnfDataPathElement new_staticJavaMethod()
    { XXXZbnfDataPathElement value = new XXXZbnfDataPathElement(parentStatement);
      value.whatisit = 's';
      return value;
      //ScriptElement contentElement = new ScriptElement('j', null); ///
      //subContent.content.add(contentElement);
      //return contentElement;
    }
    
    public void add_staticJavaMethod(XXXZbnfDataPathElement val) { add_datapathElement(val); }



  
  }
  
  
  
  /**A Value of a expression or a left value. The syntax determines what is admissible.
   *
   */
  public static class XXXZbnfValue extends CalculatorExpr.Value {
    
    final Argument parentStatement;
    
    
    /**Name of the argument. It is the key to assign calling argument values. */
    //public String name;
    
    /**From Zbnf <""?text>, constant text, null if not used. */
    //public String text; 
    
    /**Maybe a constant value, also a String. */
    //public Object constValue;
    
    char operator;
    
    char unaryOperator;
    
    /**If need, a sub-content, maybe null.*/
    public StatementList genString;
    
    
    public XXXZbnfValue(Argument statement){ 
      this.parentStatement = statement;
    }

    public void set_operator(String val){ operator = val.charAt(0); }
    
    public void set_unaryOperator(String val){ unaryOperator = val.charAt(0); }
    
    /**Set a integer (long) argument of a access method. From Zbnf <#?intArg>. */
    public void set_intValue(long val){ type = 'o'; oVal = new Long(val); }
    
    /**Set a integer (long) argument of a access method. From Zbnf <#?intArg>. */
    public void set_floatValue(double val){ type = 'o'; oVal = new Double(val); }
    
    /**Set a integer (long) argument of a access method. From Zbnf <#?intArg>. */
    public void set_textValue(String val){ type = 'o'; oVal = val; }
    
    /**Set a integer (long) argument of a access method. From Zbnf <#?intArg>. */
    public void set_charValue(String val){ type = 'o'; oVal = new Character(val.charAt(0)); }
    
    /**ZBNF: <code>info ( < datapath? > <?datapath > )</code>.
     * 
     * @return this. The syntax supports only datapath elements. But the instance is the same.
     */
    public void set_datapath(){ type = 'd'; } 
    
    /**ZBNF: <code>info ( < datapath? > <?info > )</code>.
     * 
     * @return this. The syntax supports only datapath elements. But the instance is the same.
     */
    public void set_info(){ type = 'i'; } 
    
    public XXXZbnfDataPathElement new_datapathElement(){ return new XXXZbnfDataPathElement(parentStatement); }
    
    public void add_datapathElement(XXXZbnfDataPathElement val){ 
    }
    
    /**From Zbnf, a part <:>...<.> */
    public StatementList new_genString(){ return genString = new StatementList(); }
    
    public void add_genString(StatementList val){}
    
    @Override public String toString(){ return "value"; }

  }
  

  
  
  
  
  
  /**Superclass for ScriptElement, but used independent for arguments.
   * @author hartmut
   *
   */
  public static class Argument  { //CalculatorExpr.Datapath{
    
    
    final StatementList parentList;
    
    /**Name of the argument. It is the key to assign calling argument values. */
    protected String identArgJbat;
   
    public Expression expression;
    
    
    DataAccess dataAccess;
  
    /**From Zbnf <""?textInStatement>, constant text, null if not used. */
    protected String textArg; 
    
    /**If need, a sub-content, maybe null.*/
    public StatementList subContent;
    
    public Argument(StatementList parentList){
      this.parentList = parentList;
    }
    
    public void set_name(String name){ this.identArgJbat = name; }
    
    public String getIdent(){ return identArgJbat; }
    
    /**Designates a boolean expression as any condition, etc. if, while
     * @return The expression component.
     */
    public Expression new_condition(){ return new Expression(this); }
    
    public void add_condition(Expression val){ 
      val.closeExprPreparation();
      expression = val; 
    }
    
    public Expression XXXnew_boolExpr(){ return new Expression(this); }
    
    public void XXXadd_boolExpr(Expression val){ expression = val; }
    
    public Expression new_expression(){ return new Expression(this); }
    
    public void add_expression(Expression val){ 
      val.closeExprPreparation();
      expression = val; 
    }
    
    public Expression XXXnew_orCondition(){ return new Expression(this); }
    
    public void XXXadd_orCondition(Expression val){ expression = val; }
    
    public void set_text(String text){
      if(text.contains("testt"))
        Assert.stop();
      if(subContent == null){ subContent = new StatementList(this); }
      subContent.set_text(text);
    }
    
    
    public void set_nonEmptyText(String text){
      if(!StringFunctions.isEmptyOrOnlyWhitespaces(text)){
        if(subContent == null){ subContent = new StatementList(this); }
        subContent.set_text(text);
      }
    }
    
    
    
    /**From Zbnf, a part <:>...<.> */
    public StatementList new_genString(){ return subContent = new StatementList(); }
    
    public void add_genString(StatementList val){}
    
    /**ZBNF: <code>info ( < datapath? > <?info > )</code>.
     * 
     * @return this. The syntax supports only datapath elements. But the instance is the same.
     */
    public void set_info(){ 
      this.identArgJbat = "@info";
    }
    
    
    public DataAccess.DataAccessSet new_datapath(){ return new DataAccess.DataAccessSet(); }
    
    public void add_datapath(DataAccess.DataAccessSet val){ 
      dataAccess = val;
    }
    


    
    public void XXXset_textInStatement(String text){
      this.textArg = text;
    }
    
    /**Set from ZBNF:  (\?*<$?dataText>\?) */
    //@Override
    public Statement new_dataText(){ return new Statement(parentList, 'e', null); }
    
    /**Set from ZBNF:  (\?*<*dataText>\?) */
    //@Override
    public void add_dataText(Statement val){ 
      if(subContent == null){ subContent = new StatementList(this); }
      subContent.content.add(val); 
      subContent.onerrorAccu = null; subContent.withoutOnerror.add(val);
    }
    

    
  }
  
  
  
  /**An element of the generate script, maybe a simple text, an condition etc.
   * It may have a sub content with a list of sub scrip elements if need, see aggregation {@link #subContent}. 
   * <br>
   * UML-Notation see {@link org.vishia.util.Docu_UML_simpleNotation}:
   * <pre>
   *   ScriptElement             GenContent          ScriptElement
   *        |                         |              !The Sub content
   *        |-----subContent--------->|                  |
   *        |                         |                  |
   *                                  |----content-----*>|
   * 
   * </pre> 
   * A Statement which presents an variable contains the building algorithm for the content of the variable.
   * Script variable's content were determined on startup of the script execution. There values are stored in specific
   * Maps: TODO
   */
  public static class Statement extends Argument
  {
    /**Designation what presents the element.
     * 
     * <table><tr><th>c</th><th>what is it</th></tr>
     * <tr><td>t</td><td>simple constant text</td></tr>
     * <tr><td>n</td><td>simple newline text</td></tr>
     * <tr><td>T</td><td>textual output to any variable or file</td></tr>
     * <tr><td>l</td><td>add to list</td></tr>
     * <tr><td>i</td><td>content of the input, {@link #textArg} describes the build-prescript, 
     *                   see {@link ZmakeGenerator#getPartsFromFilepath(org.vishia.zmake.ZmakeUserScript.UserFilepath, String)}</td></tr>
     * <tr><td>o</td><td>content of the output, {@link #textArg} describes the build-prescript, 
     *                   see {@link ZmakeGenerator#getPartsFromFilepath(org.vishia.zmake.ZmakeUserScript.UserFilepath, String)}</td></tr>
     * <tr><td>e</td><td>A datatext, from <*expression> or such.</td></tr>
     * <tr><td>XXXg</td><td>content of a data path starting with an internal variable (reference) or value of the variable.</td></tr>
     * <tr><td>s</td><td>call of a subtext by name. {@link #textArg}==null, {@link #subContent} == null.</td></tr>
     * <tr><td>j</td><td>call of a static java method. {@link #identArgJbat}==its name, {@link #subContent} == null.</td></tr>
     * <tr><td>c</td><td>cmd line invocation.</td></tr>
     * <tr><td>d</td><td>cd change current directory.</td></tr>
     * <tr><td>J</td><td>Object variable {@link #identArgJbat}==its name, {@link #subContent} == null.</td></tr>
     * <tr><td>P</td><td>Pipe variable, {@link #textArg} contains the name of the variable</td></tr>
     * <tr><td>U</td><td>Buffer variable, {@link #textArg} contains the name of the variable</td></tr>
     * <tr><td>S</td><td>String variable, {@link #textArg} contains the name of the variable</td></tr>
     * <tr><td>L</td><td>Container variable, a list</td></tr>
     * <tr><td>W</td><td>Opened file, a Writer in Java</td></tr>
     * <tr><td>=</td><td>assignment of an expression to a variable.</td></tr>
     * <tr><td>B</td><td>statement block</td></tr>
     * <tr><td>C</td><td><:for:path> {@link #subContent} contains build.script for any list element,</td></tr>
     * <tr><td>E</td><td><:else> {@link #subContent} contains build.script for any list element,</td></tr>
     * <tr><td>F</td><td><:if:condition:path> {@link #subContent} contains build.script for any list element,</td></tr>
     * <tr><td>G</td><td><:elsif:condition:path> {@link #subContent} contains build.script for any list element,</td></tr>
     * <tr><td>w</td><td>while(cond) {@link #subContent} contains build.script for any list element,</td></tr>
     * <tr><td>b</td><td>break</td></tr>
     * <tr><td>?</td><td><:if:...?gt> compare-operation in if</td></tr>
     * 
     * <tr><td>Z</td><td>a target,</td></tr>
     * <tr><td>Y</td><td>the file</td></tr>
     * <tr><td>xxxX</td><td>a subtext definition</td></tr>
     * </table> 
     */
    final public char elementType;    
    
    
    /**Any variable given by name or java instance  which is used to assign to it.
     * A variable is given by the start element of the data path. An instance is given by any more complex datapath
     * null if not used. */
    List<DataAccess> assignObj;
    
    //public String value;
    
    //public List<String> path;
    
    
    /**Argument list either actual or formal if this is a subtext call or subtext definition. 
     * Maybe null if the subtext has not argument. It is null if it is not a subtext call or definition. */
    List<Argument> arguments;
    
    /**The statements in this sub-ScriptElement were executed if an exception throws
     * or if a command line invocation returns an error level greater or equal the {@link Iferror#errorLevel}.
     * If it is null, no exception handling is done.
     * <br><br>
     * This block can contain any statements as error replacement. If they fails too,
     * the iferror-Block can contain an iferror too.
     * 
     */
    List<Onerror> onerror;
    
    

    
    public Statement(StatementList parentList, char whatisit, String text)
    { super(parentList);
      this.elementType = whatisit;
      this.textArg = text;
      if("BNXYZvl".indexOf(whatisit)>=0){
        subContent = new StatementList();
      }
      else if("IVL".indexOf(whatisit)>=0){
        subContent = new StatementList(this, true);
      }
    }
    
    
    
    public List<Argument> getReferenceDataSettings(){ return arguments; }
    
    public StatementList getSubContent(){ return subContent; }
    
    @Override
    public void set_name(String name){ this.identArgJbat = name; }
    
    
    public void set_formatText(String text){ this.textArg = text; }
    
    /**Gathers a text which is assigned to any variable or output. <+ name>text<.+>
     */
    public Statement new_textOut(){ return new Statement(parentList, 'T', null); }

    public void add_textOut(Statement val){ subContent.content.add(val); } //localVariableScripts.add(val); } 
    
    
    public void set_newline(){
      if(subContent == null){ subContent = new StatementList(this); }
      subContent.content.add(new Statement(parentList, 'n', null));   /// 
    }
    
    public Statement new_setEnvVar(){ 
      if(subContent == null){ subContent = new StatementList(this); }
      return subContent.new_setEnvVar(); 
    }

    public void add_setEnvVar(Statement val){ subContent.add_setEnvVar(val); } 
    
    
    /**Defines a variable with initial value. <= <variableAssign?textVariable> \<\.=\>
     */
    public Statement new_textVariable(){
      if(subContent == null){ subContent = new StatementList(this); }
      subContent.bContainsVariableDef = true; 
      return new Statement(parentList, 'S', null); 
    } 

    public void add_textVariable(Statement val){ subContent.content.add(val); subContent.onerrorAccu = null; subContent.withoutOnerror.add(val);} 
    
    
    /**Defines a variable which is able to use as pipe.
     */
    public Statement new_Pipe(){
      if(subContent == null){ subContent = new StatementList(this); }
      subContent.bContainsVariableDef = true; 
      return new Statement(parentList, 'P', null); 
    } 

    public void add_Pipe(Statement val){ subContent.content.add(val); subContent.onerrorAccu = null; subContent.withoutOnerror.add(val); }
    
    /**Defines a variable which is able to use as String buffer.
     */
    public Statement new_StringAppend(){
      if(subContent == null){ subContent = new StatementList(this); }
      subContent.bContainsVariableDef = true; 
      return new Statement(parentList, 'U', null); 
    } 

    public void add_StringAppend(Statement val){ subContent.content.add(val);  subContent.onerrorAccu = null; subContent.withoutOnerror.add(val);}
    
        
    /**Defines a variable which is able to use as container.
     */
    public Statement new_List(){
      if(subContent == null){ subContent = new StatementList(this); }
      subContent.bContainsVariableDef = true; 
      return new Statement(parentList, 'L', null); 
    } 

    public void add_List(Statement val){ subContent.content.add(val);  subContent.onerrorAccu = null; subContent.withoutOnerror.add(val);}
    
    /**Defines a variable which is able to use as pipe.
     */
    public Statement new_Openfile(){
      if(subContent == null){ subContent = new StatementList(this); }
      subContent.bContainsVariableDef = true; 
      return new Statement(parentList, 'W', null); 
    } 

    public void add_Openfile(Statement val){ 
      if(subContent == null){ subContent = new StatementList(this); }
      subContent.content.add(val);  
      subContent.onerrorAccu = null; 
      subContent.withoutOnerror.add(val);
    }
    
    /**Defines a variable with initial value. <= <$name> : <obj>> \<\.=\>
     */
    public Statement new_objVariable(){ 
      if(subContent == null){ subContent = new StatementList(this); }
      subContent.bContainsVariableDef = true; 
      return new Statement(parentList, 'J', null); 
    } 

    public void add_objVariable(Statement val){ subContent.content.add(val); subContent.onerrorAccu = null; subContent.withoutOnerror.add(val);}
    
    
    
    /**Set from ZBNF:  \<*subtext:name: { <namedArgument> ?,} \> */
    public Argument new_formalArgument(){ return new Argument(parentList); }
    
    /**Set from ZBNF:  \<*subtext:name: { <namedArgument> ?,} \> */
    public void add_formalArgument(Argument val){ 
      if(arguments == null){ arguments = new ArrayList<Argument>(); }
      arguments.add(val); }
    
    
    /**Set from ZBNF:  \<*subtext:name: { <namedArgument> ?,} \> */
    public Argument new_actualArgument(){ return new Argument(parentList); }
    
    /**Set from ZBNF:  \<*subtext:name: { <namedArgument> ?,} \> */
    public void add_actualArgument(Argument val){ 
      if(arguments == null){ arguments = new ArrayList<Argument>(); }
      arguments.add(val); }
    
    
    /**From Zbnf: [{ <datapath?-assign> = }] 
     */
    public DataAccess.DataAccessSet new_assign(){ return new DataAccess.DataAccessSet(); }
    
    public void add_assign(DataAccess.DataAccessSet val){ 
      if(assignObj == null){ assignObj = new LinkedList<DataAccess>(); }
      assignObj.add(val); 
    }

    
    public Statement new_assignment(){ 
      return new Statement(parentList, '=', null); 
    } 

    public void add_assignment(Statement val){ 
      if(subContent == null){ subContent = new StatementList(this); }
      subContent.content.add(val);  
      subContent.onerrorAccu = null; 
      subContent.withoutOnerror.add(val);
    }
    
    
    /**From ZBNF: <code>< value></code>.
     * @return A new {@link ZbnfValue} as syntax component
     */
    public XXXZbnfValue XXXnew_value(){ return new XXXZbnfValue(this); }
    
    /**From ZBNF: <code>< value></code>.
     * The val is added to the @{@link Argument#expression} of this {@link Statement}.
     * @param val
     */
    public void XXXadd_value(XXXZbnfValue val){ 
      if(expression == null){ expression = new Expression(this); }
      //expression.values.add(val); 
    }
  
    

    
    /**Set from ZBNF:  (\?*<$?dataText>\?) */
    //public ScriptElement new_valueVariable(){ return new ScriptElement('g', null); }
    
    /**Set from ZBNF:  (\?*<$?forElement>\?) */
    //public void add_valueVariable(ScriptElement val){ subContent.content.add(val);  subContent.onerrorAccu = null; subContent.withoutOnerror.add(val);}
    
    
    public Statement new_statementBlock(){
      if(subContent == null){ subContent = new StatementList(this); }
      Statement contentElement = new Statement(parentList, 'B', null);
      subContent.content.add(contentElement);
      subContent.onerrorAccu = null; subContent.withoutOnerror.add(contentElement);
      return contentElement;
    }
    
    public void add_statementBlock(Statement val){}

    
    public Onerror new_onerror(){
      return new Onerror(parentList);
    }
    

    public void add_onerror(Onerror val){
      if(subContent == null){ subContent = new StatementList(this); }
      if(subContent.onerrorAccu == null){ subContent.onerrorAccu = new LinkedList<Onerror>(); }
      for( Statement previousStatement: subContent.withoutOnerror){
        previousStatement.onerror = onerror;  
        //use the same onerror list for all previous statements without error designation.
      }
      subContent.withoutOnerror.clear();  //remove all entries, they are processed.
    }

    
    public void set_breakBlock(){ 
      if(subContent == null){ subContent = new StatementList(this); }
      Statement contentElement = new Statement(parentList, 'b', null);
      subContent.content.add(contentElement);
    }
    
 
      
    public Statement new_forContainer()
    { if(subContent == null) { subContent = new StatementList(this); }
      return subContent.new_forContainer();
    }
    
    public void add_forContainer(Statement val){subContent.add_forContainer(val);}

    
    public Statement new_whileBlock()
    { if(subContent == null) { subContent = new StatementList(this); }
      return subContent.new_whileBlock();
    }
    
    public void add_whileBlock(Statement val){subContent.add_whileBlock(val); }

    
    public Statement new_if()
    { StatementList subGenContent = new StatementList(this, true);
      Statement contentElement = new Statement(parentList, 'F', null);
      contentElement.subContent = subGenContent;  //The contentElement contains a genContent. 
      subContent.content.add(contentElement);
      subContent.onerrorAccu = null; subContent.withoutOnerror.add(contentElement);
      return contentElement;
    }
    
    public void add_if(Statement val){}

    
    public IfCondition new_ifBlock()
    { StatementList subGenContent = new StatementList(this, true);
      IfCondition contentElement = new IfCondition(parentList, 'G');
      contentElement.subContent = subGenContent;  //The contentElement contains a genContent. 
      subContent.content.add(contentElement);
      subContent.onerrorAccu = null; subContent.withoutOnerror.add(contentElement);
      return contentElement;
    }
    
    public void add_ifBlock(IfCondition val){}

    public Statement new_hasNext()
    { Statement contentElement = new Statement(parentList, 'N', null);
      subContent.content.add(contentElement);
      subContent.onerrorAccu = null; subContent.withoutOnerror.add(contentElement);
      return contentElement;
    }
    
    public void add_hasNext(Statement val){}

    public Statement new_elseBlock()
    { StatementList subGenContent = new StatementList(this, true);
      Statement contentElement = new Statement(parentList, 'E', null);
      contentElement.subContent = subGenContent;  //The contentElement contains a genContent. 
      subContent.content.add(contentElement);
      subContent.onerrorAccu = null; subContent.withoutOnerror.add(contentElement);
      return contentElement;
    }
    
    public void add_elseBlock(Statement val){}

    
    
    public CallStatement new_callSubtext()
    { if(subContent == null){ subContent = new StatementList(this); }
      CallStatement contentElement = new CallStatement(parentList);
      subContent.content.add(contentElement);
      subContent.onerrorAccu = null; subContent.withoutOnerror.add(contentElement);
      return contentElement;
    }
    
    public void add_callSubtext(CallStatement val){}

    

    public Statement new_cmdLine()
    { if(subContent == null){ subContent = new StatementList(this); }
      Statement contentElement = new Statement(parentList, 'c', null);
      subContent.content.add(contentElement);
      subContent.onerrorAccu = null; subContent.withoutOnerror.add(contentElement);
      return contentElement;
    }
    
    public void add_cmdLine(Statement val){}

    

    public void set_cd(String val)
    { if(subContent == null){ subContent = new StatementList(this); }
      subContent.set_cd(val);
    }
    
    

    
    
    /**Set from ZBNF:  (\?*<$?forElement>\?) */
    public void set_fnEmpty(String val){ 
      if(subContent == null){ subContent = new StatementList(this); }
      Statement contentElement = new Statement(parentList, 'f', val);
      subContent.content.add(contentElement);
      subContent.onerrorAccu = null; subContent.withoutOnerror.add(contentElement);
    }
    
    public void set_outputValue(String text){ 
      if(subContent == null){ subContent = new StatementList(this); }
      Statement contentElement = new Statement(parentList, 'o', text);
      subContent.content.add(contentElement); 
      subContent.onerrorAccu = null; subContent.withoutOnerror.add(contentElement);
    }
    
    public void set_inputValue(String text){ 
      if(subContent == null){ subContent = new StatementList(this); }
      Statement contentElement = new Statement(parentList, 'i', text);
      subContent.content.add(contentElement); 
      subContent.onerrorAccu = null; subContent.withoutOnerror.add(contentElement);
    }
    
    //public void set_variableValue(String text){ subContent.content.add(new ScriptElement('v', text)); }
    
    /**Set from ZBNF:  (\?*\?)<?listElement> */
    //public void set_listElement(){ subContent.content.add(new ScriptElement('e', null)); }
    
    public Statement new_forInputContent()
    { if(subContent == null){ subContent = new StatementList(this); }
      Statement contentElement = new Statement(parentList, 'I', null);
      subContent.content.add(contentElement);
      subContent.onerrorAccu = null; subContent.withoutOnerror.add(contentElement);
      return contentElement;
    }
    
    public void add_forInputContent(Statement val){}

    
    public Statement xxxnew_forVariable()
    { if(subContent == null){ subContent = new StatementList(this); }
      Statement contentElement = new Statement(parentList, 'V', null);
      subContent.content.add(contentElement);
      subContent.onerrorAccu = null; subContent.withoutOnerror.add(contentElement);
      return contentElement;
    }
    
    public void xxxadd_forVariable(Statement val){} //empty, it is added in new_forList()

    
    public Statement new_forList()
    { if(subContent == null){ subContent = new StatementList(this); }
      Statement contentElement = new Statement(parentList, 'L', null);
      subContent.content.add(contentElement);
      subContent.onerrorAccu = null; subContent.withoutOnerror.add(contentElement);
      return contentElement;
    }
    
    public void add_forList(Statement val){} //empty, it is added in new_forList()

    
    public Statement new_addToList(){ 
      Statement subGenContent = new Statement(parentList, 'l', null);
      subContent.addToList.add(subGenContent.subContent);
      return subGenContent;
    }
   
    public void add_addToList(Statement val)
    {
    }

    
    
    
    
    /**Set from ZBNF:  (\?*<$?forElement>\?) */
    public void axxxdd_fnEmpty(Statement val){  }
    

    
    @Override public String toString()
    {
      switch(elementType){
      case 't': return textArg;
      case 'S': return "String " + identArgJbat;
      case 'J': return "Obj " + identArgJbat;
      case 'P': return "Pipe " + identArgJbat;
      case 'U': return "Buffer " + identArgJbat;
      case 'o': return "(?outp." + textArg + "?)";
      case 'i': return "(?inp." + textArg + "?)";
      case 'e': return "<*" +   ">";  //expressions.get(0).datapath
      //case 'g': return "<$" + path + ">";
      case 's': return "call " + identArgJbat;
      case 'I': return "(?forInput?)...(/?)";
      case 'L': return "(?forList " + textArg + "?)";
      case 'C': return "<:for:Container " + textArg + "?)";
      case 'F': return "<:if:Container " + textArg + "?)";
      case 'G': return "<:elsif-condition " + textArg + "?)";
      case 'N': return "<:hasNext> content <.hasNext>";
      case 'E': return "<:else>";
      case 'Z': return "<:target:" + identArgJbat + ">";
      case 'Y': return "<:file>";
      case 'b': return "break;";
      case 'X': return "<:subtext:" + identArgJbat + ">";
      default: return "(??" + elementType + " " + textArg + "?)";
      }
    }
    
    
  }

  
  
  public static class CmdLine extends Statement
  {
    
    CmdLine(StatementList parentList){
      super(parentList, 'c', null);
    }
    
  };
  
  
  
  public static class CallStatement extends Statement
  {
    
    Argument callName;
    
    CallStatement(StatementList parentList){
      super(parentList, 's', null);
    }
    
    public Argument new_callName(){ return callName = new Argument(parentList); }
    
    public void set_callName(Argument val){}
    
  };
  
  
  
  public static class IfCondition extends Statement
  {
    
    Statement XXXcondition;
    
    boolean bElse;
    
    CalculatorExpr expr;
    
    IfCondition(StatementList parentList, char whatis){
      super(parentList, whatis, null);
    }
    
    public Statement XXXnew_cmpOperation()
    { XXXcondition = new Statement(parentList, '?', null);
      return XXXcondition;
    }
    
    public void add_cmpOperation(Statement val){
      Assert.stop();
      /*
      String text;
      if(val.expression !=null && val.expression.values !=null && val.expression.values.size()==1
        && (text = val.expression.values.get(0).stringValue()) !=null && text.equals("else") ){
        bElse = true;
      }
      */        
    }


    

  }
  
  
  
  
  
  
  
  /**This class contains expressions for error handling.
   * The statements in this sub-ScriptElement were executed if an exception throws
   * or if a command line invocation returns an error level greater or equal the {@link Iferror#errorLevel}.
   * If it is null, no exception handling is done.
   * <br><br>
   * This block can contain any statements as error replacement. If they fails too,
   * the iferror-Block can contain an iferror too.
   * 
 */
  public final static class Onerror extends Statement
  {
    /**From ZBNF */
    public int errorLevel;
    
    
    /**
     * <ul>
     * <li>'n' for notfound
     * <li>'f' file error
     * <li>'i' any internal exception.
     * </ul>
     * 
     */
    char errorType = '?';
    
    public void set_errortype(String type){
      errorType = type.charAt(0); //n, i, f
    }
 
    Onerror(StatementList parentList){
      super(parentList, 'B', null);
    }
 }
  
  
  
  
  
  /**Organization class for a list of script elements inside another Scriptelement.
   *
   */
  public static class StatementList
  {
    final Argument parentStatement;
    
    /**True if < genContent> is called for any input, (?:forInput?) */
    public final boolean isContentForInput;
    
    /**Set from ZBNF: */
    public boolean expandFiles;

    public String cmpnName;
    
    public final List<Statement> content = new ArrayList<Statement>();
    

    /**List of currently onerror statements.
     * This list is referenced in the appropriate {@link Statement#onerror} too. 
     * If an onerror statement will be gotten next, it is added to this list using this reference.
     * If another statement will be gotten next, this reference is cleared. So a new list will be created
     * for a later getting onerror statement. 
     */
    List<Onerror> onerrorAccu;

    
    List<Statement> withoutOnerror = new LinkedList<Statement>();
    
    
    /**True if the block {@link Argument#subContent} contains at least one variable definition.
     * In this case the execution of the ScriptElement as a block should be done with an separated set
     * of variables because new variables should not merge between existing of the outer block.
     */
    boolean bContainsVariableDef;

    
    /**Scripts for some local variable. This scripts where executed with current data on start of processing this genContent.
     * The generator stores the results in a Map<String, String> localVariable. 
     * 
     */
    //private final List<ScriptElement> localVariableScripts = new ArrayList<ScriptElement>();
    
    public final List<StatementList> addToList = new ArrayList<StatementList>();
    
    //public List<String> datapath = new ArrayList<String>();
    
    public StatementList()
    { this.parentStatement = null;
      this.isContentForInput = false;
    }
        
    public StatementList(Argument parentStatement)
    { this.parentStatement = parentStatement;
      this.isContentForInput = false;
    }
        
    public StatementList(Argument parentStatement, boolean isContentForInput)
    { this.parentStatement = parentStatement;
      this.isContentForInput = isContentForInput;
    }
        
    /**Defines a variable with initial value. <= <$name> : <obj>> \<\.=\>
     */
    public Statement new_setEnvVar(){ return new Statement(null, 'S', null); } ///

    public void add_setEnvVar(Statement val){ 
      val.identArgJbat = "$" + val.identArgJbat;
      content.add(val); 
      onerrorAccu = null; withoutOnerror.add(val);
    } 
    

    //public List<ScriptElement> getLocalVariables(){ return localVariableScripts; }
    
    /**Set from ZBNF:  (\?*<$?dataText>\?) */
    public Statement new_dataText(){ return new Statement(this, 'e', null); }
    
    /**Set from ZBNF:  (\?*<*dataText>\?) */
    public void add_dataText(Statement val){ 
      content.add(val);
      onerrorAccu = null; withoutOnerror.add(val);
    }
    
    public void set_text(String text){
      Statement contentElement = new Statement(this, 't', text);
      content.add(contentElement);
      onerrorAccu = null; withoutOnerror.add(contentElement);
    }
    
    
    public void set_nonEmptyText(String text){
      if(!StringFunctions.isEmptyOrOnlyWhitespaces(text)){
        Statement contentElement = new Statement(this, 't', text);
        content.add(contentElement);
        onerrorAccu = null; withoutOnerror.add(contentElement);
      }
    }
    
    
    public void set_newline(){
      Statement contentElement = new Statement(this, 'n', null);
      content.add(contentElement);
      onerrorAccu = null; withoutOnerror.add(contentElement);
    }
    

    public Statement new_forContainer()
    { Statement contentElement = new Statement(this, 'C', null);
      content.add(contentElement);
      onerrorAccu = null; withoutOnerror.add(contentElement);
      return contentElement;
    }
    
    public void add_forContainer(Statement val){}


    public Statement new_whileBlock()
    { Statement contentElement = new Statement(this, 'w', null);
      content.add(contentElement);
      onerrorAccu = null; withoutOnerror.add(contentElement);
      return contentElement;
    }
    
    public void add_whileBlock(Statement val){}


    public void set_cd(String val)
    { Statement contentElement = new Statement(this, 'd', null);
      contentElement.textArg = val;
      content.add(contentElement);
      onerrorAccu = null; withoutOnerror.add(contentElement);
    }
    
    
    
    public void set_name(String name){
      cmpnName = name;
    }
    
    public void XXXadd_datapath(String val)
    {
      //datapath.add(val);
    }

    
    @Override public String toString()
    { return "genContent name=" + cmpnName + ":" + content;
    }
  }
  
  
  
  
  /**Main class for ZBNF parse result.
   * This class has the enclosing class to store {@link ZbatchGenScript#subtextScripts}, {@link ZbatchGenScript#listScriptVariables}
   * etc. while parsing the script. The <code><:file>...<.file></code>-script is stored here locally
   * and used as the main file script only if it is the first one of main or included script. The same behaviour is used  
   * <pre>
   * ZmakeGenctrl::= { <target> } \e.
   * </pre>
   */
  public final class ZbnfMainGenCtrl extends StatementList
  {

    //public String scriptclass;
    
    List<String> includes;
    
    /**The script element for the whole file of this script. 
     * It is possible that it is from a included script.
     * It shall contain calling of <code><*subtext:name:...></code> 
     */
    Statement scriptFileSub;
    

    
    public void set_include(String val){ 
      if(includes ==null){ includes = new ArrayList<String>(); }
      includes.add(val); 
    }
    
    //public Statement new_ZmakeTarget(){ return new Statement(null, 'Z', null); }
    
    //public void add_ZmakeTarget(Statement val){ zmakeTargets.put(val.name, val); }
    
    
    public Statement new_subtext(){ return new Statement(null, 'X', null); }
    
    public void add_subtext(Statement val){ 
      if(val.identArgJbat == null){
        //scriptFileSub = new ScriptElement('Y', null); 
        
        val.identArgJbat = "main";
      }
      subtextScripts.put(val.identArgJbat, val); 
    }
    
    public Statement new_genFile(){ return scriptFileSub = new Statement(null, 'Y', null); }
    
    public void add_genFile(Statement val){  }
    
    /**Defines a variable with initial value. <= <variableDef?textVariable> \<\.=\>
     */
    //public Statement new_textVariable(){ return new Statement(null, 'V', null); }

    //public void add_textVariable(Statement val){ listScriptVariables.add(val); } 
    
    
    /**Defines a variable with initial value. <= <$name> : <obj>> \<\.=\>
     */
    //public Statement new_objVariable(){ return new Statement(null, 'J', null); } ///

    //public void add_objVariable(Statement val){ listScriptVariables.add(val); } 
    
    
    

  }
  


}
