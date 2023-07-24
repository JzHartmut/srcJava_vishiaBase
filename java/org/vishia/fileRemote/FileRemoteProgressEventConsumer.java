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
public class FileRemoteProgressEventConsumer extends EventConsumerAwait<FileRemoteProgressEvData, FileRemoteCmdEventData> {

  
  /**Version, history and license.
   * <ul>
   * <li>2023-07-24 refactored: renamed from FileRemoteProgress because the name has not show, it is a consumer.
   *   Some features are now contained in {@link EventConsumerAwait} in a common kind instead here.  
   * <li>2023-07-22 Hartmut new: now contains also the progressData and cmdData as reference and the event itself.
   *   but with change on 2023-07-24 consequently in the {@link EventConsumerAwait} super class.
   *   Hence it is a complete usable class for one event handling. 
   *   This class is then only a concretion with non generics.
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

  
  
  
  /**Number of available directories and files, filled on check. */
  public int nrDirAvail, nrFilesAvail;
  
  public long nrofBytesAllAvail;

  
  
  
  /**Constructs the FileRemoteProgress instance maybe also inherit.
   * @param name A name
   * @param progressThread The thread where {@link EventThread_ifc#storeEvent(EventObject)} is called to store the event if it is used.
   * @param cmdThread if given the using command is executed in this thread.
   *   If null then the command is executed in the own thread which calls the command. 
   */
  public FileRemoteProgressEventConsumer(String name, EventThread_ifc progressThread, EventThread_ifc cmdThread) {
    super(name, new FileRemoteProgressEvData(), progressThread, new FileRemoteCmdEventData(), cmdThread);
  }



  @Override public FileRemoteProgressEventConsumer clean( ) {
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
  
  
  

}
