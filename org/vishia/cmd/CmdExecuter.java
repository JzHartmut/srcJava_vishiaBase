package org.vishia.cmd;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;

import org.vishia.mainCmd.MainCmd;
import org.vishia.util.StringPart;

/**This class organizes the execution of commands with thread-parallel getting of the process outputs.
 * @author Hartmut Schorrig
 *
 */
public class CmdExecuter
{
  /**Version and History:
   * <ul>
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
   * <li>TODO handling input pipe.
   * <li>TODO non waiting process.  
   * <li>older: TODO
   * </ul>
   */
  public static final int version = 0x20111002;

  
  /**Composite instance of the java.lang.ProcessBuilder. */
  private final ProcessBuilder processBuilder;

  /**The running process or null. Note that this class supports only running of one process at one time. */
  private Process process;
  
  /**True for ever so long this class is used, it maybe so long this application runs. */
  boolean bRunThreads;
  
  /**Both thread instances runs for ever so long bRunThreads is true.
   * They handle the output and error output if a process runs, and waits elsewhere. */
  private final OutThread outThread = new OutThread(), errThread = new OutThread();
  
  
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
    threadExecOut = new Thread(outThread, "execOut");
    threadExecError = new Thread(errThread, "execError");
    threadExecIn = null; //TODO new Thread(inputThread, "execIn");
    bRunThreads = true;
    threadExecOut.start();
    threadExecError.start();
    //threadExecIn.start();
  }
  
  /**Sets the current directory for the next execution.
   * @param dir any directory in the users file system.
   */
  public void setCurrentDir(File dir)
  {
    processBuilder.directory(dir);
  }
  
  /**Executes a command with arguments and waits for its finishing.
   * @param cmdLine The command and its arguments in one line. 
   *        To separate the command and its argument the method {@link #splitArgs(String)} is used.
   * @param input The input stream of the command. TODO not used yet.
   * @param output Will be filled with the output of the command.
   * @param error Will be filled with the error output of the command. 
   *        Maybe null, then the error output will be written to output 
   */
  public int execute(String cmdLine
  , String input
  , Appendable output
  , Appendable error
  )
  { String[] cmdArgs = splitArgs(cmdLine);
    return execute(cmdArgs, input, output, error);
  }
  
  
  
  /**Executes a command with arguments and maybe waits for its finishing.
   * @param cmdArgs The command and its arguments. The command is cmdArgs[0]. 
   *        Any argument have to be given with one element of this String array.
   * @param input The input stream of the command. TODO not used yet.
   * @param output Will be filled with the output of the command.
   *        If output ==null then no output is expected and the end of command execution is not await.
   *        But in this case error should not be ==null because errors of command invocation are written there.
   * @param error Will be filled with the error output of the command. 
   *        Maybe null, then the error output will be written to output 
   */
  public int execute(String[] cmdArgs
  , String input
  , Appendable output
  , Appendable error
  )
  { int exitCode;
    processBuilder.command(cmdArgs);
    if(error == null){ error = output; }
    //userError = error;
    //userOutput = output;
    try
    {
      process = processBuilder.start();
      if(error !=null){
        //bRunExec = true;
        errThread.processOut = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        errThread.out = error;
        synchronized(errThread){ errThread.notify(); }  //wake up to work!
        //processIn = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
      }
      if(output !=null){
        //bRunExec = true;
        outThread.processOut = new BufferedReader(new InputStreamReader(process.getInputStream()));
        outThread.out = output;
        synchronized(outThread){ outThread.notify(); }  //wake up to work!
        //processIn = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
        exitCode = process.waitFor();  //wait for finishing the process
        synchronized(outThread){
          if(outThread.processOut !=null){ //will be set to null on end of file detection.
            outThread.wait();   //wait for finishing getting output. It will be notified if end of file is detected
          }
        }
        synchronized(errThread){
          if(errThread.processOut !=null){ //may be null if err isn't used, will be set to null on end of file detection
            errThread.wait();   //wait for finishing getting error output. It will be notified if end of file is detected 
          }
        }
      } else {
        exitCode = 0; //don't wait
      }
      synchronized(this){
        process = null;  //no more used
      }
    } catch(Exception exception)
    { if(error !=null){
        try{ error.append( "Problem: ").append(exception.getMessage());}
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
      if(process !=null){
        process.destroy();
        destroyed = true;
      }
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
  
  
  
  /**Splits command line arguments.
   * The arguments can be separated with one or more as one spaces (typical for command lines)
   * or with white spaces. If commands are quoted with "" it are taken as one unit.
   * The line can be a text with more as one line, for example one line per argument.
   * If a "##" is contained, the text until the end of line is ignored.
   * @param line The line or more as one line with arguments
   * @return All arguments written in one String per element.
   */
  public static String[] splitArgs(String line)
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
          length -= 2;  
        }
      } else { //non quoted:
        posArgs[++ixArg] = posArg;
        spLine.lentoAnyChar(" \t\n\r");
        spLine.len0end();
        length = spLine.length();
      }
      posArgs[++ixArg] = posArg + length;
      spLine.fromEnd();
    }
    String[] ret = new String[(ixArg+1)/2];
    ixArg = -1;
    for(int ixRet = 0; ixRet < ret.length; ++ixRet){
      ret[ixRet] = line.substring(posArgs[++ixArg], posArgs[++ixArg]);
    }
    return ret;
  }
  
  
  void stop(){};
  
  
  
  class OutThread implements Runnable
  {
    /**Output or ErrorOutput from process. */
    BufferedReader processOut;
    
    /**Output to write.*/
    Appendable out;
    
    char state = '.';
    
    @Override public void run()
    { state = 'r';
      while(bRunThreads){
        if(processOut !=null && out !=null){  //ask only if processOutput is Set.
          String sLine;
          try{
            if( (sLine= processOut.readLine()) !=null){
              out.append(sLine).append('\n');
            } else {
              //Because processOut returns null, it is "end of file" for the output stream of the started process.
              //It means, the process is terminated now.
              processOut = null;  //Set to null because it will not be used up to now. Garbage.
              if(process !=null){
                synchronized(this){ notify(); }  //notify it!  
              }
            }
          } catch(IOException exc){
            
          }
        } else {
          //no process is active, wait
          try { synchronized(this){ wait(1000); } } catch (InterruptedException exc) { }
        }
        
      }
      state = 'x';
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
    { while(bRunThreads){
        if(bRunExec){
          String sLine;
          boolean bFinished = true;
          try{
            if( (processIn.append("")) !=null){
              bFinished = false;
            }
          } catch(IOException exc){
            stop();
          }
          if(bFinished){
            bRunExec = false;
          }
          try { Thread.sleep(50); } catch (InterruptedException exc) { }
          process.destroy();
        } else {
          try { Thread.sleep(100); } catch (InterruptedException exc) { }
        }
        
      }
    }
  }
  
  
  /**Closes the functionality, finished the threads.
   * 
   */
  public void close()
  {
    bRunThreads = false;
    while(  outThread !=null && outThread.state == 'r'
         || errThread !=null && errThread.state == 'r'
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
    bRunThreads = false;
  }
  
  
  /**A test main programm only for class test espesically in debug mode.
   * @param args not used.
   */
  public final static void main(String[] args)
  {
    CmdExecuter main = new CmdExecuter();
    main.execute("cmd /C", null, null, null);
    main.finalize();
  }
  

}
