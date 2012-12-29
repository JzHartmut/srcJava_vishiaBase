package org.vishia.zmake;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.vishia.util.Assert;
import org.vishia.util.FileSystem;


public class ZmakeUserScript
{
  
  /**Version, history and license.
   * <ul>2012-12-29 Hartmut chg: A {@link UserFilepath} is independent from a target and describes a non-completely relative path usually.
   *   The path is completed, usual as absolute path, if the {@link UserFileSet} is used in a target. The {@link TagetInput} of a target
   *   determines the location of the file set by its {@link TagetInput#srcpathInput}. The {@link UserFilepath} of all inputs are cloned
   *   and completed with that srcpath for usage. The {@link UserTarget#allInputFiles()} or {@link UserTarget#allInputFilesExpanded()}
   *   builds that list. Usage of that files provide the correct source path for the files of a target's input.
   * <li>2012-12-08 Hartmut chg: improve access rotoutines.
   * <li>2012-11-29 Hartmut new: The {@link UserFilePath} now contains all access routines to parts of the file path
   *   which were contained in the ZmakeGenerator class before. This allows to use this with the common concept
   *   of a text generator using {@link org.vishia.textGenerator.TextGenerator}. That Textgenerator does not know such
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
   * <li><b>Drive letter or select path<b>: On windows systems a drive letter can be used in form <code>A:</code>.
   *   The path should not be absolute. For example <code>"X:mypath\file.ext"</code> describes a file starting from the 
   *   current directory of the <code>X</code> drive. Any drive has its own current directory. A user can use this capability
   *   of windows to set different current directories in special maybe substitute drives.
   * <li><b>Drive letter as select path<b>:  
   *   It may be possible (future extension) to use this capability independent of windows in this class. 
   *   For that the {@link #parent} can have some paths associated to drive letters with local meaning,
   *   If the path starts with a drive letter, the associated path is searched in the parents drive list. 
   * <li><b>Absolute path</b>: If this entry starts with slash or backslash, maybe after a drive designation for windows systems,
   *   it is an absolute path. Elsewhere the parent's general path can be absolute. If an absolute path is requested
   *   calling {@link #absFile()} or adequate and the path is not given as absolute path, then the current directory is used
   *   as prefix for the path. The current directory is a property of the {@link UserScript#sCurrDir}. The current directory
   *   of the operation system is not used for that. 
   * <li><b>opeation systems current directory<b>: In opposite if you generate a relative path and the executing system
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
    
    /**From ZBNF: $<$?scriptVariable>. If given, then the {@link #basePath()} starts with it. It is an absolute path then. */
    public String scriptVariable;
    
    public String drive;
    /**From Zbnf: [ [/|\\]<?@absPath>]. Set if the path starts with '/' or '\' maybe after drive letter. */
    public boolean absPath;
    
    /**Path-part before a ':'. */
    public String pathbase;
    
    /**Localpath after ':' or the whole path. */
    public String path = "";
    
    /**From Zbnf: The filename without extension. */
    public String file = "";
    
    
    /**From Zbnf: The extension inclusive the leading dot. */
    public String ext = "";
    
    boolean allTree, someFiles, wildcardExt;
    
    private static UserFilepath emptyParent = new UserFilepath(null);
    
    UserFilepath(UserFileset parent){
      this.parent = parent;
    }
    
    public void set_someFiles(){ someFiles = true; }
    public void set_wildcardExt(){ wildcardExt = true; }
    public void set_allTree(){ allTree = true; }
    
    String getPath(){ return file; }
    
    /**Method can be called in the generation script: <*path.localPathName()>. 
     * @return the local path part with file without extension.
     */
    public CharSequence localPathName(){ 
      StringBuilder uRet = new StringBuilder(path);
      uRet.append(file);
      return uRet; 
    }
    
    public CharSequence localPathNameW(){ return toWindows(localPathName()); }

    
    /**Method can be called in the generation script: <*path.localFile()>. 
     * @return the local path to this file.
     */
    public CharSequence localFile(){ 
      StringBuilder uRet = new StringBuilder();
      return addLocalFile(uRet);
    }

    public CharSequence localFileW(){ return toWindows(localFile()); }

    
    private CharSequence addLocalFile(StringBuilder uRet){ 
      int pos;
      if( (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
      uRet.append(this.path);
      uRet.append(this.file);
      if(this.someFiles){ uRet.append('*'); }
      if(this.wildcardExt){ uRet.append(".*"); }
      uRet.append(this.ext);
      return uRet;
    }
    
    
    /**Method can be called in the generation script: <*path.localDir()>. 
     * @return the local path part with file without extension.
     */
    public String localDir(){ return path; }
    
    /**Method can be called in the generation script: <*path.localDir()>. 
     * @return the local path part with file without extension.
     */
    public String localDirW(){ return path.replace('/', '\\'); }
    
    /**Method can be called in the generation script: <*path.localPathName()>. 
     * @return the local path part with file without extension.
     * @deprecated use {@link #localDir()}
     */
    @Deprecated public String localPath(){ return path; }
    
    /**Method can be called in the generation script: <*path.localPathName()>. 
     * @return the local path part with file without extension.
     */
    public String name(){ return file; }
    
    /**Method can be called in the generation script: <*path.localPathName()>. 
     * @return the local path part with file without extension.
     */
    public CharSequence nameExt(){ StringBuilder uRet = new StringBuilder(); return uRet.append(file).append(ext); }
    
    /**Method can be called in the generation script: <*path.localPathName()>. 
     * @return the local path part with file without extension.
     */
    public String ext(){ return ext; }
    
    
    /**Method can be called in the generation script: <*basePath()>. 
     * @return the whole base path inclusive a given general path in a {@link UserFileSet}.
     *   till a ':' in the input path or an empty string.
     *   Either as absolute or as relative path how it is given.
     */
    public CharSequence basePath(){ return basePath(null); }
     
    
    /**Method can be called in the generation script: <*basePath(<*abspath>)>. 
     * @param abspath a String given path which is written before the given base path if the path is not absolute in this.
     *   If null, it is ignored. If this path is absolute, the result is a absolute path of course.
     * @return the whole base path inclusive a given general path in a {@link UserFileSet}.
     *   till a ':' in the input path or an empty string.
     *   Either as absolute or as relative path how it is given.
     */
    public CharSequence basePath(String abspath){ 
      UserFilepath generalPath = parent !=null && parent.srcpath !=null ? parent.srcpath : emptyParent;
      if(pathbase !=null || (generalPath.pathbase !=null)){
        StringBuilder uRet = new StringBuilder();
        if(this.drive !=null){ uRet.append(this.drive).append(':'); }
        else if(generalPath.drive !=null){
          uRet.append(parent.srcpath.drive).append(':'); 
        }
        if(absPath){ uRet.append('/'); }
        else if(generalPath.absPath){ uRet.append('/'); }
        else if(abspath !=null){
          uRet.append(abspath);
        }
        int pos;
        //
        //append a general path completely firstly.
        if(generalPath.pathbase !=null){
          if( (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
          uRet.append(generalPath.pathbase).append('/'); 
        }
        if( (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
        uRet.append(generalPath.path).append(generalPath.file).append(generalPath.ext);
        if(this.pathbase !=null){
          if( (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
          uRet.append(this.pathbase);
        }
        return uRet;
      } else {
        return abspath !=null ? abspath : "";
      }
    }
    
    
    
    public CharSequence basePathW(){ return toWindows(basePath()); }
    
    
    /**Method can be called in the generation script: <*absBasePath()>. 
     * @return the whole path inclusive a given general path in a {@link UserFileSet}.
     *   Either as absolute or as relative path.
     */
    public CharSequence absBasePath(){ 
      CharSequence basePath = basePath();
      if(this.absPath){ return basePath; }
      else if(basePath instanceof StringBuilder){
        //not an absolute path, complete it with the global given base path:
        StringBuilder uRet = (StringBuilder)basePath;
        String sCurrDir = parent.script.sCurrDir;
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
          uRet.insert(0, parent.script.sCurrDir);
        }
        return uRet;
      }
      else {
        //this has no basepath, then return the current dir.
        return parent.script.sCurrDir;
      }
    }
    
    
    public CharSequence absBasePathW(){ return toWindows(absBasePath()); }
    
    /**Method can be called in the generation script: <*path.file()>. 
     * @return the whole path inclusive a given general path in a {@link UserFileSet}.
     *   Either as absolute or as relative path.
     */
    public CharSequence file(){ 
      CharSequence basePath = basePath();
      StringBuilder uRet = basePath instanceof StringBuilder ? (StringBuilder)basePath : new StringBuilder(basePath);
      return addLocalFile(uRet);
    }
    
    public CharSequence fileW(){ return toWindows(file()); }
    
    
    
    /**Method can be called in the generation script: <*path.file()>. 
     * @return the whole path inclusive a given general path in a {@link UserFileSet}.
     *   The path is absolute. If it is given as relative path, the general current directory of the script is used.
     */
    public CharSequence absFile(){ 
      CharSequence basePath = absBasePath();
      StringBuilder uRet = basePath instanceof StringBuilder ? (StringBuilder)basePath : new StringBuilder(basePath);
      return addLocalFile(uRet);
    }
    
    public CharSequence absFileW(){ return toWindows(absFile()); }
    
    
    /**Method can be called in the generation script: <*path.absDir()>. 
     * @return the whole path to the parent of this file inclusive a given general path in a {@link UserFileSet}.
     *   The path is absolute. If it is given as relative path, the general current directory of the script is used.
     */
    public CharSequence dir(){ 
      CharSequence basePath = basePath();
      StringBuilder uRet = basePath instanceof StringBuilder ? (StringBuilder)basePath : new StringBuilder(basePath);
      int pos;
      if( (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
      uRet.append(path);
      return uRet;
    }
    
    public CharSequence dirW(){ return toWindows(dir()); }
    
    
    /**Method can be called in the generation script: <*path.absDir()>. 
     * @return the whole path to the parent of this file inclusive a given general path in a {@link UserFileSet}.
     *   The path is absolute. If it is given as relative path, the general current directory of the script is used.
     */
    public CharSequence absDir(){ 
      CharSequence basePath = absBasePath();
      StringBuilder uRet = basePath instanceof StringBuilder ? (StringBuilder)basePath : new StringBuilder(basePath);
      int pos;
      if( (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
      uRet.append(path);
      return uRet;
    }
    
    public CharSequence absDirW(){ return toWindows(absDir()); }
    
    
    private CharSequence toWindows(CharSequence inp)
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
    static void expandFiles(List<UserFilepath> listToadd, UserFilepath filepathWildcards, CharSequence srcpath, File currdir){
      List<FileSystem.FileAndBasePath> listFiles = new LinkedList<FileSystem.FileAndBasePath>();
      final CharSequence basePath = srcpath.toString() + "/" + filepathWildcards.basePath(); //getPartsFromFilepath(file, null, "absBasePath").toString();
      final CharSequence localfilePath = filepathWildcards.localFile(); //getPartsFromFilepath(file, null, "file").toString();
      final String sPathSearch = basePath + ":" + localfilePath;
      try{ FileSystem.addFilesWithBasePath(currdir, sPathSearch, listFiles);
      } catch(FileNotFoundException exc){
        //let it empty.
      }
      for(FileSystem.FileAndBasePath file1: listFiles){
        ZmakeUserScript.UserFilepath filepath2 = new ZmakeUserScript.UserFilepath(null);
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
        filepath2.file = sName;
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
    List<UserFilepath> filesOfFilesetExpanded;
    
    UserFileset(UserScript script){
      this.script = script;
    }
    
    
    public UserFileset(){
      this.script = null;
    }
    
    
    //void setTargetInputParam(UserInput input){ this.targetinput = input; }
    void setTargetInputParam(){  }
    
    
    /**From Zbnf: srcpath = <""?!prepSrcpath>.
     * It sets the base path for all files of this fileset. This basepath is usually relative.
     * @return ZBNF component.
     */
    public UserFilepath new_srcpath(){ return srcpath = new UserFilepath(null); }  //NOTE: it has not a parent. this is not its parent!
    public void set_srcpath(UserFilepath val){  }  //it is set already.
    
    /**From ZBNF: < file>. */
    public UserFilepath new_file(){ return new UserFilepath(this); }
    
    /**From ZBNF: < file>. */
    public void add_file(UserFilepath val){ 
      if(val.pathbase !=null || val.path.length() >0 || val.file.length() >0 || val.someFiles || val.drive !=null){
        //only if any field is set. not on empty val
        filesOfFileset.add(val); 
      }
    }
    
    

    
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
    
    public CharSequence getText()
    {
      StringBuilder u = new StringBuilder();
      for(UserStringContent el: content){
        switch(el.type){
        case '"': u.append(el.text);
        }
      }
      return u;
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
    public UserString string;
    
    ScriptVariable(UserScript script){
      this.script = script;
    }
    public UserFileset new_fileset(){ return fileset = new UserFileset(script); }
    public void add_fileset(UserFileset val){ }
    public UserString new_string(){ return string = new UserString(); }
    public void set_string(UserString val){} //left empty, is set already.
    
    /**For textscript: <*var.name.files()>
     * @return The List of files. 
     */
    public List<UserFilepath> files(){ return (fileset !=null) ? fileset.filesOfFileset : null; }
    
    
    /**For textscript: <*var.name.filesExpanded()>
     * @return The List of files. 
     */
    public List<UserFilepath> filesExpanded(String absPath){
      if(fileset == null){ return null; }
      else {
        List<UserFilepath> listToadd = new LinkedList<UserFilepath>();
        if(fileset.filesOfFilesetExpanded ==null){
          int lengthPathCurrdir = fileset.script.sCurrDir.length();
          for(ZmakeUserScript.UserFilepath filepathWildcards: fileset.filesOfFileset){
            
            UserFilepath.expandFiles(listToadd, filepathWildcards, absPath, script.currDir);
          }
          
        }
        return listToadd;
      }
    }
    
    
    @Override public String toString(){
      return name + ":" + (string !=null ? string : fileset);
    }
    
    
    /**For textscript: <*var.name.text()>
     * @return The text of the script variable. 
     */
    public CharSequence text(){ return string !=null ? string.getText() : ""; }
    
  }
  
  public static class UserParam
  { public String name;
    public String value;
    public String variable;
  }
  
  /**One of possible more inputs for a target.
   */
  public static class XXXUserInputSet
  { /**The main script. */
    final UserScript script;
    /**The target where this input set is used. */
    final UserTarget parentTarget;
  
    public String name;
    UserFilepath srcpath;
    XXXUserInputSet(UserTarget parentTarget){
      this.parentTarget = parentTarget;
      this.script = parentTarget.script;
    }
    public UserFilepath new_srcpath(){ return srcpath = new UserFilepath(null); }
    public void set_srcpath(UserFilepath val){  }
    
    @Override public String toString(){ return "$"+name + ":" + srcpath; }
  }
  
  
  /**Describes 1 input item for a target, maybe a file, maybe a inputset. */
  public static class TagetInput
  { //final UserInputSet inputSet;
    //final String inputSet;
    /**The main script. */
    //final UserScript script;
    /**The target where this input set is used. */
    final UserTarget parentTarget;
  
    /**Name of the variable which refers a {@link UserFileset} which's files are used as input. */
    public String filesetVariable;
    
    public String srcpathEnvVariable;
    
    public String srcpathVariable;
    
    public String srcpath;
    
    //UserFilepath srcpathInput;
    UserFilepath inputFile;
    
    
    
    TagetInput(UserTarget parentTarget){
      this.parentTarget = parentTarget;
      //this.script = parentTarget.script;
    }
    
    //public UserFilepath new_srcpath(){ return srcpathInput = new UserFilepath(null);}
    //public void add_srcpath(UserFilepath val){}
    
    
    //UserInput(String inputSet){ this.inputSet = inputSet; this.inputFile = null; }
    //UserInput(UserInputSet inputSet){ this.inputSet = inputSet; this.inputFile = null; }
    //UserInput(UserFilepath inputFile){ this.inputSet = null; this.inputFile = inputFile; }
    
    @Override public String toString(){ return inputFile !=null ? inputFile.toString() : filesetVariable; }
    
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
    final UserScript script;
    
    /**From ZBNF srcpath = <""?!prepSrcpath>. It is a part of the base path anyway. It may be absolute, but usually relative. 
     * If null then unused. */
    //UserFilepath srcpath;
    
    UserFilepath output;
    
    public String name;
    
    public String translator;
    
    List<TagetInput> inputs = new LinkedList<TagetInput>();
    
    Map<String,UserParam> params;
    
    UserTarget(UserScript script){
      this.script = script;
      //super(script);
    }
    
    //@Override
    //public UserFilepath new_srcpath(){ return srcpath = new UserFilepath(this); }
    
    //@Override
    //public void set_srcpath(UserFilepath value){  }
    
    public UserFilepath new_output(){ return output = new UserFilepath(null); } //this); }
    
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
    public UserParam new_param(){ return new UserParam();}
    
    /**From ZBNF < param>. */
    public void add_param(UserParam val){ 
      if(params ==null){ params = new TreeMap<String,UserParam>(); }
      params.put(val.name, val); 
    }
    
    /**From ZBNF < ?!prepInputfile> < ?input>*/
    public TagetInput new_input()
    { TagetInput userInput = new TagetInput(this);
      inputs.add(userInput);
      return userInput; 
    }

    public void add_input(TagetInput val){}

    
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
    
    public List<UserFilepath> allInputFilesExpanded(){
      return prepareInput(true);
    }
    
    
    private List<UserFilepath> prepareInput(boolean expandFiles) {
      List<UserFilepath> files = new LinkedList<UserFilepath>();
      for(TagetInput targetInputParam: inputs){
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
        if(targetInputParam.srcpath !=null){
          if(srcpath == null){ srcpath = targetInputParam.srcpath; }
          else {
            StringBuilder uSrcpath = (srcpath instanceof StringBuilder) ? (StringBuilder) srcpath : new StringBuilder(srcpath);
            uSrcpath.append(targetInputParam.srcpath);
            srcpath = uSrcpath;
          }
        }
        if(srcpath == null){ srcpath = ""; }
        if(targetInputParam.inputFile !=null){
          if(expandFiles){
            UserFilepath.expandFiles(files, targetInputParam.inputFile, srcpath, script.currDir);
          } else {
            //clone filepath! add srcpath
            files.add(targetInputParam.inputFile);
          }
        } else { 
          assert(targetInputParam.filesetVariable !=null);
          //search the input Set in the script variables:
          ScriptVariable variable = script.var.get(targetInputParam.filesetVariable);
          if(variable == null || variable.fileset == null){
            Assert.stop();
          } else {
            variable.fileset.setTargetInputParam(); //targetInputParam);  //with it the abspath is determined.
            for(UserFilepath filepath: variable.fileset.filesOfFileset){
              if(expandFiles){
                UserFilepath.expandFiles(files, filepath, srcpath, script.currDir);
              } else {
                //clone filepath! add srcpath
                files.add(filepath);
              }
            }
          }
        }
      }
      return files;
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
      sCurrDir = FileSystem.getCanonicalPath(dir);
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
    
  }

}
