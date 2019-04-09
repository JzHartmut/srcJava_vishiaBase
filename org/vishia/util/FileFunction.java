package org.vishia.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**This class is a container for some static routines and sub classes to deal with file content.
 * @author hartmut Schorrig, License LPGL
 *
 */
public class FileFunction
{

  public interface AddFileToList
  {
    void add(File file);
  }
  

  /**Not ready yet.
   * @author hartmut
   *
   */
  static class FileCheckPath implements FileVisitor {

    final String sMask;

    int pos1, pos9;


    FileCheckPath(String sMask) {
      this.sMask = sMask;
    }

    @Override
    public FileVisitResult preVisitDirectory(Object dir, BasicFileAttributes attrs) throws IOException
    {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public FileVisitResult visitFile(Object file, BasicFileAttributes attrs) throws IOException
    {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public FileVisitResult visitFileFailed(Object file, IOException exc) throws IOException
    {
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public FileVisitResult postVisitDirectory(Object dir, IOException exc) throws IOException
    {
      // TODO Auto-generated method stub
      return null;
    }
  };

  /**Add files. Not ready yet.
   * @param dir may be null, a directory as base for sPath.
   * @param sPath path may contain wildcard for path and file. May use backslash or slash.
   * @param listFiles Container to get the files.
   * @return false if the dir not exists or the deepst defined directory of a wildcard path does not exist.
   *   true if the search directory exists independent of the number of founded files.
   *   Note: The number of founded files can be query via the listFiles.size().
   */
  public static boolean addFileToList(File dir, String sPath, AddFileToList listFiles) {
  
    FileVisitor checker = new FileCheckPath(sPath);
  
  
  
    return false;
  }


  /**Returns some lines from a file, especially for documentation of sources.
   * @param file from this file
   * @param startLine at least from this start line, first line of file is 1
   * @param startString if given, from the line which contains this String after Start line. Typically an identifier. It needs few calculation time to test each line.
   * @param nrofLines either this nr of lines, set to greater value for the other conditions.
   * @param tillEmptyLine true then till any empty line is found (with spaces or only nl)
   * @param tillLesserIndent true then till a line is found which has lesser indentation as the found start line.
   *        In this case the return text does not contain the indentation. Indentation should be written only with spaces or only with tabs. 
   * @param terminateString if not null then till a found String inclusively this string, usual a ";" or "}"
   * @param onStartIndent true then the terminateString is expected only on the same indent as the start.
   * @param  
   * @return
   */
  public static CharSequence getLinesFile(File file, int startLine, String startString
  , int nrofLines, boolean tillEmptyLine, boolean tillLesserIndent
  , String terminateString, boolean onStartIndent
  , boolean removeIndent
  ) {
    BufferedReader r = null;
    try{ 
      r = new BufferedReader(new FileReader(file));
      int line = 0;
      CharSequence ret;
      String sLine;
      if(nrofLines > 1) {
      }
      do {
        sLine = r.readLine();
        line +=1;
      } while(line < startLine && sLine !=null); 
      if(startString !=null) {
        while(sLine !=null && !sLine.contains(startString)) {
          sLine = r.readLine();
        }
      }
      
      if(sLine == null) {
        ret = "startLine not contained in " + file.getAbsolutePath();
      }
      else if(nrofLines <= 1) {
        ret = sLine;
      } else {
        int posIndent = 0;
        int posIndent1 = 0;
        if(removeIndent || onStartIndent) {
          char cc;
          while(sLine.length() > posIndent && ((cc = sLine.charAt(posIndent)) == ' ' || cc == '\t')) {
            posIndent +=1;
          }
          posIndent1 = posIndent;
        }
        StringBuilder u = new StringBuilder(1000);
        if(removeIndent) { sLine = sLine.substring(posIndent); }
        u.append(sLine).append('\n');
        int nLine = 1;
        do {
          sLine = r.readLine();
          nLine +=1;
          if(sLine != null) { 
            posIndent1 = 0;
            if(removeIndent || onStartIndent) {
              char cc;
              while(sLine.length() > posIndent && posIndent1 < posIndent && ((cc = sLine.charAt(posIndent)) == ' ' || cc == '\t')) {
                posIndent1 +=1;
              }
              //do not remove not whitespace chars
              if(posIndent1 > 0 && removeIndent) { sLine = sLine.substring(posIndent1); }
            }
            
            u.append(sLine).append('\n'); 
          }
        } while (nLine < nrofLines && sLine !=null
                && (!tillEmptyLine || sLine.trim().length() == 0)
                && (  terminateString == null 
                   || (! onStartIndent && ! sLine.contains(terminateString) )
                   || (onStartIndent && ! (removeIndent ? sLine:sLine.substring(posIndent1)).startsWith(terminateString))
                )  );
        ret = u;
      }
      r.close();
      return ret;
    }
    catch(FileNotFoundException exc) {
      return "File not found: " + file.getAbsolutePath();
    } 
    catch(IOException exc) {
      if(r !=null){
        try { r.close(); } catch(IOException exc1) { throw new RuntimeException(exc1); }
      }
      return("IOException");
    }
  }



}
