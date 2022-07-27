package org.vishia.checkDeps_C;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

import org.vishia.mainCmd.MainCmdLogging_ifc;
import org.vishia.util.FileSystem;




/**This class accumulates all dependencies of a Object. It is used as argument for some methods
 * which writes its found dependencies.
 * <br><br>
 * Methods which fills it:
 * <ul>
 * <li>{@link InfoFileDependencies#notifyIncludedNewly(ObjectFileDeps)}
 * <li>{@link InfoFileDependencies#notifyNewly(ObjectFileDeps)}
 * </ul>
 * @author Hartmut Schorrig
 *
 */
class ObjectFileDeps
{
  /**Version, history and license.
   * <ul>
   * <li>2014-05-10 Hartmut chg: {@link #ObjectFileDeps(File, String, String)} for one Object directory. 
   * <li>2013-01-06 Hartmut chg: Now supports more as one obj file. This is necessary if one make process compiles several versions.
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

  
  /**The object file. */
  //private final File fileObj;
  //private final long timestampObj;
  
  private final List<File> fileObjs = new LinkedList<File>();
  
  private long timestampSrcNewest;
  
  //final File fileDeps;

  /**State of deleting the object:
   * <ul><li>'.': Not changed
   * <li>'d': was deleted because newer files
   * <li>'n': is not found, but should deleted because newer files.
   * </ul>
   */
  private char objIsDeleted = '.';
  
  /**List of all newer (changed) source files. Includes and the primary source itself. */
  private final List<InfoFileDependencies> newerFiles = new LinkedList<InfoFileDependencies>();
  
  private ObjectFileDeps(File fileObj){
    //this.fileObj = fileObj;
    //timestampObj = fileObj == null ? Long.MAX_VALUE: fileObj.lastModified();
    //this.fileDeps = null;
  }
  
  
  /**Creates an instance.
   * @param dirObjRoot File object to the object root directory. It should be a directory.
   *   It does not need existing. 
   * @param sLocalPath The file path and name.ext of the source file as local path. 
   *   It is used as object path, without the extension.
   * @param sExtObj Extension for object files inclusively dot, usual ".obj" or ".o"
   */
  ObjectFileDeps(File dirObjRoot, String sLocalPath, String sExtObj){
    int posExt = sLocalPath.lastIndexOf('.');
    File fileObj = new File(dirObjRoot, sLocalPath.substring(0, posExt) + sExtObj);
    fileObjs.add(fileObj);
  }
  
  
  /**Creates an instance.
   * @param dirObjRoots List of Strings in form "path/to/objdir/*.obj"
   *   The object-file(s) are build with replacement of "*" with the sLocalPath without extension
   * @param sLocalPath The file path and name.ext of the source file as local path. 
   *   It is used as object path, without the extension.
   */
  ObjectFileDeps(List<String> dirObjRoots, String sLocalPath){
    int posExt = sLocalPath.lastIndexOf('.');
    for(String sDirObjRoot: dirObjRoots){
      StringBuilder uDirObj = new StringBuilder(sDirObjRoot);
      int posName = uDirObj.indexOf("*");
      uDirObj.replace(posName, posName+1, sLocalPath.substring(0, posExt));
      File fileObj = new File(uDirObj.toString());
      fileObjs.add(fileObj);
    }
  }
  
  
  void storeChangedSource(String sFile){
    //sNewerFiles.add(sFile);
  }
  
  
  boolean notifyNewer(InfoFileDependencies infoFile, MainCmdLogging_ifc console){
    boolean bDelete = false;
    boolean bNewer;
    if(infoFile.isNewlyOrIncludedNewly()){
      bNewer = true;
    } else {
      bNewer = false;
      for(File fileObj: fileObjs){
        if(fileObj.exists() && fileObj.lastModified() < infoFile.timestampNewestDependingFiles_){
          bNewer = true;
        }
      }
      //assert(false);
    }
    if(bNewer){
      if(infoFile.timestampNewestDependingFiles_ > timestampSrcNewest){
        timestampSrcNewest = infoFile.timestampNewestDependingFiles_;
      }
      newerFiles.add(infoFile);
      //try to delete the obj now, not at least (better for debugging).
      for(File fileObj: fileObjs){
        if(fileObj.exists()){
          if(fileObj.delete()){
            objIsDeleted = 'd';
            console.writeInfoln("CheckDeps - obj-deleted;" + fileObj.getPath());
            bDelete = true;
          } else {
            console.writeError("Problem deleting object: " + fileObj.getAbsolutePath());
          }
        } else if(objIsDeleted == '.'){
          objIsDeleted = 'n';
          console.writeInfoln("CheckDeps - obj-recompile;" + fileObj.getPath());
          bDelete = true;        //for test without compiled obj. All obj which will be deleted if exists are countered.
        }
      }
    }
    return bDelete;
  }
  
  
  public void createObjDir(MainCmdLogging_ifc console)
  {
    for(File fileObj: fileObjs){
      try{ FileSystem.mkDirPath(fileObj); }
      catch(IOException exc){
        console.writeError("ObjectFileDeps.createObjDir - Problem creating directory for: " + fileObj.getAbsolutePath());
      }
    }
  }
  
  
  
  /**Returns true if the object file was deleted because newer files
   * <li>'n': is not found, but should deleted because newer files.
   * @return false if the Object may delete but it isn't found. True only if it is really deleted.
   */
  public boolean isObjDeleted(){ return objIsDeleted == 'd'; }

  /**Returns true if the object file may deleted because newer files
   * @return true also if it isn't found (not really deleted) 
   */
  public boolean isObjRecompile(){ return objIsDeleted != '.'; }
  
}


