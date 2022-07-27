//==JZcmd==
//JZcmd main(){ java org.vishia.util.FilepathFilter.test(); }
//==endJZcmd==
package org.vishia.util;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

/**This class helps to check whether a String given path and name matches to a mask String. It can be used for file paths especially
 * but for any other path too.  
 * It is similar to {@link java.io.FilenameFilter} or {@link java.io.FilenFilter} but deals with CharSequences exclusively.
 * The constructor prepares a String given path mask (with wild cards). The methods
 * <ul>
 * <li>{@link #checkDir(CharSequence)}
 * <li>{@link #checkName(String)}
 * </ul>
 * checks whether a path matches to the mask.
 * @author Hartmut Schorrig, www.vishia.org
 *
 */
public class FilepathFilter implements FilenameFilter
{
    /**Version, history and license.
   * <ul>
   * <li>2015-09-06 Hartmut created, The concept of such an filter was born for C programming since about 1990. The "filelist.exe" has used
   *   such an adequate algorithm. The method {@link org.vishia.util.FileSystem#addFileToList(String, List)} and its derived methods 
   *   have used an adequate algorithm. The idea to mark a deeper directory tree with "/** /" is an old idea.
   *   Now this algorithm has a frame in a class.  
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
   * <li> But the LPGL is not appropriate for a whole software product,
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
  public static final String version = "2015-01-03";

  
  
  static class NameFilter
  {
  
    final String sStartName, sEndName, sContainName;
    
    /**First position of a contains() quest and last pos from end. */
    final int posContainName, posContainNameFromEnd;
  
    NameFilter(String sMask) {
      int zMask = sMask.length();
      int posAsteriskName = sMask.indexOf('*');
      int posAsteriskName2 = sMask.indexOf('*', posAsteriskName+1);
      if(posAsteriskName >=0) {
        sStartName = posAsteriskName >0 ? sMask.substring(0, posAsteriskName) : null;
        int posEnd = posAsteriskName2 >=0? posAsteriskName2 +1 : posAsteriskName +1; 
        sEndName = posEnd < zMask ? sMask.substring(posEnd) : null;
        //null if "start**end" or "start*end"
        if(posAsteriskName2 < 0 || posAsteriskName2 == posAsteriskName+1) {
          posContainName = posContainNameFromEnd = -1;
          sContainName = null;
        } else {
          posContainName = posAsteriskName+1;
          posContainNameFromEnd = zMask - posAsteriskName2 - (posAsteriskName2 - posContainName);  // - lengthof(sContainPath)
          sContainName = sMask.substring(posContainName, posAsteriskName2); 
        }
      } else {
        sStartName = sMask;
        sEndName = null;
        sContainName = null;
        posContainName = posContainNameFromEnd = -1;
      }
      
    }
    
    
    public boolean checkName(String name)
    { int posContain;
      if(sStartName !=null && !name.startsWith(sStartName)) 
        return false;
      if(sEndName !=null && !name.endsWith(sEndName)) 
        return false;
      if(sContainName !=null) {
        posContain = name.indexOf(sContainName);
        if(posContain < posContainName && posContain > name.length()-posContainNameFromEnd) 
          return false;
      }
      return true;
    }

  }
  
  
  
  static class PathFilter
  {
  
    final String sStartPath, sEndPath, sContainPath;
    
    final boolean bAlltree1, bAlltree2;
    
    final int pos1;
    
    /**The length of the sEndPath, start of endpath from end. */
    final int posEndFromEnd; //, pos2, pos3;

    /**First position of a contains() quest and last pos from end. */
    final int posContainPath, posContainPathFromEnd;
  
    PathFilter(String sMask) {
      int zMask = sMask.length();
      pos1 = sMask.indexOf('*');
      int posAsterisk2 = sMask.indexOf('*', pos1+2);
      if(pos1 >=0) {
        bAlltree1 = zMask > pos1 +1 && sMask.charAt(pos1 +1) == '*';
        sStartPath = pos1 >0 ? sMask.substring(0, pos1) : null;
        bAlltree2 = zMask > posAsterisk2 +1 && sMask.charAt(posAsterisk2 +1) == '*';
        int posEnd = posAsterisk2 >=0? posAsterisk2 + (bAlltree2 ? 2 : 1) : pos1 + (bAlltree1 ? 2 : 1); 
        if(posEnd < zMask) {
          sEndPath = sMask.substring(posEnd);
          posEndFromEnd = zMask - posEnd;
        } else {
          sEndPath = null;
          posEndFromEnd = 0;
        }
        //null if "start**end" or "start*end"
        if(posAsterisk2 < 0) {
          posContainPath = posContainPathFromEnd = -1;
          sContainPath = null;
          //pos2 = 0;
          //pos3 = 0;
        } else {
          posContainPath = pos1;
          posContainPathFromEnd = zMask - posAsterisk2 + (posAsterisk2 - posContainPath);  // - lengthof(sContainPath)
          sContainPath = sMask.substring(posContainPath + (bAlltree1 ? 2 : 1), posAsterisk2); 
        }
      } else {
        sStartPath = sMask;
        //pos2 = pos3 = 0;
        sEndPath = null;
        sContainPath = null;
        posContainPath = posContainPathFromEnd = -1;
        bAlltree1 = bAlltree2 = false;
        posEndFromEnd = 0;
      }
      
    }
    
    
    public boolean checkPath(CharSequence name)
    { int posEnd1;   //end of wildcard part1
      boolean bSelected = true;
      if(sStartPath !=null && !StringFunctions.startsWith(name, sStartPath)) return false;
      if(sEndPath !=null && !StringFunctions.endsWith(name,sEndPath)) return false;
      if(sContainPath !=null){ 
        posEnd1 = StringFunctions.indexOf(name, sContainPath);
        if(posEnd1 < posContainPath && posEnd1 > posContainPathFromEnd) return false;
      } else {
        posEnd1 = name.length() - posEndFromEnd;
      }
      if(!bAlltree1 && StringFunctions.indexOf(name, pos1, posEnd1, '/') >=0) return false;
      if(bAlltree1) return true; //don't check
      
      return bSelected;
    }

  }
  
  final PathFilter pathFilter;
  
  /**Only one name possiblity. Let the {@link #listNameFilter} null, saves memory and time. */ 
  final NameFilter nameFilter;
  
  /**One or more entries for checking the file name or null to accept all names.  */
  final List<NameFilter> listNameFilter;
  
  /**One or more entries for checking excluding the file name or null to exlude nothing. */
  final List<NameFilter> listExcludeNameFilter;
  
  /**If set then the path contains path/before/** /path/after. It means more levels are accepted (like 'recursively') */
  final boolean bAllTree;   //yet package used (2015-09)
  
  /**Creates an instance which can check whether any path or name matches to the given path.
   * The mask can contain:
   * <ul>
   * <li>A directory and a name part. The name part is all after the last "/". 
   * <li>If the mask ends with "/" then {@link #checkName(String)} returns true for any name. All names matches.
   * <li>If the mask does not contain a "/" it can only check names. The {@link #checkDir(CharSequence)} returns true in an case.  
   * <li>The name part can contain up to two wildcards in form of "*". It separates the name in a start, middle and end part.
   *   <ul>
   *   <li><code>path/start*middle*end</code>
   *   <li><code>path/*middle*end</code>
   *   <li><code>path/*end</code>
   *   <li><code>path/start*</code>
   *   <li><code>path/start*end</code>
   *   </ul>
   *   A name matches if it starts with the <code>start</code>, contains the <code>middle</code> after the start and before the end part,  
   *   and ends with the <code>end</code>. If one of the parts are empty, it is not tested.
   *   <ul>
   *   <li>Example <code>*.txt</code>: The name should end with ".txt".
   *   <li>Example <code>my*year*.txt</code>: The name "myear_1984.txt" does not match. It starts with "my", contains "year" 
   *     but it should contain "year" after "my".
   *   </ul>
   * <li>The name part can contain of several possibilities. Then the name part should start with <code> : </code>, any further name part 
   *   should start with a next <code> : </code>. It is possible to write or not write a space before and after ':' for better readability.  
   *   <ul>
   *   <li><code>path/ : *.txt : *.log</code>: 2 possibilities of names.
   *   <li><code>: *.txt : *.log</code>: 2 possibilities of names, no directory path.
   *   </ul> 
   * <li>A name part can start with a second <code>:</code>, then it is an exclusive mask. The second ':' should be written without following space!
   *   <ul>
   *   <li><code>: save* : :*.bak</code>: All files "save*" but not "*.bak"-files.
   *   <li><code>: :*.bak</code>: All files exclusively "*.bak"
   *   </ul>  
   * <li>The path can contain a wildcard in form <code>path/part**end/end</code>, usual in form <code>start/** /end</code>:
   *   <ul>
   *   <li><code>** /name</code>: All directories in the tree.
   *   <li><code>projectX/** /src* /name</code>: All directories below "projectX" which starts with "src"
   *   </ul>
   * </ul>
   * @param mask
   */
  public FilepathFilter(CharSequence mask)
  { if(mask == null || mask.length() ==0){
      pathFilter = null;
      listNameFilter = null;
      listExcludeNameFilter = null;
      nameFilter = null;
      bAllTree = false;
    } else {
      String mask1 = mask.toString();  //Note: invoked with string, no effort.
      int posName = mask1.lastIndexOf('/') +1;  //0 if no / found.
      if(posName == 0) { 
        //an '/' was not found, only a name.
        bAllTree = false;
        pathFilter = null;
      }
      else {
        pathFilter = new PathFilter(mask1.substring(0, posName -1));
        bAllTree = pathFilter.bAlltree1;
      }
      //
      //check more as one names, check name
      //
      if(mask.charAt(posName) == ':' || StringFunctions.equals(mask, posName, posName+2, " :")) {
        this.nameFilter = null;
        List<NameFilter> nameFilter = null;
        List<NameFilter> excludeNameFilter = null;
        int startNamePart = posName + mask.charAt(posName) == ':' ? 1 : 2;
        do {
          if(mask.charAt(startNamePart) == ' ') { startNamePart +=1; }  //a space after ': '
          int sepNamePart = mask1.indexOf(':', startNamePart+1);
          if(sepNamePart < 0){ sepNamePart = mask.length(); }
          int endNamePart = mask.charAt(sepNamePart-1) == ' ' ? sepNamePart -1 : sepNamePart; //maybe space before ' :'
          if(mask.charAt(startNamePart) == ':') {
            if(excludeNameFilter == null) { excludeNameFilter = new ArrayList<NameFilter>(); }
            excludeNameFilter.add(new NameFilter(mask1.substring(startNamePart +1, endNamePart)));
          } else {
            if(nameFilter == null) { nameFilter = new ArrayList<NameFilter>(); }
            nameFilter.add(new NameFilter(mask1.substring(startNamePart, endNamePart)));
          }
          startNamePart = sepNamePart+1;
        } while(startNamePart < mask.length());
        this.listExcludeNameFilter = excludeNameFilter;
        this.listNameFilter = nameFilter;    
      }
      else {
        this.listExcludeNameFilter = null;
        this.listNameFilter = null;    
        this.nameFilter = new NameFilter(mask1.substring(posName));
      }
    }

  }
  

  
  
  /**Checks whether a given name matches to the mask. It use only the name part of the mask.
   * @param name given name
   * @return true if it matches.
   */
  public boolean checkName(String name)
  { //first check whether the name is excluded.
    if(listExcludeNameFilter !=null){
      for(NameFilter entry: listExcludeNameFilter) {
        if(entry.checkName(name)) {
          return false;
        }
      }
    }
    if(nameFilter !=null){ return nameFilter.checkName(name); } 
    //if not excluded, true if there is not a name filter:
    if(listNameFilter == null) return true;
    //check name filter:
    for(NameFilter entry: listNameFilter) {
      if(entry.checkName(name)) {
        return true;
      }
    }
    //nothing found:
    return false; //all checked.
  }
  
  
  /**Checks whether a given directory path matches to the mask. It does not use the name part of the mask.
   * @param sPath
   * @return true if it matches.
   */
  public boolean checkDir(CharSequence sPath)
  { return pathFilter ==null? true: pathFilter.checkPath(sPath);
  }




  @Override public boolean accept(File dir, String name)
  {
    // TODO Auto-generated method stub
    CharSequence sDir = FileSystem.normalizePath(dir);
    if(bAllTree) return true;  //TODO more condition?
    else return checkDir(sDir) && checkName(name);
  }
  
  
  private static void check(boolean result) {
    if(!result) System.out.println("failure");
  }
  
  
  /**This routine tests some examples. It is able to start with a JZcmd script contained in this file:
   * Invoke <pre>
   * JZcmd /path/to/srcJava_vishiaBase/org/vishia/util/FilepathFilter.java
   * </pre>
   */
  public static void test()
  { boolean ok = true;
    FilepathFilter f1 = new FilepathFilter("prod/**/sub*/that/test*.*");
    ok &= f1.checkDir("prod/a/b/subx1/that");
    FilepathFilter f2 = new FilepathFilter("prod/**/test*.*");
    ok &= f2.checkDir("prod/a/b");
    FilepathFilter f3 = new FilepathFilter(" : save* : :*.bak");
    ok &= f3.checkName("save1.txt");
    ok &= !f3.checkName("save2.bak");
  }
  
  
}
