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
import java.util.Arrays;
import java.util.Iterator;


/**
 * A Class_Jc supports the access to data from a memory image as byte[], 
 * with given Reflection Info for this memory image.  
 * The memory image to access to may be getted from any embedded controller programmed in C or Cplusplus.
 * The Class_Jc knows the reflection info via an other memory image of the reflection format,
 * defined in www.vishia.de/Java2C/CRuntimeJavalike/Reflect_Jc.h. The whole reflection image is managed by the
 * internal class of this class {@link Class_Jc.ByteImage}, any instance of class is getted by calling
 * {@link ByteImage.getClasses()} 
 */ 
public class Class_Jc extends Object_Jc
{ 
  
  //protected String name;
  
  //protected Field_Jc[] fields;
  
  
    /**creates an empty not assigned instance. */
    public Class_Jc()
    { super(kPos_Last);
    }
    
    
    public Field_Jc[] getFields() //reflection_jc
    {
        return null; //fields;
    }
 

    /**assigns data from a given Object_Jc instance 
     * which is assigned to a byte[] via {@link ByteDataAccessBase} superclass of Object_Jc.
     * The byte[] have to be containing valid data.
     */
    public void assignDataUpcast(Object_Jc base)
    { upcast(base, kPos_Last);
      //catch(Illegal exc){} //it's never thrown.
    }
    
    /** inherit from XmlBinCodeElement. Specifies the length of the head informations, used inside superclass. */
    public int XXXspecifyLengthElementHead()
    { return kPos_Last;
    }

  

  
  /** Position of the ownAddress in a Class_Jc-POD*/
  //private static final int kPosOwnAdress = 0x04;

  /** Position of the name in a Class_Jc-POD*/
  private static final int kPosName = Object_Jc.sizeof_Object_Jc;
  
  /** Nr of bytes of a name in a Class_Jc-POD*/
  public static final int kLengthName = 0x20;

  /** Position of sizeof the type*/
  private static final int kPos_posObjectBase = kPosName + kLengthName;
  
  /** Position of sizeof the type*/
  private static final int kPos_nsize = kPos_posObjectBase + 4;
  
  /** Position of the pointer to attributes in a Class_Jc-POD*/
  public static final int kPos_attributes = kPos_nsize + 4;
  
  /** Position of the pointer to methods in a Class_Jc-POD*/
  private static final int kPos_methods = kPos_attributes + 4;
  
  /** Position of the pointer to the superclass reflection in a Class_Jc-POD*/
  public static final int kPos_superClass = kPos_methods + 4;
  
  /** Position of the pointer to the interfaces in a Class_Jc-POD*/
  private static final int kPos_interfaces = kPos_superClass + 4;
  
  /** Position of the modifier in a Class_Jc-POD*/
  private static final int kPos_modifiers = kPos_interfaces + 4;
  
  /** Position after this element*/
  private static final int kPos_Vtbl = kPos_modifiers + 4;
  
  static final int kPos_Last = kPos_Vtbl + 4;
  
  /** nrofBytes of the C-POD type Class_Jc */
  public static final  int sizeof_Class_Jc = kPos_Vtbl +4;
 
  /**The object type defined in Reflect_Jc.h for Class_Jc-objects. */
  public final static int OBJTYPE_CLASS_Jc =  0x0ff80000; 

  /**The object type defined in Reflect_Jc.h for Class_Jc-objects. */
  public final static int INIZ_ID_ClassJc =  0xff8; 

  /**Definition adequate Headerfile ReflectionJc.h in enum  Modifier_reflectJc_t: */
  public static final int 
      kBitPrimitiv_Modifier = 16
    , mPrimitiv_Modifier =             0x000f0000
    , kBitfield_Modifier =             0x00070000
    , kHandlePtr_Modifier =            0x00090000
    , mStatic_Modifier =               0x00000008 
    , kObjectArrayJc_Modifier =        0x00200000
    , kStaticArray_Modifier  =         0x00800000
    , kEmbedded_Modifier_reflectJc =   0x01000000
    , kReference_Modifier =            0x02000000
    , mReference_Modifier =            0x02000000
    , mObjectJc_Modifier =             0x04000000
    , mObjectifcBaseJcpp_Modifier =    0x08000000
    , mObjectJcBased_Modifier =        0x0c000000  //both bits mObjectifcBaseJcpp and mObject
    , kEmbeddedContainer_Modifier =    0x10000000
    , kReferencedContainer_Modifier =  0x20000000
    , kEnhancedRefContainer_Modifier = 0x30000000
    ;    

  public void setIdentSize() {
    super.setIdentSize(true, false, INIZ_ID_ClassJc, sizeof_Class_Jc);
  }
  
  /**Returns the value which is stored on the pointer position for attributes.
   * It is a relative pointer for Bin-Reflection-Date 
   * but an absolute pointer of the physical address, may be different from the load address, for a data image access.
   */
  public int getFieldsAddr(){ return getInt32(kPos_attributes); }
  
  /**Returns the value which is stored on the pointer position for the super class. Relative or physical on target. */
  public int getSuperAddr(){ return getInt32(kPos_superClass); }
  
  /**Returns the value which is stored on the pointer position for the interfaces. Relative or physical on target. */
  public int getIfcAddr(){ return getInt32(kPos_interfaces); }
  
  
  
  /**Sets the value in element attributes so, that it is the offset to the given data.
   * @param value position in the same buffer where the attribute field (ObjectArray_Jc with children Field_Jc) is located.
   */
  public int setOffs_attributes(int value)
  { int posField = getPositionInBuffer() + kPos_attributes;
    setInt32(kPos_attributes, value - posField); //It is a relative offset stored from dst to src.
    return posField;  //used to store as relocate entry.
  }
  
  public int setOffs_superclasses(int value){ 
    int posField = getPositionInBuffer() + kPos_superClass;
    setInt32(kPos_superClass, value - posField);
    return posField;
  }
  
  public int setOffs_interfaces(int value) { 
    int posField = getPositionInBuffer() + kPos_interfaces;
    setInt32(kPos_interfaces, value - posField);
    return posField;
  }
  
  //TODO not used yet, correct like above.
  public void xxxsetOffs_methods(int value){ setInt32(kPos_methods, value - getPositionInBuffer() + kPos_methods); }
  
  
  public void xxxsetOffs_mtbl(int value)
  { setInt32(kPos_Vtbl, value - getPositionInBuffer() + kPos_Vtbl); }
  
  /**Sets the modifier in the ByteData. */
  public void set_modifiers(int value){ setInt32(kPos_modifiers, value); }
  
  
  //private static Charset charset = Charset.forName("iso-8859-1");
  

  
  /** Gets the name of the class, readed from the data image from target system.
   * It is assumped that the encoding is the westeuropean standard 8-bit,
   * because only ASCII-7-bit-chars are expected.
   * 
   * @return The name of the class.
   */
  public String getName(){ return getString(kPosName, kLengthName); } 
  /* the same, but with private access:
  { String ret; 
    int idxName = ixBegin() + kPosName;
    int idxEnd =  idxName + kLengthName;
    while(idxEnd > idxName && data[--idxEnd]==0);
    try{ ret = new String(data, ixBegin() + kPosName, idxEnd - idxName +1, "ISO-8859-1"); }
    catch(UnsupportedEncodingException exc){ throw new RuntimeException("ISO-8859-1 encoding is not supported.");};
    return ret;
  } */
  
  
  public void setName(String sName)
  {
    super.setString(kPosName, kLengthName, sName);
  }

  
  public void set_posObjectBase(int value){ super.setInt32(kPos_posObjectBase, value); }
 
  public void set_nSize(int value){ super.setInt32(kPos_nsize, value); }
 
  
  public String report()
  { 
    String sRet = "";
    Iterator<Class_Jc> i = null; //allClasses.iterator();
    if(i !=null) {
      while (i.hasNext())
      { 
        Class_Jc aktClass = i.next();
        sRet += "\nClass_Jc: " + aktClass.getName() + "\t OwnAdress 0x" + Integer.toHexString(aktClass.getOwnAdress()) 
                + "\t ReflectionClass 0x" + Integer.toHexString(aktClass.getReflectionClass()) + "\n";
        
      }      
    }
    return sRet;      
  }
    
  
  
  
}
