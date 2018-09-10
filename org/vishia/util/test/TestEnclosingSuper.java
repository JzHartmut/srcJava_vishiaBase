package org.vishia.util.test;

public class TestEnclosingSuper
{
  static class InnerA{
    String test = "InnerA";
    class InnerB{
      
    }
  }
  
  String test = "Outer";
  
  
  
  class InnerX extends SuperM{
    String test = "InnerX";
    class InnerY extends SuperL{
      void test(){
        System.out.println(test);
      }
    }
  }
  
  
  static class SuperM{
    String test = "SuperM";
  }
  
  static class SuperL extends InnerA{
    //String test = "SuperL";
    
  }
  
  
  public static void main(String[] args){
    TestEnclosingSuper data = new TestEnclosingSuper();
    InnerX innerX = data.new InnerX();
    InnerX.InnerY innerY = innerX.new InnerY();
    innerY.test();
  }
  
  
}
