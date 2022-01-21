package org.vishia.util;

import java.io.BufferedReader;
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

    public String argValue;             // the variables which holds the given args
    
    boolean foundOptionArg = false;     // state for check
    
    
    Args(){
      super.aboutInfo = "Your aboutInfo - 2021-12-19";
      super.helpInfo="obligate args: xxx info";  
      addArg(new Argument("-cfg", ":path to config file/dir usage $(ENV) possible", this.setCfg));
      addArg(new Argument("-data", ":path to data file/dir usage $(ENV) possible", this.setData));
      addArg(new Argument("", "argument without option", this.setDefault));
    }
    
    Arguments.SetArgument setCfg = new Arguments.SetArgument(){ @Override public boolean setArgument(String val){ 
      Args.this.foundOptionArg = true;
      argValue = val;           //the value after the colon -cfg:VAL
      return true;              //note: you can check the val and return false if not proper.
    }};
    
    
    Arguments.SetArgument setCfg = new Arguments.SetArgument(){ @Override public boolean setArgument(String val){ 
      if(Args.this.foundOptionArg) {
        return false;                  // option-less argument only admissible as alone one.
      }
      //... use val for option-less argument
    }};
    

    (at)Override
    public boolean testArgs(Appendable msg) throws IOException {
      return true;   //can check the argument values
    }
  }
 * </pre>
 * You should instantiate in your application:<pre>
  Args argData = new Args();
 * </pre>
 * In the main you should call:<pre>
  public static void main(String[] args){
    MyAppl main = new MyAppl();
    for(String arg: argData) { System.out.println(arg); }
    try {
      if(  false == main.argData.parseArgs(args, System.err)
        || false == main.argData.testArgs(System.err)
        ) { 
        System.exit(1); 
      }
      //... call your application using the read argData
 * </pre>
 * 
 * 
 * @author Hartmut Schorrig, LGPL-License Do not remove the license hint.
 */
public abstract class Arguments {

  /**Version, history and license.
   * Changes:
   * <ul>
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
  public final static String sVersion = "2022-01-17";

  
  /**Interface for implementation of setting arguments.
   * The implementation can be written with an anonymous implementation with the simple form:
   * <pre>
   * MainCmd.SetArgument setArgxy = new MainCmd.SetArgument(){
   *   @Override public boolean setArgument(String val){
   *     args.argxy = val;
   *     return true;
   * } }
   * <pre>
   * The implementation should be an instance inside the user's class. This example shows an argument instance
   * named 'args' where the values of the given arguments are stored.
   * <br><br>
   * The implementation method can test the admissibility of the argument's value. It can return false
   * to designate that the value is not valid. For example the existence of a file can be checked.
   */
  public interface SetArgument{ 
    boolean setArgument(String val); 
  }
  

  
  
  /**Class to describe one argument. One can be create static instances with constant content. Example:
   * <pre>
   * Argument[] argList =
   * { new Argument("", " argument without key on first position", setmethod)
   * , new Argument("-arg", ":keyed argument", setmethod)
   * , new Argument("-x", ":the help text", setx)
   * , new Argument("", " argument without key on any position", setx)
   * };
   * </pre>
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
  public static class Argument{ 
    final String arg; 
    final String help; 
    final SetArgument set;
    
    public Argument(String arg, String help, SetArgument set){
      this.arg = arg; 
      this.help = help; 
      this.set = set;
    }
    @Override public String toString(){ return arg + help + '\n'; }
  }
  

  protected String aboutInfo;
  
  protected String helpInfo;
  
  protected List<Argument> argList;
  
  public int exitCodeArgError = 6;
  
  String sLogPath;
  
  String sLogLevel;
  
  protected void addArg(Argument arg) {
    if(this.argList == null) { this.argList = new LinkedList<Argument>(); }
    this.argList.add(arg);
  }
  
  
  
  /**Replaces expressions "...$name... or $(name)... or $$name$ with the content of the named environment variable.
   * $name: the java-identifier is used as name. Since 2022-01
   * @param argval String with environment variables to replace.  
   * @return replaced environment
   * @throws IllegalArgumentException on faulty name of environment variable
   */
  public static String replaceEnv(String argval) {
    int posEnv;
    String argvalRet = argval;
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
   * <ul>
   * <li> --- The argument is ignored
   * <li> --report:value sets {@link #sLogPath}
   * <li> --rlevel:value sets {@link #sLogLevel}
   * </ul>
   * 
   * @param argc The given argument
   * @param nArg position of the argument in the container, counted from 0
   * @return true if argument is accepted, false if not found in the {@link #argList} given on ctor
   */
  protected boolean testArgument(String argc, int nArg) {
    String value;
    boolean bOk = true;
    if(this.argList !=null){
      Argument emptyArg = null;
      Argument argFound = null;
      int argLenFound = 0;
      int argclen = argc.length();
      int ixArglist = 0;
      int lastIxArglist = this.argList.size()-1;
      Iterator<Argument> iter = this.argList.iterator();
      while(argFound == null && iter.hasNext()){  //break while if found.
        Argument argTest = iter.next();
        int argLen = argTest.arg.length();
        if(argLen == 0){
          emptyArg = argTest;  //possible argument if nothing met. It checks all other argument possibilities.
        } else {
          boolean bSeparator = false;
          if((argc.startsWith(argTest.arg)                //correct prefix 
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
        bOk = argFound.set.setArgument(argval);   //call the user method for this argument.
        //if(!bOk) throw new ParseException("Argument value error: " + argc, nArg);
      } 
      else if(emptyArg !=null){ //argument start string not found but an empty choice possible:
        //set the empty arg.
        return emptyArg.set.setArgument(argc);
      } 
      else {
        //argument not found (not returned in for-loop):
        return false;
      }
    }
    else if((value = checkArgVal("--report", argc)) !=null) 
    { this.sLogPath = value;   //an example for default output
    }
    else if((value = checkArgVal("--rlevel", argc)) !=null) 
    { this.sLogLevel = value;   //an example for default output
    }
    else if(argc.startsWith("---")) 
    { //accept but ignore it. Commented calling arguments.
    }
    else { 
      return bOk = false;
    }
    return bOk;
  }
  
  /**Checks whether an option arg is given, the arg should be identically with check
   * @param check
   * @param arg
   * @return
   */
  protected boolean checkArg(String check, String arg) {
    if(  arg.equals(check)) { 
      return true;
    } else { return false; }
  }
  
  
  
  /**Check whether an arg with value is given.
   * The arg should contain <code>check:value</code> or <code>check=value</code>
   * whereby <code>check</code> is the given check String.
   * @param check
   * @param arg
   * @return null if not matching, the <code>value</code> it is matching.
   */
  protected String checkArgVal(String check, String arg) {
    int zCheck = check.length();
    char cc;
    if(  arg.startsWith(check) 
      && arg.length() > zCheck 
      && ((cc = arg.charAt(zCheck)) == ':' || cc == '=')
      ) {
      return arg.substring(zCheck+1);
    } else { return null; }
  }
  

  
  /**This is the user operation to process all arguments from a container as String[].
   * It is especially for  <code>main(String[] args)</code>
   * @param args The argument string array.
   * @throws IOException 
   * @throws IllegalArgumentException on argument error
   */
  public void checkArgs(String[] args) throws IOException {
    parseArgs(args, null);
  }
  
  
  /**This is the user operation to process all arguments from a container as String[].
   * It is especially for  <code>main(String[] args)</code>
   * @param args The argument string array.
   * @throws IOException 
   * @throws IllegalArgumentException on argument error
   */
  public void parseArgs(String[] args) throws IOException {
    parseArgs(args, null);
  }
  
  
  
  /**This is the user operation to process all arguments from a container as String[].
   * It is especially for  <code>main(String[] args)</code>
   * @param args The argument string array.
   * @param errMsg if given then all arguments will be parsed. Errors will be outputed here.
   * @throws IOException only on unexpected errors writing errMsg 
   * @throws IllegalArgumentException on argument error only if errMsg == null
   * @return true if all ok, but the user should call {@link #testArgs(Appendable)} afterwards.
   *         false argument error, the application may be used though if {@link #testArgs(Appendable)} returns true.
   */
  public boolean parseArgs(String[] args, Appendable errMsg) throws IOException {
    boolean bOk = true;
    int nArg = -1;
    String arg = null;
    BufferedReader farg = null;
    try {
      for(String arg1: args) {
        arg = arg1;
        if(arg.startsWith("--@")) {
          farg = new BufferedReader(new FileReader(arg.substring(3)));
          while( (arg = farg.readLine()) !=null) {
            if(!testArgument(arg, ++nArg)) {
              if(errMsg !=null) {
                errMsg.append("  ERROR: ").append(arg).append('\n');
                bOk = false;
              } else {
                farg.close();
                throw new IllegalArgumentException( arg);
              }
            }
          }
          farg.close();
          farg = null;
        } else {
          if(!testArgument(arg, ++nArg)) {
            if(errMsg !=null) {
              errMsg.append("  ERROR: ").append(arg).append('\n');
              bOk = false;
            } else {
              throw new IllegalArgumentException( arg);
            }
          }
        }
      }
    } 
    catch(IOException exc) {
      if(errMsg !=null) {
        if(farg == null) { errMsg.append("  ERROR: File not found: "); }
        else {
          errMsg.append("  ERROR: unexpected File IO-error: ");
          farg.close();
          farg = null;
        }
        errMsg.append(arg).append('\n');
        bOk = false;
      } else {
        if(farg !=null) { farg.close(); }
        throw new IllegalArgumentException("File not found or IO-error: " + arg);  //it is faulty
        //throw new IllegalArgumentException( arg);
      }
    }
    return bOk;
  }
  
  
  /**Writes all arguments with {@link Argument#arg} {@link Argument#help} in its order in one in a line.
   * @param out Any output channel
   * @throws IOException only on unexpected problems with out
   */
  public void showHelp(Appendable out) throws IOException {
    out.append(this.aboutInfo).append('\n');
    out.append(this.helpInfo).append('\n');
    for(Argument arg: this.argList) {
      out.append(arg.toString());  //note: toString ends with \n already.
    }
  }
  
  
  public String aboutInfo() { return this.aboutInfo; }
  
  
  /**This operation should be implemented and called by the user.
   * It should check the consistency of the given arguments as it is need by the application.
   * <br>
   * Implementation hint: This operation may invoke {@link #showArgs(Appendable)} to output the help info on error.
   * @param msg to write out an info as line with \n for faulty arguments. {@link java.lang.System#err} can be used.
   * @return true if consistent. 
   * @throws IOException only on unexpected problems writing msg
   */
  public abstract boolean testArgs(Appendable msg) throws IOException;
  
}
