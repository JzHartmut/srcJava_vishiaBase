package org.vishia.communication;

import org.vishia.util.Java4C;

/**This is the base class of a callback for {@link InterProcessCommRxThread}.
 * It is used to implement the callback on received data.
 * For C-Usage an anonymous implementation can be build with the macro <code>IFC_IMPL_dataMETHOD1_ObjectJc(TYPE, METHOD)</code>
 * with a simple given C implementation of the {@link #execRxData(byte[], int, Address_InterProcessComm)} method. 
 * 
 * @author Hartmut Schorrig
 *
 */
public abstract class InterProcessCommRx_ifc
{

  /**Version, history and license.
   * <ul>
   * <li>2015-06-13 Hartmut: Created especially for C-usage of InterProcessCommunication.
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
   * <li> But the LPGL is not appropriate for a whole software product,
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
   */
  public static final String version = "2015-06-13";

  
  /**This data pointer can be set by any application. It is offered to the {@link #execRxData(byte[], int)}
   * by this base class. Especially an application in C does not need to extend this class by additional data,
   * instead use #data as reference.
   */
  @Java4C.SimpleRef public Object data;
  
  /**Callback routine for received data.
   * @param buffer
   * @param nrofBytesReceived
   * @param sender The sender of the data.
   */
  public abstract void execRxData(@Java4C.PtrVal byte[] buffer, int nrofBytesReceived, Address_InterProcessComm sender);
  
}
