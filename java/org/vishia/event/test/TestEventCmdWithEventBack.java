//D:/vishia/Java/srcJava_vishiaBase/org/vishia/event/test/TestEventCmdWithEventBack.java
//==JZcmd==
//JZcmd Obj a = java org.vishia.event.test.TestEventCmdWithEventBack.main(null);
//==endJZcmd==
package org.vishia.event.test;

import java.util.EventObject;

import org.vishia.event.EventCmdtypeWithBackEvent;
import org.vishia.event.EventConsumer;

public class TestEventCmdWithEventBack
{
  enum EnumCmd{ cmdA, cmdB};
  
  enum EnumCmdBack{ cmdX, cmdY};
  
  
  
  
  
  /**defines the class for the callback event. The opponent type is MyEvent. */
  class MyEventBack extends EventCmdtypeWithBackEvent< EnumCmdBack, MyEvent>
  {
    private static final long serialVersionUID = 1L;

    /**package private or private constructor. */
    private MyEventBack(EventConsumer callback) {
      super(null, callback, null, null); //here without opponent, just unknown.
    }

  }

  
  
  /**defines the class for the forward event. The opponent type is MyEventBack. */
  class MyEvent extends EventCmdtypeWithBackEvent < EnumCmd, MyEventBack>
  {
    private static final long serialVersionUID = 1L;

 
    public MyEvent(EventConsumer dst, EventConsumer callback) {
      //creates the back event as opponent:
      super(null, dst, null, new MyEventBack(callback));
      getOpponent().setOpponent(this); //set the backward opponent reference
   }
  }

  
  
  EventConsumer responder = new EventConsumer() {
    @Override public int processEvent(EventObject evArg){
      assert(evArg instanceof MyEvent);
      MyEvent ev = (MyEvent)evArg;
      MyEventBack eventBack = ev.getOpponent();
      EnumCmd cmd = ev.getCmd();
      switch(cmd) {
        case cmdA: eventBack.sendEvent(EnumCmdBack.cmdX); break;
        case cmdB: eventBack.sendEvent(EnumCmdBack.cmdY); break;
      }
      return mEventConsumed;
    }
  };
  
  

  EventConsumer callback = new EventConsumer() {
    @Override public int processEvent(EventObject evArg){
      assert(evArg instanceof MyEventBack);
      MyEventBack ev = (MyEventBack)evArg;
      MyEvent eventBack = (MyEvent)ev.getOpponent();
      EnumCmdBack cmd = ev.getCmd();
      switch(cmd) {
        case cmdX: eventBack.sendEvent(EnumCmd.cmdB); break;
        case cmdY: finish = true; break;
      }
      return mEventConsumed;
    }
  };
  
  boolean finish;
  
  
  /**The event with the callback event as opponent. */
  MyEvent ev = new MyEvent(responder, callback);
  
  
  public static final void main(String[] args){
    TestEventCmdWithEventBack main = new TestEventCmdWithEventBack();
    main.execute();
  }

  
  
  void execute(){
    ev.sendEvent(EnumCmd.cmdA);
  }
  
  
  
}
