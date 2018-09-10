package org.vishia.util.test;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.vishia.util.TreeNodeBase;

public class TestTreeNodeBase
{
  
  ExampleNode<String> root;
  
  
  MyNode root2;
  
  public static void main(String[] args){
    TestTreeNodeBase main = new TestTreeNodeBase();
    String content;
    
    main.fill();
    content = main.printTree(main.root);
    assert(content.equals("A->B1->B2->C[a->b->b->c->d->e]->D->E"));
    System.out.println(content);
    
    main.iter1();
    content = main.printTree(main.root);
    assert(content.equals("A->B2->C[a->b->b->c->d->e]->D->E"));
    System.out.println(content);
    
    main.removeAllB();
    content = main.printTree(main.root);
    assert(content.equals("A->C[a->b->b->c->d->e]->D->E"));
    System.out.println(content);

    main.removeFirstNode();
    content = main.printTree(main.root);
    assert(content.equals("C[a->b->b->c->d->e]->D->E"));
    System.out.println(content);
    
    main.removeLastNode();
    content = main.printTree(main.root);
    assert(content.equals("C[a->b->b->c->d->e]->D"));
    System.out.println(content);
    
    main.removeLastNode();
    content = main.printTree(main.root);
    assert(content.equals("C[a->b->b->c->d->e]"));
    System.out.println(content);
    
    main.removeLastNode();
    content = main.printTree(main.root);
    assert(content.equals(""));
    System.out.println(content);
    
  }

  private void fill(){
    //root = new ExampleNode<String>("root");
    root = new MyStringNode("root");
    root.addNode(new ExampleNode<String>("A"));
    root.addNode(new ExampleNode<String>("B", "B1"));
    root.addNode(new ExampleNode<String>("B", "B2"));
    ExampleNode<String> childC = new ExampleNode<String>("C");
    root.addNode(childC);
    root.addNode(new ExampleNode<String>("E"));

    ExampleNode<String> childD = new ExampleNode<String>("D");
    childC.addSiblingNext(childD);
    fillChild(childC);
  }
  
  
  private void fillChild(ExampleNode<String> node){
    node.addNode(new ExampleNode<String>("a"));
    node.addNode(new ExampleNode<String>("b"));
    node.addNode(new ExampleNode<String>("b"));
    ExampleNode<String> childC = new ExampleNode<String>("c");
    node.addNode(childC);
    node.addNode(new ExampleNode<String>("e"));

    ExampleNode<String> childD = new ExampleNode<String>("d");
    childC.addSiblingNext(childD);
  }
  
  
  
  private void iter1(){
    Iterator<ExampleNode<String>> iter = root.iterChildren();
    while(iter.hasNext()){
      ExampleNode<String> node = iter.next();
      if(node.data !=null && node.data.equals("B1")){
        iter.remove();  //try to remove the current node.
      }
    }
  }
  

  private void removeAllB(){
    //to remove all children, which are gotten with iterator, one can not detach in the iterator loop
    //because it is a concurrentModification. First gather it.
    List<ExampleNode<?>> toRemove = new LinkedList<ExampleNode<?>>();
    for(ExampleNode<?> node: root.iteratorChildren("B")){
      toRemove.add(node);
    }
    //Then get it from an independent list and remove it.
    for(ExampleNode<?> node: toRemove){
      node.detach();   //detach of node is the removing from tree!
    }

    
  }
  
  
  private void removeFirstNode(){
    ExampleNode<String> node = root.firstChild();
    node.detach();
  }
  
  private void removeLastNode(){
    ExampleNode<String> node = root.lastChild();
    node.detach();
  }
  
  
  private String printTree(ExampleNode<String> parent){
    StringBuilder u = new StringBuilder();
    String next = "";
    for(ExampleNode<String> node: parent){
      u.append(next).append(node.data !=null ? node.data : node.getKey());
      next = "->";
      if(node.hasChildren()){
        u.append("[").append(printTree(node)).append("]");
      }
    }
    return u.toString();
  }
  
  
  
  
  /**A node may be defined for different data which are referenced. Extend TreeNodeBase with some features
   * of the node and preserve the Data generic property. The application may use this node for several Data types.
   * Unfortunately ExampleNode<String> and MyNode are not compatible though they references the same data.
   * But their derived definition are equal but not the same.
   * @author hartmut
   *
   * @param <Data> The data type which is referenced in the node.
   */
  public static class ExampleNode<Data> extends TreeNodeBase<ExampleNode<Data>, Data,ExampleNode<Data>>
  {
    public ExampleNode(String key) {
      super(key, null);
    }
    
    public ExampleNode(String key, Data data) {
      super(key, data);
    }
    
    @Override protected ExampleNode<Data> newNode(String keyP, Data dataP){
      return new ExampleNode<Data>(keyP, dataP);
    }
    
    @Override public String toString(){
      StringBuilder u = new StringBuilder(30);
      if(prev !=null) { u.append(prev.key).append("<-"); }
      u.append(key);
      if(next !=null) { u.append("->").append(next.key); }
      return u.toString();
    }
  }

  
  /**This node is defined for String data to referenced. 
   * Compare with MyNode. It's equal. This type is compatible to ExampleNode.
   */
  public final static class MyStringNode extends ExampleNode<String>{

    public MyStringNode(String key, String data)
    { super(key, data);
    }
    
    public MyStringNode(String key)
    { super(key, key);
    }
    
  }
  
  
  /**A node may be defined for a special data which are referenced. 
   * All generic are removed. This node type refers data of type String.
   *
   */
  public final static class MyNode extends TreeNodeBase<MyNode, String, MyNode>
  {
    public MyNode(String key, String data)
    {
      super(key, data);
    }

    public MyNode(String key) {
      super(key, null);
    }
    
    @Override protected MyNode newNode(String keyP, String dataP){
      return new MyNode(keyP, dataP);
    }
    
    @Override public String toString(){
      StringBuilder u = new StringBuilder(30);
      if(prev !=null) { u.append(prev.key).append("<-"); }
      u.append(key);
      if(next !=null) { u.append("->").append(next.key); }
      return u.toString();
    }
  }

  
}
