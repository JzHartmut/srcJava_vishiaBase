package org.vishia.testutil;

import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;

import org.vishia.util.Debugutil;

public class TestConditionCombi {

  /**Result item of parsing the expression. Consist of the number (the table number) and the select string.
   */
  static public class NumString { 
    
    public final int nr; 
    public final String sel; 
    
    NumString ( int nr, String sel){ this.nr = nr; this.sel = sel;} 
    @Override public String toString() { return "" + Integer.toString(this.nr) + ":" + this.sel; }
  }

  
  
  
  /**Internal operation to parse the test case string. 
   * @param select
   * @param nrConditions
   * @return combined list internal use
   * @throws ParseException
   */
  private static List<List<List<List<NumString>>>> parseTestCases ( String select, int nrConditions) throws ParseException {
    List<List<List<List<NumString>>>> selistAll = new LinkedList<List<List<List<NumString>>>>(); 
    int zSelAll = select.length();
    int ixSelAll = 0;
    while(ixSelAll < zSelAll) {                  // eval whole String, more as one xxx : xxx colon separated blocks
      int posColon = select.indexOf(':', ixSelAll); if(posColon <0) { posColon = zSelAll; }
      List<List<List<NumString>>> selistAnd = new LinkedList<List<List<NumString>>>(); 
      selistAll.add(selistAnd);
      //                                         // eval String till : 
      String selAnd = select.substring(ixSelAll, posColon); ixSelAll = posColon +1;
      int ixSelAnd = 0; int zSelAnd = selAnd.length();
      while(ixSelAnd < zSelAnd) {
        int posAmp = selAnd.indexOf('&', ixSelAnd); if(posAmp <0) { posAmp = zSelAnd; }
        List<List<NumString>> selistOr = new LinkedList<List<NumString>>(); 
        selistAnd.add(selistOr);
        //                                         // eval String till : 
        String selAdd = selAnd.substring(ixSelAnd, posAmp); ixSelAnd = posAmp +1;
        int ixSelAdd = 0; int zSelAdd = selAdd.length();
        while(ixSelAdd < zSelAdd) {
          int posSep = selAdd.indexOf('+', ixSelAdd); if(posSep <0) { posSep = zSelAdd; }
          List<NumString> selistLine = new LinkedList<NumString>(); 
          selistOr.add(selistLine);
          //                                         // eval String till : 
          String selLine = selAdd.substring(ixSelAdd, posSep); ixSelAdd = posSep +1;
          int zSelLine = selLine.length();
          List<NumString> selistItem = selistLine; //new LinkedList<NumString>(); 
          //selistLine.add(selistItem);
          String sel = null; int nLine = 0;
          int nLine1 = 0;
          for(int ix = 0; ix < zSelLine; ++ix) {
            char cc = selLine.charAt(ix);
            if(sel == null) {                  //line index
              if(" \n\r\t".indexOf(cc)>=0 && nLine1 ==0) {} //ignore
              else if(cc >='0' && cc <= '9') { nLine1 = 10*nLine1 + (cc - '0'); }
              else if(cc == '=') { 
                nLine = nLine1; sel = ""; 
                if(nLine <=0 || nLine > nrConditions) {
                  throw new ParseException("faulty nrCondition =>" + selLine.substring(ix), nLine);
                }
              }
              else { throw new ParseException("expected '=', unexpected: =>" + selLine.substring(ix), ix); }          // first character
            }
            else if(" \n\r\t".indexOf(cc)>=0 && sel.length()==0) {} //ignore
            else if( ", \n\r\t&;+".indexOf(cc) >= 0) {
              selistItem.add(new NumString(nLine, sel));
              if(cc != ',') {
                nLine1 = 0;              // not a , => another config number
                sel = null;              //sel=null forces newly read config number
              } else {
                sel = "";
              }
            } 
            else if(Character.isLetterOrDigit(cc) || cc=='_' ){
              sel += cc;                       //a next char
            }
            else {
              throw new ParseException("unexpected: =>" + selLine.substring(ix), ix);  
            }
          } //for ...zSelLine
          if(sel !=null) {    //space finishes an item
            selistItem.add(new NumString(nLine, sel));
            sel = null;
          }
        } //while ... zSelAdd
      } //while ...zSelAnd
    } //while ...zSelAll
    //                                           //selListAll is filled. 
    return selistAll;
  }

  
  
  /**Prepares the container for comparison, see {@link #checkSameItem(String, CharSequence...)}
   * The select pattern has the following syntax (ZBNF):<pre>
   * select::= { <selAnd> ? : }.  ##multiple independent select pattern, one should match, ':' is a OR 
   * selAnd::= { <selOr> ? & }.   ##select pattern which`s all parts <selOr> should match.
   * selOr::=  { <selLine> ? + }. ##select pattern part which checks some lines, one should match, separated with |
   * selLine::= { [{<#table>?,}=]{<selItem>? , }[;] }. ##select pattern for items in lines. starts optional with one digit line number.
   * selItem::= ... ends either with space or an following upper case character. </pre>     
   * Examples:
   * <ul>
   * <li>"1=Aa 2=Bb" expects "Aa" in the first line and "Bb" in the second line of table resp. cmp[0], cmp[1].
   * <li>"1=Aa,Ab 2=Bb" expects "Aa" or "Ab" in the first cmp and "Bb" in the second cmp.
   * <li>"1=Aa,Ab 2=Bb + Zz" expects as above, or "Zz" in all lines.
   * <li>"1=Aa,Ab 2=Bb + Zz & 3=X" expects as above, but "X" in cmp[2] in any case . 
   *                            If "Zz" is contained in line 3, "X" is not need.
   *                            If only 2 lines exists, "3=X" is effectless
   * <li>"1Aa,Ab 2=Bb + Zz & 3=X : Y" as above, but alternatively also "Y" in all lines is recognized as ok.
   * </ul>
   * @param select text for selection.
   * @return The container parsed from select. It contains all combinations as List. 
   *   One item contains an array of select strings for all tables. The order of elements in the array is the order of tables. 
   *   All items are selected test cases.
   *   The return .size() is the number of test cases 
   * @throws ParseException 
   */
  public static List<NumString[]> prepareTestCases(String select, int nConditions) {
    
    List<NumString[]> testcasesAll = new LinkedList<NumString[]>();  //filled for return, NumString is ix with select string
    
    List<List<List<List<NumString>>>> selistAll;
    try {
      selistAll = parseTestCases(select, nConditions);     // prepares the test cases in lists as given in select
      for( List<List<List<NumString>>> listOr: selistAll ) { // roll to stop 1th level: select::= { <selAnd> ? : }.
        List<List<NumString[]>> combinAnds = new LinkedList<List<NumString[]>>();
        for (List<List<NumString>> listAnd: listOr) {      // roll to stop 2th level: selAnd::= { <selOr> ? & }.
          List<NumString[]> combinAdds = new LinkedList<NumString[]>();
          for( List<NumString> listAdd: listAnd ) {        // roll to stop 3th level: selOr::=  { <selLine> ? + }.
            @SuppressWarnings("unchecked")
            LinkedList<String>[] conditionsArray = new LinkedList[nConditions];
            @SuppressWarnings("unused")
            int mCond = 0;                                 // ?? sort all members of listAdd to one of its conditionArray.
            for( NumString caseItem: listAdd) {            // roll to stop 3th level: selLine::= { [{<#table>?,}=]{<selItem>? , }[;] }.
              int ixCond = caseItem.nr -1;                 // ixCond is #table, count from 0, 
              mCond |= 1<<ixCond;
              if(conditionsArray[ixCond] ==null) { conditionsArray[ixCond] = new LinkedList<String>(); }
              conditionsArray[ixCond].add(caseItem.sel);
              Debugutil.stop();
            }
            Debugutil.stop();
            List<NumString[]> combinations = new LinkedList<NumString[]>();
            combinations.add(new NumString[nConditions]);  //with all entries empty.
            for(int ixCond = 0; ixCond < nConditions; ++ixCond) {
              List<String> conditions = conditionsArray[ixCond];
              if(conditions !=null) {
                boolean bFirst = true;
                LinkedList<NumString[]> combinationsNew = new LinkedList<NumString[]>();
                for(String condition : conditions) {
                  for(NumString[] testcase: combinations) {
                    NumString[] testcaseNew;
                    if(bFirst) {
                      testcaseNew = testcase; //first, use the existing one
                      bFirst = false;
                    } else {
                      testcaseNew = new NumString[nConditions];
                      for(int ixCase = 0; ixCase < testcase.length; ++ixCase) {
                        testcaseNew[ixCase] = testcase[ixCase];   //duplicate the case
                      }
                    }
                    testcaseNew[ixCond] = new NumString(ixCond+1, condition);
                    combinationsNew.add(testcaseNew);
                  }
                }
                combinations = combinationsNew;     //all test cases with condition applied
              }
            }
            combinAdds.addAll(combinations);
            Debugutil.stop();
          }
          Debugutil.stop();
          combinAnds.add(combinAdds);               //contains the ... & .... expressions
        } 
        //combin the ands
        List<NumString[]> testcases = new LinkedList<NumString[]>();
        boolean bFirstCombinAnd = true;
        for(List<NumString[]> combinAnd: combinAnds) {  //re-work with the ... + ... 
          //boolean bFirstAnd = true;
          //List<NumString[]> casesAdd = new LinkedList<NumString[]>();
          //for(List<NumString[]> combinCases: combinAdds) {     //re-work with 1=Ab,Cd; 2=Xy; 
          List<NumString[]> testcasesNew = new LinkedList<NumString[]>();
          boolean bFirstVariant = true;  
          for(NumString[] combin : combinAnd) {          //all testcase of a part till ... +
              if(bFirstCombinAnd) {
                testcasesNew.add(combin);                  //first: copy only the testcases from combinations
                //casesAnd.add(testcase);
              } else {
                
                for(NumString[] testcase: testcases) {
                  if(bFirstVariant) {
                    testcasesNew.add(testcase);
                    for(int ix =0; ix < nConditions; ++ix) {
                      if(combin[ix] !=null) {
                        testcase[ix] = combin[ix];  //merge it.
                      }
                    }
                  } else {
                    NumString[] testcaseNew = new NumString[nConditions];
                    testcasesNew.add(testcaseNew);
                    for(int ix =0; ix < nConditions; ++ix) {
                      if(combin[ix] !=null) {
                        testcaseNew[ix] = combin[ix];  //merge it.
                      } else {
                        testcaseNew[ix] = testcase[ix];
                      }
                    }
                  }
                }
                
              }
              bFirstVariant = false;
            }  
          bFirstCombinAnd = false;
          testcases = testcasesNew;
             //}
        }
        testcasesAll.addAll(testcases);
      }
    } catch (ParseException exc) {
      // TODO Auto-generated catch block
      System.out.println(exc.getMessage());
    }
    return testcasesAll;
  }
  
  
  
  /**Checks whether the nr in each item element is the same as position in array as expected.
   * @param selectList The list returned from {@link #prepareTestCases(String, int)}
   * @param shouldThrow true then throws, false then returns false on error. 
   * @return true if all ok.
   */
  boolean assertCheckNr(List<NumString[]> selectList, boolean shouldThrow) {
    boolean bOk = true;
    int ixItem = 0;
    for(NumString[] item: selectList) {
      for(int ix=0; ix < item.length; ++ix) {
        if(item[ix].nr !=ix) { 
          if(shouldThrow) {
            throw new IllegalArgumentException("inconsistent nr in item" + ixItem);
          }
          bOk = false; break; 
        }
      }
      if(!bOk) break;
      ixItem +=1;
    }
    return bOk;
  }
  
  
  
} //class
