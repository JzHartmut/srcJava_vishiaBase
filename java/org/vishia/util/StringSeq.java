package org.vishia.util;

import java.nio.charset.Charset;

/**This class wraps any {@link java.lang.CharSequence} and provides comparable features like {@link java.lang.String}
 * for commonly usage. The difference to java.lang.String is: 
 * <ul>
 * <li>A String is guaranteed immutable, the char sequence
 *   which is referred from this class is immutable only if the programming does guarantee it.  
 * <li>The advantage in comparison to java.lang.String is: If a char sequence is prepared in a {@link java.lang.StringBuilder}
 *   or such adequate, it does not need an own buffer to hold the string though the StringBuilder's buffer
 *   won't be changed by any other program part. It increases the speed for string operations especially
 *   if there is a large String.
 * </ul>
 * This class can be used as wrapper for both, mutable or immutable char sequences. The attribute {@link #isUnmated}
 * designates whether the char sequence is referred only by this class. This flag should be set and guaranteed
 * by the user's programming only.
 * <br><br>
 * A CharSequence can be cast to a StringBuilder if its implementation class is a StringBuilder or a new StringBuilder
 * can be build with it. The content of the StringBuilder can be changed. If it is unmated, no other usages
 * can be affect, only the same one which changes it. In this case the CharSequence can be changed for further usage.
 *   
 *   
 * @author Hartmut Schorrig
 *
 */
public class StringSeq implements CharSequence
{

  /**Version, history and license.
   * <ul>
   * <li>2012-02-19 Hartmut created.
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
  public final static int version = 20130829; 

  protected CharSequence cs;
  
  /**If this flag is set to true, the application declares the referred {@link #cs} as only referred from this.
   * The CharSequence can be changed only with this instance.
   */
  protected boolean isUnmated;
  
  /**An instance of this class can only build by this create method. The property whether it is unmated or not
   * should be given by the application. 
   * @param src any CharSequence, usual a {@link java.lang.StringBuilder}.
   * @param isUnmated it should be set to true if the src is not referred anywhere else.
   * @return a new instance of StringSeq 
   */
  public static StringSeq create(CharSequence src, boolean isUnmated){
    StringSeq ret = new StringSeq();
    ret.isUnmated = isUnmated;
    ret.cs = src;
    return ret;
  }
  
  /**An instance of this class can only build by this create method. The property whether it is unmated or not
   * should be given by the application. 
   * @param src any CharSequence, usual a {@link java.lang.StringBuilder}. It should be referenced never other.
   * @param isUnmated it should be set to true if the src is not referred anywhere else.
   * @return a new instance of StringSeq 
   */
  public static StringSeq create(CharSequence src){
    StringSeq ret = new StringSeq();
    ret.isUnmated = true;
    ret.cs = src;
    return ret;
  }
  
  
  public boolean isUnmated(){ return isUnmated; }
  
  public void change(CharSequence src){
    cs = src;
    //isChanged = false;
  }
  
  
  @Override public int length(){ return cs.length(); }
  
  /**A SubSequence is built as independent String in any case. Via this subSequennce it is not possible
   * to change the referred char sequence. 
   * @see java.lang.CharSequence#subSequence(int, int)
   */
  @Override public CharSequence subSequence(int start, int end){ 
    return cs.subSequence(start, end); 
  }
  
  
  /**Same as {@link #subSequence(int, int)} but it is compatible to {@link java.lang.String#substring(int, int)}
   * @param start
   * @param end
   * @return
   */
  public String substring(int start, int end){
    return cs.subSequence(start, end).toString(); 
  }
  
  
  /**Compatible to {@link java.lang.String#substring(int, int)}
   * @param start
   * @return
   */
  public String substring(int start){
    return cs.subSequence(start, cs.length()).toString(); 
  }
  
  
  @Override public char charAt(int index){ return cs.charAt(index); }
  
  
  public int compareTo(int from1, CharSequence s2, int from2, int nrofChars){
    return StringFunctions.compare(cs, from1, s2, from2, nrofChars);
  }
  
  
  
  public boolean startsWith(CharSequence cmp){
    return StringFunctions.startsWith(cs, cmp);
  }
  
  
  
  public int indexOf(char cc, int fromIndex){
    return StringFunctions.indexOf(cs, cc, fromIndex);
  }
  
  
  public int indexOf(CharSequence str, int fromIndex){
    return StringFunctions.indexOf(cs, str, fromIndex);
  }
  
  
  /**Returns a StringBuilder instance which allows the content to change.
   * @return either the same instance which is referenced with {@link #cs} or a new instance of a StringBuilder
   *   if {@link #cs} didn't reference a StringBuilder. In this case {@link #cs} references this new
   *   StrringBuilder too. 
   */
  public StringBuilder changeIt(){
    if(cs instanceof StringBuilder) return (StringBuilder)cs;
    else {
      StringBuilder u = new StringBuilder(cs);
      cs = u;
      return u;
    }
  }
  
  
  @Override public String toString(){ return cs.toString(); }
  
}
