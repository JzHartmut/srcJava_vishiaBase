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
 * @author Hartmut Schorrig www.vishia.org/Java
 *
 ****************************************************************************/
package org.vishia.util;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
//import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;


public class StringPartFromFileLines extends StringPartScan
{
  
  /**Version, history and license.
   * list of changes:
   * <ul>
   * <li>2020-02-02 Hartmut new {@link #StringPartFromFileLines(Class, String, int, String, Charset)}
   *   <br>{@link #StringPartFromFileLines(InputStream, String, int, String, Charset)} with negativ argument sizeBuffer
   * <li>2018-12-22 Hartmut bugfix in {@link StringPart}, improvement here: {@link #readnextContentFromFile(int)} shifts only {@link StringPart#begin}/2 
   *   to save the content near to and left from the current working area for access capability of a {@link Part} near the current content.  
   * <li>2018-01-06 Hartmut bugfix, bug: If the file to read was only 201..209 Bytes, the bytes after 200 were not read. 
   *   fix: readnextContentFromFile(0) instead (10) necessary, to read in any case. 2017-12: Renaming package private maxIxLinePosition vs. endIxLinePosition. 
   * <li>2016-09-25 Hartmut now works with lesser buffer.
   * <li>2015-10-24 Hartmut new: {@link StringPartFromFileLines#StringPartFromFileLines(InputStream, String, int, String, Charset)}
   *   to use with <code>ClassLoader.getSystemClassLoader().getResourceAsStream("path");   
   * <li>2015-06-07 Hartmut chg: {@link #getLineAndColumn(int[])}: column counts from 1 on leftest position.
   * <li>2014-05-22 Hartmut new: {@link #setInputfile(String)} invoked with input file 
   * <li>2014-04-22 Hartmut chg: improved line numbers 
   * <li>2012-12-22 Hartmut chg: close() the file in constructor if the whole file was read.
   * <li>2010-02-11 Hartmut new: The ctor StringPartFromFileLines(File fromFile) is added, 
   *   it is compatible to StringPartFromFile now.
   * <li>2009-05-31 Hartmut detecting and ignoring a BOM (byte order mark) as first char.
   * <li>2009-04-09 Hartmut encoding detection korrig.
   * <li>2006-05-00 Hartmut creation
   * </ul>
   * 
   * <b>Copyright/Copyleft</b>:
   * For this source the LGPL Lesser General Public License, published by the Free Software Foundation is valid.
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
  public static final String version = "2020-02-02";
  
  //final StringBuilder buffer;
  //char[] fileBuffer = new char[1024];
  //int nCharsFileBuffer = 0;
  
  final char[] cBuffer;
  
  //final ByteBuffer byteBuffer;
  
  
  //final CharBuffer chBuffer;
  
  /**Nr of characters in buffer. */
  int zBuffer;
  
  //final CharsetDecoder charDecoder;
  
  final Charset charset;
  
  /**The second newline character set if the first nl was read. 
   * It is a member variable because its possible that the buffer was filled exactly between 1. and 2. newline character.
   */
  private char cExpectedSesoncNl;
  
  /** A readed line from file.*/
  //String sLine = null;
  
  /** Nr of chars in line without trailing spaces.*/
  //int nLine = 0;
  
  /**Current line. */
  int nLineCt = 0;
  
  boolean bEof;
  
  boolean bLesserReadAsExpected;
  
  /** The reader maybe with correct charset.
   * 
   */
  //Reader readIn;
  
  
  //InputStream inp;
  
  Reader inpr;
  
  //final FileChannel fileChn;

  
  int nrFirstLineInPositions;

  IntegerBlockArray linePositions = new IntegerBlockArray(1000);
  
  /**After the last used position in the {@link #linePositions} array. */
  int endIxLinePosition;
  
  
  String sNewline = "\n";
  
  
  /**Fills a StringPart from a File. If the file is less than the maxBuffer size,
   * the whole file is inputed into the StringPart, otherwise the StringPart is 
   * reloaded if the first area is proceed. This constructor ivokes {@link #StringPartFromFileLines(File, int, String, Charset)}
   * with default arguments for charset and buffer size. 
   * 
   * @param fromFile The file to read<br>
   *        
   * @throws FileNotFoundException If the file is not found
   * @throws IOException If any other exception is thrown
   */
  public StringPartFromFileLines(File fromFile)
  throws FileNotFoundException, IOException, IllegalCharsetNameException, UnsupportedCharsetException
  {
    this(fromFile, 0, null, null);
  }
  
  

    /**fills a StringPart from a File. If the file is less than the maxBuffer size,
   * the whole file is inputted into the StringPart, otherwise the StringPart is 
   * reloaded if the first area is proceed. This constructor ivokes {@link #StringPartFromFileLines(InputStream, String, int, String, Charset)}
   * with the opened file. 
   * 
   * @param fromFile The file to read<br>
   * 
   * @param maxBuffer The maximum of length of the associated StringBuffer.<br>
   * 
   * @param sEncodingDetect If not null, this string is searched in the first line,
   *        readed in US-ASCII or UTF-16-Format. If this string is found, the followed
   *        string in quotion marks or as identifier with addition '-' char is readed
   *        and used as charset name. If the charset name is failed, a CharsetException is thrown.
   *        It means, a failed content of file may cause a charset exception.<br>
   *        
   * @param charset If not null, this charset is used as default, if no other charset is found in the files first line,
   *        see param sEncodingDetect. If null and not charset is found in file, the systems default charset is used.<br>
   *        
   * @throws FileNotFoundException If the file is not found
   * @throws IOException If any other exception is thrown
   */
  @SuppressWarnings("resource")  //NOTE: It will be closed by check bLesserReadAsExpected. It will be closed by close().
  public StringPartFromFileLines(File fromFile, int maxBuffer, String sEncodingDetect, Charset charset)
  throws FileNotFoundException, IOException, IllegalCharsetNameException, UnsupportedCharsetException
  {
    this( new FileInputStream(fromFile)
        , FileSystem.normalizePath(fromFile).toString()
        ,    maxBuffer <= 0 
          || fromFile.length() < maxBuffer +10  //if the file length is given, use +1 to detect bLesserReadAsExpected, since 2016-09  
            ? (int)fromFile.length() +1 
            : maxBuffer                         //use exactly maxBuffer if the file is longer than 10 bytes as maxBuffer
        , sEncodingDetect, charset);   
    if(this.bLesserReadAsExpected){
      inpr.close();
      inpr = null;
    }
  }
  
  
  
  
  
  
  
  /**Loads a resource from jar file into this StringBuffer. Note: This have to be {@link #close()} to close the resource.  
   * @param clazz  This class is the start point of the:
   * @param pathInJarFromClazz relative from clazz, use "../../package/path/to/resource.ext" if it is not in the same package.
   * @param maxSizeBuffer The size for the StringPart is gotten from the resource: {@link InputStream#available()},
   *   at least 1000 chars if available() returns not the length, but at least this size. 
   * @param sEncodingDetect If not null, this string is searched in the first 2 lines,
   *        read in US-ASCII or UTF-16-Format. If this string is found, the followed
   *        string in quotion marks or as identifier with addition '-' char is read
   *        and used as charset name. If the charset name is failed, a CharsetException is thrown.
   *        It means, a failed content of file may cause a charset exception.<br>
   *        
   * @param charset If not null, this charset is used as default, if no other charset is found in the files first line,
   *        see param sEncodingDetect. If null and not charset is found in file, the systems default charset is used.<br>
   *        
   * @throws IllegalCharsetNameException
   * @throws UnsupportedCharsetException
   * @throws IOException
   */
  public StringPartFromFileLines(Class<?> clazz, String pathInJarFromClazz, int maxSizeBuffer
    , String sEncodingDetect, Charset charsetDefault) 
    throws IllegalCharsetNameException, UnsupportedCharsetException, IOException {
    this( clazz.getResourceAsStream(pathInJarFromClazz)
        , "jar:" + pathInJarFromClazz
        , -maxSizeBuffer         //determine from stream, use this as maximal value.
        , sEncodingDetect
        , charsetDefault);
    
  }
  
  
  
  
  
  /**Fills a StringPart from a opened Stream. It can be used for example with 
   * <code>ClassLoader.getSystemClassLoader().getResourceAsStream("path"); </code>.
   * It is the core method called in the other constructors using a File input.
   * This is the core routine called from all other constructors. All capabilities described on the other constructors are here.
   * 
   * @param input Any input stream, maybe a ClassLoader getRessourceAsStream<br>
   * 
   * @param sInputPath Hint for error messages from which input is it.
   * 
   * @param sizeBuffer The length of the associated StringBuffer. 
   *        If negative, then determine form input.available, but use the given negate number as maximum if >= (-1000). <br>
   * 
   * @param sEncodingDetect If not null, this string is searched in the first 2 lines,
   *        read in US-ASCII or UTF-16-Format. If this string is found, the followed
   *        string in quotion marks or as identifier with addition '-' char is read
   *        and used as charset name. If the charset name is failed, a CharsetException is thrown.
   *        It means, a failed content of file may cause a charset exception.<br>
   *        
   * @param charset If not null, this charset is used as default, if no other charset is found in the files first line,
   *        see param sEncodingDetect. If null and not charset is found in file, the systems default charset is used.<br>
   *        
   * @TODO read char per char for sEncodingDetect, don't use mark and reset()!
   */
  public StringPartFromFileLines(InputStream input, String sInputPath, int sizeBuffer, String sEncodingDetect, Charset charsetDefault)
  throws IOException, IllegalCharsetNameException, UnsupportedCharsetException
  { super();
    if(input == null) throw new FileNotFoundException(sInputPath);
    setInputfile(sInputPath);
    //this.inp = input;
    bEof = false;
    int sizeBufferUsed = sizeBuffer;
    if(sizeBuffer <=0) {
      sizeBufferUsed = input.available();  //It should be the length of the ressource, but it is not sure
      if(sizeBufferUsed < 1000) { 
        sizeBufferUsed = 1000;  //assures that some enough lines are stored as once if sizeBufferUsed is to less. 
      }
      else if(sizeBuffer < -1000 && sizeBufferUsed >= -sizeBuffer) {
        sizeBufferUsed = -sizeBuffer;  //limit space. 
      }
    }
    //buffer = new StringBuilder(sizeBuffer);  //to large file
    cBuffer = new char[sizeBufferUsed];
    //byteBuffer = ByteBuffer.allocate(sizeBuffer/10*4);
    //inBuffer = byteBuffer.array();
    final byte[] inBuffer = new byte[200+7];
    linePositions.set(++endIxLinePosition, 0);  //start entry: After position 0 is line 1  
    boolean bom = false;;
    int nrofFirstBytes = input.read(inBuffer, 0, inBuffer.length-7);  //enough space for missing bytes for UTF-8
    if(nrofFirstBytes < 0) {
      nrofFirstBytes = 0;  //special case: empty file returns -1 as "end of file" but 0 bytes are read.
    }
    //TODO check BOM
    int startPos = 0;
    if(inBuffer[0] == -1 && inBuffer[1] == -2) {  //0xfffe
      bom = true;
      startPos = 2;
    } else if(inBuffer[0] == -2 && inBuffer[1] == -1) { //0xfeff
      bom = true;
      startPos = 2;
    } 
    //the sLine is used as first content for readnextContentFromFile() too, the start text.
    if(sEncodingDetect != null)
    { String sLine = new String(inBuffer, startPos, nrofFirstBytes);  //Uses the default charset, expected are characters only 00..7F  
      int posNewline = sLine.indexOf('\n'); 
      //@chg:JcHartmut-2010-0912: test 2 lines instead of the first only, because in a bash-shell script it can't be the first line!
      if(posNewline >= 0 && posNewline < nrofFirstBytes){    //= nrofFirstBytes, then an IndexOutOfBoundsException is thrown because
        posNewline = sLine.indexOf('\n', posNewline +1); //from the second line. 
      }
      if(posNewline < 0) posNewline = nrofFirstBytes;
      //test the first line to detect a charset, maybe the charset exceptions.
      StringPartScan spFirstLine = new StringPartScan(sLine.substring(0, posNewline));
      spFirstLine.setIgnoreWhitespaces(true);
      /**Check whether the encoding keyword is found: */
      if(spFirstLine.seek(sEncodingDetect, StringPartScan.seekEnd).found()
        && spFirstLine.scan("=").scanOk() 
        )
      { String sCharset;
        spFirstLine.seekNoWhitespace();
        if(spFirstLine.getCurrentChar() == '\"')
        { sCharset = spFirstLine.seek(1).lentoQuotionEnd('\"', 100).getCurrentPart().toString();
          if(sCharset.length()>0) sCharset = sCharset.substring(0, sCharset.length()-1);
        }
        else
        { sCharset = spFirstLine.lentoIdentifier(null, "-").getCurrentPart().toString();
        }
        if(sCharset.length() > 0)
        { //the charset is defined in the first line:
          this.charset = Charset.forName(sCharset);  //replace the current charset
        }
        else {
          spFirstLine.close();
          throw new IllegalArgumentException("charset requested with \"" + sEncodingDetect + "\", this String was found but the encoding name found in file is faulty");  
        }
        //sLine = new String(inBuffer, startPos, nrofFirstChars, charset);
      } else {
        //charset not found in file or syntax faulty.
        this.charset = charsetDefault == null ? Charset.defaultCharset() : charsetDefault;
      }
      spFirstLine.close();
    }
    else {
      this.charset = charsetDefault == null ? Charset.defaultCharset() : charsetDefault;
    } 
    inpr = new InputStreamReader(input, this.charset);
    //re-read the string with the correct charset
    if(charset.equals(Charset.forName("UTF-8"))) {
      int ix1 = nrofFirstBytes -1;
      while( (inBuffer[ix1] & 0xc0 ) == 0x80) ix1-=1; //return to the first character of an UTF-8 sequence
      int nrofBytesForChar = StringFunctions.nrofBytesUTF8(inBuffer[ix1]);
      int missingBytes = ix1 + nrofBytesForChar - nrofFirstBytes;
      if(missingBytes > 0) {
        int nRead =  input.read(inBuffer, nrofFirstBytes, missingBytes);
        if(nRead != missingBytes) {
          Debugutil.stop(); //File content error.
        }
        nrofFirstBytes += nRead;
      }
    }
    { //Use the java.nio.CharBuffer for encoding. Other variant: Build a temporary String.
      //NOTE: if the inBuffer contains a malformed last character, the built string contains a '\ufffd' as the last character
      //and an error is not produced.
      String start = new String(inBuffer, startPos, nrofFirstBytes - startPos, charset);
      this.zBuffer = start.length();
      for(int ix = 0; ix < this.zBuffer; ++ix){
        cBuffer[ix] = start.charAt(ix);
      }
      
      //Tested variant with CharBuffer does work too, it is commented yet.
      /*
      CharsetDecoder charDecoder = this.charset.newDecoder();
      CharBuffer chBuffer = CharBuffer.wrap(cBuffer, 0, cBuffer.length);
      ByteBuffer byteBuffer = ByteBuffer.wrap(inBuffer, startPos, nrofFirstBytes - startPos);
      CoderResult result = charDecoder.decode(byteBuffer, chBuffer, true);
      this.zBuffer = chBuffer.position();
      */
    }
    evalLineIndices(0, zBuffer);
    readnextContentFromFile(0);   //read the start content. Read anytime. Minsize is 0
    assign(new CharSq(0, -1));
  }


  
  /**Returns the line and column of the current position.
   * The line comes from an array which stores all start positions of the line, filled if the line is used.
   * The conversion between the position (used internally) and the line (only for user interface)
   * is done by binarySearch in this array. The array will be increased by demand. It is a {@link IntegerBlockArray}
   * which uses blocks of a constant size.
   * @param column The leftest position in a line is 1, like usual in editors.
   * @return line, 1 is the first line.
   * @see org.vishia.util.StringPart#getLineAndColumn(int[])
   */
  @Override public int getLineAndColumn(int[] column){ 
    int line = linePositions.binarySearch(this.begin, endIxLinePosition); 
    if(line <0){ 
      //usual negative if not found exactly.
      //The binarySearch returns the 'insertion point', it is the next line start position.
      //But the line is the previous one.
      line = -line -2; 
    } 
    if(column !=null){
      int posLineStart = linePositions.get(line);
      column[0] = this.begin - posLineStart +1; 
    }
    return line + nrFirstLineInPositions;
  }

  
  
  
  private void evalLineIndices(int from, int to) 
  {
    int ii = from; 
    while(ii < to) {
      char cc = cBuffer[ii++];
      if(cExpectedSesoncNl !=0) {  //newline with 2 characters, \r \n or \n \r:
        if(cc == cExpectedSesoncNl) { //if it is the second newline character, the new line starts after them.
          linePositions.set(++endIxLinePosition, ii);  //after xExpectedNl
          cc = 0;  //don't check it as newline character.
        } else { //if the 2. newline character don't follow, that is the first character of the new line.
          linePositions.set(++endIxLinePosition, ii-1); //cc is the first char of the next line.
        } 
        cExpectedSesoncNl = 0;
      }
      if(cc == '\r') { cExpectedSesoncNl = '\n'; }
      else if(cc == '\n') { cExpectedSesoncNl = '\r'; }
    }
  }
  
  
  
  
  
  
  /**Read next content from the file.
   * It does nothing if the current working area in the text {@link StringPart#begin} is lesser the argument minSizeForAction.
   * If does not shift the content if the {@link StringPart#begin} is < 1/8 of the size. That is especially after reading head information.
   * But it reads from file if especially minSizeForAction is ==0.
   * If shifts the text in the internal buffer {@link StringPart#content} respectively {@link #cBuffer} (the same) but only the half of current content.
   * Especially {@link Part} remain accessible if they are near the current content. See remarks there about persistence.
   * 
   * @param minSizeForAction returns without action if Current position (it is {@link StringPart#begin}) is lesser. Set =0 after for reading content after head.
   *   Set to about 2/3 of size for normal operation. 
   * @return true if eof is possible because lesser bytes than expected are read.
   * @throws IOException
   */
  public boolean readnextContentFromFile(int minSizeForAction)
  throws IOException
  { boolean bBufferFull = false;
    //check, shift only the buffer if necessary, to save calculation time. Prevent unnecessary shift.
    //if(nRestBytes < size && super.begin >0) {
    if(  super.begin >= minSizeForAction   //do only shift if necessary
      && super.begin >= (cBuffer.length /8)  //do not shift if only the head was read.
      ) {
      int sh = super.begin /2;  //remain the half content in the buffer for StringPart before begin.
      //shift the content from begin to the start of the buffer.
      int zChars = zBuffer - sh;
      for(int ii = 0; ii < zChars; ++ii) {
        cBuffer[ii] = cBuffer[ii + sh];
      }
      super.begin -= sh;
      super.begiMin -= sh;
      super.beginLast -= sh;
      super.beginScan -= sh;
      super.end -=sh;
      super.endLast -=sh;
      super.endMax -=sh;
      super.absPos0 += sh;
      zBuffer -= sh;
      //shift the linePositions.
      int idst = 0;
      for(int ix = 0; ix < endIxLinePosition; ++ix)
      {
        int pos = linePositions.get(ix);
        if(pos < sh) {
          nrFirstLineInPositions +=1;    //
        } else {
          pos -= sh;
          linePositions.set(idst++, pos);
        }
      }
      endIxLinePosition = idst;  //no more positions yet.
    }
    int nRestBytes;  //rest length from filled position till end.
    if(bEof) {
      return true;
    } else if( (nRestBytes = cBuffer.length - zBuffer) >= minSizeForAction) {
      int nrofChars = inpr.read(cBuffer, zBuffer, nRestBytes);
      if(nrofChars >0) {
        evalLineIndices(zBuffer,  zBuffer + nrofChars);
        zBuffer += nrofChars;
        super.end = super.endMax = super.endLast = this.zBuffer;
        bLesserReadAsExpected = nrofChars < nRestBytes;
        if(bLesserReadAsExpected)
          Debugutil.stop();
      }
      else {
        bLesserReadAsExpected = true;
        bEof = nrofChars <0;
      }
      return bLesserReadAsExpected;  //eof possible.
    } else {
      return false; //not eof, because nothing was read.
    }
  }


  @Override
  public void close(){
    if(inpr != null){
      try{ inpr.close(); } catch(IOException exc){}
      inpr = null;
    }
    nLineCt = 0;
    super.close();
  }
  
  
  
  private class CharSq implements CharSequence
  {
    int pos0, end;
  
    CharSq(int pos0, int end) { this.pos0 = pos0; this.end = end; }
  
    @Override public int length() { return end == -1 ? zBuffer : end - pos0; }

    @Override public char charAt(int index){ return cBuffer[index + pos0]; }
    
    /**Returns null because it is never used in StringPart. */
    @Override public CharSequence subSequence(int start, int end) { 
      return new CharSq(start, end); 
    }
    
    @Override public String toString() {
      String ret = new String(cBuffer, pos0, length());
      return ret;
    }
    
  }
  
  
  
  
}
