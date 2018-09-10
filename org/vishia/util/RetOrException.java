package org.vishia.util;

/**This class offers some static routines to handle exception situations with a special return value or with an exception.
 * @author hartmut
 *
 */
public class RetOrException
{
  /**Throws an IllegalArgumentException or returns the given value.
   * @param bExc true then exception
   * @param val the return value
   * @param text the exception text
   * @return val if bExc == false
   */
  public static int illegalArgument(boolean bExc, int val, String text) {
    if(bExc) throw new IllegalArgumentException(text);
    else return val;
  }
}
