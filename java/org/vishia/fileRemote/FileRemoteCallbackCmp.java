package org.vishia.fileRemote;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.vishia.util.Assert;
import org.vishia.util.Debugutil;
import org.vishia.util.FileSystem;
import org.vishia.util.SortedTreeWalkerCallback;
import org.vishia.util.StringFunctions;

/**This class supports comparison of files in a callback routine.
 * @author Hartmut Schorrig
 *
 */
public class FileRemoteCallbackCmp implements FileRemoteWalkerCallback
{
  
  /**Version, history and license.
   * <ul>
   * <li>2023-02-10 Hartmut new concept with the {@link FileRemoteProgressEvent}: remove <code>progress.show(...)</code>
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
  static final public String sVersion = "2023-02-10";
  
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

  
  
  
  
  
  
  private final CompareCtrl cmpCtrl = new CompareCtrl();

  private final FileRemote dir1, dir2;
  
  private final String basepath1;
  private final int zBasePath1;
  
  /**Event instance for user callback. */
  private final FileRemoteProgressEvent progress;  //FileRemote.CallbackEvent evCallback;
  
  private final FileRemoteWalkerCallback callbackUser;
  
  int mode;
  
  long minDiffTimestamp = 2000; 
  
  final static int cmp_onlyTimestamp = 1;
  final static int cmp_content = 2;
  final static int cmp_withoutLineend = 4;
  final static int cmp_withoutEndlineComment = 8;
  final static int cmp_withoutComment = 16;
  
  boolean aborted = false;
  
  /**Constructs an instance to execute a comparison of directory trees.
   * @param dir1 One directory which contains a file tree. All files are compared with dir2
   * @param dir2 The other directory to compare
   * @param callbackUser Maybe null. If given, on each directory entry, exit and file the callback will be invoked 
   *   with the handled directory or file. The second argument is an boxed Integer, which contains the bits from
   *   {@link FileMark} to inform what is with that file. 
   * @param timeOrderProgress maybe null. If given this timeOrder is used to show the progression of the comparison.
   *   The timeOrder is set with data
   */
  FileRemoteCallbackCmp(FileRemote dir1, FileRemote dir2, FileRemoteWalkerCallback callbackUser, FileRemoteProgressEvent timeOrderProgress) { //FileRemote.CallbackEvent evCallback){
    //this.evCallback = evCallback;
    this.progress = timeOrderProgress;
    this.callbackUser = callbackUser;
    this.dir1 = dir1; this.dir2 = dir2;
    basepath1 = FileSystem.normalizePath(dir1.getAbsolutePath()).toString();
    zBasePath1 = basepath1.length();
    //} catch(Exception exc){
    //  dir1 = null; //does not exists.
    //}
    cmpCtrl.ignoreToEol.add(".file");
    cmpCtrl.ignoreToEol.add("//");
    cmpCtrl.ignoreToEol.add("Compilation time:");
    cmpCtrl.ignoreToEol.add("Compiler options:");
    cmpCtrl.ignoreCommentline.add("//");
    cmpCtrl.ignoreFromTo.add(new String[]{".epcannot:", ".epcannot.end:"});
    cmpCtrl.ignoreFromTo.add(new String[]{".static1:", ".static1.end:"});
  }
  
  
  
  /**On start of comparison it refreshes the second dir tree.
   * Secondly it resets all mark bits in the known files. Don't use the java.nio.file walker,
   * Only do it for the given files. It are lesser. The other FileRemote instances are not known till now.
   * This is a fast operation because it does not access the file system. 
   */
  @Override public void start(FileRemote startDir)
  {
    if(dir1.device == null){
      dir1.device = FileRemote.getAccessorSelector().selectFileRemoteAccessor(dir1.getAbsolutePath());
    }
    if(dir2.device == null){
      dir2.device = FileRemote.getAccessorSelector().selectFileRemoteAccessor(dir2.getAbsolutePath());
    }
    dir2.refreshPropertiesAndChildren();        
    
    //try{ 
    int markReset = FileMark.markRoot | FileMark.markDir | FileMark.markDir | FileMark.cmpAlone | FileMark.cmpContentEqual
      | FileMark.cmpFileDifferences | FileMark.cmpContentNotEqual | FileMark.cmpMissingFiles;
    dir1.resetMarkedRecurs(markReset, null);
    dir2.resetMarkedRecurs(markReset, null);
    dir1.setMarked(FileMark.markRoot);            // a marker to stop going backward with dir marking.
    dir2.setMarked(FileMark.markRoot);
  }
  
  
  
  @Override public Result offerParentNode(FileRemote dir){
    //if(dir == this.dir1){ return Result.cont; } //the first entry
    //else {
    FileRemote dir2sub;
      CharSequence path = FileSystem.normalizePath(dir.getAbsolutePath());
      if(path.length() <= zBasePath1){
        dir2sub = dir2;
        //it should be file == dir1, but there is a second instance of the start directory.
        //System.err.println("FileRemoteCallbackCmp - faulty FileRemote; " + path);
        //return Result.cont;
      } else {
        //Build dir2sub with the local path from dir1:
        CharSequence localPath = path.subSequence(zBasePath1+1, path.length());
        if(StringFunctions.equals(localPath, "functionBlocks"))
          Debugutil.stop();
        //System.out.println("FileRemoteCallbackCmp - dir; " + localPath);
        dir2sub = dir2.subdir(localPath);
      }
      if(!dir2sub.exists()){
        dir.setMarked(FileMark.cmpAlone);
        dir.mark.setMarkParent(FileMark.cmpMissingFiles, false);
        System.out.println("FileRemoteCallbackCmp - offerDir, not exists; " + dir.getAbsolutePath());
        return Result.skipSubtree;  //if it is a directory, skip it.        
      } else {
        dir2sub.resetMarked(FileMark.cmpAlone);
        dir2sub.device.walkFileTree(dir2sub, true, true, 0, 0, null, 0, 1, callbackMarkSecondAlone, progress, false);
        System.out.println("FileRemoteCallbackCmp - offerDir, check; " + dir.getAbsolutePath());
        //waitfor
        //dir2sub.refreshPropertiesAndChildren(null);        
        return Result.cont;
      }
    //}
  }
  
  /**Checks whether all files are compared or whether there are alone files.
   */
  @Override public Result finishedParentNode(FileRemote file){
    
    return Result.cont;      
  }
  
  
  @Override public Result offerLeafNode(FileRemote file, Object info)
  {
    CharSequence path = FileSystem.normalizePath(file.getAbsolutePath());
    CharSequence localPath = path.subSequence(zBasePath1+1, path.length());
    //System.out.println("FileRemoteCallbackCmp - file; " + localPath);
    if(StringFunctions.compare(localPath, "functionBlocks/AngleBlocks_FB.h")==0)
      Assert.stop();
    FileRemote file2 = dir2.child(localPath);
    if(!file2.exists()){
      if(callbackUser !=null) {
        callbackUser.offerLeafNode(file, new Integer(FileMark.cmpAlone));  ////
      }
      file.setMarked(FileMark.cmpAlone);   //mark the file1, all file2 which maybe alone are marked already in callbackMarkSecondAlone.
      file.mark.setMarkParent(FileMark.cmpMissingFiles, false);
      return Result.cont;    
    } else {
      file2.resetMarked(FileMark.cmpAlone);
      int cmprBits = compareFile(file, file2);
      file.setMarked(cmprBits);
      file2.setMarked(cmprBits);
      if( (cmprBits & FileMark.cmpContentNotEqual) !=0) {
        file.mark.setMarkParent(FileMark.cmpFileDifferences, false);
        file2.mark.setMarkParent(FileMark.cmpFileDifferences, false);
      }
      if(callbackUser !=null) {
        callbackUser.offerLeafNode(file, new Integer(cmprBits));  ////
      }
      return Result.cont;
    }
  }

  
  
  @Override public boolean shouldAborted(){
    return aborted;
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
    
    mode = cmp_withoutLineend;

    if(file1.getName().equals("ReleaseNotes.topic"))
      Assert.stop();
    
    long date1 = file1.lastModified();
    long date2 = file2.lastModified();
    long len1 = file1.length();
    long len2 = file2.length();
    if(Math.abs(date1 - date2) > minDiffTimestamp && mode == cmp_onlyTimestamp){
      equal = equalDaylightSaved = contentEqual = contentEqualWithoutEndline = false;
      lenEqual = len1 == len2;
    } else if( ( Math.abs(date1 - date2 + 3600000) < minDiffTimestamp
              || Math.abs(date1 - date2 - 3600000) < minDiffTimestamp
               ) && mode == cmp_onlyTimestamp){ 
      equal = equalDaylightSaved = contentEqual = contentEqualWithoutEndline = false;
    } else if(Math.abs(date1 - date2) < minDiffTimestamp && len1 == len2){
      //Date is equal, len is equal, don't spend time for check content.
      equal = equalDaylightSaved = lenEqual = true;
    } else {
      boolean doCmpr;
      //timestamp is not tested.
      if(len1 != len2){
        //different length
        if((mode & (cmp_withoutComment | cmp_withoutEndlineComment | cmp_withoutLineend)) !=0){
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
        try{ equal = compareFileContent(file1, file2);
        } catch( IOException exc){
          readProblems = true; equal = false;
        }
      }
    }
    int ret;
    if(equal){
      ret = FileMark.cmpContentEqual;
      //file1.setMarked(FileMark.cmpContentEqual);
      //file2.setMarked(FileMark.cmpContentEqual);
    } else {
      ret = FileMark.cmpContentNotEqual;
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
      for(String sEol: cmpCtrl.ignoreToEol) {
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
      for(String[] fromTo: cmpCtrl.ignoreFromTo) {
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
    FileSystem.close(r1);
    FileSystem.close(r2);
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
        for(String sEol: cmpCtrl.ignoreCommentline) {
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
  
  
  
  
  
  
  @Override public void finished(FileRemote startDir)
  {
    if(progress !=null){
      progress.bDone = true; 
      //progress.show(FileRemote.CallbackCmd.done, null);
    }
    /*
    if(evCallback !=null && evCallback.occupyRecall(500, null, true) !=0){
      evCallback.sendEvent(FileRemote.CallbackCmd.done);
    }
    */
  }


  
  /**Callback to mark all files of the second directory as 'alone' on open directory.
   * If the files are found in the first directory after them, there are marked as 'equal' or 'non equal' then, and this selection
   * will be removed. This callback will be used in the routine {@link #offerParentNode(FileRemote)} on any directory
   * in the dir1. A new dir is searched in the dir2 tree, then the children in 1 level are marked. 
   * 
   */
  final FileRemoteWalkerCallback callbackMarkSecondAlone = new FileRemoteWalkerCallback()
  {

    @Override
    public void finished(FileRemote startDir)
    { }

    @Override
    public Result finishedParentNode(FileRemote file)
    { return Result.cont; }

    @Override
    public Result offerParentNode(FileRemote file)
    { return Result.cont; }

    @Override
    public Result offerLeafNode(FileRemote file, Object info)
    { 
      //1412 
      file.setMarked(FileMark.cmpAlone);   //yet unknown whether the 2. file exists, will be reseted if necessary.
      return Result.cont;
    }

    @Override
    public void start(FileRemote startDir)
    { }
    
    @Override public boolean shouldAborted(){
      return false;
    }

    
  };

  
  
  
  
}
