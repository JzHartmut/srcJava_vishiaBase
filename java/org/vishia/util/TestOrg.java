package org.vishia.util;

import java.io.IOException;
import java.util.Formatter;
import java.util.Locale;

/**This class supports testing. It may be seen as an alternative concept to the known Unit test concept (org.junit.Assert).
 * But it is more simple and obviously because the test case is shown, all successfully tests can be shown.
 * The result is given as text, it is able to evaluate for overview.
 * <br><br>
 * Pattern to use: <pre>
  public static void main(String[] args) { 
    TestOrg testOrg = new TestOrg("Test description", 2, args);
    TestClass thiz = new TestClass();                      // create data for test
    thiz.testRoutine(testOrg);                             // can have more as one testRoutine
    testOrg.finish();
  }
 * 
  void testRoutine(TestOrg testParent) {
    TestOrg test = new TestOrg("Test routine description", 4, testParent);
    try {
      result = doSomethingToTest();
      test.expect(result == expected, 5, "test case description");
    } 
    catch (Exception exc) {
      test.exception(exc); 
    }
    test.finish();
  }
 *   
 * </pre> 
 * The output for this situation for a well test can be:<pre>
 * ==================================================
 * ok Test description
 * ok Test routine description
 * ok test case description
 * </pre>
 * or the output is empty. It depends of a calling argument <code>"---TESTverbose:1"</code> on any position of main().
 * If the calling argument is <code>"---TESTverbose:5"</code> for this example it is outputted:
 * </pre> 
 * The output for this situation for a well test can be:<pre>
 * ==================================================
 * Test routine description
 *   ok: test case description
 * </pre>
 * It means the showing output depends on the verbose argument. The output will be written to standard output (System.out).
 * If the output should be more verbose, all test cases are shown. So the tester can see what is tested. 
 * If the verbose level is less, only an overview over the test is created.
 * <br><br>
 * If the test fails, also on verbose level 0 it is shown:<pre>
 * =========================================================================
 * Test description
 * Test routine description
 *   ERROR: test case description @ org.vishia.util.test.Test_TestOrg.mainF(Test_TestOrg.java:45); ... 
 * </pre>
 * It means the test routine (title) and the failing test case is shown inclusively the line with calling stack of mex. 5 levels. 
 * So it may be possible to immediately find the failing reason if it is trivial (which is often so).
 * <br><br>
 * Especially for string comparison using {@link #expect(CharSequence, CharSequence, int, String)} 
 * the position of the first difference can be shown, for example:<pre>
 * ==============================================
 * Test description
 * Test routine description
 *   ERROR: @45 test case description @ org.vishia....
 * ERROR  
 * </pre>
 * Nested test routines will be supported well. See the examples on org.vishia.util.test.Test_TestOrg.java which is found
 * on {@linkplain https://github.com/JzHartmut/testJava_vishiaBase}.
 * <br><br>
 * <b>The evaluation of the test results</b> can be done by searching "ERROR:" in the output text or by text difference evaluation
 * of the produced test output. Especially the second case shows additionally which test cases are changed.
 * The reference output text can be stored in a text file with a specified version in a version management system, 
 * So the progress can be visited. It is some more better than a polish html view result.
 * <br><br>
 * The advantage of this system in comparison to unit test: Very simple. Only this class should be used, no complex package.  
 * <br>
 * An individual test can be start in any main() routine of a test class
 * <br>
 * All tests of a module are programmed immediately by a <code>main()</code> routine in a <code>TestAll</code> class.
 * 
 * @author Hartmut Schorrig LPGL license or maybe second license model with special contract. Do not remove this license entry.
 *
 */
public class TestOrg {

  /**Version, history and license.
   * <ul>
   * <li>2024-07-15 Hartmut new {@link #expect(double, double, double, int, String, Object...)} numeric double and float results 
   * <li>2023-07-15 Hartmut using Locale.ENGLISH for formatted output, elsewhere a goofy 12,34 comes in German numbers instead 12.34
   * <li>2023-02-02 Hartmut {@link #expect(boolean, int, String, Object...)}, {@link #out(CharSequence, Object...)} now with arguemnts  
   * <li>2022-09-13 Hartmut {@link #expect(CharSequence, CharSequence, int, String)} now returns the position of difference for evaluation.  
   * <li>2021-10-06 Hartmut {@link #expect(CharSequence, CharSequence, int, String)} enhanced with showing positions of difference
   * <li>2020-06-21 Hartmut created as new feature for test organization.                  
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
  public static final String sVersion = "2024-07-15";

  
  
  private boolean bOk = true;
  
  private final String title;
  
  private boolean bTitleShown = false;
  
  private final int nTestVerboseLevel;
  
  private final StringBuilder sbTxt = new StringBuilder(200);
  
  private final Formatter formatter = new Formatter(this.sbTxt, Locale.ENGLISH);
  
  private final int nVerboseTitle;
  
  private Appendable out;
  
  private final TestOrg parent;
  

  /**Ctor stores the title and sets the internal {@link #bOk} to true.
   * The output is written to System.out (stdout). It calls {@link #TestOrg(String, int, String[], Appendable)}.
   * @param title it is stored on shown on {@link #finish()} or on an error.
   * @param nVerbose verbose level for the title. 
   *     A lower number is a more prior message, a higher (..9) is a message only for a verbose output.
   *     Hence the output is not shown if the basic level (default 4) is lesser.
   *     This is the possibility to hide too much information about "ok" test cases.
   * @param args if one of the args contains <code>"---TESTverbose:4"</code>
   *   where the "4" is a digit between 0 and 9, then all messages with a nVerboseLevel <= this number 
   *   are shown also in the ok case as information. 
   *   ok messages with verbose level > this level are not shown to prevent to much information about test cases.
   *   The default value is 4.  
   */
  public TestOrg(String title, int nVerbose, String[] args) {
    this(title, nVerbose, args, System.out);
  }
  
  
  /**Ctor like {@link #TestOrg(String, int, String[])} but with:
   * Parameter see {@link #TestOrg(String, int, String[])}  
   * @param testout any output channel (may be an opened file) for the test output.
   */
  public TestOrg(String title, int nVerbose, String[] args, Appendable testout) {
    this.title = title;
    this.nVerboseTitle = nVerbose;
    int nTestVerbose = 4;
    for(String arg: args) {
      if(arg.startsWith("---TESTverbose:")) {
        nTestVerbose = arg.charAt(15) - '0';
      }
    }
    this.nTestVerboseLevel = nTestVerbose;
    this.parent = null;
    this.out = testout;
  }
  
  
  /**Ctor for nested tests. stores the title and sets the internal {@link #bOk} to true. 
   * The settings for the used verbose level and the testout came from parent.
   * @param title it is stored on shown on {@link #finish()} or on an error.
   * @param nVerbose verbose level for the title. 
   *     A lower number is a more prior message, a higher (..9) is a message only for a verbose output.
   * @param parent from the calling routine. On an error or #finish() the title of all parent
   *   are shown.
   */
  public TestOrg(String title, int nVerbose, TestOrg parent) {
    this.title = title;
    this.nVerboseTitle = nVerbose;
    this.nTestVerboseLevel = parent.nTestVerboseLevel;
    this.parent = parent;
    this.out = parent.out;
  }
  
  
  
  /**Compares both CharSequences, should be equal. 
   * If not, shows the position of the first difference and the difference in s1.
   * This difference position should help for evaluate the problem, to support recognizing what's happen without debugging.
   * The difference is shown on error in form "  ERROR: @pos: s1_onPos: txt"
   * @param s1 The test result text to compare.
   * @param s2 The expected text.
   * @param nVerbose level of verbose: Same as in {@link #expect(boolean, int, String)}. 
   *     A lower number is a more prior message, a higher (..9) is a message only for a verbose output.
   * @param txt Understandable description of the test case
   * @return 0 if matching, position of difference in the strings if >0, never <0
   */
  public int expect(CharSequence s1, CharSequence s2, int nVerbose, String txt) {
    int eq = StringFunctions.comparePos(s1, s2);
    final String txtShow;
    if(eq !=0) {
      eq = Math.abs(eq);
      int pmax = s1.length();
      String more = " ";
      if(pmax > eq + 20) { pmax = eq + 20; more = "... "; }
      int plf = StringFunctions.indexOfAnyChar(s1, eq, pmax, "\n\r");
      //CharSequence txtPos = s1.subSequence(eq, pmax);
      if(plf >= eq && plf < pmax) {
        txtShow = "@" + eq + ": " + s1.subSequence(eq, plf) + "\\n" + more + ": " + txt; //show which position is different.  
      } else {
        txtShow = "@" + eq + ": " + s1.subSequence(eq, pmax) + ": " + more + txt; //show which position is different.  
      }
    } else { 
      txtShow = txt; 
    }
    expect_(eq == 0, nVerbose, txtShow, 3);
    return eq;
  }
  
  
  
  /**Compares two numeric values, should be equal in the given accuracy. 
   * @param n1 The test result value to compare.
   * @param n2 The expected value.
   * @param accuracy of the comparison. Note that a float or double operation can deviate in the last digits.
   * @param nVerbose level of verbose: Same as in {@link #expect(boolean, int, String)}. 
   *     A lower number is a more prior message, a higher (..9) is a message only for a verbose output.
   * @param txt Understandable description of the test case
   * @return 0 if matching, position of difference in the strings if >0, never <0
   */
  public void expect(double n1, double n2, double accuracy, int nVerbose, String txt, Object... args) {
    boolean bOk = Math.abs(n1 -n2) < accuracy;
    expect(bOk, nVerbose, txt, args);
  }
  
  /**Simple test check routine
   * @param cond if false, {@link #bOk} and all {@link #parent}.bOk are set to false because the test serie has an error.
   *   If false then "ERROR" on first line position is shown, the txt and 3 stack levels (File and line) are shown
   *   to locate the error in the sources.
   *   If true and nVerbose is <= the given verbose level on constructor (more prior), then then "ok" and the text is shown. 
   * @param nVerbose level of verbose: 
   *     A lower number is a more prior message, a higher (..9) is a message only for a verbose output.
   * @param txt Understandable description of the test case
   */
  public void expect(boolean cond, int nVerbose, String txt, Object ... args) {
    expect_(cond, nVerbose, txt, 3, args);
  }  
  

  /**core test routine
   */
  private void expect_(boolean cond, int nVerbose, String txt, int nStackLevel, Object ... args) {
    boolean bShowAlsoOkTxt =  this.nTestVerboseLevel >= nVerbose;
    if(!cond || bShowAlsoOkTxt && !this.bTitleShown) {
      showParentTitle(null);
    }
    if(cond) {
      if(bShowAlsoOkTxt) { out("\n  ok: "); outln(txt, args); }
    } else {
      TestOrg parent = this;
      while(parent !=null) { parent.bOk = false; parent = parent.parent; }
      //Note: the first levels before nStackLevel are from TestOrg itself, not to show. 
      CharSequence sFileLine = CheckVs.stackInfo("", nStackLevel, 3);
      out("\n  ERROR: "); out(txt, args); out(" @ "); out(sFileLine);  //sFileLine has 0x0a on end.
    }
  }
  
  
  
  private void showParentTitle(String sPreText) {
    if(!this.bTitleShown) {
      if(this.parent !=null) {
        this.parent.showParentTitle(null);
      } else { //before first parent:
        outln("=========================================================================");
      }
      if(sPreText !=null) {
        out(sPreText);
      }
      outln(this.title);  //shows the title of the first parent firstly. 
      this.bTitleShown = true;
    }
  }
  
  
  
  /**Shows a proper text on an non expected exception, sets the internal {@link #bOk} to false 
   * Note: An exception aborts the normal execution. Hence an exception in a test scenario is only practicable
   * if the exception feature itself should be tested. Then the test routine should be written in a special: <pre>
   *   boolean bOk = false;
   *   try {
   *     testRoutineShouldThrow()
   *   } catch Exception(exc) {
   *     bOk = (exc instanceof Type...);
   *     test.expect(exc.getMessage(), expectedMessage,...)
   *   }
   *   test.expect(bOk, ...)
   * </pre>
   * It means this routine <b>is not appropriate</b> to use!
   * This routine is only proper for an unexpected exception. To continue a test though unexected exceptions are possible
   * you should wrap less tests, or only one in a try frame:<pre>
   *   try {
   *     result = testRoutine();
   *     test.expect(result, expectedResult, ...);
   *   } catch Exception(exc) {
   *     test.exception(exc);  //will be notified as not successfully test.
   *   }
   * </pre>
   *   
   * @param exc The exception.
   */
  public void exception(Exception exc) {
    TestOrg parent = this;
    while(parent !=null) { parent.bOk = false; parent = parent.parent; }
    showParentTitle(null);
    CharSequence msg = CheckVs.exceptionInfo("  Exception: ", exc, 0, 5);
    out(msg);
  }
  
  
  /**This should use as last statement in a test routine. It writes "ok Test routine title"
   * if no ERROR output was produced (checks internal {@link #bOk}). depending of the verbose level. 
   * The verbose level is given on ctor of this. If an error was shown in an expect...(...) routine or in 
   * {@link #exception(Exception)} then the title of all test routines are shown already. Hence this routine does nothing. 
   * <br><br>
   * If an inner finish() is called it writes firstly the title of the test routine of parent levels 
   * if they are not shown already.
   * The title of parents are shown if either an error in another parallel test routine before was shown
   * or if a finish() of a called test routine before was executed. 
   * <br><br>
   * See examples is org.vishia.util.test.Test_TestOrg on {@linkplain https://github.com/JzHartmut/testJava_vishiaBase}.
   */
  public void finish() {
    if(this.bOk && this.nTestVerboseLevel >= this.nVerboseTitle) {  //show ok only on the first parent, if  a submodule has no error, show nothing. 
      showParentTitle("ok ");
    } 
  }
  
  
  
  /**Returns true if this test level has no error, may be invoked before finish for additional action on error.
   * @return this.{@link #bOk} of this level.
   */
  public boolean isOk() { return this.bOk; }
  
  
  
  /**Out to the test output. 
   * @param txt
   */
  public void out(CharSequence txt, Object ... args) {
    try{
      CharSequence txt2;
      if(args.length >0) {    // Note: The Appendable of the Formatter is a StringBuilder, that is a CharSequence 
        this.sbTxt.setLength(0);
        this.formatter.format(txt.toString(), args);
        txt2 = this.sbTxt;
      } else { txt2 = txt; }
      this.out.append(txt2);
    } catch(IOException exc) {
      System.err.append("Exception on TestOrg.out / ").append(txt);
    }
  }
  
  /**Out to the test output with following newline ('\n'). 
   * @param txt
   */
  public void outln(CharSequence txt, Object ... args) {
    out(txt + "\n", args);
  }
  
  
  
  
  
  
}
