package org.vishia.fileRemote;

import java.util.EventObject;

import org.vishia.event.EventConsumerAwait;
import org.vishia.event.EventThread_ifc;
import org.vishia.event.EventWithDst;

/**This is the base class for all progress actions with {@link FileRemote} calls.
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
public abstract class FileRemoteProgress extends EventConsumerAwait {

  public enum Cmd {

  }

  public final String name;
  
  /**Number of available directories and files, filled on check. */
  public int nrDirAvail, nrFilesAvail;
  
  public long nrofBytesAllAvail;

  
  
  
  /**Constructs as super class
   * @param name A name
   * @param evThread The thread where {@link EventThread_ifc#storeEvent(EventObject)} is called to store the event if it is used.
   */
  protected FileRemoteProgress(String name, EventThread_ifc evThread) {
    super(evThread);
    this.name = name;
  }



  public void clear() {
    this.nrDirAvail = 0;
    this.nrFilesAvail = 0;
    this.nrofBytesAllAvail = 0;
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
  
  /**This operation does the stuff for the application. 
   * @param progress progress data to inform
   * @param evCmd The command event may be given as opponent of the progress event for feedback,
   *   for example if a quest is given ("skip/override" etc.)
   * @return 
   */
  protected abstract int processEvent(FileRemoteProgressEvData progress, EventWithDst<FileRemote.CmdEventData, ?> evCmd);
  
  
  /**This operation is called from {@link EventWithDst#sendEvent(Object)} or from {@link org.vishia.event.EventTimerThread#stepThread()}
   * as overridden. It prepares and calls {@link #processEvent(FileRemoteProgressEvData, EventWithDst)
   * which is overridden by the application.
   */
  @Override public final int processEvent(EventObject evRaw) {
    EventWithDst<FileRemoteProgressEvData, FileRemote.CmdEventData> ev = (EventWithDst<FileRemoteProgressEvData, FileRemote.CmdEventData>) evRaw;
    EventWithDst<FileRemote.CmdEventData, FileRemoteProgressEvData> evCmd = ev.getOpponent();
    FileRemoteProgressEvData progress = ev.data();
    return processEvent(progress, evCmd);
  }


}
