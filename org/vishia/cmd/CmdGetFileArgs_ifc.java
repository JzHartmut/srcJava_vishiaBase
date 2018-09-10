package org.vishia.cmd;

import java.io.File;
import java.util.List;

/**Gets file arguments from the environment to build a command line.
 * <br><br>
 * <b>Local path conventions</b>:<br>
 * It is suitable to have a local path. A local path starts from a given directory, for example the current dir.
 * But it may be a special directory what is any base directory, too.
 * The File.getPath() may return a local path, if the file is created therewith, but it is the local path
 * starting from the current directory only. To get the base directory to any file, the methods getBaseDir()
 * are given for each file. The local path can be gotten by substring of the canonical path starting from base dir.
 * <br><br>
 * <b>File separator conventions</b>:<br>
 * The standard separator to work with paths is the slash. It is used operation-system-independently, it means
 * in windows the slash is used too. But the file may be stored using the backslash. Therefore the user
 * should convert namepart.replace('\\','/') before the name part is processed anywhere. 
 *  
 * @author Hartmut Schorrig
 * @deprecated new concept is {@link CmdGetterArguments}. This class is too restricted.
 *
 */
public interface CmdGetFileArgs_ifc
{
  /**Invokes the preparation of the file situation to get the files after them.
   * That should be invoked firstly before get...().
   */
  void prepareFileSelection();
  /**Gets a file which is the first one. */ 

  /**Gets the selected file. */
  File getFileSelect();
  
  File getFile1();
  
  /**Gets a file which is the second one. */ 
  File getFile2();
  
  /**Gets a file which is the third one. */ 
  File getFile3();

}
