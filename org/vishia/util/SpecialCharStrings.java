/****************************************************************************/
/* Copyright/Copyleft:
 *
 * For this source the LGPL Lesser General Public License,
 * published by the Free Software Foundation is valid.
 * It means:
 * 1) You can use this source without any restriction for any desired purpose.
 * 2) You can redistribute copies of this source to everybody.
 * 3) Every user of this source, also the user of redistribute copies
 *    with or without payment, must accept this license for further using.
 * 4) But the LPGL ist not appropriate for a whole software product,
 *    if this source is only a part of them. It means, the user
 *    must publish this part of source,
 *    but don't need to publish the whole source of the own product.
 * 5) You can study and modify (improve) this source
 *    for own using or for redistribution, but you have to license the
 *    modified sources likewise under this LGPL Lesser General Public License.
 *    You mustn't delete this Copyright/Copyleft inscription in this source file.
 *
 * @author JcHartmut = hartmut.schorrig@vishia.de
 * @version 2006-06-15  (year-month-day)
 * list of changes:
 * 2007-07-15: JcHartmut www.vishia.de creation
 * 2008-04-02: JcHartmut some changes
 *
 ****************************************************************************/
package org.vishia.util;

/**This class helps to handle with special chars in Strings.
 * In Java all Strings are encoded with UTF-16. But in Files the encoding is mostly implemented
 * with 1 byte per char. Different encodings are used, ISO-8859-1 is typical for windows systems,
 * but also UTF-8. To write special chars in any desired encoding, some systems are ordinary.
 * In programming, the backslash is used to switch to special char codes, typically \\n for new line.
 * In XML the system of special character codes starts with an ampersand, typically
 * &amp; for the &-character itself.
 * 
 * This class supports the backslash-philosophy to indicate special character codes.
 * @author JcHartmut
 *
 */
public class SpecialCharStrings 
{
  /** The char used to code start of text. */  
  public static final char cStartOfText = (char)(0x2);
  
  /** The char used to code end of text. */  
  public static final char cEndOfText = (char)(0x3);

  /**Resolves the given String containing some switch chars in form of backslash 
   * to a string with the appropriate character codes.
   * In the result String all char-pairs beginning with backslash are replaced by
   * one char. If the String doesn't contain backslashes, the method returns the input string
   * in a as soon as possible calculation time.
   * <ul>
   * <li>\n\r\t\f\b will converted to the known control character codes:
   * <li>\n newline 0x0a
   * <li>etc TODO
   * <li>\s will converted to a single space. It is usefull in input situations
   *     where a space will have another effect.
   * <li>\a will converted to the code 0x02, known in this class {@link cStartOfText}.
   *     It is usefull wether a String may be contain a code for start of text.
   * <li>\e will converted to the code 0x03, known in this class {@link cEndOfText}.
   * <li>\x0123 Convert from given hex code TODO
   * <li>\\ is the backslash itself.
   * <li>All other chars after backslash will be converted to the same char, 
   *     at example "\{" to "{". Don't use this feature for normal alphabetic chars
   *     because some extensions in a future may be conflict with them. But this feature
   *     may be usefull if an input text uses the special characters in a special way.
   * </ul> 
   * @param src The input string
   * @return The output string with replaces backslash pairs.
   */
  public static String resolveCircumScription(String src)
  { String sResult;
    final char cSwitch = '\\';
    int posSwitch = src.indexOf(cSwitch);
    if(posSwitch < 0)
    { sResult = src;
    }
    else
    { //escape character is found before end
      StringBuffer sbReturn = new StringBuffer(src);
      while(posSwitch >=0)
      { if(posSwitch < sbReturn.length()-1)
        { sbReturn.deleteCharAt(posSwitch);
          /*do not delete a \ as last character, because the next algorithm failed
           *in such case. The \ will kept. It is a possible input sequence of a user,
           *and it shouldn't be throw an IndexOutofBoundaryException!
           */  
        }
        char cNext = sbReturn.charAt(posSwitch);
        int iChangedChar;
        if( (iChangedChar = "snrtfb".indexOf(cNext)) >=0)
        { sbReturn.setCharAt(posSwitch, " \n\r\t\f\b".charAt(iChangedChar));
        }
        else if( cNext == 'a')
        { // \a means end of file, coded inside with 4 = EOT (end of transmission).
          sbReturn.setCharAt(posSwitch, cStartOfText);
        }
        else if( cNext == 'e')
        { // \e means end of file, coded inside with 4 = EOT (end of transmission).
          sbReturn.setCharAt(posSwitch, cEndOfText);
        }
        else
        { //the char after cEscape is valid and not changed!
        }
        posSwitch = sbReturn.toString().indexOf(cSwitch, posSwitch+1);
      }
      sResult = sbReturn.toString();
    }
    return sResult;
  }
}
