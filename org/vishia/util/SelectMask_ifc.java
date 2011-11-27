package org.vishia.util;

/**This is a universal interface to mark instances as select-able and to support selection. 
 * It supports up to 32 sources or users for selection. The management of users should be defined
 * at user level. A user is represented by a bit in the 32-bit-value of the mask.
 * Usual only one user may existing. The selection from 2 or less more sides are able to think.
 * The class {@link SelectMask} is the standard implementation. A derived class can inheritance 
 * from that class if another superclass isn't necessary. This may be usual for small data classes.
 *  
 * @author Hartmut Schorrig
 *
 */
public interface SelectMask_ifc
{
  public int getSelection();
  
  public int setDeselect(int mask);
  
  public int setSelect(int mask);

}
