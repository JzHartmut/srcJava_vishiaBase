package org.vishia.event;


import java.util.Date;

import org.vishia.util.DateOrder;

/**This event contains an event instance for back information, named "opponent".
 * The opponent should be initialized but free for use (not occupied),
 * but it should be free for {@link EventWithDst#occupy(EventSource, EventConsumer, EventTimerThread, boolean)}
 * by the destination of this event. 
 * @author Hartmut Schorrig
 *
 * @param <CmdEnum> Enum Type of commands as in the super class {@link EventCmdtype} 
 * @param <EventBack> Type of the opponent event for back information. 
 */
public class EventCmdtypeWithBackEvent<CmdEnum extends Enum<CmdEnum>, EventBack extends EventWithDst> extends EventCmdtype<CmdEnum>
{
  
  /**Version, history and license
   * <ul>
   * <li>2014-01.03 Hartmut created: Separated from older Event class in this package respectively from EventMsg.
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

  private static final long serialVersionUID = -3698408452697243855L;
  
  public enum Consumed{Consumed, RunToCompleted}

  /**Cmd which designates that the event has not an opponent. This type is able to use as second parameter
   * for Event<Cmd, Event.NoOpponent>
   */
  public enum NoOpponent{ }
  

  /**An event can have an opponent or counterpart for return information. The event and its counterpart may refer one together.
   */
  protected EventBack opponent;
  
  /**Creates an event as a static object for re-usage. Use {@link #occupy(Object, EventConsumer, EventTimerThread)}
   * before first usage. Use {@link #relinquish()} to release the usage. 
   * 
   */
  public EventCmdtypeWithBackEvent(){
    super(); //EventSource.nullSource);
  }
  

  /**Creates an event as a dynamic object for direct usage without a given {@link EventConsumer}.
   * This event should be used as parameter immediately for an event consuming routine.
   * The event is set as occupied already after creation. 
   * Don't call {@link #occupy(EventSource, boolean)} or {@link #occupy(EventSource, EventConsumer, EventTimerThread, boolean)} should be use
   * Don't call {@link #sendEvent()} because a destination is not given.
   * 
   * @param cmd a given Command. It may be null, it can be overwritten later with {@link #setCmd(Enum)}
   *   or using {@link #sendEvent(Enum)}.
   */
  public EventCmdtypeWithBackEvent(CmdEnum cmd){
    super(cmd);
  }
  
  
  /**Creates an event as dynamic object for usage. Use {@link #relinquish()} after the event is used and it is not referenced
   * anymore. 
   * @param source Source of the event. If null then the event is not occupied, especially the {@link #dateCreation()} 
   *   is set to 0.
   * @param refData Associated data to the event. It is the source of the event.
   * @param consumer The destination object for the event.
   * @param thread an optional thread to store the event in an event queue, maybe null.
   */
  public EventCmdtypeWithBackEvent(EventSource source, EventConsumer consumer, EventTimerThread_ifc thread){
    super(source, consumer, thread);
    this.opponent = null;
  }
  
  
  /**Creates an event as dynamic object for usage. Use {@link #relinquish()} after the event is used and it is not referenced
   * anymore. 
   * @param source Source of the event. If null then the event is not occupied, especially the {@link #dateCreation()} 
   *   is set to 0.
   * @param refData Associated data to the event. It is the source of the event.
   * @param consumer The destination object for the event.
   * @param thread an optional thread to store the event in an event queue, maybe null.
   * @param callback Another event to interplay with the source of this event.
   */
  public EventCmdtypeWithBackEvent(EventSource source, EventConsumer consumer, EventTimerThread_ifc thread
      , EventBack callback){
    super(); //EventSource.nullSource);
    if(source == null || consumer == null){
      this.dateCreation.set(0);
    } else {
      super.source = source;
      DateOrder date = DateOrder.get();
      this.dateCreation.set(date.date);
      this.dateOrder = date.order;
    }
    this.evDst = consumer; this.evDstThread = thread;
    this.opponent = callback;
    if(callback !=null && callback instanceof EventCmdtypeWithBackEvent) {
      //only if the callback is type of EventCmdtypeWithBackEvent too, refer both vice versa:
      @SuppressWarnings("unchecked") 
      EventCmdtypeWithBackEvent<?,EventCmdtypeWithBackEvent<?,?>> callbackTyped = ((EventCmdtypeWithBackEvent<?,EventCmdtypeWithBackEvent<?,?>>)callback);
      callbackTyped.opponent = this;  //Refer this in the callback event. 
    }
  }
  
  
  /**Sets the command into the event. The event should be occupied already.
   * @param cmd any admissible command
   * @return this to concatenate
   */
  //public EventCmdtypeWithBackEvent<CmdEnum, EventBack> setCmd(CmdEnum cmd){ cmde = cmd; return this; }
  
  public void setOpponent(EventBack opponent) { this.opponent = opponent; }
  
  
  public EventBack getOpponent(){ return opponent; }
  
  public boolean hasOpponent(){ return opponent !=null; }
  
  @Override public String toString(){ 
    long nDate = dateCreation.get();
    if(nDate == 0) return "Event not occupied";
    Date date = new Date(nDate);
    String sCmd = cmde == null ? "null" : cmde.toString();
    EventSource source1 = ((EventSource)source);  
    return "Event cmd=" + sCmd + "; " + (nDate == 0 ? "nonOccupied" : toStringDateFormat.format(date) + "." + dateOrder) + "; src=" + (source1 !=null ? source1.toString() : " noSrc") + "; dst="+ (evDst !=null ? evDst.toString() : " noDst"); 
  }
  
  
}
