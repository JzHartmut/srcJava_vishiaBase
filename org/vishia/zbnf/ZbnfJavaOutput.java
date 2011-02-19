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
 * 2006-05-15: JcHartmut www.vishia.de creation
 * 2008-04-02: JcHartmut some changes
 *
 ****************************************************************************/
package org.vishia.zbnf;

import java.lang.reflect.*;
import java.util.Iterator;
import java.util.List;

import org.vishia.mainCmd.Report;



@SuppressWarnings("unchecked")

/**This class helps to convert a ZbnfParseResult into a tree of Java objects.
 * The user have to be provide a user Object which contains 
 * <ul>
 * <li><code>public static class Name{...}</code> as inner classes for each type of Syntax Component. 
 *    The identifier should start with upper case, independent of the name of the syntax component.
 * <li>public Fields or public set- or add-methods for every parse result.
 * </ul>
 * The inner classes should contain also public fields or public set- or add-methods for every parse result
 * of the component. Using set- and add method is the better encapsulated mode. 
 * Using public fields is more simple.
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
 * <li>The identifier of a sub syntax prescript is used as name for the class type for the inner class
 *     and for the field type or argument type for set- and add- methods 
 *     to store the instance of a component class Type in its parent.  
 * <li>First a <code>set_name(Type)</code>- or <code>add_name(Type)</code>-method
 *     is searched and invoked, if found. 
 * <li>The type of the methods argument is predetermined by the type of the stored result.
 *     For components, it is the component instance type. For elements, the type of elements 
 *     determine the argument type:
 * <li>If no method is found, a field <code>Type name</code> is searched. 
 *     If it is found, the type is tested.
 *     For parse result elements, the type of fields must be java.lang.String or int, float or double.
 *     To store instances for parse result components, the type of the field have to be the same 
 *     type like the components class. If a field isn't found, it is okay. If the type are false,
 *     an IllegalArgumentException is thrown. This behavior is practicable, 
 *     because mostly a false type is a writing error of the program.         
 * <li>java.util.List<Type> name fields for the child parse result components,</li>
 * <li>Xxx xxx fields for the child parse result components if only 1 child is able.
 * <li>java.lang.String name-fields for parsed string represented results,</li>
 * <li> int,float,double xxx fields for parsed numbers.</li>
 * <li>An element <code>public int inputColumn_</code> or a method <code>set_inputColumn_(int)</code>
 *     if the input column were the parsed text is found should be stored. 
 *     This is important at example if line structures were parsed.
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
	final Report report;
	
	/**If it is set, an exception is thrown when no element is found. */
	final boolean bStrict;
	
	/**If it is set, only set_ or add_-methods and new_-methods are accepted,
	 * no fields and no inner classes as container.
	 */
  final boolean bOnlyMethods;
  
  private Class[] outputClasses; 
  
  private ZbnfJavaOutput(Report report, boolean strict, boolean methods)
  { this.report = report;
    this.bStrict = strict; this.bOnlyMethods = methods; 
  }

  
  
  /** writes the parsers result into a tree of Java objects using the reflection method.
   * @param topLevelOutput The toplevel instance
   * @param resultItem The toplevel parse result
   * @param report
   * @throws IllegalAccessException if the field is found but it is not public.
   * @throws IllegalArgumentException 
   * @throws InstantiationException if a matching class is found but it can't be instanciated. 
   */
  @SuppressWarnings("deprecation")
  public static void setOutputStrict(Object topLevelOutput, ZbnfParseResultItem resultItem, Report report) 
  throws IllegalArgumentException, IllegalAccessException, InstantiationException
  { //instance of writer only for temporary help to organize, no data are stored here: 
    ZbnfJavaOutput instance = new ZbnfJavaOutput(report, true, true);  
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
  @SuppressWarnings("deprecation")
  private void writeZbnfResult( Class parentClass, Object parentOutputInstance
            , ZbnfParseResultItem resultItem
            , int recursion
            //, ZbnfParseResultItem parentItem
            ) throws IllegalArgumentException, IllegalAccessException, InstantiationException
  {
    String semantic = resultItem.getSemantic();
    if(semantic.equals("fixArraySizes"))
      stop();
    report.reportln(Report.fineDebug, recursion, "ZbnfJavaOutput: " + semantic + ":");
      
    if(resultItem.isComponent())
    { 
      /**Try to save the content also if it is an component. */
      if(resultItem.isOption() && resultItem.getParsedString() != null)
      { search_nWriteZbnfItem(parentClass, parentOutputInstance, resultItem);
      }
      
      final Object childOutputInstance;
      final Class childClass;
      if(!bOnlyMethods)
      { //search a type instance with type of semantic
        char firstUpperChar = Character.toUpperCase(semantic.charAt(0)); 
        String nameOutputClass = firstUpperChar + semantic.substring(1);
        //if(Character.isLowerCase(semantic[0]))
        int ii;
        for(ii=0; ii < outputClasses.length; ii++) //NOTE: break
        { String nameTest = outputClasses[ii].getName(); 
          int posDelimiter = nameTest.lastIndexOf('$');  //it is an inner class
          if(posDelimiter >=0){ nameTest = nameTest.substring(posDelimiter +1);}
          if(nameTest.equals(nameOutputClass))
          { break;
          }
        }
        if(ii < outputClasses.length)
        { report.report(Report.fineDebug, " found");
          childClass = outputClasses[ii];
          childOutputInstance = childClass.newInstance();
        }
        else 
        { ChildInstanceAndClass y = searchCreateMethod(parentClass, parentOutputInstance, semantic);
          childClass = y.clazz;
          childOutputInstance = y.instance;
        }
      }  
      else 
      { ChildInstanceAndClass y = searchCreateMethod(parentClass, parentOutputInstance, semantic);
        childClass = y.clazz;
        childOutputInstance = y.instance;
      }
      
      if(childClass == null)
      { if(!bOnlyMethods)
        { search_nWriteZbnfItem(parentClass, parentOutputInstance, resultItem);
        }
        else
        { if(bStrict) throw new IllegalArgumentException("method new_" +semantic+"() not found in " + parentClass.getCanonicalName());
        }
      }
      else
      { try
        { //if an field _inputColumn is found, write to it.
          Field elementColumn = childClass.getField("inputColumn_");
          elementColumn.setInt(childOutputInstance, resultItem.getInputColumn());
        }
        catch(NoSuchFieldException exception){ /**do nothing if the field isn't found.*/ }
          
        // skip into the component resultItem:
        Iterator<ZbnfParseResultItem> iterChildren = resultItem.iteratorChildren();
        while(iterChildren.hasNext())
        { ZbnfParseResultItem childItem = iterChildren.next();
          writeZbnfResult(childClass, childOutputInstance, childItem, recursion +1);
        }
        
        //write in parent:
        search_nWriteComponentOrContentToOutput(parentClass, parentOutputInstance, childOutputInstance, resultItem);
          
      }
    }
    else
    { //write the content of the resultItem into the outputInstance:
      search_nWriteZbnfItem(parentClass, parentOutputInstance, resultItem);
    }
  }
   
  
  private static class ChildInstanceAndClass
  { final Class clazz; final Object instance; 
    
    ChildInstanceAndClass(Object instance, Class clazz)
    { this.instance = instance; this.clazz = clazz; }
  }
  
  ChildInstanceAndClass searchCreateMethod(Class parentClass, Object parentInstance, String semantic) 
  throws IllegalArgumentException, IllegalAccessException
  {
    Method method;
    final Class childClass;
    final Object childOutputInstance;
    Class superClass = parentClass.getSuperclass();
    Object[] argMethod1 = new Object[0];
    Class[] argTypes1 = new Class[0];  //method with 1 arg
    try{ method = parentClass.getDeclaredMethod("new_" + semantic, argTypes1);}
    catch(NoSuchMethodException exception){ method = null; }
    if(method == null)
    { try{ method = superClass.getDeclaredMethod("new_" + semantic, argTypes1);}
      catch(NoSuchMethodException exc2){ method = null; }
    }
    if(method != null)
    { childClass = method.getReturnType();
      try{ childOutputInstance = method.invoke(parentInstance, argMethod1); }
      catch(Exception exc)
      { throw new IllegalAccessException("can not access: " + parentClass.getCanonicalName()  + ".new_" + semantic + "()"); 
      }
      return new ChildInstanceAndClass(childOutputInstance, childClass);
    }
    else return new ChildInstanceAndClass(null, null);
  }
  
  
  
  
  private void search_nWriteZbnfItem(Class outputClass, Object outputInstance, ZbnfParseResultItem resultItem) 
  throws IllegalArgumentException, IllegalAccessException
  { search_nWriteComponentOrContentToOutput(outputClass, outputInstance, null, resultItem);
  }
  
  
  
  /**searches the method or field matching to the semantic of the resultItem and write either
   * the given resultInstance into it, 
   * or write the content of the result item, if the resultInstance is null.
   * 
   * @param outputClass The class where to found the method or field. It should be matching 
   *        to the outputInstance, either it is its class, or a superclass or interface.
   *        Otherwise an IlleganAccessException is thrown. 
   * @param outputInstance The instance to write into, it should contain a matching public field or method.
   *        A field matches, if its name is equal the semantic, but with lower case at first char.
   *        A method matches, if it is named set_SEMANTIC, where SEMANTIC is the semantic of the zbnf item.
   * <br>
   * @param componentsInstance If not null, it is set or added to the outputInstance.
   *        If it is null, the value in the result-item is set to the field or method of the output instance.
   * <br> 
   * @param resultItem The semantic is determining the matching field or method. 
   *        The content is used if childOutputInstance is null.
   * @throws IllegalAccessException if the element is not writeable especially not public. 
   * @throws IllegalArgumentException 
   */
  private void search_nWriteComponentOrContentToOutput
  ( Class outputClass
  , Object outputInstance
  , Object componentsInstance
  , ZbnfParseResultItem resultItem
  ) throws IllegalArgumentException, IllegalAccessException
  { //add the result to an element:
    final String semantic = resultItem.getSemantic();
    if(semantic.equals("fixArraySizes"))
      stop();      //test special not component (scalar) field
    char firstChar = semantic.charAt(0);
    String semanticUpperCase = firstChar >='A' && firstChar <='Z' ? semantic : Character.toUpperCase(firstChar) + semantic.substring(1);
    //Class outputClass = outputInstance.getClass();
    Class superClass = outputClass.getSuperclass();
    Object[] argMethod1 = new Object[1];
    Class[] argTypes1 = new Class[1];  //method with 1 arg
    Class[] argTypes0 = new Class[0];  //void method
    //search an appropriate method:
    Object[] argMethod = argMethod1;
    Class[] argTypes = argTypes1;
    if(componentsInstance == null)
    { //writing of a simple element result
      if(resultItem.isInteger())
      { argMethod[0] = new Integer((int)resultItem.getParsedInteger());
        argTypes[0] = Integer.TYPE;
      }
      else if(resultItem.isFloat())
      { argMethod[0] = new Double(resultItem.getParsedFloat());
        argTypes[0] = Double.TYPE;
      }
      else if(  resultItem.isString()
             || resultItem.isIdentifier()
             || resultItem.isTerminalSymbol()
             || resultItem.isOption() && resultItem.getParsedString()!=null
             )
      { //argMethod[0] = new Integer((int)resultItem.getNrofAlternative());
        argMethod[0] = new String(resultItem.getParsedString());
        argTypes[0] = String.class;
      }
      else
      { //no data in element, search of a argument-less set_- or add_-method: 
        argTypes = argTypes0; 
        argMethod = null;
      }
    }
    else
    { //childOutputInstance given,
      //than search of a set_ or add_-method for the type of the childOutputInstance.
      argTypes[0] = componentsInstance.getClass();
      argMethod[0] = componentsInstance;
    }
    Method method;
    final String sMethodToFind = outputClass.getCanonicalName()+ ".set_" + semantic + "(" + (argTypes.length >0 ? argTypes[0].getName() : "void" ) + ")";
    try{ method = outputClass.getDeclaredMethod("set_" + semantic, argTypes);}
    catch(NoSuchMethodException exception){ method = null; }
    if(method == null && superClass != null)
    { try{ method = superClass.getDeclaredMethod("set_" + semantic, argTypes);}
      catch(NoSuchMethodException exc2){ method = null; }
    }
    if(method == null)
    { try{ method = outputClass.getDeclaredMethod("add_" + semantic, argTypes);}
      catch(NoSuchMethodException exc2){ method = null; }
    }
    if(method == null && superClass != null)
    { try{ method = superClass.getDeclaredMethod("add_" + semantic, argTypes);}
      catch(NoSuchMethodException exc2){ method = null; }
    }
    if(method != null)
    { //ivoke the method with the given matching args.
      try{ method.invoke(outputInstance, argMethod); }
      catch(InvocationTargetException exc)
      { throw new IllegalAccessException("cannot access: " + sMethodToFind + " / " + exc.getMessage()); 
      }
    }
    else 
    { //if(!bOnlyMethods)
      { Field element;
        String semanticLowerCase = firstChar >='a' && firstChar <='z' ? semantic : Character.toLowerCase(firstChar) + semantic.substring(1);
        try{ element = outputClass.getDeclaredField(semanticLowerCase);}
        catch(NoSuchFieldException exception)
        { try{ element = superClass.getField(semanticLowerCase);}
          catch(NoSuchFieldException exc2){ element = null; } 
        }
        if(element != null)
        { //an element with the desired name is found, write the value to it:
          report.report(Report.fineDebug, semanticLowerCase);
          if(componentsInstance != null)
          { writeChildElement(element, outputInstance, componentsInstance);
          }
          else
          { writeInElement(element, outputInstance, resultItem);
          }
        }
        else
        { if(bStrict) throw new IllegalArgumentException("cannot found: " + sMethodToFind + " / " + semanticLowerCase);
        }
      }
      //else
      { //if(bStrict) throw new IllegalArgumentException("cannot found: " + sMethodToFind);
      }
    }  
      
    //search an integer field with name_inputColumn, if found write the input column if the parse result.
    try
    { //if an field inputColumn_ is found, write to it.
      Field elementColumn = outputClass.getField("inputColumn_" + semantic);
      elementColumn.setInt(outputInstance, resultItem.getInputColumn());
    }
    catch(NoSuchFieldException exception)
    { /**do nothing if the field isn't found.*/ 
      //not an element with the postulated name found,
      //search an appropriate method:
      argTypes1[0] = Integer.TYPE;
      try
      { method = outputClass.getDeclaredMethod("set_inputColumn_" + semantic, argTypes1);
        argMethod1[0] = new Integer(resultItem.getInputColumn());
        method.invoke(outputInstance, argMethod1);
      }
      catch(NoSuchMethodException exception1){ /**do nothing if the field isn't found.*/ }
      catch(InvocationTargetException exc)
      { throw new IllegalAccessException(exc.getMessage()); 
      }
    }
          
              
  }
  

  
  private void writeInElement( Field element
                             , Object outputInstance
                             , ZbnfParseResultItem resultItem
                             )
  throws IllegalAccessException
  { String sType = element.getType().getName();
    String debugValue = "???";
    boolean debug = report.getReportLevel() >= Report.fineDebug;
    try
    { if(sType.equals("int"))
      { int value = (int)resultItem.getParsedInteger();
        element.setInt(outputInstance, value);
        if(debug) debugValue = "" + value;
      }
      else if(sType.equals("long"))
      { long value = resultItem.getParsedInteger();
        element.setLong(outputInstance, value);
        if(debug) debugValue = "" + value;
      }
      else if(sType.equals("float"))
      { float value = (float)resultItem.getParsedFloat();
        element.setFloat(outputInstance, value);
        if(debug) debugValue = "" + value;
      }
      else if(sType.equals("double"))
      { double value = resultItem.getParsedFloat();
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

  
  private void writeChildElement( Field element
                             , Object outputInstance
                             , Object child
                             )
  throws IllegalAccessException
  { String sType = element.getType().getName();
    String sNameOutputClass = child.getClass().getName();
    try
    {
      if(sType.equals("java.util.List"))
      { java.util.List list = (java.util.List)element.get(outputInstance);
        if(list == null)
        { list = new java.util.LinkedList();
          element.set(outputInstance, list);
        }
        list.add(child);
        report.report(Report.fineDebug, " child written, type List.");
      }
      else if(sType.equals(sNameOutputClass))
      { element.set(outputInstance, child);
        
      }
      else
      { report.report(Report.fineDebug, " child not written, unknown type "+ sType);
      }
    }
    catch(Exception exc)
    { throw new IllegalAccessException("cannot access: " + element.getDeclaringClass().getCanonicalName() + "." + element.getName());
    }
  } 
  
  /**It's a debug helper. The method is empty, but it is a mark to set a breakpoint. */
  void stop()
  { //debug
  }
}
