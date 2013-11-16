package org.vishia.zbnf.example.billOfMaterial;

import java.util.LinkedList;
import java.util.List;

import org.vishia.zbnf.example.billOfMaterial.BillOfMaterial_Zbnf_ifc.ZbnfStore_date;
import org.vishia.zbnf.example.billOfMaterial.BillOfMaterial_Zbnf_ifc.ZbnfStore_order;
import org.vishia.zbnf.example.billOfMaterial.BillOfMaterial_Zbnf_ifc.ZbnfStore_position;

/**This class contains inner classes to store components of the ZBNF-parse-result using interface methods.
 * 
 * @author Hartmut Schorrig
 * @since 2009-03-21
 * @version first
 *
 */
public class BillofMaterialData_Methods implements BillOfMaterial_Zbnf_ifc.ZbnfStore_BillOfMaterial
{

  
  static class DateAndOrder 
    implements BillOfMaterial_Zbnf_ifc.ZbnfStore_date, BillOfMaterial_Zbnf_ifc.ZbnfStore_order
  {
    /**In the example the part number should be composed with the 3 separate numbers. */
    private int partNumber = 0;
    
    private String date;
    
    
    public void set_date(String value)
    { this.date = value;
    }

    public void set_part1(int value)
    { partNumber |= (value & 0xfff);
    }

    public void set_part2(int value)
    { partNumber |= (value & 0xfff)<<12;
    }

    public void set_part3(int value)
    { partNumber |= (value & 0xff)<<24;
    }
  
  }
  
  DateAndOrder basicData = new DateAndOrder();
  

  
  
  /**ZBNF: class to store values with semantic "position". */
  static class Position implements BillOfMaterial_Zbnf_ifc.ZbnfStore_position
  {
    /**ZBNF: element to store values with semantic "amount" inside syntax-prescript "position". */
    public int amount;
    
    /**ZBNF: element to store values with semantic "code" inside syntax-prescript "position". */
    public long code;
    
    public String value;

    public void set_amount(int value)
    {
      amount = value;
    }

    public void set_code(long value)
    {
      code = value;
      
    }

    public void set_description(String value)
    { //At example: don't store a description
    }

    public void set_value(String value)
    { this.value = value;
    }

    @Override
    public String toString()
    { return "" + amount + " x " + code + " " + value;
    }
    
   
  }
  
  List<Position> positions = new LinkedList<Position>();
  
  public ZbnfStore_date new_date()
  { return basicData;
  }

  public ZbnfStore_order new_order()
  { return basicData;
  }

  public ZbnfStore_position new_position()
  {
    return new Position();
  }

  public void set_date(ZbnfStore_date component)
  { //let empty, because the instance is known. 
  }

  public void set_order(ZbnfStore_order component)
  { //let empty, because the instance is known. 
  }

  public void set_position(ZbnfStore_position component)
  {
    positions.add((Position)component);    
  }

  /**This method should present the content of the object only in a short description, one line,
   * useable to debug view in eclipse or for debug reports.
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString()
  { return "Bill of material, order=0x" + Integer.toHexString(basicData.partNumber) + " from date "
           + basicData.date + ", " + positions.size() + " positions.";
  }
  
  
}
