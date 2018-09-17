package org.vishia.cmd;

import org.vishia.util.DataAccess;

/**superior class of {@link JZtxtcmdThread} and {@link JZtxtcmdThreadQueue} 
 * The elements are not used in that derived classes, but in the {@link JZtxtcmdExecuter.ExecuteLevel} 
 * while working in the threads.
 * @author Hartmut Schorrig
 *
 */
class JZtxtcmdThreadData
{
  
  /**The version, history and license.
   * <ul>
   * <li>2018-09-17 created from {@link JZtxtcmdThread}, was part of content there.
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
  public final static String version = "2018-09-17";

  
  /**Exception text. If not null then an exception is thrown and maybe thrown for the next level.
   * This text can be gotten by the "error" variable.
   */
  DataAccess.Variable<Object> error = new DataAccess.Variable<Object>('S', "error", null);
  
  /**The exception with them the thread was finished or null. */
  Throwable exception;
  
  JZtxtcmdScript.JZcmditem excStatement;
  
  int excLine, excColumn;
  
  String excSrcfile;
  

}
