package org.vishia.states.example;

import java.util.EventObject;

import org.vishia.event.EventCmdtype;
import org.vishia.states.StateComposite;
import org.vishia.states.StateSimple;
import org.vishia.states.StateMachine;
import org.vishia.states.StateSimple.Trans;

public class StateExampleSimple
{
  

  enum Ecmd{Step};
  
  EventCmdtype<Ecmd> ev = new EventCmdtype<Ecmd>();
  
  
  //class States extends StateTop
  StateMachine states1 = new StateMachine("ExampleSimple")
  {
    
    class StateIdle extends StateSimple {

      Trans step_State1 = new Trans(StateCompositeExample.class);
    
      
    }
    
    
    class StateCompositeExample extends StateComposite 
    {
      class State1 extends StateSimple {

        @SuppressWarnings("unused") 
        Trans addRequest = new Trans();
      }
      
      @SuppressWarnings("unused") 
      class State2 extends StateSimple {

        Trans addRequest = new Trans();
     
        
        Trans trans2_Idle(EventCmdtype<?> ev, Trans trans){
          if(trans == null){
            return new Trans(StateIdle.class);
          } 
          trans.retTrans = mEventConsumed;
          trans.doExit();
          trans.doEntry(ev);
          return trans;
        }
        
        
        
      }
      
    }
    
    
    
    
    
  };
  

  
  
  final public static void main(String[] args) {
    
    StateExampleSimple main = new StateExampleSimple();
    main.ev.occupy(null, true);
    main.ev.setCmd(Ecmd.Step);
    main.states1.processEvent(main.ev);
    main.ev.occupy(null, true);
    main.ev.setCmd(Ecmd.Step);
    main.states1.processEvent(main.ev);
    main.states1.processEvent(main.ev);
    main.states1.processEvent(main.ev);
  }
  
  
}
