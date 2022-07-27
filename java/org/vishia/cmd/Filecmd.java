package org.vishia.cmd;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;

import org.vishia.mainCmd.MainCmd;
import org.vishia.mainCmd.MainCmd_ifc;
import org.vishia.util.FileSystem;

/**This class executes a given command for all files with given list.
 * The command is given as argument string with placeholder for the file name, path, directory path etc.
 * The command is execute. Its output can be captured in another file.
 * @author Hartmut Schorrig
 *
 */
public class Filecmd
{
  
  /**Version of this class.
   * 
   */
  public static final int version = 0x20110720;
  
  /**Class holds command line arguments. This class can be instantiated and filled by another Java programm too.
   * It is used here if the command line invocation is used. 
   *
   */
  static public class Cargs
  {
    public String sLine;
    
    public boolean bExecute;
    
    /**This file is written as output, one line for each file. */
    public String sFileOut;
    
    public String sFileMask;
    
    /**Java class path of a class, of which main cmd is invoked with the given file.
     * 
     */
    public String sExecCmdClass;
    
    /**Java class path of a class, of which is executed as output filter.
     * 
     */
    public String sExecFilterOutputClass;
    
    
  }

  private final Cargs cargs;  
  
  private final MainCmd_ifc mainCmd;  
  
  private final PrepareCmd cmd = new PrepareCmd();
  
  private final List<File> files = new LinkedList<File>();
  
  Filecmd(MainCmd_ifc mainCmd, Cargs cargs)
  { this.mainCmd = mainCmd;
    this.cargs = cargs; 
  }
  
  ProcessBuilder processBuilder;
  
  
  Writer out;
  
  
  void execute()
  {
    boolean ok = true;
    ok = FileSystem.addFileToList(cargs.sFileMask, files);
    if(!ok){
      mainCmd.writeError("Filecmd- file not found in path; " + cargs.sFileMask);
    }
    if(ok && cargs.sFileOut !=null){
      try{ out = new FileWriter(cargs.sFileOut); }
      catch(IOException exc){ mainCmd.writeError("Filecmd- can't open outfile; " + cargs.sFileOut, exc); 
        ok = false; 
      }
    }
    if(ok){
      for(File file: files){
        if(cargs.sLine !=null){ executeCmd(file); }
      }
    }
    if(out !=null){ try{ out.close(); } catch(IOException exc){} }
  }
  
  
  
  
  void executeCmd(File file)
  { cmd.set_cmd(cargs.sLine);
    File1Arg arg = new File1Arg(file);
    String[] sCmd2 = cmd.prepareCmd(arg);
    if(cargs.bExecute){
      if(processBuilder == null){
        processBuilder = new ProcessBuilder();
      }
      StringBuilder output = new StringBuilder();
      StringBuilder error = new StringBuilder();
      mainCmd.executeCmdLine(processBuilder, sCmd2, null,0, output, error);
      if(out == null){
        try{ 
          out.append(output).append("\n"); 
          out.append(error).append("\n"); 
        }
        catch(IOException exc){ mainCmd.writeError("write error", exc); }
      }
    } else if(out !=null){
      try{ out.append(sCmd2[0]).append("\n"); }
      catch(IOException exc){ mainCmd.writeError("write error", exc); }
    }
  }
  
  
  
  
  private static class Cmdline extends MainCmd
  {

    final Cargs cargs;
    
    Cmdline(Cargs cargs)
    { this.cargs = cargs;
      addAboutInfo("Filecmd copyleft Hartmut Schorrig " + String.format("%x", Filecmd.version));
      addHelpInfo("org.vishia.cmd.Filecmd searches some files and executes a command for each file.");
      addHelpInfo("cmd=CMDLINE - executes CMDLINE as shell command of the underlying operation system");
      addHelpInfo("  <:file> - replaces the complete path-name of the file in the command line string");
      addHelpInfo("    as absolute or relative path how it is given in the mask=PATHMASK argument.");
      addHelpInfo("  <:absFile>   - complete absoute path-name of the file");
      addHelpInfo("  <:absDir>    - absoute path of the directory of the file");
      addHelpInfo("  <:localFile> - complete local path-name of the file.");
      addHelpInfo("    The local part of a path can be designate with a ':' in the mask=PATHMASK");
      addHelpInfo("  <:localDir>  - local path of the file's directory.");
      addHelpInfo("  <:name>      - onyl the name of the file, without extension");
      addHelpInfo("  <:nameExt>   - complete name of the file");
      addHelpInfo("  <:ext>       - onyl the extension, it is the part after a last dot.");
      addHelpInfo("files=PATHMASK - The path and mask to select files.");
      addHelpInfo("  There are additional capabilities to select files:");
      addHelpInfo("  PATH/**/  means any directory starting from PATH");
      addHelpInfo("  PATH:PATHLOCAL A colon is the separator for local path. The local path starts from colon.");
      addHelpInfo("                 The ':' replaces a '/'. But it is accepted from position 3 only ");
      addHelpInfo("                 because at position 2 it is a windows-drive separator.");
      addHelpInfo("execJava=JAVAPATH a java-path to a class which should be executed. ");
      addHelpInfo("                 The class should be able to found in any jar file");
      addHelpInfo("                 of the given CLASSPATH of the java invocation.");
      addHelpInfo("  execJava=org.vishia.cmd.DeleteFile (example)");
      addHelpInfo("outJava=JAVAPATH a java-path to a class which is used as output text preparer.");
      addHelpInfo("  execJava=org.vishia.cmd.CatOutput (example)");
      addHelpInfo("all other cmd line options are forwarded to the cmd=CMD and to the execJava and outJava class");
    }
    
    
    @Override protected boolean checkArguments()
    { return true;
    }

    @Override protected boolean testArgument(String argc, int nArg) //throws ParseException
    { boolean ok = true;
      if(argc.startsWith("cmd:")){ cargs.sLine = argc.substring(4); cargs.bExecute = true; }
      else if(argc.startsWith("line:")){ cargs.sLine = argc.substring(5); }
      else if(argc.startsWith("out:")){ cargs.sFileOut = argc.substring(4); }
      else if(argc.startsWith("files:")){ cargs.sFileMask = argc.substring(6); }
      else if(argc.startsWith("execjava:")){ cargs.sExecCmdClass = argc.substring(9); }
      else if(argc.startsWith("outjava:")){ cargs.sExecFilterOutputClass = argc.substring(8); }
      else { ok = false; }
      return ok;
    }
    
  }
  
  private static class File1Arg implements CmdGetFileArgs_ifc
  { final File file;
    public File1Arg(File file){ this.file = file; }
    @Override public File getFile1() { return file; }
    @Override public File getFile2() { return file; }
    @Override public File getFile3() { return file; }
    @Override public File getFileSelect() { return file; }
    @Override public void  prepareFileSelection(){}
    
  };
  
  
  public static void main(String[] args)
  { Cargs cargs = new Cargs();
    MainCmd mainCmd = new Cmdline(cargs);
    //mainCmd.
    try{ mainCmd.parseArguments(args); }
    catch(ParseException exc){  }
    Filecmd main = new Filecmd(mainCmd, cargs);
    main.execute();
  }
  
}
