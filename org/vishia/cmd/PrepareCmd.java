package org.vishia.cmd;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.vishia.util.FileSystem;


/**This class prepares a command and holds it maybe with place holder for files.
 * A command may be written in form for example ">cmd <*file1> -option <*dirAbs>".
 * The optional first char determines how the command should be executed.
 * The preparation finds out the textual given placeholder and stores it in an internal format.
 * The method {@link #PrepareCmd()} returns the command with given actual parameter
 * in the form, which is able to execute with java.lang.Process
 * <br><br>
 * <b>First char of command maybe the kind of execution.</b> See { @link #PrepareCmd(char)}
 * <br><br>
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
   * <li>2011-10-08 Hartmut new Detection of >&� to select kind of command, {@link #getJavaClass()} prepared (not ready)
   * </ul>
   */
  public static int version = 0x20111011;
  
  /**Element in the {@link #listPlaceholderForCmdArgsTemplate}. 
   * arg - index of argument, pos-position in sArg[arg], 
   * what 0x0f0000 nr of file */
  private static class CmdReplace{ int arg; int pos; int what;} 
  
  
  /**Helper class to get parts from the file path.
   * The file is accessed from {@link CmdGetFileArgs_ifc} only one time after clean().
   * The getting of file parts is optimized, for example only one time getCanonicalPath().
   */
  private static class FileParts
  { /**what part from args, 1..3 or 0 for fileSelect. */
    final int what;
    /**Reference set on clean. */
    private CmdGetFileArgs_ifc args;
    /**Get from args if firstly used. */
    File file;
    
    String sPathFile;
    String sPathLocal;
    String sPathCanonical;
    int posFile;
    int posFileInLocalPath;
    String sName;
    int posExt;
    
    FileParts(int what){ this.what = what; }
    
    /**called on new prepareCmd invocation: */
    void clean(CmdGetFileArgs_ifc args){ 
      this.args = args;
      file = null;
      sPathCanonical = sPathLocal = sPathFile = sName = null; posFileInLocalPath = posFile = posExt = -2; 
    }
    
    void getFile(){
      switch(what){
      case 0: file = args.getFileSelect(); break;
      case 1: file = args.getFile1(); break;
      case 2: file = args.getFile2(); break;
      case 3: file = args.getFile3(); break;
      }
    }
    
    
    /**Returns the directory like it is given by the File constructor.
     * It may be a local file starting from current directory.
     */
    String getGivenFile(){
      if(file ==null){ getFile(); }
      if(sPathFile ==null){ sPathFile = file.getPath(); }
      return sPathFile;  
    }
    
    
    
    String getNameExt(){
      if(file == null){ getFile(); }
      if(sName ==null){ sName = file.getName(); }
      return sName;
    }

    String getNameOnly(){
      if(file ==null){ getFile(); }
      if(sName ==null){ sName = file.getName(); }
      if(posExt ==-2){ posExt = sName.lastIndexOf('.'); }
      return posExt < 0 ? sName : sName.substring(0,posExt);
    }

    String getExt(){
      if(file ==null){ getFile(); }
      if(sName ==null){ sName = file.getName(); }
      if(posExt ==-2){ posExt = sName.lastIndexOf('.'); }
      return posExt < 0 ? "" : sName.substring(posExt+1); //Note if '.' not found, posExt==-1, returns 
    }
    
    String getLocalFile(){
      if(file ==null){ getFile(); }
      if(sPathLocal ==null){ sPathLocal = file.getPath(); }
      return sPathLocal;  
    }
    
    String getLocalDir(){
      if(file ==null){ getFile(); }
      if(sPathLocal ==null){ sPathLocal = file.getPath(); }
      if(posFileInLocalPath ==-2){ posFileInLocalPath = sPathLocal.lastIndexOf('/'); }
      return posFileInLocalPath == -1 ? "." : sPathLocal.substring(0, posFileInLocalPath);  
    }
    
    /**Returns the directory like it is given by the File constructor.
     * It may be a local file starting from current directory.
     */
    String getGivenDir(){
      if(file ==null){ getFile(); }
      if(sPathFile ==null){ sPathFile = file.getPath(); }
      if(posFileInLocalPath ==-2){ posFileInLocalPath = sPathFile.lastIndexOf('/'); }
      return posFileInLocalPath == -1 ? "." : sPathFile.substring(0, posFileInLocalPath);  
    }
    
    
    
    String getCanonicalFile(){
      if(file ==null){ getFile(); }
      if(sPathCanonical ==null){ sPathCanonical = FileSystem.getCanonicalPath(file); }
      return sPathCanonical;
    }

    String getCanonicalDir(){
      if(file ==null){ getFile(); }
      if(sPathCanonical ==null){ sPathCanonical = FileSystem.getCanonicalPath(file); }
      if(posFile == -2){ posFile = sPathCanonical.lastIndexOf('/'); }
      return sPathCanonical.substring(0, posFile);
    }
  }
  
  /**Instance combines 4 instances to get parts from given files. */
  private static class File3Parts{
    final FileParts file = new FileParts(0);
    final FileParts file1 = new FileParts(1);
    final FileParts file2 = new FileParts(2);
    final FileParts file3 = new FileParts(3);
    /**called on new prepareCmd invocation: */
    void clean(CmdGetFileArgs_ifc args){ 
      file.clean(args); file1.clean(args); file2.clean(args); file3.clean(args);
    }
  }
  
  private final File3Parts part = new File3Parts();
  
  
  /**List of positions where somewhat is to be preplaced. */
  private List<CmdReplace> listPlaceholderForCmdArgsTemplate;
  
  /**Command string without placeholder. The positions of the placeholder
   * are contained in {@link #listPlaceholderForCmdArgsTemplate}. 
   * It may be a template of the command because the correct files to use are not contained,
   * instead the {@link #listPlaceholderForCmdArgsTemplate} contains the kind of placeholder. */
  private String[] cmdArgsTemplate;
  
  /**Kind of the execution as default value given on ctor */
  private final char cKindOfExecutionDefault;
  
  /**Kind of the execution of the prepared command */
  private char cKindOfExecutionPrepared;
  
  /**The command how it is given with place holder. Maybe from ZBNF2Java-parsing. 
   * This value is used only as parameter in {@link #prepareListCmdReplace()}. It isn't use after them.
   */
  private String[] cmdSrc;
  
  
  /**Name of the command which it is showing to select. */
  private String name;
  
  
  
  /**Creates an instance of a Preparer for a command.
   * The argument determines the kind of execution. It is the default if the given command doesn't contain
   * a first char which determines the kind of execution.
   * <ul>
   * <li> '>' or '%': invocation of command with output and error output
   * <li> '&' invocation of command without in/out pipe, typical start another process without feedback.
   * <li> '$' opens a shell for invocation of command.
   * <li> '*' call of a java class. Class path after '*'.
   * </ul> 
   * @param cKindOfExecution One of 0 or '%', '>', '&', '$' or '*'
   */
  PrepareCmd(char cKindOfExecution){
    this.cKindOfExecutionDefault = cKindOfExecution;
  }
  
  PrepareCmd(){
    this.cKindOfExecutionDefault = '>';  //use pipes
  }
  
  /**Prepares the string given {@link #cmd} with placeholder in the internal form.
   * This method can be invoked on creation of this class or if  a new cmd is assigned.
   * 
   */
  public void prepareListCmdReplace()
  { int ixCmd = -1; //preincrement
    cmdArgsTemplate = new String[cmdSrc.length];
    for(String cmd: cmdSrc){
      ixCmd +=1;
      listPlaceholderForCmdArgsTemplate = new LinkedList<CmdReplace>();
      char cCmd = cmd.charAt(0);
      final StringBuilder sCmd2;
      if("%>&$".indexOf(cCmd)>=0){
        cKindOfExecutionPrepared = cCmd;
        sCmd2 = new StringBuilder(cmd.substring(1));
      } else {
        cKindOfExecutionPrepared = cKindOfExecutionDefault; 
        sCmd2 = new StringBuilder(cmd);
      }
      int posSep;
      while( (posSep = sCmd2.indexOf("<*"))>=0){
        String s3 = sCmd2.substring(posSep);
        CmdReplace cmdReplace = new CmdReplace(); 
        cmdReplace.pos = posSep;
        cmdReplace.arg = ixCmd;
        int chars;
        if(     s3.startsWith("<*file"))     { cmdReplace.what = 'f'; chars = 6; }
        else if(s3.startsWith("<*dir"))      { cmdReplace.what = 'd'; chars = 5; }
        else if(s3.startsWith("<*nameExt"))  { cmdReplace.what = 'n'; chars = 9; }
        else if(s3.startsWith("<*name"))     { cmdReplace.what = 'm'; chars = 6; }
        else if(s3.startsWith("<*ext"))      { cmdReplace.what = 'e'; chars = 5; }
        else if(s3.startsWith("<*localFile")){ cmdReplace.what = 'l'; chars = 11; }
        else if(s3.startsWith("<*absFile"))  { cmdReplace.what = 'a'; chars = 9; }
        else if(s3.startsWith("<*localDir")) { cmdReplace.what = 'c'; chars = 10; }
        else if(s3.startsWith("<*absDir"))   { cmdReplace.what = 'b'; chars = 8; }
        else if(s3.startsWith("<*wfile"))     { cmdReplace.what = 'F'; chars = 7; }
        else if(s3.startsWith("<*wdir"))      { cmdReplace.what = 'D'; chars = 6; }
        else if(s3.startsWith("<*wlocalFile")){ cmdReplace.what = 'L'; chars = 12; }
        else if(s3.startsWith("<*wabsFile"))  { cmdReplace.what = 'A'; chars = 10; }
        else if(s3.startsWith("<*wlocalDir")) { cmdReplace.what = 'C'; chars = 11; }
        else if(s3.startsWith("<*wabsDir"))   { cmdReplace.what = 'B'; chars = 9; }
        else { chars = 0; }         
        if(chars >0){
          char nrFile = s3.charAt(chars);
          switch(nrFile){
          case '1': cmdReplace.what |= 0x10000; chars +=1; break;
          case '2': cmdReplace.what |= 0x20000; chars +=1; break;
          case '3': cmdReplace.what |= 0x30000; chars +=1; break;
          case '>': break;
          default: chars = 0;  break;
          }
          if(chars >0 && s3.length() >chars && s3.charAt(chars) == '>'){
            listPlaceholderForCmdArgsTemplate.add(0,cmdReplace); 
            sCmd2.replace(posSep, posSep + chars + 1, "");
          }
        } else {
          posSep +=2; //skip over <*
          //throw new IllegalArgumentException("Illegal placeholder: " + s3.substring(0, 12));
        }
      }
      cmdArgsTemplate[ixCmd] = sCmd2.toString();
    }
    
  }
  
  
  /**Sets the command and prepares it.
   * This method may be invoked using {@link org.vishia.zbnf.ZbnfJavaOutput} with the ZBNF parser.
   * @param cmd The command with placeholder.
   */
  public void set_cmd(String cmd)
  { this.cmdSrc = CmdExecuter.splitArgs(cmd); 
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
   * @param args Interface to get the input files for the command. Getting the file is optimized. 
   *   {@link CmdGetFileArgs_ifc#getFile1()} etc. is only called one time in this method.
   *   It means the getting process can be complex in the implementor of {@link CmdGetFileArgs_ifc}.
   *   Getting of file parts is optimized, for example only one time getCanonicalPath().
   * @return The command how it is executable as parameter for java.lang.Process or any other command invocation.
   */
  public String[] prepareCmd(CmdGetFileArgs_ifc args)
  {
    int ixArg = 0;
    String [] cmdArgs = new String[cmdArgsTemplate.length];
    if(listPlaceholderForCmdArgsTemplate ==null){ 
      prepareListCmdReplace(); 
    }
    //maybe the files should be selected.
    args.prepareFileSelection();
    part.clean(args);
    //replace placeholder:
    for(CmdReplace repl: listPlaceholderForCmdArgsTemplate){
      while(ixArg < repl.arg){
        //lines without any placeholder
        cmdArgs[ixArg] = cmdArgsTemplate[ixArg];
        ixArg +=1;
      }
      
      StringBuilder sCmd2 = new StringBuilder(cmdArgsTemplate[ixArg]);
      final FileParts parts;
      switch(repl.what & 0x000f0000){
      case 0x00000000: parts = part.file; break;
      case 0x00010000: parts = part.file1; break;
      case 0x00020000: parts = part.file2; break;
      case 0x00030000: parts = part.file3; break;
      default: throw new IllegalArgumentException("faulty part designation:" + Integer.toHexString(repl.what));
      }//switch parts
      //get the requestet representation of file:   
      switch(repl.what & 0xffff){
      case 'f': sCmd2.insert(repl.pos, parts.getGivenFile()); break;
      case 'd': sCmd2.insert(repl.pos, parts.getGivenDir()); break;  //dir
      case 'm': sCmd2.insert(repl.pos, parts.getNameOnly()); break;
      case 'n': sCmd2.insert(repl.pos, parts.getNameExt()); break;
      case 'e': sCmd2.insert(repl.pos, parts.getNameExt()); break;
      case 'l': sCmd2.insert(repl.pos, parts.getLocalFile()); break;
      case 'a': sCmd2.insert(repl.pos, parts.getCanonicalFile()); break;
      case 'c': sCmd2.insert(repl.pos, parts.getLocalDir()); break;  //dir
      case 'b': sCmd2.insert(repl.pos, parts.getCanonicalDir()); break; 
      } //switch
      cmdArgs[ixArg] = sCmd2.toString();
      ixArg +=1;
    }
    while(ixArg < cmdArgs.length){
      //lines without any placeholder
      cmdArgs[ixArg] = cmdArgsTemplate[ixArg];
      ixArg +=1;
    }
    return cmdArgs;
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
    return cKindOfExecutionPrepared == '>';
  }
  
  public char getKindOfExecution(){ return cKindOfExecutionPrepared; }
  
  
}
