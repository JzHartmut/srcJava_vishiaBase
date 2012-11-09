package org.vishia.textGenerator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.ParseException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.vishia.mainCmd.MainCmdLogging_ifc;
import org.vishia.mainCmd.Report;
import org.vishia.util.Assert;
import org.vishia.util.DataAccess;
import org.vishia.zmake.ZmakeGenScript;

/**This class helps to generate texts from any Java-stored data controlled with a script. 
 * An instance of this class is used while {@link #generate(Object, File, File, boolean, Appendable)} is running.
 * You should not use the instance concurrently in more as one thread. But you can use this instance
 * for one after another call of {@link #generate(Object, File, File, boolean, Appendable)}.
 * <br><br>
 * The script is a simple text file which contains place holder for data and some control statements
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
   * <li>2012-11-04 Hartmut chg: adaption to DataAccess respectively distinguish between getting a container or an simple element.
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
  
  private boolean accessPrivate;
  
  final MainCmdLogging_ifc log;
  
  ZmakeGenScript genScript;
  
  
  public TextGenerator(MainCmdLogging_ifc log){
    this.log = log;
  }
  
  /**
   * @param userData
   * @param fileScript
   * @param fOut
   * @param accessPrivate if true then private data are accessed too. The accessing of private data may be helpfull
   *  for debugging. It is not recommended for general purpose! The access mechanism is given with 
   *  {@link java.lang.reflect.Field#setAccessible(boolean)}.
   * @param testOut
   * @return
   */
  public String generate(Object userData, File fileScript, File fOut, boolean accessPrivate, Appendable testOut){
    genScript = new ZmakeGenScript(log);
    this.accessPrivate = accessPrivate;
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
  
  
  
  

  
  
  
  
  
  
  private Object getContent(List<String> path, Map<String, Object> localVariables, boolean bContainer)
  throws IllegalArgumentException
  { return DataAccess.getData(path,data, localVariables, bWriteErrorInOutput, accessPrivate, bContainer);
  }
  
  
  
  public String genContent(ZmakeGenScript genScript, Object userData, Writer out) throws IOException{
    this.out = out;
    this.data = userData;
    this.bWriteErrorInOutput = true;
    ZmakeGenScript.ScriptElement contentScript = genScript.getFileScript();
    Gen_Content genFile = new Gen_Content(null);
    String sError = genFile.genContent(contentScript.subContent, false);
    return sError;
  }
    
  
  void writeError(String sError) throws IOException{
    if(bWriteErrorInOutput){
      out.append(sError);
    } else {
      throw new IllegalArgumentException(sError); 
    }
    

  }
  
  
  
  final class Gen_Content
  {
    final Gen_Content parent;
    
    /**Generated content of local variables or reference to any data for this content and all sub contents. */
    Map<String, Object> localVariables = new TreeMap<String, Object>();
    
    
    
    
    public Gen_Content(Gen_Content parent)
    { this.parent = parent;
      if(parent !=null){
        this.localVariables.putAll(parent.localVariables);
      }
    }


  
    /**
     * @param contentScript
     * @param bContainerHasNext Especially for <:for:element:container>SCRIPT<.for> to implement <:hasNext>
     * @return
     * @throws IOException
     */
    public String genContent(ZmakeGenScript.Zbnf_genContent contentScript, boolean bContainerHasNext) 
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
          if(contentElement.text.startsWith("'''trans ==> dst"))
            stop();
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
            Object oContent = getContent(contentElement.path, localVariables, false);
            text = DataAccess.getStringFromObject(oContent);
            //text = getTextofVariable(userTarget, contentElement.text, this);
            uBuffer.append(text); 
          } else {
            //uBuffer.append(listElement);
          }
        } break;
        case 's': {
          genSubtext(contentElement);
        } break;
        case 'C': { //generation (?:for:<$?@name>?) <genContent?> (?/for?)
          ZmakeGenScript.Zbnf_genContent subContent = contentElement.getSubContent();
          if(contentElement.name.equals("dstState"))
            stop();
          Object container = getContent(contentElement.path, localVariables, true);
          if(container instanceof String && ((String)container).startsWith("<?")){
            writeError((String)container);
          }
          else if(container !=null && container instanceof Iterable<?>){
            Iterator<?> iter = ((Iterable<?>)container).iterator();
            while(iter.hasNext()){
              Object foreachData = iter.next();
              if(foreachData !=null){
                Gen_Content genFor = new Gen_Content(this);
                genFor.localVariables.put(contentElement.name, foreachData);
                genFor.genContent(subContent, iter.hasNext());
              }
            }
          }
          else if(container !=null && container instanceof Map<?,?>){
            Map<?,?> map = (Map<?,?>)container;
            Set<?> entries = map.entrySet();
            Iterator<?> iter = entries.iterator();
            while(iter.hasNext()){
              Map.Entry<?, ?> foreachDataEntry = (Map.Entry<?, ?>)iter.next();
              Object foreachData = foreachDataEntry.getValue();
              if(foreachData !=null){
                Gen_Content genFor = new Gen_Content(this);
                genFor.localVariables.put(contentElement.name, foreachData);
                genFor.genContent(subContent, iter.hasNext());
              }
            }
          }
        } break;
        case 'F': { 
          generateIfStatement(contentElement, data);
        } break;
        case 'N': {
          generateIfContainerHasNext(contentElement, bContainerHasNext);
        } break;
        default: 
          uBuffer.append(" ===ERROR: unknown type '" + contentElement.whatisit + "' :ERROR=== ");
        }//switch
        
      }
      return null;
    }
    
    
    
    void generateIfContainerHasNext(ZmakeGenScript.ScriptElement hasNextScript, boolean bContainerHasNext) throws IOException{
      if(bContainerHasNext){
        (new Gen_Content(this)).genContent(hasNextScript.getSubContent(), false);
      }
    }
    
    
    
    /**it contains maybe more as one if block and else. */
    void generateIfStatement(ZmakeGenScript.ScriptElement ifStatement, Object userData) throws IOException{
      Iterator<ZmakeGenScript.ScriptElement> iter = ifStatement.subContent.content.iterator();
      boolean found = false;  //if block found
      while(iter.hasNext() && !found ){
        ZmakeGenScript.ScriptElement contentElement = iter.next();
        switch(contentElement.whatisit){
          case 'G': { //if-block
            
            found = generateIfBlock(contentElement, iter.hasNext());
          } break;
          case 'E': { //elsef
            if(!found){
              genContent(contentElement.subContent, false);
            }
          } break;
          default:{
            out.append(" ===ERROR: unknown type '" + contentElement.whatisit + "' :ERROR=== ");
          }
        }//switch
      }//for
    }
    
    boolean generateIfBlock(ZmakeGenScript.ScriptElement ifBlock, boolean bIfHasNext) throws IOException{
      Object check = getContent(ifBlock.path, localVariables, false);
      boolean bCondition;
      if(ifBlock.operator !=null){
        String value = check == null ? "null" : check.toString();
        if(ifBlock.operator.equals("!=")){
          bCondition = check == null || !value.trim().equals(ifBlock.value); 
        } else if(ifBlock.operator.equals("==")){
          bCondition = check != null && value.trim().equals(ifBlock.value); 
        } else {
          writeError(" faulty operator " + ifBlock.operator);
          bCondition = false;
        }
      } else {
        bCondition= check !=null;
      }
      if(bCondition){
        genContent(ifBlock.subContent, bIfHasNext);
      }
      return bCondition;
    }
    
    
    void genSubtext(ZmakeGenScript.ScriptElement contentElement) throws IOException{
      ZmakeGenScript.ScriptElement subtextScript = genScript.getSubtextScript(contentElement.name);
      if(subtextScript == null){
        writeError("<? *subtext:" + contentElement.name + "> not found.");
      }
      Gen_Content subtextGenerator = new Gen_Content(this);
      for( ZmakeGenScript.DataPath referenceSetting: contentElement.getReferenceDataSettings()){
        Object ref = DataAccess.getData(referenceSetting.path, data, this.localVariables, true, true, false);
        if(ref !=null){
          subtextGenerator.localVariables.put(referenceSetting.name, ref);
        }
      }
      subtextGenerator.genContent(subtextScript.subContent, false);
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
