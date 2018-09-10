package org.vishia.util.test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.vishia.util.DataAccess;

public class TestDataAccess
{

  int testint;
  
  float testfloat;
  
  String name;
  
  StringBuilder testBuffer = new StringBuilder(20);
  
  Map<String, TestDataAccess> testContainer = new TreeMap<String, TestDataAccess>();
  
  Map<String, DataAccess.Variable> testDatapool = new TreeMap<String, DataAccess.Variable>();
  
  TestDataAccess refer;
  
  public static void main(String[] args){
    TestDataAccess dataRoot = new TestDataAccess("dataroot", 345);
    dataRoot.refer = new TestDataAccess("refer", 7890);
    TestDerived dataRootDerived = new TestDerived("derived", 678);  //another instance
    Map<String, DataAccess.Variable<Object>> dataPool = test_createDatapool(dataRoot, dataRootDerived);
    try{
      test_getDataFromField(dataRoot, dataRootDerived);
      test_accessEnclosing(dataRoot, dataRootDerived);  
      test_secondStateField(dataRoot);
      test_datapool(dataPool);
    } catch(Exception exc){
      System.out.println(exc.getMessage());
    }
  }
  
  
  TestDataAccess(String name, int ident){
    testint = ident;
    testfloat = ident/100.0f;
    this.name = name;
  }
  
  
  /**Creates a datapool which contains 2 intances to test the pool access.
   * @param elements members of pool
   * @return the pool
   */
  static Map<String, DataAccess.Variable<Object>> test_createDatapool(TestDataAccess ... elements){
    Map<String, DataAccess.Variable<Object>> pool = new TreeMap<String, DataAccess.Variable<Object>>();
    for(TestDataAccess element: elements){
      DataAccess.Variable<Object> var = new DataAccess.Variable<Object>('O', element.name, element);
      pool.put(element.name, var);
    }
    return pool;
  }
  
  /**Simple access to fields of a instance, which may be done with reflection too.
   * The using of the basic reflection methods needs more programming effort only.
   * @param dataRoot
   * @param dataRootDerived
   * @throws Exception
   */
  static void test_getDataFromField(TestDataAccess dataRoot, TestDataAccess dataRootDerived)
  throws Exception
  {
    Object ovalue;
    int ivalue;
    DataAccess.Dst field = new DataAccess.Dst();

    //data from the direct instance. It wraps only 2 invocations of Reflection, more simple as that.
    ovalue = DataAccess.getDataFromField("testint", dataRoot, true, field);
    //assumed that it is an integer or any other numeric field:
    ivalue = DataAccess.getInt(ovalue);
    assert(ovalue instanceof Integer && ivalue == 345);
    //
    //data from the super class. It needs more effort in programming by using reflection.
    //Here it is the same.
    ovalue = DataAccess.getDataFromField("testint", dataRootDerived, true, field);
    ivalue = DataAccess.getInt(ovalue);
    assert(ovalue instanceof Integer && ivalue == 678);
    
  }
  
  
  
  
  
  /**Access to enclosing instances. This need some more effort if it would programmed 
   * by the basic reflection methods. 
   * @param dataRoot
   * @param dataRootDerived
   * @throws Exception
   */
  static void test_accessEnclosing(TestDataAccess dataRoot, TestDataAccess dataRootDerived)
  throws Exception
  {
    TestInner dataInner = dataRoot.new TestInner();
    TestInner dataInnerDerived = dataRootDerived.new TestInner();
    
    Object ovalue;
    int ivalue;
    DataAccess.Dst field = new DataAccess.Dst();
    //data from an enclosing instance.
    ovalue = DataAccess.getDataFromField("testint", dataInner, true, field);
    //assumed that it is an integer or any other numeric field:
    ivalue = DataAccess.getInt(ovalue);
    assert(ovalue instanceof Integer && ivalue == 345);
    //
    //data from the super class of the enclosing instance.
    ovalue = DataAccess.getDataFromField("testint", dataInnerDerived, true, field);
    ivalue = DataAccess.getInt(ovalue);
    assert(ovalue instanceof Integer && ivalue == 678);
    
    //Gets the enclosing instance.
    ovalue = DataAccess.getEnclosingInstance(dataInner);
    assert(ovalue instanceof TestDataAccess);
    //Same algorithm as above.
    ovalue = DataAccess.getEnclosingInstance(dataInnerDerived);
    assert(ovalue instanceof TestDataAccess.TestDerived);
  }
  
  
  
  
  static void test_secondStateField(TestDataAccess dataRoot) throws Exception{
    Object ovalue;
    int ivalue;
    //Build an access path: first field refer from the dataRoot, than in the refer instance field testint
    List<DataAccess.DatapathElement> path = new ArrayList<DataAccess.DatapathElement>(); 
    path.add(new DataAccess.DatapathElement("refer"));
    path.add(new DataAccess.DatapathElement("testint"));
    //
    DataAccess.Dst field = new DataAccess.Dst();
    ovalue = DataAccess.access(path, dataRoot, true, false, false, field);
    ivalue = DataAccess.getInt(ovalue);
    assert(ivalue == 7890);
  }
  
  
  
  /**Access via a datapool
   * @param datapool
   * @throws Exception
   */
  static void test_datapool(Map<String, DataAccess.Variable<Object>> datapool) throws Exception{
    Object ovalue;
    int ivalue;
    //Build an access path: first field refer from the dataRoot, than in the refer instance field testint
    List<DataAccess.DatapathElement> path = new ArrayList<DataAccess.DatapathElement>(); 
    path.add(new DataAccess.DatapathElement("@dataroot"));
    path.add(new DataAccess.DatapathElement("refer"));
    path.add(new DataAccess.DatapathElement("testint"));
    //
    DataAccess.Dst field = new DataAccess.Dst();
    ovalue = DataAccess.access(path, datapool, true, false, false, field);
    ivalue = DataAccess.getInt(ovalue);
    assert(ivalue == 7890);
  }
  
  
  
  
  
  /**A class which shows the possibility to simple access to members of a super class.
   * The super class is the TestDataAccess.
   */
  public static class TestDerived extends TestDataAccess
  {
    TestDerived(String name, int ident){
      super(name, ident);
    }
    
    double testshort = -2789;
    
  }

  
  
  /**A class which shows the possibility to simple access to members of a enclosing class.
   * The super class is the TestDataAccess.
   */
  public class TestInner
  {
    String test = "TestInner";
    double testdouble = Math.PI;
  }

  
  
  
  
  
  
}



