package org.vishia.util;

/**This is a only-documentation class for simple UML notation.
 * <br><br>
 * In Javadoc there may be nice to have an UML presentation of coherence between classes.
 * An UML graphic created with an UML-Tool, exported as graphic file and linked in the javadoc
 * may be a good style. But the presentation of the UML diagramm is not able to seen in the 
 * source code, only seen in an browser viewing javadoc.
 * <br><br>
 * On the other hand the coherence are simple to show usual. A complex UML diagram is not need.
 * A simple representation is sufficient.
 * <br><br>
 * Here a simple notation is described, which can be used.
 * <br><br>
 * <b>A class</b>
 * <pre>
 *          |
 *          |
 *     !Some application notes.    
 *     NameOfClass
 *     !Some application notes.    
 *     -interest_method()
 *     +overwritten_method()
 *     -interest_field
 *          |
 *          |
 * </pre>         
 * The class is presented only with a vertical line. A box written with 
 * <pre>
 *    +-----------------------+
 *    |       Name            |
 *    +-----------------------+
 * </pre>
 * is too much effort to write in the code. It may be more nice, but the beneficing is lesser then the effort.
 * Attributes or methods should only shown if there are essential for that presentations. It may be essential
 * that some methods are overwritten.
 * <br><br>
 * <b>Relations</b>
 * <pre>
 *          |      +------> ToAnotherClass
 *          |      |                                 +---------------<>|
 *          |------+                                 |                 |
 *          |                                        |          AnotherClass
 *     NameOfClass <---------------------------------+                 |
 *          |
 *          |<---------- FromAnother
 *          |
 *  ------->|
 *          |<&----------------------<&>InnerNonstaticClass
 *          |                                  |
 *          |&--&InnerStaticClass
 *    
 * </pre>   
 * All arrows can be used only in horizontal direction. There is not an ASCII character to show
 * vertical arrows. Generally only standard ASCII-character should be used!
 * 
 * <br><br>
 * <b>Type of associations:</b>
 * <ul>
 * <li><code>----------></code> A simple association (may be a null-reference, reference can be changed)
 * <li><code>-reference-----></code> The name of the reference in the own class can be named in the arrow or not.
 *   It's question of space and explicitness.
 * <li><code>---------|></code> An abstraction (inheritance). In java it referes the super class.
 *   It may be used too to refer interfaces.
 * <li><code><>--------></code> An aggregation in UML. In Java it is a final reference which is set
 *   by a parameter given in the constructor.
 * <li><code><&>------&></code> Aggregation to its environment class. 
 *   An environment class is a known construct in Java (non-static inner class has the environment class). 
 *   In UML it is a Composition.
 * <li><code><*>-------></code> An composition in UML (filled diamond). In Java it is a final reference
 *   which is set by a new in the constructor or class body.
 * <li><code>---------*></code> An association (or aggregation or composition) to any number of referenced
 *   instances. In Java it is a container like List etc. The references type is the container element type
 *   like <code>List < ReferType > </code>.
 * <li><code>---------2></code> An association (or aggregation or composition) to a given number of referenced
 *   instances. In Java it is a final array with a constant initialized number of elements.
 *   For example <code>Refertype [] array = new Refertype[2] </code>.
 * </ul>
 * <br><br><br>
 * <b>Control flow</b>
 * In UML a control flow or event transmission is shown in an sequence diagram usually.
 * But a control flow - calling of some methods of another class inside a method - may be essential to present in a graphic.
 * The following notation can be used:
 * <pre>
 *     MyClass
 *       *methodOfClass()
 *         *calledMethod()
 *           *reference.calledMethod()
 *                 *reference.calledMethod()
 *                      |<*>-reference------>CompositeClass.calledMethod()
 * </pre>
 * A called method is shown by an asterisk. The sequence of calling some methods inside may be a point of interest.
 * Don't show too many details, maybe conditions etc. Only the fact of calling something may be essential.
 * Either the name of the reference or the type or both can be shown.
 * <br><br>
 * <b>Usage of links</b>:
 * <br>
 * If there is enough space to write a {@link Docu_UML_simpleNotation}-link to the type, do it. It helps in navigation.
 *  
 * @author Hartmut Schorrig
 * */
public class Docu_UML_simpleNotation {

  public static int version = 20120707;
}
