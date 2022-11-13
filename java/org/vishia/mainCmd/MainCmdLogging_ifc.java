package org.vishia.mainCmd;

import java.io.FileNotFoundException;

import org.vishia.msgDispatch.LogMessage;

/**This interface is the access to output log messages while running an application
 * to check its work.
 * <ul>
 * <li>One implementor is the {@link MainCmd} for command line programming 
 *   or {@link org.vishia.gral.area9.GralArea9MainCmd} for the gral GUI.
 * <li>Another implementor independent of any other concept is {@link MainCmdLoggingStream}. That class
 *   adapts the standard System.out to this interface.
 * <li>Using {@link MainCmdLoggingStream} and {@link org.vishia.msgDispatch.MsgDispatchSystemOutErr}
 *   one can create messages which can be dispatched to any destinations.   
 * </ul>    
 *  <font color="0x00ffff">Dieses Interface dient zur Ausgabe von Logmeldungen for kommandozeilenartige Abarbeitung.
    </font>
    This interface is usefull for reporting something (logfiles). It should be used in every algorithm routine
    to support debugging without using an extra debugger. It may help to encircle problems. 
 * @author Hartmut Schorrig
 *
 */
public interface MainCmdLogging_ifc extends LogMessage
{
  /**Version and history:
   * <ul>
   * <li>2012-11-10 Hartmut chg: Name of this interface changed from Report to MainCmdLogging_ifc: The identifier Report
   *   may be bad understanding, this interface is used as a logging interface. A report is more a summary presentation.
   *   TODO: rename all methods which starts with report.
   * <li>2012-11-10 Hartmut inherits from LogMessage. This interface is used for log, but it contains some more.
   * <li>2012-03-30 Hartmut new {@link #getLogMessageErrorConsole()}
   * <li>2011-10-11 Hartmut new {@link #setOutputChannels(Appendable, Appendable)}. All outputs are redirect-able now.
   *   Used for output in a graphical text box.
   * <li>2007-12-29 Hartmut  some methods from mainCmd_ifc are displaced here. 
   *                 Thus the Report interface is revalued to write some informations also to the display
   *                 with the capability to control the report levels for that in the implementation.
   * <li>2006-01-07 Hartmut  initial revision
   * </ul>  
   */
  static final int version = 0x20111011;
  
  /** exit value to indicate a unconditional abort of a process.*/
  static final int exitUserAbort          = 6;
  /** exit value to indicate a parameter error, that is a programmers error mostly.*/
  static final int exitWithArgumentError = 5;
  /** exit value to indicate a file error, typicall write to a read only file, write to a failed directory, file not exists and others.*/
  static final int exitWithFileProblems   = 4;
  /** exit value to indicate a error in the own process, at example due to failed data*/
  static final int exitWithErrors         = 3;
  /** exit value to indicate some warnings, but no fatal errors. Warnings may be errors in consequence later.*/
  static final int exitWithWarnings       = 2;
  /** exit value to indicate the user should read something and medidate about.*/
  static final int exitWithNotes          = 1;
  /** exit value to indicate not at all problems.*/
  static final int exitSuccessfull        = 0;

  /** older reportlevel
      @deprecated use instead Report.error or Report.errorDisplay
  */
  @Deprecated
  static final int anytime   =1;

  /** older reportlevel
      @deprecated use instead Report.warning or Report.warningDisplay
  */
  @Deprecated
  static final int interested    =2;

  /** older reportlevel
      @deprecated use instead Report.info or Report.infoDisplay
  */
  @Deprecated
  static final int eventOrder    =3;

  /** older reportlevel
      @deprecated use instead Report.fineInfo or Report.infoDisplay
  */
  @Deprecated
  static final int fineEventOrder    =3;



  
  void writeStackTrace(Exception exc);
  

  public void openReportfile(String sFileReport, boolean bAppendReport) 
  throws FileNotFoundException;


  /*----------------------------------------------------------------------------------------------------------*/
  /** set the exitErrorLevel of the maximum of given level of every call.
      @param level Errorlevel how defined in Report, 0 is the lowest level (successfull), >0 is worse.
  */
  void setExitErrorLevel(int level);

  /** get the errorLevel setted with setExitErrorLevel().
  */
  public int getExitErrorLevel();
  

  /**Sets destinations for output and error output.
   * This method may allow to redirect output and error. 
   * @param outP  Destination for output. If null, current output isn't change.
   * @param errP Destination for error output. If null, current output isn't change.
   */
  void setOutputChannels(Appendable outP, Appendable errP);
  

}
