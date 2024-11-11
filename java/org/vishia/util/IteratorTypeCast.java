package org.vishia.util;

import java.util.Iterator;

/**This class supports iteration over an iterable which has a base (super) type of the necessary type.
 * It is assumed that the elements have really the derived types.
 * The iterator delivers it as desired.
 * @throws ClassCastException if the Iterable has faulty instances (not instanceof Tdst). 
 * @author Hartmut Schorrig
 *
 * @param <Tdst>
 */
public class IteratorTypeCast<Tdst> implements IterableIterator<Tdst> {

  final private Iterable<?> iterable;
  
  final private Iterator<?> iterator;
  
  
  public IteratorTypeCast(Iterable<?> iter) {
    this.iterable = iter;
    this.iterator = this.iterable.iterator();
  }

  @Override public boolean hasNext () {
    return this.iterator.hasNext();
  }

  @Override public Tdst next () {
    @SuppressWarnings("unchecked") 
    Tdst ret = (Tdst) this.iterator.next();
    return ret;
  }

  @Override public Iterator<Tdst> iterator () {
    return this;
  }

  
  
}
