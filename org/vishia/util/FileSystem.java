package org.vishia.util;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


/*Test with Jbat: call Jbat with this java file with its full path:
D:/vishia/Java/srcJava_vishiaBase/org/vishia/util/FileSystem.java
==JZcmd==
currdir = scriptdir;
Obj found = java org.vishia.util.FileSystem.searchInParent(File: ".", "_make/xgenjavadoc.bat", "ximg"); 
==endJZcmd==
*/





/**This class supports some functions of file system access as enhancement of the class java.io.File
 * independently of other classes of vishia packages, only based on Java standard.
 * Some methods helps a simple using of functionality for standard cases.
 * <br><br>
 * Note that the Java-7-Version supplies particular adequate functionalities in its java.nio.file package.
 * This functionality is not used here. This package was created before Java-7 was established. 
 * <br><br>
 * <b>Exception philosophy</b>:
 * The java.io.file classes use the exception concept explicitly. Thats right if an exception is not the normal case.
 * But it is less practicable if the throwing of the exception is an answer of an expected situation 
 * which should be though tested in any case.
 * <br><br>
 * Some methods of this class doesn't throw an exception if the success can be checked with a simple comparison
 * and it should be inquired in usual cases. For example {@link #getDirectory(File)} returns the directory or null 
 * if the input file is not existing. It does not throw a FileNotFoundException. The result is expected, the user
 * can do a null-check easily.
 * 
 */
/**
 * @author hartmut
 *
 */
public class FileSystem
{

  /**Version, history and license.
   * Changes:
   * <ul>
   * <li>2019-12-06 Hartmut new: {@link #readInJar(Class, String, String)} as simple read possibility of a text file. 
   * <li>2017-09-09 Hartmut new: {@link #cleandirForced(File)}, {@link #copyDir(File, File)} 
   * <li>2016-01-17 Hartmut new: {@link #getFirstFileWildcard(File)}   
   * <li>2015-09-06 JcHartmut chg: instead inner class WildcardFilter the new {@link FilepathFilter} used.    
   * <li>2015-05-25 new {@link #copyFile(File, File, boolean, boolean, boolean)}   
   * <li>2015-05-04 bugfix close in {@link #readFile(File)}
   * <li>2015-05-03 new {@link #searchInParent(File, String...)}
   * <li>2014-09-05 Hartmut bugfix: {@link #searchInFiles(List, String, Appendable)} missing close().  
   * <li>2014-08-01 Hartmut bugfix in {@link #grep1line(File, String)}: Should close the file. Nobody does it elsewhere.
   * <li>2014-06-03 Hartmut bugfix in {@link #normalizePath(CharSequence)}: it has not deleted
   *   <code>path/folder/folder/../../ because faulty following start search position. 
   * <li>2014-05-10 Hartmut new: {@link #delete(String)} 
   * <li>2013-10-27 Hartmut chg: {@link #normalizePath(CharSequence)} now uses a given StringBuilder to adjust the path
   *   inside itself. normalizePath(myStringBuilder) does not need the return value. 
   *   But normalizePath(myStringBuilder.toString()) normalizes in a new StringBuilder.
   *   Rule: A CharSequence can't be seen as persistent. Only a String as CharSequence is persistent. 
   * <li>2013-08-29 Hartmut bugfix: {@link #normalizePath(CharSequence)}, {@link #isAbsolutePath(CharSequence)}
   * <li>2013-06-27 Hartmut new: {@link #close(Closeable)}
   * <li>2013-05-04 Hartmut chg: {@link #normalizePath(CharSequence)} uses and returns a CharSequence yet.
   * <li>2013-03-31 Hartmut bugfix: {@link #addFileToList(AddFileToList, File, String, int, FilenameFilter, FilenameFilter, int)}
   *   had gotten the content of path/** /sub twice. 
   * <li>2013-03-29 Hartmut new: {@link #cleandir(File)}
   * <li>2013-02-13 Hartmut chg: {@link #addFileToList(String, AddFileToList)} new better algorithm
   * <li>2013-02-03 Hartmut chg: the {@link #addFileToList(String, AddFileToList)} does not throw a FileNotFoundException
   *   instead it returns false. All try-catch Blocks of a user calling environment may be changed to <code>catch(Exception e)</code>
   *   instead of <code>catch(FileNotFoundException e)</code> as simple work arround.
   * <li>2013-02-03 Hartmut new: {@link #normalizePath(String)}
   * <li>2013-02-03 Hartmut chg: {@link #addFilesWithBasePath(File, String, List)} improved
   * <li>2013-01-20 Hartmut bugfix: {@link #addFilesWithBasePath(File, String, List)}:If a /../ is used in the path,
   *   it was faulty. Usage of canonicalpath instead absolute path. 
   *   {@link #addFileToList(File, String, AddFileToList)}: Sometimes dir not regarded.  
   * <li>2013-01-12 Hartmut new: Method checkNewless(src, dst, deleteIt)
   * <li>2012-12-30 Hartmut chg: {@link #addFilesWithBasePath(File, String, List)} now gets a base directory.
   * <li>2012-12-25 Hartmut chg: {@link #mkDirPath(String)} now returns the directory which is designated by the argument
   *   and checks whether it is a directory, not a file. It is more stronger, elsewhere compatible.
   * <li>2012-08-12 Hartmut chg: {@link #addFileToList(File, String, List)} some changes to support zipfiles.
   * <li>2012-01-26 Hartmut new: {@link #isSymbolicLink(File)} and {@link #cleanAbsolutePath(String)}.
   * <li>2012-01-05 Hartmut new: {@link #rmdir(File)}
   * <li>2011-08-13 Hartmut chg: {@link #addFilesWithBasePath(String, List)} now stores the localPath 
   *     with '/' instead backslash on windows too. Strategy: Use slash generally in Java-applications.
   *     Only a java.lang.File instance can contain backslash, because it is gotten from basic file routines
   *     such as File.listFiles() called in addFileToList(..). TODO use File.list() instead File.listFiles()
   *     and build the File-instance after replace('/', '\\'). The advantage of only have slash: The user
   *     should not be search to both backslash and slash while evaluating a file path. 
   *     In Java a path with slash works proper any time. 
   *     Only if a line for execution of the windows operation systems command shell is generated,
   *     all slash have to be converted to backslash lastly. See change 2011-06-22 of this file:
   *     {@link #getCanonicalPath(File)} returns slash in operation system MS-Windows too.   
   * <li>2011-07-10 Hartmut new: {@link #absolutePath(String, File)}. The requirement was: 
   *   Usage of "~/path" to select in the users home in linux.
   * <li>2011-06-22 {@link #getCanonicalPath(File)} returns slash in operation system MS-Windows too.
   * <li>2011-07-10 Hartmut
   * <li>2009-12-29 Hartmut bugfix: addFilesWithBasePath(...): If the sPath contains a ':' on second pos (Windows Drive), than the method hadn't accepted a ':' inside, fixed.
   * <li>2009-12-29 Hartmut bugfix: mkDirPath(...): not it accepts '\' too (Windows)
   * <li>2009-12-29 Hartmut corr: addFileToList(...): uses now currentDir, if param dir== null.
   * <li>2009-12-29 Hartmut new: method relativatePath(String sInput, String sRefDir)
   * <li>2009-05-06 Hartmut new: writeFile(content, file);
   * <li>2009-05-06 Hartmut bugfix: addFileToList(): A directory was also added like a file if wildcards are used. It is false, now filtered file.isFile().
   * <li>2009-03-24 Hartmut new: isAbsolutePathOrDrive()
   * <li>2008-04-02 Hartmut corr: some changes
   * <li>2007-10-15 Hartmut: creation
   * <li>2007 Hartmut: created
   * </ul>
   * 
   * 
   * <b>Copyright/Copyleft</b>:
   * For this source the LGPL Lesser General Public License,
   * published by the Free Software Foundation is valid.
   * It means:
   * <ol>
   * <li> You can use this source without any restriction for any desired purpose.
   * <li> You can redistribute copies of this source to everybody.
   * <li> Every user of this source, also the user of redistribute copies
   *    with or without payment, must accept this license for further using.
   * <li> But the LPGL ist not appropriate for a whole software product,
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
  public final static String sVersion = "2016-01-17";

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
    @Override public String toString() { return basePath+ ":" + localPath; }
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
    
    /**Construtor fills the static members.
     * @param sPathBase 
     * @param posLocalPath
     * @param list
     */
    FilesWithBasePath(String sPathBase, int posLocalPath, List<FileAndBasePath> list)
    { this.sPathBase = sPathBase; 
      this.list = list;
      this.posLocalPath = posLocalPath;
    }
    
    /**Implements interface method. */
    public void add(File file)
    { final String localPath; 
      String absPath = file.getAbsolutePath();
      if(posLocalPath >0 && absPath.length() >posLocalPath)
      { localPath = absPath.substring(posLocalPath).replace('\\', '/');
      }
      else
      { localPath = absPath.replace('\\', '/');
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
   * @param baseDir A base directoy which is the base of sPath. It can be null, then sPath should describe a valid 
   *   file path.
   * @param sPath may contain a <code>:</code>, this is instead <code>/</code> 
   *        and separates the base path from a local path.
   *        The sPath may contain backslashes for windows using, it will be converted to slash. 
   * @param list The list to fill in files with the basepath. 
   *        The basepath is <code>""</code> if no basepath is given in sPath.
   * @return false if no file is found.
   * @throws FileNotFoundException
   */
  public static boolean addFilesWithBasePath(final File baseDir, final String sPath, List<FileAndBasePath> list) 
  //throws FileNotFoundException
  { final String sPathBase;
    final File dir;
    final int posLocalPath;
    int posBase = sPath.indexOf(':',2);
    final String sPathLocal;
    final CharSequence sAbsDir;
    if(posBase >=2)
    { sPathBase = (sPath.substring(0, posBase) + "/").replace('\\', '/');
      sPathLocal = sPath.substring(posBase +1).replace('\\', '/');
      String sAbsDirNonNormalized = baseDir !=null ? baseDir.getAbsolutePath() + "/" + sPathBase : sPathBase;
      sAbsDir = normalizePath(sAbsDirNonNormalized);
      posLocalPath = sAbsDir.length();
      dir = new File(sAbsDir.toString()); //baseDir, sPathBase);
    }
    else 
    { //sPathBase = "";
      String sBaseDir = baseDir.getAbsolutePath();
      sAbsDir = normalizePath(sBaseDir);
      if(sBaseDir.length() != sAbsDir.length()){
        dir = new File(sAbsDir.toString());
      } else {
        dir = baseDir;  //use same instance, the path is correct
      }
      sPathLocal = sPath.replace('\\', '/');
      posLocalPath = sAbsDir.length();
    }
    //The wrapper is created temporary to hold the informations about basepath
    // to fill in the members of the list. 
    // The wrapper instance isn't necessary outside of this static method. 
    //FilesWithBasePath wrapper = new FilesWithBasePath(sPathBase, posLocalPath, list);
    FilesWithBasePath wrapper = new FilesWithBasePath(sAbsDir.toString(), posLocalPath, list);
    return FileSystem.addFileToList(dir,sPathLocal, wrapper);
  }
  
  
  /**Reads the content of a whole file into a String.
   * This method returns a null pointer if an exception has occurs internally,
   * it throws never an Exception itself.
   * @param file The file should be exist, but don't need to exist.
   * @return null means, there is nothing to read. Otherwise the string contains the content of the file.
   */
  public static String readFile(File file)
  { String sContent;
    try
    { Reader reader = new FileReader(file);
      BufferedReader bReader = new BufferedReader(reader);
      int sizeFile = (int) file.length();
      char[] content = new char[sizeFile];
      bReader.read(content);
      sContent = new String(content);
      bReader.close();
      reader.close();
    }
    catch(Exception exc)
    { sContent = null;   //on any exception, return null. Mostly file not found.
    }
    return sContent;
  }

  
  
  
  /**Read a file ('Resource') located in a jar file or inside the compiled class files. 
   * @param clazz The path starts in the package where this class is located and uses the ClassLoader of this class.
   * @param pathInJar relative to the class.
   *   Use "filename.ext" if the file is in the same package as the clazz.
   *   Use "../package/filename.ext" if the file is in another parallel package.
   *   Note: The file is searched in the bin tree of class files on using in Eclipse IDE
   * @param encoding use usually "UTF-8" or "US-ASCII" or "ISO-8859-1" for byte-coded content
   *   or "UTF-16BE"
   * @return null on error, throws never. Usual an error is a programmatically error not a user's failure.
   *   Else the return value is a StringBuilder which may be able to change, but it should be used readonly only.
   *   Use toString() to keep it persistent. 
   */
  public static CharSequence readInJar(Class clazz, String pathInJar, String encoding) {
    //String pathMsg = "jar:" + clazz.getPackage().getName() + "/" + pathInJar;
    //ClassLoader classLoader = ClassLoader.getSystemClassLoader();
    //classLoader.getResource("slx.cfg.xml");
    try {
      InputStream jarStream = clazz.getResourceAsStream(pathInJar);  //pathInJar with slash: from root.
      if(jarStream == null) return null;
      int zBytes = jarStream.available();  //nr of bytes of this file in jar.
      StringBuilder buf = new StringBuilder(zBytes);  //maybe some less characters on encoding.
      InputStreamReader reader = new InputStreamReader(jarStream, encoding);
      int cc;
      while( (cc = reader.read()) !=-1) {
        buf.append((char)cc);
      }
      reader.close();
      //cfgStream.close();
      return buf;
    } catch(IOException exc) {
      return null; //usual faulty pathInJar 
      //throw new IllegalArgumentException("predefBlocks read from jar fails: " + pathMsg + " error: " + exc);
    }
    
  }
  
  

  /**Writes the given String as content to a file without exception throwing.
   * This method doesn't throws an exception but returns true of false. It may more simple in application.
   * @param content any textual information
   * @param sFile The path of the file.
   * @return true if done, false if there was any exception internally.
   */
  public static boolean writeFile(String content, String sFile)
  { boolean bOk = true;
    try{
      FileWriter writer = new FileWriter(sFile, false);
      writer.write(content); 
      writer.close();
    } catch (IOException e)
    { bOk = false;
    }
    return bOk;
  }


  /**Writes the given String as content to a file without exception throwing.
   * This method doesn't throws an exception but returns true of false. It may more simple in application.
   * @param content any textual information
   * @param sFile The file.
   * @return true if done, false if there was any exception internally.
   */
  public static boolean writeFile(String content, File file)
  { boolean bOk = true;
    try{
      FileWriter writer = new FileWriter(file, false);
      writer.write(content); 
      writer.close();
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
      reader.close();
    }
    catch(Exception exc)
    { content = null;   //on any exception, return null. Mostly file not found.
    }
    return content;
  }


  /**Reads the content of a file into a byte array. Only the first bytes are read from the file if the buffer is lesser than the file.
   * This can be used especially to check the head data of a file. The file is opened as {@link FileInputStream}, read and closed again. 
   * @param file The file should be exist, but don't need to exist.
   * @return nrofBytes read , see java.io.InputStream.read(byte[]). 0 if the file is not existing or any error occurs (especially file is locked).
   */
  public static int readBinFile(File file, byte[] buffer)
  { int nrofBytes;
    try
    { InputStream reader = new FileInputStream(file);
      nrofBytes = reader.read(buffer);
      reader.close();
    }
    catch(Exception exc)
    { 
    	nrofBytes = 0;
    }
    return nrofBytes;
  }


  /**Writes the content of a whole file from a byte array.
   * @param file The file should be exist, but don't need to exist.
   * @return nrofBytes written , see java.io.OutputStream.write(byte[])
   */
  public static int writeBinFile(File file, byte[] buffer)
  { int nrofBytes;
    try
    { OutputStream writer = new FileOutputStream(file);
      writer.write(buffer);
      writer.close();
      nrofBytes = buffer.length;
    }
    catch(Exception exc)
    { 
    	nrofBytes = 0;
    }
    return nrofBytes;
  }

  
  /**Copy a file. The time-stamp and read-only-properties will be kept for dst. 
   * @param src A src file. 
   * @param dst The dst directory should be exist. Use {@link #mkDirPath(String)} with this dst to create it.
   * @return Number of bytes copied. -1 if src file not found. 0 if the src file is empty.
   * @throws IOException Any error. but not if the src file not found.
   */
  public static int copyFile(File src, File dst) 
  throws IOException
  { return copyFile(src, dst, true, true, true);
  }
  
  /**Copy a file. The read-only-properties will be kept for dst.
   * If a dst file exists it will be overwritten. This is done also if the dst file may be read only. 
   * @param src A src file. 
   * @param dst The dst directory should be exist. Use {@link #mkDirPath(String)} with this dst to create it.
   * @param bKeepTimestamp true then sets the time stamp of the dst to the time stamp of the src.
   * @param bOverwrite if true then overwrite an existing file. If false exception on existing file.
   * @param bOverwriteReadonly if true then overwrite a read-only file if necessary.
   *   If false exception on trying overwrite a read-only file.
   * @return Number of bytes copied. -1 if src file not found. 0 if the src file is empty.
   * @throws IOException Any error. but not if the src file not found.
   * @throws IllegalArgumentException if the dst cannot be overwritten.
   */
  public static int copyFile(File src, File dst
      , boolean bKeepTimestamp, boolean bOverwrite, boolean bOverwriteReadonly) 
  throws IOException
  { 
  	int nrofBytes = 0;
  	byte[] buffer = new byte[16384];
  	if(dst.exists()){
  		if(!bOverwrite) throw new IllegalArgumentException("FileSystem.copyFile - dst exists, " + dst.getAbsolutePath());
  	  if(!dst.canWrite()){
  	    if(!bOverwriteReadonly) throw new IllegalArgumentException("FileSystem.copyFile - dst is read-only, " + dst.getAbsolutePath());
  			dst.setWritable(true);
  		}
  		if(!dst.delete()) throw new IllegalArgumentException("FileSystem.copyFile - dst cannot be deleted, " + dst.getAbsolutePath());
  	}
  	InputStream inp;
  	try{ inp = new FileInputStream(src);
  	}catch(FileNotFoundException exc){
  		nrofBytes = -1;
  		inp = null;
  	}
  	if(inp != null){
	  	OutputStream out = new FileOutputStream(dst);
	  	int nrofBytesBlock;
	  	do{
	  	  nrofBytesBlock = inp.read(buffer);
	  	  if(nrofBytesBlock >0){
	  	  	nrofBytes += nrofBytesBlock;
	  	  	out.write(buffer, 0, nrofBytesBlock);
	  	  }
	  	}while(nrofBytesBlock >0);
	  	inp.close();
	  	out.close();
	  	if(bKeepTimestamp) {
  	    long timeSrc = src.lastModified();
  	    dst.setLastModified(timeSrc);
	  	}
	    if(!src.canWrite()){
	    	dst.setWritable(false);
	    }
  	}
	  return nrofBytes;
  }
  
  /**checks if a path exists or execute mkdir for all not existing directory levels.
  *  If the file should be a directory but it doesn't exists, the parent directory is created.
  *  That is because it is not able to detect whether a non-existing directory path is a directory.
  *  Use {@link #mkDirPath(String)} with "/" on end to create a directory.
  * @param file Either any file or any directory with given path. 
  * @throws IOException If the path is not makeable.
  */
  public static void mkDirPath(File file)
  throws FileNotFoundException
  {
  	if(file.exists()) return;
  	String sName = file.getAbsolutePath();
  	if(file.isDirectory()){ 
      //assert(false); 
  	  sName = sName + "/"; 
    }
  	mkDirPath(sName);
  }
  

  /**checks if a path exists or execute mkdir for all not existing directory levels.
   *
   * @param sPath The path. A file name on end will ignored. 
   *        The used path to a directory is all before the last / or backslash.
   * @return the directory of the path.
   * @throws FileNotFoundException If the path exists and it is a file or if it is not able to create.
   */
  public static File mkDirPath(String sPath)
  throws FileNotFoundException
  { int pos2 = sPath.lastIndexOf('/');
    int pos3 = sPath.lastIndexOf('\\');
    if(pos3 > pos2){ pos2 = pos3; }
    if(pos2 >0)
    { String sPathDir = sPath.substring(0, pos2);
      File dir = new File(sPathDir);
      if(!dir.exists())
      { if(!dir.mkdirs())  //creates all dirs along the path.
        { //if it fails, throw an exception
          throw new FileNotFoundException("Directory path mkdirs failed;" + sPath);
        }
      }
      if(!dir.isDirectory()){
        throw new FileNotFoundException("path is a file, should be a directoy;" + sPath);
      }
      return dir;
    }
    else return new File(".");  //the current directory is the current one.
  }

  
  
  public static boolean delete(String path){
    boolean bOk;
    if(path.indexOf('*')<0){
      File fileSrc = new File(path);
      if(fileSrc.isDirectory()){
        bOk = FileSystem.rmdir(fileSrc);
      } else {
        bOk = fileSrc.delete();
      }
    } else {
      //contains wildcards
      List<File> files = new LinkedList<File>();
      bOk = addFileToList(path, files);
      if(bOk){
        for(File file: files){
          boolean bFileOk = file.delete();
          if(!bFileOk){ bOk = false; }
        }
      }
    }
    return bOk;
  }
  
  
  
  /**Removes all files inside the directory and all sub directories with its files.
   * Then remove the dir itself.
   * @param dir A directory or a file inside the directory
   * @return true if all files are deleted. If false then the deletion process was aborted.
   */
  public static boolean rmdir(File dir){
    if(!dir.isDirectory()){ dir = getDir(dir); }
    boolean bOk = cleandir(dir);
    bOk = bOk && dir.delete();   //delete only if bOk!
    return bOk;
  }
  

  /**Removes all files inside the directory and all sub directories with its files.
   * The dir itself will be remain.
   * @param dir_file A directory or any file inside the directory.
   * @return true if all files are deleted. If false then the deletion process was aborted.
   */
  public static boolean cleandir(File dir_file){
    File dir = dir_file;
    boolean bOk = true;
    if(!dir.exists()) {
      bOk = dir.mkdirs();
    }
    if(bOk) {
      if(!dir.isDirectory()){ dir = getDir(dir); }
      File[] files = dir.listFiles();
      for(File file: files){
        if(file.isDirectory()){
          bOk = bOk && cleandir(file);
        } 
        bOk = bOk && file.delete(); //maybe the directory.
      }
    }
    return bOk;
  }
  

  /**Removes all files inside the directory and all sub directories with its files.
   * The dir itself will be remain.
   * If files or directories inside cannot be deleted, they will be remain 
   * but the other files or directories will be tried to delete nevertherless.
   * It is in opposite to {@link #cleandir(File)}, the last one aborts if one file cannot delete. 
   * @param dir_file A directory or any file inside the directory.
   * @return true if all files are deleted. If false then at least one file or directory cannot delete. 
   */
  public static boolean cleandirForced(File dir_file){
    File dir = dir_file;
    boolean bOk = true;
    if(!dir.exists()) {
      bOk = dir.mkdirs();
    }
    if(!dir.isDirectory()){ dir = getDir(dir); }
    File[] files = dir.listFiles();
    for(File file: files){
      if(file.isDirectory()){
        bOk &= cleandirForced(file);
      }
      bOk &= file.delete(); //maybe the directory.
    }
    return bOk;
  }
  

  /**Removes all files inside the directory and all sub directories with its files.
   * The dir itself will be remain.
   * If files or directories inside cannot be deleted, they will be remain 
   * but the other files or directories will be tried to delete nevertherless.
   * It is in opposite to {@link #cleandir(File)}, the last one aborts if one file cannot delete. 
   * @param dir_file A directory or any file inside the directory.
   * @return true if all files are deleted. If false then at least one file or directory cannot delete. 
   */
  public static boolean copyDir(File src, File dst){
    boolean bOk = true;
    if(!dst.exists()) {
      bOk = dst.mkdirs();
    }
    File[] files = src.listFiles();
    for(File file: files){
      if(file.isDirectory()){
        File srcChild = new File(src, file.getName());
        File dstChild = new File(dst, file.getName());
        bOk &= copyDir(srcChild, dstChild);
      }
      else {
        try { 
          bOk &= (copyFile(file, new File(dst, file.getName())) >=0); //maybe the directory.
        } catch(IOException exc) {
          bOk = false;
        }
      }
    }
    return bOk;
  }
  

  /**Returns the directory of the given file.
   * Note that the {@link java.io.File#getParentFile()} does not return the directory if the File is described as a relative path
   * which does not contain a directory. This method builds the absolute path of the input file and returns its directory. 
   * @param file
   * @return null if the file is the root directory. 
   *   To distinguish whether the file is not exist or it is the root directory one can check file.exist().  
   * throws FileNotFoundException if the file is not existing and therefore the directory of it is not able to build.
   */
  public static File getDirectory(File file) throws FileNotFoundException
  { File dir;
    if(!file.exists()) throw new FileNotFoundException("not exists:" + file.getName());
    if(!file.isAbsolute()){
      file = file.getAbsoluteFile();
    }
    dir = file.getParentFile();
    return dir;
  }
  
  
  
  /**Returns all matching files of the directory. Adequate {@link java.io.File-listFiles} but builds a filter internally.
   * @param dir
   * @param nameWithWildcard Can have up to 2 '*' as wildcards
   * @return
   */
  public static File[] getFiles(File dir, CharSequence nameWithWildcard)
  {
    FilenameFilter filter = new FilepathFilter(nameWithWildcard);
    return dir.listFiles(filter);
  }
  
  
  /**Returns the first file with the matching name in the given directory.
   * A file may start with a defined name but the rest of name may be unknown, for example designated with a time stamp.
   * If only one proper file exists, this is unique. Elsewhere the first exemplar of a matching file may be usefully.
   * @param dir
   * @param nameWithWildcard Can have up to 2 '*' as wildcards
   * @return
   */
  public static File getFirstFileWildcard(File file) //, CharSequence nameWithWildcard)
  { File dir = file.getParentFile();
    String nameWithWildcard = file.getName();
    FilenameFilter filter = new FilepathFilter(nameWithWildcard);
    File[] files = dir.listFiles(filter);
    if(files !=null && files.length >0) return files[0];
    else return null;
  }
  
  
  
  
  
  /**Returns the directory of the given file.
   * Note that the {@link java.io.File#getParentFile()} does not return the directory if the File is described as a relative path
   * which does not contain a directory. This method builds the absolute path of the input file and returns its directory. 
   * @param file
   * @return null if the file does not exists or the file is the root directory. 
   *   To distinguish whether the file is not exist or it is the root directory one can check file.exist().  
   */
  public static File getDir(File file)
  { File dir;
    if(!file.exists()) return null;
    if(!file.isAbsolute()){
      file = file.getAbsoluteFile();
    }
    dir = file.getParentFile();
    return dir;
  }
  
  

  
  
  /**Returns true if the String which describes a file path is recognized as an absolute path.
   * The conditions to recognize as absolute path are:
   * <ul>
   * <li>Start with slash or backslash
   * <li>Contains a ':' as second char following by '/' or '\'. 
   *   In this case on windows it is another drive as absolute path.
   * </ul>  
   * @param filePath 
   * @return true if it is such an absolute path
   */
  public static boolean isAbsolutePath(CharSequence filePath)
  { char cc;
    return filePath.length() >=3 && filePath.charAt(1)== ':' //a drive using is detect as absolute path.
           && ( (cc=filePath.charAt(2))== '/' || cc == '\\')  //slash or backslash as first char
        || filePath.length() >=1 
           && ( (cc=filePath.charAt(0))== '/' || cc == '\\') //slash or backslash as first char
           ;
  }
  

  
  
  /**Returns true if the String which describes a file path is recognized as an absolute path or as a path with drive letter
   * which may be relative on the drive.
   * The conditions to recognize as absolute path are:
   * <ul>
   * <li>Start with slash or backslash
   * <li>Contains a ':' as second char. In this case on windows it is another drive.
   *   the path should be used as absolute path mostly, because 
   *   the current directory or any other base directory couldn't be applied.
   * </ul>  
   * @param filePath 
   * @return
   */
  public static boolean isAbsolutePathOrDrive(CharSequence filePath)
  { char cc;
    return (filePath.length() >=2 && filePath.charAt(1)== ':') //a drive using is detect as absolute path.
        || (filePath.length() >=1 
           && ( (cc=filePath.charAt(0))== '/' || cc == '\\') //slash or backslash as first char
           )
           ;
    
  }
  
  
  
  /**Gets the canonical path of a file without exception and with unique slashes. 
   * See java.io.File.getCanonicalPath().
   * The separator between directory names is the slash / in windows too!
   * It helps to work with unique designation of paths. 
   * The original java.io.File.getCanonicalPath() produces a backslash in windows systems.
   * On Unix systems the canonical path resolves symbolic links. 
   * <br><br>
   * To normalize a path use {@link #normalizePath(CharSequence)} or {@link #normalizePath(File)}.  
   * @param file The given file
   * @return null if the canonical path isn't available, for example if the path to the file doesn't exists.
   */
  public static String getCanonicalPath(File file)
  { String sPath;
  	try{ 
  	  sPath = file.getCanonicalPath();
  	  sPath = sPath.replace('\\', '/');
  	}
  	catch(IOException exc){ sPath = null; }  //the file doesn't exists.
  	return sPath;
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
   * @param sRefPath Reference from where the return value is built relative. 
   *        A file-name of this reference file isn't relevant. 
   *        If only the directory should be given, a '/' on end have to be present.
   *        The reference directory may given with a relative path, 
   *        than it should be start at the same directory as the relative given sInput.
   * @return The file path relative to the sRefFile-directory.
   */
  public static String relativatePath(String sInput, String sRefPath)
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
      int posSep2 = sRefPath.indexOf('/', posSep +1);
      if(posSep2 >=0){  //if path starts with '/', continue. It checks from the last '/' +1 or from 0.
        bCont = sInput.length() >= posSep2 && sRefPath.substring(0, posSep2).equals(sInput.substring(0, posSep2));
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
    while( (posSepRefNext2 = sRefPath.indexOf('/', posSepRefNext) )>=0){ //another / found
      if(posSepRefNext2 == posSepRefNext+2 && sRefPath.substring(posSepRefNext, posSepRefNext2).equals("..")){
        int sBackLength = sBack.length();
          if(sBackLength >=3){ sBack = sBack.substring(0, sBackLength-3);} //shorten "../"
          else { pathdepth -=1; }
      } else if( posSepRefNext2 == posSepRefNext) {
          //do nothing, same depth, two "//" one after another
      } else if( posSepRefNext2 == posSepRefNext+1 && sRefPath.substring(posSepRefNext, posSepRefNext2).equals(".")){
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
    final String sOutput = sBack + (posSep +1 < sOutput1.length() ? sOutput1.substring(posSep+1) : ""); //posSep may be 0, than its the same.
    return sOutput;
  }
  
  
  /**Converts to the absolute path if a relative path or HOME path is given.
   * The filePath may start with
   * <ul><li>"./" - then the currDir is replaced.
   * <li>"~/" - then the home dir is replaced. The home dir is the string 
   *     containing in the HOME environment variable. This style of notification is usual in Linux/Unix
   * <li>"../../" - then the parent of currDir is replaced.
   * </ul>    
   * @param sFileNameP filename. It may contain "\\". "\\" are converted to "/" firstly. 
   * @param currDir The current dir or null. If null then the current dir is gotten calling new File(".");
   * @return The path as absolute path. It is not tested whether it is a valid path. 
   *   The path contains / instead \ on windows.
   */
  public static String absolutePath(String sFileNameP, File currDir)
  { String sFileName = sFileNameP.replace('\\', '/');
    final String sAbs;
    if(sFileName.startsWith("~")){ //The home directory
      String sHome = System.getenv("HOME");
      sAbs = sHome + sFileName.substring(1);
    } else if(sFileName.startsWith("./")){
      if(currDir == null){
        currDir = new File(".");   //maybe environment PWD etc. but that is always valid 
      }
      sAbs = currDir + sFileName.substring(1); 
    }
    else if(sFileName.startsWith("../")){
      if(currDir == null){
        currDir = new File(".");   //maybe environment PWD etc. but that is always valid 
      }
      File base = currDir;
      while(sFileName.startsWith("../")){
        base = currDir.getParentFile();
        sFileName = sFileName.substring(3);
      }
      sAbs = base + "/" + sFileName; 
    } else {
      sAbs = sFileName;
    }
    return cleanAbsolutePath(sAbs);
  }
  
  
  
  /**Returns the normalized absolute path from a file. See {@link #normalizePath(CharSequence)}.
   * @param file Any relative or absolute file.
   * @return The returned CharSequence is a StringBuilder which is never referenced elsewhere
   *   or it is a String.
   */
  public static CharSequence normalizePath(File file){
    return normalizePath(file.getAbsolutePath());
  }
  
  
  /**Cleans any /../ and /./ from a path, it makes it normalized or canonical.
   *
   * @param inp Any path which may contain /./ or /../, with backslash or slash-separator.
   *   If the inp is instanceof StringBuilder, it is used directly for correction and returned in any case.
   *   Elsewhere a new StringBuilder will be created if necessary.
   * @return The originally inp if inp doesn't contain /./ or /../ or backslash or inp is a Stringbuilder, 
   *   elsewhere a new StringBuilder which presents the normalized form of the path. 
   *   It does not contain backslash but slash as separator.
   *   It does not contain any "/./" or "//" or "/../". 
   *   It does contain "../" only at start if it is necessary for a relative given path.
   */
  public static CharSequence normalizePath(final CharSequence inp){
    CharSequence test = inp;
    StringBuilder uPath = inp instanceof StringBuilder ? (StringBuilder)inp : null;
    int posBackslash = StringFunctions.indexOf(inp, '\\', 0);
    if(posBackslash >=0){
      if(uPath ==null){ test = uPath = new StringBuilder(inp); }
      do {
        uPath.setCharAt(posBackslash, '/');
        posBackslash = StringFunctions.indexOf(inp, '\\', posBackslash +1);
      } while(posBackslash >=0);
    }
    int x = 6;
    
    int posNext = 0;
    int pos;
    while( (pos = StringFunctions.indexOf(test, "//", posNext)) >=0){
      if(uPath ==null){ test = uPath = new StringBuilder(inp); }
      uPath.delete(pos, pos+1);
      posNext = pos;  //search from pos, it may be found "somewhat///follow"
    }
    posNext =0;
    while( (pos = StringFunctions.indexOf(test, "/./", posNext)) >=0){
      if(uPath ==null){ test = uPath = new StringBuilder(inp); }
      uPath.delete(pos, pos+2);
      posNext = pos;  //search from pos, it may be found "somewhat/././follow"
    }
    posNext =1;
    while( (pos = StringFunctions.indexOf(test, "/../", posNext)) >=0){
      if(uPath ==null){ test = uPath = new StringBuilder(inp); }
      int posStart = uPath.lastIndexOf("/", pos-1);
      //remove "folder/../"
      uPath.delete(posStart+1, pos+4);  //delete from 0 in case of "folder/../somemore"
      posNext = posStart;  //search from posStart, it may be found "path/folder/folder/../../folder/../follow"
    }
    int posEnd = test.length();
    while( StringFunctions.endsWith(test, "/..")){
      if(uPath ==null){ test = uPath = new StringBuilder(inp); }
      int posStart = uPath.lastIndexOf("/", posEnd-4);
      if(posStart < 0){
        //it contains "folder/..", replace it by "."
        uPath.setLength(1); uPath.setCharAt(0, '.');
      } else {
        //remove "/folder/.." 
        if(posStart == 0 || posStart == 2 && uPath.charAt(1)==':'){
          posStart +=1;   //but don't remove a slash on start of absolute path.
        }
        uPath.delete(posStart, posEnd); 
        posEnd = posStart;  //it has removed on end
      }  
    }
    while( StringFunctions.endsWith(test, "/.")){
      if(uPath ==null){ test = uPath = new StringBuilder(inp); }
      int posStart = posEnd -2;
      if(posStart == 0 || posStart == 2 && uPath.charAt(1)==':'){
        posStart +=1;   //but don't remove a slash on start of absolute path.
      }
      uPath.delete(posStart, posEnd); 
      posEnd = posStart;  //it has removed on end
    }
    while( StringFunctions.startsWith(test, "./")){
      if(uPath ==null){ test = uPath = new StringBuilder(inp); }
      uPath.delete(0, 2); 
    }
    return test;
  
    /*
    int posSlash2 = StringFunctions.indexOf(inp, "//", 0);
    int posDot1 = StringFunctions.indexOf(inp, "/./", 0); 
    int posDot2 = StringFunctions.indexOf(inp, "/../", 1);
    int posDot2e = StringFunctions.endsWith(inp, "/..") ? posEnd-3 : -1; 
    //check whether any operation is necessary:
    if(posDot1>=0 || posSlash2 >=0 || posDot2 >0 
        || StringFunctions.startsWith(inp, "./") || StringFunctions.endsWith(inp, "/.") || posDot2e >0){  
      //need of handling
      if(uPath ==null){ uPath = new StringBuilder(inp); }
      do{
        int posNext = posEnd-1;        //The position for continue.
        //the first ocurrences are handled in each while step
        if(posDot1 >= 0){                                //remove "/." 
          uPath.delete(posDot1, posDot1+2); posNext = posDot1; 
          if(posSlash2 > posDot1){ posSlash2 -=2; }  //shift to left because remove
          if(posDot2 > posDot1){ posDot2 -=2; }
          if(posDot2e > posDot1){ posDot2e -=2; }
          posEnd -=2;
        }
        if(posSlash2 >= 0){                                //remove "/" 
          uPath.delete(posSlash2, posSlash2+1); 
          if(posNext > posSlash2){ posNext = posSlash2; } 
          if(posDot2 > posSlash2){ posDot2 -=1; }  //shift to left because remove
          if(posDot2e > posSlash2){ posDot2e -=1; }  //shift to left because remove
          posEnd -=1;
        }  
        if(posDot2 > 0){
          int posStart = uPath.lastIndexOf("/", posDot2-1);
          //remove "folder/../"
          uPath.delete(posStart+1, posDot2+4);  //delete from 0 in case of "folder/../somemore"
          posNext = 0;
          posEnd -= posDot2+4 - posStart+1 +1;
          if(posDot2e >= 0){ posDot2e = posNext -3; }  //shift to left because remove
        }
        if(posDot2e > 0){
          int posStart = uPath.lastIndexOf("/", posDot2-1);
          if(posStart < 0){
            //it contains "folder/..", replace it by "."
            uPath.setLength(1); uPath.setCharAt(0, '.');
          } else {
            //remove "/folder/.." 
            if(posStart == 0 || posStart == 3 && uPath.charAt(1)==':'){
              posStart +=1;   //but don't remove a slash on start of absolute path.
            }
            uPath.delete(posStart, posDot2+4); 
            if(posNext > posStart){ posNext = posStart; }
            posEnd = posStart;  //it has removed on end
          }  
        }
        posSlash2 = uPath.indexOf("//", posNext);
        posDot1 = uPath.indexOf("/./", posNext); 
        posDot2 = uPath.indexOf("/../", posNext+1);  //should have any "folder/../" before
        if(posDot1 < 0 && posEnd >=2 && uPath.charAt(posEnd-2)=='/' && uPath.charAt(posEnd-1)=='.' ){ 
          posDot1 = posEnd-2;   //endswith "/."
        }
        posDot2e = StringFunctions.endsWith(uPath, "/..") ? posEnd-3:-1;
      } while( posDot1>=0 || posSlash2 >=0 || posDot2 >=0);
      if(uPath.charAt(0)=='.' && uPath.charAt(1)=='/' ){
        uPath.delete(0, 2); //remove "./" on  start, all others "/./ are removed already
      }
      return uPath;
    }
    */ 
  }
  
  
  /**Cleans ".." and "." from an absolute path.
   * @deprecated, use {@link #normalizPath(String)}. It does the same.
   * @param inp
   * @return
   */
  public static String cleanAbsolutePath(String inp){ return normalizePath(inp).toString(); }
  
  
  /**Returns true if the file is symbolic linked. This works on Unix-like file systems.
   * For windows it returns true if the cleaned absolute path is identical with the result of
   * file.getCanonicalPath() whereby the comparison is done ignoring the lower/upper case of letters and with
   * unique slashes instead backslash. It means that this test returns true always, so that
   * this method returns false in all situations on windows.
   * <br>
   * Implementation: The cleaned absolute path is compared with the canonical path. The absolute path is cleared
   * from unnecessary ./ parts, see {@link absolutePath(String, File)}. and {@link #cleanAbsolutePath(String)}.
   * @param file The file to test.
   * @return
   */
  public static boolean isSymbolicLink(File file){
    String sAbsPath = absolutePath(file.getAbsolutePath(), null);  //converts \ to /, removes unnecessary ./
    String sCanonPath = getCanonicalPath(file);
    if(sAbsPath.equals(sCanonPath)) return false;
    else if(File.pathSeparatorChar == '\\'){
        //on windows ignore cases
        sAbsPath = sAbsPath.toLowerCase();
        sCanonPath = sCanonPath.toLowerCase(); 
        return !sAbsPath.equals(sCanonPath);
    } else return true;  //its a symbolic link. 
  }
  
  
  
  /**Checks whether a file is newer as the other, maybe delete dst.
   * returns:
   * <ul>
   * <li>-1 if src does not exists. Don't make.
   * <li>0: if src is older than dst. Don't make.
   * <li>1: if dst does not exists. Make.
   * <li>2: if src is newer, dst is existent but should not deleted. removeDstIfOlder is given as false.
   * <li>3: if src is newer, dst is deleted.
   * <li>4: if src is newer, dst should be deleted but the deletion fails. There is a problem on dst.  
   * @param src
   * @param dst
   * @param removeDstIfOlder deletes the dst also if it may be write protected if the src is newer.
   * @return -1..4
   */
  public static int checkNewless(File src, File dst, boolean removeDstIfOlder){
    if(!dst.exists()) return 1;  //src is new
    else if(!src.exists()) return -1;  //src is not found.
    else{
      long srcdate = src.lastModified();
      long dstdate = dst.lastModified();
      if(srcdate > dstdate){
        if(removeDstIfOlder){
          if(!dst.canWrite()){
            dst.setWritable(true);
          }
          if(dst.delete()){ return 3;}
          else return 4;
        }
        else return 2;
      } 
      else return 0;  //src is older.
    }
  }
  
  
  
  /**Close operation without exception.
   * Note: A close is necessary though it might be worked well without close(). The file is closed and released
   * by the operation system any time if an application is finished. It is a property of modern operation systems.
   * But if the application runs still, a file may be blocked for access from other applications 
   * if it was opened to read or write and not closed after them. It is also so in the case of
   * not referenced file handle instances. Use the following pattern to work with files:
   * <pre> 
   * Writer myWriter = null;
   * try {
   *   myWriter = new FileWriter(...);  //opens
   *   ...
   *   myWriter.write(...)
   *   myWriter.close();     //closes
   *   myWriter = null;      //mark it closed in the reference.
   * } catch(IOException exc) {
   *   ... do anything
   *   //NOTE: the myWriter may be closed or not, depending on the exception kind
   * }
   * if(myWriter !=null){ FileSystem.close(myWriter); }  //close it if it is remained opened because any exception.
   * </pre>  
   * This method helps to close without extra effort of a second exception.
   * The close() operation should be done outside and after the main exception for the file operation.
   * <br><br>
   * The following pattern is erroneous because the file may remained opened and can't be close outside
   * the routine:
   * <pre>
   * void antiPattern() throws IOException {
   *   myWriter = new FileWriter(...);  //opens
   *   ...
   *   myWriter.write(...)
   *   myWriter.close();     //closes
   * }
   * </pre>  
   * @param file any Closeable instance. If it is null, it is okay. Then no action is done.
   * @return false if file.close() throws an exception.
   */
  public static boolean close(Closeable file){
    boolean bOk;
    if(file == null) return true;
    try {
      file.close();
      bOk = true;
    } catch (IOException e) {
      bOk = false;
    }
    return bOk;
  }
  
  
  
  

  /**Adds Files with the wildcard-path to a given list.
   *
   * @param sPath path with wildcards in the filename.
   * @param listFiles given list, the list will be extended.
   * @return false if the deepst defined directory of a wildcard path does not exist.
   *   true if the search directory exists independent of the number of founded files.
   *   Note: The number of founded files can be query via the listFiles.size().
   */
  public static boolean addFileToList(String sPath, List<File> listFiles)
  //throws FileNotFoundException
  { sPath = sPath.replace('\\', '/');
    return addFileToList(null, sPath, listFiles);
  }


  /**Adds Files with the wildcard-path to a given list.
  *
  * @param sPath path with wildcards in the filename.
  * @param listFiles given list, the list will be extended.
  * @return false if the deepst defined directory of a wildcard path does not exist.
  *   true if the search directory exists independent of the number of founded files.
  *   Note: The number of founded files can be query via the listFiles.size().
  */
  public static boolean addFileToList(String sPath, AddFileToList listFiles)
  //throws FileNotFoundException
  { sPath = sPath.replace('\\', '/');
    return addFileToList(null, sPath, listFiles);
  }

  /**Add files. It calls {@link #addFileToList(File, String, AddFileToList)}
   * with the wrapped listFiles.
   * @param dir may be null, a directory as base for sPath.
   * @param sPath path may contain wildcard for path and file.
   * @param listFiles Container to get the files.
   * @return false if the dir not exists or the deepst defined directory of a wildcard path does not exist.
   *   true if the search directory exists independent of the number of founded files.
   *   Note: The number of founded files can be query via the listFiles.size().
   */
  public static boolean addFileToList(File dir, String sPath, List<File> listFiles) //throws FileNotFoundException
  {
    ListWrapper listWrapper = new ListWrapper(listFiles);
    //NOTE: the listFiles is filled via the temporary ListWrapper.
    return addFileToList(dir, sPath, listWrapper);
  }

  
  
  
  /**Builds a File which is a directory of
   * @param dirParent parent, maybe null then unused. If posFile==0 then the current directory is returned.
   * @param sPath path from dirParent or as absolute or relative path.
   * @param posFile the substring(0..posFile) of path is used.
   *   if 0 then sPath is ignored.
   * @return The File object build from the input arguments. Whether the file exists or it is a directory
   *   is not tested here.
   */
  private static File buildDir(File dirParent, String sPath, int posFile){
    final File fDir;
    String sPathDir;
    if(posFile > 0)
    {
      sPathDir = sPath.substring(0, posFile);  //with ending '/'
      if(dirParent == null){ 
        fDir = new File(sPathDir);
      } else {
        fDir = new File(dirParent, sPathDir);  //based on given dir
      }
    }
    else
    { 
      if(dirParent == null){ 
        fDir = new File(".");
      } else {
        fDir = dirParent;  //based on given dir
      }
    }
    return fDir;
  }
  
  
  
  
  /**Executes adding file to the given list.
   * First all directories are evaluated. This routine is called recursively for directories.
   * After them the files in this directory are evaluated and listed.
   * @param listFiles destination list
   * @param dir base directory or null
   * @param sPath can contain '/' but not '\'
   * @param posWildcard first position of a '*' in the path
   * @param filterName filter for the file names or null
   * @param filterAlldir filter for directories or null
   * @param recursivect counter for recursively call. If it is >1000 this routine is aborted to prevent
   *   too many recursions because any error.
   * @return
   */
  private static boolean addFileToList(AddFileToList listFiles, File dir, String sPath, int posWildcard
    , FilenameFilter filterName, FilenameFilter filterAlldir, int recursivect
    ) {
    boolean bFound = true;
    if(recursivect > 1000) throw new RuntimeException("fatal recursion error");
    int posDir = sPath.lastIndexOf('/', posWildcard) +1;  //is 0 if '/' is not found.
    File fDir = buildDir(dir, sPath, posDir);
    if(fDir.exists()) { 
      int posBehind = sPath.indexOf('/', posWildcard);
      boolean bAllTree = false;
      String sPathSub = sPath.substring(posBehind +1);  //maybe ""
      if(sPath.startsWith("xxxZBNF/"))
        Assert.stop();
      int posWildcardSub = sPathSub.indexOf('*');
      if(posBehind >=0 || filterAlldir !=null) {
        FilepathFilter filterDir;
        //WildcardFilter filterDir;
        if(posBehind >0){
          String sPathDir = sPath.substring(posDir, posBehind);  //with ending '/'
  
          filterDir = new FilepathFilter(sPath); //Dir); 
          bAllTree = sPathDir.equals("**");
          if(filterDir.bAllTree){
            filterAlldir = filterDir;
            filterDir = null;
          }
        } else {
          filterDir = null;  //NOTE: filterAlldir may be set
        }
        if(bAllTree){
          //search from sPathSub in the current dir too, because it is "/**/name..."
          bFound = addFileToList(listFiles, fDir, sPathSub, posWildcardSub, filterName,filterAlldir, recursivect +1);
        } else {
          String[] sFiles = fDir.list();
          if(sFiles !=null){  //null on error
            for(String sFile: sFiles){
              File dirSub;
              if( (  bAllTree
                  || filterDir !=null    && filterDir.accept(fDir, sFile)
                  || filterAlldir !=null && filterAlldir.accept(fDir, sFile)
                  )
                  && (dirSub = new File(fDir, sFile)).isDirectory()
                  ){
                if(sFile.equals("ZBNF"))
                  Assert.stop();
                //dirSub is matching to the filterAlldir:
                bFound = addFileToList(listFiles, dirSub, sPathSub, posWildcardSub, filterName,filterAlldir, recursivect +1);
              }
            }
          }
        }
      }
      if(posBehind <0 || bAllTree){
        File[] files = fDir.listFiles(filterName);
        if(files !=null){
          for(File file: files)
          { //if(file.isFile())
            { listFiles.add(file);
            }
          }
        }
      }
    }
    else { 
      bFound = false;
    }
    
    return bFound;
  }
  
  
  
  

  
  /**Add files
   * @param dir may be null, a directory as base for sPath.
   * @param sPath path may contain wildcard for path and file. May use backslash or slash.
   * @param listFiles Container to get the files.
   * @return false if the dir not exists or the deepst defined directory of a wildcard path does not exist.
   *   true if the search directory exists independent of the number of founded files.
   *   Note: The number of founded files can be query via the listFiles.size().
   */
  public static boolean addFileToList(File dir, String sPath, AddFileToList listFiles) 
  //throws FileNotFoundException
  { boolean bFound = true;
    sPath = sPath.replace('\\', '/');
    //final String sDir, sDirSlash;
    //if(dir != null){ sDir = dir.getAbsolutePath(); sDirSlash = sDir + "/"; }
    //else { sDir = ""; sDirSlash = ""; }
    int posWildcard = sPath.indexOf('*');
    if(posWildcard < 0)
    {
      File fFile;
      if(dir == null){ 
        fFile = new File(sPath);
      } else {
        fFile = new File(dir, sPath);  //based on given dir
      }
      bFound = fFile.exists();
      if(bFound)
      { listFiles.add(fFile);
      }
    }
    else
    { //
      int posFile = sPath.lastIndexOf('/')+1;  //>=0, 0 if a / isn't contain.
      String sName = sPath.substring(posFile); // "" if the path ends with "/"
      FilenameFilter filterName = new FilepathFilter(sName); //WildcardFilter(sName); 
      //
      bFound = addFileToList(listFiles, dir, sPath, posWildcard, filterName, null, 0);
    }
    return bFound;
  }



  
  
  
  /**Filter for a file path maybe with wildcards in the directory path.
   * Note: The {@link java.io.FilenameFilter} is better as the {@link java.io.FileFilter}
   *   because the {@link java.io.File#list(FilenameFilter)} builds a File instance only if the name is tested positively.
   *   In opposite the {@link java.io.File#list(FileFilter)} builds a File instance anytime before the test. 
   *   The difference may be marginal.But {@link java.io.File#list(FileFilter)} produces some more instances in the heap,
   *   which are unnecessary. 
   */
  private static class WildcardFilter implements FilenameFilter
  {
    private final String sBefore, sBehind, sContain;
    
    /**True if the filter path on ctor has contained "**". Then apply the filter on subdirs too. */
    private final boolean bAllTree;
    
    /**True if the filter path on ctor was "**". Then accept all directory entries. */
    private final boolean bAllEntries;

    public WildcardFilter(String sMask)
    { 
      bAllEntries = sMask.equals("**");
      if(bAllEntries){
        bAllTree = true;
        sBefore = sBehind = sContain = null;
      } else {
        int len = sMask.length();
        int pos1 = sMask.indexOf('*');
        int pos1a;
        bAllTree = pos1 <= len-2 && sMask.charAt(pos1+1) == '*';
        if(bAllTree){
          pos1a = pos1 +1;
          
        } else {
          pos1a = pos1;
        }
        int pos2 = sMask.lastIndexOf('*');
        //
        if(pos1 >0){ sBefore = sMask.substring(0, pos1); }
        else { sBefore = null; }
        //
        if(pos2 < len){ sBehind = sMask.substring(pos2+1); }  // "*behind", "before*behind", "before*contain*behind"
        else { sBehind = null; }                             // "*", "before*", "before*contain*", "*contain*"
        //
        if(pos2 > pos1a){ sContain = sMask.substring(pos1+1, pos2); }  //Note: pos2 == pos1 if only one asterisk.
        else { sContain = null; }
      }
    }


    public boolean accept(File dir, String name)
    {
      return bAllEntries
         ||(sBefore ==null  || name.startsWith(sBefore))
           && (sContain ==null || name.contains(sContain))
           && (sBehind ==null  || name.endsWith(sBehind));
    }
  }


  
  

  /**Searches the first line with given text in 1 file, returns the line or null.
   * @param file The file
   * @param what content to search, a simple text, not an regular expression.
   * @return null if nothing was found, elsewhere the line.
   * @throws IOException File not found or any file read exception.
   */
  public static String grep1line(File file, String what)
  throws IOException
  {
    String retLine = null;
    BufferedReader r1 = new BufferedReader(new FileReader(file));
    String sLine;
    boolean fileOut = false;
    while( retLine == null && (sLine = r1.readLine()) !=null){
      if(sLine.contains(what)){
        retLine = sLine;  //breaks
      }
    }
    r1.close();
    return retLine;
  }

  
  
  
  
  /**This is equal the usual grep, but with given files. TODO this method is not ready yet.
   * @param files
   * @param what
   * @return
   */
  public static String[] searchInFiles(List<File> files, String what, Appendable searchOutput)
  {
    List<String> listResult = new LinkedList<String>();
    for(File file: files){
      BufferedReader r1 = null;
      try{
        r1 = new BufferedReader(new FileReader(file));
        String sLine;
        boolean fileOut = false;
        while( (sLine = r1.readLine()) !=null){
          if(sLine.contains(what)){
            if(!fileOut){
              searchOutput.append("<file=").append(file.getPath()).append(">").append("\n");
              fileOut = true;  
            }
            searchOutput.append("  ").append(sLine).append("\n");
            //TODO fill an ArrayList, with the line number and file path. 
          }
        }
        r1.close();
      }catch(IOException exc){ 
        try{ 
          if(r1 !=null){ r1.close(); }
          searchOutput.append("<file=").append(file.getPath()).append("> - read error.\n");
        } catch(IOException exc2){}
        //listResult.add("File error; " + file.getAbsolutePath()); 
      }
    }
    try{ searchOutput.append("<done: search in files>\n");} catch(IOException exc){}
    String[] ret = new String[1];
    return ret;
  }


  /**Searches a file given with local path in this directory and in all parent directories.
   * @param start any start file or directory. If it is a file (for example a current one), its directory is used.
   * @param path May be more as one local path. Simple it is only one "filename.ext" or "anyDirectory". Possible "path/to/file.ext".
   *   More as one argument may be given, to search one of more given files.
   * @return null if nothing found. Elsewhere the found file which matches to one of path in the start or one of the parent directories
   */
  public static File searchInParent(File start, String ... path)
  { File found = null;
    
    File parent = start.isDirectory() ? start : start.getParentFile();
    do {
      File[] children = parent.listFiles(); 
      for(String path1 : path){             //check all files with the search paths.
        int sep = path1.indexOf('/');
        String name = sep >0 ? path1.substring(0, sep) : path1;
        for(File child: children) {         //check all files with this search path.
          String fname = child.getName();
          if(fname.equals(name)) {          //name found.
            if(sep >0) {                    //if path/file
              String subPath = path1;
              File childDir = child;
              while(sep >0 && childDir != null && childDir.isDirectory()) { //a new child is checked.
                subPath = subPath.substring(sep+1);
                sep = subPath.indexOf('/');
                String subName = sep > 0 ? subPath.substring(sep) : subPath;
                File[] subChildren = childDir.listFiles();
                childDir = null;  //set newly, check in next loop
                child = null;  //set on found.
                for(File subChild1: subChildren) {
                  if(subChild1.getName().equals(subName)) {
                    childDir = subChild1;
                    child = subChild1; 
                    break;
                  }
                }//for check subchildren
              }//while path/path
              found = child;  //null or the found file.
              //else not found!
            } else { //no separator
              found = child;                //no separator in path, found!
            }//sep >0
          }//fname.equals(name)
          if(found !=null) break;
        } //for
        if(found !=null) break;
      } //for
      parent = parent.getParentFile();
    } while(found == null && parent !=null);
    return found;
  }
  
  


  private static void stop()
  { //only for breakpoint
  }


  
  
  /**Test routine with examples to test {@link #normalizePath(String)}. */
  public static void test_searchFile(){
    List<File> foundFile = new LinkedList<File>();
    boolean bOk = addFileToList("D:/**/vishia/**/srcJava_vishiaBase", foundFile);
    Assert.check(bOk);
  }  
  
  
  
  /**Test routine with examples to test {@link #normalizePath(String)}. */
  public static void test_addFilesWithBasePath(){
    CharSequence result;
    File dir = new File(".");  //it is the current dir, but without absolute path.
    File dirAbs = dir.getAbsoluteFile();
    File parent = getDir(dir);   //builds the real parent. It depends on the start directory of this routine.
    File grandparent = getDir(parent);
    String sParent = parent.getPath().replace("\\", "/");  //the path
    int posSlash = sParent.lastIndexOf('/');
    String sNameParent = sParent.substring(posSlash +1);
    String searchPath = sNameParent + "/**/*";
    List<FileAndBasePath> files = new ArrayList<FileAndBasePath>();
    addFilesWithBasePath(grandparent, searchPath, files);
    searchPath = sNameParent + "/..:**/*";
    files.clear();
    addFilesWithBasePath(parent, searchPath, files);
  }  
  
  
  
  /**Test routine with examples to test {@link #normalizePath(String)}. */
  public static void test_normalizePath(){
    CharSequence result;
    result = normalizePath("../path//../file"); 
    assert(result.toString().equals( "../file"));
    result = normalizePath("../path/file/."); 
    assert(result.toString().equals( "../path/file"));
    result = normalizePath("../path/../."); 
    assert(result.toString().equals( ".."));
    result = normalizePath("../path//../file"); 
    assert(result.toString().equals( "../file"));
    result = normalizePath("..\\path\\\\..\\file"); 
    assert(result.toString().equals( "../file"));
    result = normalizePath("/../path//../file"); 
    assert(result.toString().equals( "/../file"));  //not for praxis, but correct
    result = normalizePath("./path//file/"); 
    assert(result.toString().equals( "path/file/"));    //ending "/" will not deleted.
    result = normalizePath("path/./../file"); 
    assert(result.toString().equals( "file"));       

    File dir = new File(".");  //it is the current dir, but without absolute path.
    File dirAbs = dir.getAbsoluteFile();
    File parent = dir.getParentFile();  assert(parent == null);  //property of java.io.File
    parent = getDir(dir);   //builds the real parent. It depends on the start directory of this routine.
    String sParent = parent.getPath().replace("\\", "/");  //the path
    int posSlash = sParent.lastIndexOf('/');
    String sNameParent = sParent.substring(posSlash +1);
    File fileTest = new File(sParent + "/..",sNameParent); //constructed filecontains "/../" in its path
    CharSequence sFileTest = normalizePath(fileTest.getAbsolutePath());
    CharSequence sDirAbs = normalizePath(dirAbs.getAbsolutePath());
    assert(StringFunctions.equals(sFileTest,sDirAbs));
  }
  
  
  /**Returns true if the given file exists. This method transscripts {@link java.io.File.exists()}
   * to support simple usage for jbatch.
   * @param file Any file
   * @return true if this file exists.
   */
  public static boolean exists(File file){ return file.exists(); }
  
  
  /**Returns true if this file describes a root directory.
   * A root directory is either "/" or "D:/" whereby "D" is a drive letter in Windows.
   * The file can be given as relativ path with ".."
   * @param file Any file
   * @return true if it is the root.
   */
  public static boolean isRoot(File file){ 
    CharSequence sName = absolutePath(file.getPath(), null);
    return isRoot(sName);
  }
  
  
  public static boolean isRoot(CharSequence sName){
    return sName.equals("/") || sName.length() ==3 && sName.subSequence(1,3).equals(":/");
  }
  
  
  /**The main routine contains only tests.
   * @param args
   */
  public static void main(String[] args){
    //test_normalizePath();
    //test_addFilesWithBasePath();
    test_searchFile();
  }


}
