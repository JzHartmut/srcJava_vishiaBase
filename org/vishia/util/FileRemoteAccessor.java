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
   * <li>2011-12-10 Hartmut creation: Firstly only the {@link FileRemoteAccessorLocalFile} is written.
   *   
   * </ul>
   */
  public static final int version = 0x20111210;
  
  public boolean getFileProperties(FileRemote file);

  ReadableByteChannel openRead(FileRemote file, long passPhase);
  
  WritableByteChannel openWrite(FileRemote file, long passPhase);
  
  boolean isLocalFileSystem();

  
  
}
