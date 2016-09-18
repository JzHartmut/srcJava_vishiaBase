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
 * 2010-11-30 Hartmut exprSizeType = "..." etc in the config file.
 * 2010-01-15 Hartmut An annotation @container=ObjectArrayJc[Type*] is detect now in the comment to create a container-reflection.
 * 2010-01-09 Hartmut new big feature: A binfile and a offset file for a remote CPU is possible to create.
 * 2010-01-02 Hartmut relative path instead absolute path of #include "src.h" per Header-file.
 * 2007..2009: Hartmut: some changes
 * 2007 Hartmut created
 */
package org.vishia.header2Reflection;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.vishia.byteData.ByteDataAccess;
import org.vishia.byteData.Class_Jc;
import org.vishia.byteData.Field_Jc;
import org.vishia.mainCmd.MainCmdLogging_ifc;
import org.vishia.mainCmd.Report;
import org.vishia.util.Debugutil;
import org.vishia.util.FileSystem;


import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Iterator;

import org.vishia.util.StringPart;
import org.vishia.util.StringPart;
import org.vishia.util.StringPartFromFileLines;
import org.vishia.util.StringPartScan;
import org.vishia.zbnf.ZbnfParseResultItem;
import org.vishia.zbnf.ZbnfParser;


/* Changes
 */

/**Converts Headerfiles to reflection. */
public class Header2Reflection
{
  /**Version, history and license.
   * <ul>
   * <li>2016-09-14 Hartmut bugfix: if an bitfield is detected and the element before is a pointer, a '*' should be written in sizeof(type),
   *   especially sizeof(void) does force compiler errors. sizeof(StructType) will be erroneous. 
   * <li>2016-05-12 Hartmut new: processing of 16-bit-Bitfield, till now only 15 bit are possible. Use bit pattern 0 for 16 bit.
   *   All usages of Reflection are enhanced too (CRuntimeJavalike). 
   * <li>2015-06-04 Hartmut new: Argument -always generates always, independing of dst timestamp. Especially for test.
   * <li>2014-11-17 Hartmut chg: Recognized "struct tagname {...};" without typedef.  
   * <li>2014-05-31 Hartmut chg: ':' as separator between base and local path only after position 2, elsewhere ambiguous with "D:"
   * <li>2013-10-19 Hartmut chg: now invoke-able from JZcmd, public methods etc. Only formalism
   * <li>2013-04-12 Hartmut new: #define in Headerfile or #define in cfg-file can be used to set {@link #idxDefines},
   *   conditional #ifdef ... in headerfile only taken if #define is known.
   * <li>2013-04-02 Hartnut chg: if a pointer is given in for exprSizeType($$$) in a offset file, "void*" is used.
   *   The pointer type may be a unknown type (struct type_t*) and its sizeof(type*) is unknown therefore in compile time.
   *   use sizeof(void*), it is the same.
   * <li>2012-10-21 Hartmut created. Some algorithm are copied from {@link org.vishia.zbatch.ZbatchExecuter} in this class.
   *   That algorithm are able to use independent in some applications.
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
   * <li> But the LPGL ist not appropriate for a whole software product,
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
  static final public int version = 20130410;

  /**Aggregation to the Console implementation class.*/
  MainCmdLogging_ifc console;

  static class FileIn
  {
    /**Input files. */
    final List<File> listFileIn = new LinkedList<File>();

    /**Position in absolute path of start of relative path, may be 0. */
    int posPath;
  }


  /**Input files. */
  private final List<FileIn> listFileIn = new LinkedList<FileIn>();

  /**The parser. */
  private ZbnfParser parser;

  private String sFileZbnf;

  /**File-path of the file with the type info for Reflection generating.*/
  private String sFileReflectionTypes;


  /**File-path of the file with the type info to write out from Reflection generating useable for next pass.*/
  private String sFileReflectionTypesOut;

  /**File-path of the file with the infos for blocked types and files. */
  private File fileReflectionBlockedTypes;

  /**True if a new file for types should be created, false if append. */
  private boolean bNewFileReflectionTypesOut = false;
  
  /**Parse only struct, no class. */
  boolean c_only = false;

  /**Path and Name of the only one output.c and output.h-file. */
  private String sFileAllC;

  /**Path and Name of the binary output.h-file. */
  private String sFileBin;

  /**If true, generate only if at least one of the header files are newer then the generate. */
  private boolean bCheckNewness = true;
  
  boolean fileBinBigEndian;
  
  boolean fileBinHex;
  
  /**Path and Name of the offset.c file. It is null if an extra offset structure isn't used. */
  private String sFileOffset;

  /**Cmdline-argument, set on -o option. Directory where the out-files will be written. */
  private String sOutDir = null;
  
  /*Absolute path to the sOutDir. */
  private String sOutDirAbs;

  /*Absolute path to the sOutOffsetDir. */
  private String sOffsetDirAbs;

  private final String[] sExprTokens = new String[] {"@@@", "$$$"};
  
  private final String[] sPlaceholderType  = new String[]{"%%%"};
  
  /**The pattern for the expression for field offset, which is generated to calculate the offset value in the C-code.
   * The expression can be provided by the config-file.
   */
  private String sExprOffsField = "((int32)(&((@@@*)(0x1000))->$$$) ";;

  /**The pattern for the expression for base address, which is generated to calculate the offset value in the C-code.
   * The expression can be provided by the config-file.
   */
  private String sExprOffsBase = "(int32)(@@@*)0x1000)";

  /**The pattern for the expression for Object-Base-address in C, which is generated to calculate the offset value in the C-code.
   * The expression can be provided by the config-file.
   */
  private String sExprOffsObj = "(int32)&((@@@*)0x1000)->base.object)";

  /**The pattern for the expression for Object-Base-address for C++, which is generated to calculate the offset value in the C-code.
   * The expression can be provided by the config-file.
   */
  private String sExprOffsCppObj = "(int32)(static_cast<ObjectJc*>((@@@*)0x1000)))";
  
  /**The pattern for the expression for Object-Base-address for C++, which is generated to calculate the offset value in the C-code.
   * The expression can be provided by the config-file.
   */
  private String sExprOffsObjJcpp = "(int32)(static_cast<ObjectJc*>(static_cast<ObjectJcpp*>((@@@*)0x1000))))";

  /**The pattern for the expression for the size of a type.
   * The expression can be provided by the config-file.
   */
  private String sExprSizeType = "sizeof(%%%)";
  
  private final String sLinefeed = "\n";
  
  /**Text for leader and trailer in the offset- and C-Files. It will be filled depending on cfg-file. */
  StringBuilder leaderTextC, trailerTextC, leaderTextH, trailerTextH, leaderTextOffs, trailerTextOffs;
  
  private final static String scalarTypes = "0-void:1-bool:1-boolean:4-float:4-float32:8-double:1-char:4-int:2-short:1-byte:4-int32:4-uint32:2-int16:2-uint16:1-int8:2-uint8:";

  
  
  final static String sSyntaxReflectionTypes
    = "$setLinemode. \n"
    + "ReflectionTypes::=\n"
    + "{ \\n\n"     //simple empty line possible, may contain comments. 
    + "| \\<leaderTextOffs  [ = <textFile?leaderTextOffs>  \\> | \\> \\n<lines?leaderTextOffs>]\n"
    + "| \\<trailerTextOffs [ = <textFile?trailerTextOffs> \\> | \\> \\n<lines?trailerTextOffs>]\n"
    + "| \\<leaderTextC     [ = <textFile?leaderTextC>     \\> | \\> \\n<lines?leaderTextC>]\n"
    + "| \\<trailerTextC    [ = <textFile?trailerTextC>    \\> | \\> \\n<lines?trailerTextC>]\n"
    + "| \\<leaderTextH     [ = <textFile?leaderTextH>     \\> | \\> \\n<lines?leaderTextH>]\n"
    + "| \\<trailerTextH    [ = <textFile?trailerTextH>    \\> | \\> \\n<lines?trailerTextH>]\n"
    + "| exprOffsField = <\"\"?exprOffsField> \\n\n" 
    + "| exprOffsBase = <\"\"?exprOffsBase> \\n\n" 
    + "| exprOffsObj = <\"\"?exprOffsObj> \\n\n" 
    + "| exprOffsObjJcpp = <\"\"?exprOffsObjJcpp> \\n\n" 
    + "| exprOffsCppObj = <\"\"?exprOffsCppObj> \\n\n"
    + "| exprSizeType = <\"\"?exprSizeType> \\n\n"
    + "| <blockedFile> \\n \n" 
    + "|<?c_only> c_only \\n\n"
    + "|<?define> #define <$?name> <*\\r\\n?value> \n"  //simple #define name zeichenkette
    + "| <reflectionType> \\n \n" 
    + "| \\n\n" 
    + "} \\e.\n"
    + "blockedFile::= % <* ,\\r\\n?path>.\n"
    + "reflectionType::= [<?sign>#|-] <* ,\\r\\n?type> \n" 
    + "| [<?sign>=|@] <* =?type> = [ <$?deftype>| 0x<#x?defvalue>| <#?defvalue>] \n" 
    + "| [<?sign>!|!] <* =?type> = [ <$?deftype>].\n"  //definition of baseclass
    + "lines::={[? \\<END\\>]<*\\n?line>\\n} \\<END\\> \\n.\n"
    + "textFile::= <\"\"?fileName>.\n"
    
    ;
//    + "reflectionType::= <!.?sign> <* ,\\r\\n?path> [, {<?type> <!.?sign> <$?name> ? , } ].";


  /**Informations about type read from Configfile. */ 
  private static class TypeEntry
  { final String sType;
    final String sPath;
    final int value;
    char sign;

    TypeEntry(String sType, String sPath, char sign, int value)
    { this.sType = sType; this.sPath = sPath; this.sign = sign; this.value = value; }

  }

  /**
   * 
   */
  private final TreeMap<String, TypeEntry> identifierRelacements = new TreeMap<String, TypeEntry>();

  private final TreeMap<String, TypeEntry> types = new TreeMap<String, TypeEntry>();

  private final TreeMap<String, List<TypeEntry>> typesByFilename = new TreeMap<String, List<TypeEntry>>();


  /**created header and c file. */
  private BufferedWriter fileH;
  /**created header and c file. */
  private BufferedWriter fileC;

  /**If setOutC is called, this writer is present, else it is null. */
  private BufferedWriter fileAllC;

  /**If setOutC is called, this writer is present, else it is null. */
  private BufferedWriter fileAllH;

  /**If setOutOffset is called, this writer is present, else it is null. */
  private BufferedWriter fileOffs;

  private StringBuilder bufferOffsArray;
  
  /**The time-stamp of calling conversion. It will be written in File. */
  private Date dateNow;
  
  private int secondsAfter1970Now;
  
  private final SimpleDateFormat dataFormat = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss");
  
  BinOutPrep binOutPrep;
  
  final Map<String, String> idxDefines = new TreeMap<String, String>();
  
  
  /**This class holds some informations about a type of a element (attrubute). 
   * Instances of this class are used only in Stack for temporary values.
   */
  static class FieldTypeInfos
  {
    /**Information about scalar basic types. */
    int bytesScalarType;
    /**Decision whether the type is knwon. */ 
    boolean useReflection;
    
    /**Type ident for binary file. */
    int typeIdent;
      
  }
  
  
  
  
  
  public void setSyntax(String sFileZbnf)
  { this.sFileZbnf = sFileZbnf;
  }

  public void setCheckNewness(boolean value) {
    bCheckNewness = value;
  }
  
  public void setReflectionTypes(String sFile)
  { this.sFileReflectionTypes = sFile;
  }

  public void setReflectionTypesOut(String sFile, boolean bNew)
  { this.sFileReflectionTypesOut = sFile;
    bNewFileReflectionTypesOut = bNew;
  }

  public void setReflectionBlockedTypes(String sFile)
  { this.fileReflectionBlockedTypes = new File(sFile);
  }

  public void setConfig(String sFile)
  { this.fileReflectionBlockedTypes = new File(sFile);
  }

  public void setCfg(String sFile)
  { this.fileReflectionBlockedTypes = new File(sFile);
  }

  public boolean setOutC(String sFile)
  { boolean bOk = true;
    this.sFileAllC = sFile.replace('\\', '/');
    try { FileSystem.mkDirPath(sFileAllC);}      //create if not exist
    catch (FileNotFoundException e) {
      console.writeError("Error path: " + sFile);
      bOk = false;
    }
    return bOk;
  }

  public boolean setOutOffset(String sFile)
  { boolean bOk = true;
    this.sFileOffset = sFile.replace('\\', '/');
    try { FileSystem.mkDirPath(sFileOffset);}     //create if not exist
    catch (FileNotFoundException e) {
      console.writeError("Error path: " + sFile);
      bOk = false;
    }
    return bOk;
  }

  public boolean setOutBin(String sFile, boolean bigEndian, boolean hexBin)
  { boolean bOk = true;
    this.sFileBin = sFile.replace('\\', '/');
    this.fileBinBigEndian = bigEndian;
    this.fileBinHex = hexBin;
    try { FileSystem.mkDirPath(sFileBin);}     //create if not exist
    catch (FileNotFoundException e) {
      console.writeError("Error path: " + sFile);
      bOk = false;
    }
    return bOk;
  }

  public boolean setOutDir(String sFile)
  { boolean bOk = true;
    this.sOutDir = sFile.replace('\\', '/');
    try { FileSystem.mkDirPath(sOutDir);} 
    catch (FileNotFoundException e) {
      bOk = false;
    }
    return bOk;
  }

  
  
  
  
  /**Adds a path/file or path/mask to the {@link #listFileIn}.  
   * @param sMask
   * @return
   */
  public boolean addInputFilemask(String sMask)
  { boolean bOk = true;
    int posSep = sMask.indexOf(':',2);
    FileIn fileIn = new FileIn();
    File dir = null;
    if(posSep > 2)
    { String sDir = sMask.substring(0, posSep);
      dir = new File(sDir);
      if(dir.exists())
      { try {
          sMask = sMask.substring(posSep+1);
          String sDirAbs = dir.getCanonicalPath();
          fileIn.posPath = sDirAbs.length();
        } catch (IOException exc)
        { bOk = false;
          console.writeError("input dir exception:" + dir.getAbsolutePath());
        }
      }
      else
      { bOk = false;
        File filePWD = new File(".");  //should existing always!
        String sPWD = filePWD.getAbsolutePath();
        console.writeError("input dir not found:" + sDir +", current Dir =" + sPWD);
        
      }
    }
    else
    { fileIn.posPath = -1;
    }
    if(bOk)
    { bOk= FileSystem.addFileToList(dir, sMask, fileIn.listFileIn); 
      if(!bOk)
      { bOk = false;
        if(dir!=null)
        { console.writeError("input files not found:" + sMask + " in dir:" + dir.getAbsolutePath());
        }
        else
        { console.writeError("input files not found:" + sMask);
        }
      }
      listFileIn.add(fileIn);
    }
    return bOk;
  }



  /**The Constructor is called after parsing CmdLine arguments. */
  public Header2Reflection(MainCmdLogging_ifc report)
  { this.console = report;
    parser = new ZbnfParser(console, 10);
    parser.setReportIdents(MainCmdLogging_ifc.error, MainCmdLogging_ifc.fineInfo, MainCmdLogging_ifc.debug, MainCmdLogging_ifc.fineDebug);
  }



  public void execute() throws Exception
  { System.out.printf("Header2Reflection made by Hartmut Schorrig, version %d\n", version);
    if(readBlockedFilesAndTypes())
    //if(readReflectionTypes())
    { if(init()) //here all action are done, testversion.
      {
        boolean dontGenerate = false;
        File fileAllCFile = sFileAllC !=null ? new File(sFileAllC) : null;
        long timestampLast = 0;
        for(FileIn fileIn: listFileIn){
          for(File file: fileIn.listFileIn){
            if(!file.exists()){ console.reportln(MainCmdLogging_ifc.error, "File not found: " + file.getAbsolutePath()); 
            } else {
              long timestamp = file.lastModified();
              if(timestamp > timestampLast){
                timestampLast = timestamp;
              }
            }
        } }
        secondsAfter1970Now = (int)(timestampLast / 1000);
        dateNow = new Date(timestampLast);
        //secondsAfter1970Now = (int)(dateNow.getTime()/1000);
        if(bCheckNewness && fileAllC !=null && fileAllCFile.exists() && fileAllCFile.lastModified() > timestampLast){
          dontGenerate = true;  //not necessary
        }
        if(!dontGenerate){
          if(fileAllCFile != null)
          { //sOutDirAbs = fileAllCFile.getParentFile().getCanonicalPath().replace('\\','/');  //directory of output file
            sOutDirAbs = fileAllCFile.getCanonicalPath().replace('\\','/');  //directory of output file
            fileAllC = new BufferedWriter(new FileWriter(fileAllCFile));
            int posDot = sFileAllC.lastIndexOf('.');
            int posPath = sFileAllC.lastIndexOf('/');
            String sPathFileAllH = sFileAllC.substring(0, posDot) + ".h";
            String sFileAllH = sFileAllC.substring(posPath+1, posDot) + ".h";
            String sFileName = sFileAllC.substring(posPath+1, posDot);
            fileAllH = new BufferedWriter(new FileWriter(new File(sPathFileAllH)));
            if(leaderTextC !=null){
              fileAllC.append(leaderTextC);
            }
            fileAllC.write("\n/*This file is generated from Header2Reflection. */");
            fileAllC.write("\n#define protected public  //only active inside this compilation unit");
            fileAllC.write("\n#define private public    //  to enable the access to all elements.");
            fileAllC.write("\n#include <Jc/ReflectionJc.h>");
            fileAllC.write("\n#include <stddef.h>");
            //fileAllC.write("\n#include \"" +sFileAllH+ "\"");
            
            //fileAllC.write("\n/**The seconds after 1970 where the this file and the apprpriated reflection in binfile where created.");
            //fileAllC.write("\n * It is " + dataFormat.format(dateNow) + "*/");
            //fileAllC.write("\nconst int reflectionGenerationTime = " + secondsAfter1970Now + ";\n");
            
            fileAllH.write("\n#ifndef __" + sFileName + "_h__\n");
            fileAllH.write("\n#define __" + sFileName + "_h__\n");
            if(leaderTextH !=null){
              fileAllH.append(leaderTextH);
            }
            fileAllH.write("\n#include \"Jc/ReflectionJc.h\"");
          }
          if(sFileOffset != null)
          { File filesFileOffset = new File(sFileOffset);
            //String sFileOffsetName = filesFileOffset.getName();
            sOffsetDirAbs = filesFileOffset.getCanonicalPath().replace('\\','/');  //directory of output file
            fileOffs = new BufferedWriter(new FileWriter(filesFileOffset));
            if(leaderTextOffs !=null){
              fileOffs.append(leaderTextOffs);
            }
            //Note: append and write are the same, but append can be used with CharSequence.
            fileOffs.append("\n/*This file is generated from Header2Reflection. */");
            fileOffs.write("\n#include <os_types_def.h>");
            fileOffs.write("\n#include <stddef.h>");
            fileOffs.write("\n#define protected public  //only active inside this compilation unit");
            fileOffs.write("\n#define private public    //  to enable the access to all elements.\n\n");
            fileOffs.write("\n/**The seconds after 1970 where the this file and the apprpriated reflection in binfile where created.");
            fileOffs.write("\n * It is " + dataFormat.format(dateNow) + "*/");
            fileOffs.write("\nconst int reflectionOffsGenerationTime = " + secondsAfter1970Now + ";\n");
            
            bufferOffsArray = new StringBuilder();
          }
          if(sFileBin != null)
          { 
            binOutPrep = new BinOutPrep(sFileBin, fileBinBigEndian, fileBinHex, secondsAfter1970Now);
          }
          for(FileIn fileIn: listFileIn)
          { for(File file: fileIn.listFileIn)
            { String fileName = file.getCanonicalPath().replace('\\','/');
              fileName = fileName.substring(fileIn.posPath+1);  //regards a ':' in the input mask.
              translate(file, fileName);
            }
          }
          if(fileAllC != null){
            if(trailerTextC !=null){
              fileAllC.append(trailerTextC);
            }
            fileAllC.close();
            if(trailerTextH !=null){
              fileAllH.append(trailerTextH);
            }
            fileAllH.write("\n#endif // ___h__\n");
            fileAllH.close();
          }
          if(fileOffs != null)
          { 
            fileOffs.append("\n\n/**Array of all offsets: ------------------------------------------------------------------------*/");
            fileOffs.append("\nint32* reflectionOffsetArrays[] = \n{ null");
            fileOffs.append(bufferOffsArray);
            fileOffs.append("\n};\n");
            if(trailerTextOffs !=null){
              fileOffs.append(trailerTextOffs);
            }
            fileOffs.close();
          }
          if(binOutPrep != null)
          { binOutPrep.postProcessBinOut();
            binOutPrep.close();
          }
          console.writeInfoln("...finished.");
        } else {
          console.writeInfoln("...no newer files, nothing to do.");
        }
      }
      //if(sFileReflectionTypesOut!= null)  writeReflectionTypes();
    }
    else
    { console.writeError("read Reflection types failed.");
    }

  }


  
  
  

  /**Reads all types from the file given with -r: argument in cmdline or setted with setReflectionTypes(filepath);
   *
   * @return true if success.
   * @throws ParseException
   * @throws IllegalCharsetNameException
   * @throws UnsupportedCharsetException
   * @throws FileNotFoundException
   * @throws IOException
   */
  private boolean readBlockedFilesAndTypes()
  throws ParseException, IllegalCharsetNameException, UnsupportedCharsetException, FileNotFoundException, IOException
  { ZbnfParser parser = new ZbnfParser(console, 10);
    console.writeInfoln("read ctrl-file: " + fileReflectionBlockedTypes.getAbsolutePath());
    parser.setSyntax(sSyntaxReflectionTypes);
    StringPartScan spInput = null;
    spInput = new StringPartFromFileLines(fileReflectionBlockedTypes,-1, null, null);
    boolean bOk = parser.parse(spInput);
    if(!bOk)
    { console.writeError(parser.getSyntaxErrorReport());
      stop();
    }
    else
    { parser.reportStore(console, Report.fineInfo);
      List<ZbnfParseResultItem> items = parser.getFirstParseResult().listChildren();
      //String sType;
      for(ZbnfParseResultItem item: items)
      { String semantic = item.getSemantic();
        if(semantic.equals("c_only")){
          c_only = true;  
        } else if(semantic.equals("exprOffsField")) {
          sExprOffsField = item.getParsedString();
        } else if(semantic.equals("exprOffsBase")) {
          sExprOffsBase = item.getParsedString();
        } else if(semantic.equals("exprOffsObj")) {
          sExprOffsObj = item.getParsedString();
        } else if(semantic.equals("exprOffsObjJcpp")) {
          sExprOffsObjJcpp = item.getParsedString();
        } else if(semantic.equals("exprOffsCppObj")) {
          sExprOffsCppObj = item.getParsedString();
        } else if(semantic.equals("exprSizeType")) {
          sExprSizeType = item.getParsedString();
        } else if(semantic.equals("define")) {
          String name = item.getChildString("name");
          String value = item.getChildString("value");
          idxDefines.put(name, value);
        } else if(semantic.equals("reflectionType")) {
          char cSign = item.getChild("sign").getParsedText().charAt(0);
          String sType = item.getChild("type").getParsedString();
          String sDefType; int value;
          ZbnfParseResultItem zbnfDefType = item.getChild("deftype"); 
          ZbnfParseResultItem zbnfDefValue = item.getChild("defvalue"); 
          if(zbnfDefValue != null)
          { sDefType = null;
            value = (int)zbnfDefValue.getParsedInteger();
          }
          else if( "=@!".indexOf(cSign)>=0)
          { sDefType = zbnfDefType.getParsedString();
            value =-1;
          }
          else { sDefType = null; value = -1;}
          TypeEntry identReplacement = new TypeEntry(sType, sDefType, cSign, value);
          //TODO: Test and exception if a type is twice.
          identifierRelacements.put(sType, identReplacement);
        } else if(semantic.equals("blockedFile")){
          stop();
        } else if(semantic.equals("leaderTextC")){
          leaderTextC = getLeaderTrailerText(item, leaderTextC);
        } else if(semantic.equals("trailerTextC")){
          trailerTextC = getLeaderTrailerText(item, trailerTextC);
        } else if(semantic.equals("leaderTextH")){
          leaderTextH = getLeaderTrailerText(item, leaderTextH);
        } else if(semantic.equals("trailerTextH")){
          trailerTextH = getLeaderTrailerText(item, trailerTextH);
        } else if(semantic.equals("leaderTextOffs")){
          leaderTextOffs = getLeaderTrailerText(item, leaderTextOffs);
        } else if(semantic.equals("trailerTextOffs")){
          trailerTextOffs = getLeaderTrailerText(item, trailerTextOffs);
        } else {
          throw new RuntimeException("unknown syntax definition: " + semantic);
        }
      }
    }
    return bOk;
  }



  private StringBuilder getLeaderTrailerText(ZbnfParseResultItem zbnfResult, StringBuilder lines){
    //StringBuilder lines = new StringBuilder(500);
    if(lines == null){ lines = new StringBuilder(500); }
    List<ZbnfParseResultItem> listLine = zbnfResult.listChildren("line");
    if(listLine !=null){
      for(ZbnfParseResultItem zbnfChild: listLine){
        lines.append(zbnfChild.getParsedString()).append(sLinefeed);
      }
    }
    String sFile = zbnfResult.getChildString("fileName");
    if(sFile !=null){
      String sFileName = fileReflectionBlockedTypes.getParent() + "/" + sFile;
      File file = new File(sFileName);
      if(!file.exists()){ 
        lines.append("/*ERROR Header2Reflection - file not found: ")
             .append(sFileName).append("*/").append(sLinefeed);
      } else {
        String sContent = FileSystem.readFile(file);
        lines.append(sContent);
      }
    }
    return lines;
  }
  
  

  
  

  /**Reads all types from the file given with -r: argument in cmdline or setted with setReflectionTypes(filepath);
   *
   * @return true if success.
   * @throws ParseException
   * @throws IllegalCharsetNameException
   * @throws UnsupportedCharsetException
   * @throws FileNotFoundException
   * @throws IOException
   */
  @SuppressWarnings("unused")
  private boolean XXXreadReflectionTypes()
  throws ParseException, IllegalCharsetNameException, UnsupportedCharsetException, FileNotFoundException, IOException
  { ZbnfParser parser = new ZbnfParser(console, 10);
    console.writeInfoln("read reflection types: " + sFileReflectionTypes);
    parser.setSyntax(sSyntaxReflectionTypes);
    StringPartScan spInput = null;
    spInput = new StringPartFromFileLines(new File(sFileReflectionTypes),-1, null, null);
    boolean bOk = parser.parse(spInput);
    if(!bOk)
    { console.writeError(parser.getSyntaxErrorReport());
      stop();
    }
    else
    { List<ZbnfParseResultItem> items = parser.getFirstParseResult().listChildren("reflectionType");
      String sType;
      BufferedWriter wrTypes = null;
      if(bNewFileReflectionTypesOut)
      { wrTypes = new BufferedWriter(new FileWriter(new File(sFileReflectionTypesOut)));
        wrTypes.write("//********Generated Reflectiontypes*****************************************\n");
      }
      for(ZbnfParseResultItem item: items)
      { char cSign = item.getChild("sign").getParsedString().charAt(0);
        String sPath = item.getChild("path").getParsedString();
        int posSlash = sPath.lastIndexOf("/");  //-1 is admissible
        String sFileName = sPath.substring(posSlash+1);
        LinkedList<TypeEntry> typesPerFile = new LinkedList<TypeEntry>();
        typesByFilename.put(sFileName, typesPerFile);
        //if("+!$%".indexOf(cSign) >0)
        if("#.".indexOf(cSign) < 0)
        { //The filename is the same as a type.
          int posDot = sPath.lastIndexOf(".");
          if(posDot < posSlash){ posDot = sPath.length(); }
          sType = sPath.substring(posSlash+1, posDot);
          TypeEntry type = new TypeEntry(sPath, sType, cSign,0);
          types.put(sType, type);
          typesPerFile.add(type);
        }
        if(cSign == '#' || cSign == '-' )
        { TypeEntry notUsedFile = new TypeEntry(sPath, "###", '#',0);
          typesPerFile.add(notUsedFile);
        }

        List<ZbnfParseResultItem> typeItems = item.listChildren("type");
        if(typeItems!=null)for(ZbnfParseResultItem typeItem: typeItems)
        { sType = typeItem.getChild("name").getParsedString();
          cSign = typeItem.getChild("sign").getParsedString().charAt(0);
          TypeEntry type = new TypeEntry(sPath , sType, cSign,0);
          types.put(sType, type);
          typesPerFile.add(type);
        }
        if((cSign == '#' || cSign == '-') && wrTypes != null)
        {
          wrTypes.write("" + cSign + " " + sPath + "\n");  //note: a type is not relevant.
        }
      }
      if(wrTypes != null)
      { wrTypes.flush();
        wrTypes.close();
      }
    }
    return bOk;
  }





  private void writeReflectionTypes()
  throws IOException
  { BufferedWriter wrTypes = new BufferedWriter(new FileWriter(new File(sFileReflectionTypesOut), true));
    wrTypes.append("//***********************************************************************************\n");
    wrTypes.append("//from " + sFileAllC + "\n");


    Set<Map.Entry<String, List<TypeEntry>>> fileSet = typesByFilename.entrySet();
    for(Map.Entry<String, List<TypeEntry>> entry: fileSet)
    { List<TypeEntry> types = entry.getValue();
      if(types.size()>0 && types.get(0).sign != '#' && types.get(0).sign != '-')
      { String line = ". " + entry.getKey();
        for(TypeEntry typeEntry: types)
        {
          if((typeEntry.sign !='#' && typeEntry.sign !='-'))
          { line += ", " + typeEntry.sign + " " + typeEntry.sType;
          }
        }
        wrTypes.append(line + "\n");
      }
    }

    /*
    Set<Map.Entry<String, TypeEntry>> typeSet = types.entrySet();
    for(Map.Entry<String, TypeEntry> entry: typeSet)
    { TypeEntry typeEntry = entry.getValue();
      if(typeEntry.sign != '#')
      { String line = ". " + typeEntry.sPath + ", " + typeEntry.sign + " " + typeEntry.sType;
        wrTypes.append(line + "\n");
      }
    }
    */
    wrTypes.close();

  }






  private boolean init()
  throws IllegalCharsetNameException, UnsupportedCharsetException, FileNotFoundException, IOException
  { boolean bOk= true;
    if(sFileZbnf == null) {
      /**No -z: option, than standard file used: */
      String sZbnfHome = System.getenv("ZBNFJAX_HOME");
      if(sZbnfHome == null) {
        console.writeError("No -z:SYNTAX option, and the environment variable ZBNFJAX_HOME is unknown, abort.");
        bOk = false;
      } else {
        sFileZbnf = sZbnfHome + "/zbnf/Cheader.zbnf";
      }
    }
    if(bOk){
      File fileSyntax= new File(sFileZbnf);
      String sSyntax = FileSystem.readFile(fileSyntax);
      if(sSyntax == null){
        console.writeError("fault SYNTAX file, abort:" + sFileZbnf);
        bOk = false;
      } else {
        parser = new ZbnfParser(console, 10);
        try{ parser.setSyntax(sSyntax); }
        catch(ParseException exc)
        { console.writeError("Headersyntax fault in " + sFileZbnf, exc);
          parser.reportSyntax(console, Report.fineInfo);
          bOk = false;
        }
        parser.setReportIdents(Report.error, Report.info, Report.debug, Report.fineDebug);
      }  
    }
    return bOk;
  }




  private void translate(File headerFile, String sFilePath)
  throws IllegalCharsetNameException, UnsupportedCharsetException, FileNotFoundException, IOException, ParseException
  {
    String sFileName = headerFile.getName();
    console.writeInfoln("generate from " + sFileName +"...");
    /*
    String sTypeFromFileName;
    { int posDot = sFileName.indexOf('.');
      if(posDot < 0){ posDot = sFileName.length(); }
      sTypeFromFileName = sFileName.substring(0, posDot);
    }
    List<TypeEntry> typesInFile = typesByFilename.get(sFileNameRelative);
    if(typesInFile == null)
    { //The file is unknown yet, it should be parsed:
      typesInFile = new LinkedList<TypeEntry>();
      typesByFilename.put(sFileNameRelative, typesInFile);
    }

    if(typesInFile.size() == 0 || typesInFile.get(0).sign != '#')  //if there are no types stored, a # is written at first, do not translate it.
    {
    */
    TypeEntry typeEntry = identifierRelacements.get(sFilePath);
    if(typeEntry == null || (typeEntry.sign !='#' && typeEntry.sign !='-'))////
    { StringPartScan spInput = null;
      spInput = new StringPartFromFileLines(headerFile,-1, null, null);
      boolean bOk = parser.parse(spInput);
      if(!bOk)
      { String sError = parser.getFoundedInputOnError();
        //console.writeError(sError);
        sError = parser.getExpectedSyntaxOnError();
        //console.writeError(sError);
        sError = parser.getSyntaxErrorReport();
        console.writeError(sError);
        stop();

      }
      else
      { parser.reportStore(console, Report.fineInfo);
        if(sOutDir != null)
        { File fileOutDir = new File(sOutDir);
          //sOutDirAbs = fileOutDir.getCanonicalPath().replace('\\','/');  //directory of output file
          sOutDirAbs = fileOutDir.getCanonicalPath().replace('\\','/') + "/";  //directory of output file
          fileC = new BufferedWriter(new FileWriter(new File(sOutDir + "/Reflection_" + sFileName + ".c")));
          fileH = new BufferedWriter(new FileWriter(new File(sOutDir + "/Reflection_" + sFileName + ".h")));
        }
        else
        { fileC = fileAllC;
          fileH = null;
        }
        ZbnfParseResultItem resultItem = parser.getFirstParseResult();
        if(resultItem.getSemantic().equals("Cheader"))
        { Iterator<ZbnfParseResultItem> iter= resultItem.iteratorChildren();
        //resultItem = resultItem.nextSkipIntoComponent(null);
          if(fileH != null)
          { fileH.write("\n#ifndef __" + sFileName + "_h__\n");
            fileH.write("\n#define __" + sFileName + "_h__\n");
          }
          
          /**find out the path to the headerfile to include. */
          final String sInclude, sIncludeOffs;
          if(FileSystem.isAbsolutePathOrDrive(sFilePath)) {
            sInclude = FileSystem.relativatePath(sFilePath, sOutDirAbs);
            if(sOffsetDirAbs != null){
              sIncludeOffs = FileSystem.relativatePath(sFilePath, sOffsetDirAbs);
            } else {
              sIncludeOffs = null; //not necessary
            }
              
          } else {
            sIncludeOffs = sInclude = sFilePath;  //Situation, if the input file is given with ':' in its -i:argument.
          }
          
          fileC.write("\n#include \"" + sInclude + "\"\n");
          if(fileH != null)
          { fileH.flush();
          }
          fileC.flush();

          if(fileOffs != null){
            fileOffs.write("\n\n\n#include \"" + sIncludeOffs + "\"\n");
          }    
          
          //while(resultItem != null)
          while(iter.hasNext())
          { resultItem = iter.next();
            String sSemantic = resultItem.getSemantic();
            if(sSemantic.equals("outside") || sSemantic.equals("CLASS_C"))
            { 
              String sCLASS_C_name = null;
              if(sSemantic.equals("CLASS_C"))
              { sCLASS_C_name = resultItem.getChild("@name").getParsedString();
              }
              searchAndTranslateClassDef(resultItem, sCLASS_C_name, sFilePath);
            }
            //resultItem = resultItem.next(null);
          }
          if(fileH != null)
          { fileH.write("#endif //__" + sFileName + "_h__\n");
            fileH.flush();
          }
          fileC.flush();
        }
      }
    }
    else
    { console.writeInfo(" ignored.");
    }

  }

  
  
  
  
  
  private void searchAndTranslateClassDef(final ZbnfParseResultItem zbnfParent, String sCLASS_C_name, String sFileNameRelative) 
  throws IOException
  { final String sCLASS_C_name_s = sCLASS_C_name + "_s";  //stucture names are often ending with _s  
    final String sCLASS_C_name_i = sCLASS_C_name + "_i";  //stucture names are often ending with _s  
    Iterator<ZbnfParseResultItem> iter1= zbnfParent.iteratorChildren();
    while(iter1.hasNext())
    { ZbnfParseResultItem resultItem = iter1.next();
      String sSemantic = resultItem.getSemantic();
      if(  sSemantic.equals("structDefinition")
        || sSemantic.equals("unionDefinition")
        || ( !c_only && sSemantic.equals("classDef"))
        )
      { //String sClassName =
        final String sClassName;  //maybe "struct tagname"
        final String sStructName;  //maybe "struct tagname"
        { ZbnfParseResultItem itemName = resultItem.getChild("@name");
          if(itemName == null) {
            itemName = resultItem.getChild("@tagname");  //if un-named struct definition.  ////
            if(itemName !=null){
              sClassName = itemName.getParsedString();
              sStructName = "struct " + sClassName;
            } else {
              sStructName = sClassName = null; //can't determine
            }
          } else {
            sStructName = sClassName = itemName.getParsedString();  //child "@name" found.
          }
        }
        if(sClassName != null)
        { if(sSemantic.equals("unionDefinition"))
            stop();
          if(sClassName.equals("ListItr_LinkedListJcpp"))
            stop();
          if(sClassName.equals("ObjectJcpp"))
            stop();
          if(sClassName.equals("ObjectJc"))
            stop();
          
          if(  sCLASS_C_name == null             //no wrapper CLASS_C exists
            || sCLASS_C_name.equals(sClassName)  //or it is the struct associated with CLASS_C
            || sCLASS_C_name_s.equals(sClassName)
            || sCLASS_C_name_i.equals(sClassName)
            )
          { boolean cppClass = sSemantic.equals("classDef");
            ConverterClass converterClass = new ConverterClass(null);
            converterClass.convertClass(sClassName, sStructName, null, null, resultItem, sFileNameRelative, cppClass); //, typesInFile);  //this is a class or struct.
          }
        }
        else
          stop();
      }
      else if(sSemantic.equals("conditionBlock"))
      { //inside a condition block the same content may be found.S
        ZbnfParseResultItem zbnfCondition;
        String sCondition;
        boolean bCondition = true;;
        zbnfCondition = resultItem.getChild("conditionDef");
        if(zbnfCondition !=null){
          sCondition = zbnfCondition.getParsedString();
          bCondition = idxDefines.get(sCondition) !=null;
        }
        if(bCondition){
          searchAndTranslateClassDef(resultItem, sCLASS_C_name, sFileNameRelative);
        }
      }
      else if(sSemantic.equals("defineDefinition"))
      { //inside a condition block the same content may be found.S
        ZbnfParseResultItem zbnfName = resultItem.getChild("@name");
        if(zbnfName !=null){
          String sName = zbnfName.getParsedString();
          idxDefines.put(sName, sName);
        }
      }
    }
  }
  
  
  
  
  


  private class ConverterClass
  {
    
    boolean cppClass;
    
    /**Inheritance situation. */
    boolean bObjectJcpp = false ;
    
    /**Inheritance situation. */
    boolean bClassBasedOnObjectJc = false ;
    
    /**Inheritance situation. */
    boolean bStructHasFirstObjectJc = false ;
    
    /**Inheritance situation. */
    boolean bObjectifcBaseJcpp = false ;

    String sReflectionClassName;
    String sCppClassName;
    /**Used for offsetof(sTypeName, */ 
    String sTypeName;   
    
    /**Current Bit position in Bitfield. */
    int bitfieldPos;
    
    /**The byte-number of more bitfields. 
     * It is >0 if more as the number of bits of types are used as bit fields. */
    int bitfieldByte;
    
    /**It stores the last offset always. If a bitfield is detect and this element isn't null, 
     * the offset will built with the last element + its size.
     * If this element is null, the bitfield is the first element. 
     */
    String lastOffsetBeforeBitfield = null;
    
    /**Size of the last element. Used for a following bitfield. */
    String lastType;
    
    final TreeMap<String, String> innerTypes;

    /**This mask is defined originally in ReflectionJc.h, TODO import org.vishia.byteDataAccess.ReflectionJc
     */
    static final int mObjectJc_Modifier_reflectJc = 0x04000000;

    /**This mask is defined originally in ReflectionJc.h, TODO import org.vishia.byteDataAccess.ReflectionJc
     */
    static final int mObjectifcBaseJcpp_Modifier_reflectJc = 0x08000000;

    int nrofAttributes;
    
    boolean bFirst;
    
    StringBuffer sbCfile = new StringBuffer(50000);
    StringBuffer sbForward = new StringBuffer(5000);
    String sSeparator = "\n    ";


    ConverterClass(TreeMap<String, String> innerTypesP)
    { innerTypes =new TreeMap<String, String>();
      if(innerTypesP != null)
      { innerTypes.putAll(innerTypesP);
      }

    }


    private void convertClass
    ( String sClassName
    , String sStructName
    , String sParentCppClassName
    , String sParentReflectionClassName
    , ZbnfParseResultItem classItem
    , String sFilePath
    , boolean cppClass
    //, List<TypeEntry> typesInFile
    )
    throws IOException
    { //String sClassName = classItem.getChild("@name").getParsedString();
      this.cppClass = cppClass;
      sReflectionClassName = sParentReflectionClassName != null ? sParentReflectionClassName + "_" + sClassName : sClassName;
      sCppClassName = sParentCppClassName != null ? sParentCppClassName + "::" + sClassName : sClassName;   //used as type identifier
      sTypeName = sParentCppClassName != null ? sParentCppClassName + "::" + sClassName : sStructName;
      
      //if(sClassName.startsWith("EntryValue"))
      if(sClassName.equals("ListItr_LinkedListJcpp"))
        stop();
      if(sClassName.equals("ObjectJcpp"))
        stop();
      if(sClassName.equals("ObjectJc"))
        stop();

      /*
      TypeEntry type = types.get(sReflectionClassName);
      if(type == null)
      { //not known file:
        type = new TypeEntry(sFilePath,  sReflectionClassName, '!');
        types.put( sReflectionClassName, type);
        typesInFile.add(type);
      }
      */

      String bitModifiers = "0 ";
      int mModifiers = 0;
      
      console.writeInfo(sStructName + "...");

      if(fileAllH != null)
      { fileAllH.write("\nextern_C const ClassJc reflection_" + sReflectionClassName + ";");
        fileAllH.write("\n#define REFLECTION_" + sReflectionClassName + " &reflection_" + sReflectionClassName);
      }
      List<ZbnfParseResultItem> visibilityBlocks = classItem.listChildren("classVisibilityBlock");

      if(visibilityBlocks!=null)for(ZbnfParseResultItem visibilityBlockItem : visibilityBlocks)
      { convertInnerClassAndStruct(sCppClassName, sReflectionClassName, visibilityBlockItem, innerTypes, sFilePath, cppClass);
      }
      convertInnerClassAndStruct(sCppClassName, sReflectionClassName, classItem, innerTypes, sFilePath, cppClass);

      if(cppClass){
        fileC.append("\n#if defined(__CPLUSGEN) && defined(__cplusplus)\n");
      }
      
      
      //TODO: write this block in an extra method String testDerivationFromObject(ZbnfParseResultItem zbnfClass, int[] modifier);
      List<ZbnfParseResultItem> superclasses = classItem.listChildren("superclass");
      //determine the heredity from ObjectJc etc. note: only 1 level of heredity is supported here,
      //because the other super-classes are unknown yet.
      
      { TypeEntry typeEntry = identifierRelacements.get(sClassName);
        if(typeEntry != null) //found
        { if(typeEntry.sign == '!')  //it is a superclass definition
          { String superClass = typeEntry.sPath;
            testBasedOnObject(superClass);
          
          }
        }
      }  
      
      if(superclasses!=null)for(ZbnfParseResultItem superclassItem : superclasses)
      { String sNameSuperclass = superclassItem.getChild("name").getParsedString();
        testBasedOnObject(sNameSuperclass);
      }
      if(sReflectionClassName.equals("ObjectJc"))
      { bClassBasedOnObjectJc = false; //NOTE: ObjectJc has set bit mObjectJc_Modifier_reflectJc, but it isn't base on it.
      }
      else if(sReflectionClassName.equals("bObjectifcBaseJcpp"))
      { bObjectifcBaseJcpp = false;   //it is ObjectifcBaseJcpp itself.
      }
      
      final String sCppClassNameShow;
      final int zCppClassName = sCppClassName.length();
      if(zCppClassName <= Class_Jc.kLengthName-1){ sCppClassNameShow = sCppClassName; }
      else { 
        int nRest = Class_Jc.kLengthName -13 -2;  //regard "_" and terminating \0
        sCppClassNameShow = sCppClassName.substring(0, 13) + "_" + sCppClassName.substring(zCppClassName -nRest, zCppClassName); 
      }
      final int ixClassBin;
      if(binOutPrep != null){
        ixClassBin = binOutPrep.addClass(sCppClassName, sCppClassNameShow); //must add before fields are added.
      } else { 
        ixClassBin = 0;
      }
      if(fileOffs != null){
        fileOffs.append("\n\nint32 reflectionOffset_" + sReflectionClassName + "[] =\n{ " + ixClassBin + "   //index of offset table");
        bufferOffsArray.append("\n, &reflectionOffset_" + sReflectionClassName + "[0]");
      }
      
      nrofAttributes = 0;
      bFirst = true;
      convertElementsInClass(classItem, null);
      
      /*
      List<ZbnfParseResultItem> attributes = classItem.listChildren("attribute");  //outside visibility block
      List<ZbnfParseResultItem> listInnerStructAttributes = classItem.listChildren("implicitStructAttribute");  //outside visibility block
      List<ZbnfParseResultItem> listAllAttributes = new LinkedList<ZbnfParseResultItem>();
      
      if(attributes != null){ listAllAttributes.addAll(attributes); }
      if(listInnerStructAttributes != null)
      { listAllAttributes.addAll(listInnerStructAttributes); }
      for(ZbnfParseResultItem attributeItem : listAllAttributes)
      { if(convertAttribute(bFirst, sSeparator, sCppClassName, sReflectionClassName, attributeItem, sbCfile, sbForward, cppClass))
        { nrofAttributes +=1;
          sSeparator = "\n  , ";  //next entries.
          bFirst = false;
        }
      }

      if(visibilityBlocks!=null)for(ZbnfParseResultItem visibilityBlockItem : visibilityBlocks)
      {
        attributes = visibilityBlockItem.listChildren("attribute");
        if(attributes!=null)for(ZbnfParseResultItem attributeItem : attributes)
        { if(convertAttribute(bFirst, sSeparator, sCppClassName, sReflectionClassName, attributeItem, sbCfile, sbForward, cppClass))
          { nrofAttributes +=1;
            sSeparator = "\n  , ";  //next entries.
            bFirst = false;
          }
        }
      }
      */
      fileC.append("\n\nextern_C const ClassJc reflection_" + sReflectionClassName + ";  //the just defined reflection_");
      if(nrofAttributes >0)
      { fileC.append(sbForward); sbForward.setLength(0);
        fileC.append("\nconst struct Reflection_Fields_" + sReflectionClassName + "_t");
        fileC.append("\n{ ObjectArrayJc head;");
        fileC.append("\n  FieldJc data[" + nrofAttributes + "];");
        fileC.append("\n} reflection_Fields_" + sReflectionClassName + " =");
        fileC.append("\n{ CONST_ObjectArrayJc(FieldJc, " + nrofAttributes+ ", OBJTYPE_FieldJc, null, &reflection_Fields_" + sReflectionClassName + ")");
        fileC.append("\n, {");
        fileC.append(sbCfile); sbCfile.setLength(0);
        fileC.append("\n} };\n\n");
      }
      if(fileOffs != null){
        fileOffs.append("\n};\n");
      }
      


      sbCfile.append(  "\nconst ClassJc reflection_" + sReflectionClassName + " =");
      sbCfile.append(  "\n{ CONST_ObjectJc(OBJTYPE_ClassJc + sizeof(ClassJc), &reflection_" + sReflectionClassName + ", &reflection_ClassJc)");
      sbCfile.append(  "\n, \"" + sCppClassNameShow + "\"");
      if(sReflectionClassName.equals("ObjectJc"))
      { bitModifiers += " |mObjectJc_Modifier_reflectJc";
        mModifiers |= mObjectJc_Modifier_reflectJc;
      }   
      if(bObjectJcpp) //only for C++-classes
      { assert(cppClass);
        sbCfile.append("\n, (int)(static_cast<ObjectJc*>(static_cast<ObjectJcpp*>((" + sCppClassName + "*)0x1000))) - 0x1000     //posObjectJc");
        bitModifiers += " |mObjectJc_Modifier_reflectJc";
        mModifiers |= mObjectJc_Modifier_reflectJc;
        //NOTE: on evaluating the generated Reflections in C, the ObjectJc is getted with add offset and C-cast to ObjectJc.
      }
      else if(bStructHasFirstObjectJc)
      { sbCfile.append("\n, 0");
        bitModifiers += " |mObjectJc_Modifier_reflectJc";
        mModifiers |= mObjectJc_Modifier_reflectJc;
        //NOTE: on evaluating the generated Reflections in C, the ObjectJc is getted with add offset and C-cast to ObjectJc.
      }
      else if(bClassBasedOnObjectJc)
      { if(c_only || !cppClass){
          sbCfile.append("\n, (int)&((" + sCppClassName + "*)0x1000)->base.object) - 0x1000     //posObjectJc");
        } else {
          sbCfile.append("\n, (int)(static_cast<ObjectJc*>((" + sCppClassName + "*)0x1000)) - 0x1000     //posObjectJc");
        }
        bitModifiers += " |mObjectJc_Modifier_reflectJc";
        mModifiers |= mObjectJc_Modifier_reflectJc;
        //NOTE: on evaluating the generated Reflections in C, the ObjectJc is getted with add offset and C-cast to ObjectJc.
      }
      else if(bObjectifcBaseJcpp)  //only for C++-classes
      { assert(cppClass);
        sbCfile.append("\n, (int)(static_cast<ObjectifcBaseJcpp*>((" + sCppClassName + "*)0x1000)) - 0x1000     //posObjectJc");
        bitModifiers += " |mObjectifcBaseJcpp_Modifier_reflectJc";
        mModifiers |= mObjectifcBaseJcpp_Modifier_reflectJc;
        //NOTE: on evaluating the generated Reflections in C, the ObjectJc is getted with toObjectJc.
      }
      else
      { sbCfile.append("\n, 0");
        //bitModifiers |= 0;  //set no additional bit.
      }
      sbCfile.append(  "\n, sizeof(" + sCppClassName + ")");
      if(nrofAttributes >0)
      { sbCfile.append("\n, (FieldJcArray const*)&reflection_Fields_" + sReflectionClassName + "  //attributes and associations");
        if(binOutPrep != null){
          binOutPrep.setAttributRef(nrofAttributes);
        }
      }
      else
      { sbCfile.append("\n, null  //attributes");
      }
      sbCfile.append(  "\n, null  //method");
      sbCfile.append(  "\n, null  //superclass");
      sbCfile.append(  "\n, null  //interfaces");
      sbCfile.append(  "\n, " + bitModifiers);
      //sbCfile.append(  "\n, 0x" + Integer.toHexString(bitModifiers) + "  //modifiers");
      sbCfile.append(  "\n};\n\n\n");

      fileC.append(sbCfile.toString());
      if(cppClass){
        fileC.append("\n#endif //C++-class\n");
      }
      
    }


    
    /**Convert all element in a class or struct or inner part (visibility-block).
     */
    private void convertElementsInClass(ZbnfParseResultItem classItem, String sVariantName) 
    throws IOException
    {
      List<ZbnfParseResultItem> elements = classItem.listChildren();
      for(ZbnfParseResultItem zbnfElement : elements){
        convertElement(zbnfElement, sVariantName);
      }
    }  


    
    /**Convert 1 element in a class or struct or inner part (visibility-block).
     * @param zbnfElement The element from which the reflection should generated.
     * @param sNameStruct null or a name of the superior structure, 
     *        which is used in form sNameStruct.nameElement to build the offset.
     * @throws IOException
     * @throws AccessException
     */
    private void convertElement(ZbnfParseResultItem zbnfElement, String sNameVariant) 
    throws IOException
    {
    
      String semantic = zbnfElement.getSemantic();
      if(semantic.equals("implicitStructAttribute")){
        String sName = zbnfElement.getChildString("@name");
        convertElementsInClass(zbnfElement, sName);
      }
      else if(semantic.equals("attribute")){
            if(convertAttribute(bFirst, sSeparator, sCppClassName, sReflectionClassName, sNameVariant
          , zbnfElement, sbCfile, sbForward, cppClass))
        { nrofAttributes +=1;
          sSeparator = "\n  , ";  //next entries.
          bFirst = false;
        }
      }
      else if(semantic.equals("classVisibilityBlock")){
        convertElementsInClass(zbnfElement, null);  //elements of the classVisibilityBlock
      }
      else if(semantic.equals("variante")){  //variant of a union
        lastOffsetBeforeBitfield = null;     //A bitfield starts from offset 0 now, because it is a union.
        ZbnfParseResultItem variantContent = zbnfElement.firstChild();
        convertElement(variantContent, null);  //The structure inside a union
      }
      else if(semantic.equals("structDefinition") ) {//an inner, embedded struct
          //NOTE: commonly it is necessary to regard the name of the structure. But for bitfields it isn't necessary.
        String sVariantName = zbnfElement.getChildString("@name"); //The name of the struct inside union
        convertElementsInClass(zbnfElement, sVariantName);  //elements of the classVisibilityBlock
      }
      else if(semantic.equals("structContentInsideCondition"))
      { //inside a condition block the same content may be found.S
        ZbnfParseResultItem zbnfCondition;
        String sCondition;
        boolean bCondition = true;;
        zbnfCondition = zbnfElement.getChild("conditionDef");
        if(zbnfCondition !=null){
          sCondition = zbnfCondition.getParsedString();
          bCondition = idxDefines.get(sCondition) !=null;
        }
        if(bCondition){
          convertElementsInClass(zbnfElement, sNameVariant);
        }
      }

    }
    
    
    
    
    void testBasedOnObject(String sNameSuperclass)
    {      
      if(sNameSuperclass.equals("ObjectJcpp") || sNameSuperclass.equals("Object_Jcpp"))
      { bObjectJcpp = true;
        //type.sign = '$';
      }
      else if(sNameSuperclass.equals("ObjectJc") || sNameSuperclass.equals("ObjectJc_h") || sNameSuperclass.equals("Object_Jc"))
      { bClassBasedOnObjectJc = true;
        //type.sign = '$';
      }
      else if(sNameSuperclass.equals("ObjectifcBaseJcpp") || sNameSuperclass.equals("ObjectifcBase_Jcpp"))
      { bObjectifcBaseJcpp = true;
        //type.sign = '%';
      }
    }   

    
    
    
    /**called inside convertClass to detect inner classes. 
     */
    void convertInnerClassAndStruct
    ( String sParentCppClassName
    , String sParentReflectionClassName
    , ZbnfParseResultItem zbnfParent
    , TreeMap<String, String> innerTypesP
    , String sFilePath
    , boolean cppClass
    )
    throws IOException
    { List<ZbnfParseResultItem> innerClasses = zbnfParent.listChildren("classDef");
      if(innerClasses!=null)
      for(ZbnfParseResultItem innerClassItem : innerClasses)
      { String sTypeName = innerClassItem.getChild("@name").getParsedString();
        innerTypes.put(sTypeName, sParentReflectionClassName);
        ConverterClass converterClass = new ConverterClass(innerTypesP);
        converterClass.convertClass(sTypeName, sTypeName, sParentCppClassName, sParentReflectionClassName, innerClassItem, sFilePath, cppClass);//, typesInFile);
      }

      /**2011-04-12: don't convert implicitStructAttribute as extra type. It isn't able in C to do so.
       * If a implicitStruct is used, the type should remain anonymous. 
       * The attributes of the implicit struct are generated in form NAME_INNER already, see convertAttribute.
       *  
      List<ZbnfParseResultItem> zbnfImplicitStruct = zbnfParent.listChildren("implicitStructAttribute");
      //
      if(zbnfImplicitStruct!=null)
      for(ZbnfParseResultItem zbnfImplicitStructItem : zbnfImplicitStruct)
      { String sTagName = zbnfImplicitStructItem.getChildString("@tagname");  //@tagname");
        String sName = zbnfImplicitStructItem.getChildString("@name");  //@tagname");
        String sTypeName = sTagName != null ? sTagName : sName;
        //an unnamed embedded struct won't be recognized as an extra struct. 
        //Because, it is not able to address in the enclosing struct.
        //It is especially the situation in unions.
        //for example: union{ int first; struct{ int a; int b;};}
        if(sName != null){  //only named inner structures are extra types.
          if(!sParentCppClassName.startsWith("struct "))
          { sParentCppClassName = "struct " + sParentCppClassName;
          }
          innerTypes.put(sTypeName, sParentReflectionClassName);
          ConverterClass converterClass = new ConverterClass(innerTypesP);
          converterClass.convertClass(sTypeName, sParentCppClassName, sParentReflectionClassName, zbnfImplicitStructItem, sFilePath, cppClass);//, typesInFile);
        }
      }
      */
    }





    /**converts a attribute from parsed header.
     * @param sSeparator initialy "    " but than "  , " to implement the separator between or the introduce to the first attribute block.
     * @param sCppClassName Name of the class in c++-language, a.e. MyClass::InnerClass
     * @param sReflectionClassName Name of the class for Reflection identifier, a.e. MyClass_InnerClass
     * @param sNameVariant Name of the superior struct, which builds the variant
     * @param attributeItem The parse result item describing the attribute.
     * @param sbCfile buffer for C-file-content.
     * @param sbForward buffer for forward declarations
     * @return true if it is an attribute, false if nothing is generated.
     * @throws IOException
     */
    private boolean convertAttribute(boolean bFirst, String sSeparator, String sCppClassName
      , String sReflectionClassName
      , String sNameVariant
      , ZbnfParseResultItem attributeItem
      , StringBuffer sbCfile, StringBuffer sbForward, boolean cppClass) 
    throws IOException
    { final String sAttributeName1 = attributeItem.getChildString("@name");
      if(false && sAttributeName1 == null){
        return false;  //no name given, it may be a unnamed bitfield especially.
      } else {
        final String sAttributeName = sAttributeName1 == null ? null :  //sAttributeName == null controls flow 
          sNameVariant == null ? sAttributeName1 :
          sNameVariant + "-" + sAttributeName1;
  
        if(sAttributeName !=null && sAttributeName.equals("var1"))
          stop();
      
        if(bFirst && binOutPrep != null){
          /**Create the head of the fields*/
          binOutPrep.addFieldHead();
        }
        
        boolean bContainerObjectArray = false;
        boolean bReferenceInContainer = false;
        String sTypeInContainer = null;
        String sContainerType = null;
        int arrayLengthPerAnnotation = 0;
        ZbnfParseResultItem zbnfDescription = attributeItem.getChild("description");
        if(zbnfDescription != null){
          sTypeInContainer = zbnfDescription.getChildString("containerElementType");
          if(sTypeInContainer !=null){
            sContainerType = zbnfDescription.getChildString("containerType");
            bReferenceInContainer = zbnfDescription.getChild("referencedContainerElement") !=null;
          }
          /*
          ZbnfParseResultItem zbnfContainer = zbnfDescription.getChild("containerType");
          if(zbnfContainer != null){
            String sContainer = zbnfContainer.getParsedString();
            if(sContainer.startsWith("ObjectArrayJc")){ bContainerObjectArray = true; }
            else { console.writeWarning("unknown @container=" + sContainer); }
            int posBracket = sContainer.indexOf('[');
            int posBracketEnd = sContainer.indexOf(']');
            int posStar = sContainer.indexOf('*');
            if(posBracket > 0 && posBracketEnd >0 && posStar == posBracketEnd-1){
              sTypeInContainer = sContainer.substring(posBracket+1, posStar);
              bReferenceInContainer = true;
            } else if(posBracket > 0 && posBracketEnd >0 && posBracket < posBracketEnd){
              sTypeInContainer = sContainer.substring(posBracket+1, posBracketEnd);
            } else {
              console.writeWarning("failed @container="  + sContainer);  
            }
            if(posBracket >0){
              sContainerType = sContainer.substring(0, posBracket);
            }  
            
          }
          */
          List<ZbnfParseResultItem> listZbnfRefl = zbnfDescription.listChildren("refl");  //Hartmut-2010-11-15
          if(listZbnfRefl !=null){
            for(ZbnfParseResultItem zbnfRefl: listZbnfRefl ){
              String sRefl = zbnfRefl.getParsedString();
              if(sRefl.startsWith("arrayLength")){  ////
                int posEq = sRefl.indexOf('=');
                if(posEq <0 || sRefl.length() < posEq+1) throw new IllegalArgumentException(" Annotation refl: arrayLength = value: \"= value\" is missed. ");
                String sArrayLength = sRefl.substring(posEq+1).trim();
                TypeEntry replacement = identifierRelacements.get(sArrayLength);
                if(replacement !=null && replacement.sType == null){ //value is given then
                  arrayLengthPerAnnotation = replacement.value;
                } else {
                  try{ arrayLengthPerAnnotation = Integer.parseInt(sArrayLength); }
                  catch(NumberFormatException exc){ 
                    arrayLengthPerAnnotation = 1;
                    throw new IllegalArgumentException(
                      " Annotation refl: arrayLength = " + sArrayLength 
                      + ": This value isn't a number and it isn't found in the replacements. "
                      + "\nHint: You can add a replacement writing a line \"@ident = [0x]value\" in the config-file.");
                  }
                }
              }
            }
            
          }
        }
          
        
        ZbnfParseResultItem typeItem = attributeItem.getChild("type");
        String sType;
        int nReference;   //0: no reference, 1, 2 number of *
        if(typeItem == null)
        { if(attributeItem.getChild("@implicitStruct")!=null)
          { /*an implicit struct like "... struct Tag_t{ type element,...} name; " has a tag-name perhaps, but not always,
              therefore the name of the attribute is used as type-name.
              The same rule is taken by detect types from inner implicit struct
            */
            ZbnfParseResultItem zbnfTagName = attributeItem.getChild("@tagname");
            if(zbnfTagName != null)
            { sType = zbnfTagName.getParsedString();
            }
            else
            { sType = sAttributeName;  //may be null, then ignore.
            }
            //sType = sReflectionClassName + "_" + sAttributeName;
            nReference = 0;  //a implicit struct can not be used outside, because it is unknown.
            //struct{ type element} *name; is syntactical correct in C, but it is not useable. Do not consider here.
          }
          else
          { stop();  //TODO
            return false;
          }
        }
        else
        { ZbnfParseResultItem itemTypeName = typeItem.getChild("@name");
          if(itemTypeName == null)
          { stop();
            return false;
          }
          sType = itemTypeName.getParsedString();
          if(typeItem.getChild("@pointer") != null
            || typeItem.getChild("@constPointer") != null
            || typeItem.getChild("@volatilePointer") != null
            || typeItem.getChild("@cppRef") != null
            ){
            nReference = 1;
          } else if(
               typeItem.getChild("@volatilePointer2") != null
            || typeItem.getChild("@pointerRef") != null
            || typeItem.getChild("@pointer2") != null
            ){
            nReference = 2;
          } else {
            nReference = 0;
          }
          boolean bForwardStruct = typeItem.getChild("@forward") != null;
          if(sType.equals("ObjectJc") && bFirst)
          { /**A attribute with type ObjectJc is the first one, the class/struct is based on ObjectJc. */
            bStructHasFirstObjectJc = true;
          }
          if(sType.equals("char"))
            stop();
          
          //translate the type, possible on forward declaration struct TypeXX* or for define-replacement
          TypeEntry typeEntry = identifierRelacements.get(sType);
          if(typeEntry != null) //found
          { if(typeEntry.sign == (bForwardStruct ? '@' : '='))  //it is a translation
            { sType = typeEntry.sPath;
            }
          }
          else if(bForwardStruct && sType.endsWith("_t"))
          { //struct Type_t may be mostly of type Type
            sType = sType.substring(0,sType.length()-2);
          }
        }
        if(sTypeInContainer!= null){
          sType = sTypeInContainer;  //it is more significant, the sType may be the type of container of Type.
        }
        if(sAttributeName !=null && sAttributeName.equals("TEST"))
          stop();
        
        /**Shorten the name of the attribute, because the max. number of chars is limited. */
        final String sAttributeNameShow;
        if(sAttributeName == null){
          sAttributeNameShow = null;
        } else {
          final int zAttributeName = sAttributeName.length();
          if(zAttributeName <= Field_Jc.kLengthName-1){ sAttributeNameShow = sAttributeName; }
          else { 
            int restLength = Field_Jc.kLengthName - 13 -2;
            sAttributeNameShow = sAttributeName.substring(0, 13) + "_" + sAttributeName.substring(zAttributeName -restLength, zAttributeName); 
          }
          /**Write struct content of FieldJc in C-file.*/
          sbCfile.append(sSeparator +"{ \"" + sAttributeNameShow + "\"");
        }
        
        ZbnfParseResultItem zbnfBitfield = attributeItem.getChild("bitField");
        int bitfieldNrofBits = (zbnfBitfield != null) ? (int)zbnfBitfield.getParsedInteger(): -1;  
          
        FieldTypeInfos fieldTypeInfos = new FieldTypeInfos(); //usage of class instance is possible to optimize.
        setFieldTypeInfos(sType, sbForward, fieldTypeInfos, bitfieldNrofBits);
        
        int nrofArrayElementsOrBitfield = 0;
        ZbnfParseResultItem zbnfArraysize = attributeItem.getChild("arraysize");
        
        if(sAttributeName != null){  //generate only if a name is given.
          int mModifier = 0;  //value for modifier in the binary reflection file.
          
          if(bitfieldNrofBits >=0){
            assert(zbnfArraysize == null);  //bitfield-arrays are not supported
            nrofArrayElementsOrBitfield = ((bitfieldNrofBits <<12)&0xf000) + (bitfieldByte <<3) + bitfieldPos;
            sbCfile.append(  "\n    , 0x" + Integer.toHexString(nrofArrayElementsOrBitfield));
            sbCfile.append(" //bitfield nrofBits=" + bitfieldNrofBits );
            sbCfile.append(", bitPos=").append(bitfieldByte).append(".").append(bitfieldPos);
          } else if(zbnfArraysize != null)
          { Iterator<ZbnfParseResultItem> iterZbnf = zbnfArraysize.iteratorChildren();
            while(iterZbnf.hasNext())
            { ZbnfParseResultItem zbnfArraysizeChild = iterZbnf.next();
              if( zbnfArraysizeChild.getSemantic().equals("@value"))
              { nrofArrayElementsOrBitfield = (int)zbnfArraysizeChild.getParsedInteger(); 
              }
              else if( zbnfArraysizeChild.getSemantic().equals("@symbolValue"))
              { String name = zbnfArraysizeChild.getParsedString();
                TypeEntry entry = identifierRelacements.get(name);
                if(entry != null && entry.value >0)
                { nrofArrayElementsOrBitfield = entry.value ;
                }
                else 
                { nrofArrayElementsOrBitfield = 0; 
                  console.writeWarning("symbolic arraysize: " + name + " - no value.");
                }
              }
              else if( zbnfArraysizeChild.getSemantic().equals("binaryOperator"))
              { console.writeWarning("not supported yet: binary operator in arraysize ");
              }
            } 
            /*
              ZbnfParseResultItem zbnfValue;
              if( (zbnfValue  = zbnfArraysize.getChild("@value")) != null)
              { nrofArrayElements = (int)zbnfValue.getParsedInteger(); }
              else if( (zbnfValue  = zbnfArraysize.getChild("@symbolValue")) != null)
              { String name = zbnfValue.getParsedString();
                TypeEntry entry = blockedFilesAndTypes.get(name);
                if(entry != null && entry.value >0)
                     { nrofArrayElements = entry.value ;}
                else 
                { nrofArrayElements = 0; 
                  console.writeWarning("symbolic arraysize: " + name + " - no value.");
                }
              }
              else
              { //it is possible to have a "@symbolValue", ignore it yet.
                nrofArrayElements = 0;
              }
            */
            sbCfile.append(  "\n    , " + nrofArrayElementsOrBitfield).append("   //nrofArrayElements");
            
          } else if(arrayLengthPerAnnotation >0){
            nrofArrayElementsOrBitfield = arrayLengthPerAnnotation;
            sbCfile.append(  "\n    , " + nrofArrayElementsOrBitfield).append("   //nrofArrayElements per Annotation");
          } else { 
            nrofArrayElementsOrBitfield = 0;
            sbCfile.append(  "\n    , 0   //no Array, no Bitfield");
          }
          
          /**Write Type of field: */
          if(bitfieldNrofBits >=0){
            sbCfile.append("\n    , REFLECTION_BITFIELD");
            sbCfile.append("\n    , kBitfield_Modifier_reflectJc");
            mModifier |= Class_Jc.kBitfield_Modifier;
          }
          
          else if(fieldTypeInfos.bytesScalarType >=0)
          { sbCfile.append("\n    , REFLECTION_" + sType);
            sbCfile.append("\n    , ("+ fieldTypeInfos.bytesScalarType+"<<kBitPrimitiv_Modifier_reflectJc)");
            mModifier = fieldTypeInfos.bytesScalarType << Class_Jc.kBitPrimitiv_Modifier;
          }
          else
          {
            if(fieldTypeInfos.useReflection)
            { sbCfile.append("\n    , &reflection_" + sType);
            }
            else
            { sbCfile.append("\n    , REFLECTION_void");
            }
            sbCfile.append("\n    , 0");  //bitModifiers
          }
          
          if(sContainerType != null){
            if(sContainerType.equals("ObjectArrayJc")){
              sbCfile.append("| kObjectArrayJc_Modifier_reflectJc");
              mModifier |= Class_Jc.kObjectArrayJc_Modifier;
            }
            if(nReference >0){
              sbCfile.append("| kReferencedContainer_Modifier_reflectJc");
              mModifier |= Class_Jc.kReferencedContainer_Modifier;
            } else {
              sbCfile.append("| kEmbeddedContainer_Modifier_reflectJc");
              mModifier |= Class_Jc.kEmbeddedContainer_Modifier;
            }
            nReference = bReferenceInContainer ? 1 : 0;
          } else if (arrayLengthPerAnnotation >0 && nReference >0){
            //if the definition follows the pattern Type* array with annotation @refl:arrayLength=99,
            //it is a referenced static array of this type. 
            sbCfile.append("| kReferencedContainer_Modifier_reflectJc");
            mModifier |= Class_Jc.kReferencedContainer_Modifier;
            nReference -=1;  //typical 0
          }
          if(nReference >0){ 
            sbCfile.append("| mReference_Modifier_reflectJc"); 
            mModifier|= Class_Jc.mReference_Modifier;
          }
          if(zbnfArraysize != null && nrofArrayElementsOrBitfield >1)
          {
            sbCfile.append(" |kStaticArray_Modifier_reflectJc|kEmbeddedContainer_Modifier_reflectJc");
            mModifier|= Class_Jc.kStaticArray_Modifier|Class_Jc.kEmbeddedContainer_Modifier;
          }
          if(arrayLengthPerAnnotation >0){
            sbCfile.append(" |kStaticArray_Modifier_reflectJc");
            mModifier|= Class_Jc.kStaticArray_Modifier;
          }
          sbCfile.append(" //bitModifiers");
  
          final String sOffset;
          if(bitfieldNrofBits <0){
            sOffset = buildOffset(sTypeName, sAttributeName, cppClass);
            bitfieldPos = 0;  //a new bitfield will be start with byte0.bit0
            bitfieldByte = 0;
            lastOffsetBeforeBitfield = sOffset;
            lastType = sType + "****".substring(0, nReference);
            
            
          } else {
            
            if(lastOffsetBeforeBitfield == null){
              //the first field is a bitfield.
              sOffset = "0";   //Position is 0.
            } else {
              //TODO
              sOffset = lastOffsetBeforeBitfield + " + sizeof(" + lastType + ")";
            }
          }
          
          
          if(fileOffs != null){
            //String sTypeForSize = nReference>0 ? sType + "*" : sType;
            String sTypeForSize = nReference>0 ? "void*" : sType;    //size of a pointer anyway
            String sSize = StringPart.replace(sExprSizeType, sPlaceholderType, new String[]{sTypeForSize}, null);
            fileOffs.write("\n    , (" + sSize + "<<16) | (" + sOffset + ")");                //Offsetfile: it is a int32
            sbCfile.append(  "\n    , -1   //offset in extra file");
            
          } else {
            sbCfile.append(  "\n    , " + "(int16)" + sOffset);  //Reflection-file: cast to (int16)
          }
          
          sbCfile.append(  "\n    , 0  //offsetToObjectifcBase");
          sbCfile.append(  "\n    , &reflection_" + sReflectionClassName);
          sbCfile.append(  "\n    }");
  
          if(binOutPrep != null && sAttributeName != null){  //generate only if a name is given.
            binOutPrep.addField(sAttributeNameShow, fieldTypeInfos.typeIdent, sType, mModifier, nrofArrayElementsOrBitfield);
          }
          
        } //if(sAttributeName != null)
        if(bitfieldNrofBits >=0){
            
          bitfieldPos += bitfieldNrofBits;
          if(bitfieldPos > 8){
            bitfieldPos -= 8;
            bitfieldByte += 1;
          }
        }
        return sAttributeName != null;
      }
      
    }
    

    
    String buildOffset(String sTypeName, String sAttributeNameP, boolean cppClass)
    {
      String sAttributeName = sAttributeNameP.replace('-', '.');
      String[] sReplacement = new String[]{ sTypeName, sAttributeName };  //order of sExprTokens
      
      //String sOffset = "((int32)(&((" + sCppClassName + "*)(0x1000))->" + sAttributeName + ") ";
      String sOffset = StringPart.replace(sExprOffsField, sExprTokens, sReplacement, null);
      if(bObjectJcpp)
      { assert(cppClass);
        //sOffset += "- (int32)(static_cast<ObjectJc*>(static_cast<ObjectJcpp*>((" + sCppClassName+ "*)0x1000))))";
        sOffset += "-" + StringPart.replace(sExprOffsObjJcpp, sExprTokens, sReplacement, null);
      }
      else if(bClassBasedOnObjectJc)
      { if(c_only || !cppClass){
          //sOffset += "- (int32)&((" + sCppClassName + "*)0x1000)->base.object)";
          sOffset += "-" + StringPart.replace(sExprOffsObj, sExprTokens, sReplacement, null);
        } else {
          //sOffset += "- (int32)(static_cast<ObjectJc*>((" + sCppClassName+ "*)0x1000)))";
          sOffset += "-" + StringPart.replace(sExprOffsCppObj, sExprTokens, sReplacement, null);
        }
      }
      else
      { //sOffset += "- (int32)(" + sCppClassName+ "*)0x1000)";
        sOffset += "-" + StringPart.replace(sExprOffsBase, sExprTokens, sReplacement, null);
      }
      return sOffset;
    }

    
    
    
    /**Detects all infos about the field type.
     * @param sType The type maybe a scalar type, maybe an inner type of the class.
     * @param sbForward Buffer
     * @param ret All return informations where filled there.
     */
    void setFieldTypeInfos(String sType, StringBuffer sbForward, FieldTypeInfos ret, int bitfieldNrofBits)
    { 
      /**The return informations where declared as simple variables to detect forgotten sets. 
      /**Information about scalar basic types. */
      final int bytesScalarType;
      /**Decision whether the type is knwon. */ 
      boolean useReflection;
      
      /**Type address or ident for binary file. */
      int typeAddress;
      
      Field_Jc.TypeSizeIdent scalarSizeIdent = Field_Jc.getTypeSizeIdent(sType);

      if(bitfieldNrofBits >0){
        bytesScalarType = 7;   //designation of bitfield.
        typeAddress = Field_Jc.REFLECTION_bitfield;
        useReflection = true;
      }
      else if(scalarSizeIdent != null) {
        bytesScalarType = scalarSizeIdent.size;
        typeAddress = scalarSizeIdent.ident;
        useReflection = true;
      }
      else
      { bytesScalarType = -1;
        if(sType.startsWith("MT_"))
        { //it is a method type declaration, no reflections!
          useReflection = false;
          typeAddress = 0;
        }
        else
        { 
          /**Check whether the type is known as inner type. */
          String sAmbientType = innerTypes.get(sType);
          if(sAmbientType != null)
          { //the type is a type of the ambient class. Therefore the type should named with this information.
            sType = sAmbientType + "_" + sType;
            typeAddress = 0;   //TODO
            useReflection = true;
          }
          else {
            /**Not a scalar type, not an inner type, not a special type, check whether the type is disabled to use globally: */
            TypeEntry typeEntry = identifierRelacements.get(sType);
            if(typeEntry != null && (typeEntry.sign == '#' || typeEntry.sign == '-'))
            { useReflection = false; //disabled.
              typeAddress = 0;
            }
            else
            { useReflection = true;  //known.
              typeAddress = -1;      //flag that the type should be replaced later.
            }
          }  
          if(useReflection)
          { sbForward.append("\nextern_C const ClassJc reflection_" + sType + ";");
          }
        }  
      }
      
      /**Sets the values in ret at end, to detect forgotten assignments. */
      ret.bytesScalarType = bytesScalarType;
      ret.typeIdent = typeAddress;
      ret.useReflection = useReflection;
    } 
    
    
    
    
    /**Converts a int value to 4 bytes.
     * @param val
     * @return
     */
    byte[] int2bytes4(int val){
      byte[] ret = new byte[4];
      ret[0] = (byte)(val >>24);
      ret[1] = (byte)(val >>16);
      ret[2] = (byte)(val >>8);
      ret[3] = (byte)(val);
      return ret;
    }
    
    @Override public String toString()
    {
      String sRet = "" + bitfieldPos + innerTypes; 
      return sRet;
    }
  }

  /**Helper method for debugging. */
  void stop()
  { //debug
  }

  
  
  
  
  
  

}