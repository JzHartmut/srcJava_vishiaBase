package org.vishia.event;

import java.util.EventObject;

/**This interface should only be used for an alternative implementation of a event thread. 
 * The methods are used by the event processing. Of this package.
 *  
 * @author Hartmut Schorrig
 *
 */
public interface EventTimerThread_ifc extends EventThread_ifc
{
  /**Adds a timeout event or a time order with given execution time. The time should be set in the event already
   * using its method {@link EventTimeout#activateAt(long)} etc. That routines calls this method internally already.
   * Therefore this method should not be called by an application directly. It is only a rule to implement. 
   * @param order
   */
  void addTimeOrder(EventTimeout order);
  
  /**Removes a time order, which was activated but it is not in the event execution queue.
   * If the time order is expired and it is in the event execution queue already, it is not removed.
   * This operation is thread safe. Either the event is found and removed, or it is not found because it is
   * either in execution yet or it is not queued.
   * @param order the timeout event or the time order.
   */
  boolean removeTimeOrder(EventTimeout order);
  
}
