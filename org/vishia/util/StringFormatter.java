/****************************************************************************/
/* Copyright/Copyleft: 
 * 
 * For this source the LGPL Lesser General Public License, 
 * published by the Free Software Foundation is valid.
 * It means:
 * 1) You can use this source without any restriction for any desired purpose.
 * 2) You can redistribute copies of this source to everybody.
 * 3) Every user of this source, also the user of redistribute copies 
 *    with or without payment, must accept this license for further using.
 * 4) But the LPGL ist not appropriate for a whole software product,
 *    if this source is only a part of them. It means, the user 
 *    must publish this part of source,
 *    but don't need to publish the whole source of the own product.
 * 5) You can study and modify (improve) this source 
 *    for own using or for redistribution, but you have to license the
 *    modified sources likewise under this LGPL Lesser General Public License.
 *    You mustn't delete this Copyright/Copyleft inscription in this source file.    
 *
 * @author www.vishia.de/Java
 * @version 2007-09-12  (year-month-day)
 * list of changes: 
 * 2009-05-03: Hartmut bugfix in strPicture: nr of inserted chars was incorrect.
 * 2009-03-10: Hartmut new: addDateSeconds() and addDate()
 * 2007-09-12: JcHartmut www.vishia.org creation copy from vishia.stringScan.StringFormatter
 * 2008-04-28: JcHartmut setDecimalSeparator() to produce a gemans-MS-Excel-friendly format.
 *
 ****************************************************************************/
package org.vishia.util;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;


/**This class supports a formatted output of something in a string buffer.
 * In completion to the capability of java.lang.String.format() (since Java-5) and the class java.util.Formatter
 * this class provides<ul>
 * <li>a presentation of some hex formats and memory content as complete line.</li>
 * <li>a so named picture driven formating, see {@link setIntPicture(long, String)} or {@link setFloatPicture(long, String)}</li>
 * <li>the possibility of set and add content</li>
 * </ul>
 * To merge java.util.Formatter and this class you should assign the same StringBuffer to both classes.
 * <br>
 * The class contains add...-methods and knows internally a current position. 
 * The current position can be setted with {@link pos(int)}. 
 * In the default overwrite mode the add methods do not insert in buffer with shifting the rest to right 
 * (like StringBuffer.insert()), but they overwrite the content at the currrent position. 
 * The wording 'add' means, the current position is increment, so the next add()-operation adds 
 * something behind the previous add()-operation. In the insert mode the content at pos is shifted to right.
 * <br>
 * Every {@link pos(int)}-operation is successfull. If the buffer in shorter as the required position, spaces will be filled
 * onto the required position. So a buffer content can also be filled first right, than left.
 */
public class StringFormatter
{
  private static final byte mNrofBytesInWord = 0x1F;

  /** If this bit is set in mode, the byte with the lower index is interpreted as higher part of word
   * and is written left in insertHexLine(). Otherwise the byte with the lower index is interpreted
   * as higher lower part of word and is written right.
   */
  private static final byte mBytesInWordBigEndian = 0x20;

  /*
  public static final short k1BytePerWordLittleEndian = 1;
  public static final short k2BytePerWordLittleEndian = 2;
  public static final short k4BytePerWordLittleEndian = 4;
  public static final short k1BytePerWordBigEndian = mBytesInWordBigEndian + k1BytePerWordLittleEndian;
  public static final short k2BytePerWordBigEndian = mBytesInWordBigEndian + k2BytePerWordLittleEndian;
  public static final short k4BytePerWordBigEndian = mBytesInWordBigEndian + k4BytePerWordLittleEndian;
  */
 
  /**The constant determine the number of digits representing a (hex) value and the decision, use first byte left or right side.
   * left = first byte of a byte[] array is written left sided (like big endian coding),
   * right = first byte of a byte[] array is written right sided (like necessary in 
   */ 
  public static final short k1 = 1,
                            k2right = 2, k2left = 2 + mBytesInWordBigEndian,
                            k4right = 4, k4left = 4 + mBytesInWordBigEndian,
                            k6right = 6, k6left = 6 + mBytesInWordBigEndian,
                            k8right = 8, k8left = 8 + mBytesInWordBigEndian
                            ;
  
  private static final String spaces = "                                                          ";
  
  protected final StringBuffer buffer;

  /**The position of actual writing.
   * 
   */
  protected int pos = 0;

  /**True than add inserts, false than it overwrites. */
  private boolean bInsert = false;
  
  
  private char cDecimalSeparator = '.';
  
  public StringFormatter()
  { buffer = new StringBuffer();
  }


  
  /**Constructs an instance with a StringBuffer of the given length.
   * @param length lenngth of the internal StringBuffer.
   */
  public StringFormatter(int length)
  { buffer = new StringBuffer(length);
  }


  
  /**Constructs an instance with a StringBuffer initialized with the given string.
   * @param str initial value.
   */
  public StringFormatter(String str)
  { buffer = new StringBuffer(str);
  }


  /**Constructs an instance with a existing StringBuffer.
   * The StringBuffer content is unchanged first. The StringBuffer may be shared between several StringFormatter
   * and can also be written directly.
   * @param buffer The buffer.
   */
  public StringFormatter(StringBuffer buffer)
  { this.buffer = buffer;
  }


  /**Same as getContent, overwrites Object.toString(). 
   * Don't use it instead getContent(), use it only for debugging goals.
   * @implementInfo: optimize-toString in not set here, it may be set outside. 
   */
  public String toString()
  { return buffer.toString();
  }
  
  /**Gets the accumulated content.
   * 
   * @return The string representing the accumulated content.
   * @implementInfo: optimize-toString in not set here, it may be set outside. 
   */
  public String getContent()
  { return buffer.toString();
  }

  /**Sets an deviant decimal separator for floating point digigs, country-specific. */
  public void setDecimalSeparator(char sep)
  { cDecimalSeparator = sep;
  }
  
  

  /**Resets the internal buffer. If it is called after usage of the String getting with getContent(),
   * no additional space is used.
   *
   */
  public StringFormatter reset()
  { pos = 0;
    buffer.setLength(pos);
    return this;
  }
  

  /**Sets the current position to the end of the string. */
  public StringFormatter end()
  { pos = buffer.length();
    return this;
  }
  

  
  /**Sets the current write position to the given position. */
  public StringFormatter pos(int newPos)
  { if(newPos < 0) throw new IndexOutOfBoundsException("negativ position not supported");
    pos = newPos;
    int pos1 = buffer.length();
    while(pos1 < pos )
    { buffer.append(' '); pos1 +=1;
    }
    return this;
  }
  
  
  /**returns the current length of string. */
  public int length(){ return buffer.length(); }
  
  
  /**returns the current position for add in buffer.
   */
  public int getPos(){ return pos; }

  /** Adds at the current position a string.
   *
   * @param str String
   * @return this
   */
  public StringFormatter add(String str)
  { int nrofChars = str.length();
    prepareBufferPos(nrofChars);
    buffer.replace(this.pos, pos + nrofChars, str);
    pos += nrofChars;
    return this;
  }
  
  
  /** Adds at the current position a char[].
   *
   * @param str char array. 0-chars from backward are not added.
   * @return this
   */
  public StringFormatter add(char[] str)
  { int nrofChars = str.length;
    while(nrofChars >1 && str[nrofChars-1] == 0){ nrofChars -=1; }
    prepareBufferPos(nrofChars);
    for(int ii = 0; ii < nrofChars; ii++)
    { buffer.setCharAt(this.pos, str[ii]);
      this.pos += 1;
    }  
    return this;
  }
  
  
  /**Inserts a String at current position with shifting the actual content to right.
   * 
   * @param str
   * @return
   */
  public StringFormatter insert(String str)
  { buffer.insert(pos,str);
    pos += str.length();
    return this;
  }
  
  /**sets the overwrite mode. It is the default. In this mode add will overwrite the current content. */
  public StringFormatter overwrite(){ bInsert = false; return this; }
  
  /**sets the insert mode. In this mode add will shift the content at pos to right. */
  public StringFormatter insert(){ bInsert = true; return this; }
  
  /**sets the insert or overwrite mode, returns the current mode before change.
   * @param insert true than insert, false than overwrite.
   * @return true if insert was current, false if overwrite was current.
   */
  public boolean setInsertMode(boolean insert)
  { boolean bInsertRet = this.bInsert;
    this.bInsert = insert;
    return bInsertRet;
  }
  
  /** Adds a line of ascii representation of bytes. If the code is less than 0x20 (control chars),
   * a '.' is shown instead.
   *
   * @param data byte data
   * @param idx offset
   * @param bytes nr of bytes
   * @param charsetName encoding, typical "ISO-8859-1" or "US-ASCII", using "UTF-8" more as one byte may be present 1 char.
   * @return
   */
  public StringFormatter addStringLine(byte[] data, int idx, int nrofBytes, String charsetName)
  { //to convert bytes with a given charset, but show bytes < 0x20 with '.', copy it in a independend buffer:
    byte[] data1 = new byte[nrofBytes];
    System.arraycopy(data, idx, data1, 0, nrofBytes);
    for(int ii = 0; ii < nrofBytes; ii++)
    { if(data1[ii]< 0x20){ data1[ii] = (byte)('.'); } //write insteads control chars.
    }
    String str;
    try{ str = new String(data1, 0, nrofBytes, charsetName); }
    catch(UnsupportedEncodingException exc)
    { str = "??encoding error??"; }
    //not replace in buffer:
    int strLength = str.length(); //it should be equal nrofBytes, but not in all charsets.
    prepareBufferPos(strLength);
    buffer.replace(pos, pos + strLength, str);
    pos += strLength;
    return this;
  }
  
  
  /** Adds at the current position a line of hex numbers.
   *
   * @param data byte data
   * @param idx offset
   * @param bytes nr of bytes
   * @param mode mode see {@link k1} to {@link k8Right}
   * @return this
   * @java2c=return-this.
   */
  public StringFormatter addHexLine(final byte[] data, final int idx, final int nrofBytes, final short mode)
  { int nrofBytesInWord = mode & mNrofBytesInWord;
    int nrofWords = nrofBytes / nrofBytesInWord;
    
    prepareBufferPos(2 * nrofBytes + nrofWords);
    int nrofBytes1 = nrofBytes;
    int idx1 = idx;
    while(nrofBytes1 > 0)
    { if( nrofBytes1 < nrofBytesInWord)
      { //the last hex word is smaller as given in mode: 
        addHexWord_(data, idx1, (short)((mode & mBytesInWordBigEndian) + nrofBytes1));
        nrofBytes1 = 0;
      }
      else
      { //normal operation
        addHexWord_(data, idx1, mode); 
        buffer.setCharAt(pos++,' ');
        nrofBytes1 -= nrofBytesInWord;
        idx1 += nrofBytesInWord;
      }  
    }
    return this;
  }


  
  /**Adds a hexa line with left address and ascii
  *
  * @param addr a fitive address which may be shown the position of data
  * @param data byte data
  * @param idx offset
  * @param bytes nr of bytes
  * @param mode mode see addHex
  * @return
  *
   public StringFormatter addHexLineWithAddrAndAscii(int addr, byte[] data, int idx, int bytes, short mode)
   { int nrofBytesInWord = mode & mNrofBytesInWord;
     addHex(addr, mode);
     buffer.insert(pos++, ": ");
     while(bytes > 0)
     { if( bytes < nrofBytesInWord){ nrofBytesInWord = bytes;}
       addHex(data, idx, mode);
       buffer.insert(pos++,' ');
       bytes -= nrofBytesInWord;
       idx += nrofBytesInWord;
     }
     return this;
   }
  
  */
  
  /** Adds a number as one word readed from data in hexa form
   *
   * @param data byte data
   * @param idx offset
   * @param mode nr of bytes and big/little endian, use k4BytePerWordLittleEndian etc.
   * @return this itself
   */
  public StringFormatter addHexWord(byte[] data, int idx, short mode)
  { prepareBufferPos(2 * (mode & mNrofBytesInWord));
    return addHexWord_(data, idx, mode);
  }
  
  /** Adds a number as one word readed from data in hexa form, internal routine without prepareBufferPos 
   *
   * @param data byte data
   * @param idx offset
   * @param mode nr of bytes and big/little endian, use k4BytePerWordLittleEndian etc.
   * @return this itself
   */
  private StringFormatter addHexWord_(byte[] data, int idx, short mode)
  { int nrofBytesInWord = mode & mNrofBytesInWord;
    int incrIdx;
    if((mode & mBytesInWordBigEndian) != 0){ incrIdx = 1; }
    else { incrIdx = -1; idx += nrofBytesInWord -1;}
    while(--nrofBytesInWord >= 0)
    { byte value = data[idx];
      idx += incrIdx;  //TRICKY may be 1 or -1 dependend on BigEndian
      for(int i=0; i<2; i++)
      { char digit = (char)(((value & 0xf0)>>4) + (byte)('0'));
        if(digit > '9'){ digit = (char)(digit + (byte)('a') - (byte)('9') -1); }
        buffer.setCharAt(pos++, digit);
        value <<=4;
      }
    }
    return this;
  }

  
  /** Adds a number containing in a long variable in hexa form
  *
  * @param value the value
  * @param mode Ones of k1BytePerWordBigEndian to k4BytePerWordLittleEndian resp. k1MSD to k8LSD
  * @return this itself
  */
   public StringFormatter addHex(long value, int nrofDigits)
   { prepareBufferPos(nrofDigits);
     { //show last significant byte at right position, like normal variable or register look
       int nrofShift = (nrofDigits * 4) -4;
       for(int ii=0; ii < nrofDigits; ii++)
       { char digit = (char)(((value>>nrofShift)&0x0f) + (byte)('0'));
         if(digit > '9'){ digit = (char)(digit + (byte)('a') - (byte)('9') -1); }
         buffer.setCharAt(pos++, digit);
         nrofShift -=4;
       }
       
     }
     return this;
   }
  
   
  /** Adds a number containing in a long variable in hexa form
   *
   * @param value the value
   * @param mode Ones of k1BytePerWordBigEndian to k4BytePerWordLittleEndian resp. k1MSD to k8LSD
   * @param sPicture String as picture of output. A char 1 means, output the bit. Other chars are copied in output.
   * @param sBitCharLo Characters for lo bit at the same position like sPicture
   * @param sBitCharHi Characters for hi bit at the same position like sPicture
   * @return this itself
   */
  public StringFormatter addBinary(int value, String sPicture, String sBitCharLo, String sBitCharHi)
   { int nrofDigits = 0;
     for(int ii=0; ii< sPicture.length(); ii++)
     { if(sPicture.charAt(ii)=='1'){ nrofDigits +=1; } 
     }
     int mask = 1 << (nrofDigits-1);
     prepareBufferPos(sPicture.length());
     for(int ii=0; ii< sPicture.length(); ii++)
     { char cBitPos = sPicture.charAt(ii);
       if(cBitPos =='1')
       { int bit = value & mask;        
         char cc = bit != 0 ? sBitCharHi.charAt(ii) : sBitCharLo.charAt(ii);  
         buffer.setCharAt(pos++, cc);
         mask = (mask >> 1) & 0x7FFFFFFF; 
       }
       else 
       { buffer.setCharAt(pos++, cBitPos ); 
       }
     }
     return this;
   }
  
   
   
  /**ensures, that the space in buffer started on pos is writeable with setCharAt.
  * If the buffer content is less than pos, spaces were padded.
  * @param nrofDigits after pos to write somewhat.
  */
  private void prepareBufferPos(int nrofChars)
  { //if(true || bInsert)
    if(bInsert && pos < buffer.length())
    {
      while(nrofChars >0)
      { if(nrofChars >= spaces.length()){ buffer.insert(pos, spaces); nrofChars -=spaces.length();}
        else { buffer.insert(pos, spaces, 0, nrofChars); nrofChars = 0; }
      }      
      //buffer.insert(pos, spaces, 0, nrofChars);
    }
    else
    { int nrofCharsToEnd = buffer.length() -pos;
      assert(nrofCharsToEnd >=0);
      nrofChars -= nrofCharsToEnd;
      //nrofChars may be < 0 if the range of overwrite is inside the exiting string.
      while(nrofChars >0)
      { //appends necessary space on end. the format methods overwrites this space.
        if(nrofChars >= spaces.length()){ buffer.append(spaces); nrofChars -=spaces.length();}
        else { buffer.append(spaces, 0, nrofChars); nrofChars = 0; }
      }  
    }
    /*
    else
    { int posEnd = pos + nrofChars;
      int length = buffer.length();
      while(length < posEnd )
      { buffer.append(' '); length++;
      }
    } 
    */ 
  }
   
   
   
   
  
   /**Adds a number in form 12ab'cd34, it is typical to show 4-byte-values at example addresses.
    * 
    */
   public StringFormatter addHex44(long value)
   { addHex((value >> 16) & 0xffff, 4);
     buffer.insert(pos++, '\'');
     addHex((value) & 0xffff, 4);
     return this;
   }
  
  
   
  /**adds a double value in a fix point representation without exponent.
   * 
   * @param value The value
   * @param digitsBeforePoint Number of digits before decimal separator. 
   *        If the number of digits of the value is less, spaces will be outputted instead. 
   *        The decimal separator will be set at the same position everytime independent of the value.
   * 
   * @param digitsAfterPoint Number of digits after decimal separator. All digits will be shown. 
   * @return this itself.
   */
  public StringFormatter addFloat(double value, int digitsBeforePoint, int digitsAfterPoint)
  { int nrofCharsInPicture = digitsBeforePoint + digitsAfterPoint + 2;  //sign and dot
    prepareBufferPos(nrofCharsInPicture);
    if(value < 0)
    { buffer.setCharAt(pos++, '-');
      value = -value;
    }
    else
    { buffer.setCharAt(pos++, ' ');
    }
    String sValue = Double.toString(value);
    int posPointInValue = sValue.indexOf('.');
    if(cDecimalSeparator != '.')
    { sValue = sValue.replace('.', cDecimalSeparator);
    }
    //int posPoint = pos + digitsBeforePoint;
    int nrofSpacesBefore = digitsBeforePoint - posPointInValue;
    int nrofZeroAfter = digitsAfterPoint - (sValue.length() - posPointInValue -1);
    if(nrofZeroAfter < 0)
    { nrofZeroAfter = 0; 
    }
    int nrofValueChars = digitsBeforePoint - nrofSpacesBefore + 1 + digitsAfterPoint - nrofZeroAfter ; 
    while(nrofSpacesBefore >0)
    { buffer.setCharAt(pos++, ' ');
      nrofSpacesBefore -=1;
    }
    //int digitsAfterPointInValue =sValue.length() - posPointInValue -1;
    //if(digitsAfterPointInValue > digitsAfterPoint){ digitsAfterPointInValue = digitsAfterPoint;}
    if(nrofSpacesBefore < 0)
    { //the number of digits is to large,
      nrofValueChars = nrofValueChars - (-nrofSpacesBefore)-2;
      //crash situation: write only the beginn of the digit
      buffer.replace(pos, pos+2, "##");
      pos +=2;
    }
    buffer.replace(pos, pos+ nrofValueChars, sValue.substring(0, nrofValueChars));
    pos += nrofValueChars; 

    while(--nrofZeroAfter >=0)
    { buffer.setCharAt(pos++, '0');
    }

    return this;    
  }
  
  

  
  
  
  
  
  /**Adds a line with representation of byte content in a fixed nice format.
   * Use a combination of {@link addHexLine(byte[], int, int, int)} and [{@link addStringLine(byte[], int, int, String)}
   * to write a special defined format. This method writes 4-digit hex values lo byte right and the ASCII-represantation
   * inclusive a \n char on end. It is a static method working internal with StringFormatter.
   * */
  public static String addHexLn(byte[] data, int length, int idxStart)
  { int idx = idxStart;
    StringFormatter buffer = new StringFormatter();
    String strRet = "";
    
    while(idx < (idxStart + length))
    { int idxLineEnd = idx + 32;
      if(idxLineEnd > length){ idxLineEnd = idxStart + length; }
      buffer.addHexLine(data, idx, idxLineEnd - idx, StringFormatter.k4right);
      buffer.add(" ");
      buffer.addStringLine(data, idx, idxLineEnd - idx, "ISO-8859-1");
      strRet += buffer.getContent() + "\n";
      
      buffer.reset();
      idx = idxLineEnd;
    }
    return strRet;
  }    


  
  
  
  
  public StringFormatter add(char ch)
  { prepareBufferPos(1);
    buffer.setCharAt(this.pos++, ch);
    return this;
  }
  
  
  public StringFormatter addDate(Date date, SimpleDateFormat format)
  { String sDate = format.format(date);
    //String sDate = format.format(date, sDate, );
    add(sDate);  
    return this;
  }
  
  
  /*
  public StringFormatter addDateSeconds
  ( int seconds
  , boolean isGPS
  , SimpleDateFormat formatDate
  )
  { Date date = isGPS ? LeapSeconds.dateFromGPS(1000L*seconds) : new Date(1000*seconds);
    String sDate = formatDate.format(date);
    add(sDate);
    return this;
  }
  */
  

  
  
  
  public StringFormatter setAt(int pos, char ch)
  { buffer.setCharAt(pos, ch);
    return this;
  }
  
  
  /**Sets a integer value at current position, use the picture to determine the number of characters etc.
   * 
   * <br>
   * The presentation of values using a so named 'picture' is supported. The picture is a String showing the representation of a number.
   * Following some examples. Note: a character <code>_</code> means a space.
   * <table border=1><tr><th rowspan=2>picture</th><th colspan=5>Example numbers</th></tr>
   * <tr><td>0</td><td>1</td><td>-123</td><td>12345</td><td>12345678</td></tr>
   * 
   * <tr><td rowspan=2><code>+2221.111</code></td>
   *   <td colspan=5>A integer number will be shown with exactly 7 digits. A point is set between digits at the shown position.
   *       The sign is shown everytime, as <code>+</code> or <code>-</code><br>
   *       The number shows like a float point number. At example the value in integer is a milli-Value,
   *       the user can read it in a comfortable format.</td></tr>
   * <tr><td><code>+___0.000</code></td><td><code>+___0.001</code></td><td><code>-___0.123</code></td><td><code>+__12.2345</code></td><td><code>+####.###</code></td></tr>  
   *
   * <tr><td rowspan=2><code>+3321.111</code></td>
   *   <td colspan=5>A integer number will be shown with a maximum of 7 digits, but a minimum of 5 digits. 
   *       The sign is shown only if it is necessary. Because leftside there is a <code>3</code> a positive sign needs no space.</td></tr>
   * <tr><td><code>_0.000</code></td><td><code>_0.001</code></td><td><code>-_0.123</code></td><td><code>12.345</code></td><td><code>##.###</code></td></tr>  
   *
   * </table>
   * The digits of the number are shown at any position of digit in the picture, 
   * The last significant digit is placed at right side of digit in the picture. 
   * Followed rules of meaning of the chars are in force: 
   * <table border=1><tr><th>char</th><th>meaning</th></tr> 
   * <tr><td><code>0</code></td><td>The digit 0 in number is alway shown, also leftside or rightside.</td></tr>     
   * <tr><td><code>1</code></td><td>A leftside ditit 0 in number is shown as 0, but a rightside 0 is shown as space.</td></tr>     
   * <tr><td><code>2</code></td><td>A leftside or rightside digit 0 of number is shown as space, any middle digit 0 is shown as 0.</td></tr>     
   * <tr><td><code>3</code></td><td>In differece to <code>2</code>, a leftside or rightside digit 0 produces no output. With the number of <code>3</code>-digits, 
   *                                the maximal shown number of digits is limited. At example with a picture "<code>3331.03</code>" only a maximum of 7 digits are shown,
   *                                but a minimum of 3 digits.</td></tr>     
   * </table>
   * @param value A value as integer, it is a long type (64 bit). Range from -9223372036854775808 to 9223372036854775807, about 10 power 19.
   *        It is a possible style to apply float values with multiplication to show float values with a fix position of decimal point. 
   * @param picture The picture of digit.
   * @return this to support concatenation.
   */
  public StringFormatter addint(long nr,String sPict)  //Zahl anhngen, rechtsbndig nlen Zeichen oder mehr
  { strPicture(nr,sPict,"+-..", '.');
    return this;
  }
  
  /**@deprecated see {@link addint(long nr,String sPict)}*/
  public StringFormatter addIntPicture(long nr,String sPict)  //Zahl anhngen, rechtsbndig nlen Zeichen oder mehr
  { strPicture(nr,sPict,"+-..", '.');
    return this;
  }
  
  /**Array with power of 10 to detect the exponent size of a long value. */
  private final static long[] n10a =                      
    {1000000000L
    ,100000000L
    ,10000000L
    ,1000000L
    ,100000L
    ,10000L
    ,1000L
    ,100L
    ,10L};
    
  static String sNeg="+-%";  //dieses Zeichen im Picture (xxx Erweit. auch andere Zeichen wie %)
  
  /**This algorithm is taken over from C++ routines in strpict.cpp written by JcHartmut in 1993..1999.
   * 
   * @param src The number to show
   * @param pict Picture of the number
   * @param posNegPointExp String with 4 Character, 
   *        which are used for positive sign, negative sign, decimal point or exponent char.
   *        At ex. "+-.@".
   *        If the pict contains one of this characters, the associated function will be done. 
   * @param cFracSep Character which is set instead a exponent char
   * @return
   */
  private boolean strPicture
  ( long src      //numerisch
  , String pict  //Bild der Zahl
  , String posNegPointExp
  , char cFracSep     //Index auf Zeichen anstelle eines zweimal. Trenners
  )
  { int n10i;  //Index auf das n10-Feld waehrend der Konvertierung
    //--------------------------------------------------------------------
    /**set if the input number is negativ, and it is negated. */
    boolean bNeg=false;
    /**number of chars for the sign, it is 0 or 1. */
    int nrofCharForSign;
    
    /**1 if a '-' for sign position is given and the number is positiv. */
    int nrofCharsForSignUnused = 0;
    /** setted if left zero-digits are suppressed, no '0' and no space should be shown. */
    boolean bLeftZeroSuppress =false;
    StringPart spPict = new StringPart(pict);
    int posSignInPicture = spPict.indexOfAnyChar(sNeg);  //positChar(pict,pict.length(),sNeg,strlen(sNeg));
    if(posSignInPicture >= 0)  //im Picture ist ein neg. Vorzeichen vorgesehen
    { if(src < 0L)                //und die Zahl ist auch negativ:
      { bNeg=true;
        src=-src;   //Zahl negieren
        nrofCharForSign = 1;
      }
      else
      { if(pict.charAt(posSignInPicture) != '-')
        { nrofCharForSign = 1;  //displays the sign always.
        }
        else
        { nrofCharForSign = 0;  //don't display a sign.
          nrofCharsForSignUnused = 0;
        }
      }
    }
    else if(src < 0L)
    { throw new IllegalArgumentException("value should be only positive: " + src);
    }
    //if the number is negativ but a sign is not expected, the number will be shown as positiv value.
    //
    //----------------------------------------------------------------------
    //Feststellung der Groesse der Zahl
    for(n10i = n10a.length -1; n10i >= 0; n10i--)
    { //meistens sind es kleine Zahlen, im Mittel geht es also schneller
      //wenn von Hinten aus getestet wird ob die Zahl groesser ist,
      //damit weniger Schleifendurchlauefe:
      if(src < n10a[n10i]) break;
    }
    //n10[n10i] ist die Zahl, die um eine Stelle groeser ist.
    n10i+=1;  //damit ist n10[n10i] die als erste kleinere Zahl.
              //Achtung: n10i > arraylen(n10) wenn src <10, nur Einerstelle!
    //--------------------------------------------------------------------
    //Anzahl der Stellen
    int nDigits = n10a.length - n10i + 1;
  
    //--------------------------------------------------------------------
    //pict analysieren:
    int nrofChars = pict.length(); 
    int ii = nrofChars;     //beginne von rechts
    int n0Digit=0;   //max. Digits rechts, weggelassen wenn 0
    int n1Digit=0;   //mdst. Stelle mit 0 auszuschreiben falls Zahl kleiner ('1' im Picture)
    int n2Digit=0;   //mindest-Platz fuer Digits bzw. linke Leerstellen
    int n3Digit=0;   //max. Anzahl Digits
  
    while(ii>0)
    { char cp = pict.charAt(--ii);
      if(cp<='2' && cp>='0')
      { n2Digit+=1;      //210 in Picture: Soll-Platz fuer Digits
        n3Digit+=1;
        if(cp=='0'){ n0Digit+=1; n1Digit=n2Digit; }  //mdst. Stelle auszuschreiben
        else if(cp=='1') n1Digit=n2Digit;
      }
      else if(cp<='9' && cp>='3')
      { n3Digit+=1;
      }
    }
    
    boolean bOvf;         //Zahl ist nicht darstellbar weil zu gross
    if(nDigits > n3Digit)
    { //Zahl ist nicht darstellbar: stattdessen 99999 darstellen
      bOvf=true;
      //n3Digit=0;
      n2Digit = n3Digit;
    }
    else
    { bOvf=false;
      if(nDigits > n2Digit) n2Digit=nDigits;  //Anzahl auszugeb. Digits oder Leerstellen
    }
    prepareBufferPos(nrofChars - (n3Digit - n2Digit) - nrofCharsForSignUnused);
    char cp;
    ii = 0;
    for(ii=0; ii < nrofChars; ii++)
    { cp = pict.charAt(ii);
      char cc;
      int ixPosNegPointExp;
      if( cp>='0' && cp<='9')
      { if(--n3Digit >= n2Digit) cc=0;  //keine Ausgabe weil nicht notwendige fuehr. Stellen
        else
        { //Ausgabe aufgrund n2Digit notwendig
          if(n2Digit>nDigits)
          { //Anzahl auszuschreib. Stellen groesser als Zahl:
            if(n1Digit>=n2Digit) cc='0';   //fuerende Null
            else cc=' ';
          }
          else //nDigit<=n2Digit
          { //Ziffer bestimmen:
            n1Digit=0;  //keine fuerenden 0 mehr notwendig
            if(bOvf) cc='#';
            else if(src==0)
            { if(n0Digit>=nDigits) cc=0;  //nichts ausgeben bei weglassbaren nachfolg. 0
              else cc='0';
            }
            else if(n10i >= n10a.length)
            { cc=(char)(src+'0');  //das ist die Einerstelle
            }
            else
            { long src10=n10a[n10i]; n10i+=1;    //Dezimalstelle gehoert dazu
              cc='0';
              while( src>=src10){ cc+=(char)(1); src-=src10; } //in Schleife subtr. statt Divis.
            }
            nDigits-=1;
          }
          n2Digit-=1;
        }
      }
      else if( (ixPosNegPointExp = posNegPointExp.indexOf(cp)) >=0){
      	/**Any control character found: */
      	switch(ixPosNegPointExp){
      	case 0: cc = posNegPointExp.charAt(bNeg? 1 : 0); break; //positiv digit
      	case 1: { 
          if(bNeg)
          { //number is negativ, write a '-' always.
            cc = cp; 
          }
          else
          { //number is positive:
            if(bLeftZeroSuppress){ cc=0; }  //write nothing if number is positiv and left zeros are suppressed.
            else { cc = ' ';}    //write blank if a negative sign is required in picture and the number is positive.
          }
      	} break;
      	case 2: cc = cFracSep;  break; //show the given fractional separator if the control-char for fract. separator is found.
      	case 3: cc = cFracSep == '.' ? ' ' : cFracSep; break; //don't show if 10^0
      	default: throw new RuntimeException("unexpected case");
      	}
      }
      else
      { cc = cp; //anderes Zeichen aus Picture uebertragen
      }
      if(cc!=0)
      { //cc=0 means, the char shouls not be written.
        buffer.setCharAt(pos++, cc);
      }
    }//for
    return(!bOvf);
  }


  /**Writes a float value in technical representation with exponent as short char a..T
   * NOTE: This algorithm is taken over from C++ routines in strpict.cpp written by JcHartmut in 1993..1999.
   * <br><br>
   * The representation of the number uses always three digits left of point. 
   * The exponent is shown as short character:
   * <ul>
   * <li>a, f p, n, u, m for ato, femto, pico, nano, micro, milli (10^-18 to 10^-3)
   * <li>. or space if the exponent is 0,
   * <li>k, M, G, T for kilo, Mega, Giga, Terra (10^3 to 10^12).
   * </ul>
   * The parameters nDigit and mode controls the representation:
   * <table><th><td>nDigit, mode</td><td>
   * @param src The number
   * @param pict picture of the float number.
   * @return Number of chars written (to calculate column width).
   */
  public int addFloatPicture
  ( float src        //numerical number unsigned long
  , String pict //Erscheinung
  )
  { //Exponent der Zahl bestimmen:
    String cFrac = "afpnum.kMGT";
    int nExp = cFrac.indexOf('.');
    //array of max. normalized float for difficult nr of digits
    //final float[] aMax={0.001F, 0.01F, 0.1F, 1.0F, 10.0F, 100.0F, 1000.0F, 10000.0F, 100000.0F, 1000000.0F};
    int srcHex = Float.floatToRawIntBits(src);
    byte nExpF=(byte)((srcHex>>24) & 0x7f);  //Test auf NAN usw.
    if(nExpF >(40+0x40))
      //ueber +/-2E13, insbesond. NAN
      src=999.9999E15F;
    else if( nExpF< (0x40-40) )
    	src = 0.0F; //the 0 itself is a 0.
    //unused(nExpF + *pSrcHex);
    boolean bNeg=(srcHex <0); //(src != 0.0F && src < 0.0F);
    boolean bIsNull = (srcHex & 0x7F800000) == 0;
    long srcLong;
    if(bNeg) src=-src;

    if(!bIsNull)  //src!=0.0)  //bei src=0.0 muss "0.0" ausgeg. werden, Sonderfall.  #04
    { while(src >= 1000.0F && nExp < cFrac.length()-1)
      { src=src/1000.0F; nExp+=1;
      }
      while(src < 1.0F && nExp > 0)
      { src=src*1000.0F; nExp-=1;
      }
      if(bNeg) src=-src;
      srcLong=(long)(src);
    }
    else
    {
      srcLong = 0;
    }
    strPicture(srcLong,pict,"+-.@", cFrac.charAt(nExp));
    return(pict.length());
  }



}
