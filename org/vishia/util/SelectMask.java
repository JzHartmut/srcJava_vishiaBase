package org.vishia.util;

/**This is a universal interface to mark instances as select-able and to support selection. 
 * It supports up to 32 sources or users for selection. The management of users should be defined
 * at user level. A user is represented by a bit in the 32-bit-value of the mask.
 * Usual only one user may existing. The selection from 2 or less more sides are able to think.
 * 
 * @author Hartmut Schorrig
 *
 */
public class SelectMask implements SelectMask_ifc
{
  int selectMask;
  
  @Override public int getSelection()
  { return selectMask; }
  
  @Override public int setDeselect(int mask)
  { int selectMask1 = selectMask;
    selectMask &= ~mask;
    return selectMask1;
  }
  
  @Override public int setSelect(int mask)
  { int selectMask1 = selectMask;
    selectMask |= mask;
    return selectMask1;
  }
}
