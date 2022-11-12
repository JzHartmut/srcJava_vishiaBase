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
package org.vishia.msgDispatch;

import java.io.IOException;
import java.util.Arrays;

import org.vishia.bridgeC.ConcurrentLinkedQueue;
import org.vishia.bridgeC.MemC;
import org.vishia.bridgeC.OS_TimeStamp;
import org.vishia.bridgeC.VaArgBuffer;
import org.vishia.bridgeC.Va_list;
import org.vishia.util.Java4C;


/**This is the core of the message dispatcher. It dispatches only. 
 * The dispatch table maybe filled with a simplest algorithm. 
 * This class is able to use in a simple environment.
 * 
 * @author Hartmut Schorrig
 *
 */
public class MsgDispatcherCore extends LogMessageBase
{

  /**version, history and license.
   * <ul>
   * <li>2014-01-08 Hartmut new: {@link #setIdThreadForMsgDispatching(long)}
   * <li>2014-01-08 Hartmut chg: The {@link #tickAndFlushOrClose()} should be part of the core.  
   * <li>2013-03-02 Hartmut chg: The LogMessage is implemented here now, instead in the derived {@link MsgDispatcher}.
   * <li>2013-02-25 Hartmut new: The {@link Output#bUseText} stores whether the output channel uses the text. 
   *   The {@link #dispatchMsg(int, boolean, int, OS_TimeStamp, String, Va_list)} gets a text from configuration
   *   only if it is need.
   * <li>2013-02-25 Hartmut new: The {@link #sendMsgVaList(int, OS_TimeStamp, String, Va_list)} counts whether a message can't be queued.
   *   it is used to output a 'lostmessage' message in {@link MsgDispatcher#dispatchQueuedMsg()}.
   * <li>2012-08-22 Hartmut new {@link #setMsgTextConverter(MsgText_ifc)}. It provides a possibility to work with a
   *   translation from ident numbers to text with an extra module, it is optional.
   * <li>2012-06-15 Hartmut created as separation from the MsgDispatcher because it may necessary to deploy 
   *   in a small C environment.
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
  public final static int version = 20130302;

  /**If this bit is set in the bitmask for dispatching, the dispatching should be done 
   * in the dispatcher Thread. In the calling thread the message is stored in a queue. */
  public final static int mDispatchInDispatcherThread = 0x80000000;

  /**If this bit is set in the bitmask for dispatching, the dispatching should only be done 
   * in the calling thread. It is possible that also the bit {@link mDispatchInDispatcherThread}
   * is set, if there is more as one destination. */
  public final static int mDispatchInCallingThread =    0x40000000;

  /**Only this bits are used to indicate the destination via some Bits*/
  public final static int mDispatchBits =               0x3FFFFFFF;
  
  /**Number of Bits in {@link mDispatchWithBits}, it is the number of destinations dispatched via bit mask. */
  protected final int nrofMixedOutputs;
  
  /**Calculated mask of bits which are able to mix. */
  public final int mDstMixedOutputs;
   
  /**Calculated mask of bits which are one index. */
  public final int mDstOneOutput;
  
  
  /**Mask for dispatch the message to console directly in the calling thread. 
   * It is like system.out.println(...) respectively printf in C.
   * The console output is a fix part of the Message dispatcher.
   */
  public final static int mConsole = 0x01;
  
  /**queued Console output, it is a fix part of the Message dispatcher. */
  public final static int mConsoleQueued = 0x02;
  
  /**Used for argument mode from {@link #setOutputRange(int, int, int, int, int)} to add an output.
   * The other set outputs aren't change. 
   */
  public final static int mAdd = 0xcadd;
  
  /**Used for argument mode from {@link #setOutputRange(int, int, int, int, int)} to set an output.
   * Outputs before are removed. 
   */
  public final static int mSet = 0xc5ed;
  
  /**Used for argument mode from {@link #setOutputRange(int, int, int, int, int)} to remove an output.
   * All other outputs aren't change.
   */
  public final static int mRemove = 0xcde1;
  
  
  
  /**Stores all data of a message if the message is queued here. @java2c=noObject. */  
  public static final class Entry
  {

    /**Bit31 is set if the state is coming, 0 if it is going. */
    public int ident;
     
    /**The bits of destination dispatching are ascertained already before it is taken in the queue. */
    public int dst;
  
    /**The output and format controlling text. In C it should be a reference to a persistent,
     * typical constant String. For a simple system the text can be left empty, using a null-reference. 
     * @java2c=zeroTermString. 
     */
    public String text;
    
  
    /**The time stamp of the message. It is detected before the message is queued. */
    public final OS_TimeStamp timestamp = new OS_TimeStamp();

    /**Values from variable argument list. This is a special structure 
     * because the Implementation in C is much other than in Java. 
     * In Java it is simple: All arguments are of type Object, and the variable argument list
     * is a Object[]. The memory of all arguments are handled in garbage collection. It is really simple.
     * But in C there are some problems:
     * <ul><li>Type of arguments
     * <li>Location of arguments: Simple numeric values are in stack (only call by value is possible)
     *     but strings (const char*) may be non persistent. No safety memory management is present.
     * </ul>
     * Therefore in C language some things should be done additionally. See the special C implementation
     * in VaArgList.c. 
     */
    public final VaArgBuffer values = new VaArgBuffer(11);  //embedded in C
    
    //final Values values = new Values();  //NOTE: 16*4 = 64 byte per entry. NOTE: only a struct is taken with *values as whole.

    public static int _sizeof(){ return 1; } //it is a dummy in Java.
    
  }
  
  /**This class contains some test-counts for debugging. It is a own class because structuring of attributes. 
   * @xxxjava2c=noObject.  //NOTE: ctor without ObjectJc not implemented yet.
   */
  protected static final class TestCnt
  {
    int noOutput;
    int tomuchMsgPerThread;
  }
  
  /**This class contains all infomations for a output. There is an array of this type in MsgDispatcher. 
   * @java2c=noObject.
   */
  protected static final class Output
  {
    /**Short name of the destination, used for {@link #setOutputRange } or {@link #setOutputFromString }
     * @xxxjava2c=simpleArray.
     */
    String name;
    
    /**The output interface. */
    LogMessage outputIfc;

    /**true if this output is processed in the dispatcher thread, 
     * false if the output is called immediately in the calling thread.
     */ 
    boolean dstInDispatcherThread;
    
    /**Bit which indicates that the text information field of {@link #sendMsgVaList(int, OS_TimeStamp, String, Va_list)} is used.
     * This bit is set in the routine {@link MsgDispatcher#setOutputRoutine(int, String, boolean, boolean, LogMessage)}.
     * It is important especially in fast embedded systems.
     */
    boolean bUseText;
    
    public @Java4C.Exclude @Override String toString(){ return name + ":" + outputIfc; }
  }
  
  long idThreadForDispatching;
  
  final TestCnt testCnt = new TestCnt();
  
  /**List of messages to process in the dispatcher thread.
   * @java2c=noGC.
   */
  final ConcurrentLinkedQueue<Entry> listOrders;
  
  /**List of entries for messages to use. For C usage it is a List with a fix size.
   * The ConcurrentLinkedQueue may be implemented in a simple way if only a simplest system is used.
   * @java2c=noGC.
   */
  final ConcurrentLinkedQueue<Entry> freeOrders;
  
  protected final MsgDispatcherCore.Entry entryMsgBufferOverflow = new MsgDispatcherCore.Entry();
  

  
  /**List of idents, its current length. */ 
  protected int actNrofListIdents;

  /**List of idents, a array with lengthListIdents elements.
   * @java2c=noGC.
   */
  protected int[] listIdents;  //NOTE: It is a pointer to an array struct.

  /**List of destination bits for the idents.
   * @java2c=noGC.
   */
  protected int[] listBitDst;  //NOTE: It is a pointer to an array struct.

  /**up to 30 destinations for output.
   * @java2c=noGC,embeddedArrayElements.
   */
  protected Output[] outputs;

  
  /**Converter from the ident number to a text. Maybe null, then unused.
   * See {@link #setMsgTextConverter(MsgText_ifc)}
   */
  protected MsgText_ifc msgText;
  
  
  protected final Runnable runNoEntryMessage;
  
  int ctLostMessages;
  
  /**Initializes the instance.
   * @param maxQueue The static limited maximal size of the queue to store messages from user threads
   *        to dispatch in the dispatcher thread. If you call the dispatching in dispatcher thread
   *        cyclicly in 100-ms-steps, and you have worst case no more as 200 messages in this time,
   *        200 is okay.
   * @param nrofMixedOutputs
   */
  MsgDispatcherCore(int maxQueue, int nrofMixedOutputs, Runnable runNoEntryMessage){
    this.runNoEntryMessage = runNoEntryMessage;
    this.nrofMixedOutputs = nrofMixedOutputs;
    if(nrofMixedOutputs < 0 || nrofMixedOutputs > 28) throw new IllegalArgumentException("max. nrofMixedOutputs");
    this.mDstMixedOutputs = (1<<nrofMixedOutputs) -1;
    this.mDstOneOutput = mDispatchBits & ~mDstMixedOutputs;

    /**A queue in C without dynamically memory management should have a pool of nodes.
     * The nodes are allocated one time and assigned to freeOrders.
     * The other queues shares the nodes with freeOrders.
     * The ConcurrentLinkedQueue isn't from java.util.concurrent, it is a wrapper around that.
     */
    final MemC mNodes = MemC.alloc((maxQueue +2) * Entry._sizeof());
    this.freeOrders = new ConcurrentLinkedQueue<Entry>(mNodes);
    this.listOrders = new ConcurrentLinkedQueue<Entry>(this.freeOrders);
  }
  
  
  /**Sets the capability that messages which are create in the dispatcher thread are output immediately
   * though the output channel should be used in the dispatcher thread. The advantage of that capability
   * is given 
   * <ul>
   * <li>Especially on startup for messages of startup. Often the {@link #tickAndFlushOrClose()} is started
   *   after continue the startup routine but in the same main thread. 
   *   The startup messages should be seen without delay, especially on problems on startup.
   * <li>If some algorithm are done in a main thread, which dispatches the messages too. Then no ressources
   *   to store message entries are necessary, and the messages comes out immediately, helpfull on debugging.
   *   It may be typically that algorithm of calculation are executed in the same thread like dispatching.
   * </ul>     
   * @param idThread It should be that thread id of the Thread which runs {@link #tickAndFlushOrClose()}.
   */
  public void setIdThreadForMsgDispatching(long idThread){ this.idThreadForDispatching = idThread; }
  
  public final void setMsgTextConverter(MsgText_ifc converter){
    msgText = converter;
  }
  
  
  
  /**Searches and returns the bits where a message is dispatch to.
   * The return value describes what to do with the message.
   * @param ident The message identificator
   * @return 0 if the message should not be dispatched, else some bits or number, see {@link #mDstMixedOutputs} etc.
   */
  public final int searchDispatchBits(int ident)
  { int bitDst;
    if(ident < 0)
    { /**a negative ident means: going state. The absolute value is to dispatch! */ 
      ident = -ident;
    }
    int idx = Arrays.binarySearch(listIdents, 0, actNrofListIdents, ident);
    if(idx < 0) idx = -idx -2;  //example: nr between idx=2 and 3 returns -4, converted to 2
    if(idx < 0) idx = 0;        //if nr before idx = 0, use properties of msg nr=0
    bitDst = listBitDst[idx];     
    return bitDst;
  }

  
  
  /**Sends a message. See interface.  
   * @param identNumber
   * @param text The text representation of the message, format string, see java.lang.String.format(..). 
   *             @pjava2c=zeroTermString.
   * @param args see interface
   * @java2c=stacktrace:no-param.
   */
   @Override public final boolean  sendMsg(int identNumber, String text, Object... args)
   { /**store the variable arguments in a Va_list to handle for next call.
      * The Va_list is used also to store the arguments between threads in the MessageDispatcher.
      * @java2c=stackInstance.*/
      final Va_list vaArgs =  new Va_list(args);  
     return sendMsgVaList(identNumber, OS_TimeStamp.os_getDateTime(), text, vaArgs);
   }

   
   /**Sends a message. See interface.  
    * @param identNumber
    * @param text The text representation of the message, format string, see java.lang.String.format(..). 
    *             @pjava2c=zeroTermString.
    * @param args see interface
    * @java2c=stacktrace:no-param.
    */
    @Override public final boolean  sendMsgTime(int identNumber, final OS_TimeStamp creationTime, String text, Object... args)
    { /**store the variable arguments in a Va_list to handle for next call.
       * The Va_list is used also to store the arguments between threads in the MessageDispatcher.
       * @java2c=stackInstance.*/
      final Va_list vaArgs =  new Va_list(args);  
      return sendMsgVaList(identNumber, creationTime, text, vaArgs);
    }

    
  /**Sends a message. See interface.  
   * @param identNumber 
   * @param creationTime
   * @param text The identifier text @pjava2c=zeroTermString.
   * @param typeArgs Type chars, ZCBSIJFD for boolean, char, byte, short, int, long, float double. 
   *        @java2c=zeroTermString.
   * @param args see interface
   */
  @Override public final boolean sendMsgVaList(int identNumber, final OS_TimeStamp creationTime, String text, final Va_list args)
  {
    int dstBits = searchDispatchBits(identNumber);
    if(dstBits != 0)
    { final int dstBitsForDispatcherThread;
      boolean bDispatchAlways = idThreadForDispatching !=0 && Thread.currentThread().getId() == idThreadForDispatching;
      if((dstBits & mDispatchInCallingThread) != 0 || bDispatchAlways)
      { /**dispatch in this calling thread: */
        dstBitsForDispatcherThread = dispatchMsg(dstBits, false, bDispatchAlways, identNumber, creationTime, text, args);
      }
      else
      { /**No destinations are to use in calling thread. */
        dstBitsForDispatcherThread = dstBits;
      }
      //if((dstBits & mDispatchInDispatcherThread) != 0)
      if(dstBitsForDispatcherThread != 0)
      { /**store in queue, dispatch in a common thread of the message dispatcher:
         * To support realtime systems, all data are static. The list freeOrders contains entries,
         * get it from there and use it. No 'new' is necessary here. 
         */
        Entry entry = freeOrders.poll();  //get a new Entry from static data store
        if(entry == null)
        { /**queue overflow, no entries available. The message can't be displayed
           * This is the payment to won't use 'new'. A problem of overwork of data. 
           */
          if(runNoEntryMessage !=null){
            runNoEntryMessage.run();
          }
          if(++ctLostMessages ==0){ctLostMessages = 1; }  //never reaches 0 after incrementation.
        }
        else
        { /**write the informations to the entry, store it. */
          entry.dst = dstBitsForDispatcherThread;
          entry.ident = identNumber;
          entry.text = text;
          entry.timestamp.set(creationTime);
          entry.values.copyFrom(text, args);
          listOrders.offer(entry);
        }
      }
    }
    return true;
  }
  

  /**TODO yet message nr 0 is created, get the message from the start text...
   * since 2022-09 because enhancement of {@link LogMessage} with Appendable.
   */
  @Override public Appendable append(CharSequence csq) throws IOException {
    sendMsg(0, csq.toString());
    return this;
  }

  @Override public Appendable append(CharSequence csq, int start, int end) throws IOException {
    sendMsg(0, csq.subSequence(start, end).toString());
    return this;
  }

  /**This is more formally, only one char is nonsense for the MsgDispatcher.
   *
   */
  @Override public Appendable append(char c) throws IOException {
    String s = "" + c;
    sendMsg(0, s);
    return this;
  }


  
  @Override public final boolean isOnline()
  { return true;
    
  }


  
  /**This routine may be overridden by the inherited class (usual {@link MsgDispatcher} to support closing.
   * @see org.vishia.msgDispatch.LogMessage#close()
   */
  @Override public void close(){   }


  /**This routine may be overridden by the inherited class (usual {@link MsgDispatcher} to support flushing
   * all queued messages.
   * @see org.vishia.msgDispatch.LogMessage#close()
   */
  @Override public void flush() {  }
  
  /**Dispatches the queues messages, after them calls {@link LogMessage#flush()} for all queued outputs.
   * This method can be called in any user thread cyclically. 
   * As opposite the {@link DispatcherThread} can be instantiate and {@link DispatcherThread#start()}. 
   * That thread calls only this routine in its cycle. 
   */
  public final void tickAndFlushOrClose()
  { dispatchQueuedMsg();
    for(int ix = 0; ix < outputs.length; ix++){
      MsgDispatcherCore.Output output = outputs[ix];
      if(output.dstInDispatcherThread){
        output.outputIfc.flush();
      }
    }
  }
  
  
  /**Dispatches all messages, which are stored in the queue. 
   * This routine should be called in a user thread or maybe in the background loop respectively the main thread. 
   * This routine is called in {@link #tickAndFlushOrClose()} and in @ {@link #flush()} and link #close()}.
   * @return number of messages which are found to dispatch, for statistic use.
   */
  public final int dispatchQueuedMsg()
  { int nrofFoundMsg = 0;
    /**Limit the number of while-loops to prevent thread hanging. */
    int cntDispatchedMsg = 100;
    boolean bCont;
    MsgDispatcherCore.Entry firstNotSentMsg = null;
    do
    { MsgDispatcherCore.Entry entry = listOrders.poll();
      bCont = (entry != null && entry != firstNotSentMsg);
      if(bCont)
      { nrofFoundMsg +=1;
        dispatchMsg(entry.dst, true, false, entry.ident, entry.timestamp, entry.text, entry.values.get_va_list());
        entry.values.clean();
        entry.ident = 0;  
        freeOrders.offer(entry);
      }
      
    }while(bCont && (--cntDispatchedMsg) >=0);
    //The buffer is empty yet.
    if(ctLostMessages >0){                              //dispatch the message about overflow of queued message.
      entryMsgBufferOverflow.values.setArg(0, ctLostMessages);
      //Note: In this time after readout the queue till set ctLostMessage to 0 an newly overflow may be occurred.
      //That is possible if a higher priority task or interrupt fills the queue. But it is able to expect
      //that this overflow continues after set ctLostMessages = 0, so it is detected. A thread safe operation
      //does not necessary.
      ctLostMessages = 0;
      int dstBits = searchDispatchBits(entryMsgBufferOverflow.ident);
      entryMsgBufferOverflow.timestamp.set(OS_TimeStamp.os_getDateTime());
      ///
      dispatchMsg(dstBits, true, false, entryMsgBufferOverflow.ident, entryMsgBufferOverflow.timestamp
          , entryMsgBufferOverflow.text, entryMsgBufferOverflow.values.get_va_list());
    }
    if(cntDispatchedMsg == 0)
    { /**max nrof msg are processed:
       * The while-queue is left because a thread hanging isn't able to accept.
       * The rest of messages, if there are any, will stay in the queue.
       * This routine is repeated, if any other message is added to the queue
       * or after a well-long delay. The situation of thread hanging because any error
       * is the fatal version of software error. Prevent it. 
       */
      //printf("MsgDisp: WARNING to much messages in queue\n");
      /**Count this situation to enable to inspect it. */  
      testCnt.tomuchMsgPerThread +=1;
    }
    return nrofFoundMsg;
  }
  
  
  

  
  /**Dispatches a message. This routine is called either in the calling thread of the message
   * or in the dispatcher thread. 
   * @param dstBits Destination identificator. If the bit {@link mDispatchInDispatcherThread} is set,
   *        the dispatching should be done only for a destination if the destination is valid
   *        for the dispatcher thread. 
   *        Elsewhere if the bit is 0, the dispatching should be done only for a destination 
   *        if the destination is valid for the calling thread.
   * @param bDispatchInDispatcherThread true if this method is called in dispatcher thread,
   *        false if called in calling thread. This param is compared with {@link Output#dstInDispatcherThread},
   *        only if it is equal with them, the message is output.
   * @param bDispatchAlways true then the output is used both in calling and in dispatcher thread.
   *   This boolean should be set only if the calling thread is the same as the dispatcher thread
   *   and the message should dispatch in the messag creation thread therefore.       
   * @param identNumber identification of the message.
   * @param creationTime
   * @param text The identifier text @pjava2c=zeroTermString.
   * @param args @pjava2c=nonPersistent.
   * @return 0 if all destinations are processed, elsewhere dstBits with bits of non-processed dst.
   */
  protected final int dispatchMsg(int dstBits, boolean bDispatchInDispatcherThread, boolean bDispatchAlways
      , int identNumber, final OS_TimeStamp creationTime, String text, final Va_list args)
  { //final boolean bDispatchInDispatcherThread = (dstBits & mDispatchInDispatcherThread)!=0;
    //assert, that dstBits is positive, because >>=1 and 0-test fails elsewhere.
    //The highest Bit has an extra meaning, also extract above.
    dstBits &= mDispatchBits;  
    int bitTest = 0x1;
    int idst = 0;
    @Java4C.ZeroTermString String sTextMsg = text;  //maybe null if not used.
    boolean bMsgTextGotten = false;
    while(dstBits != 0 && bitTest < mDispatchBits) //abort if no bits are set anymore.
    { if(  (dstBits & bitTest)!=0 //test if this bit for output is set
           //test whether the bit should be used: 
        && ( bDispatchAlways
           ||( outputs[idst].dstInDispatcherThread &&  bDispatchInDispatcherThread)  //dispatch in the requested thread
           ||(!outputs[idst].dstInDispatcherThread && !bDispatchInDispatcherThread)
           )
        )
      { Output channel = outputs[idst];
        LogMessage out = channel.outputIfc;
        if(out != null)
        { if(!bMsgTextGotten && msgText !=null && channel.bUseText){
            bMsgTextGotten =true;
            sTextMsg =  msgText.getMsgText(identNumber);
            if(sTextMsg ==null || sTextMsg.isEmpty()){
              sTextMsg = text;   //replace the input text if a new one is found.
            }
          }
          boolean sent = out.sendMsgVaList(identNumber, creationTime, sTextMsg, args);
          if(sent)
          { dstBits &= ~bitTest; //if sent, reset the associated bit.
          }
        }
        else
        { dstBits &= ~bitTest; //reset the associated bit, send isn't possible
          testCnt.noOutput +=1;
        }
      }
      bitTest <<=1;
      idst += 1;
    }
    return dstBits;
  }


  
  
  


}
