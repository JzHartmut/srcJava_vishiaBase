package org.vishia.test;

import java.util.LinkedList;
import java.util.List;

public class TestStream
{
  List<String> list1 = new LinkedList<String>();
  
  void test() {
    list1.add("xx");   //stream();
    list1.stream();
  }
}
