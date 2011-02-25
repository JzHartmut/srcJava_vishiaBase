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
 * @author Hartmut Schorrig = hartmut.schorrig@vishia.de
 */
package org.vishia.byteData;

import java.io.UnsupportedEncodingException;

/**This class is a base class to control the access to binary data.
 * The binary data may typically used or produced from a part of software written in C or C++.
 * There the binary data are struct-constructs.
 * <br>
 * It is able to support several kinds of struct constructs:<ul>
 * <li>Simple <code>struct</code> are adequate mapped with a derivated class of this class, 
 *     using the protected commonly access methods like {@link _getInt(int, int)} with predefined indexes 
 *     in special methods like getValueXyz().</li>
 * <li>Complex <code>struct</code> with nested <code>struct</code> inside are mapped 
 *     with one derivated class per <code>struct</code>, define one reference per nested struct 
 *     and overwriting the method {@link assignDataToFixChilds()}</li>
 * <li>Base <code>struct</code> inside a <code>struct</code> (inherition in C) can be mapped with 
 *     extra derivated classes for the base struct and usind the
 *     {@link assignCasted_i(ByteDataAccess, int)}-method.</li>
 * <li>A pack of data with several struct may be mapped using the {@addChild(ByteDataAccess)}-method.
 *     Thereby a parent should be defined, and the structs of the pack are children of this parent. 
 *     That structs need not be from the same type.</li>
 * <li>packs of struct with parent are nestable, it is constructable as a tree of pack of structs. 
 *     The parent of the pack is the tree node. It is likewise a XML tree. 
 *     The data may be also transformed to a XML data representation
 *     or the data structure may be explained with a XML tree, but they are not
 *     XML data straight.</li>
 * </ul>
 * This application possibilities show a capable of development system to access binary data. 
 * The other, hand made way was calculate with indices of the byte[idx] specially user programmed. 
 * This class helps to make such complex index calculation needlessly. 
 * One struct at C level corresponds with one derivated class of ByteDataAccess. 
 * But last also a generation of the java code from C/C++-header files containing the structs is able to.
 * So the definition of byte positions are made only on one source. The C/C++ is primary thereby. 
 *      
 * <h2>children, currentChild, addChild</h2>
 * Children are used to evaluate or write different data structures after a known structure. 
 * The children may be from several derivated types of this class. 
 * With children and children inside children a tree of different data can be built or evaluated.
 * 
 * If no child is added yet, the indices have the following values:
 * <ul>
 * <li>idxCurrentChild = -1.
 * <li>idxCurrentChildEnd = index after known (named head-) data.
 * </ul>
 * A call of {@link next()} sets the indices to a possible but not yet defined current child:
 * <ul>
 * <li>idxCurrentChild = the index after the last known child or known (head-) data.
 *     It is idxCurrentChildEnd from state before.    
 * <li>idxCurrentChildEnd = -2, because the length of the child is unknown. 
 *     The -2 is used to mark call of next().
 * </ul>
 * A call of {@link addChild()} or its adequate addChildXY() sets the indices to the given current child:
 * <ul>
 * <li>idxCurrentChild = the index after known (head-) data, the index of the child.
 * <li>idxCurrentChildEnd = idxCurrentChild + {@link specifyLengthElement()} if this method returns >=0.     
 * <li>idxCurrentChildEnd = idxCurrentChild + {@link specifyLengthElementHead()} if this method returns >=0.     
 * <li>idxCurrentChildEnd = -1, because the length of the child is not known yet if both methods return -1. 
 * </ul>
 * The length of the current Child may be set while evaluating the child's data. 
 * The user should be call {@link setLengthElement(int)} with the child 
 * or {@link setLengthCurrentChildElement(int)} with the parent, respectively with this.
 * <ul>
 * <li>idxCurrentChild = is still the index of the child.
 * <li>idxCurrentChildEnd = idxCurrentChild + given length.
 * </ul>
 * If this methods are not called, but next() or addChild...() is called however, without a known length 
 * but this (the parent) knows the rules to determine the length of its possible children, 
 * it is possible to do that. The method {@link specifyLengthCurrentChildElement()} supplied the number of bytes.
 * But if this method is not overwritten in the inherited class, an exception is thrown.
 * 
 * 
 * <br>
 * The UML structure of such an class in a environment may be shown with the
 * followed object model diagram, <br>
 * <code> <+>---> </code>is a composition,
 * <code> <>---> </code>is a aggregation, <code> <|---- </code>is a inherition.
 *  <pre>
 *                      +-------------------------------+                 +----------+
 *                      | ByteDataAccess                |----data-------->| byte[]   |
 *                      |-------------------------------|                 +----------+
 *                      |idxBegin:int                   |
 *                      |idxChild:int                   |<---------------+ a known parent
 *  +-------------+     |idxEnd:int                     |---parent-------+ setted in addChild()
 *  | derivated   |     |-------------------------------|
 *  | user        |---|>|specifyLengthElement()         |
 *  | classes     |     |specifyLengthElementHead()     |--currentChild--+ the actual child element
 *  +-------------+     |specifyLengthCurrentChild()    |<---------------+
 *                      +-------------------------------+
 * </pre>
 *
 */
public abstract class ByteDataAccess
{
	/**The version. 
	 * <ul>
	 * <li>2010-12-20: Hartmut chg: remove the toString-method using StringFormatter, because it is too complex for C-usage.
	 *   The toString was only able to use for debugging.
	 * <li>2010-02-02: Hartmut new:  getChildFloat(), getChildDouble().
	 * <li>2010-01-16: Hartmut chg:  setBigEndian in now public. It should be better because the same user data may be interpreted in both versions depending on a parameter.
	 * <li>2005..2009: Hartmut: some changes
	 * <li>2005 Hartmut created
	 * </ul>
	 * 
	 */
	public static final int _version_ = 0x20101231;
	
  /** The array containing the binary data.*/
  protected byte[] data;

  /** Index of the beginning of the actual element in data*/
  protected int idxBegin;

  /** Index of the end of the actual element in data*/
  protected int idxEnd;

  /**True if the {@link #idxEnd} should be increment on adding children. It is to write. */
  protected boolean bExpand;
  
  /** Index of the first child element, it is after the head.*/
  protected int idxFirstChild;

  /** Index within the at position of the current child element.
   * If no current child is known, this index is -1. */
  protected int idxCurrentChild;

  /**Index of the currents child end.
   * If no current child is known this index is equal idxFirstChild, it is the position after the head. 
   * If the length of the current child is not known, this index is <= idxCurrentChild.
   */
  protected int idxCurrentChildEnd;

  /** Flag is set or get data in big endian or little endian (if false)*/
  protected boolean bBigEndian;

  /** The parent XmlBinCodeElement, necessary only for add() and expand().
   *
   */
  private ByteDataAccess parent;


  /** The child on end to add() something
   *
   */
  private ByteDataAccess currentChild;
  
  /**The charset.*/
  String charset;   //NOTE: String(..., Charset) is only support from Java 6
  //Charset charset;
  
  /** Definition of the code of end of information, return from next()*/
  public static final byte kEndOfElements = 0;

  /** Definition of the code of no information, return from next()*/
  public static final byte kNothing = (byte)(0xff);

  /** Aussage: es ist ein String (XML: text()), kein Tag im String*/
  public static final byte kText    = 1;

  /** coding: the value is undefined*/
  public static final byte kUndefined  = -0x3f;

  /** Index in the data, position of element code*/
  protected static final int kIdxElementCode = 0;

  /** Conversion of characters in range 0x80..0xbf */
  //protected static final String sConvertHiChars = "���?���^�~{}[|]���\\.............................................";


  /**returns the length of the head. It is specified by {@link specifyLengthElementHead()}
   * of the derivated class.
   */ 
  public final int getLengthHead(){ return idxFirstChild - idxBegin; }
  

  /** Sets the elements data to the default empty data.
   * This method should not called outside of this base class, likewise not in the derivated class itself.
   * This method is only called inside this base class itself inside the methods addChild() and assignEmpty().
   * But only the derivated class knows how to set empty data.
   * It must specify that like the followed sample:
   * <pre>
   *protected void specifyEmptyDefaultData()
   *{ data[idxBegin + 0] = 0;
   *  data[idxBegin + kIdxMid] = 0;
   *  data[idxBegin + kIdxMin] = kNotAValue;
   *  data[idxBegin + kIdxMax] = kNotAValue;
   *}
   *</pre>
   */
  abstract protected void specifyEmptyDefaultData();



  /** Specifies the length of the head data. This is the index of the first child relative to the start position of the element.
   * This method is called inside this base class itself inside some methods. Outside the method may be called
   * at example to calculate the size of data.</br>
   * Only the derivated class knows the position of the first child element.
   * It must specify that like the followed sample:
   * <pre>
   *protected void specifyLengthElementHead()
   *{ return kIdxFirstChild;  //kIdxFistChild should be defined as static final int.
   *}
   *</pre>
   *  @return Position of the first child element relative to the start position or -1.
   */
  public abstract int specifyLengthElementHead();


  /** Returns the actual length of the whole element presenting with this class.
   * This method should be specified in the derivated class.
   * The method is called inside this base class in the methods assignAsChild()
   * to calculate the {@link idxEnd}.
   * <br>
   * The calculation of the whole length of the element should be considered
   * all children inside this element. The structure is user-specific. Only some examples
   * may be given here:
   * <ul><li>If all children have a constant length and the number of children is known,
   *   the calculation of the length is simple: lengthOfHead + nrofChilds * lengthOfChilds.</li>
   * <li>If the children have a variably length, all children should be checked
   *   about its length.<li>
   * </ul>
   * If the actual element are TODO better explain when and why.
   *
   * @return The length of this existing element or -1, if the length is not fix yet.
   * @throws XmlBinCodeElementException If the data inside are corrupted, the user can throw this exception.
   */
  protected abstract int specifyLengthElement()
  throws IllegalArgumentException;



  /** Returns the length of a child element at current position specified in  the derivated class.
   * The derivated class must known its possible child elements and must get there length
   * with enquiry of specifyLengthElement() of the current child types.
   * This method is called inside the getLengthCurrentChildElement()-method
   * esspecially called inside the next()-method to increment the index idxChild.
   * This method is not called if setLengthCurrentChildElement() was called after calling next().
   * The user may precluding the call of this method if he calls setLengthCurrentChildElement in sequence
   * of the followed sample:
   * <pre>
   * int eChildCode = dataCode.next();
   * switch(eChildCode)
   * { case ...:
   *   { child.assignAsChild(dataCode);             //the child is known yet!
   *     dataCode.setLengthCurrentChildElement(child.getLength());
   *     ....
   *   }
   * <pre>
   * If the user always calls setLengthCurrentChildElement in this manner,
   * he don't need to overwrite specifyLengthCurrentChildElement.
   * @return The length of the existing child element at the current position idxChild.
   */
  protected int specifyLengthCurrentChildElement()
  throws IllegalArgumentException
  { throw new IllegalArgumentException("The length of the child is undefined, no user specification is known");
  }


  /** Constructs a new empty instance. Use assign() to work with it. */
  public ByteDataAccess()
  { this.data = null;
    this.bBigEndian = false;
    bExpand = false;
    idxBegin = 0;
    idxEnd = 0;
    idxCurrentChild = -1;  //to mark start.
    idxCurrentChildEnd = 0;
    parent = null;
    currentChild = null;
    //charset = Charset.forName("ISO-8859-1");  //NOTE: String(..., Charset) is only support from Java 6
    charset = "ISO-8859-1";
  }

  
  
  /**Assigns new data to this element. <br>

   * The user may overwrite this method and call super.assignData(data, length) inside
   * if some additional actions should be done.<br/>
   * This method is also usefull to deassign a current data buffer, call <code>assign(null, 0);</code>.
   * <br>
   * If the element are using before, its connection to an other parent is dissolved.
   * <br>
   * @param data The data. The length of data may be greater as
   *             the number of the significant bytes.
   * @param length Number of significant bytes in data.
   *               If length is > data.length, an exception may be thrown
   *               in any state of the evaluation.
   * @throws IllegalArgumentException if the length is > data.length
   */
  public void assignData(byte[] data, int length) 
  throws IllegalArgumentException
  { if(length == 0)
    { length = -1;  //because elsewhere an exception is thrown. At least the head length are present.
    }
    assignData(data, length, 0);  
  }
  
  
  
  /**Assigns new data to this element at given index in data. <br>
   * The user may overwrite this method and call super.assignData(data, length) inside
   * if some additional actions should be done.<br/>
   * This method is also usefull to deassign a current data buffer, call <code>assign(null, 0);</code>.
   * <br>
   * If the element are using before, its connection to an other parent is dissolved.
   * <br>
   * @param data The data. The length of data may be greater as
   *             the number of the significant bytes.
   * @param lengthData absolute Number of significant bytes in data from idx=0.
   *               If length is > data.length, an exception is thrown.
   *               If the length is <0 (especially -1), it means, it is not known outside.
   *               Than the element is initialized with its known head length.
   *               The length mustn't not ==0, it is tested. Use -1 also if the head length is 0.
   * @param index Start position in data 
   * @throws IllegalArgumentException 
   */

  public void assignData(byte[] data, int lengthData, int index) 
  throws IllegalArgumentException
  { this.data = data;
    if(index < 0)
    { throw new RuntimeException("idx have to be >=0");
    }
    if(lengthData == 0)
    { throw new RuntimeException("length ==0 is not accepted, it may be a argument mistake.");
    }
    bExpand = (lengthData <= 0);
    idxBegin = index; 
    idxCurrentChild = -1;
    idxFirstChild = idxCurrentChildEnd = index + specifyLengthElementHead(); //-1;         //no length of element is known, it means, no child is appended yet.
    idxEnd = bExpand ? idxFirstChild : lengthData;
    if(idxEnd > data.length)
    { throw new IllegalArgumentException("not enough data bytes, requested=" + idxEnd + ", buffer-length=" + data.length);
    }
    if(parent!= null && parent.currentChild == this)
    { parent.currentChild = null;  //the child is invalid because it is new assigned.
    }
    parent = null;
    currentChild = null;
    assignDataToFixChilds();
  }
  
  
  

  

  /**Initializes a top level, the data are considered as non initalized.
   * The length of the head should be a constant value, given from
   * method call {@link specifyLengthElementHead()}. The child Positions
   * are set to the end of head, no childs are presumed.
   * The head should be filled with data after that calling some methods like
   * {@link setInt32(int, int)}.<br>
   * The children should be added by calling {@link addChild(ByteDataAccess)}
   * and filled with data after that.
   *
   * <br>
   * If the element are using before, its connection to an other parent is dissolved.
   * <br>
   * Example to shown the principle:<pre>
   *   ...........the data undefined with defined length.........
   *   +++++                   Head, the length should be known.
   *        ####****#####****  Space for children,
   * </pre>
   * @param data The data. The reference should be initialized, it means
   *        the data have a defined maximum of length. But it is not tested here.
   * @throws IllegalArgumentException 
   */
  final public void assignEmpty(byte[] data) 
  { try{ assignData(data, -1, 0);} catch(IllegalArgumentException e){}

    specifyEmptyDefaultData();
  }

  
  /**Remove all children. Let the head unchanged.
   * @since 2010-11-16
   */
  public final void removeChildren()
  { 
  	idxCurrentChildEnd = idxFirstChild;
  	if(bExpand){
  		idxEnd = idxFirstChild;
  	}
  	idxCurrentChild = -1;
  	if(currentChild !=null){
  		currentChild.detach();
  		currentChild = null;
  	}
  }
  
  
  /**Remove all connections. Especially for children. */
  final public void detach()
  { if(currentChild !=null){ currentChild.detach(); }
  	data = null;
    parent = null;
    idxBegin = idxEnd = 0;
    idxFirstChild = idxCurrentChild = idxCurrentChildEnd = 0;
    bExpand = false;
  }
  
  
  /**Assigns this element to the same position in data, but it is another view. 
   * This method should be called inside a assignCasted() method if a inner head structure is known
   * and the conclusion of this structure is possible. At result, both ByteDataAccess instances reference the same data,
   * in different views.
   * @param src The known data access
   * @param offsetCastToInput typical 0 if single inherition is used.
   * @throws IllegalArgumentException if a length of the new type is specified but the byte[]-data are shorter. 
   *                         The length of byte[] is tested. 
   */
  final protected void assignCasted_i(ByteDataAccess src, int offsetCastToInput, int lengthDst)
  throws IllegalArgumentException
  { this.bBigEndian = src.bBigEndian;
    bExpand = src.bExpand;
    //assignData(src.data, src.idxBegin + lengthDst + offsetCastToInput, src.idxBegin + offsetCastToInput);
    assignData(src.data, src.idxEnd, src.idxBegin + offsetCastToInput);
    //lengthDst is unsused, not necessary because lengthElementHead is knwon!
  }


  
  
  
  
  /**Older form, see protected method {@link assignCasted_i(ByteDataAccess, int )}
   * If a cast is possible, it should be programmed in the derivated class.
   * @ deprecated because it is not necessary it's a downcast, it may be also upcast or sidecast. 
   * @param input
   * @throws IllegalArgumentException
   */
  final protected void assignDowncast_i(ByteDataAccess input)
  throws IllegalArgumentException
  { assignCasted_i(input, 0, -1);
  }
  
  
  

  /**assigns the element to the current child position of parent,
   * to represent the current child of the parent.
   * This method should be used by reading out byte[] data 
   * if it is detected, that the content of data matches of this derivated type of
   * ByteDataAccess. {@link next()} should be called before. The pattern of using is:
   * <pre>
   * 
    int nWhat;    //code of the element
    while( (nWhat = dataElement.next()) != ByteDataAccess.kEndOfElements )
    { switch(nWhat)  //test the first byte of the current element
      { case ByteDataAccess.kText:
        { sText = dataElement.getText();
        } break;
        case code1:
        { code1Element.assignAsChild(dataElement);
          evaluateValue(code1Element);
        } break:
        case code2:
        { code1Element.assignAsChild(dataElement);
          evaluateValue(code1Element);
        } break:
        default: throw new IllegalArgumentException("unknown Element", dataElement);
      }
    }
    </pre>
   * The difference to {@link addChild(ByteDataAccess)} is: addChild() is used 
   * to writeout to data, addChild() appends the child always after idxEnd,
   * but this method is used to read from data and appends the child at position of the current child.
   * <br>
   * The data reference is copied, the idxBegin of this element
   * is set to the idxChild of parent, it is the current child position.
   * All other indices are setted calling {@link specifyLengthElementHead()}: idxChild
   * and {@link specifyLengthElement()}: idxEnd.
   * The idxChildEnd of parent is setted, so calling next() after this operation
   * increments in data after this new child.
   * <br>
   * If the element are using before, its connection to an other parent is dissolved.
   * @param parent The parent. It should reference data, and a current child position
   *        should be set by calling next() before. See sample at {@link next()}.
   * @throws IllegalArgumentException If the data are wrong. The exception is thrown
   *        orginal from {@link specifyLengthElement()}.
   * @deprecated use addChild()
   */
  final public void assignAsChild(ByteDataAccess parent)
  throws IllegalArgumentException
  { parent.addChild(this);
  }




  
  /**assigns the element to the given position of the parents data to present a child of the parent
   * with a defined length.
   * The difference to {@link addChild(ByteDataAccess)} is: The position is given here
   * directly, it should not be the current child but a free child.  
   * <br>
   * The data reference is copied, the idxBegin of this element
   * is set to the idxChild given as parameter.
   * All other indices are set calling {@link specifyLengthElementHead()}: idxChild
   * and {@link specifyLengthElement()}: idxEnd.
   * <br>
   * If the element are using before, its connection to an other parent is dissolved.
   * @param parent The parent. It should reference data.
   * @param lengthChild Number of the bytes of the free child.
   * @param idxChildInParent The index of the free child in the data.
   * @throws IllegalArgumentException If the indices are wrong in respect to the data.
   */
  final public void assignAtIndex(int idxChildInParent, int lengthChild, ByteDataAccess parent)
  throws IllegalArgumentException
  { this.bBigEndian = parent.bBigEndian;
    this.bExpand = parent.bExpand;
    assignData(parent.data, parent.idxBegin + idxChildInParent + lengthChild, parent.idxBegin + idxChildInParent);
    setBigEndian(parent.bBigEndian);
  }


  /**assigns the element to the given position of the parents data to present a child of the parent.
   * The length of the child is limited to TODO the length of head - or not limited.
   * @param parent The parent. It should reference data.
   * @param idxChildInParent The index of the free child in the data.
   * @throws IllegalArgumentException If the indices are wrong in respect to the data.
   */
  final public void assignAtIndex(int idxChildInParent, ByteDataAccess parent)
  throws IllegalArgumentException
  { @SuppressWarnings("unused")
    int lengthHead = getLengthHead();
    assignData(parent.data, parent.idxEnd, parent.idxBegin + idxChildInParent);
    setBigEndian(parent.bBigEndian);
  }


  
  /**This method is called inside all assign...() methods. 
   * It should be overridden by a users derivation if any fix childs are present. 
   * In the overridden routines the method {@link assignDataAtIndex(byte[], int)} should be called for the fix childs.
   * example:<pre>
   * @Override protected void assignDataToFixChilds()
   * { myFixChild.assignDataAtIndex(super.data, super.idxBegin +kIdxmyFixChild);
   * } 
   * <pre>
   * Note that the indices are relative to idxBegin. use <code>super.</code> because an own element data or idxBegin may be present.
   * <br>
   * The originally implementation is empty.
   */
  protected void assignDataToFixChilds() throws IllegalArgumentException
  {
  }


  /**adds an child Element after the current child or as first child after head.
   * With the aid of the child Element the data can be read or write structured. 
   *<br>
   * Some children can be added after a parent like following sample:
   * <pre>
   * ByteAccessDerivation child = new ByteAccessDerivation();  //empty and unassigned.
   * parent.addChild(child);        //The byte[] data of parent are assigned, index after current child index of parent.
   * child.addChild(grandchild);    //By adding a child to this child, also the parent's index is corrected.
   * </pre>
   *
   * @param child The child will be assigned with the data of this at index after the current child's end-index.
   * @throws IllegalArgumentException if the length of the old current child is not determined yet.
   *         Either the method specifyLengthElement() should be overwritten or the method 
   *         {@link setLengthElement(int)} for the child or {@link setLengthCurrentChildElement(int)}
   *         should be called to prevent this exception.  
   * @throws IllegalArgumentException if the length of the head of the new current child is to far for the data.
   *         It means, child.idxEnd > data.length. 
   */
  final public boolean addChild(ByteDataAccess child) 
  throws IllegalArgumentException
  { notifyAddChild();
    child.bBigEndian = bBigEndian;
    child.bExpand = bExpand;
    setIdxtoNextCurrentChild();
    /**@java2c=dynamic-call.  */
    ByteDataAccess childMtb = child;
    childMtb.assignData(data, bExpand ? -1 : idxEnd, idxCurrentChild);
    childMtb.setBigEndian(bBigEndian);
    child.parent = this;
    this.currentChild = child;
    correctIdxChildEnd(child.idxCurrentChildEnd);
    return bExpand;
  }

  
  /**remove the current child to assign another current child instead of the first one.
   * This method is usefull if data are tested with several structures.
   * It mustn't be called in expand mode. In expand mode you have to be consider about your children.
   * 
   * @param child
   * @throws IllegalArgumentException
   */
  final public void removeChild() 
  throws IllegalArgumentException
  { if(bExpand) throw new RuntimeException("don't call it in expand mode");
    //revert the current child.
    idxCurrentChildEnd = idxCurrentChild;
    idxCurrentChild = -1;
  }
  
  
  
  /**sets the idxCurrentChild to the known idxCurrentChildEnd.
   * This method is called while addChild. The state before is:
   * <ul><li>idxCurrentChild is the index of the up to now current Child, or -1 if no child was added before.
   * <li>idxCurrentChildEnd is the actual end index of the current Child, 
   *     or the index of the first child (after head, may be also 0 if the head has 0 bytes), 
   *     if no child was added before.
   * <ul>
   * The state after is:    
   * <ul><li>idxCurrentChild is set to the idxCurrentChildEnd from state before. 
   * <li>idxCurrentChildEnd is set to -1, because it is not defined yet.
   * <ul>
   * If idxCurrentChildEnd >= idxCurrentChild, it means that this operation respectively {@link next()}
   * was called before. Than this operation is done already, a second call does nothing.
   * <br>
   * The length of the current child should be set after this operation and before this operation respectively the calling operation addChild() 
   * will be called a second one.
   * This is done in the calling routines. 
   */
  private final void setIdxtoNextCurrentChild() 
  //throws IllegalArgumentException
  {
    if(idxCurrentChildEnd >= idxCurrentChild )
    { //This is the standard case.
      //NOTE: idxCurrentChild = -1 is assert if no child is added before.
      idxCurrentChild = idxCurrentChildEnd;
    }
    else if(idxCurrentChildEnd == -2)
    { //next() was called before:
      //do nothing, because next() was performed before.
    }
    else
    { throw new RuntimeException("unexpected idxCurrentChildEnd"); //its a programming error.
    }
    idxCurrentChildEnd = -1;  //the child content is not checked, this index will be set if setLengthCurrentChildElement() is called.
  }
  
  
  
   /**sets the idxCurrentChildEnd and idxEnd. There are two modi:
   * <ul><li>Expand data: the idxEnd == idxCurrenChild. In this case the idxEnd will be expanded.
   *     <li>Use existing data: idxEnd > idxCurrentChild: 
   *         In this case the idxEnd should be >= idxCurrentChild + nrofBytes.
   * </ul>        
   * @param nrofBytes of the child
   * @return true if the data are expanded.
   * @throws IllegalArgumentException if there are not enough data. 
   *         In expanded mode the data.length are to less.
   *         In using existing data: idxEnd are to less. 
   */
  protected final boolean setIdxCurrentChildEnd(int nrofBytes) 
  throws IllegalArgumentException
  { if(bExpand)
    { if(data.length < idxCurrentChild + nrofBytes)
      { throw new IllegalArgumentException("data length to small:"+ (idxCurrentChild + nrofBytes));
      }
    }
    else
    { if(idxEnd < idxCurrentChildEnd)
      { //not expand, but the nrof data are to few
        throw new IllegalArgumentException("to few user data:"+ (idxCurrentChild + nrofBytes));
      }
    }
    correctIdxChildEnd(idxCurrentChild + nrofBytes);  //also of all parents
    return bExpand;
  }
  
  
 
  /**Adds a child Element at current end of data to write data. 
   * The child's data are initialized with call of <code>child.specifyEmptyDefaultData().</code>
   *
   * @param child The child will associated to this and should be used
   *              to add some content.
   * @throws IllegalArgumentException 
   */
  final public void addChildEmpty(ByteDataAccess child) 
  throws IllegalArgumentException
  { addChild(child);
    child.specifyEmptyDefaultData();
  }
  
  

  /** Notifies, that a child is added. This method may be overload,
   * if the user must take somme actions, count the number of childs or others.
   */
  protected void notifyAddChild()
  { //in default, do nothing with this.
  }


  /** Increments the idxEnd and the idxCurrentChildEnd if a new child is added. Called only
   * inside method addChild(child) and recursively to correct
   * in all parents.
   */
  final protected void correctCurrentChildEnd(int idxEndNew)
  { if(idxEnd < idxEndNew) 
    { idxEnd = idxEndNew;
    }
    if(idxCurrentChildEnd < idxEndNew) 
    { idxCurrentChildEnd = idxEndNew;
    }
    if(parent != null)
    { parent.correctCurrentChildEnd(idxEndNew);
    }
  }

  /** Increments the idxEnd if a new child is added. Called only
   * inside method addChild(child) and recursively to correct
   * in all parents.
   */
  final private void correctIdxChildEnd(int idxCurrentChildEndNew)
  { if(bExpand) 
    { //do it only in expand mode
      idxEnd = idxCurrentChildEndNew;
    }
    idxCurrentChildEnd = idxCurrentChildEndNew;
    if(parent != null)
    { parent.correctIdxChildEnd(idxCurrentChildEndNew);
    }
  }

  /** Expands the end index of the parent, it means the management
   * of the expanse of the data.
   *
   */
  final public void expandParent()
  throws IllegalArgumentException
  { if(idxBegin == 0 && parent == null)
    { //it is the top level element, do nothing
    }
    else if(parent != null)
    { if(parent.idxEnd < idxEnd)
      { parent.idxEnd = idxEnd;
      }
      parent.expandParent();
    }
    else throw new IllegalArgumentException("invalid expandParent()");
  }



  /**Adds a child for 1 integer value without a child instance, but returns the value as integer.
   * 
   * @param nrofBytes of the integer
   * @return value in long format, cast it to (int) if you read only 4 bytes etc.
   * @throws IllegalArgumentException if not data has not enaught bytes.
   */
  public final long getChildInteger(int nrofBytes) 
  throws IllegalArgumentException
  { //NOTE: there is no instance for this child, but it is the current child anyway.
    setIdxtoNextCurrentChild();
    if(!setIdxCurrentChildEnd(nrofBytes))
    { //NOTE: to read from idxInChild = 0, build the difference as shown:
      long value = _getLong(idxCurrentChild - idxBegin, nrofBytes);  
      return value;
    }
    else throw new RuntimeException("Not available in expand mode.");
  }
  
  

  
  
  /**Adds a child for 1 float value without a child instance, but returns the value as integer.
   * 
   * @return value in float format
   * @throws IllegalArgumentException if not data has not enaught bytes.
   */
  public final float getChildFloat() 
  throws IllegalArgumentException
  { //NOTE: there is no instance for this child, but it is the current child anyway.
    setIdxtoNextCurrentChild();
    if(!setIdxCurrentChildEnd(4))
    { //NOTE: to read from idxInChild = 0, build the difference as shown:
      int intRepresentation = (int)_getLong(idxCurrentChild - idxBegin, 4);  
      return Float.intBitsToFloat(intRepresentation);
     }
    else throw new RuntimeException("Not available in expand mode.");
  }
  
  

  
  
  /**Adds a child for 1 double value without a child instance, but returns the value as integer.
   * 
   * @return value in float format
   * @throws IllegalArgumentException if not data has not enaught bytes.
   */
  public final double getChildDouble() 
  throws IllegalArgumentException
  { //NOTE: there is no instance for this child, but it is the current child anyway.
    setIdxtoNextCurrentChild();
    if(!setIdxCurrentChildEnd(8))
    { //NOTE: to read from idxInChild = 0, build the difference as shown:
      long intRepresentation = _getLong(idxCurrentChild - idxBegin, 8);  
      return Double.longBitsToDouble(intRepresentation);
     }
    else throw new RuntimeException("Not available in expand mode.");
  }
  
  

  
  
  /**Adds a child for 1 integer value without a child instance, and sets the value as integer.
   * 
   * @param nrofBytes of the integer
   * @return value in long format, cast it to (int) if you read only 4 bytes etc.
   * @throws IllegalArgumentException
   */
  public final void addChildInteger(int nrofBytes, long value) 
  throws IllegalArgumentException
  { setIdxtoNextCurrentChild();
    if(data.length < idxCurrentChild + nrofBytes)
      throw new IllegalArgumentException("data length to small:"+ (idxCurrentChild + nrofBytes));
    //NOTE: there is no instance for this child, but it is the current child anyway.
    //NOTE: to read from idxInChild = 0, build the difference as shown:
    _setLong(idxCurrentChild - idxBegin, nrofBytes, value);  
    correctIdxChildEnd(idxCurrentChild + nrofBytes);
  }
  
  
  /**Adds a child for 1 integer value without a child instance, and sets the value as integer.
   * 
   * @param nrofBytes of the integer
   * @return value in long format, cast it to (int) if you read only 4 bytes etc.
   * @throws IllegalArgumentException
   */
  public final void addChildFloat(float value) 
  throws IllegalArgumentException
  { setIdxtoNextCurrentChild();
    if(data.length < idxCurrentChild + 4)
      throw new IllegalArgumentException("data length to small:"+ (idxCurrentChild + 4));
    //NOTE: there is no instance for this child, but it is the current child anyway.
    //NOTE: to read from idxInChild = 0, build the difference as shown:
    setFloat(idxCurrentChild - idxBegin, value);  
    correctIdxChildEnd(idxCurrentChild + 4);
  }
  
  
  /**Adds a child for a String value without a child instance, but returns the value as String.
   * 
   * @param nrofBytes of the integer
   * @return value in long format, cast it to (int) if you read only 4 bytes etc.
   * @throws IllegalArgumentException if not data has not enaught bytes.
   * @throws UnsupportedEncodingException 
   */
  public final String getChildString(int nrofBytes) 
  throws IllegalArgumentException, UnsupportedEncodingException
  { //NOTE: there is no instance for this child, but it is the current child anyway.
    setIdxtoNextCurrentChild();
    if(!setIdxCurrentChildEnd(nrofBytes))
    { //NOTE: to read from idxInChild = 0, build the difference as shown:
      return _getString(idxCurrentChild - idxBegin, nrofBytes);  
    }
    else throw new RuntimeException("Not available in expand mode.  ");
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
  public final void addChildString(String value, String sEncoding, boolean preventCtrlChars) 
  throws IllegalArgumentException, UnsupportedEncodingException
  { setIdxtoNextCurrentChild();
    int nrofBytes = value.length();
    if(data.length < idxCurrentChild + nrofBytes)
      throw new IllegalArgumentException("data length to small:"+ (idxCurrentChild + nrofBytes));
    //NOTE: there is no instance for this child, but it is the current child anyway.
    //NOTE: to read from idxInChild = 0, build the difference as shown:
    _setString(idxCurrentChild - idxBegin, nrofBytes, value, sEncoding, preventCtrlChars);  
    correctIdxChildEnd(idxCurrentChild + nrofBytes);
  }
  
  
  
  /**Adds a child with String value.
   * 
   * @param value String to add
   * @throws IllegalArgumentException
   */
  public final void addChildString(String value) throws IllegalArgumentException
  { try{ addChildString(value, null); } 
    catch(UnsupportedEncodingException exc){ throw new RuntimeException(exc);} //it isn't able.
  }


  /**Adds a child with String value.
   * 
   * @param value String to add, @pjava2c=nonPersistent.
   * @throws IllegalArgumentException
   * @throws UnsupportedEncodingException 
   */
  public final void addChildString(CharSequence value, String sEncoding) 
  throws IllegalArgumentException, UnsupportedEncodingException
  { setIdxtoNextCurrentChild();
    int nrofBytes = value.length();
    if(data.length < idxCurrentChild + nrofBytes)
      throw new IllegalArgumentException("data length to small:"+ (idxCurrentChild + nrofBytes));
    //NOTE: there is no instance for this child, but it is the current child anyway.
    //NOTE: to read from idxInChild = 0, build the difference as shown:
    for(int ii=0; ii<nrofBytes; ++ii){
    	byte charByte = (byte)(value.charAt(ii));  //TODO encoding
    	data[idxCurrentChild+ii] = charByte;
    }
    correctIdxChildEnd(idxCurrentChild + nrofBytes);
  }
  
  
  
  /**Adds a child with String value.
   * 
   * @param value String to add, @pjava2c=nonPersistent.
   * @throws IllegalArgumentException
   */
  public final void addChildString(CharSequence value) throws IllegalArgumentException
  { try{ addChildString(value, null); } 
    catch(UnsupportedEncodingException exc){ throw new RuntimeException(exc);} //it isn't able.
  }


  /** Writes a String into data with given color.
   * The user can overwrite this method and call super.addText(ss) inside
   * if some additional actions should be done.
   * @param ss The String to write
   * @param color The color
   * @deprecated
   */
  public final int addText(String ss)
  throws IllegalArgumentException
  { if(parent != null || idxBegin == 0)
    { /**@java2c=ByteStringJc.  */
  		byte[] chars = ss.getBytes();  //the ASCII represantation;
      int srcLen = chars.length;
      int dataLen = data.length;
      for(int ii=0; ii<srcLen; ii++)
      { byte cc = chars[ii];
        if(cc < 0x20) cc = 0x3f;  //'?' in ASCII
        if(idxEnd >= dataLen) throw new IllegalArgumentException("element to long");
        data[idxEnd++] = cc;
      }
      expandParent();
      return(idxEnd-idxBegin);  //current nr of bytes
    }
    else throw new IllegalArgumentException("inadmissable add-operation");
  }




  /** starts the calling loop of next().
   * The calling of next() after them supplies the first child element.
   *
   */
  public final void rewind()
  { idxCurrentChild = -1;
  }

  /** Sets the data index to the position after the current child element and returns its code.<br>
   * If more than 1 byte determines the code, the user should call getInt32(idxChild)
   * to get the code.
   * Usage:<pre>
   * while( (eElement = code.next()) != kEndOfElements)
   * { switch(eElement) { ... }
   * } </pre>
   * @return The first byte, that may be the code of the child element.<br>
   *         If this code is between 0x20 to 0xbf, it is assumed that is an
   *         character, kText is returned than.<br>
   *         If no more childs are available, kEndOfElements is returned.<br>
  */
  final public int next()
  throws IllegalArgumentException
  { byte eWhat;
    setIdxtoNextCurrentChild();
    idxCurrentChildEnd = -2;  //the child content is not known, this index will be set if setLengthCurrentChildElement() is called.
    if( idxCurrentChild >= idxEnd)
    { eWhat = kEndOfElements;  //no more data
    }
    else
    { eWhat = data[idxCurrentChild];
      if(isTextByte(eWhat)){ eWhat = kText; }
    }
    return eWhat;
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
  
  
  /**returns the number number of bytes there are max available from position of a next current child. 
   * ,
   * the number of bytes to the end of buffer is returned.
   * 
   * @return nrofBytes that should fitting in the given data range from current child position 
   *                  to the end of data determines by calling assingData(...)
   *                  or by calling addChild() with a known size of child or setLengthElement() .
   * @throws IllegalArgumentException if the length of the current child is not determined yet.
   *         Either the method specifyLengthElement() should be overwritten or the method 
   *         {@link setLengthElement(int)} for the child or {@link setLengthCurrentChildElement(int)}
   *         should be called to prevent this exception.  
   */ 
  final public int getMaxNrofBytesForNextChild() throws IllegalArgumentException
  { //if(idxCurrentChild == -1)
    { //there is no child added, it is the first:
      //return idxEnd - idxCurrentChild;
    }  
    if(idxCurrentChildEnd < idxCurrentChild)
      throw new IllegalArgumentException("length of current child is undefined."); 
    return idxEnd - idxCurrentChildEnd;
  }
  
  
  /**returns the number number of bytes there are max available from position of the current child. 
   * ,
   * the number of bytes to the end of buffer is returned.
   * 
   * @return nrofBytes that should fitting in the given data range from current child position 
   *                  to the end of data determines by calling assingData(...)
   *                  or by calling addChild() with a known size of child or setLengthElement() .
   */ 
  final public int getMaxNrofBytes()
  { return data.length - idxBegin;
  }
  
  
  

  /** Returns the length of the existing actual element.
   * @return The number of bytes of the actual element in the buffer.
   *         It is (idxEnd - idxBegin).
   */
  final public int getLength()
  { return idxEnd - idxBegin;
  }



  /** Returns the length of the data.
   * @return The number of bytes of data in the buffer.
   *         It is idxEnd.
   */
  final public int getLengthTotal()
  { return idxEnd;
  } 



  /** Returns the data buffer itself. The actual total length is getted with getLengthTotal().
   * @return The number of bytes of the data in the buffer.
   */
  final public byte[] getData()
  { return data;
  }

  /**Returns the position of the Element data in the assigned buffer.
   * 
   * @return index of this element in the data buffer.
   */  
  final public int getPositionInBuffer()
  { return idxBegin;
  }
  


  /**Returns the position of the current child in the assigned buffer.
   * 
   * @return index of the current child of this element in the data buffer.
   */  
  final public int getPositionNextChildInBuffer()
  { return idxCurrentChildEnd;
  }
  



  /** Returns the length of the current child element.
   * The length may be setted outside by calling setLengthCurrentChildElement()
   * from user level after any calling of next()
   * or after calling getText() if it is a text content.
   * In this case this method returns this length in a simple way. <br>
   * If the user don't have called setLengthCurrentChildElement() after the last next(),
   * the users defined specifyLengthCurrentChildElement() is called.
   * @return the length in bytes of the current child element.
   * @throws IllegalArgumentException if the user has not defined a overloaded methode specifyLengthCurrentChildElement()
   *                              or this method has thrown the exception because the length is not determinable.
   */
  final public int getLengthCurrentChildElement()
  throws IllegalArgumentException
  {
    if(idxCurrentChildEnd > idxCurrentChild)
    { //a get method, especially getText() was called,
      //so the end of the child is known yet, use it!
      return idxCurrentChildEnd - idxCurrentChild;
    }
    /*changed: it cannot be assumed that the coding use textbytes!
     * If they are textbytes, the user has called getText normally,
     * therefor the idxChildEnd is setted there.
    else if(isTextByte(data[idxChild]))
    { //text bytes are overreaded:
      idxChildEnd = idxChild;
      do
      { idxChildEnd +=1;
      } while(idxChildEnd < idxEnd && isTextByte(data[idxChildEnd]));
      return idxChildEnd - idxChild;
    }
    */
    else
    { //only the user can define the length.
      return specifyLengthCurrentChildElement();
    }
  }



  /** Sets the length of the current child element after calling next().
   *  The user may set the length due to the knowledge of the type and content of the actual child element.
   *  So the calling of specifyLengthCurrentChildElement() will be precluded.
   *  The idxChildEnd is setted.
   */
  final public void setLengthCurrentChildElement(int lengthOfCurrentChild)
  { if(currentChild != null)
    { currentChild.setLengthElement(lengthOfCurrentChild);
    }
  }


  
  /**Sets the length of the current element, considering all children.
   * If the element is the current child of its parent, the idxChildEnd of parent is set.
   * Therefore a call of next() or addChild uses the idxChildEnd-position for a new child.
   * 
   * @param length The length inclusive all children.
   */
  final public void setLengthElement(int length)
  { //if(!bExpand && )
    correctIdxChildEnd(idxBegin + length);
  }

  
  
  
  
  
  /** Returns true if the current child element represents a TEXT(), direct ASCII chars,
   * false if the element is a complex element.
   * Text chararacters are coded with 0x20..0x7f, the standard ASCII code,
   * and with 0x80 to 0xbf, special characters user defined.
   *
   */
  public final boolean isTextByte(byte nn)
  { return (nn >= 0x20);
  }


  /** Returns the current string or null on end*/
  final public String getText()
  { if(idxCurrentChild >= idxEnd) return null;
    else
    { byte nn = data[idxCurrentChild];
      if( isTextByte(nn))
      { idxCurrentChildEnd = idxCurrentChild;
        char cc = '?';
        StringBuffer ss = new StringBuffer(20);
        while(  idxCurrentChildEnd < idxEnd
             && isTextByte(nn = data[idxCurrentChildEnd])
             )
        { if(nn < 0) cc = '?'; //{ cc=convertHiChars(nn);}  //range between 0x80..0xbf
          else cc = (char)(nn);     //convert 0x20...7F to character
          ss.append(cc);  //konvert to Unicode
          idxCurrentChildEnd +=1;
        }
        return ss.toString();
      }
      else return null;  //no Text
    }
  }

  /**Returns a String from the given position inside the actual element .
   * The bytes are interpreted in the given encoding.
   * 
   * @param idx The start position inside the child.
   * @param nmax Maximal number of bytes
   * @return The String representation of the bytes.
   */
  protected String getString(int idx, int nmax)
  { String sRet;
    try{ sRet = new String(data, idxBegin + idx, nmax, "ISO-8859-1");} 
    catch (UnsupportedEncodingException e)  {sRet = null; }
    int pos0 = sRet.indexOf(0);
    if(pos0 >0 )
    { //The data are zero terminated!
      sRet = sRet.substring(0, pos0);
    }
    return sRet;
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
  protected int setString(int idx, int nmax, String ss)
  { if(ss.length()>nmax){ ss = ss.substring(0, nmax); } //truncate.
    /**Use a @java2c=ByteStringJc. In C there may not be a difference between the String
     * and the string of byte[].*/
  	byte[] byteRepresentation;
    try { byteRepresentation = ss.getBytes("ISO-8859-1");} 
    catch (UnsupportedEncodingException e){ byteRepresentation = null; }
    int len = byteRepresentation.length;
    if(len > nmax){ len = nmax; } //truncate.
    System.arraycopy(byteRepresentation, 0, data, idxBegin + idx, len);
    return len;
  }
  
  

  /**Sets the big or little endian mode. 
   * This method is override-able, because a derived class
   * may set the endian of embedded children too.
   *
   * @param val true if big endian, hi byte at lower adress, false if little endian.
   */
  public void setBigEndian(boolean val)
  { bBigEndian = val;
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
  protected final long _getLong(final int idxInChild, final int nrofBytesAndSign)
  { long val = 0;
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
    { idx = idxBegin + idxInChild;
      idxStep = 1;
    }
    else
    { idx = idxBegin + idxInChild + nrofBytes -1;
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
  protected final void _setLong(int idx, int nrofBytes, long val)
  { int idxStep;
    if(bBigEndian)
    { idx = idxBegin + idx + nrofBytes -1;
      idxStep = -1;
    }
    else
    { idx = idxBegin + idx;
      idxStep = 1;
    }
    do
    { data[idx] = (byte)(val);
      if(--nrofBytes <= 0) break;
      val >>=8;
      idx += idxStep;
    }while(true);  //see break;
  }

  
  
  
  /**sets the content inside the acutal element with the character bytes from the given String.
   * No value < 0x20 is setted. If the String value contain a control character with code < 0x20,
   * a '?' is written. This behavior protected, that bytevalues < 0x20 can use to detect no String elements,
   * see {@link getByteNextChild()}.  
   * This method is protected because at user level its using is a prone to errors because the idx is free related.
   * 
   * @param idx the position in the actual element, the data are set to data[idxBegin+idx].
   * @param nrofBytes The length of the byte[] area to set. 
   *        If the String value is longer as nrofBytes, it will be truncated. No exception occurs.
   *        If the String is shorter as nrofBytes, the rest is filled with 0.
   * @param value The String value.
   * @return The String which is stored at the designated area. 
   *         @pjava2c=nonPersistent. It references the String at the source area only. 
   * @throws UnsupportedEncodingException
   */
  protected final String _getString(final int idx, final int nrofBytes) 
  throws UnsupportedEncodingException
  {
    int idxData = idx + idxBegin;
    //int idxEnd = idxData + nrofBytes;
    String value = new String(data, idxData, nrofBytes, charset);
    int end = value.indexOf(0);
    if(end >=0){ value = value.substring(0, end); }
    return value;
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
  {
    int idxData = idx + idxBegin;
    int idxEnd = idxData + nrofBytes;
    /**@java2c=ByteStringJc. */
    byte[] chars;
    if(sEncoding == null){ sEncoding = "ISO-8859-1"; }
    chars = value.getBytes(sEncoding); 
    int srcLen = chars.length;
    for(int ii=0; ii<srcLen && ii < nrofBytes; ii++)
    { byte cc = chars[ii];
      if(preventCtrlChars && cc < 0x20){ cc = 0x3f; } //'?' in ASCII
      data[idxData++] = cc;
    }
    //fill up the rest of the string with 0-chars. 
    while(idxData < idxEnd)
    { data[idxData++] = 0;
    }
  }
  
  
  
  /**copies some data to a int[], primarily to debug a content. 
   * @param dst This array is field, but only from data of the current element between idxBegin and idxEnd
   */
  public final void copyData(int[] dst)
  { int idxMax = idxEnd - idxBegin;
    if(idxMax/4 > dst.length) idxMax = 4* dst.length;
    int iDst = 0;
    for(int idx = 0; idx < idxMax; idx+=4)
    { dst[iDst++] = (int)_getLong(idx,4);
    }
  }
  
  
  
  
  /** Gets a float value from the content of 4 byte. The float value is red
   * according to the IEEE 754 floating-point "single format" bit layout, preserving Not-a-Number (NaN) values,
   * like converted from java.lang.Float.intBitsToFloat().
   */
  protected final float getFloat(int idx)
  {
    int intRepresentation = getInt32(idx);
    float value = Float.intBitsToFloat(intRepresentation);
    return value;
  }
  
  protected final double getDouble(int idx)
  {
    long intRepresentation = _getLong(idx,8);
    return Double.longBitsToDouble(intRepresentation);
   
  }
  
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
  public final int getInt32(int idx)
  { int val;
    if(bBigEndian)
    { val =  ((  (int)data[idxBegin + idx])<<24)  //NOTE all 24 low-bits are 0
          |  (( ((int)data[idxBegin + idx+1])<<16) & 0x00ff0000 ) //NOTE the high bits may be 0 or 1
          |  (( ((int)data[idxBegin + idx+2])<< 8) & 0x0000ff00 ) //depending on sign of byte. Mask it!
          |  (( ((int)data[idxBegin + idx+3])    ) & 0x000000ff );  //NOTE: the value has only 8 bits for bitwise or.
    }
    else
    { val =  (  ((int)data[idxBegin + idx+3])<<24)  //NOTE all 24 low-bits are 0
          |  (( ((int)data[idxBegin + idx+2])<<16) & 0x00ff0000 ) //NOTE the high bits may be 0 or 1
          |  (( ((int)data[idxBegin + idx+1])<< 8) & 0x0000ff00 ) //depending on sign of byte. Mask it!
          |  (( ((int)data[idxBegin + idx  ])    ) & 0x000000ff );  //NOTE: the value has only 8 bits for bitwise or.
    }
    return val;
  }

  
  public final int getUint32(int idx)
  { return getInt32(idx);
  }
  
  /** Returns the content of 2 bytes as a positive nr between 0..65535, big-endian
   * inside the actual element.
   *
   * @param idx the position of leading byte in the actual element, the data are raken from data[idxBegin+idx].
   *            This is not the absolute position in data, idxBegin is added.<br/>
   * @return the integer value in range from -32768 to 32767
   * */
  protected final int getUint16(int idx)
  { int val;
    if(bBigEndian)
    { val =  (( ((int)data[idxBegin + idx  ])<< 8) & 0x0000ff00 ) //depending on sign of byte. Mask it!
          |  (( ((int)data[idxBegin + idx+1])    ) & 0x000000ff );  //NOTE: the value has only 8 bits for bitwise or.
    }
    else
    { val =  (( ((int)data[idxBegin + idx+1])<< 8) & 0x0000ff00 ) //depending on sign of byte. Mask it!
          |  (( ((int)data[idxBegin + idx  ])    ) & 0x000000ff );  //NOTE: the value has only 8 bits for bitwise or.
    }
    return val;
  }

  /** Returns the content of 2 bytes as a positive nr between 0..65535 inside the actual element.
   *
   * @param idx the position of leading byte in the actual element, the data are raken from data[idxBegin+idx].
   *            This is not the absolute position in data, idxBegin is added.<br/>
   * @return the integer value in range from -32768 to 32767
   * */
  protected final short getInt16(int idx)
  { int val;
    if(bBigEndian)
    { val =  (( ((short)data[idxBegin + idx  ])<< 8) & 0xff00 ) //depending on sign of byte. Mask it!
          |  (( ((short)data[idxBegin + idx+1])    ) & 0x00ff );  //NOTE: the value has only 8 bits for bitwise or.
    }
    else
    { val =  (( ((short)data[idxBegin + idx+1])<< 8) & 0xff00 ) //depending on sign of byte. Mask it!
          |  (( ((short)data[idxBegin + idx  ])    ) & 0x00ff );  //NOTE: the value has only 8 bits for bitwise or.
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
  protected final char getChar(int idx)
  { char val;
    val = (char) data[idxBegin + idx];
    return val;
  }
  
  /** Returns the content of 1 bytes as a positive or negative nr between -128..127
   * inside the actual element.
   *
   * @param idx the position of leading byte in the actual element, the data are raken from data[idxBegin+idx].
   *            This is not the absolute position in data, idxBegin is added.<br/>
   * @return the integer value in range from -32768 to 32767
   * */
  protected final byte getInt8(int idx)
  { byte val;
    val = data[idxBegin + idx];
    return val;
  }

  
  /** Returns the content of 1 bytes as a positive or negative nr between -128..127
   * inside the actual element.
   *
   * @param idx the position of leading byte in the actual element, the data are raken from data[idxBegin+idx].
   *            This is not the absolute position in data, idxBegin is added.<br/>
   * @return the integer value in range from -32768 to 32767
   * */
  protected final int getUint8(int idx)
  { int val;
    val = data[idxBegin + idx] & 0xff;
    return val;
  }
 
  
  
  

  
  protected final int getUint32(int idxBytes, int idxArray, int lengthArray)
  { if(idxArray >= lengthArray || idxArray < 0) throw new IndexOutOfBoundsException("getUint16:" + idxArray);
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
  
  protected int getUint16(int idxBytes, int idxArray, int lengthArray)
  { if(idxArray >= lengthArray || idxArray < 0) throw new IndexOutOfBoundsException("getUint16:" + idxArray);
    return getUint16(idxBytes + 2*idxArray);
  }
  
  protected int getUint8(int idxBytes, int idxArray, int lengthArray)
  { if(idxArray >= lengthArray || idxArray < 0) throw new IndexOutOfBoundsException("getUint8:" + idxArray);
    return getInt8(idxBytes + idxArray);
  }
  
  protected float getFloat(int idxBytes, int idxArray, int lengthArray)
  { if(idxArray >= lengthArray || idxArray < 0) throw new IndexOutOfBoundsException("getFloat:" + idxArray);
    return getFloat(idxBytes + 4*idxArray);
  }
  
  
  
  

  /** Set the content of 4 byte from a float variable. The float value is stored
   * according to the IEEE 754 floating-point "single format" bit layout, preserving Not-a-Number (NaN) values,
   * like converted from java.lang.Float.floatToRawIntBits().
   */
  protected final void setFloat(int idx, float value)
  {
    int intRepresentation = Float.floatToRawIntBits(value);
    setInt32(idx, intRepresentation);
  }



  /** Set the content of 8 byte from a double variable. The double value is stored
   * according to the IEEE 754 floating-point "double format" bit layout, preserving Not-a-Number (NaN) values,
   * like converted from java.lang.Double.doubleToRawLongBits().
   */
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
  { if(bBigEndian)
    { data[idxBegin + idx]   = (byte)((value>>24) & 0xff);
      data[idxBegin + idx+1] = (byte)((value>>16) & 0xff);
      data[idxBegin + idx+2] = (byte)((value>>8) & 0xff);
      data[idxBegin + idx+3] = (byte)(value & 0xff);
    }
    else
    { data[idxBegin + idx+3] = (byte)((value>>24) & 0xff);
      data[idxBegin + idx+2] = (byte)((value>>16) & 0xff);
      data[idxBegin + idx+1] = (byte)((value>>8) & 0xff);
      data[idxBegin + idx]   = (byte)(value & 0xff);
    }
  }

  /** Set the content of 1 bytes as a positive nr between 0..255, big- or little-endian.
  *
  * @param idx The position of leading byte in the current elements data.
  *            This is not the absolute position in data, idxBegin is added.<br/>
  * @param value The value in range 0..65535. The value is taken modulo 0xff.
  * */
  protected final void setUint8(int idx, int value)
  { setInt8(idx, value);  //its the same because modulo!
  }

  /** Set the content of 2 bytes as a positive nr between 0..65535, big- or little-endian.
  *
  * @param idx The position of leading byte in the current elements data.
  *            This is not the absolute position in data, idxBegin is added.<br/>
  * @param value The value in range 0..65535. The value is taken modulo 0xffff.
  * */
  protected final void setUint16(int idx, int value)
  { setInt16(idx, value);  //its the same because modulo!
  }

  /** Set the content of 4 bytes as a positive nr between 0..2pow32-1, big- or little-endian.
  *
  * @param idx The position of leading byte in the current elements data.
  *            This is not the absolute position in data, idxBegin is added.<br/>
  * @param value The value as long. The value is taken modulo 0xffffffff.
  * */
  protected final void setUint32(int idx, long value)
  { //the same algorithm in source, but other action on machine level,
    //because value is long!
    if(bBigEndian)
    { data[idxBegin + idx]   = (byte)((value>>24) & 0xff);
      data[idxBegin + idx+1] = (byte)((value>>16) & 0xff);
      data[idxBegin + idx+2] = (byte)((value>>8) & 0xff);
      data[idxBegin + idx+3] = (byte)(value & 0xff);
    }
    else
    { data[idxBegin + idx+3] = (byte)((value>>24) & 0xff);
      data[idxBegin + idx+2] = (byte)((value>>16) & 0xff);
      data[idxBegin + idx+1] = (byte)((value>>8) & 0xff);
      data[idxBegin + idx]   = (byte)(value & 0xff);
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
  { if(bBigEndian)
    { data[idxBegin + idx]   = (byte)((value>>8) & 0xff);
      data[idxBegin + idx+1] = (byte)(value & 0xff);
    }
    else
    { data[idxBegin + idx+1] = (byte)((value>>8) & 0xff);
      data[idxBegin + idx]   = (byte)(value & 0xff);
    }
  }

  /** Set the content of 1 bytes as a positive nr between 0..256.
   *
   * @param idx The position of leading byte in the current elements data.
   *            This is not the absolute position in data, idxBegin is added.<br/>
   * @param value The value in range 0..65535. The value is taken modulo 0xffff.
   * */
  protected final void setInt8(int idx, int value)
  { data[idxBegin + idx] = (byte)(value & 0xff);
  }


  
  protected final void setUint32(int idxBytes, int idxArray, int lengthArray, int val)
  { if(idxArray >= lengthArray || idxArray < 0) throw new IndexOutOfBoundsException("setUint32:" + idxArray);
    setUint32(idxBytes + 4*idxArray, val);
  }
  
  protected final void setInt32(int idxBytes, int idxArray, int lengthArray, int val)
  { if(idxArray >= lengthArray || idxArray < 0) throw new IndexOutOfBoundsException("setInt32:" + idxArray);
    setInt32(idxBytes + 4*idxArray, val);
  }
  
  protected final void setInt16(int idxBytes, int idxArray, int lengthArray, int val)
  { if(idxArray >= lengthArray || idxArray < 0) throw new IndexOutOfBoundsException("getInt16:" + idxArray);
    setInt16(idxBytes + 2*idxArray, val);
  }
  
  protected final void setInt8(int idxBytes, int idxArray, int lengthArray, int val)
  { if(idxArray >= lengthArray || idxArray < 0) throw new IndexOutOfBoundsException("getInt16:" + idxArray);
    setInt8(idxBytes + idxArray, val);
  }
  
  
  protected final void setUint16(int idxBytes, int idxArray, int lengthArray, int val)
  { if(idxArray >= lengthArray || idxArray < 0) throw new IndexOutOfBoundsException("getInt16:" + idxArray);
    setUint16(idxBytes + 2*idxArray, val);
  }
  
  protected final void setUint8(int idxBytes, int idxArray, int lengthArray, int val)
  { if(idxArray >= lengthArray || idxArray < 0) throw new IndexOutOfBoundsException("getInt16:" + idxArray);
    setUint8(idxBytes + idxArray, val);
  }
  
  protected final void setFloat(int idxBytes, int idxArray, int lengthArray, float val)
  { if(idxArray >= lengthArray || idxArray < 0) throw new IndexOutOfBoundsException("getInt16:" + idxArray);
    setFloat(idxBytes + 4 * idxArray, val);
  }
  
  
  
  
  /** Copies the data into a byte[]
   *
   * */
  final public void copyDataFrom(ByteDataAccess src)
  throws IllegalArgumentException
  { int len = src.getLength();
    if(data.length < len) throw new IndexOutOfBoundsException("copy, dst to small" + len);
    System.arraycopy(src.data,src.idxBegin,data,idxBegin,len);
  }

/** Counts the idxChild by given index, idxChild is ByteCount from idxBegin
 * 
 * @param indexObjectArray Index of Array
 */
  public final void elementAt(int indexObjectArray) {
      
      try {
          idxCurrentChild = idxBegin + specifyLengthElementHead() + specifyLengthCurrentChildElement() * indexObjectArray;
      } catch (IllegalArgumentException e) {
          e.printStackTrace();
      }
      
  }



  public final ByteDataAccess getCurrentChild() 
  {
     
      return currentChild;
  }

  
  
}
