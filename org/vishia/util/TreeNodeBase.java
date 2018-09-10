package org.vishia.util;

import java.util.Deque;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.TreeMap;


/**This class is a base class for data, which are organized itself in a tree node structure.
 * The difference to data which are organized in a {@link java.util.TreeMap} for example is:
 * Members of a TreeMap are organized in a tree, but there are not nodes itself. The TreeMap 
 * is one of more possibly access forms. The same data may be organized in another TreeMap with
 * another key at the same time, it may be organized in a List in the same time or in an array.
 * The node of a TreeMap is not visible for usage.
 * <br>
 * Data which based on this class are intrinsic nodes which are beneficial and visible as node.
 * Every node of them has one master key to search it in a tree of nodes.
 * <br><br>
 * A known example for such nodes are elements of a XML tree. Any element has its parent
 * and some children. It is a node in an intrinsic modality. The master key is the tag name
 * combined with the nameSpace key.
 * The XPATH searches nodes in a tree with given tag names.
 * 
 * <br><br> 
 * UML-Diagram, presentation style see {@link Docu_UML_simpleNotation}:
 * <pre>
 * 
 *                                    TreeNodeBase
 *                                        |---data---> T (may be null)
 *       TreeNodeBase <---parent----------|
 *       (maybe null)                     |                    TreeNodeBase
 *                                        |                        |
 *                                        |------childNodes------*>|
 *                                        |                        |---data------> T 
 *                                        |<------------parent-----|        (a leaf if it hasn't 
 *                                        |                                  childNodes)
 *                                        |----leafData-----*> T (leafs)                                                  
 * </pre>
 * <ul>
 * <li>The TreeNode refers to its node data.
 * <li>The TreeNode refers to its children, 
 * <li>The TreeNode refers to its parent. 
 * </ul>
 * The data may refer its associated node. It may be a final reference (aggregation).
 * That means, the data represents a node. Then both instances should be joined in the
 * constructor of data:
 * Sample:
 * <pre>
 * class MyData{
 *   final TreeNodeBase node;
 *   MyData(MyData parent){
 *     this.node = new TreeNodeBase(parent.node, this);
 *   }
 * }
 * </pre>  
 * <br><b>Children with key, sorted selection enabled:</b>
 * <br><br>
 *  * <pre>
 * 
 *                                    TreeNode
 *                                      -key
 *                                        |------data-------> T (data, may be null)
 *       TreeNode <---parent--------------|
 *       (maybe null)                     |------idxChildren-----------key*>|
 *                                        |                               TreeNode (children) unique
 *                                        |------unsortedChildren---------*>|
 *                                        |                                 |-------data-------> T 
 *                                        |<------------parent--------------|              (the leaf if another node 
 *                                                                                          is not referenced)
 * </pre>
 * <ul>
 * <li>The TreeNode refers to its node data. It has a key.
 * <li>The TreeNode refers to its children, both with its ambiguous key (sorted) and unsorted.
 * <li>The TreeNode refers to its parent. 
 * </ul>
 * <br>
 * <b>Usage for more as one data with the same key</b>:<br>
 * <pre>
 *                                    !Meta-Node for all nodes 
 *             !parent                !with same key 
 *             TreeNodeBase           TreeNodeBase             TreeNodeBase  
 *                |                   -key                     !one of the children
 *                |                      |                     -key
 *                |---data-->T           |--data->null            |
 *   <---parent---|                      |--idxChildren-->null    |
 *                |--idxChildren----key*>|                        |---data-->T the data
 *                |                      |--childNodes----------*>|
 *                |                      |                        |--idxChildren--->maybe deeper nodes or null
 *                |                      |                        |--childNodes---->maybe deeper nodes or null
 *                |<-----------parent----|<---------parent--------|
 *                |                                               |
 *                |--childNodes---------------------------------*>|              
 *                                                                                          
 * </pre>
 * Sometimes a container should refer more as one data with the same key. 
 * That is not possible if the java.util.TreeMap is used, it supports only one key per data.
 * For example for XML data an element can contain more as one sub element with the same tag name
 * <ul>
 * <li>The parent TreeNodeBase refers to all children independent of their key 
 *   with the {@link #childNodes} reference. That is because this list of children represents
 *   the creation order. For example for XML nodes it is the order in the XML file.
 * <li>The parent TreeNodeBase refers to a TreeNodeBase which is a meta node for the key.
 *   It is referenced with the parent's {@link #idxChildren}.
 *   This meta node refers all children with the same key with its {@link #childNodes} list.
 *   The order is the creation order.
 * <li>The field {@link #data} of the meta node is left empty, because the data are located in 
 *   {@link #childNodes}.{@link #data}. The field {@link #idxChildren} is left empty too because
 *   there are no children with other keys than the only one.  
 * <li>The child node refers the meta node as its parent. That is important for detach. 
 *   The parent of the meta node refers the real parent.
 * </ul>

 *
 *
 * @author Hartmut Schorrig
 *
 * @param <Data> The type of additional data which are referenced in this node. Use Object if data are not used.
 * @param <DerivedNode> Type of the TreeNode which is build with this class as superclass.
 *   If one will only use the underived TreeNodeBase, use {@link TreeNodeBase.TreeNode} instead.
 * @param IfcType Type used in {@link SortedTree}. It has to be a super class or interface
 *   of the DerivedNode, elsewhere there are errors in runtime. 
 *   Because both DerivedNode extends SortedTree and IfcType extends SortedTree,
 *   the IfcType can be a super type of DerivedNode. But it should be!
 */
public class TreeNodeBase
<DerivedNode extends TreeNodeBase<DerivedNode,Data, IfcType> & SortedTree<IfcType>
, Data
, IfcType extends SortedTree<IfcType> 
> 
implements TreeNode_ifc<DerivedNode, Data>, SortedTree<IfcType>, Iterable<DerivedNode> 
{

  /**Version, history and license.
   * <ul>
   * <li>2017-08-27 Hartmut new: Implementation of {@link #iteratorChildren()} but with {@link IteratorDerivedImpl}
   *   which returns the next() as user type node 
   *   TODO: {@link #iteratorChildren(String)} gardening with documentation and test.  
   * <li>2014-02-09 Hartmut preparing new methods: {@link #movetoSiblingNext(TreeNodeBase)}, {@link #swap(TreeNodeBase)}
   * <li>2013-11-03 Hartmut bugfix: Problem on {@link #removeChildren()} if an exception was before.
   * <li>2013-11-03 Hartmut bugfix: {@link #iterChildren()} have to be return null, see its interface-definition.
   *   It had returned an Iterator, which first {@link Iterator#hasNext()} returns false. But with them
   *   it is not determined simply whether a node has children or not. The {@link org.vishia.xmlSimple.SimpleXmlOutputter}
   *   has not worked correctly. There the decision whether the text() of a node should be output depends
   *   on the existence of children. If it has children, the children contains the text() already.
   * <li>2013-11-03 Hartmut chg: Restructuring: Don't use the ListNodes with index etc, Use a double queue. 
   *   New {@link IteratorDerivedNode} etc.  
   * <li>2013-11-03 Hartmut new: {@link #firstChild()}, {@link #lastChild()}. Some methods returns
   *   the DerivedNode yet instead a reference of Type TreeNodeBase or IfcType. Only methods which are declared
   *   in {@link SortedTree} returns the IfcType. It is better for usage. A node is always type of DerivedNode
   *   because it is declared and known as forward reference.
   * <li>2013-11-03 Hartmut new: {@link #addNode(TreeNodeBase, int)} not on end.
   * <li>2013-11-03 Hartmut new: {@link #next}, {@link #prev} and {@link #lastChild} for sibling navigation. 
   * <li>2013-03-02 Hartmut chg: Usage of ArrayList instead LinkedList for children- better ability to debug.
   * <li>2012-11-03 Hartmut new: Generic type DerivedNode and IfcType to support derived types of this. The returned
   *   container types have elements of this DerivedNode type or the IfcType. The IfcType is used on methods
   *   which are defined in {@link SortedTree}.
   * <li>2012-11-01 Hartmut Constructor with parent is faulty because the parent will be set on parent.addNode(...).
   * <li>2012-11-01 Hartmut This class contains the sorted and the unsorted management of nodes
   *   inclusive multiple nodes with the same key. 
   *   This class is used for {@link org.vishia.xmlSimple.XmlNodeSimple} and has reduced the complexity
   *   of that class.
   * <li>2012-07-20 Hartmut creation: it is the base class for a node which is unsorted firstly.
   *   The derived class {@link TreeNodeUniqueKey} is sorted than.
   * </ul>
   * <br><br>
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
   * @author Hartmut Schorrig = hartmut.schorrig@vishia.de, www.vishia.org
   * 
   */
  public static final int version = 20131111;

  /**This instance is used if the node is a meta node to refer more as one child with the same key. 
   * 
   */
  private static String metaNodeKey = "--metanode-key--";
  
  /**The key which is used in the {@link #idxChildren} of the parent. 
   * Note that a key need not be unique. A parent can have children with the same key. */
  protected final String key;
  
  /**The parent is a metaNodeKey if there is more as one child with the same key.
   * To get the real parent, use {@link #parent()}.
   */
  protected TreeNodeBase<DerivedNode, Data, IfcType> parent;
  
  /**The List of child nodes in order of adding. All nodes in this list are type of the DerivedNode. 
   * This order may be different from the sorted order in idxChildren. The sub nodes have to sorting orders.
   * The order of adding and insertion with this elements and the order with a key. This is the primary list.
   */
  protected DerivedNode prev, next, firstChild, lastChild;
  
  /**The child nodes sorted to the key. The key is given with the child itself, attribute {@link #key}.
   * This index can contain a so named 'meta node' which holds more as one child with the same key.
   * Note that the meta node is not registered in the {@link #childNodes}.
   * The sorting with key may not be necessary in some kinds, then this element is null. Don't use it
   * to get a list of children. Use {@link #firstChild} etc. instead. 
   */
  protected Map<String, TreeNodeBase<DerivedNode, Data, IfcType>> idxChildren;

  
  /**Instances which are a leaf of this node. That leafs should not need a node to wrap it. 
   * The tree is not visible from a leaf because such a leaf is not a TreeNode.
   * If you want to refer from a leaf to its parent, use a node without childNodes and left this list empty.
   * @deprecated. It is too much complexity for a treeNode and not typical for it. Should contain in {@link #data}.
   */
  public List<Data> leafData;
  
  protected int nrofChildren;
  
  /**Any additional data associated with this node. */
  public final Data data;
  


  /**Creates a new unbounded node. 
   * @param key The key will be used if the tree is sorted. It can be null,
   *   then the tree is not sorted. The key is used to search this nodes by its key
   *   in the parent node or from any grandparent node with the path.
   *   
   * @param data User data of this node.
   */
  public TreeNodeBase(String key, Data data){
    this.key = key;
    this.parent = null;
    this.data = data;
  }

  
  /**Creates a new unbounded node. 
   * @param key The key will be used if the tree is sorted. It can be null,
   *   then the tree is not sorted. The key is used to search this nodes by its key
   *   in the parent node or from any grandparent node with the path.
   *   
   * @param data User data of this node.
   */
  protected TreeNodeBase(String key, Data data, TreeNodeBase<DerivedNode, Data, IfcType> parent){
    this.key = key;
    this.parent = parent;
    this.data = data;
  }

  
  /**This method has to be overridden in the derived class. It has to be created an instance
   * of the DerivedNode type.
   * @param key see {@link #TreeNodeBase(String, Object)}
   * @param data see {@link #TreeNodeBase(String, Object)}
   * @return Instance of the DerivedNode class
   */
  protected DerivedNode newNode(String keyP, Data dataP){
    throw new IllegalArgumentException("This method has to be overridden.");
  }
  
  
  /**adds a leaf.
   * 
   * @param leaf The data to add.
   * @deprecated
   */
  @Deprecated
  public void addLeaf(Data leaf)
  { 
    if(leafData == null){
      leafData = new ArrayList<Data>(); 
    }
    leafData.add(leaf);
  }
  
  

  
  /**create a child node and adds it.
   * This method is only proper to use if the DerivedNode has not a more complex constructor.
   * As opposite one can create a DerivedNode in the application level and invoke {@link #addNode(TreeNodeBase)}
   * @param itsKey The key may be free defined outside, independent of the content of the child. 
   *        This key is used to find out children.
   * @param leaf The data of the node.
   */
  public DerivedNode addNode(String itsKey, Data leaf)
  {
    DerivedNode childNode = newNode(itsKey, leaf);
    addNode(childNode);
    return childNode;
  }
  
  

  /**Adds a given child node.
   * 
   * @param itsKey The key may be free defined outside, independent of the content of the child. 
   *        This key is used to find out children.
   * @param leaf The data of the node.
   */
  public void addNode(DerivedNode childNode)
  { if(childNode.parent !=null || childNode.next !=null || childNode.prev !=null){
      throw new IllegalArgumentException("Node has a parent, it is contained anywhere other, invoke detach!");
    }
    addNodeKeyed(childNode, null);
    nrofChildren +=1;
    if(firstChild == null){
      firstChild = lastChild = childNode;
    } else {
      //yet only one child:
      lastChild.next = childNode;
      childNode.prev = lastChild;
      lastChild = childNode;
    }
  }
  
  
  
  
  public void addNodeFirst(DerivedNode childNode){
    if(childNode.parent !=null){
      throw new IllegalArgumentException("Node has a parent, it is contained anywhere other, invoke detach!");
    }
    addNodeKeyed(childNode, null);
    nrofChildren +=1;
    childNode.next = firstChild;  //maybe null
    if(firstChild !=null){
      firstChild.prev = childNode;
    }
    firstChild = childNode;
    if(lastChild == null){
      lastChild = childNode;
    }
    
  }
  
  
  
  /**Adds a new node behind the given node as sibling of this respectively child of this parent.
   * @param childNode
   */
  public void addSiblingNext(DerivedNode childNode){
    if(childNode.parent !=null){
      throw new IllegalArgumentException("TreeNodeBase.add - new Node has a parent; It is contained anywhere other, invoke detach;" + childNode);
    }
    @SuppressWarnings("unchecked")
    DerivedNode dthis = (DerivedNode)this; 
    TreeNodeBase<DerivedNode, Data, IfcType> parent1 = parent();
    if(parent1 == null){
      throw new IllegalArgumentException("TreeNodeBase.addSiblingNext - this Node has not a parent;" + parent);
    }
    parent1.addNodeKeyed(childNode, null);
    parent1.nrofChildren +=1;
    childNode.prev = dthis;
    childNode.next = this.next;   //may be null if this is the last one
    if(this.next !=null){
      this.next.prev = childNode;
    }
    this.next = childNode;
    if(parent.lastChild == this){
      parent.lastChild = childNode;
    }
  }


  /**Adds a new node before the given node as sibling of this respectively child of this parent.
   * @param childNode The yet not added new instance of node.
   */
  public void addSiblingPrev(DerivedNode childNode){
    if(childNode.parent !=null){
      throw new IllegalArgumentException("TreeNodeBase.add - new Node has a parent; It is contained anywhere other, invoke detach;" + childNode);
    }
    @SuppressWarnings("unchecked")
    DerivedNode dthis = (DerivedNode)this; 
    TreeNodeBase<DerivedNode, Data, IfcType> parent1 = parent();
    if(parent1 == null){
      throw new IllegalArgumentException("TreeNodeBase.addSiblingNext - this Node has not a parent;" + parent);
    }
    parent1.addNodeKeyed(childNode, null);
    parent1.nrofChildren +=1;
    childNode.next = dthis;
    childNode.prev = this.prev;  //may be null if this is the first one
    if(this.prev !=null){
      this.prev.next = childNode;
    }
    this.prev = childNode;
    if(parent.firstChild == this){
      parent.firstChild = childNode;
    }
  }


  /**Adds the child node in the index of nodes with key of this parent and set the parent of child.
   * The referenced parent of the child is the metaNode if the key is not unique.
   * @param childNode
   */
  private void addNodeKeyed(DerivedNode childNode, TreeNodeBase<DerivedNode, Data, IfcType> prev){
    @SuppressWarnings("unchecked")
    DerivedNode dthis = (DerivedNode)this; 
    if(childNode.key == null){
      childNode.parent = dthis;  //without key, the parent is this. See metaNode
    } 
    else {
      if(idxChildren == null)
      { //idxChildren = new TreeMap<String, TreeNodeBase<DerivedNode,Data,IfcType>>();
        idxChildren = new TreeMap<String, TreeNodeBase<DerivedNode, Data, IfcType>>();
      }
      TreeNodeBase<DerivedNode, Data, IfcType> childNodeFound = idxChildren.get(childNode.key);
      //TreeNodeBase<DerivedNode,Data,IfcType> childNodeFound = idxChildren.get(childNode.key);
      if(childNodeFound == null){
        //only one entry for this key, 
        //because it may be the first one, it is in uncertain 
        //whether it may be more as one leaf with the same key in the future.
        childNode.parent = dthis;
        idxChildren.put(childNode.key, childNode);
      } else {
        //a node with the same leaf was found.
        //TreeNodeBase<DerivedNode, Data, IfcType> metaNode;
        MetaNode<DerivedNode, Data, IfcType> metaNode;
        if(childNodeFound.key == metaNodeKey){
          //it is a meta node already.
          metaNode = (MetaNode<DerivedNode, Data, IfcType>)childNodeFound;
        } else {
          //it is the only one child with this key. Build a meta node yet:
          metaNode = new MetaNode<DerivedNode, Data, IfcType>(this); 
          //replace the entry in idxChildren with the metaNode instead the childNodeFound.
          idxChildren.put(childNode.key, metaNode);  //replaces the exitsting node.
          //add the found node to the metaNode's children:
          childNodeFound.parent = metaNode;  //it is the first one node.
          @SuppressWarnings("unchecked")
          DerivedNode childFound1 = (DerivedNode)childNodeFound;
          //TODO do not add at end, add after prev!
          metaNode.children.add(childFound1);
        }
        metaNode.children.add(childNode);
        childNode.parent = metaNode;
      }
    }
    
  }
  
  
  
  
  /**Swaps two nodes, this and dst. Both nodes are members of any treeNode bought. 
   * It is not necessary that both TreeNode have the same root. This and dst may be siblings.
   * @param dst The tree node which is swapped with this.
   */
  public void swap(DerivedNode other){
    
  }
  
  
  /**Remove the dst node from its bough and add it as next sibling after this.
   * It is not necessary that both TreeNode have the same root. This and dst may be siblings.
   * @param src the tree node which should be removed and add as next sibling of this.
   */
  public void movetoSiblingNext(DerivedNode src){
    
  }
  
  
  /**Remove the dst node from its bough and add it as previous sibling before this.
   * It is not necessary that both TreeNode have the same root. This and dst may be siblings.
   * @param src the tree node which should be removed and add as next sibling of this.
   */
  public void movetoSiblingPrev(DerivedNode src){
    
  }
  
  
  /**Remove the dst node from its bough and add it as the first child of this.
   * If this has children already, at least one, it is the same like 
   * <code>firstChild().movetoSiblingPrev(src)</code>. But if this has not children, it is the first one.
   * It is not necessary that both TreeNode have the same root. This and dst may be siblings.
   * @param src the tree node which should be removed and add as next sibling of this.
   */
  public void movetoFirstChild(DerivedNode src){
    
  }
  
  
  
  
  
  /**Detaches the node from its tree. A node can only be member of one tree of TreeNodeBase, 
   * not member of more as one. Any TreeNodeBase has its {@link #getParent()}.
   * Nevertheless you can refer any node within another structure of data. 
   * <br><br>
   * If a node is detached, it can be added in another tree of the same type.
   */
  public void detach(){
    TreeNodeBase<DerivedNode, Data, IfcType> parent1 = parent();
    if(parent1 !=null){
      if(parent1.firstChild == this){
        parent1.firstChild = next;  //maybe null;
      }
      if(parent1.lastChild == this){
         parent1.lastChild = prev;  //maybe null if the parent will be stay empty.        
      }

      if(parent.key == metaNodeKey){
        //remove the child in the real parent.
        MetaNode<DerivedNode, Data, IfcType> metaNode = (MetaNode<DerivedNode, Data, IfcType>)parent;
        metaNode.children.remove(this);
        //maybe queue in metaNode, remove it too. 
        //NOTE: It is not found in the idxChildren, only the meta node is there!
        if(metaNode.children.size() == 0){
          //The meta node has not children yet. remove the metaNode in idxChildren, use key of this.
          parent1.idxChildren.remove(this.key);
        }
      }
      else if(parent.idxChildren!=null && this.key !=null){
        parent.idxChildren.remove(this.key);
      }
      parent1.nrofChildren -=1;
      assert(parent1.nrofChildren >=0);
    }
    if(prev !=null){
      prev.next = this.next;
    }
    if(next !=null){
      next.prev = prev;
    }
    prev = null;
    next = null;
    parent = null;

  }
  
  
  /**Remove all children of this node. The immediate children will be detached from this node.
   * The children of that children would not be detached from its parent. But if the children
   * are not referenced anyway other they are removed from space (garbage collected).
   * Note: Use {@link #detach()} for any child node if it and their sub tree should be used furthermore.
   */
  public void removeChildren(){
    while(firstChild !=null){
      firstChild.detach();
    }
    if(nrofChildren !=0 || lastChild !=null){
      Assert.stop();
    }
    nrofChildren = 0;
    lastChild = null;
    
  }
  
  
  
  /**Returns the key of this node.
   * @return the key, null if the node is not sorted. 
   */
  public String getKey(){ return key; }
  
  public Data getParentData(){
    TreeNodeBase<DerivedNode, Data, IfcType> parent1 = parent(); 
    if(parent1 ==null){ return null;}
    else {
      return parent1.data;
    }
  }
  
  
  /**Returns any selected node in the tree.
   * @param path The path may have more as one key to traverse into the boughs of the tree.
   * @param separator Separator between key elements in path.
   * @return
   */
  public DerivedNode getNode(String path, String separator){
    String[] elements = path.split(separator);
    TreeNodeBase<DerivedNode,Data,IfcType> child = this;
    for(String name: elements){
      if(child.idxChildren == null){
        child = null;
        break;
      }
      TreeNodeBase<DerivedNode,Data,IfcType> child1 = child.idxChildren.get(name);
      if(child1 == null){ 
        child = null;
        break;
      } else {
        child = child1;
      }
    }
    @SuppressWarnings("unchecked")
    DerivedNode retChild = (DerivedNode)child;
    return retChild;
  }
  
  

  
  /**Searches a node with the path, creates it if it is not found.
   * @param path path of keys, see {@link #getNode(String, String)}
   * @param separator The separator in path.
   * @return always a node with given key
   */
  @SuppressWarnings("unchecked")
  public DerivedNode getOrCreateNode(String path, String separator){
    String[] elements = path.split(separator);
    TreeNodeBase<DerivedNode,Data,IfcType> child = this;
    for(String name: elements){
      if(child.idxChildren == null){
        idxChildren = new TreeMap<String, TreeNodeBase<DerivedNode, Data, IfcType>>();
        //idxChildren = new TreeMap<String, TreeNodeBase<DerivedNode,Data,IfcType>>();
        DerivedNode child1 = newNode(name, null);
        child.addNode(child1);
        child = child1;
      
      } else {
        TreeNodeBase<DerivedNode,Data,IfcType> child1 = child.idxChildren.get(name);
        if(child1 == null){ 
          child1 = newNode(name, null);
          child.addNode((DerivedNode)child1);
          child = child1;
        } else {
          child = child1;
        }
      }
    }
    return (DerivedNode)child;
  }
  


  
  
  /**Gets the parent of this node or null if it is the root node or it is {@link #detach()}.
   * @see org.vishia.util.SortedTree#getParent()
   */
  @Override public IfcType getParent(){
    @SuppressWarnings("unchecked")
    IfcType ret = (IfcType)parent();
    return ret;
  }

  
  public DerivedNode parent(){ 
    if(parent !=null && parent.key == metaNodeKey){
      @SuppressWarnings("unchecked")
      DerivedNode parent1 = (DerivedNode)parent.parent;
      return parent1;
    } else {
      @SuppressWarnings("unchecked")
      DerivedNode parent1 = (DerivedNode)parent;
      return parent1;
    }
  }
  
  
  
  /* (non-Javadoc)
   * @see org.vishia.util.TreeNode_ifc#parentEquals(org.vishia.util.TreeNode_ifc)
   */
  @Override public boolean parentEquals(TreeNode_ifc<DerivedNode, Data> cmp){
    if(this.parent !=null && this.parent.key == metaNodeKey){
      return this.parent.parent == cmp;
    } else {
      return this.parent == cmp;
    }
  }
  
  
  /* (non-Javadoc)
   * @see org.vishia.util.TreeNode_ifc#nextSibling()
   */
  @Override public DerivedNode nextSibling(){ return next; } 
  
  public DerivedNode prevSibling(){ 
    DerivedNode ret = prev;
    return ret;
  }
  
  
  /**The List of child nodes in order of adding. All nodes in this list are type of the DerivedNode. 
   */
  public List<DerivedNode> childNodes(){ 
    List<DerivedNode> list = new ArrayList<DerivedNode>(nrofChildren);
    for(DerivedNode node: iterator()){
      list.add(node);
    }
    return list; 
  }
  
  
  /**Returns the number of children.
   * @return 0 if the node has not children.
   */
  public int nrofChildren(){ return nrofChildren; }
  
  /**Returns the first of all children.
   * @return null if the node has not children.
   */
  public DerivedNode firstChild(){ return firstChild; }
  
  
  public boolean hasChildren(){ return firstChild !=null; }
  
  /**Returns the last of all children.
   * @return null if the node has not children.
   */
  public DerivedNode lastChild(){ return lastChild; } 
  
  /**Returns the node with the given key. If there are more as one node with the same key,
   * the first node is returned. 
   * @param sKey
   * @return null if a node with the key is not referred from this node directly.
   */
  @Override 
  public IfcType getChild(String sKey)
  {
    if(idxChildren == null){
      return null;
    }
    TreeNodeBase<DerivedNode, Data, IfcType> nodeChild = idxChildren.get(sKey);
    //TreeNodeBase<DerivedNode,Data,IfcType> nodeChild = idxChildren.get(sKey);
    if(nodeChild !=null && nodeChild.key == metaNodeKey){
      MetaNode<DerivedNode, Data, IfcType> metaNode = (MetaNode<DerivedNode, Data, IfcType>)nodeChild;
      
      if(metaNode.children.size()>=1){
        nodeChild = metaNode.children.get(0);
      } else {
        nodeChild = null;  //meta node exists but it has not childs because all of them are detached.
      }
    }
    //return (IfcType)nodeChild;
    @SuppressWarnings("unchecked")
    IfcType ret = (IfcType)nodeChild;
    return ret;
  }

  
  @Override public String toString(){ return key; }

  
  /**It returns an instance which can used as both as {@link Iterator}
   * and as {@link Iterable}. It means the routines {@link Iterator#hasNext()} etc. are able to call
   * and {@link Iterable#iterator()} is able to call, which returns this.
   * In this kind the method can be used in a for-container-loop.
   */
  @Override public IterableIterator<DerivedNode> iterator(){
    return new IteratorDerivedNode();
  }
  
  
  public IterableIterator<DerivedNode> iteratorChildren(String keyP){
    TreeNodeBase<DerivedNode, Data, IfcType> keyNode;
    if(idxChildren !=null && (keyNode = idxChildren.get(keyP)) !=null){
      if(keyNode.key == metaNodeKey){
        MetaNode<DerivedNode, Data, IfcType> metaNode = (MetaNode<DerivedNode, Data, IfcType>)keyNode;
        return new IteratorMetaNode(metaNode); //(MetaNode)keyNode).children.iterator();
      } else {
        //only one node with this key
        @SuppressWarnings("unchecked")
        DerivedNode oneNode = (DerivedNode)keyNode;
        return new IteratorOneNode(oneNode);
      }
    } else {
      return null;  //key not found or no keys available.
    }
  }
  

  @Override
  public Iterator<IfcType> iterChildren()
  { 
    //@SuppressWarnings("unchecked")
    Iterator<IfcType> ret = firstChild == null ? null :  new IteratorImpl();
    return ret;
  }


  @Override
  public IterableIterator<DerivedNode> iteratorChildren()
  { 
    //@SuppressWarnings("unchecked")
    IterableIterator<DerivedNode> ret = firstChild == null ? null :  new IteratorDerivedImpl();
    return ret;
  }


  @Override
  public Iterator<IfcType> iterChildren(String sKey)
  { List<IfcType> listChildren = listChildren(sKey);
    return listChildren==null ? null : listChildren.iterator();
    /*
    if(idxChildren ==null){ return null; }
    else { 
      TreeNodeBase<DerivedNode,Data,IfcType> children = idxChildren.get(sKey);
      if(children ==null|| children.childNodes ==null){ return null; }
      else {
        @SuppressWarnings("unchecked")
        Iterator<IfcType> ret = (Iterator<IfcType>)children.childNodes.iterator();
        return ret; 
      }
    }
    */
  }


  @Override
  public List<IfcType> listChildren()
  {
    final List<IfcType> list;
    Iterator<IfcType> iter = iterChildren();
    if(iter !=null){
      list = new ArrayList<IfcType>(nrofChildren);
      while(iter.hasNext()){
        list.add(iter.next());
      }
    } else {
      list = null;
    }
    return list; 
  }


  /**Searches the child node with the given key.
   * If the child node is a meta node for more as one child, its {@link #childNodes(TreeNodeBase)}
   * list is the proper return value. But if the child node refers only one child,
   * a list with this one element is built temporary because this method should return a list anyway. 
   * @return A List of all children with the given key or null if there is no such child.
   * @see org.vishia.util.SortedTree#listChildren(java.lang.String)
   */
  @SuppressWarnings("unchecked")
  @Override 
  public List<IfcType>listChildren(String sKey)
  { if(idxChildren ==null) { return null; }
    else {
      TreeNodeBase<DerivedNode,Data,IfcType> childMetaNode = idxChildren.get(sKey);
      if(childMetaNode == null){ return null; }
      else{
        List<IfcType> ret;
        if(childMetaNode.key != metaNodeKey){
          //only one child, but a List is expected
          ret = new ArrayList<IfcType>();
          //IfcType childNode = (IfcType)newNode(sKey, childMetaNode.data);
          ret.add((IfcType)childMetaNode);
        } else {
          MetaNode metaNode = (MetaNode)childMetaNode;
          ret = metaNode.children;
        }
        return ret;
      }
    }
  }

  
  
  /**Walk through the tree. On any node one of the callback methods are invoked.
   * Their return value determines whether child nodes are walked through or not
   * or whether the walking should be aborted.
   * @param root The start node
   * @param depth Maximum number of child levels, maybe {@link Integer#MAX_VALUE}
   * @param callback The callback should be implemented by the user to evaluate and control the walking.
   */
  public void walkTree(DerivedNode root, int depth, TreeNodeCallback<DerivedNode> callback)
  {
    callback.start();
    walkSubTree(root, depth, callback);
    callback.finished();
  }
    
  private TreeNodeCallback.Result walkSubTree(DerivedNode node, int depth, TreeNodeCallback<DerivedNode> callback)
  {
    TreeNodeCallback.Result result = TreeNodeCallback.Result.cont;
    result = callback.offerParent(node);
    if(result == TreeNodeCallback.Result.cont){ //only walk through subdir if cont
      Iterator<DerivedNode> iter = iterator();
      while(result == TreeNodeCallback.Result.cont && iter.hasNext()) {
        DerivedNode child = iter.next();
        if(child.hasChildren()){
          if(depth >1){
            result = walkSubTree(node, depth-1, callback);  
          } else {
            result = callback.offerLeaf(node);  //show it as file instead walk through tree
          }
        } else {
          result = callback.offerLeaf(node);
        }
      }
    } 
    if(result != TreeNodeCallback.Result.terminate){
      //continue with parent. Also if offerDir returns skipSubdir or any file returns skipSiblings.
      result = TreeNodeCallback.Result.cont;
    }
    return result;  //maybe terminate
  }

  

  
  
  

  /**Returns the container with children with the same key.
   * @param sKey
   * @return null if it is not a container.
   */
  public List<DerivedNode>getContainerChildren(String sKey)
  { if(idxChildren ==null) { return null; }
    else {
      TreeNodeBase<DerivedNode,Data,IfcType> childMetaNode = idxChildren.get(sKey);
      if(childMetaNode == null){ return null; }
      else{
        List<DerivedNode> ret;
        if(childMetaNode.key != metaNodeKey){
          return null;
          //only one child, but a List is expected
        } else {
          MetaNode metaNode = (MetaNode)childMetaNode;
          ret = metaNode.children;
        }
        return ret;
      }
    }
  }


  protected List<DerivedNode> childNodes(TreeNodeBase<DerivedNode,Data,IfcType> node){ return node.childNodes(); }

  
  
  
  protected static class MetaNode  //<DerivedNode, Data, IfcType> 
  <DerivedNode extends TreeNodeBase<DerivedNode,Data, IfcType> & SortedTree<IfcType>
  , Data
  , IfcType extends SortedTree<IfcType> 
  > 
  extends TreeNodeBase<DerivedNode, Data, IfcType>
  {

    protected List<DerivedNode> children = new ArrayList<DerivedNode>();
    
    public MetaNode(TreeNodeBase<DerivedNode,Data, IfcType> parent)
    { super(metaNodeKey, null, parent);
    }
    
  }
  
  
  
  
  protected class IteratorMetaNode implements IterableIterator<DerivedNode>
  {
    MetaNode<DerivedNode, Data, IfcType> metaNode;

    DerivedNode currentNode;
    
    Iterator<DerivedNode> iter;

    protected IteratorMetaNode(MetaNode<DerivedNode, Data, IfcType> metaNode){
      this.metaNode = metaNode;
      this.iter = metaNode.children.iterator();
    }
    
    @Override
    public boolean hasNext()
    { return iter.hasNext();
    }

    @Override
    public DerivedNode next(){ return currentNode = iter.next(); }

    @Override
    public void remove()
    { if(currentNode ==null) throw new IllegalStateException("");
      iter.remove();
      currentNode.detach();
      if(metaNode.children.size() == 0){
        metaNode.parent().idxChildren.remove(currentNode.key);
      }
      currentNode = null;
    }
    

    @Override
    public Iterator<DerivedNode> iterator()
    { return this;
    }
  }
  
  
  
  
  
  
  protected class IteratorImpl implements Iterator<IfcType>
  {
    DerivedNode currentNode, nextNode;

    protected IteratorImpl(){
      currentNode = null;
      nextNode = firstChild;
    }
    
    @Override
    public boolean hasNext()
    { return nextNode !=null;
    }

    @Override
    public IfcType next()
    { currentNode = this.nextNode;
      nextNode = nextNode.next;  //maybe null then.
      @SuppressWarnings("unchecked")
      IfcType ret = (IfcType)currentNode;
      return ret;
    }

    @Override
    public void remove()
    { if(currentNode ==null) throw new IllegalStateException("");
      currentNode.detach();
      currentNode = null;
    }
    
  }
  
  
  
  
  
  /**It is similar like {@link IteratorImpl} but with DerivedNode as return. 
   * 
   */
  protected class IteratorDerivedImpl implements IterableIterator<DerivedNode>
  {
    DerivedNode currentNode, nextNode;

    protected IteratorDerivedImpl(){
      currentNode = null;
      nextNode = firstChild;
    }
    
    @Override
    public boolean hasNext()
    { return nextNode !=null;
    }

    @Override
    public DerivedNode next()
    { currentNode = this.nextNode;
      nextNode = nextNode.next;  //maybe null then.
      @SuppressWarnings("unchecked")
      DerivedNode ret = (DerivedNode)currentNode;
      return ret;
    }

    @Override
    public void remove()
    { if(currentNode ==null) throw new IllegalStateException("");
      currentNode.detach();
      currentNode = null;
    }

    @Override
    public Iterator<DerivedNode> iterator()
    { return this;
    }
    
  }
  
  
  
  
  
  
  protected class IteratorDerivedNode implements IterableIterator<DerivedNode>
  {
    DerivedNode currentNode, nextNode;

    protected IteratorDerivedNode(){
      currentNode = null;
      nextNode = firstChild;
    }
    
    @Override
    public boolean hasNext()
    { return nextNode !=null;
    }

    @Override
    public DerivedNode next()
    { currentNode = this.nextNode;
      nextNode = nextNode.next;  //maybe null then.
      return currentNode;
    }

    @Override
    public void remove()
    { if(currentNode ==null) throw new IllegalStateException("");
      currentNode.detach();
      currentNode = null;
    }
    

    @Override
    public Iterator<DerivedNode> iterator()
    { return this;
    }
  }
  
  
  
  
  
  protected class IteratorOneNode implements IterableIterator<DerivedNode>
  {
    DerivedNode currentNode;

    boolean bNext = true;
    
    protected IteratorOneNode(DerivedNode node){
      currentNode = node;
    }
    
    @Override
    public boolean hasNext()
    { return bNext;
    }

    @Override
    public DerivedNode next()
    { bNext = false;
      return currentNode;  //NOTE: keep reference for remove.
    }

    @Override
    public void remove()
    { if(currentNode ==null) throw new IllegalStateException("");
      currentNode.detach();
      currentNode = null;
    }

    @Override
    public Iterator<DerivedNode> iterator()
    { return this;
    }
    
  }
  
  
  
  
  
  
  
  

  /**This class provides a ready to use TreeNode without additional functionality like TreeNodeBase.
   * The DerivedNode is this Type.
   * @author Hartmut
   *
   * @param <Data>
   */
  public final static class TreeNode<Data> extends TreeNodeBase<TreeNode<Data>, Data,TreeNode<Data>>
  {
    public TreeNode(String key, Data data) {
      super(key, data);
    }
    
    @Override protected TreeNode<Data> newNode(String keyP, Data dataP){
      return new TreeNode<Data>(keyP, dataP);
    }
  }
}
