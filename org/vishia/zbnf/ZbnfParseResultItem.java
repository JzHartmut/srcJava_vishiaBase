/****************************************************************************/
/* Copyright/Copyleft: 
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
 * @author www.vishia.de/Java
 * @version 2006-06-15  (year-month-day)
 * list of changes: 
 * 2006-05-00: www.vishia.de creation
 *
 ****************************************************************************/
package org.vishia.zbnf;

import java.util.Iterator;

import org.vishia.util.SortedTree;

/** This interface is used to query the results of parsing. Every result item
 * can be query at the readed values from input. The next item can getted, so 
 * the user can stepped through all items. Especially the item after the end
 * of a metamorphem item can getted directly, if the parsed result of a metamorphem
 * is not interested.<br/>
 * The evaluation of the parsing result may be done with the followed pattern:
 * <pre>
 *  ParseResultItem item = parser.getFirstParseResult();  //after successfull parsing.
 *  while(item != null)
 *  { String semantic = item.getSemantic();
 *    if(semantic.equals("myExpectedSemantic"))
 *    { //do something
 *    }
 *    else if(semantic.equals("myOtherExpectedSemantic"))  
 *    { //do something:
 *      getOtherExpectedComponentResult();
 *    }
 *    item = item.next(null);  //get the next.
 *  }
 * </pre>
 * The <code>doSomehing</code> means, calling of getParsedText(), getParsedInteger()
 * and so on from this item and write the values according the users requests. In the
 * example above a mehtod <code>getOtherExpectedComponentResult()</code> is be written.
 * It is a example to get results from parsing of a metamorphem, it is typically. 
 * In this case the followed pattern may be used:
 * <pre>
 *  void getOtherExpectedComponentResult(ParseResultItem parent)
 *  { //do something with info in parent
 *    // ...
 *    ParseResultItem item = parent.nextSkipIntoComponent(parent);  
 *    //start with the second item of the component 'parent'.
 *    //It may be null if the parent is only composed of one item. 
 *    while(item != null)
 *    { String semantic = item.getSemantic();
 *      if(semantic.equals("mySecondSemantic"))
 *      { //do something
 *      }
 *      else if(semantic.equals("myThirdExpectedSemantic"))  
 *      { //do something
 *        getUsersInnerComponentResult();
 *      }
 *      item = item.next(parent);  //get the next, null on end of the component 'parent'
 *    }
 *  }    
 * </pre>
 * The inner structure is the same,  if a component with the expected semantic is detect,
 * the component is examined  by calling nextSkipIntoComponent(parent) at first, 
 * but, to step throw its components next(parent) is calling inside the while-loop
 * to skip over inner components.
 * 
 * Note: The documentation above uses the older deprecated methods, using the methods from interface 
 * are more simple.
 *
 */

public interface ZbnfParseResultItem extends SortedTree<ZbnfParseResultItem>
{
  /** Gets the semantic of the item. If no semantic is given,
   * this method returns a string beginning with "-" such as "--NoSemantic--".
   * This string is manual readable.
   * The users evaluation will be oriented on the semantic of the items. 
   * 
   */
  public String getSemantic();

  /** Returns the input text at the parsed area of this result. At example
   * a float is parsed, this method returns the string represents the float number.
   * 
   * @return null if the mode of not stored input is used.
   */
  public String getParsedText();

  /** Returns the parsed float value if isFloat() returns true.
   * Note: "float" doesn't mean the float type in java, 
   * it means a float value in mathematical wise. Thats why the return value
   * is double. The user may cast it to float, if it knows, that the value is
   * inside the float range.
   * @return The parsed float value or 0.0 if isFloat() returns false.
   * */
  public double getParsedFloat();

  /** Returns the parsed integer value if isInteger() returns true.
   * Note: "integer" doesn't mean the int type in java, 
   * it means a integer value in mathematical wise. Thats why the return value
   * is long. The user may cast it to int or short or byte, if it knows, that the value is
   * inside the expected range.
   * @return The parsed integer value or 0 if isInteger() returns false.
   * */
  public long getParsedInteger();

  /** Returns the parsed string if isString() returns true.
   * @return The parsed string or null if isString() returns false.
   * */
  public String getParsedString();

  
  /** Returns the number of the alternative, -1 if there is not a alternative choice,
   * 0 if there is an empty option.
   * @return Number of alternative 1.. if a alternative is used actively.
   */
  public int getNrofAlternative();
  
  /** Returns a short description for report, error report, from the current item.
   * The form is &lt;value?semantic&gt>.
   * @return A short description.
   */
  public String getDescription();
  
  
  /**returns the column of the input line while parsing. */
  public int getInputColumn();
  
  /** Tests if the actual item is a start item of a component. A component
   * is a complex part of the parsed input file, represent by the appropriated items
   * after this item here, determined by a non-terminal symbol of syntax.
   * @return true if it is a component, false if it is a single morphem,
   * once of the other is___().
   */
  public boolean isComponent();

  /** Tests if the actual item represents a parsed integer number. 
   * @return true if it is a integer number, false if it is once of the others types querried with is___().
   * */
  public boolean isInteger();

  /** Tests if the actual item represents a parsed float number. 
   * @return true if it is a integer number, false if it is once of the others types querried with is___().
   * */
  public boolean isFloat();

  /** Tests if the actual item represents a parsed string. 
   * It is possible to generate such an entry if the syntax SBNF-file contains &lt;EXPR?semantic>,
   * where EXPR is a regular expression, quotion string, String to an end char or such.
   * Use getString() to get the string value of this item.  
   * @return true if it is a string, false if it is once of the others types querried with is___().
   * */
  public boolean isString();

  /** Tests if the actual item represents a parsed terminal symbol. 
   * @return true if it is terminal symbol, false if it is once of the others types querried with is___().
   * */
  public boolean isTerminalSymbol();


  /** Tests if the actual item represents an identifier. 
   * An identifier is stored in result only if &lt;$ ...&gt; is given in the syntax prescript.
   * @return true if it is a identifier, false if it is once of the others types querried with is___().
   * */
  public boolean isIdentifier();

  /** Tests if the actual item represents a entry describes only semantic.
   * It is possible to generate such an entry if the syntax SBNF-file contains &lt;?semantic>.  
   * @return true if it is a only-semantic-describing entry, false if it is once of the others types querried with is___().
   * */
  public boolean isOnlySemantic();


  /** Tests if the actual item represents an entry describes a repetition of something.
   * It is possible to generate such an entry if the syntax SBNF-file contains {&lt;?semantic&gt; ... }.  
   * @return >0 if it is a repetition, it is the count of repetition; < 0 if it isn't..
   * */
  public int isRepetition();

  /** Tests if the actual item represents an entry describes a repetition of something.
   * It is possible to generate such an entry if the syntax SBNF-file contains { ... ?&lt;?semantic&gt; ... }.  
   * @return >0 if it is a repeat (backward) part of repetition, it is the count of repetition; < 0 if it isn't..
   * */
  public int isRepeat();

  /** Tests if the actual item represents an entry describes a option construct of something.
   * It is possible to generate such an entry if the syntax SBNF-file contains [&lt;?semantic&gt;...| ...|].  
   * @return true if it is a alternative, it is the count of alternative; < 0 if it isn't..
   * */
  public boolean isOption();

  
  

  /**Gets the next item or component followed by the current item or component in the current tree level.
   * use it if it is known that at least one followed item should exist. 
   * @return The next item or null on end of all items. If next() is called but the curren tree level is ended,
   *         the next item of the level above is returned. It is not tested here.
   * @deprecated use instead {@link iteratorChildren()} or {@link getChildren()} from the parent item.
   */
  ZbnfParseResultItem next();

  /** Gets the next item or component followed by the current item in the current tree level.
   * inside the component 'parent' or inside the whole parsers store.
   * This method is useful if the overview over results, test some components,
   * is interested.
   * 
   * @param parent The item that is the first item of a component. 
   *               If null, than no test of parent occurs.
   * @return The next item or null on end of parent or end of all.
   * @deprecated use instead {@link iteratorChildren()} or {@link getChildren()} from the parent item.
   */
  ZbnfParseResultItem next(/*Parser.ParserStore store, */ZbnfParseResultItem parent);

  /** Gets the next item immediate after this item 
   * inside the component 'parent' or inside the whole parsers store.
   * If this is the components first item, it supplies the components second item.
   * After that, next(componentesFirstItem) steps inside the component and supplies
   * null on end of the component.<br>
   * This method is useful especially if the content of the component
   * is interested. 
   * 
   * @param parent The item that is the first item of a component. 
   *               If null, than no test of parent occurs.
   * @return The next item after a meta morphem or null on end.
   * @deprecated use instead {@link iteratorChildren()} or {@link getChildren()} from the current item.
   */
  ZbnfParseResultItem nextSkipIntoComponent(/*Parser.ParserStore store, */ZbnfParseResultItem parent);

  
  /** Gets the first child of a component.
   * Use it if it is known that at least one child should exist.
   * @return first child or null if it is not a component with children.
   * @deprecated use instead {@link listChildren()} from the current item.
   */
  ZbnfParseResultItem firstChild();
      
  
  /**Creates an iterator and returns it reference for the actual item to iterate to its childs.
   * 
   */
  Iterator<ZbnfParseResultItem> iteratorChildren();
  
}
