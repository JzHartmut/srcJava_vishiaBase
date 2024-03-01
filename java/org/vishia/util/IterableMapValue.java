package org.vishia.util;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;


/**This class prepares an {@link Map#entrySet()} to an {@link Iterable} over the values.
 * The key is not used, but the key determines the order of the iteration.
 * But the key have to be of type String. 
 * <br>Note: You can also use {@link Map#values()} to get a value iterator. 
 * <br>This class is not necessary. But one can see how an iterator works.
 * @author Hartmut Schorrig
 * 
 * @param <T> Any value type
 * @since 2023-12-08 works with null as argument for a Map with {@link #IterableMapValue(Map)}.
 *   then {@link #hasNext()} returns false. This is sensible if a Map is not created, but this should not be checked.
 */
public class IterableMapValue<T> implements IterableIterator<T> {

    private Iterator<Map.Entry<String , T>> set;
    
    
    
    
    /**
     * @param map It is admissible that the set is null. Then {@link #hasNext()} returns false.
     */
    public IterableMapValue(Map<String, T> map) {
          this.set = map ==null ? null : map.entrySet().iterator();
    }



    /**
     * @param set It is admissible that the set is null. Then {@link #hasNext()} returns false.
     */
    public IterableMapValue(Set<Map.Entry<String, T>> set) {
          this.set = set ==null ? null : set.iterator();
    }



    @Override public boolean hasNext() {
      return this.set !=null && this.set.hasNext();
    }



    @Override
    public T next() {
      return set == null ? null : this.set.next().getValue();
    }



    @Override
    public Iterator<T> iterator() {
      return this;
    }

}
