package org.vishia.states;

import java.io.Closeable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.HashMap;
import java.util.List;

import org.vishia.event.EventConsumer;
import org.vishia.event.EventSource;
import org.vishia.event.EventTimerThread;
import org.vishia.event.EventWithDst;
import org.vishia.util.DataAccess;
import org.vishia.util.DataShow;
import org.vishia.util.Java4C;
import org.vishia.util.InfoAppend;

/**
 * To build a state machine you should build a maybe inner class derived from {@link org.vishia.states.StateMachine}. This class builds the frame for all states.
 * Any state should be defined as inner class derived from either ...or  
 * <ul>
 * <li>{@link org.vishia.states.StateSimple}: A simple state with transitions, an optional entry- and an exit-action.
 * <li>{@link org.vishia.states.StateComposite}: It contains more as one sub states.
 * <li>{@link org.vishia.states.StateParallel}: It contains more as one {@link org.vishia.states.StateComposite} for parallel behavior: 
 *   Any StateComposite contains a sub statemachine which works parallel if this state is active. 
 * </ul>
 * See example pattern {@link org.vishia.states.example.StateExampleSimple}.
 * <br><br>
 * Any state can contain an entry- and an exit action which can be build either ... or with:
 * <ul>
 * <li>Overriding the method {@link org.vishia.states.StateSimple#entry(EventObject)} respectively {@link StateSimple#exit()}.
 * <li>Set the association {@link org.vishia.states.StateSimple#entry} respectively {@link org.vishia.states.StateSimple#exit} 
 * with an Implementation of {@link org.vishia.states.StateAction} which can be located in any other class, 
 *   usual as inner anonymous class. In that cases the entry and exit can use data from any other class.
 * </ul>  
 * <br><br>
 * Any state can contain some transitions which are variables of type {@link org.vishia.states.StateSimple.Trans}.
 * If a transition should contain an action the transition variable should be an anonymous inner derived class 
 * of {@link org.vishia.states.StateSimple.Trans} which overrides the {@link org.vishia.states.StateSimple.Trans#action(EventObject)}.
 * The other possibility is assignment of action code with {@link org.vishia.states.StateSimple.Trans#setAction(StateAction)}
 * </ul>
 * Any state should override the method {@link org.vishia.states.StateSimple#checkTrans(EventObject)}. This method checks all conditions
 * which can fire any transition and returns the selected transition instance or null if nothing should fire. This method should not execute
 * actions. It should only test the condition and select the transition. The actions are done in the calling environment of this method,
 * lastly if {@link #processEvent(EventObject)} of the whole statemachine is invoked.
 * <br><br>
 * On construction of the whole {@link StateMachine} all state classes are instantiated and listed in the StateMachine. 
 * All transitions are evaluated and transformed in a unified list of transition objects.
 * For all transitions the necessary exitState()- and entryState() operations are detected and stored, so that complex state switches
 * are executed correctly. The designation of destination states in the written source code uses the class names of the states. 
 * The transition evaluation process on construction searches and lists the instances of the states.
 * <br><br>
 * A state instance can be gotten with {@link org.vishia.states.StateMachine#getState(Class)}
 * <br><br>
 * The {@link StateMachine} can be quested whether a state is active yet: {@link org.vishia.states.StateMachine#isInState(Class)} 
 * or {@link StateMachine#isInState(Class)}
 * <br><br>
 * <br>Creation of a StateMachine with or without a timer and an event queue</b>:<br>
 * One can execute the state machine in 2 modes:
 * <ul>
 * <li>State execution inside a thread which processes events. That thread can process more as one state machine. 
 *   The limit is: If many events can be occured, the execution of all state transitions should be able to done in a limited time. 
 *   The advantage is: All state machines are executed in the same thread.
 *   The transitions are <em>fibers</em> of that thread. They are non-interrupt-able. If more as one state machines work with the same data,
 *   no additional mutexes are necessary.
 * <li>State execution inside the calling thread. That is the more simple variant. 
 *   Events to trigger the state machine are possible but not necessary. The user should have control over mutual exclusion access to data 
 *   if some more threads are used.  
 * </ul>
 * In the first case an instance of {@link EventTimerThread#EventThread(String)} are necessary which is a parameter for the constructor
 * {@link #StateMachine(EventTimerThread, EventTimerMng)}.
 * <br><br>
 * If an {@link EventTimerThread} is used an instance of {@link EventTimerMng} is possible and recommended . 
 * The timer manager can manage any number of time orders.
 * It sends an {@link EventTimerMng.TimeEvent} to this StateMachine it the time is expired.
 * <br><br>
 * The state machine can be animated using {@link org.vishia.states.StateMachine#applyEvent(EventMsg2)} with or without events
 * by given a null argument, for example cyclically if an {@link EventTimerThread} is not used.
 * <br><br>
 * 
 * To see how transitions and timeouts should be written see on {@link StateSimple.Trans} and {@link StateSimple.Timeout}.
 * 
 * @author hartmut Schorrig
 *
 */
public class StateMachine implements EventConsumer, InfoAppend, Closeable
{
  
  /**Version, history and license.
   * <ul>
   * <li>2015-11-03 Hartmut chg: renaming topState in {@link #stateTop}, more unique
   * <li>2015-01-11 Hartmut chg:  
   * <li>2014-12-30 Hartmut chg: test, gardening 
   * <li>2014-10-12 Hartmut chg: Not does not inherit from {@link StateComposite}, instance of {@link #stateTop}. 
   * <li>2014-09-28 Hartmut chg: Copied from org.vishia.stateMachine.TopState, changed concept: 
   *   Nested writing of states, less code, using reflection for missing instances and data. 
   * <li>2013-04-07 Hartmut adapt: Event<?,?> with 2 generic parameter
   * <li>2012-09-17 Hartmut improved.
   * <li>2012-08-30 Hartmut created. The experience with that concept are given since about 2001 in C-language and Java.
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
  public static final String version = "2014-10-14";

  
  /**If set true, then any state transition is logged with System.out.printf("..."). One can use the 
   * {@link org.vishia.msgDispatch.MsgRedirectConsole} to use a proper log system. 
   */
  public boolean debugState = false;
  public boolean debugTrans = false;
  public boolean debugEntryExit = false;
  
  /**True then permits exceptions in one state transition. send a log output to System.err but check other transitions. */
  public boolean permitException;
  
  /**Aggregation to the used event queue or the thread for this statemachine.
   * It can be used to send events to from outer.
   * Note: Do not send events from any other thread to this directly.
   * The state machine should processed in only one thread.
   * But this aggregation may be null if the state machine will be processed in a users thread.
   */
  final EventTimerThread theThread;
  
  
  final EventWithDst triggerEvent;
  
  
  /**Aggregation to the used timer for time events. See {@link #addTimeOrder(long)}.
   * It may be null if queued time events are not necessary for this.
   */
  //final EventTimerMng theTimer;
  
  //EventTimerMng.TimeEvent evTime;
  
  /**The state which presents the whole state machine. It is not a state of the user, it is the main composite state.
   * 
   */
  protected final StateCompositeTop stateTop; 
  
  /**Map of all states defined as inner classes of the derived class, filled with reflection. 
   * This map contains hash codes as key. Because the hash code is different in execution in comparison to another execution,
   * the key values are not shown using {@link DataShow} to assure comparability. */
  @DataShow.ExcludeShowContent HashMap<Integer, StateSimple> stateMap = new HashMap<Integer, StateSimple>();
  
  /**List of all states parallel to the #stateMap for code generation, filled with reflection. */
  List<StateSimple> stateList = new ArrayList<StateSimple>();
  
  private final String name;

  protected static class StateCompositeTop extends StateComposite
  {
    StateCompositeTop(StateMachine stateMachine, StateSimple[] aSubstates) { super("top", stateMachine, aSubstates); } 
    
    
    /**Prepares the top state if it is created from a outside parsed State machine (StateMGen).
     * 
     */
    public void prepare() {
      buildStatePathSubstates(null,0);  //for all states recursively
      //the transitions are added already. Don't invoke createTransitionListSubstate(0); 
      prepareTransitionsSubstate(0);
    }
  }
  
  public List<StateSimple> stateList(){ return stateList; }
  
  /**Creates a state machine which is executed directly by {@link #processEvent}. {@link StateSimple.Timeout} is not possible.
   * 
   */
  public StateMachine(String name) { this(name, null);}
  
  /**Constructs a state machine with a given thread and a given timer manager.
   * The constructor of the whole stateMachine does the same as the {@link StateComposite#StateComposite()}: 
   * It checks the class for inner classes which are the states.
   * Each inner class which is instance of {@link StateSimple} is instantiated and stored both in the {@link #stateMap} to find all states by its class.hashCode
   * and in the {@link #stateTop} {@link StateComposite#aSubstates} for debugging only.
   * <br><br>
   * After them {@link StateComposite#buildStatePathSubstates()} is invoked for the topstate and recursively for all states 
   * to store the state path in all states.
   * Then {@link StateComposite#createTransitionListSubstate(int)} is invoked for the {@link #stateTop} 
   * which checks the transition of all states recursively. Therewith all necessary data for the state machines's processing
   * are created on construction. 
   * 
   * @see StateComposite#StateComposite()
   * @see StateComposite#buildStatePathSubstates(StateComposite, int)
   * @see StateSimple#buildStatePath(StateComposite)
   * @see StateComposite#createTransitionListSubstate(int)
   * @see StateSimple#createTransitionList()
   * 
   * @param thread if given all events are stored in the thread's event queue, the state machine is executed only in that thread.
   * @param if given timer events can be created. 
   */
  public StateMachine(String name, EventTimerThread thread) //, EventTimerMng timer)
  { this.name = name;
    this.theThread = thread;
    //this.theTimer = timer;
    if(thread !=null){
      triggerEvent = new EventWithDst(null, this, theThread);
    } else {
      triggerEvent = null;
    }
    final StateSimple[] aSubstates;
    Class<?>[] innerClasses = this.getClass().getDeclaredClasses();
    if(innerClasses.length ==0) throw new IllegalArgumentException("The StateMachine should have inner classes which are the states.");  //expected.
    aSubstates = new StateSimple[innerClasses.length];  //assume that all inner classes are states. Elsewhere the rest of elements are left null.
    stateTop = new StateCompositeTop(this, aSubstates);
    int ixSubstates = -1;
    try{
      for(Class<?> clazz1: innerClasses) {
        if(DataAccess.isOrExtends(clazz1, StateSimple.class)) {
          Constructor<?>[] ctor1 = clazz1.getDeclaredConstructors();
          ctor1[0].setAccessible(true);
          Object oState = ctor1[0].newInstance(this);
          StateSimple state = (StateSimple)oState;
          aSubstates[++ixSubstates] = state;
          state.stateId = clazz1.getSimpleName();
          state.stateMachine = this;
          state.enclState = stateTop;
          int idState = clazz1.hashCode();
          this.stateMap.put(idState, state);
          this.stateList.add(state);
          try { 
            clazz1.getDeclaredField("isDefault");
            if(stateTop.stateDefault != null){ 
              throw new IllegalArgumentException("StateMachine - more as one default state in;" + stateTop.stateId); 
            }
            stateTop.stateDefault = state;  //The first state is the default one.
          } catch(NoSuchFieldException exc){} //empty!
        }
      }
      if(stateTop.stateDefault == null){ 
        throw new IllegalArgumentException("StateMachine - a default state is necessary. Define \"final boolean isDefault = true\" in one of an inner class State;" + stateTop.stateId); 
      }
      //after construction of all subStates: complete.
      stateTop.stateId = "StateTop";
      stateTop.buildStatePathSubstates(null,0);  //for all states recursively
      stateTop.createTransitionListSubstate(0);
      stateTop.prepareTransitionsSubstate(0);
    } catch(InvocationTargetException exc){
      Throwable exc1 = exc.getCause();
      if(exc1 !=null) throw new RuntimeException(exc1);
    } catch(Exception exc){ 
      throw new RuntimeException(exc);
    }
  }

  
  
  /**Special constructor for StateMGen, with given topState, without reflection analysis. 
   * @param stateTop
   */
  protected StateMachine(StateSimple[] aSubstates) {
    this.name = "StateMachine";
    this.stateTop = new StateCompositeTop(this, aSubstates);
    //theTimer = null;
    triggerEvent = null;
    theThread = null;
  }
  
  

  /**This method can be overridden if applying an event should be debugged
   * or for example logged in the derived class. It should be done with the pattern:
   * <pre>
   * (at)Override public int eventToTopDebug(EventObject ev) {
   *   //any statements ...
   *   return super.eventToTopDebug(ev);
   * }
   * </pre>
   * The method is invoked if the event is really processed, not only stored in a queue.
   * it is applied to the {@link #stateTop} and in that way to all current states and their transitions.
   * @param ev The event from user or from queue, maybe null.
   * @return the return value of {@link StateComposite#processEvent}
   */
  protected int eventDebug(EventObject ev) {
    return stateTop.processEvent(ev); 
  }

  /**Triggers a run cycle for the statemachine in the execution thread
   * or runs the state machine immediately if the execution thread is this thread or the execution thread is not given.
   * @param source maybe null
   * @return true if the execution was done immediately, false if the execution is done in the state machine's thread.
   */
  public boolean triggerRun(EventSource source){
    if(theThread == null) {
      processEvent(null);   //runs immediately if theThread ==null, then triggerEvent == null.
      return true;
    } else {
      if(triggerEvent.occupy(source, false)){
        triggerEvent.sendEvent();
      }
      //else: if it is occupied, then it is in processing or in the queue, do nothing. Don't disturb.
      return false;
    }
  }
  
  

  /**Gets the state instance to the State class inside this state machine.
   * The state instance can be used to ask {@link StateSimple#isInState()} or for some statistically checks
   * like {@link StateSimple#dateLastEntry} etc. 
   * @param stateClass write <code>getState(MyStateMachine.StateClass.InnerStateClass.class)</code>
   * @return null if the stateClass is faulty.
   */
  public StateSimple getState(Class<? extends StateSimple> stateClass) {
    int id = stateClass.hashCode();
    StateSimple state = stateMap.get(id);
    return state;
  }


  /**Checks whether the given state is the active one. 
   * @param stateClass The hashcode of the class is used internally to check whether this is the active state.
   * @return true if the given state is active yet.
   */
  public boolean isInState(Class<? extends StateSimple> stateClass){
    int id = stateClass.hashCode();
    StateSimple state = stateMap.get(id);
    if(state == null) throw new IllegalArgumentException("not a state class: " + stateClass.getCanonicalName());
    return state.isInState();
  }


  
  /**Applies an event to this state machine. This method is invoked from {@link EventWithDst#sendEvent()} if this class is given
   * as {@link EventConsumer}. If the statemachine is aggregated with a {@link EventTimerThread} 
   * by constructor {@link #StateMachine(String, EventTimerThread)}
   * and this routine is invoked from another thread then the event will be stored in {@link #theThread}.
   * If the state machine is not aggregated with a EventTimerThread then the execution is done.
   * Then the {@link #eventDebug(EventObject)} is invoked.
   * <br><br>
   * This method can be called by the user instead {@link EventWithDst#sendEvent()} especially if the state machine should be executed
   * in the users thread and without events.  
   * This method is invoked from the {@link EventTimerThread} if the event queue is processed there. 
   */
  @Override public int processEvent(EventObject ev)
  { if(theThread == null || theThread.isCurrentThread()) {
      return eventDebug(ev); 
    } else if(ev !=null){
      if(ev instanceof EventWithDst){
        EventWithDst ev1 = (EventWithDst)ev;
        //ev1.donotRelinquish();  
        ev1.setDst(this);  //only this may be the destination of the event.
      }
      theThread.storeEvent(ev);
      return mEventConsumed         //because it should not applied to other states in the Run-to-complete cycle. 
           | mEventDonotRelinquish; //because it is stored here. Relinquishes after dequeuing!
    }
    else return 0;  //null argument.
  }
  
    
  @Override public CharSequence infoAppend(StringBuilder u){
    if(u == null){ u = new StringBuilder(200); }
    u.append(name).append(':');
    stateTop.infoAppend(u);  //fills the buffer with all aktive sub states.
    u.append("; ");
    if(theThread !=null){
      theThread.infoAppend(u);
    }
    return u;
  }
  
  
  @Override public void close() {
    this.theThread.close();
  }

  /**Shows the name of the Statemachine and all active states.
   * @see java.lang.Object#toString()
   */
  @Override public String toString() {
    return infoAppend(null).toString();
  }

}
