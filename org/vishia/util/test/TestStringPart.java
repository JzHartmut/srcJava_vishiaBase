package org.vishia.util.test;

import org.vishia.util.Assert;
import org.vishia.util.StringPartScan;

public class TestStringPart
{
  public static void main(String[] args){
    test_getCircumScriptionToAnyChar();
  }
  
  
  private static void test_getCircumScriptionToAnyChar()
  {
    StringPartScan sp = new StringPartScan("y\\<\\:arg\\><textExpr?argExpr>");
    sp.seek(1);
    String res = sp.getCircumScriptionToAnyChar("<?").toString();
    sp.close();
    Assert.check(res.equals("\\<\\:arg\\>"));
  }

}
