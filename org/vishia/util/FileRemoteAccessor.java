package org.vishia.util;

import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

/**Interface for instances, which organizes a remote access to files.
 * One instance per transfer protocol are need.
 * 
 * @author Hartmut Schorrig
 *
 */
public interface FileRemoteAccessor
{
  /**Version and history.
   * <ul>
   * <li>2012-01-06 Hartmut new {@link #setFileProperties(FileRemote)}. 
   * <li>2011-12-31 Hartmut new {@link Commission} and {@link #addCommission(Commission)}. It is used
   *   to add commissions to the implementation class to do in another thread/via communication.
   * <li>2011-12-10 Hartmut creation: Firstly only the {@link FileRemoteAccessorLocalFile} is written.
   *   
   * </ul>
   */
  public static final int version = 0x20111210;
  
  public final static int kOperation = 0xd00000, kFinishOk = 0xf10000, kFinishNok = 0xf10001
  , kFinishError = 0xf1e3303, kNrofFilesAndBytes = 0xd00001;

  
  public boolean setFileProperties(FileRemote file);

  ReadableByteChannel openRead(FileRemote file, long passPhase);
  
  WritableByteChannel openWrite(FileRemote file, long passPhase);
 
  FileRemote[] listFiles(FileRemote parent);
  
  
  void addCommission(Commission com);
  
  boolean isLocalFileSystem();

  
  /**The file object is a java.io.File for the local file system. If it is a remote file system,
   * the file object may be a instance for communication with the remote file system.
   * @param file The description of the file.
   * @return Any object.
   */
  //Object createFileObject(FileRemote file);
  
  public class Commission
  {
    public final static int kCheckFile = 0xcecf1e, kCheck = 0xcec, kCopy = 0xc0b7, kDel = 0xde1ede;
    
    
    
    int cmd;
    
    FileRemote src, dst;
    
    Event callBack;
    
  }
  
  
}
