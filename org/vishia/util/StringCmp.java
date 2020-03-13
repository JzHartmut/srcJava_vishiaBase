package org.vishia.util;

import java.io.File;

public class StringCmp {

  
  static String sWhitespace = " \t\r\n\f";
  
  static String sEndline = "\n\r";
  
  
  public static int compare(File f1, CharSequence s2
      , boolean bWhitespace, String[] comment) {
    CharSequence s1 = FileSystem.readFile(f1);
    return compare(s1, s2, bWhitespace, comment);
  }
  
  
  public static int compare(File f1, File f2
      , boolean bWhitespace, String[] comment) {
    CharSequence s1 = FileSystem.readFile(f1);
    CharSequence s2 = FileSystem.readFile(f2);
    return compare(s1, s2, bWhitespace, comment);
  }
  
  
  
  /**Complares two Strings with possibility of ignore white spaces and TODO: comments
   * @param s1
   * @param s2
   * @param bWhitespace
   * @return -1 if equal, else the character position of error
   */
  public static int compare(CharSequence s1, CharSequence s2
      , boolean bWhitespace, String[] comment) {
    if(s1 == null || s2 == null) {
      return s1==s2 ? -1: 0; //both 0 then equal, return -1
    }
    else {
      String endlineComment = comment[0];
      String commentStart = comment[1];
      String commentEnd = comment[2];
      int z1 = s1.length();
      int z2 = s2.length();
      int[] ix1= {0}, ix2= {0};
      boolean bok = true;
      while(ix1[0] < z1) {
        char c2 = '\0';
        char c1 = readNextChar(s1, z1, ix1, endlineComment, commentStart, commentEnd);
        if(bWhitespace && sWhitespace.indexOf(c1)>=0) {
          while( ix2[0] < z2 //skip all whitespaces in s2
              && sWhitespace.indexOf(c2 = readNextChar(s2, z2, ix2, endlineComment, commentStart, commentEnd))>=0 ); 
          while( ix1[0] < z1 //skip further whitespaces in s1
              && sWhitespace.indexOf(c1 = readNextChar(s1, z1, ix1, endlineComment, commentStart, commentEnd))>=0 ); 
        } 
        if(ix2[0] >= z2) {
          bok = ix1[0] >= z1; break;  //end of s2, true if end of s1 too.
        } else {
          c2 = readNextChar(s2, z2, ix2, endlineComment, commentStart, commentEnd);
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

  
  
  private static char readNextChar(CharSequence cs, int zcs, int[] ix
      , String endlineComment, String commentStart, String commentEnd ) {
    assert(ix[0] < zcs);  //ix[0] should be tested.
    char c1 = cs.charAt(ix[0]++);  
    boolean bSkipped;
    do {
      bSkipped = false;
      if(c1 == '(')
        Debugutil.stop();
      if(c1 == endlineComment.charAt(0) && (ix[0] + endlineComment.length()) <= zcs 
          && StringFunctions.equals(cs, ix[0]-1, ix[0]-1+endlineComment.length(), endlineComment)) {
        int[] whichChar = {0};
        int ixCommentEnd = StringFunctions.indexOfAnyChar(cs, ix[0] + commentStart.length(), -1, "\r\n", whichChar)+1;
        ix[0] = ixCommentEnd;
        if(whichChar[0] == 0 && ix[0] < zcs && cs.charAt(ix[0]) == '\n') {
          ix[0] +=1;  //skip \n after \r
        }
        c1 = ix[0] < zcs ? cs.charAt(ix[0]++) : '\0';
        bSkipped = true;
      }
      if(c1 == commentStart.charAt(0) && (ix[0] + commentStart.length()) <= zcs 
          && StringFunctions.equals(cs, ix[0]-1, ix[0]-1+commentStart.length(), commentStart)) {
        int ixCommentEnd = StringFunctions.indexOf(cs, commentEnd, ix[0] + commentStart.length());
        if(ixCommentEnd >=0) {
          ix[0] = ixCommentEnd + commentEnd.length();
          c1 = ix[0] < zcs ? cs.charAt(ix[0]++) : '\0';
          bSkipped = true;
        }
      }
    } while(bSkipped);
    return c1;
  }
  
  
}
