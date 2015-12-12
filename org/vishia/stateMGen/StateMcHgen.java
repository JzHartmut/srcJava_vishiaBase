package org.vishia.stateMGen;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.script.ScriptException;

import org.vishia.mainCmd.MainCmd_ifc;
import org.vishia.cmd.JZcmdExecuter;
import org.vishia.states.StateComposite;
import org.vishia.states.StateCompositeFlat;
import org.vishia.states.StateDeepHistory;
import org.vishia.states.StateMachine;
import org.vishia.states.StateParallel;
import org.vishia.states.StateSimple;
import org.vishia.util.DataShow;
import org.vishia.util.Debugutil;
import org.vishia.util.StringPart;
import org.vishia.zbnf.ZbnfJavaOutput;
import org.vishia.zcmd.JZcmd;
import org.vishia.zcmd.Zbnf2Text;

/**This class prepares information for a state machine from representation in text format 
 * for generation C-code and documentation.
 * It calls the Zbnf parser and Zbnf2Text for generation.
 * See {@link #main(String[])}
 * 
 * @author Hartmut Schorrig
 *
 */
public class StateMcHgen {
  
  /**Version, history and license.
   * <ul>
   * <li>2015-11-14 Hartmut new: Created, other concept for syntax as StateMGen
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


  protected Map<String, ZbnfState> idxStates = new TreeMap<String, ZbnfState>();




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
      StateMcHgen main = new StateMcHgen(mainCmdLine);
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

  
  
  
  
  
  
  
  
  
  
  
   
  //GenStateMachine zsrc.stateM;

  /**The root of the parsing result data. */
  ZbnfResultData zsrc;


  public StateMcHgen(MainCmd_ifc console)
  {
    this.console = console;
  }
  
  

   
  void execute(Zbnf2Text.Args args) throws IOException, IllegalAccessException
  {
    this.zsrc = parseAndStoreInput(args);  //parsed and converts into Java data presentation
    if(zsrc != null){
      if(args.sFileSrcData!=null) {
        Writer out  = new FileWriter(args.sFileSrcData);
        if(args.sFileSrcData.endsWith(".html") || args.sFileSrcData.endsWith(".htm")){
          DataShow.outHtml(zsrc, out);
        }
        else if(args.sFileSrcData.endsWith(".xml")){
          DataShow.dataTreeXml(zsrc, out, 20);
        }
        else {
          DataShow.dataTree(zsrc, out, 20);
        }
        out.close();
      }
      
      prepareStateData(zsrc);
      if(args.sFileSrcData!=null) {
        Writer out;
        if(args.sFileSrcData.endsWith(".html")){
          String sFileDstData = args.sFileSrcData.substring(0, args.sFileSrcData.length()-5) + ".dst.html";
          out = new FileWriter(sFileDstData);
          DataShow.outHtml(zsrc, out);
        }
        else if(args.sFileSrcData.endsWith(".htm")){
          String sFileDstData = args.sFileSrcData.substring(0, args.sFileSrcData.length()-4) + ".dst.htm";
          out = new FileWriter(sFileDstData);
          //DataShow.dataTreeXml(zsrc.stateM, out, 20);
        }
        else if(args.sFileSrcData.endsWith(".xml")){
          String sFileDstData = args.sFileSrcData.substring(0, args.sFileSrcData.length()-4) + ".dst.xml";
          out = new FileWriter(sFileDstData);
          //DataShow.dataTreeXml(zsrc.stateM, out, 20);
        }
        else {
          String sFileDstData = args.sFileSrcData + ".dst";
          out = new FileWriter(sFileDstData);
          //DataShow.dataTree(zsrc.stateM, out, 20);
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
        generator.setScriptVariable("zsrc", 'O', zsrc, true);
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
  
  
  
  
  
  
  
  /**This method reads the input script, parses it with ZBNF, 
   * stores all results in the Java-class {@link ZbnfResultData} 
   */
  private ZbnfResultData parseAndStoreInput(Zbnf2Text.Args args)
  {
    /**The instance to store the data of parsing result is created locally and private visible: */
    this.zsrc = new ZbnfResultData();
    /**This call processes the whole parsing and storing action: */
    File fileIn = new File(args.sFileIn);
    File fileSyntax = new File(args.sFileSyntax);
    String sError = ZbnfJavaOutput.parseFileAndFillJavaObject(zsrc.getClass(), zsrc, fileIn, fileSyntax, console, 1200);
    if(sError != null)
    { /**there is any problem while parsing, report it: */
      console.writeError("ERROR Parsing file: " + fileIn.getAbsolutePath() + "\n" + sError);
      return null;
    }
    else
    { console.writeInfoln("SUCCESS parsed: " + fileIn.getAbsolutePath());
      return zsrc;
    }
  
  }
  
  

  /**Prepares the parsed data to some dependencies of states etc. This routine is called after parse and fill to provide usability data. 
   * @param zbnfSrc The main instance for fill after parse, the main instance for generation.
   */
  void prepareStateData(ZbnfResultData zbnfSrc){
    //creates the instance for all prepared data:
    zsrc.stateM = new GenStateMachine(zbnfSrc);
    StateComposite stateTop = zsrc.stateM.stateTop();
    //stateTop.setAuxInfo(new GenStateInfo(null)); //instance for Code generation for the top state.  
    //gather all states and transitions in the parsed data and add it to the prepared data:
    zsrc.stateM.rootStates.add(stateTop); //the stateTop is the first rootState.
    gatherStatesOfComposite(stateTop, stateTop, zbnfSrc.topState);
    //
    gatherAllTransitions();
    //complete the stateSrc in the State routines.
    for(Map.Entry<String, ZbnfEntryExitCheck> e: zbnfSrc.idxCheck.entrySet()) {
      ZbnfEntryExitCheck trans = e.getValue();
      trans.stateSrc = zsrc.stateM.allStates.get(trans.stateName);
    }
    for(Map.Entry<String, ZbnfEntryExitCheck> e: zbnfSrc.idxEntry.entrySet()) {
      ZbnfEntryExitCheck trans = e.getValue();
      trans.stateSrc = zsrc.stateM.allStates.get(trans.stateName);
    }
    for(Map.Entry<String, ZbnfEntryExitCheck> e: zbnfSrc.idxExit.entrySet()) {
      ZbnfEntryExitCheck trans = e.getValue();
      trans.stateSrc = zsrc.stateM.allStates.get(trans.stateName);
    }
    
    //invoke prepare, the same as for Java state machines.
    zsrc.stateM.prepare();
    
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
  StateCompositeFlat gatherStatesOfComposite(StateCompositeFlat stateComposite, StateComposite rootState, ZbnfState zbnfComposite)
  { 
    if(zbnfComposite.listSubstates !=null) {
      for(ZbnfState zbnfState: zbnfComposite.listSubstates){
        addSubstate(rootState, stateComposite, zbnfState);
    } }
    
    return stateComposite;
  }



  void addSubstate(StateComposite rootState, StateCompositeFlat stateComposite, ZbnfState zbnfState)
  {
    if(!zbnfState.isPrepared){
      StateSimple state1;  //either a GenStateSimple or a GenStateComposite, add it after creation and evaluation.
      //
      if(zbnfState.listSubstates !=null) {
        int sizeSubStates = zbnfState.listSubstates.size();
        final StateSimple stateComposite1;
        if(zbnfState.bStateParallel) {
          //stateComposite1 = new GenStateParallel(stateComposite, rootState, zsrc.stateM, zbnfState);
          stateComposite1 = new GenStateParallel(stateComposite, rootState, zsrc.stateM, zbnfState, new StateSimple[sizeSubStates]);
          stateComposite1.setAuxInfo(zbnfState);
          state1 = gatherStatesOfParallel((StateParallel)stateComposite1, rootState, zbnfState);
        } else {
          StateComposite rootState1;
          if(zbnfState.isComposite) {  //zbnfState.stateHistory !=null ||  
            stateComposite1 = new GenStateComposite(stateComposite, rootState, zsrc.stateM, zbnfState, new StateSimple[sizeSubStates]);
            stateComposite1.setAuxInfo(zbnfState);
            rootState1 = (StateComposite)stateComposite1;
            zsrc.stateM.rootStates.add(rootState1);
          } else { 
            stateComposite1 = new GenStateCompositeFlat(stateComposite, rootState, zsrc.stateM, zbnfState, new StateSimple[sizeSubStates]);
            stateComposite1.setAuxInfo(zbnfState);
            rootState1 = rootState; 
          }  
          state1 = gatherStatesOfComposite((StateCompositeFlat)stateComposite1, rootState1, zbnfState);
        }
      } else if(zbnfState.stateIdName.equals("DeepHistory")){
        state1 = new StateDeepHistory(zbnfState.stateName);
      } else {
        state1 = new GenStateSimple(stateComposite, rootState, zsrc.stateM, zbnfState);
        state1.setAuxInfo(zbnfState);
      }
      if(!(state1 instanceof StateDeepHistory)) {
        zsrc.listStates.add(state1);
      }
      //
      stateComposite.addState(state1.hashCode(), state1);
      //Set it in index to find out destination of transitions.
      zsrc.stateM.allStates.put(state1.getName(), state1);
    }

  }








  /**Gathers a Composite state from its Zbnf parsers result.
   * @param stateComposite
   * @param zbnfComposite
   * @return
   */
  StateParallel gatherStatesOfParallel(StateParallel stateParallel, StateComposite rootState, ZbnfState zbnfParallel)
  { 
    for(ZbnfState zbnfState: zbnfParallel.listSubstates){
      if(!zbnfState.isPrepared){
        StateSimple state1;  //either a GenStateSimple or a GenStateComposite, add it after creation and evaluation.
        //
        if(zbnfState.listSubstates !=null && zbnfState.listSubstates.size() >0) {
          final StateSimple stateComposite1;
          if(zbnfState.bStateParallel) {
            throw new IllegalArgumentException("the next level of StateParallel cannot be a StateParallel");
          } else {
            StateComposite noRootState = null;
            stateComposite1 = new GenStateComposite(stateParallel, noRootState, zsrc.stateM, zbnfState, new StateSimple[zbnfState.listSubstates.size()]);
            stateComposite1.setAuxInfo(zbnfState);
            StateComposite rootState1 = (StateComposite)stateComposite1;
            zsrc.stateM.rootStates.add(rootState1);
            state1 = gatherStatesOfComposite((StateComposite)stateComposite1, rootState1, zbnfState);
          }
          
        } else {
          state1 = new GenStateSimple(stateParallel, rootState, zsrc.stateM, zbnfState);
          state1.setAuxInfo(zbnfState);
        }
        zsrc.listStates.add(state1);
        //Adds the parallel composite state to the StateParallel:
        stateParallel.addState(state1.hashCode(), state1);
        zsrc.stateM.allStates.put(state1.getName(), state1);
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
    for(StateSimple genState1 : zsrc.stateM.stateList()) {
      StateSimple parentState = genState1;
      List<StateSimple.Trans> listTrans = new LinkedList<StateSimple.Trans>();
      String parentPath = "";
      while(parentState !=null) {
        Object ozbnfState = parentState.auxInfo();
        ZbnfState zbnfState = (ZbnfState)parentState.auxInfo();
        if(zbnfState !=null) {  //not for auto generated states, for example History state.
          int zTransitions;
          if(zbnfState.dotransDst !=null && (zTransitions = zbnfState.dotransDst.size()) >0) {
            for(String zbnfTrans: zbnfState.dotransDst){
              List<String> listDst = new LinkedList<String>();
              int sep = 0;
              while(sep >=0){
                int sepe = zbnfTrans.indexOf('_', sep);
                String dst = sepe >=0 ? zbnfTrans.substring(sep, sepe) : zbnfTrans.substring(sep);  
                listDst.add(dst);
                sep = sepe >=0 ? sepe +1 : sepe;
              }
              
              int nrofForks = listDst.size(); 
              int[] dstKeys = new int[nrofForks];
              int ixDst = -1;
              GenTrans trans;
              for(String sDstState : listDst) {
                /*
                boolean bHistory = sDstState.endsWith("history"); //TODO no more necessary
                if(bHistory){
                  sDstState = sDstState.substring(0, sDstState.length() - "history".length());
                }
                */
                //ZbnfState dstState2 = idxStates.get(sDstState);
                StateSimple dstState1 = zsrc.stateM.allStates.get(sDstState);
                  if(dstState1 == null){ 
                    dstState1 = zsrc.stateM.allStates.get(sDstState + "_State"); 
                  }
                  if(dstState1 == null) {
                    throw new IllegalArgumentException("faulty dst state in transition;" + sDstState + "; from state " + parentState.getName());
                  }
                  /*
                  if(bHistory) {
                    dstState1 = ((ZbnfState)dstState1.auxInfo()).stateHistory;
                    if(dstState1 == null) throw new IllegalArgumentException("history state not found in transition;" + sDstState + "; from state " + genState.getName());
                  }
                  */
                  dstKeys[++ixDst] = dstState1.hashCode();
                  /*
                  if(zbnfTrans.joinStates !=null){
                    trans = genState.new TransJoin("Trans_" + genState.getName() + zbnfTrans.nrTrans, dstKeys); 
                    int[] joinKeys = new int[zbnfTrans.joinStates.size()];
                    int ixJoin = -1;
                    for(String sJoinState: zbnfTrans.joinStates){
                      StateSimple joinState1 = zsrc.stateM.allStates.get(sJoinState);
                      if(joinState1 == null) throw new IllegalArgumentException("faulty join state in transition;" + sJoinState + "; from state " + genState.getName());
                      joinKeys[++ixJoin] = joinState1.hashCode();
                    }
                    ((StateSimple.TransJoin)trans).srcStates(joinKeys);  //set the hashes of the join sources.
                  } else */
              }
              trans = new GenTrans(genState1, zbnfTrans, dstKeys);
              trans.parentPath = parentPath;
              trans.enclStateName = parentState.getName();
              listTrans.add(trans);
            }
          }
        }
        parentState = null; //don't gather parent transitions. parentState.enclState();
        if(parentState instanceof StateParallel) { parentPath += ".parallelParent"; }
        else { parentPath += ".parent"; }
      }
      StateSimple.PlugStateSimpleToGenState plugState = genState1.new PlugStateSimpleToGenState();
      plugState.createTransitions(listTrans.size());
      for(StateSimple.Trans trans: listTrans) {
        plugState.addTransition(trans);
      }
      
    }
  }



  /**This is the root class for the Zbnf parsing result.
   * It refers to the main syntax component.
   */
  public class ZbnfResultData //extends ZbnfState
  {
  
  
    public List<String> includeLines = new LinkedList<String>();
    
    public String stateInstance;
    
    public String topStateType;
    
    public String userDataType;
    
    public String transFnArgs;
    
    
    public Map<String, ZbnfEntryExitCheck> idxEntry = new TreeMap<String, ZbnfEntryExitCheck>();
    
    public Map<String, ZbnfEntryExitCheck> idxExit = new TreeMap<String, ZbnfEntryExitCheck>();
    
    public Map<String, ZbnfEntryExitCheck> idxCheck = new TreeMap<String, ZbnfEntryExitCheck>();
    
    //public List<ZbnfState> topStates = new LinkedList<ZbnfState>();
    
    //public String topStateType, stateName, tagName;
    
    /**Only necessary for #gatherStatesOfComposite.*/
    private ZbnfState topState = new ZbnfState();
    
    /**All states for which are to generate. */
    public List<StateSimple> listStates = new ArrayList<StateSimple>();
    
    GenStateMachine stateM;
    
    ZbnfResultData(){}
    
    /**From Zbnf: stores an include line in the syntax context: <pre>
     * { #include <* \n?includeLine>
     * }
     * </pre>
     * @param arg The parsed string to this semantic component.
     */
    public void set_includeLine(String arg){ includeLines.add(arg); }
    
    
    public ZbnfState new_stateDef(){ return new ZbnfState(); }
    
    public void add_stateDef(ZbnfState val){ 
      idxStates.put(val.stateName, val);
      if(val.zbnfParent == null) {
        //has not a parent state, it is direct member of the top state.
        if(topState.listSubstates == null) { topState.listSubstates = new LinkedList<ZbnfState>(); }
        topState.listSubstates.add(val);
      }
      if(val.tagname !=null) {
      }
    }
    
    
    public void set_entryState(String arg){  }
    
    public ZbnfEntryExitCheck new_entryState(){ return new ZbnfEntryExitCheck();  }
    
    public void add_entryState(ZbnfEntryExitCheck val){ idxEntry.put(val.stateName, val);  }
    
    public void set_exitState(String arg){  }
    
    public ZbnfEntryExitCheck new_exitState(){ return new ZbnfEntryExitCheck();  }
    
    public void add_exitState(ZbnfEntryExitCheck val){ idxExit.put(val.stateName, val);  }
    
    public void set_checkState(String arg){  }

    public ZbnfEntryExitCheck new_checkState(){ return new ZbnfEntryExitCheck();  }
    
    public void add_checkState(ZbnfEntryExitCheck val){ 
      //The last argument in the arglist is the State struct type. Search it.
      //StringPart args = new StringPart(val.formalArgList);
      //args.seekBackward(",").seekNoWhitespace().lentoIdentifier();
      //String stateName = args.toString();
      idxCheck.put(val.stateName, val);  
    }
 
    

    
  }
  
  
  
  
  public static class ZbnfEntryExitCheck
  {
    public String restName;
    
    String stateName;
    
    /**The source state of the transition routine. It is set in {@link StateMcHgen#prepareStateData(ZbnfResultData)}
     * form the {@link #state} */
    public StateSimple stateSrc;
    
    public String formalArgList;
    
    public List<String> argVariables = new LinkedList<String>();
    
    ZbnfEntryExitCheck(){}
    
    /**From Zbnf, invoked with [<?args> ....] as component in []
     * @return this as instance for the component. 
     */
    public ZbnfEntryExitCheck new_args(){ return this; }
    
    /**From Zbnf, invoked with [<?args> ....] as component in []
     * @param val it is this, not used
     */
    public void add_args(ZbnfEntryExitCheck val) {}
    
    /**From Zbnf, invoked with [<?args> ....] as String in [], the parsed content is stored as String too.
     * @param val
     */
    public void set_args(String val){
      formalArgList = val;
    }
    
    /**From Zbnf, invoked with [<?...> {<* |,|)?arg> ? , }]
     * @param val One argument till , or ) but without leading and trailing spaces.
     */
    public void set_arg(String val){
      if(val.length() >0) {
        StringPart spArg = new StringPart(val);
        this.stateName = spArg.lentoIdentifier().getCurrentPart().toString();  //the last type is the statename.
        String argName = spArg.setLengthMax().seekBackToAnyChar(" *").getCurrentPart().toString();
        argVariables.add(argName);
        //int posSpace = val.lastIndexOf(' '); //type name, after space the name starts
        //if(posSpace >0) {
        //  argVariables.add(val.substring(posSpace +1));
        //}
      } else {
        stateName = restName;
      }
      //else: empty arglist
    }
    
    
    @Override public String toString(){ return "ZbnfEntryExitCheck: " + stateName + ": " + argVariables; }
    
  }
  
  
  
  
  
  public class ZbnfState
  {
    public String tagname;
    //public String stateNameInStruct;
    
    /**From Zbnf: <code> ...} <$?stateName> .</code>, the type of a <code>stateDef::=</code> */
    public String stateName;
    
    
    /**From Zbnf: <code>_<*_; ?stateIdName></code>, the name part befor stateId */
    public String stateIdName;
    
    /**From Zbnf: <code>_<*\ ;?stateId></code>, the number for this state. For example id_0x102.
     * Use default value for parallel states. */
    public String stateId = "0";
    
    
    //public String stateHistory;
    //StateDeepHistory stateHistory;
    
    ZbnfState zbnfParent;
    
    /**Set from Zbnf: int history <?isComposite>*/
    public boolean isComposite;
    
    /**Set from Zbnf: int parallel <?stateParallel>*/
    public boolean bStateParallel;
    
    public List<String> dotransDst = new LinkedList<String>();
    
    List<ZbnfState> listSubstates;
    
    /**From Zbnf: StateParallel_Fwc <*_;\ ?stateParallel>[_<*\ ;?stateId>]. */
    public void set_stateParallel(String val) {
      bStateParallel = true;
      stateIdName = val;
    }
    
    /**ZBNF: <code>[<$?parentState> parent ; ]</code>
     * The parent state should not be referenced from this, but the parent should know its children
     * in the {@link #listSubstates}. The state with the given name is searched in {@link StateMcHgen#idxStates}.
     * In that state this instance is added as substate.
     * @param name type name in C
     */
    public void set_parentState(String name) {
      zbnfParent = StateMcHgen.this.idxStates.get(name);
      if(zbnfParent == null) {
        throw new IllegalArgumentException("Parent state not found, " + name);
      }
      if(zbnfParent.listSubstates == null) { zbnfParent.listSubstates = new LinkedList<ZbnfState>(); }
      zbnfParent.listSubstates.add(this);
    }
    
    
    
    /**ZBNF: <code>[<$?parallelParentState> parellelParent ; ]</code>
     * The parent state should not be referenced from this, but the parent should know its children
     * in the {@link #listSubstates}. The state with the given name is searched in {@link StateMcHgen#idxStates}.
     * In that state this instance is added as substate. The parent state is marked as {@link #bStateParallel}
     * @param name type name in C
     */
    public void set_parallelParentState(String name) {
      zbnfParent = StateMcHgen.this.idxStates.get(name);
      if(zbnfParent == null) {
        throw new IllegalArgumentException("Parent state not found, " + name);
      }
      zbnfParent.bStateParallel = true;
      if(zbnfParent.listSubstates == null) { zbnfParent.listSubstates = new LinkedList<ZbnfState>(); }
      zbnfParent.listSubstates.add(this);
    }
    
    
    
    
    
    
    
    /**Set from Zbnf: int history <$?stateHistory>*/
    public void set_stateHistory(String name){
      ZbnfState stateHistory = new ZbnfState();
      stateHistory.stateName = name;
      stateHistory.stateIdName = "DeepHistory";
      stateHistory.zbnfParent = this;
      if(this.listSubstates == null){ this.listSubstates = new LinkedList<ZbnfState>(); }
      this.listSubstates.add(stateHistory); 
      this.isComposite = true;
      StateMcHgen.this.idxStates.put(name, stateHistory);
      //stateHistory = new StateDeepHistory();      
    }
    
    
    /**Set to true if the state was prepared already. */
    boolean isPrepared = false;
    
    @Override public String toString(){ return stateName; }
  

    
  }
  

  
  public static class GenTrans extends StateSimple.Trans
  {
    String parentPath;
    
    String enclStateName;
    
    GenTrans(StateSimple state, String name, int[] dstKeys){
      state.super(name, dstKeys);
    }
  }
  
  
  /**This is the top level instance named stm in the generation scripts.
   */
  public static class GenStateMachine extends StateMachine 
  {
  
    public final ZbnfResultData zbnfSrc;
    
    /**All states with an own state variable and timer, it is the top state, all parallel states
     * and composite states with a history.
     */
    public final List<StateComposite> rootStates = new LinkedList<StateComposite>();
    
    Map<String, StateSimple> allStates = new TreeMap<String, StateSimple>();

    
    GenStateMachine(ZbnfResultData zbnfSrc) 
    { super(new StateSimple[zbnfSrc.topState.listSubstates.size()]);
      this.zbnfSrc = zbnfSrc;
    }
    
    StateCompositeTop stateTop(){ return stateTop; }
    
    
    void prepare() {
      stateTop.prepare();
    }
    
    
  }
  

  
  
  /**Special Class because StateSimple is abstract, only for construction and non-abstract.
   */
  static class GenStateSimple extends StateSimple
  { 
    GenStateSimple(StateSimple enclState, StateComposite rootState, StateMachine stm, ZbnfState zbnfState){
      super();
      super.setAuxInfo(zbnfState);
      super.enclState = enclState;
      super.rootState = rootState;
      super.stateMachine = stm;
      super.stateId = zbnfState.stateName;
    }
  }
  
  

  /**Special Class because StateSimple is abstract, only for construction and non-abstract.
   */
  static class GenStateParallel extends StateParallel
  { //String stateName, StateMachine stateMachine, StateSimple[] aParallelstates
    GenStateParallel(StateSimple enclState, StateComposite rootState, StateMachine stm, ZbnfState zbnfState, StateSimple[] aParallelstates){
      super(zbnfState.stateName, stm, aParallelstates);
      super.setAuxInfo(zbnfState);
      super.enclState = enclState;
      super.rootState = rootState;
    }
  }
  
  

  /**Special Class because StateComposite is abstract, only for construction and non-abstract.
   */
  static class GenStateComposite extends StateComposite
  { 
    GenStateComposite(StateSimple enclState, StateComposite rootState, StateMachine stm, ZbnfState zbnfState, StateSimple[] aSubstates){
      super(zbnfState.stateName, stm, aSubstates);
      super.setAuxInfo(zbnfState);
      super.enclState = enclState;
      super.rootState = rootState;
    }
  }
  
  

  /**Special Class because StateCompositeFlat is abstract, only for construction and non-abstract.
   */
  static class GenStateCompositeFlat extends StateCompositeFlat
  { 
    GenStateCompositeFlat(StateSimple enclState, StateComposite rootState, StateMachine stm, ZbnfState zbnfState, StateSimple[] aSubstates){
      super(zbnfState.stateName, stm, aSubstates);
      super.setAuxInfo(zbnfState);
      super.enclState = enclState;
      super.rootState = rootState;
    }
  }
  
  

  
  
  void stop(){}

}
