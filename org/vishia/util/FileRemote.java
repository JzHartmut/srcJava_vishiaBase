package org.vishia.util;

import java.io.File;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;


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
   * <li>2012-02-02 Hartmut chg: Now the {@link #sFile} (renamed from name) is empty if this describes
   *   an directory and it is known that it is an directory. The ctor is adapted therefore.
   *   {@link #getParent()} is changed. Some assertions are set.
   * <li>2012-02-02 Hartmut chg: Handling of relative paths: It is detected in ctor. TODO relative paths are not tested well. 
   * <li>2012-01-14 Hartmut chg: The toplevel directory contains only one slash in the {@link #sDir}
   *   and an empty name in {@link #name}. 
   * <li>2012-01-14 Hartmut new: {@link #getParentFile()} now implemented here.  
   * <li>2012-01-14 Hartmut new: {@link #fromFile(File)} to convert from a normal File instance.
   * <li>2012-01-06 Hartmut new: Some functionality for {@link #_setProperties(long, long, int, Object)}
   *   and symbolic linked paths.
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
  
  /**Reference file.
   * This field, the field {@link #referenceFileRef} and the reference inside {@link #referenceFileRef} 
   * are not used if this instance does not contain a relative path. 
   * That fields should but don't need to set to null.
   * <br>
   * If this field and    
   * 
   */
  protected FileRemote referenceFile;

  /**Alternative for referencing of the reference file. */
  protected FileRemote[] referenceFileRef;
  
  /**The directory path of the file. It ends with '/' always.
   * If the path is absolute, it doesn't contain any "/./" 
   * or "/../"-parts. Then it is an canonical absolute path.
   * An absolute path can contain a start string for a remote device designation.
   * <br> 
   * The directory path may be relative too. A relative path may be empty "". */
  protected final String sDir;
  
  /**The name with extension of the file. It is empty if this describes a directory. 
   * */
  protected final String sFile;
  
  /**The unique path to the file or directory entry. If the file is symbolic linked (on UNIX systems),
   * this field contains the non-linked direct path. But the {@link #sDir} contains the linked path. 
   */
  protected String sCanonicalPath;
  
  /**Timestamp of the file. */
  protected long date;
  protected long length;
  
  /**Some flag bits. @see constants #mExist etc.*/
  int flags;

  
  protected final static int  mExist =   1;
  protected final static int  mCanRead =  2;
  protected final static int  mCanWrite =  4;
  protected final static int  mHidden = 0x08;
  protected final static int  mDirectory = 0x10;
  protected final static int  mFile =     0x20;
  protected final static int  mExecute =     0x40;
  protected final static int  mRelativePath = 0x100;
  protected final static int  mAbsPath = 0x200;
  protected final static int  mSymLinkedPath = 0x400;
  

  protected final static int  mAbsPathTested = 0x10000;
  protected final static int  mTested =       0x20000;

  
  /**This is the internal file object. It is handled by the device only. */
  Object oFile;
  
  
  /**Creates an instance without access to the physical file. Only The path is stored. 
   * @param pathname device, path and name. The path may be relative. 
   */
  public FileRemote(String pathname)
  {
    this(FileRemoteAccessorLocalFile.getInstance(), pathname, null, -1, 0, 0, null);
  }

  
  /**Creates an instance without access to the physical file. Only The path is stored. 
   * @param dir device and path to the directory (parent). The dir may be relative.
   * @param name name of the file inside the given dir. 
   */
  public FileRemote(String dir, String name)
  {
    this(FileRemoteAccessorLocalFile.getInstance(), dir, name, -1, 0, 0, null);
  }

  
  /**Creates an instance without access to the physical file. Only The path is stored. 
   * @param dir instance describes the directory (parent).
   * @param name name of the file inside the given dir. 
   */
  public FileRemote(FileRemote dir, String name)
  {
    this(dir.device, dir.getPath(), name, -1, 0, 0, null);
  }

  
  
  
  /**Constructs the instance. If the length parameter is given or it is 0, 
   * this invocation does not force any access to the file system. The parameter may be given
   * by a complete communication or file access before construction of this. Then they are given
   * as parameter for this constructor.<br>
   * If the length ==-1, then the fields of this are initialized via access to
   * {@link FileRemoteAccessor#setFileProperties(FileRemote)}. This forces an access to the file system,
   * maybe a remote communication (depending on the implementation of that method).
   *  
   * @param device The device which organizes the access to the file system.
   * @param sDirP The path to the directory.
   *   The standard path separator is the slash "/". 
   *   A backslash will be converted to slash internally, it isn't distinct from the slash.
   *   If this parameter ends with an slash or backslash and the name is null or empty, this is designated 
   *   as an directory descriptor. {@link #mDirectory} will be set in {@link #flags}.
   * @param sName Name of the file. If null then the name is gotten from the last part of path
   *   after the last slash or backslash.
   * @param length The length of the file. ==-1 then all other parameter are gotten from the file
   * @param date Timestamp of the file. 
   * @param flags Properties of the file.
   */
  public FileRemote(final FileRemoteAccessor device
      , final String sDirP, final String sName
      , final long length, final long date, final int flags
      , Object oFileP) {
    super(sDirP + (sName ==null ? "" : ("/" + sName)));  //it is correct if it is a local file. 
    String sPath = sDirP.replace('\\', '/');
    this.device = device;
    this.flags = flags;
    String name1;
    if(sName == null){
      int lenPath = sPath.length();
      int posSep = sPath.lastIndexOf('/'); //, lenPath-2);
      if(posSep >=0){
        this.sDir = sPath.substring(0, posSep+1);
        if(posSep == sPath.length()-1){
          name1 = "";  //it is a directory.
          this.flags |= mDirectory;
        } else {
          name1 = sPath.substring(posSep+1);
        }
      } else { //no / found, it is only a name:
        this.flags |= mRelativePath;
        this.sDir = "";
        name1 = sPath;
      }
    } else { //name is given:
      if(!sPath.endsWith("/")){ sPath += "/";}
      this.sDir = sPath;
      name1 = sName;
    }
    //if(name1.endsWith("/")){ this.name = name1.substring(0, name1.length()-1); }
    //else { this.name = name1; }
    this.sFile = name1;
    Assert.check(this.sFile !=null);
    Assert.check(this.sDir !=null);
    Assert.check(!name1.contains("/"));
    Assert.check(this.sDir.length() == 0 || this.sDir.endsWith("/"));
    Assert.check(!this.sDir.endsWith("//"));
    if(length == -1){
      device.setFileProperties(this);   ////
    } else {
    //oFile = oFileP !=null ? oFileP : device.createFileObject(this);
      this.length = length;
      this.date = date;
      this.sCanonicalPath = this.sDir + this.sFile;  //maybe overwrite from setSymbolicLinkedPath
    }
  }
  
  
  
  /**Returns a FileRemote instance from a standard java.io.File instance.
   * If src is instanceof FileRemote already, it returns src.
   * Elsewhere it builds a new instance of FileRemote which inherits from File,
   * it is a new instance of File too.
   * @param src Any File or FileRemote instance.
   * @return src if it is instanceof FileRemote or a new Instance.
   */
  public static FileRemote fromFile(File src){
    if(src instanceof FileRemote){ return (FileRemote)src; }
    else return new FileRemote(src.getAbsolutePath());
  }
  
  
  /**Sets the properties to this.
   * 
   * This method is not intent to invoke from a users application. It should only invoked 
   * by the {@link FileRemoteAccessor} implementation to set properties of this file. 
   * Only because the {@link FileRemoteAccessor} implementation may be organized
   * in another package, it should be public.
   * 
   * @param length
   * @param date
   * @param flags
   * @param oFileP reference to a file Object, for example java.io.File for local files.
   */
  public void _setProperties(final long length, final long date, final int flags
      , Object oFileP) {
    this.length = length;
    this.date = date;
    this.flags = flags;
    this.oFile = oFileP;
  }
  
  
  /**Sets the reference for a given relative path to the named file. 
   * <br><br>
   * <b>Concepts of relative paths</b>:
   * <ul>
   * <li><b>Current directory</b>: In a local file-system most of the operation systems knows a <i>current directory</i> which is
   *   the base for relative paths. The <i>current directory</i> is a property of the running process on
   *   operation system. A java.io.File with a relative path references to this <i>current directory</i> 
   *   which can be gotten by system calls. <br>
   *   This class needs a direct reference to the <i>current directory</i> given with this method.
   * <li><b>relative link</b>: For links (browser, XML) another concept is usually: A relative path starts
   *   from that file which contains the link. This field should refer that source file then.
   * </ul>
   * This method should not be called with any reference if the file contains an absolute path. The reference
   * file is not used then.
   *  
   * @param ref If the file is given with a relative path, this is the reference file.
   */
  public void setReferenceFile(FileRemote ref){
    referenceFile = ref;
    referenceFileRef = null;
  }
  
  
  /**Sets the reference for a given relative path to the file references with ref.
   * This is a alternative to {@link #setReferenceFile(FileRemote)}. Only one of them should be invoked,
   * the last invocation wins. The advantage of this method: Changing of a referencing file
   * have to be done only in one instance, the ref. All files which references ref don't need to be changed. 
   * @param ref Reference to a FileRemote[1] which references the reference file for the relative path.
   */
  public void setReferenceFile(FileRemote[] ref){
    referenceFile = null;
    referenceFileRef = ref;
  }
  
  
  
  /**Sets this as a symbolic linked file or dir with the given path. 
   * 
   * This method is not intent to invoke from a users application. It should only invoked 
   * by the {@link FileRemoteAccessor} implementation to set properties of this file. 
   * Only because the {@link FileRemoteAccessor} implementation may be organized
   * in another package, it should be public.
   * 
   * @param pathP The path where the file is organized.
   */
  public void setSymbolicLinkedPath(String pathP){
    flags |= mSymLinkedPath;
    this.sCanonicalPath = pathP;
  }
  
  
  /**Sets this as a non-symbolic linked file or dir with the given path. 
   * 
   * This method is not intent to invoke from a users application. It should only invoked 
   * by the {@link FileRemoteAccessor} implementation to set properties of this file. 
   * Only because the {@link FileRemoteAccessor} implementation may be organized
   * in another package, it should be public.
   * 
   * @param pathP The absolute canonical path where the file is organized.
   */
  public void setCanonicalAbsPath(String pathP){
    flags |= mAbsPath;
    flags &= ~mSymLinkedPath;
    this.sCanonicalPath = pathP;
  }
  
  
  /**Check whether two files are at the same device. It means it can be copied, compared etc. remotely. 
   * @param other The other file to check
   * @return true if they are at the same device. The same device is given if the comparison
   *   of the {@link FileRemoteAccessor} instances of both files using {@link #device}.equals(other.device) 
   *   returns true.
   */
  public boolean sameDevice(FileRemote other){ return device.equals(other.device); }
  
  
  /**Opens a read access to this file.
   * @param passPhrase a pass phrase if the access is secured.
   * @return The channel to access.
   */
  public ReadableByteChannel openRead(long passPhrase){
    return device.openRead(this, passPhrase);
  }
  
  /**Opens a write access to this file.
   * @param passPhrase a pass phrase if the access is secured.
   * @return The channel to access.
   */
  public WritableByteChannel openWrite(long passPhrase){
    return device.openWrite(this, passPhrase);
  }
  
  
  @Override public long length(){ 
    if(length ==-1){
      if(device.isLocalFileSystem()) length = super.length();
      else device.setFileProperties(this);  //maybe wait for communication.
    }
    return length; 
  }
  
  @Override public long lastModified(){ 
    if(date ==0){
      if(device.isLocalFileSystem()) date = super.lastModified();
      else device.setFileProperties(this);  //maybe wait for communication.
    }
    return date; 
  }
  
  @Override public String getName(){ return sFile; }
  
  @Override public String getParent(){ 
    String sParent;
    int zDir = sDir.length();
    Assert.check(sDir.charAt(zDir-1) == '/'); //Should end with slash
    if(sFile == null || sFile.length() == 0){
      //it is a directory, get its parent path
      if(zDir > 1){
        int pDir = sDir.lastIndexOf('/', zDir-2);
        if(pDir >0){
          sParent = sDir.substring(0, pDir+1);  //with ending slash
        } else {
          sParent = null;  //sDir is forex "D:/", or "/" it hasn't a parent.
        }
      } else {
        sParent = null;    //sDir has only one char, it may be a "/", it hasn't a parent.
      }
    } else { //a sFile is given, the sDir is the parent.
      sParent = sDir;
    }
    return sParent;
    //int posSlash = sDir.indexOf('/');  //the first slash
    //if only one slash is present, return it. Elsewhere don't return the terminating slash.
    //String sParent = zDir > posSlash+1 ? sDir.substring(0, zDir-1): sDir;
    //return sParent; 
  }
  
  /**Gets the path of the file. For this class the path should be esteemed as canonical,
   * it is considered on constructor. 
   */
  @Override public String getPath(){ return sDir + sFile; }
  
  @Override public String getCanonicalPath(){ return sCanonicalPath; }
  
  
  /**Gets the parent directory.
   * It creates a new instance of FileRemote with the path infos from {@link #sDir}.
   * 
   * @return null if this is the toplevel directory.
   */
  @Override public FileRemote getParentFile(){
    final FileRemote parent;
    String sParent = getParent();
    if(sParent !=null){
      parent = new FileRemote(sParent);
    
    //if(sDir.indexOf('/') < sDir.length()-1 || name.length() > 0){
    //  parent = new FileRemote(sDir);
    } else {
      parent = null;
    }
    return parent;
  }
  
  
  @Override public boolean isDirectory(){ return (flags & mDirectory) !=0; }
  
  public boolean isSymbolicLink(){ return (flags & mSymLinkedPath) !=0; }
  
  
  /**This method overrides java.io.File.listFiles() but returns Objects from this class type.
   * @see java.io.File#listFiles()
   */
  @Override public FileRemote[] listFiles(){
    return device.listFiles(this);
  }
  
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
