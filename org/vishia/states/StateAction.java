package org.vishia.states;

import java.util.EventObject;

//import org.vishia.event.EventMsg2;

/**This interface is able to use to define a action method for entry, exit or a transition in a Statemachine, which is implemented inside any user class.
 * Use the following pattern: <pre>
 * class MyUserClass {
 *   Some things;
 *   
 *   StateAction actionForSomewhat = new StateAction() {
 *     QOverride public int exec(Event<?,?> ev) { 
 *       things.dowithThem(); 
 *   };
 * }
 * ....
 * class AnotherClass {
 *   MyUserClass associateUserClass;
 *   
 *   class StateMachine extends StateTop 
 *   {
 *     class StateXY extends StateSimple {
 *       { entry = associateUserClass.actionForSomewhat;
 *       }
 *     }
 *   
 *   
 *   }  
 * </pre>
 * Any class defines the action which uses the context of that class, it is a non-static inner class. 
 * Another class may contain the state machine which uses the action. It associates it via its {@link StateSimple#entry} field.
 * @author Hartmut Schorrig LPGL License.
 *
 */
public interface StateAction
{
  int exec(EventObject event);
}
