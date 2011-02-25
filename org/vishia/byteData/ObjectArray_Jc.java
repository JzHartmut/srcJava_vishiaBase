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



public class ObjectArray_Jc extends Object_Jc {
    
  protected Class_Jc myReflection_Jc;
  
  public final static int kPosSizeof_ElObjectArray_Jc = sizeof_Object_Jc + 0;
  
  public final static int kPosMode_ObjectArray_Jc = kPosSizeof_ElObjectArray_Jc + 2;
  
  /** Position of the attributes length inside C-ObjectArray_Jc.*/
  public final static int kPos_length_ObjectArray_Jc = kPosMode_ObjectArray_Jc +2 ;
  
  public final static int sizeof_ObjectArray_Jc = kPos_length_ObjectArray_Jc + 4;
  
  //public final static int kIdxAfterLast = sizeof_ObjectArray_Jc + 2; 
  
  
  public ObjectArray_Jc()
  {
    
  }
  
   /**assigns data from a given Object_Jc instance 
   * which is assigned to a byte[] via {@link ByteDataAccess} superclass of Object_Jc.
   * The byte[] have to be containing valid data.
   */
  public void assignDataUpcast(Object_Jc base)
  { assignCasted_i(base, 0, sizeof_ObjectArray_Jc);  //kIdxAfterLast); 
    //catch(AccessException exc){} //it's never thrown.
  }
    
   
    
    
    public int specifyLengthElementHead() {
         return sizeof_ObjectArray_Jc;
    } 
    
    public int getLength_ArrayJc()
    { 
        return getInt32(kPos_length_ObjectArray_Jc);
    }
    
    public int getSizeofElement()
    { 
        return getInt16(kPosSizeof_ElObjectArray_Jc);
    }
    
    void setReflectionClass(Class_Jc par_reflect)
    {
        myReflection_Jc = par_reflect;
    }
    

  public void set_length(int value){ super.setInt32(kPos_length_ObjectArray_Jc, value); }  
    
  public void set_sizeElement(int value){ super.setInt16(kPosSizeof_ElObjectArray_Jc, value); }  
  
  public void set_mode(int value){ super.setInt32(kPosMode_ObjectArray_Jc, value); }  
  

  
    
  protected int specifyLengthCurrentChildElement()
  //throws AccessException
  { 
    return getSizeofElement();
  }


   
      
    
}
