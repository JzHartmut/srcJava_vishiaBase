package org.vishia.util;

/**This is a very simple class to store an Object reference with a name for example in a list
 * to give them a semantic.
 * @author hartmut Schorrig www.vishia.org
 *
 */
public class NameObj {

  
  public final String name;
  public final Object data;
  
  public NameObj(String name, Object data) {
    this.name = name;
    this.data = data;
  }
  
  
}
