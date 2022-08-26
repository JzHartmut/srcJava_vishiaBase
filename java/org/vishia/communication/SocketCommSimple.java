package org.vishia.communication;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

/**This is a simple wrapper arround the basically network classes in java.net
 * for simple UDP communication. 
 * <br>
 * In opposite the {@link InterProcessComm} concept allows more flexibility to select the communication kind.
 * This is only for socket and UDP.
 * @author Hartmut Schorrig
 *
 */
public class SocketCommSimple {

  /**Version and history
   * <ul>
   * <li>2012-08 created
   * </ul>
   */
  public static final String version = "2022-08-26";

	
  DatagramSocket so;
  
  SocketAddress ownAddr, dstAddr;
  

  /**Builds the InetAddr.
   * @param addr The IpV4 address High byte is left, for example 192.168.1.3 is 0xc0a80103 
   * @param port The port 16 bit valid
   * @return proper InetAddress
   */
  InetAddress buildInetAddr(int addr, int port) {
    byte[] byteAddr = new byte[4];
    byteAddr[0] = (byte)((addr >> 24) & 0xff);
    byteAddr[1] = (byte)((addr >> 16) & 0xff);
    byteAddr[2] = (byte)((addr >>  8) & 0xff);
    byteAddr[3] = (byte)((addr >>  0) & 0xff);
    InetAddress inetAddr;
    try{ inetAddr = InetAddress.getByAddress(byteAddr); }
    catch(UnknownHostException exc)
    { System.err.println(exc.getMessage());
      inetAddr = null;
    }
    return inetAddr;
  }
  
  
  
  /**Opens a port for UDP communication. Sets the internal {@link #so} variable. 
   * @param addr The IpV4 address High byte is left, for example 192.168.1.3 is 0xc0a80103 
   * @param port The port 16 bit valid
   * On any error {@link #so} remains null and an error message is outputted on {@link System#err}.
   */
  public void open ( int addr, int port) {
    InetAddress inetAddr = buildInetAddr(addr, port);
    this.ownAddr = new InetSocketAddress(inetAddr, port);
    try {
      this.so = new DatagramSocket(this.ownAddr);
    } catch (SocketException exc) {
      System.err.println(exc.getMessage());
      this.so = null;
    }
  }

  
  
  /**Sets the destination for the next following telegrams for {@link #tx(byte[], int)}
   * @param addr The IpV4 address High byte is left, for example 192.168.1.3 is 0xc0a80103 
   * @param port The port 16 bit valid
   */
  public void setDst ( int addr, int port) {
    InetAddress inetAddrDst = buildInetAddr(addr, port);
    this.dstAddr = new InetSocketAddress(inetAddrDst, port);
  }  
  
  
  /**Closes the socket. A waiting receive action is aborted then.
   * 
   */
  public void close ( ) {
    this.so.close();
    this.so = null;
  }

  /**Transmit a data packet to the given {@link #setDst(int, int)}
   * @param data 
   * @param zdata length of data to transmit. data must have at least this length.
   * @throws IOException
   */
  public void tx(byte[] data, int zdata) {
    DatagramPacket telg = new DatagramPacket(data, zdata, this.dstAddr);
    try {
      this.so.send(telg);
    } catch (IOException exc) {
      System.err.println(exc.getMessage());
    }  
  }
	
  
  /**Transmit a String, but only as ASCII, one character is one byte.
   * @param msg
   */
  public void tx ( String msg) {
    byte[] bu = new byte[msg.length()];
    for(int ix = 0; ix < msg.length(); ++ix) {
      bu[ix] = (byte)msg.charAt(ix);
    }
    tx(bu, bu.length);
  }
  
  
  
  /**Receive operation. The operation waits for a incomming telegram (block) but aborts on close.
   * @param rxBuffer
   * @return number of bytes or -1 on error.
   * On any error a message is output on {@link System#err}.
   */
  public int rx ( byte[] rxBuffer ) {
    int zRxBuffer = rxBuffer.length;
    DatagramPacket telg = new DatagramPacket(rxBuffer, zRxBuffer);
    int zRx;
    try {
      this.so.receive(telg);
      zRx = telg.getLength();
    } catch (IOException exc) {
      System.err.println(exc.getMessage());
      zRx = -1;
    }
    return zRx;
  }
  
  
  
  
  
  
  
  
}
