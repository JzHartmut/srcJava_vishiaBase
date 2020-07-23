package org.vishia.util;

public class StringFunctions_B
{
  
  /**Version, history and license.
   * <ul>
   * <li>2020-07-22 Hartmut new {@link #checkOneSameChars(CharSequence, CharSequence...)} 
   *         and {@link #checkMoreSameChars(CharSequence, CharSequence...)} as enhancement to 2016-activity
   *         used in {@link org.vishia.simSelector.SimSelector} in the script for selection.
   * <li>2016-01-10 Hartmut new: {@link #checkSameChars(CharSequence...)} 
   * <li>2015-11-07 Hartmut created: The functionality to remove indentation was used in JZcmdExecuter.
   *   Now it is implemented here for common usage.
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
   */
  public final static String version = "2020-07-22"; 

  public static final String sWhiteSpaces = " \r\n\t\f";
  




  /**Returns the first line of any text.
   * @param src if null then returns "" (empty String)
   * @return returns src exclusive a "\r" or "\n"
   */
  public static String firstLine(CharSequence src) {
    if(src == null) return "";
    int pos = StringFunctions.indexOfAnyChar(src, 0, Integer.MAX_VALUE, "\n\r");
    if(pos <0) { pos = src.length(); }
    return src.subSequence(0, pos).toString();
  }




  
  /**Cleans a text which may be parsed or such, remove undesired indentation and replace the line end characters. 
   * @param src Any source String with indentation
   * @param indent column which indentation should be removed
   * @param IndentChars some Characters which are expected in the indentation area. 
   * @param tabSize User tab size to prevent tabs as indentation
   * @param sNewline String as newline designation, usual "\n", "\r\n" or "\r".
   * @param bSkipSpaces true then skip over the first whitespace characters
   * @return either src if it does not contain a newline character and does not contain prevented whitespaces
   *   or a StringBuiler which contains the result.
   */
  public static CharSequence removeIndentReplaceNewline(CharSequence src, int indent, String indentChars
      , int tabSize, String sNewline, boolean bSkipSpaces) {
    int zText = src.length();
    char cEnd = '\n';  
    int posEnd1 = StringFunctions.indexOf(src, cEnd, 0);
    int posEnd2 = StringFunctions.indexOf(src, '\r', 0);   //a \r\n (Windows standard) or only \r (Macintosh standard) in the script is the end of line too.
    if(posEnd1 <0 && posEnd2 < 0 && (!bSkipSpaces || zText>0 && sWhiteSpaces.indexOf(src.charAt(0))<0)) {
      return src; //not necessary to process
    } else {
      StringBuilder b = new StringBuilder(zText); //for the result.
      boolean bSkipSpaces1 = bSkipSpaces;
      int posLine = 0;
      do{
        posEnd1 = StringFunctions.indexOf(src, cEnd, posLine);
        posEnd2 = StringFunctions.indexOf(src, '\r', posLine);   //a \r\n (Windows standard) or only \r (Macintosh standard) in the script is the end of line too.
        if(posEnd2 >= 0 && (posEnd2 < posEnd1 || posEnd1 <0)){
          posEnd1 = posEnd2;  // \r found before \n
          cEnd = '\r';
        }
        if(posEnd1 >= 0){ 
          if(bSkipSpaces1) {
            while(posLine <posEnd1 && sWhiteSpaces.indexOf(src.charAt(posLine))>=0) {
              posLine +=1;
            }
            if(posLine < posEnd1) { //anything found in the line:
              bSkipSpaces1 = false;
            }
          }
          if(posLine < posEnd1) {
            b.append(src.subSequence(posLine, posEnd1));  
          }
          if(!bSkipSpaces1) { //don't append a newline if skipSpaces is still active. Then only spaces were found.
            b.append(sNewline);  //use the newline from argument.
          }
          //skip over posEnd1, skip over the other end line character if found. 
          if(++posEnd1 < zText){
            if(cEnd == '\r'){ if(src.charAt(posEnd1)=='\n'){ posEnd1 +=1; }}  //skip over both \r\n
            else            { if(src.charAt(posEnd1)=='\r'){ posEnd1 +=1; }}  //skip over both \n\r
            //posEnd1 refers the start of the next line.
            int indentCt = indent;
            char cc = 0;
            while(indentCt > 0 && posEnd1 < zText 
              && ((cc = src.charAt(posEnd1)) == ' ' || cc == '\t' || indentChars.indexOf(cc) >=0)
              ) {
              if(cc == '\t'){
                indentCt -= tabSize;
                  if(indentCt >= 0) { //skip over '\t' only if matches to the indent.
                  posEnd1 +=1;
                }
              } else {
                posEnd1 +=1; //skip over all indentation chars
                indentCt -=1;
              }
            }
            if(indentChars.indexOf(cc) >=0) { //the last skipped char was an additional indentation char:
              while(posEnd1 < zText && src.charAt(posEnd1) == cc) {
                posEnd1 +=1;  //skip over all equal indentation chars.
            } }
            //line starts after :::: which starts before indentation end
            //or line starts after first char which is not a space or tab
            //or line starts on the indent position.
          }
          posLine = posEnd1;
        } else { //the rest till end.
          b.append(src.subSequence(posLine, zText));   
        }
        
      } while(posEnd1 >=0);  //output all lines.
      return b;
    }      
  }
  
  
  
  /**Checks whether any char is existing in all given src.
   * This routine is used to check some conditions which are dedicated by some characters in a string.
   * <ul>
   * <li>For Example "ACx" " BC" "Cd" contains all the char 'C' therefore this routine returns true.
   * <li>For Example "ACx" "AC"  "Dx" There is no common character containing in all three sequences, returns false. 
   * @param src some char sequences
   * @return true if at least one char is found which is contained in all src.
   * @TODO Java2C: Yet without threadcontext because bug with variable argument list
   */
  @Java4C.NoStackTrace @Java4C.Exclude public static boolean checkSameChars(CharSequence ... src)
  {
    boolean ok = false;
    CharSequence cmp = null;
    for(CharSequence src1: src){   //search any of inputs where any key chars are contained:
      if(src1.length()>0) { cmp = src1; break; }
    }
    
    if(cmp == null) {
      ok = true;  //no input with key chars, then ok.
    } else {
      for(int ix = 0; ix < cmp.length(); ++ix) {
        char cTest = cmp.charAt(ix);
        boolean bOk1 = true;
        for(CharSequence src1: src) {
          if(  src1.length() >0      //contains any key 
            && StringFunctions.indexOf(src1, cTest) < 0) {  //this is not a common key
            bOk1 = false;           //then break;
            break;
          }
        }
        if(bOk1) {  //a key found at all:
          ok = true; break;  //then it is ok.
        }
      }
    }
    //all checked, not found, then ok is false.
    return ok;
  }
  
  
  
  
  
  /**Checks whether any char is existing in at least one given check Strings.
   * This routine is used to check some conditions which are dedicated by some characters in a string.
   * <ul>
   * <li>For Example "AC" " Axyz" "Cmno" both contains either the char 'A' or the char 'C', therefore this routine returns true.
   * <li>For Example "AB" " Axyz" "Cmno" The char 'B' is not found,  returns false. 
   * @param src some chars to check
   * @param check some strings where the check chars should be found. 
   * @return true if at least one char is found which is contained in all src.
   * @TODO Java2C: Yet without threadcontext because bug with variable argument list
   */
  @Java4C.NoStackTrace @Java4C.Exclude public static boolean checkOneSameChars(CharSequence src, CharSequence ... check)
  {
    boolean ok = true;
//    CharSequence cmp = null;
//    for(CharSequence src1: src){   //search any of inputs where any key chars are contained:
//      if(src1.length()>0) { cmp = src1; break; }
//    }
    
    if(src == null || src.length() == 0) {
      ok = true;  //no input with key chars, then ok.
    } else {
      int found = 0;
      int ixCheck = 0;
      for(CharSequence src1: check) { //Check if at least one char is contained in check.
        for(int ix = 0; ix < src.length(); ++ix) { //check all given check sources:
          char cTest = src.charAt(ix);
          if(  src1.length() >0      //contains any key 
            && StringFunctions.indexOf(src1, cTest) >= 0  //this is not a common key
            ) {
            found |= 1<<ixCheck;
            break;
          } 
        }
        ixCheck +=1;
      }
      int cmp = (1<<ixCheck) -1;
      ok = found == cmp;  //all check fields have at least one char from src.
    }
    //all checked, not found, then ok is false.
    return ok;
  }
  
  
  @Java4C.NoStackTrace @Java4C.Exclude public static boolean checkMoreSameChars(CharSequence src, CharSequence ... check)
  {
    boolean bOk = false;
    if(src !=null) {
      String[] src1 = src.toString().split(":");
      for(String src2: src1) {
        if(checkOneSameChars(src2, check)) {
          bOk = true;
          break;
        }
      }
    }
    return bOk;
  }

  
  /**Build a Java Identifier from a given String, maybe tag name in Xml etc.
   * @param src Any String. Non-conform characters will be replaced by '_'
   * @param lowerOrUpper if'a' or 'A' converts the first char to lower or upper. If '-' do not convert.
   * @return src if src is conform (less effort) else new conform CharSequence in a StringBuilder.
   */
  public static CharSequence replaceNonIdentifierChars(CharSequence src, char lowerOrUpper) {
    
    int zChars = src.length();
    if(zChars == 0) return "_Undefined_";
    char cc = src.charAt(0);
    boolean isIdentiferOk = true;
    if(  Character.isLowerCase(lowerOrUpper) && !Character.isLowerCase(cc)
      || Character.isUpperCase(lowerOrUpper) && !Character.isUpperCase(cc)
      || !Character.isJavaIdentifierStart(cc)
        ) {
      isIdentiferOk = false;
    } else {
      for(int ix = 0; ix<zChars; ++ix) {
        if(!Character.isJavaIdentifierPart(src.charAt(ix))) {
          isIdentiferOk = false;
          break;
        }
      }
    }
    if(isIdentiferOk) return src;  //unchanged, typical case, no effort.
    else {
      StringBuilder dst = new StringBuilder(zChars);
      if( !Character.isJavaIdentifierStart(cc)) {
        cc = '_';
      }
      else if( Character.isLowerCase(lowerOrUpper) && !Character.isLowerCase(cc)) {
        cc = Character.toLowerCase(cc);
      } else if(Character.isUpperCase(lowerOrUpper) && !Character.isUpperCase(cc)) {
        cc = Character.toUpperCase(cc);
      }
      dst.append(cc);
      for(int ix = 1; ix<zChars; ++ix) {
        cc = src.charAt(ix);
        if(!Character.isJavaIdentifierPart(cc)) {
          cc = '_';
        }
        dst.append(cc);
      }
      return dst;
    }
  }
  
  
  /**Converts the first character to lower case if it {@link Character#isUpperCase(char)} ('A'..'Z').
   * If the src starts with '_' or it is not an identifier, src will be returned.
   * The rest of the src is left unchanged.
   * Use {@link #replaceNonIdentifierChars(CharSequence, char)} to convert maybe non identifier words to identifier words.
   * @param src
   * @return
   */
  public static String toFirstLowercase(String src) {
    char c0 = src.charAt(0);
    if(Character.isUpperCase(c0)) { return Character.toLowerCase(c0) + src.substring(1); }
    else return src;
  }
  
  
  /**Converts the first character to upper case if it {@link Character#isLowerCase(char)} ('a'..'z').
   * If the src starts with '_' or it is not an identifier, src will be returned.
   * The rest of the src is left unchanged. 
   * Use {@link #replaceNonIdentifierChars(CharSequence, char)} to convert maybe non identifier words to identifier words.
   * @param src
   * @return
   */
  public static String toFirstUppercase(String src) {
    char c0 = src.charAt(0);
    if(Character.isLowerCase(c0)) { return Character.toUpperCase(c0) + src.substring(1); }
    else return src;
  }
  
  
  
}
