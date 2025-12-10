package org.vishia.util;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.Flushable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.vishia.msgDispatch.LogMessage;
import org.vishia.util.Java4C.ConstRef;





/**This class helps to prepare output texts with data.
 * It is a more powerful text preparer in comparison to {@link java.io.PrintStream#printf(String, Object...)}
 * or {@link String#format(String, Object...)}. The first ones are only for number and date formatting.
 * This class allows access to all Java data as placeholder values inclusively conditions and loops. 
 * <ul> 
 * <li>It may be seen as small solution in comparison to {@link org.vishia.jztxtcmd.JZtxtcmd} only for text preparation of one or a few lines. 
 * <li>In opposite to {@linkplain https://www.eclipse.org/xtend/} resolution for ''' TEXTs ''':
 *  The rule to build the text is given outside the programming language, for example in a text file able to control by a user.
 *  It is resolved on runtime, not on compile time. Hence it is able to use to control several texts by user.
 * <li>It may be seen as a better 'printf'. 'printf' is a text interpreter, in C too, but with lesser capability.
 * </ul>
 * An instance of this class is used as formatter for an output text. 
 * The pattern to format can be given as String for the constructor or also as text file 
 * using {@link #readTemplateCreatePreparer(InputStream, String, Class, Map, String)}
 * in the following forms (example, text file via readTemplate...(): 
 * <pre>
 * 
 * <:otx: otxExample : data, placeholder, NEWLINE==nli >
 * Output plain text with <&placeholder.value>
 * <:if:data.cond>conditional output<:else>other variant<.if>
 * <:for:var:data.container>element = <&var_key>: <&var><:if:var_next>, <.if><.for>
 * <:wr:data.wrBuffer>Write this text with <&var> in another buffer<.wr>
 * <:call:subtext:arg=data.var>
 * <:exec:operation(arguments,...)>
 * <.otx>
 * </pre>
 * To execute this script you should call (see {@link #createArgumentDataObj()} and {@link #exec(Appendable, DataTextPreparer)}: 
 * <pre>

StringBuilder sb = new StringBuilder(500);    // for the output text
try {
  OutTextPreparer.DataTextPreparer args = mainOtx.createArgumentDataObj();
  args.setArgument("data", myData);           // The data class for access.
  args.setArgument("placeholder", "any test text");
  args.setExecObj(myExecObj)                  // for <:exec...>
  mainOtx.exec(sb, args);                     // execute the outText script 
} catch(Exception exc) {                      // unexpected exceptions can be occurred, catch it!
  CharSequence sExc = org.vishia.util.ExcUtil.exceptionInfo("unexpected", exc, 0, 10);
  System.err.println(sExc);
}    
 * </pre>
 * <b>Arguments</b><br>
 * The arguments are simple identifier in the 3th argument of the constructor or in the given argument list
 * in an textual script (see {@link #readTemplateCreatePreparer(InputStream, String, Class, Map, String)}).
 * or also possible on start of the script as <code>&lt;:args:var, var2></code>
 * <br>
 * Its values are set in the instance which should be gotten via {@link #createArgumentDataObj()}.
 * One of the importants is:
 * <ul><li>The translated script of an OutTextPreparer after construction is given and persistent.
 *   It is never changed via {@link #exec(Appendable, DataTextPreparer)}. 
 *   That means also, it can be used concurrently in several threads.
 * <li>The {@link DataTextPreparer} which are created by call of {@link #createArgumentDataObj()}
 *   should be used in an execution thread, the data should be set due to the after called {@link #exec(Appendable, DataTextPreparer)}. 
 *   If executions of the same text pattern are executed in several threads, you can have only one reused instance
 *   of the OutTextPreparer but several instances of {@link DataTextPreparer}, each for one execution thread.
 *   You can reuse this instance with {@link #exec(Appendable, DataTextPreparer)} invocations one after another in the same thread.
 * </ul>
 * The instance of the {@link DataTextPreparer} must be matching to the OutputTextPreparer
 * because internally only integer numbers are used for access.
 * <br> 
 * To assign data to the named arguments use the {@link DataTextPreparer#setArgument(String, Object)} 
 * or also {@link DataTextPreparer#setArgument(int, Object)} with given numeric index of the arguments).
 * That are your user data which can be accessed via <code><&arg1....></code> in your pattern. 
 * Compare it with the arguments of a printf, but with the difference that the possibilities to use the arguments
 * are some more powerful, not only for numbers and date. 
 * 
 * <br>
 * <br>
 * <b>General to text and placeholder</b><br>
 * All plain text is output as given. Only a few character sequences determine a placeholder
 * with a processing functionality for data preparation and output. This sequences are only:
 * <ul>
 * <li><code>&lt;&</code> start of a data access till <code> ... ></code>
 * <li><code>&lt;:</code> start for controlling statements till <code> ... ></code> but with:
 * <li><code>&lt;.</code> end of controling statements till <code> ... ></code>
 * <li><code>&lt;:--</code> this is a commented part till <code> --></code>
 *   Note changed 2024-02 before a single <code>&lt;:--... ></code> can only be commented. Now <code>&lt;:--... --></code> is necessary for that.
 * </ul>
 * To output exact this character sequences (for example to produce another OutTextPreparer script)
 * you should use the capapbility <code>&lt;:SPECIAL TEXT></code> for example <code>&lt;:&lt;&></code>
 * to output a <code>&lt;&</code> itself. See next list.
 * <ul> 
 * <li><code>&lt;: ></code> skips over all white spaces till the next placeholder 
 * <li><code>&lt;: ></code> second time after a <code>&lt;: ></code> stops skipping withspaces but outputs a space. 
 * <li><code>&lt;:?nl></code> skips till the next line (skips over the newline character in the script) (since 2023-05)
 * <li><code>&lt;:? ></code>  skips over all white spaces till the next placeholder (since 2023-05)
 * <li><code>&lt;:s></code> outputs a space (since 2023-05, alternatively usable to a second <code>&lt;: ></code> )
 * <li><code>&lt;:n></code> outputs a newline as 0x0a
 * <li><code>&lt;:r></code> outputs a newline as 0x0d
 * <li><code>&lt;:t></code> outputs a tabulator as 0x09
 * <li><code>&lt;:x1234></code> outputs the UTF16 character with the given hexa code, here 1234 
 * <li><code>&lt;:CHARS> CHARS</code> may be any special character sequences, they will be output as given, for example:
 * <li><code>&lt;:&lt;&var>></code> produces the text <code>&lt;&var></code> for generate a OutTextPreparer-Script itself.
 * <li><code>&lt;:&lt;&>var></code> produces the same <code>&lt;&var></code>, other writing style.
 * <li><code>&lt;:&lt;&>&lt;&var>></code> produces the text <code>&lt;&CONTENT></code> 
 *   whereby <code>CONTENT</code> is the content of <code>var</code>.
 *   The truth is: <code> &lt;:&lt;&></code> procuces the <code> &lt;&</code>. 
 *   Then <code>&lt;&var></code> generates the content of the variable, then a simple <code>></code> follows in the text.
 * </ul>
 * Generally, the plain text outside the placeholder syntax  is read without skipping white spaces,
 * hence this special features helps to structure this parts also with white spaces and line breaks.
 * The syntax inside <code>&lt;&...></code> and <code>&lt;:...></code> is white space compliant. 
 * <br>
 * Sometimes the script should be written in several lines, but only one line should be output.
 * for example data from a container should be output, one per line. 
 * For that you can write (example) (since 2023-05):<pre>
 * This is a title line
 * <:for:element:container><:?nl>
 *     <&element.data> written with an indentation
 * <.for><: >
 * </pre>
 * In this script the line for data output should start with a new line, but the necessary first newline 
 * is already given after the title line. The newline after the <code>&lt;:for...></code> line should not be outputted.
 * But in the script the output line should be written as extra line. 
 * For that the <code>&lt;:?nl></code> skips over the next newline in the script.
 * Before 2023.05 the syntax was a little bit more complicated, that runs furthermore: <pre>
 * This is a title line
 * <:for:element:container><: >
 * <: >    <&element.data> written with an indentation
 * <.for><: >
 * </pre>
 * 
 * <br>
 * <br>
 * <b>Placeholder values and expression evaluation via reflection</b><br>
 * <ul>
 * <li><code>&lt;&var></code>: <code>var</code> can be an argument variable. 
 *   Then the value is immediately gotten from the argument as set via {@link DataTextPreparer#setArgument(int, Object)}.
 * <li><code>&lt;&var></code>: <code>var</code> can be an field name in the 'execClass'.
 * <li>TODO improve documentation, static from execClass? 
 * <li> or 'idxConstData' given on construction. 
 *   This is a fast access, only to an container {@link TreeMap} or via indexed array access to the arguments.
 * <li><code>&lt;&data.element></code> is a more complex access expression 
 *   then the capability of {@link DataAccess} is used. This accesses the elements via reflection.
 *   It means the given {@link Class#getDeclaredFields()}, {@link Class#getDeclaredMethods()} etc. are used
 *   to get the data, accessed via {@link java.lang.reflect.Field} etc. 
 *   The access is completely controlled in the {@link DataAccess} class. For usage it is simple.
 * <li><code>&lt;&data.operation(arg) + otherdata></code> can be also a complex expression for numeric calculation,
 *   String concatenation and boolean evaluation. This expression is executed in {@link }
 *   with an interpreted approach. 
 *   It means the calculation needs a longer time then executed in the Java-VM per bytecode/JIT machine code. 
 *   But you can also prepare complex expressions by Java programming and call this expressions or operations.
 *   For that the 'execClass' class can be used which may contain prepared operations for your own. 
 *   Hence you can decide writing simple expressions or also more complex as script or just as Java operations.
 * <li><code>&lt;&#></code> produces the line number, which is automatically count. (since 2025-05)
 * </ul>
 * <br>
 * <b>Formatted numbers</b><br>
 * With <code>&lt;&...:%format></code> an access to a number can be formatted due to the capabilities of java.util.Formatter.
 * For example output a value as hexadecimal presentation write <code>&lt;&access.to.value:%04x></code>.
 * If formatting is not possible an error text from the thrown FormatterException is output. 
 * <br>
 * <br>
 * <b>Control statements</b><br>
 * It is interesting and important to produce an output conditional depending from data, 
 * and also from some container classes.
 * <br><br><b><code>&lt;if:condition>conditional Text&lt;elsif:condition>other Text&lt;:else>else-Text&lt;.if></code></b>
 * The condition is an expression built with the {@link CalculatorExpr#setExpr(StringPartScan, Map, Class, boolean)}
 * <br>
 * for example also a type check is possible: <code><:if:obj ?instanceof classXyz></code>
 * whereas the <code>classXyz</code> can be given in the static reflection class as static variable as
 * <code>public static Class<?> classXyz = MyClassXyz.class; </code> 
 * <br>
 * <br><b><code>&lt;for:var:container>text for any element &lt;&var> with &lt;&var_key> in loop &lt;:if:variable_next>, &lt;.if>&lt;.for></code></b><br>
 * The container can be an array, any {@link Iterable} such a {@link List} or a {@link Map}.
 * Inside the statement 
 * <ul><li><code>var</code> is the value of the container element, 
 * <li><code>var_key</code> is the key if the container is a map, else null
 * <li><code>var_next</code> is the following element or null for the last element 
 * </ul>
 * One can test <code>&lt;if:var_next>....&lt;.if></code> to detect whether there is a following element for example to output an separator.
 * <br>
 * <br>
 * <b>Write to another output <code><:wr:buffer>...<.wr></code></b><br>
 * This is for example usable if texts should be placed on another position, but occurs with the data in this order.
 * It is also usable for example to write log texts in an extra buffer.
 * The <code>buffer</code> is found via reflection data access. The String building the <code>buffer</code> is used
 * to build a variable inside the {@link DataTextPreparer#args} with the index stored in {@link #nameVariables},
 * where the reference to the buffer (as {@link Appendable}) is stored for further usage.
 * It means if the <code><:wr:buffer>...<.wr></code> is used on more positions in the otx with exact the same String for <code>buffer</code>,
 * the same buffer is used immediately, only one time gotten via reflection and stored in {@link DataTextPreparer#args}.
 * <br>
 * <br>
 * <b>Call operations <code><:call:otxSubScript:arg=value:arg2=value,...></code></b><br>
 * The call operation invokes another script to output. The script is present with its own instance of {@link OutTextPreparer},
 * either manually programmed by the constructor or as part of the whole script see {@link #readTemplateCreatePreparer(InputStream, String, Class, Map, String)}.
 * <br>
 * The name of the 'otxScript' should be either found in the 'idxConstData' on construction or call of readTemplate...
 * <pre>
 * <:otx: otxSubScript : arg1, arg2>
 * Any script pattern<.end>
 * </pre>
 * or it should be found as static instance of a programmed OutTextPreparer in the 'execClass' given on construction:
 * <pre>
 * 
  static final OutTextPreparer otxListColors = new OutTextPreparer("otxListColors"
  , null            //no static data on construction
  , "colors, text"  //arguments need and used
  , "<&text>: <:for:color:colors><&color><:if:color_next>, <.if><.for>");  //The pattern.
 * </pre>
 * The association of arguments to the subscript argument variables is done on call from given values.
 * <br>
 * <br>
 * <b>Execute operations in one given class</b><br>
 * The 'execClass' as second argument of the constructor or the 3th argument of {@link #readTemplateCreatePreparer(InputStream, String, Class, Map, String)}
 * gives the possibility to have some static or non static operations to call immediately maybe related to the script.
 * For non static operations the necessary instance should be given in {@link DataTextPreparer#setExecObj(Object)}
 * due to the execution call. 
 * That is powerful because you can use specific parts programmed in Java in your script.
 * <br>
 * Call that operations with <code><:exec:operation(arg1, arg, ...)></code>. 
 * <br>It is similar to a data access written as <code><&path.to.obj.operation(arg1,arg...)</code> 
 * but it is faster parsed and executed due to the immediately given 'execClass'
 * if the operation is member of this 'execClass'.
 * <br><br>
 * <b><code>&lt;:exec:operation(arg)></code></b>:<br>
 * The <code>:arg</code> is optional. The <code>operation</code> will be searched in the given reflection class (ctor argument).
 * The operation is stored as {@link Method} for usage, so this operation is fast. 
 * The optional argument is used from given arguments of call, it should be match to the operations argument type.
 * If the operation is not found, an exception with a detailed error message is thrown on ctor. 
 * If the operations argument is not proper, an exception is thrown on {@link #exec(Appendable, DataTextPreparer)}.
 * <br>
 * <br>
 * <b>Execute operations in any given instance</b><br>
 * If you write <code><&obj.operation(args ...)></code> then the called operation can have any side effects as programmed.
 * Intrinsically it should return a String as variable text similar as <code><&var></code> or <code><&path.to.var></code>.
 * But if the operation is a void operation, or returns null, then no text is created.
 * This (and also side effects in String returning operations) allows change states in the underlying data
 * depending from the script. It is a contribution to free programming controlled by the script.
 * This is sometimes necessary. Hence, the script is not only a text preparer.
 * <br>
 * The second reason using an <code><&obj.operation(args ...)></code> is: It can be get the {@link WriteDst} as argument
 * and writes the created text itself in its body. The buffer can be also a local one, inserted later in the main location
 * of generated text. It should return null if all is ok. 
 * But if the operation has an error, it can return an error message, which is then placed in the code
 * maybe on the current main location. 
 * <br><br>
 * <b>...more</b><br>
 * <ul>
 * <li>&lt;:set:variable=value>: sets a new created variable, can be used as &lt;&variable> etc.
 * <li>&lt;:set:variable='string':value>: If the value starts with a string literal, it can be concatenated with some other values.
 * <li>&lt;:set:variable>....&lt;.set>: unfortunately not ready, should set a variable with a prepared expression with placeholder.
 * <li>&lt;:type:value:org.path.to.Class>: checks the value whether it is of the type of the given class.
 *   This produces an "<??ERROR text??>" if it is not matching. 
 *   <br>Able to switch off for faster execution using {@link DataTextPreparer#setCheck(boolean)} for each script. 
 * </ul>   
 * @author Hartmut Schorrig
 *
 */
public final class OutTextPreparer
{

  
  /**Version, history and license.
   * <ul>
   * <li>2025-12-07: remove listArgs, because there was a big bug: If a variable is declared in a script to use, but not declared to set,
   *   then this variable was missing in listArgs, and access with the faulty index from {@link #nameVariables} for listArgs.get(ix) was done.
   *   That's why now {@link #nameVariablesByIx} is created instead and filled with the names in order to the indices in {@link #nameVariables}.
   *   That is the same functionality as with old listArgs, but not with this error because {@link #nameVariablesByIx} is filled afterwards with all known ix,
   *   whereas listArgs was filled by the way, with not regarded variables.
   * <li>2025-... some changes 
   * <li>2024-08-30 new &lt;:wr:buffer>Output to another buffer.&lt;.wr> See javadoc description.
   * <li>2024-07-14 new in &lt;:for:var:container> {@link ForCmd} also an integer index can be used as var if container is an Integer. 
   *   For that in {@link #addCmd(String, int, int, ECmd, String, Class, Map, Map)}
   *   in case forCmd (called in {@link #parse(Class, Map, Map)} it is tested whether "0.." is written. 
   *   But the real important effect is, the argument container returns an Integer. This is evaluated in {@link #execFor(Appendable, ForCmd, int, Object, DataTextPreparer)}.
   *   The solution runs yet with integer index starting from 0. The solution should be a little bit improved:
   *   TODO either it is necessary to have a start index other than 0, ore superfluous statements should be removed. 
   * <li>2024-07-14 new {@link DataTextPreparer#setArgumentOptional(String, Object)} 
   * <li>2024-05-18 {@link ForCmd#ixEntryKey}, {@link #execFor(Appendable, ForCmd, int, Object, DataTextPreparer)}: 
   *   Now the &lt;:for:var.... creates also a var_key for the key value. 
   * <li>2024-05-17 {@link DataTextPreparer#DataTextPreparer(OutTextPreparer)}: now checks whether the OutTextPreparer is initialized, 
   *   prevent construction (thows) if not. This is if {@link OutTextPreparer#cmds} are empty.
   *   This prevents faulty instances if the {@link DataTextPreparer} is built to early, with a non initialized {@link OutTextPreparer}. 
   * <li>2024-03-22 ##Comment: All spaces before are also removed as space for comment. If ##comment start on a new line, the whole line is ignored.
   *   It is in {@link #readTemplateList(InputStream, String, Object, Map)}. 
   * <li>2024-02-21 Comment in script now till any --> not only this one element <:--..>  It is a changed syntax for comment stuff
   * <li>2024-02-13 new {@link #readTemplateCreatePreparer(Class, String, Map, Object, Map)} replaces the older form without Class,
   *   but {@link #readStreamTemplateCreatePreparer(InputStream, String, Map, Object, Map)} is also available.
   *   The argument String lineStart is no more supported from the newer form. 
   *   All deprecated operations {@link #XXXreadTemplate(InputStream, String)} etc. are marked with XXX, can but should not be used.
   * <li>2024-02-04 formatted output was tested only for one stage access, now works on any access. 
   * <li>2024-01-25 The whole class is now final. It should be never necessary to create a derived version. 
   *   It may optimize some in Runtime. Expect, all operations are automatically final (?) and does not need dynamic binding. 
   *   It's also better for documentation.
   * <li>2024-01-17 small refactoring in {@link #execSub(Appendable, DataTextPreparer, int, int)}:
   *   The arguments are not prepared firstly, instead as argument of the cmd. This is very more better for debugging. 
   *   For that a subroutine {@link #dataForCmd(Cmd, DataTextPreparer, Appendable)} was created. 
   * <li>2024-01-15 new capability for type check: <code>&lt;:type:vaue:classPath></code>, 
   *   able to switch off with {@link DataTextPreparer#setCheck(boolean)} 
   * <li>2024-01-15 {@link #readTemplateCreatePreparer(InputStream, String, Map, Object, Map)} and {@link #readTemplateList(InputStream, String, Object, Map)}:
   *    more arguments dataRoot, idxConstData, for compatibility provide null for both.
   *    With this on template level outside of scripts <code>&lt;:set:name=value></code> is supported now for constant data.
   * <li>2023-12-27 {@link #parseExec(String, int, int, StringPartScanLineCol, Class, Map, Map)}:
   *   Changed behavior for &lt;:exec:...> using the capability of {@link DataAccess#DataAccess(StringPartScan, Map, Class, char)}
   *   and then {@link DataAccess#access(org.vishia.util.DataAccess.DatapathElement, Object, boolean, boolean, Map, Object[], boolean, org.vishia.util.DataAccess.Dst)}
   *   for execution. This supports more as one argument. Before, &lt;:exec:name(arg,...)> has only supported one element.
   *   Syntax change: No colon after operation name, instead operation(args , ...)
   * <li>2023-12-27 The reference to the writer is stored anyway as argument "OUT" in {@link DataTextPreparer#args}.
   *   For that {@link #ixOUT} is stored as whose index in that array args.
   *   The writer reference is stored automatically for each level in the associated data on start of {@link #execSub(Appendable, DataTextPreparer, int, int)}.
   *   It can be used to recursively generate outputs in Java operations called via %lt;:exec:...>.
   * <li>2023-12-26 Some improvements, especially &lt;&...;format> for formatted numeric values. 
   * <li>2023-12-23 General two translate phases are necessary for recursively calls. 
   *   It helps also to use sub scripts before definition in order in the script.  
   *   For that the {@link OutTextPreparer#OutTextPreparer(String, String, String)} does not parse.
   *   After definition of all otx call {@link #parse(Class, Map, Map)} to parse. 
   *   This is not implemented on the other ctor, they are held compatible. 
   * <li>2023-12-23 in {@link #execCall(Appendable, CallCmd, DataTextPreparer, OutTextPreparer)}: 
   *   bugfix for <:exec:...> in deeper <:call:...> level, the exec instance is given now.
   * <li>2023-10-22 Using of {@link #idxScript}, new {@link #readTemplateCreatePreparer(InputStream, Class, Map)}. 
   * <li>2023-08 up to now history to the operations 
   * <li>2023-06-18: frame of operation now <:otx:args>...<.otx>. The <.end> is no more supported, change it to <.otx>
   *   Usage of "=== name ( args) " is deprecated but yet still possible. 
   * <li>2023-06-18: bugfix on nested <:if>...<.if> 
   * <li>2023-05-16: the evaluation of a simple variable is now more simple organized 
   *   in {@link #addCmdSimpleVar(String, int, int, ECmd, String, Class)}. 
   *   This allows now also using the 'idxConstValues' for const texts.
   * <li>2023-05-16: capability of <:x1234> any UTF16 char, not tested yet, should run
   * <li>2023-05-16: some documentation and renaming: 'execClass' instead 'reflData',
   *   {@link #execClass} instead 'clazzPattern'   
   * <li>2023-05-15: new Features <:? > as better syntax for skip spaces, <:?nl> skip after newline. 
   * <li>2023-05-12: new  {@link #readTemplateCreatePreparer(InputStream, String, Class, Map, String)} 
   *   now works completely with a String file given script.
   * <li>2022-04-17: new {@link #readTemplate(InputStream, String)} to support texts from file. Used firstly for {@link org.vishia.java2Vhdl.Java2Vhdl}
   *   This is 2023-05-12 deprecated because {@link #readTemplateCreatePreparer(InputStream, String, Class, Map, String)} does all and replaces it.
   * <li>2022-02-11: little bit improved error message, hint to error position
   * <li>2019-11-13: ## Comment in a line
   * <li>2019-10-20: &lt;: > capability 
   * <li>2019-08-26: StringPartScan instead String for {@link CalculatorExpr.Operand#Operand(StringPartScan, Map, Class, boolean)}
   *   yet not complete.
   * <li>2019-08-26: &lt;:args:....>  
   * <li>2019-05-08: Creation
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
   */
  public static final String version = "2024-08-30";
  
  
  @ConstRef static final public Map<String, Object> idxConstDataDefault = new TreeMap<String, Object>(); {
    //this.idxConstDataDefault.put("null", null);   //This does not work because null is not recognized as constData
    // but usable for others ... in future
  }
  
  
  
  /**This class is used to write out the generated text.
   * It supports line count and debug view of the text.
   * <ul>
   * <li>If one of the ctor is called with a StringBuilder as {@link Appendable}, as the output,
   *   then the field {@link #sb} is also set. It is only a second link, not immediately used for OutText preparation.
   *   It allows more possibilities to deal with the generated text, for example insert a text on a definite position,
   *   which is generated afterwards (for example local variable definition generated on usage of the variable,
   *   but necessary as definition before. But this is outside of OutText preparation. 
   *   The operation {@link #add(WriteDst)} allows adding a generated text to the current output, 
   *   the source to add needs the StringBuilder output.
   * <li>All {@link #append(CharSequence)} operations looks for '\n' inside the output text.
   *   Any found new line character increments the {@link #lineCt()}.
   *   The {@link #lineCt()} can be used to output the line number. 
   * <li>If {@link #OutTextPreparer(Appendable, int, int)} is used, an extra StringBuilder is created.
   *   then all {@link #append(CharSequence)} operations appends only to {@link #wr},
   *   buf if {@link #finishAppend()} is called, it writes out the content to {@link #wrDst},
   *   but prevents the content in the {@link #sb}. So it can be visited both in the file (after flush)
   *   and also in the {@link #sb}.
   *   And then the next any {@link #append(CharSequence)} cleans first the {@link #sb} for further operation.
   *   The flag {@link #bSbClean} controls the behavior.
   * </ul>
   */
  public static class WriteDst implements Appendable {
    
    /**The primary output if {@link #OutTextPreparer(Appendable, int, int)} is called, else null.
     * It can be a StringBuilder, then also {@link #sb} is set with the same instance reference.
     * Or it can be any other {@link Appendable}, especially {@link Writer} for file output.
     */
    private final Appendable wrDst;
    
    /**This is always the destination for all {@link #append(char)}, {@link #append(CharSequence)}, {@link #append(int)},
     * {@link #append(CharSequence, int, int)} and {@link #add(WriteDst)}.
     * It is either a StringBuilder if {@link #OutTextPreparer(Appendable, int, int)} is called,
     * or it is any other Appendable.
     */
    private final Appendable wr;
    
    /**This is set if the Appendable on construction, the last used output, is a {@link StringBuilder}. 
     * It allows more access possibilities for the user to change the generated text.
     * If the output Appendable is not a StringBulder this field remains null.
     * The user can use it immediately if the calling environment of the ctor delivers a {@link StringBuilder} as {@link Appendable}.
     * But note that changing content of this field does not effect the {@link #lineCt()}.
     * If the StringBuilder sb is changed, it is similar as the generated output text is changed afterwards,
     * independent of the functionality of the {@link OutTextPreparer} and in full responsibility to the user.
     */
    public final StringBuilder sb;
    
    /**Counts the '\n' inside appended texts. */
    private int lineCt;
    
    private final int lineStart;
    
    /**If true, then the next any {@link #append(char)} cleans first the {@link #wr} as StringBuilder
     * and sets this flag to false. 
     * This flag is set to true after {@link #finishAppend()}, after the content is written to {@link #wrDst}.
     * {@link #close()} also call {@link #finishAppend()} to write out content.
     * If this flag is true, the {@link #wr} contains the last written content
     */
    private boolean bSbClean;
    
    /**Constructs a new output destination to write the generated text.
     * @param wr may be also an instance of {@link StringBuilder}, then {@link #sb} is set also.
     * @param lineStart number of this first line. It is important for immediately instances.
     * The lineCt starts from 1 for the first line.
     */
    public WriteDst(Appendable wr, int lineStart) {
      assert(! (wr instanceof WriteDst));                  // prevent error using recursively
      this.wrDst = null;
      this.wr = wr;
      this.sb = wr instanceof StringBuilder ? (StringBuilder)wr: null;
      this.lineCt = lineStart;
      this.lineStart = lineStart;
    }

    /**Constructs a new output destination to write the generated text using a temporary used StringBuilder
     * for any {@link OutTextPreparer#exec(Appendable, DataTextPreparer) }.
     * @param wrDst may be also an instance of {@link StringBuilder}, then {@link #sb} is set also.
     * @param lineStart number of this first line. It is important for immediately instances.
     * The lineCt starts from 1 for the first line.
     */
    public WriteDst(Appendable wrDst, int lineStart, int sizeStringBuilder) {
      assert(! (wrDst instanceof WriteDst));                  // prevent error using recursively
      this.wrDst = wrDst;
      this.wr = new StringBuilder(sizeStringBuilder);
      this.sb = wrDst instanceof StringBuilder ? (StringBuilder)wrDst: null;
      this.lineCt = lineStart;
      this.lineStart = lineStart;
    }

    public void setLineCt (int lineCt) {
      this.lineCt = lineCt;
    }
    
    public int lineCt() { 
      return this.lineCt; 
    }
    
    
    
    /**Finishes one append phase. 
     * Only if this is constructed with {@link #OutTextPreparer(Appendable, int, int)}
     * and hence {@link #wr} is a StringBuilder: Writes out the content of {@link #wr} to {@link #wrDst}
     * but prevent the content in {@link #wr} (it is a StringBuilder) for debug view, else do nothing. 
     * Sets {@link #bSbClean} which forces clean {@link #sb} on the next any {@link #append(char)}.
     * This operation is also called on {@link #close()}. 
     * @throws IOException
     */
    public void finishAppend() throws IOException {
      if(!this.bSbClean && this.wrDst !=null && this.wr instanceof StringBuilder) {   // extra StringBuilder to view output text:
        this.wrDst.append((StringBuilder)this.wr);               // then transfer the output to the file, but let it readable for debug
        if(this.wrDst instanceof Flushable) {
          ((Flushable)this.wrDst).flush();             // interesting to see what's written in debug
        }
        this.bSbClean = true;
      }                                           
    }
    
    /**This operation can be used if a part of the output text is generated meanwhile with another WriteDst instance
     * and should be added now. The {@link #lineCt()} is also incremented by the 'other.lineCt'. 
     * @param other The {@link #sb} must be set, else assertion and exception.
     *   It means 'other' should be created (ctor) using a StringBuilder as Appendable.
     * @throws IOException
     */
    public void add(WriteDst other) throws IOException {
      if(this.bSbClean && this.wr instanceof StringBuilder) { ((StringBuilder)this.wr).setLength(0); this.bSbClean = false; } 
      assert(other.sb !=null);
      this.lineCt += (other.lineCt - other.lineStart);
      other.finishAppend();
      this.wr.append(other.sb);
    }
    
    
    /**Append operation similar as in {@link StringBuilder#append(int)}
     * @param val
     * @return
     * @throws IOException
     */
    public WriteDst append(int val) throws IOException {
      if(this.bSbClean && this.wr instanceof StringBuilder) { ((StringBuilder)this.wr).setLength(0); this.bSbClean = false; } 
      this.wr.append(Integer.toString(val));
      return this;
    }
    

    /**Standard append operation see {@link Appendable#append(CharSequence)} */
    @Override public WriteDst append ( CharSequence csq ) throws IOException {
      return append(csq, 0, csq.length());
    }

    /**Standard append operation see {@link Appendable#append(CharSequence, int, int)} */
    @Override public WriteDst append ( CharSequence csq, int start, int end ) throws IOException {
      if(this.bSbClean && this.wr instanceof StringBuilder) { ((StringBuilder)this.wr).setLength(0); this.bSbClean = false; } 
      for(int ix = start; ix < end; ++ix) {
        char cc = csq.charAt(ix);
        if(cc =='\n') { this.lineCt +=1; }
        this.wr.append(cc);
      }
      //this.wr.append(csq, start, end);  //TODO test calculation time.
      return this;
    }

    /**Standard append operation see {@link Appendable#append(char)} */
    @Override public WriteDst append ( char c ) throws IOException {
      if(this.bSbClean && this.wr instanceof StringBuilder) { ((StringBuilder)this.wr).setLength(0); this.bSbClean = false; } 
      if(c=='\n') { this.lineCt +=1; }
      this.wr.append(c);
      return this;
    }
    
    /**Close() if any of {@link #wrDst} or {@link #wr} is a {@link Closeable},
     * especially a File {@link Writer}
     *
     */
    public void close () throws IOException {
      finishAppend();
      if(this.wrDst !=null && this.wrDst instanceof Closeable) {
        ((Closeable)this.wrDst).close();
      }
      if(this.wr !=null && this.wr instanceof Closeable) {
        ((Closeable)this.wr).close();
      }
    }

    /**This should be used only for debug view. 
     * It outputs the {@link #lineCt()} and only the content if {@link #wr} is a {@link StringBuilder}
     */
    @Override public String toString() {
      return "lines: " + this.lineStart + " + " + (this.lineCt - this.lineStart +1) 
           + ( this.wr instanceof StringBuilder ? "\n" + this.wr 
             : this.sb == null ? "" : "\n" + this.sb);
    }
  }
  
  
  
  /**Instances of this class holds the data for one OutTextPreparer instance but maybe for all invocations.
   * An instance can be gotten from {@link OutTextPreparer#createArgumentDataObj()} proper and associated to the out text.
   * The constructor of this class is not public, it should be only gotten with this routine.
   * <br><br>
   * Invoke {@link #setArgument(String, Object)} to set any named argument. 
   *
   */
  public static class DataTextPreparer {
    
    /**The associated const data for OutText preparation used for {@link #setArgument(String, Object)} by name. */
    final OutTextPreparer prep;
    
    /**The instance where the &lt;:exec:operation(...)> are located. null if not necessary. */
    Object execObj;
    
    /**Array of all arguments. It is sorted to the {@link OutTextPreparer#nameVariables} with its value {@link DataAccess.IntegerIx}. 
     */
    final Object[] args;
    
    /**This is only to see the arguments for debug.*/
    Map<String, Object> argsByName;
    
    /**This is only to see the arguments for debug.*/
    final Map<String, DataAccess.IntegerIx> argsIxByName;
    
    /**Any &lt;call in the pattern get the data for the called OutTextPreparer, but only ones, reused. */
    DataTextPreparer[] argSub;
    
    /**Set on first usage. */
    CalculatorExpr.Data calcExprData;
    
    
    public String debugOtx;
    
    public int debugIxCmd;
    
    
    /**If this is not null, the line of the cmd is written into on certain cmds. It is for a log of execution.*/
    Appendable logExec;
    
    StringBuilder sbFormatted = new StringBuilder();
    
    /**Default use formatter for ENCLISH to be international. */
    Formatter formatter = new Formatter(this.sbFormatted, Locale.ENGLISH);
    
    boolean bChecks = true;
    
    /**Package private constructor invoked only in {@link OutTextPreparer#createArgumentDataObj()}*/
    DataTextPreparer(OutTextPreparer prep){
      if(prep.cmds.size()==0) {
        throw new IllegalStateException("OutTextPreparer is not initialized, new DataTextPreparer(...) only possible on initialized OutTextPreparer");
      }
      this.prep = prep;
      this.argsIxByName = prep.nameVariables;
      if(prep.nameVariables.size() >0) {
        this.args = new Object[prep.nameVariables.size()];
      } else {
        this.args = null;
      }
      if(prep.ctCall >0) {
        this.argSub = new DataTextPreparer[prep.ctCall];
      }
      
    }
    
    
    
    /**User routine to set a named argument with a value.
     * If a faulty name is used, an Exception is thrown 
     * @param name argument name of the argument list given in the constructor
     *   {@link OutTextPreparer#OutTextPreparer(String, Object, String, String)}, 3. argment
     *   or also given in the textual script.
     * @param value any value for this argument.
     * */
    public void setArgument(String name, Object value) {
      DataAccess.IntegerIx ix0 = this.prep.nameVariables.get(name);
      if(ix0 == null) 
        throw new IllegalArgumentException("OutTextPreparer script " + this.prep.sIdent + ", argument: " + name + " not existing: ");
      int ix = ix0.ix;
      this.args[ix] = value;
    }
    
  
    /**User routine to set a optional argument with a value.
     * This operation can be used if some text templates are possible, some have more arguments.
     * Then try to set all possible and not obligate arguments with this operation.
     * @param name argument name of the argument list given in the constructor
     *   {@link OutTextPreparer#OutTextPreparer(String, Object, String, String)}, 3. argment
     *   or also given in the textual script.
     * @param value any value for this argument.
     * @return true if set, false if the argument does not exists.
     * */
    public boolean setArgumentOptional(String name, Object value) {
      DataAccess.IntegerIx ix0 = this.prep.nameVariables.get(name);
      if(ix0 != null) {
        int ix = ix0.ix;
        this.args[ix] = value;
      }
      return ix0 !=null;
    }
    
  
    /**User routine to set a argument with an index in order of the argument list with a value.
     * @param ixArg index of the argument starting with 0 for the first argument in the list.
     * @param value any value for this argument.
     * */
    public void setArgument(int ixArg, Object value) {
      this.args[ixArg] = value;
    }
    
    
    /**Sets an output channel to output the line on certain cmds. It is for a log of execution.
     * @param log
     */
    public void setLogCmdline(Appendable log) {
      this.logExec = log;
    }
    
    /**Sets an instance due to the given 'execClass' for the script.
     * @param data The instance must be proper to the 'execClass' argument of the constructors
     *   respectively to the {@link OutTextPreparer#readTemplateCreatePreparer(InputStream, String, Class, Map, String).}
     *   If it does not match, an exception is thrown while processing a non static <code><:exec:operation(...)></code>
     */
    public void setExecObj(Object data) { this.execObj = data; }
  
    
    
    /**Sets a debug point. You have to set a breakpoint on usage position. It is only for internal debug.
     * @param patternName Because the debug info is copied to called pattern, the name of the called pattern is possible here.
     * @param ixCmd Use 0 to stop in the start cmd, see the commands with data debug, then decide.
     */
    public void setDebug(String patternName, int ixCmd) {this.debugOtx = patternName; this.debugIxCmd = ixCmd;} 
    
    /**Switches check of some stuff on or off, especially &lt;:type: check
     * @param bCheck true check on
     *   Hint for user: Set in anyway from a one time set boolean variable.    
     */
    public void setCheck(boolean bChecks) { this.bChecks = bChecks; }
    
  }

  enum ECmd{
    nothing('-', "nothing"), 
    addString('s', "str"),
    addVar('v', "var"),
    addLinenr('l', "linenr"),
    wr('W', "wr"),
    wrEnd('w', "wrEnd"),
    ifCtrl('I', "if"),
    elsifCtrl('J', "elsif"),
    elseCtrl('E', "else"),
    forCtrl('F', "for"),
    setVar('S', "set"),
    typeCheck('T', "type"),
    endLoop('B', "endloop"),
    call('C', "call"),
    exec('E', "exec"),
    debug('D', "debug"),
    pos('p', "pos")
    ;
    ECmd(char cmd, String sCmd){
      this.cmd = cmd; this.sCmd = sCmd;
    }
    char cmd;
    String sCmd;
    @Override public String toString() { return this.sCmd; }
  }
  
  
  


  
  
  static class Cmd extends CalculatorExpr.Operand{
    
    public final int ixCmd;
    
    public final int[] linecol;
    
    public final ECmd cmd;
    
    /**If necessary it is the offset skipped over the ctrl sequence. */
    public int offsEndCtrl;
    
    public Cmd(int ixCmd, int[] linecol, ECmd what, String textOrDatapath)
    { super( textOrDatapath);
      this.ixCmd = ixCmd;
      this.linecol = linecol;
      this.cmd = what;
    }
    
    
    public Cmd(int ixCmd, int[] linecol, ECmd what, Object value)
    { super( value);
      this.ixCmd = ixCmd;
      this.linecol = linecol;
      this.cmd = what;
    }
    
    
    /**A universal constructor for all which is not an Expression. 
     * @param what should be defined
     * @param ixValue -1 or index to arguments
     * @param dataAccess null or a data access path
     * @param dataConst null or a given object, maybe especially a {@link CalculatorExpr.Value}
     * @param textOrVar either the only one given String (valid) or a String info for debug.
     */
    public Cmd (int ixCmd, int[] linecol, ECmd what, int ixValue, DataAccess dataAccess, Object dataConst, String textOrVar) {
      super(ixValue, dataAccess, dataConst, textOrVar);
      this.ixCmd = ixCmd;
      this.linecol = linecol;
      this.cmd = what;
    }
    
    public Cmd ( int ixCmd, int[] linecol, OutTextPreparer outer, ECmd what, StringPartScanLineCol textOrDatapath, Class<?> reflData) throws Exception { 
      super( checkVariable(outer, textOrDatapath), outer.nameVariables, reflData, true);
      this.ixCmd = ixCmd;
      this.linecol = linecol;
      this.cmd = what;
    }
    
    public Cmd ( int ixCmd, int[] linecol, OutTextPreparer outer, ECmd what, String textOrDatapath, Class<?> reflData, Map<String, Object> idxConstData) throws Exception { 
      super( textOrDatapath, outer.nameVariables, reflData, idxConstData);
      this.ixCmd = ixCmd;
      this.linecol = linecol;
      this.cmd = what;
    }
    
    
    public Cmd ( int ixCmd, int[] linecol, OutTextPreparer outer, ECmd what, StringPartScanLineCol textOrDatapath, Class<?> reflData, Map<String, Object> idxConstData) throws Exception { 
      super( textOrDatapath, outer.nameVariables, reflData, idxConstData, true);
      this.ixCmd = ixCmd;
      this.linecol = linecol;
      this.cmd = what;
    }
    
    
    /**Maybe yet experience
     * @param outer
     * @param textOrDatapath
     * @return same 'textOrDatapath'
     */
    private static StringPartScanLineCol checkVariable(OutTextPreparer outer, StringPartScanLineCol textOrDatapath) {
//      if(outer.listArgs == null) { //variables are not given before parse String
//        //char c0 = textOr
//      }
      return textOrDatapath;
    }
    
    
    @Override public String toString() {
      return this.linecol[0] + "," + this.linecol[1] + " " + this.cmd + ":" + this.textOrVar;
    }
    
  }//sub class Cmd
  
  
  
  static class ForCmd extends Cmd {
    
    /**The index where the entry value is stored while executing. 
     * Determined in ctor ({@link OutTextPreparer#parse(String, Object)} */
    public int ixEntryVar, ixEntryVarNext, ixEntryKey;
    
    int ixStart = -1;

    public ForCmd ( int ixCmd, int[] linecol, OutTextPreparer outer, StringPartScanLineCol spDatapath, Class<?> reflData) throws Exception {
      super(ixCmd, linecol, outer, ECmd.forCtrl, spDatapath, reflData);
    }

    public ForCmd ( int ixCmd, int[] linecol,OutTextPreparer outer, String sDatapath, Class<?> reflData) throws Exception {
      super(ixCmd, linecol, outer, ECmd.forCtrl, sDatapath, reflData, null);
    }
  }
  
  
  /**Helper class as container to produce indices for <:for:ix:[0..3]>
   */
  static class ForIxContainer {
    int ixStart; int ixEnd;
    ForIxContainer(int ixStart, int ixEnd) {
      this.ixStart = ixStart; this.ixEnd = ixEnd;
    }
  }
  
  
  
  static class SetCmd extends Cmd {
    
    /**The index where the entry value is stored while executing. 
     * Determined in ctor ({@link OutTextPreparer#parse(String, Object)} */
    public int ixVariable;

    public SetCmd ( int ixCmd, int[] linecol, OutTextPreparer outer, StringPartScanLineCol spDatapath, Class<?> reflData) throws Exception {
      super(ixCmd, linecol, outer, ECmd.setVar, spDatapath, reflData);
    }

    public SetCmd ( int ixCmd, int[] linecol, OutTextPreparer outer, String sDatapath, Class<?> reflData) throws Exception {
      super(ixCmd, linecol, outer, ECmd.setVar, sDatapath, reflData, null);
    }
  }
  
  
  
  /**&lt;:wr:...>....&lt;.wr> writes to a special {@link Appendable} given as DataAccess. */
  static class WrCmd extends Cmd {
    
    /**The offset to the next &lt;:elsif or the following &lt;:else or the following &lt;.if*/
    //int offsWrEnd;
    
    /**Index of the write buffer to use in the data. */
    int ixDataWr;
    
    
//    public WrCmd(OutTextPreparer outer, StringPartScanLineCol sDatapath, Class<?> reflData) throws Exception {
//      super(outer, ECmd.wr, sDatapath, reflData);
//    }

    
    public WrCmd ( int ixCmd, int[] linecol, OutTextPreparer outer, String sDatapath, Class<?> reflData) throws Exception {
      super(ixCmd, linecol, outer, ECmd.wr, sDatapath, reflData, null);
    }
  }
  
  

  
  
  
  static class IfCmd extends Cmd {
    
    /**The offset to the next &lt;:elsif or the following &lt;:else or the following &lt;.if*/
    int offsElsif;
    
    /**The expression for a comparison. It is null if only a null check of the first argument should be done.
     * For comparison with a constant text, the constant text is contained in expr */
    //CalculatorExpr expr;
    
    /**One of character from "=!enc" for equal, non equal, starts, ends, contains or '\0' for non compare. */
    //char cmp;
    
    /**The value to compare with. 
     * @throws Exception */
    //CalculatorExpr.Operand valCmp;
    
    public IfCmd ( int ixCmd, int[] linecol, OutTextPreparer outer, ECmd cmd, StringPartScanLineCol sDatapath, Class<?> reflData) throws Exception {
      super(ixCmd, linecol, outer, cmd, sDatapath, reflData);
    }

    
    public IfCmd ( int ixCmd, int[] linecol, OutTextPreparer outer, ECmd cmd, String sDatapath, Class<?> reflData) throws Exception {
      super(ixCmd, linecol, outer, cmd, sDatapath, reflData, null);
    }
  }
  
  
  static class CallCmd extends Cmd {
    
    /**Index for {@link DataTextPreparer#argSub} to get already existing data container. */
    public int ixDataArg;
    
    /**The data to get actual arguments for this call. */
    public List<Argument> args;
    
    public CallCmd ( int ixCmd, int[] linecol, OutTextPreparer otxSub) {
      super(ixCmd, linecol, ECmd.call, otxSub);
    }
    
//    public CallCmd(OutTextPreparer outer, StringPartScanLineCol spDatapath, Class<?> reflData) throws Exception 
//    { super(outer, ECmd.call, spDatapath, reflData, outer.idxConstData); 
//    }
// 
//    public CallCmd(OutTextPreparer outer, String sDatapath, Class<?> reflData) throws Exception { 
//      super(outer, ECmd.call, sDatapath, reflData, outer.idxConstData); 
//    }                                              // search the call object also in callScripts
  }
  
  
  
  static class DebugCmd extends Cmd {
    public String cmpString;

    public DebugCmd ( int ixCmd, int[] linecol, OutTextPreparer outer, StringPartScanLineCol spDatapath, Class<?> reflData) throws Exception {
      super(ixCmd, linecol, outer, ECmd.debug, spDatapath, reflData);
    }

    public DebugCmd ( int ixCmd, int[] linecol, OutTextPreparer outer, String sDatapath, Class<?> reflData, final Map<String, Object> idxConstData) throws Exception {
      super(ixCmd, linecol, outer, ECmd.debug, sDatapath, reflData, idxConstData);
    }
  }
  
  
  
  /**A cmd for a value can contain a format string.
   */
  static class ValueCmd extends Cmd {
    final String sFormat;
    
    ValueCmd ( int ixCmd, int[] linecol, OutTextPreparer outer, String textOrDatapath, String sFormat
        , Class<?> reflData, Map<String, Object> idxConstData) throws Exception {
      super(ixCmd, linecol, outer, ECmd.addVar, textOrDatapath, reflData, idxConstData);
      this.sFormat = sFormat;
    }
  }

  
  
  /**A cmd for a type check of an instance.
   */
  static class TypeCmd extends Cmd {
    /**The expected type. */
    Class<?> type;
    
    TypeCmd ( int ixCmd, int[] linecol, OutTextPreparer outer, String textOrDatapath, Class<?> type
        , Class<?> reflData, Map<String, Object> idxConstData) throws Exception {
      super(ixCmd, linecol, outer, ECmd.typeCheck, textOrDatapath, reflData, idxConstData);
      this.type = type;
    }
  }

  
  
  static class CmdString extends Cmd {
    public CmdString ( int ixCmd, int[] linecol, String str)
    { super(ixCmd, linecol, ECmd.addString, str);
    }

  };
//  
//  
//  
//  static class CmdVar extends Cmd {
//    String sVar;
//    public CmdVar(String str)
//    { super(str, 0,0);
//      sVar = str;
//    }
//
//    public void exec() {
//      
//      fm.add(str.substring(pos0, pos1));
//    }
//  };
  
  
  static class Argument extends CalculatorExpr.Operand {

    /**Name of the argument, null for usage in {@link Cmd} */
    public final String name;
    
    /**The Index to store the value in {@link DataTextPreparer#args} of the called level or -1 if not known. */
    public final int ixDst;
    
    public Argument(OutTextPreparer outer, String name, int ixCalledArg, String sTextOrDatapath, Class<?> reflData, final Map<String, Object> idxConstData) throws Exception {
      super(sTextOrDatapath, outer.nameVariables, reflData, idxConstData);
      if(name.equals("xxxsuperType")) Debugutil.stopp();
      this.name = name;
      this.ixDst = ixCalledArg;
    }
    
    public Argument(String name, int ixCalledArg, Object dataConst, DataAccess datapath, String sText) throws Exception {
      super(-1, datapath, dataConst, sText);
      if(name.equals("xxxsuperType")) Debugutil.stopp();
      this.name = name;
      this.ixDst = ixCalledArg;
    }
    
  }
  
  
  /**All argument variables and internal variables sorted with the index to {@link DataTextPreparer#args} as index. */
  protected Map<String, DataAccess.IntegerIx> nameVariables = new TreeMap<String, DataAccess.IntegerIx>();
  
  
  /**Filled after {@link #parse(Class, Map, Map)} from all variable name from {@link #nameVariables}
   * 
   */
  private String[] nameVariablesByIx;
  
  
  /**Index of the OUT element in variables */
  int ixOUT;
  
  /**Container with {@link OutTextPreparer} instances which can be called as sub pattern. 
   * Usual they are the patterns read from one textual file with several scripts (or a few files).
   * One patter starts with &lt;:otx:NAME:ARGS&gt; and ends with &lt;.otx&gt; in a given text.
   * Note that also #idxConstData can contain some {@link OutTextPreparer} scripts to call, usual given immediately as Java instances.
   * @since 2023-10-22. Before the scripts are only contain in {@link #idxConstData}. 
   *   There were a contradiction between meaning of the idxConstData and the Map of scripts.
   *   In the past first call-able scripts were created in Java code, and stored in the #idxConstData.
   *   Later more using textual scripts have stored there script parts firstly also in #idxConstData.  
   */
  //protected final Map<String, OutTextPreparer> idxScript;
  
  /**The source of the generation script, argument of {@link #parse(Class, String)} only for debug. */
  public final String pattern;
  
  /**If not null then this is the start String of an line for output,
   * set if "NEWLINE===" is given in arguments.
   * 
   */
  String sLineoutStart;
  
  int nLineoutStart;
  
  int nrLineStartInFile;
  
  /**If this field is >=0 (usual >0), then the value in {@link DataTextPreparer#args} on this index  
   * should contain the String to produce a newline. 
   * This should be given on user level in {@link DataTextPreparer#setArgument(String, Object)} 
   * with the arg1 as 'nameNewlineVariable' given with 'NEWLINE====<$?nameNewlineVariable>' 
   * The name of this 'nameNewlineVariable' is stored in {@link #nameVariables} to get this index
   * and in {@link #nameVariablesByIx} on the position of this index to get the name.
   * 
   */
  int ixVarLineoutStart = -1;
  
  /**If this field is !=null then it is the name of the argument variable for the newline String. 
   * This should be given on user level in {@link DataTextPreparer#setArgument(String, Object)} 
   * with the arg1 as 'nameNewlineVariable' given with 'NEWLINE====<$?nameNewlineVariable>' 
   * The name of this 'nameNewlineVariable' is stored in {@link #nameVariables} to get this index
   * and in {@link #nameVariablesByIx} on the position of this index to get the name.
   * 
   */
  String nameVarLineoutStart;
  
  /**This is only stored as info, not used in this class.
   * It is the argument execClass from the user, 
   * which is immediately evaluated in the {@link #parse(Class, String)} operations while construction.
   */
  //@SuppressWarnings("unused") private final Class<?> execClass;
  
  
  int ctCall = 0;
  
  List<Cmd> cmds = new ArrayList<Cmd>();
  
  
  /**Name of the generation script used for debug and comparison with data. */
  public final String sIdent;
  
  /**Constructs completely the text generation control instance for the specific pattern and given sub pattern.
   * This is one of the preferred ctor if the scripts are all given in Java code.
   * &lt;:call..> scripts can be contained as member of execClass as static instantiation.
   * @param ident
   * @param execClass
   * @param pattern
   * @throws IllegalArgumentException if the pattern is faulty.
   *   This exception do not need to be caught immediately on the calling level, do not spend effort.
   *   Because the pattern should be given error-free, it is hard programmed.
   *   If the pattern has an error then the application throws on higher level if the calling level class is instantiated,
   *   and should be fixed in programming.
   *   Note: On some faulty pattern the prepared cmd for output contains the error message. 
   */
  public OutTextPreparer(String ident, Class<?> execClass, String pattern) {
    this.sIdent = ident;
    this.pattern = pattern;
    try {
      this.parse(execClass, null, null);
    } catch(ParseException exc) {
      throw new IllegalArgumentException(exc);
    }
  }
  
  /**Constructs completely the text generation control instance for the specific pattern.
   * This is preferred for a simple script given in Java code.
   * The variables are gotten from the pattern. For this case the order of variable depends on the order in the pattern.
   * @param ident name of otx
   * @param pattern Only a simple pattern without &lt;:call...> or &lt;:exec...>, the ctor parses it.
   * @throws IllegalArgumentException if the pattern is faulty.
   *   This exception do not need to be caught immediately on the calling level, do not spend effort.
   *   Because the pattern should be given error-free, it is hard programmed.
   *   If the pattern has an error then the application throws on higher level if the calling level class is instantiated,
   *   and should be fixed in programming.
   *   Note: On some faulty pattern the prepared cmd for output contains the error message. 
   */
  public OutTextPreparer(String ident, String pattern) {
    this.sIdent = ident;
    this.pattern = pattern;
    try {
      this.parse(null, null, null);
    } catch(ParseException exc) {
      throw new IllegalArgumentException(exc);
    }
  }
  
  /**Constructs completely the text generation control instance for the specific pattern and given sub pattern.
   * This is one of the preferred ctor if the scripts are all given in Java code.
   * &lt;:call..> scripts can be contained in #idxConstData for dynamic instantiation 
   * or also as member of execClass as static instantiation.
   * @param ident Any identification not used for the generated text.
   * @param execClass If the access via reflection should be done, null possible
   * @param variables One variable or list of identifier separated with comma, whiteSpaces possible. 
   * @param pattern The pattern in string given form. 
   *   This pattern will be parsed and divided in parts for a fast text generation.
   * @throws IllegalArgumentException if the pattern is faulty.
   *   This exception do not need to be caught immediately on the calling level, do not spend effort.
   *   Because the pattern should be given error-free, it is hard programmed.
   *   If the pattern has an error then the application throws on higher level if the calling level class is instantiated,
   *   and should be fixed in programming.
   *   Note: On some faulty pattern the prepared cmd for output contains the error message. 
   */
  public OutTextPreparer(String ident, Class<?> execClass, String variables, String pattern) {
    this(ident, execClass, variables, pattern, null);
  }

  
  
  /**Creates the text generation control data for the specific pattern and given sub pattern.
   * This is a ctor if the scripts are read from a textual file,
   * see also {@link #readTemplateCreatePreparer(InputStream, Class, Map)}
   * @param ident Name of the sub pattern, used for error outputs.
   * @param execClass If the access via reflection should be done, null possible
   * @param variables One variable or list of identifier separated with comma, whiteSpaces possible. 
   * @param pattern The pattern in string given form. 
   *   This pattern will be parsed and divided in parts for a fast text generation.
   * @param idxConstData Container for call able scripts, maybe contain all scripts, also for more const data
   * @throws ParseException 
   * @throws IllegalArgumentException if the pattern is faulty.
   *   This exception do not need to be caught immediately on the calling level, do not spend effort.
   *   Because the pattern should be given error-free, it is hard programmed.
   *   If the pattern has an error then the application throws on higher level if the calling level class is instantiated,
   *   and should be fixed in programming.
   *   Note: On some faulty pattern the prepared cmd for output contains the error message. 
   */
  @Deprecated private OutTextPreparer(String ident, Class<?> execClass, String variables, String pattern
      , Map<String, Object> idxConstData, Map<String, OutTextPreparer> idxScript) {
    this.sIdent = ident.trim();
    this.pattern = pattern;
    this.parseVariables(variables);
    try {
      this.parse(execClass, idxConstData, idxScript);
    } catch(ParseException exc) {
      throw new IllegalArgumentException(exc);
    }
  }
  
  
  /**Creates the text generation control data for the specific pattern and given sub pattern
   * read from a file or other input stream, without parsing.
   * <br>
   * This is the preferred ctor if the scripts are read from a textual file with two-phase translation,
   * see {@link #readTemplateCreatePreparer(InputStream, String, Map)}.
   * <b>Afterwards {@link #parse(Class, Map)} have to be called</b> as second phase for translate the scripts.
   * <br><br>
   * Using this, the order of sub scripts do not need to be regarded. 
   * Another script can be called in any order, in its pattern, because it can be found, though it's not translated yet.
   * TODO description One of the definitions is instantiated with this ctor and should be stored in the idxScripts.
   * See also {@link #readTemplateCreatePreparer(InputStream, String, Map)}, the principle is used there.
   * @param ident Name of the sub pattern, used for error outputs.
   * @param variables One variable or list of identifier separated with comma, whiteSpaces possible. 
   * @param pattern The pattern in string given form. 
   *   This pattern will be parsed and divided in parts for a fast text generation.
   * @param nrLineStartInFile line number where the pattern starts in a file, maybe 0.
   * @throws never. 
   */
  public OutTextPreparer(String ident, String variables, String pattern, int nrLineStartInFile ) {
    this.sIdent = ident.trim();
    this.pattern = pattern;
    this.nrLineStartInFile = nrLineStartInFile;
    this.parseVariables(variables);
  }

  
  
  /**Creates the text generation control data for the specific pattern and given sub pattern
   * without parsing.
   * <br>
   * This is the preferred ctor if the scripts is given immediately as String (hard coded scripts in Java),
   * and two-phase translation should be used.
   * <b>Afterwards {@link #parse(Class, Map)} have to be called</b> as second phase for translate the scripts.
   * <br><br>
   * Using this, the order of sub scripts do not need to be regarded. 
   * Another script can be called in any order, in its pattern, because it can be found, though it's not translated yet.
   * TODO description One of the definitions is instantiated with this ctor and should be stored in the idxScripts.
   * See also {@link #readTemplateCreatePreparer(InputStream, String, Map)}, the principle is used there.
   * <br><br>Example:<pre>
    private OutTextPreparer otxNode = new OutTextPreparer("exampleNode", "arg", 
      "pattern of the script" 
    + "<:if:arg><&arg>... in more lines <.if>"
    );
    //.....
    Map<String, OutTextPreparer> idxScript = new TreeMap<>();
    idxScript.put(this.otxNode.sIdent, this.otxNode);
    idxScript.put(this.otxCfgHead.sIdent, this.otxCfgHead);
    try {
      OutTextPreparer.parseTemplates(idxScript, this.getClass(), null, log);
      ,,,,
    </pre>
   * @param ident Name of the sub pattern, used for error outputs.
   * @param variables One variable or list of identifier separated with comma, whiteSpaces possible. 
   * @param pattern The pattern in string given form. 
   *   This pattern will be parsed and divided in parts for a fast text generation.
   * @throws never. 
   */
  public OutTextPreparer(String ident, String variables, String pattern ) {
    this(ident, variables, pattern, 0);
  }
  
  /**Creates the text generation control data for the specific pattern and given sub pattern
   * without parsing.
   * This is the preferred ctor if the scripts are read from a textual file with two-phase translation.
   * Using this sub scripts can be used before definiton. 
   * There definitions are instantiated with this ctor.
   * After this ctor {@link #parse(Class, Map)} have to be called as second phase for translate the scripts
   * which can all use the definitons. 
   * See also {@link #readTemplateCreatePreparer(InputStream, Class, Map)}, the principle is used there.
   * @param ident Name of the sub pattern, used for error outputs.
   * @param variables List of variable. 
   * @param pattern The pattern in string given form. 
   *   This pattern will be parsed and divided in parts for a fast text generation.
   * @param idxConstData Container for call able scripts, maybe contain all scripts, also for more const data
   * @throws never. 
   */
  public OutTextPreparer(String ident
      , List<String> variables, String pattern ) {
    this.sIdent = ident.trim();
    this.pattern = pattern;
    this.setVariables(variables);
  }
  
  
  
  
  
  
  /**Constructs completely the text generation control instance for the specific pattern and given sub pattern.
   * This is one of the preferred ctor if the scripts are all given in Java code.
   * &lt;:call..> scripts can be contained in #idxConstData for dynamic instantiation 
   * or also as member of execClass as static instantiation.
   * <br><br>
   * A constructor is called as: (example):<pre>
   * static OutTextPreparer otxMyText = new OutTextPreparer (
   *   "otxMyText"                   // The name of the OutText usable for call
   *   , UserClass.class             // A class which's static operations and data can be used 
   *   , "arg1, arg2",               // Name of arguments, see text below
   *    "A simple text with newline: \n"  // The output text pattern
   *  + "<&arg1>: a variable value"
   *  + "<&arg1.operation(arg2)>: Invocation of any operation in arguments resulting in an text"  
   *  + "<:call:otxSubText:arg1:arg2:...>: call of any sub text maybe with args"
   *  + "<:exec:operation(arg1, arg2, ....)> : call of any static operation in the given reflection class maybe with args"
   *  ;</pre>
   * @param ident Any identification not used for the generated text.
   * @param execClass If the access via reflection should be done, null possible
   * @param variables One variable or list of identifier separated with comma, whiteSpaces possible. 
   * @param pattern The pattern in string given form. 
   *   This pattern will be parsed and divided in parts for a fast text generation.
   * @param idxConstData Container for call able scripts, maybe contain all scripts, also for more const data
   * @throws ParseException 
   * @throws IllegalArgumentException if the pattern is faulty.
   *   This exception do not need to be caught immediately on the calling level, do not spend effort.
   *   Because the pattern should be given error-free, it is hard programmed.
   *   If the pattern has an error then the application throws on higher level if the calling level class is instantiated,
   *   and should be fixed in programming.
   *   Note: On some faulty pattern the prepared cmd for output contains the error message. 
   */
  public OutTextPreparer(String ident, Class<?> execClass
      , String variables, String pattern
      , Map<String, Object> idxConstData) {
    this.sIdent = ident.trim();
    this.pattern = pattern;
    this.parseVariables(variables);
    try {
      this.parse(execClass, idxConstData, null);
    } catch(ParseException exc) {
      throw new IllegalArgumentException(exc);
    }
  }

  
  
  /**Constructs completely the text generation control instance for the specific pattern and given sub pattern.
   * This is one of the preferred ctor if the scripts are all given in Java code.
   * &lt;:call..> scripts can be defined as member of execClass as static instantiation.
   * @param variables Identifier given as list, parsing is not necessary. 
   *        Able to use if the variable idents are anyway given in a list.
   * @throws IllegalArgumentException if the pattern is faulty.
   *   This exception do not need to be caught immediately on the calling level, do not spend effort.
   *   Because the pattern should be given error-free, it is hard programmed.
   *   If the pattern has an error then the application throws on higher level if the calling level class is instantiated,
   *   and should be fixed in programming.
   *   Note: On some faulty pattern the prepared cmd for output contains the error message. 
   * 
   */
  public OutTextPreparer(String ident, Class<?> execClass, List<String> variables, String pattern) {
    this.sIdent = ident;
    this.pattern = pattern;
    this.setVariables(variables);
    try {
      this.parse(execClass, null, null);
    } catch(ParseException exc) {
      throw new IllegalArgumentException(exc);
    }
  }
  
  
  
  /**Recommended operation to read one template script and create all {@link OutTextPreparer} instances but does not parse.
   * To parse all read scripts call {@link #parseTemplates(Map, Class, Map, LogMessage)} afterwards.
   * This assures that all sub scripts in all templates can be &lt;:call...> independent of the definition order.
   *  
   * @since 2024-02-13 opens and closes also the file in jar.
   * @since 2025-05-19 now it can also read the script only in one line '<:otx:...>...<.otx>'
   *
   * @param classInJar null or a Class in the jar as start point of path
   * @param path if classInJar is given, the relative path from classInJar to read a file from jar using {@link Class#getResourceAsStream(String)}.
   *   If this file is not available, a FileNotFoundException is thrown: Note: {@link Class#getResourceAsStream(String)} does not throw by itself.
   *   If classInJar==null this is a path in the file system to use this file. Both variants are supported.
   * @param idxScript container where the read scripts are stored, sorted by its name <:otx:NAME:...>
   *   The scripts are created, the text is referred via {@link OutTextPreparer#pattern} but the script is not parsed yet.
   *   Later parsing allows that a script can be &lt;:call:...> which is defined after the calling script. 
   * @param dataRoot null admissible, possibility to set values from there in <code>&lt;:set:name=&value></code>
   * @param idxConstData necessary if <code>&lt;:set:name=value></code> is used. 
   *   null admissible if the inp does not contain <code>&lt;:set:name=&value></code> outside of scripts
   * @throws IOException general possible on reading inp
   * @throws ParseException if the inp has syntax errors
   */
  public static void readTemplateCreatePreparer 
  ( Class<?> classInJar, String path
  , Map<String, OutTextPreparer> idxScript
  , Object dataRoot, Map<String, Object> idxConstData
  ) throws IOException, ParseException {
    final InputStream inp;
    if(classInJar == null) {
      inp = new FileInputStream(FileFunctions.newFile(path));
    } else {
      inp = classInJar.getResourceAsStream(path);          //pathInJar with slash: from root.
      if(inp == null) {                                    // null can occur. 
        throw new FileNotFoundException("resource in jar not found: " + path + " beside " + classInJar.getTypeName());
      }
    }
    readStreamTemplateCreatePreparer(inp, null, idxScript, dataRoot, idxConstData);
    inp.close();
  }

  /**Recommended operation to read one template script and create all {@link OutTextPreparer} instances but does not parse.
   * To parse all read scripts call {@link #parseTemplates(Map, Class, Map, LogMessage)} afterwards.
   * This assures that all sub scripts in all templates can be &lt;:call...> independent of the definition order.
   * @since 2025-05-19 now it can also read the script only in one line '<:otx:...>...<.otx>'
   *  
   * @param inp opened input stream, maybe also a file in jar, see {@link #readTemplateCreatePreparer(Class, String, Map, Object, Map)}
   * @param lineStart
   * @param idxScript container where the read scripts are stored, sorted by its name <:otx:NAME:...>
   *   The scripts are created, the text is referred via {@link OutTextPreparer#pattern} but the script is not parsed yet.
   *   Later parsing allows that a script can be &lt;:call:...> which is defined after the calling script. 
   * @param dataRoot null admissible, possibility to set values from there in <code>&lt;:set:name=&value></code>
   * @param idxConstData necessary if <code>&lt;:set:name=value></code> is used. 
   *   null admissible if the inp does not contain <code>&lt;:set:name=&value></code> outside of scripts
   * @throws IOException general possible on reading inp
   * @throws ParseException on error on a '<:set:... ' line or also formal error on a '<:otx:...' line.
   *   It does not parse the script here, inner parse errors in the script are not detected here, 
   *   see {@link #parseTemplates(Map, Class, Map, LogMessage)}
   */
  public static void readStreamTemplateCreatePreparer 
  ( InputStream inp, String lineStart
  , Map<String, OutTextPreparer> idxScript
  , Object dataRoot, Map<String, Object> idxConstData
  ) throws IOException, ParseException {
    readStreamTemplateCreatePreparer(idxScript, null, inp, lineStart, dataRoot, idxConstData);
  }
  
  
  
  /**Reads a given template which may contain the pattern for some associated OutTextPreparer srcipts (parts of the template).
   * It does not parse, only detect the scripts as parts and const variable associations.
   * @since 2023-05. See also {@link #readTemplateCreatePreparer(InputStream, String, Class)}
   * <br>
   * The file can contain different patterns in segments: <pre>
   * <:otx: patternName : variable : var2 >  ##comment
   * content with <&ariables>       ##comment
   * more lines<.otx>
   * 
   * free text between pattern
   * 
   * <:otx: nextPatternName ...
   *   etc.
   * </pre>
   * @param inp
   * @param lineStart null for newer versions if all scripts starts with <:otx:, elsewhere the designation of a new script.
   *   Then the arguments should be written after the lineStart string in "(arg, ...)" 
   * @param dataRoot null or can be used for access <:set:name=&element> inside dataRoot, the current value in element will be stored with name.
   * @param idxConstData necessary as destination if the script contains <:set:name=value>, elsewhere can be null
   * @return list of all read scripts as part of inp. The scripts starts with <:otx: or with the line after lineStart.
   * @throws IOException
   * @throws ParseException
   * @since 2024-01-15 more arguments dataRoot, idxConstData, for compatibility provide null for both.
   * @deprecated, use {@link #readStreamTemplateCreatePreparer(Map, List, InputStream, String, Object, Map)} with given idxScript argument
   */
  public static List<String> readTemplateList ( InputStream inp, String lineStart, Object dataRoot, Map<String, Object> idxConstData) throws IOException, ParseException {
    List<String> ret = new LinkedList<String>();
    readStreamTemplateCreatePreparer(null, ret, inp, lineStart, dataRoot, idxConstData);
    return ret;
  }
  
  
  /**Inner operation only separated because oder approach with ret argument.
   * Reads a given template which may contain the pattern for some associated OutTextPreparer srcipts (parts of the template).
   * It does not parse, only detect the scripts as parts and const variable associations.
   * @since 2023-05. See also {@link #readTemplateCreatePreparer(InputStream, String, Class)}
   * <br>
   * The file can contain different patterns in segments: <pre>
   * <:otx: patternName : variable : var2 >  ##comment
   * content with <&ariables>       ##comment
   * more lines<.otx>
   * 
   * free text between pattern
   * 
   * <:otx: nextPatternName ...
   *   etc.
   * </pre>
   * @param inp
   * @param lineStart null for newer versions if all scripts starts with <:otx:, elsewhere the designation of a new script.
   *   Then the arguments should be written after the lineStart string in "(arg, ...)" 
   * @param dataRoot null or can be used for access <:set:name=&element> inside dataRoot, the current value in element will be stored with name.
   * @param idxConstData necessary as destination if the script contains <:set:name=value>, elsewhere can be null
   * @return list of all read scripts as part of inp. The scripts starts with <:otx: or with the line after lineStart.
   * @throws IOException
   * @throws ParseException
   * @since 2024-01-15 more arguments dataRoot, idxConstData, for compatibility provide null for both.
   */
  private static void readStreamTemplateCreatePreparer ( Map<String, OutTextPreparer> idxScript, List<String> ret
    , InputStream inp, String lineStart, Object dataRoot, Map<String, Object> idxConstData
  ) throws IOException, ParseException {
    InputStreamReader reader = new InputStreamReader(inp, "UTF-8");
    BufferedReader rd = new BufferedReader(reader);
    String line;
    int nrline =0;
    StringBuilder buf = new StringBuilder(500);
    boolean bActiveScript = false;
    String name = null;
    String args = null;
    while( (line = rd.readLine()) !=null) {
      nrline +=1;
      int posComment = line.indexOf("##");
      if(posComment >=0) {
        while(posComment >0 && line.charAt(posComment-1) == ' ') { posComment -=1; }  // backward to fast non space
        line = line.substring(0, posComment);              // posComment is the position of the first space before comment or of ##comment
      }
      if(posComment == 0) {                                // ignore a line which starts with ##comment, do not produce an empty line.
      }
      if(!bActiveScript && line.startsWith("<:set:")) {           // a new sub template starts here 
        int posSep = line.indexOf('=');
        if(posSep <0) { 
          throw new ParseException("<:set:...= syntax error, missing \"=\"; line: " + line, nrline); }
        String key = line.substring(6, posSep).trim();
        int posEnd = StringFunctions.indexOutsideQuotation(line, posSep, -1, '>', "\"", "\"", '\\');  //search > outside quotation and transcription
        if(posEnd <0) {
          throw new ParseException("<:set:...> syntax error, missing \">\"; line: " + line, nrline); }
        posSep = StringFunctions.indexNoWhitespace(line, posSep+1, -1);
        final char cc = line.charAt(posSep);
        final Object data;
        if(cc == '&') { //----------------------------------- access with DataPath
          String sDatapath = line.substring(posSep+1, posEnd);
          try {
            data = DataAccess.access(sDatapath, dataRoot, true, false, false, null);
          } catch (Exception e) {
            throw new ParseException("<:set:...=datapath faulty: " + e.getMessage(), nrline);
          }
        } else if(cc == '\"' && line.charAt(posEnd-1) == '\"') {
          data = StringFunctions.convertTransliteration(line.substring(posSep +1, posEnd -1), '\\').toString(); // text inside "text\n\>" with transliteration
        } else {
          data = line.substring(posSep, posEnd);
        }
        idxConstData.put(key, data);
      }
      else if(!bActiveScript && line.startsWith("<:gTxt:")) {           // a new sub template starts here 
        int posName = 7;
        int posArgs=line.indexOf(':', 7);
        int posEndArgs = line.indexOf('>');
        name = line.substring(posName, posArgs).trim();
        //if(name.equals("castValue_FD")) Debugutil.stopp();
        args = line.substring(posArgs+1, posEndArgs);
        line = line.substring(posEndArgs+1).trim();
        if(line.length()==0) {                             // for compatibility: do not write a '\n' if the first line does not contain text.
          line = null;                                     // do not write an \n on start
        }
        bActiveScript = true;                              // switch state to "in the script" 
      }
      else if(!bActiveScript && line.startsWith("<:otx:")) {           // a new sub template starts here 
        int posName = 6;
        int posArgs=line.indexOf(':', 6);
        int posEndArgs = line.indexOf('>');
        name = line.substring(posName, posArgs).trim();
        //if(name.equals("castValue_FD")) Debugutil.stopp();
        args = line.substring(posArgs+1, posEndArgs);
        line = line.substring(posEndArgs+1).trim();
        if(line.length()==0) {                             // for compatibility: do not write a '\n' if the first line does not contain text.
          line = null;                                     // do not write an \n on start
        }
        bActiveScript = true;                              // switch state to "in the script" 
      }
      else if(lineStart !=null && !bActiveScript && line.startsWith(lineStart)) {           // a new sub template starts here 
        buf.append(line.substring(lineStart.length())).append('\n');  // first line with args starting with "("
        bActiveScript = true;
      }
      else {
        // ignore lines outside active script.
      }
      if(bActiveScript && line !=null) {                   //--------vv from the second line of a script
        int posEndScript = line.indexOf("<.gTxt>");
        int posEndScriptOld = line.indexOf("<.otx>");
        if(posEndScriptOld >=0 && (posEndScript < 0 || posEndScript > posEndScriptOld)) {
          posEndScript = posEndScriptOld;     // <.otx> found.
        } else if(posEndScript >=0){
          Debugutil.stop();   // use the new variant.
        }
        if(posEndScript >=0) {
          buf.append(line.substring(0, posEndScript));
          if(ret !=null) {
            ret.add(buf.toString());                       // old compatible variant.
          } else {
            OutTextPreparer otxScript = idxScript.get(name);
            if(otxScript !=null) {
              Debugutil.stopp();
              throw new ParseException("script is already existing, it is twice: otx: " + name, nrline);
            }
            String script = buf.toString();
            otxScript = new OutTextPreparer(name, args, script, 0);  // does not parse, only stores the script.
            idxScript.put(name, otxScript);                // new variant, store the script sorted by name.
            name = null;
            args = null;
          }
          buf.setLength(0);
          bActiveScript = false;
        } else {
          buf.append(line).append("\n");
        }
      }
    }
    if(bActiveScript) {
      if(ret !=null) {
        ret.add(buf.toString());                       // old compatible variant.
      } else {
        String script = buf.toString();
        OutTextPreparer otxScript = new OutTextPreparer(name, args, script, 0);  // does not parse, only stores the script.
        idxScript.put(name, otxScript);                // new variant, store the script sorted by name.
        name = null;
        args = null;
      }
    }
  }
  
  
  
  /**Parse all templates which are read with {@link #readTemplateCreatePreparer(InputStream, String, Map)}
   *  or which are given with hard coded text constructs with {@link OutTextPreparer#OutTextPreparer(String, String, String)}.
   * <br>
   * It calls {@link #parse(Class, Map, Map)} for all sub scripts stored in idxScript.
   * 
   * @param idxScript the index of the sub scripts, filled. Used to parse for all, also used for &lt;:call:...>
   * @param execClass May be null, from this class some operations or data or also hard coded {@link OutTextPreparer} instances can be gotten.
   *   Static operations can be called and static data can be accessed. Instance operations and data can be accessed 
   *   if {@link DataTextPreparer#setExecObj(Object)} is set with the proper instance.
   * @param idxConstData May be null, from this index some constant data and also sub scripts can be gotten. 
   * @throws ParseException
   */
  public static void parseTemplates ( Map<String, OutTextPreparer> idxScript, Class<?> execClass, final Map<String, Object> idxConstData, LogMessage log ) throws ParseException {
    if(idxScript!=null) {
      for(Map.Entry<String, OutTextPreparer> e: idxScript.entrySet()) {
        OutTextPreparer otx = e.getValue();
//        if(otx.sIdent.equals("setVar_FBexpr"))
//          Debugutil.stop();
        try { 
          otx.parse(execClass, idxConstData, idxScript);
        } catch( ParseException exc) {
          if(log !=null) {
            log.writeError("ERROR parseTemplates in script: " + otx.sIdent, exc);
          } else {
            throw exc;
          }
        }
      }
    }
  }

  /**Reads a given template which may contain the pattern for several separated OutTextPreparer.
   * See also {@link #readTemplateList(InputStream, String)}
   * <br>
   * The file can contain different patterns in segments: <pre>
   * === patternName
   * content <&withVariables>
   * more lines
   * 
   * === nextPatternName
   *   etc.
   * </pre>
   * With this template Strings several OutTextPreparer can be created due to the schema (Example for Java2Vhdl)<pre>
    InputStream inTpl = Java2Vhdl.class.getResourceAsStream("VhdlTemplate.txt");  //pathInJar with slash: from root.
    Map<String, String> tplTexts = OutTextPreparer.readTemplate(inTpl, "===");
    inTpl.close();
    this.vhdlHead = new OutTextPreparer("vhdlHead", null, "fpgaName", tplTexts.get("vhdlHead"));
    this.vhdlAfterPort = new OutTextPreparer("vhdlAfterPort", null, "fpgaName", tplTexts.get("vhdlAfterPort"));
    this.vhdlConst = new OutTextPreparer("vhdlConst", null, "name, type, value", tplTexts.get("vhdlConst"));
    this.vhdlCmpnDef = new OutTextPreparer("vhdlCmpnDef", null, "name, vars", tplTexts.get("vhdlCmpnDef"));
    this.vhdlCmpnCall = new OutTextPreparer("vhdlCmpnCall", null, "name, typeVhdl, preAssignments, vars", tplTexts.get("vhdlCmpnCall"));
    </pre>
   * 
   * @param inp The open input to read, can also a resource in jar, then use {@link Class#getResourceAsStream(String)} to open
   * @param lineStart String which marks a new pattern segment, for the exmaple it should be "=== " 
   * @return Map contains patternName and pattern String, without empty trailing lines.
   * @throws IOException
   * @deprecated use {@link #readTemplateCreatePreparer(InputStream, String, Class, Map, String)} for all.
   */
  @Deprecated public static Map<String, String> XXXreadTemplate ( InputStream inp, String lineStart) throws IOException {
    Map<String, String> ret = new TreeMap<String, String>();
    InputStreamReader reader = new InputStreamReader(inp, "UTF-8");
    BufferedReader rd = new BufferedReader(reader);
    String line;
    String name = null;
    StringBuilder buf = null;
    int posEnd = 0;
    while( (line = rd.readLine()) !=null) {
      if(line.startsWith(lineStart)) {           // a new sub template starts here 
        if(name !=null) {                        // ends the sub template before if not done yet.
          buf.setLength(posEnd);                 // without trailing newlines.
          ret.put(name, buf.toString());         // store the template before.
        }
        final boolean bHasArgs;
        int posArgs = line.indexOf('(');
        bHasArgs = posArgs >0;
        if(!bHasArgs) { posArgs = line.length(); } // only the name on this line, compatible to before 2023-05
        name = line.substring(lineStart.length(), posArgs).trim();
        buf = new StringBuilder(200);
        if(bHasArgs) {
          buf.append(line.substring(posArgs)).append('\n');  // first line with args starting with "("
        }
        posEnd = 0;
      }
      else if(buf !=null) {
        buf.append(line).append("\n");
        if(line.length() >0) {
          posEnd = buf.length();
        }
      }
      else {
        // ignore lines before the first start.
      }
    }
    if(name !=null) {
      buf.setLength(posEnd);  //without trailing newlines.
      ret.put(name, buf.toString());
    }
    return ret;
  }

  /**Reads a given template which may contain the pattern for some associated OutTextPreparer also for call operations
   * and instantiates all OutTextPreparer to execute the script.
   * @since 2023-05. 
   * <br>
   * The file can contain different patterns in segments: <pre>
   * <:otx: patternName : variable : variable2 >  ##comment
   * content with <&variables>      ##comment
   * more lines<.otx>
   * 
   * free text between pattern
   * 
   * <:otx: nextPatternName :args>
   * <:call:patternName:variables = args> --- more text
   * <.otx>
   *   etc.
   * </pre>
   * @param inp An opened input stream, which can be for example also part of a zip file content
   *   or gotten via {@link Class#getResourceAsStream(String)} from a jar file, 
   *   or also of course via {@link java.io.FileReader#FileReader(java.io.File)}
   *   or via {@link InputStreamReader#InputStreamReader(InputStream, String)} to read a file with specific encoding.
   * @param lineStart The pattern which marks the start of a output text. It was "===" in examples, now deprecated.
   * @param execClass a given class which's content is accessed as persistent data.
   * @param idxConstData An index to access const persistent data, 
   *   and also to store all created OutTextPreparer instances. This is important for <:call:otx...>
   * @param sMainScript name of the main script to return, or null.
   * @return null or the requested main script. 
   *   Note: All scripts can be found in idxConstData.
   * @throws IOException on file error
   * @throws ParseException on parsing error of the script.
   * @deprecated use {@link #readTemplateCreatePreparer(InputStream, String, Map)}.
   */
  @Deprecated public static OutTextPreparer XXXreadTemplateCreatePreparer 
  ( InputStream inp, String lineStart, Class<?> execClass
  , Map<String, Object> idxConstData, String sMainScript 
  ) throws IOException, ParseException {
    XXXreadTemplateCreatePreparerPriv(inp, lineStart, execClass, idxConstData, (Map<String, OutTextPreparer>)null);
    return sMainScript == null ? null : (OutTextPreparer)idxConstData.get(sMainScript);
  } 
    
    
  /**internal implementation, for parsing one script as a whole, but yet in two passed,
   * first gather all sub scripts, then parse all. The order of subscripts is now no more important. 
   * But see {@link #readTemplateCreatePreparer(InputStream, String, Map)}. 
   * see {@link #readTemplateCreatePreparer(InputStream, Class)}
   * @since 2023-08 separated 
   * @param inp stream input for some scripts.
   * @param lineStart null, this is only for the old form with line separation
   * @param execClass null, only necessary for parsing if idxScript is not given
   * @param idxConstData null or possible access to constant data
   * @param idxScript null, then put the script in idxConstData, elsewhere index of all scripts.
   *   If given, then the inp is firstly parsed in a first pass to gather all scripts,
   *   the second path is executed afterwards. Then a script can be &lt;:call:...> which is defined after the calling script.
   * @throws IOException general possible on reading inp
   * @throws ParseException if the inp has syntax errors
   * @deprecated use {@link #readTemplateCreatePreparer(InputStream, String, Map)}.
   */
  @Deprecated private static void XXXreadTemplateCreatePreparerPriv 
  ( InputStream inp, String lineStart, Class<?> execClass
  , Map<String, Object> idxConstData 
  , Map<String, OutTextPreparer> idxScript
  ) throws IOException, ParseException {
    List<String> tplTexts = null;
    tplTexts = OutTextPreparer.readTemplateList(inp, lineStart, null, null);
    for(String text : tplTexts) {
      int posName;
      int posArgs;
      int posEndArgs;
      int posNewline = text.indexOf('\n');
      if(text.startsWith("<:otx:")) {
        posName = 6;
        posArgs=text.indexOf(':', 6);
        posEndArgs = text.indexOf('>');
      }
      else {
        posName = 0;
        posArgs = text.indexOf('(');
        posEndArgs = text.indexOf(')');
      }
      if(! ( posArgs >0 && posEndArgs > posArgs && posNewline > posEndArgs)) throw new ParseException("first line must contain ( args ,...)", 0);
      String name = text.substring(posName, posArgs).trim();
      String args = text.substring(posArgs+1, posEndArgs);
      String script = text.substring(posNewline+1);
      if(idxScript !=null) {
        OutTextPreparer otxScript = new OutTextPreparer(name, args, script, 0);
        idxScript.put(name, otxScript); 
      }
      else { 
        OutTextPreparer otxScript = new OutTextPreparer(name, execClass, args, script, idxConstData, idxScript);
        idxConstData.put(name, otxScript); 
      }
    }
    if(idxScript !=null) {
      parseTemplates(idxScript, execClass, idxConstData, null);
    }
  }
  
  
  
  /**Older operation to read one template script and create all {@link OutTextPreparer} instances but does not parse.
   * To parse all read scripts call {@link #parseTemplates(Map, Class, Map, LogMessage)} afterwards.
   * This assures that all sub scripts in all templates can be &lt;:call...> independent of the definition order.
   *  
   * @since 2023-12 new version due to pdf documentation.
   * @since 2024-01-15 more arguments dataRoot, idxConstData, for compatibility provide null for both.
   *
   * @param inp stream input for some scripts.
   * @param lineStart null, this is only for the old form with line separation
   * @param idxScript index of all scripts. The scripts are created, the text is referred via {@link OutTextPreparer#pattern} but the script is not parsed yet.
   *   Later parsing allows that a script can be &lt;:call:...> which is defined after the calling script. 
   * @param dataRoot null admissible, possibility to set values from there in <code>&lt;:set:name=&value></code>
   * @param idxConstData necessary if <code>&lt;:set:name=value></code> is used. 
   *   null admissible if the inp does not contain <code>&lt;:set:name=&value></code> outside of scripts
   * @throws IOException general possible on reading inp
   * @throws ParseException if the inp has syntax errors
   * @deprecated remove it, use {@link #readStreamTemplateCreatePreparerNew(InputStream, String, Map, Object, Map)}
   */
  @Deprecated public static void XXXreadStreamTemplateCreatePreparer 
  ( InputStream inp, String lineStart
  , Map<String, OutTextPreparer> idxScript
  , Object dataRoot, Map<String, Object> idxConstData
  ) throws IOException, ParseException {
    List<String> tplTexts = null;
    tplTexts = OutTextPreparer.readTemplateList(inp, lineStart, dataRoot, idxConstData);
    for(String text : tplTexts) {
      int posName;
      int posArgs;
      int posEndArgs;
      int posNewline = text.indexOf('\n');
      if(text.startsWith("<:otx:")) {                      // recommended text of a script
        posName = 6;
        posArgs=text.indexOf(':', 6);
        posEndArgs = text.indexOf('>');
      }
      else {                                               // older form, separation with lineStart argument.
        posName = 0;
        posArgs = text.indexOf('(');
        posEndArgs = text.indexOf(')');
      }
      if(! ( posArgs >0 && posEndArgs > posArgs && posNewline > posEndArgs)) throw new ParseException("first line must contain ( args ,...)", 0);
      String name = text.substring(posName, posArgs).trim();
      if(name.equals("castValue_FD")) Debugutil.stopp();
      String args = text.substring(posArgs+1, posEndArgs);
      int posStartScript = posEndArgs +1;                  // script starts after '>'
      while(posStartScript < posNewline+1) {
        if("\n\r ".indexOf(text.charAt(posStartScript)) >=0) {
          posStartScript +=1;                              // but first skip over all white spaces till the next line
        } else {
          break;                                           // the first character which is not a white space also in the first line is the start.
        }
      }
      String script = text.substring(posStartScript);
      OutTextPreparer otxScript = new OutTextPreparer(name, args, script, 0);  // does not parse, only stores the script.
      idxScript.put(name, otxScript); 
    }
  }
  
  
  /**You can use this variant instead {@link #readTemplateCreatePreparer(InputStream, String, Class, Map, String)}
   * if you have not specific const Data for parsing the script.
   * @param inp An opened input stream, which can be for example also part of a zip file content
   *   or gotten via {@link Class#getResourceAsStream(String)} from a jar file, 
   *   or also of course via {@link java.io.FileReader#FileReader(java.io.File)}
   *   or via {@link InputStreamReader#InputStreamReader(InputStream, String)} to read a file with specific encoding.
   * @param execClass a given class which's content is accessed as persistent data.
   * @return Map which contains all script operations.
   *   access to the desired script via get(name).
   * @throws IOException
   * @throws ParseException
   * @since 2023-08 as common usable operation for some OutText templates in one file. 
   *   The constData processed in {@link #readTemplateCreatePreparer(InputStream, String, Class, Map, String)} 
   *   are usual not necessary for that approach, because it was the older concept to define some const data in the Map,
   *   and the Map is completed with the found scripts. 
   *   Here now it is simpler, the operation returns the new Map filled with the content of the read script. 
   * @deprecated use {@link #readTemplateCreatePreparer(InputStream, String, Map)}.
   */
  @Deprecated public static Map<String, OutTextPreparer> XXXreadTemplateCreatePreparer ( InputStream inp, Class<?> execClass) 
      throws IOException, ParseException {
      //
      Map<String, OutTextPreparer> idxScript = new TreeMap<String, OutTextPreparer>();
      XXXreadTemplateCreatePreparerPriv(inp, null, execClass, null, idxScript);
      return idxScript;
    }

  
  
  /**This is the recommended operation to read the template for some OutTextPreparer from a String given pattern..
   * @param inp An opened input stream, which can be for example also part of a zip file content
   *   or gotten via {@link Class#getResourceAsStream(String)} from a jar file, 
   *   or also of course via {@link java.io.FileReader#FileReader(java.io.File)}
   *   or via {@link InputStreamReader#InputStreamReader(InputStream, String)} to read a file with specific encoding.
   * @param execClass a given class which's content is accessed as persistent data.
   * @param idxConstData
   * @return Map which contains all script operations.
   *   access to the desired script via get(name).
   * @return index of all OutTextPreparer instance, any can be used. For example the "main" script to start.
   * @throws IOException
   * @throws ParseException
   * @since 2023-10-22 Used for LibreOffice / FBcl
   * @deprecated use {@link #readTemplateCreatePreparer(InputStream, String, Map)}.
   */
  @Deprecated public static Map<String, OutTextPreparer> XXXreadTemplateCreatePreparer ( 
      InputStream inp, Class<?> execClass, Map<String, Object> idxConstData) 
      throws IOException, ParseException {
    //
    Map<String, OutTextPreparer> idxScript = new TreeMap<String, OutTextPreparer>();
    XXXreadTemplateCreatePreparerPriv(inp, null, execClass, idxConstData, idxScript);
    return idxScript;
  }
  
  
    
  /**This is the recommended operation to read the template for some OutTextPreparer from a String given pattern..
   * @param inp An opened input stream, which can be for example also part of a zip file content
   *   or gotten via {@link Class#getResourceAsStream(String)} from a jar file, 
   *   or also of course via {@link java.io.FileReader#FileReader(java.io.File)}
   *   or via {@link InputStreamReader#InputStreamReader(InputStream, String)} to read a file with specific encoding.
   * @param execClass a given class which's content is accessed as persistent data.
   * @param idxConstData
   * @return Map which contains all script operations.
   *   access to the desired script via get(name).
   * @return index of all OutTextPreparer instance, any can be used. For example the "main" script to start.
   * @throws IOException
   * @throws ParseException
   * @since 2023-10-22 Used for LibreOffice / FBcl
   * @deprecated use {@link #readTemplateCreatePreparer(InputStream, String, Map)}.
   */
  @Deprecated public static Map<String, OutTextPreparer> XXXreadTemplateCreatePreparer ( 
      InputStream inp, Map<String, Object> idxConstData) 
      throws IOException, ParseException {
    //
    Map<String, OutTextPreparer> idxScript = new TreeMap<String, OutTextPreparer>();
    XXXreadTemplateCreatePreparerPriv(inp, null, null, idxConstData, idxScript);
    return idxScript;
  }
  
  
    
  /**Standard operation to write an output text from given data with a given template.
   * @param fout The file to write
   * @param data Data for the output with the script, only one is possible.
   * @param inTpl opened input stream which offers the otx-template to read.
   *   This inTpl is closed here after read.
   * @param execClass a given class which's content is accessed as persistent data.
   * @param sMain Name of the start or main template in the read script.
   * @throws IOException
   * @since 2023-08, but it is limited for only one data. Hence it may be used only as template.
   */
  @Deprecated public static void XXXwriteOtx(File fout, Object data, InputStream inTpl, Class<?> execClass, String sMain) throws IOException {
    OutTextPreparer otxt = null;
    try {
      Map<String, Object> idxOtxt = new TreeMap<String, Object>();
      otxt = OutTextPreparer.XXXreadTemplateCreatePreparer(inTpl, "===", execClass, idxOtxt, sMain);
      inTpl.close();
      Writer wr = new OutputStreamWriter(new FileOutputStream(fout), "UTF-8");
      //Appendable wr = new StringBuilder();
      OutTextPreparer.DataTextPreparer dp;
      dp = otxt.createArgumentDataObj();
      dp.setArgument("data", data);                        // here more flexibility is necessary,
      //                                                   // hence write an own script with more possible data 
      otxt.exec(wr, dp);                                   
      //
      wr.close();
    } catch(IOException exc) {
      System.err.println("File Exception: " + exc.getMessage());
    } catch(ParseException exc) {
      CharSequence sExc = ExcUtil.exceptionInfo("Exception", exc, 0, 20);
      System.err.println(sExc);
    }
    
    return;
  }

  
  
  /**Sets all variables from string, detects 'NEWLINE'.
   * This are the argument variables for the script.
   * It calls {@link #setVariables(List)}
   * Additional the variables 'OUT', 'OTX', 'OTXCMD', 'OTXDATA' are set.
   * <br><br>
   * If instead the variable name 'NEWLINE' is written,
   * it sets the line mode for parsing the script. Then:
   * <ul><li>The immediately next characters, which should be the same,
   *     defines the start sequence of a newline in the output script.
   *     This char sequence is set to {@link #sLineoutStart} to detect it in the script.
   *   <li>immediately after this characters a variable as indentifier may be given
   *     which is stored as variable, but its index is set to {@link #ixVarLineoutStart}.
   *     The content of this variable is then output as newline sequence.
   *     It should contain '\n' or other desired newline sequence on first position, 
   *     and then indentation characters. 
   *   <li>If this variable is not found, means 'NEWLINE---,' is found, 
   *     then '\n' will be output simple as newline character string.
   * </ul>
   * @param variables String with identifier names, separated with comma, white spaces admissible. 
   *   first name in the String gets the index 0
   */
  private void parseVariables(String variables) {
    List<String> listvarValues = new LinkedList<String>();
    StringPartScan sp = new StringPartScan(variables);
    sp.setIgnoreWhitespaces(true);
    while(sp.scanStart().scanIdentifier().scanOk()) {
      String sVariable = sp.getLastScannedString();
      if(sVariable.equals("NEWLINE")) {
        char cLineoutStart = sp.getCurrentChar();
        long pos = sp.getCurrentPosition();
        sp.seekNoChar("" + cLineoutStart);
        long pos2 = sp.getCurrentPosition();
        this.nLineoutStart = (int)(pos2 - pos);
        this.sLineoutStart = sp.getCharSequenceRange(pos, pos2).toString();
        if(sp.scanStart().scanIdentifier().scanOk()) {
          String sVariableLineIndent = sp.getLastScannedString();
          this.ixVarLineoutStart = listvarValues.size();
          this.nameVarLineoutStart = sVariableLineIndent;
          listvarValues.add(sVariableLineIndent);
        }
      } else {
        listvarValues.add(sVariable);
      }
      if(!sp.scan(",").scanOk() && !sp.scan(":").scanOk()) {
        break; //, as separator
      }
    }
    sp.close();
    setVariables(listvarValues);
  }
  
  
  
  /**Sets all variables from list, and add also "OUT", "CMD" and "OTXdata" "OTXcmd".
   * <ul>
   * <li>"OUT" is the opened output writer for generation (Type Appendable)
   * <li>"OTXcmd" is the actual {@link Cmd}
   * <li>"OTXdata" is the actual {@link DataTextPreparer}
   * <li>"OTX" it this. used for debug.
   * </ul>
   * The first variables have the index in order of the argument list of the otx scripts.
   * This is important to also access the variables manually by index.
   * Variables after "OTX" are variables created in the otx script itself.
   * <br><br>
   * If sets {@link #nameVariables} with the consecutive index adequate the order in 'listArgs'
   * and then the for named "OUT" etc. 
   * @param listArgs first argument in the list gets the index 0
   */
  private void setVariables(List<String> listArgs) {
    // Note: get nameVariables.size() newly again because if the name is already stored, it does not increment the size.
    for(String var: listArgs) {
      this.nameVariables.put(var, new DataAccess.IntegerIx(this.nameVariables.size()));
    }
    if(this.nameVariables.get("OUT") == null) {
      this.ixOUT = this.nameVariables.size();
      this.nameVariables.put("OUT", new DataAccess.IntegerIx(this.ixOUT));
      this.nameVariables.put("OTXcmd", new DataAccess.IntegerIx(this.nameVariables.size()));
      this.nameVariables.put("OTXdata", new DataAccess.IntegerIx(this.nameVariables.size()));
      this.nameVariables.put("OTX", new DataAccess.IntegerIx(this.nameVariables.size()));
      int nFault = this.ixOUT + 4 - this.nameVariables.size();
      assert(nFault >=0);   // never <0
      if(nFault >0) {
        while(--nFault >=0) {
          this.nameVariables.put("dummy_OUT_already_given_" + nFault, new DataAccess.IntegerIx(this.nameVariables.size()));
        }                                                  // assure that this variables are in the correct order, put again in this order.
        this.nameVariables.put("OTXcmd", new DataAccess.IntegerIx(this.ixOUT +1));
        this.nameVariables.put("OTXdata", new DataAccess.IntegerIx(this.ixOUT +2));
        this.nameVariables.put("OTX", new DataAccess.IntegerIx(this.ixOUT +3));
      }
    }
  }

  
  /**Parse the pattern. This routine will be called from the constructor or in application
   * or especially in {@link #readTemplateCreatePreparer(InputStream, Class, Map)}
   * for two-phase-translation. 
   * @param execClass used to parse &lt;:exec:...>, the class where the operation should be located.
   * @param idxScript Map with all sub scripts to support &lt;:call:subscript..>
   * @throws ParseException 
   */ 
  public void parse(Class<?> execClass, final Map<String, Object> idxConstDataArg, Map<String, OutTextPreparer> idxScript) throws ParseException {
    ParseHelper thiz = new ParseHelper(this, execClass, idxConstDataArg, idxScript);
    thiz.parse();
    this.nameVariablesByIx = new String[this.nameVariables.size()];
    for(Map.Entry<String, DataAccess.IntegerIx> e : this.nameVariables.entrySet()) {
      int ix = e.getValue().ix;
      String name = e.getKey();
      this.nameVariablesByIx[ix] = name;
    }
  }
  
  
  
  /**internal class to organize data for parsing.
   * @since 2025-05-07
   */
  private static class ParseHelper {
    
    final OutTextPreparer otx;
    
    final Map<String, Object> idxConstData;
    
    //final Map<String, Integer> idxIxWrBufferIdent = new TreeMap<>();
    
    final Class<?> execClass;
    
    final Map<String, OutTextPreparer> idxScript;
    
    int pos0 = 0; //start of current position after special cmdf
    
    int pos1 = 0; //end before the next special command
    
    int[] ixCtrlCmd = new int[20];  //max. 20 levels for nested things.
    
    int ixixCmd = -1;
    
    //int line =0, colmn =0;
    
    boolean bNewline = false;
    
    final StringPartScanLineCol sp;       
    
    int nLastWasSkipOverWhitespace = 0;
    
    
    ParseHelper (OutTextPreparer otx, Class<?> execClass, final Map<String, Object> idxConstDataArg, Map<String, OutTextPreparer> idxScript) {
      this.otx = otx;
      this.sp = new StringPartScanLineCol(otx.pattern);       // Note: pattern, pos0, pos1 is used to select a immediately output text
      this.idxConstData = idxConstDataArg !=null ? idxConstDataArg: idxConstDataDefault;
      this.execClass = execClass;
      this.idxScript = idxScript;
    }

  
  
    /**Parse the given script in the created {@link OutTextPreparer} instance with given variables.
     * @throws ParseException
     */
    void parse() throws ParseException {
      if(this.otx.sLineoutStart !=null) {        //---------- line mode:
        this.bNewline = true;                              // the script starts with newline.
      }
      this.sp.setIgnoreWhitespaces(true);  // it is valid inside the syntactical relevant parts <...>
      while(this.sp.length() >0) {               //========== main loop of parsing
        this.nLastWasSkipOverWhitespace +=1;
        if(this.bNewline && this.otx.sLineoutStart !=null) {  
          //===============================================vv newline in line mode
          if(this.sp.scanStart().scan(this.otx.sLineoutStart).scanOk()) { //========== newline found 
            if(this.otx.nameVarLineoutStart !=null) {      // insert name of this variable to write the newline content later.
              addCmdSimpleVar(null, this.sp.getlineCol(), 0,0, ECmd.addVar, this.otx.nameVarLineoutStart);
            } else {
              addCmd("\n", this.sp.getlineCol(), 0, 1, null);
            }
            //this.sp.seekNoChar(this.otx.sLineoutStart.substring(0,1));
          } else {
            this.sp.seekNoWhitespace();   // if no newline mark found, ignore the bNewline, start on next content
          }
          this.bNewline = false;
          this.pos0 = (int)this.sp.getCurrentPosition();
        } //===============================================^^ newline in line mode
        if(this.otx.sLineoutStart !=null) { this.sp.lentoLineEnd(); }    // line mode
        if(this.sp.scanStart().scan("##").scanOk()) {             // if a ## was found, seek till newline.
          this.sp.seek("\n").seekPos(1);                          // skip all till newline
          this.sp.seekNoWhitespace();
          this.bNewline = true;
          this.pos0 = (int)this.sp.getCurrentPosition();
        }                                                    // Note: spaces are detected because of content till <
        //    //===========================================vv search an <...>, the text before is also stored.
        this.sp.seek("<", StringPart.mSeekCheck + StringPart.mSeekEnd);  // < is always start of a special output
        if(this.sp.found()) {
          this.sp.lentoMax();                              // in line mode, here see the whole pattern.
          parseElement();                                  // stores also the text before the element.
        }     //===========================================^^ found a element in script or only in the line
        else { //==========================================vv no more '<' found in the script
          if(this.otx.sLineoutStart !=null) {    //--------vv line mode
            String sText;
            sText = this.sp.getCurrent().toString();                       // the rest of the line
            int posComment = sText.indexOf("##");
            if(posComment >=0) {
              sText = sText.substring(0, posComment);
              sText.trim();                                // without spaces before ##
            }
            int zText = sText.length();
            if(zText>0) {                                  // add the last text of the line.
              addCmd(sText.toString(), this.sp.getlineCol(), 0, zText, null);
            }
            this.sp.fromEnd().seekNoChar("\n\r\f");        // start scanning from next line start
            this.bNewline = true;
          }                                      //========^^ line mode 
          else {                                 //========vv really the end of script.
            this.sp.len0end();                             // add the last text of the pattern
            addCmd(this.otx.pattern, this.sp.getlineCol(), this.pos0, this.pos0 + this.sp.length(), ECmd.nothing, null);
            this.sp.fromEnd();  //length is null then.
          }
        }
      } //while
      if(this.ixixCmd >=0) {
        int[] lineCol = sp.getlineCol();
        this.sp.close();
        throw new ParseException("OutTextPreparer closing <.> is missing: " + this.otx.sIdent + "@" + lineCol[0] + "," + lineCol[1] + " to cmd: " + this.otx.cmds.get(this.ixCtrlCmd[this.ixixCmd]),0);
      }
      this.sp.close();
    }
  
    
    
    
    private void parseElement ( ) throws ParseException {
      this.pos1 = (int)this.sp.getCurrentPosition() -1;     //before '<' as end of text before
      this.sp.scanStart();
      //if(this.sp.scan("&").scanIdentifier().scan(">").scanOk()){
      if(this.sp.scan(":args:").scanOk()){ 
        parseArgs();
        if(!this.sp.scan(">").scanOk()) { 
          addError("faulty <:args:... at " + this.sp.getCurrentPosition(), this.sp.getlineCol());
        }
        this.pos0 = (int)this.sp.getCurrentPosition();  //after '>'
      }
      else if(  this.nLastWasSkipOverWhitespace !=0 //The last scan action was not a <: >, it it was, it is one space insertion.
          && (this.sp.scan(": >").scanOk() || this.sp.scan(":+>").scanOk() || this.sp.scan(":? >").scanOk())){ 
        addCmd(this.otx.pattern, this.sp.getlineCol(), this.pos0, this.pos1, ECmd.nothing, null);  //adds the text before <:+>
        this.sp.scanSkipSpace();
        this.pos0 = (int)this.sp.getCurrentPosition();  //after '>'
        this.nLastWasSkipOverWhitespace = -1;  //then the next check of <: > is not a skipOverWhitespace
      }  
      else if( this.sp.scan(":?nl>").scanOk()){ 
        addCmd(this.otx.pattern, this.sp.getlineCol(), this.pos0, this.pos1, ECmd.nothing, null);  //adds the text before <:+>
        this.sp.seekAfterNewline();
        this.pos0 = (int)this.sp.getCurrentPosition();  //after newline
      }
      else if(this.sp.scan("&").scanIdentifier().scan(">").scanOk()){
        //------------------------------------------------- <&varname> a simple access to a given value
        String sName = this.sp.getLastScannedString();        // it adds a simple index in Cmd, not using DataAccess
        addCmdSimpleVar(this.otx.pattern, this.sp.getlineCol(), this.pos0, this.pos1, ECmd.addVar, sName);
        this.pos0 = (int)this.sp.getCurrentPosition();  //after '>'
      }
      else if(this.sp.scan("&#>").scanOk()) { //The line count
        addCmd(this.otx.pattern, this.sp.getlineCol(), this.pos0, this.pos1, ECmd.addLinenr, null);
        this.pos0 = (int)this.sp.getCurrentPosition();  //after '>'
      }
      else if(this.sp.scan("&").scanToAnyChar(">:", '\0', '\0', '\0').scanOk()){
        //------------------------------------------------- <&data.path:format> more complex access to a given value
        final String sDatapath = this.sp.getLastScannedString();
  //      if(sDatapath.startsWith("&("))
  //        Debugutil.stop();
  //      if(sDatapath.contains("pin.pinDtype.dataType.dt.sTypeCpp"))
  //        Debugutil.stop();
        //====> ////
        final String sFormat;
        if(this.sp.scan(":").scanToAnyChar(">", '\0', '\0', '\0').scanOk()) {
          sFormat = this.sp.getLastScannedString();           // <&...:format> is given
        } else {
          sFormat = null;
        }
        addCmdValueAccess(this.otx.pattern, this.sp.getlineCol(), this.pos0, this.pos1, sDatapath, sFormat, this.execClass, this.idxConstData);
        this.sp.seekPos(1);
        this.pos0 = (int)this.sp.getCurrentPosition();  //after '>'
      }
      else if(this.sp.scan(":wr:").scanToAnyChar(">", '\\', '"', '"').scan(">").scanOk()) {
        //====>  ------------------------------------------ <:if:...>
        parseWr();
        this.ixCtrlCmd[++this.ixixCmd] = this.otx.cmds.size()-1;  //The position of the current wr
        this.pos0 = (int)this.sp.getCurrentPosition();  //after '>'
        
      }
      else if(this.sp.scan(":if:").scanToAnyChar(">", '\\', '"', '"').scan(">").scanOk()) {
        //====>  ------------------------------------------ <:if:...>
        parseIf( ECmd.ifCtrl);
        this.ixCtrlCmd[++this.ixixCmd] = this.otx.cmds.size()-1;  //The position of the current if
        this.pos0 = (int)this.sp.getCurrentPosition();  //after '>'
        
      }
      else if(this.sp.scan(":elsif:").scanToAnyChar(">", '\\', '"', '"').scan(">").scanOk()) {
        //====>
        parseIf( ECmd.elsifCtrl);
        Cmd ifCmdLast;
        int ixixIfCmd = this.ixixCmd; 
        if(  ixixIfCmd >=0 
          && ( (ifCmdLast = this.otx.cmds.get(this.ixCtrlCmd[this. ixixCmd])).cmd == ECmd.ifCtrl 
             || ifCmdLast.cmd == ECmd.elsifCtrl
          )  ) {
          ((IfCmd)ifCmdLast).offsElsif = this.otx.cmds.size() - this.ixCtrlCmd[this. ixixCmd] -1;   //The distance from <:if> to next <:elsif> 
        } else { 
          int[] lineCol = sp.getlineCol();
          this.sp.close();
          throw new ParseException("OutTextPreparer faulty <:elsif>: " + this.otx.sIdent + "@" + lineCol[0] + "," + lineCol[1], lineCol[0]);
        }
        this.ixCtrlCmd[++this. ixixCmd] = this.otx.cmds.size()-1;  //The position of the current <:elsif>
        
        this.pos0 = (int)this.sp.getCurrentPosition();  //after '>'
      }
      else if(this.sp.scan(":else>").scanOk()) {
        //====>
        addCmd(this.otx.pattern, this.sp.getlineCol(), this.pos0, this.pos1, ECmd.elseCtrl, null);
        Cmd ifCmd;
        int ixixIfCmd = this.ixixCmd; 
        if(  ixixIfCmd >=0 
            && ( (ifCmd = this.otx.cmds.get(this.ixCtrlCmd[this. ixixCmd])).cmd == ECmd.ifCtrl 
               || ifCmd.cmd == ECmd.elsifCtrl
                )  ) {
          ((IfCmd)ifCmd).offsElsif = this.otx.cmds.size() - this.ixCtrlCmd[this. ixixCmd] -1;   //The distance from <:if> to next <:elsif> 
        }else { 
          int[] lineCol = sp.getlineCol();
          this.sp.close();
          throw new ParseException("OutTextPreparer faulty <:else>: " + this.otx.sIdent + "@" + lineCol[0] + "," + lineCol[1], lineCol[0]);
        }
        this.ixCtrlCmd[++this.ixixCmd] = this.otx.cmds.size()-1;       //The position of the current <:else>
  
        this.pos0 = (int)this.sp.getCurrentPosition();  //after '>'
      }
      else if(this.sp.scan(":for:").scanIdentifier().scan(":").scanToAnyChar(">", '\\', '"', '"').scan(">").scanOk()) {
        String container = this.sp.getLastScannedString().toString();
        String entryVar = this.sp.getLastScannedString().toString();
        //====>
        ForCmd cmd = (ForCmd)addCmd(this.otx.pattern, this.sp.getlineCol(), this.pos0, this.pos1, ECmd.forCtrl, container);
        DataAccess.IntegerIx ixOentry = this.otx.nameVariables.get(entryVar); 
        if(ixOentry == null) { //Check whether the same entry variable exists already from another for, only ones.
          ixOentry = new DataAccess.IntegerIx(this.otx.nameVariables.size());         //create the entry variable newly.
          this.otx.nameVariables.put(entryVar, ixOentry);
        }
        cmd.ixEntryVar = ixOentry.ix;
        String entryVarNext = entryVar + "_next";                             // the descendant of the current element is also available. 
        ixOentry = this.otx.nameVariables.get(entryVarNext); 
        if(ixOentry == null) { //Check whether the same entry variable exists already from another for, only ones.
          ixOentry = new DataAccess.IntegerIx(this.otx.nameVariables.size());         //create the entry variable newly.
          this.otx.nameVariables.put(entryVarNext, ixOentry);
        }
        cmd.ixEntryVarNext = ixOentry.ix;
        String entryKey = entryVar + "_key";                             // the descendant of the current element is also available. 
        ixOentry = this.otx.nameVariables.get(entryKey); 
        if(ixOentry == null) { //Check whether the same entry variable exists already from another for, only ones.
          ixOentry = new DataAccess.IntegerIx(this.otx.nameVariables.size());         //create the entry variable newly.
          this.otx.nameVariables.put(entryKey, ixOentry);
        }
        cmd.ixEntryKey = ixOentry.ix;
        this.ixCtrlCmd[++this. ixixCmd] = this.otx.cmds.size()-1;
        this.pos0 = (int)this.sp.getCurrentPosition();  //after '>'
      }
      else if(this.sp.scan(":set:").scanIdentifier().scan("=").scanToAnyChar(">", '\\', '\'', '\'').scan(">").scanOk()) {
        String value = this.sp.getLastScannedString().toString();
        String variable = this.sp.getLastScannedString().toString();
  //      if(variable.equals("sIx"))
  //        Debugutil.stop();
        SetCmd cmd = (SetCmd)addCmd(this.otx.pattern, this.sp.getlineCol(), this.pos0, this.pos1, ECmd.setVar, value);
        DataAccess.IntegerIx ixOentry = this.otx.nameVariables.get(variable); 
        if(ixOentry == null) { //Check whether the same entry variable exists already from another for, only ones.
          ixOentry = new DataAccess.IntegerIx(this.otx.nameVariables.size());         //create the entry variable newly.
          this.otx.nameVariables.put(variable, ixOentry);
        }
        cmd.ixVariable = ixOentry.ix;
        this.pos0 = (int)this.sp.getCurrentPosition();  //after '>'
      }
      else if(this.sp.scan(":type:").scanToAnyChar(":", '\\', '\'', '\'').scan(":").scanToAnyChar(">", '\\', '\'', '\'').scan(">").scanOk()) {
        String sType = this.sp.getLastScannedString().toString();
        String sValue = this.sp.getLastScannedString().toString();
        Class<?> type = null;;
        try { type = Class.forName(sType);               // Class given as string found on parse time.
        } catch (ClassNotFoundException e) {
          String sError = "Class not found for <:type:"+ sValue + ":" + sType + ">";
          addError(sError, this.sp.getlineCol());                              // then ignore this cmd in execution. Write the error text.
          //throw new ParseException(sError, 0);
        }
        if(type !=null) {
          try { 
            Cmd cmd = new TypeCmd(this.otx.cmds.size(), this.sp.getlineCol(), this.otx, sValue, type, this.execClass, this.idxConstData);  //sValue builds an Operand with knowledge of nameVariables
            addCmd(this.otx.pattern, this.sp.getlineCol(), this.pos0, this.pos1, cmd);
          } catch(Exception exc) {
            String sError = "Problem with datapath <:type:"+ sValue + "...: " + exc.getMessage();
            addError(sError, this.sp.getlineCol());
            //throw new ParseException(sError, 0);
          }
        }
        this.pos0 = (int)this.sp.getCurrentPosition();  //after '>'
      }
      else if(this.sp.scan(":exec:").scanOk()) { //scanIdentifier().scanOk()) {
        //====>
        Cmd cmd = parseExec(this.otx.pattern, this.pos0, this.pos1, this.sp, this.execClass, this.idxConstData,this. idxScript);
        this.otx.cmds.add(cmd);
        this.pos0 = (int)this.sp.getCurrentPosition();  //after '>'
      }
      else if(this.sp.scan(":call:").scanIdentifier().scanOk()) {
        //====>
        parseCall(this.otx.pattern, this.pos0, this.pos1, this.sp, this.execClass, this.idxConstData,this. idxScript);
        this.pos0 = (int)this.sp.getCurrentPosition();  //after '>'
      }
      else if(this.sp.scan(":debug:").scanIdentifier().scan(":").scanToAnyChar(">", '\\', '"', '"').scan(">").scanOk()) {
        String cmpString = this.sp.getLastScannedString().toString();
        String debugVar = this.sp.getLastScannedString().toString();
        //====>
        DebugCmd cmd = (DebugCmd)addCmd(this.otx.pattern, this.sp.getlineCol(), this.pos0, this.pos1, ECmd.debug, debugVar);
        cmd.cmpString = cmpString;
        this.pos0 = (int)this.sp.getCurrentPosition();  //after '>'
      }  
      else if(this.sp.scan(":debug>").scanOk()) {
        //====>
        @SuppressWarnings("unused") DebugCmd cmd = (DebugCmd)
        addCmd(this.otx.pattern, this.sp.getlineCol(), this.pos0, this.pos1, ECmd.debug, null);
        this.pos0 = (int)this.sp.getCurrentPosition();  //after '>'
      }
      else if(this.sp.scan(":--").scanToStringEnd("-->").scanOk()) {
        //it is commented
        addCmd(this.otx.pattern, this.sp.getlineCol(), this.pos0, this.pos1, ECmd.nothing, null);
        this.pos0 = (int)this.sp.getCurrentPosition();  //after '>'
      }
      else if(this.sp.scan(":x").scanHex(4).scan(">").scanOk()) {
        char[] cText = new char[1];                      // <:x1234> a special UTF16 char
        cText[0] = (char)this.sp.getLastScannedIntegerNumber();
        String sTextSpecial = new String(cText);
        addCmd(this.otx.pattern, this.sp.getlineCol(), this.pos0, this.pos1, ECmd.addString, sTextSpecial);
      }
      else if(this.sp.scan(":").scanToAnyChar(">", '\0', '\0', '\0').scan(">").scanOk()) {
        // ------------------------------------------------ <:text> or <:n> <:r> <:t> <:s>
        final int posend = (int)this.sp.getCurrentPosition();
        final String sText = this.otx.pattern.substring(this.pos1+2, posend-1);
        //Note: do not use this.sp.getLastScannedString().toString(); because scanToAnyChar skips over whitespaces
        final int what;
        if(sText.length() == 1) {
          what = "nrts".indexOf(sText.charAt(0));        // <:n> <:s> <:r>: <:t>
        } else {
          what = -1; 
        }
        if(what >=0) {
          String[] specials = { "\n", "\r", "\t", " "};  // newline etc.
          addCmd(this.otx.pattern, this.sp.getlineCol(), this.pos0, this.pos1, ECmd.addString, specials[what]);
        } else {                                         // free text with especially special chars
          addCmd(this.otx.pattern, this.sp.getlineCol(), this.pos0, this.pos1, ECmd.addString, sText); //add the <:sText>
        }
        this.pos0 = posend;  //after '>'
      }
      else if(this.sp.scan(".wr>").scanOk()) { //The end of an if
        Cmd wrCmd;
        addCmd(this.otx.pattern, this.sp.getlineCol(), this.pos0, this.pos1, ECmd.nothing, null);  //The last text before <.wr>
        if(this. ixixCmd >=0 && (wrCmd = this.otx.cmds.get(this. ixCtrlCmd[this. ixixCmd])).cmd == ECmd.wr) {
          Cmd endWr = addCmd(this.otx.pattern, this.sp.getlineCol(), this.pos0, this.pos1, ECmd.wrEnd, null);
          endWr.offsEndCtrl = -this.otx.cmds.size() - this.ixCtrlCmd[this. ixixCmd] -1;
          this.pos0 = (int)this.sp.getCurrentPosition();  //after '>'
          wrCmd.offsEndCtrl = this.otx.cmds.size() - this.ixCtrlCmd[this. ixixCmd];
          this. ixixCmd -=1;
        } 
        
        else {
          int[] lineCol = sp.getlineCol();
          this.sp.close();
          throw new ParseException("OutTextPreparer faulty <.wr> missing opening <:wr:...>: " + this.otx.sIdent + "@" + lineCol[0] + "," + lineCol[1], lineCol[0]);
        }
        this.pos0 = (int)this.sp.getCurrentPosition();  //after '>'
      }
      else if(this.sp.scan(".if").scanOk()) {    //--------vv The end of an if // <.if:any comment>
        this.sp.seek('>', StringPart.mSeekEnd);
        Cmd cmd = null;
        addCmd(this.otx.pattern, this.sp.getlineCol(), this.pos0, this.pos1, ECmd.nothing, null);  //The last text before <.if>
        while(  this. ixixCmd >=0 
          && ( (cmd = this.otx.cmds.get(this. ixCtrlCmd[this. ixixCmd])).cmd == ECmd.ifCtrl 
            || cmd.cmd == ECmd.elsifCtrl
            || cmd.cmd == ECmd.elseCtrl
          )  ) {
          IfCmd ifcmd;
          cmd.offsEndCtrl = this.otx.cmds.size() - this.ixCtrlCmd[this. ixixCmd];
          if(cmd.cmd != ECmd.elseCtrl && (ifcmd = (IfCmd)cmd).offsElsif <0) {
            ifcmd.offsElsif = ifcmd.offsEndCtrl; //without <:else>, go after <.if>
          }
          this.ixCtrlCmd[this. ixixCmd] = 0;    // no more necessary.
          this. ixixCmd -=1;
          if(cmd.cmd == ECmd.ifCtrl) {
            break;
          } else {
            cmd = null;    //remain ifCtrl to check: at least <:if> necessary.
          }
        } 
        if(cmd == null) {  //nothing found or <:if not found: 
          String sError = this.sp.getCurrent(30).toString();
          int[] lineCol = sp.getlineCol();
          this.sp.close();
          throw new ParseException("OutTextPreparer faulty <.if> without <:if> or  <:elsif> : " + this.otx.sIdent + "@" + lineCol[0] + "," + lineCol[1] + ": " + sError, lineCol[0]);
        }
        this.pos0 = (int)this.sp.getCurrentPosition();  //after '>'
      }                                                    
      else if(this.sp.scan(".for").scanOk()) {   //--------vv The end of a for loop // <.for:any comment>
        this.sp.seek('>', StringPart.mSeekEnd);
        Cmd forCmd;
        if(this. ixixCmd >=0 && (forCmd = this.otx.cmds.get(this. ixCtrlCmd[this. ixixCmd])).cmd == ECmd.forCtrl) {
          Cmd endLoop = addCmd(this.otx.pattern, this.sp.getlineCol(), this.pos0, this.pos1, ECmd.endLoop, null);
          endLoop.offsEndCtrl = -this.otx.cmds.size() - this.ixCtrlCmd[this. ixixCmd] -1;  // it is faulty but unused.
          this.pos0 = (int)this.sp.getCurrentPosition();  //after '>'
          forCmd.offsEndCtrl = this.otx.cmds.size() - this.ixCtrlCmd[this. ixixCmd];
          this. ixixCmd -=1;
        } 
        
        else {
          int[] lineCol = sp.getlineCol();
          String sError = String.format("\nOutTextPreparer faulty <.for> missing opening <:for:...>:"
                                      + "\n  otx=%s @%d,%d, last ctrl is <:%s:...> \n  Following text is: %s\n"
                                      , this.otx.sIdent, lineCol[0], lineCol[1]
                                      , this.ixixCmd <0 ? "??" :  this.otx.cmds.get(this. ixCtrlCmd[this. ixixCmd]).cmd
                                      , this.sp.getCurrent(30).toString().replace('\n', '|'));
          this.sp.close();
          throw new ParseException(sError, 0);
        }
      }
      else { //No proper cmd found:
        Debugutil.stop();
      }
    }
  
  
  
    private void parseArgs() {
      do {
        if(this.sp.scanIdentifier().scanOk()) {
          String sArg = this.sp.getLastScannedString();
          this.otx.nameVariables.put(sArg, new DataAccess.IntegerIx(this.otx.nameVariables.size()));
        }
      } while(this.sp.scan(",").scanOk());
    }
    
    private void parseWr () throws ParseException {
      //====>
      String wrBufferIdent = this.sp.getLastScannedString().toString().trim();
      DataAccess.IntegerIx ixOentry = this.otx.nameVariables.get(wrBufferIdent);  //The string for data access is also used as variable indent in nameVariables
      if(ixOentry == null) { //Check whether the same entry variable exists already from another for, only ones.
        ixOentry = new DataAccess.IntegerIx(this.otx.nameVariables.size());         //create the entry variable newly.
        this.otx.nameVariables.put(wrBufferIdent, ixOentry);
      }
      WrCmd wrcmd = (WrCmd)addCmd(this.otx.pattern, this.sp.getlineCol(), this.pos0, this.pos1, ECmd.wr, wrBufferIdent);
      wrcmd.ixDataWr = ixOentry.ix;
      //wrcmd.offsWrEnd = -1;  //in case of no <:else> or following <:elsif is found.
      
    }
    
    
    private void parseIf ( ECmd ecmd) throws ParseException {
      String cond = this.sp.getLastScannedString().toString();
  //    if(cond.contains("?instanceof"))
  //      Debugutil.stop();
      //====>
      IfCmd ifcmd = (IfCmd)addCmd(this.otx.pattern, this.sp.getlineCol(), this.pos0, this.pos1, ecmd, cond);
      ifcmd.offsElsif = -1;  //in case of no <:else> or following <:elsif is found.
      
    }
    
    
    private Cmd parseExec(final String src, final int pos0, final int pos1, StringPartScanLineCol sp
        , Class<?> reflData, final Map<String, Object> idxConstData, final Map<String, OutTextPreparer> idxScript) throws ParseException {
      if(pos1 > pos0) {
        this.otx.cmds.add(new CmdString(this.otx.cmds.size(), this.sp.getlineCol(), src.substring(pos0, pos1)));
      }
      final Cmd cmd;
      int pos2 = (int)this.sp.getCurrentPosition();
      DataAccess access = new DataAccess(this.sp, this.otx.nameVariables, reflData, '\0');
      boolean bScanOk = this.sp.scan(")>").scanOk();  // this was necessary because of error in DataAccess fixed on 2024-08-30
      if(!bScanOk) {
        bScanOk = this.sp.scan(">").scanOk();         // this is the correct operation.
      }
      int pos3 = (int)this.sp.getCurrentPosition();
      String sOperation = src.substring(pos2, pos3-1);
      if(!bScanOk) {
        String sError = this.sp.getCurrent(30).toString();
        int[] lineCol = sp.getlineCol();
        this.sp.close();
        throw new ParseException("OutTextPreparer syntax error \")>\" expected in <:exec: " + this.otx.sIdent + "@" + lineCol[0] + "," + lineCol[1] + ": " + sError, lineCol[0]);
        //throw new IllegalArgumentException("OutTextPreparer "+ this.otx.sIdent + ": syntax error \")>\" expected in <:exec: " + sOperation + "...>");
      }
      cmd = new Cmd(this.otx.cmds.size(), this.sp.getlineCol(), ECmd.exec, -1, access, idxConstData, sOperation);
      return cmd;
    }  
  
    
    private void parseCall(final String src, final int pos0, final int pos1, final StringPartScanLineCol sp
        , Class<?> reflData, final Map<String, Object> idxConstData, final Map<String, OutTextPreparer> idxScript) throws ParseException {
      String sCallVar = this.sp.getLastScannedString();
      if(sCallVar.equals("otxIfColors"))
        otx.debug();
      CallCmd cmd = (CallCmd)addCmd(src, this.sp.getlineCol(), pos0, pos1, ECmd.call, sCallVar);
      final OutTextPreparer call;  
      if(cmd.dataConst !=null) { //given as const static:
        if(!(cmd.dataConst instanceof OutTextPreparer)) { //check the type on creation
          throw new ParseException("OutTextPreparer "+ this.otx.sIdent + ": <:call: " + sCallVar + " is const but not a OutTextPreparer", 0);
  //      } else { //call variable should be given dynamically:
  //        if(!this.otx.varValues.containsKey(sCallVar)) {
  //          String sError = "OutTextPreparer "+ sIdent + ": <:call: " + sCallVar + ": not given as argument";
  //          if(reflData != null) {
  //            sError += " and not found staticly in " + reflData.toString();
  //          } else {
  //            sError += "Hint: no staticly data given in first argument of ctor.";
  //          } 
  //          throw new ParseException(sError);
  //        }
        }
        call = (OutTextPreparer)cmd.dataConst; //check argument names with it.
      } else {
        call = null; //not possible to check arguments.
      }
      if(this.sp.scan(":").scanOk()) {  //-------------------------- arg following
        do {
          cmd.ixDataArg = this.otx.ctCall;
          this.otx.ctCall +=1;
          if(this.sp.scanIdentifier().scan("=").scanOk()) {
            if(cmd.args == null) { cmd.args = new ArrayList<Argument>(); }
            String sNameArg = this.sp.getLastScannedString();
            int ixCalledArg;
            if(call == null) {
              ixCalledArg = -1;
            } else {
              DataAccess.IntegerIx ixOcalledArg = call.nameVariables.get(sNameArg);
              if(ixOcalledArg == null) {
                throw new ParseException("OutTextPreparer "+ this.otx.sIdent + ": <:call: " + sCallVar + ":argument not found: " + sNameArg, 0);
              }
              ixCalledArg = ixOcalledArg.ix;
            }
            Argument arg;
            try {
              if(this.sp.scanLiteral("''\\", -1).scanOk()) {
                String sText = this.sp.getLastScannedString().trim();
                arg = new Argument(sNameArg, ixCalledArg, sText, null, sText);
              }
              else if(this.sp.scanToAnyChar(">,", '\\', '"', '"').scanOk()) {
                String sDataPath = this.sp.getLastScannedString().trim();    //this may be a more complex expression.
                arg = new Argument(this.otx, sNameArg, ixCalledArg, sDataPath, reflData, idxConstData);   //maybe an expression to calculate the value or a simple access
              }
              else { 
                throw new ParseException("OutTextPreparer "+ this.otx.sIdent + ": syntax error for argument value in <:call: " + sCallVar + ":arguments>", 0);
              }
            } catch(Exception exc) {
              throw new ParseException("Any unexpected Exception: "+ exc.getMessage(), 0); //"OutTextPreparer " + sIdent + ", argument: " + sNameArg + " not existing: ",);
            }
            cmd.args.add(arg);
          } else {
            throw new ParseException("OutTextPreparer "+ this.otx.sIdent + ": syntax error for arguments in <:call: " + sCallVar + ":arguments>", 0);
          }
        } while(this.sp.scan(",").scanOk());
      }
      if(!this.sp.scan(">").scanOk()) {
        throw new ParseException("OutTextPreparer "+ this.otx.sIdent + ": syntax error \">\" expected in <:call: " + sCallVar + "...>", 0);
      }
      
    }
    
    
    private void addError(String sError, int[] linecol) {
      this.otx.cmds.add(new CmdString(this.otx.cmds.size(), linecol, " <?? " + sError + "??> "));
    }
   
    
    
    
    /**Common addCmd with given Cmd instance (may be derived). Usable for all ...
     * @param src the pattern or specific String
     * @param from position of text before this cmd from pattern, or from specific String
     * @param to end text after from.
     * @param cmd the intrinsic {@link Cmd} to add. Can be null, then not added.
     */
    private void addCmd ( String src, int[] linecol, int from, int to, Cmd cmd) {
      if(to > from) {
        this.otx.cmds.add(new CmdString(this.otx.cmds.size(), linecol, src.substring(from, to)));
      }
      if(cmd !=null) {
        this.otx.cmds.add(cmd);
      }
    }
  
    
    
    /**Called if a simple <code>&lt;&name></code> is detected.
     * It is searched firstly in {@link #idxConstData}, then in {@link #nameVariables}
     * and at least in the execClass via Datapath access.
     * If not found then <code><??sName??></code> is output on runtime.
     * It adds the plain text if necessary and the data access calling {@link Cmd#Cmd(ECmd, int, DataAccess, Object, String)}
     * to the list of commands.
     * 
     * @param src The text before to output the plain text before
     * @param from range in src
     * @param to to > from, then output the plain text
     * @param eCmd The {@link ECmd} for the Cmd 
     * @param sName name of the argument or variable or field.
     * @param execClass to search in a data field.
     */
    private void addCmdSimpleVar ( String src, int[] linecol, int from, int to, ECmd eCmd, String sName) {
      if(to > from) {
        this.otx.cmds.add(new CmdString(this.otx.cmds.size(), linecol, src.substring(from, to)));
      }
      Object data = this.idxConstData ==null ? null : this.idxConstData.get(sName);
      if(data != null) {                                     // given const data on construction
        this.otx.cmds.add(new Cmd(this.otx.cmds.size(), linecol, eCmd, -1, null, data, sName));
      } else {
        DataAccess.IntegerIx ix = this.otx.nameVariables.get(sName);
        if(ix !=null) {                                      // Index to the arguments
          this.otx.cmds.add(new Cmd(this.otx.cmds.size(), linecol, eCmd, ix.ix, null, null, sName));
        } else {
          try {                                              // Only access the execClass is possible
            data = DataAccess.access(sName, this.execClass, true, false, false, null);  //TODO variable data?
            this.otx.cmds.add(new Cmd(this.otx.cmds.size(), linecol, eCmd, -1, null, data, sName));
          } catch (Exception exc) {
            addError(sName, linecol);                                 // inserts <??sName??> in the text
          }
        }
      }
    }
    
    
    
    
    /**Called if a simple <code>&lt;&name></code> is detected.
     * It is searched firstly in {@link #idxConstData}, then in {@link #nameVariables}
     * and at least in the execClass via Datapath access.
     * If not found then <code><??sName??></code> is output on runtime.
     * It adds the plain text if necessary and the data access as {@link Cmd#Cmd(ECmd, int, DataAccess, Object, String)}
     * to the list of commands.
     * 
     * @param src The text before to output the plain text before
     * @param from range in src
     * @param to to > from, then output the plain text
     * @param eCmd The {@link ECmd} for the Cmd 
     * @param sName name of the argument or variable or field.
     * @param execClass to search in a data field.
     */
    private void addCmdValueAccess ( String src, int[] linecol, int from, int to, String sDatapath
        , String sFormat, Class<?> execClass, Map<String, Object> idxConstData) {
      if(to > from) {
        this.otx.cmds.add(new CmdString(this.otx.cmds.size(), linecol, src.substring(from, to)));
      }
  //    int posSep = sDatapath.indexOf('.');                 // commented, this is done in CalculatorExpr.Operand(...)
  //    if(posSep <0) { posSep = sDatapath.length(); }
  //    String startVariable = sDatapath.substring(0, posSep); // first expression part is start or only variable
  //    DataAccess.IntegerIx ixAccess = this.otx.nameVariables.get(startVariable);  //access to variables (args)
  //    int ix = ixAccess == null ? -1 : ixAccess.ix;          // ix >0: access to variables, often used
      try { 
        Cmd cmd = new ValueCmd(this.otx.cmds.size(), linecol, this.otx, sDatapath, sFormat, execClass, idxConstData);
        this.otx.cmds.add(cmd);
      } catch (Exception exc) {
        addError(sDatapath, linecol);                                 // inserts <??sDatapath??> in the text
      }
    }
    
    
    
    
    /**Explore the sDatapath and adds the proper Cmd
     * @param src The pattern to get text contents
     * @param from start position for text content
     * @param to end position for text content, if <= from then no text content is stored (both = 0)
     * @param ecmd The cmd kind
     * @param sDatapath null or a textual data path. It will be split to {@link Cmd#ixValue} and {@link Cmd#dataAccess}
     * @param reflData class to main data, usable for static operations, also otxScript for call
     * @param idxConstData container of constant data for compile (parse) time
     * @param idxScript can contain some other OutTextPreparer instances for call.
     * @return the created Cmd for further parameters.
     * @throws ParseException 
     */
    private Cmd addCmd ( String src, int[] linecol, int from, int to, ECmd ecmd, String sDatapath ) throws ParseException {
      if(to > from) {
        String sText = src.substring(from, to);
//        if( this.bLastWasValueAccess                     // cannot be distinguish ... 
//         || StringFunctions.indexAfterAnyChar(sText, 0, -1, " ") < (to - from)) {                     // add the whole text, not trimmed, if it contains other than spaces.
          this.otx.cmds.add(new CmdString(this.otx.cmds.size(), linecol, src.substring(from, to)));
//        } else {
//          Debugutil.stop();  // only spaces.
//        }
      }
      final Cmd cmd;
      if(ecmd !=ECmd.nothing) {
        try {
          switch(ecmd) {
            case ifCtrl: case elsifCtrl: cmd = new IfCmd(this.otx.cmds.size(), linecol, this.otx, ecmd, sDatapath, this.execClass); break;
            case call: 
              Object oOtxSub = null;
              String sNameSub = sDatapath;
              //Field[] fields = data.getDeclaredFields();  // only debug
              if(this.idxScript !=null) { oOtxSub = this.idxScript.get(sNameSub); }  // search the sub script
              if(oOtxSub == null && this.idxConstData !=null) { oOtxSub = this.idxConstData.get(sNameSub); } //sub script given in const data
              if(oOtxSub == null && this.execClass !=null) {       // sub script in the given class
                Field otxField = this.execClass.getDeclaredField(sNameSub); // get also private fields.
                otxField.setAccessible(true);                     // access the private field with get()
                oOtxSub = otxField.get(null);                     // get the static field (null = without instance).
              }
              if(oOtxSub == null || ! (oOtxSub instanceof OutTextPreparer)) {
                throw new ParseException("subroutine: not found: " + sNameSub, 0);
              }
              cmd = new CallCmd(this.otx.cmds.size(), linecol, (OutTextPreparer)oOtxSub); //this, sDatapath, data); 
              //cmd = new CallCmd(this, sDatapath, data); 
              break;
            case exec: { //cmd = new ExecCmd(this, sDatapath, reflData, idxConstData); 
              cmd = parseExec(src, from, to, new StringPartScanLineCol(sDatapath), this.execClass, idxConstData, idxScript);
              break;
            }
            case forCtrl: {
              int posIx = sDatapath.indexOf("..");
              String sDatapath1 = sDatapath;
              if(posIx >=0) {
                //TODO scan the start index, yet always 0..
                sDatapath1 = sDatapath.substring(posIx+2);    // right side
              }
              cmd = new ForCmd(this.otx.cmds.size(), linecol, this.otx, sDatapath1, this.execClass); 
              if(posIx >=0) {
                ((ForCmd)cmd).ixStart = 0;  // overwrite -1 with the start index, then index for.
              }
            } break;
            case wr: cmd = new WrCmd(this.otx.cmds.size(), linecol, this.otx, sDatapath, this.execClass); break;
            case setVar: cmd = new SetCmd(this.otx.cmds.size(), linecol, this.otx, sDatapath, this.execClass); break;
            case debug: cmd = new DebugCmd(this.otx.cmds.size(), linecol, this.otx, sDatapath, this.execClass, idxConstData); break;
            case addString: cmd = new CmdString(this.otx.cmds.size(), linecol, sDatapath); break;
            default: cmd = new Cmd(this.otx.cmds.size(), linecol, this.otx, ecmd, sDatapath, this.execClass, null); break;
          }
        } catch(Exception exc) {
          throw new ParseException("OutTextPreparer " + this.otx.sIdent + ", variable or path: " + sDatapath + " error: " + exc.getMessage(), 0);
        }
        this.otx.cmds.add(cmd);
      } else {
        cmd = null;
      }
      return cmd;
    }
  

  }


  /**Returns an proper instance for argument data for a {@link #exec(Appendable, DataTextPreparer)} run.
   * The arguments should be filled using {@link DataTextPreparer#setArgument(String, Object)} by name 
   * or {@link DataTextPreparer#setArgument(int, Object)} by index if the order of arguments are known.
   * <br><br>
   * The returned argument instance can be reused in the same thread by filling arguments newly for subsequently usage.
   * If the {@link #exec(Appendable, DataTextPreparer)} is concurrently executed (multiple threads, 
   * multicore processing), then any thread should have its own data.
   * Also if it is nested used, of course the nested usage needs its own instance.
   * <br><br>
   * Important: this must be ready to use, {@link #parse(Class, Map, Map)} should be successfully called before.
   * @return the argument instance. 
   */
  public DataTextPreparer createArgumentDataObj() { return new DataTextPreparer(this); }
  
  
  /**Creates the argument data and presets the given execInstance to {@link DataTextPreparer#setExecObj(Object)}
   * @param execInstance It should be an instance of the given reflection class on ctor. 
   * @return Data
   */
  public DataTextPreparer getArgumentData(Object execInstance) { 
    DataTextPreparer data = new DataTextPreparer(this);
    data.setExecObj(execInstance);
    return data;
  }
  
  
  
  
  
  /**Compatible variant see {@link #execLineCt(WriteDst, DataTextPreparer)}
   * It is for compatibility and simplifies usage.
   * @param wr If it is an instance of {@link WriteDst} it is taken and calls {@link #execLineCt(WriteDst, DataTextPreparer)}.
   *   Else a new instance {@link WriteDst#WriteDst(Appendable, int)} is created started with lineCt = 1
   * @param args see {@link #execLineCt(WriteDst, DataTextPreparer)}
   * @throws IOException
   */
  public void exec( Appendable wr, DataTextPreparer args) throws IOException {
    if(wr instanceof WriteDst) {
      WriteDst wr1 = (WriteDst) wr;
      execLineCt(wr1, args);
    } else {
      WriteDst wrCt = new WriteDst(wr, 1);       // simple instance with the Appendable
      execLineCt(wrCt, args);
    }
  }

  
  

  /**Executes preparation of a pattern with given data.
   * Note: The instance data of this are not changed. The instance can be used 
   * in several threads or multicore processing.
   * <br><br>
   * Note: it is possible that this operation is recursively called with the same or different instances of this,
   * because in script-called operation this operation can be also used for detail outputs.
   * Writing to the same output channel is successive and possible. 
   * @param wr The output channel. If it is a {@link Flushable} then after this operation flush() is called.
   *   This allows view ouutput on file level. flush() is also possible to call inside from user level
   *   in any called sub operations where the wr instance is known, simple using this.
   *   <br>If the text generation should be done so fast as possible
   *   then wr can be especially a StringBuilder, writing to RAM,
   *   which may be used afterwards (write to a file afterwards). In this case file access times are saved.
   * @param args for preparation. 
   *   The value instance should be gotten with {@link OutTextPreparer#createArgumentDataObj()}
   *   proper to the instance of this, because the order of arguments should be match. 
   *   It is internally tested. 
   * @throws Exception 
   */
  public void execLineCt( WriteDst wrCt, DataTextPreparer args) throws IOException {
    if(args.prep != this) {
      throw new IllegalArgumentException("OutTextPreparer mismatch: The data does not match to the script.");
    }
    execSub(wrCt, args, 0, this.cmds.size());
    wrCt.finishAppend();                        // finish a possible existing append content from before.
    if(wrCt.wr instanceof Flushable) {
      ((Flushable)wrCt.wr).flush();             // interesting to see what's written in debug
    }
  }
  
  
  /**Executes preparation for a range of cmd for internal control structures
   * @param wr The output channel
   * @param args for preparation.
   * @param ixStart from this cmd in {@link #cmds} 
   * @throws IOException 
   */
  private void execSub( WriteDst wdArg, DataTextPreparer args, int ixStart, int ixEndExcl ) throws IOException {
    //int ixVal = 0;
    //if(this.sIdent.equals("StateTrans_OFB")) Debugutil.stopp();
    int ixCmd = ixStart;
    WriteDst wrCt = wdArg;
    assert(wrCt !=null);
    WriteDst wdBack = wdArg;  
    args.args[this.ixOUT] = wrCt;                            // variable "OUT" is the output writer, always stored here as OUT
    args.args[this.ixOUT+2] = args;                        // variable "OTXdata" is args itself, for debug info
    args.args[this.ixOUT+3] = this;                        // variable "OTX" is this itself, for debug info
    Cmd cmd;
    if(args.argsByName == null) {
      args.argsByName = new TreeMap<>();
      for(Map.Entry<String, DataAccess.IntegerIx> e : this.nameVariables.entrySet()) {
        int ix = e.getValue().ix;
        String name = e.getKey();
        Object value = args.args[ix];
        args.argsByName.put(name, value);
      }
    }
    while(ixCmd < ixEndExcl) {
      if(args.debugOtx !=null && args.debugOtx.equals(this.sIdent) && args.debugIxCmd == ixCmd)
        debug();
      cmd = this.cmds.get(ixCmd++);
      args.args[this.ixOUT+1] = cmd;                       // variable "OTXCMD" is the current cmd
      boolean bDataOk = true;
      if(bDataOk) {    //==================================== second execute the cmd with the data
        switch(cmd.cmd) {
          case addString: wrCt.append(cmd.textOrVar); break;
          case addVar: {                                   // <&access...>
            //Integer ixVar = varValues.get(cmd.str);
            Object data = dataForCmd(cmd, args, wrCt);
            if(data != null) {                   //--------vv call of void operation delivers null, no output then. 
              String sData = data == null ? "" : data.toString();
              assert(sData !=null);
              if(wrCt !=null && sData !=null) wrCt.append(sData); else System.err.println("xxxx");
            }
          } break;
          case setVar: {
            int ixVar = ((SetCmd)cmd).ixVariable;
            Object res =  dataForCmd(cmd, args, wrCt);
            if(res instanceof CalculatorExpr.Value) {
              CalculatorExpr.Value resExpr = (CalculatorExpr.Value) res;
              args.args[ ixVar ] = resExpr.objValue();
            } else {
              args.args[ ixVar ] = res;
            }
            String name = this.nameVariablesByIx[ixVar];
            args.argsByName.put(name, args.args[ixVar]);
          } break;
          case typeCheck: if(args.bChecks){
            TypeCmd cmdt = (TypeCmd)cmd;
            Object data = dataForCmd(cmd, args, wrCt);
            boolean bOk = cmdt.type.isInstance(data);
            if(!bOk) {
              Class<?> typefound = data.getClass();
              wrCt.wr.append("<?? typecheck fails, " + cmdt.textOrVar + " is type of " + typefound.getCanonicalName() + " ??>");
            }
          } break;
          case addLinenr: {
            wrCt.wr.append("#" + wrCt.lineCt());
          } break;
          case elsifCtrl:
          case ifCtrl: {
            Object data = dataForCmd(cmd, args, wrCt);
            ixCmd = execIf(wrCt, (IfCmd)cmd, ixCmd, data, args);
          } break;
          case elseCtrl: break;  //if <:else> is found in queue of <:if>...<:elseif> ...<:else> next statements are executed.
          case forCtrl: {
            if(((ForCmd)cmd).ixStart ==0)
              Debugutil.stop();
            Object data = dataForCmd(cmd, args, wrCt);
            execFor(wrCt, (ForCmd)cmd, ixCmd, data, args);;
            ixCmd += cmd.offsEndCtrl -1;  //continue after <.for>
          } break;
          case wr: {               //======================== replace the current output
            int ixWrBuffer = ((WrCmd)cmd).ixDataWr;
            //if(args.args[ixWrBuffer] == null) {  //---------- first get the write buffer
              args.args[ixWrBuffer] = dataForCmd(cmd, args, wrCt);
            //}
            if(args.args[ixWrBuffer] == null || !(args.args[ixWrBuffer] instanceof Appendable)) {
              wrCt.wr.append("<??:wr:buffer not found or faulty: ??>");
              ixCmd += ((WrCmd)cmd).offsEndCtrl -1;
            } else {
              Object wo = args.args[ixWrBuffer];
              if(wo instanceof WriteDst) {
                wrCt = (WriteDst)wo;
              } else {
                wrCt = new WriteDst((Appendable)wo, 1);
              }
              args.args[this.ixOUT] = wrCt;      // replace the current output
            }
          } break;
          case wrEnd: {
            args.args[this.ixOUT] = wrCt = wdBack;        // restore the current output 
          } break;
          case exec: {
            //ExecCmd ecmd = (ExecCmd)cmd;
            DataAccess.DatapathElement dataAccess1 = cmd.dataAccess.dataPath1();
            try {
              DataAccess.access(dataAccess1, args.execObj, true, false, null, args.args, false, null); 
            } catch (Exception exc) {
              wrCt.wr.append("<?? OutTextPreparer script " + this.sIdent + "<exec:" + cmd.textOrVar + ": execution exception " + exc.getMessage() + "??>");
            } 
          } break;
          case call: {
            Object data = dataForCmd(cmd, args, wrCt);
            if(data == null) {
              wrCt.wr.append("<?? OutTextPreparer script " + this.sIdent + "<call:" + cmd.textOrVar + ": variable not found, not given??>");
            }
            if(!(data instanceof OutTextPreparer)) {
              wrCt.wr.append("<?? OutTextPreparer script " + this.sIdent + "<call:" + cmd.textOrVar + ":  variable is not an OutTextPreparer ??>");
            } else {
              execCall(wrCt, (CallCmd)cmd, args, (OutTextPreparer)data);
            } 
          } break;
          case debug: {
            if(((DebugCmd)cmd).cmpString ==null || dataForCmd(cmd, args, wrCt).toString().equals(((DebugCmd)cmd).cmpString)){
              debug();
            }
          } break;
        default:
          break;
        }
      } else { //data error
        if(cmd.offsEndCtrl >0) {
          ixCmd += cmd.offsEndCtrl +1;
        }
      }
    }
  }
  

  
  private Object dataForCmd ( Cmd cmd, DataTextPreparer args, WriteDst wrCt ) throws IOException {
    @SuppressWarnings("unused") boolean bDataOk = true;
    Object data;  //========================================= first gather the data
    if(args.logExec !=null) { args.logExec.append(" " + cmd.linecol[0]); } 
    if(cmd.expr !=null) {
      try {                                                // only one time, set the destination data for calc
        //if(args.calcExprData == null) { args.calcExprData = new CalculatorExpr.Data(); }
        //======>>>>
        data = cmd.expr.calcDataAccess(null, args.args);
      } catch (Exception e) {
        bDataOk = false;
        data = null;
        wrCt.wr.append("<??OutTextPreparer script >>" + this.sIdent + "<<: >>" + cmd.textOrVar + "<< execution error: " + e.getMessage() + "??>");
      }
    }
    else if(cmd.ixValue >=0) { //-------------------------- any index to the arguments or local arguments
      data = execDataAccess(wrCt, cmd, args); 
    } 
    else if(cmd.cmd != ECmd.exec && cmd.dataAccess !=null) {
      try {
        if(cmd.dataAccess.datapath().size()>3)
          Debugutil.stop();
        //======>>>>         execObj for given refl, args.args for given ixData
        data = cmd.dataAccess.access(args.execObj, true, false, this.nameVariables, args.args);
      } catch (Exception exc) {
        bDataOk = false;
        if(args.logExec !=null) { args.logExec.append(" Exception dataAccess: ").append(this.sIdent).append(':').append(cmd.toString()); }
        CharSequence sMsg = ExcUtil.exceptionInfo("", exc, 1, 10);
        data = "<??>";
        wrCt.wr.append("<??OutTextPreparer variable error: '" + this.sIdent + ":" + cmd.toString() + "'" + sMsg + "\" ??>");
      }
      
    }
    else if(cmd.dataConst !=null){ 
      data = cmd.dataConst;  //may be given or null 
    } 
    else {
      data = cmd.textOrVar; //maybe null
    }
    if( data !=null && cmd instanceof ValueCmd) {          // check whether to format (@since 2023-12)
      String sFormat = ((ValueCmd)cmd).sFormat;
      if(sFormat !=null) {
        try {
          Object data1 = data;
          args.sbFormatted.setLength(0);
          args.formatter.format(sFormat, data1);
          data = args.sbFormatted;
        } catch(Exception exc) {
          data += "??Format error "+ exc.getMessage() + "??";
        }
      }

    }
    return data;
  }
  
  
  /**Accesses the data
   * @param wr only used on exception to write an elaborately info
   * @param cmd contains {@link Cmd#ixValue} for first variable 
   *  and possible {@link Cmd#dataAccess} for deeper values.
   *  If it is instanceof {@link ValueCmd} then {@link ValueCmd#sFormat} is used to format numeric and time values if given
   * @param args dynamic data for access
   * @return The accessed object, maybe null.
   * @throws IOException
   */
  private Object execDataAccess ( Appendable wr, Cmd cmd, DataTextPreparer args ) throws IOException {
    Object data0 = cmd.ixValue <0 ? null: args.args[cmd.ixValue];
    Object data;
    if(cmd.dataAccess !=null) {
      if(args.logExec !=null) { args.logExec.append(" " + cmd.linecol[0]); } 
//      String sDataAccess = cmd.dataAccess.toString();
//      if(sDataAccess.contains("Dtype")) {
//        Debugutil.stop();
//      }
      try {
        //====>                     // base on a given variable as args or static
        data = cmd.dataAccess.access(data0, true, false, this.nameVariables, args.args);
        if(data == null) {          // data == null possible if any reference is null on access.
          Debugutil.stop();
        }
      } catch (Exception e) {
        data = null;
        wr.append("<??OutTextPreparer dataAccess error: '" + this.sIdent + ":" + cmd.toString() + "'" + e.getMessage() + "\" ??>");
        if(wr instanceof Flushable) { 
          ((Flushable)wr).flush(); 
        }
      }
    } else {
      data = data0;
    }
    return data;
  }
  
  
  /**Executes a if branch
   * @param wr the output channel
   * @param cmd The ForCmd
   * @param ixCmd the index of the cmd in {@link #cmds}
   * @param container The container argument
   * @param args actual args of the calling level
   * @throws Exception
   */
  private int execIf(WriteDst wrCt, IfCmd ifcmd, int ixCmd, Object data, DataTextPreparer args) throws IOException {
    boolean bIf;
    if(ifcmd.cmd == ECmd.elsifCtrl)
      Debugutil.stop();
    if (data !=null) { 
      if( data instanceof CalculatorExpr.Value) { bIf = ((CalculatorExpr.Value)data).booleanValue(); }
      else if( data instanceof Boolean ) { bIf = ((Boolean)data).booleanValue(); }
      else if( data instanceof Number ) { bIf =  ((Number)data).intValue() != 0; }
      else { bIf = true; } //other data, !=null is true already. 
    } else {
      bIf = false;
    }
    if( bIf) { //execute if branch
      execSub(wrCt, args, ixCmd, ixCmd + ifcmd.offsElsif -1);
      return ixCmd + ifcmd.offsEndCtrl -1;  //continue after <.if>
    } else {
      //forward inside if to the next <:elsif or <:else
      return ixCmd + ifcmd.offsElsif -1;  //if is false, go to <:elsif, <:else or <.if.
    }
  }
  
  
  
  /**Executes preparation for a for cmd for internal control structures
   * complete the names of the for variables, only used for debug
   * @param wr The output channel
   * @param args for preparation.
   * @param ixStart from this cmd in {@link #cmds} 
   * @throws IOException 
   */
  private void execSubFor( WriteDst wdArg, DataTextPreparer args, ForCmd cmd, int ixStart, int ixEndExcl ) throws IOException {
    int ix = cmd.ixEntryKey;
    String name = this.nameVariablesByIx[ix];
    args.argsByName.put(name, args.args[ix]);
    ix = cmd.ixEntryVar;
    name = this.nameVariablesByIx[ix];
    args.argsByName.put(name, args.args[ix]);
    ix = cmd.ixEntryVarNext;
    name = this.nameVariablesByIx[ix];
    args.argsByName.put(name, args.args[ix]);
    execSub(wdArg, args, ixStart, ixEndExcl);
  }
  
  
  /**Executes a for loop
   * @param wr the output channel
   * @param cmd The ForCmd
   * @param ixCmd the index of the cmd in {@link #cmds}
   * @param container The container argument
   * @param args actual args of the calling level
   * @throws Exception
   */
  private void execFor(WriteDst wrCt, ForCmd cmd, int ixCmd, Object container, DataTextPreparer args) throws IOException {
    if(args.logExec !=null) { args.logExec.append(" " + this.sIdent + ":for@" + cmd.linecol[0]); } 
    if(container == null) {
      //do nothing, no for
    }
    else if(container instanceof Object[]) {
      Object[] array = (Object[]) container;
      boolean bFirst = true;
      args.args[cmd.ixEntryKey] = null;
      for(int ix = 0; ix < array.length; ++ix) {
        args.args[cmd.ixEntryVar] = args.args[cmd.ixEntryVarNext];
        args.args[cmd.ixEntryVarNext] = array[ix];
        if(bFirst) {
          bFirst = false;                     // first step only fills [cmd.ixEntryVarNext] 
        } else { //start on 2. item
          execSubFor(wrCt, args, cmd, ixCmd, ixCmd + cmd.offsEndCtrl -1);
        }
      }
      if(!bFirst) {  //-------------------------------------- if the container is not empty, 
        args.args[cmd.ixEntryVar] = args.args[cmd.ixEntryVarNext];
        args.args[cmd.ixEntryVarNext] = null;              // execute the last or only one element.
        execSubFor(wrCt, args, cmd, ixCmd, ixCmd + cmd.offsEndCtrl -1);
      }
    }
    else if(container instanceof int[]) {
      int[] array = (int[]) container;
      boolean bFirst = true;
      args.args[cmd.ixEntryKey] = null;
      for(int ix = 0; ix < array.length; ++ix) {
        args.args[cmd.ixEntryVar] = args.args[cmd.ixEntryVarNext];
        args.args[cmd.ixEntryVarNext] = array[ix];
        if(bFirst) {
          bFirst = false;                     // first step only fills [cmd.ixEntryVarNext] 
        } else { //start on 2. item
          execSubFor(wrCt, args, cmd, ixCmd, ixCmd + cmd.offsEndCtrl -1);
        }
      }
      if(!bFirst) {  //-------------------------------------- if the container is not empty, 
        args.args[cmd.ixEntryVar] = args.args[cmd.ixEntryVarNext];
        args.args[cmd.ixEntryVarNext] = null;              // execute the last or only one element.
        execSubFor(wrCt, args, cmd, ixCmd, ixCmd + cmd.offsEndCtrl -1);
      }
    }
    else if(container instanceof Iterable) {
      boolean bFirst = true;
      args.args[cmd.ixEntryKey] = null;
      if(container instanceof LinkedListLock) {
        ((LinkedListLock<?>)container).lock();
      }
      for(Object item: (Iterable<?>)container) {
        args.args[cmd.ixEntryVar] = args.args[cmd.ixEntryVarNext];
        args.args[cmd.ixEntryVarNext] = item;
        if(bFirst) {
          bFirst = false;
        } else { //start on 2. item
          execSubFor(wrCt, args, cmd, ixCmd, ixCmd + cmd.offsEndCtrl -1);
        }
      }
      if(container instanceof LinkedListLock) {
        ((LinkedListLock<?>)container).unlock();
      }
      if(!bFirst) {  //-------------------------------------- if the container is not empty, 
        args.args[cmd.ixEntryVar] = args.args[cmd.ixEntryVarNext];
        args.args[cmd.ixEntryVarNext] = null;              // execute the last or only one element.
        execSubFor(wrCt, args, cmd, ixCmd, ixCmd + cmd.offsEndCtrl -1);
      }
    }
    else if(container instanceof Map) {
      @SuppressWarnings("unchecked") Map<Object, Object>map = ((Map<Object,Object>)container);
      boolean bFirst = true;
      Object key = null;
      for(Map.Entry<Object,Object> item: map.entrySet()) {
        args.args[cmd.ixEntryKey] = key;
        key = item.getKey();
        args.args[cmd.ixEntryVar] = args.args[cmd.ixEntryVarNext];
        args.args[cmd.ixEntryVarNext] = item.getValue();   // buffer always 2 elements, sometimes necessary
        if(bFirst) {
          bFirst = false;
        } else { //start on 2. item
          execSubFor(wrCt, args, cmd, ixCmd, ixCmd + cmd.offsEndCtrl -1);
        }
      }
      if(!bFirst) {  //-------------------------------------- if the container is not empty, 
        args.args[cmd.ixEntryKey] = key;                   // execute the last or only one element.
        args.args[cmd.ixEntryVar] = args.args[cmd.ixEntryVarNext];
        args.args[cmd.ixEntryVarNext] = null;              // execute for the last argument.
        execSubFor(wrCt, args, cmd, ixCmd, ixCmd + cmd.offsEndCtrl -1);
      }
    }
    else if(container instanceof Integer) { //--------------- numeric for(int ix=0; ix < container, ++ix)
      int ixExcl = (Integer)container;
      boolean bFirst = true;
      args.args[cmd.ixEntryKey] = null;
      for(int ix = 0; ix < ixExcl; ++ix) {
        args.args[cmd.ixEntryVar] = ix-1;
        args.args[cmd.ixEntryVarNext] = ix;
        if(bFirst) {
          bFirst = false;                     // first step only fills [cmd.ixEntryVarNext] 
        } else { //start on 2. item
          execSubFor(wrCt, args, cmd, ixCmd, ixCmd + cmd.offsEndCtrl -1);
        }
      }
      if(!bFirst) {  //-------------------------------------- if the container is not empty, 
        args.args[cmd.ixEntryVar] = ixExcl-1;
        args.args[cmd.ixEntryVarNext] = null;              // execute the last or only one element.
        execSubFor(wrCt, args, cmd, ixCmd, ixCmd + cmd.offsEndCtrl -1);
      }
    }
    else {
      wrCt.wr.append("<?? OutTextPreparer script " + this.sIdent + ": for variable is not an container: " + cmd.textOrVar + "??>");
    }
  }
    
    
    
    
  /**Executes a call
   * @param wr the output channel
   * @param cmd The CallCmd
   * @param args actual args of the calling level
   * @param callVar The OutTextPreparer which is called here.
   * @throws Exception
   */
  private void execCall(WriteDst wrCt, CallCmd cmd, DataTextPreparer args, OutTextPreparer callVar) throws IOException {
    @SuppressWarnings("cast") OutTextPreparer callVar1 = (OutTextPreparer)callVar;
    DataTextPreparer valSub;
    if(args.logExec !=null) { args.logExec.append(" :call:" + callVar1.sIdent + "@" + cmd.linecol[0]); } 
    if(cmd.args !=null) {
      valSub = args.argSub[cmd.ixDataArg];
      if(valSub == null) { //only first time for this call
        args.argSub[cmd.ixDataArg] = valSub = callVar1.createArgumentDataObj();  //create a data instance for arguments.
        valSub.debugIxCmd = args.debugIxCmd;
        valSub.debugOtx = args.debugOtx;
        valSub.execObj = args.execObj;  //2023-12-23: bugfix for <:exec:...> in deeper <:call:...> level, the exec instance is given now.
      }
      for(Argument arg : cmd.args) {
        Object value = null;
        try{ value = arg.calc(null, args.args); }
        catch(Exception exc) { 
          wrCt.wr.append("<??OutTextPreparer call argument error: '" + this.sIdent + ":" + cmd.toString() + "'" +exc.getMessage() + "\" ??>");
        }
        if(arg.ixDst >=0) {
          valSub.setArgument(arg.ixDst, value);
        } else {
          valSub.setArgument(arg.name, value);
        }
      }
    } else {
      //<:call:name> without arguments: Use the same as calling level.
      valSub = args;
    }
    callVar1.exec(wrCt, valSub);
    if(args.logExec !=null) { args.logExec.append(" .call:" + callVar1.sIdent + "@" + cmd.linecol[0]); } 
  }
  
  
  
  
  @Override public String toString() { return this.sIdent + ":" + this.pattern; }
  
  
  void debug() { 
    Debugutil.stopp(); 
  }
  
} //class OutTextPreparer
