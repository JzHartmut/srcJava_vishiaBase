package org.vishia.util;

import java.io.IOException;

/**This is an interface to support a more powerful toSString() capability.
 * @author hartmut Schorrig
 * @since 2023-02
 *
 */
public interface ToStringBuilder {

  /**This is a better version of a toString() concept for elaborately outputs.
   * The user can concatenate Strings without to much resources and with more simple programming
   * if all classes supports this operation. Pattern: <pre>
   * </pre> 
   * @param app an Appendable, especially a StringBuilder which have no additional tyry.catch necessities
   * @param cond Any string optional, for conditions to control the output.
//   * @throws IOException This is the necessity of {@link Appendable#append(CharSequence)}.
//   * It may be caught to throw new RuntimeException(exc) it not expected.
//   * Especially a {@link StringBuilder#append(CharSequence)} does not throw an exception. 
   */
  public StringBuilder toString(StringBuilder app, String ... cond); // {
//    app.append(toString());
//    return app;
//  }
  
}
