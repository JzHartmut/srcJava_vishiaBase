package org.vishia.communication;




/**This interface is the applicable interface to realize a communication between processes at the same processor board
 * or between processes via network using UDP/IP.
 * In generally, realization of this class is hardware-depended.
 * An implementation with socket communications (using java.net.Socket*) exists.
 * But the user can realize its own solutions based on this interface, at example
 * with using dual port RAM-communication at system level.
 * <br><br>
 * Hint for Java4C-usage: In C this interface is existing too, see CRuntimeJavalike/Include/Ipc/InterProcessComm.h.
 * For Java2C-translating the CRuntimeJavalike/stc/Ipc.stc file describes this class. This class is not translated by iteself.
 */
public interface InterProcessComm
{
  /**version, history and license:
   * <ul>
   * <li>2012-08-22 Hartmut chg The constants {@link #receiverShouldbeBlocking} /Polling should be boolean because
   *   the parameter of open is boolean. The might be never used ?
   * <li>2011-01-05 Hartmut created
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
   * 
   */
  public final static int version = 20120820;
  /** Mode of receiving, determined on open(). The receive()-call either blocks 
   * if no data are available (it should be called in an extra thread), 
   * or it returns a nullpointer, if no data are available. This is a polling manner.
   */
  public static boolean receiverShouldbeBlocking = true
                  , receiverShouldbePolling  = false;
  
  /** Opens the Communication. Only called at startup.
      @param dstAddress The own address to which this interprocess-port is assigned. 
             This address is also used
             when calling send() as information about the sender of the data.
      @param mode true if the receiver can block if receivdata are not available but the connection is established.
        false then a call of {@link #receive(int[], Address_InterProcessComm)} or {@link #receiveData(int[], byte[], Address_InterProcessComm)}
        does never block. Use the {@link #receiverShouldbeBlocking} or {@link #receiverShouldbePolling} values. 
      @return true if success, false if it failed. Use getReceiveErrorMsg() to detect the cause manually.
  */
  public int open(final Address_InterProcessComm dstAddress, boolean shouldBlock);


  /** Closes the mailbox, only to be called on shutdown of the application.
  */
  public int close();


  /**Sends Data.
     The destination adress is defined on open.
     If a listener is associated with this class, the data will be forwarded to the users evaluation method
     defined in the implementation of the listener. So a evaluation may be execute directly without consideration of the users software
     and without consideration of the realization in this class.
     If no listener is associated, the data will be sent in the kind of realization of this class.
     @param data The data to be sent. The user should not touch the buffer after calling here.
     @param nBytes The number of bytes to be send.
     @param addressee The addressee, the receiver of the message. If it is a fix connection (TCP),
                      then this parameter should be null. It isn't used. The addressee is given
                      on {@link #open(Address_InterProcessComm, int)}-request. 
     @return number of Bytes sent, if >=0. If <0 its an error code.

   */
  public int send(final byte[] data, int nBytes, final Address_InterProcessComm addressee);



	/** Receives a Datagram.
	This method blocks until a Datagram is received.
	If a Datagram is received, the content of the Datagram will copied into the appropriated buffer.
	@param data out: 
	@param sender Buffer for the sender of the message.
	@param result result[0] = Nr of Bytes sendet, if >=0. If <0 its an error code.
	@return The reference to the buffer containing data.
	       On error, the content of this reference is not valid, but the user must call relinguishData() with this reference .
	*/
	public byte[] receive(int[] result, Address_InterProcessComm sender);
	
	/** Receives a Datagram.
	 * This method blocks until a Datagram is received.
	 * If a Datagram is received, the content of the Datagram will copied into the appropriated buffer.
	 * @param data out: 
	 * @param sender Buffer for the sender of the message.
	 * @param nrofBytes int-variable, which contains the number of bytes
	 *        * INPUT: if 0, than the greatest available data block is received and returned.
	 *        * INPUT: if >0, than exact the requested nr of bytes are returned. 
	 *          In non-blocking mode, null is returned, if bytes are available, but to less bytes.
	 *        * OUTPUT: <0, than an error is occured.
	 *        * OUTPUT: >=0, than the number of bytes is returned. 
	 * @param buffer If not null, than this given buffer is used to copy to it. 
	 *             In this case freeData() mustn't call.
	 *             If null, than the internal buffer is returned. {@link #freeData(byte[]} 
	 *             have to be called with given return value after processing the data.
	 * @param buffer given buffer to fill it, 
	 * @return The reference to the buffer containing data.
	*/
	/**
	 * @param nrofBytes
	 * @param buffer
	 * @param sender
	 * @return
	 */
	public byte[] receiveData(int[] nrofBytes, byte[] buffer, Address_InterProcessComm sender);


  /** Relinguishes the data buffer, after it is evaluated.
      @param data The buffer delivered from receive!
  */
  public void freeData(byte[] data);


  /** Checks the connection. This is the alternativ method for polling
   * instead of the callback methods errorConnection() and readyConnection().
   * @return value less 0: error code, the connection is not available.
   *         If the value is >=0, the connection is established.
   *
   */
  public int checkConnection();

  
  /**Tests wether a next send is done without blocking.
   * If the transfer medium supports this test and it has a limited capacity, the method is helpfull to use.
   * If the medium doesnot support such quest, the nrofBytes is returned. It means, it has a capacity. send is possible
   * but it is not guarented that the send doesnot block.
   * @return nrofBytes of capacity to send without blocking, but not greater than argument nrofBytesToSend.
   *         0 if the next send call will be blocked.
   *         negativ value if any error.
   */
  public int capacityToSendWithoutBlocking(int nrofBytesToSend);

  
  /** Tests wether data are available to receive. This is the alternativ method for polling
   * instead of the callback methods dataAvailable() from the callback interface.
   * This method can return 0 also if data are receiveable, if the implementation donot support such quest.
   * @return value less 0: error code, the connection is not available.
   *         If the value is >0, data are available. If 0, no data are available.
   *
   */
  public int dataAvailable();

  /** Abort receiving function
   */
  public int abortReceive();

  public String getName();

  /**Gets the own address of this interProcessComm, it are the receive parameters.
   * The knowledge of receive address may be necessary at user level
   *   if the information about a other receive adress should be communicate to the partner.
   * In most cases the partner knows the sender adress (back-info-argument of receive) from the implementation level,
   *   but in some cases (2-simplex-Ports TCP/UDP) the receive adress is the address getted here.
   */
  public Address_InterProcessComm getOwnAddress();

  
  
  /**Creates an empty instance for a address information, especially for the sender. 
   * @return An empty new instance apropriate to the kind of communication.
   */
  Address_InterProcessComm createAddress();

  Address_InterProcessComm createAddress(int p1, int p2);
  
  Address_InterProcessComm createAddress(String p1, int p2);
  
  Address_InterProcessComm createAddress(String address);
  
  /** Compare the address to another address, returns true if it is the same address.
   * This method can used to compare a known address with a sender address from InterProcessComm::receive().
   * @param address1 The address to compare.
   * @param address2 The address to compare.
   * return true if the addresses describes the same communication slot. On Pc that is the same IP-Address and the same port.
  */
  public boolean equals(Address_InterProcessComm address1P, Address_InterProcessComm address2P);

  public String translateErrorMsg(int nError);

  /**Flushs send data to destination. */
  public int flush();

  /**Gets a buffer for send. */
  public byte[] getSendBuffer(int len);







}
