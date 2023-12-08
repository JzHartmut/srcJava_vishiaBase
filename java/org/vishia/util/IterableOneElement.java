package org.vishia.util;

import java.util.Iterator;

/**This is a helper class to use one given element in an iterator quest.
 * The possible user pattern is:
 * <pre>
  //Often there is only one action (Operation + output event), then this aggregation is used.
  //It is null if {@link #transitions} is given, or if no action exists.
  MyType element;
 
  //If more as one action is associated to the entry of the dstState, then this list is used and {@link #transition} is null.
  List<MyType> elements;
  //...
  //Iterable works also for one given element:
  Iterable<MyType> iter = (element !=null ? new IteratorOneElement(element) : elements;
  for(Element e : iter) { ....} 
 * </pre>
 * @author hartmut
 *
 * @param <T>
 */
public class IterableOneElement<T> implements Iterable<T> {

  
  private static class IteratorOneElement<T>  implements Iterator<T> {
    private T elem;
    
    public IteratorOneElement(T elem) {
      this.elem = elem;
    }

    @Override public boolean hasNext () {
      return this.elem !=null;
    }

    @Override public T next () {
      T ret = this.elem;
      this.elem = null;
      return ret;
    }

  }

  private IteratorOneElement<T> iter;
  
  public IterableOneElement(T elem) {
    this.iter = new IteratorOneElement<T>(elem);
  }

  @Override public Iterator<T> iterator () { return this.iter; }


}
