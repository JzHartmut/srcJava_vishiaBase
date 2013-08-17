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


  private Operation actOperation = new Operation("!");
  
  
  
  public ZbatchZbnfExpression new_orValue(){
    return this;
  }
  
  
  public void add_orValue(ZbatchZbnfExpression val){ }
  
  
  
  public ZbatchZbnfExpression new_andValue(){
    return this;
  }
  
  
  public void add_andValue(ZbatchZbnfExpression val){ }
  
  
  public void set_not(String val){
    
  }
  

  public ZbatchZbnfExpression new_boolExpr(){ 
    return this;
  }

  
  public void add_boolExpr(ZbatchZbnfExpression  val){ }

  
  public ZbatchZbnfExpression new_objExpr(){ 
    return this;
  }

  public ZbatchZbnfExpression new_cmpOperation(){
    if(actOperation !=null){
      
    }
    actOperation = new Operation("c");
    return this;
  }
  
  public void add_cmpOperation(ZbatchZbnfExpression val){
    addToStack(actOperation); 
  }

  
  public void set_cmpOperator(String val){
    actOperation.setOperator(val);
  }
  
  public void add_objExpr(ZbatchZbnfExpression  val){ }

  
  public ZbatchZbnfExpression xxxnew_numExpression(){ 
    return this;
  }

  
  public void add_numExpression(ZbatchZbnfExpression  val){ }

  
  
  public ZbatchZbnfExpression new_expression(){
    return this;
  }
  
  public void add_expression(ZbatchZbnfExpression val){
     
  }

  public ZbatchZbnfExpression new_startOperation(){
    if(actOperation !=null){
      
    }
    actOperation = new Operation("!");
    return this;
  }
  
  public void add_startOperation(ZbatchZbnfExpression val){
    addToStack(actOperation); 
  }


  public ZbatchZbnfExpression new_addOperation(){
    if(actOperation !=null){
      
    }
    actOperation = new Operation("+");
    return this;
  }
  
  public void add_addOperation(ZbatchZbnfExpression val){
    addToStack(actOperation); 
  }

  public ZbatchZbnfExpression new_multOperation(){
    if(actOperation !=null){
      
    }
    actOperation = new Operation("*");
    return this;
  }
  
  public void add_multOperation(ZbatchZbnfExpression val){
    addToStack(actOperation); 
  }


  public void set_intValue(int val){
    actOperation.set_intValue(val);
  }
  
  
  
  public void set_text(String val){
    actOperation.set_textValue(val);
  }
  
  
  
  
  
  public void set_startVariable(String ident){
    DataAccess.DatapathElement element = new DataAccess.DatapathElement();
    element.whatisit = 'v';
    element.ident = ident;
    actOperation.add_datapathElement(element);
  }
  
  public ZbnfDataPathElement new_datapathElement(){ return new ZbnfDataPathElement(null); }
  
  public void add_datapathElement(ZbnfDataPathElement val){ 
    actOperation.add_datapathElement(val); 
  }
  

  
  
  @Override
  public void addToStack(Operation operation){
    stackExpr.add(operation);
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
