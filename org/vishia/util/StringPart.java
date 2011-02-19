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
 * @version 2006-06-15  (year-month-day)
 * list of changes: 
 * 2009-05-13: HScho substring(start, end): now start counts from startMin, end may be 0 or negative, then backward from endMax
 * 2009-02-05: HScho bugfix in scanHexOrDecimal(): content.substring(start).startsWith("0x")) instead equals, because IndexOutOfBoundsException if the buffer end is reached.
 * 2008-04-02: HScho some changes
 * 2006-05-00: HScho creation
 *
 ****************************************************************************/
package org.vishia.util;
import java.text.ParseException;
import org.vishia.util.SpecialCharStrings;

/** The StringPart class represents a flexible valid part of a character string which spread is changeable. 
 * This is a enhancement of the standard class String or StringBuffer.<br>
    The StringPart class is associated to a String. Additionaly 4 Parameters determine the actual part of the String
    and the limits of changing this actual part. The followed image is used to explain the spreadness of the parts:<pre>
 abcdefghijklmnopqrstuvwxyz  Sample of the whole associated String
   =====================     The === indicates the maximal part
     -----------             The --- indicates the valid part before some operation
           +++++             The +++ indicates the valid part after some operation
      </pre> 
    At a first idea a part is likely a substring. 
    But the actual part of the string is changeable, without building a new substring. 
    So some operations of seeking and scanning
    are supported. The collectivity methods to scan strings from a file (Reader) are supported from a derivated
    class StringScan of this class.<br/>
    Types of Methods:
    <ul><li>seek: changes the position of the actual (current) string part, do not change the end of the actual part,
            from there, seek changes the length. Seek returns this, so concatenation of methods calls is able.</li>
        <li>lento: changes the end of the actual string part. </li>
        <li>scan: test wether the scanning condition is matched. If so, the actual part is shortened by the overscanning
            characters, if no, the bCurrentOk-Flag is set to false and all scanning invokes after are not executed.
            The scan methods returns this, so concatenation of methods calls is able. 
            The last call should be <code>scanOk</code>, see there.</li>
        <li>get: Gets an content without changing.</li>
        <li>set: Sets a total new content.</li>
        <li>Some string methods are also supported like String</li>
    </ul>            
*/

/*
<pre>
date       who        what
2009-03-16 Hartmut new: scanStart() returns this, not void. Useable in concatenation.
2007-05-08 JcHartmut  change: seekAnyChar(String,int[]) renamed to {@link seekAnyString(String,int[])} because it was an erroneous identifier. 
2007-05-08 JcHartmut  new: {@link lastIndexOfAnyChar(String,int,int)}
                      new: {@link lentoAnyChar(String, int, int)}
                      rem: it should programmed consequently for all indexOf and lento methods.
2007-04-00 JcHartmut  some changes, not noted.
2005-06-00 JcHartmut  initial revision

*
</pre>

*/


public class StringPart
{
  /** The actual start position of the valid part.*/
  protected int start;
  /** The actual exclusive end position of the valid part.*/
  protected int end;

  /** The leftest possible start position. We speak about the 'maximal Part':
      The actual valid part can not exceed the borders startMin and endMax of the maximal part after any operation.
      The content of the associated string outside the maximal part is unconsidered. The atrributes startMin and endMax
      are not set by any operations except for the constructors and the set()-methods.
      <br/>Set to 0 if constructed from a string,
      determined by the actual start if constructed from a StringPart.
      <hr/><u>In the explanation of the methods the following notation is used as samples:</u><pre>
abcdefghijklmnopqrstuvwxyz  Sample of the whole associated String
  =====================     The === indicates the maximal part
    -----------             The --- indicates the valid part before operation
               ++++++++     The +++ indicates the valid part after operation
      </pre>
  */
  protected int startMin;



  /** The rightest possible exclusive end position. See explanation on startMin.
      <br/>Set to content.length() if constructed from a string,
      determined by the actual end if constructed from a StringPart*/
  protected int endMax;

  /** The referenced string*/
  String content;

  /** The line number, counted by founded \n */
  protected int nLineCt = 1;
  
  /**false if current scanning is not match*/
  protected boolean bCurrentOk = true;
  
  /**If true, than all idxLastScanned... are set to 0, 
   * it is after {@link #scanOk()} or after {@link #scanStart}
   */ 
  protected boolean bStartScan = true;

  /** Borders of the last part before calling of scan__(), seek__(), lento__(). If there are different to the current part,
   * the call of restoreLastPart use this values. scanOk() sets the startLast-variable to the actual start or rewinds
   * the actual start ot startLast.
   */
  protected int startLast, endLast;

  /**Position of scanStart() or after scanOk() as start of next scan operations. */
  protected int startScan;
  
  /** True if the last operation of lento__(), seek etc. has found anything. See {@link #found()}. */
  boolean bFound = true;
  
  /**Last scanned integer number.*/
  protected final long[] nLastIntegerNumber = new long[5];
  
  /**current index of the last scanned integer number. -1=nothing scanned. 0..4=valid*/
  private int idxLastIntegerNumber = -1;
  
  /**Last scanned float number*/
  protected final double[] nLastFloatNumber = new double[5];
  
  /**current index of the last scanned float number. -1=nothing scanned. 0..4=valid*/
  private int idxLastFloatNumber = -1;
  
  /** Last scanned string. */
  protected String sLastString;
  
  /** Flag to force setting the start position after the seeking string. See description on seek(String, int).
  */
  public static final int seekEnd = 1;

  /** Flag bit to force seeking backward. This value is contens impilicit in the mSeekBackFromStart or ~End,
      using to detect internal the backward mode.
  */
  private static final int mSeekBackward_ = 0x10;

  /** Flag bit to force seeking left from start (Backward). This value is contens impilicit in the seekBackFromStart
      using to detect internal the seekBackFromStart-mode.
  */
  private static final int mSeekToLeft_ = 0x40;

  /** Flag to force seeking backward from the start position. See description on seek(String).
  */
  public static final int seekToLeft = mSeekToLeft_ + mSeekBackward_;


  /** Flag to force seeking backward from the end position. See description on seek(String).
  */
  public static final int seekBack = 0x20 + mSeekBackward_;

  /** Flag to force seeking forward. See description on seek(String).
  */
  public static final int seekNormal = 0;


  /** Some mode bits */
  protected int bitMode = 0;
  
  /** Bit in mode. If this bit ist set, all whitespace are overreaded
   * before calling any scan method.
   */
  protected static final int mSkipOverWhitespace_mode = 0x1;

  /** Bit in mode. If this bit ist set, all comments are overreaded
   * before calling any scan method.
   */
  protected static final int mSkipOverCommentInsideText_mode = 0x2;

  /** Bit in mode. If this bit ist set, all comments are overreaded
   * before calling any scan method.
   */
  protected static final int mSkipOverCommentToEol_mode = 0x4;

  /** The string defined the start of comment inside a text.*/
  String sCommentStart = "/*";
  
  /** The string defined the end of comment inside a text.*/
  String sCommentEnd = "*/";
  
  /** The string defined the start of comment to end of line*/
  String sCommentToEol = "//";
  
  /** The char used to code start of text. */  
  public static final char cStartOfText = (char)(0x2);
  
  /** The char used to code end of text. */  
  public static final char cEndOfText = (char)(0x3);

  /** Interface to report something, only level Report.fineDebug is used.*/ 
  //protected final Report report;
  
  /** Creates a new empty StringPart without an associated String. See method set() to assign a String.*/
  public StringPart()
  { this.content = null; startMin = start = startLast= 0; endLast = endMax = end = 0;
    //report = null;
  }



  /** Creates a new StringPart, with the given content from a String. Initialy the whole string is valid
      and determines the maximal part.
      @param content The content, that String is associated by this.
  */
  public StringPart(String content)
  { assign(content);
    //report = null;
  }





  /** Creates a new StringPart with the same String as the given StringPart. The maximal part of the new StringPart
      are determined from the actual valid part of the src. The actual valid part is equal to the maximal one.
      <hr/><u>example:</u><pre>
abcdefghijklmnopqrstuvwxyz  The associated String
    ----------------        The valid part of src
    ================        The maximal part and initial the valid part of this
    +++++   ++++            Possible valid parts of this after some operations
       ++++      +++        Possible also
  +++++           ++++ +++  Never valid parts of this after operations because they exceeds the borders of maximal part.
      </pre>

      @param src The given StringPart.
  */
  public StringPart(StringPart src)
  { assign(src);
    //report = null;
  }



  /** Sets the content to the given string, forgets the old content. Initialy the whole string is valid.
      @java2c=return-this.
      @param content The content.
      @return <code>this</code> to concat some operations, like <code>part.set(src).seek(sKey).lento(';').len0end();</code>
  */
  public StringPart assign(String content)
  { this.content = content;
    startMin = startLast = start = 0;
    endMax = end = endLast = content.length();
    return this;
  }


  /** Sets the StringPart with the same String object as the given StringPart, forgets the old content.
      The borders of the new StringPart (the maximal part)
      are determined from the actual valid part of the src. The actual valid part is equal to this limits.
      If the src is the same instance as this (calling with 'this'), than the effect is the same.
      The maximal Part is determined from the unchanged actual part.
      <hr/><u>example:</u><pre>
abcdefghijklmnopqrstuvwxyz  The associated String
    ----------------        The valid part of src
    ================        The maximal part and initial the valid part of this
    +++++   ++++            Possible valid parts of this after some operations
       ++++      +++        Possible also
  +++++           ++++ +++  Never valid parts of this after operations because they exceeds the borders of maximal part.
      </pre>
      @java2c=return-this.
      @param src The given StringPart.
      @return <code>this</code> to concat some operations, like <code>part.set(src).seek(sKey).lento(';').len0end();</code>
  */
  public StringPart assign(StringPart src)
  { if(src == this)
    { //set from the own instance: the maxPart is the actual one.
      startMin = startLast = start; endMax = endLast = end;
    }
    else
    { //set from a other instance, inherit the content.
      this.content = src.content; startMin = startLast = start = src.start; endMax = end = endLast = src.end;
    }
    return this;
  }














  /** Sets the content of the StringPart , forgets the old content. The same string like in src is associated.
      Initialy the part from the end of the src-part to the maximal end of src is valid. The valid part and
      the maximal part is set in this same way.
      <hr/><u>example:</u><pre>
abcdefghijklmnopqrstuvwxyz  The associated String
  =====================     The maximal part of src
    ------                  The valid part of src
          =============     The maximal and initialy the valid part of this
      </pre>
      @java2c=return-this.
      @param src The source of the operation.
      @return <code>this</code> to concat some operations, like <code>part.set(src).seek(sKey).lento(';').len0end();</code>
  */
  public StringPart assignFromEnd(StringPart src)
  { this.content = src.content;
    startLast = start;
    startMin = start = src.end;       //from actual end
    endLast = endMax = end = src.endMax;          //from maximal end
    return this;
  }

  
  
  /** Set the mode of ignoring comments.
   * If it is set, comments are always ignored on every scan operation. 
   * On scan, the current position is set first after a comment if the current position began with a comment.
   * This mode may or should be combinded with setIgnoreWhitespace.<br/> 
   * The string introduces and finishes a comment is setted by calling 
   * setIgnoreComment(String sStart, String sEnd). The default value is "/ *" and "* /" like in java-programming. 
   * @param bSet If true than ignore, if false than comments are normal input to parse.
   * @return The last definition of this feature.
   */
  public boolean setIgnoreComment(boolean bSet)
  { boolean bRet = (bitMode & mSkipOverCommentInsideText_mode) != 0;
    if(bSet) bitMode |= mSkipOverCommentInsideText_mode;
    else     bitMode &= ~mSkipOverCommentInsideText_mode;
    return bRet;
  }
  
  
  /** Set the character string of inline commentmode of ignoring comments.
   * After this call, comments are always ignored on every scan operation. 
   * On scan, the current position is set first after a comment if the current position began with a comment.
   * This mode may or should be combinded with setIgnoreWhitespace.<br/> 
   * @param sStart Start character string of a inline comment
   * @param sEnd End character string of a inline comment
   * @return The last definition of the feature setIgnoreComment(boolean).
   */
  public boolean setIgnoreComment(String sStart, String sEnd)
  { boolean bRet = (bitMode & mSkipOverCommentInsideText_mode) != 0;
    bitMode |= mSkipOverCommentInsideText_mode;
    sCommentStart = sStart; 
    sCommentEnd   = sEnd;
    return bRet;
  }
  
  
  /** Set the mode of ignoring comments to end of line.
   * If it is set, end-line-comments are always ignored on every scan operation. 
   * On scan, the current position is set first after a comment if the current position began with a comment.
   * This mode may or should be combinded with setIgnoreWhitespace.<br/> 
   * The string introduces a endofline-comment is setted by calling 
   * setEndlineCommentString(). The default value is "//" like in java-programming. 
   * @param bSet If true than ignore, if false than comments are normal input to parse.
   * @return The last definition of the feature setIgnoreComment(boolean).
   */
  public boolean setIgnoreEndlineComment(boolean bSet) 
  { boolean bRet = (bitMode & mSkipOverCommentToEol_mode) != 0;
    if(bSet) bitMode |= mSkipOverCommentToEol_mode;
    else     bitMode &= ~mSkipOverCommentToEol_mode;
    return bRet;
  }
  

  
  /** Set the character string introducing the comments to end of line.
   * After this call, endline-comments are always ignored on every scan operation. 
   * On scan, the current position is set first after a comment if the current position began with a comment.
   * This mode may or should be combinded with setIgnoreWhitespace.<br/> 
   * @param sStart String introducing end line comment
   * @return The last definition of this feature.
   */
  public boolean setIgnoreEndlineComment(String sStart) 
  { boolean bRet = (bitMode & mSkipOverCommentToEol_mode) != 0;
    bitMode |= mSkipOverCommentToEol_mode;
    sCommentToEol = sStart;
    return bRet;
  }
  
  /** Set the mode of ignoring whitespaces.
   * If it is set, whitespaces are always ignored on every scan operation. 
   * On scan, the current position is set first after a comment if the current position began with a comment.
   * This mode may or should be combinded with setIgnoreWhitespace.<br/> 
   * The chars accepted as whitespace are setted by calling 
   * setWhiteSpaceCharacters(). The default value is " \t\r\n\f" like in java-programming.
   * @param bSet If true than ignore, if false than comments are normal input to parse.
   * @return The last definition of this feature.
   */
  public boolean setIgnoreWhitespaces(boolean bSet)
  { boolean bRet = (bitMode & mSkipOverWhitespace_mode) != 0;
    if(bSet) bitMode |= mSkipOverWhitespace_mode;
    else     bitMode &= ~mSkipOverWhitespace_mode;
    return bRet;
  }
  
  
  
  

  /** Sets the start of the maximal part to the actual start of the valid part.
      See also seekBegin(), that is the opposite operation.
      <hr/><u>example:</u><pre>
abcdefghijklmnopqrstuvwxyz  The associated String
    ================        The maximal part before operation
         ------             The actual part
         ===========        The maximal part after operation
      </pre>
      @java2c=return-this.
      @param src The given StringPart.
      @return <code>this</code> to concat some operations, like <code>part.set(src).seek(sKey).lento(';').len0end();</code>
  */
  public StringPart setBeginMaxPart()
  { startMin = start;
    return this;
  }



  
  
  
  /** Sets the start of the part to the exclusively end, set the end to the end of the content.
      <hr/><u>example:</u><pre>
abcdefghijklmnopqrstuvwxyz  The associated String
  =================         The maximal part
         -----              The valid part before
              +++++         The valid part after.
      </pre>
      @java2c=return-this.
      @return <code>this</code> to concat some operations, like <code>part.set(src).seek(sKey).lento(';').len0end();</code>
  */
  public StringPart fromEnd()
  {
    startLast = start;
    endLast = end;
    start = end;
    end = endMax;
    return this;
  }

  /** get the Line ct
      @return Number of last readed line.
  */
  public int getLineCt(){ return nLineCt; }

  /** Gets the length of the valid part. See length().
   * @deprecated
   */
  public int getLen(){ return length();}
  

  /** Returns the valid lenght of the acutual part. Returns also 0 if no string is valid.
      @return number of chars actual valid.
  */
  public int length()
  { if(end > start) return end - start;
    else return 0;
  }

  /** Returns the lenght of the maximal part from current position. Returns also 0 if no string is valid.
      @return number of chars from current position to end of maximal part.
  */
  public int lengthMaxPart()
  { if(endMax > start) return endMax - start;
    else return 0;
  }

  /*=================================================================================================================*/
  /*=================================================================================================================*/
  /*=================================================================================================================*/


  /** Sets the endposition of the part of string to the given chars after start.
      @java2c=return-this.
      @param len The new length. It must be positive.
      @return <code>this</code> to concat some operations.
      @throws IndexOutOfBoundsException if the len is negativ or greater than the position endMax.
  */
  public StringPart lento(int len)
  throws IndexOutOfBoundsException
  { endLast = end;
    int endNew = start + len;
    if(endNew < start) /**@java2c=StringBuilderInThreadCxt.*/ throwIndexOutOfBoundsException("lento(int) negative:" + (endNew - start));
    if(endNew > endMax) /**@java2c=StringBuilderInThreadCxt.*/ throwIndexOutOfBoundsException("lento(int) after endMax:" + (endNew - endMax));
    end = endNew;
    return this;
  }



  /** Sets the endposition of the part of string to exclusively the char cc.
      If the char cc is not found, the end position is set to start position, so the part of string is emtpy.
      It is possible to call set0end() to set the end of the part to the maximal end if the char is not found.
      That is useful by selecting a part to a separating char such ',' but the separator is not also the terminating char
      of the last part.
      <hr/><u>example:</u><pre>
abcdefghijklmnopqrstuvwxyz  The associated String
  =================         The maximal part of src
         -----              The valid part of src before calling the method
         +                  after calling lento('w') the end is set to start
                            position, the length() is 0, because the 'w' is outside.
         ++++++++++         calling set0end() is possible and produce this result.
      </pre>
      @java2c=return-this.
      @param cc char to determine the exclusively end char.
      @return <code>this</code> to concat some operations, like <code>part.set(src).seek(sKey).lento(';').len0end();</code>
  */
  public StringPart lento(char cc)
  { endLast = end;
    int pos = content.substring(start,end).indexOf(cc);
    bFound = (pos >=0);
    if(pos >= 0) { end = start + pos; }
    else         { end = start; }
    return this;
  }



  /** Sets the endposition of the part of string to exclusively the given string.
      If the string is not found, the end position is set to start position, so the part of string is emtpy.
      It is possible to call set0end() to set the end of the part to the maximal end if the char is not found.
      That is useful by selecting a part to a separating char such ',' but the separator is not also the terminating char
      of the last part, example see lento(char cc)
      @java2c=return-this.
      @param ss string to determine the exclusively end char.
      @return <code>this</code> to concat some operations, like <code>part.set(src).seek(sKey).lento(';').len0end();</code>
  */
  public StringPart lento(String ss)
  { return lento(ss, seekNormal);
  }

  
  
  /** Sets the endposition of the part of string to exclusively the given string.
      If the string is not found, the end position is set to start position, so the part of string is emtpy.
      It is possible to call set0end() to set the end of the part to the maximal end if the char is not found.
      That is useful by selecting a part to a separating char such ',' but the separator is not also the terminating char
      of the last part, example see lento(char cc)
      @java2c=return-this.
      @param ss string to determine the exclusively end char.
      @param mode Mode of seeking the end, seekEnd or 0 is possible.
      @return <code>this</code> to concat some operations, like <code>part.set(src).seek(sKey).lento(';').len0end();</code>
  */
  public StringPart lento(String ss, int mode)
  { endLast = end;
    int pos = content.substring(start, end).indexOf(ss);
    bFound = (pos >=0);
    if(pos >= 0) { end = start + pos; 
                   if((mode & seekEnd) != 0){ end += ss.length();}
                 }
    else         { end = start; }
    return this;
  }



  /** Sets the endposition of the part of string to the end of the identifier which is beginning on start.
      If the part starts not with a identifier char, the end is set to the start position.
      <hr/><u>example:</u><pre>
abcd  this is a part uvwxyz The associated String
  =====================     The border of valid parts of src
       -------              The valid part of the src before calling the method
       +++                  after calling lentoIdentifier(). The start position
                            is not effected. That's why the identifier-part is only "his".
      </pre>
      @java2c=return-this.
      @return <code>this</code> to concat some operations, like <code>part.set(src).seek(sKey).lento(';').len0end();</code>
  */
  public StringPart lentoIdentifier()
  {
    return lentoIdentifier(null, null);
  }
  
  /** Sets the endposition of the part of string to the end of the identifier which is beginning on start.
   *  If the part starts not with a identifier char, the end is set to the start position.
   *  @see lentoIdentifier().
   *  @java2c=return-this.
   *  @param additionalChars String of additinal chars there are also accept
   *         as identifier chars. 
   */
  public StringPart lentoIdentifier(String additionalStartChars, String additionalChars)
  { endLast = end;
    end = start;
    if(end >= endMax){ bFound = false; }
    else
    { char cc = content.charAt(end);
      if(   cc == '_' 
        || (cc >= 'A' && cc <='Z') 
        || (cc >= 'a' && cc <='z') 
        || (additionalStartChars != null && additionalStartChars.indexOf(cc)>=0)
        )
      { end +=1;
        while(  end < endMax 
             && (  (cc = content.charAt(end)) == '_' 
                || (cc >= '0' && cc <='9') 
                || (cc >= 'A' && cc <='Z') 
                || (cc >= 'a' && cc <='z') 
                || (additionalChars != null && additionalChars.indexOf(cc)>=0)
             )  )
        { end +=1; }
      }  
      bFound = (end > start);
    }
    return this;
  }




  /*=================================================================================================================*/
  /*=================================================================================================================*/
  /*=================================================================================================================*/

  /** Sets the len to the first position of any given char, but not if the char is escaped.
   *  'Escaped' means, a \ is disposed before the char.
   *  Example: lentoAnyNonEscapedChar("\"") ends not at a \", but at ".
   *  it detects the string "this is a \"quotion\"!".
   *  <br>
   *  This method doesn't any things, if the last scanning call isn't match. Invoking of 
   *  {@link scanOk()} before guarantees that the method works.
   *  @java2c=return-this.
   *  @param sCharsEnd Assembling of chars determine the end of the part.  
   * */
  public StringPart lentoAnyNonEscapedChar(String sCharsEnd, int maxToTest)
  { if(bCurrentOk)
    { final char cEscape = '\\';
      endLast = end;
      int pos = indexOfAnyChar(sCharsEnd,0,maxToTest);
      while(pos > start+1 && content.charAt(pos-1)==cEscape)
      { //the escape char is before immediately. It means, the end char is not matched.
        pos = indexOfAnyChar(sCharsEnd, pos+1-start, maxToTest);
      }
      if(pos < 0){ end = start; bFound = false; }
      else       { end = start + pos; bFound = true; }
    }  
    return this;
  }



  /** Sets the len to the first position of any given char, but not if the char is escaped.
   *  'Escaped' means, a \ is disposed before the char.
   *  Example: lentoAnyNonEscapedChar("\"") ends not at a \", but at ".
   *  it detects the string "this is a \"quotion\"!".
   *  <br>
   *  This method doesn't any things, if the last scanning call isn't match. Invoking of 
   *  {@link scanOk()} before guarantees that the method works.
   *  @java2c=return-this.
   *  @param sCharsEnd Assembling of chars determine the end of the part.  
   * */
  public StringPart lentoNonEscapedString(String sEnd, int maxToTest)
  { if(bCurrentOk)
    { final char cEscape = '\\';
      endLast = end;
      int pos = indexOf(sEnd,0,maxToTest);
      while(pos > 1 && content.charAt(start+pos-1)==cEscape)
      { //the escape char is before immediately. It means, the end char is not matched.
        pos = indexOf(sEnd, pos+1, maxToTest);
      }
      if(pos < 0){ end = start; bFound = false; }
      else       { end = start + pos; bFound = true; }
    }  
    return this;
  }







  /** Displaces the start of the part for some chars to left or to right.
      If the seek operation would exceeds the maximal part borders, a StringIndexOutOfBoundsException is thrown.

      <hr/><u>example:</u><pre>
abcdefghijklmnopqrstuvwxyz  The associated String
  =================         The maximal part
         -----              The valid part before
       +++++++              The valid part after calling seek(-2).
           +++              The valid part after calling seek(2).
             +              The valid part after calling seek(5).
                            The start is set to end, the lenght() is 0.
  ++++++++++++              The valid part after calling seek(-5).
      </pre>
   *  @java2c=return-this.
      @param nr of positions to displace. Negative: Displace to left.
      @return <code>this</code> to concat some operations, like <code>part.set(src).seek(sKey).lento(';').len0end();</code>
  */
  public StringPart seek(int nr)
  { startLast = start;
    start += nr;
    if(start > end)
    	/**@java2c=StringBuilderInThreadCxt.*/ 
    	throwIndexOutOfBoundsException("seek=" + nr + " start=" + (start-nr) + " end=" + end);
    else if(start < startMin) 
    	/**@java2c=StringBuilderInThreadCxt.*/
    	throwIndexOutOfBoundsException("seek=" + nr + " start=" + (start-nr) + " start-min=" + startMin);
    bFound = true;
    return this;
  }





  /** Displaces the start of the part to the first char it is no whitespace.
      If the current char at seek position is not a whitespace, the method has no effect.
      If only whitespaces are founded to the end of actual part, the position is set to this end.

      <hr/><u>example:</u><pre>
abcdefghijklmnopqrstuvwxyz  The associated String
  =================         The maximal part
    ----------              The valid part before
       +++++++              The valid part after, if 'defg' are whitespaces
    ++++++++++              The valid part after is the same as before, if no whitespace at current position
             .              The valid part after is emtpy, if only whitespaces re found.
      </pre>
   *  @java2c=return-this.
      @return <code>this</code> to concat some operations, like <code>part.set(src).seek(sKey).lento(';').len0end();</code>
  */
  public StringPart seekNoWhitespace()
  { startLast = start;
    while( start < end && " \t\r\n\f".indexOf(content.charAt(start)) >=0 )
    { start +=1;
    }
    bFound = (start > startLast);
    return this;
  }


  /*=================================================================================================================*/
  /*=================================================================================================================*/
  /*=================================================================================================================*/
  /** skip over comment and whitespaces
  */

  /**@deprecated see {@link seekNoWhitespaceOrComments()}
   *  @java2c=return-this.
   * 
   */ 
  protected StringPart skipWhitespaceAndComment()
  { return seekNoWhitespaceOrComments();
  }

  
  /** Displaces the start of the part to the first char it is no whitespace or comment.
      If the current char at seek position is not a whitespace or not the beginning of a comment, the method has no effect.
      If only whitespaces and comments are found to the end of actual part, the position is set to its end.

      <hr/><u>example:</u><pre>
abcdefghijklmnopqrstuvwxyz  The associated String
  =================         The maximal part
    ----------              The valid part before
       +++++++              The valid part after, if 'defg' are whitespaces
    ++++++++++              The valid part after is the same as before, if no whitespace at current position
             .              The valid part after is emtpy, if only whitespaces re found.
      </pre>
      @java2c=return-this.
      @return <code>this</code> to concat some operations, like <code>part.set(src).seek(sKey).lento(';').len0end();</code>
  */
  public StringPart seekNoWhitespaceOrComments()
  { int start00 = start;
    int start0;
    do
    { start0 = start;
      if( (bitMode & mSkipOverWhitespace_mode) != 0)
      { seekNoWhitespace();
      }
      if( (bitMode & mSkipOverCommentInsideText_mode) != 0)   
      { if(getCurrentPart().startsWith(sCommentStart))
        { seek(sCommentEnd, seekEnd);  
        }
      }
      if( (bitMode & mSkipOverCommentToEol_mode) != 0)   
      { if(getCurrentPart().startsWith(sCommentToEol))
        { seek('\n', seekEnd);  
        }
      }
    }while(start != start0);  //:TRICKY: if something is done, repeat all conditions.
    bFound = (start > start00);
    return this;
  }


  /**
   * 
   * @deprecated see {@link found()}
   */
  boolean isFound(){ return found(); }
  

  /** Returns true, if the last called seek__(), lento__() or skipWhitespaceAndComment()
   * operation founds the requested condition. This methods posits the current Part in a appropriate manner
   * if the seek or lento-conditions were not prosperous. In this kinds this method returns false.
   * @return true if the last seek__(), lento__() or skipWhitespaceAndComment()
   * operation matches the condition.
   */
  public boolean found()
  { return bFound;
  }
  
  

  /** Displaces the start of the part to the leftest possible start.
      <hr/><u>example:</u><pre>
abcdefghijklmnopqrstuvwxyz  The associated String
  =================         The maximal part
         -----              The valid part before
  ++++++++++++              The valid part after calling seekBegin().
      </pre>
   *  @java2c=return-this.
      @return <code>this</code> to concat some operations, like <code>part.set(src).seek(sKey).lento(';').len0end();</code>
  */
  protected StringPart seekBegin()
  { start = startLast = startMin;
    return this;
  }










  /** Searchs the given String inside the valid part, posits the start of the part to the begin of the searched string.
      The end of the part is not affected.
      If the string is not found, the start is posit on the actual end. The length()-method supplies 0.
      Methods such fromEnd() are not interacted from the result of the searching.
      The rule is: seek()-methods only shifts the start position.

      <hr/><u>example:</u><pre>
that is a liststring and his part The associated String
  =============================   The maximal part
      ----------------------      The valid part before
           +++++++++++++++++      The valid part after seek("is",StringPart.seekNormal).
             +++++++++++++++      The valid part after seek("is",StringPart.seekEnd).
                          ++      The valid part after seek("is",StringPart.back).
                           .      The valid part after seek("is",StringPart.back + StringPart.seekEnd).
     +++++++++++++++++++++++      The valid part after seek("is",StringPart.seekToLeft).
       +++++++++++++++++++++      The valid part after seek("is",StringPart.seekToLeft + StringPart.seekEnd).
  ++++++++++++++++++++++++++      The valid part after seek("xx",StringPart.seekToLeft).
                           .      The valid part after seek("xx",StringPart.seekNormal)
                                  or seek("xx",StringPart.back).

      </pre>
   *  @java2c=return-this.
      @param sSeek The string to search for.
      @param mode Mode of seeking, use ones of back, seekToLeft, seekNormal, added with seekEnd.
      @return <code>this</code> to concat some operations, like <code>part.set(src).seek(sKey).lento(';').len0end();</code>
  */
  public StringPart seek(String sSeek, int mode)
  { startLast = start;
    String sSeekArea;
    int posNotFound;  //position if not found in dependence of area of seek and direction
    if( (mode & mSeekToLeft_) == mSeekToLeft_)
    { int posAreaEnd = start + sSeek.length() -1;  //the sSeek-string may be start at (start-1)
      if(posAreaEnd > endMax) posAreaEnd = endMax;  //but not over the end.
      sSeekArea = content.substring(startMin, posAreaEnd );
      posNotFound = start; //if not found, the rightest position of area
    }
    else
    { sSeekArea = content.substring(start, end );
      posNotFound = end; //if not found, the rightest position of area
    }

    int pos;
    if( (mode & mSeekBackward_) == mSeekBackward_)
    { pos = sSeekArea.lastIndexOf(sSeek);
    }
    else
    { pos = sSeekArea.indexOf(sSeek);
    }

    if(pos < 0)
    { start = posNotFound;
      bFound = false;   
    }
    else
    { bFound = true;
      if( (mode & mSeekToLeft_) == mSeekToLeft_) start = startMin + pos;
      else                                     start = start + pos;
      if( (mode & seekEnd) == seekEnd )
      { start += sSeek.length();
      }
    }

    return this;
  }


  
  /** Searchs the given String inside the valid part, posits the start of the part to the begin of the searched string.
   *  The end of the part is not affected.<br>
   *  If the string is not found, the start is posit to the actual end. The length()-method supplies 0.
      Methods such fromEnd() are not interacted from the result of the searching.
      The rule is: seek()-methods only shifts the start position.<br>
      see {@link seek(String sSeek, int mode)}
   * @java2c=return-this.
     @param strings List of String contains the strings to search.
   * @param nrofFoundString If given, [0] is set with the number of the found String in listStrings, 
   *                        count from 0. This array reference may be null, then unused.
   * @return this.       
   */  
  public StringPart seekAnyString(String[] strings, int[] nrofFoundString)
  //public StringPart seekAnyString(List<String> strings, int[] nrofFoundString)
  { startLast = start;
    int pos;
    pos = indexOfAnyString(strings, 0, Integer.MAX_VALUE, nrofFoundString, null);
    if(pos < 0)
    { bFound = false;   
      start = end;
    }
    else
    { bFound = true;
      start = start + pos;
    }
    return this;
  }


  
  
  
  
  

  /** Searchs the given character inside the valid part, posits the start of the part to the begin of the searched char.
      The end of the part is not affected.
      If the string is not found, the start is posit on the actual end
      or, if mode contents seekBack, the begin of the maximal part. 
      In this cases isFound() returns false and a call of restoreLastPart() restores the old parts.
      The length()-method supplies 0.
      Methods such fromEnd() are not interacted from the result of the searching.
      The rule is: seek()-methods only shifts the start position.<br/>
      The examples are adequate to seek(String, int mode);

   *  @java2c=return-this.
      @param cSeek The character to search for.
      @param mode Mode of seeking, use ones of back, seekToLeft, seekNormal, added with seekEnd.
      @return <code>this</code> to concat some operations, like <code>part.set(src).seek(sKey).lento(';').len0end();</code>
  */
  public StringPart seek(char cSeek, int mode)
  { startLast = start;
    String sSeekArea;
    int posNotFound;  //position if not found in dependence of area of seek and direction
    if( (mode & mSeekToLeft_) == mSeekToLeft_)
    { int posAreaEnd = start;  //the sSeek-string may be start at (start-1)
      if(posAreaEnd > endMax) posAreaEnd = endMax;  //but not over the end.
      sSeekArea = content.substring(startMin, posAreaEnd );
      posNotFound = start; //if not found, the rightest position of area
    }
    else
    { sSeekArea = content.substring(start, end );
      posNotFound = end; //if not found, the rightest position of area
    }
    int pos;
    if( (mode & mSeekBackward_) == mSeekBackward_) pos = sSeekArea.lastIndexOf(cSeek);
    else                                         pos = sSeekArea.indexOf(cSeek);

    if(pos < 0)
    { start = posNotFound;
      bFound = false;   
    }
    else
    { bFound = true;
      if( (mode & mSeekToLeft_) == mSeekToLeft_) start = startMin + pos;
      else                                     start = start + pos;
      if( (mode & seekEnd) == seekEnd )
      { start += 1;
      }
    }

    return this;
  }



  /** Searchs the given String inside the valid part, posits the start of the part after the end of the searched string.
      The end of the part is not affected.
      <pre>sample: seek("fgh") results is:
                abcdefghijabcdefghijklmnopq
      before:            ============
      after:                      ===
      not:              =============
                             </pre>
      If the string is not found, the start is posit on the actual end. The length()-method supplies 0.
      Methods such fromEnd() are not interacted from the result of the searching.
      The rule is: seek()-methods only shifts the start position.
   *  @java2c=return-this.
      @param sSeek The string to search for.
      @return <code>this</code> to concat some operations, like <code>part.set(src).seek(sKey).lento(';').len0end();</code>
  */
  public StringPart xxxseekEnd(String sSeek)
  { startLast = start;
    int pos = content.indexOf(sSeek, start);
    if(pos >= 0) pos += sSeek.length();
    if(pos > end || pos <0 ) start = end;
    else                     start = pos;
    return this;
  }



  /** Posits the start of the part after all of the chars given in the parameter string.
      The end of the part is not affected.
      <pre>sample: seekNoChar("123") result is:
                12312312312abcd12312efghij123123
      before:       ==========================
      after:               ===================
                             </pre>
   *  @java2c=return-this.
      @param sChars String with the chars to overread.
      @return <code>this</code> to concat some operations, like <code>part.set(src).seek(sKey).lento(';').len0end();</code>
  */
  public StringPart seekNoChar(String sChars)
  { startLast = start;
    while(start < end && sChars.indexOf(content.charAt(start)) >=0) start +=1;
    if(start < end) bFound = true;
    else bFound = false;
    return this;
  }






  /**Returns the position of one of the chars in sChars within the part, started inside the part with fromIndex,
     returns -1 if the char is not found in the part started from 'fromIndex'.
    @param sChars contents some chars to find. The char with code 
    @param fromIndex start of search within the part.
    @param maxToTest maximal numbers of chars to test. It may be Integer.MAX_VALUE. 
    @return position of first founded char inside the actual part, but not greater than maxToTest, if no chars is found unitl maxToTest,
            but -1 if the end is reached.
  */
  public int indexOfAnyChar(String sChars, final int fromWhere, final int maxToTest)
  { int pos = start + fromWhere;
    int max = (end - pos) < maxToTest ? end : pos + maxToTest;
    while(pos < max && sChars.indexOf(content.charAt(pos)) < 0) pos +=1;
    int nChars = pos - start;
    if(pos < max 
      || (pos == max && sChars.indexOf(cEndOfText) >= 0)
      )
    { nChars = pos - start;
    }
    else { nChars = -1; }
    return nChars;
  }

  
  
  /**Returns the last position of one of the chars in sChars 
   * within the part of actual part from (fromIndex) to (fromIndex+maxToTest) 
   * or returs -1 if the char is not found in this part.
    @param sChars contents some chars to find. The char with code 
    @param fromIndex start of search within the part.
    @param maxToTest maximal numbers of chars to test. It may be Integer.MAX_VALUE. 
    @return position of first founded char inside the actual part, but not greater than maxToTest. 
           if no chars is found unitl maxToTest,
            but -1 if the end is reached.
  */
  public int lastIndexOfAnyChar(String sChars, final int fromWhere, final int maxToTest)
  { int pos = (end - start) < maxToTest ? end-1 : start + maxToTest-1;
    int min = start + fromWhere;
    
    while(pos >= min && sChars.indexOf(content.charAt(pos)) < 0)
    { pos -=1;
    }
    int index = pos >= min 
              ? pos - start  //relative found position
              :  -1;         //not found
    return index;
  }

  
  
  /**Returns the position of one of the chars in sChars within the part, started inside the part with fromIndex,
   *  returns -1 if the char is not found in the part started from 'fromIndex'.
   * @param listStrings contains some Strings to find.
   * @param fromWhere start of search within the part.
   * @param maxToTest maximal numbers of chars to test. It may be Integer.MAX_VALUE. 
   * @param nrofFoundString If given, [0] is set with the number of the found String in listStrings, 
   *                        count from 0. This array reference may be null, then unused.
   * @param foundString If given, [0] is set with the found String. This array reference may be null.
   * @return position of first founded char inside the actual part, but not greater than maxToTest, 
   *                 if no chars is found until maxToTest, but -1 if the end is reached.
   */
  public int indexOfAnyString
  ( String[] listStrings
  , final int fromWhere
  , final int maxToTest
  , int[] nrofFoundString
  , String[] foundString
  )
  { int pos = start + fromWhere;
    int max = (end - pos) < maxToTest ? end : pos + maxToTest;
    //int endLast = end;
    //StringBuffer sFirstCharBuffer = new StringBuffer(listStrings.size());
    assert(listStrings.length < 100);  //static size is need
    /** @java2c=stackInstance.*/
    StringBuffer sFirstCharBuffer = new StringBuffer(100);
    //Iterator<String> iter = listStrings.iterator();
    boolean acceptToEndOfText = false;
    //while(iter.hasNext())
    /**Compose a String with all first chars, to test whether a current char of src is equal. */
    { int ii = -1;
      while(++ii < listStrings.length)
      { //String sString = (String)(iter.next());
        String sString = listStrings[ii];
        if(sString.charAt(0) == cEndOfText)
        { acceptToEndOfText = true;}
        else 
        { sFirstCharBuffer.append(sString.charAt(0)); }
    } }
    /**@java2c=toStringNonPersist.*/
    String sFirstChars = sFirstCharBuffer.toString();
    boolean found = false;
    while(!found && pos < max)
    { 
    	int nrofFoundString1 = -1;
      /**increment over not matching chars, test all first chars: */
      while(pos < max && (nrofFoundString1 = sFirstChars.indexOf(content.charAt(pos))) < 0) pos +=1;
      
      if(pos < max)
      { /**a fist matching char is found! test wether or not the whole string is matched.
         * Test all Strings, the first test is the test of start char. */
        int ii = -1;
        while(!found && ++ii < listStrings.length)  //NOTE: don't use for(...) because found is a criterium of break.
        { //String sString = (String)(iter.next());
          String sString = listStrings[ii];
          int testLen = sString.length();
          if((max - pos) >= testLen 
            && substring(pos, pos + testLen).equals(sString)
            ) 
          { found = true;
            if(foundString != null)
            { foundString[0] = sString;
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
    int nChars;
    if(pos < max 
      || (pos == max && acceptToEndOfText)
      )
    { nChars = pos - start;
    }
    else { 
    	nChars = -1; 
      if(foundString != null)
      { foundString[0] = null;
      }
      if(nrofFoundString != null)
      { nrofFoundString[0] = -1;
      }
    }
    return nChars;
  }

  
  
  /** Searches any char contented in sChars,
   * but skip over quotions while testing. Example: The given string is<pre>
   * abc "yxz" end:zxy</pre>
   * The calling is<pre>
   * lentoAnyCharOutsideQuotion("xyz", 20);</pre>
   * The result current part is<pre>
   * abc "yxz" end:</pre>
   * because the char 'z' is found first as the end char, but outside the quoted string "xyz".
   * @param sChars One of this chars is a endchar. It may be null: means, every chars is a endchar.
   * @param fromWhere Offset after start to start search. It may be 0 in many cases.
   * @param maxToTest
   * @return
   */
  public int indexOfAnyCharOutsideQuotion(String sChars, final int fromWhere, final int maxToTest)
  { int pos = start + fromWhere;
    int max = (end - pos) < maxToTest ? end : start + maxToTest;
    boolean bNotFound = true;
    while(pos < max && bNotFound)
    { char cc = content.charAt(pos);
      if(cc == '\"')
      { int endQuotion = indexEndOfQuotion('\"', pos - start, max - start);
        if(endQuotion < 0){ pos = max; }
        else{ pos = endQuotion + start; }
      }
      else
      { if(sChars.indexOf(cc) >= 0){ bNotFound = false; }
        else{ pos +=1; }
      }
    }
    return (bNotFound) ? -1 : (pos - start);
  }

  
  
  

  /** Searches the end of a quotion string.
   * @param fromWhere Offset after start to start search. 
   *                  It may be 0 if the quotion starts at start, it is the position of the left
   *                  quotion mark.
   * @param maxToTest Limit for searching, offset from start. It may be Integer.MAX_INT
   * @return -1 if no end of quotion is found, else the position of the char after the quotion, 
   *          at least 2 because a quotion has up to 2 chars, the quotion marks itself.
   */
  public int indexEndOfQuotion(char cEndQuotion, final int fromWhere, final int maxToTest)
  { int pos = start + fromWhere +1;
    int max = (end - pos) < maxToTest ? end : pos + maxToTest;
    boolean bNotFound = true;
    while(pos < max && bNotFound)
    { char cc = content.charAt(pos++);
      if(cc == '\\' && (pos+1) < max)
      { pos += 1; //on \ overread the next char, test char after them!
      }
      else if(cc == cEndQuotion)
      { bNotFound = false;
      }
    }
    return (bNotFound ? -1 : (pos - start));
  }

  
  
  

  /**Returns the position of one of the chars in sChars within the part,
     returns -1 if the char is not found in the actual part.
    @param sChars contents some chars to find.
    @return position of first founded char inside the actual part or -1 if not found.
  */
  public int indexOfAnyChar(String sChars)
  { return indexOfAnyChar(sChars, 0, Integer.MAX_VALUE);
  }
  

  
  /**Returns the position of the first char other than the chars in sChars within the part, started inside the part with fromIndex,
     returns -1 if all chars inside the parts  started from 'fromIndex' are chars given by sChars.
    @param sChars contents the chars to overread.
    @param fromIndex start of search within the part.
    @return position of first foreign char inside the actual part or -1 if not found.
  */
  public int indexOfNoChar(String sChars, final int fromWhere)
  { int pos = start + fromWhere;
    while(pos < end && sChars.indexOf(content.charAt(pos)) >= 0) pos +=1;
    return (pos >= end) ? -1 : (pos - start);
  }
  

  /**Returns the position of the first char other than the chars in sChars within the part,
     returns -1 if all chars inside the parts are chars given by sChars.
    @param sChars contents the chars to overread.
    @return position of first foreign char inside the actual part or -1 if not found.
  */
  public int indexOfNoChar(String sChars)
  { return indexOfNoChar(sChars, 0);
  }
  

  
  /** Sets the length of the current part to any char content in sChars (terminate chars). 
   * If a terminate char is not found, the length of the current part is set to 0.
   * The same result occurs, if a terminate char is found at begin of the current part.
   * If the difference of this behavior is important, use instead indexOfAnyChar() and test the
   * return value, if it is &lt; 0. 
   * @java2c=return-this.
   * @param sChars Some chars searched as terminate char for the actual part.
   * @param maxToTest Maximum of chars to test. If the endchar isn't find inside this number of chars,
   *        the actual length is set to 0.
   * @param mode Possible values are StringPart.seekBack or StringPart.seekNormal = 0.       
   * @return This itself.
   */
  public StringPart lentoAnyChar(String sChars, int maxToTest)
  { return lentoAnyChar(sChars, maxToTest, seekNormal);
  }

  
  
  /** Sets the length of the current part to any char content in sChars (terminate chars). 
   * If a terminate char is not found, the length of the current part is set to 0.
   * The same result occurs, if a terminate char is found at begin of the current part.
   * If the difference of this behavior is important, use instead indexOfAnyChar() and test the
   * return value, if it is &lt; 0. 
   * @java2c=return-this.
   * @param sChars Some chars searched as terminate char for the actual part.
   * @param maxToTest Maximum of chars to test. If the endchar isn't find inside this number of chars,
   *        the actual length is set to 0.
   * @param mode Possible values are StringPart.seekBack or StringPart.seekNormal = 0.       
   * @return This itself.
   */
  public StringPart lentoAnyChar(String sChars, int maxToTest, int mode)
  { endLast = end;
    int pos;
    if((mode & mSeekBackward_) != 0)
    { pos = lastIndexOfAnyChar(sChars, 0, maxToTest);
    }
    else
    { pos = indexOfAnyChar(sChars, 0, maxToTest);
    }
    if(pos < 0){ end = start; bFound = false; }
    else       { end = start + pos; bFound = true; } 
    return this;
  }


  
  /** Sets the length of the current part to any terminate string given in sString. 
   * If a terminate string is not found, the length of the current part is set to 0.
   * The same result occurs, if a terminate string is found at begin of the current part.
   * If the difference of this behavior is important, use instead indexOfAnyChar() and test the
   * return value, if it is &lt; 0. 
   * @java2c=return-this.
   * @param sString The first char is the separator. 
   * @param maxToTest Maximum of chars to test. If the endchar isn't find inside this number of chars,
   *        the actual length is set to 0.
   * @return This itself.
   */
  public StringPart lentoAnyString(String[] strings, int maxToTest)
  //public StringPart lentoAnyString(List<String> strings, int maxToTest)
  { return lentoAnyString(strings, maxToTest, seekNormal);
  }

  
  
  /** Sets the length of the current part to any terminate string given in sString. 
   * If a terminate string is not found, the length of the current part is set to 0.
   * The same result occurs, if a terminate string is found at begin of the current part.
   * If the difference of this behavior is important, use instead indexOfAnyChar() and test the
   * return value, if it is &lt; 0. 
   * @java2c=return-this.
   * @param sString The first char is the separator. 
   * @param maxToTest Maximum of chars to test. If the endchar isn't find inside this number of chars,
   *        the actual length is set to 0.
   * @param mode possible values are StrinPart.seekNormal or StringPart.seekEnd.
   *        <ul><li>StringPart.seekEnd: the found string is inclusive.
   *        </ul>       
   * @return This itself.
   */
  public StringPart lentoAnyString(String[] strings, int maxToTest, int mode)
  //public StringPart lentoAnyString(List<String> strings, int maxToTest, int mode)
  { endLast = end;
    /**@java2c=stackInstance. It is only used internally. */
    String[] foundString = new String[1];
    int pos = indexOfAnyString(strings, 0, maxToTest, null, foundString);
    if(pos < 0){ end = start; bFound = false; }
    else       
    { if( (mode & seekEnd) != 0)
      { pos += foundString[0].length();
      }
      end = start + pos; bFound = true; 
    } 
    return this;
  }


  
  /** Sets the length of the current part to any terminate string given in sString. 
   * If a terminate string is not found, the length of the current part is set to 0.
   * The same result occurs, if a terminate string is found at begin of the current part.
   * If the difference of this behavior is important, use instead indexOfAnyChar() and test the
   * return value, if it is &lt; 0.
   * <br>
   * This method consideres the indent of the first line. In followed lines all chars are skipped 
   * if there are inclose in sIndentChars until the column position of the first line. 
   * If another char not inclosed in sIndentChars is found, than it is the beginning of this line.
   * If the last char in sIndentChars is a space " ", additional all spaces and tabs '\t' will be
   * skipped. This method is helpfull to convert indented text into a string without the indents, at example:
   * <pre>
   * . /** This is a comment
   * .   * continued in a next line with indent.
   * .  but it is able that the user doesn't respect the indentation
   * .        also with to large indentation,
   * .   * *The second asterix should not be skipped.
   * </pre>
   * From this text passage the result is:
   * <pre>
   * .This is a comment
   * .continued in a next line with indent.
   * .but it is able that the user doesn't respect the indentation
   * .also with to large indentation,
   * .*The second asterix should not be skipped.
   * </pre>
   * Using the result it is possible to detect paragraph formatting in wikipedia style 
   * (see vishia.xml.ConvertWikistyleTextToXml.java) 
   *   
   * @param strings List of type String, containing the possible end strings.
   * @param iIndentChars possible chars inside a skipped indentation. If the last char is space (" "),
   *        also spaces after the indentation of the first line are skipped. 
   * @param maxToTest Maximum of chars to test. If the endchar isn't find inside this number of chars,
   *        the actual length is set to 0.
   * @param buffer The buffer where the found String is stored. The stored String has no indentations.       
   * @since 2007, 2010-0508 changed param buffer, because better useable in C (java2c)
   */
  public void lentoAnyStringWithIndent(String[] strings, String sIndentChars, int maxToTest, StringBuilder buffer)
  //public String lentoAnyStringWithIndent(List<String> strings, String sIndentChars, int maxToTest)
  { endLast = end;
    //String sRet; sRet = "";
    buffer.setLength(0);
    int indentColumn = getCurrentColumn();
    int startLine = start;
    boolean bAlsoWhiteSpaces = (sIndentChars.charAt(sIndentChars.length()-1) == ' ');
    int pos = indexOfAnyString(strings, 0, maxToTest, null, null);
    if(pos < 0){ end = start; bFound = false; }
    else       
    { this.bFound = true;
      this.end = this.start + pos; 
      boolean bFinish = false;
      while(!bFinish)  
      { pos = content.indexOf('\n', startLine);
        if(pos < 0) pos = this.end;
        if(pos > this.end)
        { //next newline after terminated string, that is the last line.
          pos = this.end;
          bFinish = true;
        }
        else { pos +=1; } // '\n' including
        //append the line to output string:
        buffer.append(content.substring(startLine, pos));
        if(!bFinish)
        { //skip over indent.
          startLine = pos;
          int posIndent = startLine + indentColumn;
          if(posIndent > end) posIndent = end;
          while(startLine < posIndent && sIndentChars.indexOf(content.charAt(startLine)) >=0)
          { startLine +=1;
          }
          if(bAlsoWhiteSpaces)
          { while(" \t".indexOf(content.charAt(startLine)) >=0)
            { startLine +=1;
            }
          }
        }
      }  
    } 
    return ; //buffer.toString();
  }


  
  /** Sets the length of the current part to any char content in sChars (terminate chars),
   * but skip over quotions while testing. Example: The given string is<pre>
   * abc "yxz" ende:zxy</pre>
   * The calling is<pre>
   * lentoAnyCharOutsideQuotion("xyz", 20);</pre>
   * The result current part is<pre>
   * abc "yxz" ende:</pre>
   * because the char 'z' is found first as the end char, but outside the quoted string "xyz".<br/>
   * If a terminate char is not found, the length of the current part is set to 0.
   * The same result occurs, if a terminate char is found at begin of the current part.
   * If the difference of this behavior is important, use instead indexOfAnyChar() and test the
   * return value, if it is &lt; 0. 
   * @java2c=return-this.
   * @param sChars Some chars searched as terminate char for the actual part.
   * @param maxToTest Maximum of chars to test. If the endchar isn't find inside this number of chars,
   *        the actual length is set to 0.
   * @return This itself.
   */
  public StringPart lentoAnyCharOutsideQuotion(String sChars, int maxToTest)
  { endLast = end;
    int pos = indexOfAnyCharOutsideQuotion(sChars, 0, maxToTest);
    if(pos < 0){ end = start; bFound = false; }
    else       { end = start + pos; bFound = true; } 
    return this;
  }


  /** Sets the length of the current part to the end of the quotion. It is not tested here,
   * whether or not the actual part starts with a left quotion mark.  
   * @java2c=return-this.
   * @param sEndQuotion The char determine the end of quotion, it may be at example " or ' or >.
   * @param maxToTest Maximum of chars to test. If the endchar isn't find inside this number of chars,
   *        the actual length is set to 0.
   * @return This itself.
   */
  public StringPart lentoQuotionEnd(char sEndQuotion, int maxToTest)
  { endLast = end;
    int pos = indexEndOfQuotion(sEndQuotion, 0, maxToTest);
    if(pos < 0){ end = start; bFound = false; }
    else       { end = start + pos; bFound = true; } 
    return this;
  }


  /** Sets the length of the current part to any char content in sChars (terminate chars). 
   * If a terminate char is not found, the length of the current part is set to 0.
   * The same result occurs, if a terminate char is found at begin of the current part.
   * If the difference of this behavior is important, use instead indexOfAnyChar() and test the
   * return value, if it is &lt; 0. 
   * @java2c=return-this.
   * @param sChars Some chars searched as terminate char for the actual part.
   * @return This itself.
   */
  public StringPart lentoAnyChar(String sChars)
  { lentoAnyChar(sChars, Integer.MAX_VALUE);
    return this;
  }



  /**Sets the length to the end of the maximal part if the length is 0. This method could be called at example
     if a end char is not detected and for that reason the part is valid to the end.
   * @java2c=return-this.
      @return <code>this</code> to concat some operations, like <code>part.set(src).seek(sKey).lento(';').len0end();</code>
  */
  public StringPart len0end()
  { if(end <= start) end = endMax;
    return this;
  }



  /**Sets the length to the end of the maximal part.
   * @java2c=return-this.
  */
  public StringPart setLengthMax()
  { end = endMax;
    return this;
  }



  /** Posits the end of the part before all of the chars given in the parameter string.
      The start of the part is not affected.
      <pre>sample: lentoBacktoNoChar("123") result is:
                1231231231abcd12312efghij123123123klmnopq
      before:       ==========================
      after:        =====================
                             </pre>
   * @java2c=return-this.
      @param sChars String with the chars to overread.
      @return <code>this</code> to concat some operations, like <code>part.set(src).seek(sKey).lento(';').len0end();</code>
  */
  public StringPart lenBacktoNoChar(String sChars)
  { endLast = end;
    while( (--end) >= start && sChars.indexOf(content.charAt(end)) >=0);
    if(end < start)
    { end = start; bFound = false;
    }
    else bFound = true;
    return this;
  }





  /** Trims all leading and trailing whitespaces within the part.
      A Comment begins with "//".
   * @java2c=return-this.
      @return <code>this</code> to concat some operations, like <code>part.set(src).seek(sKey).lento(';').len0end();</code>
  */
  StringPart trim()
  { return seekNoChar(" \t\n\r").lenBacktoNoChar(" \t\n\r");
  }





  /** Trims a java- or C-style line-comment from end of part and all leading and trailing whitespaces.
      A Comment begins with "//".
   * @java2c=return-this.
      @return <code>this</code> to concat some operations, like <code>part.set(src).seek(sKey).lento(';').len0end();</code>
  */
  StringPart trimComment()
  { startLast = start;
    endLast = end;
    int posComment = indexOf("//");
    if(posComment >=0) end = start + posComment;
    bFound = (start > startLast);
    return trim();
  }





  /** Returns the position of the char within the part,
   * returns -1 if the char is not found in the part.
     The methode is likely String.indexOf().
    @param ch character to find.
    @return position of the char within the part or -1 if not found within the part.
    @exception The method throws no IndexOutOfBoundaryException.
    It is the same behavior like String.indexOf(char, int fromEnd).
  */
  public int indexOf(char ch)
  { return content.substring(start, end).indexOf(ch);
  }

  /** Returns the position of the char within the part, started inside the part with fromIndex,
   * returns -1 if the char is not found in the part started from 'fromIndex'.
     The methode is likely String.indexOf().
    @param ch character to find.
    @param fromIndex start of search within the part.
    @return position of the char within the part or -1 if not found within the part.
    @exception The method throws no IndexOutOfBoundaryException. If the value of fromIndex
    is negativ or greater than the end position, -1 is returned (means: not found).
    It is the same behavior like String.indexOf(char, int fromEnd).
  */
  public int indexOf(char ch, int fromIndex)
  { if(fromIndex >= (end - start) || fromIndex < 0) return -1;
    else
    { int pos = content.substring(start + fromIndex, end).indexOf(ch);
      if(pos < 0) return -1;
      else return pos + fromIndex;
    }
  }


  /** Returns the position of the string within the part. Returns -1 if the string is not found in the part.
      Example: indexOf("abc") returns 6, indexOf("fgh") returns -1 <pre>
         abcdefgabcdefghijk
  part:   =============  </pre>
    @param sCmp string to find
    @return position of the string within the part or -1 if not found within the part.
  */
  public int indexOf(String sCmp)
  { return content.substring(start, end).indexOf(sCmp);
  }




  /** Returns the position of the string within the part. Returns -1 if the string is not found in the part.
      Example: indexOf("abc") returns 6, indexOf("fgh") returns -1 <pre>
         abcdefgabcdefghijk
  part:   =============  </pre>
    @param sCmp string to find
    @return position of the string within the part or -1 if not found within the part.
  */
  public int indexOf(String sCmp, int fromIndex, int maxToTest)
  { int max = (end - start) < maxToTest ? end : start + maxToTest;
    if(fromIndex >= (max - start) || fromIndex < 0) return -1;
    else
    { int pos = content.substring(start + fromIndex, max).indexOf(sCmp);
      if(pos < 0) return -1;
      else return pos + fromIndex;
    }
  }





  /** Compares the Part of string with the given string
  */
  public boolean equals(String sCmp)
  { return content.substring(start, end).equals(sCmp);
  }




  /**compares the Part of string with the given string.
   * new since 2008-09: if sCmp contains a cEndOfText char (coded with \e), the end of text is tested.
   * @param sCmp The text to compare.
  */
  public boolean startsWith(String sCmp)
  { int pos_cEndOfText = sCmp.indexOf(cEndOfText);
    
    if(pos_cEndOfText >=0)
    { if(pos_cEndOfText ==0)
      { return start == end;
      }
      else
      { return content.substring(start, end).equals(sCmp);
      }
      
    }
    else
    { return content.substring(start, end).startsWith(sCmp);
    }
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
  public StringPart scan(final String sTestP)
  { if(bCurrentOk)   //NOTE: do not call scanEntry() because it returns false if end of text is reached,
    {                //      but the sTestP may contain only cEndOfText. end of text will be okay than.
      seekNoWhitespaceOrComments();
      String sTest;
      int len = sTestP.indexOf(cEndOfText);
      boolean bTestToEndOfText = (len >=0);
      if(bTestToEndOfText){ sTest = sTestP.substring(0, len); }
      else { len = sTestP.length(); sTest = sTestP; }
      //int len = sTest.length();
      if(  (start + len) <= endMax //content.length()
        && sTest.equals(content.substring(start, start+len))
        && (  !bTestToEndOfText 
           || start + len == end
           )
        )
      { start += len;
      }
      else 
      { bCurrentOk = false; 
        //if(report != null){ report.report(6,"ErrorScan scan(" + sTest + ")");}      //error in current scanning
      }
    }
    return this;
  }

  
  /** Test the result of scanning and set the scan Pos Ok, if current scanning was ok. If current scanning
      was not ok, this method set the current scanning pos back to the position of the last call of scanOk()
      or scanNext() or setCurrentOk().
      @return true if the current scanning was ok, false if it was not ok.
  */

  public boolean scanOk()
  { if(bCurrentOk) 
    { startScan = startLast = start;    //the scanOk-position is the start of maximal part.
      bStartScan = true;   //set all idxLast... to 0
    }
    else           
    { start = startLast= startScan;   //return to the start
    }
    //if(report != null){ report.report(6," scanOk:" + startMin + ".." + start + ":" + (bCurrentOk ? "ok" : "error")); }
    boolean bOk = bCurrentOk;
    bCurrentOk = true;        //prepare to next try scanning
    return(bOk);
  }


  /**
   * @java2c=return-this.
   * @param sQuotionmarkStart
   * @param sQuotionMarkEnd
   * @param sResult
   * @return
   */
  public StringPart scanQuotion(String sQuotionmarkStart, String sQuotionMarkEnd, String[] sResult)
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
  public StringPart scanQuotion(String sQuotionmarkStart, String sQuotionMarkEnd, String[] sResult, int maxToTest)
  { if(scanEntry())
    { scan(sQuotionmarkStart).lentoNonEscapedString(sQuotionMarkEnd, maxToTest);
      if(bCurrentOk)
      { //TODO ...ToEndString, now use only 1 char in sQuotionMarkEnd
        if(sResult != null) sResult[0] = getCurrentPart();
        fromEnd().seek(sQuotionMarkEnd.length());
      }
      else bCurrentOk = false; 
    }
    return this;
  }
  
  
  

  /** Scans if it is a integer number, contains exclusively of digits 0..9
      @param bHex true: scan hex Digits and realize base 16, otherwise realize base 10.
      @return long number represent the digits.
  */
  private long scanDigits(boolean bHex, int maxNrofChars)
  { if(bCurrentOk)
    { long nn = 0;
      boolean bCont = true;
      int pos = start;
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
      if(pos > start)
      { start = pos;
        return nn;
        //nLastIntegerNumber = nn;
      }
      else bCurrentOk = false;  //scanning failed.
    }
    return -1; //on error
  }


  /**
   * @java2c=return-this.
   * @return
   */
  public StringPart scanStart()
  { bCurrentOk = true;
    scanOk();  //turn all indicees to ok
    return this;
  }



  private boolean scanEntry()
  { if(bCurrentOk)
    { seekNoWhitespaceOrComments();
      if(bStartScan)
      { idxLastIntegerNumber = -1;
        //idxLastFloatNumber = 0;
        //idxLastString = 0;
        bStartScan = false; 
      }
      if(start == end)
      { bCurrentOk = false; //error, because nothing to scan.
      }
    }
    return bCurrentOk;
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
  public StringPart scanPositivInteger() throws ParseException  //::TODO:: scanLong(String sPicture)
  { if(scanEntry())
    { long value = scanDigits(false, Integer.MAX_VALUE);
      if(bCurrentOk)
      { if(idxLastIntegerNumber < nLastIntegerNumber.length -2)
        { nLastIntegerNumber[++idxLastIntegerNumber] = value;
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
  public StringPart scanInteger() throws ParseException  //::TODO:: scanLong(String sPicture)
  { if(scanEntry())
    { boolean bNegativValue = false;
      if( content.charAt(start) == '-')
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
        }
        else throw new ParseException("to much scanned integers",0);
      }
    }  
    return this;
  }

  /**Scans an float expression. The result as long value is stored internally
   * and have to be got calling {@link #getLastScannedIntegerNumber()}.
   * There can stored upto 5 numbers. If more as 5 numbers are stored yet,
   * an exception is thrown. 
   * @java2c=return-this.
   * @return this
   * @throws ParseException if the buffer is not free to hold an integer number.
   */
  public StringPart scanFloatNumber() throws ParseException  //::TODO:: scanLong(String sPicture)
  { if(scanEntry())
    { long nInteger = 0, nFractional = 0;
      int nDivisorFract = 1, nExponent;
      //int nDigitsFrac;
      char cc;
      boolean bNegativValue = false, bNegativExponent = false;
      boolean bFractionalFollowed = false;
      
      if( (cc = content.charAt(start)) == '-')
      { bNegativValue = true;
        seek(1);
        cc = content.charAt(start);
      }
      if(cc == '.')
      { nInteger = 0;
        bFractionalFollowed = true;
      }
      else
      { nInteger = scanDigits(false, Integer.MAX_VALUE);
        if(bCurrentOk)
        { if(content.charAt(start) == '.')
          { bFractionalFollowed = true;
          }
        }
      }
      
      if(bCurrentOk && bFractionalFollowed)
      { seek(1); //over .
        while(getCurrentChar() == '0')
        { seek(1); nDivisorFract *=10;
        }
        //int posFrac = start;
        nFractional = scanDigits(false, Integer.MAX_VALUE);
        if(bCurrentOk)
        { //nDigitsFrac = start - posFrac;
        }
        else if(nDivisorFract >=10)
        { bCurrentOk = true; //it is okay, at ex."9.0" is found. There are no more digits after "0".
          nFractional = 0;
        }
      }   
      else {nFractional = 0; } //nDigitsFrac = 0;}
      
      if(bCurrentOk)
      { int nPosExponent = start;
        if( (cc = content.charAt(start)) == 'e' || cc == 'E')
        { seek(1);
          if( (cc = content.charAt(start)) == '-')
          { bNegativExponent = true;
            seek(1);
            cc = content.charAt(start);
          }
          if(cc >='0' && cc <= '9' )
          { nExponent = (int)scanDigits(false, Integer.MAX_VALUE);
            if(!bCurrentOk)
            { nExponent = 0;
            }
          }
          else
          { // it isn't an exponent, but a String beginning with 'E' or 'e'.
            //This string is not a part of the float number.
            start = nPosExponent;
            nExponent = 0;
          }
        }
        else{ nExponent = 0; }
      } 
      else{ nExponent = 0; }
      
      if(bCurrentOk){ 
        double result = (double)nInteger;
        if(nFractional > 0)
        { double fFrac = (double)nFractional;
          while(fFrac >= 1.0)  //the read number is pure integer, it is 0.1234
          { fFrac /= 10.0; 
          }
          fFrac /= nDivisorFract;    //number of 0 after . until first digit.
          result += fFrac;
        }
        if(bNegativValue) { result = - result; }
        if(nExponent != 0)
        { if(bNegativExponent){ nExponent = -nExponent;}
          result *= Math.pow(10, nExponent);
        }
      	if(idxLastFloatNumber < nLastFloatNumber.length -2){
      		nLastFloatNumber[++idxLastFloatNumber] = result;
	      } else throw new ParseException("to much scanned floats",0);
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
  public StringPart scanHex(int maxNrofChars) throws ParseException  //::TODO:: scanLong(String sPicture)
  { if(scanEntry())
    { long value = scanDigits(true, maxNrofChars);
      if(bCurrentOk)
      { if(idxLastIntegerNumber < nLastIntegerNumber.length -2)
        { nLastIntegerNumber[++idxLastIntegerNumber] = value;
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
  public StringPart scanHexOrDecimal(int maxNrofChars) throws ParseException  //::TODO:: scanLong(String sPicture)
  { if(scanEntry())
    { long value;
      if( content.substring(start).startsWith("0x"))
      { seek(2); value = scanDigits(true, maxNrofChars);
      }
      else
      { value = scanDigits(false, maxNrofChars);
      }
      if(bCurrentOk)
      { if(idxLastIntegerNumber < nLastIntegerNumber.length -2)
        { nLastIntegerNumber[++idxLastIntegerNumber] = value;
        }
        else throw new ParseException("to much scanned integers",0);
      }
    }
    return this;
  }

  
  /**
   * @java2c=return-this.
   * @return
   */
  public StringPart scanIdentifier()
  { return scanIdentifier(null, null);
  }
  
  
  /**
   * @java2c=return-this.
   * @param additionalStartChars
   * @param additionalChars
   * @return
   */
  public StringPart scanIdentifier(String additionalStartChars, String additionalChars)
  { if(scanEntry())
    { lentoIdentifier(additionalStartChars, additionalChars);
      if(bFound)
      { sLastString = getCurrentPart();
        start = end;  //after identifier.
      }
      else
      { bCurrentOk = false;
      }
      end = endLast;  //revert the change of length, otherwise end = end of identifier.
    } 
    return this;
  }

  
  /**Returns the last scanned integer number. It is the result of the methods
   * <ul><li>{@link #scanHex(int)}
   * <li>{@link #scanHexOrDecimal(int)}
   * <li>{@link #scanInteger()}
   * </ul>
   * @return The number in long format. A cast to int, short etc. may be necessary
   *         depending on the expectable values.
   * @throws ParseException if called though no scan routine was called. 
   */
  public long getLastScannedIntegerNumber() throws ParseException
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
  public double getLastScannedFloatNumber() throws ParseException
  { if(idxLastFloatNumber >= 0)
    { return nLastFloatNumber[idxLastFloatNumber--];
    }
    else throw new ParseException("no integer number scanned.", 0);
  }
  
  
  
  public String getLastScannedString()
  { return sLastString;
  }
  
  
  
  /*=================================================================================================================*/
  /*=================================================================================================================*/
  /*=================================================================================================================*/
  /** Gets a String with translitaration.
   *  The end of the string is determined by any of the given chars.
   *  But a char directly after the escape char \ is not detected as an end char.
   *  Example: getCircumScriptionToAnyChar("\"") ends not at a char " after an \,
   *  it detects the string "this is a \"quotion\"!".
   *  Every char after the \ is accepted. But the known subscription chars
   *  \n, \r, \t, \f, \b are converted to their control-char- equivalence.
   *  The \s and \e mean start and end of text, coded with ASCII-STX and ETX = 0x2 and 0x3.</br></br>
   *  The actual part is tested for this, after this operation the actual part begins
   *  after the getting chars!
   *  @param sCharsEnd Assembling of chars determine the end of the part.  
   * */
  public String getCircumScriptionToAnyChar(String sCharsEnd)
  { return getCircumScriptionToAnyChar_p(sCharsEnd, false);
  }
  
  
  public String getCircumScriptionToAnyCharOutsideQuotion(String sCharsEnd)
  { return getCircumScriptionToAnyChar_p(sCharsEnd, true);
  }
  
  
  private String getCircumScriptionToAnyChar_p(String sCharsEnd, boolean bOutsideQuotion)
  { String sResult;
    final char cEscape = '\\';
    int posEnd    = (sCharsEnd == null) ? end 
                  : bOutsideQuotion ? indexOfAnyCharOutsideQuotion(sCharsEnd, 0, end-start)
                                    : indexOfAnyChar(sCharsEnd);
    if(posEnd < 0) posEnd = end - start;
    //int posEscape = indexOf(cEscape);
    //search the first escape char inside the string.
    int posEscape = content.substring(start, start + posEnd).indexOf(cEscape);  
    if(posEscape < 0)
    { //there is no escape char in the current part to sCharsEnd,
      //no extra conversion is necessary.
      sResult = lento(posEnd).getCurrentPart();
    }
    else
    { //escape character is found before end
      if(content.charAt(start + posEnd-1)== cEscape)
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
  }


  
  /*=================================================================================================================*/
  /*=================================================================================================================*/
  /*=================================================================================================================*/

  /** Gets the current position, useable for rewind. This method is overwritten
   * if derived classes uses partial content.
   */ 
  public long getCurrentPosition()
  { return start;
  }
  
  
  /** Sets the current position at a fix position inside the maxPart.
   * TODO what is with rewind etc? see old StringScan.
   * Idea: the max Part is never enlargeable to left, only made smaller to rihht.
   * Thats why the left border of maxPart is useable for shift left the content
   * by reading the next content from file, if the buffer is limited, larger than necessarry for a
   * whole file's content. But all pos values should be relativ. getCurrentPos must return
   * a relativ value, if shiftness is used. this method shuld use a relativ value.
   * old:, useable for rewind. This method may be overwritten
   * if derived classes uses partial content.
   
   * @param pos the absolute position
   */ 
  public void setCurrentPosition(long pos)
  { start = (int)pos;
  }
  
  /** Gets a substring inside the maximal part
   * pos position of start relative to maxPart
   * posend exclusive position of end. If 0 or negativ, it counts from end backward.
   * */
  public String substring(int pos, int posendP)
  { int posend;
    if(posendP <=0)
    { posend = endMax - posendP; //if posendP is fault, an exception is thrown.
    }
    else
    { posend = posendP;
    }
    return content.substring(pos+startMin, posend); 
  }
  
  /** Gets the next chars from current Position.
   *  This method don't consider the spread of the actutal and maximal part.
      @param nChars number of chars to return. If the number of chars available in string
      is less than the required number, only the available string is returned.
  */

  public String getCurrent(int nChars)
  { if( (content.length() - start) < nChars) nChars = content.length() - start;
    return(content.substring(start, start + nChars));
  }

  /** Gets the next char at current Position.
  */
  public char getCurrentChar()
  { if(start < content.length()){ return content.charAt(start); }
    else /**@java2c=StringBuilderInThreadCxt.*/ throw new IndexOutOfBoundsException("end of StringPart:" + start); // return cEndOfText;
  }
 
  
  /** Gets the current position in line (column of the text).
   * It is the number of chars from the last '\n' or from beginning to the actual char.
   * @return Position of the actual char from start of line, leftest position is 0.
   */
  public int getCurrentColumn()
  {
    int pos = content.lastIndexOf('\n', start);
    if(pos < 0) return start;  //first line, no \n before
    else return start - pos -1;
  }
  
  /** Returns the actual part of the string.
   * 
   */
  public String getCurrentPart()
  { if(end > start) return content.substring(start, end);
    else            return "";
  }
  

  /** Returns the last part of the string before any seek or scan operation.
   * 
   */
  public String getLastPart()
  { if(start > startLast) return content.substring(startLast, start);
    else            return "";
  }
  

  /** Returns the actual part of the string.
   * 
   */
  public String getCurrentPart(int maxLength)
  { int max = (end - start) <  maxLength ? end : start + maxLength;
    if(end > start) return content.substring(start, max);
    else            return ""; 
  }
  

  /** Returns the actual part of the string. <br/>
      The StringPart may be used within a string concatation such as <pre>
      String xy = "abc" + spStringPart + "xyz";</pre>
      In this case the StringPart is accept as a Object and the Object.toString() is called. This method is here implemented.
      So a StringPart is usualable directly in concatation.
  */
  public String toString()
  { return getCurrentPart();
  }



  /** Returns a debug information of the content of the StringPart. This information is structured in the followed way:
      <pre>"CONTENT_FROM_BEGIN<<<34,45>>>CONTENT_PART<<<"</pre>
      whereat
      <ul>
      <li>CONTENT_FROM_BEGIN are the first 20 chars of the whole content</li>
      <li>34 in this sample is the start position</li>
      <li>45 in this sample is the exclusively end position</li>
      <li>CONTENT_PART are the first 20 chars from start position</li>
      <ul>
  */
  public String debugString()
  { int len = content.length();
    /**@java2c=StringBuilderInThreadCxt,toStringNonPersist.*/ 
    String ret = content.substring(0, len > 20 ? 20 : len) + "<<<" + start + "," + end + ">>>";
    if(start < len){
    	/**@java2c=toStringNonPersist.*/ 
      ret += content.substring(start, len > (start + 20) ? start+20: len); 
    }
    /**@java2c=toStringNonPersist.*/ 
    ret += "<<<";
    return ret;  //java2c: buffer in threadContext
  }

  
  
  
  /** Central mehtod to invoke excpetion, usefull to set a breakpoint in debug
   * or to add some standard informations.
   * @param sMsg
   * @throws IndexOutOfBoundsException
   */
  private void throwIndexOutOfBoundsException(String sMsg)
  throws IndexOutOfBoundsException
  { throw new IndexOutOfBoundsException(sMsg);
  }
  
  /**Replaces up to 20 placeholder with a given content.
   * The method creates a StringBuilder with buffer and a StringPart locally. 
   * @param src The source String, it may be a line.
   * @param placeholder An array of strings, any string of them may be found in the src. 
   * @param value An array of strings appropriate to the placeholder. Any found placeholder 
   *        will be substitute with that string. 
   * @param dst A given StringBuilder-instance. If null, then a StringBuilder will be created here
   * @return The changed string contained in dst or a created StringBuilder.
   */
  public static String replace(String src, String[] placeholder, String[] value, StringBuilder dst)
  { int len = src.length();
    int ixPos = 0;
    int nrofToken = placeholder.length;
    if(nrofToken != value.length) throw new IllegalArgumentException("token and value should have same size, lesser 20"); 
    if(dst == null){ dst = new StringBuilder(len + 100); }//calculate about 53 chars for identifier
    StringPart spPattern = new StringPart(src);
    int posPatternStart = 0;
    int posPattern;
    do
    { int[] type = new int[1];
      posPattern = spPattern.indexOfAnyString(placeholder, posPatternStart, spPattern.length(), type, null);
      if(posPattern >=0){
      	dst.append(src.substring(posPatternStart, posPattern));
        int ixValue = type[0];
        dst.append(value[ixValue]);
        posPatternStart = posPattern + placeholder[ixValue].length();
      } else { //last pattern constant part:
      	dst.append(src.substring(posPatternStart));
        posPatternStart = -1;  //mark end
      }
    }while(posPatternStart >=0);
    return dst.toString();
  }
  

  
}

