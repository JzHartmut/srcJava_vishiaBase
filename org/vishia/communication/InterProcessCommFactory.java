package org.vishia.communication;

/**This class is used as interface and as singleton instance access for factory of any InterProcessComm.
 * An derived instance of this class which offers all necessary types of InterProcessComm should be created
 * on start of an application. See {@link InterProcessCommFactorySocket}. For example: 
 * <pre>
 *   new InterProcessCommFactorySocket();  //creates a singleton.
 * </pre>
 * This instance can be used with the following pattern: 
 * <pre>
 *   InterProcessCommFactory.singeton().create("UDP:127.0.0.1:60100");
 * </pre>
 * This example creates an InterProcessComm instance which uses an Socket-UDP-Protocoll on port 60100 on local host (loop back).
 * <br><br>
 * A further instance can be created, for example for test reasons. Then an association need to be set:
 * <pre>
 *   InterProcessCommFactory ipcFactory = new InterProcessCommFactorySocket();  //creates a singleton.
 *   .....
 *   ipcFactory.create("UDP:127.0.0.1:60105");
 * </pre>
 *    
 * @author Hartmut Schorrig
 *
 */
public abstract class InterProcessCommFactory
{

    /**Version, history and license. This String is visible in the about info.
   * <ul>
   * <li>2015-08-16 JcHartmut: Define as abstract class instead interface without necessity of changing an application,
   *   join with InterProcessCommFactoryAccessor, no need of 2 classes.
   * <li>2016-2011 some changes, see source files.
   * <li>2006-00-00 JcHartmut created
   * </ul>
   * 
   * <b>Copyright/Copyleft</b>:<br>
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
   *    but doesn't need to publish the whole source of the own product.
   * <li> You can study and modify (improve) this source
   *    for own using or for redistribution, but you have to license the
   *    modified sources likewise under this LGPL Lesser General Public License.
   *    You mustn't delete this Copyright/Copyleft inscription in this source file.
   * </ol>
   * If you intent to use this source without publishing its usage, you can get
   * a second license subscribing a special contract with the author. 
   * 
   * @author Hartmut Schorrig = hartmut.schorrig@vishia.de
   */
  //@SuppressWarnings("hiding")
  public static final String sVersion = "2015-08-16";

  
  private static InterProcessCommFactory theSingleton;
  
  
  /**Sets {@link #theSingleton} if not set yet. returns always a new instance. 
   * 
   */
  protected InterProcessCommFactory() 
  {
    if(theSingleton ==null){
      theSingleton = this;
    } else {
      System.out.println("Not set as singleton, it is set already. This is a second instance");
    }
  }
  
  
  
  /**Gets the instance of the {@link InterProcessCommFactory} for this application.
   * @return null if nothing was intialized. Note that an derived class of this need to be create on start of the application.
   */
  public static InterProcessCommFactory getInstance()
  { return theSingleton;  
  }
  


  /**Creates an address for a specific communication channel. It can be used for {@link #create(Address_InterProcessComm)}.
   * @param protocolAndOwnAddr String determines the channel. For example "UDP:0.0.0.0" to create a socket communication.
   * @param nPort Numeric value for the fine definition. Port number for Socket.
   * @return The address or null if not successfully.
   */
  public abstract Address_InterProcessComm createAddress(String protocolAndOwnAddr, int nPort);
  
  /**Creates an address for a specific communication channel. It can be used for {@link #create(Address_InterProcessComm)}.
   * @param protocolAndOwnAddr String determines the channel. For example "UDP:0.0.0.0:6000" to create a socket communication.
   * @return The address or null if not successfully.
   */
  public abstract Address_InterProcessComm createAddress(String protocolAndOwnAddr);
  
	/**Creates a InterProcessComm from a parameter String. The type depends on this String.
   * For example:
   * <ul>
   * <li>"UDP:192.16.35.3" for UDP via socket. Don't write spaces, set the port after ':'
   * </ul> 
   * @param protocolAndOwnAddr A string which determines the kind of communication and the own address (slot).
   *                           It depends on the underlying system which kind of communication are supported
   * @return null or an instance maybe with opened communication.
   */
  public abstract InterProcessComm create(String protocolAndOwnAddr, int nPort);
  
  /**Creates an instance of the InterProcessComm for the given communication implementation.
   * @param protocolAndOwnAddr It determines the communication channel. Use "UDP:0.0.0.0:6000" for example 
   *   to create an UDP-Socket communication which uses the port 6000 on all existing network adapters.
   * @return
   */
  public abstract InterProcessComm create(String protocolAndOwnAddr);
  
  
  /**Creates an instance of the InterProcessComm for the given communication implementation.
   * @param addr The own address, see {@link #createAddress(String)} and {@link #createAddress(String, int)}.
   * @return the instance, null if not sucessfully.
   */
  public abstract InterProcessComm create(Address_InterProcessComm addr);
  
  
}
