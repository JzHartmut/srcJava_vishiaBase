package org.vishia.zmake;

import java.io.File;
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
   * <li>2013-02-15 Hartmut chg/new: More functionality for UserFilePath, essential improved. continue work and test ...
   * <li>2013-02-10 Hartmut chg/new: UserFilepath#parent removed: A UserFilepath need not know a fileset where it is member of
   *   because the files can be arranged from more as one fileset with another basepath etc. The fileset with its is 
   *   {@link UserFileset#commonBasepath} (renamed from 'srcpath') is known if that is evaluated only.
   *   {@link UserFilepath#expandFiles(List, UserFilepath, CharSequence, File)} 
   *   and {@link UserFilepath#UserFilepath(UserScript, UserFilepath, UserFilepath, CharSequence)}
   *   has gotten the additional arguments commonBasepath and accesspath to build the necesarry information.
   * <li>2013-02-10 Hartmut chg: The element in the ZBNF-syntax <code>basepath = < file?basepath>|...</code> renamed
   *   from 'srcpath' (old name) and re-provided to usage.
   * <li>2013-02-10 Hartmut chg: {@link UserFileset}.srcext removed (it was necessary in the past while some XML translations
   *   were done in Zmake).    
   * <li>2013-02-10 Hartmut new: {@link UserTarget#allParamFiles(String)} and ...Expanded(paramName)    
   * <li>2013-02-10 Hartmut new: {@link UserFilepath#file(CharSequence)}   
   * <li>2013-02-09 Hartmut new: {@link UserFilepath#base_localfile()}
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
   *   knows its file-set where it is contained in, that is {@link UserFilepath#itsFileset}. 
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
   *   For that the {@link #itsFileset} can have some paths associated to drive letters with local meaning,
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
  public final static class UserFilepath
  {
    /**Aggregation to a given {@link UserFileset} where this is member of. 
     * A {@link UserFileset#commonBasepath} which is valid for all files of the {@link #itsFileset} is gotten from there, 
     * if it is given (not null).
     * <br> 
     * This aggregation can be null, especially if this is a member of a list returned from
     * {@link UserTarget#allInputFiles()} if more as one fileSets are used as the target's input or an accessPath is given.
     * In that kind the {@link UserFileset#filesOfFileset} are cloned without this aggregation and the commonBasePath
     * and the accessPath are part of the {@link #basepath} of this.
     */
    //private final UserFileset itsFileset;
    
    private final UserScript script;
    
    /**From ZBNF: $<$?scriptVariable>. If given, then the {@link #basePath()} starts with it. It is an absolute path then. */
    String scriptVariable, envVariable;
    
    String drive;
    /**From Zbnf: [ [/|\\]<?@absPath>]. Set if the path starts with '/' or '\' maybe after drive letter. */
    boolean absPath;
    
    /**Path-part before a ':'. It is null if the basepath is not given. */
    String basepath;
    
    /**Localpath after ':' or the whole path. It is an empty "" if a directory is not given. 
     * It ends with slash if any content is given. */
    String localdir = "";
    
    /**From Zbnf: The filename without extension. */
    String name = "";
    
    
    /**From Zbnf: The extension inclusive the leading dot. */
    String ext = "";
    
    boolean allTree, someFiles;
    
    /**An empty file path which is used as argument if a common base path is not given. */
    static UserFilepath emptyParent = new UserFilepath();
    
    private UserFilepath(){
      this.script = null;
    }
    
    
    UserFilepath(UserScript script){
      this.script = script;
    }
    
    /**Creates a UserFilepath entry with an additonal pathbase.
     * if the basepath of src is given and the pathbase0 is given, both are joined: pathbase0/src.pathbase.
     * @param script  Reference to the script, necessary for the current directory
     * @param src The source (clone source)
     * @param basepath An additional basepath usual stored as <code>basepath=path, ...</code> in a fileset, maybe null
     * @param pathbase0 additional pre-pathbase before base, maybe null
     */
    UserFilepath(UserScript script, UserFilepath src, UserFilepath commonPath, UserFilepath accessPath){
      CharSequence basePath = src.basepath(null, commonPath, accessPath, null);
      CharSequence localDir = src.localDir(null, commonPath, accessPath);
      int posbase = isRootpath(basePath);
      this.drive = posbase >=2 ? Character.toString(basePath.charAt(0)) : null;
      this.absPath = posbase == 1 || posbase == 3;
      this.basepath = basePath.subSequence(posbase, basePath.length()).toString();
      this.script = script;
      this.localdir = localDir.toString();
      this.name = src.name;
      this.ext = src.ext;
      this.allTree = localdir.indexOf('*') >=0;
      this.someFiles = src.someFiles;
    }
    

    
    /**Inserts the given drive letter and the root designation on start of buffer. It does nothing if the path is relative.
     * @param u The buffer
     * @param commonBasepath An common base path in fileset
     * @param accesspath An access path while using a fileset
     * @return true if it is a root path or it has a drive letter.
     */
    CharSequence driveRoot(CharSequence basepath, UserFilepath commonBasepath, UserFilepath accesspath){
      boolean isRoot = false;
      CharSequence ret = basepath;
      if(absPath || commonBasepath !=null && commonBasepath.absPath || accesspath !=null && accesspath.absPath){ 
        StringBuilder u = basepath instanceof StringBuilder? (StringBuilder)basepath : new StringBuilder(basepath);
        ret = u;
        u.insert(0, '/'); 
        isRoot = true; 
      }
      if(drive !=null){
        StringBuilder u = basepath instanceof StringBuilder? (StringBuilder)basepath : new StringBuilder(basepath);
        ret = u;
        u.insert(0, drive).insert(1, ':'); 
        isRoot = true;
      }
      else if(commonBasepath !=null && commonBasepath.drive !=null){
        StringBuilder u = basepath instanceof StringBuilder? (StringBuilder)basepath : new StringBuilder(basepath);
        ret = u;
        u.insert(0, commonBasepath.drive).insert(1, ':'); 
        isRoot = true;
      }
      else if(accesspath !=null && accesspath.drive !=null){
        StringBuilder u = basepath instanceof StringBuilder? (StringBuilder)basepath : new StringBuilder(basepath);
        ret = u;
        u.insert(0, accesspath.drive).insert(1, ':'); 
        isRoot = true;
      }
      return ret;
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
    static int isRootpath(CharSequence textPath){  ///
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
     * <br><br>
     * If this file contains a basepath, all other access, common and a variable is used as full file path
     * as prefix of this base path. If this doesn't contain a basepath, either the common and access path presents
     * the base path, or of one of them contains a basepath, that is the basepath. This behavior is complementary 
     * to the behavior of {@link #localDir(StringBuilder, UserFilepath, UserFilepath)}.
     * <br><br>
     * The following true table shows the constellation possibilities and there outputs.
     * <ul>
     * <li>common: represents the common and/or the access path.
     * <li>varfile: represents a {@link #scriptVariable} maybe with a {@link ScriptVariable#filepath}
     *   or the textual content of a {@link #scriptVariable} or the {@link #envVariable}.
     *   Only a {@link ScriptVariable#filepath} can represent a <code>base</code>.
     *   An environment variable can represent a <code>abs</code>, it is checked with {@link #isRootpath(CharSequence)}.
     * <li>The common or variable reference can be given (1) or it is null (0).
     * <li>base: The element has a basepath and a local part.
     * <li>abs: The element is given as absolute path
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
     * the {@link #localFile(StringBuilder, UserFilepath, UserFilepath)} is added too.
     *         
     * @param uRet If not null then append the result in it.  
     * @param generalPath if not null then its file() is used as part before the own basepath
     *   but only if this is not an absolute path.
     * @param accesspath a String given path which is written before the given base path if the path is not absolute in this.
     *   If null, it is ignored. If this path is absolute, the result is a absolute path of course.
     * @param useBaseFile null or false, the return the basepath only. 
     *   true then returns the local path part if a base path is not given inside. This element is set to false
     *   if the element has a base path and therefore the local path part of the caller should not be added.    
     * @return the whole base path of the constellation.
     *   Either as absolute or as relative path how it is given.
     *   The return instance is the given uRet if uRet is not null. 
     *   It is a StringBuilder if the path is assembled from more as one parts.
     *   It is a String if uRet is null and the basepath is simple.
     *   A returned StringBuilder may be used to append some other parts furthermore.
     */
    protected CharSequence basepath(StringBuilder uRet, UserFilepath commonPath, UserFilepath accessPath, boolean[] useBaseFile){ 
      //if(generalPath == null){ generalPath = emptyParent; }
      //first check singulary conditions
      ///
      int pos;
      ScriptVariable var;
      UserFilepath varfile;
      if((basepath !=null || useBaseFile !=null && useBaseFile[0]) && scriptVariable !=null){
        //get the variable if a base path is given or the file may be used as base path
        var = script.var.get(scriptVariable);
        varfile = var.filepath;
      } else { 
        var = null;
        varfile = null;
      }
      if(this.absPath){
        //  common    variable    this         basepath build with         : localdir build with   
        //  | base    | base abs  base abs
        //  x  x      x  x   x     1    1      /thisBase                   : thisLocal
        //  x  x      x  x   x     0    1      "/"                         : thisLocal
        if(drive !=null || basepath !=null || uRet !=null || var !=null || envVariable !=null){
          if(uRet == null){ uRet = new StringBuilder(); }  //it is necessary.
          else { uRet.setLength(0); }
          if(drive !=null){ 
            uRet.append(drive).append(":");
          }
          uRet.append("/");
          if(basepath !=null){ 
            if(useBaseFile !=null){ useBaseFile[0] = false; }  //designate it to the caller
            uRet.append(basepath);
          } else if(useBaseFile !=null && useBaseFile[0]) {
            addLocalName(uRet);
          }
          return uRet;
        } else {
          assert(uRet == null && basepath ==null && drive == null);
          return "/";
        }
      }
      else if(this.basepath !=null){
        if(useBaseFile !=null){ useBaseFile[0] = false; }  //designate it to the caller
        //
        //  common    variable    this         basepath build with         : localdir build with   
        //  | base    | base abs  base abs
        //  x         1  x   1     1    0      /varFile + thisBase         : thisLocal
        //  0         1  x   0     1    0      varFile + thisBase
        //  1  x      1  x   0     1    0      commonFile + varFile + thisBase : thisLocal
        //  0         0            1    0      thisBase                    : thisLocal
        //  1  x      0            1    0      commonFile + thisBase       : thisLocal
        if(commonPath !=null || accessPath !=null || var !=null || envVariable !=null){
          //  common    variable    this         basepath build with         : localdir build with   
          //  | base    | base abs  base abs
          //  x         1  x   1     1    0      /varFile + thisBase         : thisLocal
          //  0         1  x   0     1    0      varFile + thisBase
          //  1  x      1  x   0     1    0      commonFile + varFile + thisBase : thisLocal
          //  1  x      0            1    0      commonFile + thisBase       : thisLocal
          CharSequence prepath;
          
          if(varfile !=null){
            //  common    variable    this         basepath build with         : localdir build with   
            //  | base    | base abs  base abs
            //  x         1  x   1     1    0      /varFile + thisBase         : thisLocal
            //  0         1  x   0     1    0      varFile + thisBase
            //  1  x      1  x   0     1    0      commonFile + varFile + thisBase : thisLocal
            prepath = varfile.file(uRet, commonPath, accessPath);
          }
          else if(commonPath !=null){
            //  common    variable    this         basepath build with         : localdir build with   
            //  | base    | base abs  base abs
            //  1  x      0            1    0      commonFile + thisBase       : thisLocal
            prepath = commonPath.file(uRet, accessPath);
          } 
          else if(accessPath !=null){
            //  common    variable    this         basepath build with         : localdir build with   
            //  | base    | base abs  base abs
            //  1  x      0            1    0      commonFile + thisBase       : thisLocal
            prepath = accessPath.file(uRet, null);
          } else {
            //possible to have a variable with text or environment variable.
            prepath = uRet;
          }
          if(basepath.length() >0 || var !=null && varfile == null || envVariable !=null){
            //need to add somewhat, build the StringBuilder if not done.
            if(prepath instanceof StringBuilder){
              uRet = (StringBuilder)prepath;
            } else {
              assert(uRet == null);  //elsewhere it might be used for prepath
              uRet = prepath !=null ? new StringBuilder(prepath) : new StringBuilder();
            }
          }
          final CharSequence text;
          if(var !=null && varfile == null){
            text = var.text();
          }  
          else if(envVariable !=null){
            text = System.getenv(envVariable);
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
          if(basepath.length() ==0){
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
          return basepath;
        }
      } else { 
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
        boolean[] useBaseFileSub = new boolean[1];
        useBaseFileSub[0] = true;  //use the file of commonPath or accessPath as base path. 
        if(varfile !=null && (varfile.basepath !=null || useBaseFile !=null && useBaseFile[0])){
          //use the variableFile if it is called recursively. 
          //The variable is that one from commonPath or accessPath of the caller.
          //
          //  common    variable    this         basepath build with         : localdir build with   
          //  | base    | base abs  base abs
          //  x  x      1  1   1     0    0      /variableBase               : varLocalFile + thisLocal

          //  0  x      1  1   0     0    0      variableBase                : varLocalFile + thisLocal
          //  1  x      1  1   0     0    0      commonFile + variableBase   : varLocalFile + thisLocal
          prepath = varfile.basepath(uRet, commonPath, accessPath, useBaseFileSub);
        }
        else if(commonPath !=null){
          //  common    variable    this         basepath build with         : localdir build with   
          //  | base    | base abs  base abs
          //  1  0      0            0    0      commonFile                  : thisLocal
          //  1  1      0            0    0      commonBase                  : commonLocalfile + thisLocal
          prepath = commonPath.basepath(uRet, null, accessPath, useBaseFileSub);
        } 
        else if(accessPath !=null){
          //  common    variable    this         basepath build with         : localdir build with   
          //  | base    | base abs  base abs
          //  1  0      0            0    0      commonFile                  : thisLocal
          //  1  1      0            0    0      commonBase                  : commonLocalfile + thisLocal
          prepath = accessPath.basepath(uRet, null, null, useBaseFileSub);
        } 
        else {
          //  common    variable    this         basepath build with         : localdir build with   
          //  | base    | base abs  base abs
          //  0  x      0            0    0      ""                          : thisLocal
          if(uRet !=null){ prepath = uRet; }
          else prepath = "";
        }
        if(useBaseFileSub[0] && useBaseFile !=null && useBaseFile[0]) {
          //it is called recursively, therefore use the file as base part, and the left element has not a base part.
          if(!(prepath instanceof StringBuilder)){
            assert(uRet == null);   //if it is not null, it is used for prepath.
            uRet = new StringBuilder(prepath);
          }
          return addLocalName(uRet);
        } else {
          return prepath;
        }
      }
    }
    
    

    

    
    /**Gets the local directory path.
     * <ul>
     * <li>If this instance has a basepath, the local directory is the local directory path part of this.
     *   In this case the generalPath and the accesspath are not used.
     * <li>If this has not a basepath but the generalPath is split in a basepath and a localpath,
     *   the local path part of the generalPath is used as local directory path too.
     * <li>If this has not a basepath and the generalPath hasn't a basepath or it is null 
     *   but the accesspath is split in a basepath and a local path,
     *   the local path part of the accessPath and the whole generalPath is used as local directory path too.
     * <li>If this has not a basepath but both the generalPath and the accessPath has not a basepath too,
     *   only this {@link #localdir} is returned as local directory path. In this case the accessPath and the generalPath
     *   acts as basepath.
     * </ul>  
     * If the 
     * @param uRet If not null, then the local directory path is appended to it and uRet is returned.
     * @param generalPath
     * @param accessPath
     * @return Either the {@link #localdir} as String or a StringBuilder instance. If uRet is given, it is returned.
     */
    public CharSequence localDir(StringBuilder uRet, UserFilepath commonPath, UserFilepath accessPath){
      ///
      if(  basepath !=null 
        || commonPath == null && accessPath == null && scriptVariable == null && envVariable == null){
        if(uRet == null){
          return localdir;
        } else {
          uRet.append(localdir);
          return uRet;
        }
      }
      else {
        assert(basepath == null);
        if(uRet ==null){ uRet = new StringBuilder(); } //it is necessary. Build it if null.
        ScriptVariable var;
        UserFilepath varfile;
        if(scriptVariable !=null){
          var = script.var.get(scriptVariable);
          varfile = var.filepath;
        } else { 
          var = null;
          varfile = null;
        }
        if(varfile !=null){
          varfile.localFile(uRet, commonPath, accessPath);  //The full varfile.localfile should use.
        }
        else if(commonPath !=null){
          commonPath.localFile(uRet, null, accessPath);
        } 
        else if(accessPath !=null){
          accessPath.localFile(uRet, null, null);
        }
        //
        if(var !=null && var.filepath == null){
          CharSequence varpath = var.text();
          uRet.append(varpath);
        }
        if(envVariable !=null){
          CharSequence varpath = System.getenv(envVariable);
          uRet.append(varpath);
        }
        int pos;
        if( localdir.length() >0 && (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }

        uRet.append(localdir);
        return uRet;
      }
    }
    
    
    public CharSequence localFile(StringBuilder uRet, UserFilepath commonPath, UserFilepath accessPath){
      CharSequence dir = localDir(uRet, commonPath, accessPath);
      if(uRet ==null){
        uRet = dir instanceof StringBuilder ? (StringBuilder)dir: new StringBuilder(dir);
      }
      int pos;
      if( (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
      uRet.append(name).append(ext);
      return uRet;
    }
    
    
    

    
    
    /**Builds the absolute path with given basepath maybe absolute or not, maybe with drive letter or not. 
     * @return the whole path inclusive a given general path in a {@link UserFileSet} as absolute path.
     */
    private CharSequence absbasepath(CharSequence basepath){ 
      CharSequence ret = basepath;
      if(isRootpath(ret) ==0){       //insert the currdir of the script only if it is not a root directory already:
        if(ret.length() >0){      //only if ret is not ""
          StringBuilder uRet;
          if(ret instanceof StringBuilder){
            uRet = (StringBuilder)ret;
          } else {
            ret = uRet = new StringBuilder(ret);
          }
          String sCurrDir = script.sCurrDir;
          if(uRet.length() >=2 && uRet.charAt(1) == ':'){
            //a drive is present but it is not a root path
            if(sCurrDir.length()>=2 && sCurrDir.charAt(1)==':' && uRet.charAt(0) == sCurrDir.charAt(0)){
              //Same drive letter like sCurrDir: replace the absolute path.
              uRet.replace(0, 2, sCurrDir);
            }
            else {
              //a drive is present, but it is another drive else in sCurrDir But the path is not absolute:
              //TODO nothing yet, 
            }
          }
          else {  //a drive is not present.
            uRet.insert(0, script.sCurrDir);
          }
        }
        else {
          //ret is "", then return the current dir only.
          ret = script.sCurrDir;
        }
      }
      return ret;
    }
    
    /**Method can be called in the generation script: <*absbasepath()>. 
     * @return the whole path inclusive a given general path in a {@link UserFileSet} as absolute path.
     */
    public CharSequence absbasepath(){ 
      CharSequence basepath = basepath(null, emptyParent, null, null);
      //basepath = driveRoot(basepath, emptyParent,null);
      return absbasepath(basepath);
    }
    
    public CharSequence absbasepathW(){ return toWindows(absbasepath()); }
    

    
    /**Method can be called in the generation script: <*path.absdir()>. 
     * @return the whole path to the parent of this file inclusive a given general path in a {@link UserFileSet}.
     *   The path is absolute. If it is given as relative path, the general current directory of the script is used.
     */
    public CharSequence absdir(){ 
      CharSequence basePath = absbasepath();
      StringBuilder uRet = basePath instanceof StringBuilder ? (StringBuilder)basePath : new StringBuilder(basePath);
      int zpath = (localdir == null) ? 0 : localdir.length();
      if(zpath > 0){
        int pos;
        if( (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
        uRet.append(localdir.substring(0,zpath-1));
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
      uRet.append(this.localdir);
      if( (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
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
    public CharSequence basepath(){ return basepath(null, emptyParent, null, null); }
     
    
  
    
    public CharSequence basepathW(){ return toWindows(basepath()); }
    
    
    
    /**Method can be called in the generation script: <*path.dir()>. 
     * @return the whole path to the parent of this file inclusive a given general path in a {@link UserFileSet}.
     *   The path is absolute or relative like it is given.
     */
    public CharSequence dir(){ 
      CharSequence basePath = basepath();
      StringBuilder uRet = basePath instanceof StringBuilder ? (StringBuilder)basePath : new StringBuilder(basePath);
      int zpath = (localdir == null) ? 0 : localdir.length();
      if(zpath > 0){
        int pos;
        if( (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
        uRet.append(localdir);
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
      uRet.append(this.localdir);
      if( (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
      uRet.append(this.name);
      return uRet;
    }
    
    public CharSequence pathnameW(){ return toWindows(pathname()); }
    


    /**Returns the file path maybe with given commonBasepath and a access path. 
     * @param accesspath Access path may be given by usage.
     * @return the whole path with file name and extension.
     *   The path is absolute or relative like it is given.
     */
    public CharSequence file(StringBuilder uRet, UserFilepath accesspath){
      return file(uRet,null,accesspath);
    }
    
    /**Returns the file path maybe with given commonBasepath and a access path. 
     * @param accesspath Access path may be given by usage.
     * @return the whole path with file name and extension.
     *   The path is absolute or relative like it is given.
     */
    public CharSequence file(StringBuilder uRet, UserFilepath commonPath, UserFilepath accesspath){ 
      CharSequence basePath = basepath(null, commonPath, accesspath, null);
      uRet = basePath instanceof StringBuilder ? (StringBuilder)basePath : new StringBuilder(basePath);
      localDir(uRet, emptyParent, accesspath);
      int pos;
      if( (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
      uRet.append(name).append(ext);
      return uRet.append(ext);
    }
    
    
    
    /**Method can be called in the generation script: <*data.file()>. 
     * @return the whole path with file name and extension.
     *   The path is absolute or relative like it is given.
     */
    public CharSequence file(){ 
      CharSequence basePath = basepath();
      StringBuilder uRet = basePath instanceof StringBuilder ? (StringBuilder)basePath : new StringBuilder(basePath);
      addLocalName(uRet);
      return uRet.append(ext);
    }
    
    public CharSequence fileW(){ return toWindows(file()); }
    
    
    
    /**Method can be called in the generation script: <*data.base_localdir()>. 
     * @return the basepath:localpath in a {@link UserFileSet} with given wildcards 
     *   inclusive a given general path. The path is absolute or relative like it is given.
     */
    public CharSequence base_localdir(){ 
      CharSequence basePath = basepath();
      StringBuilder uRet = basePath instanceof StringBuilder ? (StringBuilder)basePath : new StringBuilder(basePath);
      if( uRet.length() >0){ uRet.append(":"); }
      uRet.append(this.localdir);
      return uRet;
    }
    
    public CharSequence base_localdirW(){ return toWindows(base_localdir()); }
    
    
    /**Method can be called in the generation script: <*data.base_localfile()>. 
     * @return the basepath:localpath/name.ext in a {@link UserFileSet} with given wildcards 
     *   inclusive a given general path. The path is absolute or relative like it is given.
     */
    public CharSequence base_localfile(){ 
      CharSequence basePath = basepath();
      StringBuilder uRet = basePath instanceof StringBuilder ? (StringBuilder)basePath : new StringBuilder(basePath);
      if( uRet.length() >0){ uRet.append(":"); }
      uRet.append(this.localdir);
      int pos;
      if( (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
      uRet.append(this.name);
      uRet.append(this.ext);
      return uRet;
    }
    
    public CharSequence base_localfileW(){ return toWindows(base_localfile()); }
    
    
    
  
    /**Method can be called in the generation script: <*path.localdir()>. 
     * @return the local path part of the directory of the file without ending slash. 
     *   If no directory is given in the local part, it returns ".". 
     */
    public String localdir(){
      int length = localdir == null ? 0 : localdir.length();
      return length == 0 ? "." : localdir; 
    }
    
    /**Method can be called in the generation script: <*path.localDir()>. 
     * @return the local path part with file without extension.
     */
    public String localdirW(){ return localdir.replace('/', '\\'); }
    

    
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

    
    
    
    /**Adds the local dir and the name, not the extension
     * @param uRet
     * @return uRet itself to concatenate.
     */
    private CharSequence addLocalName(StringBuilder uRet){ 
      int pos;
      if( this.localdir.length() > 0 && (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
      uRet.append(this.localdir);
      if( (pos = uRet.length()) >0 && uRet.charAt(pos-1) != '/'){ uRet.append("/"); }
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
    void expandFiles(List<UserFilepath> listToadd, UserFilepath commonPath, UserFilepath accessPath, File currdir){
      List<FileSystem.FileAndBasePath> listFiles = new LinkedList<FileSystem.FileAndBasePath>();
      final CharSequence basepath1 = this.basepath(null, commonPath, accessPath, null); //getPartsFromFilepath(file, null, "absBasePath").toString();
      int posRoot = isRootpath(basepath1);
      final CharSequence basePathNonRoot = posRoot == 0 ? basepath1: basepath1.subSequence(posRoot, basepath1.length());
      final String basepath = basePathNonRoot.toString();  //persistent content.
      //CharSequence basepathroot = driveRoot(basepath, commonPath, accessPath);
      //boolean absPath = isRootpath(basepathroot) >0;
      //String drive = basepathroot.length() >=2 && basepathroot.charAt(1) == ':' ? String.valueOf(basepathroot.charAt(0)): null;
      String drive = posRoot >=2 ? Character.toString(basepath1.charAt(0)) : null;
      boolean absPath = posRoot == 1 || posRoot == 3;
      
      final CharSequence absBasepath = absbasepath(basepath1);
      
      final CharSequence localfilePath = this.localFile(null, commonPath, accessPath); //getPartsFromFilepath(file, null, "file").toString();
      final String sPathSearch = absBasepath + ":" + localfilePath;
      try{ FileSystem.addFilesWithBasePath(null, sPathSearch, listFiles);
      } catch(Exception exc){
        //let it empty.
      }
      for(FileSystem.FileAndBasePath file1: listFiles){
        ZmakeUserScript.UserFilepath filepath2 = new ZmakeUserScript.UserFilepath(script);
        filepath2.absPath = absPath;
        filepath2.drive = drive;
        filepath2.basepath = basepath;  //it is the same. Maybe null
        int posName = file1.localPath.lastIndexOf('/') +1;  //if not found, set to 0
        int posExt = file1.localPath.lastIndexOf('.');
        final String sPath = file1.localPath.substring(0, posName);  //"" if only name
        final String sName;
        final String sExt;
        if(posExt < 0){ sExt = ""; sName = file1.localPath.substring(posName); }
        else { sExt = file1.localPath.substring(posExt); sName = file1.localPath.substring(posName, posExt); }
        filepath2.localdir = sPath;
        filepath2.name = sName;
        filepath2.ext = sExt;
        listToadd.add(filepath2);
      }

    }
    

    
    @Override
    public String toString()
    { return base_localfile().toString();
    }
  }
  
  
  
  
  /**This class is used only temporary while processing the parse result into a instance of {@link UserFilepath}
   * while running {@link ZbnfJavaOutput}. 
   */
  public static class ZbnfUserFilepath{
    
    /**The instance which are filled with the components content. It is used for the user's data tree. */
    final UserFilepath filepath;
    
    
    ZbnfUserFilepath(UserScript script){
      filepath = new UserFilepath(script);
    }
    
    /**FromZbnf. */
    public void set_drive(String val){ filepath.drive = val; }
    
    
    /**FromZbnf. */
    public void set_absPath(){ filepath.absPath = true; }
    
    /**FromZbnf. */
    public void set_scriptVariable(String val){ filepath.scriptVariable = val; }
    
    
    /**FromZbnf. */
    public void set_envVariable(String val){ filepath.envVariable = val; }
    
    

    
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
  
  
  
  /**A < fileset> in the ZmakeStd.zbnf. It is assigned to a script variable if it was created by parsing the ZmakeUserScript.
   * If the fileset is used in a target, it is associated to the target to get the absolute paths of the files
   * temporary while processing that target.
   * <br><br>
   * The Zbnf syntax for parsing is defined as
   * <pre>
   * fileset::= { basepath = <file?basepath> | <file> ? , }.
   * </pre>
   * The <code>basepath</code> is a general path for all files which is the basepath (in opposite to localpath of each file)
   * or which is a pre-basepath if any file is given with basepath.
   * <br><br>
   * Uml-Notation see {@link org.vishia.util.Docu_UML_simpleNotation}:
   * <pre>
   *               UserFileset
   *                    |------------commonBasepath-------->{@link UserFilepath}
   *                    |
   *                    |------------filesOfFileset-------*>{@link UserFilepath}
   *                                                        -drive:
   *                                                        -absPath: boolean
   *                                                        -basepath
   *                                                        -localdir
   *                                                        -name
   *                                                        -someFiles: boolean
   *                                                        -ext
   * </pre>
   * 
   */
  public static class UserFileset
  {
    
    final UserScript script;
    
    /**From ZBNF basepath = <""?!prepSrcpath>. It is a part of the base path anyway. It may be absolute, but usually relative. 
     * If null then unused. */
    UserFilepath commonBasepath;
    
    /**From ZBNF srcext = <""?srcext>. If null then unused. */
    //public String srcext;
    
    
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
    public ZbnfUserFilepath new_commonpath(){ return new ZbnfUserFilepath(script); }  //NOTE: it has not a parent. this is not its parent!
    public void set_commonpath(ZbnfUserFilepath val){ commonBasepath = val.filepath; }
    
    /**From ZBNF: < file>. */
    public ZbnfUserFilepath new_file(){ return new ZbnfUserFilepath(script); }
    
    /**From ZBNF: < file>. */
    public void add_file(ZbnfUserFilepath valz){ 
      UserFilepath val =valz.filepath;
      if(val.basepath !=null || val.localdir.length() >0 || val.name.length() >0 || val.drive !=null){
        //only if any field is set. not on empty val
        filesOfFileset.add(val); 
      }
    }
    
    
    void listFilesExpanded(List<UserFilepath> files, UserFilepath accesspath, boolean expandFiles) {  ////
      for(UserFilepath filepath: filesOfFileset){
        if(expandFiles && (filepath.someFiles || filepath.allTree)){
          filepath.expandFiles(files, commonBasepath, accesspath, script.currDir);
        } else {
          //clone filepath! add srcpath
          UserFilepath targetsrc = new UserFilepath(script, filepath, commonBasepath, accesspath);
          files.add(targetsrc);
        }
      }
    }

    public List<UserFilepath> listFilesExpanded(UserFilepath accesspath, boolean expandFiles) { 
      List<UserFilepath> files = new LinkedList<UserFilepath>();
      listFilesExpanded(files, accesspath, expandFiles);
      return files;
    }
    
    
    public List<UserFilepath> listFilesExpanded() { return listFilesExpanded(null, true); }

      
      
    @Override
    public String toString(){ 
      StringBuilder u = new StringBuilder();
      if(commonBasepath !=null) u.append("basepath="+commonBasepath+", ");
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
    
    UserFilepath filepath;
    
    TextGenScript.Expression expression;
    
    //public UserString string;
    
    ScriptVariable(UserScript script){
      this.script = script;
    }
    
    public UserFileset new_fileset(){ return new UserFileset(script); }
    public void add_fileset(UserFileset val){ fileset = val; }
    
    public ZbnfUserFilepath new_file(){ return new ZbnfUserFilepath(script); }
    public void add_file(ZbnfUserFilepath val){ filepath = val.filepath; }
    
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
    
    public TargetInput new_inputvalue(){ return new TargetInput(parentTarget); }

    public void add_inputvalue(TargetInput val){ 
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
            assert(element.fileset !=null);
            //search the input Set in the script variables:
            ScriptVariable variable = parentTarget.script.var.get(element.fileset);
            if(variable == null){
              int pos = u.length();
              u.append("<??error ZmakeScriptvariable not found: ").append(element.fileset).append(" ??>");
              parentTarget.script.abortOnError(u,pos);
            } else if(variable.expression !=null){
              u.append(variable.expression.text());
              //variable.expression.addtext(u);
            } else {
              int pos = u.length();
              u.append("<??error ZmakeScriptvariable as string expected: ").append(element.fileset).append(" ??>");
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
    public String fileset;  //referVariable;
    
    public String XXXsrcpathEnvVariable;
    
    public String XXXsrcpathVariable;
    
    public String XXXsrcpathTargetInput;
    
    //UserFilepath srcpathInput;
    UserFilepath inputFile;
    
    
    
    TargetInput(UserTarget parentTarget){
      //super(parentTarget.script);
      this.parentTarget = parentTarget;
      //this.script = parentTarget.script;
    }
    
    
    public ZbnfUserFilepath new_input(){ return  new ZbnfUserFilepath(parentTarget.script); }

    public void add_input(ZbnfUserFilepath val){ inputFile = val.filepath; }


    
    
    //public UserFilepath new_srcpath(){ return srcpathInput = new UserFilepath(null);}
    //public void add_srcpath(UserFilepath val){}
    
    
    //UserInput(String inputSet){ this.inputSet = inputSet; this.inputFile = null; }
    //UserInput(UserInputSet inputSet){ this.inputSet = inputSet; this.inputFile = null; }
    //UserInput(UserFilepath inputFile){ this.inputSet = null; this.inputFile = inputFile; }
    
    @Override public String toString(){ return inputFile !=null ? inputFile.toString() : fileset; }
    
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
    
    public String targetName;
    
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
    public ZbnfUserFilepath new_output(){ return new ZbnfUserFilepath(script); } //this); }
    
    /**From Zbnf: < output>.
     * @param instance contains the parse result of the component output::=
     */
    public void set_output(ZbnfUserFilepath value){ output = value.filepath; }
    
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
    public TargetInput new_inputvalue()
    { TargetInput userInput = new TargetInput(this);
      return userInput; 
    }

    /**From Zbnf: < inputarg>.
     * @param instance contains the parse result of the component inputarg::=
     */
    public void add_inputvalue(TargetInput val){ inputs.add(val);}

    
    /**FromZbnf: <$?inputSet>
     * An inputSet is a simple string which is the name of a script variable which should refer to a file {@link UserFileset}.
     * @param val
     */
    public void XXXset_inputSet(String val){
      //inputs.add(new UserInput(val));
    }
    
    public List<UserFilepath> allInputFiles(){
      return prepareFiles(inputs, false);
    }
    
    /**Prepares all files which are given with a parameter.
     * In the ZmakeUserScript it can be given in form (examples)
     * <pre>
     * ...target(..., param=fileset1:accesspath + fileset2:accesspath, ...);
     * ...target(..., param=file1+file2,...)
     * ...target(..., param=file,...)
     * ...target(..., param=fileset,...)
     * </pre>
     * All files and members of a fileset of this parameter are combined in one List<{@link UserFilepath}> 
     * which can be used as container for ZmakeGenerationScript.
     * @return A list of {@link UserFilepath} independent of a {@link UserFileset}.
     */
    public List<UserFilepath> allInputFilesExpanded(){
      return prepareFiles(inputs, true);
    }

    public List<UserFilepath> allParamFiles(String paramName){ return allParamFiles(paramName, false); }

    
    /**Prepares the input of the target.
     * All input files and fileset which are not parameter are combined in one {@link TargetInput} which is used as the fileset
     * @param expandFiles
     * @return
     */
    public List<UserFilepath> allParamFilesExpanded(String paramName){ return allParamFiles(paramName, false); }
    
    
    private List<UserFilepath> allParamFiles(String name, boolean expand){
      TargetParam param = params.get(name);
      if(param==null){
        if(script.bWriteErrorInOutputScript){
          return null;                //no files
        } else {
          throw new IllegalArgumentException("Zmake - param not found; " + name);
        }
      } else {
        return prepareFiles(param.referVariables, false);
      }
    }
    
    
    /**Prepares the input of the target.
     * @param filesOrFilesets A TargetInput contains either some files or some filesets or both.
     * @param expandFiles true then resolve wildcards and return only existing files.
     * @return A list of files.
     */
    private List<UserFilepath> prepareFiles( List<TargetInput> filesOrFilesets, boolean expandFiles) {
      if(targetName !=null && targetName.equals("test2"))
        Assert.stop();
      List<UserFilepath> files = new LinkedList<UserFilepath>();
      //UserFileset inputfileset = null; 
      for(TargetInput targetInputParam: filesOrFilesets){
        //UserFileset targetparaminputfileset; 
        { //if(name == null && targetInputParam.name == null || name !=null && name.equals(targetInputParam.name)){ //input or matching parameter?
          /*
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
          */
          //
          //
          //expand file or fileset:
          //
          if(targetInputParam.fileset !=null){
            ScriptVariable variable = script.var.get(targetInputParam.fileset);
            if(variable == null || variable.fileset == null){
              if(script.bWriteErrorInOutputScript){
                UserFilepath errorhint = new UserFilepath(script);
                errorhint.name = "<?error not found; " + targetInputParam.fileset + ">";
                files.add(errorhint);
              }
            } else {
              variable.fileset.listFilesExpanded(files, targetInputParam.inputFile, expandFiles);
            }
            
          }
          else if(targetInputParam.inputFile !=null){
            if(expandFiles){
              targetInputParam.inputFile.expandFiles(files, null, null, script.currDir);
            } else {
              UserFilepath targetsrc = new UserFilepath(script, targetInputParam.inputFile, null, null);
              files.add(targetsrc);  
            }
          } else { 
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
