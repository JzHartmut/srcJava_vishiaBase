package org.vishia.inspcComm;

import org.vishia.byteData.ByteDataAccessBase;

/**This is an example for {@link ByteDataAccessBase} and also the access to the <em>Inspector datagram</em>. 
 * The Inspector (short <em>Inspc</em>) is a mechanism to access to target embedded processor systems
 * which runs often in C to access all data. See {@linkplain www.vishia.org/Inspc}. 
 * <br><br>
 * A datagram can be sent via socket communication with UDP, via a serial interface or a Dual-Port-Ram access. 
 * It has a limited size of at maximum 1200 Byte. A datagram starts with a header of at least 4 byte in the following C-language struct:
 * <pre>
 * typedef struct InspcDatagramBase_t
 * {
 *   int16 nrofBytes;
 *   int16 cmdDatagram;
 * } InspcDatagramBase;
 * </pre>
 * Depending on the command the item has some more bytes which can be recognized as children of the ByteDataAccess.
 * The other possibility in C language my be: test and cast. But in C the C-translated version of this class 
 * and  {@link ByteDataAccessBase} is used for too. 
 * <br><br>
 * For yet used datagram types depending on defined values of <code>cmdDatagram</code> 
 * this struct can be casted to the following struct of 16 bytes:
 * <pre>
 * typedef struct InspcDatagramSeqEntrant_t
 * {
 *   InspcDatagramBase base;
 *   int32 encryption;
 *   int32 seqnr;
 *   int16 answer;
 *   int16 entrant;
 * } InspcDatagramSeqEntrant;
 * </pre>
 * Both head structures are defined as static sub classes of this class.
 * <br><br>
 * The Inspector datagram consists of some items which are requests or answers. This items are described in {@link Inspcitem}.
 * 
 * 
 * The {@link TestInspcDatagram} contains some methods to build and evaluate datagrams to test the algorithm.  
 * 
 * 
 * @author Hartmut Schorrig
 *
 */
public class InspcDatagram
{
  public static class Base extends ByteDataAccessBase
  {
    /**Position of data as constants.*/
    private static int k_nrofBytes=0, k_cmdDatagram=2, kSizeHead=4;

    public Base() {
      super(kSizeHead);
      super.setBigEndian(true);
    }
    
    /**Constructor only for derived head structures of a datagram
     * @param sizeHead The size of the head of the derived datagram
     */
    protected Base(int sizeHead) {
      super(sizeHead);
      super.setBigEndian(true);
    }
 
    public final int nrofBytes() { return getInt16(k_nrofBytes); }
    
    public final int cmdDatagram() { return getInt16(k_cmdDatagram); }
    
    public final void set_nrofBytes(int value){ setInt16(k_nrofBytes, value); }
    
    public final void set_cmdDatagram(int value){ setInt16(k_cmdDatagram, value); }
    
  }
  
  
  public static class SeqEntrant extends Base
  {
    /**Position of data as constants.*/
    private static int k_encryption=4, k_seqnr=8, k_answer = 12, k_entrant = 14, kSizeHead=16;

    public SeqEntrant() {
      super(kSizeHead);
      super.setBigEndian(true);
    }
 
    public final int encryption() { return getInt32(k_encryption); }
    
    public final int seqnr() { return getInt32(k_seqnr); }
    
    public final int answer() { return getInt16(k_answer); }
    
    public final int entrant() { return getInt16(k_entrant); }
    
    public final void set_encryption(int value){ setInt32(k_encryption, value); }
    
    public final void set_seqnr(int value){ setInt32(k_seqnr, value); }
    
    public final void set_answer(int value){ setInt32(k_answer, value); }
    
    public final void set_entrant(int value){ setInt32(k_entrant, value); }
    
    /**Cast from an instance of type {@link Base} which is checked for the {@link Base#cmdDatagram()} 
     * and therefore the cast is proper.
     * @param base The instance which is already tested for the correct {@link Base#cmdDatagram()}
     */
    public void castFromBase(Base base) {
      assignCasted(base, 0, 0);
    }
    
  }
  
  
  
  
}
