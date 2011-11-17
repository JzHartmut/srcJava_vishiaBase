package org.vishia.cmd;

import java.io.File;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.vishia.cmd.CmdStore.CmdBlock;
import org.vishia.mainCmd.MainCmd_ifc;
import org.vishia.mainCmd.Report;

/**This class stores some prepared commands for execution and executes it one after another.
 * The commands can contain placeholder for files.
 * @author hartmut Schorrig
 *
 */
public class CmdQueue
{
  
  /**Version and history
   * <ul>2011-10-09 Hartmut new 
   * <li>2011-11-17 Hartmut new {@link #close()} to stop threads.
   * <li>2011-07-00 created
   * </ul>
   */
  public static final int version = 0x20111009;
  
  private static class PendingCmd implements CmdGetFileArgs_ifc
  {
    final CmdStore.CmdBlock cmdBlock;
    final File[] files;
    File currentDir;
    
    public PendingCmd(CmdStore.CmdBlock cmdBlock, File[] files, File currentDir)
    { this.cmdBlock = cmdBlock;
      this.files = files;
      this.currentDir = currentDir;
    }

    @Override public void  prepareFileSelection()
    { }

    @Override public File getFileSelect()
    { return files[0];
    }
    
    @Override public File getFile1() { return files[0]; }
    
    @Override public File getFile2() { return files[1]; }
    
    @Override public File getFile3() { return files[2]; }

  }
  
  private final ConcurrentLinkedQueue<PendingCmd> pendingCmds = new ConcurrentLinkedQueue<PendingCmd>();
  
  private final CmdExecuter executer = new CmdExecuter();
  
  private final MainCmd_ifc mainCmd;

  
  private boolean busy;
  
  //private final ProcessBuilder processBuilder = new ProcessBuilder();
  
  private Appendable cmdOutput = System.out;
  
  private Appendable cmdError = System.err;
  
  public CmdQueue(MainCmd_ifc mainCmd)
  {
    this.mainCmd = mainCmd;
  }
  

  /**Sets the output and error stream destination
   * @param userOut Destination for output of all executed commands. 
   *   If null then any cmd invocation is done without awaiting the end of execution.
   *   The output and error output isn't used then.
   * @param userErr may be null, then the error is redirected to output
   */
  public void setOutput(Appendable userOut, Appendable userErr)
  {
    cmdOutput = userOut;
    cmdError = userErr;
  }
  
  public void setWorkingDir(File file)
  { final File dir;
    if(!file.isDirectory()){
      dir = file.getParentFile();
    } else {
      dir = file;
    }
    //processBuilder.directory(dir);
  }
  

  
  /**Adds a command to execute later. The execution may be done in another thread.
   * The adding is thread-safe and a cheap operation. It uses a ConcurrentListQueue. 
   * @param cmdBlock The command block
   * @param files Some files
   * @return Number of members in queue pending for execution.
   */
  public int addCmd(CmdBlock cmdBlock, File[] files, File currentDir)
  {
    pendingCmds.add(new PendingCmd(cmdBlock, files, currentDir));  //to execute.
    return pendingCmds.size();
  }

  /**Execute the pending commands.
   * This method should be called in a specified user thread.
   * 
   */
  public final void execCmds()
  {
    CmdStore.CmdBlock block;
    PendingCmd cmd1;
    while( (cmd1 = pendingCmds.poll())!=null){
      busy = true;
      try{
        if(cmd1.currentDir !=null){
          executer.setCurrentDir(cmd1.currentDir);
        }
        for(PrepareCmd cmd: cmd1.cmdBlock.getCmds()){
          Class<?> javaClass = cmd.getJavaClass();
          if(javaClass !=null){
            
          } else {
            //a operation system command:
            String sCmd = cmd.prepareCmd(cmd1);
            if(cmd.usePipes()){
              int exitCode = executer.execute(sCmd, null, cmdOutput, cmdError);
              if(exitCode == 0){ cmdOutput.append("JavaCmd: cmd execution successfull\n"); }
              else {cmdOutput.append("JavaCmd: cmd execution errorlevel = " + exitCode + "\n"); }
            } else {
              executer.execute(sCmd, null, null, null);
              cmdOutput.append("JavaCmd; started; " + sCmd + "\n");
            }
          }
        }
        System.out.println(cmd1.cmdBlock.name);
      } catch(Exception exc){ System.out.println("Exception " + exc.getMessage()); }
    }
    busy = false;
  }


  /**Aborts a running cmd. If the cmd queue contains any further cmd, it is started yet.
   * If 
   * @return true if any cmd is aborted.
   */
  public boolean abortCmd()
  {
    return executer.abortCmd();
  }
  
  
  public boolean isBusy(){ return busy; }
  
  
  public void close(){
    executer.close();
  }

}
