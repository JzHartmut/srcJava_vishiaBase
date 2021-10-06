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
 *
 ****************************************************************************/
package org.vishia.mainCmd;


import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/** The class FileWrite is a secondary helper class, the exception handling is included.
<hr/>
<pre>
date       who      change
2006-01-07 HarmutS  initial revision
*
</pre>
<hr/>
*/
public class FileWrite extends FileOutputStream
{


  public FileWrite(String sName) throws FileNotFoundException
  { super(sName);
  }

  public FileWrite(String sName, boolean bAppend) throws FileNotFoundException
  { super(sName, bAppend);
  }

  public void write(String sOut)
  {
    try{ super.write(sOut.getBytes()); }
    catch (IOException exception){ System.err.println("IOException: " + exception.getMessage()); }
  }

  public void writeln(String sOut){ write(sOut+"\r\n"); }

}

