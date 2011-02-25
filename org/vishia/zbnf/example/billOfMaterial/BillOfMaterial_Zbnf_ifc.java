package org.vishia.zbnf.example.billOfMaterial;

/**Example of an interface to show, how a parser result is able to store in a Java instance using org.vishia.zbnf.ZbnfJavaOutput.setContent(...).
 * The output instance is of type ZbnfStore_BillOfMaterial defined here as sub-interface. To store result components the other interfaces defined here are used.
 * The instances to got the data can be of any type, which implements the interfaces. 
 * 
 * @author Hartmut Schorrig
 *
 */
public interface BillOfMaterial_Zbnf_ifc
{
  
  /**Interface to store a position in the list. ZBNF-syntax-component <code>position::=...</code>.  It is returned from {@link BillOfMaterial_Zbnf_ifc.ZbnfStore_BillOfMaterial.new_position()}
   * and used from ...set_position(). 
   */	
  public interface ZbnfStore_position
  {
	/**invoked on ZBNF-result <#?amount>. */  
    void set_amount(int value);
    
    /**invoked on ZBNF-result <#?code>. */  
    void set_code(long value);
    
    /**invoked on ZBNF-result <...?description>. */  
    void set_description(String value);
    
    /**invoked on ZBNF-result <...?value>. */  
    void set_value(String value);
  }
  
  
  /**Interface to store a position in the list. ZBNF-syntax-component <code>order::=...</code>.  It is returned from {@link BillOfMaterial_Zbnf_ifc.ZbnfStore_BillOfMaterial.new_order()}
   * and used from ...set_order(). 
   */	
  public interface ZbnfStore_order
  {
	/**invoked on ZBNF-result <#?order/@part1>. */  
	void set_part1(int value);
    
	/**invoked on ZBNF-result <#?order/@part2>. */  
	void set_part2(int value);
    
	/**invoked on ZBNF-result <#?order/@part3>. */  
	void set_part3(int value);
  }
  
  
  
  /**Interface to store a date in the list. ZBNF-syntax-component <code>date::=...</code>.  It is returned from {@link BillOfMaterial_Zbnf_ifc.ZbnfStore_BillOfMaterial.new_date()}
   * and used from ...set_date(). 
   */	
  public interface ZbnfStore_date
  {
    /**invoked on ZBNF-result <#?order/@part2>. */  
    void set_date(String value);    
  }
  
  
  
  
  /**Interface to store the whole result of parsing. The given instance on call of org.vishia.zbnf.ZbnfJavaOutput.setContent(type, instance ...) have to implement this interface.
   * It is assigned to the main syntax component <code>BillOfMaterial::=...</code> 
   */	
  public interface ZbnfStore_BillOfMaterial
  {
    
    /**invoked on ZBNF-component-result <date>. */  
    ZbnfStore_date new_date();
    
    /**invoked to store ZBNF-component-result <date>. */  
    void set_date(ZbnfStore_date component);
    
    /**invoked on ZBNF-component-result <order>. */  
    ZbnfStore_order new_order();
    
    /**invoked to store ZBNF-component-result <order>. */  
    void set_order(ZbnfStore_order component);
    
    /**invoked on ZBNF-component-result <position>. */  
    ZbnfStore_position new_position();
    
    /**invoked to store ZBNF-component-result <position>. */  
    void set_position(ZbnfStore_position component);
  }
  
}
