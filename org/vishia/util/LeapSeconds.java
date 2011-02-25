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

import java.util.Arrays;
import java.util.Date;

/**This class supports the handling with Leap seconds.
 * <br>
 * Background:
 * <ul>
 * <li>UTC (= <a href="http://en.wikipedia.org/wiki/Coordinated_Universal_Time">Coordinated Universal Time</a> ) 
 *     follows the caesium atomic clock, 
 *     but it is coordinated with the earth rotation. respectively it follows the simple division 
 *     of 1 year into 365 * 24* 3600 seconds and 366 * 24 * 3600 seconds in a leap year.
 *     If leap seconds are occurred, the second count of UTC is the same for 2 seconds.
 * <li>In computer systems, often UTC is used, because the unix-time also count simple seconds
 *     without regarding of leap seconds. Therefore, also the class java.util.Date based on UTC.
 * <li>The TAI (= <a href="http://en.wikipedia.org/wiki/International_Atomic_Time">en.wikipedia.org/wiki/International_Atomic_Time</a>         
 *     counts uniformly continuous and faster than the earth rotation. 
 *     Thats why leap seconds are necessary to adapt the earth-rotation oriented second count (to UTC).
 * <li>The time from GPS positioning follows the TAI, with a constant difference of 19 seconds, 
 *     without any leap seconds. Why 19 seconds? It's historical.
 * <li>In technical systems a strict uniformly continuous time counter is better to than the UTC.
 * </ul>
 * 
 * This class helps to adapt GPS or other strict uniformly continuous time counter to UTC 
 * and to java.util.Date. 
 * A table of leap seconds may be given outside, because there are not defined yet for the future.
 *          
 * @author Hartmut Schorrig
 *
 * Changes:
 * * 2009-03-08: Hartmut: new: millisecondsGPSfromUTC(), secondsGPSfromUTC().
 * * 2008-08-00: Hartmut creation
 *
 */
public class LeapSeconds
{
  
  /**All methods are static, but uses internally data of this class. 
   * Therefore an singleton instance is created if not exist and referenced here.
   * @java2c=noGC.
   */
  protected static LeapSeconds singleton;

  /**Array of number of leap seconds. @java2c=simpleArray. */
  protected final int[] leapSeconds = new int[30];
  
  /**Array of values of seconds after 1970, which are UTC seconds. 
   * The value of seconds doesn't count leaps seconds. @java2c=simpleArray. 
   */
  protected long[] millisecondsUTCForLeapSeconds = new long[30];
  
  /**Array of values of seconds after 1970, which are leap seconds. 
   * The value of seconds counts also leaps seconds (not UTC, but GPS). @java2c=simpleArray. 
   */
  protected long[] millisecondsGPSForLeapSeconds = new long[30];
  
  protected LeapSeconds()
  { initFix();
  }
  

  /**Sets the leap seconds for a given timestamp. This routine should be called on startup phase
   * if a input is available.
   * @param index number of leap seconds for this date. It is the index in the table.
   * @param date The UTC timestamp after which a leap second is inserted. 
   */
  public static void setLeapSeconds(int idx, Date date)
  { if(LeapSeconds.singleton == null){ singleton = new LeapSeconds();}
    if(idx < 0 || idx > singleton.leapSeconds.length) 
        throw new IllegalArgumentException("The number of leap seconds should be not greater than" + (singleton.leapSeconds.length -1));
    singleton.leapSeconds[idx] = idx; 
    singleton.millisecondsUTCForLeapSeconds[idx] = date.getTime();
    singleton.millisecondsGPSForLeapSeconds[idx] = idx + date.getTime();
  }
  
  /**initializes the table of leap seconds with fix dates.
   * The dates are get from <a href="http://en.wikipedia.org/wiki/Leap_second">http://en.wikipedia.org/wiki/Leap_second</a>
   * Dates after 2008 are guessed.
   */
  @SuppressWarnings("deprecation")
  protected void initFix()
  { int idx = -1; //preincrement
    //NOTE: using the deprecated Date.UTC because it is a simple conversion.
    //The dates are getted from <a href="http://en.wikipedia.org/wiki/Leap_second">http://en.wikipedia.org/wiki/Leap_second</a>
    millisecondsUTCForLeapSeconds[++idx] = Date.UTC(72,  6, 30, 23,59,59);  //0
    millisecondsUTCForLeapSeconds[++idx] = Date.UTC(72, 12, 31, 23,59,59);
    millisecondsUTCForLeapSeconds[++idx] = Date.UTC(73, 12, 31, 23,59,59);
    millisecondsUTCForLeapSeconds[++idx] = Date.UTC(74, 12, 31, 23,59,59);
    millisecondsUTCForLeapSeconds[++idx] = Date.UTC(75, 12, 31, 23,59,59);
    millisecondsUTCForLeapSeconds[++idx] = Date.UTC(76, 12, 31, 23,59,59);
    millisecondsUTCForLeapSeconds[++idx] = Date.UTC(77, 12, 31, 23,59,59);
    millisecondsUTCForLeapSeconds[++idx] = Date.UTC(78, 12, 31, 23,59,59);
    millisecondsUTCForLeapSeconds[++idx] = Date.UTC(79, 12, 31, 23,59,59);
    millisecondsUTCForLeapSeconds[++idx] = Date.UTC(81,  6, 30, 23,59,59);
    millisecondsUTCForLeapSeconds[++idx] = Date.UTC(82,  6, 30, 23,59,59);  //10
    millisecondsUTCForLeapSeconds[++idx] = Date.UTC(83,  6, 30, 23,59,59);
    millisecondsUTCForLeapSeconds[++idx] = Date.UTC(85,  6, 30, 23,59,59);
    millisecondsUTCForLeapSeconds[++idx] = Date.UTC(87, 12, 31, 23,59,59);
    millisecondsUTCForLeapSeconds[++idx] = Date.UTC(89, 12, 31, 23,59,59);
    millisecondsUTCForLeapSeconds[++idx] = Date.UTC(90, 12, 31, 23,59,59);
    millisecondsUTCForLeapSeconds[++idx] = Date.UTC(92,  6, 30, 23,59,59);
    millisecondsUTCForLeapSeconds[++idx] = Date.UTC(93,  6, 30, 23,59,59);
    millisecondsUTCForLeapSeconds[++idx] = Date.UTC(94,  6, 30, 23,59,59);
    millisecondsUTCForLeapSeconds[++idx] = Date.UTC(95, 12, 31, 23,59,59);
    millisecondsUTCForLeapSeconds[++idx] = Date.UTC(97,  6, 30, 23,59,59);  //20
    millisecondsUTCForLeapSeconds[++idx] = Date.UTC(98, 12, 31, 23,59,59);
    millisecondsUTCForLeapSeconds[++idx] = Date.UTC(105, 12, 31, 23,59,59);
    millisecondsUTCForLeapSeconds[++idx] = Date.UTC(108, 12, 31, 23,59,59);
    //NOTE: the next dates are guessed:
    millisecondsUTCForLeapSeconds[++idx] = Date.UTC(111, 12, 31, 23,59,59);
    millisecondsUTCForLeapSeconds[++idx] = Date.UTC(113, 12, 31, 23,59,59);
    millisecondsUTCForLeapSeconds[++idx] = Date.UTC(116, 12, 31, 23,59,59);
    millisecondsUTCForLeapSeconds[++idx] = Date.UTC(119, 12, 31, 23,59,59);
    millisecondsUTCForLeapSeconds[++idx] = Date.UTC(122, 12, 31, 23,59,59);
    millisecondsUTCForLeapSeconds[++idx] = Date.UTC(125, 12, 31, 23,59,59);  //29
    for(idx = 0; idx < leapSeconds.length; idx++)
    { leapSeconds[idx] = idx;
      millisecondsGPSForLeapSeconds[idx] = millisecondsUTCForLeapSeconds[idx] + idx;
    }
  }
                                     
  /**Returns a Date object from given GPS-seconds.
   * @param millisecondsGPS Value of milliseconds from Jan-1-1970, which counts also leap seconds.
   * @return The Date-Object is UTC-oriented. 
   *         Exactly in a leap seconds the same value is returned as in second before.
   */
  public static Date dateFromGPS(long millisecondsGPS)
  { if(LeapSeconds.singleton == null){ singleton = new LeapSeconds();}
    int idx = Arrays.binarySearch(singleton.millisecondsGPSForLeapSeconds, millisecondsGPS);
    if(idx < 0)
    { //normal: no leap seconds
      idx = -idx -1;
    }
    else
    { //exactly a leap second
      //do nothing.
    }
    return new Date(millisecondsGPS - 1000*idx);
  }
  
                                     
  public static long millisecondsGPSfromUTC(long millisecondsUTC)
  { if(LeapSeconds.singleton == null){ singleton = new LeapSeconds();}
    int idx = Arrays.binarySearch(singleton.millisecondsUTCForLeapSeconds, millisecondsUTC);
    if(idx < 0)
    { //normal: no leap seconds
      idx = -idx -1;
    }
    else
    { //exactly a leap second
      //do nothing.
    }
    return millisecondsUTC + 1000*idx;
  }
  
  public static int secondsGPSfromUTC(int secondsUTC_1970)
  { if(LeapSeconds.singleton == null){ singleton = new LeapSeconds();}
    int idx = Arrays.binarySearch(singleton.millisecondsGPSForLeapSeconds, (long)secondsUTC_1970 * 1000);
    if(idx < 0)
    { //normal: no leap seconds
      idx = -idx -1;
    }
    else
    { //exactly a leap second
      //do nothing.
    }
    return secondsUTC_1970 + idx;
  }
  
                                     
}
