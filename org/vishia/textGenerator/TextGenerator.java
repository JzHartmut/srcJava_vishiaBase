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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;

import org.vishia.util.Assert;

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
  
  private class ForEach{
    StringBuilder lines = new StringBuilder(5000);
    final String sPathIterator;
    
    /**The container for this foreach. */
    Object container;
    
    /**The iterator in the container. */
    Iterator<?> iter;
    
    /**actual element in container while generating foreach. */
    Object data;
    
    ForEach(String sPathIterator, String sPath) throws IOException{
      this.sPathIterator = sPathIterator;
      try{
        container = getContent(sPath);
        if(container == null){
          
        }
        else if(container !=null && container instanceof Iterable<?>){
          iter = ((Iterable<?>)container).iterator();
        } else {
          if(bWriteErrorInOutput){
            out.append("<?error for-container, not a container; path=" + sPath + ">");
          } else {
            sError = "TextGenerator - for-containernot a container; path=" + sPath;
          }
        }
      } catch(IllegalArgumentException exc){
        if(bWriteErrorInOutput){
          out.append("<?error for-container, path=" + sPath + ">");
        } else {
          sError = "TextGenerator - for-container not found; path=" + sPath;
        }
      }
    }
    @Override public String toString(){ return sPathIterator; }
  }
  
  
  BufferedReader readerScript;
  
  Writer out;

  //StringBuilder uLine = new StringBuilder(5000);

  //String sLine;
  
  Object data;
  
  String sError = null;
  
  private boolean bWriteErrorInOutput;
  
  ForEach forEachAct;
  
  final Stack<ForEach> forEaches = new Stack<ForEach>();
  
  
  final Map<String,ForEach> idxForeaches = new TreeMap<String, ForEach>() ;
  
  
  public String generate(Object data, File fileScript, File fileOut, boolean bWriteErrorInOutput){
    this.data = data;
    this.bWriteErrorInOutput = bWriteErrorInOutput;
    try{
      readerScript = new BufferedReader(new FileReader(fileScript));
    } catch(FileNotFoundException exc){
      sError = "TextGenerator - script file not found;" + fileScript.getAbsolutePath();
      return sError;
    }
    try{
      out = new FileWriter(fileOut);
    } catch(IOException exc){
      sError = "TextGenerator - output file not able to open;" + fileOut.getAbsolutePath();
      return sError;
    }
    try{
      generateLines(readerScript);
    } catch(Exception exc){
      sError = Assert.exceptionInfo(exc.getMessage(), exc, 1, 4).toString();
    }
    try{ 
      out.close();
      readerScript.close();
    } catch(IOException exc){ 
      sError = "TextGenerator - close file error;" + exc.getMessage();
    }
    return sError;
  }
  
  
  
  void generateLines(BufferedReader reader){
    String sLine;
    try{
      while((sLine = reader.readLine())!=null && sError == null){
        generateLine(reader, sLine);
      }
    }catch(IOException exc){
      sError = "TextGenerator - any file error;" + exc.getMessage();
    }
  }
  
  
  
  
  public void generateLine(BufferedReader reader, final String sLineInp){
    int pos = 0;
    String sLine = sLineInp;
    int posPlaceholder;
    int posControl;
    try{
      do{
        posPlaceholder = sLine.indexOf("<*", pos);
        posControl = sLine.indexOf("<:", pos);
        if(posPlaceholder >=0 && (posControl < 0 || posPlaceholder < posControl)){
          out.append(sLine.substring(pos, posPlaceholder));
          int posEnd = sLine.indexOf(">", pos);
          replacePlaceholder(sLine.substring(posPlaceholder, posEnd));
          pos = posEnd+1;
          //replace a placeholder
        } else if(posControl >=0){
          out.append(sLine.substring(pos, posControl));
          sLine = processControl(posControl, sLine, reader);
          pos = 0;
        } else {
          out.append(sLine.substring(pos));  //till end.
        }
      } while(posPlaceholder >=0 || posControl >=0);
      out.append("\n");
    } catch(IOException exc){
      sError = "TextGenerator - write error output file;" + exc.getMessage();
    }
  }
  
  
  
  public String processControl(int pos, String sLine, BufferedReader reader) throws IOException
  { String sLineAfterCtrl = "";
    int posEnd = sLine.indexOf(">", pos);
    String sCtrl = sLine.substring(pos, posEnd+1);
    if(sCtrl.startsWith("<:for:")){
      sLineAfterCtrl = processForeach(sLine, pos, sCtrl, reader);
    } else {
      sError = "TextGenerator - unknown control;" + sCtrl;
    }
    return sLineAfterCtrl;
  }
  

  public String processForeach(String sLine, int pos, String sCtrl, BufferedReader reader) throws IOException
  {
    int posSep = sCtrl.indexOf(":", 6);  //after <:for:
    int posEnd = sCtrl.length()-1;
    String sPath = sCtrl.substring(posSep+1, posEnd);
    String sIterVariable = sCtrl.substring(6, posSep);
    ///
    if(forEachAct !=null){ 
      forEaches.push(forEachAct);
    }
    forEachAct = new ForEach(sIterVariable, sPath);
    idxForeaches.put(sIterVariable, forEachAct);
    //search the end in this line or following lines
    
    String sLineAfterCtrl = prepareForeach(sLine, pos, pos + posEnd+1, reader);
    if(forEachAct.iter !=null){
      while(forEachAct.iter.hasNext()){
        BufferedReader readerForeachLines = new BufferedReader(new StringReader(forEachAct.lines.toString()));
        forEachAct.data = forEachAct.iter.next();
        generateLines(readerForeachLines);
      }
    }
    idxForeaches.remove(forEachAct.sPathIterator);
    if(forEaches.size()>0){
      forEachAct = forEaches.pop();
    } else {
      forEachAct = null;
    }
    return sLineAfterCtrl;
  }
  
  
  
  String prepareForeach(String sLine, final int posForeachStart, final int posFirstStart, final BufferedReader reader) throws IOException
  { String sLineAfterForeach = null;
    int posStart = posFirstStart;
    int posSearchNested = posFirstStart;
    int zNestedForEach = 0;
    do{
      int posNestedBlock = sLine.indexOf("<:for", posSearchNested);
      int posEndBlock = sLine.indexOf("<.for>", posStart);
      if(posNestedBlock >= 0 && (posEndBlock < 0 || posNestedBlock < posEndBlock)){
        zNestedForEach +=1;
      }
      if(posEndBlock >=0){ // && (posNestedBlock < 0 || posNestedBlock > posEndBlock)){
        //end found.
        if(zNestedForEach>0){
          zNestedForEach -=1;
          forEachAct.lines.append(sLine.substring(posStart, posEndBlock+6));
          posStart = posEndBlock+6;
        } else {
          forEachAct.lines.append(sLine.substring(posStart, posEndBlock));
          sLineAfterForeach = sLine.substring(posEndBlock + 6);
        }
      } else {
        //transfer rest of first line or whole line:
        forEachAct.lines.append(sLine.substring(posStart)).append("\n");
        if((sLine = reader.readLine())==null){
          sLineAfterForeach = "";
        } else { 
          posStart = 0;
        }
      }
      posSearchNested = 0;
    } while(sLineAfterForeach == null);
    return sLineAfterForeach;
  }
  
  
  
  public void replacePlaceholder(String sPlaceholder) throws IOException{
    String sPath = sPlaceholder.substring(2);
    try{ 
      Object content = getContent(sPath);
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
      out.append(sContent);
    } catch(IllegalArgumentException exc){
      if(bWriteErrorInOutput){
        out.append("<?error path=" + sPath + ">");
      } else {
        sError = "TextGenerator - data path error;" + sPath;
      }
    }
  }
  
  
  /**Read content from data.
   * @param sPath
   * @return
   * @throws IllegalArgumentException
   */
  private Object getContent(String sPath)
  throws IllegalArgumentException
  {
    String[] paths = sPath.split("\\.");
    Class<?> clazz1;
    Object data1 = data;
    ForEach forVariable = idxForeaches.get(paths[0]);
    int ixPath;
    if(forVariable !=null){
      data1 = forVariable.data;
      ixPath = 1;
    } else {
      data1 = this.data;
      ixPath = 0;
    }
    while(ixPath < paths.length && data1 !=null){
      String sElement = paths[ixPath++];
      try{ 
        clazz1 = data1.getClass();
        Field element = clazz1.getDeclaredField(sElement);
        try{ data1 = element.get(data1);
        
        } catch(IllegalAccessException exc){ 
          throw new IllegalArgumentException("IllegalAccessException, hint: should be public;" + sElement); 
        }
      
      } catch(NoSuchFieldException exc){
        //TODO method
        throw new IllegalArgumentException(sElement);
      }
    }
    return data1;
  }
  
  
}
