package org.vishia.test;

import java.util.LinkedList;
import java.util.List;

/**This is a short example to show what is and how to use a Lambda expression.
 * @author Hartmut Schorrig, License LPGL
 *
 */
public class LambdaTest1
{

  static class MyData { 
    int x1, x2;
    MyData(int x1, int x2){ this.x1 = x1; this.x2 = x2; }
  }
  
  /**A functional interface is necessary for a lambda expression. 
   * It represents the type of expression. 
   * It does not define how to do, only the basic requirements. */
  @FunctionalInterface interface Dosomething {
    MyData doit(int val, MyData arg);
  }
  
  
  MyData d1,d2, d3, d4;
  
  
  public static void main(String[] args) {
    LambdaTest1 main = new LambdaTest1();
    main.testOperation();
  }
  
  
  void testOperation() {
    
    MyData userArg = new MyData(2,3);
    
    /**Old style definition of a functionality with an implicit implementation 
     * of an interface operation: */
    Dosomething variant1 = new Dosomething() {
      @Override public MyData doit(int val, MyData arg) {
        MyData ret = new MyData(arg.x1 * val, arg.x2); return ret; 
    }};
    
    /**It is the same, but shorter written. That is a lambda expression. */
    Dosomething variant2 = (val,arg) -> { MyData ret = new MyData(arg.x1 * val, arg.x2); arg.x2 = 0; return ret; };
    
    /**For only one statement the { return } can be omitted */
    Dosomething variant3 = (val,arg) -> new MyData(arg.x1 * val, arg.x2 = 0);
    
  
    d1 = routine(variant1, 3, userArg);
    d2 = routine(variant2, 2, userArg);
    d3 = routine(variant3, 12, userArg);
    
    /**A lambda expression can be immediately written as argument. The argument contains 'how to do'. */
    d4 = routine((val,arg) -> new MyData(arg.x1 * val, arg.x2), 23, userArg);
    
    
    List<MyData> list = new LinkedList<MyData>();
    for(int x = 2; x < 6; ++x) {              //build a container as example
      list.add(new MyData(x, 2*x+3));
    }
    
    list.stream().forEach(d -> d.x2 = 333);   //work with container data
    
    long n = list.stream().count();
    
    for(MyData arg: list) {
      MyData result = routine(variant2, 2, arg);
    }
    
  }
  
  
  
  
  
  
  
  MyData routine(Dosomething dosomething, int val, MyData arg) {
    return dosomething.doit(val,arg);
  }
  
  
  
}
