package org.vishia.util;

import java.io.File;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import org.vishia.mainCmd.MainCmd;

/**This class describes a File, which may be localized at any maybe remote device or which may be a normal local file. 
 * A remote file should be accessed by FileRemoteChannel implementations. It may be any information
 * at an embedded hardware, not only in a standard network.
 * This class executes a remote access only if properties of the file are requested.
 * It stores information about the file without access. 
 * <br><br>
 * This class inherits from java.lang.File. The advantage is:
 * <ul>
 * <li>The class File defines the interface to the remote files too. No extra definition of access methods is need.
 * <li>Any reference to a file can store the reference to a remote file too.
 * <li>The implementation for local files is given without additional effort.
 * </ul> 
 * If the file is a local file, the standard java file access algorithm are used.
 * @author Hartmut Schorrig
 *
 */
public class FileRemote extends File
{
  private static final long serialVersionUID = -5568304770699633308L;

  /**Version and history.
   * <ul>
   * <li>2012-01-01 Hartmut new: {@link #oFile}. In the future the superclass File should be used only as interface.
   *   TODO: For any file access the oFile-instance should be used by {@link #device}.
   * <li>2012-01-01 Hartmut new: {@link #copyTo(FileRemote, Event)}
   * <li>2011-12-10 Hartmut creation: It is needed for {@link org.vishia.commander.Fcmd}, this tool
   *   should work with remote files with any protocol for example FTP. But firstly it is implemented and tested
   *   only for local files. The concept is: 
   *   <ul>
   *   <li>All relevant information about a file are stored locally in this instance if they are known.
   *   <li>If information are unknown, on construction 'unknown' is stored, no access occurs.
   *     But if they are requested, an access is done.
   *   <li>Any instance of the interface {@link FileRemoteAccessor} is responsible to execute the remote access.
   *   <li>The access to local files are done with this class directly.
   *   </ul>
   * </ul>
   */
  public static final int version = 0x20111210;

  
  protected FileRemoteAccessor device;
  
  
  
  /**The directory path of the file. */
  protected final String path;
  /**The name with extension of the file. */
  protected final String name;
  protected long date;
  protected long length;
  protected boolean isWriteable;
  
  /**This is the internal file object. It is handled by the device only. */
  Object oFile;
  
  
  public FileRemote(String pathname)
  {
    this(FileRemoteAccessorLocalFile.getInstance(), pathname, null, -1, 0, false);
  }

  
  public FileRemote(String path, String name)
  {
    this(FileRemoteAccessorLocalFile.getInstance(), path, name, -1, 0, false);
  }

  
  public FileRemote(FileRemote dir, String name)
  {
    this(FileRemoteAccessorLocalFile.getInstance(), dir.getAbsolutePath(), name, -1, 0, false);
  }

  
  
  
  /**Constructs the instance. This invocation does not force any access to the file system.
   * @param device The device which organizes the access to the file system.
   * @param sPath The path to the directory.
   *   The standard path separator is the slash /. A backslash will be converted to slash internally.
   *   it isn't distinct from the slash.
   * @param sName Name of the file. If null then the name is gotten from the last part of path.
   * @param length The length of the file. If negative then the length is gotten from the file
   *   if the length is requested by {@link #length()}.
   * @param date Timestamp of the file. If 0 then the timestamp is gotten from the file
   *   if the timestamp is requested by {@link #lastModified()}.
   * @param isWriteable Status of writeable
   */
  public FileRemote(final FileRemoteAccessor device, final String sPathP, final String sName, final long length, final long date, final boolean isWriteable){
    super(sPathP + (sName ==null ? "" : ("/" + sName)));  //it is correct if it is a local file. 
    String sPath = sPathP.replace('\\', '/');
    this.device = device;
    if(sName == null){
      int lenPath = sPath.length();
      int posSep = sPath.lastIndexOf('/', lenPath-2);
      if(posSep >=0){
        this.path = sPath.substring(0, posSep+1);
        this.name = sPath.substring(posSep+1);
      } else {
        this.path = "";
        this.name = sPath;
      }
    } else {
      if(!sPath.endsWith("/")){ sPath += "/";}
      this.path = sPath;
      this.name = sName;
    }
    MainCmd.assertion(this.path.length() == 0 || this.path.endsWith("/"));
    MainCmd.assertion(!this.path.endsWith("//"));
    oFile = device.createFileObject(this);
    this.isWriteable = isWriteable;
    this.length = length;
    this.date = date;
  
  }
  
  
  
  /**Check whether two files are at the same device. It means it can be copied, compared etc. remotely. 
   * @param other The other file to check
   * @return true if they are at the same device. The same device is given if the comparison
   *   of the {@link FileRemoteAccessor} instances of both files using {@link #device}.equals(other.device) 
   *   returns true.
   */
  public boolean sameDevice(FileRemote other){ return device.equals(other.device); }
  
  
  public ReadableByteChannel openRead(long passPhrase){
    return device.openRead(this, passPhrase);
  }
  
  public WritableByteChannel openWrite(long passPhrase){
    return device.openWrite(this, passPhrase);
  }
  
  
  @Override public long length(){ 
    if(length ==-1){
      if(device.isLocalFileSystem()) length = super.length();
      else device.getFileProperties(this);  //maybe wait for communication.
    }
    return length; 
  }
  
  @Override public long lastModified(){ 
    if(date ==0){
      if(device.isLocalFileSystem()) date = super.lastModified();
      else device.getFileProperties(this);  //maybe wait for communication.
    }
    return date; 
  }
  
  @Override public String getName(){ return name; }
  
  @Override public String getParent(){ return path; }
  
  /**Gets the path of the file. For this class the path should be esteemed as canonical,
   * but that should be considered on constructor. 
   */
  @Override public String getPath(){ return path + name; }
  
  /**Deletes a file maybe in a remote device. This is a send-only routine without feedback,
   * because the calling thread should not be waiting for success.
   * The success is notified with invocation of the 
   * {@link Event#dst}.{@link EventConsumer#processEvent(Event)} method. 
   * @param backEvent The event for success.
   */
  public void delete(Event backEvent){
    if(device.isLocalFileSystem()){
      boolean bOk;
      if(super.isDirectory()){
        bOk = FileSystem.rmdir(this);
      } else {
        bOk = super.delete();
      }
      backEvent.data1 = bOk? 0 : -1;
      backEvent.dst.processEvent(backEvent);
    } else {
      //TODO
    }
  }
  
  
  
  /**Checks a file maybe in a remote device maybe a directory. 
   * This is a send-only routine without feedback, because the calling thread should not be waiting 
   * for success. The success is notified with invocation of the 
   * {@link Event#dst}.{@link EventConsumer#processEvent(Event)} method. 
   * 
   * @param dst This file will be created or filled newly. If it is existing but read only,
   *   nothing is copied and an error message is fed back.
   * @param backEvent The event for success.
   */
  public void check(Event backEvent){
    if(device.isLocalFileSystem()){
      FileRemoteAccessor.Commission com = new FileRemoteAccessor.Commission();
      com.callBack = backEvent;
      com.cmd = FileRemoteAccessor.Commission.kCheckFile;
      com.src = this;
      com.dst = null;
      device.addCommission(com);
    } else {
      //TODO
    }
  }
  
  
  /**Copies a file maybe in a remote device to another file in the same device. 
   * This is a send-only routine without feedback, because the calling thread should not be waiting 
   * for success. The success is notified with invocation of the 
   * {@link Event#dst}.{@link EventConsumer#processEvent(Event)} method. 
   * 
   * @param dst This file will be created or filled newly. If it is existing but read only,
   *   nothing is copied and an error message is fed back.
   * @param backEvent The event for success.
   */
  public void copyTo(FileRemote dst, Event backEvent){
    if(device.isLocalFileSystem() && dst.device.isLocalFileSystem()){
      FileRemoteAccessor.Commission com = new FileRemoteAccessor.Commission();
      com.callBack = backEvent;
      com.cmd = FileRemoteAccessor.Commission.kCopy;
      com.src = this;
      com.dst = dst;
      device.addCommission(com);
    } else {
      //TODO
    }
  }
  
  
  
  
  
  
}
