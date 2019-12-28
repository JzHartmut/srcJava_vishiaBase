package org.vishia.charset;


/**Classes to convert UTF-16 to 8-bit-codings.
 * The implementation of {@link java.nio.charset.Charset} etc. may be over engineered, 
 * An important feature: report non-map-able characters, is missing.
 * The conversion between the ISO-8859-1 code page and UTF16 is very simple because it is the low part
 * of UTF-16. All other code pages needs three tables with binary search for encoding, see {@link ISO8859_2} etc.  
 * @author Hartmut Schorrig
 *
 */
public class ISO8859_1 implements CodeCharset {

  
  @Override public int getCode(char cc) {
    if(cc < 256) return (int)cc;
    else return 0;
  }

  @Override public char getChar(int code) {
    return (char)code;
  }
}
