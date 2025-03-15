package org.vishia.util;

public class Debugutil
{
  /**This method is used either to set a breakpoint in it or to set a breakpoint on its call. It does nothing.
   * @return not used. A return statement is only contained to set the breakpoint. It is not remove by optimizing
   * because the method itself does not know anything about the ignoring of the return value on its call.
   */
  public static int stop(){
    return 0;
  }
  
  /**Possibility to break always in this operation.
   * You can change the sources between ...stop() and ...stopp() to activate break.
   * Set here always a break point.
   * @return not used. A return statement is only contained to set the breakpoint. It is not remove by optimizing
   * because the method itself does not know anything about the ignoring of the return value on its call.
   */
  public static int stopp(){
    return 0;
  }
  
  /**This method can be used to force re compilation and re-test for a routine with changing the value. 
   */
  public static int retest(int x){
    return 0;
  }
  
  /**Stop here while program writing. */
  public static int todo(){
    return 0;
  }
  
  /**Stop here while program writing. */
  public static int totest(){
    return 0;
  }
  
  /**Adequate assert(false) but possible to set a central breakpoint. Use only while program development. */
  public static void unexpected(){
    assert(false);
  }
  
  public static int stop(Object toView) {
    return 0;
  }
  
  
  /**Use this method in a users software to stop conditionally.
   * Set the invocation with the condition in comment if it is not need yet.
   * In this manner a conditional break can be programmed.
   * @param cond
   * @return
   */
  public static boolean stop(boolean cond) {
    if(cond) {
      return true;              //<<<<<<<< set a breakpoint here to break conditionally
    }
    else return false;
  }
    /**maybe called for unused references while development. *///NEW:  public static void unused(Object obj){   stop();  }
}
