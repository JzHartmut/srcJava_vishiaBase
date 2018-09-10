//Note: Test with jzcmd: call jzcmd with this java file with its full path:
//D:\vishia\Java\srcJava_vishiaBase\org\vishia\byteData\test\TestByteDataAccessChildren.java
//==JZcmd==
//Note: The JZcmd detect its code after //JZcmd too. It starts on the //==JZcmd== label.
//JZcmd Obj a = java org.vishia.byteData.test.TestByteDataAccessChildren.main(null);
//==endJZcmd==

package org.vishia.byteData.test;

import org.vishia.byteData.ByteDataAccessBase;

public class TestByteDataAccessChildren
{
  byte[] data = new byte[100];
  
  ByteDataAccessBase acc = new ByteDataAccessBase(8);

  ByteDataAccessBase accChild = new ByteDataAccessBase(4);

  ByteDataAccessBase accGrandChild = new ByteDataAccessBase(2);

  
  /**Testsequence. */
  void test()
  { acc.setBigEndian(true);
    acc.setException(false);
    acc.assignClear(data);     //yet only the head, 8 bytes.
    acc.addChild(accChild);    //now head and a child with head, 12 bytes.
    accChild.setLengthElement(16);  //the child has 16 bytes from 8..24 and is not expandable yet.
    accChild.addChild(accGrandChild);  //added at pos 12
    accGrandChild.addChildInteger(4, 0xabcdef01);  //This value is written to pos 14 after head of grand child.
    accGrandChild.addChildInteger(2, 0xaffe);  //This value is written to pos 18.
    accGrandChild.addChildInteger(4, 0xcafe);  //This value is written to pos 20.
    accGrandChild.addChildInteger(4, 0x12345678);  //This value is not written because accGrandChild is full.
    //
    acc.addChild(accChild);  //The accChild is used the second time, it is cleared therewith
    accChild.setLengthElement(16);  //sets this fix length, therewith positioned from 24 to 40.
    accChild.addChildInteger(2, 0xabcd);  //writes at position 28, after the head of accChild
    acc.addChild(accChild); //Use accChild newly. Add it at position  40 now because last child was till 40. 
  }
  
  public static void main(String[] args)
  {
    (new TestByteDataAccessChildren()).test();
  }

}
