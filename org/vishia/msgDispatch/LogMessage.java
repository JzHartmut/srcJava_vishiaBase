package org.vishia.msgDispatch;
/**@changes:
===2008-02-03 HScho===
*new: method isOnline

*/

import org.vishia.bridgeC.OS_TimeStamp;
import org.vishia.bridgeC.VaArgBuffer;
import org.vishia.bridgeC.Va_list;

//import java.util.Date;

/**An interface to write messages or log entries to any destination.
 * The basic idea is: The messages and logs are dispatched via a number. 
 * The number have two aspects:
 * <ul>
 * <li>The number identifies the message or kind of log entry.
 * <li>The number is used to select receiver(s) of the message or log entry.
 * <ul> 
 * Any message can be dispatched to one or more destinations which evaluate or store the message,
 * or the message can be ignored. A log entry is a special using of a message: 
 * It should only written in a log file or display, without any forcing of reactions.
 * But a log can also be dispatched to one or more destinations (log files) or can be ignored.
 * Whether a message is only a log, or the message forces actions at destination, 
 * is determined by the destination itself. The source produces the message only. 
 * The source doesn't determine the using of it.
 * <br><br>
 * A Message comprises the possibility of "coming" and "going" of the associated state. 
 * If the message are "coming", the state is activated, if it is "going", the state is released.
 * To tag the state, the identNumber is given positive for "coming" and negative for "going". 
 * It is a simple economization of calling arguments, because mostly or by first using, 
 * there is no state identify of the message. Only some chosen messages have a state identify,
 * or a state comes with the message in continue of implementations.
 * <br><br> 
 * A Message has a text. 
 * The text is used immediate if it is a textual saved log entry, readable by human. 
 * The text may be unused if the message forces actions in a technical system. 
 * The text may be unused also, if a destination medium has its own text processor.
 * If the message should be evaluated not only as human read, 
 * the identNumber have to be the correct identification. 
 * The text should only be a comment, human readable.
 * <br><br> 
 * A message has some values. The values should be displayed in the textual representation. 
 * The <code>String.format(String, Object...)</code>-Method should used to display the text.
 * But the values may be also important for evaluating the message in the destination. 
 * The meaning of the values depends on the kind of message, identified by the number.    
 * <br><br> 
 * A message has a time stamp. The time stamp may be built automatically 
 * at the time while creating the message. But it is possible to supply a time stamp to the message.
 * It is important, if the signal or event associated to the message has a deterministic time stamp
 * created at example in hardware. Following common usages the time stamp is an absolute time 
 * in milliseconds, represented by a <code>java.util.Date</code> Object. 
 * In a realtime system, compiled with C-Language, the struct Date may have a microsecond resolution
 * and another base year, but it should be absolute. 
 *    
 * @author Hartmut Schorrig
 *
 */
public interface LogMessage
{
  /**Sends a message. The timestamp of the message is build with the system time. 
   * All other parameter are identically see {@link #sendMsg(int, OS_TimeStamp, String, Object...)}.
   * @param identNumber of the message. If it is negative, it is the same message as positive number,
   *                    but with information 'going state', where the positive number is 'coming state'.
   * @param text The text representation of the message, format string, see java.lang.String.format(..).
   *             @pjava2c=zeroTermString.
   * @param args 0, 1 or more arguments of any type. 
   *             The interpretation of the arguments is controlled by param text.
   * @return TODO
   */  
  public boolean sendMsg(int identNumber, String text, Object... args);
  //{ return sendMsgVaList(identNumber, OS_TimeStamp.os_getDateTime(), text, VaArgBuffer.represent(args).get_va_list());
  //}

  /**Sends a message.
   * 
   * @param identNumber of the message. If it is negative, it is the same message as positive number,
   *                    but with information 'going state', where the positive number is 'coming state'.
   * @param creationTime absolute time stamp. @Java2C=perValue.
   * @param text The text representation of the message, format string, see java.lang.String.format(..).
   *             @pjava2c=zeroTermString.
   * @param args 0, 1 or more arguments of any type. 
   *             The interpretation of the arguments is controlled by param text.
   * @return TODO
   */
  public boolean sendMsgTime(int identNumber, OS_TimeStamp creationTime, String text, Object... args);
  //{ return sendMsgVaList(identNumber, creationTime, text, VaArgBuffer.represent(args).get_va_list());
  //}
  

  
  /**Sends a message. The functionality and the calling parameters are identically 
   * with {@link #sendMsg(int, OS_TimeStamp, String, Object...)}, but the parameter args is varied:  
   * @param identNumber
   * @param creationTime
   * @param text
   * @param typeArgs Type chars, ZCBSIJFD for boolean, char, byte, short, int, long, float double. 
   *        @java2c=zeroTermString.
   * @param args Reference to a buffer which contains the values for a variable argument list.
   *             <br>
   *             In C implementation it is a reference either to the stack, or to a buffer elsewhere,
   *             but the reference type is appropriate to provide the values in stack
   *             for calling routines with variable argument list such as 
   *             <code>vprintf(buffer, text, args)</code>.
   *             The referenced instance shouldn't accepted as persistent outside processing time 
   *             of the called routine. Therefore stack content is able to provide.
   *             <br>
   *             In Java it is a special class wrapping a Object[] tantamount to a Object...
   *             as variable argument list. Using of this wrapper class is only a concession
   *             to C-programming, because in Java an Object[] would adequate.
   * @return true than okay. It is possible, that a destination for dispatching is not available yet.
   *         Than the routine returns false. That is for special outputs of message dispatcher. 
   *         Normally the user shouldn't realize false here and react anywise. 
   *         If a message isn't able to transport, it is not visible in the creating thread. 
   *         It is possible that a message is lost anywhere in transportation way. In Generally,
   *         to secure a complex systems functionality, any timeouts, repeats 
   *         and backup strategies are necessary 
   *         in the supervise software above sending a single message.           
   */
  public abstract boolean sendMsgVaList(int identNumber, OS_TimeStamp creationTime, String text, Va_list args);

  /**Only preliminary, because Java2C doesn't support implementation of interfaces yet.
   * This method is implemented in C in another kind.
   * @param src 
   * @return the src.
   */
  //public final static LogMessage convertFromMsgDispatcher(LogMessage src){ return src; }
  
  /**A call of this method closes the devices, which processed the message. It is abstract. 
   * It depends from the kind of device, what <code>close</code> mean. 
   * If the device is a log file writer it should be clearly.
   * <code>close</code> may mean, the processing of messages is finite temporary. 
   * An <code>open</code> occurs automatically, if a new message is dispatched. 
   */
  public abstract void close();
  
  /**A call of this method causes an activating of transmission of all messages since last flush. 
   * It is abstract. It depends from the kind of device, what <code>flush</code> mean. 
   * If the device is a log file writer it should be clearly.
   * <code>flush</code> may mean, the processing of messages is ready to transmit yet. 
   */
  public abstract void flush();
  
  /**Checks whether the message output is available. */
  public abstract boolean isOnline();
  
  /**It should be implemented especially for a File-Output to flush or close
   * the file in a raster of some seconds. 
   * This routine should be called only in the same thread like the queued output,
   * It is called inside the {@link MsgDispatcher.DispatcherThread#run()}.
   * 
   */
  //public abstract void tickAndFlushOrClose();
  
  
}
