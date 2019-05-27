package org.vishia.util.test;

import java.text.ParseException;

import org.vishia.util.StringPartScan;

public class Test_StringPartScan
{

  
  
  public static void main(String[] args){
    try {
      test_NumericConversion();
    } catch (ParseException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    //test_getCircumScriptionToAnyChar();
  }

  static void check(boolean cond, String text) {
    if(!cond) {
      System.out.println("failed: " + text);
    } else {
      System.out.println("ok: " + text);
    }
  }
  
  
  
  private static void test_NumericConversion() throws ParseException {
    String src      = "            +1e+3            -0'123.045e3           1\"234'567              -0.0625    ";
    Result[] results = {new Result(+1e+3), new Result(-123.045e3), new Result(1234567), new Result(-0.0625)};
    StringPartScan sp = new StringPartScan(src);
    int ixResult = -1;
    boolean bError = false;
    sp.setIgnoreWhitespaces(true);       //necessary for leading spaces
    while(!bError && sp.length() >0) {
      if(sp.scanStart().scanFloatNumber('.', true, "'\"").scanOk()) {
        double number = sp.getLastScannedFloatNumber();
        Result result = results[++ixResult];
        check(result.kind == 'd' && result.d == number, "scanFloat" + ixResult);
      }
      else if(sp.scanStart().scanInteger("'\"").scanOk()) {
        long number = sp.getLastScannedIntegerNumber();
        Result result = results[++ixResult];
        check(result.kind == 'i' && result.i == number, "scanInteger" + ixResult);
      } else {
        bError = true;
        check(false, "scanning error on " + ixResult);
      }
      sp.seekNoWhitespace();  //necessary for end detection, skip over trailing spaces.
    }
    sp.close();
  }
  
  
  static class Result{
    double d; long i; char kind;
    Result(double d){ this.kind = 'd'; this.d = d; }
    Result(long i){ this.kind = 'i'; this.i = i; }
  }

}
