/****************************************************************************
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
 * 2009-04-26: Hartmut corr: Now all float or int parse result can set a int, long, float double fields and set_method(value).
 * 2009-04-26: Hartmut corr: better Exception text if access a non public field as components output.
 * 2009-03-23: Hartmut chg: total new structuring. Functional change is: 
 *                          Now inner classes in the output classes are unnecessary,
 *                          a destination class is found as type of a new_semantic()-method
 *                          or as type of field for a component. It should be public, but anywhere in the code.
 *                          All other functions are compatible. 
 * 2009-03-08: Hartmut new: setOutputFields. It is strict, but accept fields, not only methods.
 * 2008-04-02: Hartmut some changes
 * 2006-05-15: Hartmut www.vishia.de creation
 *
 ****************************************************************************/
package org.vishia.zbnf;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.*;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.text.ParseException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.vishia.mainCmd.Report;
import org.vishia.util.StringPart;
import org.vishia.util.StringPartFromFileLines;



@SuppressWarnings("unchecked")

/**This class helps to convert a ZbnfParseResult into a tree of Java objects.
 * The user have to be provide a user Object which contains 
 * public Fields or public new_-, set_- or add_-methods for any parse result.
 * <br>
 * For syntax components, either a field with the appropriate name (semantic identifier) 
 * or a pair of <code>type new_<i>semantic</i>()</code> 
 * and a appropriate <code>void set_<i>semantic</i>(type)</code> or <code>void add_<i>semantic</i>(type)</code>
 * should be exist. Either the <code>type new_<i>semantic</i>()</code>-method is invoked to get an instance,
 * than an associated <code>void add_<i>semantic</i>(type)</code> or <code>void set_<i>semantic</i>(type)</code>
 * should be exist and it is called after fill in the content of the component.
 * The return type of the <code>type new_<i>semantic</i>()</code>-method, or the field type of non-containers
 * or the generic-type of <code>List</code> fields is used to search fields or methods for the component.
 * This types may be defined somewhere else in the Java-code, but it should be public.
 * <br>
 * For repetition in the ZBNF container objects should be used. 
 * The <code>void add_<i>semantic</i>(type)</code>-method should add to it, the container may be encapsulated than,
 * or a field should be from type <code>java.util.List</code>. Than an instance of the generic type will be added.
 * <br> 
 * The used elements should be public, because the access to it using reflection is done.
 * Using public fields is more simple, but using public methods is better to debug and encapsulate.
 * It is possible to use interfaces.
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
 * <li>An element <code>public int inputColumn_</code> or a method <code>set_inputColumn_(int)</code>
 *     if the input column were the parsed text is found should be stored. 
 *     This is important at example if line structures were parsed.
 * <li>If no appropriate method or field is found, either an IllegalArgumentException is thrown
 *     or the errors are collected and returned in a String. The first behavior is practicable, 
 *     because mostly a false content of destination classes is a writing error of the program.
 *     The variante to produce a collected error text is more for debugging.    
 * </ul>
 * <br><br>
 * If a element is parsed only one time, a simple element to store the result is adequate. 
 * If an element may be parsed some times, in a repetition, a list<Type> should be given. 
 * Elsewhere a new parsed element overwrites the previous.
 * Example:
 * <pre>TODO
 * </pre> 
 */

public class ZbnfJavaOutput
{
	private final Report report;
	
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
  
  private  StringBuffer errors;
  
  private Class[] outputClasses; 
  
  public ZbnfJavaOutput()
  { report = null;
    init();
  }
  
  public ZbnfJavaOutput(Report report)
  { this.report = report;
    init();
  }

  private ZbnfJavaOutput(Report report, boolean strict, boolean methods)
  { this.report = report;
    init();
    this.bOnlyMethods = methods; 
  }

  
  public void setMethodsOnly(boolean value){ this.bOnlyMethods = value; }
  
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
    //The first child item:
    Iterator<ZbnfParseResultItem> iterChildren = resultItem.iteratorChildren();
    //loop of all first level elements, the output is written directly in topLevelOutput:
    while(iterChildren.hasNext())
    { ZbnfParseResultItem child = iterChildren.next();
      writeZbnfResult( topLevelClass, topLevelInstance, child, 1);
    }
  
    return errors == null ? null : errors.toString();
  }
  
  
  
  /** writes the parsers result into a tree of Java objects using the reflection method.
   * @param topLevelOutput The toplevel instance
   * @param resultItem The toplevel parse result
   * @param report
   * @throws IllegalAccessException if the field is found but it is not public.
   * @throws IllegalArgumentException 
   * @throws InstantiationException if a matching class is found but it can't be instanciated. 
   */
  public static void setOutputStrict(Object topLevelOutput, ZbnfParseResultItem resultItem, Report report) 
  throws IllegalArgumentException, IllegalAccessException, InstantiationException
  { setOutput(topLevelOutput, resultItem, report, true, true);
  }
  
  
  /** writes the parsers result into a tree of Java objects using the reflection method.
   * @param topLevelOutput The toplevel instance
   * @param resultItem The toplevel parse result
   * @param report
   * @throws IllegalAccessException if the field is found but it is not public.
   * @throws IllegalArgumentException 
   * @throws InstantiationException if a matching class is found but it can't be instanciated. 
   */
  public static void setOutputFields(Object topLevelOutput, ZbnfParseResultItem resultItem, Report report) 
  throws IllegalArgumentException, IllegalAccessException, InstantiationException
  { setOutput(topLevelOutput, resultItem, report, true, false);
  }

  
  
  /** writes the parsers result into a tree of Java objects using the reflection method.
   * @param topLevelOutput The toplevel instance
   * @param resultItem The toplevel parse result
   * @param report
   * @throws IllegalAccessException if the field is found but it is not public.
   * @throws IllegalArgumentException 
   * @throws InstantiationException if a matching class is found but it can't be instanciated. 
   */
  public static void setOutput(Object topLevelOutput, ZbnfParseResultItem resultItem, Report report, boolean strict, boolean bOnlyMethods) 
  throws IllegalArgumentException, IllegalAccessException, InstantiationException
  { //instance of writer only for temporary help to organize, no data are stored here: 
    ZbnfJavaOutput instance = new ZbnfJavaOutput(report, strict, bOnlyMethods);  
    //the available classes of the top level output instance
    instance.outputClasses = topLevelOutput.getClass().getClasses();
    //The first child item:
    Iterator<ZbnfParseResultItem> iterChildren = resultItem.iteratorChildren();
    //loop of all first level elements, the output is written directly in topLevelOutput:
    while(iterChildren.hasNext())
    { ZbnfParseResultItem child = iterChildren.next();
      instance.writeZbnfResult( topLevelOutput.getClass(), topLevelOutput, child, 1);
    }
  }

  /**@deprecated, use {@link #setOutputStrict(Object, ZbnfParseResultItem, Report)}
   * or {@link #setOutputFields(Object, ZbnfParseResultItem, Report)}
   * @param topLevelOutput
   * @param resultItem
   * @param report
   * @throws IllegalArgumentException
   * @throws IllegalAccessException
   * @throws InstantiationException
   */
  @SuppressWarnings("deprecation")
  public static void setOutput(Object topLevelOutput, ZbnfParseResultItem resultItem, Report report) 
  throws IllegalArgumentException, IllegalAccessException, InstantiationException
  { //instance of writer only for temporary help to organize, no data are stored here: 
    ZbnfJavaOutput instance = new ZbnfJavaOutput(report, false, false);  
    //the available classes of the top level output instance
    instance.outputClasses = topLevelOutput.getClass().getClasses();
    //The first child item:
    ZbnfParseResultItem childItem = resultItem.nextSkipIntoComponent(null /*no parent*/);
    //loop of all first level elements, the output is written directly in topLevelOutput:
    while(childItem != null)
    { instance.writeZbnfResult( topLevelOutput.getClass(), topLevelOutput, childItem, 1);
      childItem = childItem.next(resultItem);
    }
  }
    
  /** Writes the content of an parse result item into the outputInstance.
   * It the resultItem is not a component, a matching field or method is to be searched
   * and the content of the resultitem is written into.
   * But if the resultItem is a component, a matching class with the semantic
   * of the component, but starts with uppercase, is searched, an instance of it
   * is created and the method is called recursively for the content of the component.
   * The created instance is added to the parentOutputInstance.
   * 
   * @param parentOutputInstance The instance to add childs in some lists
   * @param resultItem
   * @throws IllegalAccessException if the field is not public.
   * @throws IllegalArgumentException 
   * @throws InstantiationException if a matching class is found but it can't be instanciated. 
   */  
  private void writeZbnfResult
  ( Class parentClass, Object parentOutputInstance
  , ZbnfParseResultItem resultItem
  , int recursion
  ) 
  throws IllegalArgumentException, IllegalAccessException, InstantiationException
  {
    final String semantic1 = resultItem.getSemantic();
    /**If the semantic is determined to store in an attribute in xml, the @ is ignored here: */
    final String semantic = semantic1.startsWith("@") ? semantic1.substring(1) : semantic1;
    if(semantic.equals("timestep"))
      stop();
    report.reportln(Report.fineDebug, recursion, "ZbnfJavaOutput: " + semantic + ":");
      
    if(resultItem.isComponent())
    { 
      /**Try to save the content also if it is an component. */
      if(resultItem.isOption() && resultItem.getParsedString() != null)
      { searchDestinationAndWriteResult(semantic, parentClass, parentOutputInstance, resultItem);
      }
      
      /**Search an instance (field or method result) which represents the semantic of the component. 
       * That instance will be used to fill the parse result of the component.
       */
      ChildInstanceAndClass componentsInstance = searchComponentsDestination
        ( semantic                //the semantic of the component.
        , parentClass             //reference type of parentOutputInstance
        , parentOutputInstance    //instance where the field or method for the component should be found.
        );
      if(componentsInstance != null)
      { /**Such an instance is found, use it to fill.
         * That instance may also be an new element of a container
         */
        /**First try if an field <code>inputColumn_</code> exists, than write the line.position there. */
        int inputColumn = resultItem.getInputColumn();
        trySetInputColumn("", componentsInstance.clazz, componentsInstance.instance, inputColumn);
        /** skip into the component resultItem: */
        Iterator<ZbnfParseResultItem> iterChildren = resultItem.iteratorChildren();
        while(iterChildren.hasNext())
        { ZbnfParseResultItem childItem = iterChildren.next();
          writeZbnfResult(componentsInstance.clazz, componentsInstance.instance, childItem, recursion+1);
        }
        if(componentsInstance.shouldAdd)
        {
          searchAddMethodAndInvoke(semantic, parentClass, parentOutputInstance, componentsInstance);
        }
      }
      else
      { 
      }  
    }
    else
    { //write the content of the resultItem into the outputInstance:
      searchDestinationAndWriteResult(semantic, parentClass, parentOutputInstance, resultItem);
    }
  }
   
  /**Instance to bundle a class to search methods or fields and the associated instance.
   * It is used especially for ZBNF-components.
   */
  private static class ChildInstanceAndClass
  { 
    /**Doc: see constructors args. */
    final Class clazz; final Object instance; final boolean shouldAdd; 
    
    /**
     * @param instance  The instance where the data should store in.
     * @param clazz     The type of the reference to the instance, not the type of the instance.
     *                  That type should be used to find components fields or method.
     *                  (Note: The type of the instance may be derivated, private or so on.)
     * @param shouldAdd true than the instance is to be add using an add_Semantic(Object)-method
     *                  of its parent, because it is got with a new_Semantic()-Method.
     *                  false than the instance is referenced from the parent already.
     */
    ChildInstanceAndClass(Object instance, Class clazz, boolean shouldAdd)
    { this.instance = instance; this.clazz = clazz; this.shouldAdd = shouldAdd; 
    }
  }
  
  
  
  
  /**Searches a method with given Name and given argument types in the class
   * or in its super-classes and interfaces.
   * @param outputClass
   * @param name
   * @param argTypesVariants
   * @return the method or null if it isn't found.
   */
  private Method searchMethod(Class outputClass, String name, Class[][] argTypesVariants)      
  { Method method;
    do
    { int ixArgTypes = 0;
      do
      { Class[] argTypes = argTypesVariants[ixArgTypes];
        try{ method = outputClass.getDeclaredMethod(name, argTypes);}
        catch(NoSuchMethodException exception){ method = null; }
      } 
      while(  method == null               //not found 
           && ++ixArgTypes < argTypesVariants.length  //but there are some variants to test.
           );
       
      /**Not found: if there is a superclass or (TODO)super-interface:  */
      if(method == null)
      { outputClass = outputClass.getSuperclass();
        if(outputClass == Object.class)
        { outputClass = null; 
        }
      }
    } while(  method == null       //not found 
           && outputClass != null);  //but there are superclassed
    return method;
  }  

  
  
  
  ChildInstanceAndClass searchCreateMethod(Class parentClass, Object parentInstance, String semantic) 
  throws IllegalArgumentException, IllegalAccessException
  {
    Method method = searchMethod(parentClass, "new_" + semantic, new Class[1][0]);
    if(method != null)
    { final Class childClass = method.getReturnType();
      final Object childOutputInstance;
      Object[] noParam = null; //without param.
      try{ childOutputInstance = method.invoke(parentInstance, noParam); }
      catch(Exception exc)
      { throw new IllegalAccessException("cannot access: " + method.toString()); 
      }
      return new ChildInstanceAndClass(childOutputInstance, childClass, true);
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
  (String semantic, Class parentClass, Object parentInstance, ChildInstanceAndClass componentsDestination) 
  throws IllegalArgumentException, IllegalAccessException
  { Class[][] argtypes = new Class[1][1];
    argtypes[0][0] = componentsDestination.clazz;
    Method method = searchMethod(parentClass, "set_" + semantic, argtypes);
    if(method == null)
    { method = searchMethod(parentClass, "add_" + semantic, argtypes);
    }
    if(method != null)
    { Object[] argMethod = new Object[1];
      argMethod[0] = componentsDestination.instance;
      try{ method.invoke(parentInstance, argMethod); }
      catch(InvocationTargetException exc)
      { throw new IllegalAccessException("the called method " +method.toGenericString() + " throws an Exception: " + exc.getTargetException() );
      }
      catch(Exception exc)
      { throw new IllegalAccessException("can not access: " + parentClass.getCanonicalName()  + ".add_" + semantic + "(...) or .set..."); 
      }
    }
    else
    { String problem = "method set_- or add_" +semantic+"(" + componentsDestination.clazz.getCanonicalName() + ") not found in " + parentClass.getCanonicalName();
      if(bExceptionIfnotFound) throw new IllegalArgumentException(problem);
      else noteError(problem);
    }
  }
  
  
  
  
  
  
  
  /**Searches a class and instance appropriate to the given semantic to store a component.
   * Either a <code>new_<i>semantic</i>()</code>-method is found or a field with the given semantic
   * is found.
   * <ul>
   * <li>If a <code>new_<i>semantic</i>()</code>-method is found, it is invoked. 
   *   The result is the instance for the component. 
   *   The return type of the found method is the components class. 
   *   The method {@link #searchCreateMethod(Class, Object, String) is called.}
   * <li>If no new_-method is found, a field with the semantic-name is searched.
   *   If it is found, and it is not a container type (List), the type of the field is the components type.
   *   If the field is <code>null</code>, a new Object with the given type is created and set.
   *   The object referenced to the field is returned.
   *   <br>
   *   If the field is a container type, a <code>List</code>, than a new Object with the generic type
   *   of the List is created and added. See called method {@link #getComponentsOutputField(Field, Object)}.    
   * </ul>
   * @param semantic The name of the new_-method or the field.
   * @param parentType The class where the method or field is searched.
   * @param parentObj The object associated to the class which contains the field or it is the instance to call the new_-method.
   * @return The class and the instance for the component. If a new_-method is called, 
   *         the attribute {@link ChildInstanceAndClass#shouldAdd} is set true. 
   *         Because the add_-method should called also. 
   * @throws IllegalArgumentException If no method or field is found and {@link #bExceptionIfnotFound} is set.
   * @throws IllegalAccessException If a problem with the field or method exists, especially the field or method should be public!
   * @throws InstantiationException If a problem calling the new_-method exists. 
   */
  private ChildInstanceAndClass searchComponentsDestination(String semantic, Class parentType, Object parentObj) 
  throws IllegalArgumentException, IllegalAccessException, InstantiationException
  { /**The returned instance if resultItem is null, and the field is searched. */
    ChildInstanceAndClass child = null;
    int posSeparator = semantic.indexOf('/');
    if(posSeparator >0)
    { String sematicFirst = semantic.substring(0, posSeparator);
      child = searchComponentsDestination(sematicFirst, parentType, parentObj);
      String semanticRest = semantic.substring(posSeparator+1);
      //Class outputClassRest = outputInstanceRest.getClass();
      return searchComponentsDestination(semanticRest, child.clazz, child.instance);
    }
    else
    {
      child = searchCreateMethod(parentType, parentObj, semantic);
      if(child == null)
      { //if(!bOnlyMethods)
        { Class superClass = parentType.getSuperclass();
          char firstChar = semantic.charAt(0);
          String semanticLowerCase = firstChar >='a' && firstChar <='z' ? semantic : Character.toLowerCase(firstChar) + semantic.substring(1);
          Field element = null;
          try{ element = parentType.getDeclaredField(semanticLowerCase);}
          catch(NoSuchFieldException exception)
          { try{ element = superClass.getField(semanticLowerCase);}
            catch(NoSuchFieldException exc2){ element = null; } 
          }
          if(element != null)
          { //an element with the desired name is found, write the value to it:
            report.report(Report.fineDebug, semanticLowerCase);
            child = getComponentsOutputField(element, parentObj);
          }
          else
          { String problem = "cannot found method new_" + semantic + "() or field " + semanticLowerCase + " in class" + parentType.getCanonicalName();
            if(bExceptionIfnotFound) throw new IllegalArgumentException(problem);
            else noteError(problem);
          }
        }
        //else
        { //if(bStrict) throw new IllegalArgumentException("cannot found: " + sMethodToFind);
        }
      }  
    }    
    return child;
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
  private ChildInstanceAndClass getComponentsOutputField(Field element, Object outputInstance) 
  throws IllegalArgumentException, IllegalAccessException, InstantiationException
  { ChildInstanceAndClass child;
    Object childInstance;
    try{ childInstance = element.get(outputInstance);}
    catch(IllegalAccessException exc)
    { throw new IllegalAccessException("ZbnfJavaOutput: cannot access " + element.getName() + " in "+ outputInstance.getClass().getCanonicalName());
    }
    Class childClass = element.getType();
    String nameRef = childClass.getName();
    if(nameRef.equals("java.util.List") || nameRef.equals("java.util.LinkedList"))
    { List childContainer = (List)childInstance;
      Class genericClass = null;
      Type generic = element.getGenericType();
      if(generic instanceof ParameterizedType)
      { ParameterizedType g1 = (ParameterizedType)generic;
        Type[] t1 = g1.getActualTypeArguments();
        genericClass = (Class)t1[0];
      }
      if(childContainer == null)
      { if(nameRef.equals("java.util.List"))
        { childClass = LinkedList.class;
        }
        childContainer = (List)childClass.newInstance();
        element.set(outputInstance, childContainer);
      }
      Object childContainerInstance = genericClass.newInstance();
      childContainer.add(childContainerInstance);
      child = new ChildInstanceAndClass(childContainerInstance, genericClass, false);
    }  
    else
    { if(childInstance == null)
      { childInstance = childClass.newInstance();
        element.set(outputInstance, childInstance);
      }
      child = new ChildInstanceAndClass(childInstance, childClass, false);
    }
    return child;          
  }
  
  

  
  
  /**searches the method or field matching to the semantic of the non-ZBNF-component-resultItem 
   * and write the given content of resultItem into it, 
   * 
   * @param semanticRaw The semantic from ZBNF parse result item. 
   *        It may be a child destination: <code>part1/part2</code> or may contain an <code>@</code>
   *        to designate attributes if it will be stored in XML. If a child destination is given,
   *        that destination is searched adequat to a destination for a component, 
   *        and than this method is called recursively with that destination and the right rest of semantic.
   *        A <code>@</code> as first char is ignored.<br>
   * @param destinationClass The class where to found the method or field. It should be matching 
   *        to the destinationInstance, either it is its class, or a superclass or interface.
   *        Otherwise an IlleganAccessException is thrown.<br> 
   * @param destinationInstance The instance to write into, it should contain a matching public field or method.
   *        A field matches, if its name is equal the semantic, but with lower case at first char.
   *        A method matches, if it is named set_SEMANTIC, where SEMANTIC is the semantic of the zbnf item.
   * <br>
   * @param resultItem The semantic is determining the matching field or method. 
   *        The content is used if childOutputInstance is null.
   * @throws IllegalAccessException if the element is not writeable especially not public. 
   * @throws IllegalArgumentException 
   * @throws InstantiationException if the creation of a child instance fails.
   */
  private ChildInstanceAndClass searchDestinationAndWriteResult
  ( final String semanticRaw
  , final Class destinationClass
  , final Object destinationInstance
  , final ZbnfParseResultItem resultItem
  ) throws IllegalArgumentException, IllegalAccessException, InstantiationException
  { 
    /**The returned instance if resultItem is null, and the field is searched. */
    ChildInstanceAndClass child = null;
          
    final String semantic = semanticRaw.startsWith("@") ? semanticRaw.substring(1) : semanticRaw;
    if(semantic.equals("controlFile"))
      stop();      //test special not component (scalar) field
    int posSeparator = semantic.lastIndexOf('/');
    if(posSeparator >0)
    { String sematicFirst = semantic.substring(0, posSeparator);
      child = searchComponentsDestination(sematicFirst, destinationClass, destinationInstance);
      String semanticRest = semantic.substring(posSeparator+1);
      //NOTE: recursively call is necessary only because the destinationClass etc. are final.
      return searchDestinationAndWriteResult(semanticRest, child.clazz, child.instance, resultItem);
    }
    else
    {
      Class[][] argTypesVariants;
      if(resultItem == null)
      { //search a new_-Method
        child = searchComponentsDestination(semantic, destinationClass, destinationInstance);
      }
      else 
      { //writing of a simple element result
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
        final String sMethodToFind = destinationClass.getCanonicalName()+ ".set_" + semantic + "(" + (argTypesVariants[0].length >0 ? argTypesVariants[0][0].getName() : "void" ) + ")";
        method = searchMethod(destinationClass, "set_" + semantic, argTypesVariants);      
        if(method == null)
        { method = searchMethod(destinationClass, "add_" + semantic, argTypesVariants);      
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
          try{ method.invoke(destinationInstance, argMethod); }
          catch(InvocationTargetException exc)
          { throw new IllegalAccessException("cannot access: " + sMethodToFind + " / " + exc.getMessage()); 
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
            Class searchClass = destinationClass;
            do
            { try{ element = destinationClass.getDeclaredField(semanticLowerCase);}
              catch(NoSuchFieldException exception)
              { element = null; 
              }
            } while(  element == null    //search in all super classes.
                   && (searchClass = searchClass.getSuperclass()) != null
                   && searchClass != Object.class
                   );  
            if(element != null)
            { //an element with the desired name is found, write the value to it:
              report.report(Report.fineDebug, semanticLowerCase);
              writeInField(element, destinationInstance, resultItem);
            }
            else
            { String problem = "cannot found method " + sMethodToFind + " or field " + semanticLowerCase + " in class" + destinationClass.getCanonicalName();
              if(bExceptionIfnotFound) throw new IllegalArgumentException(problem);
              else noteError(problem);
            }
          }
          //else
          { //if(bStrict) throw new IllegalArgumentException("cannot found: " + sMethodToFind);
          }
        }  
      }    
      //search an integer field with name_inputColumn, if found write the input column if the parse result.
      trySetInputColumn(semantic, destinationClass, destinationInstance, resultItem.getInputColumn());
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
    boolean debug = report.getReportLevel() >= Report.fineDebug;
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
      else if(sType.equals("java.util.List"))
      { String value = resultItem.getParsedText();
        List list = (java.util.List)element.get(outputInstance);
        if(list == null)
        { list = new java.util.LinkedList<List>();
          element.set(outputInstance, list);
        }
        list.add(value);
        
        if(debug) debugValue = value;
      }
      else
      { throw new IllegalAccessException("Unexpected type of field: " + sType + " " + element.getName() + " in " + outputInstance.getClass().getName());
      }
    }
    catch(IllegalAccessException exc)
    {
      throw new IllegalAccessException("access to field is denied: " + outputInstance.getClass().getName() + "." + element.getName() + " /Type: " + sType); 
    }
    report.report(Report.fineDebug, " \""+ debugValue + "\" written in Element Type " + sType);
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
  private void trySetInputColumn(String semantic, Class destinationClass, Object destinationInstance, int column) 
  throws IllegalAccessException
  { if(column >=0)
    { try
      { //if an field inputColumn_ is found, write to it.
        Field elementColumn = destinationClass.getField("inputColumn_" + semantic);
        elementColumn.setInt(destinationInstance, column);
      }
      catch(NoSuchFieldException exception)
      { /**do nothing if the field isn't found.*/ 
        //not an element with the postulated name found,
        //search an appropriate method:
        Method method;
        Class[] argTypes1 = new Class[1]; 
        argTypes1[0] = Integer.TYPE;
        try
        { method = destinationClass.getDeclaredMethod("set_inputColumn_" + semantic, argTypes1);
          Object[] argMethod1 = new Object[1];
          argMethod1[0] = new Integer(column);
          method.invoke(destinationInstance, argMethod1);
        }
        catch(NoSuchMethodException exception1){ /**do nothing if the field isn't found.*/ }
        catch(InvocationTargetException exc)
        { throw new IllegalAccessException(exc.getMessage()); 
        }
      }
    }  
  }
  
  
  /**It's a debug helper. The method is empty, but it is a mark to set a breakpoint. */
  void stop()
  { //debug
  }





  /**Parses the given file with given syntax and fills the parsed result into the result object.
   * This is a simple common use-able routine to transfer textual content into content of a Java object.
   * <br>
   * NOTE:This routine is static because it is a recognition to functional programming. 
   * No side effects are occur. This method sets nothing, it returns only anything.
   *
   * @param result  An instance which should contain methods or fields appropriating to the semantic.
   *                It will be filled by parsing results.
   * @param fInput  The input file
   * @param fSyntax The syntax file using ZBNF
   * @param report  Report for parsing process and errors
   * @param msgRange A start number of created messages in report.
   * @return null if no error, else a short error text. The explicitly error text is written in report.
   */
  public static String parseFileAndFillJavaObject(Class resultType, Object result, File fInput, File fSyntax, Report report, int msgRange) 
  { ZbnfJavaOutput javaOutput = new ZbnfJavaOutput(report);
    return javaOutput.parseFileAndFillJavaObject(resultType, result, fInput, fSyntax);
  }
  
  
  /**Parses the given file with given syntax and fills the parsed result into the result object.
   * This is a simple common use-able routine to transfer textual content into content of a Java object.
   * <br>
   * The non static variant allows to set some options using class methods.
   * 
   * @param result  An instance which should contain methods or fields appropriating to the semantic.
   *                It will be filled by parsing results.
   * @param fInput  The input file
   * @param fSyntax The syntax file using ZBNF
   * @param report  Report for parsing process and errors
   * @param msgRange A start number of created messages in report.
   * @return null if no error, else a short error text. The explicitely error text is written in report.
   */
  public String parseFileAndFillJavaObject(Class resultType, Object result, File fInput, File fSyntax) 
  { String sError = null;
    int lenFileSyntax = (int)fSyntax.length();
    StringPart spSyntax = null;
    try{ spSyntax = new StringPartFromFileLines(fSyntax, lenFileSyntax, null, null); }
    catch(FileNotFoundException exc)
    { sError = "Syntax file not found: " + fSyntax.getAbsolutePath();
    }
    catch(IOException exc)
    { sError = "Syntax file read problems: " + fSyntax.getAbsolutePath() + " msg = " + exc.getMessage();
    }
    catch(IllegalArgumentException exc)
    { //it is IllegalCharsetNameException, UnsupportedCharsetException
      sError = "Syntax file charset problems: " + fSyntax.getAbsolutePath() + " msg = " + exc.getMessage();
    }
    if(sError == null)
    { sError = parseFileAndFillJavaObject(resultType, result, fInput, spSyntax);
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
   * @param result  An instance which should contain methods or fields appropriating to the semantic.
   *                It will be filled by parsing results.
   * @param fInput  The input file
   * @param fSyntax The syntax file using ZBNF
   * @param report  Report for parsing process and errors
   * @param msgRange A start number of created messages in report.
   * @return null if no error, else a short error text. The explicitely error text is written in report.
   */
  public String parseFileAndFillJavaObject(Class resultType, Object result, File fInput, StringPart spSyntax) 
  //throws FileNotFoundException, IOException, ParseException, IllegalArgumentException, InstantiationException
  { String sError = null;
    //configure the parser:
    StringPart spInput = null;
    ZbnfParser zbnfParser = null;
    if(sError == null)
    { zbnfParser = new ZbnfParser(report);
      try{ zbnfParser.setSyntax(spSyntax); }
      catch(ParseException exc)
      { sError = "ERROR in syntax prescript: " + exc.getMessage();
      }
    }  
    if(sError == null)
    {   zbnfParser.setReportIdents(Report.error, Report.info, Report.fineDebug, Report.fineDebug);
      //parse the file:
      int lenFileInput = (int)fInput.length();
      try{ spInput = new StringPartFromFileLines(fInput, lenFileInput, null, null); }
      catch(FileNotFoundException exc)
      { sError = "Input file not found: " + fInput.getAbsolutePath();
      }
      catch(IOException exc)
      { sError = "Input file read problems: " + fInput.getAbsolutePath() + " msg = " + exc.getMessage();
      }
      catch(IllegalArgumentException exc)
      { //it is IllegalCharsetNameException, UnsupportedCharsetException
        sError = "Input file charset problems: " + fInput.getAbsolutePath() + " msg = " + exc.getMessage();
      }
    }
    if(sError == null)
    { 
      boolean bOk = zbnfParser.parse(spInput);
      if(!bOk)
      { report.writeError(zbnfParser.getSyntaxErrorReport());
        sError = "ERROR syntax in input file. ";
      }
      //The content of the setting file is stored inside the parser as 'parse result'.
      //The ZbnfJavaOutput.setOutput moves the content to the class 'settings'.
      //The class settings contains the necessary elements appropriate to the semantic keywords in the syntax prescript.
      zbnfParser.reportStore(report, Report.debug);
    }
    if(sError == null)
    { 
      try{ setContent(resultType, result, zbnfParser.getFirstParseResult()); } 
      catch (IllegalAccessException exc)
      { sError = "ERROR access to elements. Hint: The elements should be public!: " + exc.getMessage();
      } 
      catch (IllegalArgumentException exc)
      { sError = "ERROR access to elements, IllegalArgumentException: " + exc.getMessage();
      } 
      catch (InstantiationException exc)
      { sError = "ERROR access to elements, InstantiationException: " + exc.getMessage();
      } 
      
    }  
    return sError;
  }
  
  

  /**Adds an error. This method is called if {@link #bExceptionIfnotFound} = false.
   * @param problem The text.
   */
  private void noteError(String problem)
  {
    if(errors == null)
    { errors = new StringBuffer();
    }
    errors.append(problem).append('\n');
  }



}
