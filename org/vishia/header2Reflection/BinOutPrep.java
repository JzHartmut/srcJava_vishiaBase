package org.vishia.header2Reflection;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.vishia.byteData.ByteDataAccessBase;
import org.vishia.byteData.Class_Jc;
import org.vishia.byteData.Field_Jc;
import org.vishia.byteData.ObjectArray_Jc;
import org.vishia.byteData.RawDataAccess;
import org.vishia.util.StdHexFormatWriter;


/**This class prepares the binary output. 
 * 
 * @author Hartmut Schorrig
 * @since 2010-01-11
 *
 */
public class BinOutPrep
{
  
  
  /**Version, history and license.
   * <ul>
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
  public static final String version = "2018-09-08";

  
  
  /**If setOutBin is called, this writer is present, else it is null. */
  private OutputStream fileBin;
  
  
  
  
  /**Binary data for fileBin. If setOutBin is called, this array is present, else it is null. 
   * The head data contains all relocatable addresses. */
  private final byte[] binOutData, binOutHeadData, binOutClassArrayData;
  
  /**Access to binary data for fileBin. If setOutBin is called, this array is present, else it is null. 
   * This Data contains fields and classes. */
  private final RawDataAccess binOutClass;
  
  /**Access to binary head data for fileBin. If setOutBin is called, this array is present, else it is null. 
   * This data contains all relocatable addresses in binOut.*/
  private final ExtReflection_Insp_h.ExtReflection_Insp binOutHead;
  
  /**Access to binary class Array data for fileBin. If setOutBin is called, this array is present, else it is null. 
   * This data contains all references to the classes as relative pointers (offset). */
  private final ObjectArray_Jc binOutClassArray;
  
  private final Class_Jc binClass;
  
  private final ObjectArray_Jc binFieldArray;
  
  private final Field_Jc binField;
  
  private int nrofRelocEntries = 0;
  
  private int nrofClasses = 0;
  
  /**Counts the number of fields in 1 class. Set to 0 in {@link #addClass(String, String)}.
   * Used for the index in position.
   */
  private int nrofFieldsInClass;
  
  
  private static class TypeBinPosition
  { 
    
    int posClassInBuffer;
  
    public TypeBinPosition(int posClassInBuffer)
    { this.posClassInBuffer = posClassInBuffer;
    }

  
  }
  
  /**Position and type of a usage of type in a field. */
  private static class TypeNeedInBinOut
  {
    String sType;
    int posRefInFieldBuffer;
    
    public TypeNeedInBinOut(String sType, int posRefInFieldBuffer)
    { this.sType = sType;
      this.posRefInFieldBuffer = posRefInFieldBuffer;
    }
  }
  
  
  private final Map<String, TypeBinPosition> posClassesInBuffer = new TreeMap<String, TypeBinPosition>();
  
  private final Map<String, Integer> missingClasses = new TreeMap<String, Integer>();
  
  private final List<TypeNeedInBinOut> typeBinNeed = new LinkedList<TypeNeedInBinOut>();
  
  
  
  
  
  public BinOutPrep(String sFileBin, boolean fileBinBigEndian, boolean hexOutput, int sign) 
  throws FileNotFoundException
  {
    File fileBinFile = new File(sFileBin);
    if(hexOutput){
      fileBin = new StdHexFormatWriter(new File(sFileBin));
    } else {
      fileBin = new FileOutputStream(fileBinFile);
    }
    binOutData = new byte[2000000];
    binOutHeadData = new byte[400000];
    binOutClassArrayData = new byte[80000];
    
    binOutHead = new ExtReflection_Insp_h.ExtReflection_Insp(binOutHeadData);
    binOutHead.setBigEndian(fileBinBigEndian);
    binOutHead.set_sign(sign);
    
    binOutClass = new RawDataAccess();
    binOutClass.assignClear(binOutData);
    binOutClass.setBigEndian(fileBinBigEndian);
    
    binOutClassArray = new ObjectArray_Jc();
    binOutClassArray.assignClear(binOutClassArrayData);
    binOutClassArray.setBigEndian(fileBinBigEndian);
    binOutClassArray.set_sizeElement(4);  //pointer
    //instances which are used if need as child.
    binClass = new Class_Jc();
    binFieldArray = new ObjectArray_Jc();
    binField = new Field_Jc();
    
  }
  
  
  
  /**Sets a position in binFile as relocate position.
   * The address there is organized from 0 relative to the binfile data.
   * On loading the binFile the load address should be added to build a correct memory address.
   * @param posReloc Position in binFile.
   * @throws IllegalArgumentException
   */
  private void setRelocEntry(int posReloc) throws IllegalArgumentException
  { nrofRelocEntries +=1;
    binOutHead.addChildInteger(4, posReloc);  //address to relocate.
  } 
  
  
  
  /**Prepare a new ClassJc-entry in the bin Data: 
   * @throws IllegalArgumentException */
  public int addClass(String sCppClassName, String sCppClassNameShow) throws IllegalArgumentException
  { 
    binOutClass.addChild(binClass);  //binClass is used as reference to the binData.
    binClass.clearData();            //clear in binData. 
    int ixByteClass = binClass.getPositionInBuffer();
    nrofClasses +=1;
    binOutClassArray.addChildInteger(4, ixByteClass);  //Store the reference to the class in the classArray, yet relative to buffer.
    binClass.setName(sCppClassNameShow);
    binClass.set_posObjectBase(0);  //posObjectBase always 0 because nested CPU may be simple-C 
    binClass.set_nSize(0xFFFFF000 + nrofClasses); //sizeof(TYPE) is unknown here, instead: index of the class.
    posClassesInBuffer.put(sCppClassName, new TypeBinPosition(ixByteClass));  //to search and assign the relative pointer to the class 
    nrofFieldsInClass = 0;
    return nrofClasses;
  }

  
  
  
  /**Adds space for the fields to {@link #binOutClass} as child {@link #binFieldArray} and clears the child.
   * It is separated from {@link #addClass(String, String)} because not any class has fields.
   * Invoke only if fields are found, before the first field will be written.
   * @throws IllegalArgumentException
   */
  public void addFieldHead() throws IllegalArgumentException
  { binOutClass.addChild(binFieldArray);  //binClass is used as reference to the binData.
    binFieldArray.clearData();            //clear in binData. 
  }
  
  
  
  /**Adds a field to the {@link #binFieldArray} via {@link ByteDataAccessBase}.
   * The {@link #binFieldArray} is a {@link ObjectArray_Jc} instance for ByteDataAccess.
   * It presents the Fields in Reflection in binary presentation.
   * @param sAttributeNameShow
   * @param typeAddress either -1 for yet unknown or the enum ScalarTypes_ClassJc (see emC/Object_emC.h) 
   *                    or the known relative address of the type in binary file
   * @param sType  The identifier of the type. Without _t on forward declaration.
   * @param mModifier
   * @param nrofArrayElements
   * @throws IllegalArgumentException
   */
  public void addField(String sAttributeNameShow, int typeAddress, String sType, int mModifier, int nrofArrayElements) throws IllegalArgumentException
  {
    this.nrofFieldsInClass +=1;
    int ixField = this.nrofFieldsInClass;   //The index of the field from 1 for the first field.
    binFieldArray.addChildEmpty(binField);
    binField.setName(sAttributeNameShow);
    //binField.set_sizeElements(0xFFFF);
    binField.set_nrofArrayElements(nrofArrayElements);
    binField.set_position(0x8000 + ixField); //ccc-1);
    binField.set_offsetToObjectifcBase(0);
    if(nrofArrayElements >0)
      stop();
    if(typeAddress == -1){
      //The type address is not known yet, replace later:
      int posTypeInField = binField.getPositionInBuffer_type();
      typeBinNeed.add(new TypeNeedInBinOut(sType, posTypeInField));
      binField.set_type(0);
    } else {
      binField.set_type(typeAddress);
    }
    binField.set_bitModifiers(mModifier);
    //Note: the class should be added first. Otherwise as in the c-file for compiler.
    int posReloc = binField.setOffs_declaringClass(binClass.getPositionInBuffer());
    setRelocEntry(posReloc);
  }
  
  
  
  /**Sets the reference from the class to its fields.
   * @param nrofAttributes The number of the fields written.
   * @throws IllegalArgumentException 
   */
  void setAttributRef(int nrofAttributes) throws IllegalArgumentException
  { binFieldArray.set_length(nrofAttributes);
    int ixDataFields = binFieldArray.getPositionInBuffer();   //offset of field array in buffer 
    int posReloc = binClass.setOffs_attributes(ixDataFields); //set the relative address.
    setRelocEntry(posReloc);                                  //it should be relocated.
  }
  
  
  
  public void closeAddClass() {
    setAttributRef(nrofFieldsInClass);
  }
  
  
  
  
  
  public void postProcessBinOut() throws IOException, IllegalArgumentException
  {
    for(TypeNeedInBinOut need: typeBinNeed){
      int  posTypeInField = need.posRefInFieldBuffer;
      TypeBinPosition position = posClassesInBuffer.get(need.sType);
      if(position != null){
        int posClass = position.posClassInBuffer;
        int offset = posClass - posTypeInField; 
        binOutClass.setIntVal(posTypeInField, 4, offset);
        setRelocEntry(posTypeInField);                                  //it should be relocated.
      } else {
        /**Type is not known: */
        Integer nr = missingClasses.get(need.sType);
        if(nr == null) { nr = new Integer(0); missingClasses.put(need.sType, nr); }
        nr +=1;  //it does not increment, build a new nr Instance?
        binOutClass.setIntVal(posTypeInField, 4, Field_Jc.REFLECTION_void);
      }
    }
    for(Map.Entry<String, Integer> e : this.missingClasses.entrySet()) {
      System.err.println("Missing type " + e.getKey());
    }
    binOutClassArray.set_length(nrofClasses);
    int zHead = binOutHead.getLengthTotal();
    int zClassArray = binOutClassArray.getLengthTotal();
    int zData = binOutClass.getLengthTotal();
    binOutHead.set_nrofRelocEntries(nrofRelocEntries);
    binOutHead.set_arrayClasses(zHead);
    binOutHead.set_classDataBlock(zHead + zClassArray);
    fileBin.write(binOutHead.getData(), 0, zHead);
    
    fileBin.write(binOutClassArray.getData(), 0, zClassArray);
    
    fileBin.write(binOutClass.getData(), 0, zData);
    
    
  }
  
  
  
  public void close() throws IOException
  {
    fileBin.close();
  }
  
  void stop(){}
  
}
