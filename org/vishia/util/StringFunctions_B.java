package org.vishia.util;

import java.util.LinkedList;
import java.util.List;

public class StringFunctions_B
{
  
  /**Version, history and license.
   * <ul>
   * <li>2021-02-06 Hartmut new {@link #checkSameItem(String, CharSequence...)} and {@link #prepareCheckSameItem(String)}.
   *         This is newly used in the {@link org.vishia.simSelector.SimSelector} in the script for selection.
   *         More ability. 
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
  
  
  static private class NumString {final int nline; final String sel; 
    NumString ( int nline, String sel){ this.nline = nline; this.sel = sel;} 
    @Override public String toString() { return "" + Integer.toString(this.nline) + ":" + this.sel; }
  }

  
  
  /**Checks whether some selection item are existing in all given compare strings.
   * @param select pattern. See syntax on {@link #prepareCheckSameItem(String)}
   * @param cmp some char sequences, which should contain all items.
   * @return true if select matches to the items
   * @Note Java2C: Yet without threadcontext because bug with variable argument list
   */
  @Java4C.NoStackTrace @Java4C.Exclude public static boolean checkSameItem(String select, CharSequence ... cmp)
  {
    List<List<List<List<NumString>>>> selistAll = prepareCheckSameItem(select);
    return checkSameItem(selistAll, cmp);
  }
  
  
  
  /**Prepares the container for comparison, see {@link #checkSameItem(String, CharSequence...)}
   * The select pattern has the following syntax (ZBNF):<pre>
   * select::= { <selAnd> ? : }.  ##multiple independent select pattern, one should match, ':' is a OR 
   * selAnd::= { <selOr> ? & }.   ##select pattern which`s all parts <selOr> should match.
   * selOr::=  { <selLine> ? + }. ##select pattern part which checks some lines, one should match, separated with |
   * selLine::= { [{<#table>?,}=]{<selItem>? , }[;] }. ##select pattern for items in lines. starts optional with one digit line number.
   * selItem::= ... ends either with space or an following upper case character. </pre>     
   * Examples:
   * <ul>
   * <li>"1=Aa 2=Bb" expects "Aa" in the first line and "Bb" in the second line of table resp. cmp[0], cmp[1].
   * <li>"1=Aa,Ab 2=Bb" expects "Aa" or "Ab" in the first cmp and "Bb" in the second cmp.
   * <li>"1=Aa,Ab 2=Bb + Zz" expects as above, or "Zz" in all lines.
   * <li>"1=Aa,Ab 2=Bb + Zz & 3=X" expects as above, but "X" in cmp[2] in any case . 
   *                            If "Zz" is contained in line 3, "X" is not need.
   *                            If only 2 lines exists, "3=X" is effectless
   * <li>"1Aa,Ab 2=Bb + Zz & 3=X : Y" as above, but alternatively also "Y" in all lines is recognized as ok.
   * </ul>
   * @param select text for selection.
   * @return The container parsed from select
   */
  public static List<List<List<List<NumString>>>> prepareCheckSameItem ( String select ) {
    List<List<List<List<NumString>>>> selistAll = new LinkedList<List<List<List<NumString>>>>(); 
    int zSelAll = select.length();
    int ixSelAll = 0;
    while(ixSelAll < zSelAll) {                  // eval whole String, more as one xxx : xxx colon separated blocks
      int posColon = select.indexOf(':', ixSelAll); if(posColon <0) { posColon = zSelAll; }
      List<List<List<NumString>>> selistAnd = new LinkedList<List<List<NumString>>>(); 
      selistAll.add(selistAnd);
      //                                         // eval String till : 
      String selAnd = select.substring(ixSelAll, posColon); ixSelAll = posColon +1;
      int ixSelAnd = 0; int zSelAnd = selAnd.length();
      while(ixSelAnd < zSelAnd) {
        int posAmp = selAnd.indexOf('&', ixSelAnd); if(posAmp <0) { posAmp = zSelAnd; }
        List<List<NumString>> selistOr = new LinkedList<List<NumString>>(); 
        selistAnd.add(selistOr);
        //                                         // eval String till : 
        String selAdd = selAnd.substring(ixSelAnd, posAmp); ixSelAnd = posAmp +1;
        int ixSelAdd = 0; int zSelAdd = selAdd.length();
        while(ixSelAdd < zSelAdd) {
          int posSep = selAdd.indexOf('+', ixSelAdd); if(posSep <0) { posSep = zSelAdd; }
          List<NumString> selistLine = new LinkedList<NumString>(); 
          selistOr.add(selistLine);
          //                                         // eval String till : 
          String selLine = selAdd.substring(ixSelAdd, posSep); ixSelAdd = posSep +1;
          int zSelLine = selLine.length();
          List<NumString> selistItem = selistLine; //new LinkedList<NumString>(); 
          //selistLine.add(selistItem);
          String sel = null; int nLine = 0;
          int nLine1 = 0;
          for(int ix = 0; ix < zSelLine; ++ix) {
            char cc = selLine.charAt(ix);
            if(sel == null) {                  //line index
              if(cc >='0' && cc <= '9') { nLine1 = /*1<<*/(cc - '0'); }
              else if(cc == ',') { }
              else if(cc == ';') { }
              else if(cc == ' ') { }
              else if(cc == '=') { nLine = nLine1; }
              else { sel = "" + cc; }          // first character
            }
            else if( sel !=null && ", &;+".indexOf(cc) >= 0) {
              selistItem.add(new NumString(nLine, sel));
              sel = null;
              if(cc != ',') {
                nLine1 = 0;             // not a , => remove the table mask
              }
            } else {
              sel += cc;                       //a next char
            }
          } //for ...zSelLine
          if(sel !=null) {    //space finishes an item
            selistItem.add(new NumString(nLine, sel));
            sel = null;
          }
        } //while ... zSelAdd
      } //while ...zSelAnd
    } //while ...zSelAll
    //                                           //selListAll is filled. 
    return selistAll;
  }
  
  
  
  /**Executes the comparison with the given selistAll
   * @param selistAll returned from {@link #prepareCheckSameItem(String)}
   * @param cmp some Strings which should match to sellistALl 
   * @return true if matches
   */
  @Java4C.NoStackTrace @Java4C.Exclude public static boolean checkSameItem ( List<List<List<List<NumString>>>> selistAll, CharSequence ... cmp) {
    //able to improve: Get a filled list for Using in a loop of many selections. 
    boolean ok = false;
    int mLinesCheckAll = 0;                      //mLinesToCheckAll: all bits set for existing lines (from 1...)
    int mLinesToCheckAll = ((1 << (cmp.length +1)) -1) & 0xfffffffe;
    for(int ix = 1; ix <= cmp.length; ++ix) {
      CharSequence cmp1 =cmp[ix-1];
      int zCmp1 = cmp1.length();
      boolean bEmpty = true;
      for(int ic = 0; ic < zCmp1; ++ix) {
        char cc = cmp1.charAt(ic);
        if(cc != ' ') { bEmpty = false; break; }
      }
      if(bEmpty) {
        mLinesToCheckAll &= ~(1<<ix);   //do not check an empty cmp line
      }
    }
    for(List<List<List<NumString>>> selistAnd : selistAll) {
      for(List<List<NumString>> selistOr : selistAnd) {
        for(List<NumString> selistItem : selistOr) {
          int mLinesFound = 0;
          int mLinesCheck = 0;
          boolean foundItem = false;
          for(NumString selItem : selistItem) {
            for(int ixLine = 1; ixLine <= cmp.length; ++ixLine) {
              if(  (selItem.nline ==0 || (selItem.nline & (1<<ixLine)) != 0) //either requested line, or all lines
                && ((mLinesToCheckAll & (1 << ixLine)) !=0)     //exclude line from check which should not be checked. 
                ) {                                             //ixLine anyway correct for cmp to check against selItem
                mLinesCheck |= 1 << ixLine;
                CharSequence cmp1 = cmp[ixLine -1];
                int zSelItem = selItem.sel.length();
                int pos = StringFunctions.indexOf(cmp1, selItem.sel);
                char cc;
                if(  pos >=0                                 //is found
                  && (  (pos + zSelItem) == cmp1.length()    //found on end 
                     || (cc = cmp1.charAt(zSelItem)) == ' '  //follows space
                     || (cc >='A' && cc <= 'Z')              //follows upper case
                  )  ) {                                     //conditions for end
                  foundItem = true;
                  mLinesFound |= 1<< ixLine;         //set the found bit for the line in mask
                }
              }
              if((selItem.nline & (1<<ixLine)) != 0) { break; }  //exact this line found, break for.
            }//for ixLine
          }//for selItem
          if( (mLinesFound == mLinesCheck)) {
            mLinesCheckAll |= mLinesFound;
            break;                                   //one of the item in line selection is ok.
          }
        }//for selistItem
        if(mLinesCheckAll == mLinesToCheckAll) {
          ok = true;
          break;
        }
      }//for selistOr
      if(ok) { break; } //already found.
    }//for selistAnd
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
