package org.vishia.util.test;

import java.text.ParseException;

import org.vishia.util.StringPart;
import org.vishia.util.StringPartScan;

public class Test_StringPartScan
{

  
  
  public static void main(String[] args){
    try {
      //test_NumericConversion();
      test_scanPrinciple();
    } catch (Exception e) {
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
  
  
  private static void test_scanPrinciple() {
    String src = "  Test  A ";
    StringPartScan sc = new StringPartScan(src);
    StringPart sp = sc; //Use sp for basic operations.
    //
    sp.seekPos(3);       //from any reason positioned at 3, it is "est  A
    //
    if( sp.scan()        //It invokes scanStart() and returns a StringPartScan from a StringPart reference (sp is StringPart)
        .scan("est")     //Because the begin was 3 before, the scan starts on 3. sp.scanBegin remains 3.
        .scanSkipSpace() //The same as seekNoWhitespace(), but with StringPartScan-return, seeks sp.begin 
        .scan("B")       //checks whether "A" is found here. //All scan routines checks sp.currentOk. They do nothing if false. 
        .scanOk()){      //Checks sp.bCurrentOk. If true, then all scans are successfull
        //Then sets sp.scanStart to the sp.begin, the current position of the scan chain.
        //if sp.bCurrentOk == false, then sp.begin is restored to the beginScan position.
      check(false, "test_scanPrinciple");
    }
    else if( sp.scan()        //It invokes scanStart() and returns a StringPartScan from a StringPart reference (sp is StringPart)
        .scan("est")     //Because the begin was 3 before, the scan starts on 3. sp.scanBegin remains 3.
        .scanSkipSpace() //The same as seekNoWhitespace(), but with StringPartScan-return, seeks sp.begin 
        .scan("A")       //checks whether "A" is found here. //All scan routines checks sp.currentOk. They do nothing if false. 
        .scanOk()){      //Checks sp.bCurrentOk. If true, then all scans are successfull
        //Then sets sp.scanStart to the sp.begin, the current position of the scan chain.
        //if sp.bCurrentOk == false, then sp.begin is restored to the beginScan position.
      check(true, "test_scanPrinciple");
    }
    else {
      check(false, "test_scanPrinciple");
    }
    sp.setParttoMax();
    sp.setIgnoreWhitespaces(true);
    if( sp.scan()        //It invokes scanStart() and returns a StringPartScan from a StringPart reference (sp is StringPart)
        .scan("Test")     //Because the begin was 3 before, the scan starts on 3. sp.scanBegin remains 3.
        .scanSkipSpace() //The same as seekNoWhitespace(), but with StringPartScan-return, seeks sp.begin 
        .scanOk() ) {
      char cc = sp.getCurrentChar();
      check(cc=='A', "test_scanPrinciple getCurrentChar");
      if(sc.scan("B").scanOk()) {
        check(false, "test_scanPrinciple-partial");
      }
      else if(sc.scan("A").scanOk()) {
        check(true, "test_scanPrinciple-partial");
      }
      else {
        check(false, "test_scanPrinciple-partial");
      }
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
