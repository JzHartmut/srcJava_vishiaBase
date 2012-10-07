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

  private final Report console;

  /**Mirror of the content of the zmake-genctrl-file. Filled from ZBNF-ParseResult*/
  Zbnf_ZmakeGenCtrl zbnfZmakeGenCtrl = new Zbnf_ZmakeGenCtrl();
  
  Map<String, Zbnf_genContent> zmakeTargets = new TreeMap<String, Zbnf_genContent>();
  
  Zbnf_genContent zbnf_genFile;
  

  public ZmakeGenScript(Report console)
  { this.console = console;
  }

  
  boolean parseGenCtrl(File fileZbnf4GenCtrl, File fileGenCtrl) 
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
  
  public Zbnf_genContent xxxgetScriptVariable(String sName)
  {
    Zbnf_genContent content = zbnfZmakeGenCtrl.indexScriptVariables.get(sName);
    return content;
  }
  
  
  public static final class Zbnf_ScriptElement
  {
    /**Designation what presents the element:
     * <table><tr><th>c</th><th>what is it</th></tr>
     * <tr><td>t</td><td>simple constant text</td></tr>
     * <tr><td>v</td><td>content of a variable, {@link #text} contains the name of the variable</td></tr>
     * <tr><td>i</td><td>content of the input, {@link #text} describes the build-prescript, 
     *                   see {@link ZmakeGenerator#getPartsFromFilepath(org.vishia.zmake.ZmakeUserScript.UserFilepath, String)}</td></tr>
     * <tr><td>i</td><td>content of the output, {@link #text} describes the build-prescript, 
     *                   see {@link ZmakeGenerator#getPartsFromFilepath(org.vishia.zmake.ZmakeUserScript.UserFilepath, String)}</td></tr>
     * <tr><td>e</td><td>given content of a list or for-element. List: {@link #text}==null, Always: {@link #subContent} == null.</td></tr>
     * <tr><td>I</td><td>(?:forInput?): {@link #subContent} contains build.script for any input element</td></tr>
     * <tr><td>V</td><td>(?:for:variable?): {@link #subContent} contains build.script for any element of the named global variable or calling parameter</td></tr>
     * <tr><td>L</td><td>(?:forList?): {@link #subContent} contains build.script for any list element,
     *                   whereby subContent.{@link Zbnf_genContent#name} is the name of the list. </td></tr>
     * </table> 
     */
    final char whatisit;    
    
    /**Constant text or name of elements or build-script-name. */
    final public String text;
    
    public String name;
    
    public String elementPart;
    
    /**If need, a sub-content, maybe null. */
    Zbnf_genContent subContent;
    
    public Zbnf_ScriptElement(char whatisit, String text)
    { this.whatisit = whatisit;
      this.text = text;
    }
    
    
    @Override public String toString()
    {
      switch(whatisit){
      case 't': return text;
      case 'v': return "(?" + text + "?)";
      case 'o': return "(?outp." + text + "?)";
      case 'i': return "(?inp." + text + "?)";
      case 'e': return "(?*" + text + "?)";
      case 'I': return "(?forInput?)...(/?)";
      case 'V': return "(?for:" + text + "?)";
      case 'L': return "(?forList " + text + "?)";
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
    
    public String name;
    
    /**Set from ZBNF: */
    public boolean expandFiles;

    List<Zbnf_ScriptElement> content = new LinkedList<Zbnf_ScriptElement>();
    
    List<Zbnf_genContent> localVariables = new LinkedList<Zbnf_genContent>();
    
    List<Zbnf_genContent> addToList = new LinkedList<Zbnf_genContent>();
    
    public Zbnf_genContent(boolean isContentForInput)
    {this.isContentForInput = isContentForInput;
    }
        
    public void set_text(String text){ content.add(new Zbnf_ScriptElement('t', text)); }
    
    public void set_outputValue(String text){ content.add(new Zbnf_ScriptElement('o', text)); }
    
    public void set_inputValue(String text){ content.add(new Zbnf_ScriptElement('i', text)); }
    
    public void set_variableValue(String text){ content.add(new Zbnf_ScriptElement('v', text)); }
    
    /**Set from ZBNF:  (\?*<$?valueElement>\?) */
    public Zbnf_ScriptElement new_valueElement(){ return new Zbnf_ScriptElement('e', null); }
    
    /**Set from ZBNF:  (\?*<$?forElement>\?) */
    public void add_valueElement(Zbnf_ScriptElement val){ content.add(val); }
    
    /**Set from ZBNF:  (\?*\?)<?listElement> */
    public void set_listElement(){ content.add(new Zbnf_ScriptElement('e', null)); }
    
    /**Set from ZBNF:  (\?*<$?forElement>\?) */
    public void set_fnEmpty(String val){ content.add(new Zbnf_ScriptElement('f', val)); }
    
    /**Set from ZBNF:  (\?*<$?forElement>\?) */
    public void add_fnEmpty(Zbnf_ScriptElement val){ content.add(val); }
    
    public Zbnf_genContent new_setVariable(){ return new Zbnf_genContent(false); }

    public void add_setVariable(Zbnf_genContent val){ localVariables.add(val); } 
    
    public Zbnf_genContent new_forInputContent()
    { Zbnf_genContent subGenContent = new Zbnf_genContent(true);
      Zbnf_ScriptElement contentElement = new Zbnf_ScriptElement('I', null);
      contentElement.subContent = subGenContent;  //The contentElement contains a genContent. 
      content.add(contentElement);
      return subGenContent;
    }
    
    public void add_forInputContent(Zbnf_genContent val){}

    
    public Zbnf_genContent new_forVariable()
    { Zbnf_genContent subGenContent = new Zbnf_genContent(true);
      Zbnf_ScriptElement contentElement = new Zbnf_ScriptElement('V', null);
      contentElement.subContent = subGenContent;  //The contentElement contains a genContent. 
      content.add(contentElement);
      return subGenContent;
    }
    
    public void add_forVariable(Zbnf_genContent val){} //empty, it is added in new_forList()

    
    public Zbnf_genContent new_forList()
    { Zbnf_genContent subGenContent = new Zbnf_genContent(true);
      Zbnf_ScriptElement contentElement = new Zbnf_ScriptElement('L', null);
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

    
    @Override public String toString()
    { return "genContent name=" + name + ":" + content;
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
    
    public void add_ZmakeTarget(Zbnf_genContent val){ zmakeTargets.put(val.name, val); }
    
    
    public Zbnf_genContent new_genFile(){ return zbnf_genFile = new Zbnf_genContent(false); }
    
    public void add_genFile(Zbnf_genContent val){  }
    
    public Zbnf_genContent new_setVariable(){ return new Zbnf_genContent(false); }

    public void add_setVariable(Zbnf_genContent val)
    { indexScriptVariables.put(val.name, val); 
      listScriptVariables.add(val);
    } 
    

  }
  

}
