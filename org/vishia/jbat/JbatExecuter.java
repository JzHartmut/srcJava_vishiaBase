package org.vishia.jbat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.ParseException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;


import org.vishia.cmd.CmdExecuter;
import org.vishia.jbat.JbatGenScript.ZbnfDataPathElement;
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
public class JbatExecuter {
  
  
  /**Version and history
   * <ul>
   * <li>2013-01-13 Hartmut chg: The method getContent is moved and adapted to {@link JbatGenScript.Expression#ascertainValue(Object, Map, boolean, boolean, boolean)}.
   * <li>2013-01-12 Hartmut chg: improvements while documentation. Some syntax details. Especially handling of visibility of variables.
   * <li>2013-01-02 Hartmut chg: The variables in each script part are processed
   *   in the order of statements of generation. In that kind a variable can be redefined maybe with its own value (cummulative etc.).
   *   A ZText_scriptVariable is valid from the first definition in order of generation statements.
   *   But a script-global variable referred with {@link #listScriptVariables} is defined only one time on start of text generation
   *   with the routine {@link JbatExecuter#genScriptVariables(JbatGenScript, Object, boolean)}.  
   * <li>2012-12-23 Hartmut chg: {@link #getContent(org.vishia.jbat.JbatGenScript.Expression, Map, boolean)} now uses
   *   an {@link JbatGenScript.Expression} instead a List<{@link DataAccess.DatapathElement}>. Therewith const values are able to use
   *   without extra dataPath, only with a ScriptElement.
   * <li>2012-12-23 Hartmut new: formatText in the {@link JbatGenScript.Expression#text} if a data path is given, use for formatting a numerical value.
   * <li>2012-12-08 Hartmut new: <:subtext:name:formalargs> has formal arguments now. On call it will be checked and
   *   maybe default values will be gotten.
   * <li>2012-12-08 Hartmut chg: {@link #parseGenScript(File, Appendable)}, {@link #genScriptVariables()}, 
   *   {@link #genContent(JbatGenScript, Object, boolean, Appendable)} able to call extra especially for Zmake, currDir.
   *   It is possible to define any script variables in the generating script and use it then to control getting data 
   *   from the input data.
   * <li>2012-11-25 Hartmut chg: Now Variables are designated starting with $.
   * <li>2012-11-04 Hartmut chg: adaption to DataAccess respectively distinguish between getting a container or an simple element.
   * <li>2012-10-19 Hartmut chg: <:if...> works.
   * <li>2012-10-10 Usage of {@link JbatGenScript}.
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

  protected Object data;
  
  protected String sError = null;
  
  /**Variable for any exception while accessing any java ressources. It is the $error variable of the script. */
  protected String accessError = null;
  
  public final boolean bWriteErrorInOutput;
  
  private boolean accessPrivate;
  
  protected final MainCmdLogging_ifc log;
  
  /**The output which is given by invocation of {@link #generate(Object, File, Appendable, boolean, Appendable)}
   */
  protected Appendable outFile;
  
  /**The java prepared generation script. */
  JbatGenScript genScript;
  
  /**Instance for the main script part. */
  //Gen_Content genFile;

  /**Generated content of all script variables. The script variables are present in all routines 
   * in their local variables pool. The value is either a String, CharSequence or any Object pointer.  */
  final Map<String, Object> scriptVariables = new TreeMap<String, Object>();
  
  private boolean bScriptVariableGenerated;
  
  
  /**The newline char sequence. */
  String newline = "\r\n";
  

  public JbatExecuter(MainCmdLogging_ifc log){
    this.log = log;
    bWriteErrorInOutput = false;
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
  public String generate(Object userData, File fileScript, Appendable out, boolean accessPrivate, File testOut){
    String sError = null;
    this.outFile = out;
    scriptVariables.put("file", outFile);
    JbatGenScript genScript = new JbatGenScript(this, log); //gen.parseGenScript(fileGenCtrl, null);
    try { genScript.translateAndSetGenCtrl(fileScript, testOut);
    } catch (Exception exc) {
      sError = exc.getMessage();
    }
    //genScript = parseGenScript(fileScript, testOut);
    if(sError == null) { // && out !=null){
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
   * @return The generation script ready to use for {@link #genContent(JbatGenScript, Object, boolean, Appendable)}.
   */
  public JbatGenScript XXXparseGenScript(File fileScript, Appendable testOut)
  {
    genScript = new JbatGenScript(this, log);
    File fileZbnf4GenCtrl = new File("D:/vishia/ZBNF/sf/ZBNF/zbnfjax/zmake/ZmakeGenctrl.zbnf");
    try{ 
      genScript.translateAndSetGenCtrl(fileZbnf4GenCtrl, fileScript);
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
  
  

  
  
  /**Generates script-global variables.
   * 
   * @param genScript It should be the same how used on {@link #genContent(JbatGenScript, Object, boolean, Appendable)}
   *   but it may be another one for special cases.
   * @param userData Used userdata for content of scriptvariables. It should be the same how used on 
   *   {@link #genContent(JbatGenScript, Object, boolean, Appendable)} but it may be another one for special cases.
   * @param accessPrivate true than access to private data of userData
   * @return The built script variables. 
   *   One can evaluate some script variables before running {@link #genContent(JbatGenScript, Object, boolean, Appendable)}.
   *   Especially it is used for {@link org.vishia.zmake.Zmake to set the currDir.} 
   * @throws IOException
   */
  public Map<String, Object> genScriptVariables(JbatGenScript genScript, Object userData, boolean accessPrivate) 
  throws IOException
  {
    this.genScript = genScript;
    this.data = userData;
    this.accessPrivate = accessPrivate;
    
    File currDir = new File(".").getAbsoluteFile().getParentFile();
    
    scriptVariables.put("currDir", currDir);
    scriptVariables.put("error", accessError);
    scriptVariables.put("mainCmdLogging", log);
    scriptVariables.put("nextNr", nextNr);
    scriptVariables.put("nrElementInContainer", null);
    scriptVariables.put("out", System.out);
    scriptVariables.put("err", System.err);
    scriptVariables.put("jbatAccess", this);

    for(JbatGenScript.Statement scriptVariableScript: genScript.getListScriptVariables()){
      StringBuilder uVariable = new StringBuilder();
      ExecuteLevel genVariable = new ExecuteLevel(null, scriptVariables); //NOTE: use recent scriptVariables.
      genVariable.execute(scriptVariableScript.getSubContent(), uVariable, false);
      scriptVariables.put(scriptVariableScript.name, uVariable); //Buffer.toString());
    }
    bScriptVariableGenerated = true;
    return scriptVariables;
  }
  

  
  
  
  /**Generates an output with the given script.
   * @param genScript Generation script in java-prepared form. Parse it with {@link #parseGenScript(File, Appendable)}.
   * @param userData The data which are used in the script
   * @param out Any output
   * @return If null, it is okay. Elsewhere a readable error message.
   * @throws IOException only if out.append throws it.
   */
  public String genContent(JbatGenScript genScript, Object userData, boolean accessPrivate, Appendable out) 
  throws IOException
  {
    this.accessPrivate = accessPrivate;
    this.data = userData;
    this.genScript = genScript;

    if(!bScriptVariableGenerated){
      genScriptVariables(genScript, userData, accessPrivate);
    }
    JbatGenScript.Statement contentScript = genScript.getFileScript();
    ExecuteLevel genFile = new ExecuteLevel(null, scriptVariables);
    String sError = genFile.execute(contentScript.subContent, out, false);
    return sError;
  }

  
  public Object getScriptVariable(String name){ return scriptVariables.get(name); }

  
  /**Returns the reference from a given datapath.
   * It can contain only one element which is:
   * <ul>
   * <li>An environment variable: returns its String content. 
   * <li>The designation 'out' or 'err': returns System.out or System.err as Appendable.
   * <li>The designation 'file': returns the main file output as Appendable.
   * </ul>
   * The first element can be
   * <ul>
   * <li>An local variable
   * <li>An element of data1
   * </ul>
   * All other elements are elements inside the found element before.
   * 
   * @param dataPath List of elements of the datapath. 
   * @param data1
   * @param localVariables
   * @param bContainer
   * @return An object.
   * @throws Throwable 
   */
  public Object getDataObj(List<DataAccess.DatapathElement> dataPath
      , Object data1, Map<String, Object> localVariables
      , boolean bContainer)
  throws Throwable
  {  
    Object dataValue = null;
    
    boolean bWriteErrorInOutput = false;
    boolean accessPrivate = true;
    
    if(dataPath.size() >=1 && dataPath.get(0).ident !=null && dataPath.get(0).ident.equals("$checkDeps"))
      Assert.stop();
    //calculate all actual arguments:
    
    //out, err, file for the given Appendable output channels.
    if(dataPath.size()==1){
      DataAccess.DatapathElement dataElement = dataPath.get(0);
      if(dataElement.ident.equals("out")){
        dataValue = System.out;
      }
      else if(dataElement.ident.equals("err")){
        dataValue = System.err;
      }
      if(dataElement.ident.equals("xxxfile")){
        dataValue = null; //outFile;
      }
    }
    if(dataValue ==null){
      for(DataAccess.DatapathElement dataElement : dataPath){  //loop over all elements of the path with or without arguments.
        ZbnfDataPathElement zd;
        if(dataElement instanceof ZbnfDataPathElement && (zd = (ZbnfDataPathElement)dataElement).XXXactualValue !=null){
          //it is a element with arguments, usual a method call. 
          zd.removeAllActualArguments();
          /*
          for(TextGenScript.Argument zarg: zd.actualArguments){
            Object oValue = getContent(zarg, localVariables, false);
            zd.addActualArgument(oValue);
          }
          */
          for(JbatGenScript.Expression expr: zd.XXXactualValue){
            Object oValue = ascertainValue(expr, data1, localVariables, false);
            if(oValue == null){
              oValue = "??: path access: " + dataPath + "?>";
              if(!bWriteErrorInOutput){
                throw new IllegalArgumentException(oValue.toString());
              }
            }
            zd.addActualArgument(oValue);
          }
        }
      }
      try{
        dataValue = DataAccess.getData(dataPath, data1, localVariables, accessPrivate, bContainer);
      } catch(NoSuchMethodException exc){
        dataValue = "??: path not found: " + dataPath + "on " + exc.getMessage() + ".??";
        if(!bWriteErrorInOutput){
          throw new IllegalArgumentException(dataValue.toString());
        }
      } catch(NoSuchFieldException exc){
        dataValue = "??: path not found: " + dataPath + "on " + exc.getMessage() + ".??";
        if(!bWriteErrorInOutput){
          throw new IllegalArgumentException(dataValue.toString());
        }
      } catch(IllegalAccessException exc) {
        dataValue = "??: path access error: " + dataPath + "on " + exc.getMessage() + ".??";
        if(!bWriteErrorInOutput){
          throw new IllegalArgumentException(dataValue.toString());
        }
      } catch(Throwable exc){
        throw exc;
      }
    }
    return dataValue;
  }
  
  
  
  
  /**Ascertains the value which is represented by this expression. 
   * It accessed to data using {@link DataAccess#getData(String, Object, boolean, boolean)}.
   * @param data The data pool to access.
   * @param localVariables additonal container for data references
   * @param accessPrivate
   * @param bContainer true than should return an container.
   * @param bWriteErrorInOutput
   * @return the Object which represents the expression in the given environment.
   * @throws IllegalArgumentException
   */
  Object ascertainValue(JbatGenScript.Expression expr, Object data, Map<String, Object> localVariables
      , boolean bContainer
  )
  throws IllegalArgumentException, IOException, Throwable
  { Object dataRet = null;
    for(JbatGenScript.ZbnfValue value: expr.XXXvalues){   //All SumValue
      List<DataAccess.DatapathElement> dataRef = value.datapath();
      Object dataValue;
      if(dataRef !=null){
        
        dataValue = getDataObj(dataRef, data, localVariables, bContainer);
      } else if(value.genString !=null){
        
        dataValue = new StringBuilder();
        ExecuteLevel subtextGenerator = this.new ExecuteLevel(null, localVariables);
        subtextGenerator.execute(value.genString, (Appendable)dataValue, false);
      
      } else {
        dataValue = value.objValue();
      }
      if(dataRet == null){
        dataRet = dataValue;
      } else {
        //execute operation
        if(dataRet instanceof CharSequence){
          //It is only string concatenation, don't check the operator, '+'or '-' are admissible.
          if(!(dataRet instanceof StringBuilder)){
            dataRet = new StringBuilder((CharSequence)dataRet);
          }
          ((StringBuilder)dataRet).append(dataValue);
        }
        else if(dataRet instanceof Object){
          
        }
      }
    }
    return dataRet;
  }
  

  
  /**ascertains the text which is described in this Expression. Invokes {@link #ascertainValue(Object, Map, boolean, boolean, boolean)}
   * and converts it to String.<br>
   * This method does not support getting from any additional container or from datapool. Only environment variables
   * or invocation of static methods are supported.
   * @return
   */
  public String ascertainText(JbatGenScript.Expression expr){ 
    boolean bContainer = false;
    Map<String, Object> localVariables = null;
    try{ 
      Object value = ascertainValue(expr, data, localVariables, bContainer);
      return DataAccess.getStringFromObject(value, null);
    } catch(IOException exc){
      return "<??IOException>" + exc.getMessage() + "<??>";
    } catch(Throwable exc){
      return "<??Exception>" + exc.getMessage() + "<??>";
      
    }
  }
  

  /**ascertains the text which is described in this Expression. Invokes {@link #ascertainValue(Object, Map, boolean, boolean, boolean)}
   * and converts it to String.<br>
   * This method does not support getting from any additional container or from datapool. Only environment variables
   * or invocation of static methods are supported.
   * @return
   * @throws Throwable 
   * @throws IOException 
   * @throws IllegalArgumentException 
   */
  public String ascertainText(JbatGenScript.Expression expr, Map<String, Object> localVariables) throws IllegalArgumentException, IOException, Throwable{ 
    Object value = ascertainValue(expr, data, localVariables, false);
    return DataAccess.getStringFromObject(value, null);
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
  
  
  
  /**Wrapper to generate a script with specified localVariables.
   * A new Wrapper is created on <:file>, <:subtext> or on abbreviated output, especially to generate into variables.
   *
   */
  public final class ExecuteLevel
  {
    /**Not used yet. Only for debug?
     * 
     */
    final ExecuteLevel parent;
    
    
    /**Generated content of local variables in this nested level including the {@link JbatExecuter#scriptVariables}.
     * The variables are type invariant on language level. The type is checked and therefore 
     * errors are detected on runtime only. */
    final Map<String, Object> localVariables;
    
    
    
    
    /**Constructs data for a local execution level.
     * @param parentVariables if given this variable are copied to the local ones.
     *   They contains the script variables too. If not given (null), only the script variables
     *   are copied into the {@link #localVariables}. Note that subroutines do not know the
     *   local variables of its calling routine! This argument is only set if nested statement blocks
     *   are to execute. 
     */
    public ExecuteLevel(ExecuteLevel parent, Map<String, Object> parentVariables)
    { this.parent = parent;
      localVariables = new TreeMap<String, Object>();
      if(parentVariables == null){
        localVariables.putAll(scriptVariables);
      } else {
        localVariables.putAll(parentVariables);  //use the same if it is not a subText, only a 
      }
      localVariables.put("jbatExecuteLevel", this);
    }

    
    public String executeNewlevel(JbatGenScript.StatementList contentScript, final Appendable out, boolean bContainerHasNext) 
    throws IOException 
    { final ExecuteLevel level;
      if(contentScript.bContainsVariableDef){
        level = new ExecuteLevel(this, localVariables);
      } else {
        level = this;
      }
      return level.execute(contentScript, out, bContainerHasNext);
    }

  
    /**
     * @param contentScript
     * @param bContainerHasNext Especially for <:for:element:container>SCRIPT<.for> to implement <:hasNext>
     * @return
     * @throws IOException 
     */
    public String execute(JbatGenScript.StatementList contentScript, final Appendable out, boolean bContainerHasNext) throws IOException 
    //throws Exception
    {
      String sError = null;
      Appendable uBuffer = out;
      //Generate direct requested output. It is especially on inner content-scripts.
      Iterator<JbatGenScript.Statement> iter = contentScript.content.iterator();
      while(iter.hasNext() && sError == null){
        JbatGenScript.Statement contentElement = iter.next();
        //for(TextGenScript.ScriptElement contentElement: contentScript.content){
        try{    
          switch(contentElement.elementType){
          case 't': { 
            int posLine = 0;
            int posEnd;
            if(contentElement.text.startsWith("'''trans ==> dst"))
              stop();
            do{
              posEnd = contentElement.text.indexOf('\n', posLine);
              if(posEnd >= 0){ 
                uBuffer.append(contentElement.text.substring(posLine, posEnd));   
                uBuffer.append(newline);
                posLine = posEnd +1;  //after \n 
              } else {
                uBuffer.append(contentElement.text.substring(posLine));   
              }
              
            } while(posEnd >=0);  //output all lines.
          } break;
          case 'n': {
            uBuffer.append(newline);
          } break;
          case 'T': {
            if(contentElement.name.equals("file") || contentElement.name.equals("out")){
              //output to the main file, it is the out of this class:
              execute(contentElement.subContent, out, false);  //recursively call of this method.
            } else {
              textAppendToVarOrOut(contentElement); 
            }
          } break;
          case 'V': { //create a new local variable.
            StringBuilder uBufferVariable = new StringBuilder();
            ExecuteLevel genVariable = new ExecuteLevel(this, localVariables);
            JbatGenScript.StatementList content = contentElement.getSubContent();
            genVariable.execute(content, uBufferVariable, false);
            //genVariable.gen_Content(uBufferVariable, null, userTarget, variableScript, forElements, srcPath);
            localVariables.put(contentElement.name, uBufferVariable);
          } break;
          case 'P': { //create a new local variable as pipe
            StringBuilder uBufferVariable = new StringBuilder();
            localVariables.put(contentElement.name, uBufferVariable);
          } break;
          case 'U': { //create a new local variable as pipe
            StringBuilder uBufferVariable = new StringBuilder();
            localVariables.put(contentElement.name, uBufferVariable);
          } break;
          case 'L': {
            Object value = evalObject(contentElement, true); 
              //getContent(contentElement, localVariables, false);  //not a container
            localVariables.put(contentElement.name, value);
            if(!(value instanceof Iterable<?>)) 
                throw new NoSuchFieldException("JbatExecuter - exec variable must be of type Iterable ;" + contentElement.name);
          } break;
          case 'W': executeOpenfile(contentElement); break;
          case 'J': {
            if(contentElement.name.equals("checkDeps"))
              stop();
            Object value = evalObject(contentElement, false);
            localVariables.put(contentElement.name, value);
          } break;
          case 'e': {  //<*datatext>
            final CharSequence text = evalString(contentElement); //ascertainText(contentElement.expression, localVariables);
            uBuffer.append(text); 
          } break;
          case 's': {
            executeSubroutine(contentElement, out);
          } break;
          case 'c': {
            executeCmdline(contentElement);
          } break;
          case 'C': { //generation <:for:name:path> <genContent> <.for>
            executeForContainer(contentElement, out);
          } break;
          case 'B': { //statementBlock
            executeSubLevel(contentElement, out);  ///
          } break;
          case 'F': { 
            executeIfStatement(contentElement, out);
          } break;
          case 'N': {
            executeIfContainerHasNext(contentElement, out, bContainerHasNext);
          } break;
          case '=': executeAssign(contentElement); break;
          case 'b': {
            sError = "break";
          } break;
          default: 
            uBuffer.append(" ===ERROR: unknown type '" + contentElement.elementType + "' :ERROR=== ");
          }//switch
          
        } catch(Throwable exc){
          //check onerror
          boolean found = false;
          if(contentElement.onerror !=null){
            String sError1 = exc.getMessage();
            localVariables.put("errorMsg", sError1);
            Iterator<JbatGenScript.Onerror> iterError = contentElement.onerror.iterator();
            while(!found && iterError.hasNext()) {
              JbatGenScript.Onerror onerror = iterError.next();
              found = onerror.errorType == '?';
              if(found){
                sError = executeSubLevel(onerror, out);
              }
            }
          }
          if(!found){
            sError = exc.getMessage();
            System.err.println("Jbat - execute-exception; " + exc.getMessage());
            //throw exc;
            throw new IllegalArgumentException (exc.getMessage());
          }
        }
      }
      return sError;
    }
    
    
    
    void executeForContainer(JbatGenScript.Statement contentElement, Appendable out) throws Throwable
    {
      JbatGenScript.StatementList subContent = contentElement.getSubContent();  //The same sub content is used for all container elements.
      if(contentElement.name.equals("state1"))
        stop();
      Object container = evalObject(contentElement, true);
      if(container instanceof String && ((String)container).startsWith("<?")){
        writeError((String)container, out);
      }
      else if(container !=null && container instanceof Iterable<?>){
        Iterator<?> iter = ((Iterable<?>)container).iterator();
        ExecuteLevel forExecuter = new ExecuteLevel(this, localVariables);
        //a new level for the for... statements. It contains the foreachData and maybe some more variables.
        while(iter.hasNext()){
          Object foreachData = iter.next();
          if(foreachData !=null){
            //Gen_Content genFor = new Gen_Content(this, false);
            //genFor.
            forExecuter.localVariables.put(contentElement.name, foreachData);
            //genFor.
            forExecuter.execute(subContent, out, iter.hasNext());
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
            //Gen_Content genFor = new Gen_Content(this, false);
            //genFor.
            localVariables.put(contentElement.name, foreachData);
            //genFor.
            execute(subContent, out, iter.hasNext());
          }
        }
      }
    }
    
    
    
    void executeIfContainerHasNext(JbatGenScript.Statement hasNextScript, Appendable out, boolean bContainerHasNext) throws IOException{
      if(bContainerHasNext){
        //(new Gen_Content(this, false)).
        execute(hasNextScript.getSubContent(), out, false);
      }
    }
    
    
    
    /**it contains maybe more as one if block and else. 
     * @throws Throwable */
    void executeIfStatement(JbatGenScript.Statement ifStatement, Appendable out) throws Throwable{
      Iterator<JbatGenScript.Statement> iter = ifStatement.subContent.content.iterator();
      boolean found = false;  //if block found
      while(iter.hasNext() && !found ){
        JbatGenScript.Statement contentElement = iter.next();
        switch(contentElement.elementType){
          case 'G': { //if-block
            
            found = executeIfBlock((JbatGenScript.IfCondition)contentElement, out, iter.hasNext());
          } break;
          case 'E': { //elsef
            if(!found){
              execute(contentElement.subContent, out, false);
            }
          } break;
          default:{
            out.append(" ===ERROR: unknown type '" + contentElement.elementType + "' :ERROR=== ");
          }
        }//switch
      }//for
    }
    
    
    
    boolean executeIfBlock(JbatGenScript.IfCondition ifBlock, Appendable out, boolean bIfHasNext) 
    throws Throwable
    {
      //Object check = getContent(ifBlock, localVariables, false);
      
      Object check;
      try{ 
        check = ifBlock.expression.calcDataAccess(localVariables);
        //check = ascertainValue(ifBlock.expression, data, localVariables, false);
      } catch(Exception exc){
        check = null;
      }
      boolean bCondition;
      if(ifBlock.condition !=null && ifBlock.bElse){ //condition.sumExpression.constValue !=null && ifBlock.condition.sumExpression.constValue.equals("else")){
        bCondition = true;  //if the else block is found, all others have returned false.
      }
      else {
        if(ifBlock.expr == null && ifBlock.condition !=null){
            
          ifBlock.expr = new CalculatorExpr();
          ifBlock.expr.addExprToStack(0, "!");
          ifBlock.expr.addExprToStack(1, ifBlock.condition.name);
        }
        if(ifBlock.expr != null){
          Object cmp = ascertainValue(ifBlock.condition.expression, data, localVariables, false); 
            //getContent(ifBlock.condition, localVariables, false);
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
      }
      if(bCondition){
        execute(ifBlock.subContent, out, bIfHasNext);
      }
      return bCondition;
    }
    
    
    
    
    /**Invocation for <+name>text<.+>
     * @param contentElement
     * @throws IOException 
     */
    void textAppendToVarOrOut(JbatGenScript.Statement contentElement) throws IOException{
      
      String name = contentElement.name;
      Appendable out1;
      Object variable = localVariables.get(name);
      if(variable !=null){
        if(variable instanceof Appendable){
          out1 = (Appendable)variable;
        }
        else if(variable instanceof CharSequence){
          out1 = new StringBuilder(((CharSequence)variable));
        }
        else {
          out1 = null;
        }
      } else {
        out1 = null;
      }
      if(out1 == null){
        
      } else {
        ExecuteLevel genContent = new ExecuteLevel(this, localVariables);
        genContent.execute(contentElement.subContent, out1, false);
        if(out1 instanceof StringBuilder && variable != out1){
          if(variable instanceof String){ //a stored String should be replaced by a String.
            localVariables.put(name, out1.toString());  //replace content.
          }
          else if(variable instanceof CharSequence){
            localVariables.put(name, out1);  //replace content.
          }
        } else {
          //The variable in the container was used, don't put again.
        }
      }
    }
    
    

    
    
    
    
    
    void executeSubroutine(JbatGenScript.Statement contentElement, Appendable out) 
    throws IllegalArgumentException, Throwable
    {
      JbatGenScript.CallStatement callStatement = (JbatGenScript.CallStatement)contentElement;
      boolean ok = true;
      final String nameSubtext;
      /*
      if(contentElement.name == null){
        //subtext name gotten from any data location, variable name
        Object oName = ascertainText(contentElement.expression, localVariables); //getContent(contentElement, localVariables, false);
        nameSubtext = DataAccess.getStringFromObject(oName, null);
      } else {
        nameSubtext = contentElement.name;
      }*/
      nameSubtext = evalString(callStatement.callName); 
      JbatGenScript.Statement subtextScript = genScript.getSubtextScript(nameSubtext);  //the subtext script to call
      if(subtextScript == null){
        throw new NoSuchElementException("JbatExecuter - subroutine not found; " + nameSubtext);
      } else {
        ExecuteLevel subtextGenerator = new ExecuteLevel(this, null);
        if(subtextScript.arguments !=null){
          //build a Map temporary to check which arguments are used:
          TreeMap<String, CheckArgument> check = new TreeMap<String, CheckArgument>();
          for(JbatGenScript.Argument formalArg: subtextScript.arguments) {
            check.put(formalArg.name, new CheckArgument(formalArg));
          }
          //process all actual arguments:
          List<JbatGenScript.Argument> referenceSettings = contentElement.getReferenceDataSettings();
          if(referenceSettings !=null){
            for( JbatGenScript.Argument referenceSetting: referenceSettings){  //process all actual arguments
              Object ref;
              ref = evalObject(referenceSetting, false);
              //ref = ascertainValue(referenceSetting.expression, data, localVariables, false);       //actual value
              if(ref !=null){
                CheckArgument checkArg = check.get(referenceSetting.name);      //is it a requested argument (per name)?
                if(checkArg == null){
                  ok = writeError("??: *subtext;" + nameSubtext + ": " + referenceSetting.name + " faulty argument.?? ", out);
                } else {
                  checkArg.used = true;    //requested and resolved.
                  subtextGenerator.localVariables.put(referenceSetting.name, ref);
                }
              } else {
                ok = writeError("??: *subtext;" + nameSubtext + ": " + referenceSetting.name + " = ? not found.??", out);
              }
            }
            //check whether all formal arguments are given with actual args or get its default values.
            //if not all variables are correct, write error.
            for(Map.Entry<String, CheckArgument> checkArg : check.entrySet()){
              CheckArgument arg = checkArg.getValue();
              if(!arg.used){
                if(arg.formalArg.expression !=null){
                  Object ref = getContent(arg.formalArg, localVariables, false);
                  if(ref !=null){
                    subtextGenerator.localVariables.put(arg.formalArg.name, ref);
                  } else {
                    ok = writeError("??: *subtext;" + nameSubtext + ": " + arg.formalArg.name + " not found.??", out);
                  }
                /*
                if(arg.formalArg.sumExpression.text !=null){
                  subtextGenerator.localVariables.put(arg.formalArg.name, arg.formalArg.sumExpression.text);
                } else if(arg.formalArg.sumExpression.datapath !=null){
                  Object ref = getContent(arg.formalArg, localVariables, false);
                  if(ref !=null){
                    subtextGenerator.localVariables.put(arg.formalArg.name, ref);
                  } else {
                    ok = writeError("??: *subtext;" + nameSubtext + ": " + arg.formalArg.name + " = ??> not found. ?>", out);
                  }
                */  
                } else {
                  ok = writeError("??: *subtext;" + nameSubtext + ": " + arg.formalArg.name + "  missing on call.??", out);
                }
              }
            }
          }
        } else if(contentElement.getReferenceDataSettings() !=null){
          ok = writeError("??: *subtext;" + nameSubtext + " called with arguments, it has not one.??", out);
        }
        if(ok){
          subtextGenerator.execute(subtextScript.subContent, out, false);
        }
      }
    }
    
    
    
    
    /**Generates or executes any sub content.
     * @param script
     * @param out
     * @return
     * @throws IOException
     */
    public String executeSubLevel(JbatGenScript.Statement script, Appendable out) 
    throws IOException
    {
      ExecuteLevel genContent;
      if(script.subContent.bContainsVariableDef){
        genContent = new ExecuteLevel(this, localVariables);
      } else {
        genContent = this;  //don't use an own instance, save memory and calculation time.
      }
      return genContent.execute(script.subContent, out, false);
    }

    
    


    
    void executeCmdline(JbatGenScript.Statement contentElement) 
    throws IllegalArgumentException, Throwable
    {
      boolean ok = true;
      final String sCmd;
      if(contentElement.name == null){
        //cmd gotten from any data location, variable name
        Object oName = ascertainText(contentElement.expression, localVariables);
        sCmd = DataAccess.getStringFromObject(oName, null);
      } else {
        sCmd = contentElement.name;
      }
      String[] args;
      if(contentElement.arguments !=null){
        args = new String[contentElement.arguments.size() +1];
        int iArg = 1;
        for(JbatGenScript.Argument arg: contentElement.arguments){
          String sArg = ascertainText(arg.expression, localVariables);
          args[iArg++] = sArg;
        }
      } else { 
        args = new String[1]; 
      }
      args[0] = sCmd;
      List<Appendable> outCmd;
      if(contentElement.assignObj !=null){
        outCmd = new LinkedList<Appendable>();
        for(JbatGenScript.ZbnfValue assignObj1 : contentElement.assignObj){
          Object oOutCmd = getDataObj(assignObj1.datapath(), data, localVariables, false);
          //Object oOutCmd = localVariables.get(contentElement.sVariableToAssign);
          if(oOutCmd instanceof Appendable){
            outCmd.add((Appendable)oOutCmd);
          } else {
            //TODO error
            //outCmd = null;
          }
        }
      } else {
        outCmd = null;
      }
      CmdExecuter cmdExecuter = new CmdExecuter();
      File currDir = (File)localVariables.get("currDir");
      cmdExecuter.setCurrentDir(currDir);
      cmdExecuter.execute(args, null, outCmd, null);
    }
    

    
    void executeOpenfile(JbatGenScript.Statement contentElement) 
    throws IllegalArgumentException, Throwable
    {
      String sFilename = evalString(contentElement);
      Writer writer = new FileWriter(sFilename);
      localVariables.put(contentElement.name, writer);
    }
    
    /**Executes a <code>assignment::= [{ < datapath?assign > = }] < expression > ;.</code>.
     * If the datapath to assign is only a localVariable (one simple name), then the expression
     * is assigned to this local variable, a new variable will be created.
     * If the datapath to assign is more complex, the object which is described with it
     * will be gotten. Then an assignment will be done depending on its type:
     * <ul>
     * <li>Appendable: appends the gotten expression.toString(). An Appendable may be especially
     * <li>All others cause an error. 
     * </ul>
     * @param contentElement
     * @throws IllegalArgumentException
     * @throws Throwable
     */
    void executeAssign(JbatGenScript.Statement contentElement) 
    throws IllegalArgumentException, Throwable
    {
      Object val = evalObject(contentElement, false);
      //Object val = ascertainValue(contentElement.expression, data, localVariables, false);
      if(contentElement.assignObj !=null){
        for(JbatGenScript.ZbnfValue assignObj1 : contentElement.assignObj){
        
          //It is a path to any object, get it:
          Object oOut = getDataObj(assignObj1.datapath(), data, localVariables, false);
          //
          if(oOut == null){
            //not found.
            if(assignObj1.datapath().size()==1){
              //only a name of a localVariable is given: 
              String name = assignObj1.datapath().get(0).ident;
              localVariables.put(name, val);
            } else {
              throw new NoSuchFieldException("AssignObject not found: " + assignObj1.datapath().toString());
            }
          } else {
            //
            //check its type:
            //
            if(oOut instanceof Appendable){
              ((Appendable)oOut).append(val.toString());
            } else {
              
              throw new NoSuchFieldException("AssignObject type not supported: " + assignObj1.datapath().toString());
            }
          }
        }
      }
      
    }
    
    
    /**Executes any argument or such part.
     * @param arg argument execution script
     * @param localVariables of the environemnt
     * @param bContainer true: expect a container as return, false: returns the first element if a container is found.
     * @return any object. The execution may be a side effect. The goal of this invocation is get data.
     * @throws IllegalArgumentException
     * @throws IOException
     * @throws Throwable
     */
    Object getContent(JbatGenScript.Argument arg, Map<String, Object> localVariables, boolean bContainer)
    throws IllegalArgumentException, IOException, Throwable
    { assert(arg.expression !=null);
      if(arg.expression !=null){
        return ascertainValue(arg.expression, data, localVariables, bContainer);
      } else if(arg.subContent !=null){
        
        StringBuilder buffer = new StringBuilder();
        ExecuteLevel subtextGenerator = new ExecuteLevel(this, localVariables);
        subtextGenerator.execute(arg.subContent, buffer, false);
        return buffer;
      } else {
        return null;
      }
    }
    
    
    public String evalString(JbatGenScript.Argument arg) throws Throwable{
      if(arg.text !=null) return arg.text;
      else if(arg.datapath() !=null){
        Object o = DataAccess.getData(arg.datapath(), null, localVariables, accessPrivate, false);
        return o.toString();
      } else if(arg.subContent !=null){
        StringBuilder u = new StringBuilder();
        executeNewlevel(arg.subContent, u, false);
        return u.toString();
      } else if(arg.expression !=null){
        CalculatorExpr.Value value = arg.expression.calcDataAccess(localVariables);
        return value.stringValue();
      } else throw new IllegalArgumentException("JbatExecuter - unexpected, faulty syntax");
    }
    
    
    
    /**Gets the value of the given Argument. Either it is a 
     * <ul>
     * <li>String from {@link JbatGenScript.Argument#text}
     * <li>Object from {@link JbatGenScript.Argument#datapath()}
     * <li>Object from {@link JbatGenScript.Argument#expression}
     * <li>
     * </ul>
     * @param arg
     * @return
     * @throws Throwable
     */
    public Object evalObject(JbatGenScript.Argument arg, boolean bContainer) throws Throwable{
      Object obj;
      if(arg.text !=null) return arg.text;
      else if(arg.datapath() !=null){
        obj = evalGetData(arg.datapath(), bContainer);
        if(arg.name !=null && arg.name.equals("@info")){
          //Build an information string about the object:
          StringBuilder u = new StringBuilder();
          Class<?> clazz = obj.getClass();
          u.append("<?? info ");
          for(DataAccess.DatapathElement item: arg.datapath()){
            u.append(item.ident).append('.');
          }
          u.append("; Type=");
          u.append(clazz.getCanonicalName());
          u.append("; toString=").append(obj.toString());
          u.append(" ??>\n");
          obj = u.toString();
        }
      } else if(arg.subContent !=null){
        StringBuilder u = new StringBuilder();
        executeNewlevel(arg.subContent, u, false);
        obj = u.toString();
      } else if(arg.expression !=null){
        CalculatorExpr.Value value = arg.expression.calcDataAccess(localVariables);
        obj = value.objValue();
      } else throw new IllegalArgumentException("JbatExecuter - unexpected, faulty syntax");
      return obj;
    }
    
    
    
    /**Prepares the data access. First the arguments should be evaluated if a method will be called.
     * @param datapath
     * @return
     * @throws Throwable
     */
    Object evalGetData(List<DataAccess.DatapathElement> datapath, boolean bContainer)
    throws Throwable {
      for(DataAccess.DatapathElement dataElement : datapath){  
        //loop over all elements of the path to check whether it is a method and it have arguments.
        ZbnfDataPathElement zd = dataElement instanceof ZbnfDataPathElement ? (ZbnfDataPathElement)dataElement : null;
        //A DatapathItem contains the path to parameter.
        if(zd !=null && zd.paramArgument !=null){
          //it is a element with arguments, usual a method call. 
          zd.removeAllActualArguments();
          for(JbatGenScript.Argument expr: zd.paramArgument){
            //evaluate its value.
            Object value = evalObject(expr, bContainer);
            zd.addActualArgument(value);
          }
        }
      }
      //Now access the data.
      Object oVal2 = DataAccess.getData(datapath, null, localVariables, false, bContainer);
      return oVal2;

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
    final JbatGenScript.Argument formalArg;
    
    /**Set to true if this argument is used. */
    boolean used;
    
    CheckArgument(JbatGenScript.Argument formalArg){ this.formalArg = formalArg; }
  }
  
  
  
  void stop(){}

}
