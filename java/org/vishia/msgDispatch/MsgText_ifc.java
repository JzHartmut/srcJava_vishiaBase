package org.vishia.msgDispatch;

import java.util.Collection;

/**Interface to get a text to a message number. */
public interface MsgText_ifc {

  
  /**version, history and license:
   * <ul>
   * <li>
   * <li>2013-02-24 Hartmut new {@link #getListItems()}, move {@link MsgConfigItem} from {@link MsgConfig}.
   * <li>2012-08-22 Hartmut created as interface to {@link MsgConfig} to use in {@link MsgDispatcher}
   * </ul>
   * 
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
   * 
   * 
   */
  public final static int version = 20120822;

  
  /**Returns a message text to the given ident number
   * @param ident the absolute value is used to find the text.
   * @return null if no message text was found.
   *     @pjava2c=zeroTermString.
   */
  String getMsgText(int ident);

  
  
  Collection<MsgConfigItem> getListItems();
  
  
  
  /**One item for each message. 
   * From Zbnf: This class is used as setting class for Zbnf2Java, therefore all is public. The identifiers have to be used
   * as semantic in the parser script.
   *
   */
  public static class MsgConfigItem
  {
    /**The message text can contain format specifier for the additional values. */
    public String text;
    
    /**Identification String for non-numbered message, see {@link MsgPrintStream}*/
    public String identText;
    
    /**The message ident.*/
    public int identNr;
    
    /**The last ident if this entry describes a range. */
    public int identNrLast;
    
    /**Some chars which can specify the destination (output) for the message. */
    public String dst;
    
    public char type_;
    
    public void set_type(String src){ type_=src.charAt(0); }
  }
  

}
