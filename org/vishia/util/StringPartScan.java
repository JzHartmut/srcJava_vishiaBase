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
   * <li>2019-06-07 Hartmut new {@link #scanLiteral(String, int)} 
   * <li>2019-05-26 Hartmut 
   * <ul><li>new {@link #scanToAnyChar(String, char, char, char)} which stores the parse result in this class.
   *     <li>enhanced {@link #getLastScannedString()} with up to five storage places, should run in C too (TODO test).
   *     <li>gardening {@link #scanDigits(int, int, String)} invokes adequate (and new) {@link StringFunctions_C#parseUlong(CharSequence, int, int, int, int[], String)}
   *     <li>new {@link #scanInteger(String)} with possible separator chars (used in ZBNFParser with new feature)
   *     <li>improved {@link #scanFractionalNumber(long, boolean)}, {@link #scanFloatNumber(char, boolean, String)} with test, see {@link org.vishia.util.test.Test_StringPartScan}.
   * </ul>     
   * <li>2019-02-10 Hartmut For the C deployment a more simple access to the parse result was necessary. It is adapted here too: 
   * <ul><li> {@link #sLastString} is now a final instance, not a reference, set in {@link #setCurrentPartTo(Part)}.
   *     <li> It is set with the scan result in some routines, for example in {@link #scanQuotion(CharSequence, String, String[], int)},
   *          {@link #scanIdentifier(String, String)}
   * </ul>         
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
  public final static String sVersion = "2019-03-10"; 

  
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
  private int ixLastIntegerNumber = -1;
  
  /**Last scanned float number*/
  protected final double[] nLastFloatNumber = new double[5];
  
  /**current index of the last scanned float number. -1=nothing scanned. 0..4=valid*/
  private int ixLastFloatNumber = -1;
  
  /** Last scanned string. 
   * It is able to use as CharSequence. see {@link #getLastScannedString()} 
   * It is more C-friendly with a nested instance. No heap effort. */
  @Java4C.SimpleArray
  protected final Part[] sLastString = new Part[5]; //(this, 0,0);
  
  private int ixLastString = -1;
  
  public StringPartScan(CharSequence src, int begin, int end)
  { super(src, begin, end);
    for(int ix=0; ix<sLastString.length; ++ix) { sLastString[ix] = new Part(this, 0,0); }
  }

  public StringPartScan(CharSequence src)
  { super(src);
    for(int ix=0; ix<sLastString.length; ++ix) { sLastString[ix] = new Part(this, 0,0); }
  }

  
  public StringPartScan()
  { super();
    for(int ix=0; ix<sLastString.length; ++ix) { sLastString[ix] = new Part(this, 0,0); }
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
  
  
  
  /**Internal check on any scan routine. The first call after {@link #scanOk()} or {@link #scanStart()}
   * clears the buffer for numbers, so that 5 numbers can be stored in any scanning concatenation. 
   * @return
   */
  private final boolean scanEntry()
  { if(bCurrentOk)
    { seekNoWhitespaceOrComments();
      if(bStartScan) {  //true after scanOk(), after scanStart() 
        ixLastIntegerNumber = -1;
        ixLastFloatNumber = -1;
        ixLastString = -1;
        bStartScan = false;     //only for first invocation of this routine in a concatenation till scanOk()
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
        else this.setCurrentPartTo(sLastString[++ixLastString]);
        fromEnd().seekPos(sQuotionMarkEnd.length());
      }
      else bCurrentOk = false; 
    }
    return this;
  }
  
  /**Scans a literal given in quotation mark characters.
   * <br>
   * On positive test this.begin is set after the quotation of the literal and scanOk() returns true. 
   * The range between the quotation mark character is stored in this.sLastString[...], can be gotten with getLastScannedString();
   * On negative test scanOk() resets the scan position (as for all scan operations).
   * <br>
   * Note: This operation replaces the deprecated {@link #scanQuotion(CharSequence, String, String[], int)}
   * @param startEndTrans: for example "\"\"\\" or "<>'". 
   *   The first character is the start quotation character, second is end quotation mark.
   *   If the third character is given (Length >2) then the character after that character is not recognized as end quotation.
   *   Usual the third character is the transliteration character known from standard languages for \n, \" etc.
   * @param maxToTest if >=0, the maximal number of characters inclusively the quotation marks for that part.
   *   if &lt; 0, no limitation, thest till end. 
   * @return this for scan concatenation.   
   */
  public StringPartScan scanLiteral(String startEndTrans, int maxToTest) {
    char startChar = startEndTrans.charAt(0);
    char endChar = startEndTrans.charAt(1);
    char transChar = startEndTrans.length() >2 ? startEndTrans.charAt(2) : '\0';
    int zTest = end - begin;
    if(maxToTest >=0 && zTest > maxToTest) { zTest = maxToTest; }
    if(scanEntry() && getCurrentChar() == startChar) {
      int end1 = indexEndOfQuotation(endChar, transChar, 0, zTest);
      if(end1 >=0) {
        sLastString[++ixLastString].setPart(this.begin+1, this.begin + end1-1);
        this.begin += end1;  //After quotation end      
      } else {
        //non successfully:
        bCurrentOk = false;
      }
    } else {
      //non successfully:
      bCurrentOk = false;
    }
    return this;
  }

  

  /**Scans a positive number consisting of digits 0..9, A..Z or a..z 
   * whereby only digits necessary for the given radix are accepted, and sepChars.
   * Does nothing if !#bOk(), sets #bCurrentOk to check bOk() like all scan routines.
   * @param radix
   * @param maxNrofChars Maybe -1, {@link Integer#MAX_VALUE} or a lesser number as the actual part to limit the range. 
   * @param separatorChars Any character are accept inside the number as a separation character. For Example _ or ' or ,  to write:
   *   <code>12'345  12,234  12_345</code> which is parsed as 12345 in any case. Usual such as "'" 
   * @return long number represent the digits. -1 is returned if no digit is detected (as error value). 
   * @throws ParseException 
   */
  public final StringPartScan scanDigits(int radix, int maxNrofChars, String separatorChars) throws ParseException {
    if(bCurrentOk) {
      int max = (end - begin);
      if(maxNrofChars >=0 && maxNrofChars < max) { max = maxNrofChars; }
      int[] parsedChars = new int[1];
      long number = StringFunctions_C.parseUlong(content, begin, max, radix, parsedChars, separatorChars);
      if(parsedChars[0] >0) { //anything parsed
        begin += parsedChars[0];
        if(ixLastIntegerNumber < nLastIntegerNumber.length -2)
        { nLastIntegerNumber[++ixLastIntegerNumber] = number;
          nLastIntegerSign[ixLastIntegerNumber] = false;
        }
        else throw new ParseException("to much scanned integers",0);
      } else {
        bCurrentOk = false;  //canning failed
      }
    }
    return this;
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
  { return scanDigits(10, Integer.MAX_VALUE, null);
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
  public final StringPartScan scanInteger() throws ParseException {
    return scanInteger(null);
  }
  
  
  /**Scans an integer expression with possible sign char '-' or '+' as first char and possible separator chararcter.
   * The result as long value is stored internally
   * and have to be got calling {@link #getLastScannedIntegerNumber()}.
   * There can stored upto 5 numbers. If more as 5 numbers are stored yet,
   * an exception is thrown. 
   * <br><br>
   * If you want to get the scanned String, use {@link #getLastScannedPart()} with scanning only this routine: <pre>
   * if(sp.scanStart().scanInteger("\"',").scanOk()) {
   *   String scannedNumberString = sp.getLastScannedPart().toString();
   *   long scannedNumberInt = sp.getLastScannedIntegerNumber();
   *   .... </pre>
   * @param separatorChars Some character which are accepted inside the nuumber as simple separator character
   *   without semantic. For example a number with thousand separation written with 1"000'000 are convert as 1000000
   *   if separatorChars are given with "\"'". But 1'000'000 delivers the same result for this example.
   *   Usual only one separatorChar may be given.
   * @return this
   * @throws ParseException
   * @java2c=return-this.
   * @return this
   */
  public final StringPartScan scanInteger(String separatorChars) throws ParseException {
    if(scanEntry()) { 
      boolean bNegativeValue = false;
      int begin0 = this.begin;
      char cc = content.charAt(begin);
      if( cc == '-') { 
        bNegativeValue = true;
        begin +=1;
      } else if(cc=='+') {
        begin +=1;           //other chars than + - or not handled, they may cause !bCurrentOk on scanDigits.
      }
      if(scanDigits(10, Integer.MAX_VALUE, separatorChars).bCurrentOk) {
        nLastIntegerSign[ixLastIntegerNumber] = bNegativeValue;
        if(bNegativeValue)
        { nLastIntegerNumber[ixLastIntegerNumber] = - nLastIntegerNumber[ixLastIntegerNumber]; 
        }
      } else if(bNegativeValue) {
        this.begin = begin0;  //revert the scan of '-'
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
      ixLastFloatNumber = -1; 
    }
    scanFloatNumber();
    return this;
  }
  

  
  /**Scans a float / double number. The result is stored internally
   * and have to be got calling {@link #getLastScannedFloatNumber()}.
   * There can stored upto 5 numbers. If more as 5 numbers are stored yet,
   * an exception is thrown. 
   * <br><br>Note: If the number is an integer, the result is still {@link #scanOk()}, the integer is accepted as float.
   * Use {@link #scanFloatNumber(char, boolean, String)} with strict=true to distinguish from an integer. 
   * @java2c=return-this.
   * @return this
   * @throws ParseException if the buffer is not free to hold the float number.
   */
  public final StringPartScan scanFloatNumber() throws ParseException  //::TODO:: scanLong(String sPicture)
  { return scanFloatNumber('.', false, null);
  }
  
  
  /**Scans a float / double number. The result is stored internally
   * and have to be got calling {@link #getLastScannedFloatNumber()}.
   * There can stored upto 5 numbers. If more as 5 numbers are stored yet,
   * an exception is thrown. 
   * @param fractionalSeparator usual '.', maybe ',' or other for language specifica
   * @param bStrict if true than expects a fractional separator  or an exponent or both (to distinguish from integer)
   * @param separatorChars See {@link #scanInteger(String)}
   * @return this
   * @java2c=return-this.
   * @throws ParseException if the buffer is not free to hold the float number.
   */
  public final StringPartScan scanFloatNumber(char fractionalSeparator, boolean bStrict, String separatorChars) throws ParseException  //::TODO:: scanLong(String sPicture)
  {
    if(scanEntry()) { 
      int begini = this.begin;
      if(scanInteger(separatorChars).bCurrentOk) {
        long number = this.nLastIntegerNumber[ this.ixLastIntegerNumber];
        boolean bNegativeValue = this.nLastIntegerSign[ this.ixLastIntegerNumber];
        this.ixLastIntegerNumber -=1;
        scanFractionalExponent(fractionalSeparator, bStrict, separatorChars, number, bNegativeValue); //negative should be known for -0.123 to handle fractional part correctly.
        if(!bCurrentOk) {
          if(bStrict) {
            begin = begini;  //on start of scanInteger
          } else {
            //only integer number found, store as float number. It is ok.
            bCurrentOk = true;
            if(ixLastFloatNumber < nLastFloatNumber.length -2){
              nLastFloatNumber[++ixLastFloatNumber] = (double)number;
            } else throw new ParseException("to much scanned floats",0);
          }
        }
      } else  {
        begin = begini;
      }
    }
    return this;
  }

  
  
  /**Scans the fractionalPart and the exponent of a float number with '.' as first expected separator 
   * and without additional separator characters.
   * It is ok too if only a exponent is scanned, for example "E+03"
   * @param nInteger The maybe parsed integer part before this fractional part.
   * @param bNegative true if the integer part has a sign. It is essential if "-0" are scanned as integer.
   * @return this
   * @throws ParseException
   * @see {@link #scanFractionalNumber(char, String, long, boolean)}
   */
  public final StringPartScan scanFractionalNumber(long nInteger, boolean bNegative) throws ParseException  {
    return scanFractionalExponent('.', false, null, nInteger, bNegative);
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
   * @param fractionalSeparator usual '.', maybe ',' or other for language specifica
   * @param separatorChars See {@link #scanInteger(String)}
   * @param nInteger The maybe parsed integer part before this fractional part.
   * @param bNegative true if the integer part has a sign. It is essential if "-0" are scanned as integer.
   * @return this
   * @java2c=return-this.
   * @throws ParseException if the buffer is not free to hold the float number.
   */
  public final StringPartScan scanFractionalExponent(char fractionalSeparator, boolean bStrict
      , String separatorChars, long nInteger, boolean bNegative) throws ParseException { //::TODO:: scanLong(String sPicture)
    if(bCurrentOk) {            //Note: donot call scanEntry(), do not skip over white spaces.  
      //
      //switch of skip over white spaces and comment, inside the number. 
      int bitModeSave = this.bitMode;
      this.bitMode &= ~(mSkipOverCommentInsideText_mode | mSkipOverCommentToEol_mode | mSkipOverWhitespace_mode);
      //
      long nFractional = 0;
      int nDivisorFract = 1, nExponent = 0;
      //int nDigitsFrac;
      char cc;
      boolean bNegativExponent = false;
      double result;
      int begin0 = this.begin;
      //Note: a fractional part is optional, they can be an exponent only too.
      if(begin < endMax && content.charAt(begin) == fractionalSeparator) {
        seekPos(1); //over .
        while(begin < endMax && ((cc = getCurrentChar()) == '0' || (separatorChars !=null && separatorChars.indexOf(cc)>=0))) { //leading 0 of fractional
          seekPos(1); 
          if(cc == '0') { nDivisorFract *=10; }
        }
        if(scanDigits(10, Integer.MAX_VALUE, separatorChars).bCurrentOk) {
          nFractional = this.nLastIntegerNumber[ this.ixLastIntegerNumber];
          this.ixLastIntegerNumber -=1;
        } else { //if(nDivisorFract >=10 ) {
          nFractional = 0;   //if no fractional digits found, it is still ok 
          bCurrentOk = true; //it is okay, at ex."9.0" is found. There are no more digits after "0".
        }
      }
      int nPosExponent = begin;
      if( nPosExponent < endMax && ((cc = content.charAt(begin)) == 'e' || cc == 'E'))
      { seekPos(1);
        if(scanInteger().bCurrentOk) {
          nExponent = (int)this.nLastIntegerNumber[ this.ixLastIntegerNumber];
          this.ixLastIntegerNumber -=1;
        }
        else
        { // it isn't an exponent, but a String beginning with 'E' or 'e'.
          //This string is not a part of the float number.
          begin = nPosExponent;
          nExponent = 0;
        }
      }
      if(bCurrentOk && begin > begin0) {
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
        if(ixLastFloatNumber < nLastFloatNumber.length -2){
          nLastFloatNumber[++ixLastFloatNumber] = result;
        } else throw new ParseException("to much scanned floats",0);
      }
      else {  //whetter '.' nor 'E' found:
        bCurrentOk = false;   //only E is not admissible.
        this.begin = begin0;
      }
      this.bitMode = bitModeSave;
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
  { return scanDigits(16, maxNrofChars, null);
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
    { int begin0 = begin;
      if( StringFunctions.equals(content, begin, begin+2, "0x"))
      { seek(2); scanDigits(16, maxNrofChars, null);
      }
      else
      { scanDigits(10, maxNrofChars, null);
      }
      if(!bCurrentOk) { 
        begin = begin0;  //revert read "0x"
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
      { this.setCurrentPartTo(sLastString[++ixLastString]);
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
  { if(ixLastIntegerNumber >= 0)
    { return nLastIntegerSign [ixLastIntegerNumber];
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
  { if(ixLastIntegerNumber >= 0)
    { return nLastIntegerNumber [ixLastIntegerNumber--];
    }
    else throw new ParseException("no integer number scanned.", 0);
  }
  
  
  /**Returns the last scanned float number.
   * @return The number in double format. A cast to float may be necessary
   *         depending on the expectable values and the storing format.
   * @throws ParseException if called though no scan routine was called. 
   */
  public final double getLastScannedFloatNumber() throws ParseException
  { if(ixLastFloatNumber >= 0)
    { return nLastFloatNumber[ixLastFloatNumber--];
    }
    else throw new ParseException("no float number scanned.", 0);
  }
  
  
  
  /**Returns the part of the last scanning yet only from {@link #scanIdentifier()}
   * @return A persistent String of the last scanned String.
   */
  public final String getLastScannedString()
  { return sLastString[ixLastString--].toString();
  }
  

  
  /**Returns the part of the last scanning non persistently.
   * If the scanning continues with String scanning the returned instance will be resued.
   * @return The CharSequence which refers in the parent sequence. Use toString() if you need
   *   an persistent String.
   * @since 2019-02. It is optimized for C usage. No new operator is used.  
   */
  public final StringPart.Part getLastScannedPart( int nr)
  { return sLastString[ixLastString--];
  }
  

  
  /**Returns the last scanned part between {@link #scanOk()} or {@link #scanStart()} and the following positive scanOk()
   * Note: The {@link StringPart#getLastPart()} returns between {@link StringPart#beginLast} and the begin position.
   * It is not the same but lightweigth equal for seek operations.
   * @return "" if the last {@link #scanOk()} has returned false. Elsewhere the proper {@link StringPart.Part};
   */
  public final CharSequence getLastScannedPart()
  { final int nChars1 =  beginScan - beginScanLast;  
    if(nChars1 <=0) return "";
    else return( new Part(this, beginScanLast, beginScan));
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
    char quotationChar = bOutsideQuotion ? '\"' : '\0';
    int posEnd = indexOfAnyChar(sCharsEnd, 0, end-begin, '\\', quotationChar, quotationChar);
    if(posEnd >=0){
      lentoPos(posEnd);
      CharSequence ret = StringFunctions.convertTransliteration(getCurrentPart(), '\\');
      fromEnd();
      return ret;
    } else {
      return "";
    }
    
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
        lentoPos(posEnd);
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


  /**Scans a String with maybe transcription characters till one of end characters, 
   * maybe outside any quotation. A transcription character is a pair of characters 
   * with the special transcription char, usual '\' followed by any special char. 
   * This pair of characters is not regarded while search the end of the text part, 
   * <br>
   * The end of the string is determined by any of the given chars.
   * But a char directly after the transcription char is not detected as an end char.
   * Example: <pre>scanToAnyChar(">?", '\\', '\"', '\"')</pre> 
   * does not end at a char > after an \ and does not end inside the quotation.
   * If the following string is given: 
   * <pre>a text -\>arrow, "quotation>" till > ...following</pre> 
   * then the last '>' is detected as the end character. The first one is a transcription,
   * the second one is inside a quotation.
   * <br><br>
   * The meaning of the transcription characters is defined in the routine
   * {@link StringFunctions#convertTranscription(CharSequence, char)}: 
   * Every char after the transcriptChar is accepted. But the known transcription chars
   * \n, \r, \t, \f, \b are converted to their control-char- equivalence.
   * The \s and \e mean begin and end of text, coded with ASCII-STX and ETX = 0x2 and 0x3.</br></br>
   * The actual part is tested for this, after this operation the actual part begins
   * after the gotten chars!
   * <br>
   * If the scan is successfully, #scanOk() provides true and the String between the scan start position
   * and exclusively the found end character is stored in {@link #sLastString}, 
   * can be gotten with {@link #getLastScannedPart()}.
   *
   * @param sCharsEnd End characters
   * @param transcriptChar typically '\\', 0 if not used
   * @param quotationStartChar typically '\"', may be "<" or such, 0 if not used
   * @param quotationEndChar The end char, typically '\"', may be ">" or such, 0 if not used
   * @return
   * @since 2013-09-07
   * @see {@link StringPart#indexOfAnyChar(String, int, int, char, char, char)}, used here.
   * @see {@link StringFunctions#convertTransliteration(CharSequence, char)}, used here.
   */
  public final StringPartScan scanToAnyChar(String sCharsEnd
      , char transcriptChar, char quotationStartChar, char quotationEndChar)
  { if(scanEntry()){
      int posEnd = indexOfAnyChar(sCharsEnd, 0, end-begin, transcriptChar, quotationStartChar, quotationEndChar);
      if(posEnd >=0){
        lentoPos(posEnd);
        this.setCurrentPartTo(sLastString[++ixLastString]);
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
    //sLastString = null;
    beginScan = 0;
    bCurrentOk = bFound = false;

  }

  

}
