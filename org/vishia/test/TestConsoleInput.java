package org.vishia.test;

import java.io.Console;
import java.util.Scanner;

public class TestConsoleInput
{
  public static void main(String[] args) {
    try {
      System.out.println("type some character");
      //Console con = System.console();
      //char key2 = (char)con.reader().read();
      while(System.in.available() < 5);  //wait for input
      while(System.in.available() >0){
        char key1 = (char)System.in.read();
        System.out.append(key1);
      }
      System.in.skip(System.in.available());  //discards all input keys.
      Scanner scan = new Scanner(System.in);
      //scan.
      System.out.println("type a choice key A | B | C");
      
      char key = (char)System.in.read();
      switch(key){
        case 'A': case 'a': System.out.println("typed A");
      }
      scan.close();

    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
}
