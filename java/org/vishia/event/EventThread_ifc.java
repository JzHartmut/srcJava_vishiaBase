package org.vishia.event;

import java.io.Closeable;
import java.util.EventObject;

import org.vishia.util.InfoAppend;

public interface EventThread_ifc extends Closeable, InfoAppend {
  /**Adds an event to execute it in the thread of the implementor of this interface. 
   * if a {@link EventWithDst} is used it has an aggregation to the implementor of this and ivokes this method
   * in its {@link EventWithDst#sendEvent()}. 
   * In that case this method should not be called by an application directly. It is only a rule to implement. 
   * But this method should be used if another event type is used. Then the implementor should know what to do with the event.
   * @param order
   */
  void storeEvent ( EventObject ev);
  
  
  /**Removes this event from its queue if it is in the event queue.
   * If the element of type {@link EventWithDst} is found in the queue, it is designated with stateOfEvent = 'a'
   * This operation is thread safe. Either the event is found and removed, or it is not found because it is
   * either in execution yet or it is not queued. The event queue is a {@link ConcurrentLinkedQueue}.
   * @param ev The event which should be dequeued
   * @return true if found. 
   */
  public boolean removeFromQueue ( EventObject ev);
  
  /**Checks whether the thread is busy. This is used especially for debug,
   * if a {@link TimeOrder} waits for its finishing. If an execution remains in process because debugging, this method returns true. 
   * @return
   */
  boolean isBusy ( );


  
  /**quest whether the current thread is equal to this thread.
   * @return true if it is called in the same thread as using for this.
   */
  boolean isCurrentThread ( );
}
