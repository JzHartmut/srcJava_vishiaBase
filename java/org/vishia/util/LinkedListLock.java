package org.vishia.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

/**This class enhances {@link LinkedList} with the capability of a {@link #lock()} to detect and prevent {@link java.util.ConcurrentModificationException}-
 * Problem is, that a ConcurrentModificationException is thrown only on the iterator, and not on the causing {@link #add(Object)}.
 * Yet it is not finished but ready to use for {@link #add(Object)}, because all other operations to add need to be overridden too. 
 * <ul>
 * <li>Yet only {@link #add(Object)} is overridden, throws an exception if {@link #lock()} 
 *   because this would cause later a {@link java.util.ConcurrentModificationException} detected by the Iterator.
 * <li>Before get an {@link LinkedList#iterator()} (also in 'for(elemem:list)') call {@link #lock()}
 * <li>After it, call {@link #unlock()}  
 * <_ul>>
 * @param <T>
 */
public class LinkedListLock<T> extends LinkedList<T> {

  private static final long serialVersionUID = 1L;

  /**If true then add should throw an exception because an iterator is active.
   */
  boolean bLock;
  
  public LinkedListLock() {
    super();
  }

  public LinkedListLock(Collection<? extends T> c) {
    super(c);
  }

  
  /**Locks the list to use it with an #iterator()
   * @return
   */
  public boolean lock() { 
    boolean bLock = this.bLock; this.bLock = true; 
    return bLock; 
  }
  
  /**This is necessary after finish the iteration because else, {@link #add(Object)} is not possible.
   * 
   */
  public void unlock() {
    if(!this.bLock) {
      throw new IllegalStateException("unlock():List is not locked");
    }
    this.bLock = false;
  }

  /**Overridden form of {@link LinkedList#add(Object)} throws with IllegalstateException if called under {@link #lock()}
   *
   */
  @Override public boolean add(T d) {
    if(this.bLock) {
      throw new IllegalStateException("List is locked");
    }
    return super.add(d);
  }
  
  
  /**Overridden operator locks, can set here a breakpoint to see whether lock was nnot set before, also to insert the correct {@link #unlock()}
   *
   */
  @Override public Iterator<T> iterator() {
    if(!this.bLock) {
      lock();                                    //<<<<====== set here a breakpoint to monitor the lock before.
    }
    return super.iterator();
  }
  
}
