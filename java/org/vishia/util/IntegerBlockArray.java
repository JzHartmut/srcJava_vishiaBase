package org.vishia.util;

import org.vishia.bridgeC.DivideJc;

/**This class is a int array. The access is done with {@link #get(int)} and {@link #set(int, int)}.
 * The array is divide in many parts, whereby one part can be stored in one block of a block-heap,
 * see {@link org.vishia.bridgeC.AllocInBlock}. Usual one part can store about 100 values. 
 * With a 2-level tree structure 10000 values can be stored, with 3 level 1 million etc. 
 * The size of the array is general unlimited, only the memory space on execution machine is limited.
 * Increasing the size does not re-allocate and does not move large data, only allocate new memory blocks.
 *   
 * @author Hartmut Schorrig
 *
 */
public class IntegerBlockArray
{
  /**Version, history and license.
   * <ul>
   * <li>2014-05-23 Hartmut bugfix {@link #binarySearch(int, int)}, new {@link #binarySearchFirstKey(int, int)}.
   *   It was a mix before with a faulty algorithm.
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

  
  int[] array;
  
  int bit1 = 10, bit2 = 7;

  /**Size of this array inclusively maybe sub blocks. */
  int size;
  
  //int nBits;
  
  
  IntegerBlockArray[] blocks; 
  
  
  IntegerBlockArray(int newSize){
    setSize(newSize);
  }
  
  int get(int ix){
    if(blocks !=null){
      DivideJc ixBlock = DivideJc.divLess2pow(ix, bit1, bit2);
      IntegerBlockArray block = blocks[ixBlock.q];
      if(block == null) return 0;  //an uninitialized block is a value of 0.
      else {
        return block.get(ixBlock.r);
      }
    } else {
      return array[ix];
    }
  }
  
  
  void set(int ix, int val){
    if(blocks !=null){
      DivideJc ixBlock = DivideJc.divLess2pow(ix, bit1, bit2);
      IntegerBlockArray block = blocks[ixBlock.q];
      if(block == null){
        int nBlocks = (1<<bit1) - (1<<bit2);
        block = blocks[ixBlock.q] = new IntegerBlockArray(nBlocks); //
      }
      block.set(ixBlock.r, val);  
    } else {
      array[ix] = val;   //throws IndexOutOfBoundsException if ix is faulty.
    }
  }
  

  void setSize(int newSize){
    if(newSize < size){
      
    } else {
      int blocksize = (1<<bit1) - (1<<bit2);
      if(newSize <= blocksize){
       
        if(array == null){
          array = new int[newSize];
        } else {
          int[] oldArray = array;
          array = new int[newSize];
          System.arraycopy(oldArray, 0, array, 0, oldArray.length);   
        }
      } else {
        DivideJc size1 = DivideJc.divLess2pow(newSize, bit1, bit2);
        blocks = new IntegerBlockArray[blocksize];
      }
      size = newSize;
    }
  }
  
  
  
  /**Searches the value in the arrays and returns the index of that value, which is equal
   * or near lesser than the requested one. It assumes that all values are stored in ascending order.
   * @param val
   * @param max
   * @return
   */
  public int binarySearch(int val, int max){
    int low = 0;
    int high = max - 1;
    while (low <= high) 
    {
      int mid = (low + high) >> 1;
      int midVal = get(mid);
      if (midVal < val)
      { low = mid + 1;
      }
      else if(midVal > val)
      { high = mid - 1;  
      }
      else  //exact found.
      { return mid;  
      }
    }
    return -(low + 1);  // key not found.
  }



  /**Searches the value in the arrays and returns the index of the first occurence of that value, which is equal
   * or near lesser than the requested one. It assumes that all values are stored in ascending order,
   * but it is possible to have one value more as one time.
   * @param val
   * @param max
   * @return
   */
  public int binarySearchFirstKey(int val, int max){
    int low = 0;
    int high = max - 1;
    int mid = 0;
    boolean equal = false;
    while (low <= high) 
    {
      mid = (low + high) >> 1;
      int midVal = get(mid);
      if (midVal < val)
      { low = mid + 1;
      }
      else  // dont check : if(midVal > val) because search always to left to found the leftest position
      { high = mid - 1;  
        equal = equal || midVal > val;  //one time equal set, it remain set.
      }
    }
    if(equal) return low > mid ? low : mid;  //one time found, then it is low or mid 
    return -(low + 1);  // key not found.
  }



  public static void main(String[] args){
    IntegerBlockArray array = new IntegerBlockArray(5000);
    array.set(234, 0xabcd);
    array.set(4690, 0x4690);
    int val = array.get(234);
    assert(val == 0xabcd);
    val = array.get(4690);
    assert(val == 0x4690);
    val = array.get(2048);
    assert(val == 0);
    val = array.get(2047);
    assert(val == 0);
  }


}
