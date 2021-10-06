package org.vishia.util;

import java.util.Map;

public interface ReplaceAlias_ifc
{
  /**Version, history and license.
   * <ul>
   * <li>2016-09-02 Hartmut new: {@link #searchAliasForValue(String)} as revers routine to {@link #replaceDataPathPrefix(String)} 
   * <li>2014-04-28 Hartmut created: The methods were used inside {@link org.vishia.gral.ifc.GralMngBuild_ifc}
   *   as one method of the interface. But the capability was necessary outside the Graphic, therefore
   *   this interface was created.
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
  static final public String sVersion = "2014-04-30";

  /**It supports usage of an alias in the data path. See {@link #replaceDataPathPrefix(String)}.
   * @param src this map will added to the existing one.
   */
  void addDataReplace(final Map<String, String> src);
  
  /**It supports usage of an alias in the data path. See {@link #replaceDataPathPrefix(String)}.
   * @param alias Any shorter alias
   * @param value The complete value.
   */
  void addDataReplace(String alias, String value);
  
  
  /**It supports usage of an alias in the data path.
   * @param path may contain "alias:restOfPath"
   * @return if "alias" is found in {@link #addDataReplace(String, String)} the it is replaced
   *   inclusively ":". If alias is not found, it is not replaced.
   *   Note that another meaning of "prefix:restOfPath" is possible.
   */
  String replaceDataPathPrefix(final String path);

  
  /**Try to replace the start of path with a given alias. 
   * 
   * @param path
   * @return path if no alias was found, elsewhere the path starting with the alias.
   */
  String searchAliasForValue(final String path);

}
