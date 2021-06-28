package org.vishia.communication;

import java.io.Closeable;

/**Class using the {@link InterProcessComm} interface usual with socket communication
 * to transmit cmd with arguments and  wait for a receiving cmd
 * It is an opposite to the emC/Socmd/socketcmd.cpp to communicate 
 * @author Hartmut Schorrig
 * @copyright LPGL do not remove author and copyright
 * @since 2021-06
 *
 */
public class SocketCmd_InterProcessComm implements Closeable {

  String state = "inactive";
  
  
  
  int nState = 0;
  public final static int mOpened = 1;
  public final static int mReceiving = 2;
  public final static int mError = 8;
  
  
  
  InterProcessComm_SocketImpl ipc;
  
  
  
  Address_InterProcessComm ownAddress;
  
  Address_InterProcessComm dstAddress;
  
  Address_InterProcessComm lastSender;
  
  public SocketCmd_InterProcessComm(String sAddr, String sAddrDst) {
    Address_InterProcessComm_Socket soAddr = new Address_InterProcessComm_Socket(sAddr);
    this.ipc = new InterProcessComm_SocketImpl(soAddr);
    this.ownAddress = this.ipc.createAddress(sAddr);
    if(sAddrDst !=null) {
      this.dstAddress = this.ipc.createAddress(sAddrDst);
    }
    int ok = this.ipc.open(this.ownAddress, true);
    if(ok ==0) {
      this.state = "open " + sAddr;
      this.nState = mOpened; //opened
    } 
    else {
      this.state = this.ipc.getReceiveErrorMsg(true);
      System.err.println(this.state);
      this.ipc.close();
      this.nState = mError;  //failed open
    }
  }
  
  
  
  /**Transmit this cmd.
   * Separate arguments with spaces, write arguments in "" if have spaces or other special chars into
   * @param cmd
   */
  public void tx(String cmd) {
    int nBytes = cmd.length();
    byte[] data = new byte[nBytes];
    for(int ix = 0; ix < nBytes; ++ix) {
      data[ix] = (byte)cmd.charAt(ix);  //ignore special chars, only ASCII
    }
    int ok = ipc.send(data, nBytes, this.dstAddress);
    if(ok <0) {
      throw new IllegalArgumentException("tx");
    }
  }
  
  
  /**Waits and returns the cmd */
  public String waitRx() {
    
    int[] nrofBytesRx = new int[1];
    this.state = "waiting rx";
    this.nState |= mReceiving;  //wait for rx
    byte[] data = ipc.receive(nrofBytesRx, lastSender);
    this.nState &= ~mReceiving;
    if(data !=null) {
      StringBuilder sb = new StringBuilder(nrofBytesRx[0]);
      for(int ix = 0; ix < nrofBytesRx[0]; ++ix) {
        sb.append((char)(data[ix]));
      }
      this.state = "rx ok";
      this.nState = 4;  //rx ok
      return sb.toString();
    }
    this.state = "rx abort";
    System.err.println("Receive abort");
    this.nState |= mError; //rx abort
    return "null";
  }
  
  
  
  
  
  /**Get the state as number
   * <ul>
   * <li>bit 0x1 opened
   * <li>bit 0x2 receiving (waiting)
   * <li>bit 0x8 error
   * @return 
   */
  public int getNumState() { return this.nState; }
  
  public boolean hasError() { return (this.nState & mError) !=0; }
  
  
  public String getState() { return this.state; }
  
  @Override public void close() {
    this.ipc.close();
    this.state = "closed, can only be removed";
  }
  
  
  
  @Override public String toString() {
    return "org.vishia.communication.SocketCmd_InterProcessComm " + this.state;
  }
}
