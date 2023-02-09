package org.vishia.fileRemote;

import org.vishia.event.EventCmdtype;
import org.vishia.event.EventConsumer;
import org.vishia.event.EventSource;
import org.vishia.event.TimeEntry;
import org.vishia.event.EventTimerThread_ifc;
import org.vishia.event.EventWithDst;
import org.vishia.states.StateMachine;

/**This TimeOrder is used for progress showing in the callers area. It should be extended from the application
 * to start any showing process for the progress.  The extension should override the method {@link #executeOrder()} from the super class. 
 */
@SuppressWarnings("synthetic-access")  
public class FileRemoteProgressEvent  extends EventWithDst //TimeOrder
{

  /**Version, license and history.
   * <ul>
   * <li>2023-02-06 The class TimeOrder is outdated, use its super class {@link TimeEntry} also here. Some adpations done. 
   * <li>2015-05-03 Hartmut new: possibility to check {@link #isBusy()}
   * <li>2015-05-03 Hartmut chg: occupyRecall(500,...) for answer events especially abort after exception, prevent hanging of copy in Fcmd
   * <li>2015-01-11 Hartmut created
   *   this class is the base class of them now. The algorithm is able to use outside of that graphic too.
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
  public final static String version = "2023-02-05";

  
  
  private static final long serialVersionUID = 1L;


  public enum Answer{
    noCmd, cont, overwrite, skip, abortFile, abortDir, abortAll
  }
  
  @SuppressWarnings("serial")  
  public final class EventCopyCtrl extends EventCmdtype<Answer> {
    
    public EventCopyCtrl(String name) {
      super(name);
    }
    
    
    public int modeCopyOper;
    public void send(Answer cmd, int modeCopyOper) {
      if(occupyRecall(500, srcAnswer, consumerAnswer, null, false) !=0) {  //recall it for another decision if it is not processed yet.
        this.modeCopyOper = modeCopyOper;
        sendEvent(cmd);
      } //else: if the event is processed yet, it is not send.
      else { System.err.println("FileRemoteProgressTimeOrder - event hangs"); }
    }
  }
  
  
  public final TimeEntry timeEntry;
  
  
  public final EventCopyCtrl evAnswer = new EventCopyCtrl("copyAnswer");
  
  private final EventSource srcAnswer;
  
  protected int delay;

  /**super constructor:
   * @param name Only for toString(), debug
   * @param mng The manager for this time order to execute it for showing, often a graphic thread adaption.
   *  For example use {@link org.vishia.gral.base.GralMng#gralDevice()} and there {@link org.vishia.gral.base.GralGraphicThread#orderList()}. 
   * @param delay The delay to start the oder execution after #show()
   */
  public FileRemoteProgressEvent(String name, EventTimerThread_ifc timerThread, EventSource srcAnswer, EventConsumer evConsumer, int delay){ 
    //super(name, mng);
    super(name, new TimeEntry(name, timerThread, null), evConsumer, timerThread);
    this.timeEntry = (TimeEntry)this.getSource(); //new TimeEntry(name, timerThread, this);
    this.timeEntry.setEvent(this);
    this.srcAnswer = srcAnswer;
    this.delay = delay;
  }
  
  
  /**Current processed file. */
  public FileRemote currFile, currDir;
  
  /**Number of available directories and files, filled on check. */
  public int nrDirAvail, nrFilesAvail;
  
  /**Processed bytes. */
  public long nrofBytesAllAvail, nrofBytesAll, nrofBytesFile, nrofBytesFileCopied;
  
  /**Number of processed directories and files. */
  public int nrDirProcessed, nrFilesProcessed;
  
  /**Number of Files which are handled special. */
  public int nrofFilesMarked;
  
  /**Command for asking or showing somewhat. */
  private FileRemote.CallbackCmd quest;
  
  private FileRemote.Cmd answer;
  
//  private FileRemote.Cmd cmd;
  
  /**Mode of operation, see {@link FileRemote#modeCopyCreateAsk} etc. */
  public int modeCopyOper;
    
  public boolean bDone;

  
  private StateMachine consumerAnswer;
  
  
  public void clear() {
    this.nrDirAvail = 0;
    this.nrDirProcessed = 0;
    this.nrFilesAvail = 0;
    this.nrFilesProcessed = 0;
    this.nrofFilesMarked = 0;
    this.nrofBytesAllAvail = 0;
    this.nrofBytesAll = 0;
    this.nrofBytesFile = 0;
    this.nrofBytesFileCopied = 0;
    this.bDone = false;
    this.timeEntry.clear();
  }

  /**This operation moves the gathered number of to the available numbers.
   * This is the necessary operation between prepare or mark,
   * and the true action (for example copy).
   * With that the progress in percent (proportional) can be shown).
   */
  public void setAvailClear() {
    this.nrDirAvail = this.nrDirProcessed;
    this.nrDirProcessed = 0;
    this.nrFilesAvail = this.nrofFilesMarked;
    this.nrofFilesMarked = 0;
    this.nrFilesProcessed = 0;
    this.nrofBytesAllAvail = this.nrofBytesAll;
    this.nrofBytesAll = 0;
    this.nrofBytesFile = 0;
    this.nrofBytesFileCopied = 0;
    this.bDone = false;
  }
  
  
  public void activateDone() {
    this.timeEntry.deactivate();                      // removes from a timer queue if queued
    this.bDone = true;                 // activates the same thread as after activate, but yet with done.
    this.timeEntry.activate(0);                 // activate immediately.
  }
  
  public FileRemote.CallbackCmd quest(){ return quest; }
  
  public FileRemote.Cmd answer(){ return answer; }
  
//  public FileRemote.Cmd cmd(){ return cmd; }
  
  public void clearAnswer(){ answer = FileRemote.Cmd.noCmd; } //remove the cmd as event-like; }
  
  /**Invoked from any FileRemote operation, to show the state.
   * 
   * @param stateM the state machine which can be triggered to run or influenced by a pause event.
   */
  public void show(FileRemote.CallbackCmd state, StateMachine stateM) {
    this.consumerAnswer = stateM;
    this.quest = state;
    System.out.println("FileRemote.show");
    this.timeEntry.activateAt(System.currentTimeMillis() + delay);  //Note: it does not add twice if it is added already.
  }
  
  
  /**Invoked from any FileRemote operation, provides the state with requiring an answer.
   * The information in this instance should be filled actually.
   * @param cmd The quest
   * @param stateM instance which should be triggered to run on the answer.
   */
  public void requAnswer(FileRemote.CallbackCmd quest, StateMachine stateM) {
    this.quest = quest;
    this.consumerAnswer = stateM;
    this.timeEntry.activateAt(System.currentTimeMillis() + delay);   //to execute the request
  }

//  @Override protected void executeOrder () {
//    //empty implementation. For implementation use inheritance. 
//    
//  }
  
  
  
}
