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
 * 2010-01-12 Hartmut chg: sizeof_Object_Jc = 0x18, regarding extension with 8 Bytes valid in all sources.
 * 2005..2009: Hartmut: some changes
 * 2005 Hartmut created
 */
package org.vishia.byteData;



/**This class enables the access to binary data which presents a C-type ObjectJc.
 * 
 * @author Hartmut Schorrig
 *
 */
public class Object_Jc  extends ByteDataAccessBase
{
  public final static int kPos_objIdentSize = 0;
  private final static int kPos_ownAdress = 4;
  private final static int kPos_reflectionClassAdress = 12;
  
  
  /** nrofBytes of the C-POD type Object_Jc */
  public final static int sizeof_Object_Jc = 0x18;// 0x0c;

  public final static int mArray_objectIdentSize = 0xc0000000;
  
  public final static int mSizeBits_typeSizeIdent =    0x30000000; 
  
  /**Marking of size in attribute objectIdentSize in Object_Jc. The Bit representing by the constant is set
   * if it is a medium or large size object. This bits are part of the ident signification of objects.
   * The kIsSmallSize_typeSizeIdent is only used for better comment in source. 
   **/
  public final static int kIsSmallSize_typeSizeIdent = 0x00000000 
                        , mIsMediumSize_typeSizeIdent = 0x10000000 
                        , mIsLargeSize_typeSizeIdent = 0x20000000; 
  
  private final static int mSizeSmall_typeSizeIdent =   0x0000ffff; 
  public final static int mTypeSmall_typeSizeIdent =   0x0fff0000; 
  
  private final static int mSizeMedium_typeSizeIdent =   0x000fffff; 
  public final static int mTypeMedium_typeSizeIdent =   0x0ff00000;

  private final static int mSizeLarge_typeSizeIdent =   0x00ffffff; 
  public final static int mTypeLarge_typeSizeIdent =   0x1f000000;
  
  public final static int OBJTYPE_Class_Jc =           Object_Jc.kIsSmallSize_typeSizeIdent + 0x0ff80000; 
 // public final static int OBJTYPE_ReflectionImage_Jc = Object_Jc.mIsLargeSize_typeSizeIdent + 0x3e000000; 
  public final static int OBJTYPE_ReflectionImageBaseAddress_Jc = Object_Jc.kIsSmallSize_typeSizeIdent + 0x0ff70000; 
  public final static int OBJTYPE_Field_Jc =           Object_Jc.kIsSmallSize_typeSizeIdent + 0x0FF50000; 

//NEW
  /*
  public final static int OBJTYPE_Class_Jc =           Object_Jc.kIsSmallSize_typeSizeIdent + 0x0ff80000; 
  public final static int OBJTYPE_ReflectionImage_Jc = Object_Jc.mIsLargeSize_typeSizeIdent + 0x1e000000; 
  public final static int OBJTYPE_ReflectionImageBaseAddress_Jc = Object_Jc.kIsSmallSize_typeSizeIdent + 0x0ff70000; 
  */
  
  /**Constructs the access to binary data of the head data of ObjectJc.
   * Note that more data should be added by {@link #addChild(ByteDataAccessBase)} etc.
   */
  public Object_Jc()
  { super(sizeof_Object_Jc);
    setBigEndian(false);
  }

  
  /**Constructs the access to binary data of a type based on ObjectJc
   * @param size Size of that type inclusively ObjectJc.
   */
  public Object_Jc(int size)
  { super(size);
    setBigEndian(false);
  }

  
  
  /**returns the size of the Object_Jc data correlating with typeSizeIdent of Object_Jc.
   * 
   * @return nrof bytes.
   */
  public int getSizeObject()
  {
    int nResult = 0;
    int typeSizeIdent = getInt32(Object_Jc.kPos_objIdentSize);
   
    if( (typeSizeIdent & mIsLargeSize_typeSizeIdent) != 0)
    {
      nResult = typeSizeIdent & Object_Jc.mSizeLarge_typeSizeIdent;
    }
    else if( (typeSizeIdent & mIsMediumSize_typeSizeIdent) != 0)
    {
      nResult = typeSizeIdent & Object_Jc.mSizeMedium_typeSizeIdent ;
    }
    else //small
    {
      nResult = typeSizeIdent & Object_Jc.mSizeSmall_typeSizeIdent;
    }
    return nResult; 
  }
  
  /**returns the number of dimensions if it is an ObjectArray_Jc, or 0 if it is scalar.
   * 
   * @return
   */
  public int nrofArrayDimensions()
  { int typeSizeIdent = getInt32(Object_Jc.kPos_objIdentSize);
    return (typeSizeIdent >>30) & 0x03; 
  }
  
  
  public int getOwnAdress()
  {
      return getInt32(kPos_ownAdress);
  }
  
  
  /**gets the address value of Reflection Class_Jc stored in the binary data of Object_Jc.
   * To assign an existing Reflection class use {@link Reflection_Jc.getClassFromMemAddr(int)},
   * 
   * @return the address value from the original memory location.
   */
  public int getReflectionClass()
  {
      return getInt32(kPos_reflectionClassAdress);
  }
  /*  
  public int getType(int idxChild)
  {
      int nResult = 0;
      int typeSizeIdent = getInt32(Object_Jc.kPos_objIdentSize + idxChild);
      int nSizeInfo = typeSizeIdent & mSizeBits_typeSizeIdent;
      
      int nTypeMask = 0;
      
      if (nSizeInfo == 0x30000000)
  //        if (nSizeInfo == kIsSmallSize_typeSizeIdent)
          nTypeMask = ~Object_Jc.mSizeSmall_typeSizeIdent;
      else
          nTypeMask = ~Object_Jc.mSizeLarge_typeSizeIdent;
      
      nResult = typeSizeIdent & nTypeMask;
      return nResult;
  }
  */
  protected int getType(int idxChild) 
  {
    int nResult = 0;
    int typeSizeIdent = getInt32(Object_Jc.kPos_objIdentSize + idxChild);
    int nTypeMask = 0;
     
    if( (typeSizeIdent & mIsLargeSize_typeSizeIdent) != 0)
    {
      nTypeMask = Object_Jc.mTypeLarge_typeSizeIdent;
    }
    else if( (typeSizeIdent & mIsMediumSize_typeSizeIdent) != 0)
    {
      nTypeMask = Object_Jc.mTypeMedium_typeSizeIdent ;
    }
    else //small
    {
      nTypeMask = Object_Jc.mTypeSmall_typeSizeIdent;
    }
    
    nResult = typeSizeIdent & nTypeMask;
    return nResult; 
  }
  //*/
  public int getType() 
  {
      return getType(0);
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

  /**assign the data at position of Object_Jc to a RawDataAccess to access data 
   * described by positions and types getted from reflection. 
   * 
   * @param dst The destination ByteDataAccess.
   */
  public void castToRawDataAccess(RawDataAccess dst) 
  { assignCasted(dst, 0, -1);  
    //catch(AccessException exc){throw new RuntimeException("unexpected AccessException");}; //it should be never thrown
  }
  
  
  
}
