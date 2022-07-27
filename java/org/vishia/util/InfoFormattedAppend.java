package org.vishia.util;

import java.io.IOException;

/**Interface for a unique kind to add informations to a given StringFormatter instance.
 * See also {@link InfoAppend}.
 * 
 * @author Hartmut Schorrig
 *
 */
public interface InfoFormattedAppend
{
  /**The version, history and license. 
   * <ul>
   * <li>2015-03-08 Hartmut created.
   * </ul>
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
   * If you are indent to use this sources without publishing its usage, you can get
   * a second license subscribing a special contract with the author. 
   * 
   * @author Hartmut Schorrig = hartmut.schorrig@vishia.de
   * 
   * 
   */
  public final static String version = "2015-03-08";
  
  

  
  
  
  void infoFormattedAppend(StringFormatter u);
  
  /**Helper for simple application in toString().
   * <pre>
   * (at)Override public String toString(){ return (new PrepareToString(this)).ret; }
   * </pre>
   * @author hartmut
   *
   */
  public static class PrepareToString
  {
    public final String ret;
    
    public PrepareToString(InfoFormattedAppend obj) {
      StringFormatter u = new StringFormatter();
      obj.infoFormattedAppend(u);
      ret = u.getBuffer().toString();
      try{ u.close(); } catch(IOException exc){}
    }
    
    
  }
  
}
