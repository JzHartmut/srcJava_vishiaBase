package org.vishia.event;

import java.util.EventObject;
import org.vishia.states.StateSimple;   //only for comment

/**This interface describe the access to consumers for events.
 * The class which implements this interface is able to get events for example from a common queue
 * and executes the {@link #processEvent(EventMsg)} method with the event.
 * <br>
 * @author Hartmut Schorrig
 *
 */
public interface EventConsumer
{
  /**Version, history and license
   * <ul>
   * <li>2023-07-24 Hartmut remove: The 'awaitExecution(...)' is no longer member of. 
   *   It is specific defined in {@link EventConsumerAwait}, not general for all consumer, not used in vishia Software and not sense. 
   * <li>2023-03-10 Hartmut new: {@link #evThread()} 
   * <li>2023-02-21 Hartmut new: {@link #awaitExecution(long)}
   * <li>2015-01-04 Hartmut chg: It is an abstract class instead an interface yet for new data, less effort for adaption.
   *   New method {@link #shouldRun} which does not need to override in all implementation.
   * <li>2015-01-04 Hartmut chg: With the method {@link #getStateInfo()} any instance is able to quest for its state. 
   *   It may be an important method for debugging and showing.
   * <li>2013-05-11 Hartmut chg: It is an interface up to now. The idea to store a name for debugging on 
   *   anonymous overridden instances is able to implement with an <code>toString(){ return "name"}</code> alternatively.
   *   The method doProcessEvent is renamed to {@link #processEvent(EventMsg)}. 
   *   The advantage of interface: multi inheritance. 
   *   It is used as interface for {@link org.vishia.stateMachine.StateCompositeBase}.
   * <li>2013-04-07 Hartmut adap: Event<?,?> with 2 generic parameter
   * <li>2012-08-30 Hartmut new: This is an abstract class now instead of an interface. 
   *   The application should build an anonymous inner class with it. Because better debugging suppert 
   *   the class has a name which is present to the toString()-method of an event.
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
  public static final String version = "2023-07-24";

  
  /**This operation should return that thread, which is associated to this consumer.
   * @return null possible if no specific thread was associated.
   */
  EventThread_ifc evThread();
  
  
  /**This routine should be overwritten from the user to processes an event. 
   * The return value of the users implementation can contain the following bits:
   * <ul>
   * <li>0x01 = {@link #mEventConsumed} or {@link StateSimple#mEventConsumed}: The event is designated as consumed. 
   *   If not set, the event should applied on other {@link #processEvent(EventObject)} operations, 
   *   for example for other states if possible
   * <li>0x02 = {@link #mEventDonotRelinquish} or {@link StateSimple#mEventDonotRelinquish}: The event should not be relinquished
   *   by the manager of the events (EventTimerThread), it will be relinquished later in the application after completion detection.
   *   It means the application (user) declares itself responsible for relinquish. See {@link EventWithDst#relinquish()}.
   * <li>0x04 = {@link #mEventConsumerFinished}: The task which should be organized by the event is finished. 
   *   This bit is especially used to designate a {@link TimeOrder} as finished for usage, quest in {@link TimeOrder#awaitExecution(int, int)}.
   *   Note that a task can need some more events or other conditions, this bit should be set on the last expected event 
   *   in the return value of {@link #processEvent(EventObject)}. For example if a copy of a file tree is finished, 
   *   or the graphic implementation is completely built.
   * <li>0x08 = {@link #mEventConsumerException}: The execution of {@link #processEvent(EventObject)} has thrown an error,
   *   which is caught in the calling environment in {@link EventTimerThread}.
   *   How to deal with this information - the application is responsible to, the problem should be logged or debugged.
   * <li>0x010000 .. 0x80000 = see {@link StateSimple}.
   * </ul>  
   * @param ev The event. It contains some data. The type of the event is not specified here. Any events
   *   can be processed.
   * @return 0 or {@link #mEventConsumed} or {@link #mEventDonotRelinquish} etc. see list above.
   *   This value is forwarded to {@link EventSource#notifyConsumed(int)} and can be evaluated by the application 
   *   in the calling thread or in any other thread which have access to implementation of {@link EventSource}.
   */
  int processEvent(EventObject ev); 
  
  
  
  
  /**Bit in return value of the {@link #processEvent(EventObject)}
   * for designation, that the given Event object was used to switch.
   */
  public final static int mEventConsumed =0x1;
  
  
  /**Bit in return value of the {@link #processEvent(EventObject)}
   * for designation, that the given Event object is stored in another queue, therefore it should not relinquished yet..
   */
  public final static int mEventDonotRelinquish =0x2;
  
  /**This is the bit that the event consumption is really finished, the work is done. 
   * The difference to {@link #mEventConsumed}: Last one is on each call of {@link #processEvent(EventObject)},
   * If the process needs more events to transmit till if it is ready, then the last call should set this bit.
   * If the process is finished with one event processing, this bit should be set by the first return.  
   */
  public final static int mEventConsumFinished = 0x4;
  
  /**This bit is set if the consumption causes an exception which is not intended. */
  public final static int mEventConsumerException = 0x8;
  
  
  /**This is only a hint which bits are usable here for further usage. 
   * Note: Bits in range 0xff0000 are used in {@link org.vishia.states.StateSimple#mRunToComplete} etc.
   */
  final static int mMaskReservedHere = 0xfff0;  
  
  
}
