package org.vishia.util;

/**This is a universal interface to mark instances to support selection. 
 * It supports up to 32 sources or users for mark. The management of users should be defined
 * at user level. A source is represented by a bit in the 32-bit-value of the mask.
 * Usual only one source for selection may existing. 
 * It is possible that two or less more sources do a mark independently.
 * 
 * The class {@link SelectMask} is the standard implementation. A derived class can inheritance 
 * from that class if another superclass isn't necessary. This may be usual for small data classes.
 * <br><br>
 * <b>Usage of the oData parameter</b>:<br>
 * <pre>
 * Application
 *     |--------->MarkMask_ifc
 *     |                |<|----Implementor
 *                      |      maybe a table container
 *                      |             |--oData---------->SelectMask_ifc
 *                                                           |<|-----------SecondImplementor
 *                                                           |             maybe an Object
 *                                                                         which should be
 *                                                                         selected
 * </pre> 
 * The oData maybe instance of SelectMask_ifc, but maybe another one, depends on the first Implementor.
 * @author Hartmut Schorrig
 *
 */
public interface MarkMask_ifc
{
  
  /**Version, history and license.
   * <ul>
   * <li>2013-04-28 Hartmut rename: This interface is called now 'MarkMask_ifc' instead 'SelectMask_ifc'.
   *   It is a problem of wording: The instances are only marked, not yet 'selected'. See application
   *   of this interface in {@link org.vishia.gral.widget.GralFileSelector}: A selected line is the current one
   *   or some marked lines.
   * <li>2013-04-28 Hartmut chg: Use the mask for selection of mark bits.   
   * <li>2013-04-28 Hartmut chg: new parameter data proper to use. Adaption necessary. Left it empty if it don't need. 
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

  
  /**Returns the selection mask of this object. If the returned value is 0, the object is not selected.
   * If it is not 0, any source (represented by each bit) has selected this source. 
   * @return bits of selection, 0 if it is not selected.
   */
  public int getMark();
  
  
  /**Removes the mark bits of the object for the given source.  
   * @param mask The bit which presents the source.
   * @param oData data which are given with this mark. Maybe the mark should be done
   *   in this data too. It depends on usage and implementation.
   * @return The mark bits before this action is done. 
   */
  public int setNonMarked(int mask, Object data);
  
  /**Sets the mark bits of the object for the given source.  
   * @param mask The bit which presents the source.
   * @param oData data which are given with this mark. Maybe the mark should be done
   *   in this data too. It depends on usage and implementation.
   * @return The mark bits before this action is done. If it is 0, this source is the only one which has marks the object. 
   */
  public int setMarked(int mask, Object data);

}
