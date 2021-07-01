package org.vishia.util;

import java.io.Closeable;
import java.util.Iterator;

import org.vishia.bridgeC.IllegalArgumentExceptionJc;

/* This is an alternative to the {@link java.lang.String} which uses a shared reference to the char sequence.
 * This class is able to use if String processing is done in a closed thread. This class must not be used 
 * instead java.lang.String if the String would referenced persistently and used from more as one thread.
 * String with this class are not immutable.
 * @author Hartmut Schorrig
 *
 */

/**The StringPart class represents a flexible valid part of a character string which's spread is changeable. 
 * It may be seen as an alternative to the standard {@link java.lang.String} for the capability to build a {@link String#substring(int)}.
 * <ul>
 * <li>1. The substring or Part of the String can be build with some operations, {@link #seek(CharSequence, int)}, {@link #lento(CharSequence)} etc.
 * <li>2. This class represents a Part of the String which is able to change.
 * <li>3. The operation to build a Part does not build an independent String, but refers inside the given String.
 * <li>4. The Part is able to build from any CharSequence, especially from a StringBuilder or from any char[]-Array.
 * </ul>
 * <b>Calculation time and memory effect</b>:<br>
 * The 3. minute affects the calculation time for extensive using of parts of a String. The {@link String#substring(int)} method
 * of standard Java till Version 6 builds a substring using and references the stored parent String. It was a cheap operation 
 * in calculation time. 
 * <br><br>
 * In Java version 7 this behavior was changed. Up to version 7 a substring builds an new buffer for the substring
 * in the heap. The advantage is: If a long String exists firstly, then some substrings are build, and the firstly long String
 * is not used anymore, the memory of the long String can garbaged now. The application does not need yet memory for the originally long String,
 * only the typical short substrings are stored in the heap. For applications, which builds some short substrings from a
 * long parent String, it saves memory.
 * <br><br>
 * But if substrings are need extensively from one long String, to search somewhat etc, The creation of new memory for any substring
 * may be an expensive operation. This class works with the given String, builds parts of the string with indices, 
 * and does not need memory for any sub part.
 * <br><br>
 * 
 * 
 * <b>Multithreading, persistence of Strings</b>:<br>
 * A StringPart depends of its parent CharSequence. That CharSequence maybe a String, which is persistent. But that CharSequence
 * maybe a StringBuilder or any other volatile storage. Changing the CharSequence my disturb operations of the StringPart.
 * Therefore the parent CharSequence should be notice. Is it changed? 
 * <br><br>
 * If a Part should be stored persistently, one can use a {@link #toString()} method of any returned CharSequence
 * for example {@link #getCurrentPart()}.toString(). This builds a persistent String which can be stored and used independent of all others.
 * <br><br>
 * But if the Part of String is used in the same thread, not stored, and another thread does not disturb the content of the 
 * StringPart's parent CharSequence (which may be usual), the waiver to build a persistent String may save a little bit of calculation time.
 * A method which accepts a {@link java.lang.CharSequence} as parameter should not store that in suggestion of persistence. 
 * For example {@link StringBuilder#append(CharSequence)} uses a non-persistent character sequence and adds it to its own buffer.
 * <br><br>
 * 
 * 
 * <b>Access as CharSequence</b>:<br>
 * This class is a {@link java.lang.CharSequence}. The sequence of chars is represented by the {@link #getCurrentPart()}.
 * The method {@link #length()} returns the length of the current part. The method {@link #charAt(int)}
 * returns the characters from {@link #beginLast}. The method {@link #subSequence(int, int)} builds a {@link Part}
 * which refers the sub sequence inside the {@link #content}.
 * 
 * 
 * <br><br>
 * 
 *  
 * <b>Principles of operation</b>:<br>
 * The StringPart class is associated to any CharSequence. Additionally 4 Parameters determine the actual part of the String
 * and the limits of changing of the actual part. The followed image is used to explain the spread of a part:
 * <pre>
 * abcdefghijklmnopqrstuvwxyz  Sample of the whole associated String
 * =====================     The === indicates the maximal part
 *   -----------             The --- indicates the valid part before some operation
 *         +++++             The +++ indicates the valid part after some operation
 * </pre> 
 * The actual part of the string is changeable, without building a new substring. 
 * So some operations of seeking and scanning are offered. 
 * <br><br>
 * <b>Types of Methods</b>:<br>
 * <ul>
 * <li>assign: assigns a new parent string: {@link #assign(CharSequence)}, like constructor
 * <li>seek: changes the start position of the actual (current) string part, do not change the end of the actual part,
 *   from there, seek changes the length. Seek returns this, so concatenation of method calls is possible.
 *   <ul>
 *   <li>{@link #setParttoMax()}: Sets the current part of the String to the whole String. For new search operations.
 *   <li>{@link #seekPos(int)}, {@link #seekPosBack(int)}: Sets the current part begin forward, for example seek(1) to skip over one character
 *   <li>{@link #seek(char, int)}, {@link #seek(CharSequence, int)}: Searches a character or a CharSequence, sets the begin of the current part to it.
 *   <li>{@link #seekAnyChar(CharSequence)},  {@link #seekBackToAnyChar(CharSequence)}: Searches any of some given characters.
 *   <li>{@link #seek(CharSequence, int)}, {@link #seekBackward(CharSequence)}: Searches any of some given characters.
 *   <li>{@link #seekAnyString(CharSequence[], int[])}: Searches any of some given character sequences.
 *   <li>{@link #seekNoWhitespace()}, {@link #seekNoWhitespaceOrComments()}: skip over all white spaces, maybe over comments
 *   <li>{@link #seekNoChar(CharSequence)} skip over all given characters
 *   <li>{@link #seekBegin()} Expands the spread starting from the most left position (the <i>maximal part</i>)
 *   </ul>  
 * <li>lento: changes the end of the actual string part.
 *   <ul>
 *   <li>{@link #lento(int)}: set a length of the valid part
 *   <li>{@link #lento(char)}, {@link #lento(CharSequence, int)}: length till a end character or end string
 *   <li>{@link #lentoAnyChar(CharSequence, int)}, {@link #lentoAnyString(CharSequence[], int)}: length till one of some given end characters or Strings
 *   <li>{@link #lentoAnyCharOutsideQuotion(CharSequence, int)}: regards CharSequence in quotation as non-applying.
 *   <li>#lentoAnyNonEscapedChar(CharSequence, int): regards characters after a special char as non-applying.
 *   <li>#lentoAnyStringWithIndent(CharSequence[], CharSequence, int, StringBuilder): regards indentation typically for source files.
 *   <li>#lentoIdentifier(), #lentoIdentifier(CharSequence, CharSequence): accepts identifier
 *   </ul>
 * <li>{@link #firstlineMaxpart()}, {@link #nextlineMaxpart()}: line processing. Each line can be individually evaluated or scanned.   
 * <li>get: Gets an content without changing.
 *   <ul>
 *   <li>#getCurrentPart(): The valid part as CharSequence, use toString() to transform to a persistent String.
 *   <li>#getCurrent(int): Requested number of chars from start of the current part, for tests and debugging.
 *   <li>#getLastPart(): Last valid part before the last seek or scan.
 *   </ul>
 * <li>indexOf: search any one in the valid part.
 *   <ul>
 *   <li>{@link #indexEndOfQuotation(char, char, int, int)} etc.
 *   </ul>
 * <li>See {@link StringPartScan}  for further scan functions.
 * <li>See {@link StringPartAppend}, {@link StringPartFromFileLines} for complete processing.
 * <li>See {@link StringFunctions} for basic operations.  
 * </ul>            
 */
public class StringPart implements CharSequence, Comparable<CharSequence>, Closeable, Iterable<StringPart>
{
  /**Version, history and license.
   * <ul>
   * <li>2021-06-28 Hartmut new now it is an Iterable. The Iterator {@link Iter} returns this but with the next selected line.
   *   It is to evaluate a file line per line. It uses the existing routines {@link #firstlineMaxpart()} and {@link #nextlineMaxpart()}
   *   which are proper to use. The lines can be evaluated with operations of StringPart. 
   *   This is used well in JZtxtcmd in a for(element: container) loop.
   * <li>2021-03-14 Hartmut new {@link #lentoNumber(boolean, int[], String)}
   * <li>2019-06-07 Hartmut some formally changes: {@link #mSeekToLeft} etc: all bits are named with leading 'm' for mask. 
   * <li>2019-05-15 Hartmut {@link #seekCheck(CharSequence)}: A seek("xyz") shifts the actual position to the end if the seek String is not found. 
   *   This is the programmed behavior since 20 years, but it seems to be inconsequent. It may be better to remain the position, 
   *   because found() can be checked. This routine is not changed. But the new routine {@link #seekCheck(CharSequence)}
   *   and a flag {@link #mSeekCheck} is introduced instead.
   * <li>2019-04-24 Hartmut Some gardening because this sources are copied and adapted to C-sharp too:
   *   {@link Part} is a static class now, with outer reference. It is more clearly. Csharp does not know the concept of non static inner classes
   * <li>2019-04-24 Hartmut new {@link #setCurrentMaxPart()}, obvious necessary while test (independent of Csharp!).
   * <li>2019-01-22 Hartmut There were some mistakes with the position with longer files and smaller buffer in StringPartFromFileLines.
   * <li>2019-02-10 Hartmut Change of {@link StringPart.Part}: possibility of set, because there is a persistent instance possible for {@link #setCurrentPartTo(Part)}.
   *   It is because C-usage. This routines are used in the C-version for the Simulink Sfunction DataStruct...Inspc. 
   * <li>2018-12-22 Hartmut bugfix: Bug: The position in {@link StringPart.Part} were related to the current {@link #content}. 
   *   If the content was shifted in {@link StringPartFromFileLines#readnextContentFromFile(int)} the text referred with a {@link StringPart.Part} were become faulty,
   *   it has referred the same position but the text was shifted in content. 
   *   <br>
   *   Fix: New element {@link #absPos0} contains the character position in the read file in {@link StringPartFromFileLines}, it is incremented there.
   *   The {@link StringPart.Part#absPos0} contains a final value on creation. The difference between both absPos0 is the difference to refer the correct characters.
   *   If the content is shifted out, a IndexOutOfBoundsException is thrown on access of #content, with them the situation is detected. 
   *   A {@link Part} is only intended for immediate use. Therefore a shifting of {@link #content} should be acceptable, but not a greater shifting.
   *   Comment added on Part and some usages like {@link #getLastPart()} and {@link StringPartScan#getLastScannedString()}.
   * <li>2017-07-02 Hartmut new: {@link #getCurrentPart(int)} invoked with -1 returns the whole current part.
   * <li>2016-09-04 Hartmut chg: {@link #seekPosBack(int)} instead new method seekBack, better name, there was a name clash in Java2C-translation with constant definition {@link #seekBack}.
   * <li>2016-09-04 Hartmut chg: {@link #checkCharAt(int, String)} should be written with only one return statement for Java2C as define inline.
   * <li>2016-09-04 Hartmut adapt: using of {@link Java4C.InThCxtRet}  
   * <li>2016-08-28 Hartmut new: {@link #firstlineMaxpart()}, {@link #nextlineMaxpart()} as new and important mechanism for line to line scanning. 
   * <li>2016-08-28 Hartmut chg: {@link #setParttoMax()} returns this. 
   * <li>2016-08-28 Hartmut new: {@link #checkCharAt(int, String)} as replacement or additional to {@link #charAt(int)} and comparison, without exception.  
   * <li>2016-08-28 Hartmut chg: {@link #lentoPos(int)} instead {@link #lento(int)} because it is ambiguous with {@link #lento(char)} especially for {@link org.vishia.cmd.JZtxtcmdExecuter} interpretation. 
   * <li>2016-08-28 Hartmut chg: {@link #lento(CharSequence)} instead String argument. May changes CharSequence instead String without changing the implementation. It has worked with a CharSequence already. 
   * <li>2016-08-28 Hartmut new: {@link #seekPos(int)} instead {@link #seek(int)} but it seeks backward from end with negative number. Sets {@link #found()} instead exception. 
   * <li>2016-05-22 Hartmut chg: now translated to C with some changes.
   * <li>2015-02-28 Hartmut chg: {@link #seekBackward(CharSequence)} instead seekBack because name clash in Java2C, C-translated code with {@link #seekBack}
   * <li>2015-02-28 Hartmut new: {@link #lentoLineEnd()}, {@link #seekBackward(CharSequence)}, {@link #seekBackToAnyChar(CharSequence)}
   *   more simple for calling in a JZcmd script.
   * <li>2014-09-05 Hartmut new: Twice methods {@link #indexOf(CharSequence)} and {@link #indexOf(CharSequence)}. 
   *   The methods are the same in Java. But in C the handling of reference is different. In Java2C translation a StringJc does not base on CharSequence
   *   because it is a simple reference to char[] and a length only. CharSequence needs ObjectJc and virtual methods. 
   * <li>2014-05-23 Hartmut new: {@link #getLineAndColumn(int[])} instead getLineCt() because it determines the column
   *   in one function instead extra call off {@link StringPart#getCurrentColumn()}. It is faster.   
   * <li>2014-05-22 Hartmut new: {@link #setInputfile(CharSequence)}, {@link #getInputfile()} 
   * <li>2014-05-10 Hartmut new: {@link #line()} 
   * <li>2014-01-12 Hartmut new: {@link #setParttoMax()} usefully for new view to content.
   * <li>2013-12-29 Hartmut bugfix in {@link Part#Part(int, int)}   
   * <li>2013-10-26 Hartmut chg: Does not use substring yet, some gardening, renaming. 
   * <li>2013-09-07 Hartmut new: {@link StringPartScan#getCircumScriptionToAnyChar(CharSequence)}
   *   the {@link #getCircumScriptionToAnyChar(CharSequence)} does not work correctly (it has a bug). Use the new one.
   * <li>2013-01-20 Hartmut TODO: The {@link #content} should be a CharSequence. Then the instance of content may be a StringBuilder.
   *   All content.substring should be replaced by content.subsequence(). The content.indexof-Method should be implemented here.
   *   Advantage: A derived class can use the {@link #content} as StringBuilder and it can shift the string by operating with
   *   large contents. Note that a origin position should be used then. This class can contain and regard a origin position,
   *   which is =0 in this class. See {@link StringPartFromFileLines}. That class doesn't regard a less buffer yet, but it should do so. 
   * <li>2013-01-19 Hartmut new: {@link #getPart(int, int)}
   * <li>2012-02-19 Hartmut new: {@link #assignReplaceEnv(StringBuilder)}
   * <li>2011-10-10 Hartmut new: {@link #scanFloatNumber(boolean)}. It should be possible to scan a float with clearing the buffer. Using in ZbnfParser.
   * <li>1011-07-18 Hartmut bugfix: some checks of length in {@link #scanFloatNumber()}. If the String contains only the number digits,
   *                an IndexOutOfBounds-exception was thrown because the end of the String was reached. 
   * <li>2009-03-16 Hartmut new: scanStart() returns this, not void. Useable in concatenation.
   * <li>2007-05-08 JcHartmut  change: seekAnyChar(CharSequence,int[]) renamed to {@link seekAnyString(CharSequence,int[])} because it was an erroneous identifier. 
   * <li>2007-05-08 JcHartmut  new: {@link lastIndexOfAnyChar(CharSequence,int,int)}
   * <li>2007-05-08 JcHartmut  new: {@link lentoAnyChar(CharSequence, int, int)}
   *                           it should programmed consequently for all indexOf and lento methods.
   * <li>2007-04-00 JcHartmut  some changes, not noted.
   * <li>2004-01-00 JcHartmut  initial revision The idea of such functionality was created in th 1990th in C++ language.
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
  public final static String sVersion = "2019-06-07";
  
   
  /** The actual start position of the valid part.*/
  protected int begin;
  /** The actual exclusive end position of the valid part.*/
  protected int end;

  /**The most left possible start position. We speak about the 'maximal Part':
    * The actual valid part can not exceed the borders startMin and endMax of the maximal part after any operation.
    * The content of the associated string outside the maximal part is unconsidered. The atrributes startMin and endMax
    * are not set by any operations except for the constructors and the set()-methods.
      <br/>Set to 0 if constructed from a string,
      determined by the actual start if constructed from a StringPart.
      <hr/><u>In the explanation of the methods the following notation is used as samples:</u><pre>
abcdefghijklmnopqrstuvwxyz  Sample of the whole associated String
  =====================     The === indicates the maximal part
    -----------             The --- indicates the valid part before operation
               ++++++++     The +++ indicates the valid part after operation
      </pre>
  */
  protected int begiMin;

  /** The most right possible exclusive end position. See explanation on startMin.
   * <br/>Set to content.length() if constructed from a string,
   * determined by the actual end if constructed from a StringPart.
   * It is checked by assert whether endMax <= content.length(). 
   */
  protected int endMax;

  /**The absolute position of character in the input file of content[0] It is used for {@link StringPartFromFileLines} or adequate reader.*/
  protected int absPos0 = 0;
  
  
  /**Strategy for buffer shifting: {@link #begiMin} should be existing always.
   * Any of this rewind positions should be existing.
   * But the user must not forgot to remove the rewind positions.
   * TODO
   */
  @SuppressWarnings("unused")
  private int[] posRewind = new int[20];  //some positions to rewind.
  
  

  
  /** The referenced string. It is a CharSequence for enhanced using.    */
  protected CharSequence content;

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
  protected int beginLast, endLast;




  /** True if the last operation of lento__(), seek etc. has found anything. See {@link #found()}. */
  protected boolean bFound = true;

  
  /** Flag to force setting the start position after the seeking string. See description on seek(CharSequence, int).
   */
  public static final int mSeekEnd = 1;
  public static final int seekEnd = mSeekEnd;
   
   /**If this bit is set on seek, the position remains if the seek is not successfully (if {@link #found()} returns false). */
  public static final int mSeekCheck = 2;

  /**Flag bit to force seeking backward. This value is contens impilicit in the mSeekBackFromStart or ~End,
   * using to detect internal the backward mode.
   */
  private static final int mSeekBackward_ = 0x10;

  /** Flag bit to force seeking left from start (Backward). This value is contens impilicit in the seekBackFromStart
      using to detect internal the seekBackFromStart-mode.
  */
  private static final int mSeekToLeft_ = 0x40;

  /** Flag to force seeking backward from the start position. See description on seek(CharSequence).
  */
  public static final int mSeekToLeft = mSeekToLeft_ + mSeekBackward_;
  public static final int seekToLeft = mSeekToLeft;


  /** Flag to force seeking backward from the end position. See description on seek(CharSequence).
   */
  public static final int mSeekBack = 0x20 + mSeekBackward_;
  public static final int seekBack = mSeekBack;

  /** Flag to force seeking forward. See description on seek(CharSequence).
   */
  public static final int seekNormal = 0;


  /** Some mode bits. See all static final int xxx_mode. */
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

  /**Bit in mode. Only if this bit is set, the method {@link #getCurrentColumn()} calculates the column.
   * If the bit is not set, that method returns -1 if it is called. For save calculation time.
   */
  //protected static final int mGetColumn_mode = 0x8;
   
   
  /**The file from which the StringPart was build. See {@link #getInputfile()} and setInputFile. */
  String sFile;
  
  /** The string defined the start of comment inside a text.*/
  String sCommentStart = "/*";
   
  /** The string defined the end of comment inside a text.*/
  String sCommentEnd = "*/";
   
  /** The string defined the start of comment to end of line*/
  String sCommentToEol = "//";
   
  /** Creates a new empty StringPart without an associated String. See method set() to assign a String.*/
  public StringPart()
  { this.content = null; this.begiMin = this.begin = this.beginLast= 0; this.endLast = this.endMax = this.end = 0;
  }



  /**Creates a new StringPart, with the given content from a String. Initialy the whole string is valid
   *  and determines the maximal part.
   * Constructs with a given CharSequence, especially with a given String.
   * @param src Any CharSequence or String
   */
  public StringPart(CharSequence src){
    this(src, 0, src.length());
  }
  
  
  
  /**Builds a StringPart which uses the designated part of the given src.
   * Creates a new StringPart with the same String as the given StringPart. The maximal part of the new StringPart
   * are determined from the actual valid part of the src. The actual valid part is equal to the maximal one.
   * <hr/><u>example:</u><pre>
abcdefghijklmnopqrstuvwxyz  The associated String
    ----------------        The valid part of src
    ================        The maximal part and initial the valid part of this
    +++++   ++++            Possible valid parts of this after some operations
       ++++      +++        Possible also
  +++++           ++++ +++  Never valid parts of this after operations because they exceeds the borders of maximal part.
      </pre>
   * @param src It will be referenced.
   * @param start The beginMin and begin value for the StringPart.
   * @param end The end and endMax value for the StringPart.
   */
  public StringPart(CharSequence src, int start, int end){
    this.begiMin = this.begin = start;
    this.endMax = this.end = end;
    this.content = src;
    assert( end <= this.content.length());
  }
  
  
  /**Sets the input file for information {@link #getInputfile()}
   * @param file
   */
  public void setInputfile(String file) { this.sFile = file; }

  
  /** Sets the content to the given string, forgets the old content. Initialy the whole string is valid.
  @java2c=return-this.
  @param content The content.
  @return <code>this</code> to concatenate some operations, like <code>part.set(src).seek(sKey).lento(';').len0end();</code>
  */
  @Java4C.Retinline
  @Java4C.ReturnThis 
  public final StringPart assign(CharSequence src) { 
    this.content = src;
    setParttoMax();
    return this;
  }

/**Sets the content to the given string, forgets the old content. 
 * All Place-holder for System environment variable are replaced firstly.
 * A place holder for a environment variable is written like "$(name)" or "$name" like in a unix shell.
 * The replacement is done in the content. 
 * Initially the whole string is valid.
 * TODO designate input as persistent.
 * @java2c=return-this.
 * @param input The content initially maybe with place holders for environment variable, they will be replaced.
 *   For java2c-usage the content should not be changed after them, because the String is referred there
 *   originally.
 * @return <code>this</code> refers the content.
 * @deprecated: This routine processes the input. It may better to do this outside before
 * calling {@link #assign(CharSequence)} because it is not a functionality of this class.
 */
public final StringPart assignReplaceEnv(StringBuilder input)
{ int pos1 = 0;
  int zInput = input.length();
  while( (pos1 = input.indexOf("$", pos1))>=0){
    int posident, posidentend, pos9;
    if(input.charAt(pos1+1)=='('){
      posident = pos1 + 2;
      posidentend = input.indexOf(")", posident);
      pos9 = posidentend +1;  //after )
      
    } else {
      posident = pos1 +1 ;
      posidentend = pos9 = StringFunctions.indexAfterIdentifier(input, posident, zInput, null);
    }
    String sEnv = System.getenv(input.substring(posident, posidentend));
    if(sEnv == null){ sEnv = ""; }
    input.replace(pos1, pos9, sEnv);
    zInput = input.length();
  }
  this.content =  input;
  this.begiMin = this.beginLast = this.begin = 0;
  this.endMax = this.end = this.endLast = this.content.length();
  this.bStartScan = this.bCurrentOk = true;
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
  public final StringPart assign(StringPart src)
  { if(src == this)
    { //set from the own instance: the maxPart is the actual one.
      this.begiMin = this.beginLast = this.begin; this.endMax = this.endLast = this.end;
    }
    else
    { //set from a other instance, inherit the content.
      this.content = src.content; this.begiMin = this.beginLast = this.begin = src.begin; this.endMax = this.end = this.endLast = src.end;
      assert(this.endMax <= this.content.length());
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
  public final StringPart assignFromEnd(StringPart src)
  { this.content = src.content;
    this.beginLast = this.begin;
    this.begiMin = this.begin = src.end;       //from actual end
    this.endLast = this.endMax = this.end = src.endMax;          //from maximal end
    assert(this.endMax <= this.content.length());
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
  public final boolean setIgnoreComment(boolean bSet)
  { boolean bRet = (this.bitMode & mSkipOverCommentInsideText_mode) != 0;
    if(bSet) this.bitMode |= mSkipOverCommentInsideText_mode;
    else     this.bitMode &= ~mSkipOverCommentInsideText_mode;
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
  public final boolean setIgnoreComment(String sStart, String sEnd)
  { boolean bRet = (this.bitMode & mSkipOverCommentInsideText_mode) != 0;
    this.bitMode |= mSkipOverCommentInsideText_mode;
    this.sCommentStart = sStart; 
    this.sCommentEnd   = sEnd;
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
  public final boolean setIgnoreEndlineComment(boolean bSet) 
  { boolean bRet = (this.bitMode & mSkipOverCommentToEol_mode) != 0;
    if(bSet) this.bitMode |= mSkipOverCommentToEol_mode;
    else     this.bitMode &= ~mSkipOverCommentToEol_mode;
    return bRet;
  }
  

  
  /** Set the character string introducing the comments to end of line.
   * After this call, endline-comments are always ignored on every scan operation. 
   * On scan, the current position is set first after a comment if the current position began with a comment.
   * This mode may or should be combinded with setIgnoreWhitespace.<br/> 
   * @param sStart String introducing end line comment
   * @return The last definition of this feature.
   */
  public final boolean setIgnoreEndlineComment(String sStart) 
  { boolean bRet = (this.bitMode & mSkipOverCommentToEol_mode) != 0;
    this.bitMode |= mSkipOverCommentToEol_mode;
    this.sCommentToEol = sStart;
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
  public final boolean setIgnoreWhitespaces(boolean bSet)
  { boolean bRet = (this.bitMode & mSkipOverWhitespace_mode) != 0;
    if(bSet) this.bitMode |= mSkipOverWhitespace_mode;
    else     this.bitMode &= ~mSkipOverWhitespace_mode;
    return bRet;
  }
  
  


  /** Sets the start of the maximal part to the actual start of the valid part.
   * See also seekBegin(), that is the opposite operation.
   * <hr/><u>example:</u><pre>
abcdefghijklmnopqrstuvwxyz  The associated string
================        The maximal part before operation
     ------             The actual part
     ===========        The maximal part after operation
  </pre>
   * @java2c=return-this.
   * @param src The given StringPart.
   * @return <code>this</code> to concatenate some operations, like <code>part.set(src).seek(sKey).lento(';').len0end();</code>
  */
  @Java4C.Retinline
  @Java4C.ReturnThis 
  @Java4C.NoStackTrace
  public final StringPart setBeginMaxPart()
  { this.begiMin = this.begin;
    return this;
  }



  /** Sets the range of the maximal part to the currentvalid part.
   * <hr/><u>example:</u><pre>
abcdefghijklmnopqrstuvwxyz  The associated string
      ================      The maximal part before operation
         ------             The actual part
         ======             The maximal part after operation
</pre>
   * @java2c=return-this.
   * @param src The given StringPart.
   * @return <code>this</code> to concatenate some operations, like <code>part.set(src).seek(sKey).lento(';').len0end();</code>
   */
  @Java4C.Retinline
  @Java4C.ReturnThis 
  @Java4C.NoStackTrace
  public final StringPart setCurrentMaxPart()
  { this.begiMin = this.begin;
    this.endMax = this.end;
    return this;
  }




  /**Sets the full range of available text.
   * begin is set to 0, end is set to the length() of the content.
   */
  @Java4C.Retinline
  @Java4C.ReturnThis 
  public final StringPart setParttoMax()
  { this.begiMin = this.beginLast = this.begin = 0;
    this.endMax = this.end = this.endLast = this.content == null ? 0 : this.content.length();
    this.bStartScan = this.bCurrentOk = true;
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
    @return <code>this</code> to concatenate some operations, like <code>part.set(src).seek(sKey).lento(';').len0end();</code>
  */
  @Java4C.Retinline
  @Java4C.ReturnThis 
  @Java4C.NoStackTrace
  public final StringPart fromEnd()
  {
    this.beginLast = this.begin;
    this.endLast = this.end;
    this.begin = this.end;
    this.end = this.endMax;
    return this;
  }

  
/**This method returns the characters of the current part.
 * @see java.lang.CharSequence#charAt(int)
 */
@Override
@Java4C.Retinline
public final char charAt(int index){ 
  return absCharAt(this.begin + index);
}


/**Checks whether the char at pos in the current part is one of chars.
 * @param pos
 * @param chars
 * @return
 */
@Java4C.Retinline 
public final boolean checkCharAt(int pos, String chars){
  return (this.begin + pos >=this.end) ? false
  : chars.indexOf(charAt(pos)) >=0;  //char found.
}


/**Returns a volatile CharSequence from the range inside the current part.
 * If it is not possible an IllegalArgumentException is thrown.
 * The difference to {@link #subString(int, int)} is: It is not persistent.
 * This method should only used if the CharSequence is processed in the thread immediately
 * for example by adding to another StringBuilder etc. The returned instance should not be saved
 * for later usage.
 * 
 * For C usage: The returned instance is located in the Thread Context. It should be freed with <code>releaseUserBuffer_ThreadContextFw(...)<(code>.
 * The Java2C-translator does that automatically.
 *  
 * @see java.lang.CharSequence#subSequence(int, int)
 */
@Java4C.ReturnInThreadCxt
@Override public final CharSequence subSequence(int from, int to) { 
  if(from < 0 || to > (this.end - this.begin)) {
    throwSubSeqFaulty(from, to);
    return null;  //It is used for Java2C without throw mechanism.
  }
  @Java4C.InThCxtRet(sign="StringPart.subSequence") 
  Part ret = new Part(this, this.begin + from, this.begin + to);
  return ret;
} 



private static final void throwSubSeqFaulty(int from, int to)
{

  throw new IllegalArgumentException("StringPartBase.subString - faulty;" + from + ":" + to);
}

  
  /* (non-Javadoc)
   * @see java.lang.CharSequence#length()
   */
  @Override public final int 
  length(){ return this.end - this.begin; }

  /* (non-Javadoc)
   * @see java.lang.CharSequence#length()
   */
  public final int getLenCurrent(){ return this.end - this.begin; }

  /**Returns the lenght of the maximal part from current position. Returns also 0 if no string is valid.
     @return number of chars from current position to end of maximal part.
   */
  @Java4C.Retinline
  @Java4C.NoStackTrace
  public final int lengthMaxPart() { 
    if(this.endMax > this.begin) return this.endMax - this.begin;
    else return 0;
  }

  
  /** Sets the endposition of the part of string to the given chars after start.
    @java2c=return-this.
    @param len The new length. It must be positive.
    @return <code>this</code> to concat some operations.
    @throws IndexOutOfBoundsException if the len is negativ or greater than the position endMax.
    @deprecated use lenToPos, more clarify, especially for JZcmd
   */
  @Java4C.Inline
  @Java4C.ReturnThis
  @Deprecated
  public final StringPart lento(int len){ return lentoPos(len); }
  
  
  /** Sets the endposition of the part of string to the given chars after start.
    @java2c=return-this.
    @param len The new length. It must be positive.
    @return <code>this</code> to concatenate some operations.
    @throws IndexOutOfBoundsException if the len is negativ or greater than the position endMax.
   */
  public final StringPart lentoPos(int len)
  throws IndexOutOfBoundsException
  { this.endLast = this.end;
    int endNew = this.begin + len;
    if(endNew < this.begin)  /**@java2c=StringBuilderInThreadCxt.*/ throwIndexOutOfBoundsException("lento(int) negative:" + (endNew - this.begin));
    if(endNew > this.endMax) /**@java2c=StringBuilderInThreadCxt.*/ throwIndexOutOfBoundsException("lento(int) after endMax:" + (endNew - this.endMax));
    this.end = endNew;
    return this;
  }





  
  /** Sets the end position of the part of string to exclusively the char cc.
  If the char cc is not found, the end position is set to start position, so the part of string is empty.
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
   * @return <code>this</code> to concatenate some operations, usage example <code>part.set(src).seek(sKey).lento(';').len0end();</code>
   *   Hint: use {@link #found()} to detect whether the end string is found.
   *   if the end String is not found, the current part has length ==0
   */
  public final StringPart lento(char cc)
  { this.endLast = this.end;
    this.end = this.begin - 1;
    while(++this.end < this.endLast){
      if(this.content.charAt(this.end) == cc) { this.bFound = true; return this; }
    }
    this.end = this.begin;  //not found
    this.bFound = false;
    return this;
  }

  
  
  /** Sets the endposition of the part of string to exclusively the given string.
      If the string is not found, the end position is set to start position, so the part of string is emtpy.
      It is possible to call set0end() to set the end of the part to the maximal end if the char is not found.
      That is useful by selecting a part to a separating char such ',' but the separator is not also the terminating char
      of the last part, example see lento(char cc)
      @java2c=return-this.
      @param ss string to determine the exclusively end char.
   * @return <code>this</code> to concatenate some operations, usage example <code>part.set(src).seek(sKey).lento(';').len0end();</code>
   *   Hint: use {@link #found()} to detect whether the end string is found.
   *   if the end String is not found, the current part has length ==0
   */
  @Java4C.Retinline
  @Java4C.ReturnThis 
  public final 
  StringPart lento(CharSequence ss) { 
    return lento(ss, seekNormal);
  }



  /** Sets the endposition of the part of string to exclusively the given string.
    If the string is not found, the end position is set to start position, so the part of string is emtpy.
    It is possible to call set0end() to set the end of the part to the maximal end if the char is not found.
    That is useful by selecting a part to a separating char such ',' but the separator is not also the terminating char
    of the last part, example see lento(char cc)
    @java2c=return-this.
    @param ss string to determine the exclusively end char.
    @param mode Mode of seeking the end, seekEnd or 0 is possible.
   * @return <code>this</code> to concatenate some operations, usage example <code>part.set(src).seek(sKey).lento(';').len0end();</code>
   *   Hint: use {@link #found()} to detect whether the end string is found.
   *   if the end String is not found, the current part has length ==0
   */
  public final StringPart lento(CharSequence ss, int mode)
  { this.endLast = this.end;
    int pos = StringFunctions.indexOf(this.content, this.begin, this.end, ss);
    this.bFound = (pos >=0);
    if(pos >= 0) { 
      this.end = pos; 
      if((mode & mSeekEnd) != 0){ this.end += ss.length();}
    }
    else { this.end = this.begin; }
    return this;
  }




  /**Sets the endposition of the part of string to the end of the identifier which is beginning on start.
   * If the part starts not with a identifier char, the end is set to the start position.
   * <hr/><u>example:</u><pre>
    abcd  this is a part uvwxyz The associated String
    =====================     The border of valid parts of src
       -------              The valid part of the src before calling the method
       +++                  after calling lentoIdentifier(). The start position
                            is not effected. That's why the identifier-part is only "his".
      </pre>
      @java2c=return-this.
      @return <code>this</code> to concat some operations, like <code>part.set(src).seek(sKey).lento(';').len0end();</code>
    */
  @Java4C.Retinline
  @Java4C.ReturnThis 
  public final StringPart lentoIdentifier()
  {
    return lentoIdentifier(null, null);
  }


  /** Sets the endposition of the part of string to the end of the identifier which is beginning on start.
   *  If the part starts not with a identifier char, the end is set to the start position.
   *  @see lentoIdentifier().
   *  @java2c=return-this.
   *  @param additionalChars CharSequence of additinal chars there are also accept
   *         as identifier chars. 
   */
  public final StringPart lentoIdentifier(CharSequence additionalStartChars, CharSequence additionalChars)
  { this.endLast = this.end;
    this.end = this.begin;
    if(this.end >= this.endMax){ this.bFound = false; }
    else
      
    { //TODO use StringFunctions.lenIdentifier
      char cc = this.content.charAt(this.end);
      if(   cc == '_' 
        || (cc >= 'A' && cc <='Z') 
        || (cc >= 'a' && cc <='z') 
        || (additionalStartChars != null && StringFunctions.indexOf(additionalStartChars,cc)>=0)
        )
      { this.end +=1;
        while(  this.end < this.endMax 
             && (  (cc = this.content.charAt(this.end)) == '_' 
                || (cc >= '0' && cc <='9') 
                || (cc >= 'A' && cc <='Z') 
                || (cc >= 'a' && cc <='z') 
                || (additionalChars != null && StringFunctions.indexOf(additionalChars,cc)>=0)
             )  )
        { this.end +=1; }
      }  
      this.bFound = (this.end > this.begin);
    }
    return this;
  }


  /** Sets the end position of the part of string to the end of the number which is beginning on start.
   *  If the part starts not with an admissible char, the end is set to the start position 
   *  and this.bFound is set to false.
   *  If a number is found, maybe consist only as separatorChars, this.bFound is set to true.
   *  It may be recommended using this routine if a number character is really expected.
   *  <br>
   *  Example: <code> " 1'234'456.235"</code> is parsed correctly as 1234456 
   *  with separatorChars=" '". The dot is not parsed. 
   *  <br>
   *  Example: to parse an expected number with sign, test firstly the sign (before number).
   *  <pre>
   *  int[] nr = int[1];
   *  char cc = sp.{@link #getCurrentChar()};
   *  if(cc == '-') { bNeg = true; }
   *  sp.lentoNumber(false, nr, null);
   *  if(bNeg) { nr[0] = -nr[0]; }
   *  sp.{@link #fromEnd()} 
   *  </pre> 
   *  @java2c=return-this.
   *  @param bHex also parse a..f and A..F, build a hexa number
   *  @param dst either null or int[1] as destination for the parsed number. 
   *    Note: The length of the number for fractional part can be detected with separtorChars=null 
   *    and {@link #getLenCurrent()} 
   *  @param separatorChars CharSequence of additional chars there are also accept inside a number
   *        
   */
  public final StringPart lentoNumber(boolean bHex, int[] dst, String separatorChars)
  { this.endLast = this.end;
    this.end = this.begin;
    int nr = 0;
    int mult = bHex? 16 : 10;
    while(this.end < this.endMax){ 
      char cc = this.content.charAt(this.end);
      if(separatorChars !=null && separatorChars.indexOf(cc) >=0) {
        this.end +=1;                            //admissible additional character
      }
      else if(cc >='0' && cc <='9') {
        nr = nr * mult + (cc - '0');             // add 0..9
        this.end +=1;
      }
      else if(bHex) {
        if( cc >='a' && cc <='f') {
          cc -= 'a';
        }
        else if( cc >='A' && cc <='F') {
          cc -= 'A';
        }
        else {
          break;                                 // non number character, break the loop
        }
        nr = nr * mult + (cc + 10);              //add a..f or A..F
        this.end +=1;
      }
      else { 
        break;                                   // non number character, break the loop
      }
    }
    this.bFound = (this.end > this.begin);
    if(dst !=null) { 
      dst[0] = nr;                               //store parsed number, dst should be initialized
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
  public final StringPart lentoAnyNonEscapedChar(CharSequence sCharsEnd, int maxToTest)
  { if(this.bCurrentOk)
    { final char cEscape = '\\';
      this.endLast = this.end;
      int pos = indexOfAnyChar(sCharsEnd, 0, maxToTest);
      while(pos > this.begin +1 && this.content.charAt(pos-1)==cEscape)
      { //the escape char is before immediately. It means, the end char is not matched.
        pos = indexOfAnyChar(sCharsEnd, pos+1 - this.begin, maxToTest);
      }
      if(pos < 0){ this.end = this.begin; this.bFound = false; }
      else       { this.end = this.begin + pos; this.bFound = true; }
    }  
    return this;
  }

  
  
  
  /**Sets the length of the valid part to the first position of the given String, 
   * but not if the String is escaped.
   * 'Escaped' means, a \ is disposed before the char.
   * Example: lentoNonEscapedString("<?") does not accept "\\<?".
   * <br><br>
   * This method doesn't any things, if the last scanning call isn't match. Invoking of 
   * {@link scanOk()} before guarantees that the method works.
   * @java2c=return-this.
   * @param sCharsEnd Assembling of chars determine the end of the part.  
   */
  public final StringPart lentoNonEscapedString(CharSequence sEnd, int maxToTest)
  { if(this.bCurrentOk)
    { final char cEscape = '\\';
      this.endLast = this.end;
      int pos = indexOf(sEnd,0,maxToTest);
      while(pos > this.begin+1 && this.content.charAt(pos-1)==cEscape)
      { //the escape char is before immediately. It means, the end char is not matched.
        pos = indexOf(sEnd, pos+1 - this.begin, maxToTest);
      }
      if(pos < 0){ this.end = this.begin; this.bFound = false; }
      else       { this.end = this.begin + pos; this.bFound = true; }
    }  
    return this;
  }

  
  
  
  /**Sets the current Part from the current position to exactly one line.
   * The start position of the current part will be set backward to the start of the line or to the start of the maximal part.
   * The end position of the current part will be set to the end of the one line or the end of the maximal part
   *   independent from the end of the current part before.
   * The functionality of #found() is not influenced. It may be the return value from a seek before.   
   * @return this
   */
  public final StringPart line(){
    int posStart = StringFunctions.lastIndexOfAnyChar(this.content, this.begiMin, this.begin, "\r\n");
    if(posStart < 0){ posStart = this.begiMin; }
    int posEnd = StringFunctions.indexOfAnyChar(this.content, this.begin, this.endMax, "\r\n");
    if(posEnd <0){ posEnd = this.endMax; }
    this.begin = posStart;
    this.end = posEnd;
    return this;
  }
  


  /**Sets the current and the maximal part from position 0 to the first end line character.
   * If a line end character was not found - the last line without line end - the end is set to the last end.
   * The line end character is either \r or \n.
   * Because the maximal part is set to the line, anything inside the line can be selected as current part.
   * The {@link #nextlineMaxpart()} works properly nevertheless. 
   * @return this.
   */
  @Java4C.ReturnThis public final StringPart firstlineMaxpart(){
    this.begiMin = this.begin = 0;
    this.endMax = this.end = this.content.length();
    lentoAnyChar("\r\n");
    if(!found()){ len0end(); }  //last line without end-line character
    this.endMax = this.end;
    return this;
  }


  
  
  /**Sets the current and the maximal part from the current end to the next line end character.
   * <ul>
   * <li>If the current end before refers a line end character itself, it is seek after it firstly. That is the standard behavior to read lines.
   * <li>If the current end before does not refer a line end character, the next line end character is searched firstly.
   *   That behavior is important if the current part of the last line was set anywhere inside the line.
   * </ul>
   * If a line end character was not found - the last line without line end - the end is set to the last end.
   * The line end character is either \r or \n or a sequence of \r\n or \n\r 
   * @return this. Use {@link #found()} to check whether a next line was found.
   */
  @Java4C.ReturnThis public final StringPart nextlineMaxpart(){
    this.begiMin = this.begin = this.endMax;
    //char test111 = charAt(0);
    this.endMax = this.end = this.content.length();
    if(this.begiMin == this.endMax) {
      this.bFound = false;
    } else {
      if(checkCharAt(0, "\n")) { seekPos(1); if(found() && checkCharAt(0, "\r")) { seekPos(1); }}
      if(checkCharAt(0, "\r")) { seekPos(1); if(found() && checkCharAt(0, "\n")) { seekPos(1); }}
      //refers next line.
      lentoAnyChar("\r\n");
      if(!found() && this.begin < this.endMax) { 
        len0end(); this.bFound = true;   //last line without end-line character
      }
      this.begiMin = this.begin;
      this.endMax = this.end;
    }
    return this;  //found() is false on end.
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
  @deprecated use {@link #seekPos(int)} 
   */
  @Deprecated
  public final StringPart seek(int nr)
  { this.beginLast = this.begin;
    this.begin += nr;
    if(this.begin > this.end)
      /**@java2c=StringBuilderInThreadCxt.*/ 
      throwIndexOutOfBoundsException("seek=" + nr + " begin=" + (this.begin - nr) + " end=" + this.end);
    else if(this.begin < this.begiMin) 
      /**@java2c=StringBuilderInThreadCxt.*/
      throwIndexOutOfBoundsException("seek=" + nr + " begin=" + (this.begin - nr) + " begin-min=" + this.begiMin);
    this.bFound = true;
    return this;
  }



  /**Sets the begin of the current part relative to the given number of character. 
   * If the range is outside, this routine sets {@link #found()} to false and does not change the position.
   * If the range is valid, {@link #found()} returns true.
   * <br>Example: seek(3):<pre>
   * abcdefghijklmnopqrstuvwxyz  The associated String
   *       ----------            The valid part before
   *          +++++++             The valid part after
   * </pre>
   * <br>Example: seek(-3):<pre>
   * abcdefghijklmnopqrstuvwxyz  The associated String
   *       ----------            The valid part before
   *    +++++++++++++            The valid part after
   * </pre>
   * @param nr >0 then shift the current part's begin to right maximal to end
   *   nr < 0 then shift the current part's begin to left maximal to {@link #begiMin}
   * @return this
   * @see #seek(int), in opposite this method does not throw an excetion but do nothing and sets {@link #found()} to false.
   */
  public final StringPart seekPos(int nr)
  { 
    int begin1 = this.begin + nr;
    if(begin1 > this.end || begin1 < this.begiMin) {
      this.bFound = false;
    } else { 
      this.begin = begin1;
      this.bFound = true;
    }
    return this;
  }



  /**Sets the begin of the current part backward from end. 
   * If the range is outside, this routine sets {@link #found()} to false and does not change the position.
   * If the range is valid, {@link #found()} returns true.
   * <br>Example: seekBack(5):<pre>
   * abcdefghijklmnopqrstuvwxyz  The associated String
   *    ----------               The valid part before
   *         +++++               The valid part after
   * </pre>
   * @param nr >=0 the number of character from end for the new begin.
   * @return this
   */
  public final StringPart seekPosBack(int nr)
  {
    int begin1 = this.end -nr;
    if(begin1 > this.end || begin1 < this.begiMin) {
      this.bFound = false;
    } else { 
      this.begin = begin1;
      this.bFound = true;
    }
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
   * @return <code>this</code> to concatenate some operations, usage example <code>part.set(src).seek(sKey).lento(';').len0end();</code>
   *   Hint: use {@link #found()} to detect whether the end string is found.
   *   if the end String is not found, the current part has length ==0
   */
public final StringPart seekNoWhitespace()
{ this.beginLast = this.begin;
  while( this.begin < this.end && " \t\r\n\f".indexOf(this.content.charAt(this.begin)) >=0 )
  { this.begin +=1;
  }
  this.bFound = (this.begin > this.beginLast);
  return this;
}


/*=================================================================================================================*/
/*=================================================================================================================*/
/*=================================================================================================================*/
/** skip over comment and whitespaces
*/

/**See {@link #setIgnoreWhitespaces(boolean)}, {@link #setIgnoreComment(boolean)}
 * @deprecated see {@link seekNoWhitespaceOrComments()}
*  @java2c=return-this.
* 
*/ 
@Deprecated
protected final StringPart skipWhitespaceAndComment()
{ return seekNoWhitespaceOrComments();
}


/** Displaces the begin of the part to the first char it is no whitespace or comment.
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
public final StringPart seekNoWhitespaceOrComments()
{ int start00 = this.begin;
int start0;
do
{ start0 = this.begin;
  if( (this.bitMode & mSkipOverWhitespace_mode) != 0)
  { seekNoWhitespace();
  }
  if( (this.bitMode & mSkipOverCommentInsideText_mode) != 0)   
  { if(StringFunctions.compare(this.content, this.begin, this.sCommentStart, 0, this.sCommentStart.length())==0) 
    { seek(this.sCommentEnd, mSeekEnd);  
    }
  }
  if( (this.bitMode & mSkipOverCommentToEol_mode) != 0)   
  { if(StringFunctions.compare(this.content, this.begin, this.sCommentToEol, 0, this.sCommentToEol.length())==0)
    { seek('\n', mSeekEnd);  
    }
  }
} while(this.begin != start0);  //:TRICKY: if something is done, repeat all conditions.
this.bFound = (this.begin > start00);
return this;
}

/** Returns true, if the last called seek__(), lento__() or skipWhitespaceAndComment()
* operation founds the requested condition. This methods posits the current Part in a appropriate manner
* if the seek or lento-conditions were not prosperous. In this kinds this method returns false.
* @return true if the last seek__(), lento__() or skipWhitespaceAndComment()
* operation matches the condition.
*/
public final boolean found()
{ return this.bFound;
}



/**Displaces the begin of the part to the leftest possible begin.
 * <br>example:<pre>
 * abcdefghijklmnopqrstuvwxyz  The associated String
 *      =================         The maximal part
 *             -----              The valid part before
 *      ++++++++++++              The valid part after calling seekBegin().
 * </pre>
 * @java2c=return-this.
 * @return <code>this</code> to concat some operations, like <code>part.set(src).seek(sKey).lento(';').len0end();</code>
 */
public final StringPart seekBegin()
{ this.begin = this.beginLast = this.begiMin;
return this;
}










  /**Common seek operation with several modes in flags.  
   * The end of the part is not affected.
   * If the string is not found, depending on mSeekCheck the begin is unchanged 
   * or it is positioned on the actual end (mSeekCheck not set). Then the length()-method supplies 0.
   * Methods such fromEnd() are not interacted from the result of the searching.
   * The rule is: seek()-methods only shifts the begin position.
   *
    <hr/><u>example:</u><pre>
that is a liststring and his part The associated String
=============================   The maximal part
  ----------------------      The valid part before
       +++++++++++++++++      The valid part after seek("is",StringPartBase.seekNormal).
         +++++++++++++++      The valid part after seek("is",StringPartBase.seekEnd).
                      ++      The valid part after seek("is",StringPartBase.back).
                       .      The valid part after seek("is",StringPartBase.back + StringPartBase.seekEnd).
 +++++++++++++++++++++++      The valid part after seek("is",StringPartBase.seekToLeft).
   +++++++++++++++++++++      The valid part after seek("is",StringPartBase.seekToLeft + StringPartBase.seekEnd).
++++++++++++++++++++++++++      The valid part after seek("xx",StringPartBase.seekToLeft).
                       .      The valid part after seek("xx",StringPartBase.seekNormal)
                              or seek("xx",StringPartBase.back).

  </pre>
   * @java2c=return-this.
   * @param sSeek The string to search for.
   * @param mode Mode of seeking, use ones of {@link #mSeekCheck}, {@link #seekBack}, {@link #seekToLeft}, {@link #seekNormal}, added with {@link #seekEnd}.
   * @return <code>this</code> to concatenate some operations, usage example <code>part.set(src).seek(sKey).lento(';').len0end();</code>
   *   Hint: use {@link #found()} to detect whether the end string is found.
   *   if the end String is not found, the current part has length ==0
   */
  public final StringPart seek(CharSequence sSeek, int mode){ 
    this.beginLast = this.begin;
    //if(StringFunctions.startsWith(sSeek, "timestamp:"))
      Debugutil.stop();
    int seekArea1, seekArea9;
    //String sSeekArea;
    int posNotFound;  //position if not found in dependence of area of seek and direction
    if( (mode & mSeekToLeft_) == mSeekToLeft_) { 
      int posAreaEnd = this.begin + sSeek.length() -1;  //the sSeek-string may be begin at (begin-1)
      if(posAreaEnd > this.endMax) posAreaEnd = this.endMax;  //but not over the end.
      seekArea1 = this.begiMin;
      seekArea9 = posAreaEnd;
      //sSeekArea = content.substring(startMin, posAreaEnd );
      posNotFound = (mode & mSeekCheck) !=0 ? this.end : this.begin; //if not found, the rightest position of area
    }
    else { 
      seekArea1 = this.begin;
      seekArea9 = this.end;
      //sSeekArea = content.substring(begin, end );
      posNotFound = (mode & mSeekCheck) !=0 ? this.begin : this.end; //if not found, the rightest position of area
    }
    
    int pos;
    if( (mode & mSeekBackward_) == mSeekBackward_) { 
      pos = StringFunctions.lastIndexOf(this.content, seekArea1, seekArea9, sSeek); //sSeekArea.lastIndexOf(sSeek);
    }
    else { 
      pos = StringFunctions.indexOf(this.content, seekArea1, seekArea9, sSeek);
    }
    
    if(pos < 0) { 
      this.begin = posNotFound;
      this.bFound = false;   
    } 
    else { 
      this.bFound = true;
      this.begin = pos;
      if( (mode & mSeekEnd) == mSeekEnd ) { 
        this.begin += sSeek.length();
      }
    }
    
    return this;
  }


  
  /**Seeks back form the current end to the end of the given String starting from the end of the current part.
   * If the String was found, the start of the current part is changed to the end of the found String.
   * Sets {@link #found()} to false if the String is not contained in the current part.
   * Then the current part is not changed.
   * @java2c=return-this.
   * @param sSeek The string to seek backward.
   * @return
   */
  public final StringPart seekBackward(CharSequence sSeek){
    int pos = StringFunctions.lastIndexOf(this.content, this.begin, this.end, sSeek);
    if(pos <0) this.bFound = false;
    else {
      this.begin = pos + sSeek.length();
    }
    return this;
  }
  
  
  
  
  /**Seeks to one of the characters contained in chars, starting from the begin of the current part.
   * If a character was found, the start of the current part is changed to that character.
   * Sets {@link #found()} to false if a character of chars is not contained in the current part.
   * Then the current part is not changed.
   * @param sSeek The string to seek backward.
   * @return <code>this</code> to concatenate some operations, usage example <code>part.set(src).seek(sKey).lento(';').len0end();</code>
   *   Hint: use {@link #found()} to detect whether the end string is found.
   *   if the end String is not found, the current part has length ==0
   */
  public final StringPart seekAnyChar(CharSequence chars ){
    int pos = StringFunctions.indexOfAnyChar(this.content, this.begin, this.end, chars);
    if(pos <0) this.bFound = false;
    else {
      this.begin = pos;
    }
    return this;
  }
  
  
  
  /**Seeks back from the current end to one of the characters contained in chars, starting from the end of the current part.
   * If a character was found, the start of the current part is changed to that character.
   * Sets {@link #found()} to false if a character of chars is not contained in the current part.
   * Then the current part is not changed.
   * @param sSeek The string to seek backward.
   * @return <code>this</code> to concatenate some operations, usage example <code>part.set(src).seek(sKey).lento(';').len0end();</code>
   *   Hint: use {@link #found()} to detect whether the end string is found.
   *   if the end String is not found, the current part has length ==0
   */
  public final StringPart seekBackToAnyChar(CharSequence chars ){
    int pos = StringFunctions.lastIndexOfAnyChar(this.content, this.begin, this.end, chars);
    if(pos <0) this.bFound = false;
    else {
      this.begin = pos;
    }
    return this;
  }
  
  
  
  
  /**Searchs the given String inside the valid part, posits the begin of the part to the begin of the searched string.
   * The end of the part is not affected.
   * If the string is not found, the begin is posit on the actual end. The length()-method supplies 0.
   * Methods such fromEnd() are not interacted from the result of the searching.
   * The rule is: seek()-methods only shifts the begin position.
   *
    <hr/><u>example:</u><pre>
that is a liststring and his part The associated String
=============================   The maximal part
  ----------------------      The valid part before
       +++++++++++++++++      The valid part after seek("is",StringPartBase.seekNormal).
         +++++++++++++++      The valid part after seek("is",StringPartBase.seekEnd).
                      ++      The valid part after seek("is",StringPartBase.back).
                       .      The valid part after seek("is",StringPartBase.back + StringPartBase.seekEnd).
 +++++++++++++++++++++++      The valid part after seek("is",StringPartBase.seekToLeft).
   +++++++++++++++++++++      The valid part after seek("is",StringPartBase.seekToLeft + StringPartBase.seekEnd).
++++++++++++++++++++++++++      The valid part after seek("xx",StringPartBase.seekToLeft).
                       .      The valid part after seek("xx",StringPartBase.seekNormal)
                              or seek("xx",StringPartBase.back).

  </pre>
   * This operation calls {@link #seek(CharSequence, int)} with flag {@link #seekNormal}.
   * Use {@link #seekCheck(CharSequence)} or {@link #seek(CharSequence, int)} with {@link #mSeekCheck}.
   * @param sSeek
   * @return <code>this</code> to concatenate some operations, usage example <code>part.set(src).seek(sKey).lento(';').len0end();</code>
   *   Hint: use {@link #found()} to detect whether the end string is found.
   *   if the end String is not found, the current part has length ==0
   */
  @Java4C.Inline
  public final StringPart seek(CharSequence sSeek){ return seek(sSeek, seekNormal); }
  
  
  /**Seeks to the given CharSequence, result is left side of the string.
   * If the string is not found, the current part remains (in opposite to {@link #seek(CharSequence)}
   * which sets the current position to the end)
   * @param sSeek
   * @return
   */
  @Java4C.Inline
  public final StringPart seekCheck(CharSequence sSeek){ return seek(sSeek, seekNormal + mSeekCheck); }
  
  
  /**Seeks to the given CharSequence, start position is after the string.
   * If not found, seek to end.
   * Use {@link #found()} to check whether it is found.
   * @param sSeek
   * @return this
   */
  @Java4C.Exclude  //name class with const seekEnd
  public final StringPart seekEnd(CharSequence sSeek){ return seek(sSeek, mSeekEnd); }
  
  
  /**Seeks to the given CharSequence, start position is after the string.
   * If not found, remain the begin position..
   * Use {@link #found()} to check whether it is found.
   * @param sSeek
   * @return this
   */
  @Java4C.Exclude  //name class with const seekEnd
  public final StringPart seekCheckEnd(CharSequence sSeek){ return seek(sSeek, mSeekEnd + mSeekCheck); }
  
  

/** Searchs the given CharSequence inside the valid part, posits the begin of the part to the begin of the searched string.
*  The end of the part is not affected.<br>
*  If the string is not found, the begin is posit to the actual end. The length()-method supplies 0.
  Methods such fromEnd() are not interacted from the result of the searching.
  The rule is: seek()-methods only shifts the begin position.<br>
  see {@link seek(CharSequence sSeek, int mode)}
* @java2c=return-this.
 @param strings List of CharSequence contains the strings to search.
* @param nrofFoundString If given, [0] is set with the number of the found CharSequence in listStrings, 
*                        count from 0. This array reference may be null, then unused.
* @return this.       
*/  
public final StringPart seekAnyString(CharSequence[] strings, @Java4C.SimpleVariableRef int[] nrofFoundString)
//public StringPartBase seekAnyString(List<CharSequence> strings, int[] nrofFoundString)
{ this.beginLast = this.begin;
int pos;
pos = indexOfAnyString(strings, 0, Integer.MAX_VALUE, nrofFoundString, null);
if(pos < 0)
{ this.bFound = false;   
  this.begin = this.end;
}
else
{ this.bFound = true;
  this.begin = this.begin + pos;
}
return this;
}








  /** Searchs the given character inside the valid part, posits the begin of the part to the begin of the searched char.
    The end of the part is not affected.
    If the string is not found, the begin is posit on the actual end
    or, if mode contents seekBack, the begin of the maximal part. 
    In this cases isFound() returns false and a call of restoreLastPart() restores the old parts.
    The length()-method supplies 0.
    Methods such fromEnd() are not interacted from the result of the searching.
    The rule is: seek()-methods only shifts the begin position.<br/>
    The examples are adequate to seek(CharSequence, int mode);
  
  *  @java2c=return-this.
    @param cSeek The character to search for.
    @param mode Mode of seeking, use ones of back, seekToLeft, seekNormal, added with seekEnd.
   * @return <code>this</code> to concatenate some operations, usage example <code>part.set(src).seek(sKey).lento(';').len0end();</code>
   *   Hint: use {@link #found()} to detect whether the end string is found.
   *   if the end String is not found, the current part has length ==0
   */
  public final StringPart seek(char cSeek, int mode)
  { this.beginLast = this.begin;
    int seekArea1, seekArea9;
    //String sSeekArea;
    int posNotFound;  //position if not found in dependence of area of seek and direction
    if( (mode & mSeekToLeft_) == mSeekToLeft_)
    { int posAreaEnd = this.begin;  //the sSeek-string may be begin at (begin-1)
      if(posAreaEnd > this.endMax) posAreaEnd = this.endMax;  //but not over the end.
      seekArea1 = this.begiMin;
      seekArea9 = posAreaEnd;
      //sSeekArea = content.substring(startMin, posAreaEnd );
      posNotFound = this.begin; //if not found, the rightest position of area
    }
    else
    { seekArea1 = this.begin;
      seekArea9 = this.end;
      //sSeekArea = content.substring(begin, end );
      posNotFound = this.end; //if not found, the rightest position of area
    }
    int pos;
    if( (mode & mSeekBackward_) == mSeekBackward_){
      pos = StringFunctions.lastIndexOf(this.content, seekArea1, seekArea9, cSeek); 
    }
    else {                                         
      pos = StringFunctions.indexOf(this.content, seekArea1, seekArea9, cSeek);
    }
    
    if(pos < 0)
    { this.begin = posNotFound;
      this.bFound = false;   
    }
    else
    { this.bFound = true;
      this.begin = pos;
      if( (mode & mSeekEnd) == mSeekEnd )
      { this.begin += 1;
      }
    }
    
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
  @param sChars CharSequence with the chars to overread.
   * @return <code>this</code> to concatenate some operations, usage example <code>part.set(src).seek(sKey).lento(';').len0end();</code>
   *   Hint: use {@link #found()} to detect whether the end string is found.
   *   if the end String is not found, the current part has length ==0
   */
  public final StringPart seekNoChar(CharSequence sChars)
  { this.beginLast = this.begin;
    while(this.begin < this.end && StringFunctions.indexOf(sChars, this.content.charAt(this.begin)) >=0) this.begin +=1;
    if(this.begin < this.end) this.bFound = true;
    else this.bFound = false;
    return this;
  }




  /**Seeks to the next non-empty line.
   * @return this
   */
  public final StringPart seekNextLine(){
    this.beginLast = this.begin;
    while(this.begin < this.end && "\n\r".indexOf(this.content.charAt(this.begin)) <0) { this.begin +=1; }  //search the first \r or \n
    while(this.begin < this.end && "\n\r".indexOf(this.content.charAt(this.begin)) >=0) { this.begin +=1; } //skip over all \r\n one after another
    if(this.begin < this.end){
      this.bFound = true;
    }
    else this.bFound = false;
    return this;
  }
 
  
/**Searches any char contained in sChars in the current part
 * Example: The given string in the current part is
 * <pre>abc end:zxy</pre>
 * The calling is
 * <pre>indexOfAnyChar("xyz", 0, 20);</pre>
 * The result is 8 because the character 'z' is found first as the end char.
 * 
 * @param fromWhere Offset after begin to begin search. It may be 0 often.
 * @param sChars Some chars to search in sq
 *   If sChars contains a EOT character (code 03, {@link #cEndOfText}) then the search stops at this character 
 *   or it is continued to the end of the range in sq. Then the length of the text range is returned
 *   if another character in sChars is not found. 
 *   It means: The end of the text range is adequate to an EOT-character. Note that EOT is not unicode,
 *   but it is an ASCII control character.  
 * @param maxToTest number of character to test from fromWhere. 
 *   If maxToTest is greater then the length of the current part, only the whole current part is tested.
 *   Especially Integer.MAXVALUE and beu used. 
 * @return -1 if no character from sChars was found in the current part. 
 *   0.. Position of the found character inside the current part, but >= fromWhere
 */
public final int indexOfAnyChar(CharSequence sChars, final int fromWhere, final int maxToTest)
{
  int pos = this.begin + fromWhere;
  int max = (this.end - pos) < maxToTest ? this.end : pos + maxToTest;
  int found = StringFunctions.indexOfAnyChar(this.content, pos, max, sChars); 
  if(found <0) return found;
  else return found - this.begin;  //
}
  



/**Returns the position of one of the chars in sChars within the part, started inside the part with fromIndex,
  returns -1 if the char is not found in the part started from 'fromIndex'.
  It may regard transcription characters and it regard quotation. 
  A transcription character is a pair of characters 
  with the transcriptionChar, usual '\' followed by any special char. This pair of characters
  are not regarded while search the end of the text part, and the transcription
  will be resolved in the result (dst) String.
  <br>
  The end of the string is determined by any of the given chars.
  But a char directly after the transcription char is not detected as an end char.
  Example: <pre>scanTranscriptionToAnyChar(dst, ">?", '\\', '\"', '\"')</pre> 
  does not end at a char > after an \ and does not end inside the quotation.
  If the following string is given: 
  <pre>a text -\>arrow, "quotation>" till > ...following</pre> 
  then the last '>' is detected as the end character. The first one is a transcription,
  the second one is inside a quotation.
  <br><br>
  The meaning of the transcription characters is defined in the routine
  {@link StringFunctions#convertTranscription(CharSequence, char)}: 
  Every char after the transcriptChar is accepted. But the known transcription chars
  \n, \r, \t, \f, \b are converted to their control-char- equivalence.
  The \s and \e mean begin and end of text, coded with ASCII-STX and ETX = 0x2 and 0x3.</br></br>
  The actual part is tested for this, after this operation the actual part begins
  after the gotten chars!
  
 @param sChars contains some chars to find. If it contains the char with code {@link #cEndOfText}
   then the number of chars till the end of this text are returned if no char was found.
   If a char with code of {@link #cEndOfText} is found in this string, it is the end of this search process too.
 @param fromIndex begin of search within the part.
 @param maxToTest maximal numbers of chars to test. It may be Integer.MAX_VALUE.
 @param transcriptChar any char which is the transcription designation char, especially '\\'.
   Set to 0 if no transcription should be regarded.
 @param quotationStartChar any char which is the begin char of a quotation. Set to 0 if no quotation should be regarded.
 @param quotationEndChar the adequate end char   
 @return position of first founded char inside the actual part, but not greater than maxToTest, if no chars is found unitl maxToTest,
         but -1 if the end is reached.
*/
public final int indexOfAnyChar(CharSequence sChars, final int fromWhere, final int maxToTest
   , char transcriptChar, char quotationStartChar, char quotationEndChar)
{ int pos = this.begin + fromWhere;
 int max = (this.end - pos) < maxToTest ? this.end : this.begin + maxToTest;
 boolean bNotFound = true;
 while(pos < max && bNotFound){ 
   char cc = this.content.charAt(pos);
   if(cc == quotationStartChar && cc !=0)
   { int endQuotion = indexEndOfQuotation(quotationEndChar, transcriptChar, pos - this.begin, max - this.begin);
     if(endQuotion < 0){ pos = max; }
     else{ pos = endQuotion + this.begin; }
   }
   else if(cc == transcriptChar && cc != 0 && pos < (max-1)){
     pos +=2;
   }
   else
   { if(StringFunctions.indexOf(sChars, cc) >= 0){ 
     bNotFound = false; 
     } else{ 
       pos +=1; 
     }
   }
 }
 if(bNotFound){
   if(StringFunctions.indexOf(sChars, StringFunctions.cEndOfText) >= 0) return pos - this.begin;  // it is found because cEndOfText is searched too.
   else return -1;
 }
 else return (pos - this.begin);
}



/**Returns the last position of one of the chars in sChars 
* within the part of actual part from (fromIndex) to (fromIndex+maxToTest) 
* or returs -1 if the char is not found in this part.
 @param sChars contents some chars to find. The char with code 
 @param fromIndex begin of search within the part.
 @param maxToTest maximal numbers of chars to test. It may be Integer.MAX_VALUE. 
 @return position of first founded char inside the actual part, but not greater than maxToTest. 
        if no chars is found unitl maxToTest,
         but -1 if the end is reached.
*/
public final int lastIndexOfAnyChar(CharSequence sChars, final int fromWhere, final int maxToTest)
{ int pos = (this.end - this.begin) < maxToTest ? this.end -1 : this.begin + maxToTest-1;
 int min = this.begin + fromWhere;
 
 while(pos >= min && StringFunctions.indexOf(sChars, this.content.charAt(pos)) < 0)
 { pos -=1;
 }
 int index = pos >= min 
           ? pos - this.begin  //relative found position
           :  -1;         //not found
 return index;
}



  /**Returns the position of one of the chars in sChars within the part, started inside the part with fromIndex,
   *  returns -1 if the char is not found in the part started from 'fromIndex'.
   * @param listStrings contains some Strings to find.
   * @param fromWhere begin of search within the part.
   * @param maxToTest maximal numbers of chars to test. It may be Integer.MAX_VALUE. 
    * @param nrofFoundString If given, [0] is set with the number of the found CharSequence in listStrings, 
   *                        count from 0. This array reference may be null, then unused.
   * @param foundString If given, [0] is set with the found CharSequence. This array reference may be null.
   * @return position of first founded char inside the actual part, but not greater than maxToTest, 
   *                 if no chars is found until maxToTest, but -1 if the end is reached.
   */
  public final int indexOfAnyString
  ( CharSequence[] listStrings
  , final int fromWhere
  , final int maxToTest
  , @Java4C.SimpleVariableRef int[] nrofFoundString
  , @Java4C.SimpleVariableRef String[] foundString
  )
  { assert(fromWhere >=0);
    int start = this.begin + fromWhere;
    int max = (this.end - start) < maxToTest ? this.end : start + maxToTest;
    int pos = StringFunctions.indexOfAnyString(this.content, start, max, listStrings, nrofFoundString, foundString);
    if(pos >=0) {
      pos -= this.begin;  //the position counts in the current part, starting and begin.
      assert(pos >=0); //searched from begin + fromWhere
    }
    return pos;
  }



/** Searches any char contained in sChars in the current part
* but skip over quotations while testing. Example: The given string in the current part is
* <pre>abc "yxz" end:zxy</pre>
* The calling is
* <pre>lentoAnyCharOutsideQuotion("xyz", 20);</pre>
* The result is 14 because the character 'z' is found first as the end char, but outside the quoted string "xyz".
* 
* @param sChars One of this chars is a endchar. It may be null: means, every chars is a endchar.
* @param fromWhere Offset after begin to begin search. It may be 0 often.
* @param maxToTest number of character to test from fromWhere. 
*   If maxToTest is greater then the length of the current part, only the whole current part is tested.
*   Especially Integer.MAXVALUE and beu used. 
* @return -1 if no character from sChars was found in the current part. 
*   0.. Position of the found character inside the current part, but >= fromWhere
*/
public final int indexOfAnyCharOutsideQuotion(CharSequence sChars, final int fromWhere, final int maxToTest)
{ int pos = this.begin + fromWhere;
  int max = (this.end - pos) < maxToTest ? this.end : this.begin + maxToTest;
  boolean bNotFound = true;
  while(pos < max && bNotFound)
  { char cc = this.content.charAt(pos);
    if(cc == '\"')
    { int endQuotion = indexEndOfQuotion('\"', pos - this.begin, max - this.begin);
      if(endQuotion < 0){ pos = max; }
      else{ pos = endQuotion + this.begin; }
    }
    else
    { if(StringFunctions.indexOf(sChars, cc) >= 0){ bNotFound = false; }
      else{ pos +=1; }
    }
  }
  return (bNotFound) ? -1 : (pos - this.begin);
}








/**Searches the end of a quoted string. In Generally, a backslash skips over the next char
 * and does not test it as end of the quotion.  
 * @param cEndQuotion The character which is the end of the quoted text, the end-quote-character.
 * @param fromWhere Offset after begin to begin search. 
 *                  It may be 0 if the quotion starts at begin, it is the position of the left
 *                  quotion mark.
 * @param maxToTest Limit for searching, offset from begin. It may be Integer.MAX_INT
 * @return -1 if no end of quotion is found, else the position of the char after the quotion, 
 *          at least 2 because a quotion has up to 2 chars, the quotion marks itself.
 */
public final int indexEndOfQuotion(char cEndQuotion, final int fromWhere, final int maxToTest) {
  return indexEndOfQuotation(cEndQuotion, '\\', fromWhere, maxToTest);
}



/**Searches the end of a quoted string. In Generally, a backslash skips over the next char
 * and does not test it as end of the quotion.  
 * @param cEndQuotion The character which is the end of the quoted text, the end-quote-character.
 * @param transcriptChar The character which prevents end-of-quote detection if the quote-end-character follows.
 * @param fromWhere Offset after begin to begin search. 
 *                  It may be 0 if the quotion starts at begin, it is the position of the left
 *                  quotion mark.
 * @param maxToTest Limit for searching, offset from begin. It may be Integer.MAX_INT
 * @return -1 if no end of quotion is found, else the position of the char after the quotion, 
 *          at least 2 because a quotion has up to 2 chars, the quotion marks itself.
 */
public final int indexEndOfQuotation(char cEndQuotion, char transcriptChar, final int fromWhere, final int maxToTest)
{ int pos = this.begin + fromWhere +1;
 int max = (this.end - pos) < maxToTest ? this.end : pos + maxToTest;
 boolean bNotFound = true;
 while(pos < max && bNotFound)
 { char cc = this.content.charAt(pos++);
   if(cc == transcriptChar && cc !=0 && (pos+1) < max)
   { pos += 1; //on \ overread the next char, test char after them!
   }
   else if(cc == cEndQuotion)
   { bNotFound = false;
   }
 }
 return (bNotFound ? -1 : (pos - this.begin));
}





/**Returns the position of one of the chars in sChars within the part,
  returns -1 if the char is not found in the actual part.
 @param sChars contents some chars to find.
 @return position of first founded char inside the actual part or -1 if not found.
*/
public final int indexOfAnyChar(CharSequence sChars)
{ return indexOfAnyChar(sChars, 0, Integer.MAX_VALUE);
}



/**Returns the position of the first char other than the chars in sChars within the part, started inside the part with fromIndex,
  returns -1 if all chars inside the parts  started from 'fromIndex' are chars given by sChars.
 @param sChars contents the chars to overread.
 @param fromIndex begin of search within the part.
 @return position of first foreign char inside the actual part or -1 if not found.
*/
public final int indexOfNoChar(CharSequence sChars, final int fromWhere)
{ int pos = this.begin + fromWhere;
 while(pos < this.end && StringFunctions.indexOf(sChars, this.content.charAt(pos)) >= 0) pos +=1;
 return (pos >= this.end) ? -1 : (pos - this.begin);
}


/**Returns the position of the first char other than the chars in sChars within the part,
  returns -1 if all chars inside the parts are chars given by sChars.
 @param sChars contents the chars to overread.
 @return position of first foreign char inside the actual part or -1 if not found.
*/
public final int indexOfNoChar(CharSequence sChars)
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
* @param mode Possible values are StringPartBase.seekBack or StringPartBase.seekNormal = 0.       
* @return This itself.
*/
public final StringPart lentoAnyChar(CharSequence sChars, int maxToTest)
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
* @param mode Possible values are {@link #mSeekBack} or {@link #,seekNormal} = 0.       
* @return This itself.
*/
public final StringPart lentoAnyChar(CharSequence sChars, int maxToTest, int mode)
{ this.endLast = this.end;
 int pos;
 if((mode & mSeekBackward_) != 0)
 { pos = lastIndexOfAnyChar(sChars, 0, maxToTest);
 }
 else
 { pos = indexOfAnyChar(sChars, 0, maxToTest);
 }
 if(pos < 0){ this.end = this.begin; this.bFound = false; }
 else       { this.end = this.begin + pos; this.bFound = true; } 
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
public final StringPart lentoAnyString(CharSequence[] strings, int maxToTest)
//public StringPartBase lentoAnyString(List<String> strings, int maxToTest)
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
* @param mode possible values are StrinPart.seekNormal or StringPartBase.seekEnd.
*        <ul><li>StringPartBase.seekEnd: the found string is inclusive.
*        </ul>       
* @return This itself.
*/
public final StringPart lentoAnyString(CharSequence[] strings, int maxToTest, int mode)
//public StringPartBase lentoAnyString(List<String> strings, int maxToTest, int mode)
{ this.endLast = this.end;
  @Java4C.StackInstance @Java4C.SimpleArray String[] foundString = new String[1];
  int pos = indexOfAnyString(strings, 0, maxToTest, null, foundString);
  if(pos < 0){ this.end = this.begin; this.bFound = false; }
  else       
  { if( (mode & mSeekEnd) != 0)
    { pos += foundString[0].length();
    }
    this.end = this.begin + pos; this.bFound = true; 
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
* @param strings List of type CharSequence, containing the possible end strings.
* @param iIndentChars possible chars inside a skipped indentation. If the last char is space (" "),
*        also spaces after the indentation of the first line are skipped. 
* @param maxToTest Maximum of chars to test. If the endchar isn't find inside this number of chars,
*        the actual length is set to 0.
* @param buffer The buffer where the found String is stored. The stored String has no indentations.       
* @since 2007, 2010-0508 changed param buffer, because better useable in C (java2c)
*/
public final void lentoAnyStringWithIndent(CharSequence[] strings, CharSequence sIndentChars, int maxToTest, StringBuilder buffer)
//public String lentoAnyStringWithIndent(List<String> strings, String sIndentChars, int maxToTest)
{ assert(this.end <= this.content.length());
  this.endLast = this.end;
 //String sRet; sRet = "";
 buffer.setLength(0);
 int indentColumn = getCurrentColumn();
 int startLine = this.begin;
 boolean bAlsoWhiteSpaces = (sIndentChars.charAt(sIndentChars.length()-1) == ' ');
 int pos = indexOfAnyString(strings, 0, maxToTest, null, null);
 if(pos < 0){ this.end = this.begin; this.bFound = false; }
 else       
 { this.bFound = true;
   this.end = this.begin + pos; 
   boolean bFinish = false;
   while(!bFinish)  
   { pos = StringFunctions.indexOf(this.content, '\n', startLine);
     if(pos < 0) pos = this.end;
     if(pos > this.end)
     { //next newline after terminated string, that is the last line.
       pos = this.end;
       bFinish = true;
     }
     else { pos +=1; } // '\n' including
     //append the line to output string:
     buffer.append(this.content.subSequence(startLine, pos));
     if(!bFinish)
     { //skip over indent.
       startLine = pos;
       int posIndent = startLine + indentColumn;
       if(posIndent > this.end) posIndent = this.end;
       while(startLine < posIndent && StringFunctions.indexOf(sIndentChars, this.content.charAt(startLine)) >=0)
       { startLine +=1;
       }
       if(bAlsoWhiteSpaces)
       { while(" \t".indexOf(this.content.charAt(startLine)) >=0)
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
public final StringPart lentoAnyCharOutsideQuotion(CharSequence sChars, int maxToTest)
{ this.endLast = this.end;
 int pos = indexOfAnyCharOutsideQuotion(sChars, 0, maxToTest);
 if(pos < 0){ this.end = this.begin; this.bFound = false; }
 else       { this.end = this.begin + pos; this.bFound = true; } 
 return this;
}


/** Sets the length of the current part to the end of the quotion. It is not tested here,
* whether or not the actual part starts with a left quotion mark.
* In Generally, a backslash skips over the next char and does not test it as end of the quotion.  
* @java2c=return-this.
* @param sEndQuotion The char determine the end of quotion, it may be at example " or ' or >.
* @param maxToTest Maximum of chars to test. If the endchar isn't find inside this number of chars,
*        the actual length is set to 0.
* @return This itself.
*/
public final StringPart lentoQuotionEnd(char sEndQuotion, int maxToTest)
{ this.endLast = this.end;
 int pos = indexEndOfQuotion(sEndQuotion, 0, maxToTest);
 if(pos < 0){ this.end = this.begin; this.bFound = false; }
 else       { this.end = this.begin + pos; this.bFound = true; } 
 return this;
}



/**Sets the length of the current part to the end of the current line.
 * Note The current part is empty if the position is on end of a line yet.
 * @java2c=return-this.
 * @return <code>this</code> to concatenate some operations, usage example <code>part.set(src).seek(sKey).lento(';').len0end();</code>
 *   Hint: use {@link #found()} to detect whether the end string is found.
 *   if the end String is not found, the current part has length ==0
 */
@Java4C.Retinline
public final StringPart lentoLineEnd(){ return lentoAnyChar("\n\r\f"); }


/**Increments the begin of the current part over maybe found whitespaces
 * and decrements the end of the current part over maybe found whitespaces.
 * The {@link #found()} returns false if the current part has no content.
 * The {@link #getCurrentPart()} returns an empty String if the current part has no content
 * The method invokes {@link #seekNoWhitespace()} and {@link #lenBacktoNoChar(CharSequence)} with " \t\r\n\f".
 * @java2c=return-this.
 * @return this to concatenate.
 */
@Java4C.Retinline
public final StringPart trimWhiteSpaces() {
  seekNoWhitespace();
  lenBacktoNoChar(" \t\r\n\f");
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
public final StringPart lentoAnyChar(CharSequence sChars)
{ lentoAnyChar(sChars, Integer.MAX_VALUE);
 return this;
}




  /**Sets the length to the end of the maximal part if the length is 0. This method could be called at example
if a end char is not detected and for that reason the part is valid to the end.
 * @java2c=return-this.
 @return <code>this</code> to concatenate some operations, like <code>part.set(src).seek(sKey).lento(';').len0end();</code>
 */
public final StringPart len0end()
{ if(this.end <= this.begin) this.end = this.endMax;
  return this;
}



  /**Sets the length to the end of the maximal part.
   * @java2c=return-this.
  */
  public final StringPart setLengthMax()
  { this.end = this.endMax;
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
      @param sChars CharSequence with the chars to overread.
   * @return <code>this</code> to concatenate some operations, usage example <code>part.set(src).seek(sKey).lento(';').len0end();</code>
   *   Hint: use {@link #found()} to detect whether the end string is found.
   *   if the end String is not found, the current part has length ==0
   */
  public final StringPart lenBacktoNoChar(CharSequence sChars)
  { this.endLast = this.end;
    while( this.end > this.begin && StringFunctions.indexOf(sChars, this.content.charAt(this.end-1)) >=0){ this.end = this.end -1; }
    if(this.end <= this.begin)
    { this.end = this.begin; this.bFound = false;  //all chars skipped to left.
    }
    else this.bFound = true;
    return this;
  }
  
  /**Trims all trailing whitespaces within the part.
   * @java2c=return-this.
   * @return <code>this</code> to concatenate some operations, usage example <code>part.set(src).seek(sKey).lento(';').len0end();</code>
   *   Hint: use {@link #found()} to detect whether the end string is found.
   *   if the end String is not found, the current part has length ==0
   */
  public final StringPart lenBacktoNoWhiteSpaces()
  {
    return lenBacktoNoChar(" \t\n\r");
  }

  /** Trims all leading and trailing whitespaces within the part.
      A Comment begins with "//".
   * @java2c=return-this.
      @return <code>this</code> to concat some operations, like <code>part.set(src).seek(sKey).lento(';').len0end();</code>
  */
  public final StringPart trim()
  { return seekNoChar(" \t\n\r")   //start position increased
      .lenBacktoNoChar(" \t\n\r");  //end position decreased
  }



  /** Trims a java- or C-style line-comment from end of part and all leading and trailing whitespaces.
      A Comment begins with "//".
   * @java2c=return-this.
      @return <code>this</code> to concat some operations, like <code>part.set(src).seek(sKey).lento(';').len0end();</code>
  */
  final StringPart trimComment()
  { this.beginLast = this.begin;
    this.endLast = this.end;
    int posComment = indexOf("//");
    if(posComment >=0) this.end = this.begin + posComment;
    this.bFound = (this.begin > this.beginLast);
    return trim();
  }



  /**Compares the current part with the given str2
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  @Override public final int compareTo(CharSequence str2)
  { CharSequence currentpart = this;  //this is a CharSequence for the current part.
    return StringFunctions.compare(currentpart, 0, str2, 0, Integer.MAX_VALUE);
  }
  

 
  
  
  
  
  /** Returns the position of the char within the part,
   * returns -1 if the char is not found in the part.
     The methode is likely String.indexOf().
    @param ch character to find.
    @return position of the char within the part or -1 if not found within the part.
    @exception The method throws no IndexOutOfBoundaryException.
    It is the same behavior like String.indexOf(char, int fromEnd).
  */
  public final int indexOf(char ch)
  { int pos = StringFunctions.indexOf(this.content, this.begin, this.end, ch);;
    if(pos < 0) return -1;
    else return pos - this.begin;
  }

  /** Returns the position of the char within the part, started inside the part with fromIndex,
   * returns -1 if the char is not found in the part started from 'fromIndex'.
     The method is likely String.indexOf().
    @param ch character to find.
    @param fromIndex start of search within the part.
    @return position of the char within the part or -1 if not found within the part.
    @exception The method throws no IndexOutOfBoundaryException. If the value of fromIndex
    is negative or greater than the end position, -1 is returned (means: not found).
    It is the same behavior like String.indexOf(char, int fromEnd).
  */
  public final int indexOf(char ch, int fromIndex)
  { if(fromIndex >= (this.end - this.begin) || fromIndex < 0) return -1;
    else
    { int pos = StringFunctions.indexOf(this.content, this.begin + fromIndex, this.end, ch);;
      if(pos < 0) return -1;
      else return pos - this.begin + fromIndex;
    }
  }


  /** Returns the position of the string within the part. Returns -1 if the string is not found in the part.
  Example: indexOf("abc") returns 6, indexOf("fgh") returns -1 <pre>
     abcdefgabcdefghijk
part:   =============  </pre>
@param sCmp string to find
@return position of the string within the part or -1 if not found within the part.
*/
public final int indexOf(CharSequence sCmp)
{ int pos = StringFunctions.indexOf(this.content, this.begin, this.end, sCmp);  //content.substring(begin, end).indexOf(sCmp);
if(pos < 0) return -1;
else return pos - this.begin;
}




/** Returns the position of the string within the part. Returns -1 if the string is not found in the part.
Example: indexOf("abc") returns 6, indexOf("fgh") returns -1 <pre>
   abcdefgabcdefghijk
part:   =============  </pre>
@param sCmp string to find
@return position of the string within the part or -1 if not found within the part.
*/
public final int XXXindexOf(CharSequence sCmp)
{ int pos = StringFunctions.indexOf(this.content, this.begin, this.end, sCmp);  //content.substring(begin, end).indexOf(sCmp);
if(pos < 0) return -1;
else return pos - this.begin;
}




  /** Returns the position of the string within the part. Returns -1 if the string is not found in the part.
      Example: indexOf("abc") returns 6, indexOf("fgh") returns -1 <pre>
         abcdefgabcdefghijk
  part:   =============  </pre>
    @param sCmp string to find
    @return position of the string within the part or -1 if not found within the part.
  */
  public final int indexOf(CharSequence sCmp, int fromIndex, int maxToTest)
  { int max = (this.end - this.begin) < maxToTest ? this.end : this.begin + maxToTest;
    if(fromIndex >= (max - this.begin) || fromIndex < 0) return -1;
    else
    { int pos = StringFunctions.indexOf(this.content, this.begin + fromIndex, max, sCmp); //content.substring(begin + fromIndex, max).indexOf(sCmp);
      if(pos < 0) return -1;
      else return pos - this.begin + fromIndex;
    }
  }




  
  /** Compares the Part of string with the given string
   */
   public final boolean equals(CharSequence sCmp)
   { return StringFunctions.equals(this.content, this.begin, this.end, sCmp); //content.substring(start, end).equals(sCmp);
   }




   /**compares the Part of string with the given string.
    * new since 2008-09: if sCmp contains a cEndOfText char (coded with \e), the end of text is tested.
    * @param sCmp The text to compare.
   */
   public final boolean startsWith(CharSequence sCmp)
   { int pos_cEndOfText = StringFunctions.indexOf(sCmp, StringFunctions.cEndOfText, 0); //sCmp.indexOf(cEndOfText);
     
     if(pos_cEndOfText >=0)
     { if(pos_cEndOfText ==0)
       { return this.begin == this.end;
       }
       else
       { return StringFunctions.equals(this.content, this.begin, this.end, sCmp); //content.substring(start, end).equals(sCmp);
       }
       
     }
     else
     { return StringFunctions.startsWith(this.content, this.begin, this.end, sCmp); //content.substring(start, end).startsWith(sCmp);
     }
   }

   /**This routine provides the this-pointer as StringPartScan in a concatenation of StringPartBase-invocations. 
    * @return this
    * @throws ClassCastException if the instance is not a StringPartScan. That is an internal software error.
    */
   @Java4C.Exclude public StringPartScan scan()
   { return (StringPartScan)this;
   }



   /** Gets the current position, useable for rewind. This method is overwritten
    * if derived classes uses partial content.
    * It is the absolute position of the processed String. 
    */ 
   public final long getCurrentPosition()
   { return this.begin + this.absPos0;
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
   public final void setCurrentPosition(long pos)
   { if(pos < this.absPos0) {
       throw new IllegalArgumentException("Position to rewind is not possible, pos=" + pos + " abspos0=" + this.absPos0);
     }
     this.begin = (int)(pos - this.absPos0);
   }
   

   
   
   
   
   
  /** Gets a substring inside the maximal part
   * pos position of start relative to maxPart
   * posend exclusive position of end. If 0 or negativ, it counts from end backward.
   * */
  @Java4C.ReturnInThreadCxt 
  public final Part substring(int pos, int posendP)
  { int posend;
    if(posendP <=0)
    { posend = this.endMax - posendP; //if posendP is fault, an exception is thrown.
    }
    else
    { posend = posendP;
    }
    @Java4C.InThCxtRet(sign="StringPart.subString")
    Part ret = new Part(this, pos + this.begiMin, posend); //content.substring(pos+begiMin, posend); 
    return ret;
  }
  



  
  /** Gets the next chars from current Position.
   *  This method don't consider the spread of the actutal and maximal part.
      @param nChars number of chars to return. If the number of chars available in string
      is less than the required number, only the available string is returned.
  */
  @Java4C.ReturnInThreadCxt
  public final CharSequence getCurrent(int nChars)
  { final int nChars1 =  (this.endMax - this.begin) < nChars ? this.endMax - this.begin : nChars;  //maybe reduced nr of chars
    if(nChars1 ==0) return "";
    else return( new Part(this, this.begin, this.begin + nChars1));
  }

  
  
  /**Gets the chars from current Position. */
  @Java4C.ReturnInThreadCxt
  public final CharSequence getCurrent()
  { return( new Part(this, this.begin, this.end));
  }

  
  
  /** Gets the next char at current Position.
  */
  public final char getCurrentChar()
  { if(this.begin < this.endMax){ return this.content.charAt(this.begin); }
    else return '\0'; 
  }
 
  
  
  /**Get the Line number and the column of the begin position. 
   * Note: it returns null in this class, may be overridden.
   * @param column if given, it should be an int[1]. Then the column is written into. The leftest position is 1
   * @return line of the begin position if given, starting with 1 for the first line. 
   *   This basic implementation returns 0 for the line and left the column unchanged. 
   */
  public int getLineAndColumn(int[] column){ return 0; }

  

  /** Gets the current position in line (column of the text).
   * It is the number of chars from the last '\n' or from beginning to the actual char.
   * @return Position of the actual char from begin of line, leftest position is 0.
   */
  public final int getCurrentColumn()
  { //if((bitMode & mGetColumn_mode)==0){ return -1; }
    //else {
      int pos = StringFunctions.lastIndexOf(this.content, 0, this.begin, '\n');
      if(pos < 0) return this.begin;  //first line, no \n before
      else return this.begin - pos -1;
    //}
  }
  
  /**This method may be overridden to return the file which is used to build this Stringpart.
   * @return null in this implementation, no file available.
   */
  public final String getInputfile(){ return this.sFile; }
  
  
  /** Returns the actual part of the string.
   * 
   */
  @Java4C.ReturnInThreadCxt
  public final Part getCurrentPart()
  { @Java4C.InThCxtRet(sign="StringPart.getCurrentPart") final Part ret_1;
    if(this.end > this.begin) ret_1 = new Part(this, this.begin, this.end);
    else                      ret_1 = new Part(this, this.begin, this.begin);
    return ret_1 ;
  }
  

  /**Sets the actual part of the given Part as destination. The Part references the substring inside {@link #content}.
   * The content can be gotten as String from dst with {@link Part#toString()}
   * Note: renaming 1019-05-26 from setCurrentPart(...). 
   */
  protected final void setCurrentPartTo(Part dst)
  { if(this.end > this.begin) dst.setPart(this.begin, this.end);
    else                      dst.setPart(this.begin, this.begin);
  }
  

  /** Returns the last part of the string before any seek or scan operation.
   * 
   */
  @Java4C.ReturnInThreadCxt
  public final CharSequence getLastPart()
  { if(this.begin > this.beginLast) { 
      @Java4C.InThCxtRet(sign="StringPart.getLastPart") Part ret = new Part(this, this.beginLast, this.begin); return ret; 
    } 
    else return "";
  }
  

  /** Returns the actual part of the string.
   * @param maxLength if <0 (especially -1) then use the given length without limitation.
   *   elsewhere it is a limitation of characters, maybe necessary if a buffer or space to display is limited.
   *   0 : take 0 characters.
   */
  @Java4C.ReturnInThreadCxt
  public final CharSequence getCurrentPart(int maxLength)
  { int end1 = maxLength <0 ? this.end : (this.end - this.begin) <  maxLength ? this.end : this.begin + maxLength ;
    if(this.end > this.begin) {  
      @Java4C.InThCxtRet(sign="StringPart.getCurrentPart")
      final Part ret = new Part(this, this.begin, end1);
      return ret;
    }
    else return ""; 
  }
  

  
  /**Retrurn the part from start to end independent of the current positions. 
   * This method is proper to get an older part for example to log a text afterwards the text is processed.
   * Store the {@link #getCurrentPosition()} and {@link #getLen()} and apply it here!
   * Note that it is possible that an older part of string is not available furthermore if a less buffer is used
   * and the string in the buffer was shifted out. Then this method may be overridden and returns an error hint.
   * @param fromPos The start position for the returned content. It must be a valid position.
   * @param nrofChars The number of characters. It must be >= 0. If the content is shorter,
   *   that shorter part is returned without error.
   *   For example getPart(myPos, Integer.MAXINT) returns all the content till its end.
   * @return A CharSequence. Note that the returned value should be processed immediately in the same thread.
   *   before other routines are invoked from this class.
   *   It should not stored as a reference and used later. The CharSequence may be changed later.
   *   If it is necessary, invoke toString() with this returned value.
   */
  @Java4C.ReturnInThreadCxt
  public final StringPart.Part getPart(int fromPos, int nrofChars){
    final int nChars1 =  (this.endMax - fromPos) < nrofChars ? this.endMax - fromPos : nrofChars;  //maybe reduced nr of chars
    @Java4C.InThCxtRet(sign="StringPart.Part.getPart") Part ret = new Part(this, fromPos, fromPos + nChars1);
    return ret;
  }


  
  
  protected final char absCharAt(int index){
    int pos = index;
    if(this.content !=null && pos >=0 && pos < /*endMax*/this.content.length()) return this.content.charAt(pos);
    return '?'; //It is better to return a obvious char than trow.
    //else { throwIllegalArgumentException("StringPartBase.charAt - faulty; ",index); return '\0'; }
  }

  /**Returns a String from absolute range.
   * @param from The absolute position.
   * @param to The absolute end.
   * @return A valid String or an IllegalArgumentException is occurred
   */
  protected final String absSubString(int from, int to)
  { 
    int pos = from;
    int len = to - from;
    int end1 = pos + len;
    if(this.content == null){ 
      return " ??null?? ";
    }
    if(pos >=0 && end1 <= this.endMax){
      //@Java4C.ReturnNew  
      CharSequence cs1 = this.content.subSequence(pos, pos + len) ; 
      return cs1.toString(); 
    }
    else { throwIllegalArgumentException("StringPartBase.subSequence - faulty; ",from); return ""; }
  }

  
  

  
  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Java4C.ReturnInThreadCxt
  @Override public String toString(){ 
    @Java4C.InThCxtLocal(sign="toString_StringPart") CharSequence currentPart = getCurrentPart();
    @Java4C.InThCxtRet(sign="StringPart.toString.ret") String ret = currentPart.toString();
    return ret;
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
public final String debugString()
{ int len = this.endMax;
  /**@java2c=StringBuilderInThreadCxt,toStringNonPersist.*/ 
  String ret = this.content.subSequence(0, len > 20 ? 20 : len) + "<<<" + this.begin + "," + this.end + ">>>";
  if(this.begin < len){
    /**@java2c=toStringNonPersist.*/ 
    ret += this.content.subSequence(this.begin, len > (this.begin + 20) ? this.begin + 20: len); 
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
  private final void throwIndexOutOfBoundsException(String sMsg)
  throws IndexOutOfBoundsException
  { throw new IndexOutOfBoundsException(sMsg);
  }


  
  private static final void throwIllegalArgumentException(String msg, int value)
  {
    throw new IllegalArgumentExceptionJc(msg, value);
  }

  /**Closes the work. This routine should be called if the StringPart is never used, 
   * but it may be kept because it is part of class data or part of a statement block which runs.
   * The associated String is released. It can be recycled by garbage collector.
   * If this method is overridden, it should used to close a associated file which is opened 
   * for this String processing. The overridden method should call super->close() too.
   * <br>
   * Note: if only this class is instantiated and the instance will be garbaged, close is not necessary.
   * A warning or error "Resource leak" can be switched off. Therefore the interface {@link java.io.Closeable} is not used here.
   */
  @Override
  public void close()
  {
    this.content = null;
    this.begiMin = this.beginLast = this.begin = 0;
    this.endMax = this.end = this.endLast = 0;
    this.bCurrentOk = this.bFound = false;
  }
  

  /**Replaces up to 20 placeholder with a given content.
   * The method creates a StringBuilder with buffer and a StringPart locally. 
   * @param src The source String, it may be a line.
   * @param placeholder An array of strings, any string of them may be found in the src. 
   * @param value An array of strings appropriate to the placeholder. Any found placeholder 
   *        will be substitute with that string. 
   * @param dst A given StringBuilder-instance maybe with a start content. If null, then a StringBuilder will be created here
   * @return dst if given, src will be appended to it, or a created StringBuilder with result.
   *   TODO don't create a StringBuilder but return src if src does not contain anything to replace.
   * @since 2016-11: returns a CharSequence instead String, it is more optimized, does not need an extra maybe unnecessary buffer.
   *   For older usages you should add toString() after the result of this routine to preserve compatibility. 
   */
  public static CharSequence replace(CharSequence src, CharSequence[] placeholder, CharSequence[] value, StringBuilder dstArg)
  { final int len = src.length();
    //int ixPos = 0;
    int nrofToken = placeholder.length;
    if(nrofToken != value.length) {
      throwIllegalArgumentException("token and value should have same size, lesser 20", nrofToken); return src; }
    StringBuilder dst = dstArg != null ? dstArg : new StringBuilder(len + 100); //calculate about 53 chars for identifier
    //@Java4C.StackInstance final StringPart spPattern = new StringPart(src);
    int posPatternStart = 0;
    int posPattern;
    do
    { @Java4C.StackInstance @Java4C.SimpleArray int[] type = new int[1];
      posPattern = StringFunctions.indexOfAnyString(src, posPatternStart, src.length(), placeholder, type, null);
      if(posPattern >=0){
        dst.append(src.subSequence(posPatternStart, posPattern));  //characters from previous placeholder-end till next placeholder
        int ixValue = type[0];
        dst.append(value[ixValue]);
        posPatternStart = posPattern + placeholder[ixValue].length();
      } else { //last pattern constant part:
        dst.append(src.subSequence(posPatternStart, len));
        posPatternStart = -1;  //mark end
      }
    }while(posPatternStart >=0);
    return dst;
  }
  



  
  /**This class presents a part of the parent CharSequence of this class.
   * The constructor is protected because instances of this class are only created in this class or its derived, not by user.
   * The CharSequence methods get the characters from the parent CharSequence of the environment class {@link StringPart#content}.
   * <br>
   * Important: An instance is only intended for immediate use. Build a String via calling {@link #toString()} if the text should be persistently stored.
   * Using of the Part instance immediately helps safe calculation time and storage because it does not need allocated memory. It is a cheap operation only with some indices.
   * The content of a Part will become non accessible if the {@link StringPart#content} was shifted out especially by invocation of {@link StringPartFromFileLines#readnextContentFromFile(int)}.
   * It means a Part instance should only be immediately used if the position is near the current {@link StringPart#begin} position. 
   * The {@link StringPartFromFileLines#readnextContentFromFile(int)} let 1/3 of the {@link StringPart#content} accessible. 
   */
  public final static class Part implements CharSequence{ 
    
    /**Absolute positions of part of chars*/
    private int b1, e1, absPos0;
    
    
    private final StringPart outer;
    
    /**A subsequence
     * @param from absolute positions
     * @param to
     */
    protected Part(StringPart outer, int from, int to){
      this.outer = outer;
      setPart(from, to);
    }
    
    
    
    protected void setPart(int from, int to) {
      assert(from >= 0 && from <= this.outer.endMax);
      assert(to >= 0 && to <= this.outer.endMax);
      assert(from <= to);
      this.b1 = from; this.e1 = to; this.absPos0 = this.outer.absPos0;
    }
    
    
    
    @Override
    public final char charAt(int index)
    { return this.outer.absCharAt(this.absPos0 - this.outer.absPos0 + this.b1 + index);
    }
    
    
    @Override
    public final int length()
    { return this.e1 - this.b1;
    }
    
    @Override public final boolean equals(Object cmp) {
      CharSequence cmp1 = cmp instanceof CharSequence? (CharSequence)cmp : cmp.toString();
      int z = length();
      if(cmp1.length() != z) return false;
      for(int i = 0; i < z; ++i) {
        if(charAt(i) != cmp1.charAt(i)) return false;
      }
      return true;
    }
    
    @Override
    @Java4C.ReturnInThreadCxt
    public final CharSequence subSequence(int from, int end)
    { int start = this.absPos0 - this.outer.absPos0 + this.b1;
      @Java4C.InThCxtRet(sign="StringPart.Part.subSequence") Part ret = new Part(this.outer, start + from, start + end);
      return ret;
    }
  
    @Override final public String toString(){
      return this.outer.absSubString(this.absPos0 - this.outer.absPos0 + this.b1, this.absPos0 - this.outer.absPos0 + this.e1);
    }
    
    
    
    /**Copy to any other buffer to build persistent data. 
     * @param dst should be allocated with enough space, use this.{@link #length()} to detect how much is necessary.
     * @param from usual 0
     * @return the number of copied character
     */
    int copyToBuffer(char[] dst, int from, int to) {
      int pos = from;
      int max = Math.min(to - from, dst.length - from);
      if (max > this.e1 - this.b1) { max = this.e1 - this.b1; }
      for(int ix = this.absPos0 - this.outer.absPos0 + this.b1; ix < this.absPos0 - this.outer.absPos0 + this.b1 + max ; ++ix ) {
        dst[pos++] = this.outer.content.charAt(ix);
      }
      return max;
    }
    
    
    
    /**Builds a new Part without leading and trailing white spaces.
     * Without " \r\n\t"
     * @return a new Part.
     */
    @Java4C.ReturnInThreadCxt
    public final Part trim(){
      int b2 = this.absPos0 - this.outer.absPos0 + this.b1; int e2 = this.absPos0 - this.outer.absPos0 + this.e1;
      while(b2 < e2 && " \r\n\t".indexOf(this.outer.content.charAt(b2)) >=0){ b2 +=1; }
      while(e2 > b2 && " \r\n".indexOf(this.outer.content.charAt(e2-1)) >=0){ e2 -=1; }
      @Java4C.InThCxtRet(sign="StringPart.Part.subSequence") Part ret = new Part(this.outer, b2, e2);
      return ret;
    }
    
    
  }



  class Iter implements Iterator<StringPart> { 

    boolean bHasNextCalled = false;
    boolean bFirst;
    
    Iter ( ) {
      setParttoMax();
      firstlineMaxpart();
      this.bFirst = true;
    }
    
    
    /**Check whether a next line is available. 
     * Calling of hasNext() is usual always before next.
     * But it is not forbidden to call next before evaluate the content.
     * Because the returned next() is always the same instance, only with changed position,
     * here hasNext() must not invoke thinks of the following next()!  
     * <br>
     * If hasNext() is called twice without next(), it is ok.
     */
    @Override
    public boolean hasNext() { 
      int zContent = StringPart.this.content.length();
      int startNext = StringPart.this.endMax;
      char cc =0;
      if(startNext < zContent && "\r\n".indexOf(cc=StringPart.this.content.charAt(startNext)) >=0) { startNext +=1; }
      if(cc == '\r' && startNext < zContent && (cc=StringPart.this.content.charAt(startNext)) =='\n') { startNext +=1; }
      return startNext < zContent  //not on last position 
          || (this.bFirst && StringPart.this.end >0);            //one line content given without \n
    }
  
  
  
    /**Returns the next line.
     * Here the next line is preset in the same instance of the outer class, which is returned.
     * It uses {@link StringPart#nextlineMaxpart()}.
     * <br>
     * Note: Changing the {@link StringPart#endMax} with other operations changes the spread.
     * That is especially {@link StringPart#setParttoMax()} and {@link StringPart#setCurrentMaxPart()}.
     * This operations should not be used in this iterator environment.
     * <br>
     * But especially all other scan Functions can be used. Especially {@link StringPart#fromEnd()} etc. can be used.
     * The line returned here is determined with the spread from {@link StringPart#begiMin} to {@link StringPart#endMax}
     * which are not changed by this operations. 
     */
    @Override
    public StringPart next() {
      if(this.bFirst) { 
        this.bFirst = false;
      }
      else {
        nextlineMaxpart();
      }
      return StringPart.this; 
    }
  }


  @Override
  public Iterator<StringPart> iterator() {
    return new Iter();
  }


  
}
