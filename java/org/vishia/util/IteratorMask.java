package org.vishia.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**This class helps to iterate ove a given Iterable can be an array, list etc
 * but with a given mask. All non mask elements are not returned.
 * The mask has 64 bits. It means the size is delimited to 64.
 * @author Hartmut Schorrig
 * @license LPGL do not remove this designations.
 * @since 2024-07-14 also an array is Iterable, using {@link #IteratorMask(Object[], long)}.
 *
 * @param <T>
 */
public class IteratorMask <T> implements IterableIterator<T> {

  private int ix = -1;
  
  final private Iterator<T> iterSrc;
  
  final private T[] array;
  
  private T elem;
  
  final private long mask;
  
  boolean bExecHasNext = false;

  /**Initializes with given mask.
   * @param array Any array with proper type
   * @param mask selection which elements should be returned 1-of-64
   */
  public IteratorMask(Iterator<T> iteratorSrc, long mask) {
    this.iterSrc = iteratorSrc;
    this.array = null;
    this.mask = mask;
  }

  public IteratorMask(Iterable<T> iterSrc, long mask) {
    this.iterSrc = iterSrc == null ? null: iterSrc.iterator();
    this.array = null;
    this.mask = mask;
  }

  public IteratorMask(T[] array, long mask) {
    this.iterSrc = null;
    this.array = array;
    this.mask = mask;
  }

  @Override
  public boolean hasNext() {
    this.bExecHasNext = true;
    if(this.iterSrc != null) {
      while(this.iterSrc.hasNext()) {
        this.ix +=1;
        this.elem = this.iterSrc.next();
        if( (this.mask & (1<< this.ix)) !=0) {
          return true;  //ixEvout is adjusted
        }
      }
    } else if(this.array !=null) {
      while(++this.ix < this.array.length) {
        if( (this.mask & (1<< this.ix)) !=0) {
          this.elem = this.array[this.ix];
          return true;  //ixEvout is adjusted
        }
      }
    }
    // lands here if no return in while.
    this.ix = -1; //important for ix=-1 on empty container.
    return false;
  }

  @Override
  public T next() {
    if(!this.bExecHasNext) {
      hasNext();
    }
    this.bExecHasNext = false;
    if(this.ix <0) throw new NoSuchElementException();  //faulty next without hasNext==true
    else return this.elem;
  }

  @Override
  public Iterator<T> iterator() {
    return this;
  }
  
  /**Gets the next ix of a set bit in the mask starting from given
   * For example if the mask is 0x21 then the first call getNextAssocIx(-1) returns 0,
   * the next call getNextAssocIx(0) returns 5, and the last call getNextAssocIx(5) returns then -1.
   * @param ix given ix, should start with -1 to detect 0 as first one
   * @return 0..63 for given, or -1 if nothing more found. 
   */
  public static int getNextAssocIx (int ix, long mask) {
    int ix1 = ix+1;
    if( (mask & (-1L << ix1))==0) {
      return -1;
    } else {
      while( ix1 < 64 && (mask & (1L<< ix1)) == 0 ) {
        ix1 +=1;                 // return 64 only if any is faulty
      }
      return ix1;
    }
  }

}
