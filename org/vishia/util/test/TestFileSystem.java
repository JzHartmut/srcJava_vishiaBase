package org.vishia.util.test;

import org.vishia.util.Assert;
import org.vishia.util.FileSystem;
import org.vishia.util.StringFunctions;

public class TestFileSystem
{
  public static void main(String[] args){
    test_normalizePath();
  }
  
  
  private static void test_normalizePath()
  {
    String[][] tests = {
      {"D:/test/test1/..", "D:/test"}
    , {"D:/test/test1/../..", "D:/"}
    , {"D:/test///././.././test1/..", "D:/"}
    , {"D:/test/..", "D:/"}
    };
    for(String[] test: tests){
      CharSequence s1 = FileSystem.normalizePath(test[0]);
      int cmpr = StringFunctions.compare(s1, test[1]);
      Assert.check(cmpr == 0);
    }
  }
  
  
  
  
}
