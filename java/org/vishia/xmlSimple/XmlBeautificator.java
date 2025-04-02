package org.vishia.xmlSimple;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Arrays;
import java.util.Stack;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.vishia.util.Debugutil;
import org.vishia.util.StringArray;
import org.vishia.util.StringFunctions;

/**This class presents a ready to use tool to beautify the view to XML files.
 * Sometimes XML files are packed in zip and packed in one line to save storage.
 * Then the content is hard readable.
 * This file reads an XML file, shows for '<' etc. to detect the structure
 * and writes it out with indentation and newline.
 * 
 * @author Hartmut Schorrig
 * @date 2023-04
 * 
 * Hint: This solution is not complete. It was developed, and then the {@link org.vishia.xmlReader.XmlJzReader}
 * was used, and it was seen it outputs also a beautification result, exact that which was read.
 * A little bit, it can be used to see how to deal with XML.
 * 
 * This is more a common universal solution without reading an XML file, but has yet some small errors.
 * This comment was written on 2025-04, not used in the past from 2023/24....
 *
 */
public class XmlBeautificator {

  
  int len = 16384;
  char[] buffer = new char[len];
  int pos = 0;
  int end = 0;
  boolean bEof = false;
  File fin, fout;
  
  /**If not null the zip file is opened and should be closed.*/
  ZipFile zipfin;

  ZipEntry zipEntry;
  
  
  /**If not null fin is a zip file, read this content. */
  String sPathInZip;
  
  Reader rin;
  Writer wout;
  String sEncoding = "UTF-8";
  
  final String sWhitespaces = " \n\r\t";
  
  /**Indent for the next newline */
  int indent = 1;                         // outputs at leas '\n'
  
  /**Newline chars, starts with '\n' then spaces. */
  char[] sNewline = new char[200];{
    Arrays.fill(this.sNewline, ' ');
    this.sNewline[0] = '\n';
  }
  
  final CharSequence csBuffer = new CharSequence() {
    @Override public int length () {
      return XmlBeautificator.this.end - XmlBeautificator.this.pos;
    }

    @Override public char charAt ( int index ) {
      return XmlBeautificator.this.buffer[XmlBeautificator.this.pos + index];
    }

    @Override public CharSequence subSequence ( int start, int end ) {
      return new StringArray(XmlBeautificator.this.buffer, XmlBeautificator.this.pos + start, XmlBeautificator.this.pos + end);
    }

    @Override public String toString() {
      int len = length();
      if(len > 40) { len = 40;}
      return new String(XmlBeautificator.this.buffer, XmlBeautificator.this.pos, len);
    }
    
  };
  
  Stack<String> tags = new Stack<String>();
  
  /**This is the main routine.
   * It opens first the input and calls {@link #evaluateStartTag()}.
   * If the encoding (contained in first element <!... encoding="..."!>) is not UTF-8 the input will be closed
   * and opened again, done in {@link #evaluateStartTag()}. 
   * <br>
   * Then it creates the output with the same encoding as the input. 
   * Then it calls {@link #evaluateElement(int)} for the whole only one element of the top level.
   * All other is recursively.
   * 
   * @param args see {@link #main(String[])}
   */
  private void rw(String[] args) {
    String sIn = args[0];
    int posZip = sIn.indexOf(":zip:");
    if(posZip >=0) {
      this.fin = new File(sIn.substring(0, posZip));
      this.sPathInZip = sIn.substring(posZip+5);
    } else {
      this.fin = new File(args[0]);
    }
    this.fout = new File(args[1]);
    try {
      openInput();
      read();
      evaluateStartTag();                        // may close the rin and reopen on other encoding
      this.wout = new OutputStreamWriter(new FileOutputStream(this.fout), this.sEncoding);
      this.wout.write(this.buffer, 0, this.pos); // writes the <!...!> as given.
      writeNewline();
      do {
        evaluateElement(0);
      } while(this.pos < this.end);
    } 
    catch(Exception exc) {
      System.out.println(exc.toString());
    }
    finally {
      if(this.rin !=null) try{ this.rin.close(); this.rin = null; } catch(IOException exc) { System.err.println("exception on close: ");}
      if(this.zipfin !=null) try{ this.zipfin.close(); this.zipfin = null; } catch(IOException exc) { System.err.println("exception on close zip: ");}
      if(this.wout !=null) try{ this.wout.close(); this.wout = null; } catch(IOException exc) { System.err.println("exception on close out: ");}
    }
  }
  
  
  
  
  void openInput ( ) throws IOException {
    if(this.sPathInZip == null) {
      this.rin = new InputStreamReader(new FileInputStream(this.fin), this.sEncoding);
    } else {
      if(this.zipfin ==null) {
        this.zipfin = new ZipFile(this.fin);
        this.zipEntry = this.zipfin.getEntry(this.sPathInZip);
      }
      InputStream zInput = this.zipfin.getInputStream(this.zipEntry);
      this.rin = new InputStreamReader(zInput, this.sEncoding);
    }
    this.pos = this.end = 0;
  }
  
  /**Reads first the content with {@link #len} character. 
   * It looks for "encoding=\"". It his is not identically with the initial {@link #sEncoding} ("UTF-8")
   * then it closes the input file and reopens it with the given encoding. 
   * <br>
   * The XML file should start immediately with "<?", it is asserted.
   * The position will be set after the "?>" before return.
   * @throws IOException
   */
  void evaluateStartTag() throws IOException{
    CharSequence cs = new StringArray(this.buffer, this.pos, this.end);
    assert(StringFunctions.startsWith(cs, "<?"));
    int posEncoding = StringFunctions.indexOf(cs, "encoding=\"") +10;
    if(posEncoding >=10) {
      int posEndEncoding = StringFunctions.indexOf(cs, '\"', posEncoding);
      String sEncoding = new String(this.buffer, posEncoding, posEndEncoding - posEncoding);
      if(!this.sEncoding.equals(sEncoding)) {
        this.sEncoding = sEncoding;
        this.rin.close();
        openInput();
        read();
      }
    }
    this.pos = StringFunctions.indexOf(cs, "?>") + 2;
  }
  
  
  
  /**Reads a part from the {@link #rin} input file if the buffer {@link #pos} is in the second half.
   * Then also it shifts to content in buffer, {@link #pos} = 0.
   * <br>
   * If {@link #pos} is in the first half, nothing is done. 
   * As effect at least the half buffer is filled and evaluatable  from {@link #pos} to {@link #end}
   * @throws IOException
   */
  private void read ( ) throws IOException {
    if(!this.bEof && this.pos >= this.len/2 || this.pos ==0 && this.end ==0) {
      int ix0 = 0;
      for(int ix = this.pos; ix < this.end; ++ix) {
        this.buffer[ix0++] = this.buffer[ix];
      }
      this.end -= this.pos;
      this.pos = 0;
      int nrRead = this.rin.read(this.buffer, this.end, this.buffer.length - this.end);
      if(!(this.bEof = nrRead <0)) {
        this.end += nrRead;
      }
    }
  }
  
  
  /**Evaluate one element and recursively all nested ones.
   * @param recursion
   * @throws IOException
   */
  private void evaluateElement(int recursion) throws IOException {
    read();
    if(recursion > 100) {
      throw new IllegalArgumentException("recursion >100");
    }
    char cc;
    do {                                         // skip till "<", there should be only white spaces.
      cc = this.buffer[this.pos];
      if(cc != '<') {
        assert("\n\r\t ".indexOf(cc) >=0);
        this.pos +=1;                       // skip over white spaces.
      }
    } while(cc != '<' && this.pos < this.end);
    if( this.pos >= this.end) {
      return;
    }
    //----------------------------------------------------- <tag ....
    int posStart = this.pos;
    while( "\n\r\t />".indexOf(cc = this.buffer[++this.pos])<0);  // search first separator char after tag
    String sTag = new String(this.buffer, posStart+1, this.pos - posStart-1);
    int posTagEnd = StringFunctions.indexOf(this.csBuffer, '>');  // search ...> . Note posTagEnd counts from this.pos
    char cTagEnd = this.buffer[this.pos + posTagEnd -1];    //character before '>' sometimes '/'
    this.wout.write(this.buffer, posStart, (this.pos + posTagEnd+1) - posStart);
    this.pos += posTagEnd+1;
    boolean bNested = cTagEnd != '/'; 
    if(!bNested) {
      writeNewline();                          // <tag ... /> is finished.
    } else {
      this.tags.push(sTag);                    // <tag ... > ....
      this.indent +=2;                         // 
      writeNewline();                          //   newline
      do {      
        evaluateContent();                     // CONTENT till any "<"
        if(StringFunctions.startsWith(this.csBuffer, "</")) {
          this.indent -=2;
          writeCloseTag();                     // </tag>
          writeNewline();                      // and newline
          bNested = false;
        } 
        else {
          evaluateElement(recursion +1);
        }
      } while(bNested);
    }
  }
  

  /**Writes the closing tag "</tag".
   * It checks also whether the closing tag is correct to the open tag.
   * Hence it checks the XML integrity for that.
   * Writes a error message it it is faulty. 
   * @throws IOException
   */
  private void writeCloseTag () throws IOException {
    assert(this.indent >=1);
    int end1 = StringFunctions.indexOf(this.csBuffer, '>');
    CharSequence sEndTag = this.csBuffer.subSequence(2,  end1);
    String sTag = this.tags.pop();
    if(StringFunctions.compare(sEndTag, sTag) !=0) {
      System.out.printf("tag mismatching: %s != %s\n", sTag, sEndTag.toString());
    }
    this.wout.write(this.buffer, this.pos, end1+1);
    this.pos += end1+1;
    
  }
  
  
  
  /**Looks for textual content between tag elements and writes the close </tag> if found. 
   * If only white spaces are given between <tag ---elements, they are ignored and replaced by the beautification.
   *  
   * @param bNestedBefore true if an element <tag...> is opened but not closed. 
   * @return false if the closing it has been detected and written. Else value of bNestedBefore. 
   * @throws IOException
   */
  private boolean evaluateContent () throws IOException {
    read();
    int posStart = this.pos;                     // posNextElement on beginning <
    int posNextElement = StringFunctions.indexOf(this.csBuffer, '<') + this.pos;
    boolean contentFound = false;
    this.pos = posNextElement;                   // check whether there are only spaces or content
    for(int ix = posStart; ix < posNextElement; ++ix) {
      char cc = this.buffer[ix];
      if(this.sWhitespaces.indexOf(cc) <0) {     // content found:
        contentFound = true; //-------------------- write found content inclusively leading and trailing spaces 
        this.wout.write(this.buffer, posStart, posNextElement - posStart);
        break;
      }
    }
    return contentFound;
  }
  
  
  private void writeNewline() throws IOException {
    this.wout.write(this.sNewline,0, this.indent);
    this.wout.flush();
  }
  
  
  
  /**Calls the beautification from command line.
   * @param args First argument is path/to/input.xml
   * Second argument is path/to/output.xml
   * 
   */
  public static void main(String[] args) {
    XmlBeautificator thiz = new XmlBeautificator();
    thiz.rw(args);
  }
}
