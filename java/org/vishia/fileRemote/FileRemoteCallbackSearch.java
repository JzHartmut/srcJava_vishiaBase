package org.vishia.fileRemote;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import org.vishia.fileRemote.FileRemoteCallbackCmp.CompareCtrl;
import org.vishia.util.Debugutil;
import org.vishia.util.FileSystem;
import org.vishia.util.SortedTreeWalkerCallback;
import org.vishia.util.StringFunctions;
import org.vishia.util.SortedTreeWalkerCallback.Result;

public class FileRemoteCallbackSearch implements FileRemoteWalkerCallback {
  static final public String sVersion = "2016-12-27";
  
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

  private final FileRemote dir1;
  
  private FileRemote dirCurr;
  
  private final String basepath1;
  private final int zBasePath1;
  
  /**Event instance for user callback. */
  private final FileRemoteProgressEvent timeOrderProgress;  //FileRemote.CallbackEvent evCallback;
  
  private final FileRemoteWalkerCallback callbackUser;
  
  int mode;
  
  long minDiffTimestamp = 2000; 
  
  final static int cmp_onlyTimestamp = 1;
  final static int cmp_content = 2;
  final static int cmp_withoutLineend = 4;
  final static int cmp_withoutEndlineComment = 8;
  final static int cmp_withoutComment = 16;
  
  
  final byte[] buffer = new byte[16384];

  final byte[] search;
  
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
  FileRemoteCallbackSearch(FileRemote dir1, byte[] search, FileRemoteWalkerCallback callbackUser, FileRemoteProgressEvent timeOrderProgress) { //FileRemote.CallbackEvent evCallback){
    //this.evCallback = evCallback;
    this.timeOrderProgress = timeOrderProgress;
    this.callbackUser = callbackUser;
    this.dir1 = dir1;
    this.search = search;
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
  
  
  
  @Override public void start(FileRemote startDir)
  {
    if(dir1.device == null){
      dir1.device = FileRemote.getAccessorSelector().selectFileRemoteAccessor(dir1.getAbsolutePath());
    }
    
    //try{ 
    int markReset = FileMark.markRoot | FileMark.markDir | FileMark.markDir | FileMark.cmpAlone | FileMark.cmpContentEqual
      | FileMark.cmpFileDifferences | FileMark.cmpContentNotEqual | FileMark.cmpMissingFiles;
    dir1.resetMarkedRecurs(markReset, null);
    dir1.setMarked(FileMark.markRoot);
  }
  
  
  
  @Override public Result offerParentNode(FileRemote dir){
    this.dirCurr = dir;
    return Result.cont;
  }
  
  /**Checks whether all files are compared or whether there are alone files.
   */
  @Override public Result finishedParentNode(FileRemote file){
    
    return Result.cont;      
  }
  
  
  @Override public Result offerLeafNode(FileRemote file, Object info)
  {
    InputStream inp = null;
    int posB0 = 0;
    try{
      inp = file.openInputStream(0);
      if(inp != null) {
        int bytes;
        int nFound = 1;
        while( nFound >0 && (bytes = inp.read(this.buffer, posB0, this.buffer.length - posB0)) >0){
          byte b1 = this.search[0];
          int ix =0; 
          do {
            if(this.buffer[ix] == b1 && ix + this.search.length < this.buffer.length) {  //not found on end of buffer
              boolean bFound = true;
              for(int ixb = 1; ixb < this.search.length; ++ixb) {
                if(this.buffer[ix + ixb] != this.search[ixb]) {
                  bFound = false;
                  break;
                }
              }
              if(bFound) {
                nFound -=1;
              }
            }
          } while(nFound >0 && ++ix < bytes);
        }
        if( nFound <=0) {
          file.setMarked(FileMark.cmpFileDifferences);
          file.mark.setMarkParent(FileMark.cmpFileDifferences, false);
        }
      }
    } catch(IOException exc) {
      System.err.println(exc.toString());
    } finally {
      try{
        if(inp !=null) inp.close();
      } catch(IOException exc){
        throw new RuntimeException(exc);
      }
    }

    return Result.cont;
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
  int searchInFile(FileRemote file1)
  {

    

    
    return 0;
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
    if(this.timeOrderProgress !=null){
      this.timeOrderProgress.activateDone();
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

}
