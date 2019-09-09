package org.vishia.util;

import java.util.LinkedList;
import java.util.List;

import org.vishia.cmd.JZtxtcmdFileset;



/**This class holds a path to a file with its parts: base path, directory path, name, extension, drive letter etc
 * and supports getting String-paths in several forms - absolute, relative, name only, extension, with slash or backslash etc.
 * It supports using of script variables.
 * 
 * For building the absolute path or using script variables the user should provide an implementation 
 * of the {@link FilePathEnvAccess}
 * which supports access to String-given variables and the current directory of the user's environment.
 * This interface is implemented in the {@link org.vishia.cmd.JZtxtcmdExecuter} environment.
 * <br><br>
 * <b>The local path</b>:<br>
 * If you write <code>any/Path:local/path/file.ext</code> then <code>any/Path</code> is the so named base path 
 * and <code>local/path</code> is the local directory. The <code>':'</code> separates the path in that 2 parts.
 * Regarding the drive designation of windows, the <code>':'</code>-separator for base and local path
 * is expected right from the 3. position of a String given path. 
 * On 2. position it is the separator after the drive letter:
 * <pre>
 *   D:base/path:local/path/name.ext
 * </pre>  
 * <ul>
 * <li>For example in C-compilation object-files can be stored in sub directories of the objects destination directory 
 *   which follows this local path designation.
 * <li>Another example: copying some files from one directory location to another in designated sub directories.
 * </ul> 
 * <br><br>
 * <b>Common path and access path</b>:<br> 
 * If this file entry is member of a {@link FileSet}, the file set can have a common path.
 * This instance can contain a relative path, an {@link FilePath} instance as access path can determine
 * where the files are located. Therefore some methods works with this 2 arguments. 
 * The separation between the base path and the local path can be found in any of that prefix paths:
 * <pre>
 *   accessPath / commonPath / scriptVariable / localDir
 *                     :
 *                     ^---anywhere of that may have a basePath. All other are act as localDir.                    
 * </pre>
 * Of one of that paths are absolute it determines the path. If the commonPath is absolute, the accessPath is not regarded
 * for that. The most left of the prefix paths uses the {@link FilePathEnvAccess#getCurrentDir()} to build an absolute path
 * if it is required. 
 * <br>
 * <br>
 * <b>Drive letter or select path</b>: On windows systems a drive letter can be used in form <code>A:</code>.
 *   The path should not be absolute. For example <code>"X:mypath\file.ext"</code> describes a file starting from the 
 *   current directory of the <code>X</code> drive. Any drive has its own current directory. A user can use this capability
 *   of windows to set different current directories in special maybe substitute drives. This feature is not supported here
 *   because there is only one current directory. 
 * <br>
 * <br>
 * <b>Drive letter as select path</b>:  
 *   It may be possible (future extension) to use the capability of drive letters independent of windows in this class. 
 *   That should be a feature of {@link FilePathEnvAccess}.
 *   If the path starts with a drive letter, the associated path is searched in the parents drive list. 
 * <br>
 * <br>
 * <b>Absolute path</b>: If this entry starts with slash or backslash, maybe after a drive designation for windows systems,
 *   it is an absolute path. If an absolute path is requested
 *   calling {@link #absfile()} or adequate and the path is not given as absolute path, then the current directory is used
 *   as prefix for the path. The current directory is a property of the {@link FilePathEnvAccess#getCurrentDir()}. 
 *   The current directory of the operation system is not used for that. 
 * <br>
 * <br>
 * <b>operation systems current directory</b>: In opposite if you generate a relative path and the executing system
 *   expects a normal path then it may use the operation system's current directory. But that behavior is outside of this tool.
 * <br>
 * <br>
 * <b>Slash or backslash</b>: The user script can contain slash characters for path directory separation also for windows systems.
 *   It is recommended to use slash. The script which should be generate may expect back slashes on windows systems.
 *   Therefore all methods which returns a path are provided in 2 forms: <b>With "W" on end</b> of there name it is the <b>Windows version</b>
 *   which converts given slash characters in backslash in its return value. So the generated script will contain backslash characters.
 *   Note that some tools in windows accept a separation with slash too. Especial in C-sources an <code>#include <path/file.h></code>
 *   should be written with slash or URLs (hyperlinks) should be written with slash in any case.    
 * <br>
 * <br>
 * <b>Return value of methods</b>: All methods which assembles parts of the path returns a {@link java.lang.CharSequence}.
 *   The instance type of the CharSequence is either {@link java.lang.String} or {@link java.lang.StringBuilder}.
 *   It is not recommended that a user casts the instance type to StringBuilder, then changes it, stores references and
 *   expects that is unchanged. Usual either references to {@link java.lang.CharSequence} or {@link java.lang.String} are expected
 *   as argument type for further processing. If a String is need, one can invoke returnValue.toString(); 
 *   <br><br>
 *   The usage of a {@link java.lang.CharSequence} saves memory space in opposite to concatenate Strings and then return
 *   a new String. In user algorithms it may be recommended to use  {@link java.lang.CharSequence} argument types 
 *   instead {@link java.lang.String} if the reference is not stored permanently but processed immediately.
 *   <br><br> 
 *   If a reference is stored for a longer time in multithreading or in complex algorithms, a {@link java.lang.String}
 *   preserves that the content of the referenced String is unchanged in any case. A {@link java.lang.CharSequence} does not
 *   assert that it is unchanged in any case. Therefore in that case the usage of {@link java.lang.String} is recommended.
 * <br>
 * <br>
 * <b>Handling of Wildcards</b>:<br>
 * The method {@link #expandFiles(List, FilePath, FilePath, FilePathEnvAccess)} resolve wild cards with respect to
 * really found files in a specified file location.
 * <br>
 * In opposite the method {@link #absfileReplwildcard(FilePath, FilePathEnvAccess)} or {@link #localfileReplwildcard(StringBuilder, FilePath)}
 * replaces the given parts of another FilePath on positions of wildcards especially to build a new file.
 * <br>
 * All simple get methods {@link #localfile(FilePathEnvAccess)} etc. returns the wildcards like given.
 * 
 * @author Hartmut Schorrig
 *
 */
public class FilePath
{
  /**Version, history and license.
   * <ul>
   * <li>2019-09-09 Hartmut The JZtxtcmdFileset is important for execution of some JZtxtcmd scripts. 
   *   It was removed in a cleanup action in 2019-04. The cleanup was ok, but this line was too much cleaned.
   *   TODO see 2019-04-09: Should a scriptvariable present a String or other Filepath?
   * <li>2019-04-09: In {@link #FilePath(FilePath, FilePath, FilePath, FilePathEnvAccess)}:
   *   Uses env.getCurrentDir to build an absolute path. 
   *   Reason: Problematic use case detected: If the current dir is changed during exection with a non absolute FilePath, the access will be faulty.
   * <li>2019-04-09: cleanup in {@link #FilePath(FilePath, FilePath, FilePath, FilePathEnvAccess)}:
   *   The scriptvariable cannot be a String or Filepath, it is not supported. Only a FileSet is supported.
   *   It would be a poorly manageable variety (?) TODO Verify this intension on JZtxtcmd script designs. 
   * <li>2017-09-01 Hartmut chg: {@link #expandFiles(List, FilePath, FilePath, FilePathEnvAccess)} now builds a relative path 
   *   if a relative path is given in this. Before: An absolute path was build if wildcards are present. Note: The absolute path
   *   can be gotten by {@link #absfile(FilePathEnvAccess)} etc. on the result Filepaths.
   * <li>2017-09-01 Hartmut new: A FilePath can refer a {@link FileSet} variable or especially a {@link JZtxtcmdFileset} variable.
   *   Then it is not a FileSet but it is a reference to an included FileSet. On {@link FileSet#listFiles(List, FilePath, FilePathEnvAccess, boolean)}
   *   it will be recognized and unpacked. It is on runtime. Note: On script compilation time the variable content may not existent yet.
   *   The variable is only given as String.    
   * <li>2014-06-22 Hartmut note: The possibility of wildcard in the local path (not only "/** /" for allTree) 
   *   is not described, designed and tested completely. 
   * <li>2014-06-22 Hartmut chg: Set all fields private. The fine fragmentation to drive, ... name, ext was done
   *   in 2005 because in the XSLT language it is more simple to concatenate the parts and the ZBNF parser is capable
   *   to fragment. From Java language view it may be better to fragment the given path only in basepath and localpath whereby the basepath
   *   starts with the "D:/" if given and the localpath contains "local/path/name.ext". In preparation the positions of
   *   name, ext can be stored in int-fields. Then it is more simple to return for example the {@link #localfile(FilePathEnvAccess)}
   *   without using a StringBuilder. It saves memory and calculation time. The {@link #name()} is a substring only.
   *   That change can be done later to faster the algorithm. Firstly the class {@link ZbnfFilepath} should be removed.
   *   Therefore it is set to depreciated just now. This class sets the fine graduated parts.
   *   All get methods can be changed maybe without side effects. Because that the fields are private up to now.
   * <li>2014-06-20 Hartmut chg: accessPath: if it contains {@link #someFiles} then use the directory only.
   *   It may be a special case. The usage of wildcard in access path is non constructive. 
   * <li>2014-04-05 Hartmut dissolved from {@link org/vishia/cmd/JZcmdFilepath}, usage more universal.     
   * <li>2014-03-07 Hartmut new: All capabilities from Zmake are joined in JZcmd. Only one concept!
   *   The file {@link org/vishia/cmd/JZcmdFilepath} was copied from former <code>srcJava_Zbnf/org/vishia/zmake/Userfilepath</code>.
   *   The data of a file in the JZcmd context are referenced with {@link org.vishia.cmd.JZtxtcmdFilepath#zgenlevel}. 
   *   The original fields are contained in this class. Both are separated because the parts in JZcmdScript are set completely
   *   by parsing the script. This class contains the access methods which uses the reference to {@link FilePathEnvAccess}
   *   as parameter.
   * <li>2013-03-10 Hartmut new: {@link FileSystem#normalizePath(CharSequence)} called in {@link #absbasepath(CharSequence)}
   *   offers the normalize path for all absolute file paths. 
   * <li>2013-03-10 Hartmut new: Replace wildcard: {@link #absfile(JZtxtcmdFilepath)} (TODO for some more access methods)
   * <li>2013-02-12 Hartmut chg: dissolved from inner class in {@nolink ZmakeUserScript}
   * <li>2012-12-29 Hartmut chg: A {@link FilePath} is independent from a target and describes a non-completely relative path usually.
   *   The path is completed, usual as absolute path, if the {UserFileSet} is used in a target. The {TargetInput} of a target
   *   determines the location of the file set by its {@nolink TargetInput#srcpathInput}. The {@nolink UserFilepath} of all inputs are cloned
   *   and completed with that srcpath for usage. The {@nolink UserTarget#allInputFiles()} or {@nolink UserTarget#allInputFilesExpanded()}
   *   builds that list. Usage of that files provide the correct source path for the files of a target's input.
   * <li>2012-12-08 Hartmut chg: improve access rotoutines.
   * <li>2012-11-29 Hartmut new: The ZmakeUserScript.UserFilePath now contains all access routines to parts of the file path
   *   which were contained in the ZmakeGenerator class before. This allows to use this with the common concept
   *   of a text generator using org.vishia.zTextGen.TextGenerator. That Textgenerator does not know such
   *   file parts and this UserFilepath class because it is more universal. Access is possible 
   *   with that access methods {@link FilePath#localfile()} etc. To implement that a UserFilepath
   *   knows its file-set where it is contained in, that is {@link UserFilepath#itsFileset}. 
   *   The UserFileset#script knows the Zmake generation script. In this kind it is possible to use the
   *   common data of a file set and the absolute path.
   * <li>2011-02-20 Hartmut Re-engineering of Zmake, class ZmakeUserScript. The basic was the XML implementation of zmake. 
   *   The content of this user script was present in an XML tree after parsing instead in this java class. 
   *   It was processed with a XSLT translator. The new Zmake concept is attempt to use Java and a textual generation 
   *   from java data instead XSLT, it is more flexible.
   * <li>2005 Hartmut creation of this class in an XML environment parsed with ZBNF, used for generation of ANT-Zmake-scripts.
   * </ul>
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
   static final public String sVersion = "2019-09-09";

  /**An implementation of this interface should be provided by the user if absolute paths and script variables should be used. 
   * It may be a short simple implementation if that features are unused. See the {@link org.vishia.util.test.Test_FilePath}. 
   * @author Hartmut Schorrig
   *
   */
  public interface FilePathEnvAccess {
    
    /**Returns the Object which is addressed by the name. It may be a script variable, able to find in one
     * or more container sorted by name. 
     * @param variable The name
     * @return either an instance of FilePath or a CharSequence or null if the variable is not found (optional, instead Exception). 
     * @throws NoSuchFieldException if the variable is not found (optional, recommended).
     */
    Object getValue(String variable) throws NoSuchFieldException;
    
    /**Returns the current directory of the context necessary to build an absolute path in {@link FilePath}.
     */
    CharSequence getCurrentDir();
  }
  
  
  
  /**If given, then the basePath() starts with it. 
   */
  private final String scriptVariable;
  
  /**Only for running instance created with {@link FilePath#FilePath(FilePath, FilePath, FilePath, FilePathEnvAccess)}:
   * Another Filepath which acts as basepath. The {@link #scriptVariable} is null. It is the content of the scriptVariable on runtime.
   */
  private final FilePath varFilePath;
  
  /**Only for running instance created with {@link FilePath#FilePath(FilePath, FilePath, FilePath, FilePathEnvAccess)}:
   * A charSequence as basepath. The {@link #scriptVariable} is null. It is the content of the scriptVariable on runtime.
   */
  private final CharSequence varChars;
  
  /**Only for running instance created with {@link FilePath#FilePath(FilePath, FilePath, FilePath, FilePathEnvAccess)}:
   * Another Fileset instead an single FilePath. The {@link #scriptVariable} is null. It is the content of the scriptVariable on runtime.
   */
  private final FileSet varFileset;
  
  /**The drive letter if a drive is given. */
  private String drive;
  
  /**Set if the path starts with '/' or '\' maybe after drive letter. */
  private boolean absPath;
  
  /**Base path-part before a ':' in String-given path. It is null if the basepath is not given. */
  private String basepath;
  
  /**Localpath after ':' or the whole path. It is an empty "" if a directory is not given. 
   * It does not contain a slash on end. */
  private String localdir = "";
  
  /**From Zbnf: The filename without extension. Note: On a "." as only one first char (".git" or such) it is the name, not the extension. */
  private String name = "";
  
  
  /**From Zbnf: The extension inclusive the leading dot. */
  private String ext = "";
  
  /**Set to true if a "*" was found in any directory part.*/
  private boolean allTree;
  
  /**Set to true if a "*" was found in the name or extension.*/
  private boolean someFiles;
  
  
  /**An empty file path which is used as argument if a common base path is not given. */
  private static FilePath emptyParent = new FilePath();
  


  /**Empty instance. */
  private FilePath(){
    scriptVariable = null; varFilePath = null; varChars = null; varFileset = null;
  }
  

  
  /**parses the string given path. The path is split in its parts.
   * <pre>
   * D:/base/path:local/path.name.ext
   * </pre>
   * <ul>
   * <li>One can use '/' or '\' as path separator. Internally the '/' is stored.
   * <li>If a ':' is found on charAt(1) the character before is the drive letter.
   * <li>If a '/' on first position or after the second ':': it is an absolute path.
   * <li>A ':' not on second position: The separator between base path and local path.
   * <li>The extension is the part inclusive the last dot.
   * <li>A simple '.' or '..' is stored as the name 
   * </ul> 
   * The syntax may be described in ZBNF-Form. This syntax was used in the originally XML presentation from 2005: 
   * <pre>
 prepFilePath::=<$NoWhiteSpaces><! *?>
 [ &<$?@scriptVariable> [\\|/|]   ##path can start with a scriptvariable's content
 | [<!.?@drive>:]                 ## only 1 char with followed : is the drive letter
   [ [/|\\]<?@absPath>]           ## starting with / maybe after d: is absolute path
 |]
 [ <*:?@pathbase>[?:=]:]            ## all until : is pathbase, but not till a :=
 [ <toLastChar:/\\?@path>[\\|/|]] ## all until last \ or / is path
 [ <toLastChar:.?@name>             ## all until exclusive dot is the name
   <*\e?@ext>                      ## from dot to end is the extension
 | <*\e?@name>                     ## No dot is found, all is the name.
 ] . 
   * </pre>
   * But the parsing is done with java basics (indexOf etc.)
   */
  public FilePath(String pathP){
    String path = pathP.replace('\\', '/');
    int zpath = path.length();
    int posColon = path.indexOf(':');
    int pos1slash = path.indexOf('/');
    int posbase;   //start of base path, ==poslocal if not given
    int poslocal;  //start of local path or whole path, after ':' or after root-/ 
    int pos1;
    if(zpath >=1 && path.charAt(0) == '&'){ //starts with a script variable:
      int pos9 = posColon > 0 && (posColon < pos1slash || pos1slash < 0) ? posColon : pos1slash > 0 ? pos1slash : zpath;
      this.scriptVariable = path.substring(1, pos9); //access it on runtime, not on preparation time.
      varFilePath = null; varChars = null; varFileset = null;  //set only in the running version.
      absPath = false;  //hint: it may be an absolute path depending of content of scriptVariable 
      if(pos9 == pos1slash){
        pos1 = pos9 +1;   //rest of path starts after slash as separator. A colon may be found behind.
      } else {
        pos1 = pos9;      //rest of path starts on colon, it is an empty base path
      }
    } else if(posColon == 1){ //it means a ':' is found anywhere: check if it is a drive designation
      drive = path.substring(0,1);
      posColon = path.indexOf(':', 2);
      absPath = pos1slash == 2;
      pos1 = absPath ? 3 : 2;
      scriptVariable = null; varFilePath = null; varChars = null; varFileset = null;
    } else { //no variable, no drive
      absPath = pos1slash == 0;
      pos1 = absPath ? 1 : 0;
      scriptVariable = null; varFilePath = null; varChars = null; varFileset = null;
    }
    if(posColon >0){
      posbase = pos1;
      poslocal = posColon+1;
    } else {
      posbase = -1;
      poslocal = pos1;
    }
    //drive, absPath is set.
    //posbase, poslocal is set.
    //
    if(posbase >=0){
      basepath = path.substring(posbase, poslocal-1);  //may be ""
    } else { 
      basepath = null; // left empty
    }
    int posname = path.lastIndexOf('/') +1;
    if(posname < poslocal){ posname = poslocal; }
    //
    if(posname > poslocal){
      localdir = path.substring(poslocal, posname-1);
      allTree = localdir.indexOf("/**/")>=0;
      someFiles = localdir.indexOf('*') >=0;
    } else {
      localdir = "";
    }
    int posext = path.lastIndexOf('.');
    if(posext <= posname){  //not found, or any '.' before start of name
      posext = zpath;  //no extension.
    } 
    name = path.substring(posname, posext);
    someFiles |= name.indexOf('*')>=0;
    ext = path.substring(posext);  //with "."
    someFiles |= ext.indexOf('*')>=0;
    if(posname +1 == posext && posname +2 == zpath && path.charAt(posname) == '.'){
      //special form.
      name = "..";
      ext = "";
    }
  }
  
  
  
  
  /**Creates a new FilePath from a given FilePath with possible given common and access path or an environment variable.
   * It builds an absolute path in any kind using the env {@link FilePathEnvAccess#getCurrentDir()}.
   * This method is used especially to build a new set of FilePath from a given set
   * with common and access paths and maybe variables. The variables are resolved all
   * and the relation between base and local parts in all components are resolved too,
   * so the new FilePath is simple to access.
   * This method is used in {@link org.vishia.cmd.JZtxtcmdFileset#listFiles(org.vishia.cmd.JZtxtcmdFilepath, boolean)}.
   * <br>
   * <b>Scriptvariable in src: </b><br>
   * If src contains a {@link FilePath#scriptVariable} it can refer another {@link FileSet} resp. {@link JZtxtcmdFileset} 
   * which is stored in {@link #varFileset} to evaluate in {@link FileSet#listFiles(List, FilePath, FilePathEnvAccess, boolean)}
   * with this given FilePath with recursive call of {@link FileSet#listFiles(List, FilePath, FilePathEnvAccess, boolean)}
   * to evaluate it too.
   * @since 2019-04: The scriptvariable cannot be a String or Filepath, it is not supported. Only a FileSet is supported.
   *   It would be a poorly manageable variety (?)  
   * 
   * @param src Any given FilePath, usual member of a {@link FileSet}.
   *   This src is used but completed with the maybe here given common and access path. 
   * @param commonPath A common path of this FilePath enhances the local or given base part of FilePath
   * @param accessPath An access path enhances the given local or base part
   * @param env To resolve variables and access to the currDir.
   * @throws NoSuchFieldException if a variable is not found.
   * @since 2019-03-31: Uses env.getCurrentDir to build an absolute path. 
   *   Reason: Problematic use case detected: If the current dir is changed during exection with a non absolute FilePath, the access will be faulty.
   */
  public FilePath(FilePath src, FilePath commonPath, FilePath accessPath, FilePathEnvAccess env) 
  throws NoSuchFieldException {
    FilePath commonFilePath = commonPath !=null ? commonPath : null;
    FilePath accessFilePath = accessPath !=null ? accessPath : null;
    Object oValue = null;
    this.scriptVariable = null; this.varFilePath = null; this.varChars = null; 
    if(src.scriptVariable !=null){
      oValue = env.getValue(src.scriptVariable);
      if(oValue == null) throw new NoSuchFieldException("FilePath.basepath - scriptVariable not found; "+ src.scriptVariable);
      if(oValue instanceof FileSet){
        this.varFileset = (FileSet)oValue;
        oValue = null; //processed
      } else if(oValue instanceof JZtxtcmdFileset) { //Note: has an aggregation to a FileSet
        this.varFileset = ((JZtxtcmdFileset)oValue).data.fileset;
      } else {
        this.varFileset = null;
      }
      
    } else { 
      //src.scriptVariable not set:
      this.varFileset = null;
    }
    if(this.varFileset !=null) {
      drive = null; absPath = false; basepath = null; localdir = null; name = null; ext = null; allTree = false; someFiles = false;
    } else {
    
      CharSequence basePath = src.basepath(null, commonFilePath, accessFilePath, oValue, env);
      CharSequence localDir = src.localdir(null, commonFilePath, accessFilePath, oValue, env);
      int posbase = FilePath.isRootpath(basePath);
      this.drive = posbase >=2 ? Character.toString(basePath.charAt(0)) : null;
      this.absPath = posbase == 1 || posbase == 3;
      this.basepath = basePath.subSequence(posbase, basePath.length()).toString();
//      CharSequence currDir = null;
//      if(posbase !=3) {
//        currDir = env.getCurrentDir();
//        switch(posbase) {
//        case 0: 
//          this.drive = "" + currDir.charAt(0);
//          this.basepath = currDir.subSequence(2, currDir.length()) + "/" + basePath;
//          break;
//        case 1:
//          this.drive = "" + currDir.charAt(0);
//          this.basepath = basePath.toString();
//          break;
//        case 2:
//          this.drive = "" + basePath.charAt(0);
//          this.basepath = currDir.subSequence(2, currDir.length()) + "/" + basePath.subSequence(posbase, basePath.length());
//        } 
//        
//      } else {
//        this.drive = Character.toString(basePath.charAt(0));
//        this.basepath = basePath.subSequence(2, basePath.length()).toString();
//      }
//      this.absPath = true;
      this.localdir = localDir.toString();
      if(!localdir.endsWith("/"))
        Assert.stop();
      else
        Assert.stop();
      this.name = src.name;
      this.ext = src.ext;
      this.allTree = localdir.indexOf('*') >=0;
      this.someFiles = src.someFiles;
    }
  }
  
  
  
  
  
  
  
  
  /**An empty instance has not a localdir nor basepath nor name nor drive.
   * @return false if nothing is given, true if at least one element is given which determines a path.
   */
  public boolean isNotEmpty(){
    return basepath !=null || localdir.length() >0 || name.length() >0 || drive !=null
    || scriptVariable !=null || varFilePath !=null || varChars !=null ;
      
  }
  
  
  
  /**Special case: a FilePath contains a scriptVariable which refers a FileSet.
   * This operation is only sensible if the instance was construct with {@link FilePath#FilePath(FilePath, FilePath, FilePath, FilePathEnvAccess)}
   * specially used in {@link FileSet#listFiles(List, FilePath, FilePathEnvAccess, boolean)}.
   * @return null or the FileSet.
   */
  public FileSet isFileSet() { return varFileset; }
  
  
  /**It should return the input String of {@link #FilePath(String)}. Only used for debug view.
   * @see java.lang.Object#toString()
   */
  @Override public String toString() {
    StringBuilder u = new StringBuilder();
    if(scriptVariable !=null){ u.append('&').append(scriptVariable).append(" "); }
    if(drive!=null) { u.append(drive).append(':'); }
    if(absPath) { u.append("/"); }
    if(basepath!=null) { u.append(basepath).append(":"); }
    if(localdir.length()>0) { u.append(localdir).append("/"); }
    u.append(name).append(ext);
    return u.toString();
  }
  

  
  
  
  /**Returns the local directory path part. This method is usefully if another file path should be built 
   * but with the same local directory part. Usual {@link #localname(FilePathEnvAccess)} may be more usefully. 
   * @param env Access to the environment to resolve variables.
   * @throws NoSuchFieldException if a {@link #scriptVariable} is used and it is not found in the context. 
   */
  public CharSequence localdir(FilePathEnvAccess env) throws NoSuchFieldException{
    CharSequence ret = localdir(null, null, null, env);
    if(ret.length() == 0){ return "."; }
    else return ret;
  }



  /**Returns the local directory path part for Windows environment with backslash as separator.
   * It wraps {@link #localdir(FilePathEnvAccess)}, see there.
   */
  public CharSequence localdirW(FilePathEnvAccess env) throws NoSuchFieldException{ return toWindows(localdir(env)); }



  /**Returns the local file path part without extension. This method is usefully if another file path should be built 
   * with the same local file part but another absolute directory and another extension. 
   * That is the usual usefully if destination files should be build from given source files.
   * @param env Access to the environment to resolve variables.
   * @throws NoSuchFieldException if a {@link #scriptVariable} is used and it is not found in the context. 
   */
  public CharSequence localname(FilePathEnvAccess env) throws NoSuchFieldException{ 
    StringBuilder uRet = new StringBuilder();
    return addLocalName(uRet, env); 
  }



  /**Returns the local file path part without extension for Windows environment with backslash as separator.
   * It wraps {@link #localname(FilePathEnvAccess)}, see there.
   */
  public CharSequence localnameW(FilePathEnvAccess env) throws NoSuchFieldException{ return toWindows(localname(env)); }



  /**Returns the local file path part. This method is usefully if another file path should be built 
   * with the same local file path part but another absolute directory. 
   * That is the usual usefully if a file with the same name and local directory should be copied to another location.
   * @param env Access to the environment to resolve variables.
   * @throws NoSuchFieldException if a {@link #scriptVariable} is used and it is not found in the context. 
   */
  public CharSequence localfile(FilePathEnvAccess env) throws NoSuchFieldException{ 
    StringBuilder uRet = new StringBuilder();
    addLocalName(uRet, env);
    uRet.append(this.ext);
    return uRet;
  }



  /**Returns the local file path part for Windows environment with backslash as separator.
   * It wraps {@link #localfile(FilePathEnvAccess)}, see there.
   */
  public CharSequence localfileW(FilePathEnvAccess env) throws NoSuchFieldException{ return toWindows(localfile(env)); }



  /**Returns the name of the file without extension.
   * Note: If the file starts with ".name" this is the name, not the extension.
   */
  public CharSequence name(){ return this.name; }



  /**Returns the name of the file inclusively the extension.
   */
  public CharSequence namext(){ 
    StringBuilder uRet = new StringBuilder(); 
    uRet.append(this.name);
    uRet.append(this.ext);
    return uRet;
  }



  /**Returns the extension of the file inclusively the dot on start.
   */
  public String ext(){ return this.ext; }


  /**Return true if the local path, name or extension contains a wildcard.
   * @return false if name and extension are given without wildcard. 
   */
  public boolean someFiles(){ return someFiles; }
  
  
  /**Return true if the local path contains a <code>"/** /"</code> which means, all files in the directory tree
   * should be used.
   */
  public boolean allTree(){ return allTree; }
  

  /**Returns the base path part as absolute path. It does not end with a '/'. 
   * A current directory is gotten via {@link FilePathEnvAccess#getCurrentDir()}
   * and not from the systems current directory. 
   * @param env Access to the environment to get the current directory and to resolve variables.
   * @throws NoSuchFieldException if a {@link #scriptVariable} is used and it is not found in the context. 
   *  
   */
  public CharSequence absbasepath(FilePathEnvAccess env) throws NoSuchFieldException { 
    CharSequence sBasepath = basepath(null, emptyParent, null, env);
    return absbasepath(sBasepath, env);
  }
  
  /**Returns the base path part as absolute path for Windows environment with backslash as separator.
   * It wraps {@link #absbasepath(FilePathEnvAccess)}, see there.
   */
  public CharSequence absbasepathW(FilePathEnvAccess env) throws NoSuchFieldException { return toWindows(absbasepath(env)); }
  

  
  /**Returns the directory part as absolute path. It does not end with a '/'. 
   * A current directory is gotten via {@link FilePathEnvAccess#getCurrentDir()}
   * and not from the systems current directory. 
   * @param env Access to the environment to get the current directory and to resolve variables.
   * @throws NoSuchFieldException if a {@link #scriptVariable} is used and it is not found in the context. 
   *  
   */
  public CharSequence absdir(FilePathEnvAccess env) throws NoSuchFieldException  { 
    CharSequence basePath = absbasepath(env);
    StringBuilder uRet = basePath instanceof StringBuilder ? (StringBuilder)basePath : new StringBuilder(basePath);
    return localdir(uRet, null, null, env);
  }
  
  /**Returns the directory part as absolute path for Windows environment with backslash as separator.
   * It wraps {@link #absdir(FilePathEnvAccess)}, see there.
   */
  public CharSequence absdirW(FilePathEnvAccess env) throws NoSuchFieldException{ return toWindows(absdir(env)); }
  
  
  /**Returns the file path but without extension as absolute path. It does not end with a '.'. 
   * The extension is the last part of the filename after the last dot including the dot.
   * Usual new files are build with the same name but with other extension. Therefore this method is given.
   * A current directory is gotten via {@link FilePathEnvAccess#getCurrentDir()}
   * and not from the systems current directory. 
   * @param env Access to the environment to get the current directory and to resolve variables.
   * @throws NoSuchFieldException if a {@link #scriptVariable} is used and it is not found in the context. 
   */
  public CharSequence absname(FilePathEnvAccess env) throws NoSuchFieldException{ 
    CharSequence basePath = absbasepath(env);
    StringBuilder uRet = basePath instanceof StringBuilder ? (StringBuilder)basePath : new StringBuilder(basePath);
    int pos;
    if( (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
    uRet.append(this.localdir);
    if( (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
    uRet.append(this.name);
    return uRet;
  }
  
  /**Returns the file path but without extension as absolute path for Windows environment with backslash as separator.
   * It wraps {@link #absname(FilePathEnvAccess)}, see there.
   */
  public CharSequence absnameW(FilePathEnvAccess env) throws NoSuchFieldException{ return toWindows(absname(env)); }
  


  
  /**Returns the complete file path as absolute path. 
   * A current directory is gotten via {@link FilePathEnvAccess#getCurrentDir()}
   * and not from the systems current directory. 
   * @param env Access to the environment to get the current directory and to resolve variables.
   * @throws NoSuchFieldException if a {@link #scriptVariable} is used and it is not found in the context. 
   */
  public CharSequence absfile(FilePathEnvAccess env) throws NoSuchFieldException{ 
    CharSequence basePath = absbasepath(env);
    StringBuilder uRet = basePath instanceof StringBuilder ? (StringBuilder)basePath : new StringBuilder(basePath);
    addLocalName(uRet);
    uRet.append(this.ext);
    return uRet;
  }
  
  /**Returns the complete file path as absolute path for Windows environment with backslash as separator.
   * It wraps {@link #absfile(FilePathEnvAccess)}, see there.
   */
  public CharSequence absfileW(FilePathEnvAccess env) throws NoSuchFieldException{ return toWindows(absfile(env)); }
  
  
  /**Returns the local file with replaced wildcard in the local dir. See {@link #addLocalNameReplwildcard(StringBuilder, FilePath).
   * @param replWildc With them localdir and name a wildcard in this.localdir and this.name is replaced.
   * @return the whole path inclusive a given general path .
   *   The path is absolute. If it is given as relative path, the general current directory of the script is used.
   * @throws NoSuchFieldException 
   */
  public CharSequence absfileReplwildcard(FilePath replWildc, FilePathEnvAccess env) throws NoSuchFieldException{ 
    CharSequence basePath = absbasepath(env);
    StringBuilder uRet = basePath instanceof StringBuilder ? (StringBuilder)basePath : new StringBuilder(basePath);
    localfileReplwildcard(uRet, replWildc);
    //uRet.append(this.ext);
    return uRet;
  }
  
  /**Returns the base path part like given, either as absolute path or relative path. 
   * @param env Access to the environment to resolve variables.
   * @throws NoSuchFieldException if a {@link #scriptVariable} is used and it is not found in the context. 
   */
  public CharSequence basepath(FilePathEnvAccess env) throws NoSuchFieldException{ return basepath(null, emptyParent, null, env); }
   
  

  
  /**Returns the base path part like given for Windows environment with backslash as separator.
   * It wraps {@link #basepath(FilePathEnvAccess)}, see there.
   */
  public CharSequence basepathW(FilePathEnvAccess env) throws NoSuchFieldException{ return toWindows(basepath(env)); }
  
  
  
  /**Returns the directory part like given, either as absolute path or relative path. 
   * @param env Access to the environment to resolve variables.
   * @throws NoSuchFieldException if a {@link #scriptVariable} is used and it is not found in the context. 
   */
  public CharSequence dir(FilePathEnvAccess env) throws NoSuchFieldException{ 
    CharSequence basePath = basepath(env);
    StringBuilder uRet = basePath instanceof StringBuilder ? (StringBuilder)basePath : new StringBuilder(basePath);
    int zpath = (this.localdir == null) ? 0 : this.localdir.length();
    if(zpath > 0){
      int pos;
      if( (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
      uRet.append(this.localdir);
    }
    return uRet;
  }

  
  
  /**Returns the directory part like given for Windows environment with backslash as separator.
   * It wraps {@link #dir(FilePathEnvAccess)}, see there.
   */
  public CharSequence dirW(FilePathEnvAccess env) throws NoSuchFieldException{ return toWindows(dir(env)); }
  
  /**Returns the file path without extension like given, either as absolute path or relative path. 
   * @param env Access to the environment to resolve variables.
   * @throws NoSuchFieldException if a {@link #scriptVariable} is used and it is not found in the context. 
   */
  public CharSequence pathname(FilePathEnvAccess env) throws NoSuchFieldException{ 
    CharSequence basePath = basepath(env);
    StringBuilder uRet = basePath instanceof StringBuilder ? (StringBuilder)basePath : new StringBuilder(basePath);
    int pos;
    if( (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
    uRet.append(this.localdir);
    if( (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
    uRet.append(this.name);
    return uRet;
  }
  
  /**Returns the file path without extension like given for Windows environment with backslash as separator.
   * It wraps {@link #pathname(FilePathEnvAccess)}, see there.
   */
  public CharSequence pathnameW(FilePathEnvAccess env) throws NoSuchFieldException{ return toWindows(pathname(env)); }
  


  
  /**Returns the file path like given, either as absolute path or relative path. 
   * @param env Access to the environment to resolve variables.
   * @throws NoSuchFieldException if a {@link #scriptVariable} is used and it is not found in the context. 
   */
  public CharSequence file(FilePathEnvAccess env) throws NoSuchFieldException{ 
    CharSequence basePath = basepath(env);
    StringBuilder uRet = basePath instanceof StringBuilder ? (StringBuilder)basePath : new StringBuilder(basePath);
    addLocalName(uRet);
    return uRet.append(this.ext);
  }
  
  /**Returns the file path like given for Windows environment with backslash as separator.
   * It wraps {@link #file(FilePathEnvAccess)}, see there.
   */
  public CharSequence fileW(FilePathEnvAccess env) throws NoSuchFieldException{ return toWindows(file(env)); }
  
  
  
  /**Returns the base path and the local dir like given with ':' as separator between both parts. 
   * @param env Access to the environment to resolve variables.
   * @throws NoSuchFieldException if a {@link #scriptVariable} is used and it is not found in the context. 
   */
  public CharSequence base_localdir(FilePathEnvAccess env) throws NoSuchFieldException{ 
    CharSequence basePath = basepath(env);
    StringBuilder uRet = basePath instanceof StringBuilder ? (StringBuilder)basePath : new StringBuilder(basePath);
    if( uRet.length() >0){ uRet.append(":"); }
    uRet.append(this.localdir);
    return uRet;
  }
  
  /**Returns the base path and the local dir like given with ':' as separator between both parts
   * for Windows environment with backslash as separator.
   * It wraps {@link #base_localdir(FilePathEnvAccess)}, see there.
   */
  public CharSequence base_localdirW(FilePathEnvAccess env) throws NoSuchFieldException{ return toWindows(base_localdir(env)); }
  
  
  /**Returns the base path and the local file like given with ':' as separator between both parts. 
   * @param env Access to the environment to resolve variables.
   * @throws NoSuchFieldException if a {@link #scriptVariable} is used and it is not found in the context. 
   */
  public CharSequence base_localfile(FilePathEnvAccess env) throws NoSuchFieldException{ 
    CharSequence basePath = basepath(env);
    StringBuilder uRet = basePath instanceof StringBuilder ? (StringBuilder)basePath : new StringBuilder(basePath);
    if( uRet.length() >0){ uRet.append(":"); }
    uRet.append(this.localdir);
    int pos;
    if( (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
    uRet.append(this.name);
    uRet.append(this.ext);
    return uRet;
  }
  
  /**Returns the base path and the local file like given with ':' as separator between both parts
   * for Windows environment with backslash as separator.
   * It wraps {@link #base_localdir(FilePathEnvAccess)}, see there.
   */
  public CharSequence base_localfileW(FilePathEnvAccess env) throws NoSuchFieldException{ return toWindows(base_localfile(env)); }
  
  
  

  /**Converts a given path to the windows presentation with backslash.
   * @param inp It is is a StringBuilder, it is used and its content is changed.
   * @return either inp if it is a StringBuilder, or a new unmated StringBuilder instance.
   */
  public static CharSequence toWindows(CharSequence inp)
  {
    if(inp instanceof StringBuilder){
      StringBuilder uRet = (StringBuilder)inp;
      for(int ii=0; ii<inp.length(); ++ii){
        if(uRet.charAt(ii)=='/'){ uRet.setCharAt(ii, '\\'); }
      }
      return uRet;
    }
    else { //it is String!
      return ((String)inp).replace('/', '\\');
    }
  }
  
  /**Builds non-wildcard instances for any found file, add the given common and access path. 
   * Adds all these instances to the given list.
   * If a file without wildcards is given, it is added always independent of its existing on the file system.
   * This may be usefully because the absolute path can be checked to evaluate why instances with wildcards may not able to found.
   * If a file with wildcards is given the file system is used to get all instances. If a proper instance is not found,
   * nothing will be added.
   * The expansion with wild-cards is a capability of {@link FileSystem#addFilesWithBasePath(File, String, List)}
   * which is used here as core routine.
   * <br><br>
   * The possible given commonPath and accessPath will be resolved. The absolute base path will be gotten from that,
   * see {@link #basepath(StringBuilder, FilePath, FilePath, boolean[], FilePathEnvAccess)}.
   * The result instances of FilePath will be contain the resulting base path maybe as absolute or relative one.
   * <br><br>
   * The instances from given FilePath with wild-cards will be gotten from the currently content of the file system
   * using the absolute path from {@link FilePathEnvAccess#getCurrentDir()}. 
   * That current dir will be set in the created Filepath-instances, therewith they are absolute. 
   * But note that the given local path build maybe from the commonPath or accessPath too will be gotten from the source.
   * <br><br>
   * For example the accessPath contains "myDir:subdir", this file contains <code>"*.*"</code> 
   * and the env.getCurrentDir is <code>"D:/dir"</code>.
   * Then the basepath is <code>"D:/dir/myDir"</code> and all files have a localPath <code>"subdir/...."</code>. 
   * 
   * @param listToadd List which will be completed with all found files
   * @param commonPath if not null, the path before this given file path. 
   * @param accessPath if not null, the path before a commonPath and before this given file path.
   * @throws NoSuchFieldException 
   */
  public void expandFiles(List<FilePath> listToadd, FilePath commonPath, FilePath accessPath, FilePathEnvAccess env) throws NoSuchFieldException{
    final CharSequence basePathChildren;
    final CharSequence localfilePath = this.localfile(null, commonPath, accessPath, env); //getPartsFromFilepath(file, null, "file").toString();
    final String driveChildren;
    final boolean absPathChildren;
    final String sBasePathChildren;  
    final String sPathSearch;
    int posLocalPath = isRootpath(localfilePath);
    if(posLocalPath ==1 || posLocalPath ==3) {  //absolute Path given in localPath
      driveChildren = posLocalPath == 1 ? "" : String.valueOf(localfilePath.charAt(0));
      absPathChildren = true;
      sBasePathChildren = "";  //it is not relevant, maybe given.
      sPathSearch = absbasepath(localfilePath, env).toString();
    } else {
      basePathChildren = basepath(new StringBuilder(), commonPath, accessPath, env);
      int posBasePath = isRootpath(basePathChildren);
      absPathChildren = (posBasePath ==1 || posBasePath ==3);  //absolute Path given in basePath
      if(posLocalPath >= 2) {
        driveChildren = String.valueOf(localfilePath.charAt(0));  //drive in localpath determines.
      } else {
        driveChildren = "";
      }
      sBasePathChildren = basePathChildren.subSequence(posBasePath, basePathChildren.length()).toString();
      sPathSearch = absbasepath(basePathChildren, env) + ":" + localfilePath;
    }
    //
   
    /*
    absPathChildren = true;
    if(absBasepath.charAt(1)==':'){
      driveChildren = String.valueOf(absBasepath.charAt(0));
      sBasePathChildren = absBasepath.subSequence(3, absBasepath.length()).toString();
    } else { 
      //only on unix systems.
      driveChildren = null;  
      sBasePathChildren = absBasepath.subSequence(1, absBasepath.length()).toString();
    }
    */
    //
    List<FileSystem.FileAndBasePath> listFiles = new LinkedList<FileSystem.FileAndBasePath>();
    try{ FileSystem.addFilesWithBasePath(null, sPathSearch, listFiles);
    } catch(Exception exc){
      //let it empty. Files may not be found.
    }
    for(FileSystem.FileAndBasePath file1: listFiles){
      //Build a new instance for any found file
      //with the same structure like the given input paths.
      //
      FilePath filepath2 = new FilePath();  //new instance for found file.
      filepath2.absPath = absPathChildren;
      filepath2.drive = driveChildren;
      filepath2.basepath = sBasePathChildren;  //it is the same. Maybe null
      int posName = file1.localPath.lastIndexOf('/') +1;  //if not found, set to 0
      int posExt = file1.localPath.lastIndexOf('.');
      if(posExt < posName){ 
        posExt = -1; }        //no extension given.  
      final String sPath = posName >0 ? file1.localPath.substring(0, posName-1) : "";  //"" if only name
      final String sName;
      final String sExt;
      if(posExt < 0){ sExt = ""; sName = file1.localPath.substring(posName); }
      else { sExt = file1.localPath.substring(posExt); sName = file1.localPath.substring(posName, posExt); }
      filepath2.localdir = sPath;
      assert(!sPath.endsWith("/"));
      filepath2.name = sName;
      filepath2.ext = sExt;
      listToadd.add(filepath2);
    }

  }
  

  
  
  
  /**Checks whether the given path describes a root directory or drive letter.
   * returns
   * <ul>
   * <li>0 if it is a relative path.
   * <li>1 if it is a absolute path without drive letter: "/path" or "\path"
   * <li>2 if it is a relative path with a drive letter: "D:path"
   * <li>3 if it is an absolute path with a drive letter: "D:/path" or "D:\path"
   * </ul>
   * The return value is the start of the non-root path part in textPath.
   * If return=2 or 3, the drive letter is textPath.charAt(0).
   * @param textPath
   * @return 
   */
  public static int isRootpath(CharSequence textPath){  ///
    int start;
    if(textPath.length() >=1 && "\\/".indexOf(textPath.charAt(0)) >=0 ){
      start =1;
    } 
    else if(textPath.length() >=2 && textPath.charAt(1) == ':'){
      if(textPath.length() >=3 &&  "\\/".indexOf(textPath.charAt(2)) >=0 ){
        start = 3;
      } else {
        start = 2;
      }
    } else {
      start = 0;  //relative path
    }
    return start;
  }
  

  
  
  
  
  
  /**Gets the base path part of this. This method regards the existence of a common and an access path.
   * The common path may be given by a {@link UserFileset#commonBasepath}. The access path may be given 
   * by application of a fileset in a {@link UserTarget} especially while preparing the input files or files
   * for parameter in {@link UserTarget#prepareFiles(List, boolean)}.
   * This is the substantial method to build the basepath

   * <br><br>
   * <ul>
   * <li>If this file contains a basepath, all other access, common and a variable is used as full file path
   *   as prefix of this base path.
   * <li>If this does contain a variable but does not contain a base path, the variable's base path is used as base path
   *   and the variable's local path is the prefix for the local path.    
   * <li>If this does not contain a basepath, a given common and access path is checked whether it contains 
   *   a base path. Then that basepath is used.
   * <li>If a given common or access path does not contain a basepath, the common and access path is used
   *   as base path.
   * <li>This behavior is complementary to the behavior of {@link #localDir(StringBuilder, FilePath, FilePath)}.
   * </ul>
   * <br><br>
   * The following true table shows the constellation possibilities and there outputs.
   * <ul>
   * <li>common: represents the common and/or the access path.
   * <li>varfile: represents a {@link #scriptVariable} maybe with a {@link ScriptVariable#filepath}
   *   or the textual content of a {@link #scriptVariable}.
   *   Only a {@link ScriptVariable#filepath} can represent a <code>base</code>.
   *   An textual variable can represent a <code>abs</code>, it is checked with {@link #isRootpath(CharSequence)}.
   * <li>The common or variable reference can be given (1) or it is null (0).
   * <li>base: The element has a basepath and a local part.
   * <li>abs: The element is given as absolute path
   * <li>x means, it does not decide.
   * <li>1 means, it is given.
   * <li>0 means, it is not given.
   * </ul>
   * Results:
   * <ul>
   * <li>/: Absolute path maybe with drive letter
   * <li>thisBase, varBase, commonBase: Use the base part of the element.
   * <li>varFile, commonFile: Use the whole path of the element.
   * </ul> 
   * <pre>
   *  common    variable    this         basepath build with         : localdir build with   
   *  | base    | base abs  base abs
   *  x  x      x  x   x     1    1      /thisBase                   : thisLocal
   *  x  x      x  x   x     0    1      "/"                         : thisLocal
   *  
   *  x         1  x   1     1    0      /varFile + thisBase         : thisLocal
   *  0         1  x   0     1    0      varFile + thisBase
   *  1  x      1  x   0     1    0      commonFile + varFile + thisBase : thisLocal
   *  0         0            1    0      thisBase                    : thisLocal
   *  1  x      0            1    0      commonFile + thisBase       : thisLocal
 
   *  x  x      1  1   1     0    0      /variableBase               : varLocalFile + thisLocal

   *  0  x      1  1   0     0    0      variableBase                : varLocalFile + thisLocal
   *  1  x      1  1   0     0    0      commonFile + variableBase   : varLocalFile + thisLocal
   *    
   *  x  x      1  0   1     0    0      /                           : varLocalFile + thisLocal
  
   *  0  x      1  0   0     0    0      ""                          : varLocalFile + thisLocal
   *  1  0      1  0   0     0    0      commonFile                  : varLocalFile + thisLocal
   *  1  1      1  0   0     0    0      commonBase                  : varLocalFile + thisLocal
   *    
   *  0  x      0            0    0      ""                          : thisLocal
   *  1  0      0            0    0      commonFile                  : thisLocal
   *  1  1      0            0    0      commonBase                  : commonLocalfile + thisLocal
   * </pre>
   * To implement this true table with less tests the algorithm is recursive. If this has not a {@link #basepath},
   * this routine is called recursively for an existing {@link ScriptVariable#filepath} or for the existing
   * commonPath or accessPath each with given left side element (variable with common and access, common with access).
   * If any of the elements commonPath or accessPath have not a base path, 
   * the {@link #localFile(StringBuilder, FilePath, FilePath)} is added too.
   *         
   * @param uRet If not null then append the result in it.  
   * @param generalPath if not null then its file() is used as part before the own basepath
   *   but only if this is not an absolute path.
   * @param accesspath a String given path which is written before the given base path if the path is not absolute in this.
   *   If null, it is ignored. If this path is absolute, the result is a absolute path of course.
   * @param returnFileIfNoBasepath null or false, then returns the basepath. 
   *   true then returns the whole file path if an extra basepath is not given or it returns the basepath. 
   *   In the last case the element is set to false.
   *   This argument is used for recursive calling to get the basepath() from a scriptVariable given
   *   FilePath or from a accessPath or commonPath. The basepath is the whole path from such an
   *   prefix element if the prefix has not a basepath itself. 
   * @return the whole base path of the constellation.
   *   Either as absolute or as relative path how it is given.
   *   The return instance is the given uRet if uRet is not null. 
   *   It is a StringBuilder if the path is assembled from more as one parts.
   *   It is a String if uRet is null and the basepath is simple.
   *   A returned StringBuilder may be used to append some other parts furthermore.
   * @throws NoSuchFieldException if the basepath contains a scriptvariable and this scriptvariable was not found. 
   *  
   */
  public CharSequence basepath(StringBuilder uRetP, FilePath commonPath, FilePath accessPath, FilePathEnvAccess env)
      throws NoSuchFieldException 
  { 
    return basepath(uRetP, commonPath, accessPath, null, env);
  }

  
  /**Same as {@link #basepath(StringBuilder, FilePath, FilePath, FilePathEnvAccess)} but the scriptvariable may be given immediately.
   * Then this must not contain a script variable too. Both is too much.
   * @param uRetP
   * @param commonPath
   * @param accessPath
   * @param scriptVariable either a CharSequence or FilePath. If given the this.scriptVariable is not used. It is processed with this parameter already.
   * @param env
   * @return
   * @throws NoSuchFieldException
   */
  public CharSequence basepath(StringBuilder uRetP, FilePath commonPath, FilePath accessPath, Object scriptVariable, FilePathEnvAccess env)
      throws NoSuchFieldException 
  { 
    //if(generalPath == null){ generalPath = emptyParent; }
    //first check singularly conditions
    ///
    int pos;
    final FilePath varfile;
    final CharSequence varpath;
    Object oValue = scriptVariable;
    if(oValue == null && this.scriptVariable !=null) { //
      assert(scriptVariable == null); //Parameter
      oValue = env.getValue(this.scriptVariable);
      if(oValue == null) throw new NoSuchFieldException("FilePath.basepath - scriptVariable not found; "+ this.scriptVariable);
      ;
    }
    if(oValue !=null){
      if(oValue instanceof FilePath){
        varfile = (FilePath)oValue;
        varpath = null;
      } else if(oValue instanceof CharSequence){
        varpath = (CharSequence)oValue;
        varfile = null;
      } else {
        throw new IllegalArgumentException("FilePath.basepath - scriptVariable faulty type; " + oValue.getClass().getSimpleName());
      }
    } else {
      varfile = this.varFilePath; varpath = this.varChars;  //may be given already expanded..
    }
    /*
    if((this.basepath !=null || returnFileIfNoBasepath !=null && returnFileIfNoBasepath[0]) && this.scriptVariable !=null){
      //get the variable if a base path is given or the file may be used as base path
      varO = env.getValue(this.scriptVariable);
      varfile = varO instanceof FilePath ? (FilePath) varO : null;
    } else { 
      varfile = null;
      varO = null;
    }
    */
    if(this.absPath){
      //  common    variable    this         basepath build with         : localdir build with   
      //  | base    | base abs  base abs
      //  x  x      x  x   x     1    1      /thisBase                   : thisLocal
      //  x  x      x  x   x     0    1      "/"                         : thisLocal
      if(this.drive !=null || this.basepath !=null || uRetP !=null || varfile !=null){
        StringBuilder uRet = uRetP !=null ? uRetP : new StringBuilder();   //it is necessary.
        uRet.setLength(0);
        if(this.drive !=null){ 
          uRet.append(this.drive).append(":");
        }
        uRet.append("/");
        if(this.basepath !=null){ 
          uRet.append(this.basepath);
        } 
        return uRet;
      } else {
        assert(uRetP == null && this.basepath ==null && this.drive == null);
        return "/";
      }
    }
    else if(this.basepath !=null){  //maybe path after the script variable
      //
      //  common    variable    this         basepath build with         : localdir build with   
      //  | base    | base abs  base abs
      //  x         1  x   1     1    0      /varFile + thisBase         : thisLocal
      //  0         1  x   0     1    0      varFile + thisBase
      //  1  x      1  x   0     1    0      commonFile + varFile + thisBase : thisLocal
      //  0         0            1    0      thisBase                    : thisLocal
      //  1  x      0            1    0      commonFile + thisBase       : thisLocal
      if(commonPath !=null || accessPath !=null || varfile !=null || varpath !=null){
        //a StringBuilder is necessary to assemble to path:
        //
        //  common    variable    this         basepath build with         : localdir build with   
        //  | base    | base abs  base abs
        //  x         1  x   1     1    0      /varFile + thisBase         : thisLocal
        //  0         1  x   0     1    0      varFile + thisBase
        //  1  x      1  x   0     1    0      commonFile + varFile + thisBase : thisLocal
        //  1  x      0            1    0      commonFile + thisBase       : thisLocal
        CharSequence prepath;
        StringBuilder uRet = uRetP !=null ? uRetP : new StringBuilder();   //it is necessary.
        
        if(varfile !=null){
          //  common    variable    this         basepath build with         : localdir build with   
          //  | base    | base abs  base abs
          //  x         1  x   1     1    0      /varFile + thisBase         : thisLocal
          //  0         1  x   0     1    0      varFile + thisBase
          //  1  x      1  x   0     1    0      commonFile + varFile + thisBase : thisLocal
          prepath = varfile.file(uRet, commonPath, accessPath, env);
        }
        else if(commonPath !=null){
          //  common    variable    this         basepath build with         : localdir build with   
          //  | base    | base abs  base abs
          //  1  x      0            1    0      commonFile + thisBase       : thisLocal
          prepath = commonPath.file(uRet, accessPath, env);
        } 
        else if(accessPath !=null){
          //  common    variable    this         basepath build with         : localdir build with   
          //  | base    | base abs  base abs
          //  1  x      0            1    0      commonFile + thisBase       : thisLocal
          prepath = accessPath.file(uRet, null, env);
        } else {
          //possible to have a variable with text or environment variable.
          prepath = uRet;
        }
        if(this.basepath.length() >0 || varfile !=null && varfile == null || varpath !=null){
          //need to add somewhat, build the StringBuilder if not done.
          if(prepath instanceof StringBuilder){
            uRet = (StringBuilder)prepath;
          } else {
            assert(uRet == null);  //elsewhere it might be used for prepath
            uRet = prepath !=null ? new StringBuilder(prepath) : new StringBuilder();
          }
        }
        final CharSequence text;
        if(varpath !=null && varfile == null){
          text = varpath.toString();
        } else {
          text = null;
        }
        if(text!=null && text.length() >0){
          pos = uRet.length();
          if(pos >0 && isRootpath(text)>0){
            //  common    variable    this         basepath build with         : localdir build with   
            //  | base    | base abs  base abs
            //  x         1  x   1     1    0      /varFile + thisBase         : thisLocal
            uRet.setLength(0);
          }
          else {
            //  common    variable    this         basepath build with         : localdir build with   
            //  | base    | base abs  base abs
            //  0         1  x   0     1    0      varFile + thisBase
            //  1  x      1  x   0     1    0      commonFile + varFile + thisBase : thisLocal
            if( pos >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
          }
          uRet.append(text);
        }
        if(this.basepath.length() ==0){
          //it is possible to have an empty basepat, writing $variable:
          return uRet !=null ? uRet : prepath;
        } else {
          if( (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
          uRet.append(this.basepath);
          return uRet;
        }
      } else {
        //  common    variable    this         basepath build with         : localdir build with   
        //  | base    | base abs  base abs
        //        0         0            1    0      thisBase                    : thisLocal
        return this.basepath;
      }
    } else { //this.basepath == null 
      //  common    variable    this         basepath build with         : localdir build with   
      //  | base    | base abs  base abs
      //  x  x      1  1   1     0    0      /variableBase               : varLocalFile + thisLocal

      //  0  x      1  1   0     0    0      variableBase                : varLocalFile + thisLocal
      //  1  x      1  1   0     0    0      commonFile + variableBase   : varLocalFile + thisLocal
      //    
      //  x  x      1  0   1     0    0      /                           : varLocalFile + thisLocal
     
      //  0  x      1  0   0     0    0      ""                          : varLocalFile + thisLocal
      //  1  0      1  0   0     0    0      commonFile                  : varLocalFile + thisLocal
      //  1  1      1  0   0     0    0      commonBase                  : varLocalFile + thisLocal
      //    
      //  0  x      0            0    0      ""                          : thisLocal
      //  1  0      0            0    0      commonFile                  : thisLocal
      //  1  1      0            0    0      commonBase                  : commonLocalfile + thisLocal
      assert(this.basepath == null); //check whether other parts have a basepath
      //The varFile is part of the localpath, because basepath == null. Don't use here.
      //Get the basepath from the whole common or access.
      //It one of them defines a basepath only that is returned. 
      CharSequence prepath;
      boolean[] returnFileIfNoBasepathSub = new boolean[1];
      returnFileIfNoBasepathSub[0] = true;  //use the file of commonPath or accessPath as base path. 
      StringBuilder uRet = uRetP !=null ? uRetP : new StringBuilder();   //it is necessary.
      if(varfile !=null && (varfile.basepath !=null)){
        //use the variableFile if it is called recursively. 
        //The variable is that one from commonPath or accessPath of the caller.
        //
        //  common    variable    this         basepath build with         : localdir build with   
        //  | base    | base abs  base abs
        //  x  x      1  1   1     0    0      /variableBase               : varLocalFile + thisLocal

        //  0  x      1  1   0     0    0      variableBase                : varLocalFile + thisLocal
        //  1  x      1  1   0     0    0      commonFile + variableBase   : varLocalFile + thisLocal
        prepath = varfile.basepath(uRet, commonPath, accessPath, env);
      }
      else if(commonPath !=null){
        //  common    variable    this         basepath build with         : localdir build with   
        //  | base    | base abs  base abs
        //  1  0      0            0    0      commonFile                  : thisLocal
        //  1  1      0            0    0      commonBase                  : commonLocalfile + thisLocal
        prepath = commonPath.basepath(uRet, null, accessPath, env);
      } 
      else if(accessPath !=null){
        //  common    variable    this         basepath build with         : localdir build with   
        //  | base    | base abs  base abs
        //  1  0      0            0    0      commonFile                  : thisLocal
        //  1  1      0            0    0      commonBase                  : commonLocalfile + thisLocal
        prepath = accessPath.basepath(uRet, null, null, env);
      } 
      else {
        //  common    variable    this         basepath build with         : localdir build with   
        //  | base    | base abs  base abs
        //  0  x      0            0    0      ""                          : thisLocal
        if(uRet !=null){ prepath = uRet; }
        else prepath = "";
      }
      return prepath;
    }
  }
  
  

  

  
  /**Gets the local directory path.
   * This is the substantial method to build the localdir or file
   * <ul>
   * <li>If this instance has a {@link #basepath}, the local directory is the local directory path part of this.
   *   In this case the commonPath, the accessPath and a {@link #scriptVariable} are not used because it enhanced the base path.
   * <li>If this has not a basepath but a {@link #scriptVariable} the scriptVariable is resolved and used as prefix
   *   before the localDir. The script variable may have a basepath, then the commonPath and the accessPath are not used.
   *   Elsewhere they are used.   
   * <li>If this has not a basepath but the commonPath is split in a basepath and a localpath,
   *   the local path part of the commonPath is used as the prefix for the local directory path.
   * <li>If this has not a basepath and the commonPath hasn't a basepath or it is null 
   *   but the accessPath is split in a basepath and a local path,
   *   the local path part of the accessPath and the whole generalPath is used as a prefix for the local directory path.
   * </ul>  
   * If the accessPath contains a wildcard in the name or ext ({@link #someFiles} is true),
   * then only the {@link #localdir(StringBuilder, FilePath, FilePath, FilePathEnvAccess)} is used from them. 
   * Elsewhere the {@link #localfile(StringBuilder, FilePath, FilePath, FilePathEnvAccess)} is evaluated from it.
   * 
   * <pre>
   *  accessPath / commonPath / scriptVariable / basepath : localDir
   *                  :                                    ^-----This localDir is the local
   *                  ^----another base path is not regarded.
   * 
   *   accessPath / commonPath / scriptVariable / localDir
   *                     :
   *                     ^---anywhere of that may have a basePath. All other are act as localDir.                    
   * </pre>
   * @param uRet If not null, then the local directory path is appended to it and uRet is returned.
   * @param commonPath possible prefix before the {@link #scriptVariable}
   * @param accessPath possible prefix before the commonPath
   * @return Either the {@link #localdir} as String or a StringBuilder instance. If uRet is given, it is used and returned.
   *   If the localdir is empty, "" is returned. (Not ".", see {@link #localdir(FilePathEnvAccess)}).
   * @throws NoSuchFieldException if a requested scriptVariable was not found.
   * @since 2014-06-20 doc improved
   *  
   */

  public CharSequence localdir(StringBuilder uRetP, FilePath commonPath, FilePath accessPath
      , FilePathEnvAccess env) 
  throws NoSuchFieldException {
    return localdir(uRetP, commonPath, accessPath, null, env);
  }
  
  
  
  
  public CharSequence localdir(StringBuilder uRetP, FilePath commonPath, FilePath accessPath
      , Object scriptVarValue, FilePathEnvAccess env) 
  throws NoSuchFieldException {
    ///
    Object oValue = scriptVarValue;
    if(oValue == null && this.scriptVariable !=null) { //
      oValue = env.getValue(this.scriptVariable);
      if(oValue == null) throw new NoSuchFieldException("FilePath.basepath - scriptVariable not found; "+ this.scriptVariable);
      ;
    }
    if(  this.basepath !=null     //if a basepath is given, then only this localpath is valid.
      || oValue == null && commonPath == null && accessPath == null //nothing else is given: The given path is local.  
      ){  //Then only the local dir of this is used.
      if(uRetP == null){
        return localdir;
      } else {
        int pos;
        if( this.localdir.length() >0 && (pos = uRetP.length()) >0 && uRetP.charAt(pos-1) != '/'){ uRetP.append("/"); }
        uRetP.append(localdir);
        return uRetP;
      }
    }
    else { 
      //this does not contain a basepath, therefore the localDir can be defined in :
      assert(this.basepath == null);
      //use a StringBuilder to concatenate anyway.
      StringBuilder uRet = (uRetP == null)? new StringBuilder() : uRetP; //it is necessary. Build it if null.
      //NOTE: appends localDir on end.
      //Firstly get localFile from scriptVariable, commonPath, accessPath as prefix.
      //If one of that has a basePath, return its localDir/file.ext.
      final FilePath varfile;
      final CharSequence varpath;
      if(oValue !=null){
        if(oValue instanceof FilePath){
          varfile = (FilePath)oValue;
          varpath = null;
        } else if(oValue instanceof CharSequence){
          varpath = (CharSequence)oValue;
          varfile = null;
        } else {
          throw new  NoSuchFieldException("FilePath.basepath - scriptVariable faulty type; "+ this.scriptVariable + ";" + oValue.getClass().getSimpleName());
        }
      } else {
        varfile = this.varFilePath; varpath = this.varChars;  //may be given already expanded..
      }
      if(varfile !=null) {
        //get the localFile from the scriptVariable or varfile, not only the localDir because it is the dir.
        if(varfile.someFiles){  //but use only the dir if some files.
          //insert localdir from varfile firstly
          varfile.localdir(uRet, commonPath, accessPath, env); //append localDir of variable 
        } else {
          //insert localfile from varfile firstly, assume it is a directory.
          varfile.localfile(uRet, commonPath, accessPath, env); //append localDir of variable 
        }
        
      } else if(varpath !=null) {
        assert(this.varChars == null); //should not be used.
        //insert the content of the scriptvariable given as argument firstly.
        uRet.append(varpath); 
      } else if(varChars !=null) {
        //insert the content of the scriptvariable firstly.
        uRet.append(varChars);
      }
      else if(commonPath !=null){  //TODO missing append? Test!
        commonPath.localfile(uRet, null, accessPath, env);
      } 
      else if(accessPath !=null){
        if(accessPath.someFiles){
          accessPath.localdir(uRet, null, null, env);
        } else {
          accessPath.localfile(uRet, null, null, env);
        }
      }
      //
      int pos;
      if( this.localdir.length() >0 && (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
      uRet.append(this.localdir);
      return uRet;
    }
  }
  
    
  
  
  
  
  /**Builds the <code>localDir/name.ext</code>.
   * See #localDir(StringBuilder, FilePath, FilePath, FilePathEnvAccess) for all conditions. 
   * @param uRetP If given appends.
   * @param commonPath
   * @param accessPath
   * @param env
   * @return Either a String which is the name or a new StringBuilder or uRetP if uRetP given.
   * @throws NoSuchFieldException
   */
  public CharSequence localfile(StringBuilder uRetP, FilePath commonPath, FilePath accessPath, FilePathEnvAccess env) 
  throws NoSuchFieldException {
    if(localdir.length() ==0 && ext.length() == 0 && commonPath ==null && accessPath == null && this.scriptVariable == null){ 
      //simplest case: only name is given.
      if(uRetP == null){ return name; }
      else { uRetP.append(name); return uRetP; }
    } else {
      CharSequence dir = localdir(uRetP, commonPath, accessPath, env);
      final StringBuilder uRet = dir instanceof StringBuilder ? (StringBuilder)dir: new StringBuilder(dir);
      int pos;
      if( (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
      uRet.append(this.name).append(this.ext);
      return uRet;
    }
  }
  
  
  
  /**Returns the file path maybe with given commonBasepath and a access path. 
   * @param accesspath Access path may be given by usage.
   * @return the whole path with file name and extension.
   *   The path is absolute or relative like it is given.
   * @throws NoSuchFieldException 
   */
  public CharSequence file(StringBuilder uRet, FilePath accesspath, FilePathEnvAccess env) throws NoSuchFieldException{
    return file(uRet,null,accesspath, env);
  }
  
  /**Returns the file path maybe with given commonBasepath and a access path. 
   * @param accesspath Access path may be given by usage.
   * @return the whole path with file name and extension.
   *   The path is absolute or relative like it is given.
   * @throws NoSuchFieldException
   * @since 2014-06-19 bugfix uRet was not used.
   */
  public CharSequence file(StringBuilder uRet, FilePath commonPath, FilePath accesspath, FilePathEnvAccess env) 
  throws NoSuchFieldException { 
    CharSequence basePath = basepath(uRet, commonPath, accesspath, env);
    StringBuilder uRet1 = basePath instanceof StringBuilder ? (StringBuilder)basePath : new StringBuilder(basePath);
    localdir(uRet1, emptyParent, accesspath, env);
    int pos;
    if( (pos = uRet1.length()) >0 && uRet1.charAt(pos-1) != '/'){ uRet1.append("/"); }
    uRet1.append(this.name).append(this.ext);
    return uRet1.append(this.ext);
  }
  
  
  /**Adds the local dir and the name, not the extension
   * @param uRet
   * @return uRet itself to concatenate.
   */
  private CharSequence addLocalName(StringBuilder uRet){ 
    int pos;
    if( this.localdir.length() > 0 && (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
    uRet.append(this.localdir);
    if( (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
    uRet.append(this.name);
    return uRet;
  }
  
  /**Adds the local dir and the name, not the extension
   * @param uRet
   * @return uRet itself to concatenate.
   * @throws NoSuchFieldException 
   */
  private CharSequence addLocalName(StringBuilder uRet, FilePathEnvAccess env) throws NoSuchFieldException{ 
    int pos;
    CharSequence localdir1 = localdir(env);
    if(!StringFunctions.equals(localdir1, ".")){
      if( this.localdir.length() > 0 && (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
      uRet.append(localdir1);
    }
    if( (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
    uRet.append(this.name);
    return uRet;
  }
  
  /**Adds the file with replaced wildcard in the local dir. 
   * <ul>
   * <li>The first "*" or "**" wildcard in the local path part is replaced by the whole local path part of the replWildc-argument.
   *   Usual the local path part of this consists only of a "**" or it consists of a path ending with "**" or it contains
   *   a prefix and a postfix like "pre/ ** /post". All variants may be proper. 
   * <li>The first "*" in the name is replaced by the whole name of the 'replWildc' argument. Usual the name may consist only
   *   of a single "*".
   * <li>The second "*" in the name is replaced by the whole ext of the 'replWildc' argument. Usual the name.ext of this 
   *   may consist of "*.*.ext". See examples.      
   * </ul>       
   * Examples
   * <table><tr><th>this </th><th> replWildc</th><th>result</th></tr>
   * <tr><td>** / *.*.ext</td><td>rlocal/rpath/rname.rxt</td><td>rlocal/rpath/rname.rxt.ext</td></tr>
   * <tr><td>local/path / *.*.ext</td><td>rlocal/rpath/rname.rxt</td><td>local/path/rname.rxt.ext</td></tr>
   * <tr><td>local/path/ ** / *.*.ext</td><td>rlocal/rpath/rname.rxt</td><td>local/path/rlocal/rpath/rname.rxt.ext</td></tr>
   * <tr><td>local/ ** /path / *.*.ext</td><td>rlocal/rpath/rname.rxt</td><td>local/rlocal/rpath/path/rname.rxt.ext</td></tr>
   * <tr><td>*.*.ext</td><td>rlocal/rpath/rname.rxt</td><td>rname.rxt.ext</td></tr>
   * <tr><td>*.ext</td><td>rlocal/rpath/rname.rxt</td><td>rname.ext</td></tr>
   * <tr><td>*.*</td><td>rlocal/rpath/rname.rxt</td><td>rname.rxt</td></tr>
   * </table>
   * @param uRet
   * @param replWildc With that localdir and name a wildcard in this is replaced.
   * @return uRet itself to concatenate.
   */
  public CharSequence localfileReplwildcard(StringBuilder uRet, FilePath replWildc){ 
    int pos;
    if( this.localdir.length() > 0 && (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
    int posW = this.localdir.indexOf('*');
    int posW2 = this.localdir.length() > posW+1 && this.localdir.charAt(posW+1) == '*' ? posW+2 : posW+1;
    if(posW >=0){
      uRet.append(this.localdir.substring(0,posW));
      uRet.append(replWildc.localdir);
      uRet.append(this.localdir.substring(posW2));
    } else{
      uRet.append(this.localdir);
    }
    if( (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
    posW = this.name.indexOf('*');
    posW2 = this.name.indexOf('*', posW+1);
    if(posW >=0){
      uRet.append(this.name.substring(0,posW));     //may be empty
      uRet.append(replWildc.name);
      if(posW2 >=0){
        uRet.append(this.name.substring(posW+1, posW2));
        if(replWildc.ext.length() >1){ uRet.append(replWildc.ext.substring(1)); }  //without leading dot
        uRet.append(this.name.substring(posW2+1));  //may be empty
      } else {
        uRet.append(this.name.substring(posW+1));   //may be empty
      }
    } else{
      uRet.append(this.name);
    }
    return uRet;
  }
  
  

  
  
  /**Builds the absolute path with given basepath maybe absolute or not, maybe with drive letter or not. 
   * @return the whole path inclusive a given general path in a {@link UserFileSet} as absolute path.
   */
  private CharSequence absbasepath(CharSequence sBasepath, FilePathEnvAccess env){ 
    CharSequence ret = sBasepath;
    if(isRootpath(ret) ==0){ //a relative path: insert the currdir of the script only if it is not a root directory already:
      if(ret.length() >0){      //only if ret is not ""
        StringBuilder uRet;
        if(ret instanceof StringBuilder){
          uRet = (StringBuilder)ret;
        } else {
          ret = uRet = new StringBuilder(ret);
        }
        CharSequence sCurrDir = env.getCurrentDir();
        if(uRet.length() >=2 && uRet.charAt(1) == ':'){
          //a drive is present but it is not a root path
          if(sCurrDir.length()>=2 && sCurrDir.charAt(1)==':' && uRet.charAt(0) == sCurrDir.charAt(0)){
            //Same drive letter like sCurrDir: replace the absolute path.
            uRet.insert(2, '/'); //will stay after sCurrDir
            uRet.replace(0, 2, sCurrDir.toString());
          }
          else {
            //a drive is present, but it is another drive else in sCurrDir But the path is not absolute:
            //TODO nothing yet, 
          }
        }
        else {  //a drive is not present.
          uRet.insert(0, '/'); //will stay after sCurrDir
          uRet.insert(0, sCurrDir);
        }
      }
      else {
        //ret is "", then return the current dir only.
        ret = env.getCurrentDir();
      }
    }
    ret = FileSystem.normalizePath(ret);
    return ret;
  }
  

  
  
  /**This class is used only temporary while processing the parse result into a instance of {@link FilePath}
   * while running {@link ZbnfJavaOutput}. 
   * @deprecated This is the only one reason that the fields of FilePath are fine graduated. It saves calculation time
   * if a better algorithm is used. This class will be removed if the {@link org.vishia.zcmd.JZtxtcmdScript} and its syntax
   * does not need it anymore.
   */
  @Deprecated public static class XXXZbnfFilepath{
    
    /**The instance which are filled with the components content. It is used for the user's data tree. */
    public final FilePath filepath;
    
    
    public XXXZbnfFilepath(){
      filepath = new FilePath();
    }
    
    /**FromZbnf. */
    public void set_drive(String val){ filepath.drive = val; }
    
    
    /**FromZbnf. */
    public void set_absPath(){ filepath.absPath = true; }
    
    /**FromZbnf. */
    public void set_scriptVariable(String val){ System.err.println("not supported"); }
    

    
    //public void set_someFiles(){ someFiles = true; }
    //public void set_wildcardExt(){ wildcardExt = true; }
    //public void set_allTree(){ allTree = true; }
    
    /**FromZbnf. */
    public void set_pathbase(String val){
      filepath.basepath = val.replace('\\', '/');   //file is empty and ext does not start with dot. It is a filename without extension.
      filepath.allTree = val.indexOf('*')>=0;
    }
    
    /**FromZbnf. */
    public void set_path(String val){
      filepath.localdir = val.replace('\\', '/');   //file is empty and ext does not start with dot. It is a filename without extension.
      filepath.allTree = val.indexOf('*')>=0;
    }
    
    /**FromZbnf. */
    public void set_name(String val){
      filepath.name = val;   //file is empty and ext does not start with dot. It is a filename without extension.
      filepath.someFiles |= val.indexOf('*')>=0;
    }
    
    /**FromZbnf. If the name is empty, it is not the extension but the name.*/
    public void set_ext(String val){
      if(val.equals(".") && filepath.name.equals(".")){
        filepath.name = "..";
        //filepath.localdir += "../";
      }
      else if((val.length() >0 && val.charAt(0) == '.') || filepath.name.length() >0  ){ 
        filepath.ext = val;  // it is really the extension 
      } else { 
        //a file name is not given, only an extension is parsed. Use it as file name because it is not an extension!
        filepath.name = val;   //file is empty and ext does not start with dot. It is a filename without extension.
      }
      filepath.someFiles |= val.indexOf('*')>=0;
    }
    

  }
  
  
  
}
