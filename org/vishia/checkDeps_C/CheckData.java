package org.vishia.checkDeps_C;

import java.io.File;
import java.io.Writer;
import java.util.Map;
import java.util.TreeMap;

/**This class contains all data which are necessary to process all files.
 * @author Hartmut Schorrig
 *
 */
public class CheckData
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

  

  /**List of paths which are given as source path. It is from the argument -src=SRCPATH. */
  //List<InputSrc> sPathsSrc = new LinkedList<InputSrc>();

  /**Some counters to report. */
  int nrofNewDeps, nrofChangedFiles, nrofNewFiles, nrofSrcFiles, nrofExtInclFiles, nrofDelObj, nrofRecompilings;

  /**All found Dependencies read from the input dependency file. The input dependency file was created by the last run
   * of this programm and contains all dependencies of the files in that time.
   * <br>
   * The {@link InfoFileDependencies#includedPrimaryDeps} - index is filled with all 
   * primary depending files from the input dependency description file. 
   * That index contains the canonical file names only.
   * <br> 
   * If an entry of this list is used for check, all includes and their includes are checked then.
   * The InfoFileDependencies-entry will be completed therefore. Then this entry is removed from here
   * and added in the {@link #indexAllInclFilesAbsPath} instead. It means, this list will be cleared
   * step by step. If any entry isn't removed at last, it means it isn't used as source file
   * for translation furthermore. This situation can occur for yet unused header files for example.
   * */
  final Map<String, InfoFileDependencies> indexInfoInput = new TreeMap<String, InfoFileDependencies>();

  /**All found and parsed include files sorted by name in the include statement, with relative paths. 
   * It is the path which was written in the source-files in the include statement.
   * If the same included file is processed already, it is found here quickly.*/
  final Map<String, InfoFileDependencies> indexAllInclFilesShortPath = new TreeMap<String, InfoFileDependencies>();
  
  /**All found and parsed include files sorted by name in the include statement, with absolute paths. 
   * If there is written a different include statement in the sources, but the file is the same
   * then it is processed only one time. The referenced InfoFileDependencies are the same
   * like in the relative path.
   * Searching in this index needs the search of the include-line in the include path.
   * Because it is less quickly, the {@link #indexAllInclFilesShortPath} helps to search more quickly. */
  final Map<String, InfoFileDependencies> indexAllInclFilesAbsPath = new TreeMap<String, InfoFileDependencies>();
  
  
  /**All found and parsed source files sorted by name in the include statement, with absolute path. */
  final Map<String, InfoFileDependencies> indexSrcFilesAbs = new TreeMap<String, InfoFileDependencies>();

  /**Writer for all dependencies, use it if not null. */
  Writer writerDepAll;
  
  /**The dirSrcMirror refers to the build root directory. It is null, if the calling argument -srcBuild= isn't given.
   * All input paths are located inside this directory tree. 
   * For the src directories given with -src= calling argument, a local path part can be given.
   * The argument is written in form PATH:PATHLOCAL. The PATHLOCAL is used inside that attribute.
   * Additional the local path of the files are regarded. 
   */
  final File dirSrcMirrorRoot;

  final File dirDepsRoot;
  
  /**The dirObj refers to the object root directory.
   */
  final File dirObjRoot;

  public CheckData(File dirSrcMirror, File dirObjRoot, File dirDepRoot, Writer writerDepAll)
  {
    super();
    this.dirSrcMirrorRoot = dirSrcMirror;
    this.dirObjRoot = dirObjRoot;
    this.writerDepAll = writerDepAll;
    this.dirDepsRoot = dirDepRoot;
  }
  

  void xxxputIntoIndexAllInclFilesAbsPath(String sAbsolutePath, InfoFileDependencies info){
    indexAllInclFilesAbsPath.put(sAbsolutePath, info);
  }

  

}
