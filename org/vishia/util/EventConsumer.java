package org.vishia.util;

/**This interface describe the consumer interface for events.
 * The class which implements this interface is able to get events for example from a common queue
 * and executes the {@link #processEvent(Event)} method with the event.
 * 
 * @author Hartmut Schorrig
 *
 */
public interface EventConsumer
{

  /**Processes a event.
   * @param ev The event. It contains some data.
   * @return true if this method can process this type of event. False if the event doesn't macht.
   *   It is possible to build a queue of responsibility. It is possible too to process a event from 
   *   more as one instance. 
   */
  boolean processEvent(Event ev);
}
