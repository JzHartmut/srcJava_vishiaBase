package org.vishia.msgDispatch;

import java.io.IOException;

import org.vishia.bridgeC.OS_TimeStamp;
import org.vishia.bridgeC.Va_list;
import org.vishia.util.ExcUtil;

/**This class implements some operations of LogMessage which are independent of the particular implementation.
 * It is used via extends instead immediately implements the LogMessage for the particular derived classes.
 * @author Hartmut Schorrig
 * @since 2022-10-00
 */
public abstract class LogMessageBase implements LogMessage {

  
  /** All reports with a level less than or equal this level will be reported. Other report calls has non effect.*/
  public int nLogLevel = error;  //default: reports only errors, old: warnings and info, but no debug


  
  /**Implemented only as wrapper around simple append output.
   * It is without throws, simple usage.
   * May be adapted to message output in derived classes
   */
  @Override public void writeInfo(String msg, Object... args) {
    try {
      writeInfo(String.format("\n" + msg, args));
    } catch(Exception exc) {
      System.err.println("EXCEPTION: " + msg + " exc: " + exc.getMessage());
    }
  }

  @Override public void writeInfoAdd(String msg, Object... args) {
    try {
      writeInfo(String.format(msg, args));
    } catch(Exception exc) {
      System.err.println("EXCEPTION: " + msg + " exc: " + exc.getMessage());
    }
  }

  /**Implemented only as wrapper around simple append output.
   * It is without throws, simple usage.
   * May be adapted to message output in derived classes
   */
  @Override public void writeWarning(String msg, Object... args) {
    try {
      append(String.format("\n" + msg, args));
    } catch(Exception exc) {
      System.err.println("EXCEPTION: " + msg + " exc: " + exc.getMessage());
    }
  }

  @Override public void writeWarningAdd(String msg, Object... args) {
    try {
      append(String.format("\n" + msg, args));
    } catch(Exception exc) {
      System.err.println("EXCEPTION: " + msg + " exc: " + exc.getMessage());
    }
  }

  /**Implemented only as wrapper around simple append output.
   * It is without throws, simple usage.
   * May be adapted to message output in derived classes
   */
  @Override public void writeError(String msg, Object... args) {
    try {
      append('\n').append(String.format(msg, args));
    } catch(IOException exc) {
      System.err.println("EXCEPTION: " + msg + " exc: " + exc.getMessage());
    }
  }

  @Override public void writeErrorAdd(String msg, Object... args) {
    try {
      append(String.format(msg, args));
    } catch(IOException exc) {
      System.err.println("EXCEPTION: " + msg + " exc: " + exc.getMessage());
    }
  }

  /**Implemented only as wrapper around simple append output.
   * It is without throws, simple usage.
   * May be adapted to message output in derived classes
   */
  @Override public void writeError(CharSequence text) {
    try {
      append(text).append('\n');
    } catch(IOException exc) {
      System.err.println(text);
    }
  }

  /**Writes an information line to this or alternatively to System.out
   * @since 2023-12-08 does not write "Info: " because it is on line end. 
   */
  @Override public void writeInfoln ( CharSequence sInfo ) {
    try {
      append(sInfo).append('\n');
    } catch(IOException exc) {
      System.out.println(sInfo);
    }
  }

  @Override public void writeInfo ( CharSequence sInfo ) {
    try {
      append(sInfo);
    } catch(IOException exc) {
      System.out.append(sInfo);
    }
  }

  @Override public void writeWarning ( CharSequence sError ) {
    try {
      append("Warning: ").append(sError).append('\n');
    } catch(IOException exc) {
      System.err.println("Error: " + sError);
    }
  }

  @Override public void writeError ( String sError, Throwable exc ) {
    CharSequence s = ExcUtil.exceptionInfo(sError, exc, 0, 10);
    writeError(s);
  }

  @Override public void report ( int nLevel, CharSequence string ) {
    // TODO Auto-generated method stub
    
  }

  @Override public void reportln ( int nLevel, int nLeftMargin, CharSequence string ) {
    // TODO Auto-generated method stub
    
  }

  @Override public void reportln ( int nLevel, CharSequence string ) {
    // TODO Auto-generated method stub
    
  }

  @Override public void report ( CharSequence sText, Throwable exception ) {
    // TODO Auto-generated method stub
    
  }

  
  /** Set another level inside programming. It is advisable to restore the old level
   * after the designated operation.
   * @param newLevel The level to be set, use one of the defines MainCmdLogging_ifc.info to MainCmdLogging_ifc.fineDebug
   * @return the current level, usefull to restore it.
   */
  @Override public int setReportLevel(int newLevel)
  { int oldLevel = this.nLogLevel;
    this.nLogLevel = newLevel;
    return oldLevel;
  }

  
  @Override public int getReportLevel () {
    // TODO Auto-generated method stub
    return this.nLogLevel;
  }

  @Override public void flushReport () {
    flush();
  }

  @Override public void setReportLevelToIdent ( int ident, int nLevelActive ) {
    // TODO Auto-generated method stub
    
  }

  @Override public int getReportLevelFromIdent ( int ident ) {
    // TODO Auto-generated method stub
    return 0;
  }


}
