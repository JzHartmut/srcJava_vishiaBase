package org.vishia.util;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

/**This class organizes the execution of events in a own thread. The class contains the event queue.
 * @author Hartmut Schorrig
 *
 */
public class EventThread implements Runnable, Closeable
{
  
  protected final ConcurrentLinkedQueue<Event> queueEvents = new ConcurrentLinkedQueue<Event>();

  /**The state of the thread
   * <ul>
   * <li>'.' non initialized.
   * <li>'s' started
   * <li>'r' Running, an event is processed
   * <li>'w' Waiting because the queue is empty
   * <li>'c' Should be closed, finished
   * <li>'x' finished.
   * 
   */
  protected char stateOfThread = '.';  
  

  protected final Thread thread = new Thread(this, "EventQueue");
  
  EventThread()
  {
    //thread.start();
  }
  
  
  public void startThread(){ thread.start(); }
  
  
  public void storeEvent(Event ev){
    queueEvents.offer(ev);
    synchronized(this){
      if(stateOfThread == 'w'){
        notify();
      }
    }
  }
  
  
  
  
  @Override public void run()
  { stateOfThread = 'r';
    while(stateOfThread != 'c'){
      Event event = queueEvents.poll();
      if(event !=null){
        event.dst.processEvent(event);
      } else {
        synchronized(this){
          stateOfThread = 'w';
          try{ wait(); } catch(InterruptedException exc){}
          if(stateOfThread == 'w') { stateOfThread = 'r'; }
        }
      }
    }
    stateOfThread = 'x';
  }


  @Override public void close() throws IOException
  { synchronized(this){
      if(stateOfThread == 'w'){
        notify();
      }
      stateOfThread = 'c';
    }  
  }
    
  
}
