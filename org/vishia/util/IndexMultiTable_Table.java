package org.vishia.util;

import java.util.Arrays;

import org.vishia.bridgeC.AllocInBlock;

/**One Table for a {@link IndexMultiTable}. This is a package-private class only used in {@link IndexMultiTable}.
 * @author hartmut Schorrig
 *
 * @param <Key>
 * @param <Type>
 */
class IndexMultiTable_Table<Key extends Comparable<Key>, Type>
{
  /**Version, history and license.
   * <ul>
   * <li>2016-09-25 Hartmut new, copied from {@link IndexMultiTable} .Table without functional changes. 
   * </ul>
   * 
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
   * 
   */
  static final public String sVersion = "2016-10-15";

  
  
  
  
  /**The maximal nr of elements in a block, maximal value of sizeBlock.
   * It is the same value as obj.length or key.length. */
  protected final static int maxBlock = AllocInBlock.restSizeBlock(IndexMultiTable.class, 160) / 8; //C: 8=sizeof(int) + sizeof(Object*) 

  static int identParent_ = 100;



  /**Array of all keys. Note: It is the first element to see firstly while debugging. */
  protected final Key[] aKeys; // = new Key[maxBlock];

  /**Array of objects appropriate to the keys. It is either a {@link IndexMultiTable_Table} if this is a hyper-table, or a Type-instance */
  protected final Object[] aValues = new Object[maxBlock];

  /**actual number of objects stored in this table. */
  int sizeBlock;

  /**actual number of leaf objects stored in the table tree of this and its children. */
  int sizeAll;

  /**True, than {@link #aValues} contains instances of this class too. */
  protected boolean isHyperBlock;

  /**A identifier number for debugging.*/
  final int identParent = ++identParent_;

  /**Index of this table in its parent. */
  int ixInParent;
 
  /**The parent if it is a child table. The parent is always a hypertable. */
  IndexMultiTable_Table<Key, Type> parent;

  /**Reference to the root data. */
  final IndexMultiTable<Key, Type> rootIdxTable;



  /**constructs an empty root table. */
  IndexMultiTable_Table(IndexMultiTable.Provide<Key> provider)
  { //this(1000, 'I');
    this.rootIdxTable = (IndexMultiTable<Key, Type>)this;
    this.aKeys = provider.createSortKeyArray(maxBlock);
    final Key maxKey__ = provider.getMaxSortKey();
    for(int idx = 0; idx < maxBlock; idx++){ aKeys[idx] = maxKey__; }
    sizeBlock = 0;
    ixInParent = -1;
  }


  /**constructs an empty table. */
  IndexMultiTable_Table(IndexMultiTable<Key, Type> root1)
  { //this(1000, 'I');
    this.rootIdxTable = root1;
    this.aKeys = this.rootIdxTable.provider.createSortKeyArray(maxBlock);
    for(int idx = 0; idx < maxBlock; idx++){ aKeys[idx] = this.rootIdxTable.maxKey__; }
    sizeBlock = 0;
    ixInParent = -1;
  }



  /**Binary search of the element, which is the first with the given key.
   * The algorithm is modified from the known algorithm (for example in copied from Arrays.binarySearch0(...))
   * The modification is done because more as one entry with the same key can be existing.
   * The algorithm searches the first occurrence of it. 
   * @param a The array
   * @param fromIndex start index, first position
   * @param toIndex end index. exclusive last position in a
   * @param key search key
   * @return index in a where the key is found exactly the first time (lessest index)
   *   or negative number: insertion point index is (-return -1)
   */
  int binarySearchFirstKey(Comparable<Key>[] a, int fromIndex, int toIndex, Object key) 
  {
    int low = fromIndex;
    int high = toIndex - 1;
    int mid =0;
    boolean equal = false;
    while (low <= high) 
    {
      mid = (low + high) >> 1;
      Comparable<Key> midVal = a[mid];
      int cmp = compare(midVal, key);
      if ( cmp < 0)
      { low = mid + 1;
        //equal = false;
      }
      else { // dont check : if(cmp >0) because search always to left to found the leftest position
        high = mid - 1;   //search in left part also if key before mid is equal
        equal = equal || cmp ==0;  //one time equal set, it remain set.
      }
    }
    if(equal) return low > mid ? low : mid;  //one time found, then it is low or mid 
    else return -(low + 1);  // key not found.
  }



  /**Assures that if val1 is a String, the key is converted toString() before comparison.
   * @param val1
   * @param key
   * @return
   */
  protected int compare(Comparable<Key> val1, Object key){
    int cmp;
    if(val1 instanceof CharSequence){
      //prevent String.compareTo(AnyOtherCharSequence) because only String.compareTo(String) works:
      //but enables comparison of any other key type.
      CharSequence key1 = key instanceof CharSequence ? (CharSequence)key : key.toString();
      cmp = StringFunctions.compare((CharSequence)val1, key1);  
    } else {
      //assert(key instanceof Key);
      @SuppressWarnings("unchecked") 
      Key key1 = (Key)key;
      cmp = val1.compareTo(key1);  //compare CharSequence, not only Strings
    }
    return cmp;
  }



  /**Puts or adds an object in the table. The key may be ambiguous. On adding a new object with the same key is placed
   * after an containing object with this key. On puting that object which is found with the key will be replaces.
   * If the table is full, a new table will be created internally.
   *  
   * 
   */
  Type putOrAdd(Key sortKey, Type value, Type valueNext, IndexMultiTable.KindofAdd kind)
  { //NOTE: returns inside too.
    //check();
    Type lastObj = null;
    if(isHyperBlock && sizeBlock == maxBlock){
      //split the block because it may be insufficient. If it is insufficient,
      //the split from child to parent does not work. split yet.
      if(parent !=null){
        IndexMultiTable_Table<Key, Type> sibling = splitIntoSibling(-1, null, null);
        if(compare(sibling.aKeys[0],sortKey) <=0){
          //do it in the sibling.
          return sibling.putOrAdd(sortKey, value, valueNext, kind);
        }
      } else {
        //it is the top level table:
        splitTopLevel(-1, null, null);
      }
    }
    //check();
    //place object with same key after the last object with the same key.
    int idx = Arrays.binarySearch(aKeys, sortKey); //, sizeBlock, key1);
    if(idx < 0) {
      //not found
      idx = -idx-1;  //NOTE: sortin after that map, which index starts with equal or lesser index.
      if(isHyperBlock)
      { //call it recursively with sub index.
        //the block with the range 
        idx -=1;
        if(idx<0)
        { //a index less than the first block is gotten.
          //sortin it in the first block.
          idx = 0;
          /*faulty, do later, check fails.
          Table<Key, Type> parents = this;
          while(parents != null)
          { //if(key1 < key[0])
            if(compare(sortKey,aKeys[0]) <0)
            { aKeys[0] = sortKey; //correct the key, key1 will be the less of child.
            }
            parents = parents.parent;
          }
          //NOTE: if a new child will be created, the key[0] is set with new childs key.
         * 
         */
        }
        @SuppressWarnings("unchecked")
        IndexMultiTable_Table<Key, Type> childTable = (IndexMultiTable_Table<Key, Type>)aValues[idx];
        //check();  //firstly the key should be inserted before check!
        lastObj = childTable.putOrAdd(sortKey, value, valueNext, kind); 
        //check();
      }
      else {
        //no hyperblock, has leaf data:
        if(idx <0)
        { idx = -idx -1;
          sortin(idx, sortKey, value);  //idx+1 because sortin after found position.            
          //check();
        }
        else
        { sortin(idx, sortKey, value);  //idx+1 because sortin after found position.            
          //check();
        }
      }
    }
    else
    { //if key1 is found, sorting after the last value with that index.
      switch(kind){
        case replace: {
          //should replace.
          if(isHyperBlock){
            @SuppressWarnings("unchecked")
            IndexMultiTable_Table<Key, Type> childTable = (IndexMultiTable_Table<Key, Type>)aValues[idx];
            lastObj = childTable.putOrAdd(sortKey, value, valueNext, kind);
          } else {
            @SuppressWarnings("unchecked")
            Type lastObj1 = (Type)aValues[idx];
            lastObj = lastObj1;
            aValues[idx] = value;   //replace the existing one.
          }
        } break;
        case addBefore: {
          boolean ok = searchAndSortin(sortKey, value, idx, valueNext);
          if(!ok){
            searchbackAndSortin(sortKey, value, idx, valueNext);
          }
        } break;
        case addLast: {
          assert(valueNext ==null);
          searchLastAndSortin(sortKey, value, idx);
        } break;
        case addOptimized: {
          if(isHyperBlock){
            @SuppressWarnings("unchecked")
            IndexMultiTable_Table<Key, Type> childTable = (IndexMultiTable_Table<Key, Type>)aValues[idx];
            childTable.putOrAdd(sortKey, value, valueNext, kind);
          } else {
            sortin(idx, sortKey, value);
          }
        } break;
      }//switch
    }
    //check();
    return lastObj;
  }



  /**Sorts in the given value after all other values with the same key.
   * <ul>
   * <li>If this table has a parent table and the parent table or its parent has a next child with the same key,
   *   then this method is started in the parent. It searches the last key in the parent firstly. Therewith
   *   it is faster to search the end of entries with this key.
   * <li>Elsewhere the last key is searched in this table. Only this table and maybe its children contains the key.
   * <li>If this table is not a hyper block, the value is {@link #sortin(int, Comparable, Object)} after the last
   *   found key.
   * <li>If this table is a hyper block, the last child table with the same key is entered with this method
   *   to continue in the child.     
   * </ul>
   * @param sortkey The key for sort values.
   * @param value
   * @param ixstart The start index where a this key is found.
   * @return
   */
  boolean searchLastAndSortin(Key sortkey, Type value, int ixstart){
    boolean cont = true;
    int ix = ixstart;
    IndexMultiTable_Table<Key, Type> parent1 = parent, child1 = this;
    while(parent1 !=null){
      if( child1.ixInParent +1 < parent1.sizeBlock 
        && compare(parent1.aKeys[child1.ixInParent+1], sortkey)==0) {
        //the next sibling starts with the same key, look in the parent!
        //Note that it is recursively, starts with the highest parent with that property,
        //walk trough the parent firstly, therefore it is fast.
        return parent1.searchLastAndSortin(sortkey, value, ixInParent+1);
      }
      else if(child1.ixInParent == parent1.sizeBlock){
        //it is possible that the parent's parent have more same keys:
        child1 = parent1; parent1 = parent1.parent;  //may be null, then abort
      } else {
        parent1 = null; //forces finish searching parent.
      }
    }
    while(cont && ix < sizeBlock){
      if((++ix) == sizeBlock             //end of block reached. The sibling does not contain the key because parent is tested.
        || compare(aKeys[ix],sortkey) != 0  //next child has another key 
        ){
        if(isHyperBlock){
          ix-=1;  //it should be stored in the last hyper block, not in the next one. 
          @SuppressWarnings("unchecked")
          IndexMultiTable_Table<Key, Type> childTable = (IndexMultiTable_Table<Key, Type>)aValues[ix];
          childTable.searchLastAndSortin(sortkey, value, 0);
          cont = false;
        } else {
          cont = false;
          sortin(ix, sortkey, value);   //sortin after 
        }
      }
    }
    //check();
    
    return !cont;
  }



  /**Sorts in the given value before the given element with the same key forward.
   * Starting from the given position all elements where iterate.
   * @param sortkey The key for sort values.
   * @param value
   * @param ixstart The start index where a this key is found.
   * @param valueNext the requested next value.
   * @return
   */
  boolean searchAndSortin(Key sortkey, Type value, int ixstart, Type valueNext){
    boolean cont = true;
    boolean ok = false;
    int ix = ixstart;
    while(cont && ix < sizeBlock){
      if(isHyperBlock){
        @SuppressWarnings("unchecked")
        IndexMultiTable_Table<Key, Type> childTable = (IndexMultiTable_Table<Key, Type>)aValues[ix];
        ok = childTable.searchAndSortin(sortkey, value, ix, valueNext);
        //check();
        cont = !ok;
        if(cont){
          ix +=1;  //continue with next.
        }
      } else {
        if(ix < (sizeBlock -1) || aValues[ix+1] == valueNext){
          sortin(ix, sortkey, value);
          //check();
          ok = true;
          cont = false;
        }
        else if((++ix) < sizeBlock && compare(aKeys[ix],sortkey) != 0){
          cont = false;
        }
      }
    }
    //check();
    return ok;
  }



  /**Sorts in the given value before the given element with the same key backward.
   * Starting from the given position all elements where iterate. TODO not ready yet.
   * @param sortkey The key for sort values.
   * @param value
   * @param ixstart The start index where a this key is found.
   * @param valueNext the requested next value.
   * @return
   */
  boolean searchbackAndSortin(Key sortkey, Type value, int ixstart, Type valueNext){
    return false;
  }



  /**inserts the given element into the table at given position.
   * If the table is less, it will be split either in an additional sibling 
   * or, if it is the top level table, into two new tables under the top table.
   * Split is done with {@link #splitIntoSibling(int, Comparable, Object)} or {@link #splitTopLevel(int, Comparable, Object)}.
   * If the table is split, the value is inserted in the correct table.
   * @param ix The index position for the actual table where the value should be sorted in.
   * @param sortkey sorting string to insert.
   * @param value value to insert.
   */
  void sortin(int ix, Key sortkey, Object value)
  { //assert(value instanceof Type || value instanceof IndexMultiTable_Table);
    if(sizeBlock == maxBlock)
    { //divide the block:
      if(isHyperBlock)
        rootIdxTable.stop();
      if(parent != null)
      { //it has a hyper block, use it!
        //create a new sibling of this.
        splitIntoSibling(ix, sortkey, value);
        //check();
      }
      else
      { //It is the top level block, it can be splitted only.
        //divide the content of the current block in 2 blocks.
        splitTopLevel(ix, sortkey, value);
        //check();
      }
    }
    else
    { //shift all values 1 to right, regard ixInParent if it is a child table.
      if(ix < sizeBlock)
      { //move all following items to right:
        movein(this, this, ix, ix+1, sizeBlock - ix);
      }
      sizeBlock +=1;
      setKeyValue(ix, sortkey, value);
      //if(value instanceof IndexMultiTable<?, ?>){
      if(value instanceof IndexMultiTable_Table && !(value instanceof IndexMultiTable)){  //a sub table is either an instance of IndexMultiTable_Table or any other Object
        //a sub table was sorted in
        @SuppressWarnings("unchecked")
        IndexMultiTable_Table<Key,Type> childTable = (IndexMultiTable_Table<Key,Type>)value;
        childTable.ixInParent = ix;
        childTable.parent = this;
      } else {
        addSizeAll(1);  //add a leaf.
      }
    }
    //don't check(); because it is not consistent in this state.
  }



  /**Splits the top level into 2 child tables and inserts the given element. The top level will refer this 2 tables after them.
   * Note: Splitting of sub tables creates 2 tables from one given, another algorithm.
   * @param idx
   * @param key1
   * @param obj1
   */
  private void splitTopLevel(int idx, Key key1, Object obj1){
    IndexMultiTable_Table<Key, Type> left = new IndexMultiTable_Table<Key, Type>(rootIdxTable);
    IndexMultiTable_Table<Key, Type> right = new IndexMultiTable_Table<Key, Type>(rootIdxTable);
    left.parent = right.parent= this;  //.super;  //Note: super is correct, but does not compile for Java 8 lesser versions.
    left.isHyperBlock = right.isHyperBlock = this.isHyperBlock;
    left.ixInParent = 0;
    right.ixInParent = 1;
    //the current block is now a hyper block.
    this.isHyperBlock = true;
    int newSize = this.sizeBlock/2;
    IndexMultiTable_Table<Key, Type> rootTable = this;  //NOTE: better to use a type-exact meta variable than 'super' for the root table.
    if(idx > newSize){  //new object to the right table
      left.sizeAll = this.movein(rootTable, left, 0, 0, newSize);
      left.sizeBlock = newSize;
      right.sizeAll = this.movein(rootTable, right, newSize, 0, idx - newSize);
      int ix1 = idx - newSize;
      right.aKeys[ix1] = key1;
      right.aValues[ix1] = obj1;
      if(obj1 instanceof IndexMultiTable_Table && !(obj1 instanceof IndexMultiTable)){ //insert a table.
        @SuppressWarnings("unchecked")
        IndexMultiTable_Table<Key,Type> childTable = (IndexMultiTable_Table<Key,Type>)obj1;
        right.sizeAll += childTable.sizeAll;    //don't change sizeAll of parent because there are not new leafs.
        childTable.ixInParent = ix1;
        childTable.parent = right;
      } else { //simple element.
        right.addSizeAll(1);
      }
      right.sizeAll += this.movein(rootTable, right, idx, ix1+1, this.sizeBlock - idx);
      right.sizeBlock = this.sizeBlock - newSize +1;
      this.aValues[0] = left;
      this.aValues[1] = right;
      left.check();
      right.check();
    } else { //new object to the left table.
      if(idx >=0){
        left.sizeAll = this.movein(rootTable, left, 0, 0, idx);
        left.aKeys[idx] = key1;
        left.aValues[idx] = obj1;
        if(obj1 instanceof IndexMultiTable_Table && !(obj1 instanceof IndexMultiTable)){
          @SuppressWarnings("unchecked")
          IndexMultiTable_Table<Key,Type> childTable = (IndexMultiTable_Table<Key,Type>)obj1;
          childTable.ixInParent = idx;
          childTable.parent = left;
        }
        left.addSizeAll(1);
        left.sizeAll += this.movein(rootTable, left, idx, idx+1, newSize - idx);
        left.sizeBlock = newSize +1;
      } else {
        left.sizeAll = this.movein(rootTable, left, 0, 0, newSize);
        left.sizeBlock = newSize;
      }
      right.sizeAll = this.movein(rootTable, right, newSize, 0, this.sizeBlock - newSize);
      right.sizeBlock = this.sizeBlock - newSize;
      this.aValues[0] = left;
      this.aValues[1] = right;
      //left.check();
      //right.check();
    }
    this.aKeys[0] = left.aKeys[0]; //minKey__;  //because it is possible to sort in lesser keys.
    this.aKeys[1] = right.aKeys[0];
    this.sizeBlock = 2;
    this.clearRestArray(rootTable);
    if(rootIdxTable.shouldCheck) { this.check(); }
  }

  
  
  
  /**Split this table in 2 tables referred from the parent table. This is not applicable for the root table.
   * @param ix if <0 then do not sortin a key, obj1
   * @param key1
   * @param obj1
   */
  private IndexMultiTable_Table<Key, Type> splitIntoSibling(final int ix, Key key1, Object obj1){
    IndexMultiTable_Table<Key, Type> sibling = new IndexMultiTable_Table<Key, Type>(rootIdxTable);
    //@SuppressWarnings("unused") int sizeall1 = parent.check();
    sibling.parent = parent;
    sibling.isHyperBlock = isHyperBlock;
    sibling.ixInParent = this.ixInParent +1;
    //sortin divides the parent in 2 tables if it is full.
    int ixSplit = sizeBlock/2;
    if(ix > ixSplit){
      //new element moved into the sibling.
      final int ixInSibling = ix - ixSplit;
      if(ixInSibling > 0) {
        //move first part into siblinh.
        sibling.sizeAll = movein(this, sibling, ixSplit, 0, ixInSibling);
      }
      sibling.aKeys[ixInSibling] = key1;  //note: sibling.aKeys[0] will be handled 16 lines later.
      sibling.aValues[ixInSibling] = obj1;
      if(sizeBlock > ix) {
        //move rest.
        sibling.sizeAll += movein(this, sibling, ix, ixInSibling +1, sizeBlock - ix);
      }
      sibling.sizeBlock = sizeBlock - ixSplit +1;
      this.sizeBlock = ixSplit;
      this.sizeAll -= sibling.sizeAll; //The elements in sibling.
      if(obj1 instanceof IndexMultiTable_Table && !(obj1 instanceof IndexMultiTable)){
        @SuppressWarnings("unchecked")
        IndexMultiTable_Table<Key,Type> childTable = (IndexMultiTable_Table<Key,Type>)obj1;
        childTable.ixInParent = ixInSibling;
        childTable.parent = sibling;
      }
      clearRestArray(this);
      parent.sortin(sibling.ixInParent, sibling.aKeys[0], sibling);  //sortin the empty table in parent.      
      if(!(obj1 instanceof IndexMultiTable_Table && !(obj1 instanceof IndexMultiTable))){//add a leaf
        sibling.addSizeAll(1); //the new element. Add leaf only on ready structure
      }
      //parent.check();
    } else {
      //new element moved into this.
      sibling.sizeAll = movein(this, sibling, ixSplit, 0, sizeBlock - ixSplit);
      sibling.sizeBlock = sizeBlock - ixSplit; 
      if(ix >=0){
        if(ix < ixSplit){ //move only if it is not on the end. idx == newSize: movein not necessary.
          movein(this, this, ix, ix+1, ixSplit -ix);
        }
        sizeBlock = ixSplit +1;
        sizeAll = sizeAll - sibling.sizeAll;
        setKeyValue(ix, key1, obj1);
        if(obj1 instanceof IndexMultiTable_Table&& !(obj1 instanceof IndexMultiTable)){
          @SuppressWarnings("unchecked")
          IndexMultiTable_Table<Key,Type> childTable = (IndexMultiTable_Table<Key,Type>)obj1;
          childTable.ixInParent = ix;
          childTable.parent = this;
        }
      } else { //idx < 0, nothing to add
        sizeBlock = ixSplit;
        sizeAll = sizeAll - sibling.sizeAll;
      }
      clearRestArray(this);
      parent.sortin(sibling.ixInParent, sibling.aKeys[0], sibling);  //sortin the empty table in parent.      
      if(ix >=0  && !(obj1 instanceof IndexMultiTable_Table&& !(obj1 instanceof IndexMultiTable))){ //has add a leaf
        this.addSizeAll(1); //the new element. Add leaf only on ready structure
      }
      //parent.check();
    }
    return sibling;
  }



  /**Moves some elements of the src table in the dst table. Note that this method does not need any information
   * of this. It is a static method. Only because Key and Src should be known - the same like the calling instance -
   * this method is not static.
   * @param src The source table
   * @param dst The destination table
   * @param ixSrc Position in src
   * @param ixDst Position in dst
   * @param nrof number of elements to move from src to dst.
   * @return the number of elements moved inclusively all elements in children, to build {@link #sizeAll}
   */
  int movein(IndexMultiTable_Table<Key,Type> src, IndexMultiTable_Table<Key,Type> dst, int ixSrc, int ixDst, int nrof){
    int sizeRet = 0;
    //int ix2 = ixDst + nrof - 1;
    int ct1 = nrof;
    int ix1Src, ix1Dst;
    final int dx;
    if(src == dst && ixSrc < ixDst) {
      //start from end, backward
      dx = -1;
      ix1Src = ixSrc + nrof -1;
      ix1Dst = ixDst + nrof -1;
    } else {
      dx = 1;
      ix1Src = ixSrc;
      ix1Dst = ixDst;
    }
    boolean bHypertable = nrof >0 && (src.aValues[ixSrc] instanceof IndexMultiTable_Table && !(src.aValues[ixSrc] instanceof IndexMultiTable));
    while(--ct1 >=0) {
    //for(int ix1 = ixSrc + nrof-1; ix1 >= ixSrc; --ix1){
      Object value = src.aValues[ix1Src];
      if(bHypertable) {
        @SuppressWarnings("unchecked")
        IndexMultiTable_Table<Key,Type> childTable = (IndexMultiTable_Table<Key,Type>)value;
        childTable.ixInParent = ix1Dst;
        childTable.parent = dst;
        sizeRet += childTable.sizeAll;
      } else {
        sizeRet += 1;
      }
      dst.aValues[ix1Dst] = value;
      dst.aKeys[ix1Dst] = src.aKeys[ix1Src];
      ix1Src += dx;  //count forward or backward.
      ix1Dst += dx;
    }
    return sizeRet;
  }



  /**Cleanup the part if {@link #aKeys} and {@link #aValues} which are not used.
   * Note: {@link #sizeBlock} have to be set correctly.
   * @param dst The table where clean up is done.
   */
  void clearRestArray(IndexMultiTable_Table<Key,Type> dst){
    //Key maxKey = root1.provider.getMaxSortKey();
    for(int ix = dst.sizeBlock; ix < maxBlock; ix++)
    { dst.aKeys[ix] = rootIdxTable.maxKey__; 
      dst.aValues[ix] = null;
    }
    
  }





  /**Deletes the element on ix in the current table.
   * @param ix
   */
  protected void delete(int ix){
    //Key keydel = aKeys[ix];
    sizeBlock -=1;
    if(!(aValues[ix] instanceof IndexMultiTable_Table)) {
      addSizeAll(-1);
    }
    if(ix < sizeBlock){ //Note:sizeBlock is decremented already.
      movein(this, this, ix+1, ix, this.sizeBlock - ix);
    }
    if(ix == 0) {
      correctKey0InParents();
    }
    aKeys[sizeBlock] = rootIdxTable.maxKey__;
    aValues[sizeBlock] = null;   //prevent dangling references!
    ////
    if(sizeBlock == 0) {
      if(parent !=null){
        //this sub-table is empty
        //
        //int ixParent = binarySearchFirstKey(parent.aKeys, 0, parent.sizeBlock, keydel); //, sizeBlock, key1);
        //if(ixParent < 0)
        //{ ixParent = -ixParent-1;  
        //}
        parent.delete(this.ixInParent);  //call recursively.
        //it has delete the child table. The table may be referenced by an iterator still.
        //But the iterator won't detect hasNext() and it continoues on its parent iterator too. 
      } else {
        //The root table is empty. If it was an Hypertable:
        isHyperBlock = false; //elsewhere problems on next put.
      }
    } else { //check only if this table is not deleted.
      //check();
    }
  }




  /**Delete all content. 
   * @see java.util.Map#clear()
   */
  void clear()
  {
    for(int ix=0; ix<sizeBlock; ix++){
      if(isHyperBlock){ 
        @SuppressWarnings("unchecked")
        IndexMultiTable_Table<Key, Type> subTable = (IndexMultiTable_Table<Key, Type>)aValues[ix];
        subTable.clear();
      }
      aValues[ix] = null;
      aKeys[ix] = rootIdxTable.maxKey__; 
    }
    sizeBlock = 0;
    sizeAll = 0;
    isHyperBlock = false;
    //check();
  }

  
  /**Searches the key in the tables.
   * @param key1 The key
   * @param exact if true then returns null and retFound[0] = false if the key was not found
   *   if false then returns the first value at or after the key, see {@link #search(Comparable)}.
   * @param ixFound should be create newly or initialize. 
   *   If the key was found, the found is set to true. If the key is not found, the found is not
   *   touched. It should be false initially. If the key is found and the value for this key is null, found true.
   *   Only with this the {@link #containsKey(Object)} works probably.
   *   ix is set in any case if this method does not return null. 
   * @return The table where the element is found. 
   *   null if the key is lesser than all other keys (it should the first position).
   *   null if the value for this key is null.
   *   null if exact = true and the key is not found.
   */
  //private  
  IndexMultiTable_Table<Key, Type> searchInTables(Object key1, boolean exact, IndexMultiTable<Key, Type>.IndexBox ixFound)
  { IndexMultiTable_Table<Key, Type> table = this;
    //place object with same key after the last object with the same key.
    while(table.isHyperBlock)
    { int idx = binarySearchFirstKey(table.aKeys, 0, table.sizeBlock, key1); //, sizeBlock, key1);
      if(idx < 0)
      { //an non exact found index is possible if it is an Hyper block.
        idx = -idx-2;  //NOTE: access to the lesser element before the insertion point.
      }
      if(idx<0)
      { return null;
      }
      else
      { assert(idx < table.sizeBlock);
        @SuppressWarnings( "unchecked")
        IndexMultiTable_Table<Key, Type> table1 = ((IndexMultiTable_Table<Key, Type>)(table.aValues[idx]));
        table = table1;
      }
    }
    int idx = binarySearchFirstKey(table.aKeys, 0, table.sizeBlock, key1); //, sizeBlock, key1);
    { if(idx < 0){
        if(exact) return null;
        else {
          //ixFound.found remain false
          idx = -idx -2;   //NOTE: access to the lesser element before the insertion point.
        }
      } else {
        ixFound.found = true; 
      }
      if(idx >=0)
      { ixFound.ix = idx;
        return table;
      }
      else  
      { //not found, before first.
        return null;
      }  
    }
  }


  /* it is unused:
  Table<Key, Type> nextSibling(){
    Table<Key, Type> sibling = null;
    if(parent !=null){
      if(ixInParent < sizeBlock-1){
        @SuppressWarnings("unchecked")
        Table<Key, Type> sibling1 = (Table<Key, Type>)parent.aValues[ixInParent+1];
        sibling = sibling1;
      } else {
        Assert.check(false);
      }
    } else {
      Assert.check(false);
    }
    return sibling;
  }
  */
  
  
  /**Change the size in this table and in all parents.
   * @param add usual +1 or -1 for add and delete.
   */
  private void addSizeAll(int add){
    this.sizeAll += add;
    if(parent !=null) {
      parent.addSizeAll(add);
    }
  }



  /**Correct the key in the parent if the key on aKeys[0] was changed.
   * This method is invoked on put or delete.
   */
  private void correctKey0InParents()
  {
    int ix2 = 0;
    IndexMultiTable_Table<Key, Type> table = this; 
    while(table.parent !=null && ix2 == 0) {
      ix2 = table.ixInParent;  //the ix of this table in the parent. 
      table.parent.aKeys[ix2] = table.aKeys[0];  //The key to the child
      table = table.parent;
    }
  }
  

  private void setKeyValue(int ix, Key key, Object value){
    aKeys[ix] = key;
    aValues[ix] = value;
    if(ix == 0) {
      correctKey0InParents();
    }
  }
  
  
  @SuppressWarnings("unchecked")
  int check()
  { int sizeAllCheck = 0; 
    if(parent!=null){
      assert1(parent.aValues[ixInParent] == this);
    }
    //if(sizeBlock >=1){ assert1(aValues[0] != null); }  //2015-07-10: removed a value can be null
    for(int ii=1; ii < sizeBlock; ii++)
    { assert1(compare(aKeys[ii-1],aKeys[ii]) <= 0);
      //assert1(aValues[ii] != null);  //2015-07-10: removed a value can be null
      if(aValues[ii] == null)
        rootIdxTable.stop();
    }
    if(isHyperBlock)
    { for(int ii=0; ii < sizeBlock; ii++)
      { assert1(aValues[ii] instanceof IndexMultiTable_Table && !(aValues[ii] instanceof IndexMultiTable));
        IndexMultiTable_Table<Key, Type> childtable = (IndexMultiTable_Table<Key, Type>)aValues[ii]; 
        assert1(aKeys[ii].equals(childtable.aKeys[0])); //check start key is equal first key in sub table.
        assert1(childtable.ixInParent == ii);           //check same index in sub table.
        sizeAllCheck += childtable.check();  //recursively call of check.
      }
    } else {
      sizeAllCheck = this.sizeBlock; //elements to return..
    }
    assert1(sizeAllCheck == this.sizeAll);  //regarded to this (sub-) table.
    for(int ii=sizeBlock; ii < maxBlock; ii++) //check rest of table is empty.
    { assert1(aKeys[ii] == rootIdxTable.maxKey__);
      assert1(aValues[ii] == null);
    }
    return sizeAllCheck;
  }





  private static void assert1(boolean cond)
  {
    if(!cond)
    { Debugutil.stop();
      throw new RuntimeException("IndexMultiTable - is corrupted;");
    }  
  }

  
  


  /**Checks the consistency of the table. This method is only proper for test of the algorithm
   * and assurance of correctness.  
   * @param parentP The parent of this table or null for the top table.
   * @param keyParentP The key of this table in the parents entry. null for top table.
   * @param ixInParentP The position of this table in the parent's table. -1 for top table.
   * @param keylastP The last key from the walking through the last child, minimal key for top table.
   * @return The last found key in order of tables.
   */
  Key checkTable(IndexMultiTable_Table<Key, Type> parentP, Key keyParentP, int ixInParentP, Key keylastP){
    Key keylast = keylastP;
    assert1(parentP == null || keyParentP.equals(aKeys[0]));
    assert1(this.parent == parentP);
    assert1(this.ixInParent == ixInParentP);
    for(int ix = 0; ix < sizeBlock; ++ix){
      assert1(compare(aKeys[ix], keylast) >= 0);
      if(isHyperBlock){
        assert1(aValues[ix] instanceof IndexMultiTable_Table && !(aValues[ix] instanceof IndexMultiTable));
        @SuppressWarnings("unchecked")
        IndexMultiTable_Table<Key,Type> childTable = (IndexMultiTable_Table<Key,Type>)aValues[ix];
        keylast = childTable.checkTable(this, aKeys[ix], ix, keylast);
      } else {
        assert1(!(aValues[ix] instanceof IndexMultiTable_Table && !(aValues[ix] instanceof IndexMultiTable)));
        keylast = aKeys[ix];
      }
    }
    for(int ix=sizeBlock; ix < maxBlock; ix++)
    { assert1(aKeys[ix] == rootIdxTable.maxKey__);
      assert1(aValues[ix] == null);
    }
    return keylast;
  }



  private void toString(StringBuilder u){
    if(sizeBlock ==0){
      u.append("..emptyIndexMultiTable...");
    }
    else if(isHyperBlock){
      for(int ii=0; ii<sizeBlock; ++ii){
        IndexMultiTable_Table<?,?> subTable = (IndexMultiTable_Table<?,?>)aValues[ii];
        subTable.toString(u);
      }
    } else { 
      for(int ii=0; ii<sizeBlock; ++ii){
        u.append(aKeys[ii]).append(", ");
      }
    }
    
  }



  @Override
  public String toString(){
    StringBuilder u = new StringBuilder();
    if(parent !=null){
      u.append("#").append(parent.identParent);
    }
    if(isHyperBlock){ u.append(':'); } else { u.append('='); }
    toString(u);
    return u.toString();
  }
  

  
  

}
