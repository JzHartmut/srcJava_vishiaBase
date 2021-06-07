package org.vishia.cmd;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.script.ScriptException;

import org.vishia.mainCmd.MainCmdLogging_ifc;
import org.vishia.util.DataAccess;
import org.vishia.util.Debugutil;
import org.vishia.util.StringPart;

/**This class organizes the execution of commands with thread-parallel getting of the process outputs.
 * It supports both command line invocations of the operation system (via {@link ProcessBuilder})
 * and call of {@link JZtxtcmdScript} sub routines (which may call internally command lines).
 * It is especially proper for graphic applications: 
 * The execution of complex callbacks on graphical buttons should not be done in the graphic thread!
 *  
 * @author Hartmut Schorrig
 *
 */
public class CmdExecuter implements Closeable
{
  /**Version, License and History:
   * <ul>
   * <li>2020-03-14 Hartmut new: {@link #addCmd(org.vishia.cmd.JZtxtcmdScript.Subroutine, List, Appendable, File, ExecuteAfterFinish)}.  
   * <li>2017-10-05 Hartmut new: {@link #setCharsetForOutput(String)}: git outputs its status etc. in UTF-8.  
   * <li>2017-01-01 Hartmut new: now alternatively to the command process the {@link JZtxtcmdExecuter#execSub(org.vishia.cmd.JZtxtcmdScript.Subroutine, List, boolean, Appendable, File)}
   *   can be invoked by a #add  The JZcmdExecuter may invoke an command process and uses this CmdExecuter too in the main thread. Therefore it is proper
   *   to join both invocation variants. The old class org.vishia.cmd.CmdQueue is obsolete then. 
   * <li>2016-09-18 Hartmut new: {@link #addCmd(String[], String, List, List, File, ExecuteAfterFinish)} and {@link #executeCmdQueue(boolean)}.
   *   That is a new feature to store simple system commands. The class {@link CmdQueue} may be over-engineered for some applications. 
   * <li>2016-09-17 Hartmut new: {@link #execute(String[], boolean, String, List, List, ExecuteAfterFinish)} have the argument {@link ExecuteAfterFinish}.
   *   This method is executed in the {@link #outThread} after finishing the command and can evaluate the output of the command with a Java routine.
   *   The interface {@link ExecuteAfterFinish} can be overridden in any application for that appoach.   
   * <li>2016-08-26 Hartmut new: {@link #splitArgs(String, String[], String[])} with pre- and post-arguments. 
   *   Therewith both the invocation of a batch in Windows with "cmd.exe" can be organized as a Linux cmd with "sh.exe" can be invoked.
   *   The possibility {@link #execute(String, String, Appendable, Appendable, boolean)} with "useShell=true" is not the favor
   *   because it is only one of the necessities. Nevertheless it works now with initial setting of {@link #sConsoleInvocation}. 
   * <li>2015-07-18 Hartmut chg: Now on {@link #execute(String[], String, Appendable, Appendable)} etc. the error can be the same
   *   as the output. It is equate to error=null, only one output instance is used.
   * <li>2013-07-12 Hartmut new: {@link #execute(String[], boolean, String, List, List)} now with donotwait
   * <li>2013-07-12 Hartmut new: {@link #execute(String[], String, List, List)} for more as one output or error.
   *   It is nice to write a process output similar to the System.out and to a internal buffer while the process runs.
   *   Catching in a buffer and write to System.out after the process is finished is not fine if the process needs some time or hangs.
   * <li>2012-11-19 Hartmut bug: Command line arguments in "": now regarded in {@link #splitArgs(String)}.
   * <li>2012-02-02 Hartmut bug: Calling {@link #abortCmd()}: There was a situation were in {@link OutThread#run()} readline() hangs,
   *   though the {@link #process} was destroyed. It isn't solved yet. Test whether it may be better
   *   to read the InputStream direct without wrapping with an BufferedReader. 
   * <li>2011-12-31 Hartmut bugfix: The OutThread doesn't realize that the process was finished,
   *   because BufferedReader.ready() returns false and the buffer was not checked. So 'end of file'
   *   was not detected. Therefore {@link OutThread#bProcessIsRunning} set to false to abort waiting.
   * <li>2011-11-17 Hartmut new {@link #close()} to stop threads.
   * <li>2011-10-09 Hartmut chg: rename 'execWait' to {@link #execute(String, String, Appendable, Appendable)}
   *   because it has the capability of no-wait too.
   * <li>2011-10-09 Hartmut new: {@link #abortCmd()}. experience: On windows the output getting thread may block.   
   * <li>2011-10-08 chg the {@link #execute(String[], String, Appendable, Appendable)} waits until 
   *   all outputs are gotten in the {@link #outThread} and errThread. Extra class {@link OutThread}
   *   for both errThread and outThread.
   * <li>2011-10-02 chg some experiences: It needs parallel threads to capture the output. Extra threads
   *   for out and err, because a read() waits in the out-Buffer and blocks while an error-info is present etc.
   *   The outputs should presented while the process runs, not only if it is finished. It is because
   *   the user should be informed why a process blocks or waits for something etc. This fact is implemented already
   *   in {@link org.vishia.mainCmd.MainCmd#executeCmdLine(String[], ProcessBuilder, int, Appendable, String)}.
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
   */
  public static final String version = "2020-03-14";

  
  /**Composite instance of the java.lang.ProcessBuilder. */
  private final ProcessBuilder processBuilder;

  /**The running process or null. Note that this class supports only running of one process at one time. */
  private Process process;
  
  /**Set with an execute invocation, set to null if it was processed. */
  private ExecuteAfterFinish executeAfterCmd;
  
  /**A maybe special charset for Input and output stream. Hint: git uses UTF-8
   * 
   */
  private Charset charsetCmd = Charset.defaultCharset();
  
  /**True for ever so long this class is used, it maybe so long this application runs. */
  boolean bRunThreads;
  
  /**Both thread instances runs for ever so long bRunThreads is true.
   * They handle the output and error output if a process runs, and waits elsewhere. */
  private final OutThread outThread = new OutThread(true), errThread = new OutThread(false);
  
  
  /**True if a process is started, false if it is finished. */
  //boolean bRunExec;
  
  //boolean bFinishedExec;

  //BufferedReader out1;
  //BufferedReader err1;
  //BufferedWriter processIn;

  //Appendable userOutput;
  //Appendable userError;

  final Thread threadExecOut;
  final Thread threadExecIn;
  final Thread threadExecError;
  
  ConcurrentLinkedQueue<CmdQueueEntry> cmdQueue;
  
  /**For {@link JZtxtcmdScript.Subroutine}. It is created on demand if necessity.
   * 
   */
  private JZtxtcmdExecuter jzcmdExecuter;
  

  
  String[] sConsoleInvocation = {"cmd.exe", "/C"};  //default for window.
  
  /**Constructs the class and starts the threads to getting output and error stream
   * and putting the input stream to a process. This three threads runs anytime unless the class
   * is garbaged or {@link #finalize()} is called manually.
   * <br><br>
   * Note: Starting of threads only if a command is executed results in a longer waiting time
   * for fast simple commands. The input and output putting/getting threads wait in a while-loop
   * until a command is executed. 
   * 
   */
  public CmdExecuter()
  { this.processBuilder = new ProcessBuilder("");
    this.threadExecOut = new Thread(this.outThread, "execOut");
    this.threadExecError = new Thread(this.errThread, "execError");
    this.threadExecIn = null; //TODO new Thread(inputThread, "execIn");
    this.bRunThreads = true;
    this.threadExecOut.start();
    this.threadExecError.start();
    //threadExecIn.start();
  }
  
  
  
  
  public void initJZcmdExecuter(JZtxtcmdScript script, String sCurrdir, MainCmdLogging_ifc log) throws Throwable
  { if(this.jzcmdExecuter == null) {
      this.jzcmdExecuter = new JZtxtcmdExecuter(log);
    }
    this.jzcmdExecuter.initialize(script, false, sCurrdir);
  }
  

  
  /**Sets the current directory for the next execution.
   * @param dir any directory in the users file system.
   */
  public void setCurrentDir(File dir)
  {
    this.processBuilder.directory(dir);
  }
  
  
  public void setCharsetForOutput(String nameCharset)
  {
    this.charsetCmd = Charset.forName(nameCharset);
  }
  
  
  
  /**Returns the environment map for the internal {@link ProcessBuilder}. The returned Map can be modified,
   * its modified content will be used on the next execute(...) invocation. The modification is valid for all following executes.
   * <br>
   * Internal:  {@link ProcessBuilder#environment()} will be called. 
   * 
   * @return the environment of the following executes, able to explore, able to change.
   */
  public Map<String,String> environment(){
    return this.processBuilder.environment();
  }
  
  
  public void specifyConsoleInvocation(String cmd){
    this.sConsoleInvocation = splitArgs(cmd);
  }
  
  public void addCmd(String cmdline
      , String input
      , Appendable output
      , File currentDir
      , ExecuteAfterFinish executeAfterCmd
      ) {
    String[] cmdArgs = splitArgs(cmdline);
    List<Appendable> outputs = new LinkedList<Appendable>();
    outputs.add(output);
    addCmd(cmdArgs, input, outputs, null, currentDir, executeAfterCmd);
  }
  
  
  public void addCmd(String[] cmdArgs
      , String input
      , List<Appendable> outputs
      , List<Appendable> errors
      , File currentDir
      , ExecuteAfterFinish executeAfterCmd
      )
  { if(this.cmdQueue == null) { this.cmdQueue = new ConcurrentLinkedQueue<CmdQueueEntry>(); }
    CmdQueueEntry e = new CmdQueueEntry();
    e.cmd = cmdArgs;
    e.input = input;
    e.out = outputs;
    e.err = errors;
    e.currentDir = currentDir;
    e.executeAfterFinish = executeAfterCmd;
    this.cmdQueue.offer(e);
  }
  
  

  
  /**Same as {@link #addCmd(org.vishia.cmd.JZtxtcmdScript.Subroutine, List, Appendable, File, ExecuteAfterFinish)}
   * but without execFinish
   * @param jzsub
   * @param args
   * @param out
   * @param currDir
   */
  public void addCmd( JZtxtcmdScript.Subroutine jzsub
  , List<DataAccess.Variable<Object>> args
  , Appendable out
  , File currDir
  ) {
    addCmd(jzsub, args, out, currDir, null);
  }
  
  
  
  /**Adds a Subroutine of an jzTc script to execute. 
   * The subroutine can invoke a cmd for a new process internally, this cmdExecuter is used
   * because the given instance of a {@link JZtxtcmdExecuter} knows this.
   * <br>
   * Note: call {@link #initJZcmdExecuter(JZtxtcmdScript, String, MainCmdLogging_ifc)} with that script
   * which contains the sub routine before call this operation.
   * <br><br>
   * Note: The execution of the subroutine is done in maybe another thread which call 
   * {@link #executeCmdQueue(boolean)}. Hint: It can be done for example by the main thread in a graphical application.
   * The execution of complex callbacks on graphical buttons should not be done in the graphic thread!
   * 
   * @param jzsub The dedicated subroutine from the script given with {@link #initJZcmdExecuter(JZtxtcmdScript, String, MainCmdLogging_ifc)}
   * @param args proper arguments
   * @param out The <:>...<.> text output is written into
   * @param currDir current directory for script execution
   * @param execFinish maybe null, this operation is called on finishing the subroutine. 
   *   Maybe the text output on out should be append to an graphical window or such other,
   *   or the resulting files are used. 
   */
  public void addCmd( JZtxtcmdScript.Subroutine jzsub
  , List<DataAccess.Variable<Object>> args
  , Appendable out
  , File currDir
  , ExecuteAfterFinish execFinish
  ) {
    if(this.jzcmdExecuter == null) throw new IllegalArgumentException("The CmdExecuter should be initiaized with initJZcmdExecuter(script,...)");
    if(this.cmdQueue == null) { this.cmdQueue = new ConcurrentLinkedQueue<CmdQueueEntry>(); }
    CmdQueueEntry e = new CmdQueueEntry();
    e.jzsub = jzsub;
    e.args = args;
    e.out1 = out;
    e.out = new LinkedList<Appendable>();
    e.out.add(out);
    e.currentDir = currDir;
    e.executeAfterFinish = execFinish;
    this.cmdQueue.offer(e);
  }
  
  
  
  
  
  
  public void clearCmdQueue(){
    if(this.cmdQueue !=null) { this.cmdQueue.clear(); }
  }
  
  
  /**Executes the commands stored with {@link #addCmd(String[], String, List, List, ExecuteAfterFinish)}.
   * This can be invoked in a specific thread with the following pattern: <pre>
  Thread cmdThread = new Thread("gitGui-Cmd") {
    @Override public void run() {
      do {
        cmd.executeCmdQueue(true);
        try {
          wait(1000);
        } catch (InterruptedException e) { }
      } while (!bCmdThreadClose);
    }
  };
   * </pre>
   * @param abortOnError stops execution if a command does not return 0.
   * @return the last executed entry if an error occurs and 'abortOnError'==true. Elsewhere null
   */
  public CmdQueueEntry executeCmdQueue(boolean abortOnError)
  { CmdQueueEntry e = null;
    while( this.cmdQueue !=null && (e = this.cmdQueue.poll())!=null) {
      if(e.jzsub !=null) {
        try{ this.jzcmdExecuter.execSub(e.jzsub, e.args, true, e.out1, e.currentDir, CmdExecuter.this);
        } catch(ScriptException exc){ 
          String text = "execute JZsub, scriptexception, " + exc.getMessage();
          try{ e.out1.append(text); } catch(IOException exc1){}
        }
        if(e.executeAfterFinish !=null) {
          e.executeAfterFinish.exec(0, e.out1, null);
        }
      } else {
        if(e.currentDir !=null) {
          setCurrentDir(e.currentDir);
        }
        e.errorCmd = execute(e.cmd, false, e.input, e.out, e.err, e.executeAfterFinish);  //invokes executeAfterFinis if given, after command.
        if(e.errorCmd !=0 && abortOnError) {
          break;
        }
      }
    }
    return e;  //it is null if all cmds are processed.
  }
  
  
  
  /**Executes a command with arguments and waits for its finishing.
   * @param cmdLine The command and its arguments in one line. 
   *        To separate the command and its argument the method {@link #splitArgs(String)} is used.
   * @param input The input stream of the command. TODO not used yet.
   * @param output Will be filled with the output of the command.
   * @param error Will be filled with the error output of the command. 
   *        Maybe null or equal output, then the error output will be written to output with the same output thread. 
   */
  public int execute(String cmdLine
  , String input
  , Appendable output
  , Appendable error
  )
  { String[] cmdArgs = splitArgs(cmdLine);
    return execute(cmdArgs, input, output, error);
  }
  
  
  /**Executes a command with arguments and waits for its finishing.
   * This routine is intent to open a shell window. It is not ready yet. On windows it should invoke cmd.exe,
   * on linux maybe console. It should be configured for this instance. see {@link #setConsoleInvocation(String)}.
   * @param cmdLine The command and its arguments in one line. 
   *        To separate the command and its argument the method {@link #splitArgs(String)} is used.
   * @param input The input stream of the command. TODO not used yet.
   * @param output Will be filled with the output of the command.
   * @param error Will be filled with the error output of the command. 
   *        Maybe null or equal output, then the error output will be written to output with the same output thread. 
   */
  public int execute(String cmdLine
  , String input
  , Appendable output
  , Appendable error
  , boolean useShell
  )
  { String[] preArgs = useShell ? this.sConsoleInvocation : null;
    String[] cmdArgs = splitArgs(cmdLine, preArgs, null);
    return execute(cmdArgs, input, output, error);
  }
  

  
  /**Executes a command with arguments and maybe waits for its finishing.
   * @param cmdArgs The command and its arguments. The command is cmdArgs[0]. 
   *        Any argument have to be given with one element of this String array.
   * @param input The input stream of the command. TODO not used yet.
   * @param output Will be filled with the output of the command.
   *        If output ==null then no output is expected and the end of command execution is not awaited.
   *        But in this case error should not be ==null because errors of command invocation are written there.
   * @param error Will be filled with the error output of the command. 
   *        Maybe null or equal output, then the error output will be written to output with the same output thread. 
   * @return exit code if output !=null. If output==null the end of command execution is not awaited.       
   */
  public int execute(String[] cmdArgs
  , String input
  , Appendable output
  , Appendable error
  )
  { boolean donotwait = true;
    List<Appendable> outputs, errors;
    if(output !=null){
      donotwait = false;
      outputs = new LinkedList<Appendable>();
      outputs.add(output);
    } else {
      outputs = null;
    }
    if(error !=null && error != output){ //if the same channel is used, use onle output in a queue.
      donotwait = false;
      errors = new LinkedList<Appendable>();
      errors.add(error);
    } else {
      errors = null;
    }
    return execute(cmdArgs, donotwait, input, outputs, errors, null);
  }
  
  
  /**Same as {@link #execute(String[], String, Appendable, Appendable, boolean)}
   * but do not wait for finish if both outputs and errors are null.
   * @param cmdArgs
   * @param input
   * @param outputs
   * @param errors
   * @return
   */
  public int execute(String[] cmdArgs
      , String input
      , List<Appendable> outputs
      , List<Appendable> errors
      )
  { return execute(cmdArgs, outputs == null && errors == null, input, outputs, errors, null);
  }
  
  /**Executes a command with arguments and maybe waits for its finishing.
   * @param cmdArgs The command and its arguments. The command is cmdArgs[0]. 
   *        Any argument have to be given with one element of this String array.
   * @param donotwait true and all outputs, errors and input == null, then starts the process without wait.
   *   If one of input, outputs and errors is not null, the execution should wait in this thread because
   *   the threads for input, output and error should be running to capture that data. Then this flag is not used.       
   * @param input The input stream of the command. TODO not used yet.
   * @param outputs Will be filled with the output of the command.
   *        If output ==null then no output is expected and the end of command execution is not awaited.
   *        But in this case error should not be ==null because errors of command invocation are written there.
   * @param errors Will be filled with the error output of the command. 
   *        Maybe null, then the error output will be written to output
   * @param executeAfterCmd maybe null, will be executing if given. If the process waits it waits on end of this method.        
   * @return exit code if it is waiting for execution, elsewhere 0.       
   */
  public int execute(String[] cmdArgs
  , boolean donotwait
  , String input
  , List<Appendable> outputs
  , List<Appendable> errors
  , ExecuteAfterFinish executeAfterCmd
  )
  { int exitCode;
    this.executeAfterCmd = executeAfterCmd;
    this.processBuilder.command(cmdArgs);
    if(errors == null){ //merge errors in the output stream. It is a feature of ProcessBuilder.
      this.processBuilder.redirectErrorStream(true); 
    }
    try
    {
      this.process = this.processBuilder.start();       //starts another process on operation system.
      //
      if(errors !=null){                     //it follows immediately: capture the error output from the process
        this.errThread.bProcessIsRunning = true;  //in the error thread.
        this.errThread.processOut = new BufferedReader(new InputStreamReader(this.process.getErrorStream(), this.charsetCmd));
        this.errThread.outs = errors;             
        synchronized(this.errThread){ this.errThread.notify(); }  //wake up to work!
        //processIn = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
      }
      if(outputs !=null){                    //it follows immediately: capture the output from the process
        this.outThread.bProcessIsRunning = true;  //in the output thread.
        this.outThread.processOut = new BufferedReader(new InputStreamReader(this.process.getInputStream(), this.charsetCmd));
        this.outThread.outs = outputs;
        synchronized(this.outThread){ this.outThread.notify(); }  //wake up to work!
      }
      if(input !=null){
        OutputStream sinput = this.process.getOutputStream();
        sinput.write(input.getBytes());
        //Writer processIn = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
      }
      //
      //wait for
      if(input !=null || outputs !=null || errors !=null){
        exitCode = this.process.waitFor();  //wait for finishing the process
        //If the outThread or errThread will be attempt to wait, it realizes that the process has been finished. 
        this.outThread.bProcessIsRunning =false;
        this.errThread.bProcessIsRunning =false;
        //It is possible that the last output isn't gotten because the outThread or errThread
        //is not run in the last time. Run it till it has recognized the end of process itself.
        synchronized(this.outThread){
          if(this.outThread.processOut !=null){ //will be set to null on 'end of file' detection.
            this.outThread.wait();   //wait for finishing getting output. It will be notified if end of file is detected
          }
        }
        synchronized(this.errThread){
          if(this.errThread.processOut !=null){ //may be null if err isn't used, will be set to null on 'end of file' detection
            this.errThread.wait();   //wait for finishing getting error output. It will be notified if end of file is detected 
          }
        }
      } else if(donotwait){
        exitCode = 0; //don't wait
      } else {
        exitCode = this.process.waitFor(); //wait without input, outputs, errors
      }
      synchronized(this){
        this.process = null;  //no more used
      }
    } 
    catch(Exception exception) { 
      if(errors !=null){
        try{
          String sError = "CmdExecuter - Problem;" + exception.getMessage();
          for(Appendable error : errors){
            error.append( sError);
          }
        }
        catch(IOException exc){ throw new RuntimeException(exc); }
      } else {
        throw new RuntimeException(exception);
      }
      exitCode = -1; //Exception
    }
    return exitCode;
  }
  
  
  
  
  /**Aborts the running cmd. 
   * @return true if any cmd is aborted.
   */
  public boolean abortCmd()
  { boolean destroyed = false;
    synchronized(this){
      if(this.process !=null){
        this.process.destroy();
        destroyed = true;
      }
      try{ 
        //problem, if it is in readline(), then deadlock! because readkube and close() are synchronized.
        //how to abort the readline???
        //if(outThread.processOut !=null) { outThread.processOut.close(); } 
        //if(errThread.processOut !=null) { errThread.processOut.close(); } 
      } catch(Exception exc){ 
        System.err.println("error close");
        exc.printStackTrace();
      }
      this.outThread.outs = null;  //to abort
      this.errThread.outs = null;
    }
    //TODO doesn't work:
    /*
    if(outThread.processOut !=null){
      try{ outThread.processOut.close(); }
      catch(IOException exc){
        stop();
      }
    }
    if(errThread.processOut !=null){
      try{ errThread.processOut.close(); }
      catch(IOException exc){
        stop();
      }
    }
    */
    return destroyed;
  }
  

  
  public boolean abortAllCmds()
  {
    if(this.cmdQueue !=null) { this.cmdQueue.clear(); }
    return abortCmd();
  }

  /**Splits command line arguments. See {@link #splitArgs(String, String[], String[])}, without preArgs and postArgs.
   */
  public static String[] splitArgs(String line) { return splitArgs(line, null, null); }

  
  
  /**Splits command line arguments.
   * The arguments can be separated with one or more as one spaces (typical for command lines)
   * or with white spaces. If arguments are quoted with "" it are taken as one argument.
   * The line can be a text with more as one line, for example one line per argument.
   * If a "##" is contained, the text until the end of line is ignored.
   * @param line The line or more as one line with arguments
   * @param preArgs maybe null, some arguments before them from line.
   * @param postArgs maybe null, some arguments after them from line.
   * @return All arguments written in one String per element.
   */
  public static String[] splitArgs(String line, String[] preArgs, String[] postArgs)
  {
    StringPart spLine = new StringPart(line);
    spLine.setIgnoreWhitespaces(true);
    spLine.setIgnoreEndlineComment("##");
    int ixArg = -1;
    int[] posArgs = new int[1000];  //only local, enought size
    int posArg = 0;
    while(spLine.length() >0){
      spLine.seekNoWhitespaceOrComments();
      posArg = (int)spLine.getCurrentPosition();
      int length;
      if(spLine.length() >0 && spLine.getCurrentChar()=='\"'){
        posArgs[++ixArg] = posArg+1;
        spLine.lentoQuotionEnd('\"', Integer.MAX_VALUE);
        length = spLine.length();
        if(length <=2){ //especially 0 if no end quotion found
          spLine.setLengthMax();
          length = spLine.length() - 1;  //without leading "
        } else {
          length -= 1;  //without leading ", the trailing " is not   
        }
      } else { //non quoted:
        posArgs[++ixArg] = posArg;
        spLine.lentoAnyCharOutsideQuotion(" \t\n\r", Integer.MAX_VALUE);
        spLine.len0end();
        length = spLine.length();
      }
      posArgs[++ixArg] = posArg + length;
      spLine.fromEnd();
    }
    spLine.close();
    int nArgs = (ixArg+1)/2;
    if(preArgs !=null) { nArgs += preArgs.length; }
    if(postArgs !=null) { nArgs += postArgs.length; }
    String[] ret = new String[nArgs];
    int ixRet = 0;
    if(preArgs !=null) {
      for(String preArg: preArgs){ ret[ixRet++] = preArg; }
    }
    ixArg = -1;
    while(ixRet < ret.length){
      ret[ixRet++] = line.substring(posArgs[++ixArg], posArgs[++ixArg]);
    }
    if(postArgs !=null) {
      for(String preArg: preArgs){ ret[ixRet++] = preArg; }
    }
    return ret;
  }
  
  
  void stop(){};
  
  
  
  
  public static class CmdQueueEntry
  {
    public String[] cmd;
    /**If given this subroutine should be executed. cmd is not used then. */
    public JZtxtcmdScript.Subroutine jzsub;
    List<DataAccess.Variable<Object>> args;
    public String input;
    Appendable out1;
    public List<Appendable> out;
    public List<Appendable> err;
    public File currentDir;
    public ExecuteAfterFinish executeAfterFinish;
    public int errorCmd;
  }
  
  
  
  
  /**An implementation of this interface can be evaluate the output of the process after finish.
   */
  public interface ExecuteAfterFinish
  {
    /**Gets the Appendable given by the execute invocation. The Appendable may be instances
     *   which allows read out. For example StringBuilder.
     * @param out contains the process output. null if the output was not captured.
     * @param err contains the process error output. null if the output was not captured.
     */
    void exec(int errorcode, Appendable out, Appendable err);
  
  };
  
  
  
  class OutThread implements Runnable
  {
    /**Output or ErrorOutput from process. */
    BufferedReader processOut;
    
    /**Output to write.*/
    List<Appendable> outs;
  
    private final boolean isOutThread;
    
    char state = '.';
    
    /**Set to true before a process is started, set to false if the process has finished. */
    boolean bProcessIsRunning;
    
    
    OutThread(boolean isOutThread){ this.isOutThread = isOutThread; }
    
    @Override public void run()
    { this.state = 'r';
      while(CmdExecuter.this.bRunThreads){
        try{
          if(this.processOut !=null && this.outs !=null){  //ask only if processOutput is Set.
            String sLine;
            if(this.outs != null && this.processOut.ready()){
              if( (sLine= this.processOut.readLine()) !=null){
                for(Appendable out :this.outs){
                  try{
                    out.append(sLine).append("\n");
                  } catch(Exception exc){
                    //do nothing. continue.
                  }
                }
              } else {
                //Because processOut returns null, it is "end of file" for the output stream of the started process.
                //It means, the process is terminated now.
                this.processOut = null;  //Set to null because it will not be used up to now. Garbage.
              }
            } else {
              //yet no output available. The process may be run still, but doesn't produce output.
              //The process may be finished.
              if(!this.bProcessIsRunning){
                if(this.isOutThread && CmdExecuter.this.executeAfterCmd !=null) {
                  try {
                    int exitValue = CmdExecuter.this.process.exitValue();
                    CmdExecuter.this.executeAfterCmd.exec
                    ( exitValue
                    , this.outs == null ? null : this.outs.get(0)
                    , CmdExecuter.this.errThread == null || CmdExecuter.this.errThread.outs ==null 
                        ? null : CmdExecuter.this.errThread.outs.get(0)
                    );
                  } catch (Exception exc) {
                    System.err.println("CmdExecuter - exception in executeAfterCmd");
                  }
                  CmdExecuter.this.executeAfterCmd = null; //is done.
                }
                this.outs = null;  
              } else {
                Thread.sleep(100);
              }
            }
            
          } else {
            //no process is active, wait
            try { synchronized(this){ wait(1000); } } catch (InterruptedException exc) { }
          }
          if(this.outs == null && this.processOut !=null){  //aborted or process is finished.
            //if(process !=null){
              synchronized(this){ 
                this.processOut.close();
                this.processOut = null;
                notify();
              }  //notify it!  
            //}
          }
        } catch(Exception exc){
          Debugutil.stop();
        }
      }
      this.state = 'x';
    }
  }
  
  

  class InThread implements Runnable
  { 
    boolean bRunExec = false;

    /**Output or ErrorOutput from process. */
    BufferedWriter processIn;
    
    /**Output to write.*/
    BufferedReader userInput;    /**Output or ErrorOutput from process. */
    
    
    
    @Override public void run()
    { while(CmdExecuter.this.bRunThreads){
        if(this.bRunExec){
          //String sLine;
          boolean bFinished = true;
          try{
            if( (this.processIn.append("")) !=null){
              bFinished = false;
            }
          } catch(IOException exc){
            stop();
          }
          if(bFinished){
            this.bRunExec = false;
          }
          try { Thread.sleep(50); } catch (InterruptedException exc) { }
          CmdExecuter.this.process.destroy();
        } else {
          try { Thread.sleep(100); } catch (InterruptedException exc) { }
        }
        
      }
    }
  }
  
  
  /**Closes the functionality, finished the threads.
   * 
   */
  @Override public void close()
  {
    this.bRunThreads = false;
    while(  this.outThread !=null && this.outThread.state == 'r'
         || this.errThread !=null && this.errThread.state == 'r'
         //|| inThread !=null && inThread == 'r'
         ){
      synchronized(this){
        try{ wait(100); } catch(InterruptedException exc){}
      }
    }
  }
  
  
  /**Stops the threads to get output.
   * @see java.lang.Object#finalize()
   */
  @Override public void finalize()
  {
    this.bRunThreads = false;
  }
  
  
  /**A test main programm only for class test espesically in debug mode.
   * @param args not used.
   */
  public final static void main(String[] args)
  {
    CmdExecuter main = new CmdExecuter();
    main.execute("cmd /C", null, null, null);
    main.close();
    main.finalize();
  }
  

}
