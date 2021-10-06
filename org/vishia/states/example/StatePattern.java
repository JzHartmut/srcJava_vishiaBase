package org.vishia.states.example;

import java.util.EventObject;

import org.vishia.event.EventCmdtype;
import org.vishia.event.EventTimerThread;
import org.vishia.event.EventWithDst;
import org.vishia.states.StateComposite;
import org.vishia.states.StateMachine;
import org.vishia.states.StateParallel;
import org.vishia.states.StateSimple;

public class StatePattern
{
  /**Only syntax check, common example: */
  EventWithDst evA = new EventWithDst();
  enum CmdX { cmdY };
  class EvCmdX extends EventCmdtype<CmdX>{ private static final long serialVersionUID = 1L;}
  
  
  
  /**Only syntax check, common example: */
  class MyStates extends StateMachine
  { 
    MyStates(EventTimerThread thread){ super("name", thread); }
  
    class StateA extends StateSimple 
    { 
      final boolean isDefault = true;  //default-state
      
      @Override protected int entry(EventObject ev) {
        System.out.println("Entry in state A");
        return 0;
      }
      
      @Override protected void exit() {
        System.out.println("Exit from state A");
      }
      
      Trans transTo_B1 = new Trans(StateB.StateB1.class);
      
      Trans transParallel = new Trans( 
            StateB.StateP.StateP1.StateP1A.class
          , StateB.StateP.StateP2.StateP2X.class) { 
        @Override protected void action(EventObject ev) { 
          System.out.println("action of transition StateA -> Parallel P1A, P2X");
        }
      };
      
      @Override protected Trans checkTrans(EventObject ev){
        if(ev == evA) return transTo_B1.eventConsumed();
        else if(ev instanceof EvCmdX && ((EvCmdX)ev).getCmd() == CmdX.cmdY)
          return transParallel.eventConsumed();
        else return null;
      }  
    }  
    
    
    class StateB extends StateComposite {
      class StateB1 extends StateSimple {    } //inner state
      
      class StateP extends StateParallel {    
        //container for parallel states
        class StateP1 extends StateComposite {
          class StateP1A extends StateSimple {   }
          //...
        }  
        class StateP2 extends StateComposite {
          class StateP2X extends StateSimple {   }
          //...
        }
        
        Trans toStateA = new Trans(StateA.class);
        
        @Override protected Trans checkTrans(EventObject ev){
          if(  stateMachine.isInState(StateP1.StateP1A.class) 
            && stateMachine.isInState(StateP2.StateP2X.class)) return toStateA;
          else return null;
        }

      }  
      //....
    }
  };
  
  
  
  
}
