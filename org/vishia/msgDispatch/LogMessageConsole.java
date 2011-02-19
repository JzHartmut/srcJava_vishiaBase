package org.vishia.msgDispatch;

import java.text.SimpleDateFormat;

import org.vishia.bridgeC.OS_TimeStamp;
import org.vishia.bridgeC.Va_list;

public class LogMessageConsole extends LogMessage
{
  
  public static LogMessage create()
  {
    return new LogMessageConsole();
  }

  final private SimpleDateFormat dateFormat = new SimpleDateFormat("MMM-dd HH:mm:ss.SSS: ");
  

  public LogMessageConsole()
  {
    
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
    String line = dateFormat.format(creationTime) + "; " + identNumber + "; " + String.format(text,args.buffer.get());
    System.out.println(line);
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
  
}

