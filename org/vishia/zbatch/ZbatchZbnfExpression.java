package org.vishia.zbatch;

import java.util.ArrayList;
import java.util.List;

import org.vishia.util.CalculatorExpr;
import org.vishia.util.DataAccess;

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


  private Operation actOperation; // = new Operation("!");
  
  
  
  public ZbatchZbnfExpression new_orValue(){
    return this;
  }
  
  
  public void add_orValue(ZbatchZbnfExpression val){ }
  
  
  
  public ZbatchZbnfExpression new_andValue(){
    return this;
  }
  
  
  public void add_andValue(ZbatchZbnfExpression val){ }
  
  
  public void set_not(String val){
    if(actOperation ==null){
      actOperation = new Operation();
    }
    actOperation.setUnaryOperator("~");
  }
  

  public ZbatchZbnfExpression new_boolExpr(){ 
    return this;
  }

  
  public void add_boolExpr(ZbatchZbnfExpression  val){ }

  
  public ZbatchZbnfExpression new_objExpr(){ 
    return this;
  }

  public void add_objExpr(ZbatchZbnfExpression  val){ 
    if(actOperation ==null){
      actOperation = new Operation();
      actOperation.setStackOperand();  
    }
    addToOperations(); 
  }

  public ZbatchZbnfExpression new_cmpOperation(){
    return this;
  }
  
  public void add_cmpOperation(ZbatchZbnfExpression val){
    addToOperations(); 
  }

  
  public void set_cmpOperator(String val){
    if(actOperation ==null){
      actOperation = new Operation("c");
    }
    actOperation.setOperator(val);
  }
  
  
  public ZbatchZbnfExpression xxxnew_numExpression(){ 
    return this;
  }

  
  public void xxxadd_numExpression(ZbatchZbnfExpression  val){ 
    
  }

  
  
  /**Designates, that a expression in parenthesis is given, which should be calculated firstly.
   * @return this
   */
  public ZbatchZbnfExpression new_expression(){
    if(actOperation !=null){ //A start operation before
      addToOperations();
    }
    return this;
  }
  
  /**Designates the end of an expression in any syntax tree.
   * @param val unused, it is this.
   */
  public void add_expression(ZbatchZbnfExpression val){
    //assert(actOperation == null);  //it was added before on end of expression.   
    if(actOperation !=null){
      addToOperations();  //if it is a start operation.
    }
    //actOperation = new Operation();
    //actOperation.setStackOperand();  //NOTE the operation will be set by following set_multOperation() etc.
  }

  public ZbatchZbnfExpression XXXnew_startOperation(){
    return this;
  }
  
  public void XXXadd_startOperation(ZbatchZbnfExpression val){
    addToOperations(); 
  }

  /**Designates the start of a new adding operation. The first start value should be taken into the
   * stackOperation statement list as start operation.
   * @return this
   */
  public ZbatchZbnfExpression new_addOperation(){
    if(actOperation !=null){ addToOperations(); }
    assert(actOperation == null);  //will be set by values. operator will be set by add_addOperation
    return this;  
  }
  
  /**Designates the end of an add operation. Takes the operation into the expression list.
   * @param val this, unused
   */
  public void add_addOperation(ZbatchZbnfExpression val){
    if(actOperation ==null){
      actOperation = new Operation();
      actOperation.setStackOperand();  
    }
    actOperation.setOperator("+");
    addToOperations(); 
  }

  public ZbatchZbnfExpression new_multOperation(){
    if(actOperation !=null){ addToOperations(); }
    assert(actOperation == null);  //will be set by values. operator will be set by add_addOperation
    return this;
  }
  
  public void add_multOperation(ZbatchZbnfExpression val){
    if(actOperation ==null){
      actOperation = new Operation();
      actOperation.setStackOperand();  
    }
    actOperation.setOperator("*");
    addToOperations(); 
  }


  /**Sets a value to the current operation. 
   * @param val
   */
  public void set_intValue(int val){
    if(actOperation == null){ actOperation = new Operation(); }
    actOperation.set_intValue(val);
  }
  
  
  
  public void set_text(String val){
    if(actOperation == null){ actOperation = new Operation(); }
    actOperation.set_textValue(val);
  }
  
  
  
  
  
  public void set_startVariable(String ident){
    if(actOperation == null){ actOperation = new Operation(); }
    DataAccess.DatapathElement element = new DataAccess.DatapathElement();
    element.whatisit = 'v';
    element.ident = ident;
    actOperation.add_datapathElement(element);
  }
  
  public ZbatchZbnfExpression new_datapath(){ return this; }
  
  public void add_datapath(ZbatchZbnfExpression val){ 
    //actOperation.add_datapathElement(val); 
  }
  

  public ZbnfDataPathElement new_datapathElement(){ return new ZbnfDataPathElement(null); }
  
  public void add_datapathElement(ZbnfDataPathElement val){ 
    if(actOperation == null){ actOperation = new Operation(); }
    actOperation.add_datapathElement(val); 
  }
  

  /**This routine must be called at least. It adds a simple value to the operation list.
   * If any second value was added already, the routine does nothing.
   */
  public void closeExprPreparation(){
    if(actOperation !=null){
      addToOperations();
    }
  }
  
  private void addToOperations(){
    if(!actOperation.hasOperator()){
      actOperation.setOperator("!");  //it is initial value
    }
    super.addOperation(actOperation);
    actOperation = null;  //a new one is necessary.
  }


  
  
  public static class ZbnfDataPathElement extends CalculatorExpr.DataPathItem
  {
    final CalculatorExpr.Datapath parentStatement;
    
    //protected List<CalculatorExpr.Datapath> paramArgument;

    
    
    public ZbnfDataPathElement(CalculatorExpr.Datapath statement){
      this.parentStatement = statement;
    }
    
    
    
    public ZbatchZbnfExpression new_argument(){
      ZbatchZbnfExpression actualArgument = new ZbatchZbnfExpression();
      return actualArgument;
    }

    
    /**From Zbnf.
     * The Arguments of type {@link Statement} have to be resolved by evaluating its value in the data context. 
     * The value is stored in {@link DataAccess.DatapathElement#addActualArgument(Object)}.
     * See {@link #add_datapathElement(org.vishia.util.DataAccess.DatapathElement)}.
     * @param val The Scriptelement which describes how to get the value.
     */
    public void add_argument(ZbatchZbnfExpression val){ 
      if(paramExpr == null){ paramExpr = new ArrayList<CalculatorExpr>(); }
      paramExpr.add(val);
    } 
    
    public void set_javapath(String text){ this.ident = text; }
    


  }
  

  
}
