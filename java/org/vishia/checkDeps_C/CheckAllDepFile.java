package org.vishia.checkDeps_C;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import org.vishia.mainCmd.MainCmdLogging_ifc;
import org.vishia.util.FileFunctions;

public class CheckAllDepFile
{

  /**Version, history and license.
   * <ul>
   * <li>2012-12-25 Hartmut new: Inserted in the Zbnf component because it is an integral part of the Zmake concept
   *   for C-compilation.
   * <li>2011-05-00 Hartmut created: It was necessary for C-compilation to check real dependencies in a fast way.
   *   The dependency check from a GNU compiler was to slow.
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
   */
  public static final int version = 20121225;

  
  final CfgData cfgData;
  
  final MainCmdLogging_ifc console;
  
  final CheckData checkData;
  
  /**The timestamp of operation system may be not exactly. Admiss this abbreviation. */
  private final static int maxTimeAbbreviation = 2500;

  
  CheckAllDepFile(CfgData cfgData, MainCmdLogging_ifc console, CheckData checkData)
  {
    this.cfgData = cfgData;
    this.console = console;
    this.checkData = checkData;
  }
  
  
  void readDepFile(String sFileName)
  {
    try{
      BufferedReader reader = null;
      File fileDep = FileFunctions.newFile(sFileName);
      if(fileDep.exists()){
        reader = new BufferedReader(new FileReader(fileDep));
        String sLine;
        sLine = readFileEntry(null, reader);
        while( sLine !=null ){
          if(sLine.startsWith("File: ")){
            sLine = readFileEntry(sLine.substring(6), reader);
          } else {
            throw new IllegalArgumentException("CheckDeps: Syntax error in file:" + fileDep.getAbsolutePath() + "; line:" + sLine + "expected: >File: ...");
          }
            
        }
      }
    } catch(IOException exc){
      throw new IllegalArgumentException("CheckDeps_C - argument error; cannot create -depAll=" + sFileName);
    }
    
  }
  
  
  
  /**
   * @param sLine
   * @param reader
   * @return the next line, not used
   * @throws IOException 
   */
  String readFileEntry(String sLine, BufferedReader reader) throws IOException
  {
    final InfoFileDependencies infoRead;
    if(sLine !=null){
      String[] lineParts = sLine.split(";");
      String sDeepness = lineParts[0].trim();
      char cDeepness = sDeepness.charAt(0);
      final long dateBuild, dateSource;
      final String sAbsPathFileBuild, sAbsPathFileSource;
      final File fileMirror, fileSource;
      if(lineParts.length >5){
        dateBuild = Long.parseLong(lineParts[1].trim());
        dateSource = Long.parseLong(lineParts[5].trim());
        sAbsPathFileBuild = lineParts[2].trim();
        sAbsPathFileSource = lineParts[4].trim();
        fileMirror = FileFunctions.newFile(sAbsPathFileBuild);
        fileSource = FileFunctions.newFile(sAbsPathFileSource);
      } else {
        dateBuild = dateSource = Long.parseLong(lineParts[1].trim());
        sAbsPathFileBuild = sAbsPathFileSource = lineParts[2].trim();
        fileMirror = null;
        fileSource = FileFunctions.newFile(sAbsPathFileSource);
      }
      //check the timestamps from .dep-file-line and reality:
      if( Math.abs((int)(fileSource.lastModified() - dateSource)) < maxTimeAbbreviation
        && (fileMirror==null || Math.abs((int)(fileMirror.lastModified() - dateBuild)) < maxTimeAbbreviation)
        ){
        //the source is not change in comparison to the last build (.dep-file).
        //store the info with all its unchangend primary dependencies:
        String sLocalPath = cfgData.checkIsInSourcePool(sAbsPathFileSource);
        boolean isSourceFile = sLocalPath !=null; 
        String sFilePath = isSourceFile ? sLocalPath : sAbsPathFileSource;
        infoRead = new InfoFileDependencies(sFilePath, fileSource, fileMirror, isSourceFile, console);
        checkData.indexInfoInput.put(sAbsPathFileSource, infoRead);
      } else {
        infoRead = null;
      }
    } else {
      infoRead = null;  //read the first line.
    }
    boolean bFileLine = false;
    while( !bFileLine && (sLine = reader.readLine()) !=null ){
      int posComment = sLine.indexOf('#');
      if(posComment >=0){ sLine = sLine.substring(0, posComment); }
      sLine = sLine.trim();
      int posStart;
      if(sLine.length() >0){
        if(sLine.startsWith("File: ")){
          bFileLine = true;
        } else if( (posStart = sLine.indexOf("-")) >=0 && posStart < 4){ //direct include
          String sIncludefile = sLine.substring(posStart+1).trim();
          if(infoRead !=null){
            //put an empty information with the filename into:
            InfoFileDependencies infoEmpty = new InfoFileDependencies(sIncludefile, console);
            infoRead.includedPrimaryDeps.put(sIncludefile, infoEmpty);
          }
        } else {
          //skip over, non interesting.
        }
      }
    }
    
    return sLine;
  }
  

  
  
  void stop(){}
  
  
  
}
