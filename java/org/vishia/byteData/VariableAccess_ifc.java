/****************************************************************************
 * Copyright/Copyleft:
 *
 * For this source the LGPL Lesser General Public License,
 * published by the Free Software Foundation is valid.
 * It means:
 * 1) You can use this source without any restriction for any desired purpose.
 * 2) You can redistribute copies of this source to everybody.
 * 3) Every user of this source, also the user of redistribute copies
 *    with or without payment, must accept this license for further using.
 * 4) But the LPGL is not appropriate for a whole software product,
 *    if this source is only a part of them. It means, the user
 *    must publish this part of source,
 *    but don't need to publish the whole source of the own product.
 * 5) You can study and modify (improve) this source
 *    for own using or for redistribution, but you have to license the
 *    modified sources likewise under this LGPL Lesser General Public License.
 *    You mustn't delete this Copyright/Copyleft inscription in this source file.
 *
 * @author Hartmut Schorrig: hartmut.schorrig@vishia.de, www.vishia.org
 * @version 0.93 2011-01-05  (year-month-day)
 *******************************************************************************/ 
package org.vishia.byteData;

/**This interface describes the access to any form of variable.
 * A variable may be a field gotten with reflection, see implementation <code>org.vishia.reflect.FieldJcVariableAccess (srcJava_vishiaRun)</code>.
 * A variable may be a entity in a byte-datagram, see {@link org.vishia.byteData.ByteDataSymbolicAccess.Variable}.
 * The quality of a variable and the kind of access is described in the implementation.
 * The user can access to a substantial variable with this interface.
 * <br><br>
 * It is possible to have one list or index with variables which are found in several data storage types,
 * for example in Java-data using <code>org.vishia.reflect.FieldJcVariableAccess (srcJava_vishiaRun)</code> or in data from files.
 * use Container <code>List &lt; VariableAccess_ifc ></code> or <code>Map &lt; String,VariableAccess_ifc ></code>.
 * To work as integral whole, this interface supports all of them.
 * <br><br> 
 * Variables may assembled in an array structure. The implementation knows about the array property of any variable.
 * To support an indexed access all methods can have additional index parameter. If they are not need, let it empty.
 * If indices are given but they are not expected, an {@link IndexOutOfBoundsException} or an {@link IllegalArgumentException}
 * may be thrown. 
 * 
 * @author Hartmut Schorrig
 *
 */
public interface VariableAccess_ifc
{
  /**Version, history and license.
   * <ul>
   * <li>2016-01-23 Hartmut chg: {@link #isRequestedValue(long, boolean)} with time parameter for earliest request.
   * <li>2014-05-22 Hartmut new: {@link #isRefreshed()}, implemented in all implementors.  
   * <li>2014-01-10 Hartmut new  enhancement of {@link #requestValue(long, Runnable) with Runnable.
   * <li>2013-12-10 Hartmut new Separate {@link VariableAccessArray_ifc} and this interface.
   *   All simple accesses and especially {@link VariableAccessWithIdx} need this interface.
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
  int getInt();
  
  /**Sets the value into the variable. If the variable is of byte or short type and the value is not able
   * to present, an IllegalArgumentException may be thrown or not, it depends on the implementation.
   * @param value The value given as int.
   * @param ixArray unused if it isn't an indexed variable.
   * @return The value really set (maybe more imprecise).
   */
  int setInt(int value);
  
  /**Gets a long value from this variable. The variable contains the information, 
   * whether it is long, short etc. 
   * If the variable is a float or double it may be convert to the long format.
   * If the value is not able to present, an IllegalArgumentException may be thrown or not, it depends on the implementation.
   * If the variable is a boolean, it is converted to 0 or 1.
   * @param ixArray unused if it isn't an indexed variable.
   * @return the value.
   */
  long getLong();
  
  /**Sets the value into the variable. If the variable is of byte, short or int type and the value is not able
   * to present, an IllegalArgumentException may be thrown or not, it depends on the implementation.
   * @param value The value given as int.
   * @param ixArray unused if it isn't an indexed variable.
   * @return The value really set (maybe more imprecise).
   */
  long setLong(long value);
  
  /**Gets the value from this variable. If the variable is in another format than float, 
   * a conversion to be will be done.
   * @param ixArray unused if it isn't an indexed variable.
   * @return the value.
   */
  float getFloat();
  
  /**Sets the value from this variable. 
   * @param ixArray unused if it isn't an indexed variable.
   * @return the value.
   */
  float setFloat(float value);
  
  
  /**Gets the value from this variable. If the variable is in another format than double, 
   * a conversion to be will be done.
   * @param ixArray unused if it isn't an indexed variable.
   * @return the value.
   */
  double getDouble();
  
  /**Sets the value from this variable. If the variable is from float type, and the range (exponent)
   * is able to present in float, the value will be stored in float with truncation of digits.
   * @param ixArray unused if it isn't an indexed variable.
   * @return the value.
   */
  double setDouble(double value);
  
  
  /**Gets the value from this variable. If the variable is numerical, it is converted to a proper representation.
   * @param ixArray unused if it isn't an indexed variable.
   * @return the value.
   */
  String getString();
  
  /**Sets the value into the variable
   * @param value The value given as String.
   * @param ixArray unused if it isn't an indexed variable.
   * @return The value really set (maybe shortened).
   */
  String setString(String value);
  
  /**Requests a new value from the target device.
   * @param timeRequested the current time, used to mark the request.
   */
  void requestValue(long timeRequested);
  
  /**Requests a new value from the target device with the {@link java.lang.System#currentTimeMillis()}
   */
  void requestValue();
  
  /**Requests a new value from the target device.
   * @param timeRequested the current time
   * @param run method which should be invoked if the request is resolved, optional operation.
   */
  void requestValue(long timeRequested, Runnable run);
  
  /**Returns true if the request is newer than the last refresh.
   * @param timeEarlyRequested The earliest time where the variable is requested. If the request time is more early, it is not regarded. 
   *   This is since 2016-01. An old request should be ignored but the time stamp of the request should be visible for debug or view the state.
   * @param retryFaultyVariables A variable which is set to invalid is requested though.
   * @return true if a new value should be gotten from target.
   */
  boolean isRequestedValue(long timeEarlyRequested, boolean retryFaultyVariables);
  //boolean requestValueFromTarget(long timeRequested, boolean retryDisabledVariable);  

  /**Returns true if the variable was refresed after it was requested.
   * false if the variable was never requested or it was requested and not refreshed after them.
   */
  boolean isRefreshed();
  
  public void setRefreshed(long time);
  /**Gets the time stamp when this variable was refreshed lastly.
   * @return -1 if this function is not supported, 0 if this variable was never set.
   */
  long getLastRefreshTime();
  
  /**Returns the type of the variable:
   * @return B, S, I, J, F, D, s for byte, short, integer, long, float, double and string.
   */
  char getType();
  

}
