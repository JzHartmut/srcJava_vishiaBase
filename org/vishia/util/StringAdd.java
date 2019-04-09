package org.vishia.util;


/**This is a small helper class to add something to build a String for optimized calculate time and space effort.
 * Note: The String s += add needs internally a StringBuilder. This class uses it explicitly with lesser effort.
 * @author Hartmut Schorrig, License LPGL
 *
 */
public final class StringAdd {

  private StringBuilder u;
  private CharSequence cs;
  private int size;
  
  
  /**The ctor does not create a StringBuilder. 
   * @param defaultSizeBuffer Only stored if necessary.
   */
  public StringAdd(int defaultSizeBuffer){
    this.size = defaultSizeBuffer;
  }
  
  
  /**Creates or returns the StringBuilder Buffer. */
  private StringBuilder buffer() {
    if(u == null) { 
      u = new StringBuilder(size);
      if(cs !=null) { u.append(cs); }
      cs = u;
    }
    return u;
  }
  
  
  /**Adds a String. Note: 'add' is shorten to write and read as 'append'
   * If it is the first and only one operation, src will be stored and returned.
   * Note that src may not be persistent. 
   * @param src
   */
  public StringAdd add(CharSequence src) {
    if(cs == null) { cs = src; }
    else { buffer().append(src);}
    return this;
  }
  
  
  /**Adds a Character. Note: 'add' is shorten to write and read as 'append'
   * It creates a StringBuilder buffer internally.
   * @param src
   */
  public StringAdd add(char cc) {
    buffer().append(cc);
    return this;
  }
  
  
  /**Returns either src from only one add or the internal StringBuilder.
   * It has lesser effort than toString, if a CharSequence is sufficient to process the content.*/
  public CharSequence get() { return cs; }
  
  
  /**Returns either the only one added CharSequence src or the content of the internal Buffer (StringBuilder).
   * @see java.lang.Object#toString()
   */
  @Override public String toString() { return cs.toString(); }
}
