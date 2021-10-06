package org.vishia.msgDispatch;

import java.io.File;
import java.util.TimeZone;

import org.vishia.mainCmd.MainCmd;
import org.vishia.mainCmd.MainCmd_ifc;
import org.vishia.util.Assert;

/**This class replaces the System.out and System.err with 2 inputs which creates
 * a LogMessage and dispatch it with the Message Dispatcher.
 * Without any other effort both outputs will be dispatch to the originally
 * System.out and System.err output, but with additional time stamp and identification number.
 * <br><br>
 * The messages can be redirected and prevented using the {@link MsgDispatcher} capability.
 * <br><br>
 * @author Hartmut Schorrig
 *
 */
public class MsgRedirectConsole extends MsgDispatcher
{

  /**Version, history and license.
   * <ul>
   * <li>2015-11-15 Hartmut new {@link MsgRedirectConsole#MsgRedirectConsole()} for simplest usage. 
   * <li>2013-03-31 Hartmut new {@link #readConfig(File)}, experience with usage.
   * <li>2013-03-24 Hartmut rewrite, create this class from MsgDispatchSystemOutErr
   * <li>2013-01-26 Hartmut fine tuning
   * <li>2012-01-07 Hartmut creation of MsgDispatchSystemOutErr, to support a simple usage of {@link MsgPrintStream} to redirect System.out and System.err.
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
  
  private final TimeZone timeZoneForFile = TimeZone.getTimeZone("GMT");
  
  /**The output channels to console. */
  public final LogMessage cmdlineOut, cmdlineErr;
  
  private final MsgConfig msgConfig = new MsgConfig();
  
  /**Initializes the redirection of System.out and System.err in a full accessible way for Message Dispatching.
   * @param identStartOut  The message number of auto-generate numbers for first System.out
   * @param identStartErr  The message number of auto-generate numbers for first System.err
   *   if -1 then the System.err is not overridden.
   * @param sizeNoGroup    max. number of non-grouped messages
   * @param sizeGroup      max. number of messages in a group. See {@link MsgPrintStream#MsgPrintStream(LogMessage, int, int, int)}
   * @param mainCmd        An existing implementation especially {@link MainCmd} or its graphical derivation.
   *   This parameter can be null if the MainCmd concept is not used. 
   * @param maxDispatchEntries Further parameter see superclass {@link MsgDispatcher}
   * @param maxQueue
   * @param maxOutputs
   * @param msgIdentQueueOverflow
   * @param runNoEntryMessage
   */
  public MsgRedirectConsole(int identStartOut, int identStartErr, int sizeNoGroup, int sizeGroup,
      MainCmd mainCmd, 
      int maxDispatchEntries, int maxQueue,
      int maxOutputs, int msgIdentQueueOverflow,
      Runnable runNoEntryMessage)
  {
    super(maxDispatchEntries, maxQueue, maxOutputs, 0,
        msgIdentQueueOverflow, runNoEntryMessage);
    Assert.check(true);  //capture the System.err and System.out for Assert.consoleOut(...).
    printOut = new MsgPrintStream(this, identStartOut, sizeNoGroup, sizeGroup);
    printErr = identStartErr >=0 ? new MsgPrintStream(this, identStartErr, sizeNoGroup, sizeGroup): null;
    int maskOut = 0;
    if(mainCmd == null){
      cmdlineOut = new LogMessageStream(System.out);
      cmdlineErr = new LogMessageStream(System.err);
    } else { //use it from:
      cmdlineOut = mainCmd.getLogMessageOutputConsole();
      cmdlineErr = mainCmd.getLogMessageErrorConsole();
      mainCmd.setLogMessageDestination(this);
      setOutputRoutine(ixMsgOutputFile, "stdlog", true, true, mainCmd.getLogMessageOutputFile());
    }
    this.setOutputRoutine(ixMsgOutputStdOut, "stdout", false, true, cmdlineOut);
    this.setOutputRoutine(ixMsgOutputStdErr, "stderr", false, true, cmdlineErr);
    this.setOutputRange(0, 49999, maskOut | (1<<ixMsgOutputStdOut), MsgDispatcher.mSet, 0);
    this.setOutputRange(50000, Integer.MAX_VALUE, maskOut | (1<<ixMsgOutputStdErr), MsgDispatcher.mSet, 0);
    if(mainCmd == null){
      this.setOutputRange(0, Integer.MAX_VALUE, 1<<ixMsgOutputFile, MsgDispatcher.mAdd, 0);
    }
    System.setOut(printOut.getPrintStreamLog(""));
    if(printErr !=null){ System.setErr(printErr.getPrintStreamLog("")); }
  }

  
  
  /**Initializes the redirection of System.out and System.err in a simple standard way.
   * @param fileOut A output for the Message Dispatcher which should act as the log file output.
   *   It is assigned to the {@link #ixMsgOutputFile} for dispatching.
   * @param addOutputs 0 or >0 if more outputs are considered.
   * @param runNoEntryMessage Routine which is called on message overflow, can be null.
   */
  public MsgRedirectConsole(MainCmd mainCmd, int addOutputs, Runnable runNoEntryMessage)
  { this(10000, 40000, 10000, 100, mainCmd, 10000, 1000, addOutputs+3, 9999, runNoEntryMessage);
  }
  
  /**Initializes the redirection of System.out and System.err in the simplest standard way.
   * All System.out and System.err will be completed with a timestamp and output then.
   * You can use {@link #readConfig(File)} to set associations between the message start texts and numbers
   * and switch messages on or off depending on their start texts. 
   */
  public MsgRedirectConsole()
  { this(10000, 40000, 10000, 100, null, 10000, 1000, 3, 9999, null);
  }
  
  public void setMsgIdents(MsgConfig src, String chnChars){
    super.setMsgTextConverter(src);
    printOut.setMsgIdents(src);
    if(printErr !=null){
      printOut.setMsgIdents(src);
    }
    src.setMsgDispaching(this, chnChars);
  }
  
  public String readConfig(File fileConfig){ 
    String sError = msgConfig.readConfig(fileConfig); 
    if(sError == null){
      setMsgIdents(msgConfig, "dfl");
    } else {
      System.err.println("MsgRedirectConsole.readConfig - syntax error; " + sError);
    }
    return sError;
  }

  
}
