package org.vishia.states;

/**This is the Pseudo-state for deep history transitions. It is only a marker without extra data.
 * 
 * @author Hartmut Schorrig
 *
 */
public class StateShallowHistory extends StateSimple
{
  public StateShallowHistory(){
    super();
    stateId = "shallowHistory";
    
  }

}
