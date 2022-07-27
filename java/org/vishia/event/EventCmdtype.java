package org.vishia.event;


public class EventCmdtype<CmdEnum extends Enum<CmdEnum>> extends EventWithDst
{
  private static final long serialVersionUID = -137947223084491817L;

  /**Any number to identify. It is dst-specific. */
  //private final AtomicInteger cmd = new AtomicInteger();
  
  //private final AtomicReference<Cmd> cmde = new AtomicReference<Cmd>();
  
  CmdEnum cmde;
  
  /**Creates an event as a dynamic object for direct usage without a given {@link EventConsumer}.
   * This event should be used as parameter immediately for an event consuming routine.
   * The event is set as occupied already after creation. 
   * Don't call {@link #occupy(EventSource, boolean)} or {@link #occupy(EventSource, EventConsumer, EventTimerThread, boolean)} should be use
   * Don't call {@link #sendEvent()} because a destination is not given.
   * 
   * @param cmd a given Command. It may be null, it can be overwritten later with {@link #setCmd(Enum)}
   *   or using {@link #sendEvent(Enum)}.
   */
  public EventCmdtype(CmdEnum cmd){
    super(EventSource.nullSource);
    dateCreation.set(System.currentTimeMillis());
    this.cmde = cmd;
  }
  
  
  /**Creates an event as a static object for re-usage. Use {@link #occupy(Object, EventConsumer, EventTimerThread)}
   * before first usage. Use {@link #relinquish()} to release the usage. 
   * 
   */
  public EventCmdtype(){ super(); }
  
  
  
  /**Creates an event as dynamic object for usage. Use {@link #relinquish()} after the event is used and it is not referenced
   * anymore. 
   * @param source Source of the event. If null then the event is not occupied, especially the {@link #dateCreation()} 
   *   is set to 0.
   * @param refData Associated data to the event. It is the source of the event.
   * @param consumer The destination object for the event.
   * @param thread an optional thread to store the event in an event queue, maybe null.
   */
  public EventCmdtype(EventSource source, EventConsumer consumer, EventTimerThread_ifc thread){ super(source, consumer, thread);}
  
  

  public void setCmd(CmdEnum cmd){ this.cmde = cmd; }
  
  
  
  public CmdEnum getCmd(){ return cmde; }

  
  /**Sends this event to its destination instance.
   * The event is used to send only if it is not in use yet. See <a href="#lifecycle">Life cycle of an event object</a>.
   * <ul>
   * <li>Either the element {@link #evDstThread} is not null, then the event is put in the queue
   *   and the event thread is notified.
   * <li>Or the dstQueue is null, then the {@link #evDst}.{@link EventConsumer#processEvent(Event this)}
   *   is invoked. After them this event itself is relinquished because it was applicated.
   * </ul>
   * <br><br>
   * 
   * @param cmd The cmd to complete the event.
   * @return true if the event was sent.
   */
  public boolean sendEvent(CmdEnum cmd){
    cmde = cmd;
    return sendEvent();
    /*
    if(source == null)
      source = null;
    CmdEnum cmd1 = this.cmde.get();
    //int value = cmd1.ordinal();
    boolean bOk = (cmd1 == null); 
    if(bOk) {
      bOk = this.cmde.compareAndSet(cmd1, cmd);
      if(bOk){
        sendEventAgain();
      } else {
        notifyShouldSentButInUse();
      }
    } else {
      notifyShouldSentButInUse();
    }
    return bOk;
    */
  }

  
}
