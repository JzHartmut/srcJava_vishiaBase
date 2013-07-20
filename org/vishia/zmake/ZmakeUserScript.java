package org.vishia.zmake;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


import org.vishia.jbat.JbatExecuter;
import org.vishia.jbat.JbatGenScript;
import org.vishia.util.Assert;
import org.vishia.util.FileSystem;


public class ZmakeUserScript
{
  
  /**Version, history and license.
   * <ul>
   * <li>2013-07-11 Hartmut chg/new: {@link ScriptVariable} inherits from {@link JbatGenScript.Argument},
   *   this concept are merged into: The ZmakeUserScript has properties of the Jbat generation for the ScriptVariables.
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
   *   of a text generator using {@link org.vishia.jbat.JbatExecuter}. That Textgenerator does not know such
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
  static final public int version = 20130310;


  
  
  
  
  
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
    UserStringContent(char type){ this.type = type; }
    UserStringContent(char type, String text){ this.type = type; this.text = text; }
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
  public static class ScriptVariable extends JbatGenScript.Argument
  { final UserScript script;
    //public String name;
    UserFileset fileset;
    
    UserFilepath filepath;
    
    //JbatGenScript.Expression expression;
    
    //public UserString string;
    
    ScriptVariable(UserScript script){
      super(null);  //No parent statement list.
      this.script = script;
    }
    
    public UserFileset new_fileset(){ return new UserFileset(script); }
    public void add_fileset(UserFileset val){ fileset = val; }
    
    public ZbnfUserFilepath new_file(){ return new ZbnfUserFilepath(script); }
    public void add_file(ZbnfUserFilepath val){ filepath = val.filepath; }
    
    //@Override
    //public JbatGenScript.Expression new_expression(){ return new JbatGenScript.Expression(this); }
    
    //@Override
    //public void add_expression(JbatGenScript.Expression val){ expression = val; }
    
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
    public CharSequence text(){ return expression !=null ? script.jbatexecuter.ascertainText(expression) : ""; }
    
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
            ScriptVariable variable = parentTarget.script.scriptVarZmake.get(element.fileset);
            if(variable == null){
              int pos = u.length();
              u.append("??:error ZmakeScriptvariable not found: ").append(element.fileset).append(".??");
              parentTarget.script.abortOnError(u,pos);
            } else if(variable.expression !=null){
              JbatExecuter gen = parentTarget.script.jbatexecuter;
              u.append(gen.ascertainText(variable.expression));
              //variable.expression.addtext(u);
            } else {
              int pos = u.length();
              u.append("??:error ZmakeScriptvariable as string expected: ").append(element.fileset).append(".??");
              parentTarget.script.abortOnError(u,pos);
            } 
          }
        }
        return u;
      }
    }

    
    public CharSequence textW(){ return UserFilepath.toWindows(text()); }
  
    @Override public String toString(){
      return "TargetParam " + name + " = " + value;
    }
  
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
    
    @Override public String toString(){
      StringBuilder ret = new StringBuilder();
      if(inputFile !=null){ ret.append(inputFile.toString()); }
      if(fileset !=null){ ret.append('&').append(fileset); }
      return ret.toString();
    }
    
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
      //
      //check whether the target has a parameter srcpath=... or commonpath = ....
      UserFilepath commonPathTarget = null;
      if(params !=null){
        TargetParam pSrcpath = params.get("srcpath");
        if(pSrcpath == null){ pSrcpath = params.get("commonpath"); }
        if(pSrcpath !=null){
          TargetInput inpSrcpath = pSrcpath.referVariables.get(0);
          if(inpSrcpath ==null) throw new IllegalArgumentException("ZmakeUserScript - srcpath");
          commonPathTarget = inpSrcpath.inputFile;
        }
      }
      List<UserFilepath> files = new LinkedList<UserFilepath>();
      //UserFileset inputfileset = null; 
      for(TargetInput targetInputParam: filesOrFilesets){
        { //expand file or fileset:
          //
          if(targetInputParam.fileset !=null){
            ScriptVariable variable = script.scriptVarZmake.get(targetInputParam.fileset);
            if(variable == null || variable.fileset == null){
              if(script.bWriteErrorInOutputScript){
                UserFilepath errorhint = new UserFilepath(script);
                errorhint.name = "??:error not found; " + targetInputParam.fileset + ".??";
                files.add(errorhint);
              }
            } else {
              variable.fileset.listFilesExpanded(files, targetInputParam.inputFile, expandFiles);
            }
            
          }
          else if(targetInputParam.inputFile !=null){
            if(expandFiles){
              targetInputParam.inputFile.expandFiles(files, commonPathTarget, null, script.currDir);
            } else {
              UserFilepath targetsrc = new UserFilepath(script, targetInputParam.inputFile, commonPathTarget, null);
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
    /**It may be necessary to evaluate parts of zmake user script, especially script variables. 
     * 
     */
    final JbatExecuter jbatexecuter;
    
    int nextNr = 0;
    
    /**The current directory for access to all files which are necessary as absolute file.
     * It should be end with slash!
     */
    private String sCurrDir = "";
    
    private File currDir;
    
    boolean bWriteErrorInOutputScript = true;
    
    //Map<String, String> currDir = new TreeMap<String, String>();
    
    Map<String, ScriptVariable> scriptVarZmake = new TreeMap<String, ScriptVariable>();
    
    public List<UserTarget> targets = new LinkedList<UserTarget>();
    
    /**From ZBNF: < variable> */
    public ScriptVariable new_variable(){ return new ScriptVariable(this); }
    
    public void add_variable(ScriptVariable  value){ scriptVarZmake.put(value.name, value); }
    
    public UserTarget new_target(){ return new UserTarget(this); }
    
    public void add_target(UserTarget value){ targets.add(value); }
    
    
    public UserScript(JbatExecuter jbatexecuter){
      this.jbatexecuter = jbatexecuter;
    }
    
    /**This method can be called from a user script.
     * @return an incremented number started from 1.
     */
    public String nextNr(){
      nextNr +=1;
      return Integer.toString(nextNr);
    }
    
    
    public String sCurrDir(){ return sCurrDir; }
    
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
