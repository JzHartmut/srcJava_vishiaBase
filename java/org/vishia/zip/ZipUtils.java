package org.vishia.zip;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipUtils {

  
  
  
  /**This creates a new Zip with only the one entry, removes all existing content. 
   * Not usable.
   * @param fileZip
   * @param sPathInZip
   * @return
   * @throws IOException
   */
  public static OutputStream openToZipOneFile(File fileZip, String sPathInZip) throws IOException {
    ZipOutputStream outZip = null;
    FileOutputStream outstream = null;
//    try {
      outstream = new FileOutputStream(fileZip);
      outZip = new ZipOutputStream(outstream);
      ZipEntry zipEntry = new ZipEntry(sPathInZip);
      outZip.putNextEntry(zipEntry);
      
//    } finally {
//      try{ if(outZip !=null) outZip.close();
//      } catch(IOException exc){ throw new RuntimeException("Archiver.createJar - unexpected IOException"); }
//      try{ if(outstream !=null) outstream.close();
//      } catch(IOException exc){ throw new RuntimeException("Archiver.createJar - unexpected IOException"); }
//      
//    }
    return outZip;
  }
  
  
  
  //@SuppressWarnings("resource") 
  public static FileSystem openZip(File fileZip) throws IOException {
    Map<String, String> env = new HashMap<>();
    env.put("create", "true");
    Path path = Paths.get(fileZip.getAbsolutePath());
    URI uri = URI.create("jar:" + path.toUri());
    FileSystem fs = FileSystems.newFileSystem(uri, env);
    return fs;
  }  
  
  
  
  /**Replaces the given file in zip with the given, or copies the given.
   * @param sfileZip (absolute) path to the zip file
   * @param snewFile (absolute) path to the file which replaces
   * @param pathInZip path in zip from root, not starting with "/"
   * @throws IOException
   */
  public static void replaceFile(String sfileZip, String snewFile, String pathInZip) throws IOException {
    Path fPath = Paths.get(snewFile);
    Path zipFilePath = Paths.get(sfileZip);
    FileSystem fs = FileSystems.newFileSystem(zipFilePath, null);
    Path fileInsideZipPath = fs.getPath("/" + pathInZip);
    if(Files.exists(fileInsideZipPath)) {
      Files.delete(fileInsideZipPath);
    }
    Files.copy(fPath, fileInsideZipPath);
    fs.close();
  }
  
  
  
  @SuppressWarnings("resource") 
  public static BufferedWriter openZipEntryToWrite(File fileZip, String sPathInZip, Charset encoding) throws IOException {
    Charset encoding1 = encoding != null ? encoding : Charset.forName("UTF-8");
    Map<String, String> env = new HashMap<>();
    env.put("create", "true");
  
    Path path = Paths.get(fileZip.getAbsolutePath());
    URI uri = URI.create("jar:" + path.toUri());
  
    FileSystem fs = FileSystems.newFileSystem(uri, env);
    Path nf = fs.getPath(sPathInZip);
    return Files.newBufferedWriter(nf, encoding1);
  }  
  
  
  public static boolean closeZipEntry(OutputStream out) throws IOException {
    ZipOutputStream outZip = ((ZipOutputStream)out);
    outZip.closeEntry();
    outZip.close();
    return true;
  }
  
  
}
