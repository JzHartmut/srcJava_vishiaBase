package org.vishia.util;

import java.io.Flushable;
import java.io.Closeable;
import java.io.IOException;
import java.io.Writer;

/**This class creates a java.io.Writer instance with a given Appendable.
 * <ul>
 * <li>All write operations result to an output of the characters to the {@link Appendable#append(char)}.
 * <li>The operations {@link #flush()} flushes if the {@link Writer_Appendable#Writer_Appendable(Appendable, Flushable)}
 *   was created with a flush instances. This instances may/should be the same as the Appendable instance.
 *   If a flush instance is not given, flush has no effect. 
 * <li>The operations {@link #close()} flushes and closes this instances. It closes the Flushable instance if it is Closeable
 * </ul>   
 * @author Hartmut Schorrig
 * @since 2015-07-15
 * LPGL-license.
 *
 */
public class Writer_Appendable extends Writer
{

  /**Version, history and license.
   * <ul>
   * <li>2022-02-07 Hartmut: Enhanced  with close and compatibility with a StringBuilder.
   * <li>20xx: Created.
   * </ul> 
   */
  public static final String sVersion = "2022-02-07";

  
  
  
  private Appendable app; 
  
  private final Flushable flush;

  /**Instance which does not support flush(), but flush() can be called
   * @param app
   */
  public Writer_Appendable(Appendable app)
  { this.app = app;
    this.flush = null;
  }

  /**Instance which supports a maybe specific flush
   * @param app Can be the same as flush, if it is a Writer
   * @param flush same as app or specific other.
   */
  public Writer_Appendable(Appendable app, Flushable flush)
  { this.app = app;
    this.flush = flush;
  }

  
  @Override public void write(char[] cbuf, int off, int len) throws IOException
  { if(this.app !=null) {
      if(this.app instanceof Writer) {
        ((Writer)this.app).write(cbuf, off, len);  //delegate.
      } 
      else {
        for(int ii=0; ii< len; ++ii){
          this.app.append(cbuf[off+ii]);
        }
      }
    }    
  }

  
  /**If the Appendable is also a CharSequence, such as StringBuilder, then return it.
   * @return
   */
  public CharSequence getContent() {
    if(this.app instanceof CharSequence) {
      return (CharSequence)this.app;
    }
    else return null;
  }
  
  /**If the Appendable is also a CharSequence, such as StringBuilder, then return it.
   * @return
   */
  public void clear() {
    if(this.app instanceof StringBuilder) {
      ((StringBuilder)this.app).setLength(0);
    }
  }
  
  @Override public void flush() throws IOException
  { if(this.flush !=null) {
      this.flush.flush();
    }
  }

  @Override public void close() throws IOException
  { flush();
    if(this.flush instanceof Closeable) {
      ((Closeable)this.flush).close();
    }
    this.app = null;
  }
  
}
