package org.vishia.event;

import java.io.Closeable;
import java.util.EventObject;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.vishia.msgDispatch.LogMessage;
import org.vishia.util.Debugutil;
import org.vishia.util.ExcUtil;
import org.vishia.util.InfoAppend;

/**This class stores events, starts the processing of the events in one thread and manages and executes time orders in the same thread. 
 * An instance of this class is the main instance to execute state machines in one thread. 
 * The methods of this class except {@link #start()} and {@link #close()} are used from instances of {@link EventWithDst}, 
 * {@link TimeOrder} and {@link TimeOrder} internally.
 * They should not be invoked by an application directly. 
 * But if any other event derived from {@link java.util.EventObject} is used then the methods 
 * {@link #setStdEventProcessor(EventConsumer)}, {@link #storeEvent(EventObject)} and maybe {@link #removeFromQueue(EventObject)}
 * should be used. 
 * <br><br>
 * To instantiate and start use 
 * <pre>
 * 
  EventTimerThread myExecutionThread = new EventTimerThread("thread-name");
  myExecutionThread.start();
 * </pre>
 * On end of the application {@link #close()} should be invoked to end the thread.
 * <br><br>
 * To add any event of derived type of {@link EventWithDst} from any other thread use this instance as argument for the event: 
 * <pre>
 * 
  MyEventWithDst event = new MyEventWithDst(source, dst, myExecutionThread);
  //or:
  event.occupy(source, dst, myExecutionThread, ...)
  event.sendEvent();
 * </pre>
 * To add any other type of {@link java.util.EventObject} you should set a {@link #setStdEventProcessor(EventConsumer)}. 
 * Then use 
 * <pre>
 * 
  myExecutionThread.storeEvent(eventObject);
 * </pre>
 * This class is used as time manager too. It manages and executes {@link TimeOrder} 
 * which are used especially for {@link org.vishia.states.StateMachine},
 * but it can execute {@link TimeOrder} in this thread too. To add a timeout event or a time order 
 * use the methods of the {@link TimeOrder#activateAt(long)} etc:
 * <pre>
 * 
  TimeOrder myTimeOrder = new TimeOrder("name", myExecutionThread) {
    QOverride protected void executeOrder(){ ...execution of the time order ...}
  };
  ...
  myTimeOrder.activate(100);  //in 100 milliseconds.
 * </pre> 
 * That routine invokes the routine {@link #addTimeEntry(TimeOrder)} of this class.
 * <br><br>
 * This class starts a thread. The thread sleeps if no event or time order is given. 
 * <br><br>
 * Events are stored in a {@link java.util.concurrent.ConcurrentLinkedQueue} which is thread-safe to enqueue events
 * from any thread with the method {@link #storeEvent(EventObject)}.
 * Then {@link Object#notify()} is called to weak up the thread if it sleeps. Then the all stored events are dequeued
 * and its execution routine of {@link EventConsumer#processEvent(EventObject)} is invoked. The events are processed one after another.
 * The execution routine is usual a {@link org.vishia.states.StateMachine} but any other {@link EventConsumer} is able to use too. 
 * <br><br>
 * {@link TimeOrder} are stored in another {@link java.util.concurrent.ConcurrentLinkedQueue}. 
 * The absolute time of the next execution is stored in the internal value of {@link #timeCheckNew}. The thread sleeps either
 * till this time is expired or till an event is given. If the time is expired all stored time orders are checked
 * whether its time is elapsed. Then either the {@link EventConsumer#processEvent(EventObject)} is invoked 
 * if an {@link TimeOrder} is given or a {@link TimeOrder} has a destination. Elsewhere the {@link TimeOrder#doExecute} 
 * is invoked to execute the {@link TimeOrder#executeOrder()} in this thread.
 * <br><br>
 * An event can be removed from the queue if it is not executed up to now. The routine {@link #removeFromQueue(EventObject)}
 * is invoked if an event should be {@link EventWithDst#occupyRecall(EventSource, boolean)}. That is if the event should be used
 * newly.
 * <br><br>
 * Note: An {@link TimeOrder} can be removed from execution with the method {@link TimeOrder#deactivate()}.
 * Adequate is done with {@link #removeTimeEntry(TimeOrder)}.
 * 
 *   
 * @author Hartmut Schorrig
 *
 */
@SuppressWarnings("synthetic-access") 
public class EventTimerThread implements EventTimerThread_ifc, Closeable, InfoAppend
{
  
  
  /**Version, license and history.
   * <ul>
   * <li>2023-02-09 Operations with TimeOrder now synchronized, some fine refactoring.  
   * <li>2023-02-06 An inheritance of this class is used for {@link org.vishia.gral.base.GralMng} as graphic thread.
   *   Hence some stuff is now protected, only a few operations are overridden, see there. 
   *   All non overridden operations are set to final now here.    
   * <li>2023-01-31 The {@link #delayMax} now also valid if no time order was processed.
   *   Before, it was 1000 days. This is not problematically for event processing, 
   *   because an event wakes up the wait by {@link #runTimer}.{@link Object#notify()} but it is stupid on debugging.  
   * <li>2022-09-24 Some comments and changed while searching the problem that the execution sometimes hangs. 
   *   But no functionality changes.  
   * <li>2015-05-03 Hartmut new: possibility to check {@link #isBusy()}
   * <li>2015-01-10 Hartmut chg: Better algorithm with {@link #timeCheckNew}
   * <li>2015-01-10 Hartmut renamed from <code>OrderListExecuter</code>
   * <li>2014-02-23 Hartmut created: The algorithm is copied from {@link org.vishia.gral.base.GralGraphicThread},
   *   this class is the base class of them now. The algorithm is able to use outside of that graphic too.
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
  public final static String version = "2023-02-05";

  
  


  /**Bit variable to control some System.out.printf debug outputs. 
   * 
   * 
   * */
  private int debugPrint = 0;
  
  /**Only for inspector or debug access: Use it to show delaying. */
  int debugPrintViewDelayed = 0x001  //wait time
                            | 0x400  //not notified, later
                            | 0x200  //TimeOrderMng not notified because checking                       
                            ; 
  
  protected final String threadName;

  /**The thread which executes delayed wake up. */
  protected Thread threadTimer;


  private EventConsumer eventProcessor;
  
  /**Queue of orders which are executed with delay yet. */
  private final ConcurrentLinkedQueue<EventObject> queueEvents = new ConcurrentLinkedQueue<EventObject>();
  
  /**Queue of orders which are executed with delay yet. */
  private final ConcurrentLinkedQueue<TimeOrder> queueDelayedOrders = new ConcurrentLinkedQueue<TimeOrder>();
  
  /**Temporary used instance for orders ready to execute while {@link #runTimer} organizes the delayed orders.
   * This queue is empty outside running one step of runTimer(). */
  private final ConcurrentLinkedQueue<TimeOrder> queueOrdersToExecute = new ConcurrentLinkedQueue<TimeOrder>();
  
  protected boolean bThreadRun;
  
  /**The delay [ms] for one step if nothing is to do.
   * It is a proper time also for debugging to see what's happen. 
   * Can modified, maybe also 1000 ms
   */
  private int delayMax = 10000;
  
  /**timestamp for a new time entry. It is set in synchronized operation between {@link #addTimeOrder(TimeEvent, long)}
   * and the wait in the {@link #run()} operation.
   * The default value is 10 seconds after now, because no time order may be added. 
   * 10 sec is the limited delay to run one step. 
   */
  protected long timeCheckNew = System.currentTimeMillis() + this.delayMax;

  /**The time on start waiting*/
  private long timeSleep;

  
  //private final boolean bExecutesTheOrder;
  
  /**State of the thread, used for debug and mutex mechanism. This variable is set 'W' under mutex while the timer waits. Then it should
   * be notified in {@link #addTimeEntry(TimeOrder)} with delayed order. */
  protected char stateThreadTimer = '?';
  
  /**Set if any external event is set. Then the dispatcher shouldn't sleep after finishing dispatching. 
   * This is important if the external event occurs while the GUI is busy in the operation-system-dispatching loop.
   */
  private final AtomicBoolean extEventSet = new AtomicBoolean(false);

  protected boolean startOnDemand;
  
  private int ctWaitEmptyQueue;
  
  protected int maxCtWaitEmptyQueue = 5;
  
  
  private boolean preserveRecursiveInfoAppend;


  
  /**Creates the Manager for time orders.
   * It is necessary to invoke {@link #start()} at begin and {@link #close()} on end of an application. 
   * @param executesTheOrder true then the method {@link #step(int, long)} need not be called (not necessary) because the TimeOrder
   *   are executed from the threadTimer already. 
   *   <br>
   *   false then the {@link #step(int, long)} have to be called cyclically from any other thread.
   */
  public EventTimerThread(String threadName)
  {
    this.threadName = threadName;
  }
  
  
  /**Sets the event processor for all events which are not type of {@link EventWithDst}. That events are executed with
   * the given eventProcessor's method {@link EventConsumer#processEvent(EventObject)}.
   * This eventProcessor is not used for {@link EventWithDst}. They need a destination or they should be a {@link TimeOrder}.
   * <br>
   * This routine should be invoked usual one time before start. But the changing of the eventProcessor is possible.
   * 
   * @param eventProcessor the event processor.
   */
  public final void setStdEventProcessor(EventConsumer eventProcessor) {
    this.eventProcessor = eventProcessor;
  }

  
  /**Creates and starts the thread. If this routine is called from the user, the thread runs
   * till the close() method was called. If this method is not invoked from the user,
   * the thread is created and started automatically if {@link #storeEvent(EventCmdtype)} was called.
   * In that case the thread stops its execution if the event queue is empty and about 5 seconds
   * are gone.  */
  public final void start(){ 
    if(this.threadTimer == null && !this.bThreadRun) {
      createThread_();
      this.startOnDemand = false;
      this.threadTimer.start(); 
    }
  }
  

  /**This operation can be overridden if another thread organization, 
   * especially another {@link #stepThread()} should be used.
   * Then just another Runnable implementation for the thread is used.
   * 
   */
  protected void createThread_ ( ){ 
    this.threadTimer = new Thread(this.runTimer, this.threadName);
  }
  

  
  
  /**Stores an event in the queue, able to invoke from any thread.
   * @param ev
   */
  @Override public final boolean storeEvent(EventObject ev){
    if(ev instanceof EventWithDst) {
      EventWithDst event = (EventWithDst)ev;
      if(event.stateOfEvent == 'q') {                      // if the event is already queued, don't do it again.
        return false;            
      }
      ((EventWithDst)ev).stateOfEvent = 'q';               // it is queued.
      //System.out.println(LogMessage.timeCurr("EventTimerThread.storeEvent(..): ") + event.name + ExcUtil.stackInfo(" stack: ", 1, 10));
    }
    this.queueEvents.offer(ev);
    startOrNotify();
    return true;
  }
  

  private void startOrNotify(){
    if(this.threadTimer == null){
      createThread_();
      this.startOnDemand = true;
    } else {
      synchronized(this.runTimer){
        if(this.stateThreadTimer == 'W'){
          wakeup_();
        } else {
          //stateOfThread = 'c';
        }
      }
    }
  }


  /**Wakes up the waiting thread because a new event is enqueues.
   * <br>This operation can be overridden for another thread organization. 
   * 
   */
  protected void wakeup_ ( ) {
    runTimer.notify();
  }
  
  
  
  
  
  /**Should only be called on end of the whole application to finish the timer thread. This method does not need to be called
   * if a @link {@link ConnectionExecThread} is given as Argument of @link {@link EventTimerThread#TimeOrderMng(ConnectionExecThread)}
   * and this instance implements the @link {@link ConnectionExecThread#isRunning()} method. If that method returns false
   * then the timer thread is finished too.
   * <br>This operation can be overridden for another thread organization. 
   * 
   * @see java.io.Closeable#close()
   */
  @Override public void close(){
    this.bThreadRun = false;
    notifyTimer();
  }


  /* (non-Javadoc)
   * @see org.vishia.event.EventThreadIfc#addTimeOrder(org.vishia.event.EventTimeout)
   */
  @Override public final char addTimeEntry(TimeOrder order){ 
    final char retc;
    long delay = order.timeToExecution(); 
    if(delay >=0){
      this.queueDelayedOrders.offer(order);                // enqueue the timeEntry
      //----------------------------------------------------- check whether the new wake up time is lesser than the given one 
      long delayAfterCheckNew = order.timeExecution - this.timeCheckNew;  //positive: It is after timeCheckNew
      if((delayAfterCheckNew) < -2) {                      // an imprecision of 2 ms are admissible, don't wakeup because calculation imprecisions.
        this.timeCheckNew = order.timeExecution;           // an earlier wakeup is necessary than current wait(timediffToCheckNew)
        boolean notified;
        synchronized(this) {                           // then the wait(timediffToCheckNew) should be interrupted
          notified = stateThreadTimer == 'W';
          if(notified){                                    // wake up the time thread to poll orders
            retc = 'n';   //new time
            this.notify();                             // (elsewhere it would sleep till the last decided sleep time.) 
          } else {                                         // should wake up and adjust the sleep time newly.
            // thread is busy, not wait, it will detect the new this.timeCheckNew          
            retc = 'b';   // 
            this.notify();                             // (elsewhere it would sleep till the last decided sleep time.) 
          }
        }
        //System.out.println(LogMessage.timeCurr("addTimeOrder added:") + order.event.name + LogMessage.msgSec(", timeExec=", order.timeExecution) + " notify:" + (notified? "yes" : "no"));
      } else {
        retc = 'l';  // it is later
      }
    } else {
      //System.out.println(LogMessage.timeCurr("addTimeOrder expired:") + order.event.name + LogMessage.msgSec(", timeExec=", order.timeExecution));
      order.event.sendEvent();  //doTimeElapsed();
      retc = 'x';  //eXecuted
    }
    return retc;
  }
  
  
  /**Removes a time order, which was activated but it is not in the event execution queue.
   * If the time order is expired and it is in the event execution queue already, it is not removed.
   * This operation is thread safe. Either the event is found and removed, or it is not found because it is
   * either in execution yet or it is not queued. The event queue is a {@link ConcurrentLinkedQueue}.
   * 
   * @param order
   */
  public final boolean removeTimeEntry(TimeOrder order) {
    boolean found = this.queueDelayedOrders.remove(order);
    //System.out.println(LogMessage.timeCurr(found ? "timeOrder removed: " : "timeOrder not removed") + order.event.name + LogMessage.msgSec(" at ", order.timeExecution) + ExcUtil.stackInfo("", 2, 8));

    //do not: if(!found){ removeFromQueue(order); }  //it is possible that it hangs in the event queue.
    return found;
  }
  
  
  /**Removes this event from its queue if it is in the event queue.
   * If the element of type {@link EventWithDst} is found in the queue, it is designated with stateOfEvent = 'a'
   * This operation is thread safe. Either the event is found and removed, or it is not found because it is
   * either in execution yet or it is not queued. The event queue is a {@link ConcurrentLinkedQueue}.
   * @param ev The event which should be dequeued
   * @return true if found. 
   */
  public final boolean removeFromQueue(EventObject ev){
    boolean found;
    found= queueEvents.remove(ev);
    if(found && ev instanceof EventWithDst){ 
      ((EventWithDst)ev).stateOfEvent = 'a'; 
    }
    return found;
  }
  
  
  @Override public boolean isBusy(){ return stateThreadTimer == 'c'; }
  

  
  
  /**Applies an event from the queue to the destination in the event thread. 
   * This method should be overridden if other events then {@link EventCmdtype} are used because the destination of an event
   * is not defined for a java.util.EventObject. Therefore it should be defined in a user-specific way in the overridden method.
   * This method is proper for events of type {@link EventCmdtype} which knows their destination.
   * @param ev
   */
  private final void applyEvent ( EventObject ev) {
    if(ev instanceof EventWithDst){
      EventWithDst event = (EventWithDst) ev;
      //System.out.println(LogMessage.timeCurr("applyEvent:") + event.name);
      event.processEvent();
    }
    else if(this.eventProcessor !=null) {
      this.eventProcessor.processEvent(ev);
    } 
    else {
      throw new IllegalStateException("destination for event execution is unknown. Use setStdEventProcessor(...). ");
    }
  }
  
  
  

  /**
   * @return true if any action was done because an event was found. false if the queue is empty.
   */
  private boolean checkEventAndRun()
  { boolean processedOne = false;
    try{ //never let the thread crash
      EventObject ev;
      if( (ev = this.queueEvents.poll()) !=null){
        if(ev instanceof EventWithDst ) {
          EventWithDst event = (EventWithDst)ev;
          event.stateOfEvent = 'e';
        }
        this.ctWaitEmptyQueue = 0;
        synchronized(this){
          if(stateThreadTimer != 'x'){
            stateThreadTimer = 'b'; //busy
          }
        }
        if(stateThreadTimer == 'b'){
          applyEvent(ev);
          processedOne = true;
        }
      }
    } catch(Exception exc){
      CharSequence text = ExcUtil.exceptionInfo("EventThread unexpected Exception - ", exc, 0, 50);
      System.err.append(text);
    }
    return processedOne;
  }
  
  

  
  /**Check all time orders whether there are expired, or if not calculate the next time to check. 
   * This operation runs under mutex in that part which changes TimeOrders and the {@link #queueDelayedOrders}
   * regarding changing also for and because {@link TimeOrder#activateAt(long, long)}, which uses this as mutex.
   * <br><br>
   * If a TimeOrder is expired:
   * <br>If the {@link #evDst()} is given the {@link EventConsumer#processEvent(java.util.EventObject)} is called
   * though this instance may be a timeOrder. This method can enqueue this instance in another queue for execution
   * in any other thread which invokes then {@link TimeOrder#doExecute()}.
   * <br> 
   * It this is a {@link TimeOrder} and the #evDst is not given by constructor
   * then the {@link TimeOrder#doExecute()} is called to execute the time order.
   * <br>
   * If this routine is started then an invocation of {@link #activate(int)} etc. enqueues this instance newly
   * with a new time for elapsing. It is executed newly therefore.
   * @return the delay time in ms.
   */
  private int checkTimeOrders(){
    int timeWait = this.delayMax; //10 seconds.
    this.timeCheckNew = System.currentTimeMillis() + timeWait;  //the next check time in 10 seconds as default if no event found 
    TimeOrder order;
    //System.out.print("$" + this.queueDelayedOrders.size());
    synchronized(this) {                         // operations executed under mutex, synchronized to TimeOrder.activateAt
      Iterator<TimeOrder> iter = this.queueDelayedOrders.iterator();
      long timeNow = System.currentTimeMillis();
      while(iter.hasNext()) {
        order = iter.next();
        long delay = order.timeExecution - timeNow; 
        if((delay) < 3){                                   //if it is expired in <=2 milliseconds, execute now.
          iter.remove();
          order.timeExecutionLatest = 0;                   // Note: set first before timeExecution = 0. Thread safety.
          order.timeExecution = 0;                         // may force newly adding if requested. Before execution itself!
          this.queueOrdersToExecute.offer(order);
        }
        else {
          //not yet to proceed
          if(delay < timeWait) {                           // calculate the timeWait, for the first event.
            this.timeCheckNew = order.timeExecution;  //earlier
            timeWait = (int) delay;
          }
        }                                        //                                     |
      }
    } //synchronized
    //System.out.println(LogMessage.timeMsg(System.currentTimeMillis(), "checkTimeOrders").toString() + " timeCheckNew = " + LogMessage.timeMsg(this.timeCheckNew,"") );
    //System.out.print("°" + this.queueDelayedOrders.size());
    // ====================================================== The queueDelayedOrders is evaluated, now execute outside sync.
    //                                                        possible, that the same TimeOrder is queued again via TimeOrder.activateAt(...)
    while( (order = this.queueOrdersToExecute.poll()) !=null){    // that can be especially done just in the processEvent execution!
      if(order.timerThread != order.event.evDstThread) {
        //System.out.println(LogMessage.timeCurr("timeOrder send event: ") + order.event.name + LogMessage.msgSec(" time execution= ", order.timeExecution));  //+ ExcUtil.stackInfo("", 2, 8));
        order.event.sendEvent();                           // it is enqueued in the evThread
      } else {
        //System.out.println(LogMessage.timeCurr("timeOrder process event: ") + order.event.name + LogMessage.msgSec(" time execution= ", order.timeExecution));  //+ ExcUtil.stackInfo("", 2, 8));
        order.event.processEvent();                        // it is executed immediately in the timerThread if evThread is the same
      }
    }
    //======================================================= During processEvent new TimeOrders may be added.
    //                                                        It can be change the timeCheckNew. Hence new calculation. 
    long timeWait1 = this.timeCheckNew - System.currentTimeMillis();
    if(timeWait1 < timeWait) {
      timeWait = (int)timeWait1;
    }
    if(timeWait <2)
      Debugutil.stop();
    if(timeWait > 500) {
      timeWait = 500;
    }
    return timeWait;                                       // this is the sleep time for the thread.
  }
  
  
  
  /**The core run routine for the timer thread.
   * The timer thread is in an delay till
   * @return time to wait. 
   */
  protected final int stepThread()
  {
    boolean bExecute;
    int timeWait;
    do {
      stateThreadTimer = 'c';
      //----------------------------------------------------- check all time order, timeWait is the minimal time for next call.
      timeSleep = System.currentTimeMillis();
      timeWait = (int)(this.timeCheckNew - timeSleep);     // use timeCheckNew for decision look in all time orders.
      if(timeWait < 0){                                    // check all time orders only if at least one of them is expired.
        timeWait = checkTimeOrders();                      // execute expired events, calculate new waiting time
      }
      //----------------------------------------------------- check all current stored events.
      bExecute = false;
      while(checkEventAndRun()){
        bExecute = true;
      }
      if(bExecute){
        //else: check the orders and events newly. One of them may near to execute.
        if((debugPrint & 0x0002)!=0) System.out.printf("TimeOrderMng not wait %d\n", timeWait);
      }
      //if any event was executed, it should be supposed that 2.. milliseconds have elapsed.
      //therefore check time newly. don't wait, run in this loop.
    } while(bExecute);
    //wait only the calculated timeWait if no additional event has executed.
    if((debugPrint & 0x0001)!=0) System.out.printf("TimeOrderMng wait %d\n", timeWait);
    if(timeWait <2){
      timeWait = 2;  //should not 0  
    }
    return timeWait;
  }
  
  
  
  
  
  /**Instance as Runnable contains invocation of {@link EventTimerThread#stepThread()}
   * and the {@link Object#wait()} with the calculated timeWait.
   * Note desired for overridden implementation.
   */
  //NOTE: On debugging and changing the stepThread can be repeated because the CPU stays in the wait or breaks here.
  // This routine cannot be changed on the fly.
  private Runnable runTimer = new Runnable(){
    @Override public void run(){ 
      EventTimerThread.this.bThreadRun = true;
      EventTimerThread.this.stateThreadTimer = 'r';
      while(EventTimerThread.this.stateThreadTimer == 'r' && EventTimerThread.this.bThreadRun ){
        int timeWait = stepThread();
        synchronized(EventTimerThread.this){
          EventTimerThread.this.stateThreadTimer = 'W';
          //====>wait
          //System.out.println(org.vishia.msgDispatch.LogMessage.timeMsg(System.currentTimeMillis(), String.format("runTimer, timeWait=%d\n", timeWait)));
          try{ EventTimerThread.this.wait(timeWait);} catch(InterruptedException exc){}
          if(EventTimerThread.this.stateThreadTimer == 'W'){ //can be changed while waiting, set only to 'r' if 'W' is still present
            EventTimerThread.this.stateThreadTimer = 'r';
          }
        } //synchronized

      } //while runs
      EventTimerThread.this.stateThreadTimer = 'f';
    }
  };

  /**Returns true if the current thread is the thread which is aggregate to this EventThread.
   * It means the {@link #run()} method has called this method.
   * @return false if a statement in another thread checks whether this EventThread runs.
   */
  public final boolean isCurrentThread() {
    return this.threadTimer == Thread.currentThread();
  }
  
  /**Returns the current state of the thread.
   * <ul>
   * <li>?: Thread never started
   * <li>c: executes an order
   * <li>W: waits, don't have the processor
   * <li>f: finished
   * </ul>
   */
  public final char getState(){ return this.stateThreadTimer; }



  /**Wakes up the {@link #runTimer} queue to execute delayed requests.
   * <br>This operation can be overridden for another thread organization. 
   */
  public void notifyTimer(){
    synchronized(runTimer){
      if(stateThreadTimer == 'W'){
        runTimer.notify();  
      }
    }
  }
  
  
  
  /**Info for debugging 
   * <br>This operation can be overridden for another thread organization. 
   *
   */
  @Override public CharSequence infoAppend(StringBuilder u) {
    if(u == null) { u = new StringBuilder(); }
    u.append("Thread ").append(this.threadName);
    u.append("; ");
    return u;
  }

  /*no: Returns only the thread name. Note: Prevent recursively call for gathering info.
   */
  @Override public String toString() { if(preserveRecursiveInfoAppend) return threadName; else return infoAppend(null).toString(); } 
  
  
}
