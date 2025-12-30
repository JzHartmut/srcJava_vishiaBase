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

  
  /**Version, history and license.
   * <ul>
   * <li>20xx-xx-xx Hartmut creation.  
   * </ul>
   *
   * 
   * <b>Copyright/Copyleft</b>:
   * For this source the LGPL Lesser General Public License,
   * published by the Free Software Foundation is valid.
   * It means:
   * <ol>
   * <li> You can use this source without any restriction for any desired purpose.
   * <li> You can redistribute copies of this source to everybody.
   * <li> Every user of this source, also the user of redistribute copies
   *    with or without payment, must accept this license for further using.
   * <li> But the LPGL ist not appropriate for a whole software product,
   *    if this source is only a part of them. It means, the user
   *    must publish this part of source,
   *    but don't need to publish the whole source of the own product.
   * <li> You can study and modify (improve) this source
   *    for own using or for redistribution, but you have to license the
   *    modified sources likewise under this LGPL Lesser General Public License.
   *    You mustn't delete this Copyright/Copyleft inscription in this source file.
   * </ol>
   * If you are intent to use this sources without publishing its usage, you can get
   * a second license subscribing a special contract with the author. 
   * 
   * @author Hartmut Schorrig = hartmut.schorrig@vishia.de
   * 
   * 
   */
  //@SuppressWarnings("hiding")
  public final static String version = "2024-09-20";

  
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
  
  
  
  /**Read the requested file from zip .
   * @param sfileZip (absolute) path to the zip file
   * @param sFileDst (absolute) path to the file to write from zip content
   * @param pathInZip path in zip from root, not starting with "/"
   * @throws IOException
   */
  public static void copyFileFromZip(String sfileZip, String sFileDst, String pathInZip) throws IOException {
    Path fPathDst = Paths.get(sFileDst);
    Path zipFilePath = Paths.get(sfileZip);
    FileSystem fs = FileSystems.newFileSystem(zipFilePath, (ClassLoader)null);
    Path fileInsideZipPath = fs.getPath("/" + pathInZip);
    if(Files.exists(fPathDst)) {
      Files.delete(fPathDst);
    }
    Files.copy(fileInsideZipPath, fPathDst);
    fs.close();
  }
  

  
  
  /**Read the requested file from zip .
   * @param fileZip zip file
   * @param fileDst file to write from zip content
   * @param pathInZip path in zip from root, not starting with "/"
   * @return null or error message on exception. Do not throw the exception.
   */
  public static String copyFileFromZip(File fileZip, File fileDst, String pathInZip) {
    try { copyFileFromZip(fileZip.getAbsolutePath(), fileDst.getAbsolutePath(), pathInZip);
    
    } catch(IOException exc) {
      return exc.getMessage();
    }
    return null;
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
    FileSystem fs = FileSystems.newFileSystem(zipFilePath, (ClassLoader)null);
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
