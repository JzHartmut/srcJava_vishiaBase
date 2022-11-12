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
 * @author Hartmut Schorrig = hartmut.schorrig@vishia.de
 * @version 2009-06-15  (year-month-day)
 * list of changes:
 * 2009-09-27: Hartmut ctor, param nrofSecondsToFlush: if ==0, than it doesn't close, but flush any time on write. 
 * 2008..2009: Hartmut: some changes
 * 2008 Hartmut created
 */
package org.vishia.msgDispatch;

/**@changes:
 * 2009-02-03 HScho   *new: setLogMessageOpenClose(...) it is a message output backward to designate open, close especially for test.
 *                    *new: method isOnline
 * 2009-02-05 HScho   *adap: no try on org.vishia.util.FileWriter.close()
 */

import org.vishia.util.FileWriter;
import java.io.IOException;
import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.Formatter;
import java.util.IllegalFormatConversionException;
import java.util.IllegalFormatPrecisionException;
import java.util.Locale;
import java.util.MissingFormatArgumentException;
import java.util.TimeZone;

import org.vishia.bridgeC.ConcurrentLinkedQueue;
import org.vishia.bridgeC.OS_TimeStamp;
import org.vishia.bridgeC.Va_list;
import org.vishia.util.Java4C;

/**This class is a LogMessage output which writes the messages into a file.
 * <br>Capability:
 * <ul>
 * <li>The file will be flushed or closed after a given time. Therefore it is possible to read the file with another tool.
 * <li>It is possible to define a name with a timestamp part. Then new files will be created with a timestamp in the name
 * <li>etc. todo
 * </ul>
 * @author Hartmut Schorrig
 *
 */
public class LogMessageFile extends LogMessageBase
{

  /**Version, history and license.
   * <ul>
   * <li>2014-12-19 Hartmut chg: moved from srcJava_vishiaRun to this component srcJava_vishiaBase because it is a base concept
   *   which does not use any specials with communication etc. Creating a new archive. 
   * <li>2013-03-24 Hartmut check size 
   * <li>2013-03-24 Hartmut bugfix: If a message text contains '%' formatting character but the variable arguments
   *   are empty, it should not call format(). In this case the text should keep its '%' characters.
   * <li>2012-04-05 Hartmut chg: If only a simple file name is given, it is closed and re-opened with append.
   *   This is proper if the logging file is written only on demand and it is removed or copied manually.
   * <li>2009-09-27: Hartmut ctor, param nrofSecondsToFlush: if ==0, than it doesn't close, but flush any time on write. 
   * <li>2008..2009: Hartmut: some changes
   * <li>2008 Hartmut created
   * </ul>
   * <br><br>
   * <b>Copyright/Copyleft</b>:
   * For this source the LGPL Lesser General Public License,
   * published by the Free Software Foundation is valid.
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
   */
  public static final int version = 20130324;


  
  
  /**@java2c=fixStringBuffer. */
  final StringBuilder sFilenameBuffer = new StringBuilder(120);
  
  /**StringBuilder to store the converted timeStamp for file time. 
   * Note: Need a StringBuffer instead StringBuilder because {@link SimpleDateFormat#format(java.util.Date, StringBuffer, FieldPosition)}
   * @java2c=fixStringBuffer. */
  final StringBuffer sDateformatBuffer = new StringBuffer(32);
  
  /**TODO  */
  String sFormatTimestampFilename;
  
  final SimpleDateFormat formatTimestamp = new SimpleDateFormat();
  
  /**Actual counter of multiFile. It is set to 1 before first open, if an asterisk is used in filename.
   */
  int counterMultifile = 0;
  
  final FieldPosition formatField = new FieldPosition(SimpleDateFormat.DATE_FIELD);
  
  //@Java4C.SimpleArray 
  final char[] charsFormatTimestampFilename;
  
  //final StringBuilder charsFormatTimestampFilename = new StringBuilder(32); 
  
  int posTimestampInFilename;
  
  /**
   * If it is -1, no multiFile designation is used.
   */
  int posMultifileInFilename;
  
  int currentLengthMultifileNr;
  
  final FileWriter file = new FileWriter();
  
  /**All OS:TimeStamp are simple embedded instances in C, 
   * but derived instances from java.util.Data in java.
   */
  final OS_TimeStamp timeOpen = new OS_TimeStamp() ;
  
  boolean bNewFile = true;
  
  final OS_TimeStamp timeWrite = new OS_TimeStamp();
  
  /**The last close time. */
  final OS_TimeStamp timeClose = new OS_TimeStamp();
  
  final int nrofSecondsToFlush;

  final int nrofSecondsToClose;
  
  /**Counts all non flushed writes (seconds without flush). */
  int cntAllNonFlushedWrite;
  
  /**Counts all non flushed writes in a flush period. */
  int cntNonFlushedWrite;

  /**Little statistic: max nr of writes without flush. */
  int maxCntNonFlushedWrite;
  
  boolean shouldFlushed = false;

  /**An open or close operation may be logged. See setLogMessageOpenClose(). */
  LogMessage msgOpenClose;
  
  /**Number for open and close message. The close messages have offsets see kMsg....*/
  int msgIdentOpenClose;
  
  
  public final static int kMsgOpen = 0;
  public final static int kMsgNewFile = 1;
  public final static int kMsgOpenError = 2;
  public final static int kMsgClose = 3;
  
  
  
  /**
   * -1 than close never automaticly.
   */
  int nrofHoursPerFile;
  
  /**Helper class for some debug informations.
   * @java2c=noObject, staticInstance.
   */
  static final class Dbg
  {
    int cntWriteError;
    
    int cntCloseError;
    
    int cntOpenFailed;
    
    int cntCreateNewBecauseOpenFailed;
    
    int cntFilePathIncorrect;
  }
  
  final Dbg dbg = new Dbg();
  
  /**List of messages to process if the file is able to open.
   * @java2c=noGC.
   */
  final ConcurrentLinkedQueue<MsgDispatcherCore.Entry> parkedOrders = new ConcurrentLinkedQueue<MsgDispatcherCore.Entry>(false);

  /**Common pool of entries to save messages.
   * @java2c=noGC.
   */
  final ConcurrentLinkedQueue<MsgDispatcherCore.Entry> freeEntries;
  
  /**The date format is fix. 
   * Use ' no . to separate milliseconds, because elsewhere MS-Excel shows it badly.
   */
  final private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS; ");
  
  final private Locale localization;
  
  final private TimeZone timeZone;
  
  //final char[] buffer = new char[1000];
  
  /**Buffer for the current line to assign. Don't use dynamic memory! */
  final private StringBuilder sBuffer = new StringBuilder(1000);
  
  /**Constructs the instance. 
   * @param sFilename path and name. 
   *        <br>If the name contains $..$, the string between $..$ is the date format in the filename.
   *        Example <code>"path/name_$MMM-ddhh_mm$.log"</code> produces a filename at ex.
   *        <code>"path/name_Jan27-1233.log"</code>. 
   *        <br>If the filename contains an asterisk '*', than at this position a number is counted 
   *        for every new file (multiFile-case), but the time stamp is not changed. 
   *        The time stamp is the start time of the application respectively the first using of the log after start.
   *          
   * @param nrofSecondsToFlush If negative, than the file will closed after this time of opend file.
   *        The file will be in closing state at least this time.
   *        <br>
   *        If positive, than a flush will be called after this time from first not flushed write.
   *        
   * @param nrofHoursPerFile 1... the time in hours, after which a new file is created.
   *                         0: A new file isn't created.
   *                         -1.. the time in seconds, after which a new file is created (only to use for test).
   * @param freeEntriesP A pool of free entries, managed by a ConcurrentLinkedQueue.
   */
  public LogMessageFile
  ( final String sFilename
  , final int nrofSecondsToFlush
  , final int nrofHoursPerFile
  , final Locale localization
  , final TimeZone timeZoneP
  , final ConcurrentLinkedQueue<MsgDispatcherCore.Entry> freeEntriesP
  )
  { if(localization ==null){
  	  this.localization = Locale.ROOT;
    } else {
    	this.localization = localization;
    }
    this.charsFormatTimestampFilename = new char[32];
    this.timeZone = timeZoneP == null ? TimeZone.getTimeZone("GMT") : timeZoneP;
    dateFormat.setTimeZone(this.timeZone);
    formatTimestamp.setTimeZone(this.timeZone);
  	String sTimestampInFilename;
    this.freeEntries = freeEntriesP;
    if(freeEntriesP != null)
    { parkedOrders.shareNodePool(freeEntriesP); 
    }
    else
    { //parkedOrders = null;
    }
    final int pos2TimestampInFilename;
    if(nrofSecondsToFlush > 0)
    { this.nrofSecondsToFlush = nrofSecondsToFlush;
      this.nrofSecondsToClose = -1;
    }
    else
    { this.nrofSecondsToClose = -nrofSecondsToFlush;  //use positive value!
      this.nrofSecondsToFlush = -1;
    }
    this.nrofHoursPerFile = nrofHoursPerFile;
    posTimestampInFilename = sFilename.indexOf('$');
    if(posTimestampInFilename >=0)
    { pos2TimestampInFilename = sFilename.indexOf('$', posTimestampInFilename+1);
      if(pos2TimestampInFilename >0)
      { sTimestampInFilename = sFilename.substring(this.posTimestampInFilename+1, pos2TimestampInFilename);
        int nrofCharsTimestampInFilename = sTimestampInFilename.length();
        /*Store the format String localy in this class. It's not depend from outside memory management. */
        sTimestampInFilename.getChars(0, nrofCharsTimestampInFilename, charsFormatTimestampFilename, 0);
        //charsFormatTimestampFilename.append(sTimestampInFilename);
        /*Represent the StringBuilder with a String.
         *@java2c=declarePersist. Because the String can reference the buffer direct in C. The buffer is persistent really.*/
        sFormatTimestampFilename = new String(charsFormatTimestampFilename, 0, nrofCharsTimestampInFilename);
        //sFormatTimestampFilename = charsFormatTimestampFilename.toString();
        //copyToBuffer_StringJc(sTimestampInFilename, ythis->charsFormatTimestampFilename, sizeof(ythis->charsFormatTimestampFilename)); 
      }
      else throw new IllegalArgumentException("second $ to delimit timestamp in filename missing."); //, this.posTimestampInFilename);
      this.formatTimestamp.applyPattern(sFormatTimestampFilename);
      this.sFilenameBuffer.append(sFilename.substring(0, this.posTimestampInFilename));
      if(sFilename.indexOf('*')>=0)
      { //The current timeStamp should be applied only here, it is the startup of the application.
        this.timeOpen.set(OS_TimeStamp.os_getDateTime());
        sDateformatBuffer.setLength(0);  //clear it.
        formatTimestamp.format(this.timeOpen, sDateformatBuffer, formatField);
        /**@java2c=toStringNonPersist. */
        String sTimeFileOpen = sDateformatBuffer.toString();
        this.sFilenameBuffer.append(sTimeFileOpen);
        posTimestampInFilename = -1;   //don't replace the time stamp a second one.
      }
      else
      { //use the format string only as placeholder
        this.sFilenameBuffer.append(sFormatTimestampFilename);
      }
      this.sFilenameBuffer.append(sFilename.substring(pos2TimestampInFilename+1));
    }
    else
    { //no timeStamp in filename given, not used.
      this.sFilenameBuffer.append(sFilename);
      bNewFile = false;  //append on existing file.
    }
    /**Determines the position of an asterisk. It shouldn't done in sFilename-parameter because deleted $$.
     * Do it in the once-for-all (resp. positions) filename.
     * @java2c=toStringNonPersist. The buffer-content is used only here.
     */ 
    posMultifileInFilename = this.sFilenameBuffer.toString().indexOf('*');  //may be -1, than not used.
    currentLengthMultifileNr = 1;  //initial the '*' is to replace.
    
  }
  
  
  /**Sets a log output if a open or close action is done. This is useful especially in test situations.
   * @param msg The message output may be the message dispatcher, and the message may be sent
   *            to this instance itself, it is no problem.
   * @param msgIdentOpenClose The msg ident number for the open message. The close message is +1.           
   */
  public void setLogMessageOpenClose(LogMessage msg, int msgIdentOpenClose)
  {
    msgOpenClose = msg;
    this.msgIdentOpenClose = msgIdentOpenClose;
  }
  
  
  /**Sends a message. See interface.  
   * @param identNumber
   * @param text The text representation of the message, format string, see java.lang.String.format(..). 
   *             @pjava2c=zeroTermString.
   * @param args see interface
   * @java2c=stacktrace:no-param.
   */
   @Override public boolean  sendMsg(int identNumber, String text, Object... args)
   { /**store the variable arguments in a Va_list to handle for next call.
      * The Va_list is used also to store the arguments between threads in the MessageDispatcher.
      * @java2c=stackInstance.*/
   	 final Va_list vaArgs = new Va_list(args);  
     return sendMsgVaList(identNumber, OS_TimeStamp.os_getDateTime(), text, vaArgs);
   }

   
   /**Sends a message. See interface.  
    * @param identNumber
    * @param creationTime
    * @param text The text representation of the message, format string, see java.lang.String.format(..). 
    *             @pjava2c=zeroTermString.
    * @param args see interface
    * @java2c=stacktrace:no-param.
    */
    @Override final public boolean  sendMsgTime(int identNumber, final OS_TimeStamp creationTime, String text, Object... args)
    { /**store the variable arguments in a Va_list to handle for next call.
       * The Va_list is used also to store the arguments between threads in the MessageDispatcher.
       * @java2c=stackInstance.*/
    	final Va_list vaArgs = new Va_list(args);  
      return sendMsgVaList(identNumber, creationTime, text, vaArgs);
    }

    
  
  /**Sends a message. See interface.  
   * @param identNumber
   * @param creationTime
   * @param text The text representation of the message, format string, see java.lang.String.format(..). 
   *             @pjava2c=zeroTermString.
   * @param typeArgs Type chars, ZCBSIJFD for boolean, char, byte, short, int, long, float double. 
   *                 @pjava2c=zeroTermString.
   * @param args see interface
   */
  @Override
  public boolean sendMsgVaList(int identNumber, final OS_TimeStamp creationTime, String text, final Va_list args)
  {
    final boolean sent; 
    /**@java2c=dynamic-call. Internal reference in stack. */
    final LogMessage msgOpenClose = this.msgOpenClose;  //may be null
    
    if(!bNewFile && nrofHoursPerFile !=0)
    { /**Test whether the filename is used longer as nrofHoursPerFile.
       * Do not entry this branch on startup, because timeOpen is not initialized.
       */
      final OS_TimeStamp timeTest = new OS_TimeStamp();
      int secDiff;
      timeTest.set(OS_TimeStamp.os_getDateTime());
      secDiff = timeTest.time_sec - this.timeOpen.time_sec;
      int secDiffTest = nrofHoursPerFile > 0? 3600 * nrofHoursPerFile : -nrofHoursPerFile;
      if(secDiff >= secDiffTest)
      { if(this.file.isOpen())
        { //printf("\nclose log-file\n");
          file.close();
          //catch(IOException exc){  }
        }
        /*indicate, that a new filename should be used. */
        bNewFile = true;  
      }
    }
    if(!this.file.isOpen())
    { /**file open because data are to write: */
      final boolean canOpen;
      if(nrofSecondsToClose > 0)
      { final OS_TimeStamp timeTest = new OS_TimeStamp(); 
      	timeTest.set(OS_TimeStamp.os_getDateTime());
        int secDiff = timeTest.time_sec - this.timeClose.time_sec;
        canOpen = (secDiff >= nrofSecondsToClose);
        /**If the file is closed less seconds up to now, it should be in close. */
      }
      else
      { canOpen = true;
      }
      if(canOpen)
      { boolean isOpen = false;
        boolean shouldOpenWithNewName = false;
        do
        { if(bNewFile && posTimestampInFilename >=0)
          { /**Build a new filename, after nrofHoursPerFile, but also first. */ 
            this.timeOpen.set(OS_TimeStamp.os_getDateTime());
            sDateformatBuffer.setLength(0);  //clear it.
            formatTimestamp.format(this.timeOpen, sDateformatBuffer, formatField);
            /** @java2c=toStringNonPersist. The buffer-content is used only here. */
            String sTimeFileOpen = sDateformatBuffer.toString();
            sFilenameBuffer.replace(posTimestampInFilename, posTimestampInFilename + sTimeFileOpen.length(), sTimeFileOpen);
          } else  if(bNewFile && posMultifileInFilename >=0)
          { /**Build a new filename, after nrofHoursPerFile, but also first, with counted number (multiFile)  
             * @java2c=stackInstance, fixStringBuffer. 
             */ 
            final StringBuilder bufferFormat = new StringBuilder(20);
            /**@java2c=stackInstance. */
            bufferFormat.append(++counterMultifile);
            /** @java2c=toStringNonPersist. The buffer-content is used only here. */
            String sCounterMultifile = bufferFormat.toString();
            sFilenameBuffer.replace(posMultifileInFilename, posMultifileInFilename + currentLengthMultifileNr, sCounterMultifile);
            currentLengthMultifileNr = sCounterMultifile.length();
          } else {
            bNewFile = false; //reopen the existing one.
          }
          /** @java2c=toStringNonPersist. The buffer-content is used only here. */
          int error = this.file.open(sFilenameBuffer.toString(), !bNewFile);
          if(error >= 0)
          { isOpen = true;
            bNewFile = false;
            if(msgOpenClose != null)
            { /** @java2c=toStringNonPersist. The buffer-content is used only here. */
              msgOpenClose.sendMsg(msgIdentOpenClose + kMsgOpen, "open %s", sFilenameBuffer.toString());
            }
          }
          else
          { if(false && error == FileWriter.kFileNotFound)  //TODO C can't difference the errors, is there a solution? open_FileWriterJc!
            { //System.err.println("file path incorrect");
              dbg.cntFilePathIncorrect +=1;
            }
            else
            { if(!shouldOpenWithNewName)
              { bNewFile = true;
                shouldOpenWithNewName = true;
                dbg.cntCreateNewBecauseOpenFailed +=1;
                if(msgOpenClose != null)
                { msgOpenClose.sendMsg(msgIdentOpenClose + kMsgNewFile, "new File");
                }
              }
              else
              { /**Don't try again if it is tried already. */
                shouldOpenWithNewName = false;
                dbg.cntOpenFailed +=1;
                if(msgOpenClose != null)
                { msgOpenClose.sendMsg(msgIdentOpenClose + kMsgOpenError, "fatalOpenError");
                }
              }
            }  
          }
        }while(!isOpen && shouldOpenWithNewName);
      }  
    }
    if(file.isOpen())
    { 
      MsgDispatcherCore.Entry parkedEntry;
      do
      { parkedEntry = parkedOrders.poll();
        if(parkedEntry != null)
        { /**There are parked outputs, now output it. */
          writeInFile(parkedEntry.ident, parkedEntry.timestamp, parkedEntry.text, parkedEntry.values.get_va_list());
          parkedEntry.values.clean();
          parkedEntry.ident = 0;  
          freeEntries.offer(parkedEntry);
        }
      }while(parkedEntry != null);
      /**Output the current message. */      
      writeInFile(identNumber, creationTime, text, args);
      sent = true;
    }
    else
    { //file can't open, 
      if(freeEntries != null)
      { MsgDispatcherCore.Entry entry = freeEntries.poll();  //get a new Entry from static data store
        if(entry == null)
        { /**Not able to send, because no entries. */
          sent = false;
        }
        else
        { /**write the informations to the entry, store it. */
          entry.dst = 0;
          entry.ident = identNumber;
          entry.text = text;
          entry.timestamp.set(creationTime);
          entry.values.copyFrom(text, args);
          parkedOrders.offer(entry);
          /**Storing message in queue is adequate to send. */
          sent = true; 
        }
      }  
      else
      { /**Not able to send. No queue available. */
        sent = false;
      }
    }
    return sent;
  }


  @Override public Appendable append(CharSequence csq) throws IOException {
    sendMsg(0, csq.toString());
    return this;
  }

  @Override public Appendable append(CharSequence csq, int start, int end) throws IOException {
    sendMsg(0, csq.subSequence(start, end).toString());
    return this;
  }

  @Override public Appendable append(char c) throws IOException {
    String s = "" + c;
    sendMsg(0, s);
    return this;
  }


  
  /**This method can be called after a cyclic time less than the nrofSecondsToFlush,
   * but in the same thread like writing.
   * The file will be flushed or closed
   */
  @Override public void flush()
  { if( nrofSecondsToClose > 0 && file.isOpen())
    { int secDiff;
      final OS_TimeStamp   timeTest1 = new OS_TimeStamp(true);
      secDiff = timeTest1.time_sec - this.timeWrite.time_sec;
      if(secDiff >= nrofSecondsToClose)
      { file.close();
        //catch(IOException exc){ dbg.cntCloseError +=1; }
        if(maxCntNonFlushedWrite < cntNonFlushedWrite)
        { /**Gets the maximum. */
          maxCntNonFlushedWrite = cntNonFlushedWrite;
        }
        shouldFlushed = false;
        timeClose.set(timeTest1);
        if(msgOpenClose != null)
        { msgOpenClose.sendMsg(msgIdentOpenClose + kMsgClose, "close");
        }
      }
    }
    if( nrofSecondsToFlush > 0 && shouldFlushed)
    { int secDiff;
    	final OS_TimeStamp timeTest = new OS_TimeStamp(true);
    	secDiff = timeTest.time_sec - this.timeOpen.time_sec;
      if(secDiff >= nrofSecondsToFlush)
      { try{ file.flush(); }
        catch(IOException exc){ dbg.cntCloseError +=1; }
        if(maxCntNonFlushedWrite < cntNonFlushedWrite)
        { /**Gets the maximum. */
          maxCntNonFlushedWrite = cntNonFlushedWrite;
        }
        shouldFlushed = false;
      }
    }
    
  }
  
  

  
  
  /**Tests
   * @param identNumber
   * @param creationTime
   * @param text The text representation of the message, format string, see java.lang.String.format(..). 
   *             @pjava2c=zeroTermString.
   * @param args
   */
  void writeInFile(int identNumber, final OS_TimeStamp creationTime, String text, final Va_list args)
  {
    /**@java2c=stackInstance, fixStringBuffer. */
    //@Java4C.
    final StringBuilder bufferFormat = new StringBuilder(1000);
    @Java4C.StringJc
    final CharSequence formattedText;  //In C a StringJc
    if(args.size() >0){
      /**@java2c=stackInstance. */
      final Formatter formatter = new Formatter(bufferFormat, localization);
      try{ formatter.format(text, args.get());
  		} catch(IllegalFormatConversionException exc){
  			bufferFormat.append("error in text format: ").append(text);
  		} catch(IllegalFormatPrecisionException exc){
        bufferFormat.append("error-precision in text format: ").append(text);
  		} catch(MissingFormatArgumentException exc){
        bufferFormat.append("error-argument in text format: ").append(text);
      } catch(Exception exc){
  		  bufferFormat.append("error-unknown in text format: ").append(text);
  	  }
      formatter.close();
      formattedText = bufferFormat;  //XX
    } else {
      formattedText = text;   //without args, don't try to format! The text may contain format characters.    
    }
		/**@java2c=stackInstance, fixStringBuffer. */
    final StringBuffer bufferTimestamp = new StringBuffer(30);
    dateFormat.format(creationTime, bufferTimestamp, formatField);
    sBuffer.setLength(0);
    /**Comming or Going-identification as String, negative identNumer = going message. @java2c=zeroTermString. */
    String sComGo;
    if(identNumber >=0)
    { sComGo = "+";
    }
    else
    { sComGo = "-";
      identNumber = -identNumber;
    }
    sBuffer.append(bufferTimestamp)
           .append("; ")   //hint: use semicolon to view the data with ms-excel in csv-format.
           .append(identNumber)
           .append(";").append(sComGo).append(";")  //do not concat strings, no using temp memory!
           .append(formattedText)
           //.append(String.format(text,args.buffer.get()))
           .append("\r\n")
           ;
    try
    { /**The StringBuilder-instance exists only one time, to prevent dynamically memory.
       * The toString() is only used in the thread immediately. 
       * Therefore: @java2c=toStringNonPersist. */
    	file.write(sBuffer.toString());
      /**The file should closed only after nrofSecondsToFlush,
       * but Thread usage is not prepared yet.
       * Only flush may be not sufficient, because the file access may be locked also for reading,
       * and no visualization of content may be possible.
       */
      if(nrofSecondsToFlush == 0)
      { //file.close();
        file.flush();
      }
      else if(!shouldFlushed)
      { /**The first write after a flush or in new file, get and store the time. */ 
        timeWrite.set(OS_TimeStamp.os_getDateTime()); 
        cntNonFlushedWrite = 1;
        shouldFlushed = true;
      }
      else
      { /**A second write in non-flushed file. */
        cntAllNonFlushedWrite +=1;
        cntNonFlushedWrite +=1;
      }
    }    
    catch(IOException exc){ dbg.cntWriteError +=1; }
  }  
  
  
  
  /**Closes the file and forces usage of a new file on next open.
   */
  public void close()
  { 
    { file.close();
      //catch(IOException exc){ dbg.cntCloseError +=1; }
    }
    bNewFile = true;
  }



  
  @Override
  public boolean isOnline()
  { return true; 
  }


}
