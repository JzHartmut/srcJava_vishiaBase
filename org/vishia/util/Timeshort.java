package org.vishia.util;

/**This class supports working with a short time which may be count in microseconds or any other unit, and its conversion
 * to an absolute timestamp.
 * @author Hartmut Schorrig
 *
 */
public class Timeshort {
  
  /**Version, history and license.
   * <ul>
   * <li>2016-03-06 Hartmut new: {@link #clean()} and {@link #isCleaned()} to set a new pair of absTime_short and absTime.
   * <li>2016-03-06 Hartmut new: set data to private, access only via methods! (Why they were public?) 
   * <li>2013-04-30 Hartmut new: {@link #sleep(long)} as wrapper around Thread.sleep() without Exception.
   * <li>2012-10-14 Hartmut created as util class from well known usage.
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
   * <li> But the LPGL is not appropriate for a whole software product,
   *    if this source is only a part of them. It means, the user
   *    must publish this part of source,
   *    but doesn't need to publish the whole source of the own product.
   * <li> You can study and modify (improve) this source
   *    for own using or for redistribution, but you have to license the
   *    modified sources likewise under this LGPL Lesser General Public License.
   *    You mustn't delete this Copyright/Copyleft inscription in this source file.
   * </ol>
   * If you intent to use this source without publishing its usage, you can get
   * a second license subscribing a special contract with the author. 
   * 
   * @author Hartmut Schorrig = hartmut.schorrig@vishia.de
   * 
   */
  public static final int version = 20130430;
  
  
  
  /**The shorttime-stamp to the {@link #absTime} timestamp. Set with {@link GralCurveView#setTimePoint(long, int, float)}. */
  private int absTime_short;
  
  /**Any absolute  timestamp to the {@link #absTime_short}. Set with {@link GralCurveView#setTimePoint(long, int, float)}. */
  private long absTime =-1;
  
  /**Milliseconds for 1 step of shorttime. */
  private float absTime_Millisec7short = 1.0f;


  public Timeshort(){}
  
  public Timeshort(Timeshort src){
    synchronized(this){
      absTime_short = src.absTime_short;
      absTime = src.absTime;
      absTime_Millisec7short = src.absTime_Millisec7short;
    }
  }
  
  
  /**Returns the absolute time in milliseconds after 1970 to a given timeshort. */
  public synchronized long absTimeshort(int timeshort){
    return (long)((timeshort - absTime_short) * absTime_Millisec7short) + absTime;
  }

  
  public synchronized void setTimePoint(long date, int timeshort, float millisecPerTimeshort){
    absTime_short = timeshort;
    absTime_Millisec7short = millisecPerTimeshort;
    absTime = date; //graphic thread: now complete and consistent.
  }
  
  
  public void clean(){ absTime = -1; absTime_short = 0; }
  
  public boolean isCleaned(){ return absTime == -1L; }
  
  /**Returns the factor between milliseconds / shorttime_difference
   * @return
   */
  public float millisec7short(){ return absTime_Millisec7short; }
  
  /**Returns the milliseconds after the last {@link #setTimePoint(long, int, float)} according to the given timeshort. */
  public synchronized float millisecShort(int timeshort){ return absTime_Millisec7short * (timeshort - absTime_short); }
  
  /**Returns the timeshort steps to the given date according to the last {@link #setTimePoint(long, int, float)}.
   * @param date The current date, it should be later than the date on the setTimePoint(...)
   * @return timeshort steps to the date in respect to the time point.
   */
  public int timeshort4abstime(long date) {
    double millisec = date - absTime;  //divide in double to preserve 64 bits.
    long timeshort1 = (long)(millisec / absTime_Millisec7short);
    if(timeshort1 < 0x100000000L) {
      return (int)(timeshort1 + absTime_short);
    } else {
      return 0;
    }
  }
  
  
  
  /**Universal wait routine without necessity of a try-catch wrapping.
   * @param millisec
   * @return true if it was interrupted.
   */
  public static boolean sleep(long millisec){
    boolean interrupted = false;
    try{
      Thread.sleep(millisec); 
    } catch( InterruptedException exc){
      interrupted = true;
    }
    return interrupted;
  }
  
}
