package org.vishia.zbnf.example.billOfMaterial;

import java.util.LinkedList;
import java.util.List;

/**This class is used to store a ZBNF parse result directly using the {@link org.vishia.zbnf.ZbnfJavaOutput}.
 * 
 * @author Hartmut Schorrig
 * @since 2009-03-21
 * @version first
 *
 */
public class BillOfMaterialZbnf_Fields
{
  
  
  /**ZBNF: instance to store values with semantic "order/...". */
  public BillOfMaterialData_Fields.Order order = new BillOfMaterialData_Fields.Order();
  
  
  
  /**ZBNF: instance to store values with semantic "position". 
   * It is a List because there is a repetition in syntax, more as one value are possible.
   */
  public final List<BillOfMaterialData_Fields.Position> position = new LinkedList<BillOfMaterialData_Fields.Position>();
  
  
  
  
  /**ZBNF: instance to store values with semantic "date". */
  public BillOfMaterialData_Fields.Date date;
  
  
  
  /**This method should present the content of the object only in a short description, one line,
   * useable to debug view in eclipse or for debug reports.
   * @see java.lang.Object#toString()
   */
  public String toString()
  { return "Bill of material, order=" + order.part1 + "." + order.part2 + "." + order.part3 + " from date "
           + date.date + ", " + position.size() + " positions.";
  }
  
}
