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
  
  boolean bPreventRequeue;
  
  
  /**Should be defined by usage. This operation should build a key
   * which determines the functional same instance which may be already in queue
   * to prevent multiple add to queue.
   * @param obj
   * @return String as key.
   */
  public abstract String key (T obj); 
  
  
  /**Constructs
   * @param preventRequeue true means, later the same obj cannot be  added again in the queue.
   *   If {@link #addToQueue(int, Object)} is called with the same Object is prevented,
   *   till {@link #reset()} is called.
   */
  public QueuePrc (boolean preventRequeue) {this.bPreventRequeue = preventRequeue; }
  
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
    if(exist !=null) { 
      return exist; 
    } else {
      this.idxinQueue.put(key, obj);
      if(pos <0) this.queue.add(obj); else this.queue.add(pos, obj);
      assert(this.bPreventRequeue || this.idxinQueue.size()==this.queue.size());
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
      assert(this.bPreventRequeue || this.idxinQueue.size()==0);
      return null;
    }
    else {
      T obj = this.queue.remove(0);
      if(!this.bPreventRequeue) {
        String key = key(obj);
        T exist = this.idxinQueue.remove(key);
        assert(exist == obj);
      }
      return obj;
    }
  }
  
  
  
  /**Resets the queue, cleans, also for prevent requeue.
   * The queue is empty as after ctor.
   * @param preventRequeue
   */
  public void reset (boolean preventRequeue) {
    this.bPreventRequeue = preventRequeue; 
    this.idxinQueue.clear();
    this.queue.clear();
  }
  
  
}
