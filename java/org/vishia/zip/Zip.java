package org.vishia.zip;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.vishia.util.Arguments;
import org.vishia.util.FileFunctions;


//import org.vishia.mainCmd.MainCmd;
//import org.vishia.mainCmd.MainCmdLogging_ifc;

//import org.apache.tools.zip.ZipEntry;

/**This class supports creating a jar file and working with zip files using the standard java zip methods.
 * This class supports a base path and wildcards like described 
 * in {@link org.vishia.util.FileFunctions#addFilesWithBasePath(File, String, List, boolean)}.
 * <br><br>
 * Usage template:
 * <pre>
    Zip zip = new Zip();   //instance can be re-used in the same thread.
    zip.addSource(directory, path);
    zip.addSource(directory2, path2);
    String sError = zip.exec(dst, compressionLevel, comment);
    //the added sources are processed and removed.
    //next usage:
    zip.addSource(directory, path);
    String sError = zip.exec(dst, compressionLevel, comment);
 * </pre>
 * @author Hartmut Schorrig
 *
 *
 */
public class Zip {

  
  /**Version, history and license.
   * <ul>
   * <li>2020-03-20 Hartmut improvements and extensions for jar, especially sort and time stamp 
   * <li>2014-06-15 Hartmut new: Supports jar 
   * <li>2013-02-09 Hartmut chg: {@link Cmdline#argList} now handled in {@link MainCmd#setArguments(org.vishia.mainCmd.MainCmd.Argument[])}.
   * <li>2013-01-20 Hartmut creation: New idea to use the zip facility of Java.
   * </ul>
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
   */
  public final static String version = "2020-03-20";

  
  
  
  static class Src { 
    String path; File dir;
    Src(String path, File dir) { this.path = path;  this.dir = dir; } 
  }
  
  private final List<Src> listSrc;
  
  
  /**A manifest file. If it is set then a jar file will be created instead zip.
   * 
   */
  private Manifest manifest;
  
  private boolean bsort;
  
  /**Creates an empty instance for Zip.
   * One should call {@link #addSource(String)} or {@link #addSource(File, String)}
   * and then {@link #exec(File, int, String)} to create a Zipfile. 
   * <br><br>
   * The instance can be reused: After exec(...) the added sources are removed. call
   * {@link #addSource(String)} and {@link #exec(File, int, String)} with other files.
   * <br><br>
   * The instance should not be used in multithreading uncoordinated.
   */
  public Zip(){
    this.listSrc = new LinkedList<Src>();
  }
  
  /**Creates an instance for Zip with given sources. It is used especially in {@link #main(String[])}.
   * One can {@link #addSource(String)} with additional sources. The invoke {@link #exec(File, int, String)}
   * @param listSrc List of sources.
   */
  //public Zip(List<Src> listSrc){
  //  this.listSrc = listSrc;
  //}
  
  
  /**Adds a source file or some files designated with wildcards
   * @param src Path may contain the basebase:localpath separatet with ':'. The localpath is used inside the zip file
   *   as file tree. The localpath may contain wildcards. The basepath may be an absolute path or it is located
   *   in the systems current directory..
   *   If the sPath does not contain a basepath (especially it is a simple path to a file), this path is used in the zipfile.
   *   Especially the path can start from the current directory.
   *   For usage of basepath, localpath and wildcards see {@link org.vishia.util.FileFunctions#addFilesWithBasePath(File, String, List, boolean)}.
   */
  public void addSource(String src){
    this.listSrc.add(new Src(src, null));
  }
  

  
  /**Adds a source file or some files designated with wildcards
   * @param dir the directory where the source path starts.
   * @param src Path may contain a basebase:localpath separatet with ':'. The localpath is used inside the zip file
   *   as file tree. The localpath may contain wildcards.
   *   If the sPath does not contain a basepath (especially it is a simple path to a file), this path is used in the zipfile.
   *   For usage of basepath, localpath and wildcards see {@link org.vishia.util.FileFunctions#addFilesWithBasePath(File, String, List, boolean)}.
   */
  public void addSource(File dir, String src){
    this.listSrc.add(new Src(src, dir));
  }
  

  
  /**Sets a manifest information from a given file.
   * If this routine are invoked before {@link #exec(File, int, String)}, a jar file will be created
   * with this manifest. If the fileManifest does not contain a version info, the {@link Attributes.Name#MANIFEST_VERSION}
   * with the value "1.0" will be written.
   * 
   * @param fileManifest a textual manifest file.
   * 
   * @throws IOException file not found, any file error.
   */
  public void setManifest(File fileManifest) 
  throws IOException
  { 
    this.manifest = new Manifest();
    //manifest.  
    InputStream in = null;
    try{ 
      File fileManifestAbs = fileManifest.getAbsoluteFile();
      //Note: bug detected Java 8.241: 
      //After System.setProperty("user.dir", cd); 
      //    InputfileStream(relPath) does not recognize the changed curr dir.
      in = new FileInputStream(fileManifestAbs);  
      this.manifest.read(in);
    }
    finally { 
      if(in !=null) { 
        in.close(); 
    } }
    //String vername = Attributes.Name.MANIFEST_VERSION.toString();
    Attributes attr = this.manifest.getMainAttributes();  //read from file
    String version1 = attr.getValue(Attributes.Name.MANIFEST_VERSION);
    if(version1 == null){
      attr.put(Attributes.Name.MANIFEST_VERSION, "1.0");
    }
  }
  
  
  
  /**Executes the creation of zip or jar with the given source files to a dst file.
   * If {@link #setManifest(File)} was invoked before, a jar file will be created.
   * All source files should be set with {@link #addSource(File, String)} or {@link #addSource(String)}.
   * 
   * @param fileZip The destination file.
   * @param compressionLevel Level from 0..9 for compression
   * @param comment in the zip file.
   * @throws IOException on any file error. 
   *   Note: If a given source file was not found, it was ignored but write as return hint.
   * @return an error hint of file errors or null if successful.
   */
  public String exec(File fileZip, int compressionLevel, String comment, long timestamp, boolean bFollowSymbLinks)
  throws IOException
  { StringBuilder errorFiles = null;
    final byte[] buffer = new byte[0x4000];
    ZipOutputStream outZip = null;
    FileOutputStream outstream = null;
    try {
      outstream = new FileOutputStream(fileZip);
      if(this.manifest != null){
        if(timestamp !=0) {
          //jar without manifest
          outZip = new JarOutputStream(outstream);
          //but add the manifest here, with given timestamp:
          ZipEntry e = new ZipEntry(JarFile.MANIFEST_NAME);
          e.setTime(timestamp);
          outZip.putNextEntry(e);
          this.manifest.write(outZip); //new BufferedOutputStream(outZip));
          outZip.closeEntry();
          System.out.println("jar-file with timestamp ");
        } else {
          outZip = new JarOutputStream(outstream, this.manifest);
          System.out.println("jar-file with current file time ");
        }
      } else {
        outZip = new ZipOutputStream(outstream);
      }
      //outZip.setComment(comment);
      outZip.setLevel(compressionLevel);
      
      
      //get the files.
      List<FileFunctions.FileAndBasePath> listFiles= new ArrayList<FileFunctions.FileAndBasePath>();
      for(Src src: this.listSrc){
        String path = src.path;
        if(path.startsWith("/tmp/")) {
          String sTmp = System.getenv("TMP");  //found on windows
          if(sTmp !=null) {
            path = sTmp + path.substring(4);  //use the $TMP instead /tmp
          }
        }
        System.out.println("  + " /*+ src.dir.getAbsoluteFile() */+ " : " + path);
        FileFunctions.addFilesWithBasePath (src.dir, path, listFiles, bFollowSymbLinks);  //Note: src.dir = null always, path contains :
      }
      if(this.bsort) {
        Map<String, FileFunctions.FileAndBasePath> idxSrc = new TreeMap<String, FileFunctions.FileAndBasePath>();
        for(FileFunctions.FileAndBasePath src: listFiles) {
          idxSrc.put(src.localPath, src);
        }
        listFiles.clear();
        for(Map.Entry<String, FileFunctions.FileAndBasePath> e: idxSrc.entrySet()) {
          listFiles.add(e.getValue());
        }
      }
      //      
      System.out.println(" files:" + listFiles.size());
      for(FileFunctions.FileAndBasePath filentry: listFiles){
        if(filentry.file.isFile()){
          ZipEntry zipEntry = null;
          InputStream in = null;
          String sPath = filentry.localPath;
          if(sPath.startsWith("/")){    //The entries in zip/jar must not start with /
            sPath = sPath.substring(1);
          }
          try{
            if(manifest !=null){
              zipEntry = new JarEntry(sPath);
            } else {
              zipEntry = new ZipEntry(sPath);
            }
            zipEntry.setTime(timestamp == 0 ? filentry.file.lastModified(): timestamp);
            outZip.putNextEntry(zipEntry);
            in = new FileInputStream(filentry.file);
            int bytes;
            while( (bytes = in.read(buffer))>0){
              outZip.write(buffer, 0, bytes);
            }
          } catch(IOException exc){
            if(errorFiles == null) { errorFiles = new StringBuilder(); }
            errorFiles.append(exc.getMessage()).append("\n");
          } finally {
            if(in !=null) { in.close(); }
            if(zipEntry !=null) { outZip.closeEntry(); }
          }
        } else {
          //directory is written in zip already by filentry.localPath
        }
      }
      outZip.close();
    } finally {
      try{ if(outZip !=null) outZip.close();
      } catch(IOException exc){ throw new RuntimeException("Archiver.createJar - unexpected IOException"); }
      try{ if(outstream !=null) outstream.close();
      } catch(IOException exc){ throw new RuntimeException("Archiver.createJar - unexpected IOException"); }
      
    }
    this.listSrc.clear();
    if(errorFiles == null) return null;
    else return errorFiles.toString();
  }
  
  
  
  
  public static void unzip(InputStream in) throws IOException {
    ZipInputStream zipInput = new ZipInputStream(in);
    ZipEntry entry;
    while( (entry = zipInput.getNextEntry()) !=null) {
      System.out.println(entry.getName());
    }
  }
  
  
  
  /**Executes Zip with given arguments.
   * Note: One can have add sources calling {@link #addSource(String)} before additional to the sources in add.
   * But usual the sources should be given only in args.
   * <br><br>
   * Invoke:
   * <pre>
   * Zip zip = new Zip();
   * Zip.Args args = .... get args from somewhere
   * zip.exec(args);
   * @param args
   * @return
   * @throws IOException 
   */
  public String exec(Args args) throws IOException{
    if(args.sManifest !=null) {
      this.setManifest(new File(args.sManifest));
    }
    this.bsort = args.sortFiles;
    this.listSrc.addAll(args.listSrc);
    long timestamp = 0;
    if(args.timestamp !=null) {
      System.out.println("org.vishia.util.Zip: timestamp = " + args.timestamp + " (" + args.timeFormat + ")");
      SimpleDateFormat df = new SimpleDateFormat(args.timeFormat);
      try {
        String timestampGmt = args.timestamp;
        if(args.timeFormat.contains("z") && !timestampGmt.contains("GMT")) {
          timestampGmt += " GMT";  //recommended: Times are always given in GMT without extra designation
        }
        Date dd = df.parse(timestampGmt);
        timestamp = dd.getTime();
      } catch (ParseException e) {
        System.err.println("org.vishia.util.Zip: faulty format for -time:" + args.timestamp + " - uses the current file time stamp");
      }
    }
    System.out.println("org.vishia.util.Zip: write zip file to " + args.fOut);
    return exec(args.fOut, args.compress, args.comment, timestamp, args.bFollowSymbLinks);
  }
  
  
  
  /**Zips some files in a dst file.
   * @param dst The destination file.
   * @param sPath Path should contain the basebase:localpath separatet with ':'. The localpath is used inside the zip file
   *   as file tree. For usage of basepath, localpath see {@link org.vishia.util.FileFunctions#addFilesWithBasePath(File, String, List, boolean)}.
   * @param compressionLevel Level from 0..9 for compression
   * @param comment in the zip file.
   * @param bNotFollowSymbLInks true then exclude symbolic linked directories and Junctions in Windows
   * @return an error hint or null if successful.
   * @throws IOException 
   */
  public static String zipfiles(File dst, File srcdir, String sPath, int compressionLevel, String comment, boolean bNotFollowSymbLInks) throws IOException{
    Zip zip = new Zip();
    zip.addSource(sPath);
    return zip.exec(dst, compressionLevel, comment, 0, bNotFollowSymbLInks); 
  }

  
  
  /**Zips some files in a dst file, also following symbolic links. (compatible to older behavior).
   * @param dst The destination file.
   * @param sPath Path should contain the basebase:localpath separatet with ':'. The localpath is used inside the zip file
   *   as file tree. For usage of basepath, localpath see {@link org.vishia.util.FileFunctions#addFilesWithBasePath(File, String, List, boolean)}.
   * @param compressionLevel Level from 0..9 for compression
   * @param comment in the zip file.
   * @return an error hint or null if successful.
   * @throws IOException 
   */
  public static String zipfiles(File dst, File srcdir, String sPath, int compressionLevel, String comment) throws IOException{
    Zip zip = new Zip();
    zip.addSource(sPath);
    return zip.exec(dst, compressionLevel, comment, 0, true); 
  }

  
  
  /**Invocation from java command line
   * Zip routine from Java made by HSchorrig, 2013-02-09 - 2020-06-09
   * <ul>
   * <li>obligate arguments: -o:ZIP.zip { INPUT}
   * <li>-compress:0..9 set the compression rate 0=non .. 9=max
   * <li>-o:ZIP.zip file path for zip output
   * <li>-sort sorts entries with path
   * <li>-time:yyyy-MM-dd+hh:mm sets a timestamp for all entries in UTC (GMT)
   * <li>-timeformat:yyyy-MM-dd+hh:mm is default, can define other format, see java.text.SimpleDataFormat
   * <li>-manifest:<manifestfile> if given creates a jar file
   * <li>INPUT file possible with localpath-separation and wildcards as "path:** /dir* /name*.ext*"
   * <li>For possibilities of the inputpath see {@link FileFunctions#addFilesWithBasePath(File, String, List, boolean)}
   * </ul>
   * @param args command line arguments
   */
  public static void main(String[] args){
    int exitCode = smain(args);
    System.exit(exitCode);
  }
  
  
  
  
  
  /**Main routine able to call inside another java process without exit VM.
   * @param args cmd line args adequate {@link #main(String[])}
   * @return exit code
   */
  public static int smain(String[] args){
    Args argData = new Args();
    System.out.println(argData.aboutInfo());
    try{ 
      for(String arg: args) { System.out.println(arg); }
      argData.parseArgs(args, System.err);
      if(!argData.testConsistence(System.err)) { return 1; }
    } catch(Exception exc){
      System.err.println(exc.getMessage());
      return argData.exitCodeArgError;
    }
    Zip zip = new Zip();
    try{ 
      zip.exec(argData);
    } catch(IOException exc){
      System.err.println(exc.getMessage());
    }
    return 0;
  }
  
  
  
  
  /**This class holds arguments to zip.
   */
  public static class Args extends Arguments {

    public final List<Src> listSrc = new ArrayList<Src>();
    
    boolean bFollowSymbLinks = true;  // compatible old behavior is default.

    public int compress = 5;
    
    public File fOut;
    
    public String comment = "";
    
    public boolean sortFiles;
    
    public String sManifest;

    public String timestamp;
    
    public String timeFormat = "yyyy-MM-dd+hh:mm z";
    
      
    Arguments.SetArgument setCompress = new Arguments.SetArgument(){ @Override public boolean setArgument(String val){ 
      char cc;
      if(val.length()== 1 && (cc=val.charAt(0))>='0' && cc <='9'){
        Args.this.compress = cc-'0';
        return true;
      } else return false;
    }};
    
    Arguments.SetArgument setInput = new Arguments.SetArgument(){ @Override public boolean setArgument(String val){ 
      Args.this.listSrc.add(new Src(val, null));
      return true;
    }};
    
    Arguments.SetArgument setOutput = new Arguments.SetArgument(){ @Override public boolean setArgument(String val){ 
      Args.this.fOut = new File(val);
      return true;
    }};
    
    Arguments.SetArgument setManifest = new Arguments.SetArgument(){ @Override public boolean setArgument(String val){ 
      Args.this.sManifest = val;
      return true;
    }};
    
    Arguments.SetArgument setTimestamp = new Arguments.SetArgument(){ @Override public boolean setArgument(String val){ 
      Args.this.timestamp = val;
      return true;
    }};
    
    
    
    Arguments.SetArgument setNotFollowSymbLinks = new Arguments.SetArgument(){ @Override public boolean setArgument(String val){ 
      Args.this.bFollowSymbLinks = false;
      return true;
    }};
    
    
    Arguments.SetArgument sort = new Arguments.SetArgument(){ @Override public boolean setArgument(String val){ 
      Args.this.sortFiles = true;
      return true;
    }};
    

    
    
    Args(){
      super.aboutInfo = "Zip routine from Java made by HSchorrig, 2013-02-09 - 2020-06-09";
      super.helpInfo="obligate args: -o:ZIP.zip { INPUT}";  //[-w[+|-|0]]
      addArg(new Argument("-compress", ":0..9 set the compression rate 0=non .. 90max", this.setCompress));
      addArg(new Argument("-o", ":ZIP.zip file for zip output", this.setOutput));
      addArg(new Argument("-sort", " sorts entries with path", this.sort));
      addArg(new Argument("-time", ":yyyy-MM-dd+hh:mm sets a timestamp in UTC (GMT)", this.setTimestamp));
      addArg(new Argument("-timeformat", ":yyyy-MM-dd+hh:mm is default, can define other format, see java.text.SimpleDataFormat", this.setTimestamp));
      addArg(new Argument("-manifest", ":<manifestfile> creates a jar file", this.setManifest));
      addArg(new Argument("-noSymbLinks", "do not follow symbolik links and JUNCTION in Windows", this.setNotFollowSymbLinks));
      addArg(new Argument("", "INPUT file possible with localpath-separation and wildcards as \"path:**/dir*/name*.ext*\"", this.setInput));
      
    }




    @Override
    public boolean testConsistence(Appendable msg) throws IOException {
      boolean bOk = true;
      if(this.fOut == null) { msg.append("-o:ZIP.zip is obligate\n"); bOk = false; }
      if(this.listSrc.size() == 0) { msg.append("warning: no input files given\n"); }
      if(!bOk) {
        super.showHelp(msg);
      }
      return bOk;
    }
    
    
    
    
    
  }
  
  
  
  
  
}
