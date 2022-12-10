/****************************************************************************
 * Copyright/Copyleft:
 *
 * For this source the LGPL Lesser General Public License,
 * published by the Free Software Foundation is valid.
 * It means:
 * 1) You can use this source without any restriction for any desired purpose.
 * 2) You can redistribute copies of this source to everybody.
 * 3) Every user of this source, also the user of redistribute copies
 *    with or without payment, must accept this license for further using.
 * 4) But the LPGL is not appropriate for a whole software product,
 *    if this source is only a part of them. It means, the user
 *    must publish this part of source,
 *    but don't need to publish the whole source of the own product.
 * 5) You can study and modify (improve) this source
 *    for own using or for redistribution, but you have to license the
 *    modified sources likewise under this LGPL Lesser General Public License.
 *    You mustn't delete this Copyright/Copyleft inscription in this source file.
 *
 * @author Hartmut Schorrig: hartmut.schorrig@vishia.de, www.vishia.org
 *******************************************************************************/ 
package org.vishia.util;

import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.Flushable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

/**This class is a alternative to java.io.FileWriter. 
 * It based on java.util.OutputStreamWriter, adequate to java.io.FileWriter.
 * But in opposite to that, 
 * <ul>
 * <li>The instance of this class will be preserved also if the file is closed, 
 *   respectively the instance can be created static, without opened file.
 *   It is because usage in fast realtime systems in C via Java2C-translator.
 *   The implementation in C is written without any dynamically memory.
 * <li>On open() no exceptions are thrown, a boolean return value is used.  
 * <li>Also {@link #append(CharSequence)} is exception-free similar as in {@link System#out} ( {@link java.io.PrintStream} ).
 *   Advantage for the user: exchange both without effort of try-catch.  
 * <li>On {@link #open(String, boolean)} and {@link #open(String, String, boolean)} a path with environment variables
 *   or starting with "/tmp/" or "~" can be used.
 * </ul>  
 * @author Hartmut Schorrig
 *
 */
public class FileAppend implements Appendable, Closeable, Flushable
{
  /**Version, history and license.
   * Changes:
   * <ul>
   * <li>2022-01-17 Hartmut new: {@link #open(String, String, boolean)} with specific encoding. 
   * <li>2011 Hartmut: created
   * </ul>
   * 
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
  public final static String sVersion = "2022-01-17";

  public static class Wr implements Appendable {

    int error;
    
    OutputStreamWriter writer;
    
    public Wr(OutputStream out) {
      this.writer = new OutputStreamWriter(out);
    }
    
    public Wr(OutputStream out, String encoding) throws UnsupportedEncodingException {
      this.writer = new OutputStreamWriter(out, encoding);
    }
    
    @Override public FileAppend.Wr append(CharSequence text) {
      return append(text, 0, text.length());
    }

    @Override public FileAppend.Wr append ( CharSequence csq, int start, int end ) {
      try {
        this.writer.append(csq, start, end);
      } catch(IOException exc) {
        this.error = kWriteError;
      }
      return this;
    }

    @Override public FileAppend.Wr append ( char c ) {
      try {
        this.writer.append(c);
      } catch(IOException exc) {
        this.error = kWriteError;
      }
      return this;
    }
    
    
  };
  
  
  
  /**The OutputStreamWriter can't be used as superclass, because the file is able to open
   * only with calling constructor. 
   * Thats why all methods of OutputStreamWriter have to be wrapped here.
   */
  protected Wr writer;
  
  public static final int kFileNotFound = -1;
  
  public static final int kFileOpenError = -2;
  
  public static final int kWriteError = -3;
  
  public String sError;
  
  private int error;
  
  /**opens a file to write. 
   * Hint: You must not forget {@link #close()} on end of usage. 
   * @param filePath Path to the file. To separate folder, a slash '/' should be used.
   *        But a backslash is also accepted. 
   *        An absolute path should be start either with slash
   *        or with a one-char drive specifier, following by ':/'.
   *        The filePath can also contain environment variables due to {@link Arguments#replaceEnv(String)}.
   *        and also can start with /tmp/ or ~ for home, see {@link FileFunctions#absolutePath(String, java.io.File)}.
   * @param append If the file exists, the content written than is appended.
   * @return 0 if the file is opened, -1 if the file is not found, -2 on other conditions.
   */
  @SuppressWarnings("resource") 
  public int open(String filePath, boolean append)
  { int error = 0;
    try { 
      String fileNameRepl = Arguments.replaceEnv(filePath);
      String sName = FileFunctions.absolutePath(fileNameRepl, null); 
      this.writer = new Wr(new FileOutputStream(sName, append)); 
    }
    catch(FileNotFoundException exc)
    { error = kFileNotFound; 
    }
    catch(Exception exc)
    { error = kFileOpenError;
      this.sError = exc.getMessage(); 
    }
    return error;
  }

  /**opens a file to write. 
   * Hint: You must not forget {@link #close()} on end of usage. 
   * @param filePath Path to the file. To separate folder, a slash '/' should be used.
   *        But a backslash is also accepted. 
   *        An absolute path should be start either with slash
   *        or with a one-char drive specifier, following by ':/'.
   *        The filePath can also contain environment variables due to {@link Arguments#replaceEnv(String)}
   *        and also can start with /tmp/ or ~ for home, see {@link FileFunctions#absolutePath(String, java.io.File)}.
   * @param sEncoding character encoding for the binary file, "UTF-8" etc. 
   *   see {@link OutputStreamWriter#OutputStreamWriter(java.io.OutputStream, String)}
   * @param append If the file exists, the content written than is appended.
   * @return 0 if the file is opened, -1 if the file is not found, -2 on other conditions.
   */
  @SuppressWarnings("resource") 
  public int open(String fileName, String sEncoding, boolean append)
  { int error = 0;
    try { 
      String fileNameRepl = Arguments.replaceEnv(fileName);
      String sName = FileFunctions.absolutePath(fileNameRepl, null); 
      this.writer = new Wr(new FileOutputStream(sName, append), sEncoding); 
    }
    catch(FileNotFoundException exc)
    { error = kFileNotFound; 
    }
    catch(Exception exc)
    { error = kFileOpenError;
      this.sError = exc.getMessage(); 
    }
    return error;
  }

  
  public boolean isOpen()
  { return this.writer != null;
  }
  
  @Override public void close()
  { if(this.writer != null)
    { try{ this.writer.writer.close(); }
      catch(IOException exc)
      { //ignore it. The file would never used from here. 
      }
      this.writer = null; 
    }
  }
  
  public void write(String text) throws IOException
  { if(this.writer == null) throw new IOException("file isn't opend");
    this.writer.append(text);
  }
  
  /**In opposite to the original {@link Writer#append(CharSequence)}
   * this operation does not throw. Instead it does nothing on error,
   * but the internal {@link #error} is set to {@value #kWriteError}, returned by {@link #getError()}.
   * The original throw is catched here. 
   * It is similar System.out.append(), the user does not need additional try, 
   * An error is able to check for the whole output. 
   */
  @Override public FileAppend.Wr append(CharSequence text) {
    return this.writer.append(text);   
  }
  
  @Override public void flush() throws IOException
  { if(this.writer != null)
    { this.writer.writer.flush();
    }
  }
  
  /**Important: On garbage at least close() is called here to prevent resource leaks. */
  @Override public void finalize()
  { close();
  }


  public int getError() { return this.error; }
  

  @Override public Appendable append ( CharSequence csq, int start, int end ) throws IOException {
    return this.writer.append(csq, start, end);
  }

  @Override public Appendable append ( char c ) throws IOException {
    // TODO Auto-generated method stub
    return this.writer.append(c);
  }
  
}
