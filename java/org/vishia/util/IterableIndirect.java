package org.vishia.util;

import java.util.Iterator;

/**Iterator which returns an indirect instance because of a given {@link Iterable}.
 * The implementor have to override the {@link #conv(Object)} which converts from the 
 * original elements of the iterable to the return instance.
 * @author Hartmut Schorrig
 *
 * @param <Tsrc> Type of the original Iterable
 * @param <Tdst> Type of the indirect gotten instance
 */
public abstract class IterableIndirect<Tsrc, Tdst> implements Iterator<Tdst>, Iterable<Tdst>{

  /**Converts with given element to the expected result element.
   * @param src element of the given iterSrc of ctor
   * @return element which is expected.
   */
  public abstract Tdst conv(Tsrc src);
  
  
  
  int ix = -1;

  //final Object[] arraySrc;
  
  final Iterator<Tsrc> iterSrc;

  boolean bExecHasNext = false;

  /**Constructs with the given Iterable
   * @param iterSrc
   */
  public IterableIndirect(Iterable<Tsrc> iterSrc) {
    this.iterSrc = iterSrc.iterator();
  }

  @Override
  public boolean hasNext() {
    this.bExecHasNext = true;
    return this.iterSrc.hasNext();
  }

  @Override
  public Tdst next() {
    if(!this.bExecHasNext) {
      hasNext();
    }
    this.bExecHasNext = false;
    Tsrc e = this.iterSrc.next();
    return conv(e);
  }

  @Override
  public Iterator<Tdst> iterator() {
    return this;
  }

  
}
