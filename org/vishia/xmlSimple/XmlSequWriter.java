package org.vishia.xmlSimple;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Stack;

import org.vishia.charset.CodeCharset;
import org.vishia.util.DataAccess;
import org.vishia.util.StringFunctions;


/**This is a XML writer which does not store anything. It writes only.
 * The usage is seen in the test files, {@link #testWritetree2(File, StringBuilder)} etc with following template:
 * <pre>
    open(file, "US-ASCII", out);
    writeElement("root");
      writeText("contentRoot", true);
    writeElement("child1");
      writeAttribute("attr", "value");
      writeText("contentChild &lt;���> special chars", false);
      writeElementEnd();
    writeElement("child2");
      writeAttribute("attr2", "text � special chars");
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

  
  private class ElementInfo{
    String sTag;
    boolean bIndented;
    
    public ElementInfo(String sTag) {
      super();
      this.sTag = sTag;
    }
  }
  
  
  
  
//  private class XmlEncoder extends CharsetEncoder {
//
//    protected XmlEncoder(Charset cs) {
//      super(cs, 1.4f, 20.0f);
//      // TODO Auto-generated constructor stub
//    }
//
//    @Override protected CoderResult encodeLoop(CharBuffer in, ByteBuffer out) {
//      
//      CoderResult res = super.encodeLoop(in, out);
//      
//      return res;
//    }
//    
//  }
  
  
  
  
  
  
  private Writer wr;
  
  private OutputStream fwr;
  
  private Appendable twr;
  
  
  
  private byte[] outBuffer;
  
  String sEncoding;
  
//  CharsetEncoder encoding;
  Charset encoding;
  
  CodeCharset encoding2;
  
  /**true then no reason to output transcription characters. */
  boolean bFullEncoding;
   
  boolean bElementStart;
  
  int nColumn;
  
  int nColumnMax = 88;
  
  /**Info of the current element, only {@link ElementInfo#bIndented} should be accessed. */
  ElementInfo info;
  
  Stack<ElementInfo> tags = new Stack<ElementInfo>();
  
  int indent = 1;
  
  public boolean bTreeComment = true;
  
  private static String sIndent = "\n                                                                                ";
  

  
  
  public void setEncoding(String sEncoding) throws IllegalCharsetNameException {
    if(sEncoding.startsWith("UTF-")) {
      this.encoding2 = null;
      this.encoding = Charset.forName(sEncoding);
    } else {
      this.encoding = null;
      this.encoding2 = CodeCharset.forName(sEncoding);
      if(this.encoding2 == null) throw new IllegalCharsetNameException("Illegal encoding id: " + sEncoding);
      
    }
    this.sEncoding = sEncoding; //not reached because exception on faulty identifier.
  }
  
  
  
  
  
  /**Opens for writing in a file, closes an opened file if any is open.
   * @param file can be null, then only the buffer is written.
   * @param encoding can be null, then default "UTF-8" or the last used one, 
   *   should be usually "UTF-8" or one of "ISO-8859-x" x=1... or "US-ASCII"
   * @param buffer can be null, if given, the output is written there, the same as in {@link #setDebugTextOut(Appendable)}  
   * @return null on success, the error on error. It is either "Encoding ..." on unsupportedCharset 
   *   or "File not able to create: ..." with the absolute path of the file.
   * @throws IOException only on closing an oped file. Not expected. 
   */
  public String open(File file, String encoding, Appendable buffer) throws IOException {
    this.twr = buffer;
    try{
      if(encoding !=null) { setEncoding(encoding); }
      else if(this.sEncoding == null) { setEncoding("UTF-8"); }
      
      if(file !=null) {
        OutputStream fwr = new FileOutputStream(file);
        this.fwr = fwr;
        if(this.encoding !=null) {
          this.wr = new OutputStreamWriter(fwr, this.encoding); 
        } else {
          //open(fwr, encoding);
          this.wr = null;
        }
      }
      return null;
    } catch(UnsupportedCharsetException exc) {
      return "Encoding faulty: " + exc.getMessage();
    } catch (FileNotFoundException e) {
      return "File not able to create: " + file.getAbsolutePath();
    }
  }
  
  
  
  /**Writes closing tags if necessary, then close the files.
   * @throws IOException
   */
  public void close() throws IOException {
    while(this.info !=null) {
      writeElementEnd();
    }
    wrNewline(0);
    if(this.wr !=null) {
      this.wr.close();   //flush and close fwr too
      this.wr = null;
      this.fwr = null;
    }
    else if(this.fwr !=null) {
      this.fwr.close();
      this.fwr = null;
      this.wr = null;
    }
    this.twr = null;
  }
  
  
  public void flush() throws IOException {
    if(this.wr !=null) {
      this.wr.flush();    //flushes fwr too because used in wr
    } else if(this.fwr !=null) {
      this.fwr.flush();
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
  
  
  /**
   * @throws IOException
   */
  private void writeHead() throws IOException {
    wrTxtAscii("<?xml version=\"1.0\" encoding=\"" + this.sEncoding + "\"?>\n");
  }
  
  public void writeComment() {
    
  }
  
  
  /**Writes a new Element with its tag, without closing the element. 
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
    if(this.info == null) {
      writeHead();
    }
    else { //if(this.info.bIndented) { //null on root
      this.info.bIndented = true;
      wrNewline(1);
    }
    wrTxtAscii("<"); wrTxt(sTag);
    if(this.info !=null) { this.tags.push(this.info); }
    this.info = new ElementInfo(sTag);
    this.bElementStart = true;
    //increase indent anyway, may be not used.
    if((this.indent +=2) > sIndent.length()) { this.indent = sIndent.length(); }

  }
  
  
  private void wrNewline(int add) throws IOException {
//    if(add >0) {
//      if((this.indent +=2) > sIndent.length()) { this.indent = sIndent.length(); }
//    } else if(add <0) {
//      if((this.indent -=2) < 1) { this.indent = 1; }
//    }
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
    this.info.bIndented = bNewline;
  }
  
  /**Writes a simple end "</tag>" or "/>" depending on invocation of {@link #writeElementHeadEnd()} before
   * @throws IOException 
   * 
   */
  public void writeElementEnd() throws IOException {
    //decrease indent anyway
    if((this.indent -=2) < 1) { this.indent = 1; }
    if(this.bElementStart) {
      wrTxtAscii("/");            //writes "/>" with newline-indent
      writeElementHeadEnd(true);
      this.info.bIndented = true;
      this.bElementStart = false;
    } else {
      if(this.info.bIndented) {
        wrNewline(-1);
      }
      wrTxtAscii("</"); wrTxt(this.info.sTag); wrTxtAscii(">");
      if(this.bTreeComment) {
        wrTxtAscii("<!--");
        for(ElementInfo elem: this.tags) {
          wrTxtAscii(elem.sTag); wrTxtAscii(" = ");
        }
        wrTxtAscii("-->");
      }
    }
    if(this.tags.size() >0 ) { this.info = this.tags.pop(); }
    else { this.info = null; } //after the root.
  }
  
  
  public void writeAttribute(String name, String value) throws IOException {
    if(!this.bElementStart) throw new IllegalStateException("should be written not after content");
    wrTxtAscii(" "); wrTxt(name); wrTxtAscii("=\""); wrTxt(value); wrTxtAscii("\"");
  }
  

  public void writeText(CharSequence txt, boolean bNewline) throws IOException {
    if(this.bElementStart) { //finish the parent element start
      writeElementHeadEnd(true);
    }
    this.info.bIndented = bNewline;
    if(this.info.bIndented) {
      wrNewline(0);
    }
    wrTxt(txt);  //TODO long text: line break
  }
  
  
  
  private static String[] stdReplacement = 
    { "&amp;", "&quot;", "&apos;", "&lt;", "&gt;", "&#x0a;", "&#x0d;", "&#x09;"};
  
  
  
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
    while( ( posCorr= StringFunctions.indexOfAnyChar(txt1, begin, endMax, "&\"'<>\n\r\t", nr)) >=0) {
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
  
  

  
  
  /**Write as given, only ASCII (7 bit) especially for XML syntax.
   * @param txt
   * @throws IOException
   */
  private void wrTxtEncoded(CharSequence txt) throws IOException {
    //this.encoding.
    
    if(this.wr !=null) {
      this.wr.write(txt.toString());  //writes with encoding to fwr.
      if(this.twr !=null) {
        this.twr.append(txt);
      }
    } else if(this.fwr !=null) { //writes ASCII
      assert(this.encoding2 !=null);
      int zChars = txt.length();
      for(int ix = 0; ix < zChars; ++ix) {
        char cc = txt.charAt(ix);
        int b = this.encoding2.getCode(cc);
        if(b !=0) {
          this.fwr.write(b);
          if(this.twr !=null) {
            this.twr.append(cc);
          }
        } else {
          String hex = "&#x" + Integer.toHexString(cc) + ";";
          wrTxtAscii(hex);
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
  
  
  
  
  
  /**Write as given, only ASCII (7 bit) especially for XML syntax.
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
  
  
  
  
  
  public static void main(String[] args) {
    File dir = new File(args.length == 0 ? "T:/" : args[0]);
    StringBuilder testout = new StringBuilder();
    XmlSequWriter thiz = new XmlSequWriter();
    thiz.setEncoding("UTF-8");
    try {
      thiz.testWritetree2(new File(dir, "testWritetree2.xml"), testout);
      thiz.testWritetree2t(new File(dir, "testWritetree2t.xml"), testout);
      thiz.testWritetree2at(new File(dir, "testWritetree2at.xml"), testout);
      thiz.testWriteEncodingFile(new File(dir, "testWriteEncodingFile.xml"), testout);
    } catch(Exception exc) {
      System.err.println(exc.getMessage());
    }
  }

  
  
  
  private void testWritetree2(File file, StringBuilder out) throws IOException {
    String sCmp = 
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + 
        "<root>contentRoot\n" + 
        "  <child1/>\n" + 
        "</root>\n" + 
        "";
    out.setLength(0);
    open(file, null, out);
    writeElement("root");
    writeText("contentRoot", false);
    writeElement("child1");  //without content
    close();
    if(!sCmp.equals(out.toString())) {
      throw new IllegalArgumentException("faulty= testWriteTree");
    }
  }
  

  private void testWritetree2t(File file, StringBuilder out) throws IOException {
    String sCmp = 
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + 
        "<root>\n" + 
        "  contentRoot\n" + 
        "  <child1>contentChild</child1>\n" + 
        "</root>\n" + 
        "";
    out.setLength(0);
    open(file, null, out);
    writeElement("root");
    writeText("contentRoot", true);
    writeElement("child1");
    writeText("contentChild", false);
    close();
    if(!sCmp.equals(out.toString())) {
      throw new IllegalArgumentException("faulty= testWriteTree");
    }
  }
  

  private void testWritetree2at(File file, StringBuilder out) throws IOException {
    String sCmp = 
        "<?xml version=\"1.0\" encoding=\"US-ASCII\"?>\n" + 
        "<root>\n" + 
        "  contentRoot\n" + 
        "  <child1 attr=\"value\">\n" + 
        "    contentChild\n" + 
        "  </child1>\n" + 
        "</root>\n" + 
        "";
    out.setLength(0);
    open(file, "US-ASCII", out);
    writeElement("root");
    writeText("contentRoot", true);
    writeElement("child1");
    writeAttribute("attr", "value");
    writeText("contentChild", true);
    close();
    if(!sCmp.equals(out.toString())) {
      throw new IllegalArgumentException("faulty= testWriteTree");
    }
  }
  
  
  
  private void testWriteEncodingFile(File file, StringBuilder out) throws IOException {
    String sCmp = 
        "<?xml version=\"1.0\" encoding=\"US-ASCII\"?>\n" + 
        "<root>\n" + 
        "  contentRoot\n" + 
        "  <child1 attr=\"value\">contentChild &lt;&#xc4;&#xdc;&#xd6;&gt; special chars</child1>\n" + 
        "  <child2 attr2=\"text &#x20ac; special chars\"/>\n" + 
        "</root>\n" + 
        "";
    out.setLength(0);
    setEncoding("US-ASCII");
    open(file, "US-ASCII", out);
    writeElement("root");
      writeText("contentRoot", true);
    writeElement("child1");
      writeAttribute("attr", "value");
      writeText("contentChild <���> special chars", false);
      writeElementEnd();
    writeElement("child2");
      writeAttribute("attr2", "text � special chars");
    close();
    if(!sCmp.equals(out.toString())) {
      throw new IllegalArgumentException("faulty= testWriteTree");
    }
  }
  

}
