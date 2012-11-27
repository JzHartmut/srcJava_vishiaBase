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
import org.vishia.util.DataAccess;
import org.vishia.util.StringPartFromFileLines;
import org.vishia.xmlSimple.XmlException;
import org.vishia.zbnf.ZbnfJavaOutput;
import org.vishia.zbnf.ZbnfParser;
import org.vishia.zbnf.ZbnfXmlOutput;

/**This class contains control data and sub-routines to generate output texts from internal data.
 * 
 * @author Hartmut Schorrig
 *
 */
public class TextGenScript {
  /**Version, history and license.
   * <ul>
   * <li>2012-10-19 Hartmut chg: <:if...> works.
   * <li>2012-10-19 Hartmut chg: Renaming: {@link ScriptElement} instead Zbnf_ScriptElement (shorter). The Scriptelement
   *   is the component for the genContent-Elements now instead Zbnf_genContent. This class contains attributes of the
   *   content elements. Only if a sub content is need, an instance of Zbnf_genContent is created as {@link ScriptElement#subContent}.
   *   Furthermore the {@link ScriptElement#subContent} should be final because it is only created if need for the special 
   *   {@link ScriptElement#whatisit}-types (TODO). This version works for {@link org.vishia.stateMGen.StateMGen}.
   * <li>2012-10-11 Hartmut chg Syntax changed of ZmakeGenCtrl.zbnf: datapath::={ <$?path>? \.}. 
   *   instead valueElement::=<$?name>\.<$?elementPart>., it is more universal. adapted. 
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
  @SuppressWarnings("hiding")
  static final public int version = 20121130;

  private final MainCmdLogging_ifc console;

  /**Mirror of the content of the zmake-genctrl-file. Filled from ZBNF-ParseResult*/
  Zbnf_ZmakeGenCtrl zbnfZmakeGenCtrl = new Zbnf_ZmakeGenCtrl();
  
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

  
  public boolean parseGenCtrl(File fileZbnf4GenCtrl, File fileGenCtrl) 
  throws FileNotFoundException, IOException
    , ParseException, XmlException, IllegalArgumentException, IllegalAccessException, InstantiationException
  { boolean bOk;
    console.writeInfoln("* Zmake: parsing gen script \"" + fileZbnf4GenCtrl.getAbsolutePath() 
    + "\" with \"" + fileGenCtrl.getAbsolutePath() + "\"");

    ZbnfParser parserGenCtrl = new ZbnfParser((Report)console);
    parserGenCtrl.setSyntax(fileZbnf4GenCtrl);
    if(console.getReportLevel() >= MainCmdLogging_ifc.fineInfo){
      console.reportln(MainCmdLogging_ifc.fineInfo, "== Syntax GenCtrl ==");
      parserGenCtrl.reportSyntax((Report)console, MainCmdLogging_ifc.fineInfo);
    }
    console.writeInfo(" ... ");
    bOk = parserGenCtrl.parse(new StringPartFromFileLines(fileGenCtrl));
    if(!bOk){
      String sError = parserGenCtrl.getSyntaxErrorReport();
      throw new ParseException(sError,0);
    }
    if(console.getReportLevel() >= MainCmdLogging_ifc.fineInfo){
      parserGenCtrl.reportStore((Report)console, MainCmdLogging_ifc.fineInfo, "Zmake-GenScript");
    }
    console.writeInfo(", ok set output ... ");
    //ZbnfParseResultItem parseResultGenCtrl = parserGenCtrl.getFirstParseResult();
    ZbnfXmlOutput xmlOutputGenCtrl = new ZbnfXmlOutput();
    xmlOutputGenCtrl.write(parserGenCtrl, fileGenCtrl.getAbsoluteFile()+".xml");  //only for test
    //write into Java classes:
    ZbnfJavaOutput parserGenCtrl2Java = new ZbnfJavaOutput((Report)console);
    parserGenCtrl2Java.setContent(zbnfZmakeGenCtrl.getClass(), zbnfZmakeGenCtrl, parserGenCtrl.getFirstParseResult());
    console.writeInfo(" ok");
    return bOk;
  }
  
  
  /**Searches the Zmake-target by name (binary search. TreeMap.get(name).
   * @param name The name of given < ?translator> in the end-users script.
   * @return null if the Zmake-target is not found.
   */
  public final Zbnf_genContent searchZmakeTaget(String name){ 
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
  
  
  
  public static final class Arguments{
    public String name;
    //public List<String> path;
    List<DataAccess.DatapathElement> datapath;
    
    public DataAccess.DatapathElement new_datapathElement(){ return new DataAccess.DatapathElement(); }
    
    public void add_datapathElement(DataAccess.DatapathElement val){ 
      if(datapath == null){
        datapath = new LinkedList<DataAccess.DatapathElement>();
      }
      datapath.add(val); 
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
  public final class ScriptElement
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
     * <tr><td>e</td><td>content of a data path.</td></tr>
     * <tr><td>XXXg</td><td>content of a data path starting with an internal variable (reference) or value of the variable.</td></tr>
     * <tr><td>s</td><td>call of a subtext by name. {@link #text}==null, {@link #subContent} == null.</td></tr>
     * <tr><td>I</td><td>(?:forInput?): {@link #subContent} contains build.script for any input element</td></tr>
     * <tr><td>V</td><td>(?:for:variable?): {@link #subContent} contains build.script for any element of the named global variable or calling parameter</td></tr>
     * <tr><td>L</td><td>(?:forList?): {@link #subContent} contains build.script for any list element,
     *                   whereby subContent.{@link Zbnf_genContent#name} is the name of the list. </td></tr>
     * <tr><td>C</td><td><:for:path> {@link #subContent} contains build.script for any list element,
     * <tr><td>E</td><td><:else> {@link #subContent} contains build.script for any list element,
     * <tr><td>F</td><td><:if:condition:path> {@link #subContent} contains build.script for any list element,
     * <tr><td>G</td><td><:elsif:condition:path> {@link #subContent} contains build.script for any list element,
     * 
     * <tr><td>Z</td><td>a target,
     * <tr><td>Y</td><td>the file
     * <tr><td>X</td><td>a subtext
     * </table> 
     */
    final public char whatisit;    
    
    /**Constant text or name of elements or build-script-name. */
    public final String text;
    
    public String name;
    
    public String operator;
    
    public String value;
    
    //public List<String> path;
    
    List<DataAccess.DatapathElement> datapath;
    
    public DataAccess.DatapathElement new_datapathElement(){ return new DataAccess.DatapathElement(); }
    
    public void add_datapathElement(DataAccess.DatapathElement val){ 
      if(datapath == null){
        datapath = new LinkedList<DataAccess.DatapathElement>();
      }
      datapath.add(val); 
    }
    
    private List<Arguments> refenceData;
    
    //public String elementPart;
    
    /**If need, a sub-content, maybe null. TODO should be final*/
    public Zbnf_genContent subContent;
    
    public ScriptElement(char whatisit, String text)
    { this.whatisit = whatisit;
      this.text = text;
      if("NXYZvl".indexOf(whatisit)>=0){
        subContent = new Zbnf_genContent(false);
      }
      else if("IVL".indexOf(whatisit)>=0){
        subContent = new Zbnf_genContent(true);
      }
    }
    
    
    public List<Arguments> getReferenceDataSettings(){ return refenceData; }
    
    public Zbnf_genContent getSubContent(){ return subContent; }
    
    public void set_text(String text){ subContent.content.add(new ScriptElement('t', text)); }
    
    
    
    /**Defines a variable with initial value. <= <variableAssign?setVariable> \<\.=\>
     */
    public ScriptElement new_setVariable(){ return new ScriptElement('v', null); }

    public void add_setVariable(ScriptElement val){ subContent.localVariableScripts.add(val); } 
    
    
    
    /**Set from ZBNF:  \<*subtext:name: { <referencedData> ?,} \> */
    public Arguments new_referencedData(){ return new Arguments(); }
    
    /**Set from ZBNF:  \<*subtext:name: { <referencedData> ?,} \> */
    public void add_referencedData(Arguments val){ 
      if(refenceData == null){ refenceData = new LinkedList<Arguments>(); }
      refenceData.add(val); }
    
    
    
    /**Set from ZBNF:  (\?*<$?valueElement>\?) */
    public ScriptElement new_valueElement(){ return new ScriptElement('e', null); }
    
    /**Set from ZBNF:  (\?*<$?forElement>\?) */
    public void add_valueElement(ScriptElement val){ subContent.content.add(val); }
    
    /**Set from ZBNF:  (\?*<$?valueElement>\?) */
    //public ScriptElement new_valueVariable(){ return new ScriptElement('g', null); }
    
    /**Set from ZBNF:  (\?*<$?forElement>\?) */
    //public void add_valueVariable(ScriptElement val){ subContent.content.add(val); }
    
    public ScriptElement new_forContainer()
    { Zbnf_genContent subGenContent = new Zbnf_genContent(true);
      ScriptElement contentElement = new ScriptElement('C', null);
      contentElement.subContent = subGenContent;  //The contentElement contains a genContent. 
      subContent.content.add(contentElement);
      return contentElement;
    }
    
    public void add_forContainer(ScriptElement val){}

    
    public ScriptElement new_ifContainer()
    { Zbnf_genContent subGenContent = new Zbnf_genContent(true);
      ScriptElement contentElement = new ScriptElement('F', null);
      contentElement.subContent = subGenContent;  //The contentElement contains a genContent. 
      subContent.content.add(contentElement);
      return contentElement;
    }
    
    public void add_ifContainer(ScriptElement val){}

    
    public ScriptElement new_ifBlock()
    { Zbnf_genContent subGenContent = new Zbnf_genContent(true);
      ScriptElement contentElement = new ScriptElement('G', null);
      contentElement.subContent = subGenContent;  //The contentElement contains a genContent. 
      subContent.content.add(contentElement);
      return contentElement;
    }
    
    public void add_ifBlock(ScriptElement val){}

    public ScriptElement new_hasNext()
    { ScriptElement contentElement = new ScriptElement('N', null);
      subContent.content.add(contentElement);
      return contentElement;
    }
    
    public void add_hasNext(ScriptElement val){}

    public ScriptElement new_elseBlock()
    { Zbnf_genContent subGenContent = new Zbnf_genContent(true);
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

  
  /**Organization class for a list of script elements inside another Scriptelement.
   *
   */
  public final class Zbnf_genContent
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
    
    public final List<Zbnf_genContent> addToList = new LinkedList<Zbnf_genContent>();
    
    //public List<String> datapath = new LinkedList<String>();
    
    public Zbnf_genContent(boolean isContentForInput)
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
  public final class Zbnf_ZmakeGenCtrl
  {

    
    public ScriptElement new_ZmakeTarget(){ return new ScriptElement('Z', null); }
    
    public void add_ZmakeTarget(ScriptElement val){ zmakeTargets.put(val.name, val); }
    
    
    public ScriptElement new_subtext(){ return new ScriptElement('X', null); }
    
    public void add_subtext(ScriptElement val){ subtexts.put(val.name, val); }
    
    public ScriptElement new_genFile(){ return zbnf_genFile = new ScriptElement('Y', null); }
    
    public void add_genFile(ScriptElement val){  }
    
    public ScriptElement new_setVariable(){ return new ScriptElement('v', null); }

    public void add_setVariable(ScriptElement val)
    { indexScriptVariables.put(val.name, val); 
      listScriptVariables.add(val);
    } 
    

  }
  


}
