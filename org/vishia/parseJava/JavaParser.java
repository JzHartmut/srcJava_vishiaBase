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
//    JavaParser thiz = new JavaParser();
//    try {
//      thiz.parseJava("D:/vishia/Java/cmpnJava_vishiaBase/src/test/java/org/vishia/parseJava/test/ParseExample.java");
//    } catch (IllegalCharsetNameException | UnsupportedCharsetException | IOException e) {
//      // TODO Auto-generated catch block
//      e.printStackTrace();
//    }
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
        , "-class:JavaSrc"
        , "-struct:$(TMP)/JavaParser/JavaSyntax.zbnf.struct.txt"
        , "-all"
      };
    String sRet = GenZbnfJavaData.smain(args_genJavaOutClass);
    System.out.println(sRet);
    Debugutil.stop();
  }


  
  public JavaSrc parseJava(String pathJavasrc) throws IllegalCharsetNameException, UnsupportedCharsetException, FileNotFoundException, IOException {
    File fileIn = new File(pathJavasrc);
    JavaSrc res = parseJava(fileIn);
    for(JavaSrc.ClassDefinition theClass: res.get_classDefinition()) {
      JavaSrc.ClassContent theClassC = theClass.get_classContent();
      for(JavaSrc.MethodDefinition rOper: theClassC.get_methodDefinition()) {
        for(JavaSrc.Statement rStmnt: rOper.get_methodbody().get_statement()) {
          JavaSrc.Expression rExpr = rStmnt.get_Expression();      // statement is an expression
          if(rExpr !=null) {
            //rExpr.prep(rExpr, null);
            //JavaSrcPrep.ExpressionPrep rpExpr = new JavaSrcPrep.ExpressionPrep(rExpr, null);
            Debugutil.stop();
          }
        }
      }
    }
    return res;
  }  
  
  public JavaSrc parseJava(File fileIn) throws IllegalCharsetNameException, UnsupportedCharsetException, FileNotFoundException, IOException {
    boolean bOk = false;
    long timeStart = System.currentTimeMillis();
    Writer logParsingComponents = new FileWriter("T:/logParsingComponents.txt");
    this.parser.setLogComponents(logParsingComponents);
    //try 
    { bOk = this.parser.parseFile(fileIn); 
    } 
    logParsingComponents.close();
    this.parser.setLogComponents(null);
    
    //catch(Exception exc){ throw new IllegalArgumentException("JavaParser - file ERROR; " + fileIn.getAbsolutePath() + ":" + exc.getMessage() ); }
    if(!bOk) {
      String sError = this.parser.getSyntaxErrorReport();
      System.err.println("ERROR Parsing file: " + fileIn.getAbsolutePath() + "\n" + sError);
      return null;
    }
    else {
      System.out.println("JavaParser: parsing ok ms:" + Long.toString(System.currentTimeMillis() - timeStart));
      try {
        Writer outStore = new FileWriter("T:/javaParsResult.text");
        this.parser.writeResultAsTextList(outStore);
        outStore.close();
        System.out.println("JavaParser: write to T:/javaParsResult.text done ms:" + Long.toString(System.currentTimeMillis() - timeStart));
        
        
//        Zbnf2Xml.writeZbnf2Xml(parser, "T:/javaParsResult.xml", null);
//        System.out.println("JavaParser: write to T:/javaParsResult.xml done ms:" + Long.toString(System.currentTimeMillis() - timeStart));
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      //Content resultFile = new Content(pathJavasrc);   //Container for the parsed file.
      JavaSrc_Zbnf result = new JavaSrc_Zbnf();
      ZbnfParseResultItem resultItem = this.parser.getFirstParseResult();
      try{ ZbnfJavaOutput.setOutputStrictNewFromType(result, resultItem, this.console); }
      catch(Exception exc) {
        String sError = exc.getMessage();
        System.out.println("JavaParser: store to internal data ERROR ms:" + Long.toString(System.currentTimeMillis() - timeStart) + " ..." + sError);
        throw new IllegalArgumentException("JavaParser - internal ERROR storing parse result; " + exc.getMessage());
      }
      System.out.println("JavaParser: store to internal data done ms:" + Long.toString(System.currentTimeMillis() - timeStart));
      result.dataJavaSrc.postPrepare();
      return result.dataJavaSrc;
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
