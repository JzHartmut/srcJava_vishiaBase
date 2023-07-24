package org.vishia.event;

import java.util.EventObject;

/**Base class for all event consumer, same as implementation of the simple {@link EventConsumer} interface,
 * but the {@link EventConsumer#awaitExecution(long)} is already implemented here.
 * The {@link EventConsumer#processEvent(java.util.EventObject)} implementation
 * should  call {@link #setDone(String)} on receive and execute an event
 * if the event succeeds the execution or succeeds with exception or error.
 * <br><br>
 * How the 'done' information may be gotten from the event is not able to clarify from this universal base class,
 * the Payload of the event for {@link EventConsumer#processEvent(java.util.EventObject)} is not defined here.
 * Hence the implementation of 'processEvent(...)' should be done in the specific kind in the inherit class.
 * 
 * @author Hartmut Schorrig
 *
 */
public class EventConsumerAwait<T_PayloadBack extends PayloadBack, T_Payload extends Payload> implements EventConsumer{

  /**Version, history and license.
   * <ul>
   * <li>2023-07-24 The class is enhanced as own usable class with features which were implemented before
   *   in {@link org.vishia.fileRemote.FileRemoteProgressEventConsumer}:
   *   <ul><li>The Consumer contains the own event reference which should be consumed here, ready to use.
   *   <li>The payload data are referenced. Whereby the {@link PayloadBack} contains the minimal necessary payload to test done and error.
   *   <li>The class is no more abstract, it is usable whenever only the done or error should be tested as callback.
   *   </ul>
   * <li>2023-07-22 possible bug fixed: {@link #awaitExecution(long, boolean)}: If the event was cleaned
   *   without success this operation should be left. It is done via {@link #bWait} = false test.
   *   The adequate notifyAll in {@link #clean()} wakes up the 'await...'.  
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
  public static final String version = "2023-07-24";

  /**This cmdData are used and reused for calling commands to the {@link FileRemote#device()} during walking. 
   * It are referenced from evBack.
   */
  protected final T_Payload cmdData;
  
  /**Data for the progress and success back event used and reused for all back event calling, referenced from {@link #evBack}   */
  protected final T_PayloadBack progressData;

  /**Back event with the evCmd as opponent. Because the opponent has no thread and no Consumer as event destination, 
   * only its payload is used as {@link T_Payload} for internal commands which are executed if possible in the same thread. 
   * The back event is used for check success, and also necessary if the device uses communication and/or an own thread.
   * The EventConsumer for this evBack is this own class. 
   * It offers {@link #awaitExecution(long, boolean)} in its base class {@link FileRemoteProgressEventConsumer}. 
   */
  public final EventWithDst<T_PayloadBack, T_Payload> evBack;

  public final String name;
  

  
  /**It is set in {@link #setDone(String)}, should be called  if the event was received with the "done" information.
   * It is used to continue after {@link #awaitExecution(long, boolean)}. */
  protected boolean bDone;
  
  /**Marker that {@link Object#wait()} was called in {@link #awaitExecution(long, boolean)}. */
  private boolean bWait;
  
  /**Set in {@link #setDone(String)} as info about error from the event.  */
  protected String sError;

  
  /**This reference should be used if the Consumer implementor (the inherit class) should offer a specific thread
   * where the events are intermediately stored before execution in this consumer.  
   * It should be set on construction of the inherit consumer instance or set to null if not used.
   */
  protected final EventThread_ifc evThread;
  

  /**Called from the inherit class.
   * @param evThread maybe null if not used here.
   */
  protected EventConsumerAwait ( String name, T_PayloadBack backData, EventThread_ifc evThread, T_Payload cmdData, EventThread_ifc cmdThread) {
    this.cmdData = cmdData;
    this.progressData = backData;
    this.evBack = new EventWithDst<T_PayloadBack, T_Payload> (
        "ev" + name, null, this, evThread, backData, "evCmd" + name, null, null, cmdThread, cmdData);
      this.name = name;

    this.evThread = evThread;  
  }

  /**If the inherit consumer defines a specific thread, get it here.
   * Else returns null.
   */
  @Override public EventThread_ifc evThread () {
    return this.evThread;
  }

  
  
  /**This operation should be overridden and called as super.processEvent()
   * if more should be done with the progress event.
   * It is sufficient if only success or error is expected. 
   * @param progress progress data to inform
   * @param evCmd The command event may be given as opponent of the progress event for feedback,
   *   for example if a quest is given ("skip/override" etc.)
   *   This can be used in the overridden operations. 
   * @return 
   */
  protected int processEvent(T_PayloadBack progress, EventWithDst<T_Payload, T_PayloadBack> evCmd) {
    if(progress.done()) {                                // check whether done() is given, all other is not interest.
      this.setDone(progress.error());                     // setDone awakes a waiting thread which has called await
    }
    return mEventDonotRelinquish;      // will be relinquished after wait
  }
  

  
  
  /**This operation is called from {@link EventWithDst#sendEvent(Object)} or from {@link org.vishia.event.EventTimerThread#stepThread()}
   * as overridden. It prepares and calls {@link #processEvent(T_PayloadBack, EventWithDst)
   * which is overridden by the application.
   */
  @Override public int processEvent(EventObject evRaw) {
    assert(evRaw instanceof EventWithDst);                 // an EventWithDst is expected
    @SuppressWarnings("unchecked") EventWithDst<T_PayloadBack, T_Payload> ev = 
        (EventWithDst<T_PayloadBack, T_Payload>) evRaw;  // cast it to the basic type
    T_PayloadBack progress = ev.data();                         // get and test type of the data
    EventWithDst<T_Payload, T_PayloadBack> evCmd = ev.getOpponent();
    return processEvent(progress, evCmd);
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
   * @return true if done was detected, false on timeout or if the event was cleaned. 
   */
  public final boolean awaitExecution(long timeout, boolean clearDone) { 
    long timeEnd = System.currentTimeMillis() + timeout; 
    boolean ret;
    synchronized(this) {
      long waitingTime = 1;
      while(!this.bDone && (waitingTime >0 || timeout == 0)) {
        waitingTime = timeout == 0 ? 0 : timeEnd - System.currentTimeMillis();
        if(waitingTime > 0 || timeout == 0 ) { //should wait, or special: wait till end of routine.
          this.bWait = true;
          try{ wait(waitingTime); } catch(InterruptedException exc){}
          if(!this.bWait) break;                 // break the while loop especially if clean() was called
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
        this.bWait = false;            // set bWait anytime to false, clean an unexpected situation. 
        notifyAll();                   // if awaitExecution was called it ends with the bWait=false information.
      }
    }
    return this;
  }

}
