package org.vishia.util.test;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.vishia.util.Assert;
import org.vishia.util.Debugutil;
import org.vishia.util.FilePath;
import org.vishia.util.StringFunctions;

public class Test_FilePath
{

  FilePath.FilePathEnvAccess env = new FilePath.FilePathEnvAccess(){

    Map<String, Object> container = new TreeMap<String, Object>();
    { fillContainerEnv();
    }
    
    void fillContainerEnv(){
      FilePath p1 = new FilePath("d:/varbase/path:varlocal/path/");
      container.put("d-base-local", p1);
      p1 = new FilePath("varlocal/path/");
      container.put("local", p1);
    }
    
    @Override public Object getValue(String variable) throws NoSuchFieldException
    { return container.get(variable); }
    
    @Override public CharSequence getCurrentDir(){ return "D:/test/currentdir"; }
  };

  
  FilePath pDriveAbsBaseLocalNameExt = new FilePath("d:/base/path:local/path/name.ext");   
  FilePath pName = new FilePath("name");   
  FilePath pNameExt = new FilePath("name.ext");   
  FilePath pLocalNameExt = new FilePath("local/path/name");   
  FilePath pRelbaseLocalNameExt = new FilePath("base/path:local/path/name.name2.ext");   
  FilePath pDriveRellocalNameExt = new FilePath("d:local/path.name");   
  FilePath pDriveAbsPathNameExt = new FilePath("d:/local/path.name");   
  FilePath pDriveRelbaseLocalNameExt = new FilePath("d:base/path:local/path.name.ext");   
  FilePath pDriveAbsbaseNameExt = new FilePath("d:/base/path:name.ext");   
  FilePath pVariable = new FilePath("&variable");   
  FilePath pVariableBaseNameExt = new FilePath("&variable/base/path:name.ext");   
  
  FilePath pVarBaseLocal_LocalNameExt = new FilePath("&d-base-local/local/path/name.ext");
  FilePath pVarBaseLocal_Base_LocalNameExt = new FilePath("&d-base-local:local/path/name.ext");
  
  FilePath pVarBaseLocal_Base_NameExt = new FilePath("&d-base-local:name.ext");

  FilePath pVar_NameExt = new FilePath("&variable/name.ext");   
  FilePath pVar_Base_LocalNameExt = new FilePath("&variable/base/path:local/path/name.ext");   

  
  @SuppressWarnings("unused")
  public void test(){
    
    showTestoper();
    StringBuilder buf = new StringBuilder();
    CharSequence file, basepath, localdir, localfile;
    try{
      localdir = pLocalNameExt.localdir(buf, null, null, env);
      basepath = pVarBaseLocal_LocalNameExt.localdir(env);
      localdir = pVarBaseLocal_LocalNameExt.localdir(env);
      localfile = pVarBaseLocal_LocalNameExt.localfile(env);
      check(StringFunctions.equals(localdir, "varlocal/path/local/path"));
      
      localdir = pVarBaseLocal_Base_LocalNameExt.localdir(env);
      check(StringFunctions.equals(localdir, "local/path"));
      
      localdir = pVarBaseLocal_Base_NameExt.localdir(env);
      check(StringFunctions.equals(localdir, "."));
      
      Debugutil.stop();
    } catch(NoSuchFieldException exc){
      System.err.println(exc.getMessage());
    }
  }

  
  
  void testExpand() throws NoSuchFieldException {
    showTestoper();
    String sWildcardPath = "D:/vishia/ZBNF/srcJava_Zbnf:org/**/*.java";
    FilePath pathWildcard = new FilePath(sWildcardPath); 
    File checkdir = new File(pathWildcard.absbasepath(null).toString());
    if(!(checkdir.exists())) {
      System.err.println("check testExpand cannot executed, change to an existing directory.");
    }
    else {
      List<FilePath> dst = new LinkedList<FilePath>();
      pathWildcard.expandFiles(dst, null, null, null);
      check(dst.size() > 100);  //should contain a lot of files, more than 300, test is ok if some files are found.
    }
  }
  
  
  void testReplWildcard() throws NoSuchFieldException {
    showTestoper();
    String sWildcardPath = "D:/vishia/ZBNF/srcJava_Zbnf:org/**/*.java";
    FilePath pathWildcard = new FilePath(sWildcardPath); 
    File checkdir = new File(pathWildcard.absbasepath(null).toString());
    if(!(checkdir.exists())) {
      System.err.println("check testExpand cannot executed, change to an existing directory.");
    }
    else {
      FilePath pathRepl = new FilePath("path/exmpl.java");
      CharSequence res = pathWildcard.absfileReplwildcard(pathRepl, null);
      check("D:/vishia/ZBNF/srcJava_Zbnf/org/path/exmpl.java".contentEquals(res));  //should contain a lot of files, more than 300, test is ok if some files are found.
    }
  }
  
  
  private void check(boolean ok) {
    if(!ok) {
      System.err.println(Assert.stackInfo("Test error in: ", 2, 1));
    }
  }
  
  private void showTestoper() {
    System.out.print(Assert.stackInfo("Test: ", 2, 1));
  }
  
  public static void main(String[] noArgs){ 
    Test_FilePath main = new Test_FilePath();
    try {
      main.testReplWildcard();
      main.testExpand();
      main.test(); 
    } catch(Exception exc) {  //all exceptions are unexpected. Not used for regular problems.
      System.err.println("unexpected: " + exc.getMessage());
      exc.printStackTrace(System.err);
    }
  }
  
}
