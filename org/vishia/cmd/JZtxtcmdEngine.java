package org.vishia.cmd;

import java.io.File;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.vishia.util.DataAccess;

/**This interface describes the JZcmd script engine as enhancment of the javax.script.ScriptEngine.
 * This interface is implemented in org.vishia.zcmd.JZcmd which is located in the srcJava_Zbnf component
 * because it needs the Zbnf parser. It is possible to implement that methods with another compiler and script language.

 * @author Hartmut Schorrig
 *
 */
public interface JZtxtcmdEngine extends ScriptEngine
{
  /**Version, history and license.
   * <ul>
   * <li>2014-06-10 created. The access from the {@link JZtxtcmdExecuter} to the ScriptEngine is necessary.
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
   * 
   */
  //@SuppressWarnings("hiding")
  static final public String sVersion = "2014-06-10";

  
  //Object eval(File script);
  
  /**Executes a sub routine in a given script, which should be translated firstly.
   * This routine can be called inside another script with invocation:
   * <pre>
   * { ## any KZcmd script
   *     Map args;
   *     String args.name  = value;
   *     java org.vishia.zcmd.JZcmd.execSub(File:"path/JZcmdscript.jzcmd", "class.subroutine-Name", args, jzcmdsub);
   * }
   * </pre>
   * @param fileScript The file which contains the script
   * @param subroutine name of the subroutine in the script.
   * @param args Arguments for this subroutine. They should be matching to the subroutine's formal arguments.
   * @param execLevel Execution level from where this routine is called.
   * @return
   */
  public Object evalSub(File fileScript, String subroutine
      , Map<String, DataAccess.Variable<Object>> args
      , JZtxtcmdExecuter.ExecuteLevel execLevel)
  throws ScriptException;

}
