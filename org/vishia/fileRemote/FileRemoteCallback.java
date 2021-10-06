package org.vishia.fileRemote;

import java.io.FileFilter;

import org.vishia.util.SortedTreeWalkerCallback;


/**This interface is used as callback for {@link FileRemoteAccessor#getChildren(FileRemote, FileFilter)}
 * It is similar like the concept of {@link java.nio.file.Files#walkFileTree(java.nio.file.Path, java.nio.file.FileVisitor)}
 * with its visitor interface. But it is implemented for Java6-usage too.
 */
public interface FileRemoteCallback extends SortedTreeWalkerCallback<FileRemote>
{
  
}
