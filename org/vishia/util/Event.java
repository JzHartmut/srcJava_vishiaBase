package org.vishia.util;

import java.util.concurrent.atomic.AtomicLong;

/**Base class for all events in a event driven software or for communications.
 * Events may contain data. Special data are contained in derived classes of this. The type of the event
 * can be checked using event instanceof DerivedEvent. The id may be only a hint.
 * <br><br>
 * <b>What is a event driven software<b>?<br>
 * There are 4 types of organization of the cooperation in a multi thread system or in a distributed system:
 * <ol>
 * <li>request, wait and notify: The action which should be done by another thread or in another remote device
 *   is requested. Then the requesting thread waits for the execution (for response, for answer, for notify). 
 *   
 * <li>request and poll: The action which should be done by another thread or in another remote device
 *   is requested. Then the requesting thread does any other action, it can't be used the result from the request yet.
 *   But the thread polls cyclically (may be for some milliseconds or longer) the result of the requested order.
 * <li>callback: The action which should be done by another thread or in another remote device
 *   is requested. With the request a callback reference is taken. If the request is executed (received response etc),
 *   the other thread calls the callback method. In the callback method the results of the request can be processed.
 * <li>event driven: The action which should be done by another thread or in another remote device
 *   is requested. The request may be forced by a event, by a direct invocation of a communication call
 *   or by filling any queue with the request. The requested thread does nothing with any response in the immediate
 *   programming environment. It requests only. But the response or execution program may send an event 
 *   to the requesting thread or more exactly, to the requesting module. The thread is stored in a queue firstly.
 *   The queue will be checked by the 'event execution thread', it may be that thread which forces the request.
 *   Because the event is received, any actions may be done to process the response or result.
 * </ol>
 * This 4 types may be present in a users program. The following remarks should be recognized:
 * <ol>
 * <li>request wait and notify: This is the simplest and basic methodology for inter-thread-cooperation,
 *    supported by the basic operations Object.wait() and Object.notify() as basic feature provided in Java.
 *    <br>
 *    The waiting thread can't execute any other things while waiting. A timeout of waiting may be good 
 *    for exception situations. 
 *    <br>
 *    A user may use this methodology for simple functionalities in the application software. But it needs
 *    an extra thread if other things should be done also. Especially in a graphic environment (which is event driven
 *    for graphic operations), a wait for less milliseconds may be ok, but if the response is delayed
 *    or it isn't received, the whole graphic execution is hanging on. (Do you know this effect in some
 *    applications on PC operations systems?).   
 *    <br>Therefore it may be recommended that this methodology should not be used as the favorite solution.
 *    in application programming.
 *    <br>The wait and notify is proper for basic implementations for example to organize a event driven system.
 *    The thread waits for events in the queue. If any other thread put an event in the queue, it notifies
 *    the waiting thread. That event execution thread has only one mission: providing the events to its destinations.
 *    If there are no events, it should wait and sleep.
 * <li>request and poll: This methodology may be used in cyclic execution systems for example for
 *   analog-like controlling of signals. The cyclic execution tests results from communication or inter-thread-communication
 *   in any cycle of execution. 
 * <li>callback: This is a simple methodology to prevent waiting. But the callback method is executed 
 *   in the other thread, which is responsible to the execution of the request or which is the receiving thread
 *   of any communication with a remote device. Maybe some thread mutex mechanism should be necessary.
 * <li>event driven: This methodology needs a system support in user space: The event execution thread. 
 *   That thread polls the event queue. It waits if the queue is empty. If an event is gotten, it is
 *   distributed to its destination. The destination is any instance of any class which offers a the 
 *   event processing method. 
 *   <br>
 *   The event driven methodology can be esteemed as the best for complex systems.     
 * </ol>  
 * <br>
 * <b>remote software, communication</b>:<br>
 * If any action should be executed in any other device which is connected via any communication maybe
 * via ethernet, or serial or any other special, the request for the execution should be sent. 
 * The execution occurs independently in the other device then. The communication may be work with
 * request and response. If the action is received and accepted, a response will be sent back which may
 * contain a answer.
 * <br>
 * The thread which is requesting the action for the remote device may not wait for the response only
 * but may do other things also. It is a adequate theme like presented above.
 * The response will be received from a special receiver thread of the communication usually. 
 * That receiver thread can execute either a callback associated to the request or it can be generate
 * an event causing by the received information. <br>
 * If request and response are assigned together the request can contain an unique order number
 * which is used for the response. In this kind the order of a request can be stored and used if the
 * response is received for callback or to generate the correct event.
 * <br><br>
 * Where is this Event class able to use:
 * <ul>
 * <li>As item in the event queue using the {@link EventThread}
 * <li>As callback item for communication for example in {@link FileRemote}.
 * </ul> 
 * 
 * @author Hartmut Schorrig
 *
 */
public class Event
{
  /**The src instance for the event. This reference should not be used for processing, it is only
   * a hint while debugging.
   */
  protected Object src;
  
  /**The destination instance for the Event. If the event is stored in a common queue, 
   * the dst is invoked while polling the queue. */
  protected EventConsumer dst;
  
  /**The order number for the request, which may be answered by this event. */
  public long order;
  
  /**Timestamp of the requested order. */
  public AtomicLong dateOrder = new AtomicLong();
  
  /**Any number to identify. It is dst-specific. */
  public int id;
  
  /**Any value of this event. Mostly it is a return value. */
  public int iData;
  
  /**Any value refererence. */ 
  public Object oData;
  
  public Event(Object src, EventConsumer dst){
    this.src = src; this.dst = dst;
  }
  
  /**Check whether this event is in use and use it. An event instance can be re-used. If the order or the dateOrder
   * is set, the event is in use. In a target communication with embedded devices often the communication
   * resources are limited. It means that only one order can be requested at one time, and the execution
   * should be awaited. The usage of an re-used event for such orders can be help to organize the
   * requests step by step. If the answer-event instance is in use, a request is pending.
   *  
   * @return true if the event instance is able to use.
   */
  public boolean use(long order, int id, Object src){ 
    if(dateOrder.compareAndSet(0, System.currentTimeMillis())){
      this.order = order;
      this.id = id;
      this.src = src;
      return true;
    }
    else return false;
  }
  
  
  public void consumed(){
    this.id = 0;
    this.src = null;
    this.order = 0;
    dateOrder.set(0);
  }
  
}
