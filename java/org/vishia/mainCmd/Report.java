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
 * @author JcHartmut = hartmut.schorrig@vishia.de
 * @version 2006-06-15  (year-month-day)
 * list of changes:
 * 2006-05-00: JcHartmut www.vishia.de creation
 * 2008-04-02: JcHartmut some changes
 *
 ****************************************************************************/

package org.vishia.mainCmd;

import org.vishia.msgDispatch.LogMessage;



/**This interface is the access to output Log- or report messages while running an application
 * to check its work.
 * In opposite to the {@link org.vishia.msgDispatch.LogMessage}, 
 * the report-interface doesn't use ident-numbers...
 * 
 *  <font color="0x00ffff">Dieses Interface dient zur Ausgabe von Reportmeldungen for kommandozeilenartige Abarbeitung.
    </font>
    This interface is usefull for reporting something (logfiles). It should be used in every algorithm routine
    to support debugging without using an extra debugger. It may help to encircle problems. 
<hr/>
<pre>
*
</pre>
<hr/>
 * @deprecated. The new name for that interface is {@link MainCmdLogging_ifc}. That identifier is better to understand.
 * 'Report' may disunderstand. But both are compatible yet.
 * 
 * If Report is used anywhere, the implementor of the MainCmdLogging_ifc should implement Report for compatibility.
*/
public interface Report extends MainCmdLogging_ifc{
  /**Gets a LogMessage output.
   * @return never null. 
   */
  LogMessage getLogMessageOutputConsole();
  
  /**Gets a LogMessage error output.
   * @return never null. 
   */
  LogMessage getLogMessageErrorConsole();
  
  /**Gets a LogMessage output.
   * @return never null. 
   */
  LogMessage getLogMessageOutputFile();
  

}


/** Some static functions to convert strings,
  usefull on C++-conversion
*/


class Stringc
{
  public static String convert(int nVal){ return("" + nVal); }
}

