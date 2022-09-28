package org.vishia.byteData;

/**This class supports the access to any type of variable with given indices and bit masks.
 * The indices and the bit mask are not part of a variable itself because a variable is an entity to
 * access data in any target system. 
 * <br>
 * Reason: If some bits should be read from a variable, and then other bits should be read,
 * the variable should be gotten from the target device only one time. Therefore it is only one
 * variable. But the access to several bits - or indices are controlled with this class.
 * 
 * @author Hartmut Schorrig
 *
 */
public class VariableAccessWithIdx implements VariableAccess_ifc
{
  
  
  /**Version, history and license
   * <ul>
   * <li>2012-09-24 Hartmut new {@link #getLong(int...)}  
   * <li>2012-08-23 Hartmut new {@link #setFloat(float, int...)} Access a variable with an index
   *   if this describes the non-indexed variable. It is only implmented yet for the setFloat. TODO: for all others too.
   * <li>2012-03-31 Hartmut created. Before that, a {@link org.vishia.gral.base.GralWidget}
   *   has accessed a variable via {@link VariableAccess_ifc }and the necessary index {@link #idx} for an arry variable
   *   was contained in the GralWidget. Secondary the possibility of {@link #mask} of some bits was impossible. 
   *   The access to a array or bitfield variable is not a problem of the widget in a GUI, 
   *   it is a problem of access to the variable. Therefore this class was created. 
   *   <br>
   *   A implementation of {@link VariableAccess_ifc} should not contain this details of access 
   *   A variable accessed via {@link VariableAccess_ifc} is an entity of access to a whole data element.
   *   This class controls the access to details of a variable.
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
  public static final int version = 20120331;

  /**The entity of a variable. */
  protected final VariableAccessArray_ifc variable;
  
  /**Array of indices to access if the variable has more as one dimension.
   */
  protected final int[] ixArray;
  
  /**The mask to access to some  bits. The mask is shifted to right with bit.
   * For example if bit 3..1 should be gotten, mask = 0x7 and bit=1. It is -1 (all bits set)
   * if the value should not be mask.
   */
  protected final int mask;
  
  /**The position of the last significant bit if the value of the variable is placed in some bits
   * or specially in only one bit, a boolean value. 0 per default.
   */
  protected final int bit;

  public VariableAccessWithIdx(VariableAccessArray_ifc variable, int[] idx, int bit, int mask){
    if(variable ==null){
      throw new IllegalArgumentException("Variable is not given");
    }
    this.variable = variable;
    this.ixArray = idx;
    this.bit = bit;
    this.mask = mask;
  }
  
  public VariableAccessWithIdx(VariableAccessArray_ifc variable, int[] idx){
    this(variable, idx, 0, -1);
  }
  
  public VariableAccessWithIdx(VariableAccessArray_ifc variable){
    this(variable, null, 0, -1);
  }
  
  public VariableAccessWithIdx(VariableAccessArray_ifc variable, int bit, int mask){
    this(variable, null, bit, mask);
  }
  
  public double getDouble(){ return ixArray == null ? variable.getDouble() : variable.getDouble(ixArray); }

  public float getFloat(){ return ixArray == null ? variable.getFloat() : variable.getFloat(ixArray); }

  public String getString(){ return variable.getString(ixArray); }
  
  
  @Override public void setRefreshed(long time){
    if(variable != null){
      variable.setRefreshed(time); 
    }
    
  }
  
  public long getLastRefreshTime(){ 
    if(variable == null){
      return -1;
    } else {
      return variable.getLastRefreshTime(); 
    }
  }

  @Override public int[] getLastRefreshTimeShort () {
    return this.variable.getLastRefreshTimeShort();
  }

  public char getType(){ 
    if(variable == null){
      return '?';
    } else {
      return variable.getType();
    }
  }
 
  /**Returns the integer representation of the value of the variable.
   * Note: If the variable itself is a byte or 16-bit-type, only 1 or 2 bytes are gotten from the variable
   * respectively the index is regarded to the length of the variable.
   * Note: The bit and mask will be regarded.
   * @return The value
   */
  public int getInt(){ 
    int value = ixArray == null ? variable.getInt() : variable.getInt(ixArray);
    int bValue = (value >> bit) & mask;
    return bValue;
  }
  
  /**Returns the integer representation of the value of the variable.
   * Note: If the variable itself is a byte or 16-bit-type, only 1 or 2 bytes are gotten from the variable
   * respectively the index is regarded to the length of the variable.
   * Note: The bit and mask will be regarded.
   * @return The value
   */
  public long getLong(){ 
    long value = ixArray == null ? variable.getLong() : variable.getLong(ixArray);
    long bValue = (value >> bit) & mask;
    return bValue;
  }
  
  public VariableAccess_ifc getVariable(){ return variable; }

  /**Sets a float to the variable. 
   * See {@link #setFloat(float, int...)}. The index may be stored here.
   * @param value
   */
  public float setFloat(float value){ return ixArray == null ? variable.setFloat(value) : variable.setFloat(value, ixArray); }

  
  /**Sets a value to the variable which is an array variable.
   * @param value
   * @param ix
   */
  public void setFloat(float value, int ...ix){ variable.setFloat(value, ix); }

  
  public String setString(String src){ return ixArray == null ? variable.setString(src) : variable.setString(src, ixArray); }

  @Override
  public void requestValue(long timeRequested){ variable.requestValue(timeRequested); }

  @Override public void requestValue(){ variable.requestValue(System.currentTimeMillis()); }

  @Override
  public void requestValue(long timeRequested, Runnable run){ variable.requestValue(timeRequested, run); }

  @Override public boolean isRequestedValue(long timeEarlyRequested, boolean retryFaultyVariables){
    return variable.isRequestedValue(timeEarlyRequested, retryFaultyVariables);
  }
  
  @Override public boolean isRefreshed(){ return variable.isRefreshed(); }

  
  @Override
  public double setDouble(double value)
  {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public int setInt(int value)
  {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public long setLong(long value)
  {
    // TODO Auto-generated method stub
    return 0;
  }
  
}
