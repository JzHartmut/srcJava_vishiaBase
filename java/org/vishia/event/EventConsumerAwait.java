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

  /**Version, history and license.
   * <ul>
   * <li>2023-02-12 Hartmut created, because universal approach to wait till done
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
  public static final String version = "2023-03-12";

  
  /**It is set in {@link #setDone(String)}, should be called  if the event was received with the "done" information.
   * It is used to continue after {@link #awaitExecution(long, boolean)}. */
  protected boolean bDone;
  
  /**Marker that {@link Object#wait()} was called in {@link #awaitExecution(long, boolean)}. */
  private boolean bWait;
  
  /**Set in {@link #setDone(String)} as info about error from the event.  */
  protected String sError;

  
  protected final EventThread_ifc evThread;
  

  public EventConsumerAwait ( EventThread_ifc evThread) {
    this.evThread = evThread;  
  }

  /**
   * Can be overridden if the 
   */
  @Override public EventThread_ifc evThread () {
    return this.evThread;
  }


  /**This operation should be called by the implementing class if an event was received with "done" information. 
   * @param sError If the event contains an error string, it may be helpfully.
   */
  protected final void setDone(String sError) {
    this.sError = sError;
    this.bDone = true;
    if(this.bWait) {
      synchronized(this) {
        notify();
      }
    }
  }
  

  
  /**Quest whether done was coming. 
   * @return {@link #bDone}
   */
  public boolean done() { return this.bDone; }
  
  
  /**Awaits for execution till "done" was set by the event which uses this destination or till the timeout is expired.
   * or more exact, till {@link #setDone(String)} was called. 
   * @param timeout milliseconds timeout
   * @param clearDone clear the done flag again before return (not on entry!). Use {@link #clean()} to clean done before entry.
   * @return true if done was detected, false on timeout. 
   */
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

  
  /**Cleans {@link #bDone} and also {@link #sError}.
   * It should be called on initializing the event, 
   * in any case before {@link #awaitExecution(long, boolean)} was called.
   */
  public EventConsumerAwait clean ( ) {
    this.bDone = false;
    this.sError = null;
    if(this.bWait) {                   // any thread is waiting
      synchronized(this) {
        notify();
      }
      this.bWait = false;              // set bWait anytime to false, clean an unexpected situation. 
    }
    return this;
  }

}
