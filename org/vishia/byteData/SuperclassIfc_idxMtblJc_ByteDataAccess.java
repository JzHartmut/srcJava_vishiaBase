package org.vishia.byteData;

import org.vishia.header2Reflection.BinOutShow;
import org.vishia.header2Reflection.Header2Reflection;

/**This class allows access and write of Data for the C-language struct <code>SuperclassIfc_idxMtblJc_ByteDataAccess</code>
 * see emc/source/emC/Object_emC.h.
 * @author Hartmut Schorrig
 *
 */
public class SuperclassIfc_idxMtblJc_ByteDataAccess extends ByteDataAccessBase
{
  
  /**Version, history and license.
   * <ul>
   * <li>2018-12-31 Hartmut set {@link Field_Jc.EModifier#kEmbedded_Modifier_reflectJc} on {@link #set_Field(String)} 
   * <li>2018-12-28 Hartmut Now writes more Information of ObjectJc base data to help detection of instance types in the binary data.
   *     It is used for {@link BinOutShow}.
   * <li>2018-12-18 Hartmut new: Support for inheritance (derivation) for target-proxy-concept: usage of {@link SuperclassIfc_idxMtblJc_ByteDataAccess}. 
   * <li>2018-09-08 Hartmut {@link #missingClasses} for report.  
   * <li>2018-08-29 Hartmut improved, commented, using for Reflection generation binary with JZtxtcmd-script.  
   * <li>2010-01-11 Hartmut new: for {@link Header2Reflection}, binary output   
   * </ul>
   * 
   * <b>Copyright/Copyleft</b>:
   * For this source the LGPL Lesser General Public License,
   * published by the Free Software Foundation is valid.
   * It means:
   * <ol>
   * <li> You can use this source without any restriction for any desired purpose.
   * <li> You can redistribute copies of this source to everybody.
   * <li> Every user of this source, also the user of redistribute copies
   *    with or without payment, must accept this license for further using.
   * <li> But the LPGL is not appropriate for a whole software product,
   *    if this source is only a part of them. It means, the user
   *    must publish this part of source,
   *    but don't need to publish the whole source of the own product.
   * <li> You can study and modify (improve) this source
   *    for own using or for redistribution, but you have to license the
   *    modified sources likewise under this LGPL Lesser General Public License.
   *    You mustn't delete this Copyright/Copyleft inscription in this source file.
   * </ol>
   * If you are intent to use this sources without publishing its usage, you can get
   * a second license subscribing a special contract with the author. 
   * 
   * @author Hartmut Schorrig = hartmut.schorrig@vishia.de
   * 
   * 
   */
  //@SuppressWarnings("hiding")
  public static final String version = "2018-12-31";

  
  
  
  
  
  
  private static final int
    kPos_reserve = 0
  , kPos_ixMtbl = kPos_reserve + 4
  , kPos_field = kPos_ixMtbl + 4  //integer-size
  ;
  public static final int size = kPos_field + Field_Jc.sizeOf_Field_Jc;
  
  
  public static final int ID_refl_ClassOffset_idxVtblJc = 0xffa;  //0xff9;
  
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

  
  
  /**Sets the address of the type in the data as offset for relocation.
   * @param addrInSameBuffer
   * @return The position in the byte data to store for relocation
   */
  public int set_TypeOffs(int addrInSameBuffer) {
    int posInBuffer = field_access.getPositionInBuffer_type();
    field_access.set_type(addrInSameBuffer - posInBuffer);
    return posInBuffer;
  }
  
  /**Sets the current field of the super or interface array.
   * It sets the {@link Field_Jc.EModifier#kEmbedded_Modifier_reflectJc} too because it should be seen with the declared type in Inspector view. 
   * @param name "super" or the Interface name.
   */
  public void set_Field(String name) {
    field_access.setName(name);
    field_access.set_bitModifiers(Field_Jc.EModifier.kEmbedded_Modifier_reflectJc.e);
  }
  
  
  
}
