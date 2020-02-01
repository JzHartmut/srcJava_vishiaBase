package org.vishia.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.CRC32;

import org.vishia.mainCmd.MainCmd;
import org.vishia.mainCmd.MainCmd_ifc;

/**This class creates a list which information about a file tree and supports change timestamp of given files (touch). 
 * A file list can be created from any directory tree. 
 * It contains properties of files: timestamp, length, relative path inside this file tree
 * and a CRC checksum of the file content.
 * The list can be used to 
 * <ul>
 * <li>touch the timestamp of files if their length and CRC matches.
 * <li>compare files with the content of the list - detect changes, maybe used for version check.
 * </ul>
 * @author Hartmut Schorrig
 *
 */
public class FileList
{

  /**Version, history and license.
   * <ul>
   * <li>2016-11-13 Hartmut new: touch only one file from a given list possible. 
   * <li>2016-11-13 Hartmut chg: The directory line does not contain a date up to now.
   * <li>2016-08-20 Hartmut chg: Other format, better able to read, used for restoring time stamps after git-revert.
   * <li>2014-01-14 Hartmut chg: round up and down to 10 seconds, to ignore second differences on writing.
   * <li>2013-08-09 Hartmut created: The FileList was written by me in 1992..2001 in C++-Language.
   *   Now it is available for Java usage. One of the motivation was the necessity of correction of
   *   time stamps of reverted files from Bazaar and git.
   * </ul>
   * <br><br>
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
  public static final String sVersion = "2016-08-20";

  
  public static class Args
  {
    public char cCmd;
    
    /**Directory where the list is regarded to. */
    public String sDirectory;
    
    public String sMask;
    
    /**Any output for operation. One line per file. Maybe null to prevent output. */
    public Appendable out;
    
    //public String sDateFormat = "yyyy-MM-dd_HH:mm:ss";
    
    public boolean crc = true;
    
    /**Name, maybe path of the file list relative to {@link #sDirectory}. */
    public String sFileList;
  }
  
  final Args args;
  
  
  /**The data format is ISO 8601 but without 'T' as separator between date and time, a space is better readable. */
  final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
  
  /**The data format is ISO 8601 but without 'T' as separator between date and time, a space is better readable. */
  final SimpleDateFormat date_Format = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
  
  final StringFormatter formatter = new StringFormatter(new StringBuilder(100));

  final CRC32 crcCalculator = new CRC32();

  
  public FileList(Args args){
    this.args = args;
    if(args.out == null){
      args.out = System.out;
    }
  }
  
  
  /**Static method to create a list from any directory maybe with selected files.
   * TODO if a list exists alread, read the timestamps from there and compare it. If the timestamp differ in a few seconds,
   * take the timestamp from the given list instead. It is possible that the timestamp may differ a few seconds
   * by small differences in the time of the PC.
   * @param dir path to any directory.
   * @param mask Use "*" to select all files.
   * @param sFilelist Name of the file list relative to the dir, can contain a relative path.
   * @throws IOException
   */
  public static void list(String dir, String mask, String sFilelist) throws IOException
  {
    FileList.Args args = new FileList.Args();
    args.sDirectory = dir;
    args.crc = true;
    args.sMask = mask;
    args.sFileList = dir + "/" + sFilelist;
    FileList main = new FileList(args);
    main.list();
    
  }
  
  
  /**Creates a list of all files which's path and mask is given by {@link #args}. 
   * @throws IOException */
  protected void xxxlist() throws IOException
  {
    List<FileSystem.FileAndBasePath> list = new LinkedList<FileSystem.FileAndBasePath>();
    File baseDir = args.sDirectory == null ? null: new File(args.sDirectory);
    FileSystem.addFilesWithBasePath(baseDir, args.sMask, list);
    Map<String, FileSystem.FileAndBasePath> sort = new TreeMap<String, FileSystem.FileAndBasePath>();
    for(FileSystem.FileAndBasePath entry: list){
      boolean bExclude = false;
      if(entry.localPath.startsWith("/.bzr")){ 
        bExclude = true;
      }
      if(!bExclude){  
        sort.put(entry.localPath, entry);  //sort alphabetical
      }
    }
    Writer out = null;
    out = new java.io.FileWriter(args.sFileList);
    for(Map.Entry<String, FileSystem.FileAndBasePath> entry: sort.entrySet()){
      FileSystem.FileAndBasePath e1 = entry.getValue();
      writeOneFile(out, e1.file, e1.localPath, "");
    }
    if(out !=null) { try{ out.close(); } catch(IOException exc){}}
  }
  
  

  /**Creates a list of all files which's path and mask is given by {@link #args}. 
   * @throws IOException */
  protected void list() throws IOException
  {
    File dir = new File(args.sDirectory);
    CharSequence sDir = FileSystem.normalizePath(dir);  
    int posLocalPath = sDir.length()+1;
    Writer out = null;
    out = new java.io.FileWriter(args.sFileList);
    list(dir, posLocalPath, "", out, 0);
    if(out !=null) { out.close(); }

  }
  
  
  /**Creates a list of all files which's path and mask is given by {@link #args}. 
   * @throws IOException */
  protected void list(File dir, int posLocalPath, CharSequence localDir, Writer out, int recurs) throws IOException
  { if(recurs > 100) throw new IllegalArgumentException("to deep recursion");
    System.out.println("Filelist.list dir=" + dir.getAbsolutePath());
    if(dir.exists()) {
      File[] files = dir.listFiles();
      Map<String, File> sort = new TreeMap<String, File>();
      for(File file: files){ sort.put(file.getName(), file); }
      for(Map.Entry<String, File> entry: sort.entrySet()){
        //write files:
        File file = entry.getValue();
        if( ! file.isDirectory()) {
          String name = file.getName();
          if(name.charAt(0) !='.'){
            writeOneFile(out, file, localDir, name);
          }
        }
      }
      for(Map.Entry<String, File> entry: sort.entrySet()){
        File file = entry.getValue();
        if( file.isDirectory()) {
          String name = file.getName();
          if(name.charAt(0) !='.' && !name.equals(args.sFileList)){
            writeDirectoryLine(out, file, localDir, name);  //the directory entry
            CharSequence path = FileSystem.normalizePath(file);
            CharSequence localDirSub = path.subSequence(posLocalPath, path.length());
            list(file, posLocalPath, localDirSub, out, recurs+1);
          }
        }
      }
    }
  }
  
  String spaces = "                                                                                                    ";  
  
  
  
  @SuppressWarnings("boxing")
  private void writeOneFile(Writer out, File file, CharSequence localDir, String name) throws IOException
  {
    long date = file.lastModified();
    //date = ((date + 5000) /10000) * 10000;      //round up and down to 10 seconds, to ignore second differences
    long length = file.length();
    formatter.reset();
    formatter.addint(length, "2222222222.222 ");
    StringBuilder flags = new StringBuilder("       ");
    if(file.isDirectory()){ flags.setCharAt(1, 'D'); }
    if(file.canExecute()){ flags.setCharAt(2, 'x'); }
    if(!file.canRead()){ flags.setCharAt(3, 'h'); }
    if(!file.canWrite()){ flags.setCharAt(4, 'r'); }
    if(file.isHidden()){ flags.setCharAt(5, 'H'); }
    int crc = 0;
    if(args.crc && file.isDirectory()){
      formatter.add("==DIR===");
    } else {
      try{
        crcCalculator.reset();
        InputStream inp = new FileInputStream(file);
        byte[] buffer = new byte[4096];
        int bytes;
        while((bytes = inp.read(buffer)) >0){
          crcCalculator.update(buffer, 0, bytes);
        }
        inp.close();
        crc = (int)crcCalculator.getValue();
        formatter.addint(crc, "-1111111111");
        //formatter.addHex(crc, 8);
      } catch(Exception exc){
        formatter.add("????????");        
      }
    }
    formatter.add(' ');
    
    //out.append(" // ");
    if(file.isDirectory()){ out.append('\n'); }
    out.append(dateFormat.format(new Date(date)));
    out.append(flags).append(' ');
    if(file.isDirectory()) {
      out.append("=== ").append(localDir);
      if(localDir.length() >0){ out.append('/'); }
      out.append(name).append("/ ===");
      int zDir = localDir.length() + name.length() +1;
      if(zDir < 64) { out.append("================================================================================".subSequence(0,  64-zDir)); }
    } else {
      int zName = name.length();
      if(name.contains(" ")) {
        out.append("\"").append(name).append("\"");
        zName +=2;
      } else {
        out.append(name);
      }
      if(zName < 40) { out.append(spaces.subSequence(0,  40-zName)); }
      out.append(" :: ");
      out.append(formatter.getBuffer());
    }
    //out.append(" */");
    
    /*
    int zDir = localDir.length();
    if(zDir > 40){
      out.append("...");
      out.append(localDir.subSequence(zDir-37, zDir));
      zDir = 40;
    } else {
      out.append(localDir);
    }
    int zName = name.length();
    out.append('/');
    out.append(name);
    if(file.isDirectory()){ 
      out.append("/");
      zName +=1;
    }
    int rest = 70 - zDir - zName;
    if(rest > 0){
      out.append(spaces.subSequence(0, rest));
    }
    */
    out.append("\n");
  }
  
  
  
  
  
  @SuppressWarnings("boxing")
  private void writeDirectoryLine(Writer out, File file, CharSequence localDir, String name) throws IOException
  {
    out.append("\n============================== ").append(localDir);
    if(localDir.length() >0){ out.append('/'); }
    out.append(name).append("/ ===");
    int zDir = localDir.length() + name.length() +1;
    if(zDir < 64) { out.append("================================================================================".subSequence(0,  64-zDir)); }
  
    out.append("\n");
  }
  
  
  
  @SuppressWarnings("boxing")
  private void xxxwriteOneFile(Writer out, File file, CharSequence localPath) throws IOException
  {
    long date = file.lastModified();
    date = ((date + 5000) /10000) * 10000;      //round up and down to 10 seconds, to ignore second differences
    long length = file.length();
    formatter.reset();
    formatter.addint(length, "2222'222'222'222 ");
    StringBuilder flags = new StringBuilder("       ");
    if(file.isDirectory()){ flags.setCharAt(1, 'D'); }
    if(file.canExecute()){ flags.setCharAt(2, 'x'); }
    if(!file.canRead()){ flags.setCharAt(3, 'h'); }
    if(!file.canWrite()){ flags.setCharAt(4, 'r'); }
    if(file.isHidden()){ flags.setCharAt(5, 'H'); }
    int crc = 0;
    if(args.crc && file.isDirectory()){
      formatter.add("==DIR===");
    } else {
      try{
        crcCalculator.reset();
        InputStream inp = new FileInputStream(file);
        byte[] buffer = new byte[4096];
        int bytes;
        while((bytes = inp.read(buffer)) >0){
          crcCalculator.update(buffer, 0, bytes);
        }
        inp.close();
        crc = (int)crcCalculator.getValue();
        formatter.addHex(crc, 8);
      } catch(Exception exc){
        formatter.add("????????");        
      }
    }
    formatter.add(' ');
    out.append(dateFormat.format(new Date(date)));
    out.append(flags);
    out.append(formatter.getBuffer());
    out.append(localPath);
    out.append("\n");
  }
  
  
  
  /**Static method to touch all files form a given list.
   * @param dir path to any directory.
   * @param mask Use "*" to select all files.
   * @param sFile file with local path, if given only that file will be touched, null: all files to touch.
   * @param sFilelist Name of the file list relative to the dir, can contain a relative path.
   * @throws IOException
   */
  public static void touch(String dir, String sFilelist, String sFile, Appendable out) throws IOException
  {
    FileList.Args args = new FileList.Args();
    args.out = out;
    if(sFilelist ==null ){
      int posDir = dir.lastIndexOf('/');
      int posDir2 = dir.lastIndexOf('\\');
      if(posDir2 > posDir) {
        posDir = posDir2;
      }
      args.sDirectory = dir.substring(0, posDir);
      args.sFileList = dir;  
    } else {
      args.sDirectory = dir;
      args.sFileList = dir + "/" + sFilelist;
    }
    args.crc = true;
    args.sMask = "*";
    FileList main = new FileList(args);
    main.touch(sFile);
  }
  

  /**Static method to touch all files form a given list.
   * @param dir path to any directory.
   * @param mask Use "*" to select all files.
   * @param sFilelist Name of the file list relative to the dir, can contain a relative path.
   * @throws IOException
   */
  public static void touch(String dir, String sFilelist, Appendable out) throws IOException
  { touch(dir, sFilelist, null, out); }

  
  public void touch() { touch(null); }
  
  
  public void touch(String sFile){
    BufferedReader inp = null;
    File dir = new File(args.sDirectory);
    String sDirlocal = "";
    try {
      inp = new BufferedReader(new FileReader(args.sFileList));
      String sLine;
      while((sLine = inp.readLine())!=null){
        sDirlocal = touchOneLine(sLine, dir, sDirlocal, sFile);  
      }
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    if(inp !=null) { try{ inp.close(); } catch(IOException exc){}}
  }
  
  
  private String touchOneLine(String sLine, File dir, String sDirlocal, String sFile){
    Date filetime;
    String sPath;
    String sDirlocalNew = sDirlocal;
    long listlen;
    int crclist;
    try {
      if(sLine.startsWith("===================")) {
        //a directory line
        int posStart = sLine.indexOf(' ') +1;
        int posEnd = sLine.indexOf("================", posStart) -1;  //one space before ======
        if(posStart > 0 && posEnd > posStart) {
          sDirlocalNew = sLine.substring(posStart, posEnd);
        }
      } else if(sLine.length() > 20) {
        
        filetime = (sLine.charAt(10)=='_') ? date_Format.parse(sLine) : dateFormat.parse(sLine);
        long listtime = filetime.getTime();
        
        if(sLine.charAt(30) =='\'') {
          listlen = StringFunctions_C.parseLong(sLine, 26, 16, 10, null, " '");
          crclist = StringFunctions_C.parseIntRadix(sLine, 43, 8, 16, null);
          String sPath1 = sLine.substring(53).trim();
          int zPath1 = sPath1.length();
          if(sPath1.charAt(0)=='\"' && sPath1.charAt(zPath1-1)=='\"'){ sPath1 = sPath1.substring(1, zPath1-1); }
          sPath = sDirlocal + sPath1;
        } 
        else if(StringFunctions.equals(sLine.subSequence(27,30),"===")) {
          listlen = -1;
          crclist = 0;
          sPath = null;
          int posEnd = sLine.indexOf(" ===", 31);
          if(posEnd < 0){ posEnd = sLine.length(); }
          sDirlocalNew = sLine.substring(31, posEnd);
        }
        else {
          //line with file new format
          int posEnd = sLine.indexOf("::");
          String sPath1 = sLine.substring(27, posEnd).trim();
          int zPath1 = sPath1.length();
          if(sPath1.charAt(0)=='\"' && sPath1.charAt(zPath1-1)=='\"'){ sPath1 = sPath1.substring(1, zPath1-1); }
          sPath = sDirlocal + sPath1;
          listlen = StringFunctions_C.parseLong(sLine, posEnd+2, 16, 10, null, " .");
          crclist = (int)StringFunctions_C.parseLong(sLine, posEnd+18, 11, 10, null, " ");
        }
        
        if(sPath !=null
          && (sFile == null || sFile.equals(sPath))) {  //File line found        
        
          File file = new File(dir, sPath);
          if(!file.exists()){
            if(args.out !=null) { args.out.append("FileList - touch, file not exist; ").append(sPath).append("\n"); } 
          } else if(file.isDirectory()){
            //do nothing for a directory.
          } else {
            long lastModify = file.lastModified();
            if(file.length() == listlen){
              if(Math.abs(listtime - lastModify) > 10000){ //round effect of seconds to 10!
                //the time stamp is false, the length is equal.
                //check crc to determine whether the file may be the same.
                crcCalculator.reset();
                InputStream inp = new FileInputStream(file);
                byte[] buffer = new byte[4096];
                int bytes;
                while((bytes = inp.read(buffer)) >0){
                  crcCalculator.update(buffer, 0, bytes);
                }
                inp.close();
                int crc = (int)crcCalculator.getValue();
                if(crc == crclist){
                  file.setLastModified(listtime);
                  if(args.out !=null) { args.out.append("FileList - touching; ").append(sPath).append("\n"); } 
                } else {
                  if(args.out !=null) { args.out.append("FileList - touch, file with same length is changed; ").append(sPath).append("\n"); } 
                }
              } else {
                //file may not be changed, has the correct timestamp
              }
            } else {
              if(args.out !=null) { args.out.append("FileList - touch, file is changed; ").append(sPath).append("\n"); }
            }
          }
        }
      }
      //System.out.println(filetime.toGMTString() + " " + sPath);
    } catch (IOException e) {
      e.printStackTrace();
    } catch (ParseException e) {
      e.printStackTrace();
    }
    return sDirlocalNew;
  }
  
  
  private void XXXreadOneLine(String sLine, File dir){
    Date filetime;
    String sPath;
    try {
      filetime = dateFormat.parse(sLine);
      long listtime = filetime.getTime();
      long listlen = StringFunctions_C.parseLong(sLine, 26, 16, 10, null, " '");
      sPath = sLine.substring(53);
      File file = new File(dir, sPath);
      if(!file.exists()){
        args.out.append("FileList - touch, file not exist; ").append(sPath).append("\n"); 
      } else if(file.isDirectory()){
        //do nothing for a directory.
      } else {
        long lastModify = file.lastModified();
        if(file.length() == listlen){
          if(Math.abs(listtime - lastModify) > 6000){ //round effect of seconds to 10!
            //check crc
            crcCalculator.reset();
            InputStream inp = new FileInputStream(file);
            byte[] buffer = new byte[4096];
            int bytes;
            while((bytes = inp.read(buffer)) >0){
              crcCalculator.update(buffer, 0, bytes);
            }
            inp.close();
            int crc = (int)crcCalculator.getValue();
            int crclist = StringFunctions_C.parseIntRadix(sLine, 43, 8, 16, null);
            if(crc == crclist){
              file.setLastModified(listtime);
              args.out.append("FileList - touching; ").append(sPath).append("\n"); 
            } else {
              args.out.append("FileList - touch, file with same length is changed; ").append(sPath).append("\n"); 
            }
          } else {
            //file may not be changed, has the correct timestamp
          }
        } else {
          args.out.append("FileList - touch, file is changed; ").append(sPath).append("\n"); 
        }
      }
      //System.out.println(filetime.toGMTString() + " " + sPath);
    } catch (IOException e) {
      e.printStackTrace();
    } catch (ParseException e) {
      e.printStackTrace();
    }
  }
  
  
  
  
  public static void main(String[] sArgs){
    smain(sArgs, true);
  }
  
  
  /**Invocation from another java program without exit the JVM
   * @param sArgs same like {@link #main(String[])}
   * @return "" or an error String
   */
  public static String smain(String[] sArgs){ return smain(sArgs, false); }

  
  private static String smain(String[] sArgs, boolean shouldExitVM){
    String sRet = null;
    Args args = new Args();
    CmdLine mainCmdLine = new CmdLine(args, sArgs); //the instance to parse arguments and others.
    try{
      try{ mainCmdLine.parseArguments(); }
      catch(Exception exception)
      { sRet = "Jbat - Argument error ;" + exception.getMessage();
        mainCmdLine.report(sRet, exception);
        mainCmdLine.setExitErrorLevel(MainCmd_ifc.exitWithArgumentError);
      }
      if(args.cCmd !=0){
        FileList main = new FileList(args);     //the main instance
        if(sRet == null)
        { /** The execution class knows the SampleCmdLine Main class in form of the MainCmd super class
              to hold the contact to the command line execution.
          */
          try{ 
            switch(args.cCmd){
              case 'L': main.list(); break;
              case 'T': main.touch(); break;
            }
          }
          catch(Exception exception)
          { //catch the last level of error. No error is reported direct on command line!
            sRet = "Jbat - Any internal error;" + exception.getMessage();
            mainCmdLine.report(sRet, exception);
            exception.printStackTrace(System.out);
            mainCmdLine.setExitErrorLevel(MainCmd_ifc.exitWithErrors);
          }
        }
      } else {
        mainCmdLine.writeHelpInfo(null);
      }
    } catch(Exception exc){
      sRet = exc.getMessage();
    }
    
    if(shouldExitVM) { mainCmdLine.exit(); }
    return sRet;
  }



  
  
  protected static class CmdLine extends MainCmd
  {

    public final Args argData;

    protected final MainCmd.Argument[] argList =
    { new MainCmd.Argument("", "[L|T|C|D]: List, Touch date, Compare, last files per Date"
        , new MainCmd.SetArgument(){ @Override public boolean setArgument(String val){ 
          argData.cCmd = val.charAt(0); return true;
        }})
    , new MainCmd.Argument("-l", ":<files.txt> The list file "
        , new MainCmd.SetArgument(){ @Override public boolean setArgument(String val){ 
          argData.sFileList = val; return true;
        }})
    , new MainCmd.Argument("-d", ":<directory path>  "
        , new MainCmd.SetArgument(){ @Override public boolean setArgument(String val){ 
          argData.sDirectory = val; return true;
        }})
    , new MainCmd.Argument("-m", ":<Mask> mask of files"
        , new MainCmd.SetArgument(){ @Override public boolean setArgument(String val){ 
          argData.sMask = val; return true;
        }})
    };


    protected CmdLine(Args argData, String[] sCmdlineArgs){
      super(sCmdlineArgs);
      this.argData = argData;
      super.addAboutInfo("Generating a list of files or evaluating it");
      super.addAboutInfo("made by HSchorrig, Version 1.0, 2013-08-07..2013-08-07");
      super.addArgument(argList);
      super.addStandardHelpInfo();
      
    }
    
    @Override protected void callWithoutArguments()
    { //overwrite with empty method - it is admissible.
    }

    
    @Override protected boolean checkArguments()
    {
      return true;
    } 
    
  }
  
}
