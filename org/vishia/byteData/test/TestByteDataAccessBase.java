//Note: Test with jzcmd: call jzcmd with this java file with its full path:
//D:/vishia/Java/srcJava_vishiaBase/org/vishia/byteData/test/TestByteDataAccessBase.java
//==JZcmd==
//Note: The JZcmd detect its code after //JZcmd too. It starts on the //==JZcmd== label.
//JZcmd Obj a = java org.vishia.byteData.test.TestByteDataAccessBase.main();
//==endJZcmd==

package org.vishia.byteData.test;


public class TestByteDataAccessBase
{
  byte[] data = new byte[100];
  
  ExampleByteDataAccessBase accHead = new ExampleByteDataAccessBase();
  
  ExampleStringOrInt accStringOrInt = new ExampleStringOrInt();
  
  
  
  /**Writes a String in the structure, test it with {@link #testread(int)}. */
  int testwrite(String testString)
  {
    accHead.assignClear(data);
    accHead.set_HeadSignification(0xabcd);
    accHead.set_cmd(1);
    accHead.addChild(accStringOrInt);
    accStringOrInt.write(testString);
    int lengthWithAllChild = accHead.getLengthTotal();
    accHead.set_length(lengthWithAllChild);
    return lengthWithAllChild;
  }
  

  /**Writes two integer in the structure, test it with {@link #testread(int)}. */
  int testwrite(int cmd, int value1, short value2)
  {
    accHead.assignClear(data);
    accHead.set_HeadSignification(0xabcd);
    accHead.set_cmd(cmd);
    accHead.addChild(accStringOrInt);
    accStringOrInt.write(value1, value2);
    int lengthWithAllChild = accHead.getLengthTotal();
    accHead.set_length(lengthWithAllChild);
    return lengthWithAllChild;
  }
  
  /**Reads the written data and output its to System.out. */
  void testread(int lengthData)
  { accHead.assign(data, lengthData);
    int signification = accHead.get_headSignification();
    int cmd = accHead.get_cmd();
    int length = accHead.get_length();
    if(accHead.sufficingBytesForNextChild(accStringOrInt.kMinLength)){
      accHead.addChild(accStringOrInt, length - accHead.getLengthCurrent());
      ExampleStringOrInt.JavaData data = accStringOrInt.read();
      if(data.str !=null) {
        System.out.println("TestByteDataAccess - read, sign=" + signification + ", cmd=" + cmd + ", length=" + length 
            + ", text=" + data.str);
      } else {
        System.out.println("TestByteDataAccess - read, sign=" + signification + ", cmd=" + cmd + ", length=" + length 
            + ", val1,2=" + data.s1 + "," + data.s2);
      }
    }
  }

  
  /**Testsequence. */
  void test()
  { accHead.setBigEndian(true);
    int length = testwrite("Hello ByteData");
    testread(length);
    accHead.setBigEndian(false);  //use another endian.
    length = testwrite(2, 17, (short)1000);
    testread(length);
  
  }
  
  public static void main(String[] args){
    (new TestByteDataAccessBase()).test();
  }
}
