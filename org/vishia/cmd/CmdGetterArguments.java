package org.vishia.cmd;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.vishia.util.DataAccess;

/**This interface is used to get arguments for {@link CmdExecuter#addCmd(org.vishia.cmd.JZtxtcmdScript.Subroutine, List, Appendable, File)}.
 * to invoke a {@link JZtxtcmdExecuter#execSub(org.vishia.cmd.JZtxtcmdScript.Subroutine, List, boolean, Appendable, File)} with the Map of actual arguments.
 * It should be implemented in environments to build the argument. See {@link org.vishia.gral.widget.GralCommandSelector}
 */
public interface CmdGetterArguments
{
  /**Version, history and license. This String is visible in the about info.
   * <ul>
   * <li>2016-12-26 Hartmut created
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
  //@SuppressWarnings("hiding")
  public static final String version = "2016-12-27";

  /**
   * @param cmd The command contains formal arguments. The name of the arguments, not the order should be evaluated
   * in the implementation routine.
   * @return filled Map with the argument values with its names as key.
   */
  List<DataAccess.Variable<Object>> getArguments(JZtxtcmdScript.Subroutine jzsub);
  
  /**Gets the current directory as excution environment. */
  File getCurrDir();

}
