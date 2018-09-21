package org.vishia.util.test;

import java.io.FileWriter;
import java.io.IOException;

import org.vishia.util.StringFormatter;

public class Test_StringFormatter
{

  
  
  
  
  
  
  public static void main(String[] args) {
    test_writeHexBlock();
  }
  
  
  
  static void test_writeHexBlock() {
    byte[] data = new byte[0x23];
    for(int ix = 0; ix < data.length; ++ix) {
      data[ix] = (byte)ix;
    }
    try{ 
      FileWriter fw = new FileWriter("t:/test.hex");
      
      StringFormatter fm = new StringFormatter(fw, true, "\r\n", 120);
      //writes 0x22 bytes because the last word with 2 bytes is incomplete.
      fm.addHexBlock(data, 0, -1, (short)16, (short)2);
      fm.close();
      //ByteDataAccessSimple.writeHex16(trcByteDst[ixTrace], 0, -1, fout);
    
    } catch(IOException exc) { System.err.println("IOException file "); }

  }
  
  
  
}
