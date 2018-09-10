package org.vishia.msgDispatch;

import java.util.TimeZone;

import org.vishia.util.Assert;

/**This class replaces the System.out and System.err with 2 inputs which creates
 * a LogMessage and dispatch it with the Message Dispatcher.
 * Without any other effort both outputs will be dispatch to the originally
 * System.out and System.err output, but with additional time stamp and identification number.
 * <br><br>
 * The messages can be redirected and prevented using the {@link MsgDispatcher} capability.
 * <br><br>
 * Note that this class is a simple variant for redirection. It is limited
 * @author Hartmut Schorrig
 *
 */
public final class MsgDispatchSystemOutErr {

  
  /**Version, history and license.
   * <ul>
   * <li>2014-12-19 Hartmut chg: moved from srcJava_vishiaRun to this component srcJava_vishiaBase because it is a base concept
   *   which does not use any specials with communication etc. Creating a new archive. 
   * <li>2013-01-26 Hartmut fine tuning
   * <li>2012-01-07 Hartmut creation, to support a simple usage of {@link MsgPrintStream} to redirect System.out and System.err.
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
  public static final int version = 20130126;

  
  /**Indices to the output channels. */
  public static final int ixMsgOutputStdOut = 0, ixMsgOutputStdErr = 1, ixMsgOutputFile = 2;
  
  
  /**The converter from any output to a message.
   * Note that you can invoke {@link MsgPrintStream#setMsgGroupIdent(String, int, int)} to manipulate the building
   * of the ident numbers.
   */
  public final MsgPrintStream printOut, printErr;
  
  /**The message dispatcher */
  public final MsgDispatcher msgDispatcher;

  /**It is null when {@link #MsgDispatchSystemOutErr(String)} was called with null. */
  public final LogMessageFile fileOutput;
  
  private final TimeZone timeZoneForFile = TimeZone.getTimeZone("GMT");
  
  /**The output channels to console. */
  public final LogMessageStream cmdlineOut, cmdlineErr;
  
  public static MsgDispatchSystemOutErr singleton;
  
  /**Initializes the redirection of System.out and System.err
   * @param msgFiles A file path. If not null, then all messages will be written to the file additionally to the console.
   *   Note that the {@link MsgDispatcher} can be configured after this constructor to modify the outputs.
   */
  private MsgDispatchSystemOutErr(String msgFiles, int identStartOut, int identStartErr, int sizeNoGroup, int sizeGroup){
    Assert.check(true);  //capture the System.err and System.out for Assert.consoleOut(...).
    cmdlineOut = new LogMessageStream(System.out);
    cmdlineErr = new LogMessageStream(System.err);
    MsgDispatcher msgDispatcher = new MsgDispatcher(1000, 100, 3, 0, 1, null);
    this.msgDispatcher = msgDispatcher;
    
    printOut = new MsgPrintStream(msgDispatcher, identStartOut, sizeNoGroup, sizeGroup);
    printErr = new MsgPrintStream(msgDispatcher, identStartErr, sizeNoGroup, sizeGroup);
    int secondsToClose = -10; //any 10 seconds, the file will be closed, and re-openened at least after 10 seconds.
    int hoursPerFile = -3600; //This is 3600 seconds.
    int maskOut = 0;
    if(msgFiles !=null){
      fileOutput = new LogMessageFile(msgFiles, secondsToClose, hoursPerFile, null
        , timeZoneForFile, msgDispatcher.getSharedFreeEntries());
      msgDispatcher.setOutputRoutine(ixMsgOutputFile, "File", false, true, fileOutput);
      maskOut = 1 << ixMsgOutputFile;
    } else {
      fileOutput = null;
    }
    msgDispatcher.setOutputRoutine(ixMsgOutputStdOut, "stdout", false, true, cmdlineOut);
    msgDispatcher.setOutputRoutine(ixMsgOutputStdErr, "stderr", false, true, cmdlineErr);
    msgDispatcher.setOutputRange(0, 49999, maskOut | (1<<ixMsgOutputStdOut), MsgDispatcher.mSet, 0);
    msgDispatcher.setOutputRange(50000, Integer.MAX_VALUE, maskOut | (1<<ixMsgOutputStdErr), MsgDispatcher.mSet, 0);
    System.setOut(printOut.getPrintStreamLog(""));
    System.setErr(printErr.getPrintStreamLog(""));
  }
  
  
  /**Creates a static instance to redirect System.err and System.out
   * @param msgFiles A file path. If not null, then all messages will be written to the file additionally to the console.
   *   Note that the {@link MsgDispatcher} can be configured after this constructor to modify the outputs.
   * @return The instance, able to get with {@link #singleton} too.
   */
  public static MsgDispatchSystemOutErr create(String msgFiles, int identStartOut, int identStartErr, int sizeNoGroup, int sizeGroup){
    singleton = new MsgDispatchSystemOutErr(msgFiles, identStartOut, identStartErr, sizeNoGroup, sizeGroup);
    return singleton;
  }
}
