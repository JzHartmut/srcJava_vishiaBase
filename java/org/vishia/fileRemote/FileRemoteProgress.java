package org.vishia.fileRemote;

import java.util.EventObject;

import org.vishia.event.EventConsumerAwait;
import org.vishia.event.EventThread_ifc;
import org.vishia.event.EventWithDst;

/**This is the base class for all progress actions with {@link FileRemote} calls.
 * It is an {@link org.vishia.event.EventConsumer} and hence immediately usable
 * for a {@link org.vishia.event.EventWithDst} with {@link FileRemoteProgressEvData} as payload.
 * <br>
 *  
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

  
  
  
  public FileRemoteProgress(String name, EventThread_ifc evThread) {
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
  
  protected abstract int processEvent(FileRemoteProgressEvData progress, EventWithDst<FileRemote.CmdEvent, ?> evCmd);
  
  
  @Override public final int processEvent(EventObject evRaw) {
    EventWithDst<FileRemoteProgressEvData, FileRemote.CmdEvent> ev = (EventWithDst<FileRemoteProgressEvData, FileRemote.CmdEvent>) evRaw;
    EventWithDst<FileRemote.CmdEvent, FileRemoteProgressEvData> evCmd = ev.getOpponent();
    FileRemoteProgressEvData progress = ev.data();
    return processEvent(progress, evCmd);
  }


}
