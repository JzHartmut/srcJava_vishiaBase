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

  
  /**Version, history and license.
   * <ul>
   * <li>2025-03-15: {@link #writef(String, Object...)} Now calls String.format only 
   * if at least one argument for it is given.
   * The problem was, that without arguments the string has contained '%' in the text, not considerate,
   * and the format has produced an error.
   * <li>2023-12-11 created, necessity of some base operations-
   * </ul>
   * <br><br>
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
  public static final String version = "2023-03-15";

  
  /** All reports with a level less than or equal this level will be reported. Other report calls has non effect.*/
  public int nLogLevel = error;  //default: reports only errors, old: warnings and info, but no debug


  
  /**Implemented only as wrapper around simple append output.
   * It is without throws, simple usage.
   * It prepares the text with args using {@link String#format(String, Object...)}
   * and then sends it via {@link #writeInfo(CharSequence)}.
   * @since 2025-03: If args are not given, then it does not call String.format(...). 
   *   It means no consideration of (maybe faulty) format chars in msg. This makes it more universal.
   */
  @Override public void writef(String msg, Object... args) {
    try {
      if(args !=null && args.length >0) {
        writeInfo(String.format(msg, args));
      } else {
        writeInfo(msg);
      }
      flush();
    } catch(Exception exc) {
      System.err.println("EXCEPTION writef: " + msg + " exc: " + exc.getMessage());
    }
  }

  /**Implemented only as wrapper around simple append output.
   * It is without throws, simple usage.
   * May be adapted to message output in derived classes
   */
  @Override public void writeInfo(String msg, Object... args) {
    try {
      String msg2 = msg.charAt(0) != '\n' ? "\n" + msg : msg;  // only stupid compatibility
      writeInfo(String.format(msg2, args));
      flush();
    } catch(Exception exc) {
      System.err.println("EXCEPTION writeInfo: " + msg + " exc: " + exc.getMessage());
    }
  }

  @Override public void writeInfoAdd(String msg, Object... args) {
    try {
      writeInfo(String.format(msg, args));
      flush();
    } catch(Exception exc) {
      System.err.println("EXCEPTION writeInfoAdd: " + msg + " exc: " + exc.getMessage());
    }
  }

  /**Implemented only as wrapper around simple append output.
   * It is without throws, simple usage.
   * May be adapted to message output in derived classes
   */
  @Override public void writeWarning(String msg, Object... args) {
    try {
      String msg2 = msg.charAt(0) != '\n' ? "\n" + msg : msg;  // only stupid compatibility
      append(String.format(msg2, args));
    } catch(Exception exc) {
      System.err.println("EXCEPTION writeWarning: " + msg + " exc: " + exc.getMessage());
    }
  }

  @Override public void writeWarningAdd(String msg, Object... args) {
    try {
      append(String.format("\n" + msg, args));
    } catch(Exception exc) {
      System.err.println("EXCEPTION writeWarningAdd: " + msg + " exc: " + exc.getMessage());
    }
  }

  /**Implemented only as wrapper around simple append output.
   * It is without throws, simple usage.
   * May be adapted to message output in derived classes
   */
  @Override public void writeError(String msg, Object... args) {
    try {
      if(msg.charAt(0) != '\n') append('\n');  // only stupid compatibility
      append(String.format(msg, args));
    } catch(IOException exc) {
      System.err.println("EXCEPTION writeError: " + msg + " exc: " + exc.getMessage());
    }
  }

  @Override public void writeErrorAdd(String msg, Object... args) {
    try {
      append(String.format(msg, args));
    } catch(IOException exc) {
      System.err.println("EXCEPTION writeErrorAdd: " + msg + " exc: " + exc.getMessage());
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
