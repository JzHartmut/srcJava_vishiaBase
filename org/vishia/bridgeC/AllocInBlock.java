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
package org.vishia.bridgeC;

import org.vishia.msgDispatch.LogMessage;


/**This class supports allocation in a given block with equal size.
 * In Java it executes a normal new operation. The feature of using blocks with equal size 
 * is a concept of C embedded programming. The class is a counterpart to the Block-Heap-Concept 
 * of the CRuntimeJavalike.
 * <br>
 * All methods are called with a parent. The parent is any Object, to which the array may associated.
 * The concept in C with blockHeap is: The Object uses a part of a block, the array should use 
 * the free spaces in the same block. In Java it is not regarded. 
 * <br>
 * The size-Parameter may be -1.
 * Than in C the rest of memory is used, the size of the returned array depends of the amount 
 * of memory. The algorithm using the returned array have to check the size and have to regard
 * several sizes. It is possible that the functionality of a system depends of the amount of memory
 * in a block. It is a possible and useable way in less embedded systems or for some functionalities. 
 * <br>
 * At example see {@link org.vishia.util.IndexMultiTable}: 
 * The size of one table depends on this system property. It uses the block heap optimal. 
 * But the functionality of IndexMultiTable doesn't depend on it. Using less or greater tables
 * influences only the calculation time in less kind, not the algorithm itself.
 * <br>
 * If StringBuffer are used in this kind, and the increasing of its size isn't able, 
 * a limitation of string size is given.
 * <br>
 * @author Hartmut Schorrig
 *
 */
public class AllocInBlock
{

  static int defaultSize = 400;
  
  public static final int checkGcBlockUsed = 0;
  public static final int checkGcBlockFreed = 1;
  public static final int checkGcFinished = 2;
  
  
  /**Returns the rest of available bytes in the block, which contains parent.
   * For java using it isn't relevant, the method returns a constant value between 400..2000.
   * The method is relevant for C to calculate some array sizes.
   * @param parent The object which is allocated already in the block. If parent isn't allocated
   * in a block of BlockHeap, this method returns the amount of bytes in a new block.
   * @return
   */
  public static int restSizeBlock(Class<?> parent_type, int sizeBlock)
  { return 1000;
  }
  
  
  /**Returns the sizeof a pointer in the C implementation. In Java it returns a constant of 4.
   * This method is able to use to calculate the space for pointer arrays (references in Java).
   */
  public static int sizeofPointer()
  { return 4;
  }
  
  
  public static byte[] allocByteArray(Object parent, int size)
  { if(size == -1)
    { size = defaultSize;
    }
    return new byte[size];
  }
  
  public static char[] allocCharArray(Object parent, int size)
  { if(size == -1)
    { size = defaultSize;
    }
    return new char[size];
  }
  
  public static StringBuffer allocStringBuffer(Object parent, int size)
  { if(size == -1)
    { size = defaultSize;
    }
    return new StringBuffer(size);
  }
  
  public static int[] allocIntArray(Object parent, int size)
  { if(size == -1)
    { size = defaultSize/4;
    }
    return new int[size];
  }
  
  public static short[] allocShortArray(Object parent, int size)
  { if(size == -1)
    { size = defaultSize/2;
    }
    return new short[size];
  }
  
  public static long[] allocLongArray(Object parent, int size)
  { if(size == -1)
    { size = defaultSize/8;
    }
    return new long[size];
  }
  
  public static float[] allocFloatArray(Object parent, int size)
  { if(size == -1)
    { size = defaultSize/4;
    }
    return new float[size];
  }
  
  public static double[] allocDoubleArray(Object parent, int size)
  { if(size == -1)
    { size = defaultSize/8;
    }
    return new double[size];
  }
  
  public static Object[] allocObjectArray(Object parent, int size)
  { if(size == -1)
    { size = defaultSize/4;
    }
    return new Object[size];
  }
  
  
  /**Sets the run mode for all instances of BlockHeapJc. Any new operation uses the block heap from up to now.
   * In Java it is an empty not necessary instruction, because all actions are done in normal heap.
   * In C this routine have to be implement from the user, because it is situational to all user-defined block heaps. 
   */
  public static void setRunModeAll()
  { /**empty in Java*/
  }
  
  
  public static void setLogMessageOutput(LogMessage output, int baseNumber)
  { //empty in Java. It is the log output for the Blockheap in C
  }
  
  /**Organizes 1 step for garbage collection. It is thought for the CRuntimeJavalike - BlockHeap.
   * The garbage collector may be run in a users thread. Therefore it is able to call with this method.
   * @param bUserCall false for the normal garbage collector call.
   */
  public static int garbageCollection_BlockHeapJc(boolean bUserCall){
  	//empty in Java. It is to organize the GC in C. 
  	return checkGcFinished;
  }
  
}
