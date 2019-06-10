package org.vishia.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;



/**This class helps to prepare a text with data. It is the small solution for {@link org.vishia.jztxtcmd.JZtxtcmd}
 * only for text preparation of one or a few lines.
 * <br>
 * The prescript to build the text is parsed from a given String: {@link #parse(String)}.
 * It is stored in an instance of this as a list of items. Any item is marked with a value of {@link ECmd}.
 * On one of the {@link #exec(StringFormatter, Object...)} routines a text output is produced with this prescript and given data.
 * <br>
 * <b>Basic example: </b>
 * ...
 * <br>
 * @author Hartmut Schorrig
 *
 */
public class OutTextPreparer
{

  /**Version, history and license.
   * <ul>
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
   * An instance can be gotten from {@link OutTextPreparer#getArgumentData()} proper and associated to the out text.
   * The constructor of this class is not public, it should be only gotten with this routine.
   * <br><br>
   * Invoke {@link #setArgument(String, Object)} to set any named argument. 
   *
   */
  public static class DataTextPreparer {
    
    /**The associated const data for OutText preparation. */
    final OutTextPreparer prep;
    
    /**Array of all arguments. */
    Object[] args;
    
    /**Any &lt;call in the pattern get the data for the called OutTextPreparer, but only ones, reused. */
    DataTextPreparer[] argSub;
    
    /**Package private constructor invoked only in {@link OutTextPreparer#getArgumentData()}*/
    DataTextPreparer(OutTextPreparer prep){
      this.prep = prep;
      if(prep.ctVar >0) {
        args = new Object[prep.ctVar];
      }
      if(prep.ctCall >0) {
        argSub = new DataTextPreparer[prep.ctCall];
      }
      
    }
    
    
    
    /**User routine to set a named argument with a value.
     * If a faulty name is used, an Exception is thrown 
     * @param name argument name of the {@link OutTextPreparer#OutTextPreparer(String, Object, String, String)}, 3. argment
     * @param value any value for this argument.
     * */
    public void setArgument(String name, Object value) {
      IntegerIx ix0 = prep.vars.get(name);
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
    
  
  }

  enum ECmd{
    nothing('-', "nothing"), 
    addString('s', "str"),
    addVar('v', "var"),
    ifCtrl('I', "if"),
    forCtrl('F', "for"),
    endLoop('B', "endloop"),
    call('C', "call"),
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
  
  
  
  static class ValueAccess {
    
    /**Index of the value in the parents {@link OutTextPreparer.DataTextPreparer#args} or -1 if not used.*/
    public int ixValue;
    /**Set if the value uses reflection. */
    public final DataAccess dataAccess;
    
    /**Set on construction of {@link OutTextPreparer#OutTextPreparer(String, Object, String, String)}
     * if the value reference is constant, non modified by user data). 
     * It is especially for call otxXYZ. */
    public final Object dataConst;
    
    /**The String literal or the given sDatapath. */
    public final String text;
    
    
    /**A ValueAccess without value, only a constant text literal, especially for Text Cmd.
     * @param text
     */
    public ValueAccess(String text) {
      this.ixValue = -1;
      this.dataAccess = null;
      this.dataConst = null;
      this.text = text;
    }
    
    
    /**Either a value access with given data (base of {@link Argument}, or a Command with value access, base of {@link Cmd}
     * @param outer
     * @param sTextOrDatapath
     * @param data
     */
    public ValueAccess(OutTextPreparer outer, String sTextOrDatapath, Object data) {
      this.text = sTextOrDatapath;
      final String sVariable;
      if(sTextOrDatapath !=null){
        int posep = sTextOrDatapath.indexOf('.');
        if(posep <0) {
          sVariable = sTextOrDatapath;
        } else {
          sVariable = sTextOrDatapath.substring(0, posep);
        }
        IntegerIx ixO = outer.vars.get(sVariable);
        if(ixO == null) {
          //The variable is not part of the argument names, it should be able to access via reflection in data:
          try{ 
            this.dataConst = DataAccess.access(sTextOrDatapath, data, true, false, false, null);
            this.ixValue = -1;
            this.dataAccess = null;
          } catch(Exception exc) {
            throw new IllegalArgumentException("OutTextPreparer script " + outer.sIdent + ", argument: " + sVariable + " not existing: ");
          }
        } else {
          this.dataConst = null;
          this.ixValue = ixO.ix;
          if(posep <0) {
            this.dataAccess = null;
          } else {
            this.dataAccess = new DataAccess(sTextOrDatapath.substring(posep+1));
          }
        }
      }
      else { //empty without text and without datapath
        this.ixValue = -1;
        this.dataAccess = null;
        this.dataConst = null;
      }
    }

  }



  
  
  static class Cmd extends ValueAccess{
    public final ECmd cmd;
    
    /**If necessary it is the offset skipped over the ctrl sequence. */
    public int offsEndCtrl;
    
    public Cmd(ECmd what, String textOrDatapath)
    { super( textOrDatapath);
      this.cmd = what;
    }
    
    public Cmd(OutTextPreparer outer, ECmd what, String textOrDatapath, Object data)
    { super( outer, textOrDatapath, data);
      this.cmd = what;
    }
    
    @Override public String toString() {
      return cmd + ":" + text;
    }
    
  }//sub class Cmd
  
  
  
  static class ForCmd extends Cmd {
    
    /**The index where the entry value is stored while executing. 
     * Determined in ctor ({@link OutTextPreparer#parse(String, Object)} */
    public int ixEntryVar, ixEntryVarNext;

    public ForCmd(OutTextPreparer outer, String sDatapath, Object data) {
      super(outer, ECmd.forCtrl, sDatapath, data);
    }
  }
  
  
  static class CallCmd extends Cmd {
    
    /**Index for {@link DataTextPreparer#argSub} to get already existing data container. */
    public int ixDataArg;
    
    /**The data to get actual arguments for this call. */
    public List<Argument> args;
    
    public CallCmd(OutTextPreparer outer, String sDatapath, Object data) 
    { super(outer, ECmd.call, sDatapath, data); 
    }
  }
  
  

  static class DebugCmd extends Cmd {
    public String cmpString;

    public DebugCmd(OutTextPreparer outer, String sDatapath, Object data) {
      super(outer, ECmd.debug, sDatapath, data);
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
  
  
  static class Argument extends ValueAccess {

    /**Name of the argument, null for usage in {@link Cmd} */
    public final String name;
    
    /**The Index to store the value in {@link DataTextPreparer#args} of the called level or -1 if not known. */
    public final int ixDst;
    
    public Argument(OutTextPreparer outer, String name, int ixCalledArg, String sTextOrDatapath, Object data) {
      super(outer, sTextOrDatapath, data);
      this.name = name;
      this.ixDst = ixCalledArg;
    }
    
  }
  
  
  /**Wrapper around the index as integer. An instance is member of {@link OutTextPreparer#vars}. 
   * Necessary to detect a null reference. In Java Integer can be used too, but that is more explicitly. 
   */
  static class IntegerIx {
    public int ix;
    IntegerIx(int value){ ix = value; }
  }
  
  /**All argument variables sorted. */
  private Map<String, IntegerIx> vars = new TreeMap<String, IntegerIx>();
  
  private int ctVar = 0;
  

  int ctCall = 0;
  
  private List<Cmd> cmds = new ArrayList<Cmd>();
  
  
  public final String sIdent;
  
  /**Instantiates for a given prescript. 
   * @param prescript 
   */
  public OutTextPreparer(String ident, Object data, String variables, String pattern) {
    this.sIdent = ident;
    this.parseVariables(variables);
    this.parse(pattern, data);
  }
  
  
  
  private void parseVariables(String variables) {
    StringPartScan sp = new StringPartScan(variables);
    sp.setIgnoreWhitespaces(true);
    while(sp.scanStart().scanIdentifier().scanOk()) {
      String sVariable = sp.getLastScannedString();
      vars.put(sVariable, new IntegerIx(ctVar));
      ctVar +=1;
      if(!sp.scan(",").scanOk()) {
        break; //, as separator
      }
    }
    sp.close();
  }
  
  
  private void parse(String pattern, Object data) {
    int pos0 = 0; //start of current position after special cmd
    int pos1 = 0; //end before the next special command
    int[] ixCmd = new int[10];  //max. 10 levels for nested things.
    int ixixCmd = -1;
    StringPartScan sp = new StringPartScan(pattern);
    sp.setIgnoreWhitespaces(true);
    while(sp.length() >0) {
      sp.seek("<", StringPart.mSeekCheck + StringPart.mSeekEnd);
      if(sp.found()) {
        
        pos1 = (int)sp.getCurrentPosition() -1; //before <
        //sp.fromEnd().seek("<").scan().scanStart();
        sp.scan().scanStart();
        //if(sp.scan("&").scanIdentifier().scan(">").scanOk()){
        if(sp.scan("&").scanToAnyChar(">", '\0', '\0', '\0').scan(">").scanOk()){
          final String sDatapath = sp.getLastScannedString();
          addCmd(pattern, pos0, pos1, ECmd.addVar, sDatapath, data);
          pos0 = (int)sp.getCurrentPosition();  //after '>'
        }
        else if(sp.scan(":if:").scanToAnyChar(">", '\\', '"', '"').scan(">").scanOk()) {
          String cond = sp.getLastScannedString().toString();
          addCmd(pattern, pos0, pos1, ECmd.ifCtrl, cond, data);
          ixCmd[++ixixCmd] = cmds.size()-1;
          pos0 = (int)sp.getCurrentPosition();  //after '>'
          
        }
        else if(sp.scan(":for:").scanIdentifier().scan(":").scanToAnyChar(">", '\\', '"', '"').scan(">").scanOk()) {
          String container = sp.getLastScannedString().toString();
          String entryVar = sp.getLastScannedString().toString();
          ForCmd cmd = (ForCmd)addCmd(pattern, pos0, pos1, ECmd.forCtrl, container, data);
          IntegerIx ixOentry = vars.get(entryVar); 
          if(ixOentry == null) { //Check whether the same entry variable exists already from another for, only ones.
            ixOentry = new IntegerIx(ctVar); ctVar +=1;         //create the entry variable newly.
            vars.put(entryVar, ixOentry);
          }
          cmd.ixEntryVar = ixOentry.ix;
          entryVar += "_next";
          ixOentry = vars.get(entryVar); 
          if(ixOentry == null) { //Check whether the same entry variable exists already from another for, only ones.
            ixOentry = new IntegerIx(ctVar); ctVar +=1;         //create the entry variable newly.
            vars.put(entryVar, ixOentry);
          }
          cmd.ixEntryVarNext = ixOentry.ix;
          ixCmd[++ixixCmd] = cmds.size()-1;
          pos0 = (int)sp.getCurrentPosition();  //after '>'
        }
        else if(sp.scan(":call:").scanIdentifier().scanOk()) {
          parseCall(pattern, pos0, pos1, sp, data);
          pos0 = (int)sp.getCurrentPosition();  //after '>'
        }
        else if(sp.scan(":debug:").scanIdentifier().scan(":").scanToAnyChar(">", '\\', '"', '"').scan(">").scanOk()) {
          String cmpString = sp.getLastScannedString().toString();
          String debugVar = sp.getLastScannedString().toString();
          DebugCmd cmd = (DebugCmd)addCmd(pattern, pos0, pos1, ECmd.debug, debugVar, data);
          cmd.cmpString = cmpString;
          pos0 = (int)sp.getCurrentPosition();  //after '>'
          
        }
        else if(sp.scan(".if>").scanOk()) { //The end of an if
          Cmd ifCmd;
          if(ixixCmd >=0 && (ifCmd = cmds.get(ixCmd[ixixCmd])).cmd == ECmd.ifCtrl) {
            addCmd(pattern, pos0, pos1, ECmd.nothing, null, data);
            pos0 = (int)sp.getCurrentPosition();  //after '>'
            ifCmd.offsEndCtrl = cmds.size() - ixCmd[ixixCmd];
            ixixCmd -=1;
          } 
          else throw new IllegalArgumentException("faulty <:if>...<.if> ");
        }
        else if(sp.scan(".for>").scanOk()) { //The end of an if
          Cmd forCmd;
          if(ixixCmd >=0 && (forCmd = cmds.get(ixCmd[ixixCmd])).cmd == ECmd.forCtrl) {
            Cmd endLoop = addCmd(pattern, pos0, pos1, ECmd.endLoop, null, null);
            endLoop.offsEndCtrl = -cmds.size() - ixCmd[ixixCmd] -1;
            pos0 = (int)sp.getCurrentPosition();  //after '>'
            forCmd.offsEndCtrl = cmds.size() - ixCmd[ixixCmd];
            ixixCmd -=1;
          } 
          else throw new IllegalArgumentException("faulty <:for>...<.for> ");
        }
        else { //No proper cmd found:
          
        }
      }
      else { //no more '<' found:
        sp.len0end();
        addCmd(pattern, pos0, pos0 + sp.length(), ECmd.nothing, null, data);
        sp.fromEnd();  //length is null then.
      }
    } //while
    sp.close();
  }

  
  
  private void parseCall(final String src, final int pos0, final int pos1, final StringPartScan sp, Object data) {
    String sCallVar = sp.getLastScannedString();
    CallCmd cmd = (CallCmd)addCmd(src, pos0, pos1, ECmd.call, sCallVar, data);
    OutTextPreparer call = null;
    if(cmd.dataConst !=null) {
      if(!(cmd.dataConst instanceof OutTextPreparer)) {
        throw new IllegalArgumentException("OutTextPreparer "+ sIdent + ": <:call: " + sCallVar + " is const but not a OutTextPreparer");
      }
      call = (OutTextPreparer)cmd.dataConst;
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
            IntegerIx ixOcalledArg = call.vars.get(sNameArg);
            if(ixOcalledArg == null) {
              throw new IllegalArgumentException("OutTextPreparer "+ sIdent + ": <:call: " + sCallVar + ":argument not found: " + sNameArg);
            }
            ixCalledArg = ixOcalledArg.ix;
          }
          Argument arg;
          if(sp.scanLiteral("''\\", -1).scanOk()) {
            String sText = sp.getLastScannedString().trim();
            arg = new Argument(this, sNameArg, ixCalledArg, sText, null);
          }
          else if(sp.scanToAnyChar(">,", '\\', '"', '"').scanOk()) {
            String sDataPath = sp.getLastScannedString().trim();
            arg = new Argument(this, sNameArg, ixCalledArg, sDataPath, data);
          }
          else { 
            throw new IllegalArgumentException("OutTextPreparer "+ sIdent + ": syntax error for argument value in <:call: " + sCallVar + ":arguments>");
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
  
  
  
 
  
  
  
  /**Explore the sDatapath and adds the proper Cmd
   * @param src The pattern to get text contents
   * @param from start position for text content
   * @param to end position for text content, if <= from then no text content is stored (both = 0)
   * @param ecmd The cmd kind
   * @param sDatapath null or a textual data path. It will be split to {@link Cmd#ixValue} and {@link Cmd#dataAccess}
   * @return the created Cmd for further parameters.
   */
  private Cmd addCmd(String src, int from, int to, ECmd ecmd, String sDatapath, Object data) {
    if(to > from) {
      cmds.add(new CmdString(src.substring(from, to)));
    }
    final Cmd cmd;
    if(ecmd !=ECmd.nothing) {
      switch(ecmd) {
        case call: cmd = new CallCmd(this, sDatapath, data); break;
        case forCtrl: cmd = new ForCmd(this, sDatapath, data); break;
        case debug: cmd = new DebugCmd(this, sDatapath, data); break;
        default: cmd = new Cmd(this, ecmd, sDatapath, data); break;
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
  public DataTextPreparer getArgumentData() { return new DataTextPreparer(this); }
  
  
  
  

  
  

  /**Executes preparation of a pattern with given data.
   * Note: The instance data of this are not changed. The instance can be used 
   * in several threads or multicore processing.
   * @param wr The output channel
   * @param args for preparation. 
   *   The value instance should be gotten with {@link OutTextPreparer#getArgumentData()}
   *   proper to the instance of this, because the order of arguments should be match. 
   *   It is internally tested. 
   * @throws Exception 
   */
  public void exec( Appendable wr, DataTextPreparer args) throws IOException {
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
      Cmd cmd = cmds.get(ixCmd++);
      Object data;
      if(cmd.ixValue >=0) {
        data = args.args[cmd.ixValue];
        if(cmd.dataAccess !=null) {
          try {
            data = cmd.dataAccess.access(data, true, false);
          } catch (Exception e) {
            wr.append("<??OutTextPreparer script " + sIdent + ": " + cmd.text + " not found or access error: " + e.getMessage() + "??>");
          }
        }
      } else { 
        data = cmd.dataConst;  //may be given or null 
      }
      switch(cmd.cmd) {
        case addString: wr.append(cmd.text); break;
        case addVar: {
          //Integer ixVar = vars.get(cmd.str);
          wr.append(data.toString());
        } break;
        case ifCtrl: {
          if(  data ==null 
            || (data instanceof Boolean && ! ((Boolean)data).booleanValue()) 
            || (data instanceof Number  && ((Number)data).intValue() == 0)) {
            ixCmd += cmd.offsEndCtrl -1;  //if is false, go to the end.
          } else {
            //forward inside if
            Debugutil.stop();
          }
        } break;
        case forCtrl: {
          execFor(wr, (ForCmd)cmd, ixCmd, data, args);;
          ixCmd += cmd.offsEndCtrl -1;
        } break;
        case call: 
          if(!(data instanceof OutTextPreparer)) {
            wr.append("<?? OutTextPreparer script " + sIdent + "<call: variable is not an OutTextPreparer ??>");
          } else {
            execCall(wr, (CallCmd)cmd, args, (OutTextPreparer)data);
          } 
          break;
        case debug: {
          if(data.toString().equals(((DebugCmd)cmd).cmpString)){
            debug();
          }
        } break;
      }
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
      wr.append("<?? OutTextPreparer script " + sIdent + ": for variable is not an container: " + cmd.text + "??>");
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
        args.argSub[cmd.ixDataArg] = valSub = callVar1.getArgumentData();  //create a data instance for arguments.
      }
      for(Argument arg : cmd.args) {
        Object value;
        if(arg.ixValue <0) {
          value = arg.text;   //String literal
        } else {
          value = args.args[arg.ixValue];
          if(arg.dataAccess !=null) {
            try{ value = arg.dataAccess.access(value, true, false); }
            catch(Exception exc) { wr.append("<??OutTextPreparer script " + this.sIdent + ": " + arg.text + " not found or access error: " + exc.getMessage() + "??>"); }
          }
        }
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
