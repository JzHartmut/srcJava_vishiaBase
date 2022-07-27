package org.vishia.msgDispatch;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


/**This class holds configuration information about messages, especially the message text associated to a ident number. 
 * */
public class MsgConfig implements MsgText_ifc
{

  /**version, history and license:
   * <ul>
   * <li>2014-12-20 Hartmut new: Reads information for file outputs, see {@link FileOutput}, in preparing, not used yet.
   * <li>2014-12-20 Hartmut bugfix: closes the read file, important for long running applications.
   * <li>2013-03-31 Hartmut new {@link #readConfig(File)} now regards 1000..1099 (range), $$ for ident strings,
   *   re-read of the config is possible (experience).
   * <li>2013-02-24 Hartmut new {@link #getListItems()}, move {@link MsgConfigItem} to {@link MsgText_ifc}.
   * <li>2010-08-00 Hartmut created 
   * </ul>
   * 
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
   * 
   */
  public final static int version = 20120822;

  
  
  /**From Zbnf: This class is used as setting class for Zbnf2Java, therefore all is public. The identifiers have to be used
   * as semantic in the parser script.
   */
  public static class MsgConfigZbnf
  { public final List<MsgConfigItem> item = new LinkedList<MsgConfigItem>();
  }
  
  
  public static class FileOutput
  {
    char dstChar;
    String path;
    int mask;
    
    public FileOutput(char dstChar, String path, int mask)
    {
      super();
      this.dstChar = dstChar;
      this.path = path;
      this.mask = mask;
    }
    
  }
  
  
  List<FileOutput> listFileOutput;
  
  
  /**Index over all ident numbers. */
  protected final Map<Integer, MsgConfigItem> indexIdentNr = new TreeMap<Integer, MsgConfigItem>(); 
  
  
  /**
   * @param log
   * @param sPathCfg
   */
  public MsgConfig()
  {
  }
  

  
  /**Reads the configuration from a file with given syntax. 
   * <br>Syntax:
   * <pre>
MsgConfig::= { <item> } \e.

item::= <#?identNr>[..<#?identNrLast>]  <!.?type> <*|\t|\ \ ?dst> [$$<*|\r|\n|\t|\ \ |$$|:|;?identText>[??<?useInternalText>]]  <*|\r|\n|\t|\ \ ?text>.
   * </pre>
   * The <code>dst</code> is a String with one or more characters which are associated to output channels of the message dispatcher.
   * The association between the characters and the outputs are made be the routine {@link #setMsgDispaching(MsgDispatcher, String)}.
   * Examples:
   * <pre>
1303 i x  $$Ident text is equal to - the output text; some more %d values; val=%d;
1400 i d  $$Ident text from - internal$$Other output text; val=%d; val2=%d;
1500 i d  $$Ident text from - internal$$
   * </pre>
   * Any line is designated with the semantic 'line'. A line is build by the shown syntax elements. 
   * The semantic have to be used like shown. The semantic identifier are given by the element names of the classes 
   * {@link MsgConfigZbnf} and {@link MsgConfigItem}. The syntax can be another one.
   *  
   * @param fileConfig
   * @return null if successfully, an error hint on error.
   */
  public String readConfig(File fileConfig){
    String sError = null;
    BufferedReader reader;
    MsgConfigZbnf rootParseResult = new MsgConfigZbnf();
    try{ 
      reader = new BufferedReader(new FileReader(fileConfig));
    } catch(FileNotFoundException exc){
      sError = "MsgConfig - config file not found; " + fileConfig.getAbsolutePath();
      reader = null;
    }
    if(sError == null){
      try{
        String sLine;
        int maskFileOutput = 0x10;
        rootParseResult.item.clear();
        while( sError == null && (sLine = reader.readLine()) !=null){
          sLine = sLine.trim();
          int zLine = sLine.length();
          if(sLine.startsWith("MsgFile:")) {
            int posDst = 8; while(posDst < zLine-2 && sLine.charAt(++posDst) == ' '){ posDst +=1; };
            char charDst = sLine.charAt(posDst);
            int posPath = posDst+1; while(posPath < zLine-2 && sLine.charAt(++posPath) == ' '){ posPath +=1; };
            String path = sLine.substring(posPath).trim();  //can contain spaces, but not recommended.
            if(listFileOutput == null) { listFileOutput = new LinkedList<FileOutput>(); };
            FileOutput item = new FileOutput(charDst, path, maskFileOutput);
            maskFileOutput <<=1;
            listFileOutput.add(item);
          }
          else if(!sLine.startsWith("//") && !sLine.startsWith("#") && zLine >0){
            int posIdentEnd = sLine.indexOf(' ');
            int posType = posIdentEnd; while(posType < zLine-2 && sLine.charAt(posType) == ' '){ posType +=1; };
            int posTypeEnd = sLine.indexOf(' ', posType);
            int posDst = posTypeEnd; while(posDst < zLine-2 && sLine.charAt(++posDst) == ' '){ posDst +=1; };
            int posDstEnd = sLine.indexOf(' ', posDst);
            MsgConfigItem item = new MsgConfigItem();
            try{ 
              int posIdentRange = sLine.indexOf("..");
              if(posIdentRange > 0 && posIdentRange < posIdentEnd){  //it is 1000..1099
                item.identNr = Integer.parseInt(sLine.substring(0, posIdentRange));
                item.identNrLast = Integer.parseInt(sLine.substring(posIdentRange+2, posIdentEnd));
              } else {
                item.identNr = Integer.parseInt(sLine.substring(0, posIdentEnd));
              }
            } catch(NumberFormatException exc){
              sError = "MsgConfig - Ident number false; " + sLine;
            }
            item.type_ = sLine.charAt(posType);
            item.dst = sLine.substring(posDst, posDstEnd);
            String text = sLine.substring(posDstEnd).trim();
            if(text.startsWith("$$")){
              int posSep2 = text.indexOf("$$", 2);
              int posSep = posSep2;
              if(posSep < 0){
                posSep = text.indexOf(';');
              }
              if(posSep >0){ //till ; or $$: it is the ident text of auto generated messages.
                item.identText = text.substring(2, posSep);
                if(posSep2 >= 0){
                  item.text = text.substring(posSep2+2).trim();
                  if(item.text.length() == 0){
                    item.text = null;    //the use internal text.
                  }
                } else {
                  item.text = text.substring(2);   //the whole text is the output text too.
                }
              } else {
                throw new IllegalArgumentException("format error, $$ or ; is needed in:" + sLine);
              }
            } else {
              item.text = text;  //the whole text is the ident string.
            }
            rootParseResult.item.add(item);
          }
        }
      }catch(Exception exc){
        sError = "MsgConfig - any read problem on config file; " + fileConfig.getAbsolutePath();
      }
    }
    if(sError ==null){
      //success parsing
      indexIdentNr.clear();
      for(MsgConfigItem item: rootParseResult.item){
        indexIdentNr.put(item.identNr, item);
      }
    }
    if(reader !=null){
      try{ reader.close(); }
      catch(IOException exc){ System.err.println("MsgConfig - cannot close the cfg file;"); }
      reader = null;
    }
    return sError;
  }  
  
  public int getNrofItems(){ return indexIdentNr.size(); }
  
  
  /**Sets the dispatching of all captured messages.
   * @param msgDispatcher
   * @param chnChars The characters which are associated to dstBits 0x0001, 0x0002 etc in
   *   {@link MsgDispatcher#setOutputRange(int, int, int, int, int)} in respect to the characters
   *   stored in the message config dst field. For example "df" if the dstBit 0x0001 is associated to the display
   *   and the dstBit 0x0002 is associated to an output file and "d" means "Display" and "f" means "File" in the config text.
   * @return The last message ident number which was used by this configuration.
   */
  public int setMsgDispaching(MsgDispatcher msgDispatcher, String chnChars){

    String dstMsg = "";
    int firstIdent = 0, lastIdent = -1;
    for(Map.Entry<Integer,MsgConfig.MsgConfigItem> entry: indexIdentNr.entrySet()){
      MsgConfig.MsgConfigItem item = entry.getValue();
      if(dstMsg.equals(item.dst)){
        lastIdent = item.identNr;    //items with equal dst, search the last one.
      } else {
        //a new dst, process the last one.
        if(lastIdent >=0){
          setRange(msgDispatcher, dstMsg, firstIdent, lastIdent, chnChars);
        }
        //for next dispatching range: 
        firstIdent = lastIdent = item.identNr;
        dstMsg = item.dst;
      }
    }
    setRange(msgDispatcher, dstMsg, firstIdent, lastIdent, chnChars);  //for the last block.
    //prevent the output of all other messages ???
    //do not so! //setRange(msgDispatcher, "", lastIdent, Integer.MAX_VALUE, chnChars);  //for the last block.
    System.err.println("MsgConfig - test message; test");
    return lastIdent;
  }
  
  
  private void setRange(MsgDispatcher msgDispatcher, String dstMsg, int firstIdent, int lastIdent, String chnChars){
    int dstBits = 0;
    for(int ixChn = 0; ixChn < chnChars.length(); ++ixChn){
      char chnChar = chnChars.charAt(ixChn);
      if(dstMsg.indexOf(chnChar)>=0){ dstBits |= (1<<ixChn); }  //output to file
    }
    msgDispatcher.setOutputRange(firstIdent, lastIdent, dstBits, MsgDispatcher.mSet, 3);
  }

  
  
  public Collection<MsgConfigItem> getListItems(){
    Collection<MsgConfigItem> list = indexIdentNr.values();
    return list;
  }
  
  
  
  /**Returns the text to a message ident.
   * @see org.vishia.msgDispatch.MsgText_ifc#getMsgText(int)
   */
  @Override public String getMsgText(int ident){
    MsgConfigItem item;
    if(ident < 0) item = indexIdentNr.get(-ident);
    else item = indexIdentNr.get(ident);
    return item !=null ? item.text : null;
  }
  
  
}
