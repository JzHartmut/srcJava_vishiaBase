package org.vishia.util;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.vishia.cmd.JZtxtcmdExecuter;
import org.vishia.cmd.JZtxtcmdFileset;
import org.vishia.util.FilePath.FilePathEnvAccess;


/**A Fileset instance contains some {@link FilePath} maybe with a common path.
 * The method {@link #listFiles(List, FilePath, org.vishia.util.FilePath.FilePathEnvAccess, boolean)}
 * prepares a List of simple {@link FilePath} with resolved access- and common path and resolved variables.
 * <br><br>
 * Uml-Notation see {@link org.vishia.util.Docu_UML_simpleNotation}:
 * <pre>
 *                FileSet
 *                    |------------commonpath------------>{@link FilePath}
 *                    |
 *                    |------------filesOfFileset-------*>{@link FilePath}
 *                                                        -drive:
 *                                                        -absPath: boolean
 *                                                        -basepath
 *                                                        -localdir
 *                                                        -name
 *                                                        -someFiles: boolean
 *                                                        -ext
 * </pre>
 */
public final class FileSet
{
  
  
  /**Version, history and license.
   * <ul>
   * <li>2017-09-01 Hartmut new: A FilePath can refer a {@link FileSet} variable or especially a {@link JZtxtcmdFileset} variable.
   *   Then it is not a FileSet but it is a reference to an included FileSet. On {@link FileSet#listFiles(List, FilePath, FilePathEnvAccess, boolean)}
   *   it will be recognized and unpacked. It is on runtime. Note: On script compilation time the variable content may not existent yet.
   *   The variable is only given as String.    
   * <li>2014-06-22 Hartmut chg: creation of {@link FileSet} as extra class from {@link org.vishia.cmd.JZtxtcmdFileset}. 
   * <li>2014-03-07 created. From srcJava_Zbnf/org/vishia/zmake/ZmakeUserScript.UserFileset.
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
  static final public String sVersion = "2017-09-01";

  
  /**From ZBNF basepath = <""?!prepSrcpath>. It is a part of the base path anyway. It may be absolute, but usually relative. 
   * If null then unused. */
  private FilePath commonPath;
  
  /**From ZBNF srcext = <""?srcext>. If null then unused. */
  //public String srcext;
  
  
  /**All entries of the file set how it is given in the users script. */
  private final List<FilePath> filesOfFileset = new ArrayList<FilePath>();
  
  public void set_commonPath(String val){ commonPath = new FilePath(val); }
  
  public void add_filePath(String val){ 
    FilePath filepath = new FilePath(val); 
    if(filepath.isNotEmpty()){
      //only if any field is set. not on empty val
      filesOfFileset.add(filepath); 
    }
    
  }

  
  
  /**Builds a list of {@link FilePath} from the given fileSet. The new FilePath in files are absolute without a {@link FilePath#scriptVariable}
   * and without {@link FilePath#varChars} and {@link FilePath#varFileset}. This relations are dissolved.
   * if a file set is detect inside this fileset, it is expanded too. Only 100 nested filesets are possible (typically 1 or 2) 
   * @param files The new {@link FilePath} are added here.
   * @param accesspath The access path is an additional part of a base path.
   * @param env Access to variables and currDir of a {@link JZtxtcmdExecuter} or other environment
   * @param expandFiles expands wildcards, if false, the wildcard remain in the path paprts.
   * @throws NoSuchFieldException
   */
  public void listFiles(List<FilePath> files, FilePath accesspath, FilePath.FilePathEnvAccess env, boolean expandFiles) throws NoSuchFieldException {
    listFiles(files, accesspath, env, expandFiles, 0);
  }
  
  /**Called fro {@link #listFiles(List, FilePath, FilePathEnvAccess, boolean)}
   * @param recursionCount max. 100 recursions.
   * @throws NoSuchFieldException
   */
  private void listFiles(List<FilePath> files, FilePath accesspath, FilePath.FilePathEnvAccess env, boolean expandFiles, int recursionCount) throws NoSuchFieldException {
    for(FilePath filepath: filesOfFileset){
      if(expandFiles && (filepath.someFiles() || filepath.allTree())){
        List<FilePath> files1 = new LinkedList<FilePath>();
        filepath.expandFiles(files, commonPath, accesspath, env);
      } else {
        //clone and resolve common and access path
        FilePath targetsrc = new FilePath(filepath, commonPath, accesspath, env);
        FileSet addFileSet = targetsrc.isFileSet();
        if(addFileSet !=null) {
          if(recursionCount >=100) throw new IllegalArgumentException("too many recurions, may be faulty: " + filepath.toString());
          addFileSet.listFiles(files, accesspath, env, expandFiles, recursionCount +1);  //add its files.
        } else {
          files.add(targetsrc);
        }
      }
    }
  }

  
  @Override
  public String toString(){ 
    StringBuilder u = new StringBuilder();
    if(commonPath !=null) u.append("commonPath="+commonPath+", ");
    u.append(filesOfFileset);
    return u.toString();
  }


}
