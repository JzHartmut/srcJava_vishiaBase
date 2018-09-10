package org.vishia.batch;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;

import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import javax.tools.JavaFileObject.Kind;

public class RunJavaBatch {

  
  StringBuilder source = new StringBuilder();
  
  InputJava inputJava = new InputJava();

  public static final void main(String[] args){
    RunJavaBatch main = new RunJavaBatch();
    main.execute(args);
  }
  
  
  void execute(String[] args)
  {
    String sJavaFile = args[0];
    try{
      BufferedReader reader = new BufferedReader(new FileReader(sJavaFile));
      String sLine;
      boolean bStart = false;
      while( (sLine = reader.readLine())!=null){
        if(bStart){
          source.append(sLine).append("\n");
        } else {
          bStart = sLine.contains("exit");
        }
      }
      reader.close();
    } catch(IOException exc){}
  }
  
  void compile(String sJavaFile)
  {
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    JavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);
    DiagnosticListener<JavaFileObject> diagnosticListener = null;
    List<String> classesForAnnotation = null;
    List<String> options = new LinkedList<String>();
    options.add("-d");
    options.add("../..");
    options.add("-sourcepath");
    options.add(".");
    options.add("-classpath");
    options.add("D:/vishia/Java/exe/vishiaBase.jar");

    List<SimpleJavaFileObject> compilationUnits = new LinkedList<SimpleJavaFileObject>();
    compilationUnits.add(inputJava);
    //compilationUnits.add(sJavaFile + ".java");
    Writer out;
    boolean bOk = true;
    try{ out = new FileWriter(sJavaFile + ".class");
    
    } catch(IOException exc){
      System.err.println("RunJavaBatch - can't create classfile; " + sJavaFile);
      out = null;
      bOk = false;
    }
    if(bOk){
      JavaCompiler.CompilationTask task= compiler.getTask(out, fileManager, null/*diagnosticListener*/, options
          , classesForAnnotation, compilationUnits);

    }
  }
  
  
  
  private class InputJava extends SimpleJavaFileObject
  {
    protected InputJava() {
      super(URI.create("string:///jbatch" + Kind.SOURCE.extension), Kind.SOURCE);
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors){
      return source;
    }
  }
  
  
  
  
  
  
}
