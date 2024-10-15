package org.vishia.util;

import java.util.Iterator;

/**This class contains some basically operations to deal with bits in arrays or words.
 * @author hartmut Schorrig, www.vishia.org
 * @license LGPL, Lesser General Public License: You can use this software for your own, can copy and change it, 
 * but you must not remove the text of author and license.
 *
 */
public class Bitfield {

  
  
  /**Returns the bit number of the last set bit in the given bitArray.
   * This is a usefully operation if some elements are addressed by a set bit,
   * and the last set bit should be known for access this elements.
   * Using a long value for the bits is recommended, because usual the CPU architecture is anyway 64 bit or can deal with int64_t.
   * For any number of elements the bits are organized in an array with any length.
   * @bitArray any array of bits.
   * @return shown on examples: <ul>
   * <li>-1 if bits == null, bits == int[] or all bits are 0. 
   * <li> 0 if bits.length ==1 AND bits[0] == 0x00000001  because the last set bit is bit 0
   * <li> 8 if bits.length ==1 AND bits[0] == 0x000001ab  because the last set bit is bit 8
   * <li>65 if bits.length ==2 AND bits[1] == 0x00000002  because the last member which is !=0 is [1], and the last bit there is bit1
   * </ul>
   */
  public static int getLastBit (long[] bitArray) {
    if(bitArray == null ) { return -1; }
    else {
      int ix = bitArray.length;
      long bits = 0;
      while(--ix >=0 && (bits = bitArray[ix])==0); // the last word which is not 0
      return ix <0 ? -1 :  getLastBit(bits) + 64*ix;       // ix <0 if the array is 0 or empty
//      int nr = ix >=0 ? ix * 64 : 0;
//      if(bits <0) { nr += 64; }   // the last bit is set.
//      else {
//        while(bits >=0x100) {
//          nr +=8;
//          bits >>=8; 
//        }
//        while(bits !=0) {
//          nr +=1;
//          bits >>=1; 
//        }
//      }
//      return nr-1;  // nr from the last seen bit
    }
  }
  

  public static int getLastBit (long bitWord) {
    long bits = bitWord;
    int nr = 0;
    if(bits <0) { nr += 64; }   // the last bit is set.
    else {
      while(bits >=0x100) {
        nr +=8;
        bits >>=8; 
      }
      while(bits !=0) {
        nr +=1;
        bits >>=1; 
      }
    }
    return nr-1;  // nr from the last seen bit
  }
  

  public static class IterBit implements IterableIterator<Integer> {

    final long[] bitArray;
    
    long bits;
    
    int ix;
    
    long mask; 
    
    int nrBit = -1;
    
    
    public IterBit(long[] bitArray) {
      this.bitArray = bitArray;
      this.ix = -1;
      this.mask = 0x0000000000000000L;
      next();
    }
    
    @Override public boolean hasNext () {
      return this.nrBit >=0;
    }

    @Override public Integer next () {
      int nrBit = this.nrBit;
      if(this.bitArray != null) {
        int nrBitNext = nrBit +1;    // hint this.mask was shifted before
        this.nrBit = -1;
        while(this.ix < this.bitArray.length && this.nrBit == -1) {    // loop so long bits available and nrBit not set
          if( this.mask ==0                                 // if is overdriven, use next index, repeat with while
           || (this.bitArray[this.ix] & ~(this.mask-1)) ==0 ){    // if all bits equal or higher than mask, use next index 
            this.ix +=1; this.mask = 1;                      // start mask with 1;
            nrBitNext = this.ix <<6;
            if(this.ix < this.bitArray.length) {
              this.bits = this.bitArray[this.ix];
            }
          }
          else {
            if( (this.bits & this.mask) !=0) {
              this.nrBit = nrBitNext;                        // breaks the while
            }
            nrBitNext +=1;
            this.mask = this.mask <<1;    // first shift the mask 1 to left or start with 1
          }
      } }
      return Integer.valueOf(nrBit);
    }

    @Override public Iterator<Integer> iterator () {
      return this;
    }
    
  }  
  
}
