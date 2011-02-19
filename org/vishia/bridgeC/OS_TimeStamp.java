package org.vishia.bridgeC;

import java.util.Date;


/**This class is useable in C. 
 * In Java it should be derived from Date because it is used at example 
 * in SimpleDateFormat.format(...) as argument. 
 */
public class OS_TimeStamp 
  extends Date   //because using in SimpleDateFormat.format, also in C
{
  //Date date;
  
  private static final long serialVersionUID = 1L;

  public int time_sec;
  
  public int time_nsec;
  
  public OS_TimeStamp()
  { super(0);
    time_sec = 0;
    time_nsec = 0;
  }
  
  
  public OS_TimeStamp(long milliSecondsAfter1970)
  { super(milliSecondsAfter1970);
    long millisec = getTime();
    time_sec = (int)(millisec/1000);
    time_nsec = (int)((millisec - 1000 * time_sec)*1000000);
  }
  
  public OS_TimeStamp(boolean now)
  { super();
    long millisec = getTime();
    time_sec = (int)(millisec/1000);
    time_nsec = (int)((millisec - 1000 * time_sec)*1000000);
  }
  
  /**Returns an instance with the current system time. 
   * In C the instance is returned by value. Therefore no allocation is done.
   * In Java a new instance is created.
   */
  public static OS_TimeStamp os_getDateTime()
  { //Date date = new Date(); //gets the system date.
    OS_TimeStamp ret = new OS_TimeStamp(true);
    return ret;
  }
  
  
  
  public static boolean os_delayThread(int millisec)
  { boolean breaked = false;
    try{ Thread.sleep(millisec);} 
    catch (InterruptedException e){ breaked = true; }
    return breaked;
  }
  
  
}
