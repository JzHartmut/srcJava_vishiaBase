package org.vishia.cmd;



/**Obsolete because the JZcmdScript is better.
 *   Use Map<String, JZcmdScript.Subroutine> or other to store the subroutines of a script.
 *   Use {@link JZtxtcmdScript#addContentToSelectContainer(org.vishia.cmd.JZtxtcmdScript.AddSub2List)} to add the content.
 *   The content of this class is now removed.
 * @author Hartmut Schorrig
 *
 */
public class CmdStore
{

  /**Version, history and license.
   * <ul>2017-01-01 Hartmut now obsolete because the JZcmdScript is better.
   *   Use Map<String, JZcmdScript.Subroutine> or other to store the subroutines of a script.
   *   Use {@link JZtxtcmdScript#addContentToSelectContainer(org.vishia.cmd.JZtxtcmdScript.AddSub2List)} to add the content.
   *   The content of this class is now removed.
   * <li>2016-12-26 Hartmut new: {@link #addSubOfJZcmdClass(org.vishia.cmd.JZtxtcmdScript.JZcmdClass)}: now adds classes 
   *   and subroutines in the order of the source, not in alphabetic order with separation classes and sub like before. 
   *   Therewith the script determines the order in a choice list {@link org.vishia.gral.widget.GralCommandSelector }
   * <li>2013-09-08 Hartmut chg: {@link #addSubOfJZcmdClass(org.vishia.cmd.JZtxtcmdScript.JZcmdClass, int)} now public
   *   because readCmdCfg(...) removed to {@link org.vishia.commander.FcmdExecuter}. It has dependencies
   *   to the Zbnf package {@link org.vishia.jztxtcmd.JZtxtcmd} which is not visible in this component by standalone compilation.
   *   The problem is: The {@link JZtxtcmdScript} is visible here, but the used translator for the JZcmdScript needs ZBNF 
   * <li>2013-09-08 Hartmut new: {@link CmdBlock#zgenSub} may replace the {@link CmdBlock#listBlockCmds}
   *   and may replace the {@link PrepareCmd} in future, first test. 
   * <li>2012-02-19 Hartmut chg: {@link #readCmdCfg(File)} accepts $ENV, comment lines with // and #
   *   and start of command not with spaces on line start.
   * <li>2011-12-31 Hartmut chg {@link CmdBlock#title} is new, the syntax of configfile is changed.
   *   This class is used to capture all executables for a specified extension for The.file.Commander 
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
  public static String version = "2017-01-01";
  
}
