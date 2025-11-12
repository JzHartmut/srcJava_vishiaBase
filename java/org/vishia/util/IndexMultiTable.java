package org.vishia.util;

import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;




/**This class contains sorted references of objects (values) with a sorting key (usual String or Integer) 
 * in one or more tables of a defined size. 
 * <ul>
 * <li>This class can be used similar like {@link java.util.TreeMap}. 
 *   In opposite to a {@link java.util.TreeMap} the key need not be unique for all values.
 *   More as one value can have the same key. The method {@link #get(Object)} defined in 
 *   {@link java.util.Map} searches the first Object with the given key. 
 *   The method {@link #put(Comparable, Object)} replaces an existing value with the same key like defined in Map
 *   where the methods {@link #add(Comparable, Object)}, {@link #addBefore(Comparable, Object, Object)}
 *   and {@link #append(Comparable, Object)} puts the new value with an existing key beside the last existing one.  
 * <li>There is a method {@link #iterator(Comparable)} which starts on the first occurrence of the search key
 *   or after that key which is one lesser as the key.
 *   It iterates both to the end or backward of the whole collection. The user can check the key in the returned {@link java.util.Map.Entry}
 *   whether the key is proper. In this kind a sorted view of a part of the content can be done.
 * <li>The container consist of one or more instances of tables which have a limited size. If a table in one instance is filled,
 *   two new instances will be created in memory which contains the half amount of elements and the original
 *   instance is changed to a hyper table. The size of the instances are equal and less. It means, the search time
 *   is less and the memory requirement is constant. In this kind this class is able to use in C programming
 *   with a block heap of equal size blocks in an non-dynamic memory management system (long living
 *   fast real time system).    
 * </ul>
 * The inner class {@link Table} contains the table of objects and the table of key of type Comparable. 
 * The tables are simple arrays of a fix size.
 * <br>
 * An instance of Table class may be either a hyper table which have some children, 
 * or it is a end-table of keys and its references.
 * An end table may have a parent, which is a hyper table, and some sibling instances.
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
 *     allows fast search using a binary search algorithm. 
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
@SuppressWarnings("synthetic-access") 
public class IndexMultiTable<Key extends Comparable<Key>, Type> 
extends IndexMultiTable_Table<Key, Type>
implements Map<Key,Type>, Iterable<Type>  //TODO: , NavigableMap<Key, Type>
{
  
  /**Version, history and license.
   * <ul>
   * <li>2017-08-27 Hartmut bugfix: last change ... but if a key was searched which is lesser as the lesses first key, 
   *   then the aValues[0] should be used as start.  
   * <li>2017-08-22 Hartmut bugfix: {@link IteratorImpl} has start from the next table instead hyper table with lesser key. Presume it was never tested well.  
   * <li>2016-10-15 Hartmut bugfix: because of the root table is instanceof {@link IndexMultiTable}, a value in {@link IndexMultiTable_Table#aValues} 
   *   which is type of {@link IndexMultiTable} has failed. It was recognized as a sub table with the simple check 
   *   <code>tableStart.aValues[idx] instanceof IndexMultiTable_Table</code> etc. Now it is checked whether the value is not an instance of IndexMultitable itself.
   *   The root table is not the point of quest but a sub table. That is not an instance of IndexMultiTable. 
   *   On the other hand a sub table is never a user value because {@link IndexMultiTable_Table} is package private and not free for usage.
   *   
   * <li>2016-10-15 Hartmut bugfix: The usage of <code>IndexMultiTable.super</code> works in Oracle-Java version 8.102, but not in a lesser version.
   *   To save compatibility it is avoided. Sometimes a typed meta variable is the better solution in any case.    
   * <li>2016-09-25 Hartmut chg: The root element is removed, instead this class extends a Table which is the root table.
   *   Only one advantage: 1 level lesser for debug-show of the data. But that may be proper for deep nested trees.
   *   For the change: The inner class Table is moved to an own {@link IndexMultiTable_Table} class.
   *   Some delegate methods are overridden now (clear), therefore super.clear() is called, to prevent circular invocation.
   *   No functional changes.
   * <li>2015-11-09 Hartmut bug: {@link #put(Comparable, Object)} fails in a constellation where insertion in the first child table, 
   *   {@link Table#splitIntoSibling(int, Comparable, Object)} with ix=0. The key was not updated in the parent table.
   *   fix: Using {@link Table#setKeyValue(int, Comparable, Object)} with check whether it is ix=0 and the parent key should be updated.
   * <li>2015-07-09 Hartmut chg: All methods are synchronized now because instances are used in more as one thread usual.
   *   The non-synchronized variant may be a special case. It has some more less calculation time.
   *   Idea: Offer non-synchronized methods with special name to save calculation time for applications
   *   which's instances are guaranteed not used in more as one thread.    
   * <li>2015-07-05 Hartmut bugfix: The calculation of {@link Table#sizeAll} was faulty. {@link Table#check()} improved,
   *   it checks the sizeAll and checks all child tables. Calculation of sizeAll improved. 
   * <li>2015-03-07 Hartmut bugfix false instanceof check, sub-tables has not worked.
   * <li>2014-12-21 Hartmut chg structure: Now a {@link Table} is an own instance. Improved and simplified. 
   * <li>2014-12-20 Hartmut new The iterator is a ListIterator now with {@link ListIterator#hasPrevious()} etc.
   *   The advantage and usage: Set the iterator to a expected point with {@link #iterator(Comparable)} and traverse back- and forward.
   *   There are some changes in algorithm. 
   * <li>2014-01-12 Hartmut chg: {@link #sortin(int, Comparable, Object)} new Algorithm
   * <li>2014-01-12 Hartmut chg: toString better for viewing, with all keys 
   * <li>2013-12-02 Hartmut new: Implementation of {@link #remove(Object)} was missing, {@link #searchInTables(Comparable, boolean, IndexBox)}
   *   restructured. It returns the table and index, able to use for internal searching. 
   * <li>2013-09-15 Hartmut new: Implementation of {@link #delete(int)} and {@link EntrySetIterator#remove()}.
   * <li>2013-09-15 Hartmut chg: rename and public {@link #search(Comparable, boolean, boolean[])}.  
   * <li>2013-09-07 Hartmut chg: {@link #put(Comparable, Object)} should not create more as one object
   *   with the same key. Use add to do so.
   * <li>2013-08-07 Hartmut improved.
   * <li>2013-04-21 Hartmut created, derived from {@link IndexMultiTableInteger}.
   * <li>2009-05-08 Hartmut corr/bugfix: IteratorImpl(IndexMultiTableInteger<Type> firstTable, int startKey) used in iterator(startkey):
   *                    If a non-exakt start key is found, the iterator starts from the key after it. 
   *                    If no data are available, hasnext() returns false now. TODO test if one hyperTable contains the same key as the next, some more same keys!
   * <li>2009-04-28 Hartmut new: iterator(key) to start the iterator from a current position. It is the first position with the given key. 
   *                    corr: Some empty methods are signed with xxxName, this methods were come from planned but not implemented interface
   *                    meditated: implement ListInterface instead Interface to supply getPrevious() to iterate starting from a key forward and backward.
   *                    corr: binarySearch self implemented, regarding the first occurrence of a key.
   * <li>2009-04-26 Hartmut meditated: Several key types in one file isn't good! The search routines are optimal only if the key type is fix.
   *                                There are some adequate classes necessary for int, long keys.
   *                    docu
   *                    chg: Using of class AllocInBlock to get the size of a block.
   *                    corr: Now more deepness as 2 tables is programmed and tested. Older versions support only 2 tables.
   *                          It was 1000000 entries max. with a table size of 1000.
   * <li>2009-03-01 Hartmut new: IndexMultiTableInteger(int size, char type): in preparation of using, functionality not ready yet.
   *                    planned: Type may be int, long, String, size are able to choose. The arrays should be assigned not as embedded instances in C. 
   *                    new: method get() does anything, not tested in all cases.
   * <li>2007-06-00 Hartmut Created.                  
   * </ul>
   * <br><br>
   * <b>Copyright/Copyleft</b>:
   * For this source the LGPL Lesser General Public License,
   * published by the Free Software Foundation is valid.
   * It means:
   * <ol>
   * <li> You can use this source without any restriction for any desired purpose.
   * <li> You can redistribute copies of this source to everybody.
   * <li> Every user of this source, also the user of redistribute copies
   *    with or without payment, must accept this license for further using.
   * <li> But the LPGL is not appropriate for a whole software product,
   *    if this source is only a part of them. It means, the user
   *    must publish this part of source,
   *    but don't need to publish the whole source of the own product.
   * <li> You can study and modify (improve) this source
   *    for own using or for redistribution, but you have to license the
   *    modified sources likewise under this LGPL Lesser General Public License.
   *    You mustn't delete this Copyright/Copyleft inscription in this source file.
   * </ol>
   * If you are intent to use this sources without publishing its usage, you can get
   * a second license subscribing a special contract with the author. 
   * 
   * @author Hartmut Schorrig = hartmut.schorrig@vishia.de
   * 
   */
  public static final String sVersion = "2016-10-15";

  enum KindofAdd{ addOptimized, addLast, addBefore, replace};
  
  /**This class is the Iterator for the outer class. Every {@link #iterator()}-call
   * produces one instance. It is possible to create some instances simultaneously.
   * The iterator iterates over all values in all tables, independent of their keys.
   * But because the values are sorted with the keys, the values are gotten in its key-sorted order.
   *
   * @param <Type>
   */
  protected final class IteratorImpl implements ListIterator<Type>
  {
  
    /**The helperNext and helperPrev contain the values of the iterator. Because there is a tree of tables.
     * The current IteratorHelpers of the current table depths is referenced.
     * A IteratorHelper contains a parent and child reference. If the end of table
     * of the current depth level is reached, the parent of the current helperNext is referenced here
     * than, the next table is got, than the child is initialized and referenced here.  
     * There are 2 helpers necessary because next and previous can have different tables.
     */
    IndexMultiTable.IteratorHelper<Key, Type> helperPrev, helperNext;
    
    /**Comparison of this value with {@link IndexMultiTable#modcount} to detect changes. */
    private final int modcount;
    
    /**Information whether {@link #next()} or {@link #previous()} was invoked at last.
     * It is important for the functionality of {@link #remove()}, {@link #add(Object)} and {@link #set(Object)}
     * If {@link #next()} was called at last, the last element or insertion point is that element which is referred by {@link #helperPrev}:
     * <br>
     * Before next():
     * <pre>
     *         prev--+      +--next
     *               |  ,   |
     *      [1]     [2]    [3]    [4]
     *                      |  ^   |
     *                prev--+      +--next
     * </pre>       
     * <b>next()</b> has returned [3].       
     * The {@link #remove()} operation has to remove the element at position 3, which is referred yet as the previous. 
     * The operation {@link #set(Object)} should be changed the element at position [3].
     * <br><br>
     * <be>previous()</b> has returned [2], after them:
     * <pre>
     *         prev--+      +--next
     *               |  ,   |
     *       [1]    [2]    [3]    [4]
     *        |  ^   |
     *  prev--+      +--next
     * </pre>       
     * The element [2] which is yet referred from {@link #helperNext} should be deleted or after [2] is to change.
     */
    private boolean bLastWasNext = false;

    /**The value which is returned by the last {@link #next()} and {@link #previous()}. */
    Key lastKey; Type lastValue;
    
    
    /**Creates the iterator before the first element. 
     * @param firstTable
     */
    IteratorImpl()
    { this.modcount = IndexMultiTable.this.modcount;
      helperNext = new IteratorHelper<Key, Type>(false);
      helperNext.table = IndexMultiTable.this;  //.super;  //Note: super is correct, but does not compile for Java 8 lesser versions. 
      helperNext.idx = 0;
      helperNext.checkHyperTable();  //maybe create sub tables.
      //
      helperPrev = new IteratorHelper<Key, Type>(true);
      helperPrev.table = IndexMultiTable.this;  //.super;  //Note: super is correct, but does not compile for Java 8 lesser versions.
      helperPrev.idx = -1;
      helperPrev.currKey = null;
      helperPrev.currValue = null;
    }
    
    
    /**Ctor for the Iterator which starts on any position inside the table.
     * The previous element {@link #previous()} is any element which's key is lesser or equal the given key.
     * The next element {@link #next()} is any element which's key is greater the given key.
     * The previous element is null {@link #hasPrevious()} == false if no element is found which is lesser or equal the given key.
     * The next element is null, {@link #hasNext()} == false if not element is found which's key is greater. 
     * @param firstTable The super table.
     * @param startKey The key
     * 
     */
    IteratorImpl(Key startKey)
    { this.modcount = IndexMultiTable.this.modcount;
      IndexMultiTable_Table<Key, Type> tableStart = IndexMultiTable.this;  //.super  //Note: super is correct, but does not compile for Java 8 lesser versions.;
      while(tableStart.isHyperBlock)
      { //call it recursively with sub index.
        int idx = tableStart.binarySearchFirstKey(tableStart.aKeys, 0, tableStart.sizeBlock, startKey); //, sizeBlock, key1);
        if(idx < 0)
        { /**an non exact found, accept it.
           * use the table with the key lesser than the requested key
           */
          idx = -idx-1 -1; //insertion point is .idx-1, but use the previous
        }
        if(idx < 0){
          idx = 0;   //only if the key is lesser, then start with the next table.
        }
        assert(tableStart.aValues[idx] instanceof IndexMultiTable_Table && !(aValues[idx] instanceof IndexMultiTable));
        @SuppressWarnings("unchecked") 
        IndexMultiTable_Table<Key, Type> tableNext = (IndexMultiTable_Table)tableStart.aValues[idx];
        tableStart = tableNext;
      }
      helperPrev = new IteratorHelper<Key, Type>(true);
      helperNext = new IteratorHelper<Key, Type>(false);
      helperPrev.table = helperNext.table = tableStart;
      
      int idx = tableStart.binarySearchFirstKey(tableStart.aKeys, 0, tableStart.sizeBlock,  startKey); //, sizeBlock, key1);
      if(idx < 0)
      { /**an non exact found, accept it.
         * start from the element with first key greater than the requested key
         */
        idx = -idx-1;  //it is the position of the next element.
        //helperPrev left emtpy! will be filled with next()
        helperNext.idx = idx;  //start with the next element which is the first greater then the key
        helperNext.currKey = helperNext.table.aKeys[idx];
        helperNext.checkHyperTable();
        
      } else {
        //exact found, it is the previous to add new elements with the given key after that one with the same name.
      //helperPrev left emtpy! will be filled with next()
        helperNext.idx = idx;  //it has correct parents, it is not a hyper table.
        helperNext.currKey = helperNext.table.aKeys[idx];
        helperNext.checkHyperTable();
        
      }
    }
    
    
    @Override public boolean hasNext() { return helperNext.currKey !=null; }
  
    
    @Override public boolean hasPrevious() {  return helperPrev.currKey !=null; }
  
    
    
    
    
    /**Implements the standard behavior for {@link java.util.Iterator#next()}.
     * For internal usage the {@link #helperNext} is set.
     * With them 
     */
    @SuppressWarnings("unchecked")
    @Override public Type next()
    { checkForModification();
      bLastWasNext = true;
      lastKey = helperNext.currKey;
      lastValue = helperNext.currValue;
      if(lastKey !=null) { //only if has next
        next_i();  //executes always next after last next.
      }
      return lastValue;
    }
  
    /**Implements the standard behavior for {@link java.util.Iterator#next()}.
     * For internal usage the {@link #helperNext} is set.
     * With them 
     */
    @SuppressWarnings("unchecked")
    @Override public Type previous()
    { checkForModification();
      bLastWasNext = false;
      lastKey = helperPrev.currKey;
      lastValue = helperPrev.currValue;
      if(lastKey !=null) { //only if have previous
        prev_i();  //executes always previous after last previous.
      }
      return lastValue;
    }
    
    
    @Override public int nextIndex() { return 0; }
    
    @Override public int previousIndex() { return 0; }
    
    @Override public void add(Type e) { throw new IllegalStateException("not implemented"); }
    
    
    
    /**Sets a new value to the given key!
     * @param value
     */
    @Override public void set(Type value) { throw new IllegalStateException("not implemented"); }
    
  
    Key lastKey(){ return lastKey; }
    
    
    private void checkForModification() {
      if(this.modcount != IndexMultiTable.this.modcount) throw new ConcurrentModificationException();
    }
    
    
    /**executes the next(), on entry {@link bHasNextProcessed} is false.
     * If the table is a child table and its end is reached, this routine is called recursively
     * with the now current parent, typical the parent contains a child table
     * because the table is a hyper table. Than the child helperNext is initialized
     * and reused, and this routine will be called a third time, now with the new child:
     * <pre>
     * child: end of table; parent: next table; child: test next table.
     * </pre>
     * If the tree of tables is deeper than two, and the end of a child and also the parent table
     * is reached, this routine is called recursively more as three times.
     * The maximum of recursively call depends on the deepness of the table tree.
     */
    //@SuppressWarnings("unchecked")
    private void next_i()
    { checkForModification();
      //the yet next is the new previous:
      helperPrev.copy(helperNext);
      if( ++helperNext.idx >= helperNext.table.sizeBlock) {  //next in current table.
        //The table is left.
        helperNext.checkHyperTable();
      } else {
        //element in the same table:
        helperNext.currKey = helperNext.table.aKeys[helperNext.idx];
        @SuppressWarnings("unchecked") 
        Type value = (Type)helperNext.table.aValues[helperNext.idx];
        helperNext.currValue = value;
        
      }
      
    }
    
    
    /**executes the next(), on entry {@link bHasNextProcessed} is false.
     * If the table is a child table and its end is reached, this routine is called recursively
     * with the now current parent, typical the parent contains a child table
     * because the table is a hyper table. Than the child helperNext is initialized
     * and reused, and this routine will be called a third time, now with the new child:
     * <pre>
     * child: end of table; parent: next table; child: test next table.
     * </pre>
     * If the tree of tables is deeper than two, and the end of a child and also the parent table
     * is reached, this routine is called recursively more as three times.
     * The maximum of recursively call depends on the deepness of the table tree.
     */
    //@SuppressWarnings("unchecked")
    private void prev_i()
    { checkForModification();
      //the yet next is the new previous:
      helperNext.copy(helperPrev); 
      if(--helperPrev.idx < 0) {  //next in current table.
        //The table is left.
        helperPrev.checkHyperTable(); 
      } else {
        //element in the same table:
        helperPrev.currKey = helperPrev.table.aKeys[helperPrev.idx];
        @SuppressWarnings("unchecked") 
        Type value = (Type)helperPrev.table.aValues[helperPrev.idx];
        helperPrev.currValue = value;
      }
    }
    
    
    public void remove()
    { //super.check();
      checkForModification();
      if(bLastWasNext) {
        //because the cursor is set after the next, the previous is the candidate to remove. It was the next before.
        helperPrev.table.delete(helperPrev.idx);  //delete element in table.
        //to preserve the situation, the new previous is one before:
        helperPrev.idx -=1;  //may be negative now
        helperPrev.checkHyperTable();
        //the next is unchanged normally. But because it is on another position, seek it from previous: 
        helperNext.table = helperPrev.table;  //may be a changed table, sibling before, parent etc.
        helperNext.idx = helperPrev.idx+1;  //the next
        helperNext.checkHyperTable();
        //lastkeyNext = bHasNext ? helperNext.table.aKeys[helperNext.idx] : null;
        
      } else {
        //because the cursor is set before the previous, the next is the candidate to remove.
        helperNext.table.delete(helperNext.idx);  //delete element in table.
        //left helperNext.ix unchanged
        helperNext.checkHyperTable();  //maybe end of table, correct next
        //lastkeyNext = bHasNext ? helperNext.table.aKeys[helperNext.idx] : null;
        
      }
      //if(shouldCheck){ super.check(); }
    }
  
    @Override public String toString() { return helperPrev.toString() + " ... " + helperNext.toString(); }   
  }

  /**This class contains the data for a {@link IndexMultiTable.IteratorImpl}
   * for one table.
   * 
   *
   */ 
  private static class IteratorHelper<Key extends Comparable<Key>, Type>
  {
    /**Dedication whether it is the instance for previous or for next. */
    private final boolean bPrev;
    
    /**Current index in the associated table for the next or previous entry. It is not the last returned one but the next or previous. */ 
    protected int idx;
    
    /**The associated table appropriate to the idx. */
    IndexMultiTable_Table<Key, Type> table;
    
    /**The current key and value which will be returned from following next() or prev(). */
    Key currKey; Type currValue;
    
    IteratorHelper(boolean bPrev)
    { this.bPrev = bPrev;
      this.table = null;
      idx = -1;
    }
    
    void copy(IteratorHelper<Key, Type> src) {
      this.idx = src.idx;
      this.table = src.table;
      this.currKey = src.currKey;
      this.currValue = src.currValue;
    }
    
    
    /**Checks the helper's idx and table.
     * <ul>
     * <li>If the #idx is > {@link IndexMultiTable#sizeBlock} then use the next table, over parent.
     * <li>If the #idx is < 0 then use the previous table, from parent.
     * <li>If the idx refers a hypertable then use the first non-hypertable.
     * </ul>
     * @param helper
     * @param bPrev
     * @return maybe another table, maybe null if not previous or not next
     */
    private boolean checkHyperTable()
    {
      while(idx >= table.sizeBlock && table.parent !=null) { //on end of any table:
        idx = table.ixInParent +1;  //next entry in parent table
        table = table.parent;
      }
      while(idx < 0 && table.parent !=null) { //on end of any table:
        idx = table.ixInParent -1;  //previous entry in parent table
        table = table.parent;
      }
      while(table.isHyperBlock && idx >=0 && idx < table.sizeBlock) //check whether a hyperBlock is found:
      { //
        @SuppressWarnings("unchecked")
        IndexMultiTable_Table<Key, Type> childTable = (IndexMultiTable_Table<Key, Type>)table.aValues[idx];
        table = childTable;
        if(bPrev) {
          idx = childTable.sizeBlock-1;  //prev is executed.
        } else  {
          idx = 0;  //next is executed.
        }    
      }
      if( idx >=0 && idx < table.sizeBlock) {
        currKey = table.aKeys[idx];
        @SuppressWarnings("unchecked") 
        Type value = (Type)table.aValues[idx];
        currValue = value;
        return true;
      } else {
        currKey = null;
        currValue = null;
        return false;
      }
    }

    @Override public String toString() { return table == null ? "null" : idx < 0 ? "--no-previous--" : idx >= table.sizeBlock ? "--no-next--" : table.aKeys[idx].toString(); } 
  }

  
  /**One table contains keys and values in two array lists in sorted order.
   * A table can be a hyper table. The {@link IndexMultiTable_Table#aValues} contains references to a sub table then. There is a tree of table.
   * The last element of the tree (leaf) is not a hyper-table. It contains instances of Type.
   *
   * @param <Key> Same like the {@link IndexMultiTable}
   * @param <Type>
   */
  static private class XXXTable<Key extends Comparable<Key>, Type>
  {
    
  }    

    
  final Key minKey__;

  final Key maxKey__;

  final Provide<Key> provider;

  boolean shouldCheck = true;

  //private final IndexMultiTable_Table<Key, Type> super;



  /**modification access counter for Iterator. */
  //@SuppressWarnings("unused")
  private int modcount;



  /**constructs an empty instance without data. */
  public IndexMultiTable(Provide<Key> provider)
  { super(provider);
    //this(1000, 'I');
    this.provider = provider;
    this.minKey__ = provider.getMinSortKey();
    this.maxKey__ = provider.getMaxSortKey();
  }

  /**Delete all content. 
   * @see java.util.Map#clear()
   */
  public synchronized void clear(){ modcount +=1; super.clear(); }

  /**Puts the (key - value) pair to the container. An existing value with the same key will be replaced
   * like described in the interface. If more as one value with this key are existing, the first one
   * will be replaced only. 
   * <br>
   * See {@link #add(Comparable, Object)} and {@link #append(Comparable, Object)}.
   * @param key
   * @param value
   * @return The last value with this key if existing.
   */
  @Override public synchronized Type put(Key key, Type value){
    modcount +=1;
    if(key.equals("639e10603dd6bf976b75c6b7f275a31e.pack"))
      Debugutil.stop();
    //super.check();
    Type ret = super.putOrAdd(key, value, null, KindofAdd.replace);
    if(shouldCheck) { super.check(); }
    return ret;
  }

  /**Adds the (key - value) pair to the container. All existing values with the same key will be retained.
   * If one or some values with the same key are contained in the container already, the new value is placed
   * in a non-defined order with that values. The order depends from the search algorithm. It is the fastest
   * variant to sort in a new value. This method should be used if the order of values with the same key
   * are not regardless.
   * <br>
   * See {@link #put(Comparable, Object)} and {@link #append(Comparable, Object)}.
   * @param key
   * @param value
   * @return The last value with this key if existing.
   */
  public synchronized void add(Key key, Type value){
    modcount +=1;
    //super.check();
    super.putOrAdd(key, value, null, KindofAdd.addOptimized);
    if(shouldCheck) { super.check(); }
  }

  @SuppressWarnings("unchecked")
  public synchronized boolean containsKey(Object key)
  { boolean[] found = new boolean[1];
    return search((Key)key, true, found) !=null || found[0];
  }

  /**Enables or disables a check. </b><br>
   * The method {@link IndexMultiTable_Table#check()} (package private) is called whenever a changing operation is done, before and after the operation.
   * It checks the consistency of the table. Because this operation needs calculation time, the flag {@link #shouldCheck(boolean)}
   * can be set or reset. If the check fails there is a bug in this algorithm. 
   * For bugfix in development it is proper to program a check of the current key before the operation
   * and step with the debugger.    
   * @param val true enables, false disables the ckeck.
   */
  public void shouldCheck(boolean val){ 
    shouldCheck = val; 
  }
  

  
  /**Executes a consistency check. If it fails, an RuntimeException is thrown.
   * Note: set a breakpoint in debugger to the RuntimeException.
   * @return number of stored elements.
   */
  public int checkIndex(){ return super.check(); }
  
  
  
  /**Appends the (key - value) pair to the container. All existing values with the same key will be retained.
   * If one or some values with the same key are contained in the container already, the new value is placed
   * in order after all other. The values with the same key are sorted by its append order.
   * If some more values with the same key are existing, the searching can be need some calculation time.
   * But the time is less if the table size is less. 
   * This method should be used if the order of values with the same key is need.
   * <br>
   * See {@link #put(Comparable, Object)} and {@link #append(Comparable, Object)}.
   * @param key
   * @param value
   * @return The last value with this key if existing.
   */
  public synchronized void append(Key key, Type obj){
    if(key.equals("ckgro") && super.sizeAll == 19)
      Assert.stop();
    modcount +=1; 
    //super.check();
    super.putOrAdd(key, obj, null, KindofAdd.addLast);
    if(shouldCheck) { super.check(); }
  }

  
  /**Adds the (key - value) pair to the container. All existing values with the same key will be retained.
   * The value is placed before the given next value which must have the same key. If the nextValue is not found,
   * the key is placed in a non deterministic order.
   * If some more values with the same key are existing, the searching can be need some calculation time.
   * This method should be used only if that order of values with the same key is need.
   * <br>
   * See {@link #put(Comparable, Object)}, {@link #add(Comparable, Object)} and {@link #append(Comparable, Object)}.
   * @param key
   * @param value
   * @return The last value with this key if existing.
   */
  public synchronized void addBefore(Key key, Type value, Type valueNext){
    modcount +=1;
    //super.check();
    super.putOrAdd(key, value, valueNext, KindofAdd.addBefore);
    if(shouldCheck) { super.check(); }
  }

  
  
  
  
  public boolean containsValue(Object arg0)
  {
    // TODO Auto-generated method stub
    return false;
  }







  public Set<java.util.Map.Entry<Key, Type>> entrySet()
  {
    return entrySet;
  }


  @SuppressWarnings({ "unchecked" })
  @Override public synchronized Type get(Object keyArg){
    //assert(keyArg instanceof Comparable<?>);
    if(keyArg == null) return null; //prevent NullPointerException in deeper levels. Object with key==null is not found. That is ok.
    IndexBox ixRet = new IndexBox();
    IndexMultiTable_Table<Key, Type> table = super.searchInTables(keyArg, true, ixRet);
    if(table !=null){
      return (Type)table.aValues[ixRet.ix];
    } else return null;
  }



  /**Searches the object with exact this key or the object which's key is the nearest lesser one.
   * For example if given is "Bx, By, Bz" as keys and "Bya" is searched, the value with the key "By" is returned.
   * The user should check the returned object whether it is matching to the key or whether it is able to use.
   * The found key is unknown outside of this routine but the key or adequate properties should able to get 
   * from the returned value.
   * @param key
   * @return
   */
  public synchronized Type search(Key key){ 
    return search(key, false, null);
  }

  
  
  /**Searches the key in the tables.
   * @param keyArg The key
   * @param exact if true then returns null and retFound[0] = false if the key was not found
   *   if false then returns the first value at or after the key, see {@link #search(Comparable)}.
   * @param retFound If null then not used. If not null then it must initialized with new boolean[1].
   *   retFound[0] is set to true or false if the key was found or not.
   *   Note: If the key is found and the value for this key is null, retFound[0] is set to true.
   *   Only with this the {@link #containsKey(Object)} works probably. 
   * @return The exact found value or the non exact found value with key before. 
   *   null if the key is lesser than all other keys (it should the first position).
   *   null if the value for this key is null.
   *   null if exact = true and the key is not found.
   */
  public synchronized Type search(Key keyArg, boolean exact, boolean[] retFound)
  { 
    IndexBox ixRet = new IndexBox();
    IndexMultiTable_Table<Key, Type> table = super.searchInTables(keyArg, exact, ixRet);
    if(table !=null){
      if(retFound !=null){ retFound[0] = ixRet.found; }
      @SuppressWarnings("unchecked")
      Type ret = (Type)table.aValues[ixRet.ix];
      return ret;
    } else return null;
  }
  
  
  public boolean isEmpty()
  {
    // TODO Auto-generated method stub
    return false;
  }







  public Set<Key> keySet()
  {
    // TODO Auto-generated method stub
    return null;
  }


















  public int size()
  { return super.sizeAll;
  }







  @Override public Collection<Type> values()
  {
    //Should return an implementation of Collection which deals with the inner data.
    //see iterator(), entrySet
    // TODO Auto-generated method stub
    return null;
  }











  /**Returns an iterator over the table.
   * Note: The code which uses the iterator may be crimped by a synchronized(...){...}
   * because the table should not be changed.
   * @see java.lang.Iterable#iterator()
   */
  public ListIterator<Type> iterator()
  {
    return new IteratorImpl();
  }



  /**Creates an iterator which is placed in the occurrence of the given key.
   * The {@link ListIterator#previous()} is lesser or equal and the {@link ListIterator#next()}
   * is greater or equal. If a lesser or greater element is not found then the {@link ListIterator#hasPrevious()} 
   * and {@link ListIterator#hasNext()} returns false. 
   * Note: The code which uses the iterator may be crimped by a synchronized(...){...}
   * because the table should not be changed.
   * @param fromKey The start key.
   * @return
   */
  public ListIterator<Type> iterator(Key fromKey)
  {
    return new IteratorImpl(fromKey);
  }



  /**
   * @param keyArg
   * @return
   */
  @SuppressWarnings({ "unchecked" })
  @Override synchronized public Type remove(Object keyArg){
    assert(keyArg instanceof Comparable<?>);
    IndexBox ixRet = new IndexBox();
    //super.check();
    IndexMultiTable_Table<Key, Type> table = super.searchInTables((Key)keyArg, true, ixRet);
    if(table !=null){ //null if keyArg is not found.
      Type ret = (Type)table.aValues[ixRet.ix];  //deleted value should be returned.
      table.delete(ixRet.ix);  
      if(ixRet.ix == 0){
        //remove key in parent table
        Debugutil.stop();
      }
      if(shouldCheck) { super.check(); }
      return ret;    
    } else return null;
  }

  
  void stop()
  { //debug
  }
  
  
  
  /**Checks the consistency of the table. This method is only proper for test of the algorithm
   * and assurance of correctness. 
   * The check starts with the top table. It iterates over all children in order of the tables and checks:
   * <ul>
   * <li>{@link #ixInParent} and {@link #parent} of a child hyper table.
   * <li>equality of the {@link #aKeys}[ixInParent] in the parent and {@link #aKeys}[0] in the child hyper table.
   * <li>Order of all sort keys in all tables.
   * <li>All not used entries in {@link #aKeys} should have the {@link Provide#getMaxSortKey()}
   *   and all not used entries in {@link #aValues} should have null.
   * </ul> 
   * If any error is found, an RuntimeException is invoked. It is recommended to test in a debugger.
   * If the algorithm in this class is correct, the exception should not be invoked.
   */ 
  public void checkTable(){ super.checkTable(null, null, -1, provider.getMinSortKey() );}
  
  


  @Override
  public synchronized void putAll(Map<? extends Key, ? extends Type> m)
  {
    for(Map.Entry<? extends Key, ? extends Type> e: m.entrySet()){
      put(e.getKey(), e.getValue());
    }
  }
  
  
  
  @Override public String toString(){ return super.toString(); }
  
    /**This interface is necessary to provide tables and the minimum and maximum value for any user specific type.
   * For the standard type String use {@link IndexMultiTable#providerString}.
   * @param <Key>
   */
  public interface Provide<Key>{
    /**Creates an array of the Key type with given size. */
    Key[] createSortKeyArray(int size);
    
    /**Returns the minimal value of the key. It is a static value. */
    Key getMinSortKey();
    
    /**Returns the maximal value of the key. It is a static value. */
    Key getMaxSortKey();
  }

  /**Provider for String keys. Used for {@link #IndexMultiTable(Provide)}. */
  public static final IndexMultiTable.Provide<String> providerString = new IndexMultiTable.Provide<String>(){

    @Override public String[] createSortKeyArray(int size){ return new String[size]; }

    @Override public String getMaxSortKey(){ 
      return "\uffff\uffff\uffff\uffff\uffff\uffff\uffff\uffff\uffff"; 
    }

    @Override public String getMinSortKey(){ return ""; }
  };
  

  Set<Map.Entry<Key, Type>> entrySet = new Set<java.util.Map.Entry<Key, Type>>()
  {

    @Override
    public boolean add(Map.Entry<Key, Type> e)
    {
      put(e.getKey(), e.getValue());
      return true;
    }

    @Override
    public boolean addAll(Collection<? extends java.util.Map.Entry<Key, Type>> c)
    { for(Map.Entry<Key, Type> e: c){
        put(e.getKey(), e.getValue());
      }
      return true;
    }

    @Override
    public void clear()
    {
      IndexMultiTable.this.clear();
    }

    @Override
    public boolean contains(Object o) { 
//      if(o instanceof Key) {
//      
//      }
      return false; //TDOO ... never used: IndexMultiTable.this.containsValue(o);
    }

    @Override
    public boolean containsAll(Collection<?> c)
    { boolean ok = true;
      for(Object obj: c){
        if(false) { //TDOO ... never used: !IndexMultiTable.this.containsValue(obj)){
          ok = false;
        }
      }
      return ok;
    }

    @Override
    public boolean isEmpty()
    {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public Iterator<java.util.Map.Entry<Key, Type>> iterator()
    { return IndexMultiTable.this.new EntrySetIterator();
    }

    @Override
    public boolean remove(Object o)
    {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public boolean removeAll(Collection<?> c)
    {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public boolean retainAll(Collection<?> c)
    {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public int size()
    {
      // TODO Auto-generated method stub
      return 0;
    }

    @Override
    public Object[] toArray()
    {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public <T> T[] toArray(T[] a)
    { 
      // TODO Auto-generated method stub
      return null;
    }
    
  };
  
  
  
  protected class EntrySetIterator implements Iterator<Map.Entry<Key, Type>>
  {

    private final IteratorImpl tableIter = (IteratorImpl)IndexMultiTable.this.iterator();
    
    @Override public boolean hasNext()
    {
      return tableIter.hasNext();
    }

    @Override public Map.Entry<Key, Type> next()
    {
      //IteratorHelper<Key, Type> helperNext = tableIter.helperNext;
      Type value = tableIter.next();
      Key key = tableIter.lastKey();
      return new Entry(key, value); 
    }

    @Override
    public void remove()
    { tableIter.remove();
      //tableIter.helperNext.table.delete(tableIter.helperNext.idx);
      //tableIter.
      /*
      tableIter.helperNext.idx -=1;  //maybe -1 if first was deleted.
      //IndexMultiTable.IteratorHelper<Key, Type> helperTest = tableIter.helperNext;
      while(tableIter.helperNext.parentIter !=null && tableIter.helperNext.table.sizeBlock ==0){
        tableIter.helperNext = tableIter.helperNext.parentIter;
        tableIter.helperNext.idx -=1;  //on idx it is the next, it has deleted the child table!
      }
      */
    }
    
    
    @Override public String toString(){ return tableIter.toString(); }
    
  };
  
  
  protected class Entry implements Map.Entry<Key, Type>{
    final Type value; final Key key;
    Entry(Key key, Type value){ this.key = key; this.value = value; }
    @Override public Key getKey()
    { return key;
    }
    @Override
    public Type getValue()
    { return value;
    }
    
    @Override
    public Type setValue(Type value)
    { throw new IllegalArgumentException("IndexMultiTable.Entry does not support setValue()");
    }
    
    @Override public String toString(){ return "[ " + key + ", " + value + " ]"; }
  }
  
  
  
  class IndexBox{
    int ix;
    boolean found;
  }
  
  
}
