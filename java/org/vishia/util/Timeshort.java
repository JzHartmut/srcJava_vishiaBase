package org.vishia.util;

/**This class supports working with a short time which may be count in microseconds or any other unit, and its conversion
 * to an absolute time stamp.
 * <br>
 * It is a contradiction that must be resolved: For a time continue control the absolute time has prior no meaning. 
 * But a relative time should be exact count in steps of the control. 
 * This relative time may tuned to the real time, for example a step cycle should be exactly 0.001000 seconds (1 ms)
 * related to the absolute time (UTC). But this cycle step width has small errors due to tolerances.
 * Now it is not a good idea to change the running step counter for the control cycle, because then, 
 * local differences are incorrect. It should not be done.
 * <br>
 * Instead, the absolute time associated to the relative time should be noted in a longer cycle.
 * Hence, the relative time can be associated to the correct absolute time, 
 * but with difference associations depending from the absolute time itself. 
 * Look on a simple example:
 * <pre>
 timeShort:   0  1  2  3  4  5  6  7  8  9 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24 25 26 27 28 29 30 31 32 33 34 35 36 37 38 39  
 seconds UTC:12 13  14  15 16 17  18 19 20 21  22 23 24 25 26  27 28 29 30  31 32 33  34 35 36 37 38 39 30  41 42 43  44 45 46 47 48 49
 timeAdd     12 12 12 12 12 12 12 11 11 11 11 11 11 11 11 11 11 11 11 11 11 10 10 10 10 10 10 10 10 10 10 10 10 10 10  9  9  9  9  9
 </pre>
 * For this example the timeShort (relative time) is ~ 6% faster then the UTC time. Realistic a relative time may have toleances
 * in range of ppm, parts per million. For a millisecond counter it means you 
 * for 10 ppm tolerance you have a jump in the assignment every 100 second. 
 * <br>
 * If you want to view a curve in a time spread of for example 10 seconds with 10000 points, one point is 1 ms, 
 * you can exactly associated the timeShort or relative time to the absolute one without error.
 * But if you look about one hour (3600 seconds) in shift to different ranges, the association between the currently relative time
 * and the absolute time is not unique. 
 * In order to have accurate differences everywhere, you should refrain from assigning exact times to each point. 
 * Instead use one time association from the currently relative time to the absolute one maybe in the mid of your measurement points.
 * Then you get an timing error from 18 milliseconds on the left and right side of your measurements
 * for the given spread of one hour, on a quartz tolerance of 10 ppm.
 * For exact comparison to other absolute times of measurements you should currently associated any point in your spread to the absolute time.
 * The differences are always correct. 
 * <br>
 * But if you assign the exact absolute time to each point, strange differences arise at the points where the time assignment is adjusted.
 * <br>
 * In conclusion, there a two systems possible to assign the absolute time to currently relative time stamps:
 * <ul>
 * <li>a) you store two values for each measurement point, one is the currently relative time (timeShort) 
 *   and the other is the relation to a given absolute time, also as shortTime value. In the example above it is the line "timeAdd".
 *   Only ones you should store the absolute time association to your relative times minus "timeAdd".
 *   This is the time where your relative time has the zero crossing. 
 * <li>b) you store only one value for each measurement point, the currently relative time (timeShort).
 *   You have a sorted table, where you can find the association from your timeShort to the absolute time. 
 *   This sorted table should contain entries only on that points where the association between timeShort and timeAbs changes. 
 *   For a Tolerance of 10 ppm it is one entry for 100000 measurement steps or 36 entries for one hour and 1 ms step of timeSHort.
 * </ul>
 * On both variants an unsolved problem of absolute time association remains if your timeShort period is lesser than your spread of viewing. 
 * For a timeShort width of 32 bit this is +- 23 days for 1 ms step (2000000 seconds), or approximately +- 1 day for a time step of 50 us.
 * For that reason it is helpfully that the absolute time is stored due to one measurement, which's spread must be lesser than this period.
 * Usual the measurement are stored in separate files, and any file can get the associated absolute time for the whole spread. 
 * <br>
 * Hence you can use for the both variants of measurement files:
 * <ul>
 * <li>a) One absolute time association to your whole measurement values and two values for timeShort and timeAdd per measurement point.
 * <li>b) A not too long sorted table of time associations to your whole measurement and only one value for timeShort per measurement point. 
 * </ul>
 * The variant a) is better if you have the problem that the relative timeShort counting may jump or restart during the measurement period.
 * That can be occur if a controller is reseted. It means that variant a) should favored if such is expectable.
 * <br>
 * This class helps to deal with this timing system. Instances of this class can be used even for the association 
 * between the currently relative measurement time (timeShort) and the absolute time.   
 *      
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
