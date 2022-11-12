package org.vishia.util;

/**Supports special handling of Outputs and assertions especially in debug phase. 
 * @deprecated use {@link ExcUtil}
 * @author Hartmut Schorrig
 *
 */
@Deprecated public class Assert extends CheckVs
{

  /**Version, history and license.
   * <ul>
   * <li>2020-03-22 Hartmut removed to {@link CheckVs} because name clash with junit..Assert
   * <li>2013-07-14 Hartmut chg: {@link #stackInfo(String, int, int)} produces a text which supports hyperlinking
   *   in Eclipse output output console window, like {@link Exception#printStackTrace()}.
   * <li>2013-07-14 Hartmut new: {@link #throwCompleteExceptionMessage(String, Exception)}
   * <li>2013-01-26 Hartmut new: {@link #consoleErr(String, Object...)}, {@link #consoleOut(String, Object...)}:
   *   Possibility to use the original System.out channel even System.setErr() etc. may be invoked.
   * <li> 2013-01-26 Hartmut chg: {@link #assertion(boolean)} etc. are protected now and commented. That are the methods
   *   which can be overridden in another class which is used by {@link #setAssertionInstance(Assert)}.  
   * <li>2012-11-19 Hartmut new: stop() as dummy routine here now.
   * <li>2012-09-02 Hartmut new {@link #exceptionInfo(String, Throwable, int, int)} and {@link #stackInfo(String, int)}
   *   to support a short info output for example for messages. Not the whole stacktrace!
   * <li>2012-08-30 Hartmut some enhancements, especially assert with send a message to System.err.
   * <li>2012-01-19 Hartmut created. The reason was: set an individual breakpoint on assertion statement.
   *   The second reason: flexibility for debugging. The java language 'assert' is too less in functionality. 
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
   */
  public static final int version = 20130126;


}
