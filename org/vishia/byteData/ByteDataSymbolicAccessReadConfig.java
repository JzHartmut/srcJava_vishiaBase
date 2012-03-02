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

import org.vishia.mainCmd.Report;
import org.vishia.zbnf.ZbnfJavaOutput;


public class ByteDataSymbolicAccessReadConfig extends ByteDataSymbolicAccess {

  /**Version, able to read as hex yyyymmdd.
	 * Changes:
	 * <ul>
	 * <li>2012-03-02 Hartmut new: extract from its super class, only
	 * </ul>
	 */
	public final static int versionStamp = 0x20120302;


	public class ZbnfResult
	{ private List<Variable> variable1 = new LinkedList<Variable>();
	  //public Variable new_variable(){ return new Variable(); }

		/**New instance for ZBNF-component: <variable> */
		public Variable new_variable(){
	  	return new Variable(ByteDataSymbolicAccessReadConfig.this);
	  }

		/**Set from ZBNF-component: <variable> */
		public void add_variable(Variable item){
	  	variable1.add(item);
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

  public ByteDataSymbolicAccessReadConfig(Report log) {
    super(log);
  }

	public int readVariableCfg(String sFileCfg)
	{
		ZbnfJavaOutput parser = new ZbnfJavaOutput(log);
		ZbnfResult rootParseResult = new ZbnfResult();
		File fileConfig = new File(sFileCfg);
		//File fileSyntax = new File("exe/oamVar.zbnf");
		String sError = parser.parseFileAndFillJavaObject(rootParseResult.getClass(), rootParseResult, fileConfig, syntaxSymbolicDescrFile);
	  int nrofVariable = 0;
		if(sError != null){
	  	log.writeError(sError);
	  } else {
	  	//success parsing
	  	for(Variable item: rootParseResult.variable1){
	  		addVariable(item.name, item);
	  		nrofVariable +=1;
	  	}
		}
		return nrofVariable;
	}

}
