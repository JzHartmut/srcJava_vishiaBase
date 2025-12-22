package org.vishia.util;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**Filter for a file path maybe with wildcard and include and exclude options (specific multi selection) also in the directory path.
 * <br>
 * For example selecting all files for the FileList but only in dedicated directories and exclusing specifics are written as:
 * <br><code>[[src/**|result/**]/[~#*|~*#]|~_*]</code>
 * <br>it means, files are gotten either from <code>src</code> or <code>result</code> sub directory.
 * But from the root level the valid mask for files is <code>~_*</code>, it means, excluding, should not start with "_".
 * For all deeper levels the files should not start or end with an <code>#</code>.
 * Hence all source files are gathered, but not commented files with <code>#</code>.
 * <ul>
 * <li>{@link #createWildcardFilter(String)}: operation to creates an instance of this class as filter, including sub instances for all sub paths.
 * <li>{@link #check(String, boolean)}: operation to check a file or directory name with the current filter.
 *   It returns the proper child filter to continue check in the child levels or the path. Use it in a loop or for... for the longer path.
 * </ul>
 * Syntax of the path, it is recursively for variants, see ZBNF parser.
 * Note, the ZBNF parser is not used here, only its syntax is used for description. 
 * Writing without spaces. Spaces are (not recommended) part of the name. <pre>
 * filter::={ &lt;filterElement> ? / }.
 * filterElement::= **                        ## This means 'all tree', accepts all directory entries till the next matches. 
 * | [ [~&lt;?bNotBegin>] &lt;*?sBegin> ]           ## optional begin text, ~ if should not match
 *   [ \[ { &lt;filter?variantBegin> ? \| } \] ] ## [...|...] variants to begin optional, can contain ~ for exclude
 *   [ * &lt;*?sContain> * ]                     ## Between two '*' an inner String which should contain
 *   [ *                                      ## after asterisk check of end string 
 *     [ \[ { [~&lt;?mNotEnd>] &lt;*?variantEnd> ? \| } \] ] ## [...|...] variants to end optional
 *     [ &lt;*?sEnd> ]                           ## an end String optional. 
 *   ] .
 * </pre> 
 * <br><br>
 * 
 */
public class FilepathFilterM implements ToStringBuilder {

//  * Note: The {@link java.io.FilenameFilter} is better than the {@link java.io.FileFilter}
//  *   because the {@link java.io.File#list(FilenameFilter)} builds a File instance only if the name is tested positively.
//  *   In opposite the {@link java.io.File#list(FileFilter)} builds a File instance anytime before the test. 
//  *   The difference may be marginal.But {@link java.io.File#list(FileFilter)} produces some more instances in the heap,
//  *   which are unnecessary. 
  
  /**Version, history and license.
   * Changes:
   * <ul>
   * <li>2025-12-21 Hartmut bugfix "Directory" has had the effect of "Directory*", now correct. 
   *   Fixed in {@link #checkRecursive(String, boolean, int[])} if only {@link #sBegin} is given, and {@link #bNoWildcard},
   *   then the length of the input should be equal {@link #zBegin}, or here tested, compare with 'posEnd' which is the length. 
   * <li>2025-12-05 Hartmut more commented and elaborately tested what about variants in [..|..],
   *   necessary (tested for) arguments of the file list for git commit. 
   * <li>2025-11-02 Hartmut {@link #checkRecursive(String, boolean, int[])};
   *   bugfix the last only file entry was used to accept a directory with this name.
   *   Now test in {@link #checkRecursive(String, boolean, int[])} whether it is the last entry and not dir or vice versa,
   *   but now only for the end of variants. Because variants may have not {@link #aFilterChild}.
   * <li>2025-11-02 Hartmut changes, not more as one excluding Strings are possible also with including strings,
   *   means <code>[~dirA*|~dirB]/*]</code> this has not worked before. 
   * <li>2023-07-16 Hartmut new {@link #selAllFilesInDir()}, {@link #selAllDirEntries()}. {@link #selAllEntries()}
   *   It is used for quest delete a directory entry, only if the first two conditions are met.
   *   It checks the given mask.
   * <li>2023-02-14 Hartmut improved: better usable operation {@link #check(String, boolean)}, test is done. 
   *   Adaption in application necessary (since 2 weeks...)  
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
  public final static String sVersion = "2023-07-16";

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
  
  /**bit from 0 for each {@link #variantsEnd} whether it must match (0) or not (1). 
   * See {@link #checkVariantsEnd(String, int)}.
   */
  int mNotEnd;
  
  
  /**This list is not null if more as one varieties are exsiting,
   * separated by [var1|var2]
   */
  //List<FilepathFilterM> filterVariety;
  
  //FilepathFilterM afterVarieties;

  /**Creates a FilepathFilterM for one level. 
   * protected, use {@link #createWildcardFilter(String)}.
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
   * @param filterChild null or a child filter for the next level. Note: Parsing is done backward.
   */
  protected FilepathFilterM ( String sMask, boolean bLast, FilepathFilterM filterChild) { 
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

  
  /**Variants are given in sPart in [...|...]. Whereby inside variants also variants are possible: '[.../[...|...]/xx|...]'
   * <ul><li>reads out each variant with detection matching [ | ]
   * <li> call {@link #createWildcardFilter(String, FilepathFilterM)} recursively for each found variant. 
   * <li> adds the variants {@link FilepathFilterM} to the list, to store in the parents {@link #variantsBegin}.
   * @param sPart The given textual part with variants till posBracket.
   * @param posBracket the end of the given variants in sPart (behind some other parts follow)
   * @param list The list to add the variants. 
   * @param filterChild The given {@link FilepathFilterM} for children in the path (Note: parsing is processed backward)
   * @return not used
   */
  private static void parseVariants(String sPart, int posBracket, List<FilepathFilterM> list, FilepathFilterM filterChild) {
    // search ] but not inside a quotation with [...]
    //if(sPart.startsWith("[~docuSrc*")) Debugutil.stopp();
    int posEndBracket = StringFunctions.indexOfAnyCharOutsideQuotation(sPart, posBracket+1, -1, "]", "[", "]", '\\', null);
        //sPart.indexOf(']');
    if(posEndBracket <0) { posEndBracket = sPart.length(); }
    int posVariety = posBracket +1;                    // check [variety|...
    while(posVariety < posEndBracket) {                // do not enter if no [ or ] found.  
      //int posEndVariety = sPart.indexOf('|', posVariety);
      int posEndVariety = StringFunctions.indexOfAnyCharOutsideQuotation(sPart, posVariety, -1, "|", "[", "]", '\\', null);;
      if(posEndVariety <0) { posEndVariety = posEndBracket; }
      String sVariant = sPart.substring(posVariety, posEndVariety);
      //
      //======>>>>                                         // recursively call of the variants filter.
      FilepathFilterM variant = createWildcardFilter(sVariant, filterChild);
      list.add(variant);
      posVariety = posEndVariety +1;
    }
  }
  
  
  
  /**Similar as {@link #parseVariants(String, int, List, FilepathFilterM)} but only for end.
   * The end cannot contain directories, hence list is only a String.
   * @param sPart used after posBracket
   * @param posBracket the position of the opening bracket for the variants in sPart
   * @param list
   * @return
   */
  private static int parseVariantsEnd(String sPart, int posBracket, List<String> list) {
    int posEndBracket = sPart.indexOf(']');
    if(posEndBracket <0) { posEndBracket = sPart.length(); }
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
      }                                                    //vv add the variant string.
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
  
  
  
   /**Checks the given name against the current Level of this FilepathFilterM.
   * <br>If it is a directory, the current filter is "**" and the next directory level matches, 
   * then it uses the next level.
   * <br>If it is a directory, the current filter is "**" and the next directory level does not match,
   *   it uses the "**", means matches, and returns also the current level this itself for next level.
   *    
   * @param name The name of a file or directory to check.
   * @param bDir true then it is a directory name what is to check.
   * @return null if the filter does not match, or the next level to use for the next entry.
   *   On a non directory entry this return value should not be used, it marks only !=null, it is not used on a leaf.  
   */
  public FilepathFilterM check(String name, boolean bDir) { //, FilepathFilterM[] next) { 
    int posEndBegin[] = new int[1];
    return checkRecursive(name, bDir, posEndBegin);
  }
  
  
  
  /**This is the essential check operation:
   * <ul>
   * <li>_A_ If {@link #bAllTree} is set, then this 'name' is tested with the {@link #aFilterChild}.
   *   <ul>
   *   <li>_A1_ If {@link #aFilterChild} matches, it is the child. 
   *     Then the return value of the recursively call is returned as filter for the next path entry. 
   *   <li>_A2_ If {@link #aFilterChild} does not match, and 'name' is a directory, 
   *     then name is matching, because {@link #bAllTree} matches.
   *     Then this is returned as next filter to check the next path entry.
   *   <li>_A3_ If {@link #aFilterChild} does not match but 'name' is not a directory, then it returns null for not matching.
   *     The {@link #bAllTree} flag is only valid for directory entries as 'name'
   *   </ul>
   * <li>_B_ If {@link #bAllTree} is not set, then the internal 'nextf' is set to the {@link #aFilterChild} or to 'this'
   *   if 'name' is a file (not directory, 'bDir' == false.
   *   This value is prepared for return if all tests matches. 
   *   If tests are not matching, null is returned.  
   * <li>_C_ If {@link #variantsBegin} are not given, then the 'bDir' of name should matching to {@link #aFilterChild}:
   *   A directory need {@link #aFilterChild}, a file must not have {@link #aFilterChild}. If this is not proper, returns null.
   *   But if {@link #variantsBegin} are given, {@link #aFilterChild} can be null, because the variants contains the filterchild.
   *   Hence 'bDir' is then not evalueated.
   * <li>The tests of {@link #zBegin} and {@link #zEnd} shortens the middle part of 'name'.
   * <li>_D_ If {@link #bNotBegin} is given and the {@link #sBegin} is longer then 'name' without {@link #zEnd}, 
   *   then it is ok, the {@link #sBegin} does not match, the test goes on. 
   *   But if {@link #bNotBegin} is false, the test failes, return null.
   * <li>_E_ If {@link #sBegin} is given, name without {@link #zEnd} is tested.
   *   returns false if is ok but {@link #bNotBegin} is set, or if not ok and {@link #bNotBegin} is false.
   *   Else continue with further tests.
   * <li>_F_ If {@link #variantsBegin} is given, then all variants are tested with the name part after {@link #zBegin}
   *   till {@link #zEnd}. 
   *   This is done with recursively call of this operation via {@link #checkVariants(String, boolean, int[], int)}.
   *   return value null of this call returns with null, it does not match. 
   *   The detailed {@link #bNotBegin} etc. are done in the recursion.
   * <li>_G_ If {@link #variantsEnd} are given, they are checked via {@link #checkVariantsEnd(String, int)}.
   *   If this operation returns null, return null.   
   * <li>_H_ If {@link #sEnd} is given, it should match {@link #zEnd} == 0, then not relevant.
   * <li>_J_ at last {@link #sContain} is test with the remaining middle part.
   * </ul>
   * @param name Name of the entry in path, either a directory or file name.
   * @param bDir true if 'name' is a directory entry
   * @param posEndBegin position to test 'name', >0 if this is called as inner variant test.
   * @return The next {@link FilepathFilterM} to test the next entry in the path.
   *   <ul><li>It is 'this' if {@link #bAllTree} matches. It is 'this' only to have a value !=null if not 'bDir'. 
   *   the last check does not call more checks because it is the last entry. 
   *   <li>It is {@link #aFilterChild} on matching the current entry.#
   *   <li>It is null for non matching.
   *   </ul>
   */
  private FilepathFilterM checkRecursive(String name, boolean bDir, int posEndBegin[]
  ) { //, FilepathFilterM[] next) { 
    FilepathFilterM nextf;
    if( this.bAllTree                            //--------vv it is for '**'
     && this.aFilterChild !=null                           // && Filter contains aFilterChild, it is for a directory:
     && ( bDir && this.aFilterChild.aFilterChild !=null    // only check the child for bDir if it is not the last child 
       || !bDir && this.aFilterChild.aFilterChild ==null   // or the entry after "**/aFilterChild" has not a child
      ) ) {
      nextf = this.aFilterChild.checkRecursive(name, bDir, posEndBegin);         // then use the child filter to check the entry 
      if( nextf !=null ) {                                 // if the child filter accepts the entry,
        return nextf;                                      // then continue with it. On a file as especially also on a directory 
      } else if(!bDir) {
        return null;                                       // the next entry must accept the name, if it is the last (!bDir)
      } else {
        nextf = this;                                      // the next entry was not accepted, it means this with "**" is valid.
      }
    } else {                                               // it is not "**"
      nextf = !bDir ? this :                               // default return on positive test is for !bDir  !=null (leaf)  
              this.bAllTree ? this : this.aFilterChild;    // use normally child for next, but for "**" this itself.
    }
    if(this.variantsBegin ==null) {              //---------- _C_ only for the end of variants, test file or directory
      if(!bDir && this.aFilterChild !=null) {
        return null;                                       // a file, it must be the last entry in the filter queue
      }
      if(bDir && this.aFilterChild ==null) {
        return null;                                       // a dir, it must not be the last entry in the filter queue
      }
    }
    int zName = name.length();
    int posStartEnd = zName - this.zEnd;
    if(this.zBegin > posStartEnd) {                        // _D_ name is to short for the mask sBefore*sBehind, cannot match
      if(this.bNotBegin) {                                 // should not match, hence returns true, with used length unchanged
      } else {  
        return null;                                      // should match, hence return null
      }
    } 
    if(this.zBegin >0 ) {                                  //_E_ check sBegin
      boolean bMatchBegin = name.startsWith(this.sBegin); 
      if(this.bNotBegin == bMatchBegin) {                  // bNotBegin && sbegin matches or !bNotBegin and does not match:
        return null;                                       // then faulty.
      } else {
        posEndBegin[0] = this.zBegin;
      }
    }
    if(this.variantsBegin !=null) {                        //_F_ variants given on begin, test it.
      nextf = checkVariants(name, bDir, posEndBegin, posStartEnd); //The variants contains the next filter 
      if(nextf == null) {               // ^- updated on the posBegin inside the variant. 
        return null;
      }
    }
    int posEnd = zName;
    if(this.variantsEnd !=null) {                          //_G_ "*[end1|end2|...]" given
      int posEndVariant = checkVariantsEnd(name, zName);
      if(posEndVariant <0) {                               // posEndVariant is the position in name before fond [end1|end2|...]
        return null;                                       // -1 then checkVariantsEnd does not match, return null
      }
      posEnd = posEndVariant;
      posStartEnd = posEndVariant - this.zEnd;
    } else {
      posEnd = zName;
    }
    if(this.zEnd >0) {                           //--------vv _H_ check end
      if(  posStartEnd <0                                  // not enough character for sEnd 
       || !name.substring(posStartEnd, posEnd).equals(this.sEnd)
       ) {
        if(this.bNotBegin) {                     //--------<< end test does not match and should not match, do nothing. 
        } else {
          return null;                           //<<<<====== end test does not match, should match, return null
        }
      } else {
        posStartEnd = zName - this.sEnd.length();
      }
    } else {                                     //--------^^ vv no check end.
      posStartEnd = zName;
    }                                            //--------^^ check end, posStartEnd is set.
    //
    if(this.sContain !=null) {                   //--------vv _J_
        if(! name.substring(posEndBegin[0], posStartEnd).contains(this.sContain)) {
            return null;                                    // does non contains *contain*
        } else {
            return nextf;
        }
    } else if(!this.bNoWildcard) {
        posEndBegin[0] = zName;    // the rest is wildcard
        return nextf;
    } else if(posEndBegin[0] < posEnd) {         //--------vv bNoWildcard is set, then posEndBegin[0] == posEnd ?
        return null;                                       // if lesser, then it is false.
    } else {
        return nextf;                                         // all has matched
    }
  }
  
  
  
  /**returns true if this filter will select all files asked from a dir level. It is adequate to the file filter "xx/*".
   * It tests the last file level, not the directory level (it means #aFilterChild).
   * The directory level should be given but as the last. Means "dirLevel/*"
   * @return false if a child filter condition is given which may exclude files.
   */
  public boolean selAllFilesInDir ( ) {
    return this.aFilterChild !=null                        // it is an dir entry
        && this.aFilterChild.aFilterChild == null          // it is the last dir entry
        && this.aFilterChild.selAllEntries();              // all files in child are selected
  }
  
  
  /**Returns true if this filter selects all sub directories till end of tree.
   * @return false also if there may be a sub tree which is not selected. true if "** /fileFilter"
   */
  public boolean selAllDirEntries ( ) {
    return this.aFilterChild !=null                  // it is a dir entry, has a child filter 
        && this.aFilterChild.aFilterChild == null    // it is the last dir entry
        && this.selAllEntries();                     // all dir entries are selected.
  }
  
  /**returns true if this filter will select all entries of this level. 
   * It is adequate "*" whereby more levels can follow, means "* /moreLevels"
   * Also "**" is included, because the second '*' means {@link #bAllTree}.
   * @return false if a filter condition is given which may exclude files.
   */
  public boolean selAllEntries ( ) {
    return (this.sBegin == null || this.sBegin.length()==0) 
        && (this.sEnd ==null || this.sEnd.length()==0) && this.sContain ==null
        && this.variantsBegin ==null && this.variantsEnd ==null;
  }
  
  
  
  
  private FilepathFilterM checkVariants ( String name, boolean bDir, int[] posEndBegin, int posStartEnd) {
    boolean bOkBeginVariants = false;
    boolean bCheckFalse = false;
    boolean bCheckFalseAll = true;
    boolean bCheckPositive = false;
    FilepathFilterM nextf2 = null;
    FilepathFilterM nextfok = null;
    FilepathFilterM nextfPositive = null;
    int posEndBeginGiven = posEndBegin[0];                 // given on call, it is the length before this variants, often 0
    int posEndBeginOk = -1;
    int posEndBeginOkPositive = -1;
    final String sPart = name.substring(posEndBeginGiven, posStartEnd);
    for(FilepathFilterM variant: this.variantsBegin) {
      //if(name.startsWith("+")) Debugutil.stopp();
      //if(name.equals("fbg") && this.toString().startsWith("[~docuSrc*|~asciidoc-gen]/**/[~#*|~*#]")) Debugutil.stopp();
      posEndBegin[0] = posEndBeginGiven;                   // as given on call
      bCheckFalse |= variant.bNotBegin;  // true then check all variants, if one returns null, it is false.
      //
      //======>>>>
      nextf2 = variant.checkRecursive(sPart, bDir, posEndBegin);                 //<<<<====== check the variant
      //
      if(bCheckFalse) {                   // it is a not variant which does not match, means it is one of the not matching variants:
        bCheckPositive |= !variant.bNotBegin;              //<<-- true if at least one variant found with postive check.
        if(variant.bNotBegin && nextf2 == null) { //-------vv one variant with bNotBegin has not matched, abort all.
          nextfok = nextfPositive = null;
          break;                                             // not matching variant found, it does not match. 
        } else if(nextf2 !=null) {               //--------vv candidate which matches. Either (the last) with bNotBegin, or one of the positive variants.
          if(bCheckPositive) {
            nextfPositive = nextf2;                        // use it if all negative variants are ok
            posEndBeginOkPositive = variant.sBegin.length();         // may use it.
          } else {
            nextfok = nextf2;
            posEndBeginOk = posEndBegin[0];                // as returned from 'variant.check'.
          }
        }
      } else {                         //------------------vv only positive tests, the first which matches is taken. 
        bCheckPositive = true;
        if(nextf2 !=null) {         // check a variant
          nextfPositive = nextf2;                                  // candidate to match, but test all variant.bNotBegin
          posEndBeginOkPositive = variant.sBegin.length();           // may use it.
          break;
        }
      }
    }
    if(bCheckPositive) {
      nextfok = nextfPositive;                             // maybe 0
      posEndBeginOk = posEndBeginOkPositive;               // undetermined if nextof == null
    }
    if(nextfok !=null) {
      posEndBegin[0] = posEndBeginOk;
    } else {
      posEndBegin[0] = posEndBeginGiven;                   // as given on call
    }
    return nextfok;
  }
  
  
  
  
  private int checkVariantsEnd ( String name, int zName) {
    int mBit = 0x1;
    for(String sVar: this.variantsEnd) {
      if(name.endsWith(sVar)) {                        // end is proper, but:
        if((this.mNotEnd & mBit)!=0) {
          return -1; //false;                                // false if variantBefore matches but with not
        } else {                                    
          return zName - sVar.length();
        }
      }
      mBit <<=1;
    }
    return -1;
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
  
  
  
  /**Creates 
   * @param maskP
   * @param bEnd true if [internal] is parsed in sEnd range
   * @return
   */
  private static FilepathFilterM createWildcardFilter(String maskP, FilepathFilterM filterChildP) {
    String mask = maskP.replace('\\', '/');
    int zMask = mask.length();
    int pos1 = 0;
    List<String> parts = new LinkedList<String>();
    while(pos1 >=0 && pos1 < zMask) {            //--------vv separates folder till '/' and adds backward in parts. 
      int pos2 = StringFunctions.indexOfAnyCharOutsideQuotation(mask, pos1, zMask, "/", "[", "]", '\\', null);
      if(pos2 <0) { pos2 = zMask; }                        //path[~*.bak]/to/xy results in 'xy', 'to', path[~*.bak]
      parts.add(0, mask.substring(pos1, pos2));
      pos1 = pos2 +1;  // after '/'
    }
//    int posEnd = zMask;
    FilepathFilterM filter = filterChildP;
    boolean bLast = true;
    for(String part : parts) {                   //--------vv builds with the given parts the filter
      //======>>>>                                         // with recursively call for the filter from right to left
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


  
  
  
  @Override public StringBuilder toString(StringBuilder app, String ... cond) {
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
    if(this.sContain !=null) { app.append('*').append(this.sContain); }  // maybe also only "...*" if sBehind ==""
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
    return toString(sb).toString();
  }
  

  
}
