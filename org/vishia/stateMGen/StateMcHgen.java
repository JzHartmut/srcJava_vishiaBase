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
import org.vishia.states.StateSimple.PlugStateSimpleToGenState;
import org.vishia.states.StateSimple.Trans;
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

  
  
  
  
  
  
  
  
  
  
  
   
  GenStateMachine genStm;




  public StateMcHgen(MainCmd_ifc console)
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
          //DataShow.dataTreeXml(genStm, out, 20);
        }
        else if(args.sFileSrcData.endsWith(".xml")){
          String sFileDstData = args.sFileSrcData.substring(0, args.sFileSrcData.length()-4) + ".dst.xml";
          out = new FileWriter(sFileDstData);
          //DataShow.dataTreeXml(genStm, out, 20);
        }
        else {
          String sFileDstData = args.sFileSrcData + ".dst";
          out = new FileWriter(sFileDstData);
          //DataShow.dataTree(genStm, out, 20);
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
        //generator.setScriptVariable("stm", 'O', genStm, true);
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
  
  

  /**Prepares the parsed data to some dependencies of states etc. This routine is called after parse and fill to provide usability data. 
   * @param zbnfSrc The main instance for fill after parse, the main instance for generation.
   */
  void prepareStateData(ZbnfResultData zbnfSrc){
    //creates the instance for all prepared data:
    genStm = new GenStateMachine(zbnfSrc);
    StateComposite stateTop = genStm.stateTop();
    //stateTop.setAuxInfo(new GenStateInfo(null)); //instance for Code generation for the top state.  
    //gather all states and transitions in the parsed data and add it to the prepared data:
    genStm.rootStates.add(stateTop); //the stateTop is the first rootState.
    gatherStatesOfComposite(stateTop, stateTop, zbnfSrc.topStateStruct);
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
  StateCompositeFlat gatherStatesOfComposite(StateCompositeFlat stateComposite, StateComposite rootState, ZbnfState zbnfComposite)
  { 
    for(ZbnfState zbnfState: zbnfComposite.listSubstates){
      if(!zbnfState.isPrepared){
        StateSimple state1;  //either a GenStateSimple or a GenStateComposite, add it after creation and evaluation.
        //
        if(zbnfState.listSubstates !=null && zbnfState.listSubstates.size() >0) {
          final StateSimple stateComposite1;
          if(zbnfState.stateParallel) {
            //stateComposite1 = new GenStateParallel(stateComposite, rootState, genStm, zbnfState);
            stateComposite1 = new StateParallel(zbnfState.stateName, genStm, new StateSimple[zbnfState.listSubstates.size()]);
            state1 = gatherStatesOfParallel((StateParallel)stateComposite1, rootState, zbnfState);
          } else {
            StateComposite rootState1;
            if(zbnfState.hasHistory) { 
              stateComposite1 = new StateComposite(zbnfState.stateName, genStm, new StateSimple[zbnfState.listSubstates.size()]);
              rootState1 = (StateComposite)stateComposite1;
              genStm.rootStates.add(rootState1);
            } else { 
              stateComposite1 = new StateCompositeFlat(zbnfState.stateName, genStm, new StateSimple[zbnfState.listSubstates.size()]);
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
  StateParallel gatherStatesOfParallel(StateParallel stateParallel, StateComposite rootState, ZbnfState zbnfParallel)
  { 
    for(ZbnfState zbnfState: zbnfParallel.listSubstates){
      if(!zbnfState.isPrepared){
        StateSimple state1;  //either a GenStateSimple or a GenStateComposite, add it after creation and evaluation.
        //
        if(zbnfState.listSubstates !=null && zbnfState.listSubstates.size() >0) {
          final StateSimple stateComposite1;
          if(zbnfState.stateParallel) {
            throw new IllegalArgumentException("the next level of StateParallel cannot be a StateParallel");
          } else {
            StateComposite noRootState = null;
            stateComposite1 = new StateComposite(zbnfState.stateName, genStm, new StateSimple[zbnfState.listSubstates.size()]);
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
      Object ozbnfState = genState.auxInfo();
      ZbnfState zbnfState = (ZbnfState)genState.auxInfo();
      int zTransitions;
      if(zbnfState.dst_Dst !=null && (zTransitions = zbnfState.dst_Dst.size()) >0) {
        plugState.createTransitions(zTransitions);
        for(String zbnfTrans: zbnfState.dst_Dst){
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
          for(String sDstState : listDst) {
            StateSimple.Trans trans;
            if(sDstState.endsWith("history")) {
              int lenDstState = sDstState.length() - 7; //without "history"
              StateSimple dstState1 = genStm.allStates.get(sDstState.substring(0,lenDstState));
              //trans = dstState1.new TransDeepHistory(zbnfTrans, dstKeys);
              trans = dstState1.new Trans(zbnfTrans, dstKeys);
              
            } else {
              StateSimple dstState1 = genStm.allStates.get(sDstState);
              if(dstState1 == null) throw new IllegalArgumentException("faulty dst state in transition;" + sDstState + "; from state " + genState.getName());
              dstKeys[++ixDst] = dstState1.hashCode();
              /*
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
              } else */
              trans = dstState1.new Trans(zbnfTrans, dstKeys);
            }
            plugState.addTransition(trans);
          }
        }
      }
    }
  }



  /**This is the root class for the Zbnf parsing result.
   * It refers to the main syntax component.
   */
  public static class ZbnfResultData
  {
  
  
    public List<String> includeLines = new LinkedList<String>();
    
    public String transFnArgs;
    
    
    public Map<String, ZbnfEntryExitCheck> idxEntry = new TreeMap<String, ZbnfEntryExitCheck>();
    
    public Map<String, ZbnfEntryExitCheck> idxExit = new TreeMap<String, ZbnfEntryExitCheck>();
    
    public Map<String, ZbnfEntryExitCheck> idxCheck = new TreeMap<String, ZbnfEntryExitCheck>();
    
    /**From Zbnf: stores an include line in the syntax context: <pre>
     * { #include <* \n?includeLine>
     * }
     * </pre>
     * @param arg The parsed string to this semantic component.
     */
    public void set_includeLine(String arg){ includeLines.add(arg); }
    
    public void set_entryState(String arg){  }
    
    public ZbnfEntryExitCheck new_entryState(){ return new ZbnfEntryExitCheck();  }
    
    public void add_entryState(ZbnfEntryExitCheck val){ idxEntry.put(val.state, val);  }
    
    public void set_exitState(String arg){  }
    
    public ZbnfEntryExitCheck new_exitState(){ return new ZbnfEntryExitCheck();  }
    
    public void add_exitState(ZbnfEntryExitCheck val){ idxExit.put(val.state, val);  }
    
    public void set_checkState(String arg){  }

    public ZbnfEntryExitCheck new_checkState(){ return new ZbnfEntryExitCheck();  }
    
    public void add_checkState(ZbnfEntryExitCheck val){ idxCheck.put(val.state, val);  }
 
    
    public ZbnfState topStateStruct;
    
    public String topStateType, topStateName;
    
  }
  
  
  
  
  public static class ZbnfEntryExitCheck
  {
    public String state;
    
    public String args;
    
  }
  
  
  public static class ZbnfState
  {
    public String tagname, stateName;
    
    public String stateId;
    
    /**Set from Zbnf: int history <?hasHistory>*/
    public boolean hasHistory;
    
    /**Set from Zbnf: int parallel <?stateParallel>*/
    public boolean stateParallel;
    
    public List<String> dst_Dst = new LinkedList<String>();
    
    List<ZbnfState> listSubstates;
    
    public ZbnfState new_stateStruct() { return new ZbnfState(); }
    
    public void add_stateStruct(ZbnfState val) { 
      if(listSubstates == null) { listSubstates = new LinkedList<ZbnfState>(); }
      listSubstates.add(val); }
    
    /**Set to true if the state was prepared already. */
    boolean isPrepared = false;
    
    @Override public String toString(){ return stateName; }
  

    
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
    { super(new StateSimple[zbnfSrc.topStateStruct.listSubstates.size()]);
      this.zbnfSrc = zbnfSrc;
    }
    
    StateCompositeTop stateTop(){ return stateTop; }
    
    
    void prepare() {
      stateTop.prepare();
    }
    
    
  }
  

  
  static class GenStateSimple extends StateSimple
  {
    
    GenStateSimple(StateSimple enclState, StateComposite rootState, StateMachine stm, ZbnfState zbnfState){
      super();
      super.setAuxInfo(zbnfState);
      this.enclState = enclState;
      this.rootState = rootState;
      this.stateMachine = stm;
      stateId = zbnfState.stateName;
    }
    
    
  }
  
  

  
  
  void stop(){}

}
