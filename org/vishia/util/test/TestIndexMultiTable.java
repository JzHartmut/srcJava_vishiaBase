package org.vishia.util.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.vishia.util.Assert;
import org.vishia.util.IndexMultiTable;
import org.vishia.util.StringFunctions;

public class TestIndexMultiTable
{

  static class Test{
    static int _ident = 0;
    String name;
    String check;
    int ident;
    Test(String name){ this.name = name; this.check = name; ident = ++_ident;}
    Test(String name, String check){ this.name = name; this.check = check; ident = ++_ident;}
    @Override public String toString(){ return name; }
  }
  
  IndexMultiTable.Provide<String> provider = new IndexMultiTable.Provide<String>(){

    @Override public String[] createSortKeyArray(int size){ return new String[size]; }

    @Override public String getMaxSortKey(){ return "zzzzzzzzzzzzz"; }

    @Override public String getMinSortKey(){ return " "; }
  };
  
  
  IndexMultiTable<String, Test> idx = new IndexMultiTable<String, Test>(IndexMultiTable.providerString);
  Map<String, Object> idx2 = new TreeMap<String, Object>();
  
  void test(){
    idx.shouldCheck(true);
    //Map<String, Test> idx = new TreeMap<String, Test>();
    Test next;
    List<Test> list = new ArrayList<Test>();
    list.add(new Test("b2"));    
    list.add(new Test("n2"));    
    list.add(new Test("z2"));    
    list.add(new Test("m2"));    
    list.add(new Test("m1"));    
    list.add(new Test("ab1"));    
    list.add(new Test("ab2"));    
    list.add(new Test("d2a"));    
    list.add(new Test("d2b"));    
    list.add(new Test("d3"));    
    list.add(new Test("d4"));    
    list.add(new Test("i1"));    
    list.add(new Test("j1"));    
    list.add(new Test("k1"));    
    list.add(new Test("i2"));    
    list.add(new Test("d2c"));    
    list.add(next = new Test("d2e"));    
    list.add(new Test("j2"));    
    list.add(new Test("k2a"));    
    list.add(new Test("d2f"));    
    list.add(new Test("d2f1"));    
    list.add(new Test("d2f2"));    
    list.add(new Test("d2f3"));    
    list.add(new Test("d2f4"));    
    list.add(new Test("d2f5"));    
    list.add(new Test("d2f6"));    
    list.add(new Test("d2f7"));    
    list.add(new Test("d2f8"));    
    list.add(new Test("d2f9"));    
    list.add(new Test("d2fa"));    
    list.add(new Test("d2fb"));    
    list.add(new Test("d2fc"));    
    list.add(new Test("d2fd"));    
    list.add(new Test("d2fe"));    
    list.add(new Test("d2ff"));    
    list.add(new Test("d2fg"));    
    list.add(new Test("d2g"));    
    list.add(new Test("l1"));    
    list.add(new Test("k2b"));    
    list.add(new Test("b2b"));    
    list.add(new Test("ac"));
    
    for(Test test: list){
      String cmpr = test.name.substring(0,2);
      if(test.ident == 14)
        Assert.stop();
      idx.add(cmpr, test);
      idx.checkTable();
    }
    
    idx.addBefore("d2", new Test("d2d"), next);    
    idx.checkTable();
    idx.add("aa", new Test("aa"));    
    idx.checkTable();

    Test value = idx.search("b21");
    System.out.println(value.name);
    StringBuilder utest = new StringBuilder(); 
    //check sorted content:
    for(Test test: idx){
      utest.append(test.name).append('-');
    }
    //Assert.check(StringFunctions.equals(utest, ",ab1,ab2,ac,b2,b2b,c5,d2a,d2b,d3,d4,i1,i2,j1,j2,k1,k2a,k2b,l1,m1,m2,n2,z2"));
    //
    //Iterator starting from any point between:
    //
    utest.setLength(0);
    Iterator<Test> iter = idx.iterator("d");
    Assert.stop();
    while(iter.hasNext()){
      Test obj = iter.next();
      utest.append(obj.name);
      //System.out.println(obj.name);
    }
    //Assert.check(StringFunctions.equals(utest, ",d2a,d2b,d3,d4,i1,i2,j1,j2,k1,k2a,k2b,l1,m1,m2,n2,z2"));
    
  }
  
  
  
  
  void testFile(File file){
    float timediff;
    int testct = 0;
    idx.clear();
    idx2.clear();
    idx.shouldCheck(false);
    try{
      BufferedReader rd = new BufferedReader(new FileReader(file));
      String line;
      long timestart = System.nanoTime();
      while((line = rd.readLine())!=null){
        int zline = line.length();
        int pos = 0;
        int lenKey = 5;
        while(pos >=0 && pos < zline-lenKey){
          String key = line.substring(pos, pos + lenKey);
          Test value = new Test(key);
          if(++testct == 27){
            Assert.stop();
          }
          if(key.equals("capab"))
            Assert.stop();
          idx.put(key, value);
          //idx2.put(key, value);
          //idx.append(key, value);
          //idx.checkTable();
          //addTreemap(idx2,key, value);
          pos = line.indexOf(' ', pos+1);
          if(pos >=0){
            while(pos < zline && line.charAt(pos) ==' '){ pos +=1; }
          }
        }
      }
      long timeend = System.nanoTime();
      timediff = (timeend - timestart)/1000000.0f;
      rd.close();
    } catch(IOException exc){
      System.err.println("TestIndexMultiTable - IOException; "+ exc);
      timediff = -1;
    }
    //check it with iterate through both lists.
    File fileout = new File("TestIndexMultiTable-result.txt");
    
    try{
      Writer out = new FileWriter(fileout);
      out.append(Float.toString(timediff)).append("ms \n");
      Iterator<Map.Entry<String, Object>> iterTreemap = idx2.entrySet().iterator();
      Iterator<Test> iterList = null;
      Map.Entry<String, Object> entryTreemap = null;
      for(Map.Entry<String, Test> entry: idx.entrySet()){
        Test value = entry.getValue();
        
        //get the value2
        Test value2;
        if(iterList !=null && iterList.hasNext()){
          value2 = iterList.next();
        } else {
          entryTreemap = iterTreemap.next();
          Object treemapitem = entryTreemap.getValue();
          if(treemapitem instanceof ArrayList<?>){
            @SuppressWarnings("unchecked")
            ArrayList<Test> node = (ArrayList<Test>) treemapitem;
            iterList = (node).iterator();
            value2 = iterList.next();
          } else {
            value2 = (Test)treemapitem;
            iterList = null;
          }
        }
        out.append(entry.getKey()).append(":").append(Integer.toString(value.ident))
           .append("   ").append(entryTreemap.getKey()).append(":").append(Integer.toString(value2.ident))
           .append('\n');
        assert(value == value2); //same order, same instances.
      }
      out.close();
    } catch(IOException exc){
      System.err.println("TestIndexMultiTable - file problem;");
    }
  }
  
  
  
  
  
  /**This method fills a Map with maybe Objects with the same key.
   * All Objects with the same key are stored in an ArrayList, which is the member of the idx for this key.
   * @param idx Any Map, unified key
   * @param key The key
   * @param value The object
   */
  public static void addTreemap(Map<String, Object> idx, String key, Test value){
    Object valTreemap = idx.get(key);  //key containing?
    if(valTreemap!=null){
      //key already containing
      if(valTreemap instanceof ArrayList<?>){
        @SuppressWarnings("unchecked")
        ArrayList<Test> listnode = (ArrayList<Test>) valTreemap;
        listnode.add(value);  //append
      } else {
        assert(valTreemap instanceof Test);
        ArrayList<Test> listnode = new ArrayList<Test>();
        listnode.add((Test)valTreemap);   
        listnode.add(value);
        idx.put(key, listnode);
      }
    } else {
      idx.put(key, value);
    }
  }
  
  
  
  
  
  public static void main(String[] args){
    TestIndexMultiTable main = new TestIndexMultiTable();
    main.test();
    main.testFile(new File("D:/vishia/ZBNF/examples_XML/DocuGenerationViaXML/docuSrc/ZGen.topic")); //any text file to test
  }
}
