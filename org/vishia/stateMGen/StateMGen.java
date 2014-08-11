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
  public static class ConstDef
  {  public String description;
  
     public List<String> ident;
     
     public String code;
  }
  

  
  
  /**This class gets and stores the results from a parsed ZBNF-component <code>State</code>.
   * 
   */
  public static class ZbnfState
  {
    /**From ZBNF: <$?@stateName>. */
    public String stateName;
    
    public String stateNr;
    
    /**From ZBNF: <$?@enclState>. It is the name of the state where this state is member of. */
    public String enclState;
    
    /**Name of the state which is the container of the parallel state machine. Only set on enclosing state
     * of a parallel state machine. */
    String parallelParent;
    
    /**From ZBNF: <$?@enclState>.<$?@parallelParentState>. It is the name of the state where this state is member of
     * in a parallel machine of the enclosing state. */
    public String parallelParentState;
    
    /**From ZBNF: \? <*|\?\.?description>. */
    public String description;
    
    /**From ZBNF: \? <*|\?\.?shortdescription>. */
    public String shortdescription;
    
    /**From ZBNF: + <*|\?\.?additionaldescription>. */
    public String additionaldescription;
    
    /**From ZBNF: ! <*|\?\.?description>. */
    public String tododescription;
    
    public boolean hasHistory;
    
    public Entry entry;
    
    public Exit exit;
    
    public Entry instate;
    
    public List<Trans> trans;
    
    public Entry new_instate() { return new Entry(); }

    public void set_instate(Entry val) { instate = val; }
    
    public void set_hasHistory(){ hasHistory = true; }
    
    /**Set to true if the state was prepared already. */
    boolean isPrepared = false;
    
    @Override public String toString(){ return stateName; }
  
  }
  

  
  
  /**The prepared State.
   * 
   */
  public static class SimpleState
  { ZbnfState src;

    boolean isComposite;
  
    /**Name of the variable where the state ident is stored in the users code. 
     * It is "state"<&the name of the parent state>, or the parent of parent if no history is used,  or it is "state" for the first level. */
    public String stateVariableName;
    
    SimpleState(ZbnfState src){ this.src = src; }
  }  
  
  
  
  /**The prepared State.
   * 
   */
  public static class CompositeState extends SimpleState
  { 
    /**From ZBNF: the default state of a complex state. */
    public String startState;
    
    /**All states which are direct sub-states in this state.  */
    public Map<String, SimpleState> subStates;

    /**All states which uses the state variable of this state. */
    public Map<String, SimpleState> subStatesVariable;

    /**All states which are parallel states in this states. null if the state has not parallel sub-states. */
    public Map<String, CompositeState> parallelStates;

    public ConstDef constDef;
    
    CompositeState(ZbnfState src){ super(src); isComposite = true; }
    
    /**converting from Simple to Composite. */
    CompositeState(SimpleState src){ 
      super(src.src); 
      isComposite = true;
      stateVariableName = src.stateVariableName;
    }
    
    @Override public String toString(){ return src.stateName; }
  }
  
  
  /**This class gets and stores the results from a parsed ZBNF-component <code>constant</code>.
   * 
   */
  public static class Entry
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
  public static class Exit
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
  public static class Trans
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
    
    public List<SimpleState> joinStates;
    
    public String event;
    
    public String code;
    
    private final List<Trans> subCondition = new LinkedList<Trans>();

    
    private final List<SimpleState> exitStates = new LinkedList<SimpleState>();
    
    private DstState dstStateTree; 

    public List<String> dstState;
    
    public Trans new_subCondition(){ return new Trans(); }
    
    public void add_subCondition(Trans value){ subCondition.add(value); }
    
    /**From Zbnf parser: <dstState> dstState::=<$?name>. */
    public void add_dstState(String val){ 
      if(dstState == null){
        dstState = new LinkedList<String>();
      }
      dstState.add(val);
      //return dstState; 
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
  public static class DstState
  {
    /**From Zbnf parser: dstState::=<$?name>. */
    public String name;
    
    /**The state from which this entry starts. */
    SimpleState srcState;
    
    SimpleState entryState;
    
    private List<DstState> entrySubStates;
    
    @Override public String toString(){ return name; }
    
  }
  
  
  public static class NameValue{ 
    public String name;
    public String value;
  }
  
  public static class ZbnfResultData
  {

    public XXXStateStructure stateStructure;

    public List<String> includeLines = new LinkedList<String>();
    
    public List<String> statefnargs = new LinkedList<String>();
    
    final List<ZbnfState> srcStates = new ArrayList<ZbnfState>();
    
    private final Map<String, ZbnfState> idxSrcStates = new TreeMap<String, ZbnfState>();
    
    private final Map<String, SimpleState> idxStates = new TreeMap<String, SimpleState>();
    
    private final Map<String, String> idxStateVariables = new HashMap<String, String>();
    
    private final Map<String, SimpleState> topStates = new TreeMap<String, SimpleState>();
    
    final List<SimpleState> states = new ArrayList<SimpleState>();
    
    final Map<String, String> variables =new TreeMap<String, String>();
    
    public ZbnfState new_state()
    { return new ZbnfState();
    }
    
    public void add_state(ZbnfState value)
    { 
      //idxSrcStates.put(value.stateName, value);
      if(value.parallelParentState !=null){
        if(idxStates.get(value.parallelParentState) ==null){
          CompositeState parallel = new CompositeState(new ZbnfState());
          parallel.src.stateName = value.parallelParentState;
          parallel.src.parallelParent = value.enclState;
          parallel.src.shortdescription = "Parallel state machine inside " + value.enclState;
          idxStates.put(value.parallelParentState, parallel);
          //srcStates.add(parallel);  //add first the parallel self created state.
        }
      }
      srcStates.add(value);
      idxSrcStates.put(value.stateName, value);
    }
    
    public NameValue new_variable(){ return new NameValue(); }
    
    public void add_variable(NameValue inp){ variables.put(inp.name, inp.value); }
    
    public void set_statefnarg(String arg){ statefnargs.add(arg); }
   
    public void set_includeLine(String arg){ includeLines.add(arg); }
    
  }
  
  
  
  public StateMGen(MainCmd_ifc console, Args args)
  {
    this.console = console;
  }
  
  
  void execute(Args args) throws IOException, IllegalAccessException
  {
    ZbnfResultData stateData = parseAndStoreInput(args);
    if(stateData != null){
      prepareStateData(stateData);
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
        generator.setScriptVariable("stateData", 'O', stateData, true);
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
   * @param stateData The main instance for fill after parse, the main instance for generation.
   */
  void prepareStateData(ZbnfResultData stateData){
    for(ZbnfState state: stateData.srcStates){
      if(!state.isPrepared){
        prepareStateStructure(state, stateData, false, 0);
      }
    }
    for(SimpleState state: stateData.states){
      if(state.src.trans !=null){
        for(Trans trans: state.src.trans){
          prepareStateTrans(trans, state, stateData);
        }
      }
    }
  }
  
  
  
  /**Prepares the data for one state, called in a loop of all states.
   * @param stateP
   * @param stateData
   */
  void prepareStateStructure(ZbnfState srcState, ZbnfResultData stateData, boolean composite, int recurs){
    //stateStructure
    if(srcState.isPrepared){
      return;
    }
    //ZbnfState state = stateP;
    final SimpleState state1 = composite ? new CompositeState(srcState) : new SimpleState(srcState);
    if(srcState.stateName.equals("Active1"))
      stop();
    if(srcState.enclState !=null){
      
      SimpleState enclState = stateData.idxStates.get(srcState.enclState);
      if(enclState ==null || !enclState.src.isPrepared){
        //call this routine recursively to assert that all enclosing states are prepared.
        if(recurs > 20) throw new IllegalArgumentException("StateMGen - parent states in a loop.");
        ZbnfState srcEnclState = stateData.idxSrcStates.get(srcState.enclState);
        prepareStateStructure(srcEnclState, stateData, true, recurs+1);
      }
      if(!(enclState instanceof CompositeState)){
        CompositeState encl1 = new CompositeState(enclState);
        stateData.idxStates.put(encl1.src.stateName, encl1);  //replace old SimpleState.
      }
      //all parent states are prepared!
      if(enclState.src.hasHistory || enclState.src.parallelParentState !=null){
        state1.stateVariableName = "state" + enclState.src.stateName;
        stateData.idxStateVariables.put(state1.stateVariableName, state1.stateVariableName);  //add only one time because it is a key-map
      } else {
        state1.stateVariableName = enclState.stateVariableName;  //use same variable if a history is not necessary.
      }
    }
    else if(srcState.parallelParentState !=null) {
      SimpleState parallelState = stateData.idxStates.get(srcState.parallelParentState);
      if(parallelState == null || !parallelState.src.isPrepared){
        //call this routine recursively to assert that all enclosing states are prepared.
        if(recurs > 20) throw new IllegalArgumentException("StateMGen - parent states in a loop.");
        ZbnfState srcEnclState = stateData.idxSrcStates.get(srcState.enclState);
        prepareStateStructure(srcEnclState, stateData, true, recurs+1);
      }
      //all parent states are prepared!
      state1.stateVariableName = null;  //The container for the parallel states has not an stateVariable itself.
    }
    else {
      //it is a state of top level:
      state1.stateVariableName = "state"; 
      if(stateData.topStates.get(state1.src.stateName) == null){
        stateData.topStates.put(state1.src.stateName, state1);
      }
    }
    //register the state in all its enclosing states.
    boolean bEnclosingStateCheck = true;  //set to false on top level
    SimpleState state = state1;
    while(bEnclosingStateCheck && state1.src.enclState !=null){
      String enclStateName = state1.src.enclState;
      SimpleState enclStateS = stateData.idxStates.get(enclStateName);
      if(enclStateS !=null) {
        assert(enclStateS instanceof CompositeState);
        CompositeState enclState = (CompositeState)enclStateS;
        if(state1.src.parallelParentState !=null){
          String parallelStateName = state1.src.parallelParentState;
          SimpleState parallelStateS = stateData.idxStates.get(parallelStateName);
          assert(parallelStateS !=null && parallelStateS instanceof CompositeState);
          CompositeState parallelState = (CompositeState)parallelStateS;
          if(enclState.parallelStates ==null){
            enclState.parallelStates = new TreeMap<String, CompositeState>(); 
          }
          if(enclState.parallelStates.get(parallelState.src.stateName) == null){
            enclState.parallelStates.put(parallelState.src.stateName, parallelState);
          }
          if(parallelState.subStates ==null){
            parallelState.subStates = new TreeMap<String, SimpleState>(); 
          }
          if(parallelState.subStates.get(state.src.stateName) == null){
            parallelState.subStates.put(state.src.stateName, state);
          }
          
        } else {
          if(enclState.subStates ==null){
            enclState.subStates = new TreeMap<String, SimpleState>(); 
          }
          if(enclState.subStates.get(state.src.stateName) == null){
            enclState.subStates.put(state.src.stateName, state);
          }
        }
        state = enclState;
      }else {
        bEnclosingStateCheck = false;  //top level reached.
      }
    }
    state1.src.isPrepared = true;  
  }

  
  /**Prepares the primary data of a Transition, produce ready-to-use data for code generation.
   * @param trans
   * @param state
   * @param stateData
   */
  void prepareStateTrans(Trans trans, SimpleState state, ZbnfResultData stateData){
    if(trans.dstState == null && trans.subCondition !=null){
      for(Trans subCond: trans.subCondition){
        prepareStateTrans(subCond, state, stateData);
      }
    } else if(trans.dstState != null) {
      
    	//A transition can contain joinStates, States in a parallel machine which should be active.
    	if(trans.joinStatesSrc !=null){
    		trans.joinStates = new LinkedList<SimpleState>();
    		for(String sJoinState: trans.joinStatesSrc){
          SimpleState joinState = stateData.idxStates.get(sJoinState);
          if(joinState ==null){
          	System.err.println("faulty joinState");
          } else {
            trans.joinStates.add(joinState);          	
          }
    		}
    	}
    	
    	
    	
      int nrofDstStates = trans.dstState.size();
      @SuppressWarnings("unchecked")
      List<SimpleState>[] listDst1 = new LinkedList[nrofDstStates];
      SimpleState[] entryStates = new SimpleState[nrofDstStates];
      int ix = 0;
      for(ix = 0; ix < listDst1.length; ++ix) {
        listDst1[ix] = new LinkedList<SimpleState>();
      }
      SimpleState exitState = state;
      boolean found = false;
      while(!found){
        trans.exitStates.add(exitState);
        if(exitState.src.enclState != null){
          SimpleState exitState2 = stateData.idxStates.get(exitState.src.enclState);
          if(exitState2 == null){
            System.out.println("\nSemantic error: parent state \"" + exitState.src.enclState + "\" in state \"" + state.src.stateName + "\"not found. ");
            return;
          }
          exitState = exitState2;
        } else {
          exitState = null;  //it is the top state.
        }
        found = true;  //set to false if any not found.
        ix = 0;
        for(String dstStateName: trans.dstState){
          listDst1[ix].clear();
          entryStates[ix] = stateData.idxStates.get(dstStateName);
          if(entryStates[ix] == null){
            System.out.println("\nSemantic error: destination state \"" + dstStateName + "\" from state \"" + state.src.stateName + "\" not found. ");
            return;
          }
          do { 
            //check entryStates down to either the exitState or down to to top state.
            //If the exit state is equal any entry enclosing state or both are null (top state), then it is the common state.
            listDst1[ix].add(0, entryStates[ix]);
            if(entryStates[ix].src.enclState != null){
              SimpleState entryState = stateData.idxStates.get(entryStates[ix].src.enclState);
              if(entryState == null){
                System.out.println("\nSemantic error: parent state \"" + entryStates[ix].src.enclState + "\" in state \"" + state.src.stateName + "\"not found. ");
                return;
              }
              entryStates[ix] = entryState;
            } else {
              entryStates[ix] = null;  //it is the top state.
            }
          }while(entryStates[ix] != null && exitState != entryStates[ix]);
          //the loop is finished if the exitstate and the enclosing entryState are equal or the enclosing entrystate reaches the topstate.
          if(exitState != entryStates[ix]){  //check whether both are equal or both are the top state, it is null.
            //not equal, the exitstate is not a common state of this dst state, it means it is not the common state of all.
            found = false;
          }
          ix +=1;
        }
        //the common enclosing state is found.
        //dst.entrySubStates.clear();
        trans.dstStateTree = new DstState();
        
        for(List<SimpleState> listEntries: listDst1){
          DstState dstStateTreeNode = trans.dstStateTree;
          //dstStateTreeNode = dst;
          for(SimpleState entryState: listEntries){
            if(dstStateTreeNode.entrySubStates == null){
              dstStateTreeNode.entrySubStates = new LinkedList<DstState>();
            }
            boolean foundEntry = false;
            for(DstState entry: dstStateTreeNode.entrySubStates){
              if(entry.entryState == entryState){
                foundEntry = true;
                dstStateTreeNode = entry;
              }
            }
            if(!foundEntry){
              DstState entryDst = new DstState();
              entryDst.entryState = entryState;
              entryDst.name = entryState.src.stateName;
              dstStateTreeNode.entrySubStates.add(entryDst);
              dstStateTreeNode = entryDst;
            }
          }
        }
        
      }
    } else {
      System.err.println("unexpected");
    }
  }
  
  
  /**This method reads the input VHDL-script, parses it with ZBNF, 
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
