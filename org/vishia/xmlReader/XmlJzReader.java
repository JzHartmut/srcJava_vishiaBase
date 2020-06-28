package org.vishia.xmlReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.vishia.util.Assert;
import org.vishia.util.DataAccess;
import org.vishia.util.Debugutil;
import org.vishia.util.FileSystem;
import org.vishia.util.IndexMultiTable;
import org.vishia.util.StringFunctions;
import org.vishia.util.StringPartFromFileLines;
import org.vishia.util.StringPartScan;
import org.vishia.xmlSimple.XmlSequWriter;



/*Test with jztxtcmd: call jztxtcmd with this java file with its full path:
D:/vishia/ZBNF/srcJava_vishiaBase/org/vishia/xmlReader/XmlJzReader.java
==JZtxtcmd==
currdir = "D:/vishia/ZBNF/examples_XML/XMLi2reader";
Obj cfg = File:"readExcel.cfg.xml"; 
Obj xmlReader = new org.vishia.xmlReader.XmlJzReader();
xmlReader.setDebugStop(-1);
xmlReader.readCfg(cfg);    
Obj src = File:"testExcel_a.xml";
Obj data = new org.vishia.xmlReader.ExcelData();
xmlReader.setDebugStop(-1);
xmlReader.readXml(src, data);

==endJZcmd==
 */


/**This is the main class to read an XML file with a given configuration file, store data in a Java instance via reflection paths given in the config.xml.
 * A configuration file, written in XML too, contains the data path for the XML elements which should be stored in Java data.
 * A main method does not exists because it is only proper to invoke the read routine inside a Java application. 
 * <br>
 * Application example:
 * <pre>
 * XmlJzReader xmlReader = new XmlJzReader(); //instance to work, more as one file one after another
 * xmlJzReader.readCfg(cfgFile);             //configuration for next xmlRead()
 * AnyClass data = new AnyClass();        //a proper output instance matching to the cfg
 * xmlReader.readXml(xmlInputFile, data); //reads the xml file and stores read data.
 * </pre>
 * 
 * The configuration file contains the template of the xml file with paths to store the content in a proper user instance
 * The paths is processed with {@link DataAccess.DatapathElement}. The configuration is hold in an instance of {@link XmlCfg}.
 * Detail description of the config file see {@link XmlCfg}.
 * @author Hartmut Schorrig.
 *
 */
public class XmlJzReader
{
  /**Version, License and History:
   * <ul>
   * <li>2020-06-28 Hartmut new now supports &lt;![CDATA[ ... ]]>
   * <li>2020-02-12 Hartmut new {@link #readXml(Reader, String, Object)} and {@link #readXml(StringPartScan, Object)} 
   * <li>2020-01-15 Hartmut Improve handling of &#code characters. 
   * <li>2020-01-01 Hartmut Exception on file errors. 
   * <li>2019-12-30 Hartmut Using {@link XmlSequWriter} for Test output the read content, 
   *   if {@link #openXmlTestOut(File)} is invoked. . 
   * <li>2019-08-19 Hartmut full support of text special sequences. 
   * <li>2019-08-10 Hartmut refactoring of {@link DataAccess}, handling of arguments on access routines changed. 
   * <li>2019-05-29 Now skips over &lt;!DOCTYPE....>
   * <li>2018-09-09 Renamed from XmlReader to XmlJzReader, because: It is a special reader. 
   * It stores Data to Java (J) and works with a configfile which has a semantic (z as reverse 's').
   * <li>2017-12-25 first version which can be used.
   * <li>2017-01 created.
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
   */
  public static final String version = "2020-06-28";
  
  
  /**To store the read configuration. */
  XmlCfg cfg = new XmlCfg();
  
  
  /**Configuration to read a config file. */
  final XmlCfg cfgCfg;
  
  
  
  /**Size of the buffer to hold a part of the xml input file. It should be enough large to hold 1 element with attributes (without content). */
  int sizeBuffer = 20000;
  
  int debugStopLine = -1;
  
  /**Assignment between nameSpace-alias and nameSpace-value gotten from the xmlns:ns="value" declaration in the read XML file. */
  Map<String, String> namespaces = new IndexMultiTable<String, String>(IndexMultiTable.providerString);
   
   
  private final Map<String, String> replaceChars = new TreeMap<String, String>();

   
  
  XmlSequWriter xmlTestWriter;
  
  
  
  public XmlJzReader() {
    this.cfgCfg = XmlCfg.newCfgCfg();
    this.replaceChars.put("&amp;", "&");
    this.replaceChars.put("&lt;", "<");
    this.replaceChars.put("&gt;", ">");
    this.replaceChars.put("&quot;", "\"");
    this.replaceChars.put("&apos;", "\'");
    this.replaceChars.put("&nl;", "\n");
    this.replaceChars.put("&cr;", "\r");
    this.replaceChars.put("&#9;", "\t");
    this.replaceChars.put("&#A;", "\n");
    this.replaceChars.put("&#D;", "\r");
    this.replaceChars.put("&#20;", " ");
    //not xml conform, old style html
    this.replaceChars.put("&auml;", "ä");   //Note: UTF-8 in source, compile with UTF8 for the javac
    this.replaceChars.put("&ouml;", "ö");
    this.replaceChars.put("&uuml;", "ü");
    this.replaceChars.put("&Auml;", "Ä");
    this.replaceChars.put("&Ouml;", "Ö");
    this.replaceChars.put("&Uuml;", "Ü");
  }   
   
  
  /**Only for internal debug. See implementation. There is a possibility to set a break point if the parser reaches the line.
   * @param line
   */
  public void setDebugStop(int line) {
    this.debugStopLine = line;
  }
   
  public void XXXXreadXmlCfg(File input) {
    this.cfg = new XmlCfg();
    //read
  }
  

  public void openXmlTestOut(File fout) throws IOException {
    if(this.xmlTestWriter == null) { this.xmlTestWriter = new XmlSequWriter(); }
    this.xmlTestWriter.open(fout, "UTF-8", null);
  }
  
  

  /**Reads an xml file with a given config. 
   * It does not change the stored config which is gotten by {@link #readCfg(File)} or {@link #readCfgFromJar(String)}.
   * This operation is used internally in for all read operations too. It is the common entry to read.
   * @param input
   * @param output
   * @param xmlCfg
   * @return
   */
  public String readXml(File input, Object output, XmlCfg xmlCfg) throws IOException {
    String error = null;
    InputStream sInput = null; 
    try{ 
      sInput = new FileInputStream(input);
      String sPathInput = FileSystem.normalizePath(input.getAbsoluteFile()).toString();
      error = readXml(sInput, sPathInput, output, xmlCfg);
      sInput.close();
    } catch(FileNotFoundException exc) {
      throw new FileNotFoundException( "XmlJzReader.readXml(...) file not found: " + input.getAbsolutePath());
    } catch(IOException exc) {
      throw new IOException( "XmlJzReader.readXml(...) any IO exception: " + input.getAbsolutePath());
    }
    return error;
  }




  /**Reads an xml File from a zipfile. It uses the given config. 
   * Either {@link #readXmlCfg(File)} was invoked before or the config will be read itself (with {@link #cfgCfg} as configuration).
   * @param zipInput The zipfile itself
   * @param pathInZip The path of a file inside the zip file
   * @param output destination data
   * @return
   */
  public String readZipXml(File zipInput, String pathInZip, Object output) {
    String error = null;
    try {
      ZipFile zipFile = new ZipFile(zipInput);
      ZipEntry zipEntry = zipFile.getEntry(pathInZip);
      InputStream sInput = zipFile.getInputStream(zipEntry);
      String sInputPath = zipInput.getAbsolutePath() + ":" + pathInZip;
      error = readXml(sInput, sInputPath, output, this.cfg);
      sInput.close();
      zipFile.close();
    } catch(Exception exc) {
      error = exc.getMessage();
    } 
    return error;
  }



  
  /**Reads the xml content from an opened stream.
   * The stream is firstly tested whether the first line contains a encoding hint. This is obligate in XML.
   * Then the input is read into a character buffer using the {@link StringPartScan} class. 
   * The {@link StringPartScan} scans the XML syntax. 
   * <br>
   * The xmlCfg determines which elements, attributes and textual content is transferred to the output data.
   * See Description of {@link XmlJzReader}.
   * @param input any opened InputStream. Typically it is an FileInputStream or InputStream from a {@link ZipEntry}.
   * @param sInputPath The path to the input stream, used for error hints while parsing.
   * @param output Any output data. The structure should match to the xmlCfg.
   * @param xmlCfg A configuration. It can be gotten via {@link #readCfg(File)}.
   * @return null if no error. Elsewhere an error message, instead of throwing.
   */
  public String readXml(InputStream input, String sInputPath, Object output, XmlCfg xmlCfg) {
    String error = null;
    StringPartScan inp = null;
    try {
      inp = new StringPartFromFileLines(input, sInputPath, this.sizeBuffer, "encoding", null);
      readXml(inp, output, xmlCfg);
    } catch (IllegalCharsetNameException | UnsupportedCharsetException | IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch(Throwable exc) {
      exc.printStackTrace();
      Debugutil.stop();
    } finally {
      if(inp !=null) { inp.close(); }
    }
    return error;
  }
  
  
  /**Reads the xml content from an StringPart.
   * The stream is firstly tested whether the first line contains a encoding hint. This is obligate in XML.
   * Then the input is read into a character buffer using the {@link StringPartScan} class. 
   * The {@link StringPartScan} scans the XML syntax. 
   * <br>
   * The xmlCfg determines which elements, attributes and textual content is transferred to the output data.
   * See Description of {@link XmlJzReader}.
   * @param input any opened InputStream. Typically it is an FileInputStream or InputStream from a {@link ZipEntry}.
   * @param sInputPath The path to the input stream, used for error hints while parsing.
   * @param output Any output data. The structure should match to the xmlCfg.
   * @param xmlCfg A configuration. It can be gotten via {@link #readCfg(File)}.
   * @return null if no error. Elsewhere an error message, instead of throwing.
   * @throws Exception 
   */
  public void readXml(StringPartScan input, Object output) throws Exception {
    readXml(input, output, this.cfg);
  }
  
  
  /**Reads the xml content from an opened stream.
   * The stream is firstly tested whether the first line contains a encoding hint. This is obligate in XML.
   * Then the input is read into a character buffer using the {@link StringPartScan} class. 
   * The {@link StringPartScan} scans the XML syntax. 
   * <br>
   * The xmlCfg determines which elements, attributes and textual content is transferred to the output data.
   * See Description of {@link XmlJzReader}.
   * @param input any opened InputStream. Typically it is an FileInputStream or InputStream from a {@link ZipEntry}.
   * @param sInputPath The path to the input stream, used for error hints while parsing.
   * @param output Any output data. The structure should match to the xmlCfg.
   * @param xmlCfg A configuration. It can be gotten via {@link #readCfg(File)}.
   * @return null if no error. Elsewhere an error message, instead of throwing.
   */
  public String readXml(Reader input, String sInputPath, Object output, XmlCfg xmlCfg) {
    String error = null;
    StringPartScan inp = null;
    try {
      inp = new StringPartFromFileLines(input, sInputPath, this.sizeBuffer);
      readXml(inp, output, xmlCfg);
    } catch (IllegalCharsetNameException | UnsupportedCharsetException | IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch(Throwable exc) {
      exc.printStackTrace();
      Debugutil.stop();
    } finally {
      if(inp !=null) { inp.close(); }
    }
    return error;
  }
  
  
  
  /**Core routine to read in whole XML stream.
   * @param inp
   * @param output
   * @param cfg1
   * @throws Exception
   */
  private void readXml(StringPartScan inp, Object output, XmlCfg cfg1) 
  throws Exception
  { inp.setIgnoreWhitespaces(true);
    while(inp.seekEnd("<").found()) { //after the beginning < of a element
      inp.scanStart();
      if(inp.scan("?").scanOk()) { //skip over the first "<?xml declaration line ?>
        inp.seekEnd("?>");    //skip over the <? head info ?>. Note: The encoding is regarded from StringPartScan
        inp.scanOk();    //sets the scan start to this position.
      } else if(inp.scan("!").scanOk()) { //skip over the first "<?xml declaration line ?>
        inp.seekEnd(">");    //skip over the <? head info ?>. Note: The encoding is regarded from StringPartScan
        inp.scanOk();    //sets the scan start to this position.
      } else if(inp.scan().scan("!--").scanOk()) { //comment line
        inp.seekEnd("-->");
       }
       else {
        inp.scanOk();
        inp.readNextContent(this.sizeBuffer*2/3);
        {
          parseElement(inp, output, cfg1.rootNode);  //the only one root element.
        }
      }
    }
    if(this.xmlTestWriter !=null) {
      this.xmlTestWriter.close();
      this.xmlTestWriter = null;
    }

    Debugutil.stop();
  }



  /**Parse a whole element with all inner content
   * @param inp scanOk-Position after the "<" before the identifier.
   * @param output
   * @param cfg1
   * @throws Exception 
   */
  private void parseElement(StringPartScan inp, Object output, XmlCfg.XmlCfgNode cfgNode) 
  throws Exception
  { 
    int dbgline = -7777;
    if(this.debugStopLine >=0){
      dbgline = inp.getLineAndColumn(null);
      if(dbgline == this.debugStopLine)
        Debugutil.stop();
    }
    //scan the <tag
    if(!inp.scanIdentifier(null, "-:.").scanOk()) {
      throw new IllegalArgumentException("tag name expected");
    }
    //
    //The tag name of the element:
    String sTag = inp.getLastScannedString().toString();
    
    if(this.xmlTestWriter !=null) {
      this.xmlTestWriter.writeElement(sTag);
    }
    
    if(sTag.contains("   "))
      Debugutil.stop();
    if(sTag.equals("Object"))
      Debugutil.stop();
    //TODO replace alias.
    //
    //search the tag name in the cfg:
    //
    Object subOutput = null;
    XmlCfg.XmlCfgNode subCfgNode;
    if(cfgNode == null) {   //check whether this element should be regarded:
      subOutput = null;     //this element should not be evaluated.
      subCfgNode = null;
    } else {
      if(output ==null) {
        Debugutil.stop();
      }
      Assert.check(output !=null);
      if(sTag.toString().contains("   "))
        Debugutil.stop();
      if(sTag.toString().startsWith("Object@"))
        Debugutil.stop();
      if(cfgNode.subnodes == null) {
        subCfgNode = null; //don't read inner content
      } else {
        subCfgNode = cfgNode.subnodes.get(sTag);  //search the proper cfgNode for this <tag
        if(subCfgNode == null) {
          subCfgNode = cfgNode.subnodes.get("?");  //check whether it is a node with any unspecified tag name possible. 
//          subCfgNode = cfgNode.subNodeUnspec; 
        }
      }
      if(sTag.toString().contains("   "))
        Debugutil.stop();
      //subCfgNode is null if inner content should not be read.
      //
      //get the subOutput before parsing attributes because attribute values should be stored in the sub output.:
//      if(subCfgNode !=null && subCfgNode.bStoreAttribsInNewContent) { //the tag was found, the xml element is expected.
//        subOutput = getDataForTheElement(output, subCfgNode, sTag, null);
//        if(subOutput == null) {
//          Debugutil.stop();
//        }
//        //
//      } else {
//        subOutput = null; //don't store output. 
//      }
    }
    //
    @SuppressWarnings("unchecked")
    Map<String, DataAccess.IntegerIx>[] attribNames = new Map[1];
    if(subCfgNode !=null) {
      attribNames[0] = subCfgNode.allArgNames;  //maybe null if no attribs or text and tag are used.
    }
    //@SuppressWarnings("unchecked")
    String[] attribValues = null;
    @SuppressWarnings("unchecked")
    List<AttribToStore>[] attribsToStore = new List[1];
    //
    //For attribute evaluation, use the subCfgNode gotten from sTag. It may be necessary to change the subCfgNode after them. 
    //
    if(attribNames[0] !=null) {
      attribValues = new String[subCfgNode.allArgNames.size()];
      DataAccess.IntegerIx ixO = attribNames[0].get("tag");
      if(ixO !=null) { attribValues[ixO.ix] = sTag; } 
    }
    if(dbgline == this.debugStopLine)
      Debugutil.stop();
    CharSequence keyResearch = parseAttributes(inp, sTag, subCfgNode, attribsToStore, attribNames, attribValues);
    //
    if(keyResearch.length() > sTag.length()) {
      //Search the appropriate cfg node with the qualified keySearch, elsewhere subCfgNode is correct with the sTag as key. 
      subCfgNode = cfgNode.subnodes == null ? null : cfgNode.subnodes.get(keyResearch);  //search the proper cfgNode for this <tag
    }
    //The subOutput is determined with the correct subCfgNode, either with keySearch == sTag or a attribute-qualified key:
    subOutput = subCfgNode == null ? null : getDataForTheElement(output, subCfgNode.elementStorePath, attribValues);
    //
    //store all attributes in the content which are not used as arguments for the new instance (without "!@"):
    if(attribsToStore[0] !=null) { 
      if(subOutput ==null) {
        System.err.println("Problem storing attribute values, getDataForTheElement \"" + subCfgNode.elementStorePath + "\" returns null");
      } else {
        for(AttribToStore e: attribsToStore[0]) {
          storeAttrData(subOutput, e.daccess, subCfgNode.allArgNames, e.name, e.value);  //subOutput is the destination to store
    } } }
    //
    //check content.
    //
    if(inp.scan("/").scan(">").scanOk()) {
      //end of element
    }
    else if(inp.scan(">").scanOk()) {
      //textual content or sub nodes
      if(this.xmlTestWriter !=null) {
        this.xmlTestWriter.writeElementHeadEnd(false);
      }
      StringBuilder contentBuffer = null;
      //
      //loop to parse <tag ...> THE CONTENT </tag>
      while( ! inp.scan().scan("<").scan("/").scanOk()) { //check </ as end of node
        inp.readNextContent(this.sizeBuffer/2);
        if(inp.scan("<").scanOk()) {
          if(inp.scan("!--").scanOk()) {
            inp.seekEnd("-->");
          }
          else if(inp.scan("![CDATA[").scanOk()) {
            if(contentBuffer == null && subOutput !=null) { contentBuffer = new StringBuilder(500); }
            boolean bEndFound;
            do { inp.lento("]]>");
              bEndFound = inp.found();
              if(!bEndFound) { //the current input does not contain the end characters.
                inp.len0end().seekPos(-3);  //do not seek till end. It may contain a start of the "]]>" sequence.
                contentBuffer.append(inp.getCurrent());
                inp.fromEnd();
                inp.readNextContent(this.sizeBuffer/2);
              } else {
                contentBuffer.append(inp.getCurrent());
                inp.fromEnd();
              }
            } while(!bEndFound || inp.length() == 0);  //should be abort if no "]]>" found in the whole XML
            inp.seekPos(3); //skip over the "]]>"
          }
          else {
            parseElement(inp, subOutput, subCfgNode);  //nested element.
          }
        } else {
          if(contentBuffer == null && subOutput !=null) { contentBuffer = new StringBuilder(500); }
          parseContent(inp, contentBuffer);  //add the content between some tags to the content Buffer.
        }
      }
      //
      inp.readNextContent(this.sizeBuffer/2);
      //the </ is parsed on end of while already above.
      if(!inp.scanIdentifier(null, "-:.").scanOk())  throw new IllegalArgumentException("</tag expected");
      inp.setLengthMax();  //for next parsing
      if(!inp.scan(">").scanOk())  throw new IllegalArgumentException("</tag > expected");
      if(contentBuffer !=null && subOutput !=null) { //subOutput is the destination
        if(contentBuffer !=null) {
          assert(attribNames[0] !=null);
          DataAccess.IntegerIx ixO = attribNames[0].get("text");
          if(ixO !=null) { attribValues[ixO.ix] = contentBuffer.toString(); } 
        }
        storeContent(contentBuffer, subCfgNode, subOutput, attribNames, attribValues);
      }
    } else {
      throw new IllegalArgumentException("either \">\" or \"/>\" expected");
    }
    inp.setLengthMax();  //for next parsing
    if(this.xmlTestWriter !=null) {
      this.xmlTestWriter.writeElementEnd();
    }
  }










  /**
   * @param inp
   * @param tag
   * @param output
   * @param cfgNode
   * @param attribNames Reference to a map with key,name for attribute values. 
   *        The key is given in the config file, cfgNode.({@link XmlCfg.XmlCfgNode#attribs}) contains the association 
   *        it is not the attribute name anyway, but often the attribute name if the config file determines that. 
   * @return null then do not use this element because faulty attribute values. "" then no special key, length>0: repeat search config.
   * @throws Exception
   */
  private CharSequence parseAttributes(StringPartScan inp, CharSequence tag, XmlCfg.XmlCfgNode cfgNode
      , List<AttribToStore>[] attribsToStore, Map<String, DataAccess.IntegerIx>[] attribNames, String[] attribValues) 
  throws Exception
  { CharSequence keyret = tag; //no special key. use element.
    StringBuilder keyretBuffer = null;
    //read all attributes. NOTE: read formally from text even if bUseElement = false.
    while(inp.scanIdentifier(null, "-:").scan("=").scanOk()) {  //an attribute found:
      final CharSequence sAttrNsNameRaw = inp.getLastScannedString();
      if(!inp.scanQuotion("\"", "\"", null).scanOk()) throw new IllegalArgumentException("attr value expected");
      if(cfgNode !=null) {
        String sAttrValue = replaceSpecialCharsInText(inp.getLastScannedString()).toString();  //"value" in quotation
        if(this.xmlTestWriter !=null) {
          this.xmlTestWriter.writeAttribute(sAttrNsNameRaw.toString(), sAttrValue);
        }
        if(sAttrNsNameRaw.equals("xmlinput:class"))
          Debugutil.stop();
        int posNs = StringFunctions.indexOf(sAttrNsNameRaw, ':');  //namespace check
        final CharSequence sAttrNsName;
        if(posNs >=0) {
          //Namespace
          CharSequence ns = sAttrNsNameRaw.subSequence(0, posNs);
          final CharSequence sAttrName = sAttrNsNameRaw.subSequence(posNs+1, sAttrNsNameRaw.length());
          //String nsName = sAttrName.toString();
          if(StringFunctions.equals(ns, "xmlns")){
            String nsValue = sAttrValue.toString();
            this.namespaces.put(sAttrName.toString(), nsValue);
            sAttrNsName = null;
          } else {
            String nsValue = this.namespaces.get(ns);  //defined in this read xml file.
            if(nsValue == null) {
              sAttrNsName = null;  //Namespace not registered in the input file, especially "xml".
            } else if(cfgNode.cfg.xmlnsAssign !=null) {
              String nsCfg = cfgNode.cfg.xmlnsAssign.get(nsValue);
              //Todo search defined name in cfg for the nameSpace.
              if(nsCfg == null) { 
                sAttrNsName = null;  //Namespace not in cfg registered. 
              } else {
                sAttrNsName = nsCfg + ":" + sAttrName; //nameSpace alias from cfg file. Unified.
              }
            } else {
              //read the config file, here the xmlnsAssign is null, use the attribute name as given.
              sAttrNsName = sAttrNsNameRaw;
            }
          }
        }
        else {
          sAttrNsName = sAttrNsNameRaw;
          
        }
        if(sAttrNsName !=null) {
          XmlCfg.AttribDstCheck cfgAttrib = null;
          if(cfgNode.attribs != null) { 
            cfgAttrib= cfgNode.attribs.get(sAttrNsName);
            if(cfgAttrib == null) {
              cfgAttrib= cfgNode.attribs.get("?");  //for all attributes
            }
          }
          if(cfgAttrib != null) {
            if(cfgAttrib.bUseForCheck) {
              if(keyretBuffer == null) { keyretBuffer = new StringBuilder(64); keyretBuffer.append(tag); keyret = keyretBuffer; }
              keyretBuffer.append("@").append(sAttrNsName).append("=\"").append(sAttrValue).append("\"");
            }
            else if(cfgAttrib.daccess !=null) { //store the dataaccess to eval if the element is created.
              if(attribsToStore[0]==null) {attribsToStore[0] = new LinkedList<AttribToStore>(); }
              attribsToStore[0].add(new AttribToStore(cfgAttrib.daccess, sAttrNsName.toString(), sAttrValue));
            } else if(cfgAttrib.storeInMap !=null) {
//              if(attribNames[0] == null){ 
//                attribNames[0] = new TreeMap<String, DataAccess.IntegerIx>(); 
//                attribValues[0] = new LinkedList<String>();
//              }
              ////
              DataAccess.IntegerIx ixO = attribNames[0].get(cfgAttrib.storeInMap);
              if(ixO !=null) {
                attribValues[ixO.ix] = sAttrValue;
              } else {
                Debugutil.stop(); //not used attribute
              }
            }
          } else {
            if(cfgNode.attribsUnspec !=null) { //it is especially to read the config file itself.
              if(attribsToStore[0]==null) {attribsToStore[0] = new LinkedList<AttribToStore>(); }
              attribsToStore[0].add(new AttribToStore(cfgNode.attribsUnspec, sAttrNsName.toString(), sAttrValue));
            }
          }
        }
      }
      inp.readNextContent(this.sizeBuffer/2);
    } //while
    return keyret;
  }





  /**Invokes the associated method to get/create the appropriate data instance for the element.
   * @param output The current output in the parent context
   * @param elementStorePath if null, returns output. Else: The storePath in the {@link XmlCfg.XmlCfgNode}
   * @param attribValues
   * @param subCfgNode The cfgNode for the found element
   * @param sTag it is the keyResearch maybe assembled with attribute values, not only the tag name.
   * @return output if elementStorePath is null, either result of an invoked method via {@link DataAccess} or accessed data
   * @throws Exception
   */
  Object getDataForTheElement( Object output, DataAccess.DatapathElement elementStorePath, String[] attribValues) 
  {
    Object subOutput;
    if(elementStorePath == null) { //no attribute xmlinput.data="pathNewElement" is given:
      subOutput = output; //use the same output. No new element in data.
    }
    else {  //invoke an routine which gets the new output. The routine may use arguments from attributes.
      try{ 
//        if(subCfgNode.elementStorePath.ident().equals("new_EventInput"))
//          Debugutil.stop();
        if(elementStorePath.isOperation()) {
          subOutput = DataAccess.invokeMethod(elementStorePath, null, output, true, attribValues, false);
        } else {
          //it may be a method too but without textual parameter.
          subOutput = DataAccess.access(elementStorePath, output, true, false, null, attribValues, false, null);
        }
      } catch(Exception exc) {
        subOutput = null;
        CharSequence sError = Assert.exceptionInfo("", exc, 1, 30);
        System.err.println("error getDataForTheElement: " + elementStorePath);
        System.err.println("help: ");
        System.err.println(sError);
      }
    }
    return subOutput;
  }



  /**Invokes the associated method to store the attribute value.
   * <ul>
   * <li> This method is invoked while reading the cfg.xml to store the attribute value with XmlNode#addAttributeStorePath().
   * <li> This method is invoked while reading the user.xml to store the users attribute value in the users data.
   * </ul>
   * @param output It is an instance of {@link XmlCfgNode} while reading the cfg.xml, it is an user instance while reading the user.xml
   * @param dstPath The dataAccess which should be executed.
   * @param sAttrNsName
   * @param sAttrValue
   */
  @SuppressWarnings("static-method")
  void storeAttrData( Object output, DataAccess.DatapathElement dstPath, Map<String, DataAccess.IntegerIx> attribNames, CharSequence sAttrName, CharSequence sAttrValue) 
  {
    try{ 
      if(dstPath.isOperation()) {
        String[] vars = null; 
        if(attribNames !=null) {
          vars = new String[attribNames.size()];
          for(Map.Entry<String, DataAccess.IntegerIx> earg: attribNames.entrySet()) {
            String argName = earg.getKey();
            int ix = earg.getValue().ix;       //supply the three expected arguments, other are not possible.
            if(argName.equals("name")) { vars[ix] = sAttrName.toString(); }
            else if(argName.equals("value")) { vars[ix] = sAttrValue.toString(); }
          }
        }
        DataAccess.invokeMethod(dstPath, null, output, true, vars, true);
        //DataAccess.invokeMethod(dstPath, null, output, true, false, args);
      } else {
        DataAccess.storeValue(dstPath, output, sAttrValue, true);
      }
    } catch(Exception exc) {
      System.err.println("error storeAttrData: " + exc.getMessage());
    }
  }




//  @SuppressWarnings({ "static-method", "unused" })
//  private void setOutputAttr(Object output, DataAccess.DatapathElement dstPath, CharSequence name, CharSequence value)
//  {
//    if(dstPath.equals("@") && output instanceof Map) {
//      @SuppressWarnings("unchecked")
//      Map<String, Object> oMap = (Map<String, Object>)output;
//      oMap.put("@"+ name, value.toString());
//    }
//  }



  /**Reads the textual content of an element.
   * leading and trailing whitespaces are not part of the content.
   * If the content contains a hard coded line feed with &amp;#xa; etc. all textual line feeds and following indents are removed from the text.
   * See {@link #replaceSpecialCharsInText(CharSequence)} 
   * @param inp
   * @param buffer maybe null then ignore content.
   * @throws ParseException 
   */
  private CharSequence parseContent(StringPartScan inp, StringBuilder buffer)
  throws IOException, ParseException
  { boolean bContReadContent;
    //int posAmp = buffer == null ? 0 : buffer.length()-1; //NOTE: possible text between elements, append, start from current length.
    inp.seekNoWhitespace();
    boolean bEofSupposed = false;
    CharSequence content = null;
    do { //maybe read a long content in more as one portions.
      inp.lento('<');  //The next "<" is either the end with </tag) or a nested element.
      bContReadContent = !inp.found();
      if(bContReadContent) {
        if(bEofSupposed) {
          throw new IllegalArgumentException("Format error in XML file, missing \"<\", file: " + inp.getInputfile());
        }
        inp.setLengthMax();
      } else {
        inp.lenBacktoNoWhiteSpaces();
      }
      CharSequence content2 = replaceSpecialCharsInText(inp.getCurrentPart());
      if(this.xmlTestWriter !=null) {
        this.xmlTestWriter.writeText(content2, false);
      }
      inp.fromEnd();
      if(buffer !=null && buffer.length() > 0) { 
        //any content already stored, insert a space between the content parts.
        buffer.append(' ');
      }
      if(buffer !=null) { 
        buffer.append(content2); //old version, with buffer from out
        content = buffer;
      } else {
        if(content ==null) { 
          content = content2; //the read content immediately
        } else { //already read something before
          if(!(content instanceof StringBuilder)) {
            content = new StringBuilder(content);
          }
          ((StringBuilder)content).append(content2);
        } 
      }
      bEofSupposed = inp.readNextContent(this.sizeBuffer/2);
    } while(bContReadContent);
    return content;
  }
  
  
  
  private static void storeContent(StringBuilder buffer, XmlCfg.XmlCfgNode cfgNode, Object output, Map<String, DataAccess.IntegerIx>[] attribs, String[] attribValues) {
    DataAccess.DatapathElement dstPath = cfgNode.contentStorePath;
    if(dstPath !=null) {
      try{ 
        if(dstPath.isOperation()) {
          //String[] vars = null; 
          if(cfgNode.allArgNames !=null) {
            //vars = new String[cfgNode.allArgNames.size()];
            for(Map.Entry<String, DataAccess.IntegerIx> earg: cfgNode.allArgNames.entrySet()) {
              String argName = earg.getKey();
              int ix = earg.getValue().ix;       //supply the three expected arguments, other are not possible.
              if(argName.equals("text") && buffer !=null) { 
                attribValues[ix] = buffer.toString(); 
              }
            }
          }
          DataAccess.invokeMethod(dstPath, null, output, true, attribValues, true);
          //DataAccess.invokeMethod(dstPath, null, output, true, false, args);
        } else if(buffer !=null) {
          DataAccess.storeValue(dstPath, output, buffer, true);
        }
      } catch(Exception exc) {
        System.err.println("error storeContent: " + exc.getMessage());
      }
    }
  }

  
  
  
  
  /**Replaces the basic sequences &amp;lt; etc. and the UTF 16 &lt;#xaaaa;
   * and replaces line feed with indent with one space.
   * Before call of this routine the leading and trailing white spaces are removed alread. 
   * @param src
   * @return src if no such sequences found
   * @throws ParseException
   */
  private CharSequence replaceSpecialCharsInText(CharSequence src) throws ParseException {
    //Note: older form, since 2020-01: do not convert \n if no &#xa is contained:
    if(StringFunctions.contains(src, "Comment of Network1"))
      Debugutil.stop();
    //commented: if(StringFunctions.indexOfAnyChar(src, 0, -1, "&\n") < 0) { return src; }
    if(StringFunctions.indexOf(src, '&') < 0) { return src; } //unchange, no effort
    else {
      StringBuilder b = new StringBuilder(src); //need, at least one & is to be replaced.
//      boolean bLinefeedfound = false;
      int pos = 0;
      while( ( pos  = StringFunctions.indexOf(b, '&', pos)) >=0) {
        int posEnd = StringFunctions.indexOf(b, ';', pos);
        if(posEnd >0 && (posEnd - pos) < 11) {
          String search = b.subSequence(pos, posEnd+1).toString();
          String replChar = this.replaceChars.get(search);
          if(replChar == null) {
            if( search.charAt(1) == '#') {
              int radix =10, posInt = 2;
              if(search.charAt(posInt) == 'x') {
                radix = 16; posInt +=1;
              }
              int nUTF;
              try{ nUTF = Integer.parseUnsignedInt(search.substring(posInt, posEnd-pos), radix); }
              catch(NumberFormatException exc) {
                nUTF = 0x3f;
                assert(false);
              }
              posEnd +=1; //now after ';'
              if(nUTF == 0x0a) { 
                //other variant used: bLinefeedfound = true;
                //because of a hard coded line feed is detected, skip over an textual linefeed and maybe indents:
                //Note: All real requested indentation chars should be hard coded true, with &#x20; etc. 
                int zb = b.length();
                while( posEnd < zb && "\n\r \t".indexOf(b.charAt(posEnd)) >=0) {
                  posEnd +=1; //skip over any whitespace after &#xa; and following &#x20;
                }
              }
              char[] replChars = Character.toChars(nUTF);
              b.delete(pos, posEnd);
              b.insert(pos, replChars);
              pos += replChars.length;
            }
            else {
              //Problem: unrecognized replacing:
              //do nothing yet.
            }
          }
          else if(replChar !=null) {
            if(replChar.equals("&"))
              Debugutil.stop();
            b.replace(pos, posEnd+1, replChar);
            pos +=1;
          }
        } else {
          throw new ParseException("faulty Characters on ", pos);
        }
      }
      pos = 0;
//      if(bLinefeedfound) {
        //replace line feed and following indent with one space only if the text contains any line feed with &#xa; or &#10;
        //It means if the XML-source uses a hard coded line feed a textual linefeed is used as whiteSpace.
        //If the text contains hard coded &#xa; it should contain hard coded indents too (&#x20; or #x32;).
        //If the text does not contain hard coded line feed the original content for line feed and spaces should be preserved.
        //What does the w3c says: It is not clearly.
//Other variant used, skip over \r\n indent after hard coded line feed.        
//        while( ( pos  = StringFunctions.indexOfAnyChar(b, pos, -1, "\n\r")) >=0) {
//          int posEnd = pos +1;
//          while(posEnd < b.length() &&  " \t\n\r".indexOf(b.charAt(posEnd)) >=0) { posEnd +=1; } //skip over all chars till next line content
//          b.replace(pos, posEnd, " ");  //remove all indent white spaces after line break. Replace line break with indent by 1 space. 
//        }
//      }
      return b;
    }
  }
  
  
  
  public XmlCfg readCfg(File file) throws IOException {
    readXml(file, this.cfg.rootNode, this.cfgCfg);
    this.cfg.finishReadCfg(this.namespaces);
    return this.cfg;
  }



  /**Read from a resource (file inside jar archive).
   * @param clazz A class in any jar, from there the relative path to the pathInJar is built.
   *   Usually the clazz should be the output data clazz. 
   * @param pathInJar relative Path from clazz. 
   *   Usually the cfg should be in the same directory as the output data class. Then this is only the file name.
   * @throws IOException
   */
  public XmlCfg readCfgFromJar(Class<?> clazz, String pathInJarFromClazz) throws IOException {
    String pathMsg = "jar:" + pathInJarFromClazz;
    //ClassLoader classLoader = ClassLoader.getSystemClassLoader();
    //classLoader.getResource("slx.cfg.xml");
    InputStream xmlCfgStream = clazz.getResourceAsStream(pathInJarFromClazz);
    if(xmlCfgStream == null) throw new FileNotFoundException(pathMsg);
    readXml(xmlCfgStream, pathMsg, this.cfg.rootNode, this.cfgCfg);
    xmlCfgStream.close();
    this.cfg.finishReadCfg(this.namespaces);
    return this.cfg;

  }


  
  

  public String readXml(File file, Object dst) throws IOException {
    return this.readXml(file, dst, this.cfg);
  }  
    
  
  
  public String readXml(InputStream stream, String sInputPath, Object dst) {
    return readXml(stream, sInputPath, dst, this.cfg);
  }
  
  public String readXml(Reader stream, String sInputPath, Object dst) {
    return readXml(stream, sInputPath, dst, this.cfg);
  }
  
  
  static class AttribToStore {
    /**The data access to store the value.*/
    final DataAccess.DatapathElement daccess;
    final String name, value;
    AttribToStore(DataAccess.DatapathElement daccess, String name, String value){
      this.daccess = daccess;
      this.name = name;
      this.value = value;
    }
  }


}
