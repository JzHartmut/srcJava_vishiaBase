package org.vishia.util;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**Filter for a file path maybe with wildcards in the directory path
 * and also with a multi selection.
 * <br>
 * For example selecting all files for the FileList but only in dedicated directories and exclusing specifics are written as:
 * <br><code>[[src/**|result/**]/[~#*|~*#]|~_*]</code>
 * <br>it means, files are gotten either from <code>src</code> or <code>result</code>, form no other sub directory.
 * But from the root level the valid mask for files is <code>~_*</code>, it means should not start with "_".
 * For all deeper levels the files should not start or end with an <code>#</code>.
 * Hence all source files are gathered, but not commented files with <code>#</code>.  
 */
public class FilepathFilterM extends ObjectVishia {

//  * Note: The {@link java.io.FilenameFilter} is better as the {@link java.io.FileFilter}
//  *   because the {@link java.io.File#list(FilenameFilter)} builds a File instance only if the name is tested positively.
//  *   In opposite the {@link java.io.File#list(FileFilter)} builds a File instance anytime before the test. 
//  *   The difference may be marginal.But {@link java.io.File#list(FileFilter)} produces some more instances in the heap,
//  *   which are unnecessary. 
  
  /**Version, history and license.
   * Changes:
   * <ul>
   * <li>2023-01-26 Hartmut created: This class is an enhancement of {@link FilepathFilter} with the multi selection. 
   *   The idea is old, it was present in the path as <code>WildcardFilter</code> in {@link FileFunctions}
   *   but not used. Now it es tested, and used for the {@link FileList}.   
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
   */
  public final static String sVersion = "2023-01-26";

  /**This is the next part in the original path between /child/*/
  public final FilepathFilterM aFilterChild;

  private final String sBegin, sContain, sEnd;
  
  int zBegin, zContain, zEnd;
  
  /**True if the filter path on ctor has contained "**". Then apply the filter on subdirs too. */
  public final boolean bAllTree;
  
  private final boolean bNoWildcard;
  
  public final boolean bLast;
  
  /**Variants either instead sBefore or instead sBehind. 
   * 
   */
  List<FilepathFilterM> variantsBegin;
  
  List<String> variantsEnd;
  
  /**up to 32 bit for negation of variants, bit 0 for first text etc.*/
  final boolean bNotBegin;
  
  int mNotEnd;
  
  
  /**This list is not null if more as one varieties are exsiting,
   * separated by [var1|var2]
   */
  //List<FilepathFilterM> filterVariety;
  
  //FilepathFilterM afterVarieties;

  /**Creates a FilepathFilterM for one level. 
   * <br>
   * <b>Basic wildcard features:</b>
   * 
   * <table>
   * <tr><td><code>fix</code></td>
   *   <td>The filter accepts only this name</td></tr>
   * <tr><td><code>begin*</code></td>
   *   <td>The filter accepts names starting with "begin" and "begin" itself.
   *   <br>Familiar example: "myfile.*", all files with this name and any extension. 
   *   Or "myFile.c*" accepts also "myFile.cpp", but also for example "myFile.ci" if given. </td></tr>
   * <tr><td><code>*end</code></td>
   *   <td>The filter accepts names ending with "end" and "end" itself.
   *   Familiar example: "*.cpp", all files with extension "cpp"
   * <tr><td><code>begin*end</code></td>
   *   <td>The filter accepts names starting with "begin" and ending with end after before.
   *   <br>Familiar examples: "file*.cpp": All files starting with "file" with extension ".cpp"</td></tr>
   * <tr><td><code>begin*mid*end</code></td>
   *   <td>The filter accepts names starting with "begin", ending with end after before
   *   and containing "mid" between "begin" and "end".
   *   <br>Familiar examples: "file*.cpp": All files starting with "file" with extension ".cpp"</td></tr>
   * <tr><td><code>begin*mid*</code></td>
   *   <td>The filter accepts names starting with "begin" and containing "mid" after "begin".
   *   <br>Familiar examples: "file*.c*": All files starting with "file" with extension ".c" but also ".cpp"
   *   <br>This is a special case of <code>begin*mid*end</code>.</td></tr>
   * <tr><td><code>*mid*</code></td>
   *   <td>The filter accepts names which contains "mid" but also only "mid".
   *   <br>Familiar examples: "*.c*": All fileswith extension ".c" but also ".cpp"
   *   <br>This is a special case of <code>begin*mid*end</code>.</td></tr>
   * <tr><td><code>*</code></td>
   *   <td>The filter accepts all names, returns always true.</tr></td>
   * </table>
   * Generally two wildcards are accepted in the mask.
   * <br>
   * <b>bAlltree: Two <code>**</code></b>
   * <br>General if the first asterisk is written twice, it means the same mask is used also for the next level,
   * especially child on a file path. Only if it does not match, the next {@link #aFilterChild} is used.
   * <br>Some examples (applicable for all <code>*</code>)
   * <table>
   * <tr><td><code>**</code></td>
   *   <td>The filter accepts all names, returns always true.
   *   This stands for any deepness of a directory tree till the following child pattern does also match. 
   *   It means the child pattern is tested. If it does match, it (and the following children) is used for further checking. 
   *   If the child pattern does not match, matching is true and the child pattern is not used for this level. 
   *   </td></tr>
   * <tr><td><code>begin**</code></td>
   *   <td>The filter accepts names starting with "begin" and "begin" itself. 
   *   <br>It the next level starts also with begin, it is preconditional.
   *   But nevertheless the next child level is checked. If it matches, it is used. 
   * <tr><td><code>begin**mid*end</code></td>
   *   <td>It is similar for all other wildcard variants.
   *   <br><b>Note: The <code>"**"</code> must be written on the first wildcard position.</td></tr>
   * </table>
   * <br>
   * <b>variants given</b>
   * <br>This is the new feature and  difference to {@link FilepathFilter}. 
   * Variants are written in <code>[var1|var2]</code> for positive variants 
   * for the <code>begin</code> and <code>end</code> parts or as part of them between the wildcards.
   * <br> and written as <code>[~var1|~var2]</code> for negative variants. 
   * Then not only one given string for <code>begin</code> and <code>end</code> are valid (matching)
   * but either one of the possible, or all Strings exclusively the given. Look on examples:
   * <br>
   * The variants before a first asterisk (or if an asterisk is not given)
   * can contain a complete path, see {@link #createWildcardFilter(String)}.
   * This path is then continued with the following parts after '/' if given.
   * But parts after asterisk are not used:
   * <table>
   * <tr><td><code>[dirA/**|dirB/spec]/*.c*</code></td>
   *   <td>Here all *.c* files either from dirA in any deepness or from dirB/spec/*.c* are matching
   *   </td></tr>
   * <tr><td><code>[dirA/[spec1|spec2]|dirB/spec]/*.c*</code></td>
   *   <td>The parts of the path can be also nested. 
   *   </td></tr>
   * </table>
   * Any part in <code>[...part...|</code> is evaluated as an own expression in {@link #createWildcardFilter(String)}.
   * Hence any nesting is possible. 
   * <br>
   * Variants in the second part, after <code>'*'</code> can only be simple texts:
   * <table>
   * <tr><td><code>*.[~bak|~old]</code></td>
   *   <td>The filter does not accept names ending with this both
   *   <br>Note: Either a positive or a negative variant choice is possible. Both is nonsense.</td></tr>
   * <tr><td><code>*.[c|cpp]</code></td>
   *   <td>The filter accepts names ending with ".c" or ".cpp".
   *   This is the better possibility instead <code>*.c*</code> to accept C and C++ files.
   *   <br>Note: The list to check {@link #variantsEnd} contains "<code>.c</code>" and "<code>.cpp</code>",
   *   the complete text. 
   *   The writing style with the "<code>.</code>" outside of the variants is only an abbreviation for writing. </td></tr>
   * </table>
   * For usage see also {@link FileFunctions#createWildcardFilter(String)} for a whole path
   * and see {@link #accept(File, String)}.
   * @param sMask mask due to given examples
   * @param filterChild null or a child filter for the next level. 
   */
  public FilepathFilterM ( String sMask, boolean bLast, FilepathFilterM filterChild) { 
    int pos0 = 0;
    int zMask = sMask.length();                            // regard internal paths can also contain '*', do not find them
    int pos1 = StringFunctions.indexOfAnyCharOutsideQuotation(sMask, 0, zMask, "*", "[", "]", '\\', null); //  sMask.indexOf('*');
    
    int pos2 = pos1 <0 ? pos1 : StringFunctions.indexOfAnyCharOutsideQuotation(sMask, pos1+2, zMask, "*", "[", "]", '\\', null); //  sMask.indexOf('*');
    if(pos2 <0) { 
      if(pos1 <= zMask -2 && sMask.charAt(pos1+1) == '*') {
        pos2 = pos1 +1;                                    // refers second **
      } else {
        pos2 = pos1;                                       // for evaluation: second * is same as first
      }
    }
    //int pos2 = sMask.lastIndexOf('*');
    this.aFilterChild = filterChild;
    this.bLast = bLast;
    int posBracket;
    //
    if(sMask.charAt(0)== '~') {
      this.bNotBegin = true;
      pos0 = 1;
    } else {
      this.bNotBegin = false;
    }
    this.bNoWildcard = pos1 <0;
    if(this.bNoWildcard) {
      pos1 = zMask;
    }
    posBracket = sMask.indexOf('[');
    if(posBracket >=0 && (posBracket < pos1 || pos1 <0)) { // variants before first '*'
      this.sBegin = sMask.substring(pos0, posBracket);
      this.variantsBegin = new LinkedList<FilepathFilterM>();
      parseVariants(sMask.substring(pos0, pos1), posBracket, this.variantsBegin, filterChild);
      this.zBegin = posBracket;
    }
    else if(pos1 >=0) {                                  // "before*" but without [
      this.sBegin = sMask.substring(pos0, pos1);
      this.zBegin = pos1 - pos0;                               
    } else {
      this.sBegin = sMask;
      this.zBegin = zMask;
    }
    //
  
    if(pos1 <= zMask -2 && sMask.charAt(pos1 +1) == '*') {
      this.bAllTree = true;                      // "...**..."
      pos1 +=1;
    } else {
      this.bAllTree = false;
    }
    //
    if(pos2 >=0 && pos2 < zMask) {                                   // "....*behind"
      posBracket = sMask.indexOf('[', pos2+1);
      if(posBracket >=0) { // variants before first '*'
        this.sEnd = sMask.substring(pos2+1, posBracket);
        this.variantsEnd = new LinkedList<String>();
        this.mNotEnd = parseVariantsEnd(sMask.substring(pos2+1), posBracket-pos2-1, this.variantsEnd);
        this.zEnd = this.sEnd.length();
      }
      else {                                  // "before*" but without [
        this.mNotEnd = 0;
        this.sEnd = sMask.substring(pos2+1);
        this.zEnd = zMask - pos2 -1;                               
      }
    } else {
      this.mNotEnd = 0;
      this.variantsEnd = null;
      this.sEnd = null;
      this.zEnd = 0;
    }
    //
    this.zContain = pos2 - pos1 -1;            // "....**contain*...."
    if(this.zContain >0) {
      this.sContain = sMask.substring(pos1+1, pos2);
    } else {
      this.sContain = null;
    }
//    
//    
//    
//      this.bAllEntries = sMask.equals("**");
//      this.bNot = sMask.startsWith("~");
//      int pos0 = this.bNot ? 1 : 0;
//      if(this.bAllEntries){
//        this.bAllTree = true;
//        this.sBefore = this.sBehind = this.sContain = null;
//      } else {
//        int len = sMask.length();
//        int pos1a;
//        this.bAllTree = pos1 <= len-2 && sMask.charAt(pos1+1) == '*';   //before**between*end also possible
//        if(this.bAllTree) {
//          pos1a = pos1 +1;
//          
//        } else {
//          pos1a = pos1;
//        }
//        //
//        if(pos1 <0) {
//          this.sBefore = sMask.substring(pos0);
//          this.zBefore = sMask.length() - pos0;
//          this.sBehind = null;
//          this.sContain = null;
//        } else {
//          this.sBefore = sMask.substring(pos0, pos1);     // sBefore*...
//          this.zBefore = pos1 - pos0;
//          //
//          if(pos2 < len -1) { 
//            this.sBehind = sMask.substring(pos2+1);        // "*behind"
//            this.zBehind = sMask.length()-pos2-1;
//          } else { 
//            this.sBehind = "";                             // "...*" 
//          }                             // "*", "before*", "before*contain*", "*contain*"
//          //
//          if(pos2 > pos1a){                                // "*sContain*"
//            this.sContain = sMask.substring(pos1a+1, pos2); 
//            this.zContain = pos2 - pos1a -1;
//          } else { 
//            this.sContain = null; 
//          }
//        }
//    }
  }

  
  private static String parseVariants(String sPart, int posBracket, List<FilepathFilterM> list, FilepathFilterM filterChild) {
    // search ] but not inside a quotation with [...]
    int posEndBracket = StringFunctions.indexOfAnyCharOutsideQuotation(sPart, posBracket+1, -1, "]", "[", "]", '\\', null);
        //sPart.indexOf(']');
    String sPart1 = sPart.substring(0, posBracket);
    String sPart2;
    if(posEndBracket <0) { 
      posEndBracket = sPart.length(); 
      sPart2 = "";
    } else {
      sPart2 = sPart.substring(posEndBracket+1);
    }
    int posVariety = posBracket +1;                    // check [variety|...
    while(posVariety < posEndBracket) {                // do not enter if no [ or ] found.  
      //int posEndVariety = sPart.indexOf('|', posVariety);
      int posEndVariety = StringFunctions.indexOfAnyCharOutsideQuotation(sPart, posVariety, -1, "|", "[", "]", '\\', null);;
      if(posEndVariety <0) { posEndVariety = posEndBracket; }
      String sVariant = sPart.substring(posVariety, posEndVariety);
      FilepathFilterM variant = createWildcardFilter(sVariant, filterChild);
      list.add(variant);
      posVariety = posEndVariety +1;
    }
    return sPart1 + "*" + sPart2;
  }
  
  
  
  private static int parseVariantsEnd(String sPart, int posBracket, List<String> list) {
    int posEndBracket = sPart.indexOf(']');
    String sPart1 = sPart.substring(0, posBracket);
    String sPart2;
    if(posEndBracket <0) { 
      posEndBracket = sPart.length(); 
      sPart2 = "";
    } else {
      sPart2 = sPart.substring(posEndBracket+1);
    }
    int posVariety = posBracket +1;                    // check [variety|...
    int mBit = 0x1;
    int mNotEnd = 0x0;
    while(posVariety < posEndBracket) {                // do not enter if no [ or ] found.  
      int posEndVariety = sPart.indexOf('|', posVariety);
      if(posEndVariety <0) { posEndVariety = posEndBracket; }
      //String sVariant = sPart1 + sPart.substring(posVariety, posEndVariety) + sPart2;
      //list.add(sVariant);
      if(sPart.charAt(posVariety)== '~') {
        mNotEnd |= mBit;
        posVariety += 1;
      }
      String sVariant = sPart.substring(posVariety, posEndVariety);
      list.add(sVariant);
      posVariety = posEndVariety +1;
      mBit <<=1;
    }
    return mNotEnd;
  }
  
  
  
  
  
  public FilepathFilterM nextChild () {
    return this.aFilterChild;
  }
  

  /**Checks whether the name matches due to this filter. 
   * It checks only this level, not {@link #nextChild()} and also not a path separated with '/' or '\'.
   * See FileFunctions#checkPathTODO
   */
  //@Override 
  public boolean XXaccept(File dir, String name) { 
    int zName = name.length();
    int posEndBegin = this.zBegin;
    int posStartEnd = zName - this.zEnd;
    if(this.zBegin > posStartEnd) {                      // name is to short for the mask sBefore*sBehind
      return this.bNotBegin;                             // returns true if the matching should be false
    } else {
      if(this.variantsBegin !=null) {
        boolean bOkBefore = true;
        //for(String sBefore: this.variantsBegin) {
        for(FilepathFilterM variant: this.variantsBegin) {
          boolean bOk = false; //variant.accept(dir, name.substring(posEndBegin, posStartEnd));
          if(!bOk) { return this.bNotBegin; }
//          if(name.startsWith(sBefore)) {
//            if(this.bNotBegin) {
//              return false;                              // false if variantBefore matches but with not
//            } else {
//              bOkBefore = true;                          // a matching sBefore found in list.
//              posEndBefore = sBefore.length();
//              break;
//            }
//          } else if(!this.bNotBegin) {
//            bOkBefore = false;                           // at least one positive variant not found, a positive variant is necessary. 
//          }
        }
        if(!bOkBefore) {
          return this.bNotBegin;                                  // positive varinants given, both nothing matches.
        }
      } else if(this.sBegin !=null ) {
        if(!name.startsWith(this.sBegin)) {
          return this.bNotBegin;
        }
      }
      if(this.variantsEnd !=null) {
        boolean bOkBehind = true;
        for(FilepathFilterM variant: this.variantsBegin) {
          return this.bNotBegin;
//          if(name.endsWith(sBefore)) {
//            if(this.bNotEnd) {
//              return false;                              // false if variantBefore matches but with not
//            } else {
//              bOkBehind = true;                          // a matching sBefore found in list.
//              posStartBehind = zName - sBefore.length();
//              break; 
//            }
//          }
        }
      } else if(this.sEnd !=null ) {
        if(!name.endsWith(this.sEnd)) {
          return this.bNotBegin;
        }
      }
      if(this.sContain !=null) {
        if(! name.substring(posEndBegin, posStartEnd).contains(this.sContain)) {
          return this.bNotBegin;                                  // does non contains *contain*
        }
      }
      return ! this.bNotBegin;                                       // all has matched
    }
  }
  
  public boolean check(String name, boolean bDir, FilepathFilterM[] next) { 
    if( this.bAllTree && this.aFilterChild !=null 
     && ( bDir && this.aFilterChild.aFilterChild !=null  // only check the child for bDir if it is not the last child 
       || !bDir && this.aFilterChild.aFilterChild ==null
      ) ) { 
      if( this.aFilterChild.check(name, bDir, next)) {     // the next filter accepts the entry,
        if(next !=null) {
          next[0] = this.aFilterChild.bAllTree ? this.aFilterChild //reuse the used child for the next test.
                  : this.aFilterChild.aFilterChild;        // use the next next child for next test
        }
        return true;                                       // then continue with it. 
      } else if(!bDir) {
        return false;                                      // the next entry must accept the name, if it is the last (!bDir)
      }
    } else {
      if(next !=null) { next[0] = this.aFilterChild; }     // yet guess, use the next filter aFilterChild
    }
    if(!bDir && this.aFilterChild !=null) {
      return false;                                        // a file, it must be the last entry
    }
    int zName = name.length();
    int posEndBegin = this.zBegin;
    int posStartEnd = zName - this.zEnd;
    if(this.zBegin > posStartEnd) {                        // name is to short for the mask sBefore*sBehind
      return this.bNotBegin;                               // returns true if the matching should be false
    } 
    if(this.sBegin !=null ) {
      if(this.bNotBegin == name.startsWith(this.sBegin)) { // bNotBegin && sbegin matches or !bNotBegin and does not match:
        return false;                                      // then faulty.
      }
    }
    if(this.variantsBegin !=null) {
      boolean bOkBeginVariants = false;
      FilepathFilterM[] filterNext = new FilepathFilterM[1];
      //for(String sBefore: this.variantsBegin) {
      for(FilepathFilterM variant: this.variantsBegin) {
        final String sPart = name.substring(posEndBegin, posStartEnd);
        if(variant.check(sPart, bDir, filterNext)) {         // check a variant
            bOkBeginVariants = true;                          // if the variant is matching
            posEndBegin = this.sBegin.length();        // then use it.
            break;
          }
      }
      if(this.bNotBegin == bOkBeginVariants) {
        return false;                                      // positive varinants given, both nothing matches.
      }
      if(filterNext[0]!=null && next !=null) {
        next[0] = filterNext[0];
      }
    }
    boolean bOk = true; //! this.bNotBegin;                      // false if begin has matched
    int posEnd = zName;
    if(this.variantsEnd !=null) {
      boolean bOkEnd = false;
      int mBit = 0x1;
      for(String sVar: this.variantsEnd) {
        if(name.endsWith(sVar)) {                        // end is proper, but:
          if((this.mNotEnd & mBit)!=0) {
            return !bOk; //false;                                // false if variantBefore matches but with not
          } else {                                    
            bOkEnd = true;                               // a matching sBefore found in list.
            posEnd = zName - sVar.length();
            posStartEnd = posEnd - this.zEnd;
            break; 
          }
        }
        mBit <<=1;
      }
      if(!bOkEnd) {
        return !bOk; //false;
      }
    }
    if(this.sEnd !=null ) {
      if(  posStartEnd <0                                // not enough character for sEnd 
       || !name.substring(posStartEnd, posEnd).equals(this.sEnd)
       ) {
        return !bOk;
      }
    }
    if(this.sContain !=null) {
      if(! name.substring(posEndBegin, posStartEnd).contains(this.sContain)) {
        return false;                                    // does non contains *contain*
      }
    }
    if(next !=null && bOk && this.bAllTree) {
      next[0] = this;                                      // reuse this furthermore because it has matched with bAllTree.
    }
    return bOk;                                         // all has matched
  }
  
  
  
  /**Creates a FilepathFilterM with given mask.
   * Examples for filter strings:
   * <ul><li>"path/to/file*ext": dedicated parent path, one wildcard in last children
   * <li>"[dirX|dirY]/**": exact two named directories on level 1, then all files
   * <li>"~[dirX|dirY]/[dirA|dirB]/** /name.*ext": all directories exclusive determined ones, next level only this two determined directories,
   *   then all levels, but on last level name with wildcard.
   * </ul>
   * @param maskP as examples above, for possibilities in each level see {@link FilepathFilterM#FilepathFilterM(String, FilepathFilterM)}
   * @return filter to use for example in TODO
   */
  public static FilepathFilterM createWildcardFilter(String maskP) {
    return createWildcardFilter(maskP, null);
  }
  
  
  
  /**
   * @param maskP
   * @param bEnd true if [internal] is parsed in sEnd range
   * @return
   */
  private static FilepathFilterM createWildcardFilter(String maskP, FilepathFilterM filterChildP) {
    String mask = maskP.replace('\\', '/');
    int zMask = mask.length();
    int pos1 = 0;
    List<String> parts = new LinkedList<String>();
    while(pos1 >=0 && pos1 < zMask) {
      int pos2 = StringFunctions.indexOfAnyCharOutsideQuotation(mask, pos1, zMask, "/", "[", "]", '\\', null);
      if(pos2 <0) { pos2 = zMask; }
      parts.add(0, mask.substring(pos1, pos2));
      pos1 = pos2 +1;  // after '/'
    }
//    int posEnd = zMask;
    FilepathFilterM filter = filterChildP;
    boolean bLast = true;
    for(String part : parts) {
      //======>>>>
      filter = new FilepathFilterM(part, bLast, filter);  // filter =^ before*behind till [ or /
      bLast = false;
    }
//    while(posEnd >0) {
//      int posDir = mask.lastIndexOf('/', posEnd-1);
//      String sChild = mask.substring(posDir+1, posEnd);
//      //======>>>>
//      FilepathFilterM filter = new FilepathFilterM(sChild, bLast, filterChild);  // filter =^ before*behind till [ or /
//      bLast = false;
//      posEnd = posDir;                                  // next child level
//      filterChild = filter;                          
//    }
    
    return filter;
  }


  
  
  
  @Override public Appendable toString(Appendable app, String ... cond) throws IOException {
    if(this.bNotBegin) {
      app.append('~');
    }
    if(this.sBegin !=null) { 
      app.append(this.sBegin); 
    }
    if(this.variantsBegin !=null) {
      char cSep = '[';
      if( this.bNotBegin) { app.append('~'); }
      for(FilepathFilterM variant: this.variantsBegin) {
//      for(String var : this.variantsBegin) {
        app.append(cSep);
        cSep ='|';
        variant.toString(app);
//        app.append(var);
      }
      app.append(']');
      
    }
    
    if(this.bAllTree) { 
      app.append('*');
      if(this.sEnd == null && this.sContain ==null) {
        app.append('*');                                 // "before**" or only "**"
      }
    }
    if(this.sContain !=null) { app.append('*').append(this.sContain); }  // maybe also only "...*" if sBehing ==""
    //
    if(this.sEnd !=null) { 
      app.append('*').append(sEnd); 
    }
    if(this.variantsEnd !=null) {
      char cSep = '[';
      int mBit = 0x1;
      for(String var : this.variantsEnd) {
        app.append(cSep);
        cSep ='|';
        if( (this.mNotEnd & mBit)!=0) { app.append('~'); }
        app.append(var);
        mBit <<=1;
      }
      app.append(']');
      
    }
    if(this.aFilterChild !=null && !this.bLast) {
      app.append('/');
      this.aFilterChild.toString(app);
    }
    return app;
  }
  
  @Override public String toString() {
    StringBuilder sb = new StringBuilder();
    try { toString(sb);} catch(IOException exc) {}
    return sb.toString();
  }
  

  
}
