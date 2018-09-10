package org.vishia.util;

import java.io.Closeable;

/**This class builds a Thread which runs a routine in a cycle.
 * The intension for an extra class is: On debugging, if a source is changed while running a program,
 * it can reloaded normally for example on eclipse working. But sometimes that is only possible if
 * the CPU does not run in the byteCode of that source. If the thread's loop is part of that source,
 * the class may not able to reload and the debugging is 'out of sync' with the sources.
 * This extra class enables the change of the working class because the CPU is on waiting in this class
 * usual, respectively a thread can hold here by setting a breakpoint. 
 * @author Hartmut Schorrig
 *
 */
public class ThreadRun implements Closeable
{
  /**Version, history and license.
   * <ul>
   * <li>2016-01-23 Hartmut chg: If the wait time was set faulty with a large value, it waits for a long time and blocks.
   *   Therefore the maximal wait time is set to 2 seconds. Typically this class should be used for millisecond-handlings. 
   *   It may okay for all usages? 
   * <li>2014-09-21 Hartmut TODO some variables to class for inspect-ability. Separate const and volatile variables.
   * <li>2013-12-11 Hartmut: Created
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
  public static final int version = 0x20131211;

  
  private final Thread thread;
  
  private final String name;
  
  /**Helper variable to stop one or more threads without changing the source.
   */
  protected boolean stopThread;

  
  /**If this variable is true, the thread runs. Set false on close(); */
  private boolean runThread;

  
  private boolean bThreadWaits, bIsNotified;
  
  
  /**The cyclically wait. */
  private int waitMillisec = 100;
  
  protected int ctTimeoverflow;
  
  protected int stepTimeMeasure;
  
  protected float stepTimeMeasureMid;
  
  private final int cycletime;
  
  private int nextCycle;
  
  /**The step routine.
   * 
   */
  private final Step step;

  
  
  public ThreadRun(String name, Step step, int cycletime){
    thread = new Thread(run, name);
    this.name = name;
    this.step = step;
    this.cycletime = cycletime; 
  }
  

  public void start(){
    thread.start();
  }
  
  
  /**Force the execution of step of this thread invoked from another thread.
   * If this routine is invoked from the own thread (in its step.run()-Routine),
   * then the next wait() is skipped, the step() routine is invoked without wait
   * for the next time. 
   * @param forceNextStep if it is in its step-routine, it does not wait after the current step
   *   but starts the next step immediately. If it is in its wait cycle, this value effects nothing.
   */
  public void forceStep(boolean forceNextStep){
    synchronized(this){
      if(bThreadWaits){ 
        bIsNotified = true;
        notify();   //wakes up.
      }
    }
  }
  
  
  /**Closes this thread. It will end if the {@link #step}-routine will finished or if the thread waits yet.
   * If the thread waits it will be waken up to finish. 
   * If the step routine hangs or has its own timing, waits of anything other etc.
   * the close action is delayed. Not that a step routine should not hang in a proper application.  
   * @see java.io.Closeable#close()
   */
  @Override public void close(){
    runThread = false;
    synchronized(this){
      if(bThreadWaits){ notify(); }  //wakes up.
    }
  }
  
  
  
  /**The threads run routine.
   * @see java.lang.Runnable#run()
   */
  protected void run(){
    runThread = true;
    int shortTime = ((int)System.currentTimeMillis());
    nextCycle = shortTime;
    int timeStepLast = shortTime;
   
    int ctOutofrange=0, ctless0=0, ctOk=0, ctWaitInterrupted=0;
    //
    int timewait = step.start(cycletime);  //TODO some variables to class for inspect-ability. Separate const and volatile variables.
    //
    while(runThread){
      shortTime = ((int)System.currentTimeMillis());
      int calctimelast = shortTime - timeStepLast;
      if(timewait <0){
        waitMillisec = nextCycle - shortTime;
        if(waitMillisec > (5* cycletime) || waitMillisec < -cycletime){
          System.out.printf("ThreadRun name=" + name + " - new time synchronization; %d; ctOk=%d; ctless0=%d; ctNowait=%d;\n"
            , new Integer(waitMillisec), new Integer(ctOk), new Integer(ctless0), new Integer(ctWaitInterrupted));
          ctOk = ctless0 = ctWaitInterrupted = 0;
          ctOutofrange +=1;
          waitMillisec = cycletime;   //synchronize with a faulty cycletime, maybe on time error.
          nextCycle =  shortTime + cycletime;
          Debugutil.stop();
        } else if(waitMillisec <= 0) {
          waitMillisec = (cycletime / 16)+1;  //wait at least 1 ms. 
          ctTimeoverflow +=1;
          waitMillisec = cycletime;   //synchronize with a faulty cycletime, maybe on time error.
          if(waitMillisec < 50){
            System.out.printf("ThreadRun name=" + name + "- less 50 ms;\n", new Integer(waitMillisec));
            waitMillisec = 50;
          }
          ctOk = 0;
          ctless0 +=1;
          nextCycle =  shortTime;
        } else {
          ctOk += 1;
          stepTimeMeasure = cycletime - waitMillisec;
          stepTimeMeasureMid += 0.01f * (stepTimeMeasure - stepTimeMeasureMid);
        }
      } else {
        this.nextCycle = shortTime + timewait;
        waitMillisec = timewait;
      }
      nextCycle += cycletime;
      synchronized(this){
        if(waitMillisec > 0){ //NOTE: its possible that the step routine will continue immediately, then not wait.
          if(waitMillisec > 2000) {
            waitMillisec = 2000;  //no more than 2 seconds, prevent faulty setting.
          }
          bIsNotified = false;
          bThreadWaits = true;
          try{ wait(waitMillisec); } 
          catch(InterruptedException exc){
            ctWaitInterrupted +=1;
          }
          bThreadWaits = false;
        } else {
          Assert.stop();
        }
      }
      if(runThread){
        long timeAbs = System.currentTimeMillis();
        int timeWait = ((int)timeAbs) - shortTime;
        if(timeWait < cycletime/2){
          Debugutil.stop();
        }
        shortTime = ((int)timeAbs);
        if(bIsNotified){
          //the thread was notified. It means the last cycle was shorter by user intention.
          //plan the next cycle  starting from the current time
          nextCycle = shortTime + cycletime;
        }
        int cycletimelast = shortTime - timeStepLast;
        if(cycletimelast < cycletime/2){
          Debugutil.stop();
        }
        try{
          timeStepLast = shortTime;
          timewait = step.step(cycletime, cycletimelast, calctimelast, timeAbs);
        }catch(Throwable exc){
          System.err.println(Assert.exceptionInfo("ThreadRun name=" + thread.getName() + " - unexpected Exception; ", exc, 0, 7));
          exc.printStackTrace(System.err);
        }

      }
    }
  }

 
  private final Runnable run = new Runnable(){
    /**The threads run routine.
     * @see java.lang.Thread#run()
     */
    @Override public void run(){ ThreadRun.this.run(); }
  };//run  
  
  public interface Step{
    
    /**Invoked one time of start of the thread in the thread routine.
     * @return >=0 then it is the wait time for the next step execution (start time).
     *   if ==0 then the next step() is executed without wait.
     *   <0, especially -1 then waits the cycle time to the first step.
     */
    public int start(int cycletimeNom);
    
    /**Invoked cyclically. The cycle is independent of the calculation time of the step
     * if the step calculation time is lesser than the cycle time. 
     * @param cycletime the programmed cycle time
     * @param lastCalctime the last need calculation time.
     *   The step routine can check whether there is a time overflow.
     * @return >=0 then waits till the next step with this given time,
     *   especially if ==0 then the next step() is executed without wait.
     *  <0, especially -1: Executes the next step in exactly the cycle time.
     */
    public int step(int cycletimeNom, int cycletimeLast, int calctimeLast, long millisecAbs);
  }
  
  
}
