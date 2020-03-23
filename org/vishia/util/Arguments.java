package org.vishia.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


/**This is a base class for simple argument handling of main(...) arguments.
 * Usage example: See org.vishia.zip.Zip. It substitutes the org.vishia.mainCmd.MainCmd, less dependencies
 * @author Hartmut Schorrig, LGPL-License
 */
public abstract class Arguments {

  
  
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
  
  
  
  /**Replaces expressions "...$$name$... with the content of the named environment variable
   * @param argval
   * @return replaced environment
   * @throws IllegalArgumentException on faulty name of environment variable
   */
  public String replaceEnv(String argval) {
    int posEnv;
    String argvalRet = argval;
    while( (posEnv=argvalRet.indexOf("$$")) >=0) {
      int posEnvEnd = argvalRet.indexOf('$', posEnv+2);
      String nameEnv = argvalRet.substring(posEnv+2, posEnvEnd);
      String env = System.getenv(nameEnv);
      if(env == null) throw new IllegalArgumentException("Environment variable " + nameEnv + "expected, not found");
      argvalRet = argvalRet.substring(0, posEnv) + env + argvalRet.substring(posEnvEnd+1);
    }
    return argvalRet;
  }
  
  
  


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
          emptyArg = argTest;  //possible argument if nothing met.
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
  
  
  public void checkArgs(String[] args) {
    int nArg = -1;
    String arg = null;
    try {
    for(String arg1: args) {
      arg = arg1;
      if(arg.startsWith("--@")) {
        BufferedReader farg = new BufferedReader(new FileReader(arg.substring(3)));
        while( (arg = farg.readLine()) !=null) {
          if(!testArgument(arg, ++nArg)) {
            farg.close();
            throw new IllegalArgumentException( arg);
          }
        }
        farg.close();
      } else {
        if(!testArgument(arg, ++nArg)) {
          throw new IllegalArgumentException( arg);
        }
      }
    }
    } catch(IOException exc) {
      throw new IllegalArgumentException("File not found or IO-error: " + arg);  //it is faulty
    }
  }
  
  
}
