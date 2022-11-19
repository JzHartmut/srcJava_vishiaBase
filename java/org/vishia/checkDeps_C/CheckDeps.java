package org.vishia.checkDeps_C;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.vishia.mainCmd.MainCmdLoggingStream;
import org.vishia.mainCmd.MainCmdLogging_ifc;
import org.vishia.util.Arguments;
import org.vishia.util.FileFunctions;

public class CheckDeps {

  
  public CheckDependencyFile checkDepfile;
  
  public CheckDeps() {
    MainCmdLogging_ifc log = new MainCmdLoggingStream(System.out);
    this.checkDepfile = new CheckDependencyFile(log, true);
  }
  
  
  
  
  
  int exec(Args args) {
    for(String sObjExt: args.listRootObjExt) {
      this.checkDepfile.setDirObj(sObjExt);
    }
    this.checkDepfile.readCfgData(args.sFileCfg, args.currdir);
    this.checkDepfile.readDependencies(args.sFileDep);
    System.out.println("checkDeps_C: " + args.sFileDep + " read successfully");
    for(String src : args.pathsSrcGen) {
      this.checkDepfile.processSrcfile(src);
    }
    this.checkDepfile.writeDependencies();
    this.checkDepfile.close();
    
    return 0;
  }
  
  
  
  
  /**Invocation from java command line
   * Zip routine from Java made by HSchorrig, 2013-02-09 - 2020-06-09
   * <ul>
   * <li>obligate arguments: -o:ZIP.zip { INPUT}
   * </ul>
   * @param args command line arguments
   */
  public static void main(String[] args){
    int exitCode = smain(args);
    System.exit(exitCode);
  }
  
  
  
  
  
  /**Main routine able to call inside another java process without exit VM.
   * @param args cmd line args adequate {@link #main(String[])}
   * @return exit code
   */
  public static int smain(String[] cmdArgs){
    Args args = new Args();
    System.out.println(args.aboutInfo());
    try{ 
      args.parseArgs(cmdArgs, System.err);
      if(!args.testArgs(System.err)) { return 1; }
    } catch(Exception exc){
      System.err.println("ERROR CheckDeps arguments: " + exc.getMessage());
      return args.exitCodeArgError;
    }
    CheckDeps thiz = new CheckDeps();
    int ret;
    try{ 
      ret = thiz.exec(args);
    } catch(Exception exc){
      System.err.println("ERROR unexpected in CheckDeps: " + exc.getMessage());
      ret = 9; //unexpected error
    }
    return ret;
  }
  
  

  
  
  public static class Args extends Arguments {

    
    /**The currdir which is valid to search header etc. */
    public File currdir;
    
    public String sFileCfg;
    
    public List<String> pathsSrcGen = new LinkedList<String>();
    
    /**Path where all mirror-files are read and written. Command line argument -srcMirrorRoot= or -srcBuild= */
    public String sPathSrcMirrorRoot;

    /**Path where all .dep-files are read and written. Command line argument -deps= */
    //public String sPathDepsRoot;
    
    /**Possible more as one Object root directory with Object Extension.
     * Any entry should have the form "path/to/rootObj/*.o" whereby "o" is the extension for the objectfiles (may be "obj")
     */
    public List<String> listRootObjExt = new LinkedList<String>();
    
    public String sFileDep;
    
    public boolean cmpAndCpy;
    
    public boolean evalDeps;
    
    public String sPathLast;
    
    public boolean testDummyObj;

    
    
    Arguments.SetArgument setCurrdir = new Arguments.SetArgument(){ @Override public boolean setArgument(String val){ 
      final boolean bOk;
      CheckDeps.Args.this.currdir = FileFunctions.newFile(val).getAbsoluteFile();
      bOk = CheckDeps.Args.this.currdir.exists();
      return bOk;
    }};
    
    
    Arguments.SetArgument setFileCfg = new Arguments.SetArgument(){ @Override public boolean setArgument(String val){ 
      CheckDeps.Args.this.sFileCfg = val;
      return true;
    }};
    
    
    Arguments.SetArgument setPathSrcMirrorRoot = new Arguments.SetArgument(){ @Override public boolean setArgument(String val){ 
      CheckDeps.Args.this.sPathSrcMirrorRoot = val;
      return true;
    }};
    
    
    Arguments.SetArgument setPathsSrcGen = new Arguments.SetArgument(){ @Override public boolean setArgument(String val){ 
      CheckDeps.Args.this.pathsSrcGen.add(val);
      return true;
    }};
    
    
    Arguments.SetArgument setPathObj = new Arguments.SetArgument(){ @Override public boolean setArgument(String val){ 
      boolean bOk;
      int posAsterisk = val.indexOf("/*.");
      if(posAsterisk <0){
        System.err.println("argument -obj should contain a *: Syntay -obj=path/*.ext");
        bOk = false;
      } else {
        if(CheckDeps.Args.this.listRootObjExt == null) { CheckDeps.Args.this.listRootObjExt = new LinkedList<String>(); }
        CheckDeps.Args.this.listRootObjExt.add(val);
        bOk = true;
      }    
      return bOk;
    }};
    
    
    Arguments.SetArgument setFileDep = new Arguments.SetArgument(){ @Override public boolean setArgument(String val){ 
      CheckDeps.Args.this.sFileDep = val;
      return true;
    }};
    
    
    Arguments.SetArgument setCmpAndCpy = new Arguments.SetArgument(){ @Override public boolean setArgument(String val){ 
      CheckDeps.Args.this.cmpAndCpy = true;
      return true;
    }};
    
    
    Arguments.SetArgument setEvalDeps = new Arguments.SetArgument(){ @Override public boolean setArgument(String val){ 
      CheckDeps.Args.this.evalDeps = true;
      return true;
    }};
    
    
    Arguments.SetArgument setPathLast = new Arguments.SetArgument(){ @Override public boolean setArgument(String val){ 
      CheckDeps.Args.this.sPathLast = val;
      return true;
    }};
    
    
    Arguments.SetArgument setDummyObj = new Arguments.SetArgument(){ @Override public boolean setArgument(String val){ 
      CheckDeps.Args.this.testDummyObj = true;
      return true;
    }};
    
    
    Args(){
      super.aboutInfo = "Depedency checker for C/++ files, 2009-10 - 2020-07-23";
      super.helpInfo="obligate args: -cfg:CFG -depALL:DEP.txt { -obj/PATH/*.OBJ } { -src:SOURCES}";  //[-w[+|-|0]]
      addArg(new Argument("-currdir", ":path/to/currdir For all actions with relative path", this.setCurrdir));
      addArg(new Argument("-src", ":path/to/src A source file to check, more as one possible", this.setPathsSrcGen));
      addArg(new Argument("-srcBuild", ":path/to/rootMirror root directory for mirror sources to compare ...", this.setPathSrcMirrorRoot));
      addArg(new Argument("-srcMirrorRoot", ":path/to/rootMirror       ... whether only comments are changed", this.setPathsSrcGen));
      addArg(new Argument("-obj", ":path/to/rootObj/*.obj root directory to Objectfiles and extension for Obj", this.setPathObj));
      addArg(new Argument("-cfg", ":path/to/CheckDeps.cfg Configuration file, write ? for help", this.setFileCfg));
      addArg(new Argument("-depAll", ":path/to/MyProject.deps - file contains existing, dependencies, will be written or replaced", this.setFileDep));
      addArg(new Argument("cmpAndCpy", " command to .... TODO", this.setCmpAndCpy));
      addArg(new Argument("evalDeps", " command to only evaluation dependencies", this.setEvalDeps));
      addArg(new Argument("-lastPath", ":path/to/TODO", this.setPathLast));
      addArg(new Argument("-testDummyObj", " option for DummyObject", this.setDummyObj));
         
    }

    
    
    
    @Override
    public boolean testArgs(Appendable msg) throws IOException {
      boolean bOk = true;
      if(this.listRootObjExt.size() ==0) {
        msg.append("-obj:path/to/rootObj/*.o should given at least one time.");
        bOk = false;
      }
      // TODO Auto-generated method stub
      return bOk;
    }
  
  
  
  }
}
