package org.vishia.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;



/**This class contains a functionality to compare file also in sub directories.
 * The result of comparison is presented in a tree.
 * @author Hartmut Schorrig
 *
 */
public class FileCompare
{
  
  /**Version and history
   * <ul>
   * <li>2012-02-04 Hartmut new: {@link Result#parent}, {@link Result#setToEqual()}
   *   used if after comparison the files are copied (The.file.Commander)
   * </ul>
   * 
   */
  public final static int version = 0x20120204;
  
  final static int onlyTimestamp = 1;
  final static int content = 2;
  final static int withoutLineend = 4;
  final static int withoutEndlineComment = 8;
  final static int withoutComment = 16;
  
  
  final int mode;
  
  long minDiffTimestamp = 2000; 
  
  final String[] sIgnores;
  
  
  public FileCompare(int mode, String[] ignores, long minDiffTimestamp)
  {
    this.mode = mode;
    this.sIgnores = ignores;
    this.minDiffTimestamp = minDiffTimestamp;
  }



  /**Class contains the comparison result for two files or sub directories in both trees.
   *
   */
  public static class Result
  {
    /**The left and the right file. If one of this is null, only the file at one side exists.
     */
    public final File file1, file2;
    
    public final String name;
    
    public final List<Result> subFiles;
    public final Result parent;
    public boolean alone;
    public boolean equal;
    /**For directories: There are some files missing in this dir or deeper dirs. */
    public boolean missingFiles;
    public boolean lenEqual;
    public boolean readProblems;
    
    public boolean equalDaylightSaved;
    public boolean contentEqual;
    public boolean contentEqualWithoutEndline;
    
    public Result(Result parent, File file1, File file2 )
    { this.parent = parent;
      this.file1 = file1;
      this.file2 = file2;
      this.name = file1 !=null ? file1.getName(): file2.getName();
      if(file1 !=null && file2 !=null && file1.isDirectory()){
        subFiles = new LinkedList<Result>();
      } else {
        subFiles = null;
      }
      //default values will be changed during comparison.
      alone = false; readProblems = false;
      equal = true; lenEqual = true; equalDaylightSaved = true; contentEqual = true;
      missingFiles = false;
      contentEqualWithoutEndline = true;
    }
  
    /**Sets the comparison information to 'equal at all'. This routine can be called
     * if a file is copied after comparison and therefore equal. */
    public void setToEqual(){
      alone = false;
      missingFiles = false;
      readProblems = false;
      lenEqual = true;
      contentEqual = true;
      contentEqualWithoutEndline = true;
      equal = true;
      if(parent !=null){
        adjustParent(parent, 0);
      }
    }
    
    
    void adjustParent(Result parent1, int recursion){
      if(recursion >100) throw new IllegalArgumentException("recursion problem");
      parent1.contentEqual = true;
      parent1.contentEqualWithoutEndline = true;
      parent1.equal = true;
      parent1.missingFiles = false;
      parent1.equalDaylightSaved = true;
      parent1.readProblems = false;
      //check whether all files have some problems:
      for(Result resEntry: parent1.subFiles){
        if(!resEntry.contentEqual){ parent1.contentEqual = false; } 
        if(!resEntry.contentEqualWithoutEndline){ parent1.contentEqualWithoutEndline = false; } 
        if(!resEntry.equal){ parent1.equal = false; } 
        if(resEntry.missingFiles || resEntry.alone){ parent1.missingFiles = true; } 
        if(!resEntry.equalDaylightSaved){ parent1.equalDaylightSaved = false; } 
        if(resEntry.readProblems){ parent1.readProblems = true; } 
      }
      if(parent1.parent != null){
        adjustParent(parent1.parent, recursion +1);
      }
    }
    
    
    @Override public String toString(){ return name; }
    
  }
  
  
  
  /**Compares two directory trees. This method will be called recursively for all sub directories
   * which are found on both sides.
   * Symbolic linked directories (Linux) will be excluded from comparison.
   * @param list1 List for result for dir1
   * @param list2 list for result for dir2
   * @param dir1 A directory
   * @param dir2 The second directory
   * @param sExclude Exclude filter for files (TODO)
   */
  public void compare(Result result, String[] sExclude, int recursion)
  {
    if(recursion > 100) throw new IllegalArgumentException("deepness");
    int zIgnores = sIgnores !=null ? sIgnores.length : 0;
    File[] files1 = result.file1.listFiles();
    File[] files2 = result.file2.listFiles();
    //fill all files sorted by name in the index, to get it by name:
    Map<String, File> idxFiles1 = new TreeMap<String, File>();
    Map<String, File> idxFiles2 = new TreeMap<String, File>();
    //
    //sort all files per name, exclude some, 
    if(files1 !=null) for(File file: files1){ 
      String name = file.getName();
      boolean isSymbolicLink = FileSystem.isSymbolicLink(file);
      if(!isSymbolicLink){
        int ixIgnore = -1;
        //don't fill in ignored files.
        while(++ixIgnore < zIgnores ){
          if(name.equals(sIgnores[ixIgnore])){ 
            ixIgnore = Integer.MAX_VALUE -1;  //its a break;
          }
        }
        if(ixIgnore == zIgnores){ //all ignores checked and nothing found  
          String name4cmp = file.isDirectory() ? ":" + name : name;
          idxFiles1.put(name4cmp, file);
        }
      }
    }
    if(files2 !=null) for(File file: files2){ 
      boolean isSymbolicLink = FileSystem.isSymbolicLink(file);
      if(!isSymbolicLink){
        String name = file.getName();
        int ixIgnore = -1;
        while(++ixIgnore < zIgnores ){
          if(name.equals(sIgnores[ixIgnore])){ 
            ixIgnore = Integer.MAX_VALUE -1;  //its a break;
          }
        }
        if(ixIgnore == zIgnores){ //all ignores checked  
          String name4cmp = file.isDirectory() ? ":" + name : name;
          idxFiles2.put(name4cmp, file);
        }
      }
    }
    //
    //iterate over sorted files
    Set<Map.Entry<String, File>> setFiles1 = idxFiles1.entrySet();
    Set<Map.Entry<String, File>> setFiles2 = idxFiles2.entrySet();
    Iterator<Map.Entry<String, File>> iter1 = setFiles1.iterator();
    Map.Entry<String, File> entry1 = null;
    Iterator<Map.Entry<String, File>> iter2 = setFiles2.iterator();
    Map.Entry<String, File> entry2 = null;
    String name1 = null;
    String name2 = null;
    File file1 = null, file2 = null;
    boolean bCont = true;
    //for(Map.Entry<String, File> entry1: setFiles1){
    do {
      if(entry1 == null) {  //get next entry  
        entry1 = iter1.hasNext() ? iter1.next() : null;
        if(entry1 !=null)
        { name1 = entry1.getKey();
          file1 = entry1.getValue();
        } else{ 
          name1 = null; file1 = null; 
        }
      }
      if(entry2 == null) {  //get next entry  
        entry2 = iter2.hasNext() ? iter2.next() : null;
        if(entry2 !=null)
        { name2 = entry2.getKey();
          file2 = entry2.getValue();
        } else{ 
          name2 = null; file2 = null; 
        }
      }
      if(entry1 != null && entry2 != null && name1.equals(name2)){
        final Result resEntry;
        resEntry = new Result(result, file1, file2);
        if(name1.startsWith(":")){
            //a directory
          compare(resEntry, sExclude, recursion +1);
        } else {
          //the same file names, compare it:
          compareFile(resEntry);
        }
        result.subFiles.add(resEntry);
        if(!resEntry.contentEqual){ result.contentEqual = false; } 
        if(!resEntry.contentEqualWithoutEndline){ result.contentEqualWithoutEndline = false; } 
        if(!resEntry.equal){ result.equal = false; } 
        if(resEntry.missingFiles || resEntry.alone){ result.missingFiles = true; } 
        if(!resEntry.equalDaylightSaved){ result.equalDaylightSaved = false; } 
        if(resEntry.readProblems){ result.readProblems = true; } 
        entry1 = entry2 = null;    //use next
      } else if( entry2 != null && (entry1 == null || name1.compareTo(name2) >0)){
        //file2 has no presentation at left because name2 is less than name1
        Result resEntry = new Result(result, null, file2);
        resEntry.alone = true;
        //result.equal = false;
        result.subFiles.add(resEntry);
        entry2 = null;  //use next
      } else if( entry1 != null){
        //file1 has no presentation at right because the name
        Result resEntry = new Result(result, file1, null);
        resEntry.alone = true;
        //result.equal = false;
        result.subFiles.add(resEntry);
        entry1 = null;
      } else {
        bCont = false;
      }
    } while(bCont);
  }
  
  
  
  
  /**Compare two files.
   * @param file
   */
  void compareFile(Result file)
  {
    long date1 = file.file1.lastModified();
    long date2 = file.file2.lastModified();
    long len1 = file.file1.length();
    long len2 = file.file1.length();
    if(Math.abs(date1 - date2) > minDiffTimestamp && mode == onlyTimestamp){
      file.equal = file.equalDaylightSaved = file.contentEqual = file.contentEqualWithoutEndline = false;
      file.lenEqual = len1 == len2;
    } else if( ( Math.abs(date1 - date2 + 3600000) < minDiffTimestamp
              || Math.abs(date1 - date2 - 3600000) < minDiffTimestamp
               ) && mode == onlyTimestamp){ 
      file.equalDaylightSaved = file.contentEqual = file.contentEqualWithoutEndline = false;
    } else {
      //timestamp is not tested.
      if(len1 != len2){
        //different length
        file.equal = file.contentEqual = file.contentEqualWithoutEndline = file.lenEqual = false;
      }
      //Files are different in timestamp or timestamp is insufficient for comparison:
      if(file.file1.getName().equals("MainCmd.java"))
        file.alone = false;
      file.equal = compareFileContent(file);
    }
  }
  /**Compare two files.
   * @param file
   */
  boolean compareFileContent(Result result)
  {
    boolean bEqu = true;
    try {
      BufferedReader r1 = new BufferedReader(new FileReader(result.file1));
      BufferedReader r2 = new BufferedReader(new FileReader(result.file2));
      String s1, s2;
      while( bEqu && (s1 = r1.readLine()) !=null){
        s2 = r2.readLine();
        if(s2 ==null || !s1.equals(s2)){
          //check trimmed etc.
          bEqu = false;
        }
      }
    } catch( IOException exc){
      result.readProblems = true; bEqu = false;
    }
    result.equal = bEqu;
    return bEqu;
  }  
  
  
  void reportResult(PrintStream out, List<Result> list)
  {
    boolean bWriteDir = false;
    for(Result entry: list){
      if(entry.equal){
        out.append("    ====     ; ").append(entry.name).append("\n");
        
      } else if(entry.contentEqual){
        out.append("     ==      ; ").append(entry.name).append("\n");
        
      } else if(entry.lenEqual){
        out.append("    =?=      ; ").append(entry.name).append("\n");
           
      }
        else if(entry.alone && entry.file1 !=null){
        out.append("left         ; ").append(entry.name).append("\n");
      } else if(entry.alone && entry.file2 !=null){
        out.append("       right ; ").append(entry.name).append("\n");
      } else { 
        if(!entry.alone && entry.subFiles !=null){
          reportResult(out, entry.subFiles);
        } else {
          if(!bWriteDir){ bWriteDir = writeDir(out, entry); }
          out.append("     ??      ; ").append(entry.name).append("\n");
        }
      }
    }
    
  }
  
  
  
  boolean writeDir(PrintStream out, Result entry)
  {
    out.append("=========================================").append("\n");
    out.append(entry.file1.getAbsolutePath()).append("  ==  ").append(entry.file2.getAbsolutePath()).append("\n");
    return true;
  }
  
  
  public static void main(String[] args)
  {
    File dir1 = new File(args[0]);
    File dir2 = new File(args[1]);
    String[] ignores = new String[]{".bzr"};
    FileCompare main = new FileCompare(FileCompare.onlyTimestamp, ignores, 2000);
    Result result = new Result(null, dir1, dir2);
    main.compare(result, null, 0);
    main.reportResult(System.out, result.subFiles);
  }
  
}
