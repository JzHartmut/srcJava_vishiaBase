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
 * @author www.vishia.de/Java
 * @version 2006-06-15  (year-month-day)
 * list of changes: 
 * 2006-05-00: www.vishia.de creation
 *
 ****************************************************************************/
package org.vishia.util;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

public class StringPartFromFile extends StringPart
{
  final StringBuffer buffer;
  char[] fileBuffer = new char[1024];
  int nCharsFileBuffer = 0;
  boolean bEof;
  Reader readIn;

  /**Creates a StringPart from given file. 
   * @param fromFile
   * @throws FileNotFoundException
   * @throws IOException
   */
  public StringPartFromFile(File fromFile)
  throws FileNotFoundException, IOException
  { super();
    bEof = false;
    long nMaxBytes = fromFile.length();
    if(nMaxBytes < 65535)
    { buffer = new StringBuffer((int)(nMaxBytes+10));
    }
    else buffer = new StringBuffer(10000);  //to large file

    readIn = new FileReader(fromFile);
    readnextContentFromFile();
    assign(buffer.toString());
  }

  void readnextContentFromFile()
  throws IOException
  { {
      boolean bBufferFull = false;
      while(!bEof && !bBufferFull)
      { int nRestBytes = buffer.capacity() - buffer.length();
        if(nRestBytes >= nCharsFileBuffer)
        { if(nCharsFileBuffer > 0)
          { buffer.append(fileBuffer, 0, nCharsFileBuffer);
          }
          nCharsFileBuffer = readIn.read(fileBuffer);
          if(nCharsFileBuffer <0)
          { bEof = true;
          }
        }
        else bBufferFull = true;
      }
    }
  }


}
