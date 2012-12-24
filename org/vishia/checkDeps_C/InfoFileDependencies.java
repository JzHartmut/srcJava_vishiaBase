package org.vishia.checkDeps_C;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.vishia.mainCmd.Report;
import org.vishia.util.FileSystem;


/**Information about any file, source or header, with its dependencies
 * to included files.
 */
public class InfoFileDependencies implements AddDependency_InfoFileDependencies
{
  /**Version, history and license.
   * <ul>
   * <li>2012-12-25 Hartmut new: Inserted in the Zbnf component because it is an integral part of the Zmake concept
   *   for C-compilation.
   * <li>2011-05-00 Hartmut created: It was necessary for C-compilation to check real dependencies in a fast way.
   *   The dependency check from a GNU compiler was to slow.
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
  public static final int version = 20121225;

  
  /**Format to produce a human readable timestamp of file. */
  private static DateFormat formatTimestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
  
  /**The length of this string determines the maximal processed deepness of nested files. 
   * The length is 2 * deepness.
   */
  public static String spaces = "                                                                                                ";
  
  final Report console;
  
  /**File path like written in Include line between "path" or < path>. */
  final String sFilePathIncludeline;
  
  /**The existing or non-existing include file. */
  final File fileSrc;
  
  final String sAbsolutePath;
  
  final File fileMirror;
  
  /**The timestamp of the file (lastModified). */
  final long dateFileSrc;
  
  private long dateFileMirror;
  
  long timestampNewestDependingFiles_;
  
  /**State of the file:
   * <ul>
   * <li>' ' if it is unchanged, and all included files are unchanged.
   * <li>'^' if the timestamp is changed, but the content in comparision with mirror file is unchanged.
   * <li>'&' if the file itself is unchanged but an included file is changed. Should recompiled!
   * <li>'$' if the file itself is unchanged in content but the timestamp is changed, 
   *         and an included file is changed. Should recompiled!
   * <li>'!' if the file is detected as newly against a comparison with a mirror file
   *         or against the timestamp in comparison with the dep-file.
   * <li>'?' if the file is not found.        
   * </ul>
   */
  private char cNewly = ' ';
  
  /**True if new timestamps are found for the source and/or mirror file in comparison with dep-file-content. */
  //private boolean bChangedTimestamp;
  
  /**True if it is a file from the source pool. */
  private final boolean isSrc;
  
  /**Timestamp and name of the file for .dep file and for some reports.
   * This info is prepared if requested. */
  private String sDateNameLine;
  
  /**All included files in this file read from the dependency input. */
  final Map<String, InfoFileDependencies> includedPrimaryDeps = new TreeMap<String, InfoFileDependencies>();

  /**All included files in this included file. */
  final Map<String, InfoFileDependencies> includedPrimaryFiles = new TreeMap<String, InfoFileDependencies>();

  /**All included files in this included file and all other included files. */
  final Map<String, InfoFileDependencies> includedAllFiles = new TreeMap<String, InfoFileDependencies>();

  /**All files which includes this file directly. */
  final Map<String, InfoFileDependencies> includingPrimaryFiles = new TreeMap<String, InfoFileDependencies>();

  /**All files which includes this file directly or indirectly. */
  final Map<String, InfoFileDependencies> includingAllFiles = new TreeMap<String, InfoFileDependencies>();

  
  public InfoFileDependencies(String sFilePathIncludeline, File fileSrc, File fileMirror
    , boolean isSrc, Report console) 
  {
    this.console = console;
    this.sFilePathIncludeline = sFilePathIncludeline;
    this.fileSrc = fileSrc;
    this.fileMirror = fileMirror;
    this.sAbsolutePath = fileSrc.exists() ? FileSystem.getCanonicalPath(fileSrc): null;
    this.isSrc = isSrc;
    this.dateFileSrc = fileSrc ==null ? 0 : fileSrc.lastModified();
    this.timestampNewestDependingFiles_ = this.dateFileSrc;
  }

  public InfoFileDependencies(String sFileAbs, Report console) 
  { this.console = console;
    this.sFilePathIncludeline = sFileAbs;
    this.fileSrc = null;
    this.fileMirror = null;
    this.sAbsolutePath = sFileAbs;
    this.isSrc = false;
    this.dateFileSrc = 0;
    this.timestampNewestDependingFiles_ = 0;
  }


  
  
  /**Recursive call of adding the dependency. If the child is changed ({@link #isNewly()},
   * this will be notified as newly too. If the child is changed, the parent is changed too 
   * because it includes the child.
   * @param child It will be added to this.{@link #includingAllFiles}.
   */
  void addAllParentDependency(InfoFileDependencies child, ObjectFileDeps objDeps)
  {
    child.includingAllFiles.put(sAbsolutePath, this);
    includedAllFiles.put(child.sAbsolutePath, child);      //this file includes the child.
    for(Map.Entry<String,InfoFileDependencies> allEntry: child.includedAllFiles.entrySet()){
      //notify all parents in all child-children
      InfoFileDependencies childchild = allEntry.getValue();
      childchild.includingAllFiles.put(sAbsolutePath, this);
      this.includedAllFiles.put(childchild.sAbsolutePath, childchild);
    }
    if(child.isNewlyOrIncludedNewly()){
      notifyIncludedNewly(objDeps);      //if the child is changed, the parent is changed too because it includes the child.
    }
    for(Map.Entry<String,InfoFileDependencies> parentEntry: includingPrimaryFiles.entrySet()){
      InfoFileDependencies parent = parentEntry.getValue();
      parent.addAllParentDependency(child, objDeps);
    }
  }
  
  /**Adds a depending file.
   * @see org.vishia.checkDeps_C.AddDependency_InfoFileDependencies#addDependency(InfoFileDependencies, ObjectFileDeps)
   */
  public void addDependency(InfoFileDependencies child, ObjectFileDeps objDeps){
    includedPrimaryFiles.put(child.sAbsolutePath, child);  //this file includes the child.
    child.includingPrimaryFiles.put(sAbsolutePath, this);  //the child knows that this uses it.
    if(child.isNewlyOrIncludedNewly()){
      notifyIncludedNewly(objDeps);
    }
    addAllParentDependency(child, objDeps);
  }

  /**The fileMirror is copied from fileSrc, because the fileSrc is changed. */
  public void notifyNewly(ObjectFileDeps objDeps)
  { cNewly = '!'; 
    dateFileMirror = fileMirror !=null ? fileMirror.lastModified(): 0; 
    sDateNameLine = null;  //build it new if necessary
    if(objDeps !=null){
      objDeps.notifyNewer(this, console);
    }
  }
  
  /**The fileMirror is copied from fileSrc, because the fileSrc is changed. */
  public void notifyIncludedNewly(ObjectFileDeps objDeps)
  { 
    switch(cNewly){
    case '!': break;
    case '&': break;
    case ' ': cNewly = '&'; break;
    case '^': cNewly = '$'; break;
    case '?': break;
    case '$': break;
    default: assert(false);
    }
    if(objDeps !=null){
      objDeps.notifyNewer(this, console);
    }
    dateFileMirror = fileMirror !=null ? fileMirror.lastModified(): 0; 
    sDateNameLine = null;  //build it new if necessary
  }
  
  public void notifyChangedTimestamp()
  { 
    switch(cNewly){
    case '!': break;
    case '&': cNewly = '$'; break;
    case ' ': cNewly = '^'; break;
    case '^': break;
    case '?': break;
    case '$': break;
    default: assert(false);
    }
    //if("!&".indexOf(cNewly) >=0)
    //bChangedTimestamp = true; 
    dateFileMirror = fileMirror !=null ? fileMirror.lastModified(): 0; 
    sDateNameLine = null;  //build it new if necessary
  }
  
  /**Returns true if the file is changed in comparison with a last used file for translation. 
   * @return false if the newness is not detected. It is possible that the file is newer however,
   *         check timestamp! See {@link #getDateFile()}.
   */
  public boolean isNewlyItself(){ return cNewly == '!'; }
  
  /**Returns true if the file is changed in comparison with a last used file for translation. 
   * @return false if the newness is not detected. It is possible that the file is newer however,
   *         check timestamp! See {@link #getDateFile()}.
   */
  public boolean isNewlyOrIncludedNewly(){ return "!&$".indexOf(cNewly)>=0; }
  
  
  /**Returns true if the file is changed in comparison with a last used file for translation
   * or if the source file has non-relevant changes but it is stored with other timestamp. 
   */
  public boolean shouldWriteNewly(){ return cNewly != ' '; }
  
  /**Returns true if the file is in the source pool.
   * 
   * @return false if it isn't in the source pool.
   */
  public boolean isSrcFile(){ return isSrc; }
  
  
  
  long getDateFile(){ 
    if(dateFileMirror != 0){
      return dateFileMirror;
    } else {
      //assert(fileMirror == null);
      return dateFileSrc;
    }
  }
  
  
  public String getDataNameLine()
  { if(sDateNameLine ==null){
      long timestampSrc = fileSrc.lastModified();
      final String sTimestampSrc = formatTimestamp.format(new Date(timestampSrc));
      if(fileMirror !=null && fileMirror.exists()){
        final long timestampMirror = fileMirror.lastModified();
        final String sTimestampMirror = formatTimestamp.format(new Date(timestampMirror));
        //final String sNewly = bNewly ? "!!" : "=";
        sDateNameLine = sTimestampMirror + "; " + timestampMirror + "; " + FileSystem.getCanonicalPath(fileMirror)
         + "; " + cNewly + "; " + FileSystem.getCanonicalPath(fileSrc) + "; " + timestampSrc + "; "+ sTimestampSrc;
      } else {
        sDateNameLine = sTimestampSrc + "; " + timestampSrc + "; " + sAbsolutePath;
      }
    }
    return sDateNameLine;
  }
  
  
  /**Write dependencies from the given level to a file, including all included files.
   * @param fileDep The file will be created. The directory of file will be created if not exists.
   * TODO use for new alldep-file
   */
  public void xxx_writeDependenciesOfFile(File fileDep, Report console){
    console.reportln(Report.debug, "write file.dep; " + fileDep.getAbsolutePath());
    Map<String, String> indexWrittenDeps = new TreeMap<String, String>();
    try{
      FileSystem.mkDirPath(fileDep);
      //FileWriter fWriter = 
      Writer writer = new FileWriter(fileDep);
      //if(includedFiles !=null){
        writeDependendingFiles(writer, indexWrittenDeps, console, 0);
      //}
      writer.close();
    } catch(IOException exc){
      console.writeError("writeDependencies:", exc);
    }
  }

  
  
  /**Recursively called method to write the dependencies. 
   * @param writer The writer
   * @param indexWrittenDeps Index of processed header-files, any header is written only one time.
   * @param index The tree of dependencies of this level.
   * @param nRecursion counter, will be checked and used as indent
   * @throws IOException file writing.
   */
  public void writeDependendingFiles(Appendable writer
      , Map<String, String> indexWrittenDeps
      , Report console
      , int nRecursion
      ) throws IOException
  {
    //writes the dependency line.
    if(nRecursion >= spaces.length()/2){
      writer.append("- ; to many includes\n");
    } else {
      final String sDateName = getDataNameLine();
      String sRecursion = Integer.toString(nRecursion);
      writer.append(sRecursion).append("; ").append(spaces.substring(0, 2* nRecursion)).append(sDateName).append("\n");
      console.reportln(Report.debug, "  depending; " + sDateName);
      if(includedPrimaryFiles !=null) for(Map.Entry<String, InfoFileDependencies> entry: includedPrimaryFiles.entrySet()) { //index.entrySet()){
        InfoFileDependencies infoFileIncl = entry.getValue();
        //
        //for any entry of the list of direct depending files:
        //
        String name = entry.getKey();
        if(indexWrittenDeps.get(name) == null){ //write and evaluate any header one time only!
          infoFileIncl.writeDependendingFiles(writer, indexWrittenDeps, console, nRecursion +1);
          //
          //store the name to prevent double writing of the same dependencies. The index is taken over to all levels of deepness.
          indexWrittenDeps.put(name, name);
        } else {
          String sDateNameIncl = infoFileIncl.getDataNameLine();
          writer.append("+").append(Integer.toString(nRecursion+1)).append(";").append(spaces.substring(0, 2* (nRecursion+1))).append(sDateNameIncl).append("\n");  //already included file above
        }
      }
    }
  }
    
  /**Writes a list with all files which are known with the files, which includes it (backward dependencies).
   * 
   */
  static void writeAllBackDeps(String sDepFileName, Map<String,InfoFileDependencies> indexAllDeps)
  { try{
      Writer writer = null;
      if(sDepFileName !=null){
        File fileDep = new File(sDepFileName);
        try{
          FileSystem.mkDirPath(fileDep);
          writer = new FileWriter(fileDep);
        } catch(IOException exc){
          throw new IllegalArgumentException("CheckDeps_C - argument error; cannot create -depAll=" + sDepFileName);
        }
      }
      writer.append("#This file contains all dependencies. It will be used as input for dependency check.");
      writer.append("\n#This file will be replaced after the dependency check with the new situation.");
      writer.append("\n#Special designation chars:");
      writer.append("\n# ^ Changed timestamp, but no content change outside comments in comparison with the mirror file.");
      writer.append("\n# & Recompile the file and all using files because an included file is changed.");
      writer.append("\n# $ Recompile like &, this file has a changed timestamp, but no content change outside comments too.");
      writer.append("\n# ! This file is changed, force recompile of all including files.");
      writer.append("\n");
      writer.append("\n#Include designation:");
      writer.append("\n# - Direct included.");
      writer.append("\n# + Indirect included in another included file.");
      writer.append("\n# * This file includes the named file directly. It depends on it.");
      writer.append("\n# % This file includes the named file indirectly. It depends on it.");
      writer.append("\n");
      //
      for(Map.Entry<String,InfoFileDependencies> infoEntry: indexAllDeps.entrySet()){
        //for all parents of this:
        InfoFileDependencies info = infoEntry.getValue();
        writer.append("\n");
        //writer.append("\nFile: " + info.sAbsolutePath + ": " );
        writer.append("\nFile: " + info.getDataNameLine());
        writer.append("\n  .includes: ");
        for(Map.Entry<String,InfoFileDependencies> includedEntry: info.includedPrimaryFiles.entrySet()){
          InfoFileDependencies including = includedEntry.getValue();
          writer.append("\n ").append(including.cNewly);
          writer.append("- ").append(including.sAbsolutePath);
        }
        for(Map.Entry<String,InfoFileDependencies> includedEntry: info.includedAllFiles.entrySet()){
          InfoFileDependencies including = includedEntry.getValue();
          if(info.includedPrimaryFiles.get(including.sAbsolutePath) ==null){ //only if not included primary:
            writer.append("\n ").append(including.cNewly);
            writer.append("+ ").append(including.sAbsolutePath);
         }
        }
        writer.append("\n  .is included in: ");
        for(Map.Entry<String,InfoFileDependencies> includingEntry: info.includingPrimaryFiles.entrySet()){
          InfoFileDependencies including = includingEntry.getValue();
          writer.append("\n  * " + including.sAbsolutePath);
        }
        for(Map.Entry<String,InfoFileDependencies> includingEntry: info.includingAllFiles.entrySet()){
          InfoFileDependencies including = includingEntry.getValue();
          if(info.includingPrimaryFiles.get(including.sAbsolutePath) ==null){ //only if not included primary:
            writer.append("\n  % " + including.sAbsolutePath);
          }
        }
      }
      writer.append("\n");
    }catch(IOException exc) {
      throw new RuntimeException(exc);
    }
  }


  void xxx_writeBackDeps(InfoFileDependencies info, InfoFileDependencies parent, Writer writer) 
  throws IOException
  {
    for(Map.Entry<String,InfoFileDependencies> includingEntry: parent.includingPrimaryFiles.entrySet()){
      InfoFileDependencies including = includingEntry.getValue();
      if(info.includingPrimaryFiles.get(including.sAbsolutePath) ==null){ //only if not included primary:
        writer.append("\n  + " + including.sAbsolutePath);
      }
      xxx_writeBackDeps(info, including, writer);
    }

  }
  
  
  
  
  
  @Override public String toString(){ return "" + cNewly + sFilePathIncludeline; }
  
}



