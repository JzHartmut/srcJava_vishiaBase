package org.vishia.charset;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.TreeMap;

/**Interface to convert UTF-16 to 8-bit-codings.
 * The implementation of {@link java.nio.charset.Charset} etc. may be over engineered, 
 * An important feature: report non-map-able characters, is missing.
 * The conversion between the ISO-8859-1 code page and UTF16 is very simple because it is the low part
 * of UTF-16, see {@link ISO8859_1}. 
 * All other code pages needs three tables with binary search for encoding, see {@link ISO8859_2} etc.  
 * In the moment only ISO8859-1 is supported.
 * 
 * @author Hartmut Schorrig
 * @since 2019-12-28
 *
 */
public interface CodeCharset {

  
  
  
  /**Converts from a UTC-16(Java-Character to a 8 bit coding. 
   * @param cc the character
   * @return 0 if not mapped. 1..255 for 8-bit-mapping.
   *   If >=256 then it is a sequence of bytes (UTF8) with max. 4 Byte (for not all UTF characters)
   */
  int getCode(char cc);
  
  
  /**Converts from a 8-bit-code to a character 
   * @param code in range 0..255, maybe >= 256 for UTF-8 sequences (The lowest byte is the first byte),
   *   with max. 4 Byte (for not all UTF characters)
   * @return The character.
   */
  char getChar(int code);
  
  
  Charset getCharset();
  
  static Map<String, CodeCharset> charsets = new TreeMap<String, CodeCharset>();
  
  
  public static CodeCharset forName(String name) {
    if(charsets.size() == 0) {
      charsets.put("ISO8859-1", iso8859_1);
      charsets.put("ISO-8859-1", iso8859_1);
      charsets.put("US-ASCII", ascii);
    }
    return charsets.get(name); //maybe null if not exits
  }

  
  public static CodeCharset iso8859_1 = new ISO8859_1();
  
  public static CodeCharset ascii = new ASCII7bit();
  
  
}
