package org.vishia.stateMGen;

public class ExampleState extends ExampleStateData
{

  String StateClass = "ExampleSimple";
  String StateSuffix = "_State_ExampleSimple";
  String StateMethodSuffix = "_ExampleSimple";
  
  /**It is the off-state. This part contains a documentation of the state
   */
  class Off extends StateMBase
  {
    
    /**Comment for entry-action. Note: If nothing is specified on entry, you can remove the entry method.*/
    void entry(){ }
    
    /**Comment for all transitions. */
    void trans(){
      /**Comment for this transition. */
      if(on){ work = 1; Ready.set(); }
      /**Comment for the other transition. */
      if(start){ Off.set(); }
      /**Comment for the action-in-state*/
      else {
        counter +=1;
      }
    }
    
  }
  
  /**It is a composite state. But that is seen on the inner states only. */
  class Work extends StateMBase
  {
    
    
    void entry(){ work = 1; }
    
    void trans(){
      if(off){ work = 0; Off.set(); }
    }
    
  }
  
  
  /**This state is an inner state of Work. it extends Work. */
  class Ready extends Work
  {
    
    
    @Override
    void trans(){
      if(start){ Active.set(); }
    }
    
  }
  
    
  /**An second inner state of Work. */
  class Active extends Work
  {
    
    
    @Override
    void trans(){
      if(on){ work = 1; Ready.set(); }
    }
    
  }
  
    
  
  
}
