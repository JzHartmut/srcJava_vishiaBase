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

import java.net.*;

/** 
 * Implementation of Adress_InterProcessComm represents a java.net.socketAddr
 * for socket communication.
 * In C: This class is defined forward in the headerfile, the implementation is only visible here.
*/
public class Address_InterProcessComm_Socket 
implements Address_InterProcessComm
{

  /**Version, history and license.
   * <ul>
   * <li>2013-12-05 Hartmut chg: {@link #toString()} does not returns the hostName, this needs communication 
   *   and a DNS. Instead it returns the address (ip-number)
   * <li>2005-01-01 Hartmut created.
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
   * If you are intent to use this sources without publishing its usage, you can get
   * a second license subscribing a special contract with the author. 
   * 
   * @author Hartmut Schorrig = hartmut.schorrig@vishia.de
   * 
   */
  //@SuppressWarnings("hiding")
  public final static int version = 0x20131206;

   @Override
  public String toString() 
   {
       String str = socketAddr.getAddress() + ":" + socketAddr.getPort();
       return str;
   }
  private InetSocketAddress socketAddr;
  
  /**UDP, TCP
   * 
   */
  private final String type;

  /** Empty constructor to call setLocalHostPort later*/
  public Address_InterProcessComm_Socket()
  { type="empty";
    socketAddr = null;
  }
  
  
  /** Constructor with full destination address given as 32-bit-integer value.
   * @param addr The inet destination adress, Hi bytes are left, example 192.168.10.1 is 0xc0a80a01
   * @param port The port of destination. 
   * */
  public Address_InterProcessComm_Socket(String type, int addr, int port)
  { this.type = type;
  	byte[] byteAddr = new byte[4];
    byteAddr[0] = (byte)((addr >> 24) & 0xff);
    byteAddr[1] = (byte)((addr >> 16) & 0xff);
    byteAddr[2] = (byte)((addr >>  8) & 0xff);
    byteAddr[3] = (byte)((addr >>  0) & 0xff);
    InetAddress inetAddr;
    try{ inetAddr = InetAddress.getByAddress(byteAddr); }
    catch(UnknownHostException excption)
    { inetAddr = null;
    }
    socketAddr = new InetSocketAddress(inetAddr, port);
    
  }

  
  /**Constructor with address given as String.
   * @param addr The address inclusively type given in string format with port. Example "UDP:192.168.10.1:1234" 
   *   The type and the port is separated by ':'.
   * */
  public Address_InterProcessComm_Socket(String addr)
  { this(null, addr, -1);
  }

  /**Constructor with type, address and port.
   * @param type The type should be "UDP", "TCP" or it may be null. If the type is null, the type is gotten
   *   from the addr-parameter as string-part before a ':'.
   * @param addr The address. If type and port are given, it is a String which is compatible to
   *   java.net.InetSocketAddress. Usual it is a String with 4 octets with numbers
   *   like "192.168.1.1". If the type is not given, the type is contained in this parameter
   *   before a ':'. If the port is not given the port is the integer number after a trailing ':'.
   * @param port A positive value for the Port number or -1 if the port is contained in addr.
   */
  public Address_InterProcessComm_Socket(String type, String addr, int port)
  { if(type == null){
  	  int posAddr = addr.indexOf(':');
  	  this.type = addr.substring(0, posAddr);
  	  addr = addr.substring(posAddr+1);
    } else {
  	  this.type = type;
    }
		if(port <=0){
	  	int posPort = addr.indexOf(':');
	  	if(posPort <0) throw new IllegalArgumentException
	  		("param addr needs a port in form \"UDP:URL:port\" where port is a number or hexNumber with \"0x\"-prefix.");
	  	if(addr.substring(posPort+1).startsWith("0x")){
			  port = Integer.parseInt(addr.substring(posPort+3),16);
			} else {
			  port = Integer.parseInt(addr.substring(posPort+1));
	  	}
			addr = addr.substring(0, posPort);
  	}	
  	socketAddr = new InetSocketAddress(addr, port);
    
  }


  /** Creates an destination adress at local host
    * @param port the portnumber at localhost
    */
  public void setLocalHostPort(int port)
  { socketAddr = new InetSocketAddress("localhost", port);
  }

  
  /**Sets an receive socket on any local IP-Address.
   * Note: It uses the IP-wildcard address internally. It is 0.0.0.0 usually. Drivers listen
   * at any IP-Hardware inclusive the local-host-software-interface.  
   */
  public void setReceiveSocket(int port)
  {
    socketAddr = new InetSocketAddress(port);
  }
  
  
  

  /** gets the pointer to the socket address. This methods are only usefully 
   * for the implementation of the InterProcessComm, therefore package-private.
   */
  InetSocketAddress getSocketAddress()
  {
    return socketAddr;
  }

  
  /** store the socket Address into, only usefully if it is a SenderAddressInterProcessComm.
   * It is used only package-private for Implementation of the {@link InterProcessComm_SocketImpl}
   * @param sender The sender address.
   */
  void storeSender(SocketAddress sender)
  {
    socketAddr = (InetSocketAddress) sender; 
  }
  
  
  //private int getSocketAddressSize(){ return sizeof(socketAddr); }


};


