package org.vishia.util;

import java.util.Arrays;
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
    }
  }
  

  
  public static long[][] orConditionBits( long[][] src, @AccessPolicy.ReadOnly long[][] or, long[] allBitsArg) {
    if(or == null ) { return src; }                    //no condition given. 
    else if(or.length ==0) {                             //unconditional given. 
      return src;
//      if(this.condBits == unconditional) { return false; } // unchanged
//      else { this.condBits = unconditional; return true; } // changed to null
    }
    else {
      long[] allBits = allBitsArg == null ? null : Arrays.copyOf(allBitsArg, allBitsArg.length);
      int ix0;
      long[][] dst;
      if(src == null) {
        dst = new long[or.length][or[0].length];
        ix0 = 0;
      } else {
        ix0 = src.length;
        dst = Arrays.copyOf(src, ix0 + or.length);
      }
      boolean bChanged = false;
      for(int ixOr = 0; ixOr < dst.length; ++ixOr) {
        if(ixOr >= ix0) {                                    // set to the 2th dimension new elements.
          dst[ixOr] = Arrays.copyOf(or[ixOr - ix0], or[ixOr - ix0].length );
          bChanged = true;
        }
        if(allBits !=null) {
          for(int ix = 0; ix < dst[ixOr].length; ++ix) {
            allBits[ix] &= ~dst[ixOr][ix];           // check all bits whether fulfills the allBits.
          }
        }
      }
      if(allBits !=null) {
        boolean allBitsSet = true;
        for(int ix = 0; ix < allBits.length; ++ix) {  //-------__ check allBits whether all are 0
          if(allBits[ix] !=0) { allBitsSet = false; break; }
        }
        if(allBitsSet) {
          dst = new long[0][];
        }
      }
      return dst;
    }
  }

  
  public static long[] orConditionBits( long[] dst, long[] or) {
    if(or == null) return dst;
    else {
      final long[] ret;
      if(or.length > dst.length) { ret = Arrays.copyOf(dst, or.length); }
      else ret = dst;
      for(int ix = 0; ix < ret.length; ++ix) {
        if(ix < or.length) {
          ret[ix] |= or[ix];  
        }
      }
      return ret;
    }
  }
  
  
  /**Builds either a new long[] with all given and newBits
   * or returns src if newBits == null or returns newBits if src==null.
   * Given bit arrays are never changed (read only).
   * @param src given bits, maybe null
   * @param newBits new bits, maybe null
   * @return given and new bits are set here.
   */
  public static long[] setBits ( long[] src, long[] newBits) {
    if(newBits == null) return src;           // no change, may return null
    else if(src == null) return newBits;
    else {
      int z = src.length > newBits.length ? src.length : newBits.length;
      long[] ret = Arrays.copyOf(src, z);
      for(int ix = 0; ix < z; ++ix) {
        if(ix < newBits.length) {
          ret[ix] |= newBits[ix];  
        }
      }
      return ret;
    }
  }


  /**Builds either a new long[][] with all given and newBits
   * whereby the newBits are merged in any index of the second dimension of src.
   * or returns src if newBits == null or returns newBits if src==null.
   * Given bit arrays are never changed (read only).
   * @param src given bits, maybe null
   * @param newBits new bits, maybe null
   * @return given and new bits are set here.
   */
  public static long[][] setBits ( long[][] src, long[] newBits) {
    if(newBits == null) return src;           // no change, may return null
    else if(src == null) {
      long[][] ret = new long[1][];
      ret[0] = newBits;
      return ret;
    }
    else {
      long[][] ret = new long[src.length][];
      for(int ix2 = 0; ix2 < src.length; ++ix2) {
        int z = src[ix2].length > newBits.length ? src[ix2].length : newBits.length;
        ret[ix2] = Arrays.copyOf(src[ix2], z);
        for(int ix = 0; ix < z; ++ix) {
          if(ix < newBits.length) {
            ret[ix2][ix] |= newBits[ix];  
          }
        }
      }
      return ret;
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
  
  
  public static boolean equals(long[][] c1, long[][] c2) {
    if(c1 == null) { return c2 == null; }
    else if(c2 == null) { return c1 == null; }
    else if(c1.length != c2.length) { return false; }
    else {
      int z = c1.length;
      int mask1 = 0, mask2=0;
      for(int ix1 =0; ix1 < z; ++ix1) {             // cpmpare the OR dimension
        for(int ix2 =0; ix2 < z; ++ix2) {           // with all OR dimensions of the second condiiton.
          if( (mask2 & 1<<ix2)==0) {                // do not use this entry if it was used already.
            if(c1[ix1].length == c2[ix2].length) {
              int z0 = c1[ix1].length;
              boolean bDiff1 = false;
              for(int ix =0; ix < z0; ++ix) {
                if( c1[ix1][ix] != c2[ix2][ix]) { 
                  bDiff1 = true;
                  break;
                }
              }
              if(!bDiff1) {
                mask1 |= 1<<ix2;
                mask2 |= 1<<ix2;
              }
            }
          }
        }
      }
      return mask1 == mask2;  // proper equate OR conditions found.
    }
  }
  
  
  
  public static boolean equals(long[] c1, long[] c2) {
    if(c1 == null) { return c2 == null; }
    else if(c2 == null) { return c1 == null; }
    else if(c1.length != c2.length) { return false; }
    else {
      for(int ix =0; ix < c1.length; ++ix) {
        if(c1[ix] != c2[ix]) { 
          return false;
        }
      }
      return true;
    }
  }
  
  
  
  /**Checks whether in all cond OR elements all bits of allBitsArg are set.
   * This is then unconditionally.
   * @param cond given condition
   * @param allBitsArg all condition bits which are possible
   * @return true if cond contains all bits of allBitsArg 
   */
  public static boolean checkAllset ( long[][] cond, long[] allBitsArg) {
    long[] allBits = allBitsArg == null ? null : Arrays.copyOf(allBitsArg, allBitsArg.length);
    for(int ixOr = 0; ixOr < cond.length; ++ixOr) {
      for(int ix = 0; ix < cond[ixOr].length; ++ix) {
        allBits[ix] &= ~cond[ixOr][ix];           // check all bits whether fulfills the allBits.
      }
    }
    boolean allBitsSet = true;
    for(int ix = 0; ix < allBits.length; ++ix) {  //-------__ check allBits whether all are 0
      if(allBits[ix] !=0) { allBitsSet = false; break; }
    }
    return allBitsSet;
  }
  
  
  
  
  public static void writeBits(StringBuilder sb, long[] bits) {
    if(bits == null) { sb.append("null"); }
    else { char sep = '[';
      for(int ix = 0; ix<bits.length; ++ix) { sb.append(sep).append(Long.toHexString(bits[ix])); sep = ','; }
      sb.append(']');
    }
  }
  
  public static void writeBits(StringBuilder sb, long[][] bits) {
    if(bits == null) { sb.append("null"); }
    else {
      for(int ix = 0; ix<bits.length; ++ix) { writeBits(sb, bits[ix]); }
    }
  }

  
  public static CharSequence writeBits(long[] bits) {
    StringBuilder sb = new StringBuilder();
    writeBits(sb, bits);
    return sb;
  }  
  
  public static CharSequence writeBits(long[][] bits) {
    StringBuilder sb = new StringBuilder();
    writeBits(sb, bits);
    return sb;
  }  
  
  
  
  public static void writeBits(StringBuilder sb, int[] bits) {
    if(bits == null) { sb.append("null"); }
    else { char sep = '[';
      for(int ix = 0; ix<bits.length; ++ix) { sb.append(sep).append(Integer.toHexString(bits[ix])); sep = ','; }
      sb.append(']');
    }
  }
  
  public static void writeBits(StringBuilder sb, int[][] bits) {
    if(bits == null) { sb.append("null"); }
    else {
      for(int ix = 0; ix<bits.length; ++ix) { writeBits(sb, bits[ix]); }
    }
  }

  
  public static CharSequence writeBits(int[] bits) {
    StringBuilder sb = new StringBuilder();
    writeBits(sb, bits);
    return sb;
  }  
  
  public static CharSequence writeBits(int[][] bits) {
    StringBuilder sb = new StringBuilder();
    writeBits(sb, bits);
    return sb;
  }  
  
  
  
  
  public static void writeBitsObj(StringBuilder sb, Object obj) {
    if(obj == null) {
      sb.append("---");
    }
    else {
      sb.append('@').append(Integer.toHexString(obj.hashCode() & 0xfff)).append(':');
      if(obj instanceof int[]) {
        int[] ai = (int[])obj;
        writeBits(sb,ai);
      } else if(obj instanceof Object[]) {
        Object[] aobj = (Object[]) obj;
        sb.append('{');
        for(Object obj1: aobj) {
          writeBitsObj(sb,obj1);
          sb.append('|');
        }
        sb.append('}');
      } else {
        sb.append("?");
      }
    }
  }

  
  public static CharSequence writeBitsObj(Object obj) {
    StringBuilder sb = new StringBuilder();
    writeBitsObj(sb, obj);
    return sb;
  }  
  
  
  public static class IterBit implements IterableIterator<Integer> {

    final long[] bitArray;
    
    long bits;
    
    /**Counts the bit number */
    int ix;
    
    /**Shifts the bit to test*/
    private long mask; 
    
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
