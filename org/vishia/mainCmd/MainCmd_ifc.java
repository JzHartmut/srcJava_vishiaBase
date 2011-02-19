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


/** The interface MainCmd_Ifc is an interface for all java programms to do something
    in respect to command line things.
    Specifically the writing of messages is supported. The implemented class effectuate the
    writing at example to the System.out or to a window in GUI-applications.<br/>
    The interface Report is extended here, so all report things are also accessable via this interface.
<hr/>
<pre>
date       who      change
2007-12-29 JcHartmut displace some methods to org.vishia.mainCmd.Report.
2007-03-07 JcHartmut Method getReportFileName
2006-01-07 JcHartmut some corrections
2004-06-00 JcHartmut initial revision
*
</pre>
<hr/>
*/
public interface MainCmd_ifc extends Report
{



  /**Returns the name of the report file to write out as info.
   * 
   * @return name of Reportfile.
   */
  public String getReportFileName();




  /** Execute a command, invoke a cmdline call.<a name=executeCmdLine>
      The call must not be needed any input ::TODO:: see input. The output is written with a separate thread either to writeInfoln()
      or to report.
      @param cmd String with the separeted parts of the command. cmd[0] is the command to invoke.
      @param nReportLevel Determines the kind of writing the output. Possible values are 1..6 using Report.error to Report.fineInfo
                          and -1..-3 using -Report.error to -report.info (with a negativ sign!). On negativ values the
                          report is written to display with the write..()-methods of this interface, with the
                          positive value, the report is written to Report.report..()-Methods.<br/>
                          The error output is always written to the reportfile with Report.error or with writeError().<br/>
                          Example: -3 detemines, that the normal output is written with writeInfoln() and the error output
                          is written with writeError(), 3 detemines that the normal output is written with reportln(3,...)
                          and the error output is written with Report(1, ...).
      @param output    If not null, than the stdout is written in this Buffer. If null, the stdout is written to report.
      @param input     If not null, this input is send to the cmdline process. If null, no input is send.
      @return exitErrorLevel from the command line process.
  */
  public int executeCmdLine(String[] cmd, int nReportLevel, StringBuffer output, String input);

  /** Execute a command, invoke a cmdline call.
      The call must not be needed any input. The output is written with a separate thread either to writeInfoln()
      or to report.
      @param cmdLine String represents the command with all arguments. The arguments must be separated by exactly one space.
      The rest of paramter and return see <a href="#executeCmdLine(java.lang.String[], int, java.lang.StringBuffer, java.lang.String)">executeCmdLine(String[], ...)</a>.
  */
  public int executeCmdLine(String cmdLine, int nReportLevel, StringBuffer output, String input);
  
  
  
	/**Executes a command line call maybe as pipe, waiting for finishing..
	 * The output is written with a separate thread, using the internal (private) class ShowCmdOutput.
	 * @param processBuilder The ProcessBuilder. There may be assigned environment variables and a current directory.
	 * @param cmd The cmd and arguments. If it is null, the command assigened to the processBuilder is used.
	 * @param input Any pipe-input. It may be null.
	 * @param nReportLevel The report level which is used for output. 
	 *        If it is 0, then the output isn't written TODO
	 * @param output The output pipe.
	 * @param error The error pipe. If it is null, then errors are written in the output pipe.
	 * @return
	 */
	public int executeCmdLine
	( ProcessBuilder processBuilder
  , String cmd
  , String input
	, int nReportLevel
	, StringBuffer output
	, StringBuffer error
	);
	
	/**Executes a command line call maybe as pipe, waiting for finishing..
	 * The output is written with a separate thread, using the internal (private) class ShowCmdOutput.
	 * @param processBuilder The ProcessBuilder. There may be assigned environment variables and a current directory.
	 * @param cmd The cmd and arguments. If it is null, the command assigend to the processBuilder is used.
	 * @param input Any pipe-input. It may be null.
	 * @param nReportLevel The report level which is used for output. 
	 *        If it is 0, then the output isn't written TODO
	 * @param output The output pipe.
	 * @param error The error pipe. If it is null, then errors are written in the output pipe.
	 * @return
	 */
	public int executeCmdLine
	( ProcessBuilder processBuilder
  , String[] cmd
  , String input
	, int nReportLevel
	, StringBuffer output
	, StringBuffer error
	);
  
  
	/**Starts a command invocation for a independent window.
	 * This command does not have any input or output. The command will be started,
	 * the finishing isn't await. This command line invocation is proper for commands,
	 * which create a new window in the operation system. The new window has its own live cycle then,
	 * independent of the invocation.
	 * @param cmd The command. Some arguments are possible, they should be separated by space.
	 * @param processBuilder The processBuilder.
	 * @return
	 */
	public int startCmdLine(ProcessBuilder processBuilder, String cmd);
	
	/**Starts a command invocation for a independent window.
	 * This command does not have any input or output. The command will be started,
	 * the finishing isn't await. This command line invocation is proper for commands,
	 * which create a new window in the operation system. The new window has its own live cycle then,
	 * independent of the invocation.
	 * @param cmd The command and some arguments.
	 * @param processBuilder The processBuilder.
	 * @return
	 */
	public int startCmdLine(ProcessBuilder processBuilder,  String[] cmd);

	
	/**Searches the window for the already running process 
	 * or starts the process with command invocation for a independent window.
	 * This command does not have any input or output. The command will be started,
	 * the finishing isn't await. This command line invocation is proper for commands,
	 * which create a new window in the operation system. The new window has its own live cycle then,
	 * independent of the invocation.
	 * @param cmd The command and some arguments.
	 * @param processBuilder The processBuilder.
	 * @param sWindowTitle The title or the start of the window if the process is running already.
	 *                     Note: The title is depending from the application.
	 *                     Sometimes the title starts with the associated file, forex calling windows-notepad. 
	 * @return 0 on success, 255 if any start error.
	 */
	int switchToWindowOrStartCmdline(ProcessBuilder processBuilder, String sCmd, String sWindowTitle);
	
  
}
