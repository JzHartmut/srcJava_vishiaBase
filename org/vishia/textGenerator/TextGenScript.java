package org.vishia.textGenerator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.vishia.mainCmd.MainCmdLogging_ifc;
import org.vishia.mainCmd.Report;
import org.vishia.util.CalculatorExpr;
import org.vishia.util.DataAccess;
import org.vishia.util.StringPart;
import org.vishia.util.StringPartFromFileLines;
import org.vishia.xmlSimple.XmlException;
import org.vishia.zbnf.ZbnfJavaOutput;
import org.vishia.zbnf.ZbnfParser;

/**This class contains control data and sub-routines to generate output texts from internal data.
 * 
 * @author Hartmut Schorrig
 *
 */
public class TextGenScript {
  /**Version, history and license.
   * <ul>
   * <li>2012-12-24 Hartmut chg: Now the 'ReferencedData' are 'namedArgument' and it uses 'dataAccess' inside. 
   *   The 'dataAccess' is represented by a new {@link ScriptElement}('e',...) which can have {@link Argument#constValue} 
   *   instead a {@link Argument#datapath}. 
   * <li>2012-12-24 Hartmut chg: {@link ZbnfDataPathElement} is a derived class of {@link DataAccess.DatapathElement}
   *   which contains destinations for argument parsing of a called Java-subroutine in a dataPath.  
   * <li>2012-12-23 Hartmut chg: A {@link ScriptElement} and a {@link Argument} have the same usage aspects for arguments
   *   which represents values either as constants or dataPath. Use Argument as super class for ScriptElement.
   * <li>2012-12-23 Hartmut new: formatText in the {@link Argument#text} if a data path is given, use for formatting a numerical value.   
   * <li>2012-12-22 Hartmut new: Syntax as constant string inside. Some enhancements to set control: {@link #setGenCtrl(StringPart)} etc.
   * <li>2012-12-22 Hartmut chg: <:if:...> uses {@link CalculatorExpr} for expressions.
   * <li>2012-11-24 Hartmut chg: @{@link ScriptElement#datapath} with {@link DataAccess.DatapathElement} 
   * <li>2012-11-25 Hartmut chg: Now Variables are designated starting with $.
   * <li>2012-10-19 Hartmut chg: <:if...> works.
   * <li>2012-10-19 Hartmut chg: Renaming: {@link ScriptElement} instead Zbnf_ScriptElement (shorter). The Scriptelement
   *   is the component for the genContent-Elements now instead Zbnf_genContent. This class contains attributes of the
   *   content elements. Only if a sub content is need, an instance of Zbnf_genContent is created as {@link ScriptElement#subContent}.
   *   Furthermore the {@link ScriptElement#subContent} should be final because it is only created if need for the special 
   *   {@link ScriptElement#whatisit}-types (TODO). This version works for {@link org.vishia.stateMGen.StateMGen}.
   * <li>2012-10-11 Hartmut chg Syntax changed of ZmakeGenCtrl.zbnf: datapath::={ <$?path>? \.}. 
   *   instead dataAccess::=<$?name>\.<$?elementPart>., it is more universal. adapted. 
   * <li>2012-10-10 new: Some enhancements, it is used for {@link org.vishia.textGenerator.TextGenerator} now too.
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
  static final public int version = 20121130;

  private final MainCmdLogging_ifc console;

  /**Mirror of the content of the zmake-genctrl-file. Filled from ZBNF-ParseResult*/
  MainGenCtrl zbnfZmakeGenCtrl;
  
  private final Map<String, ScriptElement> zmakeTargets = new TreeMap<String, ScriptElement>();
  
  private final Map<String, ScriptElement> subtexts = new TreeMap<String, ScriptElement>();
  
  
  
  private final Map<String,ScriptElement> indexScriptVariables = new TreeMap<String,ScriptElement>();

  /**List of the script variables in order of creation in the zmakeCtrl-file.
   * The script variables can contain inputs of other variables which are defined before.
   * Therefore the order is important.
   */
  private final List<ScriptElement> listScriptVariables = new LinkedList<ScriptElement>();

  private ScriptElement zbnf_genFile;
  
  public TextGenScript(MainCmdLogging_ifc console)
  { this.console = console;
  }

  
  public boolean XXXparseGenCtrl(File fileZbnf4GenCtrl, File fileGenCtrl) 
  throws FileNotFoundException, IOException
    , ParseException, XmlException, IllegalArgumentException, IllegalAccessException, InstantiationException
  { console.writeInfoln("* Zmake: parsing gen script \"" + fileZbnf4GenCtrl.getAbsolutePath() 
    + "\" with \"" + fileGenCtrl.getAbsolutePath() + "\"");

    int lengthBufferSyntax = (int)fileZbnf4GenCtrl.length();
    StringPart spSyntax = new StringPartFromFileLines(fileZbnf4GenCtrl, lengthBufferSyntax, "encoding", null);

    int lengthBufferGenctrl = (int)fileGenCtrl.length();
    StringPart spGenCtrl = new StringPartFromFileLines(fileGenCtrl, lengthBufferGenctrl, "encoding", null);

    
    return parseGenCtrl(spSyntax, spGenCtrl);
    
  }
  

  public boolean setGenCtrl(File fileGenCtrl) 
  throws FileNotFoundException, IllegalArgumentException, IllegalAccessException, InstantiationException, IOException, ParseException, XmlException 
  {
    int lengthBufferGenctrl = (int)fileGenCtrl.length();
    StringPart spGenCtrl = new StringPartFromFileLines(fileGenCtrl, lengthBufferGenctrl, "encoding", null);
    return parseGenCtrl(new StringPart(TextGenSyntax.syntax), new StringPart(spGenCtrl));
  }
  
  
  public boolean setGenCtrl(String spGenCtrl) 
  throws FileNotFoundException, IllegalArgumentException, IllegalAccessException, InstantiationException, IOException, ParseException, XmlException 
  {
    return parseGenCtrl(new StringPart(TextGenSyntax.syntax), new StringPart(spGenCtrl));
  }
  
  
  public boolean setGenCtrl(StringPart spGenCtrl) 
  throws ParseException, IllegalArgumentException, IllegalAccessException, InstantiationException 
  {
    return parseGenCtrl(new StringPart(TextGenSyntax.syntax), spGenCtrl);
  }
  
  
  
  public boolean parseGenCtrl(StringPart sZbnf4GenCtrl, StringPart spGenCtrl) 
  throws ParseException, IllegalArgumentException, IllegalAccessException, InstantiationException 
  { boolean bOk;
    ZbnfParser parserGenCtrl = new ZbnfParser((Report)console);
    parserGenCtrl.setSyntax(sZbnf4GenCtrl);
    if(console.getReportLevel() >= MainCmdLogging_ifc.fineInfo){
      console.reportln(MainCmdLogging_ifc.fineInfo, "== Syntax GenCtrl ==");
      parserGenCtrl.reportSyntax((Report)console, MainCmdLogging_ifc.fineInfo);
    }
    console.writeInfo(" ... ");
    bOk = parserGenCtrl.parse(spGenCtrl);
    if(!bOk){
      String sError = parserGenCtrl.getSyntaxErrorReport();
      throw new ParseException(sError,0);
    }
    if(console.getReportLevel() >= MainCmdLogging_ifc.fineInfo){
      parserGenCtrl.reportStore((Report)console, MainCmdLogging_ifc.fineInfo, "Zmake-GenScript");
    }
    console.writeInfo(", ok set output ... ");
    //write into Java classes:
    zbnfZmakeGenCtrl = new MainGenCtrl();
    ZbnfJavaOutput parserGenCtrl2Java = new ZbnfJavaOutput((Report)console);
    parserGenCtrl2Java.setContent(zbnfZmakeGenCtrl.getClass(), zbnfZmakeGenCtrl, parserGenCtrl.getFirstParseResult());
    console.writeInfo(" ok");
    return bOk;
  }
  
  
  /**Searches the Zmake-target by name (binary search. TreeMap.get(name).
   * @param name The name of given < ?translator> in the end-users script.
   * @return null if the Zmake-target is not found.
   */
  public final GenContent searchZmakeTaget(String name){ 
    ScriptElement target = zmakeTargets.get(name);
    return target == null ? null : target.subContent;
  }
  
  
  public final ScriptElement getFileScript(){ return zbnf_genFile; }
  
  
  public ScriptElement getSubtextScript(String name){ return subtexts.get(name); }
  
  public ScriptElement xxxgetScriptVariable(String sName)
  {
    ScriptElement content = indexScriptVariables.get(sName);
    return content;
  }
  
  
  
  public List<ScriptElement> getListScriptVariables(){ return listScriptVariables; }



  public static class ZbnfDataPathElement extends DataAccess.DatapathElement
  {
    List<ScriptElement> actualArguments;
    
    public ScriptElement new_actualArgument(){
      ScriptElement actualArgument = new ScriptElement('e', null);
      return actualArgument;
    }

    
    /**From Zbnf.
     * The Arguments of type {@link ScriptElement} have to be resolved by evaluating its value in the data context. 
     * The value is stored in {@link DataAccess.DatapathElement#addActualArgument(Object)}.
     * See {@link #add_datapathElement(org.vishia.util.DataAccess.DatapathElement)}.
     * @param val The Scriptelement which describes how to get the value.
     */
    public void add_actualArgument(ScriptElement val){ 
      if(actualArguments == null){ actualArguments = new LinkedList<ScriptElement>(); }
      actualArguments.add(val);
    } 
    
    public void set_javapath(String text){ this.ident = text; }
    


  }
  
  
  
  public static class Argument{
    
    /**Name of the argument. It is the key to assign calling argument values. */
    public String name;
    
    /**From Zbnf <""?text>, constant text, null if not used. */
    public String text; 
    
    /**Maybe a constant value, also a String. */
    public Object constValue;

    /**The description of the path to any data if the script-element refers data. It is null if the script element
     * does not refer data. If it is filled, the instances are of type {@link ZbnfDataPathElement}.
     * If it is used in {@link DataAccess}, its base class {@link DataAccess.DatapathElement} are used. The difference
     * are the handling of actual values for method calls. See {@link ZbnfDataPathElement#actualArguments}.
     */
    List<DataAccess.DatapathElement> datapath;
    
    public ZbnfDataPathElement new_datapathElement(){ return new ZbnfDataPathElement(); }
    
    public void add_datapathElement(ZbnfDataPathElement val){ 
      if(datapath == null){
        datapath = new LinkedList<DataAccess.DatapathElement>();
      }
      datapath.add(val); 
    }
    
    
    
    
    /**Set a integer (long) argument of a access method. From Zbnf <#?intArg>. */
    public void set_intValue(long val){ constValue = new Long(val); }
    
    /**Set a integer (long) argument of a access method. From Zbnf <#?intArg>. */
    public void set_floatValue(double val){ constValue = new Double(val); }
    
    /**Set a integer (long) argument of a access method. From Zbnf <#?intArg>. */
    public void set_textValue(String val){ constValue = val; }
    
    /**Set a integer (long) argument of a access method. From Zbnf <#?intArg>. */
    public void set_charValue(String val){ constValue = new Character(val.charAt(0)); }
    
    public ZbnfDataPathElement new_newJavaClass()
    { ZbnfDataPathElement value = new ZbnfDataPathElement();
      value.whatisit = 'n';
      //ScriptElement contentElement = new ScriptElement('J', null); ///
      //subContent.content.add(contentElement);
      return value;
    }
    
    public void add_newJavaClass(ZbnfDataPathElement val) { add_datapathElement(val); }


    public ZbnfDataPathElement new_staticJavaMethod()
    { ZbnfDataPathElement value = new ZbnfDataPathElement();
      value.whatisit = 's';
      return value;
      //ScriptElement contentElement = new ScriptElement('j', null); ///
      //subContent.content.add(contentElement);
      //return contentElement;
    }
    
    public void add_staticJavaMethod(ZbnfDataPathElement val) { add_datapathElement(val); }



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
  public static class ScriptElement extends Argument
  {
    /**Designation what presents the element:
     * <table><tr><th>c</th><th>what is it</th></tr>
     * <tr><td>t</td><td>simple constant text</td></tr>
     * <tr><td>v</td><td>content of a variable, {@link #text} contains the name of the variable</td></tr>
     * <tr><td>l</td><td>add to list</td></tr>
     * <tr><td>i</td><td>content of the input, {@link #text} describes the build-prescript, 
     *                   see {@link ZmakeGenerator#getPartsFromFilepath(org.vishia.zmake.ZmakeUserScript.UserFilepath, String)}</td></tr>
     * <tr><td>o</td><td>content of the output, {@link #text} describes the build-prescript, 
     *                   see {@link ZmakeGenerator#getPartsFromFilepath(org.vishia.zmake.ZmakeUserScript.UserFilepath, String)}</td></tr>
     * <tr><td>e</td><td>A value, maybe content of a data path or a constant value.</td></tr>
     * <tr><td>XXXg</td><td>content of a data path starting with an internal variable (reference) or value of the variable.</td></tr>
     * <tr><td>s</td><td>call of a subtext by name. {@link #text}==null, {@link #subContent} == null.</td></tr>
     * <tr><td>j</td><td>call of a static java method. {@link #name}==its name, {@link #subContent} == null.</td></tr>
     * <tr><td>J</td><td>creation of a java class. {@link #name}==its name, {@link #subContent} == null.</td></tr>
     * <tr><td>I</td><td>(?:forInput?): {@link #subContent} contains build.script for any input element</td></tr>
     * <tr><td>V</td><td>(?:for:variable?): {@link #subContent} contains build.script for any element of the named global variable or calling parameter</td></tr>
     * <tr><td>L</td><td>(?:forList?): {@link #subContent} contains build.script for any list element,
     *                   whereby subContent.{@link GenContent#name} is the name of the list. </td></tr>
     * <tr><td>C</td><td><:for:path> {@link #subContent} contains build.script for any list element,
     * <tr><td>E</td><td><:else> {@link #subContent} contains build.script for any list element,
     * <tr><td>F</td><td><:if:condition:path> {@link #subContent} contains build.script for any list element,
     * <tr><td>G</td><td><:elsif:condition:path> {@link #subContent} contains build.script for any list element,
     * <tr><td>?</td><td><:if:...?gt> compare-operation in if
     * 
     * <tr><td>Z</td><td>a target,
     * <tr><td>Y</td><td>the file
     * <tr><td>X</td><td>a subtext definition
     * </table> 
     */
    final public char whatisit;    
    
    //public String value;
    
    //public List<String> path;
    
    
    /**Argument list either actual or formal if this is a subtext call or subtext definition. 
     * Maybe null if the subtext has not argument. It is null if it is not a subtext call or definition. */
    List<Argument> arguments;
    
    //public String elementPart;
    
    /**If need, a sub-content, maybe null. TODO should be final*/
    public GenContent subContent;
    
    public ScriptElement(char whatisit, String text)
    { this.whatisit = whatisit;
      this.text = text;
      if("NXYZvl".indexOf(whatisit)>=0){
        subContent = new GenContent(false);
      }
      else if("IVL".indexOf(whatisit)>=0){
        subContent = new GenContent(true);
      }
    }
    
    
    
    public List<Argument> getReferenceDataSettings(){ return arguments; }
    
    public GenContent getSubContent(){ return subContent; }
    
    public void set_name(String name){ this.name = name; }
    
    public void set_text(String text){ subContent.content.add(new ScriptElement('t', text)); }
    
    
    public void set_formatText(String text){ this.text = text; }
    
    /**Defines a variable with initial value. <= <variableAssign?textVariable> \<\.=\>
     */
    public ScriptElement new_textVariable(){ return new ScriptElement('v', null); }

    public void add_textVariable(ScriptElement val){ subContent.localVariableScripts.add(val); } 
    
    
    /**Defines a variable with initial value. <= <$name> : <obj>> \<\.=\>
     */
    public ScriptElement new_objVariable(){ return new ScriptElement('J', null); } ///

    public void add_objVariable(ScriptElement val){ subContent.localVariableScripts.add(val); } 
    
    
    
    /**Set from ZBNF:  \<*subtext:name: { <namedArgument> ?,} \> */
    public Argument new_formalArgument(){ return new Argument(); }
    
    /**Set from ZBNF:  \<*subtext:name: { <namedArgument> ?,} \> */
    public void add_formalArgument(Argument val){ 
      if(arguments == null){ arguments = new LinkedList<Argument>(); }
      arguments.add(val); }
    
    
    /**Set from ZBNF:  \<*subtext:name: { <namedArgument> ?,} \> */
    public Argument new_actualArgument(){ return new Argument(); }
    
    /**Set from ZBNF:  \<*subtext:name: { <namedArgument> ?,} \> */
    public void add_actualArgument(Argument val){ 
      if(arguments == null){ arguments = new LinkedList<Argument>(); }
      arguments.add(val); }
    
    
    
    /**Set from ZBNF:  (\?*<$?dataAccess>\?) */
    public ScriptElement new_dataText(){ return new ScriptElement('e', null); }
    
    /**Set from ZBNF:  (\?*<$?forElement>\?) */
    public void add_dataText(ScriptElement val){ subContent.content.add(val); }
    
    /**Set from ZBNF:  (\?*<$?dataAccess>\?) */
    //public ScriptElement new_valueVariable(){ return new ScriptElement('g', null); }
    
    /**Set from ZBNF:  (\?*<$?forElement>\?) */
    //public void add_valueVariable(ScriptElement val){ subContent.content.add(val); }
    
    public ScriptElement new_forContainer()
    { GenContent subGenContent = new GenContent(true);
      ScriptElement contentElement = new ScriptElement('C', null);
      contentElement.subContent = subGenContent;  //The contentElement contains a genContent. 
      subContent.content.add(contentElement);
      return contentElement;
    }
    
    public void add_forContainer(ScriptElement val){}

    
    public ScriptElement new_if()
    { GenContent subGenContent = new GenContent(true);
      ScriptElement contentElement = new ScriptElement('F', null);
      contentElement.subContent = subGenContent;  //The contentElement contains a genContent. 
      subContent.content.add(contentElement);
      return contentElement;
    }
    
    public void add_if(ScriptElement val){}

    
    public IfCondition new_ifBlock()
    { GenContent subGenContent = new GenContent(true);
      IfCondition contentElement = new IfCondition('G');
      contentElement.subContent = subGenContent;  //The contentElement contains a genContent. 
      subContent.content.add(contentElement);
      return contentElement;
    }
    
    public void add_ifBlock(IfCondition val){}

    public ScriptElement new_hasNext()
    { ScriptElement contentElement = new ScriptElement('N', null);
      subContent.content.add(contentElement);
      return contentElement;
    }
    
    public void add_hasNext(ScriptElement val){}

    public ScriptElement new_elseBlock()
    { GenContent subGenContent = new GenContent(true);
      ScriptElement contentElement = new ScriptElement('E', null);
      contentElement.subContent = subGenContent;  //The contentElement contains a genContent. 
      subContent.content.add(contentElement);
      return contentElement;
    }
    
    public void add_elseBlock(ScriptElement val){}

    
    
    public ScriptElement new_callSubtext()
    { ScriptElement contentElement = new ScriptElement('s', null);
      subContent.content.add(contentElement);
      return contentElement;
    }
    
    public void add_callSubtext(ScriptElement val){}

    

    
    
    /**Set from ZBNF:  (\?*<$?forElement>\?) */
    public void set_fnEmpty(String val){ 
      ScriptElement contentElement = new ScriptElement('f', val);
      subContent.content.add(contentElement);
    }
    
    public void set_outputValue(String text){ subContent.content.add(new ScriptElement('o', text)); }
    
    public void set_inputValue(String text){ subContent.content.add(new ScriptElement('i', text)); }
    
    //public void set_variableValue(String text){ subContent.content.add(new ScriptElement('v', text)); }
    
    /**Set from ZBNF:  (\?*\?)<?listElement> */
    //public void set_listElement(){ subContent.content.add(new ScriptElement('e', null)); }
    
    public ScriptElement new_forInputContent()
    { ScriptElement contentElement = new ScriptElement('I', null);
      subContent.content.add(contentElement);
      return contentElement;
    }
    
    public void add_forInputContent(ScriptElement val){}

    
    public ScriptElement new_forVariable()
    { ScriptElement contentElement = new ScriptElement('V', null);
      subContent.content.add(contentElement);
      return contentElement;
    }
    
    public void add_forVariable(ScriptElement val){} //empty, it is added in new_forList()

    
    public ScriptElement new_forList()
    { ScriptElement contentElement = new ScriptElement('L', null);
      subContent.content.add(contentElement);
      return contentElement;
    }
    
    public void add_forList(ScriptElement val){} //empty, it is added in new_forList()

    
    public ScriptElement new_addToList()
    { ScriptElement subGenContent = new ScriptElement('l', null);
      subContent.addToList.add(subGenContent.subContent);
      return subGenContent;
    }
   
    public void add_addToList(ScriptElement val)
    {
    }

    
    /**Set from ZBNF:  (\?*<$?forElement>\?) */
    public void axxxdd_fnEmpty(ScriptElement val){  }
    

    
    @Override public String toString()
    {
      switch(whatisit){
      case 't': return text;
      case 'v': return "(?" + text + "?)";
      case 'o': return "(?outp." + text + "?)";
      case 'i': return "(?inp." + text + "?)";
      case 'e': return "<*" + datapath + ">";
      //case 'g': return "<$" + path + ">";
      case 's': return "<*subtext:" + name + ">";
      case 'I': return "(?forInput?)...(/?)";
      case 'V': return "(?for:" + text + "?)";
      case 'L': return "(?forList " + text + "?)";
      case 'C': return "<:for:Container " + text + "?)";
      case 'F': return "<:if:Container " + text + "?)";
      case 'G': return "<:elsif-condition " + text + "?)";
      case 'N': return "<:hasNext> content <.hasNext>";
      case 'E': return "<:else>";
      case 'Z': return "<:target:" + name + ">";
      case 'Y': return "<:file>";
      case 'X': return "<:subtext:" + name + ">";
      default: return "(??" + text + "?)";
      }
    }
    
    
  }

  
  
  public static class IfCondition extends ScriptElement
  {
    
    ScriptElement condition;
    
    CalculatorExpr expr;
    
    IfCondition(char whatis){
      super(whatis, null);
    }
    
    public ScriptElement new_cmpOperation()
    { condition = new ScriptElement('?', null);
      return condition;
    }
    
    public void add_cmpOperation(ScriptElement val){}


    

  }
  
  
  
  /**Organization class for a list of script elements inside another Scriptelement.
   *
   */
  public final static class GenContent
  {
    /**True if < genContent> is called for any input, (?:forInput?) */
    public final boolean isContentForInput;
    
    /**Set from ZBNF: */
    public boolean expandFiles;

    public String cmpnName;
    
    public final List<ScriptElement> content = new LinkedList<ScriptElement>();
    
    /**Scripts for some local variable. This scripts where executed with current data on start of processing this genContent.
     * The generator stores the results in a Map<String, String> localVariable. 
     * 
     */
    private final List<ScriptElement> localVariableScripts = new LinkedList<ScriptElement>();
    
    public final List<GenContent> addToList = new LinkedList<GenContent>();
    
    //public List<String> datapath = new LinkedList<String>();
    
    public GenContent()
    {this.isContentForInput = false;
    }
        
    public GenContent(boolean isContentForInput)
    {this.isContentForInput = isContentForInput;
    }
        
    
    public List<ScriptElement> getLocalVariables(){ return localVariableScripts; }
    
    
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
  
  
  
  
  /**Main class for ZBNF parse result 
   * <pre>
   * ZmakeGenctrl::= { <target> } \e.
   * </pre>
   */
  public final class MainGenCtrl
  {

    
    public ScriptElement new_ZmakeTarget(){ return new ScriptElement('Z', null); }
    
    public void add_ZmakeTarget(ScriptElement val){ zmakeTargets.put(val.name, val); }
    
    
    public ScriptElement new_subtext(){ return new ScriptElement('X', null); }
    
    public void add_subtext(ScriptElement val){ subtexts.put(val.name, val); }
    
    public ScriptElement new_genFile(){ return zbnf_genFile = new ScriptElement('Y', null); }
    
    public void add_genFile(ScriptElement val){  }
    
    /**Defines a variable with initial value. <= <variableAssign?textVariable> \<\.=\>
     */
    public ScriptElement new_textVariable(){ return new ScriptElement('v', null); }

    public void add_textVariable(ScriptElement val){ listScriptVariables.add(val); } 
    
    
    /**Defines a variable with initial value. <= <$name> : <obj>> \<\.=\>
     */
    public ScriptElement new_objVariable(){ return new ScriptElement('J', null); } ///

    public void add_objVariable(ScriptElement val){ listScriptVariables.add(val); } 
    
    
    public ScriptElement new_XXXsetVariable(){ return new ScriptElement('v', null); }

    public void add_XXXsetVariable(ScriptElement val)
    { indexScriptVariables.put(val.name, val); 
      listScriptVariables.add(val);
    } 
    

  }
  


}
