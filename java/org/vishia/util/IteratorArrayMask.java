package org.vishia.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**An instance of this class iterates over an array and returns elements which are marked with the mask.
 * The mask is a 1-of-64 selection of array elements.
 * 
 * @author Hartmut Schorrig
 *
 * @param <T> Any type of elements in the array.
 */
public class IteratorArrayMask<T> implements Iterator<T>, Iterable<T> {

  int ix = -1;
  
  final T[] array;
  
  final long mask;
  
  boolean bExecHasNext = false;

  /**Initializes with given mask.
   * @param array Any array with proper type
   * @param mask selection which elements should be returned 1-of-64
   */
  public IteratorArrayMask(T[] array, long mask) {
    this.array = array;
    this.mask = mask;
  }

  @Override
  public boolean hasNext() {
    this.bExecHasNext = true;
    if(this.array == null) { return false; }
    while(++this.ix < this.array.length) {
      if( (this.mask & (1<< this.ix)) !=0) {
        return true;  //ixEvout is adjusted
      }
    }
    this.ix = -1;
    return false;
  }

  @Override
  public T next() {
    if(!this.bExecHasNext) {
      hasNext();
    }
    this.bExecHasNext = false;
    if(this.ix <0) throw new NoSuchElementException();  //faulty next without hasNext==true
    else return this.array[this.ix];
  }

  @Override
  public Iterator<T> iterator() {
    return this;
  }
}

