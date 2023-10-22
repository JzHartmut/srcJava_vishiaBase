package org.vishia.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;





/**This class helps to prepare output texts with data.
 * It is a more powerful text preparer in comparison to {@link java.io.PrintStream#printf(String, Object...)}
 * or {@link String#format(String, Object...)}. The first ones are only for number and date formatting.
 * This class allows access to all Java data as placeholder values inclusively conditions and loops. 
 * <ul> 
 * <li>It may be seen as small solution in comparison to {@link org.vishia.jztxtcmd.JZtxtcmd} only for text preparation of one or a few lines. 
 * <li>In opposite to {@linkplain https://www.eclipse.org/xtend/} resolution for ''' TEXTs ''':
 *  The rule to build the text is given outside the programming language, for example in a text file able to control by a user.
 *  It is resolved on runtime, not on compile time. Hence it is able to use to control several texts by user.
 * <li>It may be seen as a better 'printf'. 'printf' is a text interpreter, in C too, but with lesser capability.
 * </ul>
 * An instance of this class is used as formatter for an output text. 
 * The pattern to format can be given as String for the constructor or also as text file 
 * using {@link #readTemplateCreatePreparer(InputStream, String, Class, Map, String)}
 * in the following forms (example, text file via readTemplate...(): 
 * <pre>
 * 
 * <:otx: otxExample : data: placeholder >
 * Output plain text with <&placeholder.value>
 * <:if:data.cond>conditional output<:else>other variant<.if>
 * <:for:var:data.container>element = <&var><.for>
 * <:call:subtext:arg=data.var>
 * <:exec:operation:data>
 * <.otx>
 * </pre>
 * To execute this script you should call (see {@link #createArgumentDataObj()} and {@link #exec(Appendable, DataTextPreparer)}: 
 * <pre>

StringBuilder sb = new StringBuilder(500);    // for the output text
try {
  OutTextPreparer.DataTextPreparer args = mainOtx.createArgumentDataObj();
  args.setArgument("data", myData);           // The data class for access.
  args.setArgument("placeholder", "any test text");
  args.setExecObj(myExecObj)                  // for <:exec...>
  mainOtx.exec(sb, args);                     // execute the outText script 
} catch(Exception exc) {                      // unexpected exceptions can be occurred, catch it!
  CharSequence sExc = org.vishia.util.ExcUtil.exceptionInfo("unexpected", exc, 0, 10);
  System.err.println(sExc);
}    
 * </pre>
 * <b>Arguments</b><br>
 * The arguments are simple identifier in the 3th argument of the constructor or in the given argument list
 * in an textual script (see {@link #readTemplateCreatePreparer(InputStream, String, Class, Map, String)}).
 * or also possible on start of the script as <code>&lt;:args:var, var2></code>
 * <br>
 * Its values are set in the instance which should be gotten via {@link #createArgumentDataObj()}.
 * One of the importants is:
 * <ul><li>The translated script of an OutTextPreparer after construction is given and persistent.
 *   It is never changed via {@link #exec(Appendable, DataTextPreparer)}. 
 *   That means also, it can be used concurrently in several threads.
 * <li>The {@link DataTextPreparer} which are created by call of {@link #createArgumentDataObj()}
 *   should be used in an execution thread, the data should be set due to the after called {@link #exec(Appendable, DataTextPreparer)}. 
 *   If executions of the same text pattern are executed in several threads, you can have only one reused instance
 *   of the OutTextPreparer but several instances of {@link DataTextPreparer}, each for one execution thread.
 *   You can reuse this instance with {@link #exec(Appendable, DataTextPreparer)} invocations one after another in the same thread.
 * </ul>
 * The instance of the {@link DataTextPreparer} must be matching to the OutputTextPreparer
 * because internally only integer numbers are used for access.
 * <br> 
 * To assign data to the named arguments use the {@link DataTextPreparer#setArgument(int, Object)}.
 * That are your user data which can be accessed via <code><&arg1....></code> in your pattern. 
 * Compare it with the arguments of a printf, but with the difference that the possibilities to use the arguments
 * are some more powerful, not only for numbers and date. 
 * But possibilities for number formatting are not given yet (maybe interesting, in future).
 * For number formatting just call your java operations.
 * 
 * <br>
 * <br>
 * <b>General to text and placeholder</b><br>
 * All plain text is output as given. Only a few character sequences determine a placeholder
 * with a processing functionality for data preparation and output. This sequences are only:
 * <ul>
 * <li><code>&lt;&</code> start of a data access till <code> ... ></code>
 * <li><code>&lt;:</code> start for controlling statements till <code> ... ></code> but with:
 * <li><code>&lt;.</code> end of controling statements till <code> ... ></code>
 * </ul>
 * To output exact this character sequences (for example to produce another OutTextPreparer script)
 * you should use the capapbility <code>&lt;:SPECIAL TEXT></code> for example <code>&lt;:&lt;&></code>
 * to output a <code>&lt;&</code> itself. See next list.
 * <ul> 
 * <li><code>&lt;: ></code> skips over all white spaces till the next placeholder 
 * <li><code>&lt;: ></code> second time after a <code>&lt;: ></code> stops skipping withspaces but outputs a space. 
 * <li><code>&lt;:?nl></code> skips till the next line (skips over the newline character in the script) (since 2023-05)
 * <li><code>&lt;:? ></code>  skips over all white spaces till the next placeholder (since 2023-05)
 * <li><code>&lt;:s></code> outputs a space (since 2023-05, alternatively usable to a second <code>&lt;: ></code> )
 * <li><code>&lt;:n></code> outputs a newline as 0x0a
 * <li><code>&lt;:r></code> outputs a newline as 0x0d
 * <li><code>&lt;:t></code> outputs a tabulator as 0x09
 * <li><code>&lt;:x1234></code> outputs the UTF16 character with the given hexa code, here 1234 
 * <li><code>&lt;:CHARS> CHARS</code> may be any special character sequences, they will be output as given, for example:
 * <li><code>&lt;:&lt;&var>></code> produces the text <code>&lt;&var></code> for generate a OutTextPreparer-Script itself.
 * <li><code>&lt;:&lt;&>var></code> produces the same <code>&lt;&var></code>, other writing style.
 * <li><code>&lt;:&lt;&>&lt;&var>></code> produces the text <code>&lt;&CONTENT></code> 
 *   whereby <code>CONTENT</code> is the content of <code>var</code>.
 *   The truth is: <code> &lt;:&lt;&></code> procuces the <code> &lt;&</code>. 
 *   Then <code>&lt;&var></code> generates the content of the variable, then a simple <code>></code> follows in the text.
 * </ul>
 * Generally, the plain text outside the placeholder syntax  is read without skipping white spaces,
 * hence this special features helps to structure this parts also with white spaces and line breaks.
 * The syntax inside <code>&lt;&...></code> and <code>&lt;:...></code> is white space compliant. 
 * <br>
 * Sometimes the script should be written in several lines, but only one line should be output.
 * for example data from a container should be output, one per line. 
 * For that you can write (example) (since 2023-05):<pre>
 * This is a title line
 * <:for:element:container><:?nl>
 *     <&element.data> written with an indentation
 * <.for><: >
 * </pre>
 * In this script the line for data output should start with a new line, but the necessary first newline 
 * is already given after the title line. The newline after the <code>&lt;:for...></code> line should not be outputted.
 * But in the script the output line should be written as extra line. 
 * For that the <code>&lt;:?nl></code> skips over the next newline in the script.
 * Before 2023.05 the syntax was a little bit more complicated, that runs furthermore: <pre>
 * This is a title line
 * <:for:element:container><: >
 * <: >    <&element.data> written with an indentation
 * <.for><: >
 * </pre>
 * 
 * <br>
 * <br>
 * <b>Placeholder values and expression evaluation via reflection</b><br>
 * A simple value is immediately gotten from the argument as set via {@link DataTextPreparer#setArgument(int, Object)}.
 * or also as constant argument given in the 'execClass' or 'idxConstData' given on construction. 
 * This is a fast access, only to an container {@link TreeMap} or via indexed array access to the arguments.
 * <br><br>
 * If the <code>&lt;&data.element></code> is a more complex access expression 
 * then the capability of {@link DataAccess} is used. This accesses the elements via reflection.
 * It means the given {@link Class#getDeclaredFields()}, {@link Class#getDeclaredMethods()} etc. are used
 * to get the data, accessed via {@link java.lang.reflect.Field} etc. 
 * The access is completely controlled in the {@link DataAccess} class. For usage it is simple.
 * <br><br>
 * The <code>&lt;&data.operation(arg) + otherdata></code> can be also a complex expression for numeric calculation,
 * String concatenation and boolean evaluation. This expression is executed in {@link CalculatorExpr}
 * with an interpreted approach. 
 * It means the calculation needs a longer time then executed in the Java-VM per bytecode/JIT machine code. 
 * But you can also prepare complex expressions by Java programming and call this expressions or operations.
 * For that the 'execClass' class can be used which may contain prepared operations for your own. 
 * Hence you can decide writing simple expressions or also more complex as script or just as Java operations.
 * <br>
 * <br>
 * <b>Control statements</b><br>
 * It is interesting and important to produce an output conditional depending from data, 
 * and also from some container classes.
 * <ul> 
 * <li><code>&lt;if:condition>conditional Text&lt;elsif:condition>other Text&lt;:else>else-Text&lt;.if></code>
 * The condition is an expression built with the {@link CalculatorExpr#setExpr(StringPartScan, Map, Class, boolean)}
 * <br>
 * for example also a type check is possible: <code><:if:obj ?instanceof classXyz></code>
 * whereas the <code>classXyz</code> can be given in the static reflection class as static variable as
 * <code>public static Class<?> classXyz = MyClassXyz.class; </code> 
 * <li><code>&lt;for:variable:container>text for any element &lt;&variable.element> in loop &lt;:if:variable_next>, &lt;.if>&lt;.for></code><br>
 *   ##The next variable is also present, here to test whether a separator character should be output. 
 * </ul>
 * <br>
 * <br>
 * <b>Call operations <code><:call:otxSubScript:arg=value:arg2=value,...></code></b><br>
 * The call operation invokes another script to output. The script is present with its own instance of {@link OutTextPreparer},
 * either manually programmed by the constructor or as part of the whole script see {@link #readTemplateCreatePreparer(InputStream, String, Class, Map, String)}.
 * <br>
 * The name of the 'otxScript' should be either found in the 'idxConstData' on construction or call of readTemplate...
 * <pre>
 * <:otx: otxSubScript : arg1: arg2>
 * Any script pattern<.end>
 * </pre>
 * or it should be found as static instance of a programmed OutTextPreparer in the 'execClass' given on construction:
 * <pre>
 * 
  static final OutTextPreparer otxListColors = new OutTextPreparer("otxListColors"
  , null            //no static data on construction
  , "colors, text"  //arguments need and used
  , "<&text>: <:for:color:colors><&color><:if:color_next>, <.if><.for>");  //The pattern.
 * </pre>
 * The association of arguments to the subscript argument variables is done on call from given values.
 * <br>
 * <br>
 * <b>Execute operations in one given class</b><br>
 * The 'execClass' as second argument of the constructor or the 3th argument of {@link #readTemplateCreatePreparer(InputStream, String, Class, Map, String)}
 * gives the possibility to have some static operations to call immediately maybe related to the script.
 * The operations should be static because there are given in the script persistent without data.
 * But they can have data arguments of course. That is powerful because you can use specific parts programmed in Java
 * in your script.
 * <br>
 * Call that operations with <code><:exec:operation:arg1:arg...></code>. 
 * <br>It is similar to a data access written as <code><&path.to.StaticClass.operation(arg1,arg...)</code> 
 * but it is faster parsed and executed due to the immediately given 'execClass'.
 * <br><br>
 * <b><code>&lt;:exec:operation:arg></code></b>:<br>
 * The <code>:arg</code> is optional. The <code>operation</code> will be searched in the given reflection class (ctor argument).
 * The operation is stored as {@link Method} for usage, so this operation is fast. 
 * The optional argument is used from given arguments of call, it should be match to the operations argument type.
 * If the operation is not found, an exception with a detailed error message is thrown on ctor. 
 * If the operations argument is not proper, an exception is thrown on {@link #exec(Appendable, DataTextPreparer)}.
 * <br><br>
 * @author Hartmut Schorrig
 *
 */
public class OutTextPreparer
{

  /**Version, history and license.
   * <ul>
   * <li>2023-10-22 Using of {@link #idxScript}, new {@link #readTemplateCreatePreparer(InputStream, Class, Map)}. 
   * <li>2023-08 up to now history to the operations 
   * <li>2023-06-18: frame of operation now <:otx:args>...<.otx>. The <.end> is no more supported, change it to <.otx>
   *   Usage of "=== name ( args) " is deprecated but yet still possible. 
   * <li>2023-06-18: bugfix on nested <:if>...<.if> 
   * <li>2023-05-16: the evaluation of a simple variable is now more simple organized 
   *   in {@link #addCmdSimpleVar(String, int, int, ECmd, String, Class)}. 
   *   This allows now also using the 'idxConstValues' for const texts.
   * <li>2023-05-16: capability of <:x1234> any UTF16 char, not tested yet, should run
   * <li>2023-05-16: some documentation and renaming: 'execClass' instead 'reflData',
   *   {@link #execClass} instead 'clazzPattern'   
   * <li>2023-05-15: new Features <:? > as better syntax for skip spaces, <:?nl> skip after newline. 
   * <li>2023-05-12: new  {@link #readTemplateCreatePreparer(InputStream, String, Class, Map, String)} 
   *   now works completely with a String file given script.
   * <li>2022-04-17: new {@link #readTemplate(InputStream, String)} to support texts from file. Used firstly for {@link org.vishia.java2Vhdl.Java2Vhdl}
   *   This is 2023-05-12 deprecated because {@link #readTemplateCreatePreparer(InputStream, String, Class, Map, String)} does all and replaces it.
   * <li>2022-02-11: little bit improved error message, hint to error position
   * <li>2019-11-13: ## Comment in a line
   * <li>2019-10-20: &lt;: > capability 
   * <li>2019-08-26: StringPartScan instead String for {@link CalculatorExpr.Operand#Operand(StringPartScan, Map, Class, boolean)}
   *   yet not complete.
   * <li>2019-08-26: &lt;:args:....>  
   * <li>2019-05-08: Creation
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
   */
  public static final String version = "2023-10-22";
  
  
  /**Instances of this class holds the data for one OutTextPreparer instance but maybe for all invocations.
   * An instance can be gotten from {@link OutTextPreparer#createArgumentDataObj()} proper and associated to the out text.
   * The constructor of this class is not public, it should be only gotten with this routine.
   * <br><br>
   * Invoke {@link #setArgument(String, Object)} to set any named argument. 
   *
   */
  public static class DataTextPreparer {
    
    /**The associated const data for OutText preparation used for {@link #setArgument(String, Object)} by name. */
    final OutTextPreparer prep;
    
    /**The instance where the &lt;:exec:operation...> are located. null if not necessary. */
    Object execObj;
    
    /**Array of all arguments. It is sorted to the {@link OutTextPreparer#nameVariables} with its value {@link DataAccess.IntegerIx}. 
     */
    Object[] args;
    
    /**Any &lt;call in the pattern get the data for the called OutTextPreparer, but only ones, reused. */
    DataTextPreparer[] argSub;
    
    /**Set on first usage. */
    CalculatorExpr.Data calcExprData;
    
    
    public String debugOtx;
    
    public int debugIxCmd;
    
    
    /**Package private constructor invoked only in {@link OutTextPreparer#createArgumentDataObj()}*/
    DataTextPreparer(OutTextPreparer prep){
      this.prep = prep;
      if(prep.nameVariables.size() >0) {
        this.args = new Object[prep.nameVariables.size()];
      }
      if(prep.ctCall >0) {
        this.argSub = new DataTextPreparer[prep.ctCall];
      }
      
    }
    
    
    
    /**User routine to set a named argument with a value.
     * If a faulty name is used, an Exception is thrown 
     * @param name argument name of the {@link OutTextPreparer#OutTextPreparer(String, Object, String, String)}, 3. argment
     * @param value any value for this argument.
     * */
    public void setArgument(String name, Object value) {
      DataAccess.IntegerIx ix0 = prep.nameVariables.get(name);
      if(ix0 == null) throw new IllegalArgumentException("OutTextPreparer script " + prep.sIdent + ", argument: " + name + " not existing: ");
      int ix = ix0.ix;
      args[ix] = value;
    }
    
  
    /**User routine to set a argument in order with a value.
     * @param name argument name of the {@link OutTextPreparer#OutTextPreparer(String, Object, String, String)}, 3. argment
     * @param value any value for this argument.
     * */
    public void setArgument(int ixArg, Object value) {
      this.args[ixArg] = value;
    }
    
    
    /**Sets an instance due to the given 'execClass' for the script.
     * @param data The instance must be proper to the 'execClass' argument of the constructors
     *   respectively to the {@link OutTextPreparer#readTemplateCreatePreparer(InputStream, String, Class, Map, String).}
     *   If it does not match, an exception is thrown while processing a non static <code><:exec:operation:...></code>
     */
    public void setExecObj(Object data) { this.execObj = data; }
  
    
    
    /**Sets a debug point. You have to set a breakpoint on usage position. It is only for internal debug.
     * @param patternName Because the debug info is copied to called pattern, the name of the called pattern is possible here.
     * @param ixCmd Use 0 to stop in the start cmd, see the commands with data debug, then decide.
     */
    public void setDebug(String patternName, int ixCmd) {this.debugOtx = patternName; this.debugIxCmd = ixCmd;} 
    
  }

  enum ECmd{
    nothing('-', "nothing"), 
    addString('s', "str"),
    addVar('v', "var"),
    ifCtrl('I', "if"),
    elsifCtrl('J', "elsif"),
    elseCtrl('E', "else"),
    forCtrl('F', "for"),
    setVar('S', "set"),
    endLoop('B', "endloop"),
    call('C', "call"),
    exec('E', "exec"),
    debug('D', "debug"),
    pos('p', "pos")
    ;
    ECmd(char cmd, String sCmd){
      this.cmd = cmd; this.sCmd = sCmd;
    }
    char cmd;
    String sCmd;
    @Override public String toString() { return sCmd; }
  }
  
  
  


  
  
  static class Cmd extends CalculatorExpr.Operand{
    public final ECmd cmd;
    
    /**If necessary it is the offset skipped over the ctrl sequence. */
    public int offsEndCtrl;
    
    public Cmd(ECmd what, String textOrDatapath)
    { super( textOrDatapath);
      this.cmd = what;
    }
    
    
    public Cmd(ECmd what, Object value)
    { super( value);
      this.cmd = what;
    }
    
    
    /**A universal constructor for all which is not an Expression. 
     * @param what should be defined
     * @param ixValue -1 or index to arguments
     * @param dataAccess null or a data access path
     * @param dataConst null or a given object, maybe especially a {@link CalculatorExpr.Value}
     * @param textOrVar either the only one given String (valid) or a String info for debug.
     */
    public Cmd (ECmd what, int ixValue, DataAccess dataAccess, Object dataConst, String textOrVar) {
      super(ixValue, dataAccess, dataConst, textOrVar);
      this.cmd = what;
    }
    
    public Cmd ( OutTextPreparer outer, ECmd what, StringPartScan textOrDatapath, Class<?> reflData) throws Exception { 
      super( checkVariable(outer, textOrDatapath), outer.nameVariables, reflData, true);
      this.cmd = what;
    }
    
    public Cmd ( OutTextPreparer outer, ECmd what, String textOrDatapath, Class<?> reflData, Map<String, Object> idxConstData) throws Exception { 
      super( textOrDatapath, outer.nameVariables, reflData, idxConstData);
      this.cmd = what;
    }
    
    public Cmd ( OutTextPreparer outer, ECmd what, StringPartScan textOrDatapath, Class<?> reflData, Map<String, Object> idxConstData) throws Exception { 
      super( textOrDatapath, outer.nameVariables, reflData, idxConstData, true);
      this.cmd = what;
    }
    
    
    private static StringPartScan checkVariable(OutTextPreparer outer, StringPartScan textOrDatapath) {
      if(outer.listArgs == null) { //variables are not given before parse String
        //char c0 = textOr
      }
      return textOrDatapath;
    }
    
    
    @Override public String toString() {
      return cmd + ":" + textOrVar;
    }
    
  }//sub class Cmd
  
  
  
  static class ForCmd extends Cmd {
    
    /**The index where the entry value is stored while executing. 
     * Determined in ctor ({@link OutTextPreparer#parse(String, Object)} */
    public int ixEntryVar, ixEntryVarNext;

    public ForCmd(OutTextPreparer outer, StringPartScan spDatapath, Class<?> reflData) throws Exception {
      super(outer, ECmd.forCtrl, spDatapath, reflData);
    }

    public ForCmd(OutTextPreparer outer, String sDatapath, Class<?> reflData) throws Exception {
      super(outer, ECmd.forCtrl, sDatapath, reflData, null);
    }
  }
  
  
  static class SetCmd extends Cmd {
    
    /**The index where the entry value is stored while executing. 
     * Determined in ctor ({@link OutTextPreparer#parse(String, Object)} */
    public int ixVariable;

    public SetCmd(OutTextPreparer outer, StringPartScan spDatapath, Class<?> reflData) throws Exception {
      super(outer, ECmd.setVar, spDatapath, reflData);
    }

    public SetCmd(OutTextPreparer outer, String sDatapath, Class<?> reflData) throws Exception {
      super(outer, ECmd.setVar, sDatapath, reflData, null);
    }
  }
  
  
  static class IfCmd extends Cmd {
    
    /**The offset to the next &lt;:elsif or the following &lt;:else or the following &lt;.if*/
    int offsElsif;
    
    /**The expression for a comparison. It is null if only a null check of the first argument should be done.
     * For comparison with a constant text, the constant text is contained in expr */
    //CalculatorExpr expr;
    
    /**One of character from "=!enc" for equal, non equal, starts, ends, contains or '\0' for non compare. */
    //char cmp;
    
    /**The value to compare with. 
     * @throws Exception */
    //CalculatorExpr.Operand valCmp;
    
    public IfCmd(OutTextPreparer outer, ECmd cmd, StringPartScan sDatapath, Class<?> reflData) throws Exception {
      super(outer, cmd, sDatapath, reflData);
    }

    
    public IfCmd(OutTextPreparer outer, ECmd cmd, String sDatapath, Class<?> reflData) throws Exception {
      super(outer, cmd, sDatapath, reflData, null);
    }
  }
  
  
  static class CallCmd extends Cmd {
    
    /**Index for {@link DataTextPreparer#argSub} to get already existing data container. */
    public int ixDataArg;
    
    /**The data to get actual arguments for this call. */
    public List<Argument> args;
    
    public CallCmd(OutTextPreparer otxSub) {
      super(ECmd.call, otxSub);
    }
    
//    public CallCmd(OutTextPreparer outer, StringPartScan spDatapath, Class<?> reflData) throws Exception 
//    { super(outer, ECmd.call, spDatapath, reflData, outer.idxConstData); 
//    }
// 
//    public CallCmd(OutTextPreparer outer, String sDatapath, Class<?> reflData) throws Exception { 
//      super(outer, ECmd.call, sDatapath, reflData, outer.idxConstData); 
//    }                                              // search the call object also in callScripts
  }
  
  
  static class ExecCmd extends Cmd {
    
    /**Index for {@link DataTextPreparer#argSub} to get already existing data container. */
    public Method execOperation;
    
    
    public ExecCmd(OutTextPreparer outer, StringPartScan spDatapath, Class<?> reflData) throws Exception 
    { super(outer, ECmd.exec, spDatapath, reflData); 
    }
    
    public ExecCmd(OutTextPreparer outer, String sDatapath, Class<?> reflData) throws Exception 
    { super(outer, ECmd.exec, sDatapath, reflData, outer.idxConstData); 
    }
  }
  
  

  static class DebugCmd extends Cmd {
    public String cmpString;

    public DebugCmd(OutTextPreparer outer, StringPartScan spDatapath, Class<?> reflData) throws Exception {
      super(outer, ECmd.debug, spDatapath, reflData);
    }

    public DebugCmd(OutTextPreparer outer, String sDatapath, Class<?> reflData) throws Exception {
      super(outer, ECmd.debug, sDatapath, reflData, outer.idxConstData);
    }
  }
  
  
  
  static class CmdString extends Cmd {
    public CmdString(String str)
    { super(ECmd.addString, str);
    }

  };
//  
//  
//  
//  static class CmdVar extends Cmd {
//    String sVar;
//    public CmdVar(String str)
//    { super(str, 0,0);
//      sVar = str;
//    }
//
//    public void exec() {
//      
//      fm.add(str.substring(pos0, pos1));
//    }
//  };
  
  
  static class Argument extends CalculatorExpr.Operand {

    /**Name of the argument, null for usage in {@link Cmd} */
    public final String name;
    
    /**The Index to store the value in {@link DataTextPreparer#args} of the called level or -1 if not known. */
    public final int ixDst;
    
    public Argument(OutTextPreparer outer, String name, int ixCalledArg, String sTextOrDatapath, Class<?> reflData) throws Exception {
      super(sTextOrDatapath, outer.nameVariables, reflData, outer.idxConstData);
      this.name = name;
      this.ixDst = ixCalledArg;
    }
    
    public Argument(String name, int ixCalledArg, Object dataConst, DataAccess datapath, String sText) throws Exception {
      super(-1, datapath, dataConst, sText);
      this.name = name;
      this.ixDst = ixCalledArg;
    }
    
  }
  
  
  /**All argument variables and internal variables sorted. */
  private Map<String, DataAccess.IntegerIx> nameVariables = new TreeMap<String, DataAccess.IntegerIx>();
  
  /**Container with some const data given before parsing the scripts,
   * also used to store parsed scripts which can be used to call on <:call:script:args>
   */
  protected final Map<String, Object> idxConstData;
  
  /**Container with {@link OutTextPreparer} instances which can be called as sub pattern. 
   * Usual they are the patterns read from one textual file with several scripts (or a few files).
   * One patter starts with &lt;:otx:NAME:ARGS&gt; and ends with &lt;.otx&gt; in a given text.
   * Note that also #idxConstData can contain some {@link OutTextPreparer} scripts to call, usual given immediately as Java instances.
   * @since 2023-10-22. Before the scripts are only contain in {@link #idxConstData}. 
   *   There were a contradiction between meaning of the idxConstData and the Map of scripts.
   *   In the past first call-able scripts were created in Java code, and stored in the #idxConstData.
   *   Later more using textual scripts have stored there script parts firstly also in #idxConstData.  
   */
  protected final Map<String, OutTextPreparer> idxScript;
  
  /**The source of the generation script, argument of {@link #parse(Class, String)} only for debug. */
  public final String pattern;
  
  /**This is only stored as info, not used in this class.
   * It is the argument execClass from the user, 
   * which is immediately evaluated in the {@link #parse(Class, String)} operations while construction.
   */
  @SuppressWarnings("unused") private final Class<?> execClass;
  
  
  /**Via script given arguments for the outText. It does not contain the internal created variables. */
  private List<String> listArgs;
  
 
  int ctCall = 0;
  
  private List<Cmd> cmds = new ArrayList<Cmd>();
  
  
  /**Name of the generation script used for debug and comparison with data. */
  public final String sIdent;
  
  /**Instantiates for a given pattern. See {@link #OutTextPreparer(String, Class, String, String)}
   * The variables are gotten from the pattern. For this case the order of variable depends on the order in the pattern.
   * It is essential for access to data via index or via name. ...TODO see where
   */
  public OutTextPreparer(String ident, Class<?> execClass, String pattern) {
    this.sIdent = ident;
    this.pattern = pattern;
    this.execClass = execClass;
    this.parse(execClass, pattern);
    this.idxScript = null;
    this.idxConstData = null;
  }
  
  /**Constructs the text generation control data for the specific pattern. 
   * @param ident Any identification not used for the generated text.
   * @param execClass If the access via reflection should be done, null possible
   * @param variables One variable or list of identifier separated with comma, whiteSpaces possible. 
   * @param pattern The pattern in string given form. 
   *   This pattern will be parsed and divided in parts for a fast text generation.
   * @throws never because the instantiation is possible especially for static variables.
   *   On faulty pattern the prepared cmd for output contains the error message. 
   */
  public OutTextPreparer(String ident, Class<?> execClass, String variables, String pattern) {
    this(ident, execClass, variables, pattern, null);
  }

  
  
  /**Creates the text generation control data for the specific pattern and given sub pattern.
   * This is the preferred ctor if the scripts are read from a textual file,
   * see also {@link #readTemplateCreatePreparer(InputStream, Class, Map)}
   * @param ident Name of the sub pattern, used for error outputs.
   * @param execClass If the access via reflection should be done, null possible
   * @param variables One variable or list of identifier separated with comma, whiteSpaces possible. 
   * @param pattern The pattern in string given form. 
   *   This pattern will be parsed and divided in parts for a fast text generation.
   * @param idxConstData Container for call able scripts, maybe contain all scripts, also for more const data
   * @throws never because the instantiation is possible especially for static variables.
   *   On faulty pattern the prepared cmd for output contains the error message. 
   */
  public OutTextPreparer(String ident, Class<?> execClass, String variables, String pattern
      , Map<String, Object> idxConstData, Map<String, OutTextPreparer> idxScript) {
    this.sIdent = ident.trim();
    this.pattern = pattern;
    this.execClass = execClass;
    this.idxScript = idxScript;
    this.idxConstData = idxConstData;
    this.parseVariables(variables);
    this.parse(execClass, pattern);
  }
  
  
  
  /**Constructs the text generation control data for the specific pattern and given sub pattern.
   * This is the preferred ctor if the scripts are all given in Java code
   * and call-able scripts are contained in #idxConstData.
   * <br><br>
   * A constructor is called as: (example):<pre>
   * static OutTextPreparer otxMyText = new OutTextPreparer (
   *   "otxMyText"                   // The name of the OutText usable for call
   *   , UserClass.class             // A class which's static operations and data can be used 
   *   , "arg1, arg2",               // Name of arguments, see text below
   *    "A simple text with newline: \n"  // The output text pattern
   *  + "<&arg1>: a variable value"
   *  + "<&arg1.operation(arg2)>: Invocation of any operation in arguments resulting in an text"  
   *  + "<:call:otxSubText:arg1>: call of any sub text maybe with args"
   *  + "<:exec:operation:arg1> : call of any static operation in the given reflection class maybe with args"
   *  ;</pre>
   * @param ident Any identification not used for the generated text.
   * @param execClass If the access via reflection should be done, null possible
   * @param variables One variable or list of identifier separated with comma, whiteSpaces possible. 
   * @param pattern The pattern in string given form. 
   *   This pattern will be parsed and divided in parts for a fast text generation.
   * @param idxConstData Container for call able scripts, maybe contain all scripts, also for more const data
   * @throws never because the instantiation is possible especially for static variables.
   *   On faulty pattern the prepared cmd for output contains the error message. 
   */
  public OutTextPreparer(String ident, Class<?> execClass, String variables, String pattern, Map<String, Object> idxConstData) {
    this.sIdent = ident.trim();
    this.pattern = pattern;
    this.execClass = execClass;
    this.idxScript = null;
    this.idxConstData = idxConstData;
    this.parseVariables(variables);
    this.parse(execClass, pattern);
  }

  
  
  /**Instantiates for a given pattern. See {@link #OutTextPreparer(String, Class, String, String)}
   * @param variables Identifier given as list, parsing is not necessary. 
   *        Able to use if the variable idents are anyway given in a list.
   * 
   */
  public OutTextPreparer(String ident, Class<?> execClass, List<String> variables, String pattern) {
    this.sIdent = ident;
    this.pattern = pattern;
    this.execClass = execClass;
    this.setVariables(variables);
    this.parse(execClass, pattern);
    this.idxScript = null;
    this.idxConstData = null;
  }
  
  
  
  /**Reads a given template which may contain the pattern for several separated OutTextPreparer.
   * See also {@link #readTemplateList(InputStream, String)}
   * <br>
   * The file can contain different patterns in segments: <pre>
   * === patternName
   * content <&withVariables>
   * more lines
   * 
   * === nextPatternName
   *   etc.
   * </pre>
   * With this template Strings several OutTextPreparer can be created due to the schema (Example for Java2Vhdl)<pre>
    InputStream inTpl = Java2Vhdl.class.getResourceAsStream("VhdlTemplate.txt");  //pathInJar with slash: from root.
    Map<String, String> tplTexts = OutTextPreparer.readTemplate(inTpl, "===");
    inTpl.close();
    this.vhdlHead = new OutTextPreparer("vhdlHead", null, "fpgaName", tplTexts.get("vhdlHead"));
    this.vhdlAfterPort = new OutTextPreparer("vhdlAfterPort", null, "fpgaName", tplTexts.get("vhdlAfterPort"));
    this.vhdlConst = new OutTextPreparer("vhdlConst", null, "name, type, value", tplTexts.get("vhdlConst"));
    this.vhdlCmpnDef = new OutTextPreparer("vhdlCmpnDef", null, "name, vars", tplTexts.get("vhdlCmpnDef"));
    this.vhdlCmpnCall = new OutTextPreparer("vhdlCmpnCall", null, "name, typeVhdl, preAssignments, vars", tplTexts.get("vhdlCmpnCall"));
    </pre>
   * 
   * @param inp The open input to read, can also a resource in jar, then use {@link Class#getResourceAsStream(String)} to open
   * @param lineStart String which marks a new pattern segment, for the exmaple it should be "=== " 
   * @return Map contains patternName and pattern String, without empty trailing lines.
   * @throws IOException
   * @deprecated use {@link #readTemplateCreatePreparer(InputStream, String, Class, Map, String)} for all.
   */
  @Deprecated public static Map<String, String> readTemplate ( InputStream inp, String lineStart) throws IOException {
    Map<String, String> ret = new TreeMap<String, String>();
    InputStreamReader reader = new InputStreamReader(inp, "UTF-8");
    BufferedReader rd = new BufferedReader(reader);
    String line;
    String name = null;
    StringBuilder buf = null;
    int posEnd = 0;
    while( (line = rd.readLine()) !=null) {
      if(line.startsWith(lineStart)) {           // a new sub template starts here 
        if(name !=null) {                        // ends the sub template before if not done yet.
          buf.setLength(posEnd);                 // without trailing newlines.
          ret.put(name, buf.toString());         // store the template before.
        }
        final boolean bHasArgs;
        int posArgs = line.indexOf('(');
        bHasArgs = posArgs >0;
        if(!bHasArgs) { posArgs = line.length(); } // only the name on this line, compatible to before 2023-05
        name = line.substring(lineStart.length(), posArgs).trim();
        buf = new StringBuilder(200);
        if(bHasArgs) {
          buf.append(line.substring(posArgs)).append('\n');  // first line with args starting with "("
        }
        posEnd = 0;
      }
      else if(buf !=null) {
        buf.append(line).append("\n");
        if(line.length() >0) {
          posEnd = buf.length();
        }
      }
      else {
        // ignore lines before the first start.
      }
    }
    if(name !=null) {
      buf.setLength(posEnd);  //without trailing newlines.
      ret.put(name, buf.toString());
    }
    return ret;
  }
  
  
  
  /**Reads a given template which may contain the pattern for some associated OutTextPreparer also for call operations
   * @since 2023-05. See also {@link #readTemplateCreatePreparer(InputStream, String, Class)}
   * <br>
   * The file can contain different patterns in segments: <pre>
   * <:otx: patternName : variable : var2 >  ##comment
   * content with <&ariables>       ##comment
   * more lines<.otx>
   * 
   * free text between pattern
   * 
   * <:otx: nextPatternName ...
   *   etc.
   * </pre>
   */ 
  public static List<String> readTemplateList ( InputStream inp, String lineStart) throws IOException {
    List<String> ret = new LinkedList<String>();
    InputStreamReader reader = new InputStreamReader(inp, "UTF-8");
    BufferedReader rd = new BufferedReader(reader);
    String line;
    StringBuilder buf = new StringBuilder(500);
    boolean bActiveScript = false;
    while( (line = rd.readLine()) !=null) {
      
      int posComment = line.indexOf("##");
      if(posComment >=0) { line = line.substring(0, posComment); }
      if(!bActiveScript && line.startsWith("<:otx:")) {           // a new sub template starts here 
        buf.append(line).append('\n');  // first line with args starting with "("
        bActiveScript = true;
      }
      else if(lineStart !=null && !bActiveScript && line.startsWith(lineStart)) {           // a new sub template starts here 
        buf.append(line.substring(lineStart.length())).append('\n');  // first line with args starting with "("
        bActiveScript = true;
      }
      else if(bActiveScript) {
        int posEndScript = line.indexOf("<.otx>");
        if(posEndScript >=0) {
          buf.append(line.substring(0, posEndScript));
          ret.add(buf.toString());
          buf.setLength(0);
          bActiveScript = false;
        } else {
          buf.append(line).append("\n");
        }
      }
      else {
        // ignore lines outside active script.
      }
    }
    if(bActiveScript) {
      ret.add(buf.toString());
    }
    return ret;
  }
  
  
  
  /**Reads a given template which may contain the pattern for some associated OutTextPreparer also for call operations
   * and instantiates all OutTextPreparer to execute the script.
   * @since 2023-05. 
   * <br>
   * The file can contain different patterns in segments: <pre>
   * <:otx: patternName : variable : variable2 >  ##comment
   * content with <&variables>      ##comment
   * more lines<.otx>
   * 
   * free text between pattern
   * 
   * <:otx: nextPatternName :args>
   * <:call:patternName:variables = args> --- more text
   * <.otx>
   *   etc.
   * </pre>
   * @param inp An opened input stream, which can be for example also part of a zip file content
   *   or gotten via {@link Class#getResourceAsStream(String)} from a jar file, 
   *   or also of course via {@link java.io.FileReader#FileReader(java.io.File)}
   *   or via {@link InputStreamReader#InputStreamReader(InputStream, String)} to read a file with specific encoding.
   * @param lineStart The pattern which marks the start of a output text. It was "===" in examples, now deprecated.
   * @param execClass a given class which's content is accessed as persistent data.
   * @param idxConstData An index to access const persistent data, 
   *   and also to store all created OutTextPreparer instances. This is important for <:call:otx...>
   * @param sMainScript name of the main script to return, or null.
   * @return null or the requested main script. 
   *   Note: All scripts can be found in idxConstData.
   * @throws IOException on file error
   * @throws ParseException on parsing error of the script.
   */
  public static OutTextPreparer readTemplateCreatePreparer 
  ( InputStream inp, String lineStart, Class<?> execClass
  , Map<String, Object> idxConstData, String sMainScript 
  ) throws IOException, ParseException {
    readTemplateCreatePreparerPriv(inp, lineStart, execClass, idxConstData, null);
    return sMainScript == null ? null : (OutTextPreparer)idxConstData.get(sMainScript);
  } 
    
    
  /**internal implementation, see {@link #readTemplateCreatePreparer(InputStream, Class)}
   * @since 2023-08 separated from {@link #readTemplateCreatePreparerPriv(InputStream, String, Class, Map, Map)}
   *   which has only returned the main script.
   * @param inp
   * @param lineStart
   * @param execClass
   * @param idxConstData
   * @param idxScript
   * @throws IOException
   * @throws ParseException
   */
  private static void readTemplateCreatePreparerPriv 
  ( InputStream inp, String lineStart, Class<?> execClass
  , Map<String, Object> idxConstData 
  , Map<String, OutTextPreparer> idxScript
  ) throws IOException, ParseException {
    List<String> tplTexts = null;
    tplTexts = OutTextPreparer.readTemplateList(inp, lineStart);
    for(String text : tplTexts) {
      int posName;
      int posArgs;
      int posEndArgs;
      int posNewline = text.indexOf('\n');
      if(text.startsWith("<:otx:")) {
        posName = 6;
        posArgs=text.indexOf(':', 6);
        posEndArgs = text.indexOf('>');
      }
      else {
        posName = 0;
        posArgs = text.indexOf('(');
        posEndArgs = text.indexOf(')');
      }
      if(! ( posArgs >0 && posEndArgs > posArgs && posNewline > posEndArgs)) throw new ParseException("first line must contain ( args ,...)", 0);
      String name = text.substring(posName, posArgs).trim();
      String args = text.substring(posArgs+1, posEndArgs);
      String script = text.substring(posNewline+1);
      OutTextPreparer otxScript = new OutTextPreparer(name, execClass, args, script, idxConstData, idxScript);
      if(idxScript !=null) { idxScript.put(name, otxScript); }
      else { idxConstData.put(name, otxScript); }
    }
  }
  
  
  /**You can use this variant instead {@link #readTemplateCreatePreparer(InputStream, String, Class, Map, String)}
   * if you have not specific const Data for parsing the script.
   * @param inp An opened input stream, which can be for example also part of a zip file content
   *   or gotten via {@link Class#getResourceAsStream(String)} from a jar file, 
   *   or also of course via {@link java.io.FileReader#FileReader(java.io.File)}
   *   or via {@link InputStreamReader#InputStreamReader(InputStream, String)} to read a file with specific encoding.
   * @param execClass a given class which's content is accessed as persistent data.
   * @return Map which contains all script operations.
   *   access to the desired script via get(name).
   * @throws IOException
   * @throws ParseException
   * @since 2023-08 as common usable operation for some OutText templates in one file. 
   *   The constData processed in {@link #readTemplateCreatePreparer(InputStream, String, Class, Map, String)} 
   *   are usual not necessary for that approach, because it was the older concept to define some const data in the Map,
   *   and the Map is completed with the found scripts. 
   *   Here now it is simpler, the operation returns the new Map filled with the content of the read script. 
   */
  public static Map<String, OutTextPreparer> readTemplateCreatePreparer ( InputStream inp, Class<?> execClass) 
      throws IOException, ParseException {
      //
      Map<String, OutTextPreparer> idxScript = new TreeMap<String, OutTextPreparer>();
      readTemplateCreatePreparerPriv(inp, null, execClass, null, idxScript);
      return idxScript;
    }

  
  
  /**This is the recommended operation to read the template for some OutTextPreparer from a String given pattern..
   * @param inp An opened input stream, which can be for example also part of a zip file content
   *   or gotten via {@link Class#getResourceAsStream(String)} from a jar file, 
   *   or also of course via {@link java.io.FileReader#FileReader(java.io.File)}
   *   or via {@link InputStreamReader#InputStreamReader(InputStream, String)} to read a file with specific encoding.
   * @param idxConstData
   * @param execClass a given class which's content is accessed as persistent data.
   * @return Map which contains all script operations.
   *   access to the desired script via get(name).
   * @return index of all OutTextPreparer instance, any can be used. For example the "main" script to start.
   * @throws IOException
   * @throws ParseException
   * @since 2023-10-22 Used for LibreOffice / FBcl
   */
  public static Map<String, OutTextPreparer> readTemplateCreatePreparer ( InputStream inp, Class<?> execClass, Map<String, Object> idxConstData) 
      throws IOException, ParseException {
    //
    Map<String, OutTextPreparer> idxScript = new TreeMap<String, OutTextPreparer>();
    readTemplateCreatePreparerPriv(inp, null, execClass, idxConstData, idxScript);
    return idxScript;
  }
  
  
    
  /**Standard operation to write an output text from given data with a given template.
   * @param fout The file to write
   * @param data Data for the output with the script, only one is possible.
   * @param inTpl opened input stream which offers the otx-template to read.
   *   This inTpl is closed here after read.
   * @param execClass a given class which's content is accessed as persistent data.
   * @param sMain Name of the start or main template in the read script.
   * @throws IOException
   * @since 2023-08, but it is limited for only one data. Hence it may be used only as template.
   */
  public static void writeOtx(File fout, Object data, InputStream inTpl, Class<?> execClass, String sMain) throws IOException {
    OutTextPreparer otxt = null;
    try {
      Map<String, Object> idxOtxt = new TreeMap<String, Object>();
      otxt = OutTextPreparer.readTemplateCreatePreparer(inTpl, "===", execClass, idxOtxt, sMain);
      inTpl.close();
      Writer wr = new OutputStreamWriter(new FileOutputStream(fout), "UTF-8");
      //Appendable wr = new StringBuilder();
      OutTextPreparer.DataTextPreparer dp;
      dp = otxt.createArgumentDataObj();
      dp.setArgument("data", data);                        // here more flexibility is necessary,
      //                                                   // hence write an own script with more possible data 
      otxt.exec(wr, dp);                                   
      //
      wr.close();
    } catch(IOException exc) {
      System.err.println("File Exception: " + exc.getMessage());
    } catch(ParseException exc) {
      CharSequence sExc = ExcUtil.exceptionInfo("Exception", exc, 0, 20);
      System.err.println(sExc);
    }
    
    return;
  }

  
  
  private void parseVariables(String variables) {
    List<String> listvarValues = new LinkedList<String>();
    StringPartScan sp = new StringPartScan(variables);
    sp.setIgnoreWhitespaces(true);
    while(sp.scanStart().scanIdentifier().scanOk()) {
      String sVariable = sp.getLastScannedString();
      listvarValues.add(sVariable);
      if(!sp.scan(",").scanOk() && !sp.scan(":").scanOk()) {
        break; //, as separator
      }
    }
    sp.close();
    setVariables(listvarValues);
  }
  
  
  
  private void setVariables(List<String> listArgs) {
    this.listArgs = listArgs; 
    for(String var: listArgs) {
      this.nameVariables.put(var, new DataAccess.IntegerIx(this.nameVariables.size()));
    }
  }
  
  
  
  /**Parse the pattern. This routine will be called only from the constructor.
   * @param ident data instance to get data in the construction phase via reflection. 
   *   It is used for identifier in the pattern which is not found in the variables (parameter 'variables' of ctor).
   *   It can be null.
   * @throws ParseException 
   */ 
  private void parse(Class<?> execClass, String pattern) {
    int pos0 = 0; //start of current position after special cmd
    int pos1 = 0; //end before the next special command
    int[] ixCtrlCmd = new int[20];  //max. 20 levels for nested things.
    int ixixCmd = -1;
    StringPartScan sp = new StringPartScan(pattern);       // Note: pattern, pos0, pos1 is used to select a immediately output text
    sp.setIgnoreWhitespaces(true);  // it is valid inside the syntactical relevant parts <...>
    int nLastWasSkipOverWhitespace = 0;
    while(sp.length() >0) {
      nLastWasSkipOverWhitespace +=1;
      if(sp.scanStart().scan("##").scanOk()) {             // if a ## was found, seek till newline.
        sp.seek("\n").seekPos(1);                          // skip all till newline
        pos0 = (int)sp.getCurrentPosition();
      }                                                    // Note: spaces are detected because of content till <
      sp.seek("<", StringPart.mSeekCheck + StringPart.mSeekEnd);  // < is always start of a special output
      if(sp.found()) {
        
        pos1 = (int)sp.getCurrentPosition() -1; //before <
        sp.scanStart();
        //if(sp.scan("&").scanIdentifier().scan(">").scanOk()){
        if(sp.scan(":args:").scanOk()){ 
          parseArgs(sp);
          if(!sp.scan(">").scanOk()) { 
            addError("faulty <:args:... at " + sp.getCurrentPosition());
          }
          pos0 = (int)sp.getCurrentPosition();  //after '>'
        }
        else if(  nLastWasSkipOverWhitespace !=0 //The last scan action was not a <: >, it it was, it is one space insertion.
            && (sp.scan(": >").scanOk() || sp.scan(":+>").scanOk() || sp.scan(":? >").scanOk())){ 
         addCmd(pattern, pos0, pos1, ECmd.nothing, null, null);  //adds the text before <:+>
         sp.scanSkipSpace();
         pos0 = (int)sp.getCurrentPosition();  //after '>'
         nLastWasSkipOverWhitespace = -1;  //then the next check of <: > is not a skipOverWhitespace
        }  
        else if( sp.scan(":?nl>").scanOk()){ 
         addCmd(pattern, pos0, pos1, ECmd.nothing, null, null);  //adds the text before <:+>
         sp.seekAfterNewline();
         pos0 = (int)sp.getCurrentPosition();  //after newline
        }
        else if(sp.scan("&").scanIdentifier().scan(">").scanOk()){
          String sName = sp.getLastScannedString();
          addCmdSimpleVar(pattern, pos0, pos1, ECmd.addVar, sName, execClass);
          pos0 = (int)sp.getCurrentPosition();  //after '>'
        }
        else if(sp.scan("&").scanToAnyChar(">", '\0', '\0', '\0').scan(">").scanOk()){
          //------------------------------------------------- <&dataPath>
          final String sDatapath = sp.getLastScannedString();
//          if(sDatapath.startsWith("&("))
//            Debugutil.stop();
          //====> ////
          addCmd(pattern, pos0, pos1, ECmd.addVar, sDatapath, execClass);
          pos0 = (int)sp.getCurrentPosition();  //after '>'
        }
        else if(sp.scan(":if:").scanToAnyChar(">", '\\', '"', '"').scan(">").scanOk()) {
          //====>
          parseIf( pattern, pos0, pos1, ECmd.ifCtrl, sp, execClass);
          ixCtrlCmd[++ixixCmd] = this.cmds.size()-1;  //The position of the current if
          pos0 = (int)sp.getCurrentPosition();  //after '>'
          
        }
        else if(sp.scan(":elsif:").scanToAnyChar(">", '\\', '"', '"').scan(">").scanOk()) {
          //====>
          parseIf( pattern, pos0, pos1, ECmd.elsifCtrl, sp, execClass);
          Cmd ifCmdLast;
          int ixixIfCmd = ixixCmd; 
          if(  ixixIfCmd >=0 
            && ( (ifCmdLast = this.cmds.get(ixCtrlCmd[ixixCmd])).cmd == ECmd.ifCtrl 
               || ifCmdLast.cmd == ECmd.elsifCtrl
            )  ) {
            ((IfCmd)ifCmdLast).offsElsif = this.cmds.size() - ixCtrlCmd[ixixCmd] -1;   //The distance from <:if> to next <:elsif> 
          }else { 
            sp.close();
            throw new IllegalArgumentException("OutTextPreparer " + this.sIdent + ": faulty <.elsif> without <:if> ");
          }
          ixCtrlCmd[++ixixCmd] = this.cmds.size()-1;  //The position of the current <:elsif>
          
          pos0 = (int)sp.getCurrentPosition();  //after '>'
        }
        else if(sp.scan(":else>").scanOk()) {
          //====>
          addCmd(pattern, pos0, pos1, ECmd.elseCtrl, null, null);
          Cmd ifCmd;
          int ixixIfCmd = ixixCmd; 
          if(  ixixIfCmd >=0 
              && ( (ifCmd = this.cmds.get(ixCtrlCmd[ixixCmd])).cmd == ECmd.ifCtrl 
                 || ifCmd.cmd == ECmd.elsifCtrl
                  )  ) {
            ((IfCmd)ifCmd).offsElsif = this.cmds.size() - ixCtrlCmd[ixixCmd] -1;   //The distance from <:if> to next <:elsif> 
          }else { 
            sp.close();
            throw new IllegalArgumentException("OutTextPreparer " + this.sIdent + ": faulty <.elsif> without <:if> ");
          }
          ixCtrlCmd[++ixixCmd] = this.cmds.size()-1;  //The position of the current <:elsif>

          pos0 = (int)sp.getCurrentPosition();  //after '>'
        }
        else if(sp.scan(":for:").scanIdentifier().scan(":").scanToAnyChar(">", '\\', '"', '"').scan(">").scanOk()) {
          String container = sp.getLastScannedString().toString();
          String entryVar = sp.getLastScannedString().toString();
          //====>
          ForCmd cmd = (ForCmd)addCmd(pattern, pos0, pos1, ECmd.forCtrl, container, execClass);
          DataAccess.IntegerIx ixOentry = this.nameVariables.get(entryVar); 
          if(ixOentry == null) { //Check whether the same entry variable exists already from another for, only ones.
            ixOentry = new DataAccess.IntegerIx(this.nameVariables.size());         //create the entry variable newly.
            this.nameVariables.put(entryVar, ixOentry);
          }
          cmd.ixEntryVar = ixOentry.ix;
          entryVar += "_next";                             // the descendant of the current element is also available. 
          ixOentry = this.nameVariables.get(entryVar); 
          if(ixOentry == null) { //Check whether the same entry variable exists already from another for, only ones.
            ixOentry = new DataAccess.IntegerIx(this.nameVariables.size());         //create the entry variable newly.
            this.nameVariables.put(entryVar, ixOentry);
          }
          cmd.ixEntryVarNext = ixOentry.ix;
          ixCtrlCmd[++ixixCmd] = this.cmds.size()-1;
          pos0 = (int)sp.getCurrentPosition();  //after '>'
        }
        else if(sp.scan(":set:").scanIdentifier().scan("=").scanToAnyChar(">", '\\', '\'', '\'').scan(">").scanOk()) {
          String value = sp.getLastScannedString().toString();
          String variable = sp.getLastScannedString().toString();
          SetCmd cmd = (SetCmd)addCmd(pattern, pos0, pos1, ECmd.setVar, value, execClass);
          DataAccess.IntegerIx ixOentry = this.nameVariables.get(variable); 
          if(ixOentry == null) { //Check whether the same entry variable exists already from another for, only ones.
            ixOentry = new DataAccess.IntegerIx(this.nameVariables.size());         //create the entry variable newly.
            this.nameVariables.put(variable, ixOentry);
          }
          cmd.ixVariable = ixOentry.ix;
          pos0 = (int)sp.getCurrentPosition();  //after '>'
        }
        else if(sp.scan(":exec:").scanIdentifier().scanOk()) {
          //====>
          parseExec(pattern, pos0, pos1, sp, execClass);
          pos0 = (int)sp.getCurrentPosition();  //after '>'
        }
        else if(sp.scan(":call:").scanIdentifier().scanOk()) {
          //====>
          parseCall(pattern, pos0, pos1, sp, execClass);
          pos0 = (int)sp.getCurrentPosition();  //after '>'
        }
        else if(sp.scan(":debug:").scanIdentifier().scan(":").scanToAnyChar(">", '\\', '"', '"').scan(">").scanOk()) {
          String cmpString = sp.getLastScannedString().toString();
          String debugVar = sp.getLastScannedString().toString();
          //====>
          DebugCmd cmd = (DebugCmd)addCmd(pattern, pos0, pos1, ECmd.debug, debugVar, execClass);
          cmd.cmpString = cmpString;
          pos0 = (int)sp.getCurrentPosition();  //after '>'
        }  
        else if(sp.scan(":debug>").scanOk()) {
          //====>
          //DebugCmd cmd = (DebugCmd)addCmd(pattern, pos0, pos1, ECmd.debug, null, reflData);
          pos0 = (int)sp.getCurrentPosition();  //after '>'
        }
        else if(sp.scan(":--").scanToAnyChar(">", '\0', '\0', '\0').scan(">").scanOk()) {
          //it is commented
          addCmd(pattern, pos0, pos1, ECmd.nothing, null, null);
          pos0 = (int)sp.getCurrentPosition();  //after '>'
        }
        else if(sp.scan(":x").scanHex(4).scan(">").scanOk()) {
          char[] cText = new char[1];                      // <:x1234> a special UTF16 char
          cText[0] = (char)sp.getLastScannedIntegerNumber();
          String sTextSpecial = new String(cText);
          addCmd(pattern, pos0, pos1, ECmd.addString, sTextSpecial, null);
        }
        else if(sp.scan(":").scanToAnyChar(">", '\0', '\0', '\0').scan(">").scanOk()) {
          // ------------------------------------------------ <:text> or <:n> <:r> <:t> <:s>
          final int posend = (int)sp.getCurrentPosition();
          final String sText = pattern.substring(pos1+2, posend-1);
          //Note: do not use sp.getLastScannedString().toString(); because scanToAnyChar skips over whitespaces
          final int what;
          if(sText.length() == 1) {
            what = "nrts".indexOf(sText.charAt(0));
          } else {
            what = -1; 
          }
          if(what >=0) {
            String[] specials = { "\n", "\r", "\t", " "};  // newline etc.
            addCmd(pattern, pos0, pos1, ECmd.addString, specials[what], null);
          } else {                                         // free text with especially special chars
            addCmd(pattern, pos0, pos1, ECmd.addString, sText, null); //add the <:sText>
          }
          pos0 = posend;  //after '>'
        }
        else if(sp.scan(".if>").scanOk()) { //The end of an if
          Cmd cmd = null;
          addCmd(pattern, pos0, pos1, ECmd.nothing, null, execClass);  //The last text before <.if>
          while(  ixixCmd >=0 
            && ( (cmd = this.cmds.get(ixCtrlCmd[ixixCmd])).cmd == ECmd.ifCtrl 
              || cmd.cmd == ECmd.elsifCtrl
              || cmd.cmd == ECmd.elseCtrl
            )  ) {
            IfCmd ifcmd;
            cmd.offsEndCtrl = cmds.size() - ixCtrlCmd[ixixCmd];
            if(cmd.cmd != ECmd.elseCtrl && (ifcmd = (IfCmd)cmd).offsElsif <0) {
              ifcmd.offsElsif = ifcmd.offsEndCtrl; //without <:else>, go after <.if>
            }
            ixCtrlCmd[ixixCmd] = 0;    // no more necessary.
            ixixCmd -=1;
            if(cmd.cmd == ECmd.ifCtrl) {
              break;
            } else {
              cmd = null;    //remain ifCtrl to check: at least <:if> necessary.
            }
          } 
          if(cmd == null) {  //nothing found or <:if not found: 
            String sError = sp.getCurrent(30).toString();
            sp.close();
            throw new IllegalArgumentException("OutTextPreparer " + this.sIdent + ": faulty <.if> without <:if> or  <:elsif> : " + sError);
          }
          pos0 = (int)sp.getCurrentPosition();  //after '>'
        }
        else if(sp.scan(".for>").scanOk()) { //The end of an if
          Cmd forCmd;
          if(ixixCmd >=0 && (forCmd = this.cmds.get(ixCtrlCmd[ixixCmd])).cmd == ECmd.forCtrl) {
            Cmd endLoop = addCmd(pattern, pos0, pos1, ECmd.endLoop, null, null);
            endLoop.offsEndCtrl = -this.cmds.size() - ixCtrlCmd[ixixCmd] -1;
            pos0 = (int)sp.getCurrentPosition();  //after '>'
            forCmd.offsEndCtrl = this.cmds.size() - ixCtrlCmd[ixixCmd];
            ixixCmd -=1;
          } 
          
          else {
            sp.close();
            throw new IllegalArgumentException("OutTextPreparer " + this.sIdent + ": faulty <:for>...<.for> ");
          }
        }
        else { //No proper cmd found:
          
        }
      }
      else { //no more '<' found:
        sp.len0end();
        addCmd(pattern, pos0, pos0 + sp.length(), ECmd.nothing, null, execClass);
        sp.fromEnd();  //length is null then.
      }
    } //while
    if(ixixCmd >=0) {
      sp.close();
      throw new IllegalArgumentException("OutTextPreparer " + this.sIdent + ": closing <.> for <" + this.cmds.get(ixCtrlCmd[ixixCmd]) +"> is missing ");
    }
    sp.close();
  }

  

  
  private void parseArgs(StringPartScan sp) {
    do {
      if(sp.scanIdentifier().scanOk()) {
        String sArg = sp.getLastScannedString();
        this.nameVariables.put(sArg, new DataAccess.IntegerIx(this.nameVariables.size()));
        if(this.listArgs == null) { this.listArgs = new LinkedList<String>(); }
        this.listArgs.add(sArg);
      }
    } while(sp.scan(",").scanOk());
  }
  
  private void parseIf ( final String pattern, final int pos0, final int pos1, ECmd ecmd, final StringPartScan sp, Class<?> reflData) {
    String cond = sp.getLastScannedString().toString();
//    if(cond.contains("?instanceof"))
//      Debugutil.stop();
    //====>
    IfCmd ifcmd = (IfCmd)addCmd(pattern, pos0, pos1, ecmd, cond, reflData);
    ifcmd.offsElsif = -1;  //in case of no <:else> or following <:elsif is found.
    
  }
  
  
  

  private void parseExec(final String src, final int pos0, final int pos1, StringPartScan sp, Class<?> reflData) {
    String name = sp.getLastScannedString();
//    if(name.equals("writeFBlocks"))
//      Debugutil.stop();
    Class argType;
//    String sArg;
//    if(sp.scan(":").scanToAnyChar(">", '\\', '\'', '\'').scanOk()) {
//      sArg = sp.getLastScannedString();
//      argType = Object.class;
//    } else {
//      sArg = null;
//      argType = null;
//    }
    StringPartScan spArg;
    if(sp.scan(":").scanOk()) {
      spArg = sp;
      argType = Object.class;
    } else {
      spArg = null;
      argType = null;
    }
    ExecCmd cmd = (ExecCmd)addCmdSp(src, pos0, pos1, ECmd.exec, spArg, reflData);
    Method[] operations = reflData.getMethods();
    Class<?>[] argTypes = null;
    for(Method operation: operations) {
      if(operation.getName().equals(name)) {
        argTypes = operation.getParameterTypes();
        cmd.execOperation = operation;
        break;
      }
    }
    //try{ oper = argType !=null ? reflData.getMethod(name, argType) : reflData.getMethod(name); }
    //catch(NoSuchMethodException exc) 
    if(cmd.execOperation == null){ throw new IllegalArgumentException("<:exec:" + name + "> not found in " + reflData.toString() + ". "); }
    if(!sp.scan(">").scanOk()) {
      throw new IllegalArgumentException("OutTextPreparer "+ sIdent + ": syntax error \">\" expected in <:exec: " + name + "...>");
    }
  }
  
  
  private void parseCall(final String src, final int pos0, final int pos1, final StringPartScan sp, Class<?> reflData) {
    String sCallVar = sp.getLastScannedString();
    if(sCallVar.equals("otxIfColors"))
      debug();
    CallCmd cmd = (CallCmd)addCmd(src, pos0, pos1, ECmd.call, sCallVar, reflData);
    final OutTextPreparer call;  
    if(cmd.dataConst !=null) { //given as const static:
      if(!(cmd.dataConst instanceof OutTextPreparer)) { //check the type on creation
        throw new IllegalArgumentException("OutTextPreparer "+ sIdent + ": <:call: " + sCallVar + " is const but not a OutTextPreparer");
//      } else { //call variable should be given dynamically:
//        if(!this.varValues.containsKey(sCallVar)) {
//          String sError = "OutTextPreparer "+ sIdent + ": <:call: " + sCallVar + ": not given as argument";
//          if(reflData != null) {
//            sError += " and not found staticly in " + reflData.toString();
//          } else {
//            sError += "Hint: no staticly data given in first argument of ctor.";
//          } 
//          throw new IllegalArgumentException(sError);
//        }
      }
      call = (OutTextPreparer)cmd.dataConst; //check argument names with it.
    } else {
      call = null; //not possible to check arguments.
    }
    if(sp.scan(":").scanOk()) {
      do {
        cmd.ixDataArg = this.ctCall;
        this.ctCall +=1;
        if(sp.scanIdentifier().scan("=").scanOk()) {
          if(cmd.args == null) { cmd.args = new ArrayList<Argument>(); }
          String sNameArg = sp.getLastScannedString();
          int ixCalledArg;
          if(call == null) {
            ixCalledArg = -1;
          } else {
            DataAccess.IntegerIx ixOcalledArg = call.nameVariables.get(sNameArg);
            if(ixOcalledArg == null) {
              throw new IllegalArgumentException("OutTextPreparer "+ sIdent + ": <:call: " + sCallVar + ":argument not found: " + sNameArg);
            }
            ixCalledArg = ixOcalledArg.ix;
          }
          Argument arg;
          try {
            if(sp.scanLiteral("''\\", -1).scanOk()) {
              String sText = sp.getLastScannedString().trim();
              arg = new Argument(sNameArg, ixCalledArg, sText, null, sText);
            }
            else if(sp.scanToAnyChar(">,", '\\', '"', '"').scanOk()) {
              String sDataPath = sp.getLastScannedString().trim();
              arg = new Argument(this, sNameArg, ixCalledArg, sDataPath, reflData);
            }
            else { 
              throw new IllegalArgumentException("OutTextPreparer "+ sIdent + ": syntax error for argument value in <:call: " + sCallVar + ":arguments>");
            }
          } catch(Exception exc) {
            throw new IllegalArgumentException("OutTextPreparer " + sIdent + ", argument: " + sNameArg + " not existing: ");
          }
          cmd.args.add(arg);
        } else {
          throw new IllegalArgumentException("OutTextPreparer "+ sIdent + ": syntax error for arguments in <:call: " + sCallVar + ":arguments>");
        }
      } while(sp.scan(",").scanOk());
    }
    if(!sp.scan(">").scanOk()) {
      throw new IllegalArgumentException("OutTextPreparer "+ sIdent + ": syntax error \">\" expected in <:call: " + sCallVar + "...>");
    }
    
  }
  
  
  private void addError(String sError) {
    cmds.add(new CmdString(" <?? " + sError + "??> "));
  }
 
  
  
  
  /**Called if a simple <code>&lt;&name></code> is detected.
   * It is searched firstly in {@link #idxConstData}, then in {@link #nameVariables}
   * and at least in the execClass via Datapath access.
   * If not found then <code><??sName??></code> is output on runtime.
   * It adds the plain text if necessary and the data access as {@link Cmd#Cmd(ECmd, int, DataAccess, Object, String)}
   * to the list of commands.
   * 
   * @param src The text before to output the plain text before
   * @param from range in src
   * @param to to > from, then output the plain text
   * @param eCmd The {@link ECmd} for the Cmd 
   * @param sName name of the argument or variable or field.
   * @param execClass to search in a data field.
   */
  private void addCmdSimpleVar ( String src, int from, int to, ECmd eCmd, String sName, Class<?> execClass) {
    if(to > from) {
      this.cmds.add(new CmdString(src.substring(from, to)));
    }
    Object data = this.idxConstData ==null ? null : this.idxConstData.get(sName);
    if(data != null) {                                     // given const data on construction
      this.cmds.add(new Cmd(eCmd, -1, null, data, sName));
    } else {
      DataAccess.IntegerIx ix = this.nameVariables.get(sName);
      if(ix !=null) {                                      // Index to the arguments
        this.cmds.add(new Cmd(eCmd, ix.ix, null, null, sName));
      } else {
        try {
          data = DataAccess.access(sName, execClass, true, false, false, null);
          this.cmds.add(new Cmd(eCmd, -1, null, data, sName));
        } catch (Exception exc) {
          addError(sName);                                 // inserts <??sName??> in the text
        }
      }
    }
  }
  
  
  
  
  /**Explore the sDatapath and adds the proper Cmd
   * @param src The pattern to get text contents
   * @param from start position for text content
   * @param to end position for text content, if <= from then no text content is stored (both = 0)
   * @param ecmd The cmd kind
   * @param sDatapath null or a textual data path. It will be split to {@link Cmd#ixValue} and {@link Cmd#dataAccess}
   * @return the created Cmd for further parameters.
   */
  private Cmd addCmd ( String src, int from, int to, ECmd ecmd, String sDatapath, Class<?> data) {
    if(to > from) {
      cmds.add(new CmdString(src.substring(from, to)));
    }
    final Cmd cmd;
    if(ecmd !=ECmd.nothing) {
      try {
        switch(ecmd) {
          case ifCtrl: case elsifCtrl: cmd = new IfCmd(this, ecmd, sDatapath, data); break;
          case call: 
            Object oOtxSub = null;
            String sNameSub = sDatapath;
            if(this.idxScript !=null) { oOtxSub = this.idxScript.get(sNameSub); }
            if(oOtxSub == null && this.idxConstData !=null) { oOtxSub = this.idxConstData.get(sNameSub); }
            if(oOtxSub == null || ! (oOtxSub instanceof OutTextPreparer)) {
              throw new IllegalArgumentException("OutTextPreparer " + this.sIdent + ": subroutine: " + sNameSub + " not found ");
            }
            cmd = new CallCmd((OutTextPreparer)oOtxSub); //this, sDatapath, data); 
            //cmd = new CallCmd(this, sDatapath, data); 
            break;
          case exec: cmd = new ExecCmd(this, sDatapath, data); break;
          case forCtrl: cmd = new ForCmd(this, sDatapath, data); break;
          case setVar: cmd = new SetCmd(this, sDatapath, data); break;
          case debug: cmd = new DebugCmd(this, sDatapath, data); break;
          case addString: cmd = new CmdString(sDatapath); break;
          default: cmd = new Cmd(this, ecmd, sDatapath, data, null); break;
        }
      } catch(Exception exc) {
        throw new IllegalArgumentException("OutTextPreparer " + sIdent + ", variable or path: " + sDatapath + " error: " + exc.getMessage());
      }
      cmds.add(cmd);
    } else {
      cmd = null;
    }
    return cmd;
  }




  /**Explore the sDatapath and adds the proper Cmd
   * @param src The pattern to get text contents
   * @param from start position for text content
   * @param to end position for text content, if <= from then no text content is stored (both = 0)
   * @param ecmd The cmd kind
   * @param sDatapath null or a textual data path. It will be split to {@link Cmd#ixValue} and {@link Cmd#dataAccess}
   * @return the created Cmd for further parameters.
   */
  private Cmd addCmdSp(String src, int from, int to, ECmd ecmd, StringPartScan sDatapath, Class<?> data) {
    if(to > from) {
      this.cmds.add(new CmdString(src.substring(from, to)));
    }
    final Cmd cmd;
    if(ecmd !=ECmd.nothing) {
      try {
        switch(ecmd) {
          case ifCtrl: case elsifCtrl: cmd = new IfCmd(this, ecmd, sDatapath, data); break;
          case call: 
            Object oOtxSub = null;
            String sNameSub = sDatapath.getCurrent().toString();
            if(this.idxScript !=null) { oOtxSub = this.idxScript.get(sNameSub); }
            if(oOtxSub == null && this.idxConstData !=null) { oOtxSub = this.idxConstData.get(sNameSub); }
            if(oOtxSub == null || ! (oOtxSub instanceof OutTextPreparer)) {
              throw new IllegalArgumentException("OutTextPreparer " + this.sIdent + ": subroutine: " + sNameSub + " not found ");
            }
            cmd = new CallCmd((OutTextPreparer)oOtxSub); //this, sDatapath, data); 
            break;
          case exec: cmd = new ExecCmd(this, sDatapath, data); break;
          case forCtrl: cmd = new ForCmd(this, sDatapath, data); break;
          case setVar: cmd = new SetCmd(this, sDatapath, data); break;
          case debug: cmd = new DebugCmd(this, sDatapath, data); break;
          default: cmd = new Cmd(this, ecmd, sDatapath, data); break;
        }
      } catch(Exception exc) {
        throw new IllegalArgumentException("OutTextPreparer " + sIdent + ", variable or path: " + sDatapath + " error: " + exc.getMessage());
      }
      cmds.add(cmd);
    } else {
      cmd = null;
    }
    return cmd;
  }


  /**Returns an proper instance for argument data for a {@link #exec(Appendable, DataTextPreparer)} run.
   * The arguments should be filled using {@link DataTextPreparer#setArgument(String, Object)} by name 
   * or {@link DataTextPreparer#setArgument(int, Object)} by index if the order of arguments are known.
   * The argument instance can be reused in the same thread by filling arguments newly for subsequently usage.
   * If the {@link #exec(Appendable, DataTextPreparer)} is concurrently executed (multiple threads, 
   * multicore processing), then any thread should have its own data.
   * @return the argument instance.
   */
  public DataTextPreparer createArgumentDataObj() { return new DataTextPreparer(this); }
  
  
  /**Creates the argument data and presets the given execInstance to {@link DataTextPreparer#setExecObj(Object)}
   * @param execInstance It should be an instance of the given reflection class on ctor. 
   * @return Data
   */
  public DataTextPreparer getArgumentData(Object execInstance) { 
    DataTextPreparer data = new DataTextPreparer(this);
    data.setExecObj(execInstance);
    return data;
  }
  
  
  
  
  
  

  
  

  /**Executes preparation of a pattern with given data.
   * Note: The instance data of this are not changed. The instance can be used 
   * in several threads or multicore processing.
   * @param wr The output channel
   * @param args for preparation. 
   *   The value instance should be gotten with {@link OutTextPreparer#createArgumentDataObj()}
   *   proper to the instance of this, because the order of arguments should be match. 
   *   It is internally tested. 
   * @throws Exception 
   */
  public void exec( Appendable wr, DataTextPreparer args) throws IOException {
    if(args.prep != this) {
      throw new IllegalArgumentException("OutTextPreparer mismatch: The data does not match to the script.");
    }
    execSub(wr, args, 0, cmds.size());
  }
  
  
  /**Executes preparation for a range of cmd for internal control structures
   * @param wr The output channel
   * @param args for preparation.
   * @param ixStart from this cmd in {@link #cmds} 
   * @throws IOException 
   */
  private void execSub( Appendable wr, DataTextPreparer args, int ixStart, int ixEndExcl ) throws IOException {
    //int ixVal = 0;
    int ixCmd = ixStart;
    while(ixCmd < ixEndExcl) {
      if(args.debugOtx !=null && args.debugOtx.equals(this.sIdent) && args.debugIxCmd == ixCmd)
        debug();
      Cmd cmd = cmds.get(ixCmd++);
      boolean bDataOk = true;
      Object data;
      if(cmd.expr !=null) {
        try { 
          //====>
          if(args.calcExprData == null) { args.calcExprData = new CalculatorExpr.Data(); }
          data = cmd.expr.calcDataAccess(args.calcExprData, null, args.args); 
        } catch (Exception e) {
          bDataOk = false;
          data = null;
          wr.append("<??OutTextPreparer script >>" + sIdent + "<<: >>" + cmd.textOrVar + "<< execution error: " + e.getMessage() + "??>");
        }
      }
      else if(cmd.ixValue >=0) {
        data = args.args[cmd.ixValue];
        if(cmd.dataAccess !=null) {
          try {
            //====>
            data = cmd.dataAccess.access(data, true, false, nameVariables, args.args);
          } catch (Exception e) {
            bDataOk = false;
            wr.append("<??OutTextPreparer in script >>" + sIdent + "<<: >>" + cmd.textOrVar + "<< not found or access error: " + e.getMessage() + "??>");
          }
        }
      } 
      else if(cmd.dataAccess !=null) {
        try {
          //====>
          data = cmd.dataAccess.access(null, true, false, nameVariables, args.args);
        } catch (Exception e) {
          bDataOk = false;
          data = "<??>";
          wr.append("<??OutTextPreparer script >>" + sIdent + "<<: >>" + cmd.textOrVar + "<< not found or access error: " + e.getMessage() + "??>");
        }
        
      }
      else if(cmd.dataConst !=null){ 
        data = cmd.dataConst;  //may be given or null 
      } 
      else {
        data = cmd.textOrVar; //maybe null
      }
      if(bDataOk) {
        switch(cmd.cmd) {
          case addString: wr.append(cmd.textOrVar); break;
          case addVar: {                                   // the data are already prepared before switch
            //Integer ixVar = varValues.get(cmd.str);
            if(data == null) { wr.append("<??null??>"); }
            else { wr.append(data.toString()); }
          } break;
          case setVar: {
            int ixVar = ((SetCmd)cmd).ixVariable;
            args.args[ ixVar ] = data;
          } break;
          case elsifCtrl:
          case ifCtrl: {
            ixCmd = execIf(wr, (IfCmd)cmd, ixCmd, data, args);
          } break;
          case elseCtrl: break;  //if <:else> is found in queue of <:if>...<:elseif> ...<:else> next statements are executed.
          case forCtrl: {
            execFor(wr, (ForCmd)cmd, ixCmd, data, args);;
            ixCmd += cmd.offsEndCtrl -1;  //continue after <.for>
          } break;
          case exec: {
            ExecCmd ecmd = (ExecCmd)cmd;
            Class<?>[] argTypes = ecmd.execOperation.getParameterTypes();
            int zArgs = argTypes.length;
            try {
              if(zArgs == 0) { //any argument:
                ((ExecCmd)cmd).execOperation.invoke(args.execObj);  //without argument
              } else {
                if(DataAccess.istypeof(data, argTypes[0])) {
                  ((ExecCmd)cmd).execOperation.invoke(args.execObj, data);
                } else {
                  wr.append("<?? OutTextPreparer script " + sIdent + "<exec:" + cmd.textOrVar + ": argument type error " + data.getClass() + "??>");
                }
              }
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException exc) {
              // TODO Auto-generated catch block
              wr.append("<?? OutTextPreparer script " + sIdent + "<exec:" + cmd.textOrVar + ": execution exception " + exc.getMessage() + "??>");
            } 
          } break;
          case call: 
            if(data == null) {
              wr.append("<?? OutTextPreparer script " + sIdent + "<call:" + cmd.textOrVar + ": variable not found, not given??>");
            }
            if(!(data instanceof OutTextPreparer)) {
              wr.append("<?? OutTextPreparer script " + sIdent + "<call:" + cmd.textOrVar + ":  variable is not an OutTextPreparer ??>");
            } else {
              execCall(wr, (CallCmd)cmd, args, (OutTextPreparer)data);
            } 
            break;
          case debug: {
            if(data ==null || ((DebugCmd)cmd).cmpString ==null || data.toString().equals(((DebugCmd)cmd).cmpString)){
              debug();
            }
          } break;
        }
      } else { //data error
        if(cmd.offsEndCtrl >0) {
          ixCmd += cmd.offsEndCtrl +1;
        }
      }
    }
  }
  
  
  
  
  /**Executes a if branch
   * @param wr the output channel
   * @param cmd The ForCmd
   * @param ixCmd the index of the cmd in {@link #cmds}
   * @param container The container argument
   * @param args actual args of the calling level
   * @throws Exception
   */
  private int execIf(Appendable wr, IfCmd ifcmd, int ixCmd, Object data, DataTextPreparer args) throws IOException {
    boolean bIf;
    if (data !=null) { 
      if( data instanceof CalculatorExpr.Value) { bIf = ((CalculatorExpr.Value)data).booleanValue(); }
      else if( data instanceof Boolean ) { bIf = ((Boolean)data).booleanValue(); }
      else if( data instanceof Number ) { bIf =  ((Number)data).intValue() != 0; }
      else { bIf = true; } //other data, !=null is true already. 
    } else {
      bIf = false;
    }
    if( bIf) { //execute if branch
      execSub(wr, args, ixCmd, ixCmd + ifcmd.offsElsif -1);
      return ixCmd + ifcmd.offsEndCtrl -1;  //continue after <.if>
    } else {
      //forward inside if to the next <:elsif or <:else
      return ixCmd + ifcmd.offsElsif -1;  //if is false, go to <:elsif, <:else or <.if.
    }
  }
  
  
  
  /**Executes a for loop
   * @param wr the output channel
   * @param cmd The ForCmd
   * @param ixCmd the index of the cmd in {@link #cmds}
   * @param container The container argument
   * @param args actual args of the calling level
   * @throws Exception
   */
  private void execFor(Appendable wr, ForCmd cmd, int ixCmd, Object container, DataTextPreparer args) throws IOException {
    if(container == null) {
      //do nothing, no for
    }
    else if(container instanceof Object[]) {
      Object[] array = (Object[]) container;
      boolean bFirst = true;
      for(int ix = 0; ix < array.length; ++ix) {
        args.args[cmd.ixEntryVar] = args.args[cmd.ixEntryVarNext];
        args.args[cmd.ixEntryVarNext] = array[ix];
        if(bFirst) {
          bFirst = false;
        } else { //start on 2. item
          execSub(wr, args, ixCmd, ixCmd + cmd.offsEndCtrl -1);
        }
      }
      if(!bFirst) {  //true only if the container is empty.
        args.args[cmd.ixEntryVar] = args.args[cmd.ixEntryVarNext];
        args.args[cmd.ixEntryVarNext] = null;
        execSub(wr, args, ixCmd, ixCmd + cmd.offsEndCtrl -1);
      }
    }
    else if(container instanceof Iterable) {
      boolean bFirst = true;
      for(Object item: (Iterable<?>)container) {
        args.args[cmd.ixEntryVar] = args.args[cmd.ixEntryVarNext];
        args.args[cmd.ixEntryVarNext] = item;
        if(bFirst) {
          bFirst = false;
        } else { //start on 2. item
          execSub(wr, args, ixCmd, ixCmd + cmd.offsEndCtrl -1);
        }
      }
      if(!bFirst) {  //true only if the container is empty.
        args.args[cmd.ixEntryVar] = args.args[cmd.ixEntryVarNext];
        args.args[cmd.ixEntryVarNext] = null;
        execSub(wr, args, ixCmd, ixCmd + cmd.offsEndCtrl -1);
      }
    }
    else if(container instanceof Map) {
      @SuppressWarnings("unchecked") Map<Object, Object>map = ((Map<Object,Object>)container);
      boolean bFirst = true;
      for(Map.Entry<Object,Object> item: map.entrySet()) {
        args.args[cmd.ixEntryVar] = args.args[cmd.ixEntryVarNext];
        args.args[cmd.ixEntryVarNext] = item.getValue();   // buffer always 2 elements, sometimes necessary
        if(bFirst) {
          bFirst = false;
        } else { //start on 2. item
          execSub(wr, args, ixCmd, ixCmd + cmd.offsEndCtrl -1);
        }
      }
      if(!bFirst) {  //true only if the container is empty.
        args.args[cmd.ixEntryVar] = args.args[cmd.ixEntryVarNext];
        args.args[cmd.ixEntryVarNext] = null;              // execute for the last argument.
        execSub(wr, args, ixCmd, ixCmd + cmd.offsEndCtrl -1);
      }
    }
    else {
      wr.append("<?? OutTextPreparer script " + sIdent + ": for variable is not an container: " + cmd.textOrVar + "??>");
    }
  }
    
    
    
    
  /**Executes a call
   * @param wr the output channel
   * @param cmd The CallCmd
   * @param args actual args of the calling level
   * @param callVar The OutTextPreparer which is called here.
   * @throws Exception
   */
  private void execCall(Appendable wr, CallCmd cmd, DataTextPreparer args, OutTextPreparer callVar) throws IOException {
    OutTextPreparer callVar1 = (OutTextPreparer)callVar;
    DataTextPreparer valSub;
    if(cmd.args !=null) {
      valSub = args.argSub[cmd.ixDataArg];
      if(valSub == null) { //only first time for this call
        args.argSub[cmd.ixDataArg] = valSub = callVar1.createArgumentDataObj();  //create a data instance for arguments.
        valSub.debugIxCmd = args.debugIxCmd;
        valSub.debugOtx = args.debugOtx;
      }
      for(Argument arg : cmd.args) {
        Object value = null;
        try{ value = arg.calc(null, args.args); }
        catch(Exception exc) { wr.append("<??OutTextPreparer script " + this.sIdent + ": " + arg.textOrVar + " not found or access error: " + exc.getMessage() + "??>"); }
        if(arg.ixDst >=0) {
          valSub.setArgument(arg.ixDst, value);
        } else {
          valSub.setArgument(arg.name, value);
        }
      }
    } else {
      //<:call:name> without arguments: Use the same as calling level.
      valSub = args;
    }
    callVar1.exec(wr, valSub);
  }
  
  
  
  
  @Override public String toString() { return this.sIdent + ":" + this.pattern; }
  
  
  void debug() {}
  
} //class OutTextPreparer
