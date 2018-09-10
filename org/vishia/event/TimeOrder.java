package org.vishia.event;

import java.util.EventObject;


/**This is the base class for time orders.
 * To define a time order a derived class of this should be written which overrides the method 
 * {@link #executeOrder()}. An instance of this class can be created statically as persistent reference
 * or on demand with new {@link TimeOrder#TimeOrder(String, EventTimerThread)}. The {@link EventTimerThread} should 
 * be existing as instance before an instance of this class is created.
 * <br><br>
 * <b>Pattern for application</b>:<br><pre>
 * EventThread thread = new EventThread("name");
 * ...
 * //a time order as inner anonymous class:
 * EventTimeOrder order = new EventTimeOrder("name", thread) {
 *   QOverride protected void executeOrder(){
 *     ...code for time order
 *   }
 * }
 * ...
 * thread.start();
 * order.activate(100); //the order will be executed in the thread in 100 ms.
 * </pre>
 * <b>An EventTimeOrder can be enqueued in the EventThread but executed in another thread.</b> 
 * It uses an {@link EventConsumer}, see {@link org.vishia.event.test.TestTimeOrder}, with the following pattern</b>:
 * <pre>
 *  
 * private static class EnqueueInGraphicThread implements EventConsumer {
 *   QOverride public int processEvent(EventObject ev)
 *   { execThread.queueOrders.put((< EventTimeOrder >)ev);  //casting admissible because special using.
 *     return mEventConsumed;
 *   }
 * }
 * </pre>
 * This class is designated as EventObject because it can be used as event immediately after 
 * or independent whether it is handled by the timer.
 * <br><br>
 * <ul>
 * <li>Constructors: {@link TimeOrder#TimeOrder(String, EventTimerThread)},
 *   {@link TimeOrder#TimeOrder(String, EventConsumer, EventTimerThread)}, {@link TimeOrder#TimeOrder(String)} .
 * <li>methods to activate from {@link EventTimeout}: {@link #activate(int)}, {@link #activateAt(long)}, {@link #activateAt(long, long)}
 * <li>Remove a currently timeout: {@link #deactivate()}
 * <li>Check from {@link EventTimeout}: {@link #timeExecution()}, {@link #timeExecutionLatest()}, {@link #timeToExecution()} {@link #used()}
 * <li>Check: {@link #getCtDone(int)}
 * <li>Execute: {@link #doExecute()} will be called from the {@link EventTimerThread}, only call by application in a special thread. 
 * <li>Wait for execution in another thread: {@link #awaitExecution(int, int)}.
 * <li>see {@link EventTimeout}.
 * <li>see {@link EventTimerThread}
 * <li>see {@link org.vishia.states.StateMachine}
 * </ul>
 * @author Hartmut Schorrig
 *
 */
public abstract class TimeOrder extends EventTimeout //Object //implements EventConsumer
{

  private static final long serialVersionUID = 1998821310413113722L;


  /**Version and history.
   * <ul>
   * <li>2015-05-03 Hartmut on {@link #awaitExecution(int, int)} wait if the thread is busy. It should be done
   *   especially if the order is in debug. Not ready checked yet whether it is well running.
   * <li>2015-01-10 Hartmut renamed from <code>OrderForList</code>
   * <li>2014-02-23 Hartmut removed from the component srcJava_vishiaGui to the srcJava_vishiaBase 
   *   because it is commonly able to use.
   * <li>2012-02-14 Hartmut corr: {@link #addToList(GralGraphicThread, int)}:
   *   For time saving: If an instance is added already and its new execution time
   *   is up to 5 ms later, nothing is done. It saves time for more as one call of this routine
   *   in a fast loop.
   * <li>2012-02-14 Hartmut corr: It was happen that an instance was designated with {@link #bAdded}==true
   *   but it wasn't added, Therefore on {@link #addToList(GralGraphicThread, int)} it is removed
   *   from list and added newly. It is more save. 
   * <li>2012-01-26 Hartmut chg: rename removeFromGraphicThread(GralGraphicThread) 
   *   to {@link #removeFromList(GralGraphicThread)}. It is a better naming because it is removed from the queue
   *   in the graphic thread. This class is only used in that queue. 
   * <li>2012-01-15 Hartmut new: {@link #name} to identify, proper for debugging
   * <li>2012-01-08 Hartmut new: {@link #addToList(GralGraphicThread, int)} and 
   *   {@link #removeFromList(GralGraphicThread)} as thread-safe functions which 
   *   marks the instance as added (for delayed execution, for re-using).
   * <li>2010-06-00 Hartmut created.
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
  public final static String version = "2015-01-25";

  
  /**The name of the dispatch worker, used for debug at least. */
  public final String name;
  
  private int ctDone = 0;
  
  /**It is counted only. Used for debug. Possible to set. */
  public int dbgctDone = 0;
  
  /**True if a thread waits, see {@link #awaitExecution(int, int)}. */
  private boolean reqCtDone = false;

  //private boolean bAdded;
  
  
  
  public TimeOrder(String name)
  { super();
    this.name = name;
  }
  
  
  /**Initializes an time order for usage. The time order can be created dynamically on demand
   * or as permanent instance. Use {@link #activate(int)} etc. to activate it.
   * <br><br>
   * If the time is expired after {@link #activateAt(long, long)} then the {@link #executeOrder()}
   * is invoked in the thread. Note: Use {@link TimeOrder#EventTimeOrderBase(String, EventConsumer, EventTimerThread)}
   * if you want to execute the {@link #executeOrder()} routine in another thread. 
   * 
   * @param name the TimeOrder should have a name for debugging. 
   * @param thread thread to handle the time order. It is obligatory.
   */
  public TimeOrder(String name, EventTimerThread_ifc thread){
    super(null, thread);  //no EventSource necessary, no eventConsumer because this is an order.
    this.name = name;
  }
  

  /**Initializes an time order for usage. The time order can be created dynamically on demand
   * or as permanent instance. Use {@link #activate(int)} etc. to activate it.
   * <br><br>
   * If the time is expired after {@link #activateAt(long, long)} then the {@link EventConsumer#processEvent(EventObject)}
   * is invoked in the thread. This routine can store this instance in another queue of any other thread
   * to execute the {@link #executeOrder()} in any other thread.
   *  
   * @param name the TimeOrder should have a name for debugging. 
   * @param dst The destination object for the event. If it is null then the overridden {@link #executeOrder()} 
   *   will be executed if the time is expired. If given then {@link EventConsumer#processEvent(EventObject)}
   *   is invoked in the given dst instance. The #executeOrder is not invoked. It can be used in the dst for own things.
   * @param thread thread to handle the time order. It is obligatory.
   */
  public TimeOrder(String name, EventConsumer dst, EventTimerThread_ifc thread){
    super(dst, thread);  //no EventSource necessary, no eventConsumer because this is an order.
    this.name = name;
  }
  

  
  /**Returns the state of the consumer in a manual readable form. */
  //@Override 
  public String getStateInfo(){ 
    StringBuilder u = new StringBuilder(100);
    u.append(name);
    if(timeExecution >0){
      //TODO some information!
    }
    return u.toString();
  }
  

  
  /**Executes the order. This abstract method should be implemented in a user's instance.    
   * This method should not be called directly. It is called from {@link #doExecute()}. 
   */
  protected abstract void executeOrder();
  
  /**Executes and sets the execute state to false. A user should not invoke this method. 
   * Use {@link #activate(int)} etc. to activate the time order for execution.
   * <br>
   * This routine is invoked instead {@link #executeOrder()}, it wraps it.
   * It is called inside the package private {@link EventTimerThread} or from a special derived instance
   * which will be invoke {@link #executeOrder()} in any other thread.
   * <br>
   * If this routine is started then it is added newly on new invocation of {@link #activate(int)} etc.
   */
  public final void doExecute(){ 
    executeOrder();
    dbgctDone +=1;
    ctDone +=1;
    if(reqCtDone){
      synchronized(this){
        notify();
      }
    }
  }
  
  
  
  
  /**Gets the information, how many times the routine is executed.
   * Especially it is for quest, whether it is executed 1 time if it is a single-execution-routine.
   * @param setCtDone set the count for a new execution-counting. For example 0.
   * @return The number of times of execution after initializing or after last call of this method.
   */
  synchronized public int getCtDone(int setCtDone) {
    //reqCtDone = true;   //it is to notify, if this routine is called.
    int ctDone = this.ctDone;
    if(setCtDone >=0){ 
      this.ctDone = setCtDone; 
    }
    return ctDone;
  }
  
  

  
  /**Waits for execution in any other thread. This method can be called in any thread, especially in that thread, 
   * which initializes the request. Note that {@link #doExecute()} should be invoked to execute the time order
   * and notify this waiting routine. That is done if the {@link EventTimerThread} is used.
   * @param ctDoneRequested Number of executions requested.
   * @param timeout maximal waiting time in milliseconds, 0 means wait forever.
   * @return true if it is executed the requested number of.
   */
  public synchronized boolean awaitExecution(int ctDoneRequested, int timeout)
  { 
    long timeEnd = System.currentTimeMillis() + timeout; 
    boolean bWait;
    do {
      if(this.ctDone < ctDoneRequested ){
        reqCtDone = true;
        long waitingTime = timeEnd - System.currentTimeMillis();
        if(waitingTime > 0 || timeout == 0 || evDstThread.isBusy()) { //should wait, or special: wait till end of routine.
          try{ wait(timeout); } catch(InterruptedException exc){}
          bWait = true;
        } else bWait = false;
      } else bWait = false;
    } while(bWait);
    return(this.ctDone >= ctDoneRequested);
  }
  

  /**Returns the name of this to show it for debugging.
   */
  @Override public String toString(){ return name; }  

  
}
