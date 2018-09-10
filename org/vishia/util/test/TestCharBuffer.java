package org.vishia.util.test;

import java.io.IOException;
import java.io.PipedWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.CharBuffer;

public class TestCharBuffer
{
  
  
  
  public static void main(String[] args){
    TestCharBuffer main = new TestCharBuffer();
    //main.testWriter();
    main.testReader();
    
  }
  
  
  void testWriter(){
    int pos;
    CharBuffer buf = CharBuffer.allocate(1000);
    StringWriter writer = new StringWriter();
    buf.put("test");
    pos = buf.position();
    buf.flip();             //prepare read out
    writer.append(buf);     //appends the content of the buffer but doesn't change the position.
    pos = buf.position();
  }
  
  
  
  void testReader(){
    int pos;
    CharBuffer buf = CharBuffer.allocate(1000);
    Reader reader = new StringReader("content");
    try {
      reader.read(buf);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      System.err.println("testReader");
    }
    pos = buf.position();
    buf.flip();             //prepare read out
    System.out.println(buf.toString());
    pos = buf.position();
  }
  
  
  
  void testPutGet(){
    CharBuffer buf = CharBuffer.allocate(1000);
    StringBuilder out = new StringBuilder();
    buf.put("test");
    buf.flip();
    char cc;
    while(true){
      cc = buf.get();
      out.append(cc);
    }
  }
}
