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
   * <li>2014-08-10 Hartmut chg: disperse ZBNF result and prepared state data
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


  /**This class holds the got arguments from command line. 
   * It is possible that some more operations with arguments as a simple get process.
   * The arguments may got also from configuration files named in cmd line arguments or other.
   * The user of arguments should know (reference) only this instance, not all other things arround MainCmd,
   * it it access to arguments. 
   */
  public static class Args extends Zbnf2Text.Args
  {
    /**Cmdline-argument, set on -i: option. Inputfile to to something. */
    String sFileIn = null;
  
    /**Cmdline-argument, set on -s: option. Zbnf-file to output something. */
    String sFileZbnf = null;

    String sFileData = null;
    
    String sScriptCheck = null;
    
    
    public void setInput(String val){ sFileIn = val; }
    
  }



  /*---------------------------------------------------------------------------------------------*/
  /** main started from java*/
  public static void main(String [] args)
  { StateMGen.Args cmdlineArgs = new StateMGen.Args();     //holds the command line arguments.
    CmdLine mainCmdLine = new CmdLine(args, cmdlineArgs); //the instance to parse arguments and others.
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
      StateMGen main = new StateMGen(mainCmdLine, cmdlineArgs);
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

  
  
  /**The inner class CmdLine helps to evaluate the command line arguments
   * and show help messages on command line.
   * This class also implements the {@link MainCmd_ifc}, for some cmd-line-respective things
   * like error level, output of texts, exit handling.  
   */
  static class CmdLine extends MainCmd
  {
  
  
    final Args cmdlineArgs;
    
    /*---------------------------------------------------------------------------------------------*/
    /** Constructor of the main class.
        The command line arguments are parsed here. After them the execute class is created as composition of SampleCmdLine.
    */
    private CmdLine(String[] argsInput, Args cmdlineArgs)
    { super(argsInput);
      this.cmdlineArgs = cmdlineArgs;
      //:TODO: user, add your help info!
      //super.addHelpInfo(getAboutInfo());
      super.addAboutInfo("Conversion Statemachine-script to any output");
      super.addAboutInfo("made by Hartmut Schorrig, 2012-10-06");
      super.addHelpInfo("Conversion Statemachine-script to any output V 2012-10-06");
      super.addHelpInfo("param: -i:INPUT -s:ZBNF -c:OUTCTRL -y:OUTPUT");
      super.addHelpInfo("-i:INPUT    inputfilepath, this file is testing.");
      super.addHelpInfo("-s:ZBNF     syntaxfilepath, this file is read.");
      super.addHelpInfo("-c:ZBNF     output script, this file is read.");
      super.addHelpInfo("-y:OUTPUT   outputfilepath, this file is written.");
      super.addStandardHelpInfo();
    }
  
  
  
  
  
  
    /*---------------------------------------------------------------------------------------------*/
    /** Tests one argument. This method is invoked from parseArgument. It is abstract in the superclass MainCmd
        and must be overwritten from the user.
        :TODO: user, test and evaluate the content of the argument string
        or test the number of the argument and evaluate the content in dependence of the number.
  
        @param argc String of the actual parsed argument from cmd line
        @param nArg number of the argument in order of the command line, the first argument is number 1.
        @return true is okay,
                false if the argument doesn't match. The parseArgument method in MainCmd throws an exception,
                the application should be aborted.
    */
    @Override
    protected boolean testArgument(String arg, int nArg)
    { boolean bOk = true;  //set to false if the argc is not passed
  
      if(arg.startsWith("-i:"))      cmdlineArgs.sFileIn   = getArgument(3);
      else if(arg.startsWith("-s:")) cmdlineArgs.sFileZbnf  = getArgument(3);
      else if(arg.startsWith("-d:")) cmdlineArgs.sFileData  = getArgument(3);
      else if(arg.startsWith("-scriptcheck:")) cmdlineArgs.sScriptCheck  = getArgument(13);
      else if(arg.startsWith("-y:")) {
        if(cmdlineArgs.lastOut == null){ cmdlineArgs.lastOut = new Zbnf2Text.Out(); cmdlineArgs.listOut.add(cmdlineArgs.lastOut); }
        cmdlineArgs.lastOut.sFileOut  = getArgument(3);
        if(cmdlineArgs.lastOut.sFileScript !=null){ cmdlineArgs.lastOut = null; } //filled. 
      }
      else if(arg.startsWith("-c:")) {
        if(cmdlineArgs.lastOut == null){ cmdlineArgs.lastOut = new Zbnf2Text.Out(); cmdlineArgs.listOut.add(cmdlineArgs.lastOut); }
        cmdlineArgs.lastOut.sFileScript  = getArgument(3);
        if(cmdlineArgs.lastOut.sFileOut !=null){ cmdlineArgs.lastOut = null; } //filled. 
      }
      else if(arg.startsWith("-t:")) {
        if(cmdlineArgs.lastOut == null){ cmdlineArgs.lastOut = new Out(); cmdlineArgs.listOut.add(cmdlineArgs.lastOut); }
        cmdlineArgs.lastOut.sFileOut  = getArgument(3);
        if(cmdlineArgs.lastOut.sFileScript !=null){ cmdlineArgs.lastOut = null; } //filled. 
      }
      else if(arg.startsWith("-c:")) {
        if(cmdlineArgs.lastOut == null){ cmdlineArgs.lastOut = new Out(); cmdlineArgs.listOut.add(cmdlineArgs.lastOut); }
        cmdlineArgs.lastOut.sFileScript  = getArgument(3);
        if(cmdlineArgs.lastOut.sFileOut !=null){ cmdlineArgs.lastOut = null; } //filled. 
      }
  
      return bOk;
    }
  
    /** Invoked from parseArguments if no argument is given. In the default implementation a help info is written
     * and the application is terminated. The user should overwrite this method if the call without comand line arguments
     * is meaningfull.
     * @throws ParseException 
     *
     */
    @Override
    protected void callWithoutArguments() throws ParseException
    { //:TODO: overwrite with empty method - if the calling without arguments
      //having equal rights than the calling with arguments - no special action.
      super.callWithoutArguments();  //it needn't be overwritten if it is unnecessary
    }
  
  
  
  
    /*---------------------------------------------------------------------------------------------*/
    /**Checks the cmdline arguments relation together.
       If there is an inconsistents, a message should be written. It may be also a warning.
       :TODO: the user only should determine the specific checks, this is a sample.
       @return true if successfull, false if failed.
    */
    @Override
    protected boolean checkArguments()
    { boolean bOk = true;
  
      if(cmdlineArgs.sFileIn == null)            { bOk = false; writeError("ERROR argument -i is obligat."); }
      else if(cmdlineArgs.sFileIn.length()==0)   { bOk = false; writeError("ERROR argument -i without content.");}
  
      if(!bOk) setExitErrorLevel(exitWithArgumentError);
    
      return bOk;
    
    }
  }//class CmdLine
  
  
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
  

  
  
  
  public static class ZbnfStateCompositeBase
  {
    List<ZbnfState> subStates;
    
    int nrofSubstates, nrofParallelStates;
    
    final Map<String, ZbnfState> idxSrcStates = new TreeMap<String, ZbnfState>();
    
    final Map<String, ZbnfSimpleState> idxStates = new TreeMap<String, ZbnfSimpleState>();
    
    
    public ZbnfState new_state()
    { return new ZbnfState();
    }
    
    public void add_state(ZbnfState value)
    { 
      //idxSrcStates.put(value.stateName, value);
      if(value.parallelParentState !=null){
        if(idxStates.get(value.parallelParentState) ==null){
          ZbnfCompositeState parallel = new ZbnfCompositeState(new ZbnfState());
          parallel.src.stateName = value.parallelParentState;
          //parallel.src.parallelParent = value.enclState;
          parallel.src.shortdescription = "Parallel state machine inside " + value.parallelParentState;
          idxStates.put(value.parallelParentState, parallel);
          //srcStates.add(parallel);  //add first the parallel self created state.
        }
        nrofParallelStates +=1;
      }
      else {
        nrofSubstates +=1;
      }
      if(subStates == null) { subStates = new ArrayList<ZbnfState>(); }
      subStates.add(value);
      idxSrcStates.put(value.stateName, value);
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
    
    
    /**From ZBNF: the default state of a complex state. */
    //public String startState;
    

    /**From ZBNF: <$?@enclState>. It is the name of the state where this state is member of. */
    //public String enclState;
    
    /**Name of the state which is the container of the parallel state machine. Only set on enclosing state
     * of a parallel state machine. */
    //String parallelParent;
    
    /**From ZBNF: <$?@enclState>.<$?@parallelParentState>. It is the name of the state where this state is member of
     * in a parallel machine of the enclosing state. */
    public String parallelParentState;
    
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
  

  
  
  /**The prepared State.
   * 
   */
  public static class ZbnfSimpleState
  { ZbnfState src;

    boolean isComposite;
  
    /**Name of the variable where the state ident is stored in the users code. 
     * It is "state"<&the name of the parent state>, or the parent of parent if no history is used,  or it is "state" for the first level. */
    public String stateVariableName;
    
    ZbnfSimpleState(ZbnfState src){ this.src = src; }
  }  
  
  
  
  /**The prepared State.
   * 
   */
  public static class ZbnfCompositeState extends ZbnfSimpleState
  { 
    /**All states which are direct sub-states in this state.  */
    public Map<String, ZbnfSimpleState> subStates;

    /**All states which uses the state variable of this state. */
    public Map<String, ZbnfSimpleState> subStatesVariable;

    /**All states which are parallel states in this states. null if the state has not parallel sub-states. */
    public Map<String, ZbnfCompositeState> parallelStates;

    public ZbnfConstDef constDef;
    
    ZbnfCompositeState(ZbnfState src){ super(src); isComposite = true; }
    
    /**converting from Simple to Composite. */
    ZbnfCompositeState(ZbnfSimpleState src){ 
      super(src.src); 
      isComposite = true;
      stateVariableName = src.stateVariableName;
    }
    
    @Override public String toString(){ return src.stateName; }
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
  
  
  /**This class gets and stores the results from a parsed ZBNF-component <code>constant</code>.
   * 
   */
  public static class ZbnfTrans
  {
    /**From ZBNF: \? <*|\?\.?description>. */
    public String description;
    
    /**From ZBNF: + <*|\?\.?additionaldescription>. */
    public String additionaldescription;
    
    /**From ZBNF: ! <*|\?\.?description>. */
    public String tododescription;
    
    public String cond;
    
    public String time;
    
    private List<String> joinStatesSrc;
    
    public List<ZbnfSimpleState> joinStates;
    
    public String event;
    
    public String code;
    
    private final List<ZbnfTrans> subCondition = new LinkedList<ZbnfTrans>();

    
    private final List<ZbnfSimpleState> exitStates = new LinkedList<ZbnfSimpleState>();
    
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
    
    /**The state from which this entry starts. */
    ZbnfSimpleState srcState;
    
    ZbnfSimpleState entryState;
    
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
    
    private final Map<String, ZbnfSimpleState> topStates = new TreeMap<String, ZbnfSimpleState>();
    
    final List<ZbnfSimpleState> states = new ArrayList<ZbnfSimpleState>();
    
    final Map<String, String> variables =new TreeMap<String, String>();
    
    public ZbnfNameValue new_variable(){ return new ZbnfNameValue(); }
    
    public void add_variable(ZbnfNameValue inp){ variables.put(inp.name, inp.value); }
    
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
   
    public void set_includeLine(String arg){ includeLines.add(arg); }
    
  }
  
  
  public static class GenStateMachine extends StateMachine 
  {
  
    public final ZbnfResultData zsrcFile;
    
    public final List<StateComposite> rootStates = new LinkedList<StateComposite>();
    
    public final List<StateSimple> listStates = new LinkedList<StateSimple>();

    Map<String, StateSimple> allStates = new TreeMap<String, StateSimple>();

    
    GenStateMachine(ZbnfResultData zsrcFile, StateSimple[] aFirstStates) 
    { super(aFirstStates);
      this.zsrcFile = zsrcFile;
    }
    
    StateCompositeTop stateTop(){ return topState; }
    
    
    void prepare() {
      topState.prepare();

    }
    
    
  }
  
  
  
  static class GenStateInfo
  {
    public final ZbnfState zsrcState;
    
    /**The state which contains the reference to this state if this state is the active one.
     * If there are no parallel or history states that is the topState of the statemachine.
     * If there is a parallel state, it is the composite parallel state.
     * If there is a history in a composite state, that is the root state.
     */
    public final StateComposite rootState;
    
    /**List of all sub states with the same rootState, it means with the same state-switch-variable.
     * It is possible that the inner states of a composite states is member of this list too.
     * That is if the composite state has not a history entry and it is not a StateParallel.
     * This list remain null if it is a simple state.
     */
    public List<StateSimple> subStates;
    
    /**Set if this state has a time condition. */
    public String timeCondition;
    
    /**Set to true if any of the sub states has a timeout transition 
     * therefore this state as rootstate should have a timer variable.
     */
    public boolean hasTimer;

    public GenStateInfo(ZbnfState zsrcState, StateComposite rootState)
    { this.zsrcState = zsrcState;
      this.rootState = rootState;
    }
    
  }  
  
  
  
  static class GenStateSimple extends StateSimple
  {
    
    GenStateSimple(StateSimple enclState, StateComposite rootState, StateMachine stm, ZbnfState zbnfState){
      super();
      super.setAuxInfo(new GenStateInfo(zbnfState, rootState));
      this.enclState = enclState;
      this.stateMachine = stm;
      stateId = zbnfState.stateName;
    }
    
    
    /**Exports the protected method for this package, respectively this class. */
    void genStatePrepareTransitions(){ super.prepareTransitions(); }
    
  }
  
  
  
  static class GenStateCompositeFlat extends StateCompositeFlat {
    
    GenStateCompositeFlat(StateSimple enclState, StateComposite rootState, GenStateMachine genStm, ZbnfState zbnfComposite)
    { super(zbnfComposite.stateName, genStm, zbnfComposite.nrofSubstates == 0 ? null : new StateSimple[zbnfComposite.nrofSubstates] );
      super.setAuxInfo(new GenStateInfo(zbnfComposite, rootState));
      this.enclState = enclState;
      stateId = zbnfComposite.stateName;
    }
    
    /**Exports the protected method for this package, respectively this class. */
    void genStatePrepareTransitions(){ super.prepareTransitions(); }
    
  }
  
  
  static class GenStateComposite extends StateComposite {
    
    GenStateComposite(StateSimple enclState, StateComposite rootState, GenStateMachine genStm, ZbnfState zbnfComposite)
    { super(zbnfComposite.stateName, genStm, zbnfComposite.nrofSubstates == 0 ? null : new StateSimple[zbnfComposite.nrofSubstates] );
      super.setAuxInfo(new GenStateInfo(zbnfComposite, rootState));
      this.enclState = enclState;
      stateId = zbnfComposite.stateName;
    }
    
    /**Exports the protected method for this package, respectively this class. */
    void genStatePrepareTransitions(){ super.prepareTransitions(); }
    
  }
  
  
  static class GenStateParallel extends StateParallel{
    
    GenStateParallel(StateCompositeFlat enclState, StateComposite rootState, GenStateMachine genStm, ZbnfState zbnfComposite)
    { super(zbnfComposite.stateName, genStm, zbnfComposite.nrofSubstates == 0 ? null : new StateComposite[zbnfComposite.nrofSubstates]  );
      super.setAuxInfo(new GenStateInfo(zbnfComposite, rootState));
      this.enclState = enclState;
      stateId = zbnfComposite.stateName;
    }
    
    /**Exports the protected method for this package, respectively this class. */
    void genStatePrepareTransitions(){ super.prepareTransitions(); }
    
  }
  
  
  
  
  
  
  
  static class GenStateTrans extends StateSimple.Trans
  {
    
    public final ZbnfTrans zsrcTrans;
    
    GenStateTrans(ZbnfTrans zsrcTrans, StateSimple state, int[] dstKeys){
      state.super(dstKeys);
      this.zsrcTrans = zsrcTrans;
    }
  }
  
  
  
  GenStateMachine genStm;
  
  
  

  public StateMGen(MainCmd_ifc console, Args args)
  {
    this.console = console;
  }
  
  
  void execute(Args args) throws IOException, IllegalAccessException
  {
    ZbnfResultData zsrcFile = parseAndStoreInput(args);
    if(zsrcFile != null){
      GenStateMachine genStm = prepareStateData(zsrcFile);
      if(args.sFileData!=null) {
        Writer out = new FileWriter(args.sFileData);
        DataShow.dataTree(genStm, out, 20);
        out.close();
        out = new FileWriter(args.sFileData + ".xml");
        DataShow.dataTreeXml(genStm, out, 20);
        out.close();
        out = new FileWriter(args.sFileData + ".html");
        DataShow.outHtml(genStm, out);
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
        JZcmdExecuter generator = new JZcmdExecuter(console);
        if(outData !=null) {
          outData.append("===================").append(outArgs.sFileScript);
        }
        Writer out = new FileWriter(fOut);
        generator.setScriptVariable("sOutfile", 'S', fOut.getAbsolutePath(), true);
        generator.setScriptVariable("stm", 'O', genStm, true);
        try{ 
          JZcmd.execute(generator, fileScript, out, console.currdir(), true, args.sScriptCheck == null ? null : new File(args.sScriptCheck), console);
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
  GenStateMachine prepareStateData(ZbnfResultData zbnfSrc){
    StateSimple[] aStates = new StateSimple[zbnfSrc.subStates.size()];
    genStm = new GenStateMachine(zbnfSrc, aStates);
    StateComposite stateTop = genStm.stateTop();
    genStm.rootStates.add(stateTop);
    stateTop.setAuxInfo(new GenStateInfo(null, null));
    gatherStatesOfComposite(stateTop, stateTop, zbnfSrc);
    gatherAllTransitions();
    
    genStm.prepare();
    
    prepareAllTransitions();
    return genStm;
    
  }
  
  
  
  
  /**Gathers a Composite state from its Zbnf parsers result.
   * @param stateComposite
   * @param zbnfComposite
   * @return
   */
  StateCompositeFlat gatherStatesOfComposite(StateCompositeFlat stateComposite, StateComposite rootState, ZbnfStateCompositeBase zbnfComposite)
  { 
    GenStateInfo genStateinfo = (GenStateInfo)rootState.auxInfo(); //set on construction from the derived instance of StateSimple
    assert (genStateinfo !=null);
    if(genStateinfo.subStates == null) { 
      genStateinfo.subStates = new LinkedList<StateSimple>();
    }
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
        genStateinfo.subStates.add(state1);
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
    if(genStateinfo.subStates == null) { 
      genStateinfo.subStates = new LinkedList<StateSimple>();
    }
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
        genStateinfo.subStates.add(state1);
        genStm.allStates.put(state1.getName(), state1);
        genStm.listStates.add(state1);
        //prepareStateStructure(state, stateData, false, 0);
      }
    }
    
    
    return stateParallel;
  }
  
  
  
  void gatherAllTransitions() {
    for(StateSimple genState : genStm.listStates) {
    //for(Map.Entry<String, StateSimple> entry : genStm.allStates.entrySet()) {
      //StateSimple genState = entry.getValue();
      GenStateInfo stateInfo = (GenStateInfo)genState.auxInfo();
      ZbnfState zbnfState = stateInfo.zsrcState;
      if(zbnfState.trans !=null && zbnfState.trans.size() >0) {
        int zTransitions = zbnfState.trans.size();
        if(zTransitions >0){
          genState.createTransitions(zTransitions);
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
            GenStateTrans trans = new GenStateTrans(zbnfTrans, genState, dstKeys);
            genState.addTransition(trans);
            if(zbnfTrans.time !=null) { //condition is a time condition.
              stateInfo.timeCondition = zbnfTrans.time;
              GenStateInfo rootStateInfo = (GenStateInfo)stateInfo.rootState.auxInfo();
              rootStateInfo.hasTimer = true;
            }
          }
        }
      }
    }
  }


  
  
  void prepareAllTransitions() {
    for(StateSimple genState : genStm.listStates) {
      //for(Map.Entry<String, StateSimple> entry : genStm.allStates.entrySet()) {
      //StateSimple genState = entry.getValue();
      if(genState instanceof GenStateSimple) {
        ((GenStateSimple)genState).genStatePrepareTransitions();
      } else if(genState instanceof GenStateComposite) {
        ((GenStateComposite)genState).genStatePrepareTransitions();  //same method like GenStateSimple
      }
    }
  }
  
  
  
  
  /**This method reads the input script, parses it with ZBNF, 
   * stores all results in the Java-class {@link ZbnfResultData} 
   */
  private ZbnfResultData parseAndStoreInput(Args args)
  {
    /**The instance to store the data of parsing result is created locally and private visible: */
    ZbnfResultData zbnfResultData = new ZbnfResultData();
    /**This call processes the whole parsing and storing action: */
    File fileIn = new File(args.sFileIn);
    File fileSyntax = new File(args.sFileZbnf);
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
