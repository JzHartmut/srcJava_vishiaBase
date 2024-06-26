package org.vishia.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class IteratorArray<T> implements IterableIterator<T> {

  int ix = -1;
  
  final T[] array;
  
  
  /**Set to true if {@link #hasNext()} was called, set to false on {@link #next()}.
   * If false, then {@link #next()} calls first {@link #hasNext()} to prepare the next access.
   * With this help {@link #next()} can be used without {@link #hasNext()}
   * which is proper due the interface definition. 
   */
  boolean bExecHasNext = false;

  /**Initializes with given mask.
   * @param array Any array with proper type
   * @param mask selection which elements should be returned 1-of-64
   */
  public IteratorArray(T[] array) {
    this.array = array;
  }

  @Override
  public boolean hasNext() {
    this.bExecHasNext = true;
    if(this.array == null) { 
      return false; 
    }
    else if(++this.ix < this.array.length) {
      return true;  
    }
    else {
      this.ix = -1;
      return false;
    }
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
