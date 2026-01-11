package org.vishia.fileLocalAccessor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.vishia.event.EventWithDst;
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

/**This class supports comparison of files in a tree. It is a callback class for the File Walker.
 * It works with {@link FileRemote} instances. The result of comparison is set in any file in {@link FileRemote#setMarked(int)}
 * whereby the argument is an int as mask with one of the bits in 
 * <ul><li>{@link FileMark#cmpAlone} if the second file is not existing.
 * <li>{@link FileMark#cmpContentEqual} if both files are compared by content and have equal content, whereby comments etc. are maybe ignored.
 * <li>{@link FileMark#cmpContentNotEqual} if both files are compared with different content. 
 * <li>{@link FileMark#cmpLenTimeEqual} if both files have the same length and time stamp. They can be differ though! 
 *   But the difference is not detected if {@link #mode} have the bit {@link FileCompare#onlyTimestamp} is set.
 * <li>{@link FileMark#cmpFileDifferences} if the length is differ.
 * <li>{@link FileMark#cmpTimeGreater} or {@link FileMark#cmpTimeLesser} if there are time differences. 
 *   Note that it is possible that the files have also set {@link FileMark#cmpContentEqual} if only the timestamp is touched.
 * </ul>
 * The directory till the root is marked with
 * <ul><li>{@link FileMark#selectSomeInDir} if any differences are in the dir and sub directories
 * <li>{@link FileMark#cmpMissingFiles} if some files are missing in  the dir and sub directories
 * </ul>
 * That information are displayed by the {@link org.vishia.gral.widget.GralFileSelector#setCmpFileResult(int)} see there. 
 * @author Hartmut Schorrig
 *
 */
public class FileCallbackLocalCmp extends FileRemoteWalkerCallback
{
  
  /**Version, history and license.
   * <ul>
   * <li>2024-04-08 {@link #offerParentNode(FileRemote, Object, Object)} do not skip but enter empty directories,
   *   because the files intern should be also marked with {@link FileMark#cmpAlone}.
   * <li>2024-02-12 Comparison of file trees now also in mode fast, without content:
   *   <ul><li>The cmp_onlyTimestamp etc. are removed here, instead used {@link FileCompare#onlyTimestamp} etc. They are the same values. Unique!
   *   <li>{@link #FileCallbackLocalCmp(FileRemote, FileRemote, int, FileRemoteWalkerCallback, EventWithDst)} has a new argument cmpMode
   *   </ul>
   * <li>2023-07-14 Hartmut adapted because cleanup of FileRemote 
   * <li>2023-02-10 Hartmut new concept with the {@link FileRemoteProgressEvData}: remove <code>progress.show(...)</code>
   *   because it is called in the timer thread instead. Independent of continue the process here. 
   * <li>2016-12-20 Hartmut bugfix: {@link #readIgnoreComment(BufferedReader)}: The second line after //line is ignored too. In a rarely case
   *   it was the only one line which was different, and the comparison has failed. 
   * <li>2014-12-12 Hartmut bugfix: {@link #compareFileContent(FileRemote, FileRemote)}: if the 2. file is longer, it is a difference!  
   * <li>2014-12-12 Hartmut new: {@link CompareCtrl}: Comparison with suppressed parts especially comments. 
   * <li>2013-09-19 created. Comparison in callback routine of walkThroughFiles instead in the graphic thread.
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
  static final public String sVersion = "2024-02-12";
  
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

  
  @SuppressWarnings("removal") // the cache function for this value since Java9 may force a too long cache. Use the old approach" 
  static Integer objCmpAlone = new Integer(FileMark.cmpAlone);
  
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
  public FileCallbackLocalCmp(FileRemote dir1, FileRemote dir2, int cmpMode, FileRemoteWalkerCallback callbackUser, EventWithDst<FileRemoteProgressEvData,?> evBack) { //FileRemote.CallbackEvent evCallback){
    super(dir1, dir2, callbackUser, evBack);
    this.mode = cmpMode;

    //this.evCallback = evCallback;
    //this.evWalker2 = new FileRemoteWalkerEvent("", dir2.device(), null, null, 0);
    //} catch(Exception exc){
    //  dir1 = null; //does not exists.
    //}
    this.cmpCtrl.ignoreToEol.add(".file");
    this.cmpCtrl.ignoreToEol.add("//");
    this.cmpCtrl.ignoreToEol.add("Compilation time:");
    this.cmpCtrl.ignoreToEol.add("Compiler options:");
    this.cmpCtrl.ignoreCommentline.add("//");
    this.cmpCtrl.ignoreFromTo.add(new String[]{".epcannot:", ".epcannot.end:"});
    this.cmpCtrl.ignoreFromTo.add(new String[]{".static1:", ".static1.end:"});
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
    this.dir2Base.resetMarkedRecurs(markReset, null);
    this.dir1Base.setMarked(FileMark.markRoot);            // a marker to stop going backward with dir marking.
    this.dir2Base.setMarked(FileMark.markRoot);
  }
  
  
  
  @Override public Result offerParentNode(FileRemote dir, Object oPath, Object oWalkInfo){
    //if(dir == this.dir1Base) Debugutil.stopp();  //{ return Result.cont; } //the first entry
    //else {
    super.prepareDirs(dir, false);
    if(this.dir2Curr == null || !this.dir2Curr.exists()){  // correct fix for nullpointer situation
      dir.setMarked(FileMark.cmpAlone);
      dir.mark().setMarkParent(FileMark.cmpMissingFiles, false);
      //System.out.println("FileRemoteCallbackCmp - offerDir, not exists; " + dir.getAbsolutePath());
      return Result.skipSubtree;  //if it is a directory, but do not skip it, enter to detect all files intern are cmpAlone. Mark it!        
    } else {
      //-------------------------------------------------vv second directory to compare found, refresh it first.
      this.dir2Curr.refreshPropertiesAndChildren(true, null);    // refresh the second side, in this thread. first side is refreshed in the calling WalkFileTreeVisitor      
      //reportFileRemoteDir(new File("/tmp/RAMd/FcmdCmp_" + dir2sub.getName() + ".txt"), dir2sub);  // DEBUG only
      //                                       //--------vv but yet not clarified whether all sub file/dir:
      FileMark mark2 = this.dir2Curr.mark();
      if( mark2==null || (mark2.getMark() & FileMark.cmpAlone) ==0) {  //mark only with cmpAlone if not marked already with.
        // mark all files and sub dir with cmpAlone because elsewhere they are not marked with cmpAlone because never found. 
        this.dir2Curr.walkLocal(null, FileMark.cmpAlone, FileMark.cmpAlone, null, 0, 0, null, null, 0, null);
        // an all found and used for comparision children, cmpAlone is reseted.
      }
      this.dir2Curr.resetMarked(FileMark.cmpAlone);            // hence set cmpAlone for all sub file/dir, but reset for this.
      //System.out.println("FileRemoteCallbackCmp - offerDir, check; " + dir.getAbsolutePath());
      //waitfor
      //dir2sub.refreshPropertiesAndChildren(null);        
      return Result.cont;
    }
    //}
  }
  
  /**Checks whether all files are compared or whether there are alone files.
   */
  @Override public Result finishedParentNode(FileRemote file, Object data, Object oWalkInfo) {
    super.restoreDirs();
    return Result.cont;      
  }
  
  
  /**This does the comparison of the file.
   *
   */
  @Override public Result offerLeafNode(FileRemote file, Object info) {
//    CharSequence path = FileFunctions.normalizePath(file.getAbsolutePath());
//    CharSequence localPath = path.subSequence(this.zBasePath1+1, path.length());
    //if(StringFunctions.compare(localPath, "asciidoc/CppJava.css")==0) Debugutil.stopp();
    FileRemote file2 = super.getFile2(file, false);
    if(file2 == null ) { //|| !file2.exists()) {           // if it is removed in the time between refresh and yet, it is not exists()
      if(this.callbackUser !=null) {
        this.callbackUser.offerLeafNode(file, objCmpAlone);  ////
      }
      file.setMarked(FileMark.cmpAlone);   //mark the file1, all file2 which maybe alone are marked already in callbackMarkSecondAlone.
      file.mark().setMarkParent(FileMark.cmpMissingFiles, false);
      return Result.cont;    
    } else {
      // There is a minimal reason that the file does no more exists, 
      // if it is deleted just in the millisecond between refreshe in offerParentNode() and now.
      // then if fast comparison is done, this is not detected. 
      // it is detected by compareFile because the file is not able to open. 
      // Then because of IOException it is marked as not equal.
      //DO NOT: assert(file2.exists());  // do not assert, may be faulty, see above
      //
      file2.resetMarked(FileMark.cmpAlone);
      //
      //======>>>> compareFile
      int cmprBits = compareFile(file, file2);
      //if( (cmprBits & FileMark.cmpContentNotEqual) !=0) Debugutil.stopp(); 
      file.setMarked(cmprBits);                            // set the result of compare.
      if((cmprBits & FileMark.cmpTimeGreater)!=0) {
        cmprBits &= ~FileMark.cmpTimeGreater;
        cmprBits |= FileMark.cmpTimeLesser;                // revert time greater/lesser for the other file.
      } else if((cmprBits & FileMark.cmpTimeLesser)!=0) {
        cmprBits &= ~FileMark.cmpTimeLesser;
        cmprBits |= FileMark.cmpTimeGreater;               // revert time greater/lesser for the other file.
      }
      file2.setMarked(cmprBits);                           // set the result of compare on second. 
      if( (cmprBits & (FileMark.cmpContentEqual | FileMark.cmpLenTimeEqual)) ==0) {
        file.mark().setMarkParent(FileMark.cmpFileDifferences, false);
        file2.mark().setMarkParent(FileMark.cmpFileDifferences, false);
        if(this.progress !=null) { this.progress.nrofFilesMarked +=1; }
      }
      if(this.callbackUser !=null) {
        //@SuppressWarnings("removal") 
        Integer objCmprBits = new Integer(cmprBits);
        this.callbackUser.offerLeafNode(file, objCmprBits);  ////
      }
      return Result.cont;
    }
  }

  
  
  
  /**Compare two files.
   * @param file1
   * @param file2 both file should be exist. It is tested before.
   * @return either {@link FileMark#cmpContentEqual} or {@value FileMark#cmpContentNotEqual}
   * @since 2015-09-05 returns comparison result, does not set bits in the file mask. Done on calling level.
   */
  int compareFile(FileRemote file1, FileRemote file2)
  {

    
    @SuppressWarnings("unused")
    boolean equal, lenEqual;
    @SuppressWarnings("unused")
    boolean equalDaylightSaved = false;
    @SuppressWarnings("unused")
    boolean contentEqual;
    @SuppressWarnings("unused")
    boolean contentEqualWithoutEndline;
    @SuppressWarnings("unused")
    boolean readProblems;
    

    //if(file1.getName().equals("ReleaseNotes.topic")) Debugutil.stopp();
    int ret = 0;
    
    long date1 = file1.lastModified();
    long date2 = file2.lastModified();
//  only debug, to see which dateTime:
//    final DateFormat formatDateInfo = new SimpleDateFormat("yyyy-MM-dd 'at' HH:mm:ss");
//    String sDate1 = formatDateInfo.format(new Date(date1));
//    String sDate2 = formatDateInfo.format(new Date(date2));
//    long dateDiff = date1 - date2;
    
    long len1 = file1.length();
    long len2 = file2.length();
    if(date1 > (date2 + this.minDiffTimestamp)) {
      ret |= FileMark.cmpTimeGreater;
    } else if(date1 < (date2 - this.minDiffTimestamp)) {
      ret |= FileMark.cmpTimeLesser;
    } else if(len1 == len2){
      ret |= FileMark.cmpLenTimeEqual;
    } else {
      ret |= FileMark.cmpFileDifferences;
    }
//    if(Math.abs(date1 - date2) > minDiffTimestamp && mode == FileCompare.onlyTimestamp){
//      equal = equalDaylightSaved = contentEqual = contentEqualWithoutEndline = false;
//      lenEqual = len1 == len2;
//    } else if( ( Math.abs(date1 - date2 + 3600000) < minDiffTimestamp
//              || Math.abs(date1 - date2 - 3600000) < minDiffTimestamp
//               ) && mode == FileCompare.onlyTimestamp){ 
//      equal = equalDaylightSaved = contentEqual = contentEqualWithoutEndline = false;
//    } else if(Math.abs(date1 - date2) < minDiffTimestamp && len1 == len2){
//      //Date is equal, len is equal, don't spend time for check content.
//      equal = equalDaylightSaved = lenEqual = true;
//    } else {
    if( (this.mode & FileCompare.onlyTimestamp) ==0) {
      boolean doCmpr;
      //timestamp is not tested.
      if(len1 != len2){
        //different length
        if((this.mode & (FileCompare.withoutComment | FileCompare.withoutEndlineComment | FileCompare.withoutLineend)) !=0){
          //comparison is necessary because it may be equal without that features:
          doCmpr = true;
          equal = false;  //compare it, set only because warning.
        } else {
          equal = contentEqual = contentEqualWithoutEndline = lenEqual = false;
          doCmpr = false;
        }
      } else {
        doCmpr = true;
        equal = false;  //compare it, set only because warning.
        //Files are different in timestamp or timestamp is insufficient for comparison:
      }
      if(doCmpr){
        try{ //======>>>> compare
          equal = compareFileContent(file1, file2);
          if(equal){
            ret |= FileMark.cmpContentEqual;
            //file1.setMarked(FileMark.cmpContentEqual);
            //file2.setMarked(FileMark.cmpContentEqual);
          } else {
            ret |= FileMark.cmpContentNotEqual;
          }
        } catch( IOException exc){
          readProblems = true; equal = false;
        }
      }
    }
    if( (ret & FileMark.cmpContentNotEqual) !=0) {
      //file1.setMarked(FileMark.cmpContentEqual);
      //file2.setMarked(FileMark.cmpContentEqual);
    } else {
//      ret |= FileMark.cmpContentNotEqual;
      /*
      file1.setMarked(FileMark.cmpContentNotEqual);
      file2.setMarked(FileMark.cmpContentNotEqual);
      file1.mark.setMarkParent(FileMark.cmpFileDifferences, false);
      file2.mark.setMarkParent(FileMark.cmpFileDifferences, false);
      */
    }
    return ret;
  }
  
  
  
  
  /**Compare two files.
   * @param file
   * @throws FileNotFoundException 
   */
  boolean compareFileContent(FileRemote file1, FileRemote file2) 
  throws IOException
  {
    boolean bEqu = true;
    BufferedReader r1 =null, r2 = null;
    r1 = new BufferedReader(new FileReader(file1));
    r2 = new BufferedReader(new FileReader(file2));
    String s1, s2;
    while( bEqu && (s1 = readIgnoreComment(r1)) !=null) {  //read lines of file 1 maybe with ignored comment.
      s2 = readIgnoreComment(r2);                          //read the line of the file2
      //check if an eol ignore String is contained:
      for(String sEol: this.cmpCtrl.ignoreToEol) {
        int z1 = s1.indexOf(sEol);
        if( z1 >=0){
          s1 = s1.substring(0, z1);    //shorten s1 to eol text
          int z2 = s2.indexOf(sEol);
          if(z2 >=0 && z2 == z1){
            s2 = s2.substring(0, z2);  //shorten s2 to eol text
          } //else: non't shorten, it is possible that s2 ends exactly without the sEol text. Than it is accepted.
          break; //break the for
        }
      }
      //check if an ignore String is contained, not after the eol!
      for(String[] fromTo: this.cmpCtrl.ignoreFromTo) {
        int z1 = s1.indexOf(fromTo[0]);
        if(z1 >=0){
          //from-marker was found:
          s1 = s1.substring(0, z1);
          //read the file lines till end was found:
          String s3;
          while( (s3 = readIgnoreComment(r1)) !=null){
            int z3 = s3.indexOf(fromTo[1]);
            if(z3 >=0){
              s1 += s3.substring(z3 + fromTo[1].length());  //rest after to-string, maybe length=0
              break;  //break while readLine()
            }
          }
          int z2 = s2.indexOf(fromTo[0]);  //check second line whether the marker is contained too
          if(z2 >=0){
            s2 = s2.substring(0, z2);
            //read the file lines till end was found:
            String s4;
            while( (s4 = readIgnoreComment(r2)) !=null){
              int z4 = s4.indexOf(fromTo[1]);
              if(z4 >=0){
                s2 += s4.substring(z4 + fromTo[1].length());  //rest after to-string, maybe length=0
                break;  //break while readLine()
              }
            }    
          } //else: accept that the s2 does not contain anything of this text part.
          //s1, s2 contains the start of line till from-String and the end of line after the to-String, compare it.
          //If the end marker is not found the rest to end of file is ignored.
          break; //break the for
        }
      }
      if(s2 ==null || !s1.equals(s2)){
        //check trimmed etc.
        bEqu = false;
      }
    }
    //file1 is finished.
    if( readIgnoreComment(r2) !=null) {
      //the file2 contains still some lines, it is longer:
      bEqu = false;
    }
    r1.close();
    r2.close();
    r1 = r2 = null;
    FileFunctions.close(r1);
    FileFunctions.close(r2);
    return bEqu;
  }  
  

  
  private String readIgnoreComment(BufferedReader reader) 
  throws IOException
  { boolean cont;
    String line;
    do {
      cont = false;
      line = reader.readLine();
      if(line != null){
        for(String sEol: this.cmpCtrl.ignoreCommentline) {
          if(line.startsWith(sEol)){
            //ignore it, read next.
            //faulty: line = reader.readLine();
            cont = true;  //read the next line in loop.
            break; //break the for
          }
        }
      }
    } while(cont);
    return line;
  }
  
  
  
  
  
  
  
  
  
  
}
