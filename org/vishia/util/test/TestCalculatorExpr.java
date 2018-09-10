//D:/vishia/Java/srcJava_vishiaBase/org/vishia/util/test/TestCalculatorExpr.java
//==JZcmd==
//JZcmd java org.vishia.util.test.TestCalculatorExpr.main();
//##JZcmd Obj a = java org.vishia.util.test.TestCalculatorExpr.testSimpleExpression();
//==endJZcmd==
package org.vishia.util.test;

import org.vishia.util.Assert;
import org.vishia.util.CalculatorExpr;

public class TestCalculatorExpr
{
  public static void main(String[] args){ main(); }

  
  public static void main(){
    testSimpleExpression();
    testParenthesis();
  }

  
  /**Tests the {@link CalculatorExpr#setExpr(String)} and {@link CalculatorExpr#calc(float)}.
   * 
   */
  public static void testSimpleExpression()
  {
    String sError;
    CalculatorExpr calc = new CalculatorExpr();
    sError = calc.setExpr("5.25 * X");
    Assert.checkMsg(sError == null, sError);
    //NOTE: to exactly compare float values the fractional part should be a division by 2 only.  
    float result = calc.calc(3.0f);
    Assert.checkMsg(result == 15.75f, "testSimpleExpression fails");
    result = calc.calc(3.5f);
    Assert.checkMsg(result == 18.375f, "testSimpleExpression fails");
    //
    sError = calc.setExpr("3.0F * (X+5 + X)");
    Assert.checkMsg(sError == null, sError);
    result = calc.calc(2.5f);
    Assert.checkMsg(result == 30.0f, "testSimpleExpression fails");
  }
  
  
  public static void testParenthesis(){
    String sError;
    CalculatorExpr calc = new CalculatorExpr();
    sError = calc.setExpr("3 * (5 + X)");
    Assert.checkMsg(sError == null, sError);
    //NOTE: to exactly compare float values the fractional part should be a division by 2 only.  
    float result = calc.calc(3.0f);
    Assert.checkMsg(result == 24.0f, "testSimpleExpression fails");
      
  } 
  
  
  
  public static void testParenthesisSpecial(){
    float y = 5+ ~- -(3*4);
    boolean b = !(13>12);
    Assert.check(y== -8);
  }

  
  
  
}
