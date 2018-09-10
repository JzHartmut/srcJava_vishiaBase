package org.vishia.bridgeC;

import java.util.Date;


/**This class is useable in C. 
 * In Java it should be derived from Date because it is used at example 
 * in SimpleDateFormat.format(...) as argument. 
 */
public class OS_TimeStamp 
  extends Date   //because using in SimpleDateFormat.format, also in C
{
  /**Version, history and license
   * <ul>
   * <li>2012-04-05 Hartmut new: {@link #os_getSeconds()}: seconds are able to process with a
   *   32-bit integer. They are supported in most of operation systems. It is the originally unix concept
   *   for the timestamp. It should be available if measurements of seconds are enough and proper to use.
   *   In Java it is the milliseconds from 1970 divide by 1000, converted to (int).
   * <li>2009-00-00 Hartmut: Created as opposite to CRuntimeJavalike/OSAL/os_time.h
   *   Intension: Java has some good definitions of a operation system interface. In C it isn't well in this form.
   *   The CRuntimeJavalike/OSAL is an approach for an Operation System Adaption Layer in C.
   *   This class is the opposite in Java for Java2C.
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
  public static final int version = 20120409;


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
  
  
  /**Sets this instance to the timestamp given in src.
   * @C It is an immediately set from source.
   * @Java It sets the super class Date too, of course.
   * @param src Any source.
   * @return this
   */
  public OS_TimeStamp set(OS_TimeStamp src)
  { this.setTime(src.getTime());
  	time_sec = src.time_sec;
  	time_nsec = src.time_nsec;
  	return this;
  }
  
  public static boolean os_delayThread(int millisec)
  { boolean breaked = false;
    try{ Thread.sleep(millisec);} 
    catch (InterruptedException e){ breaked = true; }
    return breaked;
  }

  
  
  public static int os_getSeconds(){
    return (int)(System.currentTimeMillis() / 1000); 
  }
  
}
