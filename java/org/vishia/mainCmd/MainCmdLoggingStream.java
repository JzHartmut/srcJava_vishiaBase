package org.vishia.mainCmd;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.vishia.bridgeC.OS_TimeStamp;
import org.vishia.bridgeC.Va_list;
import org.vishia.msgDispatch.LogMessage;
import org.vishia.util.Assert;
import org.vishia.util.Writer_Appendable;

/**This class is a mainCmd logging output with adequate features as the {@link MainCmd} class,
 * but independent of that. It is intent to use if the logging features are other than in the 
 * main routine given, or if {@link MainCmd} is not used for the originally invocation of a Java program.
 * Especially it can be used for logging output inside a {@link org.vishia.cmd.JZtxtcmdExecuter} for some
 * special routines.
 * @author Hartmut Schorrig
 *
 */
public class MainCmdLoggingStream implements MainCmdLogging_ifc
{
  /**Version, history and license.
   * <ul>
   * <li>2013-10-19 Hartmut new: Supports an Appendable now alternatively to the PrintStream as aggregation for output.
   *   More flexibility, able to use for other output channels than System.out.
   * <li>2013-10-19 Hartmut created as bridge between the {@link MainCmdLogging_ifc} and any class which does not have
   *   an association to the {@link MainCmd}.
   * </ul>
   * 
   * <b>Copyright/Copyleft</b>:<br>
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
   *    but doesn't need to publish the whole source of the own product.
   * <li> You can study and modify (improve) this source
   *    for own using or for redistribution, but you have to license the
   *    modified sources likewise under this LGPL Lesser General Public License.
   *    You mustn't delete this Copyright/Copyleft inscription in this source file.
   * </ol>
   * If you intent to use this source without publishing its usage, you can get
   * a second license subscribing a special contract with the author. 
   * 
   * @author Hartmut Schorrig = hartmut.schorrig@vishia.de
   */
  //@SuppressWarnings("hiding")
  public static final String sVersion = "2015-07-19";

  /**Stream to output all logging information. */
  protected OutputStream out;
  
  protected final Appendable app;
  
  private final StringBuilder u = new StringBuilder();
  
  final private SimpleDateFormat dateFormat;
  
  private static String sLeft = "                                                                ";
  
  /**Created on demand. */
  private PrintWriter printWriter; 
  
  /**Created on demand. */
  private PrintStream printStream; 
  
  /**This variable determines which level is output. See {@link MainCmdLogging_ifc#fineDebug} etc.*/
  int reportLevel = MainCmdLogging_ifc.info;
  
  /**Create with given output stream, or empty.
   * @param out may be null, use {@link #openReportfile(String, boolean)} than.
   */
  public MainCmdLoggingStream(OutputStream out){
    this.out = out;
    this.app = null;
    dateFormat = new SimpleDateFormat("MMM-dd HH:mm:ss.SSS: ");
  }
  
  
  /**Create with given output stream, or empty.
   * @param out may be null, use {@link #openReportfile(String, boolean)} than.
   */
  public MainCmdLoggingStream(OutputStream out, int reportLevel){
    this.out = out;
    this.app = null;
    dateFormat = new SimpleDateFormat("MMM-dd HH:mm:ss.SSS: ");
    this.reportLevel = reportLevel;
  }
  
  
  /**Create with given Appendable and dateformat.
   * Note: The sDateFormat is the first parameter to distinquish a System.out between Appendable and PrintStream.
   * @param sDateFormat see simpleDateFormatter, use "MMM-dd HH:mm:ss.SSS: " for example. 
   * @param app may be null, use {@link #openReportfile(String, boolean)} than.
   */
  public MainCmdLoggingStream(String sDateFormat, Appendable app){
    dateFormat = new SimpleDateFormat(sDateFormat);
    this.out = null;
    this.app = app;
  }
  
  
  /**Create with given output stream, or empty.
   * Note: The sDateFormat is the first parameter to distinquish a System.out between Appendable and PrintStream.
   * @param sDateFormat see simpleDateFormatter, use "MMM-dd HH:mm:ss.SSS: " for example. 
   * @param app may be null, use {@link #openReportfile(String, boolean)} than.
   */
  public MainCmdLoggingStream(String sDateFormat, Appendable app, int reportLevel){
    dateFormat = new SimpleDateFormat(sDateFormat);
    this.out = null;
    this.app = app;
    this.reportLevel = reportLevel;
  }
  
  
  @Override
  public void flushReport()
  { flush();
  }

  /**Returns 0
   * @see org.vishia.mainCmd.MainCmdLogging_ifc#getExitErrorLevel()
   */
  @Override
  public int getExitErrorLevel()
  { return 0;
  }

  @Override
  public int getReportLevel()
  { return reportLevel;
  }

  @Override
  public int getReportLevelFromIdent(int ident)
  {
    return ident <= MainCmdLogging_ifc.fineDebug ? ident : MainCmdLogging_ifc.info;
  }

  @Override
  public void openReportfile(String sFileReport, boolean bAppendReport)
      throws FileNotFoundException
  {
    out = new FileOutputStream(sFileReport);
  }

  @Override
  public void report(int nLevel, CharSequence string)
  { if( (nLevel & mReportLevel) <= reportLevel)
    { u.append(string);
    }
     
  }

  
  void writeBuffer(){
    try{
      if(out != null){ out.write(u.toString().getBytes()); }
      else if(app != null){ app.append(u); }
    }
    catch(IOException exc){
      System.err.println("MainCmdLoggingStream - IOException;");
    }
    u.setLength(0);
  }
  
  
  void writeln(int level, int nLeftMargin, CharSequence ss){
    if(u.length() >0){
      for(int i = 0; i < u.length(); ++i){
        if(u.charAt(i) < 0x20){
          u.setCharAt(i, '|');
        }
      }
      u.append("\n"); 
      writeBuffer(); 
    }
    u.append(dateFormat.format(new Date(System.currentTimeMillis())));
    u.append(level).append(": ").append(sLeft.subSequence(0, nLeftMargin)).append(ss);
  }
  
  
  @Override
  public void report(CharSequence sText, Throwable exception)
  { CharSequence ctext = Assert.exceptionInfo(sText, exception, 0, 20);
    writeln(MainCmdLogging_ifc.error, 0, ctext);
  }

  @Override
  public void reportln(int nLevel, int nLeftMargin, CharSequence string)
  {
    if( (nLevel & mReportLevel) <= reportLevel)
    { //writes also an error as info on display.
      writeln(nLevel, nLeftMargin, string);
    }  
  }

  @Override
  public void reportln(int nLevel, CharSequence string)
  { reportln(nLevel, 0 , string);
  }

  @Override
  public void setExitErrorLevel(int level)
  { 
  }

  @Override
  public void setOutputChannels(Appendable outP, Appendable errP)
  {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void setReportLevelToIdent(int ident, int nLevelActive)
  {
    // TODO Auto-generated method stub
    
  }

  
  
  @Override public int setReportLevel(int newLevel) {
    return LogMessage.fineDebug;
  }

  
  
  @Override
  public void writeError(CharSequence sError)
  { reportln(MainCmdLogging_ifc.error, sError); 
  }

  @Override
  public void writeError(String sError, Throwable exception)
  { report(sError, exception); 
  }

  @Override
  public void writeInfo(CharSequence sInfo)
  { report(MainCmdLogging_ifc.info, sInfo); 
  }

  @Override
  public void writeInfoln(CharSequence sInfo)
  { reportln(MainCmdLogging_ifc.info, sInfo); 
  }

  @Override
  public void writeStackTrace(Exception exc)
  { if(out !=null){
      if(printStream == null){ printStream = new PrintStream(out, true); }
      exc.printStackTrace(printStream);
    } else if (app !=null) {
      if(printWriter == null){ printWriter = new PrintWriter(new Writer_Appendable(app), true); }
      exc.printStackTrace(printWriter);
    }
  }

  @Override
  public void writeWarning(CharSequence sError)
  { reportln(MainCmdLogging_ifc.warning, sError); 
  }

  @Override
  public void close()
  {
    if(u.length() >0){ u.append("\n"); writeBuffer(); }
  }

  @Override
  public void flush()
  { if(u.length() >0){ writeBuffer(); }
    try{ 
      if(out !=null) { out.flush(); }
    } catch(IOException exc){}
  }
  
  @Override
  public boolean isOnline()
  { return out !=null || app !=null;
  }

  @Override
  public boolean sendMsg(int identNumber, String text, Object... args)
  { return sendMsgTime(identNumber, new OS_TimeStamp(System.currentTimeMillis()), text, args);
  }

  @Override
  public boolean sendMsgTime(int identNumber, OS_TimeStamp creationTime, String text, Object... args)
  {
    String sline = dateFormat.format(creationTime) + "; " + identNumber + "; " + String.format(text,args);
    try{ 
      if(out !=null) {
        out.write(sline.getBytes());
        out.write('\n');
      } else if(app !=null) {
        app.append(sline).append('\n');
      }
    }
    catch(IOException exc){ }
    return true;
  }

  @Override
  public boolean sendMsgVaList(int identNumber, OS_TimeStamp creationTime, String text, Va_list args)
  { String line = "?";
    try{
      line = dateFormat.format(creationTime) + "; " + identNumber + "; " + String.format(text,args.get());
    } catch(Exception exc){
      line = dateFormat.format(creationTime) + "; " + identNumber + "; " + text;
    }
      try{ 
      if(out !=null) {
        out.write(line.getBytes());
        out.write('\n');
      } else if(app !=null) {
        app.append(line).append('\n');
      }
    }
    catch(Exception exc){ 
    }
    return true;
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


}
  
