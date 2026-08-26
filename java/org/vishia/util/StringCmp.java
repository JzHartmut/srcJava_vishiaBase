package org.vishia.util;

import java.io.File;

/**The routines of this class are used to compare Strings especially for tests.
 * The capability is skip whitespaces, skip comments in both texts.
 * @author Hartmut Schorrig, LPGL-License, Special second license possible.
 *
 */
public class StringCmp {

  
  /**Version, history and license.
   * <ul>
   * <li>2026-08-25 bugfix important: If the last line was ending with an end comment and no newline was found,
   *   then comparison has start on beginning. A rarely big error. Fixed. 
   * <li>2022-07-27 only source tree moved (?)
   * <li>2020-03-23 enhancements, white space detection, commented.
   * <li>2020-03-13 Some enhancements, especially return int instead bool with the position of the first difference.
   * <li>2020-02-15 Created
   * </ul>
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
   */
  public static String version = "2026-08-25";
  
  static String sWhitespace = " \t\r\n\f";
  
  static String sEndline = "\n\r";
  
  
  /**Compare one file contents with chars, see {@link #compare(CharSequence, CharSequence, boolean, String[])}
   */
  public static int compare(File f1, CharSequence s2
      , boolean bWhitespace, String[] comment) {
    CharSequence s1 = FileSystem.readFile(f1);
    return compare(s1, s2, bWhitespace, comment);
  }
  
  
  /**Compare two file contents, see {@link #compare(CharSequence, CharSequence, boolean, String[])}
   */
  public static int compare(File f1, File f2
      , boolean bWhitespace, String[] comment) {
    CharSequence s1 = FileSystem.readFile(f1);
    CharSequence s2 = FileSystem.readFile(f2);
    return compare(s1, s2, bWhitespace, comment);
  }
  
  
  
  /**Compares two Strings with possibility of ignore white spaces and comments
   * @param s1
   * @param s2
   * @param bWhitespace true accept one space to compare but skip over " \t\r\n\f"
   * @param comment The 3 entries are: 
   *   <br>[0]start sequence for endline comment
   *   <br>[1]start sequence for comment
   *   <br>[2]end sequence for comment
   *   <br>For C-like comment it is <code>{"//", "/*", "* /"}</code>
   * @return -1 if equal, else the character position of error
   */
  public static int compare(CharSequence s1, CharSequence s2
      , boolean bWhitespace, String[] comment) {
    if(s1 == null || s2 == null) {
      return s1==s2 ? -1: 0; //both 0 then equal, return -1
    }
    else {
      int dbgStop = 0;      //set manually to force stop
      String endlineComment = comment[0];
      String commentStart = comment[1];
      String commentEnd = comment[2];
      int z1 = s1.length();
      int z2 = s2.length();
      int[] ix1= {0}, ix2= {0};
      int ix1z = -1, ix2z = -1;
      boolean bok = true;
      while(ix1[0] < z1) {
        if(ix1z < ix1[0]) { ix1z = ix1[0]; }
        else { Debugutil.stopp(); assert(false); bok = false; throw new IllegalArgumentException("ERROR in compare"); }
        if(ix2z < ix1[0]) { ix2z = ix1[0]; }
        else { Debugutil.stopp(); assert(false); bok = false; throw new IllegalArgumentException("ERROR in compare"); }
        
        //if(ix1z == 7100) Debugutil.stopp();
        if(dbgStop >0 && ix1[0] == dbgStop) {
          dbgStop +=0;  //set breakpoint here
        }
        char c2 = '\0';
        char c1 = readNextChar(s1, z1, ix1, bWhitespace, endlineComment, commentStart, commentEnd, 0);
        if(ix2[0] >= z2) {
          bok = ix1[0] >= z1; break;  //end of s2, true if end of s1 too.
        } else {
          c2 = readNextChar(s2, z2, ix2, bWhitespace, endlineComment, commentStart, commentEnd, 0);
        }
        if(c1 !=c2) { 
          bok = false; break; //not equals
        }
      }
      if(ix2[0] < z2) {
        bok = false;  //s2 is longer than s1
      }
      return bok ? -1 : ix1[0];
    }
  }

  
  
  /**Get the next character from the charsequence, but consider comment, whitespace.
   * 
   * @param cs Charsequence contains the text
   * @param zcs the length of cs
   * @param ix [0] is the current index, returned even by reference here
   * @param bWhitespace true then skip white spaces, on any sequence of white spaces all are skipped and a space is returned.
   * @param endlineComment String for Start endline comment
   * @param commentStart String for comment start
   * @param commentEnd String for comment end
   * @param recursive recursions counter to abort faulty deep nesting only 2 level.
   * @return The next character outside comments, space instead whitespaces
   */
  private static char readNextChar(CharSequence cs, int zcs, int[] ix, boolean bWhitespace
      , String endlineComment, String commentStart, String commentEnd, int recursive ) {
    assert(ix[0] < zcs);  //ix[0] should be tested.
    if(recursive >2) throw new RuntimeException("recursion"); //abort independent of assertion mode
    char cc = cs.charAt(ix[0]++);     //<<--------------------- the next character, valid if not special conditions       
    boolean bSkipped;
    do {
      bSkipped = false;
      if(cc == '(')
        Debugutil.stop();
      if(cc == endlineComment.charAt(0) && (ix[0] + endlineComment.length()) <= zcs  //------vv candidate for end line comment
          && StringFunctions.equals(cs, ix[0]-1, ix[0]-1+endlineComment.length(), endlineComment)) {
        int[] whichChar = {0};                               // end line comment: search the end of line for next char.
        int ixCommentEnd = StringFunctions.indexOfAnyChar(cs, ix[0] + commentStart.length(), -1, "\r\n", whichChar);
        if(ixCommentEnd >=0) {
          ix[0] = ixCommentEnd+1;
          if(whichChar[0] == 0 && ix[0] < zcs && cs.charAt(ix[0]) == '\n') {
            ix[0] +=1;  //skip \n after \r
          }
          cc = ix[0] < zcs ? cs.charAt(ix[0]++) : '\0';        // the next char on start of the next line.
          bSkipped = true;   // repeat here even the check of end line comment for the next line after end line comment.
        } else {                       //--------------------vv comment till end of text, not closed.
          ix[0] = zcs;  
          cc = '\0';
        } 
      }
      if(cc == commentStart.charAt(0) && (ix[0] + commentStart.length()) <= zcs      //----- vv candidate of more line comment
          && StringFunctions.equals(cs, ix[0]-1, ix[0]-1+commentStart.length(), commentStart)) {
        int ixCommentEnd = StringFunctions.indexOf(cs, commentEnd, ix[0] + commentStart.length());
        if(ixCommentEnd >=0) {             //----------------vv end of comment found maybe in line later:
          ix[0] = ixCommentEnd + commentEnd.length();
          cc = ix[0] < zcs ? cs.charAt(ix[0]++) : '\0';
          bSkipped = true;  // repeat here even the first check for end line comment.
        } else {                       //--------------------vv comment till end of text, not closed.
          ix[0] = zcs;  
          cc = '\0';
        }
      }
      if(bWhitespace && sWhitespace.indexOf(cc)>=0) {
        while( ix[0] < zcs //skip all whitespaces after them, skip all comments and enline comment too, hence recursively 
            && sWhitespace.indexOf(cc = cs.charAt(ix[0])) >=0) {
          ix[0] +=1;
        }
//                //in recursion do not detect whitespace, elsewhere it would be recursively again.
//                 readNextChar(cs, zcs, ix, false, endlineComment, commentStart, commentEnd, recursive +1)
//                                      )>=0 
//              );  //incremented ix[0]
//        if(ix[0] < zcs) { ix[0] -=1; } //backward to the last character, it is not a whitespace if ix[0] is not on end.
        cc = ' ';  //use a space for comparison instead white space sequence
        bSkipped = false;    // do not bSkipped=true because comments are tested after white space comparison, the space is the valid returned.
      }
    } while(bSkipped);
    return cc;
  }
  
  
}
