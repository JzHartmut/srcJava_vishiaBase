package org.vishia.states;

import java.lang.reflect.Constructor;
import java.util.EventObject;
import java.util.LinkedList;
import java.util.List;

import org.vishia.util.DataAccess;

/**Base class for a state which contains more as one {@link StateComposite} which are executed parallel if this state is active.
 * 
 * @author Hartmut Schorrig
 *
 */
public abstract class StateParallel extends StateSimple
{
  
  /**Version, history and license.
   * <ul>
   * <li>2015-12-20 Hartmut chg: If the parallel state was left by a transition switch, another transition in a parallel bough cannot fire. 
   * <li>2015-02-10 Hartmut chg: A {@link TransJoin} have to be quest in the {@link #checkTrans(EventObject)} routine.
   *   The transition after the join bar can have a condition or event trigger, which is to quest in the checkTrans.
   *   The automatically transition is removed, because it is not explicitly and cannot have conditions.
   *   Applications should be adapted.  
   * <li>2015-02-10 Hartmut chg: Up to now this is the container for parallel {@link StateComposite}, not one of the parallel composites.
   *   It is more simple and logical.
   * <li>2014-09-28 Hartmut chg: Copied from {@link org.vishia.stateMachine.StateAdditionalParallelBase}, changed concept: 
   *   Nested writing of states, less code, using reflection for missing instances and data. 
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
   * <li> But the LPGL ist not appropriate for a whole software product,
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
  
  
  
  /**Array of all composite states which are executed parallel if this state is active.
   */
  final StateSimple[] aParallelstates;
  
  /**Special constructor to build a state machine from other data. See org.vishia.stateMGen.StateMGen. */
  public StateParallel(String stateName, StateMachine stateMachine, StateSimple[] aParallelstates){
    this.aParallelstates = aParallelstates;
    this.stateMachine = stateMachine;
    this.stateId = stateName;
  };
  
  
  
  
  /**The constructor of any StateParallel checks the class for inner classes which are the parallel states.
   * Each inner class which is instance of {@link StateComposite} is instantiated and stored both in the {@link StateMachine#stateMap} 
   * to find all states by its class.hashCode
   * and in {@link #aParallelstates} for debugging only.
   * <br><br>
   * After them {@link #buildStatePathSubstates()} is invoked to store the state path in all states.
   * Then {@link #createTransitionListSubstate(int)} is invoked which checks the transition of all states recursively. 
   * Therewith all necessary data for the state machines's processing are created on construction. 
   * 
   * @see StateMachine#StateMachine()
   * @see StateParallel#buildStatePathSubstates(StateComposite, int)
   * @see StateSimple#buildStatePath(StateComposite)
   * @see StateParallel#createTransitionListSubstate(int)
   * @see StateSimple#createTransitionList()
   */
  public StateParallel() {
    super();
    Class<?> clazz = this.getClass();
    Object env = this;
    do {
      Class<?> clazzTop2 = env.getClass();
      if(DataAccess.isOrExtends(clazzTop2, StateMachine.class)){
        this.stateMachine = (StateMachine)env;
        //firstly create the stateMap. Note: This is called in constructor of the StateTop-superclass.
      } else {
        env = DataAccess.getEnclosingInstance(env);
      }
    } while(stateMachine == null && env !=null);
    //
    Class<?>[] innerClasses = clazz.getDeclaredClasses();
    if(innerClasses.length >0) {  //it is a composite state.
      List<StateSimple> listParallelstates = null;
      try{
        for(Class<?> clazz1: innerClasses) {  //check all inner classes
          if(DataAccess.isOrExtends(clazz1, StateSimple.class)) {
            Constructor<?>[] ctor1 = clazz1.getDeclaredConstructors();
            //assert only one constructor, the default one.
            ctor1[0].setAccessible(true);
            final Object oState = ctor1[0].newInstance(this);   //creates the instance and all sub states of a maybe StateComposite or a StateSimple.
            //Note that the inner states are processed already in the yet called constructor.
            final StateSimple state = (StateSimple)oState;
            if(listParallelstates ==null) { listParallelstates = new LinkedList<StateSimple>(); }
            listParallelstates.add(state);
            state.stateId = clazz1.getSimpleName();
            state.stateMachine = this.stateMachine;
            state.enclState = this;           
            int idState = clazz1.hashCode();
            stateMachine.stateMap.put(idState, state);
            stateMachine.stateList.add(state);
          }
        }
      } catch(Exception exc){
        throw new RuntimeException(exc);
      }
      if(listParallelstates !=null ){
        this.aParallelstates = listParallelstates.toArray(new StateSimple[listParallelstates.size()]); 
      } else {
        this.aParallelstates = null;
      }
    } else { //no inner states
      this.aParallelstates = null;
    }
  }
  
  
  /**Special method to build a state machine from other data. See org.vishia.stateMGen.StateMGen. */
  public void addState(int key, StateSimple state){
    int ix = 0;
    while(ix < aParallelstates.length && aParallelstates[ix] !=null){ ix +=1; } //search next free
    if(ix >= aParallelstates.length) throw new IllegalArgumentException("too many parallel states to add");
    aParallelstates[ix] = state;
    stateMachine.stateMap.put(state.hashCode(), state);
    stateMachine.stateList.add(state);
  }
  
  
  
  /**Sets the path to the state for this and all {@link #aParallelstates}, recursively call.
   * This method is invoked in the constructor of the state machine only one time.
   * @param enclState
   * @param recurs
   */
  @Override void buildStatePathSubstates(StateSimple enclState, int recurs) {
    if(recurs > 1000) throw new IllegalArgumentException("recursion faulty");
    this.buildStatePath(enclState);
    if(aParallelstates !=null) {
      for(StateSimple parallelState: this.aParallelstates){
        parallelState.buildStatePathSubstates(this, recurs +1);
      }
    }
  }
  
  
  /**Create all transition list for this state and all {@link #aParallelstates}, recursively call.
   * This method is invoked in the constructor of the state machine only one time.
   * It is not for application.
   * @param recurs
   */
  @Override void createTransitionListSubstate(int recurs){
    if(recurs > 1000) throw new IllegalArgumentException("recursion faulty, too many subStates; state=" + stateId);
    this.createTransitionList(this, null, 0);  
    if(aParallelstates !=null) {
      for(StateSimple subState: this.aParallelstates){
        subState.createTransitionListSubstate(recurs+1);
      } 
    }
  }
  
  
  
  @Override void prepareTransitionsSubstate(int recurs) {
    if(recurs > 1000) throw new IllegalArgumentException("recursion faulty, too many subStates; state=" + stateId);
    this.prepareTransitions(null, 0);  
    if(aParallelstates !=null) {
      for(StateSimple subState: this.aParallelstates){
        subState.prepareTransitionsSubstate(recurs+1);
      } 
    }
  }
  
  /**This method is used to entry the default state of all parallel composites.  */
  final int XXXentryDefaultState(){ 
    int ret = 0;
    if(aParallelstates !=null) {
      for(StateSimple state: aParallelstates){
        if(state instanceof StateComposite) {
          ret |= ((StateComposite)state).entryDefaultState();
        } else {
          ret |= state.entryTheState(null, false);
        }
      }
    }
    return ret;
  }
  
  
  /**Processes the event for all composite states.
   * First the event is applied to all parallel composite states {@link #aParallelstates} one after another 
   * invoking its {@link StateComposite#processEvent(EventObject)} which calls this method recursively.
   * <br><br>
   * Then the {@link #_checkTransitions(EventObject)} of this state is invoked but only if the event is not processed
   * or the state contains non-event triggered (conditional) transitions. Last one is signified by the {@link #modeTrans}.
   * <br><br>
   * This method overrides the {@link StateSimple#processEvent(EventObject)} which is overridden by {@link StateComposite#processEvent(EventObject)}
   * too to provide one method for event processing for all state kinds with the necessary different handling.
   * 
   * @param evP The event.
   * @return Some bits especially {@link StateSimpleBase#mEventConsumed} as result of the inside called {@link #_checkTransitions(EventObject)}.
   */
  /*package private*/ @Override int processEvent(final EventObject evP){  //NOTE: should be protected.
    int cont = 0;
    EventObject evTrans = evP;
    boolean isLeft = false;
    //Process for all parallel states
    if(aParallelstates !=null) {
      for(StateSimple stateParallel : aParallelstates) {
        cont |= stateParallel.processEvent(evTrans);
        if(!isInState()) {
          //abort applying the event if the parallel state was left by one of the transitions yet. 
          isLeft = true;
          break;
        }
      }
    }
    if((cont & StateSimple.mEventConsumed) != 0){
      evTrans = null;
    }
    if( ( evTrans != null   //evTrans is null if it was consumed in inner transitions. 
        || (modeTrans & StateSimple.mRunToComplete) !=0  //state has only conditional transitions
        ) && !isLeft
        ){
      //process the own transition. Do it after processing the inner state (omg.org)
      //and only if either an event is present or the state has only conditional transitions.
      int trans = _checkTransitions(evTrans); //of this state.
      cont |= trans;
    }
    
    return cont;
  }
  
  
  
  
  /**Exits all actual sub state (and that exits its actual sub state) of all parallel states. After them this state is exited.
   * It calls {@link StateSimple#exitTheState()} which invokes the maybe application overridden {@link StateSimple#exit()} routine.
   */
  //@Override 
  void XXXexitTheState(int level){ 
    if(aParallelstates !=null) {
      for(StateSimple parallelState : aParallelstates) {
        parallelState.exitTheState(parallelState.statePath.length -1); //leave the StateSimple/StateComposite  in StateParallel
      }
    }
    super.exitTheState(level);
  }
  
  
  
  @Override public CharSequence infoAppend(StringBuilder u) {
    if(u == null) { u = new StringBuilder(200); }
    String separator = "";
    u.append(stateId).append(":");
    if(aParallelstates !=null) {
      for(StateSimple stateParallel: aParallelstates){
        u.append(separator);
        stateParallel.infoAppend(u);  //writes -inactive if it is not active.
        separator = "||";
      }
    }
    return u;
    
  }

  @Override public String toString(){ return infoAppend(null).toString(); }

}
