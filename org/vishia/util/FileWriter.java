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
 * @version 0.93 2011-01-05  (year-month-day)
 *******************************************************************************/ 
package org.vishia.util;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
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
 * </ul>  
 * @author Hartmut Schorrig
 *
 */
public class FileWriter extends Writer
{
  
  /**The OutputStreamWriter can't be used as superclass, because the file is able to open
   * only with calling constructor. 
   * Thats why all methods of OutputStreamWriter have to be wrapped here.
   */
  protected OutputStreamWriter writer;
  
  public static final int kFileNotFound = -1;
  
  public static final int kFileOpenError = -2;
  
  public String sError;
  
  /**opens a file to write. 
   * @param fileName Path to the file. To separate folder, a slash '/' should be used.
   *        But a backslash is also accepted. 
   *        An absolute path should be start either with slash
   *        or with a one-char drive specifier, following by ':/'.
   * @param append If the file exists, the content written than is appended.
   * @return 0 if the file is opened, -1 if the file is not found, -2 on other conditions.
   */
  public int open(String fileName, boolean append)
  { int error = 0;
    try
    { writer = new OutputStreamWriter(new FileOutputStream(fileName, append)); 
    }
    catch(FileNotFoundException exc)
    { error = kFileNotFound; 
    }
    catch(Exception exc)
    { error = kFileOpenError;
      sError = exc.getMessage(); 
    }
    return error;
  }

  
  public boolean isOpen()
  { return writer != null;
  }
  
  public void close()
  { if(writer != null)
    { try{ writer.close(); }
      catch(IOException exc)
      { //ignore it. The file would never used from here. 
      }
      writer = null; 
    }
  }
  
  public void write(String text) throws IOException
  { if(writer == null) throw new IOException("file isn't opend");
    writer.write(text);
  }
  
  public void flush() throws IOException
  { if(writer != null)
    { writer.flush();
    }
  }
  
  @Override
  public void finalize()
  { close();
  }


  @Override
  public void write(char[] cbuf, int off, int len) throws IOException
  {
    if(writer == null) throw new IOException("file isn't opend");
    writer.write(cbuf,off,len);
    
  }
  
}
