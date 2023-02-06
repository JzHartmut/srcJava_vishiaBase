package org.vishia.event;

import java.io.Closeable;
import java.util.EventObject;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.vishia.util.ExcUtil;
import org.vishia.util.InfoAppend;

/**This class stores events, starts the processing of the events in one thread and manages and executes time orders in the same thread. 
 * An instance of this class is the main instance to execute state machines in one thread. 
 * The methods of this class except {@link #start()} and {@link #close()} are used from instances of {@link EventWithDst}, 
 * {@link EventTimeout} and {@link TimeOrder} internally.
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
 * This class is used as time manager too. It manages and executes {@link EventTimeout} 
 * which are used especially for {@link org.vishia.states.StateMachine},
 * but it can execute {@link TimeOrder} in this thread too. To add a timeout event or a time order 
 * use the methods of the {@link EventTimeout#activateAt(long)} etc:
 * <pre>
 * 
  TimeOrder myTimeOrder = new TimeOrder("name", myExecutionThread) {
    QOverride protected void executeOrder(){ ...execution of the time order ...}
  };
  ...
  myTimeOrder.activate(100);  //in 100 milliseconds.
 * </pre> 
 * That routine invokes the routine {@link #addTimeOrder(EventTimeout)} of this class.
 * <br><br>
 * This class starts a thread. The thread sleeps if no event or time order is given. 
 * <br><br>
 * Events are stored in a {@link java.util.concurrent.ConcurrentLinkedQueue} which is thread-safe to enqueue events
 * from any thread with the method {@link #storeEvent(EventObject)}.
 * Then {@link Object#notify()} is called to weak up the thread if it sleeps. Then the all stored events are dequeued
 * and its execution routine of {@link EventConsumer#processEvent(EventObject)} is invoked. The events are processed one after another.
 * The execution routine is usual a {@link org.vishia.states.StateMachine} but any other {@link EventConsumer} is able to use too. 
 * <br><br>
 * {@link EventTimeout} are stored in another {@link java.util.concurrent.ConcurrentLinkedQueue}. 
 * The absolute time of the next execution is stored in the internal value of {@link #timeCheckNew}. The thread sleeps either
 * till this time is expired or till an event is given. If the time is expired all stored time orders are checked
 * whether its time is elapsed. Then either the {@link EventConsumer#processEvent(EventObject)} is invoked 
 * if an {@link EventTimeout} is given or a {@link TimeOrder} has a destination. Elsewhere the {@link TimeOrder#doExecute} 
 * is invoked to execute the {@link TimeOrder#executeOrder()} in this thread.
 * <br><br>
 * An event can be removed from the queue if it is not executed up to now. The routine {@link #removeFromQueue(EventObject)}
 * is invoked if an event should be {@link EventWithDst#occupyRecall(EventSource, boolean)}. That is if the event should be used
 * newly.
 * <br><br>
 * Note: An {@link EventTimeout} can be removed from execution with the method {@link EventTimeout#deactivate()}.
 * Adequate is done with {@link #removeTimeOrder(EventTimeout)}.
 * 
 *   
 * @author Hartmut Schorrig
 *
 */
@SuppressWarnings("synthetic-access") 
public class EventTimerThread implements EventTimerThread_ifc, Closeable, InfoAppend
{
  
  
  /**Version and history.
   * <ul>
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
  private final ConcurrentLinkedQueue<EventTimeout> queueDelayedOrders = new ConcurrentLinkedQueue<EventTimeout>();
  
  /**Temporary used instance of delayed orders while {@link #runTimer} organizes the delayed orders.
   * This queue is empty outside running one step of runTimer(). */
  private final ConcurrentLinkedQueue<EventTimeout> queueDelayedTempOrders = new ConcurrentLinkedQueue<EventTimeout>();
  
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
   * be notified in {@link #addTimeOrder(EventTimeout)} with delayed order. */
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
  @Override public final void storeEvent(EventObject ev){
    if(ev instanceof EventWithDst) { ((EventWithDst)ev).stateOfEvent = 'q'; }
    this.queueEvents.offer(ev);
    startOrNotify();
  }
  

  private void startOrNotify(){
    if(threadTimer == null){
      createThread_();
      startOnDemand = true;
    } else {
      synchronized(runTimer){
        if(stateThreadTimer == 'W'){
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
  public final void addTimeOrder(EventTimeout order){ 
    long delay = order.timeToExecution(); 
    if(delay >=0){
      String sorder = order.toString();
      if(sorder.equals("showFilesProcessing"))
        System.out.println("addTimeOrder;" + order.toString());
      this.queueDelayedOrders.offer(order);
      long delayAfterCheckNew = order.timeExecution - this.timeCheckNew;
      if((delayAfterCheckNew) < -2) {  //an imprecision of 2 ms are admissible, don't wakeup because calculation imprecisions.
        this.timeCheckNew = order.timeExecution;  //earlier.
        boolean notified;
        synchronized(runTimer){
          notified = stateThreadTimer == 'W';
          if(notified){                          // wake up the waiting thread to poll orders
            runTimer.notify();                   // elsewhere it would sleep till the last decided sleep time. 
          } else {                               // should wake up and adjust the sleep time newly.
            // thread is busy, not wait          
          }
        }
        if(notified){
          if((debugPrint & 0x100)!=0) System.out.printf("TimeOrderMng notify %d\n", delayAfterCheckNew);
        } else {
          if((debugPrint & 0x200)!=0) System.out.printf("TimeOrderMng not notified because checking %d\n", delayAfterCheckNew);
        }
      } else {
        //don't notify because the time order is later than the planned check time (or not so far sooner)
        if((debugPrint & 0x400)!=0) System.out.printf("TimeOrderMng not notified, future %d\n", delayAfterCheckNew);
      }
    } else {
      if((debugPrint & 0x800)!=0) System.out.printf("TimeOrderMng yet %d\n", delay);
      order.doTimeElapsed();
    }
  }
  
  
  /**Removes a time order, which was activated but it is not in the event execution queue.
   * If the time order is expired and it is in the event execution queue already, it is not removed.
   * This operation is thread safe. Either the event is found and removed, or it is not found because it is
   * either in execution yet or it is not queued. The event queue is a {@link ConcurrentLinkedQueue}.
   * 
   * @param order
   */
  public final boolean removeTimeOrder(EventTimeout order)
  { boolean found = queueDelayedOrders.remove(order);
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
  private final void applyEvent(EventObject ev)
  {
    if(ev instanceof EventWithDst){
      EventWithDst event = (EventWithDst) ev;
      event.stateOfEvent = 'e';
      event.notifyDequeued();
      int retProcess = 0;  //check doNotRelinquish, relinquishes it in case of exception too!
      try{
        //event.donotRelinquish = false;   //may be overridden in processEvent if the event is stored in another queue
        event.stateOfEvent = 'r';
        EventConsumer dst = event.evDst;
        if(dst != null ) { //&& ev instanceof TimeOrder) {
//          ((TimeOrder)ev).executeOrder();   //relinquish it.
//        } else {
          retProcess = dst.processEvent(event);  //may set bit doNotRelinquish 
        }
      } catch(Exception exc) {
        CharSequence excMsg = ExcUtil.exceptionInfo("EventThread.applyEvent exception", exc, 0, 50);
        System.err.append(excMsg);
        //exc.printStackTrace(System.err);
      }
      //if(event.stateOfEvent == 'r') {  //doNotRelinquish was not invoked inside processEvent().
      if( (retProcess & EventConsumer.mEventDonotRelinquish) ==0) {
        //Note: relinquishes it in case of exception too!
        event.relinquish();  //the event can be reused, a waiting thread will be notified.
      }
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
      EventObject event;
      if( (event = queueEvents.poll()) !=null){
        this.ctWaitEmptyQueue = 0;
        synchronized(this){
          if(stateThreadTimer != 'x'){
            stateThreadTimer = 'b'; //busy
          }
        }
        if(stateThreadTimer == 'b'){
          applyEvent(event);
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
   * @return the delay time in ms.
   */
  private int checkTimeOrders(){
    int timeWait = this.delayMax; //10 seconds.
    this.timeCheckNew = System.currentTimeMillis() + timeWait;  //the next check time in 10 seconds as default if no event found 
    { EventTimeout order;
      long timeNow = System.currentTimeMillis();
      while( (order = queueDelayedOrders.poll()) !=null){
        long delay = order.timeExecution - timeNow; 
        if((delay) < 3){                         //if it is expired in <=2 milliseconds, execute now.
          order.doTimeElapsed();                 // execute the event now.
          timeNow = System.currentTimeMillis();  //new time after execution for further check
        }
        else {
          //not yet to proceed
          if(delay < timeWait) {                 // calculate the timeWait, for the first event.
            this.timeCheckNew = order.timeExecution;  //earlier
            timeWait = (int) delay;
          }
          queueDelayedTempOrders.offer(order);   // gather this order firstly in the tempOrders
        }                                        //                                     |
      }                                          //                                     |
      //delayedChangeRequest is tested and empty now.                                   |
      //copy the non-expired orders back to queueDelayedOrders.                         |
      while( (order = queueDelayedTempOrders.poll()) !=null){    // <-------------------+
        queueDelayedOrders.offer(order); 
      }
    }
    return timeWait;
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
        synchronized(EventTimerThread.this.runTimer){
          EventTimerThread.this.stateThreadTimer = 'W';
          //====>wait
          try{ EventTimerThread.this.runTimer.wait(timeWait);} catch(InterruptedException exc){}
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
