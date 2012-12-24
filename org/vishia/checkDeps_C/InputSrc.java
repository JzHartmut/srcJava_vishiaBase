package org.vishia.checkDeps_C;

import java.io.File;
import java.io.IOException;

import org.vishia.util.FileSystem;

/**This class stores canonical pathes of source files and there associated local part.
 * If any canonical path of a given file starts with the here given sCanonicalPathSrc,
 * then it is a source file. The local path is the rest of the canonical path of the source file
 * and the here given sLocalSrcPath:
 * <pre>
 * ../..\\dir:path1/path2        - given path for constructor, not canonical. : as separator for local part
 *            path1/path2        - this is the sLocalSrcPath.
 * D:\\SBOX\\dir\\path1\\path2\\ - the sCanonicalPathSrc            
 * D:\\SBOX\\dir\\path1\\path2\\pathX\\file.c - any given file
 *                              pathX\\file.c - the local path of file
 *                 path1/path2\\pathX\\file.c - the local path to build obj, dep and mirror file                       
 */
public class InputSrc
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

  
  /**The directory which is designated by the argument -src=PATH. */
  final File dirSrcBase;
  
  /**The local part of the path after a ':' in the argument. */
  final String sLocalSrcPath;
  
  /**The canonical path of dirSrc in the file system. The separator is / always, on windows systems too!*/
  final String sCanonicalPathSrc;
  
  /**Creates an instance adequate to the input argument -src=PATH,
   * which allows the association of any source to the sourcepool.
   * @param sPathSrcP Maybe a relative path.
   */
  public InputSrc(final String sPathSrcP)
  { final String sPathSrc = sPathSrcP.replace('\\', '/');
    final String sPathSrcDir;
    int nSep = sPathSrc.indexOf(':', 2);  //from relative path.
    if(nSep >= 2){
      sLocalSrcPath = sPathSrc.substring(nSep+1);
      sPathSrcDir = sPathSrc.substring(0, nSep) + "/" + sLocalSrcPath;
    } else {
      sLocalSrcPath = "";
      sPathSrcDir = sPathSrc;
    }
    dirSrcBase = new File(sPathSrcDir);
    if(!dirSrcBase.exists()){
      if(!dirSrcBase.exists()) throw new IllegalArgumentException("CheckDeps - src directory not found: " + dirSrcBase.getAbsolutePath());
      if(!dirSrcBase.isDirectory()) throw new IllegalArgumentException("CheckDeps -src=PATH should be a directory: " + dirSrcBase.getAbsolutePath()); 
    }
    sCanonicalPathSrc = FileSystem.getCanonicalPath(dirSrcBase) + '/'; 
  }

  /**For debugging. Returns the absolute path to show what is it. */
  @Override public String toString(){ return sCanonicalPathSrc; }
}
