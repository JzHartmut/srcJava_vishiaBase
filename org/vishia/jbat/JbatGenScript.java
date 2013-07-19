package org.vishia.jbat;

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
public class JbatGenScript {
  /**Version, history and license.
   * <ul>
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
   * <li>2012-12-23 Hartmut new: formatText in the {@link Expression#text} if a data path is given, use for formatting a numerical value.   
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
   * <li>2012-10-10 new: Some enhancements, it is used for {@link org.vishia.jbat.JbatExecuter} now too.
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

  /**Mirror of the content of the zmake-genctrl-file. Filled from ZBNF-ParseResult*/
  //ZbnfMainGenCtrl zTextGenCtrl;
  
  final Map<String, Statement> zmakeTargets = new TreeMap<String, Statement>();
  
  final Map<String, Statement> subtextScripts = new TreeMap<String, Statement>();
  
  
  
  final Map<String,Statement> indexScriptVariables = new TreeMap<String,Statement>();

  /**List of the script variables in order of creation in the zmakeCtrl-file.
   * The script variables can contain inputs of other variables which are defined before.
   * Therefore the order is important.
   */
  final List<Statement> listScriptVariables = new ArrayList<Statement>();

  /**The script element for the whole file. It shall contain calling of <code><*subtext:name:...></code> 
   */
  Statement scriptFile;
  

  public String scriptclassMain;

  public JbatGenScript(MainCmdLogging_ifc console)
  { this.console = console;
  }

  
  public boolean translateAndSetGenCtrl(File fileZbnf4GenCtrl, File fileGenCtrl, File checkXmlOut) 
  throws FileNotFoundException, IOException
    , ParseException, XmlException, IllegalArgumentException, IllegalAccessException, InstantiationException
  { console.writeInfoln("* Zmake: parsing gen script \"" + fileZbnf4GenCtrl.getAbsolutePath() 
    + "\" with \"" + fileGenCtrl.getAbsolutePath() + "\"");

    int lengthBufferSyntax = (int)fileZbnf4GenCtrl.length();
    StringPart spSyntax = new StringPartFromFileLines(fileZbnf4GenCtrl, lengthBufferSyntax, "encoding", null);

    int lengthBufferGenctrl = (int)fileGenCtrl.length();
    StringPart spGenCtrl = new StringPartFromFileLines(fileGenCtrl, lengthBufferGenctrl, "encoding", null);

    File fileParent = FileSystem.getDir(fileGenCtrl);
    return translateAndSetGenCtrl(new StringPart(JbatSyntax.syntax), new StringPart(spGenCtrl), checkXmlOut, fileParent) !=null;
  }
  

  public boolean translateAndSetGenCtrl(File fileGenCtrl) 
  throws FileNotFoundException, IllegalArgumentException, IllegalAccessException, InstantiationException, IOException, ParseException, XmlException 
  {
    return translateAndSetGenCtrl(fileGenCtrl, null);
  }
  
  
  public boolean translateAndSetGenCtrl(File fileGenCtrl, File checkXmlOut) 
  throws FileNotFoundException, IllegalArgumentException, IllegalAccessException, InstantiationException, IOException, ParseException, XmlException 
  {
    int lengthBufferGenctrl = (int)fileGenCtrl.length();
    StringPart spGenCtrl = new StringPartFromFileLines(fileGenCtrl, lengthBufferGenctrl, "encoding", null);
    File fileParent = FileSystem.getDir(fileGenCtrl);
    return translateAndSetGenCtrl(new StringPart(JbatSyntax.syntax), new StringPart(spGenCtrl), checkXmlOut, fileParent) !=null;
  }
  
  
  public boolean translateAndSetGenCtrl(String spGenCtrl) 
  throws IllegalArgumentException, IllegalAccessException, InstantiationException, ParseException 
  {
    try{ 
      return translateAndSetGenCtrl(new StringPart(JbatSyntax.syntax), new StringPart(spGenCtrl), null, null) !=null;
    } catch(IOException exc){ throw new UnexpectedException(exc); }
  }
  
  
  public boolean translateAndSetGenCtrl(StringPart spGenCtrl) 
  throws ParseException, IllegalArgumentException, IllegalAccessException, InstantiationException 
  {
    try { 
      return translateAndSetGenCtrl(new StringPart(JbatSyntax.syntax), spGenCtrl, null, null) !=null;
    }catch(IOException exc){ throw new UnexpectedException(exc); }
  }
  
  
  
  /**Translate the generation control file - core routine.
   * It sets the {@link #zTextGenCtrl} aggregation. This routine must be called before  the script can be used.
   * There are some routines without the parameter sZbnf4GenCtrl, which uses the internal syntax. Use those if possible:
   * {@link #translateAndSetGenCtrl(File)}, {@link #translateAndSetGenCtrl(String)}
   * <br><br>
   * This routine will be called recursively if scripts are included.
   * 
   * @param sZbnf4GenCtrl The syntax. This routine can use a special syntax. The default syntax is {@link JbatSyntax#syntax}.
   * @param spGenCtrl The input file with the genCtrl statements.
   * @param checkXmlOut If not null then writes the parse result to this file, only for check of the parse result.
   * @param fileParent directory of the used file as start directory for included scripts. 
   *   null possible, then the script should not contain includes.
   * @return a new instance of {@link ZbnfMainGenCtrl}. This instance is temporary only because it is a non-static 
   *   inner class of this. All substantial data are stored in this. Only the {@link ZbnfMainGenCtrl#scriptclass}
   *   and the {@link ZbnfMainGenCtrl#scriptFileSub} is read out and stored in @{@link #scriptclassMain} and {@link #scriptFile}
   *   if it is the first one given. This method returns null if there is an error. 
   * @throws ParseException
   * @throws IllegalArgumentException
   * @throws IllegalAccessException
   * @throws InstantiationException
   * @throws IOException only if xcheckXmlOutput fails
   * @throws FileNotFoundException if a included file was not found or if xcheckXmlOutput file not found or not writeable
   */
  public ZbnfMainGenCtrl translateAndSetGenCtrl(StringPart sZbnf4GenCtrl, StringPart spGenCtrl, File checkXmlOutput, File fileParent) 
  throws ParseException, IllegalArgumentException, IllegalAccessException, InstantiationException, FileNotFoundException, IOException 
  { boolean bOk;
    ZbnfParser parserGenCtrl = new ZbnfParser(console);
    parserGenCtrl.setSyntax(sZbnf4GenCtrl);
    if(console.getReportLevel() >= MainCmdLogging_ifc.fineInfo){
      console.reportln(MainCmdLogging_ifc.fineInfo, "== Syntax GenCtrl ==");
      parserGenCtrl.reportSyntax(console, MainCmdLogging_ifc.fineInfo);
    }
    console.writeInfo(" ... ");
    return translateAndSetGenCtrl(parserGenCtrl, spGenCtrl, checkXmlOutput, fileParent);
  }
    
    
    
    
  private ZbnfMainGenCtrl translateAndSetGenCtrl(ZbnfParser parserGenCtrl, StringPart spGenCtrl
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
    ZbnfJavaOutput parserGenCtrl2Java = new ZbnfJavaOutput(console);
    parserGenCtrl2Java.setContent(ZbnfMainGenCtrl.class, zbnfGenCtrl, parserGenCtrl.getFirstParseResult());
    if(scriptFile ==null){
      scriptFile = zbnfGenCtrl.scriptFileSub;   //use the first found <:file>, also from a included script
    }
    if(this.scriptclassMain ==null){
      this.scriptclassMain = zbnfGenCtrl.scriptclass;
    }
    if(zbnfGenCtrl.includes !=null){
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
    return zbnfGenCtrl;
  }
  
  
  /**Searches the Zmake-target by name (binary search. TreeMap.get(name).
   * @param name The name of given < ?translator> in the end-users script.
   * @return null if the Zmake-target is not found.
   */
  public final StatementList searchZmakeTaget(String name){ 
    Statement target = zmakeTargets.get(name);
    return target == null ? null : target.subContent;
  }
  
  
  public final String getScriptclass(){ return scriptclassMain; }
  
  public final Statement getFileScript(){ return scriptFile; }
  
  
  public Statement getSubtextScript(String name){ return subtextScripts.get(name); }
  
  public Statement xxxgetScriptVariable(String sName)
  {
    Statement content = indexScriptVariables.get(sName);
    return content;
  }
  
  
  
  public List<Statement> getListScriptVariables(){ return listScriptVariables; }



  public static class ZbnfDataPathElement extends DataAccess.DatapathElement
  {
    final Argument parentStatement;
    
    
    //List<ZbnfDataPathElement> actualArguments;
    
    List<Expression> actualValue;
    
    boolean bExtArgs;
    
    
    public ZbnfDataPathElement(Argument statement){
      this.parentStatement = statement;
    }
    
    
    /**Set if the arguments are listed outside of the element
     * 
     */
    public void set_extArgs(){
      if(actualValue == null){ actualValue = new ArrayList<Expression>(); }
      bExtArgs = true;
      //Expression actualArgument = new Expression();
      //actualValue.add(actualArgument);
    }
    
    
    public Expression new_argument(){
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
    public void add_argument(Expression val){ 
      if(actualValue == null){ actualValue = new ArrayList<Expression>(); }
      actualValue.add(val);
    } 
    
    public void set_javapath(String text){ this.ident = text; }
    


  }
  
  

  /**
  *
  */
  public static class Expression extends CalculatorExpr
  {
  
    final Argument parentStatement;
    
    /**If need, a sub-content, maybe null.*/
    public StatementList genString;
    
    List<ZbnfValue> XXXvalues = new ArrayList<ZbnfValue>();
  
  
    public Expression(Argument statement){
      this.parentStatement = statement;  
    }
    
    //public ZbnfValue new_value(){ return new ZbnfValue(parentStatement); }
    
    //public void add_value(ZbnfValue val){ values.add(val); }
  
    
    public ZbnfOperation new_startOperation(){ return new ZbnfOperation('!', parentStatement); }
    
    public void add_startOperation(ZbnfOperation val){ addToStack(val); }
  
    /**From Zbnf, a part <:>...<.> */
    public StatementList new_genString(){ return genString = new StatementList(); }
    
    public void add_genString(StatementList val){}
    

  }
  
  
  
  
  public static class ZbnfOperation extends CalculatorExpr.Operation
  {
    final Argument parentStatement;
    
    ZbnfOperation(char operator, Argument parentStatement){ 
      super(operator); 
      this.parentStatement = parentStatement;
    }

  
    /**Set a integer (long) argument of a access method. From Zbnf <#?intArg>. */
    public void set_intValue(long val){ this.value = new CalculatorExpr.Value(val); }
    
    /**Set a integer (long) argument of a access method. From Zbnf <#?intArg>. */
    public void set_floatValue(double val){ this.value = new CalculatorExpr.Value(val); }
    
    /**Set a integer (long) argument of a access method. From Zbnf <#?intArg>. */
    public void set_textValue(String val){ this.value = new CalculatorExpr.Value(val); }
    
    /**Set a integer (long) argument of a access method. From Zbnf <#?intArg>. */
    public void set_charValue(String val){ this.value = new CalculatorExpr.Value(val.charAt(0)); }
    
    /**ZBNF: <code>info ( < datapath? > <?datapath > )</code>.
     * 
     * @return this. The syntax supports only datapath elements. But the instance is the same.
     */
    public void set_datapath(){ 
      if(this.value == null){ this.value = new CalculatorExpr.Value(); }
    }
    
    /**ZBNF: <code>info ( < datapath? > <?info > )</code>.
     * 
     * @return this. The syntax supports only datapath elements. But the instance is the same.
     */
    public void set_info(){ 
      if(this.value == null){ this.value = new CalculatorExpr.Value(); }
      this.value.setInfoType();
    }
    
    public ZbnfDataPathElement new_datapathElement(){ return new ZbnfDataPathElement(parentStatement); }
    
    public void add_datapathElement(ZbnfDataPathElement val){ 
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
    
    
    public ZbnfDataPathElement new_newJavaClass()
    { ZbnfDataPathElement value = new ZbnfDataPathElement(parentStatement);
      value.whatisit = 'n';
      //ScriptElement contentElement = new ScriptElement('J', null); ///
      //subContent.content.add(contentElement);
      return value;
    }
    
    public void add_newJavaClass(ZbnfDataPathElement val) { add_datapathElement(val); }


    public ZbnfDataPathElement new_staticJavaMethod()
    { ZbnfDataPathElement value = new ZbnfDataPathElement(parentStatement);
      value.whatisit = 's';
      return value;
      //ScriptElement contentElement = new ScriptElement('j', null); ///
      //subContent.content.add(contentElement);
      //return contentElement;
    }
    
    public void add_staticJavaMethod(ZbnfDataPathElement val) { add_datapathElement(val); }



  
  }
  
  
  
  /**A Value of a expression or a left value. The syntax determines what is admissible.
   *
   */
  public static class ZbnfValue extends CalculatorExpr.Value {
    
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
    
    
    public ZbnfValue(Argument statement){ 
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
    
    public ZbnfDataPathElement new_datapathElement(){ return new ZbnfDataPathElement(parentStatement); }
    
    public void add_datapathElement(ZbnfDataPathElement val){ 
      if(datapath == null){
        datapath = new ArrayList<DataAccess.DatapathElement>();
      }
      datapath.add(val); 
    }
    
    public void set_envVariable(String ident){
      if(datapath == null){
        datapath = new ArrayList<DataAccess.DatapathElement>();
      }
      DataAccess.DatapathElement element = new DataAccess.DatapathElement();
      element.whatisit = 'e';
      element.ident = ident;
      datapath.add(element); 
    }
    

    public void set_startVariable(String ident){
      if(datapath == null){
        datapath = new ArrayList<DataAccess.DatapathElement>();
      }
      DataAccess.DatapathElement element = new DataAccess.DatapathElement();
      element.whatisit = 'v';
      element.ident = ident;
      datapath.add(element); 
    }
    
    
    public ZbnfDataPathElement new_newJavaClass()
    { ZbnfDataPathElement value = new ZbnfDataPathElement(parentStatement);
      value.whatisit = 'n';
      //ScriptElement contentElement = new ScriptElement('J', null); ///
      //subContent.content.add(contentElement);
      return value;
    }
    
    public void add_newJavaClass(ZbnfDataPathElement val) { add_datapathElement(val); }


    public ZbnfDataPathElement new_staticJavaMethod()
    { ZbnfDataPathElement value = new ZbnfDataPathElement(parentStatement);
      value.whatisit = 's';
      return value;
      //ScriptElement contentElement = new ScriptElement('j', null); ///
      //subContent.content.add(contentElement);
      //return contentElement;
    }
    
    public void add_staticJavaMethod(ZbnfDataPathElement val) { add_datapathElement(val); }


    /**From Zbnf, a part <:>...<.> */
    public StatementList new_genString(){ return genString = new StatementList(); }
    
    public void add_genString(StatementList val){}
    
    @Override public String toString(){ return "value"; }

  }
  

  
  
  
  
  
  /**Superclass for ScriptElement, but used independent for arguments.
   * @author hartmut
   *
   */
  public static class Argument{
    
    
    final StatementList parentList;
    
    /**Name of the argument. It is the key to assign calling argument values. */
    public String name;
   
    public Expression expression;
  
    /**From Zbnf <""?textInStatement>, constant text, null if not used. */
    public String text; 
    
    /**If need, a sub-content, maybe null.*/
    public StatementList subContent;
    
    public Argument(StatementList parentList){
      this.parentList = parentList;
    }
    
    
    public Expression new_expression(){ return new Expression(this); }
    
    public void add_expression(Expression val){ expression = val; }
    
    public void set_text(String text){
      if(subContent == null){ subContent = new StatementList(this); }
      subContent.content.add(new Statement(parentList, 't', text)); 
    }
    
    public void XXXset_textInStatement(String text){
      this.text = text;
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
   *
   */
  public static class Statement extends Argument
  {
    /**Designation what presents the element:
     * <table><tr><th>c</th><th>what is it</th></tr>
     * <tr><td>t</td><td>simple constant text</td></tr>
     * <tr><td>n</td><td>simple newline text</td></tr>
     * <tr><td>T</td><td>textual output to any variable or file</td></tr>
     * <tr><td>l</td><td>add to list</td></tr>
     * <tr><td>i</td><td>content of the input, {@link #text} describes the build-prescript, 
     *                   see {@link ZmakeGenerator#getPartsFromFilepath(org.vishia.zmake.ZmakeUserScript.UserFilepath, String)}</td></tr>
     * <tr><td>o</td><td>content of the output, {@link #text} describes the build-prescript, 
     *                   see {@link ZmakeGenerator#getPartsFromFilepath(org.vishia.zmake.ZmakeUserScript.UserFilepath, String)}</td></tr>
     * <tr><td>e</td><td>A datatext, from <*expression> or such.</td></tr>
     * <tr><td>XXXg</td><td>content of a data path starting with an internal variable (reference) or value of the variable.</td></tr>
     * <tr><td>s</td><td>call of a subtext by name. {@link #text}==null, {@link #subContent} == null.</td></tr>
     * <tr><td>j</td><td>call of a static java method. {@link #name}==its name, {@link #subContent} == null.</td></tr>
     * <tr><td>c</td><td>cmd line invocation.</td></tr>
     * <tr><td>V</td><td>A variable, {@link #text} contains the name of the variable</td></tr>
     * <tr><td>J</td><td>Object variable {@link #name}==its name, {@link #subContent} == null.</td></tr>
     * <tr><td>P</td><td>Pipe variable, {@link #text} contains the name of the variable</td></tr>
     * <tr><td>U</td><td>Buffer variable, {@link #text} contains the name of the variable</td></tr>
     * <tr><td>S</td><td>String variable, {@link #text} contains the name of the variable</td></tr>
     * <tr><td>L</td><td>Container variable, a list</td></tr>
     * <tr><td>W</td><td>Opened file, a Writer in Java</td></tr>
     * <tr><td>=</td><td>assignment of an expression to a variable.</td></tr>
     * <tr><td>B</td><td>statement block</td></tr>
     * <tr><td>C</td><td><:for:path> {@link #subContent} contains build.script for any list element,</td></tr>
     * <tr><td>E</td><td><:else> {@link #subContent} contains build.script for any list element,</td></tr>
     * <tr><td>F</td><td><:if:condition:path> {@link #subContent} contains build.script for any list element,</td></tr>
     * <tr><td>G</td><td><:elsif:condition:path> {@link #subContent} contains build.script for any list element,</td></tr>
     * <tr><td>b</td><td>break</td></tr>
     * <tr><td>?</td><td><:if:...?gt> compare-operation in if</td></tr>
     * 
     * <tr><td>Z</td><td>a target,</td></tr>
     * <tr><td>Y</td><td>the file</td></tr>
     * <tr><td>xxxX</td><td>a subtext definition</td></tr>
     * </table> 
     */
    final public char elementType;    
    
    
    /**Any variable name of a script variable where the content should assigned to.
     * null if not used. */
    //String sVariableToAssign;
    List<ZbnfValue> assignObj;
    
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
      this.text = text;
      if("BNXYZvlJ".indexOf(whatisit)>=0){
        subContent = new StatementList();
      }
      else if("IVL".indexOf(whatisit)>=0){
        subContent = new StatementList(this, true);
      }
    }
    
    
    
    public List<Argument> getReferenceDataSettings(){ return arguments; }
    
    public StatementList getSubContent(){ return subContent; }
    
    public void set_name(String name){ this.name = name; }
    
    
    public void set_formatText(String text){ this.text = text; }
    
    /**Gathers a text which is assigned to any variable or output. <+ name>text<.+>
     */
    public Statement new_textOut(){ return new Statement(parentList, 'T', null); }

    public void add_textOut(Statement val){ subContent.content.add(val); } //localVariableScripts.add(val); } 
    
    
    public void set_newline(){
      if(subContent == null){ subContent = new StatementList(this); }
      subContent.content.add(new Statement(parentList, 'n', null));   /// 
    }
    
    
    /**Defines a variable with initial value. <= <variableAssign?textVariable> \<\.=\>
     */
    public Statement new_textVariable(){
      if(subContent == null){ subContent = new StatementList(this); }
      subContent.bContainsVariableDef = true; 
      return new Statement(parentList, 'V', null); 
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
    
    /**Defines a variable which is able to use as pipe.
     */
    public Statement new_Stringb(){
      if(subContent == null){ subContent = new StatementList(this); }
      subContent.bContainsVariableDef = true; 
      return new Statement(parentList, 'U', null); 
    } 

    public void add_Stringb(Statement val){ subContent.content.add(val);  subContent.onerrorAccu = null; subContent.withoutOnerror.add(val);}
    
    /**Defines a variable which is able to use as pipe.
     */
    public Statement new_String(){
      if(subContent == null){ subContent = new StatementList(this); }
      subContent.bContainsVariableDef = true; 
      return new Statement(parentList, 'S', null); 
    } 

    public void add_String(Statement val){ subContent.content.add(val);  subContent.onerrorAccu = null; subContent.withoutOnerror.add(val);}
    
    /**Defines a variable which is able to use as pipe.
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
    public ZbnfValue new_assign(){ return new ZbnfValue(this); }
    
    public void add_assign(ZbnfValue val){ 
      if(assignObj == null){ assignObj = new LinkedList<ZbnfValue>(); }
      assignObj.add(val); 
    }

    
    public Statement new_assignment(){ 
      return new Statement(parentList, '=', null); 
    } 

    public void add_assignment(Statement val){ 
      subContent.content.add(val);  
      subContent.onerrorAccu = null; 
      subContent.withoutOnerror.add(val);
    }
    
    
    /**From ZBNF: <code>< value></code>.
     * @return A new {@link ZbnfValue} as syntax component
     */
    public ZbnfValue XXXnew_value(){ return new ZbnfValue(this); }
    
    /**From ZBNF: <code>< value></code>.
     * The val is added to the @{@link Argument#expression} of this {@link Statement}.
     * @param val
     */
    public void XXXadd_value(ZbnfValue val){ 
      if(expression == null){ expression = new Expression(this); }
      //expression.values.add(val); 
    }
  
    

    
    /**Set from ZBNF:  (\?*<$?dataText>\?) */
    //public ScriptElement new_valueVariable(){ return new ScriptElement('g', null); }
    
    /**Set from ZBNF:  (\?*<$?forElement>\?) */
    //public void add_valueVariable(ScriptElement val){ subContent.content.add(val);  subContent.onerrorAccu = null; subContent.withoutOnerror.add(val);}
    
    
    public Statement new_statementBlock(){
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
      Statement contentElement = new Statement(parentList, 'b', null);
      subContent.content.add(contentElement);
    }
    
 
      
    public Statement new_forContainer()
    { StatementList subGenContent = new StatementList(this, true);
      Statement contentElement = new Statement(parentList, 'C', null);
      contentElement.subContent = subGenContent;  //The contentElement contains a genContent. 
      subContent.content.add(contentElement);
      subContent.onerrorAccu = null; subContent.withoutOnerror.add(contentElement);
      return contentElement;
    }
    
    public void add_forContainer(Statement val){}

    
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

    
    
    public Statement new_callSubtext()
    { Statement contentElement = new Statement(parentList, 's', null);
      subContent.content.add(contentElement);
      subContent.onerrorAccu = null; subContent.withoutOnerror.add(contentElement);
      return contentElement;
    }
    
    public void add_callSubtext(Statement val){}

    

    public Statement new_cmdLine()
    { Statement contentElement = new Statement(parentList, 'c', null);
      subContent.content.add(contentElement);
      subContent.onerrorAccu = null; subContent.withoutOnerror.add(contentElement);
      return contentElement;
    }
    
    public void add_cmdLine(Statement val){}

    

    
    
    /**Set from ZBNF:  (\?*<$?forElement>\?) */
    public void set_fnEmpty(String val){ 
      Statement contentElement = new Statement(parentList, 'f', val);
      subContent.content.add(contentElement);
      subContent.onerrorAccu = null; subContent.withoutOnerror.add(contentElement);
    }
    
    public void set_outputValue(String text){ 
      Statement contentElement = new Statement(parentList, 'o', text);
      subContent.content.add(contentElement); 
      subContent.onerrorAccu = null; subContent.withoutOnerror.add(contentElement);
    }
    
    public void set_inputValue(String text){ 
      Statement contentElement = new Statement(parentList, 'i', text);
      subContent.content.add(contentElement); 
      subContent.onerrorAccu = null; subContent.withoutOnerror.add(contentElement);
    }
    
    //public void set_variableValue(String text){ subContent.content.add(new ScriptElement('v', text)); }
    
    /**Set from ZBNF:  (\?*\?)<?listElement> */
    //public void set_listElement(){ subContent.content.add(new ScriptElement('e', null)); }
    
    public Statement new_forInputContent()
    { Statement contentElement = new Statement(parentList, 'I', null);
      subContent.content.add(contentElement);
      subContent.onerrorAccu = null; subContent.withoutOnerror.add(contentElement);
      return contentElement;
    }
    
    public void add_forInputContent(Statement val){}

    
    public Statement xxxnew_forVariable()
    { Statement contentElement = new Statement(parentList, 'V', null);
      subContent.content.add(contentElement);
      subContent.onerrorAccu = null; subContent.withoutOnerror.add(contentElement);
      return contentElement;
    }
    
    public void xxxadd_forVariable(Statement val){} //empty, it is added in new_forList()

    
    public Statement new_forList()
    { Statement contentElement = new Statement(parentList, 'L', null);
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
      case 't': return text;
      case 'V': return "<=" + name + ">";
      case 'J': return "<=" + name + ":objVariable>";
      case 'P': return "Pipe " + name;
      case 'U': return "Buffer " + name;
      case 'o': return "(?outp." + text + "?)";
      case 'i': return "(?inp." + text + "?)";
      case 'e': return "<*" +   ">";  //expressions.get(0).datapath
      //case 'g': return "<$" + path + ">";
      case 's': return "<*subtext:" + name + ">";
      case 'I': return "(?forInput?)...(/?)";
      case 'L': return "(?forList " + text + "?)";
      case 'C': return "<:for:Container " + text + "?)";
      case 'F': return "<:if:Container " + text + "?)";
      case 'G': return "<:elsif-condition " + text + "?)";
      case 'N': return "<:hasNext> content <.hasNext>";
      case 'E': return "<:else>";
      case 'Z': return "<:target:" + name + ">";
      case 'Y': return "<:file>";
      case 'b': return "break;";
      case 'X': return "<:subtext:" + name + ">";
      default: return "(??" + elementType + " " + text + "?)";
      }
    }
    
    
  }

  
  
  public static class CmdLine extends Statement
  {
    
    CmdLine(StatementList parentList){
      super(parentList, 'c', null);
    }
    
  };
  
  
  
  public static class IfCondition extends Statement
  {
    
    Statement condition;
    
    boolean bElse;
    
    CalculatorExpr expr;
    
    IfCondition(StatementList parentList, char whatis){
      super(parentList, whatis, null);
    }
    
    public Statement new_cmpOperation()
    { condition = new Statement(parentList, '?', null);
      return condition;
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
  public final static class StatementList
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
        
    
    //public List<ScriptElement> getLocalVariables(){ return localVariableScripts; }
    
    /**Set from ZBNF:  (\?*<$?dataText>\?) */
    public Statement new_dataText(){ return new Statement(this, 'e', null); }
    
    /**Set from ZBNF:  (\?*<*dataText>\?) */
    public void add_dataText(Statement val){ 
      content.add(val);
      withoutOnerror.add(val);
    }
    
    public void set_text(String text){
      content.add(new Statement(this, 't', text));
    }
    
    
    public void set_newline(){
      content.add(new Statement(this, 'n', null));   /// 
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
   * This class has the enclosing class to store {@link JbatGenScript#subtextScripts}, {@link JbatGenScript#listScriptVariables}
   * etc. while parsing the script. The <code><:file>...<.file></code>-script is stored here locally
   * and used as the main file script only if it is the first one of main or included script. The same behaviour is used  
   * <pre>
   * ZmakeGenctrl::= { <target> } \e.
   * </pre>
   */
  public final class ZbnfMainGenCtrl
  {

    public String scriptclass;
    
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
    
    public Statement new_ZmakeTarget(){ return new Statement(null, 'Z', null); }
    
    public void add_ZmakeTarget(Statement val){ zmakeTargets.put(val.name, val); }
    
    
    public Statement new_subtext(){ return new Statement(null, 'X', null); }
    
    public void add_subtext(Statement val){ 
      if(val.name == null){
        //scriptFileSub = new ScriptElement('Y', null); 
        
        val.name = "main";
      }
      subtextScripts.put(val.name, val); 
    }
    
    public Statement new_genFile(){ return scriptFileSub = new Statement(null, 'Y', null); }
    
    public void add_genFile(Statement val){  }
    
    /**Defines a variable with initial value. <= <variableDef?textVariable> \<\.=\>
     */
    public Statement new_textVariable(){ return new Statement(null, 'V', null); }

    public void add_textVariable(Statement val){ listScriptVariables.add(val); } 
    
    
    /**Defines a variable with initial value. <= <$name> : <obj>> \<\.=\>
     */
    public Statement new_objVariable(){ return new Statement(null, 'J', null); } ///

    public void add_objVariable(Statement val){ listScriptVariables.add(val); } 
    
    
    public Statement new_XXXsetVariable(){ return new Statement(null, 'V', null); }

    public void add_XXXsetVariable(Statement val)
    { indexScriptVariables.put(val.name, val); 
      listScriptVariables.add(val);
    } 
    

  }
  


}
