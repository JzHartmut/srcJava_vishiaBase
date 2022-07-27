package org.vishia.charset;

import java.nio.charset.Charset;

public class ASCII7bit implements CodeCharset {
  
  
  Charset charset = Charset.forName("US-ASCII");

  @Override public int getCode(char cc) {
    if(cc < 128) return (int)cc;
    else return 0;
  }

  @Override public char getChar(int code) {
    return (char)code;
  }

  @Override
  public Charset getCharset() {
    return this.charset;
  }
}
