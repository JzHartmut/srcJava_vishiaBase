package org.vishia.zcmd;

//==JZcmd==
//JZcmd main(){
//JZcmd   Obj args = java new org.vishia.zcmd.Bin2Data$Args();
//JZcmd   //args.syntaxDescription = "bin2data::= recordStart : <#?posFirstRecord> ; recordLen : <#?recordLength> ; { <element> }. element::= <$?name> : <#?position> <$?type> ;. ";
//JZcmd   args.sFileDescription = $1;
//JZcmd   args.sFileSyntaxDescription = $2;
//JZcmd   //args.sDescription = "recordStart:1000; recordLen:200; var1:20F; var2:24I;";
//JZcmd   Obj bin2Data = new org.vishia.zcmd.Bin2Data(args);
//JZcmd   bin2Data.parseDescription();
//JZcmd   bin2Data.loadBinary($3);
//JZcmd   bin2Data.setRecordStartLength(0xa4, 0x254); 
//JZcmd   Num f = bin2Data.getFloat(225, "IActrl.wIAp");
//JZcmd }
//==endJZcmd==


import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.vishia.byteData.ByteDataAccessBase;
import org.vishia.mainCmd.MainCmd;
import org.vishia.util.FileSystem;
import org.vishia.util.IndexMultiTable;
import org.vishia.util.StringFunctions;
import org.vishia.zbnf.ZbnfJavaOutput;




/**This class reads a regular binary file which contains data records with equal length into a container of signals.
 * The structure of the data should be described with an additional textual file. The usage of the ZBNF parser supports several syntax possibilities.
 * Optional the content can be written out in several formats.
 * @author Hartmut Schorrig
 *
 */
public class Bin2Data
{
  /**Version, history and license.
   * <ul>
   * <li>2016-01-15 Hartmut created.
   * </ul>
   * <br><br>
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
   */
  public final static String version = "2016-01-15"; 

  
  public static class Args
  {
    public String sFileBin;
    
    /**With that argument the syntax of the description can be read from a file using {@link Bin2Data#readDescrSyntax()}. 
     */
    public String sFileSyntaxDescription;
    
    /**A string given syntax for the description file. 
     * This argument may be filled from {@link Bin2Data#readDescrSyntax()} with given {@link #sFileSyntaxDescr} with that file content.
     * It may be given direct from invocation. The syntaxDescr should be regard the {@link ZbnfResultDescr} for its semantic.
     */
    public String syntaxDescription;
    
    /**With that argument the description can be read from a file using {@link Bin2Data#readDescrSyntax()}. 
     */
    public String sFileDescription;
    
    /**A string given description of the content of binary data. 
     * It should be match to the #syntaxDescr. 
     */
    public String sDescription;
    
  }
  
  
  private static class CmdLine extends MainCmd
  {

    @Override protected boolean checkArguments()
    {
      return false;
    }
    
  }
  
  
  public static class DescrDataElement
  {
    public String nameElement;
    
    public int posInRecord;
    
    public float scale;
    
    char type;
    
  }
 
  
  
  public static class DescrData
  {
    public int posFirstRecord;
    
    public int recordLength;
    
    List<DescrDataElement> listElements = new ArrayList<DescrDataElement>();
    
    Map<String, DescrDataElement> idxElements = new IndexMultiTable<String, DescrDataElement>(IndexMultiTable.providerString);
    
    
  }
  
  
  @SuppressWarnings("synthetic-access") 
  public class ZbnfResultDescr extends DescrData
  {
   
    public ZbnfResultDescrElement new_element(){ return zbnfElement.clean(); }
    
    public void add_element(ZbnfResultDescrElement val) 
    { DescrDataElement item = val.createDescrDataElement();
      idxElements.put(val.nameElement, item);
      listElements.add(item);
    }
    
    
  }
  
  public class ZbnfResultDescrElement extends DescrDataElement
  {
    public boolean addPosition;
    
    int posInRecordBase;
    
    public void set_type(String val) { type = val.charAt(0); }
    
    public void set_position(int val) { 
      if(addPosition) {
        posInRecord = 2*val + posInRecordBase; 
      } else {
        posInRecordBase = posInRecord = 2*val;
      }
      addPosition = false;
    }
    
    public void set_name(String val) { nameElement = val; }
    
    public void set_scale(double val) { scale = (float)val; }
    
    DescrDataElement createDescrDataElement(){
      DescrDataElement ret = new DescrDataElement();
      ret.nameElement = this.nameElement;
      ret.posInRecord = this.posInRecord;
      ret.scale = this.scale;
      ret.type = this.type;
      return ret;
    }
    
    ZbnfResultDescrElement clean()
    {
      this.nameElement = null;
      this.posInRecord = 0;
      this.scale = 0;
      this.type = 0;
      this.addPosition = false;
      return this;
    }
    
  }
  
  
  final Args args;
  
  
  DescrData descrData;
  
  ZbnfResultDescrElement zbnfElement = new ZbnfResultDescrElement();
    

  byte[] data;
  
  
  ByteDataAccessBase accData = new ByteDataAccessBase(0);

  
  public Bin2Data(Args args) {
    this.args = args;
  }
  
  
  public Bin2Data() {
    this.args = new Args();
  }
  
  
  public void readDescrSyntax() {
    
  }
  
  
  public boolean parseDescription()
  {
    if(args.sDescription == null && args.sFileSyntaxDescription == null) {
      args.sDescription = "bin2data::= recordStart : <#?posFirstRecord> ; recordLen : <#?recordLength> ; { <element> }. element::= <$?name> : <#?position> <$?type> ;. ";
    }
    if(descrData == null || descrData.listElements !=null) {
      //create new if non exists or if old filled exists. 
      //don't create new if setRecordStartLenght() was called only.
      descrData = new ZbnfResultDescr();
    }
    ZbnfJavaOutput parser = new ZbnfJavaOutput();
    String error;
    if(args.sDescription !=null) {
      //String given
      error= parser.parseFileAndFillJavaObject(ZbnfResultDescr.class, descrData, args.sDescription, args.sFileDescription, args.syntaxDescription, null);
    } else if(args.syntaxDescription !=null ){
      error= parser.parseFileAndFillJavaObject(ZbnfResultDescr.class, descrData, new File(args.sFileDescription), args.syntaxDescription);
    } else {
      error= parser.parseFileAndFillJavaObject(ZbnfResultDescr.class, descrData, new File(args.sFileDescription), new File(args.sFileSyntaxDescription));
    }
    if(error!=null){
      System.err.println(error);
      return false;
    } else {
      return true;
    }
  }

  
  
  /**Load the given file.
   * @param file The file can have up to 2 wildcards ('*') in the name part. The first (usual only one) instance is used.
   * A file may start with a defined name but the rest of name may be unknown, for example designated with a time stamp.
   * @param bigEndian
   * @return true if successfully, false if data == null
   */
  public boolean loadBinary(File file, boolean bigEndian)
  { data = null; //default, on error
    File fBin = FileSystem.getFirstFileWildcard(file);
    data = FileSystem.readBinFile(fBin);
    accData.assign(data);  //clean if data == null
    accData.setBigEndian(bigEndian);
    return data !=null;
  }
  
  
  /**Returns a value from absolute position, e.g. in the head data of the file.
   * This is especially to read number of records etc. in the head data.
   * @param pos absolute position
   * @param nrofBytes 1,2,4 or -1, -2, -4 to read byte, short, int. Positive values: read unsigned. 
   * @return
   */
  public int getIntegerHead(int pos, int nrofBytes) {
    ByteDataAccessBase accElement = new ByteDataAccessBase(0);
    accData.addChildAt(pos, accElement, nrofBytes);
    return accElement.getChildInt(nrofBytes);
    
  }
  
  /**Returns a value from absolute position, e.g. in the head data of the file.
   * This is especially to read number of records etc. in the head data.
   * @param pos absolute position
   * @param nrofBytes 1,2,4 or -1, -2, -4 to read byte, short, int. Positive values: read unsigned. 
   * @return
   */
  public String getStringHead(int pos, int nrofBytes) {
    ByteDataAccessBase accElement = new ByteDataAccessBase(0);
    accData.addChildAt(pos, accElement, nrofBytes);
    return accElement.getChildString(nrofBytes);
    
  }
  
  /**Sets the record structure information
   * @param start
   * @param length
   * @return true if the length of the loaded file matches to the start and length, the last record is complete.
   *   false if a file is not loaded or the length does not match.
   */
  public boolean setRecordStartLength(int start, int length) 
  {
    if(descrData == null) {
      //create new if non exists 
      descrData = new ZbnfResultDescr();
    }
    descrData.posFirstRecord = start;
    descrData.recordLength = length;
    if(data !=null && (data.length - start) % length == 0) return true;
    else return false;
  }
  
  
  /**Returns the number of records of the file. */
  public int nrofRecords()
  { if(descrData !=null && descrData.recordLength >0 && data !=null) {
      float n= (data.length - descrData.posFirstRecord)/descrData.recordLength;
      return (int)n;
    } else {
      return 0;
    }
  }
  
  
  
  
  private float getFloat(int record, DescrDataElement descr, ByteDataAccessBase accElement)
  {
    float f = 0;
    int pos = descrData.posFirstRecord + (record * descrData.recordLength) + descr.posInRecord;
    if(pos > data.length -4) throw new IllegalArgumentException("Element out of range: " + descr.nameElement + ", record = " + record);
    switch(descr.type){
      case 'S': {
        accData.addChildAt(pos, accElement, 2);
        int val = accElement.getChildInt(2);
        if(descr.scale < 0) {
          f = (val / 32768.0f) * -descr.scale;
        } else {
          if(val <0) { val +=65536; }
          f = (val / 65536.f) * descr.scale;
        }
      } break;
      case 'F': {
        accData.addChildAt(pos, accElement, 4);
        f = accElement.getChildFloat();
      } break;
      case 'I': {
      } break;
    }
    return f;
  }
  
  
  public float getFloat(int record, String name)
  {
    DescrDataElement descr = descrData.idxElements.get(name);
    if(descr == null) throw new IllegalArgumentException("Element does not exist: " + name);
    ByteDataAccessBase accElement = new ByteDataAccessBase(0);
    return getFloat(record, descr, accElement);
  }
  

  
  /**Searches the minima and maxima and build the average from a value in range.
   * @param begin start record
   * @param end exclusive end record
   * @param name variable
   * @return array min mid max
   */
  public float[] getMinMidMaxFloat(int begin, int end, String name) 
  { float[] minmidmax = new float[3];
    minmidmax[0] = 1e38f;
    minmidmax[2] = -1e38f;
    DescrDataElement descr = descrData.idxElements.get(name);
    if(descr == null) throw new IllegalArgumentException("Element does not exist: " + name);
    ByteDataAccessBase accElement = new ByteDataAccessBase(0);
    for(int ix=begin; ix < end; ++ix) {
      float f = getFloat(ix, descr, accElement);      
      if(f < minmidmax[0]){ minmidmax[0] = f; } 
      if(f > minmidmax[2]){ minmidmax[2] = f; }
      minmidmax[1] += f;
    }
    minmidmax[1] /= (begin - end);
    return minmidmax;
  }
  
  
  
  
}
