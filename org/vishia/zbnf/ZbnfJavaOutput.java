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
import java.util.List;

import org.vishia.mainCmd.Report;



@SuppressWarnings("unchecked")

/**This class helps to convert a ZbnfParseResult into a tree of Java objects.
 * The user have to be provide a toplevel Object which contains 
 * <ul><li>java.util.List of objects for the child parse result levels,</li>
 * <li>java.lang.String for parsed string represented results,</li>
 * <li> int,float,double for parsed numbers.</li></ul>
 * <br>
 * In the current version the toplevel class has to contain inner classes for the child objects
 * and empty lists. All elements (fields) should be public. Please see the sample.
 * The name of the fields and sub-classes(current) should be the same as the semantic of the parse results,
 * but with the naming conventions of Java (first letter of classes in upper case, ..of fields in lower case.
 * <br>
 * In future releases there will be Object=add_Semantic() and set_Semantic(String) - Methods which should to be public.
 * The add methods should be called if components are in the parse result. The name of the methods after add_... or set_... 
 * should be following the semantic 
 * <br>
 * 
 */

public class ZbnfJavaOutput
{
	final Report report;

  private Class[] outputClasses; 
  
  private ZbnfJavaOutput(Report report)
  { this.report = report;
  }

  /** writes the parsers result into a tree of Java objects using the reflection method.
   * @param topLevelOutput The toplevel instance
   * @param resultItem The toplevel parse result
   * @param report
   * @throws IllegalAccessException if the field is found but it is not public.
   * @throws IllegalArgumentException 
   */
  @SuppressWarnings("deprecation")
  public static void setOutput(Object topLevelOutput, ZbnfParseResultItem resultItem, Report report) throws IllegalArgumentException, IllegalAccessException
  { //instance of writer only for temporary help to organize, no data are stored here: 
    ZbnfJavaOutput instance = new ZbnfJavaOutput(report);  
    //the available classes of the top level output instance
    instance.outputClasses = topLevelOutput.getClass().getClasses();
    //The first child item:
    ZbnfParseResultItem childItem = resultItem.nextSkipIntoComponent(null /*no parent*/);
    //loop of all first level elements, the output is written directly in topLevelOutput:
    while(childItem != null)
    { instance.writeChild( topLevelOutput
           , childItem  
           //, resultItem
           , 1
           );
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
   * @throws IllegalAccessException 
   * @throws IllegalArgumentException 
   */  
  @SuppressWarnings("deprecation")
  private void writeChild( Object parentOutputInstance
            , ZbnfParseResultItem resultItem
            , int recursion
            //, ZbnfParseResultItem parentItem
            ) throws IllegalArgumentException, IllegalAccessException
  {
    String semantic = resultItem.getSemantic();
    if(semantic.equals("time"))
      stop();
    Object childOutputInstance = null;
    report.reportln(Report.fineDebug, recursion, "ZbnfJavaOutput: " + semantic + ":");
    if(resultItem.isComponent())
    { //create a new instance with type of semantic
      char firstUpperChar = Character.toUpperCase(semantic.charAt(0)); 
      String nameOutputClass = firstUpperChar + semantic.substring(1);
      //if(Character.isLowerCase(semantic[0]))
      Class outputClass = null;
      for(int ii=0; ii < outputClasses.length; ii++)
      { String nameTest = outputClasses[ii].getName(); 
        int posDelimiter = nameTest.lastIndexOf('$');  //it is an inner class
        if(posDelimiter >=0){ nameTest = nameTest.substring(posDelimiter +1);}
        if(nameTest.equals(nameOutputClass))
        { outputClass = outputClasses[ii];
          ii = Integer.MAX_VALUE-1;  //found, break for
        }
      }
      if(outputClass != null)
      { report.report(Report.fineDebug, " found");
        try{ childOutputInstance = outputClass.newInstance();}
        catch(IllegalAccessException exception)
        { report.report(Report.fineDebug, " IllegalAccessException");
      	  childOutputInstance = null; 
        }
        catch(InstantiationException exception)
        { report.report(Report.fineDebug, " InstanciationException");
        	childOutputInstance = null; 
        }
        if(childOutputInstance != null)
        { //write in parent:
        	search_nWriteDstElement(parentOutputInstance, childOutputInstance, resultItem);
          
        	try
          { //if an field _inputColumn is found, write to it.
        	  Field elementColumn = outputClass.getField("inputColumn_");
            elementColumn.setInt(childOutputInstance, resultItem.getInputColumn());
          }
          catch(NoSuchFieldException exception){ /**do nothing if the field isn't found.*/ }
          
        	// skip into the component resultItem:
          ZbnfParseResultItem childItem = resultItem.nextSkipIntoComponent(resultItem);
          while(childItem != null)
          { 
            writeChild(childOutputInstance, childItem, recursion +1);
            childItem = childItem.next(resultItem);         
          }
        }
      }
      else
      { report.report(Report.fineDebug, ": ERROR class not found!");
      }
    }
    else
    { //write the content of the resultItem into the outputInstance:
      search_nWriteDstElement(parentOutputInstance, null, resultItem);
    }
  }
   
  
  /** Searches if inside the outputInstance exists a field or method
   * matching to the semantic of the resultItem, and writes the content in.
   * If the resultItem is a component, the childOutputInstance is filled before.
   * It contains the content of the component. The child output instance is added.
   * Otherwise, the childOutputInstance is null, and the content of resultItem
   * is written to the founded element in outputInstance.
   * <br>
   * @param outputInstance The Instance to write into, thows elements should be matched.
   * <br>
   * @param childOutputInstance If not null, it is be setted or added.
   * <br> 
   * @param resultItem The semantic is determining for matching fields, 
   *        the content is be used if childOutputInstance is null.
   * @throws IllegalAccessException if the element is not writeable. 
   * @throws IllegalArgumentException 
   */
  private void search_nWriteDstElement
  ( Object outputInstance
  , Object childOutputInstance
  , ZbnfParseResultItem resultItem
  ) throws IllegalArgumentException, IllegalAccessException
  { //add the result to an element:
    String semantic = resultItem.getSemantic();
    char firstLowerChar = Character.toLowerCase(semantic.charAt(0)); 
    String nameOutputElement = firstLowerChar + semantic.substring(1);
    Class dataClass = outputInstance.getClass();
    Class superClass = dataClass.getSuperclass();
    Object[] argMethod1 = new Object[1];
    Class[] argTypes1 = new Class[1];
    Class[] argTypes0 = new Class[0];
    Field element;
    try{ element = dataClass.getField(nameOutputElement);}
    catch(NoSuchFieldException exception)
    { try{ element = superClass.getField(nameOutputElement);}
      catch(NoSuchFieldException exc2){ element = null; } 
    }
    if(element != null)
    { //an element with the desired name is found, write the value to it:
    	report.report(Report.fineDebug, nameOutputElement);
  	  if(childOutputInstance != null)
      { writeChildElement(element, outputInstance, childOutputInstance);
      }
      else
      { writeInElement(element, outputInstance, resultItem);
      }
    }
    else
    { //not an element with the postulated name found,
      //search an appropriate method:
      Object[] argMethod = argMethod1;
      Class[] argTypes = argTypes1;
      if(resultItem.isInteger())
      { argMethod[0] = new Integer((int)resultItem.getParsedInteger());
        argTypes[0] = Integer.TYPE;
      }
      else if(resultItem.isFloat())
      { argMethod[0] = new Double((int)resultItem.getParsedFloat());
        argTypes[0] = Double.TYPE;
      }
      else if(resultItem.isOption())
      { argMethod[0] = new Integer((int)resultItem.getNrofAlternative());
        argTypes[0] = Integer.TYPE;
      }
      else
      { //main.writeError("Not an integer or float element parsed: " + semantic);
        argTypes = argTypes0;
        argMethod = null;
        //argTypes[0] = null;
      }
      Method method;
      try{ method = dataClass.getDeclaredMethod("set_" + nameOutputElement, argTypes);}
      catch(NoSuchMethodException exception)
      { try{ method = superClass.getDeclaredMethod("set_" + nameOutputElement, argTypes);}
        catch(NoSuchMethodException exc2){ method = null; }
      }
      if(method != null)
      {
        
      }
    }
    //search an integer field with name_inputColumn, if found write the input column if the parse result.
    try
    { //if an field inputColumn_ is found, write to it.
      Field elementColumn = dataClass.getField(nameOutputElement + "inputColumn_");
      elementColumn.setInt(outputInstance, resultItem.getInputColumn());
    }
    catch(NoSuchFieldException exception)
    { /**do nothing if the field isn't found.*/ 
      //not an element with the postulated name found,
      //search an appropriate method:
      argTypes1[0] = Integer.TYPE;
      Method method;
      try
      { method = dataClass.getDeclaredMethod("setinputColumn_" + nameOutputElement, argTypes1);
        argMethod1[0] = new Integer(resultItem.getInputColumn());
        method.invoke(outputInstance, argMethod1);
      }
      catch(NoSuchMethodException exception1){ /**do nothing if the field isn't found.*/ }
      catch(InvocationTargetException exc){ throw new IllegalAccessException(exc.getMessage()); }
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
    if(sType.equals("int"))
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
    
    report.report(Report.fineDebug, " \""+ debugValue + "\" written in Element Type " + sType);
  }

  
  private void writeChildElement( Field element
                             , Object outputInstance
                             , Object child
                             )
  throws IllegalAccessException
  { String sType = element.getType().getName();
    String sNameOutputClass = child.getClass().getName();
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
  
  
  void stop()
  { //debug
  }
}
