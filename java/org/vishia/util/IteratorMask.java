package org.vishia.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class IteratorMask <T> implements Iterator<T>, Iterable<T> {

  int ix = -1;
  
  final Iterator<T> iterSrc;
  
  private T elem;
  
  final long mask;
  
  boolean bExecHasNext = false;

  /**Initializes with given mask.
   * @param array Any array with proper type
   * @param mask selection which elements should be returned 1-of-64
   */
  public IteratorMask(Iterator<T> iteratorSrc, long mask) {
    this.iterSrc = iteratorSrc;
    this.mask = mask;
  }

  @Override
  public boolean hasNext() {
    this.bExecHasNext = true;
    if(this.iterSrc == null) { return false; }
    while(this.iterSrc.hasNext()) {
      this.ix +=1;
      this.elem = this.iterSrc.next();
      if( (this.mask & (1<< this.ix)) !=0) {
        return true;  //ixEvout is adjusted
      }
    }
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
}
