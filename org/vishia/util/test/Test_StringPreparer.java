package org.vishia.util.test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.vishia.util.StringFormatter;
import org.vishia.util.StringPreparer;

public class Test_StringPreparer
{

  Map<String, Object> vars = new TreeMap<String, Object>(); 

  List<String> colors = new LinkedList<String>();

  StringPreparer fm = new StringPreparer("fm", "Example <&var> text\n");
  
  StringPreparer sListColors = new StringPreparer("sListColors"
  , "Text: <&obj1> \n"
  + "\n"
  + "<:for:color:colors><&color> , <.for>\n"
  + "\n"
  + "\n"
  + "\n"
  );

  StringPreparer sCall = new StringPreparer("sCall"
  , "Test Call: <:call:sListColors> \n"
  );


  
  
  
  
  
  Test_StringPreparer() {
    colors.add("white");
    colors.add("yellow");
    colors.add("red");
    colors.add("blue");
    colors.add("green");

  }


  static void check(boolean cond, String text) {
    if(!cond) {
      System.out.println("failed: " + text);
    } else {
      System.out.println("ok: " + text);
    }
  }


  
  void test1() throws Exception {
    //Object obj = DataAccess.access("s1", data, true, false, false, null);
    vars.put("colors", colors);
    vars.put("obj1", "exampleText");
    StringWriter sw = new StringWriter(1000);
    @SuppressWarnings("unused") StringBuffer sb = sw.getBuffer();
    sListColors.exec(sw, vars);
  }


  void testCall() throws Exception {
    //Object obj = DataAccess.access("s1", data, true, false, false, null);
    Map<String, Object> vars = new TreeMap<String, Object>(); 
    vars.put("colors", colors);
    vars.put("obj1", "exampleText");
    vars.put("sListColors", sListColors);
    StringWriter sw = new StringWriter(1000);
    StringBuffer sb = sw.getBuffer();
    sCall.exec(sw, vars);
    StringBuilder bb;
    String resultExpected = "Test Call: Text: exampleText \n" + 
        "\n" + 
        "white , yellow , red , blue , green , \n" + 
        "\n" + 
        "\n" + 
        "\n" + 
        " \n" + 
        "";
    check(sb.toString().equals(resultExpected), "testCall");
  }


  
  
  void test2() throws Exception {
    StringFormatter fm1 = new StringFormatter(100);
    Map<String, Object> vars = new TreeMap<String, Object>();
    vars.put("var", "TT");
    fm.exec(fm1, vars);
  }
  
  


  public static void main(String[] args) {
    Test_StringPreparer test = new Test_StringPreparer();
    try {
      test.testCall();
    } catch (Exception e) {
      System.err.println("Exception: " + e.getMessage());
    }
  }
  
} //class Test_StringPreparer 
