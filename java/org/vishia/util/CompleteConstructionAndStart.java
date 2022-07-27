package org.vishia.util;

/**This interface supports a building and startup-process of static instances in three stage.
 * Use the following pattern:
 * <pre>
class Example{

  private final List<CompleteConstructionAndStart> composites = new LinkedList<CompleteConstructionAndStart>();

  Example(){
    AnyComposite comps = new AnyComposite(param, existingAggregations);
    composites.add(variableMng);
    this.comps = comps;  //maybe cast to an interface type.
  }
  
  @Override public void completeConstruction(){
    otherAggreation = comps.getIts
    for(CompleteConstructionAndStart composite: composites){
      composite.completeConstruction();
    }
  }
  
  @Override public void startupThreads(){
    for(CompleteConstructionAndStart composite: composites){
      composite.startupThreads();
    }
  }
}
 * </pre>
 *
 * @author Hartmut Schorrig
 *
 */
public interface CompleteConstructionAndStart
{
  /**This routine should be called after all instances were created. 
   * Sometimes instances should be referred vice-versa or any referred instance from an aggregation
   * should be known.
   */
  void completeConstruction();
  
  /**This routine should be called from the main builder and then all children if the 
   * {@link #completeConstruction()} was called for all.
   * It should implement starting of threads, if necessary. Or assign start data.
   */
  void startupThreads();
}
