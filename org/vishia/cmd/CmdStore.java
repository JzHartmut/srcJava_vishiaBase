package org.vishia.cmd;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.vishia.mainCmd.MainCmd_ifc;
import org.vishia.mainCmd.Report;


/**This class stores some prepared commands. The input of the store is a file, 
 * which contains the assembling of command lines maybe with placeholder for files.
 * 
 * @author Hartmut Schorrig
 *
 */
public class CmdStore
{

  /**Description of one command.
   */
  public class CmdBlock
  {
    /**The identification for user in the selection list. */
    public String name;
    
    /**Some commands of this block. */
    public final List<PrepareCmd> listCmd = new LinkedList<PrepareCmd>();

    /**Possible call from {@link org.vishia.zbnf.ZbnfJavaOutput}. Creates an instance of one command */
    public PrepareCmd new_cmd(){ return new PrepareCmd(); }
    
    /**Possible call from {@link org.vishia.zbnf.ZbnfJavaOutput}. Adds the instance of command */
    public void add_cmd(PrepareCmd cmd)
    { cmd.prepareListCmdReplace();
      listCmd.add(cmd); 
    }
    
  }
  
  /**Contains all commands read from the configuration file in the read order. */
  public final List<CmdBlock> listCmd = new LinkedList<CmdBlock>();

  /**Contains all commands read from the configuration file in the read order. */
  private final Map<String, CmdBlock> idxCmd = new TreeMap<String, CmdBlock>();

  private String syntaxCmd = "Cmds::={ <cmd> }\\e. "
    + "cmd::= <* :?name> : { <*\\n?cmd> \\n } ."; 

  
  public CmdStore()
  {
  }
  
  
  
  /**Possible call from {@link org.vishia.zbnf.ZbnfJavaOutput}. Creates an instance of one command block */
  public CmdBlock new_CmdBlock(){ return new CmdBlock(); }
  
  /**Possible call from {@link org.vishia.zbnf.ZbnfJavaOutput}. Adds the instance of command block. */
  public void add_CmdBlock(CmdBlock value){ listCmd.add(value); idxCmd.put(value.name, value); }
  
  
  public String readCmdCfg(File cfgFile)
  { String sError = null;
    BufferedReader reader = null;
    try{
      reader = new BufferedReader(new FileReader(cfgFile));
    } catch(FileNotFoundException exc){ sError = "CommandSelector - cfg file not found; " + cfgFile; }
    if(reader !=null){
      CmdBlock actBlock = null;
      listCmd.clear();
      String sLine;
      int posSep;
      try{ 
        while( (sLine = reader.readLine()) !=null){
          if( sLine.startsWith("==")){
            posSep = sLine.indexOf("==", 2);  
            //a new command block
            if(actBlock !=null){ add_CmdBlock(actBlock); } 
            actBlock = new_CmdBlock();
            actBlock.name = sLine.substring(2, posSep);
          } else if(sLine.startsWith("@")){
              
          } else  if(sLine.startsWith(" ")){  //a command line
            PrepareCmd cmd = actBlock.new_cmd();
            cmd.cmd = sLine.trim();
            cmd.prepareListCmdReplace();
            actBlock.add_cmd(cmd);
          }      
        }
        if(actBlock !=null){ add_CmdBlock(actBlock); } 
      } 
      catch(IOException exc){ sError = "CommandStore - cfg file read error; " + cfgFile; }
      catch(IllegalArgumentException exc){ sError = "CommandStore - cfg file error; " + cfgFile + exc.getMessage(); }
    }
    return sError;
  }
  
  
  /**Gets a named command
   * @param name The name given in configuration file
   * @return The prepared CmdBlock or null if not found.
   */
  public CmdBlock getCmd(String name){ return idxCmd.get(name); }
  
  
  
}
