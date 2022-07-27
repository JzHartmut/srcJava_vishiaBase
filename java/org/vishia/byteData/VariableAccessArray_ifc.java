package org.vishia.byteData;

public interface VariableAccessArray_ifc extends VariableAccess_ifc
{
  /**Version, history and license.
   * <ul>
   * <li>2013-12-10 Hartmut new rename this and separate {@link VariableAccess_ifc}.
   *   All simple accesses and especially {@link VariableAccessWithIdx} need {@link VariableAccess_ifc}.
   *   All implementations need the {@link VariableAccessArray_ifc} if they support arrays.
   * <li>2012-09-24 Hartmut new {@link #getLong(int...)} and {@link #setLong(long, int...)} 
   * <li>2012-04-25 Hartmut new {@link #requestValue(long)}, {@link #getLastRefreshTime()}:
   *   A variable should be refreshed by determined call of {@link #requestValue(long)} if it holds
   *   a value from a remote device.
   * <li>2012-03-31 Hartmut enhanced {@link #getType()}
   * <li>2010-06-00 Hartmut created to access values in an UDP-telegram with given positions.
   *   The interface permits an access independent of the concrete implementation. 
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
  public static final int version = 20120425;

  /**Gets a integer value from this variable. The variable contains the information, 
   * whether it is long, short etc. If the variable contains a long value greater as the integer range,
   * an IllegalArgumentException may be thrown or not, it depends on the implementation.
   * If the variable is a float or double it may be convert to the integer format.
   * If the variable is a boolean, it is converted to 0 or 1.
   * @param ixArray unused if it isn't an indexed variable.
   * @return the value.
   */
  int getInt(int ...ixArray);
  
  /**Sets the value into the variable. If the variable is of byte or short type and the value is not able
   * to present, an IllegalArgumentException may be thrown or not, it depends on the implementation.
   * @param value The value given as int.
   * @param ixArray unused if it isn't an indexed variable.
   * @return The value really set (maybe more imprecise).
   */
  int setInt(int value, int ...ixArray);
  
  /**Gets a long value from this variable. The variable contains the information, 
   * whether it is long, short etc. 
   * If the variable is a float or double it may be convert to the long format.
   * If the value is not able to present, an IllegalArgumentException may be thrown or not, it depends on the implementation.
   * If the variable is a boolean, it is converted to 0 or 1.
   * @param ixArray unused if it isn't an indexed variable.
   * @return the value.
   */
  long getLong(int ...ixArray);
  
  /**Sets the value into the variable. If the variable is of byte, short or int type and the value is not able
   * to present, an IllegalArgumentException may be thrown or not, it depends on the implementation.
   * @param value The value given as int.
   * @param ixArray unused if it isn't an indexed variable.
   * @return The value really set (maybe more imprecise).
   */
  long setLong(long value, int ...ixArray);
  
  /**Gets the value from this variable. If the variable is in another format than float, 
   * a conversion to be will be done.
   * @param ixArray unused if it isn't an indexed variable.
   * @return the value.
   */
  float getFloat(int ...ixArray);
  
  /**Sets the value from this variable. 
   * @param ixArray unused if it isn't an indexed variable.
   * @return the value.
   */
  float setFloat(float value, int ...ixArray);
  
  
  /**Gets the value from this variable. If the variable is in another format than double, 
   * a conversion to be will be done.
   * @param ixArray unused if it isn't an indexed variable.
   * @return the value.
   */
  double getDouble(int ...ixArray);
  
  /**Sets the value from this variable. If the variable is from float type, and the range (exponent)
   * is able to present in float, the value will be stored in float with truncation of digits.
   * @param ixArray unused if it isn't an indexed variable.
   * @return the value.
   */
  double setDouble(double value, int ...ixArray);
  
  
  /**Gets the value from this variable. If the variable is numerical, it is converted to a proper representation.
   * @param ixArray unused if it isn't an indexed variable.
   * @return the value.
   */
  String getString(int ...ixArray);
  
  /**Sets the value into the variable
   * @param value The value given as String.
   * @param ixArray unused if it isn't an indexed variable.
   * @return The value really set (maybe shortened).
   */
  String setString(String value, int ...ixArray);
  
  
  /**Returns the number of dimension if it is an array or its number of elements.
   * @param dimension 0 to get number of dimension, 1... to get the length of the dimension.
   * @return 0 if it hasn't such an dimension.
   */
  int getDimension(int dimension);
  
 
}
