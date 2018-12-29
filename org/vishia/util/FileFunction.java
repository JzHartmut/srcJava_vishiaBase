package org.vishia.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class FileFunction
{

  public interface AddFileToList
  {
    void add(File file);
  }
  

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

  /**Add files
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


   



}
