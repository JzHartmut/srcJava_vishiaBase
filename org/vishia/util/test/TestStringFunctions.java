//D:/vishia/Java/srcJava_vishiaBase/org/vishia/util/test/TestStringFunctions.java
//==JZcmd==
//JZcmd java org.vishia.util.test.TestStringFunctions.test_comparePos();
//Obj test = 
//==endJZcmd==
package org.vishia.util.test;

import org.vishia.util.Assert;
import org.vishia.util.StringFunctions;

public class TestStringFunctions
{
  public static void main(String[] args){
    testCompare();
    test_indexOf();
  }
  
  
  private static void testCompare()
  {
    String s1 = "abcdxyz";
    String s2 = "dexy";
    int cmpr = StringFunctions.compare(s1, 4, s2, 2, 4);
    Assert.check(cmpr == 0);
  }
  
  
  private static void test_indexOf()
  {
    int pos;
    pos = "123abcxy".indexOf("abc", 2);
    Assert.check(pos == 3);
    
    pos = StringFunctions.indexOf("123abcxy", "abc", 2);
    Assert.check(pos == 3);
    
  }  
  
  
  public static void test_comparePos()
  {
    String s1 = "a";
    String s2 = "b";
    int cmp = StringFunctions.comparePos(s1, 0, s2, 0, Integer.MAX_VALUE);
    if(cmp !=-1) error("TestStringFunctions.comparePos e1");
    //
    s1 = "abcx";
    s2 = "abcy";
    cmp = StringFunctions.comparePos(s1, 0, s2, 0, Integer.MAX_VALUE);
    if(cmp !=-4) error("TestStringFunctions.comparePos abc ? abcx ");
    //
    s1 = "abc";
    s2 = "abcy";
    cmp = StringFunctions.comparePos(s1, 0, s2, 0, Integer.MAX_VALUE);
    if(cmp !=-4) error("TestStringFunctions.comparePos abc ? abcx ");
    //
    s1 = "abcx";
    s2 = "abc";
    cmp = StringFunctions.comparePos(s1, 0, s2, 0, Integer.MAX_VALUE);
    if(cmp != 4) error("TestStringFunctions.comparePos abc ? abcx ");
  }

  
  static void error(String s){
    System.out.println(s);
  }
  
  
}
