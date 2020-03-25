package org.vishia.mainCmd;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

/**This class contains three operations to replace {@link System#out} {@link System#err} and {@link System#in}
 * with Appendable respectively CharSequence for in. It is especially for test.
 * <br>
 * The nested classes {@link InStream} and {@link OutStream} are the links between InputStream and CharSequence
 * respectively PrintStream and Appendable, which are not contained in the Java core capabilities.
 * 
 * @author Hartmut Schorrig, LPGL license or second specific license
 * @since 2020-03-24
 *
 */
public class PrintStreamBuffer {

  
  

  /**This class is used for all outputs to the {@link PrintStreamAdapter} which are not
   * gathered by the overridden methods of PrintStream.
    * There should not be such methods. Therefore the write-methods is not called.
    * But the outStream should not be empty.
    * 
    */
    public final static class OutStream extends OutputStream {
     
     final Appendable out;
     
     OutStream(Appendable out){
       this.out = out;
     }
     
     @Override
     public void write(int b) throws IOException
     { this.out.append((char)b);
     }
    }; //outStream 

    
    
    
    
    public final static class InStream extends InputStream {

      final CharSequence cs;
      
      int pos;
      
      InStream(CharSequence cs){
        this.cs = cs;
      }
      
      @Override
      public int read() throws IOException {
        int zs = this.cs.length();  //current length, can be changed
        if(this.pos >= zs) return -1;  //end of stream
        else return this.cs.charAt(this.pos++);
      }
      
    }
    
    
    /**Replaces the System.out for the whole JVM
     * @param buffer with this Appendable, usual a StringBuilder
     * @return the System.out before to restore after usage. 
     */
    @SuppressWarnings("resource")
    public static PrintStream replaceSystemOut(Appendable buffer) {
      PrintStream outBefore = System.out;
      PrintStream outNew = new PrintStream(new OutStream(buffer));
      System.setOut(outNew);
      return outBefore;
    }
    
    
    /**Replaces the System.err for the whole JVM
     * @param buffer with this Appendable, usual a StringBuilder
     * @return the System.err before to restore after usage. 
     */
    @SuppressWarnings("resource")  
    public static PrintStream replaceSystemErr(Appendable buffer) {
      PrintStream outBefore = System.err;
      PrintStream outNew = new PrintStream(new OutStream(buffer));
      System.setErr(outNew);
      return outBefore;
    }
    
    
    
    /**Replaces the System.in for the whole JVM
     * @param buffer with this CharSequence, usual a String or a StringBuilder
     * @return the System.in before to restore after usage. 
     */
    @SuppressWarnings("resource")  
    public static InputStream replaceSystemIn(CharSequence buffer) {
      InputStream inBefore = System.in;
      InputStream inNew = new InStream(buffer);
      System.setIn(inNew);
      return inBefore;
    }
    
    

}
