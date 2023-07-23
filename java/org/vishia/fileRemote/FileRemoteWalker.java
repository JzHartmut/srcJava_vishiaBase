package org.vishia.fileRemote;

import java.util.Iterator;
import java.util.Map;

import org.vishia.event.EventWithDst;
import org.vishia.util.ExcUtil;
import org.vishia.util.FilepathFilterM;
import org.vishia.util.SortedTreeWalkerCallback;

/**This class offers a walker similar as {@link java.nio.file.FileTreeWalker}
 * but it does only deal with the FileRemote instances.
 * The access to the file system can be done in some callback operations.
 * @author Hartmut Schorrig
 *
 */
public class FileRemoteWalker {

  /**Version, history and license.
   * Changes:
   * <ul>
   * <li>2023-07-22 improved while test. 
   * <li>2023-07-18 Hartmut created with a new class, does only work with FileRemote instances.
   * <li>201x Hartmut created as part of FileRemote. But the concept of separation of a remote file system in a device
   *   and the mapping to FileRemote instances was never consequently developed. 
   *   Usual a mix of access to the local file system first via java.lang.File and then via {@link java.nio.file.Files}
   *   was present.   
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
  public final static String sVersion = "2023-07-22";

  
  private WalkInfo walkInfo;
  
  final FileRemoteCmdEventData co;
  
  final FileRemoteWalkerCallback callback;
  
  final EventWithDst<FileRemoteProgressEvData,?> evBack;
  

  /**Walks to the tree of children with given files, <b>without</b> synchronization with the device.
   * To run this routine in an extra Thread use {@link #walkFileTreeThread(int, FileRemoteWalkerCallback)}.
   * This operation is not intent to use working with the real file system.
   * For example {@link #copyDirTreeTo(FileRemote, int, String, int, FileRemoteProgressEvData)}
   * calls internally {@link FileRemoteAccessor#walkFileTree(FileRemote, boolean, boolean, boolean, String, long, int, FileRemoteWalkerCallback)}
   * which is implemented in the device, for example using java.nio.file.Files operations.
   * This operation iterates only over the children and sub children in the FileRemote directory tree.
   * Whether the FileRemote instances are synchronized with the file device or not, should be clarified in the callback.
   * 
   * Note: it should not change the children list, because it uses an iterator.
   * @param depth at least 1 for enter in the first directory. Use 0 if all levels should enter.
   * @param callback contains the quest and operations due to the files.
   */
  public static void walkFileTree(FileRemoteCmdEventData co, EventWithDst<FileRemoteProgressEvData,?> evBack)
  {
    FileRemoteWalker thiz = new FileRemoteWalker(co, evBack);
    thiz.walkFileTree();
  }
    
  
  
  
  /**Walks to the tree of children with given files, <b>without</b> synchronization with the device.
   * This routine creates and starts a {@link WalkThread} and runs {@link #walkFileTree(int, FileRemoteWalkerCallback)} in that thread.
   * The user is informed about progress and results via the callback instance.
   * Note: The file tree should not be changed outside or inside the callback methods because the walk method uses an iterators.
   * If the children lists are changed concurrently, then the walking procedure may be aborted because an {@link ConcurrentModificationException}
   * is thrown.
   * @param depth at least 1. Use 0 to enter all levels.
   * @param callback
   */
  public static void walkFileTreeThread(FileRemoteCmdEventData co, EventWithDst<FileRemoteProgressEvData,?> evBack)
  {
    FileRemoteWalker thiz = new FileRemoteWalker(co, evBack);
    WalkThread thread1 = thiz.new WalkThread();
    thread1.start();
  }
 
  
  
  
  
  
  
  
  public FileRemoteWalker(FileRemoteCmdEventData co, EventWithDst<FileRemoteProgressEvData, ?> evBack) {
    super();
    this.co = co;
    this.callback = co.callback();
    this.evBack = evBack;
  }


  
  protected void walkFileTree()
  {
    FilepathFilterM filter = this.co.selectFilter() == null ? null : FilepathFilterM.createWildcardFilter(this.co.selectFilter());
    this.walkInfo = new WalkInfo(this.co.filesrc(), null, filter);
    if(this.callback!=null) { this.callback.start(this.co.filesrc(), this.co); }
    this.walkSubTree(this.co.filesrc(), this.co.depthWalk() <=0 ? Integer.MAX_VALUE: this.co.depthWalk());
    if(this.callback!=null) { this.callback.finished(this.co.filesrc()); }
  }
    



  /**See {@link #walkFileTree(int, FileRemoteWalkerCallback)}, invoked internally recursively.
   */
  private FileRemoteWalkerCallback.Result walkSubTree(FileRemote dir, int depth)
  {
    
    FileRemoteWalkerCallback.Result result = preVisitDirectory(dir);
    //
    if(result == FileRemoteWalkerCallback.Result.cont){ //only walk through subdir if cont
      Map<String, FileRemote> children = dir.children();
      if( children !=null) {
        Iterator<Map.Entry<String, FileRemote>> iter = children.entrySet().iterator();
        while(result == FileRemoteWalkerCallback.Result.cont && iter.hasNext()) {
          try{
            Map.Entry<String, FileRemote> file1 = iter.next();
            FileRemote file2 = file1.getValue();
            if(file2.isDirectory()){
  //            cnt.nrofParents +=1;
              if(depth >1){
                //invokes offerDir for file2
                result = walkSubTree(file2, depth-1);
              } else {
                //because the depth is reached, offerFile is called.
                result = visitFile(file2);  //show it as file instead walk through tree
              }
            } else {
  //            cnt.nrofLeafss +=1;
              result = visitFile(file2);  //a regular file.
            }
          }catch(Exception exc) { 
            System.err.println(ExcUtil.exceptionInfo("FileRemote unexpected - walkSubtree", exc, 0, 20, true)); 
          }
        }
      }
      result = postVisitDirectory(dir);
    } else if(result == FileRemoteWalkerCallback.Result.skipSubtree) { //this was related to the own tree
      result = FileRemoteWalkerCallback.Result.cont;       // continue in the parent tree
    }
    return result;  //maybe terminate
  }


  
  /**pre visit a new found directory before enter all children
   * Note: this is similar {@link FileAccessorLocalJava7.WalkFileTreeVisitor#preVisitDirectory(Path, java.nio.file.attribute.BasicFileAttributes)}
   *   but improved. TODO improve also in Accessor
   * @param walkInfo
   * @param co
   * @return
   */
  private FileRemoteWalkerCallback.Result preVisitDirectory(FileRemote dir) {
    FileRemoteWalkerCallback.Result result = FileRemoteWalkerCallback.Result.cont;
    boolean selected;
    final FilepathFilterM childFilter;
    if(this.walkInfo.parent ==null) {                           // the first level of preVisistDirectory, the given one
      selected = true;                                     // is always selected (elsewhere the operation will no t be called)
      childFilter = this.walkInfo.fileFilter;                   // the fileFilter is effective from the next level
    } else { 
      int selectMask = this.co.selectMask();
      if((selectMask & FileMark.ignoreSymbolicLinks) !=0 &&  (dir.flags & FileRemote.mSymLinkedPath) !=0) {
        selected = false;                                  // skip a directory which is a symbolic link if desired
        childFilter = null;
      } else if(this.walkInfo.fileFilter == null) {             // do not skip if no fileFilter given, because files may be marked
        selected = (selectMask & FileMark.orWithSelectString) ==0;  //the selectMask bits should be valid anyway.
        result = SortedTreeWalkerCallback.Result.cont;
        childFilter = null;
      } else {                                             // evaluate fileFilter, skip if no file is selected.
        String name = dir.getName();
        childFilter = this.walkInfo.fileFilter.check(name, true); 
        selected = (childFilter != null); 
      }
      if((selectMask & FileMark.mSelectMarkBits) !=0) { //- evaluate selectMark bits with the given dir
        int mark = dir.getMark();
        boolean bMarkSelect = (mark & FileMark.mSelectMarkBits & selectMask) !=0;
        if( (selectMask & FileMark.orWithSelectString) !=0) {
          selected |= bMarkSelect;                         // modify selected with or bits
        } else {
          selected &= bMarkSelect;                         // exclusive selected with AND bits
        }
      }                                          
    }
    if( !selected ) {       
      result = FileRemoteWalkerCallback.Result.skipSubtree;     // ====>> return skipSubtree, if not selected and no more to do 
    }
    else {  //=============================================== not skipSubtree
      if(this.walkInfo.parent !=null && this.co.markSet() !=0) {                // only reset a mark here, set only if files are marked.
        if( (this.co.markSet() & FileMark.resetMark) !=0) {
          dir.resetMarked(this.co.markSet());
        } else {
          //NO??: dir.setMarked(this.co.markSet);
        }
      }
      WalkInfo walkInfoChild = new WalkInfo(dir, this.walkInfo, childFilter);

      if(this.walkInfo.parent !=null && this.callback !=null) {
        result = this.callback.offerParentNode(dir, null, walkInfoChild);  // not for the entry level
      } else {
        result = SortedTreeWalkerCallback.Result.cont;
      }
      if(result == SortedTreeWalkerCallback.Result.cont){
        this.walkInfo = walkInfoChild;                  // only store this currInfo if a new level is given, removed on postVisistDirectory
      }
    }
    return result;
  }
  

  
  private FileRemoteWalkerCallback.Result postVisitDirectory(FileRemote dir) {
    FileRemoteWalkerCallback.Result result = FileRemoteWalkerCallback.Result.cont;
    if(this.walkInfo.nrofFilesSelected >0 && this.co.markSetDir() !=0 && (this.co.markSetDir() & FileMark.resetMark) ==0) {
      FileMark mark = this.walkInfo.dir.getCreateMark();
      mark.nrofBytesSelected = this.walkInfo.nrBytesInDirSelected;
      mark.nrofFilesSelected = this.walkInfo.nrofFilesSelected;
      mark.setMarked(this.co.markSetDir(), null);
    }
    if(this.walkInfo.parent !=null && this.walkInfo.parent.parent !=null //do not callback not for the exit level (first level directory) 
        && this.callback !=null
      ) { 
      result = this.callback.finishedParentNode(this.walkInfo.dir, dir, this.walkInfo);
    } else {
      result = SortedTreeWalkerCallback.Result.cont;
    }
    if(this.walkInfo.parent !=null) {
      this.walkInfo.parent.nrBytesInDirSelected += this.walkInfo.nrBytesInDirSelected;
      this.walkInfo.parent.nrofFilesSelected += this.walkInfo.nrofFilesSelected;
    }
    this.walkInfo = this.walkInfo.parent;   
    return result;
  }

  
  
  
  private FileRemoteWalkerCallback.Result visitFile(FileRemote file) {
    boolean selected;
    boolean bDirectory = (file.flags & FileRemote.mDirectory)!=0;
    if(this.evBack !=null) {
      FileRemoteProgressEvData progress = this.evBack.data();
      if(bDirectory) {
        progress.nrDirVisited +=1;
      } else {
        progress.nrFilesVisited +=1;
      }
    }
    //
    int selectMask = this.co.selectMask();
    if(this.walkInfo.fileFilter == null) {             // do not skip if no fileFilter given, because files may be marked
      selected = (selectMask & FileMark.orWithSelectString) ==0;  //the selectMask bits should be valid anyway.
    } else {                                             // evaluate fileFilter, skip if no file is selected.
      String name = file.getName();
      selected = this.walkInfo.fileFilter.check(name, bDirectory) !=null; 
    }
    if((this.co.selectMask() & FileMark.mSelectMarkBits) !=0) { //- evaluate selectMark bits with the given dir
      boolean bMarkSelect = (file.getMark() & FileMark.mSelectMarkBits & this.co.selectMask()) !=0;
      if( (selectMask & FileMark.orWithSelectString) !=0) {
        selected |= bMarkSelect;                         // modify selected with or bits
      } else {
        selected &= bMarkSelect;                         // exclusive selected with AND bits
      }
    }                                          
    final FileRemoteWalkerCallback.Result result;
    if( selected ) {   //==================================== is selected, set or reset some mark bits.
      if( (this.co.markSet() & FileMark.resetMark) !=0) {
        file.resetMarked(this.co.markSet());
      } else {
        file.setMarked(this.co.markSet());
      }
      this.walkInfo.nrofFilesSelected +=1;
      this.walkInfo.nrBytesInDirSelected += file.length;
      if(this.callback !=null) {
        result = this.callback.offerLeafNode(file, null);  // callback can modify cont, can abort the sub tree
      } else {
        result = SortedTreeWalkerCallback.Result.cont;     // cont if callback is not given
      }
    } else {
      result = SortedTreeWalkerCallback.Result.cont;       // cont if the file is not selected
    }
    return result;
  }
  
  
  
  /**Data used for walk. It is chained from a first parent to deepness of dir tree for each level.
   * This data are created while {@link FileRemote#walkFileTree(int, FileRemoteCmdEventData, FileRemoteWalkerCallback)} 
   * or also  {@link FileAccessorLocalJava7#walkFileTree(FileRemote, FileFilter, int, FileRemoteWalkerCallback)} runs.
   */
  public static class WalkInfo{
    /**The directory of the level. */
    public FileRemote dir;
    
    /**This mark should be set to the directory on postVisitDirectory, */
    //int markSetDirCurrTree;
    
    
    /**This is the sum of all files length independent of mask seen on refresh.
     * It will be stored in {@link FileRemote#length()} for the {@link #dir}.*/
    public long nrBytesInDir;
    
    /**This is the sum of all files length which are selected.
     * It will be stored in {@link FileRemote#mark()} for the {@link #dir}, there in {@link FileMark#nrofBytesSelected}. */
    public long nrBytesInDirSelected;
    
    public int nrofFilesSelected;
    
    /**Current level of the file path filter. */
    public final FilepathFilterM fileFilter;
    
    public int levelProcessMarked;
    
    /**parallel structure of all children.
     * The child entries are gotten from the dir via {@link FileCluster#getFile(CharSequence, CharSequence, boolean)}. It means, existing children
     * are gotten from the existing {@link FileRemote} instances. They are written in this map while walking through the directory.
     * After walking, in {@link WalkFileTreeVisitor#postVisitDirectory(Path, IOException)}, the {@link #dir}.{@link FileRemote#children()}
     * are replaced by this instance because it contains only existing children. {@link FileRemote} instances for non existing children are removed then.
     */
    //Map<String,FileRemote> children;
    /**The parent. null on first parent. */
    public final WalkInfo parent;
    
    public WalkInfo(FileRemote dir, WalkInfo parent, FilepathFilterM fileFilter){
      this.dir = dir; this.parent = parent;
      this.levelProcessMarked = (parent == null) ? 0: parent.levelProcessMarked -1;
      this.fileFilter = fileFilter;
    }
  }

  
  
  /**Class to build a Thread instance to execute {@link FileRemote#walkFileTree(int, FileRemoteWalkerCallback)} in an extra thread.
   * This instance is created while calling {@link FileRemote#walkFileTreeThread(int, FileRemoteWalkerCallback)}.
   * The thread should be started from outside via {@link #start()}. The thread is destroyed on finishing the walking.
   * 
   * The user is informed about the progress via the callback interface, see ctor. 
   */
  class WalkThread extends Thread 
  { 
    /**Constructs with given file and callback.
     * @param startdir
     * @param depth
     * @param callback
     */
    WalkThread() {
      super("walkFileTreeThread");
    }
    
    
    @Override public void run(){
      try{
        walkFileTree();
      } catch( Exception exc){
        CharSequence msg = ExcUtil.exceptionInfo("unexpected ", exc, 0, 10);
        System.err.println(msg);
      }
    }
  }


  
}
