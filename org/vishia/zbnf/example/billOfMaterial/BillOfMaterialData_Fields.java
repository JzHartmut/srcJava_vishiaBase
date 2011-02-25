package org.vishia.zbnf.example.billOfMaterial;

import java.util.LinkedList;
import java.util.List;


/**This class contains inner classes to store components of the ZBNF-parse-result setting public fields.
 * 
 * @author Hartmut Schorrig
 * @since 2009-03-21
 * @version first
 *
 */
public class BillOfMaterialData_Fields
{
  
  /**ZBNF: class to store values with semantic "order/...". */
  public static class Order
  {
    /**ZBNF: element to store values with semantic "order/@part1". */ 
    public int part1, part2, part3;
  }
  
  /**ZBNF: class to store values with semantic "position". */
  public static class Position
  {
    /**ZBNF: element to store values with semantic "amount" inside syntax-prescript "position". */
    public int amount;
    
    /**ZBNF: element to store values with semantic "code" inside syntax-prescript "position". */
    public int code;
    
    public String description;
    
    public String value;
    
    public String toString()
    { return "" + amount + " x " + code + " " + value + "/" + description;
    }
    
  }
  
  
  
  /**ZBNF: class to store values with semantic "date". */
  public static class Date
  { 
    /**ZBNF: element to store values with semantic "date" inside syntax-prescript "date". */
    public String date;
  
  }
  
  
  
}
