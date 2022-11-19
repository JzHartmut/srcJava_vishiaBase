package org.vishia.checkDeps_C;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.vishia.mainCmd.MainCmdLogging_ifc;
import org.vishia.util.FileFunctions;
import org.vishia.util.FileSystem;

/**This class is create temporary to parse the config file.
 * The results are written to a created instance of {@link CfgData}. Only this is used then.
 * @author Hartmut Schorrig
 *
 */
public class ParserConfigFile
{
  /**Version, history and license.
   * <ul>
   * <li>2017-05-08 Hartmut new use {@link #currdir} for the config file, given in ctor.  
   * <li>2012-12-25 Hartmut new: Inserted in the Zbnf component because it is an integral part of the Zmake concept
   *   for C-compilation.
   * <li>2011-05-00 Hartmut created: It was necessary for C-compilation to check real dependencies in a fast way.
   *   The dependency check from a GNU compiler was to slow.
   * </ul>
   * <br><br>
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
   */
  public static final int version = 20121225;

  

  /**Aggregation to any console or log output. */
  private final MainCmdLogging_ifc console;
  
  /**Index of some variables with name=value. It are defined in the config file.  */
  private final Map<String, String> indexEnvirVariables = new TreeMap<String, String>();

  private final CfgData data; 

  private final File currdir;
  
  
  public ParserConfigFile(CfgData cfgData, MainCmdLogging_ifc console, File currdir)
  { this.console = console;
    this.data = cfgData;
    this.currdir = currdir;
  }



  /**Parses the given configuration file.
   * Sets 
   * <ul>
   * <li>{@link listInclPaths}
   * </ul>
   * @param sFileCfg
   * @throws IOException
   */
  String parseConfigFile(String sFileCfg){
    try{
      File fileCfg = new File(currdir, sFileCfg);
      BufferedReader reader = new BufferedReader(new FileReader(fileCfg));
      String sLine;
      while( (sLine = reader.readLine())!=null){
        int posComment = sLine.indexOf('#');
        if(posComment >=0){ sLine = sLine.substring(0, posComment); }
        sLine = sLine.trim();
        if(sLine.startsWith("-s=")){ //an include path
          parseCfgIncludeLine(sLine, 's');
        } else if(sLine.startsWith("-i=")){ //an include path
          parseCfgIncludeLine(sLine, 'i');
        } else if(sLine.startsWith("-j=")){ //an include path
          parseCfgIncludeLine(sLine, 'j');
        } else if(sLine.startsWith("-g=")){ //an include path
          parseCfgIncludeLine(sLine, 'g');
        } else if(sLine.startsWith("-y=")){ //an include path
          parseCfgIncludeLine(sLine, 'y');
        } else {
          if(sLine.length()>1){
            switch(sLine.charAt(0)){
            case '$': parseCfgEnvironmentVariable(sLine); break;
            default: throw new IllegalArgumentException("config file - error in line; " + sLine);
            }
          }
        }
      }
      reader.close();
    }catch(IllegalArgumentException exc){
      return "CheckDeps -" + exc.getMessage();
    }catch(IOException exc){
      return "CheckDeps - config file read error;" + exc.getMessage();
    }
    return null;
  }
  
  
  
  /**Parses a line with the setting of an environment variable.
   * <br><br>
   * Syntax::= $ <$?name> = <*\ \n?value>.
   * @param sLine
   */
  private void parseCfgEnvironmentVariable(String sLine){
    int posSep = sLine.indexOf('=');
    String sName = sLine.substring(1, posSep).trim();
    String sValue = sLine.substring(posSep+1).trim();
    indexEnvirVariables.put(sName, sValue);
  }
  
  
  /**Parses a line with the setting of an include path.
   * <br><br>
   * Syntax::= -i= <*\ \n?path>.
   * @param sLine
   */
  private void parseCfgIncludeLine(String sLine, char cWhat){
    StringBuilder uInclPath = new StringBuilder(sLine.substring(3));
    //String sInclPath;
    int posVariable;
    while( (posVariable = uInclPath.indexOf("$"))>=0){
      int posEnd = uInclPath.indexOf(")", posVariable+2);
      if(uInclPath.charAt(posVariable+1) != '(' || posEnd <0)
        throw new IllegalArgumentException("Error in cfg-file: Write $(name). Line;" + sLine);
      String sName = uInclPath.substring(posVariable +2, posEnd);
      String sValue = indexEnvirVariables.get(sName);
      if(sValue == null) {
        sValue = System.getenv(sName);  //read from environment variable
      }
      if(sValue == null) {
        sValue = System.getProperty(sName);  //read from Java system property
      }
      if(sValue == null) throw new IllegalArgumentException("config file - env-variable not found; " + sName);
      uInclPath.replace(posVariable, posEnd+1, sValue);
    }
    String sInclPathLocalSep = uInclPath.toString();
    int posSep = sInclPathLocalSep.indexOf(':', 2);
    if(posSep >0){
      uInclPath.setCharAt(posSep, '/');
    }
    String sInclPath = uInclPath.toString();
    boolean bAbspath = FileSystem.isAbsolutePath(sInclPath);
    File dirIncludePath = bAbspath ? FileFunctions.newFile(sInclPath) : new File(currdir, sInclPath);
    if(!dirIncludePath.exists()) {
      console.writeWarning("config file - include path not found; " + sInclPath);
    } else if(!dirIncludePath.isDirectory()) {
      console.writeWarning("config file - include path is not a directory; " + sInclPath);
    } else {
      switch(cWhat){
        case 's': case 'i':{
          InputSrc srcInfo = new InputSrc(sInclPathLocalSep, currdir);
          data.listSourcePaths.add(srcInfo);  //to detect whether a file is in the source pool.
          data.listInclPaths.add(dirIncludePath);
          if(cWhat == 's'){
            data.listProcessPaths.add(srcInfo);  //that files are processed for Objects.
          }
        } break;
        case 'j': data.listInclPaths.add(dirIncludePath); break;
        case 'y': data.listSystemInclPaths.add(dirIncludePath); break;
        case 'g': data.listGenSrcInclPaths.add(dirIncludePath); break;
        default: assert(false);
      }//switch;
    }
  }
  
  
  

  
}
