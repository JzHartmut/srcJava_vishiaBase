/****************************************************************************/
/* Copyright/Copyleft:
 *
 * For this source the LGPL Lesser General Public License,
 * published by the Free Software Foundation is valid.
 * It means:
 * 1) You can use this source without any restriction for any desired purpose.
 * 2) You can redistribute copies of this source to everybody.
 * 3) Every user of this source, also the user of redistribute copies
 *    with or without payment, must accept this license for further using.
 * 4) But the LPGL ist not appropriate for a whole software product,
 *    if this source is only a part of them. It means, the user
 *    must publish this part of source,
 *    but don't need to publish the whole source of the own product.
 * 5) You can study and modify (improve) this source
 *    for own using or for redistribution, but you have to license the
 *    modified sources likewise under this LGPL Lesser General Public License.
 *    You mustn't delete this Copyright/Copyleft inscription in this source file.
 *
 * @author JcHartmut = hartmut.schorrig@vishia.de
 * @version 2006-06-15  (year-month-day)
 * list of changes:
 * 2007-10-15: JcHartmut www.vishia.de creation
 * 2008-04-02: JcHartmut some changes
 *
 ****************************************************************************/
package org.vishia.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.FileFilter;
import java.util.List;

/**This class supports some functions of file system access above the class java.io.File
 * and independent of other commonly or special solutions, only based on Java standard.
 * Some methods helps a simple using of functionality for standard cases.
 */
public class FileSystem
{


  /**Reads the content of a whole file into a String.
   * This method supplies a null pointer if a exception has occurs internally,
   * it throws never an Exception itself.
   * @param file The file should be exist, but don't need to exist.
   * @return null means, there is nothing to read. Otherwise the string contains the content of the file.
   */
  public static String readFile(File file)
  { String sContent;
    int sizeFile = (int) file.length();
    char[] content = new char[sizeFile];
    try
    { Reader reader = new FileReader(file);
      BufferedReader bReader = new BufferedReader(reader);
      bReader.read(content);
      sContent = new String(content);
    }
    catch(Exception exc)
    { sContent = null;   //on any exception, return null. Mostly file not found.
    }
    return sContent;
  }






  /**Reads the content of a whole file into a byte array.
   * This method supplies a null pointer if a exception has occurs internally,
   * it throws never an Exception itself.
   * @param file The file should be exist, but don't need to exist.
   * @return null means, there is nothing to read. Otherwise the byte[] contains the content of the file.
   */
  public static byte[] readBinFile(File file)
  { int sizeFile = (int) file.length();
    byte[] content;
    try
    { InputStream reader = new FileInputStream(file);
      content = new byte[sizeFile];   //reserve memory only if open not fails.
      reader.read(content);
    }
    catch(Exception exc)
    { content = null;   //on any exception, return null. Mostly file not found.
    }
    return content;
  }


  /**checks if a path exists or execute mkdir for all not existing directory levels.
   *
   * @param sPath The path. A file name on end will ignored. The last directory is written bevor last / .
   * @throws IOException If the path is not makeable.
   */
  public static void mkDirPath(String sPath)
  throws FileNotFoundException
  { int pos2 = sPath.lastIndexOf('/');
    if(pos2 >0)
    { String sPathDir = sPath.substring(0, pos2);
      File dir = new File(sPathDir);
      if(!dir.exists())
      { if(!dir.mkdirs())  //creates all dirs along the path.
        { //if it fails, throw an exception
          throw new FileNotFoundException("Directory path mkdirs failed:" + sPath);
        }
      }
    }
  }



  /**adds Files with the wildcard-path to a given list.
   *
   * @param sPath path with wildcards in the filename.
   * @param listFiles given list, the list will be extended.
   * @return true if anything is added, false if no matching file is found.
   * @throws FileNotFoundException
   */
  public static boolean addFileToList(String sPath, List<File> listFiles)
  throws FileNotFoundException
  { sPath = sPath.replace('\\', '/');
    return addFileToList(null, sPath, listFiles);
  }

  public static boolean addFileToList(File dir, String sPath, List<File> listFiles) throws FileNotFoundException
  { boolean bFound = true;
    String sDir = dir != null ? dir.getAbsolutePath() + "/" : "";
    int posWildcard = sPath.indexOf('*');
    if(posWildcard < 0)
    {
      File fFile = new File(sPath);
      bFound = fFile.exists();
      if(bFound)
      { listFiles.add(fFile);
      }
    }
    else
    { String sPathBefore = sPath.substring(0, posWildcard);
      String sPathBehind = sPath.length() >= posWildcard+3
                           && sPath.substring(posWildcard, posWildcard+3).equals("*.*")
                          ? sPath.substring(posWildcard +3)
                          : sPath.substring(posWildcard +1);
      int posSepBehind = sPathBehind.indexOf('/');
      int posSepDir = sPathBefore.lastIndexOf('/');
      if(posSepBehind >=0)
      { //after a wildcard-asterix a slash is found. It means, that some directory entries are to use.
        File dirBase;
        boolean bAllTree;
        if(sPathBehind.startsWith("*/"))
        { //it is the form path/**/path. it means, all levels are subdirs are to use.
          bAllTree = true;
        }
        else
        { //TODO example: path/name*name/path.
          bAllTree = false;
        }
        sPathBehind = sPathBehind.substring(posSepBehind+1);
        int posLastSlash = sPathBefore.lastIndexOf('/');
        String sPathDir; //, sDirMask;
        FileFilter dirMask = null;
        if(posLastSlash >=0)
        { sPathDir = sPathBefore.substring(0, posLastSlash);
          //sDirMask = sPathBefore.substring(posLastSlash+1);
        }
        else
        { sPathDir = ".";
          //sDirMask = sPathBefore;
        }
        dirBase = new File(sPathDir);
        if(!dirBase.isDirectory()) throw new FileNotFoundException("Dir not found:" + sPathDir);
        if(bAllTree)
        { addDirRecursivelyToList(dirBase, dirMask, sPathBehind, listFiles);
        }
      }
      else
      { //file filter
        String sPathDir;
        if(posSepDir >= 0)
        {
          sPathDir = sDir + sPathBefore.substring(0, posSepDir);
          sPathBefore = sPathBefore.substring(posSepDir+1);  //may be ""
        }
        else
        { sPathDir = sDir;
        }
        File fDir = new File(sPathDir);
        if(fDir.exists())
        { FileFilter filter = new WildcardFilter(sPathBefore, sPathBehind);
          File[] files = fDir.listFiles(filter);
          for(File file: files)
          { listFiles.add(file);
          }
        }
        else
        { bFound = false;
        }
      }
    }
    return bFound;
  }



  private static void addDirRecursivelyToList(File dirParent, FileFilter dirMask, String sPath, List<File> listFiles) throws FileNotFoundException
  {
    addFileToList(dirParent, sPath, listFiles);  //the files inside
    File[] subDirs = dirParent.listFiles();  //TODO use dirmask to filter /name**name/ or
    for(File dirChild: subDirs)
    { if(dirChild.isDirectory())
      { addDirRecursivelyToList(dirChild, dirMask, sPath, listFiles);
      }
    }
  }







  private static class WildcardFilter implements FileFilter
  {
    private final String sPathNameBeforeWildchar, sPathNameBehindWildchar;

    public WildcardFilter(String sPathBefore, String sPathBehind)
    { sPathNameBeforeWildchar = sPathBefore;
      sPathNameBehindWildchar = sPathBehind;
    }


    public boolean accept(File file)
    {
      String sName =file.getName();
      if(sName.startsWith(sPathNameBeforeWildchar) && sName.endsWith(sPathNameBehindWildchar))
      { return true;
      }
      else
      { return false;
      }
    }
  }












}
