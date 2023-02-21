package org.vishia.event;




/**This enum can be used to enhance for state machine events,
 * which are used also as timeout. 
 * Non enhanced it is used for a EventCmdtype as generic for a timeout event type.
 * It is checked and recognized in the operation {@link EventCmdtype#isTimeout()}-
 *  
 * @author Hartmut Schorrig
 * @since 2023-02-07
 */
public enum TimeOrderCmd {
  /**Used typically in state machines for a timeout. */
  timeout("timeout"),
  
  /**Used typical in one call of any order for a delayed operation. */
  timeOrder("timeOrder"),

  /**Used typical in a cyclically call of any order. */
  progress("progress"),
  
  /**Used typically on end of an order which calls progress between. */
  done("done"),

  /**Used typically on error of an order which calls progress between. */
  error("error"),

  /**Used typically on a situation where a question is given of an order which calls progress between. */
  ask("ask"),

  /**Used typically on a abort situation of an order which calls progress between. */
  abort("abort")
  ;
  final String ident;
  TimeOrderCmd ( String ident ){
    this.ident = ident;
  }
}
