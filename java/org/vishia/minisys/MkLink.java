package org.vishia.minisys;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;


/**Supports compatible symbolic links on linux/UNix and Windows. 
 * Not used yet because the symbolic links in Windows with this approach need administrator rights. 
 * It is possible to create a symbolic link in Windows with mklink /J without adminstrator rights,
 * but this seems to be not supported in the JRE (?) 
 * @author hartmut
 *
 */
public class MkLink {

  
  public static void main(String[] args) {
  
    if(args.length < 2) {
      System.out.println("org.vishia.minisys.MkLink <target> <link>");
      System.out.println(" creates a link to the named <target> directory.");
      System.out.println(" <target> is ");
      System.out.println(" <link> is a absolute or relative path written with / as separator.\n");
      System.out.println(" Note: Use slash instead backslash on windows also for file paths");
      System.out.println(" Note: It needs administrator rihgts in MS-Windows, better use mklink /J");
      System.out.println(" Made by Hartmut Schorrig, www.vishia.org, 2020-03-20");
    } else {
      String sTarget = args[0];
      String sLinkname = args[1];
      Path target = null, link = null;
      try {
        System.out.print("MkLink " + sLinkname + " <= " + sTarget + " ...");
        target = Paths.get(sTarget);
        link = Paths.get(sLinkname);
        Files.createSymbolicLink(link, target);
        System.out.println("ok");
      } catch(InvalidPathException | IOException exc) {
        if(target == null) System.err.println(" faulty: " + sTarget);
        else if(link == null) System.err.println(" faulty: " + sLinkname);
        else System.err.println(" error MkLink");
      }
    }
  
  }
}
