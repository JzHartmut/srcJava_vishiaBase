/****************************************************************************
 * Copyright/Copyleft:
 *
 * For this source the LGPL Lesser General Public License,
 * published by the Free Software Foundation is valid.
 * It means:
 * 1) You can use this source without any restriction for any desired purpose.
 * 2) You can redistribute copies of this source to everybody.
 * 3) Every user of this source, also the user of redistribute copies
 *    with or without payment, must accept this license for further using.
 * 4) But the LPGL is not appropriate for a whole software product,
 *    if this source is only a part of them. It means, the user
 *    must publish this part of source,
 *    but don't need to publish the whole source of the own product.
 * 5) You can study and modify (improve) this source
 *    for own using or for redistribution, but you have to license the
 *    modified sources likewise under this LGPL Lesser General Public License.
 *    You mustn't delete this Copyright/Copyleft inscription in this source file.
 *
 * @author Hartmut Schorrig: hartmut.schorrig@vishia.de, www.vishia.org
 * @version 0.93 2011-01-05  (year-month-day)
 *******************************************************************************/ 
package org.vishia.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

import org.vishia.bridgeC.AllocInBlock;


/* Created: Hartmut Schorrig, ca. 2007-06
 * Changed:
 * 2009-05-08 Hartmut corr/bugfix: IteratorImpl(IndexMultiTableInteger<Type> firstTable, int startKey) used in iterator(startkey):
 *                    If a non-exakt start key is found, the iterator starts from the key after it. 
 *                    If no data are available, hasnext() returns false now. TODO test if one hyperTable contains the same key as the next, some more same keys!
 * 2009-04-28 Hartmut new: iterator(key) to start the iterator from a current position. It is the first position with the given key. 
 *                    corr: Some empty methods are signed with xxxName, this methods were come from planned but not implemented interface
 *                    meditated: implement ListInterface instead Interface to supply getPrevious() to iterate starting from a key forward and backward.
 *                    corr: binarySearch self implemented, regarding the first occurrence of a key.
 * 2009-04-26 Hartmut meditated: Several key types in one file isn't good! The search routines are optimal only if the key type is fix.
 *                                There are some adequate classes necessary for int, long keys.
 *                    docu
 *                    chg: Using of class AllocInBlock to get the size of a block.
 *                    corr: Now more deepness as 2 tables is programmed and tested. Older versions support only 2 tables.
 *                          It was 1000000 entries max. with a table size of 1000.
 * 2009-03-01 Hartmut new: IndexMultiTableInteger(int size, char type): in preparation of using, functionality not ready yet.
 *                    planned: Type may be int, long, String, size are able to choose. The arrays should be assigned not as embedded instances in C. 
 *                    new: method get() does anything, not tested in all cases.

 */

/**This class contains sorted references of objects with an integer key 
 * in tables of a limited size, but more as one table (a tree) if necessary.
 * This class contains the table of objects and the table of key of type integer. 
 * The tables are simple arrays of a fix size.
 * <br>
 * An instance of this class may be either a hyper table which have some children, 
 * or it is a end-table of keys and its references.
 * A end table may have a parent, which is a hyper table, and some sibling instances.
 * If there isn't so, only the end-table exists. It is, if the number of objects is less than 
 * the max capacity of one table, typical for less data. 
 * In that case, the data structure is very simple. Commonly a search process should regard 
 * the tree of tables. But the tree is not deep, because it is a square function. 
 * With a table of 100 entries, a 2-ary tree may contain up to 10000.
 * <br>
 * Objects with the same key can stored more as one time.
 *   
 * <br> 
 * This concept have some advantages:
 * <ul>
 * <li>A system of simple arrays for the key and a parallel array of the associated object
 *     allows fast search using java.util.Arrays.binarysearch(int[], int). 
 *     It is a simple algorithm. Only less memory objects are needed. 
 *     It doesn't need nodes for TreeMap etc. 
 *     It is a simple layout for fast embedded control applications. 
 * <li>But using only one array of keys and one array of objects, 
 *   <ul>
 *   <li>first, the number of objects are limited or a large portion of memory should be provided,
 *   <li>second, if a new object is inserted, and the amount of objects is large, 
 *       a larger calculation time of System.arraycopy() would be necessary.
 *   </ul>
 * <li>Therefore the portioning in more as one table needs no large memory objects. 
 *     If the number of objects is increased, a additional new memory object with limited size 
 *     is necessary - no resize of existing object.
 * <li>The calculation time for insertion an object is limited because one table is limited,
 *     also if the number of objects is large.
 * <li>If thread safety is necessary, only that table should be locked or exclusive copied, 
 *     which is touched, for a limited time too. So the number of clashes is less. 
 *     In the time this class doesn't support thread safety, but it is planned in two ways, 
 *     using synchronized and using the lock free atomic mechanism. 
 * <li>Using lock free programming, the change of data should be done in an copy of the table.
 *     This copy have less memory size.
 * <li>The limited number of memory space is convenient for the application in fast real time systems.
 * </ul>                     
 * 
 * @author Hartmut Schorrig
 *
 * @param <Type>
 */
public class IndexMultiTableInteger<Type> implements Map<Integer,Type>, Iterable<Type>
{
  /**The maximal nr of elements in a block, maximal value of sizeBlock.
   * It is the same value as obj.length or key.length. */
  private final static int maxBlock = AllocInBlock.restSizeBlock(IndexMultiTableInteger.class, 1000) / 8; //C: 8=sizeof(int) + sizeof(Object*) 

  /**actual number of objects stored in this table. */
  private int sizeBlock;
  
  /**True, than {@link #obj} contains instances of this class too. */
  private boolean isHyperBlock;
  
  /**modification access counter for Iterator. */
  @SuppressWarnings("unused")
  private int modcount;
  
  /**Array of objects appropritate to the keys. */
  private final Object[] obj = new Object[maxBlock];
  
  /**Array of keys, there are sorted ascending. The same key can occure some times. */ 
  private final int[] key = new int[maxBlock];
  
  /**The parent if it is a child table. */
  private IndexMultiTableInteger<Type> parent;
  
  
 
  
  
  /**This class is the Iterator for the outer class. Every {@link #iterator()}-call
   * produces one instance. It is possible to create some instances simultaneously.
   * @author HSchorrig
   *
   * @param <Type>
   */
  private static class IteratorImpl<Type> implements Iterator<Type>
  {

    /**The helper contains the values of the iterator. Because there are a tree of tables,
     * the current IteratorHelper of the current table depths is referenced.
     * A IteratorHelper contains a parent and child reference. If the end of table
     * of the current depth level is reached, the parent of the current helper is referenced here
     * than, the next table is got, than the child is initialized and referenced here.  
     * 
     */
    private IndexMultiTableInteger.IteratorHelper helper;
    
    /**True if hasNext. The value is valid only if {@link bHasNextProcessed} is true.*/
    private boolean bHasNext = false;
      
    /**true if hasnext-test is done. */
    private boolean bHasNextProcessed = false;
    
    @SuppressWarnings("unused")
    private int modcount;
    
    /**Only for test. */
    private int lastkey; 
    
    private IteratorImpl(IndexMultiTableInteger<Type> firstTable)
    { helper = new IteratorHelper(null);
      helper.table = firstTable;
      helper.idx = -1;
      lastkey = Integer.MIN_VALUE;
    }
    
    
    /**Ctor for the Iterator with a range.
     * @param firstTable
     * @param startKey
     * @param endKey
     */
    private IteratorImpl(IndexMultiTableInteger<Type> firstTable, int startKey)
    { helper = new IteratorHelper(null);
      helper.table = firstTable;
      helper.idx = -1;
      lastkey = Integer.MIN_VALUE;
      while(helper.table.isHyperBlock)
      { //call it recursively with sub index.
        int idx = binarySearchFirstKey(helper.table.key, 0, helper.table.sizeBlock, startKey); //, sizeBlock, key1);
        if(idx < 0)
        { /**an non exact found, accept it.
           * use the table with the key lesser than the requested key
           */
          idx = -idx-2; //insertion point -1 
          if(idx < 0)
          { /**Use the first table if the first key in the first table is greater. */
            idx = 0; 
          }
        }
        //idx -=1;  //?
        @SuppressWarnings("unchecked")
      	IndexMultiTableInteger<Type> childTable = (IndexMultiTableInteger<Type>)helper.table.obj[helper.idx];
        helper.idx = idx;
        helper.child = new IteratorHelper(helper); 
        
        helper.child.table = childTable;
        helper = helper.child;  //use the sub-table to iterate.          
      }
      int idx = binarySearchFirstKey(helper.table.key, 0, helper.table.sizeBlock,  startKey); //, sizeBlock, key1);
      if(idx < 0)
      { /**an non exact found, accept it.
         * start from the element with first key greater than the requested key
         */
        idx = -idx-1;  
      }
      helper.idx = idx;
      /**next_i() shouldn't called, because the helper.idx is set with first occurrence. */
      bHasNextProcessed = true;
      /**next() returns true always, except if the idx is 0 and the table contains nothing. */
      bHasNext =  idx < helper.table.sizeBlock;
    }
    
    
    public boolean hasNext()
    { if(!bHasNextProcessed)
      { next_i();  //call of next set bHasNext!
      }
      return bHasNext;
    }

    
    
    
    
    @SuppressWarnings("unchecked")
    public Type next()
    { if(!bHasNextProcessed)
      {  next_i();
      }
      if(bHasNext)
      { bHasNextProcessed = false;  //call it at next access!
        IndexMultiTableInteger<?> table = (IndexMultiTableInteger<?>)helper.table;
        assert(table.key[helper.idx] >= lastkey);  //test
        if(table.key[helper.idx] < lastkey) throw new RuntimeException("assert");
        if(table.key[helper.idx] < lastkey)
          stop();
        lastkey = table.key[helper.idx];
        return (Type)table.obj[helper.idx];
      }
      else return null;
    }

    
    
    /**executes the next(), on entry {@link bHasNextProcessed} is false.
     * If the table is a child table and its end is reached, this routine is called recursively
     * with the now current parent, typical the parent contains a child table
     * because the table is a hyper table. Than the child helper is initialized
     * and reused, and this routine will be called a third time, now with the new child:
     * <pre>
     * child: end of table; parent: next table; child: test next table.
     * </pre>
     * If the tree of tables is deeper than two, and the end of a child and also the parent table
     * is reached, this routine is called recursively more as three times.
     * The maximum of recursively call depends on the deepness of the table tree.
     */
    @SuppressWarnings("unchecked")
    private void next_i()
    {
      bHasNext = ++helper.idx < helper.table.sizeBlock;  //next in current table.
      if(bHasNext)
      { if(helper.table.isHyperBlock)
        { //
          IndexMultiTableInteger<Type> childTable = (IndexMultiTableInteger<Type>)helper.table.obj[helper.idx];
          if(helper.child == null)
          { //no child yet. later reuse the instance of child.
            helper.child = new IteratorHelper(helper); 
          }
          helper.child.idx = -1;  //increment as first action.
          helper.child.table = childTable;
          helper = helper.child;  //use the sub-table to iterate.          
        }
        else
        { //else: bHasNext is true.
          bHasNextProcessed = true;
        }
      }
      else
      { if(helper.parentIter != null)
        { //no next, but it is a sub-table. This sub-table is ended.
          //a next obj may be exist in the sibling table.
          helper.table = null;  //the child helper is unused now.
          helper = helper.parentIter; //to top of IteratorHelper, test there.
          /*Because bHasNextProcessed is false, this routine is called recursively, see method description. */
        }
        else
        { //else: bHasNext is false, it is the end.
          bHasNextProcessed = true;
        }
      }
      if(!bHasNextProcessed)
      { next_i();
      }
    }
    
    
    
    public void remove()
    {
      // TODO Auto-generated method stub
      
    }

    private void stop()
    { //debug
    }
    
    
  }

  
  
  
  /**This class contains the data for a {@link IndexMultiTableInteger.IteratorImpl}
   * for one table.
   * 
   *
   */ 
  private static class IteratorHelper
  {
    /**If not null, this helper is associated to a deeper level of table, the parent
     * contains the iterator value of the higher table.*/
    private final IteratorHelper parentIter;
    
    /**If not null, an either an empty instance for a deeper level of tables is allocated already 
     * or the child is used actual. The child is used, if the child or its child 
     * is the current IteratorHelper stored on {@link IteratorImpl#helper}. */ 
    private IteratorHelper child;
    
    /**Current index in the associated table. */ 
    private int idx;
    
    /**The associated table, null if the instance is not used yet. */
    IndexMultiTableInteger<?> table;
    
    IteratorHelper(IteratorHelper parentIter)
    { this.parentIter = parentIter;
      this.table = null;
      idx = -1;
    }
  }
  
  
  
  /**constructs an empty instance without data. */
  public IndexMultiTableInteger()
  { //this(1000, 'I');
    for(int idx = 0; idx < maxBlock; idx++){ key[idx] = Integer.MAX_VALUE; }
    sizeBlock = 0;
  }
  

  /**constructs an empty instance without data with a given size and key type. 
   * @param size The size of one table.
   * @param type one of char I L or s for int, long or String key.
   */
  /*
  public IndexMultiTableInteger(int size, char type)
  { //TODO: allocate the fields etc. with given size. 
    for(int idx = 0; idx < maxBlock; idx++){ key[idx] = Integer.MAX_VALUE; }
    sizeBlock = 0;
    
  }
  */
  
  
  
  
  /**Put a object in the table. The key may be ambiguous, a new object with the same key is placed
   * after an containing object with this key. If the table is full, a new table will be created internally.
   *  
   * 
   */
  @SuppressWarnings("unchecked")
  public Type put(Integer arg0, Type obj1)
  { Type lastObj = null;
    int key1 = arg0.intValue();
    //place object with same key after the last object with the same key.
    int idx = Arrays.binarySearch(key, key1); //, sizeBlock, key1);
    if(key1 == -37831)
      stop();
    if(idx < 0)
    { idx = -idx-1;  //NOTE: sortin after that map, which index starts with equal or lesser index.
    }
    else
    { //if key1 is found, sorting after the last value with that index.
      while(idx <sizeBlock && key[idx] == key1)
      { idx+=1; 
      } 
    }
    if(isHyperBlock)
    { //call it recursively with sub index.
      //the block with the range 
      idx -=1;
      IndexMultiTableInteger<Type> child;
      if(idx<0)
      { //a index less than the first block is getted.
        //sortin it in the first block.
        idx = 0;
        IndexMultiTableInteger<Type> parents = this;
        while(parents != null)
        { if(key1 < key[0])
          { key[0] = key1; //correct the key, key1 will be the less of child.
          }
          parents = parents.parent;
        }
        //NOTE: if a new child will be created, the key[0] is set with new childs key.
      }
      if(idx < sizeBlock)
      { if(! (obj[idx] instanceof IndexMultiTableInteger))
          stop();
        child = ((IndexMultiTableInteger<Type>)(obj[idx]));
      }
      else
      { //index after the last block.
        child = null;
        stop();
      }
      if(child.sizeBlock == maxBlock)
      { //this child is full, divide it before using
        //int idxH = maxBlock / 2;
        if(child.isHyperBlock)
          stop();
        int idxInChild = Arrays.binarySearch(child.key, key1);
        if(idxInChild <0){ idxInChild = -idxInChild -1; }
        else{ while(idxInChild <sizeBlock && key[idxInChild] == key1){ idxInChild+=1;}}
        
        IndexMultiTableInteger<Type> right;
        
        right = new IndexMultiTableInteger<Type>();
        if(child.isHyperBlock)
        { int key0right = separateIn2arrays(child,child, right);
          sortin(idx+1, right.key[0], right);
          if(key1 >= key0right)
          { right.put(arg0, obj1);
          }
          else
          { child.put(arg0, obj1);
          }
        }
        else
        {
          sortInSeparated2arrays(idxInChild, key1, obj1, child, child, right);
          sortin(idx+1, right.key[0], right);
        }
      }
      else 
      { //the child has space.
        child.put(arg0, obj1); 
      }
    }
    else
    {
      if(idx <0)
      { //not found
        //lastObj = null;
        idx = -idx -1;
      }
      else
      { //found
        //lastObj = (Type)obj[idx];
        //idx +=1;  
      }
      sortin(idx, key1, obj1);  //idx+1 because sortin after found position.            
    }
    check();
    return lastObj;
  }

  

  
  
  
  
  private void sortin(int idx, int key1, Object obj1)
  { if(sizeBlock == maxBlock)
    { //divide the block:
      if(isHyperBlock)
        stop();
      if(parent != null)
      { //it has a hyper block, use it!
        //IndexInteger<Type> hyper = parent; 
        stop();
      }
      else
      { //divide the content of the current block in 2 blocks.
        IndexMultiTableInteger<Type> left = new IndexMultiTableInteger<Type>();
        IndexMultiTableInteger<Type> right = new IndexMultiTableInteger<Type>();
        left.parent = this; right.parent=this;
        sortInSeparated2arrays(idx, key1, obj1, this, left, right);
        //the current block is now a hyper block.
        this.isHyperBlock = true;
        obj[0] = left;
        obj[1] = right;
        key[0] = left.key[0]; //Integer.MIN_VALUE;  //because it is possible to sort in lesser keys.
        key[1] = right.key[0];
        for(int idxFill = 2; idxFill < maxBlock; idxFill++)
        { key[idxFill] = Integer.MAX_VALUE; 
          obj[idxFill] = null;
        }
        sizeBlock = 2;
        left.check();right.check();
      }
        
    }
    else
    { if(idx < sizeBlock)
      { System.arraycopy(key, idx, key, idx+1, sizeBlock-idx);
        System.arraycopy(obj, idx, obj, idx+1, sizeBlock-idx);
      }
      sizeBlock +=1;
      key[idx] = key1;
      obj[idx] = obj1;
    }
    check();
  }
  
  
  
  
  
  /**separates the src into two arrays with the half size and sort in the object.
   * 
   * @param idx Primordially index of the obj in the src array. 
   * @param key1 The key value
   * @param obj1 The object
   * @param src The src table
   * @param left The left table. It may be the same as src.
   * @param right The right table.
   */
  private static void sortInSeparated2arrays
  ( final int idx, int key1, Object obj1
  , IndexMultiTableInteger<?> src 
  , IndexMultiTableInteger<?> left 
  , IndexMultiTableInteger<?> right
  )
  {
    left.isHyperBlock = src.isHyperBlock; right.isHyperBlock = src.isHyperBlock;  //copy it. 
            
    final int idxH = maxBlock / 2;
    if(idx < idxH)
    { /**sortin the obj1 in the left table. */
      System.arraycopy(src.key, idxH, right.key, 0, src.sizeBlock - idxH);
      System.arraycopy(src.obj, idxH, right.obj, 0, src.sizeBlock - idxH);

      System.arraycopy(src.key, 0, left.key, 0, idx);
      System.arraycopy(src.obj, 0, left.obj, 0, idx);
      System.arraycopy(src.key, idx, left.key, idx+1, idxH-idx);
      System.arraycopy(src.obj, idx, left.obj, idx+1, idxH-idx);
      left.key[idx] = key1;
      left.obj[idx] = obj1;

    }  
    else
    { /**sortin the obj1 in the right table. */
      System.arraycopy(src.key, 0, left.key, 0, idxH);
      System.arraycopy(src.obj, 0, left.obj, 0, idxH);
      
      int idxR = idx-idxH; //valid for right block.
      System.arraycopy(src.key, idxH, right.key, 0, idxR);
      System.arraycopy(src.obj, idxH, right.obj, 0, idxR);
      System.arraycopy(src.key, idx, right.key, idxR+1, src.sizeBlock - idx);
      System.arraycopy(src.obj, idx, right.obj, idxR+1, src.sizeBlock - idx);
      right.key[idxR] = key1;
      right.obj[idxR] = obj1;
    }
    /**Set the sizeBlock and clear the content after copy of all block data,
     * because it is possible that src is equal left or right!
     */
    if(idx < idxH)
    { left.sizeBlock = idxH +1;
      right.sizeBlock = maxBlock - idxH;
    }
    else
    { left.sizeBlock = idxH;
      right.sizeBlock = maxBlock - idxH +1;
    }
    for(int idxFill = left.sizeBlock; idxFill < maxBlock; idxFill++)
    { left.key[idxFill] = Integer.MAX_VALUE; 
      left.obj[idxFill] = null;
    }
    for(int idxFill = right.sizeBlock; idxFill < maxBlock; idxFill++)
    { right.key[idxFill] = Integer.MAX_VALUE; 
      right.obj[idxFill] = null;
    }
    src.check();
    left.check();
    right.check();
  }





  /**separates the src into two arrays with the half size .
   * 
   * @param src The src table
   * @param left The left table. It may be the same as src.
   * @param right The right table.
   * @return the first key of the right table.
   */
  private static int separateIn2arrays
  ( IndexMultiTableInteger<?> src 
  , IndexMultiTableInteger<?> left 
  , IndexMultiTableInteger<?> right
  )
  {
    left.isHyperBlock = src.isHyperBlock; right.isHyperBlock = src.isHyperBlock;  //copy it. 
            
    final int idxH = maxBlock / 2;
  
    System.arraycopy(src.key, idxH, right.key, 0, src.sizeBlock - idxH);
    System.arraycopy(src.obj, idxH, right.obj, 0, src.sizeBlock - idxH);

    System.arraycopy(src.key, 0, left.key, 0, idxH);
    System.arraycopy(src.obj, 0, left.obj, 0, idxH);
    /**Set the sizeBlock and clear the content after copy of all block data,
     * because it is possible that src is equal left or right!
     */
    left.sizeBlock = idxH;
    for(int idxFill = idxH; idxFill < maxBlock; idxFill++)
    { left.key[idxFill] = Integer.MAX_VALUE; 
      left.obj[idxFill] = null;
    }
    right.sizeBlock = maxBlock - idxH;
    for(int idxFill = right.sizeBlock; idxFill < maxBlock; idxFill++)
    { right.key[idxFill] = Integer.MAX_VALUE; 
      right.obj[idxFill] = null;
    }
    src.check();
    left.check();
    right.check();
    return right.key[0];
  }







  /**Delete all content. 
   * @see java.util.Map#clear()
   */
  public void clear()
  {
    for(int ix=0; ix<sizeBlock; ix++){
      if(isHyperBlock){ 
        @SuppressWarnings("unchecked")
        IndexMultiTableInteger subTable = (IndexMultiTableInteger)obj[ix];
        subTable.clear();
      }
      obj[ix] = null;
      key[ix] = Integer.MAX_VALUE; 
    }
    sizeBlock = 0;
    isHyperBlock = false;
  }







  public boolean containsKey(Object arg0)
  {
    // TODO Auto-generated method stub
    return false;
  }







  public boolean containsValue(Object arg0)
  {
    // TODO Auto-generated method stub
    return false;
  }







  public Set<java.util.Map.Entry<Integer, Type>> entrySet()
  {
    // TODO Auto-generated method stub
    return null;
  }







  //public Type get(Object arg0)
  @SuppressWarnings({ "unchecked" })
  public Type get(Object key2)
  { Type lastObj = null;
    int key1 = ((Integer)(key2)).intValue();
    //place object with same key after the last object with the same key.
    int idx = Arrays.binarySearch(key, key1); //, sizeBlock, key1);
    if(isHyperBlock)
    { //call it recursively with sub index.
      if(idx < 0)
      { //an non exact found index is possible if it is an Hyper block.
        idx = -idx-1;  //NOTE: sortin after that map, which index starts with equal or lesser index.
      }
    
      idx -=1;
      IndexMultiTableInteger<Type> child;
      if(idx<0)
      { lastObj = null;
      }
      else if(idx < sizeBlock)
      { child = ((IndexMultiTableInteger<Type>)(obj[idx]));
        //the child has space.
        lastObj = child.get(key2); 
      }
    }
    else
    { if(idx >=0)
      { lastObj = (Type)obj[idx];
      }
      else  
      { //not found
        lastObj = null;
      }  
    }
    return lastObj;
  }







  public boolean isEmpty()
  {
    // TODO Auto-generated method stub
    return false;
  }







  public Set<Integer> keySet()
  {
    // TODO Auto-generated method stub
    return null;
  }







  public void putAll(Map<? extends Integer, ? extends Type> arg0)
  {
    // TODO Auto-generated method stub
    
  }












  public int size()
  {
    // TODO Auto-generated method stub
    return 0;
  }







  public Collection<Type> values()
  {
    // TODO Auto-generated method stub
    return null;
  }







  public boolean xxxadd(Type arg0)
  {
    // TODO Auto-generated method stub
    return false;
  }







  public void xxxadd(int arg0, Type arg1)
  {
    // TODO Auto-generated method stub
    
  }







  public boolean xxxaddAll(Collection<? extends Type> arg0)
  {
    // TODO Auto-generated method stub
    return false;
  }







  public boolean xxxaddAll(int arg0, Collection<? extends Type> arg1)
  {
    // TODO Auto-generated method stub
    return false;
  }







  public boolean xxxcontains(Object arg0)
  {
    // TODO Auto-generated method stub
    return false;
  }







  public boolean xxxcontainsAll(Collection<?> arg0)
  {
    // TODO Auto-generated method stub
    return false;
  }







  public Type xxxget(int arg0)
  {
    // TODO Auto-generated method stub
    return null;
  }







  public int xxxindexOf(Object arg0)
  {
    // TODO Auto-generated method stub
    return 0;
  }







  public Iterator<Type> iterator()
  {
    return new IteratorImpl<Type>(this);
  }



  public Iterator<Type> iterator(int fromKey)
  {
    return new IteratorImpl<Type>(this, fromKey);
  }



  public int xxxlastIndexOf(Object arg0)
  {
    // TODO Auto-generated method stub
    return 0;
  }







  public ListIterator<Type> xxxlistIterator()
  {
    // TODO Auto-generated method stub
    return null;
  }







  public ListIterator<Type> xxxlistIterator(int arg0)
  {
    // TODO Auto-generated method stub
    return null;
  }







  public Type xxxremove(int arg0)
  {
    // TODO Auto-generated method stub
    return null;
  }







  public boolean xxxremoveAll(Collection<?> arg0)
  {
    // TODO Auto-generated method stub
    return false;
  }







  public boolean xxxretainAll(Collection<?> arg0)
  {
    // TODO Auto-generated method stub
    return false;
  }







  public Type xxxset(int arg0, Type arg1)
  {
    // TODO Auto-generated method stub
    return null;
  }







  public List<Type> xxxsubList(int arg0, int arg1)
  {
    // TODO Auto-generated method stub
    return null;
  }







  public Object[] xxxtoArray()
  {
    // TODO Auto-generated method stub
    return null;
  }












  public Type remove(Object arg0)
  {
    // TODO Auto-generated method stub
    return null;
  }
  
  
  
  void stop()
  { //debug
  }
  
  
  
  @SuppressWarnings("unchecked")
  void check()
  { boolean shouldCheck = false;
    if(shouldCheck)
    { if(sizeBlock >=1){ assert1(obj[0] != null); }
      for(int ii=1; ii < sizeBlock; ii++)
      { assert1(key[ii-1] <= key[ii]);
        assert1(obj[ii] != null);
        if(obj[ii] == null)
          stop();
      }
      if(isHyperBlock)
      { for(int ii=1; ii < sizeBlock; ii++)
        { assert1(key[ii] == ((IndexMultiTableInteger<Type>)obj[ii]).key[0]); 
        }
      }
      for(int ii=sizeBlock; ii < maxBlock; ii++)
      { assert1(key[ii] == Integer.MAX_VALUE);
        assert1(obj[ii] == null);
      }
    }  
  }
  
  
  void assert1(boolean cond)
  {
    if(!cond)
    { stop();
      throw new RuntimeException("assertiuon");
    }  
  }

  
  
  /**Binaray search of the element, which is the first with the given key.
   * The algorithm is copied from Arrays.binarySearch0(...) and modified.
   * @param a
   * @param fromIndex
   * @param toIndex
   * @param key
   * @return
   */
  private static int binarySearchFirstKey(int[] a, int fromIndex, int toIndex, int key) 
  {
    int low = fromIndex;
    int high = toIndex - 1;

    while (low <= high) 
    {
      int mid = (low + high) >> 1;
      int midVal = a[mid];
      int midValLeft = mid >fromIndex ? a[mid-1] : Integer.MIN_VALUE;  
      
      if (midVal < key)
      { low = mid + 1;
      }
      else 
      { if(midValLeft >= key)
        { high = mid - 1;  //search in left part also if key before mid is equal
        }
        else
        { return mid;  //midValLeft is lesser, than it is the first element with key!
        }
      }
    }
    return -(low + 1);  // key not found.
  }
  
  
  
  
}
