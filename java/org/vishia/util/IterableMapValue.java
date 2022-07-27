package org.vishia.util;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;


/**This class prepares an {@link Map#entrySet()} to an {@link Iterable} over the values.
 * The key is not used, but the key determines the order of the iteration.
 * But the key have to be of type String. 
 * @author Hartmut Schorrig
 *
 * @param <T> Any value type
 */
public class IterableMapValue<T> implements Iterator<T>, Iterable<T> {

    private Iterator<Map.Entry<String , T>> set;
    
    
    
    
    //public IterableMapValue(Set<Map.Entry<? extends Object, T>> set) {
    public IterableMapValue(Set<Map.Entry<String, T>> set) {
          this.set = set.iterator();
    }



    @Override
    public boolean hasNext() {
      return this.set.hasNext();
    }



    @Override
    public T next() {
      return this.set.next().getValue();
    }



    @Override
    public Iterator<T> iterator() {
      return this;
    }

}
