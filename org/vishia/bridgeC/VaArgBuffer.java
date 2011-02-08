package org.vishia.bridgeC;


/**This class represents a buffer, which stores variable arguments in the kind of C language:
 * The Implementation in C is much other than in Java. 
 * In Java it is simple: All arguments are of type Object, and the variable argument list
 * is a Object[]. The memory of all arguments are handled in garbage collection. It is really simple.
 * But in C there are some problems:
 * <ul><li>Type of arguments
 * <li>Location of arguments: Simple numeric values are in stack (only call by value is possible)
 *     but strings (const char*) may be non persistent. No safety memory management is present.
 * </ul>
 * Therefore in C language some things should be done additionally. See the special C implementation
 * in VaArgList.c. 
 * 
 * @author JcHartmut
 *
 */
public class VaArgBuffer
{
  private final Object[] args;

  public final int length;
  
  private final Va_list va_list;
  
  public VaArgBuffer(int size)
  { this.length = size;
    args = new Object[size];
    va_list = new Va_list(this);
  }
  
  public VaArgBuffer(Object[] src)
  { args = src;
    length = src.length;
    va_list = new Va_list(this);
  }
  
  /**Copies values from a source variable argument list. 
   * In C a variable argument list is only a byte sequence in stack, without any description.
   * The meaning of the bytes is only known from the evaluating routine, at example printf(...).
   * In Java a variable argument list is an array of instances.
   * 
   * @param formatText This argument is only used in C/C++ if the input doesn't contain a type information.
   *        In java it's unnecessary because Va_list always contains the type.
   * @param inputVaArgs
   */
  public void copyFrom(String formatText, Va_list input)  //Object... inputVaArgs)
  { int len = input.buffer.length;
    if(len > length)
    { throw new ArrayIndexOutOfBoundsException("too many arguments");
      //len = length;
    }
    for(int idx = 0; idx < len; idx++)
    { args[idx] = input.buffer.args[idx];
    }
  }
  
  
  
  public static VaArgBuffer represent(Object... inputArgs)
  {
    VaArgBuffer argBuffer = new VaArgBuffer(inputArgs);
    return argBuffer;
  }
  
  
  public Va_list get_va_list()
  { return va_list;
  }
  
  
  
  /**Cleans the values in Buffer. 
   * For java-programming this action cleans up unnecessary references, the associated objects may be freed.
   * For C-programming this action cleans up the storage to prevent errors. This maybe also an effect in Java.
   */
  public void clean()
  {
    for(int idx = 0; idx < length; idx++)
    { args[idx] = null;
    }
  }
  
  public Object[] get(){ return args; }
}
