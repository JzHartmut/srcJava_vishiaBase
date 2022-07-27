package org.vishia.util;

import java.util.Map;
import java.util.TreeMap;

/**Method to shorten strings to a maximum length.
 * @author Hartmut
 *
 */
public class ShortenString {
  
  
  
  
  
  
  private static class StringNr{
    String name;
    int actNr;
    int maxNr;
    StringNr(String part){
      name = part;
      actNr = 0;
      int range = 24 - part.length();
      maxNr = (int)(Math.pow(10, range))-1;
    }
  }
  
  private final Map<String, StringNr> idxName24 = new TreeMap<String, StringNr>();
  
  private final int maxNrofChars;
  
  public ShortenString(int maxNrofChars){
    this.maxNrofChars = maxNrofChars;
  }
  
  
  public String adjustLength(String inp){
    if(inp.length() <=maxNrofChars) return inp;
    else {
      StringNr nr = adjust2(inp, maxNrofChars-1);
      return nr.name + nr.actNr;
    }
  }

  
  private StringNr adjust2(String inp, int nrofChars){
    String part = inp.substring(0, nrofChars);
    StringNr d = idxName24.get(part);
    if(d == null){
      d = new StringNr(part);
      idxName24.put(part, d);
      return d;
    } else {
      if(d.actNr < d.maxNr){
        d.actNr +=1;
        return d;
      } else {
        return adjust2(inp, nrofChars-1);
      }
    }
    
  }
  
  
  
}
