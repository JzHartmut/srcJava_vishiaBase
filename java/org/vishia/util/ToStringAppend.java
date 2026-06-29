package org.vishia.util;

import java.io.IOException;

/**
 * see {@link ToStringBuilder}, this is the more universal version. 
 * @author Hartmut Schorrig
 * @since 2026-06-25
 *
 */
public interface ToStringAppend {

  
  /**This is a better version of a toString() concept for elaborately outputs.
   * The user can concatenate Strings without too much resources and with more simple programming
   * if all classes supports this operation. Pattern: <pre>
   * </pre> 
   * @param app an Appendable, especially a StringBuilder which have no additional try.catch necessities
   *   null not admissible, should be called with 'new StringBuilder()' if not given. 
   * @param cond Any string optional, for conditions to control the output.
   * @throws IOException This is the necessity of {@link Appendable#append(CharSequence)}.
   * It may be caught to throw new IllegalArgumentException(exc) it not expected.
   * Especially a {@link StringBuilder#append(CharSequence)} does not throw an exception. 
   */
  public Appendable toStringAppend(Appendable app, Object ... cond) throws IOException;

  
  /**Static wrapper for simple exception handling. 
   * It calls thiz. {@link #toStringAppend(Appendable, String...)} with a given appendable
   * with catch any IOException
   * @param thiz
   * @param app
   * @param cond see {@link #toStringAppend(Appendable, String...)}
   * @return same append as given
   * @throws IllegalArgumentException on exception, 
   *   this exception is not necessary to declare -> more simple programming. 
   *   An exception comes usual only on unexpected situations, never if a StringBuilder is given as Appendable.
   *   If it comes really, then it comes usual even before with the same Appendable, which may be thrown
   *   to elaborate the problem.   
   * 
   */
  public static Appendable toStringAppendS(ToStringAppend thiz, Appendable app, Object ... cond) {
    try {
      return thiz.toStringAppend(app, cond);
    } catch (IOException exc) {
      throw new IllegalArgumentException(exc.getCause());
    }
  }
  
  
    /**Static wrapper for more simple exception handling for a simple toString() implementation. 
     * It calls thiz. {@link #toStringAppend(Appendable, String...)} with a new StringBuilder()
     * @param thiz
     * @param cond see {@link #toStringAppend(Appendable, String...)}
     * @return String representaion from called {@link #toStringAppend(Appendable, String...)}
     * @throws never a RuntimeException because StringBuilder.append does never throw. 
     * 
     */
    public static String toStringS(ToStringAppend thiz, Object ... cond) {
      try {
        return thiz.toStringAppend(new StringBuilder(), cond).toString();
      } catch (IOException exc) {
        throw new RuntimeException(exc.getCause());
      }

  
  }
  
  
}
