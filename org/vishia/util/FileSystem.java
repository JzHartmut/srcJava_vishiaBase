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
 * 2009-05-06 Hartmut: new: writeFile(content, file);
 * 2009-05-06 Hartmut: bugfix: addFileToList(): A directory was also added like a file if wildcards are used. It is false, now filtered file.isFile().
 * 2009-03-24 Hartmut: new: isAbsolutePathOrDrive()
 * 2008-04-02 Hartmut: some changes
 * 2007-10-15 Hartmut: www.vishia.org creation
 *
 ****************************************************************************/
package org.vishia.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.FileFilter;
import java.io.Writer;
import java.util.List;

/**This class supports some functions of file system access above the class java.io.File
 * and independent of other commonly or special solutions, only based on Java standard.
 * Some methods helps a simple using of functionality for standard cases.
 */
public class FileSystem
{

  public interface AddFileToList
  {
    void add(File file);
  }
  
  
  /**This class supports the call of {@link #addFileToList(String, List)}. */
  private static class ListWrapper implements AddFileToList
  { private final List<File> files;
    public ListWrapper(List<File> files){ this.files = files; }
    public void add(File file){ files.add(file); }
  };

  
  /**This class holds a File with its Basepath. It is used inside 
   * {@link #addFilesWithBasePath(String, List)}.
   * The user should create a List<FileAndBasePath> and supply it to this method.
   *
   */
  public static class FileAndBasePath
  { final public File file;
    final public String basePath;
    final public String localPath;
    FileAndBasePath(File file, String sBasePath, String localPath)
    { this.file = file; 
      this.basePath = sBasePath;
      this.localPath = localPath;
    }
  }
  
  /**Temporary class used only inside {@link #addFilesWithBasePath}.
   * An instance is created for all files of one call of {@link #addFileToList}
   */
  private static class FilesWithBasePath implements AddFileToList
  {
    /**injected composition of the String of base path. */
    final String sPathBase;
    /**Aggregation of the list, it is defined at user level. */
    final List<FileAndBasePath> list;   
    
    final int posLocalPath;
    
    /**Construtor fills the static members. */
    FilesWithBasePath(String sPathBase, int posLocalPath, List<FileAndBasePath> list)
    { this.sPathBase = sPathBase; 
      this.list = list;
      this.posLocalPath = posLocalPath;
    }
    
    /**Implements interface method. */
    public void add(File file)
    { final String localPath; 
      String absPath = file.getAbsolutePath();
      if(posLocalPath >0)
      { localPath = absPath.substring(posLocalPath);
      }
      else
      { localPath = absPath;
      }
      FileAndBasePath entry = new FileAndBasePath(file, sPathBase, localPath); //sPathBase ist from constructor
      list.add(entry);
    }
    
    
  }
  
  /**Fills the list with found files to sPath.
   * Example:
   * <pre>
   * addFilesWithBasePath("..\\example/dir:localdir/ ** /*.h", list);
   * </pre>
   * fills <code>../example/dir/</code> in all elements basePath of list,
   * and fills all files with mask <code>*.h</code> from localdir and all sub folders,
   * with the local name part starting with <code>localdir/...</code> 
   * in all elements localPath.
   * @param sPath may contain a <code>:</code>, this is instead <code>/</code> 
   *        and separates the base path from a local path.
   *        The sPath may contain backslashes for windows using, it will be converted to slash. 
   * @param list The list to fill in files with the basepath. 
   *        The basepath is <code>""</code> if no basepath is given in sPath.
   * @return false if no file is found.
   * @throws FileNotFoundException
   */
  public static boolean addFilesWithBasePath(String sPath, List<FileAndBasePath> list) 
  throws FileNotFoundException
  { final String sPathBase;
    final File dir;
    final int posLocalPath;
    int posBase = sPath.indexOf(':');
    if(posBase >=2)
    { sPathBase = (sPath.substring(0, posBase) + "/").replace('\\', '/');
      dir = new File(sPathBase);
      String sBasepathAbsolute = dir.getAbsolutePath();
      //The position after the separator after the absPath of base directory
      // is the start of the local path.
      posLocalPath = sBasepathAbsolute.length() +1;  
      sPath = sPath.substring(posBase +1);
    }
    else 
    { sPathBase = ""; 
      posLocalPath = 0;
      dir = null;
    }
    //The wrapper is created temporary to hold the informations about basepath
    // to fill in the members of the list. 
    // The wrapper instance isn't necessary outside of this static method. 
    FilesWithBasePath wrapper = new FilesWithBasePath(sPathBase, posLocalPath, list);
    return FileSystem.addFileToList(dir,sPath, wrapper);
  }
  
  
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


  public static boolean writeFile(String content, String sFile)
  { boolean bOk = true;
    try{
      FileWriter writer = new FileWriter(sFile, false);
      if(true) //writer.open(sFile, false)>=0)
      {
        writer.write(content); 
        writer.close();
      } else {
        bOk = false;
      }
    } catch (IOException e)
    { bOk = false;
    }
    return bOk;
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


  
  
  
  /**Returns true if the String which describes a file path is recognized as an absolute path.
   * The conditions to recognize as absolute path are:
   * <ul>
   * <li>Start with slash or backslash
   * <li>Contains a ':' as second char. In this case on windows it is a drive letter.
   *   the path should used as absolute path mostly, because an access to another drive is done,
   *   the current directory or another directory couldn't be applied.
   * </ul>  
   * @param filePath 
   * @return
   */
  public static boolean isAbsolutePathOrDrive(String filePath)
  { char cc;
    return (filePath.length() >=2 && filePath.charAt(1)== ':') //a drive using is detect as absolute path.
        || (filePath.length() >=1 
           && ( (cc=filePath.charAt(0))== '/' || cc == '\\') //slash or backslash as first char
           )
           ;
    
  }
  
  
  
  
  /**Builds a relative path from a given directory, from an input path and the reference directory.
   * The input path and the reference directory may be relative paths both, than both have to start 
   * on the same point in the file tree. If there are absolute paths, it have to start with the same drive letter (Windows).
   * <br><br>
   * Examples:
   * 
   * <table>
   * <tr><td>a/b/c/d  </td><td>a/b      </td><td>c/d</td><td>If the sRefDirectory is within sInput, sInput will be shortened:</td></tr>  
   * <tr><td>a/b/c/d  </td><td>x/y      </td><td>../../a/b/c/d</td><td>If the sRefDirectory is parallel to sInput, ../ will be added and sRefDirectory will be added:</td></tr>  
   * <tr><td>a/b/c/d  </td><td>a/x      </td><td>../b/c/d</td><td>If the sRefDirectory is within sInput but parallel, ../ will be added and the rest of sInput will be shortened:</td></tr>  
   * @param sInput The given file path.
   * @param sRefDir Reference from where the return value is built relative. 
   *        A file-name of this reference file isn't relevant. 
   *        If only the directory should be given, a '/' on end have to be present.
   *        The reference directory may given with a relative path, 
   *        than it should be start at the same directory as the relative given sInput.
   * @return The file path relative to the sRefFile-directory.
   */
  public static String relativatePath(String sInput, String sRefDir)
  { int posInput = 0;
    if(sInput.startsWith("../../../examples_XML"))
      FileSystem.stop();
    /* old algorithm, the newer does the same and more.
    int posHtmlRef =0;
    while(posHtmlRef >=0 && sInput.substring(posInput, posInput+3).equals("../")){
      //relative path to left
      if(sRefFile.substring(posHtmlRef, posHtmlRef+3).equals("../")){
         posHtmlRef +=3;  //both files are more left, delete 1 level of  ../
         posInput +=3;
      }
      else {
        int posSlash = sRefFile.indexOf('/', posHtmlRef);
        if(posSlash >=0){
          posHtmlRef =posSlash +1;  //after '/'
          posInput -=3;  //go to left, because the output file is more right.
        }
        else {
          posHtmlRef = -1; //to break.
        }
        while(posInput < 0){
          posInput +=3;
          sInput = "../" + sInput; //it may be errornuoes because the input is more left as a root.
        }
      }
    }
    */
    final String sOutput1 = sInput.substring(posInput);
    //check whether the both paths are equal, shorten the sInput at the equal part.
    boolean bCont;
    int posSep = -1;
    do{
      int posSep2 = sRefDir.indexOf('/', posSep +1);
      if(posSep2 >0){
        bCont = sRefDir.substring(0, posSep2).equals(sInput.substring(0, posSep2));
      } else {
        bCont = false;
      }
      if(bCont){
        posSep = posSep2;
      }
    }while(bCont);
    //detect a longer ref path:
    String sBack = "";
    int posSepRefNext = posSep+1; //follow after last equate '/'
    int posSepRefNext2; 
    int pathdepth = 0;
    /**check path of reference dir, which is the source of the relative path.
     * Correct the path with necessary "../" to go out a directory.
     */ 
    while( (posSepRefNext2 = sRefDir.indexOf('/', posSepRefNext) )>=0){ //another / found
      if(posSepRefNext2 == posSepRefNext+2 && sRefDir.substring(posSepRefNext, posSepRefNext2).equals("..")){
        int sBackLength = sBack.length();
          if(sBackLength >=3){ sBack = sBack.substring(0, sBackLength-3);} //shorten "../"
          else { pathdepth -=1; }
      } else if( posSepRefNext2 == posSepRefNext) {
          //do nothing, same depth, two "//" one after another
      } else if( posSepRefNext2 == posSepRefNext+1 && sRefDir.substring(posSepRefNext, posSepRefNext2).equals(".")){
          //do nothing, same depth  
      } else
      { //deeper level of source of link:
        if(pathdepth <0){ 
          pathdepth +=1;
        } else {
    	    sBack +="../";
        }  
      }
      posSepRefNext = posSepRefNext2 + 1;
    }
    final String sOutput = sBack + sOutput1.substring(posSep+1); //posSep may be 0, than its the same.
    return sOutput;
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

  public static boolean addFileToList(String sPath, AddFileToList listFiles)
  throws FileNotFoundException
  { sPath = sPath.replace('\\', '/');
    return addFileToList(null, sPath, listFiles);
  }

  public static boolean addFileToList(File dir, String sPath, List<File> listFiles) throws FileNotFoundException
  {
    ListWrapper listWrapper = new ListWrapper(listFiles);
    //NOTE: the listFiles is filled via the temporary ListWrapper.
    return addFileToList(dir, sPath, listWrapper);
  }
  
  public static boolean addFileToList(File dir, String sPath, AddFileToList listFiles) throws FileNotFoundException
  { boolean bFound = true;
    String sDir = dir != null ? dir.getAbsolutePath() + "/" : "";
    int posWildcard = sPath.indexOf('*');
    if(posWildcard < 0)
    {
      File fFile = new File(sDir + sPath);
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
        { sPathDir = sDir + sPathBefore.substring(0, posLastSlash);
          //sDirMask = sPathBefore.substring(posLastSlash+1);
        }
        else
        { sPathDir = sDir + ".";
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
        { sPathDir = sDir.length()==0 ? "." : sDir; //current Dir if no Dir is given.
        }
        File fDir = new File(sPathDir);
        if(fDir.exists())
        { FileFilter filter = new WildcardFilter(sPathBefore, sPathBehind);
          File[] files = fDir.listFiles(filter);
          for(File file: files)
          { if(file.isFile())
            { listFiles.add(file);
            }
          }
        }
        else
        { bFound = false;
        }
      }
    }
    return bFound;
  }



  private static void addDirRecursivelyToList(File dirParent, FileFilter dirMask, String sPath, AddFileToList listFiles) throws FileNotFoundException
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






  private static void stop()
  { //only for breakpoint
  }





}
