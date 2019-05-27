package org.vishia.util;

import java.nio.charset.Charset;

/**Contains routines to convert String to number format.
 * @author Hartmut Schorrig
 *
 */
public class StringFunctions_C
{
  /**Version, history and license.
   * <ul>
   * <li>2019-05-23 Hartmut new: {@link #parseUlong(CharSequence, int, int, int, int[], String)} and adaption of the adequate using routines.
   *   Note: It is a base routine now used in {@link StringPartScan#scanDigits(int, int, String)}. It is done because gardening, adequate code unified.
   * <li>2016-10-14 Hartmut bugfix: {@link #parseFloat(CharSequence, int, int, char, int[])} has had a problem with 1 digit after decimal point. 
   *   This bug was not present in older versions. maybe forced with the last change on 2016-02-07.
   * <li>2016-02-07 Hartmut bugfix: {@link #parseFloat(String, int, int, char, int[])} has had a problem with negative numbers. 
   * <li>2015-11-07 Hartmut chg: Now the number conversion routines are moved to {@link StringFunctions_C}. 
   *   Reason: Dispersing the content because for some embedded applications a fine tuning of used sources is necessary.
   * <li>2013-09-07 Hartmut new: {@link #parseFloat(String, int, int, char, int[])} with choiceable separator (123,45, german decimal point)
   * <li>2013-08-10 Hartmut new: {@link #parseIntRadix(String, int, int, int, int[], String)} now can skip
   *   over some characters. In this kind a number like 2"123'456.1 is able to read.
   * <li>2013-08-10 Hartmut new: {@link #parseLong(String, int, int, int, int[], String)} as counterpart to parseInt  
   * <li>2012-04-01 Hartmut new {@link #parseIntRadix(String, int, int, int, int[])} etc.
   *   taken from C-Sources CRunntimeJavalike/source/Fwc/fw_Simple.c
   * <li>2012-02-19 Hartmut created: basic functions also existent in C (Java2C-usage).
   * </ul>
   * <br><br>
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
   */
  public final static String version = "2019-05-22"; 

  
  
  /**Parses a given String and convert it to the integer number.
   * The String may start with a negative sign ('-') and should contain digits after them.
   * The digits for radix > 10 where built by the numbers 'A'..'Z' respectively 'a'..'z',
   * known as hexa numbers A..F or a..f. 
   * @param srcP The String, non 0-terminated, see ,,size,,.
   * @param pos The position in src to start.
   * @param sizeP The maximal number of chars to parse. If it is more as the length of the String, no error. 
   *   One can use {@link Integer#MAX_VALUE} to parse till the end of the String
   * @param radix The radix of the number, typical 2, 10 or 16, max 36.
   * @param parsedChars number of chars which is used to parse the integer. The pointer may be null if not necessary. @pjava2c=simpleVariableRef. 
   * @param spaceChars maybe null, some characters which are skipped by reading the digits. It is especially ". '" to skip over a dot, or spaces or '
   * @return the Number.
   * @throws never. All possible digits where scanned, the rest of non-scanable digits are returned.
   *  For example the String contains "-123.45" it returns -123, and the retSize is 3.
   */
  public static int parseIntRadix(final CharSequence srcP, final int pos, final int sizeP, final int radix
      , final int[] parsedChars, final String spaceChars)
  { int val = 0;
    boolean bNegativ;
    int digit;
    char cc;
    int ixSrc = pos;
    int size = srcP.length() - pos;
    if(size > sizeP){ size = sizeP; }
    int maxDigit = (radix <=10) ? '0' + radix -1 : '9'; 
    if(size > 0 && srcP.charAt(ixSrc) == '-') { 
      ixSrc+=1; size -=1; bNegativ = true; 
    }
    else { bNegativ = false; }
    while(--size >= 0){
      cc = srcP.charAt(ixSrc);
      if(spaceChars !=null && spaceChars.indexOf(cc)>=0){
        ixSrc +=1;
      } else if((digit = cc - '0') >=0 
          && (  cc <= maxDigit 
              || (radix >10 && (  cc >= 'A' && (digit = (cc - 'A'+ 10)) <=radix
                               || cc >= 'a' && (digit = (cc - 'a'+ 10)) <=radix)
           )  )                )
      { val = radix * val + digit;
        ixSrc+=1;
      } else {
        break;
      }
    }
    if(bNegativ){ val = -val; }
    if(parsedChars !=null){
      parsedChars[0] = ixSrc - pos;
    }
    return( val);
  }
  

  
  /**
   * @param parsedChars number of chars which is used to parse. The pointer may be null if not necessary. @pjava2c=simpleVariableRef.
   * @return
   */
  @Java4C.Inline public static int parseIntRadix(final CharSequence srcP, final int pos, final int sizeP, final int radix, final int[] parsedChars)
  {
    return parseIntRadix(srcP, pos, sizeP, radix, parsedChars, null);
  }

  

  
  /**Adequate method for long values, see {@link #parseIntRadix(String, int, int, int, int[], String)}.
   * @param parsedChars number of chars which is used to parse. The pointer may be null if not necessary. @pjava2c=simpleVariableRef.
   * @param separatorChars Any character are accept inside the number as a separation character. For Example _ or ' or ,  to write:
   *   <code>12'345  12,234  12_345</code> which is parsed as 12345 in any case. Usual such as "'" 
   */
  public static long parseLong(final CharSequence srcP, final int pos, final int sizeP, final int radix
      , final int[] parsedChars, final String separatorChars) {
    int size = srcP.length() - pos;
    if(size > sizeP){ size = sizeP; }
    boolean bNegativ;
    int pos1 = pos;
    if(size > 0 && srcP.charAt(pos1) == '-') { 
      pos1+=1; size -=1; bNegativ = true; 
    }
    else { bNegativ = false; }
    long val = parseUlong(srcP, pos1, sizeP, radix, parsedChars, separatorChars);
    if(bNegativ){ val = -val; }
    return val;
  }

  
  
  
  
  /**Adequate method for long values, see {@link #parseIntRadix(String, int, int, int, int[], String)}.
   * @param parsedChars number of chars which is used to parse. The pointer may be null if not necessary. @pjava2c=simpleVariableRef.
   * @param spaceChars maybe null, some characters which are skipped by reading the digits. It is especially ". '" to skip over a dot, or spaces or '
   */
  /**
   * @param src Any CharSequence which should start on pos with a digit representation.
   * @param pos start position in src.
   * @param size max number of chars from src. The given size should not exceed the  size of src. Note: other than in {@link #parseLong(CharSequence, int, int, int, int[], String)}.
   * @param radix of the number, it can be any value 0..36- If >10 then characters A..Z or a..z are used for digits.
   *   Usually 16 for hex numbers. 
   * @param parsedChars maybe null, if not null it should be intialized with int[1]. The position [0] returns the number of the digits.
   * @param separatorChars Any character are accept inside the number as a separation character. For Example _ or ' or ,  to write:
   *   <code>12'345  12,234  12_345</code> which is parsed as 12345 in any case. Usual such as "'" 
   * @return The parsed number, 0 if no character is detected.
   */
  public static long parseUlong(final CharSequence src, final int pos, final int size, final int radix
      , final int[] parsedChars, final String separatorChars)
  { long val = 0;  //exact same lines as parseInt, difference is: using long instead int. 
    int size1 = size;
    int digit;
    char cc;
    int ixSrc = pos;
    int maxDigit = (radix <=10) ? '0' + radix -1 : '9'; 
    while(--size1 >= 0){
      cc = src.charAt(ixSrc);
      if(separatorChars !=null && separatorChars.indexOf(cc)>=0){
        ixSrc +=1;
      } 
      else if((digit = cc - '0') >=0 
          && (  cc <= maxDigit   // max '9'
              || (radix >10 && (  cc >= 'A' && (digit = (cc - 'A'+ 10)) <=radix  //digit is only used if cc >= 'A'
                               || cc >= 'a' && (digit = (cc - 'a'+ 10)) <=radix)
           )  )                ) {
        val = radix * val + digit;
        ixSrc+=1;
      } else {
        break;  //other char
      }
    }
    if(parsedChars !=null){
      parsedChars[0] = ixSrc - pos;
    }
    return( val);
  }
  

  
  
  /**Parses a given String backward and convert it to the integer number.
   * The String may start with a negative sign ('-') and should contain digits after them.
   * The digits for radix > 10 where built by the numbers 'A'..'Z' respectively 'a'..'z',
   * known as hexa numbers A..F or a..f. 
   * @param srcP The String.
   * @param pos The position in src to the last digit.
   * @param size The maximum of chars to parse. Should be less or equal pos.
   * @param radix The radix of the number, typical 2, 10 or 16, max 36.
   * @param parsedChars number of chars which is used to parse. The pointer may be null if not necessary. @pjava2c=simpleVariableRef.
   * @return the Number.
   * @throws never. All possible digits where scanned, the rest of non-scanable digits are returned.
   *  At example the String contains "-123.45" it returns -123, and the retSize is 3.
   */
  public static int parseIntRadixBack(final CharSequence srcP, final int pos, final int sizeP, final int radix, final int[] parsedChars)
  { int val = 0;
    boolean bNegativ;
    int digit;
    char cc;
    int ixSrc = pos;
    int size = srcP.length() - pos;
    if(size > sizeP){ size = sizeP; }
    int maxDigit = (radix <=10) ? '0' + radix -1 : '9'; 
    int maxHexDigitLower = 'A' + radix - 11; 
    int maxHexDigitUpper = 'a' + radix - 11; 
    int multPosition = 1;
    while(size > 0 && ixSrc >=0 && (digit = (cc = srcP.charAt(ixSrc)) - '0') >=0 
         && (  cc <= maxDigit 
            || (radix >10 && (  cc >= 'A' && (digit = (cc - 'A'+ 10)) <=radix
                             || cc >= 'a' && (digit = (cc - 'a'+ 10)) <=radix)
         )  )                )
    { val += multPosition * digit;
      multPosition *= radix;
      ixSrc-=1;
      size -=1;
    }
    if(size > 0 && ixSrc >=0 && srcP.charAt(ixSrc) == '-') { 
      ixSrc-=1; size -=1; 
      val = -val;
    }
    if(parsedChars !=null){
      parsedChars[0] = pos - ixSrc;
    }
    return( val);
  }
  
  
  /**
   * @param src
   * @param pos
   * @param sizeP
   * @param parsedChars number of chars which is used to parse. The pointer may be null if not necessary. @pjava2c=simpleVariableRef.
   * @return
   */
  public static float parseFloat(CharSequence src, int pos, int sizeP, int[] parsedChars)
  { return parseFloat(src, pos, sizeP, '.', parsedChars);
  }  
  
  
  /**Parses a given String and convert it to the float number.
   * An exponent is not regarded yet (TODO).
   * @param src The String, see ,,size,,.
   * @param pos The position in src to start.
   * @param sizeP The number of chars to regard at maximum. A value of -1 means: use the whole String till end. 
   *   sizeP = 0 is possible, then no float was parsed and paredCharsP[0] is set to 0. It may be possible
   *   that the number of characters to parse will be calculated outside, and 0 is a valid result. 
   *   If sizeP is > the length, then the whole String is used.
   *   You can set both sizeP = -1 or sizeP = Integer.MAXVALUE to deactivate this argument.
   * @param decimalpoint it is possible to use a ',' for german numbers.
   * @return the Number.
   * @param parsedCharsP number of chars which is used to parse. The pointer may be null if not necessary. @pjava2c=simpleVariableRef.
   * @return
   */
  public static float parseFloat(CharSequence src, int pos, int sizeP, char decimalpoint, int[] parsedCharsP)
  {
    float ret;
    int poscurr = pos;
    int restlen = src.length() - pos;
    if(sizeP >=0 && restlen > sizeP){ restlen = sizeP; }
    boolean bNegative;
    if(restlen > 0 && src.charAt(poscurr) == '-') { 
      poscurr+=1; restlen -=1; bNegative = true; 
    }
    else { 
      bNegative = false; 
    }
    @Java4C.StackInstance @Java4C.SimpleArray int[] zParsed = new int[1];
    ret = parseIntRadix(src, poscurr, restlen, 10, zParsed, null);  //parses only a positive number.
    poscurr += zParsed[0];   //maybe 0 if .123 is written
    restlen -= zParsed[0];
    //if(poscurr < (restlen+pos) && src.charAt(poscurr)==decimalpoint){
    if(restlen >0 && src.charAt(poscurr)==decimalpoint){
      float fracPart = parseIntRadix(src, poscurr +1, restlen-1, 10, zParsed);
      if(zParsed[0] >0){
        switch(zParsed[0]){
        case 1: fracPart *= 0.1f; break;
        case 2: fracPart *= 0.01f; break;
        case 3: fracPart *= 0.001f; break;
        case 4: fracPart *= 0.0001f; break;
        case 5: fracPart *= 1.0e-5f; break;
        case 6: fracPart *= 1.0e-6f; break;
        case 7: fracPart *= 1.0e-7f; break;
        case 8: fracPart *= 1.0e-8f; break;
        case 9: fracPart *= 1.0e-9f; break;
        case 10: fracPart *= 1.0e-10f; break;
        }
        ret += fracPart;
      }
      poscurr += zParsed[0]+1;  //don't forget the decimal point  
      //restlen -= zParsed[0]-1;
    }
    //TODO exponent
    if(parsedCharsP !=null){
      parsedCharsP[0] = poscurr - pos;
    }
    if(bNegative) {
      ret = -ret;  
    }
    return ret;
  }

  
  
}
