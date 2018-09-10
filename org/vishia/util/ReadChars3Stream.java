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
package org.vishia.util;

import java.io.BufferedReader;
import java.io.IOException;

/**This class helps to convert a special ASCII format in a byte[]. The input represents a 16-bit-word in memory
 * with 3 chars. 
 * <br><br>
 * The coding:<br><br>
 * The first 2 chars contains bits 0..5 and bits 6..11. It is the (char - 0x30) & 0x3F.
 * The last char contains bit 12..15, (char-0x30) & 0x0f.
 * The bits (char-0x30)&x30 are a checksum over all other bits, with modulo 4.
 * The value "&&&" marks the end of the stream.
 * <br><br>
 * This coding helps to transmit binary data with an ASCII medium.
 * <br><br>
 * At example "000" is the value 0, "@10" is the value 0x40. (bit 6=1).
 *
 * @author Hartmut Schorrig
 * @since created 1996, Java-source created 2009-09.
 */
public class ReadChars3Stream
{
  
  /**
   * @param data Output array to write data.
   * @param idxStart Start index in data to write
   * @param input Input stream starting with a line containing 3-char-words.
   *              The terminated line should start with <code>&&&</code>.
   * @return The following line after the byte data if there is a following line, elsewhere null.             
   * @throws IOException if any io-operation failed.
   */
  public static String read(byte[] data, int idxStart, BufferedReader input) throws IOException
  { boolean bCont = true;
    int ixData = idxStart;
    boolean bStart = true;
    String sLine;
    do{ //at least read one line.
      sLine = input.readLine();
      if(sLine.startsWith("&&&")){
        if(!bStart){   //on first lines &&& will be accepted.
          bCont = false;  //break the loop. 
        }
      }
      else {
        bStart = false; //at least first data line.
        int zLine = sLine.length();
        int ixLine = 0;
        while(ixLine < zLine){
          short value = convert3Chars(sLine.substring(ixLine, ixLine+3));
          data[ixData++] = (byte)(value);
          data[ixData++] = (byte)(value>>8);
          ixLine += 3;
        }
        sLine = null; //because it is processed.
      }  
    } while(bCont && input.ready());
      
    if(ixData != data.length){
      for(int ix = ixData; ix < data.length; ix++){
        data[ix] = 0;
      }
      throw new IllegalArgumentException("to less data: " + data.length + "expected, " + ixData + " got. ");
    }
    return sLine;
  }
  
  
  private static short convert3Chars(String s3)
  {
    char cc;
    int charVal;
    int val = 0;
    short nCheckSoll;
    
    cc = s3.charAt(0);
    charVal = cc - '0';
    if(charVal >= 0x40) throw new IllegalArgumentException("fault first char " + s3);
    val = charVal;
    
    cc = s3.charAt(1);
    charVal = cc - '0';
    if(charVal >= 0x40) throw new IllegalArgumentException("fault second char " + s3);
    val = val | (charVal<<6);
    
    cc = s3.charAt(2);
    charVal = cc - '0';
    if(charVal >= 0x40) throw new IllegalArgumentException("fault third char " + s3);
    nCheckSoll = (short)(charVal >>4); 
    val = val | (charVal<<12);
    
    { /**calculate check code:*/
      short nCheck=0;
      short nBits=(short)val;
      { int ii; for(ii=0; ii<7; ii++){ nCheck+=nBits; nBits>>=2;} //checksumm 2 bit
      }
      nCheck&=3;   //2 bit Checksumm
      if(nCheckSoll != nCheck) throw new IllegalArgumentException("checksum error");
    }
    return (short)val;
    
  }
  

}
