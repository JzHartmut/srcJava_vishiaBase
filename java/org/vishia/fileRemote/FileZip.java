package org.vishia.fileRemote;

import java.io.File;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.vishia.util.TreeNodeBase;
import org.vishia.util.TreeNodeBase.TreeNode;

/**
 * Entry in a zip file which can use as a FileRemote.
 * 
 * See {@link FileAccessZip}.
 * 
 * @author Hartmut Schorrig
 * 
 */
public class FileZip extends FileRemote {
  private static final long serialVersionUID = 8789821733982259444L;

  final FileRemote theFile;

  /** The ZipFile data. java.util.zip */
  final ZipFile zipFile;

  /** The entry of the file in the zip file. java.util.zip */
  final ZipEntry zipEntry;

  /** The path of the file inside the zip file. */
  final String sPathZip;

  // final List<ZipEntry> entries = new LinkedList<ZipEntry>();

  /**
   * All files which are contained in that directory if it is a directory entry
   * in the zip file or if it is the top node in zipfile. This aggregation is
   * null, if this instance represents only a file entry in the zip file (a leaf
   * in tree).
   */
  TreeNodeBase.TreeNode<FileZip> children;

  public FileZip(FileRemote parent) {
    super(parent.itsCluster, parent.device, null, parent.getName(), 0, 0, 0, 0, 0, null, true);
    //super(parent.getName());
    this.theFile = parent;
    ZipFile zipFile = null;
    children = new TreeNodeBase.TreeNode<FileZip>("/", this);
    try {
      zipFile = new ZipFile(parent);
    } catch (Exception exc) {
      zipFile = null;
    }
    this.zipFile = zipFile;
    this.zipEntry = null;
    this.sPathZip = "";
    Enumeration<? extends ZipEntry> entries = zipFile.entries();
    while (entries.hasMoreElements()) {
      ZipEntry entry = entries.nextElement();
      String sPathEntry = entry.getName();
      int sep = sPathEntry.lastIndexOf('/');
      if (sep >= 0) {
        String sDir = sPathEntry.substring(0, sep);
        String sName = sPathEntry.substring(sep + 1);
        if (sName.length() > 0) {
          TreeNodeBase.TreeNode<FileZip> dir = children.getOrCreateNode(sDir, "/");
          FileZip child = new FileZip(theFile, zipFile, entry);
          dir.addNode(sName, child);
        } else {
          // a directory entry found, it ends with '/'
          sep = sDir.lastIndexOf('/');
          TreeNodeBase.TreeNode<FileZip> parentDir;
          if (sep >= 0) {
            parentDir = children.getOrCreateNode(sDir, "/");
          } else {
            parentDir = children;
          }
          FileZip zipDir = new FileZip(parentDir.data, theFile, zipFile, entry);
        }
      }
      // this.entries.add(entry);
    }
  }

  public FileZip(FileZip parent, FileRemote theFile, ZipFile zipFile, ZipEntry zipEntry) {
    super(parent.itsCluster, parent.device, parent, /*zipFile.getName() + '/' +*/ zipEntry.getName(), zipEntry.getSize()
        , zipEntry.getTime(), 0, 0, 0, null, true); 
    this.sPathZip = zipEntry.getName();
    this.theFile = theFile;
    String sEntryPath = zipEntry.getName();
    int p1, p2 = sEntryPath.length();
    if(sEntryPath.endsWith("/")){
      p2 -=1;
    }
    p1 = sEntryPath.lastIndexOf('/', p2-1);  //maybe -1, then string from 0
    String sEntryName = sEntryPath.substring(p1+1, p2);
    this.children = new TreeNodeBase.TreeNode<FileZip>(sEntryName, this);
    parent.children.addNode(this.children);
    this.zipFile = zipFile;
    this.zipEntry = zipEntry;
  }

  public FileZip(FileRemote theFile, ZipFile zipFile, ZipEntry zipEntry) {
    super(theFile.itsCluster, theFile.device, null, zipFile.getName() + '/' + zipEntry.getName(), zipEntry.getSize()
        , zipEntry.getTime(), 0, 0, 0, null, true); 
    this.sPathZip = zipEntry.getName();
    this.theFile = theFile;
    this.children = null;
    this.zipFile = zipFile;
    this.zipEntry = zipEntry;
  }


  @Override
  public FileZip[] listFiles() {
    int zChildren = children == null ? 0 : children.nrofChildren()
        + (children.leafData == null ? 0 : children.leafData.size());
    if (zChildren > 0) {
      int ii = -1;
      FileZip[] ret = new FileZip[zChildren];
      if (children.hasChildren())
        for (TreeNodeBase.TreeNode<FileZip> node1 : children.iterator()) {
          ret[++ii] = node1.data;
        }
      if (children.leafData != null)
        for (FileZip node1 : children.leafData) {
          ret[++ii] = node1;
        }
      return ret;
    } else {
      return null;
    }
  }

  @Override
  public boolean isDirectory() {
    return children != null && children.hasChildren();
  }

  @Override
  public boolean exists() {
    return true;
  }

  @Override
  public String getName() {
    if (zipEntry != null) {
      return zipEntry.getName();
    } else {
      return theFile.getName();
    }
  }

  @Override
  public FileRemote getParentFile() {
    FileRemote parent1;
    if (children != null && children.parent() != null) {
      parent1 = children.parent().data;
    } else {
      parent1 = theFile;
    }
    return parent1;
  }

  @Override
  public String getParent() {
    File parent1 = getParentFile();
    return parent1.getAbsolutePath();
  }

  @Override
  public String toString() {
    return sPathZip;
  }

  /**
   * Only for test.
   * 
   * @param args
   */
  public static void main(String[] args) {
    FileCluster fileCluster = FileRemote.clusterOfApplication;
    FileRemote file = fileCluster.getDir("/home/hartmut/vishia/Java/srcJava_Zbnf.zip");
    FileZip fileZip = new FileZip(file);
    //TreeNodeBase.TreeNode<FileZip> test = new TreeNodeBase.TreeNode<FileZip>(fileZip.children, "", null);
    // boolean x = test.isDirectory();
    // boolean y = test instanceof FileZip;
    fileZip.listFiles();
  }

}
