package org.vishia.util;

//import java.util.concurrent.atomic.AtomicInteger;
//import java.util.concurrent.atomic.AtomicLong;

/**This class contains the current time stamp of creation and a counted number of instances which are created in the same millisecond.
 * A log output or such may need the exact order of events. If more events have the same time stamp in milliseconds 
 * and they are stored unsorted, then its order would not able determine. The {@link #order} helps to detect the correct order.
 * The order is derived from private static elements for time stamp in millisecond and a counter 
 * which are presented only one time application-widely.
 * 
 * @author Hartmut Schorrig
 *
 */
public class DateOrder {

  
  /**Version and history.
   * <ul>
   * <li>2012-09-12 Hartmut created.
   * </ul> 
   * 
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
   *    but don't need to publish the whole source of the own product.
   * <li> You can study and modify (improve) this source
   *    for own using or for redistribution, but you have to license the
   *    modified sources likewise under this LGPL Lesser General Public License.
   *    You mustn't delete this Copyright/Copyleft inscription in this source file.
   * </ol>
   * If you are indent to use this sources without publishing its usage, you can get
   * a second license subscribing a special contract with the author. 
   * 
   * @author Hartmut Schorrig = hartmut.schorrig@vishia.de
   * 
   * 
   */
  public final static String version = "2015-01-17";

  
  
  //private static AtomicInteger orderInTime = new AtomicInteger(0);
  
  //private static AtomicLong lastTime = new AtomicLong(0);
  
  private static int orderInTime = 0;
  
  private static long lastTime = 0;
  
  //private static boolean TEST;
  //private static int TEST2;

  
  public final long date;
  public final int order;
  
  
  public synchronized static DateOrder get() {
    return new DateOrder();
  }
  
  
  private DateOrder(){
    date = System.currentTimeMillis();
    if(date != lastTime){
      orderInTime = 0;
    }
    order = ++orderInTime;
  }
  
  /** does not work because 2 consistence-need data:
  public DateOrder(){
    boolean repeat;
    long date1; 
    int catastrophicCount = 10000;
    do {
      date1 = System.currentTimeMillis();
      long lastTime1 = lastTime.get();
      if(date1 != lastTime1) {
        int orderInTimeLast = orderInTime.get();
        if(TEST && ++TEST2 ==1) { new DateOrder(); } //invoke recursively.
        if(   orderInTime.compareAndSet(orderInTimeLast, 0)
          &&  lastTime.compareAndSet(lastTime1, date1)){
          //this thread has reset the orderInTime and set the new date.
          repeat = false;
        } else {
          //another process has interrupted between 2 lines get and compareAndSet.
          //it has set the new time already.
          //but that process may have a duration longer as 1 millisecond.
          //Therefore repeat the action.
          repeat = true;
        }
      } else { 
        repeat = false; 
      }
    } while(repeat && --catastrophicCount >=0);
    if(catastrophicCount <0) throw new RuntimeException("too many loops, software problem");
    date = date1;
    order = orderInTime.incrementAndGet();
  }
  */
  
  
  /**Test routine.
   * @param args
   */
  public static void main(String[] args){
    //TEST=true;
    new DateOrder();
    new DateOrder();
    
  
  }
  
  
}
