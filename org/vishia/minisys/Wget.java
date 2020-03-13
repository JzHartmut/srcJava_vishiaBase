package org.vishia.minisys;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

/**Reads a file from Web given as URL
 * @author Hartmut Schorrig
 * @since 2020-08-10, LPGL-License. Do not remove this license declaration.
 *
 */
public class Wget {
  public static void main(String[] args) {
    InputStream sin = null;
    OutputStream sout = null;
    if(args.length < 2) {
      System.out.println("org.vishia.minisys.Wget <URL> <dst>");
      System.out.println(" reads a file content from <URL> and writes it to <dst>");
      System.out.println(" <URL> is the same like a link in Web Browser to a file for download");
      System.out.println(" <dst> is a absolute or relative path written with / as separator.\n"
                       + "       if it ends with / then it is the directory for the named URL file.");
      System.out.println(" Note: Use slash instead backslash on windows for file name too!");
      System.out.println(" Made by Hartmut Schorrig, www.vishia.org, 2020-03-10, LPGL-License");
    } else {
      String src = args[0];   //The URL
      String dst = args[1]; 
      if(dst.endsWith("/")) {
        dst += src.substring(src.lastIndexOf('/') + 1);  //add the file name of the url
      }
      try {
        URL url = new URL(src);
        sin = url.openStream();
    
        //The dst file name is anytime the second argument. 
        //If this ends with a / then it is a directory.
        sout = new FileOutputStream(dst);
        byte[] buffer = new byte[16384];  //16 k Buffer
        System.out.print("copy to: " + dst + " : from URL: " + src + " ... ");
        int zbytes;
        int zbytesSum = 0; 
        do {
          zbytes = sin.read(buffer);
          if(zbytes >0) {
            sout.write(buffer, 0, zbytes);
            zbytesSum += zbytes;
          }
        } while(zbytes >0);
        System.out.println(Integer.toString(zbytesSum) + " bytes");
      }
      catch(IOException exc) {
        if(sin == null) System.err.println("cannot open URL:" + src);
        else if(sout == null) System.err.println("cannot create file:" + dst);
        else System.err.println("exception: " + exc.getMessage());
      }
      if(sin !=null) { 
        try{ sin.close(); }
        catch(IOException exc) { System.err.println("unexpected exception on close URL"); }
      }
      if(sout !=null) { 
        try{ sout.close(); }
        catch(IOException exc) { System.err.println("unexpected exception on close file"); }
      }
    }
  }

}
