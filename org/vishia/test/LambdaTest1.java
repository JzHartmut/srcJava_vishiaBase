package org.vishia.test;

/**This is a short example to show what is and how to use a Lambda expression.
 * @author Hartmut Schorrig, License LPGL
 *
 */
public class LambdaTest1
{

  static class MyData { 
    int x1, x2;
    MyData(int init){ x1 = init; }
  }
  
  /**A functional interface is necessary for a lambda expression. It represents the type of expression. 
   * It does not define how to do, only the basic requirements. */
  @FunctionalInterface interface Dosomething {
    MyData doit(float val);
  }
  
  
  MyData d1,d2, d3, d4;
  
  
  void main(String[] args) {
    LambdaTest1 main = new LambdaTest1();
    main.testOperation();
  }
  
  
  void testOperation() {
    
    /**Old style definition of a functionality with an implicit implementation of a interface operation: */
    Dosomething variant1 = new Dosomething() {
      @Override public MyData doit(float val) {
        MyData ret = new MyData((int)val); return ret; 
    }};
    
    /**It is the same, but shorter written. That is a lambda expression. */
    Dosomething variant2 = val -> { MyData ret = new MyData((int)val); return ret; };
    
    /**For only one statement the { return } can be omitted */
    Dosomething variant3 = val -> new MyData((int)val);
    
  
    d1 = routine(variant1, 3.5f);
    d2 = routine(variant2, 2.5f);
    d3 = routine(variant3, 12.5f);
    
    /**A lambda expression can be immediately written as argument. The argument contains 'how to do'. */
    d4 = routine(val -> new MyData((int)val), 23.5f);
    
  }
  
  
  
  
  MyData routine(Dosomething dosomething, float val) {
    return dosomething.doit(val);
  }
  
  
  
}
