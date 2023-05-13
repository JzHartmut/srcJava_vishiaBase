package org.vishia.util;

import java.io.IOException;

/**This is a new replacement for the org.vishia.mainCmd.MainCmd which is now deprecated.
 * <br>
 * TODO in future, yet empty.
 * 
 * @author hartmut Schorrig
 *
 */
public class ApplMain {
  
  /**Version, history and license.
   * <ul>
   * <li>2022-11-16 created following the concept idea of the older {@link org.vishia.mainCmd.MainCmd}.
   *   The last one may be removed in future. <br>
   *   Why remove MainCmd:
   *   MainCmd combines several aspects which may be implemented in different ways in a application.
   *   <br>
   *   In opposite this new class contains only a few obviously properties. 
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
  public static final String version = "2022-11-16";

  
  /**A string for indentation up to 100 spaces. Should be normally enough. */
  public static final String sNewlineIndent = "\n                                                                                                    ";

  public final static void outNewlineIndent ( Appendable out, int nIndent) throws IOException {
    out.append(sNewlineIndent.substring(0, nIndent+1));
  }
  
  public final static void outIndent ( Appendable out, int nIndent) throws IOException {
    out.append(sNewlineIndent.substring(1, nIndent+1));
  }
  
  protected ApplMain(){}

}
