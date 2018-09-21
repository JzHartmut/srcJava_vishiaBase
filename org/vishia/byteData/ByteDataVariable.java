package org.vishia.byteData;

/**This class contains some static sub classes for Variable with primitive types inside a {@link ByteDataAccessBase}-derived access class.
 * It is a new better concept to access single data in generated derived classes of ByteDataAccessBase.
 * Use the following pattern:
 * <pre>
 * class MyDataAccessClass extends ByteDataAccessBase {
 *   
 *   ByteDataVariable.Int16 firstValue = new ByteByteDataVariable.Int16(this, 0);
 *   
 *   ByteDataVariable.Uint16 value2 = new ByteByteDataVariable.Int16(this, 2);
 *   
 * }
 * </pre>
 * The access class may/should be generated from a header file. The positions are calculated from the header file
 * or are countered accurately.
 * 
 * The delegation to operations {@link ByteDataAccessBase#getInt16(int)} etc. are no more necessary to access. 
 * The access can be done instead via:<pre>
 *   int val = myAccess.value2.get();
 * </pre>
 * The important advantage is: The values are seen in debugging via toString-implementations. 
 * With them a better view to binary data is given.  
 * 
 * @author Hartmut Schorrig
 *
 */
public class ByteDataVariable
{

  /**The version, history and license. 
   * <ul>
   * <li>2018-09-16  Hartmut created
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
  public final static String version = "2018-09-18";

  
  /**Documentation adequate valid for other sub classes.
   * This class describes a int32_t value (C99) in data.
   *
   */
  public static class Int32 {
    
    private final int ixData;
    private final ByteDataAccessBase acc;
    
    /**Gets the value in the associated binary data. */
    public int get() { return acc.getInt32(ixData); } 
    
    /**Sets the value in the associated binary data. */
    public void set(int val) { acc.setInt32(ixData, val); } 
    
    /**Constructs on a given position inside the parent access. 
     * @param acc parent contains the data pointer and the base index in data.
     * @param ixData byte position relative in parent structure.
     */
    public Int32(ByteDataAccessBase acc, int ixData){
      this.acc = acc; this.ixData = ixData;
    }
    
    /**Shows the value for debugging view integer and hexa.  */
    @Override public String toString() { int val = get(); return Integer.toString(val) + " = 0x" + Integer.toHexString(val) + " @" + Integer.toString(ixData) + "=0x" + Integer.toHexString(ixData); }
  }

  
  /**Documentation adequate valid for other array sub classes.
   * This class describes a int32_t array  in data.
   *
   */
  public static class Int32Array {
    
    private final int ixData, lengthData;
    private final ByteDataAccessBase acc;
    
    /**The array can be inspect in debug view. The values are shown immediately. */
    public final Int32[] array; 
    
    
    /**Get a value from indexed element.
     * Note: an indexed element can be also accessed via <code>array[ix].get();</code>
     * @param ix the position in the one-dimensional array
     * @return value.
     */
    public int get(int ix) { return acc.getInt32(ixData, ix, lengthData); } 
    
    /**Set a value to the indexed element.
     * @param ix the position in the one-dimensional array
     * @param val
     */
    public void set(int ix, int val) { acc.setInt32(ixData, ix, lengthData, val); } 
    
   
    
    /**Constructs  on a given position inside the parent access. 
     * @param acc parent contains the data pointer and the base index in data.
     * @param ixData byte position relative in parent structure. 
     * @param size size in numbers of elements from the given type.
     */
    public Int32Array(ByteDataAccessBase acc, int ixData, int size){
      this.acc = acc; this.ixData = ixData; this.lengthData = size;
      this.array = new Int32[size];
      for(int ixa = 0; ixa < size; ++ixa) { array[ixa] = new Int32(acc, ixData + 4*ixa); }
    }
    
    /**Shows the value for debugging view integer and hexa.  */
    @Override public String toString() { return "[" + Integer.toString(lengthData)+ "] @" + Integer.toString(ixData) + "=0x" + Integer.toHexString(ixData); }
  }

  
  public static class Int16{
    private final int ixData;
    private final ByteDataAccessBase acc;
    
    public short get() { return acc.getInt16(ixData); } 
    public void set(int val) { acc.setInt16(ixData, val); } 
    
    public Int16(ByteDataAccessBase acc, int ixData){
      this.acc = acc; this.ixData = ixData;
    }
    
    @Override public String toString() { int val = get(); return Integer.toString(val) + " = 0x" + Integer.toHexString(val) + " @" + Integer.toString(ixData) + "=0x" + Integer.toHexString(ixData); }
  }

  
  public static class Uint16{
    private final int ixData;
    private final ByteDataAccessBase acc;
    
    public int get() { return acc.getUint16(ixData); } 
    public void set(int val) { acc.setUint16(ixData, val); } 
    
    public Uint16(ByteDataAccessBase acc, int ixData){
      this.acc = acc; this.ixData = ixData;
    }
    
    @Override public String toString() { int val = get(); return Integer.toString(val) + " = 0x" + Integer.toHexString(val) + " @" + Integer.toString(ixData) + "=0x" + Integer.toHexString(ixData); }
  }

  public static class Uint16Array{
    private final int ixData, lengthData;
    private final ByteDataAccessBase acc;
    
    public final Uint16[] array; 
    
    public int get(int ix) { return acc.getUint16(ixData, ix, lengthData); } 
    public void set(int ix, int val) { acc.setUint16(ixData, ix, lengthData, val); } 
    
    public Uint16Array(ByteDataAccessBase acc, int ixData, int size){
      this.acc = acc; this.ixData = ixData; this.lengthData = size;
      this.array = new Uint16[size];
      for(int ixa = 0; ixa < size; ++ixa) { array[ixa] = new Uint16(acc, ixData + 2*ixa); }
    }
    
    @Override public String toString() { return "[" + Integer.toString(lengthData)+ "] @" + Integer.toString(ixData) + "=0x" + Integer.toHexString(ixData); }
  }



  public static class Uint32{
    private final int ixData;
    private final ByteDataAccessBase acc;
    
    public int get() { return acc.getUint32(ixData); } 
    public void set(int val) { acc.setUint32(ixData, val); } 
    
    public Uint32(ByteDataAccessBase acc, int ixData){
      this.acc = acc; this.ixData = ixData;
    }
    
    @Override public String toString() { int val = get(); return Integer.toString(val) + " = 0x" + Integer.toHexString(val) + " @" + Integer.toString(ixData) + "=0x" + Integer.toHexString(ixData); }
  }

  public static class Uint32Array{
    private final int ixData, lengthData;
    private final ByteDataAccessBase acc;
    
    public final Uint32[] array; 
    
    public int get(int ix) { return acc.getUint32(ixData, ix, lengthData); } 
    public void set(int ix, int val) { acc.setUint32(ixData, ix, lengthData, val); } 
    
    public Uint32Array(ByteDataAccessBase acc, int ixData, int size){
      this.acc = acc; this.ixData = ixData; this.lengthData = size;
      this.array = new Uint32[size];
      for(int ixa = 0; ixa < size; ++ixa) { array[ixa] = new Uint32(acc, ixData + 4*ixa); }
    }
    
    @Override public String toString() { return "[" + Integer.toString(lengthData)+ "] @" + Integer.toString(ixData) + "=0x" + Integer.toHexString(ixData); }
  }




}
