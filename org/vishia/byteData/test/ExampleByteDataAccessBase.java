package org.vishia.byteData.test;

import org.vishia.byteData.ByteDataAccessBase;

/**This class is an example for a derived class of ByteDataAccessBase
 * which manages 8 bytes for a head structure and some data. Such a class Java source code can be generated automatically
 * from parsed header files.
 */
class ExampleByteDataAccessBase extends ByteDataAccessBase
{ 
  /**It is a good idea to define some constants which describe the position of elements in the head.
   * That constants may be written manually by knowing the memory layout of such a struct.
   * This adequate class can be generated from a header file automatically too. See TODO 
   * The struct of the head may be (C-syntax):
   * <pre>
   * typedef struct MyStruct_t
   * { int32 headSignification;
   *   u_int16 cmd;
   *   int16 length;
   * } MyStruct;
   * </pre>
   * */
  private static final int kHeadSignification = 0;
  
  /**Position of the element cmd */
  private static final int kCmd = 4;
  
  /**Position of the element length */
  private static final int kLength = 6;
  
  /**The length, <code>sizeof(MyStruct)</code> in C-language. */
  public static final int kSizeHead = 8;
  
  
  /**The constructor is parameterless, because the head size is known internally. It invokes
   * super(kSizeHead) whereby super() is {@link ByteDataAccessBase#ByteDataAccessBase(int)}. */
  protected ExampleByteDataAccessBase()
  {
    super(kSizeHead);
  }
  
  
  /**For each element of the struct a get and a set method are written here.
   * The user can access this simple methods to get and set the data which are presented in the associated byte[] data
   * on the position where the instance of this class is assigned.
   * 
   * @return The value from byte[] data on the correct position converted from the correct coding.
   */
  int get_headSignification(){ return getInt32(kHeadSignification); }
  
  /**Sets the data for head signification. 
   * @param value The value which are written in byte[] data on the correct position converted to the correct coding.
   */
  void set_HeadSignification(int value){ setInt32(kHeadSignification, value); } 

  /**Gets the command. Note: Because the C-struct defines a unsigned int with 16 bit in Java the usage of short is not sufficient.
   * The 16-bit unsigned value can be processed only in an int value because the value range is positive from 0 till 65535.
   * @return
   */
  int get_cmd(){ return getUint16(kCmd); }
  
  
  void set_cmd(int value){ setUint16(kCmd, value); }
  
  
  /**Gets the length. In this example the length is a signed int16. It may be convenient 
   * because  the length value may be in range of at maximum 4000 for a limited size and a flag for 'unused' as value -1
   * may be set in C-language.  
   * @return The length. For java usage a short in proper. But an int may be expedient too because the processor for Java
   *   is a 32-bit-machine usually.
   */
  short get_length(){ return getInt16(kLength); }
  
  
  void set_length(int value){ setInt16(kLength, value); }
  
}
