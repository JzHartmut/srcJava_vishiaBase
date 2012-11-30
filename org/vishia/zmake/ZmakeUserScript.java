package org.vishia.zmake;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


public class ZmakeUserScript
{
  
  /**Version, history and license.
   * <ul>
   * <li>2012-11-29 Hartmut new: The {@link UserFilePath} contains now all access routines to parts of the file path
   *   which were contained in the ZmakeGenerator class before. This allows to use this with the common concept
   *   of a text generator using {@link org.vishia.textGenerator.TextGenerator}. That Textgenerator does not know such
   *   file parts and this {@link UserFilepath} class because it is more universal. Access is possible 
   *   with that access methods {@link UserFilepath#localPath()} etc. To implement that a {@link UserFilepath}
   *   should know its set where it is contained in, that is {@link UserFilepath#parent}. 
   *   And the {@link UserFileset#script} knows the Zmake generation script. In this kind it is possible to use the
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
    public String localPathName(){ return path + file; }
    
    
    
    /**Method can be called in the generation script: <*path.localFile()>. 
     * @return the local path to this file.
     */
    public CharSequence localFile(){ 
      StringBuilder uRet = new StringBuilder();
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
    public CharSequence basePath(){ 
      UserFilepath generalPath = parent !=null && parent.srcpath !=null ? parent.srcpath : emptyParent;
      if(pathbase !=null || (generalPath.pathbase !=null)){
        StringBuilder uRet = new StringBuilder();
        if(this.drive !=null){ uRet.append(this.drive).append(':'); }
        else if(generalPath.drive !=null){
          uRet.append(parent.srcpath.drive).append(':'); 
        }
        if(absPath){ uRet.append('/'); }
        else if(generalPath.absPath){ uRet.append('/'); }
        //
        if(this.pathbase !=null){
          //append a general path completely firstly.
          if(generalPath.pathbase !=null){ uRet.append(this.pathbase).append('/'); }
          int pos = uRet.length();
          uRet.append(generalPath.path).append(generalPath.file).append(generalPath.ext);
          if(uRet.length() > pos){ uRet.append('/');  } //after path.file.ext
        }
        else if(generalPath.pathbase !=null){
          //only pathbase of the generalPath because a local pathBase is not given.
          uRet.append(parent.srcpath.pathbase); 
        }
        return uRet;
      } else {
        return "";
      }
    }
    
    
    
    public CharSequence basePathW(){ return toWindows(basePath()); }
    
    
    /**Method can be called in the generation script: <*path.file()>. 
     * @return the whole path inclusive a given general path in a {@link UserFileSet}.
     *   Either as absolute or as relative path.
     */
    public CharSequence absBasePath(){ 
      StringBuilder uRet = new StringBuilder();
      if(this.drive !=null){ uRet.append(this.drive); }
      if(!this.absPath){ uRet.append(parent.script.sCurrDir); }
      if(this.pathbase !=null){ uRet.append(this.pathbase); }
      return uRet;
    }
    
    
    /**Method can be called in the generation script: <*path.file()>. 
     * @return the whole path inclusive a given general path in a {@link UserFileSet}.
     *   Either as absolute or as relative path.
     */
    public CharSequence file(){ 
      StringBuilder uRet = new StringBuilder();
      if(parent.srcpath !=null){
        if(parent.srcpath.drive !=null){ uRet.append(parent.srcpath.drive); }
        if(parent.srcpath.pathbase !=null && this.pathbase.length() >0){ uRet.append(parent.srcpath.pathbase).append('/'); }
        uRet.append(parent.srcpath.path);  //ends with /
      } else {
        if(this.drive !=null){ uRet.append(this.drive); }
        if(this.pathbase !=null && this.pathbase.length() >0){ uRet.append(this.pathbase).append('/'); }
      }
      uRet.append(this.path);
      uRet.append(this.file);
      if(this.someFiles){ uRet.append('*'); }
      if(this.wildcardExt){ uRet.append(".*"); }
      uRet.append(this.ext);
      return uRet;
    }
    
    
    
    /**Method can be called in the generation script: <*path.file()>. 
     * @return the whole path inclusive a given general path in a {@link UserFileSet}.
     *   The path is absolute. If it is given as relative path, the general current directory of the script is used.
     */
    public CharSequence absFile(){ 
      StringBuilder uRet = new StringBuilder();
      if(parent.srcpath !=null){
        if(parent.srcpath.drive !=null){ uRet.append(parent.srcpath.drive); }
        if(!parent.srcpath.absPath){ 
          uRet.append(parent.script.sCurrDir); 
        }
        if(parent.srcpath.pathbase !=null && this.pathbase.length() >0){ uRet.append(parent.srcpath.pathbase).append('/'); }
        uRet.append(parent.srcpath.path);  //ends with /
      } else {
        if(this.drive !=null){ uRet.append(this.drive); }
        if(!this.absPath){ uRet.append(parent.script.sCurrDir); }
        if(this.pathbase !=null && this.pathbase.length() >0){ uRet.append(this.pathbase).append('/'); }
      }
      uRet.append(this.path);
      uRet.append(this.file);
      if(this.someFiles){ uRet.append('*'); }
      if(this.wildcardExt){ uRet.append(".*"); }
      uRet.append(this.ext);
      return uRet;
    }
    
    
    /**Method can be called in the generation script: <*path.absDir()>. 
     * @return the whole path to the parent of this file inclusive a given general path in a {@link UserFileSet}.
     *   The path is absolute. If it is given as relative path, the general current directory of the script is used.
     */
    public CharSequence absDir(){ 
      StringBuilder uRet = new StringBuilder();
      if(this.drive !=null){ uRet.append(this.drive); }
      if(!this.absPath){ uRet.append(parent.script.sCurrDir); }
      if(this.pathbase !=null && this.pathbase.length() >0){ uRet.append(this.pathbase).append('/'); }
      uRet.append(this.path);
      return uRet;
    }
    
    
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
    
    
    
    @Override
    public String toString()
    { return path + file + ext;
    }
  }
  
  
  
  
  /**A < fileset> in the ZmakeStd.zbnf:
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
    
    /**From ZBNF: */
    UserFilepath srcpath;
    public String srcext;
    List<UserFilepath> filesOfFileset = new LinkedList<UserFilepath>();
    
    UserFileset(UserScript script){
      this.script = script;
    }
    
    
    public UserFileset(){
      this.script = null;
    }
    
    
    public UserFilepath new_srcpath(){ return srcpath = new UserFilepath(this); }
    public void set_srcpath(UserFilepath val){  }
    
    /**From ZBNF: < file>. */
    public UserFilepath new_file(){ return new UserFilepath(this); }
    /**From ZBNF: < file>. */
    public void add_file(UserFilepath val){ filesOfFileset.add(val); }
    
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
  }
  
  public static class UserParam
  { public String name;
    public String value;
    public String variable;
  }
  
  public static class UserInputSet
  { final UserScript script;
  
    public String name;
    UserFilepath srcpath;
    UserInputSet(UserScript script){
      this.script = script;
    }
    public UserFilepath new_srcpath(){ return srcpath = new UserFilepath(null); }
    public void set_srcpath(UserFilepath val){  }
  }
  
  
  /**Describes 1 input item for a target, maybe a file, maybe a inputset. */
  static class UserInput
  { final UserInputSet inputSet;
    final UserFilepath inputFile;
    UserInput(UserInputSet inputSet){ this.inputSet = inputSet; this.inputFile = null; }
    UserInput(UserFilepath inputFile){ this.inputSet = null; this.inputFile = inputFile; }
  }
  
  
  /**
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
  public final static class UserTarget extends UserFileset
  {
    UserFilepath output;
    
    public String name;
    
    public String translator;
    
    //UserFilepath srcpath;
    
    //public String srcext;
    
    UserTarget(UserScript script){
      super(script);
    }
    
    @Override
    public UserFilepath new_srcpath(){ return srcpath = new UserFilepath(this); }
    
    @Override
    public void set_srcpath(UserFilepath value){  }
    
    public UserFilepath new_output(){ return output = new UserFilepath(this); }
    
    public void set_output(UserFilepath value){  }
    
    List<UserInput> inputs = new LinkedList<UserInput>();
    
    Map<String,UserParam> params;
    
    /**From ZBNF < inputSet>*/
    public UserInputSet new_inputSet()
    { UserInput userInput = new UserInput(new UserInputSet(script));
      inputs.add(userInput);
      return userInput.inputSet; 
    }
    
    public void add_inputSet(UserInputSet value){  }
    
    
    /**From ZBNF < param>. */
    public UserParam new_param(){ return new UserParam();}
    
    /**From ZBNF < param>. */
    public void add_param(UserParam val){ 
      if(params ==null){ params = new TreeMap<String,UserParam>(); }
      params.put(val.name, val); 
    }
    
    /**From ZBNF < ?!prepInputfile> < ?input>*/
    public UserFilepath new_input()
    { UserInput userInput = new UserInput(new UserFilepath(this));
      inputs.add(userInput);
      return userInput.inputFile; 
    }

    public void add_input(UserFilepath val){}

  
  }
  
  
  
  /**The main class of the data of the users script for zmake.
   * Filled from {@link org.vishia.zbnf.ZbnfJavaOutput}.
   *
   */
  public static class UserScript
  {
    int nextNr = 0;
    
    /**The current directory for access to all files which are necessary as absolute file.
     * 
     */
    String sCurrDir = "";
    
    Map<String, ScriptVariable> allVariables = new TreeMap<String, ScriptVariable>();
    
    List<UserTarget> targets = new LinkedList<UserTarget>();
    
    /**From ZBNF: < variable> */
    public ScriptVariable new_variable(){ return new ScriptVariable(this); }
    
    public void add_variable(ScriptVariable  value){ allVariables.put(value.name, value); }
    
    public UserTarget new_target(){ return new UserTarget(this); }
    
    public void add_target(UserTarget value){ targets.add(value); }
    
    public String nextNr(){
      nextNr +=1;
      return Integer.toString(nextNr);
    }
    
  }

}
