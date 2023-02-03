package org.vishia.fileRemote;

import java.io.File;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.Map;

import org.vishia.util.Assert;
import org.vishia.util.Debugutil;
import org.vishia.util.FileFunctions;
import org.vishia.util.FileSystem;
import org.vishia.util.IndexMultiTable;
import org.vishia.util.StringFunctions;
import org.vishia.util.StringPart;

/**This class combines some {@link FileRemote} instances for common usage.
 * It ensures that the same FileRemote object is used for the same string given path.
 * It means the properties of a file of the operation system are known only one time for one Java application.
 * Difference to the operation system's file handling: There are more properties for a file especially mark. They are able to test
 * without operation system access. But the operation system's file properties should be synchronized with the FileRemote properties
 * if necessary.
 * @author Hartmut Schorrig
 *
 */
public class FileCluster
{
  /**Version, history and license.
   * <ul>
   * <li>2013-02-03 Hartmut chg: not processed but asserted that the given directory for all calls is normalized. 
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
  public static final String version = "2023-02-03";
  
  /**This index contains the association between paths and its FileRemote instances for all known directories.
   * It are the used directories in the application, not all of the file system. 
   * The files in the directory and also sub directories are contained in {@link FileRemote#children}.
   * It is possible that a sub directory is referenced in children, but not registered here. 
   * That is if it is itself not used with the content. 
   */
  protected IndexMultiTable<String, FileRemote> idxPaths = new IndexMultiTable<String, FileRemote>(IndexMultiTable.providerString);
  
  
  public FileCluster(){
  }

  
  
  /**Gets the existing directory instance with this path from the file system or creates and registers a new one.
   * If the file is not existing on the file system it is created anyway because the file may be a new candidate. 
   */
  public FileRemote getDir( final CharSequence sPath){
   return(getFile(sPath, null));
  }

  
  /**Gets the existing file instance with this path from the file system or creates and registers a new one.
   * If the file is not existing on the file system it is created anyway because the file may be a new candidate. 
   */
  public FileRemote getFile( final CharSequence sDirP, final CharSequence sName){
    return getFile(sDirP, sName, true);
  }  
  
  
  /**Gets the existing file instance with this path from the FileCluster or creates and registers a new one.
   * <br><br>
   * If the file is not existing on the file system it is created anyway because the file may be a new candidate.
   * The path is not checked against the file system.
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
  FileRemote getFile( final CharSequence sDirP, final CharSequence sName, boolean assumeChild){
    //File file1 = new File(sDirP.toString());
    //String sDir1 = FileSystem.getCanonicalPath(file1); //problem: it accesses to the file system. not expected here. 
    assert(sDirP == FileFunctions.normalizePath(sDirP)); //sPath.replace('\\', '/'); // it should be incomming normalized!
    final CharSequence sDir1 = sDirP;
    if(!FileFunctions.isAbsolutePath(sDirP)) {
      throw new IllegalArgumentException("absolute path expected, " + sDir1);
    }
    final String sDir;
    int zDir = sDir1.length();
    if(sDir1.charAt(zDir-1) == '/' && zDir >3)
    { Debugutil.stop();
      sDir = sDir1.subSequence(0, zDir-1).toString();
      zDir -=1;
    } else { sDir = sDir1.toString(); }
    //Sets the iterator after the exact found position or between a possible position:
    FileRemote dirCheck; // = idxPaths.search(sDir.toString());
    int flagDir = sName == null ? FileRemote.mDirectory : 0;  //if name is not given, it is a directory. Elsewhere a file.
    dirCheck = idxPaths.search(sDir);
    if(dirCheck == null) { //nothing found, a path lesser then all other. for example first time if "C:/path" is searched whereby any "D:/path" are registered already.
      dirCheck = searchOrCreateDir(sDir);
      //dirCheck = new FileRemote(this, null, null, sDir, 0, 0, 0, 0, flagDir, null, true);
      //idxPaths.put(sDir, dirCheck);
    } else {
      CharSequence sDirCheck = dirCheck.getPathChars();
      int zDirCheck = sDirCheck.length();
      int cmpPathCheck = StringFunctions.comparePos(sDir, 0, sDirCheck, 0, -1);
      if(cmpPathCheck ==0){ //equal
        //it is found.
      }
      else if(  cmpPathCheck -1 == zDirCheck        //sDirCheck is a substring of sDir
             && assumeChild
             && sDir.length() >= zDirCheck && sDir.charAt(zDirCheck) == '/'){  //dirCheck is a parent directory of sDir:
        //any parent directory of the file was found. Create the child directory.
        StringPart pathchild = new StringPart(sDir + '/', zDirCheck+1, sDir.length()+1);
        //pathchild.append('/');
        dirCheck = dirCheck.subdir(pathchild);   //it calls this method recursively! It puts the directories. 
      } else { //other directory name, maybe shorter for ex. "path" vs. "path2" or "path1" vs. "path2".
        dirCheck = searchOrCreateDir(sDir);
        //dirCheck = new FileRemote(this, null, null, sDir, 0, 0, 0, 0, flagDir, null, true);
        //idxPaths.put(sDir, dirCheck);  //Note: parents of the new FileRemote are recognized.
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
        int pos1 = StringFunctions.lastIndexOf(path, '/');
        int pos2 = StringFunctions.indexOf(path, '/', 0);
        final CharSequence sGrandParent; 
        final int flags;
        if(pos1 == pos2){
          //root path
          sGrandParent = path.subSequence(0, pos1+1);  //with ending /
          flags = FileRemote.mDirectory | FileRemote.mRoot;
        } else {
          sGrandParent = path.subSequence(0, pos1);  //without ending /
          flags = FileRemote.mDirectory; // | FileRemote
        }
        parentdirCheck = idxPaths.get(sGrandParent);
        if(parentdirCheck == null) {
          parentdirCheck = new FileRemote(this, dirCheck.device, null, sGrandParent, 0, 0, 0, 0, flags, null, true);
          idxPaths.put(sGrandParent.toString(), parentdirCheck);
        }
        dirCheck.parent = parentdirCheck;
      }
      //check if the child is known in the parent:
      Map<String,FileRemote> children = parentdirCheck.children();
      if(children == null || children.get(dirCheck.getName()) ==null) {
        parentdirCheck.putNewChild(dirCheck);
      }
      dirCheck = parentdirCheck;
    } //while
    return fileRet;
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
  private FileRemote searchOrCreateDir(String sPath)
  { FileRemote ret;
    int pos9 = sPath.lastIndexOf('/');
    assert(pos9 >=0);  //because it is an absolute path.
    if(pos9 == 0 || pos9 ==2 && sPath.charAt(1)==':') {
      //the root
      //It is a root drive, because not found '/'
      ret = new FileRemote(this, null, null, sPath , 0, 0, 0, 0, FileRemote.mDirectory, null, true);
      idxPaths.put(sPath, ret);  //Note: parents of the new FileRemote are recognized.
    } 
    else {
      String sParent = sPath.substring(0, pos9);
      FileRemote parent = getFile(sParent, null, true);
      if(parent ==null) {
        parent = searchOrCreateDir(sParent);  //recursively
      }
      if(parent == null) {
        parent = new FileRemote(this, null, null, sParent, 0, 0, 0, 0, FileRemote.mDirectory, null, true);
        idxPaths.put(sParent, parent);  //Note: parents of the new FileRemote are recognized.
      }
      ret = parent.subdir(sPath.substring(pos9+1));  //creates the child if not found, or found the existing instance.
      idxPaths.put(sPath, ret);  //Note: parents of the new FileRemote are recognized.
    }
    return ret;
  }




  
  
  
  
  public ListIterator<FileRemote> listSubdirs(String startDir) {
    return idxPaths.iterator(startDir);
  }
  
  
  
  
  
  
  
  public FileRemote XXXgetFile( final CharSequence sDirP, final CharSequence sName, boolean strict){
    CharSequence sDir1 = FileSystem.normalizePath(sDirP); //sPath.replace('\\', '/');
    final String sDir;
    int zDir = sDir1.length();
    if(sDir1.charAt(zDir-1) == '/' && zDir >3)
    { Debugutil.stop();
      sDir = sDirP.subSequence(0, zDir-1).toString();
      zDir -=1;
    } else { sDir = sDir1.toString(); }
    //Sets the iterator after the exact found position or between a possible position:
    FileRemote dirCheck; // = idxPaths.search(sDir.toString());
    int flagDir = sName == null ? FileRemote.mDirectory : 0;  //if name is not given, it is a directory. Elsewhere a file.
    boolean bFound = false;
    boolean putit = false;
    ListIterator<FileRemote> iter = idxPaths.iterator(sDir);  //search a nearest directory. next is greater, previous is equal or less.
    if(iter.hasNext()){
      dirCheck = iter.next();  //start with following, then backward
      iter.previous();
    } else if(iter.hasPrevious()){
      dirCheck = iter.previous();
    } else {
      //Only on first time.
      //Register this path as first occurrence. 
      dirCheck = new FileRemote(this, null, null, sDir, 0, 0, 0, 0, flagDir, null, true);
      System.out.println("FileCluster - create FileRemote because no found previous, the first one; " + sDir);
      idxPaths.put(sDir, dirCheck);
      bFound = true;
      putit = false;
    }
    while(!bFound) {
      putit = true;
      bFound = false;
      String sPathCheck = dirCheck.getAbsolutePath();
      int zPathCheck = sPathCheck.length();
      int cmpPathCheck = StringFunctions.comparePos(sDir, 0, sPathCheck, 0, -1);
      if(cmpPathCheck ==0){ //equal
        //the same pathm found.
        putit = false; //it is the same, found.
        bFound = true;
      }
      else if(cmpPathCheck == zPathCheck){  //sPathRet is complete substring
        if(sPathCheck.length() < sDir.length()){ //any super directory found.
          if(sDir.charAt(zPathCheck) == '/'){    //is it that?
            //any directory of the file was found. Create the child directory.
            StringPart pathchild = new StringPart(sDir, zPathCheck+1, sDir.length());
            dirCheck = dirCheck.child(pathchild);
            putit = false;  //it is existed as child of any file in the cluster.
            bFound = true;
          } else { //other directory name, maybe shorter for ex. "path" vs. "path2".
            //dirRet = new FileRemote(this, null, null, sDir, 0, 0, 0, 0, flagDir, null, true);
          }
        } else {
        }
      } else if(cmpPathCheck == -zDir && sPathCheck.charAt(zDir) == '/') { //sDir is shorter and contained in checkDir
        //it means a directory is found which is a child of sDir.
        //check whether dir is a parent of sPathCheck
        FileRemote checkWhetherParent = null; //check whether the previous entry in idxPaths is the parent.
        String sCheckWhetherParent = "";
        int zCheckWhetherParent = -1;
        while( (zPathCheck = sPathCheck.lastIndexOf('/')) //builds the parent path from sPathRet
               >= zDir) {
          if(checkWhetherParent == null) {  //get new check parent if necessary only.
            checkWhetherParent = iter.hasPrevious() ? iter.previous() : null;
            if(checkWhetherParent !=null) {
              sCheckWhetherParent = checkWhetherParent.getAbsolutePath();
              zCheckWhetherParent = sCheckWhetherParent.length();
            }
          }
          sPathCheck = sPathCheck.substring(0, zPathCheck); //shorten the found pathCheck one / shorter, the parent of sPathCheck.
          if(dirCheck.parent ==null) { //parent of parent is not registered up to now:
            int posCheckParent;
            if(checkWhetherParent !=null){
              posCheckParent = StringFunctions.comparePos(sCheckWhetherParent, sPathCheck);
            } else { posCheckParent = 0; }
            FileRemote parent;
            if(-posCheckParent == zCheckWhetherParent) { //previous entry is fully contained in sPathRet, it is a parent 
              if(zCheckWhetherParent == zPathCheck) {
                //it is exactly the parent of this level:
                parent = checkWhetherParent;
                checkWhetherParent = null;  //to test for next parent level
                sCheckWhetherParent = null;
                zCheckWhetherParent = 0;
              } else {
                String localPath = sPathCheck.substring(zCheckWhetherParent, zPathCheck);
                parent = checkWhetherParent.getChild(localPath);
              }
            } else {
              parent = new FileRemote(this, dirCheck.device, null, sPathCheck, 0, 0, 0, 0, flagDir, null, true);
              idxPaths.put(sPathCheck, parent);  //Store it for later search.
              System.out.println("FileCluster - create FileRemote for parent; " + sDir);
            }
            parent.putNewChild(dirCheck);  //register the dirCheck in the parent as child.
            dirCheck = parent;
          } else { //dirCheck.parent exists, then it is registered in idxPath already.
            //it should be the existing parent:
            dirCheck = dirCheck.parent;
            putit = false;  //it is found
            if(!dirCheck.getAbsolutePath().equals(sPathCheck)) 
            { throw new IllegalStateException("FileCluster - faulty parent found."); }
            
          }
          //idxPaths.put(sPathCheck, dirCheck);  //Store it for later search.
        }
        bFound = true;
      } 
      if(!bFound) {
        //another directory
        if(//cmpPath > 0 ||               //the sDir is greater than found path, not convenient to go back 
          !iter.hasPrevious()) {   //not possible to go back
          //the new entry. found.
          dirCheck = new FileRemote(this, null, null, sDir, 0, 0, 0, 0, flagDir, null, true);
          System.out.println("FileCluster - create FileRemote because nonfound entry; " + sDir);
          bFound = true;
          putit = true;
        } else {
          dirCheck = iter.previous();
        }
      }
    }
    if(putit){ 
      //check whether next entries in the FileCluster are children of this
      //and register it as children.
      iter.next(); //start with next entry, not with its own.
      //zDir should be the position of the '/' in the child's paths.
      //if sDir is a root directory, it contains the '/' on end.
      //therefore reduce zDir, if sDir ens with '/':
      if(sDir.charAt(zDir-1) == '/'){
        zDir -=1;  
      }
      while(iter.hasNext()){
        FileRemote childNext = iter.next();
        String pathNext = childNext.getAbsolutePath();
        int zpath = pathNext.length();
        if(zDir < zpath && StringFunctions.startsWith(pathNext, sDir)){
          CharSequence sChild = pathNext.subSequence(zDir+1, zpath);
          int pos1 = 0, pos2;
          FileRemote dir2 = dirCheck; //parent of any child level
          while( (pos2 = StringFunctions.indexOf(sChild, '/', pos1))  >pos1){
            CharSequence sChild2 = sChild.subSequence(pos1, pos2);  //child levels
            //dir2 = dir2.child(sChild2, FileRemote.mDirectory, 0,0,0,0,0);  //child level assigned to parent level, maybe exising, maybe a new FileRemote.
            FileRemote child = dir2.getChild(sChild2);
            if(child == null){
              child = dir2.internalAccess().newChild(sChild2,0, 0, 0, 0 , FileRemote.mDirectory, null);
              dir2.putNewChild(child);
            }
            dir2 = child;
            pos1 = pos2 +1;
          }
          dir2.putNewChild(childNext);
        } else {
          break; //other base path
        }
      }
      //puts the found directory.
      idxPaths.put(sDir.toString(), dirCheck);
    }
    //create the named file in the directory if given.
    final FileRemote fileRet;
    if(sName !=null){
      fileRet = dirCheck.child(sName);
    } else {
      fileRet = dirCheck;
    }
    //
    //checks and registers all parent till the root.
    FileRemote parent = dirCheck;
    while(parent.parent !=null) { parent = parent.parent; } //search the parent==null
    while(!parent.isRoot()){
      CharSequence path = parent.getPathChars();
      int pos1 = StringFunctions.lastIndexOf(path, '/');
      int pos2 = StringFunctions.indexOf(path, '/', 0);
      final CharSequence sGrandParent; 
      final int flags;
      if(pos1 == pos2){
        //root path
        sGrandParent = path.subSequence(0, pos1+1);  //with ending /
        flags = FileRemote.mDirectory | FileRemote.mRoot;
      } else {
        sGrandParent = path.subSequence(0, pos1);  //without ending /
        flags = FileRemote.mDirectory; // | FileRemote
      }
      FileRemote grandParent = new FileRemote(this, parent.device, null, sGrandParent, 0, 0, 0, 0, flags, null, true);
      parent.parent = grandParent;
      idxPaths.put(sGrandParent.toString(), grandParent);
      parent = grandParent;
    }
    return fileRet;
  }


  
  /**Gets the existing file instance with this path from the file system or creates and registers a new one.
   * If the file is not existing on the file system it is created anyway because the file may be a new candidate. 
   * @param sDirP
   * @param sName
   * @return null if the file is not registered.
   */
  FileRemote XXXcheck( final CharSequence sDirP, final CharSequence sName){
    CharSequence sDir1 = FileSystem.normalizePath(sDirP); //sPath.replace('\\', '/');
    StringBuilder uPath = sDir1 instanceof StringBuilder ? (StringBuilder)sDir1: new StringBuilder(sDir1);
    if(sName !=null) {
      if(uPath.charAt(uPath.length()-1) !='/'){ uPath.append('/'); }
      uPath.append(sName); 
    }
    String sPath = uPath.toString();
    FileRemote ret = idxPaths.get(sPath);
    return ret;
  }
  
  
}
