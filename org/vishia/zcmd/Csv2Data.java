package org.vishia.zcmd;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.vishia.mainCmd.MainCmd;
import org.vishia.mainCmd.MainCmdLogging_ifc;
import org.vishia.util.Assert;
import org.vishia.util.FileSystem;
import org.vishia.util.IndexMultiTable;
import org.vishia.util.StringFunctions;
import org.vishia.util.StringPart;
import org.vishia.util.StringPartFromFile;
import org.vishia.util.StringPartFromFileLines;
import org.vishia.util.StringPartScan;
import org.vishia.xmlSimple.SimpleXmlOutputter;
import org.vishia.xmlSimple.XmlNode;
import org.vishia.zbnf.ZbnfParser;


/**This class can parse an Excel file given in the textual csv format and translate it either to XML or to internal Java data.
 * There are some specials in the text format of csv:
 * <ul>
 * <li>If the text in an Excel cell contains more as one line, the line will be separated with 0x0a instead 0x0d 0x0a. That is not meaningfully here.
 * <li>A text in an Excel cell which contains more as one line is presented in more as one line in the csv format with 0x0a as line separator.
 * The parser does not distinguish between 0x0a and 0x0d as line separators. The conclusion of the text of the cell is detected by recognizing
 * the quotes at begin of the cell and the closing quotes at the end of the cell in some lines later.
 * </ul>
 * csv-example:
 * <pre>
 * "column 1"; "column 2"; "column 3"; "column 4";
 * "This is a cell"; 123,05; "Text with
 * more as one
 * line in a cell"; "more data"
 * </pre>
 * @author Hartmut Schorrig
 *
 */
public class Csv2Data
{
 
  /**Version, history and license.
   * <ul>
   * <li>2015-07-04 new {@link #value(String)}, returns a float value if it is possible. 
   * <li>2014-02-09 created. Evaluation from excel content.
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
  //@SuppressWarnings("hiding")
  static final public String sVersion = "2014-06-10";

  //final Map<String, String> dataInput = new TreeMap<String, String>();
  
  
  final MainCmdLogging_ifc log;
  
  //Map<String, String> data = new TreeMap<String, String>();
  //Map<String, String> data = new IndexMultiTable<String, String>(IndexMultiTable.providerString);
  Map<String, Map<String, String>> dataMap = new IndexMultiTable<String, Map<String, String>>(IndexMultiTable.providerString);
  
  List<Map<String, String>> lines = new LinkedList<Map<String, String>>();
  
  List<String> columns = new ArrayList<String>();
  
  /**Column where the identifier is found. */
  int colident;
  
  /**The character which is the separator of columns in the line.
   * It is either ";" or ",", detect from the first line in {@link #createColumns(String)}. 
   */
  char separator;
  
  /**'.' if the {@link #separator} is ',', elsewhere ','. */
  char cDecimalSep;
  
  public Csv2Data(MainCmdLogging_ifc log){
    this.log = log;
  }
  
  public Csv2Data(){
    this.log = null;
  }
  
  public static void main(String[] cmdargs){
    String sError = null;
    Args args = new Args();
    CmdHandler mainCmd = new CmdHandler(args, cmdargs);
    try{ mainCmd.parseArguments(); }
    catch(Exception exception)
    { mainCmd.report("Argument error:", exception);
      mainCmd.setExitErrorLevel(MainCmdLogging_ifc.exitWithArgumentError);
      mainCmd.writeHelpInfo(null);
      sError = "Csv2Data - cmd line error";
    }
    if(sError ==null){
      Csv2Data main = new Csv2Data(mainCmd);
      main.parseCsv(args.sInputCsv);
    }
    if(sError !=null){
      mainCmd.writeError(sError);
    }
  }
  
  
  
  public String parseCsv(String sFileIn) {
    String sError = null;
    File fileIn = new File(sFileIn);
    BufferedReader reader = null;
    dataMap.clear();
    lines.clear();
    try {
      reader = new BufferedReader(new FileReader(fileIn));
    } catch (FileNotFoundException e) {
      sError = "Csv2Data - can't open file; " + fileIn.getAbsolutePath();
      reader = null;
    }
    if(reader != null){
      String sLine;
      try{
        sLine = reader.readLine();
        createColumns(sLine);
        while( sError == null && (sLine = reader.readLine())!=null){
          parseLine(sLine, reader);
        }
      } catch(IOException exc){ sError = "Csv2Data - readline failed in; " + fileIn.getAbsolutePath(); }
    }
    return sError;
  }
  
  
  /**The first line should contain the identifier for the columns.
   * The column of that identifier which is named "Identifier" is set as {@link #colident}.
   * It is the column which contains the identifier for the line.
   * @param sLine
   */
  public void createColumns(String sLine){
    columns.clear();
    String[] aColumns = sLine.split(";");   
    String[] aColumns2 = sLine.split(",");
    //detect separator by testing the proper splitting char:
    if(aColumns.length > aColumns2.length){  
      separator = ';';
      cDecimalSep = ',';
    } else {
      separator = ',';
      cDecimalSep = '.';
      aColumns = aColumns2;
    }
    int col = 0;
    for(String sColumn1: aColumns){
      String sColumn = sColumn1.trim();
      columns.add(sColumn); 
      if(sColumn.equals("Identifier")){
        colident = col;
      }
      col +=1;
    }
  }
  
  
  
  public String XXXparseLine(String sLine){
    int nCol = 0;
    String[] sCells = sLine.split(";");
    String sError = null;
    StringPart spLine = new StringPart(sLine);
    boolean bLineEnd = false;
    do{
      spLine.lentoAnyCharOutsideQuotion(";", Integer.MAX_VALUE);
      if(!spLine.found()){
        spLine.len0end();
        bLineEnd = spLine.length() ==0;
      }
      if(!bLineEnd){
        String sCell = spLine.getCurrentPart().toString();
        //data.put("xx", sCell);
        spLine.fromEnd().seek(1); //skip the separator
      }
    } while(spLine.found());
    return sError;
  }
  

  
  public void parseLine(String sLine, BufferedReader reader){
    int nCol = 0;
    //List<String> cells = new ArrayList<String>();
    Map<String, String> cells = new IndexMultiTable<String, String>(IndexMultiTable.providerString);
    int posQuotion, posEnd, posColon, pos=0;
    boolean cont = true;
    int col1 = 0;
    String sIdent = null;
    while(cont){
      posQuotion = sLine.indexOf('\"', pos);
      posColon = sLine.indexOf(separator, pos);
      String sCell;
      if(posColon <0){ 
        posColon = sLine.length(); 
        cont = false;
      } 
      
      if(posQuotion >=0 && posQuotion < posColon){
        //content in quotion
        posEnd = sLine.indexOf('\"', posQuotion+1);
        if(posEnd < 0){
          //text in "" is continued in the next line, in csv maybe separated with 0a instead 0d0a,
          //but java reads a line till 0a only too.
          sCell = sLine.substring(posQuotion+1);
          do { 
            try{ sLine = reader.readLine(); }
            catch(IOException exc){
              sLine = null;  //forces RuntimeException
            }
            pos = 0;
            posEnd = sLine.indexOf('\"');
            if(posEnd >=0){
              sCell += '\n' + sLine.substring(0, posEnd);
            } else {
              sCell += '\n' + sLine;
            }
          } while(posEnd <0);
        } else {
          sCell = sLine.substring(posQuotion+1, posEnd);
        }
        posColon = sLine.indexOf(';', posEnd);
        if(posColon < 0){
          cont = false;
        }
        
      } else {
        sCell = sLine.substring(pos, posColon).trim();
      }
        //cells.add(sCell);
      final String column;
      if(col1 >=0 && col1 < columns.size()){
        column = columns.get(col1);
      } else {
        column = "column_" + col1;   //more cells in line as in head line.
      }
      cells.put(column, sCell);
      pos = posColon +1;
      if(col1 == colident){
        sIdent = sCell;  
      }
      col1 +=1;
    }
    if(sIdent !=null && !sIdent.isEmpty()){
      col1 = 0;
      dataMap.put(sIdent, cells);
      lines.add(cells);
      /*
      for(String cell: cells){
        String key = sIdent + '-' + columns.get(col1);
        data.put(key, cell);
        col1 +=1;   
      }
      */
    }
  }
  

  
  
  public Object value(String cell){
    if(cell == null || cell.length() ==0){
      return null;  //no information
    } 
    else if(cell.startsWith("\"")){
      return cell;
    }
    else if(cell.startsWith("0x")){
      return cell;
    }
    else {  
      try{
        if(cell.indexOf(',')<0)
          Assert.stop();
        float val = StringFunctions.parseFloat(cell, 0, Integer.MAX_VALUE, cDecimalSep, null);
        return val;
      } catch(Exception exc){
        return 777777.7f;
      }
    }
  }
  
  
  
  public float floatVal(String cell){
    try{
      if(cell.indexOf(',')<0)
        Assert.stop();
      float val = StringFunctions.parseFloat(cell, 0, Integer.MAX_VALUE, cDecimalSep, null);
      return val;
    } catch(Exception exc){
      return 777777.7f;
    }
    
  }
  
  
  
  
  public String parseCsvZbnf(String sFileIn) {
    String sError = null;
    ZbnfParser parser = new ZbnfParser(log);
    
    String sSyntax = FileSystem.readFile(new File("zbnf/csvSyntax.zbnf"));
    
    try{ parser.setSyntax(sSyntax);
    
    } catch(ParseException exc){
      sError = "Csv2Data - zbnf syntax fails";
      log.writeError(sError, exc);
    }
    if(sError ==null){
      File fileIn = new File(sFileIn);
      StringPartScan spInput = null;
      try {
        spInput = new StringPartFromFileLines(fileIn);
      } catch (IOException exc) {
        sError = "Csv2Data - input file read fails";
        log.writeError(sError, exc);
      }
      if(sError ==null){
        boolean bOk = parser.parse(spInput);
        if(!bOk){
          sError = "Csv2Data - input file syntax";
          String sErrorSyntax = parser.getSyntaxErrorReport();
          log.writeError(sErrorSyntax);
        }
      }
      if(sError ==null){
        XmlNode xmlTree = parser.getResultTree();
        SimpleXmlOutputter writerXml = new SimpleXmlOutputter();
        try{
          OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream("csvinput.xml"));          
          writerXml.write(writer, xmlTree);
        } catch(IOException exc){
          log.writeError("Csv2Data - xmlOut fails", exc);
        }
      }
    }
    
    
    return sError;
  }
  

  
  static class Args
  {
    String sInputCsv;
  }
  
  
  static class CmdHandler extends MainCmd
  {

    protected final Args args;
    
    private final MainCmd.Argument[] arguments =
    { new MainCmd.Argument("-i", "=<CSV> Input csv file"
        , new MainCmd.SetArgument(){ @Override public boolean setArgument(String val){ 
          args.sInputCsv = val;
          return true; }})
    };

    
    CmdHandler(Args args, String[] cmdArgs){ 
      super(cmdArgs);
      super.addArgument(arguments);
      this.args = args; 
    }
    
    @Override
    protected boolean checkArguments()
    { return args.sInputCsv !=null;
    }
    
  }
}
