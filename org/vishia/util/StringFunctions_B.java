package org.vishia.util;

public class StringFunctions_B
{
  
  /**Version, history and license.
   * <ul>
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
  public final static String version = "2015-11-07"; 

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
  
  
}
