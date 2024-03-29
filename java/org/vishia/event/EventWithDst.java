package org.vishia.event;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EventObject;
import java.util.concurrent.atomic.AtomicLong;

import org.vishia.msgDispatch.LogMessage;
import org.vishia.util.DateOrder;
import org.vishia.util.ExcUtil;

/**This class is a universal basic for all events in the vishia Java source suite. 
 * It is derived from the Java standard {@link EventObject}. 
 * It contains a reference to its destination, which should execute this event,
 * and optional to an instance of {@link EventThread_ifc} which is used as first destination to queue the events
 * and forces the execution (delegates to the destination).
 * The super class from java standard EventObject does only contain the reference to the source. 
 * <pre>
 * Object <--source-+                   +---->{@link Payload}<|--T_Payload
 *         EventObject <|-+             |                  |
 *                        |             |                -more_Data
 *                     EventWithDst -d--+
 *                        |
 *                        |-------evDstThread--->{@link EventTimerThread_ifc}
 *                        |
 *                        |-------evDst--------->{@link EventConsumer}
 *                        |
 *                        |-------evOpp--------->{@link EventWithDst}
 *                     -name
 *                     -dateCreation 
 *                     -dateOrder
 *                     -stateOfEvent
 *                     -ctConsumed
 *                     -orderId 
 * </pre>
 * UML Presentation see {@link org.vishia.util.Docu_UML_simpleNotation}
 * <br><br>
 * <b>Usage of the event</b><br>
 * First the event is the receiver for message data, the instance for queueing and the trigger instance for execution.
 * It is used for trigger an action or for receive a feedback from an action.
 * <br>
 * <br>
 * <b>Simple form, using only for trigger:</b>
 * <pre>
 * Application                    evDst instance of EventConsumer
 *   +-->set any data               |
 *   |   to thisEvent payload       |
 *   +-->sendEvent(thisEvent)------>|
 *                                  +--processEvent()
 * </pre>
 * <ul>
 * <li>{@link #sendEvent(Object)}: emit the event to the dedicated {@link #evDst()} instance (given by construction or {@link #occupy(EventSource, EventConsumer, EventThread_ifc, boolean)}
 * <li>{@link #processEvent()} is called immediately in the same thread, it calls in the evDst instance: {@link EventConsumer#processEvent(EventObject)}.
 *   This can evaluate the data of the event.
 * </ul>
 * Here no thread change is done. The event is only used to transport the immediately call.
 * Examples for this usage are some operations of FileRemote such as {@link org.vishia.fileRemote.FileRemote#mkdir(boolean, EventWithDst)}.
 * If the 'make dir' operation is executed in the same thread because it is a cheap operation on the local file system, 
 * the 'Application' of the above sequence diagram is the 'mkdir' operation.
 * The {@link EventConsumer} should be given with the event. It is only an instance, not related to another thread.
 * <br><br>
 * The application should call:
 * <pre>
 * EventConsumer 
 * <br>
 * <br>
 * <b>More simple form, only using the payload:</b>
 * <pre>
 * Application
 *   +-->do any which uses the event 
 *              +-->set any data
 *   +<---------+   to thisEvent payload
 *   +<--read data
 *   |   from the events payload
 * </pre>
 * In this also sensible case the event is only used to store some data. 
 * The called operation which uses the event fills some feedback data but don't send the event.
 * The event instance is only used as data transporter. 
 * This is for example done if some operations of FileRemote such as {@link org.vishia.fileRemote.FileRemote#mkdir(boolean, EventWithDst)}
 * and the operation is executed in the same 
 * <br>
 * ... no the using operation should call anyway sendEvent and a #evDst should be available.
 * <br>
 * Note: If the message for this event was transferred via for example socket connection, 
 *   its data of the derived "UserEvent" should be received as payload and reconstructed.
 *   The aggregated data {@link #evDstThread} and {@link #evDst()} are set by initialization
 *   in the received environment.
 * @author Hartmut Schorrig
 *
 */
public final class EventWithDst<T_Payload extends Payload, T_PayloadOpp extends Payload> extends EventObject
{

  private static final long serialVersionUID = 3976120105528632683L;
  
  
  /**Version, history and license.
   * <ul>
   * <li>2023-07-22 Hartmut enhancement: More ctor versions. The {@link #evOpp} was not used till now. 
   * <li>2023-03-20 Hartmut The payload {@link #d} is now typed with the new interface {@link Payload}.
   *   For serialize of the payload some thinking should be done. 
   * <li>2023-03-20 Hartmut chg the role of {@link EventObject#source}: originally it should be the creator of the event. 
   *   But intrinsic it should be the instance which emits the event. The last one is only important on debugging. 
   *   It is interesting on receiving from a queue, from where comes the event. 
   *   Intrinsically it should show a __FILE__ and __LINE__ (as in C) to see where the event was emitted. 
   *   That may be possible with StackTrace entries. Yet only sometimes a string is stored for that reason.
   *   The source is filled as argument now on {@link #sendEvent(Object)}. 
   *   The up to now usage of source as the owner of the event is now stored in {@link #evOwner} in this class.    
   * <li>2023-03-12 Hartmut chg The org.vishia.event is refactored:
   *   <ul><li>Only the final EventWithDst is existent.
   *   <li>The data of the event are not defined by derivation, now they are defined by a aggregated payload.
   *   <li>The payload should be Serializable, because exact this are all data to transfer an event.
   *   <li>The classes EventCmdtype and EventCmdtypeWithBackEvent are dissolved.
   *     The Cmd is part of Payload. The aggregation {@link #evOpp} is now contained here.
   *     It is null if a back event is not given.
   *   <li>{@link TimeOrder} is no more aggregated, but the TimeOrder aggregates its event.
   *   <li>{@link EventConsumer} can decide about the thread.
   *   <li>{@link #occupy(EventSource, EventConsumer, EventThread_ifc, boolean)} is related only to the {@link #dateCreation} and not to the {@link #evDst}
   *     The {@link #evDst()} can be determined by construction, as well as the {@link EventObject#source}. 
   *   </ul>
   * <li>2023-02-07 Hartmut chg {@link #timeOrder} as possible aggreation (on construction) with the proper ctor:
   *   {@link #EventWithDst(String, EventTimerThread_ifc, EventSource, EventConsumer, EventThread_ifc)}
   * <li>2023-02-07 Hartmut chg now the event has a {@link #name}, helps on debug  
   * <li>2023-02-07 Hartmut refactor the evDstThread is not a EventTimerThread, it is a common possible {@link EventThread_ifc}.
   *   this has less impact on usage.   
   * <li>2023-02-07 Hartmut new {@link #bStaticOccupied}: now eventually clarified, necessary using occupy or not.
   * <li>2015-01-03 Hartmut chg: Separated in 2 classes: {@link EventCmdtypeWithBackEvent} with opponent and this class.
   *   A simple event has not an opponent per default. This class is named EventWithDst yet because the destination
   *   is the significant difference to its base class {@link java.util.EventObject}.
   * <li>2015-01-03 Hartmut chg: Renamed to EventMsg: more significant name. Derived from EventObject: A basicly Java concept.
   * <li>2013-10-06 Hartmut chg: Some checks for thread safety.
   * <li>2013-10-06 Hartmut chg: {@link #occupy(int, EventSource, EventConsumer, EventThread_ifc)} with timeout
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
  public static final String version = "2023-07-22";

  
  /**The current owner of the event. It is that instance, which has gotten the event instance.
   */
  //EventSource sourceMsg;
  
  
  public final String name;
  
  EventSource evOwner;
  
  /**The queue for events of the {@link EventThread_ifc} if this event should be used
   * in a really event driven system (without directly callback). 
   * If it is null, the dst. {@link EventConsumer#processEvent(EventMsg)} should be called immediately. */
  EventThread_ifc evDstThread;
  
  
  /**The destination instance for the Event. If the event is stored in a common queue, 
   * the dst is invoked while polling the queue. Elsewhere the dst is the callback instance. */
  EventConsumer evDst;
  
  /**All data to this event, should be transferred via InterProcessComm.
   * Also the type of the event should be transferred, for events without payload.
   * It means Payload can be null, if the type is sufficient to regocnize the event.
   */
  T_Payload d;
  
  
  /**This is an associated event which can be used for the back direction.
   * Often a pair of events for back communication is necessary.
   * It is null if unused.
   */
  final EventWithDst<T_PayloadOpp, T_Payload> evOpp;
  
  /**State of the event: 
   * <ul>
   * <li>0 or '.': unused.
   * <li>a: requested or allocated. The {@link EventConsumer} and the {@link EventThread_ifc} is set, but the event 
   *   is not in send to the consumer. It is not in a queue and not in process.
   * <li>q: queued in dstThread
   * <li>e: executing
   * <li>B: queued for callback
   * <li>b: callback invoked
   * 
   * </ul> 
   */
  public char stateOfEvent;
  
  ///**package private*/ boolean donotRelinquish;
  
  boolean bAwaitReserve;
  
  protected int ctConsumed;
  
  /**The commission number for the request, which may be answered by this event. */
  protected long orderId;
  
  /**Timestamp of the request. It is atomic because the timestamp may be an identification
   * that the event instance is occupied, see {@link #occupy(EventSource, boolean)}.
   * It is for new-obviating usage. */
  protected final AtomicLong dateCreation = new AtomicLong();
  
  /**Order of time stamps created in the same millisec of #dateCreation. Only used if necessary, usual not. 
   * Maybe for debugging approaches. */
  int dateOrder;
  
  /**Any value of this event. Mostly it is a return value. */
  //public int data1, data2;
  
  

  
  /**Creates an event as a static object for re-usage. Use {@link #occupy(Object, EventConsumer, EventThread_ifc)}
   * before first usage. {@link #relinquish()} will be called on release the usage. 
   * @param name of the event used only for debug.
   */
  public EventWithDst(String name, T_Payload data) {
    this(name, EventSource.nullSource, data);
  }
  
  public EventWithDst(String name, T_Payload data, EventWithDst<T_PayloadOpp, T_Payload> evOpp) {
    this(name, EventSource.nullSource, data, evOpp);
  }

  
  public EventWithDst(String name, Object source, T_Payload data) {
    this(name, source,  data, null);
  }
  
  public EventWithDst(String name, Object source, T_Payload data, EventWithDst<T_PayloadOpp, T_Payload> evOpp)
  { super(source);
    this.d = data;
    this.name = name;
    this.evOpp = evOpp;
  }


  
  /**Creates an event as static or dynamic object for usage.  
   * @param name of the event used only for debug.
   * @param source Source of the event. 
   * @param consumer If null then the event should be occupied before usage.
   *   If given {@link #occupy(EventSource, boolean)} is not necessary for usage but possible.
   * @param thread an optional thread to store the event in an event queue, maybe null.
   *   If null but the {@link EventConsumer#evThread()} returns not null, it is used.
   *   It means the {@link EventConsumer} can decide about the event thread.
   *   But this argument is used if given.
   * @param d the instance for the payload for this event. If the event is transmitted via a medium,
   *   the payload will be serialized.  
   */
  public EventWithDst(String name, EventSource source, EventConsumer consumer, EventThread_ifc evThread, T_Payload d){
    super("free");                                 // the source will be set on sent
    this.evOwner = source == null ? EventSource.nullSource : source;   //EventObject does not allow null pointer.
    this.name = name;
    this.evDst = consumer;                                 //maybe null if occupy is intent to call.  
    @SuppressWarnings("resource") 
    EventThread_ifc thread = (consumer == null ? null : consumer.evThread());
    this.evDstThread = evThread != null ? evThread : thread;                           //maybe null
    this.d = d;
    this.evOpp = null;
  }
  

  /**Creates an event as static or dynamic object for usage with its opponent event.  
   * The opponent event is created without a specified {@link EventConsumer} and without a specified event thread.
   * It means the opponent event can be used in a specific kind (for example to transmit the data)
   * or this information should be given using {@link #occupy(EventSource, EventConsumer, EventThread_ifc, boolean)}.
   * But the payload for the opponent is referenced if the dOpp argument is not null.
   * @param name of the event used only for debug. The opponent gets the name + "~".
   * @param source Source of the event. Can be null, then a default source is used, or can be also a string literal.
   * @param consumer If null then the event should be occupied before usage.
   *   If given {@link #occupy(EventSource, boolean)} is not necessary for usage but possible.
   * @param thread an optional thread to store the event in an event queue, maybe null.
   *   If null but the {@link EventConsumer#evThread()} returns not null, it is used.
   *   It means the {@link EventConsumer} can decide about the event thread.
   *   But this argument is used if given.
   * @param d the instance for the payload for this event. If the event is transmitted via a medium,
   *   the payload will be serialized.  
   * @param dOpp the instance for the payload for the opponent of the event. If the event is transmitted via a medium,
   *   the payload will be serialized. This instance is then used as destination for deserialize.  
   */
  public EventWithDst(String name, EventSource source, EventConsumer consumer, EventThread_ifc evThread, T_Payload d, T_PayloadOpp dOpp){
    super("free");                                 // the source will be set on sent
    this.evOwner = source == null ? EventSource.nullSource : source;   //EventObject does not allow null pointer.
    this.name = name;
    this.evDst = consumer;                                 //maybe null if occupy is intent to call.  
    @SuppressWarnings("resource") 
    EventThread_ifc thread = (consumer == null ? null : consumer.evThread());
    this.evDstThread = evThread != null ? evThread : thread;                           //maybe null
    this.d = d;
    this.evOpp = new EventWithDst<T_PayloadOpp, T_Payload>(name + "~", dOpp, this);
  }
  

  /**Only used in {@link EventWithDst#EventWithDst(String, EventSource, EventConsumer, EventThread_ifc, Payload, String, EventSource, EventConsumer, EventThread_ifc, Payload)}
   * to create the opponent itself with this as reference.
   * If this class would be used in an application the opponent would not refer its mutual opponent.
   * Hence it is private. 
   * @param name fed with the nameOpp
   * @param source 
   * @param consumer
   * @param evThread
   * @param d
   * @param evOpp this
   */
  private EventWithDst(String name, EventSource source, EventConsumer consumer, EventThread_ifc evThread, T_Payload d
      , EventWithDst<T_PayloadOpp, T_Payload> evOpp){
    super("free");                                 // the source will be set on sent
    this.evOwner = source == null ? EventSource.nullSource : source;   //EventObject does not allow null pointer.
    this.name = name;
    this.evDst = consumer;                                 //maybe null if occupy is intent to call.  
    @SuppressWarnings("resource") 
    EventThread_ifc thread = (consumer == null ? null : consumer.evThread());
    this.evDstThread = evThread != null ? evThread : thread;                           //maybe null
    this.d = d;
    this.evOpp = evOpp;
  }
  

  
  /**Creates an event as static or dynamic object for usage with its opponent event  
   * with a given opponent event. The opponent event should be created without this as opponent of course.
   * But this event is referenced in the opponent also as opponent by this constructor.
   * The opponent can be created with a specific {@link EventConsumer}, {@link EventThread_ifc} and {@link Payload}.
   * The implementation type of Payload should match the T_PayloadOpp in this class and vice versa.
   * @param name of the event used only for debug. The opponent gets the name + "~".
   * @param source Source of the event. Can be null, then a default source is used, or can be also a string literal.
   * @param consumer If null then the event should be occupied before usage.
   *   If given {@link #occupy(EventSource, boolean)} is not necessary for usage but possible.
   * @param thread an optional thread to store the event in an event queue, maybe null.
   *   If null but the {@link EventConsumer#evThread()} returns not null, it is used.
   *   It means the {@link EventConsumer} can decide about the event thread.
   *   But this argument is used if given.
   * @param d the instance for the payload for this event. If the event is transmitted via a medium,
   *   the payload will be serialized.  
   * @param opponent Already created opponent first as event without opponent. 
   *   The type of {@link Payload} (generic types of this class) should match.   
   */
  public EventWithDst(String name, EventSource source, EventConsumer consumer, EventThread_ifc evThread, T_Payload d
      , String nameOpp, EventSource sourceOpp, EventConsumer consumerOpp, EventThread_ifc evThreadOpp, T_PayloadOpp dOpp){
    super("free");                                 // the source will be set on sent
    this.evOwner = source == null ? EventSource.nullSource : source;   //EventObject does not allow null pointer.
    this.name = name;
    this.evDst = consumer;                                 //maybe null if occupy is intent to call.  
    @SuppressWarnings("resource") 
    EventThread_ifc thread = (consumer == null ? null : consumer.evThread());
    this.evDstThread = evThread != null ? evThread : thread;                           //maybe null
    this.d = d;
    this.evOpp = new EventWithDst<T_PayloadOpp, T_Payload>(nameOpp, sourceOpp, consumerOpp, evThreadOpp, dOpp, this);
  }
  

  
  void setSource(Object src) { super.source = src; }
 
  public void setOrderId(long order){ this.orderId = order; }
  
  
  
  
  /**Prevent that the event is relinquished after processing.
   * This method should be called in the processing routine of an event
   * only if the event is stored in another queue to execute delayed.
   */
  @Deprecated public void XXXdonotRelinquish(){ 
    //if(stateOfEvent !='r') throw new IllegalStateException("donotRelinquish() should be called only in a processEvent-routine.");
    if(this.stateOfEvent == 'r'){
      this.stateOfEvent = 'p';  //secondary queued. donotRelinquish = true;
    }
  }
  
  /**This method should only be called if the event should be processed.
   * The {@link #donotRelinquish()} will be set to false, so that the event will be relinquished
   * except {@link #donotRelinquish()} is called while processing the event. 
   * @return The event consumer to call {@link EventConsumer#processEvent(EventMsg)}.
   */
  public EventConsumer evDst() { return this.evDst; }
  
  
  public T_Payload data() { return this.d; }
  
  /**Returns the time stamp of creation or occupying the event.
   * @return null if the event is not occupied, a free static object.
   */
  public Date dateCreation(){ long date = this.dateCreation.get(); return date == 0 ? null : new Date(date); }
  
  
  
  
  
  public boolean hasDst(){ return this.evDst !=null; }
  
  
  public void setDst(EventConsumer dst) { this.evDst = dst; }
  
  
  
  /**This is an empty operation for the basic event. 
   * It is intent to override for derived events to clean the event data.
   */
  protected void cleanData ( ) { }
  
  
  public void clean() {
    cleanData();
    this.removeFromQueue();
    this.relinquish();
  }
  
  
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
   * @param thread A thread which queues the event, stored in {@link #evDstThread} .
   *   If null, then the given thread by constructor or the last given thread is used.
   *   If null and dst !=null then the {@link EventConsumer#evThread()} is used, but it can be also null. 
   *   If the {@link #evDstThread} remains null, then the event is used as callback,
   *   the {@link EventConsumer#processEvent(EventObject)} is immediately called on {@link #sendEvent()}.
   * @param expect If true and the event is not able to occupy, then the method {@link EventSource#notifyShouldOccupyButInUse()} 
   *   from the given source is invoked. It may cause an exception for example. 
   * @return true if the event instance is occupied and ready to use.
   */
  public boolean occupy(EventSource source, EventConsumer dst, EventThread_ifc thread, boolean expect){ 
    DateOrder date = DateOrder.get();
    if(this.dateCreation.compareAndSet(0, date.date)){
      cleanData();                                         // cleanDate overridden in derived events, clean.
      this.dateOrder = date.order;
      if(source !=null) {
        this.evOwner = source;  //don't use null pointer.
      } else if( this.evOwner == null) {
        this.evOwner = EventSource.nullSource;             //don't use null pointer.
      }
      this.ctConsumed =0;
      //if(refData !=null || dst !=null) { this.refData = refData; }
      if(dst != null) { 
        this.evDst = dst;
        if(thread == null) {
          this.evDstThread = dst.evThread();     // dst.evThread may also be null, then immediately forward the event.
        } else {
          this.evDstThread = thread;
        }
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
  public boolean occupy(int timeout, EventSource evSrc, EventConsumer dst, EventThread_ifc thread){
    boolean bOk = occupy(evSrc, dst, thread, false);
    if(!bOk){
      synchronized(this){
        this.bAwaitReserve = true;
        try{ wait(timeout); } catch(InterruptedException exc){ }
        this.bAwaitReserve = false;
        bOk = occupy(this.evOwner, dst, thread, false);
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
   *   See {@link #occupyRecall(int, Object, EventConsumer, EventThread_ifc)}.   
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
  public boolean occupyRecall(EventSource source, EventConsumer dst, EventThread_ifc thread, boolean expect){ 
    boolean bOk = occupy(source, dst, thread, false);
    if(!bOk){
      if(this.evDstThread !=null){
        bOk = this.evDstThread.removeFromQueue(this);
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
  
  
  
  
  /**Try to occupy the event for usage, recall a stored event in  queue, wait till it is available and force occupying
   * on hanging.
   * This method may block if the event is yet processing. The method blocks only for the given timeout.
   * <br><br>
   * An event is a container for a message. If it is send to a queue but it is not executed yet and the sender knows
   * that a re-fill is better, it should use this method. For example a progress message can be send to a queue. If that
   * progress message is not used and a newer progress message is better to have, the event may be recalled. 
   * <br><br>
   * If the event was send, it is not queued yet but it is occupied though, It is possible that the consumer has not invoked
   * {@link #relinquish()} because the consumer process was aborted especially by an exception or there is any other error.
   * If the event was occupied a longer time before it is relinquished by this method and occupied newly.
   * <br><br>
   * The only one reason that this method returns 0 and does not occupy is: The event was occupied newly in the timeout period
   * after this method was started by any other process. It means the event does not hang but it is used by any other.
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
   * See {@link #occupyRecall(Object, EventConsumer, EventThread_ifc)}.   
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
   *   <li>3 if the event is forced relinquish because it was hanging.
   *   </ul> 
   */
  public int occupyRecall(int timeout, EventSource source, EventConsumer dst, EventThread_ifc thread, boolean expect){
    int ok = 0;
    boolean bOk = occupy(source, dst, thread, false);
    if(bOk){ ok = 1; }
    if(!bOk){
      if(this.evDstThread !=null){
        bOk = this.evDstThread.removeFromQueue(this);
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
        this.bAwaitReserve = true;
        try{ wait(timeout); } catch(InterruptedException exc){ }
        this.bAwaitReserve = false;
      }//synchronized
      bOk = occupy(source, dst, thread, false);
      if(bOk){ ok = 1; }
      else {
        synchronized(this){
          long timeCreation = this.dateCreation.get();
          if(!bOk && timeCreation !=0 && (timeCreation - time1) < 0) {
            //the event is not occupied newly. It means it hangs usual because an exception which has prevented relinquish():
            //force new usage:
            //donotRelinquish = false;
            this.bAwaitReserve = false;
            this.stateOfEvent = 'r';
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
  
  
  /**Try to occupy the event for usage, recall it if it is in stored in an event queue, wait till it is available, occupy a hanging event.
   * Same as {@link #occupyRecall(int, EventSource, EventConsumer, EventThread_ifc, boolean)} but left the destination unchanged.
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
   * are done only one after another. In that kind the event instance is created one time,
   * and re-used whenever it is needed. 
   * If the answer hangs, {@link #occupyRecall(int, EventSource, boolean)} may be called to occupy it though.
   * 
   * @return false if it is ready to re-use.
   */
  public boolean isOccupied(){ return this.dateCreation.get() !=0; }
  
  
  /**Returns the stored destination thread for queuing the event. */
  public EventThread_ifc getDstThread(){ return this.evDstThread; }
  
  
  /**Returns the destination. 
   * @return
   */
  public EventConsumer getDst(){ return this.evDst; }
  
  
  public EventWithDst<T_PayloadOpp, T_Payload> getOpponent ( ){ return this.evOpp;}
  
  /**Try to remove the event from the queue of the destination thread.
   * @return true if it was in the queue and it is removed successfully.
   *   false if the event was not found in the queue or the destination thread is not set.
   */
  public boolean removeFromQueue(){
    if(this.evDstThread !=null) return this.evDstThread.removeFromQueue(this);
    else return false;
  }
  
  
  
  /**Relinquishes the event object. It is the opposite to the {@link #occupy(Object, EventConsumer, EventThread_ifc)} method.
   * The {@link #dateCreation} is set to 0 especially to designate the free-state of the Event instance.
   * The {@link #refData}, {@link #evDst()}, {@link #evDstThread} and {@link #opponent} are not changed
   * because the event may be reused in the same context.
   * All other data are reseted, so no unused references are hold. 
   * <br><br>
   * If any thread waits for this Event object, a {@link Object#notify()} is called. See {@link #occupyRecall(int)}
   * and {@link #occupyRecall(int, Object, EventConsumer, EventThread_ifc)}.
   * <br><br> 
   * If {@link #donotRelinquish()} was called inside the {@link EventConsumer#processEvent(EventMsg)} for this event,
   * this method return without effect. This is helpfully if the event was stored in another queue for any reason.
   */
  public void relinquish(){
    //if(stateOfEvent != 'r') throw new IllegalStateException("relinquish should only be called in stateOfEvent == 'r'");
    if(this.dateCreation.get() !=0) {
      this.dateCreation.set(0);
      EventSource source1 = ((EventSource)this.evOwner);
      this.orderId = 0;
      if(source1 !=null){
        source1.notifyRelinquished(this.ctConsumed);
      }
    }
    this.stateOfEvent = 'f';                   // event is "free"
    if(this.bAwaitReserve){                    // any thread waits for this event ...
      synchronized(this){ notify(); }
    }
  }

  
  
  
  

  
  
  
  
  

  
  

  

  
  /**Sends the event to the given destination.
   * If the {@link #evDstThread} is given on construction or on {@link #occupy(int, EventSource, EventConsumer, EventThread_ifc)}
   * then the event is stored in the queue of this thread to execute it using {@link EventThread_ifc#storeEvent(EventObject)}. 
   * <br>If the thread is not given, {@link EventConsumer#processEvent(EventObject)} is called to execute the event in the current thread.
   * <br>Generally, the event is relinquished and the {@link EventSource} is informed if the event is processed,
   * see description of {@link EventConsumer#processEvent(EventObject)}.
   * It means, if the event is applied or processed, it is free for further usage or it can contain back information, 
   * then it should be evaluated and freed by the application. This is decided with the return value of the processEvent operation.
   * <br>The {@link #evDst()} must no == null, should be given by construction with {@link EventWithDst#EventWithDst(String, EventSource, EventConsumer, EventThread_ifc)}
   * or by {@link #occupy(EventSource, EventConsumer, EventThread_ifc, boolean)}. If it is null, an exception is thrown.
   * @return true if the event is already processed, false if it is stored in a queue, not processed yet.
   */
  public boolean sendEvent ( Object source ) {
    super.source = source;                       // it is only for debug, info about the source of the event.
    assert(this.evDst !=null);
    //if(this.evDst == null) throw new IllegalArgumentException("event should have a destination");
    if(this.evDstThread !=null){  //--------------- dstThread given, then
      if(this.dateCreation.get()==0) {           // occupy the event afterwards
        DateOrder date = DateOrder.get();
        this.dateCreation.set(date.date);
        this.dateOrder = date.order;
      }
      this.evDstThread.storeEvent(this);         // and store in queue
      return false;
    } else {                  //------------------- or
      processEvent();                            // it is a callback, immediately
      return true;
    }
  }

  
  
  /**Applies the event to its {@link #evDst()} to processes it (EventConsumer{@link #processEvent()}) 
   * On {@link #sendEvent()} this operation is called immediately if the #evDstThread is not set. For callback like events.
   * This operation should not be called immediately from the user, call {@link #sendEvent()}, 
   * but it is equal to {@link #sendEvent()} if no thread is given. 
   * It is called from {@link EventTimerThread} and it is intent to call from other event queue organizations.
   */ 
  public int processEvent() {
    int retProcess = 0;  //check doNotRelinquish, relinquishes it in case of exception too!
    //System.out.println(LogMessage.timeCurr("processEvent:") + this.name);
    try{
      retProcess = this.evDst.processEvent(this);  //may set bit doNotRelinquish
    } catch(Exception exc) {
      CharSequence excMsg = ExcUtil.exceptionInfo("EventThread.applyEvent exception", exc, 0, 50);
      EventSource source1 = ((EventSource)this.evOwner);
      if(source1 !=null){
        source1.notifyUnexpectedException(excMsg);
      }
      retProcess |= EventConsumer.mEventConsumerException;
    }
    return retProcess;
  }
  
  
  

  
  
  /**This routine is be called after consuming the event. It counts the number of consuming and invokes
   * {@link EventSource#notifyConsumed(int)} with this number if a source is given. Usual that is helpfully for debugging. 
   * If a {@link #timeOrder} is given which is not identically with the source,
   * also {@link TimeOrder#notifyConsumed(int)} is called.
   */
//  private void consumed(int retFromConsumer){
//    this.ctConsumed +=1;
//    int valueNotify = (this.ctConsumed<<24) | (retFromConsumer & 0x00ffffff);
//    EventSource source1 = ((EventSource)super.source);
//    if(source1 !=null){
//      source1.notifyConsumed(valueNotify);
//    }
//    if(this.timeOrder !=null && this.timeOrder != source1) {
//      this.timeOrder.notifyConsumed(valueNotify);
//    }
//  }
  

  /**Informs the {@link EventSource#notifyDequeued()}, invoked on dequeuing.  */
  /*package private*/ void notifyDequeued(){
    EventSource source1 = (this.evOwner);
    if(source1 !=null){
      source1.notifyDequeued();
    }
  }

  /*
  private void notifyShouldSentButInUse(){
    EventSource source1 = ((EventSource)source);
    if(source1 !=null){
      source1.notifyShouldSentButInUse();
    }
  }*/

  private void notifyShouldOccupyButInUse(){
    EventSource source1 = ((EventSource)this.evOwner);
    if(source1 !=null){
      source1.notifyShouldOccupyButInUse();
    }
  }

  
  static final SimpleDateFormat toStringDateFormat = new SimpleDateFormat("MMM-dd HH:mm:ss.SSS");
  
  

}
