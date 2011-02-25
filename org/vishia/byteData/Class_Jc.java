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
    { 
    }
    
    
    public Field_Jc[] getFields() //reflection_jc
    {
        return null; //fields;
    }
 

    /**assigns data from a given Object_Jc instance 
     * which is assigned to a byte[] via {@link ByteDataAccess} superclass of Object_Jc.
     * The byte[] have to be containing valid data.
     */
    public void assignDataUpcast(Object_Jc base)
    { assignCasted_i(base, 0, kPos_Vtbl);
      //catch(Illegal exc){} //it's never thrown.
    }
    
    /** inherit from XmlBinCodeElement. Specifies the length of the head informations, used inside superclass. */
    public int specifyLengthElementHead()
    { return kPos_Last;
    }

    /** inherit from XmlBinCodeElement. This method is left empty because only given data are processed. */
    protected void specifyEmptyDefaultData()
    {
      Arrays.fill(data, super.idxBegin, super.idxBegin + kPos_Last, (byte)0);
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
  protected static final int kPos_attributes = kPos_nsize + 4;
  
  /** Position of the pointer to methods in a Class_Jc-POD*/
  private static final int kPos_methods = kPos_attributes + 4;
  
  /** Position of the pointer to the superclass reflection in a Class_Jc-POD*/
  private static final int kPos_superClass = kPos_methods + 4;
  
  /** Position of the pointer to the interfaces in a Class_Jc-POD*/
  private static final int kPos_interfaces = kPos_superClass + 4;
  
  /** Position of the modifier in a Class_Jc-POD*/
  private static final int kPos_modifiers = kPos_interfaces + 4;
  
  /** Position after this element*/
  private static final int kPos_Vtbl = kPos_modifiers + 4;
  
  private static final int kPos_Last = kPos_Vtbl + 4;
  
  /** nrofBytes of the C-POD type Class_Jc */
  public static final  int sizeof_Class_Jc = kPos_Vtbl +4;
 
  /**The object type defined in Reflect_Jc.h for Class_Jc-objects. */
  public final static int OBJTYPE_CLASS_Jc =  0x0ff80000; 

  /**Definition adequate Headerfile ReflectionJc.h in enum  Modifier_reflectJc_t: */
  public static final int 
      kBitPrimitiv_Modifier = 16
    , mPrimitiv_Modifier =             0x000f0000
    , kBitfield_Modifier =             0x00070000
    , mStatic_Modifier =               0x00000008 
    , kObjectArrayJc_Modifier =        0x00200000
    , kStaticArray_Modifier  =         0x00800000
    , kEmbedded_Modifier_reflectJc =   0x01000000
    , mReference_Modifier =            0x02000000
    , kEmbeddedContainer_Modifier =    0x10000000
    , kReferencedContainer_Modifier =  0x20000000
    , kEnhancedRefContainer_Modifier = 0x30000000
    ;    

  
  
  public int getFieldsAddr()
  {
      return getInt32(kPos_attributes);
  }
  
  /**Sets the value in element attributes so, that it is the offset to the given data.
   * @param value position in the same buffer where the attribute field (ObjectArray_Jc with children Field_Jc) is located.
   */
  public int setOffs_attributes(int value)
  { int posField = getPositionInBuffer() + kPos_attributes;
    setInt32(kPos_attributes, value - posField); 
    return posField;
  }
  
  //TODO not used yet, correct like above.
  public void setOffs_methods(int value){ setInt32(kPos_methods, value - getPositionInBuffer() + kPos_methods); }
  
  public void setOffs_superclasses(int value){ setInt32(kPos_superClass, value - getPositionInBuffer() + kPos_superClass); }
  
  public void setOffs_interfaces(int value){ setInt32(kPos_interfaces, value - getPositionInBuffer() + kPos_interfaces); }
  
  public void set_modifiers(int value){ setInt32(kPos_modifiers, value - getPositionInBuffer() + kPos_modifiers); }
  
  public void setOffs_mtbl(int value){ setInt32(kPos_Vtbl, value - getPositionInBuffer() + kPos_Vtbl); }
  
  
  
  
  //private static Charset charset = Charset.forName("iso-8859-1");
  

  
  /** Gets the name of the class, readed from the data image from target system.
   * It is assumped that the encoding is the westeuropean standard 8-bit,
   * because only ASCII-7-bit-chars are expected.
   * 
   * @return The name of the class.
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

  
  public void set_posObjectBase(int value){ super.setInt32(kPos_posObjectBase, value); }
 
  public void set_nSize(int value){ super.setInt32(kPos_nsize, value); }
 
  
  public String report()
  { 
      String sRet = "";
      Iterator<Class_Jc> i = null; //allClasses.iterator();
      while (i.hasNext())
      { 
        Class_Jc aktClass = i.next();
        sRet += "\nClass_Jc: " + aktClass.getName() + "\t OwnAdress 0x" + Integer.toHexString(aktClass.getOwnAdress()) 
                + "\t ReflectionClass 0x" + Integer.toHexString(aktClass.getReflectionClass()) + "\n";
        
      }      
    return sRet;      
  }
    
  
  
  
}
