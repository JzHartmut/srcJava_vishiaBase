package org.vishia.zcmd;

import java.io.File;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.Map;
import java.util.TreeMap;

import org.vishia.util.DataAccess;

/**This class is used for test in ZBNF/examples_JZcmd/TestAll/testAll.jz.bat
 * @author Hartmut Schorrig
 *
 */
public class JZcmdUserExample
{
  public final String simpeTestFinalString = "the final simple test string.";
  
  public String simpeTestString = "the simple test string.";
  
  
  public final Map<String, DataAccess.Variable<Object>> testVariables = new TreeMap<String, DataAccess.Variable<Object>>();


  
  private final PrintWriter out;
  
  public JZcmdUserExample(Writer out){
    this.out = new PrintWriter(out);
    DataAccess.Variable<Object> var;
  }

  
  @SuppressWarnings("boxing")
  public void methodTest(float x, int y){
    out.printf("x=%f, y=%d\n", x,y);
    
  }
  
  @SuppressWarnings("boxing")
  public void methodTest(char cc){
    out.printf("char=%c\n", cc);
    
  }
  
  @SuppressWarnings("boxing")
  public void methodTest(char cc, String text){
    out.printf("char=%c, text=%s\n", cc, text);
    
  }
  
  
  public void methodTest(String text){
    out.printf("text=%s\n", text);
    
  }
  
  
  public void methodTest(File file){
    out.printf("file=%s\n", file.getPath());
    
  }
  
  

}
