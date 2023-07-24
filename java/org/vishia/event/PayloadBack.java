package org.vishia.event;

/**This class offers a simple implementation for {@link Payload} which can be used for a simple callback.
 * Usual this class should be inherit for more data.
 * That's why the implementation of {@link #serialize()} and {@link #deserialize(byte[])} is not sensible implemented.
 * @author Hartmut Schorrig
 *
 */
public class PayloadBack implements Payload {

  
  /**Version, license and history.
   * <ul>
   * <li>2023-07-24 Hartmut created. necessary for the universal approach for {@link EventConsumerAwait}
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
  public final static String version = "2023-07-24";

  
  /**This bit should be set if the back event should notify that the task is done with or without error.
   * For progress messages, quests etc. more data are necessary.
   */
  protected boolean bDone;
  
  /**A possible error message. If null then all should be seen as successfully.*/
  protected String sError;
  
  public void setDone(boolean bDone, String sError) { 
    this.sError = sError;
    this.bDone = bDone;
  }
  
  public String error() { return this.sError; }
  
  public boolean done() { return this.bDone; }
  
  @Override public PayloadBack clean () {
    this.bDone = false;
    this.sError = null;
    return this;
  }

  /**Serialize is not supported by this implementation. */
  @Override public byte[] serialize () {
    return null;
  }

  /**Deserialize is not supported by this implementation. */
  @Override public boolean deserialize ( byte[] data ) {
    return false;
  }

}
