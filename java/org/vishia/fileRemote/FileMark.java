package org.vishia.fileRemote;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.vishia.util.Debugutil;
import org.vishia.util.MarkMask_ifc;
import org.vishia.util.SelectMask;

/**Class can be associated with a {@link FileRemote} to store comparison or mark information.
 * @author Hartmut Schorrig
 *
 */
public class FileMark extends SelectMask 
{
  
  /**Version, history and license.
   * <ul>
   * <li>2023-02-12 Hartmut it has uses constants from {@link MarkMask_ifc} de facto, but not documented.
   *   Not documented. Enhanced with {@link #selectForCopy} =^ {@link MarkMask_ifc#select2}.
   *   Also shown as colors in the {@link org.vishia.gral.widget.GralFileSelector}. 
   * <li>2013-05-22 Hartmut created: A {@link FileRemote} may have information about a select status
   *   or about a comparison result. It is stored in a referred instance of this type. Experience.
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
   */
  public static final String sVersion = "2023-02-13";


  /**Flags as result of an comparison: the other file is checked by content maybe with restricitons. */
  public static final char XXXcharCmpContentEqual = '=';


  /**Flags as result of an comparison: the other file is checked by content maybe with restricitons. */
  public static final char XXXcharCmpContentEqualWithoutComments = '#';


  /**Flags as result of an comparison: the other file is checked by content maybe with restricitons. */
  public static final char XXXcharCmpContentEqualWithoutEndlines = '$';


  /**Flags as result of an comparison: the other file is checked by content maybe with restricitons. */
  public static final char XXXcharCmpContentEqualwithoutSpaces = '+';

  /**Flags is a simple marker for manual selecting for a file, also for a directory, shown in red in Fcmd. */
  public static final int select = MarkMask_ifc.select;

  /**Flags means that some but not all files are marked inside a directory. */
  public static final int selectSomeInDir = MarkMask_ifc.selectParent;

  /**Flags is a marker for files or the whole dir to copy, select in orange in Fcmd. */
  public static final int selectForCopy = MarkMask_ifc.select2;

  /**Flags is a marker for directory which contains some to copy, select in light orange in Fcmd. */
  public static final int selectForCopySomeInDir = MarkMask_ifc.select2Parent;


  /**Flags means that this file is the root of mark. It is used for {@link FileMark#setMarkParent(int, boolean)} */
  public static final int markRoot = 0x00100000;

  /**Flags means that this file is any directory which is in the mark tree. */
  public static final int markDir = 0x00200000;


  
  /**Flags as result of an comparison: the other file does not exist, or any files of an directory does not exists
   * or there are differences. */
  public static final int cmpAlone =           0x01000000;

  /**Flags as result of an comparison: the other file does not exist, or exists only with same length or with same time stamp */
  //public static final int cmpLenEqual = 0x02000000;

  /**Flags as result of an comparison: the other file has the same length and same time stamp, it seems it may be equal, but not tested. */
  public static final int cmpLenTimeEqual =    0x02000000;

  /**Flags as result of an comparison: the other file does not exist, or exists only with same length or with same time stamp */
  public static final int cmpContentEqual =    0x04000000;

  /**Flags as result of an comparison: the other file does not exist, 
   * or exists with other length or other timestamp, or with other content also with same length or with same time stamp */
  public static final int cmpContentNotEqual = 0x08000000;

  /**mask of all bits for comparison one file. */
  public static final int mCmpFile =           0x0f000000;

  

  /**Flags as result of an comparison: the other file does not exist, or any files of an directory does not exists
   * or there are differences. */
  public static final int cmpMissingFiles = 0x10000000;

  /**Flags as result of an comparison: the other file does not exist, or any files of an directory does not exists
   * or there are differences. */
  public static final int cmpFileDifferences = 0x20000000;

  /**This is not used for mark, only for a mark command. 
   * It means if this bit is set: OR relation for selection together with a select mask.
   * If this bit is not set, it is a AND relation with the select mask.
   */
  public static final int orWithSelectString = 0x40000000;
  
  /**This is not used for mark, only for a mark command. 
   * It means if this bit is set: reset instead set of a mark */
  public static final int alternativeFunction = 0x80000000;
  
  
  
  protected final FileRemote itsFile;
  
  public int nrofFilesSelected;
  
  
  
  public long nrofBytesSelected;

  
  public FileMark(FileRemote itsFile){
    this.itsFile = itsFile;
  }
  
  public int nrofFilesSelected(){ return nrofFilesSelected; } 
  
  
  public int setNonMarkedRecursively(int mask, Object data, boolean recursively)
  { if(itsFile.getName().equals("ReleaseNotes.topic"))
      Debugutil.stop();
    int selectOld = super.setNonMarked(mask, null);
    if(itsFile.isDirectory()){
      
    }
    if(recursively){
      FileRemote parent = itsFile;
      while( (parent = parent.getParentFile()) !=null
        && parent.mark !=null   //abort while-loop if the parent is not marked 
        ){
        parent.mark.nrofFilesSelected -=this.nrofFilesSelected;
        if(parent.mark.nrofFilesSelected <=0){
          parent.mark.nrofFilesSelected = 0;
          parent.mark.selectMask &= ~mask;
        }
        parent.mark.nrofBytesSelected -=this.nrofBytesSelected;
        if(parent.mark.nrofBytesSelected <=0){
          parent.mark.nrofBytesSelected = 0;
        }
      }
    }
    this.nrofBytesSelected = 0;
    this.nrofFilesSelected = 0;
    return selectOld;
  }

  @Override public int setMarked(int mask, Object data)
  { if(itsFile.getName().equals("ReleaseNotes.topic"))
      Debugutil.stop();
  
    int selectOld = super.setMarked(mask, null);
    //FileRemote file = (FileRemote)data;
    if(itsFile.isDirectory()){
      //remain selection info set from children
      //but it may be 0.
    } else {
      this.nrofBytesSelected = itsFile.length();
      this.nrofFilesSelected = 1;
      /*
      FileRemote parent = itsFile;
      List<FileRemote> parents = null;
      while( (parent = parent.getParentFile()) !=null){
        if(parent.mark !=null && parent.mark.selectMask & Mark)
      }
      //inform all parents about a selection into. Mark it with select it too.
      while( (parent = parent.getParentFile()) !=null){
        if(parent.mark == null){
          parent.mark = new Filemark(parent);
        }
        parent.mark.selectMask |= mask;
        parent.mark.nrofFilesSelected += this.nrofFilesSelected;
        parent.mark.nrofBytesSelected += this.nrofBytesSelected;
      }
      */
    }
    return selectOld;
  }
  
  
  /**Sets a mark to all parents of this till the {@link #markRoot} is found.
   * @param mask
   * @param count TODO
   */
  public void setMarkParent(int mask, boolean count){
    FileRemote parent = itsFile;
    List<FileRemote> parents = null;
    FileRemote lastDirParent = itsFile;
    if((selectMask & markRoot) ==0){
      while( (parent = parent.getParentFile()) !=null){  //break inside!
        if(parent.mark== null) {
          parent.mark = new FileMark(parent);  //it is not the root of marking. Any directory between this and root.
        }
        //if(parent.mark !=null && (parent.mark.selectMask & (FileMark.markDir | FileMark.markRoot))!=0){
          lastDirParent = parent;
          parent.mark.selectMask |= mask;
          if(count){
            parent.mark.nrofFilesSelected += this.nrofFilesSelected;
            parent.mark.nrofBytesSelected += this.nrofBytesSelected;
          }
          if((parent.mark.selectMask & FileMark.markRoot)!=0){
            break;
          }
        //} else {
        //  if(parents == null){ parents = new LinkedList<FileRemote>(); }
        //  parents.add(parent);  //in case of found a markRoot, mark all that with markDir
        //}
      }
      if(parent !=null){ //any markRoot found
        if(parents !=null){ //but not all markDir existing:
          //This routine is done only the first time if any parent is marked as root
          //but all other child directories in the path are not marked. 
          for(FileRemote parent1: parents){
            parent1.setMarked(mask | FileMark.markDir);
            if(count){
              parent.mark.nrofFilesSelected += this.nrofFilesSelected;
              parent.mark.nrofBytesSelected += this.nrofBytesSelected;
            }
          }
        } else {
          //all ok
        }
      } else {
        //a markRoot was not found, set the markRoot to the last valid file.
        lastDirParent.setMarked(FileMark.markRoot);
      }
    }
  }
  
  
  
  
  
}
