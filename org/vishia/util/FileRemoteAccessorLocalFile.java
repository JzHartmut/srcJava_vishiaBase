package org.vishia.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

/**Implementation for a standard local file.
 */
public class FileRemoteAccessorLocalFile implements FileRemoteAccessor
{
  
  /**Version and history.
   * <ul>
   * <li>2011-12-10 Hartmut creation: See {@link FileRemoteAccessor}.
   * </ul>
   */
  public static final int version = 0x20111210;

  private static FileRemoteAccessor instance = new FileRemoteAccessorLocalFile();
  
  public static FileRemoteAccessor getInstance(){
    return instance;
  }
  
  @Override public boolean getFileProperties(FileRemote file)
  { return false; //file.super.lastModified();
  }

  @Override public ReadableByteChannel openRead(FileRemote file, long passPhase)
  { try{ 
      FileInputStream stream = new FileInputStream(file);
      return stream.getChannel();
    } catch(FileNotFoundException exc){
      return null;
    }
  }

  @Override
  public WritableByteChannel openWrite(FileRemote file, long passPhase)
  { try{ 
    FileOutputStream stream = new FileOutputStream(file);
    return stream.getChannel();
    } catch(FileNotFoundException exc){
      return null;
    }
  }

  @Override public boolean isLocalFileSystem()
  {  return true;
  }
  
}
