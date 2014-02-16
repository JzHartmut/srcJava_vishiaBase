package org.vishia.stateMGen;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.TreeMap;


import org.vishia.mainCmd.MainCmd;
import org.vishia.mainCmd.MainCmd_ifc;
import org.vishia.cmd.ZGenExecuter;
import org.vishia.zbnf.ZbnfJavaOutput;
import org.vishia.zgen.OutputDataTree;
import org.vishia.zgen.ZGen;
import org.vishia.zgen.Zbnf2Text;
import org.vishia.zgen.Zbnf2Text.Out;


public class StateMGen {
  


  /** Aggregation to the Console implementation class.*/
  final MainCmd_ifc console;


  /**This class holds the got arguments from command line. 
   * It is possible that some more operations with arguments as a simple get process.
   * The arguments may got also from configuration files named in cmd line arguments or other.
   * The user of arguments should know (reference) only this instance, not all other things arround MainCmd,
   * it it access to arguments. 
   */
  static class Args extends Zbnf2Text.Args
  {
    /**Cmdline-argument, set on -i: option. Inputfile to to something. */
    String sFileIn = null;
  
    /**Cmdline-argument, set on -s: option. Zbnf-file to output something. */
    String sFileZbnf = null;

    String sFileData = null;
    
    String sScriptCheck = null;
    
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
  public static class SimpleState
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
  public static class CompositeState
  {
    /**From ZBNF: <$?@stateName>. */
    public String stateName;
    
    /**From ZBNF: <""?description>. */
    public String description;
    
    public List<SimpleState> simpleState;
    
    public List<CompositeState> compositeState;
    
    public List<ParallelState> parallelState;
    
  }
  

  /**This class gets and stores the results from a parsed ZBNF-component <code>constant</code>.
   * 
   */
  public static class ParallelState extends CompositeState
  {
  }
  
  
  /**This class gets and stores the results from a parsed ZBNF-component <code>constant</code>.
   * 
   */
  public static class StateStructure
  {
    //public List<CompositeState> compositeState;
    public final List<CompositeState> compositeStates = new LinkedList<CompositeState>();
    
    
    public CompositeState new_CompositeState()
    { return new CompositeState();
    }
    
    public void add_CompositeState(CompositeState value)
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
  public static class State
  {
    /**From ZBNF: <$?@stateName>. */
    public String stateName;
    
    /**From ZBNF: <$?@enclState>. It is the name of the state where this state is member of. */
    public String enclState;
    
    /**From ZBNF: <$?@enclState>.<$?@parallelState>. It is the name of the state where this state is member of
     * in a parallel machine of the enclosing state. */
    public String parallelState;
    
    /**Name of the state which is the container of the parallel state machine. Only set on enclosing state
     * of a parallel state machine. */
    String parallelParent;
    
    /**All states which are direct sub-states in this state. null if the state has not sub-states. */
    private Map<String, State> subStates;

    /**All states which are parallel states in this states. null if the state has not parallel sub-states. */
    private Map<String, State> parallelStates;

    /**From ZBNF: \? <*|\?\.?description>. */
    public String description;
    
    /**From ZBNF: \? <*|\?\.?shortdescription>. */
    public String shortdescription;
    
    /**From ZBNF: + <*|\?\.?additionaldescription>. */
    public String additionaldescription;
    
    /**From ZBNF: ! <*|\?\.?description>. */
    public String tododescription;
    
    public ConstDef constDef;
    
    public Entry entry;
    
    public Exit exit;
    
    public Entry instate;
    
    public List<Trans> trans;
    
    public Entry new_instate() { return new Entry(); }

    public void set_instate(Entry val) { instate = val; }
    
    @Override public String toString(){ return stateName; }
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
    
    public String event;
    
    public String code;
    
    private final List<Trans> subCondition = new LinkedList<Trans>();

    
    private final List<State> exitStates = new LinkedList<State>();
    
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
    State srcState;
    
    State entryState;
    
    private List<DstState> entrySubStates;
    
    @Override public String toString(){ return name; }
    
  }
  
  
  public static class NameValue{ 
    public String name;
    public String value;
  }
  
  public static class ZbnfResultData
  {

    public StateStructure stateStructure;

    private final Map<String, State> idxStates = new TreeMap<String, State>();
    
    private final Map<String, State> topStates = new TreeMap<String, State>();
    
    final List<State> state = new LinkedList<State>();
    
    final Map<String, String> variables =new TreeMap<String, String>();
    
    public State new_state()
    { return new State();
    }
    
    public void add_state(State value)
    { 
      idxStates.put(value.stateName, value);
      if(value.parallelState !=null){
        if(idxStates.get(value.parallelState) ==null){
          State parallel = new State();
          parallel.stateName = value.parallelState;
          parallel.parallelParent = value.enclState;
          parallel.shortdescription = "Parallel state machine inside " + value.enclState;
          idxStates.put(value.parallelState, parallel);
          state.add(parallel);  //add first the parallel self created state.
        }
      }
      state.add(value);
    }
    
    public NameValue new_variable(){ return new NameValue(); }
    
    public void add_variable(NameValue inp){ variables.put(inp.name, inp.value); }
    
  }
  
  
  
  StateMGen(MainCmd_ifc console, Args args)
  {
    this.console = console;
  }
  
  
  void execute(Args args) throws IOException, IllegalAccessException
  {
    ZbnfResultData stateData = parseAndStoreInput(args);
    if(stateData != null){
      prepareStateData(stateData);
      if(args.sFileData !=null){
        FileWriter outData = new FileWriter(args.sFileData);
        OutputDataTree outputterData = new OutputDataTree();
        outputterData.output(0, stateData, outData, false);
        outData.close();
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
        ZGen.Args argsZ = new ZGen.Args();
        //ZGen zbatch = new ZGen(argsZ, console);
        ZGenExecuter generator = new ZGenExecuter(console);
        if(outData !=null) {
          outData.append("===================").append(outArgs.sFileScript);
        }
        Writer out = new FileWriter(fOut);
        generator.setScriptVariable("state", 'O', stateData, true);
        CharSequence sError = ZGen.execute(generator, fileScript, out, args.sCurrdir, true, args.sScriptCheck == null ? null : new File(args.sScriptCheck), console);
        out.close();
        if(sError !=null){
          console.writeError(sError.toString());
        } else {
          console.writeInfoln("SUCCESS outfile: " + fOut.getAbsolutePath());
        }
      }
      if(outData !=null) {
        outData.close();
      }

    } else {
      console.writeInfoln("ERROR");
      
    }
  }
  
  
  void prepareStateData(ZbnfResultData stateData){
    for(State state: stateData.state){
      prepareStateStructure(state, stateData);
    }
    for(State state: stateData.state){
      if(state.trans !=null){
        for(Trans trans: state.trans){
          prepareStateTrans(trans, state, stateData);
        }
      }
    }
  }
  
  
  void XXXprepareStateStructure(State stateP, ZbnfResultData stateData){
    //stateStructure
    State state = stateP;
    if(state.enclState !=null){
      if(state.stateName.equals("Set_UfastIctrl"))
        stop();
      //search top state, enter it.
      List<State> enclStates = new LinkedList<State>();
      State state1 = state;
      enclStates.add(state);
      while(state1.enclState !=null){
        String state1Name = state1.enclState;
        state1 = stateData.idxStates.get(state1Name);
        assert(state1 !=null);
        enclStates.add(state1);
      }
      int states = enclStates.size();
      State enclState1 = enclStates.get(states-1);  //The last in the list is the top state
      if(stateData.topStates.get(enclState1.stateName) == null){
        stateData.topStates.put(enclState1.stateName, enclState1);
      }
      ListIterator<State> iter = enclStates.listIterator(states-1);
      while(iter.hasPrevious()){
        State subState1 = iter.previous();
        if(enclState1.subStates ==null){
          enclState1.subStates = new TreeMap<String, State>(); 
        }
        if(enclState1.subStates.get(subState1.stateName) == null){
          enclState1.subStates.put(subState1.stateName, subState1);
        }
        enclState1 = subState1;
      }
      
    }
  }

  
  void prepareStateStructure(State stateP, ZbnfResultData stateData){
    //stateStructure
    State state = stateP;
      if(state.stateName.equals("Set_UfastIctrl"))
        stop();
      while(state.enclState !=null){
        String enclStateName = state.enclState;
        State enclState = stateData.idxStates.get(enclStateName);
        assert(enclState !=null);
        if(state.parallelState !=null){
          String parallelStateName = state.parallelState;
          State parallelState = stateData.idxStates.get(parallelStateName);
          assert(parallelState !=null);
          if(enclState.parallelStates ==null){
            enclState.parallelStates = new TreeMap<String, State>(); 
          }
          if(enclState.parallelStates.get(parallelState.stateName) == null){
            enclState.parallelStates.put(parallelState.stateName, parallelState);
          }
          if(parallelState.subStates ==null){
            parallelState.subStates = new TreeMap<String, State>(); 
          }
          if(parallelState.subStates.get(state.stateName) == null){
            parallelState.subStates.put(state.stateName, state);
          }
          
        } else {
          if(enclState.subStates ==null){
            enclState.subStates = new TreeMap<String, State>(); 
          }
          if(enclState.subStates.get(state.stateName) == null){
            enclState.subStates.put(state.stateName, state);
          }
        }
        state = enclState;
      }
      if(state.enclState == null){
        if(stateData.topStates.get(state.stateName) == null){
          stateData.topStates.put(state.stateName, state);
        }
          
      }
      
  }

  
  void prepareStateTrans(Trans trans, State state, ZbnfResultData stateData){
    if(trans.dstState == null && trans.subCondition !=null){
      for(Trans subCond: trans.subCondition){
        prepareStateTrans(subCond, state, stateData);
      }
    } else if(trans.dstState != null){
      
      int nrofDstStates = trans.dstState.size();
      @SuppressWarnings("unchecked")
      List<State>[] listDst1 = new LinkedList[nrofDstStates];
      State[] entryStates = new State[nrofDstStates];
      int ix = 0;
      for(ix = 0; ix < listDst1.length; ++ix) {
        listDst1[ix] = new LinkedList<State>();
      }
      State exitState = state;
      boolean found = false;
      while(!found){
        trans.exitStates.add(exitState);
        if(exitState.enclState != null){
          State exitState2 = stateData.idxStates.get(exitState.enclState);
          if(exitState2 == null){
            System.out.println("\nSemantic error: parent state \"" + exitState.enclState + "\" in state \"" + state.stateName + "\"not found. ");
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
            System.out.println("\nSemantic error: destination state \"" + dstStateName + "\" from state \"" + state.stateName + "\" not found. ");
            return;
          }
          do { 
            //check entryStates down to either the exitState or down to to top state.
            //If the exit state is equal any entry enclosing state or both are null (top state), then it is the common state.
            listDst1[ix].add(0, entryStates[ix]);
            if(entryStates[ix].enclState != null){
              State entryState = stateData.idxStates.get(entryStates[ix].enclState);
              if(entryState == null){
                System.out.println("\nSemantic error: parent state \"" + entryStates[ix].enclState + "\" in state \"" + state.stateName + "\"not found. ");
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
        
        for(List<State> listEntries: listDst1){
          DstState dstStateTreeNode = trans.dstStateTree;
          //dstStateTreeNode = dst;
          for(State entryState: listEntries){
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
              entryDst.name = entryState.stateName;
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
