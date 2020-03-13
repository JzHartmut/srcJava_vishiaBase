package org.vishia.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**This is a base class for simple argument handling of main(...) arguments.
 * @author hartmut Schorrig, LGPL-License
 *
 */
public abstract class Arguments {

  
  String sLogPath;
  
  String sLogLevel;
  
  protected boolean testArgument(String arg, int nArg) {
    String value;
    boolean bOk = true;
    if((value = checkArgVal("--report", arg)) !=null) 
    { this.sLogPath = value;   //an example for default output
    }
    else if((value = checkArgVal("--rlevel", arg)) !=null) 
    { this.sLogLevel = value;   //an example for default output
    }
    else if(arg.startsWith("---")) 
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
