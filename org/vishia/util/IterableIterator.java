package org.vishia.util;

import java.util.Iterator;

/**This interface implements both a {@link Iterator} and a {@link Iterable}. 
 * It means the routines {@link Iterator#hasNext()} etc. are able to call
 * and {@link Iterable#iterator()} is able to call, which returns this.
 * In this kind the method can be used in a for-container-loop.
 * @param <T>
 */
public interface IterableIterator<T> extends Iterator<T>, Iterable<T>{}

