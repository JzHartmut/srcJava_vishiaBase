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

import org.vishia.byteData.ByteDataAccess;
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
/*package private*/ class BinOutPrep
{
  
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
  
  int ixField;
  
  private int nrofRelocEntries = 0;
  
  private int nrofClasses = 0;
  
  
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
  
  private final List<TypeNeedInBinOut> typeBinNeed = new LinkedList<TypeNeedInBinOut>();
  
  
  
  
  
  BinOutPrep(String sFileBin, boolean fileBinBigEndian, boolean hexOutput, int sign) 
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
  
  
  void setRelocEntry(int posReloc) throws IllegalArgumentException
  { nrofRelocEntries +=1;
    binOutHead.addChildInteger(4, posReloc);  //address to relocate.
  } 
  
  /**Prepare a new ClassJc-entry in the bin Data: 
   * @throws IllegalArgumentException */
  int addClass(String sCppClassName, String sCppClassNameShow) throws IllegalArgumentException
  { 
    binOutClass.addChild(binClass);
    binClass.clearData();
    int ixByteClass = binClass.getPositionInBuffer();
    nrofClasses +=1;
    binOutClassArray.addChildInteger(4, ixByteClass);  
    binClass.setName(sCppClassNameShow);
    binClass.set_posObjectBase(0);  //posObjectBase always 0 because nested CPU may be simple-C 
    binClass.set_nSize(0xFFFFF000 + nrofClasses); //sizeof(TYPE) is unknown here, instead: index of the class.
    posClassesInBuffer.put(sCppClassName, new TypeBinPosition(ixByteClass));
    ixField = 1;
    return nrofClasses;
  }

  
  
  
  void addFieldHead() throws IllegalArgumentException
  { binOutClass.addChild(binFieldArray);
    binFieldArray.clearData();
  }
  
  
  
  void addField(String sAttributeNameShow, int typeAddress, String sType, int mModifier, int nrofArrayElements) throws IllegalArgumentException
  {
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
    ixField +=1;
  }
  
  
  
  /**Sets the reference from the class to its fields.
   * @throws IllegalArgumentException 
   * 
   */
  void setAttributRef(int nrofAttributes) throws IllegalArgumentException
  { binFieldArray.set_length(nrofAttributes);
    int ixDataFields = binFieldArray.getPositionInBuffer();   //offset of field array in buffer 
    int posReloc = binClass.setOffs_attributes(ixDataFields); //set the relative address.
    setRelocEntry(posReloc);                                  //it should be relocated.
  }
  
  
  void postProcessBinOut() throws IOException, IllegalArgumentException
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
        binOutClass.setIntVal(posTypeInField, 4, Field_Jc.REFLECTION_void);
      }
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
  
  
  void close() throws IOException
  {
    fileBin.close();
  }
  
  void stop(){}
  
}
