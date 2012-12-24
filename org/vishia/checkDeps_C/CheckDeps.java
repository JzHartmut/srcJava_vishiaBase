package org.vishia.checkDeps_C;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;

import org.vishia.mainCmd.MainCmd;
import org.vishia.mainCmd.Report;
import org.vishia.util.FileSystem;


/**This is the class for command line invocation for a dependency checker for C-files.
 * @author Hartmut Schorrig
 *
 */
public class CheckDeps
{

  /**Version, history and license.
   * <ul>
   * <li>2012-12-25 Hartmut new: Inserted in the Zbnf component because it is an integral part of the Zmake concept
   *   for C-compilation.
   * <li>2011-05-00 Hartmut created: It was necessary for C-compilation to check real dependencies in a fast way.
   *   The dependency check from a GNU compiler was to slow.
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
   * <li> But the LPGL ist not appropriate for a whole software product,
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
  public static final int version = 20121225;

  
  public static class Args
  {
    public String sFileCfg;
    
    public List<String> pathsSrcGen = new LinkedList<String>();
    
    /**Path where all mirror-files are read and written. Command line argument -srcMirrorRoot= or -srcBuild= */
    public String sPathSrcMirrorRoot;

    /**Path where all .dep-files are read and written. Command line argument -deps= */
    public String sPathDepsRoot;
    
    public String sPathObj;
    
    public String sObjExt;

    public String sFileDep;
    
    public boolean cmpAndCpy;
    
    public boolean evalDeps;
    
    public String sPathLast;
    
    public boolean testDummyObj;
  }
  

  /**Inner Class to handle the command line invocation and the outputs to console. */
  private static class Cmdline extends MainCmd
  {
    /**The only one dependency to the main class: The arguments. */
    private final Args args;
    
    /**Checks whether all arguments are matching together, 
     * invoked after processing all command line arguments.
     * @see org.vishia.mainCmd.MainCmd#checkArguments()
     */
    @Override public boolean checkArguments() {
      boolean ok = true;
      if(args.sFileCfg == null){ ok = false; }
      return ok;
    }
  
    /**Tests the command line arguments. */
    @Override public boolean testArgument(String argc, int nArg) throws ParseException {
      boolean bOk = true;  //set to false if the argc is not passed
      if(argc.startsWith("-src="))          { args.pathsSrcGen.add(getArgument(5)); }
      else if(argc.startsWith("-srcBuild="))     { args.sPathSrcMirrorRoot  = getArgument(10); }
      else if(argc.startsWith("-srcMirrorRoot=")){ args.sPathSrcMirrorRoot  = getArgument(15); }
      else if(argc.startsWith("-deps="))         { args.sPathDepsRoot  = getArgument(6); }
      else if(argc.startsWith("-obj=")){ 
        String sArg = getArgument(5);
        int posAsterisk = sArg.indexOf("*");
        if(posAsterisk <0){
          writeError("argument -obj should contain a *: Syntay -obj=path/*.ext");
          bOk = false;
        } else {
          args.sPathObj = sArg.substring(0, posAsterisk);
          args.sObjExt = sArg.substring(posAsterisk +1);
        }
      }
      else if(argc.startsWith("-cfg=")) args.sFileCfg  = getArgument(5);
      else if(argc.startsWith("-depAll=")) args.sFileDep  = getArgument(8);
      else if(argc.startsWith("cmpAndCpy")) args.cmpAndCpy = true;
      else if(argc.startsWith("evalDeps")) args.evalDeps = true;
      else if(argc.startsWith("-lastPath=")) args.sPathLast = getArgument(10);
      else if(argc.startsWith("-testDummyObj")) args.testDummyObj = true;
      else bOk=false;
      return bOk;
    }

    Cmdline(Args args){
      this.args = args;
    }
    
  }
  
  
  /**List of all source files. */
  List<File> listFilesSrcAll = new LinkedList<File>();
  
  /**Data from the config file. Composition. */
  final CfgData cfgData;
  
  /**Data from arguments -src=SRCPATH. Composition. */
  final CheckData checkData;
  
  /**Aggregation to arguments. It are instantiated outside. */
  private final Args args;
  
  /**Aggregation to any console or log output. */
  private final Report console;

  CheckDependencyFile checkerDependencyFile;
  
  CheckAllDepFile readerInputDepFile;
  
  /**Constructor of this class. */
  public CheckDeps(Report console, Args args, CfgData cfgData) {
    super();
    this.console = console;
    this.args = args;
    this.cfgData = cfgData;
    //
    //create directory-File-instance for the srcBuid if the argument -srcBuild= is given.
    final File dirSrcMirror;
    if(args.sPathSrcMirrorRoot !=null){
      String sDirSrcBuild = args.sPathSrcMirrorRoot;
      dirSrcMirror = new File(sDirSrcBuild);
      try{ FileSystem.mkDirPath(sDirSrcBuild + "/"); }  //assert that the directory exists. A build file can written into.
      catch(IOException exc){ throw new IllegalArgumentException("CheckDeps - Problem with path; " + sDirSrcBuild); }
    } else {
      dirSrcMirror = null;  //not given.
    }
    final File dirDepsRoot;
    if(args.sPathDepsRoot !=null){
      String sDir = args.sPathDepsRoot;
      dirDepsRoot = new File(sDir);
      try{ FileSystem.mkDirPath(sDir + "/"); }  //assert that the directory exists. A build file can written into.
      catch(IOException exc){ throw new IllegalArgumentException("CheckDeps - Problem with path; " + sDir); }
    } else {
      dirDepsRoot = null;  //not given.
    }
    //
    //create directory-File-instance for the main directories: obj
    final String sDirObj = args.sPathObj;
    File dirObj = new File(sDirObj);
    try{ FileSystem.mkDirPath(sDirObj + "/");
    } catch(IOException exc){
      throw new IllegalArgumentException("CheckDeps_C - argument error; directory for objects not found and not able to create: " + dirObj.getAbsolutePath());
    }
    if(!dirObj.isDirectory()) throw new IllegalArgumentException("CheckDeps_C - directory for build-sources have to be a directory: " + args.sPathSrcMirrorRoot); 
    //
    //open file for depAll:
    Writer writerDepAll = null;
    if(args.sFileDep !=null){
      File fileDep = new File(args.sFileDep + ".old");
      try{
        FileSystem.mkDirPath(fileDep);
        writerDepAll = new FileWriter(fileDep);
      } catch(IOException exc){
        throw new IllegalArgumentException("CheckDeps_C - argument error; cannot create -depAll=" + args.sFileDep);
      }
    }
    this.checkData = new CheckData(dirSrcMirror, dirObj, dirDepsRoot, writerDepAll);
    //checker for all files:
    checkerDependencyFile = new CheckDependencyFile(cfgData, checkData, console);
    this.readerInputDepFile = new CheckAllDepFile(checkerDependencyFile, console, checkData);
  }
  

  
  /**The main routine invoke-able from command line.
   * @param argsP The command line arguments
   */
  public static void main(String[] argsP){
    Args args = new Args();
    Cmdline cmdline = new Cmdline(args);
    cmdline.writeInfoln("CheckDeps " + DateTime.sDateBuild_CheckDeps + " - checks and builds Dependencies, deletes Objects");
    try{ cmdline.parseArguments(argsP);
    
    } catch(ParseException exc){
      cmdline.writeError("cmd line arguments: ", exc);
    }
    
    ParserConfigFile parserCfg = new ParserConfigFile(cmdline);
    CfgData cfgData = null;
    try{ cfgData = parserCfg.parseConfigFile(args.sFileCfg); }
    catch(IOException exc){cmdline.writeError("Configfile error; ", exc);
    }
    
    if(cfgData !=null){
      CheckDeps main = new CheckDeps(cmdline, args, cfgData);
      try{ main.execute(); } 
      catch(Exception exc){ cmdline.writeError("Any execution error; ", exc); }
    }
  }
  
  /**Main execution routine of this class. It processes all given source files 
   * with the method {@link #processSrcTree().
   * That method will be called recursively depending on the include-lines in a changed and tested
   * source file or depending on the content of the *.dep-file if the source file isn't newer
   * as the object. It may happen that a included file is newer.
   */
  private void execute()
  {
    File dirSrcMirror = new File(args.sPathSrcMirrorRoot);
    if(!dirSrcMirror.exists()) throw new IllegalArgumentException("CheckDeps_C - directory for build-sources not found: " + dirSrcMirror.getAbsolutePath());
    if(!dirSrcMirror.isDirectory()) throw new IllegalArgumentException("CheckDeps_C - directory for build-sources have to be a directory: " + dirSrcMirror.getAbsolutePath()); 
    
    if(args.sFileDep !=null){
       readerInputDepFile.readDepFile(args.sFileDep);
    }
    //List<InputSrc> listSrcDirs = new LinkedList<InputSrc>();
    
    for(String sPathSrc : args.pathsSrcGen){
      InputSrc srcInfo = new InputSrc(sPathSrc);
      cfgData.listProcessPaths.add(srcInfo);  //store for usage immediately
      cfgData.listSourcePaths.add(srcInfo);
      //checkData.sPathsSrc.add(srcInfo);  //to detect whether a file is in the source pool.
    }
 
    for(InputSrc srcDir: cfgData.listProcessPaths){

      int nrofFiles = gatherSrcFiles(srcDir);
      console.reportln(Report.info, "source file path; " + srcDir.sCanonicalPathSrc + "; nrof files=" + nrofFiles);
    }
    processSrcTree();
    
    InfoFileDependencies.writeAllBackDeps(args.sFileDep, checkData.indexAllInclFilesAbsPath);

    if(args.evalDeps){
      //writeDependencies(new File);
    }
    console.reportln(Report.info, "cmdAndCpy - srcFiles= " + checkData.nrofSrcFiles 
      + ", newDeps= " + checkData.nrofNewDeps
      + ", changedFiles= " + checkData.nrofChangedFiles
      + ", del-obj= " + checkData.nrofDelObj
      + ", re-compile= " + checkData.nrofRecompilings
      + ", newFiles= " + checkData.nrofNewFiles 
      + ", extInclFiles= " + checkData.nrofExtInclFiles); 
    console.reportln(Report.info, ""); 
  }


  
  
  /**Gather all source files.
   * @param srcDir
   */
  private int gatherSrcFiles(InputSrc srcDir)
  {
    List<File> listFilesSrcC = new LinkedList<File>();
    //gather all source files.
    try{
      FileSystem.addFileToList(srcDir.dirSrcBase, "**/*.cpp" , listFilesSrcC);
      FileSystem.addFileToList(srcDir.dirSrcBase, "**/*.c" , listFilesSrcC);
      FileSystem.addFileToList(srcDir.dirSrcBase, "**/*.C" , listFilesSrcC);
      FileSystem.addFileToList(srcDir.dirSrcBase, "**/*.s" , listFilesSrcC);
      FileSystem.addFileToList(srcDir.dirSrcBase, "**/*.S" , listFilesSrcC);
      //FileSystem.addFileToList(srcDir.dirSrc, "**/*.h" , listFilesSrcAll);
      //FileSystem.addFileToList(srcDir.dirSrc, "**/*.H" , listFilesSrcAll);
      listFilesSrcAll.addAll(listFilesSrcC);  //complete all list with c list
    } catch(FileNotFoundException exc){
      console.writeError("File not found", exc);
    }
    //write report of all files:
    if(console.getReportLevel() >=Report.debug){
      for(File fileSrc: listFilesSrcAll){
        String sFileSrcAbs = fileSrc.getAbsolutePath();
        console.reportln(Report.debug, "gather source file; " + sFileSrcAbs);
      }
    }
    return listFilesSrcC.size(); 
  }
  
  



  /**It processes all given source files of one argument -src=SRCPATH. 
   * That method will be called recursively depending on the include-lines in a changed and tested
   * source file or depending on the content of the *.dep-file if the source file isn't newer
   * as the object. It may happen that a included file is newer.
   */
  private void processSrcTree()
  {
    //process all source files:
    //The source files maybe generated from a tool as secondary sources: 
    for(File fileSrc: listFilesSrcAll){
      checkData.nrofSrcFiles +=1;
      String sFileSrcAbs = FileSystem.getCanonicalPath(fileSrc);
      console.reportln(Report.debug, "CheckDeps - check source file; " + sFileSrcAbs);
      if(sFileSrcAbs.contains("ObjectJc.h"))
        stop();
      InfoFileDependencies infoDepsPrimary;
      final ObjectFileDeps objDeps;
      if( (infoDepsPrimary = checkData.indexSrcFilesAbs.get(sFileSrcAbs))!=null){
        console.report(Report.debug, " : is processed already.");
        objDeps = null;
      } else {
        //build the local pathName inside the build- and object-directory.
        //It is the given local name from -src=SRCPATH + the local path inside the SRCPATH
        String sLocalPathName = cfgData.checkIsInSourcePool(sFileSrcAbs);
        if(sLocalPathName == null){
          console.reportln(Report.error, "CheckDeps - error check source file - sLocalPathName not found in config; " + sFileSrcAbs);
        } else {
          int posExt = sLocalPathName.lastIndexOf('.');
          String sExt = sLocalPathName.substring(posExt+1);
          //
          if(sExt.startsWith("c") || sExt.startsWith("C") || sExt.equals("s") || sExt.equals("S")){
            objDeps = new ObjectFileDeps(checkData.dirObjRoot, sLocalPathName, args.sObjExt); 
            objDeps.createObjDir(console);
          } else {
            objDeps = null;
          }
          //
          //process the source file:
          //
          infoDepsPrimary = checkerDependencyFile.processSrcfile(fileSrc, objDeps, 0);
          if(objDeps.isObjDeleted()){ 
            checkData.nrofDelObj +=1; 
          }
          if(objDeps.isObjRecompile()){ 
            checkData.nrofRecompilings +=1; 
          }
        }
      }
    }//for all source files
    
  }
  
  

  void stop()
  {
  }



}
