/****************************************************************************/
/* Copyright/Copyleft:
 *
 * For this source the LGPL Lesser General Public License,
 * published by the Free Software Foundation is valid.
 * It means:
 * 1) You can use this source without any restriction for any desired purpose.
 * 2) You can redistribute copies of this source to everybody.
 * 3) Every user of this source, also the user of redistribute copies
 *    with or without payment, must accept this license for further using.
 * 4) But the LPGL ist not appropriate for a whole software product,
 *    if this source is only a part of them. It means, the user
 *    must publish this part of source,
 *    but don't need to publish the whole source of the own product.
 * 5) You can study and modify (improve) this source
 *    for own using or for redistribution, but you have to license the
 *    modified sources likewise under this LGPL Lesser General Public License.
 *    You mustn't delete this Copyright/Copyleft inscription in this source file.
 *
 * @author JcHartmut = hartmut.schorrig@vishia.de
 * @version 2006-06-15  (year-month-day)
 * list of changes:
 * 2006-05-00: JcHartmut www.vishia.de creation
 * 2008-04-02: JcHartmut some changes
 *
 ****************************************************************************/

package org.vishia.mainCmd;

import java.io.FileNotFoundException;


/** <font color="0x00ffff">Dieses Interface dient zur Ausgabe von Reportmeldungen für kommandozeilenartige Abarbeitung.
    </font>
    This interface is usefull for reporting something (logfiles). It should be used in every algorithm routine
    to support debugging without using an extra debugger. It may help to encircle problems. 
<hr/>
<pre>
date       who       change
2007-12-29 HartmutS  some methods from mainCmd_ifc are displaced here. 
                     Thus the Report interface is revalued to write some informations also to the display
                     with the capability to control the report levels for that in the implementation.
2006-01-07 HartmutS  initial revision
*
</pre>
<hr/>
*/
public interface Report
{
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
  static final int anytime   =1;

  /** older reportlevel
      @deprecated use instead Report.warning or Report.warningDisplay
  */
  static final int interested    =2;

  /** older reportlevel
      @deprecated use instead Report.info or Report.infoDisplay
  */
  static final int eventOrder    =3;

  /** older reportlevel
      @deprecated use instead Report.fineInfo or Report.infoDisplay
  */
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
  public void writeError(String sError, Exception exception);


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
  void report(String sText, Exception exception);
  
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
   *       report.reportln( myReportLevel2, "DebugText");//wether it is outputted or not. 
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
  

}


/** Some static functions to convert strings,
  usefull on C++-conversion
*/


class Stringc
{
  public static String convert(int nVal){ return("" + nVal); }
}

