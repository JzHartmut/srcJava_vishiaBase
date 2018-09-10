/****************************************************************************
 * Copyright/Copyleft:
 *
 * For this source the LGPL Lesser General Public License,
 * published by the Free Software Foundation is valid.
 * It means:
 * 1) You can use this source without any restriction for any desired purpose.
 * 2) You can redistribute copies of this source to everybody.
 * 3) Every user of this source, also the user of redistribute copies
 *    with or without payment, must accept this license for further using.
 * 4) But the LPGL is not appropriate for a whole software product,
 *    if this source is only a part of them. It means, the user
 *    must publish this part of source,
 *    but don't need to publish the whole source of the own product.
 * 5) You can study and modify (improve) this source
 *    for own using or for redistribution, but you have to license the
 *    modified sources likewise under this LGPL Lesser General Public License.
 *    You mustn't delete this Copyright/Copyleft inscription in this source file.
 *
 * @author Hartmut Schorrig: hartmut.schorrig@vishia.de, www.vishia.org
 * @version 0.93 2011-01-05  (year-month-day)
 *******************************************************************************/ 
package org.vishia.byteData;



/**This interface defines the access to search a variable in any container. 
 * It is used as universal concept to hold named variables.
 * @author Hartmut Schorrig
 */
public interface VariableContainer_ifc
{

  /**Version, history and license
   * <ul>
   * <li>2012-03-31 Hartmut new: {@link #setCallbackOnReceivedData(Runnable)}. It is used for the
   *   {@link org.vishia.inspcPC.mng.InspcMng} and {@link org.vishia.guiInspc.InspcGui} now. 
   * <li>2010-06-00 Hartmut created. 
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
   * 
   */
  public static final int version = 20120331;
  
	/**Searches a variable in the given container by textual given name.
	 * @param name The name of the variable. It may contain indices in textual form
	 *             written in [index,index,...] whereby index is a numerical value.
	 *             ZBNF: \[ { <#?index> ? , } \]
	 * @param index may be null. If an index is expected in textual form inside the name,
	 *        this references should not be null. Then the index is stored there.
	 *        If an index is contained in name and this reference is null, the index won't be stored,
	 *        but the array-variable is returned correctly. The information about the index is lost then.
	 * @return null if the variable not found, else the variable access description.
	 */
	//VariableAccess_ifc getVariable(String name, int[] index);
	
	
	
	
	/**Searches the variable in the container and returns the access to it.
	 * <ul>
	 * <li>path[index]: The dataPath can have array access designations "[index]". Than usual an instance of
   *   {@link VariableAccessWithIdx} is returned, which contains the index and the reference to the variable
   *   in the container. If the variable is an array variable and the dataPath does not contains an "[index]",
   *   the array variable is returned which implements the {@link VariableAccessArray_ifc} usual. 
   * <li>path.9..5: The dataPath can contain a bit designation as last part. "9..5" means the bits 9 to 5, 
   *   it is the mask 0x03e, shiftet >>3. That value are presented then.
   *   If a bit designation is given, the return instance is an {@link VariableAccessWithBitmask} instance.
   * </ul>  
	 * @param dataPath Path. 
	 * @return Instance of access control to the variable. The instance may instanceof {@link VariableAccessArray_ifc}.
	 *   Then a cast should be recommended to access the array elements. That is a special case.
	 */
	VariableAccess_ifc getVariable(String dataPath);
	
	
	
	/**Ensures that the variables will get the actual values, maybe forces communication with any device. */
	//void refreshValues();
	
	void setCallbackOnReceivedData(Runnable callback);
}
