package org.vishia.event;

/**Access to the current source of a event. It is referred inside any event.
 * The source is that class, which has transmitted the event.
 * Usual an event which were transmitted has no more connection to the source of it.
 * But especially for debugging situations it may be a point of interest whether and when the event is processed.
 * For debugging it may be a point of interest which instance is the source of an event.
 * <br><br>
 * This is a class, not an interface, because it contains a name for debugging. 
 * The user can instantiate the class as an inner anonymous class with following scheme:
 * <pre>
  EventSource theSource = new EventSource("debugname"){
    @Override public void notifyDequeued(){}
    @Override public void notifyConsumed(){}
    @Override public void notifyRelinquished(){}
  };
 * </pre>
 * The bodies of the methods may access any data of the environment class.
 * @author Hartmut Schorrig
 *
 */
public class EventSource
{
  /**Version, history and license
   * <ul>
   * <li>The meaning and name is changed. It is only a debugging helper. The functionality of freeing is solved
   *   in the {@link EventCmdtype} class.
   * <li>2012-03-10 Hartmut created: 
   *   It is a problem if a request may be crashed in a remote device, but the event is reserved 
   *   for answer in the proxy. It should be freed. Events may be re-used. 
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
  public static final int version = 20120812;
  
  /**The name is private because it is only used for toString(). */
  private final String name;  
  
  /**A default source with empty methods.
   * 
   */
  public static EventSource nullSource = new EventSource("defaultEventMsgSource"){
    
  };
  
  
  /**
   * @param name
   */
  public EventSource(String name){ this.name = name; }
  
  /**It is called if the event is dequeued from the {@link EventWithDst#evDstThread} queue and attempt to processed. */
  public void notifyDequeued(){}
  
  /**This operation is called after calling {@link EventConsumer#processEvent(java.util.EventObject)}
   * in {@link EventTimerThread} and should be called also in all other implementations of {@link EventThread_ifc}
   * to assign, how the event is used. 
   * @param state The return value on {@link EventConsumer#processEvent(java.util.EventObject)} given from the user implementation.
   *   It is recommended to use the defined bits in {@link EventConsumer#mEventConsumed} etc.
   *   or also from {@link org.vishia.states.StateSimple#mStateEntered} etc.
   *   The bits 31..24 the {@link EventWithDst#ctConsumed} especially for debugging. 
   */
  public void notifyConsumed(int state){}
  
  public void notifyRelinquished(int ctConsumed){}

  public void notifyShouldSentButInUse(){}

  public void notifyShouldOccupyButInUse(){}
  
  public void notifyUnexpectedException(CharSequence msg) {}

  @Override public String toString(){ return name; }
  
}

