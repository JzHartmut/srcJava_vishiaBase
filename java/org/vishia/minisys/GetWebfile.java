package org.vishia.minisys;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
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
    int exitcode = smain(args);
    System.exit(exitcode);
  }
  
  
  
  public static int smain(String[] args) {
    int exitcode = 0;
    if(args.length == 0) {
      exitcode = 4;
      System.out.println("org.vishia.minisys.GetWebfile @<File> [<dst>] [-md5:<hash> [-strict]]");
      System.out.println("org.vishia.minisys.GetWebfile <URL> [<dst>] [-md5:<hash> [-strict]]");
      System.out.println(" reads a file content from <URL> and writes it to <dst>");
      System.out.println(" if a file <dst> exists then does not load another one from web. Accept it.");
      System.out.println(" if @<File> is given <File> is the path to a text file which contains in maybe more as one line with:");
      System.out.println("    \"[<dst>@]<URL>  [?][!]MD5=<hash>\" ");
      System.out.println("    example: \"myFile@https://addr.org/Archive/File-2020-03-23.ext !MD5=01234567890123456789012345678901\"");
      System.out.println(" <URL> is the same like a link in Web Browser to a file for download");
      System.out.println(" <dst> is an absolute or relative path written with / as separator.\n"
                       + "       dst as given second argument and dst in file line are combined\n"
                       + "       if it ends with / then it is the directory for the named URL file.\n"
                       + "       if dst is not given, the dst is the named URL file in the current dir.");
      System.out.println(" <hash> is the known and expected MD5-hash code of the downloaded file");
      System.out.println(" -strict or !MD5 in file, then the file is removed if hash is faulty");
      System.out.println(" ?MD5= or ?!MD5= in line and file exists, then calculate and output the MD5 from the existing file");
      System.out.println("exitcode 0: all ok, 1: one or more files already exist 2: hash faulty 5: parameter error");
      System.out.println(" Note: Use slash instead backslash on windows for file name too!");
      System.out.println(" Made by Hartmut Schorrig, www.vishia.org, 2020-03-15, LPGL-License");
    } else {
      String src = args[0];   //The URL
      
      //
      //Read all arguments after first, maybe used for @file too
      String dst = null;
      boolean bStrict = false;
      boolean bCheck = false;
      String md5 = null;
      int nArgMd5 = 2;
      if(args.length >=2) {
        dst = args[1];
        if(dst.startsWith("-md5")) {
          dst = null;
          nArgMd5 = 1;
        }
        if(args.length >=nArgMd5+1 && args[nArgMd5].startsWith("-md5:"))
        { md5 = args[nArgMd5].substring(5);
          if(args.length >=nArgMd5+2) {
            if(args[nArgMd5+1].equals("-strict")) {
              bStrict = true;
            } else {
              System.err.println("-strict expected as last argument, found: " + args[nArgMd5+1]);
              exitcode = 5;
            }
          }
        }
      }
      
      
      
      if(src.startsWith("@")) {
        File f = new File(src.substring(1));
        BufferedReader fr = null;
        String sLine = null;
        try{ 
          fr = new BufferedReader(new FileReader(f));
          do {
            sLine = fr.readLine();
            if(sLine !=null && (sLine = sLine.trim()).length() >0 && !sLine.startsWith("#")) {
              bStrict = false;
              bCheck = false;
              md5 = null;
              int pos = sLine.indexOf("@");
              String dstLine = dst;  //may be null
              if(pos >0) {
                if(dst !=null) {
                  dstLine = dst + sLine.substring(0, pos);  //the file or directory to store
                } else {
                  dstLine = sLine.substring(0, pos);
                }
              }
              pos +=1;  //after @ or 0 if @ not contained
              int end = sLine.indexOf(' ', pos);  //first space after URL, URL must not contain a space
              if(end <=0) {
                end = sLine.length();
              }
              src = sLine.substring(pos, end);
              pos = sLine.indexOf("MD5=", pos);
              if(pos >=0) {
                if(sLine.length() < pos+4+32) {
                  System.err.println("@file contains MD5=... but too less characters, necessary: 32");
                  exitcode = 5;
                } else {
                  md5 = sLine.substring(pos+4, pos+4+32);
                  bStrict = sLine.charAt(pos-1) == '!';
                  bCheck = sLine.charAt(bStrict ? pos-2 : pos-1) == '?';
                }
              }
              if(exitcode <=2) {
                int exitcode1 = readFile(src, dstLine, md5, bStrict, bCheck);
                if(exitcode1 > exitcode) {
                  exitcode = exitcode1; //build the max
                }
              }
            }
          } while(sLine !=null);
          fr.close();
          fr = null;
        } catch(IOException exc) {
          if(fr == null) { System.err.println(src + ": cannot read " + f.getAbsolutePath() ); }
          else { 
            System.err.println(src + ": " + exc.getMessage() ); 
            if(fr !=null) {
              try{ fr.close();} catch(IOException exc1) { System.err.println(exc1.getMessage()); }
            }
          }
          exitcode = 5;
        }
      }
      else {
        //immediate arguments:
        if(exitcode <=2) {
          exitcode = readFile(src, dst, md5, bStrict, bCheck);
        }
      }
    }
    return exitcode;
  }

  
  
  
  
  public static int readFile(String src, String dstArg, String md5, boolean bStrict, boolean bCheck) {
    final String dst1;
    if(dstArg == null) {
      dst1 = src.substring(src.lastIndexOf('/') + 1);  //use the file name of the url
    }
    else if(dstArg.endsWith("/")) {
      dst1 = dstArg + src.substring(src.lastIndexOf('/') + 1);  //add the file name of the url
    } else {
      dst1 = dstArg;
    }
    File dst = new File(dst1); 
    boolean bDstExists = dst.exists();
    int exitcode = bDstExists ? 1 : 0;
    if(!bDstExists || bCheck) {  //do nothing if there is already a file with the same name.
      InputStream sin = null;
      FileOutputStream sout = null;
      MessageDigest md = null;
      if(md5 !=null || bCheck) {
        try {
          md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
      int zbytesSum = 0; 
      try {
        if(bDstExists) {
          sin = new FileInputStream(dst); //check dst
        } else {
          System.out.print("copy to: " + dst.getAbsolutePath() + " : from URL: " + src + " ... ");
          sout = new FileOutputStream(dst);
          URL url = new URL(src);
          sin = url.openStream();
        }
        //The dst file name is anytime the second argument. 
        //If this ends with a / then it is a directory.
        byte[] buffer = new byte[16384];  //16 k Buffer
        int zbytes;
        do {
          zbytes = sin.read(buffer);  //read from web or read from file if exists
          if(zbytes >0) {
            if(sout !=null) {
              sout.write(buffer, 0, zbytes);
            }
            zbytesSum += zbytes;
            if(md !=null) {
              md.update(buffer, 0, zbytes);
            }
          }
        } while(zbytes >0);
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
      }
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
        boolean bMD5ok = true;
        int ix = 0;
        if(md5read.length() !=32) {
          System.err.println("internal MD5 algorithm faulty, not 16 byte");
          exitcode = 5;
        } else {
          if(md5.length() !=32) {
            bMD5ok = false;
            if(!bDstExists) {
              System.err.println("faulty length for -md5:, should be 32, detect: " +md5.length());
              exitcode = 5;
            } 
          } else {
            for(ix = 0; ix < 32; ++ix) {
              if(md5read.charAt(ix) != md5.charAt(ix)) {
                bMD5ok = false;
                break;
              }
            }
          }
        }
        if(bDstExists && bCheck) {
          System.out.print("MD5=" + md5read + (bMD5ok ? " ok: " : " faulty: ") + dst.getAbsolutePath() + " ... ");
        }
        else if(!bMD5ok) {
          if(bStrict) {
            System.err.println("-md5: faulty at position " + ix);
            exitcode = 2;
            boolean bok = dst.delete();
            if(!bok) {
              System.err.println("pay attention: file cannot be deleted: " + dst.getAbsolutePath());
            }
          } else {
            System.out.print(" faulty MD5, read MD5=" + md5read + "  ");
          }
        }
        System.out.println(Integer.toString(zbytesSum) + " bytes");
      }
    }
    return exitcode;
  }
  
  
}
