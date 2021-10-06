package org.vishia.byteData;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Arrays;

import org.vishia.util.InfoFormattedAppend;
import org.vishia.util.Java4C;
import org.vishia.util.RetOrException;
import org.vishia.util.StringFormatter;

/**This class is a base class to control the access to binary data.
 * The binary data may typically used or produced from a part of software written in C or C++.
 * Therewith the binary data are struct-constructs. Another example - build of datagram structures.
 * <br>
 * This class is a base class which should be derived for user's necessities. 
 * The methods {@link #getInt16(int)} etc. are protected. That should prevent erratic free accesses to data 
 * on application level. A derived class of this class structures the software for byte data access.
 * <br><br> 
 * It is able to support several kinds of structured data access:<ul>
 * <li>Simple C-like <code>struct</code> are adequate mapped with a simple derived class of this class, 
 *     using the protected commonly access methods like {@link #_getLong(int, int)} with predefined indexes 
 *     in special methods like getValueXyz().
 * <li>Complex <code>struct</code> with nested <code>struct</code> inside are mapped 
 *     with one derived class per <code>struct</code>, define one reference per nested struct 
 * <li>Base <code>struct</code> inside a <code>struct</code> (inheritance in C) can be mapped with 
 *     extra derived classes for the base struct and usind the
 *     {@link #assignCasted(ByteDataAccessBase, int, int)}-method.
 * <li>A pack of data with several struct may be mapped using the {@link #addChild(ByteDataAccessBase)}-method.
 *     Thereby a parent should be defined, and the structs of the pack are children of this parent. 
 *     That structures need not be from the same type.
 * <li>packs of struct with parent are able to nesting, it is able to construct as a tree of pack of structures. 
 *     The parent of the pack is the tree node. It is likewise a XML tree. 
 *     The data may be also transformed to a XML data representation
 *     or the data structure may be explained with a XML tree, but they are not
 *     XML data straight.
 * </ul>
 * This application possibilities show a capable of development system to access binary data. 
 * The other, hand made way was calculate with indices of the byte[idx] specially user programmed. 
 * This class helps to make such complex index calculation needlessly. 
 * One struct at C level corresponds with one derivated class of ByteDataAccessBase. 
 * But last also a generation of the java code from C/C++-header files containing the structs is able to.
 * So the definition of byte positions are made only on one source. The C/C++ is primary thereby. 
 *      
 * <br>
 * The UML structure of such an class in a environment may be shown with the
 * followed object model diagram, <br>
 * <code> <+>---> </code>is a composition,
 * <code> <>---> </code>is a aggregation, <code> <|---- </code>is a inherition.
 *  <pre>
 *                      +-------------------------------+                 +----------+
 *                      | ByteDataAccessBase(sizeHead)  |----data-------->| byte[]   |
 *                      |-------------------------------|                 |          |
 *                      |- idxBegin:int                 |                 | d a t a  |
 *                      |- idxNextChild:int             |                 +----------+
 *  +-------------+     |- idxEnd:int                   |
 *  | derivated   |     |-------------------------------|<---------------+ a known parent
 *  | user        |---|>|+ addChild(child)              |---parent-------+ set in addChild()                
 *  | classes     |     |+ addChildFloat()              |
 *  +-------------+     |% getInt32()                   |----currChild---+
 *                      |                               |<---------------+
 *                      +-------------------------------+
 * </pre>
 *
 * This class is the base class for ByteDataAccess. It works without dynamic methods proper for C usage.
 * All variables are package private because they should be changed only with methods of this class.
 * <br><br>
 * <h2>Initialization and instances</h2>
 * The root instance to access to data should be initialized with
 * <pre>
 MyByteDataAccessRoot accessRoot = new MyByteDataAccessRoot(); 
   //... invokes super.ByteDataAccessBase(lengthHead);
 accessRoot.assign(myData, dataLength);
 * </pre>  
 * Any instances which represents a sub structure in data can be created as re-useable instances, which can be added
 * as child of the root instance or as child of any already added child on demand:
 * <pre>
 MySubStruct subStruct = new SubStruct();  //an instance for accessing 
   ....
   accessRoot.addChild(subStruct);         //adds on current position.
   int value = subStruct.getValuexyz();    //now can access the data.
   ....  //later on code:
   accessOther.addChild(subStruct);        //reuse the instance for access
   int value = subStruct.getValueAbc();    //access other data.
 * </pre>
 * The instances of this derived class are helper to access to the data, they are not container for the data. 
 * The data are stored in a <code>byte[]</code>-array 
 * 
 * 
 * <br>  
 * <h2>children, currentChild, addChild</h2>
 * Children are used to evaluate or write different data structures after a known structure. 
 * The children may be from several derived types of this class. 
 * With children and children inside children a tree of different data can be built or evaluated.
 * <ul>
 * <li>{@link #addChild(ByteDataAccessBase)}: adds a child with its head size
 * <li>{@link #addChild(ByteDataAccessBase, int)}: adds a child with a given length
 * <li>{@link #addChildEmpty(ByteDataAccessBase)}: adds a child with its head size for writing, set all data to 0.
 * <li>{@link #addChildEmpty(ByteDataAccessBase, int)}: same with given size of child.
 * <li>{@link #addChildFloat(float)}, {@link #addChildInteger(int, long)}: writes the value and increments the #ixChildEnd after it.
 * <li>{@link #addChildString(CharSequence)}, {@link #addChildString(CharSequence, String)}: writes the String.
 * <li>{@link #getChildFloat()}, {@link #getChildDouble()}, {@link #getChildInteger(int)}: reads the value at {@link #ixNextChild}
 *   and increments the {@link #ixNextChild}.
 * <li>{@link #getChildString(int)} reads a String and increments the {@link #ixNextChild}
 * <li>{@link #addChildAt(int, ByteDataAccessBase)}, {@link #addChildAt(int, ByteDataAccessBase, int)}: Used if the byte structure
 *   is known. Adds a child not at #ixNextChild but at the given position. 
 * </ul>
 * Mechanism and indices see {@link #addChild(ByteDataAccessBase, int)}.   
 * 
 * 
 * <h2>Expand, check</h2>
 * If an instance is set for read or change, a given number of valid data bytes are known. The instance is marked as 'non expandable', see
 * The data should be assigned with
 * <ul>
 * <li>{@link #assign(byte[], int)} with given length, not expandable
 * <li>{@link #assign(byte[], int, int)} at a defined position in the data usefull for special cases.
 * With the given length >= the sizehead of the {@link ByteDataAccessBase#ByteDataAccessBase(int)} 
 * the internal flag {@link #bExpand} is set to false.
 * </ul>
 * If an instance is set for write, the maximal number of bytes is limited by the size of data (byte[]).
 * The data should be assigned with:
 * <ul>
 * <li>{@link #assignClear(byte[])} as empty instance, the data are cleared in its whole length.
 * <li>{@link #assign(byte[], int)} with 0 as length argument as instance which only knows the head data, but the data are not cleared.
 * </ul>
 * In both cases the internal flag {@link #bExpand} is set to true. It means that the {@link #ixEnd()} which is set initially to the {@link #sizeHead}
 * is increased if a child is added.
 * <br><br>
 * If an instance of this is set to non expandable,
 * an exception is thrown if a children is added after the given length. If an instance is designated as 'expandable' the end index
 * #ixEnd is increased by adding children till the length of data as its maximal value.
 * <br><br>
 * With the operations {@link #sufficingBytesForNextChild(int)} or {@link #getMaxNrofBytes()} it can be tested whether a child can be added
 * with its known length.   
 * <br>
 * 
 * <h2>Examples</h2>
 * See 
 * <ul>
 * <li>{@link org.vishia.byteData.test.TestByteDataAccessBase}
 * <li>{@link org.vishia.byteData.test.ExampleStringOrInt}
 * </ul> 
 * @java2c = noObject.
 * 
 * @author Hartmut Schorrig
 *
 */
@Java4C.ExcludeInterface("InfoFormattedAppend")

public class ByteDataAccessBase implements InfoFormattedAppend
{
  
  /**The version, history and license. 
   * <ul>
   * <li>2018-12-27 Hartmut new {@link #upcast(ByteDataAccessBase, int)} replaces the {@link #assignCasted(ByteDataAccessBase, int, int)}. Problems detect on usage of assignCasted:
   *   The cast of a child should influence the parent because the next {@link #addChild(ByteDataAccessBase)} should regard the casting operation. The casting is regarded
   *   to the data in sum. It is not only a isolated view to data. Strategy of upcast is documented there.  
   * <li>2018-09-18 Hartmut new {@link #ixBeginLocal} stores the position inside the child, whereby ixBegin is global. 
   *   TODO description of strategy of fix children. It may be proper if assignData(...) will be invoked only for the derived class
   *   which knows its fix children. The old concept of overridden methods here are not proper for simple C applications. 
   * <li>2018-09-16 Hartmut chg: All access routines calls {@link #checkData()}. 
   *   The reason for checkData(): If a child (at an index) was not assigned with data, it gets the data from its parent now. 
   *   With them it is possible to call {@link #assign(byte[])} for the parent only and it runs.
   * <li>2016-01-24 Hartmut chg: All {@link #addChild(ByteDataAccessBase)} etc. returns false if {@link #setException(boolean)} is false and the child cannot be added.
   *   If the user invokes setException(false) the user must check the return value of the add...() operations.
   * <li>2015-08-08 Hartmut chg: Change of concept: prevent Exception if the environment does not process it: Tested on {@link #setIdxtoNextCurrentChild(int)}:
   *   This routine can throw an Exception, but it can return -1 on error too, possible with an exception message. Therewith more operations can be disabled by quest of return value.
   * <li>2015-08-08 Hartmut chg: {@link #setLengthElement(int)} should set the ixEnd of the child and the ixNextChild of the parent
   *   but does not influence the this.ixNextChild. See test cases (TODO). This is fixed and all usages of this class for 
   *   org.vishia.insp* are tested well by testing the reflection access and GUI.  
   * <li>2015-08-08 Hartmut new: {@link #getChildInt(int)},  {@link #_getInt(int, int)} for 32 bit operation, {@link #_getLong(int, int)} uses 64 bit. 
   *   Used for in derived classes also. The 64-bit-operation is not proper for 32-bit embedded systems in C. TODO use it also if the size argument is <=4.
   * <li>2015-04-12 Hartmut docu and fineTuning: 
   * <li>2015-03-08 Hartmut chg: ixChild removed, unnecessary, instead {@link #currChild}, proper for debug. 
   * <li>2014-09-05 Hartmut chg: Some problems fixed in C-Application. Runs in Java. Meaning of {@link #bExpand}
   * <li>2014-09-05 Hartmut chg: Designation with some Java4C annotation.
   * <li>2014-09-05 Hartmut chg: now based on a {@link #charset} of type {@link java.nio.charset.Charset}. In C it is a dummy yet.
   *  In Java the Charset can be used immediately, more simple.
   * <li>2014-09-05 Hartmut bugfix: Some references changed from ByteDataAccess to ByteDataAccessBase.
   * <li>2014-09-05 Hartmut chg: {@link #getString(int, int)} now may contain 0-character. Only the 0-bytes on end are removed because they are fill-bytes
   *   for a 4-byte-alignment. _getString(int, int) removed because it was the same.
   * <li>2014-08-23 Hartmut chg: Member idxFirstChild removed, the information is contained in {@link #sizeHead}. 
   * <li>2014-08-23 Hartmut new: this class ByteDataAccessBase works without virtual methods (overrideable), it is proper for usage in C-language
   *   and it is not necessary. The {@link #sizeHead} is set by construction. Only for the compatible now deprecated {@link ByteDataAccess} the sizeHead 
   *   is able to set via the package-private {@link #_setSizeHead(int)}.  
   * <li>2014-01-12 Hartmut new: Java4C.inline for C-compilation. 
   * <li>2013-12-08 Hartmut new: {@link #ByteDataAccessBase(int, int)} as super constructor with given head and data size.
   *   {@link #addChild(ByteDataAccessBase)} accepts an initialized not used child. Uses {@link #kInitializedWithLength}.
   *   That is the possibility to work without dynamic linked methods {@link #specifyLengthElement()} etc. for proper work
   *   especially for C usage. The overridden methods {@link #specifyEmptyDefaultData()}, {@link #notifyAddChild()} etc.
   *   are proper to use in C too with the concept of the dynamic linked methods, but there does not be necessary.
   *   It is an optimizing for C. Maybe in future the {@link #specifyLengthElement()} and {@link #specifyLengthElementHead()}
   *   may be depreciated because the new variant of initialized children is better to use, more simple for usage. 
   *   But it should be compatible with older versions. 
   * <li>2013-12-08 Hartmut chg: {@link #reset(int, int)} is protected now and has a second parameter. necessary public? 
   * <li>2012-12-15 Hartmut chg: Some changes are done which cleans up this class. If any problem occurs, the {@link ByteDataAccessOld}
   *   can be used. It is compatible with the last version before this changes.
   *   <ul>
   *   <li>The currentChild is not necessary. The only reason to have that association is: changes of size in parent
   *     force changes in the current child. But it is contrary: The changes should be done in the child and should affect
   *     the indices in all parents which should be known all in the child. If a child is added and it is never used furthermore
   *     then an unnecessary dangling reference exists. If the child instance is reused and detach is not called, mistakes are happen.
   *   <li>the next() and rewind() are removed. It is an unused old concept.  
   *   </ul>
   * <li>2012-04-07 Hartmut new: {@link #reset(int)}, some comments.
   * <li>2012-03-00 Hartmut Note: compare it with java.nio.ByteBuffer. But a ByteBuffer is abstract.
   * <li>2010-12-20: Hartmut chg: remove the toString-method using StringFormatter, because it is too complex for C-usage.
   *   The toString was only able to use for debugging.
   * <li>2010-02-02: Hartmut new:  getChildFloat(), getChildDouble().
   * <li>2010-01-16: Hartmut chg:  setBigEndian in now public. It should be better because the same user data may be interpreted in both versions depending on a parameter.
   * <li>2005..2009: Hartmut: some changes
   * <li>2005 Hartmut created
   * </ul>
   * <b>Copyright/Copyleft</b>:
   * For this source the LGPL Lesser General Public License,
   * published by the Free Software Foundation is valid.
   * It means:
   * <ol>
   * <li> You can use this source without any restriction for any desired purpose.
   * <li> You can redistribute copies of this source to everybody.
   * <li> Every user of this source, also the user of redistribute copies
   *    with or without payment, must accept this license for further using.
   * <li> But the LPGL is not appropriate for a whole software product,
   *    if this source is only a part of them. It means, the user
   *    must publish this part of source,
   *    but don't need to publish the whole source of the own product.
   * <li> You can study and modify (improve) this source
   *    for own using or for redistribution, but you have to license the
   *    modified sources likewise under this LGPL Lesser General Public License.
   *    You mustn't delete this Copyright/Copyleft inscription in this source file.
   * </ol>
   * If you are indent to use this sources without publishing its usage, you can get
   * a second license subscribing a special contract with the author. 
   * 
   * @author Hartmut Schorrig = hartmut.schorrig@vishia.de
   * 
   * 
   */
  public final static String version = "2018-09-18";
  
  
  
  /**Number of Memory locations (usual bytes) for the head of this instance's Type.  
   * Set on construction.
   */
  protected final int sizeHead;
  
  /** The array containing the binary data. Note this reference should be private, not protected because the kind of access
   * may be changed or held more flexible for newer versions. The usage should not deal with this reference immediately.
   * Use addChild etc. to adapt to data. */
  protected @Java4C.PtrVal byte[] data;
  
  /**Index of the beginning of the actual element in data*/
  private int ixBegin;

  /**Index of the beginning of the element inside the parent, relative to the parent. */
  private int ixBeginLocal;

  /**Index of the currents child end respectively the position of a new child.
   * If no current child is known this index is equal ixBegin + sizeHead, it is the position after the head. 
   * If the length of the current child is not known, this index is <= -1. That is after {@link #addChild(ByteDataAccessBase, int)} with lenght=-1.
   */
  private int ixNextChild;

  /** Index of the end of the actual element in data. If {@link #bExpand} is set, this idxEnd and the idxEnd of all parents are increased
   * if an child was added. If bExpand==false then this value is set via the {@link #addChild(ByteDataAccessBase, int)} or {@link #addChildAt(int, ByteDataAccessBase, int)}.*/
  private int ixEnd;

  /**True if the {@link #ixEnd} should not be set to the {@link #sizeHead} on removing children. */
  private boolean bExpand;

  /** Flag is set or get data in big endian or little endian (if false)*/
  protected boolean bBigEndian;
  
  /**If false then never an exception is thrown, Instead the work is done as soon as possible. 
   * See descriptions and return values of some methods.
   */
  protected boolean bExc = true;

  /** The parent element, necessary especially for expand(), also for {@link #removeChild(ByteDataAccessBase)}
   * More as one children can refer the same parent. But this refers only the current child.
   */
  private ByteDataAccessBase parent;

  /**The last added child, null either if a child is not added or a child was added but the instance is used meanwhile otherwise.
   * This reference is used in the productive code only for {@link #removeChild()}. It is contained here especially for debugging.
   * Therefore it is private to forbid other usage.
   */
  private ByteDataAccessBase currChild;

  /**The charset to build Strings.*/
  @Java4C.SimpleRef
  private Charset charset = Charset.forName("ISO-8859-1") ;   //NOTE: String(..., Charset) is only support from Java 6
  
  /**Use especially for test, only used in toString(). */ 
  @Java4C.Exclude
  protected StringFormatter toStringformatter = null;


  /**Any instance of ByteDataAccessBase is associated to a determined derived instance which has defined head size.
   * The sizeHead can be given with -1 only for derived instances of {@link ByteDataAccess}.
   * That class defines a method {@link ByteDataAccess#specifyLengthElementHead()} to get the head's size.
   * @param sizeHead The size of head data, it is the number of bytes.
   */
  public ByteDataAccessBase(int sizeHead){
    this.sizeHead = sizeHead;
  }
  
  
  /**Constructs a new empty instance with a given head size and a given size for children.
   * That instance is not expandable.
   * @deprecated Remark: Because {@link #assign(byte[], int)} decides on the size of data, and {@link #addChild(ByteDataAccessBase, int)}
   * decides on the size of data of a child, this constructor makes no sense. Jchartmut 2015-04-12
   *  
   * @param sizeHead The size of head data, it is the number of bytes.
   * @param sizeData number of significant bytes in data for all children.
   * */
  protected ByteDataAccessBase(int sizeHead, int sizeData){
    assert(sizeHead >=0);
    assert(sizeData >0);
    this.sizeHead = sizeHead;
    ixBeginLocal = ixBegin = 0;
    ixEnd = sizeData;
    bExpand = false;
    ixNextChild = sizeHead;
    parent = null;
  }


  
  
  /**Sets the big or little endian mode. 
   *
   * @param val true if big endian, hi byte at lower adress, false if little endian.
   */
  @Java4C.Inline
  public final void setBigEndian(boolean val)
  { bBigEndian = val;
  }


  /**Sets whether an exception should be thrown on adding a child or it should be return with false to continue
   * as soon as possible without exception handling. This is especially for C programming if an exception handling
   * is not desired. It is usefull in Java too depending on the user solution.
   * The exception state is inherited to all added childs. It should be set only for the root instance. 
   *
   * @param val true if addChild should throw an exception on error, false if addChild... operations should be return false on error.
   *   Because the default value for the exception is true, the user should set setException(false) usually.
   */
  @Java4C.Inline
  public final void setException(boolean val)
  { bExc = val;
  }


  @Java4C.Exclude
  public final void setCharset(String value) {
    charset = Charset.forName(value);
  }
  
  
  

  
  /** Returns the content of 1 to 8 bytes inside the actual element as a long number,
   * big- or little-endian depending on setBigEndian().
   * This method is protected because at user level its using is a prone to errors because the idx is free related.
   *
   * @param idxInChild The position of leading byte in the actual element, the data are taken from data[idxBegin+idx].
   * @param nrofBytesAndSign If positiv, than the method returns the unsigned interpretation of the bytes.
   *   If negative, than the return value is negative, if the last significant bit of the given number of bytes is set.
   *   The value represents the number of bytes to interprete as integer. It may be 1..8 respectively -1...-8.   
   * @return the long value in range adequate nrof bytes.
   * @since 2009-09-30: regards negative nrofBytesAndSign. Prior Versions: returns a signed value always.
   * */
  @Java4C.NoStackTrace 
  protected final long _getLong(final int idxInChild, final int nrofBytesAndSign)
  { if(!checkData()) { return 0; }
    long val = 0;
    int idxStep;
    int idx;
    final int nrofBytes;
    final boolean bSigned;
    if(nrofBytesAndSign >=0)
    { nrofBytes = nrofBytesAndSign;
      bSigned = false;
    }
    else{
      nrofBytes = - nrofBytesAndSign;
      bSigned = true;
    }
    if(bBigEndian)
    { idx = ixBegin + idxInChild;
      idxStep = 1;
    }
    else
    { idx = ixBegin + idxInChild + nrofBytes -1;
      idxStep = -1;
    }
    int nByteCnt = nrofBytes;
    do
    { val |= data[idx] & 0xff;
      if(--nByteCnt <= 0) break;  //TRICKY: break in mid of loop, no shift operation.
      val <<=8;
      idx += idxStep;
    }while(true);  //see break;
    if(bSigned){
      int posSign = (nrofBytes*8)-1;  //position of sign of the appropriate nrofBytes 
      long maskSign = 1L<<posSign;
      if( (val & maskSign) != 0)
      { long bitsSign = 0xffffffffffffffffL << (posSign);
        val |= bitsSign;  //supplement the rest bits of long with the sign value,it's negativ.   
      }
    }  
    return val;
  }

  
  
  /** Returns the content of 1 to 4 bytes inside the actual element as a int number,
   * big- or little-endian depending on setBigEndian().
   * This method is protected because at user level its using is a prone to errors because the idx is free related.
   *
   * @param idxInChild The position of leading byte in the actual element, the data are taken from data[idxBegin+idx].
   * @param nrofBytesAndSign If positiv, than the method returns the unsigned interpretation of the bytes.
   *   If negative, than the return value is negative, if the last significant bit of the given number of bytes is set.
   *   The value represents the number of bytes to interprete as integer. It may be 1..8 respectively -1...-8.   
   * @return the long value in range adequate nrof bytes.
   * @since 2009-09-30: regards negative nrofBytesAndSign. Prior Versions: returns a signed value always.
   * */
  @Java4C.NoStackTrace 
  protected final int _getInt(final int idxInChild, final int nrofBytesAndSign)
  { if(!checkData()) { return 0; }
    int val = 0;
    int idxStep;
    int idx;
    final int nrofBytes;
    final boolean bSigned;
    if(nrofBytesAndSign >=0)
    { nrofBytes = nrofBytesAndSign;
      bSigned = false;
    }
    else{
      nrofBytes = - nrofBytesAndSign;
      bSigned = true;
    }
    if(bBigEndian)
    { idx = ixBegin + idxInChild;
      idxStep = 1;
    }
    else
    { idx = ixBegin + idxInChild + nrofBytes -1;
      idxStep = -1;
    }
    int nByteCnt = nrofBytes;
    do
    { val |= data[idx] & 0xff;
      if(--nByteCnt <= 0) break;  //TRICKY: break in mid of loop, no shift operation.
      val <<=8;
      idx += idxStep;
    }while(true);  //see break;
    if(bSigned){
      int posSign = (nrofBytes*8)-1;  //position of sign of the appropriate nrofBytes 
      long maskSign = 1L<<posSign;
      if( (val & maskSign) != 0)
      { long bitsSign = 0xffffffffffffffffL << (posSign);
        val |= bitsSign;  //supplement the rest bits of long with the sign value,it's negativ.   
      }
    }  
    return val;
  }

  
  
  /**sets the content of 1 to 8 bytes inside the actual element as a long number,
   * big- or little-endian depending on setBigEndian().
   * This method is protected because at user level its using is a prone to errors because the idx is free related.
   *
   * @param idx the position of leading byte in the actual element, the data are set to data[idxBegin+idx].
   * @param nrofBytes The number of bytes of the value. 
   * @param val the long value in range adequate nrof bytes.
   * */
  @Java4C.NoStackTrace 
  protected final void _setLong(int idx, int nrofBytes, long val)
  { if(!checkData()) { return; }
    int idx1, nrofBytes1 = nrofBytes; long val1 = val;  //prevent change of parameters, use register internally.
    int idxStep;
    if(bBigEndian)
    { idx1 = ixBegin + idx + nrofBytes -1;
      idxStep = -1;
    }
    else
    { idx1 = ixBegin + idx;
      idxStep = 1;
    }
    do
    { data[idx1] = (byte)(val1);
      if(--nrofBytes1 <= 0) break;
      val1 >>=8;
      idx1 += idxStep;
    }while(true);  //see break;
  }

  
  
  /**sets the content of 1 to 4 bytes inside the actual element as a long number,
   * big- or little-endian depending on setBigEndian().
   * This method is protected because at user level its using is a prone to errors because the idx is free related.
   *
   * @param idx the position of leading byte in the actual element, the data are set to data[idxBegin+idx].
   * @param nrofBytes The number of bytes of the value. 
   * @param val the long value in range adequate nrof bytes.
   * */
  @Java4C.NoStackTrace 
  protected final void _setInt(int idx, int nrofBytes, int val)
  { if(!checkData()) { return; }
    int idx1, nrofBytes1 = nrofBytes, val1 = val;  //prevent change of parameters, use register internally.
    int idxStep;
    if(bBigEndian)
    { idx1 = ixBegin + idx + nrofBytes -1;
      idxStep = -1;
    }
    else
    { idx1 = ixBegin + idx;
      idxStep = 1;
    }
    do
    { data[idx1] = (byte)(val1);
      if(--nrofBytes1 <= 0) break;
      val1 >>=8;
      idx1 += idxStep;
    } while(true);  //see break;
  }

  
  

  /**Increments the {@link #ixNextChild} and/or increments the ixEnd of this and all parents.
   * It is called if a new child is added inside method addChild(child) and recursively to correct
   * in all parents. It is called for {@link #setLengthElement(int)}
   * 
   * @param ixNextChildNew The possible set ixNextChild position, 0 if not to change.
   * @param ixEndNew The new end position, ixEnd is only changed if it is greater the current one.
   */
  private final void _expand(int ixNextChildNew, int ixEndNew)
  { assert(ixEndNew < 0 || ixEndNew >= ixBegin + sizeHead);
    if(!checkData()) { return; }
    if(ixEndNew > data.length){
      throw new IllegalArgumentException("child long as data, data.length= " + data.length + ", ixChildEndNew= " + ixEndNew);
    }
    if(bExpand) {
      if(ixEnd < ixEndNew) { 
        //do it only in expand mode
        ixEnd = ixEndNew;
      }
      if(ixEnd < ixNextChildNew) { //If ixEnd is less because it is the old one.
        //do it only in expand mode
        ixEnd = ixNextChildNew;
      }
      if(parent != null)
      { parent._expand(ixEnd, ixEnd);  //all parents nextChild set to end of child, expand the parent if necessary.
      }
    } else {
      //not in expand mode
      if(ixEndNew >=0) { //-1: don't use! 
        ixEnd = ixEndNew;  //it is valid.     
      }
      if(parent != null)
      { parent._expand(ixEndNew, -1);  //all parents nextChild set to end of child, don't change the parent's ixEnd!
      }
    }
    if(ixNextChild < ixNextChildNew)  { 
      if(ixNextChildNew > ixEnd){
        throw new IllegalArgumentException("next child pos after ixend = " + ixEnd + ", ixNextChilNew= " + ixNextChildNew);
      }
      ixNextChild = ixNextChildNew;
    }
  }

  
  /**This method can be overridden by Java applications for derived instances which contains elements of this class
   * assigned at defined positions in data. The method will be called automatically as overridden method for Java applications.
   * For C applications this method will not supported because that would be the only one reason for virtual methods of this class. Keep it simple in C!
   * For C applications the assignment to fix children should be done by special methods of the derived class. 
   */
  @Java4C.Exclude
  protected void assignDataToFixChildren(){}

  /**Assigns new data to this element at given index in data. This method should be used only for an root element.  
   * If the element was used before, its connection to an other parent is dissolved.
   * The position of the next child {@link #ixNextChild} is set always after the known head data of this derived type
   * stored in the final {@link #sizeHead}.
   * <br>
   * @param data The dataP. The length of data may be greater as the number of the significant bytes.
   * @param lengthData absolute Number of significant bytes in data from start of data (index=0).
   *   <ul>
   *   <li>If length is > data.length, an exception is thrown.
   *   <li>If the length is < sizeHead (especially 0), then the access is expand-able. {@link #bExpand} = true.
   *   <li>If the length is >={@link #sizeHead()}, it defines the size of this access. Between the head and this length
   *     some children can be added to access that data. The access is not expanded. Adding a child after this given length
   *     throws an exception.
   *   </ul>  
   * @param index Start position in data, often 0. 
   * @throws IllegalArgumentException 
   */
  public final void assign(@Java4C.PtrVal byte[] dataP, int lengthData, int index) 
  throws IllegalArgumentException
  { assert (index >= 0 && this.sizeHead >=0);
    detach();
    this.data = dataP;
    ixBegin = index;
    ixBeginLocal = index;
    parent = null;
    currChild = null;
    bExpand = lengthData < sizeHead;  //expand if the data have no head.
    ixNextChild = ixBegin + sizeHead;
    //lengthData is inclusively head. maybe checl lengthData, >=sizeHead or 0.
    this.ixEnd = bExpand ? this.ixBegin + this.sizeHead : this.ixBegin + lengthData;
    { //@Java4C.Exclude
      if(data !=null && ixEnd > data.length)
      { @Java4C.StringBuilderInThreadCxt(sign="ByteDataAccess-error reset") String msg = "not enough data bytes, requested=" + ixEnd + ", buffer-length=" + data.length;
        throw new IllegalArgumentException(msg);
      }
    }
    
    {//@Java4C.Exclude
      ////TODO assignDataToFixChildren();
    }
  }
  
  /**Assigns data with a given length. This method is used usual to read data. Writing as 'changing' is possible too.
   * The expand flag is set to false if the given lengthData is >= the {@link #sizeHead()}. 
   * This method should be used only for an root element respectively this is a root element after calling this method.
   * If the element was used before, its connection to an other parent is dissolved.
   *
   * @param data The data which are accessed with this instance
   * @param lengthData The known length of data. If 0 then the instance is expandable, see {@link #isExpandable()}.
   */
  @Java4C.Inline
  public final void assign(@Java4C.PtrVal byte[] data, int lengthData){ assign(data, lengthData, 0); } 
  
  
  
  /**Assigns the given data with its given length to read the data. 
   * This method is also usefully to {@link #detach()} a current data buffer, call <code>assign(null);</code>.
   * @param dataP
   */
  @Java4C.Inline
  public final void assign(@Java4C.PtrVal byte[] dataP){ 
    if(dataP == null) { 
      detach();
    } else {
      assign(dataP, dataP.length, 0); 
    }
  } 
  
  /**Initializes a top level instance, the data will be cleared, set to 0, overall.
   * <br>
   * If this instance is using before, its connection to an other parent is dissolved.
   * <br>
   * @param data The data. It should be not null. The data are filled with 0 initial. 
   */
  @Java4C.Inline
  final public void assignClear(@Java4C.PtrVal byte[] data) 
  { Arrays.fill(data, (byte)0);
    assign(data, -1, 0);
  }

  

  /**assigns the element to the given position of the parents data to present a child of the parent
   * with a defined length.
   * The difference to {@link addChild(ByteDataAccessBase)} is: The position is given here
   * directly, it should not be the current child but a free child.  
   * <br>
   * The data reference is copied, the idxBegin of this element
   * is set to the idxChild given as parameter.
   * All other indices are set calling {@link specifyLengthElementHead()}: idxChild
   * and {@link specifyLengthElement()}: idxEnd.
   * <br>
   * If the element was used before, its connection to an other parent is dissolved.
   * @param parent The parent. It should reference data.
   * @param lengthChild Number of the bytes of the free child.
   * @param idxChildInParent The index of the free child in the data.
   * @throws IllegalArgumentException If the indices are wrong in respect to the data.
   */
  @Java4C.Inline
  final public void assignAt(int idxChildInParent, int lengthChild, ByteDataAccessBase parent)
  throws IllegalArgumentException
  { if(this == parent) {
      if(idxChildInParent !=0) throw new IllegalArgumentException("TODO");
    } else {
      detach();
      this.bBigEndian = parent.bBigEndian;
      this.bExc = parent.bExc;
      this.parent = parent;
      this.ixBegin = parent.ixBegin + idxChildInParent;
      this.ixBeginLocal = idxChildInParent;
      currChild = null;
      this.bExpand = lengthChild < sizeHead;  //expand if the data have no head.
      ixNextChild = ixBegin + sizeHead;
      //lengthData is inclusively head. maybe checl lengthData, >=sizeHead or 0.
      this.ixEnd = bExpand ? this.ixBegin + this.sizeHead : this.ixBegin + lengthChild;
      //old: assign(parent.data(), parent.ixBegin + idxChildInParent + lengthChild, parent.ixBegin + idxChildInParent);
    }
  }


  /**assigns the element to the given position of the parents data to present a child of the parent.
   * The length of the child is limited to TODO the length of head - or not limited.
   * @param parent The parent. It should reference data.
   * @param idxChildInParent The index of the free child in the data.
   * @throws IllegalArgumentException If the indices are wrong in respect to the data.
   */
  @Java4C.Inline
  final public void assignAt(int idxChildInParent, ByteDataAccessBase parent)
  throws IllegalArgumentException
  { assignAt(idxChildInParent, sizeHead, parent);
  }


  

  

  /**Assigns this element to the same position in data, but it is another view. 
   * This method should be called inside a assignCasted() method if a inner head structure is known
   * and the conclusion of this structure is possible. At result, both ByteDataAccessBase instances reference the same data,
   * in different views.
   * @param src The known data access
   * @param offsetCastToInput typical 0 if single inherition is used.
   * @param lengthDst it is the same like {@link #assign(byte[], int)}: If 0 then the child is set to expandable
   *   and its {@link #ixEnd()} is set to the {@link #sizeHead()}. If >= sizeHead then this instance is not expandable
   *   and this value determines the valid number of data which are able to access via it.
   * @throws IllegalArgumentException if a length of the new type is specified but the byte[]-data are shorter. 
   *                         The length of byte[] is tested. 
   * @deprecated it is confuse. Use 
   */
  @Java4C.Inline
  @Deprecated
  final protected void assignCasted(ByteDataAccessBase src, int offsetCastToInput, int lengthDst)
  throws IllegalArgumentException
  { assign(src.data(), lengthDst, src.ixBegin + offsetCastToInput);
    bExpand = src.bExpand;
    bBigEndian = src.bBigEndian;
    bExc = src.bExc;
    if(lengthDst >0){
      setLengthElement(lengthDst);
    }
    //_expand(lengthDst, lengthDst); //if lengthDst ==0 it does nothing.
    //lengthDst is unsused, not necessary because lengthElementHead is knwon!
  }

  
  
  final private void copyStdValues(ByteDataAccessBase src) {
    this.data = src.data;
    this.bExpand = src.bExpand;
    this.bExc = src.bExc;
    this.bBigEndian = src.bBigEndian;
    this.charset = src.charset;
  }
  
  
  
  
  /**This method is intend to use to change the view to the data from a firstly simple view (src) to the really type
   * if the type is detected inside the (head) data of the firstly view. It is important that the change of view
   * is done for the whole data, represented with the {@link #isCurrentChildInParent()} relation.
   * <br> 
   * The operation is protected because it should be only used in an derived class to cast from the src instance to this derived instance
   * <br>
   * This instance is filled with the data from src, but with other length.
   * If src is the current child of src.parent then this becomes the current child of parent 
   * and the parent.{@link #ixNextChild} is set proper to this: = this.{@link #ixBegin} + length.
   * That helps to step in parent via {@link #addChild(ByteDataAccessBase)}
   * @param src The firstly associated access instance to the data typically via {@link #addChild(ByteDataAccessBase)}, 
   *            it remains unchanged but it is no more the child of the parent. 
   * @param length if <= this.sizeHead, typically 0, then unused, the this{@link #sizeHead()} is used instead. 
   *   If > this.sizeHead then it determines the {@link #ixEnd()} of this and the parent {@link #ixNextChild}. 
   *   In that case it is a longer value because this instance should access head and inner children.
   */
  final protected void upcast(ByteDataAccessBase src, int length) {
    this.detach();
    copyStdValues(src);
    this.ixBegin = src.ixBegin;
    this.ixBeginLocal = src.ixBeginLocal;
    this.ixNextChild = this.ixBegin + this.sizeHead;
    if(length > this.sizeHead) { //given deterministic length
      this.ixEnd = this.ixBegin + length;
    } else {
      this.ixEnd = this.ixBegin + this.sizeHead;
    }
    this.parent = src.parent;
    if(src.isCurrentChildInParent()) {
      this.parent.ixNextChild = this.ixEnd;
      this.parent.currChild = this;
      //TODO may be adjust the parent's parent too for ixNextChild!
    }
  }
  
  

  /**Returns true if the instance is set as expandable, see {@link #assign(byte[], int)}
   */
  @Java4C.Retinline
  public final boolean isExpandable(){ return bExpand; }

  /**Returns the given head size, which is set on constructor respectively which is a determined value of an derived instance of this.
   * @return the number of head bytes defined by construction of {@link ByteDataAccessBase#ByteDataAccessBase(int)}.
   */
  @Java4C.Retinline
  public final int sizeHead() { return sizeHead; }
  
  
  /** Returns the data buffer itself. The actual total length is got with getLengthTotal().
   * @return The number of bytes of the data in the buffer.
   */
  @Java4C.Retinline @Java4C.PtrVal 
  public final byte[] getData()
  { return getData(99);
  }



  /** Returns the data buffer itself. The actual total length is got with getLengthTotal().
   * @return The number of bytes of the data in the buffer.
   */
  @Java4C.Retinline @Java4C.PtrVal 
  public final byte[] data()
  { return getData(99);
  }



  /**Gets the data from parent if {@link #data} == null 
   * @param recursion if <0 ends and call {@link #throwexc(String, int)}
   * @return data, maybe null if all parents have null.
   * @since 2018-09
   */
  private final byte[] getData(int recursion)
  { if(this.data == null && parent !=null) {
      if(--recursion <0) {
        throwexc("too many recursions in parent relation",0);
        return null;
      }
      this.data = parent.getData(recursion-1);
      this.bBigEndian = parent.bBigEndian;
    }
    return this.data;
  }




  /** starts the calling loop of next().
   * The calling of next() after them supplies the first child element.
   *
   */
  @Java4C.Inline
  public final void XXXrewind() //same as removeChildren
  { ixNextChild = ixBegin + sizeHead;
    if(currChild !=null){
      currChild.detach();
      currChild = null;
    }
  }




  /**Returns the length of the head. This method returns the size of the head given on construction
   * The size of the head cannot changed for an existing instance.
   */ 
  @Java4C.Retinline
  public final int getLengthHead(){ return sizeHead; }
  
  
  
  /**Returns the length of the element with all yet added children. 
   * It is the current used length of this element. Note that {@link #getLength()} returns the length
   * which may be not evaluated with children up to know but given with {@link #assign(byte[], int)}.
   * 
   * @return index of the current child's end of this element relative to this start position.
   */  
  @Java4C.Retinline
  final public int getLengthCurrent()
  { return ixNextChild - ixBegin; 
  }


  /** Returns the length of the existing actual element.
   * @return The number of bytes of the actual element in the buffer.
   *         It is (idxEnd - idxBegin).
   */
  @Java4C.Retinline
  final public int getLength()
  { return ixEnd - ixBegin;
  }




  /** Returns the length of the data.
   * @return The number of bytes of data in the buffer.
   *         It is idxEnd.
   */
  @Java4C.Retinline
  final public int getLengthTotal()
  { return ixEnd;
  }




  /**returns the number number of bytes there are max available from position of the current child. 
   * ,
   * the number of bytes to the end of buffer is returned.
   * 
   * @return nrofBytes that should fitting in the given data range from current child position 
   *                  to the end of data determines by calling assingData(...)
   *                  or by calling addChild() with a known size of child or setLengthElement() .
   */ 
  @Java4C.Retinline
  final public int getMaxNrofBytes()
  { if(!checkData()) { return 0; }
    if(bExpand) return data.length - ixBegin;
    else return ixEnd - ixBegin;
  }

  
  
  
  /**Checks whether a given size is possible as {@link #setLengthElement(int)} for the given instance.
   * <code>size >= sizeHead</code>.
   * If this is set to expand it returns <code>end <= data.length</code>
   * else it returns <code>end <= ixEnd</code>
   * whereby <code>end</code> means the end position of the element {@link #ixNextChild} 
   * which will be given after call of {@link #setLengthElement(size)}.
   * This method checks whether a calculated length is proper.
   * If this method returns false and {@link #setLengthElement(int)} is called notwithstanding
   * the {@link #setLengthElement(int)} throws an IllegalArgumentException. 
   * See {@link #getMaxNrofBytes()}, this routine is used.
   * @param size
   * @return
   */
  @Java4C.Retinline
  public final boolean checkLengthElement(int size)
  { return size >= sizeHead && getMaxNrofBytes() >=size;
  }
  

  



  @Java4C.Retinline final public boolean getBigEndian(){ return bBigEndian; }
  

  /**Sets the length of the element in this and all {@link #parent} of this. 
   * The {@link #ixEnd} of this is set with the (this.{@link #ixBegin}+length). 
   * This method sets this as not expandable, but does not change the expandable status of the parent.
   * The {@link #ixNextChild} of all parents are set to this index. So adding another child to the parent
   * starts with that given position. A parent is expand if necessary.
   * <br><br>
   * This routine is usefully for example if the length of a part of data is determined in the data itself.
   * 
   * @param length The length of data of this current (last) child.
   */
  @Java4C.Inline
  final public void setLengthElement(int length)
  { bExpand = false;
    _expand(0, ixBegin + length);
  }


  


  /**Sets all data of the head of this element to 0.
   * Note: If the element has not a head, this method does nothing.
   */
  @Java4C.Inline
  public final void clearHead(){
    if(!checkData()) { return; }
    Arrays.fill(data, ixBegin, ixBegin + sizeHead, (byte)0);
  }




  /**Sets all data of this element to 0.
   * Note: The idxEnd should be set to the end of the element.
   * This method is proper to use for a simple element only.
   */
  @Java4C.Inline
  public final void clearData(){
    if(!checkData()) { return; }
    Arrays.fill(data, ixBegin, ixEnd, (byte)0);
  }


  @Java4C.Retinline
  final public boolean isInUse()
  { return data() !=null;
  }

  
  final public boolean isCurrentChildInParent() {
    return parent !=null && parent.ixNextChild == this.ixNextChild;
  }
  

  /**returns true if the given number of bytes is sufficing in the data from position of next child. 
   * 
   * @param nrofBytes that should fitting in the given data range from current child position 
   *                  to the end of data determines by calling assingData(...)
   *                  or by calling addChild() with a known size of child or setLengthElement() .
   * @return true if it is okay, false if the nrofBytes are negative or to large.
   * @throws IllegalArgumentException see {@link getMaxNrofBytesForNextChild()} 
   */ 
  final public boolean sufficingBytesForNextChild(int nrofBytes) 
  throws IllegalArgumentException
  { int maxNrofBytesChild = getMaxNrofBytesForNextChild();
    return nrofBytes < 0 ? false : maxNrofBytesChild >= nrofBytes;
  }




  /**returns the maximal number of bytes which are available from position of a next current child. 
   * It returns (ixEnd - ixChildEnd) for non-expandable instances respectively data.length - ixChildEnd if this is expandable. 
   * With this information any child which head length is less or equal can be added to check whether it is matched to the data. 
   * 
   * @return nrofBytes that should fitting in the given data range from current child position 
   *                  to the end of data determines by calling assingData(...)
   *                  or by calling addChild() with a known size of child or setLengthElement() .
   */ 
  @Java4C.Retinline
  final public int getMaxNrofBytesForNextChild() throws IllegalArgumentException
  { if(!checkData()) { return 0; }
    return (bExpand ? data.length : ixEnd) - ixNextChild;
  }




  /**Adds a child Element after the current child or as first child after head.
   * With the aid of the child Element the data can be read or write structured.
   * <br><br>
   * The child instance will be initialized newly. Any old usage of the child instance will be ignored.
   * The child is a helper to get data and to manage the indices.
   * The child can be used to add further children to build a tree of children for complex structured data.
   * <br><br>
   * 
   *<br>
   * Some children can be added after a parent like following sample:
   * <pre>
   * ByteAccessDerivation child = new ByteAccessDerivation();  //empty and unassigned.
   * ...
   * parent.addChild(child);        //The byte[] data of parent are assigned, index after current child index of parent.
   * child.addChild(grandchild);    //By adding a child to this child, also the parent's index is corrected.
   * </pre>
   * <b>Indices in the parent and child</b>:<br>
   * If no child is added yet, the indices have the following values:
   * <ul>
   * <li>ixNextChild == sizeHead
   * <li>ixEnd == either given data length or sizeHead if bExpand == true.
   * <li>currChild == null
   * </ul>
   * A call of {@link addChild()} or its adequate derived addChildXY() sets the indices after the given current child:
   * <ul>
   * <li>ixNextChild == the index after the child, either after its head data or after the given length.
   * <li>ixEnd == either left unchanged if bExpand == false or incremented, maximum of added children
   * <li>currChild == added child, used for debug view, {@link #rewind()} etc.    
   * </ul>
   * The length of the current Child may be increased while evaluating the child's data. 
   * The user should call {@link #setLengthElement(int)} with the child or add some more grand children.
   * 
   * <br><br>
   * <b>data, charset, expand</b>:
   * That properties are inherit from the parent (this).
   *
   * @param child The child will be assigned with the data of this at index after the current child's end-index.
   *   Note that the child's sizeHead should be set correctly.
   * @param sizeChild The number of bytes which are used from the child or 0. If it is 0, then the child's sizeHead is used 
   *   to set the position of a possible next children {@link #ixNextChild}, elsewhere {@link #ixNextChild} is set using that value.
   *   The child itself does not use this value.
   * @return true if success, false if the child cannot be added because there is no space for it and {@link #bExc} is false. 
   * @throws IllegalArgumentException if the length of the head of the new current child is to far for the data.
   *         It means, child.idxEnd > data.length. 
   */
  final public boolean addChild(ByteDataAccessBase child, int sizeChild) 
  throws IllegalArgumentException
  { if(!checkData()) { return false; }
    child.detach();  //detatch the child from further usage.
    assert(sizeChild == 0 || sizeChild >= child.sizeHead);
    assert(child.sizeHead >=0);
    int ixChild1 = setIdxtoNextCurrentChild(sizeChild ==0 ? child.sizeHead: sizeChild);
    if(ixChild1 < 0) return false;
    child.ixBegin = ixChild1;   
    child.ixBeginLocal = child.ixBegin - this.ixBegin;
    child.bBigEndian = bBigEndian;
    child.bExc = bExc;
    child.bExpand = bExpand;
    child.data = this.data;
    child.parent = this;
    child.charset = this.charset;
    child.ixNextChild = child.ixBegin + child.sizeHead;  //the child does not contain grand children.
    //Set the end in child always to the end of parent. Either the parent is expandable, then the ixEnd will be increased,
    //or it is the size of data.
    child.ixEnd = this.ixEnd;  //bExpand ? child.ixNextChild : this.ixEnd;  //use the full data range maybe for child.
    currChild = child;
    return true;
  }

  
  
  /**Adds a child with its given head size without additional data space.
   * It calls {@link #addChild(ByteDataAccessBase, int)}.
   * @param child The child will be initialized newly.
   */
  @Java4C.Retinline
  final public boolean addChild(ByteDataAccessBase child){ return addChild(child, child.sizeHead); } 
  
  
  final public boolean addChildEmpty(ByteDataAccessBase child) 
  { if(addChild(child)) {  //first add the child
      child.clearHead(); //then clears its data.
      return true;
    } else return false;
  }
  
  
  
  final public boolean addChildEmpty(ByteDataAccessBase child, int sizeChild) 
  { if(addChild(child, sizeChild)) {  //first add the child
      child.clearData(); //then clears its data.
      return true;
    } else return false;
  }
  

  
  /**Adds a child at any position. This method is usefully if the data structure is known and specific elements should be accessed.
   * The child can be used to detect data in a sub structure.  
   * @param idxChild The index from the position of this (from this.{@link #ixBegin}).
   *   It is not the index inside the {@link #data}. 
   *   If {@link #ixBegin} ==0 because this is the root ByteDataAccess instance, it is the index in data.
   * @param child An empty instance for a child with given child.{@link #sizeHead}. Usual instances are re-used (especially in a C-environment). 
   *   child will be filled completely. A content before is cleared except the child.{@link #sizeHead}. This value should be known in the child.
   * @param sizeChild The size of the child. It may be 0, but >=0 (asserted)
   * @throws IllegalArgumentException
   */
  final public void addChildAt(int idxChild, ByteDataAccessBase child, int sizeChild) 
  throws IllegalArgumentException
  { assert(child.sizeHead >=0);
    assert(sizeChild >= child.sizeHead);
    if(!checkData()) { return; }
    if(child.parent !=null && child.parent.currChild == child){ child.parent.currChild = null; } //detatch
    child.data = data;
    int idxBegin = this.ixBegin + idxChild;
    child.ixBegin = idxBegin;
    child.ixBeginLocal = idxChild;
    child.ixEnd = idxBegin + sizeChild;
    child.ixNextChild = idxBegin + child.sizeHead;
    child.bBigEndian = bBigEndian;
    child.bExc = bExc;
    child.bExpand = false; //never expand on a fix position, no sense.  this.bExpand;
    child.parent = this;
    _expand(child.ixNextChild, child.ixEnd);  //expand the parent only if child.ixEnd is > parent.ixEnd
    //return bExpand;
  }

  
  /**Adds a child at any position with its head size. 
   * This method is usefully if the data structure is known and specific elements should be accessed.
   * The child can be used to detect data in a sub structure.  
   * @param idxChild The index from the position of this (from this.{@link #ixBegin}).
   *   It is not the index inside the {@link #data}. 
   *   If {@link #ixBegin} ==0 because this is the root ByteDataAccess instance, it is the index in data.
   * @param child An empty instance for a child with given child.{@link #sizeHead}. Usual instances are re-used (especially in a C-environment). 
   *   child will be filled completely. A content before is cleared except the child.{@link #sizeHead}. This value should be known in the child.
   * @throws IllegalArgumentException
   * @throws IllegalArgumentException
   */
  @Java4C.Retinline
  final public void addChildAt(int idxChild, ByteDataAccessBase child) 
  throws IllegalArgumentException
  { addChildAt(idxChild, child, child.sizeHead);
  }  
  
  

  /**Adds a child for 1 integer value without a child instance, and sets the value as integer.
   * 
   * @param nrofBytes of the integer
   * @return value in long format, cast it to (int) if you read only 4 bytes etc.
   * @throws IllegalArgumentException
   */
  public final boolean addChildInt(int nrofBytes, int value) 
  throws IllegalArgumentException
  { assert(nrofBytes >0);
    int ixChild1 = setIdxtoNextCurrentChild(nrofBytes);
    if(ixChild1 < 0) return false;
    //NOTE: there is no instance for this child, but it is the current child anyway.
    //NOTE: to read from idxInChild = 0, build the difference as shown:
    _setInt(ixChild1 - ixBegin, nrofBytes, value);
    return true;
  }




  /**Adds a child for 1 integer value without a child instance, and sets the value as integer.
   * 
   * @param nrofBytes of the integer
   * @return value in long format, cast it to (int) if you read only 4 bytes etc.
   * @throws IllegalArgumentException
   */
  public final boolean addChildInteger(int nrofBytes, long value) 
  throws IllegalArgumentException
  { assert(nrofBytes >0);
    int ixChild1 = setIdxtoNextCurrentChild(nrofBytes);
    if(ixChild1 < 0) return false;
    //NOTE: there is no instance for this child, but it is the current child anyway.
    //NOTE: to read from idxInChild = 0, build the difference as shown:
    _setLong(ixChild1 - ixBegin, nrofBytes, value);
    return true;
  }




  /**Adds a child for 1 float value without a child instance, and sets the value as float.
   * The indices of this are incremented to the next child position after the added float value.
   * 
   * The byte representation is the IEEE 754 floating-point "single format" bit layout, preserving Not-a-Number (NaN) values.
   * See {@link java.lang.Float#floatToRawIntBits(float)}.
   * 
   * @return value in long format, cast it to (int) if you read only 4 bytes etc.
   * @throws IllegalArgumentException
   */
  public final boolean addChildFloat(float value) 
  throws IllegalArgumentException
  { int ixChild1 = setIdxtoNextCurrentChild(4);
    if(ixChild1 < 0) return false;
    //NOTE: there is no instance for this child, but it is the current child anyway.
    //NOTE: to read from idxInChild = 0, build the difference as shown:
    setFloat(ixChild1 - ixBegin, value);
    return true;  
  }




  /**Adds a child with String value.
   * 
   * @param value String to add
   * @param sEncoding If null then use the standard encoding of the system-environment.
   * @param preventCtrlChars true then values < 0x20 are not set. 
   *                         If the String value contain a control character with code < 0x20,
   *                         a '?' is written. This behavior guarantees, that byte-values < 0x20 
   *                         can use to detect no-String elements, see {@link getByteNextChild()}. 
   * @throws IllegalArgumentException
   * @throws UnsupportedEncodingException
   */
  public final boolean addChildString(String value, String sEncoding, boolean preventCtrlChars) 
  throws IllegalArgumentException, UnsupportedEncodingException
  { int nrofBytes = value.length();
    int ixChild1 = setIdxtoNextCurrentChild(nrofBytes);
    if(ixChild1 < 0) return false;
    //NOTE: there is no instance for this child, but it is the current child anyway.
    //NOTE: to read from idxInChild = 0, build the difference as shown:
    _setString(ixChild1 - ixBegin, nrofBytes, value, sEncoding, preventCtrlChars);
    return true;  
  }




  /**Adds a child with String value.
   * 
   * @param valueCs String to add, @pjava2c=nonPersistent.
   * @param sEncoding String describes the encoding for translation from UTF16 to bytes. 
   * @throws IllegalArgumentException XXX
   * @throws UnsupportedEncodingException 
   */
  public final boolean addChildString(CharSequence valueCs, String sEncoding) 
  throws IllegalArgumentException, UnsupportedEncodingException
  { if(!checkData()) { return false; }
    int nrofBytes = valueCs.length();
    int ixChild1 = setIdxtoNextCurrentChild(nrofBytes);
    if(ixChild1 < 0) return false;
    //NOTE: there is no instance for this child, but it is the current child anyway.
    //NOTE: to read from idxInChild = 0, build the difference as shown:
    for(int ii=0; ii<nrofBytes; ++ii){
      byte charByte = (byte)(valueCs.charAt(ii));  //TODO encoding
      data[ixChild1+ii] = charByte;
    }
    return true;
  }




  /**Adds a child with String value.
   * 
   * @param value String to add, @pjava2c=nonPersistent.
   * @throws IllegalArgumentException
   */
  @Java4C.Inline
  public final void addChildString(CharSequence value) throws IllegalArgumentException
  { try{ addChildString(value, null); } 
    catch(UnsupportedEncodingException exc){ throw new RuntimeException(exc);} //it isn't able.
  }



  /**Adds a child for 1 short value without a child instance, returns the value as short.
   * 
   * @return The value which is represented by the next bytes after the current child before call this routine.
   */
  @Java4C.Retinline
  public final short getChildInt16()
  { int ixChild1 = setIdxtoNextCurrentChild(2);
    if(ixChild1 < 0) return 0;
    return getInt16(ixChild1 - ixBegin);
  }
  

  /**Adds a child for 1 short value without a child instance, returns the value as short.
   * 
   * @return The value which is represented by the next bytes after the current child before call this routine.
   */
  @Java4C.Retinline
  public final int getChildUint16()
  { int ixChild1 = setIdxtoNextCurrentChild(2);
    if(ixChild1 < 0) return 0;
    return getUint16(ixChild1 - ixBegin);
  }
  

  /**Adds a child for 1 short value without a child instance, returns the value as short.
   * 
   * @return The value which is represented by the next bytes after the current child before call this routine.
   */
  @Java4C.Retinline
  public final short getChildUint8()
  { int ixChild1 = setIdxtoNextCurrentChild(1);
    if(ixChild1 < 0) return 0;
    return getUint8(ixChild1 - ixBegin);
  }
  

  /**adds a child for 1 integer value without a child instance and returns the value as long integer.
   * 
   * @param nrofBytes of the integer, if negative then gets as signed integer, elsewhere as unsigned
   * @return value in long format, cast it to (int) if you read only 4 bytes etc.
   * @throws IllegalArgumentException if not data has not enaught bytes.
   */
  public final long getChildInteger(int nrofBytes) 
  throws IllegalArgumentException
  { //NOTE: there is no instance for this child, but it is the current child anyway.
    int bytes1 = nrofBytes < 0 ? -nrofBytes : nrofBytes;
    int ixChild1 = setIdxtoNextCurrentChild(bytes1);
    if(ixChild1 < 0) return 0;
    //NOTE: to read from idxInChild = 0, build the difference as shown:
    long value = _getLong(ixChild1 - ixBegin, nrofBytes);  
    return value;
  }
  
  

  
  /**Adds a child for 1 integer value without a child instance and returns the value as 32-bit-integer.
   * 
   * @param nrofBytes of the integer, if negative then gets as signed integer, elsewhere as unsigned, Only till 4.
   * @return value in int format.
   * @throws IllegalArgumentException if not data has not enaught bytes.
   */
  public final int getChildInt(int nrofBytes) 
  throws IllegalArgumentException
  { //NOTE: there is no instance for this child, but it is the current child anyway.
    int bytes1 = nrofBytes < 0 ? -nrofBytes : nrofBytes;
    int ixChild1 = setIdxtoNextCurrentChild(bytes1);
    if(ixChild1 < 0) return 0;
    //NOTE: to read from idxInChild = 0, build the difference as shown:
    int value = _getInt(ixChild1 - ixBegin, nrofBytes);  
    return value;
  }
  
  

  
  /**Adds a child for 1 float value without a child instance, but returns the value as integer.
   * 
   * @return value in float format
   * @throws IllegalArgumentException if not data has not enaught bytes.
   */
  public final float getChildFloat() 
  throws IllegalArgumentException
  { //NOTE: there is no instance for this child, but it is the current child anyway.
    int ixChild1 = setIdxtoNextCurrentChild(4);
    if(ixChild1 < 0) return 0;
    //NOTE: to read from idxInChild = 0, build the difference as shown:
    int intRepresentation = (int)_getLong(ixChild1 - ixBegin, 4);  
    return Float.intBitsToFloat(intRepresentation);
  }
  
  

  
  /**Adds a child for 1 double value without a child instance, but returns the value as integer.
   * 
   * @return value in float format
   * @throws IllegalArgumentException if not data has not enaught bytes.
   */
  public final double getChildDouble() 
  throws IllegalArgumentException
  { //NOTE: there is no instance for this child, but it is the current child anyway.
    int ixChild1 = setIdxtoNextCurrentChild(8);
    if(ixChild1 < 0) return 0;
    //NOTE: to read from idxInChild = 0, build the difference as shown:
    long intRepresentation = _getLong(ixChild1 - ixBegin, 8);  
    return Double.longBitsToDouble(intRepresentation);
  }
  
  

  
  
  /**Adds a child for a String value without a child instance, but returns the value as String.
   * 
   * @param nrofBytes of the String maybe with 0-bytes on end which will be removed (alignment).
   * @return value as String
   * @throws IllegalArgumentException if not data has not enough bytes.
   * @throws UnsupportedEncodingException 
   */
  public final String getChildString(int nrofBytes) 
  { //NOTE: there is no instance for this child, but it is the current child anyway.
    assert(nrofBytes >=0);
    int ixChild1 = setIdxtoNextCurrentChild(nrofBytes);
    if(ixChild1 < 0) return null;
    //NOTE: to read from idxInChild = 0, build the difference as shown:
    return getString(ixChild1 - ixBegin, nrofBytes);  
   
  }
  
  
  /**Removes the current child to assign another current child instead on the position of the current child.
   * This method is usefully if data are tested with several structures.
   * See {@link #removeChild(ByteDataAccessBase)}.
   * 
   * @param child
   * @throws IllegalArgumentException
   */
  @Java4C.Inline
  final public void removeChild() 
  throws IllegalArgumentException
  { if(currChild ==null) throw new IllegalStateException("programming error - a current child is not known yet.");
    removeChild(currChild);
  }


  
  /**Shorten the evaluated content of the data to the position of the given child. The given child is removed.
   * See {@link #removeChild()}. The {@link #ixEnd} is set to {@link #ixNextChild} if the expand mode is set.
   * In expand mode the {@link #ixEnd} is set to the {@link #ixNextChild}, to the current child position. 
   * The content which was written before at following positions is not removed. It means a content can be written 
   * with one child type, and checked with another child type after them.
   *  
   * @param child It should be a child of this. The parent of the child should be this.
   */
  final public void removeChild(ByteDataAccessBase child)
  {
    if(child.parent !=this) throw new IllegalArgumentException("programming error - child is not parent of this.");
    this.ixNextChild = child.ixBegin;   //set end index to the child's start
    if(bExpand) {
      this.ixEnd = this.ixNextChild;
    }
    if(currChild != null) { currChild.detach(); }
  }
  
  


  /**Remove all children. Let the head unchanged.
   * @since 2010-11-16
   */
  @Java4C.Inline
  public final void removeChildren()
  { if(currChild !=null){
      currChild.detach();
      currChild = null;
    }
    ixNextChild = ixBegin + sizeHead;
    if(bExpand){
      ixEnd = ixBegin + sizeHead;  //reset idxEnd only in expand method, let it unchanged in read mode.
    }
  }




  /**Remove all connections. Especially for children. */
  //@ J ava4C.Inline
  final public void detach()
  { if(parent !=null && parent.currChild == this){ 
      parent.currChild = null;  //detach in parent
    }
    if(currChild !=null) {
      currChild.detach();
      currChild = null;  //necessary if currentChild don't refers this parent because any error before.
    }
    data = null;
    parent = null;
    ixBegin = ixEnd = 0;
    //don't change ixBeginLocal.
    ixNextChild = 0;
    bExpand = false;
  }




  /**Returns the position of the Element data in the assigned buffer.
   * 
   * @return index of this element in the data buffer.
   */  
  @Java4C.Retinline
  final public int getPositionInBuffer()
  { return ixBegin;
  }




  /**Returns the position of a next child which can be added in the assigned buffer.
   * See {@link #getLengthCurrent()}.
   * 
   * @return index of the current child's end of this element in the data buffer.
   */  
  @Java4C.Retinline
  final public int getPositionNextChildInBuffer()
  { return ixNextChild;  ////
  }




  /**copies the data from another references data into this data.
   * The src is positioned to any child. That's data are copied in this data at the position of this child.
   * The length of both children should be equal. If it is not so, the routines throws or copies the lesser length
   * Note this method is never used in actual implementations of vishia. Check whether it is necessary.
   * */
  //@Java4C.Inline
  final public void copyDataFrom(ByteDataAccessBase src)
  throws IllegalArgumentException
  { if(!checkData()) { return; }
    int len = src.getLength();
    if(len > (this.ixEnd - this.ixBegin)) {
      len = this.ixEnd - this.ixBegin;
    }
    if(data.length < this.ixBegin + len){
      throwexc("copy, dst to small", len);
    }
    System.arraycopy(src.data,src.ixBegin,data,ixBegin,len);
  }




  /**copies some data to a int[], primarily to debug a content. 
   * @param dst This array is field, but only from data of the current element between idxBegin and idxEnd
   */
  public final void copyData(int[] dst)
  { int idxMax = ixEnd - ixBegin;
    if(idxMax/4 > dst.length) idxMax = 4* dst.length;
    int iDst = 0;
    for(int idx = 0; idx < idxMax; idx+=4)
    { dst[iDst++] = (int)_getLong(idx,4);
    }
  }






  /**Returns a String from the given position inside the actual element .
   * The bytes are interpreted in the given encoding. 
   * 0-characters on end of the spread in child till idx + nrofBytes are not taken as String characters.
   * They are 4-byte-alignment fill bytes usually. 
   * 0-bytes inside the String are taken as normal characters.
   * 
   * @param idx The start position inside the child.
   * @param nrofBytes The number of bytes to build the String.
   * @return The String representation of the bytes.
   *   Note: In C the String refers the bytes in {@link #data}.
   * @throws IndexOutOfBoundsException if any index is faulty.  
   */
  protected final String getString(int idx, int nrofBytes)
  { if(!checkData()) { return null; }
    int idxData = idx + ixBegin;
    int idxEnd1 = idxData + nrofBytes;
    assert(idxEnd1 <= ixEnd && idxEnd1 <= data.length);
    while( data[--idxEnd1] ==0 && idxEnd1 > idxData);  //skip 0 character on end
    int len = idxEnd1 +1 - idxData; //resulting len without 0-character.
    String value;
    value = new String(data, idxData, len, charset);
    return value;
  }


  /**Sets a String to the the given position inside the actual element .
   * The bytes are interpreted in the given encoding.
   * 
   * @param idx The start position inside the child.
   * @param nmax Maximal number of bytes
   * @param ss The String representation of the bytes.
   * @return Number of written chars.
   * @deprecated, use {@link _setString(int, int, String)}
   * @throws  
   */
  protected final int setString(int idx, int nmax, String ss)
  { if(!checkData()) { return 0; }
    if(ss.length()>nmax){ ss = ss.substring(0, nmax); } //truncate.
    /**Use a @java2c=ByteStringJc. In C there may not be a difference between the String
     * and the string of byte[].*/
    @Java4C.ByteStringJc byte[] byteRepresentation;
    try { byteRepresentation = ss.getBytes("ISO-8859-1");} 
    catch (UnsupportedEncodingException e){ byteRepresentation = null; }
    int len = byteRepresentation.length;
    if(len > nmax){ len = nmax; } //truncate.
    System.arraycopy(byteRepresentation, 0, data, ixBegin + idx, len);
    return len;
  }
  
  

  /**sets the content inside the actual element with the character bytes from the given String.
   *  
   * This method is protected because at user level its using is a prone to errors because the idx is free related.
   * 
   * @param idx the position in the actual element, the data are set to data[idxBegin+idx].
   * @param nrofBytes The length of the byte[] area to set. 
   *        If the String value is longer as nrofBytes, it will be truncated. No exception occurs.
   *        If the String is shorter as nrofBytes, the rest is filled with 0.
   * @param value The String value.
   * @param sEncoding The encoding of the String. null: Use standard encoding.
   * @param preventCtrlChars true then values < 0x20 are not set. 
   *                         If the String value contain a control character with code < 0x20,
   *                         a '?' is written. This behavior guarantees, that byte-values < 0x20 
   *                         can use to detect no-String elements, see {@link getByteNextChild()}. 
   * @throws UnsupportedEncodingException
   */
  protected final void _setString(final int idx, final int nrofBytes, final String value
    , String sEncoding, boolean preventCtrlChars) 
  throws UnsupportedEncodingException
  { if(!checkData()) { return; }
    int idxData = idx + ixBegin;
    int idxEnd = idxData + nrofBytes;
    /**@java2c=ByteStringJc. */
    byte[] chars;
    if(sEncoding == null){ sEncoding = "ISO-8859-1"; }
    chars = value.getBytes(sEncoding); 
    int srcLen = chars.length;
    if(srcLen > nrofBytes){ srcLen = nrofBytes; }
    for(int ii=0; ii < srcLen; ++ii)
    { byte cc = chars[ii];
      if(preventCtrlChars && cc < 0x20){ cc = 0x3f; } //'?' in ASCII
      data[idxData++] = cc;
    }
    //fill up the rest of the string with 0-chars. 
    while(idxData < idxEnd)
    { data[idxData++] = 0;
    }
  }
  
  
  
  /** Gets a float value from the content of 4 byte. The float value is red
   * according to the IEEE 754 floating-point "single format" bit layout, preserving Not-a-Number (NaN) values,
   * like converted from java.lang.Float.intBitsToFloat().
   */
  @Java4C.Retinline @Java4C.NoStackTrace
  protected final float getFloat(int idx)
  {
    int intRepresentation = getInt32(idx);
    float value = Float.intBitsToFloat(intRepresentation);
    return value;
  }
  
  @Java4C.Retinline @Java4C.NoStackTrace
  protected final double getDouble(int idx)
  {
    long intRepresentation = _getLong(idx,8);
    return Double.longBitsToDouble(intRepresentation);
   
  }
  
  @Java4C.Retinline @Java4C.NoStackTrace
  protected final long getInt64(int idx)
  { int nLo,nHi;
    if(bBigEndian)
    { nLo = getInt32(idx);
      nHi =  getInt32(idx + 4);
    }
    else
    { nLo = getInt32(idx+4);
      nHi =  getInt32(idx);
    }
    long val = nHi << 32;
    val |= nLo & 0xFFFFFFFF;
    return val;
  }

  /** Returns the content of 4 bytes inside the actual element as a integer number between -2147483648 and 2147483647,
   * big- or little-endian depending on setBigEndian().
   *
   * @param idx the position of leading byte in the actual element, the data are raken from data[idxBegin+idx].
   *            This is not the absolute position in data, idxBegin is added.<br/>
   * @return the integer value in range from -2147483648 and 2147483647
   * */
  @Java4C.NoStackTrace
  protected final int getInt32(int idx)
  { if(!checkData()) { return 0; }
    int val;
    if(bBigEndian)
    { val =  ((  data[ixBegin + idx])<<24)  //NOTE all 24 low-bits are 0
          |  (( (data[ixBegin + idx+1])<<16) & 0x00ff0000 ) //NOTE the high bits may be 0 or 1
          |  (( (data[ixBegin + idx+2])<< 8) & 0x0000ff00 ) //depending on sign of byte. Mask it!
          |  (( (data[ixBegin + idx+3])    ) & 0x000000ff );  //NOTE: the value has only 8 bits for bitwise or.
    }
    else
    { val =  (  (data[ixBegin + idx+3])<<24)  //NOTE all 24 low-bits are 0
          |  (( (data[ixBegin + idx+2])<<16) & 0x00ff0000 ) //NOTE the high bits may be 0 or 1
          |  (( (data[ixBegin + idx+1])<< 8) & 0x0000ff00 ) //depending on sign of byte. Mask it!
          |  (( (data[ixBegin + idx  ])    ) & 0x000000ff );  //NOTE: the value has only 8 bits for bitwise or.
    }
    return val;
  }

  
  @Java4C.Retinline
  @Java4C.NoStackTrace
  protected final int getUint32(int idx)
  { return getInt32(idx);
  }
  
  /** Returns the content of 2 bytes as a positive nr between 0..65535, big-endian
   * inside the actual element.
   *
   * @param idx the position of leading byte in the actual element, the data are raken from data[idxBegin+idx].
   *            This is not the absolute position in data, idxBegin is added.<br/>
   * @return the integer value in range from -32768 to 32767
   * */
  @Java4C.NoStackTrace
  protected final int getUint16(int idx)
  { if(!checkData()) { return 0; }
    int val;
    if(bBigEndian)
    { val =  (( (data[ixBegin + idx  ])<< 8) & 0x0000ff00 ) //depending on sign of byte. Mask it!
          |  (( (data[ixBegin + idx+1])    ) & 0x000000ff );  //NOTE: the value has only 8 bits for bitwise or.
    }
    else
    { val =  (( (data[ixBegin + idx+1])<< 8) & 0x0000ff00 ) //depending on sign of byte. Mask it!
          |  (( (data[ixBegin + idx  ])    ) & 0x000000ff );  //NOTE: the value has only 8 bits for bitwise or.
    }
    return val;
  }

  /** Returns the content of 2 bytes as a positive nr between 0..65535 inside the actual element.
   *
   * @param idx the position of leading byte in the actual element, the data are raken from data[idxBegin+idx].
   *            This is not the absolute position in data, idxBegin is added.<br/>
   * @return the integer value in range from -32768 to 32767
   * */
  @Java4C.NoStackTrace
  protected final short getInt16(int idx)
  { if(!checkData()) { return 0; }
    int val;
    if(bBigEndian)
    { val =  (( (data[ixBegin + idx  ])<< 8) & 0xff00 ) //depending on sign of byte. Mask it!
          |  (( (data[ixBegin + idx+1])    ) & 0x00ff );  //NOTE: the value has only 8 bits for bitwise or.
    }
    else
    { val =  (( (data[ixBegin + idx+1])<< 8) & 0xff00 ) //depending on sign of byte. Mask it!
          |  (( (data[ixBegin + idx  ])    ) & 0x00ff );  //NOTE: the value has only 8 bits for bitwise or.
    }
    return (short)val;
  }

  /** Returns the content of 1 bytes as ASCII
   * inside the actual element.
   *
   * @param idx the position of char in the actual element, the data are raken from data[idxBegin+idx].
   *            This is not the absolute position in data, idxBegin is added.<br/>
   * @return the ASCII value
   * */
  @Java4C.NoStackTrace
  protected final char getChar(int idx)
  { if(!checkData()) { return 0; }
    char val;
    val = (char) data[ixBegin + idx];
    return val;
  }
  
  /** Returns the content of 1 bytes as a positive or negative nr between -128..127
   * inside the actual element.
   *
   * @param idx the position of leading byte in the actual element, the data are raken from data[idxBegin+idx].
   *            This is not the absolute position in data, idxBegin is added.<br/>
   * @return the integer value in range from -32768 to 32767
   * */
  @Java4C.NoStackTrace
  protected final byte getInt8(int idx)
  { byte val;
    if(!checkData()) { return 0; }
    val = data[ixBegin + idx];
    return val;
  }

  
  /** Returns the content of 1 bytes as a positive or negative nr between -128..127
   * inside the actual element.
   *
   * @param idx the position of leading byte in the actual element, the data are raken from data[idxBegin+idx].
   *            This is not the absolute position in data, idxBegin is added.<br/>
   * @return the integer value in range from -32768 to 32767
   * */
  @Java4C.NoStackTrace
  protected final short getUint8(int idx)
  { short val;
    if(!checkData()) { return 0; }
    val = data[ixBegin + idx];
    if(val < 0){ val += 0x100; }
    return val;
  }
 
  
  
  

  
  protected final int getUint32(int idxBytes, int idxArray, int lengthArray)
  { if(idxArray >= lengthArray || idxArray < 0) throw new IndexOutOfBoundsException("getUint32:" + idxArray);
    return getUint32(idxBytes + 4*idxArray);
  }
  
  protected final int getInt32(int idxBytes, int idxArray, int lengthArray)
  { if(idxArray >= lengthArray || idxArray < 0) throw new IndexOutOfBoundsException("getInt32:" + idxArray);
    return getInt32(idxBytes + 4*idxArray);
  }
  
  protected final int getInt16(int idxBytes, int idxArray, int lengthArray)
  { if(idxArray >= lengthArray || idxArray < 0) throw new IndexOutOfBoundsException("getInt16:" + idxArray);
    return getInt16(idxBytes + 2*idxArray);
  }
  
  protected final int getInt8(int idxBytes, int idxArray, int lengthArray)
  { if(idxArray >= lengthArray || idxArray < 0) throw new IndexOutOfBoundsException("getInt8:" + idxArray);
    return getInt8(idxBytes + idxArray);
  }
  
  protected final int getUint16(int idxBytes, int idxArray, int lengthArray)
  { if(idxArray >= lengthArray || idxArray < 0) throw new IndexOutOfBoundsException("getUint16:" + idxArray);
    return getUint16(idxBytes + 2*idxArray);
  }
  
  protected final int getUint8(int idxBytes, int idxArray, int lengthArray)
  { if(idxArray >= lengthArray || idxArray < 0) throw new IndexOutOfBoundsException("getUint8:" + idxArray);
    return getInt8(idxBytes + idxArray);
  }
  
  protected final float getFloat(int idxBytes, int idxArray, int lengthArray)
  { if(idxArray >= lengthArray || idxArray < 0) throw new IndexOutOfBoundsException("getFloat:" + idxArray);
    return getFloat(idxBytes + 4*idxArray);
  }
  
  
  
  

  /** Set the content of 4 byte from a float variable. The float value is stored
   * according to the IEEE 754 floating-point "single format" bit layout, preserving Not-a-Number (NaN) values,
   * like converted from java.lang.Float.floatToRawIntBits().
   */
  @Java4C.Inline
  protected final void setFloat(int idx, float value)
  {
    int intRepresentation = Float.floatToRawIntBits(value);
    _setLong(idx, 4, intRepresentation);
  }



  /** Set the content of 8 byte from a double variable. The double value is stored
   * according to the IEEE 754 floating-point "double format" bit layout, preserving Not-a-Number (NaN) values,
   * like converted from java.lang.Double.doubleToRawLongBits().
   */
  @Java4C.Inline
  protected final void setDouble(int idx, double value)
  {
    long intRepresentation = Double.doubleToRawLongBits(value);
    _setLong(idx, 8, intRepresentation);
  }



  /** Set the content of 4 bytes as a integer number between -2147483648 and 2147483647,
   * big- or little-endian depended from setBigEndian().
   *
   * @param idx The position of leading byte in the current elements data.
   *            This is not the absolute position in data, idxBegin is added.<br/>
   * @param value The value in range 0..65535. The value is taken modulo 0xffffffff.
   * */
  protected final void setInt32(int idx, int value)
  { if(!checkData()) { return; }
    if(bBigEndian)
    { data[ixBegin + idx]   = (byte)((value>>24) & 0xff);
      data[ixBegin + idx+1] = (byte)((value>>16) & 0xff);
      data[ixBegin + idx+2] = (byte)((value>>8) & 0xff);
      data[ixBegin + idx+3] = (byte)(value & 0xff);
    }
    else
    { data[ixBegin + idx+3] = (byte)((value>>24) & 0xff);
      data[ixBegin + idx+2] = (byte)((value>>16) & 0xff);
      data[ixBegin + idx+1] = (byte)((value>>8) & 0xff);
      data[ixBegin + idx]   = (byte)(value & 0xff);
    }
  }

  /** Set the content of 1 bytes as a positive nr between 0..256.
   *
   * @param idx The position of leading byte in the current elements data.
   *            This is not the absolute position in data, idxBegin is added.<br/>
   * @param value The value in range 0..65535. The value is taken modulo 0xffff.
   * */
  @Java4C.Inline
  protected final void setInt8(int idx, int value)
  { if(!checkData()) { return; }
    data[ixBegin + idx] = (byte)(value & 0xff);
  }



  /** Set the content of 1 bytes as a positive nr between 0..255, big- or little-endian.
  *
  * @param idx The position of leading byte in the current elements data.
  *            This is not the absolute position in data, idxBegin is added.<br/>
  * @param value The value in range 0..65535. The value is taken modulo 0xff.
  * */
  @Java4C.Inline
  protected final void setUint8(int idx, int value)
  { setInt8(idx, value);  //its the same because modulo!
  }

  /** Set the content of 4 bytes as a positive nr between 0..2pow32-1, big- or little-endian.
  *
  * @param idx The position of leading byte in the current elements data.
  *            This is not the absolute position in data, idxBegin is added.<br/>
  * @param value The value as long. The value is taken modulo 0xffffffff.
  * */
  protected final void setUint32(int idx, long value)
  { if(!checkData()) { return; }
    //the same algorithm in source, but other action on machine level,
    //because value is long!
    if(bBigEndian)
    { data[ixBegin + idx]   = (byte)((value>>24) & 0xff);
      data[ixBegin + idx+1] = (byte)((value>>16) & 0xff);
      data[ixBegin + idx+2] = (byte)((value>>8) & 0xff);
      data[ixBegin + idx+3] = (byte)(value & 0xff);
    }
    else
    { data[ixBegin + idx+3] = (byte)((value>>24) & 0xff);
      data[ixBegin + idx+2] = (byte)((value>>16) & 0xff);
      data[ixBegin + idx+1] = (byte)((value>>8) & 0xff);
      data[ixBegin + idx]   = (byte)(value & 0xff);
    }
  }

  /** Set the content of 2 bytes from an integer between -32768..32768,
   * or from an integer number between 0..65535. The value is interpreted
   * from the input parameter with modulo 0x10000.
   * Big- or little-endian.
   *
   * @param idx The position of leading byte in the current elements data.
   *            This is not the absolute position in data, idxBegin is added.<br/>
   * @param value The value in range 0..65535. The value is taken modulo 0xffff.
   * */
  protected final void setInt16(int idx, int value)
  { if(!checkData()) { return; }
    if(bBigEndian)
    { data[ixBegin + idx]   = (byte)((value>>8) & 0xff);
      data[ixBegin + idx+1] = (byte)(value & 0xff);
    }
    else
    { data[ixBegin + idx+1] = (byte)((value>>8) & 0xff);
      data[ixBegin + idx]   = (byte)(value & 0xff);
    }
  }
  
  
  /** Set the content of 2 bytes as a positive nr between 0..65535, big- or little-endian.
  *
  * @param idx The position of leading byte in the current elements data.
  *            This is not the absolute position in data, idxBegin is added.<br/>
  * @param value The value in range 0..65535. The value is taken modulo 0xffff.
  * */
  @Java4C.Inline
  protected final void setUint16(int idx, int value)
  { setInt16(idx, value);  //its the same because modulo!
  }


  private final void throwexc(String text, int idxArray){
    @Java4C.StringBuilderInThreadCxt(sign="ByteDataAccess-exception") String textExc = text + idxArray; 
    throw new IndexOutOfBoundsException(textExc);
  }
  
  
  @Java4C.Inline
  protected final void setUint32(int idxBytes, int idxArray, int lengthArray, int val)
  { 
    if(idxArray >= lengthArray || idxArray < 0) throwexc("setUint32:", idxArray);   
    setUint32(idxBytes + 4*idxArray, val);
  }
  
  @Java4C.Inline
  protected final void setInt32(int idxBytes, int idxArray, int lengthArray, int val)
  { if(idxArray >= lengthArray || idxArray < 0) throwexc("setInt32:", idxArray);
    setInt32(idxBytes + 4*idxArray, val);
  }
  
  @Java4C.Inline
  protected final void setInt16(int idxBytes, int idxArray, int lengthArray, int val)
  { if(idxArray >= lengthArray || idxArray < 0) throw new IndexOutOfBoundsException("getInt16:" + idxArray);
    setInt16(idxBytes + 2*idxArray, val);
  }
  
  @Java4C.Inline
  protected final void setInt8(int idxBytes, int idxArray, int lengthArray, int val)
  { if(idxArray >= lengthArray || idxArray < 0) throw new IndexOutOfBoundsException("getInt16:" + idxArray);
    setInt8(idxBytes + idxArray, val);
  }
  
  
  @Java4C.Inline
  protected final void setUint16(int idxBytes, int idxArray, int lengthArray, int val)
  { if(idxArray >= lengthArray || idxArray < 0) throw new IndexOutOfBoundsException("getInt16:" + idxArray);
    setUint16(idxBytes + 2*idxArray, val);
  }
  
  @Java4C.Inline
  protected final void setUint8(int idxBytes, int idxArray, int lengthArray, int val)
  { if(idxArray >= lengthArray || idxArray < 0) throw new IndexOutOfBoundsException("getInt16:" + idxArray);
    setUint8(idxBytes + idxArray, val);
  }
  
  @Java4C.Inline
  protected final void setFloat(int idxBytes, int idxArray, int lengthArray, float val)
  { if(idxArray >= lengthArray || idxArray < 0) throw new IndexOutOfBoundsException("getInt16:" + idxArray);
    setFloat(idxBytes + 4 * idxArray, val);
  }
  
  
  
  
  /**Prepares a new child for this. It sets the this.{@link #ixNextChild} to the end of the new child given by argument sizeChild.
   * The size of the new child, at least its head size, should be known. It is given by the calling argument sizeChild.
   * <br><br>
   * This method is called while addChild. The state before is:
   * <ul>
   * <li>ixChildEnd is the actual end index of the current Child, 
   *     or the index of the first child (after head, may be also 0 if the head has 0 bytes), 
   *     if no child was added before. ixChildEnd is always positive and should be valid.
   * </ul>
   * The state after is:    
   * <ul>
   * <li>ixChildEnd is set to the ixChild + argument sizeChild.
   * </ul>
   * The size of the child may be increased later by calling {@link #addChild(ByteDataAccessBase)} for an added child or by calling 
   * {@link #setLengthElement(int)} for the child. Therefore an argument =0 is possible.
   * <br><br>
   * The method is package private because it should not invoked from the user directly. 
   * 
   * @param sizeChild yet known size of the child to add. It have to be >=0. 
   * @return position in data of the current child, that is the {@link #ixNextChild()} before this is processed.
   *   returns -1 if the child cannot be added.
   */
  private final int setIdxtoNextCurrentChild(int sizeChild) 
  //throws IllegalArgumentException
  { assert(sizeChild >=0);
    assert(ixNextChild >=0);          //==0 os possible on an empty element without head.
    if(!checkData()) { return 0; }
    int ixMax = bExpand? data.length : ixEnd;
    if(ixNextChild + sizeChild > ixMax) return RetOrException.illegalArgument(bExc, -1, "child on limit of expand");
    int ixChild1 = ixNextChild;
    ixNextChild += sizeChild;  
    _expand(ixNextChild, ixEnd);    //expand always the ixChildEnd
    return ixChild1;
  }


  protected final int ixBegin(){ return ixBegin; }
  
  protected final int ixNextChild(){ return ixNextChild; }
  
  protected final int ixEnd(){ return ixEnd; }
  
  
  
  /**Checks {@link #data} == null and gets the data from parent if necessary.
   * With them it is possible to call {@link #assign(byte[])} for the parent only and it runs.
   * If the data remain null {@link #throwexc(String, int)} is invoked. It may force an exeption if that is activated.
   * Elsewhere it returns false, to force prevent access to data in the calling routine. 
   * @return false if data remain == null
   * @since 2018-09
   */
  boolean checkData() {
    if(data == null && parent !=null) {
      data = parent.getData(99);
      bBigEndian = parent.bBigEndian;
    }
    if(data == null) {
      throwexc("--no data--",0);
      return false;
    } else {
      return true;
    }
  }
  
  
  
  
  
  /**Appends the information about the indices and the current Children.
   * For C it is excluded because it is only for test.
   * @see org.vishia.util.InfoFormattedAppend#infoFormattedAppend(org.vishia.util.StringFormatter)
   */
  @Java4C.Exclude
  @Override 
  public void infoFormattedAppend(StringFormatter u)
  { 
    //show content of head
    if(!checkData()) { u.add("--no-data--"); return; }
    int bytesHex = getLengthHead();
    if(bytesHex > 16){ bytesHex = 16; }
    if(bytesHex <0){ bytesHex = 0; }
    if(ixBegin + bytesHex > data.length){ bytesHex = data.length - ixBegin; }  
    infoAppendHead(u, bytesHex);
    if(bytesHex < 24 && currChild ==null) {
      int bytesHexChild = ixEnd - ixBegin - sizeHead;
      if(bytesHexChild >(24 - bytesHex)) { bytesHexChild = (24 - bytesHex); }  //don't show more as 24 bytes in sum.
      if(bytesHexChild >0) {
        u.add(": ").addHexLine(data, ixBegin + sizeHead, bytesHexChild, bBigEndian? StringFormatter.k4left: StringFormatter.k4right);
      }
    }
    else if(currChild !=null && u.length() < 2200) {
      u.add(", child ");
      currChild.infoFormattedAppend(u);  //it is possible that it is overridden.
    }
  }
  
  
  /**Appends information about the head of the given element.
   * @param u The destination buffer
   * @param bytesHex Number of shown bytes of data as hexa line.
   */
  @Java4C.Exclude
  protected void infoAppendHead(StringFormatter u, int bytesHex){
    u.addint(ixBegin, "333331")
    .add("..").addint(ixBegin + sizeHead,"333331")
    .add("..").addint(ixNextChild,"333331")
    .add(bExpand ? '+' : ':').addint(ixEnd,"333331").add(":");
    if(!checkData()) { u.add("--no-data--"); return; }
    u.addHexLine(data(), ixBegin, bytesHex, bBigEndian? StringFormatter.k4left: StringFormatter.k4right);
  }

 

  /**This method is especially usefully to debug in eclipse. 
    * It shows the first bytes of head, the position of child and the first bytes of the child.
    */
   @Override
   @Java4C.Exclude
   public String toString() 
   { //NOTE: do not create a new object in every call, it is uneffective.
     if(data==null){ return "no data"; }
     else
     { if(toStringformatter == null){ toStringformatter = new StringFormatter(); }
       else { toStringformatter.reset(); }
       
       //addToString(this,0);  //recursively call for all children, limitaded with recursCount.
       infoFormattedAppend(toStringformatter);
       final String ret = toStringformatter.toString();
       return ret;
     }  
   }


}
