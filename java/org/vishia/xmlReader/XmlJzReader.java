package org.vishia.xmlReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Field;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.vishia.msgDispatch.LogMessage;
import org.vishia.util.CheckVs;
import org.vishia.util.DataAccess;
import org.vishia.util.Debugutil;
import org.vishia.util.ExcUtil;
import org.vishia.util.FileSystem;
//import org.vishia.util.IndexMultiTable;
import org.vishia.util.StringFunctions;
import org.vishia.util.StringFunctions_B;
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
   * <li>2024-06-07: Important fix: An simple space after > but before \n or before < is also content! Seen in LibreOffice.odt content.xml 
   * <li>2024-05-27: Meaningful enhancement: Now it writes also without config in an {@link XmlAddData_ifc} instance, if given as data on {@link #readXml(File, Object)} etc.
   *   The config is only necessary for the namespace declaration, because the namespace alias are not anytime the same in a read XML.
   *   The {@link #bUseNonSemanticDataStore} is set true if detected and a dummy config {@link #cfgNodeNonSemanticDataStore} 
   *   is used instead not found config nodes on the elements.
   *   Writing to the output is always done via the operations of {@link XmlAddData_ifc} if {@link #bUseNonSemanticDataStore} is set.
   * <li>2024-05-23: {@link #parseElement(StringPartScan, Object, org.vishia.xmlReader.XmlCfg.XmlCfgNode)} refactored.
   *   Now it uses consequently the subtree if given. See documentation in LibreOffice. 
   * <li>2024-05-22: New {@link #readCfgTxtFromJar(Class, String)}
   * <li>2023-05-23: Now all text content parts are stored immediately after its parsing, and NOT as a whole. 
   *   The last one is faulty if the text is mixed with some more order relevant content. Especially formatted text.
   *   This is detected and used while parsing Libre Office files (odg). 
   * <li>2023-05-23: same as newline characters now also in {@link #parseContent(StringPartScan, StringBuilder)} 
   * <li>2023-05-02: chg: newline characters after and before text: Then all white spaces after and before text are removed.
   *   If a newline char does not appear, nothing is removed. spaces on start or end are admissible.
   *   Then the XML file should not do beautification and should not contain newline characters there.
   *   If a newline character is necessary in text content, the &#x0a; etc. should be used.
   *   Changed in {@link #storeContent(StringBuilder, org.vishia.xmlReader.XmlCfg.XmlCfgNode, Object, Map[], String[])}.
   * <li>2022-06-25: chg: in {@link #storeContent(StringBuilder, org.vishia.xmlReader.XmlCfg.XmlCfgNode, Object, Map[], String[])}:
   *   the {@link DataAccess#invokeMethod(org.vishia.util.DataAccess.DatapathElement, Class, Object, boolean, Object[], boolean)} 
   *   was called with true for non exception, that is faulty. To detect writing errors in the xmlcfg file, exceptions are necessary. 
   * <li>2022-06-23: "xmlinput:finish" regarded, see {@link #finishElement(Object, Object, org.vishia.util.DataAccess.DatapathElement)} 
   * <li>2022-06-06: new {@link #setNamespaceEntry(String, String)}
   * <li>2021-12-16 documentation and fine tuning of storeAttrData(..). Now usage of value is not tested. 
   *   If the operation for an attribute in cfg returns a #{@link java.lang.reflect.Field} then the value is stored in this return field
   *   independent whether it is used also as argument. This can be helpfully and it is a special condition lesser.
   * <li>2021-10-18 new: in {@link #storeAttrData(StringPartScan, Object, org.vishia.util.DataAccess.DatapathElement, Map, CharSequence, CharSequence, String)}
   *   In the Xmlcfg file now you can use <code>"tag"</code> as attribute value for a <code>xmlinput:data="!CREATE_OPER(..., tag, ...)</code> now. 
   *   Additionally: If the <code>attr="!OPERATION(?)"</code> does not use "value" as argument 
   *   and it returns a java.lang.reflect.Field then this field is used to store the attribute value, as also for a simple expression.
   * <li>2021-10-18 improved: The {@link #readCfg(File)} and {@link #readCfgFromJar(Class, String)} produces now a new instance of {@link XmlCfg} data.
   *   Before the read cfg data are written in the given instance of {@link #cfg} which means, immediately able to use,
   *   but the older instance was destroyed. This new version is compatible with the old one (both activates updates the {@link #cfg}),
   *   but now the reference to a older read config remain valid and can be reused for the now public {@link #readXml(File, Object, XmlCfg)}.
   *   It means some {@link XmlCfg} can be present and use for different reading approaches with the same XmlJzReader. 
   *   You can switch between config. This change was done primary because the older implementation was not really good able to document.
   *   The older approach was firstly done to save memory space (prevent new instances). 
   *   But this is not true because the Garbage Collector does its work if necessary.  
   * <li>2021-10-07 new {@link #storeAttrData(StringPartScan, Object, org.vishia.util.DataAccess.DatapathElement, Map, CharSequence, CharSequence, String)}
   *   gets inp as info only for a better error msg.
   * <li>2021-10-07 new in {@link #parseElement(StringPartScan, Object, org.vishia.xmlReader.XmlCfg.XmlCfgNode)}
   *   stores also a namespace definition in user data if required. See cfg.
   * <li>2021-10-07 new {@link #errMsg(int, StringPartScan, CharSequence...)} as central routine if problems occur. 
   * <li>2021-10-07 formally change: General using TreeMap (java.util) instead the specific IndexMultiTable, same functionality 
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
  public static final String version = "2024-05-23";
  
  
  /**To store the last used configuration, for parsing with the same config. */
  XmlCfg cfg;
  
  
  /**Configuration to read a config file. */
  final XmlCfg cfgCfg;
  
  private boolean bUseNonSemanticDataStore; 
  
  
  private XmlCfg.XmlCfgNode cfgNodeNonSemanticDataStore;
  
  /**Size of the buffer to hold a part of the xml input file. 
   * It should be enough large to hold 1 element with attributes (without content). 
   * On 2/3 of the buffer it is reloaded. It means the most length of an attribute should be < sizeBuffer/3.
   * Note that sometimes attributes are very long. 
   * Maximum size is 16 MByte. 
   */
  int sizeBuffer = 200000;
  
  /**It is able to set with {@link #setDebugStop(int)} */
  private int debugStopLine = -1;
  
  /**It is able to set with {@link #setDebugStopTag(String)}*/
  private String debugTag = null;
  
  /**Assignment between nameSpace-alias and nameSpace-value gotten from the xmlns:ns="value" declaration in the read XML file. */
  Map<String, String> namespaces = new TreeMap/*IndexMultiTable*/<String, String>(/*IndexMultiTable.providerString*/);
   
   
  private final Map<String, String> replaceChars = new TreeMap<String, String>();

   
  
  XmlSequWriter xmlTestWriter;
  
  protected final LogMessage log;

  
  public XmlJzReader() {
    this(null);
  }
  
  public XmlJzReader(LogMessage log) {
    this.log = log;
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

  
  
  public void setDebugStopTag(String stag) {
    this.debugTag = stag;
  }
   
  
  
  /**Writes the parsed content without processing in this file. 
   * The writing regards indents for any node, so that a beautification is gotten for the content. 
   * That is the important application of this functionality.
   * The call of this operation opens the {@link #xmlTestWriter}.
   * It is closed on end of JZtxtReader. It means this operation should be called 
   * immediately before call of {@link #readXml(File, Object)} or also {@link #readCfg(File)}
   * and all its abbreviations. It should not be called without a read Xml invocation
   * because otherwise the resource remains open.
   * @param fout File to write
   * @throws IOException if open fails
   */
  public void openXmlTestOut(File fout) throws IOException {
    if(this.xmlTestWriter == null) { this.xmlTestWriter = new XmlSequWriter(); }
    this.xmlTestWriter.open(fout, "UTF-8", null);
  }
  
  
  /**This operation is only necessary if a name space declaration is missing
   * in the xml file to read. It should be called before reading. 
   * @param key
   * @param url
   */
  public void setNamespaceEntry(String key, String url) {
    this.namespaces.put(key, url);
  }
  
  

  /**Reads an xml file with a given config. 
   * It does not change the stored config which is gotten by {@link #readCfg(File)} or {@link #readCfgFromJar(String)}.
   * This operation is used internally in for all read operations too. It is the common entry to read.
   * @param input The xml file
   * @param output The empty data where the first operation is called or the first data are stored via reflection.
   * @param xmlCfg Already read XmlCfg from a file using {{@link #readCfg(File)}} or {@link #readCfgFromJar(Class, String)}
   *   or alternatively a immediate prepared XmlCfg. 
   *   <ul>
   *   <li>For reading the config file itself it is {@link XmlCfg#newCfgCfg()}.
   *   <li>For storing to a XmlNodeSimple it is 
   *   <li>Also a user can prepare a XmlCfg by himself.
   *   </ul>
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
   * @return null or an error message if any exception. It is the {@link Exception#getMessage()}.
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
   * @param inp A StringPartScan can be built from a String too, using {@link StringPartScan#assign(CharSequence)}.
   *   for file reading the constructor {@link StringPartFromFileLines#StringPartFromFileLines(File)} is used.
   * @param output the data to store the read content. Should match to the root instance in the cfg file.
   * @param xmlCfg The used configuration (it does not access the {@link #cfg} if the instance, may call with {@link #cfg}.
   * @throws Exception
   */
  public void readXml(StringPartScan inp, Object output, XmlCfg xmlCfg) 
  throws Exception { 
    inp.setIgnoreWhitespaces(true);
    this.bUseNonSemanticDataStore = output instanceof XmlAddData_ifc;
    if(this.bUseNonSemanticDataStore) {
      this.cfgNodeNonSemanticDataStore = new XmlCfg.XmlCfgNode(null, this.cfg, "::NonSemanticNode");
    }
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
          parseElement(inp, output, xmlCfg.rootNode, 100);  //the only one root element.
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
   * <ul>
   * <li>First the tag is parsed, then all attributes using {@link #parseAttributes(StringPartScan, String, org.vishia.xmlReader.XmlCfg.XmlCfgNode, List[], List[])}.
   *   This parses stores only the values of the attributes in , and evaluate the CHECK attributes.
   *   The attributes are stored in a List<AttribToStore> attribsToStore and namespacesToStore.
   * <li>Then the correct sub node in cfg is searched, maybe depending on attribute CHECK values.
   * <li>With the cfgSubNode it is detected whether a SUBTREE node should be used.
   * <li>All attributes which are stored in attribsToStore are now completed with the maybe SUBTREE given information.
   *   Done in {@link #getCfgAttrib(CharSequence, org.vishia.xmlReader.XmlCfg.XmlCfgNode)}
   *   Not (old solution) with the immediately subCfgNode.
   *   But the attribute values are not stored till now, because the output instance is not given.
   * <li>The output instance for this new node is gotten via the NEW:dataAccess either given in the non-SUBTREE subCfgNode,
   *   it this is given, if wins. Or the NEW:dataAccess in the maybe given SUBTREE subCfgNode is used.
   *   Whereby to get the output instance the stored attribute values may be used.
   *   
   * <li>Then the attribut values are stored in the new given output instance which have an {@link AttribToStore#daccess} path.
   *   this is done in {@link #storeAttributesDueToSubCfgNode(org.vishia.xmlReader.XmlCfg.XmlCfgNode, List, List, Map, String[])}
   *   {@link #storeAttrData(StringPartScan, Object, org.vishia.util.DataAccess.DatapathElement, Map, CharSequence, CharSequence, String)}   
   * <li>Then this operation is called recursively if more elements are found.   
   * </ul>  
   * @param inp scanOk-Position after the "<" before the identifier.
   * @param output instance to store the parsed data.
   * @param cfgNode The config for this element
   * @throws Exception 
   */
  private void parseElement(StringPartScan inp, Object output, XmlCfg.XmlCfgNode cfgNode, int recursion) 
  throws Exception
  { 
    if(recursion <=0) {
      throw new IllegalArgumentException("too many recursions in XML node");
    }
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
    //--------------------------------------------- The tag name of the element:
    String sTag = inp.getLastScannedString().toString();
    if(this.debugTag !=null && sTag.equals(this.debugTag)) {
      Debugutil.stop();
    }
    if(this.xmlTestWriter !=null) {
      this.xmlTestWriter.writeElement(sTag);
    }
    
    if(sTag.contains("   "))
      Debugutil.stop();
    if(sTag.equals("Object"))
      Debugutil.stop();
    //TODO replace alias.
    //
    //--------------------------------------------- Search the tag name in the subnode in current cfg:
    //
    XmlCfg.XmlCfgNode subCfgNode;
    if(this.bUseNonSemanticDataStore) {
      subCfgNode = this.cfgNodeNonSemanticDataStore;
    } else if (cfgNode == null) {   //check whether the parent element should be regarded:
      subCfgNode = null;
    } else {  //----------------------------------- The parent element is expected, content should be stored
      if(output ==null) {
        Debugutil.stop();
      }
      CheckVs.check(output !=null);
      if(sTag.toString().contains("   "))
        Debugutil.stop();
      if(sTag.toString().startsWith("text:p")
        && inp.getCurrentPart(38).toString().startsWith(" text:style-name=\"P268\"")
          )
        Debugutil.stop();
      if(cfgNode.subnodes == null) {             // inner content, it is this element should not be stored.
        subCfgNode = null; //don't read inner content
      } else {  
        subCfgNode = cfgNode.subnodes.get(sTag); //search the proper cfgNode for this <tag
        if(subCfgNode == null) {
          subCfgNode = cfgNode.subnodes.get("?");//check whether it is a node with any unspecified tag name possible. 
        }
      }
      if(sTag.toString().contains("   "))
        Debugutil.stop();
    }
    //--------------------------------------------- The subCfgNode for the element is either null or found.
    //                                              This is the common subCfgNode if attributes determine the subCfgNode.
    //                                              This common subCfgNode contains the attributes to check.
    @SuppressWarnings("unchecked")
    Map<String, DataAccess.IntegerIx> attribNames = null;
    //@SuppressWarnings("unchecked")
    String[] attribValues = null;
    @SuppressWarnings("unchecked")
    List<AttribToStore>[] attribsToStore = new List[1];
    List<AttribToStore>[] nameSpacesToStore = new List[1];
    //
    //For attribute evaluation, use the subCfgNode gotten from sTag. It may be necessary to change the subCfgNode after them. 
    //
    if(dbgline == this.debugStopLine)
      Debugutil.stop();
    //Hint: The element (node) where the attributes should be associated is not created.
    //output is currently the parent node. Hence store attributes firstly locally. Do not offer output as argument.
    String keyResearch = parseAttributes(inp, sTag, subCfgNode, attribsToStore, nameSpacesToStore);
    //
    if(keyResearch.length() > sTag.length()) {
      //Search the appropriate cfg node with the qualified keySearch, elsewhere subCfgNode is correct with the sTag as key. 
      subCfgNode = cfgNode.subnodes == null ? null : cfgNode.subnodes.get(keyResearch);  //search the proper cfgNode for this <tag //bugfix .toString() 2023-09-18
    }
    //============================================= Search a possible SUBTREE entry for the subCfgNode
    //============================================= and determine the store paths either from subCfgNode or from the SUBTREE
    final DataAccess.DatapathElement elementStorePath, elementFinishPath, contentStorePath, nameSpaceDef;
    if(subCfgNode !=null) {
      if(subCfgNode.cfgSubtreeName !=null) {
        XmlCfg.XmlCfgNode subCfgNodeSubtree = null;
        subCfgNodeSubtree = this.cfg.subtrees.get(subCfgNode.cfgSubtreeName);
        elementStorePath = subCfgNode.elementStorePath !=null ? subCfgNode.elementStorePath : subCfgNodeSubtree.elementStorePath;
        elementFinishPath = subCfgNode.elementFinishPath !=null ? subCfgNode.elementFinishPath : subCfgNodeSubtree.elementFinishPath;
        contentStorePath = subCfgNode.contentStorePath !=null ? subCfgNode.contentStorePath : subCfgNodeSubtree.contentStorePath;
        nameSpaceDef = subCfgNode.nameSpaceDef !=null ? subCfgNode.nameSpaceDef : subCfgNodeSubtree.nameSpaceDef;
        //only test: subCfgNode.cmpNode(subCfgNodeSubtree, this.log);
        subCfgNode = subCfgNodeSubtree;
      } else {  //--------------------------------- not in SUBTREE
        elementStorePath = subCfgNode.elementStorePath;
        elementFinishPath = subCfgNode.elementFinishPath;
        contentStorePath = subCfgNode.contentStorePath;
        nameSpaceDef = subCfgNode.nameSpaceDef;
      }

      attribNames = subCfgNode.allArgNames;   // maybe null if no attribs or text and tag are used.
      if(subCfgNode.allArgNames !=null) {
        attribValues = new String[subCfgNode.allArgNames.size()];
        DataAccess.IntegerIx ixO = subCfgNode.allArgNames.get("tag");
        if(ixO !=null) { attribValues[ixO.ix] = sTag; }
      }
    } else {     //-------------------------------- subCfgNode is not given, do not store anything. 
      elementStorePath = null;
      elementFinishPath = null;
      contentStorePath = null;
      nameSpaceDef = null;
    }
    if(subCfgNode ==null) {
      Debugutil.stop();
    }
    final Object subOutput;
    if(this.bUseNonSemanticDataStore) {
      final Map<String,String> attribs;
      if(attribsToStore[0] ==null) { attribs = null;
      } else {
        attribs = new TreeMap<>();
        for(AttribToStore attrib: attribsToStore[0]) {
          attribs.put(attrib.name, attrib.value);
        }
      }
      subOutput = ((XmlAddData_ifc)output).newNode(sTag, attribs);
    } else {
      if(attribsToStore[0] !=null) {
        storeAttributesDueToSubCfgNode(subCfgNode, attribsToStore[0], nameSpacesToStore[0], subCfgNode.allArgNames, attribValues);
      }
      //The subOutput is determined with the correct subCfgNode, either with keySearch == sTag or a attribute-qualified key:
      subOutput = subCfgNode == null ? null 
                             : elementStorePath == null ? output : getDataForTheElement(output, elementStorePath, attribValues);
      if(cfgNode !=null && cfgNode.subnodes !=null && subCfgNode ==null) {
        Debugutil.stop();       // a node which should not be evaluated
      }
      //
      //store all attributes in the content which are not used as arguments for the new instance (without "!@"):
      if(attribsToStore[0] !=null) { 
        if(subOutput ==null) {
          System.err.print("\nProblem storing attribute values, getDataForTheElement \"" + subCfgNode.elementStorePath + "\" returns null");
        } else {
          for(AttribToStore e: attribsToStore[0]) {
            if(e.daccess !=null) {
              storeAttrData(inp, subOutput, e.daccess, subCfgNode.allArgNames, e.name, e.value, sTag);  //subOutput is the destination to store
            }
      } } }
      if(nameSpacesToStore[0] !=null) { 
        if(subOutput ==null) {
          System.err.print("\nProblem storing attribute values, getDataForTheElement \"" + subCfgNode.elementStorePath + "\" returns null");
        } else {
          for(AttribToStore e: nameSpacesToStore[0]) {
            if(e.daccess !=null) { //only if should be stored. nameSpacesToStore[0] contains all xmlns attributes.
              storeAttrData(inp, subOutput, e.daccess, subCfgNode.allArgNames, e.name, e.value, sTag);  //subOutput is the destination to store
            }
    } } } }
      
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
      //
      //=====================================================
      boolean bEndOfElement = false;
      do { //================================================ Loop to parse content and <subElement...> till </ 
        inp.readNextContent(this.sizeBuffer/2);
        if( inp.getCurrent(2).toString().startsWith(" <"))
          Debugutil.stop();
        char cc = inp.getCurrentChar();
        if("\n\r<".indexOf(cc)<0) {    //-------------------- after> not immediately <tag or newline, 
          StringBuilder contentBuffer = new StringBuilder(100);  // them it is content, maybe only a space or such.
          parseContent(inp, contentBuffer);                // add the content between some tags to the content Buffer.
          if(contentBuffer !=null && subOutput !=null) { //subOutput is the destination
            if(this.bUseNonSemanticDataStore) {
              ((XmlAddData_ifc)subOutput).addText(contentBuffer.toString());
            } else {
              assert(subCfgNode.allArgNames !=null);
              DataAccess.IntegerIx ixO = subCfgNode.allArgNames.get("text");
              if(ixO !=null) { attribValues[ixO.ix] = contentBuffer.toString(); } 
            }
            storeContent(contentBuffer, subCfgNode, subOutput, contentStorePath, subCfgNode.allArgNames, attribValues);
          }
        }
        if(inp.scan("</").scanOk()) {  //==================== "</": tag> end of element.
          bEndOfElement = true;                            // leave the loop.
        }
        else if(inp.scan("<").scanOk()) {  //================ "<": start of a new element. Hint: skips over whitespace with scan(...)
          if(inp.scan("!--").scanOk()) {
            inp.seekEnd("-->");
          }
          else if(inp.scan("![CDATA[").scanOk()) {
            StringBuilder contentBuffer = new StringBuilder(500);
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
            if(contentBuffer !=null && subOutput !=null) { //subOutput is the destination
              if(this.bUseNonSemanticDataStore) {
                ((XmlAddData_ifc)subOutput).addText(contentBuffer.toString());
              } else {
                assert(attribNames !=null);
                assert(attribNames == subCfgNode.allArgNames);
                DataAccess.IntegerIx ixO = attribNames.get("text");
                if(ixO !=null) { attribValues[ixO.ix] = contentBuffer.toString(); } 
              }
              storeContent(contentBuffer, subCfgNode, subOutput, contentStorePath, subCfgNode.allArgNames, attribValues);
            }
          }
          else {
            parseElement(inp, subOutput, subCfgNode, recursion -1);      // nested element.
          }
        } else {}  // go back to loop begin, maybe parse CONTENT
      } while(! bEndOfElement); //inp.scan().scan("<").scan("/").scanOk());
      //=====================================================
      inp.readNextContent(this.sizeBuffer/2);  //============ if reached here, on complex node is parsed, </ is reached.
      if(!inp.scanIdentifier(null, "-:.").scanOk())  throw new IllegalArgumentException("</tag expected");
      inp.setLengthMax();  //for next parsing
      if(!inp.scan(">").scanOk())  throw new IllegalArgumentException("</tag > expected");
    } else {
      int[] colmn = new int[1];
      int line = inp.getLineAndColumn(colmn);
      String sFile = inp.getInputfile();
      throw new IllegalArgumentException("either \">\" or \"/>\" expected, " + " in " + sFile + " @" + line + ":" + colmn[0]);
    }
    if(subOutput !=null) { //-------------------------------- finish also for a <tag.../> simple node, this was missing before 2024-05-25
      finishElement(output, subOutput, elementFinishPath); // cfg: ADD:operation
    }
    inp.setLengthMax();  //for next parsing
    if(this.xmlTestWriter !=null) {
      this.xmlTestWriter.writeElementEnd();
    }
  }










  /**Parses all given Attributes from the XML element. 
   * @param inp
   * @param tag
   * @param cfgNode
   * @param attribsToStore [0] maybe given or may be created. If an attribute is found 
   *          which's {@link XmlCfg.AttribDstCheck#daccess} is set, then the attribute with the daccess is stored in [0]
   *          to write it in the new created node if this was created after this operation.
   * @param namespacesToStore
   * @param attribNames Reference to a map with key,name for attribute values. 
   *        The key is given in the config file, cfgNode.({@link XmlCfg.XmlCfgNode#attribs}) contains the association 
   *        it is not the attribute name anyway, but often the attribute name if the config file determines that. 
   * @param attribValues
   * @return
   * @throws Exception
   */
  private String parseAttributes(StringPartScan inp, String tag, XmlCfg.XmlCfgNode cfgNode
      , List<AttribToStore>[] attribsToStore, List<AttribToStore>[] namespacesToStore
  ) throws Exception { 
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
        String dbgAttrname = null; //"bom:count";  //"xmlinput:class"
        if(dbgAttrname !=null && sAttrNsNameRaw.equals(dbgAttrname)) {
          Debugutil.stop();
        }
        int posNs = StringFunctions.indexOf(sAttrNsNameRaw, ':');  //namespace check
        final CharSequence sAttrNsName;
        if(posNs >=0) {  //================================== ns:... given, replace with real ns definition
          //Namespace
          CharSequence ns = sAttrNsNameRaw.subSequence(0, posNs);
          final CharSequence sAttrName = sAttrNsNameRaw.subSequence(posNs+1, sAttrNsNameRaw.length());
          //String nsName = sAttrName.toString();
          if(StringFunctions.equals(ns, "xmlns")) { //======= xmlns: as specific attribute 
            String nsValue = sAttrValue.toString();        // stored in this.namespaces
            String nsName = sAttrName.toString();          // and also returned in namespacesToStore[0]
            this.namespaces.put(nsName, nsValue);
            if(namespacesToStore[0]==null) {namespacesToStore[0] = new LinkedList<AttribToStore>(); }
            namespacesToStore[0].add(new AttribToStore(cfgNode.nameSpaceDef, nsName, nsValue));
            sAttrNsName = null;
          } else {
            String nsValue = this.namespaces.get(ns.toString());  //defined in this read xml file.
            if(nsValue == null && ns.equals("xml")) {
              nsValue = "http://www.w3.org/1999/xml";  //default for xmlns:xml 
            }
            if(nsValue == null) {
              errMsg(1, inp, "XmlJzReader-Namespace of attribute not found: ", ns);
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
        //----------------------------------------- sAttrNsName is the attribute inclusively the full namespace
        if(sAttrNsName !=null) {
          XmlCfg.AttribDstCheck cfgAttrib = getCfgAttrib(sAttrNsName, cfgNode);
          if(attribsToStore[0]==null && (cfgAttrib !=null && !cfgAttrib.bUseForCheck || cfgNode.attribsUnspec !=null || this.bUseNonSemanticDataStore)) {
            attribsToStore[0] = new LinkedList<AttribToStore>();
          }
          if(cfgAttrib != null) {
            if(cfgAttrib.bUseForCheck) {
              if(keyretBuffer == null) { keyretBuffer = new StringBuilder(64); keyretBuffer.append(tag); }
              keyretBuffer.append("@").append(sAttrNsName).append("=\"").append(sAttrValue).append("\"");
            } else {
              attribsToStore[0].add(new AttribToStore(cfgAttrib.daccess, sAttrNsName.toString(), sAttrValue));
            }
          } 
          else if( this.bUseNonSemanticDataStore           // read all content unspecifically
                || cfgNode.attribsUnspec !=null) {         // it is especially to read the config file itself.
            attribsToStore[0].add(new AttribToStore(cfgNode.attribsUnspec, sAttrNsName.toString(), sAttrValue));
          }

        }
      }
      inp.readNextContent(this.sizeBuffer/2);
    } //while
    return keyretBuffer == null ? tag : keyretBuffer.toString();
  }


  
  private XmlCfg.AttribDstCheck getCfgAttrib ( CharSequence sAttrNsName, XmlCfg.XmlCfgNode cfgNode ) {
    XmlCfg.AttribDstCheck cfgAttrib = null;
    if(cfgNode.attribs != null) { 
      cfgAttrib= cfgNode.attribs.get(sAttrNsName.toString());
      if(cfgAttrib == null) {
        cfgAttrib= cfgNode.attribs.get("?");  //for all attributes
      }
    }
    return cfgAttrib;
  }
  
  
  private void storeAttributesDueToSubCfgNode ( XmlCfg.XmlCfgNode cfgNode
    , List<AttribToStore> attribStore, List<AttribToStore> namespacesStore
    , Map<String, DataAccess.IntegerIx> attribNames, String[] attribValues 
  ) {
    assert(attribNames == cfgNode.allArgNames);
    for(AttribToStore attrib: attribStore) {     // The attributes are all already gathered from XML to the attribStore
      XmlCfg.AttribDstCheck cfgAttrib = getCfgAttrib(attrib.name, cfgNode);
      if(cfgAttrib !=null) {
        if(cfgAttrib.storeInMap !=null) {          // check store the value to use as calling argument for new_Element...(...)
          DataAccess.IntegerIx ixO = attribNames.get(cfgAttrib.storeInMap);
          if(ixO !=null) {
            attribValues[ixO.ix] = attrib.value;             // store the attribute value for further processing on correct index due to attribNames
          } else {
            Debugutil.stop(); //not used attribute
          }
        } else if(cfgAttrib.daccess !=null && attrib.daccess == null) {
          attrib.daccess = cfgAttrib.daccess;      // determine the daccess from found valid subNode only if not given before.
        }
      } 
      else if(cfgNode.attribsUnspec !=null) {
        attrib.daccess = cfgNode.attribsUnspec;            // maybe null, common store function
      }
      else {} // Attribute ignored
    }
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
        CharSequence sError = CheckVs.exceptionInfo("", exc, 1, 30);
        System.err.print("\nerror getDataForTheElement: " + elementStorePath);
        System.err.print("\nhelp: ");
        System.err.println(sError);
      }
    }
    if(subOutput == null)
      Debugutil.stop();
    return subOutput;
  }


  /**This op is called on finishing of parsing a element with its sub content.
   * <ul>
   * <li>The "xmlinput:data" delivers the data storage, but need not insert the data storage for this element in the whole data tree.
   * <li>The "xmlinput:finish" inserts the data storage (via reference, hold stack local till now) in the whole data tree.
   *   Depending from the finish operation the data can be post prepared in any kind.
   *   For ZBNF data storage this is the set... operation. It should be named set_... also here. 
   * <li>If the data does not need to be post-prepared, it is also possible that this operation is not given or empty.
   *   But then the "xmlinput:data" should set the reference in the whole data tree already for the new created data.
   *   That is the approach till now, furthermore supported.
   * </ul>
   * @param parent The element where the content is to insert as sub content.
   * @param element to insert
   * @param elementStorePath rule where to insert: It contains:
   *   <ul>
   *   <li>{@link DataAccess.DatapathElement#ident}  Immediately the name of the variable of operation to call in parent
   *   <li>{@link DataAccess.DatapathElement#args} Arguments, whereas the argument for finish should be usual "value", 
   *     which is the [1] element in the immediately argument list
   *     for the internal called {@link DataAccess#invokeMethod(org.vishia.util.DataAccess.DatapathElement, Class, Object, boolean, Object[], boolean)} 
   *     for the "varValues".
   *   </ul>  
   * 
   */
  void finishElement( Object parent, Object element, DataAccess.DatapathElement elementStorePath) {
    if(elementStorePath !=null) {
      try{ 
        Object[] args = new Object[2];
        args[1] = element;                                 // use position of "value" in the common #allArgs field
        if(elementStorePath.isOperation()) {
          DataAccess.invokeMethod(elementStorePath, null, parent, true, args, false);
        } else {
          //it may be a method too but without textual parameter.
          //subOutput = DataAccess.access(elementStorePath, output, true, false, null, attribValues, false, null);
        }
      } catch(Exception exc) {
        CharSequence sError = CheckVs.exceptionInfo("", exc, 1, 30);
        System.err.print("\nerror finishElement: " + elementStorePath);
        System.err.print("help: ");
        System.err.println(sError);
      }
    } else if(this.bUseNonSemanticDataStore) {
      ((XmlAddData_ifc)parent).addNode((XmlAddData_ifc)element);
    }
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
  private void storeAttrData( StringPartScan inp, Object output, DataAccess.DatapathElement dstPath, Map<String, DataAccess.IntegerIx> attribNames, CharSequence sAttrName, CharSequence sAttrValue, String sTag) 
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
            else if(argName.equals("tag")) { vars[ix] = sTag; }
          }
        }
        Object dst = DataAccess.invokeMethod(dstPath, null, output, true, vars, true);
        if(dst !=null) { //TODO test it with String and Integer etc.
          if(dst instanceof Field) {
            ((Field)dst).set(output, sAttrValue);
          }
        }
        //DataAccess.invokeMethod(dstPath, null, output, true, false, args);
      } else {
        DataAccess.storeValue(dstPath, output, sAttrValue, true);
      }
    } catch(Exception exc) {
      errMsg(1, inp, "error storeAttrData: ", exc.getMessage());
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
    //?? inp.seekNoWhitespace();
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
//      } else {
//        inp.lenBacktoNoWhiteSpaces();   // Note: if no LF is contained, do not remove spaces
      }
      CharSequence csText = inp.getCurrentPart();
      if(csText.charAt(0)== ' ')
        Debugutil.stop();
      int posLf = StringFunctions.indexOf(csText, '\n');
      if(posLf>0) {                                        // The read text contains \n, then spaces should be removed
        csText = StringFunctions_B.removeIndentReplaceNewline(csText, 999, " \t", 8, "\r\n", false);
      }
      CharSequence content2 = replaceSpecialCharsInText(csText);
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
    inp.scanStart();  // scan newly after reading content 
    return content;
  }
  
  
  
  private static void storeContent(StringBuilder buffer, XmlCfg.XmlCfgNode cfgNode, Object output
  , DataAccess.DatapathElement contentStorePath
  , Map<String, DataAccess.IntegerIx> attribs, String[] attribValues
  ) {
    if(contentStorePath !=null) {
//      if(dstPath.ident().equals("set_text") && output instanceof org.vishia.odg.data.XmlForOdg_Zbnf.Text_span_Zbnf)
//        Debugutil.stop();
      try{ 
        if(contentStorePath.isOperation()) {
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
          DataAccess.invokeMethod(contentStorePath, null, output, true, attribValues, false);
          //DataAccess.invokeMethod(dstPath, null, output, true, false, args);
        } else if(buffer !=null) {
          DataAccess.storeValue(contentStorePath, output, buffer, true);
        }
      } catch(Exception exc) {
        CharSequence sExc = ExcUtil.exceptionInfo("error XmlKzReader storeContent: ", exc, 0, 10);
        System.err.println(sExc);
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
    if(StringFunctions.contains(src, "Comment of Network1xx"))
      Debugutil.stop();
    //commented: if(StringFunctions.indexOfAnyChar(src, 0, -1, "&\n") < 0) { return src; }
    int posStartNewline = -1;
    int zChar = src.length();
    int posEndNewline = zChar;
    boolean bNewline = false;
    while(++posStartNewline < zChar) {           // check whether text starts with spaces which contains newline
      char cc = src.charAt(posStartNewline);
      if("\n\r\t ".indexOf(cc)<0) break;
      bNewline |= cc == '\n' || cc == '\r';
    }
    if(!bNewline) {
      posStartNewline = 0;   // does not starts with spaces which contains newline
    }
    while(--posEndNewline >= posStartNewline) {                // check whether text ends with spaces which contains newline
      char cc = src.charAt(posEndNewline);
      if("\n\r\t ".indexOf(cc)<0) break;
      bNewline |= cc == '\n' || cc == '\r';
    }
    if(!bNewline) {                               
      posEndNewline = zChar;  // does not ends with spaces which contains newline
    } else {
      posEndNewline +=1;      // because it refers just the last char before newline, now at newline
    }
    if(StringFunctions.indexOf(src, '&') < 0 && posStartNewline ==0 && posEndNewline == zChar) { 
      return src;                                //unchanged src, no effort returns src
    } else {
      StringBuilder b = new StringBuilder(src); //need, at least one & is to be replaced.
//      boolean bLinefeedfound = false;
      if(posEndNewline < zChar) {b.delete(posEndNewline, zChar); }
      if(posStartNewline >0) { b.delete(0, posStartNewline); }
      
      int pos = 0;
      while( ( pos  = StringFunctions.indexOf(b, '&', pos)) >=0) {
        int posEnd = StringFunctions.indexOf(b, ';', pos);
        String search = "";
        if(posEnd >0 && (posEnd - pos) < 11) {
          search = b.subSequence(pos, posEnd+1).toString();
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
          throw new ParseException("faulty special Characters:" + search + "on #", pos);
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
  
  
  
  private void errMsg(int nr, StringPartScan inp, CharSequence ... txt) {
    //TODO check when throw
    System.err.append("\n");
    for(CharSequence txt1: txt) {
      System.err.append(txt1); 
    }
    System.err.append( " @line:").append(Integer.toString(inp.getLineAndColumn(null)));
    System.err.append(inp.getCurrent(20)).append("...");
  }
  
  
  
  
  
  /**Read the configuration file for this instance. The configuration contains
   * how elements of a read xml file are processed and stored.
   * @param file from this file
   * @return A new XmlCfg instance which is automatically set as referenced and used in {@link #cfg}.
   *   But you can store this reference and used for {@link #readXml(File, Object, XmlCfg)} later too.
   * @throws IOException
   */
  public XmlCfg readCfg(File file) throws IOException {
    XmlCfg cfg = new XmlCfg(false);
    readXml(file, cfg.rootNode, this.cfgCfg);
    cfg.transferNamespaceAssignment(this.namespaces); //necessary since 2024-05-17

    cfg.finishReadCfg();
    this.namespaces.clear();                     // do not merge with namespace of the read XML file
    this.cfg = cfg;
    return cfg;
  }

  
  /**Set the CmlCfg with a Java-given structure, especially for config reading.
   * @param cfg
   */
  public void setCfg(XmlCfg cfg) { this.cfg = cfg; }


  /**Gets the XmlCfg data for additional evaluation.
   * The current config is also returned on {@link #readCfg(File)} or {@link #readCfgFromJar(Class, String)}-as result from setting
   */
  public XmlCfg getCfg () { return this.cfg; }


  /**Read from a resource (file inside jar archive).
   * @param clazz A class in any jar, from there the relative path to the pathInJar is built.
   *   Often the clazz should be the output data clazz. 
   * @param pathInJar relative Path from clazz. 
   *   Usually the cfg should be in the same directory as the output data class. Then this is only the file name.
   * @return A new XmlCfg instance which is automatically set as referenced and used in {@link #cfg}.
   *   But you can store this reference and used for {@link #readXml(File, Object, XmlCfg)} later too.
   * @throws IOException
   */
  public XmlCfg readCfgFromJar(Class<?> clazz, String pathInJarFromClazz) throws IOException {
    String pathMsg = "jar:" + pathInJarFromClazz;
    //ClassLoader classLoader = ClassLoader.getSystemClassLoader();
    //classLoader.getResource("slx.cfg.xml");
    InputStream xmlCfgStream = clazz.getResourceAsStream(pathInJarFromClazz);
    if(xmlCfgStream == null) throw new FileNotFoundException(pathMsg);
    XmlCfg cfg = new XmlCfg(false);
    readXml(xmlCfgStream, pathMsg, cfg.rootNode, this.cfgCfg);
    xmlCfgStream.close();
    cfg.transferNamespaceAssignment(this.namespaces);
    cfg.writeToText(new File("T:/" + pathInJarFromClazz + ".txt"), log); //only for test yet.

    cfg.finishReadCfg();
    this.cfg = cfg;
    return cfg;
  }


  
  
  /**Read from a resource (file inside jar archive).
   * @param clazz A class in any jar, from there the relative path to the pathInJar is built.
   *   Often the clazz should be the output data clazz. 
   * @param pathInJar relative Path from clazz. 
   *   Usually the cfg should be in the same directory as the output data class. Then this is only the file name.
   * @return A new XmlCfg instance which is automatically set as referenced and used in {@link #cfg}.
   *   But you can store this reference and used for {@link #readXml(File, Object, XmlCfg)} later too.
   * @throws IOException
   */
  public XmlCfg readCfgTxtFromJar(Class<?> clazz, String pathInJarFromClazz) throws IOException {
    String pathMsg = "jar:" + pathInJarFromClazz;
    //ClassLoader classLoader = ClassLoader.getSystemClassLoader();
    //classLoader.getResource("slx.cfg.xml");
//    InputStream xmlCfgStream = clazz.getResourceAsStream(pathInJarFromClazz);
//    if(xmlCfgStream == null) throw new FileNotFoundException(pathMsg);
    XmlCfg cfg = new XmlCfg(true);
    cfg.readFromJar(clazz, pathInJarFromClazz, this.log);
//    xmlCfgStream.close();
//    cfg.transferNamespaceAssignment(this.namespaces);
    cfg.writeToText(new File("T:/" + pathInJarFromClazz + ".txt"), log); //only for test yet.

    cfg.finishReadCfg();
    this.cfg = cfg;
    return cfg;
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
    /**The data access to store the value how it is given in the immediately node, not from SUBTREE.
     * Maybe null if not given in immediately node, but may exist in SUBTREE. 
     * It is null if the attribute is not given in immediately node. */
    DataAccess.DatapathElement daccess;
    /**Name and value of the attribute. */
    final String name, value;
    AttribToStore(DataAccess.DatapathElement daccess, String name, String value){
      this.daccess = daccess;
      this.name = name;
      this.value = value;
    }
  }


}
