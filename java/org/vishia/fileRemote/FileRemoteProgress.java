package org.vishia.fileRemote;

import java.util.EventObject;

import org.vishia.event.EventConsumerAwait;
import org.vishia.event.EventThread_ifc;
import org.vishia.event.EventWithDst;
import org.vishia.event.Payload;

/**This is the base class for all event consumer instances for progress actions which {@link FileRemote} calls.
 * It is an {@link org.vishia.event.EventConsumer} and hence immediately usable
 * for a {@link org.vishia.event.EventWithDst} with {@link FileRemoteProgressEvData} as payload.
 * <br>
 * An implementor overrides the {@link #processEvent(FileRemoteProgressEvData, EventWithDst)}
 * to evaluate the progress information given in {@link FileRemoteProgressEvData}.
 * It includes the {@link FileRemoteProgressEvData#bDone} to inform all is done and more.
 * 
 * @author Hartmut Schorrig
 *
 */
public class FileRemoteProgress extends EventConsumerAwait {

  
  /**Version, history and license.
   * <ul>
   * <li>2023-07-22 Hartmut new: now contains also the progressData and cmdData as reference and the event itself.
   *   Hence it is a complete usable class for one event handling. 
   *   TODO idea just now: the universal approach may be assemble it all with generics in the {@link EventConsumerAwait} class,
   *   this class is then only a concretion with non generics.
   * <li>2023-03-12 Hartmut created as common solution for all FileRemote oriented event consumer. 
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
  public static final String version = "2023-07-22";

  
  
  
  /**This cmdData are used and reused for calling commands to the {@link FileRemote#device()} during walking. 
   * It are referenced from evBack.
   */
  final FileRemoteCmdEventData cmdData = new FileRemoteCmdEventData();
  
  /**Data for the progress and success back event used and reused for all back event calling, referenced from {@link #evBack}   */
  final FileRemoteProgressEvData progressData = new FileRemoteProgressEvData();

  /**Back event with the evCmd as opponent. Because the opponent has no thread and no Consumer as event destination, 
   * only its payload is used as {@link FileRemoteCmdEventData} for internal commands which are executed if possible in the same thread. 
   * The back event is used for check success, and also necessary if the device uses communication and/or an own thread.
   * The EventConsumer for this evBack is this own class. 
   * It offers {@link #awaitExecution(long, boolean)} in its base class {@link FileRemoteProgress}. 
   */
  public final EventWithDst<FileRemoteProgressEvData, FileRemoteCmdEventData> evBack;

  public final String name;
  
  /**Number of available directories and files, filled on check. */
  public int nrDirAvail, nrFilesAvail;
  
  public long nrofBytesAllAvail;

  
  
  
  /**Constructs the FileRemoteProgress instance maybe also inherit.
   * @param name A name
   * @param progressThread The thread where {@link EventThread_ifc#storeEvent(EventObject)} is called to store the event if it is used.
   * @param cmdThread if given the using command is executed in this thread.
   *   If null then the command is executed in the own thread which calls the command. 
   */
  public FileRemoteProgress(String name, EventThread_ifc progressThread, EventThread_ifc cmdThread) {
    super(progressThread);
    this.evBack = new EventWithDst<FileRemoteProgressEvData, FileRemoteCmdEventData> (
      "ev" + name, null, this, progressThread, this.progressData, "evCmd" + name, null, null, cmdThread, this.cmdData);
    this.name = name;
  }



  @Override public FileRemoteProgress clean( ) {
    this.nrDirAvail = 0;
    this.nrFilesAvail = 0;
    this.nrofBytesAllAvail = 0;
    this.evBack.clean();
    this.cmdData.clean();
    this.progressData.clean();
    return this;
  }

  
  
  /**This operation moves the gathered number of to the available numbers.
   * This is the necessary operation between prepare or mark,
   * and the true action (for example copy).
   * With that the progress in percent (proportional) can be shown).
   */
  public void setAvail(FileRemoteProgressEvData ev) {
    this.nrDirAvail = ev.nrDirProcessed;
    this.nrFilesAvail = ev.nrofFilesSelected;
    this.nrofBytesAllAvail = ev.nrofBytesAll;
  }
  
  /**This operation should be overridden and called as super.processEvent()
   * if more should be done with the progress event.
   * It is sufficient if only success or error is expected. 
   * @param progress progress data to inform
   * @param evCmd The command event may be given as opponent of the progress event for feedback,
   *   for example if a quest is given ("skip/override" etc.)
   *   This can be used in the overridden operations. 
   * @return 
   */
  protected int processEvent(FileRemoteProgressEvData progress, EventWithDst<FileRemoteCmdEventData, FileRemoteProgressEvData> evCmd) {
    if(progress.done()) {                                // check whether done() is given, all other is not interest.
      this.setDone(progress.sError);                     // setDone awakes a waiting thread which has called await
    }
    return mEventDonotRelinquish;      // will be relinquished after wait
  }
  
  
  
  /**This operation is called from {@link EventWithDst#sendEvent(Object)} or from {@link org.vishia.event.EventTimerThread#stepThread()}
   * as overridden. It prepares and calls {@link #processEvent(FileRemoteProgressEvData, EventWithDst)
   * which is overridden by the application.
   */
  @Override public int processEvent(EventObject evRaw) {
    assert(evRaw instanceof EventWithDst);                 // an EventWithDst is expected
    @SuppressWarnings("unchecked") EventWithDst<FileRemoteProgressEvData, FileRemoteCmdEventData> ev = 
        (EventWithDst<FileRemoteProgressEvData, FileRemoteCmdEventData>) evRaw;  // cast it to the basic type
    FileRemoteProgressEvData progress = ev.data();                         // get and test type of the data
    EventWithDst<FileRemoteCmdEventData, FileRemoteProgressEvData> evCmd = ev.getOpponent();
    return processEvent(progress, evCmd);
  }


}
