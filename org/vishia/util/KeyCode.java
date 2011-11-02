package org.vishia.util;

/**This class helps to present key codes in combination with shift, alt, control and the function keys
 * in a well readable form. It contains a string representation for debug-view 
 * and a unique integer representation for fast switch-case-processing.
 * The key code consist of 2 blocks, which are able to read in hexa view:
 * <ul><li>The lower 16 bit usual are the character code of the key, whereby typically only 8 bit are used
 *   if the key is presented by a ASCII-code. It may be the number of a special key too.
 *   The key code pressed on keyboard depends on the language-specific keyboard layout.     
 * <li>The middle bits 19..16 respectively the middle hex digit is set to a b e f, see {@link #mSpecialKeys}.
 * <li>The higher 12 bit, 3 hex digits are set with hexa-readable digits 5, c, a and f whereby 5 means 'S' for shift.
 *   See the codings {@link #alt}, {@link #ctrl}, {@link #shift} and {@link #F},
 * </ul>
 * <br><br>
 * <b>Mouse button<b><br>
 * The mouse buttons are coded here too. Concept:
 * <ul>
 * <li>The left, right and middle button is designated as 'first', 'second' because a mouse may be set to left hand mode.
 *   Then the left button is the 'second'. The middle button may be supported or not. Depending on driver, hardware
 *   it is designated as third button.
 * <li>The code contains the hex digit 'b' in bits 19..16 and a character 'D', 'U', 'C' for 'up', 'down' and 'doubleClick' 
 *   of the first button (right hand mouse: the left button). Furthermore 'd', 'u' and 'c' for the other button
 *   and '3' and '4' for the third button up and down.
 * <li>A pressed alt, control and/or shift key is presented in the bits 31..20 
 * </ul>     
 * 
 * @author Hartmut Schorrig
 *
 */
public class KeyCode
{
 
  
  /**The version
   * <ul>
   * <li>2011-09-30 improved
   * </ul>
   */
  public static final int version = 0x20110930;
  

  public static final int alt =           0x00a00000;
  
  public static final int ctrl =          0x0c000000;
  
  public static final int shift =         0x50000000;
  
  public static final int ctrlAlt =       0x0ca00000;
  
  public static final int shiftAlt =      0x50a00000;
  
  public static final int shiftCtrl =     0x5c000000;
  
  public static final int shiftCtrlAlt =  0x5ca00000;
  
  /**Bits to designate special keys.
   * <ul>
   * <li>a: arrow
   * <li>b: mouse button, menu
   * <li>d: drag and drop
   * <li>e: enter, esc etc.
   * <li>f: function key
   * <li>Use letter, not digits to recognize it well.
   * <li>Don't use a, e, 5 to distinguish with the alt, control, shift
   * 
   * </ul>
   */
  public final static int mSpecialKeys = 0x000f0000;
  
  public final static int mAddKeys = 0xfff00000;
  
  public final static int cursor = 0x000c0000;
  
  public static final int F =             0x000f0000;
  
  public static final int F1 = 0x000f0031;
  
  public static final int F2 = 0x000f0032;
  
  public static final int F3 = 0x000f0033;
  
  public static final int F4 = 0x000f0034;
  
  public static final int F5 = 0x000f0035;
  
  public static final int F6 = 0x000f0036;
  
  public static final int F7 = 0x000f0037;
  
  public static final int F8 = 0x000f0038;
  
  public static final int F9 = 0x000f0039;
  
  public static final int F10 = 0x000f0041;
  
  public static final int F11 = 0x000f0042;
  
  public static final int F12 = 0x000f0043;
  
  public static final int left =  0x000a0000 + 'l';  //'a' means arrow, not c for cursor, c is ctrl
  
  public static final int right = 0x000a0000 + 'r';
  
  public static final int up =    0x000a0000 + 'u';
  
  public static final int dn =    0x000a0000 + 'd';
  
  public static final int pgup =  0x000a0000 + 'U';
  
  public static final int pgdn =  0x000a0000 + 'D';
  
  public static final int home =  0x000a0000 + 'h';
  
  public static final int end =   0x000a0000 + 'e';
  
  public final static int ins =   0x000e0000 + 'i';
  public final static int del =   0x000e0000 + 'd';
  public final static int enter = 0x000e0000 + '\r';
  public final static int back =  0x000e0000 + '\b';
  public final static int esc =   0x000e0000 + 0x1b;
  
  public final static int mouse1Down =   0x000b0000 + 'D';  
  public final static int mouse1Up =     0x000b0000 + 'U';  
  public final static int mouse1Double = 0x000b0000 + 'C';  
  
  public final static int mouse2Down =   0x000b0000 + 'd'; 
  public final static int mouse2Up =     0x000b0000 + 'u'; 
  public final static int mouse2Double = 0x000b0000 + 'c'; 
  
  public final static int mouse3Down =   0x000b0000 + '3'; 
  public final static int mouse3Up =     0x000b0000 + '4'; 
  
  public final static int menuEntered =  0x000b0000 + 'M'; 
  
  public final static int dropFiles =    0x000d0000 + 'F'; 
  public final static int dropText =     0x000d0000 + 'T'; 
  
  public final static int dragFiles =    0x000d0000 + 'f'; 
  public final static int dragText =     0x000d0000 + 't'; 
  
  
  
  public final int code;
  
  public final String str;
  
  public KeyCode(String src){
    this.code = convert(src);
    this.str = src;
  }
  
  public KeyCode(int code){
    this.code = code;
    StringBuilder u = new StringBuilder(20);
    if((code & 0xf0000000) == shift){ u.append("shift-"); }
    if((code & 0x0f000000) == alt){ u.append("alt-"); }
    if((code & 0x00f00000) == ctrl){ u.append("ctrl-"); }
    if((code & 0x000f0000) == F){ u.append("F"); }
    u.append((char)(code & 0x000000ff)); 
    this.str = u.toString();
  }
  
  @Override public String toString(){ return str; }
  
  
  public static int convert(String src)
  { 
    int ret = 0;
    int len = src.length();
    int test;
    if(src.contains("sh-")){ ret |= shift; }
    if(src.contains("alt-")){ ret |= alt; }
    if(src.contains("ctrl-")){ ret |= ctrl; }
    if(src.contains("ctr-")){ ret |= ctrl; }
    if( ret == 0 //nothing found above
      && (test = src.indexOf('-')) >0
      ){
      //check one-key c a C A
      for(int p=0; p < test; ++p){
        switch(src.charAt(p)){
        case 'a': ret |= alt;
        case 'A': ret |= shift | alt;
        case 'c': ret |= ctrl;
        case 'C': ret |= shift | ctrl;
        }
      }
    }
    if(len >=2 && src.charAt(len-2)=='F' && (test = "0123456789abcABC".indexOf(src.charAt(len-1))) >=0){
      //ends with F1..F9, F0, Fa, FB for F10, F11, F12
      if(test == 0){ test = 10;}
      else if(test >=13){ test -=3; }
      ret |= F;
      ret |= '0' + test;
      //TODO how to present F10, F11, F12?
    } else {
      ret |= src.charAt(len-1);  //The last char is the key.
    }
    return ret;
  }
}
