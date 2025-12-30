package org.vishia.fileRemote;

import java.io.File;
import java.io.IOException;
import java.io.Writer;

import org.vishia.util.Debugutil;
import org.vishia.util.OutTextPreparer;

/**This class helps especially for debugging situations to report the tree of FileRemote.
 * 
 */
public class FileRemoteReport {

  
  public static String newline = "\n | | | | | | | | | | | | | | | | | | | | | | | | | | | | | | ";
  
  static final OutTextPreparer gTxtFile = new org.vishia.util.OutTextPreparer("fTxtFile", null, "file"
  , "<&file.sFile><:tab:20><&file.flags:%8X><:if:file.mark> mark:<&file.mark.selectMask:%8X><.if>");
  
  
  final OutTextPreparer.DataTextPreparer dFile = gTxtFile.getArgumentData(this);
  
  public void showFile (Appendable out, FileRemote fr, int deepn) throws IOException {
    if((fr.flags & FileRemote.mRefreshChildPending) !=0) {
      out.append('?');
    }
    this.dFile.setArgument("file", fr);
    this.dFile.exec(out);
  }
  
  
  public void showDir (Appendable out, FileRemote dir, int deepn) throws IOException {
    int deepn2 = 2*deepn+2;
    if(deepn2 > newline.length()){
      deepn2 = newline.length();
      Debugutil.stopp();
    }
    out.append(newline.substring(0, 2* deepn)).append("+-");
    showFile(out, dir, deepn);
    if(dir.children() !=null) for(FileRemote child: dir.children().values()) {
      if(child.isDirectory()) {
        showDir(out, child, deepn+1);
      } else {
        out.append(newline.substring(0, 2* deepn +2)).append("+-");
        showFile(out, child, deepn);
      }
    }
  }
  
  public void showTree (File fOut, FileRemote dir) {
    try (
      Writer writer = new java.io.FileWriter(fOut)) {
      showDir(writer, dir, 0);
      writer.close();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
  
}
