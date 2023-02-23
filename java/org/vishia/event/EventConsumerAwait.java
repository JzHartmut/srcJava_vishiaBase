package org.vishia.event;


/**Base class for all event consumer, same as implementation of the simple {@link EventConsumer} interface,
 * but the {@link EventConsumer#awaitExecution(long)} is already implemented here.
 * The {@link EventConsumer#processEvent(java.util.EventObject)} implementation
 * must set call {@link #setDone(String)} on receive and execute an event
 * if the event succeeds the execution or succeeds with exception or error.
 * 
 * @author Hartmut Schorrig
 *
 */
public abstract class EventConsumerAwait implements EventConsumer{

  protected boolean bWait, bDone;
  
  protected String sError;


  protected final void setDone(String sError) {
    this.sError = sError;
    this.bDone = true;
    if(this.bWait) {
      synchronized(this) {
        notify();
      }
    }
  }
  

  
  public boolean done() { return this.bDone; }
  
  
  /**Awaits for execution of the TimeOrder. */
  @Override public final boolean awaitExecution(long timeout, boolean clearDone) { 
    long timeEnd = System.currentTimeMillis() + timeout; 
    boolean ret;
    synchronized(this) {
      long waitingTime = 1;
      while(!this.bDone && (waitingTime >0 || timeout == 0)) {
        waitingTime = timeout == 0 ? 0 : timeEnd - System.currentTimeMillis();
        if(waitingTime > 0 || timeout == 0 ) { //should wait, or special: wait till end of routine.
          this.bWait = true;
          try{ wait(waitingTime); } catch(InterruptedException exc){}
          this.bWait = false;
        } 
      };                                         // clear bDone if clearDone.
      ret = this.bDone;
      if(clearDone) { this.bDone = false; }      // clear also if timeout is expired, expected bDone is still false.
    }
    return(ret);
  }

  
  public void clean() {
    this.bDone = false;
    this.sError = null;
    if(this.bWait) {                   // any thread is waiting
      synchronized(this) {
        notify();
      }
      this.bWait = false;              // set bWait anytime to false, clean an unexpected situation. 
    }
  }

}
