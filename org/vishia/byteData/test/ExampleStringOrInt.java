package org.vishia.byteData.test;

import org.vishia.byteData.ByteDataAccessBase;

/**This class is an example how to work with different structures of a payload determined by the content.
 * In this example the following C-language structure is presented:
 * <pre>
 *   typedef union 
 *   { struct { uint16 s1; int16 s2; } val;
 *     struct ( int16 s0; int8 strlen; char str[1]; } string;
 *   } TheUnion; 
 * </pre>
 * whereby the number of character in <code>str</code> are depending of the <code>strlen</code>.
 * Both variants will be distinguish with the value of the first integer s1. A value 0 means, it is a String. That's only an example. 
 * In C the <code>str</code> may be used as <code>char*</code> in a </code>strncpy(dst, data.string.str, data.string.strlen)</code>.
 * a pointer arithmetic sets a next pointer to the end of that data calculated with the <code>strlen</code>.
 * That may be middle level sophisticated. In C it may be recommended to use the ByteDataAccess in the same way
 * as in Java. It is available in the CRuntimeJavalike library.
 * 
 * @author hartmut Schorrig
 *
 */
public class ExampleStringOrInt extends ByteDataAccessBase
{
  public static class JavaData
  { int s1; 
    short s2;
    String str;
  }
  
  /**It is a constant to check the minimal number of bytes which is used by this element.
   * Note: The head size is 0 because the structure of the head is not unique.
   */
  static int kMinLength = 4;
  
  ExampleStringOrInt(){ super(0); }
  
  /**Reads the data, checks whether it is 2 short values or a string, fills and returns the output data.
   * @return a new Instance of JavaData. Either the {@link JavaData#str} is not null if a String is given
   *   or it is null and the {@link JavaData#s1} and s2 are set.
   */
  JavaData read() {
    JavaData data = new JavaData();
    int s0 = getChildUint16();     //read and check the first 2 bytes
    if(s0 == 0xffff){
      //A String is stored:
      int length = getChildUint8();
      data.str = getChildString(length);
    } else {
      data.s1 = s0;  //first read value
      data.s2 = getChildInt16();
      data.str = null; // explicit.
    }
    return data;
  }
  
  
  
  /**Writes a String in the requested format. The first 2 bytes are 0xffff, The next byte is the length of the String
   * and the String follows.
   * 
   * @param str If longer than 255 character the String is shortened.
   */
  void write(final String str){
    addChildInteger(2, 0xffff);  //adds the 0xffff, 2 Bytes signed.
    int strlen = str.length();
    final String str2;
    if(strlen > 255){   //shorten the input string if too long (alternatively: throw IllegalArgumentException)
      str2 = str.substring(0, 255);
      strlen = 255;
    } else {
      str2 = str;
    }
    addChildInteger(1, strlen);
    addChildString(str2);
  }
  

  
  /**Writes two values in the requested format. 
   * 
   * @param value1 should be in range 0..0xfffe, elsewhere IllegalArgumentException.
   * @param value2 the second 2-Byte-Value.
   */
  void write(int value1, short value2)
  {
    if(value1 <0 || value1 > 0xfffe) throw new IllegalArgumentException("value1 should not be 0xffff");
    addChildInteger(2, value1);   //unsigned 2 Byte
    addChildInteger(2, value2);  //signed 2 Byte
  }
  
  
}
