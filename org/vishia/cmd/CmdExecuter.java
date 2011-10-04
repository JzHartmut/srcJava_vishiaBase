package org.vishia.cmd;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import org.vishia.mainCmd.MainCmd;
import org.vishia.util.StringPart;

public class CmdExecuter
{
  /**Version and History:
   * <ul>
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

  
  private final ProcessBuilder processBuilder;

  private Process process;
  
  /**True for ever so long the application should run. */
  boolean bRunThreads;
  
  /**True if a process is started, false if it is finished. */
  boolean bRunExec;
  
  boolean bFinishedExec;

  BufferedReader out1;
  BufferedReader err1;
  BufferedWriter processIn;

  Appendable userOutput;
  Appendable userError;

  Thread threadExecOut;
  Thread threadExecIn;
  Thread threadExecError;
  
  public CmdExecuter()
  { this.processBuilder = new ProcessBuilder("");
    threadExecOut = new Thread(inOutThread, "execOut");
    threadExecError = new Thread(outErrorThread, "execError");
    threadExecIn = new Thread(inputThread, "execIn");
    bRunThreads = true;
    threadExecOut.start();
    threadExecError.start();
    //threadExecIn.start();
  }
  
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
  public void execWait(String cmdLine
  , String input
  , Appendable output
  , Appendable error
  )
  { String[] cmdArgs = splitArgs(cmdLine);
    execWait(cmdArgs, input, output, error);
  }
  
  
  
  /**Executes a command with arguments and waits for its finishing.
   * @param cmdArgs The command and its arguments. The command is cmdArgs[0]. 
   *        Any argument have to be given with one element of this String array.
   * @param input The input stream of the command. TODO not used yet.
   * @param output Will be filled with the output of the command.
   *        If output ==null then no output is expected and the end of command execution is not await.
   *        But in this case error should not be ==null because errors of command invocation are written there.
   * @param error Will be filled with the error output of the command. 
   *        Maybe null, then the error output will be written to output 
   */
  public void execWait(String[] cmdArgs
  , String input
  , Appendable output
  , Appendable error
  )
  {
    processBuilder.command(cmdArgs);
    if(error == null){ error = output; }
    userError = error;
    userOutput = output;
    try
    {
      process = processBuilder.start();
      if(output !=null){
        bRunExec = true;
        out1 = new BufferedReader(new InputStreamReader(process.getInputStream()));
        err1 = new BufferedReader(new InputStreamReader(process.getErrorStream()));
        //processIn = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
        process.waitFor();
      }
    } catch(Exception exception)
    { try{ error.append( "Problem: ").append(exception.getMessage());}
      catch(IOException exc){ throw new RuntimeException(exc); }
    }
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
  
  
  Runnable inOutThread = new Runnable()
  { @Override public void run()
    { while(bRunThreads){
        if(bRunExec){
          String sLine;
          boolean bFinished = true;
          try{
            if( (sLine= out1.readLine()) !=null){
              userOutput.append(sLine).append('\n');
              bFinished = false;
            }
          } catch(IOException exc){
            
          }
          if(bFinished){
            bRunExec = false;
          }
          try { Thread.sleep(50); } catch (InterruptedException exc) { }
          
        } else {
          try { Thread.sleep(100); } catch (InterruptedException exc) { }
        }
        
      }
    }
  };
  
  

  Runnable outErrorThread = new Runnable()
  { @Override public void run()
    { while(bRunThreads){
        if(bRunExec){
          String sLine;
          boolean bFinished = true;
          try{
            if( (sLine= err1.readLine()) !=null){
              userError.append(sLine).append('\n');
              bFinished = false;
            }
          } catch(IOException exc){
            
          }
          if(bFinished){
            bRunExec = false;
          }
          try { Thread.sleep(50); } catch (InterruptedException exc) { }
          
        } else {
          try { Thread.sleep(100); } catch (InterruptedException exc) { }
        }
        
      }
    }
  };
  
  

  Runnable inputThread = new Runnable()
  { @Override public void run()
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
  };
  
  @Override public void finalize()
  {
    bRunThreads = false;
  }
  
  
  public final static void main(String[] args)
  {
    CmdExecuter main = new CmdExecuter();
    main.execWait("cmd /C", null, null, null);
    main.finalize();
  }
  

}
