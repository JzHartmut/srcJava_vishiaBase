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
  public void writeInfoln(String sInfo);

  /** Appends an info to the end of the previous info, @see #writeInfoln.
      @param sInfo String to be written.
  */
  public void writeInfo(String sInfo);

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
  public void writeWarning(String sError);

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
  public void writeError(String sError);


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
  public void writeError(String sError, Throwable exception);

  
  void writeStackTrace(Exception exc);
  

  public void openReportfile(String sFileReport, boolean bAppendReport) 
  throws FileNotFoundException;


  /** report inside a line*/
  void report(int nLevel, String string);
  //void report(String string);

  /** report begins at a new a line with left margin
    * @param nLevel write the report only if the demand level is greater or equal.
    * @param nLeftMargin determins a left margin. First a new line is outputted, followed by '*' and spaces.
    * @param string String to write.
  */
  void reportln(int nLevel, int nLeftMargin, String string);

  /** report begins at a new a line
    * @param nLevel write the report only if the demand level is greater or equal.
    * @param string String to write.
  */
  void reportln(int nLevel, String string);

  /** report an error*/
  //void reportError(String string);
  /** report a waring*/
  //void reportWarning(String string);
  
  /** report of a excpetion (in a new line)*/
  void report(String sText, Throwable exception);
  
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
  
  
  /**gets the associated report level to a report identifier.
   * @param ident The identifier.
   * @return the level.
   */
  int getReportLevelFromIdent(int ident);
  
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
