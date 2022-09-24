package org.vishia.event;

/**This class is a ready-to-use timeout event for state machines or the base class for time orders.
 * <br>
 * An EventTimeout is used inside {@link org.vishia.states.StateMachine} as persistent instance 
 * of a parallel state machine or of the top state if timeouts are used in the states.
 * <br>
 * Instantiation pattern:<pre>
 *   EventThread thread = new EventThread("stateThread");
 *   //uses the thread as timer manager and as event executer
 *   EventTimeout timeout = new EventTimeout(stateMachine, thread);
 * </pre>
 * Activate the timeout event:<pre>
 *   timeout.activate(7000);  //in 7 seconds.
 * </pre>
 * <ul>
 * <li>Constructors: {@link EventTimeout#EventTimeout(EventConsumer, EventTimerThread)}, {@link EventTimeout#EventTimeout()}.
 * <li>methods to activate: {@link #activate(int)}, {@link #activateAt(long)}, {@link #activateAt(long, long)}
 * <li>Remove a currently timeout: {@link #deactivate()}
 * <li>Check: {@link #timeExecution()}, {@link #timeExecutionLatest()}, {@link #timeToExecution()} {@link #used()}
 * <li>see {@link TimeOrder}.
 * <li>see {@link EventTimerThread}
 * <li>see {@link org.vishia.states.StateMachine}
 * </ul>
 * @author Hartmut Schorrig
 *
 */
public class EventTimeout extends EventWithDst
{
  
  /**Version and history:
   * <ul>
   * <li>2022-09-24 Some comments and changed in {@link #activateAt(long, long)} while searching the problem 
   *   that the execution sometimes hangs. Now it does not hang since ~10 min , but the reason is not fully clarified.
   *   Before, the problem was that the thread has used its 10 seconds waiting time if the event queue is empty.
   *   It seems to be a missing notify(), but the notify() was programmed. 
   *   The problem occurs in the Gral vishiaGui with CurveView for redraw events. 
   * <li>2015-01-02 Hartmut created: as super class of {@link TimeOrder} and as event for timeout for state machines.
   * </ul>
   * 
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
   * If you are indent to use this sources without publishing its usage, you can get
   * a second license subscribing a special contract with the author. 
   * 
   * @author Hartmut Schorrig = hartmut.schorrig@vishia.de
   * 
   * 
   */
  public final static String version = "2015-01-11";


  private static final long serialVersionUID = 2695620140769906847L;

  /**If not 0, it is the time to execute it. Elsewhere it should be delayed. */
  protected long timeExecution;
  
  /**If not 0, it is the last time to execute it if the execution will be deferred by additional invocation of
   * {@link #activateAt(long)} while it is activated already. 
   */
  protected long timeExecutionLatest;

  /**It is counted only. Used for debug. Possible to set. */
  public int dbgctWindup = 0;
  

  /**Creates an event as a static object for re-usage. Use {@link #occupy(EventSource, EventConsumer, EventTimerThread, boolean)}
   * before first usage. Use {@link #relinquish()} to release the usage.
   * Usual the parameterized method {@link EventTimeout#EventTimeout(EventSource, EventConsumer, EventThreadIfc)} 
   * should be used.
   */
  public EventTimeout(){ super(); }

  
  /**Creates an event as static or dynamic object for usage. See also {@link EventWithDst#EventWithDst(EventSource, EventConsumer, EventThreadIfc)}
   * The timeout instance may be static usual (permanent referenced) and not allocated on demand (with new)
   * because it is used whenever a special state is entered.
   * @param consumer The destination object for the event. If it is null nothing will be executed if the event is expired.
   * @param thread thread to handle the time order. It is obligatory.
   */
  public EventTimeout(EventConsumer consumer, EventTimerThread_ifc thread){
    super(null, consumer, thread);
  }
  
  
  
  /**Activate the event for the given laps of time.
   * If the event is activated already for a shorter time, the activation time is deferred to this given time
   * but not later than a latest time given with {@link #activateAt(long, long)}. 
   * @param millisec if a negative value or a value less then 3 is given the event is processed immediately.
   * @see #activateAt(long, long).
   */
  public void activate(int millisec){ activateAt(System.currentTimeMillis() + millisec, 0);}
  
  /**Activates the timeout event to the given timestamp.
   * With this method the event or time order is enqueued in its thread given by construction.
   * @param date The absolute time stamp in seconds after 1970 UTC like given with {@link java.lang.System#currentTimeMillis()}.
   *   To set a relative time you must write <pre>
   *     myEvent.activateAt(System.currentTimeMillis() + delay);
   *   </pre>
   */
  public void activateAt(long date) { activateAt(date, 0); }
  
  
  /**Activate the event at the given absolute time 
   * If the event is activated already for a shorter time, the activation time is deferred to this given time
   * but not later than a latest time given with {@link #activateAt(long, long)}. 
   * @param executionTime The time stamp for desired execution, can be delayed on second calls.
   * @param latest The latest time stamp where the event should be processed though it is delayed.
   *   If the event is activated already for a earlier latest time, this argument is ignored. 
   *   The earlier latest time is valid. Use {@link #deactivate()} before this method to set the latest processing time newly. 
   * @see #activateAt(long, long).
   */
  public void activateAt(long executionTime, long latest) {
    //
    if(this.timeExecution !=0 && ((this.timeExecution - System.currentTimeMillis()) < -5000)){ 
      //The execution time is expired since 5 seconds,
      //can be supposed that the event was not recognized, it is old.
      //maybe the execution thread hangs:
      this.timeExecution = 0;  //hence remove it.
      this.timeExecutionLatest = 0;
      System.out.println("remove TimeOrder");
      this.evDstThread.removeTimeOrder(this);
    }
    //
    final long executionTimeUsed;
    if(this.timeExecutionLatest ==0) {
      this.timeExecutionLatest = latest;         // set it only one time if requested.
    }
    if( this.timeExecutionLatest !=0 && (executionTime - this.timeExecutionLatest) >0) {
      executionTimeUsed = this.timeExecutionLatest;   // not later as latest.
    } else {
      executionTimeUsed = executionTime;
    }
    boolean bFree = this.timeExecution ==0;
    this.timeExecution = executionTimeUsed;  //set it newly
    if( bFree) {                     // it is free, use this instead occupy
      this.dateCreation.set(System.currentTimeMillis());  //then set the new occupy time.
    } else { 
      //already added:
      this.dbgctWindup +=1;
      //else: shift order to future:
      //remove and add new, because its state added in queue or not may be false.
      this.evDstThread.removeTimeOrder(this);  //if it is not in the queue, no problem
    }
    this.evDstThread.addTimeOrder(this);    //add newly, delayed event was removed before.
  }
  
  
  /**Remove this from the queue of timer events and orders 
   */
  public void deactivate(){
    timeExecution = 0;
    timeExecutionLatest = 0;
    evDstThread.removeTimeOrder(this);
  }
  
  /**Returns the time stamp where the time is elapsed
   * @return milliseconds after 1970, 0 if the event is not activated yet.
   */
  public long timeExecution(){ return timeExecution; }
 
  
  /**Returns the time stamp where the time is elapsed latest.
   * @return milliseconds after 1970, 0 if the event is not activated yet.
   */
  public long timeExecutionLatest(){ return timeExecutionLatest; }
 
  

  
  public boolean used(){ return timeExecution !=0; }


  /**Processes the event or timeOrder. This routine is called in the {@link EventTimerThread} if the time is elapsed.
   * <br><br>
   * If the {@link #evDst()} is given the {@link EventConsumer#processEvent(java.util.EventObject)} is called
   * though this instance may be a timeOrder. This method can enqueue this instance in another queue for execution
   * in any other thread which invokes then {@link TimeOrder#doExecute()}.
   * <br><br> 
   * It this is a {@link TimeOrder} and the #evDst is not given by constructor
   * then the {@link TimeOrder#doExecute()} is called to execute the time order.
   * <br><br>
   * If this routine is started then an invocation of {@link #activate(int)} etc. enqueues this instance newly
   * with a new time for elapsing. It is executed newly therefore.
   */
  protected final void doTimeElapsed() {
    timeExecutionLatest = 0; //set first before timeExecution = 0. Thread safety.
    timeExecution = 0;     //forces new adding if requested. Before execution itself!
    if(evDst !=null){
      evDst.processEvent(this);  //especially if it is a timeout. Executed in the timer respectively event thread.
    } else if(this instanceof TimeOrder){
      ((TimeOrder)this).doExecute();   //executes immediately in this thread.
    }
  }
  
  
  

  
  
  /**Checks whether it should be executed.
   * @return time in milliseconds for first execution or value <0 to execute immediately.
   */
  public int timeToExecution(){ 
    return timeExecution == 0 ? -1 : (int)( timeExecution - System.currentTimeMillis()); 
  }
  

}
