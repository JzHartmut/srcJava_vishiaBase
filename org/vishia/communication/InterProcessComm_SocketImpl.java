/****************************************************************************
 * Copyright/Copyleft:
 *
 * For this source the LGPL Lesser General Public License,
 * published by the Free Software Foundation is valid.
 * It means:
 * 1) You can use this source without any restriction for any desired purpose.
 * 2) You can redistribute copies of this source to everybody.
 * 3) Every user of this source, also the user of redistribute copies
 *    with or without payment, must accept this license for further using.
 * 4) But the LPGL is not appropriate for a whole software product,
 *    if this source is only a part of them. It means, the user
 *    must publish this part of source,
 *    but don't need to publish the whole source of the own product.
 * 5) You can study and modify (improve) this source
 *    for own using or for redistribution, but you have to license the
 *    modified sources likewise under this LGPL Lesser General Public License.
 *    You mustn't delete this Copyright/Copyleft inscription in this source file.
 *
 * @author Hartmut Schorrig: hartmut.schorrig@vishia.de, www.vishia.org
 * @version 0.93 2011-01-05  (year-month-day)
 *******************************************************************************/ 
package org.vishia.communication;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
//import java.net.*;
import java.net.InetSocketAddress;





/** This class implements the interprocess communication for mailbox system with UDP-IP telegrams on Windows.
*/
public class InterProcessComm_SocketImpl implements InterProcessComm
{

	

  private static final int kDataBufferSize = 1500;

  private final Address_InterProcessComm_Socket ownAddress;
  
  /** The UDP Socket handling as receiver */
  private DatagramSocket udpSocket;

  private String sRxErrorMsg;

  private String sTxErrorMsg;
  


  public InterProcessComm_SocketImpl(Address_InterProcessComm ownAddress)
  { assert(ownAddress instanceof Address_InterProcessComm_Socket);
  	this.ownAddress = (Address_InterProcessComm_Socket)ownAddress;
  }


  /***********************************************************************/
  /* Implements InterPorcessComm.open(): Opens the communication for sending 
   * and receiving. 
   * @param ownAddress The own adress, it is the port received
   * @return null if success, an readable error message if it fault.
  */
  @Override public int open(final Address_InterProcessComm dstAddress, boolean shouldBlock)
  { int error = 0;
    Address_InterProcessComm_Socket ownAddress1 = this.ownAddress; 
    /*
    switch(mode)
    { case receiverShouldbeBlocking: break; //its ok
      case receiverShouldbePolling:
      { sRxErrorMsg = sTxErrorMsg = "InterProcessComm_Socket.open(): The polling mode is not supported yet.";
        error = -1;
      }
      default:
      { //it is an software error, throw it!
        throw new RuntimeException("InterProcessComm_Socket.open(): Fault mode value=" + mode);
      }
    }
    */
    if(!shouldBlock){
      this.sRxErrorMsg = this.sTxErrorMsg = "InterProcessComm_Socket.open(): The polling mode is not supported yet.";
      error = -1;
    } else {
	    InetSocketAddress ownAddressSocket = ownAddress1.getSocketAddress();
	    try
	    { this.udpSocket = new DatagramSocket(ownAddressSocket);
	    this.sRxErrorMsg = null;
	    }
	    catch (SocketException e)
	    { //note: getHostString not visible on Java 6
	      //sRxErrorMsg = e.getMessage() + "ipaddress=" + ownAddressSocket.getHostString() + " port=" + ownAddressSocket.getPort();
	      this.sRxErrorMsg = e.getMessage() + "ipaddress= ..." + " port=" + ownAddressSocket.getPort();
        error = -1;
	    }
    }
    return error;
  }


  public int close()
  { if(this.udpSocket != null)
    { this.udpSocket.close();
      this.udpSocket = null;
    }
    return 0;
  }


  public int send(final byte[] data, int nBytes, final Address_InterProcessComm addresseeP)
  { Address_InterProcessComm_Socket addressee = (Address_InterProcessComm_Socket) addresseeP; 
    try
    { if(udpSocket !=null){ //may be closed in another thread.
        DatagramPacket dataOut = new DatagramPacket(data, nBytes, addressee.getSocketAddress());
        this.udpSocket.send(dataOut);
      } else {
        nBytes = -1;
      }
    }
    catch (SocketException e)
    { this.sTxErrorMsg = e.getMessage();
      nBytes = -1;
    } 
    catch (IOException e)
    { this.sTxErrorMsg = e.getMessage();
      nBytes = -1;
    } 
    return nBytes;
  }


  @Override public byte[] receive(int[] result, Address_InterProcessComm senderP)
  { boolean bOk = true;
    if(this.udpSocket == null)
    {
      return null;
    }
    else
    { Address_InterProcessComm_Socket sender = (Address_InterProcessComm_Socket)(senderP);
      byte[] dataBuffer = new byte[kDataBufferSize];
      DatagramPacket datagramPacket = new DatagramPacket(dataBuffer, kDataBufferSize);
      try{ this.udpSocket.receive(datagramPacket); }
      catch(IOException exception)
      { bOk = false;
        result[0] = -1;
        dataBuffer = null;
      } //try it again.
      if(bOk)
      {
        result[0] = datagramPacket.getLength();
        if(sender != null)
        { sender.storeSender(datagramPacket.getSocketAddress());
        }
      }
      return dataBuffer;
    }
  }


  public void freeData(byte[] data)
  { //This method is left empty because the garbage collector deletes unneccerry data.
  }


  @Override public boolean equals(final Address_InterProcessComm address1, final Address_InterProcessComm address2)
  {
    return false;
  }




  public String getReceiveErrorMsg(boolean clearIt)
  {
    return this.sRxErrorMsg;
  }




  public String getSendErrorMsg(boolean clearIt)
  {
    return this.sTxErrorMsg;
  }


	@Override
	public int abortReceive() {
		// TODO Auto-generated method stub
		return 0;
	}


	@Override
	public int capacityToSendWithoutBlocking(int nrofBytesToSend) {
		// TODO Auto-generated method stub
		return 0;
	}


	@Override
	public int checkConnection() {
		// TODO Auto-generated method stub
		if(this.udpSocket == null){ return -1; }
		return 0;
	}


	@Override
	public int dataAvailable() {
		// TODO Auto-generated method stub
		return 0;
	}



	@Override
	public int flush() {
		// TODO Auto-generated method stub
		return 0;
	}




	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public Address_InterProcessComm getOwnAddress() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
	public byte[] getSendBuffer(int len) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override	public byte[] receiveData(int[] result, byte[] bufferP,	Address_InterProcessComm senderP) 
	{
	  boolean bOk = true;
    if(this.udpSocket == null)
    {
      return null;
    }
    else
    { Address_InterProcessComm_Socket sender = (Address_InterProcessComm_Socket)(senderP);
      byte[] dataBuffer = bufferP != null ? bufferP : new byte[kDataBufferSize];
      DatagramPacket datagramPacket = new DatagramPacket(dataBuffer, dataBuffer.length);
      try{ this.udpSocket.receive(datagramPacket); }
      catch(IOException exception)
      { bOk = false;
        if(result != null) { result[0] = -1; }
        dataBuffer = null;
      } //maybe try it again with later call
      if(bOk)
      {
        if(result !=null){ result[0] = datagramPacket.getLength(); }
        if(sender != null)
        { sender.storeSender(datagramPacket.getSocketAddress());
        }
      }
      return dataBuffer;
    }
  }


	@Override
	public String translateErrorMsg(int nError) {
		// TODO Auto-generated method stub
		return null;
	}


	@Override public Address_InterProcessComm createAddress()
	{
		return new Address_InterProcessComm_Socket();
	}

	@Override public Address_InterProcessComm createAddress(int p1, int p2)
	{
		return new Address_InterProcessComm_Socket("Socket", p1, p2);
	}

	
	public Address_InterProcessComm createAddress(String p1, int p2)
	{
		return new Address_InterProcessComm_Socket("Socket", p1, p2);
	}

	public Address_InterProcessComm createAddress(String address)
	{
		return new Address_InterProcessComm_Socket(address);
	}

	
	

}






