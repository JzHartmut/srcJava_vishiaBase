package org.vishia.util.test;

import org.vishia.util.StringFormatter;
import org.vishia.util.StringPreparer;

public class Test_StringPreparer
{

  StringPreparer fm = new StringPreparer("Example <&var> text\n");
  
  public static void main(String[] args) {
    Test_StringPreparer test = new Test_StringPreparer();
    test.test1();
  }
  
  
  
  void test1() {
    StringFormatter fm1 = new StringFormatter(100);
    fm.exec(fm1, "TT");
  }
  
  
  
}
