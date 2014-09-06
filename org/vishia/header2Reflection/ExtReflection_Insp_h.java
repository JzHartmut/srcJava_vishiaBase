package org.vishia.header2Reflection;

import org.vishia.byteData.ByteDataAccess;
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
    { super.setBigEndian(true);
      super.assignEmpty(emptyData);
    }
    
    
    void set_sign(int value)
    {  setInt32(kIdx_sign, value);
    }
    
    void set_nrofRelocEntries(int value)
    {  setInt32(kIdx_nrofRelocEntries, value);
    }
    
    void set_arrayClasses(int value)
    {  setInt32(kIdx_arrayClasses, value);
    }
    
    void set_classDataBlock(int value)
    {  setInt32(kIdx_classDataBlock, value);
    }
    
    @Override
    protected void specifyEmptyDefaultData()
    {
      setInt32(0, 0x1234);
    }
  
  
    @Override
    public int specifyLengthElementHead()
    {
      // TODO Auto-generated method stub
      return kIdxAfterLast;
    }
  }  
}
