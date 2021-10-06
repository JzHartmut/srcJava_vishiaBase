package org.vishia.util;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;

/**This class manages identifications of Objects (instances). An object which is created in the heap has not a identification
 * but it has its unique storage address for this session. The storage address is used as Object.hashCode() usual to identify
 * an object. But this identifier is not the same if the same application runs twice on the same or another execution environment
 * because the memory situation is not the same on the running environment (PC, Operation system). 
 * Therefore produced data are different because the hash code is different. 
 * It is not proper if textual data are evaluated by a text difference tool.
 * <br><br>
 * This class helps to get a unique object identification independent of the memory address and independent of the hash code.
 * 
 * @author Hartmut Schorrig
 *
 */
public class ObjectId
{
  
    /**Version, history and license.
   * <ul>
   * <li>2015-08-01 created. Necessary for {@link org.vishia.util.DataShow}.
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
  static final public String sVersion = "2015-08-01";

  private static class InstancesOfType
  {
    Map<Integer, Integer> mapInstances = new TreeMap<Integer, Integer>();
    final int typeId;
    
    int nextInstanceId = 0;
    
    InstancesOfType(int id){ typeId = id; }
  }
  
  
  private int nextTypeId = 0;
  
  private Map<String, InstancesOfType> allInstances = new TreeMap<String, InstancesOfType>();
  
  
  private Map<Integer, String> allInstancesHash2Id = new TreeMap<Integer, String>();
  
  private final Map<String, Object> allInstancesPerId = new TreeMap<String, Object>();
  
  private final Map<Integer, Object> allInstancesPerHash = new TreeMap<Integer, Object>();
  
  private final List<Object> allInstancesInOrder = new LinkedList<Object>();
  
  
  
  public ObjectId(){
  }
  
  
  
  
  /**Returns a unique identification String for this instance.
   * The identification string is unique for a run session which uses this instance and method to get it.
   * The identification string is the same if this method was invoked in different sessions but with the same order of instances,
   * especially if an application with the same data runs twice. This is important for compare-ability.
   *  
   * @param data The instance
   * @return identification String for this instance.
   */
  public String instanceId(Object data, Queue<Object> newInstances) {
    Class clazz = data.getClass();
    String classname = clazz.getCanonicalName();
    if(classname == null){ //anonymous class:
      classname = clazz.getName();
    }
    InstancesOfType instancesOfType = allInstances.get(classname);
    if(instancesOfType == null){ 
      instancesOfType = new InstancesOfType(++nextTypeId);
      allInstances.put(classname, instancesOfType);
    }
    int hash = data.hashCode();
    Integer instanceId = instancesOfType.mapInstances.get(hash);
    boolean bNewInstance = (instanceId == null);
    if(bNewInstance ) {
      instanceId = new Integer(++instancesOfType.nextInstanceId);
      instancesOfType.mapInstances.put(hash, instanceId);
    }
    String id = instancesOfType.typeId + "_" + instanceId.intValue();
    if(bNewInstance) {
      allInstancesHash2Id.put(hash, id);
      allInstancesPerId.put(id, data);
      allInstancesInOrder.add(data);
      if(newInstances !=null) {
        newInstances.offer(data);
      }
      allInstancesPerHash.put(hash, data);
    }
    return id;
  }

  
  
  /**Returns the Object by a given identification for all instances which are already registered with {@link #instanceId(Object)}.
   * It returns null if the identification is unknown here.
   * @param id given identification
   * @return the object if {@link #instanceId(Object)} was called for this object before, elsewhere null. 
   */
  public Object getPerId(String id){ return allInstancesPerId.get(id); }
  
  /**Returns the object by a given hash for all instances which are already registered with {@link #instanceId(Object)}.
   * It returns null if the hash is unknown here.
   * @param hash given hashCode from an Object
   * @return the object if {@link #instanceId(Object)} was called for this object before, elsewhere null. 
   */
  public Object getPerHash(int hash){ return allInstancesPerHash.get(hash); }
  
  /**Returns the identification by a given hash for all instances which are already registered with {@link #instanceId(Object)}.
   * It returns null if the hash is unknown here.
   * @param hash given hashCode from an Object
   * @return the object identification if {@link #instanceId(Object)} was called for this object before, elsewhere null. 
   */
  public String getIdFromHash(int hash){ return allInstancesHash2Id.get(hash); }
  
  
  
  /**Returns a list which contains all instances in order of registration with {@link #instanceId(Object)}.
   * The list should not be modified for further usage.
   * @return List of all instances.
   */
  public List<Object> allInstances(){ return allInstancesInOrder; }
  
  
  /**This method executes the toString() method for data. The Object.toString() produces an output which ends with "@hash".
   * If this pattern was found and the hash code is found for a registered instance with {@link #instanceId(Object)}
   * then the hash code is replaced by the instanceId. Elsewhere the toString() output is not replaced.
   * @param data
   * @return
   */
  public String toStringNoHash(Object data) {
    String content = data == null ? null : data.toString();
    if(content !=null){ //NOTE: toString may supply a null pointer, it is possible.
      int posHash = content.lastIndexOf('@');  
      if(posHash >0){
        String sHash = content.substring(posHash+1);
        try{ 
          int nHash = Integer.parseInt(sHash, 16);
          String sId = allInstancesHash2Id.get(nHash);
          if(sId !=null) {
            content = content.substring(0, posHash+1) + sId;  //replace the hash on end with the id.
          }
        } catch(Exception exc){ } //do nothing.
      }
    }
    return content;
  }
  
  
  
  
  


}
