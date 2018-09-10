package org.vishia.util;

import java.io.IOException;
import java.io.OutputStream;

/**This class combines an Appendable with the capability of {@link StringPartScan}.
 * All append methods sets the endMax of the StringPart to the new length. The current part end
 * will be set to endMax only if is on endMax before append.
 * @author Hartmut Schorrig
 *
 */
public class StringPartAppend extends StringPartScan implements Appendable
{
  
  /**Version, history and license.
   * <ul>
   * <li>2014-01-13 Hartmut new: {@link #outputStream()}, able to use as {@link OutputStream} 
   *   especially for {@link javax.tools.Tool#run(java.io.InputStream, OutputStream, OutputStream, String...)}
   *   or such other. 
   * <li>2014-01-13 Hartmut created: It is the idea to combine the StringPart to analyze Strings
   *   with an Appendable, especially a StringBuilder.   
   * </ul>
   * 
   * <b>Copyright/Copyleft</b>:
   * For this source the LGPL Lesser General Public License,
   * published by the Free Software Foundation is valid.
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
  //@SuppressWarnings("hiding")
  static final public String sVersion = "2014-01-12";

  
  private OutputStream outputStream;
  
  
  /**Creates an String jar with 1000 character (default size). Not that the size of the internal used
   * {@link java.lang.StringBuilder} will be increased if necessary. */
  public StringPartAppend() {
    super(new StringBuilder(1000));
  }

  
  /**Creates an String jar with a given character size. */
  public StringPartAppend(int size) {
    super(new StringBuilder(size));
  }

  
  /**Returns the internal StringBuilder to use methods of that.
   * Note that a change of content may need invocation of {@link #seekBegin()} and {@link #setLengthMax()}
   * or {@link #setParttoMax()} of the returned stringBuilder().
   * Use the following schema: <pre>
   *   StringBuilder myBuffer = myStringPartAppend.buffer();
   *   myBuffer.insert("do anything with it");
   *   myStringPartAppend.setParttoMax();  
   * </pre>  
   * @return the internal used StringBuilder instance.
   */
  public StringBuilder buffer(){ return (StringBuilder)content; }
  
  
  
  /**Clears the content of the StringBuilder and resets all StringPart length.
   * 
   */
  public void clear() { 
    ((StringBuilder)content).setLength(0); 
    assign(content); //assigns the own content to set all positions to 0.
  }

  
  
  
  @Override public Appendable append(CharSequence csq) throws IOException
  { ((StringBuilder)content).append(csq);
    if(end == endMax){
      end = endMax = content.length();
    } else {
      endMax = content.length();
    }
    return this;
  }

  
  
  
  @Override public Appendable append(char c) throws IOException
  { ((StringBuilder)content).append(c);
    if(end == endMax){
      end = endMax = content.length();
    } else {
      endMax = content.length();
    }
    return this;
  }

  
  
  
  @Override public Appendable append(CharSequence csq, int from, int to) throws IOException
  { ((StringBuilder)content).append(csq, from, to);
    if(end == endMax){
      end = endMax = content.length();
    } else {
      endMax = content.length();
    }
    return this;
  }


  
  
  /**Creates or returns an instance which handles this class as OutputStream.
   * The first call creates the managing instance. Any second call returns the reference to it.
   * It is only an adaption instance without own data.
   * @return Access to this as OutputStream.
   */
  public OutputStream outputStream(){ 
    if(outputStream == null) { outputStream = new OutputStream_StringPartAppend(); }
    return outputStream;
  }
  
  
  
  
  private class OutputStream_StringPartAppend extends OutputStream
  {

    @Override public void write(byte b[], int off, int len) throws IOException {
      String out = new String(b, off, len);
      StringPartAppend.this.append(out);
    }
    
    
    
    @Override public void write(int b) throws IOException
    {
      //TODO works correct only with ASCII
      StringPartAppend.this.append((char)b);
    }
    
    @Override public void flush() throws IOException {
    }

    @Override public void close() throws IOException {
    }

    
  }

  
  
}
