package org.vishia.util;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;

/**This class outputs to 2 destination, for example an opened file and System.out.
 * Since 2022-09: Also Closeable and Flushable, {@link Flushable#flush()} is often necessary and desired,
 * {@link Closeable#close()} is sometimes necessary. 
 * If the referred outputs are not Closeable or not Flushable, it is not a problem. 
 * But this instance expects now close() as warning also if the referred outputs does not need it. 
 * Advantage: More universal usable, especially for log and reports.
 * @author hartmut Schorrig
 *
 */
public class OutputTwice implements Appendable, Closeable, Flushable {

  private final Appendable out1, out2;
  
  /**Creates
   * <br>
   * both outputs can be null, prevent output (since 2022-09). 
   * both outputs are used also to flush() and close() if they are Closeable and Flushable.
   * @param out1 can be a simple StringBuilder, but also any Writer
   * @param out2
   */
  public OutputTwice(Appendable out1, Appendable out2) {
    this.out1 = out1; this.out2 = out2;
  }

  @Override
  public Appendable append(CharSequence csq) throws IOException {
    if(this.out1 !=null) { this.out1.append(csq); }
    if(this.out2 !=null) { this.out2.append(csq); }
    return this;
  }

  @Override
  public Appendable append(CharSequence csq, int start, int end) throws IOException {
    if(this.out1 !=null) { this.out1.append(csq, start, end); }
    if(this.out2 !=null) { this.out2.append(csq, start, end); }
    return this;
  }

  @Override
  public Appendable append(char c) throws IOException {
    if(this.out1 !=null) { this.out1.append(c); }
    if(this.out2 !=null) { this.out2.append(c); }
    return this;
  }

  @Override public void flush () throws IOException {
    if(this.out1 instanceof Flushable) { ((Flushable)this.out1).flush(); }
    if(this.out2 instanceof Flushable) { ((Flushable)this.out2).flush(); }
  }

  @Override public void close () throws IOException {
    if(this.out1 instanceof Closeable) { ((Closeable)this.out1).close(); }
    if(this.out2 instanceof Closeable) { ((Closeable)this.out2).close(); }
  }
  
  
}
