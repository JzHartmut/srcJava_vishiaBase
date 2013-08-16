package org.vishia.zbatch;

import org.vishia.util.CalculatorExpr;

/**This class provides a Zbnf interface to the CalculatorExpr. It contains only methods
 * to store Zbnf parse results for the expression.
 * @author Hartmut Schorrig
 *
 */
public class ZbatchZbnfExpression extends CalculatorExpr
{
  /**Version, history and license.
   * <ul>
   * <li>2013-08-16 Hartmut created
   * </ul>
   * 
   * <b>Copyright/Copyleft</b>:
   * For this source the LGPL Lesser General Public License, published by the Free Software Foundation is valid.
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
  @SuppressWarnings("hiding")
  static final public int version = 20131003;


  public ZbatchZbnfExpression new_orCondition(){
    return this;
  }
  
  
  public void add_orCondition(ZbatchZbnfExpression val){ }
  
  
  
  public ZbatchZbnfExpression new_andCondition(){
    return this;
  }
  
  
  public void add_andCondition(ZbatchZbnfExpression val){ }
  
  
  public void set_not(String val){
    
  }
  

  public ZbatchZbnfExpression XXXnew_boolExpr(){ 
    return this;
  }

  
  public void add_boolExpr(ZbatchZbnfExpression  val){ }

  
  public ZbatchZbnfExpression new_objExpr(){ 
    return this;
  }

  
  public void add_objExpr(ZbatchZbnfExpression  val){ }

  
  public ZbatchZbnfExpression xxxnew_numExpression(){ 
    return this;
  }

  
  public void add_numExpression(ZbatchZbnfExpression  val){ }

  
  
  public ZbatchGenScript.ZbnfOperation new_startOperation(){
    return new ZbatchGenScript.ZbnfOperation("!", null);
    //return this;
  }
  
  public void add_startOperation(ZbatchGenScript.ZbnfOperation val){
    addToStack(val); 
  }


  @Override
  public void addToStack(Operation operation){
    stackExpr.add(operation);
  }


}
