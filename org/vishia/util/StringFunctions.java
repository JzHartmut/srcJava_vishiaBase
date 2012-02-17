
package org.vishia.util;

/**This class contains static String functions without any other dependency. 
 * In C the functions are contained in the Fwc/fw_String.c.
 * @author Hartmut Schorrig
 *
 */
/**
 * @author hartmut
 *
 */
public class StringFunctions {

  /**Version and history:
   * <ul>
   * <li>2012-02-19 Hartmut created: basic functions also existent in C (Java2C-usage).
   * </ul>
   */
  public final static int version = 0x20120219; 
  
  /**Returns the position of the end of an identifier.
   * @param src The input string
   * @param start at this position the indentier starts.
   * @param end max number of chars to check
   * @return 0 if src[start] doesn't match to an identifier character, number of found identifier chars after src until end.
   */
  public static int posAfterIdentifier(CharSequence src, int start, int endMax){ return posAfterIdentifier(src, start, endMax, null, null); }

  /**Returns the position of the end of an identifier.
   * @param src The input string
   * @param start at this position the identifier starts.
   * @param end max number of chars to check
   * @param additionalStartChars maybe null, some chars as additional start chars of an identifier.
   * @param additionalChars maybe null, some chars as additional chars of an identifier.
   * @return 0 if src[start] doesn't match to an identifier character, number of found identifier chars after src until end.
   */
  public static int posAfterIdentifier(CharSequence src, int start, int endMax, String additionalStartChars, String additionalChars){
    int pos = start;
    char cc = src.charAt(pos);
    if(   cc == '_' 
      || (cc >= 'A' && cc <='Z') 
      || (cc >= 'a' && cc <='z') 
      || (additionalStartChars != null && additionalStartChars.indexOf(cc)>=0)
      )
    { pos +=1;
      while(  pos < endMax 
           && (  (cc = src.charAt(pos)) == '_' 
              || (cc >= '0' && cc <='9') 
              || (cc >= 'A' && cc <='Z') 
              || (cc >= 'a' && cc <='z') 
              || (additionalChars != null && additionalChars.indexOf(cc)>=0)
           )  )
      { pos +=1; }
    }
    return pos;
  }
  

  
  
}
