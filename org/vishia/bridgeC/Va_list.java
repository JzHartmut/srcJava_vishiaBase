package org.vishia.bridgeC;


/**Represents a va_list type from stdarg.h. */
public class Va_list
{
  public final VaArgBuffer buffer;
  
  public Va_list(VaArgBuffer buffer){ this.buffer = buffer; }
  
  /**Returns the stored arguments. In Java it is a Object[].
   * In C the va_list is used immediate.
   */
  public Object[] get(){ return buffer.get(); }
}
