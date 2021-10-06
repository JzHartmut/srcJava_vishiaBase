package org.vishia.util;

/**This interface can be used on all classes which are intent to support removing of all referenced
 * data and resources. It can be set to any class. It is universal and independent of any other.
 * 
 * @author Hartmut Schorrig
 *
 */
public interface Removeable
{
  
  /**Removes all referenced data, close all opened resources, removes all graphical widgets etc. 
   * This method prepares a dereferencing of the instance. It is similar to the concept of
   * Object.finalize(). But finalize() is designed for the last clearing before the garbage collector
   * deletes an instance. In opposite remove cleans and closes on active run time.
   * It may be proper to call remove inside the finalize() implementation. 
   * The remove() implementation should check whether some instances are removed already to support
   * a second call of remove() after a first one!
   *  
   * @return true if successfully. If false it is a hint for the application that the instance itself
   *   should not be removed.
   * */
  public boolean remove();
}
