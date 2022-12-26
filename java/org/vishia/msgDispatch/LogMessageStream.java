package org.vishia.msgDispatch;

import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.Flushable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.vishia.bridgeC.OS_TimeStamp;
import org.vishia.bridgeC.Va_list;
import org.vishia.util.ExcUtil;

/**This class adapts a given stream output channel to the LogMessage interface to output messages for example
 * to the System.out or System.err but also to an ordinary file.
 * In 2022-09 it is enhanced with a second channel to output twice, 
 * and also with an Appendable channel as second channel to output immediately to a StringBuilder or any other Appendable destination. 
 * <br>
 * Note that Java has two concepts dealing with textual information:
 * <ul>
 * <li>All String processing stuff uses UTF16 with 2 byte character without encoding problems.
 *   UTF16 and hence the Java String processing does not support all characters which are possible in UTF-8, 
 *   but this is not an meaningful disadvantage. The advantage of UTF16 is the possible internal index calculation,
 *   any character has 2 Bytes in memory. The amount of internal memory necessary is also usual not a problem. 
 * <li>All ordinary String outputs usual written to {@link OutputStream} as also the {@link System#out} 
 *   and any other {@link PrintStream} uses bytes for Strings. The encoding depends on settings. 
 *   Often only ASCII is expected, whereas (byte)(c) is used from c, a character. 
 *   But sometimes also UTF8 is produced.<br>
 *   Outputs on operation systems often uses this approach. 
 *   A general usage of UTF16 for all outputs was tried to establish by some operation systems and tools,
 *   but it was not successfully. That is also because UTF-16 is limited for special character, and it is ineffective to store.
 *   Instead the UTF-8 was established. 
 * </ul>
 * To support both approaches Java knows both, the {@link OutputStream} and its implementations for byte-characters
 * and the {@link Writer} and its implementations for UTF-16, and also the adequate inputs.
 * <br>
 * The {@link Appendable} is a simple interface for UTF16, implemented by the StringBuilder and also for all {@link Writer}.
 * Hence the other possibility is the simple Appendable as output, which is checked whether it is Closable and Flushable for close and flush.
 * @author Hartmut Schorrig
 *
 */
public class LogMessageStream extends LogMessageBase
{
  
  
  /**Version, history and license.
   * <ul>
   * <li>2022-09-23 enhanced  with a second channel to output twice, 
   * and also with an Appendable channel as second channel to output immediately to a StringBuilder or any other Appendable destination. 
   * <li>2009-00-00 Hartmut created to output a message via LogMessage interface simple in any opened stream.  
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
   * 
   */
  public static final int version = 20220923;

  
  
  final FileDescriptor fd;
  
  final OutputStream out1, out2;
  
  final Appendable out3;
  
  /**If true than {@link #close()} is effective for all associated channels */
  final boolean closeOnClose;
  
  final Charset encoding;
  
  /**Newline-String, univeral solution not for windows especially (since 2022-09, before it was \r\n. */
  byte[] sNewLine = { '\n'};
  
  public static LogMessage create(FileDescriptor fd)
  {
    return new LogMessageStream(fd);
  }

  final private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM-dd HH:mm:ss.SSS: ");
  

  /**Associates the LogMessageStream with the named opened file.
   * Note: The file is not closed by this instance if {@link #close()} is called. 
   * @param fd
   */
  public LogMessageStream(FileDescriptor fd)
  {
    this.fd = fd; 
    this.out1 = new FileOutputStream(fd);
    this.out2 = null;
    this.out3 = null;
    this.closeOnClose = false;
    this.encoding = Charset.defaultCharset();
  }
  
  /**Associates the LogMessageStream with the named opened file.
   * Note: The file is not closed by this instance if {@link #close()} is called. 
   * @param fd
   */
  public LogMessageStream(OutputStream out)
  {
    this.out1 = out; 
    this.fd = null;
    this.out2 = null;
    this.out3 = null;
    this.closeOnClose = false;
    this.encoding = Charset.defaultCharset();
  }
  
  /**Associates upto three output channels, all gets the same information. 
   * This is usefull for example if a message should be seen on console (System.out) and also in a file
   * and also in a StringBuilder for evaluation. 
   * @param out1 any Stream, can be null, for example System.out
   * @param out2 any Stream, can be null, for example an opened {@link FileOutputStream}
   * @param out3 any Appendable, can be null, can be a {@link Closeable} too.
   * @param closeOnClose true than {@link #close()} of this instance closes all associated resources 
   * @param Charset if null use the default Charset of the JVM, used for the Stream outputs.
   *   It is recommended to set null (default Charset) if System.out is used as output channel.
   */
  public LogMessageStream(OutputStream out1, OutputStream out2, Appendable out3
      , boolean closeOnClose, Charset encoding)
  {
    this.out1 = out1; 
    this.fd = null;
    this.out2 = out2;
    this.out3 = out3;
    this.closeOnClose = closeOnClose;
    this.encoding = encoding == null ? Charset.defaultCharset() : encoding;
  }
  
  /**Sends a message. See interface.  
   * @param identNumber
   * @param creationTime
   * @param text
   * @param typeArgs Type chars, ZCBSIJFD for boolean, char, byte, short, int, long, float double. 
   *        @java2c=zeroTermString.
   * @param args see interface
   */
  @Override
  public boolean sendMsgVaList(int identNumber, OS_TimeStamp creationTime, CharSequence text, Va_list args)
  { String line = "?";
    try{
      line = dateFormat.format(creationTime) + "; " + identNumber + "; " + String.format(text.toString(), args.get());
    } catch(Exception exc){
      line = dateFormat.format(creationTime) + "; " + identNumber + "; " + text;
    }
    try{ 
      byte[] b = line.getBytes(this.encoding);
      if(this.out1 !=null) {
        this.out1.write(b); 
        this.out1.write(this.sNewLine);
      }
      if(this.out2 !=null) {
        this.out2.write(b); 
        this.out2.write(this.sNewLine);
      }
      if(this.out3 !=null) {
        this.out3.append(line).append('\n');
      }
    }
    catch(Exception exc){ 
    }
    return true;
  }

  @Override public void close()
  { 
    if(this.out3 instanceof Closeable) { 
      try { ((Closeable)this.out3).close(); }
      catch(IOException exc) { throw new RuntimeException(exc); }
    }
  }

  @Override public void flush() { 
    try{ 
      if(this.out1 !=null) { this.out1.flush();}
      if(this.out2 !=null) { this.out2.flush();}
      if(this.out3 instanceof Flushable) { ((Flushable)this.out3).flush();}
    } catch(IOException exc){}
  }

  @Override
  public boolean isOnline()
  { return true; 
  }

  @Override
  public boolean sendMsg(int identNumber, CharSequence text, Object... args) {
    CharSequence stackInfo = ExcUtil.stackInfo(" : ", 2, 1);
    String line = dateFormat.format(new Date(System.currentTimeMillis())) 
                + "; " + identNumber + "; " + String.format(text.toString(),args)
                + stackInfo;
    try{ 
      byte[] b = line.getBytes(this.encoding);
      if(this.out1 !=null) {
        this.out1.write(b); 
        //this.out1.write(this.sNewLine);
      }
      if(this.out2 !=null) {
        this.out2.write(b); 
        //this.out2.write(this.sNewLine);
      }
      if(this.out3 !=null) {
        this.out3.append(line); //.append('\n');
      }
    }
    catch(IOException exc){ }
    return true;
  }

  @Override
  public boolean sendMsgTime(int identNumber, OS_TimeStamp creationTime,
      CharSequence text, Object... args) {
    String line = dateFormat.format(creationTime) + "; " + identNumber + "; " + String.format(text.toString(), args);
    try{ 
      byte[] b = line.getBytes(this.encoding);
      if(this.out1 !=null) {
        this.out1.write(b); 
        this.out1.write(this.sNewLine);
      }
      if(this.out2 !=null) {
        this.out2.write(b); 
        this.out2.write(this.sNewLine);
      }
      if(this.out3 !=null) {
        this.out3.append(line).append('\n');
      }
    }
    catch(IOException exc){ }
    return true;
  }
  
  
  @Override public Appendable append(CharSequence csq) throws IOException {
    byte[] b = csq.toString().getBytes(this.encoding);
    if(this.out1 !=null) { this.out1.write(b); }
    if(this.out2 !=null) { this.out2.write(b); }
    if(this.out3 !=null) { this.out3.append(csq); }
    return this;
  }

  @Override public Appendable append(CharSequence csq, int start, int end) throws IOException {
    byte[] b = csq.subSequence(start, end).toString().getBytes(this.encoding);
    if(this.out1 !=null) { this.out1.write(b); }
    if(this.out2 !=null) { this.out2.write(b); }
    if(this.out3 !=null) { this.out3.append(csq, start, end); }
    return this;
  }

  @Override public Appendable append(char c) throws IOException {
    byte[] b = ("" + c).getBytes(this.encoding); 
    if(this.out1 !=null) { this.out1.write(b); }
    if(this.out2 !=null) { this.out2.write(b); }
    if(this.out3 !=null) { this.out3.append(c); }
    return this;
  }


}

