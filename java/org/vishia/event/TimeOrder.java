package org.vishia.event;

import org.vishia.util.Debugutil;

/**This class builds a time order instance usable as timeout for state machines or other time orders.
 * It is intent do use for {@link EventTimerThread} or another implementation using {@link EventTimerThread_ifc}.
 * It is also used for the vishia graphical programming (GRAL) referenced in {@link org.vishia.gral.base.GralGraphicEventTimeOrder}
 * and also for {@link org.vishia.fileRemote.FileRemoteProgressEvent}.
 * It is referenced by {@link EventWithDst#timeOrder} and only constructed in that class
 * calling {@link EventWithDst#EventWithDst(String, EventTimerThread_ifc, EventSource, EventConsumer, EventThread_ifc)}
 * or enhanced classes from {@link EventWithDst} if the event should be delayed.
 * <br>
 * An {@link EventCmdtype} with {@link TimeoutCmd} is used inside {@link org.vishia.states.StateMachine} as persistent instance 
 * of any parallel state machine or of the top state if timeouts are used in the states.
 * <br><br>
 * Activate the timeout event:<pre>
 *   myEvent.timeOrder.activate(7000);  //activate execution of the event in 7 seconds.
 * </pre>
 * <ul>
 * <li>Constructor for a given event of any derived specific type:
 *   {@link TimeOrder#TimeEntry(String, EventTimerThread_ifc, EventWithDst)}.    
 * <li>methods to activate: {@link #activate(int)}, {@link #activateAt(long)}, {@link #activateAt(long, long)}
 * <li>Remove a currently timeout: {@link #deactivate()}
 * <li>Check: {@link #timeExecution()}, {@link #timeExecutionLatest()}, {@link #timeToExecution()} {@link #used()}
 * <li>see {@link EventTimerThread}
 * <li>see {@link org.vishia.states.StateMachine}
 * </ul>
 * @author Hartmut Schorrig
 *
 */
public class TimeOrder extends EventSource
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
  @SuppressWarnings("hiding") public final static String version = "2015-01-11";

  public final String name;
  
  
  
    
  protected EventWithDst event;
  
  public final EventTimerThread_ifc timerThread;
  
  private static final long serialVersionUID = 2695620140769906847L;

  /**If not 0, it is the time to execute it. Elsewhere it should be delayed. */
  protected long timeExecution;
  
  /**If not 0, it is the last time to execute it if the execution will be deferred by additional invocation of
   * {@link #activateAt(long)} while it is activated already. 
   */
  protected long timeExecutionLatest;

  /**Set if {@link #awaitExecution(int, int)} is called. 
   * It means a thread is waiting. this.notify() is necessary.
   */
  protected boolean bAwaiting;
  
  protected boolean bNotifyAlsoOnException;
  
  protected boolean bTimeElapsed;
  
  /**True if {@link EventConsumer#processEvent(java.util.EventObject)} has returned the bit {@link EventConsumer#mEventConsumerException}. */
  protected boolean bEventException;
  
  protected int ctConsumed;  
  
  /**True if {@link EventConsumer#processEvent(java.util.EventObject)} has returned the bit {@link EventConsumer#mEventConsumed}. */
  protected boolean bEventExecuted;
  
  /**True if {@link EventConsumer#processEvent(java.util.EventObject)} has returned the bit {@link EventConsumer#mEventConsumFinished}. */
  protected boolean bDone;
  
  
  /**It is counted only. Used for debug. Possible to set. */
  public int dbgctWindup = 0;
  

 
  /**Construction of a time order with any desired specific event to execute.
   * The ctor is package private, only used in {@link EventWithDst#EventWithDst(String, EventTimerThread_ifc, EventSource, EventConsumer, EventThread_ifc)}. 
   * @param name for debugging and log
   * @param timerThread where the time entry should be used in, it is determined.
   * @param event The event for execution.
   */
  TimeOrder ( String name, EventTimerThread_ifc timerThread, EventWithDst event) { 
    super(name);
    this.name = name;
    this.event = event;
    this.timerThread = timerThread;
  }

  
  void setEvent(EventWithDst event) {
    this.event = event;
  }
  
  public EventWithDst event() { return event; }
  
  /**Creates an event as static or dynamic object for usage. See also {@link EventWithDst#EventWithDst(EventSource, EventConsumer, EventThreadIfc)}
   * The timeout instance may be static usual (permanent referenced) and not allocated on demand (with new)
   * because it is used whenever a special state is entered.
   * @param name for debugging and log
   * @param timerThread where the time entry should be used in, it is determined.
   * @param src the event is occupied with this given EventSource instance.
   * @param consumer The destination object for the event. If it is null nothing will be executed if the event is expired.
   *   But you can set the consumer with {@link #occupy(EventSource, EventConsumer, EventThread_ifc, boolean)}
   *   or with {@link #setDst(EventConsumer)} afterwards
   * @param thread evThread if null, then the event is handled by the same thread as the timerThread, immediately after expire.
   *   It means the timeout is handled before the other queued events are handled. This is important for state machine. 
   *   It means if a timeout transition has its timeout, then this is prior.  
   *   <br>If you give the same thread as timerThread (not null), then the event is firstly enqueued in the own event queue after expiring. 
   *   It means events which are queued before already are first executed. For state machines this means, if just a timeout has expired
   *   but there are other events given in the queue, the other events changes the state before and the timeout event may be not used.
   *   The difference is only in the small time of expiring.
   *   <br>  
   *   If this is another thread than timerThread, then the event is handled in the other thread. 
   *   Instead execution with {@link EventConsumer#processEvent(java.util.EventObject)} it is queued in the other thread
   *   and there executed.
   *   With this approach a central timer can be used for several statemachine threads.
   */
  public TimeOrder ( String name, EventTimerThread_ifc timerThread, EventSource src, EventConsumer consumer, EventThread_ifc evThread){
    super(name);
    this.name = name;
    this.event = new EventWithDst(name, src, consumer, evThread !=null ? evThread : timerThread);

    this.timerThread = timerThread;
  }
  
  
  public void clear() {
    this.timeExecution = this.timeExecutionLatest = 0;
    this.bAwaiting = false;
    this.bNotifyAlsoOnException = false;
    this.bTimeElapsed = false;
    this.bEventException = false;
    this.bEventExecuted = false;
    this.ctConsumed = 0;
    this.bDone = false;
    this.timerThread.removeTimeEntry(this);
    this.timerThread.removeFromQueue(this.event);
    this.event.clean();
  }
  
  
  /**Activate the event for the given laps of time.
   * If the event is activated already for a shorter time, the activation time is deferred to this given time
   * but not later than a latest time given with {@link #activateAt(long, long)}. 
   * @param millisec if a negative value or a value less then 3 is given the event is processed immediately.
   * @see #activateAt(long, long).
   */
  public final void activate(int millisec){ activateAt(System.currentTimeMillis() + millisec, 0);}
  
  /**Enters the TimeEntry to activates the event to the given timestamp.
   * <br>If the event is entered already for a shorter time, and this is also the latest time,
   * then nothing is done. The event will be activated at the time as given before. 
   * See also {@link #activateAt(long, long)}.
   * @param date The absolute time stamp in seconds after 1970 UTC like given with {@link java.lang.System#currentTimeMillis()}.
   *   To set a relative time you must write <pre>
   *     myEvent.activateAt(System.currentTimeMillis() + delay);
   *   </pre>
   */
  public final void activateAt(long date) { activateAt(date, 0); }
  
  
  /**Enters the TimeEntry to activates the event to the given timestamp with a given latest time stamp.
   * This is proper for deferment an event by given a new time also by call of {@link #activateAt(long)} or {@link #activate(int)}.
   * If the event is activated already for a shorter time, then the activation time is deferred to this given time
   * but not later than a latest time given with {@link #activateAt(long, long)}. 
   * @param executionTime The time stamp for desired execution, can be delayed on second calls.
   * @param latest The latest time stamp where the event should be processed though it is delayed.
   *   If the event is activated already for a earlier latest time, this argument is ignored. 
   *   The earlier latest time is valid. Use {@link #deactivate()} before this method to set the latest processing time newly. 
   * @see #activateAt(long).
   */
  public final void activateAt(long executionTime, long latest) {
    //
    if(this.timeExecution !=0 && ((this.timeExecution - System.currentTimeMillis()) < -5000)){ 
      //The execution time is expired since 5 seconds,
      //can be supposed that the event was not recognized, it is old.
      //maybe the execution thread hangs:
      this.timeExecution = 0;  //hence remove it.
      this.timeExecutionLatest = 0;
      System.out.println("remove TimeEntry");
      this.timerThread.removeTimeEntry(this);
    }
    if(this.timeExecution ==0 && this.timeExecutionLatest !=0) {
      Debugutil.stop();
    }
    //
    final long executionTimeUsed;
    if(this.timeExecutionLatest ==0) {                     // only on a new time order
      this.timeExecutionLatest = latest == 0 ? executionTime : latest;
    }
    if( this.timeExecutionLatest !=0 && (executionTime - this.timeExecutionLatest) >0) {
      executionTimeUsed = this.timeExecutionLatest;   // not later as latest.
    } else {
      executionTimeUsed = executionTime;
    }
    boolean bFree = this.timeExecution ==0;
    this.timeExecution = executionTimeUsed;  //set it newly
    if( bFree) {                     // it is free, use this instead occupy
      this.event.dateCreation.set(System.currentTimeMillis());  //then set the new occupy time.
    } else { 
      //already added:
      this.dbgctWindup +=1;
      //else: shift order to future:
      //remove and add new, because its state added in queue or not may be false.
      this.timerThread.removeTimeEntry(this);  //if it is not in the queue, no problem
    }
    this.timerThread.addTimeEntry(this);    //add newly, delayed event was removed before.
  }
  
  
  /**Remove this from the queue of timer events and orders 
   */
  public final void deactivate(){
    this.timeExecution = 0;
    this.timeExecutionLatest = 0;
    if(this.timerThread !=null) {
      this.timerThread.removeTimeEntry(this);
    }
  }
  
  /**Returns the time stamp where the time is elapsed
   * @return milliseconds after 1970, 0 if the event is not activated yet.
   */
  public final long timeExecution(){ return this.timeExecution; }
 
  
  /**Returns the time stamp where the time is elapsed latest.
   * @return milliseconds after 1970, 0 if the event is not activated yet.
   */
  public final long timeExecutionLatest(){ return this.timeExecutionLatest; }
 
  

  
  public final boolean used(){ return this.timeExecution !=0; }


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
    this.timeExecutionLatest = 0;                // Note: set first before timeExecution = 0. Thread safety.
    this.timeExecution = 0;                      // may force newly adding if requested. Before execution itself!
    if(this.timerThread != this.event.evDstThread) {
      this.event.sendEvent();                    // it is enqueued in the evThread
    } else {
      this.event.processEvent();                 // it is executed immediately in the timerThread if evThread is the same
    }
  }
  
  
  /**This operation should not called by the user. It is intent to call via the interface {@link EventSource}
   * from the event management classes, especially {@link EventTimerThread} or adequate implementations of {@link EventThread_ifc}
   * if {@link EventConsumer#processEvent(java.util.EventObject)} is returned.
   * @param state the return value of processEvent, bits 31..24 is the {@link EventWithDst#ctConsumed}.
   */
  @Override public void notifyConsumed ( int state) {
    this.bEventException = (state & EventConsumer.mEventConsumerException) !=0;
    if( (state & EventConsumer.mEventConsumed) !=0) {
      this.ctConsumed +=1;
    }
    int mask = EventConsumer.mEventConsumFinished;
    if(this.bNotifyAlsoOnException) {
      mask += EventConsumer.mEventConsumerException;
    }
    if((state & mask) !=0) {
      synchronized(this) {
        this.bDone = true;
        if(this.bAwaiting) {
          this.notify();
        }
      }
    }
  }


  
  
  /**Checks whether it should be executed.
   * @return time in milliseconds for first execution or value <0 if it is expired (to execute immediately)
   *   or the Integer.MAX_VALUE if it is not in use (expires in a very long time).
   */
  public final int timeToExecution(){ 
    return this.timeExecution == 0 ? Integer.MAX_VALUE : (int)( this.timeExecution - System.currentTimeMillis()); 
  }
  

  
  
  /**Waits for execution in any other thread. This method can be called in any thread, especially in that thread, 
   * which initializes the request. 
   * @param ctDoneRequested Number of executions requested.
   * @param timeout maximal waiting time in milliseconds, 0 means wait forever.
   * @return true if it is executed the requested number of.
   */
  public boolean awaitExecution(int timeout, boolean bAlsoOnException)
  { 
    long timeEnd = System.currentTimeMillis() + timeout; 
    synchronized(this) {
      this.bNotifyAlsoOnException = bAlsoOnException;
      while(!this.bDone) {
        long waitingTime = timeEnd - System.currentTimeMillis();
        if(waitingTime > 0 || timeout == 0 ) { //should wait, or special: wait till end of routine.
          this.bAwaiting = true;
          try{ wait(waitingTime); } catch(InterruptedException exc){}
          this.bAwaiting = false;
        } 
      };
    }
    return(this.bEventExecuted);
  }
  

  
  
  
}
