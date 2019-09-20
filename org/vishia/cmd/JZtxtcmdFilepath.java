package org.vishia.cmd;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.vishia.util.Assert;
import org.vishia.util.DataAccess;
import org.vishia.util.FilePath;
import org.vishia.util.FileSystem;

/**This class describes a Filepath instance in an executer level of JZcmd. The file entity can contain wild cards.
 * It can refer to a variable which contains the base path.
 * It may be a absolute or a relative path. It can have a base path and a local path part.
 * This class contains the reference to the {@link JZtxtcmdExecuter.ExecuteLevel} where this variable is located
 * and a reference to the {@link JZtxtcmdScript.Filepath}. The last one contains all information about the
 * file entity. This class is used to get all presentation possibilities of the file. Therefore the current directory
 * should be known which is given in the JZcmd executer level. 
 * <br><br>
 * <br><br>
 */
public final class JZtxtcmdFilepath {

  
  
  /**Version, history and license.
   * <ul>   
   * <li>2014-06-10 Hartmut chg: {@link ExecuteLevel} implements {@link FilePath.FilePathEnvAccess} now
   *   instead this, therewith a {@link JZtxtcmdFileset#listFiles(List, JZtxtcmdFilepath, boolean, org.vishia.util.FilePath.FilePathEnvAccess)}
   *   does not need an accessPath, it may be empty respectively null.
   * <li>2014-03-07 Hartmut new: All capabilities from Zmake are joined here. Only one concept!
   *   This file was copied from srcJava_Zbnf/org/vishia/zmake/Userfilepath.
   *   The data of a file are referenced with {@link #data}. The original fields are contained in
   *   {@link JZtxtcmdScript.Filepath}. Both are separated because the parts in JZcmdScript are set completely
   *   by parsing the script. This class contains the access methods which uses the reference {@link #zgenlevel}.
   * <li>2013-03-10 Hartmut new: {@link FileSystem#normalizePath(CharSequence)} called in {@link #absbasepath(CharSequence)}
   *   offers the normalize path for all absolute file paths. 
   * <li>2013-03-10 Hartmut new: Replace wildcards: {@link #absfile(JZtxtcmdFilepath)} (TODO for some more access methods)
   * <li>2013-02-12 Hartmut chg: dissolved from inner class in {@link ZmakeUserScript}
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
   static final public int version = 20130310;


  
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
  
  private final JZtxtcmdExecuter.ExecuteLevel zgenlevel;
  

  final FilePath data;
  
  
  
  
  /**Creates an instance with given data.
   * @param zgenlevel
   * @param filepath given data
   */
  JZtxtcmdFilepath(JZtxtcmdExecuter.ExecuteLevel zgenlevel, FilePath filepath){
    this.zgenlevel = zgenlevel;
    this.data = filepath;
  }
  
  /**Creates an instance with given data.
   * Note: it is package private because only used from {@link JZtxtcmdExecuter}. 
   * Use there the construct <code>Filepath: < stringexpression></code>
   * @param zgenlevel
   * @param filepath given data
   */
  JZtxtcmdFilepath(JZtxtcmdExecuter.ExecuteLevel zgenlevel, String filepath){
    this.zgenlevel = zgenlevel;
    this.data = new FilePath(filepath);
  }
  

  
  /**Creates a JZcmdFilepath entry with an additonal pathbase.
   * if the basepath of src is given and the pathbase0 is given, both are joined: pathbase0/src.pathbase.
   * @param zgenlevel  Reference to the zgenlevel, necessary for the current directory
   * @param src The source (clone source)
   * @param basepath An additional basepath usual stored as <code>basepath=path, ...</code> in a fileset, maybe null
   * @param pathbase0 additional pre-pathbase before base, maybe null
   * @throws NoSuchFieldException if the basepath contains a scriptvariable and this scriptvariable was not found. 
   *  
   */
  JZtxtcmdFilepath(JZtxtcmdExecuter.ExecuteLevel zgenlevel, JZtxtcmdFilepath src, JZtxtcmdFilepath commonPath, JZtxtcmdFilepath accessPath) throws NoSuchFieldException {
    this.zgenlevel = zgenlevel;
    FilePath fcommonPath = commonPath == null ? null : commonPath.data;
    FilePath faccessPath = accessPath == null ? null : accessPath.data;
    data = new FilePath(src.data, fcommonPath, faccessPath, zgenlevel);
  }
  

  
  
  
  
  /**Assembles and returns the designated part from this Filepath as String or StringBuilder:: <&absbasepath()>. 
   * @return the whole path inclusive a given general path in a {@link UserFileSet} as absolute path.
   * @throws NoSuchFieldException if the basepath contains a scriptvariable and this scriptvariable was not found. 
   *  
   */
  public CharSequence absbasepath() throws NoSuchFieldException { return data.absbasepath(zgenlevel); }
  
  public CharSequence absbasepathW() throws NoSuchFieldException { return data.absbasepathW(zgenlevel); }
  

  
  /**Assembles and returns the designated part from this Filepath as String or StringBuilder:: <&path.absdir()>. 
   * @return the whole path to the parent of this file inclusive a given general path in a {@link UserFileSet}.
   *   The path is absolute. If it is given as relative path, the general current directory of the script is used.
   * @throws NoSuchFieldException if the basepath contains a scriptvariable and this scriptvariable was not found. 
   *   
   */
  public CharSequence absdir() throws NoSuchFieldException  { return data.absdir(zgenlevel); } 
  
  public CharSequence absdirW() throws NoSuchFieldException{ return data.absdirW(zgenlevel); }
  
  
  /**Assembles and returns the designated part from this Filepath as String or StringBuilder: <&data.absname()>. 
   * @return the whole path with file name but without extension inclusive a given general path in a {@link UserFileSet}.
   *   Either as absolute or as relative path.
   * @throws NoSuchFieldException if the basepath contains a scriptvariable and this scriptvariable was not found. 
   */
  public CharSequence absname() throws NoSuchFieldException{ return data.absname(zgenlevel); }
  
  public CharSequence absnameW() throws NoSuchFieldException{ return data.absnameW(zgenlevel); }
  


  
  /**Assembles and returns the designated part from this Filepath as String or StringBuilder: <&path.absfile()>. 
   * @return the whole path inclusive a given general path .
   *   The path is absolute. If it is given as relative path, the general current directory of the script is used.
   * @throws NoSuchFieldException if the basepath contains a scriptvariable and this scriptvariable was not found. 
   */
  public CharSequence absfile() throws NoSuchFieldException{ return data.absfile(zgenlevel); }
  
  public CharSequence absfileW() throws NoSuchFieldException{ return data.absfileW(zgenlevel); }
  
  
  
  /**Assembles and returns the designated part from this Filepath as String or StringBuilder: <&basepath()>. 
   * @return the whole base path inclusive a given general path in a {@link UserFileSet}.
   *   till a ':' in the input path or an empty string.
   *   Either as absolute or as relative path how it is given.
   * @throws NoSuchFieldException if the basepath contains a scriptvariable and this scriptvariable was not found. 
   */
  public CharSequence basepath() throws NoSuchFieldException{ return data.basepath(zgenlevel); }
   
  

  
  public CharSequence basepathW() throws NoSuchFieldException{ return data.basepathW(zgenlevel); }
  
  
  
  /**Assembles and returns the designated part from this Filepath as String or StringBuilder: <&path.dir()>. 
   * @return the whole path to the parent of this file inclusive a given general path in a {@link UserFileSet}.
   *   The path is absolute or relative like it is given.
   * @throws NoSuchFieldException if the basepath contains a scriptvariable and this scriptvariable was not found. 
   */
  public CharSequence dir() throws NoSuchFieldException{ return data.dir(zgenlevel); } 
  
  
  public CharSequence dirW() throws NoSuchFieldException{ return data.dirW(zgenlevel); }
  
  /**Assembles and returns the designated part from this Filepath as String or StringBuilder: <&data.pathname()>. 
   * @return the whole path with file name but without extension inclusive a given general path in a {@link UserFileSet}.
   *   The path is absolute or relative like it is given.
   * @throws NoSuchFieldException if the basepath contains a scriptvariable and this scriptvariable was not found. 
   */
  public CharSequence pathname() throws NoSuchFieldException{ return data.pathname(zgenlevel); }
  
  public CharSequence pathnameW() throws NoSuchFieldException{ return data.pathnameW(zgenlevel); }
  

  /**Assembles and returns the designated part from this Filepath as String or StringBuilder: <&data.file()>. 
   * @return the whole path with file name and extension.
   *   The path is absolute or relative like it is given.
   * @throws NoSuchFieldException if the basepath contains a scriptvariable and this scriptvariable was not found. 
   */
  public CharSequence file() throws NoSuchFieldException{ return data.file(zgenlevel); } 
  
  public CharSequence fileW() throws NoSuchFieldException{ return data.fileW(zgenlevel); }
  
  public CharSequence file(StringBuilder uRet, FilePath commonPath, FilePath accesspath) 
  throws NoSuchFieldException {
    return data.file(uRet, commonPath, accesspath, zgenlevel);
  }

  public CharSequence fileW(StringBuilder uRet, FilePath commonPath, FilePath accesspath) 
  throws NoSuchFieldException {
    return FilePath.toWindows(data.file(uRet, commonPath, accesspath, zgenlevel));
  }

  
  /**Assembles and returns the designated part from this Filepath as String or StringBuilder: <&data.base_localdir()>. 
   * @return the basepath:localpath in a {@link UserFileSet} with given wildcards 
   *   inclusive a given general path. The path is absolute or relative like it is given.
   * @throws NoSuchFieldException if the basepath contains a scriptvariable and this scriptvariable was not found. 
   */
  public CharSequence base_localdir() throws NoSuchFieldException{ return data.base_localdir(zgenlevel); } 
  
  public CharSequence base_localdirW() throws NoSuchFieldException{ return data.base_localdirW(zgenlevel); }
  
  
  /**Assembles and returns the designated part from this Filepath as String or StringBuilder: <&data.base_localfile()>. 
   * @return the basepath:localpath/name.ext in a {@link UserFileSet} with given wildcards 
   *   inclusive a given general path. The path is absolute or relative like it is given.
   * @throws NoSuchFieldException if the basepath contains a scriptvariable and this scriptvariable was not found. 
   */
  public CharSequence base_localfile() throws NoSuchFieldException{ return data.base_localfile(zgenlevel); }
  
  public CharSequence base_localfileW() throws NoSuchFieldException{ return data.base_localfileW(zgenlevel); }
  
  
  

  /**Assembles and returns the designated part from this Filepath as String or StringBuilder: <&path.localdir()>. 
   * @return the local path part of the directory of the file without ending slash. 
   *   If no directory is given in the local part, it returns ".". 
   * @throws NoSuchFieldException if the basepath contains a scriptvariable and this scriptvariable was not found. 
   */
  public CharSequence localdir() throws NoSuchFieldException{ return data.localdir(zgenlevel); }
  
  /**Assembles and returns the designated part from this Filepath as String or StringBuilder: <&path.localDir()>. 
   * @return the local path part with file without extension.
   * @throws NoSuchFieldException if the basepath contains a scriptvariable and this scriptvariable was not found. 
   */
  public CharSequence localdirW() throws NoSuchFieldException{ return data.localdirW(zgenlevel); }
  

  
  /**Assembles and returns the designated part from this Filepath as String or StringBuilder: <&path.localname()>. 
   * @return the local path part with file without extension.
   * @throws NoSuchFieldException if the basepath contains a scriptvariable and this scriptvariable was not found. 
   */
  public CharSequence localname() throws NoSuchFieldException{ return data.localname(zgenlevel); }
  
  public CharSequence localnameW() throws NoSuchFieldException{return data.localnameW(zgenlevel); }

  
  /**Assembles and returns the designated part from this Filepath as String or StringBuilder: <&path.localfile()>. 
   * @return the local path to this file inclusive name and extension of the file.
   * @throws NoSuchFieldException 
   */
  public CharSequence localfile() throws NoSuchFieldException{ return data.localfile(zgenlevel); }

  public CharSequence localfileW() throws NoSuchFieldException{ return data.localfileW(zgenlevel); }

  
  /**Returns true if the local path contains (ends with) "/**"
   * @return
   */
  public boolean allTree() { return this.data.allTree(); }
  
  /**Returns the local file with replaced wildcard in the local dir. See {@link #addLocalNameReplwildcard(StringBuilder, FilePath).
   * @param replWildc With them localdir and name a wildcard in this.localdir and this.name is replaced.
   * @return the whole path inclusive a given general path .
   *   The path is absolute. If it is given as relative path, the general current directory of the script is used.
   * @throws NoSuchFieldException 
   */
  CharSequence absfileReplwildcard(FilePath replWildc) throws NoSuchFieldException{ 
    return data.absfileReplwildcard(replWildc, zgenlevel);
  }
  
  public CharSequence localfileReplwildcard(StringBuilder uRet, JZtxtcmdFilepath replWildc){ 
    return data.localfileReplwildcard(uRet, replWildc.data);
  }

  /**Returns the name from this Filepath as String. 
   * @return the name of the file without extension.
   */
  public CharSequence name(){ return data.name(); }
  
  /**Assembles and returns the designated part from this Filepath as String or StringBuilder: <&path.namext()>. 
   * @return the file name with extension.
   */
  public CharSequence namext(){ return data.namext(); }
  
  /**Assembles and returns the extension from this Filepath as String.. 
   * @return the file extension.
   */
  public String ext(){ return data.ext(); }
  
  
  

  
  @Override
  public String toString()
  { //try{ 
    return data.toString(); //} //base_localfile().toString();}
    //catch(NoSuchFieldException exc){
    //  return "faulty variable";
    //}
  }




}
