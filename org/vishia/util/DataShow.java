package org.vishia.util;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.vishia.xmlSimple.SimpleXmlOutputter;

/**This class supports output of the content of any instance maybe with associated other Objects.
 * It shows the data structure of a class with its content as a snap shoot.
 * @author Hartmut Schorrig
 *
 */
public class DataShow extends ObjectId
{
  /**Version, history and license.
   * <ul>
   * <li>2017-07-29 Hartmut new: {@link #outData(Object, Appendable, boolean)} can decide whether the true hash code is shown 
   *   (for search the instance proper to a Object.toString()-output with @hash or a unique hash is shown for compare ability of 2 html files.j 
   * <li>2015-11-03 Hartmut new: possibility of hyperlink to any instance via toString-anchor. 
   * <li>2015-08-01 Hartmut chg: Instead hash-code as link label use the {@link #instanceId(Object)}. The advantage:
   *   If the same data are used in a second session (repeated invocation of the same for example conversion process)
   *   then the identification is the same. The hash code depends on the memory address, which depends on the situation
   *   on the operation system. With them the generated html outputs are able to compare by a text difference tool.
   *   With that intension the standard toString() output are changed, instead "type@address" "type@instanceId" is replaced. 
   * <li>2014-11-09 Hartmut new: with html output
   * <li>2014-07-24 Hartmut chg: Argument maxRecurs necessary.
   * <li>2014-07-24 Hartmut chg: Move the algorithm from to this class org.vishia.util.DataShow. It is an own topic.
   * <li>2014-06-29 Hartmut chg move the algorithm form srcJava_Zbnf:/org/vishia/zcmd/OutputDataTree to {@link org/vishia/jzcmd/JZcmdTester}.
   * <li>2013-07-28 Hartmut chg/new: dataTree now writes XML
   * <li>2013-03-10 Hartmut chg/new: dataTree supports superclass content.
   * <li>2012-12-00 Hartmut improved: dataTree circular references with @ 1234 (address) to mark it.
   * <li>2012-10-08 created. dataTree A presentation of the content of a Java data tree was necessary.
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
   * <li> But the LPGL is not appropriate for a whole software product,
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
  static final public String sVersion = "2017-07-29";

  /**If a Field is designated with this annotation then the content is not shown.
   * This is for example for a Hash map because the key values may be hash codes which are abbreviated between several runs.
   */
  @Retention(RetentionPolicy.RUNTIME)
  public @interface ExcludeShowContent{ }

  
  //private final List<Object> listData = new LinkedList<Object>();
  
  
  public static void dataTreeXml(Object data, Appendable out, int maxRecurs) throws IOException {
    out.append("<?xml version=\"1.0\" encoding=\"windows-1252\"?>\n"); 
    out.append("<!-- written with org.vishia.jbat.OutputDataTree -->\n"); 
    out.append("<data "); 
    final Map<String, Object> processedAlready = new TreeMap<String, Object>();
    outData(0, maxRecurs, data, out, true, processedAlready);
    out.append("\n</data>\n"); 
  }
  
  
  
  public static void dataTree(Object data, Appendable out, int maxRecurs) throws IOException {
    final Map<String, Object> processedAlready = new TreeMap<String, Object>();
    outData(0, maxRecurs, data, out, false, processedAlready);
  }
  
  
  
  /**Output of a complex data class.
   * @param recurs
   * @param data
   * @param out
   * @param bXML
   * @throws IOException
   * @return true: one line data, end with "/ >", false: more as one line.
   */
  private static boolean outData(int recurs, int maxRecurs, Object data, Appendable out, boolean bXML, Map<String, Object> processedAlready) throws IOException {
    boolean bOneline = false;
    String hash = Integer.toHexString(data.hashCode());
    if(bXML){
      out
      .append(" hash=\"").append(hash)
      .append("\" objtype=\"").append(data.getClass().getName())
      .append("\" toString=\"").append(SimpleXmlOutputter.convertString(data == null ? "null" : data.toString()))
      .append("\""); 
    }
    if(processedAlready.get(hash) !=null){ //prevent circular associations
      if(bXML){
        out.append(" circular=\"").append("@"+hash).append("\" />"); bOneline = true; 
      } else {
        out.append(" = ").append(data.toString()).append(" (circular=").append("@"+hash).append(")");
      }
    } else { 
      processedAlready.put(hash, data);
      if(!bXML){
        out.append(" (").append("hash=@" + hash).append(") = ");  ///
      }
      if(recurs > 200){
            out.append("\n========================too many recursions\n");
        return false;
      }
      if(bXML){ out.append(" >\n"); }
      Class<?> clazz = data.getClass();
      
      while(clazz !=null){  //for all superclasses too.
        
        Field[] fields = clazz.getDeclaredFields();
        for(Field field: fields){
          Class<?> type = field.getType();
          int modi = field.getModifiers();
          if((modi & Modifier.STATIC)==0){
            field.setAccessible(true);
            outIndent(recurs, out, bXML);
            String sName = field.getName();
            if(sName.equals("whatisit")){
              Debugutil.stop();
            }
            String sType = type.getName();
            if(bXML){
              out.append("<data name=\"").append(sName)
              .append("\" reftype=\"").append(sType)
              .append("\"");
            } else {
              out.append(sName).append(" : ").append(sType).append(" = ");
            }
            if(type.isPrimitive()){
              if(bXML){
                out.append("\" value=\"");
              }
              try{
                if(sType.equals("int")){
                  out.append("" + field.getInt(data));
                } else if(sType.equals("short")){
                  out.append("" + field.getShort(data));
                } else if(sType.equals("byte")){
                  out.append("" + field.getByte(data));
                } else if(sType.equals("boolean")){
                  out.append("" + field.getBoolean(data));
                } else if(sType.equals("char")){
                  out.append("" + field.getChar(data));
                } else if(sType.equals("float")){
                  out.append("" + field.getFloat(data));
                } else if(sType.equals("double")){
                  out.append("" + field.getDouble(data));
                } else if(sType.equals("long")){
                  out.append("" + field.getLong(data));
                }
              }catch (Exception exc){
                out.append(" ?access ").append(exc.getMessage());
              }
              if(bXML){
                out.append("\" />");
              }
            } else {  //non primitive
              try{
                Object elementData = field.get(data);
                if(elementData ==null){
                  if(bXML){ out.append(" value=\"null\" />");} else { out.append("null"); }
                } 
                else if(elementData instanceof CharSequence){
                  if(bXML){ 
                    out.append(" value=\"").append(SimpleXmlOutputter.convertString(elementData.toString())).append("\" />");
                  } else {
                    out.append(elementData.toString());
                  }
                }
                else if(type.isArray()) {  ////
                  Class<?> componentType = type.getComponentType();
                  if(componentType.isPrimitive()) {
                    if(bXML){ 
                      out.append(" primitive array length=\"").append("\" />");
                    } else {
                      out.append(" primitive array length=\"");
                    }
                    
                  } else {
                    Object[] array = (Object[])elementData;
                    if(bXML){ 
                      out.append(" array length=\"").append(Integer.toString(array.length)).append("\" />");
                    } else {
                      out.append(" array length=\"").append(Integer.toString(array.length));
                    }
                    if(recurs < maxRecurs){
                      for(int ixItem = 0; ixItem < array.length; ++ixItem) {
                        Object arrayElement = array[ixItem];
                        outIndent(recurs+1, out, bXML);
                        if(bXML) { out.append("<array-element ");
                        } else { out.append(Integer.toString(ixItem)).append(". Array element: ------------------------------------------------------------------------");
                        }
                        outIndent(recurs+2, out, bXML);
                        outData(recurs+2, maxRecurs, arrayElement, out, bXML, processedAlready);
                        if(bXML){ out.append("</array-element>"); }
                      }
                    }
                  }                  
                }
                else if(elementData instanceof List<?>){
                  List<?> list = (List<?>)elementData;
                  if(bXML){ 
                    out.append(" listsize=\"").append(Integer.toString(list.size())).append("\" />");
                  } else {
                    out.append(" listsize=\"").append(Integer.toString(list.size()));
                  }
                  if(recurs < maxRecurs){
                    int ixItem = -1;
                    for(Object listElement: list){
                      outIndent(recurs+1, out, bXML);
                      if(bXML) { out.append("<List-entry ");
                      } else { 
                        out.append(Integer.toString(ixItem)).append(". List entry: ------------------------------------------------------------------------");
                        outIndent(recurs+2, out, bXML);
                      }
                      outData(recurs+2, maxRecurs, listElement, out, bXML, processedAlready);
                      if(bXML){ 
                        outIndent(recurs+1, out, bXML);
                        out.append("</List-entry>"); 
                      }
                    }
                  }
                } 
                else if(elementData instanceof Map<?,?>){
                  Map<?,?> map = (Map<?,?>)elementData;
                  if(bXML){ 
                    out.append(" mapsize=\"").append(Integer.toString(map.size())).append("\" >");
                  } else {
                    out.append(" mapsize=\"").append(Integer.toString(map.size()));
                  }
                  if(recurs < maxRecurs){
                    int ixEntries = -1;
                    for(Map.Entry<?,?> element: map.entrySet()){
                      Object key = element.getKey();
                      Object value = element.getValue();
                      outIndent(recurs+1, out, bXML);
                      if(bXML){ 
                        out.append("<Map-Entry ix=\"").append(Integer.toString(++ixEntries)).append("\" key=\"").append(key.toString()).append("\" >");
                      } else {
                        out.append(Integer.toString(++ixEntries)).append(". Map-Entry key=").append(key.toString());
                      }
                      if(recurs+1 < maxRecurs){
                        outData(recurs+2, maxRecurs, value, out, bXML, processedAlready);
                      }
                      if(bXML){ out.append("</Map-entry>"); }
                    }
                  }
                  if(bXML){ 
                    out.append("</Map>");
                  }
                } else {
                  if(bXML){ ////
                    out.append(" value=\"").append(SimpleXmlOutputter.convertString(elementData.toString())).append("\" />");
                  } else {
                    out.append(elementData.toString());
                  }
                  if(recurs < maxRecurs){
                    boolean bOneLineElement = outData(recurs+1, maxRecurs, elementData, out, bXML, processedAlready);
                    if(bXML && !bOneLineElement){ outIndent(recurs, out, bXML); out.append("</data>"); }
                  }
                }
              } catch(IllegalAccessException exc){
                out.append(" notAccessible=\"1\" />");
              }
            }
            if(bXML){
              //out.append("\"");
            }
          }
        }
        clazz = clazz.getSuperclass();
      }
    }
    //out.append("\n");
    return bOneline;
  }
  
  
  
  /**Generates a html file which contains the description of all data contained in referred in the given data instance.
   * The given data instance is evaluated with all files, also private, using reflection.
   * All references are evaluated in the same kind. The references are referenced in the html presentation using html links inside the generated file.
   * If data refers a large amount of other instances, maybe with a large list etc then the created file is large. There is no limitation.
   * Circulate references are detected, any Object is presented only one time.
   * @param data Any Object.
   * @param out To write
   * @throws IOException
   */
  public static void outHtml(Object data, Appendable out)
  throws IOException { outHtml(data, out, true); }



  /**Same as {@link #outHtml(Object, Appendable)} but content with or without hash is able to control
   * 
   * @param data
   * @param out
   * @param bNoHash true than a @ hash from toString() will be replaced by the internal id to save compare ability.
   * @throws IOException
   */
  public static void outHtml(Object data, Appendable out, boolean bNoHash)
  throws IOException
  {
    DataShow dataShow = new DataShow();
    out.append("<html>\n<head><title>DataShow</title></head>");
    out.append("\n<body>\n");
    out.append("\n<p>This file shows the content of the given Java instance and all of its associated instances.\n");
    out.append("\nThe associated instances are able to found by local hyper links inside this file.\n");
    out.append("\nOne can navigate with any browser in all associated instances and back with known &lt;alt-back&gt;.\n");
    out.append("\n</p><p>This file was generated by the java class <code>org/vishia/util/DataShow.java</code> mady by Hartmut Schorrig</p>\n");
    out.append("\n<p>You will find any element usual in the form:</p>\n");
    out.append("\n<ul><li>name : type = value</li></ul>\n");
    out.append("\n<p>Container and arrays are listed with their elements.</p>\n");
    if(data == null) {
      out.append("\n<p>null</p>");
    } else {
      dataShow.instanceId(data, dataShow.newInstances);  //registers the top level instance, fill newInstances therewith
      Object refData;
      while( (refData = dataShow.newInstances.poll()) !=null) { //show all instances which are found as reference.
        //while outData is invoked, newInstances is filld with other new instances. 
        dataShow.outData(refData, out, bNoHash);  //may add further referenced data.
      }
    }
    out.append("\n</body>\n</html>\n");
    
  }
  

  
  Queue<Object> newInstances = new ConcurrentLinkedQueue<Object>();
  
  
  
  private DataShow(){
    super();
  }
  
  
  
  /**Output to one element.
   * 
   * @param data
   * @param type
   * @param out
   * @throws IOException
   */
  private void outDataShort(Object data, Class<?> type, Appendable out) throws IOException {
    //Note: don't use the hash code. it may be the best, but the hash produce another content
    //for the same data. Older and newer files are not comparable.
    //faulty: String hash = Integer.toHexString(data.hashCode()); 
    String hash = instanceId(data, newInstances);
    String contentShort = toStringNoHash(data).replace("<", "&lt;");    //< won't be displayed in html.
    out.append(" <a href=\"#obj-").append(hash).append("\">")
       .append(" id=").append(hash)
       .append(" = ").append(contentShort)
       .append("</a> (Instancetype: ")
       .append(type.getName())
       .append(")")
      ;
  }
  
  
  /**Output of a complex data class.
   * @param recurs
   * @param data
   * @param out
   * @param bXML
   * @throws IOException
   * @return true: one line data, end with "/ >", false: more as one line.
   */
  private void outData(Object data, Appendable out, boolean bNoHash) throws IOException {
    //Note: don't use the hash code. it may be the best, but the hash produce another content
    //for the same data. Older and newer files are not comparable.
    //faulty: String hash = Integer.toHexString(data.hashCode());
    String hash = instanceId(data, newInstances);
    Class<?> clazz = data.getClass();
    out.append("\n<a name=\"obj-").append(hash).append("\"/>");
    //anchor for hyperlink from outside for documentation. Use the toString().output.
    String content = (bNoHash ? toStringNoHash(data) : data.toString()).replace("<", "&lt;");    //< won't be displayed in html.
//    if(content !=null && content.indexOf('\"')<0){
//      //often a " is not contained in the toString-output. It can be presumed that it is never true,
//      //if the toString() should be used as anchor. But prevent trouble if " is contained.
//      out.append("\n<a name=\"").append(content).append("\"/>");
//    }
    out.append("\n<hr>\n<h2>");
    out.append(clazz.getName()).append(" id=").append(hash).append("</h2>");
    if(data instanceof Map) {
      ////
      Map<?,?> map = (Map<?,?>)data;
      out.append(" Map length = ").append(Integer.toString(map.size()));
      if(map.size() >0) {
        out.append("\n      <ol start=\"0\">");
        for(Map.Entry<?,?> entry: map.entrySet()) {
          Object item = entry.getValue();
          Object key = entry.getKey();
          String sKey = toStringNoHash(key);
          outField(sKey, item, item.getClass(), null, out, 0);
          /*  
          out.append("\n        <li>");
          if(item ==null) {
            out.append(" = null");
          } else {
            addRef(item);
            outDataShort(item, item.getClass(), item.toString(), out);   
          }
          out.append("</li>");
          */
        }
        out.append("\n      </ol>");
      }
    } else {
      out.append("<p>").append(" = ").append(content).append("</p>");
      out.append("\n  <h3>this</h3>");      
      while(clazz !=null) {  //for all superclasses too.
        out.append("\n    <ul>");
        Field[] fields = clazz.getDeclaredFields();
        for(Field field: fields) {
          ExcludeShowContent doNotShowContent = field.getAnnotation(ExcludeShowContent.class);
          Class<?> type = field.getType();
          int modi = field.getModifiers();
          if((modi & Modifier.STATIC)==0) {
            field.setAccessible(true);
            String sName = field.getName();
            String sType = type.getName();
            String sInfo = sName + ": " + sType;
            if(doNotShowContent != null) {
              out.append("\n    <li>").append(sInfo).append(" = ?not shown? </li> ");
            } else {
              outField(sName, data, type, field, out, 0);
            }
          }
        }
        out.append("\n    </ul>");
        clazz = clazz.getSuperclass();
        if(clazz !=null) {
          out.append("\n  <h3>super ").append(clazz.getName()).append("</h3>");
        }
      }
    }
  }
  



  /**
   * @param sName
   * @param sType
   * @param data If a field is given, then the data where the field is found, else the data itself.
   * @param type Type of a reference or type of instance to show.
   * @param field maybe null
   * @param out
   * @throws IOException
   */
  private void outField(String sInfo, Object data, Class<?> type, Field field, Appendable out, int recursiveCount) throws IOException
  {
    if(recursiveCount >10) return;
    
    
    out.append("\n    <li>");
    if(sInfo !=null && sInfo.length() >0) {
      out.append(sInfo).append(" = ");
    }
    if(type.isPrimitive()){
      try{
        if(type == Integer.TYPE) { //sType.equals("int")){
          out.append("" + field.getInt(data));
        } else if(type == Short.TYPE) { //sType.equals("short")){
          out.append("" + field.getShort(data));
        } else if(type == Byte.TYPE) { //sType.equals("byte")){
          out.append("" + field.getByte(data));
        } else if(type == Boolean.TYPE) { //sType.equals("boolean")){
          out.append("" + field.getBoolean(data));
        } else if(type == Character.TYPE) { //sType.equals("char")){
          out.append("" + field.getChar(data));
        } else if(type == Float.TYPE) { //sType.equals("float")){
          out.append("" + field.getFloat(data));
        } else if(type == Double.TYPE) { //sType.equals("double")){
          out.append("" + field.getDouble(data));
        } else if(type == Long.TYPE) { //sType.equals("long")){
          out.append("" + field.getLong(data));
        }
      }catch (Exception exc){
        out.append(" ?access ").append(exc.getMessage());
      }
    } else {  //non primitive
      try{
        Object elementData = field == null ? data : field.get(data);
        Class<?> instanceType = elementData == null ? null : elementData.getClass();
        
        if(elementData ==null){
          out.append("null");
        } 
        else if(elementData instanceof CharSequence){
          out.append(instanceType.getSimpleName()).append(": ");
          out.append(SimpleXmlOutputter.convertString(toStringNoHash(elementData).replace("<", "&lt;")));    //< won't be displayed in html.
        }
        else if(type.isArray()) {  ////
          Class<?> componentType = type.getComponentType();
          if(componentType.isPrimitive()) {
            out.append(" primitive array length = ");
            
          } else {
            Object[] array = (Object[])elementData;
            out.append(" array length = ").append(Integer.toString(array.length));
            if(array.length >0) {
              out.append("\n      <ol start=\"0\">");
              for(int ixItem = 0; ixItem < array.length; ++ixItem) {
                Object arrayElement = array[ixItem];
                outField("", arrayElement, arrayElement.getClass(), null, out, recursiveCount+1);
                /*
                out.append("\n        <li>");
                if(arrayElement ==null) {
                  out.append(" = null");
                } else {
                  addRef(arrayElement);
                  outDataShort(arrayElement, arrayElement.getClass(), arrayElement.toString(), out);   
                }
                out.append("</li>");
                */
              }
              out.append("\n      </ol>");
            }
          }                  
        }
        else if(elementData instanceof List<?>){
          List<?> list = (List<?>)elementData;
          out.append(" List length = ").append(Integer.toString(list.size()));
          if(list.size() >0) {
            out.append("\n      <ol start=\"0\">");
            for(Object item: list) {
              outField("", item, item.getClass(), null, out, recursiveCount+1);
              /*
              out.append("\n        <li>");
              if(item ==null) {
                out.append(" = null");
              } else {
                addRef(item);
                outDataShort(item, item.getClass(), item.toString(), out);   
              }
              out.append("</li>");
              */
            }
            out.append("\n      </ol>");
          }
        } 
        else if(elementData instanceof Map<?,?>){
          Map<?,?> map = (Map<?,?>)elementData;
          out.append(" Map length = ").append(Integer.toString(map.size()));
          if(map.size() >0) {
            out.append("\n      <ol start=\"0\">");
            for(Map.Entry<?,?> entry: map.entrySet()) {
              Object item = entry.getValue();
              Object key = entry.getKey();
              String sKey = toStringNoHash(key);
              outField(sKey, item, item.getClass(), null, out, recursiveCount+1);
              /*  
              out.append("\n        <li>");
              if(item ==null) {
                out.append(" = null");
              } else {
                addRef(item);
                outDataShort(item, item.getClass(), item.toString(), out);   
              }
              out.append("</li>");
              */
            }
            out.append("\n      </ol>");
          }
        } else {
          outDataShort(elementData, elementData.getClass(), out);
          //out.append(" = ").append(content1);
        }
      } catch(IllegalAccessException exc){
        out.append(" notAccessible=\"1\" />");
      }
    }
    out.append("</li>");
  }


  
  
  
  static void outIndent(int recurs, Appendable out, boolean bXml) throws IOException{
    if(bXml){
      out.append("\n");
      for(int i = 0; i < recurs; ++i){
        out.append("  ");
      }
      out.append("  ");
      
    } else {
      out.append("\n");
      for(int i = 0; i < recurs; ++i){
        out.append(" |");
      }
      out.append(" +-");
    }
  }
  
  
  
  private static boolean XXXoutData(int recurs, String sName, Object dataField, Appendable out, boolean bXML) throws IOException{
    boolean bOneLine = false;
    //if(bXML){ out.append(">"); }
    if(dataField == null){ out.append("null"); }
    else if(dataField instanceof Iterable<?>){
      if(bXML){ out.append(" >"); }
      
      Iterator<?> iter = ((Iterable<?>)dataField).iterator();
      while(iter.hasNext()){
        outIndent(recurs+1, out, bXML); 
        if(bXML){ out.append("<element "); }
        else { out.append(sName).append("[]"); }
        Object element = iter.next();
        if(element == null){
          out.append("value=\"null\" ");
        } else {
          boolean bOneLineElement = XXXoutData(recurs+1, sName, element, out, bXML);
          //boolean bOneLineELement = outData(recurs+1, "", iterData, out, bXML);
          if(bXML && !bOneLineElement){ outIndent(recurs+1, out, bXML); out.append("</element>"); }
        }
      }
    } 
    else if(dataField instanceof CharSequence){
      appendContent(dataField, out);
    } 
    //else if(dataField)
    else {
      bOneLine = XXXoutData(recurs+1, sName, dataField, out, bXML);
    }
    return bOneLine;
  }
  
  
  private static void appendContent(Object data, Appendable out) throws IOException{
    String content = data.toString();
    content = content.replace("\r\n", "|").replace("\n", "|");
    out.append(content);
  }

  
  
  
}
