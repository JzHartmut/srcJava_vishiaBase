package org.vishia.util;

import java.io.IOException;

public class ObjectVishia {

  /**This is a better version of a toString() concept for elaborately outputs.
   * The user can concatenate Strings without to much resources and with more simple programming
   * if all classes supports this operation. Pattern: <pre>
   * </pre> 
   * @param app any Appendable, maybe especially a StringBuilder.
   * @param cond Any string optional, for conditions to control the output.
   * @throws IOException This is the necessity of {@link Appendable#append(CharSequence)}.
   * It may be caught to throw new RuntimeException(exc) it not expected.
   * Especially a {@link StringBuilder#append(CharSequence)} does not throw an exception. 
   */
  public Appendable toString(Appendable app, String ... cond) throws IOException {
    app.append(toString());
    return app;
  }
  
}
