package org.vishia.communication;

import org.vishia.util.Assert;
import org.vishia.util.Java4C;


/**This class provides a bundle with an InterProcessCommuniation and a receive thread.
 * On received telegrams it invokes the {@link InterProcessCommRx_ifc#execRxData(byte[], int, Address_InterProcessComm)}
 * which's instance should be given by construction.
 * <ul>
 * <li>Use the static method {@link #create(String, InterProcessCommRx_ifc)} or the constructor to create the instance
 *  with the given receive port and the given callback on received data.
 * <li>Invoke {@link #start()} to open the communication and start the receive thread. It returns false on failures, true on success.
 * <li>Invoke {@link #shutdown()} to close and finish the receive thread. 
 * <li>Use {@link #createDstAddr(String)} to create a destination address for the given InterProcessComm for sending activities.
 * <li>Use {@link #send(byte[], int, Address_InterProcessComm)} to send.  
 * </ul>
 * 
 * @author Hartmut Schorrig
 *
 *
 *
 */
public class InterProcessCommRxThread
{
  
  /**Version, history and license.
   * <ul>
   * <li>2015-06-13 Hartmut: Created especially for C-usage of InterProcessCommunication.
   *   It is derived from {@link org.vishia.inspectorTarget.Comm} which uses this class as super class yet.
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
  public static final String version = "2015-06-13";

  
  /**Reference to the execute routine on receiving data. */
  @Java4C.SimpleRef
  private final InterProcessCommRx_ifc execRxData;
  /**State of function.
   * <ul>
   * <li>o: opened
   * <li>e: error (open error)
   * <li>x: exit
   * <li>z: is exited.
   * </ul>
   * 
   */
  private char state;
  
  private boolean bEnablePrintfOnComm;
  
  /**@java2c=simpleRef. */
  private final InterProcessComm ipc;
  
  private int ctErrorTelg;
  
  /**@java2c=simpleRef. */
  private final Thread thread;
  
  /**@java2c=simpleArray. */
  private final int[] nrofBytesReceived = new int[1];
  
  /**Use a static receive buffer. It is important for C-applications. */
  @Java4C.SimpleArray
  private final byte[] data_rxBuffer = new byte[1500];
  
  /**For C: store the reference and length of the SimpleArray in the next structure. */
  @Java4C.PtrVal private final byte[] rxBuffer =  data_rxBuffer;
  
  /**@java2c=simpleRef. */
  private final Address_InterProcessComm myAnswerAddress;
  
  /**Creates the communication but does not open it yet. See {@link #start()}
   * The InterProcessComm interface implementation is got depending on
   * <ul><li>the ownAddrIpc-string
   * <li>the existing InterProcessComm-Implementation, which analyzes the address-string.
   * <ul>
   * It means, the communication is not determined from this implementation, it depends
   * on the parameter of the ownAddrIpc and the possibilities. 
   * @param ownAddrIpc The address String for receiving data. This String determines the kind of InterProcessComm.
   *   Use "UDP:127.9.0.1:6000" to open an UDP-Communication for this example via loop-back on port 6000.
   *   See {@link InterProcessCommFactory}, {@link InterProcessCommFactorySocket}.
   * @param execRxData aggregation of executer of all commands.
   */
  public InterProcessCommRxThread(String ownAddrIpc, InterProcessCommRx_ifc execRxData)
  { 
    this.execRxData = execRxData;
    /**use the existent factory, it is determined by linker or classLoader, which it is.
     * @java2c=dynamic-call. */
    InterProcessCommFactory ipcFactory = InterProcessCommFactory.getInstance();
    /**The interProcessCommunication, depends from the factory and the own Address.
     * It is not socket anyway.
     * @java2c=dynamic-call.  */
    InterProcessComm ipcMtbl = ipcFactory.create (ownAddrIpc);
    //create the correct type for the sender-adress.
    myAnswerAddress = ipcMtbl.createAddress();  //empty address for receiving and send back
    //create the receive-thread
    
    thread = new Thread(threadRoutine, "IpcRx");
    //set it to class ref.
    this.ipc = ipcMtbl;
  }
  
  
  /**Static method to create invokes the constructor.
   * @param ownAddrIpc
   * @param execRxData
   * @return
   */
  public static InterProcessCommRxThread create(String ownAddrIpc, InterProcessCommRx_ifc execRxData)
  {
    InterProcessCommRxThread obj = new InterProcessCommRxThread(ownAddrIpc, execRxData);
    return obj;
  }
  
  
  /**Create any destination address for the given InterprocessComm implementation. 
   * The Address should be stored in the users space and should be used for {@link #send(byte[], int, Address_InterProcessComm)}
   * @param sAddr Proper String to create the address, see {@link InterProcessComm#createAddress(String)}.
   *   Hint: For Socket communication it should not start with "UDP:", only the own address on constructor should start with the protocol.
   *   It should have the form "192.168.1.123:6000" with ip and port.
   * @return the address, null on failure.
   */
  public final Address_InterProcessComm createDstAddr(String sAddr){
    @Java4C.DynamicCall InterProcessComm mipc = this.ipc;
    return mipc.createAddress(sAddr);
  }
  
  
  private final boolean openComm(boolean blocking) {
    int ok;
    /**@java2c=dynamic-call. */
    InterProcessComm ipcMtbl = ipc; 
    //------------------------------------------->Open the communication:
    ok = ipcMtbl.open(null, blocking);
    state = (ok >=0 ? 'o' : 'e');
    if(ok <0 && !bEnablePrintfOnComm){
      String sError = ipcMtbl.translateErrorMsg(ok);
      System.out.format("\nopen fails: error, %d = %s\n", ok & 0x7fffffff, sError);
    }
    if(bEnablePrintfOnComm)
    { //only for debug:
      if(ok >=0)
      { System.out.print("\nopen RxThread-Communication ok\n"); 
      }      
      else
      { System.out.format("\nopen RxThread-Communication error: %d\n", -ok);
      }
    }
    return ok >=0;  //true if InterProcessComm.open() successfully
  }



  
  /**Start opens the InterProcessComm and starts the receiver thread.
   * @return true on success. On false the receiver thread was not started. Open error.
   */
  final public boolean start(){
    boolean bOk = openComm(true);
    if(bOk){
      thread.start();
    } 
    return bOk;
  }
  
  
  /**Send a telegram to the given dst. It delegates to {@link InterProcessComm#send(byte[], int, Address_InterProcessComm)}.
   * An address should be create using {@link #createDstAddr(String)}.
   * @param data
   * @param nrofBytesToSend
   * @param dstAddr
   * @return
   */
  public final int send(@Java4C.PtrVal byte[] data, int nrofBytesToSend, Address_InterProcessComm dstAddr){
    @Java4C.DynamicCall InterProcessComm mipc = this.ipc;
    return mipc.send(data, nrofBytesToSend, dstAddr);
  }
  
  

  private final void runThread()
  { while(state != 'x'){
      if(state == 'o'){
        receiveAndExecute();
      } else {
        state = 'E';
        while(state == 'E'){
          try{ Thread.sleep(1000); } catch(InterruptedException exc){}
          //check state after a waiting time, repeat open.
        }
      }
    }
    synchronized(this){
      state = 'z';
      notify();
    }
    
  }

  
  private final void receiveAndExecute()
  {
    /**@java2c=dynamic-call. */
    @Java4C.DynamicCall final InterProcessCommRx_ifc execRxDataMtbl = this.execRxData; 
    /**@java2c=dynamic-call. */
    @Java4C.DynamicCall final InterProcessComm ipcMtbl = ipc;  //java2c: build Mtbl
    while(state !='x'){  //x to terminate
      //chgData_TestData_Inspc(ythis->testInspc);   //only for test.
      this.nrofBytesReceived[0] = 0; //expected the nrof available data
      //
      //----> receiveData(...)
      //
      state = 'r';  //receive
      try{
        ipcMtbl.receiveData(this.nrofBytesReceived, this.rxBuffer, this.myAnswerAddress);
        if(state !='x'){
          if(nrofBytesReceived[0] <0){ //error situation
            //it is possible that a send request has failed because the destination port is not
            //able to reach any more. Therefore wait a moment and listen new
            state = 'e';  //prevent send
            //wait a moment! If it is a permanent error, a loop can be occur. Prevent it. Let other threads work.
            try{ Thread.sleep(50); } catch(InterruptedException exc){}
            state = 'r';
            //
            //cmdExecuterMtbl.executeCmd(rxBuffer, nrofBytesReceived[0]);
          } else {
            execRxDataMtbl.execRxData(rxBuffer, nrofBytesReceived[0], this.myAnswerAddress);      
            //unnecessary because usage receiveData: ipcMtbl.freeData(rxBuffer);
          }
        }
      } catch(Exception exc){
        /** @java2c=toStringNonPersist, StringBuilderInStack=100. */
        CharSequence msg = Assert.exceptionInfo("org.vishia.inspector.Comm - unexpected Exception; ", exc, 0, 7);
        System.err.println(msg);
        exc.printStackTrace( System.err);
      }
    }//while state !='x'
  }

  
  /**Shutdown the communication, close the thread. This routine should be called 
   * either on shutdown of the whole system or on closing the inspector functionality.
   * The inspector functionality can be restarted calling {@link #start(Object)}.
   * 
   */
  public final void shutdown(){
    if(state == 'e') return; //nothing to do, error.
    
    state = 'x';
    /**@java2c=dynamic-call. */
    InterProcessComm ipcMtbl = ipc; 
    ipcMtbl.close();  //breaks waiting in receive socket
    //waits till the receive thread is finished.
    while(state !='z'){
      synchronized(this){
        try{ wait(100); } catch(InterruptedException exc){}
      }
    }
    try{ 
      Thread.sleep(1000);  //wait a second.
    } catch(InterruptedException exc){}
  }


  
  @SuppressWarnings("synthetic-access") 
  private final Runnable threadRoutine = new Runnable()
  {
    public void run() {
      runThread();
    }
  };
  
  
}
