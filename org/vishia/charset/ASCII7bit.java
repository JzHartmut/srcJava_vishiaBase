package org.vishia.charset;

public class ASCII7bit implements CodeCharset {
  
  
  
  @Override public int getCode(char cc) {
    if(cc < 128) return (int)cc;
    else return 0;
  }

  @Override public char getChar(int code) {
    return (char)code;
  }

}
