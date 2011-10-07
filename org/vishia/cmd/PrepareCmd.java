package org.vishia.cmd;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;


/**This class holds a command line which is prepared with place holder
 * @author Hartmut Schorrig
 *
 */
public class PrepareCmd
{
  private static class CmdReplace{ int pos; char what;} 
  
  private List<CmdReplace> listCmdReplace;
  
  /**Command string without placeholder. The positions of the placeholder
   * are contained in {@link #listCmdReplace}.
   * 
   */
  private String sCmdTemplate;
  
  
  /**The command how it is given with place holder. Maybe from ZBNF2Java-parsing.
   * 
   */
  public String cmd;
  
  
  /**Name of the command which it is showing to select. */
  public String name;
  
  public void prepareListCmdReplace()
  {
    listCmdReplace = new LinkedList<CmdReplace>();
    StringBuilder sCmd2 = new StringBuilder(cmd);
    int posSep;
    while( (posSep = sCmd2.indexOf("<*"))>=0){
      String s3 = sCmd2.substring(posSep);
      CmdReplace cmdReplace = new CmdReplace(); 
      cmdReplace.pos = posSep;
      int chars = 0;
      if(     s3.startsWith("<*file>"))     { cmdReplace.what = 'f'; chars = 4; }
      else if(s3.startsWith("<*dir>"))      { cmdReplace.what = 'F'; chars = 3; }
      else if(s3.startsWith("<*name>"))     { cmdReplace.what = 'm'; chars = 4; }
      else if(s3.startsWith("<*nameExt>"))  { cmdReplace.what = 'n'; chars = 7; }
      else if(s3.startsWith("<*ext>"))      { cmdReplace.what = 'e'; chars = 3; }
      else if(s3.startsWith("<*localFile>")){ cmdReplace.what = 'l'; chars = 9; }
      else if(s3.startsWith("<*absFile>"))  { cmdReplace.what = 'a'; chars = 8; }
      else if(s3.startsWith("<*localDir>")) { cmdReplace.what = 'L'; chars = 8; }
      else if(s3.startsWith("<*absDir>"))   { cmdReplace.what = 'A'; chars = 6; }
      else { cmdReplace = null; }         
      if(chars >0){
        listCmdReplace.add(0,cmdReplace); 
        sCmd2.replace(posSep, posSep + chars + 3, "");
      } else {
        throw new IllegalArgumentException("Illegal placeholder: " + sCmd2.substring(posSep));
      }
    }
    sCmdTemplate = sCmd2.toString();
    
  }
  
  public String prepareCmd(CmdGetFileArgs_ifc args)
  {
    if(listCmdReplace ==null){ 
      prepareListCmdReplace(); 
    }
    StringBuilder sCmd2 = new StringBuilder(sCmdTemplate);
    args.prepareFileSelection();
    File file = args.getFileSelect();
    if(file !=null){
      String sPath = file.getPath().replace('\\', '/');
      String sPathCmd = sPath;
      if(File.separatorChar == '\\'){ 
        sPathCmd = sPath.replace('/', '\\');
      }
      String sPathAbs = ""; 
      try{ sPathAbs = file.getCanonicalPath(); } catch(Exception exc){ sPathAbs="?notFound?"; }
      int posName = sPath.lastIndexOf('/') +1; //0 if no / found.
      int posNameEnd = sPath.lastIndexOf('.');
      int posExt = posNameEnd +1;
      if(posExt < 0){ posExt = posNameEnd = sPath.length();}
      //replace placeholder:
      for(CmdReplace repl: listCmdReplace){
        switch(repl.what){
        case 'f': sCmd2.insert(repl.pos, sPathCmd); break;
        case 'm': sCmd2.insert(repl.pos, sPathCmd.substring(posName, posNameEnd)); break;
        case 'n': sCmd2.insert(repl.pos, sPathCmd.substring(posName)); break;
        case 'e': sCmd2.insert(repl.pos, sPathCmd.substring(posExt)); break;
        case 'l': sCmd2.insert(repl.pos, sPathCmd); break;
        case 'a': sCmd2.insert(repl.pos, sPathAbs); break;
        case 'F': sCmd2.insert(repl.pos, posName ==0 ? "." : sPathCmd.substring(0, posName-1)); break;  //dir
        case 'L': sCmd2.insert(repl.pos, posName ==0 ? "." : sPathCmd.substring(0, posName-1)); break;  //dir
        case 'A': 
          String sDirAbs = null;
          try{ sDirAbs = file.getParentFile().getCanonicalPath();
          } catch(IOException exc){ sDirAbs = "?notFound?"; }
          sCmd2.insert(repl.pos, sDirAbs);
        }
      }
    }
    return sCmd2.toString();
  }

}
