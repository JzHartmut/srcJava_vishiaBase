package org.vishia.ant;


//import org.apache.tools.ant.*;
//import java.util.*;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.Path;
//import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.taskdefs.condition.FilesMatch;
import org.apache.tools.ant.util.FileUtils;
import org.vishia.util.FileSystem;
//import org.apache.tools.ant.util.FileUtils;

import java.io.File;
//import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

//diff file generation
//import bmsi.util.DiffPrint;
//import java.io.PrintWriter;
//import java.io.StringWriter;
//import java.io.FileWriter;


/**Zcopy is useable as ANT Task and tests and copies with modifications. 
 * It is compareable to ANT: &lt;copy...&gt; but is enhanced.
 * ANT arguments:<ul>
 * <li>attribute file: a source file</li>
 * <li>attribute tofile: a single destination file</li>
 * <li>attribute testContent: If this attribute is set, the copy is executed only if the content is different. 
 * <li>It tests the content, it doesn't tests the timestamp. 
 * <br>Possible values:
 *   <ul><li>bytewise: tests byte for byte completely</li>
 *   <ul><li>ignoreNewlineCode: tests textline per textline, all newline character sequences 0d, 0a , 0d0a are accepted as same.</li>
 *   </ul>
 * </ul>
 * It tests the file content independently to its timestamp using org.apache.tools.ant.taskdefs.condition.FilesMatch.
 *   
 */
public class Zcopy extends Task {
    
  //private Vector<Path> src_files = new Vector<Path>();
    
  /**set with setDir(File). */
  File srcDir = null;
  
  /**set with setFile. It may be a mask with wildcards such as "folder/** /folder* /file*.*"
   * TODO only simple variant realized yet: folder/folder/file*.ext oder ...file*.*
   */
  Vector<String> srcFileMask = new Vector<String>();
  
  //private String destPath = null;
  
  /**set with setTodir(File). */
  String sDstDir = null;
  
  
  @SuppressWarnings("unused")
  private boolean bTestBytewise = false;
  
  
  @SuppressWarnings("unused")
  private boolean bIgnoreNewlineCode = false;
  
  private String sDstFile = null;
  
  private final FilesMatch fcompare = new FilesMatch();

  public void setFile(String str) //Path inputFile)
  { //src_files.add(inputFile);
    srcFileMask.add(str);
  }
  
  public void setDir(Path inputDir)
  { String[] sDirs = inputDir.list();
    if(sDirs.length > 0)
    { srcDir = new File(sDirs[0]);
    }
    else
    { System.out.println("Zcopy: @dir fault.");
    }
  }
  

  /**
   * Set the destination file.
   * @param destFile the file to copy to.
   */
  public void setTofile(String destFile) 
  {
    this.sDstFile = destFile;
  }

  /**
   * Set the destination directory.
   * @param destDir the destination directory.
   */
  public void setTodir(String destDir) 
  {
    this.sDstDir = destDir;
  }

  public void setTestcontent(String sKind)
  { if(sKind.equals("bytewise"))
    { bTestBytewise = true;
    }
    else if(sKind.equals("ignoreNewlineCode"))
    { bIgnoreNewlineCode = true;
    }
    
  }
  
  
  public void execute()
  {
    try
    { String sSrcDir = srcDir != null ? srcDir.getAbsolutePath() + "/" : "";
      int posSrcDir = sSrcDir.length()-1;  //may be negative if srcDir==null
      
      Iterator<String> iterSrcFiles = srcFileMask.iterator();  
      //System.out.println("CopyChangedFiles");
      while (iterSrcFiles.hasNext())
      {               
        //search in srclist
        //String[] src_includedFiles = path_src_file.list();
        
        //for(int i=0; i<src_includedFiles.length; i++) 
        { String sSrcfile = iterSrcFiles.next(); //src_includedFiles[i];
          System.out.println("Zcopy file=" + sSrcfile);
          if(sSrcfile.indexOf('*')>=0)
          { //contains wildcards
            List<File> srcFiles = new LinkedList<File>();
            if(srcDir != null)
            { FileSystem.addFileToList(srcDir, sSrcfile, srcFiles);
            }
            else
            { FileSystem.addFileToList(sSrcfile, srcFiles);
            }
            Iterator<File> iter = srcFiles.iterator();
            while(iter.hasNext())
            { File srcfile = iter.next();
              testAndCopy(srcfile, posSrcDir);          
            }  
          }
          else
          { File srcfile = new File(sSrcDir + sSrcfile);
            testAndCopy(srcfile, posSrcDir);
          }
        }//for
      }//while
    }
    catch(Exception exc)
    { System.out.println(exc.getMessage());
      exc.printStackTrace(System.out);
      System.exit(1);
    }
  }
    
    
  /**tests the content and copy files dependend on test result.
   * 
   * @param srcFile The srcFile, it should exists.
   * @param posSrcDir The position of local pathname inside srcFile to get the name of the dstFile.
   * @return true if copied.
   */  
  protected boolean testAndCopy(File srcFile, int posSrcDir)
  { File dstFile = null;
    //System.out.println("Zcopy: " + sDstFile + " := " + srcFile.getAbsolutePath());
    boolean bCopy = false;
    if(sDstFile != null)
    { dstFile = new File(sDstFile);
    }
    else
    { if(sDstDir == null)
      { throw new IllegalArgumentException("either attribute toFile or toDir should be given");
      }
      else
      { String sDstFile;
        if(posSrcDir >=0)
        { sDstFile = srcFile.getAbsolutePath().substring(posSrcDir);
        }
        else
        { sDstFile = " /" + srcFile.getName();
        }
        dstFile = new File(sDstDir + sDstFile);
      }
    }
    //System.out.println("Zcopy: " + dstFile.getAbsolutePath() + " := " + srcFile.getAbsolutePath());
    if(dstFile.exists())
    {
      //System.out.println("compare " + srcFile.getAbsolutePath() + " --- " + destFile.getAbsolutePath());
      //System.out.println("compare " + src_includedFiles[i] + " --- " + dstFile.getAbsolutePath());
      fcompare.setFile1(srcFile);
      fcompare.setFile2(dstFile);
      
      //if (myIO.fcompare(srcFile, destFile) == false)
      if (!fcompare.eval())
      { System.out.print("replace " + dstFile.getAbsolutePath() + "...");
        bCopy = true;
      }
    }//if destFile.exists
    else
    { System.out.print("create " + dstFile.getAbsolutePath() + "...");
      bCopy = true;
    }
    if(bCopy)
    { try 
      { //not available in JEE5: dstFile.setWritable(true);
        FileUtils.getFileUtils().copyFile(srcFile, dstFile);
        System.out.println("done");
      } 
      catch(Exception e) 
      { //System.out.println( e );
        System.out.println("ERROR");
      }
    }  
    return true;
  }
    
    
    
   /* 
   public String FindDestFile(String file, Vector<Path> dest_files){
 
    //   System.out.println(file);
    //   System.out.println("public String FindDestFile(String file)");
       String result = "";
     //  String FileName  = file.substring(file.lastIndexOf("/")+1, file.lastIndexOf("."));
     //  String FileExtension = file.substring(file.lastIndexOf(".")+1); 
       
       // find all files
       for(Iterator<Path> itPaths = dest_files.iterator(); itPaths.hasNext(); ) {
           Path path = (Path)itPaths.next();
           String[] includedFiles = path.list();
           for(int i=0; i<includedFiles.length; i++) {
               String filename = includedFiles[i].replace('\\','/');
               filename = filename.substring(filename.lastIndexOf("/")+1);
               //file - file to Find, "search*.bat"    
               
               if (file.equals(filename)) {
               //if (filename.matches(file) && !foundFiles.contains(includedFiles[i])) {  
                    result = includedFiles[i];
                    break;
                 //   foundFileNames.add( );
               }
           }
       }
     
       return result;
   }
   */
   
    public static void main(String args[])
    {
      Zcopy test = new Zcopy();
      /*
      test.setDir(new Path(test.getProject(), "d:/SBOX/_TRC_edit/AAXDDA/DataAcquire_TRC_RC/GenSrc"));
      test.setFile("pkgTRC/pkgTRCT_SCP_Adapter/*.*");
      test.setTodir("d:/SBOX/_TRC_edit/AAXDDA/DataAcquire_TRC_RC/GenSrcSave");
      */
      test.setDir(new Path(test.getProject(), "d:/vishia/Zmake_test/../Xml/docuSrc/"));
      test.setFile("UmlDocu.topic.txt");
      test.setTodir("d:/vishia/Zmake_test/test1");
      
      //Path path1 = new Path(test.getProject(), "myFile.h");
      //test.addSrc_files(path1); 

 //     Path path2 = new Path(test.getProject(), "D:\\SBOX\\_VFN\\OCA_OcsAdap_edit\\AAXRMM\\include\\pkgVFN\\pkgVFN_ParamVehFun_ifc\\OcsAdap_Param_ifcVFN.h");
 //     test.addDest_files(path2); 
//      test.includes = "";
      
      test.execute();
    }
    
}
