package org.vishia.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

/**This class uses the {@link Files#walkFileTree(Path, FileVisitor)} capability to gather files to a list with a given path with wildcards.
 * @author Hartmut Schorrig
 *
 */
public class FilesToList
{


  /**Version, history and license.
 * <ul>
 * <li>2018-01-20 Hartmut created. It should replace {@link FileSystem#addFileToList(String, org.vishia.util.FileSystem.AddFileToList)} with the better {@link Files} class from Java7
 *   instead the {@link File} capability from the Java 1.0.  
 * </ul>
 * <br><br>
 * <b>Copyright/Copyleft</b>:
 * For this source the LGPL Lesser General Public License,
 * published by the Free Software Foundation is valid.
 * It means:
 * <ol>
 * <li> You can use this source without any restriction for any desired purpose.
 * <li> You can redistribute copies of this source to everybody.
 * <li> Every user of this source, also the user of redistribute copies
 *    with or without payment, must accept this license for further using.
 * <li> But the LPGL is not appropriate for a whole software product,
 *    if this source is only a part of them. It means, the user
 *    must publish this part of source,
 *    but don't need to publish the whole source of the own product.
 * <li> You can study and modify (improve) this source
 *    for own using or for redistribution, but you have to license the
 *    modified sources likewise under this LGPL Lesser General Public License.
 *    You mustn't delete this Copyright/Copyleft inscription in this source file.
 * </ol>
 * If you are intent to use this sources without publishing its usage, you can get
 * a second license subscribing a special contract with the author. 
 * 
 * @author Hartmut Schorrig = hartmut.schorrig@vishia.de
 * 
 */
  public String version = "2018-01-20";

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


  FilepathFilter.NameFilter[] nameCheck;




  /**Add files
   * @param dir may be null, a directory as base for sPath.
   * @param sPath path may contain wildcard for path and file. May use backslash or slash.
   * @param listFiles Container to get the files.
   * @return false if the dir not exists or the deepst defined directory of a wildcard path does not exist.
   *   true if the search directory exists independent of the number of founded files.
   *   Note: The number of founded files can be query via the listFiles.size().
   */
  public static boolean addFileToList(File dir, String sPath, AddFileToList listFiles) {
  
    CharSequence sPath1 = FileSystem.normalizePath(sPath);
    if(dir !=null) {
      CharSequence sDir = FileSystem.normalizePath(dir.getAbsolutePath());
      StringBuilder sPath2;
      if(sPath1 instanceof StringBuilder) {
        sPath2 = (StringBuilder)sPath1;
      } else {
        sPath2 = new StringBuilder(sPath1); 
      }
      sPath2.insert(0, '/');
      sPath2.insert(0, sDir);
      sPath1 = sPath2;  
    }
    int posWildcard = StringFunctions.indexOf(sPath1, '*');
    Path start;
    if(posWildcard <0) {
      int posLastSlash = StringFunctions.lastIndexOf(sPath1, 0, posWildcard, '/');
      assert(posLastSlash >= 0); //because it is an absolute path with / on start.
      start = Paths.get(sPath1.toString());
    } else {
      int posLastSlash = StringFunctions.lastIndexOf(sPath1, 0, posWildcard, '/');
      assert(posLastSlash >= 0); //because it is an absolute path with / on start.
      start = Paths.get(sPath1.toString());
    }

    FileVisitor checker = new FileCheckPath(sPath);
  
    
  
    try {
    Files.walkFileTree(start, checker);
    } catch(IOException exc) {
      return false;
    }
  
    return true;
  }


   



}
