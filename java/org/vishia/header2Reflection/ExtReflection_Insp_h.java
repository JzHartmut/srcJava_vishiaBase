package org.vishia.header2Reflection;

import org.vishia.byteData.Object_Jc;

public class ExtReflection_Insp_h
{
  
  public static class ExtReflection_Insp extends Object_Jc
  {
    
    
    final static int kIdx_sign = Object_Jc.sizeof_Object_Jc
      , kIdx_nrofRelocEntries = kIdx_sign + 4
      , kIdx_arrayClasses     = kIdx_nrofRelocEntries + 4
      , kIdx_classDataBlock   = kIdx_arrayClasses + 4
      , kIdxAfterLast         = kIdx_classDataBlock +4
      ;
    
    ExtReflection_Insp(byte[] emptyData)
    { super(kIdxAfterLast);
      super.setBigEndian(true);
      super.assignClear(emptyData);
      setInt32(kPos_objIdentSize, 0x1234);  //only for test.
    }
    
    
    
    public ExtReflection_Insp(){
      super(kIdxAfterLast);
    }
    
    void set_sign(int value)
    {  setInt32(kIdx_sign, value);
    }
    
    void set_nrofRelocEntries(int value)
    {  setInt32(kIdx_nrofRelocEntries, value);
    }
    
    int get_nrofRelocEntries()
    {  return getInt32(kIdx_nrofRelocEntries);
    }
    
    void set_arrayClasses(int value)
    {  setInt32(kIdx_arrayClasses, value);
    }
    
    int get_arrayClasses()
    {  return getInt32(kIdx_arrayClasses);
    }
    
    void set_classDataBlock(int value)
    {  setInt32(kIdx_classDataBlock, value);
    }
    
    int get_classDataBlock()
    {  return getInt32(kIdx_classDataBlock);
    }
    
    
    void add_relocateAddr(int posReloc) {
      super.addChildInteger(4, posReloc);
    }
    
    
    int get_relocateAddr(int ix) {
      return super.getInt32(4*ix + kIdxAfterLast);
    }
    
  
  }  
}
