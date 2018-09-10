package org.vishia.bridgeC;

/**This class is an analog to the MemC class in C. It should represent an amount of memory. 
 * For Standard Java implementation it is a dummy.
 * 
 * @author JcHartmut
 *
 */
public class MemC
{

  /**version, history and license:
   * <ul>
   * <li>2012-08-20 Hartmut new {@link #free(Object)}
   * <li>2011-01-05 Hartmut created
   * </ul>
   * 
   * <b>Copyright/Copyleft</b>:
   * For this source the LGPL Lesser General Public License,
   * published by the Free Software Foundation is valid.
   * It means:
   * <ol>
   * <li> You can use this source without any restriction for any desired purpose.
   * <li> You can redistribute copies of this source to everybody.
   * <li> Every user of this source, also the user of redistribute copies
   *    with or without payment, must accept this license for further using.
   * <li> But the LPGL ist not appropriate for a whole software product,
   *    if this source is only a part of them. It means, the user
   *    must publish this part of source,
   *    but don't need to publish the whole source of the own product.
   * <li> You can study and modify (improve) this source
   *    for own using or for redistribution, but you have to license the
   *    modified sources likewise under this LGPL Lesser General Public License.
   *    You mustn't delete this Copyright/Copyleft inscription in this source file.
   * </ol>
   * If you are intent to use this sources without publishing its usage, you can get
   * a second license subscribing a special contract with the author. 
   * 
   * @author Hartmut Schorrig = hartmut.schorrig@vishia.de
   * 
   * 
   */
  public final static int version = 20120820;

  
  
  public MemC(int nrofBytes){}
  
  public static MemC alloc(int nrofBytes){ return null; }
  
  /**This method can be called in Java environments whenever the memory should be freed for C-usage.
   * In java it is empty because the garbage collector does that.
   * @param obj any object which should be freed.
   */
  public static void free(Object obj){}
}
