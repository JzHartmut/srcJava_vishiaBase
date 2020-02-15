package org.vishia.util;

//import java.lang.annotation.Retention;
//import java.lang.annotation.RetentionPolicy;


/**This interface determines how a field can be accessed or how the reference-return value of an operation can be used.
 * @author Hartmut Schorrig
 *
 */
public class AccessPolicy
{

  
  /**Annotation for an operation which do not modify its instance
   * or for a reference which cannot be used to modify the data.
   *
   */
  public @interface ReadOnly{}
  
  /**Annotation for a argument. 
   * It means that the reference given with this argument is only evaluated in the operation, 
   * the reference itself is never stored. 
   *
   */
  public @interface NonPersist{}
  
  /**Annotation for an argument or field which is definite changed 
   * especially it is an reference as argument of an operation.  
   *
   */
  public @interface ChangeAccess{}
  
  /**Annotation for an operation which do not modify its instance
   * and which's returned reference is @ReadOnly itself, can only be assigned to a @ReadOnly reference.
   */
  public @interface ReadOnlyRet{}
}
