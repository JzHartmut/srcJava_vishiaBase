package org.vishia.test;

import java.util.Iterator;
import java.util.List;

public class TestGeneric
{
  class MyTest<T>{
    T ref;
  }
  
  
  MyTest<? extends Object> test;
  
  MyTest<Object> testO;
  
  MyTest<TestGeneric> testG;
  
  
  List<MyTest<? extends Object>> list;
  
  List<MyTest<Object>> listO;
  
  
  public static void main(String[] args){
    TestGeneric main = new TestGeneric();
    main.exec();
  }
  
  
  
  
  void exec(){
    //-test.ref = this;  //- this
    testO.ref = this;
    Iterator<MyTest<Object>> iter = listO.iterator();
    MyTest<Object> item = iter.next();
    Object obj = item.ref;
    System.out.println(obj);
    
    Iterator<MyTest<? extends Object>> iter2 = list.iterator();
    MyTest<? extends Object> item2 = iter2.next();
    Object obj2 = item2.ref;
    System.out.println(obj2);
    
    test = testO;
    testO = (MyTest<Object>)test;
    //- testG = testO;
    //- testG = (MyTest<TestGeneric>)testO;     //from <Object> to <Derived> not possible
    @SuppressWarnings("unchecked")
    MyTest<TestGeneric> testG = (MyTest<TestGeneric>)test;  //from <? extends Object> to <Derived>
    
    //- list = (List<MyTest<? extends Object>>)listO;
  }
}
