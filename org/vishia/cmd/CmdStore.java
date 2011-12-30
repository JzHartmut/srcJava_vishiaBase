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

  /**Version and history
   * <ul>
   * <li>2011-12-31 Hartmut chg {@link CmdBlock#title} is new, the syntax of configfile is changed.
   *   This class is used to capture all executables for a specified extension for The.file.Commander 
   * </ul>
   */
  int version = 0x20111231;
  
  /**Description of one command.
   */
  public static class CmdBlock
  {
    /**The identification for user in the selection list. */
    public String name;
  
    /**The title of the cmd, for selection. It is the part after ':' in the title line:
     * <pre>
     * ==name: title==
     * </pre>
     * */
    public String title;
    
    /**Some commands of this block. */
    public final List<PrepareCmd> listBlockCmds = new LinkedList<PrepareCmd>();

    /**Possible call from {@link org.vishia.zbnf.ZbnfJavaOutput}. Creates an instance of one command */
    public PrepareCmd new_cmd(){ return new PrepareCmd(); }
    
    /**Possible call from {@link org.vishia.zbnf.ZbnfJavaOutput}. Adds the instance of command */
    public void add_cmd(PrepareCmd cmd)
    { cmd.prepareListCmdReplace();
      listBlockCmds.add(cmd); 
    }
    
    /**Returns all commands which are contained in this CmdBlock. */
    public final List<PrepareCmd> getCmds(){ return listBlockCmds; }
    
  }
  
  /**Contains all commands read from the configuration file in the read order. */
  private final List<CmdBlock> listCmds = new LinkedList<CmdBlock>();

  /**Contains all commands read from the configuration file in the read order. */
  private final Map<String, CmdBlock> idxCmd = new TreeMap<String, CmdBlock>();

  private String XXXsyntaxCmd = "Cmds::={ <cmd> }\\e. "
    + "cmd::= <* :?name> : { <*\\n?cmd> \\n } ."; 

  
  public CmdStore()
  {
  }
  
  
  
  /**Possible call from {@link org.vishia.zbnf.ZbnfJavaOutput}. Creates an instance of one command block */
  public CmdBlock new_CmdBlock(){ return new CmdBlock(); }
  
  /**Possible call from {@link org.vishia.zbnf.ZbnfJavaOutput}. Adds the instance of command block. */
  public void add_CmdBlock(CmdBlock value){ listCmds.add(value); idxCmd.put(value.name, value); }
  
  
  public String readCmdCfg(File cfgFile)
  { String sError = null;
    BufferedReader reader = null;
    try{
      reader = new BufferedReader(new FileReader(cfgFile));
    } catch(FileNotFoundException exc){ sError = "CommandSelector - cfg file not found; " + cfgFile; }
    if(reader !=null){
      CmdBlock actBlock = null;
      listCmds.clear();
      String sLine;
      try{ 
        while( (sLine = reader.readLine()) !=null){
          if( sLine.startsWith("==")){
            final int posColon = sLine.indexOf(':');
            final int posEnd = sLine.indexOf("==", 2);  
            //a new command block
            if(actBlock !=null){ add_CmdBlock(actBlock); }  //the last one. 
            actBlock = new_CmdBlock();
            if(posColon >=0 && posColon < posEnd){
              actBlock.name = sLine.substring(2, posColon).trim();
              actBlock.title = sLine.substring(posColon+1, posEnd).trim();
            } else {
              actBlock.name = sLine.substring(2, posEnd).trim();
              actBlock.title = "";
            }
          } else if(sLine.startsWith("@")){
              
          } else  if(sLine.startsWith(" ")){  //a command line
            PrepareCmd cmd = actBlock.new_cmd();
            cmd.set_cmd(sLine.trim());
            //cmd.prepareListCmdReplace();
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
  
  /**Gets a contained commands for example to present in a selection list.
   * @return The list.
   */
  public final List<CmdBlock> getListCmds(){ return listCmds; }
  
}
