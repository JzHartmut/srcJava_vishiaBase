package org.vishia.util.test;

import java.lang.reflect.Field;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.vishia.util.CalculatorExpr;
import org.vishia.util.DataAccess;
import org.vishia.util.StringPartScan;

public class TestDataAccess
{

  
    static class Example {
    
    
    /**A class which shows the possibility to simple access to members of a enclosing class.
       * The super class is the TestDataAccess.
       */
      public class TestInner
      {
        String test = "TestInner";
        double testdouble = Math.PI;
      }

    int testint;
    
    float testfloat;
    
    String name;
    
    String[] str = {"s1", "s2", "s3"};
    
    StringBuilder testBuffer = new StringBuilder(20);
    
    Map<String, TestDataAccess> testContainer = new TreeMap<String, TestDataAccess>();
    
    Map<String, DataAccess.Variable> testDatapool = new TreeMap<String, DataAccess.Variable>();
    
    Example refer;
  
    Example(String name, int ident){
      testint = ident;
      testfloat = ident/100.0f;
      this.name = name;
    }
    
    
    String operationExmpl(String s1, String s2) {
      return s1 + s2;
    }
    
    
    
  }
  
  /**A class which shows the possibility to simple access to members of a super class.
     * The super class is the TestDataAccess.
     */
    public static class TestDerived extends Example
    {
      TestDerived(String name, int ident){
        super(name, ident);
      }
      
      double testshort = -2789;
      
    }

  public static void main(String[] args){
    Example dataRoot = new Example("dataroot", 345);
    dataRoot.refer = new Example("refer", 7890);
    TestDerived dataRootDerived = new TestDerived("derived", 678);  //another instance
    Map<String, DataAccess.Variable<Object>> dataPool = test_createDatapool(dataRoot, dataRootDerived);
    try{
      test_dataOperationStringArgs(dataRoot);
      test_dataArrayDataAccess(dataRoot);
      test_getDataFromField(dataRoot, dataRootDerived);
      test_accessEnclosing(dataRoot, dataRootDerived);  
      test_secondStateField(dataRoot);
      test_datapool(dataPool);
    } catch(Exception exc){
      System.out.println(exc.getMessage());
    }
  }
  
  
  
  /**Creates a datapool which contains 2 intances to test the pool access.
   * @param elements members of pool
   * @return the pool
   */
  static Map<String, DataAccess.Variable<Object>> test_createDatapool(Example ... elements){
    Map<String, DataAccess.Variable<Object>> pool = new TreeMap<String, DataAccess.Variable<Object>>();
    for(Example element: elements){
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
  static void test_getDataFromField(Example dataRoot, Example dataRootDerived)
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
  static void test_accessEnclosing(Example dataRoot, Example dataRootDerived)
  throws Exception
  {
    Example.TestInner dataInner = dataRoot.new TestInner();
    Example.TestInner dataInnerDerived = dataRootDerived.new TestInner();
    
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
  
  
  
  
  static void test_secondStateField(Example dataRoot) throws Exception{
    Object ovalue;
    int ivalue;
    //Build an access path: first field refer from the dataRoot, than in the refer instance field testint
    List<DataAccess.DatapathElement> path = new ArrayList<DataAccess.DatapathElement>(); 
    path.add(new DataAccess.DatapathElement("refer"));
    path.add(new DataAccess.DatapathElement("testint"));
    //
    DataAccess.Dst field = new DataAccess.Dst();
    ovalue = DataAccess.access(path, dataRoot, true, false, null, null, false, field);
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
    ovalue = DataAccess.access(path, datapool, true, false, null, null, false, field);
    ivalue = DataAccess.getInt(ovalue);
    assert(ivalue == 7890);
  }
  
  
  
  static void test_dataArrayAccessOperand() {
    String strAccess = "v2[1]";
    StringPartScan sp = new StringPartScan(strAccess);
    sp.setIgnoreWhitespaces(true);
    Map<String, DataAccess.IntegerIx> idxIdentifier = new TreeMap<String, DataAccess.IntegerIx>();
    idxIdentifier.put("v1", new DataAccess.IntegerIx(0));
    idxIdentifier.put("v2", new DataAccess.IntegerIx(1));
    Object[] vars = { "a1", new String[] {"a21" ,"a22,"}};
    Object value = null;
    try {
      CalculatorExpr.Operand acc = new CalculatorExpr.Operand(sp, idxIdentifier, null, false);
      value = acc.calc(null, vars);
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    check("a22".equals(value), "Test_DataAccess.test_dataArrayAccess");
  }

  
  
  static void test_dataOperationStringArgs(Example dataRoot) {
    String strAccess = "operationExample(v1, v1)";
    StringPartScan sp = new StringPartScan(strAccess);
    sp.setIgnoreWhitespaces(true);
    Map<String, DataAccess.IntegerIx> idxIdentifier = new TreeMap<String, DataAccess.IntegerIx>();
    idxIdentifier.put("v1", new DataAccess.IntegerIx(0));
    idxIdentifier.put("v2", new DataAccess.IntegerIx(1));
    Object[] vars = { "a1", new String[] {"a21" ,"a22,"}};
    Object value = null;
    try {
      DataAccess acc = new DataAccess(sp, null, null, '\0');
      value = DataAccess.invokeMethod(acc.datapath().get(0), null, dataRoot, true, vars, false);
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    check("a22".equals(value), "Test_DataAccess.test_dataArrayAccess");
  }

  
  
  static void test_dataArrayDataAccess(Example dataRoot) {
    String strAccess = "str[1]";
    Object value = null;
    try {
      DataAccess acc = new DataAccess(strAccess);
      value = acc.access(dataRoot, true, false, null, null);
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    check("s2".equals(value), "Test_DataAccess.test_dataArrayAccess");
  }
  
  static void check(boolean cond, String text) {
    if(!cond) {
      System.out.println("failed: " + text);
    } else {
      System.out.println("ok: " + text);
    }
  }

  
  
  
  
}



