package org.vishia.header2Reflection;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.Map;
import java.util.TreeMap;

import org.vishia.byteData.ByteDataAccessSimple;
import org.vishia.byteData.Class_Jc;
import org.vishia.byteData.Field_Jc;
import org.vishia.byteData.ObjectArray_Jc;
import org.vishia.byteData.Object_Jc;
import org.vishia.byteData.SuperclassIfc_idxMtblJc_ByteDataAccess;
import org.vishia.util.Debugutil;
import org.vishia.util.FileSystem;

public class BinOutShow
{

  /**The array with the binary data, read from file or immediately on creation.
   * The length of the array me be > zBinRefl.
   */
  final byte[] binRefl;
  
  /**Real used length of the binRefl. Note: On creation it is possible that binRefl is more large then need.*/
  final int zBinRefl;
  
  /**Access to binary data for fileBin. If setOutBin is called, this array is present, else it is null. 
   * This Data contains fields and classes. */
  private final ByteDataAccessSimple binOutRefl = new ByteDataAccessSimple(false);
  
  /**Access to binary head data for fileBin. If setOutBin is called, this array is present, else it is null. 
   * This data contains all relocatable addresses in binOut.*/
  private final ExtReflection_Insp_h.ExtReflection_Insp binOutHead = new ExtReflection_Insp_h.ExtReflection_Insp();
  
  /**Access to binary class Array data for fileBin. If setOutBin is called, this array is present, else it is null. 
   * This data contains all references to the classes as relative pointers (offset). */
  private final ObjectArray_Jc binOutClassArray = new ObjectArray_Jc();
  
  private final Class_Jc binClass = new Class_Jc();
  
  private final ObjectArray_Jc binSuperClassArray = new ObjectArray_Jc();
  
  private final SuperclassIfc_idxMtblJc_ByteDataAccess binSuperClass = new SuperclassIfc_idxMtblJc_ByteDataAccess();
  
  private final ObjectArray_Jc binFieldArray = new ObjectArray_Jc();
  
  private final Field_Jc binField = new Field_Jc();
  
  int posClassArray, posRefl;
  
  
  private Map<Integer, Boolean> usedReloc = new TreeMap<Integer, Boolean>();
  
  BinOutShow(byte[] binRefl, int zBinRefl){
    this.binRefl = binRefl;
    this.zBinRefl = zBinRefl;
  }
  
  public void show(File fileLst) {
    binOutHead.assign(this.binRefl, this.zBinRefl);
    this.posClassArray = binOutHead.get_arrayClasses();
    this.posRefl = binOutHead.get_classDataBlock();
    binOutClassArray.assign(binRefl, this.posRefl - this.posClassArray, this.posClassArray);
    binOutRefl.assign(binRefl, this.zBinRefl - this.posRefl, this.posRefl);
    
    listContent(fileLst);
  }
  
  
  
  private void listContent(File sFileList) {
    StringBuilder u = new StringBuilder(200);
    PrintStream fileList = null;
    try {
      fileList = new PrintStream(new FileOutputStream(sFileList));
      //fileList = new PrintStream(new ByteArrayOutputStream(10000));
    } catch (Exception e) {
      System.err.println("error cannot create " + sFileList);
    }
    try {
      if(fileList !=null) {
        int zHead = binOutHead.getLengthTotal();
        int zClassArray = binOutClassArray.getLengthTotal();
        int zData = binOutRefl.getLengthTotal();
        fileList.printf("Listing of structure of the ").printf(sFileList.getAbsolutePath()).printf("\n");
        fileList.printf("The bin file consists of 3 parts:\n");
        fileList.printf("@0x000000: Table of relocate positions, addresses which should be corrected. \n");
        fileList.printf("@0x%06x: ObjectArrayJc with references to all ClassJc in this file\n", posClassArray);
        fileList.printf("@0x%06x: ClassJc, FieldJc instances. \n", posRefl);
        fileList.printf("@0x%06x: -length. \n", this.zBinRefl);
        
        int nrRelocEntries = binOutHead.get_nrofRelocEntries();
        int posRelocEntries = binOutHead.getLengthHead();
        fileList.printf("Re-allocation @%04x: nrof=%d \n", posRelocEntries, nrRelocEntries);
        for(int ix = 0; ix < nrRelocEntries; ++ix) {
          int posReloc = binOutHead.get_relocateAddr(ix);
          usedReloc.put(posReloc, false);
          fileList.printf("%06x ", posReloc);
          if((ix & 0x7)==7) { fileList.printf("\n"); }
        }
        fileList.printf("\n\n==@0x%06x: ObjectArrayJc arrayClasses:\n", this.posClassArray);
        u.setLength(0);
        fileList.printf(binOutClassArray.showContent(u));
        int nrEntries = binOutClassArray.getLength_ArrayJc();
        
        for(int ix = 0; ix < nrEntries; ++ix) {
          int posClassPtr = binOutClassArray.getIntVal(4*ix + ObjectArray_Jc.sizeof_ObjectArray_Jc, 4);
          fileList.printf("\n ClassJc [0x%06x]  @0x%06x", posClassPtr, posClassPtr + this.posRefl);
        }
        listRefl(fileList, zHead + zClassArray);
      
        fileList.close(); 
        fileList = null;
      }
    } catch(Exception exc) { 
      System.err.println(exc.getMessage());
      if(fileList !=null) {
        fileList.close();
      }
    }
  }

  
  
  
  private void listRefl(PrintStream fileList, int pos) {
    fileList.printf("\n\n==@0x%06x: Reflection data:\n", this.posRefl);
    Object_Jc obj = new Object_Jc();
    try {
      while(binOutRefl.sufficingBytesForNextChild(obj.getLengthHead())) {
        this.binOutRefl.addChild(obj);
        int ident = obj.getIdent();
        switch(ident) {
        case Class_Jc.INIZ_ID_ClassJc: showClass(fileList, obj); break;
        case Field_Jc.INIZ_ID_FieldJc: showFields(fileList, obj); break;
        case SuperclassIfc_idxMtblJc_ByteDataAccess.INIZ_ID: showSuper(fileList, obj); break;
        default: showUnknown(fileList, obj);
        }
      }
    } catch(IllegalArgumentException exc) { System.err.println(exc.getMessage());}
    Debugutil.stop();
  }
  
  
  
  private void showClass(PrintStream fileList, Object_Jc obj) {
    binClass.assignDataUpcast(obj);
    int posInBuffer = binClass.getPositionInBuffer();
    fileList.printf("\n\n==@0x%06x %s size=0x%4x (ClassJc 0x%06x): \n", posInBuffer, binClass.getName(), obj.getSizeObject(), posInBuffer - posRefl);
    fileList.printf(" -name="); fileList.printf(binClass.getName());
    int offsFields = binClass.getFieldsAddr();
    int offsSuper = binClass.getSuperAddr();
    
    fileList.printf("\n -fields");
    checkPrintReference(fileList, posInBuffer + Class_Jc.kPos_attributes, offsFields);
    fileList.printf("\n -super");
    checkPrintReference(fileList, posInBuffer + Class_Jc.kPos_superClass, offsSuper);
  }
  
  
  
  /**
   * @param fileList
   * @param pos The position of the reference in the bin data.
   * @param offs
   */
  void checkPrintReference(PrintStream fileList, int pos, int offs) {
    int addr = offs == 0 ? 0 : offs + pos - this.posRefl;  //addr is related to the posRefl
    int posReloc = pos - this.posRefl;
    Boolean isReloc = usedReloc.get(posReloc);
    fileList.printf(" @0x%06x =0x%06x =^ @0x%06x", posReloc, offs & 0xffffff, addr);
    if(isReloc !=null) {
      fileList.printf(" ok");
      usedReloc.put(posReloc, true);
    }
  }
  
  
  
  
  
  private void showFields(PrintStream fileList, Object_Jc obj) {
    binFieldArray.assignDataUpcast(obj);
    int size = binFieldArray.getSizeObject();
    int nrFields = binFieldArray.getLength_ArrayJc();
    int posInBuffer = obj.getPositionInBuffer();
    fileList.printf("\n\n  @0x%06x + 0x%4x: FieldArrayJc[%d] (0x%06x):", posInBuffer, size, nrFields, posInBuffer - posRefl);
    if(size != nrFields * Field_Jc.sizeOf_Field_Jc + binFieldArray.getLengthHead()) {
      fileList.printf(" - faulty size vs. nrofFields");
    }
    while(binFieldArray.sufficingBytesForNextChild(Field_Jc.sizeOf_Field_Jc)) {
      binFieldArray.addChild(binField);
      int posInBufferField = binField.getPositionInBuffer();
      int typeval = binField.getType();
      fileList.printf("\n  @0x%06x: %s : at %04x type=0x%08x", posInBufferField, binField.getName(), binField.getPosValue(), typeval);
      int modifier = binField.getModifiers();
      if((modifier & Field_Jc.EModifier.mPrimitiv_Modifier.e) ==0) { //a non-simple type:
        checkPrintReference(fileList, binField.getPositionInBuffer_type(), typeval);
      }
    }
  }
  
  
  private void showSuper(PrintStream fileList, Object_Jc obj) {
    binSuperClassArray.assignDataUpcast(obj);
    int size = binSuperClassArray.getSizeObject();
    int nrFields = binSuperClassArray.getLength_ArrayJc();
    int posInBuffer = obj.getPositionInBuffer();
    fileList.printf("\n\n==@0x%06x + 0x%4x: SuperArrayJc[%d] (0x%06x):\n", posInBuffer, size, nrFields, posInBuffer);
    if(size != nrFields * SuperclassIfc_idxMtblJc_ByteDataAccess.size + binSuperClassArray.getLengthHead()) {
      fileList.printf(" - faulty size vs. nrofFields\n");
    }
    while(binSuperClassArray.sufficingBytesForNextChild(SuperclassIfc_idxMtblJc_ByteDataAccess.size)) {
      binSuperClassArray.addChild(binSuperClass); 
      binSuperClass.addInnerChildren();
      int offsType = binSuperClass.field_access.getType();
      
      fileList.printf("\n + %s : at %d", binSuperClass.field_access.getName(), binSuperClass.field_access.getPosValue());
      checkPrintReference(fileList, binSuperClass.field_access.getPositionInBuffer_type(), offsType);
    }
  }
  
  
  private void showUnknown(PrintStream fileList, Object_Jc bin) {
    fileList.printf("\n\n==@0x%06x: unknown ObjectJc:\n", this.posRefl + bin.getPositionInBuffer());
  }
  
  

  
  public static void main(String[] args) {
    File fileBin = new File(args[0]);
    File fileList = new File(args[0] + ".lst");
    byte[] binRefl = FileSystem.readBinFile(fileBin);
    BinOutShow main = new BinOutShow(binRefl, binRefl.length);
    main.show(fileList);
  }
  
  
}
