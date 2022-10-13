package org.vishia.util;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

/**This class offers a head structure for timed values, typically measurement values.
 * Each track (one value array) has a name, type, and its data. 
 * All tracks are organized in a container, Map with name as key.
 * The absolute time stamp is associated once to all values.
 * It means the spread of the values should be lesser than the half length of a timeShort.
 * TimeShort is 32 bit. 
 * <br>
 * It means for 50 microseconds time step, it is sufficient for 1 day
 * (exactly one day, 5 hours, 49 minutes, 34 seconds, 192.35 ms for a difference of 0x7fffffff.
 * For 1 ms time step it is approximately 23 days.
 * 
 * @author Hartmut Schorrig, LPGL license, 2022-09-28 - 2022-09-28
 *
 */
public class TimedValues {

  
  /**Version, history and license.
   * <ul>
   * <li>2022-08-28 Hartmut created, as excerp from the {@link org.vishia.gral.base.GralCurveView} data. It is commonly usable.
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
  public final static int version = 20130327;

  
  public final static class Track {
    
    public final String name;
    
    /**Only one of the array references is used, proper to this type Char. */
    public final char typeChar;
    
    /**For evaluation: the max and the min inside the values, free usage. */
    public double max, min;
    
    /**The value array. Only one of the array types is used for one curve.
     * The other remain null
     */
    float[] values_F;
    double[] values_D;
    int[] values_I;
    short[] values_S;
    
    /**Constructs a Track. This is only possible from this class. 
     * Use {@link TimedValues#addTrack(String, char)} to create a track.
     * @param name short name should be unique in all appropriate the data.
     * @param typeChar character D F I S are possible for double, float, int and short.
     * @param length length due to {@link TimedValues#length}
     */
    protected Track(String name, char typeChar, int length) {
      this.name = name;
      this.typeChar = typeChar;
      switch(typeChar) {
      case 'D': this.values_D = new double[length]; break; 
      case 'F': this.values_F = new float[length]; break;
      case 'I': this.values_I = new int[length]; break;
      case 'S': this.values_S = new short[length]; break;
      default: throw new IllegalArgumentException(" org.vishia.util.TimedValues.Track: only D F I S is admissible as typechar");
      }
    }
    
    
    public float getFloat(int ix) {
      switch(this.typeChar) {
      case 'D': return (float)this.values_D[ix];
      case 'F': return this.values_F[ix];
      case 'I': return this.values_I[ix];
      case 'S': return this.values_S[ix];
      default: throw new IllegalArgumentException("should not occure: typeChar faulty");
      }
    }

  
    public int getInt(int ix) {
      switch(this.typeChar) {
      case 'D': return (int)this.values_D[ix];
      case 'F': return (int)this.values_F[ix];
      case 'I': return this.values_I[ix];
      case 'S': return this.values_S[ix];
      default: throw new IllegalArgumentException("should not occure: typeChar faulty");
      }
    }

  
    public double getDouble(int ix) {
      switch(this.typeChar) {
      case 'D': return this.values_D[ix];
      case 'F': return this.values_F[ix];
      case 'I': return this.values_I[ix];
      case 'S': return this.values_S[ix];
      default: throw new IllegalArgumentException("should not occure: typeChar faulty");
      }
    }

    
    public void setFloat(int ix, float value) {
      switch(this.typeChar) {
      case 'D': this.values_D[ix] = value; break;
      case 'F': this.values_F[ix] = value; break;
      case 'I': this.values_I[ix] = (int)value; throw new IllegalArgumentException("assignment float to int");
      case 'S': this.values_S[ix] = (short)value; throw new IllegalArgumentException("assignment float to short");
      default: throw new IllegalArgumentException("should not occure: typeChar faulty");
      }
    }
  
    public void setDouble(int ix, double value) {
      switch(this.typeChar) {
      case 'D': this.values_D[ix] = value; break;
      case 'F': this.values_F[ix] = (float)value; break;
      case 'I': this.values_I[ix] = (int)value; throw new IllegalArgumentException("assignment double to int");
      case 'S': this.values_S[ix] = (short)value; throw new IllegalArgumentException("assignment double to short");
      default: throw new IllegalArgumentException("should not occure: typeChar faulty");
      }
    }
  
    public void setInt(int ix, int value) {
      switch(this.typeChar) {
      case 'D': this.values_D[ix] = value; break;
      case 'F': this.values_F[ix] = value; break;
      case 'I': this.values_I[ix] = value;
      case 'S': this.values_S[ix] = (short)value; throw new IllegalArgumentException("assignment int to short");
      default: throw new IllegalArgumentException("should not occure: typeChar faulty");
      }
    }
  
    public void setShort(int ix, short value) {
      switch(this.typeChar) {
      case 'D': this.values_D[ix] = value; break;
      case 'F': this.values_F[ix] = value; break;
      case 'I': this.values_I[ix] = value; break;
      case 'S': this.values_S[ix] = value; break;
      default: throw new IllegalArgumentException("should not occure: typeChar faulty");
      }
    }
  
  }
  
  /**Association to the absolute time of all values of this series. */
  private Timeshort timeAbs;
  
  private Map<String, Track> tracks = new TreeMap<String, Track>();
  
  /**Proper to the values: relative wrapping time associated to each measurement point. 
   * This timeShort can be newly organized for some points. 
   * For example if a controller is restarted, it counts again from 0.
   * See #timeShortAbs
   */
  private int[] timeShort;
  
  /**This is the relation between #timeShort and the absolute time stored in #timeAbs.
   * To get the correspond time to {@link Timeshort#absTime_short},
   * the {@link #timeShort} value and this value should be added.
   * Then {@link Timeshort#absTime_short} should be subtract. 
   * The result is the number of timeShort increments which should be added to the given {@link Timeshort#absTime}.
   */
  private int[] timeShortAdd;
  
  /**The current index can be helpfully to fill.
   */
  int ix;
  
  private int length = 10000;  //only default
  
  
  public TimedValues(int capacity) {
    cleanSetCapacity(capacity);
  }
  
  
  public int getLength() { return length; }
  
  /**Defines the capacity for new tracks and cleans existing tracks. 
   * @param length the number of values able to store.
   */
  public void cleanSetCapacity(int length) {
    this.length = length;
    for(Map.Entry<String, Track> etrack: this.tracks.entrySet()) {
      Track track = etrack.getValue();
      if(track.values_D !=null) {track.values_D = new double[length]; }
      if(track.values_F !=null) {track.values_F = new float[length]; }
      if(track.values_I !=null) {track.values_I = new int[length]; }
      if(track.values_S !=null) {track.values_S = new short[length]; }
      track.max = Double.MIN_VALUE;                        // to build max, min newly
      track.min = Double.MAX_VALUE;
    }
    this.timeShort = new int[length];
    this.timeShortAdd = new int[length];
  }
  
  
  
  /**Increases the capacity of all given tracks and set the length for new tracks.
   * @param length new length If the length is equivalent to the given length, nothing else is done. 
   * @return false if the new length is less than the given, true if length is correct.
   */
  public boolean increaseCapacity(int length) {
    if(this.length >= length) { return this.length == length; } // true if it is length;
    else {
      this.length = length;
      for(Map.Entry<String, Track> etrack: this.tracks.entrySet()) {
        Track track = etrack.getValue();
        if(track.values_D !=null) { track.values_D = Arrays.copyOf(track.values_D, length); }
        if(track.values_F !=null) {track.values_F = Arrays.copyOf(track.values_F, length); }
        if(track.values_I !=null) {track.values_I = Arrays.copyOf(track.values_I, length); }
        if(track.values_S !=null) {track.values_S = Arrays.copyOf(track.values_S, length); }
        track.max = Double.MIN_VALUE;                        // to build max, min newly
        track.min = Double.MAX_VALUE;
      }
      this.timeShort = Arrays.copyOf(this.timeShort, length);
      this.timeShortAdd = Arrays.copyOf(this.timeShortAdd, length);
      return true;
    }
  }
  
  
  
  
  
  public Track addTrack(String name, char typeChar) {
    Track track = new Track(name, typeChar, this.length);
    this.tracks.put(name, track);
    return track;
  }
  
  /**Get a track to work.
   * @param name
   * @return null if the track with this name does not exists.
   */
  public Track getTrack(String name) { return tracks.get(name); }
  
  
  public int getIncrIx() { 
    int ix = this.ix; 
    this.ix +=1;              // forces an ArrayOutOfBoundsException on the next faulty access.
    return ix;
  }
  
  
  public int getIncrWrapIx() { 
    int ix = this.ix; 
    if(++this.ix >= this.length) {
      this.ix = 0;
    };            
    return ix;
  }
  
  
  public int getTimeShort(int ix) { return this.timeShort[ix] + this.timeShortAdd[ix]; }
  
  
  public int getsetTimeShort(int ix, int timeShort, int timeShortAdd) { 
    int timeShortLast = this.timeShort[ix] + this.timeShortAdd[ix] ; 
    this.timeShort[ix] = timeShort;
    this.timeShortAdd[ix] = timeShortAdd;
    return timeShortLast;
  }
  
  
  /**Set the timeShort to the correspond value.
   * @param ix to this index
   * @param timeShort given timeShort from the source (controller etc.) may be recount from 0 after reset
   * @param timeShortAdd adding value either due to timeAbs adjustment or due to restart/recount. Maybe 0 if not used.
   */
  public void setTimeShort(int ix, int timeShort, int timeShortAdd) { 
    this.timeShort[ix] = timeShort;
    this.timeShortAdd[ix] = timeShortAdd;
  }
  
  
}
