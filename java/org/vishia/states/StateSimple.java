package org.vishia.states;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.EventObject;
import java.util.Map;







//import org.vishia.event.EventMsg2;
import org.vishia.event.EventConsumer;
import org.vishia.event.EventTimeout;
import org.vishia.util.Assert;
import org.vishia.util.DataAccess;
import org.vishia.util.Debugutil;
import org.vishia.util.IndexMultiTable;
import org.vishia.util.InfoAppend;



/**Base class of a State in a State machine. It is the super class of {@link StateComposite} too.
 * The user should build states in a {@link StateMachine} by writing derived classes of this class or of {@link StateComposite}
 * which contains transitions and may override the {@link #entry(EventMsg2)} and {@link #exit()} method: <pre>
 * class MyStateMachine extends StateMachine
 * {
 *   class StateA extends StateSimple  //one  state
 *   {
 *     (add)Override protected int entry(Event<?,?> ev) { ...code of entry... } 
 *     (add)Override protected void exit() { ...code of exit... }
 *     
 *     //Form of a transition as method which is detected by reflection:
 *     Trans checkX_DstState(Event<?,?> ev, Trans trans){
          if(trans == null){
            return new Trans(DstState.class);   //called on startup, programmed the destination state(s)
          }  
          else if(...condition...) {            //Check any condition
            trans.retTrans =  mEventConsumed;  //if an event is used as condition
            trans.doExit();
            ...transition action code...
            trans.doEntry(ev);
            return trans;
          }
          else return null;
        }
      
        //Form of a transition as instance of an derived anonymous class:
        Trans checkY_StateY = new Trans(StateY.class) {
        
          (add)Override protected int check(Event<?, ?> ev) {  //overrides the trans method:
            if(...condition...) { 
              retTrans = mEventConsumed;
              doExit();
              ...transition action code...
              doEntry(ev);
              return retTrans;
            }
            else return 0;
          }
        } 
         
 *   }
 * }
 * </pre>
 * For building a {@link StateComposite} see there.
 * <ul>
 * <li>A state can contains either transition methods with the shown arguments and {@link Trans} as return instance, 
 * <li>or a transition instances as derived anonymous types of Trans 
 * <li>or a class derived from {@link Trans] without an instance. It is not shown in this example, see {@link Trans}.
 * </ul>
 * The decision between this three forms maybe a topic of view in a browser tree of the program structure (Outline view on Eclipse).
 * <br><br> 
 * <b>How does it work</b>:<br> 
 * On startup all of this kinds of transitions are detected by reflection and stored in the private array {@link #aTransitions}.
 * On runtime the transitions are tested in the order of its priority. If any transition returns not 0 or null they do fire.
 * Either the {@link Trans#doExit()}, {@link Trans#doAction(EventObject, int)} and {@link Trans#doEntry(EventMsg2, int)} is programmed
 * in the if-branch of the condition before return, or that methods are executed from the state execution process automatically.
 * Manual programmed - it can be tested (step-debugger) in the users programm. Not manually, therefore automatically executed - 
 * one should set a breakpoint in the user-overridden {@link #exit()}, {@link #entry(EventMsg2)} or {@link Trans#action(EventMsg2)} methods
 * or the debugging step should be done over the package private method {@link #_checkTransitions(EventMsg2)} of this class. 
 * Note that there are private boolean variables {@link Trans#doneExit}, {@link Trans#doneAction} and {@link Trans#doneEntry} 
 * to detect whether that methods are executed already.
 * <br><br>
 * <br>
 * @author Hartmut Schorrig
 *
 */
public abstract class StateSimple implements InfoAppend
{
  
/**Version, history and license.
 * <ul>
 * <li>2015-12-20 Hartmut chg: The deprecated possibilities of definition of methods for transition and check inside Trans are removed yet. 
 *   It was an concept which has not proven.
 * <li>2015-12-20 Hartmut chg: exit: There were some mistakes: If a enclosing state transition was used to exit an inner state, its exit routine
 *   was not invoked, only that from the enclosing state. The strategy is changed: The {@link Trans#exitStates} is not used anymore,
 *   instead the {@link StateComposite#stateAct} is taken with the {@link #statePath()} to invoke the correct exit routines.
 *   See {@link #exitTheState(int)}. 
 * <li>2015-12-20 Hartmut chg: {@link #_checkTransitions(EventObject)} now checks the transition from all enclosing but not composite states, that are {@link StateCompositeFlat}.
 *   That was done before in the {@link StateComposite#processEvent(EventObject)} routine but in a more difficult algorithm. Now for debugging it is more simple. 
 *   The own {@link #checkTrans(EventObject)} is invoked firstly. If not transition is found, the {@link #checkTrans(EventObject)} of all enclosing states till a StateComposite are tested.
 *   The transitions of an enclosing StateComposite are checked in thats {@link StateComposite#processEvent(EventObject)} routine which calls this {@link #_checkTransitions(EventObject)}. 
 * <li>2015-12-20 Hartmut chg: The timeout should be queried in the user's {@link #checkTrans(EventObject)} routine. The automatic check is removed.
 *   Reason: A timeout event is disturbing often for debug. If it cannot be detect or prevented simply, it is adversarial. It is better to have the timeout handling
 *   in the same routine like all other transition conditions. The {@link #isTimeout(EventObject)} routine helps to write simple code for that.
 * <li>2015-12-20 Hartmut chg: Now entry- and exit outputs with {@link StateMachine#debugEntryExit}.
 * <li>2015-11-21 Hartmut chg: The history pseudo state runs yet. Some problems. 
 * <li>2015-02-10 Hartmut chg: A {@link TransJoin} have to be quest in the {@link #checkTrans(EventObject)} routine.
 *   The transition after the join bar can have a condition or event trigger, which is to quest in the checkTrans.
 *   The automatically transition is removed, because it is not explicitly and cannot have conditions.
 *   Applications should be adapted.  
 * <li>2014-11-09 Hartmut new: {@link #setAuxInfo(Object)}, {@link #auxInfo()} used for state machine generation for C/C++
 * <li>2014-09-28 Hartmut chg: Copied from {@link org.vishia.stateMachine.StateSimpleBase}, changed concept: 
 *   Nested writing of states, less code, using reflection for missing instances and data. 
 * <li>2013-05-11 Hartmut chg: Override {@link #exitAction()} instead {@link #exitTheState()}!
 * <li>2013-04-27 Hartmut chg: The {@link #entry(EventMsg2)} and the {@link #entryAction(EventMsg2)} should get the event
 *   from the transition. It needs adaption in users code. The general advantage is: The entry action can use data
 *   from the event. A user algorithm does not need to process the events data only in the transition. A user code
 *   can be executed both in a special transition to a state and as entry action. Both possibilities do not distinguish
 *   in formal possibilities. The second advantage is: If the event is used, it should given to the next entry. If it is
 *   not used, a 'null' should be given. The user need not pay attention in the correct usage of {@link #mEventConsumed} or not.
 * <li>2013-04-27 Hartmut new: {@link #mStateEntered} is returned on any {@link #entry(EventMsg2)}. If the state is not changed,
 *   a 'return 0;' should be written in the transition code. With the new bit especially the debugging can distinguish
 *   a state changed from a non-switching transition.    
 * <li>2013-04-13 Hartmut re-engineering: 
 *   <ul>
 *   <li>The property whether or not there are non-event transitions is set on ctor. It is a property
 *     established in source code, therefore it should be known in runtime after construction already.
 *     The entry method does not need to modified because non-event transitions. It may be better because the
 *     entry method's code should not depend on the trans method's content.  
 *   <li>entry is final, it can be final now. For overwriting the {@link #entryAction()} is given. It is more simple to use.  
 *   <li>The trans method is protected: It should not be called from outside in any case.
 *   </ul>
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
public static final String version = "2015-12-20";

/**Specification of the consumed Bit in return value of a Statemachine's {@link #_checkTransitions(EventMsg2)} or {@link #entry(int)} 
 * method for designation, that the given Event object was not used to switch. The value of this is 0.
 */
public final static int notTransit =0x0;


/**Bit in return value of a Statemachine's {@link #_checkTransitions(EventMsg2)} or entry method for designation, 
 * that the given Event object was used to switch. It is 0x01.
 */
public final static int mEventConsumed = EventConsumer.mEventConsumed; //== 0x1

/**Bit in return value of the {@link #processEvent(EventObject)}
 * for designation, that the given Event object is stored in another queue, therefore it should not relinquished yet.
 * See {@link EventConsumer.mEventDonotRelinquish}
 */
public final static int mEventDonotRelinquish = EventConsumer.mEventDonotRelinquish;

//private final static int mMaskReservedInEventConsumer = EventConsumer.mMaskReservedHere; 

/**Bit in return value of a Statemachine's trans method for designation, 
 * that the given Transition is true. The bit should be set together with {@link #mEventConsumed} if the event forces true.
 * If this bit is set without {@link #mEventConsumed} though an event is given the transition has not tested the event.
 * In that case the event may be used in the following run-to-complete transitions.
 */
public final static int mTransit = 0x20000;


/**Bit in return value of a Statemachine's trans and entry method for designation, 
 * that the given State has entered yet. If this bit is not set, an {@link #entry(EventMsg2)} action is not called.
 * It means a state switch has not occurred. Used for debug.
 */
public final static int mStateEntered = 0x40000;


/**If a composite state is leaved, any other parallel composite states don't processed then. */
public final static int mStateLeaved = 0x80000;


/**Bit in return value of a Statemachine's trans and entry method for designation, 
 * that the given State has non-event-driven transitions, therefore the trans method should be called
 * in the same cycle.
 */
public final static int mRunToComplete =0x100000;

/**Use this designation for {@link Trans#retTrans} to signal that an event is not consumed in {@link #checkTrans(EventObject)}. */
public final static int mEventNotConsumed = 0x200000;

//public final static int mFlatHistory = 0x400000;

//public final static int mDeepHistory = 0x800000;

/**Aggregation to the whole state machine. Note: it cannot be final because it will be set on preparing only. */
protected StateMachine stateMachine;


/**Reference to the enclosing state. With this, the structure of state nesting can be observed in data structures.
 * The State knows its enclosing state to set and check the state identification there. 
 * Note: it cannot be final because it will be set on preparing only. 
 */
protected StateSimple enclState;

/**The state which is the composite state for this. */
protected StateComposite compositeState;

protected int ixCompositeState_inStatePath;

/**Any additional information. Used for special cases. */
private Object auxInfo;

/**Path to this state from the top state. The element [0] is the top state.
 * The last element is the enclosing state.
 */
protected StateSimple[] statePath;

/**The own identification of the state. Note: it cannot be final because it will be set on preparing only. */
protected String stateId;

/**If set, on state entry the timer for timeout is set. */
int millisectimeout;


/**A timeout transition created in the application. */
Timeout transTimeout;


/**If set this state has one or more join transitions with other states.
 * Note that the join transition is noted only on one (the first) of the join states.
 * The other states are tested from here with {@link #isInState()}.
 */
//TransJoin[] transJoins;

/**The timeout event set if necessary. This is a static instance, reused. It is the same instance in all non-parallel states. */
//EventTimerMng.TimeEvent evTimeout;

/**The timeout event for this state stored also in the top state or a {@link StateParallel}. 
 * This reference is null if this state has not a timeout transition. 
 * If this state is the top state or a StateParallel this refers the same instance for all states of this Composite.
 * More as one parallel states have differen timeouts. */
EventTimeout evTimeout;


/**It is either 0 or {@link #mRunToComplete}. Or to the return value of entry. */
protected final int modeTrans;

/**Reference to the data of the class where the statemachine is member off. */
//protected Environment env;

/**Debug helper. This counter counts any time on entry this state. Because it is public, the user can read it 
 * and reset it to 0 for some debug inventions. This variable should never be used for algorithm. */
public int ctEntry;


/**Debug helper. This timstamp is set for System.currentTimeMilliSec() any time on entry this state. 
 * Because it is public, the user can read it for some debug inventions. This variable should never be used for algorithm. */
public long dateLastEntry;

/**Debug helper. This time difference is set any time on exit this state. It is the time in milliseconds stayed in this state.
 * Because it is public, the user can read it for some debug inventions. This variable should never be used for algorithm. */
public long durationLast;


/**Action for entry and exit. It can be set by the code block constructor in a derived class. Left empty if unused. */
protected StateAction entry;

protected Runnable exit;

/**Set to true if the {@link #checkTrans(EventMsg2)} is not overridden. Then check the {@link #aTransitions} -list */
//private boolean bCheckTransitionArray;

/**This array contains all transitions to handle it commonly. 
 * The transitions are given as instances in the derived classes from {@link StateSimple} in a Java written state machine. 
 * The transitions may be given with {@link #addTransition(Trans)} instead in a State machine which is parsed and translated
 * for another language.
 * 
 */
private Trans[] aTransitions;


/**An instance of this class represents a transition of the enclosing SimpleState to one or more destination states.
 * The application should build an instance of Trans maybe as derived anonymous classes of {@link Trans}
 * for each transition, which should be initialized with the destination state classes.
 * <pre>
 *   class MyState_A extends StateSimple
 *   {
 *      Trans cause_State1 = new Trans(MyState1.class);
 *   
 *      Trans trans1_State2 = new Trans(MyState_B.class) {
 *        (at)Override void action(EventObject ev) {
 *          ...any action
 *        } 
 *     }; 
 * </pre>
 * The method {@link #action(EventObject)} in the transition instance is optional.
 * The state should override the method {@link StateSimple#checkTrans(EventObject)} which contains the check of all transition conditions
 * except for given {@link Timeout} and {@link TransJoin}. That transitions are selected without user code only because they are existing
 * as instances.
 * 
 * <br><br> 
 * The method {@link #buildTransitionPath()} is invoked only on startup of the statemachine. 
 * It creates the {@link #exitStates} and {@link #entryStates} -list.
 * <br><br>
 * How does it works:
 * <ul>
 * <li>The derived Trans class or the element of type Trans will be detected by checking the State class content via reflection.
 * <li>The transition instance is stored in the private {@link StateSimple#aTransitions} array.
 * <li>The entry and exit states are completed.
 * </ul>
 * <br><br>
 */
public class Trans
{
  
  /**Return value of the last used transition test. For debugging and for the {@link TransitionMethod}. */
  public int retTrans;
  
  /**All destination classes from constructor. They are not necessary furthermore after {@link #buildTransitionPath()}
   * because they are processed in the {@link #exitStates} and {@link #entryStates} lists.
   */
  final int[] dst;
  
  
  /**If an action is given, the condition should not contain an action itself. The action is executed after the exitState()-operation
   * and before the entryState()-operation. 
   * <br><br>
   * The action can be given by any StateAction instance in any other class. It can be set by construction.
   */
  protected StateAction action;
  
  /**More as one choice can be given if this transition is a choice-transition. 
   * The transition is only fired if at least one choice-condition is true. The {@link #action} of the state and the {@link #action} 
   * of the choice is executed one after another after exit() and before entry() only if the transition is fired.
   * If some choice are given, the choice should contain the destination state. The constructor of this transition have to be empty.
   */
  protected Trans[] choice;
 
  
  /**It is null for a simple transition in a state. It contains the parent transition for a choice, which is an inner transition. */
  Trans parent;
  
  /**All states which's {@link StateSimple#exitTheState()} have to be processed if the transition is fired. */
  StateSimple[] exitStates;
  
  /**Index of the common state for this transition in the {@link StateSimple#statePath} of any source state. It is used to execute the exit()
   * of all nested source states. */
  int ixCommonInStatePath;
  
  /**All states which's {@link StateSimple#entry(EventMsg2)} have to be processed if the transition is fired. 
   * The array is filled by reflection in the constructor of the state class. There the private {@link Trans.BuildTransitionPath#buildEntryStates()}
   * is invoked to fill the array. On {@link Trans#doneEntry} the states in this array are entered.
   * For a simple transition the content of this array is simple, the order of the entry states from the last {@link #exitStates}.
   * For a fork transition to parallel states this array contains all entered states as a parallel fork. See an example:
   * <br><br>
   * Example for 3 branches of a fork:
   * <pre>
   *    statePath  statePath  statePath
   * --+---------+----------+----------   
   * 0  topState   topState   topState
   * 1  StateC     StateC     StateC     
   * 1  StateAp    StateAp    StateAp     StateAp is a StateParallel
   * 2  StateA1    StateBp    StateBp     StateBp is a StateParallel
   * 3  StateA11   StateB1    StateB2
   * 4             StateB12   StateB25
   * 5                        StateB252
   * </pre>
   * The common state may be the StateC because the transition goes from a State inside stateC to the shown 3 destination states. Therefore the {@link #ixInStatePath}
   * is initialized with <code>[2, 2, 2]</code>.
   * <ul>
   * <li>[2]: Now the algorithm enteres <code>stateC</code> found at <code>statePath[2]</code> but only one time because it is the same for all state paths.   
   * <li>[3]: <code>stateA1</code> is entered. After them <code>StateBp</code> but only one time because it is the same twice.
   * <li>[4]: <code>stateA11</code>, then <code>StateB1</code>, then <code>StateB2</code> is entered.
   * <li>[5]: Because the first statePath is ended, nothing is done for that. Then <code>StateB12</code> and <code>StateB25</code> is entered.
   * <li>[6]: At last <code>StateB525</code> is entered.
   * </ul>
   * Another presentation shows:
   * <pre>
   *  0     1        2           3          4      5      6  ix in statePath
   *            +<---Cxy
   *            |        
   * top--->C   +--->Ap --->|--->A1
   *                        |
   *                        |--->Bp--->|--->B1--->B12
   *                                   |--->B2--->B25--->B251
   * </pre>
   * <code>Cxy</code> may be the exit state, <code>C</code> is the common state. The array contains:
   * <pre>
   * Ap, A1, Bp, B1, B2, B12, B25, B251
   * </pre>                              
   * 
   * */
  StateSimple[] entryStates;
  
  /**If set either a flat history entry or a deep history entry should be executed.  */
  //boolean flatHistory, deepHistory;
  
  /**Identification String for the transition, for debug. */
  String transId;
  
  /**Set it to false on start of check this transition. The methods {@link #doEntry(EventMsg2)}, {@link #doAction(Trans, EventMsg2, int)} and {@link #doExit()} sets it to true. 
   * If a {@link #check(EventMsg2)} method has not invoked this methods, they are invoked from the {@link StateSimple#_checkTransitions(EventMsg2)}. */
  boolean doneExit, doneAction, doneEntry;
  
    
  /**This constructor is used to initialize a Transition instance maybe for a derived anonymous class 
   * with null, one or more destination state of this transition
   * or as super constructor for derived classes if a priority should be given. All transitions without priority are lower.
   * This constructor should not be used if a transition class contains {@link #choice}-transitions.
   * @param dst null, one ore more destination classes.
   *   If the dst is null or not given, the transition is a inner-state-transition. If it fires the {@link StateSimple#exit()} and the entry is not executed.
   *   If more as one dst is given this is a fork transition to more as one state, which should be states in parallel composite states.  
   */
  public Trans(Class<?> ...dst){
    if(dst == null || dst.length ==0) this.dst = null;
    else {
      this.dst = new int[dst.length];
      for(int ix = 0; ix < dst.length; ++ix){
        this.dst[ix] = dst[ix].hashCode();     //store the hashcode to find it.
      }
    }
  }
  
  
  
  
  /**This constructor should be used if the destination states are given from an outer algorithm.
   * @param dstKeys
   */
  public Trans(String name, int[] dstKeys){
    this.transId = name;
    this.dst = dstKeys;
  }
  
  
  /**Sets the action to this transition. The action can be implemented in any other class of the application as inner nonstatic class
   * and can be accessed to the data to its environment class. That is the advantage in comparison to a overridden method {@link #action(EventObject)}.
   * This action is used only if the method {@link #action} is not overridden.
   * @param action
   */
  public void setAction(StateAction action){ this.action = action; }

  /**Set the state of transition execution to {@link EventConsumer#mEventConsumed} and return this.
   * This method should be used in {@link StateSimple#checkTrans(EventObject)} if an event is the trigger:
   * <pre>
   *  QOverride public Trans selectTransition(EventObject ev) {
   *   if(ev == expectedEv) return myTrans.eventConsumed();
   *   ...
   * </pre>  
   */
  public Trans eventConsumed(){ retTrans |= mEventConsumed; return this; }
  
  
  
  /**Builds the transition path from given state to all dst states. Called on startup.
   * 
   */
  void buildTransitionPath(){
    if(choice !=null) {
      for(Trans choice1: choice) {
        PrepareTransition d = new PrepareTransition(StateSimple.this, choice1, this.exitStates);
        d.execute();
      }
    } //else {
    PrepareTransition d = new PrepareTransition(StateSimple.this, this, this.exitStates);
    d.execute();
    //}
  }
  

  
  
  
  /**The non-overridden action executes the {@link #action} or returns 0 if it is not given.
   * @param ev event
   * @return especially {@link StateSimple#mRunToComplete} for state control, elsewhere 0. 
   */
  protected void action(EventObject ev) {
    if(action !=null){
      action.exec(ev);  //may set mEventConsumed or
    }
  }
  
  
  
  /**Processes the action of the transition. If it is a choice transition: firstly process the action of the condition before.
   * It is calling recursively for more as one choice.
   * @param parent The transition before this choice or null on State-transition
   * @param ev The given event.
   * @param recurs recursion count. throws IllegalArgumentException if > 20 
   */
  final void doAction(EventObject ev, int recurs) {
    if(recurs > 20) throw new IllegalArgumentException("too many recursions");
    if(parent !=null) {
      parent.doAction(ev, recurs+1);  //recursion to the first parent
    }
    try{ action(ev);  //maybe overridden, default checks whether a StateAction action is given.
    } catch(Exception exc){
      if(stateMachine.permitException){
        StringBuilder u = new StringBuilder(1000);
        u.append("StateSimple trans action exception - ");
        stateMachine.infoAppend(u);
        u.append(";");
        if(ev !=null){ u.append("event: ").append(ev.toString()); }
        CharSequence text = Assert.exceptionInfo(u, exc, 0, 50);
        System.err.append(text);
      } else {
        throw new RuntimeException(exc); //forward it but without need of declaration of throws exception
      }
    }
    doneAction = true;
  }
  
  
  
  /**Executes the exit from this state and all enclosing States to fire the transition.
   * Because this state may be a {@link StateCompositeFlat} the {@link StateComposite#stateAct} of its {@link #compositeState()} will be used
   * to invoke {@link StateSimple#exitTheState(int)} always.
   */
  public final void doExit()
  { 
    retTrans |= mStateLeaved;
    //
    //execute all exit routines from the current state(s)
    StateSimple stateCurr = compositeState.stateAct;  //it may be this if it is a leaf state, if this is a StateCompositeFlat it is the really stateAct.
    //StateSimple stateExitLast = null;
    stateCurr.exitTheState(ixCommonInStatePath+1);
    
    
    /*   
    if(exitStates !=null) {
      for(StateSimple state: exitStates){
        state.exitTheState();
    } }
    */
    doneExit = true;
  }
  
  
  /**Entry in all states for this transition maybe with history entry.
   * This method can be invoked inside an application-specific {@link StateSimple#checkTrans(EventObject)} routine
   * with the selected transition. The routine sets the flag {@link #doneEntry}. If the method is not invoked by the application
   * it will be invoked in the {@link StateSimple#_checkTransitions(EventObject)}. 
   * The states will be entered in the order from outer to inner state
   * and for all parallel states then from outer to inner one in each parallel both, using the {@link #entryStates}.
   * It regards history entry: {@link StateDeepHistory}, {@link StateShallowHistory}.
   * @param ev The data of the event can be used by entry actions.
   */
  public final void doEntry(EventObject ev)
  { 
    if(entryStates !=null) {
      int ixLast = entryStates.length -1;
      int ix = 0; 
      StateSimple entryStateLast = null;
      while(ix < entryStates.length) { 
        StateSimple entryState = entryStates[ix];
        StateSimple history;
        if(entryState instanceof StateDeepHistory) {
          assert(entryStateLast instanceof StateComposite);
          retTrans |= ((StateComposite)entryStateLast).entryDeepHistory(ev);
        } else if(entryState instanceof StateShallowHistory) {
          assert(entryStateLast instanceof StateComposite);
          retTrans |= ((StateComposite)entryStateLast).entryShallowHistory(ev);
        } else {
          retTrans |= entryState.entryTheState(ev, false);
          
        }
        /*
        //if(ix < ixLast && (( (history = entryStates[ix+1]) instanceof StateDeepHistory) || (history instanceof StateShallowHistory))) { 
          //the last entry is an history entry.
          StateComposite stateC = (StateComposite)entryState;  //it have to be a composite state.
          if(history instanceof StateDeepHistory) {
            retTrans |= stateC.entryDeepHistory(ev);
          } else {
            retTrans |= stateC.entryShallowHistory(ev);
          }
          ix +=2; //skip over History
        } else {
          retTrans |= entryState.entryTheState(ev, false);
          ix +=1; //next
        }*/
        ix +=1; //next
        entryStateLast = entryState;
    } }
    doneEntry = true;
  }
  
  
  @Override public String toString(){ return transId == null ? "-unknown transId" : transId; }




  
}



@SuppressWarnings("synthetic-access") 
public abstract class TransChoice extends Trans
{
  protected Trans nullTrans = new Trans();
  
  /**Constructs a transition with {@link #choice} sub-transitions.
   */
  public TransChoice(){
    super();
  }
  
  /**This method is used to check all transitions of this choice. It need be overridden by the application.
   * @return the choice branch inside this derived class with them the transition should be fired.
   */
  public abstract Trans choice();
  
  /**Prepares all transitions. This method is public only for usage from StateMGen
   * 
   */
  public void prepareTransitions(int nRecurs) {
    if(nRecurs > 1000) throw new RuntimeException("too many recursions");
    if(choice !=null) {
      for(Trans trans: choice) {
        prepareTransition(trans, nRecurs +1);
      }
    }
  }




}





/**If a state contains a field of this class, a timeout is given for that state.
 */
public class Timeout extends Trans
{
  int millisec;
  
  /**Creates the timeout.
   * @param millisec The milliseconds to the timeout as positive number > 0 
   * @param dstStates The destination state(s)
   */
  public Timeout(int millisec, Class<?> ...dstStates) {
    super(dstStates);
    transId = "timeout";
    this.millisec = millisec;
  }
}



public class TransJoin extends Trans
{
  
  int[] joinStateHashes;
  /**If not null then it is a join transitions. All of this states should be active to fire the transition. */
  StateSimple[] joinStates;

  /**Filled with {@link StateParallel#join(TransJoin, Class...)}
   * which should be used to construct a Join transition. 
   * Used by {@link #buildJoinStates()}. */
  //Class<?>[] joinStateClasses;
  
  public TransJoin(Class<?> ...dstStates) {
    super(dstStates);
    transId = "join";
  }
  
  
  
  public TransJoin(String name, int[] dstKeys) { super(name, dstKeys); }
  
  /**Invoked with the constructor to set the source states of the join transition. Pattern:
   * <pre>
   * TransJoin myTransJoin = (new TransJoin(DstState.class, DstState2.class)).srcStates(SrcState1.class, SrcState2.class);
   * 
   *      DstState <-----|<----- SrcState1
   *      DstState2 <----|<----- SrcState2
   * </pre>
   * Check the transition with {@link StateSimple.TransJoin#joined()}.
   * </pre>
   * The constructor argument(s) is/are the destination state(s), more as one for a fork in the join transition.
   * This method names the source states to join.
   * 
   * @param joinStateClassesArg
   * @return
   */
  public TransJoin srcStates(Class<?> ... joinStateClassesArg){
    if(joinStateClassesArg == null || joinStateClassesArg.length ==0) this.joinStateHashes = null;
    else {
      this.joinStateHashes = new int[joinStateClassesArg.length];
      for(int ix = 0; ix < joinStateHashes.length; ++ix){
        this.joinStateHashes[ix] = joinStateClassesArg[ix].hashCode();     //store the hashcode to find it.
      }
    }
    //this.joinStateClasses = joinStateClassesArg;
    return this;
  }
  
  
  
  public void srcStates(int[] stateHashes) { this.joinStateHashes = stateHashes; }
  
  
  protected void buildJoinStates() {
    joinStates = new StateSimple[joinStateHashes.length];
    int ix =-1;
    for(int joinState: joinStateHashes) {
      StateSimple state = stateMachine.stateMap.get(joinState);  //getState(joinState1);
      joinStates[++ix] = state;
    }
  }
  
  
  /**Checks this transition, returns true if all join states are {@link StateSimple#isInState()}. */
  public boolean joined() {
    for(StateSimple state: joinStates) {
      if(! state.isInState()) return false;
    }
    return true;
  }
  
}



protected StateSimple(){
  this.modeTrans = mRunToComplete;  //default: call trans(), it has not disadvantages
  //search method exit and entry and transition methods
  /*
  Method[] methods = this.getClass().getDeclaredMethods();
  
  for(Method method: methods) {
    String name = method.getName();
    Class<?>[] argTypes = method.getParameterTypes();
    Class<?> retType = method.getReturnType();
    if(name.equals("entry")){
      if(argTypes.length != 1 || argTypes[0] != Event.class){
        throw new IllegalArgumentException("entry method found, but with failed argument list. Should be \"entry(Event<?,?> ev)\" ");
      }
      entryMethod = method;
      entry = new EntryMethodAction();  //use internal action which calls entryMethod
    }
    else if(name.equals("exit")){
      if(argTypes.length != 0){
        throw new IllegalArgumentException("exit method found, but with argument list. Should be \"exit()\" ");
      }
      exitMethod = method;
      exit = new ExitMethodAction();    //use internal action which calls exitMethod
    }
    else {
      //all other methods should be transition methods.
      
    }
  }
  */
}


/**Sets any auxiliary information to that state. This method is used especially in {@link org.vishia.stateMGen.StateMGen} in the component srcJava_Zbnf
 * to help generate a state machine for C language. But it can be used in the user's application in any way too. 
 * @param info Any object referred from this state.
 */
public void setAuxInfo(Object info) { auxInfo = info; }

/**Gets the auxiliary information to that state which is set with {@link #setAuxInfo(Object)}. This method is used especially in {@link org.vishia.stateMGen.StateMGen} in the component srcJava_Zbnf
 * to help generate a state machine for C language. But it can be used in the user's application in any way too. 
 * @return Any object referred from this state.
 */
public Object auxInfo() { return auxInfo; }



/**Returns the path of all states from the top state in the hierarchie of this state. It may be used for debugging. 
 * It is used in StateMgen. Note: Do not change a state in the path! (Java does not know const keyword, rather than in C/C++) */
public StateSimple[] statePath(){ return statePath; }

/**Returns that state which is the composite state which controls the activity of that independent part of the StateMachine.
 * Note: If a composite state is only used to build a pool of simple states which have common transition(s) the pool-building state
 * is a {@link StateCompositeFlat} and it is not the rootState.
 * 
 */
public StateComposite compositeState(){ return compositeState; }

/**Returns that state which is the enclosing state. It may be equal to the {@link #compositeState()}. Otherwise it may be a
 * {@link StateCompositeFlat} or a {@link StateParallel}. 
 * Note: If a composite state is only used to build a pool of simple states which have common transition(s) the pool-building state
 * is a {@link StateCompositeFlat} and it is not the {@link #compositeState()}.
 * 
 */
public StateSimple enclState(){ return enclState; }



  /**Sets the entry-action for this state. The entry-action can be implemented in any other class of the application as inner non static class
   * and can be accessed to the data to its environment class. That is the advantage in comparison to a overridden method {@link #entry(EventObject)}.
   * This action is used only if the method {@link #entry(EventObject)} is not overridden.
   * @param action
   */
  public void setEntryAction(StateAction entry){ this.entry = entry; }


  /**Sets the exit-action for this state. The exit-action can be implemented in any other class of the application as inner non static class
   * and can be accessed to the data to its environment class. That is the advantage in comparison to a overridden method {@link #exit()}.
   * This action is used only if the method {@link #exit()} is not overridden.
   * @param action
   */
  public void setExitAction(Runnable exit){ this.exit = exit; }

  
  
  /**Checks whether the given event is the timeout event for this state.
   * @param ev
   * @return true if this state has a timeout.
   */
  protected boolean isTimeout(EventObject ev){
    return ev !=null && ev == evTimeout;
  }
  

/**This method may be overridden for a entry action. In that case the {@link #entry} or {@link #entryMethod} is either not used
 * or it is used especially in the overridden method responsible to the user.
 * If this method is not overridden, a given {@link StateAction} stored on {@link #entry} is executed.
 * That may be the execution of the {@link #entryMethod}.
 * @param ev The data of the event can be used for the entry action.
 * @return {@link #mRunToComplete} if next states should be tested for conditions.
 */
protected int entry(EventObject ev) {
  if(entry !=null) { return entry.exec(ev); }
  else return 0;
}




/**This method may be overridden for a exit action. In that case the {@link #exit} or {@link #exitMethod} is either not used
 * or it is used especially in the overridden method responsible to the user.
 * If this method is not overridden, a given {@link Runnable} stored on {@link #exit} is executed.
 * That may be the execution of the {@link #exitMethod}.
 */
protected void exit() {
  if(exit !=null) { exit.run(); }
}







/**It is overridden package-local in all derived {@link StateComposite}, {@link StateParallel}.
 * @param enclState
 */
void buildStatePathSubstates(StateSimple enclState, int recurs) { buildStatePath(enclState); }



/**Sets the path to the state for this and all {@link #aSubstates}, recursively call.
 * @param enclState
 * @param recurs
 */
final void buildStatePath(StateSimple enclState) {
  if(enclState == null) { 
    //special handling for TopState
    statePath = new StateSimple[1];
    statePath[0] = this;              //first element is the top state.
  } else {
    //search the root StateComposite:
    int ixe = 1;
    StateSimple enclState1 = this.enclState;
    while(enclState1 !=null && !(enclState1 instanceof StateComposite) &&  !(enclState1 instanceof StateParallel)) {
      enclState1 = enclState1.enclState;
      ixe +=1;
    }
    if(enclState1 instanceof StateComposite) { 
      compositeState = (StateComposite)enclState1;
    } //else: It is a StateSimple in a StateParallel.
    //
    //copy the path from the top state to the new dst state. It is one element lengths.
    int topPathLength = enclState.statePath.length;
    this.statePath = new StateSimple[topPathLength +1];
    System.arraycopy(enclState.statePath, 0, this.statePath, 0 , topPathLength);
    statePath[topPathLength] = this;  //last element is this itself.
    this.ixCompositeState_inStatePath = topPathLength - ixe;
  }
}



/**Create all transition list for this state and all {@link #aSubstates}, recursively call.
 * It is overridden package-local in all derived {@link StateComposite}, {@link StateParallel}.
 * @param recurs
 */
void createTransitionListSubstate(int recurs){ this.createTransitionList(this, null, 0); }


/**Creates the list of all transitions for this state, invoked one time after constructing the statemachine.
 * 
 */
void createTransitionList(Object stateInstance, Trans parent, int nRecurs){
  //Use a sorted list if the transitions needs priorities.
  IndexMultiTable<String, Trans> transitions1 = new IndexMultiTable<String, Trans>(IndexMultiTable.providerString);
  //List<Trans>transitions = new LinkedList<Trans>();
  Class<?> clazz = stateInstance.getClass();
  if(nRecurs > 2) {
    Debugutil.stop();
  }
  if(nRecurs > 10) throw new IllegalArgumentException("too many recursion, choice-transition; state=" + stateId);
  try{
    Class<?>[] clazzs = clazz.getDeclaredClasses();
    for(Class<?> clazz1: clazzs){
      //it is deprecated, don't use!
      if(DataAccess.isOrExtends(clazz1, Trans.class)){
        System.err.println("The possibility of writing a Transition as class should not use furthermore. ");
        @SuppressWarnings("unused")  //only test
        Constructor<?>[] ctora = clazz1.getDeclaredConstructors();
        //Note: the constructor needs the enclosing class as one parameter because it is non-static.
        //The enclosing instance is the StateSimple always, also on choice-transitions.
        Constructor<?> ctor1 = clazz1.getDeclaredConstructor(StateSimple.this.getClass());
        ctor1.setAccessible(true);
        Trans trans = (Trans)ctor1.newInstance(this);
        trans.transId = clazz1.getSimpleName();
        //transitions.add(trans);
        transitions1.add("", trans);
        trans.parent = parent;  //null for a state transition
        if(trans instanceof TransChoice) {
          createTransitionList(trans, trans, nRecurs+1);  //for choices
        }
        //checkBuiltTransition(trans, nRecurs);
      }
    }
    Field[] fields = clazz.getDeclaredFields();
    for(Field field: fields){
      field.setAccessible(true);
      Object oField = field.get(stateInstance);
      if(oField !=null && DataAccess.isOrExtends(oField.getClass(), Trans.class)) {
        String sFieldName = field.getName();
        if(sFieldName.equals("timeout")){
          Debugutil.stop();
        }
        if(!DataAccess.isReferenceToEnclosing(field)) { //don't test the enclosing instance, it is named this$0 etc. 
          Trans trans = (Trans) oField;
          trans.transId = sFieldName;  //automatic set the name of the transition.
          //transitions.add(trans);
          transitions1.add("", trans);
          trans.parent = parent;  //null for a state transition
          if(trans instanceof TransJoin) {
            //getJoinSrcStateHash((TransJoin)trans);
          }
          if(trans instanceof TransChoice) {
            createTransitionList(trans, trans, nRecurs+1);  //for choices
          }
          //checkBuiltTransition(trans, nRecurs);
        }
      }
    }
  } catch(Exception exc){
    exc.printStackTrace();
  }  
  if(transitions1.size() >0) {
    int nrofTransitions = transitions1.size();
    Trans[] aTrans = new Trans[nrofTransitions];
    int ixTrans = -1;
    for(Map.Entry<String, Trans> entry: transitions1.entrySet()){
      aTrans[++ixTrans] = entry.getValue();
    }
    if(parent == null) {
      this.aTransitions = aTrans; // transitions.toArray(new Trans[transitions.size()]);
    } else {
      parent.choice = aTrans; //transitions.toArray(new Trans[transitions.size()]);
    }
  }
}



/**Prepare the own transitions. This method is overridden for {@link StateCompositeFlat} etc
 * which checks the sub states too.
 * @param recurs recursion for sub states.
 */
void prepareTransitionsSubstate(int recurs) {
  prepareTransitions(null, 0);  //prepare the own transitions. recurs =0, used for choice transitions.
}



/**Prepares all transitions. This method is public only for usage from StateMGen
 * 
 */
void prepareTransitions(Trans parent, int nRecurs) {
  if(aTransitions !=null) {
    for(Trans trans: aTransitions) {
      prepareTransition(trans, 0);
    }
  }
}






private void prepareTransition(Trans trans, int nRecurs) {
  if(trans.transId.equals("timeout")) {
    Debugutil.stop();
  }
  //if(trans.dst !=null) {
    trans.buildTransitionPath();
  //}
  
  if(trans instanceof TransChoice) {
    ((TransChoice)trans).prepareTransitions(nRecurs+1);
  }
}







/**Check and returns true if the enclosing state has this state as active one.
 * @return
 */
public final boolean isInState(){
  if(compositeState !=null) {
    return compositeState.isActive && compositeState.isInState(this);
  } else {
    //it is the top state or a StateSimple inside a StateParallel
    if(enclState == null) return true; //it is the top state.
    else return enclState.isInState();
  }
  /*
  if(enclState == null) return true;   //it is the top state.
  else if(enclState instanceof StateComposite){
    StateComposite enclc = (StateComposite)enclState;
    return enclc.isActive && enclc.isInState(this);  //the enclosing state has this as active one.
  } else {
    //encl is a StateParallel
    return enclState.isInState();  //check whether it is active.
  }
  */
}


/**This method should be overridden by the user to select a transition which should be fire with given conditions.
 * <br><br>
 * If only a breakpoint should be able to set the user should write:
 * Q Override Trans selectTrans(Event<?,?> ev) { return super.selectTrans(ev); }
 * <br><br>
 * The returned transition will be fire in the calling environment. This method is invoked
 * if {@link StateMachine#processEvent(EventObject)} is called.
 * <br>Template for the overridden method: <pre>
 * Q Override Trans selectTrans(Event<?,?> ev) {
 *   if(ev instanceof MyEvent) return trans_A;
 *   else if(otherCondition) return trans_B;
 *   else return null;
 * }</pre> 
 * It is possible to write the action code immediately in this method. Then {@link Trans#doExit()} should be invoked before.
 * If {@link Trans#doExit()} is not invoked the exit actions are executed after the transition code. That is not problematically 
 * but not conform to the UML state machine definition:<pre>
 * Q Override Trans selectTrans(Event<?,?> ev) {
 *   if(ev instanceof MyEvent){
 *     trans_A.doExit();  //it exits all necessary states may be from a deeper level.
 *     users_transition_code();  
 *     return trans_A;
 *   }
 *   else if(otherCondition) {
 *     users_transition_code();  
 *     //doExit() will be invoked from the calling level.
 *     return trans_B;
 *   }
 *   else return null;
 * }</pre> 
 * <br><br>
 * The Transitions which are used here should be declared as fields of {@link Trans} which may be overridden anonymously.
 * Especially the {@link Trans#action(EventObject)} can be overridden or the field {@link Trans#action} can be set. 
 * An overridden {@link Trans#check(EventObject)} field {@link Trans#check} of that transitions is not regarded.
 * <br>Template for the necessary transition definition:
 * <pre>
 * Trans trans_A = new Trans(Destination.class);   //without action
 * 
 * Trans trans_B = new Trans(Destination.class) {
 *   protected void action(Event<?,?> ev) {
 *     //do action code
 *   }
 * };
 * 
 * Trans trans_C = new Trans(Destination.class) {
 *   { action = ref.action;  //instanceof StateAction, callback 
 *   }
 * };
 * 
 * @param ev The event to test.
 * @return The transition which should be fired or null.
 */
protected Trans checkTrans(EventObject ev) { return null; }



/**Applies an event to this state respectively processes the event with this state.
 * Note: This method is overridden by {@link StateComposite#processEvent(EventObject)} with an more complex algorithm.
 * For a simple state it invokes only {@link #_checkTransitions(EventObject)}.
 * @param ev The event to apply.
 * @return information about the switch, Bits {@link #mEventConsumed}, {@link #mTransit}, {@link #mRunToComplete}, {@link #mStateEntered}, {@link #mStateLeaved}.
 */
/*package private*/ int processEvent(EventObject ev){ return _checkTransitions(ev); }


/**Check all transitions and fire one transition if true.
 * This method is called primary in {@link StateComposite#_processEvent(EventMsg2)} if the state is active.
 * @param ev Given event
 * @return Bits {@link #mRunToComplete}, {@value #mTransit}, {@link #mEventConsumed}, {@link #mStateEntered} to control the event processing.
 */
final int _checkTransitions(EventObject ev) {
  int res = 0;
  //clear all transition data before test it:
  /*
  if(aTransitions !=null) {
    for(Trans trans1: aTransitions){ //check all transitions
      trans1.doneExit = trans1.doneAction = trans1.doneEntry = false;
      trans1.retTrans = 0;
  } }
  */
  Trans trans;
  //either the first time or overridden check method: Use it.
  try{ 
    StateSimple statetest = this;
    do {
      trans = statetest.checkTrans(ev); 
      /*if(trans == null && statetest.transTimeout !=null && ev == statetest.evTimeout) { //the own timeout event is expected and received
        trans = statetest.transTimeout.eventConsumed();  
      }*/
      statetest = statetest.enclState;
    } while(trans == null && statetest !=null && !(statetest instanceof StateComposite));  //Check for all enclosing flat states too, not for a composite, it has its own check.
    
    if(trans !=null){
      //it is possible to invoke doExit and any transition code in the checkTrans() method already.
      if(stateMachine.debugTrans) printTransInfo(trans, ev);
      if(!trans.doneExit)   { trans.doExit(); }
      if(!trans.doneAction) { trans.doAction(ev,0); }
      if(!trans.doneEntry)  { trans.doEntry(ev); }
      trans.doneExit = trans.doneAction = trans.doneEntry = false;  //for next usage.
      int ret = trans.retTrans | mTransit;
      trans.retTrans = 0;
      return ret;
    }
  }
  catch(Exception exc) {
    if(stateMachine.permitException) {
      StringBuilder u = new StringBuilder(1000);
      u.append("StateSimple trans exception - "); stateMachine.infoAppend(u); u.append(";");
      if(ev !=null){ u.append("event: ").append(ev.toString()); }
      CharSequence text = Assert.exceptionInfo(u, exc, 0, 50);
      System.err.append(text);
      trans = null;
    } else {
      throw new RuntimeException(exc); //forward it but without need of declaration of throws exception
    }
  }
  return res;
}



private void printTransInfo(Trans trans, EventObject ev) {
  if( (trans.retTrans & mEventConsumed) !=0) {
    System.out.println("StateSimple - Trans ev consmd;" + trans.transId + ";" + trans.exitStates[0].toString() + ";==>" + trans.entryStates[trans.entryStates.length-1].toString() + "; event =" + ev + ";");
  } else if(ev !=null) {
    System.out.println("StateSimple - Trans ev nCons.;" + trans.transId + ";" + trans.exitStates[0].toString() + ";==>" + trans.entryStates[trans.entryStates.length-1].toString() + "; event not consumed =" + ev + ";");
  } else {
    System.out.println("StateSimple - Trans runToCmpl;" + trans.transId + ";" + trans.exitStates[0].toString() + ";==>" + trans.entryStates[trans.entryStates.length-1].toString());
  }
}





/**Executes enter in this state. This routine will be invoked in {@link Trans#doEntry(EventObject)} immediately for the entry states,
 * in {@link StateCompositeFlat#entryDefaultState()}
 * or in {@link StateComposite#entryDeepHistory(EventObject)} or {@link StateComposite#entryShallowHistory(EventObject)}.
 * It enters only in this state. Enter is sub states is done in the calling routines.  
 * <ul>
 * <li>The reference of {@link StateComposite#stateAct} will be set in its {@link #compositeState()} to mark this state as current one..
 * <li>The {@link #compositeState()} (its composite) will be marked as {@link StateComposite#isActive} (may be marked already).
 * <li>Sets debug informations: {@link #ctEntry}+=1, {@link #dateLastEntry}=yet, {@link #durationLast}=0.
 * <li>If it is a {@link StateComposite} without history, sets the own {@link StateComposite#stateAct} to null, 
 *   therewith forces maybe entry in the default state if an entry in a member of the composite is not done by this transition
 *   but sets its own {@link StateComposite#isActive}. Note: The enter in the default state is done by the next state switch.
 * <li>Note: If it has a history and it is an history entry, the {@link StateComposite#stateAct} will be entered as history state 
 *   in the {@link StateComposite#entryDeepHistory(EventObject)} or {@link StateComposite#entryShallowHistory(EventObject)}
 *   in the {@link Trans#doEntry(EventObject)} 
 * <li>At least invokes the user defined {@link #entry(EventObject)} action.  
 * </ul>
 * @param ev
 * @return
 */
final int entryTheState(EventObject ev, boolean history) { //int isConsumed){
  if(compositeState !=null) {
    compositeState.stateAct = this;
    compositeState.isActive = true;
  } //else only null on a StateSimple in a StateParallel.
  //
  ctEntry +=1;
  dateLastEntry = System.currentTimeMillis();
  durationLast = 0;
  //if(this instanceof StateAddParallel){
  //  ((StateAddParallel)this).entryAdditionalParallelBase();
  //}
  if(!history && this instanceof StateComposite){ //quest after StateParallel handling because isActive is set to true.
    StateComposite cthis = (StateComposite)this;
    //entry for a composite state forces the default state if entries for deeper states are not following.
    //cc101512: Now the default entry is done in the transition, not necessary, faulty for history entry: 
    //no: cthis.stateAct = null;   
    cthis.isActive = true;
  }
  if(this.transTimeout !=null && evTimeout !=null){
    evTimeout.activateAt(System.currentTimeMillis() + this.millisectimeout);
  }
  int entryVal;
  try { 
    entryVal = entry(ev);  //run the user's entry action.
    if(stateMachine.debugEntryExit) {
      System.out.println("StateSimple - entry, " + stateId);
    }
  } catch(Exception exc){
    if(stateMachine.permitException){
      StringBuilder u = new StringBuilder(1000);
      u.append("StateSimple entry exception - "); stateMachine.infoAppend(u); u.append(";");
      if(ev !=null){ u.append("event: ").append(ev.toString()); }
      CharSequence text = Assert.exceptionInfo(u, exc, 0, 50);
      System.err.append(text);
      entryVal = 0;
    } else {
      throw new RuntimeException(exc); //forward it but without need of declaration of throws exception
    }
  }
  return entryVal | mStateEntered | modeTrans;
  //return isConsumed | modeTrans;
}




/**Exits the state and all enclosing states till level.
 * <ul>
 * <li>If the state is a StateComposite, then this method is called recursively for the current state of composite. 
 *   It means, the current state is exiting. If the current state of the StateComposite is a StateComposite too, this rule is valid for that too.
 *   It means, this method is called recursively for all StateComposite of the current statePath.
 * <li>If the exit will be invoked for a StateCompositeFlat, this routine is called for the current StateSimple of the {@link StateComposite#compositeState()}.
 *   It means always the really current state will be exiting. See {@link Trans#doExit()}.  
 * <li>If the state has a timeout {@link #evTimeout} then it is removed from {@link StateMachine#theThread}. {@link org.vishia.event.EventTimerThread#removeTimeOrder(EventTimeout)}.
 * <li>If the state is a {@link StateParallel} all current parallel states are exiting.
 * <li>The users {@link #exit()} routine is invoked, or the {@link #exit} is run, if {@link #setExitAction(Runnable)} was set.
 * <li>If {@link StateMachine#debugEntryExit} is set the exit text is written with System.out().
 * <li>If the users routine is thrown an exception an System.err is written if {@link StateMachine#permitException} is set, or a RuntimeException is forwarding. 
 * </ul>
 * Note: The routine is protected to see in documentation. It is final because all necessities for parallel and composite states are considered here already. 
 * @param level according to the {@link #statePath} the last level which is exiting. It is the index in the statepath array.  
 */
/*package private*/ 
protected final void exitTheState(int level) { 
  long time = System.currentTimeMillis();  
  //
  int ixStatePath = statePath.length -1;  //refers to this initially, all states in path till level will be exiting.
  //exit all states in statePath till level:
  StateSimple stateExitLast = null;
  while(ixStatePath >= level) { //don't exit the common state.
    StateSimple stateExit = statePath[ixStatePath];
    stateExit.durationLast = time - dateLastEntry;
    if(stateExit.evTimeout !=null && stateExit.evTimeout.used()) {
      stateMachine.timerThread.removeTimeOrder(evTimeout);
    }
    if(stateExit instanceof StateComposite && stateExitLast == null) { //NOTE: don't use dynamic linked methods, it is better to seen what's happen in one method.
      //exits the current state of composite if it is the first state to exit.
      //Note if it is not the first state to exit, the current state(s) of this StateComposite are exiting yet already. 
      //it calls this method recursively.
      StateComposite thisComposite = (StateComposite)this;
      if(thisComposite.isActive && thisComposite.stateAct !=null) {
        thisComposite.stateAct.exitTheState(ixStatePath +1);  //all states in composite.
        //it calls recursively exitTheState for inner composites.
      }
    }
    else if(stateExit instanceof StateParallel) {
      //exits all parallel bough of this StateParallel. The stateExitLast refers to that bough which is exiting already yet.
      //If the StateParallel is the first state to exit, all boughs will be exiting because stateExitLast == null
      StateParallel exitParallel = (StateParallel)stateExit;
      if(exitParallel.aParallelstates !=null) {
        for(StateSimple parallelState : exitParallel.aParallelstates) {
          if(parallelState != stateExitLast) { //stateExitLast is that parallel bough which is exited already.
            parallelState.exitTheState(ixStatePath+1);  //exit till parallel state, not till StateParallel itself.
          } }
      }
      
    }
    try{ 
      //==>>
      stateExit.exit();  //run the user's exit action.
      if(stateMachine.debugEntryExit) {
        System.out.println("StateSimple - exit, " + stateExit.stateId);
      }
      
    } catch(Exception exc) {
      if(stateMachine.permitException){
        StringBuilder u = new StringBuilder(1000);
        u.append("StateSimple exit exception - "); stateMachine.infoAppend(u); u.append(";");
        CharSequence text = Assert.exceptionInfo(u, exc, 0, 50);
        System.err.append(text);
      } else {
        throw new RuntimeException(exc); //forward it but without need of declaration of throws exception
      }
    }
    if(stateExit.enclState instanceof StateComposite) { 
      ((StateComposite)stateExit.enclState).isActive = false; 
    }
    stateExitLast = stateExit;
    ixStatePath -=1;
  }//while
}



/**Gets the path to this state. The path is build from the {@link #stateId} of all enclosing states
 * separated with a dot and at least this stateId.
 * For example "topStateName.compositeState.thisState". 
 */
CharSequence getStatePath(){
  StringBuilder uPath = new StringBuilder(120);
  StateSimple state = this;
  while((state = state.enclState) !=null){
    uPath.insert(0,'.').insert(0, state.stateId);
  }
  uPath.append('.').append(stateId);
  return uPath;
}



/**Returns the name of the state, for debugging.
 * @return
 */
public String getName(){ return stateId; }


public CharSequence infoAppend(StringBuilder u) {
  if(u !=null) {
    u.append(stateId);
    return u;
  }
  else return stateId;
}

/**Returns the state Id and maybe some more debug information.
 * @see java.lang.Object#toString()
 */
@Override public String toString(){ return stateId; } //return getStatePath().toString(); }




/**This class is used especially in {@link org.vishia.stateMGen.StateMGen} in the component srcJava_Zbnf
 * to help generate a state machine for C language. It is public for such approaches. 
 * This class should not be used by an application which deals with states only in a Java context. 
 * The class is a non-static inner class to access private methods from its outer class.
 */
@SuppressWarnings("synthetic-access") 
public class PlugStateSimpleToGenState
{
  /**Creates the empty yet array of transitions. 
   * Invoked if transitions should be defined from any Java program outside, not from the Source of this class. Used for StateMgen.
   * @param nrofTransitions Number should be the number of transitions to add, see {@link #addTransition(Trans)}
   */
  public void createTransitions(int nrofTransitions) {
    aTransitions = new Trans[nrofTransitions];
  }


  /**Adds a transition.
   * Invoked if transitions should be defined from any Java program outside, not from the Source of this class. Used for StateMgen.
   * @param trans
   */
  public void addTransition(StateSimple.Trans trans) {
    int ix = 0;
    while(ix < aTransitions.length && aTransitions[ix] !=null){ ix +=1; } //search next free
    if(ix >= aTransitions.length) throw new IllegalArgumentException("too many states to add");
    aTransitions[ix] = trans;

  }
  
  
}







  
}
