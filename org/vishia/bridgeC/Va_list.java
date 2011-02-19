package org.vishia.bridgeC;


/**Represents a va_list type from stdarg.h. */
public class Va_list
{
  final VaArgBuffer buffer;
  
  public Va_list(VaArgBuffer buffer){ this.buffer = buffer; }
  
  public Va_list(Object... args)
  { buffer = new VaArgBuffer(args);
  }
  
  /**Returns the stored arguments. In Java it is a Object[].
   * In C the va_list is used immediate.
   */
  public Object[] get(){ return buffer.get(); }
}
