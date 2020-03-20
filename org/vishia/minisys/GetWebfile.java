package org.vishia.minisys;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


/**Reads a file from Web given as URL
 * @author Hartmut Schorrig
 * @since 2020-08-10, LPGL-License. Do not remove this license declaration.
 *
 */
public class GetWebfile {
  
  public static void main(String[] args) {
    int exitcode = 0;
    InputStream sin = null;
    FileOutputStream sout = null;
    if(args.length < 2) {
      exitcode = 4;
      System.out.println("org.vishia.minisys.GetWebfile <URL> <dst> [-md5:<hash>] [-strict]");
      System.out.println(" reads a file content from <URL> and writes it to <dst>");
      System.out.println(" <URL> is the same like a link in Web Browser to a file for download");
      System.out.println(" <dst> is a absolute or relative path written with / as separator.\n"
                       + "       if it ends with / then it is the directory for the named URL file.");
      System.out.println(" <hash> is the known and expected MD5-hash code of the downloaded file");
      System.out.println(" -strict then the file is not loaded if hash is faulty");
      System.out.println("exitcode 0: all ok, 2: hash faulty 5: parameter error");
      System.out.println(" Note: Use slash instead backslash on windows for file name too!");
      System.out.println(" Made by Hartmut Schorrig, www.vishia.org, 2020-03-15, LPGL-License");
    } else {
      String src = args[0];   //The URL
      String md5 = args.length >=3 && args[2].startsWith("-md5:") ? args[2].substring(5) : null;
      String dst1 = args[1]; 
      if(dst1.endsWith("/")) {
        dst1 += src.substring(src.lastIndexOf('/') + 1);  //add the file name of the url
      }
      File dst = new File(dst1); 
      boolean bStrict = false;
      MessageDigest md = null;
      if(md5 !=null) {
        if(md5.length() !=32) {
          System.err.println("faulty length for -md5:, should be 32, detect: " +md5.length());
          exitcode = 2;
        } else {
          try {
            md = MessageDigest.getInstance("MD5");
          } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
          }
        }
        if(args.length >=4) {
          if(args[3].equals("-strict")) {
            bStrict = true;
          } else {
            System.err.println("-strict expected as 4. argument, found: " + args[3]);
            exitcode = 5;
          }
        }
      }
      if(exitcode <=2) {
        try {
          URL url = new URL(src);
          sin = url.openStream();
      
          //The dst file name is anytime the second argument. 
          //If this ends with a / then it is a directory.
          sout = new FileOutputStream(dst);
          byte[] buffer = new byte[16384];  //16 k Buffer
          System.out.print("copy to: " + dst.getAbsolutePath() + " : from URL: " + src + " ... ");
          int zbytes;
          int zbytesSum = 0; 
          do {
            zbytes = sin.read(buffer);
            if(zbytes >0) {
              sout.write(buffer, 0, zbytes);
              zbytesSum += zbytes;
              if(md !=null) {
                md.update(buffer, 0, zbytes);
              }
            }
          } while(zbytes >0);
          System.out.println(Integer.toString(zbytesSum) + " bytes");
        }
        catch(IOException exc) {
          if(sin == null) System.err.println("cannot open URL:" + src);
          else if(sout == null) System.err.println("cannot create file:" + dst.getAbsolutePath());
          else System.err.println("exception: " + exc.getMessage());
          exitcode = 4;
        }
        if(sin !=null) { 
          try{ sin.close(); }
          catch(IOException exc) { System.err.println("unexpected exception on close URL"); }
        }
        if(sout !=null) { 
          try{ sout.close(); }
          catch(IOException exc) { System.err.println("unexpected exception on close file"); }
          if(md !=null) {
            byte[] md5code = md.digest();
            
            StringBuilder md5read = new StringBuilder();
            for(byte md5b: md5code) {
              String md5h = Integer.toHexString(md5b);
              if(md5h.length() >2) { //FFFF before
                md5h = md5h.substring(md5h.length()-2);  //last 2 characters
              } else if(md5h.length() ==1) {
                md5read.append('0');
              }
              md5read.append(md5h);
            }
            if(md5read.length() !=32) {
              System.err.println("internal MD5 algorithm faulty, not 16 byte");
              exitcode = 5;
            } else {
              for(int ix = 0; ix < 32; ++ix) {
                if(md5read.charAt(ix) != md5.charAt(ix)) {
                  System.err.println("-md5: faulty at position " + ix);
                  exitcode = 2;
                  if(bStrict) {
                    boolean bok = dst.delete();
                    if(!bok) {
                      System.err.println("pay attention: file cannot be deleted: " + dst.getAbsolutePath());
                    }
                  }
                  break;
                }
              }
            }
          }
        }
      }
    }
    System.exit(exitcode);
  }

}
