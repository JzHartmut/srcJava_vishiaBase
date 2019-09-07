package org.vishia.util;

//import java.lang.annotation.Retention;
//import java.lang.annotation.RetentionPolicy;


public class Annotation_ReadOnly
{

  
  /**Annotation for an operation which do not modify its instance
   * or for a reference which cannot be used to modify the data.
   *
   */
  public @interface ReadOnly{}
  
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
