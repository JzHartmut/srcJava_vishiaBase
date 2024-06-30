package org.vishia.xmlSimple;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Stack;
import java.util.zip.ZipOutputStream;

import org.vishia.charset.CodeCharset;
import org.vishia.util.Debugutil;
import org.vishia.util.ExcUtil;
import org.vishia.util.StringFunctions;
import org.vishia.zip.ZipUtils;



/**This is a XML writer which does not store anything. It writes only.
 * The usage is seen in the test files, {@link #testWritetree2(File, StringBuilder)} etc with following template:
 * <pre>
    open(file, "US-ASCII", out);
    writeElement("root");
      writeText("contentRoot", true);
    writeElement("child1");
      writeAttribute("attr", "value");
      writeText("contentChild &lt;???> special chars", false);
      writeElementEnd();
    writeElement("child2");
      writeAttribute("attr2", "text ? special chars");
    close();
 * </pre>
 * The output for that is: <pre>
&lt;?xml version="1.0" encoding="US-ASCII"?>
&lt;root>
  contentRoot
  &lt;child1 attr="value">contentChild &amp;lt;&amp;#xc4;&amp;#xdc;&amp;#xd6;&amp;gt; special chars&lt;/child1>
  &lt;child2 attr2="text &amp;#x20ac; special chars"/>
&lt;/root>
 * </pre>
 * Note: The source contains the transcription for Javadoc. See the original. Javadoc presents correctly.<br>
 * Features:
 * <ul>
 * <li>Supported encodings are all UTF-, US-ASCII and ISO-8859-1 yet. Other ISO-8859-x can be enhanced 
 *   (needs a special solution, see {@link CodeCharset}
 * <li>Parallel to the file an Appendable can be written, for debug or Java-internal String usage of XML output.
 * <li>Namespaces should be written manually as attributes and as "ns:tag"
 * <li>Beatification on output: default 2 spaces for indent on any element, 
 *   indent on text content is able to parametrize  
 * </ul>  
 * @author Hartmut Schorrig
 *
 */
public class XmlSequWriter {

  
  /**Version, License and History:
   * <ul>
   * <li>2024-06-30 new {@link #writeElementEndInline(String)} necessary for LibreOffice 
   *   because this tool interprets a newline before &lt;/text:p> as a space in the paragraph.
   *   It was obvious if a paragraph does only contain an image, because then no {@link #writeElementInline(String)}
   *   was called which also forces &lt:text:p> on end of line. 
   * <li>2024-05-28 new {@link #writeElementInline(String)} to write contigous content in one line.
   * <li>2024-05-28 {@link #writeElementEnd(String)} important first to check correctness, 
   *   second also for documentation which kind of element is closed.
   * <li>2024-05-28 {@link #openZip(File, String, String, Appendable)} Now also to write zips.
   * <li>2020-10-05 check encoding stuff, some better documented and changed.
   *   Hint: Introducing namespace processing is faulty here. 
   *   The namespace can be written as normal attribute name string or tag,
   *   as given. Handling of namespaces should be done in the application.
   *   This class is in documentation yet. See {@linkplain https://vishia.org/Java/html/RWTrans/XmlSeqWriter.html}
   * <li>2019-12-24 created.
   * </ul>
   * 
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
   * 
   */
  public static final String version = "2020-01-01";

  
  private static class ElementInfo{
    final String sTag;
    boolean bIndented;
    @SuppressWarnings("unused") final ElementInfo parent;
    
    //Map<String, String> nameSpaces;
    
    public ElementInfo(String sTag, ElementInfo parent) {
      this.sTag = sTag;
      this.parent = parent;
    }
    
    @Override public String toString () {return this.sTag; }
  }
  
  
  
  
  
  
  
  /**Used for zip files. */
  private FileSystem fileSystem;
  
  private Writer wr;
  
  private OutputStream fwr;
  
  private Appendable twr;
  
  
  
  String sEncoding;
  
  //CharsetEncoder encodingX;
  Charset encoding;
  
  /**It is null for UTF encoding*/
  CodeCharset encodingCode;
  
  /**true then no reason to output transcription characters. */
  boolean bFullEncoding;
   
  boolean bElementStart;
  
  int nColumn;
  
  int nColumnMax = 88;
  
  /**Info of the current element. */
  ElementInfo elementCurr;
  
  /**Store all parent elements, necessary for close etc.
   * and also for parent namespaces.
   */
  Stack<ElementInfo> elementsParent = new Stack<ElementInfo>();
  
  int indent = 1;
  
  private String replaceBasics;
  
  public boolean bTreeComment = false;  //only able to use for a comment for test.
  
  private static String sIndent = "\n                                                                                ";
  
  public XmlSequWriter ( ) {
    replaceNewline(false);  //in texts, preserve the \n also in output.
  }
  
  public void setEncoding(String sEncoding) throws IllegalCharsetNameException {
    this.encoding = Charset.forName(sEncoding);
    if(sEncoding.startsWith("UTF-")) {
      this.encodingCode = null;        // it is not necessary, UTF can all character
    } else {
      this.encodingCode = CodeCharset.forName(sEncoding);  
      //it may be null, if the encoding is not supported.
      //if not null, then character are checked and &#x1234; is generated for unknown codes.
    }
    if(this.encoding == null) throw new IllegalCharsetNameException("Illegal encoding id: " + sEncoding);
    this.sEncoding = sEncoding; //not reached because exception on faulty identifier.
  }
  
  
  
  
  
  /**Opens for writing in a file, closes an opened file if any is open.
   * @param file can be null, then only the buffer is written.
   * @param encoding can be null, then the last used or given {@link #setEncoding(String)} is valid.
   *   Default is "UTF-8". If given the {@link #setEncoding(String)} is called here.
   *   <br>If the encoding is "ISO-8859-x" or "US-ASCII" then non map able characters are written as &#xC0DE;
   *   <br>On other encodings non mapable character are faulty written as defined by Writer(... Charset).
   * @param buffer can be null, if given, the output is written there, the same as in {@link #setDebugTextOut(Appendable)}  
   *   The buffer is written with replaced characters by &#xC0DE; as in file.
   * @return null on success, the error on error. It is either "Encoding ..." on unsupportedCharset 
   *   or "File not able to create: ..." with the absolute path of the file.
   * @throws IOException only on closing an opened file. Not expected. 
   */
  //Sets either this.wr or this.fwr, never both.
  public String open(File file, String encoding, Appendable buffer) throws IOException {
    this.twr = buffer;
    if(encoding !=null) { setEncoding(encoding); }
    else if(this.sEncoding == null) { setEncoding("UTF-8"); }
    try{
      if(this.wr !=null) { this.wr.close(); }
      else if(this.fwr !=null) { this.fwr.close(); }
      this.wr = null; this.fwr = null;
      if(file !=null) {
        if(this.encodingCode !=null) { // encoding with byte output
          this.fwr = new FileOutputStream(file);
          assert(this.wr ==null);
        } else {
          this.wr = new FileWriter(file);
//          OutputStream fwr = new FileOutputStream(file);
//          this.fwr = fwr;
//          if(this.sEncoding.startsWith("UTF-")) {  // this is multibyte, use a Writer
//            this.wr = new OutputStreamWriter(fwr, this.encoding); 
//          } else {
//            assert(this.wr == null);     // not necessary for 1-byte-encoding. 
//          }
          assert(this.fwr ==null);
        }
      }
      return null;
    } catch(UnsupportedCharsetException exc) {
      return "Encoding faulty: " + exc.getMessage();
    } catch (FileNotFoundException e) {
      return "File not able to create: " + file.getAbsolutePath();
    }
  }
  
  
  
  /**Opens for writing in a file, closes an opened file if any is open.
   * @param file can be null, then only the buffer is written.
   * @param encoding can be null, then the last used or given {@link #setEncoding(String)} is valid.
   *   Default is "UTF-8". If given the {@link #setEncoding(String)} is called here.
   *   <br>If the encoding is "ISO-8859-x" or "US-ASCII" then non map able characters are written as &#xC0DE;
   *   <br>On other encodings non mapable character are faulty written as defined by Writer(... Charset).
   * @param buffer can be null, if given, the output is written there, the same as in {@link #setDebugTextOut(Appendable)}  
   *   The buffer is written with replaced characters by &#xC0DE; as in file.
   * @return null on success, the error on error. It is either "Encoding ..." on unsupportedCharset 
   *   or "File not able to create: ..." with the absolute path of the file.
   * @throws IOException only on closing an opened file. Not expected. 
   */
  //Sets either this.wr or this.fwr, never both.
  public String openZip(File file, String sPathInZip, String encoding, Appendable buffer) throws IOException {
    this.twr = buffer;
    if(encoding !=null) { setEncoding(encoding); }
    else if(this.sEncoding == null) { setEncoding("UTF-8"); }
    try{
      if(this.wr !=null) { this.wr.close(); }
      else if(this.fwr !=null) { this.fwr.close(); }
      this.wr = null; this.fwr = null;
      if(file !=null) {
        this.fileSystem = ZipUtils.openZip(file);
        Path nf = this.fileSystem.getPath(sPathInZip);
        this.wr = Files.newBufferedWriter(nf, this.encoding);
        assert(this.fwr ==null);
      }
      return null;
    } catch(UnsupportedCharsetException exc) {
      return "Encoding faulty: " + exc.getMessage();
    } catch (FileNotFoundException e) {
      return "File not able to create: " + file.getAbsolutePath();
    }
  }
  
  
  
  /**Writes closing elementsParent if necessary, then close the files.
   * @throws IOException
   */
  public void close() throws IOException {
    while(this.elementCurr !=null) {
      writeElementEnd();
    }
    wrNewline();
    fileclose();
    if(this.fileSystem !=null) {
      this.fileSystem.close();
    }
  }
  
  
  public void flush() throws IOException {
    if(this.wr !=null) {
      this.wr.flush();    //flushes fwr too because used in wr
    } else if(this.fwr !=null) {
      this.fwr.flush();
    }
  }

  public void fileclose() {
    try { 
      if(this.wr !=null) {
        this.wr.close();   //flush and close fwr too
        this.wr = null;
        assert(this.fwr == null);
      }
      else if(this.fwr !=null) {
        if(this.fwr instanceof ZipOutputStream) {
          ZipUtils.closeZipEntry(this.fwr);
        } else {
          this.fwr.close();   //flush and close fwr too
        }
       this.fwr.close();
       this.fwr = null;
       assert( this.wr == null);
      }
      this.twr = null;
    } catch(IOException exc) {
      String err = ExcUtil.exceptionInfo("XmlSeqWriter problem on close", exc, 1, 99).toString();
      System.err.println(err);
    }
  }





  /**Sets an output for text output of the created XML. 
   * If {@link #open(File, String)} is not used respectively {@link #close()} is called before, 
   * the XML output is written only to this Appendable. 
   * If {@link #open(File, String)} is used too, the XML output is written twice, to this Appendable too.
   * The Appendable instance can used outside of this XML write operations in any kind, it can be delete etc.
   * Hence it may be used as debug check. 
   * @param out if null then this output is disabled.
   */
  public void setDebugTextOut(Appendable out) {
    this.twr = out;
  }
  
  
  /**Switch the mode for replacing a \n in texts or attribute values.
   * @param bReplace true, then do not output a \n (line break), instead output &#x0A;
   *   false then break the line with \n if it occurs in texts and attribute values.
   */
  public void replaceNewline(boolean bReplace) {
    if(bReplace) {
      this.replaceBasics = "&\"'<>\t\r\n";
    }
    else {
      this.replaceBasics = "&\"'<>\t\r";   //write \n as is.
    }
  }
  
  
  
  /**
   * @throws IOException
   */
  private void writeHead() throws IOException {
    wrTxtAscii("<?xml version=\"1.0\" encoding=\"" + this.encoding.name() + "\"?>\n");
  }
  
  public void writeComment() {
    
  }
  
  
  /**Writes a new Element with its tag in a new line with incremented indentation.
   * The element is not closed with >, because attributes should be written.
   * See also {@link #writeElementInline(String)}.
   * It is a child node of a non closed {@link #writeElement(String)} before.
   * After this call firstly {@link #writeAttribute(String, String)} can be written,
   * then {@link #writeElement(String)} for child nodes or {@link #writeText()}
   * @param sTag
   * @throws IOException
   */
  public void writeElement(String sTag) throws IOException {
    if(this.bElementStart) { //finish the parent element start
      writeElementHeadEnd(true);
    }
    if(this.elementCurr == null) {
      writeHead();
    }
    else { //if(this.elementCurr.bIndented) { //null on root
      this.elementCurr.bIndented = true;
      wrNewline();
    }
    wrTxtAscii("<"); wrTxt(sTag);
    if(this.elementCurr !=null) { this.elementsParent.push(this.elementCurr); }
    this.elementCurr = new ElementInfo(sTag, this.elementCurr);
    this.bElementStart = true;
    //increase indent anyway, may be not used.
    if((this.indent +=2) > sIndent.length()) { this.indent = sIndent.length(); }

  }
  
  
  
  
  /**Writes a new Element with its tag in the line without line break and change or indentation.
   * The element is not closed with >, because attributes should be written.  
   * It is a child node of a non closed {@link #writeElement(String)} before.
   * After this call firstly {@link #writeAttribute(String, String)} can be written,
   * then {@link #writeElement(String)} for child nodes or {@link #writeText()}
   * <br><br>
   * Writing an element in the line, without newline, is sometimes necessary for systems
   * which reads the text inside the elements. 
   * In XML it seems to be not absolute clear, when a newline has a meaning for text content or not.
   * Usual between elements, if no text is there, only the line feed and indentation, 
   * this is ignored by reading the XML content.
   * But it seems to be some programs does not so, (Libre Office odt content.xml?)
   * They seems to interpret an new line as a space. 
   * That's why all stuff from one text paragraph is written in one line.
   * Line breaks are only introduced between the paragraphs. 
   * This is a seen, not explained behavior.
   * This operation helps to build one line for a defined XML content. 
   * 
   * @param sTag
   * @throws IOException
   */
  public void writeElementInline(String sTag) throws IOException {
    if(this.bElementStart) { //finish the parent element start
      writeElementHeadEnd(true);
    }
    wrTxtAscii("<"); wrTxt(sTag);
    if(this.elementCurr !=null) { this.elementsParent.push(this.elementCurr); }
    this.elementCurr = new ElementInfo(sTag, this.elementCurr);
    this.bElementStart = true;
  }
  
  
  
  
//  public void writeNamespace(String namespace, String key) throws IOException {
//    writeAttribute("xmlns:key", namespace);
//    if(this.elementCurr.nameSpaces == null) { this.elementCurr.nameSpaces = new TreeMap<String, String>(); }
//    this.elementCurr.nameSpaces.put(key, namespace);
//  }
  
  
  private void wrNewline() throws IOException {
    wrTxtAscii(sIndent.substring(0, this.indent));
    this.nColumn = this.indent;
  }
  
  
  
  /**Writes a simple end ">" because child nodes may be added to the element.
   * @throws IOException 
   * 
   */
  public void writeElementHeadEnd(boolean bNewline) throws IOException {
    if(!this.bElementStart) throw new IllegalStateException("not in writing head");
    this.bElementStart = false;
    wrTxtAscii(">");
    this.elementCurr.bIndented = bNewline;
  }
  
  
  
  /**Writes the </tag> or .../> in the same line as the opened tag
   * or in a new line, depending on written child elements.
   * @param sTag should correspond with the open tag.
   * @throws IOException
   * @throws IllegalStateException on tag mismatch.
   */
  public void writeElementEnd(String sTag) throws IOException {
    writeElementEnd(sTag, true);
  }

  /**Writes the </tag> or .../> but checks whether this is the correct tag.
   * @param sTag should correspond with the open tag.
   * @param bNewline true then writes the </tag> in a newline, 
   *   but not if the element was created with {@link #writeElementInline(String)}.  
   * @throws IOException
   * @throws IllegalStateException on tag mismatch.
   */
  private void writeElementEnd(String sTag, boolean bNewline) throws IOException {
    if(!this.elementCurr.sTag.equals(sTag)) {
      throw new IllegalStateException("element mismatch, current = " + this.elementCurr.sTag + " != requ = " + sTag );
    }
    writeElementEnd(bNewline);
  }

  
  
  /**Writes the </tag> or .../> anyway without newline, independent of the tree situation.
   * This can be used for special cases to prevent interpretation of a newline from the evaluation of the XML.
   * @param sTag should correspond with the open tag.
   * @throws IOException
   * @throws IllegalStateException on tag mismatch.
   */
  public void writeElementEndInline(String sTag) throws IOException {
    writeElementEnd(sTag, false);
  }
  
  
  /**Writes a simple end "</tag>" or "/>" depending on invocation of {@link #writeElementHeadEnd()} before
   * @throws IOException 
   * 
   */
  public void writeElementEnd () throws IOException {
    writeElementEnd(true);
  }

  /**Writes a simple end "</tag>" or "/>" depending on invocation of {@link #writeElementHeadEnd()} before
   * @throws IOException 
   * 
   */
  private void writeElementEnd( boolean bNewline) throws IOException {
    //decrease indent anyway
    if((this.indent -=2) < 1) { this.indent = 1; }
    if(this.bElementStart) {
      wrTxtAscii("/");            //writes "/>" with newline-indent
      writeElementHeadEnd(true);
      this.elementCurr.bIndented = true;
      this.bElementStart = false;
    } else {
      if( bNewline 
       && this.elementCurr.bIndented) {        // bIndented is false if the element was written with writeElementInline(...)
        wrNewline();
      }
      wrTxtAscii("</"); wrTxt(this.elementCurr.sTag); wrTxtAscii(">");
      if(this.bTreeComment) {
        wrTxtAscii("<!--");
        for(ElementInfo elem: this.elementsParent) {
          wrTxtAscii(elem.sTag); wrTxtAscii(" = ");
        }
        wrTxtAscii("-->");
      }
    }
    if(this.elementsParent.size() >0 ) { this.elementCurr = this.elementsParent.pop(); }
    else { this.elementCurr = null; } //after the root.
  }
  

  
  public void writeAttribute(String name, String value) throws IOException {
    if(!this.bElementStart) {
      throw new IllegalStateException("should be written not after content");
    }
    wrTxtAscii(" "); wrTxt(name); wrTxtAscii("=\""); wrTxt(value); wrTxtAscii("\"");
  }
  

  public void writeText(CharSequence txt, boolean bNewline) throws IOException {
    String txt1 = txt.toString();
    if(bNewline && txt1.startsWith(" ") || txt1.endsWith(" ")) {
      Debugutil.stop();
    }
    if(this.bElementStart) { //finish the parent element start
      writeElementHeadEnd(true);
    }
    this.elementCurr.bIndented = bNewline;
    if(this.elementCurr.bIndented) {
      wrNewline();
    }
    wrTxt(txt);  //TODO long text: line break
  }
  
  
  
  private static String[] stdReplacement =                   //newline character on end!
    { "&amp;", "&quot;", "&apos;", "&lt;", "&gt;", "&#x09;", "&#x0d;", "&#x0a;"};
  
  
  
  /**Writes text with all replacements.
   * @param txt
   * @throws IOException
   */
  private void wrTxt(CharSequence txt) throws IOException {
    CharSequence txt1 = txt == null? "?null?" : txt;
    
    int begin = 0;
    int endMax = txt1.length();
    int posCorr;
    int[] nr = new int[1];
    while( ( posCorr= StringFunctions.indexOfAnyChar(txt1, begin, endMax, this.replaceBasics, nr)) >=0) {
      if(begin < posCorr) { 
        wrTxtEncoded(txt1.subSequence(begin, posCorr));
      }
      wrTxtAscii(stdReplacement[nr[0]]);
      begin = posCorr +1;
    }
    if(begin < endMax) {
      wrTxtEncoded(txt1.subSequence(begin, endMax));
    }
  }
  
  

  
  
  /**Write a text, using {@link #encoding2} to check write character immediately
   * or as &#x0123; for non encode able character. 
   * All character which are defined in the 
   * @param txt
   * @throws IOException
   */
  private void wrTxtEncoded(CharSequence txt) throws IOException {
    //this.encoding.
    
    if(this.wr !=null) {               //Writes with given encoding. 
      this.wr.write(txt.toString());   // writes with encoding to fwr.
      if(this.twr !=null) {
        this.twr.append(txt);
      }
    } else if(this.fwr !=null) { //writes Byte code
      assert(this.encodingCode !=null);
      int zChars = txt.length();
      for(int ix = 0; ix < zChars; ++ix) {
        char cc = txt.charAt(ix);
        int b = this.encodingCode.getCode(cc);      
        if(b !=0) {                    // char is mapped, write it.
          this.fwr.write(b);
          if(this.twr !=null) {
            this.twr.append(cc);
          }
        } else {                        // if not possible to map in given encoding:
          String hex = Integer.toHexString(cc).toUpperCase();
          wrTxtAscii("&#x");            // hex is the code point, not the bytes of UTF8. It is the value of UTF-16
          if((hex.length() & 1) ==1) {
            wrTxtAscii("0");            // leading 0 for 2, 4, 6, 8 character
          }
          wrTxtAscii(hex);
          wrTxtAscii(";");
        }
      }
    } else if(this.twr !=null) { //all other are null, output to twr at least
      this.twr.append(txt);
    }
  }

  
  
  
//  private void XXXwrTxt(String txt) throws IOException {
//    //this.encoding.
//    int z = txt.length();
//    
//    if(this.wr !=null) {
//      wr.append(txt);
//    }
//    if(this.fwr !=null) {
//      if(this.bFullEncoding) {
//        
//      }
//      else {
//        //check whether characters should be subscribed
//        ByteBuffer bu = this.encoding.encode(txt);
//        int ix0 = 0;
//        int ix = 0;
//        while(bu.remaining() >0 && (ix - ix0) < this.outBuffer.length) {
//          if((ix - ix0 >= this.outBuffer.length)) {
//            //no special characters found, output the current.
//            bu.get(this.outBuffer, 0, this.outBuffer.length);
//            this.fwr.write(this.outBuffer, 0, this.outBuffer.length);
//            ix0 = ix+1;
//          } else {
//            byte b = bu.get();
//            switch(b) {
//            case 0x3f: { //the "?" maybe unsupported
//              char cc = txt.charAt(ix0 + ix);
//              if(cc != '?') {
//                int zOut = ix - ix0;
//                this.fwr.write(0x3f);
//                this.fwr.write(0x21);
//                this.fwr.write(0x3f);
//                ix0 = ix+1;
//              }
//            } break;
//            default: {
//              fwr.write(b);
//            }
//            } //switch
//          }
//          ix += 1;
//        }
//      }
//    }
//  }
//  
  
  
  
  
  
  /**Write as given, only ASCII (7 bit) especially for XML head.
   * @param txt
   * @throws IOException
   */
  private void wrTxtAscii(String txt) throws IOException {
    //this.encoding.
    
    if(this.twr !=null) {
      this.twr.append(txt);
    }
    if(this.wr !=null) {
      this.wr.write(txt);  //writes with encoding to fwr.
    } else if(this.fwr !=null) { //writes ASCII
      int zChars = txt.length();
      for(int ix = 0; ix < zChars; ++ix) {
        this.fwr.write(((byte)(txt.charAt(ix))));
      }
    } 
  }
  
  
  @Override public String toString() {
    if(this.elementCurr !=null) return this.elementCurr.toString();
    else return "closed or root";
  }
  
  

}
