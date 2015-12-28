package org.vishia.stateMGen;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.TreeMap;

import javax.script.ScriptException;

import org.vishia.mainCmd.MainCmd;
import org.vishia.mainCmd.MainCmd_ifc;
import org.vishia.cmd.JZcmdExecuter;
import org.vishia.cmd.JZcmdFilepath;
import org.vishia.states.StateComposite;
import org.vishia.states.StateCompositeFlat;
import org.vishia.states.StateMachine;
import org.vishia.states.StateParallel;
import org.vishia.states.StateSimple;
import org.vishia.util.DataShow;
import org.vishia.util.Debugutil;
import org.vishia.zbnf.ZbnfJavaOutput;
import org.vishia.zcmd.OutputDataTree;
import org.vishia.zcmd.JZcmd;
import org.vishia.zcmd.Zbnf2Text;
import org.vishia.zcmd.Zbnf2Text.Out;

/**This class prepares information for a state machine from representation in text format 
 * for generation C-code and documentation.
 * It calls the Zbnf parser and Zbnf2Text for generation.
 * See {@link #main(String[])}
 * 
 * @author Hartmut Schorrig
 *
 */
public class StateMGen {
  
  /**Version, history and license.
   * <ul>
   * <li>2015-11-07 Hartmut chg: some comment and renaming while documentation.
   * <li>2014-08-10 Hartmut chg: dissipate ZBNF result and prepared state data
   * <li>2014-06-15 Hartmut chg: scriptVariable is stateData instead state, better more non-ambiguous.
   * <li>
   * <li>2012-10-07 Hartmut creation 
   * </ul>
   * 
   * <b>Copyright/Copyleft</b>:
   * For this source the LGPL Lesser General Public License, published by the Free Software Foundation is valid.
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
   * 
   * 
   */
  //@SuppressWarnings("hiding")
  static final public String sVersion = "2014-06-26";


  /** Aggregation to the Console implementation class.*/
  final MainCmd_ifc console;




  /*---------------------------------------------------------------------------------------------*/
  /** main started from java*/
  public static void main(String [] args)
  { Zbnf2Text.Args cmdlineArgs = new Zbnf2Text.Args();     //holds the command line arguments.
    Zbnf2Text.CmdLineText mainCmdLine = new Zbnf2Text.CmdLineText(cmdlineArgs, args); //the instance to parse arguments and others.
    boolean bOk = true;
    try{ mainCmdLine.parseArguments(); }
    catch(Exception exception)
    { mainCmdLine.setExitErrorLevel(MainCmd_ifc.exitWithArgumentError);
      bOk = false;
    }
    if(bOk)
    { /**Now instantiate the main class. 
       * It is possible to create some aggregates (final references) first outside depends on args.
       * Therefore the main class is created yet here.
       */
      StateMGen main = new StateMGen(mainCmdLine);
      /** The execution class knows the Main class in form of the MainCmd super class
          to hold the contact to the command line execution.
      */
      try
      { main.execute(cmdlineArgs); 
      }
      catch(Exception exception)
      { //catch the last level of error. No error is reported direct on command line!
        main.console.report("Uncatched Exception on main level:", exception);
        main.console.setExitErrorLevel(MainCmd_ifc.exitWithErrors);
      }
    }
    mainCmdLine.exit();
  }

  
  
  
  /**This class gets and stores the results from a parsed ZBNF-component <code>constant</code>.
   * 
   */
  public static class XXXSimpleState
  {
    /**From ZBNF: <$?@stateName>. */
    public String stateName;
    
    /**From ZBNF: <$?@enclState>. */
    public String enclState;
    
    /**From ZBNF: <""?description>. */
    public String description;
    
    
  }
  
  
  /**This class gets and stores the results from a parsed ZBNF-component <code>constant</code>.
   * 
   */
  public static class XXXCompositeState
  {
    /**From ZBNF: <$?@stateName>. */
    public String stateName;
    
    /**From ZBNF: <""?description>. */
    public String description;
    
    public List<XXXSimpleState> simpleState;
    
    public List<XXXCompositeState> compositeState;
    
    public List<XXXParallelState> parallelState;
    
  }
  

  /**This class gets and stores the results from a parsed ZBNF-component <code>constant</code>.
   * 
   */
  public static class XXXParallelState extends XXXCompositeState
  {
  }
  
  
  /**This class gets and stores the results from a parsed ZBNF-component <code>constant</code>.
   * 
   */
  public static class XXXStateStructure
  {
    //public List<CompositeState> compositeState;
    public final List<XXXCompositeState> compositeStates = new LinkedList<XXXCompositeState>();
    
    
    public XXXCompositeState new_CompositeState()
    { return new XXXCompositeState();
    }
    
    public void add_CompositeState(XXXCompositeState value)
    { 
      compositeStates.add(value);
    }
    
    
  }
  
  
  
  /**This class gets and stores the results from a parsed ZBNF-component <code>State</code>.
   * 
   */
  public static class ZbnfConstDef
  {  public String description;
  
     public List<String> ident;
     
     public String code;
  }
  

  
  
  
  /**This is the base class both from any {@link ZbnfState} which can be a composite state and the {@link ZbnfResultData}
   * which presents the whole state machine.
   */
  public static class ZbnfStateCompositeBase
  {
    List<ZbnfState> subStates;
    
    int nrofSubstates;
    
    final Map<String, ZbnfState> XXXidxSrcStates = new TreeMap<String, ZbnfState>();
    
    public ZbnfState new_state()
    { return new ZbnfState();
    }
    
    public void add_state(ZbnfState value)
    { 
      nrofSubstates +=1;
      if(subStates == null) { subStates = new ArrayList<ZbnfState>(); }
      subStates.add(value);
      XXXidxSrcStates.put(value.stateName, value);
    }
    


  }
  
  
  
  
  
  /**This class gets and stores the results from a parsed ZBNF-component <code>State</code>.
   * 
   */
  public static class ZbnfState extends ZbnfStateCompositeBase
  {
    /**From ZBNF: <$?@stateName>. */
    public String stateName;
    
    public String stateNr;
    
    /**From ZBNF: <?stateParallel>>. Set if it is a parallel state container. */
    public boolean stateParallel;
    
    /**From ZBNF: \? <*|\?\.?description>. */
    public String description;
    
    /**From ZBNF: \? <*|\?\.?shortdescription>. */
    public String shortdescription;
    
    /**From ZBNF: + <*|\?\.?additionaldescription>. */
    public String additionaldescription;
    
    /**From ZBNF: ! <*|\?\.?description>. */
    public String tododescription;
    
    public boolean hasHistory;
    
    public ZbnfEntry entry;
    
    public ZbnfExit exit;
    
    public ZbnfEntry instate;
    
    public List<ZbnfTrans> trans;
    
    public ZbnfEntry new_instate() { return new ZbnfEntry(); }

    public void set_instate(ZbnfEntry val) { instate = val; }
    
    public void set_hasHistory(){ hasHistory = true; }
    
    /**Set to true if the state was prepared already. */
    boolean isPrepared = false;
    
    @Override public String toString(){ return stateName; }
  
  }
  

  
  
  
  /**This class gets and stores the results from a parsed ZBNF-component <code>constant</code>.
   * 
   */
  public static class ZbnfEntry
  {
    /**From ZBNF: \? <*|\?\.?description>. */
    public String description;
    
    /**From ZBNF: + <*|\?\.?additionaldescription>. */
    public String additionaldescription;
    
    /**From ZBNF: ! <*|\?\.?description>. */
    public String tododescription;
    
    public String code;
  }
  
  /**This class gets and stores the results from a parsed ZBNF-component <code>constant</code>.
   * 
   */
  public static class ZbnfExit
  {
    /**From ZBNF: \? <*|\?\.?description>. */
    public String description;
    
    /**From ZBNF: + <*|\?\.?additionaldescription>. */
    public String additionaldescription;
    
    /**From ZBNF: ! <*|\?\.?description>. */
    public String tododescription;
    
    public String code;
  }
  
  
  
  
  public static class ZbnfJoinState
  {
    public String name;
  }
  
  
  
  
  
  /**This class gets and stores the results from a parsed ZBNF-component <code>constant</code>.
   * 
   */
  public static class ZbnfTrans
  {
    /**From ZBNF: \? <*|\?\.?description>. */
    public String description;
    
    public int nrTrans = 0;
    
    /**From ZBNF: + <*|\?\.?additionaldescription>. */
    public String additionaldescription;
    
    /**From ZBNF: ! <*|\?\.?description>. */
    public String tododescription;
    
    public String cond;
    
    public String time;
    
    private List<String> joinStatesSrc;
    
    //public List<ZbnfSimpleState> joinStates;
    public List<String> joinStates;
    
    public String event;
    
    public String code;
    
    private final List<ZbnfTrans> subCondition = new LinkedList<ZbnfTrans>();

    
    private ZbnfDstState dstStateTree; 

    public List<String> dstStates;

    public String dstState;
    
    public ZbnfTrans new_subCondition(){ return new ZbnfTrans(); }
    
    public void add_subCondition(ZbnfTrans value){ subCondition.add(value); }
    
    /**From Zbnf parser: <dstState> dstState::=<$?name>. */
    public void set_dstState(String val){ 
      dstState = val; 
    }
    
    public void set_fork() {
      if(dstStates == null){
        dstStates = new LinkedList<String>();
      }
      dstStates.add(dstState);
      dstState = null;
    }

    public void set_condP(String val){
      int ix = val.lastIndexOf(')');  //the condP ends with ')' in any case, don't store it
      if(ix>0){                       //because it is ( cond  ) {
        cond = val.substring(0, ix);  //without last )
      } else {
      	cond = val;
      }
    }
    
    
    public void set_timeP(String val){
      int ix = val.lastIndexOf(')');  //the condP ends with ')' in any case, don't store it
      if(ix>0){                       //because it is ( cond  ) {
        time = val.substring(0, ix);  //without last )
      } else {
      	time = val;
      }
    }
    
    
    public void set_joinState(String val){
    	if(joinStatesSrc==null){ joinStatesSrc = new LinkedList<String>(); }
    	joinStatesSrc.add(val);
    }
    
    
    public void set_history(){
    	//TODO assign to last add_dstState
    }
    
    
    public ZbnfJoinState new_joinState(){ return new ZbnfJoinState(); }
    
    public void add_joinState(ZbnfJoinState val){ 
      if(joinStates == null){ joinStates = new LinkedList<String>(); }
      joinStates.add(val.name);  //only the name is need, garbage the val.
    }
    
    //public void add_dstState(DstState val){} 
    
  }
  
  
  
  
  
  /**This class gets and stores the results from a parsed ZBNF-component <code>dstState</code>.
   * After {@link StateMGen#prepareStateTrans(State, ZbnfResultData)} it holds the information
   * about entry in state while processing a transition.
   * 
   */
  public static class ZbnfDstState
  {
    /**From Zbnf parser: dstState::=<$?name>. */
    public String name;
    
    private List<ZbnfDstState> entrySubStates;
    
    @Override public String toString(){ return name; }
    
  }
  
  
  public static class ZbnfNameValue{ 
    public String name;
    public String value;
  }
  
  
  /**This is the root class for the Zbnf parsing result.
   * It refers to the main syntax component.
   */
  public static class ZbnfResultData extends ZbnfStateCompositeBase
  {

    public XXXStateStructure stateStructure;

    public List<String> includeLines = new LinkedList<String>();
    
    public List<String> statefnargs = new LinkedList<String>();
    
    /**String of all formal arguments for the state routines built from ZBNF result statefnarg. */
    public StringBuilder formalArgs;
    
    /**String of all calling arguments for the state routines built from ZBNF result statefnarg. */
    public StringBuilder callingArgs;
    
    
    
    private final Map<String, String> idxStateVariables = new HashMap<String, String>();
    
    final Map<String, String> variables =new TreeMap<String, String>();
    
    /**From Zbnf: creates an instance for the Zbnf component in the syntax context: <pre>
     * { static <*;?statefnarg>;  
     * }
     * </pre>
     * @param arg The parsed string to this semantic component.
     */
    public ZbnfNameValue new_variable(){ return new ZbnfNameValue(); }
    
    public void add_variable(ZbnfNameValue inp){ variables.put(inp.name, inp.value); }
    
    /**From Zbnf: stores the definition of a function argument in the syntax context: <pre>
     * { static <*;?statefnarg>;  
     * }
     * </pre>
     * @param arg The parsed string to this semantic component.
     */
    public void set_statefnarg(String arg){ 
      arg = arg.trim();
      int posIdentifier = arg.lastIndexOf(' ') +1;
      String ident = arg.substring(posIdentifier);
      statefnargs.add(arg); 
      if(formalArgs ==null){ 
        formalArgs = new StringBuilder(arg);
        callingArgs = new StringBuilder(ident);
      } else {
        formalArgs.append(", ").append(arg);
        callingArgs.append(", ").append(ident);
      }
    }
   
    /**From Zbnf: stores an include line in the syntax context: <pre>
     * { #include <* \n?includeLine>
     * }
     * </pre>
     * @param arg The parsed string to this semantic component.
     */
    public void set_includeLine(String arg){ includeLines.add(arg); }
    
  }
  
  
  public static class GenStateMachine extends StateMachine 
  {
  
    public final ZbnfResultData zbnfSrc;
    
    /**All states with an own state variable and timer, it is the top state, all parallel states
     * and composite states with a history.
     */
    public final List<StateComposite> rootStates = new LinkedList<StateComposite>();
    
    public final List<StateSimple> listStates = new LinkedList<StateSimple>();

    Map<String, StateSimple> allStates = new TreeMap<String, StateSimple>();

    
    GenStateMachine(ZbnfResultData zbnfSrc) 
    { super(new StateSimple[zbnfSrc.subStates.size()]);
      this.zbnfSrc = zbnfSrc;
    }
    
    StateCompositeTop stateTop(){ return stateTop; }
    
    
    void prepare() {
      stateTop.prepare();
    }
    
    
  }
  
  
  
  /**Instances of this class are added as {@link StateSimple#setAuxInfo(Object)} to get this information
   * from instances of StateSimple which code generation.
   */
  static class GenStateInfo
  {
    public final ZbnfState zsrcState;
    
    
    /**Set if this state has a time condition. */
    public String timeCondition;
    
    /**Set to true if any of the sub states has a timeout transition 
     * therefore this state as rootState should have a timer variable.
     */
    public boolean hasTimer;

    public GenStateInfo(ZbnfState zsrcState)
    { this.zsrcState = zsrcState;
    }
    
  }  
  
  
  
  static class GenStateSimple extends StateSimple
  {
    
    GenStateSimple(StateSimple enclState, StateComposite rootState, StateMachine stm, ZbnfState zbnfState){
      super();
      super.setAuxInfo(new GenStateInfo(zbnfState));
      this.enclState = enclState;
      this.compositeState = rootState;
      this.stateMachine = stm;
      stateId = zbnfState.stateName;
    }
    
    
  }
  
  
  
  static class GenStateCompositeFlat extends StateCompositeFlat {
    
    GenStateCompositeFlat(StateCompositeFlat enclState, StateComposite rootState, GenStateMachine genStm, ZbnfState zbnfComposite)
    { super(zbnfComposite.stateName, genStm, zbnfComposite.nrofSubstates == 0 ? null : new StateSimple[zbnfComposite.nrofSubstates] );
      super.setAuxInfo(new GenStateInfo(zbnfComposite));
      this.enclState = enclState;
      this.compositeState = rootState;
      stateId = zbnfComposite.stateName;
    }
    
    
  }
  
  
  static class GenStateComposite extends StateComposite {
    
    GenStateComposite(StateSimple enclState, StateComposite rootState, GenStateMachine genStm, ZbnfState zbnfComposite)
    { super(zbnfComposite.stateName, genStm, zbnfComposite.nrofSubstates == 0 ? null : new StateSimple[zbnfComposite.nrofSubstates] );
      super.setAuxInfo(new GenStateInfo(zbnfComposite));
      this.enclState = enclState;
      this.compositeState = rootState;
      stateId = zbnfComposite.stateName;
    }
    
  }
  
  
  static class GenStateParallel extends StateParallel{
    
    GenStateParallel(StateSimple enclState, StateComposite rootState, GenStateMachine genStm, ZbnfState zbnfComposite)
    { super(zbnfComposite.stateName, genStm, zbnfComposite.nrofSubstates == 0 ? null : new StateComposite[zbnfComposite.nrofSubstates]  );
      super.setAuxInfo(new GenStateInfo(zbnfComposite));
      this.enclState = enclState;
      this.compositeState = rootState;
      stateId = zbnfComposite.stateName;
    }
    
    
  }
  
  
  
  
  
  
  
  static class GenStateTrans extends StateSimple.Trans
  {
    
    public final ZbnfTrans zsrcTrans;
    
    GenStateTrans(ZbnfTrans zsrcTrans, StateSimple state, int[] dstKeys){
      state.super("Trans_" + state.getName() + zsrcTrans.nrTrans, dstKeys);
      this.zsrcTrans = zsrcTrans;
    }
  }
  
  
  
  GenStateMachine genStm;
  
  
  

  public StateMGen(MainCmd_ifc console)
  {
    this.console = console;
  }
  
  
  void execute(Zbnf2Text.Args args) throws IOException, IllegalAccessException
  {
    ZbnfResultData zsrcData = parseAndStoreInput(args);  //parsed and converts into Java data presentation
    if(zsrcData != null){
      if(args.sFileSrcData!=null) {
        Writer out  = new FileWriter(args.sFileSrcData);
        if(args.sFileSrcData.endsWith(".html") || args.sFileSrcData.endsWith(".htm")){
          DataShow.outHtml(zsrcData, out);
        }
        else if(args.sFileSrcData.endsWith(".xml")){
          DataShow.dataTreeXml(zsrcData, out, 20);
        }
        else {
          DataShow.dataTree(zsrcData, out, 20);
        }
        out.close();
      }
      
      prepareStateData(zsrcData);
      if(args.sFileSrcData!=null) {
        Writer out;
        if(args.sFileSrcData.endsWith(".html")){
          String sFileDstData = args.sFileSrcData.substring(0, args.sFileSrcData.length()-5) + ".dst.html";
          out = new FileWriter(sFileDstData);
          DataShow.outHtml(genStm, out);
        }
        else if(args.sFileSrcData.endsWith(".htm")){
          String sFileDstData = args.sFileSrcData.substring(0, args.sFileSrcData.length()-4) + ".dst.htm";
          out = new FileWriter(sFileDstData);
          DataShow.dataTreeXml(genStm, out, 20);
        }
        else if(args.sFileSrcData.endsWith(".xml")){
          String sFileDstData = args.sFileSrcData.substring(0, args.sFileSrcData.length()-4) + ".dst.xml";
          out = new FileWriter(sFileDstData);
          DataShow.dataTreeXml(genStm, out, 20);
        }
        else {
          String sFileDstData = args.sFileSrcData + ".dst";
          out = new FileWriter(sFileDstData);
          DataShow.dataTree(genStm, out, 20);
        }
        out.close();
      }
      FileWriter outData;
      if(args.sScriptCheck !=null){
        outData = new FileWriter(args.sScriptCheck);
      } else {
        outData = null;
      }
      for(Zbnf2Text.Out outArgs: args.listOut){
        File fOut = new File(outArgs.sFileOut);
        File fileScript = new File(outArgs.sFileScript);
        File fScriptCheck = args.sScriptCheck == null ? null : new File(args.sScriptCheck);
        if(outData !=null) {
          outData.append("===================").append(outArgs.sFileScript);
        }
        Writer out = new FileWriter(fOut);
        JZcmdExecuter generator = new JZcmdExecuter(console);
        generator.setScriptVariable("sOutfile", 'S', fOut.getAbsolutePath(), true);
        generator.setScriptVariable("stm", 'O', genStm, true);
        try{ 
          JZcmd.execute(generator, fileScript, out, console.currdir(), true, fScriptCheck, console);
          console.writeInfoln("SUCCESS outfile: " + fOut.getAbsolutePath());
        } catch(ScriptException exc){
          console.writeError(exc.getMessage());
        }
        out.close();
        
      }
      if(outData !=null) {
        outData.close();
      }

    } else {
      console.writeInfoln("ERROR");
      
    }
  }
  
  
  /**Prepares the parsed data to some dependencies of states etc. This routine is called after parse and fill to provide usability data. 
   * @param zbnfSrc The main instance for fill after parse, the main instance for generation.
   */
  void prepareStateData(ZbnfResultData zbnfSrc){
    //creates the instance for all prepared data:
    genStm = new GenStateMachine(zbnfSrc);
    StateComposite stateTop = genStm.stateTop();
    stateTop.setAuxInfo(new GenStateInfo(null)); //instance for Code generation for the top state.  
    //gather all states and transitions in the parsed data and add it to the prepared data:
    genStm.rootStates.add(stateTop); //the stateTop is the first rootState.
    gatherStatesOfComposite(stateTop, stateTop, zbnfSrc);
    //
    gatherAllTransitions();
    //invoke prepare, the same as for Java state machines.
    genStm.prepare();
    
  }
  
  
  
  
  /**Gathers a Composite state from its Zbnf parsers result.
   * <ul>
   * <li>It stores all that states as root states, which are composites with history or which are parallel states. 
   *   For that states an own state variable is necessary. Composite states which are not root states 
   *   are not threaded extra. All sub states of them gets the transition of the composite only.
   * </ul>   
   * @param stateComposite
   * @param zbnfComposite
   * @return
   */
  StateCompositeFlat gatherStatesOfComposite(StateCompositeFlat stateComposite, StateComposite rootState, ZbnfStateCompositeBase zbnfComposite)
  { 
    GenStateInfo genStateinfo = (GenStateInfo)rootState.auxInfo(); //set on construction from the derived instance of StateSimple
    assert (genStateinfo !=null);
    /*
    if(genStateinfo.subStates == null) { 
      genStateinfo.subStates = new LinkedList<StateSimple>();
    }
    */
    for(ZbnfState zbnfState: zbnfComposite.subStates){
      if(!zbnfState.isPrepared){
        StateSimple state1;  //either a GenStateSimple or a GenStateComposite, add it after creation and evaluation.
        //
        if(zbnfState.subStates !=null && zbnfState.subStates.size() >0) {
          final StateSimple stateComposite1;
          if(zbnfState.stateParallel) {
            stateComposite1 = new GenStateParallel(stateComposite, rootState, genStm, zbnfState);
            state1 = gatherStatesOfParallel((StateParallel)stateComposite1, rootState, zbnfState);
          } else {
            StateComposite rootState1;
            if(zbnfState.hasHistory) { 
              stateComposite1 = new GenStateComposite(stateComposite, rootState, genStm, zbnfState);
              rootState1 = (StateComposite)stateComposite1;
              genStm.rootStates.add(rootState1);
            } else { 
              stateComposite1 = new GenStateCompositeFlat(stateComposite, rootState, genStm, zbnfState);
              rootState1 = rootState; 
            }  
            state1 = gatherStatesOfComposite((StateCompositeFlat)stateComposite1, rootState1, zbnfState);
          }
          
        } else {
          state1 = new GenStateSimple(stateComposite, rootState, genStm, zbnfState);
        }
        //
        stateComposite.addState(state1.hashCode(), state1);
        //genStateinfo.subStates.add(state1);
        genStm.allStates.put(state1.getName(), state1);
        genStm.listStates.add(state1);
        //prepareStateStructure(state, stateData, false, 0);
      }
    }
    
    
    return stateComposite;
  }
  
  
  
  
  /**Gathers a Composite state from its Zbnf parsers result.
   * @param stateComposite
   * @param zbnfComposite
   * @return
   */
  StateParallel gatherStatesOfParallel(StateParallel stateParallel, StateComposite rootState, ZbnfStateCompositeBase zbnfParallel)
  { 
    GenStateInfo genStateinfo = (GenStateInfo)rootState.auxInfo(); //set on construction from the derived instance of StateSimple
    assert (genStateinfo !=null);
    /*
    if(genStateinfo.subStates == null) { 
      genStateinfo.subStates = new LinkedList<StateSimple>();
    }
    */
    for(ZbnfState zbnfState: zbnfParallel.subStates){
      if(!zbnfState.isPrepared){
        StateSimple state1;  //either a GenStateSimple or a GenStateComposite, add it after creation and evaluation.
        //
        if(zbnfState.subStates !=null && zbnfState.subStates.size() >0) {
          final StateSimple stateComposite1;
          if(zbnfState.stateParallel) {
            throw new IllegalArgumentException("the next level of StateParallel cannot be a StateParallel");
          } else {
            StateComposite noRootState = null;
            stateComposite1 = new GenStateComposite(stateParallel, noRootState, genStm, zbnfState);
            StateComposite rootState1 = (StateComposite)stateComposite1;
            genStm.rootStates.add(rootState1);
            state1 = gatherStatesOfComposite((StateComposite)stateComposite1, rootState1, zbnfState);
          }
          
        } else {
          state1 = new GenStateSimple(stateParallel, rootState, genStm, zbnfState);
        }
        //
        stateParallel.addState(state1.hashCode(), state1);
        //genStateinfo.subStates.add(state1);
        genStm.allStates.put(state1.getName(), state1);
        genStm.listStates.add(state1);
        //prepareStateStructure(state, stateData, false, 0);
      }
    }
    
    
    return stateParallel;
  }
  
  
  
  /**
   * Note: timeout transitions don't use the {@link StateSimple#transTimeout} facility. A timeout is generated by a condition
   * in another special way. If the {@link ZbnfTrans#time} is set, the {@link GenStateInfo#timeCondition} will be set and the 
   * {@link GenStateInfo#hasTimer} of the 
   */
  void gatherAllTransitions() {
    for(StateSimple genState : genStm.listStates) {
      StateSimple.PlugStateSimpleToGenState plugState = genState.new PlugStateSimpleToGenState();
      GenStateInfo stateInfo = (GenStateInfo)genState.auxInfo();
      ZbnfState zbnfState = stateInfo.zsrcState;
      if(zbnfState.trans !=null && zbnfState.trans.size() >0) {
        int zTransitions = zbnfState.trans.size();
        if(zTransitions >0){
          plugState.createTransitions(zTransitions);
          for(ZbnfTrans zbnfTrans: zbnfState.trans){
            int nrofForks = zbnfTrans.dstStates !=null ? 1 + zbnfTrans.dstStates.size() : 1; 
            int[] dstKeys = new int[nrofForks];
            int ixDst = -1;
            if(zbnfTrans.dstStates !=null) {
              for(String sDstState : zbnfTrans.dstStates) {
                StateSimple dstState1 = genStm.allStates.get(sDstState);
                if(dstState1 == null) throw new IllegalArgumentException("faulty dst state in transition;" + sDstState + "; from state " + genState.getName());
                dstKeys[++ixDst] = dstState1.hashCode();
              } }
            StateSimple dstState1 = genStm.allStates.get(zbnfTrans.dstState);
            if(dstState1 == null) throw new IllegalArgumentException("faulty dst state in transition;" + zbnfTrans.dstState + "; from state " + genState.getName());
            dstKeys[++ixDst] = dstState1.hashCode();
            StateSimple.Trans trans;
            if(zbnfTrans.joinStates !=null){
              trans = genState.new TransJoin("Trans_" + genState.getName() + zbnfTrans.nrTrans, dstKeys); 
              int[] joinKeys = new int[zbnfTrans.joinStates.size()];
              int ixJoin = -1;
              for(String sJoinState: zbnfTrans.joinStates){
                StateSimple joinState1 = genStm.allStates.get(sJoinState);
                if(joinState1 == null) throw new IllegalArgumentException("faulty join state in transition;" + sJoinState + "; from state " + genState.getName());
                joinKeys[++ixJoin] = joinState1.hashCode();
              }
              ((StateSimple.TransJoin)trans).srcStates(joinKeys);  //set the hashes of the join sources.
            } else {
              trans = new GenStateTrans(zbnfTrans, genState, dstKeys);
            }
            if(zbnfTrans.time !=null) { //condition is a time condition.
              stateInfo.timeCondition = zbnfTrans.time;
              //genState.transTimeout = trans;
              GenStateInfo rootStateInfo = (GenStateInfo)genState.compositeState().auxInfo();
              rootStateInfo.hasTimer = true;
            } else {
            }
            plugState.addTransition(trans);
            
          }
        }
      }
    }
  }


  
  
  
  
  
  /**This method reads the input script, parses it with ZBNF, 
   * stores all results in the Java-class {@link ZbnfResultData} 
   */
  private ZbnfResultData parseAndStoreInput(Zbnf2Text.Args args)
  {
    /**The instance to store the data of parsing result is created locally and private visible: */
    ZbnfResultData zbnfResultData = new ZbnfResultData();
    /**This call processes the whole parsing and storing action: */
    File fileIn = new File(args.sFileIn);
    File fileSyntax = new File(args.sFileSyntax);
    String sError = ZbnfJavaOutput.parseFileAndFillJavaObject(zbnfResultData.getClass(), zbnfResultData, fileIn, fileSyntax, console, 1200);
    if(sError != null)
    { /**there is any problem while parsing, report it: */
      console.writeError("ERROR Parsing file: " + fileIn.getAbsolutePath() + "\n" + sError);
      return null;
    }
    else
    { console.writeInfoln("SUCCESS parsed: " + fileIn.getAbsolutePath());
      return zbnfResultData;
    }
  
  }
  
  

  void stop(){}

}
