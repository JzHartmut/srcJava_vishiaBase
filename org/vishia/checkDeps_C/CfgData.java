package org.vishia.checkDeps_C;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.vishia.util.Assert;


/**Contains the data read from the config file. */
public class CfgData
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

  

  
  /**This directories are set as include path to search include files. */
  final List<File> listInclPaths = new LinkedList<File>();
  
  /**This directories are set as include path to search include files. */
  final List<File> listGenSrcInclPaths = new LinkedList<File>();
  
  /**This directories are set as include path to search system include files. */
  final List<File> listSystemInclPaths = new LinkedList<File>();
  
  /**List of all paths which are recognized as source files. For files in that path
   * a .dep-file and a mirror file can be build.
   */
  final List<InputSrc> listSourcePaths = new LinkedList<InputSrc>();
  
  /**List of all paths which are to processed for checking translation. 
   * For files in that path a object-file is build.
   * All members of this list are contained in listSourcePaths too.
   */
  final List<InputSrc> listProcessPaths = new LinkedList<InputSrc>();
  
  
  /**Checks whether the given File is in the source pool
   * @param sAbsolutePath The absolute path
   * @return null if it isn't in sourcepool. Else the local path with name and ext of the file
   *   starting after the ':' in the -src= argument of command line invocation.
   */
  String checkIsInSourcePool(String sAbsolutePath)
  {
    if(sAbsolutePath.contains("ccs_param_values.h"))
      Assert.stop();
    for(InputSrc inputSrc: listSourcePaths){
      if(sAbsolutePath.startsWith(inputSrc.sCanonicalPathSrc)){ 
        String sLocalPath = sAbsolutePath.substring(inputSrc.sCanonicalPathSrc.length());
        return inputSrc.sLocalSrcPath + "/" + sLocalPath; 
      }
    }
    return null;  //returns if found.
  }




  
}
