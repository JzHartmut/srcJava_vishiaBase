package org.vishia.zcmd;

import java.io.File;
import java.util.Map;
import java.util.TreeMap;

import org.vishia.util.DataAccess;

public class JZcmdUserExample
{
  public final String simpeTestFinalString = "the final simple test string.";
  
  public String simpeTestString = "the simple test string.";
  
  
  public final Map<String, DataAccess.Variable<Object>> testVariables = new TreeMap<String, DataAccess.Variable<Object>>();


  public JZcmdUserExample(){
    DataAccess.Variable<Object> var;
  }

  
  @SuppressWarnings("boxing")
  public void methodTest(float x, int y){
    System.out.printf("x=%f, y=%d\n", x,y);
    
  }
  
  @SuppressWarnings("boxing")
  public void methodTest(char cc){
    System.out.printf("char=%c\n", cc);
    
  }
  
  @SuppressWarnings("boxing")
  public void methodTest(char cc, String text){
    System.out.printf("char=%c, text=%s\n", cc, text);
    
  }
  
  
  public void methodTest(String text){
    System.out.printf("text=%s\n", text);
    
  }
  
  
  public void methodTest(File file){
    System.out.printf("file=%s\n", file.getPath());
    
  }
  
  

}
