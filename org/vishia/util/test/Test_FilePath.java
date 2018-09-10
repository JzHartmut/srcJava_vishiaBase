package org.vishia.util.test;

import java.util.Map;
import java.util.TreeMap;

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
    
    StringBuilder buf = new StringBuilder();
    CharSequence file, basepath, localdir, localfile;
    try{
      localdir = pLocalNameExt.localdir(buf, null, null, env);
      basepath = pVarBaseLocal_LocalNameExt.localdir(env);
      localdir = pVarBaseLocal_LocalNameExt.localdir(env);
      localfile = pVarBaseLocal_LocalNameExt.localfile(env);
      assert(StringFunctions.equals(localdir, "varlocal/path/local/path"));
      
      localdir = pVarBaseLocal_Base_LocalNameExt.localdir(env);
      assert(StringFunctions.equals(localdir, "local/path"));
      
      localdir = pVarBaseLocal_Base_NameExt.localdir(env);
      assert(StringFunctions.equals(localdir, "."));
      
      Debugutil.stop();
    } catch(NoSuchFieldException exc){
      System.out.println(exc.getMessage());
    }
  }

  
  public static void main(String[] noArgs){ 
    Test_FilePath main = new Test_FilePath();
    main.test(); 
  }
  
}
