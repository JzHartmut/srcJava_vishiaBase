package org.vishia.cmd;

import java.io.File;

/**Gets file arguments from the environment to build a command line.
 * @author Hartmut Schorrig
 *
 */
public interface CmdGetFileArgs_ifc
{
  /**Gets a file which is the first one. */ 
  File getFile1();
  
  /**Gets a file which is the second one. */ 
  File getFile2();
  
  /**Gets a file which is the third one. */ 
  File getFile3();
  
  /**Gets the selected file. */
  File getFileSelect();
  
  void prepareFileSelection();
}
