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

