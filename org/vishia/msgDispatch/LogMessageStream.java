package org.vishia.msgDispatch;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.vishia.bridgeC.OS_TimeStamp;
import org.vishia.bridgeC.Va_list;

public class LogMessageStream implements LogMessage
{
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
  {
    String line = dateFormat.format(creationTime) + "; " + identNumber + "; " + String.format(text,args.get());
    try{ 
    	out.write(line.getBytes()); 
    	out.write(sNewLine);
    }
    catch(IOException exc){ }
    //System.out.println(line);
    return true;
  }

  @Override
  public void close()
  { //do nothing.
  }

  @Override
  public void flush()
  { //do nothing.
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

