package org.vishia.util;

/**This class helps to present key codes and other events in combination with shift, alt, control 
 * in a maybe readable form. It defines unique integer representations for all keys and events.
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
 * <br><br>
 * <b>Selection events<b><br>
 * The class defines constants which are used to determine which cause calls a user method.
 * 
 * @author Hartmut Schorrig
 *
 */
public class KeyCode
{
 
  
  /**The version
   * <ul>
   * <li>2013-11-23 Hartmut new: Masks for {@link #shiftDigit} etc, Definition of {@link #tab}
   * <li>2013-11-23 Hartmut chg: {@link #userSelect} and {@link #defaultSelect} instead tableLineSelect 
   * <li>2012-07-15 Hartmut new: {@link #isAsciiTextKey(int)}
   * <li>2012-06-17 new {@link #isControlFunctionMouseUpOrMenu(int)} etc. cluster of keys and actions.
   * <li>2011-11-18 new {@link #mouse1UpMoved}
   * <li>2011-09-30 improved
   * </ul>
   */
  public static final String sVersion = "2015-08-23";
  

  public static final int altDigit =      0x00f00000; 

  public static final int alt =           0x00a00000;
  
  public static final int ctrlDigit =     0x0f000000; 

  public static final int ctrl =          0x0c000000;
  
  public static final int shiftDigit =    0xf0000000; 
  
  /**Use the 8. digit looks line "S" for "shift", value "5". */
  public static final int shift =         0x50000000;
  
  /**Use the 7.6. digit looks line "ca" for "control alt". */
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
   * <li>f: function key {@link #F}
   * <li>Use letter, not digits to recognize it well.
   * <li>Don't use a, e, 5 to distinguish with the alt, control, shift
   * 
   * </ul>
   */
  public final static int mSpecialKeys =     0x000f0000;
  public final static int mSpecialKeyDigit = 0x000f0000;
  
  /**Mask for the additional ctrl, alt and shift key.
   * If a key code is mask with them, it can be compared with
   * #alt, #ctrl. #shift, #ctrlAlt, #shiftAlt, shiftCtrl, shiftCtrlAlt.
   */
  public final static int mAddKeys = 0xfff00000;
  
  /**Any of control or alt is pressed. */
  public final static int mCtrlAlt =         0x0ff00000;
  public final static int mCtrlAltDigit =    0x0ff00000;
  
  //public final static int cursor = 0x000c0000;
  
  /**Function keys in {@link #mSpecialKeys}. */
  public static final int function=           0x000f0000;
  public static final int functionDigit=      0x000f0000;
  
  /**Non-alpha-numeric keys in {@link #mSpecialKeys}. */
  public static final int nonAlphanum =  0x000e0000;
  
  /**Alpha-numeric keys in {@link #mSpecialKeys}. */
  public static final int alphanum =      0x00000000;
  
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

  public final static int back =  0x000e0000 + '\b';

  public final static int enter = 0x000e0000 + '\r';
  
  public final static int ins =   0x000e0000 + 'i';
  public final static int del =   0x000e0000 + 'd';
  public final static int esc =   0x000e0000 + 0x1b;
  public final static int tab =   0x000e0000 + 0x09;
  
  public final static int mouse1Down =   0x000b0000 + 'D';  
  public final static int mouse1Up =     0x000b0000 + 'U';  
  
  /**The mouse button is released, but at another position where pressed. */
  public final static int mouse1UpMoved = 0x000b0000 + 'R';  
  /**Doubleclick on mouse button. */
  public final static int mouse1Double = 0x000b0000 + 'C';  
  
  /**The mouse button 2 is usual the right button. */
  public final static int mouse2Down =   0x000b0000 + 'd'; //down
  public final static int mouse2Up =     0x000b0000 + 'u'; //up
  /**The mouse button is released, but at another position where pressed. */
  public final static int mouse2UpMoved = 0x000b0000 + 'r';  //removed
  public final static int mouse2Double = 0x000b0000 + 'c'; //click
  
  /**Both mouse buttons left and right are pressed down.*/
  public final static int mouseBothDown = 0x000b0000 + 'b'; //both
  
  public final static int mouseWheelUp = 0x000b0000 + 't';  //top
  public final static int mouseWheelDn = 0x000b0000 + 'e';  //end
  
  /**The mouse button 3 is usual the middle button. */
  public final static int mouse3Down =   0x000b0000 + '3'; 
  public final static int mouse3Up =     0x000b0000 + '4'; 
  
  /**Determines that a menu item is entered. */
  public final static int menuEntered =  0x000b0000 + 'M'; 

  /**Determines that a menu item is entered. */
  public final static int Entered =  0x000b0000 + 'M'; 

  /**Determines that a line of a table is selected by user actions. */
  public final static int userSelect =  0x000b0000 + 'S'; 
  
  /**Determines that a line of a table is selected per default. */
  public final static int defaultSelect =  0x000b0000 + 's'; 
  
  /**Determines that a field has got a focus. */
  public final static int focusGained =  0x000b0000 + 'F'; 
  
  public final static int focusLost =  0x000b0000 + 'f'; 
  
  /**A textfields content is changed. */
  public final static int valueChanged =  0x000b0000 + 'v'; 
  
  /**Key code for anything activated. */
  public final static int activated =  0x000b0000 + 'a'; 
  
  /**Key code for anything removed. */
  public final static int removed =  0x000b0000 + 'x'; 
  
  
  //----- drag and drop ---------------------------------------------------------------
  public final static int dropFiles =    0x000d0000 + 'F'; 
  public final static int dropText =     0x000d0000 + 'T'; 
  
  public final static int dragFiles =    0x000d0000 + 'f'; 
  public final static int dragText =     0x000d0000 + 't'; 
  
  public final static int undefined = nonAlphanum;
  
  public final int code;
  
  public final String str;
  
  public KeyCode(String src){
    this.code = convert(src);
    this.str = src;
  }
  
  public KeyCode(int code){
    this.code = code;
    StringBuilder u = new StringBuilder(20);
    if((code & shiftDigit) == shift){ u.append("shift-"); }
    if((code & altDigit) == alt){ u.append("alt-"); }
    if((code & ctrlDigit) == ctrl){ u.append("ctrl-"); }
    if((code & 0x000f0000) == function){ u.append("F"); }
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
      ret |= function;
      ret |= '0' + test;
      //TODO how to present F10, F11, F12?
    } else {
      ret |= src.charAt(len-1);  //The last char is the key.
    }
    return ret;
  }
  
  
  /**A key which is used in an text field to write something. It includes the del key. 
   * @param key
   * @return
   */
  public static boolean isWritingKey(int key){
    if((key & (mAddKeys | mSpecialKeys))==0) return true; 
    if(key == del) return true;
    if(key == back) return true;
    return false;
  }
  
  /**Any key between the codes 0x20 .. 0x7e, it is an ASCII text key.
   * @param key
   * @return
   */
  public static boolean isAsciiTextKey(int key){
    return key >= ' ' && key <= 0x7e;
  }
  
  /**Any text key, in UTF16 range but not a ASCII control key (0x0 .. 0x1f)
   * @param key
   * @return
   */
  public static boolean isTextKey(int key){
    return (key & (mCtrlAlt | mSpecialKeys))==0 && key >=' ';
  }
  
  /**returns true if it is a control or function key. Either function or ctrl combination.
   * A control key controls actions of the user. The original control keys in the 1960-80 age were associated
   * with the control key button. On PCs the function keys enhances this concept.
   * Usual keys with ctrl-button and all function keys maybe in combination with ctrl, shift, alt are used for user-control of an application
   * via keyboard. This combinations returns true.<br>
   * A alphanumeric key only in combination with alt is not a control key. It is intended for menu actions. 
   *   
   * @param key
   * @return
   */
  public static boolean isControlOrFunction(int key){
    int type = key & mSpecialKeys;
    return type == function || (type == alphanum && (key & ctrl) !=0);
  }
  
  
  /**Returns true if it is a control, function or graphical control key.
   * It supports the typical situation for an action which should be invoked by a control or function key from the keyboard
   * or only on mouse-up in any widget, or from an menu click.
   * Usual a graphical button should act on mouse-up, not on mouse-down. That is because on touch screen a mouse down
   * shows maybe a fault position or fault button activation. If the cursor will be move away, the button is not activated.  
   * @param key
   * @return
   */
  public static boolean isControlFunctionMouseUpOrMenu(int key){
    int type = key & mSpecialKeys;
    return key == KeyCode.menuEntered 
      || key == KeyCode.mouse1Up 
      || type == function 
      || type == nonAlphanum 
      //|| (type == alphanum && (key & mCtrlAlt) !=0);
      || ((key & mCtrlAlt) !=0);
    
  }

  
  public static boolean isWritingOrTextNavigationKey(int key){
    if((key & (mAddKeys | mSpecialKeys))==0) return true; 
    if((key & mSpecialKeys)==0x000a0000) return true;
    if(key == back) return true;
    if(key == enter) return true;
    if(key == del) return true;
    return false;
  }
}
