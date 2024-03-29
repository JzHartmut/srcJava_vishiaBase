/****************************************************************************/
/* Copyright/Copyleft:
 *
 * For this source the LGPL Lesser General Public License,
 * published by the Free Software Foundation is valid.
 * It means:
 * 1) You can use this source without any restriction for any desired purpose.
 * 2) You can redistribute copies of this source to everybody.
 * 3) Every user of this source, also the user of redistribute copies
 *    with or without payment, must accept this license for further using.
 * 4) But the LPGL ist not appropriate for a whole software product,
 *    if this source is only a part of them. It means, the user
 *    must publish this part of source,
 *    but don't need to publish the whole source of the own product.
 * 5) You can study and modify (improve) this source
 *    for own using or for redistribution, but you have to license the
 *    modified sources likewise under this LGPL Lesser General Public License.
 *    You mustn't delete this Copyright/Copyleft inscription in this source file.
 *
 * @author Hartmut Schorrig = hartmut.schorrig@vishia.de
 * @version 2009-06-15  (year-month-day)
 * list of changes:
 * 2010-01-16: Hartmut corr:  adaption to actual version of CRuntimeJavalike, 
 * 2005..2009: Hartmut: some changes
 * 2005 Hartmut created
 */
package org.vishia.byteData;

import org.vishia.util.Java4C;

/**This class is use-able to free access to data.
 * @deprecated, use {@link ByteDataAccessSimple} instead.
 */ 
@Deprecated public class RawDataAccess  extends ByteDataAccessBase
{
  
  
  
  public RawDataAccess()
  { super(0); 
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
  @Java4C.Inline
  public final double getDoubleVal(int idx)
  { return Double.longBitsToDouble(_getLong(idx, 8));
  }

  
  /**
   * @param idx
   * @param nrofBytes
   * @param value  xxxxxx
   */
  @Java4C.Inline
  public final void setIntVal(int idx, int nrofBytes, long value)
  { try{
      _setLong(idx, nrofBytes, value);  //test2
    }catch(Exception exc){
      throw new RuntimeException(exc); //only test
    }
  }
  
  public final void setFloatVal(int idx, float value)
  { //call of the protected super method.
    /** @Java4C.StringBuilderInThreadCxt*/
    if(idx < 0) throw new IndexOutOfBoundsException("setDoubleVal:" + idx); 
  	super.setFloat(idx, value);
  }
  
  @Java4C.Inline
  public final void setDoubleVal(int idx, double value)
  { //call of the protected super method.
    /** @Java4C.StringBuilderInThreadCxt*/
    if(idx < 0){ 
      @Java4C.StringBuilderInThreadCxt(sign="setDoubleValue-error") String msg = "setDoubleVal:" + idx; 
      throw new IndexOutOfBoundsException(msg); 
    }
    super.setDouble(idx, value);
  }
  
  
  @Override 
  @Java4C.Exclude
  public String toString(){ return super.toString(); }
  
}
