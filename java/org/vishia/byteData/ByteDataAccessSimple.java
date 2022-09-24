package org.vishia.byteData;

/**This class helps to access to binary data without any given structure or without a head of data.
 * <br><br>
 * Use this class only for instantiation but not as reference if children should be added,
 * only if the structure has not a head:
 * <pre>
 * ByteDataAccess accMy = new ByteDataAccessSimple(data);  //we have not head data
 * addMy.addChild(anyChild);                               //but there is a structure with children
 * <pre>
 * <br><br>
 * Use this class as reference to access to any byte position:
 * <pre>
 * ByteDataAccessSimple accMy = new ByteDataAccessSimple(data);  //any access to byte data necessary
 * accMy.setIntVal(ix, 3, value);      //sets the int value with 24 bit, it is an example
 * float var = accMy.getFloatVal(24);  //gets the bytes from position 24 and present them as float.
 * <pre>
 * In that case you can add children too, there are added from position 0. But it is not a good style
 * to mix the both aproaches.
 * 
 * @author Hartmut Schorrig
 *
 */
public class ByteDataAccessSimple extends ByteDataAccessBase
{
  
  /**Version, history and license
   * <ul>
   * <li>2018-09-17 Hartmut The class might to be use only for writing. 
   *   The ctors with lengthData {@link ByteDataAccessSimple#ByteDataAccessSimple(byte[], boolean, int)} are documented with 'filled Buffer'
   *   but they had clean it. Now correct adequate documentation. 
   * <li>2012-04-07 Hartmut created. The now deprecated RawDataAccess has the same approach,
   *   but this naming should be better to find out it. Additional constructors are given yet. 
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
  public static final String version = "2018-09-19";



  
  /** Constructs a new instance which is assigned to an empty buffer. */
  public ByteDataAccessSimple(byte[] data, boolean bigEndian)
  { super(0);
    super.bBigEndian = bigEndian; 
    assign(data);
    clearData();
  }

  
  
  /** Constructs a new instance not assigned, to read several buffers. */
  public ByteDataAccessSimple(boolean bigEndian)
  { super(0);
    super.bBigEndian = bigEndian; 
  }

  
  
  /** Constructs a new instance which is assigned to an filled buffer. */
  public ByteDataAccessSimple(byte[] data, boolean bigEndian, int lengthData)
  { super(0);
    assign(data, lengthData);
    super.bBigEndian = bigEndian; 
    assert(lengthData >=0 && lengthData <= data.length);
  }
  
  
  /** Constructs a new instance which is assigned to an filled buffer. */
  public ByteDataAccessSimple(byte[] data, boolean bigEndian, int lengthData, int index)
  { super(0);
    assign(data, lengthData, index);
    super.bBigEndian = bigEndian; 
    assert(lengthData >=0 && lengthData + index <= data.length);
  }
  

  
  /**Gets a integer value from any offset started from Object_Jc
   * 
   * @param idx byte-offset, the offset is not tested. If the offset is wrong, a null-pointer-exception throws.
   * @param nrofBytes to read.  
   * @return integer value
   */
  public final int getIntVal(int idx, int nrofBytes)
  { return (int)_getLong(idx, nrofBytes);
  }

  
  /**Gets a float value from any offset started from Object_Jc
   * 
   * @param idx byte-offset, the offset is not tested. If the offset is wrong, a null-pointer-exception throws.
   * @return float value
   */
  public final float getFloatVal(int idx)
  { return Float.intBitsToFloat((int)_getLong(idx, 4));
  }

  
  
  /**Gets a double value from any offset started from Object_Jc
   * 
   * @param idx byte-offset, the offset is not tested. If the offset is wrong, a null-pointer-exception throws.
   * @return double value
   */
  public final double getDoubleVal(int idx)
  { return Double.longBitsToDouble(_getLong(idx, 8));
  }

  
  
  public final void setIntVal(int idx, int nrofBytes, long value)
  { _setLong(idx, nrofBytes, value);
  }
  
  public final void setFloatVal(int idx, float value)
  { //call of the protected super method.
    super.setFloat(idx, value);
  }
  
  public final void setDoubleVal(int idx, double value)
  { //call of the protected super method.
    super.setDouble(idx, value);
  }

  
}
