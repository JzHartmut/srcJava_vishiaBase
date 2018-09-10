package org.vishia.util;

import java.util.List;

/**This class contains some conversion routine. The routines are non-static though there does not use any 
 * instance data. They can be called with an instance of Conversion.
 * The instance of Conversion does not contain any data.
 * @author Hartmut Schorrig
 *
 */
public class Conversion
{
  /**Version, history and license.
   * <ul>
   * <li>2014-02-21 created. Used especially in JZcmd
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
   * 
   * 
   */
  //@SuppressWarnings("hiding")
  static final public String sVersion = "2014-01-12";
  
  
  
  /**Converts a given List content to an String[]. Especially used for invocation
   * of any Javaclass.main(String[]) routine.
   * @param list The list may or should contain Strings. All others are converted with toString()
   * @return String[] represents the list.
   */
  public String[] stringArray(List<Object> list){
    String[] ret = new String[list.size()];
    int ix =-1;
    for(Object item: list){
      ret[++ix] = item.toString();
    }
    return ret;
  }
  
  

}
