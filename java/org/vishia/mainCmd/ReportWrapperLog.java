package org.vishia.mainCmd;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.vishia.bridgeC.OS_TimeStamp;
import org.vishia.bridgeC.Va_list;
import org.vishia.msgDispatch.LogMessage;

/**This class adapts a Report output to the LogMessage or Message Dispatcher.
 * @author Hartmut Schorrig
 *
 */
public class ReportWrapperLog implements Report
{

  final LogMessage log;
  
  
  
  public ReportWrapperLog(LogMessage log)
  { this.log = log;
  }

  @Override
  public void flushReport()
  {
    // TODO Auto-generated method stub
    
  }

  @Override
  public int getExitErrorLevel()
  {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public LogMessage getLogMessageOutputConsole()
  {
    return log;
  }

  @Override
  public LogMessage getLogMessageErrorConsole()
  {
    return log;
  }

  @Override
  public LogMessage getLogMessageOutputFile()
  {
    return log;
  }

  @Override
  public int getReportLevel()
  {
    return 0;
  }

  @Override
  public int getReportLevelFromIdent(int ident)
  {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public void openReportfile(String sFileReport, boolean bAppendReport)
    throws FileNotFoundException
  {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void report(int nLevel, String string)
  {
    log.sendMsg(nLevel, string);
    
  }

  @Override
  public void report(String sText, Throwable exception)
  {
    log.sendMsg(0, sText + exception.getMessage());
    
  }

  @Override
  public void reportln(int nLevel, int nLeftMargin, String string)
  {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void reportln(int nLevel, String string)
  {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void setExitErrorLevel(int level)
  {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void setReportLevelToIdent(int ident, int nLevelActive)
  {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void writeError(String sError)
  {
    log.sendMsg(0, sError);
    
  }

  @Override
  public void writeError(String sError, Throwable exception)
  {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void writeInfo(String sInfo)
  {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void writeInfoln(String sInfo)
  {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void writeWarning(String sError)
  {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void writeStackTrace(Exception exc)
  {
    // TODO Auto-generated method stub
    
  }
  
  
  @Override public void setOutputChannels(Appendable outP, Appendable errP)
  {
  }

  
  
  
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
  public boolean sendMsg(int identNumber, String text, Object... args)
  { return log.sendMsg(identNumber, text, args);
  }

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
  public boolean sendMsgTime(int identNumber, OS_TimeStamp creationTime, String text, Object... args)
  { return log.sendMsgTime(identNumber, creationTime, text, args);
  }
  

  
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
  public boolean sendMsgVaList(int identNumber, OS_TimeStamp creationTime, String text, Va_list args){
    return log.sendMsgVaList(identNumber, creationTime, text, args);
  }

  @Override public Appendable append(CharSequence csq) throws IOException {
    sendMsg(0, csq.toString());
    return this;
  }

  @Override public Appendable append(CharSequence csq, int start, int end) throws IOException {
    sendMsg(0, csq.subSequence(start, end).toString());
    return this;
  }

  @Override public Appendable append(char c) throws IOException {
    String s = "" + c;
    sendMsg(0, s);
    return this;
  }

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
  public void close(){
    log.close();
  }
  
  /**A call of this method causes an activating of transmission of all messages since last flush. 
   * It is abstract. It depends from the kind of device, what <code>flush</code> mean. 
   * If the device is a log file writer it should be clearly.
   * <code>flush</code> may mean, the processing of messages is ready to transmit yet. 
   */
  public void flush(){
    log.flush();
  }
  
  /**Checks whether the message output is available. */
  public boolean isOnline(){
    return log.isOnline();
  }


}
