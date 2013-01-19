package org.vishia.zmake;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.vishia.util.Assert;
import org.vishia.util.FileSystem;
import org.vishia.zTextGen.TextGenScript;


public class ZmakeUserScript
{
  
  /**Version, history and license.
   * <ul>
   * <li>2013-01-19 Hartmut chg: All access methods to {@link UserFilepath} are renamed and improved. Changing of Zbnf-Syntax
   *   for the "prepFilePath::=..."
   * <li>2013-01-02 Hartmut chg: The {@link TargetParam} is established now, it can contain some {@link TargetParam#referVariables}
   *   in that kind a parameter can describe more as one fileset for other files then the input file set. Changed syntax see Component zbnfjax.
   * <li>2012-12-29 Hartmut chg: A {@link UserFilepath} is independent from a target and describes a non-completely relative path usually.
   *   The path is completed, usual as absolute path, if the {@link UserFileSet} is used in a target. The {@link TargetInput} of a target
   *   determines the location of the file set by its {@link TargetInput#srcpathInput}. The {@link UserFilepath} of all inputs are cloned
   *   and completed with that srcpath for usage. The {@link UserTarget#allInputFiles()} or {@link UserTarget#allInputFilesExpanded()}
   *   builds that list. Usage of that files provide the correct source path for the files of a target's input.
   * <li>2012-12-08 Hartmut chg: improve access rotoutines.
   * <li>2012-11-29 Hartmut new: The {@link UserFilePath} now contains all access routines to parts of the file path
   *   which were contained in the ZmakeGenerator class before. This allows to use this with the common concept
   *   of a text generator using {@link org.vishia.zTextGen.TextGenerator}. That Textgenerator does not know such
   *   file parts and this {@link UserFilepath} class because it is more universal. Access is possible 
   *   with that access methods {@link UserFilepath#localPath()} etc. To implement that a {@link UserFilepath}
   *   knows its file-set where it is contained in, that is {@link UserFilepath#parent}. 
   *   The {@link UserFileset#script} knows the Zmake generation script. In this kind it is possible to use the
   *   common data of a file set and the absolute path.
   * <li>2011-08-14 Hartmut chg: ZmakeGenerator.Gen_Content#genContentForInputset(...) now regards < ?expandFiles>:
   *   If this tag is found in a < :forInput>...< .forInput> content generation prescript, 
   *   then wildcards in the file name are expanded to the execution of the content prescript for all files 
   *   which are found matching to the wildcard filename. The {@link FileSystem#addFilesWithBasePath(String, List)} 
   *   is used to evaluate the files. The user scripts can be more shorten now if all files in a directory should be taken.
   * <li>2011-03-25 Hartmut chg: * Zmake: improved. Usage of <*name.> instead (?name?), but not finished. (VDSP compiled and linked)
   * <li>2011-03-10 Hartmut chg: Zmake improved with the possibility of (?for:paramName?)...(?*paramName?)...(?/for?).
   *   The for-statements are nestable, an access to elements of all levels is possible using the adequate (?*name?) of the (?:for:name....level. See examples.
   *   This feature is used first to include the include path in compiling commands or to include some libs for the linker.
   * <li>2011-02-20 created. The basic was the XML implementation of zmake. The content of this user script was present
   *   in an XML tree after parsing instead in this java class. It was processed with a XSLT translator.
   *   The new Zmake concept is attempt to use Java and a textual generation from java data instead XSLT, it is more flexible.
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
  static final public int version = 20121021;


  
  
  /**This class describes one file entry in a zmake script. The file entry can contain wild cards.
   * It may be a absolute or a relative path. It can have a base path and a local path part.
   * <ul>
   * <li>
   * <li><b>localpath</b>:
   *   If you write <code>anyPath/path:localPath/path/file.ext</code> then it describes a path which part 
   *    from <code>localPath</code> can be used as path to the file in some scripts. 
   *    <ul>
   *    <li>For example in C-compilation object-files can be stored in sub directories of the objects destination directory 
   *      which follows this local path designation.
   *    <li>Another example: copying some files from one directory location to another in designated sub directories.
   *    <ul> 
   * <li><b>General path</b>: If this file entry is member of a file set, the file set can have a general path.
   *   It is given by the {@link UserFileset#srcpath}. A given general path is used for all methods. 
   *   Only this entry describes an absolute path the general path is not used. 
   * <li><b>Drive letter or select path</b>: On windows systems a drive letter can be used in form <code>A:</code>.
   *   The path should not be absolute. For example <code>"X:mypath\file.ext"</code> describes a file starting from the 
   *   current directory of the <code>X</code> drive. Any drive has its own current directory. A user can use this capability
   *   of windows to set different current directories in special maybe substitute drives.
   * <li><b>Drive letter as select path</b>:  
   *   It may be possible (future extension) to use this capability independent of windows in this class. 
   *   For that the {@link #parent} can have some paths associated to drive letters with local meaning,
   *   If the path starts with a drive letter, the associated path is searched in the parents drive list. 
   * <li><b>Absolute path</b>: If this entry starts with slash or backslash, maybe after a drive designation for windows systems,
   *   it is an absolute path. Elsewhere the parent's general path can be absolute. If an absolute path is requested
   *   calling {@link #absFile()} or adequate and the path is not given as absolute path, then the current directory is used
   *   as prefix for the path. The current directory is a property of the {@link UserScript#sCurrDir}. The current directory
   *   of the operation system is not used for that. 
   * <li><b>opeation systems current directory</b>: In opposite if you generate a relative path and the executing system
   *   expects a normal path then it may use the operation system's current directory. But that behaviour is outside of this tool.
   * <li><b>Slash or backslash</b>: The user script can contain slash characters for path directory separation also for windows systems.
   *   It is recommended to use slash. The script which should be generate may expect back slashes on windows systems.
   *   Therefore all methods which returns a path are provided in 2 forms: With "W" on end of there name it is the windows version
   *   which converts given slash characters in backslash in its return value. So the generated script will contain backslash characters.
   *   Note that some tools in windows accept a separation with slash too. Especial in C-sources an <code>#include <path/file.h></code>
   *   should be written with slash or URLs (hyperlinks) should be written with slash in any case.    
   * <li><b>Return value of methods</b>: All methods which assembles parts of the path returns a {@link java.lang.CharSequence}.
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
   * </ul>  
   * <br>
   * ZBNF-syntax parsing an Zmake input script for this class:
   * <pre>
prepFilePath::=<$NoWhiteSpaces><! *?>
[<!.?@drive>:]               ## only 1 char with followed : is drive
[ [/|\\]<?@absPath>]         ## starting with / is absolute path
[<*:?@pathbase>[?:=]:]       ## all until : is pathbase But ":=" is not expected after that.
[ <*|**?@path>               ## all until ** is path
| <toLastCharIncl:/\\?@path> ## or all until last \\ or / is path
|]                           ## or no path is given.
[ **<?@allTree>[/\\] ]       ## ** / is found, than files in subtree
[ <**?@file>                 ## all until * is the file (begin)
  *<?@someFiles>             ## * detect: set someFiles
| <toLastChar:.?@file>       ## or all until dot is the file
|]                           ## or no file is given.
[\.*<?@wildcardExt>]         ## .* is wildcard-extension
[ <* \e?@ext> ]              ## the rest is the extension
.
   * </pre>
   */
  public static class UserFilepath
  {
    /**Aggregation to a given srcpath in the {@link UserFileset} which is valid for all this files. */
    private final UserFileset parent;
    
    private final UserScript script;
    
    /**From ZBNF: $<$?scriptVariable>. If given, then the {@link #basePath()} starts with it. It is an absolute path then. */
    public String scriptVariable;
    
    public String drive;
    /**From Zbnf: [ [/|\\]<?@absPath>]. Set if the path starts with '/' or '\' maybe after drive letter. */
    public boolean absPath;
    
    /**Path-part before a ':'. */
    String pathbase;
    
    /**Localpath after ':' or the whole path. */
    String path = "";
    
    /**From Zbnf: The filename without extension. */
    String name = "";
    
    
    /**From Zbnf: The extension inclusive the leading dot. */
    String ext = "";
    
    boolean allTree, someFiles;
    
    private static UserFilepath emptyParent = new UserFilepath();
    
    private UserFilepath(){
      this.parent = null;
      this.script = null;
    }
    
    
    UserFilepath(UserFileset parent){
      this.parent = parent;
      this.script = parent.script;
    }
    
    UserFilepath(UserScript script){
      this.parent = null;
      this.script = script;
    }
    
    UserFilepath(UserScript script, UserFilepath src, CharSequence pathbase0){
      this.parent = null;
      this.script = script;
      this.drive = src.drive;
      this.absPath = src.absPath;
      if(pathbase0 == null || pathbase0.length() == 0){
        this.pathbase = src.basepath().toString();
      } else {
        StringBuilder u = new StringBuilder(pathbase0);
        u.append(src.basepath());
        this.pathbase = u.toString();
      }
      this.path = src.path;
      this.name = src.name;
      this.ext = src.ext;
      this.allTree = src.allTree;
      this.someFiles = src.someFiles;
    }
    
    //public void set_someFiles(){ someFiles = true; }
    //public void set_wildcardExt(){ wildcardExt = true; }
    //public void set_allTree(){ allTree = true; }
    
    /**FromZbnf. */
    public void set_pathbase(String val){
      pathbase = val.replace('\\', '/');   //file is empty and ext does not start with dot. It is a filename without extension.
      allTree = val.indexOf('*')>=0;
    }
    
    /**FromZbnf. */
    public void set_path(String val){
      path = val.replace('\\', '/');   //file is empty and ext does not start with dot. It is a filename without extension.
      allTree = val.indexOf('*')>=0;
    }
    
    /**FromZbnf. */
    public void set_name(String val){
      name = val;   //file is empty and ext does not start with dot. It is a filename without extension.
      someFiles |= val.indexOf('*')>=0;
    }
    
    /**FromZbnf. If the name is empty, it is not the extension but the name.*/
    public void set_ext(String val){
      if(val.equals(".") && name.equals(".")){
        name = "..";
      }
      else if((val.length() >0 && val.charAt(0) == '.') || name.length() >0  ){ 
        ext = val;  // it is really the extension 
      } else { 
        //a file name is not given, only an extension is parsed. Use it as file name because it is not an extension!
        name = val;   //file is empty and ext does not start with dot. It is a filename without extension.
      }
      someFiles |= val.indexOf('*')>=0;
    }
    

    /**Method can be called in the generation script: <*absbasepath()>. 
     * @return the whole path inclusive a given general path in a {@link UserFileSet} as absolute path.
     */
    public CharSequence absbasepath(){ 
      CharSequence basePath = basepath();
      if(this.absPath){ return basePath; }
      else if(basePath instanceof StringBuilder){
        //not an absolute path, complete it with the global given base path:
        StringBuilder uRet = (StringBuilder)basePath;
        String sCurrDir = script.sCurrDir;
        if(uRet.length() >=2 && uRet.charAt(1) == ':'){
          //a drive is present
          if(sCurrDir.length()>=2 && sCurrDir.charAt(1)==':' && uRet.charAt(0) == sCurrDir.charAt(0)){
            //Same drive letter how sCurrDir: replace the absolute path.
            uRet.replace(0, 2, sCurrDir);
          }
          else {
            //a drive is present, it is another drive else in sCurrDir But the path is not absolute:
            //TODO nothing yet, 
          }
        }
        else {
          uRet.insert(0, script.sCurrDir);
        }
        return uRet;
      }
      else {
        //this has no basepath, then return the current dir.
        return parent.script.sCurrDir;
      }
    }
    
    
    public CharSequence absbasepathW(){ return toWindows(absbasepath()); }
    

    
    /**Method can be called in the generation script: <*path.absdir()>. 
     * @return the whole path to the parent of this file inclusive a given general path in a {@link UserFileSet}.
     *   The path is absolute. If it is given as relative path, the general current directory of the script is used.
     */
    public CharSequence absdir(){ 
      CharSequence basePath = absbasepath();
      StringBuilder uRet = basePath instanceof StringBuilder ? (StringBuilder)basePath : new StringBuilder(basePath);
      int zpath = (path == null) ? 0 : path.length();
      if(zpath > 0){
        int pos;
        if( (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
        uRet.append(path.substring(0,zpath-1));
      }
      return uRet;
    }
    
    public CharSequence absdirW(){ return toWindows(absdir()); }
    
    
    /**Method can be called in the generation script: <*data.absname()>. 
     * @return the whole path with file name but without extension inclusive a given general path in a {@link UserFileSet}.
     *   Either as absolute or as relative path.
     */
    public CharSequence absname(){ 
      CharSequence basePath = absbasepath();
      StringBuilder uRet = basePath instanceof StringBuilder ? (StringBuilder)basePath : new StringBuilder(basePath);
      int pos;
      if( (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
      uRet.append(this.path);
      uRet.append(this.name);
      return uRet;
    }
    
    public CharSequence absnameW(){ return toWindows(absname()); }
    


    
    /**Method can be called in the generation script: <*path.absfile()>. 
     * @return the whole path inclusive a given general path .
     *   The path is absolute. If it is given as relative path, the general current directory of the script is used.
     */
    public CharSequence absfile(){ 
      CharSequence basePath = absbasepath();
      StringBuilder uRet = basePath instanceof StringBuilder ? (StringBuilder)basePath : new StringBuilder(basePath);
      addLocalName(uRet);
      uRet.append(ext);
      return uRet;
    }
    
    public CharSequence absfileW(){ return toWindows(absfile()); }
    
    
    /**Method can be called in the generation script: <*basepath()>. 
     * @return the whole base path inclusive a given general path in a {@link UserFileSet}.
     *   till a ':' in the input path or an empty string.
     *   Either as absolute or as relative path how it is given.
     */
    public CharSequence basepath(){ return basepath(null); }
     
    
    /**Method can be called in the generation script: <*basePath(<*abspath>)>. 
     * @param accesspath a String given path which is written before the given base path if the path is not absolute in this.
     *   If null, it is ignored. If this path is absolute, the result is a absolute path of course.
     * @return the whole base path inclusive a given general path in a {@link UserFileSet}.
     *   till a ':' in the input path or an empty string.
     *   Either as absolute or as relative path how it is given.
     */
    public CharSequence basepath(String accesspath){ 
      UserFilepath generalPath = parent !=null && parent.srcpath !=null ? parent.srcpath : emptyParent;
      if(pathbase !=null || (generalPath.pathbase !=null)){
        StringBuilder uRet = new StringBuilder();
        if(this.drive !=null){ uRet.append(this.drive).append(':'); }
        else if(generalPath.drive !=null){
          uRet.append(parent.srcpath.drive).append(':'); 
        }
        if(absPath){ uRet.append('/'); }
        else if(generalPath.absPath){ uRet.append('/'); }
        else if(accesspath !=null){
          uRet.append(accesspath);
        }
        int pos;
        //
        //append a general path completely firstly.
        if(generalPath.pathbase !=null){
          if( (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
          uRet.append(generalPath.pathbase).append('/'); 
        }
        if( (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
        uRet.append(generalPath.path).append(generalPath.name).append(generalPath.ext);
        if(this.pathbase !=null){
          if( (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
          uRet.append(this.pathbase);
        }
        return uRet;
      } else if(drive !=null){
        StringBuilder uRet = new StringBuilder();
        uRet.append(drive).append(":");
        if(absPath){ uRet.append('/'); }
        else if(accesspath !=null){
          uRet.append(accesspath);
        }
        return uRet;
      } else if(generalPath.drive !=null){
        StringBuilder uRet = new StringBuilder();
        uRet.append(generalPath.drive).append(":");
        if(generalPath.absPath){ uRet.append('/'); }
        else if(accesspath !=null){
          uRet.append(accesspath);
        }
        return uRet;
      } else {
        return accesspath !=null ? accesspath : "";
      }
    }
    
    
    
    public CharSequence basepathW(){ return toWindows(basepath()); }
    
    
    
    /**Method can be called in the generation script: <*path.dir()>. 
     * @return the whole path to the parent of this file inclusive a given general path in a {@link UserFileSet}.
     *   The path is absolute or relative like it is given.
     */
    public CharSequence dir(){ 
      CharSequence basePath = basepath();
      StringBuilder uRet = basePath instanceof StringBuilder ? (StringBuilder)basePath : new StringBuilder(basePath);
      int zpath = (path == null) ? 0 : path.length();
      if(zpath > 0){
        int pos;
        if( (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
        uRet.append(path.substring(0,zpath-1));
      }
      return uRet;
    }

    
    
    public CharSequence dirW(){ return toWindows(dir()); }
    
    /**Method can be called in the generation script: <*data.pathname()>. 
     * @return the whole path with file name but without extension inclusive a given general path in a {@link UserFileSet}.
     *   The path is absolute or relative like it is given.
     */
    public CharSequence pathname(){ 
      CharSequence basePath = basepath();
      StringBuilder uRet = basePath instanceof StringBuilder ? (StringBuilder)basePath : new StringBuilder(basePath);
      int pos;
      if( (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
      uRet.append(this.path);
      uRet.append(this.name);
      return uRet;
    }
    
    public CharSequence pathnameW(){ return toWindows(pathname()); }
    

    
    /**Method can be called in the generation script: <*data.file()>. 
     * @return the whole path with file name and extension inclusive a given general path in a {@link UserFileSet}.
     *   The path is absolute or relative like it is given.
     */
    public CharSequence file(){ 
      CharSequence basePath = basepath();
      StringBuilder uRet = basePath instanceof StringBuilder ? (StringBuilder)basePath : new StringBuilder(basePath);
      addLocalName(uRet);
      return uRet.append(ext);
    }
    
    public CharSequence fileW(){ return toWindows(file()); }
    
    
    
  
    /**Method can be called in the generation script: <*path.localdir()>. 
     * @return the local path part of the directory of the file without ending slash. 
     *   If no directory is given in the local part, it returns "./". 
     */
    public String localdir(){
      int length = path == null ? 0 : path.length();
      return length == 0 ? "." : path.substring(0, length-1); 
    }
    
    /**Method can be called in the generation script: <*path.localDir()>. 
     * @return the local path part with file without extension.
     */
    public String localdirW(){ return path.replace('/', '\\'); }
    

    
    /**Method can be called in the generation script: <*path.localname()>. 
     * @return the local path part with file without extension.
     */
    public CharSequence localname(){ 
      StringBuilder uRet = new StringBuilder();
      return addLocalName(uRet); 
    }
    
    public CharSequence localnameW(){ return toWindows(localname()); }

    
    /**Method can be called in the generation script: <*path.localfile()>. 
     * @return the local path to this file inclusive name and extension of the file.
     */
    public CharSequence localfile(){ 
      StringBuilder uRet = new StringBuilder();
      addLocalName(uRet);
      uRet.append(this.ext);
      return uRet;
    }

    public CharSequence localfileW(){ return toWindows(localfile()); }

    
    private CharSequence addLocalName(StringBuilder uRet){ 
      int pos;
      if( (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
      uRet.append(this.path);
      uRet.append(name);
      return uRet;
    }
    
    
    /**Method can be called in the generation script: <*path.name()>. 
     * @return the name of the file without extension.
     */
    public CharSequence name(){ return name; }
    
    /**Method can be called in the generation script: <*path.namext()>. 
     * @return the file name with extension.
     */
    public CharSequence namext(){ 
      StringBuilder uRet = new StringBuilder(); 
      uRet.append(name);
      uRet.append(ext);
      return uRet;
    }
    
    /**Method can be called in the generation script: <*path.ext()>. 
     * @return the file extension.
     */
    public CharSequence ext(){ return ext; }
    
    
    private static CharSequence toWindows(CharSequence inp)
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
    
    /**Fills this.{@link #filesOfFilesetExpanded} with all files, which are selected by the filepathWildcards and the absPath.
     * The filepathWildcards does not need to contain in this UserFileset, it may be contained in another one too.
     * Especially the {@link UserTarget} in form of this base class can be filled.
     * @param filepathWildcards
     * @param absPath
     */
    void expandFiles(List<UserFilepath> listToadd, CharSequence srcpath, File currdir){
      List<FileSystem.FileAndBasePath> listFiles = new LinkedList<FileSystem.FileAndBasePath>();
      final CharSequence basePath = srcpath.toString() + "/" + this.basepath(); //getPartsFromFilepath(file, null, "absBasePath").toString();
      final CharSequence localfilePath = this.localfile(); //getPartsFromFilepath(file, null, "file").toString();
      final String sPathSearch = basePath + ":" + localfilePath;
      try{ FileSystem.addFilesWithBasePath(currdir, sPathSearch, listFiles);
      } catch(FileNotFoundException exc){
        //let it empty.
      }
      for(FileSystem.FileAndBasePath file1: listFiles){
        ZmakeUserScript.UserFilepath filepath2 = new ZmakeUserScript.UserFilepath(script);
        /*
        if(file1.basePath !=null ){
          int posEnd = file1.basePath.length() -1;  //last char is path separator, file2.pathbase without separator!
          if(filepathWildcards.absPath){
            file2.pathbase = file1.basePath.substring(0, posEnd);
          } else {
            //The file1.basePath contains the currDir, remove it.
            if(lengthPathCurrdir < posEnd){
              file2.pathbase = file1.basePath.substring(lengthPathCurrdir, posEnd);
            } else {file2.pathbase = "..";}
          }
        }
        */
        filepath2.pathbase = basePath.toString();  //it is the same. Maybe null
        int posName = file1.localPath.lastIndexOf('/') +1;  //if not found, set to 0
        int posExt = file1.localPath.lastIndexOf('.');
        final String sPath = file1.localPath.substring(0, posName);  //"" if only name
        final String sName;
        final String sExt;
        if(posExt < 0){ sExt = ""; sName = file1.localPath.substring(posName); }
        else { sExt = file1.localPath.substring(posExt); sName = file1.localPath.substring(posName, posExt); }
        filepath2.path = sPath;
        filepath2.name = sName;
        filepath2.ext = sExt;
        listToadd.add(filepath2);
      }

    }
    

    
    @Override
    public String toString()
    { return file().toString();
    }
  }
  
  
  
  
  /**A < fileset> in the ZmakeStd.zbnf. It is assigned to a script variable if it was created by parsing the user script.
   * If the fileset is used in a target, it is associated to the target to get the absolute paths of the files
   * temporary while processing that target. See 
   * <pre>
fileset::= 
{ srcpath = <""?!prepSrcpath>
| srcext = <""?srcext>
| <file>
? , | + 
}.

   * </pre>
   * @author Hartmut
   *
   */
  public static class UserFileset
  {
    
    final UserScript script;
    
    /**From ZBNF srcpath = <""?!prepSrcpath>. It is a part of the base path anyway. It may be absolute, but usually relative. 
     * If null then unused. */
    UserFilepath srcpath;
    
    /**From ZBNF srcext = <""?srcext>. If null then unused. */
    public String srcext;
    
    
    /**The target which handles with this file set yet. The target can contain an additional part of the base path. */
    //private UserTarget target;
    //private UserInput targetinput;
    
    /**All entries of the file set how it is given in the users script. */
    List<UserFilepath> filesOfFileset = new LinkedList<UserFilepath>();
    
    /**All files without wildcards build from all found files of the {@link #filesOfFileset}
     * but only build if requested. */
    //List<UserFilepath> filesOfFilesetExpanded;
    
    UserFileset(UserScript script){
      this.script = script;
    }
    
    
    public UserFileset(){
      this.script = null;
    }
    
    
    
    /**From Zbnf: srcpath = <""?!prepSrcpath>.
     * It sets the base path for all files of this fileset. This basepath is usually relative.
     * @return ZBNF component.
     */
    public UserFilepath new_srcpath(){ return srcpath = new UserFilepath(script); }  //NOTE: it has not a parent. this is not its parent!
    public void set_srcpath(UserFilepath val){  }  //it is set already.
    
    /**From ZBNF: < file>. */
    public UserFilepath new_file(){ return new UserFilepath(this); }
    
    /**From ZBNF: < file>. */
    public void add_file(UserFilepath val){ 
      if(val.pathbase !=null || val.path.length() >0 || val.name.length() >0 || val.drive !=null){
        //only if any field is set. not on empty val
        filesOfFileset.add(val); 
      }
    }
    
    
    void listFilesExpanded(List<UserFilepath> files, CharSequence accesspath) {  ////
      boolean expandFiles = true;
      for(UserFilepath filepath: filesOfFileset){
        if(expandFiles && filepath.someFiles || filepath.allTree){
          filepath.expandFiles(files, accesspath, script.currDir);
        } else {
          //clone filepath! add srcpath
          UserFilepath targetsrc = new UserFilepath(script, filepath, accesspath);
          files.add(targetsrc);
        }
      }
    }

    public List<UserFilepath> listFilesExpanded(CharSequence accesspath) { 
      List<UserFilepath> files = new LinkedList<UserFilepath>();
      listFilesExpanded(files, accesspath);
      return files;
    }
    
    
    public List<UserFilepath> listFilesExpanded() { return listFilesExpanded(null); }

      
      
    @Override
    public String toString(){ 
      StringBuilder u = new StringBuilder();
      if(srcpath !=null) u.append("srcpath="+srcpath+", ");
      u.append(filesOfFileset);
      return u.toString();
    }
  }
  
  
  private final static class UserStringContent
  {
    private final char type;
    private String text;
    private UserStringContent(char type){ this.type = type; }
    private UserStringContent(char type, String text){ this.type = type; this.text = text; }
  }
  
  
  /**A < string> in the ZmakeStd.zbnf is
   * string::= { <""?literal> | <forInputString> | @<$?inputField> | $<$?contentOfVariable> ? + }.
   *
   */
  public final static class UserString
  {
    private final List<UserStringContent> content = new LinkedList<UserStringContent>();
    
    /**Set From ZBNF: */
    public UserStringContent new_literal(){ return new UserStringContent('\"'); }
    
    /**Set From ZBNF: */
    public void add_literal(UserStringContent val){ content.add(val); }
    
    public void set_literal(String val){ 
      UserStringContent el = new UserStringContent('\"', val);
      content.add(el);
    }
    
    
    public CharSequence getText(){ return text(); }

    
    public CharSequence text()
    {
      StringBuilder u = new StringBuilder();
      for(UserStringContent el: content){
        switch(el.type){
        case '"': u.append(el.text);
        }
      }
      return u;
    }

    public void addtext(StringBuilder u)
    {
      for(UserStringContent el: content){
        switch(el.type){
        case '"': u.append(el.text);
        }
      }
    }

    @Override public String toString(){ return getText().toString(); }
    
  }
  
  /**A < variable> in the ZmakeStd.zbnf is
   * variable::=<$?@name> = [ fileset ( <fileset> ) | <string> | { <execCmd> } ].
   *
   */
  public static class ScriptVariable
  { final UserScript script;
    public String name;
    UserFileset fileset;
    
    TextGenScript.Expression expression;
    
    //public UserString string;
    
    ScriptVariable(UserScript script){
      this.script = script;
    }
    public UserFileset new_fileset(){ return fileset = new UserFileset(script); }
    public void add_fileset(UserFileset val){ }
    
    public TextGenScript.Expression new_expression(){ return new TextGenScript.Expression(); }
    
    public void add_expression(TextGenScript.Expression val){ expression = val; }
    
    //public UserString new_string(){ return string = new UserString(); }
    //public void set_string(UserString val){} //left empty, is set already.
    
    /**For textscript: <*var.name.files()>
     * @return The List of files. 
     */
    public List<UserFilepath> files(){ return (fileset !=null) ? fileset.filesOfFileset : null; }
    
    
    @Override public String toString(){
      return name + ":" + (expression !=null ? expression : fileset);
    }
    
    
    /**For textscript: <*var.name.text()>
     * @return The text of the script variable. 
     */
    public CharSequence text(){ return expression !=null ? expression.text() : ""; }
    
  }
  
  public static class TargetParamZbnf
  { 
    /**The target where this input set is used. */
    final UserTarget parentTarget;

    /**From ZBNF: [<$?name> =] of a parameter or null  */
    public String name;
    /**From ZBNF: [<$?name> = [ <""?value>]] of a parameter or null  */
    public String value;
    
    //public String referVariable;
    
    List<TargetInput> referVariables; 
    
    TargetParamZbnf(UserTarget parentTarget){ this.parentTarget = parentTarget; }
    
    public TargetInput new_inputarg(){ return new TargetInput(parentTarget); }

    public void add_inputarg(TargetInput val){ 
      if(referVariables == null){
        referVariables = new LinkedList<TargetInput>();
      }
      referVariables.add(val);
    }
  }
  
  
  public static class TargetParam
  { 
    /**The target where this input set is used. */
    final UserTarget parentTarget;

    /**From ZBNF: [<$?name> =] of a parameter or null  */
    final String name;
    /**From ZBNF: [<$?name> = [ <""?value>]] of a parameter or null  */
    final String value;
    
    //final String referVariable;
    
    final List<TargetInput> referVariables; 
    
    TargetParam(TargetParamZbnf src){ 
      this.parentTarget = src.parentTarget;
      this.name = src.name;
      this.value = src.value;
      //this.referVariable = null;
      this.referVariables = src.referVariables;
    }
    
    
    /**Returns the text which is referred with this parameter. 
     * <ul>
     * <li>If the parameter is given with a constant stringLiteral, it is returned without any effort. 
     * <li>If the parameter refers a string script variable, its content is evaluated and returned.
     * <li>If the parameter referes a concatenation of script variables, it is returned.
     * </ul>
     * <br><br> 
     * If the scriptVariable or any of them in a concatenation was not found, either a IllegalArgumentException is thrown
     * or a "<??errorText??> will be produced.
     * @return Any case a string if no exception.
     */
    public CharSequence text(){
      if(value !=null) return value;
      else {
        StringBuilder u = new StringBuilder();
        for(TargetInput element: referVariables){
          if(element.inputFile !=null){
            u.append(element.inputFile.file());
          } else { 
            assert(element.referVariable !=null);
            //search the input Set in the script variables:
            ScriptVariable variable = parentTarget.script.var.get(element.referVariable);
            if(variable == null){
              int pos = u.length();
              u.append("<??error ZmakeScriptvariable not found: ").append(element.referVariable).append(" ??>");
              parentTarget.script.abortOnError(u,pos);
            } else if(variable.expression !=null){
              u.append(variable.expression.text());
              //variable.expression.addtext(u);
            } else {
              int pos = u.length();
              u.append("<??error ZmakeScriptvariable as string expected: ").append(element.referVariable).append(" ??>");
              parentTarget.script.abortOnError(u,pos);
            } 
          }
        }
        return u;
      }
    }

    
    public CharSequence textW(){ return UserFilepath.toWindows(text()); }
  
  
  }
  

  

  
  /**Describes 1 input item for a target, maybe a file, maybe a inputset. */
  public static class TargetInput //extends UserFileset
  { //final UserInputSet inputSet;
    //final String inputSet;
    /**The main script. */
    //final UserScript script;
    /**The target where this input set is used. */
    final UserTarget parentTarget;
  
    /**From ZBNF: [<$?name> =] of a parameter or null  */
    //public String paramname;
    
    /**From ZBNF: [<$?name> = [ <""?value>]] of a parameter or null  */
    //public String paramvalue;
    
    /**Name of the variable which refers a {@link UserFileset} which's files are used as input. */
    public String referVariable;
    
    public String srcpathEnvVariable;
    
    public String srcpathVariable;
    
    public String srcpathTargetInput;
    
    //UserFilepath srcpathInput;
    UserFilepath inputFile;
    
    
    
    TargetInput(UserTarget parentTarget){
      //super(parentTarget.script);
      this.parentTarget = parentTarget;
      //this.script = parentTarget.script;
    }
    
    
    public UserFilepath new_input()
    { inputFile = new UserFilepath(parentTarget.script);
      return inputFile; 
    }

    public void add_input(UserFilepath val){
      
    }


    
    
    //public UserFilepath new_srcpath(){ return srcpathInput = new UserFilepath(null);}
    //public void add_srcpath(UserFilepath val){}
    
    
    //UserInput(String inputSet){ this.inputSet = inputSet; this.inputFile = null; }
    //UserInput(UserInputSet inputSet){ this.inputSet = inputSet; this.inputFile = null; }
    //UserInput(UserFilepath inputFile){ this.inputSet = null; this.inputFile = inputFile; }
    
    @Override public String toString(){ return inputFile !=null ? inputFile.toString() : referVariable; }
    
  }
  
  
  /**A target processes files, especially input files or input and output files described with the same fileSet.
   * A target is a UserFileSet which combines all files of the input from all input file sets or separate files.
   * <pre>
   * target::=  [:<$?@target>:] 
      [ <specials?do>  ##action without dst file. 
      |
        ##<*|\ |\r|\n|:=?!prepOutputfile> :=  
        <output> :=
        [ for ( <input?> ) <routine?doForAll> 
        | <routine?>
        ##| exec <exec?>
        ##| <$?@translator> ( <input?> )
        ]
      ].

input::=
{ \$<inputSet>
| <""?!prepInputfile>
| \{ <target> \}
| srcpath = <""?!prepSrcpath>
| srcext = <""?srcext>
##| target = <""?@target>
##| task = <""?@target>
| <param>
| <*\ \r\n,)?!prepInputfile>
? , | +
}.

   * </pre>
   * @author Hartmut
   *
   */
  public final static class UserTarget //extends UserFileset
  {
    public final UserScript script;
    
    /**From ZBNF srcpath = <""?!prepSrcpath>. It is a part of the base path anyway. It may be absolute, but usually relative. 
     * If null then unused. */
    //UserFilepath srcpath;
    
    public UserFilepath output;
    
    public String name;
    
    public String translator;
    
    public List<TargetInput> inputs = new LinkedList<TargetInput>();
    
    public Map<String,TargetParam> params;
    
    UserTarget(UserScript script){
      this.script = script;
      //super(script);
    }
    
    //@Override
    //public UserFilepath new_srcpath(){ return srcpath = new UserFilepath(this); }
    
    //@Override
    //public void set_srcpath(UserFilepath value){  }
    
    /**From Zbnf: < output>.
     * @return instance to store the parse result of the component output::=
     */
    public UserFilepath new_output(){ return output = new UserFilepath(script); } //this); }
    
    /**From Zbnf: < output>.
     * @param instance contains the parse result of the component output::=
     */
    public void set_output(UserFilepath value){  }
    
    /**From ZBNF < inputSet>*
    public UserInputSet new_inputSet()
    { UserInput userInput = new UserInput(new UserInputSet(this));
      inputs.add(userInput);
      return userInput.inputSet; 
    }
    
    public void add_inputSet(UserInputSet value){  }
    */
    
    
    /**From ZBNF < param>. */
    public TargetParamZbnf new_param(){ return new TargetParamZbnf(this);}
    
    /**From ZBNF < param>. */
    public void add_param(TargetParamZbnf val){ 
      if(params ==null){ params = new TreeMap<String,TargetParam>(); }
      params.put(val.name, new TargetParam(val)); 
    }

    /**From ZBNF < ?!prepInputfile> < ?input>*/
    public TargetInput new_input()
    { TargetInput userInput = new TargetInput(this);
      inputs.add(userInput);
      return userInput; 
    }

    /**From ZBNF < ?!prepInputfile> < ?input>*/
    public void add_input(TargetInput val){}

    
    /**From Zbnf: < inputarg>.
     * @return instance to store the parse result of the component inputarg::=
     */
    public TargetInput new_inputarg()
    { TargetInput userInput = new TargetInput(this);
      return userInput; 
    }

    /**From Zbnf: < inputarg>.
     * @param instance contains the parse result of the component inputarg::=
     */
    public void add_inputarg(TargetInput val){ inputs.add(val);}

    
    /**FromZbnf: <$?inputSet>
     * An inputSet is a simple string which is the name of a script variable which should refer to a file {@link UserFileset}.
     * @param val
     */
    public void XXXset_inputSet(String val){
      //inputs.add(new UserInput(val));
    }
    
    public List<UserFilepath> allInputFiles(){
      return prepareInput(false);
    }
    
    /**Prepares the input of the target.
     * All input files and fileset which are not parameter are combined in one {@link TargetInput} which is used as the fileset
     * @param expandFiles
     * @return
     */
    public List<UserFilepath> allInputFilesExpanded(){
      return prepareInput(true);
    }
    
    
    /**Prepares the input of the target.
     * @param expandFiles
     * @return
     */
    private List<UserFilepath> prepareInput(boolean expandFiles) {
      List<UserFilepath> files = new LinkedList<UserFilepath>();
      //UserFileset inputfileset = null; 
      for(TargetInput targetInputParam: inputs){
        //UserFileset targetparaminputfileset; 
        { //if(name == null && targetInputParam.name == null || name !=null && name.equals(targetInputParam.name)){ //input or matching parameter?
          CharSequence srcpath = null;
          if(targetInputParam.srcpathEnvVariable !=null){
            srcpath = System.getenv(targetInputParam.srcpathEnvVariable);
            if(srcpath == null){
              if(script.bWriteErrorInOutputScript){
                srcpath = "<??missing environment variable: " + targetInputParam.srcpathEnvVariable + "??>";
              } else throw new IllegalArgumentException("Zmake - environment variable not found; " + targetInputParam.srcpathEnvVariable);
            }
          }
          else if(targetInputParam.srcpathVariable !=null){
            ScriptVariable var = script.var.get(targetInputParam.srcpathVariable);
            if(var == null){
              if(script.bWriteErrorInOutputScript){
                srcpath = "<??missing script variable: " + targetInputParam.srcpathVariable + "??>";
              } else throw new IllegalArgumentException("Zmake - script variable not found; " + targetInputParam.srcpathEnvVariable);
            } else {
              srcpath = var.text();
            }
          }
          if(targetInputParam.srcpathTargetInput !=null){
            if(srcpath == null){ srcpath = targetInputParam.srcpathTargetInput; }
            else {
              StringBuilder uSrcpath = (srcpath instanceof StringBuilder) ? (StringBuilder) srcpath : new StringBuilder(srcpath);
              uSrcpath.append(targetInputParam.srcpathTargetInput);
              srcpath = uSrcpath;
            }
          }
          if(srcpath == null){ srcpath = ""; }
          //
          //
          //expand file or fileset:
          //
          if(targetInputParam.inputFile !=null){
            if(expandFiles){
              targetInputParam.inputFile.expandFiles(files, srcpath, script.currDir);
            } else {
              UserFilepath targetsrc = new UserFilepath(script, targetInputParam.inputFile, srcpath);
              files.add(targetsrc);  
            }
          } else { 
            assert(targetInputParam.referVariable !=null);
            //search the input Set in the script variables:
            ScriptVariable variable = script.var.get(targetInputParam.referVariable);
            if(variable == null || variable.fileset == null){
              if(script.bWriteErrorInOutputScript){
                UserFilepath errorhint = new UserFilepath(script);
                errorhint.name = "<?error not found; " + targetInputParam.referVariable + ">";
                files.add(errorhint);
              }
            } else {
              variable.fileset.listFilesExpanded(files, srcpath);
            }
          }
        }
      }
      return files;
    }
    
    @Override public String toString(){
      return name + ":=" + translator;
    }
  
  }
  
  
  
  /**The main class of the data of the users script for zmake.
   * Filled from {@link org.vishia.zbnf.ZbnfJavaOutput}.
   *
   */
  public static class UserScript
  {
    int nextNr = 0;
    
    /**The current directory for access to all files which are necessary as absolute file.
     * It should be end with slash!
     */
    String sCurrDir = "";
    
    File currDir;
    
    boolean bWriteErrorInOutputScript = true;
    
    //Map<String, String> currDir = new TreeMap<String, String>();
    
    Map<String, ScriptVariable> var = new TreeMap<String, ScriptVariable>();
    
    List<UserTarget> targets = new LinkedList<UserTarget>();
    
    /**From ZBNF: < variable> */
    public ScriptVariable new_variable(){ return new ScriptVariable(this); }
    
    public void add_variable(ScriptVariable  value){ var.put(value.name, value); }
    
    public UserTarget new_target(){ return new UserTarget(this); }
    
    public void add_target(UserTarget value){ targets.add(value); }
    
    /**This method can be called from a user script.
     * @return an incremented number started from 1.
     */
    public String nextNr(){
      nextNr +=1;
      return Integer.toString(nextNr);
    }
    
    
    /**This method should be called after parsing the input script maybe from information from the script
     * maybe inside a text generating script.
     * @param sDrive If null or "" then sets the main current directory for paths without local drive.
     *   Elsewhere it should be a drive designation.
     * @param sDir The directory path.
     */
    public void setCurrentDir(File dir){
      sCurrDir = FileSystem.getCanonicalPath(dir) + "/";
      currDir = dir;
      /*
      sDir = sDir.replace('\\', '/');  //use slash internally.
      if(!sDir.endsWith("/")){
        sDir += "/";
      }
      if(sDrive == null || sDrive.length() == 0){
        sCurrDir = sDir;
      } else {
        currDir.put(sDrive, sDir);
      }
      */
    }
    
    
    /**This routine does nothing,if {@link #bWriteErrorInOutputScript} is set.
     * elsewhere it throws an {@link IllegalArgumentException}
     * @param msg The msg
     * @param fromPos substring from this position.
     */
    void abortOnError(CharSequence msg, int fromPos){
      if(bWriteErrorInOutputScript){
        throw new IllegalArgumentException(msg.subSequence(fromPos, msg.length()).toString());
      }
    }
    
  }

}
