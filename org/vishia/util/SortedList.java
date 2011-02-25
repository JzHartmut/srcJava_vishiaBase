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
/****************************************************************************/
/* Copyright/Copyleft: 
 * 
 * For this source the LGPL Lesser General Public License, 
 * published by the Free Software Foundation is valid.
 * It means:
 * 1) You can use this source without any restriction for any desired purpose.
 * 2) You can redistribute copies of this source to everybody.
 * 3) Every user of this source, also the user of redistribute copies 
 *    with or without payment, must accept this license for further using.
 * 4) But the LPGL ist not appropriate for a whole software product,
 *    if this source is only a part of them. It means, the user 
 *    must publish this part of source,
 *    but don't need to publish the whole source of the own product.
 * 5) You can study and modify (improve) this source 
 *    for own using or for redistribution, but you have to license the
 *    modified sources likewise under this LGPL Lesser General Public License.
 *    You mustn't delete this Copyright/Copyleft inscription in this source file.    
 *
 * @author www.vishia.de/Java
 * @version 2006-06-15  (year-month-day)
 * list of changes: 
 * 2006-05-00: www.vishia.de creation
 *
 ****************************************************************************/

package org.vishia.util;

import java.io.*;
import java.util.*;

import org.vishia.mainCmd.Report;


/**This abstract class supports sorting Objects in a list with a string-oriented key.
 * The class may be implemented by a users class: The users class must 
 * implement the method {@link getKey(Object)}. The considered types of Object should be
 * all Types of instances, there will be added to the SortedList. Typically it may be
 * only one special type. Write at example:
 * <pre>
 * class MySortedList extends SortedList
 * { ...
 *   public String getKey(Object obj)
 *   { MyListItem item = (MyListItem)obj;  //casting
 *     return item.stringxyz;              //returns a string expression from items data.
 *   }
 * }
 * </pre>
 * The method getKey is not defined as method from the stored object, but as method from this SortedList
 * using the object. Therefore the Object should not implement any interface, the evaluation of the object is done here.
 * But the Object have to be from the expected type, otherwise a ClassCastException is thrown when calling getKey(falseObject).
 * <br>    
 * This class based on the interface java.util.List and implements all methods from List. 
 * Inside SortedList there are a association to a list, it may be either an ArrayList 
 * or a LinkedList. Using of LinkedList means, the velocity of sorting search operations 
 * is not so fast in comparision to using a array list, but there are no limits.
 * Default, a LinkedList is created. But by constructor of this class, you can hand over
 * an instance of a List, it may be an Arraylist of the adequate size.
 * It is possible to create implicite Instances of such a SortedList, using the java construct:
 * <pre>
 * List mySortedList = new SortedList(new ArrayList(1000))
 * { public String getKey(Object obj)
 *   { MyListItem item = (MyListItem)obj;  //casting
 *     return item.stringxyz;              //returns a string expression from items data.
 *   }
 * }
 * </pre>
 * In this sample the instance <code>mySortedList</code> is created with the given special 
 * <code>getKey()</code>-Method applicated to the added items on search algoritms,
 * but no extra class declaration is necessary. Just as, the constructor is called. You don't need an extra
 * constructor delegating the input list to the super class. This is a optimized programming. 
 * <br>
 * The binary search algorithm is used. It means, a searching of 1 item in a list of 1000 items
 * needs 10 steps. Inside, Collections.binarySearch() is used.
 * <br>
 * In opposite to TreeMap a SortedList may have more as one object with the same key. 
<hr/>
<pre>
date       who      change
2007-03-07 JcHartmut implements List, now it is useable as 'normal' List from outside.
2007-03-05 JcHartmut The old version of users derivated SortedList should implement the Comparator interface,
                     but now, the Comparators are private inside. Instead a Method getKey() should be implemented.
                     This is more explicit for using. The idea of SortedList is, it is a String based sorting. 
                     The method {@link search(String)} is one of the base methods of this idea. One List can sorting
                     Objects in one sorting order, but the same Objects may be contained in another SortedList in another
                     sorting order. So the SortedList should determine the key for ordering, the sorting should not
                     depends on the sorted Objects itself. This is a concept changing without great changing in the users programming.
                     Only the new Method getKey() should be implemented. The before it required method compare() are not used anymore. 
2007-03-03 JcHartmut Constructor for List definition outside.
2006-01-07 JcHarmut  initial revision
*
</pre>
<hr/>
*/
@SuppressWarnings("unchecked")
abstract public class SortedList implements List
{ 
	private static final long serialVersionUID = 1L;
  private int ix = -2;

  private final List list;
  
  public SortedList()
  { list = new LinkedList(); 
  }

  /** Uses a list outside, may be an java.util.ArrayList or a LinkedList.*/
  public SortedList(List theList)
  { list = theList; 
  }


  public abstract String getKey(Object item);
  
  
  /** The compare Method requested by interface Comparator.
   * The implementation here delegates the decision about the compareable data
   * to the abstract method getKey().
   */
  public int compare(Object o1, Object o2)
  { String item1 = getKey(o1);            //Object in List
    String item2 = getKey(o2);            //Object to compare
    return item1.compareTo(item2);
  }

  /** Instance of a Comparator which compares an Object as item from list
   * with a String search key, used by search() 
   */
  final protected Comparator stringComparator = new Comparator()
  { public int compare(Object o1, Object o2)
		{ return getKey(o1).compareTo(((String)(o2)));
		}
  };
  
  /** Instance of a Comparator which compares an Object as item from list
   * with an input Object, used by add()
   */
  final protected Comparator objectComparator = new Comparator()
  { public int compare(Object o1, Object o2)
		{ return getKey(o1).compareTo(getKey(o2));
		}
  };
  
  
  
  public int getIx(){ return ix; }

  
  /** Returns the actual size of the list (nr of stored Objects).
   * 
   * @return
   */
  public int size()
  { return list.size();
  }
  
  
  /** Adds only an Object if the key is not already content in list,
   * returns false otherwise.   
   * @param The Object to add.
   * @return true if success, false if an object with the same key is content always.
   * @throws SortedListException
   */
  public boolean addNew(Object obj)
  //throws SortedListException
  { ix = Collections.binarySearch(list, obj, objectComparator);
    if(ix >=0) return(false);   //element is already contend
    else
    { list.add(-ix-1, obj);
      return true;              //added
    }
  }



  /** Adds an Object independend from the existence of a object with the same key in list.
   * @param The Object to add.
   * @return true if success, false if an object with the same key is content always.
   * @throws SortedListException
   */
  public boolean add(Object obj)
  //throws SortedListException
  { ix = Collections.binarySearch(list, obj, objectComparator);
    if(ix >=0) list.add(ix, obj);
    else       list.add(-ix-1, obj); //add after the element with lower key.
    return true;              //always added
  }







  public boolean addAll(Collection list)
  { Iterator ii = list.iterator();
    while(ii.hasNext())
    try{ addNew(ii.next()); } //classCastException may be occurs if the lists are not syntonic.
    catch(Exception ee){} //this exception will be ignored.
    return true;
  }




  /** Searches the first Object with the given Key.
   * 
   * @param sKey
   * @return
   */
  public Object get(String sKey)
  { ix = Collections.binarySearch(list, sKey, stringComparator);
    if(ix<0) return null;
    else return list.get(ix);
  }

  
  public int search(String sKey)
  { ix = Collections.binarySearch(list, sKey, stringComparator);
    return ix;
  }  
  
  /** returns the Object at the given index position. The index may be returned
   * typically by calling {@link search(String)}. If the ix ist negative, the Object
   * before in key order is returned. If the key is less than all Objects, it is
   * if the ix is -1, null will be returned 
   * @param ix Index of the element or negativ Index
   * @return
   */
  public Object get(int ix)
  { if(ix < 0){ ix = -ix-2; }
    if(ix < 0) return null;
    else return list.get(ix);
  	
  }
  
  
  
  public void report(Report report, String sLine)
  { report.reportln(1,"--------------------- SortedList: " + sLine + "---------------------------------------");
    Iterator iList = list.iterator();
    while(iList.hasNext())
    { Object item = iList.next();
      report.reportln(1,item.toString());
    }
    report.reportln(1,"-------------------------------------------------------------------------");
  }

  /**
   * 
   * @param fileOut
   * @throws IOException
    @deprecated it is from the old implementation ... TODO
   */
  public void writeTextFile(File fileOut)
  throws IOException
  { FileOutputStream fOut = new FileOutputStream(fileOut);
    Iterator iList = list.iterator();
    while(iList.hasNext())
    { Object item = (iList.next());
      String sLine = item.toString();
      fOut.write((sLine + "\n").getBytes());
    }
  }

  /**
    adds the textual representation of the list content from a file to the list. To convert
    the textual represantation into the binary image, the overloaded method 'setFromStringLine()' is used.
    The type of the elements in the list to be added were from baseclass 'SortedListItem', but were get
    by calling typeElement.newInstance(), so the requested type of element with the appropriate convert method
    is added.
    @param typeElement Class object to build a new instance for a new element of the list
    //@param cloneItem a blank instance of the item with the requested type
    @deprecated it is from the old implementation ... TODO
  */

  public void addTextFile(File fileIn, Class typeElement) //SortedListItem cloneItem)
  throws IOException//, SortedListException
  { BufferedReader fIn =  new BufferedReader(new InputStreamReader(new FileInputStream(fileIn)));   /**file to read*/
    boolean bContinue = true;
    while(bContinue)   //false if sLine no appendable to sSrc or eof
    { String sLine = fIn.readLine();
      if(sLine == null) bContinue = false;
      else
      { Object item = null;
        try
        { item = (typeElement.newInstance());  //cloneItem.getBlankCopy();  //get a copy
        } catch(Exception exception){ throw new RuntimeException("addTextFile: InstantiationException"); }
        //TODO item.setFromStringLine(sLine);
        addNew(item);
      }
    }
  }


  /** Gets an iterator through the associated list.
   * With the iterator the elements are returned in the sorted order,
   * because there are inputted in sorted order managed by this class.
   */
  public Iterator iterator()
  { return list.iterator();
  }

  
  /** This Method throws a RuntimeException because the adding to the list 
   * at a specified position is not concept of the SortedList.
   */
	public void add(int index, Object element)
	{ throw new RuntimeException("SortedList.add(int index, Object) is not supported");
	}

  /** This Method throws a RuntimeException because the adding to the list 
   * at a specified position is not concept of the SortedList.
   */
	public boolean addAll(int index, Collection c)
	{ throw new RuntimeException("SortedList.addAll(int index, Collection) is not supported");
	}

	
	/**Delegates to List inside, see java.util.List. */
	public void clear()
	{ list.clear();
	}

	/**Delegates to List inside, see java.util.List. */
	public boolean contains(Object o)
	{return list.contains(o);
	}

	/**Delegates to List inside, see java.util.List. */
	public boolean containsAll(Collection c)
	{ return list.containsAll(c);
	}

	/**Delegates to List inside, see java.util.List. */
	public int indexOf(Object o)
	{ return list.indexOf(o);
	}

	/**Delegates to List inside, see java.util.List. */
	public boolean isEmpty()
	{ return list.isEmpty();
	}

	/**Delegates to List inside, see java.util.List. */
	public int lastIndexOf(Object o)
	{ return list.lastIndexOf(o);
	}

	/**Delegates to List inside, see java.util.List. */
	public ListIterator listIterator()
	{ return list.listIterator();
	}

	/**Delegates to List inside, see java.util.List. */
	public ListIterator listIterator(int index)
	{ return list.listIterator(index);
	}


	/**Delegates to List inside, see java.util.List. */
	public Object remove(int index)
	{ return list.remove(index);
	}

	/**Delegates to List inside, see java.util.List. */
	public boolean remove(Object o)
	{ return list.remove(o);
	}

	/**Delegates to List inside, see java.util.List. */
	public boolean removeAll(Collection c)
	{ return list.remove(c);
	}

	/**Delegates to List inside, see java.util.List. */
	public boolean retainAll(Collection c)
	{ return list.retainAll(c);
	}

  /** This Method throws a RuntimeException because the adding to the list 
   * at a specified position is not concept of the SortedList.
   */
	public Object set(int index, Object element)
	{ throw new RuntimeException("SortedList.set(int index, Object) is not supported");
	}

	/**Delegates to List inside, see java.util.List. */
	public List subList(int fromIndex, int toIndex)
	{ return list.subList(fromIndex, toIndex);
	}

	/**Delegates to List inside, see java.util.List. */
	public Object[] toArray()
	{ return list.toArray();
	}

	/**Delegates to List inside, see java.util.List. */
	public Object[] toArray(Object[] a)
	{ return list.toArray(a);
	}


}




  