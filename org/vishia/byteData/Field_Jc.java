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
 * 2010-01-16: Hartmut new: some new methods, especially to write informations.
 * 2005..2009: Hartmut: some changes
 * 2005 Hartmut created
 */
package org.vishia.byteData;

import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.TreeMap;


    
/**
 * The Field_Jc supports the access to a single data field of a users byte image.
 * See {@link Class_Jc.getDeclaredField(String)} and {@link Class_Jc.getDeclaredFields()}  
 */ 
public  class Field_Jc extends ByteDataAccess
{
      /** Position of the name in a Field_Jc-POD*/
      private static final int kPosName = 0x0;
      
      /** Nr of bytes of a name in a Field_Jc-POD*/
      public static final int kLengthName = 30;
    
      /** Position of the type reference in a Field_Jc-POD*/
      private static final int kPos_nrofArrayElements = kPosName + kLengthName;
    
      /** Position of the type reference in a Field_Jc-POD*/
      private static final int kPos_type = kPos_nrofArrayElements + 2;
    
      /** Position of the modifier in a Field_Jc-POD*/
      private static final int kPos_bitModifiers = kPos_type + 4;
    
      /** Position of the Position of the data in a Field_Jc-POD*/
      private static final int kPos_position = kPos_bitModifiers + 4;
      
      private static final int kPos_offsetToObjectifcBase = kPos_position + 2;
      
      private static final int kPos_declaringClass = kPos_offsetToObjectifcBase + 2;
    
      private static final int sizeOf_Field_Jc = kPos_declaringClass + 4;
      
 
  public static final int REFLECTION_void  =              0x01; 
  public static final int REFLECTION_int64  =             0x02; 
  public static final int REFLECTION_uint64 =             0x03;
  public static final int REFLECTION_int32  =             0x04;
  public static final int REFLECTION_uint32 =             0x05;
  public static final int REFLECTION_int16  =             0x06;
  public static final int REFLECTION_uint16 =             0x07;
  public static final int REFLECTION_int8   =             0x08;
  public static final int REFLECTION_uint8  =             0x09;
  public static final int REFLECTION_int    =             0x0a;
  public static final int REFLECTION_uint   =             0x0b;
  public static final int REFLECTION_float  =             0x0c;
  public static final int REFLECTION_double =             0x0d;
  public static final int REFLECTION_char   =             0x0e;
  public static final int REFLECTION_bool   =             0x0f;
  public static final int REFLECTION_boolean=             0x0f;
  public static final int REFLECTION_String =             0x10;
  public static final int REFLECTION_bitfield =           0x17;
 
  /**Helper class for TreeMap of scalar type properties. */
  public static class TypeSizeIdent
  { public final int size, ident;
    public TypeSizeIdent(int size, int ident)
    { this.size = size;
      this.ident = ident;
    }
  }

  
  private final static Map<String, TypeSizeIdent> scalarTypeProperties = new TreeMap<String, TypeSizeIdent>();

  /**Gets the type ident and size of a given scalar type.
   * 
   * @param sType The type 'int', 'int32' etc.
   * @return null if it isn't a known scalar type.
   */
  public static TypeSizeIdent getTypeSizeIdent(String sType)
  { if(scalarTypeProperties.size()==0)
    { //initialize only one time in life cycle, it is static.
      scalarTypeProperties.put("void",   new TypeSizeIdent(8, REFLECTION_void));
      scalarTypeProperties.put("int64",  new TypeSizeIdent(8, REFLECTION_int64));
      scalarTypeProperties.put("uint34", new TypeSizeIdent(8, REFLECTION_uint64));
      scalarTypeProperties.put("int32",  new TypeSizeIdent(4, REFLECTION_int32));
      scalarTypeProperties.put("uint32", new TypeSizeIdent(4, REFLECTION_uint32));
      scalarTypeProperties.put("int16",  new TypeSizeIdent(2, REFLECTION_int16));
      scalarTypeProperties.put("uint16", new TypeSizeIdent(2, REFLECTION_uint16));
      scalarTypeProperties.put("int8",   new TypeSizeIdent(1, REFLECTION_int8));
      scalarTypeProperties.put("uint8",  new TypeSizeIdent(1, REFLECTION_uint8));
      scalarTypeProperties.put("int",    new TypeSizeIdent(4, REFLECTION_int));
      scalarTypeProperties.put("uint",   new TypeSizeIdent(4, REFLECTION_uint));
      scalarTypeProperties.put("float",  new TypeSizeIdent(4, REFLECTION_float));
      scalarTypeProperties.put("double", new TypeSizeIdent(8, REFLECTION_double));
      scalarTypeProperties.put("char",   new TypeSizeIdent(1, REFLECTION_char));
      scalarTypeProperties.put("bool",   new TypeSizeIdent(1, REFLECTION_bool));
      scalarTypeProperties.put("boolean", new TypeSizeIdent(1, REFLECTION_bool));
    }
    return scalarTypeProperties.get(sType); 
  }
  
  
  
      public static final int REFLECTION_Object_Jc =         0x41 ;
//    #define REFLECTION_Object_Array_Jc    ((struct Class_Jc_t const*) 0x42 )
//    #define REFLECTION_String_Jc          ((struct Class_Jc_t const*) 0x43 )
      public static final int REFLECTION_ObjectRefValues_Jc =0x44 ;
      public static final int REFLECTION_Class_Jc =          0x45 ;
      public static final int REFLECTION_Class_Jc_t =        0x45 ;
      
      
      
  /**Definition of type ident see Reflection_Jc.h. */    
  public final static int OBJTYPE_Field_Jc =  0x0FF50000; 
      
  public Field_Jc()
  {
  }

  
  
  
   /** Hat keine head informations!!! Verwaltungsklasse ist ObjectArray_Jc */
      public int specifyLengthElementHead()
      {
        return sizeOf_Field_Jc; //kPos_position + 8;
      }

      /** inherit from ByteDataAccess. This method is left empty because only given data are processed. */
      protected void specifyEmptyDefaultData()
      {
      }

      /** inherit from ByteDataAccess. Specifies the length of the head informations, used inside superclass. */
      protected int specifyLengthElement()
      { return sizeOf_Field_Jc;
      }

  public int getTypeSize(int nType)
  {
      int nResult = 0;
      
      switch (nType)
      {
      case REFLECTION_int64: 
      case REFLECTION_double:
      case REFLECTION_uint64: 
          nResult =  8;
          break;
      case REFLECTION_int32:
      case REFLECTION_bool: //???
      case REFLECTION_int:
      case REFLECTION_uint32:
      case REFLECTION_uint:
      case REFLECTION_float:    
          nResult =  4;
          break;
      case REFLECTION_int16:
      case REFLECTION_uint16:
           nResult =  2;
          break;
      case REFLECTION_int8:
      case REFLECTION_uint8:
      case REFLECTION_char:    
          nResult =  1;
          break;
      default:
          nResult = 4;
          break;
       }
      return nResult;
  }
      
      /** Gets the name of the field, readed from the data image from target system.
       * It is assumped that the encoding is the westeuropean standard 8-bit,
       * because only ASCII-7-bit-chars are expected.
       * 
       * @return The name of the field.
       */
      public String getName() 
      { String ret; 
        int idxName = idxBegin + kPosName;
        int idxEnd =  idxName + kLengthName;
        while(idxEnd > idxName && data[--idxEnd]==0);
        try{ ret = new String(data, idxBegin + kPosName, idxEnd - idxName +1, "ISO-8859-1"); }
        catch(UnsupportedEncodingException exc){ throw new RuntimeException("ISO-8859-1 encoding is not supported.");};
        return ret;
      }
 
  public void setName(String sName)
  {
    super.setString(kPosName, kLengthName, sName);
  }

      
      
      public  String getValue(Object_Jc Data)
      {
          String sResult = "";
          int idxOffset= (Data.idxCurrentChild - Data.idxBegin) + this.getPosValue();//Data.idxChild - Data.idxBegin; //DataStart + offset
          
          switch (getType())
          {
          case REFLECTION_int64: 
          case REFLECTION_double:
          case REFLECTION_uint64: 
              sResult += convert2String(Data.getInt64(idxOffset));
              break;
          case REFLECTION_int32:
          case REFLECTION_bool: //???
          case REFLECTION_int:
              sResult +=  convert2String(Data.getInt32(idxOffset));
              break;
          case REFLECTION_uint32:
          case REFLECTION_uint:
              sResult +=  convert2String(Data.getInt32(idxOffset));
              break;
          case REFLECTION_int16:
              sResult +=  convert2String(Data.getInt16(idxOffset));
              break;
          case REFLECTION_uint16:
              sResult +=  convert2String(Data.getUint16(idxOffset));
              break;
          case REFLECTION_int8:
              sResult +=  convert2String(Data.getInt8(idxOffset));
              break;
          case REFLECTION_uint8:
              sResult +=  convert2String(Data.getUint8(idxOffset));
              break;
          case REFLECTION_float:
              sResult +=  Data.getFloat(idxOffset);
              break;
          case REFLECTION_char:
              sResult +=  Data.getChar(idxOffset);
              break;
          case REFLECTION_Object_Jc:
//        #define REFLECTION_Object_Array_Jc    ((struct Class_Jc_t const*) 0x42 )
//        #define REFLECTION_String_Jc          ((struct Class_Jc_t const*) 0x43 )
          case REFLECTION_ObjectRefValues_Jc:
          case REFLECTION_Class_Jc:
          default:
              sResult += "###";
              break;
           }
          return sResult;
      }
      
      private String convert2String(int n) 
      {
        String str = n + " [0x" + Integer.toHexString(n) + "]";// Data.getInt32(idxOffset);
        return str;
    }
      
      private String convert2String(double n) 
      {
        String str = n + " [0x" + Double.toHexString(n) + "]";// Data.getInt32(idxOffset);
        return str;
    }      

    public int getTypeSize()
      {
          int nResult = 0;
          int nTypeCode = getType();
      
          switch (nTypeCode)
          {
            case REFLECTION_Object_Jc:
                nResult =  Object_Jc.sizeof_Object_Jc;
                break;
      //    #define REFLECTION_Object_Array_Jc    ((struct Class_Jc_t const*) 0x42 )
      //    #define REFLECTION_String_Jc          ((struct Class_Jc_t const*) 0x43 )
            case REFLECTION_ObjectRefValues_Jc:
                nResult =  4;
                break;
            case REFLECTION_Class_Jc:
                nResult =  Class_Jc.sizeof_Class_Jc;
                break;
            default:
                nResult = getTypeSize(nTypeCode);
          }
          
          return nResult;
      }
      
  public int getType()
  {
    return getInt32(kPos_type); //4 bytes
  }

  
  public int getModifiers()
  { return getInt32(kPos_bitModifiers);
  }
  
  
  /**gets the number of bytes if it is a primitive type
   * or gets 0 elsewhere. It is the content of bits mPrimitiv_Modifier_reflect_Jc
   * @return
   */
  public int getNrofBytesPrimitiveType()
  { int modifier = getModifiers();
    return (modifier >> 16) & 0xf;
  }
  
  
            
  public int getDeclClass()
  {
    return getInt32(kPos_declaringClass); //4 bytes
  }

      
      /** Gets the value from any data byte structure descibed with XmlBinCodeElmeent
       *  TODO Type Kontrolle auskomentiert
       * 
       * @param obj
       * @return
       * @throws IllegalArgumentException
       * @throws IllegalAccessException
       */
      public float getFloat(ByteDataAccess obj)
      throws IllegalArgumentException
//             IllegalAccessException
      {  
//          if (getType() != REFLECTION_float)
//              throw new IllegalArgumentException();
          
          int idx = this.getInt32(kPos_position);
          return obj.getFloat(idx);
      }
    
      public long  getInt32(ByteDataAccess obj)
//      throws IllegalArgumentException,
//             IllegalAccessException
      { 
//        if (getType() != REFLECTION_int32)
//        throw new IllegalArgumentException();
         
          int idx = this.getInt32(kPos_position);
          return obj.getInt32(idx);
      }
      
      public long  getInt16(ByteDataAccess obj)
//    throws IllegalArgumentException,
//           IllegalAccessException
    { 
//        if (getType() != REFLECTION_int16)
//        throw new IllegalArgumentException();
          
        int idx = this.getInt32(kPos_position);
        return obj.getInt16(idx);
    }
     
      /** Gets the position of the data appropriate to this field inside a instance, readed from the data image from target system.
       * 
       * @return position of the data appropriate to this field inside a instance.
       */
      public int getPosValue() 
      { return getInt32(kPos_position); 
      }
      
      
  public void set_nrofArrayElements(int value){ super.setInt16(kPos_nrofArrayElements, value); }
      
  public void set_type(int value){ super.setInt32(kPos_type, value); }
      
  public void set_bitModifiers(int value){ super.setInt32(kPos_bitModifiers, value); }
      
  public void set_position(int value){ super.setInt16(kPos_position, value); }
      
  public void set_offsetToObjectifcBase(int value){ super.setInt32(kPos_offsetToObjectifcBase, value); }
      
  public int setOffs_declaringClass(int value)
  { int posField = getPositionInBuffer() + kPos_declaringClass; 
    super.setInt32(kPos_declaringClass, value - posField);  
    return posField; 
  }
      
  
  public int getPositionInBuffer_type(){ return getPositionInBuffer() + kPos_type; }
      
}


