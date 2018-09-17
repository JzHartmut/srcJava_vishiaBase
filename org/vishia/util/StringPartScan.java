package org.vishia.util;

import java.text.ParseException;



/**This class extends the capability of StringPartBase for scanning capability.
 * In opposite to the {@link StringPart#seek(int)} functionality with several conditions 
 * the scan methods does not search till a requested char or string but test the string
 * starting from the begin of the valid part. If the test is ok, the begin of the valid part
 * is shifted to right behind the scanned string. The result of the scanning process
 * may be evaluated later. Therefore it is stored in this class, for example {@link #getLastScannedIntegerNumber()}
 * can be gotten after scan.
 * <pre>
 * abcdefghijklmnopqrstuvwxyz  Sample of the whole associated String
 *   --------------------    The valid part before scan
 *         ++++++++++++++    The valid part after scan
 *   ******                  The successfully scanned part.
 *         xxxxx             Starting next scan      
 * </pre> 
 * A scan works with the current valid part always.
 * <br><br>  
 * <b>concatenated sequence of scans</b>:<br>
 * It is possible to concatenate scans, for example
 * <pre>
 *   sp.scanStart();
 *   if(sp.scan("keyword").scan('=').scanIdentifier().scanOk()){
 *     String sIdent = sp.getLastScannedString().toString();
 *   } else if(sp.scan("other-keyword").scan(.....
 * </pre>
 * The following rule is valid:
 * <ul>
 * <li>The operations are concatenated, because any operation returns this.
 *   It is a nice-to-have writing style.
 * <li>If a scan fails, the following scan operations are not executed.
 * <li>{@link #scanOk()} returns false if any of the scan methods after {@link #scanStart()}
 *   or the last {@link #scanOk()} fails.
 * <li>If a {@link #scanOk()} was invoked and the scan before that fails, the begin of the valid part
 *   is set to that position where the scan has started before this scan sequence. It is the position
 *   where {@link #scanStart()} was called or the last {@link #scanOk()} with return true was called.
 * </ul>
 * With them a well readable sequential test of content can be programmed in the shown form above.
 * In a sequence of scans white space and comments may be skipped over if the method
 * {@link #setIgnoreWhitespaces(boolean)} etc. are invoked before. That setting is valid for all following
 * scan invocations.
 *     
 * @author Hartmut Schorrig
 *e
 */
public class StringPartScan extends StringPart
{
  /**Version, history and license.
   * <ul>
   * <li>2017-09-17 Hartmut new: {@link #seekScanPos(int)} invokes {@link StringPart#seekPos(int)} but returns this as StringPartScan to concatenate.
   *   It is the first typical routine for concatenation. TODO more. The problem is, the routines from {@link StringPart} can be invoked, but not simple concatenate.
   * <li>2017-12-30 Hartmut new: {@link #getLastScannedPart()}
   * <li>2017-07-02 Hartmut new: {@link #scanStart(boolean)} possible to switch on bSkipWhitepaces. More simple for invocation.
   * <li>2016-09-25 Hartmut chg: {@link #scan()} now invokes {@link #scanStart()} automatically, it is the common use case.
   * <li>2016-02-13 Hartmut bugfix: {@link #scanFractionalNumber(long, boolean)} has had a problem with negative numbers. 
   *   Therefore {@link #scanFractionalNumber(long, boolean)} with bNegative as argument. Used in {@link CalculatorExpr} too.
   *   new {@link #getLastScannedIntegerSign()} to check whether "-0" was scanned which may be "-0.123" as a negative number.  
   * <li>2014-12-12 Hartmut chg: Comment: {@link #scanOk()} cannot used nested! It should only used on user level. 
   *   Elsewhere the scan start position is erratic changed. Don't use it in {@link #scanFloatNumber()}. 
   * <li>2014-12-06 Hartmut new: {@link #scanFractionalNumber(long)} enables scanning first an integer, then check whether
   *   it is a possibility to detect whether an intgeger or a float value is given.
   * <li>2014-12-06 Hartmut new: {@link #scanSkipSpace()} and {@link #scanSkipComment()} calls {@link #seekNoWhitespace()()} etc
   *   but returns this to concatenate. 
   * <li>2013-10-26 Hartmut creation from StringPart. Same routines, but does not use substring yet, some gardening, renaming. 
   * <li>1997 Hartmut: The scan routines in approximately this form were part of the StringScan class in C++ language,
   *   written of me.
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
   * <li> But the LPGL ist not appropriate for a whole software product,
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
  public final static int version = 20131027; 

  
  /**Position of scanStart() or after scanOk() as begin of next scan operations. */
  protected int beginScan;
  
  /**Position of last scanStart() after scanOk().
   * The last scanned part is from beginScanLast till beginScan. */
  protected int beginScanLast;
  
  /**Buffer for last scanned integer numbers.*/
  protected final long[] nLastIntegerNumber = new long[5];
  
  /**Buffer for last scanned signs of integer numbers.*/
  protected final boolean[] nLastIntegerSign = new boolean[5];
  
  /**current index of the last scanned integer number. -1=nothing scanned. 0..4=valid*/
  private int idxLastIntegerNumber = -1;
  
  /**Last scanned float number*/
  protected final double[] nLastFloatNumber = new double[5];
  
  /**current index of the last scanned float number. -1=nothing scanned. 0..4=valid*/
  private int idxLastFloatNumber = -1;
  
  /** Last scanned string. */
  protected CharSequence sLastString;
  

  
  public StringPartScan(CharSequence src, int begin, int end)
  { super(src, begin, end);
  }

  public StringPartScan(CharSequence src)
  { super(src);
  }

  
  public StringPartScan()
  { super();
  }

  
  /**Sets the current begin to pos relativ to the current part.
   * It calls {@link #seekPos(int)} and returns this.
   * @param pos
   * @return
   */
  public StringPartScan seekScanPos(int pos) {
    super.seekPos(pos);
    return this;
  }
  
  
  
  /**Skips over white spaces. It calls {@link StringPart#seekNoWhitespace()} and return this. */
  public final StringPartScan scanSkipSpace()
  { seekNoWhitespace();
    return this;
  }
  
  /**Skips over white spaces and comments. It calls {@link StringPart#seekNoWhitespaceOrComments()} and return this. */
  public final StringPartScan scanSkipComment()
  { seekNoWhitespaceOrComments();
    return this;
  }
  
  /**Starts the scan at current position
   * @param bSkipWhitepaces invokes {@link #setIgnoreWhitespaces(boolean)} with this argument.
   *   If true then whitespaces will be skipped by the next scan invocations.
   * @java2c=return-this.
   * @return this
   */
  public final StringPartScan scanStart(boolean bSkipWhitepaces)
  { setIgnoreWhitespaces(bSkipWhitepaces);
    bCurrentOk = true;
    scanOk();  //turn all indicees to ok
    return this;
  }

  /**Starts the scan at current position
   * @java2c=return-this.
   * @return
   */
  public final StringPartScan scanStart()
  { bCurrentOk = true;
    scanOk();  //turn all indicees to ok
    return this;
  }

  /**Invocation of scan() for a {@link StringPart} is the same than scanStart().
   * @see org.vishia.util.StringPart#scan()
   */
  @Java4C.Retinline @Override public final StringPartScan scan()
  { return scanStart();
  }
  
  
  
  private final boolean scanEntry()
  { if(bCurrentOk)
    { seekNoWhitespaceOrComments();
      if(bStartScan)
      { idxLastIntegerNumber = -1;
        //idxLastFloatNumber = -1;
        //idxLastFloatNumber = 0;
        //idxLastString = 0;
        bStartScan = false; 
      }
      if(begin == end)
      { bCurrentOk = false; //error, because nothing to scan.
      }
    }
    return bCurrentOk;
  }
  

  
  /**Test the result of scanning and set the scan Pos Ok, if current scanning was ok. If current scanning
   * was not ok, this method set the current scanning pos back to the position of the last call of scanOk()
   * or scanStart().
   * This method should only used in the user space to scan options. 
   * If it is not okay, the scan starts on the last position where it was okay, for the next option test:
   * <pre>
   * scanStart(); //call scanOk() independent of the last result. Set the scan start.
   * if(scanIdentifier().scanOk()) { //do something with the indentifier
   * } else if(scanFloat().scanOk()) { //a float is detected
   * } else if ....
   * </pre>
   * It is not yet possible for nested options.  
   * @return true if the current scanning was ok, false if it was not ok.
   */

  public final boolean scanOk()
  { if(bCurrentOk) 
    { beginScanLast = beginScan;
      beginScan =  beginLast = begin;    //the scanOk-position is the begin of maximal part.
      bStartScan = true;   //set all idxLast... to 0
    }
    else           
    { begin = beginLast= beginScan;   //return to the begin
    }
    //if(report != null){ report.report(6," scanOk:" + beginMin + ".." + begin + ":" + (bCurrentOk ? "ok" : "error")); }
    boolean bOk = bCurrentOk;
    bCurrentOk = true;        //prepare to next try scanning
    return(bOk);
  }


  
  /*=================================================================================================================*/
  /*=================================================================================================================*/
  /*=================================================================================================================*/
  /** scan next content, test the requested String.
   *  new since 2008-09: if sTest contains cEndOfText, test whether this is the end.
   *  skips over whitespaces and comments automatically, depends on the settings forced with
   *  calling of {@link #seekNoWhitespaceOrComments()} .<br/>
   *  See global description of scanning methods.
   * @java2c=return-this.
   *  @param sTest String to test
      @return this
  */
  public final StringPartScan scan(final CharSequence sTestP)
  { if(bCurrentOk)   //NOTE: do not call scanEntry() because it returns false if end of text is reached,
    {                //      but the sTestP may contain only cEndOfText. end of text will be okay than.
      seekNoWhitespaceOrComments();
      CharSequence sTest;
      int len = StringFunctions.indexOf(sTestP,StringFunctions.cEndOfText,0);
      int len2 = StringFunctions.indexOf(sTestP,StringFunctions.cNoCidentifier,0);
      boolean bTestToNoCidentifier = (len2 >=0);
      boolean bTestToEndOfText = (len >=0);
      if(bTestToNoCidentifier){ 
        sTest = sTestP.subSequence(0, len2); 
        len = len2;
      }
      else if(bTestToEndOfText){ 
        sTest = sTestP.subSequence(0, len); 
      }  //only one of the end symbols.
      else { len = sTestP.length(); sTest = sTestP; }
      char cc;
      if(  (begin + len) <= endMax //content.length()
        && StringFunctions.equals(content, begin, begin+len, sTest)
        && ( bTestToEndOfText ? begin + len == end  //should be the exact length
           : (bTestToNoCidentifier ? 
             (begin + len == end)   //either end of text
               || (   (cc = content.charAt(begin + len)) != '_'   //or not an identifier character.
                   && !(cc >= '0' && cc <='9') && !(cc >= 'A' && cc <='Z') && !(cc >= 'a' && cc <='z')) 
           : true))     
        )
      { begin += len;
      }
      else 
      { bCurrentOk = false; 
      }
    }
    return this;
  }


  
  /**
   * @java2c=return-this.
   * @param sQuotionmarkStart
   * @param sQuotionMarkEnd
   * @param sResult
   * @return
   */
  public final StringPartScan scanQuotion(CharSequence sQuotionmarkStart, String sQuotionMarkEnd, String[] sResult)
  { return scanQuotion(sQuotionmarkStart, sQuotionMarkEnd, sResult, Integer.MAX_VALUE);
  }
  
  
  /**
   * @java2c=return-this.
   * @param sQuotionmarkStart
   * @param sQuotionMarkEnd
   * @param sResult
   * @param maxToTest
   * @return
   */
  public final StringPartScan scanQuotion(CharSequence sQuotionmarkStart, String sQuotionMarkEnd, String[] sResult, int maxToTest)
  { if(scanEntry())
    { scan(sQuotionmarkStart).lentoNonEscapedString(sQuotionMarkEnd, maxToTest);
      if(bCurrentOk)
      { //TODO ...ToEndString, now use only 1 char in sQuotionMarkEnd
        if(sResult != null) sResult[0] = getCurrentPart().toString();
        else this.sLastString = getCurrentPart();
        fromEnd().seekPos(sQuotionMarkEnd.length());
      }
      else bCurrentOk = false; 
    }
    return this;
  }
  
  
  

  /**Scans if it is a integer number, contains exclusively of digits 0..9
      @param bHex true: scan hex Digits and realize base 16, otherwise realize base 10.
      @return long number represent the digits.
  */
  private final long scanDigits(boolean bHex, int maxNrofChars)
  { if(bCurrentOk)
    { long nn = 0;
      boolean bCont = true;
      int pos = begin;
      int max = (end - pos) < maxNrofChars ? end : pos + maxNrofChars;
      do
      {
        if(pos < max)
        { char cc = content.charAt(pos);
          if(cc >= '0' &&  cc <='9') nn = nn * (bHex? 16:10) + (cc - '0');
          else if(bHex && cc >= 'a' &&  cc <='f') nn = nn * 16 + (cc - 'a' + 10);
          else if(bHex && cc >= 'A' &&  cc <='F') nn = nn * 16 + (cc - 'A' + 10);
          else bCont = false;
          if(bCont){ pos +=1; }
        }
        else bCont = false;
      } while(bCont);
      if(pos > begin)
      { begin = pos;
        return nn;
        //nLastIntegerNumber = nn;
      }
      else { 
        bCurrentOk = false;  //scanning failed.
      }
    }
    return -1; //on error
  }


  
  /**Scanns a integer number as positiv value without sign. 
   * All digit character '0' to '9' will be proceed. 
   * The result as long value is stored internally
   * and have to be got calling {@link #getLastScannedIntegerNumber()}.
   * There can stored upto 5 numbers. If more as 5 numbers are stored yet,
   * an exception is thrown. 
   * @throws ParseException if the buffer is not free to hold an integer number.
   * @java2c=return-this.
   * @return
   */
  public final StringPartScan scanPositivInteger() throws ParseException  //::TODO:: scanLong(String sPicture)
  { if(scanEntry())
    { long value = scanDigits(false, Integer.MAX_VALUE);
      if(bCurrentOk)
      { if(idxLastIntegerNumber < nLastIntegerNumber.length -2)
        { nLastIntegerNumber[++idxLastIntegerNumber] = value;
          nLastIntegerSign[idxLastIntegerNumber] = false;
        }
        else throw new ParseException("to much scanned integers",0);
      }  
    } 
    return this;
  }

  /**Scans an integer expression with possible sign char '-' at first.
   * The result as long value is stored internally
   * and have to be got calling {@link #getLastScannedIntegerNumber()}.
   * There can stored upto 5 numbers. If more as 5 numbers are stored yet,
   * an exception is thrown. 
   * @throws ParseException if the buffer is not free to hold an integer number.
   * @java2c=return-this.
   * @return this
   */
  public final StringPartScan scanInteger() throws ParseException  //::TODO:: scanLong(String sPicture)
  { if(scanEntry())
    { boolean bNegativValue = false;
      if( content.charAt(begin) == '-')
      { bNegativValue = true;
        seek(1);
      }
      long value = scanDigits(false, Integer.MAX_VALUE);
      if(bNegativValue)
      { value = - value; 
      }
      if(bCurrentOk)
      { if(idxLastIntegerNumber < nLastIntegerNumber.length -2)
        { nLastIntegerNumber[++idxLastIntegerNumber] = value;
          nLastIntegerSign[idxLastIntegerNumber] = bNegativValue;
        }
        else throw new ParseException("to much scanned integers",0);
      }
    }  
    return this;
  }

  
  /**Scans a float number. The result is stored internally
   * and have to be got calling {@link #getLastScannedFloatNumber()}.
   * There can stored upto 5 numbers. If more as 5 numbers are stored yet,
   * an exception is thrown.
   * @param cleanBuffer true then clean the float number buffer because the values are not used. 
   * @java2c=return-this.
   * @return this
   * @throws ParseException if the buffer is not free to hold the float number.
   */
  public final StringPartScan scanFloatNumber(boolean cleanBuffer)  throws ParseException
  {
    if(cleanBuffer){
      idxLastFloatNumber = -1; 
    }
    scanFloatNumber();
    return this;
  }
  

  
  /**Scans a float / double number. The result is stored internally
   * and have to be got calling {@link #getLastScannedFloatNumber()}.
   * There can stored upto 5 numbers. If more as 5 numbers are stored yet,
   * an exception is thrown. 
   * @java2c=return-this.
   * @return this
   * @throws ParseException if the buffer is not free to hold the float number.
   */
  public final StringPartScan scanFloatNumber() throws ParseException  //::TODO:: scanLong(String sPicture)
  {
    if(scanEntry()) { 
      boolean bNegativValue = false;
      char cc;
      if( (cc = content.charAt(begin)) == '-')
      { bNegativValue = true;
        seek(1);
      }
      long nInteger = scanDigits(false, Integer.MAX_VALUE);
      //if(scanOk()) {  //if only an integer is scanned, it is a float too! set scanOk().
      if(bCurrentOk) {
        if(bNegativValue)
        { nInteger = - nInteger; 
        }
        scanFractionalNumber(nInteger, bNegativValue);
        //if(!scanFractionalNumber(nInteger).scanOk()){
        if(!bCurrentOk) {
          //only integer number found, store as float number. It is ok.
          bCurrentOk = true;
          if(idxLastFloatNumber < nLastFloatNumber.length -2){
            nLastFloatNumber[++idxLastFloatNumber] = (double)nInteger;
          } else throw new ParseException("to much scanned floats",0);
        }
      }
    }
    return this;
  }
  
  
  /**Scans the fractional part of a float / double number with given integer part and sign. 
   * The result is stored internally and have to be gotten calling {@link #getLastScannedFloatNumber()}.
   * <br><br>
   * Application-sample:
   * <pre>
   * if(spExpr.scanSkipSpace().scanInteger().scanOk()) {
      Value value = new Value();
      long longvalue = spExpr.getLastScannedIntegerNumber();
      if(spExpr.scanFractionalNumber(longvalue).scanOk()) {
        double dval = spExpr.getLastScannedFloatNumber();
        if(spExpr.scan("F").scanOk()){
          value.floatVal = (float)dval;
          value.type = 'F';
        } else {
          value.doubleVal = dval;
          value.type = 'D';
        }
      } else {
        //no float, check range of integer
        if(longvalue < 0x80000000L && longvalue >= -0x80000000L) {
          value.intVal = (int)longvalue; value.type = 'I';
        } else {
          value.longVal = longvalue; value.type = 'L';
        }
      }
   * </pre>
   * @java2c=return-this.
   * @return this
   * @throws ParseException if the buffer is not free to hold the float number.
   */
  public final StringPartScan scanFractionalNumber(long nInteger, boolean bNegative) throws ParseException  //::TODO:: scanLong(String sPicture)
  { if(scanEntry()) { 
      long nFractional = 0;
      int nDivisorFract = 1, nExponent = 0;
      //int nDigitsFrac;
      char cc;
      boolean bNegativExponent = false;
      double result;
      int begin0 = this.begin;
      if(begin < endMax && content.charAt(begin) == '.') {
        seek(1); //over .
        while(begin < endMax && getCurrentChar() == '0')
        { seek(1); nDivisorFract *=10;
        }
        //int posFrac = begin;
        nFractional = scanDigits(false, Integer.MAX_VALUE);  //set bCurrentOk = false if there are no digits.
        if(bCurrentOk)
        { //the it has set begin to end of scan position.
          //nDigitsFrac = begin - posFrac;
        }
        else if(nDivisorFract >=10)
        { bCurrentOk = true; //it is okay, at ex."9.0" is found. There are no more digits after "0".
          nFractional = 0;
        }
      }
      int nPosExponent = begin;
      if( bCurrentOk && nPosExponent < endMax && ((cc = content.charAt(begin)) == 'e' || cc == 'E'))
      { seek(1);
        if( (cc = content.charAt(begin)) == '-')
        { bNegativExponent = true;
          seek(1);
          cc = content.charAt(begin);
        }
        if(cc >='0' && cc <= '9' )
        { nExponent = (int)scanDigits(false, Integer.MAX_VALUE);  //set bCurrentOk if there are no digits
          if(!bCurrentOk)
          { nExponent = 0;
            assert(false);  //0..9 was tested!
          }
        }
        else
        { // it isn't an exponent, but a String beginning with 'E' or 'e'.
          //This string is not a part of the float number.
          begin = nPosExponent;
          nExponent = 0;
        }
      }
      if(begin > begin0) {
        //either fractional or exponent found
        result = nInteger;
        if(nFractional > 0)
        { double fFrac = nFractional;
          while(fFrac >= 1.0)  //the read number is pure integer, it is 0.1234
          { fFrac /= 10.0; 
          }
          fFrac /= nDivisorFract;    //number of 0 after . until first digit.
          if(result < 0 || bNegative) {  //bNegativ is set on negative 0
            fFrac = -fFrac;  //Should be subtract if integer part is negative!
          }
          result += fFrac;
        }
        if(nExponent != 0)
        { if(bNegativExponent){ nExponent = -nExponent;}
          result *= Math.pow(10, nExponent);
        }
        if(idxLastFloatNumber < nLastFloatNumber.length -2){
          nLastFloatNumber[++idxLastFloatNumber] = result;
        } else throw new ParseException("to much scanned floats",0);
      }
      else {  //whetter '.' nor 'E' found:
        bCurrentOk = false;
      }
    }
    return this;
  }

  
  /**Scans a sequence of hex chars a hex number. No '0x' or such should be present. 
   * See scanHexOrInt().
   * The result as long value is stored internally
   * and have to be got calling {@link #getLastScannedIntegerNumber()}.
   * There can stored upto 5 numbers. If more as 5 numbers are stored yet,
   * an exception is thrown. 
   * @throws ParseException if the buffer is not free to hold an integer number.
   * @java2c=return-this.
   */
  public final StringPartScan scanHex(int maxNrofChars) throws ParseException  //::TODO:: scanLong(String sPicture)
  { if(scanEntry())
    { long value = scanDigits(true, maxNrofChars);
      if(bCurrentOk)
      { if(idxLastIntegerNumber < nLastIntegerNumber.length -2)
        { nLastIntegerNumber[++idxLastIntegerNumber] = value;
          nLastIntegerSign[idxLastIntegerNumber] = false;
        }
        else throw new ParseException("to much scanned integers",0);
      }
    }
    return this;
  }

  /**Scans a integer number possible as hex, or decimal number.
   * If the number starts with 0x it is hexa. Otherwise it is a decimal number.
   * Octal numbers are not supported!  
   * The result as long value is stored internally
   * and have to be got calling {@link #getLastScannedIntegerNumber()}.
   * There can stored upto 5 numbers. If more as 5 numbers are stored yet,
   * an exception is thrown. 
   * @throws ParseException if the buffer is not free to hold an integer number.
   * @java2c=return-this.
   * @param maxNrofChars The maximal number of chars to scan, if <=0 than no limit.
   * @return this to concatenate the call.
   */
  public final StringPartScan scanHexOrDecimal(int maxNrofChars) throws ParseException  //::TODO:: scanLong(String sPicture)
  { if(scanEntry())
    { long value;
      if( StringFunctions.equals(content, begin, begin+2, "0x"))
      { seek(2); value = scanDigits(true, maxNrofChars);
      }
      else
      { value = scanDigits(false, maxNrofChars);
      }
      if(bCurrentOk)
      { if(idxLastIntegerNumber < nLastIntegerNumber.length -2)
        { nLastIntegerNumber[++idxLastIntegerNumber] = value;
          nLastIntegerSign[idxLastIntegerNumber] = false;
        }
        else throw new ParseException("to much scanned integers",0);
      }
    }
    return this;
  }

  
  /**Scans an identifier with start characters A..Z, a..z, _ and all characters 0..9 inside.
   * If an identifier is not found, scanOk() returns false and the current position is preserved.
   * The identifier can be gotten with call of {@link #getLastScannedString()}.
   * @java2c=return-this.
   * @return
   */
  public final StringPartScan scanIdentifier()
  { return scanIdentifier(null, null);
  }
  
  
  /**Scans an identifier with start characters A..Z, a..z, _ and all characters 0..9 inside,
   * and additional characters.
   * If an identifier is not found, scanOk() returns false and the current position is preserved.
   * The identifier can be gotten with call of {@link #getLastScannedString()}.
   * @java2c=return-this.
   * @param additionalStartChars
   * @param additionalChars
   * @return this
   */
  public final StringPartScan scanIdentifier(String additionalStartChars, String additionalChars)
  { if(scanEntry())
    { lentoIdentifier(additionalStartChars, additionalChars);
      if(bFound)
      { sLastString = getCurrentPart();
        begin = end;  //after identifier.
      }
      else
      { bCurrentOk = false;
      }
      end = endLast;  //revert the change of length, otherwise end = end of identifier.
    } 
    return this;
  }

  
  /**Returns the last scanned integer sign. It is the result of the methods
   * <ul><li>{@link #scanHex(int)}
   * <li>{@link #scanHexOrDecimal(int)}
   * <li>{@link #scanInteger()}
   * </ul>
   * This routine have to be called <b>before</b> the associated {@link #getLastScannedIntegerNumber()} is invoked.
   * 
   * @return True if a negative sign was found before the number. The scanned number is negative too if it is >0. 
   * But a value "-0" is not able to present. But it is important if some fractional parts are parsed after them. 
   * @throws ParseException if called though no scan routine was called. 
   */
  public final boolean getLastScannedIntegerSign() throws ParseException
  { if(idxLastIntegerNumber >= 0)
    { return nLastIntegerSign [idxLastIntegerNumber];
    }
    else throw new ParseException("no integer number scanned.", 0);
  }
  
  /**Returns the last scanned integer number. It is the result of the methods
   * <ul><li>{@link #scanHex(int)}
   * <li>{@link #scanHexOrDecimal(int)}
   * <li>{@link #scanInteger()}
   * </ul>
   * This routine can be called only one time for a scan result. After them the number, and its sign for {@link #getLastScannedIntegerSign()},
   * is removed.
   * @return The number in long format. A cast to int, short etc. may be necessary depending on the expectable values.
   * @throws ParseException if called though no scan routine was called. 
   */
  public final long getLastScannedIntegerNumber() throws ParseException
  { if(idxLastIntegerNumber >= 0)
    { return nLastIntegerNumber [idxLastIntegerNumber--];
    }
    else throw new ParseException("no integer number scanned.", 0);
  }
  
  
  /**Returns the last scanned float number.
   * @return The number in double format. A cast to float may be necessary
   *         depending on the expectable values and the storing format.
   * @throws ParseException if called though no scan routine was called. 
   */
  public final double getLastScannedFloatNumber() throws ParseException
  { if(idxLastFloatNumber >= 0)
    { return nLastFloatNumber[idxLastFloatNumber--];
    }
    else throw new ParseException("no float number scanned.", 0);
  }
  
  
  
  /**Returns the part of the last scanning yet only from {@link #scanIdentifier()}
   * @return The CharSequence which refers in the parent sequence. Use toString() if you need
   *   an persistent String.
   */
  public final CharSequence getLastScannedString()
  { return sLastString;
  }
  

  
  /**Returns the last scanned part between {@link #scanOk()} or {@link #scanStart()} and the following positive scanOk()
   * Note: The {@link StringPart#getLastPart()} returns between {@link StringPart#beginLast} and the begin position.
   * It is not the same but lightweigth equal for seek operations.
   * @return "" if the last {@link #scanOk()} has returned false. Elsewhere the proper {@link StringPart.Part};
   */
  public final CharSequence getLastScannedPart()
  { final int nChars1 =  beginScan - beginScanLast;  
    if(nChars1 <=0) return "";
    else return( new Part(beginScanLast, beginScan));
  }
  
  
  /*=================================================================================================================*/
  /*=================================================================================================================*/
  /*=================================================================================================================*/
  /** Gets a String with transliteration.
   *  The end of the string is determined by any of the given chars.
   *  But a char directly after the escape char \ is not detected as an end char.
   *  Example: getCircumScriptionToAnyChar("\"") ends not at a char " after an \,
   *  it detects the string "this is a \"quotation\"!".
   *  Every char after the \ is accepted. But the known transliteration chars
   *  \n, \r, \t, \f, \b are converted to their control-char- equivalence.
   *  The \s and \e mean begin and end of text, coded with ASCII-STX and ETX = 0x2 and 0x3.</br></br>
   *  The actual part is tested for this, after this operation the actual part begins
   *  after the getting chars!
   *  @param sCharsEnd Assembling of chars determine the end of the part.
   *  @return CharSequence which should be used in the thread, not stored persistently.
   *  @see #scanToAnyChar(CharSequence[], String, char, char, char), 
   *  this method allows all transliteration and quotation characters.  
   * */
  @Java4C.Exclude //see StringFunctions.convertTransliteration(...)
  public final CharSequence getCircumScriptionToAnyChar(String sCharsEnd)
  { return getCircumScriptionToAnyChar_p(sCharsEnd, false);
  }
  
  
  /** Gets a String with transliteration and skip over quotation while searchin.
   *  @param sCharsEnd Assembling of chars determine the end of the part.
   *  @see #getCircumScriptionToAnyChar(String)
   *  @see #scanToAnyChar(CharSequence[], String, char, char, char), 
   *  @return CharSequence which should be used in the thread, not stored persistently.
   *  this method allows all transliteration and quotation characters.  
   * */
  @Java4C.Exclude //see StringFunctions.convertTransliteration(...)
  public final CharSequence getCircumScriptionToAnyCharOutsideQuotion(String sCharsEnd)
  { return getCircumScriptionToAnyChar_p(sCharsEnd, true);
  }
  
  @Java4C.Exclude //see StringFunctions.convertTransliteration(...)
  private final CharSequence getCircumScriptionToAnyChar_p(String sCharsEnd, boolean bOutsideQuotion)
  { 
    char quotationChar = bOutsideQuotion ? '\"' : 0;
    int posEnd = indexOfAnyChar(sCharsEnd, 0, end-begin, '\\', quotationChar, quotationChar);
    if(posEnd >=0){
      lento(posEnd);
      CharSequence ret = StringFunctions.convertTransliteration(getCurrentPart(), '\\');
      fromEnd();
      return ret;
    } else {
      return "";
    }
    
    /*
    CharSequence sResult;
    if(begin == 4910)
      Assert.stop();
    final char cEscape = '\\';
    int posEnd    = (sCharsEnd == null) ? end 
                  : bOutsideQuotion ? indexOfAnyCharOutsideQuotion(sCharsEnd, 0, end-begin)
                                    : indexOfAnyChar(sCharsEnd, 0, end-begin);
    if(posEnd < 0) posEnd = end - begin;
    //int posEscape = indexOf(cEscape);
    //search the first escape char inside the string.
    int posEscape = StringFunctions.indexOf(content, begin, begin + posEnd, cEscape) - begin;  
    if(posEscape < 0)
    { //there is no escape char in the current part to sCharsEnd,
      //no extra conversion is necessary.
      sResult = lento(posEnd).getCurrentPart();
    }
    else
    { //escape character is found before end
      if(content.charAt(begin + posEnd-1)== cEscape)
      { //the escape char is the char immediately before the end char. 
        //It means, the end char isn't such one and posEnd is faulty. 
        //Search the really end char:
        do
        { //search the end char after part of string without escape char
          posEnd    = (sCharsEnd == null) ? end : indexOfAnyChar(sCharsEnd, posEscape +2, Integer.MAX_VALUE);
          if(posEnd < 0) posEnd = end;
          posEscape = indexOf(cEscape, posEscape +2);
        }while((posEscape +1) == posEnd);
      }
      lento(posEnd);
      
      sResult = SpecialCharStrings.resolveCircumScription(getCurrentPart());
    }
    fromEnd();
    return sResult;
    */
  }


  
  /**Scans a String with maybe transliterated characters till one of end characters, 
   * maybe outside any quotation. A transliterated character is a pair of characters 
   * with the special transliteration char, usual '\' followed by any special char. 
   * This pair of characters are not regarded while search the end of the text part, 
   * and the transliteration will be resolved in the result (dst) String.
   * <br>
   * The end of the string is determined by any of the given chars.
   * But a char directly after the transliteration char is not detected as an end char.
   * Example: <pre>scanToAnyChar(dst, ">?", '\\', '\"', '\"')</pre> 
   * does not end at a char > after an \ and does not end inside the quotation.
   * If the following string is given: 
   * <pre>a text -\>arrow, "quotation>" till > ...following</pre> 
   * then the last '>' is detected as the end character. The first one is a transcription,
   * the second one is inside a quotation.
   * <br><br>
   * The meaning of the transliterated characters is defined in the routine
   * {@link StringFunctions#convertTranscription(CharSequence, char)}: 
   * Every char after the transcriptChar is accepted. But the known transcription chars
   * \n, \r, \t, \f, \b are converted to their control-char- equivalence.
   * The \s and \e mean begin and end of text, coded with ASCII-STX and ETX = 0x2 and 0x3.</br></br>
   * The actual part is tested for this, after this operation the actual part begins
   * after the gotten chars!
   *
   * @param dst if it is null, then no result will be stored, elsewhere a CharSequence[1].
   * @param sCharsEnd End characters
   * @param transcriptChar typically '\\', 0 if not used
   * @param quotationStartChar typically '\"', may be "<" or such, 0 if not used
   * @param quotationEndChar The end char, typically '\"', may be ">" or such, 0 if not used
   * @return
   * @since 2013-09-07
   * @see {@link StringPart#indexOfAnyChar(String, int, int, char, char, char)}, used here.
   * @see {@link StringFunctions#convertTransliteration(CharSequence, char)}, used here.
   */
  @Java4C.Exclude //see StringFunctions.convertTransliteration(...)
  public final StringPartScan scanToAnyChar(CharSequence[] dst, String sCharsEnd
      , char transcriptChar, char quotationStartChar, char quotationEndChar)
  { if(scanEntry()){
      int posEnd = indexOfAnyChar(sCharsEnd, 0, end-begin, transcriptChar, quotationStartChar, quotationEndChar);
      if(posEnd >=0){
        lento(posEnd);
        if(dst !=null){
          dst[0] = StringFunctions.convertTransliteration(getCurrentPart(), transcriptChar);
        }
        fromEnd();
      } else {
        bCurrentOk = false;
      }
    }
    return this;
  }


  /**Closes the work. This routine should be called if the StringPart is never used, 
   * but it may be kept because it is part of class data or part of a statement block which runs.
   * The associated String is released. It can be recycled by garbage collector.
   * If this method is overridden, it should used to close a associated file which is opened 
   * for this String processing. The overridden method should call super->close() too.
   * <br>
   * <br>
   * Note: if only this class is instantiated and the instance will be garbaged, close is not necessary.
   * A warning or error "Resource leak" can be switched off. Therefore the interface {@link java.io.Closeable} is not used here.
   */
  @Override
  public void close()
  {
    super.close();
    sLastString = null;
    beginScan = 0;
    bCurrentOk = bFound = false;

  }

  

}
