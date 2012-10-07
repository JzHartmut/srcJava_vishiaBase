package org.vishia.textGenerator;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Iterator;

/**Writes a data tree to a text file maybe in XML format.
 * @author Hartmut Schorrig
 *
 */
public class OutputDataTree {
  
  
  Object data;
  
  
  public void output(int recurs, Object data, Appendable out, boolean bXML) throws IOException{
    Class<?> clazz = data.getClass();
    Field[] fields = clazz.getDeclaredFields();
    for(Field field: fields){
      for(int i = 0; i < recurs; ++i){
        out.append(" |");
      }
      out.append(" +-");
      out.append(field.getName()).append(" = ");
      try{ Object dataField = field.get(data);
        outData(recurs, dataField, out, bXML);
      } catch(Exception exc){
        out.append("not accessible");
      }
    }
  }
  
  
  private void outData(int recurs, Object dataField, Appendable out, boolean bXML) throws IOException{
    if(dataField == null){ out.append("null\n"); }
    else if(dataField instanceof String){ out.append((String)dataField).append("\n"); }
    else if(dataField instanceof Iterable<?>){
      Iterator<?> iter = ((Iterable<?>)dataField).iterator();
      while(iter.hasNext()){
        Object iterData = iter.next();
        outData(recurs+1, iterData, out, bXML);
      }
    }
    else {
      out.append("\n");
      output(recurs+1, dataField, out, bXML);
    }
    
  }
  
}
