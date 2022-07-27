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
import java.util.Arrays;

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
  
  /**Reads 3-char coded input
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
          CharSequence char3 = sLine.subSequence(ixLine, ixLine+3);
          int error = convert3Chars(char3, data, ixData);
          if(error !=0) {
            throw new IllegalArgumentException("error in data at pos " + ixData + " with chars: " + char3);
          }
          ixData+=2;
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
  
  
  /**Reads 3-char coded input
   * @param data Output array to write data.
   * @param idxStart Start index in data to write
   * @param input Input stream starting with a line containing 3-char-words.
   *              The terminated line should start with <code>&&&</code>.
   * @return null if no error and the number of read bytes are exactly data.length.
   *   If not null, then [0] contains the number of read chars. More elements contains the positions of errors.            
   */
  public static int[] readNoExc(byte[] data, int idxStart, BufferedReader input) throws IOException
  { boolean bCont = true;
    int ixData = idxStart;
    boolean bStart = true;
    String sLine;
    int[] faultyPos = null;
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
          CharSequence char3 = sLine.subSequence(ixLine, ixLine+3);
          int error = convert3Chars(char3, data, ixData);
          if(error !=0) {
            if(faultyPos == null) { faultyPos = new int[2];}
            else { 
              faultyPos = Arrays.copyOf(faultyPos, faultyPos.length +1);
            }
            faultyPos[faultyPos.length -1] = ixData;
          }
          ixData+=2;
          ixLine += 3;
        }
        sLine = null; //because it is processed.
      }  
    } while(bCont && input.ready());
      
    if(ixData != data.length){
      if(faultyPos == null) { faultyPos = new int[1];}
      faultyPos[0] = ixData;
      for(int ix = ixData; ix < data.length; ix++){
        data[ix] = 0;
      }
      throw new IllegalArgumentException("to less data: " + data.length + "expected, " + ixData + " got. ");
    } else if(faultyPos !=null) {
      faultyPos[0] = ixData;  //it is data.length
    }
    return faultyPos;
  }
  
  
  private static int convert3Chars(CharSequence s3, byte[] data, int ixData)
  {
    int error = 0;
    char cc;
    int charVal;
    int val = 0;
    short nCheckSoll;
    
    cc = s3.charAt(0);
    charVal = cc - '0';
    if(charVal >= 0x40) error = 1;
    val = charVal;
    
    cc = s3.charAt(1);
    charVal = cc - '0';
    if(charVal >= 0x40) error = 2;
    val = val | (charVal<<6);
    
    cc = s3.charAt(2);
    charVal = cc - '0';
    if(charVal >= 0x40) error = 3;
    nCheckSoll = (short)(charVal >>4); 
    val = val | (charVal<<12);
    
    { /**calculate check code:*/
      short nCheck=0;
      short nBits=(short)val;
      { int ii; for(ii=0; ii<7; ii++){ nCheck+=nBits; nBits>>=2;} //checksumm 2 bit
      }
      nCheck&=3;   //2 bit Checksumm
      if(nCheckSoll != nCheck) error = 4;
    }
    data[ixData] = (byte)(val);
    data[ixData+1] = (byte)(val>>8);

    return error;
    
  }
  
  
  
  
  
  /**
   * @param data
   * @param out
   * @throws IOException 
   */
  public static void write(byte[] data, int from, int length, Appendable out) throws IOException {
    //Note copied and adapted from SUL 9902
    int nWords = length /2;  
    int ixData = from;
    while(nWords >0)
    { for(int ixLine =0; ixLine < 16; ++ixLine)
      { { int nWord = (data[ixData] & 0xff) | ((data[ixData+1] <<8) & 0xff00); ;  //get one Word
          ixData +=2;
          int nBits=nWord;
          int nCheck=0;
          { //Note: should be ii<8 but this is so since 1996
            int ii; for(ii=0; ii<7; ii++){ nCheck += nBits; nBits >>= 2;} //checksumm 2 bit
          }
          nCheck&=3;   //2 bit Checksumm
          char cc;
          nBits=nWord & 0x3f;                            //first 6 bit;
          out.append(cc = (char)(nBits + '0'));  
          nWord>>=6; nBits=(nWord & 0x003f);             //next 6 bits
          out.append(cc = (char)(nBits + '0'));  
          nWord>>=6; nBits=(nWord & 0x000f)+(nCheck<<4); //last 4 bits and nCheck
          out.append(cc = (char)(nBits + '0'));  
        }
        if(--nWords==0) break;       //shorten line
      }
      out.append("\r\n");  
      
    }

  }
  
  
  

}
