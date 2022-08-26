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

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.vishia.zbnf.ZbnfJavaOutput;


public class ByteDataSymbolicAccessReadConfig {

  /**Version, able to read as hex yyyymmdd.
	 * Changes:
	 * <ul>
	 * <li>2022-08-26 use association instead inheritance for {@link ByteDataSymbolicAccess} 
	 *   because this ...ReadConfig has its own meaning. The config class need not have the "IS A" property to the {@link ByteDataSymbolicAccess}.
	 *   It is better to have an extra organization of the access and the config. 
   * <li>2022-08-26 log removed, use System.err and System.out with its possibility to redirection, a better basically concept. 
   * <li>2013-11-22 Hartmut new: {@link #readVariableCfg(String, String)}, supports more as one instances
   *   with the same variable configuration, but from divergent sources. The variables should have a prefix. 
   * <li>2012-03-02 Hartmut new: extract from its super class, only
	 * </ul>
	 */
	public final static int versionStamp = 0x20220826;

	
	public final ByteDataSymbolicAccess symAccess;

	public final class ZbnfResult
	{ protected final List<ByteDataSymbolicAccess.Variable> variable1 = new LinkedList<ByteDataSymbolicAccess.Variable>();
	  //public Variable new_variable(){ return new Variable(); }

		/**New instance for ZBNF-component: <variable> */
		public ByteDataSymbolicAccess.Variable new_variable(){
	  	return ByteDataSymbolicAccessReadConfig.this.symAccess.new Variable();
	  }

		/**Set from ZBNF-component: <variable> */
		public void add_variable(ByteDataSymbolicAccess.Variable item){
	  	this.variable1.add(item);
	  }


	}



	/**This syntax describes the oam-variable-config-file.
	 * For example it is <pre>
	 * binMirror/loadLock: Z @0.0x2;  //a boolean value named "binMirror/loadLock" in byte 0 masked with 0x2
   * param/capbModulId.capId: B @734 +1 *96;  //an array of bytes started from address 734, 96 * 1 byte
	 * </pre>
   */
	private static final String syntaxSymbolicDescrFile =
		"OamConfig::= ==[OamVariables|AllVariableFromDB]== { <variable> ; } \\e. "
	+ "variable::= <$/\\.?name> : <!.?typeChar> @<#?bytePos>[\\.0x<#x?bitMask>] [ + <#?nrofBytes> [ * <#?nrofArrayElements>]]."
	;

  public ByteDataSymbolicAccessReadConfig() {
    super();
    this.symAccess = new ByteDataSymbolicAccess();
  }

  public ByteDataSymbolicAccessReadConfig(ByteDataSymbolicAccess symAccess) {
    super();
    this.symAccess = symAccess;
  }

  
  
  public int readVariableCfg(String sFileCfg)
  {
    return readVariableCfg("", sFileCfg); 
  }
  
  
  
  
  public int readVariableCfg(String preName, String sFileCfg)
  {
		ZbnfJavaOutput parser = new ZbnfJavaOutput();
		ZbnfResult rootParseResult = new ZbnfResult();
		File fileConfig = new File(sFileCfg);
		//File fileSyntax = new File("exe/oamVar.zbnf");
		String sError = parser.parseFileAndFillJavaObject(rootParseResult.getClass(), rootParseResult, fileConfig, syntaxSymbolicDescrFile);
	  int nrofVariable = 0;
		if(sError != null){
	  	System.err.println(sError);
	  } else {
	  	//success parsing
	  	for(ByteDataSymbolicAccess.Variable item: rootParseResult.variable1){
	  		this.symAccess.addVariable(preName + item.name, item);
	  		nrofVariable +=1;
	  	}
		}
		return nrofVariable;
	}

}
