package org.vishia.util.test;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import org.vishia.util.StringFormatter;
import org.vishia.util.StringPreparer;

public class Test_StringPreparer
{

  StringPreparer fm = new StringPreparer("fm", "Example <&var> text\n");
  
  public static void main(String[] args) {
    Test_StringPreparer test = new Test_StringPreparer();
    try {
      test.test1();
    } catch (IOException e) {
      System.err.println("Exception: " + e.getMessage());
    }
  }
  
  
  
  void test1() throws IOException {
    StringFormatter fm1 = new StringFormatter(100);
    Map<String, Object> vars = new TreeMap<String, Object>();
    vars.put("var", "TT");
    fm.exec(fm1, vars);
  }
  
  
  
}
