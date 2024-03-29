package org.vishia.util;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


/**This is a base class for simple argument handling of main(...) arguments.
 * It substitutes the org.vishia.mainCmd.MainCmd, less dependencies.
 * Usage example:<br>
 * In your main class you should create (template):<pre>
  public static class Args extends Arguments {

    /**Argument is manually tested * /
    public String sTitle;

    /**Argument needs a Arguments.SetArgument(...) operation, longer form * /
    public File fOut;
    
    /**Argument contains the timestamp.val to use as value, shorter form * / 
    public Argument timestamp = new Argument("-time", ":yyyy-MM-dd+hh:mm sets a timestamp in UTC (GMT)");
    
    Arguments.SetArgument setOutput = new Arguments.SetArgument(){ @Override public boolean setArgument(String val){ 
      Args.this.fOut = new File(val);
      return true;
    }};
    
    Args(){
      super.aboutInfo = "...your about info";
      super.helpInfo="obligate args: -o:...";
      addArg(new Argument("-o", ":path/to/output.file", this.setOutput));
      addArg(timestamp);
    }

    /**This operation is necessary only for manual test of argument Strings. * /
    (at)Override protected boolean testArgument(String arg, int nArg) { 
      boolean bOk = true;  //set to false if the argc is not passed
      String value;
      if( (value = checkArgVal("-title", arg)) !=null) {       
        this.sTitle = value;  //the graphic GUI-appearance
      }
      else {
        bOk = super.testArgument(arg, nArg);
      }
      return bOk;
    }

    /**This operation checks the consistence of all operations. * /
    (at)Override
    public boolean testArgs(Appendable msg) throws IOException {
      boolean bOk = true;
      if(this.fOut == null) { msg.append("-o:outfile obligate\n"); bOk = false; }
      if(!bOk) {
        super.showHelp(msg);
      }
      return bOk;
    }
    
  }
 * </pre>
 * You should instantiate in your application:<pre>
  public static int amain(Args args) {
    MyAppl thiz = new MyAppl(args);          // call with your proper prepared arguments
    int exitCode = thiz.exec();              // arguments can be given whatever it is necessary or possible
    return exitCode;
  }</pre>
 * It offers an <code>amain(args)</code> which is also usable inside another Java program, with given Args,
 * or for example inside a JzTxtCmd script where also an exception or a error return value will be processed in a specific kind.
 * <br><br>
 * The <code>amain(args)</code> is called from:<pre>
  public static int smain ( String[] sArgs, Appendable logHelp, Appendable logError) {
    CmdArgs args = new CmdArgs();                // your inherit class of Arguments
    if(sArgs.length ==0) {                       // show help if no arguments are given.
      args.showHelp(logHelp);
      return(1);                
    }
    if(  ! args.parseArgs(sArgs, logError)       // returns true if no argument error
      || ! args.testConsistence(logError)        // returns true if all arguments are proper
      ) { 
      return(254);                               // argument error
    }
    //LogMessageStream log = new LogMessageStream(System.out);
    int exitCode = amain(args);                  // internal amain with prepared Args
    return exitCode;
  }</pre>
 * In the main you should call:<pre>
  public static void main ( String[] sArgs) {
    try {
      int exitCode = smain(sArgs);
      System.exit(exitCode);                     // the exit code for the cmd line script
    } catch (Exception e) {                      // last fall back, unexpected exception
      System.err.println("Unexpected: " + e.getMessage());
      e.printStackTrace(System.err);
      System.exit(255);                          // the exit code for unexpected for the cmd line script
    }
  }</pre> 
 * <br>
 * This class supports
 * <ul>
 * <li>Arguments in any order
 * <li>Arguments starting with a prefix, typical <code>-x:...</code>
 * <li>Detection of arguments without prefix if no prefix matches
 * <li><code>---comment</code> simple comment args with three minus
 * <li><code>--@path/to/file</code> read arguments from a file, whereas one line is one argument.
 * <li><code>--@path/to/file:label</code> read arguments from a part of a file after the label, see next.
 * </ul>
 * The possibility to read the arguments from any file after a label provides the possibility to write arguments in the calling file
 * which are recognized as commented lines in that file. For example:<pre>
 * java -cp classpath pkg.path.Class --@localfile.bat:myLabel
 * ::myLabel ##
 * ::-o:path/to/myoutput  ##That is the output file for xyz
 * ::inputfile
 * pause
 * </pre>
 * This is an example/pattern for a batch file, which contains the arguments one per line in the argument file.
 * <ul>
 * <li>The characters before the label are used as identification for the arguments lines. 
 *   <ul><li>So long lines with this start chararcters, here ".." comes, this are argument lines.
 *   <li>A line without this identification ends the argument line sequence.
 *   <li>proper for Windows-batch script: The identification ".." is also the designation of a comment line in Windows
 *   <li>For shell script use "#" or "##" instead with the same effect.
 *   </ul>
 * <li>If a space and a identification for end line comments are written, here " ##", 
 *   then this end line comment and all trailing spaces before the comment are removed.
 *   <br>Hence, a nice possibility to comment arguments. The " ##" is usual proper except you need ## in your argument.
 * </ul>    
 * 
 * @author Hartmut Schorrig, LGPL-License Do not remove the license hint.
 */
public abstract class Arguments {

  /**Version, history and license.
   * Changes:
   * <ul>
   * <li>2023-12-08 Hartmut new: now supports "--@:", then the next argument string is one line per argument. 
   *   This is not for command line arguments, it is for java String given arguments 
   *   for example while call with a JZtxtCmd script.  
   * <li>2023-08-09 Hartmut new: supports --help and --version
   * <li>2023-08-09 Hartmut new: An exception in evaluation of the argument creates a proper message, which is output or thrown.
   *   {@link #parseArgs(String[], Appendable)} is now exception free, more simple in handling.
   *   If the {@link #helpInfo} starts with "!" then it will be written on emtpy arguments.
   *   With that changes the usage is more simple, see example in javadoc to the class.
   * <li>2023-07-14 Hartmut new in {@link #replaceEnv(String)}: If it starts with "/tmp/" it substitutes the TMP on windows.  
   * <li>2023-03-17 Hartmut new {@link #readArguments(String[])} as ready to usable in main().
   * <li>2023-01-27 Hartmut new in sArg in a argument file: "$=" is the directory of the argument file.
   *   With this designation all arguments can be related to the argfile's directory
   *   which is given in the "--@path/to/argfile". This is an amazing feature, but similar as relative links in html. 
   * <li>2022-11-13 Hartmut chg {@link #checkArgVal(String, String)} improved, see comments in code there. Accept spaces
   * <li>2022-11-13 Hartmut chg rename {@link #testConsistence(Appendable)} instead testArgs(...), more expressive. Adaption necessary and done in all vishia Files 
   * <li>2022-11-13 Hartmut chg all files are created with new File(System.getProperty("user.dir"),
   *   hence the current directory comes from a maybe changed user.dir in Java, not from the original operation system's one.
   *   Important if a java program runs in a JZtxtcmd script, then cd can be used. 
   * <li>2022-05-24 Hartmut new {@link Argument} can now also contain the value itself, more simple for pure String arguments.
   * <li>2022-05-24 Hartmut new possibility of --@file:label as described. 
   * <li>2022-04-09 Hartmut Desription of class improved, from another usage. 
   * <li>2022-04-09 Hartmut ---commented-argument now accepted
   * <li>2022-01-17 Hartmut only comment for usage 
   * <li>2022-01-17 Hartmut enhancement: {@link #replaceEnv(String)} with also $identifier 
   * <li>2020-03-20 Hartmut: created, more simple as the before used {@link org.vishia.mainCmd.MainCmd}.
   * </ul>
   * 
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
   */
  public final static String sVersion = "2023-07-14";

  
  /**Interface for implementation of setting arguments.
   * The implementation can be written with an anonymous implementation with the simple form:
   * <pre>
   * MainCmd.SetArgument setArgxy = new MainCmd.SetArgument(){
   *   @Override public boolean setArgument(String val) throws FileNotFoundException{
   *     args.argxy = val;    // can also uses conversion, may be throwing the FileNotFoundException
   *     return true;
   * } }
   * <pre>
   * The implementation should be an instance inside the user's class. This example shows an argument instance
   * named 'args' where the values of the given arguments are stored.
   * <br><br>
   * The implementation method can test the admissibility of the argument's value. It can return false
   * to designate that the value is not valid. For example the existence of a file can be checked.
   * @since 2023-09-21 with possible {@link FileNotFoundException}
   */
  public interface SetArgument{ 
    boolean setArgument(String val) throws FileNotFoundException; 
  }
  
  

  
  /**Class to describe one argument. One can be create static instances with constant content. Example:
   * <pre>
  Argument[] argList =
  { new Argument("", " argument without key on first position", setmethod)
  , new Argument("-arg", ":keyed argument", setmethod)
  , new Argument("-x", ":the help text", setx)
  , new Argument("", " argument without key on any position", setx)
  };
  </pre>
   * <ul>
   * <li>If the {@link #arg} is empty and it is not on the last position, this is a non keyed argument
   * which is expect on this position in the argument list.
   * <li>If the {@link #arg} is not empty, it is the key for the argument. Usual it starts with "-"
   *   but that is not necessary for the algorithm of argument detection. It is only a style of guide
   *   to give arguments. After the argument one of the character '=' or ':' are possible to follow.
   *   But that is not necessary too. If that characters follow, the argument key is taken.
   *   Elsewhere the longest key is detected which is matching. It is possible to request:
   *   "key1value" and "key11value", the longest key detection wins.
   * <li>If the {@link #arg} is empty and it is the last Argument[] which is added with {@link MainCmd#addArgument(Argument[])}
   *   respectively it is the last entry in {@link MainCmd#argList}, then any argument which does not start 
   *   with the given keys are recognized on any position. In this case the distinction between arguments
   *   should be done at user level. For example it may be usual to write "key value key2 value".  
   * </ul>    
   *
   */
  public final static class Argument{ 
    
    /**The value of the string given argument. */
    public String val;
    
    /**The option switch used for detect the argument, accessible for help. */
    public final String option; 
    
    public final String help; 
    
    protected final SetArgument set;
    
    
    /**Ctor for an instance which holds the argument value, stored as string.
     * @param option The option to select this argument, usual "-option", "" for option-less arguments.
     * @param help ":help text", write the dot for proper explanation
     * @param set the operation to set the value.
     */
    public Argument(String option, String help){
      this.option = option; 
      this.help = help; 
      this.set = null;
    }
    
    /**Ctor for an instance which holds the argument value, stored as string and given as default.
     * @param option The option to select this argument, usual "-option", "" for option-less arguments.
     * @param help ":help text", write the dot for proper explanation
     * @param set the operation to set the value.
     */
    public Argument(String option, String value, String help){
      this.val = value;
      this.option = option; 
      this.help = help; 
      this.set = null;
    }
    
    /**Ctor with a specific set operation, the argument value us not used here.
     * @param option The option to select this argument, usual "-option", "" for option-less arguments.
     * @param help ":help text", write the dot for proper explanation
     * @param set the operation to set the value.
     */
    public Argument(String option, String help, SetArgument set){
      this.option = option; 
      this.help = help; 
      this.set = set;
    }
    
    @Override public String toString(){ return option + help + '\n'; }
  }
  

  

  protected String aboutInfo;
  
  protected String helpInfo;
  
  protected List<Argument> argList;
  
  public static int exitCodeArgError = 6;
  
  String sLogPath;
  
  String sLogLevel;
  
  Appendable outMsg, errMsg;
  
  protected final void addArg(Argument arg) {
    if(this.argList == null) { this.argList = new LinkedList<Argument>(); }
    this.argList.add(arg);
  }
  
  
  
  /**Replaces expressions "...$name... or $(name)... or $$name$ with the content of the named environment variable.
   * if starts with "/tmp/" for Windows, replaces "/tmp" with the PATH stored in TMP or TEMP
   * <ul>
   * <li>$name: A java-identifier format is used as name. Since 2022-01,
   * <li>$(name): should be used if after them other Java-identifier are following, as in shell scripts
   * <li>$$name$: Other syntax for the same approach, may be better readable.
   * </ul>
   * @param argval String with environment variables to replace.  
   * @return replaced environment, if nothing is replaced, this is identical with argval (same instance referred)
   *   Hence with argval==returnedArgval it can be checked whether a replacement was done. 
   * @throws IllegalArgumentException on faulty name of environment variable
   */
  public static String replaceEnv(String argval) {
    int posEnv;
    String argvalRet = argval;
    if(argval.startsWith("/tmp/")){                        // this is ok for linux
      String os = System.getenv("OS");
      if(os.startsWith("Windows")) {                       // for Windows replace with TMP path
        String sTmp = System.getenv("TMP");
        if(sTmp == null) { sTmp = System.getenv("TEMP"); }
        if(sTmp !=null) {
          argvalRet = sTmp + argval.substring(4);
        }
      }
    }
    while( (posEnv=argvalRet.indexOf("$$")) >=0) {
      int posEnvEnd = argvalRet.indexOf('$', posEnv+2);
      String nameEnv = argvalRet.substring(posEnv+2, posEnvEnd);
      String env = System.getenv(nameEnv);
      if(env == null) throw new IllegalArgumentException("Environment variable " + nameEnv + "expected, not found");
      argvalRet = argvalRet.substring(0, posEnv) + env + argvalRet.substring(posEnvEnd+1);
    }
    while( (posEnv=argvalRet.indexOf("$(")) >=0) {
      int posEnvEnd = argvalRet.indexOf(')', posEnv+2);
      String nameEnv = argvalRet.substring(posEnv+2, posEnvEnd);
      String env = System.getenv(nameEnv);
      if(env == null) throw new IllegalArgumentException("Environment variable " + nameEnv + "expected, not found");
      argvalRet = argvalRet.substring(0, posEnv) + env + argvalRet.substring(posEnvEnd+1);
    }
    while( (posEnv=argvalRet.indexOf("$")) >=0) {
      int posEnvEnd = posEnv +1;
      int posEnvEnd9;
      char cc;
      if( (cc = argvalRet.charAt(posEnvEnd)) == '(') {
        posEnvEnd = argvalRet.indexOf(')', posEnv+2);
        posEnvEnd9 = posEnvEnd +1;    //after ')' 
      } else {
        while(  posEnvEnd < (argvalRet.length()-1) 
             && Character.isJavaIdentifierPart(cc = argvalRet.charAt(++posEnvEnd))) {
        }
        posEnvEnd9 = posEnvEnd;
      }
      String nameEnv = argvalRet.substring(posEnv+2, posEnvEnd);
      String env = System.getenv(nameEnv);
      if(env == null) throw new IllegalArgumentException("Environment variable " + nameEnv + "expected, not found");
      argvalRet = argvalRet.substring(0, posEnv) + env + argvalRet.substring(posEnvEnd9 + 1);
    }
    return argvalRet;
  }
  
  
  

  /**This operation tests one argument maybe from a container as String[].
   * It can be overridden by the inherit Arguments class but should unconditionally also call
   * <pre>
   *   super.textArgument(argc, nArg);
   * </pre>
   * This operation is only called in {@link #tryTestArgument(String, int, Appendable, Closeable)},
   * which is called in {@link #parseArgs(String[], Appendable)} and nothing else.
   * It is protected to prevent faulty calling.  
   * <ul>
   * <li> --- The argument is ignored
   * <li> --report:value sets {@link #sLogPath}
   * <li> --rlevel:value sets {@link #sLogLevel}
   * </ul>
   * 
   * @param argc The given argument
   * @param nArg position of the argument in the container, counted from 0
   * @return true if argument is accepted, false if not found in the {@link #argList} given on ctor
   * @throws FileNotFoundException If the argument is formally accepted but evaluation cause an exception
   * @since 2023-09-21 with possible {@link FileNotFoundException} accepted by user implementations.
   */
  protected boolean testArgument ( String argc, int nArg) throws FileNotFoundException {
    String value;
    boolean bOk = true;
    if((value = checkArgVal("--report", argc)) !=null) 
    { this.sLogPath = value;   //an example for default output
    }
    else if((value = checkArgVal("--rlevel", argc)) !=null) 
    { this.sLogLevel = value;   //an example for default output
    }
    else if((value = checkArgVal("--help", argc)) !=null) { 
      if(this.helpInfo !=null) { showHelp(this.outMsg); }
    }
    else if((value = checkArgVal("--version", argc)) !=null) { 
      if(this.aboutInfo !=null) { 
        try { 
          this.outMsg.append(this.aboutInfo).append('\n'); 
        } catch(IOException exc) {
          throw new RuntimeException("unexpected IOException: ", exc);
        }
      }
    }
    else if(argc.startsWith("---")) 
    { //accept but ignore it. Commented calling arguments.
    }
    else if(this.argList !=null) {                         // search the argument in the given arglist
      Argument emptyArg = null;
      Argument argFound = null;
      int argLenFound = 0;
      int argclen = argc.length();
      int ixArglist = 0;
      int lastIxArglist = this.argList.size()-1;
      Iterator<Argument> iter = this.argList.iterator();
      while(argFound == null && iter.hasNext()){  //break while if found.
        Argument argTest = iter.next();
        int argLen = argTest.option.length();
        if(argLen == 0){
          emptyArg = argTest;  //possible argument if nothing met. It checks all other argument possibilities.
        } else {
          boolean bSeparator = false;
          if((argc.startsWith(argTest.option)                //correct prefix 
               && (  argclen == argLen                      //only the prefix
                  || (bSeparator = ":=".indexOf(argc.charAt(argLen))>=0))  //or prefix ends with the separator characters.
                  || (argLen == 0 && (ixArglist == nArg || ixArglist == lastIxArglist))                         //argument without key characters
                  )
               && argLen >= argLenFound   
            ){ //then the argument is a candidat
            argLenFound = bSeparator ? argLen +1 : argLen;
            argFound = argTest;
          }
        }
        ixArglist +=1;
      }
      if(argFound !=null){
        //then the argument is correct and associated to this argTest.
        String argval = argclen == argLenFound //no additional value, use argument 
                        //|| argLen == 0      //argument without prefix (no option)
                        ? argc              //then use the whole argument as value.
                        : argc.substring(argLenFound);  //use the argument after the separator as value.
        argval = replaceEnv(argval);
        if(argFound.set ==null) {
          argFound.val = argval;
        } else {
          bOk = argFound.set.setArgument(argval);   //call the user method for this argument.
        }
        //if(!bOk) throw new ParseException("Argument value error: " + argc, nArg);
      } 
      else if(emptyArg !=null){ //argument start string not found but an empty choice possible:
        //set the empty arg.
        bOk = emptyArg.set.setArgument(argc);
      } 
      else {
        //argument not found (not returned in for-loop):
        bOk = false;
      }
    }
    else { 
      bOk = false;
    }
    return bOk;
  }
  
  /**Checks whether an option arg is given without argument value.
   * It is an alternative to {@link #checkArgVal(String, String)}.
   * The arg should be identically with check
   * @param check
   * @param arg
   * @return
   */
  protected final boolean checkArg(String check, String arg) {
    if(  arg.equals(check)) { 
      return true;
    } else { return false; }
  }
  
  
  
  /**Check whether an arg with value is given.
   * Possible variants in arg, whereby <code>check</code> is the given check String.
   * <ul>
   * <li>"check" returns "", no argument value
   * <li>"check  " returns "", nor argument value, trailing spaces are ignored
   * <li>"check:value" returns "value"
   * <li>"check=value" returns "value"
   * <li>"check : value" returns " value", spaces before ":" are ignored
   * <li>"check = value" returns " value"
   * <li>"check value more text" returns "value more text", all after spaces 
   * <li>"checkother ..." returns null, check fails, "check" must end with space or : or =
   * </ul> 
   * @param check The start sequence of the arg
   * @param arg given one argument, either one line in arg file, or sequence till space in cmd line
   * @return null if not matching, the <code>value</code> it is matching.
   */
  protected final String checkArgVal(String check, String arg) {
    int zCheck = check.length();
    if(  arg.startsWith(check) ) {
      if(arg.length() == zCheck) { return ""; }            // single argument -arg
      else {
        int posEnd = " :=".indexOf(arg.charAt(zCheck)); 
        if(posEnd <0) { return null; }                     // not matching, arg must end with space, or : =, such as -argother not accepted
        else {
          while(posEnd ==0 && arg.length() < (++zCheck)) {
            posEnd = " :=".indexOf(arg.charAt(zCheck));    // skip over spaces
          }
          if(posEnd >=0) {                                 // positive case, -arg:value, returns the value
            return arg.substring(zCheck+1);                // or returns "" if only spaces are found. 
          }
          else {
            return arg.substring(zCheck);                  // the case -arg other text returns the "other text" 
          }                                                // it is only for file content or "-arg other text" in cmd line
        }
      }
    } 
    else { return null; }                                  // not matching
  }
  

  
  /**This is the user operation to process all arguments from a container as String[].
   * It is especially for  <code>main(String[] args)</code>
   * @param args The argument string array.
   * @throws IOException 
   * @throws IllegalArgumentException on argument error
   */
  public final boolean parseArgs(String[] args) throws IOException {
    return parseArgs(args, null);
  }
  
  
  
  /**This is the user operation to process all arguments from a container as String[].
   * It is especially for  <code>main(String[] args)</code>
   * @param args The argument string array.
   *   If the {@link #helpInfo} starts with "!" the help info is shown on empty arguments.
   * @param errMsg if given then all arguments will be parsed. Errors will be output here.
   * @throws IOException only on unexpected errors writing errMsg 
   * @throws IllegalArgumentException on argument error only if errMsg == null
   * @return true if all ok, but the user should call {@link #testConsistence(Appendable)} afterwards.
   *         false argument error, the application may be used though if {@link #testConsistence(Appendable)} returns true.
   */
  public final boolean parseArgs(String[] args, Appendable errMsg) {
    boolean bOk = true;
    this.errMsg = errMsg;
    this.outMsg = errMsg !=null ? errMsg: System.out;
    try {
      if(args.length ==0 && this.helpInfo !=null && this.helpInfo.startsWith("!")) {
        showHelp(this.outMsg);
      }
      bOk = parseArgs(args, 0);
    } 
    catch(Exception exc) {  // it is only on file error and maybe not expectable for errMsg.append(...)
      throw new RuntimeException(exc);  // not possible to handle, it is IOException on errMsg.append(...) and farg.close()
    }
    return bOk;
  }
  
  
  private final boolean parseArgs(String[] args, int recursive) throws IOException {
    boolean bOk = true;
    int nArg = -1;
    boolean bPerLine = false;
    for(String arg: args) {
      if(bPerLine) {
        String[] argLines = arg.split("\n");
        parseArgs(argLines, recursive +1);  // <<<<<<<<<<<<<<<<<< call recursively with this lines.
        bPerLine = false;
      }
      else if(arg.equals("--@:")) { //====================== Read args from the next arg String as line per line
        bPerLine = true;
      }
      else if(arg.startsWith("--@")) { //======================= Read args from a file
        int posLabel = arg.indexOf(':', 6);    //search --@D:x:label after 6. position because on 4th position may be a drive separation
        final String sFile, sLabel;
        if(posLabel >0) {
          sFile = arg.substring(3, posLabel);
          sLabel = arg.substring(posLabel+1);            // A label after file path is given, in form: --@D:/path/to/file.ext:Label
        } else {
          sFile = arg.substring(3);
          sLabel = null;
        }
        BufferedReader farg = null;
        File argFile = null;
        String sDirArgFile = null;
        try {
          argFile = FileFunctions.newFile(sFile);          // File in the maybe changed current directory, not only in the originally OS PWD
          try { farg = new BufferedReader(new FileReader(argFile)); }
          catch(IOException exc) {
            if(this.errMsg !=null) {
              this.errMsg.append("  ERROR not found: --@" + argFile.getAbsolutePath()); 
            } else {
              throw new IOException(exc);
            }
          }
          if(farg !=null) {    //---------------------------- The --@file was found and opened:
            sDirArgFile = argFile.getParent();
            int posArg;                                    // position of the argument in the line may be >0
            final String sStartLineArg;                    // then all lines should start with this text before posArg.
            final String sCommentEndline;
            final boolean bTrimTrailingSpaces;
            if(sLabel !=null) {                            // if posLabel >0, then search the first line with this label, after this line args starts.
              int zCheck = sLabel.length()+5;
              posArg = -1;
              String sCheckLine = "";
              while( posArg <0 && (sCheckLine = farg.readLine()) !=null) {
                posArg = StringFunctions.indexOf(sCheckLine, 0, zCheck, sLabel);  //check whether sLabel is found in range 0...4 in the line
              }
              if(posArg <0) {
                errMsg.append("  ERROR: label not found in ").append(arg).append('\n');
                posArg = 0;
                sStartLineArg = null;
                sCommentEndline = null;
                bTrimTrailingSpaces = false;
              } else { //------------------------------------ the requested '   Label' was found in this line, defines the:
                sStartLineArg = sCheckLine.substring(0, posArg); // start of the line before the label, for example '::Label'
                int zLabelEnd = posArg + sLabel.length();        // All lines with arguments should then start with sStartLineArgs
                if(sCheckLine.length() > zLabelEnd) {
                  sCommentEndline = sCheckLine.substring(zLabelEnd).trim();  // Also a comment string can be defined there
                  bTrimTrailingSpaces = sCheckLine.charAt(zLabelEnd) == ' '; // comment string after spaces, then trim the argument till comment
                } else {
                  sCommentEndline = null;                  // comment in arguments only possible with a label
                  bTrimTrailingSpaces = false;
                }
              }
            } else {
              posArg = 0;
              sStartLineArg = null;
              sCommentEndline = null;
              bTrimTrailingSpaces = false;
            }
            // =============================================== Now reads the line of the file for arguments
            while( (arg = farg.readLine()) !=null) {
              if(posArg >0 && !arg.startsWith(sStartLineArg)) { // label mode: break of arguments lines if no more lines with the start line found
                break;
              }
              String sArg = arg.substring(posArg);         // The argument itself from 0 or from the posArg
              int posEnd = sCommentEndline == null ? -1 : sArg.indexOf(sCommentEndline);
              if(posEnd <0 && bTrimTrailingSpaces) {
                posEnd = sArg.length();                    //to trim trailing spaces on non commented line.
              }
              if(posEnd >0) {                              // trim trailing white spaces always on comment or if desired.
                while(posEnd >0 && sArg.charAt(posEnd-1)==' ') { posEnd -=1;}
                sArg = sArg.substring(0, posEnd);
              }
              sArg = sArg.replace("$=", sDirArgFile);      // $= replaces the absolute directory of the own argument file.
              //
              if(  sArg.length() >0) {                     // don't test an empty line in the file
                if(!tryTestArgument(sArg, ++nArg, errMsg, farg)) {
                  bOk = false;
                }
              }
            }
            farg.close();
            farg = null;
            argFile = null;
          }
        }
        catch(IOException exc) {  // it is only on file error and maybe not expectable for errMsg.append(...)
            try {
              if(farg !=null) { farg.close(); }
              String sMsg = arg + "  ERROR: " + ( argFile !=null ? " File=" + argFile.getAbsolutePath() : "" )
                              + exc.getMessage();
              if(errMsg !=null) {
                errMsg.append(sMsg).append('\n');
                bOk = false;
              } else {
                throw new IllegalArgumentException(sMsg);  //it is faulty
              }
            } catch(IOException exc2) {
              throw new RuntimeException(exc2);  // not possible to handle, it is IOException on errMsg.append(...) and farg.close()
            }
          } 
      }
      else {                                             // other argument than --@ in command line
        if(!tryTestArgument(arg, ++nArg, errMsg, null)) {
          bOk = false;
        }
      }
    }
    return bOk;
  }
  
  
  
  /**Wraps {@link #testArgument(String, int)} in try-catch to catch a user Exception while evaluating argument strings.
   * @param argc
   * @param nArg
   * @param errMsg if given, outputs the error
   * @param farg if given, close it on error if error is not given, before an {@link IllegalArgumentException} is thrown
   * @return true if all is ok, false if the argument is faulty and errM(sg is given. never if errMsg==null then throws
   * @throws IOException only on formally IO error on errMsg
   * @throws IllegalArgumentException if errMsg==null and the argument is faulty.
   */
  private final boolean tryTestArgument(String argc, int nArg, Appendable errMsg, Closeable farg) throws IOException {
    boolean bOkArg;
    CharSequence sError = "";
    try {
      bOkArg = testArgument(argc, ++nArg);                 // this operation may be overridden, but should call super.testArguments(...)
    } catch(Exception exc) {                               // an exception comes if testArgument causes it in user level.
      sError = ExcUtil.exceptionInfo(" argument eval error: ", exc, 1, 20);  //prepare a proper info with stack trace
      bOkArg = false;
    }
    if(!bOkArg) {                 // test it, overridden
      if(errMsg !=null) {
        errMsg.append("  ERROR: ").append(argc).append(sError).append('\n');
      } else {
        if(farg !=null) {
          farg.close();
        }
        throw new IllegalArgumentException( argc + sError);
      }
    }
    return bOkArg;
  }
  
  
  
  
  /**Writes all arguments with {@link Argument#arg} {@link Argument#help} in its order in one in a line.
   * @param out Any output channel
   * @throws IOException only on unexpected problems with out
   */
  public void showHelp(Appendable out) {
    try {
      if(this.aboutInfo !=null) { out.append(this.aboutInfo).append('\n'); }
      int pos0 = this.helpInfo.startsWith("!")? 1 :0;
      out.append(this.helpInfo.substring(pos0)).append('\n');
      for(Argument arg: this.argList) {
        out.append(arg.toString());  //note: toString ends with \n already.
      }
    } catch(IOException exc) {
      throw new RuntimeException("unexpected IOException:", exc);
    }
  }
  
  
  public String aboutInfo() { return this.aboutInfo; }
  
  
  /**This operation should be implemented and called by the user.
   * It should check the consistency of the given arguments as it is need by the application
   * and may also prepare some derived arguments, for example .
   * <br>
   * Implementation hint: This operation may invoke {@link #showArgs(Appendable)} to output the help info on error.
   * @param msg to write out an info as line with \n for faulty arguments. {@link java.lang.System#err} can be used.
   * @return true if consistent. 
   * @throws IOException only on unexpected problems writing msg
   */
  public abstract boolean testConsistence(Appendable msg) throws IOException;
 
  
  
  
  /**Read arguments from given command line arguments. Can be used immediately in main().
   * This operation calls System.exit if arguments are wrong. 
   * Alternatively use {@link #parseArgs(String[], Appendable)} and {@link #testConsistence(Appendable)}
   * @param cmdArgs
   */
  public void readArguments(String[] cmdArgs) {
    try {
      if(cmdArgs.length ==0) {
        this.showHelp(System.out);
        System.exit(1);                      // no arguments, help is shown.
      }
      if( ! this.parseArgs(cmdArgs, System.err)
       || ! this.testConsistence(System.err)
        ) { 
        System.exit(Arguments.exitCodeArgError);  // argument error
      }
    }
    catch(Exception exc) {
      System.err.println("Unexpected Exception: " + exc.getMessage());
      exc.printStackTrace();
      System.exit(255);
    }
  }
  
}
