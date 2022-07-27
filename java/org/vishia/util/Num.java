package org.vishia.util;


/**Contains some numeric operations able to use in JzTxtCmd
 * @author Hartmut Schorrig
 *
 */
public class Num
{
  /**Version, history and license.
   * <ul>
   * <li>2018-08-29 Hartmut new:  
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
   * <li> But the LPGL is not appropriate for a whole software product,
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
   * 
   * 
   */
  //@SuppressWarnings("hiding")
  public static final String version = "2018-08-28";

  
  
  public static int shBits32To(int src, int bitPos, int nrofBits) { return ( (src & ((1<<nrofBits)-1)) << bitPos); }
  
  public static int shBits32From(int src, int bitPos, int nrofBits) { return ( (src >> bitPos) & ((1<<nrofBits)-1)); }
    
  public static long shBits64To(long src, int bitPos, int nrofBits) { return ( (src & ((1<<nrofBits)-1)) << bitPos); }
  
  public static long shBits64From(long src, int bitPos, int nrofBits) { return ( (src >> bitPos) & ((1<<nrofBits)-1)); }
  
  public static short shBits16To(short src, int bitPos, int nrofBits) { return (short)( (src & ((1<<nrofBits)-1)) << bitPos); }
  
  public static short shBits16From(short src, int bitPos, int nrofBits) { return (short)( (src >> bitPos) & ((1<<nrofBits)-1)); }
  
  
  /**Shift the src to bitPos and mask after them.
   * @param src any number usual 0...n
   * @param bitPos positive: shift left.
   * @param mask regarded to the return value bitPos
   * @return for example shMask32To(0xeeee5, 4, 0x00f0) results in 0x50
   */
  public static int shMask32To(int src, int bitPos, int mask) { return ( (src << bitPos)  & mask); }
  
  /**Shift the src from bitPos and mask after them.
   * @param src any bit combination
   * @param bitPos positive: shift right.
   * @param mask regarded to the return value bitPos
   * @return for example shMask32To(0xeee5e, 4, 0x000f) results in 0x5
   */
  public static int shMask32From(int src, int bitPos, int mask) { return ( (src >> bitPos) & mask ); }
    
  public static long shMask64To(long src, int bitPos, int mask) { return ( (src << bitPos)  & mask); }
  
  public static long shMask64From(long src, int bitPos, int mask) { return ( (src >> bitPos) & mask ); }
  
  public static short shMask16To(short src, int bitPos, int mask) { return (short)( (src << bitPos)  & mask); }
  
  public static short shMask16From(short src, int bitPos, int mask) { return (short)( (src >> bitPos) & mask ); }
  
  

  
}
