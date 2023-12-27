package org.vishia.util;


import java.io.IOException;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;

import org.vishia.util.Assert;
import org.vishia.util.TreeNodeBase;



/**This class contains methods to access and set data and invoke methods with symbolic access using reflection mechanism.
 * The class is helpful to deal with reflection. Some methods are offered to make it simply. This class is independent
 * of other classes else {@link Assert} and {@link TreeNodeBase}. The last one is checked whether it is used as container.
 * It is not necessary to work with.
 * <br><br> 
 * All methods throws the proper exception if anything is not correct. The user should catch it if necessary.
 * <ul>
 * <li>Simple access to fields, also to super and enclosing instances
 * <li>Access to referred instance in one method.
 * <li>access to container with a name as key similar to fields.
 * <li>Support a datapool using variables
 * <li>Invocation of methods and creation of instances (Constructor)
 * <li>Working with additional ClassLoader
 * </ul>
 * public static methods:
 * <ul>
 * <li>{@link #getDataFromField(String, Object, boolean)}: Data from one instance, also from super and enclosing.
 *   Enhances {@link java.lang.Class#getField(String)} and {@link java.lang.reflect.Field#get(Object)}
 * <li>{@link #getData(String, Object, boolean, boolean)}: Data from one instance. If the instance is a {@link java.util.Map}
 *   it accessed to an element of this container. Elsewhere it tries to get from a field. 
 *   Invokes {@link #getDataFromField(String, Object, boolean)}. 
 * <li>{@link #invokeNew(DatapathElement)} creates an instance by symbolic name maybe with parameters, maybe with 
 *   another ClassLoader (from additional jar files etc.). 
 *   Enhances {@link java.lang.Class#newInstance()} and {@link java.lang.reflect.Constructor#newInstance(java.lang.Object...)}. 
 * <li>{@link #invokeMethod(DatapathElement, Class, Object, boolean, boolean)} invokes a static method by symbolic name 
 *   maybe with parameters, maybe with another ClassLoader (from additional jar files etc.). 
 *   Enhances {@link java.lang.Class#getMethod(String, Class...)} and {@link java.lang.reflect.Method#invoke(Object, Object...)}. 
 * <li>{@link #access(List, Object, Map, boolean, boolean, boolean, Dst)} Data from a complex referenced instance
 *   maybe with method invocations and creation of instances. 
 *   It uses a <code>List< {@link DatapathElement}></code> to access,
 *   it uses method arguments. Static methods and creation of instances can invoked too. See {@link DatapathElement}.
 * <li>{@link #access(CharSequence, Object, Map, boolean, boolean, boolean, Dst)}: Path String given.
 * <li>{@link #storeValue(List, Map, Object, boolean)} stores instead accesses
 * <li>{@link #setVariable(Map, String, Object)}, {@link #getVariable(Map, String, boolean)}: Deal with variables.
 * <li>{@link #getEnclosingInstance(Object)}: Gets the enclosing instance
 * <li>{@link #getStringFromObject(Object, String)}, {@link #getInt(Object)}, {@link #getFloat(Object)}: access to simple data,
 *  conversions.
 * <li>{@link #setBit(int, int, boolean)} Helper to deal with bits
 * <li>{@link #getInt(Object)}, {@link #getFloat(Object)} etc. 
 * <li>      
 * </ul>
 * This class can hold a datapath, see {@link #add_datapathElement(DatapathElement)} and can access with this path
 * using the non-static method {@link #access(Map, boolean, boolean)}.
 * <br><br> 
 * Datapath elements are stored in {@link DatapathElement} and {@link DatapathElementClass}.
 * <br><br> 
 * A {@link Variable} is a wrapper for any Object which is used and regarded. Sometimes the Variable is returned
 * which's content can be changed: {@link Variable#setValue(Object)}. A Variable is helpfully if the content
 * of referenced instances should be changed without changing all references:
 * <pre>
 *      any_instance---------------->|
 *      other_instance-------------->|
 *                              Variable
 *                                   |--value------->references_somewhat
 *                                       ^   \
 *                                       |    ------>other_reference
 *                                     is changed:  
 * </pre>                                   
 * @author Hartmut Schorrig
 *
 */
public class DataAccess {
  /**Version, history and license.
   * <ul>
   * <li>2023-12-27: new field {@link DatapathElement#ixData}
   *   initial set in {@link DatapathElement#set(StringPartScan, Map, Class, boolean)} for an element found in the variable list "nameVariables",
   * <li>2023-12-27: new field {@link DatapathElement#reflAccess} 
   *   initial set in {@link DatapathElement#set(StringPartScan, Map, Class, boolean)} for an operation from first level,
   *   Used for {@link OutTextPreparer} &lt;:exec:operation(...)> to get the operation from the given Class reflData
   *   while invocation {@link DatapathElement#set(StringPartScan, Map, Class, boolean) for the first element}.
   *   TODO planned: set on demand on first usage, check the type.
   * <li>2023-12-27: {@link #listDatapath} renamed from "datapath" but additional {@link #oneDatapathElement}. It is faster.
   *   If only one element is contained in DataAccess, no list organization. {@link #add_datapathElement(DatapathElement)} regards it.
   *   {@link #datapath()} builds a List anyway, but {@link #dataPath1()} returns the only one element or null if more. 
   * <li>2023-12-25: {@link #access(DatapathElement, Object, boolean, boolean, Map, Object[], boolean, Dst)} one data==null test.
   *   General: not proper documented, an reference ==null forces return = null without exception. 
   *   Hence some more ==null tests may be necessary. 
   * <li>2023-05-21: {@link #istypeof(Object, Class)} now also can detect the class Object itself. More universal. 
   * <li>2023-05-05: Hartmut improved error message on method not found / argument mismatch, in {@link #invokeMethod(DatapathElement, Class, Object, boolean, boolean, Object[])} 
   * <li>2021-12-30: Hartmut: {@link #access(CharSequence, Object, boolean, boolean, boolean, Dst)} now regards a "%..." path for static access.
   * <li>2019-08-20: some operations throws {@link ParseException} on syntax errors.
   * <li>2019-08-23 Hartmut new: Supports now &(path) as indirect access, access to variable with gotten name in path.
   *   Therefore the operations {@link #access(Object, boolean, boolean, Map, Object[])} etc. needs the variableNames.  
   * <li>2019-08-19 Hartmut chg: error msg on throw improved on non found operation etc, line feed for better read ability. 
   * <li>2019-08-10 Hartmut new / chg: Usage of variable-values in a Object[] array: 
   *   <ul>
   *   <li>{@link #DataAccess(StringPartScan, Map, Class, char)} has a new 2. and 3. argument nameVariables and , as well as 
   *     {@link DataAccess.DatapathElement#DatapathElement(StringPartScan, Map, Class)}. If given this map is used 
   *     to search the first identifier there. If found  
   *   <li>The operations {@link #access(Object, boolean, boolean, Object[])}, 
   *     {@link #access(List, Object, boolean, boolean, Object[], boolean, Dst)},
   *     {@link #access(DatapathElement, Object, boolean, boolean, Object[], boolean, Dst)}
   *     {@link #invokeMethod(DatapathElement, Class, Object, boolean, Object[], boolean)} gets a new argument varValues
   *     to access the variable given on ctor and set operations.  
   *   <li>The routine {@link #invokeMethod(DatapathElement, Class, Object, boolean, Object[], boolean)}
   *   calculates the arguments of the method with the given calculation rule in {@link DatapathElement#args}. This approach is used in, see next
   *   <li>The operation {@link #invokeMethod(DatapathElement, Class, Object, boolean, Object[], boolean)} calculates the arguments 
   *     due to given algorithm in {@link DatapathElement#args} using the varValues which corresponds to the nameVariables argument on setting the expression.
   *   <li>The {@link DatapathElement} has the new field args, instead argNames and argExpr are removed. 
   *   </ul>
   * <li>2019-08-10 Hartmut chg: Some operations, all which parses, throws an {@link ParseException} on parsing errors. Hence the request to handle it is forced.
   * <li>2019-06-18 Hartmut new ctor: {@link #DataAccess(StringPartScan, char)} and {@link DatapathElement#set(StringPartScan)} used in parsing pattern in {@link CalculatorExpr}.
   * <li>2019-06-18 Hartmut new ctor: {@link DatapathElement#XXXargExpr} to calculate more complex arguments with given expression outside JZtxtcmd
   * <li>2019-05-30 Hartmut gardening for C#-translation: The {@link Conversion#key()} may be a better documentation for the conversion classes. It is experience, not used yet.
   * <li>2018-12-18 Hartmut improve: rename {@link DatapathElement#operation_}, private and setter and getter. The JzTxtCmd invokes the set_operation() without changes.
   *     It is set now on any operation. Used via {@link DatapathElement#isOperation()} in the {@link org.vishia.xmlReader.XmlZzReader}
   * <li>2018-09-28 Hartmut improve: An dataAccess element now knows {@link DatapathElement#operation}. If that is not set and there are no fnArgs
   *   then a field (static) is accessed. That were missing. Used for JzTxtCmd.
   * <li>2018-09-28 Hartmut improve: {@link #initConversion()} accepts boolean from Boolean, important for JzTxtCmd
   * <li>2017-12-20 Hartmut improve: {@link #invokeMethod(DatapathElement, Class, Object, boolean, boolean)}: Show used class on error.
   * <li>2017-08-30 Hartmut new: {@link #istypeof(Object, String)} with String argument, based on {@link #istypeof(Object, Class)}. 
   * <li>2017-07-02 Hartmut new: {@link #debugMethod(String)} can be invoked with "". Then it invokes {@link #debug()} on the next found method
   *   and removes the settings after them. It is usual in a JZtxtcmd Script to stop on the next method.
   * <li>2016-10-01 Hartmut new: {@link #storeValue(DatapathElement, Object, Object, boolean)} and {@link #access(DatapathElement, Object, boolean, boolean, boolean, Dst)}
   *   only with an datapath element, more simple. 
   * <li>2016-10-01 Hartmut new: {@link #invokeMethod(DatapathElement, Class, Object, boolean, boolean, Object[])} 
   *   with the arguments of the method. 
   *   The {@link DatapathElement#argNames} contains names of the arguments which are given with {@link DatapathElement#set(String)}.
   *   With that concept it is possible to define a method call in a script with control which values in the script are used as parameters.
   *   It is used for the {@link org.vishia.xmlReader.XmlJzReader}. 
   * <li>2016-01-17 Hartmut new: Now an element can be an array, which is accessed with indices: {@link SetDatapathElement#set_index(int)} 
   *   and {@link DatapathElement#indices}. Syntax for JZcmd adapted ({@link org.vishia.jztxtcmd.JZtxtcmdSyntax}).  
   * <li>2016-01-09 Hartmut bugfix: {@link #access(CharSequence, Object, boolean, boolean, boolean, Dst)} has dissolved a {@link Variable} twice,
   *   problem if a variable contains a Variable as value. The access <&myVariable.name> was faulty. Not it works. 
   * <li>2015-05-17 Hartmut new: conversion routines {@link #shortFromUnsignedByte(byte)} and {@link #intFromUnsignedShort(short)}.
   * <li>2015-05-17 Hartmut chg: Messages if methods not found.
   * <li>2014-10-19 Hartmut bugfix: {@link #invokeStaticMethod(DatapathElement)} with variable argument list but only 1 argument
   *   has not worked. TODO okay if no argument?
   * <li>2014-10-19 Hartmut new: {@link #isReferenceToEnclosing(Field)} and {@link #isOrExtends(Class, Class)} 
   * <li>2014-06-15 Hartmut chg: {@link #access(List, Object, boolean, boolean, boolean, Dst)} 
   *   and {@link #access(CharSequence, Object, boolean, boolean, boolean, Dst)} now with only one dataRoot.
   *   Not differenced between a Map<String,?) dataPool and a then non necessary dataRoot.
   * <li>2014-06-15 Hartmut new: {@link Variable#type()} to check whether the type of value is correct.
   *   It is not ready yet. Idea: Parameter Class<?> on creation of variables. JZcmd feature.
   * <li>2014-06-10 Hartmut new: {@link Conversions#list2array}: If a List is provided as argument
   *   and an array is expected and the first element matches, it is used. It is assumed that all arguments matches.
   *   TODO special handling for main(String[]) with List argument is not necessary then. 
   * <li>2014-06-01 Hartmut new: {@link DatapathElementClass#clazz} with a given class. Used on 
   *   {@link #invokeNew(DatapathElement)} and {@link #invokeStaticMethod(DatapathElement)} 
   * <li>2014-06-01 Hartmut new: {@link DatapathElementClass#loader}: static Methods 
   *   and {@link #invokeMethod(DatapathElement, Class, Object, boolean, boolean)}
   *   with an special {@link java.lang.ClassLoader}.
   * <li>2014-05-28 Hartmut new: some conversions added, especially for main(String[] args): compatible with 
   *   some CharSequence arguments. Automatic conversion from String to File removed.
   * <li>2014-05-25 Hartmut new: access(...): gets static data. It is essential for example for Math.PI 
   * <li>2014-05-18 Hartmut new: {@link #expandElements(String, char)} for String-given elements. The first only one element
   *   of {@link #access(List, Object, Map, boolean, boolean, boolean, Dst)} can contain "path.subpath..."
   *   {@link #access(CharSequence, Object, Map, boolean, boolean, boolean, Dst)} for String given elements.
   * <li>2014-05-18 Hartmut new: {@link ObjMethod} for invocation of an method described with reflection,
   *   not used yet but prepared. 
   * <li>2014-04-25 Hartmut chg: {@link #invokeMethod(DatapathElement, Class, Object, boolean, boolean)} with Class as parameter.
   *   {@link #access(List, Object, Map, boolean, boolean, boolean, Dst)} checks whether the obj is stored in an {@link Variable}
   *   with type 'C' and value instanceof Class. Then the static method of this Class type is searched and invoked.
   *   That feature is used in conclusion with <code>JZcmd Class myClass = package.path.Class;</code> 
   * <li>2014-03-08 Hartmut new: {@link #debugIdent(String)} sets an identifier on which debug() was called, to set manual breakpoint while debugging. 
   * <li>2014-01-26 Hartmut chg: The element <code>fnArgsExpr</code> of {@link DatapathElement} is removed from here. 
   *   It is now located in {@link org.vishia.cmd.JZtxtcmdScript.JZcmdDatapathElement} because it is necessary
   *   only for the JZcmd usage. This class is more simple in its functionality.
   * <li>2014-01-25 Hartmut chg: some methods of {@link DataAccessSet} are final now. Nobody overrides.  
   * <li>2013-12-26 Hartmut chg: {@link #createOrReplaceVariable(Map, String, char, Object, boolean)} instead setVariable(...)
   *   with type argument.
   * <li>2013-12-26 Hartmut chg: {@link #getData(String, Object, boolean, boolean, boolean, Dst)} returns the variable
   *   if the argument bVariable is set.
   * <li>2013-11-03 Hartmut chg: rename getData(...) to {@link #access(List, Object, Map, boolean, boolean, boolean, Dst)},
   *   return value Dst for setting. The {@link #storeValue(List, Map, Object, boolean)} may be obsolte now.
   * <li>2013-11-03 Hartmut chg: Handling of variable in {@link #getData(List, Object, Map, boolean, boolean, boolean)}
   * <li>2013-10-27 Hartmut chg: Definition of a String name [= value] in JZcmd is handled like assign. Stored with 
   *   {@link DataAccess#storeValue(List, Map, Object, boolean)} with special designation in {@link DataAccess.DatapathElement#whatisit}
   *   with 'new Variable' designation.
   * <li>2013-10-20 Hartmut new/chg: The start-variables are all of type {@link Variable} up to now. This concept is changed
   *   in {@link org.vishia.cmd.JZtxtcmdExecuter} originally. Any other application of this class have to wrapped its data
   *   in such an instance {@link Variable}, it is a low-cost effort. 
   * <li>2013-10-09 Hartmut new: {@link #storeValue(List, Map, Object, boolean)} put in a map, replaces the value.
   * <li>2013-09-14 Hartmut new: support of null as Argument.
   * <li>2013-08-18 Hartmut new: This class now contains the List of {@link #listDatapath} as only one attribute.
   *   Now this class can be used instead a <code>List<DataAccess.DatapathElement></code> as bundled instance.
   * <li>2013-08-18 Hartmut new: {@link DataAccessSet} is moved from the {@link org.vishia.zbatch.ZbatchGenScript}
   *   because it is more universal.
   * <li>2013-07-28 Hartmut chg: improvement of conversion of method arguments.
   * <li>2013-07-14 Hartmut chg: {@link #checkAndConvertArgTypes(List, Class[])} now checks super classes and interfaces,
   * <li>2013-07-14 Hartmut chg: Exception handling for invoked methods.
   * <li>2013-06-23 Hartmut new: {@link #invokeNew(DatapathElement)}.
   * <li>2013-03-26 Hartnut improved: {@link #getData(String, DataAccess.Variable, boolean, boolean)} Now accesses to all elements,
   *   also to enclosing and super classes.
   * <li>2013-03-26 Hartmut new: {@link #getDataFromField(String, DataAccess.Variable, boolean)}
   * <li>2013-03-26 Hartmut new: {@link #getEnclosingInstance(Object)}
   * <li>2013-03-26 Hartmut bugfix: {@link #getData(String, DataAccess.Variable, boolean, boolean)} has thrown an Exception if a existing
   *   element has a null-value. Instead it should return null. Exception only if the field is not found. 
   * <li>2013-03-23 Hartmut chg: {@link #checkAndConvertArgTypes(List, Class[])}: Now supports a (String[]) arg which is
   *   typical for a main(String[]) routine. General: The last formal argument can be an array, then all further
   *   non-array arguments are tried to build the element of it. 
   * <li>2013-03-10 Hartmut new: Now supports access to elements of the super class (TODO: outer classes).
   * <li>2013-01-13 Hartmut chg: {@link #getData(List, Object, Map, boolean, boolean)} can be invoked with null for dataPool
   *   to invoke new or static methods.
   * <li>2013-01-12 Hartmut new: {@link #checkAndConvertArgTypes(List, Class[])} improved, 
   *   new {@link #invokeStaticMethod(DatapathElement, Object, boolean, boolean)}
   * <li>2013-01-05 Hartmut new: reads $$ENV_VAR.
   * <li>2013-01-02 Hartmut new: Supports access to methods whith parameter with automatic cast from CharSequence to String and to File.
   *   Uses the {@link DatapathElement#fnArgs} and {@link #getData(List, Object, Map, boolean, boolean)}.
   * <li>2012-12-23 Hartmut chg, new: {@link #getStringFromObject(Object, String)} now uses a format string.
   * <li>2012-12-22 Hartmut new: {@link DatapathElement#constValue} as general possibility, usual for the first element of a path.
   * <li>2012-12-08 Hartmut new: {@link #getData(String, Object, boolean)} as subroutine in {@link #getData(List, Object, Map, boolean, boolean)}
   *   and able to use to get with non treed path, only direct but with all facilities to get from Map etc..
   * <li>2012-11-24 Hartmut new: {@link DatapathElement} for describing more complex path for access.
   * <li>2012-11-18 Hartmut new: {@link #setBit(int, int, boolean)} as little universal routine.
   * <li>2012-11-16 Hartmut new: {@link #getInt(Object)}, {@link #getFloat(Object)} from {@link ObjectValue}, last one is deprecated now.
   * <li>2012-11-04 Hartmut chg: parameter bContainer in getData(...): Sometimes a container is ispected
   *   to iterate though only one element is found, sometimes only a simple element is expected
   *   though a container is addressed maybe with one element. 
   * <li>2012-10-21 Hartmut created. Some algorithm are copied from {@link org.vishia.zbatch.ZbatchExecuter} in this class.
   *   That algorithm are able to use independent in some applications.
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
   * 
   * 
   */
  static final public String sVersion = "2023-12-27";


  /**Wrapper around the index as integer. An instance is member of {@link OutTextPreparer#varValues}. 
   * Necessary to detect a null reference. In Java Integer can be used too, but that is more explicitly. 
   */
  public static class IntegerIx {
    public final int ix;
    public IntegerIx(int value){ this.ix = value; }
    @Override public String toString() { return Integer.toString(this.ix); }
  }
  

  
  /**Interface to convert between data.
   */
  private static interface Conversion
  {
    /**Executes the conversion
     * @param src the source data
     * @return the result data.
     */
    Object convert(Object src); 
    
    /**Checks whether the value of source allows the conversion.
     * @param src The src value.
     * @return true if allowed
     */
    boolean canConvert(Object src); 
    
    /**This key is used as key in the Index to search the possible conversion.
     * It should be build in form: "GIVENTYPE:DSTTYPE" whereby GIVENTYPE is the result of TODO
     * @return
     */
    String key();
  }
  

  
  /**This inner class contains all possible automatic special conversions.
   * <ul>
   * <li>Note that conversions from an derived type to a reference of a basic type
   *   are detected automatically. Therefore a {@link #number2char} is sufficient for
   *   integer2char, short2char etc.
   * <li>Note that all simple types are boxed into there Wrapper types, both for input and output.
   * <li>Note that a {@link java.lang.Integer} is based on {@link java.lang.Number} etc.
   * </ul>
   */
  private static class Conversions {
    
    /**{@link CalculatorExpr.Value} to Integer. */
    protected static Conversion calcValue2int = new Conversion(){
      @Override public Object convert(Object src){
        return new Integer(((CalculatorExpr.Value)src).intValue());
      }
      @Override public boolean canConvert(Object src){ return true; }
      @Override public String key() { return "org.vishia.util.CalculatorExpr$Value:int"; }
      @Override public String toString(){ return "calcValue:int"; }
    };
    
    /**Long to Integer. */
    protected static Conversion long2int = new Conversion(){
      @Override public Object convert(Object src){
        return new Integer(((Long)src).intValue());
      }
      @Override public boolean canConvert(Object src){
        long val = ((Long)src).longValue();
        return  val <= 0x7fffffffL && val >= 0xFFFFFFFF80000000L;
      }
      @Override public String key() { return "java.lang.Long:int"; }
      @Override public String toString(){ return "long:int"; }
    };
    
    /**Integer to Character. Adequate to Number to Boolean, but a tick faster because the superclass should not be tested. */
    protected static Conversion int2char = new Conversion(){
      @Override public Object convert(Object src){
        return new Character((char)((Integer)src).intValue());
      }
      @Override public boolean canConvert(Object src){ 
        return true;
      }
      @Override public String key() { return "java.lang.Integer:char"; }
      @Override public String toString(){ return "int:double"; }
    };
    
    /**Integer to Boolean. Adequate to Number to Boolean, but a tick faster because the superclass should not be tested. */
    protected static Conversion int2bool = new Conversion(){
      @Override public Object convert(Object src){
        return new Boolean(((Integer)src).intValue()!=0);
      }
      @Override public boolean canConvert(Object src){ return true;}
      @Override public String key() { return "java.lang.Integer:boolean"; }
      @Override public String toString(){ return "int:long"; }
    };
    
    /**Integer to Byte. */
    protected static Conversion int2byte = new Conversion(){
      @Override public Object convert(Object src){
        return new Byte((byte)((Integer)src).intValue());
      }
      @Override public boolean canConvert(Object src){ 
        int val = ((Integer)src).intValue();
        return  val <= 0x7f && val >= 0xFFFFFF80;
      }
      @Override public String key() { return "java.lang.Integer:byte"; }
      @Override public String toString(){ return "int:long"; }
    };
    
    /**Integer to Short. */
    protected static Conversion int2short = new Conversion(){
      @Override public Object convert(Object src){
        return new Short((byte)((Integer)src).intValue());
      }
      @Override public boolean canConvert(Object src){ 
        int val = ((Integer)src).intValue();
        return  val <= 0x7fff && val >= 0xFFFF8000;
      }
      @Override public String key() { return "java.lang.Integer:short"; }
      @Override public String toString(){ return "int:long"; }
    };
    
    /**Integer to Long. */
    protected static Conversion int2long = new Conversion(){
      @Override public Object convert(Object src){
        return new Long(((Integer)src).intValue());
      }
      @Override public boolean canConvert(Object src){ return true;}
      @Override public String key() { return "java.lang.Integer:long"; }
      @Override public String toString(){ return "int:long"; }
    };
    
    /**Integer to Float. */
    protected static Conversion int2float = new Conversion(){
      @Override public Object convert(Object src){
        return new Float(((Integer)src).intValue());
      }
      @Override public boolean canConvert(Object src){ return true;}
      @Override public String key() { return "java.lang.Integer:float"; }
      @Override public String toString(){ return "int:float"; }
    };
    
    /**Integer to Double. */
    protected static Conversion int2double = new Conversion(){
      @Override public Object convert(Object src){
        return new Double(((Integer)src).intValue());
      }
      @Override public boolean canConvert(Object src){ return true;}
      @Override public String key() { return "java.lang.Integer:double"; }
      @Override public String toString(){ return "int:double"; }
    };
    
    /**Number to Character. */
    protected static Conversion number2char = new Conversion(){
      @Override public Object convert(Object src){
        return new Character((char)((Number)src).longValue());
      }
      @Override public boolean canConvert(Object src){ 
        return true;
      }
      @Override public String key() { return "java.lang.Number:char"; }
      @Override public String toString(){ return "int:double"; }
    };
    
    /**Double to Float. */
    protected static Conversion double2float = new Conversion(){
      @Override public Object convert(Object src){
        return new Float(((Double)src).floatValue());
      }
      @Override public boolean canConvert(Object src){ return true;}
      @Override public String key() { return "java.lang.Double:float"; }
      @Override public String toString(){ return "double:float"; }
    };
    
    /**Float to Double*/
    protected static Conversion float2double = new Conversion(){
      @Override public Object convert(Object src){
        return new Double(((Float)src).floatValue());
      }
      @Override public boolean canConvert(Object src){ return true;}
      @Override public String key() { return "java.lang.Float:double"; }
      @Override public String toString(){ return "float:double"; }
    };
    
    /**Number to Boolean. Note that Byte, Short, Integer, Long are Number. */
    protected static Conversion number2bool = new Conversion(){
      @Override public Object convert(Object src){
        return new Boolean(((Number)src).longValue() !=0);
      }
      @Override public boolean canConvert(Object src){
        return true;
      }
      @Override public String key() { return "java.lang.Number:bool"; }
      @Override public String toString(){ return "number:bool"; }
    };
    
    /**Any Object to Boolean. It tests obj != null. */
    protected static Conversion obj2bool = new Conversion(){
      @Override public Object convert(Object src){
        return new Boolean(src != null);
      }
      @Override public boolean canConvert(Object src){
        return true;
      }
      @Override public String key() { return "java.lang.Object:bool"; }
      @Override public String toString(){ return "obj:bool"; }
    };
    
    
    /**CharSequence to String. It uses src.toString(); */
    protected static Conversion charSequence2String = new Conversion(){
      @Override public Object convert(Object src){
        return src.toString();
      }
      @Override public boolean canConvert(Object src){
        return true;
      }
      @Override public String key() { return "java.lang.CharSequence:java.lang.String"; }
      @Override public String toString(){ return "CharSequence:String"; }
    };
    
    
    /**CharSequence to Character. It matches only if the CharSequence contains exactly 1 character. */
    protected static Conversion charSeq2char = new Conversion(){
      @Override public Object convert(Object src){
        return new Character(((CharSequence)src).charAt(0));
      }
      @Override public boolean canConvert(Object src){
        return ((CharSequence)src).length() ==1;
      }
      @Override public String key() { return "java.lang.CharSequence:char"; }
      @Override public String toString(){ return "CharSequence:char"; }
    };
    
    
    /**List container to appropriate array. */  
    protected static Conversion list2array = new Conversion(){
      @Override public Object convert(Object src){
        List<?> items = (List<?>)src;
        
        Object item = items.get(0);
        int size = items.size();
        Class<?> itemClass = item.getClass();
        Object[] ret = (Object[]) Array.newInstance(itemClass, size);
        int ix = -1;
        for(Object item1: items){
          ret[++ix] = item1;
        }
        return ret;
      }
      @Override public boolean canConvert(Object src){
        return true;
      }
      @Override public String key() { return null; }
      @Override public String toString(){ return "obj:obj"; }
    };

    
    /**Without conversion, returns src. */  
    protected static Conversion obj2obj = new Conversion(){
      @Override public Object convert(Object src){
        return src;
      }
      @Override public boolean canConvert(Object src){
        return true;
      }
      @Override public String key() { return null; }
      @Override public String toString(){ return "obj:obj"; }
    };
  }
  
  
  private static Map<String, Conversion> idxConversions = initConversion();
  
  /**Can be set immediately for debug approach. Set debug break on usage! */
  private static String debugIdent;
  
  private static String debugMethod;
  
  /**The description of the path to any data if the script-element refers data. It is null if the script element
   * does not refer data. If it is filled, the instances are of type {@link ZbnfDataPathElement}.
   * If it is used in {@link DataAccess}, its base class {@link DataAccess.DatapathElement} are used. The difference
   * are the handling of actual values for method calls. See {@link ZbnfDataPathElement#actualArguments}.
   */
  protected List<DataAccess.DatapathElement> listDatapath;
  
  protected DataAccess.DatapathElement oneDatapathElement;
  
  /**Creates a Datapath with given String.
   * The first character of an element determines the type, see {@link DatapathElement#set(String)}.
   * <ul>
   * <li>If the path does not start with $+%! then it is separated in path elements, the separator is the dot.
   * <li>If the path starts with $ it is an access to an environment variable.
   * <li>+ new
   * <li>% ! static method.
   * <li>
   * </ul> 
   * For example for a simple local variable in the datapool write "@name".
   * @param path
   * @throws ParseException 
   */
  public DataAccess(String path) throws ParseException{
    if("$+%!".indexOf(path.charAt(0))>=0){
      //only one element, environment variable, new Instance, static method.
      DatapathElement element = new DatapathElement(path);
      add_datapathElement(element);
    } else {
      //First element starts with '@' then from datapool.
      String[] pathElements = path.split("\\.");
      for(String sElement: pathElements){
        DatapathElement element = new DatapathElement(sElement);
        add_datapathElement(element);
      }
    }
  }
  
  
  /**Creates a Datapath with given String.
   * The first character of an element determines the type, see {@link DatapathElement#set(String)}.
   * <ul>
   * <li>If the path does not start with $+%! then it is separated in path elements, the separator is the dot.
   * <li>If the path starts with $ it is an access to an environment variable.
   * <li>+ new
   * <li>% ! static method.
   * <li>
   * </ul> 
   * For example for a simple local variable in the datapool write "@name".
   * @param path
   * @param cTypeNewVariable if A...Z then the last element will be designated with it.
   *   Then a new variable should be created in the parent's container with the access.
   * @throws ParseException 
   */
  public DataAccess(String path, char cTypeNewVariable) throws ParseException{
    assert(this.listDatapath==null && this.oneDatapathElement == null);
    String[] pathElements = path.split("\\.");
    DatapathElement element = null;
    for(int ii=0; ii < pathElements.length; ++ii){
      String sElement = pathElements[ii];
      element = new DatapathElement(sElement);
      add_datapathElement(element);
    }
    if(cTypeNewVariable >= 'A' && cTypeNewVariable <='Z' && element !=null){
      element.whatisit = cTypeNewVariable;  //from the last element.
    }
  }
  
  
  
  
  /**Creates a Datapath with given StringPartScan.
   * The first character of an element determines the type, see {@link DatapathElement#set(String)}.
   * <ul>
   * <li>If the path does not start with $+%! then it is separated in path elements, the separator is the dot.
   * <li>If the path starts with $ it is an access to an environment variable.
   * <li>+ new
   * <li>% ! static method.
   * <li>
   * </ul> 
   * For example for a simple local variable in the datapool write "@name".
   * @param path
   * @param nameVariables if not null then the argument names can start with this given names. The first identifier is searched here.
   *   If it is not found, firstly reflData are used to find it as static variable. 
   *   If it is not possible then the identifier is added to the nameVariables map. Hence used names are documented
   *   to outside and can be evaluate on invocation before {@link #access(Object, boolean, boolean, Object[])}.
   *   This feature is introduced with {@link OutTextPreparer} to support simple expressions in a fast way. 
   *   If null then the arguments are parsed as DataAccess.
   * @param reflData type wich can contain a static member with the required name in sDatapath if the name is not found in variables
   *   If nameVariables are given and this argument is not null, then an identifier will be searched in this
   *   reflData to get its Access routine. 
   *   This feature is introduced with {@link OutTextPreparer} to support simple expressions in a fast way.  
   * @param cTypeNewVariable if A...Z then the last element will be designated with it.
   *   Then a new variable should be created in the parent's container with the access.
   * @throws ParseException on errors on sp, on not found variables too. 
   */
  public DataAccess(StringPartScan sp, Map<String, DataAccess.IntegerIx> nameVariables
  , Class<?> reflData, char cTypeNewVariable) throws ParseException {
    assert(this.listDatapath==null && this.oneDatapathElement == null);
    boolean bFirst = true;
    do {
      DatapathElement element = new DatapathElement(sp, nameVariables, reflData, bFirst);
      if(cTypeNewVariable >= 'A' && cTypeNewVariable <='Z' && element !=null){
        element.whatisit = cTypeNewVariable;  //from the last element.
      }
      add_datapathElement(element);
      bFirst = false;
    } while(sp.scan(".").scanOk());
  }
  
  
  
  /**Empty constructor. Use {@link #add_datapathElement(DatapathElement)} to determine the datapath. */
  public DataAccess(){}
  
  
  
  /**Returns the datapath of this access to check details. 
   * If only one element is stored, an independent List with this element is returned. */
  public final List<DataAccess.DatapathElement> datapath(){ 
    if(this.oneDatapathElement !=null) { 
      List<DataAccess.DatapathElement> list = new LinkedList<>();
      list.add(this.oneDatapathElement);
      return list;
    } else {
      return this.listDatapath; // maybe null if empty
    }
  }
  
  
  /**Returns the only one element in the DataAccess path if it is only one.
   * If there are more elements (deeper access) this returns null. 
   * Then {@link #datapath()} should be used to iterate over all elements.
   * @return
   */
  public final DataAccess.DatapathElement dataPath1 () {
    return this.oneDatapathElement;
  }
  
  /**Sets the datapath while adding one element after another.
   * More elements are necessary if the datapath has references.
   * This operation is not thread safe. For multithreading use outer synchronize.
   * @param item
   * @see {@link DatapathElement}
   */
  public void add_datapathElement(DatapathElement item){ 
    if(this.listDatapath == null){
      if(this.oneDatapathElement==null) {
        this.oneDatapathElement = item;
      } else {
        this.listDatapath = new ArrayList<DataAccess.DatapathElement>();
        this.listDatapath.add(this.oneDatapathElement);
        this.oneDatapathElement = null;
        this.listDatapath.add(item); 
      }
    }
  }


  

  /**Searches the Object maybe invoking some methods which is referred with this instances, {@link #listDatapath}. 
   * @param dataRoot Either a Map<String, ?> or any other Object which is the root for access.
   *  it is the object where the path starts from. A Map<?,?> which's key is not a String is not admissible. 
   * @param bContainer true then returns a found container or build one.
   * @param varValues maybe null or array of some variables which are sorted by parameter idxVariables 
   *   in {@link #DataAccess(StringPartScan, Map, Class, char)}
   * @return Maybe null only if the last reference refers null. 
   * @throws Exception on any not found or etc.
   */
  public Object access( Object dataRoot , boolean accessPrivate, boolean bContainer
  , Map<String, IntegerIx> nameVariables, Object[] varValues) 
  throws Exception{
    if(this.oneDatapathElement!=null) {
      boolean bVariable = false;
      Object data1 = access(this.oneDatapathElement, dataRoot, accessPrivate, bContainer, nameVariables, varValues, false, null);
      if(data1 instanceof Variable<?> && !bVariable){  //use the value of the variable.
        @SuppressWarnings("unchecked") Variable<Object> var = (Variable<Object>)data1;
        data1 = var.value;
      }
      if(data1 == null) return null;
      else if(bContainer){
        //should return a container
        if(data1.getClass().isArray()) return data1;
        if(data1 instanceof Iterable<?> || data1 instanceof Map<?,?>) return data1;
        else {
          //Build a container if only one element is addressed.
          List<Object> list1 = new LinkedList<Object>();
          list1.add(data1);
          return list1;
        }
      }
      else return data1;
    }
    else {
      return access(listDatapath, dataRoot, accessPrivate, bContainer, nameVariables, varValues, false, null);
    }
  }

  
  
  
  
  
  
  
  
  /**Stores the given value in the element determined by the data path, maybe create a new Variable therewith.
   * 
   * <ul>
   * <li>If the last or only one element of the path is designated with 'A'...'Z' in its 
   *   {@link DatapathElement#whatisit}  {@link Variable#type()}, this variable is created newly. 
   *   The destination before, that is either the param variables or the result of the path before,
   *   have to be a <code>Map< String, DataAccess.Variables></code>.
   * <li>If the path exists and refers to a {@link Variable} then the value of the variable is replaced.   
   * <li>If the destination referred by the path exists and it is a {@link java.util.List} or
   *   a {@link java.lang.Appendable}, then the value is added respectively appended to it.
   * <li>If the destination referred by the path exists, and the path before is a Map, then
   *   the element of the map is replaced.
   * <li>If the destination referred by the path does not exists, and the path before is a Map<String, Type>, then
   *   the element of the map is put. 
   * <li>If the path consists of more as one element and any parent element does not exists too,
   *   it is added only if its parent is of type {@link java.util.Map} with a String as key.
   * <li>The parent of the first element is the variables container. It is of type Map. 
   * <li>If the path consists of only one element and this element is a new one, it is created 
   *   as a new variable in the variables container.
   * <li>If the path consists of some elements and all of them are Map or one of them does not exist,
   *   a Tree of Maps is build in variable.      
   * </ul>
   * 
   * @param path
   * @param dataRoot Either a Map<String, ?> or any other Object which is the root for access.
   *  it is the object where the path starts from. A Map<?,?> which's key is not a String is not admissible. 
   * @param value
   * @throws IllegalAccessException
   * @throws IOException if append fails.
   * @throws IllegalAccessException if a field exists but can't access. Note that private members can be accessed.
   */
  public static void storeValue(List<DatapathElement> path, Object dataRoot, Object value, boolean bAccessPrivate) 
  throws Exception {
    Dst dst = new Dst();
    //accesses the data object with given path. 
    //If it is a Variable, return the Variable, not its content.
    //If it is not a Variable, the dst contains the Field.
    Object o = access(path, dataRoot, bAccessPrivate, false, null, null, true, dst);
    if(o instanceof Variable<?>){
      @SuppressWarnings("unchecked")
      Variable<Object> var = (Variable<Object>)(o); 
      var.setValue(value);
    } else {
      dst.set(value);  //try to set the value to the field. If the type is not proper, throws an exception.
    }
  }

  


  /**Stores a value in the given field or with the given method into the data instance.
   * <ul>
   * <li>If the destination is instanceof {@link Variable} its value will be changed.
   * <li>If the destination is a field inside data, the value will set to the field. 
   * </ul> 
   * @param path describes a field or method of data. It is the 'destination'.
   * @param data instance where path is found. 
   * @param value value
   * @param bAccessPrivate
   * @throws Exception any exception is possible because faulty path, faulty data types etc.
   */
  public static void storeValue(DatapathElement path, Object data, Object value, boolean bAccessPrivate) 
  throws Exception {
    Dst dst = new Dst();
    //accesses the data object with given path. 
    //If it is a Variable, return the Variable, not its content.
    //If it is not a Variable, the dst contains the Field.
    Object o = access(path, data, bAccessPrivate, false, null, null, true, dst);
    if(o instanceof Variable<?>){
      @SuppressWarnings("unchecked")
      Variable<Object> var = (Variable<Object>)(o); 
      var.setValue(value);
    } else {
      dst.set(value);  //try to set the value to the field. If the type is not proper, throws an exception.
    }
  }






  /**Stores the value in the given path. See {@link #storeValue(List, Map, Object, boolean)}.
   * @param dataRoot Either a Map<String, ?> or any other Object which is the root for access.
   *  it is the object where the path starts from. A Map<?,?> which's key is not a String is not admissible. 
   * @param value
   * @param bAccessPrivate
   * @throws Exception
   */
  public void storeValue( Object dataRoot, Object value, boolean bAccessPrivate) 
  throws Exception
  {
    storeValue(listDatapath, dataRoot, value, bAccessPrivate);
  }
  
  
  

  
  
  
  
  
  /**This method initializes the internal conversion index. It is only public to document
   * which conversions are possible. One can invoke the method and view the result.
   * The keys in the map describe possible conversions <code>fromType:toType</code>.
   * @return An index table for conversion. Used internal.
   */
  static Map<String, Conversion> initConversion(){
    Map<String, Conversion> conversion1 = new TreeMap<String, Conversion>();
    conversion1.put("org.vishia.util.CalculatorExpr$Value:int", Conversions.calcValue2int);
    conversion1.put("java.lang.Long:int", Conversions.long2int);
    conversion1.put("java.lang.Integer:boolean", Conversions.int2bool);
    conversion1.put("java.lang.Integer:byte", Conversions.int2byte);
    conversion1.put("java.lang.Integer:short", Conversions.int2short);
    conversion1.put("java.lang.Integer:int", Conversions.obj2obj);
    conversion1.put("java.lang.Integer:long", Conversions.int2long);
    conversion1.put("java.lang.Integer:float", Conversions.int2float);
    conversion1.put("java.lang.Integer:double", Conversions.int2double);
    conversion1.put("java.lang.Integer:char", Conversions.int2char);
    conversion1.put("java.lang.Float:float", Conversions.obj2obj);
    conversion1.put("java.lang.Float:double", Conversions.float2double);
    conversion1.put("java.lang.Double:double", Conversions.obj2obj);
    conversion1.put("java.lang.Double:float", Conversions.double2float);
    conversion1.put("java.lang.Number:boolean", Conversions.number2bool);
    conversion1.put("java.lang.Number:char", Conversions.number2char);
    conversion1.put("java.lang.Boolean:boolean", Conversions.obj2obj);  //returns the Boolean itself
    conversion1.put("java.lang.Object:boolean", Conversions.obj2bool);  //checks src !=null
    conversion1.put("java.lang.CharSequence:char", Conversions.charSeq2char);
    conversion1.put("java.lang.CharSequence:java.lang.String", Conversions.charSequence2String);
    return conversion1;
  }
  
  
  
  private final static Class<?> getClass(String name){
    try{
      return Class.forName(name);
    } catch(Exception exc){
      return null;
    }
  }
  
  
  /**Splits a String given path to elements.
   * @param path If starts with "&@$" it is the type. 
   * @param whatisit only used if path does not start with type.
   * @return list.
   * @throws ParseException 
   */
  public static List<DatapathElement> expandElements(CharSequence path, char whatisit) throws ParseException{
    List<DatapathElement> list = new LinkedList<DatapathElement>();
    int pos;
    char type = path.charAt(0);
    if("&@$".indexOf(type)>=0){      // only for the first element: Do the same as in new DataPathElement
      pos = 0;                         // but use whatisit instead '.' if one of this chars are not given. 
    } else {
      pos = -1;
      type = whatisit;
    }
    int length = path.length();
    do{
      int end = StringFunctions.indexOf(path, '.', pos+1);
      if(end < 0){ end = length; }
      String se = path.subSequence(pos+1, end).toString();
      DatapathElement e = new DatapathElement(se);
      e.whatisit = type;
      type = '.';
      list.add(e);
      pos = end;
    } while(pos < length);
    return list;
  }
  

  
  
  
  /**Accesses data with a String given path. The path can contain more elements
   * separated with dot. A first char can define from what to read: For example
   * <ul>
   * <li>"$ENV": From an environment variable
   * <li>"@variable.element": Read start instance form dataPool 
   * <li>"%java.package.Class.operation()": invokes a static method.
   * <li>"element": read from dataRoot. Read via get(element) if dataRoot is a Map<String, ?>  
   * <li>"reference.element": read from dataRoot, referenced instance. 
   * </ul>
   * @param datapathArg 
   * @param dataRoot Either a Map<String, ?> or any other Object which is the root for access.
   *  it is the object where the path starts from. A Map<?,?> which's key is not a String is not admissible. 
   * @param accessPrivate
   * @param bContainer
   * @param bVariable
   * @param dst
   * @return
   * @throws Exception
   */
  public static Object access(
      CharSequence datapathArg
      , Object dataRoot
      //, Map<String, DataAccess.Variable<Object>> dataPool
      , boolean accessPrivate
      , boolean bContainer
      , boolean bVariable
      , Dst dst
  ) 
  throws Exception 
  {
    if(StringFunctions.startsWith(datapathArg, "%")) {
      //If access to a static operation is given, do not expand. 
      DataAccess.DatapathElement element = new DataAccess.DatapathElement(datapathArg.toString());
      return access(element, dataRoot, accessPrivate, bContainer, null, null, bVariable, dst);
    }
    else {
      //Expand the path, it accesses maybe via more as one instance.
      List<DatapathElement> list = expandElements(datapathArg, '.');
      return access(list, dataRoot, accessPrivate, bContainer, null, null, bVariable, dst);
    }
  }

  
  
  
  
  /**Universal method to accesses data. 
   * The argument datapathArg contains elements, which describes the access path. 
   * <br><br>
   * The {@link DatapathElement#whatisit} is used like:
   * <ul>
   * <li>The elements of datapath describe the access to data. Any element before supplies a reference for the path 
   *   of the next element.
   * <li><code>$</code>: The datapath can start with an element designated with {@link DatapathElement#whatisit} == '$'. 
   *   It describes an access to an <em>environment variable</em>.
   *   The the variable is searched first in the given dataPool with an additional '$' on start of identifier.
   *   Note that the name of the variable ({@link DatapathElement#ident}) does not start with that '$' itself.
   *   If it is found, the dataPool contains this environment variable, it is prior.
   *   If the environment variable is not found in the datapool (a normal case), then the 
   *   variable is searched in the environment variables of the operation system. Its String representation is returned.
   *   If the environment variable is not found, null is returned. 
   *   The datapath should contain only this one element. Only this first element of the datapath is used. 
   * <li><code>@</code> or <code>.</code>: A variable, {@link DatapathElement#ident} contains the name of the field. It is searched as a 
   *   {@link java.lang.reflect.Field} in the given instance of the parent level, firstly in dataRoot.
   * <li><code>@</code> or <code>.</code>: and {@link DatapathElement#ident} == "[]" It should be the last element of the path.
   *   Then the length of the array of the path before is returned, or the size of a container.
   * <li><code>A..Z a..z</code>: If the only one first element or the last element is designated with {@link DatapathElement#whatisit} == 'A' .. 'Z'
   *   or 'a' ...'z' a new Variable will be created in the given datapool. 
   *   The character 'A'...'Z' describes the type. If lower case is written then the variable is created as const.  
   * <li><code>+</code>: The only one first element with {@link DatapathElement#whatisit} == '+' is the creation of instance maybe with or without arguments 
   *   in {@link DatapathElement#fnArgs}
   * <li><code>%</code>: The only one first element with {@link DatapathElement#whatisit} == '%' is a call of a static routine maybe with or without arguments 
   *   in {@link DatapathElement#fnArgs}
   * <li><code>(</code>: An element with {@link DatapathElement#whatisit} == '(' is a method invocation maybe with or without arguments.
   *   in {@link DatapathElement#fnArgs}
   * <li>If the instance of the parent level is instanceof {@link java.util.Map} then the element is searched by name
   *   as member of this map. The key of the map should be instanceof String.
   * <li>If an element is not found a {@link NoSuchFieldException} or {@link NoSuchMethodException} is returned.
   * <li>If any method is invoked and this method throws an Exception, a {@link InvocationTargetException} is thrown.       
   * </ul>
   * <br><br><b>Examples</b>
   * <ul>
   * <li>datapath = <code>[myField:.]</code>: The content respectively referenced instance of the field 
   *   <code>myField</code> inside the dataRoot instance is returned.
   * <li>datapath = <code>[myReference:., myField:.]</code>: The content respectively referenced instance 
   *   of the field <code>myReference</code> inside the dataRoot instance is read. From that instance the
   *   <code>myField</code> content of referenced instance is returned.
   * </ul>  
   * <br><br>
   * <b>Variable</b>:<br>
   * {@link Variable} are designated especially for referencing from one or more Map<String, Variable>. 
   * If the value should be changed, the value is changed inside the {@link Variable#value()}. Therewith all references
   * sees the new value. This method can deal especially with Variable in Map-container additionally to any other accesses.
   * <br><br>
   * If an element of the 'datapath' argument is designated with {@link DatapathElement#whatisit} = 'A' .. 'Z', then 
   * a new variable is created in the context. The context should be a Map<String, Variable>. 
   * <br><br>
   * <b>Access with an element with {@link DatapathElement#whatisit} == '.'</b>:<br>
   * The  {@link DatapathElement#ident} may determine a field of the current data reference or it may be a key for a indexed container.
   * The {@link #getData(String, DataAccess.Variable, boolean, boolean)} is invoked, see there for further explanation. 
   * <br><br>
   * <b>Calculation of arguments</b>:<br>
   * This routine calculates all method's arguments it an expression is given in the datapathElement
   * in {@link DatapathElement#addArgumentExpression(CalculatorExpr)}.
   * before the method is called. The expression to calculate is stored in 
   * {@link DatapathElement#fnArgsExpr} whereby the calculated results is stored in {@link DatapathElement#fnArgs}.
   * <br><br>
   * <b>Assignment of arguments of methods or constructor</b>:<br>
   * If the method or class is found per name, all methods with this name respectively all constructors are tested 
   * whether they match to the {@link DatapathElement#fnArgs}. The number of args should match and the types should be compatibel.
   * See {@link #checkAndConvertArgTypes(List, Class[])}.
   * 
   * @param dataRoot Either a Map<String, ?> or any other Object which is the root for access.
   *  it is the object where the path starts from. A Map<?,?> which's key is not a String is not admissible. 
   * @param accessPrivate if true then private data are accessed too. The accessing of private data may be helpfully
   *  for debugging. It is not recommended for general purpose! The access mechanism is given with 
   *  {@link java.lang.reflect.Field#setAccessible(boolean)}.
   * @param bVariable if true then return the found {@link Variable} and not its content. If false then return
   *   the {@link Variable#value()} if a variable is the last element.
   * @param bContainer If the element is a container, returns it. Elsewhere build a List
   *    to return a container for iteration with only the found element.
   *    A container is any object implementing java.util.Map or java.util.Iterable or an Array.
   * @param nameVariables necessary on indirect access to variable via String, construct  such as &lt;&&(path)>
   *   can be null if not used, should be null especially if varValues == null
   * @param varValues maybe null or array of some variables which are sorted by parameter idxVariables 
   *   in {@link #DataAccess(StringPartScan, Map, Class, char)}
   * @param dst If not null then fill the last {@link Field} and the associated Object in the dst.
   *   It can be used to set the field with a new value.    
   * @return Any data object addressed by the path. Returns null if the last datapath element refers null.
   * <ul>
   * <li>null: returns null
   * <li>Variable: bVariable = true: returns it
   * <li>Variable: bVariable = false: access to its {@link Variable#value()}, then the other rules.
   * <li>Iterable: returns it
   * <li>Map: returns it
   * <li>any Object: bContainer = true: returns a {@link List} with this Object as member.
   * <li>any Object: bContainer = false: returns it 
   * <li>Not found: throws an {@link NoSuchFieldException} or {@link NoSuchMethodException}
   * <li>Any Exception while invocation of methods: throws it. 
   * </ul>
   * @throws ReflectiveOperationException 
   * @throws Throwable 
   * @throws IllegalArgumentException if the datapath does not address an element. The exception message contains a String
   *  as hint which part does not match.
   */
  public static Object access(
      List<DatapathElement> datapathArg
      , Object dataRoot
      //, Map<String, DataAccess.Variable<Object>> dataPool
      , boolean accessPrivate
      , boolean bContainer
      , Map<String, IntegerIx> nameVariables
      , Object[] varValues
      , boolean bVariable
      , Dst dst
  ) 
  //throws ReflectiveOperationException  //only Java7
  throws Exception
  { //final List<DatapathElement> datapath;
    Iterator<DatapathElement> iter = datapathArg.iterator();
    DatapathElement element = iter.next();
    if("+%".indexOf(element.whatisit) <0 && element.ident !=null && element.ident.contains(".")){ //new and static needs . in datapath
      final List<DatapathElement> datapath = expandElements(element.ident, element.whatisit);
      iter = datapath.iterator();  //use that. ignore all other elements of dataPathArg, they should not any.
      element = iter.next();
    } else {
      //datapath = datapathArg;
    }
    
    Object data1 = dataRoot;  //the currently instance of each element.
    while(element !=null){
      //has a next element
      if(debugIdent !=null && element.ident !=null && element.ident.equals(debugIdent)){
        debug();
      }
      //====>
      data1 = access(element, data1, accessPrivate, bContainer, nameVariables, varValues, bVariable, dst);
      element = iter.hasNext() ? iter.next() : null;
    }//while
    //return
    if(data1 instanceof Variable<?> && !bVariable){  //use the value of the variable.
      @SuppressWarnings("unchecked") Variable<Object> var = (Variable<Object>)data1;
      data1 = var.value;
    }
    if(data1 == null) return null;
    else if(bContainer){
      //should return a container
      if(data1.getClass().isArray()) return data1;
      if(data1 instanceof Iterable<?> || data1 instanceof Map<?,?>) return data1;
      else {
        //Build a container if only one element is addressed.
        List<Object> list1 = new LinkedList<Object>();
        list1.add(data1);
        return list1;
      }
    }
    else return data1;
  }

  
  /**Access to the given element, used internally, also able to use for one element form extern.
   * @param element describes the access, can be a field, operation, environment variable or all possibilities
   * @param data1 If it is instanceof Class, then static access. Can be null for instance access, then output null.
   * @param accessPrivate
   * @param bContainer
   * @param nameVariables
   * @param varValues
   * @param bVariable
   * @param dst
   * @return The accessed content. 
   * @throws Exception
   */
  public static Object access(
      DatapathElement element
      , Object data1
      //, Map<String, DataAccess.Variable<Object>> dataPool
      , boolean accessPrivate
      , boolean bContainer
      , Map<String, IntegerIx> nameVariables
      , Object[] varValues
      , boolean bVariable
      , Dst dst
  ) throws Exception {
    boolean bStatic;
    if(element.reflAccess !=null) {
      Debugutil.stop();
    }
    //                                 // Hint: data1 can be null, if the previous access has deliverd null
    if(data1 instanceof Class){
      bStatic = true;              //If a class type is given as argument, static accesses are supported.
    } else if(data1 instanceof Variable<?>){
          @SuppressWarnings("unchecked") Variable<Object> var = (Variable<Object>)data1;
      if(var.type == 'C'){
        bStatic = true;
      } else {
        bStatic = false;
      }
      data1 = var.value;  //take the content of a variable!
    } else {
      bStatic = false;
    }
    //
    switch(element.whatisit) {
      case '&': {
        Object arg = element.args[0].calc(nameVariables, varValues);
        assert(arg instanceof String);
        if(nameVariables !=null) {
          IntegerIx ixvar = nameVariables.get((String)arg); 
          if(ixvar !=null) {
            data1 = varValues[ixvar.ix];
          } else {
            throw new IllegalArgumentException(" variable with name in &(>>" + arg + "<<) not found");
          }
        } else if(data1 instanceof Map) {
          Debugutil.todo();
        }
      } break;
      case '@': case '.': {
        if(bStatic){
          data1 = getDataFromField(element.ident, null, accessPrivate, (Class<?>)data1, dst, 0); 
        } else {
          if(data1 !=null){     // reference null because access before, remain null as return.
            //retain a Variable.
            data1 = getDataPriv(element.ident, data1, accessPrivate, bContainer, true /*bVariable*/, dst);
          }
        }
      } break;
      case '+': {  //create a new instance, call constructor
        data1 = invokeNew(element);
      } break;
      case '(': {
        if(data1 !=null){
          Class<?> clazz = bStatic && data1 instanceof Class<?> ? (Class<?>)data1: data1.getClass();
          data1 = invokeMethod(element, clazz, data1, accessPrivate, varValues, false); 
        }
        //else: let data1=null, return null
      } break;
      case '%': { 
        data1 = element.operation_ || element.args !=null || element.fnArgs !=null 
          ? data1 = invokeMethod(element, null, data1, accessPrivate, varValues, false)//invokeStaticMethod(element, varValues) 
          : getStaticValue(element); 
      } break;
      case '$': {
        if((data1 instanceof Map<?,?>)){  //should be Map<String, Variable>
          @SuppressWarnings("unchecked")
          Map<String, DataAccess.Variable<Object>> dataPool = (Map<String, DataAccess.Variable<Object>>)data1;
          data1 = dataPool.get("$" + element.ident);
        }
        if(data1 == null){
          data1 = System.getenv(element.ident);
        }
        if(data1 == null) {
          data1 = System.getProperty(element.ident);  //read from Java system property
        }
        if(data1 == null) throw new NoSuchElementException("DataAccess - environment variable not found: >>" + element.ident + "<<");
      } break;
      default: {
        final boolean bConstNewVariable;
        char whatisit = element.whatisit;
        if(element.whatisit >='a' && element.whatisit <='z'){
          bConstNewVariable = true;
          whatisit -= ('a' - 'A');
        } else {
          bConstNewVariable = false;
        }
        if(whatisit >='A' && whatisit <='Z') {
          //It is a new defined variable. 
          if(data1 instanceof Map<?,?>){ //unable to check generic type.
            //it should be a variable container!
            @SuppressWarnings("unchecked")
            Map<String, DataAccess.Variable> varContainer = (Map<String, DataAccess.Variable>)data1;
            Variable<Object> newVariable = new DataAccess.Variable<Object>(element.whatisit, element.ident, null, bConstNewVariable);
            varContainer.put(element.ident, newVariable);
            data1 = newVariable;
          } else {
            throw new IllegalArgumentException("DataAccess.storeValue - destination should be Map<String, DataAccess.Variable>; " + dst);
          }
        } 
        else if(bStatic){
          data1 = getDataFromField(element.ident, null, accessPrivate, (Class<?>)data1, dst, 0); 
        } else {
          if(data1 !=null){
            data1 = getDataPriv(element.ident, data1, accessPrivate, bContainer, bVariable, dst);
          }
        }
      }//default

    }//switch
    if(data1 !=null && element.indices !=null) { //since 2019-06: check indices on any access, not only default
      data1 = getArrayElement(data1, element.indices);
    }
    return data1;
  } 
  
  
  
  
  /**Invokes the static method which is described with the element.
   * @param element its {@link DatapathElement#whatisit} == '%'.
   *   The {@link DatapathElement#identArgJbat} should contain the full qualified "packagepath.Class.methodname" separated by dot.
   * @return the return value of the method
   * @throws NoSuchMethodException 
   */
  protected static Object invokeNew( DatapathElement element) throws Exception 
  { final Class<?> clazz;
    if(element instanceof DatapathElementClass && ((DatapathElementClass)element).clazz !=null){
      clazz = ((DatapathElementClass)element).clazz;
    } else {
      String sClass = element.ident;
      if(debugIdent !=null && sClass.equals(debugIdent))
        debug();
      ClassLoader classloader = getClassLoader(element);
      clazz = classloader.loadClass(sClass);
    }
    Constructor<?>[] methods = clazz.getConstructors();
    boolean bOk = false;
    Object data1 = null;
    if(methods.length==0 && element.fnArgs ==null){
      //only a default constructor, it is requested
      data1 = clazz.newInstance();
      bOk = data1 !=null;
    } else {
      for(Constructor<?> method: methods){
        bOk = false;
        Class<?>[] paramTypes = method.getParameterTypes();
        Object[] actArgs = checkAndConvertArgTypes(element.fnArgs, paramTypes);
        if(actArgs !=null){
          bOk = true;
          try{ 
            data1 = method.newInstance(actArgs);
          } catch(IllegalAccessException exc){
            CharSequence stackInfo = Assert.stackInfo(" called ", 3, 5);
            throw new NoSuchMethodException("DataAccess - method access problem: >>" 
              + clazz.getName() + "." + element.ident + "(...)<<\n ..." + stackInfo);
          } catch(InstantiationException exc){
            CharSequence stackInfo = Assert.stackInfo(" called ", 3, 5);
            throw new NoSuchMethodException("DataAccess - new invocation problem: >>" 
              + clazz.getName() + "." + element.ident + "(...)<<\n ..." + stackInfo);
          }
          break;  //method found.
        }
      }
    }
    if(!bOk) {
      StringBuilder msg = new StringBuilder(1000);
      msg.append("DataAccess - constructor not found in class: >>")
         .append(clazz.getName()).append(", ") .append(element.ident) .append("(");
      if(element.fnArgs !=null) {
        for(Object arg: element.fnArgs) {
          msg.append(arg.getClass()).append(", ");
        }
      } else if(element.args !=null) {
        for(CalculatorExpr.Operand arg: element.args) {
          msg.append(arg.textOrVar).append(", ");
        }
      }
      
      msg.append(")<<\n ... stackInfo: ");
      CharSequence stackInfo = Assert.stackInfo(msg, 3, 8);
      throw new NoSuchMethodException(stackInfo.toString());
    }
    return data1;    
  }
  
  
  
  
  
  /**Prepares firstly the arguments of the method and invokes the method.
   * The arguments of the method comes from access to 'varValues' maybe with sub elements or access to elements from obj
   * due to the given {@link DatapathElement#args} which contains the access algorithm. 
   * The association to the arguments of the operation in argument 'args' is done here 
   * before {@link #invokeMethod(DatapathElement, Class, Object, boolean, boolean, Object[])} is called.
   * 
   * @param element its {@link DatapathElement#whatisit} == '('.
   *   The {@link DatapathElement#ident} is the "methodname".
   * @param clazz the Class instance where the method should be found. 
   *   For non-static methods the relation obj instanceof clazz should be valid, 
   *   elsewhere an exception is thrown.
   * @param obj The instance which is the instance of the method. The obj
   *   is used as first argument of {@link Method#invoke(Object, Object...)}.
   *   For static methods obj is not used. It may be null. 
   * @param accessPrivate true then uses private methods too
   * @param varValues maybe null or array of some variables which are sorted by parameter idxVariables 
   *   in {@link #DataAccess(StringPartScan, Map, Class, char)}
   *   used to get the value due to {@link DatapathElement#args} as their DataPath (stored in {@link CalculatorExpr.Operand}
   * @param bNoExceptionifNotFound if the then does not a NochSuchMethodException if the method was not found.
   *   This is a special flag if the method is optional.
   * @return the return value of the method, null if the method is void.
   * @throws InvocationTargetException 
   * @throws NoSuchMethodException 
   */
  public static Object invokeMethod(      
      DatapathElement element
    , Class<?> clazz  
    , Object obj
    , boolean accessPrivate
    , Object[] varValues
    , boolean bNoExceptionifNotFound
  ) throws InvocationTargetException, NoSuchMethodException, Exception {
    Object[] args = null;
    if(element.ident.equals("toStringDependingNext"))
      Debugutil.stop();
    if(element.fnArgs == null) { //only if fnArgs not given from call, 
      //then evaluate here.
      if(element.args !=null) {
        args = new Object[element.args.length];
        int ix = -1;
        for(CalculatorExpr.Operand expr: element.args) {   // expr contains the ix in varValues
          args[++ix] = expr.calc(null, varValues);         // get the proper varValue[ix] due to expr
        }
      }
    }
     return invokeMethodWithGivenArgValues(element, clazz, obj, accessPrivate, bNoExceptionifNotFound, args);
  }  
  
  
  /**Invokes the method which is described with the element with given immediately calling arguments. 
   *
   * @param element its {@link DatapathElement#whatisit} == '('.
   *   The {@link DatapathElement#ident} is the "methodname".
   * @param clazz the Class instance where the method should be found. 
   *   For non-static methods the relation obj instanceof clazz should be valid, 
   *   elsewhere an exception is thrown.
   * @param obj The instance which is the instance of the method. The obj
   *   is used as first argument of {@link Method#invoke(Object, Object...)}.
   *   For static methods obj is not used. It may be null. But then clazz should be given 
   *   or the Method is given already in param element in {@link DatapathElement#reflAccess}.
   * @param accessPrivate true then also access private and protected operations
   * @param bNoExceptionifNotFound If true then an output text is returned instead throwing an exception
   * @param args if not null then this arguments are used.
   * @return the return value of the method, null if the method is void.
   * @throws InvocationTargetException on exceptions in the operation
   * @throws NoSuchMethodException if not found 
   * @throws Exception
   * @since 2023-12-17 renamed from "invokeMethod" which is able to confuse with {@link #invokeMethod(DatapathElement, Class, Object, boolean, Object[], boolean)}.
   * 
   */
  public static Object invokeMethodWithGivenArgValues(
    DatapathElement element
  , Class<?> clazz  
  , Object obj
  , boolean accessPrivate
  , boolean bNoExceptionifNotFound
  , Object[] args
  ) throws InvocationTargetException, NoSuchMethodException, Exception {
    Object data1 = null;
    if(element.ident.equals("prcEvChain"))
      Debugutil.stop();
    Method method = (Method) element.reflAccess;           // primary use given method, ==null if not given
    Class<?> clazz1 = method !=null ? method.getDeclaringClass() : clazz == null ? obj.getClass() : clazz;
    Class<?> clazzcheck = clazz1;   //in loop superclass checked etc.
    Object[] givenArgs = args !=null ? args : element.fnArgs;
    if(element.ident.equals("new_draw_polygon"))
      Debugutil.stop();
    while( method == null && clazzcheck !=null ){          // search the method if not given
      if(accessPrivate || (clazzcheck.getModifiers() & Modifier.PUBLIC) !=0){
        Method[] methods = accessPrivate ? clazzcheck.getDeclaredMethods() : clazzcheck.getMethods();
        for(Method methodCheck: methods){                  // check all methods
          if(methodCheck.getName().equals(element.ident)){
            if(debugMethod !=null) {
              if(debugMethod.equals(element.ident) || debugMethod.equals("")){
                debug();
                if(debugMethod.equals("")){
                  debugMethod = null; //only one time after set.
                }
              }
            }                                              // with correct name found, check argument types
            Class<?>[] paramTypes = methodCheck.getParameterTypes();
            Object[] actArgs = checkAndConvertArgTypes(givenArgs, paramTypes);
            if(actArgs !=null){
              method = methodCheck;                        // also the arguments are proper
              break;
            }
          }
        }
      }
      if(method == null) {                                 // not found in this level
        clazzcheck = clazzcheck.getSuperclass();           // go into super class
      }
    } // while
    boolean bOk = false;
    CharSequence sError = "Method not found";
    if(method !=null) {                                    // method found or given:
      if(element.reflAccess == null) {
          //TODO: element.reflAccess = method;               // store if now found.
        }
      method.setAccessible(accessPrivate);
      Class<?>[] paramTypes = method.getParameterTypes();  // arguments from the method's signature
      Object[] actArgs = checkAndConvertArgTypes(givenArgs, paramTypes); // tune it with actual arguments
      if(actArgs !=null){                                  // check if actual argument types matches.
        try{ 
          data1 = method.invoke(obj, actArgs);             // invoke
          sError = null;                                      // bOk = true if no exception
        } catch(Exception exc){
          sError = ExcUtil.stackInfo("DataAccess - method access problem: " 
              + method.getClass().getCanonicalName() + method.getName() + "(...)", 3, 5);
          if(!bNoExceptionifNotFound) { 
            throw new InvocationTargetException(exc, sError.toString());
          }
        }
        
      }
    }
    if(sError !=null) {  //================================== error handling
      StringBuilder msg = new StringBuilder(1000);
      msg.append(sError);
      if(method !=null) {
        clazzcheck = clazz1;   //in loop superclass checked etc.
        do {
          if(accessPrivate || (clazzcheck.getModifiers() & Modifier.PUBLIC) !=0){
            Method[] methods = accessPrivate ? clazzcheck.getDeclaredMethods() : clazzcheck.getMethods();
            for(Method methodCheck: methods){
              bOk = false;
              if(method.getName().equals(element.ident)){
                Parameter[] param = method.getParameters();
                msg.append("  ").append(clazzcheck.getName()).append(".") .append(element.ident) .append("(");
                boolean bNext = false;
                for(Parameter par: param) {
                  if(bNext) { msg.append(", "); }
                  else { bNext = true; }
                  msg.append(par.getType().getName()); //.append(' ').append(par.getName());
                }
                msg.append(")\n");
          } } }
        } while(!bOk && (clazzcheck = clazzcheck.getSuperclass()) !=null);
        msg.append("  ... given arg names: ");
        boolean bNext = false;
        if(element.args !=null) {
          for(CalculatorExpr.Operand arg: element.args) {
            if(bNext) { msg.append(", "); }
            else { bNext = true; }
            msg.append(arg.textOrVar);
          }
        }
        msg.append("<<;\n  ... stackInfo: ");
        CharSequence stackInfo = Assert.stackInfo(msg, 3, 5);
        if(debugMethod !=null && debugMethod.equals("")){
          debug();
          debugMethod = null; //only one time after set.
        }
      } else {
        msg.append("DataAccess - method not found, searched:\n  ");
        msg.append(clazz1.getName()).append(".") .append(element.ident) .append("(");
        boolean bNext = false;
        if(args !=null) {
          for(Object arg: args) {
            if(bNext) { msg.append(", "); }
            else { bNext = true; }
            if(arg !=null) {
              msg.append(arg.getClass());
            }
          }
        }
        msg.append(")\n");
      }
      if(!bNoExceptionifNotFound) {
        throw new NoSuchMethodException(msg.toString());
      }
    }
    return data1;    
  }
  
  
  
  /**Invokes the static method which is described with the element.
   * This operation is no more necessary, because {@link #invokeMethod(DatapathElement, Class, Object, boolean, boolean, Object[])} does the same.
   * @param element its {@link DatapathElement#whatisit} == '%'.
   *   The {@link DatapathElement#identArgJbat} should contain the full qualified "packagepath.Class.methodname" separated by dot.
   * @return the return value of the method
   * @throws Throwable 
   */
  protected static Object XXXinvokeStaticMethod( DatapathElement element, Object[] args ) 
  throws Exception
  { final Class<?> clazz; 
    final String sMethod;
    if(element instanceof DatapathElementClass && ((DatapathElementClass)element).clazz !=null){
      clazz = ((DatapathElementClass)element).clazz;
      sMethod = element.ident;
    } else {
      int posClass = element.ident.lastIndexOf('.');
      String sClass = element.ident.substring(0, posClass);
      sMethod = element.ident.substring(posClass +1);
      ClassLoader classloader = getClassLoader(element);
      clazz = classloader.loadClass(sClass);
    }
    Method[] methods = clazz.getMethods();
    boolean bOk = false;
    Object data1 = null;
    for(Method method: methods){
      bOk = false;
      String sMethodName = method.getName();
      if(sMethodName.equals(sMethod)){
        if(debugMethod !=null) {
          if(debugMethod.equals(element.ident) || debugMethod.equals("")){
            debug();
            if(debugMethod.equals("")){
              debugMethod = null; //only one time after set.
            }
          }
        }
        Class<?>[] paramTypes = method.getParameterTypes();
        
        Object[] actArgs = checkAndConvertArgTypes(args !=null ? args: element.fnArgs, paramTypes);
        if(actArgs !=null){
          if((method.getModifiers() & Modifier.STATIC) ==0) { 
            throw new IllegalArgumentException("DataAccess - invokeStaticMethod on non static method, " + sMethod);
          }
          bOk = true;
          try{ 
            data1 = method.invoke(null, actArgs);
          } catch(IllegalAccessException exc){
            CharSequence stackInfo = Assert.stackInfo(" called ", 3, 5);
            throw new NoSuchMethodException("DataAccess - method access problem:\n in class: >>" 
              + clazz.getName() + "." + element.ident + "(...)<<\n ... stackInfo:" + stackInfo);
          }
          break;  //method found.
        }
      }
    }
    if(!bOk) {
      StringBuilder msg = new StringBuilder(1000);
      msg.append("DataAccess - static method not found: ")
      //.append(clazz.getName()).append(".") 
      .append(element.ident) .append("(");
      Object[] args1 = args !=null ? args : element.fnArgs;
      if(args1 !=null) {
        for(Object arg: args1) {
          if(arg == null) msg.append("null, ");
          else msg.append(arg.getClass()).append(", ");
      } }
//      else if(args !=null) {
//        for(Object arg: args) {
//          msg.append(arg.getClass()).append(", ");
//        }
//      }
      else if(element.args !=null) {
        for(CalculatorExpr.Operand arg: element.args) {
          msg.append(arg.textOrVar).append(", ");
        }
      }
      
      msg.append(")|, ");
      CharSequence stackInfo = Assert.stackInfo(msg, 3, 5);
      if(debugMethod !=null && debugMethod.equals("")){
        debug();
        debugMethod = null; //only one time after set.
      }

      throw new NoSuchMethodException(stackInfo.toString());
    }
    //} catch 
    return data1;    
  }
  
  
  
  /**Invokes the static method which is described with the element.
   * @param element its {@link DatapathElement#whatisit} == '%'.
   *   The {@link DatapathElement#identArgJbat} should contain the full qualified "packagepath.Class.methodname" separated by dot.
   * @return the return value of the method
   * @throws Throwable 
   */
  protected static Object getStaticValue( DatapathElement element ) 
  throws Exception
  { final Class<?> clazz; 
    final String sField;
    if(element instanceof DatapathElementClass && ((DatapathElementClass)element).clazz !=null){
      clazz = ((DatapathElementClass)element).clazz;
      sField = element.ident;
    } else {
      int posClass = element.ident.lastIndexOf('.');
      String sClass = element.ident.substring(0, posClass);
      sField = element.ident.substring(posClass +1);
      ClassLoader classloader = getClassLoader(element);
      clazz = classloader.loadClass(sClass);
    }
    return getDataFromField(sField, null, false, clazz, null, 0);    
  }
  
 
  
  
  private static ClassLoader getClassLoader(DatapathElement element){
    ClassLoader classloader = element.getClass().getClassLoader();
    if(element instanceof DatapathElementClass){
      DatapathElementClass elementClass = (DatapathElementClass) element;
      if(elementClass.loader !=null){
        classloader = elementClass.loader;
      }
    }
    return classloader;
  }
  
  
  
  
  
  /**Checks whether the given arguments matches to the necessary arguments of a method or constructor invocation.
   * Converts the arguments if possible and necessary:
   * <ul>
   * <li>same type of providedArg and argType: use providedArg without conversion
   * <li>argType is String[]: check providedArgs. If all of them are CharSequence, return String[] of this CharSequences.
   *   Convert the CharSequences toString().
   * <li>provideArg instanceof  -> argType: conversion
   * <li>{@link java.lang.CharSequence} -> {@link java.lang.CharSequence}: arg
   * <li>{@link java.lang.CharSequence} -> {@link java.lang.String}: arg.toString()
   * <li>{@link java.lang.CharSequence} -> {@link java.io.File} : new File(arg)
   * </ul>
   * @param providedArgs Given arguments
   * @param argTypes requested argument types
   * @return actArgs This array will be filled with converted parameter if all parameter matches.
   *   <br>If the number of args is 0, then Object[0] is returned.
   *   <br>null if the number of argtypes is not equal to the number of providedArgs or if the providedArgs and argTypes does not match. 
   *   The array have to be created with the size proper to 
   */
  protected static Object[] checkAndConvertArgTypes(Object[] providedArgs, Class<?>[] argTypes){
    Object[] actArgs = null;
    if(argTypes.length==1 && argTypes[0].isArray() && argTypes[0].getName().equals("[Ljava.lang.String;")){
      //especially for main(String[] args)
      //check whether the element.fnArgs are matching to a String array 
      if(providedArgs == null){ 
        actArgs = new Object[1];
        actArgs[0] = new String[0];
      } else {
        actArgs = new Object[1];
        String[] actArgs1 = new String[providedArgs.length];
        actArgs[0] = actArgs1;
        int ix = -1;
        for(Object arg: providedArgs){
          if(arg instanceof CharSequence){
            actArgs1[++ix] = ((CharSequence)arg).toString();
          } else {
            actArgs = null; break;  //do not match
          }
        }
      }
    }
    if(actArgs ==null){
      if(argTypes.length == 0 && providedArgs == null){
        actArgs = new Object[0]; //matches, but no args.
      }
      else if(providedArgs !=null 
        && (  argTypes.length == providedArgs.length
           || argTypes.length > 0 && argTypes.length < providedArgs.length && argTypes[argTypes.length -1].isArray()  
        )  ){
        //check it
        boolean bOk = true;
        int iParam = 0;  //iterator-index in argTypes, maybe less then ix
        //check the matching of parameter types inclusive convertibility.
        Class<?> argType = null;
        Conversion[] conversions = new Conversion[providedArgs.length];
        int ix = -1;    //iterator-index in actTypes
        //Iterator<Object> iter = providedArgs.iterator();
        int iProvideArgs = -1;
        //
        //first check all types, do not convert, see next loop
        //
        boolean bVarArg = false;  //variable argument list
        while(bOk && ++iProvideArgs < providedArgs.length) {                        
          Object actValue = providedArgs[iProvideArgs];              //iterate through provided arguments
          bOk = false;   //check for this arg
          ix +=1;
          if(actValue == null){
            bOk = true;  //may be compatible with all ones.
            conversions[ix] = Conversions.obj2obj;
          } else {
            Conversion conv = null;
            Class<?> actType = actValue.getClass();
            if(argTypes[iParam].isArray() && (actValue instanceof List<?>)
                &&  istypeof(((List<?>)actValue).get(0), argTypes[iParam].getComponentType()) ){ 
              conv = Conversions.list2array; 
            }else if(iParam == argTypes.length-1 && argTypes[iParam].isArray()
                && (  providedArgs.length > iParam+1 
                   || (providedArgs.length == iParam+1 && !actType.isArray())  //it is the last argument
                )  ){
              //There are more given arguments and the last one is an array or a variable argument list.
              //store the rest in lastArrayArg instead.
              argType = argTypes[iParam].getComponentType();
              bVarArg = true;
            } else {
              argType = argTypes[iParam];
            }
            //check super classes and all interface types.
            if(conv == null){
              conv = checkArgTypes(argType, actType, actValue);
            }
            if(conv != null){ 
              conversions[ix] = conv; 
              bOk = true; 
            }  //check first, fast variant.
            if(!bOk) { break; }
          }
          if(iParam < argTypes.length-1) { iParam +=1; }
        } //for, terminated with some breaks.
        if(bOk){
          //conversion matches:
          //
          //the last or only one Argument as array
          Object[] lastArrayArg;
          //if(argTypes.length < providedArgs.length){
          if(bVarArg) {
            Class<?> lastType = argTypes[argTypes.length-1].getComponentType();
            //create the appropriate array type:
            if(lastType == String.class){ 
              //A String is typical especially for invocation of a static main(String[] args)
              lastArrayArg = new String[providedArgs.length - argTypes.length +1]; }
            else {
              //TODO what else
              lastArrayArg = new String[providedArgs.length - argTypes.length +1]; }
          } else {
            lastArrayArg = null;
          }
          actArgs = new Object[argTypes.length];
          Object[] dstArgs = actArgs;
          iParam = 0;  //now convert instances:
          ix = -1;
          for(Object arg: providedArgs){ ////
            ix +=1;
            if(dstArgs == actArgs){
              if(iParam >= argTypes.length-1 && lastArrayArg !=null){
                //The last arg is ready to fill, but there are more given arguments and the last one is an array or a variable argument list.
                //store the rest in lastArrayArg instead.
                actArgs[iParam] = lastArrayArg;
                dstArgs = lastArrayArg;
                iParam = 0;
                argType = argTypes[iParam].getComponentType();
              } else {
                argType = argTypes[iParam];
              }
            } //else: it fills the last array of variable argument list. remain argType unchanged.
            Object actArg;
            assert(conversions[ix] !=null);
            //if(conversions[ix] !=null){
              actArg = conversions[ix].convert(arg);
            //}
            /*
            else if(arg instanceof CharSequence){
              if(argType == File.class){ actArg = new File(((CharSequence)arg).toString()); }
              else if(argType == String.class){ actArg = ((CharSequence)arg).toString(); }
              else {
                actArg = arg;
              }
            } else if( (typeName = argType.getName()).equals("Z") || typeName.equals("boolean")){
              if(arg instanceof Boolean){ actArg = ((Boolean)arg).booleanValue(); }
              if(arg instanceof Byte){ actArg = ((Byte)arg).byteValue() == 0 ? false : true; }
              if(arg instanceof Short){ actArg = ((Short)arg).shortValue() == 0 ? false : true; }
              if(arg instanceof Integer){ actArg = ((Integer)arg).intValue() == 0 ? false : true; }
              if(arg instanceof Long){ actArg = ((Long)arg).longValue() == 0 ? false : true; }
              else { actArg = arg == null ? false: true; }
            } else {
              actArg = arg;
            }
            */
            dstArgs[iParam] = actArg;
            iParam +=1;
          } //for, terminated with some breaks.
        } else {
          actArgs = null;
        }
      } else { //faulty number of arguments
        actArgs = null;
      }
    }
    return actArgs;
  }
  

  
  /**Checks whether the given actType with its value arg matches to the given argType. 
   * It checks all its super and interface types and searches a possible conversion, see {@link #initConversion()}.
   * If actType is an interface, all super interfaces are checked after them.
   * If actType is a class, all interfaces are checked but not the superclass.
   * This routine will be called recursively for the interfaces.
   * To get the interfaces of a class and all super interfaces of an interface,
   * the routine {@link java.lang.Class#getInterfaces()} is called.
   * Last not least the {@link #checkTypes(Class, Class, Object)} is called
   * for a possible conversion.
   * 
   * @param argType Requested type
   * @param actType Given type, it may be a super class, an interface or a conversion may exists.
   * @param arg The argument itself to check value ranges for conversion using {@link Conversion#canConvert(Object)}.
   * @return null if it does not match, elsewhere a conversion routine for conversion.
   *   If it is an super or interface type, the Conversion routine does return the instance itself.
   */
  public static Conversion checkArgTypes(Class<?> argType, Class<?> actType, Object arg){
    Conversion conv = null;
    Class<?> supertype = actType;
    while(conv == null && supertype !=null){
      conv = checkIfcTypes(argType, supertype, arg);  //this routine searches possible conversions
      if(conv == null){
        supertype = supertype.getSuperclass();
      }
    }
    return conv;
  }
  
  

    
  private static Conversion checkIfcTypes(Class<?> argType, Class<?> ifcType, Object arg){
    Conversion conv = checkTypes(argType, ifcType, arg);  //this routine searches possible conversions
    if(conv == null){
      Class<?>[] superIfcs = ifcType.getInterfaces();
      int ix = -1;
      int zz = superIfcs.length;
      while(conv == null && ++ix < zz) {
        Class<?> superIfc = superIfcs[ix];
        conv = checkIfcTypes(argType, superIfc, arg); 
      }
    }
    return conv;
  }
  
  
  

  /**Checks whether a given type with its value can be converted to a destination type. 
   * @param argType The destination type.
   * @param actType The given type.
   * @param arg The value
   * @return null if conversion is not possible, elsewhere the conversion.
   */
  public static Conversion checkTypes(Class<?> argType, Class<?> actType, Object arg){
    if(argType == actType){ return Conversions.obj2obj; }
    else {
      String conversion2 = actType.getName() + ":" + argType.getName(); //forex "Long:int"
      Conversion conv = idxConversions.get(conversion2); //search the conversion
      if(conv !=null && !conv.canConvert(arg)){
        conv = null;    //arg does not match.
      }
      return conv;
    }
  }
  
  
  
  /**Checks whether the given obj is type of the given type.
   * It gets the type with <code>Class.forName(sType)</code> and invokes {@link #istypeof(Object, Class)}
   * @param obj any instance
   * @param type String given type
   * @return true if type matches
   */
  public static final boolean istypeof(Object obj, String sType) 
  throws ClassNotFoundException {
    Class<?> type = Class.forName(sType);
    return istypeof(obj, type);
  }
  
  
  
  /**Checks whether the given obj is type of the given type.
   * @param obj any instance
   * @param type can be a base type or an interface type
   * @return true if type matches
   */
  public static final boolean istypeof(Object obj, Class type){
    if(obj == null) return false;
    else {
      Class<?> objClazz = obj.getClass();
      do {
        if(objClazz == type) return true;
        objClazz = objClazz.getSuperclass();
        //TODO check interfaces
      } while(objClazz != null); //Object.class);
      return false;
    }
  }

  /**Gets data from a field or from an indexed container.
   *    
   * <ul>
   * <li>If the instance is typeof {@link DataAccess.Variable} then its value is used.
   * <li>If the actual instance is instanceof Map with String-key, then the next object
   *    is gotten from the map with the name used as key.
   * <li>If the name is <code>"[]"</code> and the instance is an array, its length is gotten and returned. 
   *   If the instance is instance of an Container or Map, then the size() is returned. This property can be used especially 
   *   for {@link #access(Object, boolean, boolean)} as last element.    
   * <li>Elsewhere a field with ident as name is searched.
   * <li>If the instance is instanceof {@link TreeNodeBase} and the field identifier is not found in this instance,
   *    a child node with the given name is searched. 
   *    The TreeNodeBase is the super class of {@link org.vishia.xmlSimple.XmlNodeSimple}
   *    which is used to present a ZBNF parse result. Therewith the {@link org.vishia.zbnf.ZbnfParser#getResultTree()}
   *    can be used as data input. The tag names of that result tree follow the semantic in the string given Syntax script.
   * <li>If the field is not found in this class, it is try to get from the super classes.   
   * <li>If the found instance is of type {@link Variable} and bContainer = false, then the value of the Variable is returned.
   * </ul>
   * @param name Name of the field or key in the container
   * @param instance The instance where the field or element is searched.  
   * @param accessPrivate true than accesses also private data. 
   * @param bContainer only used for a TreeNodeBase: If true then returns the List of children as container, If false returns the first child with that name. 
   * @param bVariable returns the variable if it is found. if false then returns the value inside a variable. 
   * @return The reference described by name.
   * @throws NoSuchFieldException If not found.
   */
  public static Object getData(
      String name
      , Object instance
      , boolean accessPrivate
      , boolean bContainer
      , boolean bVariable
      , Dst dst) 
  throws NoSuchFieldException, IllegalAccessException
  {     final Object instance1;
    if(instance instanceof Variable<?>){
      @SuppressWarnings("unchecked") Variable<Object> var = (Variable<Object>)instance;
      instance1 = var.value;  
    } else {
      instance1 = instance;
    }
    return getDataPriv(name, instance1, accessPrivate, bContainer, bVariable, dst);
  }
  
  
  
  
  
  
  
  /**It does not resolve the instance if it is a Variable
   * @param name
   * @param instance
   * @param accessPrivate
   * @param bContainer
   * @param bVariable
   * @param dst
   * @return
   * @throws NoSuchFieldException
   * @throws IllegalAccessException
   */
  private static Object getDataPriv(
      String name
      , Object instance
      , boolean accessPrivate
      , boolean bContainer
      , boolean bVariable
      , Dst dst) 
  throws NoSuchFieldException, IllegalAccessException
  {
    if(name.equals("cellx"))
      Debugutil.stop();
    Object data1 = null;
    if(name.equals("[]")) {
      if(instance instanceof Object[]){
        return new Integer(((Object[])instance).length);
      } else if(instance instanceof Collection) {
        return new Integer(((Collection<?>)instance).size());
      } else if(instance instanceof Map) {
        return new Integer(((Map<?,?>)instance).size());
      } else {
        throw new IllegalArgumentException("is not a container or array, " + name);
      }
    } else if(instance instanceof Map<?, ?>){
      @SuppressWarnings("unchecked")
      //Note: on runtime the generic type of map can be set in any case because it is unknown.
      //Try to store that type, 
      Map<String, Object> map = (Map<String,Object>)instance;
      data1 = map.get(name);
      if(data1 == null){
        if(!map.containsKey(name)){ //checks whether this key with value null is stored.
          throw new NoSuchFieldException(name);
        }
      }
      /*
      if(data1 == null && bVariable){
        //not found, but a variable is expected: create one.
        data1 = new Variable<Object>('?', name, null);
        map.put(name, data1);
      }
      */
    } else {
      try{
        data1 = getDataFromField(name, instance, accessPrivate, dst);
      }catch(NoSuchFieldException exc){
        //NOTE: if it is a TreeNodeBase, first search a field with the name in the TreeNode reflection, then search in the TreeNode data
        if(instance instanceof TreeNodeBase<?,?,?>){
          TreeNodeBase<?,?,?> treeNode = (TreeNodeBase<?,?,?>)instance;
          if(bContainer){ data1 = treeNode.listChildren(name); }
          else { data1 = treeNode.getChild(name); }  //if more as one element with that name, select the first one.
          if(data1 == null){
            throw new NoSuchFieldException(name + " ;in TreeNode, contains; " + treeNode.toString());
          }
        } else throw exc;
      }
    }
    if(bVariable==false && data1 instanceof Variable<?>){
      @SuppressWarnings("unchecked") Variable<Object> var = (Variable<Object>)data1;
      data1 = var.value;  
    }
    
    return data1;  //maybe null
  }
  
  
  /**Returns the data which are stored in the named field of the given instance. The method searches the field
   * in the super class hierarchy and in all enclosing classes of each super classes starting with the last super class.
   * If a field is defined in the enclosing class and in the super class or an outer class of the super class twice, 
   * it is searched firstly in the super hierarchy. It means it meets the field in the outer class of any super class
   * instead of its own outer class.
   * 
   * @param name The name of the field.
   * @param obj The instance where the field are searched.
   * @param accessPrivate true then read from private or protected fields, false then the access to such fields
   *   throws the IllegalAccessException
   * @return the data of the field. Maybe null if the field contains a null pointer.
   * 
   * @throws NoSuchFieldException If the field does not exist in the obj
   * @throws IllegalAccessException if the field exists but is not accessible.
   */
  public static Object getDataFromField(String name, Object obj, boolean accessPrivate, Dst dst)
  throws NoSuchFieldException, IllegalAccessException {
    return getDataFromField(name, obj, accessPrivate, obj.getClass(), dst, 0);
  }
  
  
  /**Core method to get data from a field, static or non static
   * @param name of the field
   * @param obj null if static fields should be accessed.
   * @param accessPrivate also search private fields
   * @param clazz Should be match to obj if obj !=null
   * @param dst
   * @param recursiveCt 0 on user call, max. 100 recursions for sub classes and outer classes
   * @return The data which are referred with the field in the given obj or with the static field.
   * @throws NoSuchFieldException If the field was not found in clazz
   * @throws IllegalAccessException If the field cannot be accessed in obj
   */
  private static Object getDataFromField(String name, Object obj, boolean accessPrivate
      , Class<?> clazz, Dst dst, int recursiveCt)
  throws NoSuchFieldException, IllegalAccessException {
    if(recursiveCt > 100) throw new IllegalArgumentException("recursion error");
    Object ret = null;
    boolean bNotfound = false;
    try{ 
      Field field = clazz.getDeclaredField(name); 
      field.setAccessible(accessPrivate);
      if(dst !=null){ 
        dst.field = field;
        dst.obj = obj;
      }
      ret = field.get(obj);
      
    }
    catch(NoSuchFieldException exc){ bNotfound = true; }
    if(bNotfound){
      Class<?> superClazz = clazz.getSuperclass();
      if(superClazz !=null){
        try{
          ret = getDataFromField(name, obj, accessPrivate, superClazz, dst, recursiveCt+1);  //searchs in thats enclosing and super classes.  
          bNotfound = false;
        }catch(NoSuchFieldException exc){
          //not found in the super hierarchies:
          bNotfound = true;
        }
      }
    }
    if(bNotfound){
      Class<?> outerClazz = clazz.getEnclosingClass();
      if(outerClazz !=null){
        Object outer = getEnclosingInstance(obj);
        try{
          ret = getDataFromField(name, outer, accessPrivate, outerClazz, dst, recursiveCt+1);  //searchs in thats enclosing and super classes.  
          bNotfound = false;
        }catch(NoSuchFieldException exc){
          //not found in the super hierarchie:
          bNotfound = true;
        }
      }
    }
    if(bNotfound){
      //Note: this exception occurs often in JZcmd if a variable will be used which is not existing in a condition. The condition is false after catch!
      throw new NoSuchFieldException(name + " ;in class ;" + clazz.getCanonicalName()  + ", data," + (obj==null ? "" : obj.toString()));
    }
    return ret;
  }
  
  
  
  
  
  /**Returns the enclosing instance (outer class) of an instance which is type of any inner non-static class.
   * Returns null if the instance is not type of an inner class.
   * The access searches the internal field "this$0" and returns its reference.
   * Not that an inner non-static class aggregates the instance which is given on construction of the inner instance.
   * On source level all elements of the enclosing instance are visible without additional designation
   * or with the "Enclosingclass.this" construction. On run level it is that aggregation.
   * 
   * @param obj The instance
   * @return the enclosing instance or null.
   */
  public static Object getEnclosingInstance(Object obj){
    return getEnclosingInstance(0, obj);
    /*
    Object encl;
    try{ Field fieldEncl = obj.getClass().getDeclaredField("this$0");
      fieldEncl.setAccessible(true);
      encl = fieldEncl.get(obj);
    } catch(NoSuchFieldException exc){
      encl = null;        //the class is not an inner non static class.
    } catch(IllegalAccessException exc){
      encl = null;        //Any access problems ? 
    }
    return encl;
    */
  }
  
  
  
  
  private static Object getEnclosingInstance(int recurs, Object obj){
    Object encl;
    boolean bNext = false;
    if(recurs >10){ encl = null; }
    else {
      String enclName = "this$" + recurs;
      try{ Field fieldEncl = obj.getClass().getDeclaredField(enclName);
        fieldEncl.setAccessible(true);
        encl = fieldEncl.get(obj);
      } catch(NoSuchFieldException exc){
        bNext = true;
        encl = null;
      } catch(IllegalAccessException exc){
        encl = null;        //Any access problems ? 
      }
    }
    if(bNext){
      encl = getEnclosingInstance(recurs+1, obj);
    }
    return encl;
  }
  
  
  
  /**Checks whether a given Field is the reference to the enclosing respectively outer class.
   * Such an reference is not visible in the souce code often. In source code it should be written with <pre>
   *  EnclosingType.this </pre>
   * but it can be omitted. The internal name of the field is <pre>
   * this$99</pre>
   * whereby 99 is a number, <code>this$0</pre> for the immediate enclosing instance. 
   * Note that a enclosing instance is accessible immediately, but it is a referenced object.
   *  
   * @param ref the Field
   * @return true if the name of the field starts with <code>this$</code>
   */
  public static boolean isReferenceToEnclosing(Field ref) {
    return ref.getName().startsWith("this$");
  }
  
  
  
  /**Check whether a given class is type of another class. This method does not check interfaces, only the extends-path.
   * Example:<pre>
   * MyClass myInstance = new MyClass();  //MyClass extends MySuperclass.
   * ...
   * if(DataAcess.isOrExtends(myInstance.getClass(), MySuperclass.class)) { ...
   * </pre> 
   * This method is similar like <pre>
   *   myInstance instanceof MySuperclass
   * </pre>.
   * The difference is: It checks classes.
   *   
   * @param thisclazz Class to test, usual build with <code>myInstance.getClass()</code>
   * @param cmpclazz A Class to check, usual build with <code>Classtype.class</code>
   * @return true if thisclazz is of type cmpclazz. Note: It does not check interfaces.
   */
  public static boolean isOrExtends(Class<?> thisclazz, Class<?> cmpclazz)
  {
    Class<?> clazzret = thisclazz;
    do{
      if(clazzret == cmpclazz) return true;
      else {
        clazzret = clazzret.getSuperclass();
      }
    } while(clazzret !=null);
    return false; //if not found.
  }
  
  
  
  
  
  /**Returns a string representation of the object.
   * <ul>
   * <li>content == null returns an empty string.
   * <li>content is a numerical type, returns a formatted string from it.
   * <li>else return content.toString().
   * </ul>
   * @param content any object
   * @param format may be null, if not null it is used with {@link java.lang.String#format(String, Object...)}.
   * @return A string which represents content.
   */
  public static String getStringFromObject(Object content, String format){
    String sContent;
    Object val1;
    if(content instanceof Variable<?>){
      @SuppressWarnings("unchecked") Variable<Object> var = (Variable<Object>)content;
      val1 = var.value;
    } else {
      val1 = content;
    }
    if(val1 == null){
      sContent = "";
    }
    else if(val1 instanceof String){ 
      sContent = (String) val1; 
    } else if(val1 instanceof Integer){ 
      if(format !=null){
        try{ sContent = String.format(format, val1); 
        } catch(Exception exc){ sContent = "<??format:"+ format + " exception:" + exc.getMessage() + "??>"; }
      } else {
        int value = ((Integer)val1).intValue();
        sContent = Integer.toString(value);
      }
    } else {
      sContent = content.toString();
    }
    return sContent;
  }
  
  
  /**Gets the int value from any Object. If the Object can represent a int val, convert and returns it.
   * Elsewhere it returns 0. TODO return int from a String (CharSequence) with conversion.
   * @param val The Object. An int value is returned from all numerical wrappers: Byte, ... Float, Double.
   * @return the value.
   */
  public static int getInt(Object val){
    if(val instanceof Byte){ return ((Byte)val).byteValue(); }
    else if(val instanceof Short){ return ((Short)val).shortValue(); }
    else if(val instanceof Integer){ return ((Integer)val).intValue(); }
    else if(val instanceof Long){ return (int)((Long)val).longValue(); }
    else if(val instanceof Float){ return (int)((Float)val).floatValue(); }
    else if(val instanceof Double){ return (int)((Double)val).doubleValue(); }
    else return 0;
  }
  
  
  /**Gets the float value from any Object. If the Object can represent a float val, convert and returns it.
   * Elsewhere it returns 0. TODO return int from a String (CharSequence) with conversion.
   * @param val The Object. An float value is returned from all numerical wrappers: Byte, ... Float, Double.
   * @return the value.
   */
  public static float getFloat(Object val){
    if(val instanceof Byte){ return ((Byte)val).byteValue(); }
    else if(val instanceof Short){ return ((Short)val).shortValue(); }
    else if(val instanceof Integer){ return ((Integer)val).intValue(); }
    else if(val instanceof Long){ return ((Long)val).longValue(); }
    else if(val instanceof Float){ return ((Float)val).floatValue(); }
    else if(val instanceof Double){ return (float)((Double)val).doubleValue(); }
    else return 0;
  }


  
  /**Checks whether data is an array and returns the current number of elements.
   * <br>Note: Use data.getClass(): {@link Class#isArray()} to check whether it is an array
   * @param data any data
   * @return -1 if it is not an array. The current number of elements if it is an array.
   */
  public static int getLengthOfArray(Object data) 
  {
    Class<?> clazz = data.getClass();
    if(clazz.isArray()) {
      Object[] array = (Object[]) data;
      return array.length;
    }
    else return -1;
  }
  
  
  
  
  /**Gets the indexed element of the given array.
   * @param data Should be an array with the expected number of dimensions, elsewhere see throws
   * @param ixArray The indices, maybe missed or lesser than array depth, then data or an element as array is returned.
   *   if ixArray contains more indices than necessary, there are ignored.
   * @return the array element
   * @throws IndexOutOfBoundsException on faulty ixArray
   * @since 2016-01-17 for primitive types too.
   */
  public static Object getArrayElement(Object data, int ... ixArray) 
  { int ixix = 0;
    Object data1 = data;
    Class<?> clazz1 = data.getClass();
    while(clazz1.isArray() && ixix < ixArray.length) {
      clazz1 = clazz1.getComponentType();
      if(clazz1 == Integer.TYPE){           //Note: the order is regarded to probability and calculation time. Integer is frequently and should be fast. 
        float[] data2 = (float[]) data1;
        data1 = data2[ixArray[ixix]];
      } else if(clazz1 == Character.TYPE){
        float[] data2 = (float[]) data1;
        data1 = data2[ixArray[ixix]];
      } else if(clazz1 == Long.TYPE){
        float[] data2 = (float[]) data1;
        data1 = data2[ixArray[ixix]];
      } else if(clazz1 == Short.TYPE){
        float[] data2 = (float[]) data1;
        data1 = data2[ixArray[ixix]];
      } else if(clazz1 == Byte.TYPE){
        float[] data2 = (float[]) data1;
        data1 = data2[ixArray[ixix]];
      } else if(clazz1 == Boolean.TYPE){
        float[] data2 = (float[]) data1;
        data1 = data2[ixArray[ixix]];
      } else if(clazz1 == Float.TYPE){
        float[] data2 = (float[]) data1;
        data1 = data2[ixArray[ixix]];
      } else if(clazz1 == Double.TYPE){
        float[] data2 = (float[]) data1;
        data1 = data2[ixArray[ixix]];
      } else {
        data1 = ((Object[])data1)[ixArray[ixix]];
      }
    }
    return data1;
  }
  
  
  
  
  
  
  
  /**Sets a bit in a int word
   * @param value The actual value of the word
   * @param mask Designation of bits to change. Usual only one bit. Tip: Use symbolic names.
   * @param set true: set this bits to 1, false: reset the bits to 0.
   * @return The new value of the word. You should invoke: myBitword = setBit(myBitword, ....);
   */
  public static int setBit(int value, int mask, boolean set){
    return set ? value | mask : value & ~mask;
  }
  
  
  
  /**Creates or replaces a variable with a simple name in the given container. 
   * If the variable exists, its content will be replaced by the new definition.
   * @param map The container for variables.
   * @param name The name of the variable in the container.
   * @param type one of A O Q J S U L M V E C = Appendable, Object, Boolean, Long, 
   *        String, StringBuilder, ListContainer, Map, VariableTree, EnvironmentVariable, Class
   * @param content The new value
   * @param isConst true then create a const variable, or change content of a constant variable. 
   *   A const variable is designated by {@link Variable#isConst} boolean element. A const variable can change its content
   *   by setting another const value with this param, but not by assign without this const param flag.
   * @throws IllegalAccessException  if a const variable is attempt to modify without isConst argument.
   */
  public static Variable<Object> createOrReplaceVariable(Map<String, Variable<Object>> map, String name, char type, Object content, boolean isConst) throws IllegalAccessException{
    DataAccess.Variable<Object> var = map.get(name);
    if(var == null){
      var = new DataAccess.Variable<Object>(type, name, content);
      var.isConst = isConst;
      map.put(name, var);
    } else if(var.isConst &&!isConst){
      throw new IllegalAccessException("DataAccess.setVariable - modification of const; " + var.name);
    } else {
      var.value = content;
      var.type = type;
      var.isConst = isConst;
    }
    return var;
  }
  
  
  /**Set or replaces a variable with a simple name in the given container. 
   * If the variable exists, its content will be replaced by the new definition.
   * In opposite to {@link #createOrReplaceVariable(Map, String, char, Object, boolean)}
   * the container will contain the original given ref. 
   * @param map The container for variables.
   * @param ref Initialized variable with or without value.
   */
  public static void setVariable(Map<String, Variable<Object>> map, DataAccess.Variable<Object> ref) {
    DataAccess.Variable<Object> var = map.get(ref.name);
    if(var != null) {
      map.remove(ref.name);  //necessary if map is IndexMultiTable
    }
    map.put(ref.name, ref);
    return;
  }
  
  
  /**Searches the variale in the container and returns it.
   * @param map The container
   * @param name name of the variable in the container
   * @param strict true then throws an {@link NoSuchFieldException} if not found.
   * @return null if strict = false and the variable was not found.  
   * @throws NoSuchFieldException
   */
  public static Variable<Object> getVariable(Map<String, Variable<Object>> map, String name, boolean strict) 
  throws NoSuchFieldException{
    Variable<Object> var = map.get(name);
    if(var !=null) return var; //maybe null
    else {
      if(strict) throw new NoSuchFieldException("DataAccess.getVariable - not found; " + name);
      return null;
    }
  }
  
  /**Should be used only for debug to view what is it.
   * @see java.lang.Object#toString()
   */
  @Override public String toString(){ 
    return this.oneDatapathElement !=null ? this.oneDatapathElement.toString() 
         : this.listDatapath !=null ? this.listDatapath.toString() 
         : "emtpy DataAccess";
  }
  
  /**Returns a CharSequence with the idents of the path separated with "."
   * @return null if datapath is empty.
   */
  public CharSequence idents(){
    if(this.oneDatapathElement !=null) {
      return this.oneDatapathElement.ident;
    } 
    else {
      int zDatapath = this.listDatapath == null ? 0 : this.listDatapath.size();
      if(zDatapath == 0 ) return null;
      else {
        StringBuilder u = new StringBuilder();
        for(DataAccess.DatapathElement item : this.listDatapath){
          u.append(item.ident).append(".");
        }
        return u;
      }
    }
  }
  
  
  /**A debug helper: Set this ident to any String, which is expected for access.
   * Then a breakpoint may be used. Set the breakpoint in the routine {@link #debug()}
   * in any IDE (Eclipse...)
   * @param ident The ident to break;
   */
  public static void debugIdent(String ident){ debugIdent = ident; }
  
  
  /**A debug helper: Set this identifier to any String, which is expected for invocation of a method.
   * Then a breakpoint may be used. Set the breakpoint in the routine {@link #debug()}
   * in any IDE (Eclipse...)
   * @param ident The identifier to break;
   */
  public static void debugMethod(String ident){ debugMethod = ident; }
  
  
  /**Set a breakpoint here to edit a {@link #debugIdent(String)}
   * 
   */
  protected static void debug(){
    Debugutil.stop();
  }
  
  
  public void writeStruct(Appendable out) throws IOException {
    String sep = "";
    for(DatapathElement element: listDatapath){
      out.append(sep);
      element.writeStruct(out);
      sep = ".";
    }
  }
  
  
  
  
  
  
  /**This class extends its outer class and provides the capability to set the data path
   * especially from a ZBNF parser result.
   * It can be instantiate if that capability is necessary, and used than as a DataAccess instance.
   * The reason for the derivation - more structure.
   */
  public static class DataAccessSet extends DataAccess{

    /**Invoked if an access to an existing variable is stored. */
    public DataAccessSet(){ super(); }
    
    /**This method may be overridden if a derived instance is necessary. */
    public SetDatapathElement new_datapathElement(){ return new SetDatapathElement(); }

    public final void add_datapathElement(SetDatapathElement val){ 
      super.add_datapathElement(val); //Note: super does not get a SetDatapathElement but only its superclass.
    }
    
    
    /**This method may be overridden if a derived instance is necessary. */
    public DatapathElementClass newDatapathElementClass(){ return new DatapathElementClass(); }

    public final void add_datapathElementClass(DatapathElementClass val){ 
      super.add_datapathElement(val); //Note: super does not get a DatapathElementClass but only its superclass.
    }
    
    
    public SetDatapathElement new_startDatapath(){ return new SetDatapathElement(); }

    public final void add_startDatapath(SetDatapathElement val){ 
      super.add_datapathElement(val); //Note: super does not get a SetDatapathElement but only its superclass.
    }
    
    
    public final void set_envVariable(String ident){
      assert(this.listDatapath==null && this.oneDatapathElement == null);
      DataAccess.DatapathElement element = new DataAccess.DatapathElement();
      element.whatisit = '$';
      element.ident = ident;
      add_datapathElement(element); 
    }
    

    public final void set_startVariable(String ident){
      assert(this.listDatapath==null && this.oneDatapathElement == null);
      DataAccess.DatapathElement element = new DataAccess.DatapathElement();
      element.whatisit = '@';
      element.ident = ident;
      add_datapathElement(element); 
    }
    
    
    public final DatapathElementClass new_newJavaClass()
    { DatapathElementClass value = newDatapathElementClass();
      value.whatisit = '+';
      //ScriptElement contentElement = new ScriptElement('J', null); ///
      //subContent.content.add(contentElement);
      return value;
    }
    
    public final void add_newJavaClass(DatapathElementClass val) { add_datapathElement(val); }


    public final DatapathElementClass new_staticJavaMethod()
    { DatapathElementClass value = newDatapathElementClass();
      value.whatisit = '%';
      return value;
      //ScriptElement contentElement = new ScriptElement('j', null); ///
      //subContent.content.add(contentElement);
      //return contentElement;
    }
    
    public final void add_staticJavaMethod(DatapathElementClass val) { add_datapathElement(val); }


    /**This routine have to be invoked as last one to set the type. */
    public final void setTypeToLastElement(char type){
      if(this.oneDatapathElement !=null) {
        this.oneDatapathElement.whatisit = type;
      }
      else {
        int ix = this.listDatapath.size() -1;
        if(ix >=0){
          DatapathElement last = this.listDatapath.get(ix);
          last.whatisit = type;
        }
      }
    }
    
  }

 



  
  
  
  
  
  
  
  
  
  
  /**This class extends a {@link DatapathElement} and provides the capability to set the data path
   * especially from a ZBNF parser result, see {@link org.vishia.zbnf.ZbnfJavaOutput}.
   * It is instantiated if the {@link DataAccessSet} is used, see {@link DataAccessSet#new_datapathElement()}
   */
  public static class SetDatapathElement extends DatapathElement{
  
    protected final Object dbgParent;
    
    public SetDatapathElement(Object dbgParent){ this.dbgParent = dbgParent; }
    
    public SetDatapathElement(){ this.dbgParent = null; }
    
    
    public void set_javapath(String text){ this.ident = text; }
    
    public void set_ident(String text){ this.ident = text; }
    
    @Deprecated public void set_index(int val) { 
      int ixindices;
      //assert(false);                       //TODO comment this assert if the operation is used anywhere, it is a todo.
      if(super.indices !=null) {
        //TODO increase
        super.indices = new int[1];
        ixindices = 0;
        
      } else {
        super.indices = new int[1];
        ixindices = 0;
      }
      this.indices[ixindices] = val;  
    }

  }
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  /**Class holds one element for access to data.
   * Instances of this class can be created using {@link org.vishia.zbnf.ZbnfJavaOutput} to fill from a parse result.
   * Therefore some methods have a special naming which matches to the semantic of the used parser script.
   * <br><br>
   * <b>Meaning and using of the {@link #fnArgs} and {@link #indices}:</b><br>
   * Both should be set before {@link DataAccess#access(Object, boolean, boolean)} 
   * or {@link DataAccess#access(DatapathElement, Object, boolean, boolean, boolean, Dst)} is called. 
   * The calculation of the arguments and indices should be done outside of the class before usage with adequate operations.
   * To set the results {@link #setActualArgumentArray(Object[])} or {@link #setActualArguments(Object...)} should be used.
   * <br><br>
   * {@link #set(String)}: Textual parsing of the path. Its capability was enhanced in 2019-06: Before, the usage of DataAccess 
   * was usually related to the ZBNF parser and the {@link org.vishia.jztxtcmd.JZtxtcmd}, which uses 
   * the derived class {@link SetDatapathElement} and the {@link #set_ident(String)} etc. from the parse result.
   * The other possibility, set the path immediately only from a given text, was necessary with the {@link OutTextPreparer}
   * and it may be proper from user level. That possibility supports only simple argument names stored in {@link #argNames}, 
   * String literal arguments stored immediately in {@link #fnArgs} and numeric literal indices stored in {@link #indices}    
   * 
   */
  public static class DatapathElement
  {
    /**Name of the element or method in any instance.
     * From Zbnf <$?ident>
     */
    protected String ident;
    

    
    /**Kind of element
     * <ul>
     * <li>'$': An environment variable.
     * <li>'@': A variable from the additional data pool.
     * <li>'.': a field with ident as name.
     * <li>'+': new ident, creation of instance maybe with or without arguments in {@link #fnArgs}
     * <li>'%'; call of a static routine maybe with or without arguments in {@link #fnArgs}
     * <li>'&'; indirect path
     * <li>'(': subroutine maybe with or without arguments in {@link #fnArgs}.
     * <li>'A': A new Appendable variable
     * <li>'C': A new Class variable
     * <li>'E': A new environment variable
     * <li>'K': A new Value variable
     * <li>'L': A new list container.
     * <li>'O': A new Object variable
     * <li>'P': A new pipe variable.
     * <li>'Q': A new Boolean variable
     * <li>'S': A new String variable
     * <li>'X': A new Codeblock variable (eXecute, Subtextvar, Subroutinevar)
     * </ul>
     * A new Variable should be stored newly as {@link Variable} with that given type using {@link DataAccess#storeValue(List, Map, Object, boolean)}.
     */
    protected char whatisit = '.';

    /**List of actual arguments of a method. If null, it is not a method or the method has not arguments
     * or the arguments should be prepared in {@link DataAccess#invokeMethod(DatapathElement, Class, Object, boolean, Object[], boolean)}. 
     * @deprecated because it is a violation of the principle that this class stores only data of construction or set, not for current access. 
     * It is used yet (2019-08) especially in {@link org.vishia.cmd.JZtxtcmdExecuter}. */
    @Deprecated protected Object[] fnArgs;

    
    /**It is is a method call, contains the argument calculation rules. 
     * The {@link CalculatorExpr.Operand#textOrVar} contains the name of the start variable if given. */
    private CalculatorExpr.Operand[] args;
    
    
    /**If set the source class is already . To access the value use it. */
    java.lang.reflect.Member reflAccess;
    
    /**Idf >=0 then the data should be accessed simple from a given List or array with this index.
     * Not using reflection. 
     */
    int ixData = -1;
    
    /**Numerical values to access elements of the given variable. More as one for multi-dimensional element. 
     * TODO replace it by using {@link #args} maybe with const values.*/
    int[] indices;
    
    /**true then an operation call, false: a variable access. @since 2018-08*/
    private boolean operation_;
    
    /**Creates an empty element.
     * 
     */
    protected DatapathElement(){}

    /**Creates a datapath element.
     * @param name see {@link #set(String)}
     * @throws ParseException 
     */
    public DatapathElement(String path) throws ParseException {
      set(path);
    }
    
    
    /**Creates a datapath element. see {@link #set(String)} also for details of the arguments. 
     * @throws ParseException
     */
    public DatapathElement(StringPartScan path, Map<String, DataAccess.IntegerIx> nameVariables
    , Class<?> reflData, boolean bFirst) throws ParseException{
      set(path, nameVariables, reflData, bFirst);
    }
    
    
    /**Creates a datapath element. see {@link #set(String)}
     * @param path
     * @param nameVariables see {@link DataAccess#DataAccess(StringPartScan, Map, Class, char)}
     * @param reflData see {@link DataAccess#DataAccess(StringPartScan, Map, Class, char)}
     * @throws ParseException
     */
    public DatapathElement(StringPartScan path, Map<String, DataAccess.IntegerIx> nameVariables
    , Class<?> reflData) throws ParseException{
      set(path, nameVariables, reflData, false);
    }
    
    
    /**Invoked from {@link org.vishia.zbnf.ZbnfJavaOutput} or other via reflection. */
    public void set_operation(boolean val) { operation_ = val; }
    
    /**Invoked from {@link org.vishia.zbnf.ZbnfJavaOutput} or other via reflection. */
    public void set_operation() { operation_ = true; }
    
    public boolean isOperation() { return operation_; }
    
    public int nrArgNames(){ return args == null ? 0 : args.length; }
    
    
    
    /**Returns the name for the argument start variable. 
     * It helps to fill the set the varValues on call of 
     * {@link DataAccess#invokeMethod(DatapathElement, Class, Object, boolean, Object[], boolean)}
     */
    public String argName(int ix){ return args[ix].textOrVar; }
    
    
    
    /**Creates a datapath element, for general purpose.
     * If the name starts with the following special chars "$@%+", it is an element with that {@link #whatisit}.
     * If the name contains a '(' it is a method call. Elsewhere it is the name of a field.
     * If it is a method call, the following rules are taken for evaluating parameters:
     * <ul>
     * <li>Argument in "": a constant string
     * <li>Argument able to convert to a numeric value: The numeric value
     * <li>Argument starts with '*': A data path
     * <li>Elsewhere use {@link CalculatorExpr#setExpr(String)}.
     * </ul>
     * @param path 
     * @throws ParseException 
     */
    public void set(String path) throws ParseException{
      char cStart = path.charAt(0);
      int posNameStart = 1;
      if("$@+%".indexOf(cStart) >=0){
        whatisit = cStart;
      } else {
        whatisit = '.';
        posNameStart = 0;
      }
      int posNameEnd;
      if((posNameEnd = path.indexOf('(')) != -1) { //Function
        whatisit = whatisit == '%' ? '%' : '('; //%=static or non (=static routine.
        this.operation_ = true;
        
        
        int posEnd = posNameEnd +1; //path.lastIndexOf(')', posNameEnd);
        int zPath = path.length();
        char cc = ' ';
        while(posEnd < zPath && cc == ' ') {
          cc = path.charAt(posEnd++);
        }
        if(cc != ' ' && cc != ')') {
          //
          //parse the arguments
          //
          StringPartScan sp = new StringPartScan(path.substring(posNameEnd+1));
          sp.setIgnoreWhitespaces(true);
          this.args = parseArgumentExpr(sp, null, null);
        }
      } else if((posNameEnd = path.indexOf('[')) != -1) { //Index for element
        List<Object> lIndices = new LinkedList<Object>();
        CalculatorExpr exprIndex = new CalculatorExpr();
        StringPartScan sp = new StringPartScan(path);
        sp.seekPos(posNameEnd +1);
        sp.setIgnoreWhitespaces(true);
        sp.scanStart();
        boolean bArgExpr = false;
        do {
          exprIndex.setExpr(sp, null, false);
          CalculatorExpr.Operand value = exprIndex.isSimpleSetExpr();
          if(value !=null && value.dataConst instanceof CalculatorExpr.Value) {
            lIndices.add(((CalculatorExpr.Value)value.dataConst).intValue());  //should be an int if given as const.
          } else {
            bArgExpr = true;
            lIndices.add(exprIndex);
            exprIndex = new CalculatorExpr();
          }
        } while(sp.scan(",").scanOk());
        this.indices = new int[lIndices.size()];
        if(bArgExpr) {
          this.args = new CalculatorExpr.Operand[lIndices.size()];
        }
        int ixix = 0;
        for(Object ix: lIndices) { 
          if(ix instanceof Integer) { this.indices[+ixix] = (Integer)ix; }
          else {
            this.indices[ixix] = -1;
            this.args[ixix] = new CalculatorExpr.Operand((CalculatorExpr)ix);
          }
          ixix +=1;
        } //for
        Debugutil.stop();
      } else {
        posNameEnd = path.length();
      }
      this.ident = path.substring(posNameStart, posNameEnd);
    }

    
    /**Creates the first datapath element, which can also access to the variable pool and a reflData class
     * or create further elements but supports the first element for operations arguments.
     * <ul>
     * <li>If the sp starts with the following special chars "$@%+", it is an element with that {@link #whatisit}.
     * <li>if the sp contains a <code>operation(arg, ...)</code> then it is marked as Operation call. 
     * <li>If the sp contains <code>array[indices]</code> the index values are parsed with {@link CalculatorExpr#setExpr(StringPartScan)}.
     *   Usual they are only integer literal constants, then the value is stored immediately.  
     * </ul>
     * @param path It is used to parse this one element from the whole datapath. 
     *   After this operation the current position is the position after.
     *   It means either a '.' for a next element of the following stuff after the path. 
     * @param nameVariables If bFirst is set, it is primary checked whether the path element will be found there.
     *   Then ixData is set to access the correct element in varValues, last argument of {@link DataAccess#access(Object, boolean, boolean, Map, Object[])}
     *   see {@link DataAccess#DataAccess(StringPartScan, Map, Class, char)}
     * @param reflData If bFirst is set, it is secondary checked whether the path element will be found there.
     * @param bFirst ==true for the first element. It can be start with a member of nameVariables or in reflData.
     *   if bFirst== false, nameVariables and refldata are not used for this element, 
     *   but they are necessary for arguments of an operation call. 
     * @throws ParseException 
     */
    public void set(StringPartScan path, Map<String
    , DataAccess.IntegerIx> nameVariables
    , Class<?> reflData
    , boolean bFirst
    ) throws ParseException {
      char cStart = '?';
      if(bFirst) {
        cStart = path.getCurrentChar();
        if("$@+%".indexOf(cStart) >=0){  // envVar, givenVar, new, static
          this.whatisit = cStart;
          path.seekPos(1);
        } else {
          this.whatisit = '.';                //default:  field with ident as name
        }
      }
      if(cStart == '&' ) { //-------------------------------- an indirect path
        this.whatisit = '&';
        if(path.scan("&(").scanOk()) {
          this.args = new CalculatorExpr.Operand[1];       // stores the path to the path
          this.args[0] = new CalculatorExpr.Operand(path, nameVariables, reflData, false);
          if(!path.scan(")").scanOk()) {
            throw new ParseException("&(<expression>) expected: , \")\" missing" + path.getCurrent(32), 0);
          }
        } else {
          throw new ParseException("&(<expression>) expected, \"(\" missing " + path.getCurrent(32), 0);
        }
      } else {
        if(!path.scanStart().scanIdentifier().scanOk()) {
          throw new ParseException("idenfifier expected instead: " + path.getCurrent(32), 0);
        }
        this.ident = path.getLastScannedString();
        if(path.scan("(").scanOk()) { //Function
          this.whatisit = this.whatisit == '%' ? '%' : '('; //%=static or non (=static routine.
          this.operation_ = true;
          if(!path.scan(")").scanOk()) { 
            // ========>        
            this.args = parseArgumentExpr(path, nameVariables, reflData);
          }
          
        }
      }
      if(path.scan("[").scanOk()) { //Index for element
        List<Object> lIndices = new LinkedList<Object>();
        CalculatorExpr exprIndex = new CalculatorExpr();
        path.scanStart();
        boolean bArgExpr = false;
        do {
          exprIndex.setExpr(path, null, false);
          CalculatorExpr.Operand value = exprIndex.isSimpleSetExpr();
          if(value !=null && value.dataConst instanceof CalculatorExpr.Value) {
            lIndices.add(((CalculatorExpr.Value)value.dataConst).intValue());  //should be an int if given as const.
          } else {
            bArgExpr = true;
            lIndices.add(exprIndex);
            exprIndex = new CalculatorExpr();
          }
        } while(path.scan(",").scanOk());
        this.indices = new int[lIndices.size()];
        if(bArgExpr) {
          this.args = new CalculatorExpr.Operand[lIndices.size()];
        }
        int ixix = 0;
        for(Object ix: lIndices) { 
          if(ix instanceof Integer) { this.indices[+ixix] = (Integer)ix; }
          else {
            this.indices[ixix] = -1;
            this.args[ixix] = new CalculatorExpr.Operand((CalculatorExpr)ix);
          }
          ixix +=1;
        } //for
        if(!path.scan("]").scanOk()) {
          throw new IllegalArgumentException("indices, missing ]");
        }
        Debugutil.stop();
      } // [...]
      if(bFirst && this.ident !=null) {
        if(nameVariables !=null) {
          IntegerIx ix1 = nameVariables.get(this.ident);
          if(ix1 !=null) {
            this.ixData = ix1.ix;
          }
        } 
        if(this.ixData <0 && reflData !=null) {
          if("%(".indexOf(this.whatisit) >=0) {            // operation
            Method[] operations = reflData.getMethods();
            Class<?>[] argTypes = null;
            for(Method operation: operations) {
              if(operation.getName().equals(this.ident)) {
                argTypes = operation.getParameterTypes();
                this.reflAccess = operation;
                this.whatisit = '%';   // only a static operation is possible
                break;
              }
            }
          } else {
            //TODO try get field
          }
        }
      }
    }

    
    
    
    /**Parses an argument String with expressions
     * @param sArgs The starting "(" is parsed already, by previous check. A following ")" is not read. 
     *   It means the sArgs can contain the argument String without ().
     *   After parsing the sArgs is set before ")" if exist, after the args.
     *   The Syntax of any arg is determined by functionality of {@link CalculatorExpr#CalculatorExpr(StringPartScan, Map, Class)}.  
     * @param nameVariables Map of variables
     * @param reflData 
     * @return
     * @throws ParseException
     */
    CalculatorExpr.Operand[] parseArgumentExpr(StringPartScan sArgs, Map<String,DataAccess.IntegerIx> nameVariables, Class<?> reflData) 
    throws ParseException {
      List<CalculatorExpr.Operand> listArgs = new LinkedList<CalculatorExpr.Operand>();
      do {
        CalculatorExpr.Operand arg = new CalculatorExpr.Operand(sArgs, nameVariables, reflData, false);
        listArgs.add(arg);
      } while(sArgs.scan(",").scanOk()); 
      if(listArgs.size() >=0) {
        CalculatorExpr.Operand[] ret = new CalculatorExpr.Operand[listArgs.size()];
        listArgs.toArray(ret);
        return ret;
      } else {
        return null;  //empty arguments.
      }
    }
    
    
    public void set_ident(String text){ this.ident = text; }
    
    public void set_whatisit(String text){ this.whatisit = text.charAt(0); }
    

    public String ident(){ return ident; }
    
    public void setIdent(String ident){ this.ident = ident; }
    
    /**Adds any argument with its value.  */
    public void setActualArguments(Object... args){
      fnArgs = args;
    }
    

    /**Adds any argument with its value.  */
    public void setActualArgumentArray(Object[] args){
      fnArgs = args;
    }
    
    
    
    public void writeStruct(Appendable out) throws IOException {
      out.append(whatisit);
      if(whatisit >='A' && whatisit <='Z'){
        out.append(':');
      }
      out.append(ident);
      if(fnArgs!=null){
        String sep = "(";
        for(Object arg: fnArgs){
          out.append(sep).append(arg.toString());
          sep = ", ";
        }
        out.append(")");
      }
    }


    /**For debugging.*/
    @Override public String toString(){
      if(whatisit == 0){ return ident + ":?"; }
      else if(whatisit !='('){ return ident + ":" + whatisit;}
      else{
        return ident + "(...)";
      }
    }
  }

  
  
  
  /**Variant of a DatapathElement which can contain a ClassLoader for a new Java class or a static method invocation
   * or a Class which's field, method or constructor should access.
   * The {@link DatapathElement#ident} contains the method or field name if a class is given.
   * Elsewhere it contains the package.path.Class.element
   */
  public static class DatapathElementClass extends DatapathElement
  {
    ClassLoader loader;
    
    Class<?> clazz;

    public void set_javapath(String text){ this.ident = text; }
    
    public void set_loader(ClassLoader arg){ this.loader = arg; } 
    
    public void set_Class(Class<?> arg){ this.clazz = arg; } 
    
  }
 
  
  public static final class ObjMethod
  { public final Method method;
    public final Object obj;

    public ObjMethod(Method method, Object obj)
    { this.method = method;
      this.obj = obj;
    }
  }
  
  
  
  
  /**Result of an {@link DataAccess#access(List, Object, Map, boolean, boolean, boolean, Dst)}
   * to store a value.
   */
  public static final class Dst
  {
    protected Field field;
    
    protected Object obj;
    
    /**Sets the val to the given instance.
     * @param val It is tried to cast, see 
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    public void set(Object val) throws IllegalArgumentException, IllegalAccessException
    {
      Conversion conversion = checkArgTypes(field.getType(), val.getClass(), val);
      if(conversion !=null){ 
        Object val2 = conversion.convert(val);
        field.set(obj, val2);
      }
      else throw new IllegalArgumentException("DataAccess - cannot assign >>" + field + "<< = >>" + val + "<<");
    }
    
  }
  
  
  
  /**This class wraps any Object which is used for a variable. 
   * A Variable is helpfully if the content
   * of referenced instances should be changed without changing all references:
   * <pre>
   *      any_instance---------------->|
   *      other_instance-------------->|
   *                              Variable
   *                                   |--value------->references_somewhat
   *                                       ^   \
   *                                       |    ------>other_reference
   *                                     is changed:  
   *                                   
   * </pre>                                   
   * 
   * <br><br>
   * A variable is member of a 
   * container <code>Map< String, DataAccess.Variable></code> which is used to access in the {@link DataAccess}
   * class and which is used especially for variables in the {@link org.vishia.cmd.JZtxtcmdExecuter#setScriptVariable(String, Object)}
   * and {@link org.vishia.cmd.JZtxtcmdExecuter.ExecuteLevel#setLocalVariable(String, Object)}
   * which are accessed with the {@link DataAccess} class while setting and evaluating.
   * A user can build a datapool independently of the JZcmd approach writing the code:
   * <pre>
   *   Map< String, DataAccess.Variable> datapool = new TreeMap< String, DataAccess.Variable>();
   *   String name = "thename";
   *   DataAccess.Variable variable = new DataAccess.Variable('O', name, anyInstance);
   *   datapool.put(variable);
   * </pre>
   * This datapool can be used to access with {@link DataAccess#getData(List, Object, Map, boolean, boolean)}.
   */
  public final static class Variable<T>{
    
    /**Type of the variable. Type designations see {@link #type()}. */
    protected char type;
    
    /**A Class which should be base class of the value. */
    protected Class<? extends T> clazz;
    
    /**Property whether this variable should be non-changeable (true) or changeable (false). 
     * It should be tested and realized on runtime. 
     * <br>A const variable can be set by another 'const' value, see {@link DataAccess#createOrReplaceVariable(Map, String, char, Object, boolean)}.
     * It is prevented to change by a non-const value only.  */
    protected boolean isConst;
    
    /**Same name of the variable like it is stored in the container. */
    protected final String name;
    
    /**Reference to the data. */
    protected T value
    ;
    
    public Variable(char type, String name, T value){
      this.type = type; this.name = name; this.value = value;
      if(value !=null){ 
        @SuppressWarnings("unchecked")
        Class<? extends T>clazz1 = (Class<? extends T>) value.getClass(); 
        clazz = clazz1;
      }
    }
    
    /**Creates a variable which's value is const or not.
     * @param type One of "SAPLFOEVC" see {@link #type()}
     * @param name
     * @param value
     * @param isConst true then the value is const.
     */
    public Variable(char type, String name, T value, boolean isConst){
      this(type, name, value);
      this.isConst = isConst;
    }
    
    /**Builds a copy of this. 
     * @param src any variable
     */
    public Variable(Variable<T> src){
      this.type = src.type; this.name = src.name; this.isConst = src.isConst;
      this.clazz = src.clazz;
      if(src.value instanceof Appendable && src.value instanceof CharSequence){ this.value = /*new StringBuilder((CharSequence)*/src.value; }
      else{ this.value = src.value; }
    }
    
    public String name(){ return name; }
    
    public T value(){ return value; }
    
    /**Returns the type of the variable: 
     * The type follows the {@link DatapathElement#whatisit} on creation of the variable:
     * <ul>
     * <li>'A': A new Appendable variable
     * <li>'C': A new Class variable
     * <li>'E': A new environment variable
     * <li>'F': ? open file
     * <li>'K': A new Value variable
     * <li>'L': A new list container.
     * <li>'O': A new Object variable
     * <li>'P': A new pipe variable.
     * <li>'Q': A new Boolean variable
     * <li>'S': A new String variable
     * <li>'V': ?? container
     * <li>'X': A new Codeblock variable (eXecute, Subtextvar, Subroutinevar)
     * </ul>
     */
    public char type(){ return type; }
    
    public boolean isConst(){ return isConst; }
    
    public void setValue(T value){ this.value = value; }
    
    @Override public String toString(){ return "Variable " + type + " " + name + " = " + value; }
  }
  

  /**Converts a value in a byte to a unsigned value, which can be stored in a short in Java. Note that unsigned values
   * are often used in C, and stored in a less memory location such as a byte if possible. For algorithm which are used in C too
   * (tranlated with Java2C), this feature may be necessary. 
   * @param val a byte value in range -128..0..127
   * @return unsigned value of the byte, -128..-1 is presented by 128..255
   */
  public short shortFromUnsignedByte(byte val){ return val >= 0 ? val : (short)(((int)val)+ 256); }
  
  /**Converts a value in a short to a unsigned value, which can be stored in a int in Java. Note that unsigned values
   * are often used in C, and stored in a less memory location such as a byte if possible. For algorithm which are used in C too
   * (tranlated with Java2C), this feature may be necessary. 
   * @param val a short value in range -32768..0..32767
   * @return unsigned value of the byte, -32768..-1 is presented by 32768..65535
   */
  public int intFromUnsignedShort(short val){ return val >= 0 ? val : (((int)val)+ 32768); }
  
  
  
  public static class Test
  {
    public static void testArrayElement() 
    {
      float[][] test = new float[5][3];
      Object value = getArrayElement(test, 2);
      System.out.println(value);
    }
  }
  
  
}



//==JZcmd==
//JZcmd main(){
//JZcmd  %org.vishia.util.DataAccess$Test.testArrayElement();
//JZcmd }
//==endJZcmd==
