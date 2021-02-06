package org.vishia.testutil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import org.vishia.util.FileFunctions;

public class EvalTestoutfiles {


  public static String evalFiles(List<String> testNames, File fTestDir, Appendable problems) {
    if(!fTestDir.exists() && !fTestDir.isDirectory()) {
      throw new IllegalArgumentException("not a directory: " + fTestDir.getAbsolutePath());
    } 
    StringBuilder result = new StringBuilder(40);
    try {
      for(String testName : testNames) {
        if(testName !=null) {
          File ftestOut = new File(fTestDir, testName + ".out");
          if(ftestOut.exists()) {
            try {
              BufferedReader testFileContent = new BufferedReader(new FileReader(ftestOut));
              String line;
              int nrTest =0, nrTestOk =0, nrTestError =0;
              boolean inTest = false;
              while( (line = testFileContent.readLine()) !=null) {
                if(!inTest) {
                  if(line.startsWith("Test: ")) {
                    nrTest +=1;
                    inTest = true;
                  }
                }
                else { //inTest
                  if(line.startsWith("Test: ")) {
                    nrTestError +=1;  //ok missing
                  }
                  else if(line.equals("ok")) {
                    nrTestOk +=1;
                    inTest = false;
                  }
                }
              }
              char cresult;
              if(nrTestError >0) { cresult = 'E'; }
              else if(nrTestOk != nrTest) { cresult = 'e'; }
              else if(nrTestOk > 12) { cresult = '.'; }
              else  { cresult = '\"'; }
              result.append(cresult);
              if(cresult !='.') {
                problems.append(testName).append('\n');
              }
            } catch (IOException e) {
              // TODO Auto-generated catch block
              result.append('?');
              e.printStackTrace();
            }
          } else {
            result.append('-');
          }
        }
      } //while(testName !=null);
      //
      //FileFunctions.writeFile(result.toString(), new File(fTestDir, "eval.txt"));
//    } catch (FileNotFoundException e) {
//      // TODO Auto-generated catch block
//      e.printStackTrace();
//    } catch (IOException e) {
//      // TODO Auto-generated catch block
//      e.printStackTrace();
    } finally {
//      if(rNames !=null) {
//        try {
//          rNames.close();
//          rNames = null;
//        } catch (IOException e) {}
//      }
    }
    return result.toString();
  }


}
