package org.vishia.fileRemote;

import org.vishia.event.EventCmdtype;
import org.vishia.event.EventSource;
import org.vishia.event.EventTimerThread_ifc;
import org.vishia.event.TimeOrder;
import org.vishia.states.StateMachine;

/**This TimeOrder is used for progress showing in the callers area. It should be extended from the application
 * to start any showing process for the progress.  The extension should override the method {@link #executeOrder()} from the super class. 
 */
@SuppressWarnings("synthetic-access")  
public abstract class FileRemoteProgressTimeOrder  extends TimeOrder
{
  private static final long serialVersionUID = 1L;


  public enum Answer{
    noCmd, cont, overwrite, skip, abortFile, abortDir, abortAll
  }
  
  @SuppressWarnings("serial")  
  public final class EventCopyCtrl extends EventCmdtype<Answer> {
    
    public int modeCopyOper;
    public void send(Answer cmd, int modeCopyOper) {
      if(occupyRecall(500, srcAnswer, consumerAnswer, null, false) !=0) {  //recall it for another decision if it is not processed yet.
        this.modeCopyOper = modeCopyOper;
        sendEvent(cmd);
      } //else: if the event is processed yet, it is not send.
      else { System.err.println("FileRemoteProgressTimeOrder - event hangs"); }
    }
  }
  
  public final EventCopyCtrl evAnswer = new EventCopyCtrl();
  
  private final EventSource srcAnswer;
  
  protected int delay;

  /**super constructor:
   * @param name Only for toString(), debug
   * @param mng The manager for this time order to execute it for showing, often a graphic thread adaption.
   *  For example use {@link org.vishia.gral.base.GralMng#gralDevice()} and there {@link org.vishia.gral.base.GralGraphicThread#orderList()}. 
   * @param delay The delay to start the oder execution after #show()
   */
  protected FileRemoteProgressTimeOrder(String name, EventSource srcAnswer, EventTimerThread_ifc mng, int delay){ 
    super(name, mng);
    this.srcAnswer = srcAnswer;
    this.delay = delay;
  }
  
  
  /**Current processed file. */
  public FileRemote currFile, currDir;
  
  /**Number of available directories and files, filled on check. */
  public int nrDirAvail, nrFilesAvail;
  
  /**Processed bytes. */
  public long nrofBytesAll, nrofBytesFile, nrofBytesFileCopied;
  
  /**Number of processed directories and files. */
  public int nrDirProcessed, nrFilesProcessed;
  
  /**Number of Files which are handled special. */
  public int nrofFilesMarked;
  
  /**Command for asking or showing somewhat. */
  private FileRemote.CallbackCmd quest;
  
  private FileRemote.Cmd answer;
  
  private FileRemote.Cmd cmd;
  
  /**Mode of operation, see {@link FileRemote#modeCopyCreateAsk} etc. */
  public int modeCopyOper;
    
  public boolean bDone;

  
  private StateMachine consumerAnswer;
  
  public FileRemote.CallbackCmd quest(){ return quest; }
  
  public FileRemote.Cmd answer(){ return answer; }
  
  public FileRemote.Cmd cmd(){ return cmd; }
  
  public void clearAnswer(){ answer = FileRemote.Cmd.noCmd; } //remove the cmd as event-like; }
  
  /**Invoked from any FileRemote operation, to show the state.
   * 
   * @param stateM the state machine which can be triggered to run or influenced by a pause event.
   */
  public void show(FileRemote.CallbackCmd state, StateMachine stateM) {
    this.consumerAnswer = stateM;
    this.quest = state;
    System.out.println("FileRemote.show");
    activateAt(System.currentTimeMillis() + delay);  //Note: it does not add twice if it is added already.
  }
  
  
  /**Invoked from any FileRemote operation, provides the state with requiring an answer.
   * The information in this instance should be filled actually.
   * @param cmd The quest
   * @param stateM instance which should be triggered to run on the answer.
   */
  public void requAnswer(FileRemote.CallbackCmd quest, StateMachine stateM) {
    this.quest = quest;
    this.consumerAnswer = stateM;
    activateAt(System.currentTimeMillis() + delay);   //to execute the request
  }
  
  
  
}
