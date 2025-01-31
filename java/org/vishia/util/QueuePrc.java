package org.vishia.util;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**Helper class to handle with queues.
 * Basically, it prevents add to queue on already existing object in the queue.
 * @param <T>
 */
public abstract class QueuePrc<T> {

  
  List<T> queue = new LinkedList<>();
  
  Map<String, T> idxinQueue = new TreeMap<>();
  
  
  /**Should be defined by usage. This operation should build a key
   * which determines the functional same instance which may be already in queue
   * to prevent multiple add to queue.
   * @param obj
   * @return String as key.
   */
  public abstract String key (T obj); 
  
  
  public QueuePrc () {}
  
  /**Adds an object to the queue and checks before whether it is contained already.
   * The key to check contained is built with {@link #key(Object)}.
   * This should be unique to identify the same instance.
   * @param pos 0 on begin, -1 on end
   * @param obj to add
   * @return null if ok, else the already added instance with same key.
   */
  public T addToQueue(int pos, T obj) {
    String key = key(obj);
    T exist = this.idxinQueue.get(key);
    if(exist !=null) { return exist; }
    else {
      this.idxinQueue.put(key, obj);
      if(pos <0) this.queue.add(obj); else this.queue.add(pos, obj);
      assert(this.idxinQueue.size()==this.queue.size());
      return null;
    }
  }
  
  
  /**Gets the next Object from begin of the queue
   * and removes it also from the existence check.
   * It means, later the same obj can be already added again in the queue.
   * @return null if the queue is empty.
   */
  public T getFromQueue () {
    if(this.queue.size() == 0) {
      assert(this.idxinQueue.size()==0);
      return null;
    }
    else {
      T obj = this.queue.remove(0);
      String key = key(obj);
      T exist = this.idxinQueue.remove(key);
      assert(exist == obj);
      return exist;
    }
  }
  
  
}
