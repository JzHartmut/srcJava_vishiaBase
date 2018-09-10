package org.vishia.util;

/**This is a standard implementation of the universal interface to mark instances as select-able and to support selection. 
 * It supports up to 32 sources or users for selection. The management of users should be defined
 * at user level. A source is represented by a bit in the 32-bit-value of the mask.
 * Usual only one source may existing.
 * 
 * This class can be used either as super class for anything which should be marked as selected
 * or as composition in that class whereby the data item {@link #selectMask} is the only one data of that composition.
 * 
 * @author Hartmut Schorrig
 *
 */
public class SelectMask implements MarkMask_ifc
{
  /**Version, history and license.
   * <ul>
   * <li>2013-04-28 Hartmut chg: new parameter data left empty here. 
   * <li>2011-11-28 Hartmut creation
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
  public static final int version = 20130428;

  
  protected int selectMask;
  
  @Override public int getMark()
  { return selectMask; }
  
  @Override public int setNonMarked(int mask, Object data)
  { int selectMask1 = selectMask;
    selectMask &= ~mask;
    return selectMask1;
  }
  
  @Override public int setMarked(int mask, Object data)
  { int selectMask1 = selectMask;
    selectMask |= mask;
    return selectMask1;
  }
}
