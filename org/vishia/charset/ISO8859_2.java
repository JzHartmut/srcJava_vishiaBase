package org.vishia.charset;

import java.nio.charset.Charset;

/**Character set of middle european characters
 * See {@linkplain http://www.gymel.com/charsets/ISO8859-2.html}
 * See {@linkplain https://www.unicode.org/Public/UCD/latest/ucd/UnicodeData.txt}
 * It is todo.
 * @author hartmut Schorrig
 *
 */
public class ISO8859_2 implements CodeCharset {

  Charset charset = Charset.forName("ISO8859-2");
  
  @Override public int getCode(char cc) {
    if(cc < 128) return (int)cc;
    else return 0;
  }

  @Override public char getChar(int code) {
    return (char)code;
  }

  private static int[] chartable =
    { 0x00a0, 0x0104, 0x02d8};
  
  private static int[] chartableSorted = 
    {0x00a0, 0x0104, 0x0141, 0x02d8};
  
  private static int[] codeSorted = 
    { 0xa0, 0xa1, 0xa3, 0xa2} ;
  
  
  
  /*
  xA0
  160
  240   [ �?� ]
   
  00A0  Ą
  0104  ˘
  02D8  �?
  0141  ¤
  00A4  Ľ
  013D  Ś
  015A  §
  00A7      ¨
  00A8  Š
  0160  Ş
  015E  Ť
  0164  Ź
  0179  ­
  00AD  Ž
  017D  Ż
  017B
  xB0
  176
  260   °
  00B0  ą
  0105  ˛
  02DB  ł
  0142  ´
  00B4  ľ
  013E  ś
  015B  ˇ
  02C7      ¸
  00B8  š
  0161  ş
  015F  ť
  0165  ź
  017A  �?
  02DD  ž
  017E  ż
  017C
  xC0
  192
  300   Ŕ
  0154  �?
  00C1  Â
  00C2  Ă
  0102  Ä
  00C4  Ĺ
  0139  Ć
  0106  Ç
  00C7      Č
  010C  É
  00C9  Ę
  0118  Ë
  00CB  Ě
  011A  �?
  00CD  Î
  00CE  Ď
  010E
  xD0
  208
  320   �?
  0110  Ń
  0143  Ň
  0147  Ó
  00D3  Ô
  00D4  �?
  0150  Ö
  00D6  ×
  00D7      Ř
  0158  Ů
  016E  Ú
  00DA  Ű
  0170  Ü
  00DC  �?
  00DD  Ţ
  0162  ß
  00DF
  xE0
  224
  340   ŕ
  0155  á
  00E1  â
  00E2  ă
  0103  ä
  00E4  ĺ
  013A  ć
  0107  ç
  00E7      �?
  010D  é
  00E9  ę
  0119  ë
  00EB  ě
  011B  í
  00ED  î
  00EE  �?
  010F
  xF0
  240
  360   đ
  0111  ń
  0144  ň
  0148  ó
  00F3  ô
  00F4  ő
  0151  ö
  00F6  ÷
  00F7      ř
  0159  ů
  016F  ú
  00FA  ű
  0171  ü
  00FC  ý
  00FD  ţ
  0163  ˙
  02D9
  */

  @Override
  public Charset getCharset() {
    return this.charset;
  }
}
