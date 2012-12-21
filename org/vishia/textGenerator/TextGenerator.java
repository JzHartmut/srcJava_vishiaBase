package org.vishia.textGenerator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.vishia.mainCmd.MainCmdLogging_ifc;
import org.vishia.util.Assert;
import org.vishia.util.CalculatorExpr;
import org.vishia.util.DataAccess;

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
   * <li>2012-12-08 Hartmut new: <:subtext:name:formalargs> has formal arguments now. On call it will be checked and
   *   maybe default values will be gotten.
   * <li>2012-12-08 Hartmut chg: {@link #parseGenScript(File, Appendable)}, {@link #genScriptVariables()}, 
   *   {@link #genContent(TextGenScript, Object, boolean, Appendable)} able to call extra especially for Zmake, currDir.
   *   It is possible to define any script variables in the generating script and use it then to control getting data 
   *   from the input data.
   * <li>2012-11-25 Hartmut chg: Now Variables are designated starting with $.
   * <li>2012-11-04 Hartmut chg: adaption to DataAccess respectively distinguish between getting a container or an simple element.
   * <li>2012-10-19 Hartmut chg: <:if...> works.
   * <li>2012-10-10 Usage of {@link TextGenScript}.
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

  
  
  
  //BufferedReader readerScript;
  
  //Appendable out;

  //StringBuilder uLine = new StringBuilder(5000);

  //String sLine;
  
  Object data;
  
  String sError = null;
  
  private boolean bWriteErrorInOutput;
  
  private boolean accessPrivate;
  
  final MainCmdLogging_ifc log;
  
  /**The java prepared generation script. */
  TextGenScript genScript;
  
  /**Instance for the main script part. */
  Gen_Content genFile = new Gen_Content(null);

  private boolean bScriptVariableGenerated;
  
  public TextGenerator(MainCmdLogging_ifc log){
    this.log = log;
  }
  
  
  
  /**Generates a text described with a file given script from any data. This is the main routine of this class.
   * @param userData The data pool where all data are stored
   * @param fileScript The script to generate the text context
   * @param out output for the text
   * @param accessPrivate if true then private data are accessed too. The accessing of private data may be helpfull
   *  for debugging. It is not recommended for general purpose! The access mechanism is given with 
   *  {@link java.lang.reflect.Field#setAccessible(boolean)}.
   * @param testOut if not null then outputs a data tree of the generate script.
   * @return null if no error or an error string.
   */
  public String generate(Object userData, File fileScript, Appendable out, boolean accessPrivate, Appendable testOut){
    String sError = null;
    genScript = parseGenScript(fileScript, testOut);
    if(out !=null){
      try{
        sError = genContent(genScript, userData, accessPrivate, out);
        //out.close();
      } catch(IOException exc){
        System.err.println(Assert.exceptionInfo("", exc, 0, 4));
      }
    }
    //String sError = generator.generate(zbnfResultData, fileScript, fOut, true);
    return sError;
  }
  
  
  
  /**Parses and returns a Java-prepared generation script form a file.
   * @param fileScript The file which contains the script.
   * @param testOut If not null, the content of the generation script will be reported there.
   * @return The generation script ready to use for {@link #genContent(TextGenScript, Object, boolean, Appendable)}.
   */
  public TextGenScript parseGenScript(File fileScript, Appendable testOut)
  {
    genScript = new TextGenScript(log);
    File fileZbnf4GenCtrl = new File("D:/vishia/ZBNF/sf/ZBNF/zbnfjax/zmake/ZmakeGenctrl.zbnf");
    try{ 
      genScript.parseGenCtrl(fileZbnf4GenCtrl, fileScript);
      if(testOut !=null){
        OutputDataTree outputterData = new OutputDataTree();
        outputterData.output(0, genScript, testOut, false);
      }
      //out = new FileWriter(fOut);
    
    } catch(ParseException exc){
      System.err.println(Assert.exceptionInfo("\n", exc, 0, 4));
      
    } catch(FileNotFoundException exc){
      System.err.println(Assert.exceptionInfo("\n", exc, 0, 4));
    } catch(Exception exc){
      System.err.println(Assert.exceptionInfo("\n", exc, 0, 4));
    }
    return genScript;
  }
  
  

  
  
  public Map<String, Object> genScriptVariables() throws IOException
  {
    for(TextGenScript.ScriptElement scriptVariableScript: genScript.getListScriptVariables()){
      StringBuilder uVariable = new StringBuilder();
      Gen_Content genVariable = new Gen_Content(null);
      genVariable.genContent(scriptVariableScript.getSubContent(), uVariable, false);
      genFile.localVariables.put(scriptVariableScript.name, uVariable); //Buffer.toString());
    }
    bScriptVariableGenerated = true;
    return genFile.localVariables;
  }
  

  
  
  
  /**Generates an output with the given script.
   * @param genScript Generation script in java-prepared form. Parse it with {@link #parseGenScript(File, Appendable)}.
   * @param userData The data which are used in the script
   * @param out Any output
   * @return If null, it is okay. Elsewhere a readable error message.
   * @throws IOException only if out.append throws it.
   */
  public String genContent(TextGenScript genScript, Object userData, boolean accessPrivate, Appendable out) 
  throws IOException
  {
    this.accessPrivate = accessPrivate;this.data = userData;
    this.bWriteErrorInOutput = true;
    this.genScript = genScript;

    if(!bScriptVariableGenerated){
      genScriptVariables();
    }
    
    /*
    //the variable (?=currDir?) may exist. Get it:
    sCurrDir = scriptVariables.get("currDir");
    if(sCurrDir == null){
      sCurrDir = "";
    }
    */

    
    
    
    
    TextGenScript.ScriptElement contentScript = genScript.getFileScript();
    String sError = genFile.genContent(contentScript.subContent, out, false);
    return sError;
  }

  
  
  
  
  

  
  private Object getContent(List<DataAccess.DatapathElement> dataRef, Map<String, Object> localVariables, boolean bContainer)
  throws IllegalArgumentException
  { Object dataRet;
    if(dataRef.size() >=1 && dataRef.get(0).ident !=null && dataRef.get(0).ident.equals("$objDirW"))
      Assert.stop();
    try{
      dataRet = DataAccess.getData(dataRef, data, localVariables, accessPrivate, bContainer);
    } catch(NoSuchFieldException exc){
      dataRet = "<? path access: " + dataRef + "on " + exc.getMessage() + "?>";
      if(!bWriteErrorInOutput){
        throw new IllegalArgumentException(dataRet.toString());
      }
    }
    return dataRet;
  }
  

  
  
  /**
   * @param sError
   * @param out
   * @return false to assign to an ok variable.
   * @throws IOException
   */
  boolean writeError(String sError, Appendable out) throws IOException{
    if(bWriteErrorInOutput){
      out.append(sError);
    } else {
      throw new IllegalArgumentException(sError); 
    }
    return false;

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
     * @throws IOException if out throws it.
     */
    public String genContent(TextGenScript.GenContent contentScript, Appendable out, boolean bContainerHasNext) 
    throws IOException
    {
      Appendable uBuffer = out;
      //Fill all local variable, which are defined in this script.
      //store its values in the local Gen_Content-instance.
      for(TextGenScript.ScriptElement variableScript: contentScript.getLocalVariables()){
        StringBuilder uBufferVariable = new StringBuilder();
        Gen_Content genVariable = new Gen_Content(this);
        TextGenScript.GenContent content = variableScript.getSubContent();
        genVariable.genContent(content, uBufferVariable, false);
        //genVariable.gen_Content(uBufferVariable, null, userTarget, variableScript, forElements, srcPath);
        localVariables.put(variableScript.name, uBufferVariable);
      }
    
    
      //Generate direct requested output. It is especially on inner content-scripts.
      for(TextGenScript.ScriptElement contentElement: contentScript.content){
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
            //String val = "" + nextNr.toString();
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
          } else if(contentElement.datapath !=null){
            Object oContent = getContent(contentElement.datapath, localVariables, false);
            text = DataAccess.getStringFromObject(oContent);
            //text = getTextofVariable(userTarget, contentElement.text, this);
            uBuffer.append(text); 
          } else {
            //uBuffer.append(listElement);
          }
        } break;
        /*
        case 'g': {
          final CharSequence text;
          Object oContent = getContent(contentElement, localVariables, false);
          text = DataAccess.getStringFromObject(oContent);
          uBuffer.append(text); 
        } break;
        */
        case 's': {
          genSubtext(contentElement, out);
        } break;
        case 'C': { //generation <:for:name:path> <genContent> <.for>
          TextGenScript.GenContent subContent = contentElement.getSubContent();
          if(contentElement.name.equals("state1"))
            stop();
          Object container = getContent(contentElement.datapath, localVariables, true);
          if(container instanceof String && ((String)container).startsWith("<?")){
            writeError((String)container, out);
          }
          else if(container !=null && container instanceof Iterable<?>){
            Iterator<?> iter = ((Iterable<?>)container).iterator();
            while(iter.hasNext()){
              Object foreachData = iter.next();
              if(foreachData !=null){
                Gen_Content genFor = new Gen_Content(this);
                genFor.localVariables.put(contentElement.name, foreachData);
                genFor.genContent(subContent, out, iter.hasNext());
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
                genFor.genContent(subContent, out, iter.hasNext());
              }
            }
          }
        } break;
        case 'F': { 
          generateIfStatement(contentElement, data, out);
        } break;
        case 'N': {
          generateIfContainerHasNext(contentElement, out, bContainerHasNext);
        } break;
        default: 
          uBuffer.append(" ===ERROR: unknown type '" + contentElement.whatisit + "' :ERROR=== ");
        }//switch
        
      }
      return null;
    }
    
    
    
    void generateIfContainerHasNext(TextGenScript.ScriptElement hasNextScript, Appendable out, boolean bContainerHasNext) throws IOException{
      if(bContainerHasNext){
        (new Gen_Content(this)).genContent(hasNextScript.getSubContent(), out, false);
      }
    }
    
    
    
    /**it contains maybe more as one if block and else. */
    void generateIfStatement(TextGenScript.ScriptElement ifStatement, Object userData, Appendable out) throws IOException{
      Iterator<TextGenScript.ScriptElement> iter = ifStatement.subContent.content.iterator();
      boolean found = false;  //if block found
      while(iter.hasNext() && !found ){
        TextGenScript.ScriptElement contentElement = iter.next();
        switch(contentElement.whatisit){
          case 'G': { //if-block
            
            found = generateIfBlock((TextGenScript.IfCondition)contentElement, out, iter.hasNext());
          } break;
          case 'E': { //elsef
            if(!found){
              genContent(contentElement.subContent, out, false);
            }
          } break;
          default:{
            out.append(" ===ERROR: unknown type '" + contentElement.whatisit + "' :ERROR=== ");
          }
        }//switch
      }//for
    }
    
    
    
    boolean generateIfBlock(TextGenScript.IfCondition ifBlock, Appendable out, boolean bIfHasNext) throws IOException{
      Object check = getContent(ifBlock.datapath, localVariables, false);
      boolean bCondition;
      if(ifBlock.expr == null && ifBlock.condition !=null){
          
        ifBlock.expr = new CalculatorExpr();
        ifBlock.expr.addExprToStack(0, "!");
        ifBlock.expr.addExprToStack(1, ifBlock.condition.name);
      }
      if(ifBlock.expr != null){
        Object cmp = getContent(ifBlock.condition.datapath, localVariables, false);
        CalculatorExpr.Value result = ifBlock.expr.calc(check, cmp);
        bCondition = result.booleanValue();
      } else {
        /*
        if(ifBlock.operator !=null){
          String value = check == null ? "null" : check.toString();
          if(ifBlock.operator.equals("!=")){
            bCondition = check == null || !value.trim().equals(ifBlock.value); 
          } else if(ifBlock.operator.equals("==")){
            bCondition = check != null && value.trim().equals(ifBlock.value); 
          } else {
            writeError(" faulty operator " + ifBlock.operator, out);
            bCondition = false;
          }
          */
        bCondition= check !=null;
      }
      if(bCondition){
        genContent(ifBlock.subContent, out, bIfHasNext);
      }
      return bCondition;
    }
    
    
    
    
    
    
    
    
    
    
    void genSubtext(TextGenScript.ScriptElement contentElement, Appendable out) throws IOException{
      boolean ok = true;
      if(contentElement.name == null){
        Object oName = getContent(contentElement.datapath, localVariables, false);
        contentElement.name = DataAccess.getStringFromObject(oName);
      }
      TextGenScript.ScriptElement subtextScript = genScript.getSubtextScript(contentElement.name);
      if(subtextScript == null){
        ok = writeError("<? *subtext:" + contentElement.name + " not found.?>", out);
      } else {
        Gen_Content subtextGenerator = new Gen_Content(this);
        if(subtextScript.refenceData !=null){
          TreeMap<String, CheckArgument> check = new TreeMap<String, CheckArgument>();
          for(TextGenScript.Arguments formalArg: subtextScript.refenceData) {
            check.put(formalArg.name, new CheckArgument(formalArg));
          }
          List<TextGenScript.Arguments> referenceSettings = contentElement.getReferenceDataSettings();
          if(referenceSettings !=null){
            for( TextGenScript.Arguments referenceSetting: referenceSettings){
              Object ref;
              ref = getContent(referenceSetting.datapath, localVariables, false);
              if(ref !=null){
                CheckArgument checkArg = check.get(referenceSetting.name);
                if(checkArg == null){
                  ok = writeError("<? *subtext;" + contentElement.name + ": " + referenceSetting.name + " faulty argument.?> ", out);
                } else {
                  checkArg.used = true;
                  subtextGenerator.localVariables.put(referenceSetting.name, ref);
                }
              } else {
                ok = writeError("<? *subtext;" + contentElement.name + ": " + referenceSetting.name + " = ? not found. ?>", out);
              }
            }
            //check whether all formal arguments are given with actual args or get its default values.
            //if not all variables are correct, write error.
            for(Map.Entry<String, CheckArgument> checkArg : check.entrySet()){
              CheckArgument arg = checkArg.getValue();
              if(!arg.used){
                if(arg.formalArg.text !=null){
                  subtextGenerator.localVariables.put(arg.formalArg.name, arg.formalArg.text);
                } else if(arg.formalArg.datapath !=null){
                  Object ref = getContent(arg.formalArg.datapath, localVariables, false);
                  if(ref !=null){
                    subtextGenerator.localVariables.put(arg.formalArg.name, ref);
                  } else {
                    ok = writeError("<? *subtext;" + contentElement.name + ": " + arg.formalArg.name + " = ??> not found. ?>", out);
                  }
                } else {
                  ok = writeError("<? *subtext;" + contentElement.name + ": " + arg.formalArg.name + " = ??> missing on call. ?>", out);
                }
              }
            }
          }
        } else if(contentElement.getReferenceDataSettings() !=null){
          ok = writeError("<? *subtext;" + contentElement.name + " called with arguments, it has not one. ?>", out);
        }
        if(ok){
          subtextGenerator.genContent(subtextScript.subContent, out, false);
        }
      }
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
  
  
  /**Class only to check argument lists and use default values for arguments. */
  private class CheckArgument
  {
    /**Reference to the formal argument. */
    final TextGenScript.Arguments formalArg;
    
    /**Set to true if this argument is used. */
    boolean used;
    
    CheckArgument(TextGenScript.Arguments formalArg){ this.formalArg = formalArg; }
  }
  
  
  
  void stop(){}

}
