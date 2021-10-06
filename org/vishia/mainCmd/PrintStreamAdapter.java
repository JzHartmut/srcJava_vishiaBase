package org.vishia.mainCmd;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

/**Simple adapter between given Appendable to required PrintStream.
 * It is especially to set 
 * <pre>
 * System.setOut(new PrintStreamAdapter("text at line begin", myAppendable));
 * </pre>
 * @author hartmut
 *
 */
public class PrintStreamAdapter extends PrintStream
{
     /**This class is used for all outputs to the {@link PrintStreamAdapter} which are not
   * gathered by the overridden methods of PrintStream.
   * There should not be such methods. Therefore the write-methods is not called.
   * But the outStream should not be empty.
   * 
   */
  final static class OutStream extends OutputStream{
    
    final Appendable out;
    
    OutStream(Appendable out){
      this.out = out;
    }
    
    @Override
    public void write(int b) throws IOException
    { out.append((char)b);
    }
  }; //outStream 
  

  
   final String pre;
    
   final Appendable outAppend;
   
    
   @SuppressWarnings("resource") //the argument of PrintStream(arg) will be closed on PrintStream.close.
   public PrintStreamAdapter(String pre, Appendable outAppend) {
      super(new OutStream(outAppend));  //the argument of PrintStream(arg) will be closed on PrintStream.close.
      this.outAppend = outAppend;
      this.pre = pre;
    }
    
    
    @Override public void print(String str){
      append(str);
    }
    
    @Override
    public PrintStream append(CharSequence csq, int start, int end) {
      CharSequence cs = (csq == null ? "null" : csq);
      append(cs.subSequence(start, end).toString());
      return this;
    }

    
    /**This method is called if {@link java.io.OutputStream#append} was invoked
     * @see java.io.PrintStream#print(java.lang.String)
     */
    @Override public PrintStream append(CharSequence s) { 
      try{ outAppend.append(s); } catch (IOException exc){}
      return this;
    }
    
    /**The println method is used usually. 
     * 
     * @see java.io.PrintStream#println(java.lang.String)
     */
    @Override public void println(Object o) { 
      try{ outAppend.append(o.toString()).append("\n"); } catch (IOException exc){}
    }

    
    /**The println method is used usually. 
     * 
     * @see java.io.PrintStream#println(java.lang.String)
     */
    @Override public void println(String s) { 
      try{ outAppend.append(s).append("\n"); } catch (IOException exc){}
    }

    @Override public PrintStream printf(String s, Object... args) { 
      try{ outAppend.append(s).append("TODO args").append("\n"); } catch (IOException exc){}
      return this;
    }
    
    
    @Override public void close() { super.close();} //closes the PrintStread of ctor too.
  
}
