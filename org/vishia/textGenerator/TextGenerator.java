package org.vishia.textGenerator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.lang.reflect.Field;
import java.text.ParseException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;

import org.vishia.mainCmd.Report;
import org.vishia.util.Assert;
import org.vishia.zmake.ZmakeGenScript;

/**This class helps to generate texts from any Java-stored data controlled with a script.
 * The script is a simple text file which contains placeholder for data and some control statements
 * for repeatedly generated data from any container.
 * <br><br>
 * The placeholder and control tags have the following form:
 * <ul>
 * <li> 
 *   <*path>: Access to any data element in the given data pool. The path starts from that object, which is given as input.
 *   Access to deeper nested elements are possible to write a dot. The return value is converted to a String.
 *   <br>
 *   The elements in the path can be public references, fields or method invocations. Methods can be given with constant parameters
 *   or with parameters stored in a script variable.
 *   <br>
 *   Example: <*data1.getData2().data2> 
 * <li>
 *   <:for:element:path>...<.for>: The text between the tags is generated for any member of the container, 
 *   which is designated with the path. The access to the elements is able to use the <*element.path>, where 'element'
 *   is any String identifier used in this for control. Controls can be nested.
 * <li>
 *   <:if:conditionpath>...<:elif>...<:else>...<.if>: The text between the tags is generated only if the condition is met. 
 * <li>
 *   <:switch:path><:case:value>...<:case:value>...<:else>...<.switch>: One variable is tested. The variable can be numerical or a String.
 *   Several values are tested.   
 * </ul> 
 * @author Hartmut
 *
 */
public class TextGenerator {
  
  
  /**Version and history
   * <ul>
   * <li>2012-10-19 Hartmut chg: <:if...> works.
   * <li>2012-10-10 Usage of {@link ZmakeGenScript}.
   * <li>2012-10-03 created. Backgorund was the {@link org.vishia.zmake.Zmake} generator, but that is special for make problems.
   *   A generator which converts ZBNF-parsed data from an Java data context to output texts in several form, documenation, C-sources
   *   was need.
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
  static final public int version = 20121010;

  
  
  
  BufferedReader readerScript;
  
  Writer out;

  //StringBuilder uLine = new StringBuilder(5000);

  //String sLine;
  
  Object data;
  
  String sError = null;
  
  private boolean bWriteErrorInOutput;
  
  final Report console;
  
  
  public TextGenerator(Report console){
    this.console = console;
  }
  
  
  
  public String generate(Object userData, File fileScript, File fOut, Appendable testOut){
    ZmakeGenScript genScript = new ZmakeGenScript(console);
    File fileZbnf4GenCtrl = new File("D:/vishia/ZBNF/sf/ZBNF/zbnfjax/zmake/ZmakeGenctrl.zbnf");
    Writer out = null;
    String sError = null;
    try{ 
      genScript.parseGenCtrl(fileZbnf4GenCtrl, fileScript);
      if(testOut !=null){
        OutputDataTree outputterData = new OutputDataTree();
        outputterData.output(0, genScript, testOut, false);
      }
      out = new FileWriter(fOut);
    
    } catch(ParseException exc){
      System.err.println(Assert.exceptionInfo("\n", exc, 0, 4));
      
    } catch(FileNotFoundException exc){
      System.err.println(Assert.exceptionInfo("\n", exc, 0, 4));
    } catch(Exception exc){
      System.err.println(Assert.exceptionInfo("\n", exc, 0, 4));
    }
    if(out !=null){
      try{
        sError = genContent(genScript, userData, out);
        out.close();
      } catch(IOException exc){
        System.err.println(Assert.exceptionInfo("", exc, 0, 4));
      }
    }
    //String sError = generator.generate(zbnfResultData, fileScript, fOut, true);
    return sError;
  }
  
  
  
  

  
  
  
  
  
  String getStringFromObject(Object content){
    String sContent;
    if(content == null){
    sContent = "";
    }
    else if(content instanceof String){ 
      sContent = (String) content; 
    } else if(content instanceof Integer){ 
      int value = ((Integer)content).intValue();
      sContent = "" + value;
    } else {
      sContent = content.toString();
    }
    return sContent;
  }
  
  
  
  
  /**Read content from data.
   * @param sPath
   * @return
   * @throws IllegalArgumentException
   */
  private Object getContent(List<String> path, Map<String, Object> localVariables)
  throws IllegalArgumentException
  {
    Class<?> clazz1;
    Object data1 = data;
    Iterator<String> iter = path.iterator();
    String sElement = iter.next();
    //ForEach forVariable = idxForeaches.get(sElement);
    data1 = localVariables.get(sElement);
    if(data1 !=null){
      sElement = iter.hasNext() ? iter.next() : null;
    } else {
      data1 = this.data;
    }
    while(sElement !=null && data1 !=null){
      try{ 
        clazz1 = data1.getClass();
        Field element = clazz1.getDeclaredField(sElement);
        element.setAccessible(true);
        try{ data1 = element.get(data1);
        
        } catch(IllegalAccessException exc){ 
          if(bWriteErrorInOutput){
            return "<? path access: " + path.toString() + "?>";
          } else {
            throw new IllegalArgumentException("IllegalAccessException, hint: should be public;" + sElement); 
          }
        }
        sElement = iter.hasNext() ? iter.next() : null;
      } catch(NoSuchFieldException exc){
        //TODO method
        if(bWriteErrorInOutput){
          return "<? path fault: " + path.toString() + "?>";
        } else {
          throw new IllegalArgumentException("NoSuchFieldException;" + sElement); 
        }
      }
    }
    return data1;
  }
  
  
  
  public String genContent(ZmakeGenScript genScript, Object userData, Writer out) throws IOException{
    this.out = out;
    this.data = userData;
    this.bWriteErrorInOutput = true;
    ZmakeGenScript.ScriptElement contentScript = genScript.getFileScript();
    Gen_Content genFile = new Gen_Content(null);
    String sError = genFile.genContent(contentScript.subContent, userData);
    return sError;
  }
    
  
  
  final class Gen_Content
  {
    final Gen_Content parent;
    
    Map<String, Object> localVariables = new TreeMap<String, Object>();
    
    
    
    
    public Gen_Content(Gen_Content parent)
    { this.parent = parent;
      if(parent !=null){
        this.localVariables.putAll(parent.localVariables);
      }
    }


  
    public String genContent(ZmakeGenScript.Zbnf_genContent contentScript, Object userTarget) 
    throws IOException{
      Appendable uBuffer = out;
      //Fill all local variable, which are defined in this script.
      //store its values in the local Gen_Content-instance.
      for(ZmakeGenScript.Zbnf_genContent variableScript: contentScript.getLocalVariables()){
        StringBuilder uBufferVariable = new StringBuilder();
        Gen_Content genVariable = new Gen_Content(this);
        //genVariable.gen_Content(uBufferVariable, null, userTarget, variableScript, forElements, srcPath);
        localVariables.put(variableScript.cmpnName, uBufferVariable);
      }
    
    
      //Generate direct requested output. It is especially on inner content-scripts.
      for(ZmakeGenScript.ScriptElement contentElement: contentScript.content){
        switch(contentElement.whatisit){
        case 't': { 
          int posLine = 0;
          int posEnd;
          do{
            posEnd = contentElement.text.indexOf('\n', posLine);
            if(posEnd >= 0){ 
              uBuffer.append(contentElement.text.substring(posLine, posEnd));   
              uBuffer.append("\r\n");
              posLine = posEnd +1;  //after \n 
            } else {
              uBuffer.append(contentElement.text.substring(posLine));   
            }
            
          } while(posEnd >=0);  //output all lines.
        } break;
        case 'f': {
          if(contentElement.text.equals("nextNr")){
            String val = "" + nextNr.toString();
            uBuffer.append(nextNr.toString());
          }
       } break;
        case 'v': {
          //TODO: delete it later
          if(contentElement.text.equals("target")){
            //generates all targets, only advisable in the (?:file?)
            //genUserTargets(out);
          } else {
            //XXXreplacePlaceholder(contentElement.text);
         }
        } break;
        case 'e': {
          final CharSequence text;
          if(contentElement.text !=null && contentElement.text.equals("target")){
            //generates all targets, only advisable in the (?:file?)
            //genUserTargets(out);
          } else if(contentElement.path !=null){
            Object oContent = getContent(contentElement.path, localVariables);
            text = getStringFromObject(oContent);
            //text = getTextofVariable(userTarget, contentElement.text, this);
            uBuffer.append(text); 
          } else {
            //uBuffer.append(listElement);
          }
        } break;
        case 'C': { //generation (?:for:<$?@name>?) <genContent?> (?/for?)
          ZmakeGenScript.Zbnf_genContent subContent = contentElement.getSubContent();
          if(contentElement.name.equals("sub"))
            stop();
          Object container = getContent(contentElement.path, localVariables);
          if(container !=null && container instanceof Iterable<?>){
            Iterator<?> iter = ((Iterable<?>)container).iterator();
            while(iter.hasNext()){
              Object foreachData = iter.next();
              if(foreachData !=null){
                Gen_Content genFor = new Gen_Content(this);
                genFor.localVariables.put(contentElement.name, foreachData);
                genFor.genContent(subContent, userTarget);
              }
            }
          } else {
            if(bWriteErrorInOutput){
              return "<? for container path fault: " + container + "?>";
            } else {
              throw new IllegalArgumentException("container path;"); 
            }
          }
        } break;
        case 'F': { 
          generateIfStatement(contentElement, userTarget);
        } break;
        default: 
          uBuffer.append(" ===ERROR: unknown type '" + contentElement.whatisit + "' :ERROR=== ");
        }//switch
        
      }
      return null;
    }
    
    /**it contains maybe more as one if block and else. */
    void generateIfStatement(ZmakeGenScript.ScriptElement ifStatement, Object userData) throws IOException{
      Iterator<ZmakeGenScript.ScriptElement> iter = ifStatement.subContent.content.iterator();
      boolean found = false;  //if block found
      while(iter.hasNext() && !found ){
        ZmakeGenScript.ScriptElement contentElement = iter.next();
        switch(contentElement.whatisit){
          case 'G': { //if-block
            
            found = generateIfBlock(contentElement, userData);
          } break;
          case 'E': { //elsef
            if(!found){
              genContent(contentElement.subContent, userData);
            }
          } break;
          default:{
            out.append(" ===ERROR: unknown type '" + contentElement.whatisit + "' :ERROR=== ");
          }
        }//switch
      }//for
    }
    
    boolean generateIfBlock(ZmakeGenScript.ScriptElement ifBlock, Object userData) throws IOException{
      Object check = getContent(ifBlock.path, localVariables);
      boolean bCondition = check !=null;
      if(bCondition){
        genContent(ifBlock.subContent, userData);
      }
      return bCondition;
    }
    
  }    
  /**Small class instance to build a next number. 
   * Note: It is anonymous to encapsulate the current number value. 
   * The only one access method is Object.toString(). It returns a countered number.
   */
  private final Object nextNr = new Object(){
    int nr = 0;
    @Override
    public String toString(){
      return "" + ++nr;
    }
  };
  
  void stop(){}

}
