package org.vishia.xmlparser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.vishia.util.StringPartFromFileLines;
import org.vishia.util.StringPartScan;

/**This class reads an XML with another XML template to store all or some data in Java instances. 
 * @author hartmut
 *
 */
public class Xml2Jdata
{
  public String XXXparse(File file) {
    String err = null;
    BufferedReader rd = null;
    try{ 
      rd = new BufferedReader(new FileReader(file));
    } catch(FileNotFoundException exc){
      err = exc.getMessage();
    }
    if(err == null) {
      String line;
      try {
        while( (line = rd.readLine())!=null){
        
        }
      } catch(IOException exc) {
        err = exc.getMessage();
      }
      try { rd.close();
      } catch(IOException exc) {
        err = exc.getMessage();
      }
    }
    return err;
  }



  public String parse(File fileIn) {
    String sError = null;
    StringPartScan spInput = null;
    try {
      spInput = new StringPartFromFileLines(fileIn);
    } catch (IOException exc) {
      sError = "Xml2Jdata - File not found," + fileIn.getAbsolutePath();
    }
    spInput.seekNoWhitespaceOrComments();
    if(!spInput.scan("<").scanOk()) sError = "XmlParser - Formaterror, first < not found. ";
    if(sError == null){
      if(spInput.scan("?xml").scanOk()) {  //<?xml .... ?> title line
        spInput.seek("?>");
      }
    }
    return sError;
  }


}
