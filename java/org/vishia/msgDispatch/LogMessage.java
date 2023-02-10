package org.vishia.msgDispatch;
/**@changes:
===2008-02-03 HScho===
*new: method isOnline

*/

import java.text.SimpleDateFormat;
import java.util.Date;

import org.vishia.bridgeC.OS_TimeStamp;
import org.vishia.bridgeC.Va_list;

//import java.util.Date;

/**An interface to write messages or log entries to any destination.
 * The basic idea is: The messages and logs are dispatched via a number. 
 * The number have two aspects:
 * <ul>
 * <li>The number identifies the message or kind of log entry.
 * <li>The number is used to select receiver(s) of the message or log entry.
 * </ul> 
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
 * created for example in hardware. Following common usages the time stamp is an absolute time 
 * in milliseconds, represented by a <code>java.util.Date</code> Object. 
 * In a realtime system, compiled with C-Language, the struct Date may have a microsecond resolution
 * and another base year, but it should be absolute. 
 *    
 * @author Hartmut Schorrig
 *
 */
public interface LogMessage extends Appendable
{
  /**Version, history and license.
   * <ul>
   * <li>2022-12-25 Hartmut new: Some enhancements with nice static operations:
   *   {@link #timeCurr(String)} etc.
   * <li>2022-12-25 Hartmut new: {@link #timeMsg(long, String)} as helper for simple preparation an String with timestamp in absolute in ms. 
   * <li>2022-12-25 Hartmut chg: the log texts are now CharSequence, not String. This allows give a StringBuilder reference immediately.
   *   For post processing also usual a CharSequence is sufficient. This is a refactoring in all message and log sources.
   *   But it does not influence user sources. Note that a String is also a CharSequence. 
   * <li>2022-09-23 Hartmut new: Most of the operations from the {@link org.vishia.mainCmd.MainCmdLogging_ifc} are moved to this.
   *   It makes possible that some programs such as {@link org.vishia.zbnf.ZbnfParser} now uses the basically LogMessage
   *   instead the older and more complex MainCmdLogging_ifc. 
   *   Default implementations for all additional operations are contains in {@link LogMessageBase}. 
   * <li>2022-09-23 Hartmut new: {@link #writeError(String)} is declared in {@link org.vishia.mainCmd.MainCmd_ifc}
   *   and used sometimes. It is also yet defined here, but redirected to {@link #append(CharSequence)}
   *   in the new LogMessageBase implementation. 
   * <li>2022-09-23 Hartmut new: extends now Appendable, hence usefull for more approaches,
   *   see also changes in {@link LogMessageStream}. 
   * <li>2014-06-17 Hartmut chg: meaning of return value of {@link #sendMsg(int, String, Object...)} described.
   *   It was not defined and maybe not used in the last 8 years.
   * <li>2008-02-03 Hartmut new: method isOnline
   * <li>2006- created. Concept in Java and C/C++ especially for debug on runtime.
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
   * <li> But the LPGL is not appropriate for a whole software product,
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
  //@SuppressWarnings("hiding")
  static final public String sVersion = "2023-02-10";

  
  /*---------------------------------------------------------------------------------------------------------*/
  /** report level to indicate the report should be written anytime and anyway. Useable especially for errors*/
  static final int error   =1;

  /** report level to indicate the report should be written if a user is interested on warnings.*/
  static final int warning    =2;

  /** report level to indicate the report should be written if a user is interested on notes of the progression of programm is working.*/
  static final int info    =3;

  /** report level to indicate the report should be written if a user is interested on the order of the events finely.*/
  static final int fineInfo    =4;

  /** report level to indicate the report should be written to detect problems in software.*/
  static final int debug    =5;

  /** report level to indicate all report should be written to detect problems in software with finely and heavyset reports.*/
  static final int fineDebug  =6;

  /** Mask for the in fact reportlevel, other bits may have another meaning.*/
  static final int mReportLevel = 0x7;
  
  /** Mask bit to indicate, do not write to display. This bit is used internally in MainCmd.
   * It is outside the mReportLevel-mask.
   * */
  static final int mNeverOutputToDisplay = 0x8;
  

  
  /**Sends a message. The timestamp of the message is build with the system time. 
   * All other parameter are identically see {@link #sendMsg(int, OS_TimeStamp, String, Object...)}.
   * @param identNumber of the message. If it is negative, it is the same message as positive number,
   *                    but with information 'going state', where the positive number is 'coming state'.
   * @param text The text representation of the message, format string, see java.lang.String.format(..).
   *             @pjava2c=zeroTermString. Java2C: No conversion necessary.
   * @param args 0, 1 or more arguments of any type. 
   *             The interpretation of the arguments is controlled by param text.
   * @return true if the message will be dispatched, false if it is suppressed
   */  
  public boolean sendMsg(int identNumber, CharSequence text, Object... args);

  /**Sends a message.
   * 
   * @param identNumber of the message. If it is negative, it is the same message as positive number,
   *                    but with information 'going state', where the positive number is 'coming state'.
   * @param creationTime absolute time stamp. @Java2C=perValue.
   * @param text The text representation of the message, format string, see java.lang.String.format(..).
   *             @pjava2c=zeroTermString.
   * @param args 0, 1 or more arguments of any type. 
   *             The interpretation of the arguments is controlled by param text.
   * @return true if the message will be dispatched, false if it is suppressed
   */
  public boolean sendMsgTime(int identNumber, OS_TimeStamp creationTime, CharSequence text, Object... args);
  

  
  /**Sends a message. The functionality and the calling parameters are identically 
   * with {@link #sendMsg(int, OS_TimeStamp, String, Object...)}, but the parameter args is varied:  
   * @param identNumber
   * @param creationTime
   * @param text The text of the message: Hint for Java2C: This is a StringJc, not a simple char const*.
   *   That is necessary because the String may be replaced.
   *             @pjava2c=zeroTermString. Java2C: No conversion necessary.
   * @param typeArgs Type chars, ZCBSIJFD for boolean, char, byte, short, int, long, float double. 
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
  public abstract boolean sendMsgVaList(int identNumber, OS_TimeStamp creationTime, CharSequence text, Va_list args);

  
  /** Writes an info line.
  This method should be used instead a directly write via System.out.println(...).
  The using of System.out writes the output directly on console window of the command line, but in general,
  the user mostly don't want to write to the console, he only will give an information out. In a GUI-application,
  this information can be displayed in a status line or scrolling output window.
  It may also be possible to write the information in a file additional.
  The implementation of this method writes the output in the conformed way to the application frame. <br/>
  Before output, the previous line is terminated by a newline, or a status line will be cleared.
  @param sInfo String to be written.
*/
public void writeInfoln(CharSequence sInfo);

/** Appends an info to the end of the previous info, @see #writeInfoln.
  @param sInfo String to be written.
*/
public void writeInfo(CharSequence sInfo);

/** Writes an error line.
  This method should be used instead a directly write via System.err.println(...).
  The using of System.err writes the output directly on console window of the command line, but in general,
  the user mostly don't want to write to the console, he will give an information out. In a GUI-application,
  this information can be displayed in a status line or scrolling output window.
  It may also be possible to write the information in a file additional.
  The implementation of this method writes the output in the conformed way to the application frame. <br/>
  Before output, the previous line is terminated by a newline, or a status line will be cleared.
  @param sError The error text, it should be without such hot spot words line "!!!WARNING!!!",
         because the distinction in display should be done by the implementation of this method.
         A good sample is writeErrorln("file is empty: " + sFileName);
*/
public void writeWarning(CharSequence sError);

/** Writes an error line.
  This method should be used instead a directly write via System.err.println(...).
  The using of System.err writes the output directly on console window of the command line, but in general,
  the user mostly don't want to write to the console, he will give an information out. In a GUI-application,
  this information can be displayed in a status line or scrolling output window.
  It may also be possible to write the information in a file additional.
  The implementation of this method writes the output in the conformed way to the application frame. <br/>
  Before output, the previous line is terminated by a newline, or a status line will be cleared.
  @param sError The error text, it should be without such hot spot words line "!!!ERROR!!!",
         because the distinction in display should be done by the implementation of this method.
         A good sample is writeErrorln("cannot create file: " + sFileName);
*/
public void writeError(CharSequence sError);


/** Writes an error line caused by an exception.
  This method should be used instead a directly write via System.err.println(...) and by catching
  an exception.
  The using of System.err writes the output directly on console window of the command line, but in general,
  the user mostly don't want to write to the console, he will give an information out. In a GUI-application,
  this information can be displayed in a status line or scrolling output window.
  It may also be possible to write the information in a file additional.
  The implementation of this method writes the output in the conformed way to the application frame. <br/>
  Before output, the previous line is terminated by a newline, or a status line will be cleared.
  @param sError The error text, it should be without such hot spot words line "!!!ERROR!!!",
         because the distinction in display should be done by the implementation of this method.
         A good sample is writeErrorln("cannot create file: " + sFileName);
  @param exception The catched Exception. The getMessage()-part of the exception is written after sError.
         The stacktrace of the exception is written to report.      
*/
public void writeError(String CharSequence, Throwable exception);
  


/** report inside a line*/
void report(int nLevel, CharSequence string);
//void report(String string);

/** report begins at a new a line with left margin
  * @param nLevel write the report only if the demand level is greater or equal.
  * @param nLeftMargin determins a left margin. First a new line is outputted, followed by '*' and spaces.
  * @param string String to write.
*/
void reportln(int nLevel, int nLeftMargin, CharSequence string);

/** report begins at a new a line
  * @param nLevel write the report only if the demand level is greater or equal.
  * @param string String to write.
*/
void reportln(int nLevel, CharSequence string);

/** report an error*/
//void reportError(String string);
/** report a waring*/
//void reportWarning(String string);

/** report of a excpetion (in a new line)*/
void report(CharSequence startText, Throwable exception);

/** access to the level of report. With the knowledge of the maximal reportlevel
 * the user can decide on some actions in context of report.
 * @return The report level, defined by user invoking.
 * */
int getReportLevel();


/**Writes the content in the physical medium.
 * The implementation of flush is in the best way as possible. It depends on the possibilities
 * of the output medium.
 */
void flushReport();

/**Sets a dedicated level number to the known output priorities.
 * This method helps to define several levels to dispatch it.
 * @param nLevel The number identifying a dedicated level. This number should be greater than
 *        the known priority levels, it means >= 10 or >=1000. 
 *        Use dedicated group of numbers for an application. 
 * @param nLevelActive Ones of the known priotity levels {@link Report.error} to {@link Report.fineDebug}.
 * <br>
 * Example of using:
 * <pre>
 *   class MyModule
 *   { /**Define module-specific numbers to identify a level. 
 *      * The numbers should be define regarding a band of numbers in the application.
 *      * /  
 *     static final int myReportLevel1 = 3500, myReportLevel2=3501; 
 *     
 *     void init()
 *     { setLevelActive(myReportLevel1, Report.info);  //This reports should be outputted always
 *       setLevelActive(myReportLevel2, Report.debug); //This reports are debug infos.
 *     }
 *     
 *     void processAnything()
 *     { report.reportln( myReportLevel1, "InfoText"); //It depends on the report level settings 
 *       report.reportln( myReportLevel2, "DebugText");//whether it is outputed or not. 
 *     }  
 * </pre>    
 * 
 */
void setReportLevelToIdent(int ident, int nLevelActive);



/**Set another level inside programming. It is advisable to restore the old level
 * after the designated operation.
 * @param newLevel The level to be set, use one of the defines {@link LogMessage#info} to {@link LogMessage#fineDebug}
 * @return the current level, usefull to restore it.
 */
int setReportLevel(int level);



/**gets the associated report level to a report identifier.
 * @param ident The identifier.
 * @return the level.
 */
int getReportLevelFromIdent(int ident);





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
  
  final public static SimpleDateFormat dateFormat = new SimpleDateFormat("MMM-dd HH:mm:ss.SSS: ");
  
  final public static SimpleDateFormat minSecondsFormat = new SimpleDateFormat("mm:ss.SSS: ");
  
  /**This is a simple static output operation independent of the log system.
   * @param ms Milliseconds after 1970
   * @param msg
   * @return 
   */
  public static String timeMsg(long ms, String msg) { return dateFormat.format(new Date(ms)) + ": " + msg; } 
  
  /**It prepares a message part with given mm:ss.SSS (till millisec accuracy) with a given absolute time. 
   * @param msg Text before, shold have the form "time=" or "at " or such.
   * @param ms absolute time for the millisec
   * @return It shows msg 13:45.124
   */
  public static String msgSec(String msg, long ms) { return msg + minSecondsFormat.format(new Date(ms)); } 
  
  /**It prepares a message with the current time and a String after it.
   * @param msg any text.
   * @return String to log
   */
  public static String timeCurr(String msg) { return dateFormat.format(System.currentTimeMillis()) + msg; }
}
