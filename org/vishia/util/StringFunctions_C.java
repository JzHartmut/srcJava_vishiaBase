package org.vishia.util;

import java.io.IOException;
import java.nio.charset.Charset;

/**Contains routines to convert String to number format.
 * @author Hartmut Schorrig
 *
 */
public class StringFunctions_C
{
  /**Version, history and license.
   * <ul>
   * <li>2022-03-29 Hartmut new: {@link #appendIntPict(Appendable, long, String)}, for that 
   *   the {@link #strPicture(long, String, String, char, PrepareBufferPos)} was moved and adapted from {@link StringFormatter} to here,
   *   the internal interface {@link PrepareBufferPos}  allows the adaption (it is simple).
   *   That is the basic to append a number in a specific outfit, TODO use also for float and double.
   * <li>2022-03-29 Hartmut new: {@link #appendHex(Appendable, long, int)} Algorithm copied from {@link StringFormatter#addHex(long, int)}
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
  public final static String version = "2022-05-24"; 

  
  
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

  
  
  
  /**Append a value in hexa with given number of digits.
   * @param out to append
   * @param value
   * @param nrofDigits if <0 then upper case digits A..F are used, else a..f
   * @return out to further append
   * @throws IOException
   */
  public static Appendable appendHex ( Appendable out, long value, int nrofDigits) throws IOException {
    final int ctDigits;
    final char hexBase;
    if(nrofDigits < 0) {
      hexBase = 'A';
      ctDigits = -nrofDigits;
    } else { 
      hexBase = 'a';
      ctDigits = nrofDigits; 
    }
    { //show last significant byte at right position, like normal variable or register look
      int nrofShift = (ctDigits * 4) -4;
      for(int ii=0; ii < ctDigits; ii++)
      { char digit = (char)(((value>>nrofShift) & 0x0f) + (byte)('0'));
        if(digit > '9'){ digit = (char)(digit + (byte)(hexBase) - (byte)('9') -1); }
        out.append(digit);
        nrofShift -=4;
    } }
    return out;
  }
  
  
  
  
  
  
  
  
  /**Array with power of 10 to detect the exponent size of a long value. */
  private static final long[] n10a =                      
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
   * @param pict picture or outfit, for example "+33221.11"
   *   <ul><li>a character which is also contained in posNegPointExp can be used for special functions.
   *   <li>"1" the digit is always written, write "0" on leading 0
   *   <li>"2" A space is written on a leading 0
   *   <li>"3" A leading 0 is omitted. All other digits are written.
   *   <li>"4".."9", "0" no meaning
   *   <li>All other character are written as given.
   *   <ul>
   * @param posNegPointExp String with 4 character which are contained in the pict, usual "+-.E", can be null
   *        or lesser if the function is not used.
   *        This arguments clarifies the meaning of this special characters.
   *        If the character is in the pict and is also found here, it has the following meaning
   *        depending on the position where it is found:
   *        <ul>
   *        <li>[0] sign also present for a positive number. Use it, use instead posNegPointExp[1] if the number is negative.
   *        <li>[1] sign only present for a negative number. Omit it if the number is positive.
   *        <li>[2] character used as decimal point only relevant for float
   *        <li>[3] character used for the exponent only relevant for float 
   * @param cFracSep Character which is used instead a exponent char
   * @return
   * @throws IOException 
   */
  /*package private */ static boolean strPicture
  ( long src      //numerisch
  , String pict  //Bild der Zahl
  , String posNegPointExp
  , char cFracSep     //Index auf Zeichen anstelle eines zweimal. Trenners
  , PrepareBufferPos bufferPos
  ) throws IOException
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
    int posSignInPicture = StringFunctions.indexOfAnyChar(pict, 0, Integer.MAX_VALUE, sNeg);;  //positChar(pict,pict.length(),sNeg,strlen(sNeg));
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
    bufferPos.prepareBufferPos(nrofChars - (n3Digit - n2Digit) - nrofCharsForSignUnused);
    ii = 0;
    for(ii=0; ii < nrofChars; ii++)
    { char cp = pict.charAt(ii);
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
      else if( posNegPointExp !=null && (ixPosNegPointExp = posNegPointExp.indexOf(cp)) >=0){
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
        bufferPos.addBufferPos(cc);
      }
    }//for
    return(!bOvf);
  }
  
  
  
  /**Appends a long value with a dedicated outfit
   * @param appendable append here
   * @param src the number
   * @param pict picture or outfit, for example "+33221.11"
   *   <ul><li>+ or - is used as position for a sign, write anytime + or - on "+", omit sign on "-" for a positive number. 
   *   <li>"1" the digit is always written, write "0" on leading 0
   *   <li>"2" A space is written on a leading 0
   *   <li>"3" A leading 0 is omitted. All other digits are written.
   *   <li>"4".."9", "0" no meaning
   *   <li>All other character are written as given.
   *   <ul>
   *   For example you can place a decimal point oth 1000^th^ separator to better read the digit.   
   * @return The appendable argument to append more in concatenation.
   * @throws IOException not expected
   */
  public static Appendable appendIntPict
  ( Appendable appendable
  , long src      //numerisch
  , String pict  //Bild der Zahl
  ) throws IOException {
    PrepareAppendablePos bufferPos = new PrepareAppendablePos(appendable);
    strPicture(src, pict, "+-", '\0', bufferPos);
    return appendable;
  }
  
  
  
  /**This interface is used also for {@link StringFormatter}
   * to use similar approaches for setCharAt and append.
   *
   */
  public interface PrepareBufferPos {
    
    /**Ensures that on the destination {@link #addBufferPos(char)} is applicable.
     * This operation can be empty on a simple appendable.
     * It should be fullfilled if a given position is used.
     * @param nrofChars planned number of characters to add at position.
     */
    void prepareBufferPos(int nrofChars);
    
    
    /**Adds a character either on end (Appendable#append)
     * or on a defined position
     * @param cc
     * @throws IOException
     */
    void addBufferPos(char cc)  throws IOException;
  }
  
  
  /**Implementation of the {@link PrepareBufferPos} for a simple Appendable
   */
  private static class PrepareAppendablePos implements PrepareBufferPos {

    private final Appendable out;
    
    /**Use this Appendable
     * @param out
     */
    PrepareAppendablePos(Appendable out){
      this.out = out;
    }
    
    /**Does nothing, nothing necessary. */
    @Override public void prepareBufferPos(int nrofChars) {
    }

    /**Append the character. */
    @Override public void addBufferPos(char cc) throws IOException {
      this.out.append(cc);
    }
    
  }
  
  
  
}
