package org.vishia.zmake;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.vishia.mainCmd.Report;
import org.vishia.util.StringPartFromFileLines;
import org.vishia.xmlSimple.XmlException;
import org.vishia.zbnf.ZbnfJavaOutput;
import org.vishia.zbnf.ZbnfParser;
import org.vishia.zbnf.ZbnfXmlOutput;

/**This class contains control data and sub-routines to generate the output-file for all Zmake-targets.
 * 
 * @author Hartmut Schorrig
 *
 */
public class ZmakeGenScript
{
  /**Version and history
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
   * For this source the LGPL Lesser General Public License,
   * published by the Free Software Foundation is valid.
   * It means:
   * <ol>
   * <li> You can use this source without any restriction for any desired purpose.
   * <li> You can redistribute copies of this source to everybody.
   * <li> Every user of this source, also the user of redistribute copies
   *    with or without payment, must accept this license for further using.
   * <li> But the LPGL ist not appropriate for a whole software product,
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
  static final public int version = 20111010;

  private final Report console;

  /**Mirror of the content of the zmake-genctrl-file. Filled from ZBNF-ParseResult*/
  Zbnf_ZmakeGenCtrl zbnfZmakeGenCtrl = new Zbnf_ZmakeGenCtrl();
  
  Map<String, Zbnf_genContent> zmakeTargets = new TreeMap<String, Zbnf_genContent>();
  
  private final Map<String, ScriptElement> subtexts = new TreeMap<String, ScriptElement>();
  
  
  
  ScriptElement zbnf_genFile;
  

  public ZmakeGenScript(Report console)
  { this.console = console;
  }

  
  public boolean parseGenCtrl(File fileZbnf4GenCtrl, File fileGenCtrl) 
  throws FileNotFoundException, IOException
    , ParseException, XmlException, IllegalArgumentException, IllegalAccessException, InstantiationException
  { boolean bOk;
    console.writeInfoln("* Zmake: parsing gen script \"" + fileZbnf4GenCtrl.getAbsolutePath() 
    + "\" with \"" + fileGenCtrl.getAbsolutePath() + "\"");

    ZbnfParser parserGenCtrl = new ZbnfParser(console);
    parserGenCtrl.setSyntax(fileZbnf4GenCtrl);
    if(console.getReportLevel() >= Report.fineInfo){
      console.reportln(Report.fineInfo, "== Syntax GenCtrl ==");
      parserGenCtrl.reportSyntax(console, Report.fineInfo);
    }
    console.writeInfo(" ... ");
    bOk = parserGenCtrl.parse(new StringPartFromFileLines(fileGenCtrl));
    if(!bOk){
      String sError = parserGenCtrl.getSyntaxErrorReport();
      throw new ParseException(sError,0);
    }
    if(console.getReportLevel() >= Report.fineInfo){
      parserGenCtrl.reportStore(console, Report.fineInfo, "Zmake-GenScript");
    }
    console.writeInfo(", ok set output ... ");
    //ZbnfParseResultItem parseResultGenCtrl = parserGenCtrl.getFirstParseResult();
    ZbnfXmlOutput xmlOutputGenCtrl = new ZbnfXmlOutput();
    xmlOutputGenCtrl.write(parserGenCtrl, fileGenCtrl.getAbsoluteFile()+".xml");  //only for test
    //write into Java classes:
    ZbnfJavaOutput parserGenCtrl2Java = new ZbnfJavaOutput(console);
    parserGenCtrl2Java.setContent(zbnfZmakeGenCtrl.getClass(), zbnfZmakeGenCtrl, parserGenCtrl.getFirstParseResult());
    console.writeInfo(" ok");
    return bOk;
  }
  
  
  /**Searches the Zmake-target by name (binary search. TreeMap.get(name).
   * @param name The name of given < ?translator> in the end-users script.
   * @return null if the Zmake-target is not found.
   */
  Zbnf_genContent searchZmakeTaget(String name){ return zmakeTargets.get(name); }
  
  
  public final ScriptElement getFileScript(){ return zbnf_genFile; }
  
  
  public ScriptElement getSubtextScript(String name){ return subtexts.get(name); }
  
  public Zbnf_genContent xxxgetScriptVariable(String sName)
  {
    Zbnf_genContent content = zbnfZmakeGenCtrl.indexScriptVariables.get(sName);
    return content;
  }
  
  
  public static final class DataPath{
    public String name;
    public List<String> path;
  }
  
  
  
  /**An element of the generate script, maybe a simple text, an condition etc.
   * It may have a sub content with a list of sub scrip elements if need, see aggregation {@link #subContent}. 
   * 
   *
   */
  public final class ScriptElement
  {
    /**Designation what presents the element:
     * <table><tr><th>c</th><th>what is it</th></tr>
     * <tr><td>t</td><td>simple constant text</td></tr>
     * <tr><td>v</td><td>content of a variable, {@link #text} contains the name of the variable</td></tr>
     * <tr><td>i</td><td>content of the input, {@link #text} describes the build-prescript, 
     *                   see {@link ZmakeGenerator#getPartsFromFilepath(org.vishia.zmake.ZmakeUserScript.UserFilepath, String)}</td></tr>
     * <tr><td>o</td><td>content of the output, {@link #text} describes the build-prescript, 
     *                   see {@link ZmakeGenerator#getPartsFromFilepath(org.vishia.zmake.ZmakeUserScript.UserFilepath, String)}</td></tr>
     * <tr><td>e</td><td>given content of a list or for-element. List: {@link #text}==null, Always: {@link #subContent} == null.</td></tr>
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
    
    public List<String> path;
    
    private List<DataPath> refenceData;
    
    //public String elementPart;
    
    /**If need, a sub-content, maybe null. TODO should be final*/
    public Zbnf_genContent subContent;
    
    public ScriptElement(char whatisit, String text)
    { this.whatisit = whatisit;
      this.text = text;
      if("NXYZ".indexOf(whatisit)>=0){
        subContent = new Zbnf_genContent(false);
      }
    }
    
    
    public List<DataPath> getReferenceDataSettings(){ return refenceData; }
    
    public Zbnf_genContent getSubContent(){ return subContent; }
    
    public void set_text(String text){ subContent.content.add(new ScriptElement('t', text)); }
    
    
    
    
    
    /**Set from ZBNF:  \<*subtext:name: { <referencedData> ?,} \> */
    public DataPath new_referencedData(){ return new DataPath(); }
    
    /**Set from ZBNF:  \<*subtext:name: { <referencedData> ?,} \> */
    public void add_referencedData(DataPath val){ 
      if(refenceData == null){ refenceData = new LinkedList<DataPath>(); }
      refenceData.add(val); }
    
    
    
    /**Set from ZBNF:  (\?*<$?valueElement>\?) */
    public ScriptElement new_valueElement(){ return new ScriptElement('e', null); }
    
    /**Set from ZBNF:  (\?*<$?forElement>\?) */
    public void add_valueElement(ScriptElement val){ subContent.content.add(val); }
    
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

    
    @Override public String toString()
    {
      switch(whatisit){
      case 't': return text;
      case 'v': return "(?" + text + "?)";
      case 'o': return "(?outp." + text + "?)";
      case 'i': return "(?inp." + text + "?)";
      case 'e': return "<*" + path + ">";
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

  
  /**Class for ZBNF parse result 
   * <pre>
genContent::=  ##<$NoWhiteSpaces>
{ [?(\?/\?)]
[ (\?= <variableAssignment?setVariable> (\?/=\?)
| (\?:forInput\?) <genContent?forInputContent> (\?/forInput\?)
| (\?:forList : <forList> (\?/forList\?)
| (\?+ <variableAssignment?addToList> (\?/+\?)
| (\?input\.<$?inputValue>\?)    
| (\?output\.<$?outputValue>\?)
| (\?*\?)<?listElement>
| (\?<$?variableValue>\?)
| (\?:\?)<genContentNoWhitespace?>(\?/\?)
| (\?:text\?)<*|(\??text>(\?/text\?)  ##text in (?:text?).....(?/text?) with all whitespaces 
| <*|(\??text>                        ##text after whitespace but inclusive trailing whitespaces
]
}.
   * </pre>
   * It is the content of a target in a generating script.
   */
  public final class Zbnf_genContent
  {
    /**True if < genContent> is called for any input, (?:forInput?) */
    final boolean isContentForInput;
    
    /**Set from ZBNF: */
    public boolean expandFiles;

    public String cmpnName;
    
    public final List<ScriptElement> content = new LinkedList<ScriptElement>();
    
    /**Scripts for some local variable. This scripts where executed with current data on start of processing this genContent.
     * The generator stores the results in a Map<String, String> localVariable. 
     * 
     */
    List<Zbnf_genContent> localVariableScripts = new LinkedList<Zbnf_genContent>();
    
    public final List<Zbnf_genContent> addToList = new LinkedList<Zbnf_genContent>();
    
    //public List<String> datapath = new LinkedList<String>();
    
    public Zbnf_genContent(boolean isContentForInput)
    {this.isContentForInput = isContentForInput;
    }
        
    
    public List<Zbnf_genContent> getLocalVariables(){ return localVariableScripts; }
    
    public void set_outputValue(String text){ content.add(new ScriptElement('o', text)); }
    
    public void set_inputValue(String text){ content.add(new ScriptElement('i', text)); }
    
    public void set_variableValue(String text){ content.add(new ScriptElement('v', text)); }
    
    /**Set from ZBNF:  (\?*\?)<?listElement> */
    public void set_listElement(){ content.add(new ScriptElement('e', null)); }
    
    /**Set from ZBNF:  (\?*<$?forElement>\?) */
    public void set_fnEmpty(String val){ content.add(new ScriptElement('f', val)); }
    
    /**Set from ZBNF:  (\?*<$?forElement>\?) */
    public void add_fnEmpty(ScriptElement val){ content.add(val); }
    
    public Zbnf_genContent new_setVariable(){ return new Zbnf_genContent(false); }

    public void add_setVariable(Zbnf_genContent val){ localVariableScripts.add(val); } 
    
    public Zbnf_genContent new_forInputContent()
    { Zbnf_genContent subGenContent = new Zbnf_genContent(true);
      ScriptElement contentElement = new ScriptElement('I', null);
      contentElement.subContent = subGenContent;  //The contentElement contains a genContent. 
      content.add(contentElement);
      return subGenContent;
    }
    
    public void add_forInputContent(Zbnf_genContent val){}

    
    public Zbnf_genContent new_forVariable()
    { Zbnf_genContent subGenContent = new Zbnf_genContent(true);
      ScriptElement contentElement = new ScriptElement('V', null);
      contentElement.subContent = subGenContent;  //The contentElement contains a genContent. 
      content.add(contentElement);
      return subGenContent;
    }
    
    public void add_forVariable(Zbnf_genContent val){} //empty, it is added in new_forList()

    
    public Zbnf_genContent new_forList()
    { Zbnf_genContent subGenContent = new Zbnf_genContent(true);
      ScriptElement contentElement = new ScriptElement('L', null);
      contentElement.subContent = subGenContent;  //The contentElement contains a genContent. 
      content.add(contentElement);
      return subGenContent;
    }
    
    public void add_forList(Zbnf_genContent val){} //empty, it is added in new_forList()

    
    public Zbnf_genContent new_addToList()
    { Zbnf_genContent subGenContent = new Zbnf_genContent(false);
      addToList.add(subGenContent);
      return subGenContent;
    }
   
    public void add_addToList(Zbnf_genContent val)
    {
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

    Map<String,Zbnf_genContent> indexScriptVariables = new TreeMap<String,Zbnf_genContent>();

    /**List of the script variables in order of creation in the zmakeCtrl-file.
     * The script variables can contain inputs of other variables which are defined before.
     * Therefore the order is important.
     */
    List<Zbnf_genContent> listScriptVariables = new LinkedList<Zbnf_genContent>();

    
    public Zbnf_genContent new_ZmakeTarget(){ return new Zbnf_genContent(false); }
    
    public void add_ZmakeTarget(Zbnf_genContent val){ zmakeTargets.put(val.cmpnName, val); }
    
    
    public ScriptElement new_subtext(){ return new ScriptElement('X', null); }
    
    public void add_subtext(ScriptElement val){ subtexts.put(val.name, val); }
    
    public ScriptElement new_genFile(){ return zbnf_genFile = new ScriptElement('Y', null); }
    
    public void add_genFile(ScriptElement val){  }
    
    public Zbnf_genContent new_setVariable(){ return new Zbnf_genContent(false); }

    public void add_setVariable(Zbnf_genContent val)
    { indexScriptVariables.put(val.cmpnName, val); 
      listScriptVariables.add(val);
    } 
    

  }
  

}
