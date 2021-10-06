package org.vishia.event.test;

import java.util.EventObject;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.vishia.event.EventConsumer;
import org.vishia.event.EventTimerThread;
import org.vishia.event.TimeOrder;
import org.vishia.event.EventWithDst;

/**This class creates a time order and executes it in an extra thread.
 * Two threads were used, an {@link EventTimerThread} only for the timer organization
 * and the 
 * @author hartmut
 *
 */
public class TestTimeOrder
{
  public static void main(String[] args) {
    (new TestTimeOrder()).run();
  }
  
  boolean shouldRunning = true;
  
  ExecThread execThread = new ExecThread();
   
  /**Method to enqueue the time order in the queue of the execThread.
   * It is used as destination for the {@link EventWithDst} which is the base of the {@link TestTimeOrder#order},
   * used as argument of constructor of order.
   */
  private EventConsumer enqueue = new EventConsumer()  {
    @Override public int processEvent(EventObject ev)
    { execThread.addOrder((TimeOrder)ev);  //casting admissible because special using.
      return mEventConsumed;
    }
  };
  
  /**The timer thread to organize the time order. It can be used for some other time orders or state machines
   * additionally. */
  EventTimerThread threadTimer = new EventTimerThread("timer thread");
  
  
  /**a time order as inner anonymous class with its executOrder-method. */
  @SuppressWarnings("serial") 
  TimeOrder order = new TimeOrder("name", enqueue, threadTimer) {
    int counter = 5;
    @Override protected void executeOrder(){
      if(--counter <0) { 
        System.out.println("TestTimeOrder - the last time order");
        shouldRunning = false;  //terminate the thread. 
      } else {
        System.out.println("TestTimeOrder - execute the time order");
        activate(1000);  //repeat it.
      }
    }
  };

  
  
  
  
  /**The main routine of this class for test. */
  public void run() {
    threadTimer.start();
    execThread.start();
    order.activate(2000);  //first activation is done here, next activation in the time order itself.
    do {                   //wait only.
      try { Thread.sleep(100);
      } catch (InterruptedException e) {
        System.err.println("TestTimeOrder - unexpected interrupt sleep in main");
      }
    } while(shouldRunning);
    threadTimer.close();
    execThread.close();
  }
}
