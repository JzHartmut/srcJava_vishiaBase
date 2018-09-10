package org.vishia.fileLocalAccessor.test;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Set;
import java.util.TreeSet;

public class TestJava7Files
{

  public static void main(String[] args){
    test_children();  
  }
  
  
  /**Gets children files from any directory.
   * 
   */
  protected static void test_children(){
    
    //FileSystem fsystem = FileSystem.;
    Path path = Paths.get("D:/Docu/Java/javadoc6");
    Set<FileVisitOption> options = new TreeSet<FileVisitOption>();
    
    try {
      Files.walkFileTree(path, options, 2, visitor);
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    
  }
  
  
  
  protected static FileVisitor<Path> visitor = new FileVisitor<Path>(){

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc)
        throws IOException
    {
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
        throws IOException
    {
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
        throws IOException
    {
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc)
        throws IOException
    {
      return FileVisitResult.CONTINUE;
    }
    
  };
  
}
