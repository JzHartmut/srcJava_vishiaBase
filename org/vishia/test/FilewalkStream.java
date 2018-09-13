package org.vishia.test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;
 
/**Simple test of {@link Files#walk(Path, int, java.nio.file.FileVisitOption...)} with {@link Stream}
 * @author Hartmut Schorrig
 *
 */
public class FilewalkStream {
 
  public static void main(String[] args) {
    
    // java 7 : try with resources and use of Path
    Path path = Paths.get("C:", "Program Files");  //Will be force java.nio.file.AccessDeniedException on some windows dirs, are adminstrator accessible only
    try(Stream<Path> stream = Files.walk(path,2)) {
    
      stream.filter(path1 -> path1.toFile().isDirectory())
                                      .forEach(System.out::println);
 
    } catch(IOException ioe ){
      System.err.println(ioe.getMessage());
    }
  }
 
}
