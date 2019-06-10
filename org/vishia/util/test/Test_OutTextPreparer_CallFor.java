package org.vishia.util.test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.vishia.util.OutTextPreparer;
import org.vishia.util.StringFunctions;

public class Test_OutTextPreparer_CallFor
{
  /**Example for a user class, here with two lists of some colors. */
  public static class DataColor {

    /**Some colors for example for the for control. */
    public List<String> colors1 = new ArrayList<String>();
    public List<String> colors2 = new ArrayList<String>();

    public DataColor() {
      colors1.add("white");
      colors1.add("yellow");
      colors1.add("red");
      colors1.add("blue");
      colors1.add("green");
    
      colors2.add("cyan");
      colors2.add("magenta");
      colors2.add("gray");
      colors2.add("black");
    }

  } //class DataColor

  /**Composition class. */
  DataColor dataColor = new DataColor();

  /**It is an output String pattern which is called. See otxCall. 
   * It uses a variable 'colors' which may be supplied as argument. 
   */
  static final OutTextPreparer otxListColors = new OutTextPreparer("otxListColors" 
  , null            //no static data on construction
  , "colors, text"  //arguments need and used
  , "Colors: <:for:color:colors><&color><:if:color_next>, <.if><.for>");  //The pattern.


  /**It is the used output String pattern containing two calls of otxListColors. 
   * With different arguments for both calls the results are different.
   * It is an example for a complex output text. */
  static OutTextPreparer otxCall = new OutTextPreparer("otxCall"
  , Test_OutTextPreparer_CallFor.class
  , "dataColor, text1"    //arguments need and used.
  , "Text: <&text1> \n"   //The pattern.
  + "Test Call colors1: <:call:otxListColors: colors = dataColor.colors1, text='testtext'> END\n"
  + "Test Call colors2: <:call:otxListColors: colors = dataColor.colors2 > END\n"
  );

  /**To compare the test result. */
  static String resultExpected = 
    "Text: any test text \n" + 
    "Test Call colors1: Colors: white, yellow, red, blue, green END\n" + 
    "Test Call colors2: Colors: cyan, magenta, gray, black END\n";

  
  
  
  /**Ctor sets all data in this example. */
  Test_OutTextPreparer_CallFor() {
  }


  static void check(boolean cond, String text) {
    if(!cond) {
      System.out.println("failed: " + text);
    } else {
      System.out.println("ok: " + text);
    }
  }





  void testCall() throws IOException {
    StringBuilder sb = new StringBuilder(1000);
    OutTextPreparer.DataTextPreparer vars = otxCall.getArgumentData();
    //vars.setArgument("otxListColors", otxListColors);  //The sub OutTextPreparer have to be registered ad data too.
    vars.setArgument("dataColor", dataColor);        //The data class for access.
    vars.setArgument("text1", "any test text");
    otxCall.exec(sb, vars);
    int posOk = StringFunctions.compareChars(sb, 0, -1, resultExpected);
    check(sb.toString().equals(resultExpected), "Test_OutTextPreparer_CallFor:testCall()");
  }


  
  


  public static void test(String[] args) {
    //DataAccess.debugIdent = "dataColor";  //possibility to set a data depending debug break
    try {
      Test_OutTextPreparer_CallFor test = new Test_OutTextPreparer_CallFor();
      test.testCall();
    } catch (Exception e) {
      System.out.println("Exception: " + e.getMessage());
    }
  }

  
  
  public static void main(String[] args) { test(args); }
  
  
}
