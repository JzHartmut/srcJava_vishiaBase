
package org.vishia.util;

import java.nio.charset.Charset;


/**This class contains static String functions without any other dependency able to use with CharSequence (instead java.lang.String).
 * <br>
 * All routines with a range selection have the following rule:
 * <ul>
 * <li>The start position (from) counts from 0 till end as absolute position.
 * <li>The end position (to) counts positive from 0 till end as absolute position
 * <li>The end position (to) counts negative from -1 till start as relative position from end.
 * <li>The end position -1 means, the whole String.
 * <li>If the end position is greater end (positive), it is till end. It means, {@link Integer#MAX_VALUE} can be used instead -1 too.
 * <li>If the end position is less than the start, the range is 0. An exception is not thrown. Due to end position an exception is never thrown.
 * <li>If the start position is outside the range of source, an Exception is thrown.  
 * In C the functions are contained in the emC/String_emC.c.
 * @author Hartmut Schorrig
 *
 */
public class StringFunctions {

  /**Version, history and license.
   * <ul>
   * <li>2024-09-19 Hartmut new featurefix {@link #indexOf(CharSequence, int, int, CharSequence)} and {@link #lastIndexOf(CharSequence, int, int, CharSequence)}
   *   regards now an empty "" to search (length()==0). Then the left respectively right position is returned.
   *   The String variants are removed, they were identically. 
   * <li>2024-06-21 Hartmut bugfix in {@link #lastIndexOf(CharSequence, int, int, CharSequence)} has had returned faulty position in specific (rarely) cases.
   * <li>2024-06-21 Hartmut new {@link #lastIndexOfAnyString(CharSequence, int, int, CharSequence[], int[])} 
   * <li>2024-01-15 Hartmut  {@link #convertTransliteration(CharSequence, char)} now it's done at last conversion "\x0041" for example character 'A', also some comments improved.
   * <li>2023-06-08 Hartmut bugfix/chg: Now {@link #indexAfterIdentifier(CharSequence, int, int, String)} returns 0   if the position starts with a number.
   *   That is proper also for test whether currently it is an identifier and it is consequent.
   *   The old behavior of this operation is now available in {@link #indexAfterIdentifierOld(CharSequence, int, int, String)}.
   * <li>2023-04-30 Hartmut bugfix {@link #startsWith(CharSequence, CharSequence)}: was faulty if number of chars to compare is equal the length. 
   * <li>2023-04-02 Hartmut bugfix {@link #comparePos(CharSequence, int, CharSequence, int, int)}: 
   *   If any String is longer than nrofChars but the rest is equal, now returns 0. (Before, returns a value > nrofChars).  
   * <li>2022-05-09 Hartmut new {@link #startsWithAfterAnyChar(CharSequence, CharSequence, String)}, new {@link #indexAfterAnyChar(CharSequence, int, int, CharSequence)}
   * <li>2022-05-09 Hartmut new {@link #compareWhSpacePos(CharSequence, int, int, CharSequence, int, int, int[])}
   * <li>2022-01-20 Hartmut new {@link #indexOfAnyCharOutsideQuotation(CharSequence, int, int, CharSequence, CharSequence, CharSequence, char, int[])}
   *   etc, handling quotations, here also (not only in {@link org.vishia.util.StringPart}, used for {@link org.vishia.cmd.CmdExecuter#splitArgs(String, int, int)}
   * <li>2021-12-19 Hartmut bugfix {@link #comparePos(CharSequence, int, CharSequence, int, int)}: change on 2021-20-04 was wrong!
   *   The problem is, if the first char is different, it should not return 0, because that means equal. It should retunr 1 or -1
   *   Because of that sometimes a bad directory was selected in Fcmd (a non exist new directory was detected as the last used).
   * <li>2021-10-04 Hartmut new {@link #utf16toUTF8bytes(char, byte[], int)} but also not tested and not used but may be correct. 
   * <li>2021-10-04 Hartmut rename @link {@link #utf8to16(byte[], int[])} instead link #byte2UTF8(byte[], int[])} but: this function is not tested
   *   and may be not used yet, may be not correct.
   * <li>2021-10-04 Hartmut bugfix {@link #comparePos(CharSequence, int, CharSequence, int, int)}: return was one time to high.
   * <li>2021-06-21 Hartmut bugfix {@link #comparePos(CharSequence, int, CharSequence, int, int)} for comparing empty strings
   * <li>2019-12-28 Hartmut new {@link #indexOfAnyChar(CharSequence, int, int, CharSequence, int[])} returns the number of the found character too
   * <li>2019-06-08 Hartmut new: All StringFunctions with negative to argument, count from end, -1 is till end.
   * <li>   * <li>2019-06-07 Hartmut new: {@value #compareChars(CharSequence, int, int, CharSequence)} as helper to find where is the difference, versus {@link #equals(CharSequence, int, int, CharSequence)} 
   * <li>2016-12-02 Hartmut new: {@value #cNoCidentifier} 
   * <li>2016-09-25 Hartmut new: {@link #nrofBytesUTF8(byte)} used in {@link StringPartFromFileLines} 
   * <li>2016-05-22 Hartmut chg: {@link #indexOfAnyString(CharSequence, int, int, CharSequence[], int[], String[])}: Algorithm from StringPart
   *   copied to here. It is common. temporary instance in StrinPart prevented. 
   * <li>2015-11-07 Hartmut chg: Now the number conversion routines are moved to {@link StringFunctions_C}. 
   *   Reason: Dissipate the content because for some embedded applications a fine tuning of used sources is necessary.
   * <li>2015-10-23 Hartmut new: {@link #indexWhitespace(CharSequence, int, int)}, {@link #indexNoWhitespace(CharSequence, int, int)}.
   *   {@link #indexAfterIdentifier(CharSequence, int, int, String)} 
   * <li>2015-06-05 Hartmut chg: {@link #equals(CharSequence, int, int, CharSequence)} regards null-pointer too.
   * <li>2014-09-05 Hartmut new: Twice methods {@link #indexOf(CharSequence, int, int, String)} and {@link #indexOf(CharSequence, int, int, CharSequence)}.
   *   Twice methods {@link #lastIndexOf(CharSequence, int, int, String)} and {@link #lastIndexOf(CharSequence, int, int, CharSequence)}.
   *   The methods are the same in Java. But in C the handling of reference is different. In Java2C translation a StringJc does not base on CharSequence
   *   because it is a simple reference to char[] and a length only. CharSequence needs ObjectJc and virtual methods. 
   * <li>2014-05-04 Hartmut new: {@link #indexOfAnyChar(CharSequence, int, int, String)}: Algorithm transfered from 
   *   {@link StringPart#indexOfAnyChar(String, int, int, char, char, char)} to this class for common usage,
   *   called in StringPart. TODO do it with all that algoritm.
   * <li>2014-05-04 Hartmut new {@link #cEndOfText} now defined here, parallel to {@link StringPart#cEndOfText}.
   *   Note: that char is ASCII but not UTF.   
   * <li>2014-03-11 Hartmut new: {@link #indent2(int)}
   * <li>2013-09-07 Hartmut new: {@link #parseFloat(String, int, int, char, int[])} with choiceable separator (123,45, german decimal point)
   * <li>2013-09-07 Hartmut new: {@link #convertTranscription(CharSequence, char)} used form {@link SpecialCharStrings#resolveCircumScription(String)}
   * <li>2013-08-29 Hartmut bugfix: {@link #compare(CharSequence, int, CharSequence, int, int)}, {@link #indexOf(CharSequence, CharSequence, int)}
   * <li>2013-08-10 Hartmut new: {@link #parseIntRadix(String, int, int, int, int[], String)} now can skip
   *   over some characters. In this kind a number like 2"123'456.1 is able to read.
   * <li>2013-08-10 Hartmut new: {@link #parseLong(String, int, int, int, int[], String)} as counterpart to parseInt  
   * <li>2013-07-28 Hartmut new: {@link #isEmptyOrOnlyWhitespaces(CharSequence)} 
   * <li>2013-05-04 Hartmut new some methods for usage CharSequence: {@link #compare(CharSequence, int, CharSequence, int, int)},
   *   {@link #startsWith(CharSequence, CharSequence)}, {@link #endsWith(CharSequence, CharSequence)},
   *   {@link #indexOf(CharSequence, char, int)}, {@link #indexOf(CharSequence, CharSequence, int)}
   *   Generally usage of CharSequence as StringBuilder instance saves calculation time in comparison with usage String,
   *   because a new allocation is saved. This saving can be done any time if the StringBuilder is non thread-shared
   *   and its reference is not stored permanently but only used immediately in the thread.
   * <li>2013-02-03 Hartmut new  {@link #compare(CharSequence, CharSequence)} and {@link #equals(Object)}.
   * <li>2012-08-22 Hartmut new {@link #copyToBuffer(String, char[])} and {@link #copyToBuffer(String, byte[], Charset)}:
   *   This methods are existent at the C-level. They are usefully if dynamic memory usage should be prevented.
   *   They are need for Java-usage with static data too. 
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
  public final static String version = "2024-06-21"; 
  
  
  /** The char used to code end of text. It is defined in ASCII as EOT. 
   * In Unicode it is the same like {@value Character#TITLECASE_LETTER}, another meaning. */  
  public static final char cEndOfText = (char)(0x3);

  /** The char used to code start of text. */  
  public static final char cStartOfText = (char)(0x2);
  
  /**The char used to coding any char which is not an identifier character
   * usual in programming languages like C, Java: A..Z a..z 0..9 _. */  
  public static final char cNoCidentifier = (char)(0x4);




  /**Searches the first whitespace in src from start till at maximal endMax.
   * A whitespace is one of " \r\n\t\f"
   * @param src
   * @param start
   * @param endMax >=0: absolute exclusive end position for search, <0: end position relative to end, -1 is the end of src
   * @return >=0: position in src of the first space from start, -1 if not found in range. 
   */
  public static int indexWhitespace(CharSequence src, int start, int endMax){
    int pos = start;
    int zsq = src.length();
    int end = (endMax < 0 ? zsq + endMax +1 : (endMax >= zsq ? zsq : endMax)) ;  //end is <pos if endMax is left from start
    char cc;
    while(  pos < end && (cc = src.charAt(pos)) != ' ' && cc != '\r' && cc != '\n' && cc != '\t' && cc != '\f' )
    { pos +=1;
    }
    return pos;
  }
  
  
  /**Searches the first non-whitespace in src from start till at maximal endMax.
   * A whitespace is one of " \r\n\t\f"
   * @param src
   * @param start
   * @param endMax >=0: absolute exclusive end position for search, <0: end position relative to end, -1 is the end of src
   * @return anyway >= start, <=end, position in src of the first non space from start. 
   */
  public static int indexNoWhitespace(CharSequence src, int start, int endMax){
    int pos = start;
    int zsq = src.length();
    int end = (endMax < 0 ? zsq + endMax +1 : (endMax >= zsq ? zsq : endMax)) ;  //end is <pos if endMax is left from start
    char cc;
    while(  pos < end &&  ((cc = src.charAt(pos)) == ' ' || cc == '\r' || cc == '\n' || cc == '\t' || cc == '\f' ))
    { pos +=1;
    }
    return pos;
  }
  
  
  /**Searches the position of the first identifier character starting from the given position.
   * If the given position is on an identifier start character, it will be returned without change.
   * @param src
   * @param start
   * @param endMax >=0: absolute exclusive end position for search, <0: end position relative to end, -1 is the end of src
   * @param additionalStartChars null: do not use, 
   * @return -1 if an identifier is not found. Elsewhere it is the position of the following identifier character.
   */
  public static int indexIdentifier(CharSequence src, int start, int endMax, String additionalStartChars){
    int pos = start;
    int zsq = src.length();
    int end = (endMax < 0 ? zsq + endMax +1 : (endMax >= zsq ? zsq : endMax)) ;  //end is <pos if endMax is left from start
    char cc;
    while( pos < end 
      && (cc = src.charAt(pos)) != '_' 
      && (cc < 'A' || cc >'Z') 
      && (cc < 'a' || cc >'z') 
      && (additionalStartChars == null || additionalStartChars.indexOf(cc)<0)
      )
    { pos +=1;
    }
    return pos < end? pos : -1;
  }
  
  
  /**Returns the position after the end of an identifier.
   * Note: An identifier usual never starts with 0..9. But this routine assumes that it is an identifier. 
   * It means starting with 0..9 should be detected and processed outside. If this src starts with 0..9 it is accepted here.
   * @param src The input string
   * @param start at this position the identifier starts.
   * @param endMax >=0: absolute exclusive end position for search, <0: end position relative to end, -1 is the end of src
   * @param additionalChars maybe null, some chars as additional chars of an identifier, inside and as start.
   * @return 0 if src[start] doesn't match to an identifier character, number of found identifier chars after src until end.
   */
  @Deprecated public static int indexAfterIdentifierOld(CharSequence src, int start, int endMax, String additionalChars) {
    int pos = start;
    int zsq = src.length();
    int end = (endMax < 0 ? zsq + endMax +1 : (endMax >= zsq ? zsq : endMax)) ;  //end is <pos if endMax is left from start
    char cc;
    while(  pos < end 
           && (  (cc = src.charAt(pos)) == '_' 
              || (cc >= '0' && cc <='9') 
              || (cc >= 'A' && cc <='Z') 
              || (cc >= 'a' && cc <='z') 
              || (additionalChars != null && additionalChars.indexOf(cc)>=0)
           )  ) 
    {
      pos +=1; 
    }
    return pos;
  }
  
  
  
  /**Returns the position after the end of an identifier.
   * @param src If the first char on [start] is 0..9 it returns 0, it is not an identifier.
   * @param start from this position
   * @param endMax >=0: absolute exclusive end position for search, <0: end position relative to end, -1 is the end of src
   * @param additionalChars maybe null, some chars as additional chars of an identifier, inside and as start.
   * @return 0 if it is not start with an identifier start character. '0'..'9' is not an identifier start.
   *         elsewhere the number of chars which are identifier characters A..Z, a..z, _, 0..9 and the additionalChars  
   */
  public static int indexAfterIdentifier(CharSequence src, int start, int endMax, String additionalChars){
    int pos = start;
    int zsq = src.length();
    int end = (endMax < 0 ? zsq + endMax +1 : (endMax >= zsq ? zsq : endMax)) ;  //end is <pos if endMax is left from start
    char cc;
    while(  pos < end 
           && (  (cc = src.charAt(pos)) == '_' 
              || (cc >= '0' && cc <='9' && pos > start) 
              || (cc >= 'A' && cc <='Z') 
              || (cc >= 'a' && cc <='z') 
              || (additionalChars != null && additionalChars.indexOf(cc)>=0)
           )  ) {
      pos +=1; 
    }
    return pos;
  }

  /**Returns the position of the end of an identifier.
   * @param src The input string
   * @param start at this position the identifier starts.
   * @param endq max number of chars to check
   * @param additionalStartChars maybe null, some chars as additional start chars of an identifier.
   * @param additionalChars maybe null, some chars as additional chars of an identifier.
   * @return 0 if src[start] doesn't match to an identifier character, number of found identifier chars after src until end.
   * @deprecated use {@link #indexAfterIdentifier(CharSequence, int, int, String)}
   */
  @Deprecated
  public static int posAfterIdentifier(CharSequence src, int start, int endMax, String additionalStartChars, String additionalChars){
    int pos = start;
    char cc = src.charAt(pos);
    if(   cc == '_' 
      || (cc >= 'A' && cc <='Z') 
      || (cc >= 'a' && cc <='z') 
      || (additionalStartChars != null && additionalStartChars.indexOf(cc)>=0)
      )
    { pos +=1;
      while(  pos < endMax 
           && (  (cc = src.charAt(pos)) == '_' 
              || (cc >= '0' && cc <='9') 
              || (cc >= 'A' && cc <='Z') 
              || (cc >= 'a' && cc <='z') 
              || (additionalChars != null && additionalChars.indexOf(cc)>=0)
           )  )
      { pos +=1; }
    }
    return pos;
  }
  
  /**Returns the position of the end of an identifier.
   * @param src The input string
   * @param start at this position the indentier starts.
   * @param endq max number of chars to check
   * @return 0 if src[start] doesn't match to an identifier character, number of found identifier chars after src until end.
   * @deprecated use {@link #indexAfterIdentifier(CharSequence, int, int, String)}
   */
  @Deprecated
  public static int posAfterIdentifier(CharSequence src, int start, int endMax){ return posAfterIdentifier(src, start, endMax, null, null); }


  /**Copies the content of a String in the given byte buffer with the requested encoding.
   * If this method is used in a C/C++ environment, it is programmed in a special maybe more simple way
   * because not all encodings are supported. Usual a String is only a byte array itself, it is copied.  
   * @param src The src String
   * @param buffer The desination buffer with given length. The last byte will be set to 0.
   * @param encoding The encoding. If null, then use the UTF8-encoding.
   * @return number of bytes in buffer.
   */
  @Java4C.Exclude public static int copyToBuffer(String src, byte[] buffer, Charset encoding){
    if(encoding == null){ 
      encoding = Charset.forName("UTF8"); 
    }
    byte[] bytes = src.getBytes(encoding);
    int nChars = bytes.length;
    if(buffer.length < nChars){
      nChars = buffer.length -1;
      int ix;
      for(ix=0; ix<nChars; ++ix){
        char cc = src.charAt(ix);
        buffer[ix] = bytes[ix];  
      }
      buffer[ix] = 0;
    }
    return nChars;
  }
  
  
  /**Copies the content of a String in the given char buffer.
   * If this method is used in a C/C++ environment, the char buffer may be a byte buffer really. 
   * @param src The src String
   * @param buffer The desination buffer with given length. The last byte will be set to 0.
   * @return number of chars in buffer.
   */
  @Java4C.Exclude public static int copyToBuffer(String src, char[] buffer){
    int nChars = src.length();
    if(buffer.length < nChars){
      nChars = buffer.length -1;
    }
    int ix;
    for(ix=0; ix<nChars; ++ix){
      char cc = src.charAt(ix);
      buffer[ix] = src.charAt(ix);  
    }
    buffer[ix] = '\0';
    return nChars;
  }
  
  
  
  /**Converts a String in a buffer in a java.lang.String.
   * @param buffer It is zero-terminated.
   * @return A String which contains all characters till the first '\0' or the whole buffer.
   */
  @Java4C.Exclude public static String z_StringJc(char[] buffer){
    int ix=-1;
    while(++ix < buffer.length && buffer[ix] !='0');
    return new String(buffer, 0, ix);
  }
  

  
  /**Compares two CharSequence (Strings, StringBuilder-content etc.
   * It is the adequate functionality like {@link java.lang.String#compareTo(String)}.
   * but it works proper with {@link java.lang.CharSequence}. See example on {@link #equals(Object)}.
   *  
   * @param s1 left char sequence
   * @param s2 right char sequence
   * @return 0 if all characters are equal, positive if the part of s1 > s2,  negative if s1 < s2.
   *   <br>The absolute of return is the number of equal characters +1.
   *   <br>Note that the different character is charAt(returnValue -1) or the length of the shorter CharSeqence is returnVal -1.
   *     This convention is necessary because 0 means equal. It should be distinguish from the result charAt(0) is different.
   *   <br>-1 means, the first character is different whereby s1.charAt(0) < s2.charAt(0) or s1.length()==0 && s2.length() >0
   *   <br> 1 means, the first character is different whereby s1.charAt(0) > s2.charAt(0) or s1.length() >= && s2.length()==0
   *   <br> The comparison of "abcx" with "abcy" results -4 because 'x' < 'y' on the position 3.
   *   <br> The comparison of "abc" with "abcy" results -4 because 'x' < 'y' on the position 3.
   *   
   */
  public static int comparePos(CharSequence s1, CharSequence s2){
    return comparePos(s1, 0, s2, 0, -1);
  }
  
  
  /**Compares two CharSequence (Strings, StringBuilder-content etc.)
   * It detects that position where the Strings are different. That is not done by {@link String#compareTo(String)}
   * or {@link #compare(CharSequence, int, CharSequence, int, int)}
   *  
   * @param s1 left char sequence
   * @param from1 start position
   * @param s2 right char sequence
   * @param from2 start position
   * @param nrofChars maximal number of chars to compare. It can be negative or {@link java.lang.Integer#MAX_VALUE}
   *   to compare all characters to the end. Use -1 to compare all characters is recommended.
   *   Note: if 0, the return value of this method is 0 because all (=0) characters are equal. This may be important for some extrem situations.
   * @return 0 if all characters are equal, positive if the part of s1 > s2,  negative if s1 < s2.
   *   <br>The absolute of return is the number of equal characters +1.
   *   <br>Note that the different character is charAt(returnValue -1) or the length of the shorter CharSeqence is returnVal -1.
   *     This convention is necessary because 0 means equal. It should be distinguish from the result charAt(0) is different.
   *   <br>-1 means, the first character is different whereby s1.charAt(0) < s2.charAt(0) or s1.length()==0 && s2.length() >0
   *   <br> 1 means, the first character is different whereby s1.charAt(0) > s2.charAt(0) or s1.length() >= && s2.length()==0
   *   <br> The comparison of "abcx" with "abcy" results -4 because 'x' < 'y' on the position 3.
   *   <br> The comparison of "abc" with "abcy" results -4 because 'x' < 'y' on the position 3.
   *   
   */
  public static int comparePos(CharSequence s1, int from1, CharSequence s2, int from2, int nrofChars){
    int i1 = from1;
    int i2 = from2;  //post-increment
    int z1 = s1.length();
    int z2 = s2.length();
    if(nrofChars ==0) return 0; //NOTE: following while compares at least one char
    int zChars =  nrofChars >= 0 ? Math.min(nrofChars, Math.min(z1- i1, z2-i2)) : Math.min(z1-i1, z2-i2);
    //z1 -=1; z2 -=1;  //compare before increment then.
    if(z1 > zChars) { z1 = zChars; }
    if(z2 > zChars) { z2 = zChars; }
    char c1=0, c2=0;
    while(c1 == c2 && --zChars >=0) {
      c1 = s1.charAt(i1++);
      c2 = s2.charAt(i2++);
    } 
    //Note: On evaluation regard postincrement of i1, @date 2021-10-05 => but this was faulty. Returns -1 if the first char is different.
    //      Elsewhere it returns 0 on first char different, that is not able to use!!!! The contract is clarified. Change on 2021-10-05 was faulty.
    if(zChars == -1){
      //all characters compared, maybe difference in length.
      if(i2 < z2) return -(i1 - from1 +1);     // s2 is longer, s1 is less.
      else if(i1 < z1) return i1 - from1 +1;   // positive value: s1 is greater because i1 < z2, is longer and c1==c2 
      else return 0;  //both equal, comparison to end. 
    } 
    else {
      //not all possible characters compared, difference in character
      if(c1 < c2) return -(i1 - from1);       // c1 !=c2, then compare the last characters. <0 because s1 is lesser.
      else return (i1 - from1);               //note: == i2 - from2, s2 is lesser.
    }
  }
  

  
  
  /**Compare two Strings whereby white spaces are ignored. 
   * This is ideal for test results where white spaces are non relevant for the results. 
   * @param s1 Any string, can be null
   * @param from1 start position
   * @param to1 -1 then till end, -2... before end, positive: number of chars
   * @param s2 Second String
   * @param from2
   * @param to2
   * @param p2Out null or an int[1] variable for the error position in s2
   * @return 0 if both are equal, >=1 if s1 is alphabetic greater or longer, <=-1 if s1 is lesser or shorter. 
   *   The return value is the error position +1. Errror on start position is 1 (to distinguish from 0 = ok. )
   */
  public static int compareWhSpacePos(CharSequence s1, int from1, int to1, CharSequence s2, int from2, int to2, int[] p2Out){
    if(s1 ==null && s2 ==null) return 0;
    if(s1 ==null) { return -1;}
    if(s2 ==null) { return 1; }
    char c1=0, c2=0;
    int to11 = to1 >0 ? to1: s1.length() + to1 +1;         // to1 == -1, then till end, to1 <= -2 then before end
    int to21 = to2 >0 ? to2: s2.length() + to2 +1;
    int p1 = from1, p2 = from2;
    
    while(p1 < to11 && p2 < to21) {
      c1 = s1.charAt(p1++);
      c2 = s2.charAt(p2++);
      while(" \t\n\t\f".indexOf(c1) >=0 && p1 < to11) {                    // whitespace in c1
        c1 = s1.charAt(p1++);
      }
      while(" \t\n\t\f".indexOf(c2) >=0 && p2 < to21) {                    // whitespace in c1
        c2 = s2.charAt(p2++);
      }
      if(c1 != c2) {
        break;
      }
    }
    if(c1 == c2) {
      while(p1 < to11 
          && " \t\n\t\f".indexOf(s1.charAt(p1++)) >=0) {                    // whitespace in c1
      }
      while(p2 < to21 
          && " \t\n\t\f".indexOf(s2.charAt(p2++)) >=0) {                    // whitespace in c1
      }
    }
    if(c1 == c2 && p1 == to11 && p2 == to21) { return 0;}
    if(p2Out !=null && p2Out.length >=1) {
      p2Out[0] = p2 - from2;
    }
    if(c1 < c2) { return -(p1 - from1);}
    return (p1 - from1);
  }
  
  
  
  
  boolean test(String s){
    if(s==null) return false;
    else return true;
  }
  
  
  
  
  
  /**Compares two CharSequence (Strings, StringBuilder-content etc.
   * It is the adequate functionality like {@link java.lang.String#compareTo(String)}.
   * but it works proper with {@link java.lang.CharSequence}. See example on {@link #equals(Object)}.
   *  
   * @param s1 left char sequence, maybe null, then return -1 or 0 (both null)
   * @param from1 start position
   * @param s2 right char sequence, maybe null, then return 0 (both null) or 1. 
   * @param from2 start position
   * @param nrofChars maximal number of chars to compare. It can be {@link java.lang.Integer#MAX_VALUE}
   *   to compare all characters to the end.
   * @return 0 if all characters are equal, 1 if the part of s1 > s2,  -1 if s1 < s2.
   */
  public static int compare(CharSequence s1, int from1, CharSequence s2, int from2, int nrofChars){
    int i1 = from1 -1;
    int i2 = from2 -1;  //pre-increment
    int z = nrofChars + from1;
    int returnEq = 0;
    //zero check
    if(s1 == null){ return s2 == null ? 0: -1; }  //equal if both are null
    else if(s2 == null){ return 1; } //s1 is greater.
    //
    if(z > s1.length()){ 
      z = s1.length();
      int nrofChars1 = z - from1;
      int z2= from2 + nrofChars1;
      if( z2 == s2.length()){ returnEq = 0; }  //both have the same length after shorten.
      else if(z2 > s2.length()){
        int nrofChars2 = s2.length() - from2;
        z = from1 + nrofChars2;   //reduce length because s2
        returnEq = 1;  //returns 1 if equal because s2 is shorter
      }
      else {returnEq = -1; }    //returns -1 if equal because s1 is shorter
    } 
    else if((from2 + nrofChars) > s2.length()){ 
      //s2 is shorter than the requested or adjusted length:
      z = (s2.length()-from2) + from1;
      returnEq = 1;     //returns 1 if equal because s2 is shorter
    } 
    while(++i1 < z){
      char c1 = s1.charAt(i1), c2 =s2.charAt(++i2);
      if(c1 != c2){
        if(c1 < c2){ return -1; }
        else if(c1 > c2){ return 1; }
      }
    }
    //all chars till z are equal.
    return returnEq;
  }
  
  
  /**Compares two Strings or StringBuilder-content or any other CharSequence.
   * It is the adequate functionality like {@link java.lang.String#compareTo(String)}. 
   * @param s1
   * @param s2
   * @return 0 if all characters are equal, 1 if s1 > s2,  -1 if s1 < s2
   */
  public static int compare(CharSequence s1, CharSequence s2){
    return compare(s1, 0, s2, 0, Integer.MAX_VALUE);
  }  
  
  
  /**Compares two charsequences. It is similar String.equals(String), but works with CharSequence and accepts null-pointer.
   * @param s1 first, if null then returns true if s2== null. Equals is both null too.
   * @param from start position in s1
   * @param to exclusive end position in s1, if <0, especially -1 or > s1.length, then till length of s1. That is 'endsWith'.
   * @param s2 to compare with
   * @return true if all chars equals or both null.
   */
  public static boolean equals(CharSequence s1, int from, int to, CharSequence s2){
    //Test possibility:
    //if((s1 instanceof StringSeq) && contains(((StringSeq)s1).cs, "hide"))
    //  Debugutil.stop();
    int z1 = s1.length();
    if(s1 == null || s2 == null){ return s1 == null && s2 == null; }  //equals is both null, else not equal
    int zz = (to < 0 ? z1 + to +1 : (to >= z1 ? z1 : to)) - from;  //max is negative if to is left from fromIndex
    if( zz != s2.length()) return false;
    else {
      for(int ii = 0; ii<zz; ++ii){
        if(s1.charAt(from + ii) != s2.charAt(ii)) return false;
      }
      return true;
    }
  }
  

  /**Compare two character Strings. It returns the position of non equal character.
   * Therefore it has some more calculation time then equals.
   * @return >=0 number of chars which are equal, -1: all are equal and the length are equal.
   */
  public static int compareChars(CharSequence s1, int from, int to, CharSequence s2){
    int zsq = s1.length();
    int z2 = s2.length();
    if(s1 == null || s2 == null){ return 0; }  //equals is both null, else not equal
    int zz = (to < 0 ? zsq + to +1 : (to >= zsq ? zsq : to)) - from;  //max is negative if to is left from fromIndex
    if(zz > z2) { zz = z2; }
    for(int ii = 0; ii<zz; ++ii){
      if(s1.charAt(from + ii) != s2.charAt(ii)) return ii;
    }
    return zz == z2 ? -1 : zz;
  }

  

  /**Compares two Strings or StringBuilder-content or any other CharSequence.
   * It is the adequate functionality like {@link java.lang.String#equals(Object)}.
   * But the  {@link java.lang.String#equals(Object)} does only compare instances of Strings,
   * it does not compare a String with any other {@link java.lang.CharSequence} whether there are equal.
   * Not that: <pre>
   * String str = "abc";
   * String str2 = "abc";
   * StringBuilder sb = new StringBuilder(str);
   * assert(str.equals(str2));
   * assert(str.contentEquals(sb));  //special String comparator
   * assert( ! str.equals(sb));      //it is not equals, sb is not a String.
   * assert(StringFunctions.equals(str, sb));
   * assert(StringFunctions.equals(sb, str)); //compares any CharSequences
   * </pre>
   * @param s1
   * @param s2
   * @return 0 if all characters are equal, 1 if s1 > s2,  -1 if s1 < s2
   */
  public static boolean equals(CharSequence s1, CharSequence s2){
    return s1 == null ? s2 == null : equals(s1, 0, s1.length(), s2);
  }

  /**Returns true if s2 is contained in s1. 
   * It invokes {@link #indexOf(CharSequence, int, int, CharSequence)}.
   */
  public static boolean contains(CharSequence s1, CharSequence s2) {
    return indexOf(s1, 0, Integer.MAX_VALUE, s2) >=0; 
  }
  
  /**Checks whether the given CharSequence starts with a defined CharSequence.
   * It is the adequate functionality like {@link java.lang.String#startsWith(String)}
   * but it works proper with {@link java.lang.CharSequence}. See example on {@link #equals(Object)}.
   */
  public static boolean startsWith(CharSequence sq, CharSequence start){
    return compare(sq, 0, start, 0, start.length()) == 0;
  }
  
  
  /**Checks whether a given sq starts with the compare sequence start,
   * but after some admissible characters. 
   * @param sq The string to check
   * @param start start characters to check
   * @param anyCharBeforeStart often only one space, or specific indent character.
   * @return -1 if not starts with start, else position after the start string.
   */
  public static int startsWithAfterAnyChar(CharSequence sq, CharSequence start, String anyCharBeforeStart) {
    int begin = indexAfterAnyChar(sq, 0, -1, anyCharBeforeStart);
    int zStart = start.length();
    if(compare(sq, 0, start, 0, zStart)==0) {
      return begin +zStart; //position of the start string
    }
    else {
      return -1;    //not matched.
    }
  }

  /**Checks whether the given CharSequence starts with a CharSequence.
   * It is the adequate functionality like {@link java.lang.String#startsWith(String)}
   * but it works proper with {@link java.lang.CharSequence}. See example on {@link #equals(Object)}.
   * @param endMax >=0: absolute exclusive end position for search, <0: end position relative to end, -1 is the end of src
   */
  public static boolean startsWith(CharSequence sq, int from, int endMax, CharSequence start){
    int zstart = start.length();
    int zsq = sq.length();
    int max = (endMax < 0 ? zsq + endMax +1 : (endMax >= zsq ? zsq : endMax)) ;  //max is negative if to is left from fromIndex
    if((max - from) < zstart) return false;
    return compare(sq, from, start, 0, zstart) == 0;
  }
  

  /**Checks whether the given CharSequence ends with a CharSequence.
   * It is the adequate functionality like {@link java.lang.String#startsWith(String)}
   * but it works proper with {@link java.lang.CharSequence}. See example on {@link #equals(Object)}.
   */
  public static boolean endsWith(CharSequence sq, CharSequence end){
    int z = end.length();
    if(z > sq.length()) return false;
    else return compare(sq, sq.length()-z, end, 0, z) == 0;
  }
  

  
  /**Returns false if at least one char was found in text which is not a whitespace.
   * A whitespace is one of " \t\n\r" 
   * @param text to check
   * @return true if text is empty or contains only whitespaces.
   */
  public static boolean isEmptyOrOnlyWhitespaces(CharSequence text){
    char cc;
    int zz = text.length();
    int ii = -1;
    while(++ii < zz){
      cc = text.charAt(ii);
      if(" \t\n\r".indexOf(cc) <0){ return false; } //other character than whitespace
    }
    return true;
  }
  
  
  
  /**Searches the first occurrence of the given character in a CharSequence.
   * It is the adequate functionality like {@link java.lang.String#indexOf(char, int)}. 
   * @param sq search into
   * @param fromIndex start search
   * @param to >=0: absolute exclusive end position for search, <0: end position relative to end, -1 is the end of src
   * @param ch The character which is searched.
   * @return -1 if not found, else first occurrence where sq.charAt(return) == ch. 
   */
  public static int indexOf(CharSequence sq, int fromIndex, int to, char ch){
    int zsq = sq.length();
    int max = (to < 0 ? zsq + to +1 : (to >= zsq ? zsq : to)) ;  //max is negative if to is left from fromIndex
    int ii = fromIndex-1;  //pre-increment
    if (fromIndex < 0) {
        ii = -1;
    } else if (fromIndex >= max) {
        return -1;
    }
    while(++ii < max){
      char cc = sq.charAt(ii);
      if(cc == ch) {
        return ii;
      }
    }
    return -1;  //not found;
  }
  

  
  /**Searches the first occurrence of the given Character in a CharSequence.
   * It is the adequate functionality like {@link java.lang.String#indexOf(String, int)}. 
   * @param sq A CharSequence
   * @param str CharSequence which is searched.
   * @param fromIndex first checked position in sq
   * @return -1 if not found, else first occurrence where sq.charAt(return) == ch. 
   */
  public static int indexOf(CharSequence sq, char ch, int fromIndex){
    return indexOf(sq, fromIndex, Integer.MAX_VALUE, ch);
  }
  
  

  /**Searches the first occurrence of the given Character in a CharSequence.
   * It is the adequate functionality like {@link java.lang.String#indexOf(String)}. 
   * @param sq A CharSequence
   * @param str CharSequence which is searched.
   * @return -1 if not found, else first occurrence where sq.charAt(return) == ch. 
   */
  public static int indexOf(CharSequence sq, char ch){
    return indexOf(sq, 0, Integer.MAX_VALUE, ch);
  }
  
  

  
  /**Searches any char inside sChars in the given CharSequence. 
   * @param sq The String to search in
   * @param begin start position to search in sq
   * @param endMax >=0: absolute exclusive end position for search, <0: end position relative to end, -1 is the end of src
   * @param sChars Some chars to search in sq
   *   If sChars contains a EOT character (code 03, {@link #cEndOfText}) then the search stops at this character 
   *   or it is continued to the end of the range in sq. Then the length of the text range is returned
   *   if another character in sChars is not found. 
   *   It means: The end of the text range is adequate to an EOT-character. Note that EOT is not unicode,
   *   but it is an ASCII control character.  
   * @param nr null or initialized with new int[1], then returns the number of the found character in sChars from 0 
   * @return The first position in sq of one of the character in sChars or -1 if not found in the given range.
   */
  public static int indexOfAnyChar(CharSequence sq, int begin, int endMax, CharSequence sChars, int[] nr)
  { int zsq = sq.length();
    int end = (endMax < 0 ? zsq + endMax +1 : (endMax > zsq ? zsq : endMax)) ;  //max is negative if to is left from fromIndex
    int pos = begin-1;  //pre-increment
    int nr1 = -1;
    while(++pos < end && (nr1 = indexOf(sChars, sq.charAt(pos))) < 0){ }  //while any of char in sChars not found:
    if(pos < end 
      || (pos == end && indexOf(sChars, cEndOfText) >= 0)
      ) { 
      if(nr !=null) { nr[0] = nr1; }
      return pos;
    }
    else  return -1;
  }

  
  /**Searches the first character which is not contains in any given characters.
   * It is for example proper to skip over white spaces, then set <code>sChars = " \t\n\r\f"</code>.
   * @param sq search this string
   * @param begin >=0 for start
   * @param endMax >=0: absolute exclusive end position for search, <0: end position relative to end, -1 is the end of src
   * @param sChars This characters are skipped
   * @return position of the first character which is not contained in sChars or the length if to whole string contains only chars from sChars.
   *   returns begin if sq at begin does not start with one of the sChars.
   */
  public static int indexAfterAnyChar(CharSequence sq, int begin, int endMax, CharSequence sChars) {
    int zsq = sq.length();
    int end = (endMax < 0 ? zsq + endMax +1 : (endMax > zsq ? zsq : endMax)) ;  //max is negative if to is left from fromIndex
    int pos = begin -1;
    while(++pos < end && (indexOf(sChars, sq.charAt(pos))) >= 0){ }  //while any of char in sChars not found:
    return pos;
  }
  
  

  
  
  /**Only internal used. Used to check
   * @param sq
   * @param pos current, maybe a quotation or not, this operation tests it.
   * @param endMax
   * @param cc character on sq[pos]
   * @param sQuotChars maybe null
   * @param sQuotEndChars maybe null
   * @param zQu
   * @param transcriptChar
   * @return Position of the end character of the found quotation or unchanged pos.
   */
  private static int skipOverQuotation (CharSequence sq,  int pos, int endMax, char cc, CharSequence sQuotChars, CharSequence sQuotEndChars, int zQu, char transcriptChar) {
    int pos1 = pos;
    if(sQuotChars !=null) {
      int posQuot = indexOf(sQuotChars, cc);
      if(posQuot >=0) {
        char cQuotEnd = sQuotEndChars !=null ? sQuotEndChars.charAt(posQuot) : sQuotChars.charAt(posQuot);
        pos1 = indexOutsideQuotation(sq, pos1 +1, endMax, cQuotEnd, sQuotChars, sQuotEndChars, transcriptChar);
        if(pos1 <0) {                                        // ^search the end quotation char
          return pos1;     //-1 if end of quotation not found. 
        }
      }
    }
    return pos1;             // on the end char of the quotation
  }
  
  
  
  
  /**Searches the first occurrence of the given character in a CharSequence
   * whereby characters in quotation are not checked.
   * It is an enhancement in functionality of {@link #indexOf(CharSequence, char)} 
   * or adequate {@link java.lang.String#indexOf(char, int)} but regarding quotation.
   * @param sq search into
   * @param begin start search
   * @param endMax >=0: absolute exclusive end position for search, <0: end position relative to end, -1 is the end of src
   * @param csearch The character which is searched.
   * @param sQuotChars null or List of possible start quotation character, for example "\"'"
   * @param sQuotEndChars null or List of associated end quotation character. If null then the same as start character are used (familiar for "")
   * @param transcriptChar Character after this char is not checked, usual \ for transcription of \", \n etc. 
   * @return -1 if not found, else first occurrence where sq.charAt(return) == ch. 
   */
  public static int indexOutsideQuotation(CharSequence sq, int begin, int endMax, char csearch, CharSequence sQuotChars, CharSequence sQuotEndChars, char transcriptChar) {
    int zsq = sq.length();
    int end = (endMax < 0 ? zsq + endMax +1 : (endMax > zsq ? zsq : endMax)) ;  //max is negative if to is left from fromIndex
    int pos = begin-1;  //pre-increment
    int zQu = sQuotChars.length();
    char cc;
    while(++pos < end && (cc = sq.charAt(pos)) != csearch) {
      pos = skipOverQuotation(sq, pos, endMax, cc, sQuotChars, sQuotEndChars, zQu, transcriptChar);
      if(pos <0) {
        return pos; 
      }
    }
    if(pos < end) { 
      return pos;
    }
    else  return -1;
  }

  
  
  
  
  /**Searches the first occurrence of some given characters in a CharSequence
   * whereby characters in quotation are not checked.
   * It is an enhancement in functionality of {@link #indexOf(CharSequence, char)} 
   * or adequate {@link java.lang.String#indexOf(char, int)} but regarding quotation.
   * @param sq search into
   * @param begin start search
   * @param endMax >=0: absolute exclusive end position for search, <0: end position relative to end, -1 is the end of src
   * @param sChars Some characters which are searched.
   * @param sQuotChars null or List of possible start quotation character, for example "\"'"
   * @param sQuotEndChars null or List of associated end quotation character. If null then the same as start character are used (familiar for "")
   * @param transcriptChar Character after this char is not checked, usual \ for transcription of \", \n etc. 
   * @return -1 if not found, else first occurrence where sq.charAt(return) == ch. 
   */
  public static int indexOfAnyCharOutsideQuotation(CharSequence sq, int begin, int endMax, CharSequence sChars, CharSequence sQuotChars, CharSequence sQuotEndChars, char transcriptChar, int[] nr) {
    int zsq = sq.length();
    int end = (endMax < 0 ? zsq + endMax +1 : (endMax > zsq ? zsq : endMax)) ;  //max is negative if to is left from fromIndex
    int pos = begin-1;  //pre-increment
    int nr1 = -1;
    int zQu = sQuotChars == null ? 0 : sQuotChars.length();
    char cc;
    while(++pos < end && (nr1 = indexOf(sChars, (cc = sq.charAt(pos)))) < 0){   //while any of char in sChars not found:
      pos = skipOverQuotation(sq, pos, endMax, cc, sQuotChars, sQuotEndChars, zQu, transcriptChar);
      if(pos <0) {
        return pos; 
      }
    }
    if(pos < end 
      || (pos == end && indexOf(sChars, cEndOfText) >= 0)
      ) { 
      if(nr !=null) { nr[0] = nr1; }
      return pos;
    }
    else  return -1;
  }

  
  
  
  
  
  
  
  /**Searches any char inside sChars in the given Charsequence
   * @param begin start position to search in sq
   * @param endMax >=0: absolute exclusive end position for search, <0: end position relative to end, -1 is the end of src
   * @param sChars Some chars to search in sq
   *   If sChars contains a EOT character (code 03, {@link #cEndOfText}) then the search stops at this character 
   *   or it is continued to the end of the range in sq. Then the length of the text range is returned
   *   if another character in sChars is not found. 
   *   It means: The end of the text range is adequate to an EOT-character. Note that EOT is not unicode,
   *   but it is an ASCII control character.  
   * @return The first position in sq of one of the character in sChars or -1 if not found in the given range.
   */
  public static int indexOfAnyChar(CharSequence sq, int begin, int endMax, CharSequence sChars) {
    return indexOfAnyChar(sq, begin, endMax, sChars, null);
  }
  
  /**Searches the last occurrence of the given char in a CharSequence.
   * It is the adequate functionality like {@link java.lang.String#lastIndexOf(char, fromEnd)}. 
   * @param sq Any sequence
   * @param from range, it ends searching on from
   * @param end >=0: absolute exclusive end position for search, <0: end position relative to end, -1 is the end of src
   * @param ch to search
   * @return -1 if not found, elsewhere the position inside sq, >=fromIndex and < to 
   */
  public static int lastIndexOf(CharSequence sq, int from, int end, char ch){
    int zsq = sq.length();
    int ii = (end < 0 ? zsq + end +1 : (end > zsq ? zsq : end)) ;  //max is negative if to is left from fromIndex
    while(--ii >= from){
      if(sq.charAt(ii) == ch) {
        return ii;
      }
    }
    return -1;  //not found;
  }
  

  /**Searches the last occurrence of the given char in a CharSequence.
   * It is the adequate functionality like {@link java.lang.String#lastIndexOf(char)}. 
   * @param sq A CharSequence
   * @param str CharSequence which is searched.
   * @return -1 if not found, else first occurrence where sq.charAt(return) == ch. 
   */
  public static int lastIndexOf(CharSequence sq, char ch){
    return lastIndexOf(sq, 0, Integer.MAX_VALUE, ch);
  }

  
  
  /**Searches the last occurrence of one of the given chars in a CharSequence.
   * @param sq Any sequence
   * @param from range, it ends searching on from
   *   if from < 0 it throws IndexOutOfBoundsException
   * @param to start backward from here 
   *   if > sq.length() starts from end as well as to = -1
   *   if to < from then returns -1 always.
   *   if to < 0 then starts from end - to, use to = -1 to start from end to = -2 to start one for end
   * @param chars to search for
   * @return -1 if not found, elsewhere the position inside sq, >=fromIndex and < to 
   * @throws IndexOutOfBoundsException if from < 0 or to < 0
   */
  public static int lastIndexOfAnyChar(CharSequence sq, int from, int to, CharSequence chars){
    int zsq = sq.length();
    int ii = (to < 0 ? zsq + to +1 : (to >= zsq ? zsq : to)) ;  //ii is negative if to is left from fromIndex
    if(from <0) throw new IndexOutOfBoundsException("StringFunctions.lastIndexOfAnyChar - form <0; " + from);
    while(--ii >= from && indexOf(chars, sq.charAt(ii))<0); //pre-decrement.
    return ii >= from? ii : -1;  //not found;
  }
  

  /**Searches the last occurrence in a CharSequence of any char which is not given in chars.
   * @param sq Any sequence
   * @param from range, it ends searching on from
   *   if from < 0 it throws IndexOutOfBoundsException
   * @param to start backward from here 
   *   if > sq.length() starts from end as well as to = -1
   *   if to < from then returns -1 always.
   *   if to < 0 then starts from end - to, use to = -1 to start from end to = -2 to start one for end
   * @param chars to search for, for example " \t\n\r" to search the last non-whitespace chararcter
   * @return -1 if not found, elsewhere last position inside sq, >=fromIndex and < to which is not a char from chars 
   * @throws IndexOutOfBoundsException if from < 0 or to < 0
   */
  public static int lastIndexOfNoChar(CharSequence sq, int from, int to, CharSequence chars){
    int zsq = sq.length();
    int ii = (to < 0 ? zsq + to +1 : (to >= zsq ? zsq : to)) ;  //ii is negative if to is left from fromIndex
    if(from <0) throw new IndexOutOfBoundsException("StringFunctions.lastIndexOfAnyChar - form <0; " + from);
    while(--ii >= from && indexOf(chars, sq.charAt(ii))>=0); //pre-decrement.
    return ii >= from? ii : -1;  //not found;
  }
  

  /**Checks whether the given CharSequence contains the other given CharSequence.
   * It is the adequate functionality like {@link java.lang.String#indexOf(String, int)}. 
   * @param sq A CharSequence
   * @param fromIndex first checked position in sq
   * @param to >=0 then end position of checked range inclusive str
   *        to <0 then relative position from end. to = -1 exact till end
   * @param str  which is searched. If it is "", fromIndex is returned (found at first position).
   * @return -1 if not found, else first occurrence of str in sq which is >= fromIndex. 
   */
  public static int indexOf(CharSequence sq, int fromIndex, int to, CharSequence str){
    int zsq = sq.length();
    int max = (to < 0 ? zsq + to +1 : (to >= zsq ? zsq : to)) - str.length()+1 ;  //max is negative if to is left from fromIndex
    int ii = fromIndex-1;  //pre-increment
    if (fromIndex < 0) {  // what about this?
        ii = -1;
    } else if (fromIndex >= max) {
        return -1;
    }
    if(str.length()==0) {   // special case maybe in special context, str to search is ""
      return fromIndex;     // then return the current position.
    }
    char ch = str.charAt(0);   //search first char of str
    while(++ii < max){
      if(sq.charAt(ii) == ch) { //search first char of str
        int s1 = 0;
        for(int jj = ii+1; jj < ii + str.length(); ++jj){
          if(sq.charAt(jj) != str.charAt(++s1)){
            s1 = -1; //designate: not found
            break;
          }
        }
        if(s1 >=0) return ii;  //found.
      }
    }
    return -1;  //not found;
  }

  
  
  /**Searches the first occurrence of the given CharSequence in a CharSequence.
   * It is the adequate functionality like {@link java.lang.String#indexOf(String, int)}. 
   * @param sq A CharSequence
   * @param str CharSequence which is searched.
   * @param fromIndex first checked position in sq
   * @return -1 if not found, else first occurrence where {@link #equals(CharSequence sq, int return , int MAX_VALUE, CharSequence str)} ==0. 
   */
  public static int indexOf(CharSequence sq, CharSequence str, int fromIndex){
    return indexOf(sq, fromIndex, Integer.MAX_VALUE, str);
  }
  
  
  /**Searches the first occurrence of the given CharSequence in a CharSequence.
   * It is the adequate functionality like {@link java.lang.String#indexOf(String)}. 
   * @param sq A CharSequence
   * @param str CharSequence which is searched.
   * @return -1 if not found, else first occurrence where {@link #equals(CharSequence sq, int return , int MAX_VALUE, CharSequence str)} ==0. 
   */
  public static int indexOf(CharSequence sq, CharSequence str){
    return indexOf(sq, 0, Integer.MAX_VALUE, str);
  }
  
  
  
  
  
  /**Checks whether the given CharSequence contains the other given CharSequence.
   * Note: The algorithm and source lines are the same like in {@link #lastIndexOfAnyChar(CharSequence, int, int, String)}.
   * The difference is by translating to C source.
   * @param sq Any sequence where to search in
   * @param from range, it ends searching on from
   * @param to >=0: absolute exclusive end position for search, <0: end position relative to end, -1 is the end of src
   * @param str comparison sequence, check whether contained fully.
   *            If it is  "", the most right position is returned (found at right).
   *            That is not identically to, the real length of sq is considerate.
   * @return -1 if not found, elsewhere the position inside sq >=fromIndex and <= to - str.length()
   */
  public static int lastIndexOf(CharSequence sq, int fromIndex, int to, CharSequence str){
    int zsq = sq.length();
    int max = (to < 0 ? zsq + to +1 : (to >= zsq ? zsq : to)) - str.length()+1 ;  //max is negative if to is left from fromIndex
    if (fromIndex >= max) {
      return -1;
    }
    if(str.length()==0) {   // special case maybe in special context, str to search is ""
      return max;           // then return the current right position.
    }
    char ch = str.charAt(0);
    while(--max >= fromIndex){
      if(sq.charAt(max) == ch) {
        int s1 = 0;
        for(int jj = max+1; jj < max + str.length(); ++jj) {
          if(sq.charAt(jj) != str.charAt(++s1)){
            s1 = -1; //designate: not found
            break;
          }
        }
        if(s1 >0) return max;  //found.
      }
    }
    return -1;  //not found;
  }
  
  
  
  
  
  /**Returns the position of one of the strings in listStrings within the given sq, maybe started inside the sq with from,
   *  returns -1 if the char is not found in the part started from 'fromIndex'.
   * @param from begin of search within the part.
   * @param to >=0: absolute exclusive end position for search, <0: end position relative to end, -1 is the end of src
   * @param listStrings contains some Strings to find.
   * @param nrofFoundString If given, [0] is set with the number of the found String in listStrings, 
   *                        count from 0. This array reference may be null, then unused.
   * @param foundString If given, [0] is set with the found String. This array reference may be null.
   * @return either -1 if not found, or position of first founded char inside sq in range from..to
   */
  public static int indexOfAnyString
  ( CharSequence sq
  , int from, int to
  , CharSequence[] listStrings
  , @Java4C.SimpleVariableRef int[] nrofFoundString
  , @Java4C.SimpleVariableRef String[] foundString
  )
  { int pos = from; // + fromWhere;
    int zsq = sq.length();
    int end = (to < 0 ? zsq + to +1 : (to >= zsq ? zsq : to));  //max is negative if to is left from fromIndex
    assert(listStrings.length < 100);  //static size is need
    /** @xxxjava2c=stackInstance.*/
    @Java4C.StackInstance 
    StringBuilder sFirstCharBuffer = new StringBuilder(100);
    boolean acceptToEndOfText = false;
    //while(iter.hasNext())
    /**Compose a String with all first chars, to test whether a current char of src is equal. */
    { int ii = -1;
    while(++ii < listStrings.length)
    { CharSequence sString = listStrings[ii];
      if(sString.charAt(0) == cEndOfText)
      { acceptToEndOfText = true;}
      else 
      { sFirstCharBuffer.append(sString.charAt(0)); }  //to search the first char as one of chars
    } }
    /**@java2c=toStringNonPersist.*/
    String sFirstChars = sFirstCharBuffer.toString();
    boolean found = false;
    while(!found && pos < end)
    { 
      int nrofFoundString1 = -1;
      /**increment over not matching chars, test all first chars: */
      while(pos < end && (nrofFoundString1 = sFirstChars.indexOf(sq.charAt(pos))) < 0) pos +=1;
      
      if(pos < end)
      { /**a fist matching char is found! test wether or not the whole string is matched.
         * Test all Strings, the first test is the test of begin char. */
        int ii = -1;
        while(!found && ++ii < listStrings.length)  //NOTE: don't use for(...) because found is a criterium of break.
        { CharSequence sString = listStrings[ii];
          int testLen = sString.length();
          if((end - pos) >= testLen 
              && StringFunctions.equals(sq, pos, pos+testLen, sString)
          ) 
          { found = true;
          if(foundString != null)
          { foundString[0] = sString.toString();
          }
          if(nrofFoundString != null)
          { nrofFoundString[0] = ii;
          }
          }
          //else { nrofFoundString1 +=1; }
        }
        if(!found){ pos +=1; }  //check from the next char because no string matches.
        
      }
    }
    if(pos > end 
        || (pos == end && !acceptToEndOfText)
    ) { //nothing found 
      pos = -1; 
      if(foundString != null)
      { foundString[0] = null;
      }
      if(nrofFoundString != null)
      { nrofFoundString[0] = -1;
      }
    }
    return pos;
  }

  
  
  
  
  
  /**Returns the position of one of the strings in listStrings within the given sq, maybe started inside the sq with from,
   *  returns -1 if the char is not found in the part started from 'fromIndex'.
   * @param from begin of search within the part.
   * @param to >=0: absolute exclusive end position for search, <0: end position relative to end, -1 is the end of src
   * @param listStrings contains some Strings to find.
   * @param nrofFoundString If given, [0] is set with the number of the found String in listStrings, 
   *                        count from 0. This array reference may be null, then unused.
   * @param foundString If given, [0] is set with the found String. This array reference may be null.
   * @return either -1 if not found, or position of first founded char inside sq in range from..to
   */
  public static int lastIndexOfAnyString
  ( CharSequence sq
  , int from, int to
  , CharSequence[] listStrings
  , @Java4C.SimpleVariableRef int[] nrofFoundString
  )
  { int zsq = sq.length();
    int end = (to < 0 ? zsq + to +1 : (to >= zsq ? zsq : to));  //max is negative if to is left from fromIndex
    assert(listStrings.length < 100);  //static size is need for C language
    /** @xxxjava2c=stackInstance.*/
    @Java4C.StackInstance 
    StringBuilder sFirstCharBuffer = new StringBuilder(100);
    boolean acceptToEndOfText = false;
    //while(iter.hasNext())
    /**Compose a String with all first chars, to test whether a current char of src is equal. */
    for( int ii = 0; ii < listStrings.length; ++ii) { 
      CharSequence sString = listStrings[ii];
      if(sString.charAt(0) == cEndOfText) { 
        acceptToEndOfText = true;
      } else { 
        sFirstCharBuffer.append(sString.charAt(0));   //to search the first char as one of chars
      }
    }
    /**@java2c=toStringNonPersist.*/
    String sFirstChars = sFirstCharBuffer.toString();
    boolean found = false;
    int pos = end;
    CharSequence check;
    int ixFoundString = -1;
    while(!found && --pos >=from) {
      ixFoundString = 0;
      while(!found && ixFoundString >=0 && ixFoundString < listStrings.length) {
        ixFoundString = sFirstChars.indexOf(sq.charAt(pos), ixFoundString+1);
        if(ixFoundString >=0) {
          check = listStrings[ixFoundString];
          int zCheck = check.length();
          if(pos + zCheck <= end && compare(sq, pos, check, 0, zCheck)==0) {
            found = true;
          }
        }
      }
    } // while
    if(pos < from) { // not found:
      pos = -1;
      ixFoundString = -1;
    }
    if(nrofFoundString !=null && nrofFoundString.length >=1) {
      nrofFoundString[0] = ixFoundString;
    }
    return pos;
  }

  
  
  
  
  
  
  
  
  
  
  
  
  
  /**Resolves the given String containing some transcription chars (usual backslash) 
   * to a string with the appropriate character codes.
   * In the result String all char-pairs beginning with the transciptionChar are replaced by
   * one char. If the String doesn't contain a transcriptChar, the method returns the input string
   * in a as soon as possible calculation time.
   * <ul>
   * <li>The transcript char following by n r t f b will converted to the known control character codes:
   * <li>\\ is the backslash itself.
   * <li>\n newline 0x0a, \r return 0x0d, \t tabulator 0x09, \f form feed 0x0c, b backspace 0x08
   * <li>\s will converted to a single space. It is useful in input situations
   *     where a space will have another effect.
   * <li>\a will converted to the code 0x02, known in this class {@link #cStartOfText}.
   *     It is useful wether a String may be contain a code for start of text.
   * <li>\e will converted to the code 0x03, known in this class {@link #cEndOfText}.
   * <li>\W will converted to the code 0x04, known in this class {@link #cNoCidentifier}.
   * <li>\x0123 Convert from given hex code TODO
   * <li>All other chars after the transcription char will be converted to the same char, 
   *     for example "\{" to "{". Don't use this feature for normal alphabetic chars
   *     because some extensions in a future may be conflict with them. But this feature
   *     may be useful if an input text uses the special characters in a special way.
   * </ul> 
   * @param src The input string
   * @param transcriptChar the transcript character, usual a '\\'
   * @return The output string with replaces backslash pairs. It is a non-referenced StringBuilder
   *   if the src contains transcription chars, it is src itself if src does not contain transcription chars.
   */
  //why excluded ? JcHartmut 2016-02-14 yet problems with CharSequence 
  @Java4C.Exclude
  @Java4C.ReturnInThreadCxt
  public static CharSequence convertTransliteration(CharSequence src, char transcriptChar)
  { CharSequence sResult;
    int posSwitch = indexOf(src,0, src.length(), transcriptChar);
    if(posSwitch < 0)
    { sResult = src;
    }
    else
    { //escape character is found before end
      @Java4C.StringBuilderInThreadCxt(sign="StringFunctions.convertTransliteration")
      StringBuilder sbReturn = new StringBuilder(src);
      while(posSwitch >=0)
      { if(posSwitch < sbReturn.length()-1)
        { sbReturn.deleteCharAt(posSwitch);
          /*do not delete a \ as last character, because the next algorithm failed
           *in such case. The \ will kept. It is a possible input sequence of a user,
           *and it shouldn't be throw an IndexOutofBoundaryException!
           */  
        }
        char cNext = sbReturn.charAt(posSwitch);
        int iChangedChar;
        if( (iChangedChar = "snrtfb".indexOf(cNext)) >=0)
        { sbReturn.setCharAt(posSwitch, " \n\r\t\f\b".charAt(iChangedChar));
        }
        else if( cNext == 'a')
        { // \a means end of file, coded inside with 4 = EOT (end of transmission).
          sbReturn.setCharAt(posSwitch, cStartOfText);
        }
        else if( cNext == 'e')
        { // \e means end of file, coded inside with 4 = EOT (end of transmission).
          sbReturn.setCharAt(posSwitch, cEndOfText);
        } else if( cNext == 'W')
        { // \W means a non-word character like in regulare expressions.
          sbReturn.setCharAt(posSwitch, cNoCidentifier);
        }
        else if( cNext == 'x') {
          int[] zParsedChars = new int[1];
          int nr = StringFunctions_C.parseIntRadix(src, posSwitch+2, 4, 16, zParsedChars);
          sbReturn.setCharAt(posSwitch, (char)nr);
          sbReturn.delete(posSwitch+1, posSwitch +1 + zParsedChars[0]);  // let one char on posSwitch remain.
        }
        else
        { //the char after cEscape is valid and not changed!
        }
        posSwitch = sbReturn.toString().indexOf(transcriptChar, posSwitch+1);
      }
      sResult = sbReturn;
    }
    return sResult;
  }

  
  
  
  
  static String indentString = "\n                                                                                                    ";
  
  /**Returns a String with 2*indent spaces for indentation.
   * If indent is >50, return only 100 spaces. 
   * @param indent indentation
   * @return
   */
  public static String indent2(int indent){
    if(2*indent < indentString.length()-1){ return indentString.substring(1, 1 + 2*indent); }
    else return indentString.substring(1);
  }
  
  /**Returns a String with a newline \n character and 2*indent spaces for indentation.
   * If indent is >50, return only 100 spaces. 
   * @param indent indentation
   * @return
   */
  public static String nl_indent2(int indent){
    if(2*indent < indentString.length()-1){ return indentString.substring(0, 1 + 2*indent); }
    else return indentString;
  }
  
  
  /**Returns the number of bytes to the UTF start byte.
   * <ul>
   * <li>0x00..0x7f: 1 byte UTF8. First byte contains 7 bit. Result 7 bit.
   * <li>0x80..0xbf: It is any second byte of a UTF8 stream. This routine returns 0. Any following byte contains 6 bit.
   * <li>0xc0..0xdf: 2 byte UTF8. First byte contains 5 bit. Result 12 bit. 0x0fff..0x0
   * <li>0xef..0xe0: 3 byte UTF8. First byte contains 4 bit. Result 16 bit  0xffff..0, This it the range of UTF-16
   * <li>0xf7..0xf0: 4 byte UTF8. First byte contains 3 bit. Result 21 bit   
   * <li>0xfb..0xf8: 5 byte UTF8. First byte contains 2 bit. Result 26 bit   
   * <li>0xfd..0xfc: 6 byte UTF8. First byte contains 1 bit. Result 31 bit   
   * <li>0xfe:       7 byte UTF8. First byte contains 0 bit. Result 36 bit   
   * <li>0xff:       8 byte UTF8. First byte contains 0 bit. Result 42 bit   
   * <li>
   * <li>The first byte does not have the bits 10xx xxxx (range 0x80..0xbf). Then return 0, the ixSrc[0] will not be incremented. 
   * </ul>
   * @param b any byte of a UTF8 stream.
   * @return 1..8 for a valid UTF8 start character. 0 for a character which is not a start character. 
   */
  public static int nrofBytesUTF8(byte b) {
    if( b >=0) return 1;
    if((b & 0xe0)==0xc0) return 2;
    if((b & 0xf0)==0xe0) return 3;
    if((b & 0xf8)==0xf0) return 4;
    if((b & 0xfc)==0xf8) return 5;
    if((b & 0xfe)==0xfc) return 6;
    if( b == 0xfe) return 7;
    if( b == 0xff) return 8;
    else return 0;  //all codes 80..BF = 10xxxxxx
  }
  
  
  
  
  
  
  /**Converts the current bytes in a byte[] from UTF-8 in a UTF16-character.
   * @deprecated, replaced with {@link #utf8to16(byte[], int[])}
   * @param src
   * @param ixSrc
   * @return
   */
  public static short byte2UTF8(byte[] src, int[] ixSrc) {
    return utf8to16(src, ixSrc);
  }
  
  /**Converts the current bytes in a byte[] from UTF-8 in a UTF16-character.
   * <br>
   * Special code error situations: 
   * <ul>
   * <li>The first byte does not have the bits 10xx xxxx (range 0x80..0xbf). Then return 0, the ixSrc[0] will not be incremented. 
   * <li>The following bytes must have the bits 10xx xxxx. If not, then return 0 with the ixSrc[0] on the position of the errorneous byte.
   * <li>Characters outside UTF16: The character (short)(0xfffd) is returned, but the bytes are correctly skipped, 
   *   so the next character start byte is referred by the ixSrc[0].
   * </ul>
   * If that error occurs, the routine returns 0 and the ixDst[0] refers the faulty byte. With comparison 
   * with the start index before invocation, the error can be ...TODO
   *   
   * @param src any byte array
   * @param ixSrc [0] The current position in this byte array. It should refer the first byte of a UTF-8-coded character.
   *   The ixSrc[0] will be incremented by the processed bytes for this one character. If the routine does not return 0,
   *   than the ixSrc[0] refers the first byte of the next UTF8 character, or the end of the array.  
   * @return the character from the read UTF-8 code bytes. It is returned as short (or int16 in C). It can be casted to char
   *   for Java applications. For C programming the handling of UTF16 is a special case.
   *   Special cases: return 0 if byte[ixSrc[0]] does not contain a valid UTF-8 code sequence. 
   *   
   */
  public static short utf8to16(byte[] src, int[] ixSrc)
  { byte b = src[ixSrc[0]];
    if(b >=0) { return (short) b; }
    //
    if( (b & 0xc0) == 0x80) return 0;
    //
    byte b2 = src[ixSrc[0]];
    if( (b2 & 0xc0) != 0x80) return 0;
    ixSrc[0] +=1;
    int cc = b; cc <<=6; cc |= b2 & 0x3f;
    if((b & 0xe0)==0xc0) { 
      return (short)(cc & 0x7ff);  //remove 3 MSB from b.
    }
    //
    byte b3 = src[ixSrc[0]];
    if( (b3 & 0xc0) != 0x80) return 0;
    ixSrc[0] +=1;
    cc <<=6; cc |= b3 & 0x3f;
    if((b & 0xf0)==0xe0) { 
      return (short)(cc & 0xffff);  //remove 3 MSB from b.
    }
    //
    //That is an higher UTF character than UTF16-range:
    //returns '?' but skip over that bytes. 
    byte b4 = src[ixSrc[0]];
    if( (b4 & 0xc0) != 0x80) return 0;
    ixSrc[0] +=1;
    if((b & 0xf8)==0xf0) { return (short)(0xfffd); }
    //    
    byte b5 = src[ixSrc[0]];
    ixSrc[0] +=1;
    if( (b5 & 0xc0) != 0x80) return 0;
    if((b & 0xfc)==0xf8) { return (short)(0xfffd); }
    //    
    byte b6 = src[ixSrc[0]];
    if( (b6 & 0xc0) != 0x80) return 0;
    ixSrc[0] +=1;
    if((b & 0xfe)==0xfc) { return (short)(0xfffd); }
    //    
    byte b7 = src[ixSrc[0]];
    if( (b7 & 0xc0) != 0x80) return 0;
    ixSrc[0] +=1;
    if((b & 0xff)==0xfe) { return (short)(0xfffd); }
    //    
    assert(b == 0xff);
    byte b8 = src[ixSrc[0]++];
    if( (b8 & 0xc0) != 0x80) return 0;
    ixSrc[0] +=1;
    return (short)(0xfffd);
  }

  
  
  /**Converts a UTF16 character to 1..3 bytes UTF8
   * @param cc The character
   * @param buffer for bytes
   * @param posBuffer first write position in buffer. The buffer size should be at least posBuffer +3
   * @return number of written bytes.
   * See {@link https://en.wikipedia.org/wiki/UTF-8}, {@linkplain https://de.wikipedia.org/wiki/UTF-8}
   */
  int utf16toUTF8bytes(char cc, byte[] buffer, int posBuffer) {
    int ix = posBuffer;
    int mask, sh;
    int ci = (int)cc;  //the value
    if(ci >=0x1000) {  //uses 3 byte output
      sh = 12;
      buffer[ix++] = (byte)(0xe0 | (ci >> sh));  //result e0..ef 1. byte
    }
    else if(ci >= 0x0080) {
      sh = 6;
      buffer[ix++] = (byte)(0xc0 | (ci >> sh));  //result e0..ef 1. byte
    }
    else {
      sh = 0;
      buffer[ix++] = (byte)(ci);  //result e0..ef 1. byte
    }
    while( (sh-=6) >=0) {
      buffer[ix++] = (byte)(0x80 | ((ci >> sh) & 0x3f));
    }
    return ix - posBuffer;
  }
  
}
