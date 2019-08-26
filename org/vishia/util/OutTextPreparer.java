package org.vishia.util;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;





/**This class helps to prepare an output text with data. It may be seen as small solution in comparison to {@link org.vishia.jztxtcmd.JZtxtcmd}
 * only for text preparation of one or a few lines.
 * <br><br>
 * An instance of this class is used as formatter for an output text. On construction a pattern is given:<pre>
 * static OutTextPreparer otxMyText = new OutTextPreparer("otxMyText", UserClass.class, "arg1, arg2",
 *    "A simple text with newline: \n"
 *  + "<&arg1>: a variable value"
 *  + "<&arg1.operation(arg2)>: Invocation of any operation in arguments resulting in an text"  
 *  + "<:call:otxSubText:arg1>: call of any sub text maybe with args"
 *  + "<:exec:operation:arg1> : call of any operation in the given reflection class maybe with args"
 *  + "");
 * </pre>
 * Arguments of ctor: see also {@link OutTextPreparer#OutTextPreparer(String, Class, String, String)}:
 * <ul><li>First arg: only a text for error reports
 * <li>Second: The reflection class where <code>&lt:exec:operation></code> is searched
 * <li>3.: All argument names
 * <li>last: The pattern to format the output.
 * </ul>
 * <b>Data for execution:</b>
 * A proper instance to store the argument data should be gotten via {@link #createArgumentDataObj()}. It is possible 
 * to build this instance on construction in the ctor of the users organization class. Then this instance is reused 
 * (save calculation time for allocation and garbage), possible in single thread usage. <pre>
 *   public MyWriterClass() { 
 *     dataMyText =   otxMyText.getArgumentData(this);
</pre>
 * If multithreading should be used (speed up generation time) to built output texts with the same instance of this, 
 * it is possible. This class is readonly and can be used without any restriction in multithreading, after it is contsructed.
 * Any thread need its own instance of the {@link DataTextPreparer} instance to work independent. 
 * <br><br>
 * <b>Call of preparation for output of a text:</b><br><pre>
 *    try {
 *      wr = new BufferedWriter(new FileWriter(toFile));
 *      dataMyText.setArgument("arg1", arg1Value);
 *      dataMyText.setArgument("arg2", arg2Value);
 *      otxMyText.exec(wr, dataMyText);
 *    } 
 *    catch(IOException exc) {....
 * </pre>The argument value are set. It is possible to use {@link DataTextPreparer#setArgument(int, Object)} too
 *   if the order of arguments is known and definitely. It is faster because the position of the argument 
 *   should not be searched (uses binary search, fast too). The it is executed. The output text is written 
 *   in the given {@link Appendable}, it may be a {@link java.lang.StringBuilder} too.
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
  public static final String version = "2019-05-12";
  
  
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
      args[ixArg] = value;
    }
    
    
    public void setExecObj(Object data) { execObj = data; }
  
    
    
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
    
    public Cmd(OutTextPreparer outer, ECmd what, StringPartScan textOrDatapath, Class<?> reflData) throws Exception { 
      super( checkVariable(outer, textOrDatapath), outer.nameVariables, reflData, true);
      this.cmd = what;
    }
    
    public Cmd(OutTextPreparer outer, ECmd what, String textOrDatapath, Class<?> reflData) throws Exception { 
      super( textOrDatapath, outer.nameVariables, reflData);
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
      super(outer, ECmd.forCtrl, sDatapath, reflData);
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
      super(outer, ECmd.setVar, sDatapath, reflData);
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
      super(outer, cmd, sDatapath, reflData);
    }
  }
  
  
  static class CallCmd extends Cmd {
    
    /**Index for {@link DataTextPreparer#argSub} to get already existing data container. */
    public int ixDataArg;
    
    /**The data to get actual arguments for this call. */
    public List<Argument> args;
    
    public CallCmd(OutTextPreparer outer, StringPartScan spDatapath, Class<?> reflData) throws Exception 
    { super(outer, ECmd.call, spDatapath, reflData); 
    }
 
    public CallCmd(OutTextPreparer outer, String sDatapath, Class<?> reflData) throws Exception 
    { super(outer, ECmd.call, sDatapath, reflData); 
    }
  }
  
  
  static class ExecCmd extends Cmd {
    
    /**Index for {@link DataTextPreparer#argSub} to get already existing data container. */
    public Method execOperation;
    
    
    public ExecCmd(OutTextPreparer outer, StringPartScan spDatapath, Class<?> reflData) throws Exception 
    { super(outer, ECmd.exec, spDatapath, reflData); 
    }
    
    public ExecCmd(OutTextPreparer outer, String sDatapath, Class<?> reflData) throws Exception 
    { super(outer, ECmd.exec, sDatapath, reflData); 
    }
  }
  
  

  static class DebugCmd extends Cmd {
    public String cmpString;

    public DebugCmd(OutTextPreparer outer, StringPartScan spDatapath, Class<?> reflData) throws Exception {
      super(outer, ECmd.debug, spDatapath, reflData);
    }

    public DebugCmd(OutTextPreparer outer, String sDatapath, Class<?> reflData) throws Exception {
      super(outer, ECmd.debug, sDatapath, reflData);
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
      super(sTextOrDatapath, outer.nameVariables, reflData);
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
  
  
  /**Via script given arguments for the outText. It does not contain the internal created variables. */
  private List<String> listArgs;
  
 
  int ctCall = 0;
  
  private List<Cmd> cmds = new ArrayList<Cmd>();
  
  
  public final String sIdent;
  
  /**Instantiates for a given pattern. 
   * @param pattern 
   * @throws Exception never, because the instantiation is possible especially for static variables.
   *   On faulty pattern the prepared cmd for output contains the error message. 
   */
  public OutTextPreparer(String ident, Class<?> reflData, String pattern) {
    this.sIdent = ident;
    this.parse(reflData, pattern);
  }
  
  /**Instantiates for a given pattern. 
   * @param pattern 
   * @throws Exception never, because the instantiation is possible especially for static variables.
   *   On faulty pattern the prepared cmd for output contains the error message. 
   */
  public OutTextPreparer(String ident, Class<?> reflData, String variables, String pattern) {
    this.sIdent = ident;
    this.parseVariables(variables);
    this.parse(reflData, pattern);
  }
  
  /**Instantiates for a given pattern. 
   * @param pattern 
   * @throws Exception never, because the instantiation is possible especially for static variables.
   *   On faulty pattern the prepared cmd for output contains the error message. 
   */
  public OutTextPreparer(String ident, Class<?> reflData, List<String> variables, String pattern) {
    this.sIdent = ident;
    this.setVariables(variables);
    this.parse(reflData, pattern);
  }
  
  
  
  private void parseVariables(String variables) {
    List<String> listvarValues = new LinkedList<String>();
    StringPartScan sp = new StringPartScan(variables);
    sp.setIgnoreWhitespaces(true);
    while(sp.scanStart().scanIdentifier().scanOk()) {
      String sVariable = sp.getLastScannedString();
      listvarValues.add(sVariable);
      if(!sp.scan(",").scanOk()) {
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
  private void parse(Class<?> reflData, String pattern) {
    int pos0 = 0; //start of current position after special cmd
    int pos1 = 0; //end before the next special command
    int[] ixCtrlCmd = new int[20];  //max. 20 levels for nested things.
    int ixixCmd = -1;
    StringPartScan sp = new StringPartScan(pattern);
    sp.setIgnoreWhitespaces(true);
    while(sp.length() >0) {
      sp.seek("<", StringPart.mSeekCheck + StringPart.mSeekEnd);
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
        else if(sp.scan(": >").scanOk()){ 
          sp.scanSkipSpace();
          pos0 = (int)sp.getCurrentPosition();  //after '>'
        }
        else if(sp.scan("&").scanToAnyChar(">", '\0', '\0', '\0').scan(">").scanOk()){
          final String sDatapath = sp.getLastScannedString();
//          if(sDatapath.startsWith("&("))
//            Debugutil.stop();
          //====> ////
          addCmd(pattern, pos0, pos1, ECmd.addVar, sDatapath, reflData);
          pos0 = (int)sp.getCurrentPosition();  //after '>'
        }
        else if(sp.scan(":if:").scanToAnyChar(">", '\\', '"', '"').scan(">").scanOk()) {
          //====>
          parseIf( pattern, pos0, pos1, ECmd.ifCtrl, sp, reflData);
          ixCtrlCmd[++ixixCmd] = cmds.size()-1;  //The position of the current if
          pos0 = (int)sp.getCurrentPosition();  //after '>'
          
        }
        else if(sp.scan(":elsif:").scanToAnyChar(">", '\\', '"', '"').scan(">").scanOk()) {
          //====>
          parseIf( pattern, pos0, pos1, ECmd.elsifCtrl, sp, reflData);
          Cmd ifCmdLast;
          int ixixIfCmd = ixixCmd; 
          if(  ixixIfCmd >=0 
            && ( (ifCmdLast = cmds.get(ixCtrlCmd[ixixCmd])).cmd == ECmd.ifCtrl 
               || ifCmdLast.cmd == ECmd.elsifCtrl
            )  ) {
            ((IfCmd)ifCmdLast).offsElsif = cmds.size() - ixCtrlCmd[ixixCmd] -1;   //The distance from <:if> to next <:elsif> 
          }else { 
            throw new IllegalArgumentException("OutTextPreparer " + this.sIdent + ": faulty <.elsif> without <:if> ");
          }
          ixCtrlCmd[++ixixCmd] = cmds.size()-1;  //The position of the current <:elsif>
          
          pos0 = (int)sp.getCurrentPosition();  //after '>'
        }
        else if(sp.scan(":else>").scanOk()) {
          //====>
          addCmd(pattern, pos0, pos1, ECmd.elseCtrl, null, null);
          Cmd ifCmd;
          int ixixIfCmd = ixixCmd; 
          if(  ixixIfCmd >=0 
              && ( (ifCmd = cmds.get(ixCtrlCmd[ixixCmd])).cmd == ECmd.ifCtrl 
                 || ifCmd.cmd == ECmd.elsifCtrl
                  )  ) {
            ((IfCmd)ifCmd).offsElsif = cmds.size() - ixCtrlCmd[ixixCmd] -1;   //The distance from <:if> to next <:elsif> 
          }else { 
            throw new IllegalArgumentException("OutTextPreparer " + this.sIdent + ": faulty <.elsif> without <:if> ");
          }
          ixCtrlCmd[++ixixCmd] = cmds.size()-1;  //The position of the current <:elsif>

          pos0 = (int)sp.getCurrentPosition();  //after '>'
        }
        else if(sp.scan(":for:").scanIdentifier().scan(":").scanToAnyChar(">", '\\', '"', '"').scan(">").scanOk()) {
          String container = sp.getLastScannedString().toString();
          String entryVar = sp.getLastScannedString().toString();
          //====>
          ForCmd cmd = (ForCmd)addCmd(pattern, pos0, pos1, ECmd.forCtrl, container, reflData);
          DataAccess.IntegerIx ixOentry = this.nameVariables.get(entryVar); 
          if(ixOentry == null) { //Check whether the same entry variable exists already from another for, only ones.
            ixOentry = new DataAccess.IntegerIx(this.nameVariables.size());         //create the entry variable newly.
            this.nameVariables.put(entryVar, ixOentry);
          }
          cmd.ixEntryVar = ixOentry.ix;
          entryVar += "_next";
          ixOentry = this.nameVariables.get(entryVar); 
          if(ixOentry == null) { //Check whether the same entry variable exists already from another for, only ones.
            ixOentry = new DataAccess.IntegerIx(this.nameVariables.size());         //create the entry variable newly.
            this.nameVariables.put(entryVar, ixOentry);
          }
          cmd.ixEntryVarNext = ixOentry.ix;
          ixCtrlCmd[++ixixCmd] = cmds.size()-1;
          pos0 = (int)sp.getCurrentPosition();  //after '>'
        }
        else if(sp.scan(":set:").scanIdentifier().scan("=").scanToAnyChar(">", '\\', '\'', '\'').scan(">").scanOk()) {
          String value = sp.getLastScannedString().toString();
          String variable = sp.getLastScannedString().toString();
          SetCmd cmd = (SetCmd)addCmd(pattern, pos0, pos1, ECmd.setVar, value, reflData);
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
          parseExec(pattern, pos0, pos1, sp, reflData);
          pos0 = (int)sp.getCurrentPosition();  //after '>'
        }
        else if(sp.scan(":call:").scanIdentifier().scanOk()) {
          //====>
          parseCall(pattern, pos0, pos1, sp, reflData);
          pos0 = (int)sp.getCurrentPosition();  //after '>'
        }
        else if(sp.scan(":debug:").scanIdentifier().scan(":").scanToAnyChar(">", '\\', '"', '"').scan(">").scanOk()) {
          String cmpString = sp.getLastScannedString().toString();
          String debugVar = sp.getLastScannedString().toString();
          //====>
          DebugCmd cmd = (DebugCmd)addCmd(pattern, pos0, pos1, ECmd.debug, debugVar, reflData);
          cmd.cmpString = cmpString;
          pos0 = (int)sp.getCurrentPosition();  //after '>'
        }  
        else if(sp.scan(":debug>").scanOk()) {
          //====>
          DebugCmd cmd = (DebugCmd)addCmd(pattern, pos0, pos1, ECmd.debug, null, reflData);
          pos0 = (int)sp.getCurrentPosition();  //after '>'
        }
        else if(sp.scan(":--").scanToAnyChar(">", '\0', '\0', '\0').scan(">").scanOk()) {
          //it is commented
          addCmd(pattern, pos0, pos1, ECmd.nothing, null, null);
          pos0 = (int)sp.getCurrentPosition();  //after '>'
        }
        else if(sp.scan(".if>").scanOk()) { //The end of an if
          Cmd cmd = null;
          addCmd(pattern, pos0, pos1, ECmd.nothing, null, reflData);  //The last text before <.if>
          while(  ixixCmd >=0 
            && ( (cmd = cmds.get(ixCtrlCmd[ixixCmd])).cmd == ECmd.ifCtrl 
              || cmd.cmd == ECmd.elsifCtrl
              || cmd.cmd == ECmd.elseCtrl
            )  ) {
            IfCmd ifcmd;
            cmd.offsEndCtrl = cmds.size() - ixCtrlCmd[ixixCmd];
            if(cmd.cmd != ECmd.elseCtrl && (ifcmd = (IfCmd)cmd).offsElsif <0) {
              ifcmd.offsElsif = ifcmd.offsEndCtrl; //without <:else>, go after <.if>
            }
            if(cmd.cmd != ECmd.ifCtrl) {
              cmd = null; //at least <:if> necessary.
            }
            ixixCmd -=1;
          } 
          if(cmd == null) {  //nothing found or <:if not found: 
            throw new IllegalArgumentException("OutTextPreparer " + this.sIdent + ": faulty <.if> without <:if> or  <:elsif> ");
          }
          pos0 = (int)sp.getCurrentPosition();  //after '>'
        }
        else if(sp.scan(".for>").scanOk()) { //The end of an if
          Cmd forCmd;
          if(ixixCmd >=0 && (forCmd = cmds.get(ixCtrlCmd[ixixCmd])).cmd == ECmd.forCtrl) {
            Cmd endLoop = addCmd(pattern, pos0, pos1, ECmd.endLoop, null, null);
            endLoop.offsEndCtrl = -cmds.size() - ixCtrlCmd[ixixCmd] -1;
            pos0 = (int)sp.getCurrentPosition();  //after '>'
            forCmd.offsEndCtrl = cmds.size() - ixCtrlCmd[ixixCmd];
            ixixCmd -=1;
          } 
          else throw new IllegalArgumentException("OutTextPreparer " + this.sIdent + ": faulty <:for>...<.for> ");
        }
        else { //No proper cmd found:
          
        }
      }
      else { //no more '<' found:
        sp.len0end();
        addCmd(pattern, pos0, pos0 + sp.length(), ECmd.nothing, null, reflData);
        sp.fromEnd();  //length is null then.
      }
    } //while
    if(ixixCmd >=0) {
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
 
  
  
  
  /**Explore the sDatapath and adds the proper Cmd
   * @param src The pattern to get text contents
   * @param from start position for text content
   * @param to end position for text content, if <= from then no text content is stored (both = 0)
   * @param ecmd The cmd kind
   * @param sDatapath null or a textual data path. It will be split to {@link Cmd#ixValue} and {@link Cmd#dataAccess}
   * @return the created Cmd for further parameters.
   */
  private Cmd addCmd(String src, int from, int to, ECmd ecmd, String sDatapath, Class<?> data) {
    if(to > from) {
      cmds.add(new CmdString(src.substring(from, to)));
    }
    final Cmd cmd;
    if(ecmd !=ECmd.nothing) {
      try {
        switch(ecmd) {
          case ifCtrl: case elsifCtrl: cmd = new IfCmd(this, ecmd, sDatapath, data); break;
          case call: cmd = new CallCmd(this, sDatapath, data); break;
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
      cmds.add(new CmdString(src.substring(from, to)));
    }
    final Cmd cmd;
    if(ecmd !=ECmd.nothing) {
      try {
        switch(ecmd) {
          case ifCtrl: case elsifCtrl: cmd = new IfCmd(this, ecmd, sDatapath, data); break;
          case call: cmd = new CallCmd(this, sDatapath, data); break;
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
      throw new IllegalArgumentException("The argument type does not match to the OutTextPreparer.");
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
          wr.append("<??OutTextPreparer script " + sIdent + ": " + cmd.textOrVar + " execution error: " + e.getMessage() + "??>");
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
            wr.append("<??OutTextPreparer script " + sIdent + ": " + cmd.textOrVar + " not found or access error: " + e.getMessage() + "??>");
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
          wr.append("<??OutTextPreparer script " + sIdent + ": " + cmd.textOrVar + " not found or access error: " + e.getMessage() + "??>");
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
          case addVar: {
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
        args.args[cmd.ixEntryVarNext] = item.getValue();
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
  
  
  
  
  @Override public String toString() { return sIdent; }
  
  
  void debug() {}
  
} //class OutTextPreparer
