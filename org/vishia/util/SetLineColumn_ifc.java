package org.vishia.util;

/**This interface is used to mark functionality to set a line and column information.
 * It is used especially for {@link org.vishia.zbnf.ZbnfJavaOutput} to determine whether or not 
 * a line and / or column information is necessary. 
 * 
 * @author Hartmut Schorrig, LPGL license or second license
 *
 */
public interface SetLineColumn_ifc
{
  void setLineColumnFile(int line, int column, String sFile);

  
  public static final int mLine = 1, mColumn = 2, mFile = 4;
  
  /**Returns wheter only the line or only the column should be set.
   * It can save calculation time if one of the components are not necessary.
   * @return Bits mLine, mColumn, mFile.
   */
  int setLineColumnFileMode();


}
