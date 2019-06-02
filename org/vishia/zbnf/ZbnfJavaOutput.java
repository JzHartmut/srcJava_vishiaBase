/* ***************************************************************************
 * Copyright/Copyleft:
 *
 * For this source the LGPL Lesser General Public License,
 * published by the Free Software Foundation is valid.
 * It means:
 * 1) You can use this source without any restriction for any desired purpose.
 * 2) You can redistribute copies of this source to everybody.
 * 3) Every user of this source, also the user of redistribute copies
 *    with or without payment, must accept this license for further using.
 * 4) But the LPGL ist not appropriate for a whole software product,
 *    if this source is only a part of them. It means, the user
 *    must publish this part of source,
 *    but don't need to publish the whole source of the own product.
 * 5) You can study and modify (improve) this source
 *    for own using or for redistribution, but you have to license the
 *    modified sources likewise under this LGPL Lesser General Public License.
 *    You mustn't delete this Copyright/Copyleft inscription in this source file.
 *
 * @author JcHartmut = hartmut.schorrig@vishia.de
 * @version 2006-06-15  (year-month-day)
 * list of changes:
 *
 ****************************************************************************/
package org.vishia.zbnf;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.*;
import java.text.ParseException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.vishia.mainCmd.MainCmd;
import org.vishia.mainCmd.MainCmdLoggingStream;
import org.vishia.mainCmd.MainCmdLogging_ifc;
import org.vishia.util.DataAccess;
import org.vishia.util.Debugutil;
import org.vishia.util.FileSystem;
import org.vishia.util.GetTypeToUse;
import org.vishia.util.SetLineColumn_ifc;
import org.vishia.util.StringPartFromFileLines;
import org.vishia.util.StringPartScan;




/**This class helps to convert a ZBNF-parse-result into a tree of Java objects.
 * The user have to be provide a toplevel-instance with given access type which contains 
 * public Fields or public new_-, set_- or add_-methods for any parse result. The fields or methods are find out
 * using the reflection.
 * <br><br>
 * <b>Saving the content of simple result using fields</b>: Simple results are the basicly components of ZBNF
 * such as &lt;#?any_number> or &lt;""?Astring>. Depending on the type of result a field with the appropriate name
 * is searched in the users Object of the current level, and the result content is stored there. The field should be start
 * with a lower-case letter, independent of the case of the first letter of the semantic identifier of the parse result. 
 * The type of the field should be matched to the type of the parse result: <code>String</code> for String-results,
 * and int, float, double for value results. The numeric type is flexible.
 * <br><br>
 * <b>Saving the content of result-components using fields</b>: Result-components are the complex results, which have a tree
 * of results inside. The appropriate syntax-components are declared in ZBNF writing <code>component::=...</code>.
 * If such results should be stored using fields, a field of type <code>List</code> should be exists with the semantic identifier
 * of the parse result. Than a new element of the List type will be created, that object will be filled with the
 * component's result and than add to the list.
 * <br><br>
 * <b>Saving the content of simple result using methods</b>: A method with name <code>set_Semantic(Type)</code> should be exist.
 * The <code>Semantic</code> is the semantic identifier of the parse result. The <code>Type</code> of the argument should follow 
 * the type of the parse result. Thereby an integer parse result can be associated to one of the scalar number types, 
 * a float or double result can associated to both a float or double parameter type. Any other result should be have a String
 * parameter. While accepting several parameter types, the architecture of the user methods can be adapted to the user's requirements.
 * <br><br>
 * <b>Saving the content of syntax components using methods</b>: 
 * <br><br>
 * If a <code>Type new_<i>semantic</i>()</code>-method will be found, it is invoked to get an instance 
 * for the syntax-components's content. It is possible, that this instance isn't a new instance, but it is a static allocated instance
 * which is reused temporary to hold the components data. Another way is to call a <code>new</code> operator to get a new instance.
 * The return type of the <code>Type new_<i>semantic</i>()</code>-method, or the field type of non-containers
 * or the generic-type of <code>List</code> fields is used to search fields or methods for the component.
 * The types may be defined somewhere else in the Java-package-tree, but the classes have to be be public.
 * <br><br>
 * Than the got instance is used to fill the content of the component like described above.
 * <br><br>
 * After them the appropriated method <code>void add_<i>semantic</i>(Type)</code> or <code>void set_<i>semantic</i>(Type)</code>
 * is called to store the content. One of that methods have to be exist. Both method-variants, <code>add_</code> or <code>set_</code> 
 * have the same meaning for processing the data,
 * the name considers the meaning of the users action. <i>add</i> associates adding a content to a container, where <i>set</i>
 * means a simple setting of data. The implementation of this method can be store the reference of the instance, or it can evaluate 
 * the content with special tests, copy in a special way etc. It is user-free. 
 * <br><br>
 * <br><br>
 * <b>Public fields or methods, interfaces</b>
 * <br><br>
 * All elements which should be used to store parse results have to be public, because the access to it using reflection is done.
 * Using public fields is more simple, but using public methods is better to debug and encapsulate. The methods may be overridden
 * or interface methods, the class-type which is returned by <code>new_Name(...)</code> may be a basic type or interface.
 * The implementation method is called of course. This concept enables a break of dependency between the classes, which are known
 * in context of storing a parse result and the really implementing classes. It is a application of the standard interface pattern.  
 * <br><br> 
 * <br><br> 
 * <b>Encapsulation, Safety of data while using public access methods or public fields</b> 
 * <br><br>
 * The toplevel-instance is provided internally. If it isn't able to access in a public way, it and all its child data are safety,
 * though the elements of the class are public. The access to the instance is protected. 
 * <br><br> 
 * <br><br> 
 * <b>Comparison with Apache-ANT concept to store parameters for Tasks</b> 
 * <br><br> 
 * The rules for this things are some different but with same principle likewise arguments 
 * for call of java classes extends <code>org.apache.tools.ant.Task</code> called in ant 
 * (http://ant.apache.org/manual/develop.html).
 * There are much more simple:
 * <br>
 * <ul>
 * <li>The semantic identifier is used as name for fields and methods, 
 *     named <code>name</code> in the description below. For set- and get-methods
 *     the name is used exactly like written in the ZBNF syntax script.
 *     For searching field names the first character is converted to lower case.
 * <li>For non-ZBNF-components first a <code>set_name(Type)</code>- or <code>add_name(Type)</code>-method
 *     is searched and invoked, if found. 
 * <li>The type of the methods argument is predetermined by the type of the stored result.
 *     For components, it is the component instance type. For elements, the type of elements 
 *     determine the argument type:
 * <li>If no method is found, a field <code>Type name</code> is searched. 
 *     If it is found, the type is tested.
 *     For parse result elements, the type of fields must be java.lang.String or long, int, float or double.
 *     To store instances for parse result components, the type of the field determines 
 *     the components class type.         
 * <li>java.util.List< Type> name fields for the child parse result components,</li>
 * <li>Type fields for the child parse result components if only 1 child is able.
 * <li>java.lang.String name-fields for parsed string represented results,</li>
 * <li>long, int, float, double fields for parsed numbers.</li>
 * </ul>
 * <br><br> 
 * <br><br> 
 * <b>Special functionality for storing the line and column number of the input text</b> 
 * <br><br>
 * The knowledge of the column inside a source line may be important for evaluating the result. That is for example for indented texts
 * whereby the indentation have a semantic meaning. The line and file may be important especially for some information
 * which should refer to the source.
 * <br><br>
 * The the user's class can implement {@link SetLineColumn_ifc}. Then the column position, the line, the file 
 * will be stored in the users instance. If only the column is necessary, only it will be stored. The effort to get the line
 * and the file will be higher, therefore it is not gotten if not necessary. The parser knows the column, line and file it is 
 * parse result if the input allows to detect it. To capture the line number a {@link org.vishia.util.StringPartFromFileLines}
 * have to be used for input for the ZBNF parser, because only this class overrides the {@link org.vishia.util.StringPart#getLineCt()}
 * method with a really line number. This class reads a file line per line.    
 * <br><br> 
 * There is an older form: The user class can have an element <code>public int inputLine_</code> or a method <code>set_inputLine_(int)</code>
 * to store the line number of the input text, respectively <code>public int inputColumn_</code> or a method <code>set_inputColumn_(int)</code>
 * if the input column of the parsed text should be stored. It will be checked whether that fields are existent,
 * then this information will be stored.
 * <br><br> 
 * <br><br>
 * <b>Error detecting and handling</b>
 * <br><br>
 * If a appropriate method or field isn't found, either an IllegalArgumentException is thrown
 * or the errors are collected and returned in a String. The first behavior is practicable, 
 * because mostly a false content of destination classes may be writing error of the program.
 * The variant to produce a collected error text is more usefull for debugging.    
 * 
 */
@SuppressWarnings("unchecked")
public final class ZbnfJavaOutput
{
  /**Version, history and license.
   * <ul>
   * <li>2019-05-30 Hartmut: some improvements to store <code>[<&lt;cmpnSemantic/@text></code> see documentation. 
   * <li>2019-05-21 chg: {@link #parser} is now a class variable and final public. Advantage: Accessible from outer to set some log things.
   * <li>2018-09-11 bugfix: On error reading file: The error was ignored and the parser was started, which returns null as parse result. 
   *     fix: return sError.
   * <li>2018-08-28 Hartmut new: improve change with <?_end>. add_semantic is called, it was missing, only ok in the concretely case. 
   * <li>2017-04-22 Hartmut new: If a <code>new_xyz</code> method is found for a non syntaxComponent, the return value
   *   is a child instance to write into the next result items, till <code><?_end></code>. The last one switches back to the parent again.
   * <li>2017-03-25 Hartmut new It is possible that the user provides a method <code>new_semantic(ZbnfParseResultItem zbnfItem)</code>.
   *   Then the parse result item is supplied. The application can evaluate both the {@link ZbnfParseResultItem} or the {@link ZbnfParseResultItem#syntaxItem()}
   *   for more informations. Especially the {@link ZbnfSyntaxPrescript#getAttribute(String)} can be used.
   * <li>2016-01-17 Hartmut new {@link #parseFileAndFillJavaObject(Class, Object, String, String, String, File)} with simple string given input an syntax.
   * <li>2015-12-06 Hartmut new {@link #parseFileAndFillJavaObject(Class, Object, File, StringPartScan, File)} with the directory of scripts to include. 
   * <li>2015-05-27 Hartmut improved output on method not found.
   * <li>2015-04-25 Hartmut chg: The class to search the destination is the {@link DstInstanceAndClass#clazz} 
   *   and not the getClass() of the {@link DstInstanceAndClass#instance}.  
   *   except the interface {@link GetTypeToUse} is implemented by the returned instance of a <code>new_<semantic>()</code>.
   *   This is documented now. The JZcmdScript (JbatScript) had used the property that the instance.getClass() 
   *   is the destination for overridden classes from {@link org.vishia.util.DataAccess}, therefore this treatment was used
   *   for all instances. But that is a poorly defined concept. 
   *   It is better to distinguish between the dst instance and the type where the information are searched.
   *   Only in special cases, with the marker interface {@link GetTypeToUse} an abbreviated type, usual the type of the instance
   *   can be used. 
   * <li>2015-04-25 Hartmut chg: Restructured to get file and line for the top level parse result. Gardening. Documentation
   * <li>2014-05-23 Hartmut chg: remove invocation of set_inputColumn_ and set_InputInfo_. Instead
   *   use interface {@link SetLineColumn_ifc}. It is faster, because a method which does not exists often
   *   should not be invoked. Only 'instanceof' is checked. This needs adaption of user classes
   *   which uses the method set_inputColumn_(int).
   * <li>2014-05-22 Hartmut chg: instead trySetInputLine {@link #trySetInputInfo(DstInstanceAndClass, int, int, String)}.
   *   The destination class should provide <code> boolean set_InputInfo_(int, int, String)</code> 
   * <li>2014-04-27 Hartmut new: ctor without {@link MainCmdLogging_ifc} for simple usage. 
   * <li>2014-01-01 Hartmut new: {@link #parseFileAndFillJavaObject(Object, File, String)}
   * <li>2014-01-01 Hartmut new: {@link #trySetInputLine(DstInstanceAndClass, int)}. Uses {@link DataAccess} first time.
   *   there are some parallel functionality of this class and DataAccess. The DataAccess is the more universal one. 
   * <li>2013-09-02 Hartmut chg: The {@link DstInstanceAndClass#clazz} is build from the instance instead from the
   *   given clazz Argument. It means an derived instance can contain other set_, add_method.
   * <li>2012-12-26 Hartmut new: {@link #writeInField(Field, Object, ZbnfParseResultItem)} supports a char field too,
   *   it gets the first char of a parsed string or the numeric value converted in a char (ASCII, UTF-16).
   * <li>2012-10-07 Hartmut bugfix on writing in a public List<String> getParsedString() should be used too.
   * <li>2012-10-07 Hartmut chg: error text output, able to use for {@link org.vishia.msgDispatch.MsgDispatchSystemOutErr}
   * <li>2011-09-03 Hartmut chg: {@link #writeZbnfResult(DstInstanceAndClass, ZbnfParseResultItem, int)}: check semantic, if empty, does nothing
   * <li>2010-12-03 Hartmut new: parseFileAndFillJavaObject(...String syntax), better user support for simple tasks
   * <li>2010-12-02 Hartmut fnChg: parseFileAndFillJavaObject(): no report output of sError, because it is supplied in the return value.
   * <li>2010-12-02 Hartmut new: Up to now this version variable, its description contains the version history.
   * <li>2010-01-24: Hartmut docu improved, MainCmdLogging_ifc-output before exception on error in called routine to visit the problem.
   * <li>2009-04-26: Hartmut corr: Now all float or int parse result can set a int, long, float double fields and set_method(value).
   * <li>2009-04-26: Hartmut corr: better Exception text if access a non public field as components output.
   * <li>2009-03-23: Hartmut chg: total new structuring. Functional change is: 
   * <ul><li> Now inner classes in the output classes are unnecessary,
   *   <li>a destination class is found as type of a new_semantic()-method
   *   <li>or as type of field for a component. It should be public, but anywhere in the code.
   *   <li>All other functions are compatible. 
   * </ul>  
   * <li>2009-03-08: Hartmut new: setOutputFields. It is strict, but accept fields, not only methods.
   * <li>2008-04-02: Hartmut some changes
   * <li>2006-05-15: Hartmut www.vishia.de creation
   * </ul>
   * <ul>
   * <li>new: new functionality, downward compatibility.
   * <li>fnChg: Change of functionality, no changing of formal syntax, it may be influencing the functions of user,
   *            but mostly in a positive kind. 
   * <li>chg: Change of functionality, it should be checked syntactically, re-compilation necessary.
   * <li>adap: No changing of own functionality, but adapted to a used changed module.
   * <li>corr: correction of a bug, it should be a good thing.
   * <li>bug123: correction of a tracked bug.
   * <li>nice: Only a nice correction, without changing of functionality, without changing of syntax.
   * <li>descr: Change of description of elements.
   * </ul> 
   */
  public static final String sVersion = "2018-08-28";
  
  /**Helper Instance to bundle a class to search methods or fields and the associated instance.
   * It is the destination to search elements via semantic in its {@link #clazz} and store the data in the {@link #instance}.
   */
  protected final static class DstInstanceAndClass
  { 
    /**Doc: see constructors args. */
    final Class<?> clazz;  final Object instance; final boolean shouldAdd; 
    
    final DstInstanceAndClass parentResult;
    
    final String semantic;
    
    //final Object parentInstance;
    /**All elements are final and set one time on creation.
     * @param parent    The parent destination is used on error messages.
     * @param semantic  To show for error messages
     * @param instance  The instance where the data should be stored in.
     *   If this instance implements the {@link GetTypeToUse} then that type is used to search the destination
     *   of the component's children.
     * @param clazz     The type of the reference to the instance, not the type of the instance.
     *                  That type should be used to find components fields or method.
     *                  (Note: The type of the instance may be derived, private or so on.)
     * @param shouldAdd true than the instance is to be add using an add_Semantic(Object)-method
     *                  of its parent, because it is got with a new_Semantic()-Method.
     *                  false than the instance is referenced anywhere already in the user's data.
     */
    DstInstanceAndClass(DstInstanceAndClass parent, String semantic, Object instance, Class<?> clazz
      , boolean shouldAdd)
    { this.parentResult = parent;
      this.semantic = semantic;
      this.instance = instance; this.clazz = clazz; 
      this.shouldAdd = shouldAdd; 
    }
  }



  private final MainCmdLogging_ifc report;
  
  /**If it is set, only set_ or add_-methods and new_-methods are accepted,
   * no fields and no inner classes as container.
   */
  private boolean bOnlyMethods;
  
  /**If it is set, only fields are accepted, no methods.
   * This option is better to use if fields are used only and the calculation time should be shorten.
   */
  private boolean bOnlyFields;
  
  /**If it is set, an IllegalArgumentException is thrown, if a matching field or method are not found.
   */
  private boolean bExceptionIfnotFound;
  
  
  public final ZbnfParser parser;
  
  /**Buffer to note errors during working. 
   * Its content will be returned as String-return value of {@link #setContent(Class, Object, ZbnfParseResultItem)}.
   */
  private  StringBuffer errors;
  
  /**Empty constructor. The {@link MainCmdLogging_ifc}-output will be build with a {@link MainCmdLoggingStream}
   * if the {@link MainCmd} is not used.
   * 
   */
  public ZbnfJavaOutput()
  { report = MainCmd.getLogging_ifc();
    parser = new ZbnfParser(report);
    init();
  }
  
  /**Constructor with given log output. 
   * @param report for logging the process of associated, only {@link org.vishia.mainCmd.MainCmdLogging_ifc#fineDebug} will be used.
   */
  public ZbnfJavaOutput(MainCmdLogging_ifc report)
  { this.report = report;
    parser = new ZbnfParser(report);
    init();
  }

  private ZbnfJavaOutput(MainCmdLogging_ifc report, boolean strict, boolean methods)
  { this.report = report;
    parser = new ZbnfParser(report);
    init();
    this.bOnlyMethods = methods; 
  }

  
  /**Sets the association mode: 
   * @param value true, than fields aren't searched, only methods.
   */
  public void setMethodsOnly(boolean value){ this.bOnlyMethods = value; }
  
  /**Sets the association mode: 
   * @param value true, than methods aren't searched, only fields. It may have some less calculation time.
   */
  public void setFieldsOnly(boolean value){ this.bOnlyFields = value; }
  
  /**Sets the behavior if no appropriate method or field is found for a parser result.
   * @param value true, than no exception is thrown in this case. 
   *              Instead the problem is noted in the returned String.
   *              false, than an IllegalArguementException is throw in this case. 
   *              It is the standard behavior after {@link #init()}.
   */
  public void setWeakErrors(boolean value){ this.bExceptionIfnotFound = !value; }
  
  /**Sets the default settings and clears saved errors.
   * <ul><li>searches both, fields and methods. 
   * <li>throws an IllegalArgumentException if a necessary field or method isn't found.
   * </ul>
   */
  public void init()
  { errors = null;
    bOnlyFields = false;
    bOnlyMethods = false;
    bExceptionIfnotFound = true;
  }
  
  
  /**This is the main method to set content (non-static variant).
   * @param topLevelClass The top level class, where methods and fields are searched.
   *         It is able that it is an interface type.
   * @param topLevelIntance The instance to output. It have to be <code>instanceof topLevelClass</code>.
   * @param resultItem The parsers result.
   * @return null if no error or an error string.
   * @throws IllegalArgumentException Invoked if {@link #setWeakErrors(boolean)} is set with true.
   * @throws IllegalAccessException Especially if fields and methods are non-public.
   * @throws InstantiationException Especially if problems with methods exists.
   */
  public String setContent(Class topLevelClass, Object topLevelInstance, ZbnfParseResultItem resultItem) 
  throws IllegalArgumentException, IllegalAccessException, InstantiationException
  { errors = null;
    DstInstanceAndClass dst = new DstInstanceAndClass(null, "topLevel", topLevelInstance, topLevelClass, false);
    writeZbnfResult(dst, resultItem, 1);
    return errors == null ? null : errors.toString();
  }
  
  
  
  /**Writes the parsers result into a tree of Java objects using the reflection method.
   * @param topLevelOutput The toplevel instance
   * @param resultItem The toplevel parse result
   * @param report
   * @throws IllegalAccessException if the field is found but it is not public.
   * @throws IllegalArgumentException 
   * @throws InstantiationException if a matching class is found but it can't be instanciated. 
   */
  public static void setOutputStrict(Object topLevelOutput, ZbnfParseResultItem resultItem, MainCmdLogging_ifc report) 
  throws IllegalArgumentException, IllegalAccessException, InstantiationException
  { setOutput(topLevelOutput, resultItem, report, true, true);
  }
  
  
  /**Writes the parsers result into a tree of Java objects using the reflection method.
   * @param topLevelOutput The toplevel instance
   * @param resultItem The toplevel parse result
   * @param report
   * @throws IllegalAccessException if the field is found but it is not public.
   * @throws IllegalArgumentException 
   * @throws InstantiationException if a matching class is found but it can't be instanciated. 
   */
  public static void setOutputFields(Object topLevelOutput, ZbnfParseResultItem resultItem, MainCmdLogging_ifc report) 
  throws IllegalArgumentException, IllegalAccessException, InstantiationException
  { setOutput(topLevelOutput, resultItem, report, true, false);
  }

  
  
  /**Writes the parsers result into a tree of Java objects using the reflection method.
   * @param topLevelOutput The toplevel instance
   * @param resultItem The toplevel parse result
   * @param report
   * @throws IllegalAccessException if the field is found but it is not public.
   * @throws IllegalArgumentException 
   * @throws InstantiationException if a matching class is found but it can't be instanciated. 
   */
  public static void setOutput(Object topLevelOutput, ZbnfParseResultItem resultItem, MainCmdLogging_ifc report, boolean strict, boolean bOnlyMethods) 
  throws IllegalArgumentException, IllegalAccessException, InstantiationException
  { //instance of writer only for temporary help to organize, no data are stored here: 
    ZbnfJavaOutput instance = new ZbnfJavaOutput(report, strict, bOnlyMethods);  
    //the available classes of the top level output instance
    DstInstanceAndClass dst = new DstInstanceAndClass(null, "topLevel", topLevelOutput, topLevelOutput.getClass(), false);
    instance.writeZbnfResult(dst, resultItem, 1);
  }

  /**@deprecated, use {@link #setOutputStrict(Object, ZbnfParseResultItem, MainCmdLogging_ifc)}
   * or {@link #setOutputFields(Object, ZbnfParseResultItem, MainCmdLogging_ifc)}
   * @param topLevelOutput
   * @param resultItem
   * @param report
   * @throws IllegalArgumentException
   * @throws IllegalAccessException
   * @throws InstantiationException
   */
  @SuppressWarnings("deprecation")
  public static void setOutput(Object topLevelOutput, ZbnfParseResultItem resultItem, MainCmdLogging_ifc report) 
  throws IllegalArgumentException, IllegalAccessException, InstantiationException
  { //instance of writer only for temporary help to organize, no data are stored here: 
    ZbnfJavaOutput instance = new ZbnfJavaOutput(report, false, false);  
    //the available classes of the top level output instance
    DstInstanceAndClass dst = new DstInstanceAndClass(null, "topLevel", topLevelOutput, topLevelOutput.getClass(), false);
    instance.writeZbnfResult(dst, resultItem, 1);
  }
    
  /**Writes the content of one parse result component into the given outputInstance.
   * This is the core method which is invoked for any component of the parsers result
   * starting from the toplevel result, recursively if components are processed.
   * <br><br>
   * This routine checks whether the dst instance implements the interface {@link SetLineColumn_ifc}.
   * If it does then the column, line and the file or stored with that in the dst instance.
   * <br><br>
   * All children of the given component are processed. 
   * <ul>
   * <li>If a child is a component then firstly a destination is searched referred from the given dst.
   *   This is done with the method {@link #searchComponentsDestination(String, DstInstanceAndClass)}
   *   with the given semantic of the child and the given dst (calling argument) for this component.
   *   With the child's dst this routine is invoked recursively.
   *   If the dst needs an 'add_semantic' method then {@link #searchAddMethodAndInvoke(String, DstInstanceAndClass, DstInstanceAndClass)}
   *   is called after them for the child. 
   * <li>If a child is not a component then the routine
   *   {@link #searchDestinationAndWriteResult(String, DstInstanceAndClass, ZbnfParseResultItem)}
   *   is invoked for that child to store the content.
   * </ul> 
   * 
   * @param dstArg The class and instance to add children results.
   * @param zbnfComponent The ZBNF parsers's result item which is either the top level result or any component.
   * @throws IllegalAccessException if the field is not public.
   * @throws IllegalArgumentException 
   * @throws InstantiationException if a matching class is found but it can't be instantiated. 
   */  
  protected void writeZbnfResult
  ( DstInstanceAndClass dstArg  
  , ZbnfParseResultItem zbnfComponent
  , int recursion
  ) 
  throws IllegalArgumentException, IllegalAccessException, InstantiationException
  {
    //write column, line and file into it if expected from the instance type: 
    if(dstArg.instance instanceof SetLineColumn_ifc){
      SetLineColumn_ifc check = (SetLineColumn_ifc) dstArg.instance;
      int mode = check.setLineColumnFileMode();
      final int inputLine, inputColumn;
      String inputFile;
      if((mode & SetLineColumn_ifc.mColumn) !=0){ inputColumn = zbnfComponent.getInputColumn(); } 
      else { inputColumn = -1; }
      if((mode & SetLineColumn_ifc.mLine) !=0){ inputLine = zbnfComponent.getInputLine(); } 
      else { inputLine = -1; }
      if((mode & SetLineColumn_ifc.mFile) !=0){ inputFile = zbnfComponent.getInputFile(); } 
      else { inputFile = null; }
      check.setLineColumnFile(inputLine, inputColumn, inputFile);
    }
    
    { //skip into the component resultItem:
      Iterator<ZbnfParseResultItem> iterChildren = zbnfComponent.iteratorChildren();
      DstInstanceAndClass dst = dstArg;
      //
      //This is the loop over all result items of this component.
      //
      while(iterChildren.hasNext()) { 
        ZbnfParseResultItem childItem = iterChildren.next();
        ZbnfSyntaxPrescript childSyntax = childItem.syntaxItem();
        if(  childSyntax.sDefinitionIdent !=null 
            && childSyntax.eType == ZbnfSyntaxPrescript.EType.kSyntaxComponent
            && childSyntax.sDefinitionIdent.contains("real_type_name")
            ) {
          Debugutil.stop();
        }
        if(  childSyntax.sDefinitionIdent !=null 
            && childSyntax.eType == ZbnfSyntaxPrescript.EType.kSyntaxComponent
            && childSyntax.sDefinitionIdent.contains("real_literal")
            ) {
          Debugutil.stop();
        }
        final String semantic1 = childItem.getSemantic();
        /**If the semantic is determined to store in an attribute in xml, the @ is ignored here: */
        final String semantic = semantic1.startsWith("@") ? semantic1.substring(1) : semantic1;
        if(semantic.length() >0){ ///
          if(semantic.equals("ST/@Text"))
            stop();
          report.reportln(MainCmdLogging_ifc.fineDebug, recursion, "ZbnfJavaOutput: " + semantic + ":");
          if(childItem.isComponent() 
            ) { //Try to save the content also if it is an component:
            int posSep = semantic.lastIndexOf('/');  //check whether 2-stage semantic for text storage
            String semanticCmpn = semantic;
            if(posSep >0) {
              semanticCmpn = semantic.substring(0,posSep);
            }
            //Search an instance (field or method result) which represents the semantic of the component. 
            // That instance will be used to fill the parse result of the component.
            DstInstanceAndClass dstChild = searchComponentsDestination
              ( semanticCmpn, childItem                //the semantic of the component.
              , dst    //instance where the field or method for the component should be found.
              );
            if(childItem.isOption() && childItem.getParsedString() != null)
            { if(posSep >=0) {
                //It is [<?obj/@attr>... then store in the found dstChild without @attr in the attr as String.
                //This is new since 2019, this form is not used in the past. 
                String semanticForText = semantic.substring(posSep+1);
                if(semanticForText.length() >0) { //[<?semantic/> forces only create cmpn, no text storage.
                  //Note: may start with "@...", not important here. The "@" will be removed for the name (important only for XML)
                  searchDestinationAndWriteResult(semanticForText, dstChild, childItem);
                }
              } else {
                //It is usual [<?obj>... then store the text in set_obj(String val) in the current dst.
                //It is the compatible behavior since 2008
                searchDestinationAndWriteResult(semantic, dst, childItem);
              }
            
            }
            
            if(dstChild != null) {
              //writes the sub result to the found dstChild.
              writeZbnfResult(dstChild, childItem, recursion+1);
              if(dstChild.shouldAdd)
              { searchAddMethodAndInvoke(semanticCmpn, dst, dstChild);  //add child in dst.
              }
            }
          }
          else
          { //write the content of the resultItem into the outputInstance:
            if(semantic.equals("_end")){
              if(dst.shouldAdd)
              { searchAddMethodAndInvoke(dst.semantic, dst.parentResult, dst);  //add child in dst.
              }
              dst = dst.parentResult;
            } else {
              DstInstanceAndClass dstChild = searchDestinationAndWriteResult(semantic, dst, childItem);
              if(dstChild !=null) {
                dst = dstChild;  //continue with writing to the child instance,
                //switch to the parent instance with capability of child.
              }
            }
          }
        } //semantic given
      } //while
      int ctError = 1000;
      while(dst !=dstArg && dst.parentResult !=null && --ctError >=0) {
        if(dst.shouldAdd)
        { searchAddMethodAndInvoke(dst.semantic, dst.parentResult, dst);  //add child in dst.
        }
        
      }
    }
  }
   
  
  
  
  /**Searches a method with given Name and given argument types in the class
   * or in its super-classes and interfaces.
   * @param retVariant null or int[]. retVariant[0] will be set with the variant of argTypesVariants.
   * @param outputClass
   * @param name
   * @param argTypesVariants
   * @return the method or null if it isn't found.
   */
  private Method searchMethod(int[] retVariant, Class<?> outputClass, String name, Class<?>[][] argTypesVariants)      
  { Method method;
    //System.out.println("ZbnfJavaOutput - search method; " + name + "; in class; " + outputClass.getName());
    do
    { int ixArgTypes = 0;
      do
      { Class<?>[] argTypes = argTypesVariants[ixArgTypes];
        try{ method = outputClass.getDeclaredMethod(name, argTypes);}
        catch(NoSuchMethodException exception){ 
          method = null; 
          //System.out.println("ZbnfJavaOutput - search method, not found, try argument types; " + name + "; in class; " + outputClass.getName());
        }
        if(method !=null && retVariant !=null) {
          retVariant[0] = ixArgTypes;
        }
      } 
      while(  method == null               //not found 
           && ++ixArgTypes < argTypesVariants.length  //but there are some variants to test.
           );
       
      /**Not found: if there is a superclass or (TODO)super-interface:  */
      if(method == null)
      { outputClass = outputClass.getSuperclass();
        if(outputClass == Object.class)
        { outputClass = null; 
        } else {
          //System.out.println("ZbnfJavaOutput - search method in superclass; " + name + "; in class; " + outputClass.getName());
        }
      }
    } while(  method == null       //not found 
           && outputClass != null);  //but there are superclassed
    return method;
  }  

  
  
  
  /**Searches a method with signature <code>new_<semantic>()</code>.
   * @param inClazz class type where the method will be searched. The method won't be searched in super classes here.
   *   Searching in super classes is organized in the routine {@link #searchComponentsDestination(String, DstInstanceAndClass)}
   *   where this method is called.  
   * @param instance with them the found method will be invoked.
   * @param semantic name
   * @param parent only used for parent in the return instance.
   * @return null if the method is not found. Elsewhere the destination for the component to store, which's instance 
   *   was created by the new_ method. The {@link DstInstanceAndClass#clazz} is the return type of the method.
   *   That type is used to find out destinations for children of the component. It means the instance may be an derived type,
   *   the special properties of the derived type are not used to store the parse result, only the return type is substantial.  
   * @throws IllegalAccessException if any error occurs in the found method.
   */
  protected DstInstanceAndClass searchCreateMethod(Class<?> inClazz, Object instance, String semantic
      , DstInstanceAndClass parent, ZbnfParseResultItem zbnfElement) 
      //throws IllegalArgumentException, IllegalAccessException
  throws IllegalAccessException
  {
    if(semantic.equals("textExprTEST"))
      Debugutil.stop();
    Class<?>[][] argtypes = new Class[2][];
    argtypes[0]= null;
    argtypes[1] = new Class[1];
    argtypes[1][0] = ZbnfParseResultItem.class;
    int[] ixVariant = new int[1];
    Method method = searchMethod(ixVariant, inClazz, "new_" + semantic, argtypes); //new Class[1][0]);
    if(method != null)
    { final Class<?> childClass = method.getReturnType();
      final Object childOutputInstance;
      Object[] param;
      if(ixVariant[0] == 0) {
        param = null; //without param.
      } else {
        param = new Object[1];
        param[0] = zbnfElement;
      }
      try{ childOutputInstance = method.invoke(instance, param); }
      catch(Exception exc)
      { //throw new IllegalAccessException("exception inside: " + method.toString()); 
        throw new RuntimeException("exception inside: " + method.toString(), exc);
      }
      return new DstInstanceAndClass(parent, semantic, childOutputInstance, childClass, true);
    }
    else return null;
  }
  
  
  
  
  /**Searches the <code>add_<i>semantic</i>(Object)</code>-Method and invokes it.
   * This routine will be called after a ZBNF-components content is set into the destination Object.
   * @param semantic The semantic from ZBNF.
   * @param parentClass The class where the <code>add_<i>semantic</i>(Object)</code>-Method should be declared
   *                    -or in its superclasses.
   * @param parentInstance The instance with them the <code>add_<i>semantic</i>(Object)</code>-Method
   *                       should be called. 
   * @param componentsDestination The Object where the content of the component is set.
   * @throws IllegalArgumentException If the method isn't found and {@link #bExceptionIfnotFound} is true.
   * @throws IllegalAccessException If the <code>add_<i>semantic</i>(Object)</code>-Method isn't public.
   */
  private void searchAddMethodAndInvoke
  (String semantic, DstInstanceAndClass component, DstInstanceAndClass componentsDestination) 
  throws IllegalArgumentException, IllegalAccessException
  { Class<?>[][] argtypes = new Class[1][1];
    argtypes[0][0] = componentsDestination.clazz;
    Class<?> inClazz0 = component.instance instanceof GetTypeToUse ? ((GetTypeToUse)component.instance).getTypeToUse() : component.clazz; //component.instance.getClass();
    Class<?> inClazz = inClazz0;
    Method method = null;
    do {
      method = searchMethod(null, inClazz, "set_" + semantic, argtypes);
      if(method == null)
      { method = searchMethod(null, inClazz, "add_" + semantic, argtypes);
      }
      if(method != null)
      { Object[] argMethod = new Object[1];
        argMethod[0] = componentsDestination.instance;
        try{ method.invoke(component.instance, argMethod); }
        catch(InvocationTargetException exc)
        { String sMsg = "The called method " +method.toGenericString() + " throws an Exception: " + exc.getTargetException(); // + ", msg: " + exc.getTargetException().getMessage();
          if(report!=null){ report.writeWarning(sMsg); }
          throw new IllegalAccessException( sMsg );
        }
        catch(Exception exc)
        { throw new IllegalAccessException("can not access: " + inClazz.getCanonicalName()  + ".add_" + semantic + "(...) or .set..."); 
        }
      }
    } while(method == null && (inClazz = inClazz.getSuperclass()) != Object.class);
    //
    if(method == null)
    { String sProblem = "method " + inClazz0.getCanonicalName() + ".set_- or .add_" +semantic+"(" + componentsDestination.clazz.getCanonicalName() + ") not found";
      problem(component, sProblem);
    }
  }
  
  
  
  
  
  
  
  /**Searches a class and instance appropriate to the given semantic to store a component.
   * Either a <code>new_<i>semantic</i>()</code>-method is found or a field with the given semantic
   * is found.
   * <ul>
   * <li>If a <code>new_<i>semantic</i>()</code>-method is found, it is invoked. 
   *   The returned instance is the instance for the component. Note that the <code>new_...()</code> method
   *   don't need to create a new instance in any case. It is possible to returns any destination instance
   *   which is used temporary till the <code>add_...()</code> method ({@link #searchAddMethodAndInvoke(String, DstInstanceAndClass, DstInstanceAndClass)}).
   *   The return type of the found method is the components class. 
   *   The method {@link #searchCreateMethod(Class, Object, String)} is called for that.
   * <li>If a new_-method is not found, a field with the semantic-name is searched.
   *   If it is found, and it is not a container type (List), the type of the field is the components type.
   *   If the field is <code>null</code>, a new Object with the given type is created and set.
   *   The object referenced to the field is returned.
   *   <br>
   *   If the field is a container type, a <code>List</code>, than a new Object with the generic type
   *   of the List is created and added. See called method {@link #getComponentsOutputField(Field, Object)}.    
   * </ul>
   * @param semantic The name of the new_-method or the field.
   * @param parentDst The destination instance and class where the method or field is searched.
   * @return The class and the instance for the component. If a new_-method is called, 
   *         the attribute {@link DstInstanceAndClass#shouldAdd} is set true. 
   *         Because the add_-method should called also. 
   * @throws IllegalArgumentException If no method or field is found and {@link #bExceptionIfnotFound} is set.
   * @throws IllegalAccessException If a problem with the field or method exists, especially the field or method should be public!
   * @throws InstantiationException If a problem calling the new_-method exists. 
   */
  protected DstInstanceAndClass searchComponentsDestination(String semantic, ZbnfParseResultItem zbnfItem
    , DstInstanceAndClass parentDst) 
  throws IllegalArgumentException, IllegalAccessException, InstantiationException
  { /**The returned instance if resultItem is null, and the field is searched. */
    DstInstanceAndClass child = null;
    int posSeparator = semantic.indexOf('/');
    String semanticLowerCase = null;
    if(posSeparator >0) { 
      assert(false);  //checked outside yet.
      String sematicFirst = semantic.substring(0, posSeparator);
      child = searchComponentsDestination(sematicFirst, zbnfItem, parentDst);
      String semanticRest = semantic.substring(posSeparator+1);
      if(semanticRest.startsWith("@")) {
        //If the second (resp. last) part of the semantic is an attribute for XML, it is not an Object to store.
        return child;
      } else {
        //Note: it is an recursively call till end of obj1/obj2/obj3
        return searchComponentsDestination(semanticRest, zbnfItem, new DstInstanceAndClass(parentDst, semantic, child.instance, child.clazz, false));
      }
    }
    else
    { final Class<?> clazz1 = parentDst.instance instanceof GetTypeToUse ? ((GetTypeToUse)parentDst.instance).getTypeToUse() : parentDst.clazz; //instance.getClass(); //search in the class
      Class<?> clazz = clazz1;
      do {
        child = searchCreateMethod(clazz, parentDst.instance, semantic, parentDst, zbnfItem);
        
        if(child == null)
        { //if(!bOnlyMethods)
          { char firstChar = semantic.charAt(0);
            semanticLowerCase = firstChar >='a' && firstChar <='z' ? semantic : Character.toLowerCase(firstChar) + semantic.substring(1);
            if(semanticLowerCase.equals("operator"))
              stop();
            try{ 
              Field element = clazz.getDeclaredField(semanticLowerCase);
              //an element with the desired name is found, write the value to it:
              report.report(MainCmdLogging_ifc.fineDebug, semanticLowerCase);
              child = getComponentsOutputField(semantic, element, parentDst.instance);
            }
            catch(NoSuchFieldException exception){}
          }
          //else
          { //if(bStrict) throw new IllegalArgumentException("cannot found: " + sMethodToFind);
          }
        }
      } while(child == null && (clazz = clazz.getSuperclass())!=Object.class);
      if(child == null)
      { String sProblem = "cannot found method new_" + semantic + "() or field " + semanticLowerCase; 
        problem(parentDst, sProblem);
      }
      return child;
    }    
  }
  

  
  /**Gets the instance and class to write a components content into.
   * <ul>
   * <li>If the given element references is a simple instance, it is returned. The returned class is the type of the reference,
   *   not the type of the instance.
   * <li>If the given element is a container type for more instances, especially a java.util.List,
   *   a new instance of the generic type of a List entry is returned. This instances is added at end of list.
   * </ul>      
   * @param element The element (field) of the parent, which is matching to the semantic of the component. 
   * @param outputInstance The output instance where the element is located.
   * @return new Wrapper of instance and class.
   * @throws IllegalArgumentException
   * @throws IllegalAccessException
   * @throws InstantiationException
   */
  private DstInstanceAndClass getComponentsOutputField(String semantic, Field element, Object outputInstance) 
  throws IllegalArgumentException, IllegalAccessException, InstantiationException
  { DstInstanceAndClass child;
    Object childInstance;
    try{ childInstance = element.get(outputInstance);}
    catch(IllegalAccessException exc)
    { throw new IllegalAccessException("ZbnfJavaOutput: cannot access " + element.getName() + " in "+ outputInstance.getClass().getCanonicalName());
    }
    Class<?> childClass = element.getType();
    String nameRef = childClass.getName();
    if(nameRef.equals("java.util.List") || nameRef.equals("java.util.LinkedList"))
    { List<Object> childContainer = (List<Object>)childInstance;
      Class<?> genericClass = null;
      Type generic = element.getGenericType();
      if(generic instanceof ParameterizedType)
      { ParameterizedType g1 = (ParameterizedType)generic;
        Type[] t1 = g1.getActualTypeArguments();
        genericClass = (Class<?>)t1[0];
      }
      if(childContainer == null)
      { if(nameRef.equals("java.util.List"))
        { childClass = LinkedList.class;
        }
        childContainer = (List<Object>)childClass.newInstance();
        element.set(outputInstance, childContainer);
      }
      Object childContainerInstance = genericClass.newInstance();
      childContainer.add(childContainerInstance);
      child = new DstInstanceAndClass(null, semantic, childContainerInstance, genericClass, false);
    }  
    else
    { if(childInstance == null)
      { try{ childInstance = childClass.newInstance();
      
        } catch(Exception exc){
          throw new InstantiationException(exc.getMessage());
        }
        element.set(outputInstance, childInstance);
      }
      child = new DstInstanceAndClass(null, semantic, childInstance, childClass, false);
    }
    return child;          
  }
  
  

  
  
  /**searches the method or field matching to the semantic of the leaf resultItem (not a ZBNF-component) 
   * and write the given content of resultItem into it, 
   * @param semanticRaw The semantic from ZBNF parse result item. 
   *        It may be a child destination: <code>part1/part2</code> or may contain an <code>@</code>
   *        to designate attributes if it will be stored in XML. If a child destination is given,
   *        that destination is searched adequat to a destination for a component, 
   *        and than this method is called recursively with that destination and the right rest of semantic.
   *        A <code>@</code> as first char is ignored.<br>
   * @param destComponent The class where to found the method or field. and the instance to write into or invoke methods.
   *        It should contain a matching public field or method.
   *        A field matches, if its name is equal the semantic, but with lower case at first char.
   *        A method matches, if it is named set_SEMANTIC, where SEMANTIC is the semantic of the zbnf item.
   * <br>
   * @param resultItem The semantic is determining the matching field or method. 
   *        The content is used if childOutputInstance is null.
   * @return null usual, A child instance if a new_semantic method was found.        
   * @throws IllegalAccessException if the element is not writeable especially not public. 
   * @throws IllegalArgumentException 
   * @throws InstantiationException if the creation of a child instance fails.
   */
  protected DstInstanceAndClass searchDestinationAndWriteResult
  ( final String semanticRaw
  , final DstInstanceAndClass destComponent
  , final ZbnfParseResultItem resultItem
  ) throws IllegalArgumentException, IllegalAccessException, InstantiationException
  { 
    /**The returned instance if resultItem is null, and the field is searched. */
    DstInstanceAndClass child = null;
          
    final String semantic = semanticRaw.startsWith("@") ? semanticRaw.substring(1) : semanticRaw;
    if(semantic.equals("posIndent"))
      stop();     
    int posSeparator = semantic.lastIndexOf('/');
    if(posSeparator >0)
    { String sematicFirst = semantic.substring(0, posSeparator);
      child = searchComponentsDestination(sematicFirst, resultItem, destComponent);
      String semanticRest = semantic.substring(posSeparator+1);
      //NOTE: recursively call is necessary only because the destinationClass etc. are final.
      return searchDestinationAndWriteResult(semanticRest, child, resultItem);
    }
    else
    {
      Class[][] argTypesVariants;
      if(resultItem == null)
      { //search a new_-Method
        child = searchComponentsDestination(semantic, resultItem, destComponent);
      }
      else 
      { //writing of a simple element result
        if(destComponent.instance instanceof Map){
          child = writeInMap(semantic, destComponent, resultItem);
        } else {
          if(resultItem.isInteger() || resultItem.isFloat())
          { argTypesVariants = new Class[4][1];
            argTypesVariants[0][0] = Integer.TYPE;
            argTypesVariants[1][0] = Long.TYPE;
            argTypesVariants[2][0] = Float.TYPE;
            argTypesVariants[3][0] = Double.TYPE;
          }
          else if(resultItem.isFloat())
          { argTypesVariants = new Class[2][1];
            argTypesVariants[0][0] = Float.TYPE;
            argTypesVariants[1][0] = Double.TYPE;
          }
          else if(  resultItem.isString()
                 || resultItem.isIdentifier()
                 || resultItem.isTerminalSymbol()
                 || resultItem.isOption() && resultItem.getParsedString()!=null
                 )
          { //argMethod[0] = new Integer((int)resultItem.getNrofAlternative());
            argTypesVariants = new Class[1][1];
            argTypesVariants[0][0] = String.class; 
          }
          else
          { //no data in element, search of a argument-less set_- or add_-method: 
            argTypesVariants = new Class[1][0];
          }
          Method method;
          boolean bNewChild = false;
          final String sMethodToFind = destComponent.clazz.getCanonicalName()+ ".set_/add_/new_" + semantic + "(" + (argTypesVariants[0].length >0 ? argTypesVariants[0][0].getName() : "void" ) + ")";
          method = searchMethod(null, destComponent.clazz, "set_" + semantic, argTypesVariants);      
          if(method == null)
          { method = searchMethod(null, destComponent.clazz, "add_" + semantic, argTypesVariants);      
          }
          if(method == null)
          { method = searchMethod(null, destComponent.clazz, "new_" + semantic, argTypesVariants);      
            bNewChild = true;
          }
          if(method != null)
          { //invoke the method with the given matching args.
            Class[] parameterTypes = method.getParameterTypes();
            Object[] argMethod;
            if(parameterTypes.length >= 1)
            { argMethod = new Object[1];
              Class type1 = parameterTypes[0];
              boolean isFloat = resultItem.isFloat();
              double floatVal = isFloat ? resultItem.getParsedFloat() : resultItem.getParsedInteger();
              long intVal = isFloat ? (long)resultItem.getParsedFloat() : resultItem.getParsedInteger();
              
              String type =type1.getName();
              if     (type.equals("long"))   { argMethod[0] = new Long(isFloat ? (int)floatVal : intVal); }
              else if(type.equals("int"))    { argMethod[0] = new Integer((int)(isFloat ? floatVal : intVal)); }
              else if(type.equals("double")) { argMethod[0] = new Double(isFloat ? floatVal : intVal); }
              else if(type.equals("float"))  { argMethod[0] = new Float((float)(isFloat ? floatVal : intVal)); }
              else if(type1 == String.class) 
              { argMethod[0] = new String(resultItem.getParsedString());
                if(argMethod[0] == null)
                { argMethod[0] = new String(resultItem.getParsedText());
                }
              }
              else
              { throw new IllegalAccessException("unexpected argument type: " + sMethodToFind + " / " + type1.getName()); 
              }
            }
            else
            { argMethod = null;  //parameterless
              
            }
            try{ final Object childOutputInstance = method.invoke(destComponent.instance, argMethod); 
              if(bNewChild) {
                //A new_ method returns an instance, it is a child destination (similar an syntax component). 
                //This component is used till end of component.
                final Class<?> childClass = method.getReturnType();
                child = new DstInstanceAndClass(destComponent, semantic, childOutputInstance, childClass, true);
              }
            }
            catch(InvocationTargetException exc)
            { throw new IllegalAccessException("error in accessing: " + sMethodToFind + " : " + exc.getMessage() + exc.getTargetException().getMessage()); 
            }
            catch(Exception exc)
            { throw new IllegalAccessException("error calling: " + sMethodToFind + " / " + exc.getMessage()); 
            }
          }
          else 
          { //if(!bOnlyMethods)
            { char firstChar = semantic.charAt(0);
              String semanticLowerCase = firstChar >='a' && firstChar <='z' ? semantic : Character.toLowerCase(firstChar) + semantic.substring(1);
              Field element = null;
              Class searchClass = destComponent.clazz;
              if(semanticLowerCase.equals("cssHtml"))
                stop();
              do
              { try{ element = searchClass.getDeclaredField(semanticLowerCase);}
                catch(NoSuchFieldException exception)
                { element = null; 
                }
              } while(  element == null    //search in all super classes.
                     && (searchClass = searchClass.getSuperclass()) != null
                     && searchClass != Object.class
                     );  
              if(element != null)
              { //an element with the desired name is found, write the value to it:
                report.report(MainCmdLogging_ifc.fineDebug, semanticLowerCase);
                writeInField(element, destComponent.instance, resultItem);
              }
              else
              { String sProblem = "cannot found method " + sMethodToFind + " or field " + semanticLowerCase; 
                problem(destComponent, sProblem);
              }
            }
            //else
            { //if(bStrict) throw new IllegalArgumentException("cannot found: " + sMethodToFind);
            }
          }
        }
      }    
      //search an integer field with name_inputColumn, if found write the input column if the parse result.
      trySetInputColumn(semantic, destComponent, resultItem.getInputColumn());
      return child; //outputInstanceNew;      
    }              
  }
  
  
  
  
  
  /**Writes a value in a given field.
   * @param element The field
   * @param outputInstance the associated instance
   * @param resultItem The ZBNF parser result.
   * @throws IllegalAccessException
   */
  private void writeInField( Field element
                             , Object outputInstance
                             , ZbnfParseResultItem resultItem
                             )
  throws IllegalAccessException
  { String sType = element.getType().getName();
    String debugValue = "???";
    boolean debug = report.getReportLevel() >= MainCmdLogging_ifc.fineDebug;
    boolean isFloat = resultItem.isFloat();
    double floatVal = isFloat ? resultItem.getParsedFloat() : resultItem.getParsedInteger();
    long intVal = isFloat ? (long)resultItem.getParsedFloat() : resultItem.getParsedInteger();
    try
    { if(sType.equals("int"))
      { int value = (int)(isFloat ? floatVal : intVal);
        element.setInt(outputInstance, value);
        if(debug) debugValue = "" + value;
      }
      else if(sType.equals("long"))
      { long value = (isFloat ? (long)floatVal : intVal);
        element.setLong(outputInstance, value);
        if(debug) debugValue = "" + value;
      }
      else if(sType.equals("float"))
      { float value = (float)(isFloat ? floatVal : intVal);
        element.setFloat(outputInstance, value);
        if(debug) debugValue = "" + value;
      }
      else if(sType.equals("double"))
      { double value = (isFloat ? floatVal : intVal);
        element.setDouble(outputInstance, value);
        if(debug) debugValue = "" + value;
      }
      else if(sType.equals("boolean"))
      { element.setBoolean(outputInstance, true);
        if(debug) debugValue = "true";
      }
      else if(sType.equals("java.lang.String"))
      { String value = resultItem.getParsedString();
        if(value == null){ value = resultItem.getParsedText(); }
        element.set(outputInstance, value);
        if(debug) debugValue = value;
      }
      else if(sType.equals("char"))
      { String value = resultItem.getParsedString();
        if(value == null){ value = resultItem.getParsedText(); }
        char cc;
        if(value !=null && value.length() >=1){
          cc = value.charAt(0);
        } else {
          cc = (char)resultItem.getParsedInteger();
        }
        element.setChar(outputInstance, cc);
        if(debug) debugValue = value;
      }
      else if(sType.equals("java.util.List"))
      { String value = resultItem.getParsedString();
        if(value == null){ value = resultItem.getParsedText(); }
        List list = (java.util.List)element.get(outputInstance);
        if(list == null)
        { list = new java.util.LinkedList<List>();
          element.set(outputInstance, list);
        }
        list.add(value);
        
        if(debug) debugValue = value;
      }
      else
      { throw new IllegalArgumentException("Unexpected type of field: " + sType + " " + element.getName() 
        + " in " + outputInstance.getClass().getName()
        + " Hint: An access to a public element can only be done for types int, long, float, double, boolean, String and List<String>."
        );
      }
    }
    catch(IllegalAccessException exc)
    {
      throw new IllegalAccessException("access to field is denied: " + outputInstance.getClass().getName() + "." + element.getName() + " /Type: " + sType); 
    }
    report.report(MainCmdLogging_ifc.fineDebug, " \""+ debugValue + "\" written in Element Type " + sType);
  }

  
  
  
  
  
  /**Tries if an field <code>inputLine_</code> or a method
   * <code>set_inputInfo_</code> exists and set resp. calls it.
   * The method <code>set_inputInfo_</code> have to return any object, not null, to detect whether it is existence.
   * Use boolean as return value.
   * If such a field or method isn't found, nothing is done. It is okay.
   * @param semantic Name, it may be emtpy especially to search <code>inputColumn_</code> for the component.
   * @param destinationClass Class where searched.
   * @param destinationInstance Associated instance where set or called.
   * @param column The value of column. If it is negative, nothing is done. A negative value may indicate,
   *               that no valid column is given to set.
   * @return true if the method "set_InputInfo(int, int, String)" was found.              
   * @throws IllegalAccessException If any problem with the set-method exists.
   */
  private boolean trySetInputInfo(DstInstanceAndClass destComponent, int line, int column, String sFile) 
  throws IllegalAccessException
  { boolean bok = false;
    if(line >=0)
    { DataAccess.DatapathElement datapath = new DataAccess.DatapathElement("inputLine_");
      try
      { //if an field inputColumn_ is found, write to it.
        Field elementColumn = destComponent.clazz.getField("inputLine_");
        elementColumn.setInt(destComponent.instance, line);
        bok = true;  //successfull, no exception
      }
      catch(NoSuchFieldException exception)
      { /**do nothing if the field isn't found.*/ 
        //not an element with the postulated name found,
        //search an appropriate method:
        try
        { //NOTE: use DataAccess to find in super classes, manually it is complex.
          datapath.set("set_inputInfo_()");
          datapath.setActualArguments(new Integer(line),  new Integer(column),  sFile);
          Object ok = DataAccess.invokeMethod(datapath, destComponent.clazz, destComponent.instance, false, true);
          bok = ok !=null;   
          //method = destComponent.clazz.getDeclaredMethod("set_inputLine_", argTypes1);
          //Object[] argMethod1 = new Object[1];
          //argMethod1[0] = new Integer(line);
          //method.invoke(destComponent.instance, argMethod1);
        }
        catch(NoSuchMethodException exception1){ 
          /*
          try{
            Method method;
            Class[] argTypes1 = new Class[3]; 
            argTypes1[0] = Integer.TYPE;
            argTypes1[1] = Integer.TYPE;
            argTypes1[2] = sFile.getClass();
            method = destComponent.clazz.getDeclaredMethod("set_inputInfo_", argTypes1);
            Object[] argMethod1 = new Object[3];
            argMethod1[0] = new Integer(line);
            argMethod1[1] = new Integer(column);
            argMethod1[2] = sFile;
            method.invoke(destComponent.instance, argMethod1);
          }
          catch(NoSuchMethodException exception2){ } //do nothing if the field isn't found.
          catch(Exception exc)
          { throw new IllegalAccessException(exc.getMessage()); 
          }
          */
        }
        catch(Exception exc)
        { throw new IllegalAccessException(exc.getMessage()); 
        }
      }
    }  
    return bok;
  }
  
  
  
  
  
  /**Tries if an field <code>inputColumn_<i>semantic</i></code> or a method
   * <code>set_inputColumn_<i>semantic</i></code> exists and set resp. calls it.
   * If such a field or method isn't found, nothing is done. It is oksy.
   * @param semantic Name, it may be emtpy especially to search <code>inputColumn_</code> for the component.
   * @param destinationClass Class where searched.
   * @param destinationInstance Associated instance where set or called.
   * @param column The value of column. If it is negative, nothing is done. A negative value may indicate,
   *               that no valid column is given to set.
   * @throws IllegalAccessException If any problem with the set-method exists.
   */
  private void trySetInputColumn(String semantic, DstInstanceAndClass destComponent, int column) 
  throws IllegalAccessException
  { if(column >=0)
    { try
      { //if an field inputColumn_ is found, write to it.
        Field elementColumn = destComponent.clazz.getField("inputColumn_" + semantic);
        elementColumn.setInt(destComponent.instance, column);
      }
      catch(NoSuchFieldException exception)
      { /**do nothing if the field isn't found.*/ 
        //not an element with the postulated name found,
        //search an appropriate method:
        Method method;
        Class[] argTypes1 = new Class[1]; 
        argTypes1[0] = Integer.TYPE;
        try
        { method = destComponent.clazz.getDeclaredMethod("set_inputColumn_" + semantic, argTypes1);
          Object[] argMethod1 = new Object[1];
          argMethod1[0] = new Integer(column);
          method.invoke(destComponent.instance, argMethod1);
        }
        catch(NoSuchMethodException exception1){ /**do nothing if the field isn't found.*/ }
        catch(InvocationTargetException exc)
        { throw new IllegalAccessException(exc.getMessage()); 
        }
      }
    }  
  }
  
  
  private void problem(DstInstanceAndClass destComponent, String sProblem){
    StringBuilder u = new StringBuilder(sProblem);
    u.append("\n");
    DstInstanceAndClass parentComponent = destComponent;
    while(parentComponent !=null){
      u.append(", invoked from \"").append(parentComponent.semantic).append("\" in ").append(parentComponent.clazz.getCanonicalName())
      .append(" = " + parentComponent.instance.toString()).append("\n");
      parentComponent = parentComponent.parentResult;
    }
    if(bExceptionIfnotFound) throw new IllegalArgumentException(u.toString());
    else noteError(u);

  }
  
  
  
  DstInstanceAndClass writeInMap(final String semantic
      , final DstInstanceAndClass destComponent
      , final ZbnfParseResultItem resultItem
  ){
    Map<String, String> dest = (Map)destComponent.instance;
    String sParseResult = resultItem.getParsedText();
    dest.put(semantic, sParseResult);
    return null;
  }
  
  
  
  /**It's a debug helper. The method is empty, but it is a mark to set a breakpoint. */
  void stop()
  { //debug
  }





  /**Parses the given file with given syntax and fills the parsed result into the result object.
   * This is a simple common use-able routine to transfer textual content into content of a Java object.
   * <br>
   * NOTE:This routine is static because it is a recognition to functional programming. 
   * No side effects are occurring. This method sets nothing, except the content of result. it returns only anything.
   * An instance of this class is created internally temporary.
   *
   * @param resultType The type or a interface or basic type of result. The fields and methods are searched in this type.
   * @param result The instance, it have to be of type 'resultType', but may be derived.
   * @param fInput The input file to parse.
   * @param fSyntax The syntax file using ZBNF
   * @param report  MainCmdLogging_ifc for parsing process and errors
   * @param msgRange A start number of created messages in report.
   * @return null if no error, else a short error text. The explicitly error text is written in report.
   */
  public static String parseFileAndFillJavaObject(Class resultType, Object result, File fInput, File fSyntax, MainCmdLogging_ifc report, int msgRange) 
  { ZbnfJavaOutput javaOutput = new ZbnfJavaOutput(report);
    return javaOutput.parseFileAndFillJavaObject(resultType, result, fInput, fSyntax);
  }
  

  
  /**Parses the given file with given syntax and fills the parsed result into the result object.
   * This is a simple common use-able routine to transfer textual content into content of a Java object.
   * <br>
   * The non static variant allows to set some options using class methods.
   * 
   * @param resultType The type or a interface or basic type of result. The fields and methods are searched in this type.
   * @param result The instance, it have to be of type 'resultType', but may be derived.
   * @param fInput The input file to parse.
   * @param fSyntax The syntax file using ZBNF
   * @return null if no error, else a short error text. The explicitly error text is written in report.
   */
  public String parseFileAndFillJavaObject(Object result, File fInput, File fSyntax){
    return parseFileAndFillJavaObject(result.getClass(), result, fInput, fSyntax);
  }

  
  
  /**Parses the given file with given syntax and fills the parsed result into the result object.
   * This is a simple common use-able routine to transfer textual content into content of a Java object.
   * <br>
   * The non static variant allows to set some options using class methods.
   * 
   * @param resultType The type or a interface or basic type of result. The fields and methods are searched in this type.
   * @param result The instance, it have to be of type 'resultType', but may be derived.
   * @param fInput The input file to parse.
   * @param fSyntax The syntax file using ZBNF
   * @return null if no error, else a short error text. The explicitly error text is written in report.
   */
  public String parseFileAndFillJavaObject(Class resultType, Object result, File fInput, File fSyntax) 
  { String sError = null;
    int lenFileSyntax = (int)fSyntax.length();
    StringPartScan spSyntax = null;
    File dirSyntax = null;
    try{
      dirSyntax = FileSystem.getDirectory(fSyntax);
      spSyntax = new StringPartFromFileLines(fSyntax, lenFileSyntax, null, null); 
    }
    catch(FileNotFoundException exc)
    { sError = "ZbnfJavaOutput - Syntax file not found; " + fSyntax.getAbsolutePath();
    }
    catch(IOException exc)
    { sError = "ZbnfJavaOutput - Syntax file read problems; " + fSyntax.getAbsolutePath() + " msg = " + exc.getMessage();
    }
    catch(IllegalArgumentException exc)
    { //it is IllegalCharsetNameException, UnsupportedCharsetException
      sError = "ZbnfJavaOutput - Syntax file charset problems; " + fSyntax.getAbsolutePath() + " msg = " + exc.getMessage();
    }
    if(sError == null)
    { sError = parseFileAndFillJavaObject(resultType, result, fInput, spSyntax, dirSyntax);
      if(sError != null && sError.startsWith("ERROR in syntax"))
      { sError += " in file " + fSyntax.getAbsolutePath();
      }
    }
    return sError;
  }

  
  
  /**Parses the given file with given syntax and fills the parsed result into the result object.
   * This is a simple common use-able routine to transfer textual content into content of a Java object.
   * <br>
   * The non static variant allows to set some options using class methods.
   * 
   * @param resultType The type or a interface or basic type of result. The fields and methods are searched in this type.
   * @param result The instance, it have to be of type 'resultType', but may be derived.
   * @param fInput The input file to parse.
   * @param sSyntax The syntax using ZBNF
   * @return null if no error, else a short error text. The explicitly error text is written in report.
   */
  public String parseFileAndFillJavaObject(Class resultType, Object result, File fInput, String sSyntax) 
  { StringPartScan spSyntax = new StringPartScan(sSyntax);
    return parseFileAndFillJavaObject(resultType, result, fInput, spSyntax);
  }
  
  
  /**Parses the given file with given syntax and fills the parsed result into the result object.
   * This is a simple common use-able routine to transfer textual content into content of a Java object.
   * <br>
   * The non static variant allows to set some options using class methods.
   * 
   * @param result The instance to store the result.
   * @param fInput The input file to parse.
   * @param sSyntax The syntax using ZBNF
   * @return null if no error, else a short error text. The explicitly error text is written in report.
   */
  public String parseFileAndFillJavaObject(Object result, File fInput, String sSyntax) 
  { StringPartScan spSyntax = new StringPartScan(sSyntax);
    return parseFileAndFillJavaObject(result.getClass(), result, fInput, spSyntax);
  }
  

  public String parseFileAndFillJavaObject(Class resultType, Object result, File fInput, StringPartScan spSyntax) 
  {
    return parseFileAndFillJavaObject(resultType, result, fInput, spSyntax, null);
  }
  
  
  /**Parses the given file with given syntax and fills the parsed result into the result object.
   * This is a simple common use-able routine to transfer textual content into content of a Java object.
   * <br>
   * The non static variant allows to set some options using class methods.
   * 
   * @param resultType The type or a interface or basic type of result. The fields and methods are searched in this type.
   * @param result The instance, it have to be of type 'resultType', but may be derived.
   * @param fInput The input file to parse.
   * @param spSyntax The ZBNF-syntax of the fInput, semantic should be proper to the resultType and result.
   * @param fSyntaxDir directory which is used as base for import statements. It is the directory of the syntax file usually.
   *   It may be null. The imports are not supported in the syntax.
   * @return null if no error, else a short error text. The explicitly error text is written in report.
   * @since 2018-09-11 bugfix: On error reading file: The error was ignored and the parser was started, which returns null as parse result.
   *        fix: return sError.
   */
  public String parseFileAndFillJavaObject(Class resultType, Object result, File fInput, StringPartScan spSyntax, File fSyntaxDir) 
  //throws FileNotFoundException, IOException, ParseException, IllegalArgumentException, InstantiationException
  { String sError = null;
    //configure the parser:
    StringPartFromFileLines spInput = null;
    if(sError == null)
    { int lenFileInput = (int)fInput.length();
      try{ spInput = new StringPartFromFileLines(fInput, lenFileInput, null, null); }
      catch(FileNotFoundException exc)
      { sError = "ZbnfJavaOutput - Input file not found, " + fInput.getAbsolutePath();
      }
      catch(IOException exc)
      { sError = "ZbnfJavaOutput - Input file read problems; " + fInput.getAbsolutePath() + " msg = " + exc.getMessage();
      }
      catch(IllegalArgumentException exc)
      { //it is IllegalCharsetNameException, UnsupportedCharsetException
        sError = "ZbnfJavaOutput - Input file charset problems; " + fInput.getAbsolutePath() + " msg = " + exc.getMessage();
      }
    }
    if(sError !=null) { return sError; }
    else return parseFileAndFillJavaObject(resultType, result, spInput, fInput.getAbsolutePath(), spSyntax, fSyntaxDir);
  }


  
  
  /**Parses the given String with given syntax and fills the parsed result into the result object.
   * This is a simple common use-able routine to transfer textual content into content of a Java object.
   * <br>
   * The non static variant allows to set some options using class methods.
   * 
   * @param resultType The type or a interface or basic type of result. The fields and methods are searched in this type.
   * @param result The instance, it have to be of type 'resultType', but may be derived.
   * @param sInput Textual input may read from a file or given as constant, especially by using JZcmd
   * @param sFileInput This String is used only for output messages on error. It may be ""
   * @param syntax Textual given syntax in ZBNF format.
   * @param fSyntaxDir directory which is used as base for import statements. It is the directory of the syntax file usually.
   *   It may be null. The imports are not supported in the syntax.
   * @return
   */
  public String parseFileAndFillJavaObject(Class resultType, Object result, String sInput, String sFileInput,  String syntax, File fSyntaxDir) 
  //throws FileNotFoundException, IOException, ParseException, IllegalArgumentException, InstantiationException
  { String sError = null;
    StringPartScan spInput = new StringPartScan(sInput);
    StringPartScan spSyntax = new StringPartScan(syntax);
    return parseFileAndFillJavaObject(resultType, result, spInput, sFileInput, spSyntax, fSyntaxDir);
  }
  
  
  
  /**Parses the given file with given syntax and fills the parsed result into the result object.
   * This is a simple common use-able routine to transfer textual content into content of a Java object.
   * <br>
   * The non static variant allows to set some options using class methods.
   * 
   * @param resultType The type or a interface or basic type of result. The fields and methods are searched in this type.
   * @param result The instance, it have to be of type 'resultType', but may be derived.
   * @param spInput
   * @param sFileInput
   * @param spSyntax
   * @param fSyntaxDir directory which is used as base for import statements. It is the directory of the syntax file usually.
   *   It may be null. The imports are not supported in the syntax.
   * @return
   */
  private String parseFileAndFillJavaObject(Class resultType, Object result, StringPartScan spInput, String sFileInput,  StringPartScan spSyntax, File fSyntaxDir) 
  //throws FileNotFoundException, IOException, ParseException, IllegalArgumentException, InstantiationException
  { String sError = null;
  
    //ZbnfParser zbnfParser = null;
    if(sError == null)
    { //zbnfParser = new ZbnfParser(report);
      try{ parser.setSyntax(spSyntax, fSyntaxDir == null ? null: fSyntaxDir.getAbsolutePath()); }
      catch(ParseException exc)    
      { sError = "ZbnfJavaOutput - ERROR in syntax prescript; " + exc.getMessage();
      }
      catch(FileNotFoundException exc){ 
        sError = "import in ZBNF-script is not supported here."; 
      }
      catch(IOException exc){ 
        sError = "import in ZBNF-script is not supported here."; 
      }
    }  
    if(sError == null && spInput !=null)
    { 
      parser.setReportIdents(7,7,7,7); //MainCmdLogging_ifc.error, MainCmdLogging_ifc.info, MainCmdLogging_ifc.error, MainCmdLogging_ifc.debug);
      //parser.setReportIdents(MainCmdLogging_ifc.error, MainCmdLogging_ifc.info, MainCmdLogging_ifc.error, MainCmdLogging_ifc.error);
      //parse the file:
      boolean bOk = parser.parse(spInput);
      if(!bOk)
      { final String sParserError = parser.getSyntaxErrorReport();
        sError = "ZbnfJavaOutput - ERROR syntax in input file; " + sFileInput + "\n" + sParserError;
        //report.writeError(sError);
      }
      spInput.close();
      //The content of the setting file is stored inside the parser as 'parse result'.
      //The ZbnfJavaOutput.setOutput moves the content to the class 'settings'.
      //The class settings contains the necessary elements appropriate to the semantic keywords in the syntax prescript.
      //zbnfParser.reportStore(report, MainCmdLogging_ifc.debug);
    }
    if(sError == null)
    { /*store the whole parse result in the instance 'result', using the 'resultType'. */ 
      System.out.println("ZbnfJavaOutput - fillin;" + resultType.getCanonicalName());
      try{ setContent(resultType, result, parser.getFirstParseResult()); } 
      catch (IllegalAccessException exc)
      { sError = "ZbnfJavaOutput - ERROR access to elements;. Hint: The elements should be public!: " + exc.getMessage();
      } 
      catch (IllegalArgumentException exc)
      { sError = "ZbnfJavaOutput - ERROR access to elements, IllegalArgumentException;: " + exc.getMessage();
      } 
      catch (InstantiationException exc)
      { sError = "ZbnfJavaOutput - ERROR access to elements, InstantiationException; Hint: Any sub-classes for parse results should be static; " + exc.getMessage();
      } 
      
    }  
  
    return sError;
  }
  

  /**Adds an error. This method is called if {@link #bExceptionIfnotFound} = false.
   * @param problem The text.
   */
  private void noteError(CharSequence problem)
  {
    if(errors == null)
    { errors = new StringBuffer();
    }
    errors.append('\n').append(problem).append('\n');
  }



}
