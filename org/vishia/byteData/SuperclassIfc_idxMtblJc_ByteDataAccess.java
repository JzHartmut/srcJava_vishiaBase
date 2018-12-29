package org.vishia.byteData;

public class SuperclassIfc_idxMtblJc_ByteDataAccess extends ByteDataAccessBase
{
  private static final int
    kPos_reserve = 0
  , kPos_ixMtbl = kPos_reserve + 4
  , kPos_field = kPos_ixMtbl + 4  //integer-size
  ;
  public static final int size = kPos_field + Field_Jc.sizeOf_Field_Jc;
  
  
  public static final int INIZ_ID = 0xff9;
  
  final ObjectArray_Jc head_access = new ObjectArray_Jc();
  
  int nrofElements;
  
  public final Field_Jc field_access = new Field_Jc();
  
  /**Constructor is the instance to create bin data. it is not the bin data itself. Do not create or fill bin data.*/
  public SuperclassIfc_idxMtblJc_ByteDataAccess()
  { super(size);
  }
  
  
  
  /**assigns data to inner accesses 
  */
  public void addInnerChildren()
  { this.addChildAt(kPos_field, field_access);
  }

  
  
  public int set_TypeOffs(int addrInSameBuffer) {
    int posInBuffer = field_access.getPositionInBuffer_type();
    field_access.set_type(addrInSameBuffer - posInBuffer);
    return posInBuffer;
  }
  
  public void set_name(String name) {
    field_access.setName(name);
  }
  
  
  
}
