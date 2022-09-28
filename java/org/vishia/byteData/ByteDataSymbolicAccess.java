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

import java.util.Map;
import java.util.TreeMap;

import org.vishia.bridgeC.ConcurrentLinkedQueue;
import org.vishia.bridgeC.IllegalArgumentExceptionJc;
import org.vishia.byteData.ByteDataAccessSimple;
import org.vishia.mainCmd.Report;


/**This class supports the access to binary data with some variable which describes parts of data.
 * The {@link ByteDataSymbolicAccess.Variable} contains a name, a data path (full name),
 * the type and the position of the bytes for the variable in the whole raw byte area.
 * 
 * The class {@link org.vishia.byteData.ByteDataSymbolicAccessReadConfig} contained in the
 * javaSrc_Zbnf component extends this class with a routine, which reads the information
 * for all variable from a script file. Therewith the ZBNF-Parser {@link org.vishia.zbnf}
 * is used. 
 * 
 * The variable may be given with Java-written instantiation too.
 * 
 * The data are given calling {@link #assignData(byte[])}, {@link #assignData(byte[], int, int)}
 * from any source. New data can be updated outside, for example by a communication via network receive,
 * and update in the data buffer by calling {@link #copyNewData(byte[], int, int, long)}.
 * Thereby a timestamp of updating is provided. It supports the method {@link VariableAccess_ifc#getLastRefreshTime()}. 
 * 
 * @author Hartmut Schorrig
 *
 */
public class ByteDataSymbolicAccess implements VariableContainer_ifc {

  /**Version, history and license. The version number is a date written as yyyymmdd as decimal number.
   * <ul>
   * <li>2013-11-26 Hartmut new: {@link #copyNewData(byte[], int, int, long)}
   * <li>2013-11-26 Hartmut chg {@link #assignData(byte[], long)} with timestamp required. Usages should updated.
   *   A timestamp is a substantial propertiy of dynamic data. Use {@link System#currentTimeMillis()}.
   * <li>2012-09-24 Hartmut new {@link Variable#getLong(int...)} and {@link Variable#setLong(long, int...)} 
   * <li>2012-04-22 Hartmut minor change: {@link Variable#nrofBytes} is necessary for ZBNF parse result.
   *   But it isn't used yet.
   * <li>2012-04-01 Hartmut new: Because enhanced {@link VariableAccess_ifc} new {@link Variable#getType()}
   *   and {@link Variable#getDimension(int)}.
   * <li>2012-03-02 Hartmut chg: Dissolving the readConfig because it needs the ZBNF parser
   *   which isn't part of this component up to now. It may be seen as additional functionality.
   *   Base functionality is gotten without the more complex parser.
   * <li>2010-12-03 Hartmut fnChg: The syntax is defined internally, see {@link #syntaxSymbolicDescrFile}.
   * </ul>
   * <ul>
   * <li>new: new functionality, downward compatibility.
   * <li>fnChg: Change of functionality, no changing of formal syntax, it may be influencing the functions of user,
   *            but mostly in a positive kind. 
   * <li>chg: Change of functionality, it should be checked syntactically, re-compilation necessary.
   * <li>adap: No changing of own functionality, but adapted to a used changed module.
   * <li>corr: correction of a bug, it should be a good thing.
   * <li>bug123: correction of a tracked bug.
   * <li>nice: Only a nice correction, without changing of functionality, without changing of syntax.
   * <li>descr: Change of description of elements.
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
   */
  public final static int versionStamp = 20220826;
  
  /**An instance can be created and filled from ZBNF-parser using reflection
   * or also programmatically created using .
   * It is a non static class because it is related to the instance of the given {@link ByteDataSymbolicAccess}.
   */
  public class Variable implements VariableAccessArray_ifc
  {
    public final ByteDataSymbolicAccess byteDataAccess;
    
    /**The data path. */
    public String name;
    
    /**A simple read-able identifier */
    public String nameShow;
    
    private char typeChar;
    
    public char getTypeChar(){ return typeChar; }
    
    public void set_typeChar(String src){ typeChar = src.charAt(0); }
    
    public int bytePos;
    
    public int bitMask;
    
    /**From ZBNF parsing.
     * 
     */
    public int nrofBytes;
    
    public int nrofArrayElements;

    /**Creates an instance which's fields can be set afterwards, especially while parsing a textual given file. */
    public Variable()
    { this.byteDataAccess = ByteDataSymbolicAccess.this;
    }

    
    /**Creates an instance which's fields can be set afterwards, especially while parsing a textual given file. */
    public Variable(String name, String nameShow, int bytePos, char typeChar, int nrofArrayElements)
    { this.byteDataAccess = ByteDataSymbolicAccess.this;
      setProps(name, nameShow, bytePos, typeChar, nrofArrayElements);
    }

    
    @Deprecated public Variable(ByteDataSymbolicAccess XXXbytes)
    {
      super();
      this.byteDataAccess = ByteDataSymbolicAccess.this;
    }

    
    
    /**Defines which type of variable are found on which position.
     * The elements of a variable are not final because there are also set as ZBNF parse result. 
     * But usual a variable is used as final (not reused for several approaches on different positions).
     * @param name Name for internal administration, unique
     * @param nameShow name to show
     * @param bytePos position in the datagram / byte array.
     * @param typeChar see {@link #typeChar}, Z B S I L F D for boolean, byte, short, int, long, float, double
     * Note: for boolean values not on bit 0 a bitMask should be set. 
     * @param nrofArrayElements 0 for scalar, >0 an array more values one after another
     */
    public void setProps(String name, String nameShow, int bytePos, char typeChar, int nrofArrayElements) {
      this.name = name;
      this.nameShow = nameShow;
      this.bytePos = bytePos;
      this.typeChar = typeChar;
      this.nrofArrayElements = nrofArrayElements;
    }
    
    @Override public char getType(){ return typeChar; }
    
    @Override public int getDimension(int dimension){
      if(dimension == 0){ return nrofArrayElements <=1 ? 0 : 1; }
      else if(dimension == 1){ return nrofArrayElements <=1 ? 0 : nrofArrayElements; }
      else return 0;
    }
    

    
    @Override public int getInt(int ...ixArray)
    { int value = 0;
      int nrofBytes1 = getNrofBytes();
      int bytePos = ixArray.length ==0 || ixArray[0] < 0 ? this.bytePos : this.bytePos + ixArray[0] * nrofBytes1;  //sizeof(int) is 4.
      if(bytePos > data.length-nrofBytes1) throw new IllegalArgumentException("file to short: " + data.length + ", requested: " + bytePos + nrofBytes1);
      switch(this.typeChar){
      case 'D': value = (int)dataAccess.getDoubleVal(bytePos); break;
      case 'F': value = (int)dataAccess.getFloatVal(bytePos); break;
      case 'I': case 'J': case 'S': case 'B': value = dataAccess.getIntVal(bytePos, -nrofBytes1); break;
      case 'Z': value = (dataAccess.getIntVal(bytePos, 1) & this.bitMask) == 0 ? 0: 1; break;
      default: new IllegalArgumentExceptionJc("fault type, expected: int, found: ", typeChar);
      }//switch
      return value;
    }
    
    
    @Override public int setInt(int value, int ...ixArray)
    { int nrofBytes1 = getNrofBytes();
      int bytePos = ixArray.length ==0 || ixArray[0] < 0 ? this.bytePos : this.bytePos + ixArray[0] * nrofBytes1;  //sizeof(int) is 4.
      if(bytePos > data.length-nrofBytes1) throw new IllegalArgumentException("file to short: " + data.length + ", requested: " + bytePos + nrofBytes1);
      switch(typeChar){
      case 'D': dataAccess.setDoubleVal(bytePos, value); break;
      case 'F': dataAccess.setFloatVal(bytePos, value); break;
      case 'I': case 'J': case 'S': case 'B': dataAccess.setIntVal(bytePos, nrofBytes1, value); break;
      case 'Z': {
        byte byteVal = (byte)dataAccess.getIntVal(bytePos, 1);
        if(value == 0){ byteVal &= ~bitMask; }
        else { byteVal |= bitMask; }  //TODO use ixArray
        dataAccess.setIntVal(bytePos, 1, byteVal);  //rewrite
      }break;
      //TODO case 'Z': (dataAccess.setIntVal(bytePos, 1) & variable.bitMask) == 0 ? 0: 1; break;
      default: new IllegalArgumentException("fault type, expected: float, found: " + typeChar);
      }//switch
      return value;
    }

    @Override public long getLong(int ...ixArray)
    { long value = 0;
      int nrofBytes1 = getNrofBytes();
      int bytePos = ixArray.length ==0 || ixArray[0] < 0 ? this.bytePos : this.bytePos + ixArray[0] * nrofBytes1;  //sizeof(int) is 4.
      if(bytePos > data.length-nrofBytes1) throw new IllegalArgumentException("file to short: " + data.length + ", requested: " + bytePos + nrofBytes1);
      switch(this.typeChar){
      case 'D': value = (int)dataAccess.getDoubleVal(bytePos); break;
      case 'F': value = (int)dataAccess.getFloatVal(bytePos); break;
      case 'L': case 'I': case 'J': case 'S': case 'B': value = dataAccess.getIntVal(bytePos, -nrofBytes1); break;
      case 'Z': value = (dataAccess.getIntVal(bytePos, 1) & this.bitMask) == 0 ? 0: 1; break;
      default: new IllegalArgumentExceptionJc("fault type, expected: int, found: ", typeChar);
      }//switch
      return value;
    }
    
    
    @Override public long setLong(long value, int ...ixArray)
    { int nrofBytes1 = getNrofBytes();
      int bytePos = ixArray.length ==0 || ixArray[0] < 0 ? this.bytePos : this.bytePos + ixArray[0] * nrofBytes1;  //sizeof(int) is 4.
      if(bytePos > data.length-nrofBytes1) throw new IllegalArgumentException("file to short: " + data.length + ", requested: " + bytePos + nrofBytes1);
      switch(typeChar){
      case 'D': dataAccess.setDoubleVal(bytePos, value); break;
      case 'F': dataAccess.setFloatVal(bytePos, value); break;
      case 'L': case 'I': case 'J': case 'S': case 'B': dataAccess.setIntVal(bytePos, nrofBytes1, value); break;
      case 'Z': {
        byte byteVal = (byte)dataAccess.getIntVal(bytePos, 1);
        if(value == 0){ byteVal &= ~bitMask; }
        else { byteVal |= bitMask; }  //TODO use ixArray
        dataAccess.setIntVal(bytePos, 1, byteVal);  //rewrite
      }break;
      //TODO case 'Z': (dataAccess.setIntVal(bytePos, 1) & variable.bitMask) == 0 ? 0: 1; break;
      default: new IllegalArgumentException("fault type, expected: float, found: " + typeChar);
      }//switch
      return value;
    }

    @Override
    public String getString(int ...ixArray)
    {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public String setString(String value, int ...ixArray)
    {
      // TODO Auto-generated method stub
      return null;
    }

    //@Override
    public float getFloat(int... ixArray)
    { float value = 0.0f;
      int nrofBytes1 = getNrofBytes();
      int bytePos = ixArray.length ==0 || ixArray[0] < 0 ? this.bytePos : this.bytePos + ixArray[0] * nrofBytes1;  //sizeof(int) is 4.
      if(bytePos > data.length-nrofBytes1) throw new IllegalArgumentException("file to short: " + data.length + ", requested: " + bytePos + nrofBytes1);
      switch(typeChar){
      case 'D': value = (float)dataAccess.getDoubleVal(bytePos); break;
      case 'F': value = dataAccess.getFloatVal(bytePos); break;
      case 'I': case 'J': case 'S': case 'B': value = dataAccess.getIntVal(bytePos, -nrofBytes1); break;
      default: new IllegalArgumentException("fault type, expected: float, found: " + typeChar);
      }//switch
      return value;
    }

    //@Override 
    public float setFloat(float value, int ...ixArray)
    { int nrofBytes1 = getNrofBytes();
      int bytePos = ixArray.length ==0 || ixArray[0] < 0 ? this.bytePos : this.bytePos + ixArray[0] * nrofBytes1;  //sizeof(int) is 4.
      if(bytePos > data.length-nrofBytes1) throw new IllegalArgumentException("file to short: " + data.length + ", requested: " + bytePos + nrofBytes1);
      switch(typeChar){
      case 'D': dataAccess.setDoubleVal(bytePos, value); break;
      case 'F': dataAccess.setFloatVal(bytePos, value); break;
      case 'I': case 'J': case 'S': case 'B': 
        //TODO check whether the value matches
        dataAccess.setIntVal(bytePos, nrofBytes1, (int)value); break;
      //TODO case 'Z': (dataAccess.setIntVal(bytePos, 1) & variable.bitMask) == 0 ? 0: 1; break;
      default: new IllegalArgumentException("fault type, expected: float, found: " + typeChar);
      }//switch
      return value;
    }

    @Override
    public double getDouble(int... ixArray)
    { double value = 0.0;
      int nrofBytes1 = getNrofBytes();
      int bytePos = ixArray.length ==0 || ixArray[0] < 0 ? this.bytePos : this.bytePos + ixArray[0] * nrofBytes1;  //sizeof(int) is 4.
      if(bytePos > data.length-nrofBytes1) throw new IllegalArgumentException("file to short: " + data.length + ", requested: " + bytePos + nrofBytes1);
      switch(typeChar){
      case 'D': value = dataAccess.getDoubleVal(bytePos); break;
      case 'F': value = dataAccess.getFloatVal(bytePos); break;
      case 'I': case 'J': case 'S': case 'B': value = dataAccess.getIntVal(bytePos, -nrofBytes1); break;
      default: new IllegalArgumentException("fault type, expected: float, found: " + typeChar);
      }//switch
      return value;
    }

    @Override
    public double setDouble(double value, int... ixArray)
    {
      // TODO Auto-generated method stub
      return value;
    }
    
    private int getNrofBytes()
    { int nrofBytes1 = 0;  //for unexpected types: nrofBytes may be unused.
      switch(typeChar){
      case 'D': nrofBytes1 = 8; break;
      case 'F': nrofBytes1 = 4; break;
      case 'J': nrofBytes1 = 8; break;
      case 'I': nrofBytes1 = 4; break;
      case 'S': nrofBytes1 = 2; break;
      case 'B': nrofBytes1 = 1; break;
      }
      return nrofBytes1;
      
    }

    @Override public void requestValue(long timeRequested)
    {
      ByteDataSymbolicAccess.this.timeRequestNewValue = timeRequested;
    }

    @Override public void requestValue(){ requestValue(System.currentTimeMillis()); }
    
    @Override public void requestValue(long timeRequested, Runnable run)
    {
      ByteDataSymbolicAccess.this.timeRequestNewValue = timeRequested;
      int catastrophicCount = 10;
      while(ByteDataSymbolicAccess.this.runOnRecv.remove(run)){  //prevent multiple add 
        if(--catastrophicCount <0){ throw new IllegalArgumentExceptionJc("ByteDataSymbolicAccess - requestValue catastrophicalCount", run.hashCode()); }
      }
      boolean offerOk = ByteDataSymbolicAccess.this.runOnRecv.offer(run);
      if(!offerOk){ throw new IllegalArgumentExceptionJc("ByteDataSymbolicAccess - requestValue run cannot be added", run.hashCode()); }
    }
    
    @Override public boolean isRequestedValue(long timeEarlyRequested, boolean retryFaultyVariables){
      if(ByteDataSymbolicAccess.this.timeRequestNewValue == 0) return false;  //never requested
      long timeNew = ByteDataSymbolicAccess.this.timeRequestNewValue - ByteDataSymbolicAccess.this.timeSetNewValue;
      long timeReq = ByteDataSymbolicAccess.this.timeRequestNewValue - timeEarlyRequested;
      return (ByteDataSymbolicAccess.this.timeSetNewValue ==0 || timeNew >=0) && timeReq >=0;
    }
    
    @Override public boolean isRefreshed(){ return (timeSetNewValue - timeRequestNewValue ) >0; }

    @Override public long getLastRefreshTime(){ return ByteDataSymbolicAccess.this.timeSetNewValue; }

    @Override public void setRefreshed(long time){ ByteDataSymbolicAccess.this.timeSetNewValue = time; }
    
    @Override public double getDouble() { return getDouble(0); }

    @Override public float getFloat() { return getFloat(0); }

    @Override public int getInt() { return getInt(0); }

    @Override public long getLong() { return getLong(0); }

    @Override public String getString() { return getString(0); }


    @Override public double setDouble(double value) { return setDouble(value, 0); }

    @Override public float setFloat(float value) { return setFloat(value, 0); }

    @Override public int setInt(int value) { return setInt(value, 0); }

    @Override public long setLong(long value) { return setLong(value, 0); }

    @Override public String setString(String value) { return setString(value, 0); }

    @Override public String toString() {
      return this.name + "/" + this.nameShow + ':' + this.typeChar + this.nrofArrayElements + " @" + this.bytePos;
    }
    
    
  }
  
  
  private final Map<String, ByteDataSymbolicAccess.Variable> indexVariable = new TreeMap<String, ByteDataSymbolicAccess.Variable>();
  
  private byte[] data;
  
  private int ixStartData;
  
  private int nrofData;
  
  private long timeRequestNewValue;
  
  private final ConcurrentLinkedQueue<Runnable> runOnRecv = new ConcurrentLinkedQueue<Runnable>();
  
  /**The last refresh time.
   * 
   */
  private long timeSetNewValue;
  
  /**The basically access to data. Hint: It is public to use for experience.
   * Normally it should not be used from external.
   */
  public final ByteDataAccessSimple dataAccess;
  
  /**Creates with its own internal (not visible) {@link ByteDataAccessSimple} access operations. */
  public ByteDataSymbolicAccess()
  { this.dataAccess = new ByteDataAccessSimple(true);  //access big endian
  }
  
  
  /**Works with a given ByteDataAccess which can be additional touched with the {@link ByteDataAccessBase} or {@link ByteDataAccessSimple} operations.
   * with that concept only a part of the whole data may be used for the symbolic access.
   * @param dataAccess
   */
  public ByteDataSymbolicAccess(ByteDataAccessSimple dataAccess)
  { this.dataAccess = dataAccess;
  }
  
  
  public final void addVariable(String name, ByteDataSymbolicAccess.Variable variable){
    indexVariable.put(name, variable);
  }
  
  
  public void assignData(byte[] dataP, long time)
  {
    assignData(dataP, dataP.length, 0, time);
  }
  
  
  public void assignData(byte[] dataP, int length, int from, long time)
  { this.data = dataP;
    this.ixStartData = from;
    this.nrofData = length;
    assert( (from + length) <= data.length);
    try{  dataAccess.assign(data, length, from);
    } catch (IllegalArgumentException exc) { }
    dataAccess.setBigEndian(true);
    timeSetNewValue = time; 
    timeRequestNewValue = 0; 
  }
  
  /**Replace the data bytes in the buffer.
   * @param src source
   * @param srcpos maybe any position in the source buffer
   * @param srclen number of bytes in source buffer. It should be the same like number of bytes in this.data,
   *   but it may be lesser or more. Lesser: Only a part of data are replaces.
   * @param time timestamp of receive.
   */
  public void copyNewData(byte[] src, int srcpos, int srclen, long time){
    int len = srclen > data.length ? data.length : srclen;
    System.arraycopy(src, srcpos, data, 0, len);
    timeSetNewValue = time;
  }
  
  public int lengthData(){ return data ==null ? 0 : data.length; }
  
  /**Returns the time stamp (seconds after 1970) of the last call of {@link VariableAccess_ifc#requestValue(long)}
   * for any variable of this container.
   * @return 0 if no request is called after the last call of {@link #assignData(byte[])} or
   *   {@link #assignData(byte[], int, int)}.
   */
  public long isRequestedNewData(){ return timeRequestNewValue; }
  
  
  
  /**Searches a variable by name and returns it.
   * A variable is a description of the byte position. length, type in a byte[]-Array.
   * see {@link VariableContainer_ifc#getVariable(String)}
   * @param name The name 
   * @return null if not found.
   */
  @Override public Variable getVariable(String name)
  { return indexVariable.get(name);
  }
  
  /**Searches a variable by name and returns it.
   * A variable is a description of the byte position. length, type in a byte[]-Array.
   * @param name The name 
   * @return null if not found.
   * @throws NoSuchFieldException 
   */
  public Variable getVariableAssert(String name) throws NoSuchFieldException
  { ByteDataSymbolicAccess.Variable variable = indexVariable.get(name);
    if(variable == null){
      throw new NoSuchFieldException("ByteDataSymbolicAccess - Variable not found; " + name);
    }
    return variable;
  }
  
  public double getDouble(String name)
  { Variable variable = indexVariable.get(name);
    if(variable == null) throw new IllegalArgumentException("not found:" + name);
    return getDouble(variable, -1);
  }
  
  public double getDouble(Variable variable, int ixArray){ return variable.getDouble(ixArray); }
  
  
  /**Get a float value from byte-area.
   * @param name The name of the registered variable, maybe with "[ix]" where ix is a number.
   * @return The value
   */
  public float getFloat(String name)
  { final int[] ixArrayA = new int[1];
    final String sPathVariable = ByteDataSymbolicAccess.separateIndex(name, ixArrayA);
    Variable variable = indexVariable.get(sPathVariable);
    if(variable == null) return 9.999999F; //throw new IllegalArgumentException("not found:" + name);
    return variable.getFloat(ixArrayA);
  }
  
  
  public float getFloat(Variable variable){ return getFloat(variable, -1); }
  
  
  public float getFloat(Variable variable, int ixArray){ return variable.getFloat(ixArray); }
  
  
  
  
  public int getInt(String name)
  { Variable variable = indexVariable.get(name);
    if(variable == null) throw new IllegalArgumentException("not found:" + name);
    return getInt(variable, -1);
  }
  
  public int getInt(Variable variable, int ixArray){ return variable.getInt(ixArray); }
  
  
  public boolean getBool(String name)
  { Variable variable = indexVariable.get(name);
    if(variable == null) throw new IllegalArgumentException("not found:" + name);
    return getBool(variable, -1);
  }
  
  public boolean getBool(Variable variable, int ixArray)
  { boolean value = false;
    int bytePos = ixArray < 0 ? variable.bytePos : variable.bytePos + (ixArray >>3);  //sizeof(bool) is 1/8.
    if(bytePos > data.length-4) throw new IllegalArgumentException("file to short: " + data.length + ", necessary: " + bytePos);
    switch(variable.typeChar){
    case 'I': value = 0 != dataAccess.getIntVal(bytePos, 4); break;
    case 'J': value = 0 != dataAccess.getIntVal(bytePos, 8); break;
    case 'S': value = 0 != dataAccess.getIntVal(bytePos, 2); break;
    case 'B': value = 0 != dataAccess.getIntVal(bytePos, 1); break;
    case 'Z': {
      byte byteVal = (byte)dataAccess.getIntVal(bytePos, 1);
      value = (byteVal & variable.bitMask) !=0;  //TODO use ixArray
    }break;
    default: new IllegalArgumentException("fault type, expected: float, found: " + variable.typeChar);
    }//switch
    return value;
  }
  

  
  
  public void setFloat(String name, float value)
  { Variable variable = indexVariable.get(name);
    if(variable == null) throw new IllegalArgumentException("not found:" + name);
    variable.setFloat(value, -1);
  }
  
  public void setFloat(Variable variable, int ixArray, float value){ variable.setFloat(value, ixArray); }
  
  
  public void setInt(String name, int value)
  { Variable variable = indexVariable.get(name);
    if(variable == null) throw new IllegalArgumentException("not found:" + name);
    setInt(variable, -1, value);
  }
  
  public void setInt(Variable variable, int ixArray, int value){ variable.setInt(value, ixArray); }
  
  
  /**Separates an string-given index from a path.
   * The index may contain in the sPathValue at end in form "[index]",
   * where index is numerical, maybe hexa starting with "0x"
   * @param sPathValue The path
   * @param ix An array with the necessary number of  element to store the index. It may be null
   *           or shorter then the number of indices. Then the indeces are not stored.
   * @return The sPathValue without the index.
   */
  public static String separateIndex(String sPathValue, int[] ix)
  {
    int posArray = sPathValue.indexOf('[');
    final String sPathVariable = posArray <0 ? sPathValue : sPathValue.substring(0, posArray);
    final int ixArray;
    if(posArray >=0){
      int posArrayEnd = sPathValue.indexOf(']', posArray+1); 
      String sIxArray = sPathValue.substring(posArray+1, posArrayEnd); 
      
      //TODO detect 0x, copy from ...
      ixArray = Integer.parseInt(sIxArray);
    } else {
      ixArray = -1; //no array access.
    }
    if(ix != null){
      ix[0] = ixArray;
    }
    return sPathVariable;
  }


  /**see {@link VariableContainer_ifc#setCallbackOnReceivedData(Runnable)}
   * It is not implemented here.
   */
  @Override public void setCallbackOnReceivedData ( Runnable callback ) {
    // do nothing
  }

  
  
}
