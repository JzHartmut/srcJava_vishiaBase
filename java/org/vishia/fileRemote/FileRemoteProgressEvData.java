package org.vishia.fileRemote;

import java.io.Serializable;

//import org.vishia.event.EventCmdtype;
//import org.vishia.event.EventCmdtypeWithBackEvent;
import org.vishia.event.EventConsumer;
import org.vishia.event.TimeOrder;
import org.vishia.fileRemote.FileRemote;
import org.vishia.event.EventWithDst;
import org.vishia.event.Payload;

/**This are the data for an {@link EventWithDst} for FileRemote actions.
 * Hint: the {@link EventConsumer} evaluating this data repectively for the evBack of file operations should be
 * an derived instance of {@link FileRemoteProgress}.
 */
public class FileRemoteProgressEvData implements Serializable, Payload
{

  /**Version, license and history.
   * <ul>
   * <li>2023-03-26 {@link ProgressCmd} now contains all cmd for ask, yet in progress. 
   * <li>2023-02-21 new implements Payload and hence {@link #clean()}
   * <li>2023-02-21 chg {@link #progressCmd} is of type {@link ProgressCmd}, separated now from {@link FileRemote.Cmd}
   * <li>2023-02-21 {@link #done()} in cohesion with {@link org.vishia.event.EventConsumerAwait#awaitExecution(long)}
   *   and {@link TimeOrder#repeatCyclic()}. If {@link #done()} was called the cyclically repeat ends 
   *   because it calls {@link TimeOrder#clear()}.  
   * <li>2023-02-10 refactoring in progress. Tested with The.file.Commenader 
   * <li>2023-02-06 The class TimeOrder is outdated, use its super class {@link TimeOrder} also here. Some adpations done. 
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


  /**Possible answers from quest.
   * @author hartmut
   *
   */
  public enum Answer{
    noCmd, cont, overwrite, skip, abortFile, abortDir, abortAll
  }
  
//  @SuppressWarnings("serial")  
//  public final class EventCopyCtrl extends EventCmdtype<Answer> {
//    
//    public EventCopyCtrl(String name) {
//      super(name);
//    }
//    
//    
//    public int modeCopyOper;
//    public void send(Answer cmd, int modeCopyOper) {
//      if(occupyRecall(500, srcAnswer, consumerAnswer, null, false) !=0) {  //recall it for another decision if it is not processed yet.
//        this.modeCopyOper = modeCopyOper;
//        sendEvent(cmd);
//      } //else: if the event is processed yet, it is not send.
//      else { System.err.println("FileRemoteProgressTimeOrder - event hangs"); }
//    }
//  }
//  
  
  
  
  public enum ProgressCmd {
    /**Refresh answer from walker*/
    noCmd,
    refreshDirPre, refreshDirPost, refreshFile,
    refreshFileFaulty, 
    done,
    nok, error,
    /**callback to ask what to do because the source file or directory is not able to open. */
    askErrorSrcOpen,
    
    /**callback to ask what to do because the destination file or directory is not able to create. */
    askErrorDstCreate,
    
    /**callback to ask what to do on a file which is existing but able to overwrite. */
    askDstOverwr,
    
    /**callback to ask what to do on a file which is existing and read only. */
    askDstReadonly,
    
    /**callback to ask that the file is not able to overwrite. The user can try it ones more or can press skip. */
    askDstNotAbletoOverwr,
    
    /**callback to ask what to do because an copy file part error is occurred. */
    askErrorCopy,
    
    acknAbortAll,
    
    acknAbortDir,
    
    acknAbortFile,
    

  }
  

  
  //public final EventCopyCtrl evAnswer = new EventCopyCtrl("copyAnswer");
  
  //private final EventSource srcAnswer;
  

  /**Cmd describes what the progress event contains. */
  public ProgressCmd progressCmd;

  /**The original command which this is answer to. */
  public FileRemote.Cmd answerToCmd;

  /**Current processed file. */
  public FileRemote currFile, currDir;
  
  public long dateCreate, dateLastAccess;
  
  /**Processed bytes. */
  public long nrofBytesAll, nrofBytesFile, nrofBytesFileCopied;
  
  /**Number of processed directories and files independent of mark situation,
   * but don't count directories which are not entered. */
  public int nrDirVisited, nrDirProcessed, nrFilesVisited;
  
  /**Number of Files which are selected by String mask or marked bits. */
  public int nrofFilesSelected;
  
  /**Number of Files which are marked while walking and processing. */
  public int nrofFilesMarked;
  
  /**Set to not null if 
   * 
   */
  public String sError;
  
  
  
  
//  private FileRemote.Cmd cmd;
  
  /**Mode of operation, see {@link FileRemote#modeCopyCreateAsk} etc. */
  public int modeCopyOper;
    
  /**Set on success.*/
  protected boolean bDone;

  
  /**True then the service has stopped execution (thread is in wait) for an answer.
   * After set the answer bits call notify().
   */
  public boolean bQuest;
  
  /**True then the application will be stop the execution.
   * The execution should be go to wait, set #bQuest before.
   * After notify some of continue etc. should be come.
   */
  public boolean bPause;
  
  /**These are bits for communication from set from the application.*/
  protected boolean bAbort, bOverwrite, bOverwriteAll, bMkdirAll;
  
  
//  private StateMachine consumerAnswer;
  
  
  //public final EventCopyCtrl evAnswer = new EventCopyCtrl("copyAnswer");
  
  //private final EventSource srcAnswer;
  
  
  /**super constructor:
   * @param name Only for toString(), debug
   * @param mng The manager for this time order to execute it for showing, often a graphic thread adaption.
   *  For example use {@link org.vishia.gral.base.GralMng#gralDevice()} and there {@link org.vishia.gral.base.GralGraphicThread#orderList()}. 
   * @param delay The delay to start the oder execution after #show()
   */
  public FileRemoteProgressEvData ( ){ 
    //super(name, timerThread, null, evConsumer, timerThread, eventWalker);
  }


  
  @Override public FileRemoteProgressEvData clean () {
  this.currFile = null;
  this.currDir = null;
  this.sError = null;
  this.progressCmd = FileRemoteProgressEvData.ProgressCmd.noCmd;
  this.dateCreate = 0;
  this.dateLastAccess = 0;
  this.nrDirProcessed = 0;
  this.nrDirVisited = 0;
  this.nrFilesVisited = 0;
  this.nrofFilesSelected = 0;
  this.nrofFilesMarked = 0;
  this.nrofBytesAll = 0;
  this.nrofBytesFile = 0;
  this.nrofBytesFileCopied = 0;
  this.bDone = false;
  this.bQuest = false;
  this.bPause = false;
  this.bAbort = false;
  this.bOverwrite = false;
  this.bOverwriteAll = false;
  this.bMkdirAll = false;
  return this;
}


  /**Set the event to the done() state, all is done maybe with error.
   * @param infoUnusedFinish Either {@link EventConsumer#mEventConsumerException} or {@link EventConsumer#mEventConsumFinished}
   * @param sError a message if any what was unexpected. Especially on unexpected exception. 
   */
  public void done(FileRemote.Cmd answer, String sError) {
    this.sError = sError;
    this.answerToCmd = answer;
//    if(this.timeOrder !=null) {
//      this.timeOrder.notifyConsumed(timeOrderFinish);
//      this.timeOrder.clear();
//      this.timeOrder.deactivate();                      // removes from a timer queue if queued
//    }
    this.bDone = true;                 // activates the same thread as after activate, but yet with done.
//    this.sendEvent();
    //this.timeOrder.activate(0);                 // activate immediately.
  }
  
  
  public boolean done ( ){ return this.bDone; }
  
  /**This operation should be called by the executer if a non clarified situation exists.
   * The executer thread goes in wait till setAnswer is given. 
   * @param quest the quest to the application.
   * @return the given answer from the application. The stored {@link #answer()} is deleted to prevent twice usage.
   */
//  public synchronized final FileRemote.Cmd setQuest ( FileRemote.CallbackCmd quest ) {
//    this.quest = quest;
//    this.answer = FileRemote.Cmd.noCmd;  // the answer cannot be given yet.
//    do {
//      this.bQuest = true;
//      try { this.wait(10000); } catch(InterruptedException exc) {}
//      this.bQuest = false;
//    } while( this.answer == FileRemote.Cmd.noCmd);
//    FileRemote.Cmd answer = this.answer;
//    this.answer =  FileRemote.Cmd.noCmd;  
//    return answer;                                         // return the given answer from application
//  }
//  
  
  /**This operation should be called by the application if a {@link #quest()} is detected.
   * If the executer waits then it will be notified.
   * @param answer the answer for the quest.
   */
  public final void setAnswer ( FileRemoteProgressEvData.ProgressCmd answer ) {
    this.progressCmd = answer;
    if(this.bQuest) {
      synchronized(this) {
        notify();
      }
    }
  }
  
  
  
  
  /**Quest from executer. The application can {@link #setAbort()} for example by user handling any time.
   * @return true if should be aborted.
   */
  public boolean abort ( ) { return this.bAbort; }
   
  /**Set from application to force abort in the executer.*/
  public void setAbort ( ) { this.bAbort = true; }
  
  
  /**Quest from executer while working. If the bit {@link #setPause(boolean)} is given,
   * then the thread goes in wait, checks in an interval of 10 seconds whether {@link #bPause} is still set,
   * and continues if {@link #bPause} is false again. 
   * Note that {@link #setPause(boolean)} with false wakes up waiting.
   * @return true if the execution has paused.
   */
  public boolean pause ( ) { 
    boolean ret = false;
    while(this.bPause) {
      ret = true;
      synchronized(this) {
        this.bQuest = true;
        try { wait(10000); } catch (InterruptedException e) { }
        this.bQuest = false;
      }
    }
    return ret;
  }
   
  /**Set from application to force abort in the executer working in the current.*/
  public void setPause ( boolean value ) { 
    this.bPause = value; 
    if(this.bQuest && !value) {
      synchronized(this) {
        notify();
      }
    }
  }
  
  
  /**Quest from executer. The application can {@link #setAbort()} for example by user handling any time.
   * @return true if should be aborted.
   */
  public boolean overwrite ( ) { return this.bOverwrite; }
   
  /**Set from application to force abort in the executer.*/
  public void setOverwrite ( boolean value ) { this.bOverwrite = value; }
  
  /**Quest from executer. The application can {@link #setAbort()} for example by user handling any time.
   * @return true if should be aborted.
   */
  public boolean bOverwriteAll ( ) { return this.bOverwriteAll; }
   
  /**Set from application to force abort in the executer.*/
  public void setbOverwriteAll ( boolean value ) { this.bOverwriteAll = value; }
  
  /**Quest from executer. 
   * @return true if all directories should created on copy or move.
   */
  public boolean mkdirAll ( ) { return this.bMkdirAll; }
   
  /**Set from application to force abort in the executer.*/
  public void setMkdirAll ( boolean value) { this.bMkdirAll = value; }


  @Override public byte[] serialize () {
    // TODO Auto-generated method stub
    return null;
  }


  @Override public boolean deserialize ( byte[] data ) {
    // TODO Auto-generated method stub
    return false;
  }
  
  
//  @Override public boolean sendEvent() {
//    throw new IllegalStateException("use sendEvent(cmd) instead, this is not allowed here.");
//    //return false;
//  }
  
//  /**Invoked from any FileRemote operation, to show the state.
//   * 
//   * @param stateM the state machine which can be triggered to run or influenced by a pause event.
//   */
//  public void show(FileRemote.CallbackCmd state, StateMachine stateM) {
//    this.consumerAnswer = stateM;
//    this.quest = state;
//    System.out.println("FileRemote.show");
//    this.timeOrder.activateAt(System.currentTimeMillis() + delay);  //Note: it does not add twice if it is added already.
//  }
  
  
//  /**Invoked from any FileRemote operation, provides the state with requiring an answer.
//   * The information in this instance should be filled actually.
//   * @param cmd The quest
//   * @param stateM instance which should be triggered to run on the answer.
//   */
//  public void requAnswer(FileRemote.CallbackCmd quest, StateMachine stateM) {
//    this.quest = quest;
//    this.consumerAnswer = stateM;
//    this.timeOrder.activateAt(System.currentTimeMillis() + delay);   //to execute the request
//  }

//  @Override protected void executeOrder () {
//    //empty implementation. For implementation use inheritance. 
//    
//  }
  
  
  
}
