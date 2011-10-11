package org.vishia.cmd;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;


/**This class prepares a command and holds it maybe with place holder for files.
 * A command may be written in form for example ">cmd <*file1> -option <*dirAbs>".
 * The optional first char determines how the command should be executed.
 * The preparation finds out the textual given placeholder and stores it in an internal format.
 * The method {@link #PrepareCmd()} returns the command with given actual parameter
 * in the form, which is able to execute with java.lang.Process
 * <br><br>
 * <b>First char of command</b>:
 * <ul>
 * <li> '>' invocation of command with output and error output
 * <li> '&' invocation of command without in/out pipe, typical start another process without feedback.
 * <li> '§' call of a java class. Class path after §.
 * </ul> 
 * <b>Placeholder</b>
 * <ul>
 * <li> <*file>: The file how it is given in arguments, maybe a relative path.
 * <li> <*dir>: The dir how it is given in arguments, maybe a relative path.
 * <li> <*name>: The name of the file, without path, without extension. It is the string after the last '/' and before the last '.'
 * <li> <*nameExt>: The name of the file without path, but with extension. It is the string after the last '/'
 * <li> <*ext>: The extension. It is the string after the last '.'
 * <li> <*localFile>: The local path of the file. A local path is supported by some calling conventions. It makes it possible
 *   to use a path, but starting from any given directory.       
 * <li> <*absFile>: The absolute canonical path of the file. 
 * <li> <*localDir>: The local path of the directory where the file is placed.
 * <li> <*absDir>: The absolute canonical path of the directory where the file is placed.
 * </ul>
 * 
 * @author Hartmut Schorrig
 *
 */
public final class PrepareCmd
{
  
  
  /**Version and history:
   * <ul>
   * <li>2011-10-08 Hartmut new Detection of >&§ to select kind of command, {@link #getJavaClass()} prepared (not ready)
   * </ul>
   */
  public static int version = 0x20111011;
  
  /**Element in the {@link #listCmdReplace}. */
  private static class CmdReplace{ int pos; char what;} 
  
  /**List of positions where somewhat is to be preplaced. */
  private List<CmdReplace> listCmdReplace;
  
  /**Command string without placeholder. The positions of the placeholder
   * are contained in {@link #listCmdReplace}. */
  private String sCmdTemplate;
  
  /**Kind of the command, the first char:
   */
  private char cKindOfCmd;
  
  /**The command how it is given with place holder. Maybe from ZBNF2Java-parsing. 
   * This value is used only as parameter in {@link #prepareListCmdReplace()}. It isn't use after them.
   */
  private String cmd;
  
  
  /**Name of the command which it is showing to select. */
  private String name;
  
  /**Prepares the string given {@link #cmd} with placeholder in the internal form.
   * This method can be invoked on creation of this class or if  a new cmd is assigned.
   * 
   */
  public void prepareListCmdReplace()
  {
    listCmdReplace = new LinkedList<CmdReplace>();
    char cCmd = cmd.charAt(0);
    final StringBuilder sCmd2;
    if(">&§".indexOf(cCmd)>=0){
      cKindOfCmd = cCmd;
      sCmd2 = new StringBuilder(cmd.substring(1));
    } else {
      cKindOfCmd = '>';
      sCmd2 = new StringBuilder(cmd);
    }
    int posSep;
    while( (posSep = sCmd2.indexOf("<*"))>=0){
      String s3 = sCmd2.substring(posSep);
      CmdReplace cmdReplace = new CmdReplace(); 
      cmdReplace.pos = posSep;
      int chars = 0;
      if(     s3.startsWith("<*file>"))     { cmdReplace.what = 'f'; chars = 4; }
      else if(s3.startsWith("<*dir>"))      { cmdReplace.what = 'F'; chars = 3; }
      else if(s3.startsWith("<*name>"))     { cmdReplace.what = 'm'; chars = 4; }
      else if(s3.startsWith("<*nameExt>"))  { cmdReplace.what = 'n'; chars = 7; }
      else if(s3.startsWith("<*ext>"))      { cmdReplace.what = 'e'; chars = 3; }
      else if(s3.startsWith("<*localFile>")){ cmdReplace.what = 'l'; chars = 9; }
      else if(s3.startsWith("<*absFile>"))  { cmdReplace.what = 'a'; chars = 8; }
      else if(s3.startsWith("<*localDir>")) { cmdReplace.what = 'L'; chars = 8; }
      else if(s3.startsWith("<*absDir>"))   { cmdReplace.what = 'A'; chars = 6; }
      else { cmdReplace = null; }         
      if(chars >0){
        listCmdReplace.add(0,cmdReplace); 
        sCmd2.replace(posSep, posSep + chars + 3, "");
      } else {
        throw new IllegalArgumentException("Illegal placeholder: " + sCmd2.substring(posSep));
      }
    }
    sCmdTemplate = sCmd2.toString();
    
  }
  
  
  /**Sets the command and prepares it.
   * This method may be invoked using {@link org.vishia.zbnf.ZbnfJavaOutput} with the ZBNF parser.
   * @param cmd The command with placeholder.
   */
  public void set_cmd(String cmd)
  { this.cmd = cmd; 
    prepareListCmdReplace();
  }
  
  /**Sets the name of the command. Note that the name isn't necessary internal. It is only given
   * to present commands with a short name in a list.
   * This method may be invoked using {@link org.vishia.zbnf.ZbnfJavaOutput} with the ZBNF parser.
   * @param name The string given name.
   */
  public void set_name(String name)
  { this.name = name; 
  }
  
  
  /**Prepares a command for execution.
   * @param args Interface to get the input files for the command.
   * @return The command how it is executable as parameter for java.lang.Process or any other command invocation.
   */
  public String prepareCmd(CmdGetFileArgs_ifc args)
  {
    if(listCmdReplace ==null){ 
      prepareListCmdReplace(); 
    }
    StringBuilder sCmd2 = new StringBuilder(sCmdTemplate);
    args.prepareFileSelection();
    File file = args.getFileSelect();
    if(file !=null){
      String sPath = file.getPath().replace('\\', '/');
      String sPathCmd = sPath;
      if(File.separatorChar == '\\'){ 
        sPathCmd = sPath.replace('/', '\\');
      }
      String sPathAbs = ""; 
      try{ sPathAbs = file.getCanonicalPath(); } catch(Exception exc){ sPathAbs="?notFound?"; }
      int posName = sPath.lastIndexOf('/') +1; //0 if no / found.
      int posNameEnd = sPath.lastIndexOf('.');
      int posExt = posNameEnd +1;
      if(posExt < 0){ posExt = posNameEnd = sPath.length();}
      //replace placeholder:
      for(CmdReplace repl: listCmdReplace){
        switch(repl.what){
        case 'f': sCmd2.insert(repl.pos, sPathCmd); break;
        case 'm': sCmd2.insert(repl.pos, sPathCmd.substring(posName, posNameEnd)); break;
        case 'n': sCmd2.insert(repl.pos, sPathCmd.substring(posName)); break;
        case 'e': sCmd2.insert(repl.pos, sPathCmd.substring(posExt)); break;
        case 'l': sCmd2.insert(repl.pos, sPathCmd); break;
        case 'a': sCmd2.insert(repl.pos, sPathAbs); break;
        case 'F': sCmd2.insert(repl.pos, posName ==0 ? "." : sPathCmd.substring(0, posName-1)); break;  //dir
        case 'L': sCmd2.insert(repl.pos, posName ==0 ? "." : sPathCmd.substring(0, posName-1)); break;  //dir
        case 'A': 
          String sDirAbs = null;
          try{ sDirAbs = file.getParentFile().getCanonicalPath();
          } catch(IOException exc){ sDirAbs = "?notFound?"; }
          sCmd2.insert(repl.pos, sDirAbs);
        }
      }
    }
    return sCmd2.toString();
  }

  
  /**If the command is a Java class call, returns the Java class from class loader. Elsewhere returns null.
   * @return
   */
  public Class getJavaClass()
  { return null;  //TODO
  }
  
  
  /**Returns true if the stdin, stdout and stderr should be used and the end of execution is awaiting.
   * @return
   */
  public boolean usePipes(){
    return cKindOfCmd == '>';
  }
  
  
  
}
