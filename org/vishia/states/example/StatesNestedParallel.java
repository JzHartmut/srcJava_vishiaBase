package org.vishia.states.example;

import java.util.EventObject;

import org.vishia.event.EventCmdtype;
import org.vishia.event.EventTimeout;
import org.vishia.event.EventTimerThread;
import org.vishia.event.EventWithDst;
import org.vishia.event.TimeOrder;
import org.vishia.msgDispatch.MsgRedirectConsole;
import org.vishia.states.StateComposite;
import org.vishia.states.StateCompositeFlat;
import org.vishia.states.StateDeepHistory;
import org.vishia.states.StateParallel;
import org.vishia.states.StateSimple;
import org.vishia.states.StateMachine;

public class StatesNestedParallel
{
  
  public class Conditions 
  {
  
    boolean on_ready, on_cont;  
  
  
    boolean offAfterRunning;
    
    boolean cont;
    
    boolean off;
    
    boolean finished;
  }
  
  
  /**Commands for the event.
   */
  enum CmdEvent { start, ready, cyclic, off, on_cont, testDefaultParallel};
  
  /**An event type reuseable for the state machine animation. */
  class EventA extends EventCmdtype<CmdEvent>{
    
  }
  
  /**Checks whether any given Event is from this type and has the given cmd.
   * @return true if it matches.
   */
  static boolean checkEventA(EventObject ev, CmdEvent cmd){
    return ev instanceof EventA && ((EventA)ev).getCmd() == cmd;
  }

  /**Some conditions for transition in this example. */
  Conditions cond = new Conditions();
  
  /**This is the example for the progress of a process done in the states Running etc. 
   * The value is used for state transitions.
   */
  int processValue;
  
  boolean isWorking;
  
  /**An event instance reused for the state machine animation.*/
  EventA event = new EventA();
  
  
  /**The thread to execute the state machine. */
  EventTimerThread threadEventTimer = new EventTimerThread("thread");

  /**Timer organisation. */
  //EventTimerMng timer = new EventTimerMng("timer");

  /**The state machine. It is an inner class because it should not contain any other things as states. This class is analysed by reflection
   * to complete the data for state machine execution. 
   */
  class States extends StateMachine
  {
    States(EventTimerThread thread){ super("ExampleNestedParallel", thread); }

    @Override protected int eventDebug(EventObject ev){ return super.eventDebug(ev); }
    
    class StateOff extends StateSimple
    {
      boolean isDefault;
      
      //Trans ready = new Trans(StateWork.StateReady.class);
      
      //Trans history = new Trans(StateWork.StateActive.class);
      
      //@Override protected Trans selectTrans(EventObject ev) { return super.selectTrans(ev); }
      /*
      if(cond.on) {
          //choice
          if(cond.cont) return history;
          else return ready;
        } else return null;
      }
      */
      
      @Override public int entry(EventObject ev){
        cond.off = false;
        return mRunToComplete;
      }
      
      
      public TransChoice on = new TransChoice() { 
        
        Trans cont_history = new Trans(StateWork.History.class); 
        Trans ready = new Trans(StateWork.StateReady.class);
          
        @Override public Trans choice() {
          if(cond.cont){
            return cont_history;
          }
          else return ready;
        }
        
      };
      
      
      
      Trans on_Ready = new Trans(StateWork.class); 
      
      
      @Override protected Trans checkTrans(EventObject ev) {
        if(cond.on_ready) {
          return on_Ready;
        }
        else if(  ev instanceof EventA 
            && ((EventA)ev).getCmd() == CmdEvent.start ){
          return on.choice().eventConsumed();
        }
        else if(cond.on_cont){
          cond.on_cont = false;
          return on.choice();
        }
        //else if(cond.on) return on_Ready;
        else return null;
      }
      
    };
    
    
    class StateWork extends StateComposite
    {
      class History extends StateDeepHistory{}
      
      Trans to_off = new Trans(StateOff.class);
      
      @Override protected Trans checkTrans(EventObject ev){
        if(cond.off) {
          cond.off = false;
          isWorking = false;
          return to_off;
        }
        else return null;
      }
      
      class StateReady extends StateSimple
      {
        final boolean isDefault = true;
        
        Trans start_Running = new Trans( StateActive.StateActive1.StateRunning.StateRunning2.StateRunning21.class
                                       , StateActive.StateActive2.StateRemainOn.class);
        
        Trans testDefaultParallel = new Trans(StateActive.class);
        
        @Override protected Trans checkTrans(EventObject ev){
          if(checkEventA(ev,CmdEvent.start)) {
            start_Running.doExit();
            processValue = 0;
            return start_Running;
          } else if(checkEventA(ev,CmdEvent.testDefaultParallel)) {
            processValue = 0;
            return testDefaultParallel;
          }
          else return null;
        }
          
      }//StateReady
      
      
      class StateActive extends StateParallel
      {
        //TransJoin to_off = (new TransJoin(StateOff.class)).srcStates(StateActive2.StateShouldOff.class, StateActive1.StateFinit.class);
        private TransJoin to_off = new TransJoin(StateOff.class) {
          { srcStates(StateActive2.StateShouldOff.class, StateActive1.StateFinit.class); }
          @Override protected void action(EventObject ev) {
            cond.finished = true;
          }
        };
        
        Trans testOff = new Trans(States.StateOff.class);
        
        @Override protected Trans checkTrans(EventObject ev){
          /*if(true){
            return testOff;
          }*/
          if(processValue == 9994) {
            return testOff;
          } else if(to_off.joined()) {
            return to_off;
          }
          else return null;
        }

        class StateActive1 extends StateComposite
        {
          class StateRunning extends StateCompositeFlat
          { final boolean isDefault = true;
            
            @Override protected int entry(EventObject ev){ System.out.println("entry Running"); return 0; }

            @Override protected void exit(){ System.out.println(" exit Running"); }

            Timeout timeFinit = new Timeout(5000, StateFinit.class) {
              @Override protected void action(EventObject ev){
                System.out.println("  timeout");
              }
            };
            
            Trans testFinit = new Trans(StateFinit.class);
            
            /**This transition counts the {@link #processValue} only. It is invoked cyclically on a timer event.
             * @see org.vishia.states.StateSimple#checkTrans(java.util.EventObject)
             */
            @Override protected Trans checkTrans(EventObject ev){ 
              /*if(false){
                return testFinit;
              }*/
              processValue +=1;
              System.out.println("InState Running, " + processValue);
              return null; 
            }
            
            
            class StateRunning1 extends StateSimple {
              final boolean isDefault = true;
              //@Override protected int entry(EventObject ev){ System.out.println("entry " + stateId); return 0; }
              //@Override protected void exit(){ System.out.println(" exit " + stateId); }
              
              Trans trans2 = new Trans(StateRunning2.class);
              
              @Override protected Trans checkTrans(EventObject ev) {
                if(processValue == 10) {
                  return trans2;
                } else return null;
              }
            }
            
            class StateRunning2 extends StateCompositeFlat {
              class StateRunning21 extends StateSimple {
                final boolean isDefault = true;
                
                Trans trans22 = new Trans(StateRunning22.class);
                
                @Override protected Trans checkTrans(EventObject ev) {
                  if(processValue == 3 || processValue == 7 || processValue == 11) {
                    return trans22;
                  } else return null;
                }
              }
              
              class StateRunning22 extends StateSimple {
                
                Timeout transFinit = new Timeout(1000, StateFinit.class);
                Trans trans21 = new Trans(StateRunning21.class);
                Trans trans1 = new Trans(StateRunning1.class);
                Trans testOff = new Trans(StateReady.class);
                
                @Override protected Trans checkTrans(EventObject ev) {
                  if(processValue == 9994) {
                    return testOff;
                  } else if(processValue == 5) {
                    return trans21;
                  } else if(processValue == 9) {
                    return trans1;
                  } else if(isTimeout(ev)) {
                    return transFinit;  
                  } else return null;
                }
              }
              
            }

          }
          
          
          class StateFinit extends StateSimple
          { 
            Trans transReady = new Trans(StateReady.class);
            @Override protected Trans checkTrans(EventObject ev){
              if(true) return null;
              cond.on_cont = false;
              cond.on_ready = false;
              cond.cont = false;
              return transReady;
            }
            
          }//StateFinit
          
        } 
        
        
        class StateActive2 extends StateComposite
        { 
          class StateRemainOn extends StateSimple
          { final boolean isDefault = true;
          
            Trans toShouldOff = new Trans(StateShouldOff.class);
            
            @Override protected Trans checkTrans(EventObject ev){
              if(cond.offAfterRunning) return toShouldOff;
              else return null;
            }
            
          }
          
          class StateShouldOff extends StateSimple
          {
                      
          }
        }
  
      }//class StateActive
    }
  }
  
  
  final States states;
  
  
  private StatesNestedParallel(){
    states = new States(threadEventTimer);
    states.debugTrans = true;
    states.debugEntryExit = true;
  }
  
  private void execute() {
    cond.on_ready = true;
    cond.offAfterRunning = true;
    if(event.occupy(null, states, threadEventTimer, true)){
      event.sendEvent(CmdEvent.start);
      //instead:
      //states.processEvent(event);
    }
    //EventTimeout evOff = new EventTimeout(states, threadEventTimer);
    //evOff.activate(1000);
    
    @SuppressWarnings("serial") 
    TimeOrder evOff1 = new TimeOrder("off", threadEventTimer){
      @Override protected void executeOrder(){ cond.off = true;  cond.on_cont = true; cond.cont = true;}
    };
    evOff1.occupy(null, true);
    evOff1.activate(1000);
    //cond.on = false;
    int step = 0;
    do {
      try{ Thread.sleep(100);
      } catch(InterruptedException exc) {}
      //Some stimuli:
      CmdEvent cmd = CmdEvent.cyclic;  //if overwritten then an other one.
      switch(step) {
        case 1: cond.on_ready = true; System.out.println("Env, 1-on_ready"); break;
        case 2: cmd = CmdEvent.start;
        case 3: cond.on_ready = false; cond.off = true; System.out.println("Env, 3-off"); break;
        case 5: cond.off = false; cond.on_cont = true; cond.cont = true; System.out.println("Env, 5-on_cont"); break;  //forces history entry.
        case 8: cond.off = true; break;
        case 10: cond.on_ready = true; cmd = CmdEvent.testDefaultParallel; break;
      } //step;
      if(event.occupy(null, states, threadEventTimer, true)){
        event.sendEvent(cmd);  //animate the state machine with cmd maybe cyclically to check some conditions.
      }
    } while(++step < 100);  //runs 10 seconds. //!cond.finished); //!states.isInState(States.StateOff.class));
    try{
      //timer.close();
      threadEventTimer.close();
    } catch(Exception exc){}
    try{ Thread.sleep(100);  //wait for end of the threadEventTimer.
    } catch(InterruptedException exc) {}

  }
  
  
  
  public static void main(String[] args){
    MsgRedirectConsole msg = new MsgRedirectConsole();
    StatesNestedParallel main = new StatesNestedParallel();
    main.execute();
  }

  
}
