package org.vishia.fileRemote;

import java.io.File;
import java.util.LinkedList;
import java.util.List;



/**Order for copy, created on check.
 * @author Hartmut Schorrig
 *
 */
public class FileRemoteCopyOrder
{

  
  public final long ident;
  
  /**If the order is to old, remove it. */
  public long dateCreation;
  
  /**File or dir to copy. */
  public FileRemote fileSrc, fileDst;
  
  
  /**List of files to handle between {@link #checkCopy(org.vishia.util.FileRemoteAccessor.FileRemote.CallbackEvent)}
   * and {@link #execCopy(org.vishia.util.FileRemoteAccessor.FileRemote.CallbackEvent)}. */
  public final List<File> listCopyFiles = new LinkedList<File>();
  

  
  public FileRemoteCopyOrder(long ident){
    this.ident = ident;
  }
  
  
}
