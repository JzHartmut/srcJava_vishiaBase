package org.vishia.parseJava;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.text.ParseException;

import org.vishia.mainCmd.MainCmdLoggingStream;
import org.vishia.mainCmd.MainCmdLogging_ifc;
import org.vishia.util.Debugutil;
import org.vishia.zbnf.ZbnfParseResultItem;
import org.vishia.zbnf.ZbnfParser;
import org.vishia.zbnf.GenZbnfJavaData;
import org.vishia.zbnf.Zbnf2Xml;
import org.vishia.zbnf.ZbnfJavaOutput;

public class JavaParser {

  
  public static void main(String args[]) {
    
    genDstClassForContent();
    JavaParser thiz = new JavaParser();
    thiz.parseJava("D:/vishia/Java/cmpnJava_vishiaBase/src/test/java/org/vishia/spehw/SpiSlave.java");
  }
  
  private final ZbnfParser parser;
  
  private final MainCmdLogging_ifc console;
  
  
  public JavaParser() {
    this.console = new MainCmdLoggingStream(System.out);
    this.parser = new ZbnfParser(this.console, 10);
    try {
      this.parser.setSyntaxFromJar(getClass(), "JavaSyntax.zbnf");
    } catch (IllegalCharsetNameException | UnsupportedCharsetException | IOException | ParseException e) {
      System.err.println("Error initializing JavaParser for syntax: " + e.getMessage());
    }
    this.parser.setReportIdents(MainCmdLogging_ifc.error, MainCmdLogging_ifc.fineInfo, MainCmdLogging_ifc.debug, MainCmdLogging_ifc.fineDebug);
   }

  static void genDstClassForContent() {
    String[] args_genJavaOutClass = 
      { "-s:D:/vishia/Java/cmpnJava_vishiaBase/src/main/java/srcJava_vishiaBase/org/vishia/parseJava/JavaSyntax.zbnf"
        , "-dirJava:$(TMP)/JavaParser"
        , "---dirJava:D:/vishia/Java/cmpnJava_vishiaBase/src/main/java/srcJava_vishiaBase"
        , "-pkg:org.vishia.parseJava"
        , "-class:JavaContent"
        , "-all"
      };
    GenZbnfJavaData.smain(args_genJavaOutClass);
    Debugutil.stop();
  }


  public JavaContent parseJava(String pathJavasrc) {
    File fileIn = new File(pathJavasrc);
    boolean bOk = false;
    try { bOk = this.parser.parseFile(fileIn); } 
    catch(Exception exc){ throw new IllegalArgumentException("CheaderParser - file ERROR; " + fileIn.getAbsolutePath() + ":" + exc.getMessage() ); }
    ZbnfParseResultItem resultItem = this.parser.getFirstParseResult();
    if(!bOk) {
      String sError = this.parser.getSyntaxErrorReport();
      System.err.println("ERROR Parsing file: " + fileIn.getAbsolutePath() + "\n" + sError);
      return null;
    }
    else {
      
      try {
        Writer outStore = new FileWriter("T:/javaParsResult.text");
        this.parser.writeResultAsTextList(outStore);
        outStore.close();
        
        
        Zbnf2Xml.writeZbnf2Xml(parser, "T:/javaParsResult.xml", null);
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      //Content resultFile = new Content(pathJavasrc);   //Container for the parsed file.
      JavaContent_Zbnf result = new JavaContent_Zbnf();
      try{ ZbnfJavaOutput.setOutputStrictNewFromType(result, resultItem, this.console); }
      catch(Exception exc) {
        throw new IllegalStateException("JavaParser - internal ERROR storing parse result; " + exc.getMessage());
      }
      return result;
    }    
  }
    
    
  public static class Content 
  {
    public final String filePath;

    /**ZBNF result: */
    public String packageDefinition;
    
    public Content(String filePath) {
      this.filePath = filePath;
    }
    
  }  
    
  
}
