package org.vishia.util;

/**Supports individual handling of assertions especially in debug phase. 
 * This interface can be implemented at application level.
 * @author Hartmut Schorrig
 *
 */
public class Assert
{

  /**This routine instantiates the assertion handling with a special user instance.
   * All invocations of the static assertion call Assert.{@link #check(boolean)} 
   * invokes the user {@link #assertion(boolean)} routine up to now.
   * @param instance The users assertion instance.
   */
  public static void setAssertionInstance(Assert instance){
    o = instance;
  }
  
  /**Checks whether an assertion is met.
   * This routine can invoke a special handling of assertion, if
   * @param shouldTrue
   */
  public static void check(boolean shouldTrue){
    if(o == null){ 
      o = new Assert(); //if no assertion instance is given, create this. 
    }
    o.assertion(shouldTrue);
  }
  
  /**This routine can handle a assertion to support debugging or reporting.
   * The Stacktrace can help to detect where the assertion occurs.
   * 
   * @param shouldTrue
   */
  protected void assertion(boolean shouldTrue){
    if(!shouldTrue)
      assert(shouldTrue);
      
  }
  
  private static Assert o;
  

}
