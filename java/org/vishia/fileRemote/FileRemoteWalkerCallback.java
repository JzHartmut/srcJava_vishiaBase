package org.vishia.fileRemote;

import java.nio.file.Path;

import org.vishia.util.SortedTreeWalkerCallback;


/**This interface is used as callback for 
 * {@link FileRemoteAccessor#walkFileTreeCheck(FileRemote, boolean, boolean, boolean, String, long, int, FileRemoteCallback)}
 * It is similar like the concept of {@link java.nio.file.Files#walkFileTree(java.nio.file.Path, java.nio.file.FileVisitor)}
 * with its visitor interface. But it is implemented for Java6-usage too, usable as common approach.
 */
public interface FileRemoteWalkerCallback extends SortedTreeWalkerCallback<FileRemote, FileRemote.CmdEventData> {
}
