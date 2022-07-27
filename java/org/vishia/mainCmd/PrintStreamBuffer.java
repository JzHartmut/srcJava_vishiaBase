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

  
  /**Version, history and license.
   * <ul>
   * <li>2021-09-24 Hartmut Generally second output usable.
   * <li>2020-03-25 Hartmut creation .
   * </ul>
   * 
   * <b>Copyright/Copyleft</b>:
   * For this source the LGPL Lesser General Public License, published by the Free Software Foundation is valid.
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
   * 
   */
  static final public String version = "2021-09-15";
  

  /**This class is used for all outputs to the {@link PrintStreamAdapter} which are not
   * gathered by the overridden methods of PrintStream.
    * There should not be such methods. Therefore the write-methods is not called.
    * But the outStream should not be empty.
    * 
    */
    public final static class OutStream extends OutputStream {
     
     final Appendable out, out2;
     
     OutStream(Appendable out, Appendable out2){
       this.out = out;
       this.out2 = out2;
     }
     
     @Override
     public void write(int b) throws IOException
     { this.out.append((char)b);
       if(this.out2 !=null) { 
         this.out2.append((char)b); 
       }
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
      PrintStream outNew = new PrintStream(new OutStream(buffer, null));
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
      PrintStream outNew = new PrintStream(new OutStream(buffer, null));
      System.setErr(outNew);
      return outBefore;
    }
    
    
    
    /**Replaces the System.err for the whole JVM
     * @param buffer with this Appendable, usual a StringBuilder
     * @return the System.err before to restore after usage. 
     */
    @SuppressWarnings("resource")  
    public static PrintStream replaceSystemErr(Appendable buffer, Appendable buffer2) {
      PrintStream outBefore = System.err;
      PrintStream outNew = new PrintStream(new OutStream(buffer, buffer2));
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
