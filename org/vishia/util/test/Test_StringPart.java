package org.vishia.util.test;

import org.vishia.util.Assert;
import org.vishia.util.StringPart;

public class Test_StringPart
{
  public static void main(String[] args){
    test_seek_len_basics();
    //test_getCircumScriptionToAnyChar();
  }

  static void check(boolean cond, String text) {
    if(!cond) {
      System.out.println("failed: " + text);
    } else {
      System.out.println("ok: " + text);
    }
  }
  
  
  private static void test_getCircumScriptionToAnyChar()
  {
    StringPart sp = new StringPart("y\\<\\:arg\\><textExpr?argExpr>");
    sp.seekPos(1);
    String res = ""; //sp.getCircumScriptionToAnyChar("<?").toString();
    sp.close();
    Assert.check(res.equals("\\<\\:arg\\>"));
  }

  
  
  private static void test_seek_len_basics() {
    String str = " 0123456789ABC";
    int pos0 = str.indexOf('0');  //to compare, pos of 0
    StringPart sp = new StringPart(str);
    CharSequence res = sp.seek('0', 0).lento('4').getCurrent();  //inclusive 0, exclusive 4
    check(res.equals("0123"), "simple seek('c').lento('c')");
    //
    sp.setParttoMax().seek("01").lento("56");
    check(sp.getCurrent().equals("01234"), "simple seek(\"ss\").lento(\"ss\")");
    check(sp.getCurrentPosition()== pos0, "getCurrentPosition()");
    check(sp.length() == 5, "length()");
    //
    sp.lento("78");  //will not be found, because outside of current range
    check(sp.length() == 0 && sp.getCurrentPosition() == pos0, "lento(\"outsideStr\") - length()==0"); 
    //
    sp.len0end();
    check(sp.length() == str.length() - pos0 , "len0end() sets the length to last end.");
    //
    sp.lentoAnyChar("C9");
    sp.setCurrentMaxPart();
    sp.lento("AB").len0end();  //sets to "9" because "AB" is outside
    check(sp.getCurrent().equals("012345678"), "lentoAnyChar\"cc\").setCurrentMaxPart()");
  }
  
  
  
  
}
