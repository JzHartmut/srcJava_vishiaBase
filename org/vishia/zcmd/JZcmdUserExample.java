package org.vishia.zcmd;

import java.io.File;
import java.io.IOException;
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

  private int sum;
  
  //private final PrintWriter out;
  
  private final Appendable out2;
  
  public JZcmdUserExample(Appendable out){
    //this.out = new PrintWriter(out);
    this.out2 = out;
    DataAccess.Variable<Object> var;
  }

  
  @SuppressWarnings("boxing")
  public void methodTest(float x, int y) throws IOException{
    out2.append(String.format("x=%f, y=%d\n", x,y));
    //out.printf("x=%f, y=%d\n", x,y);
    
  }
  
  @SuppressWarnings("boxing")
  public void methodTest(char cc) throws IOException{
    out2.append(String.format("char=%c\n", cc));
    
  }
  
  @SuppressWarnings("boxing")
  public void methodTest(char cc, String text) throws IOException{
    out2.append(String.format("char=%c, text=%s\n", cc, text));
    
  }
  
  
  public void methodTest(String text) throws IOException{
    out2.append(String.format("text=%s\n", text));
    
  }
  
  
  public void methodTest(File file) throws IOException{
    out2.append(String.format("file=%s\n", file.getPath()));
    
  }
  

  
  void testCalctimeMethodInvocation(int x) {
    sum +=x; 
  }
  

}
