package org.vishia.msgDispatch;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.vishia.bridgeC.OS_TimeStamp;
import org.vishia.bridgeC.Va_list;

/**This class adapts a given stream output channel to the LogMessage interface to output messages for example
 * to the System.out or System.err.
 * @author Hartmut Schorrig
 *
 */
public class LogMessageStream implements LogMessage
{
  
  
  /**Version, history and license.
   * <ul>
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
  public static final int version = 20120702;

  
  
  final FileDescriptor fd;
  
  final OutputStream out;
  
  /**Newline-String, for windows, TODO. depends from OS. */
  byte[] sNewLine = { '\r', '\n'};
  
  public static LogMessage create(FileDescriptor fd)
  {
    return new LogMessageStream(fd);
  }

  final private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM-dd HH:mm:ss.SSS: ");
  

  public LogMessageStream(FileDescriptor fd)
  {
    this.fd = fd; 
    out = new FileOutputStream(fd);
  }
  
  public LogMessageStream(OutputStream out)
  {
    this.out = out; 
    this.fd = null;
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
  public boolean sendMsgVaList(int identNumber, OS_TimeStamp creationTime, String text, Va_list args)
  { String line = "?";
    try{
      line = dateFormat.format(creationTime) + "; " + identNumber + "; " + String.format(text,args.get());
    } catch(Exception exc){
      line = dateFormat.format(creationTime) + "; " + identNumber + "; " + text;
    }
      try{ 
      out.write(line.getBytes()); 
      out.write(sNewLine);
    }
    catch(Exception exc){ 
    }
    return true;
  }

  @Override
  public void close()
  { //do nothing.
  }

  @Override public void flush()
  { try{ out.flush();
    } catch(IOException exc){}
  }

  @Override
  public boolean isOnline()
  { return true; 
  }

  @Override
  public boolean sendMsg(int identNumber, String text, Object... args) {
    String line = dateFormat.format(new Date(System.currentTimeMillis())) + "; " + identNumber + "; " + String.format(text,args);
    try{ 
      out.write(line.getBytes());
      out.write(sNewLine);
    }
    catch(IOException exc){ }
    return true;
  }

  @Override
  public boolean sendMsgTime(int identNumber, OS_TimeStamp creationTime,
      String text, Object... args) {
    String line = dateFormat.format(creationTime) + "; " + identNumber + "; " + String.format(text,args);
    try{ 
      out.write(line.getBytes()); 
      out.write(sNewLine);
    }
    catch(IOException exc){ }
    return true;
  }
  
  
}

