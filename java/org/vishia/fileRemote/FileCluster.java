package org.vishia.fileRemote;

import java.io.File;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;
import java.util.TreeMap;

import org.vishia.util.Debugutil;
import org.vishia.util.FileFunctions;
import org.vishia.util.FileSystem;
import org.vishia.util.StringFunctions;
import org.vishia.util.StringFunctions_B;

/**This class ensures that the same {@link FileRemote} instances is used for the same string given path.
 * Different parts of the application can set some attributes in FileRemote, especially masks,
 * it is ensured that all usages get the same instance for the same path.   
 * It means also that the properties of a file of the operation system should be read only one time for one Java application.
 * The access to the os file system is reduced. That is usefull especially for a remote file system.
 * But of course synchronizations due to changes of the file system's content is a topic.
 * The operation system's file properties should be synchronized with the FileRemote properties if necessary.
 * <br>
 * One difference between FileRemote and the operation system's file handling is: 
 * FileRemote knows more properties for a file especially mark bits. They should be stored also unique as properties of the file.
 * That is the first reason for this solution.
 * 
 * @author Hartmut Schorrig
 *
 */
public class FileCluster
{
  /**Version, history and license.
   * <ul>
   * <li>2023-07-15 Hartmut chg: Some errors were obviously caused on non uppercase in windows and non found path from the first level
   *   if the FileRemote was also created, but only as child from the root level. fixed.
   * <li>2023-04-02 Hartmut chg: Now uses the TreeMap instead of IndexMultiTable, it's better for debugging. 
   *   But the functionality have a Iterator for a dedicated range (key start with given string) is not supported by TreeMap.
   *   To fulfill it, now the {@link ListSubdirs} iterator was built for unchanged {@link #listSubdirs(String)}.
   *   But {@link #listSubdirs(String)} is a little bit changed: It does not need to return a ListIterator, 
   *   a simple {@link Iterator} is sufficient. This simplifies the {@link ListSubdirs}.
   * <li>2023-04-02 Hartmut improved: {@link #idxPaths} contains paths in upper case for windows. There was sometimes irritations,
   *   if a directory was given as String in faulty case, was found as file, and was contained twice here.
   *   {@link #getFile(CharSequence, CharSequence, boolean)} and {@link #searchOrCreateDir(String)} 
   *   builds upper case for search key. 
   * <li>2023-04-02 Hartmut adapted: Because of {@link FileRemote#getPathChars()} now returns a '/' on end,
   *   but the paths in {@link #idxPaths} are stored without slash, some adaptions were necessary.
   * <li>2023-04-02 Hartmut cleanup XXXgetFile(...), and XXXcheck(...), that are unused since 2014-12-30  
   * <li>2023-02-03 Hartmut chg: not processed but asserted that the given directory for all calls is normalized. 
   *   This saves unnecessary calculation time, because usual the path is normalized and absolute, because it comes from the cluster itself
   *   or proper from the file system. 
   * <li>2017-09-15 Hartmut chg: on {@link #getFile(CharSequence, CharSequence, boolean)}: If the file was not found in the index,
   *   the new private {@link #searchOrCreateDir(String)} is invoked instead immediately creating a new FileRemote instance. 
   *   That routine checks whether the parent is registered and whether the path describes a known FileRemote instance as child of parent.
   *   Then that instance is used and registered. Because that routine is invoked recursively, the root will be found. 
   *   Either that is known, or the root is registered yet, especially on start. For that, all existing FileRemote instances should be re-found, 
   *   and never a new instance is created though one is exists.  
   * <li>2017-08-27 Hartmut chg: On {@link #getFile(CharSequence, CharSequence, boolean)} only {@link FileSystem#normalizePath(CharSequence)}
   *   is invoked. Not {@link FileSystem#getCanonicalPath(File)}: Reason: On this access the operation system's file system must not accessed.
   *   Only an existing {@link FileRemote} instance is searched and returned. Especially if a network directory is accessed and the
   *   network is not accessible, this invocation must not hang! It is used in the graphic thread in the org.vishia.commander.  
   * <li>2014-12-30 Hartmut chg: Now all parent directories till the root are stored in the table. Therefore the searching algorithm 
   *   is simplified. Backward search is unnecessary because the parent path is found or it is not stored. Now all as possible directory
   *   instances are stored, the tables are greater. The searching is more simple.
   * <li>2014-12-20 Hartmut bugfix: Some files were created more as one with the same path. It were files with lesser pathname 
   *   which are not found in the {@link #idxPaths}: fix: Check backward, therefore the {@link IndexMultiTable#iterator(Comparable)}
   *   is a {@link ListIterator} up to now. 
   * <li>2013-06-15 Hartmut bugfix: create a FileRemote as directory. It is the convention, that the 
   *   property whether or not a FileRemote is a directory does not depend on a {@link FileRemote#isTested()}.
   * <li>2013-05-05 Hartmut chg: get(...) now renamed to getFile(...). {@link #getFile(CharSequence, CharSequence, boolean)}
   *   checks whether a parent instance or deeper child instance is registered already and use that.
   * <li>2013-05-05 Hartmut new: {@link #check(CharSequence, CharSequence)}  
   * <li>2013-04-22 Hartmut created. The base idea was: Select files and use this selection in another part of application.
   *   It should be ensured that the same instance is used for the selection and other usage.
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
  public static final String version = "2023-07-15";
  
  /**This index contains the association between paths and its FileRemote instances for all known directories.
   * It are the used directories in the application, not all of the file system. 
   * The files in the directory and also sub directories are contained in {@link FileRemote#children}.
   * It is possible that a sub directory is referenced in children, but not registered here. 
   * That is if it is itself not used with the content. 
   */
  protected Map<String, FileRemote> idxPaths = new TreeMap<String, FileRemote>();
  
  
  public FileCluster(){
  }

  
  
  /**Gets the existing directory instance with this path from the file system or creates and registers a new one.
   * If the file is not existing on the file system it is created anyway because the file may be a new candidate. 
   */
//  public FileRemote getDir( final CharSequence sPath){
//   return(getFile(sPath, null));
//  }

  
  /**Gets the existing file instance with this path from the file system or creates and registers a new one.
   * If the file is not existing on the file system it is created anyway because the file may be a new candidate. 
   */
  FileRemote getFile( final CharSequence sDirP, final CharSequence sName){
    return getFile(sDirP, sName, true);
  }  
  
  
  /**Gets the existing file instance with this path from the FileCluster or creates and registers a new one.
   * <br><br>
   * If the file is not existing on the file system it is created anyway because the file may be a new candidate.
   * Form the java.io.File, the path is not checked against the file system.
   * <ul>
   * <li>The file is not registered in the index in this class if it is found as children of a registered directory.
   * <li>The parent of the file is created and registered too because the returned file should have a parent.
   * </ul> 
   * @param sDirP String describes a directory. This string can have backslash instead slash and a non-normalized path (with .. etc.
   *   First {@link FileFunctions#normalizePath(CharSequence)} will be called with it.
   *   It must be an absolute path without environment variable usage, asserted. The absolute path is not build here.
   * @param sName If null then a directory is returned. If given then the returned instance is decided as a file.
   * @param assumeChild true then check whether a requested directory is a child or sub child of a found parent directory.
   *   It is possible to create a child without registration here for new files which are not decided as directory firstly.
   *   <br>false then don't assume that the file is a child of a found file. That is only to break a recursion with
   *   {@link FileRemote#child(CharSequence)} because that method calls this. 
   *   It creates the FileRemote directory instance in case of not found in the {@link #idxPaths}.
   * @return A registered FileRemote instance. It is not tested yet, {@link FileRemote#refreshProperties()} may be necessary.
   * 
   * @visibility package private because used in FileRemote
   */
  FileRemote getFile( final CharSequence sDirP1, final CharSequence sName, boolean assumeChild){
    //File file1 = new File(sDirP.toString());
    //String sDir1 = FileSystem.getCanonicalPath(file1); //problem: it accesses to the file system. not expected here. 
    CharSequence sDirP2 = StringFunctions_B.replace(sDirP1, '\\', '/');
    CharSequence sDirP = FileFunctions.normalizePath(sDirP2); //sPath.replace('\\', '/'); // it should be incomming normalized!
    if(sDirP != sDirP2) {
      Debugutil.stop();
    }
    final CharSequence sDir1 = sDirP;
    if(!FileFunctions.isAbsolutePath(sDirP)) {
      throw new IllegalArgumentException("absolute path expected, " + sDir1);
    }
    boolean bWindowsPath = sDirP.length()>=2 && sDirP.charAt(1) == ':';  // windows should have always a drive letter here. regard non case sensitive
    final String sDir2;
    int zDir = sDir1.length();
    if(sDir1.charAt(zDir-1) == '/' && zDir >3)
    { Debugutil.stop();                          // '/' on end is removed here, but it is admissible
      sDir2 = sDir1.subSequence(0, zDir-1).toString();
      zDir -=1;
    } else { sDir2 = sDir1.toString(); }
    //Sets the iterator after the exact found position or between a possible position:
    final String sDirKey = bWindowsPath ? sDir2.toUpperCase() : sDir2;
    FileRemote dirCheck; // = idxPaths.search(sDir.toString());
//    int flagDir = sName == null ? FileRemote.mDirectory : 0;  //if name is not given, it is a directory. Elsewhere a file.
    dirCheck = this.idxPaths.get(sDirKey);
    if(dirCheck == null) { //nothing found, a path lesser then all other. for example first time if "C:/path" is searched whereby any "D:/path" are registered already.
      dirCheck = searchOrCreateDir(sDir2);                 // returns the parent dir, recursively down to root if necessary
      //dirCheck = new FileRemote(this, null, null, sDir, 0, 0, 0, 0, flagDir, null, true);
      //idxPathsput(sDir, dirCheck);
    } else {
      CharSequence sDirCheck = dirCheck.getPathChars();    // this ends with /
      int zDirCheck = sDirCheck.length();
      if(bWindowsPath) {
        sDirCheck = sDirCheck.toString().toUpperCase();
      }
      boolean bLenOk = ( (zDirCheck -1) == sDir2.length()); // ok if sDir has same length as sDirCheck without ending /
      int cmpPathCheck = bLenOk ? StringFunctions.comparePos(sDirKey, 0, sDirCheck, 0, zDirCheck-1) : -1;
      if(cmpPathCheck ==0){ //equal
        //it is found.
      }
      else if(  cmpPathCheck -1 == zDirCheck     //sDirCheck is a substring of sDir
             && assumeChild                      // do not found the '/' on end.
             && sDirKey.length() >= zDirCheck && sDirKey.charAt(zDirCheck-1) == '/'){  //dirCheck is a parent directory of sDir:
        //any parent directory of the file was found. Create the child directory.
        //StringPart pathchild = new StringPart(sDir + '/', zDirCheck+1, sDir.length()+1);
        String pathchild = sDir2.substring(zDirCheck);
        //pathchild.append('/');
        dirCheck = dirCheck.subdir(pathchild);   //it calls this method recursively! It puts the directories. 
      } else { //other directory name, maybe shorter for ex. "path" vs. "path2" or "path1" vs. "path2".
        dirCheck = searchOrCreateDir(sDir2);
        //dirCheck = new FileRemote(this, null, null, sDir, 0, 0, 0, 0, flagDir, null, true);
        //idxPathsput(sDir, dirCheck);  //Note: parents of the new FileRemote are recognized.
      }
    }
    //builds or stores the requested file:
    final FileRemote fileRet;
    if(sName !=null){
      fileRet = dirCheck.child(sName);
    } else {
      fileRet = dirCheck;
    }
    //checks and registers all parent till the root.
    FileRemote parentdirCheck;
    while(!dirCheck.isRoot()){
      if(dirCheck.parent !=null) { 
        parentdirCheck = dirCheck.parent; 
      } else {
        //it has no parent, but it is not a root. search the parent:
        CharSequence path = dirCheck.getPathChars();
        int zPath = path.length();
        int pos1 = StringFunctions.lastIndexOf(path, 0, zPath-1, '/');
        int pos2 = StringFunctions.indexOf(path, '/', 0);
        String sGrandParent; 
        final int flags;
        if(pos1 == pos2){
          //root path
          sGrandParent = path.subSequence(0, pos1+1).toString();  //with ending /
          flags = FileRemote.mDirectory | FileRemote.mRoot;
        } else {
          sGrandParent = path.subSequence(0, pos1).toString();  //without ending /
          flags = FileRemote.mDirectory; // | FileRemote
        }
        if(bWindowsPath) { sGrandParent = sGrandParent.toUpperCase(); }
        parentdirCheck = this.idxPaths.get(sGrandParent);
        if(parentdirCheck == null) {
          parentdirCheck = new FileRemote(this, dirCheck.device, null, sGrandParent, 0, 0, 0, 0, flags, null, true);
          idxPathsput(sGrandParent.toString(), parentdirCheck);
        }
        dirCheck.parent = parentdirCheck;
      }
      //check if the child is known in the parent:
      Map<String,FileRemote> children = parentdirCheck.children();
      String sChildKey = dirCheck.getName();
      if(bWindowsPath) { sChildKey = sChildKey.toUpperCase(); }
      if(children == null || children.get(sChildKey) ==null) {
        parentdirCheck.putNewChild(dirCheck);              // put to parent if not done yet.
      }
      dirCheck = parentdirCheck;
    } //while
    return fileRet;
  }


  
  private void idxPathsput(String key, FileRemote value) {
//    if(Character.isLowerCase(key.charAt(0))) {
//      Debugutil.stop();
//    }
    this.idxPaths.put(key, value);
  }
  


  /**Searches a directory due to the given path. 
   * If it is not found, then search the parent and creates the directory as FileRemote from the parent. 
   * This is done recursively down till the root. At least the root is created.
   * Either the directory is known, or created yet referred by the parent, 
   * at least the root is created yet, especially on start. 
   * For that, all existing FileRemote instances should be re-found, 
   * and never a new instance is created though one is exists.  
   * 
   * @param sPath a directory path
   * @return the existing or new created FileRemote instance.
   */
  private FileRemote searchOrCreateDir(String sPath){ 
    FileRemote ret;
    boolean bWindows = sPath.length()>=2 && sPath.charAt(1)==':';
    String sPathKey = bWindows ? sPath.toUpperCase(): sPath;
    int zPath = sPath.length();
    assert(!sPath.endsWith("/") || zPath==3 || zPath==1);
    int pos9 = sPath.lastIndexOf('/');
    assert(pos9 >=0);  //because it is an absolute path.
    if(pos9 == zPath-1 && (pos9 == 0 || pos9 ==2 && sPath.charAt(1)==':')) {
      
      //the root
      //It is a root drive, because not found '/'
      ret = this.idxPaths.get(sPathKey);
      if(ret == null) {
        ret = new FileRemote(this, null, null, sPath , 0, 0, 0, 0, FileRemote.mDirectory, null, true);
        this.idxPathsput(sPathKey, ret);  //Note: parents of the new FileRemote are recognized.
      }
    } 
    else {
      final String sParent;
      if(pos9 == 0 || pos9 == 2 && sPath.charAt(1)==':') {
        sParent = sPath.substring(0, pos9+1);              // the root
      } else {
        sParent = sPath.substring(0, pos9);
      }
      FileRemote parent = getFile(sParent, null, true);
      if(parent ==null) {
        parent = searchOrCreateDir(sParent);  //recursively
      }
      if(parent == null) {
        parent = new FileRemote(this, null, null, sParent, 0, 0, 0, 0, FileRemote.mDirectory, null, true);
        String sParentKey = bWindows ? sParent.toUpperCase() : sParent;
        this.idxPathsput(sParentKey, parent);  //Note: parents of the new FileRemote are recognized.
      }
      ret = parent.subdir(sPath.substring(pos9+1));  //creates the child if not found, or found the existing instance.
      this.idxPathsput(sPathKey, ret);  //Note: parents of the new FileRemote are recognized.
    }
    return ret;
  }




  
  
  
  
  public Iterator<FileRemote> listSubdirs(String startDir) {
    return new ListSubdirs(startDir); //idxPaths.iterator(startDir);
  }
  
  
  class ListSubdirs implements Iterator<FileRemote> {
  
    final String startDir;
    
    final Iterator<Map.Entry<String, FileRemote>> iterPaths;

    Map.Entry<String, FileRemote> next;
    
    boolean isNext = true;
    
    ListSubdirs(String startDirP){
      this.startDir = startDirP.toUpperCase();
      this.iterPaths = FileCluster.this.idxPaths.entrySet().iterator();
      while(this.iterPaths.hasNext()) {
        this.next = this.iterPaths.next();
        String key = this.next.getKey();
        if(key.startsWith(this.startDir)) {
          //this.iterPaths.;
          this.isNext = true;
          break;
          
        }
      }
    }
    
    @Override public boolean hasNext () {
      if(this.next !=null) { return true; }
      else {
        this.next = this.iterPaths.next();
        String key = this.next.getKey();
        if(!key.startsWith(this.startDir)) {
          this.next = null;
          return false;
        } else {
          return true;
        }
      }
    }

    @Override public FileRemote next () {
      if(hasNext()) {
        Map.Entry<String, FileRemote> ret = this.next;
        this.next = null;
        return ret.getValue();
      } else {
        return null;
      }
    }


    @Override public void remove () {
      this.iterPaths.remove();
    }

    
  };
  
  
  
  
  
  
}
