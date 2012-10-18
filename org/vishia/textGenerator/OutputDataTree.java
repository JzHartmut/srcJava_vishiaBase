package org.vishia.textGenerator;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

/**Writes a data tree to a text file maybe in XML format.
 * @author Hartmut Schorrig
 *
 */
public class OutputDataTree {
  
  final Map<Integer, Object> processedAlready = new TreeMap<Integer, Object>();
  
  Object data;
  
  
  public void output(int recurs, Object data, Appendable out, boolean bXML) throws IOException {
    int hash = data.hashCode();
    if(processedAlready.get(hash) !=null){ //prevent circular associations
      out.append(" circular -->").append("@"+hash);
      
    } else 
    { processedAlready.put(hash, data);
      if(recurs > 100){
        recurs +=1;
      }
      if(recurs > 200){
            out.append("\n========================too many recursions\n");
        return;
      }
      Class<?> clazz = data.getClass();
      Field[] fields = clazz.getDeclaredFields();
      for(Field field: fields){
        int modi = field.getModifiers();
        if((modi & Modifier.STATIC)==0){
          field.setAccessible(true);
          outIndent(recurs, out);
          String sName = field.getName();
          if(sName.equals("whatisit")){
            stop();
          }
          out.append(sName).append(" = ");
          Class<?> type = field.getType();
          if(type.isPrimitive()){
            try{
              String name = type.getName();
              if(name.equals("int")){
                out.append("" + field.getInt(data));
              } else if(name.equals("short")){
                out.append("" + field.getShort(data));
              } else if(name.equals("byte")){
                out.append("" + field.getByte(data));
              } else if(name.equals("boolean")){
                out.append("" + field.getBoolean(data));
              } else if(name.equals("char")){
                out.append("" + field.getChar(data));
              } else if(name.equals("float")){
                out.append("" + field.getFloat(data));
              } else if(name.equals("double")){
                out.append("" + field.getDouble(data));
              } else if(name.equals("long")){
                out.append("" + field.getLong(data));
              }
            }catch (Exception exc){
              out.append(" ?access ").append(exc.getMessage());
            }
          } else {
            try{ Object dataField = field.get(data);
              outData(recurs, field.getName(), dataField, out, bXML);
            } catch(Exception exc){
              out.append("not accessible");
            }
          }
        }
      }
    }
    //out.append("\n");
  }
  
  
  void outIndent(int recurs, Appendable out) throws IOException{
    out.append("\n");
    for(int i = 0; i < recurs; ++i){
      out.append(" |");
    }
    out.append(" +-");
  }
  
  
  
  private void outData(int recurs, String sName, Object dataField, Appendable out, boolean bXML) throws IOException{
    if(dataField == null){ out.append("null"); }
    else if(dataField instanceof Iterable<?>){
      Iterator<?> iter = ((Iterable<?>)dataField).iterator();
      while(iter.hasNext()){
        outIndent(recurs+1, out); out.append(sName).append("[]");
        Object iterData = iter.next();
        outData(recurs+1, "", iterData, out, bXML);
      }
    } 
    else if(dataField instanceof CharSequence){
      appendContent(dataField, out);
    } 
    //else if(dataField)
    else {
      output(recurs+1, dataField, out, bXML);
    }
    
  }
  
  
  private void appendContent(Object data, Appendable out) throws IOException{
    String content = data.toString();
    content = content.replace("\r\n", "|").replace("\n", "|");
    out.append(content);
  }
  
  
  void stop(){}
}
