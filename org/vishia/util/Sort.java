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

public class Sort
{

  /**Adequate to {@link bubbleSort(long[] data)}
   * 
   * @param data
   * @return
   */  
  public static int[] bubbleSort(int[] data)
  {
    int[] idxData = new int[data.length +1];
    int[] idxSrc = new int[data.length];
    int idx;
    for(idx=0; idx < data.length; idx++)
    { idxSrc[idx] = idx;
    }
    int nrofSwappings =0;
    { //bubblesort-Algorithmus aus Wikipedia.
      boolean bSwapped;
      int nrofTests = data.length -1;
      do
      { bSwapped = false;
        for(idx = 0; idx < nrofTests; idx++)
        { if(data[ idx ] > data[ idx + 1 ])
          { int temp = data[ idx ];
            data[ idx ] = data[idx+1];
            data[idx+1] = temp;
            int tempIdx = idxSrc[ idx ];
            idxSrc[ idx ] = idxSrc[idx+1];
            idxSrc[idx+1] = tempIdx;
            bSwapped = true;
            nrofSwappings +=1;
          }
        }
        nrofTests -= 1;
      }while(bSwapped);
    }  
    //The idxSrc contains the indices in sorted order,
    //but the result should contain the dst indices to insort the src elements.
    //Therefore the order of idxSrc must place in the element idxSrc[]
    for(idx= 0; idx < idxSrc.length; idx++)
    { idxData[idxSrc[idx]] = idx;
    }
    idxData[data.length] = nrofSwappings;
    return idxData;
  }

  /**Sorts the input array, returns the index in a new output array.
   * The input array may be only a copy of the relevant sorting informations of a larger information set.
   * The algorithm returns an array of indices to sort the whole information.
   * <br>
   * At example, an input set contains the values <code>{{ 5, "e"}, { 2, "b"}, { 7, "g"}, { 1, "a"}}</code><br>
   * The input array for this method should contain the sorting relevant information: <code>{5, 2, 7, 1 }</code><br>
   * A temporary result will contain the indices of the information in sorted order<code>{3, 1, 0, 2 }</code><br>
   * But the result will contain the indices to copy to a dst array if the src information is taken in its given order<code>{2, 1, 3, 0 }</code><br>
   * To create a sorted whole data set use the indices and copy the source elements in this order:<br>
   * by reading in given order and copy to index 2, 1, 3, 0, The result will be:<code>{{ 1, "a"}, { 2, "b"}, { 5, "e"}, { 7, "g"}}</code>
   * 
   * @param data Array with some long numbers unsorted.
   * @return Array with the indices to write the data in a dst container. 
   *         The length of the return array is equal the length of the data array + 1.
   *         The last element contains the number of sorting steps.
   */
  public static int[] bubbleSort(long[] data)
  {
    int[] idxData = new int[data.length +1];
    int[] idxSrc = new int[data.length];
    int idx;
    for(idx=0; idx < data.length; idx++)
    { idxSrc[idx] = idx;
    }
    int nrofSwappings =0;
    { //bubblesort-Algorithmus aus Wikipedia.
      boolean bSwapped;
      int nrofTests = data.length -1;
      do
      { bSwapped = false;
        for(idx = 0; idx < nrofTests; idx++)
        { if(data[ idx ] > data[ idx + 1 ])
          { long temp = data[ idx ];
            data[ idx ] = data[idx+1];
            data[idx+1] = temp;
            int tempIdx = idxSrc[ idx ];
            idxSrc[ idx ] = idxSrc[idx+1];
            idxSrc[idx+1] = tempIdx;
            bSwapped = true;
            nrofSwappings +=1;
          }
        }
        nrofTests -= 1;
      }while(bSwapped);
    }  
    //The idxSrc contains the indices in sorted order,
    //but the result should contain the dst indices to insort the src elements.
    //Therefore the order of idxSrc must place in the element idxSrc[]
    for(idx= 0; idx < idxSrc.length; idx++)
    { idxData[idxSrc[idx]] = idx;
    }
    idxData[data.length] = nrofSwappings;
    return idxData;
  }
  
  /**Only use in debugging (eclipse or so) to test the algorithm.*/
  public static int[] testBubbleSort()
  { long[] src = { 5,2,7,1};
    int[] dst = bubbleSort(src);
    return dst;
  }
  
}
