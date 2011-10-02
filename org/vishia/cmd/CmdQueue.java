package org.vishia.cmd;

import java.io.File;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.vishia.cmd.CmdStore.CmdBlock;
import org.vishia.mainCmd.MainCmd_ifc;
import org.vishia.mainCmd.Report;

/**This class stores some prepared commands for execution. 
 * @author hartmut
 *
 */
public class CmdQueue
{
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

  
  //private final ProcessBuilder processBuilder = new ProcessBuilder();
  
  private final StringBuilder cmdOutput = new StringBuilder(4000);
  
  private final StringBuilder cmdError = new StringBuilder(1000);
  
  public CmdQueue(MainCmd_ifc mainCmd)
  {
    this.mainCmd = mainCmd;
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

  public void setWorkingDir(File file)
  { final File dir;
    if(!file.isDirectory()){
      dir = file.getParentFile();
    } else {
      dir = file;
    }
    //processBuilder.directory(dir);
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
      try{
        if(cmd1.currentDir !=null){
          executer.setCurrentDir(cmd1.currentDir);
        }
        for(PrepareCmd cmd: cmd1.cmdBlock.listCmd){
          String sCmd = cmd.prepareCmd(cmd1);
          if(sCmd.startsWith("@")){
            
          } else {
            //a operation system command:
            //executer.execWait(sCmd, null, this.cmdOutput, cmdError);
            executer.execWait(sCmd, null, System.out, System.err);
            //executer.execWait(sCmd, null, cmdOutput, cmdError);
            //System.out.append(cmdOutput);
            //System.out.append(cmdError);
            cmdOutput.setLength(0);
            cmdError.setLength(0);
          }
        }
        System.out.println(cmd1.cmdBlock.name);
      } catch(Exception exc){ System.out.println("Exception " + exc.getMessage()); }
    }
  }


  

}
