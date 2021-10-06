package org.vishia.inspcComm;

import org.vishia.byteData.ByteDataAccessBase;

/**This class contains the organization of access to datagram data given in a byte[] array for one request or answer item.
 * Any item starts with at least 4 head bytes. In a C language struct it is: 
 * <pre>
 * typedef struct Inspcitem_t
 * {
 *   int16 nrofBytes;
 *   int16 cmd;
 * } Inspcitem;
 * </pre>
 * Depending on the command the item has some more bytes which can be recognized as children of the ByteDataAccess.
 * The other possibility in C language my be: test and cast. But in C the C-translated version of this class 
 * and  {@link ByteDataAccessBase} is used for too. 
 * <br><br>
 * This class contains all yet defined cmd constants for a Inspcector datagram item starting with 
 * 
 *  
 *  
 * @author Hartmut Schorrig
 *
 */
public class Inspcitem
{
  
  
  /**Request to get a list of all attributes and associations of the addressed Object.
  
  The address is given as String 
  
  ,  Cmd:
  ,  +------head------------+---------string--------------+
  ,  |kGetFields            | PATH with dot on end        |
  ,  +----------------------+-----------------------------+
  
  The positive answer datagram contains some Inspcitem for each Attribute or association:
  
  ,  Answer:
  ,  +------head-----------+-----string--+-------head----------+-----string--
  ,  |kAnswerFieldMethod   | Name:Typ    |kAnswerFieldMethod   | Name:Typ ...
  ,  +---------------------+-------------+---------------------+-------------

  The structure of the string is described on {@link #kAnswerFieldMethod}.
  */
  public final static int kGetFields = 0x10;
  
  
  /**Antwort auf Aufforderung zur Rueckgabe einer Liste von Attributen, Assoziationen oder Methoden.
  Das Antwort-Item enthaelt einen Eintrag f�r ein Element, Type DataExchangeString_OBM.
  Die Antwort auf kGetFields oder kGetMethods besteht aus mehreren Items je nach der Anzahl der vorhandenen Elemente.
  Gegebenenfalls ist die Antwort auch auf mehrere Telegramme verteilt.
  
  Die Zeichenkette fuer ein Item aus zwei Teilen, Typ und Name, getrennt mit einem Zeichen ':'.
  Der angegebenen Typ entspricht dem Typ der Assoziationsreferenz, nicht dem Typ des tatsaechlich assoziierten Objektes,
  wenn es sich nicht um einen einfachen Typ handelt.
  
  Wenn eine Methode uebergeben wird, dann werden die Aufrufargument-Typen wie eine formale Argumentliste in C angegeben.
  Beispiel:
  , returnType:methodName(arg1Typ,arg2Typ)
  
  Der Index im Head der Antwort zaehlt die uebergebenen Informationen.
  
  * One Answer for the request {@link #kGetFields} or kGetMethods (not yet ready) for one field or one method. 
  * For one request some more answer items are produced which were send all with one sequence number 
  * maybe with more as one answer datagram with incremented {@link InspcDatagram.SeqEntrant#answer()} number. 
  * All that answer items are associated to the only one request.
  * <br><br>
  * The answer contains a {@link HeadOrder} following by a String. The String should be end with 0..3 zero-Bytes
  * for 4-byte-boundary (tolerable if not). The String has the following syntax (ZBNF)
  * <pre>
  * nameType::= <*:?name> : <*:\[\.?type> [ \[\] <?container> | \[ <#?arrayLen> \] |] [\.\.\.<?hasSubstructure>].
  * <pre>
  * The String consists of two parts separated by a colon: <code>name:type</code>. The type is given as String. 
  * It can have some additional information about array, container, sub struct. The type is the type of the reference.
  * Note: If inheritance is used, the type of the associated instance can be an derived type which's fields and methods can be evaluated
  * by an invocation of {@value #kGetFields} with that path as address.
  * <br><br>
  * Examples for that syntax:
  * <ul>
  * <li><code>name:typ</code>: for simple fields.
  * <li><code>name:typ...</code>: for a struct type. In the target system it may be a reference or a embedded structure in C.
  * <li><code>name:typ[99]</code>: with the number of array elements if it is a static array with known number of elements.
  * <li><code>name:typ[99]...</code>: Array of pointers or instances which are a struct.
  * <li><code>name:typ[]</code>: Array with dynamic size or any other container. Its size ...TODO
  * 
  * </ul>
  * 
  */
  
  public final static int kAnswerFieldMethod = 0x14;
  
  public final static int kRegisterRepeat = 0x23;
  
  public final static int kAnswerRegisterRepeat = 0x123;
  
  public final static int kFailedRegisterRepeat = 0x124;
  
  public final static int kGetValueByIndex = 0x25;
  
  /**
  * <pre>
  ,  answer:
  ,  +------head-8---------+-int-4-+-int-4-+---n-------+-int-4-+---n-------+
  ,  |kAnswerValueByIndex  | ixVal | types | values    | types | values    |
  ,  +---------------------+-------+-------+-----------+-------+-----------+
  * <ul>
  * <li>First byte after head is the start index of variable for answer. It is 0 for the first answer
  *   info block. If the info block cannot contain all answers, a second info block in a second telegram
  *   will be send which starts with its start index.
  * <li>After them a block with up to 4 values follows. The first word of that block ///
  *   contains 4 types of values, see {@link InspcDataExchangeAccess#kScalarTypes}. Especially the 
  */
  public final static int kAnswerValueByIndex = 0x125;
  
  public final static int kAnswerValue = 0x26;
  
  public final static int kFailedValue = 0x27;
  
  public final static int kGetValueByPath = 0x30;
  
  public final static int kGetAddressByPath = 0x32;
  
  public final static int kSetValueByPath = 0x35;
  
  /**Sets a string value.
  * <pre>
  * < inspcitem> <@+2#?strlen> <@+strlen$?value> <@a4> <@strlen..SIZE$?path> <@a4>
  * </pre>
  * @since 2013-12-24
  */
  public final static int kSetStringByPath = 0x36;
  
  /**Request to get all messages.
  ,  Cmd:<pre>
  ,  +------head-----------+
  ,  |kGetMs               |
  ,  +---------------------+
  </pre>
  ,  Answer:<pre>
  ,  +------head-----------+---------+---------+---------+---------+
  ,  |kAnswerMsg           | Msg     | Msg     | Msg     | Msg     |
  ,  +---------------------+---------+---------+---------+---------+
  
  * Any Message has 16 to 80 Bytes. The structure of a message is described with {@link org.vishia.msgDispatch.InspcMsgDataExchg}.
  * This structure should be used as child. All values of the message are children of that child.
  */
  public final static int kGetMsg = 0x40;
  public final static int kAnswerMsg = 0x140;
  
  /**Remove gotten messages. Any message contains a sequence number. The answer of {@link #kGetMsg} 
  * contains all messages in a proper sequence order from..to. This Telegram removes the messages from..to sequence.
  ,  Cmd:<pre>
  ,  +------head-----------+--int-----+--int-----+
  ,  |kRemoveMsg           | from seq | to seq   |
  ,  +---------------------+----------+----------+
  </pre>
  ,  Answer:<pre>
  ,  +------head-----------+
  ,  |kAnswerRemoveMsg     |
  ,  +---------------------+
  
  */
  public final static int kRemoveMsg = 0x41;
  public final static int kAnswerRemoveMsgOk = 0x141;
  public final static int kAnswerRemoveMsgNok = 0x241;
  
  
  
  /**This item sets a value with a given position:
  * <pre>
  * <@8+4#?position> <@12+4#?length> <@16..SIZE#?bitImageValue>
  * </pre>
  * The position may be a memory address which is known in the client proper to the target
  * or it may be a relative offset in any target's data. The type of data is target-specific. 
  */
  public final static int kSetvaluedata = 0x50, kAnswervaluedata = 0x150;
  
  public final static int kFailedPath = 0xFe;
  
  public final static int kNoRessource = 0xFd;
  
  
  public final static int kFailedCommand = 0xFF;
  
  
  
  
  
  /**Access to head data.
   */
  public static class Head  extends ByteDataAccessBase
  {
    /**Position of data as constants.*/
    private static int k_nrofBytes=0, k_cmd=2, kSizeHead=4;

    public Head() {
      super(kSizeHead);
      super.setBigEndian(true);
    }
    
    /**Constructor only for derived head structures of a datagram
     * @param sizeHead The size of the head of the derived datagram
     */
    protected Head(int sizeHead) {
      super(sizeHead);
      super.setBigEndian(true);
    }
 
    public final int nrofBytes() { return getInt16(k_nrofBytes); }
    
    public final int cmd() { return getInt16(k_cmd); }
    
    public final void set_nrofBytes(int value){ setInt16(k_nrofBytes, value); }
    
    public final void set_cmd(int value){ setInt16(k_cmd, value); }
    
  }

  
  /**The older form of items contains an order number to associate an answer item to its request. 
   * A newer form don't use this number, instead the {@link InspcDatagram.SeqEntrant#seqnr()} should match 
   * and the deterministic of order of request and answers in the datagram is used.
   */
  public static class HeadOrder extends Head 
  {
    /**Position of data as constants.*/
    private static int k_order=4, kSizeHeadOrder=8;

    public HeadOrder(){ super(kSizeHeadOrder); }

    public final int order() { return getInt16(k_order); }
  
    public final void set_order(int value){ setInt16(k_order, value); }
    
    /**Cast from an instance of type {@link Head} which is checked for the {@link Head#cmd()} 
     * and therefore the cast is proper.
     * @param head The instance which is already tested for the correct {@link Head#cmd()}
     */
    public void castFromBase(Head head) {
      assignCasted(head, 0, 0);
    }
  }
  
  
  
  
  
  
}
