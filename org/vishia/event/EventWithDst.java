package org.vishia.event;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EventObject;
import java.util.concurrent.atomic.AtomicLong;

import org.vishia.util.DateOrder;

public class EventWithDst extends EventObject
{

  private static final long serialVersionUID = 3976120105528632683L;
  
  
  public EventWithDst(Object source)
  { super(source);
  }

  /**Version, history and license
   * <ul>
   * <li>2015-01-03 Hartmut chg: Separated in 2 classes: {@link EventCmdPingPongType} with opponent and this class.
   * <li>2015-01-03 Hartmut chg: Renamed to EventMsg: more significant name. Derived from EventObject: A basicly Java concept.
   * <li>2013-10-06 Hartmut chg: Some checks for thread safety.
   * <li>2013-10-06 Hartmut chg: {@link #occupy(int, EventSource, EventConsumer, EventThread)} with timeout
   * <li>2013-10-06 Hartmut chg: {@link #occupyRecall(EventSource, boolean)} return 0,1,2, not boolean.
   *   The state whether the recalled event is processed or it is only removed from the queued, may
   *   be important for usage. 
   * <li>2013-05-11 Hartmut new: {@link #Event(Enum)} to create an event for direct usage.
   *   {@link #cmde} does not need to be an Atomic, because {@link #dateCreation} is Atomic
   *   to designate the occupy-state of the event. 
   * <li>2013-04-12 Hartmut chg: Gardening. The attributes data1, data2, oData, refData are removed. Any special data
   *   should be defined in any derived instance of the event. A common universal data concept may be error-prone
   *   because unspecified types and meanings.
   * <li>2013-04-07 Hartmut chg: The Event class has 2 generic parameters up to now, the second for the opponent Event. 
   * <li>2012-11-16 Hartmut chg: An event is not occupied on construction if either the src or the dst is null. 
   *   Only if both references are given, it is occupied by construction.
   * <li>2012-09-12 Hartmut new: {@link #sendEventAgain()} for deferred events.
   * <li>2012-09-03 Hartmut chg: using {@link DateOrder} to log the date in milliseconds and the order as fine number.
   *   The order of events should be known. The timestamp is imprecise!
   * <li>2012-08-30 Hartmut new:  Some substantial enhancements for usage:
   *   <ul>
   *   <li>re-engineering for {@link #occupy(EventSource, boolean)}
   *   <li>Test and simplification of some use cases.
   *   <li>Meaning of the source of events, debug helping
   *   <li>documentation
   *   </ul>
   * <li>2012-08-03 Hartmut chg: Usage of Event in FileRemote. The event has more elements for forward and backward now.
   * <li>2012-07-28 renamed src, now {@link #refData}. It is not the source (creator) of the event
   *   but a value reference which may be used especially in the callback ({@link #callback}).
   *   Because it is private and the getter method {@link #getRefData()} is duplicated, the
   *   old routine {@link #getSrc()} is deprecated, it is downward compatible still. 
   * <li>2012-03-10 Hartmut new: {@link #owner}, {@link #forceRelease()}. 
   *   It is a problem if a request may be crashed in a remote device, but the event is reserved 
   *   for answer in the proxy. It should be freed. Events may be re-used. 
   * <li>2012-01-22 Hartmut chg: {@link #use(long, int, Object, EventConsumer)} needs the dst as parameter.
   * <li>2012-01-05 Hartmut improved: {@link #callbackThread}, {@link #commisionId} instead order, more {@link #data2} 
   * <li>2011-12-27 Hartmut created, concept of event queue, callback need for remote copy and delete of files
   *   (in another thread too). A adequate universal class in java.lang etc wasn't found.
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
  public static final String version = "2015-01-03";

  
  /**The current owner of the event. It is that instance, which has gotten the event instance.
   */
  EventSource sourceMsg;
  
  
  /**The queue for events of the {@link EventThread} if this event should be used
   * in a really event driven system (without directly callback). 
   * If it is null, the dst. {@link EventConsumer#processEvent(EventMsg)} should be called immediately. */
  EventThreadIfc evDstThread;
  
  
  /**The destination instance for the Event. If the event is stored in a common queue, 
   * the dst is invoked while polling the queue. Elsewhere the dst is the callback instance. */
  EventConsumer evDst;
  
  /**The queue for events of the {@link EventThread} if this event should be used
   * in a really event driven system (without directly callback). 
   * If it is null, the dst.{@link EventConsumer#processEvent(Event)} should be called immediately. */
  //private EventThread callbackThread;
  
  
  /**The destination instance for the Event. If the event is stored in a common queue, 
   * the dst is invoked while polling the queue. Elsewhere the dst is the callback instance. */
  ///*package private*/ EventConsumer callback;
  
  /**State of the event: 
   * <ul>
   * <li>0 or '.': unused.
   * <li>a: requested or allocated. The {@link EventConsumer} and the {@link EventThread} is set, but the event 
   *   is not in send to the consumer. It is not in a queue and not in process.
   * <li>q: queued in dstThread
   * <li>e: executing
   * <li>B: queued for callback
   * <li>b: callback invoked
   * 
   * </ul> 
   */
  public char stateOfEvent;
  
  /**package private*/ boolean donotRelinquish;
  
  boolean bAwaitReserve;
  
  //protected int answer;
  
  protected int ctConsumed;
  
  /**The commission number for the request, which may be answered by this event. */
  protected long orderId;
  
  /**Timestamp of the request. It is atomic because the timestamp may be an identification
   * that the event instance is occupied, see {@link #occupy(EventSource, boolean)}.
   * It is for new-obviating usage. */
  protected final AtomicLong dateCreation = new AtomicLong();
  
  int dateOrder;
  
  /**Any value of this event. Mostly it is a return value. */
  //public int data1, data2;
  
  

  
  /**Creates an event as a static object for re-usage. Use {@link #occupy(Object, EventConsumer, EventThread)}
   * before first usage. Use {@link #relinquish()} to release the usage. 
   * 
   */
  public EventWithDst(){
    super(EventSource.nullSource);
    dateCreation.set(0);
  }
  
  
  
  /**Creates an event as dynamic object for usage. Use {@link #relinquish()} after the event is used and it is not referenced
   * anymore. 
   * @param source Source of the event. If null then the event is not occupied, especially the {@link #dateCreation()} 
   *   is set to 0.
   * @param refData Associated data to the event. It is the source of the event.
   * @param consumer The destination object for the event.
   * @param thread an optional thread to store the event in an event queue, maybe null.
   */
  public EventWithDst(EventSource source, EventConsumer consumer, EventThreadIfc thread){
    super(EventSource.nullSource);
    if(source == null){
      this.dateCreation.set(0);
    } else {
      super.source = source;
      DateOrder date = new DateOrder();
      this.dateCreation.set(date.date);
      this.dateOrder = date.order;
    }
    this.sourceMsg = source;
    this.evDst = consumer; this.evDstThread = thread;
  }
  
  
 
  public void setOrderId(long order){ orderId = order; }
  
  
  
  
  /**Prevent that the event is relinquished after processing.
   * This method should be called in the processing routine of an event
   * only if the event will be stored in another queue to execute delayed.
   */
  public void donotRelinquish(){ donotRelinquish = true;}
  
  /**This method should only be called if the event should be processed.
   * The {@link #donotRelinquish()} will be set to false, so that the event will be relinquished
   * except {@link #donotRelinquish()} is called while processing the event. 
   * @return The event consumer to call {@link EventConsumer#processEvent(EventMsg)}.
   */
  public EventConsumer evDst() { donotRelinquish = false; return evDst; }
  
  
  /**Returns the time stamp of creation or occupying the event.
   * @return null if the event is not occupied, a free static object.
   */
  public Date dateCreation(){ long date = dateCreation.get(); return date == 0 ? null : new Date(date); }
  
  
  
  
  
  public boolean hasDst(){ return evDst !=null; }
  
  
  public void setDst(EventConsumer dst) { this.evDst = dst; }
  
  
  
  /**Check whether this event is free and occupies it. An event instance can be re-used. 
   * If the {@link #dateCreation()} is set, the event is occupied. In a target communication 
   * with embedded devices often the communication resources are limited. 
   * It means that only one order can be requested at one time, and the execution
   * should be awaited. The usage of an re-used event for such orders can be help to organize the
   * requests step by step. If the answer-event instance is in use, a request is pending.
   *  
   * @param source Source instance able to use for monitoring the event life cycle. null is admissible.
   * @param dst The destination instance which should receive this event.
   *   If null, the dst given by constructor or the last given dst is used.
   * @param thread A thread which queues the event. If null and dst !=null then the dst method 
   *   {@link EventConsumer#processEvent(EventMsg)} is invoked in the current thread. If dst ==null this parameter is not used.
   * @param expect If true and the event is not able to occupy, then the method {@link EventSource#notifyShouldOccupyButInUse()} 
   *   from the given source is invoked. It may cause an exception for example. 
   * @return true if the event instance is occupied and ready to use.
   */
  public boolean occupy(EventSource source, EventConsumer dst, EventThread thread, boolean expect){ 
    DateOrder date = new DateOrder();
    if(dateCreation.compareAndSet(0, date.date)){
      dateOrder = date.order;
      super.source = this.sourceMsg = source;
      this.ctConsumed =0;
      //if(refData !=null || dst !=null) { this.refData = refData; }
      if(dst != null) { 
        this.evDst = dst;
        this.evDstThread = thread;
      }
      
      this.stateOfEvent = 'a';
      return true;
    }
    else {
      if(expect){
        notifyShouldOccupyButInUse();
      }
      return false;
    }
  }
  
  
  
  /**Try to occupy the event. If it is in use yet, the thread waits the given timeout.
   * This waiting is used for thread switch, process the event and release it.
   * An event may be used only as a transport data from one thread to another.
   * The event is relinquished usual in a less time after the other thread has processed it.
   * The event can be re-used after them for another request. But it should wait a moment
   * to force thread switching. 
   *   
   * @param timeout 
   * @param evSrc
   * @param dst
   * @param thread
   * @return true if occupied, false if the other thread which should process the event hangs.
   */
  public boolean occupy(int timeout, EventSource evSrc, EventConsumer dst, EventThread thread){
    boolean bOk = occupy(evSrc, dst, thread, false);
    if(!bOk){
      synchronized(this){
        bAwaitReserve = true;
        try{ wait(timeout); } catch(InterruptedException exc){ }
        bAwaitReserve = false;
        bOk = occupy(sourceMsg, dst, thread, false);
      }
    }
    return bOk;
  }
  
  
  
  
  public boolean occupy(EventSource source, boolean expect){ return occupy(source, null, null, expect); } 
  
  //public boolean occupy(EventSource source, boolean expect){ return occupy(source, null, null, null, expect); } 
  
  
  /**Try to occupy the event for usage, recall it if it is in stored in an event queue.
   * <ul>
   * <li>If the event is free, then it is occupied, the method returns immediately with true. 
   * <li>If it is not free, but stored in any queue, it will be removed from the queue,
   *   then occupied for this new usage. The method returns imediately with true.
   * <li>If it is used and not found in any queue, then it is processed in this moment.
   *   Then this method returns false. The method doesn't wait. 
   *   See {@link #occupyRecall(int, Object, EventConsumer, EventThread)}.   
   * </ul>
   * @param source Source instance able to use for monitoring the event life cycle. null is admissible.
   * @param dst The destination instance which should receive this event.
   *   If null, the dst given by constructor or the last given dst is used.
   * @param thread A thread which queues the event. If null and dst !=null then the dst method 
   *   {@link EventConsumer#processEvent(EventMsg)} is invoked in the current thread. If dst ==null this parameter is not used.
   * @param expect If true and the event is not able to occupy, then the method {@link EventSource#notifyShouldOccupyButInUse()} 
   *   from the given source is invoked. It may cause an exception for example. 
   * @return true if the event is occupied.
   */
  public boolean occupyRecall(EventSource source, EventConsumer dst, EventThread thread, boolean expect){ 
    boolean bOk = occupy(source, dst, thread, false);
    if(!bOk){
      if(evDstThread !=null){
        bOk = evDstThread.removeFromQueue(this);
        if(bOk){
          //it was in the queue, it means it is not in process.
          //therefore set it as consumed.
          relinquish();
          bOk = occupy(source, dst, thread, false);
        }
      }
    }
    if(!bOk && expect){
      notifyShouldOccupyButInUse();
    }
    return bOk;
  }
  
  public boolean occupyRecall(EventSource source, boolean expect){ return occupyRecall(source, null, null, expect); } 
  
  //public boolean occupyRecall(EventSource source, boolean expect){ return occupyRecall(source, null, null, null, expect); } 
  
  
  
  
  /**Try to occupy the event for usage, recall it if it is in stored in an event queue, wait till it is available.
   * This method may block if the event is yet processing. The method blocks only for the given timeout. 
   * If the event process hangs then this method returns false and the event is not occupied. This situation is not occurring
   * if the event is stored in its event queue because this routine removes it from the queue. It can be only occurred if the event process
   * is really in a loop or it is in a lower priority thread which has not calculation time. That is a specific situation.
   * The caller should be inform a user about that situation with a message or adequate because such a situation can be solved
   * often with specific handling on the whole application. It is possible and maybe recommended firstly waiting a longer timeout.
   * <ul>
   * <li>If the event is free, then it is occupied, the method returns immediately with 1.
   *   The last usage of the event is processed in this case. 
   * <li>If it is not free, but stored in any queue, it will be removed from the queue,
   *   then occupied for this new usage. The method returns immediately with 2.
   *   It means the last cmd is not processed.
   * <li>If the event is occupied already and not found in any queue, then it seems to be processed in this moment.
   *   This method waits the given timeout till the event is free. If it will be free in the timeout period,
   *   the method occupies it and returns 1. 
   * <li>If the timeout is expired, it is checked whether the timestamp of the occupying of the event is lesser than the timestamp
   *   on starting wait. If it is so, the event hangs. That is especially if the event process has thrown an exception and the 
   *   event was not relinquished therefore. The event would hang forever. Therefore the {@link #dateCreation()} is forced to 0,
   *   so the event is free now. Then it is occupied from this routine.
   * <li>If that occupying fails too, it is only possible that another process has occupied it too.
   * <li>If the event cannot be occupied the method returns 0. That may be an unexpected situation, because the 
   *   processing of an event should be a short non-blocking algorithm. It may be a hint to an software error. 
   *   one may try again occupy with an increased timeout. The processing may need more time.
   *   It is possible that the processing of the event hangs (deadlock). 
   * </ul>
   * See {@link #occupyRecall(Object, EventConsumer, EventThread)}.   
   * @param timeout maximal millisecond to wait if the event is yet in processing.
   * @param source Source instance able to use for monitoring the event life cycle. null is admissible.
   * @param dst The destination instance which should receive this event.
   *   If null, the dst given by constructor or the last given dst is used.
   * @param thread A thread which queues the event. If null and dst !=null then the dst method 
   *   {@link EventConsumer#processEvent(EventMsg)} is invoked in the current thread. If dst ==null this parameter is not used.
   * @param expect If true and the event is not able to occupy, then the method {@link EventSource#notifyShouldOccupyButInUse()} 
   *   from the given source is invoked. It may cause an exception for example. 
   * @return 
   *   <ul>
   *   <li>0 if the event is blocked because it is in process for the timeout time, 
   *   <li>!= 0 if the event is occupied:
   *   <li>1 if the event was free. 
   *   <li>2 if the event is removed from another queue. It means the last one request is not done.
   *   <li>3 if the event is forced relinguish because it was hanging.
   *   </ul> 
   */
  public int occupyRecall(int timeout, EventSource source, EventConsumer dst, EventThread thread, boolean expect){
    int ok = 0;
    boolean bOk = occupy(source, dst, thread, false);
    if(bOk){ ok = 1; }
    if(!bOk){
      if(evDstThread !=null){
        bOk = evDstThread.removeFromQueue(this);
        if(bOk){
          //it was in the queue, it means it is not in process.
          //therefore set it as consumed.
          relinquish();
          bOk = occupy(source, dst, thread, false);
          if(bOk){ ok = 2; }
        }
      }
    }
    if(!bOk){
      long time1 = System.currentTimeMillis();
      synchronized(this){
        bAwaitReserve = true;
        try{ wait(timeout); } catch(InterruptedException exc){ }
        bAwaitReserve = false;
      }//synchronized
      bOk = occupy(source, dst, thread, false);
      if(bOk){ ok = 1; }
      else {
        synchronized(this){
          long timeCreation = dateCreation.get();
          if(!bOk && timeCreation !=0 && (timeCreation - time1) < 0) {
            //the event is not occupied newly. It means it hangs usual because an exception which has prevented relinquish():
            //force new usage:
            donotRelinquish = false;
            bAwaitReserve = false;
            relinquish();
            bOk = occupy(source, dst, thread, false);
          }
        }//synchronized
        if(bOk){ ok = 3; }
      }
    }
    if(!bOk && expect){
      notifyShouldOccupyButInUse();
    }
    return ok;
  }
  
  
  /**Try to occupy the event for usage, recall it if it is in stored in an event queue, wait till it is available.
   * Same as {@link #occupyRecall(int, EventSource, EventConsumer, EventThread, boolean)} but left the destination unchanged.
   * @param timeout maximal millisecond to wait if the event is yet in processing.
   * @param source Source instance able to use for monitoring the event life cycle. null is admissible.
   * @param expect If true and the event is not able to occupy, then the method {@link EventSource#notifyShouldOccupyButInUse()} 
   *   from the given source is invoked. It may cause an exception for example. 
   * @return 0 if the event is blocked because it is in process for the timeout time, != 0 if the event is occupied.
   *   1 if the event was free. 2 if the event is removed from another queue. It means the last one request is not done. 

   */
  public int occupyRecall(int timeout, EventSource source, boolean expect){ return occupyRecall(timeout, source, null, null, expect); } 
  
  //public boolean occupyRecall(int timeout, EventSource source, boolean expect){ return occupyRecall(timeout, source, null, null, null, expect); } 
  
  

  
  
  
  /**Returns true if the event is occupied. Events may be re-used. That may be necessary if a non-dynamic
   * memory organization may be need, for example in C-like programming. It is possible anyway if actions
   * are done only one after another. In that kind the event instanc is created one time,
   * and re-used whenever it is needed. A re-using should wait for answer and set {@link #consumedRetention()}.
   * If the answer hangs, {@link #forceRelease()} may be called.
   * 
   * @return false if it is ready to re-use.
   */
  public boolean isOccupied(){ return dateCreation.get() !=0; }
  
  
  
  /**Relinquishes the event object. It is the opposite to the {@link #occupy(Object, EventConsumer, EventThread)} method.
   * The {@link #dateCreation} is set to 0 especially to designate the free-state of the Event instance.
   * The {@link #refData}, {@link #evDst()}, {@link #evDstThread} and {@link #opponent} are not changed
   * because the event may be reused in the same context.
   * All other data are reseted, so no unused references are hold. 
   * <br><br>
   * If any thread waits for this Event object, a {@link Object#notify()} is called. See {@link #occupyRecall(int)}
   * and {@link #occupyRecall(int, Object, EventConsumer, EventThread)}.
   * <br><br> 
   * If {@link #donotRelinquish()} was called inside the {@link EventConsumer#processEvent(EventMsg)} for this event,
   * this method return without effect. This is helpfully if the event was stored in another queue for any reason.
   */
  public void relinquish(){
    if(donotRelinquish) return;
    EventSource source1 = this.sourceMsg;
    if(source1 !=null){
      source1.notifyRelinquished(ctConsumed);
    }
    this.stateOfEvent= 'a';
    this.orderId = 0;
    //data1 = data2 = 0;
    this.sourceMsg = null;
    super.source = EventSource.nullSource;
    dateCreation.set(0);
    if(bAwaitReserve){
      synchronized(this){ notify(); }
    }
  }

  
  
  
  

  
  
  
  
  

  
  

  

  
  /**Sends the event again to the same destination with the same command.
   * This method can be used by an application if an Event is received but stored for deferred usage.
   * @return true
   */
  public boolean sendEvent(){
    if(evDst == null) throw new IllegalArgumentException("event should have a destination");
    if(evDstThread !=null){
      evDstThread.storeEvent(this);
    } else {
      try{
        donotRelinquish = false;  //it is possible that the processEvent sets donotRelinquish to true.
        evDst.processEvent(this);
      } catch(Exception exc) {
        System.err.println("Exception while processing an event: " + exc.getMessage());
        exc.printStackTrace(System.err);
      }
      relinquish();
    }
    return true;
  }
  
  
  
  public void consumed(){
    ctConsumed +=1;
    EventSource source1 = this.sourceMsg;
    if(source1 !=null){
      source1.notifyConsumed(ctConsumed);
    }
  }
  

  /*package private*/ void notifyDequeued(){
    EventSource source1 = this.sourceMsg;
    if(source1 !=null){
      source1.notifyDequeued();
    }
  }

  private void notifyShouldSentButInUse(){
    EventSource source1 = this.sourceMsg;
    if(source1 !=null){
      source1.notifyShouldSentButInUse();
    }
  }

  private void notifyShouldOccupyButInUse(){
    EventSource source1 = this.sourceMsg;
    if(source1 !=null){
      source1.notifyShouldOccupyButInUse();
    }
  }

  
  static final SimpleDateFormat toStringDateFormat = new SimpleDateFormat("MMM-dd HH:mm:ss.SSS");
  
  

}
