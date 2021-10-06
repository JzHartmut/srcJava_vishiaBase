package org.vishia.byteData;

/**This class supports the access to any type of a simple variable with given bit masks.
 * The the bit mask are not part of a variable itself because a variable is an entity to
 * access data in any target system. 
 * <br>
 * Reason: If some bits should be read from a variable, and then other bits should be read,
 * the variable should be gotten from the target device only one time. Therefore it is only one
 * variable. But the access to several bits - or indices are controlled with this class.
 * 
 * @author Hartmut Schorrig
 *
 */
public class VariableAccessWithBitmask implements VariableAccess_ifc
{
  
  
  /**Version, history and license
   * <ul>
   * <li>2013-12-20 Hartmut new from @{@link VariableAccessWithIdx}, it uses a simple {@link VariableAccess_ifc},
   *   not the {@link VariableAccessArray_ifc}. The source is more simple.
   *   Note: The {@link VariableAccessWithIdx} support both, index and bits.
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
   */
  public static final int version = 20120331;

  /**The entity of a variable. */
  protected final VariableAccess_ifc variable;
  
  /**The mask to access to some  bits. The mask is shifted to right with bit.
   * For example if bit 3..1 should be gotten, mask = 0x7 and bit=1. It is -1 (all bits set)
   * if the value should not be mask.
   */
  protected final int mask;
  
  /**The position of the last significant bit if the value of the variable is placed in some bits
   * or specially in only one bit, a boolean value. 0 per default.
   */
  protected final int bit;

  public VariableAccessWithBitmask(VariableAccess_ifc variable, int bit, int mask){
    if(variable ==null){
      throw new IllegalArgumentException("Variable is not given");
    }
    this.variable = variable;
    this.bit = bit;
    this.mask = mask;
  }
  
  public VariableAccessWithBitmask(VariableAccess_ifc variable){
    this(variable, 0, -1);
  }
  
  public double getDouble(){ return variable.getDouble(); }

  /**Returns the presentation float value for the integer bits in the variable.
   * The variable will be interpreted anyway as an integer variable, because its bits are accessed.
   * @see org.vishia.byteData.VariableAccess_ifc#getFloat()
   */
  public float getFloat(){ return getInt(); }

  public String getString(){ return variable.getString(); }
  
  
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
    int value = variable.getInt();
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
    long value = variable.getLong();
    long bValue = (value >> bit) & mask;
    return bValue;
  }
  
  public VariableAccess_ifc getVariable(){ return variable; }

  /**Sets a float to the variable. 
   * See {@link #setFloat(float, int...)}. The index may be stored here.
   * @param value
   */
  public float setFloat(float value){ return variable.setFloat(value); }

  
  
  public String setString(String src){ return variable.setString(src); }

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
