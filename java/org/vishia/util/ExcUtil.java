package org.vishia.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.NoSuchElementException;

public class ExcUtil {

  /**Version, history and license.
   * <ul>
   * <li>2020-03-22 Hartmut {@link #exceptionInfo(CharSequence, Throwable, int, int, boolean)} without newline on end.
   *   It should be clarified from the application. This is a feature change, should be adapted in the application.
   *   Generally a function should not determine too much by itself. Whether of not the line should be terminated
   *   is not a decision by exceptionInfo(), it is a decision by the application. 
   * <li>2020-03-22 Hartmut moved from {@link CheckVs}, a better name. 
   * <li>2020-03-22 Hartmut moved from org.vishia.util.Assert to this class because name clash with junit..Assert
   * <li>2013-07-14 Hartmut chg: {@link #stackInfo(String, int, int)} produces a text which supports hyperlinking
   *   in Eclipse output output console window, like {@link Exception#printStackTrace()}.
   * <li>2013-07-14 Hartmut new: {@link #throwCompleteExceptionMessage(String, Exception)}
   * <li>2013-01-26 Hartmut new: {@link #consoleErr(String, Object...)}, {@link #consoleOut(String, Object...)}:
   *   Possibility to use the original System.out channel even System.setErr() etc. may be invoked.
   * <li> 2013-01-26 Hartmut chg: {@link #assertion(boolean)} etc. are protected now and commented. That are the methods
   *   which can be overridden in another class which is used by {@link #setAssertionInstance(ExcUtil)}.  
   * <li>2012-11-19 Hartmut new: stop() as dummy routine here now.
   * <li>2012-09-02 Hartmut new {@link #exceptionInfo(String, Throwable, int, int)} and {@link #stackInfo(String, int)}
   *   to support a short info output for example for messages. Not the whole stacktrace!
   * <li>2012-08-30 Hartmut some enhancements, especially assert with send a message to System.err.
   * <li>2012-01-19 Hartmut created. The reason was: set an individual breakpoint on assertion statement.
   *   The second reason: flexibility for debugging. The java language 'assert' is too less in functionality. 
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
  public static final String version = "2020-03-22";

  
  /**The System.err and System.out standard console outputs are copied to this class
   * able to use in its original output. The System.out and System.err may be overridden
   * by invocation of {@link java.lang.System#setErr(PrintStream)} with any other output channel, which may be redirected 
   * to the message system itself.
   * If any output should be done while dispatching a message, a loop may be caused. In this case it is wrong
   * to use System.err or System.out. Then this references can be used.
   * Any user can use this references if an output should be done definitely without usage the message system.   
   * <br>
   * This references are set when this interface is used the first time. In this time the System.err
   * and System.out are not changed by the message System itself, because this interface was not used before.
   * 
   */
  private static PrintStream out = System.out, err = System.err;

  
  /**This is only a debug helper, an empty instruction.  */
  public static void stop(){};
  
  
  /**This routine can be used in a catch block of an unexpected exception.
   * With the given Exception now a RuntimeException is thrown.
   * That is because the Exception is intrinsically unexpected. 
   * It should throw to detect the unexpected behavior,
   * but the Exception should not need specifically declared
   * to prevent effort in calling levels for unexpected stuff. 
   * <br>
   * Note: On usage in ctor with initializing final variables in a try block
   * the original RuntimeException line should be written instead,
   * can be copied from here. Because:
   * <br>
   * The java compiler can detect the throw new RuntimeException
   * as satisfaction of non set final variable, the ctor is aborted.
   * If this operation is called instead, an compiler error comes
   * with the message, that final variable are uninitialized.
   * 
   * @param exc The originally thrown Exception. 
   */
  public static void throwUnexpect(Exception exc) {
    throw new RuntimeException("Unexpected" , exc);
  }
  
  /**This routine instantiates the assertion handling with a special user instance.
   * All invocations of the static assertion call ExcUtil.{@link #check(boolean)} 
   * invokes the user {@link #assertion(boolean)} routine up to now.
   * @param instance The users assertion instance.
   */
  public static void setAssertionInstance(ExcUtil instance){
    assertObject = instance;
  }
  
  /**Checks whether an assertion is met.
   * This routine can invoke a special handling of assertion, if
   * @param shouldTrue
   */
  public static void check(boolean shouldTrue){
    if(assertObject == null){ 
      assertObject = new ExcUtil(); //if no assertion instance is given, create this. 
    }
    if(!shouldTrue){
      stop();   //set a breakpoint here to stop for debugging...
    }
    assertObject.assertion(shouldTrue);
  }
  
  
  /**Checks whether an assertion is met.
   * This routine can invoke a special handling of assertion, if
   * @param shouldTrue
   */
  public static void checkMsg(boolean shouldTrue, String msg){
    if(assertObject == null){ 
      assertObject = new ExcUtil(); //if no assertion instance is given, create this. 
    }
    assertObject.assertMsg(shouldTrue, msg);
  }
  
  

  /**Prepares an exception information inclusively some levels of stack trace in a short (one line) form.
   * @param startText Any start text of the returned text. 
   *   If this refers a StringBuilder, it is used to prepare the output text. 
   *   Note that the StringBuilder should be used only locally for that.
   *   That is a typical practice if the text is assembled before. 
   *   It saves dynamic memory allocation operations.
   * @param exc The exception, its getMessage() will be appended
   * @param firstLevel First level of stack. 0 is the routine where the exception is thrown, 1 the caller etc.
   * @param nrofLevels maximum of numbers of levels to show in stack. Use for example 10 to prevent to long lines if it may be deeper.
   * @return A string in form of CharSequence. Use ...toString() to build a String if necessary.
   */
  public static CharSequence exceptionInfo(CharSequence startText, Throwable exc, int firstLevel, int nrofLevels){
    return exceptionInfo(startText, exc, firstLevel, nrofLevels, true);
  }
  
  
  /**Prepares an exception information inclusively some levels of stack trace in a short (one line) form.
   * @param startText Any start text of the returned text. 
   *   If this refers a StringBuilder, it is used to prepare the output text. 
   *   Note that the StringBuilder should be used only locally for that.
   *   That is a typical practice if the text is assembled before. 
   *   It saves dynamic memory allocation operations.
   * @param exc The exception, its getMessage() will be appended
   * @param firstLevel First level of stack. 0 is the routine where the exception is thrown, 1 the caller etc.
   * @param nrofLevels maximum of numbers of levels to show in stack. Use for example 10 to prevent to long lines if it may be deeper.
   * @return A string in form of CharSequence ended with newline (0x0a). Use ...toString() to build a String if necessary.
   */
  public static CharSequence exceptionInfo(CharSequence startText, Throwable exc
      , int firstLevel, int nrofLevels, boolean bWithExceptiontext){
    StringBuilder u;
    if(startText instanceof StringBuilder){
      u = (StringBuilder)startText;
    } else {
      u = new StringBuilder(500);
      u.append(startText);
    }
    if(bWithExceptiontext){
      u.append(exc.toString()).append(" @ ");
      //u.append(exc.getMessage()).append("; ");
    }
    StackTraceElement[] stack = exc.getStackTrace();
    int zStack = stack.length;
    if(firstLevel >= zStack){ firstLevel = zStack-1; }
    if(zStack> firstLevel + nrofLevels){ zStack = firstLevel + nrofLevels; }
    for(int ix = firstLevel; ix < zStack; ++ix){
      u.append(stack[ix].getClassName())
      .append(".").append(stack[ix].getMethodName())
      .append("(")
      .append(stack[ix].getFileName())
      .append(":").append(stack[ix].getLineNumber())
      .append("); ");
    }
//    u.append("\n");
    return u;
  }
  
  
  /**Throws with the given exception but with a modified exception text and with a modified exception type.
   * The sAdd will be added in front of the exception messages - it may be a hint where the exception was thrown.
   * <br><br>
   * Exception types: There may be less exception types for evaluation.
   * <ul>
   * <li>FileNotFoundException: thrown
   * <li>IOException: thrown
   * <li>All other exception types causes a RuntimeException.
   * </ul>
   * The thrown exception does not contain the stackTrace of the inputed Excpetion.
   * 
   * @param sAdd
   * @param exc
   * @throws IOException, RuntimeException
   */
  public static void throwCompleteExceptionMessage(String sAdd, Exception exc) 
  throws IOException
  {
    String excText = exc.toString();
    String excName = exc.getClass().getName();
    if(excName.equals("FileNotFoundException")){
      throw new FileNotFoundException(sAdd + exc.getMessage());
    } else if(exc instanceof IOException){
      throw new IOException(sAdd + exc.getMessage());
    } else if(excName.startsWith("NoSuch")){
      throw new NoSuchElementException(sAdd + excText);
    } else {
      throw new RuntimeException(sAdd + excText);
    }
  }
  
  

  /**Prepares an information about the stack trace without occurring of a exception in a short (one line) form.
   * @param startText Any start text of the returned text
   *   If this refers a StringBuilder, it is used to prepare the output text. 
   *   Note that the StringBuilder should be used only locally for that.
   *   That is a typical practice if the text is assembled before. 
   *   It saves dynamic memory allocation operations.
   * @param nrofLevels maximum of numbers of levels to show in stack. Use for example 10 to prevent to long lines if it may be deeper.
   *   The first level is the caller of this routine.
   * @return A string in form of CharSequence. Use ...toString() to build a String if necessary.
   */
  public static CharSequence stackInfo(CharSequence startText, int nrofLevel){ return stackInfo(startText, 1, nrofLevel); }

  
  /**Prepares an information about the stack trace without occurring of a exception in a short (one line) form.
   * @param startText Any start text of the returned text
   *   If this refers a StringBuilder, it is used to prepare the output text. 
   *   Note that the StringBuilder should be used only locally for that.
   *   That is a typical practice if the text is assembled before. 
   *   It saves dynamic memory allocation operations.
   * @param firstLevel First level of stack. 0 is this routine, 1 the caller etc.
   * @param nrofLevels maximum of numbers of levels to show in stack. Use for example 10 to prevent to long lines if it may be deeper.
   * @return A string in form of CharSequence. Use ...toString() to build a String if necessary.
   */
  public static CharSequence stackInfo(CharSequence startText, int firstLevel, int nrofLevel){
    final CharSequence s;
    try{ throw new RuntimeException("stackInfo");
    } catch(RuntimeException exc){  //use a non-often used Exception type, do not debug it in Eclipse
      //exc.printStackTrace(System.out);
      s = exceptionInfo(startText, exc, firstLevel, nrofLevel, false);
    }
    return s;
  }

  
  /**This routine can handle a assertion to support debugging or reporting.
   * The Stacktrace can help to detect where the assertion occurs.
   * This routine can be overridden with any other instance to provide a special assertion handling.
   * 
   * @param shouldTrue
   */
  protected void assertion(boolean shouldTrue){
    if(!shouldTrue)
      assert(shouldTrue);
      
  }
  
  /**This routine can handle a assertion to support debugging or reporting.
   * The Stacktrace can help to detect where the assertion occurs.
   * This routine can be overridden with any other instance to provide a special assertion handling.
   * 
   * @param shouldTrue
   */
  protected void assertion(boolean shouldTrue, String msg){
    if(!shouldTrue)
      throw new RuntimeException(msg);
      
  }
  
  /**Assert the condition, writes a message to System.err if it is false.
   * This routine can be overridden with any other instance to provide a special assertion handling.
   * @param shouldTrue
   * @param msg If it is null, an info from stacktrace is build.
   */
  protected void assertMsg(boolean shouldTrue,  CharSequence msg){
    if(!shouldTrue){
      if(msg == null){
        msg = stackInfo("assertMsg ", 4);
      }
      System.err.println(msg);
    }
  }
  
  private static ExcUtil assertObject;
  
  
  /**Output to the original System.out channel though {@link java.lang.System#setOut(PrintStream)} 
   * was invoked with another channel after first usage of this class.
   * Note that the user should invoke any action from ExcUtil, for example <code>ExcUtil.check(true);</code>
   * as a first instruction in main(). Then this class saves the original System.out PrintStream object in this class
   * to use in this method.<br>
   * Note that the System.out may be redirected for any other reason, for example usage for Message Dispatching with
   * {@link org.vishia.msgDispatch.MsgPrintStream}. This method is independent of them.
   *  
   * @param text Text to output. One should use "\n" to force a line feed. Format character can be used
   *   in the same kind as {@link java.io.PrintStream#printf(String, Object...)}.
   * @param args The arguments for the formatted text.
   */
  public static void consoleOut(String text, Object ... args){
    out.printf(text, args);
  }


  /**Output to the original System.err channel though {@link java.lang.System#setErr(PrintStream)} 
   * was invoked with another channel after first usage of this class.
   * Note that the user should invoke any action from ExcUtil, for example <code>ExcUtil.check(true);</code>
   * as a first instruction in main(). Then this class saves the original System.err PrintStream object in this class
   * to use in this method.<br>
   * The System.err may be redirected for any other reason, for example usage for Message Dispatching with
   * {@link org.vishia.msgDispatch.MsgPrintStream}. This method is independent of them.
   *  
   * @param text Text to output. One should use "\n" to force a line feed. Format character can be used
   *   in the same kind as {@link java.io.PrintStream#printf(String, Object...)}.
   * @param args The arguments for the formatted text.
   */
  public static void consoleErr(String text, Object ... args){
    err.printf(text, args);
  }


}
