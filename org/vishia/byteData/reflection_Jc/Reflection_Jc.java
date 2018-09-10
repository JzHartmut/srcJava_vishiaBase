package org.vishia.byteData.reflection_Jc;

import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

import org.vishia.byteData.Class_Jc;
import org.vishia.byteData.Field_Jc;
import org.vishia.byteData.ObjectArray_Jc;


/**This class contains all reflection information from CRuntimeJavalike 
 * read from a C application via {@link ByteDataAccessBase}.
 * The informations are prepared from pure Byte data to comfortable access in Java.
 */
public class Reflection_Jc
{

  private TreeMap<Integer, Reflection_Jc.Class> allClassesByMemAddr = new TreeMap<Integer, Reflection_Jc.Class>();
  
  private TreeMap<String, Reflection_Jc.Class> allClassesByName = new TreeMap<String, Reflection_Jc.Class>();
  
  private TreeMap<Integer, FieldAccess> allFieldArraysByMemAddr = new TreeMap<Integer, FieldAccess>();
  
  private List<Class> allClasses = new LinkedList<Class>();
  
  public Reflection_Jc.Class getClassFromMemAddr(int memAddr)
  { Class clazz = allClassesByMemAddr.get(new Integer(memAddr));
    return clazz;
  }
  
  /**Represents one ClassJc. */
  public static class Class
  { private String name;
    private Field[] fields;
    private int ownPodAddr;     //the original memory address
    private int fieldsPodAddr;  //the original memory address of the fields.
    
    public String getName(){ return name; }
    
    public String getFieldNamesColonSeparated()
    { String names = ""; 
      for(Field field: fields)
      { names += field.getName() + "; ";
      }
      return names;
    }
    
    public Reflection_Jc.Field[] getFields()
    { return fields;
    }  
  }
  
  
  /**Represents all FieldJc of a ClassJc. */
  public static class FieldAccess
  { public int ownPodAddr;
    public Field[] fields;
  }
  
  
  public static class Field
  { private String name;
    private int position;
    private int typeAddr;
    private int modifier;
    
    public final String getName(){ return name; }
    public final int getPosition(){ return position; }
    public final int getModifier(){ return modifier; }
    
    /**gets the number of bytes if it is a primitive type
     * or gets 0 elsewhere. It is the content of bits mPrimitiv_Modifier_reflect_Jc
     * @return
     */
    public int getNrofBytesPrimitiveType()
    { return (modifier >> 16) & 0xf;
    }

    
    /**Returns the nrof bytes if it is an integer type or 0 if it isn't. 
     * 
     * The number of bytes is returned negativ if it is a signed type and positiv it is an unsigned type.
     * @deprecated see {@link #bytesInteger()}
     */
    public final int isInteger(){ return bytesInteger(); }
    
    /**Returns the nrof bytes if it is an integer type or 0 if it isn't. 
     * 
     * The number of bytes is returned negativ if it is a signed type and positiv it is an unsigned type.
     */
    public final int bytesInteger()
    { switch(typeAddr)
      { case Field_Jc.REFLECTION_uint32: return 4;
        case Field_Jc.REFLECTION_uint16: return 2;
        case Field_Jc.REFLECTION_uint8: return 1;
        case Field_Jc.REFLECTION_uint: return 4;
        case Field_Jc.REFLECTION_int32: return -4;
        case Field_Jc.REFLECTION_int16: return -2;
        case Field_Jc.REFLECTION_int8: return -1;
        case Field_Jc.REFLECTION_int: return -4;
        case Field_Jc.REFLECTION_char: return -1;
        default: return 0;
      }
    }

    /**Returns true if the type is float 
     */
    public final boolean isFloat()
    { return typeAddr == Field_Jc.REFLECTION_float;
    }

    /**Returns true if the type is double 
     */
    public final boolean isDouble()
    { return typeAddr == Field_Jc.REFLECTION_double;
    }

    /**Returns true if the type is float 
     */
    public final boolean isBoolean()
    { return typeAddr == Field_Jc.REFLECTION_bool || typeAddr == Field_Jc.REFLECTION_boolean ;
    }
    
    public String toString()
    { return name + ": typeAddr=" + Integer.toHexString(typeAddr);
    }
    
  }
  
  
  public void add(Class_Jc classData)
  { Reflection_Jc.Class clazz = new Reflection_Jc.Class(); 
    clazz.name = classData.getName();
    clazz.fieldsPodAddr = classData.getFieldsAddr();
    clazz.ownPodAddr = classData.getOwnAdress();
    allClassesByMemAddr.put(new Integer(clazz.ownPodAddr), clazz);
    allClassesByName.put(clazz.name, clazz);
    allClasses.add(clazz);
  }
  
  
  
  /**adds fields founded via a ObjectArray_Jc with the type-ident of Field_Jc.OBJTYPE_Field_Jc. 
   * Inside the Field_Jc elements of the array are detected and instanciated.
   * 
   * @param fieldData The ByteDataAccess for all field data
   * @return the fields from fieldData in the stored form in allFieldArraysByMemAddr, to report it.
   * @throws AccessException 
   */
  public Reflection_Jc.FieldAccess addfields(ObjectArray_Jc fieldData) 
  { int nrofFields = fieldData.getLength_ArrayJc();
    Reflection_Jc.FieldAccess fieldsAccess = new Reflection_Jc.FieldAccess();
    fieldsAccess.ownPodAddr = fieldData.getOwnAdress();
    fieldsAccess.fields = new Reflection_Jc.Field[nrofFields];
    for(int ii = 0; ii < nrofFields; ii++)
    { Field_Jc fieldAccess = new Field_Jc();
      fieldData.addChild(fieldAccess);
      
      Reflection_Jc.Field field = new Reflection_Jc.Field();
      field.name =     fieldAccess.getName();
      field.position = fieldAccess.getPosValue();
      field.typeAddr = fieldAccess.getType();
      field.modifier = fieldAccess.getModifiers();
      fieldsAccess.fields[ii] =field;
    }
    //Reflection_Jc.Field[] fields = new Reflection_Jc.Field[nrofFields]; 
    allFieldArraysByMemAddr.put(new Integer(fieldsAccess.ownPodAddr), fieldsAccess);
    return fieldsAccess;
  }
  
  
  
  public void assignAllElementsToClasses()
  { for(Class clazz: allClasses)
    { FieldAccess fieldAccess = allFieldArraysByMemAddr.get(clazz.fieldsPodAddr);
      if(fieldAccess != null)
      clazz.fields = fieldAccess.fields;
    }
  }
  
}