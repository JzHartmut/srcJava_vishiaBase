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

/**
 * @author Hartmut Schorrig
 *
 */
public class InterProcessCommFactorySocket extends InterProcessCommFactory
{
	static{
	  //the constructor does it:
		//setSingleton(new InterProcessCommFactorySocket());
	}
	
	
  public InterProcessCommFactorySocket(){
    super();  //it sets the singleton.
  }


	@Override public InterProcessComm create(String protocolAndOwnAddr)
	{ InterProcessComm obj = create(protocolAndOwnAddr, -1);
	  return obj;
	}
	

 /**Creates a InterProcessComm from a parameter String. The type depends on this String.
  * For example:
  * <ul>
  * <li>"UDP:192.16.35.3:1234" for UDP via socket. Don't write spaces, set the port after ':'
  * </ul> 
  * @param protocolAndOwnAddr A string which determines the kind of communication and the own address (slot).
  *                           It depends on the underlying system which kind of communication are supported
  * @return null or an instance maybe with opened communication.
  */
 @Override public InterProcessComm create(String protocolAndOwnAddr, int nPort)
 { final InterProcessComm ipc;
   int posSocketAddr = -1;
 	if(protocolAndOwnAddr.startsWith("UDP:")){ posSocketAddr = 4; }
 	else if(protocolAndOwnAddr.startsWith("Socket:")){ posSocketAddr = 7; }
 	if(posSocketAddr >=0){
 		Address_InterProcessComm ownAddr = createAddressSocket(null, protocolAndOwnAddr, nPort);
 		ipc = new InterProcessComm_SocketImpl(ownAddr);
 	} else {
 		ipc = null;
 	}
 	return ipc;
 }
 
 
 @Override public InterProcessComm create(Address_InterProcessComm addr){
	 if(addr instanceof Address_InterProcessComm_Socket){
		 InterProcessComm obj = new InterProcessComm_SocketImpl(addr);
		 return obj;
	 } else {
		 throw new IllegalArgumentException("only Socket-Implementation, fault type of address:");
	 }
 }
 
 
 /**Creates an address information for the InterProcessComm from a parameter String. 
  * The inner type of the address determines which kind of InterprocessComm is matching for.
  * The inner type of the address depends on the given String. 
  * Therefore the kind of InterprocessComm is able to choice with a parameter String.
  * For example choice between UDP and a Dual-Port-Ram-Communication.
  * The maybe special implementation knows some formats and types.
  * <ul>
  * <li>"UDP:192.16.35.3:1234" for UDP via socket. Don't write spaces, set the port after ':'
  * </ul> 
  * @param protocolAndOwnAddr A string which determines the kind of communication and the address (slot).
  *        The general format is: <Type>:<AdressString>
  *        It depends on the underlying system which kind of communication are supported
  * @return null or an instance of the address.
  */
 @Override public Address_InterProcessComm createAddress(String protocolAndOwnAddr, int nPort)
 { final Address_InterProcessComm addr;
	  if(protocolAndOwnAddr.startsWith("Socket:")){
			String sAddr = protocolAndOwnAddr.substring(7);
			addr = createAddressSocket(null, protocolAndOwnAddr, nPort);
			
	  } else if(protocolAndOwnAddr.startsWith("UDP:")){
 		String sAddr = protocolAndOwnAddr.substring(4);
 		addr = createAddressSocket(null, protocolAndOwnAddr, nPort);
 		
	  } else if(protocolAndOwnAddr.startsWith("TCP:")){
 		String sAddr = protocolAndOwnAddr.substring(4);
 		addr = createAddressSocket(null, protocolAndOwnAddr, nPort);
 		
	  } else {
 		addr = null;
 	}
 	return addr;
 }
 
 
 
 /**Creates an address for InterProcesscommunication with given description.
  * @return An address. The implementation class depends from the protocolAndAddr.  
  */
 @Override public Address_InterProcessComm createAddress(String protocolAndAddr)
 { Address_InterProcessComm obj =  createAddress(protocolAndAddr, -1);
   return obj;
 }
 
 public static Address_InterProcessComm createAddressSocket(String type, String addr, int nPort)
 { if(type == null){
		  int posAddr = addr.indexOf(':');
		  type = addr.substring(0, posAddr);
		  addr = addr.substring(posAddr+1);
	  } 	
 	assert(addr.length()>0);
		if(nPort <=0){
	  	int posPort = addr.indexOf(':');
	  	if(posPort <0) throw new IllegalArgumentException
	  		("param addr needs a port in form \"URL:port\" where port is a number or hexNumber with \"0x\"-prefix.");
	  	if(addr.substring(posPort+1).startsWith("0x")){
			  nPort = Integer.parseInt(addr.substring(posPort+3),16);
			} else {
			  nPort = Integer.parseInt(addr.substring(posPort+1));
	  	}
			addr = addr.substring(0, posPort);
 	}	
 	
 	Address_InterProcessComm addrSocket = new Address_InterProcessComm_Socket(type, addr, nPort);
 	return addrSocket;	
 }
 
	

}
