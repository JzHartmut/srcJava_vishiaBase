package org.vishia.util;

import java.io.Flushable;
import java.io.IOException;
import java.io.Writer;

/**This class creates a java.io.Writer instance with a given Appendable.
 * <ul>
 * <li>All write operations result to an output of the characters to the {@link Appendable#append(char)}.
 * <li>The operations {@link #flush()} flushes if the {@link Writer_Appendable#Writer_Appendable(Appendable, Flushable)}
 *   was created with a flush instances. This instances may/should be the same as the Appendable instance.
 *   If a flush instance is not given, flush has no effect. 
 * <li>The operations {@link #close()} flushes and closes this instances. It does not close any other one 
 *   because a {@link java.io.Closeable} is not associated here. To close the Appendable close it directly.
 * </ul>   
 * @author Hartmut Schorrig
 * @since 2015-07-15
 * LPGL-license.
 *
 */
public class Writer_Appendable extends Writer
{

  private Appendable app; 
  
  private final Flushable flush;

  public Writer_Appendable(Appendable app)
  { this.app = app;
    this.flush = null;
  }

  public Writer_Appendable(Appendable app, Flushable flush)
  { this.app = app;
    this.flush = flush;
  }

  
  @Override public void write(char[] cbuf, int off, int len) throws IOException
  { if(app !=null) {
      for(int ii=0; ii< len; ++ii){
        app.append(cbuf[off+ii]);
      }
    }    
  }

  @Override public void flush() throws IOException
  { if(flush !=null) {
      flush.flush();
    }
  }

  @Override public void close() throws IOException
  { flush();
    app = null;
  }
  
}
