package org.vishia.util;

/**
 * @author Hartmut
 * @deprecated, see {@link DataAccess}
 */
public class ObjectValue
{

  public static int getInt(Object val){
    if(val instanceof Byte){ return ((Byte)val).byteValue(); }
    else if(val instanceof Short){ return ((Short)val).shortValue(); }
    else if(val instanceof Integer){ return ((Integer)val).intValue(); }
    else if(val instanceof Long){ return (int)((Long)val).longValue(); }
    else if(val instanceof Float){ return (int)((Float)val).floatValue(); }
    else if(val instanceof Double){ return (int)((Double)val).doubleValue(); }
    else return 0;
  }
  
  
  public static float getFloat(Object val){
    if(val instanceof Byte){ return ((Byte)val).byteValue(); }
    else if(val instanceof Short){ return ((Short)val).shortValue(); }
    else if(val instanceof Integer){ return ((Integer)val).intValue(); }
    else if(val instanceof Long){ return ((Long)val).longValue(); }
    else if(val instanceof Float){ return ((Float)val).floatValue(); }
    else if(val instanceof Double){ return (float)((Double)val).doubleValue(); }
    else return 0;
  }
  
  
  
}
