package org.vishia.util;

import java.io.IOException;
import java.util.ArrayList;
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
  
  
  //StringFormatter fm;
  
  enum ECmd{
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
  
  
  
  class ValueAccess {
    
    /**Index of the value in the parents {@link OutTextPreparer.DataTextPreparer#args} or -1 if not used.*/
    public int ixValue;
    /**Set if the value uses reflection. */
    public final DataAccess dataAccess;
    
    public final Object dataConst;
    
    /**The String literal or the given sDatapath. */
    public final String text;

    
    public ValueAccess(String sTextOrDatapath, Object data) {
      assert(!(data instanceof Boolean));
      this.text = sTextOrDatapath;
      final String sVariable;
      if(data == null) {
        this.ixValue = -1;
        this.dataAccess = null;
        this.dataConst = null;
      } 
      else if(sTextOrDatapath !=null){
        int posep = sTextOrDatapath.indexOf('.');
        if(posep <0) {
          sVariable = sTextOrDatapath;
        } else {
          sVariable = sTextOrDatapath.substring(0, posep);
        }
        Integer ixO = vars.get(sVariable);
        if(ixO == null) {
          //The variable is not part of the argument names, it should be able to access via reflection in data:
          try{ 
            this.dataConst = DataAccess.access(sTextOrDatapath, data, true, false, false, null);
            this.ixValue = -1;
            this.dataAccess = null;
          } catch(Exception exc) {
            throw new IllegalArgumentException("OutTextPreparer script " + sIdent + ", argument: " + sVariable + " not existing: ");
          }
        } else {
          this.dataConst = null;
          this.ixValue = ixO.intValue();
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



  
  
  class Cmd extends ValueAccess{
    final ECmd cmd;
    
    /**If necessary it is the offset skipped over the ctrl sequence. */
    int offsEndCtrl;
    //abstract void exec();
    
    public Cmd(ECmd what, String textOrDatapath, Object data)
    { super(textOrDatapath, what == ECmd.addString ? null: data);
      this.cmd = what;
    }
    
    
    
    @Override public String toString() {
      return cmd + ":" + text;
    }
    
  }//sub class Cmd
  
  
  
  class ForCmd extends Cmd {
    int ixEntryVar;
    public ForCmd(String sDatapath, Object data) {
      super(ECmd.forCtrl, sDatapath, data);
    }
  }
  
  
  class CallCmd extends Cmd {
    
    /**Index for {@link DataTextPreparer#argSub} to get already existing data container. */
    public int ixDataArg;
    
    /**The data to get actual arguments for this call. */
    public List<Argument> args;
    
    public CallCmd(String sDatapath, Object data) 
    { super(ECmd.call, sDatapath, data); 
    }
  }
  
  

  class DebugCmd extends Cmd {
    String cmpString;
    public DebugCmd(String sDatapath, Object data) {
      super(ECmd.debug, sDatapath, data);
    }
  }
  
  
  
//  class CmdString extends Cmd {
//    public CmdString(String str, int pos0, int pos1)
//    { super(str, pos0, pos1);
//    }
//
//    public void exec() {
//      fm.add(str.substring(pos0, pos1));
//    }
//  };
//  
//  
//  
//  class CmdVar extends Cmd {
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
  
  
  class Argument extends ValueAccess {

    /**Name of the argument, null for usage in {@link Cmd} */
    public final String name;
    
    /**The Index to store the value in {@link DataTextPreparer#args} of the called level or -1 if not known. */
    public int ixDst;
    
    public Argument(String name, int ixCalledArg, String sTextOrDatapath, Object data)
    {
      super(sTextOrDatapath, data);
      this.name = name;
      this.ixDst = ixCalledArg;
    }
    
  }
  
  /**All argument variables sorted. */
  private Map<String, Integer> vars = new TreeMap<String, Integer>();
  
  private int ctVar = 0;
  

  int ctCall = 0;
  
  private List<Cmd> cmds = new ArrayList<Cmd>();
  
  
  public final String sIdent;
  
  /**Instantiates for a given prescript. 
   * @param prescript 
   */
  public OutTextPreparer(String ident, Object data, String variables, String pattern) {
    this.sIdent = ident;
    this.getVariables(variables);
    this.parse(pattern, data);
  }
  
  
  
  private void getVariables(String variables) {
    StringPartScan sp = new StringPartScan(variables);
    sp.setIgnoreWhitespaces(true);
    while(sp.scanStart().scanIdentifier().scanOk()) {
      String sVariable = sp.getLastScannedString();
      vars.put(sVariable, new Integer(ctVar));
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
          Integer ixOentry = vars.get(entryVar); //Check whether the same entry variable exists already from another for, only ones.
          if(ixOentry == null) { 
            ixOentry = ctVar; ctVar +=1;         //create the entry variable newly.
            vars.put(entryVar, ixOentry);
          }
          ForCmd cmd = (ForCmd)addCmd(pattern, pos0, pos1, ECmd.forCtrl, container, data);
          cmd.ixEntryVar = ixOentry;
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
            addCmd(pattern, pos0, pos1, null, null, data);
            pos0 = (int)sp.getCurrentPosition();  //after '>'
            ifCmd.offsEndCtrl = cmds.size() - ixCmd[ixixCmd];
            ixixCmd -=1;
          } 
          else throw new IllegalArgumentException("faulty <:if>...<.if> ");
        }
        else if(sp.scan(".for>").scanOk()) { //The end of an if
          Cmd forCmd;
          if(ixixCmd >=0 && (forCmd = cmds.get(ixCmd[ixixCmd])).cmd == ECmd.forCtrl) {
            Cmd endLoop = addCmd(pattern, pos0, pos1, ECmd.endLoop, null, data);
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
        addCmd(pattern, pos0, pos0 + sp.length(), null, null, data);
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
            Integer ixOcalledArg = call.vars.get(sNameArg);
            if(ixOcalledArg == null) {
              throw new IllegalArgumentException("OutTextPreparer "+ sIdent + ": <:call: " + sCallVar + ":argument not found: " + sNameArg);
            }
            ixCalledArg = ixOcalledArg;
          }
          Argument arg;
          if(sp.scanLiteral("''\\", -1).scanOk()) {
            String sText = sp.getLastScannedString().trim();
            arg = new Argument(sNameArg, ixCalledArg, sText, null);
          }
          else if(sp.scanToAnyChar(">,", '\\', '"', '"').scanOk()) {
            String sDataPath = sp.getLastScannedString().trim();
            arg = new Argument(sNameArg, ixCalledArg, sDataPath, data);
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
      cmds.add(new Cmd(ECmd.addString, src.substring(from, to), null));
    }
    final Cmd cmd;
    if(ecmd !=null) {
      switch(ecmd) {
        case call: cmd = new CallCmd(sDatapath, data); break;
        case forCtrl: cmd = new ForCmd(sDatapath, data); break;
        case debug: cmd = new DebugCmd(sDatapath, data); break;
        default: cmd = new Cmd(ecmd, sDatapath, data);
      }
      cmds.add(cmd);
    } else {
      cmd = null;
    }
    return cmd;
  }


  public DataTextPreparer getArgumentData() { return new DataTextPreparer(this); }
  
  
  
  

  
  

  /**Executes preparation
   * @param fm
   * @param values in order of first occurrence in the prescript
   * @throws IOException 
   */
  public void exec( Appendable sb, DataTextPreparer values) throws Exception {
    execSub(sb, values, 0, cmds.size());
  }
  
  
  /**Executes preparation
   * @param fm
   * @param values in order of first occurrence in the prescript
   * @throws IOException 
   */
  private void execSub( Appendable sb, DataTextPreparer values, int ixStart, int ixEndExcl ) throws Exception {
    //int ixVal = 0;
    int ixCmd = ixStart;
    while(ixCmd < ixEndExcl) {
      Cmd cmd = cmds.get(ixCmd++);
      Object data;
      if(cmd.ixValue >=0) {
        data = values.args[cmd.ixValue];
        if(cmd.dataAccess !=null) {
          try {
            data = cmd.dataAccess.access(data, true, false);
          } catch (Exception e) {
            // TODO Auto-generated catch block
            throw new IllegalArgumentException("OutTextPreparer script " + sIdent + ": " + cmd.text + " not found or access error: " + e.getMessage());
          }
        }
      } else { 
        data = cmd.dataConst;  //may be given or null 
      }
      switch(cmd.cmd) {
        case addString: sb.append(cmd.text); break;
        case addVar: {
          //Integer ixVar = vars.get(cmd.str);
          sb.append(data.toString());
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
          ForCmd cmd1 = (ForCmd)cmd;
          if(data == null) {
            //do nothing, no for
          }
          else if(data instanceof Iterable) {
            for(Object item: (Iterable)data) {
              values.args[cmd1.ixEntryVar] = item;
              execSub(sb, values, ixCmd, ixCmd + cmd.offsEndCtrl -1);
            }
          }
          else if(data instanceof Map) {
            @SuppressWarnings("unchecked") Map<Object, Object>map = ((Map<Object,Object>)data);
            for(Map.Entry<Object,Object> item: map.entrySet()) {
              values.args[cmd1.ixEntryVar] = item.getValue();
              execSub(sb, values, ixCmd, ixCmd + cmd.offsEndCtrl -1);
            }
          }
          else throw new IllegalArgumentException("OutTextPreparer script " + sIdent + ": for variable is not an container: " + cmd.text );
          ixCmd += cmd.offsEndCtrl -1;
        } break;
        case call: 
          if(!(data instanceof OutTextPreparer)) throw new IllegalArgumentException("<call: variable should be a OutTextPreparer>");
          execCall(sb, (CallCmd)cmd, values, (OutTextPreparer)data); break;
        case debug: {
          if(data.toString().equals(((DebugCmd)cmd).cmpString)){
            Debugutil.stop();
          }
        } break;
      }
    }
  }
  
  
  
  
  /**Executes a call
   * @param sb the output channel
   * @param cmd The CallCmd
   * @param values actual values of the calling level
   * @param callVar The OutTextPreparer which is called here.
   * @throws Exception
   */
  private void execCall(Appendable sb, CallCmd cmd, DataTextPreparer values, OutTextPreparer callVar) throws Exception {
    OutTextPreparer callVar1 = (OutTextPreparer)callVar;
    DataTextPreparer valSub;
    if(cmd.args !=null) {
      valSub = values.argSub[cmd.ixDataArg];
      if(valSub == null) { //only first time for this call
        values.argSub[cmd.ixDataArg] = valSub = callVar1.getArgumentData();  //create a data instance for arguments.
      }
      for(Argument arg : cmd.args) {
        Integer ixO = callVar1.vars.get(arg.name);
        if(ixO == null) { sb.append("<??ERROR: faulty arg name:" + arg.name + "??>"); }
        else {
          Object value;
          if(arg.ixValue <0) {
            value = arg.text;   //String literal
          } else {
            value = values.args[arg.ixValue];
            if(arg.dataAccess !=null) {
              value = arg.dataAccess.access(value, true, false);
            }
          }
          if(arg.ixDst >=0) {
            valSub.setArgument(arg.ixDst, value);
          } else {
            valSub.setArgument(arg.name, value);
          }
        }
      }
    } else {
      valSub = values;
    }
    callVar1.exec(sb, valSub);
  }
  
  
  
  
 
  
  
  
  @Override public String toString() { return sIdent; }
  
  
  
  
  
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
      Integer ix0 = prep.vars.get(name);
      if(ix0 == null) throw new IllegalArgumentException("OutTextPreparer script " + prep.sIdent + ", argument: " + name + " not existing: ");
      int ix = ix0.intValue();
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
  
  
}
