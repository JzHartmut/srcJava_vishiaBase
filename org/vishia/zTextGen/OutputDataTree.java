package org.vishia.zTextGen;

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
  
  /**Version, history and license.
   * <ul>
   * <li>2013-03-10 Hartmut chg/new: supports superclass content.
   * <li>2012-12-00 Hartmut improved: circular references with @ 1234 (address) to mark it.
   * <li>2012-10-08 created. A presentation of the content of a Java data tree was necessary.
   * </ul>
   * 
   * <b>Copyright/Copyleft</b>:
   * For this source the LGPL Lesser General Public License,
   * published by the Free Software Foundation is valid.
   * It means:
   * <ol>
   * <li> You can use this source without any restriction for any desired purpose.
   * <li> You can redistribute copies of this source to everybody.
   * <li> Every user of this source, also the user of redistribute copies
   *    with or without payment, must accept this license for further using.
   * <li> But the LPGL ist not appropriate for a whole software product,
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
   * 
   */
  static final public int version = 20130310;

  
  
  
  final Map<Integer, Object> processedAlready = new TreeMap<Integer, Object>();
  
  Object data;
  
  
  public void output(int recurs, Object data, Appendable out, boolean bXML) throws IOException {
    int hash = data.hashCode();
    if(processedAlready.get(hash) !=null){ //prevent circular associations
      out.append(" = ").append(data.toString()).append(" (circular=").append("@"+hash).append(")");
      
    } else 
    { processedAlready.put(hash, data);
      out.append(" (").append("@" + data.hashCode()).append(") = ");  ///
      if(recurs > 100){
        recurs +=1;
      }
      if(recurs > 200){
            out.append("\n========================too many recursions\n");
        return;
      }
      Class<?> clazz = data.getClass();
      
      while(clazz !=null){
        
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
            Class<?> type = field.getType();
            out.append(sName).append(" = ");
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
        clazz = clazz.getSuperclass();
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
