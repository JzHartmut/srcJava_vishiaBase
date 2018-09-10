package org.vishia.bridgeC;

/**This class supports some special division algorithm for processors which have not a division hardware on chip.
 * A normal division is a more complex and longer algorithm. In Java it is not a problem, because the JVM supports
 * division in a so long as proper way. But if algorithm for embedded processors are written with Java
 * and used or translated to C, this algorithm may be proper. Therefore they are used in Java too.
 * <br><br>
 * An instance of this class contains 2 integer results for the quotient and the remainder of division.
 * The 2 integer values may be returned in register variable though it is a data struct in C.
 * <br><br>
 * This class contains some static methods which returns an instance of this class as its result. 
 * 
 * @author Hartmut Schorrig
 *
 */
public class DivideJc
{
  /**Version, history and license.
   * <ul>
   * <li>2013-12-31 Hartmut created. 
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
  static final public int version = 0x20140101;

  /**Quotient and remainder. */
  public int q, r; 
  
  
  public DivideJc(int q, int r){ this.q = q; this.r = r; }
  
  /**This algorithm can be used if a number should be divide by a divisor which is near but less
   * a power of 2 value and the divisor is build in a special simple form. For example 0x3c0 is near 0x400.
   * The idea is:
   * <ul>
   * <li>A division by a power of 2 is a simple shift operation.
   * <li>A division by a number, which is near another number is aproximately the division by the other number
   *   adjusted by the abbreviation:
   *   <pre>y = x / 1.9  ~=  x / 2.0 + (x/2.0) * (2.0-1.9)/2.0; 
   *   </pre>
   *   One can divide by 2.0 instead divide by 1.9, then adjust the result with the ratio of abbreviation.
   * <li>A backward multiplying checks the correctness and calculates the remainder.  
   * </ul>
   * The divisor is given by 2 bits which describes the power of 2 - assembly of the divisor:
   * <ul>
   * <li> bit1 The bit which describes the nearest power of 2 of divisor. (1<<bit1) is the nearest power of 2.
   * <li> bit2 The bit which describes the distance from the nearest power of 2. 1<<bit2 is the distance.
   *   (1<<bit1) - (1<<bit2) is the divisor. 
   * </ul>  
   * For example:
   * <pre>
   * bit1      bit2     divisor
   *   10         2     0x03fc  = 0x0400 - 0x0004
   *   10         6     0x03c0  = 0x0400 - 0x0040
   *   10         8     0x0300  = 0x0400 - 0x0100
   *   12         2     0x0ffc  = 0x1000 - 0x0004
   *   12         6     0x0fc0  = 0x1000 - 0x0040
   *   12         8     0x0f00  = 0x1000 - 0x0100
   *   12        10     0x0c00  = 0x1000 - 0x0400
   * </pre>
   * This algorithm is used to calculate indices of arrays in a {@link org.vishia.util.IntegerBlockArray}
   * which has a size less a power of 2 because it can be used with the BlockHeap concept in C language.
   * @param val Any integer value, the dividend.
   * @param bit1 The bit which describes the nearest power of 2 of divisor. (1<<bit1) is the nearest power of 2.
   * @param bit2 The bit which describes the distance from the nearest power of 2. 1<<bit2 is the distance.
   *   (1<<bit1) - (1<<bit2) is the divisor.
   * @return quotient and remainder.
   */
  public static DivideJc divLess2pow(int val, int bit1, int bit2){
    int div1 = 1<<bit1;
    int div2 = 1<<bit2;
    int div3 = div1 - div2;  //the real divisor
    int bitdiff = bit1 - bit2;
    int quotient = 0;
    int rest = val;
    do{
      int add = rest >> bit1;
      add += add >> bitdiff;
      if(add == 0 && rest >= div3){
        add = 1;
      }
      quotient += add;
      int c1 = quotient * div3;
      rest = val - c1;
    } while(rest >= div3);
    return new DivideJc(quotient, rest);
  }



  @Override public String toString(){ return "" + q + ", R " + r; }
  
  
  
  /**Only for test in debug mode.
   * @param args not used.
   */
  public static void main(String[] args){
    int val, valtest;
    val = 0x555a001;  //example value, take others.
    int bit1 = 12;
    int bit2 = 9;
    //
    DivideJc q = divLess2pow(val, bit1, bit2);  
    //
    //check result
    //
    int divisor = (1<<bit1) - (1<<bit2); 
    valtest = q.q * divisor + q.r;
    assert(val == valtest);
  }
  
  

}
