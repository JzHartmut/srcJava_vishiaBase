package org.vishia.util;

import java.io.IOException;

/**This class is a RingBuffer which can use as pipe. Typical applications are thread-intercommunication.
 * Therefore the routines of this class are thread-safe.
 * <br><br>
 * In opposite to {@link java.io.PipedReader} and {@link java.io.PipedWriter} this class contains a method
 * to read a line: {@link #getLine(char[])}. It is similar {@link java.io.BufferedReader}.
 * In opposite to {@link java.nio.channels.Pipe} this class deals with simple Appendable, not with that channels.
 * It may be more simple for application.
 * <br><br>
 * The pipe holds characters in a buffer. The buffer will be filled with the {@link #append(CharSequence, int, int)}
 * or {@link #offer(CharSequence, int, int)} and read out with {@link #getLine(char[])}.
 * The buffer may have a larger size, for example more as 1 MByte to hold larger data. The storing of characters
 * is organized in a ring strucuture.
 * <br><br>
 * <ul>
 * <li>The {@link #offer(CharSequence, int, int)} routine does never block, it returns false if there is no space
 *   in the buffer. But on writing its data it synchronizes with this class to prevent inconsistent data.
 * <li>The append methods blocks if there is no space until any other thread has read out enough data.
 * <li>The {@link #getLine(char[])} method checks whether at least on char is in the Buffer. If the buffer is emtpy,
 *   it returns -1. If the buffer is not empty, it expects that a line may be found, because 
 *   it may be presumed that other threads writes whole lines. But it is possible that another thread has written only 
 *   a part of a line with {@link #append(CharSequence)}. In this case the other thread should work to complete.
 *   Therefore this thread waits 20 milliseconds, enough time for thread switch, and tries to complete the line.
 *   If this thread has a higher priority and the writing thread is very low, all other threads should work too.
 *   Only in that case the reading thread finishes its wait, try again, wait again etc.
 * <li>The {@link #get(Appendable, String)} does never wait. But it synchronized to prevent inconsistent data.
 * </ul>     
 * @author hartmut Schorrig
 *
 */
public class PipeRingBuffer implements Appendable
{
  /**Version, history and license.
   * <ul>
   * <li>not ready yet!
   * <li>2013-12-31 Hartmut new 
   * </ul>
   * 
   * <b>Copyright/Copyleft</b>:
   * For this source the LGPL Lesser General Public License, published by the Free Software Foundation is valid.
   * It means:
   * <ol>
   * <li> You can use this source without any restriction for any desired purpose.
   * <li> You can redistribute copies of this source to everybody.
   * <li> Every user of this source, also the user of redistribute copies
   *    with or without payment, must accept this license for further using.
   * <li> But the LPGL is not appropriate for a whole software product,
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
   * 
   */
  static final public String sVersion = "2013-12-31";

  private final char[] buffer;
  
  private int posw, posr, zchars;
  
  
  public PipeRingBuffer(int buffersize)
  {
    buffer = new char[buffersize];
  }

  
  
  @Override
  public Appendable append(CharSequence csq) throws IOException{ 
    return append(csq, 0, csq.length());
  }

  
  public boolean offer(CharSequence csq, int from, int to){
    int z = to - from;
    boolean ok;
    synchronized(this) {
      int capac = posr - posw;
      if(capac <=0){ capac += buffer.length; }
      if(capac < z){ ok= false; }
      else{
        int posw1 = posw;
        posw += z;
        if(posw >= buffer.length){
          posw -= buffer.length; 
        }
        int from1 = from;
        while(z >0){
          char cc = csq.charAt(from1);
          buffer[posw1++] = cc;
          if(posw1 >= buffer.length){
            posw1 = 0;
          }
        }
        ok = true;
      }
    }
    return ok;
  }
  
  @Override
  public Appendable append(char c) throws IOException
  {
    // TODO Auto-generated method stub
    return null;
  }

  
  
  
  @Override
  public Appendable append(CharSequence csq, int start, int end)
      throws IOException
  {
    InterruptedException eInterrupted = null;
    synchronized(this){
      while(eInterrupted ==null && !offer(csq, start, end)){
        try { wait(1000);
        } catch (InterruptedException exc) {
          eInterrupted = exc;
        }
      }
    }
    if(eInterrupted !=null){
      throw new IOException(eInterrupted);
    }
    return this;
  }
  
  
  
  int getLine(char[] dst){
    int zdst = dst.length;
    int idst = 0;
    synchronized(this){
      int lineend = posr;
      int z = posw - posr;
      if(z < 0){ z = buffer.length; }
      while(--z >=0 && idst < zdst){
        char cc = buffer[++lineend];
        if(lineend >= buffer.length){
          lineend =0;
        }
        if(cc == '\n' || cc=='r'){
          while(--z >=0){
            cc = buffer[lineend];
            if(cc == '\n' || cc=='r'){
              if(++lineend >= buffer.length){
                lineend =0;
              }
            } else { break; }
          }
          posr = lineend;
          break;
        } else {
          dst[idst++] = cc;
        }
      }
    }
    return 0;
  }
  
  
  /**Gets the available number of characters from Buffer till any of the charsBreak are found.
   * 
   * @param dst
   * @param charsBreak
   * @return 0 if the buffer has not characters to read. It is empty. <br>
   *   >0 if a charsBreak is reached, <br>
   *   <0 if some chars are read, but a charsBreak is not found till end of buffer.
   */
  public int get(Appendable dst, String charsBreak){
    return -1;
  }

  /**Skips over all characters which are member of charsBreak
   * @param charsBreak Characters to skip, for example "\r\n" to skip over both newline or return.
   * @return the number of skipped characters.
   */
  public int skip(String charsBreak){
    return 0;
  }
  
  
  
}
