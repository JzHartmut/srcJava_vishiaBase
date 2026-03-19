package org.vishia.fileLocalAccessor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.vishia.event.EventWithDst;
import org.vishia.fileLocalAccessor.FileCallbackLocalCmp.CompareCtrl;
import org.vishia.fileRemote.FileMark;
import org.vishia.fileRemote.FileRemote;
import org.vishia.fileRemote.FileRemoteProgressEvData;
import org.vishia.fileRemote.FileRemoteReport;
import org.vishia.fileRemote.FileRemoteWalkerCallback;
import org.vishia.fileRemote.FileRemoteCmdEventData;
import org.vishia.util.Debugutil;
import org.vishia.util.FileCompare;
import org.vishia.util.FileFunctions;
import org.vishia.util.FileSystem;
import org.vishia.util.SortedTreeWalkerCallback;
import org.vishia.util.StringFunctions;
import org.vishia.util.SortedTreeWalkerCallback.Result;


public class FileCallbackLocalSearch  extends FileRemoteWalkerCallback
{
  
  /**Version, history and license.
   * <ul>
   * <li>2026-03-16 created. Comparison in callback routine of walkThroughFiles instead in the graphic thread.
   * </ul>
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
   * 
   */
  //@SuppressWarnings("hiding")
  static final public String sVersion = "2026-03-16";
  
  class CompareCtrl {
    
    /**Some Strings for start Strings to ignore comparison to end of line.
     * For example it contains "//" to ignore comments in source files.
     */
    final List<String> ignoreToEol = new LinkedList<String>();
    
    /**Some Strings which are start Strings of a whole line to ignore this line both in first and second file independent.
     * If a line starts with this String, maybe after spaces, then ignore the whole line.
     * For example it contains "//" to ignore comments in source files.
     */
    final List<String> ignoreCommentline = new LinkedList<String>();
    
    /**Entries with an array of 2 Strings, start and end of non-compare regions. */
    final List<String[]> ignoreFromTo = new LinkedList<String[]>();
    
    
  }

  
  int mMarkParent;
  
  String sSearch;
  
  
  private FileRemoteReport freport;
  
  
  private final CompareCtrl cmpCtrl = new CompareCtrl();

  /**one of the bits
   * <br> {@link FileCompare#onlyTimestamp} = {@value FileCompare#onlyTimestamp}
   * <br> {@link FileCompare#withoutLineend} = {@value FileCompare#withoutLineend}
   * <br> {@link FileCompare#withoutEndlineComment} = {@value FileCompare#withoutEndlineComment}
   * <br> {@link FileCompare#withoutComment} = {@value FileCompare#withoutComment}
   * used for the comparison itself.
   */
  protected int mode;
  

  /**Constructs an instance to execute a comparison of directory trees.
   * since 2024-02: If cmpMode has set the bit {@link FileCompare#onlyTimestamp} then full content comparison is not done. 
   * The comparison is very more faster (seen 10 times). 
   * <br>TODO: here some texts are set to ignore end of line: '.file', '//', 'compilation time:'. this should be come from an argument, not fix here.
   * <br>TODO: also some arguments are set to excluding lines from to, also necessary as arguments.:  
   * @param dir1 One directory which contains a file tree. All files are compared with dir2
   * @param dir2 The other directory to compare
   * @param cmpMode can contain the bits {@link FileCompare#onlyTimestamp}, {@link FileCompare#withoutLineend}, {@link FileCompare#withoutEndlineComment}, {@link FileCompare#withoutComment}, 
   * @param callbackUser Maybe null. If given, on each directory entry, exit and file the callback will be invoked 
   *   with the handled directory or file. The second argument is an boxed Integer, which contains the bits from
   *   {@link FileMark} to inform what is with that file. 
   * @param evBack maybe null. If given this back event is used to show the progression of the comparison.
   *   The timeOrder is set with data
   *   
   */
  public FileCallbackLocalSearch(FileRemote dir1, int cmpMode, String sSearch, FileRemoteWalkerCallback callbackUser, EventWithDst<FileRemoteProgressEvData,?> evBack) { //FileRemote.CallbackEvent evCallback){
    super(dir1, null, callbackUser, evBack);
    this.mode = cmpMode;
    this.sSearch = sSearch;
    this.cmpCtrl.ignoreToEol.add(".file");
    this.cmpCtrl.ignoreToEol.add("//");
    this.cmpCtrl.ignoreToEol.add("Compilation time:");
    this.cmpCtrl.ignoreToEol.add("Compiler options:");
    this.cmpCtrl.ignoreCommentline.add("//");
    this.cmpCtrl.ignoreFromTo.add(new String[]{".epcannot:", ".epcannot.end:"});
    this.cmpCtrl.ignoreFromTo.add(new String[]{".static1:", ".static1.end:"});
    /*.static.end:         write this to not break comparison of this file itself.
      .epcannot.end: */
  }
  
  
  void reportFileRemoteDir (File fOut, FileRemote dir) {
    if(this.freport == null) { this.freport = new FileRemoteReport(); }  // create instance only one time.
    this.freport.showTree(fOut, dir);
  }
  
  
  /**On start of comparison it refreshes the second dir tree.
   * Secondly it resets all mark bits in the known files. Don't use the java.nio.file walker,
   * Only do it for the given files. It are lesser. The other FileRemote instances are not known till now.
   * This is a fast operation because it does not access the file system. 
   */
  @Override public void start(FileRemote startDir, FileRemoteCmdEventData co)
  {
    //try{ 
    int markReset = FileMark.markRoot | FileMark.markDir | FileMark.markDir | FileMark.mCmpFile;
    this.dir1Base.resetMarkedRecurs(markReset, null);
    this.dir1Base.setMarked(FileMark.markRoot);            // a marker to stop going backward with dir marking.
  }
  
  
  
  @Override public Result offerParentNode(FileRemote dir, Object oPath, Object oWalkInfo){
    //if(dir == this.dir1Base) Debugutil.stopp();  //{ return Result.cont; } //the first entry
    //else {
    this.mMarkParent = 0;   // prepared for children search.
    super.prepareDirs(dir, false);
    //                                       //--------vv but yet not clarified whether all sub file/dir:
    return Result.cont;
  }
  
  /**Checks whether all files are compared or whether there are alone files.
   */
  @Override public Result finishedParentNode(FileRemote file, Object data, Object oWalkInfo) {
    file.setMarked(this.mMarkParent);
    int markGiven = file.getMark();
    assert(this.dir1Curr == file);
    super.restoreDirs();
    this.dir1Curr.setMarked(markGiven);
    return Result.cont;      
  }
  
  
  /**This does the comparison of the file.
   *
   */
  @Override public Result offerLeafNode(FileRemote file, Object info) {
//    CharSequence path = FileFunctions.normalizePath(file.getAbsolutePath());
//    CharSequence localPath = path.subSequence(this.zBasePath1+1, path.length());
    //if(StringFunctions.compare(localPath, "asciidoc/CppJava.css")==0) Debugutil.stopp();
    String sfName = file.getName();
 
    //if(sfName.equals("FileCallbackLocalCmp.java")) Debugutil.stopp();
    //
    //======>>>> compareFile
    int resultBits;
    Result ret;
    if(this.sSearch !=null && this.sSearch.length() >0) {
      try {
        int nLine = FileFunctions.searchTextInFile(file, this.sSearch, 0, "//", new String[] {"/*", "(*"}, new String[] {"*/", "*)"});
        if(nLine >0) {
          file.setMarked(FileMark.cmpFileDifferences);      // # mark the file1 because it is found with text check
          this.mMarkParent |= FileMark.cmpFileDifferences;
          ret = Result.contMarked;
        } else {
          file.setMarked(FileMark.cmpLenTimeEqual);         // ~ mark the file1 because it is found without text check
          this.mMarkParent |= FileMark.cmpLenTimeEqual;
          ret = Result.contUsed;
        }
        //file.setMarked(FileMark.fileReadError);   //mark the file1 because it is found with text check
      } catch (Exception e) {
        file.setMarked(FileMark.fileReadError);             // ? mark the file1 because it is found but read error
        this.mMarkParent |= FileMark.fileReadError;
        ret = Result.contReadError;
      }
    } else {
      file.setMarked(FileMark.cmpFileDifferences);          // # mark the file1 because it is found by name, without text check
      this.mMarkParent |= FileMark.cmpFileDifferences;
      ret = Result.contMarked;
    }
    if(this.callbackUser !=null) {
      //@SuppressWarnings("removal") 
    }
    return ret;
  }

  
  
  
   

}
