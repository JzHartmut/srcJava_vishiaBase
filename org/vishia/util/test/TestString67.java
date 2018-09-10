package org.vishia.util.test;

import java.io.File;

import org.vishia.util.FileSystem;

public class TestString67
{
  
  String ss;
  
  public void execute(){
    File file = new File("D:/vishia/Java/srcJava_vishiaBase/org/vishia/util/StringPart.java");
    int z = (int)file.length();
    ss = FileSystem.readFile(file);
    int sum = 0;
    long start = System.currentTimeMillis();
    for(int i=0; i<z-2000; ++i){
      String sub = ss.substring(i, i+2000);
      int pos = getPos(sub);
      sum += pos;
    }
    long time = System.currentTimeMillis() - start;
    System.out.printf("sum = %d, time = %d\n", sum, time);
    
  }
  
  
  
  
  public int getPos(String inp){
    return inp.indexOf(' ');
  }
}
