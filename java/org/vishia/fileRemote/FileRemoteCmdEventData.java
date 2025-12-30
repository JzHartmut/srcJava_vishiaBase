package org.vishia.fileRemote;

import org.vishia.event.Payload;
import org.vishia.util.SortedTreeWalkerCallback;

public class FileRemoteCmdEventData  implements Payload { 
  
  private static final long serialVersionUID = 1L;

  /**The command to execute with this cmd event payload. */
  private Cmd cmd;

  /**Milliseconds for cycle or specific determined progress event:
   * <br>0= progress event for any file and directory entry. It is especially for refresh.
   */
  private int cycleProgress;
  
  /**Source and destination files for copy, rename, move or the only one filesrc. filedst may remain null then. */
  private FileRemote filesrc, filedst;

  /**List of names separated with ':' or " : " or empty. If not empty, filesrc is the directory of this files. */
  private String namesSrc;
  
  /**Bits to mark files while walking through. */
  private int markSet, markSetDir;
  
  /**Wildcard mask to select source files. 
   * Maybe empty or null, then all files are used.*/
  private String selectFilter;
  
  /**Bits to select from mark. 
   * If files are marked before by comparison, als manually, this can be used.
   * It can use some specific bits: {@link FileMark#orWithSelectString}, {@link FileMark#ignoreSymbolicLinks}
   */
  private int selectMask;
  
  /**Depths to walk in dir tree, 0: all. */
  private int depthWalk;
  
  /**Designation of destination file names maybe wildcard mask. */
  private String nameDst;
  
  /**A order number for the handled file which is gotten from the callback event to ensure thats the right file. */
  //private int orderFile;
  
  /**Mode of operation, see {@link FileRemote#modeCopyCreateAsk} etc. */
  private int modeCopyOper;
  
  public int modeCmpOper;  // maybe also the modeCopyOper can be used as only one mode...
  
  /**For {@link Cmd#chgProps}: a new name. */
  private String newName;
  
  /**For {@link Cmd#chgProps}: new properties with bit designation see {@link FileRemote#flags}. 
   * maskFlags contains bits which properties should change, newFlags contains the value of that bit. */
  int maskFlags, newFlags;
  
  /**For {@link Cmd#chgProps}: A new time stamp. */
  long newDate;
  
  private SortedTreeWalkerCallback<FileRemote, FileRemoteCmdEventData> callback;  //it may be implementation specific
  
  
  /**Creates the payload of a command event
   */
  public FileRemoteCmdEventData(){     }
  

  /**Sets the command and data for walk through the file system for remote access.
   * It is complete for a walking. Note: The {@link #callback} cannot be given here, it should be organized in the remote device.
   * It is consequently to do so also if it is executed only in another thread on the same device.
   * @param srcdir the directory which is used for walk through as base or start directory.
   * @param cmd command to execute. It determines what is done with the file. Only specific commands are admissible.
   * @param dstdir the directory which is used as destination due to the cmd.
   * @param selectFilter A filter for the sub dir and files
   * @param cycleProgress for the callback event to inform about progress
   * @param depthWalk
   */
  public void setCmdWalkRemote ( FileRemote srcdir, Cmd cmd, FileRemote dstdir
      , String selectFilter, int selectMask, int cycleProgress, int depthWalk) {
    clean();
    this.filesrc = srcdir;
    this.filedst = dstdir;
    this.cmd = cmd; this.cycleProgress = cycleProgress;
    this.selectFilter = selectFilter; 
    this.selectMask = selectMask;
    this.depthWalk = depthWalk;
  }
  
  
  
  /**Sets the command and data for changing a file in a remote file system. All data for change are in the payload.
   * It is also for delete the file or get data from the file, or also copy one file.
   * @param file
   * @param cmd
   */
  public void setCmdChgFileRemote ( FileRemote file, Cmd cmd, FileRemote fileDst, String nameNew, long dateNew  ) {
    clean();
    this.filesrc = file;
    this.filedst = fileDst;
    this.cmd = cmd; 
    this.newName = nameNew;
    this.newDate = dateNew;
    this.cycleProgress = 0;
    this.selectFilter = null; 
    this.depthWalk = 0;
  }
  
  
  
  /**Sets the command and data for walk through the file system on a local device in the same data space. 
   * It means the FileRemote instances are available. It is for the PC Operation system files on the same PC or in the PCs network access.
   * The access can be run in the same thread or in another thread.
   * @param srcdir
   * @param cmd
   * @param dstdir
   * @param markSet
   * @param markSetDir
   * @param selectFilter
   * @param selectMask
   * @param depthWalk
   * @param callback It can be given if the cmd has not an own callback.
   * @param cycleProgress determines the time of sending the progress event. -1 = never, 0=on any file, >0 is milliseconds.
   */
  public void setCmdWalkLocal ( FileRemote srcdir, Cmd cmd, FileRemote dstdir, int markSet, int markSetDir
      , String selectFilter, int selectMask, int depthWalk
      , SortedTreeWalkerCallback<FileRemote, FileRemoteCmdEventData> callback, int cycleProgress) {
    clean();
    this.filesrc = srcdir;
    this.filedst = dstdir;
    this.markSetDir = markSetDir;
    this.markSet = markSet;
    this.cmd = cmd; this.cycleProgress = cycleProgress;
    this.selectFilter = selectFilter; 
    this.selectMask = selectMask;
    this.depthWalk = depthWalk;
    this.callback = callback;
  }
  
  public Cmd cmd ( ) { return cmd; }
  
  public final FileRemote filesrc()
  {
    return filesrc;
  }

  public final FileRemote filedst()
  {
    return this.filedst;
  }

  public SortedTreeWalkerCallback<FileRemote, FileRemoteCmdEventData> callback () { return this.callback; }
  
  public void setCallback (FileRemoteWalkerCallback callback) { this.callback = callback; }
  
  public int markSet () { return this.markSet; }
  
  public int markSetDir () { return this.markSetDir; }
  
  public String selectFilter () { return this.selectFilter; }
  
  public int selectMask () { return this.selectMask; }
  
  public int cycleProgress () { return this.cycleProgress; }
  
  public int depthWalk () { return this.depthWalk; }
  
  
  public final int modeCopyOper()
  {
    return this.modeCopyOper;
  }

  public final String newName()
  {
    return this.newName;
  }

  public final int maskFlags()
  {
    return this.maskFlags;
  }

  public final int newFlags()
  {
    return this.newFlags;
  }

  public final long newDate()
  {
    return this.newDate;
  }


  @Override public FileRemoteCmdEventData clean () {
    this.cmd = Cmd.noCmd;
    this.cycleProgress = 0;
    this.filesrc = null;
    this.filedst = null;
    this.namesSrc = null;
    this.markSet = 0;
    this.markSetDir = 0;
    this.selectFilter = null;
    this.depthWalk = 0;        //means any deepness
    this.nameDst = null;
    this.modeCopyOper = 0;
    this.newName = null;
    this.maskFlags = 0;
    this.newFlags = 0;
    this.newDate = 0;
    this.callback = null;
    return this;
  }


  @Override public byte[] serialize () {
    // TODO Auto-generated method stub
    return null;
  }


  @Override public boolean deserialize ( byte[] data ) {
    // TODO Auto-generated method stub
    return false;
  }
  
  public enum Cmd {
    /**Ordinary value=0. */
    noCmd ,
    /**Ordinary value=1. */
    reserve,  //first 2 ordinaries from Event.Cmd
    /**Check files. */
    copyFile,
    moveFile,
    check,
    move,
    moveChecked,
    /**Copy to dst.*/
    copyChecked,
    chgProps,
    chgPropsRecurs,
    countLength,
    delete,
    delChecked,
    compare,
    mkDir,
    /**Error on mkdir call */
    mkDirError,
    mkDirs,
    walkTest,
    walkDelete,
    /**walk through the file tree for refreshing the selected files with really file system information.
     * For a remote device it presumes that the back event is given and received for any file.*/
    walkRefresh,
    /**walk through two file trees with given select masks, copy with overwrite selected files and resets the mark.*/
    walkCopyDirTree,
    /**walk through two file trees with given select masks, copy with overwrite selected files and resets the mark.*/
    walkMoveDirTree,
    /**walk through two file trees with given select masks, compare the files and mark due to comparison result.*/
    walkCompare,
    /**Abort the currently action. */
    abortAll,
    /**Abort the copy process of the current directory or skip this directory if it is asking a file. */
    abortCopyDir,
    /**Abort the copy process of the current file or skip this file if it is asking. */
    abortCopyFile,
    /**Overwrite a read-only file. */
    overwr,
    /**Last. */
    last,
    docontinue,
  }
  



}
