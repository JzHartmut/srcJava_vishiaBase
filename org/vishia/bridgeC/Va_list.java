package org.vishia.bridgeC;




/**Represents a Java-complement to a C-struct which contains va_list reference from stdarg.h. 
 * The struct in C has the form:
 * <pre>
 * typedef struct Va_listFW_t
 * { 
 *   char const* typeArgs;    //types of arguments.
 *   va_list args;            //pointer to the last known variable in stack
 * } Va_listFW;
 * </pre>
 * In Java the instance contains a reference to {@link VaArgBuffer} only.
 * For compatibility with C-translated Java one can only initialize the {@link Va_list} with a variable
 * which is the variable argument of a methods argument list. In Java semantic you can initalize with any variable,
 * but that is not able to translate from Java to C (Java2C).
 * */
public class Va_list
{
  
  /**Version, history and license.
   * <ul>
   * <li>2013-03-24 Hartmut new: {@link #size()} If size()=0 it has no data. 
   *   Then the list should not be used to invoke {@link java.lang.String#format(String, Object...)}.
   * <li>2007-00-00 Hartmut created 
   * </ul>
   * 
   * <b>Copyright/Copyleft</b>:
   * For this source the LGPL Lesser General Public License,
   * published by the Free Software Foundation is valid.
   * It means:
   * <ol>
   * <li> You can use this source without any restriction for any desired purpose.
   * <li> You can redistribute copies of this source to everybody.
   * <li> Every user of this source, also the user of redistribute copies
   *    with or without payment, must accept this license for further using.
   * <li> But the LPGL ist not appropriate for a whole software product,
   *    if this source is only a part of them. It means, the user
   *    must publish this part of source,
   *    but don't need to publish the whole source of the own product.
   * <li> You can study and modify (improve) this source
   *    for own using or for redistribution, but you have to license the
   *    modified sources likewise under this LGPL Lesser General Public License.
   *    You mustn't delete this Copyright/Copyleft inscription in this source file.
   * </ol>
   * If you are intent to use this sources without publishing its usage, you can get
   * a second license subscribing a special contract with the author. 
   * 
   * @author Hartmut Schorrig = hartmut.schorrig@vishia.de
   */
  public static int version = 20120609;
  

  
  
  final VaArgBuffer buffer;
  
  public Va_list(VaArgBuffer buffer){ this.buffer = buffer; }
  
  /**Constructs a Va_list object which is the conterpart to a C-language <code>Va_listFW</code>.
   * The argument <code>args</code> in C-language is a pointer to variable arguments in the stack,
   * more exact to the last known variable before a variable argument list follows. This last known variable
   * should be a String Literal with contains the type characters like <code>"FIB"</code> by convention. 
   * That is a pointer of type <code>char const*</code>. 
   * In C-language the both values are gotten with
   * <pre>
   * myVA_list.typeArgs = args;
   * va_start(myVA_list.args, args);
   * </pre>
   * In Java a variable argument list is a <code>Object[]</code>.
   * @param args refers a variable argument list.
   */
  public Va_list(Object... args)
  { buffer = new VaArgBuffer(args);
  }
  
  /**Returns the stored arguments. In Java it is a Object[].
   * In C the va_list is used immediate.
   */
  public Object[] get(){ return buffer.get(); }
  
  /**Returns the number of arguments in the variable argument list.
   * In the C implementation it is the sizeof(typeArgs).
   * @return especially 0 if no variable arguments are given.
   */
  public int size(){ return buffer.length; }
  
}
