package org.vishia.util;

import java.io.File;

/**The routines of this class are used to compare Strings especially for tests.
 * The capability is skip whitespaces, skip comments in both texts.
 * @author Hartmut Schorrig, LPGL-License, Special second license possible.
 *
 */
public class StringCmp {

  
  public static String version = "2020-03-21";
  
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
      boolean bok = true;
      while(ix1[0] < z1) {
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

  
  
  private static char readNextChar(CharSequence cs, int zcs, int[] ix, boolean bWhitespace
      , String endlineComment, String commentStart, String commentEnd, int recursive ) {
    assert(ix[0] < zcs);  //ix[0] should be tested.
    if(recursive >2) throw new RuntimeException("recursion"); //abort independent of assertion mode
    char cc = cs.charAt(ix[0]++);  
    boolean bSkipped;
    do {
      bSkipped = false;
      if(cc == '(')
        Debugutil.stop();
      if(cc == endlineComment.charAt(0) && (ix[0] + endlineComment.length()) <= zcs 
          && StringFunctions.equals(cs, ix[0]-1, ix[0]-1+endlineComment.length(), endlineComment)) {
        int[] whichChar = {0};
        int ixCommentEnd = StringFunctions.indexOfAnyChar(cs, ix[0] + commentStart.length(), -1, "\r\n", whichChar)+1;
        ix[0] = ixCommentEnd;
        if(whichChar[0] == 0 && ix[0] < zcs && cs.charAt(ix[0]) == '\n') {
          ix[0] +=1;  //skip \n after \r
        }
        cc = ix[0] < zcs ? cs.charAt(ix[0]++) : '\0';
        bSkipped = true;
      }
      if(cc == commentStart.charAt(0) && (ix[0] + commentStart.length()) <= zcs 
          && StringFunctions.equals(cs, ix[0]-1, ix[0]-1+commentStart.length(), commentStart)) {
        int ixCommentEnd = StringFunctions.indexOf(cs, commentEnd, ix[0] + commentStart.length());
        if(ixCommentEnd >=0) {
          ix[0] = ixCommentEnd + commentEnd.length();
          cc = ix[0] < zcs ? cs.charAt(ix[0]++) : '\0';
          bSkipped = true;
        }
      }
      if(bWhitespace && sWhitespace.indexOf(cc)>=0) {
        while( ix[0] < zcs //skip all whitespaces after them, skip all comments and enline comment too, hence recursively 
            && sWhitespace.indexOf(        //in recursion do not detect whitespace, elsewhere it would be recursively again.
                 readNextChar(cs, zcs, ix, false, endlineComment, commentStart, commentEnd, recursive +1)
                                      )>=0 
              );  //incremented ix[0]
        if(ix[0] < zcs) { ix[0] -=1; } //backward to the last character, it is not a whitespace if ix[0] is not on end.
        cc = ' ';  //use a space for comparison.
      }
    } while(bSkipped);
    return cc;
  }
  
  
}
