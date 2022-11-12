package org.vishia.msgDispatch;

import java.io.IOException;

/**This class implements some operations of LogMessage which are independent of the particular implementation.
 * It is used via extends instead immediately implements the LogMessage for the particular derived classes.
 * @author Hartmut Schorrig
 *
 */
public abstract class LogMessageBase implements LogMessage {

  /**Implemented only as wrapper around simple append output.
   * It is without throws, simple usage.
   * May be adapted to message output in derived classes
   */
  @Override public void writeError(String text) {
    try {
      append("Error: ").append(text).append('\n');
    } catch(IOException exc) {
      System.err.println("Error: " + text);
    }
  }

}
