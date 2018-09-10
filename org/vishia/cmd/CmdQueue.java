package org.vishia.cmd;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**This class stores some prepared commands for execution and executes it one after another.
 * The commands can contain placeholder for files. The commands may be operation system commands or {@link JZtxtcmdExecuter} invocation of sub routines.
 * @author hartmut Schorrig
 *
 */
public class CmdQueue 
{
  
  /**Version, history and license.
   * <ul>
   * <li>2017-01-01 Hartmut now obsolete, the {@link CmdExecuter} has a queue too. Because CmdStore is obsolete. Content is removed. 
   * <li>2016-12-31 Hartmut new: {@link #addCmd(org.vishia.cmd.JZcmdScript.Subroutine, List, File)} 
   * <li>2016-09-18 Hartmut new: {@link #addCmd(CmdBlock, CmdGetterArguments)} new concept supports only {@link JZtxtcmdExecuter}
   *   The {@link #addCmd(String, File, boolean)} and {@link #addCmd(String[], File, boolean)} is newly designed for simple operation system
   *   command execution without JZcmd concept. The {@link #addCmd(CmdBlock, File[], File)} which prepares file parts is designated as deprecated.
   *   Instead the JZcmdExecuter with more capability should be used. 
   * <li>2016-09-18 Hartmut chg: ctor with null as log, don't use log and JZcmd for simple applications. 
   * <li>2015-07-18 Hartmut chg: Now associated to an Appendable instead a PrintStream for error alive and error messages.
   *   A System.out is an Appendable too, for this kind the application is unchanged. Other output channels may support
   *   an Appendable rather than an PrintStream because it is more simple and substantial. 
   * <li>2013-09-08 Hartmut new: {@link #jzcmdExecuter} now included. TODO: it should use the {@link #executer}
   *   instead create a new one per call.
   * <li>2013-02-09 Hartmut chg: {@link #abortCmd()} now clears the queue too. The clearing of the command queue is a good idea, because while a program execution hangs, some unnecessary requests
   * may be initiated. 
   * <li>2013-02-03 Hartmut chg: better execution exception, uses log in {@link #execCmds(Appendable)}
   * <li>2013-02-03 Hartmut chg: {@link #execCmds(Appendable)} with only one parameter, the second one was unused.
   * <li>2011-12-03 Hartmut chg: The {@link #pendingCmds}-queue contains {@link PendingCmd} instead
   *   {@link CmdStore.CmdBlock} now. It is one level near on the really execution. A queued
   *   command may not stored in a CmdBlock. 
   * <li>2011-11-17 Hartmut new {@link #close()} to stop threads.
   * <li>2011-07-00 created
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
   */
  public static final String version = "2017-01-01";
  

}
