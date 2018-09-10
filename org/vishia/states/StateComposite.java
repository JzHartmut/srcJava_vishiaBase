package org.vishia.states;




import java.util.EventObject;

import org.vishia.util.InfoAppend;

/**A composite state with its own state variable {@link #stateAct}.
 * @author Hartmut Schorrig
 *
 */
public abstract class StateComposite extends StateCompositeFlat implements InfoAppend
{
  /**Version, history and license.
   * <ul>
   * <li>2015-12-20 Hartmut chg: {@link #processEvent(EventObject)} more simple because enclosing non-StateComposite transitions are checked in StateSimple. 
   * <li>2015-11-21 Hartmut chg: {@link #printStateSwitchInfo(StateSimple, EventObject, int)} 
   * <li>2014-11-09 Hartmut chg: Capability of StateParallel contained here, class StateParallel removed: 
   *   It is possible to have a StateComposite with its own sub states, but a second or more parallel states
   *   in an own composite, which is yet the class StateAddParallel. It is {@link StateParallel}. More simple, more flexibility. 
   * <li>2014-09-28 Hartmut chg: Copied from {@link org.vishia.stateMachine.StateCompositeBase}, changed concept: 
   *   Nested writing of states, less code, using reflection for missing instances and data. 
   * <li>2013-05-11 Hartmut new: It is a {@link EventConsumer} yet. Especially a timer event needs a destination
   *   which is this class.
   * <li>2013-04-27 Hartmut adapt: The {@link #entry(EventMsg2)} and the {@link #entryAction(EventMsg2)} should get the event
   *   from the transition. See {@link StateSimpleBase}.
   * <li>2013-04-13 Hartmut re-engineering: 
   *   <ul>
   *   <li>New method {@link #setDefaultState(StateSimpleBase)}
   *   <li>{@link #entryDefaultState()} is package private now, it regards requirements of {@link StateParallelBase}.
   *   <li>The old override-able method entryDefault() was removed.
   *   <li>The overridden entry() was removed, replaced by #entryComposite, which is called in {@link StateSimpleBase#entry(int)}
   *     if the instance is this type.    
   *   </ul>
   * <li>2013-04-07 Hartmut adap: Event<?,?> with 2 generic parameter
   * <li>2012-09-17 Hartmut improved.
   * <li>2012-08-30 Hartmut created. The experience with that concept are given since about 2003 in C-language.
   * </ul>
   * <br><br>
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
   * 
   */
  public static final String version = "2015-12-20";

  protected int maxStateSwitchesInLoop = 1000;
  
  /**Stores whether this composite state is active. Note that the #stateAct is set as history state
   * as well the state is not active. This bit is set to false too if the current state is exited
   * and a new state is not entered yet, while temporary transition processing. It helps to prevent double
   * execution of the {@link #exitTheState()} routine if exit of the enclosing state is processed.*/
  boolean isActive;
  
  /*package private*/ StateSimple stateAct;
  
  
  /**Only used for the {@link StateMachine#stateTop} 
   * and as special constructor to build a state machine from other data. See org.vishia.stateMGen.StateMGen. 
   */
  public StateComposite(String name, StateMachine stateMachine, StateSimple[] aSubstates){
    super(name, stateMachine, aSubstates);
  };
  
  
  
  
  /**The constructor of any StateComposite checks the class for inner classes which are the states.
   * Each inner class which is instance of {@link StateSimple} is instantiated and stored both in the {@link StateMachine#stateMap} 
   * to find all states by its class.hashCode
   * and in {@link #aSubstates} for debugging only.
   * <br><br>
   * After them {@link #buildStatePathSubstates()} is invoked to store the state path in all states.
   * Then {@link #createTransitionListSubstate(int)} is invoked which checks the transition of all states recursively. 
   * Therewith all necessary data for the state machines's processing are created on construction. 
   * 
   * @see StateMachine#StateMachine()
   * @see StateComposite#buildStatePathSubstates(StateComposite, int)
   * @see StateSimple#buildStatePath(StateComposite)
   * @see StateComposite#createTransitionListSubstate(int)
   * @see StateSimple#createTransitionList()
   */
  public StateComposite() {
    super();
  }
  
  
  


  /**Check whether this composite state has the given state as direct actual sub state
   * @param state Only states of the own composite are advisable. It is checked in compile time
   *   with the strong type check with the generic type of state. 
   * @return true if it is in state.
   */
  public final boolean isInState(StateSimple state){ 
    return isInState()             //this state is active too, or it is the top state.
        && (  stateAct == state   //the given state is the active.
           || aSubstates == null  //a poor parallel state container.
           );   
  }
  
  
  
  
  /**This method is called from {@link StateSimple.Trans#doEntry(EventObject)} if the history state should be entered and all history states
   * should be entered in sub states.
   * @return or-relation of all return values of {@link StateSimple#entry(EventObject)}, especially bit {@link StateSimpleBase#mRunToComplete}.
   */
  protected final int entryDeepHistory(EventObject ev){
    isActive = true;
    StateSimple stateActHistory = stateAct; 
    if(stateActHistory == null) {
      stateActHistory = stateAct = stateDefault;
    }
    return doEntryDeepHistory(stateActHistory, ev);
  }
  
  
  
  /**Execution routine of enter the deep history.
   * <ul>
   * <li>Enters all states in the statePath from the {@link StateSimple#ixCompositeState_inStatePath}.
   * <li>If the state is a StateComposite the {@link #entryDeepHistory(EventObject)} will be called recursively.
   * <li>If the state is a StateParallel {@link #entryDeepHistory(EventObject)} will be called for all parallel states.
   * </ul>
   * @param state The stored current state used as history.
   * @param ev
   * @return see {@link #entryDeepHistory(EventObject)}
   */
  private final static int doEntryDeepHistory(StateSimple state, EventObject ev) {
    int cont = 0;
    for(int ix = state.ixCompositeState_inStatePath+1; ix < state.statePath.length; ++ix) {
      StateSimple stateEntry = state.statePath[ix]; 
      cont |= stateEntry.entryTheState(ev,true);   //entry in all states in statePath till state itself.
    }
    if(state instanceof StateComposite) {  
      //if the current state is a composite too:
      cont |= ((StateComposite)state).entryDeepHistory(ev);       //The state will be entered in the recursive call.
    } 
    else if(state instanceof StateParallel) {  
      //If the current state is a StateParallel, enter all parallels with deep history:
      for(StateSimple stateP : ((StateParallel)state).aParallelstates) {
        cont |= stateP.entryTheState(ev,true);   //entry in the parallel state bough, composite or simple
        if(stateP instanceof StateComposite) {
          cont |= ((StateComposite)stateP).entryDeepHistory(ev);
        }
      }
    }
    return cont;
  }
  
  
  /**This method should be called from outside if the history state should be entered but the default state of any
   * sub state should be entered.
   * @param isProcessed The bit {@link StateSimpleBase#mEventConsumed} is supplied to return it.
   * @return isProcessed, maybe the additional bits {@link StateSimpleBase#mRunToComplete} is set by user.
   */
  public final int entryShallowHistory(EventObject ev){
    isActive = true;
    StateSimple stateActHistory = stateAct;  //save it
    int cont = entryTheState(ev, true);                  //entry in this state, remark: may be overridden, sets the stateAct to null
    cont = stateActHistory.entryTheState(ev, true);             //entry in the history sub state.
    return cont;
  }
  
  

  
  /**Processes the event for the states of this composite state.
   * First the event is applied to the own (inner) states invoking either its {@link StateComposite#processEvent(EventObject)}
   * which calls this method recursively.
   * <br><br>
   * If this method returns with {@link StateSimpleBase#mRunToComplete} that invocation is repeated in a loop, to call
   * the transition of the new state too. But if the event was consumed by the last invocation, it is not supplied again
   * in the loop, the event parameter is set to null instead. It means only conditional transitions are possible.
   * This behavior is conform with the UML definition.
   * <br><br>
   * If the loop would not terminate because any state have a valid transition and the state machine switches forever,
   * the loop is terminated with an exception for a number of {@link #maxStateSwitchesInLoop}. This exception occurs
   * if the user stateMachine conditions are faulty only.
   * <br><br>
   * At least the {@link #_checkTransitions(EventObject)} of this state is invoked but only if the event is not processed
   * or the state contains non-event triggered (conditional) transitions. Last one is signified by the {@link #modeTrans}.
   * <br><br>
   * This method overrides the {@link StateSimple#processEvent(EventObject)} which is overridden by {@link StateParallel#processEvent(EventObject)}
   * too to provide one method for event processing for all state kinds with the necessary different handling.
   * <br><br>
   * <b>Return bits</b>:
   * <ul>
   * <li> {@link StateSimpleBase#mEventConsumed} as result of the inside called {@link #_checkTransitions(EventObject)}
   *   to remove the event for further usage in an enclosing state processing.
   * <li> {@link StateSimpleBase#mRunToComplete} is not delivered because it has no sense outside.
   * <li> {@link StateSimpleBase#mRunToComplete} 
   * </ul>  
   * 
   * 
   * @param evP The event.
   * @return Some bits  
   */
  /*package private*/ @Override int processEvent(final EventObject evP){  //NOTE: should be protected.
    int cont = 0;
    EventObject evTrans = evP;
    int catastrophicalCount =  maxStateSwitchesInLoop;
    StateSimple stateActPrev;
    //do { //Run to complete loop of a transition of a flat composite state, repeat if the state of this is change by firing a superior transition.
      int contLoop = 0;
      //
      //
      do { //Run to complete loop
        contLoop &= ~mRunToComplete;  //only this is checked for any state transition. Left all other bits especially mEventConsumed.
        if(stateAct == null){
          int trans = entryDefaultState();  //regards also Parallel states.
          if(stateMachine.debugState && (trans & (mStateEntered | mStateLeaved)) !=0) { printStateSwitchInfo(null, evTrans, contLoop); }
          contLoop |= trans & ~(mStateEntered | mStateLeaved);  //regards also Parallel states.
        } 
        StateSimple statePrev = stateAct;
        //==>>
        int trans = stateAct.processEvent(evTrans);
        if(stateMachine.debugState && statePrev instanceof StateSimple && (trans & (mStateEntered | mStateLeaved)) !=0) { printStateSwitchInfo(statePrev, evTrans, trans); }
        if((trans & StateSimple.mEventConsumed) != 0){
          evTrans = null;
        }
        contLoop |= trans & ~(mStateEntered | mStateLeaved);
        //
        if(catastrophicalCount == 4) {
          catastrophicalCount = 3;  //set break point! to debug the loop
        }
      } while(isActive   //leave the loop if this composite state is exited.
          && (contLoop & mRunToComplete) !=0    //loop if runToComplete-bit is set, the new state should be checked.
          && --catastrophicalCount >=0
          );
      //
      //
      if(catastrophicalCount <0) {
        throw new RuntimeException("unterminated loop in state switches");
      }
      cont |= contLoop;
      /*
      //check the transitions of this StateComposite and all superior StateCompositeFlat too!
      StateSimple stateEncl1 = stateAct;
      stateActPrev = stateAct;
      while( stateActPrev == stateAct   //repeat so long the stateAct was not changed if a composite transition fires. 
        && (stateEncl1 = stateEncl1.enclState) instanceof StateCompositeFlat  //repeat for all enclosing flat composite states of this.
        ) {
        if(evTrans !=null || (stateEncl1.modeTrans & StateSimple.mRunToComplete) !=0 ) { //state has only conditional transitions
          //==>>
          int trans = stateEncl1._checkTransitions(evTrans);
          if(stateMachine.debugState && stateActPrev instanceof StateSimple && (trans & (mStateEntered | mStateLeaved)) !=0) { printStateSwitchInfo(stateActPrev, evTrans, trans); }
          if((trans & StateSimple.mEventConsumed) != 0){
            evTrans = null;
          }
        }
      }
    } while(stateActPrev != stateAct    //repeat it for run to completion if the stateAct was changed by an transition of the composite.
        && --catastrophicalCount >=0 );
    */
    //Process to leave this state.
    //
    //evTrans: If the event was consumed in any inner transition, it is not present for the own transitions. UML-conform.
    //
    if( ( evTrans != null   //evTrans is null if it was consumed in inner transitions. 
        || (modeTrans & StateSimple.mRunToComplete) !=0  //state has only conditional transitions
        ) && isActive
      ){
      //process the own transition. Do it after processing the inner state (omg.org)
      //and only if either an event is present or the state has only conditional transitions.
      StateSimple statePrev = stateAct;
      int trans = _checkTransitions(evTrans); 
      if(stateMachine.debugState && (trans & (mStateEntered | mStateLeaved)) !=0) { printStateSwitchInfo(statePrev, evTrans, trans); }
      cont |= trans & ~(mStateEntered | mStateLeaved);
    }
    return cont;  //runToComplete.bit may be set from an inner state transition too.
  }

  
  
  
  
  
  
  
  private void printStateSwitchInfo(StateSimple statePrev, EventObject evTrans, int cont) {
    //DateOrder date = new DateOrder();
    //Thread currThread = Thread.currentThread();
    //String sThread = currThread.getName();
    String sStatePrev = statePrev !=null ? statePrev.stateId : "INIT";
    //String sActiveState = getActiveState();
    StringBuilder uStateNext = new StringBuilder();
    //if(stateAct == null){ uStateNext.append("--inactive--"); }
    StateComposite parent = this;
    while(parent !=null && !parent.isActive) {
      parent = parent.compositeState !=null ? parent.compositeState : parent.enclState.compositeState;  //use enclState on stateParallel
    }
    //if(!isActive){ uStateNext.append("--inactive--"); }
    //else 
    if(parent ==null) {
      uStateNext.append("--StateMachine inactive--");
    } else  {
      StateSimple stateAct1 = parent.stateAct;
      uStateNext.append(stateAct1.stateId);
      if(stateAct1 instanceof StateParallel) {
        StateParallel stateP = (StateParallel)stateAct1;
        String sep = "[";
        for(StateSimple stateP1 : stateP.aParallelstates) {
          uStateNext.append(sep);
          assembleDstState(uStateNext, stateP1);
          sep = " || ";
        }
        uStateNext.append(']');
      }
      else {
        assembleDstState(uStateNext, stateAct1);
      }
    }
    if(!isActive){
      System.out.println("StateComposite -  left ; " + sStatePrev + ";==>" + uStateNext + "; event=" + evTrans + ";");
    } 
    else if((cont & StateSimple.mEventConsumed)!=0) {  //statePrev != stateAct){  //from the same in the same state!
      System.out.println("StateComposite - event ;" + sStatePrev + ";==>"  + uStateNext + "; event=" + evTrans + ";");
    } else if(evTrans !=null){ 
      System.out.println("StateComposite - nu.ev.;" + sStatePrev + ";==>"  + uStateNext + "; not used event=" + evTrans + ";");
    } else { 
      System.out.println("StateComposite - runToC;" + sStatePrev + ";==>"  + uStateNext + ";");
    }
    
  }

  
  private void assembleDstState(StringBuilder uStateNext, StateSimple stateAct1) {
    while(stateAct1 instanceof StateComposite) {
      stateAct1 = ((StateComposite)stateAct1).stateAct;
      if(stateAct1 !=null) { 
        uStateNext.append('.').append(stateAct1.stateId);
      }
    }
  }


  /**Exits first the actual sub state (and that exits its actual sub state), after them this state is exited.
   * It calls {@link StateSimple#exitTheState()} which invokes the maybe application overridden {@link StateSimple#exit()} routine.
   */
  //@Override 
  void XXXexitTheState(int level){ 
    if(isActive && stateAct !=null){
      stateAct.exitTheState(level);    //recursively call for all inner states which are yet active.
      isActive = false; //NOTE that StateSimpleBase.exit() sets isActive to false already. It is done twice.
    }
    //super.exitTheState(level);  //call routine of StateSimple
  }

  
  
  @Override public CharSequence getStatePath(){
    StringBuilder uPath = new StringBuilder(120);
    StateSimple state = this;
    while((state = state.enclState) !=null){
      uPath.append(':').append(state.stateId);
    }
    state = this;
    //*
    do{
      uPath.append('.').append(state.stateId);
      if(state instanceof StateComposite){
        state = ((StateComposite)state).stateAct;
      } else { state = null; }
    } while(state !=null);
    //*/
    return uPath;
  }
  
  
  
  @Override public CharSequence infoAppend(StringBuilder u) {
    if(u == null) { u = new StringBuilder(200); }
    String separator = "";
    u.append(stateId);
    if(isActive) {
      if(stateAct !=null) {
        u.append(".");
        stateAct.infoAppend(u);
      } else {
        u.append(" - stateAct = null");
      }
    } else {
      u.append(" - inactive");
    }
    separator = "-"; 
    return u;
    
  }
  
  
  @Override public String toString(){ return infoAppend(null).toString(); }


}
