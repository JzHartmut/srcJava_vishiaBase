package org.vishia.util;

import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;

public class CalculatorExpr
{
  
  /**Version and History:
   * <ul>
   * <li>TODO unary operator, function 
   * <li>2011-10-15 Hartmut creation. The ideas were created in 1979..80 by me.  
   * </ul>
   */
  public final int version = 0x20111015;
  
  
  
  private static final class Operation
  {
    /**The operation Symbol if it is a primitive. */
    final char operation;
    final int ixVariable;
    final double value;
    
    Operation(char operation, double value){ this.value = value; this.operation = operation; this.ixVariable = -1; }
    Operation(char operation, int ixVariable){ this.value = 0; this.operation = operation; this.ixVariable = ixVariable; }
  }
  
  private final List<Operation> stackExpr = new LinkedList<Operation>();
  
  private String[] variables;
  
  /**Converts the given expression in a stack operable form.
   * @param sExpr
   */
  public String setExpr(String sExpr, String[] sIdentifier)
  {
    this.variables = sIdentifier;
    StringPart sp = new StringPart(sExpr);
    return multExpr(sp, '!', 1);
  }
  
  public String setExpr(String sExpr)
  {
    this.variables = new String[]{"X"};
    StringPart sp = new StringPart(sExpr);
    return addExpr(sp, '!',1);
  }
  
  /**The outer expression is a add or subtract expression.
   * call recursively for any number of operands.
   * call {@link #multExpr(StringPart, char)} to get the argument values.
   * @param sp
   * @param operation The first operation.
   * @return this
   */
  private String addExpr(StringPart sp, char operation, int recursion)
  { String sError = null;
    if(recursion > 1000) throw new RuntimeException("recursion");
    sError = multExpr(sp, operation, recursion +1);
    if(sError == null && sp.length()>0){
      char cc = sp.getCurrentChar();
      if("+-".indexOf(cc)>=0){
        sp.seek(1).scanOk();
        return addExpr(sp, cc, recursion+1);
      }
    }
    return null;
  }
  
  
  /**The more inner expression is a mult or divide expression.
   * call recursively for any number of operands.
   * call functions to get the argument values. 
   * @param sp
   * @param operation
   * @return
   */
  private String multExpr(StringPart sp, char operation, int recursion)
  { if(recursion > 1000) throw new RuntimeException("recursion");
    try{
      if(sp.scanIdentifier().scanOk()){
        String sIdent = sp.getLastScannedString();
        int ix;
        for(ix = 0; ix< variables.length; ++ix){
          if(variables[ix].equals(sIdent)){
            stackExpr.add(new Operation(operation, ix));
            ix = Integer.MAX_VALUE-1; //break;
          }
        }
        if(ix != Integer.MAX_VALUE){ //variable not found
          return("unknown variable" + sIdent);
        }
      } else if(sp.scanFloatNumber().scanOk()){
        stackExpr.add(new Operation(operation, sp.getLastScannedFloatNumber()));
      }
    }catch(ParseException exc){
      return("ParseException float number"); 
    }
    if(sp.length()>0){
      char cc = sp.getCurrentChar();
      if("*/".indexOf(cc)>=0){
        sp.seek(1).scanOk();
        return multExpr(sp, cc, recursion+1);
      }
    }
    return null;  //ok
  }
  
  
  /**Calculate with more as one input value.
   * @param input
   * @return
   */
  public double calc(double[] input)
  { double val = 0;
    return val;
  }
  

  
  /**Calculates the expression with only one input.
   * @param input The only one input value (used for all variables, simple version).
   * @return The result.
   */
  public double calc(double input)
  { double val = 0;
    for(Operation oper: stackExpr){
      final double val2;
      if(oper.ixVariable >=0){ val2 = input; }
      else { val2 = oper.value; }
      switch(oper.operation){
        case '!': val = val2; break;
        case '+': val += val2; break;
        case '-': val -= val2; break;
        case '*': val *= val2; break;
        case '/': val /= val2; break;
      }
    }
    return val;
  }
  
}
