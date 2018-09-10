package org.vishia.util.test;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class TestComparable
{
  public interface GenArray<Key>{
    Key[] gen();
  }
  
  public class IndexMultiTable<Key extends Comparable<Key>, Type> implements Map<Key,Type>, Iterable<Type>
  {

    
    
    Key key2;
    
    Key[] a; // = new Key[1000];
    
    void test(Key keyP){
      int x = key2.compareTo(keyP);
      x = a[0].compareTo(keyP);
      x = keyP.compareTo(a[0]);
    }
    
    IndexMultiTable(GenArray<Key> gen){
      a = gen.gen();
    }
    
    @Override
    public Iterator<Type> iterator()
    {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public void clear()
    {
      // TODO Auto-generated method stub
      
    }

    @Override
    public boolean containsKey(Object key)
    {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public boolean containsValue(Object value)
    {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public Set<java.util.Map.Entry<Key, Type>> entrySet()
    {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public Type get(Object key)
    {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public boolean isEmpty()
    {
      // TODO Auto-generated method stub
      return false;
    }

    @Override
    public Set<Key> keySet()
    {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public Type put(Key key, Type value)
    {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public void putAll(Map<? extends Key, ? extends Type> m)
    {
      // TODO Auto-generated method stub
      
    }

    @Override
    public Type remove(Object key)
    {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public int size()
    {
      // TODO Auto-generated method stub
      return 0;
    }

    @Override
    public Collection<Type> values()
    {
      // TODO Auto-generated method stub
      return null;
    }
  }
  
  
  IndexMultiTable<String, Object> idx = new IndexMultiTable<String, Object>(null);
  
  
  
}
