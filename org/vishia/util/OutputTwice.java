package org.vishia.util;

import java.io.IOException;

/**This class outputs to 2 destination, for example an opened file and System.out.
 * @author hartmut Schorrig
 *
 */
public class OutputTwice implements Appendable {

  private final Appendable out1, out2;
  
  public OutputTwice(Appendable out1, Appendable out2) {
    this.out1 = out1; this.out2 = out2;
  }

  @Override
  public Appendable append(CharSequence csq) throws IOException {
    this.out1.append(csq); this.out2.append(csq);
    return this;
  }

  @Override
  public Appendable append(CharSequence csq, int start, int end) throws IOException {
    this.out1.append(csq, start, end); this.out2.append(csq, start, end);
    return this;
  }

  @Override
  public Appendable append(char c) throws IOException {
    this.out1.append(c); this.out2.append(c);
    return this;
  }
  
  
}
