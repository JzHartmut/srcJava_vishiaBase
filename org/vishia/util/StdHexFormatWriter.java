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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;


/**This class writes binary data in the Intel-Standard-Hex-Format.
 * This format is known since the time of perforated-tapes and it is used often for some linker results etc..
 * <br><br>
 * NOTE: yet 1M addresses are supported.
 * @author Hartmut Schorrig
 *
 */
public class StdHexFormatWriter extends OutputStream
{
 
	final Writer hexOutput;
	
	final byte[] buffer = new byte[16];
	
	final StringFormatter line = new StringFormatter(45);
	
	int addr = 0;
	
	int addrHiWritten = 0;
	
	int ixBuffer = -1;
	
	public StdHexFormatWriter(File file) throws FileNotFoundException 
	{
	  try {
	    this.hexOutput = new FileWriter(file);
    } catch (IOException e) {
      throw new FileNotFoundException(file.getAbsolutePath());
    }
  }

	/**A new address flushes the output and begins a new line of output.
	 * The address may be a linker block address.
	 * @param value The address
	 * @throws IOException if flush failes.
	 */
	public void setAddress(int value) throws IOException
	{
		flush();
		addr = value;
	}
	

	private void writeLine() throws IOException
	{
		if(addrHiWritten != (addr & 0xFFFF0000)) {
			/**Write a changed higher address. */
			addrHiWritten = addr & 0xFFFF0000;
			line.reset().add(":02000002")                              //2 Bytes, addr=0, ident=02
			    .addHex(addrHiWritten >>4, StringFormatter.k4right) ;  //address bit 20..4
	    byte chksum = (byte)(4 + (addrHiWritten >>4) + (addrHiWritten >>12));
			line.addHex(-chksum, StringFormatter.k2right).add("\r\n"); //write negative chksumm
			hexOutput.write(line.toString());
		}
		
		/**Write data bytes: */
		line.reset().add(":")                             //first char
		    .addHex(ixBuffer+1,StringFormatter.k2right)   //number of bytes in record
		    .addHex(addr, StringFormatter.k4right)        //address (16 bit)
		    .add("00");                                   //ident it is a data record
		
		/**Checksumm is the summ of all bytes inclusive length, ident and address. */
		byte chksum = (byte)(ixBuffer+1 + (addr) + (addr>>8));
		for(int ii=0; ii<=ixBuffer; ++ii){
  		byte val = buffer[ii];                   
  		chksum += val;
  		line.addHex(val, StringFormatter.k2right);      //data byte
		}	
  		line.addHex(-chksum, StringFormatter.k2right).add("\r\n"); //write negative chksumm
  		hexOutput.write(line.toString());
  		addr += (ixBuffer+1);
  		ixBuffer = -1;
  		line.reset();
		
	}
	

	@Override
  public void write(int b) throws IOException 
  {
	  buffer[++ixBuffer] = (byte)b;
	  if(ixBuffer >=15){
	  	writeLine();
	  }
  }
	
	
	@Override
  public void flush() throws IOException 
  {
  	if(ixBuffer >=0){
      writeLine();
  	}
  }
	
	
	@Override
  public void close() throws IOException 
  { flush();
    hexOutput.write(":00000001FF\r\n");
    hexOutput.close();
  }
	
	
}
