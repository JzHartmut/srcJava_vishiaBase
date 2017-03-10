package org.vishia.jzTc;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;

public class JzTcUtil
{
  
  protected static JzTcUtil singleton = new JzTcUtil();
  
  protected PrintStream systemOutOld, systemErrOld;
  protected ByteArrayOutputStream consoleOut = new ByteArrayOutputStream();

  
  
  public static CharSequence oneLine(CharSequence text, String replaceLinefeed){
    StringBuilder buffer = new StringBuilder(text);
    int zReplaced = replaceLinefeed.length();
    int posLinefeed = 0;  
    do{
      int posLinefeedEnd;
      char ctest = '\n';
      posLinefeed = buffer.indexOf("\r", posLinefeed);
      if(posLinefeed < 0){ ctest = '\r'; posLinefeed = buffer.indexOf("\n", posLinefeed); }
      char cc;
      if(posLinefeed < buffer.length()-1 && (cc=buffer.charAt(posLinefeed +1)) == ctest){ posLinefeedEnd = posLinefeed +2; }
      else { posLinefeedEnd = posLinefeed +1; }
      if(posLinefeed >=0){
        buffer.replace(posLinefeed, posLinefeedEnd, replaceLinefeed);
        posLinefeed += zReplaced;  //don't replace a linefeed in the inserted replaceLinefeed string.
      }
    } while(posLinefeed >=0);
    return buffer;
  }
  
  
  
  
  /**Redirects the System.out and the System.err to write into an internal buffer.
   * The output can read using {@link #systemOut()} and  
   * Use {@link #unRedirectOutErr()} to switch off this method.
   * If the output is redirected already, this method was called twice, it has no additional effect.
   */
  public static void redirectOutErr(){
    if(singleton.systemOutOld == null){
      singleton.systemOutOld = System.out;
      singleton.systemErrOld = System.err;
      System.setOut(new PrintStream(singleton.consoleOut));
      System.setErr(new PrintStream(singleton.consoleOut));
    }
  }
  
  
  
  
  /**annulling a redirection of System.out and the System.err.
   * If the output is not redirected, it has no additional effect.
   */
  public static void nonRedirectOutErr(){
    if(singleton.systemOutOld != null){
      System.setOut(singleton.systemOutOld);
      System.setErr(singleton.systemErrOld);
      singleton.systemOutOld = null;
      singleton.systemErrOld = null;
    }
  }
  

  
  
  /**Gets the outputted String and clear the buffer of a redirected System output.
   * see {@link #redirectOutErr()} 
   * @return
   */
  public static String out(){
    String ret = singleton.consoleOut.toString();
    singleton.consoleOut.reset();
    return ret;
  }
}
