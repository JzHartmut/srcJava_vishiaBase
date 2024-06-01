package org.vishia.xmlReader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.vishia.msgDispatch.LogMessage;
import org.vishia.util.DataAccess;
import org.vishia.util.Debugutil;
import org.vishia.util.FileFunctions;
import org.vishia.util.OutTextPreparer;
import org.vishia.util.StringFunctions;
import org.vishia.util.StringPart;
import org.vishia.util.StringPartFromFileLines;
import org.vishia.util.StringPartScan;

/**This class contains the configuration data to assign xml elements to Java data.
 * It is filled with the {@link XmlJzReader#readCfg(java.io.File)} from a given xml file.
 * <br>
 * The configuration xml file (config.xml) should have the same structure as the type of xml files to parse (user.xml).
 * <ul>
 * <li>Basic rule: Any possible element in the user.xml should present as pattern (template) one time in the config.xml. 
 *   Instead the value of attributes or instead the text in xml nodes a proper data path should be written. 
 *   The attribute or content value should be start with "!...", after "!" the data path is given. 
 *   The data path is the reflection access path to the current output instance.
 *   <pre>
 *   &lt;tag attr="!storepath" xmlinput:data="!getOrCreateCurrentOutputPath()">!contentStorePath(text) ... &lt;subelements ....
 *   </pre>
 * <li>   
 *   The data path is processed via {@link DataAccess.DataPathElement} 
 *   with invocation of {@link DataAccess#storeValue(org.vishia.util.DataAccess.DatapathElement, Object, Object, boolean)}
 *   or {@link DataAccess#invokeMethod(org.vishia.util.DataAccess.DatapathElement, Class, Object, boolean, boolean, Object[])}.
 * <li>
 *   If any tag or attribute is found in the user.xml, it is checked whether this tag or attribute is contained in the config.xml. 
 *   Then the information in the config.xml are used to store the data from the user.xml 
 * <li>   
 *   The special attribute <code>xmlinput:data</code> contains the path to get or create the output instance for this element
 *   based on the current output instance.  
 * <li>
 *  Whole structure of the config.xml:
 *  <pre>
 *  &lt;?xml version="1.0" encoding="utf-8"?>
 *  &lt;xmlinput:root xmlns:xmlinput="www.vishia.org/XmlReader-xmlinput">
 *  &lt;xmlinput:subtree xmlinput:name="NAMESUBTREE">
 *     .....
 *  &lt;xmlinput:cfg>
 *     ......   
 *  </pre>
 *   <ul><li>Elements <code>&lt;xmlinput:subtree ...</code> contains some templates of sub trees which should be parsed in the user.xml.
 *   Typically they are recursively sub trees.
 *   <li>The part <code>&lt;xmlinput:cfg ...</code> contains the configuration from the root node of the user.xml.
 *   </ul>
 * <li>Use attribute values for arguments in the store path:
 *   <pre>
 *   &lt;tag attr="!@attr" .... xmlinput:data="!newInstance(attr)" >!storeText(text, attr)
 *   </pre>  
 *   If the store path starts with <code>!@</code> then the attribute value of a given <code>attr</code> 
 *   is locally stored with a name (key) following after "!@...". It may be usual the same as the attribute name.
 *   That stored attribute value can be used for the datapath-routine as argument to create the instance to store the data, 
 *   or to invoke the store routine for the text of the element.
 *   If attribute values should be used as arguments for the <code>xmlinput:data</code> routine, then no attribute values should be stored 
 *   with a storePath (<code>attr="!storePath</code>). Both approaches are exclusively.
 * <li>Attribute as key: If an attribute plays a role as key in the user.xml, it should written in the following form:
 *   <pre>
 *   &lt;tag attr="!CHECK"/>
 *   &lt;tag attr="key1">!invoke1()</tag>
 *   &lt;tag attr="key2">!invoke2()</tag>
 *   </pre>
 *   The first <code>&lt;tag ...ATTR="!CHECK" ...></code> designates this attribute to check it, use its value as key.
 *   Any following <code>&lt;tag ...ATTR="..." ...></code> builds a key with the tag, the attribute and the value 
 *   to search the proper config node with given data in user.xml. So they are different entries for the same tag but with different attribute values.
 *   The key for this element is internally build with <code>tag@attr="key1"@anotherAttr="itsKey"</code>
 * <li>sub tree: 
 *   <pre>
 *   &lt;subtreeTag xmlinput:subtree="subtreeTag" xmlinput:data="!addmodule()"/>
 *   </pre>   
 *   If a node <code>&lt;subtreeTag ... </code> was found in the user.xml, then via <code>xmlinput:subtree="subtreeTag"</code> the named
 *   <code>&lt;xmlinput:subtree ....</code> is searched in the config file. That is used for sub nodes from this position.
 *   Typically this is proper to use for recursively content in the user.xml. Then the sub tree contains a link to the same sub tree itself.  
 *   The config node above itself can contain more attributes but no content.
 * </ul>   
 * @author Hartmut Schorrig
 *
 */
public class XmlCfg
{
  /**Version, License and History: See {@link XmlJzReader}.
   * <ul>
   * <li>2024-05-21 new: {@link #readCfgFile(File, LogMessage)}, {@link #readFromJar(Class, String, LogMessage)}, {@link #readFromText(StringPartScan, LogMessage)} 
   *   Now the representation can be better editable as normal syntactical tet. 
   * <li>2024-05-17 chg: {@link #transferNamespaceAssignment(Map)} does no more call src.clean(), instead it is called outside.
   *   The clean is not a task of this operation. Clean() is necessary before read a new Xml file in {@link XmlJzReader} instead.
   *   The solution before was dirty.  
   * <li>2024-05-17 chg: new {@link #writeToText(File, LogMessage)} as new concept starting but not finished in 2022. 
   * <li>2022-06-25 new: {@link #newCfgCfg()} also setContentStorePath(...) for the xmlinput:subtree node itself. 
   *   it is a special case, hence first not regarded, but possible and necessary.
   *   Some sorting and comments there. 
   * <li>2022-06-06 new: regards "xmlinput:finish" for a set operation. It is now the same concept as for ZbnfData:
   *   <ul>
   *   <li>The "xmlinput:data" delivers the data storage, but need not insert the data storage for this element in the whole data tree.
   *   <li>The "xmlinput:finish" inserts the data storage (via reference, hold stack local till now) in the whole data tree.
   *     Depending from the finish operation the data can be post prepared in any kind.
   *     For ZBNF data storage this is the set... operation. It should be named set_... also here. 
   *   <li>If the data does not need to be post-prepared, it is also possible that this operation is not given or empty.
   *     But then the "xmlinput:data" should set the reference in the whole data tree already for the new created data.
   *     That is the approach till now, furthermore supported.
   *   </ul>
   * <li>2021-10-07 new: {@link XmlCfgNode#nameSpaceDef} to store namespace definitions in user data.
   * <li>2021-10-07 formally change: General using TreeMap (java.util) instead the specific IndexMultiTable, same functionality 
   * <li>2019-08-16 {@link XmlCfgNode#allArgNames} gets initial name, value, tag and text as unified for ReadCfgCfg and the used cfg 
   * <li>2019-08-15 Changes in respect of {@link DataAccess} usage of variables.
   * <li>2019-03-13 new {@link XmlCfgNode#addFromSubtree(XmlCfgNode)} copies only not defined attributes from the subtree block definition.
   *   hence the definition of an attribute in the call subtree line is prior to the attribute defined in the subtree. 
   * <li>2019-03-13 new {@link #subtreeForward}. It is possible that a definition in a subtree uses another subtree which is declared below. 
   *   The information from the subtree will be copied on end of reading config. {@link #finishReadCfg(Map)} (moved from XmlJzReader) does it. 
   * <li>2018-08-15 element subNodeUnspec removed, instead store key="?" in {@link XmlCfgNode#subnodes}. 
   * <li>2018-08-15 {@link #newCfgCfg()} more simple. Don't use the root node as config for root node.
   * <li>2018-08-15 {@link #newCfgReadStruct()} accepts all XML structures, stores the structure of the nodes and attributes.
   *  
   * 
   * <li>2017-06 created.
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
  public static final String version = "2024-05-22";


  /**Assignment between nameSpace-value and nameSpace-alias gotten from the xmlns:ns="value" declaration in the read cfg.XML file. 
   * If this table is null than the config file is read yet. */
  Map<String, String> xmlnsAssign;

  /**Configuration for subtrees which can occur anywhere in the XML file. A subtree have to be determined by a tag name of its root element.
   * With them it can be found here. */
  Map<String, XmlCfgNode> subtrees;
  
  /**entries of usage of subtree instances which are defined later in the text. 
   * This element is only used for reading the config. It is used and set to null after {@link #finishReadCfg(Map)}. */
  Map<String, List<XmlCfgNode>> subtreeForward;
  
  XmlCfgNode rootNode = new XmlCfgNode(null, this, "root");

  
  Map<String, DataAccess.IntegerIx> attribNameVale;
  
  protected final boolean readFromText;
  
  
  public XmlCfg(boolean readFromText) {
    this.readFromText = readFromText;
  }
  
  /**Creates the configuration to read a config.xml file.
   * @return instance
   */
  static XmlCfg newCfgCfg()
  { XmlCfg cfgCfg = new XmlCfg(false);
    try {
      cfgCfg.rootNode = new XmlCfg.XmlCfgNode(null, cfgCfg, null);  //The rootnode of the cfg is only formalistic.
      
      XmlCfg.XmlCfgNode rootNode = new XmlCfg.XmlCfgNode(null, cfgCfg, "xmlinput:root");  //<xmlinput:cfg as node 2. level
      cfgCfg.rootNode.addSubnode(rootNode.tag.toString(), rootNode);        //The cfg file should start with a <xmlinput:root
      //because nodes.setNewElementPath(...) is not set, the same rootNode acts as cfg for the 2. level. 
      //It is possible to add <xmlinput:root...> as 2. level, but it is not do so
      
      XmlCfg.XmlCfgNode cfgNode = new XmlCfg.XmlCfgNode(null, cfgCfg, "xmlinput:cfg");  //<xmlinput:cfg as node 2. level
      rootNode.addSubnode(cfgNode.tag.toString(), cfgNode);    //2. level add to root node too.
      
      //------------------------------------------------   The next XmlCfgNode is used for all subnodes of the <xmlinput.cfg...
      XmlCfg.XmlCfgNode nodes = new XmlCfg.XmlCfgNode(null, cfgCfg, null); //Any unspecific nodes.
      cfgNode.addSubnode("?", nodes);    //2. level add to root node too.
      nodes.addSubnode("?", nodes);
      //If the XmlJzReader detects a node with this given tag, 
      //it creates an instance to store the config via addSubTree
      nodes.setNewElementPath("!newElement(tag)");
      //if the attribute xmlinput:data is read in the input config.xml, 
      //then its values should be used to set the datapath for the element.
      //It is done via invocation of setNewElementPath(...) on the output config.
      nodes.addAttribStorePath("xmlinput:data", "!setNewElementPath(value)");  
      nodes.addAttribStorePath("xmlinput:finish", "!setFinishElementPath(value)");  
      nodes.addAttribStorePath("xmlinput:class", "!dstClassName");  //This attribute value should be used to store locally in name.
      nodes.addAttribStorePath("xmlinput:list", "!setList()");  
      nodes.setContentStorePath("!setContentStorePath(text)");
      StringPartScan spAttribStorePath = new StringPartScan("addAttribStorePath(name, value)");
      nodes.attribsUnspec = new DataAccess.DatapathElement(spAttribStorePath, nodes.allArgNames, null);  //use addAttributeStorePath in the dst node to add.
      
      //------------------------------------------------   Nodes for the config-subtree
      //If the XmlJzReader detects a node with this given tag, 
      //it uses the following config and store rules.
      XmlCfg.XmlCfgNode nodeSub = new XmlCfg.XmlCfgNode(null, cfgCfg, "xmlinput:subtree");
      rootNode.addSubnode(nodeSub.tag.toString(), nodeSub);
      //Below this subtree top node the normal cfg nodes should be used.
      nodeSub.addSubnode("?", nodes);
      nodeSub.setContentStorePath("!setContentStorePath(text)");  //also the storeContent should be regarded.
      //If the XmlJzReader detects an attribute with this name, 
      //it stores the content as argument subtreename used see next.
      nodeSub.addAttribStorePath("xmlinput:name", "!@subtreename");
      //If the XmlJzReader detects a node with this given tag, 
      //it creates an instance to store the config via addSubTree
      nodeSub.setNewElementPath("!addSubTree(subtreename)");
      //... and it calls this given finish operation on end.
      nodeSub.addAttribStorePath("xmlinput:finish", "!setFinishElementPath(value)");  
      //If the XmlJzReader detects an attribute with this name, 
      //it stores the content in this designated field of the new cfg node.
      nodeSub.addAttribStorePath("xmlinput:class", "!dstClassName");
      //If the XmlJzReader detects an attribute with this name, 
      //it calls the setNewElementPath operation for the given cfg.xml element. 
      nodeSub.addAttribStorePath("xmlinput:data", "!setNewElementPath(value)");  
      nodeSub.addAttribStorePath("xmlinput:list", "!setList()");  
      spAttribStorePath.assign("addAttribStorePath(name, value)");
      nodeSub.attribsUnspec = new DataAccess.DatapathElement(spAttribStorePath, nodes.allArgNames, null);  //use addAttributeStorePath in the dst node to add.
    }
    catch(ParseException exc) {          // addAttribStorePath may invoke this exception but only in fatal error cases.
      throw new RuntimeException(exc); //it is unexpected
    }
    return cfgCfg;
  }

  
  

  
  
  /**Invoked from {@link XmlCfgNode#addSubTree(CharSequence)} to execute for the whole configuration.
   * @param name
   * @return
   */
  XmlCfgNode addSubTree(CharSequence name) //, CharSequence classDst)
  {
    String sname = name.toString();
//    if(sname.equals("text:span"))
//      Debugutil.stop();
    XmlCfgNode subtreeRoot = new XmlCfgNode(null, this, name); //The root for a subtree configuration structure.
    if(this.subtrees == null) { this.subtrees = new TreeMap/*IndexMultiTable*/<String, XmlCfgNode>(/*IndexMultiTable.providerString*/); }
    this.subtrees.put(sname, subtreeRoot);  //config-global types of subtrees
    return subtreeRoot;
  }
  


  /**Transfer the nameSpace assignments read from {@link XmlJzReader} stored there in {@link XmlJzReader#namespaces}
   * in backward direction in the {@link #xmlnsAssign}. Backward means, from the path to the namespace key.
   * This is necessary because a read XML file uses the same nameSpace string but maybe different namespace keys.
   * The simple thinking it, it's the same. Then this operation or field {@link #xmlnsAssign} would not be necessary. 
   * But this thinking is consequently worse. Follow the nameSpace concept in XML:
   * <pre>&lt;... xmlns:keyXY="nameSpace/string" ....
   *    &lt;keyXY:element</pre>
   * In an XML file instance the <code>keyXY</code> is only a locally valid. The real used value is the "nameSpace/string"
   * To search the element here in cfg this nameSpace string is used, not the short nameSpace alias.
   * <pre>
   * String aliasInCfg = xmlnsAssign.get("nameSpace/String");  // value from the read XML file
   * </pre> 
   * And this <code>aliasInCfg</code> is then used to search in cfg. 
   * It is not systematically but often given that both alias nameSpace keys are equal.
   * @param src
   * @since 2024-05 does no more invoke src.clean()
   */
  void transferNamespaceAssignment(Map<String, String> src) {
    this.xmlnsAssign = new TreeMap/*IndexMultiTable*/<String, String>();
    for(Map.Entry<String, String > ens: src.entrySet()) {
      String nsKey = ens.getKey();
      String nsPath = ens.getValue();
      this.xmlnsAssign.put(nsPath, nsKey);  //translate the found path in the XML source to the nameSpace key used in the config.xml 
    }
  }

  
  /**TODO this operation may be non sensible, change XmlJzReader, TODO
   * It transfers the {@link #subtrees} content to the real sub trees of nodes. 
   * Better search in {@link #subtrees} while parsing XML.
   * @since 2024-05-17 does not call {@link #transferNamespaceAssignment(Map)}, should be called outside.
   */
  void finishReadCfg() {
    
    if(this.subtrees !=null) for(Map.Entry<String, XmlCfgNode> e : this.subtrees.entrySet()) {
      XmlCfgNode subtree = e.getValue();
      if(subtree.dstClassName == null) {
        subtree.dstClassName = subtree.tag.toString();
      }
      else if(subtree.dstClassName == subtree.tag.toString()) {
        System.out.println("NOTE: ok dstClassName = " + subtree.dstClassName);
      } else {
        System.out.println("NOTE: ?? dstClassName = " + subtree.dstClassName + " vs. subtree.tag = " + subtree.dstClassName.toString());
        subtree.dstClassName = subtree.tag.toString();
      }
      
      //
      //check whether other subtrees has using this subtree as forward:
      List<XmlCfgNode> subtreeUsage = this.subtreeForward ==null ? null: this.subtreeForward.get(subtree.tag.toString());
      if(subtreeUsage !=null) {
        for(XmlCfgNode e1: subtreeUsage) {
          e1.addFromSubtree(subtree);
        }
      } else {
        System.out.println("NOTE: subtree " + subtree.tag + " not used");
      }
      //
    }
    this.subtreeForward = null; //processed, no more necessary.
    
  }


  
  /**Read the XmlCfg content from the textual representation. Syntax adequate {@link #writeToText(File, LogMessage)}
   * It calls {@link #readFromText(StringPartScan, LogMessage)}.
   * @param fText The file 
   * @param log log on error
   * @return true if all ok, false if any syntax error.
   * @throws IllegalCharsetNameException
   * @throws UnsupportedCharsetException
   * @throws FileNotFoundException
   * @throws IOException
   */
  public boolean readCfgFile ( File fText, LogMessage log) throws IllegalCharsetNameException, UnsupportedCharsetException, FileNotFoundException, IOException {
    StringPartScan sp = new StringPartFromFileLines(fText);
    boolean bOk = readFromText(sp, log);
    sp.close();
    return bOk;
  }
  
  
  /**Read the XmlCfg content from the textual representation. Syntax adequate {@link #writeToText(File, LogMessage)}
   * It calls {@link #readFromText(StringPartScan, LogMessage)}.
   * @param fText The file 
   * @param log log on error
   * @return true if all ok, false if any syntax error.
   * @throws IllegalCharsetNameException
   * @throws UnsupportedCharsetException
   * @throws FileNotFoundException
   * @throws IOException
   */
  public boolean readFromJar ( Class<?> clazz, String pathInJarFromClazz, LogMessage log) throws IllegalCharsetNameException, UnsupportedCharsetException, FileNotFoundException, IOException {
    StringPartScan sp = new StringPartFromFileLines(clazz, pathInJarFromClazz, 0x10000, null, Charset.forName("UTF-8"));
    boolean bOk = readFromText(sp, log);
    sp.close();
    return bOk;
  }
  
  
  /**Read the XmlCfg content from the textual representation. Syntax adequate {@link #writeToText(File, LogMessage)}
   * @param sp should be initialized with the text 
   * @param log log on error
   * @return true if all ok, false if any syntax error.
   */
  public boolean readFromText(StringPartScan sp, LogMessage log) {
    assert(this.readFromText);
    boolean bOk = true;
    sp.setIgnoreWhitespaces(true);
    if(!sp.scanStart().scan("XmlJzReader-Config 2024-05").scanOk()) {
      log.writeError("ERROR readCfgFromText, faulty head: %s", sp.getCurrent(30));
      return false;
    }
    this.xmlnsAssign = new TreeMap<String, String>();
    while(sp.scan("NS:").scanOk()) {
      String[] value = new String[1];
      if(!sp.scanIdentifier().scan("="). scanQuotion("\"", "\"", value).scanOk()) {
        log.writeError("ERROR readCfgFromText, faulty NS: %s", sp.getCurrent(50));
        return false;
      }
      String alias = sp.getLastScannedString();
      this.xmlnsAssign.put(value[0], alias);
    }
    while(sp.scan("SUBTREE:").scanToStringEnd(" ").scan("<").scanToStringEnd(">").scanOk()) {
      String sTag = sp.getLastScannedString();
      String nameSubtree = sp.getLastScannedString();
      XmlCfgNode nodeSubtree = new XmlCfgNode(null, this, sTag);
      if(this.subtrees == null) {this.subtrees = new TreeMap<>(); }
      this.subtrees.put(nameSubtree, nodeSubtree);
      bOk &= readFromTextNode(nodeSubtree, sp, log);
    }
    if(sp.scan("<root>").scanOk()) {
      bOk &= readFromTextNode(this.rootNode, sp, log);
    } else {
      log.writeError("ERROR readCfgFromText, <root> is missing %s", sp.getCurrent(50));
      bOk = false;
    }
    return bOk;
  }
  
  
  private boolean readFromTextNode(XmlCfgNode node, StringPartScan sp, LogMessage log) {
    boolean bOk = true;
    String sElementStorePath = null, sElementFinishPath = null, sTextStorePath = null, sNamespaceStorePath = null;
    if(sp.scan("=>SUBTREE:").scanToAnyChar(" \n\r", '\"', '\"', '\\').scanOk()) {
      node.cfgSubtreeName = sp.getLastScannedString();
    }
    if(sp.scan("LIST").scanOk()) {
      node.bList = true;
    }
    if(sp.scan("CLASS:").scanIdentifier().scanOk()) {
      node.dstClassName = sp.getLastScannedString();
    }
    if(sp.scan("NEW:").scanLiteral("\"\"\\", 9999).scanOk()) {
      sElementStorePath = sp.getLastScannedString();
    }
    if(sp.scan("ADD:").scanLiteral("\"\"\\", 9999).scanOk()) {
      sElementFinishPath = sp.getLastScannedString();
    }
    if(sp.scan("TEXT:").scanLiteral("\"\"\\", 9999).scanOk()) {
      sTextStorePath = sp.getLastScannedString();
    }
    if(sp.scan("NAMESPACE:").scanLiteral("\"\"\\", 9999).scanOk()) {
      sNamespaceStorePath = sp.getLastScannedString();
    }
    while(sp.scan("@").scanIdentifier(null, "-:").scan("=").scanLiteral("\"\"\\", 9999).scanOk()) {
      String sAttrStorePath = sp.getLastScannedString();
      String sAttrNsName = sp.getLastScannedString();
      try{ node.addAttribStorePath(sAttrNsName, sAttrStorePath);
      } catch(ParseException exc) {
        log.writeError("ERROR readCfgFromText, Exception ", exc);
      }
    }
    if(sElementStorePath !=null) {
      try{ node.setNewElementPath(sElementStorePath);
      } catch(ParseException exc) {
        log.writeError("ERROR readCfgFromText, Exception setNewElementStorePath " + sElementStorePath, exc);
      }
    }
    if(sElementFinishPath !=null) {
      try{ node.setFinishElementPath(sElementFinishPath);
      } catch(ParseException exc) {
        log.writeError("ERROR readCfgFromText, Exception setFinishElementStorePath " + sElementFinishPath, exc);
      }
    }
    if(sTextStorePath !=null) {
      try{ node.setContentStorePath(sTextStorePath);
      } catch(ParseException exc) {
        log.writeError("ERROR readCfgFromText, Exception setContentStorePath " + sTextStorePath, exc);
      }
    }
    if(sNamespaceStorePath !=null) {
      try{ node.setNameSpaceStorePath(sNamespaceStorePath);
      } catch(ParseException exc) {
        log.writeError("ERROR readCfgFromText, Exception setNameSpaceStorePath " + sNamespaceStorePath, exc);
      }
    }
    while(bOk && sp.scan("<").scanIdentifier(null, "-:").scan(">").scanOk()) {
      String sTag = sp.getLastScannedString();
      XmlCfgNode subnode = new XmlCfgNode(null, this, sTag);
      if(node.subnodes == null) { node.subnodes = new TreeMap<>(); }
      node.subnodes.put(sTag, subnode);
      bOk &= readFromTextNode(subnode, sp, log);
    }
    if(sp.scan("</").scanIdentifier(null, "-:").scan(">").scanOk()) {
      String sTagEnd = sp.getLastScannedString();
      if(!node.tag.equals(sTagEnd)) {
        log.writeError("ERROR readCfgFromText: faulty </%s> for node %s", sTagEnd, node.tag);
      }
    } else {
      log.writeError("ERROR readCfgFromText: missing </end:tag>", sp.getCurrentPart(30));
      bOk = false;
    }
    return bOk;
  }
  
  
  
  
  /**Control for {@link #writeToText(File, LogMessage)} for the head of the text file. */
  private OutTextPreparer otxCfgHead = new OutTextPreparer("cfgHead", "xmlCfg", 
      "XmlJzReader-Config 2024-05" 
    + "<:if:xmlCfg.xmlnsAssign><:for:ns:xmlCfg.xmlnsAssign>"
    + "<:n>NS: <&ns>=\"<&ns_key>\""
    + "<.for><.if>"
      );
  

  
  /**Control for writing a Node. 
   * <ul><li>whatis := "SUBTREE" or empty.
   * <li>indent := spaces from left.
   * <li>node := the root or sub node
   * </ul>
   * */
  private OutTextPreparer otxNode = new OutTextPreparer("node", "whatis, indent, node", 
      "<:n><&indent><&whatis><:<><&node.tag><:>>" 
    + "<:if:node.attribsForCheck> <:for:attr:node.attribsForCheck> @<&attr.name>==\"<&attr.storeInMap>\"<.for><:n><&indent><.if>"  
    + "<:if:node.cfgSubtreeName> =>SUBTREE:<&node.cfgSubtreeName><.if>"
    + "<:if:node.bList> LIST<.if>"
    + "<:if:node.dstClassName> CLASS:<&node.dstClassName><.if>"
    + "<:if:node.elementStorePath><:n><&indent>  NEW:\"<:exec:wrDataAccess(OUT, node.elementStorePath)>\"<.if>"
    + "<:if:node.elementFinishPath><:n><&indent>  ADD:\"<:exec:wrDataAccess(OUT, node.elementFinishPath)>\"<.if>"
    + "<:if:node.contentStorePath><:n><&indent>  TEXT:\"<:exec:wrDataAccess(OUT, node.contentStorePath)>\"<.if>"
    + "<:if:node.nameSpaceDef><:n><&indent>  NAMESPACE:\"<&node.nameSpaceDef>\"<.if>"
    + "<:if:node.attribs><:for:attr:node.attribs><:n><&indent>  @<&attr.name>=\"<:if:attr.storeInMap>@<&attr.storeInMap><.if><:if:attr.daccess><&attr.daccess><.if>\"<.for><.if>"  
    + "<:if:node.subnodes><:for:subnode:node.subnodes><:exec:writeSubNode(OUT, indent, subnode)><.for><.if>"  
    + "<:n><&indent><:<>/<&node.tag><:>>" 
    //<:n>"
    );
  
  
  
  
  
  
  
  /**writes the content of the config to a text file.
   * @param fText
   * @param log
   * @since 2024-05-17
   */
  public void writeToText (File fText, LogMessage log) {
    StringBuilder wb = new StringBuilder();
    Map<String, OutTextPreparer> idxScript = new TreeMap<>();
    idxScript.put(this.otxNode.sIdent, this.otxNode);
    idxScript.put(this.otxCfgHead.sIdent, this.otxCfgHead);
    try {
      OutTextPreparer.parseTemplates(idxScript, this.getClass(), null, log);
      OutTextPreparer.DataTextPreparer otdCfg = this.otxCfgHead.createArgumentDataObj();
      otdCfg.setArgument("xmlCfg", this);
      otdCfg.setExecObj(this);
      this.otxCfgHead.exec(wb, otdCfg);
      OutTextPreparer.DataTextPreparer otdNode = this.otxNode.createArgumentDataObj();
      if(this.subtrees !=null) for( Map.Entry<String, XmlCfgNode> e : this.subtrees.entrySet()) {
        String nameSubtree = e.getKey();
        XmlCfgNode subtreeNode = e.getValue();
        otdNode.setArgument("whatis", "SUBTREE:" + nameSubtree + " ");
        otdNode.setArgument("indent", "");
        otdNode.setArgument("node", subtreeNode);
        otdNode.setExecObj(this);
        this.otxNode.exec(wb, otdNode);
      }
      otdNode.setArgument("whatis", "");
      otdNode.setArgument("indent", "");
      otdNode.setArgument("node", this.rootNode);
      otdNode.setExecObj(this);
      this.otxNode.exec(wb, otdNode);
      wb.append("\n");
      FileFunctions.writeFile(wb.toString(), fText);
    } catch(Exception exc) {
      log.writeError("ERROR writing xmlcfg", exc);
    }
    
  }
  
  
  protected void writeSubNode(Appendable wr, String indent, XmlCfgNode node) throws IOException {
    if(indent.length() > 10) {
      return;
    }
    OutTextPreparer.DataTextPreparer otdSubnode = this.otxNode.createArgumentDataObj();
    otdSubnode.setArgument("whatis", "");
    otdSubnode.setArgument("indent", indent+ "  ");
    otdSubnode.setArgument("node", node);
    otdSubnode.setExecObj(this);
    this.otxNode.exec(wr, otdSubnode);
    Debugutil.stop();
  }
  
  
  /**Writes the data access path, called inside {@link #otxNode} from script via &lt:exec:wrDataAccess(...)>
   * @param wr
   * @param dacc
   * @throws IOException
   */
  protected static void wrDataAccess(Appendable wr, DataAccess.DatapathElement dacc) throws IOException {
    dacc.writeAccessString(wr);
  }
  
  
  
  /**Returns an iterator over all internal name space assignments.
   * Note: It encapsulates the content of the internal {@link #xmlnsAssign} container, prevent for change.
   * @return
   */
  public Iterable<Map.Entry<String, String>> iterNamespace () { return this.xmlnsAssign.entrySet(); }
  
  
  
  /**Detect different entries in sub node and node via sub tree to clarify it. 
   * @param log
   */
  public void checkCfg ( LogMessage log) {
    checkCfg(this.rootNode, log, new HashMap<XmlCfgNode, XmlCfgNode>(), 100);
  }
  
  private void checkCfg ( XmlCfgNode node, LogMessage log, HashMap<XmlCfgNode, XmlCfgNode> nodesChecked, int recursively) {
    if(recursively <10 ) {
      Debugutil.stop();
    } else if(recursively <0 ) {
      throw new IllegalArgumentException("too many recurions checkCfg");
    }
    if(node.subnodes !=null) for(XmlCfgNode subNode : node.subnodes.values()) {
      if(subNode.cfgSubtreeName !=null) {
        XmlCfg.XmlCfgNode subCfgNodeSubtree = this.subtrees.get(subNode.cfgSubtreeName);
        subNode.cmpNode(subCfgNodeSubtree, log);
        Debugutil.stop();
      } 
      if(nodesChecked.get(node) !=null) {
        if(log!=null) log.writeError("ERROR CheckCfgSubtree : node recursively %s in parent %s", node.tag, node.parent == null ? "??" : node.parent.tag);
      } else {
        nodesChecked.put(node, node);
        checkCfg(subNode, log, nodesChecked, recursively -1);
        nodesChecked.remove(node);
      }
    }
  }
  
  
  /**An instance of this class describes for any attribute how to proceed-
   */
  public static class AttribDstCheck {
    
    final String name;
    
    /**If given, the data access to store the value. null if not to store in the users instance immediately*/
    DataAccess.DatapathElement daccess;
    
    /**If given, name to store the attribute value in a argument map. It is alternatively to {@link #daccess} */
    String storeInMap;
    
    /**Set to true on a "!CHECK" value in the config.xml. 
     * If true than this attribute name and value in the user.xml is used to build a key with the tag and all such attributes.
     * The element in the user.xml is assigned to the proper element with this attribute value in the config.xml.
     * But that is not this xml node. The definition of xml nodes of type {@link XmlCfgNode} with the proper key should follow.
     * In the following definitions with the same tag, that attribute name and value is used as key. 
     * The key will be buld in form <code>tag@attr="value"</code> 
     */
    public final boolean bUseForCheck;
    
    public AttribDstCheck(String name, boolean bUseForCheck) {
      this.name = name;
      this.bUseForCheck = bUseForCheck;
    }
    
    
    @Override public String toString() { return this.name; }
    
  } //class
  
  
  
  /**This class describes one node as pattern how the content of a parsed xml file should be stored.
   * It is an output instance while the config.xml is read. 
   *
   */
  public static class XmlCfgNode
  {
    /**Back reference to the whole config. Especially {@link XmlCfg#xmlnsAssign} is used to evaluate attributes. 
     * The nameSpace is singular for the whole config. */
    final XmlCfg cfg;

    /**Parent node, to navigate in debug, to store this node if {@link #attribsForCheck} are present. */
    private final XmlCfgNode parent;
    
    /**Reflection path either to store the content of the node
     * or also to get an instance as "sub node" to store the content.
     * <br>
     * <br>
     * The {@link DataAccess.DatapathElement#args} contains the arguments for new_...(...). 
     * This comes from the textual given expression. This values are necessary to store in the created class, but final. 
     */
    DataAccess.DatapathElement elementStorePath;
    
    /**Reflection path of usual an operation which is called on end of the node. 
     * This is on <code>&lt;/tag></code> or also on <code>/></code> if the node has not further sub nodes.
     * This operation can be sensible, if the data for the node should be post-prepared if all information inside the node are available.
     * This element can be null if the store path is not necessary.
     */
    DataAccess.DatapathElement elementFinishPath;
    
    /**true then this element is stored with more as one instance. 
     * 
     */
    boolean bList;
    
    /**Argument names from attributes which are used for the new_<&element>
     * but also 4 possible standard argument names: tag, name, value, text. 
     * It is not the arguments of the new_<&element>(...) only, it can be all possible and unnecessary arguments.
     * Place holder for possible ones. See {@link #elementStorePath} 
     */
    Map<String, DataAccess.IntegerIx> allArgNames;
  
    /**The first node in some equal nodes in cfg, which determines the attributes used for check. */
    boolean bCheckAttributeNode;
    
    /**True if the value of attributes should be stored in the new content.
     * False if attributes are stored only in the attribute map and evaluate especially by the invocation of {@link #elementStorePath}
     * It is set if at least one attributes with  a store path (with "!...") is found.
     */
    boolean bStoreAttribsInNewContent;
    
    /**If not null, contains attribute names which's name and value are used to build the key to found the proper xml noce in the config file.
     * Firstly all attributes should be checked whether their names are contained here.
     */
    private Map/*IndexMultiTable*/<String, AttribDstCheck> attribsForCheck;
    
    /**Key (attribute name with xmlns:name) and reflection path to store the attribute value.
     * Attributes of the read input which are not found here are not stored.
     */
    Map /*IndexMultiTable*/<String, AttribDstCheck> attribs;
    
    /**If set, the attrib dst for not found attributes to store in a common way. */
    DataAccess.DatapathElement attribsUnspec;
    
    /**If not null, the attrib dst for name space definition. */
    DataAccess.DatapathElement nameSpaceDef;
    
    /**Key (tag name with xmlns:name) and configuration for a sub node.
     * Nodes of the read input which are not found here are not stored.
     * Special case: If the key is "?" this is a 'unspecific node', taken anyway.
     * The content is stored as for all nodes with the {@link #contentStorePath}.
     */
    Map<String, XmlCfgNode> subnodes;
    
    /**If set, the subnode for not found elements to store in a common way.. */
    //XmlCfgNode subNodeUnspec;
    
    /**Reflection path to store the content as String. If null than the content won't be stored. */
    DataAccess.DatapathElement contentStorePath;
  
    final CharSequence tag;
    
    String dstClassName;
    
    /**If not null, this element refers its {@link #attribs} and {@link #subnodes} in a config-subtree. */
    String cfgSubtreeName;
  
    XmlCfgNode(XmlCfgNode parent, XmlCfg cfg, CharSequence tag){ 
      this.parent = parent; this.cfg = cfg; this.tag = tag;
      this.allArgNames = new TreeMap<String, DataAccess.IntegerIx>();
      //register this 4 standard argument names, hence it are the same in XmlCfgCfg and a read XmlCfg
      this.allArgNames.put("name", new DataAccess.IntegerIx(0));
      this.allArgNames.put("value", new DataAccess.IntegerIx(1));
      this.allArgNames.put("tag", new DataAccess.IntegerIx(2));
      this.allArgNames.put("text", new DataAccess.IntegerIx(3));
    }
  
    /**Sets the path for the "new element" invocation see {@link XmlCfgNode#elementStorePath}.
     * @param dstPath either a method or an access to a field.
     * @throws ParseException 
     */
    public void setNewElementPath(String dstPath) throws ParseException {
      final String sPath;
      if(this.cfg.readFromText && !dstPath.startsWith("!")) {
        sPath = dstPath;
      } else {
        if(!dstPath.startsWith("!")) { 
          throw new IllegalArgumentException("The store path in xmlInput:data= \"" + dstPath + "\" in config.xml should start with ! because it is a store path.");
        }
        sPath = dstPath.substring(1);
      }
      StringPartScan spPath = new StringPartScan(sPath);
      spPath.setIgnoreWhitespaces(true);                   // NOTE: this.allArgNames will be completed by necessary arguments given as String
      this.elementStorePath = new DataAccess.DatapathElement(spPath, this.allArgNames, null);  //gathered necessary names.
      if(this.allArgNames.size() ==0) {
        this.allArgNames = null; //not necessary.
      }
    }
  
    
    /**Sets the path for the "set element" invocation see {@link XmlCfgNode#elementFinishPath}.
     * @param dstPath either a method or an access to a field.
     * @throws ParseException 
     */
    public void setFinishElementPath(String dstPath) throws ParseException {
      String sPath;
      if(this.cfg.readFromText && !dstPath.startsWith("!")) {
        sPath = dstPath;
      } else {
        if(!dstPath.startsWith("!")) { 
          throw new IllegalArgumentException("The store path in xmlInput:data= \"" + dstPath + "\" in config.xml should start with ! because it is a store path.");
        }
        sPath = dstPath.substring(1);
      }
      StringPartScan spPath = new StringPartScan(sPath);
      spPath.setIgnoreWhitespaces(true);
      this.elementFinishPath = new DataAccess.DatapathElement(spPath, this.allArgNames, null);  //gathered necessary names.
      if(this.allArgNames.size() ==0) {
        this.allArgNames = null; //not necessary.
      }
    }
  
    
    public void setList() {
      this.bList = true;
    }
    
    
    /**This method is invoked from the xml configuration reader to create a new attribute entry for the found attribute.
     * @param key ns:name of the found attribute in the config.xml 
     * @param dstPath datapath which is found as value in the config.xml. The datapath is used for the user.xml to store the attribute value.
     * @throws ParseException 
     */
    public void addAttribStorePath(String key, String sAttrValue) throws ParseException {
      AttribDstCheck attribForCheck;
      //Check whether the attribute is used to build the key for search the correct config node.
      if(key.equals("xmlinput:subtree")) {
        //use this subtree instead:
        this.cfgSubtreeName = sAttrValue;
        XmlCfgNode subtree = null; //should be done all via subtreeForward, or better remove it:   this.cfg.subtrees.get(sAttrValue);
        if(subtree == null) {
          //not found or later in file. 
          if(this.cfg.subtreeForward == null) { this.cfg.subtreeForward = new TreeMap<String, List<XmlCfgNode>>(); }
          List<XmlCfgNode> subtreeForwardList = this.cfg.subtreeForward.get(sAttrValue);
          if(subtreeForwardList == null) {
            subtreeForwardList = new LinkedList<XmlCfgNode>();
            this.cfg.subtreeForward.put(sAttrValue, subtreeForwardList);
          }
          subtreeForwardList.add(this); //add subnodes, attribs, dstClassName later if found.
        } else {
          addFromSubtree(subtree);
        }
//    }
//    else if(key.equals("xmlinput:class")) { //especially for build JavaDst in cfg.xml
//      this.dstClassName = sAttrValue;
      }  
      else if(  this.attribsForCheck !=null     //The attribsForCheck was set because a primary config node with bCheckAttributeNode was found before
        && (attribForCheck = this.attribsForCheck.get(key))!=null
        && attribForCheck.bUseForCheck
        ) {
          //tag is a StringBuilder in that case.
        ((StringBuilder)this.tag).append('@').append(key).append("=\"").append(sAttrValue).append("\"");
        //Note: this instance is not added in the subnodes in the parent yet, because the key is completed here.
        //Because the subnode of cfg should have any content the setContentStorePath(...) is called.
        //There it is added to parent.
      }
      else if(sAttrValue.length() >0) {  //empty attribute value in cfg: ignore attribute.
        if(this.attribs == null) { this.attribs = new TreeMap<String, AttribDstCheck>(); } //IndexMultiTable<String, AttribDstCheck>(IndexMultiTable.providerString); }
        AttribDstCheck dPathAccess;
        final String sPath;
        if(this.cfg.readFromText && !sAttrValue.startsWith("!")) {
          sPath = sAttrValue;
        } else {
          if(!sAttrValue.startsWith("!")) { 
            throw new IllegalArgumentException("The store path as attrib value \"" + sAttrValue + "\" in config.xml should start with ! because it is a store path.");
          }
          sPath = sAttrValue.substring(1);
        }
        //
        String dstPath;
        if(StringFunctions.equals(sPath, "CHECK")) {
          //use the attribute value as key for select the config and output, it is the primary config node
          dstPath = null;
          dPathAccess = new AttribDstCheck(key, true);
          this.attribs.put(key,  dPathAccess); //create if not exists
          this.bCheckAttributeNode = true;       
        }
        else  {
          dPathAccess = new AttribDstCheck(key, false);
          this.attribs.put(key,  dPathAccess);
          dstPath = sPath;
        }
        //
        //
        if(dstPath==null){
          //dPathAccess.daccess = attribShouldMatch;
        } else {
          if(dstPath.startsWith("@")) {       //store the attribute value in the attribute map to use later to store in user area.
            dPathAccess.storeInMap = dstPath.substring(1);
          } else {
            StringPartScan sp = new StringPartScan(dstPath);
            dPathAccess.daccess = new DataAccess.DatapathElement(sp, this.allArgNames, null);
            this.bStoreAttribsInNewContent = true;  
          }
        }
      }
    }

    
    /**Set the dataAccess for storing a namespace definition. 
     * This is yet only used for {@link XmlJzCfgAnalyzer}.
     * For reading data from Xml usual a namespace definition should not be used in the read data (...?)
     * @param dstPath
     * @throws ParseException
     */
    public void setNameSpaceStorePath(String dstPath) throws ParseException {
      assert(dstPath.startsWith("!"));
      StringPartScan sp = new StringPartScan(dstPath.substring(1));
      this.nameSpaceDef = new DataAccess.DatapathElement(sp, this.allArgNames, null);
    }
    
    
    /**Adds the elements from a given &lt;xmlinput:subtree ... in the xmlcfg.xml to the used xmlcfg.
     * <ul><li>The {@link XmlCfgNode#subnodes} in the &lt;xmlinput:subtree ... are also referred in original from this {@link #subnodes}
     * <li>The attributes from the &lt;xmlinput:subtree are only taken if this {@link #attribs} does not contain the attribute. 
     *   It means the &lt;xmlinput:subtree ... attribs are not meaningful if they exists in the current element already.
     *   The entry in the current element is then significant (the called store operation as attribute value).
     *   But both should be equivalent.
     * <li>The {@link #setContentStorePath(String)} is not taken from the subtree (ignored, 2024-05-12)
     * <li>The {@link #elementStorePath} and {@link #elementFinishPath} are not taken from the subtree ( ignored, 2024-05-12).  
     * @param subtree
     */
    void addFromSubtree(XmlCfgNode subtree) {
      //Use the same subtree information for all subtree references. 
      //If the data would be copied, some more memory is neccessary, but not too much. But it is not necessary to copy.
      this.subnodes = subtree.subnodes;
      if(subtree.attribs !=null) for(Map.Entry<String, AttribDstCheck> e : subtree.attribs.entrySet()) {
        String attrName = e.getKey();
        AttribDstCheck a = e.getValue();
        if(this.attribs == null) { this.attribs = new TreeMap/*IndexMultiTable*/<String, AttribDstCheck>(); }
        AttribDstCheck attr = this.attribs.get(attrName);
        if(attr == null) {
          this.attribs.put(attrName, e.getValue());  //store the attrib from the subtree instance.
        } else {
          boolean bSame = a.bUseForCheck == attr.bUseForCheck;
          bSame &= a.daccess == null && attr.daccess == null || a.daccess.toString().equals(attr.daccess.toString());
          bSame &= a.name.equals(attr.name);
          bSame &= a.storeInMap.equals(attr.storeInMap);
          if(!bSame) {
            System.out.println("NOTE read XmlCfg: Diff. Attribute both in subtree and call found:");  //The attrib in the subtree call wins. (new since 2019-03, before: both are equal)
          }
        }
      } else {
        System.out.println("NOTE read XmlCfg: No Attribute in subtree found: ");
      }
      if(this.dstClassName == null) { this.dstClassName = subtree.dstClassName; }
    }
    
    
    
    void addSubnode(String key, XmlCfgNode node) {
      if(this.subnodes == null) { this.subnodes = new TreeMap/*IndexMultiTable*/<String, XmlCfgNode>(); }
      if(key.startsWith("Object@"))
        Debugutil.stop();
      if(key.startsWith("Array@"))
        Debugutil.stop();
      this.subnodes.put(key, node);
    }
  
  
    /**This method is invoked from the xml configuration reader to create a new subNode for a found elmeent.
     * @param name ns:tag of the found element in the config.xml 
     * @return the sub node
     */
    XmlCfgNode newElement(CharSequence name) {
      String sname = name.toString();
      XmlCfgNode subNode = null;
      if(this.subnodes !=null) {
        XmlCfgNode subNodeForCheck = this.subnodes.get(sname);  //check whether a subNode with this key is existing already,
        if(subNodeForCheck !=null) {
          if(!subNodeForCheck.bCheckAttributeNode) {
            throw new IllegalArgumentException("XmlReader-cfg: An element has more as one node with the same tag name. This is only admissible if the first node contains a \"!CHECK\" attribute.");
          } else {
            StringBuilder tagBuffer = new StringBuilder(64); tagBuffer.append(subNodeForCheck.tag); //append more @attrib="value" for the key in tag.
            subNode = new XmlCfgNode(this, this.cfg, tagBuffer);
            subNode.attribsForCheck = subNodeForCheck.attribs;
            //Note: this subnode is not added in the subnodes in the parent yet, because the key is completed here.
            //Because the subnode of cfg should have any content the setContentStorePath(...) is called.
            //There it is added to parent.
          }
        }
        //than it is the first subnode with some  <tag attr="!CHECK"/> entries. Use it to work with it.
      } 
      if(subNode == null) {
        subNode = new XmlCfgNode(this, this.cfg, sname);  //A sub node for the config.
        //subNode.elementStorePath = this.elementStorePath;  //the same routine to create the next sub node.
        //subNode.subNodeUnspec = this.subNodeUnspec;
        //subNode.attribsUnspec = this.attribsUnspec;
        //subNode.attribs = this.attribs;
        addSubnode(sname, subNode);
      }
      return subNode;
    }
  
  
    
    /**This method is invoked from the xml configuration reader to create a DataAccess element for the content of the node..
     * @param text should start with ! the dataPath to store the content
     * @throws ParseException 
     */
    void setContentStorePath(String dstPath) throws ParseException {
      if(this.tag instanceof StringBuilder) { //it is a second node with same tag, but with attributes for check.
        if(this.tag.toString().startsWith("Object@"))
          Debugutil.stop();
        if(this.tag.toString().startsWith("Array@"))
          Debugutil.stop();
        this.parent.subnodes.put(this.tag.toString(), this); //put this node in its parent, it is not done yet. 
      }
      final String sPath;
      if(this.cfg.readFromText && !dstPath.startsWith("!")) {
        sPath = dstPath;
      } else {
        if(!dstPath.startsWith("!")) { 
          throw new IllegalArgumentException("The store path in xmlInput:data= \"" + dstPath + "\" in config.xml should start with ! because it is a store path.");
        }
        sPath = dstPath.substring(1);
      }
      StringPartScan sp = new StringPartScan(sPath);
      this.contentStorePath = new DataAccess.DatapathElement(sp, this.allArgNames, null);
      sp.close();
    }
    
    
    /**Invoked via reflection from {@link XmlCfg#newCfgCfg()}-given configuration. It is invoked on <code>&lt;xmlinput:subtree name="..." </code> 
     * @param name Value from attribute name 
     * @return A new config node for the subtree content.
     */
    public XmlCfgNode addSubTree(CharSequence name) //, CharSequence classDst)
    {
      XmlCfgNode subtreenode = this.cfg.addSubTree(name); //, classDst);
      //it is unnecessary and confusing: subtreenode.cfgSubtreeName = name.toString();
      return subtreenode;
    }
    
    /**This operation is only a helper to check whether the subtree entry is okay for manual changes.
     * @param nodeCmp
     */
    public boolean cmpNode(XmlCfgNode nodeCmp, LogMessage log) {
      boolean bOk = true;
      try{ 
        if(nodeCmp == null) {
          bOk = false;
          if(log !=null) log.writeError("ERROR CheckCfgSubtree : node not found in subtree: %s", this.cfgSubtreeName);
        } else {
  //        The arguments can be differ depending on attributes.
  //        for(Map.Entry<String, DataAccess.IntegerIx> e: this.allArgNames.entrySet()) {
  //          DataAccess.IntegerIx val = e.getValue();
  //          DataAccess.IntegerIx cmp = nodeCmp.allArgNames.get(e.getKey());
  //          if(cmp == null) {
  //            bOk = false;
  //            if(log !=null) log.writeError("ERROR CheckCfgSubtree allArgNames: %s allArgNames.%s ix=%d ?? not found ", this.tag, e.getKey(), val.ix);
  //          }
  //          else if(val.ix != cmp.ix) {
  //            bOk = false;
  //            if(log !=null) log.writeError("ERROR CheckCfgSubtree allArgNames: %s allArgNames.%s ix=%d ?? %d", this.tag, e.getKey(), val.ix, cmp.ix);
  //          }
  //        }
          
          if(this.attribs !=null) {
            if(nodeCmp.attribs == null) {
              bOk = false;
              if(log !=null) log.writeError("ERROR CheckCfgSubtree attribs: %s has no attributes ", this.tag);
            } else {
              for(Map.Entry<String, XmlCfg.AttribDstCheck> e: this.attribs.entrySet()) {
                // do not check, attributes are all equal, do only check this contains equal or lesser attributes.
                XmlCfg.AttribDstCheck val = e.getValue();
                XmlCfg.AttribDstCheck cmp = nodeCmp.attribs.get(e.getKey());
                if(cmp == null) {
                  bOk = false;
                  if(log !=null) log.writeError("ERROR CheckCfgSubtree attribs: %s attribs.%s ?? not found ", this.tag, e.getKey());
                }
                else {
                  if(val.daccess != cmp.daccess) {
                    bOk = false;
                    if(log !=null) log.writeError("ERROR CheckCfgSubtree attribs: %s attribs.%s daccess %d ?? %d", this.tag, e.getKey(), val.daccess, cmp.daccess);
                  } 
                  if(!val.name.equals(cmp.name)) {
                    bOk = false;
                    if(log !=null) log.writeError("ERROR CheckCfgSubtree attribs: %s attribs.%s name %d ?? %d", this.tag, e.getKey(), val.name, cmp.name);
                  } 
                  if(!val.storeInMap.equals(cmp.storeInMap)) {
                    bOk = false;
                    if(log !=null) log.writeError("ERROR CheckCfgSubtree attribs: %s attribs.%s storeInMap %d ?? %d", this.tag, e.getKey(), val.storeInMap, cmp.storeInMap);
                  } 
                }
              }
            }
          } // for attribs
          if(this.subnodes !=null) {
            if(nodeCmp.subnodes == null) {                 // nodeCmp should contain subnodes if node contains it
              bOk = false;
              if(log !=null) log.writeError("ERROR CheckCfgSubtree subnodes: %s has no subnodes ", this.tag);
            } else if(this.subnodes != nodeCmp.subnodes) { // if both have the same subnodes, it is ok
              for(Map.Entry<String, XmlCfg.XmlCfgNode> e: this.subnodes.entrySet()) {
                // do not check, subnodes are all equal, do only check this contains equal or lesser nodes.
                XmlCfg.XmlCfgNode val = e.getValue();
                XmlCfg.XmlCfgNode cmp = nodeCmp.subnodes.get(e.getKey());
                if(cmp == null) {
                  bOk = false;
                  if(log !=null) log.writeError("ERROR CheckCfgSubtree subnodes: %s: %s ?? not found ", this.tag, e.getKey());
                }
                else if(cmp != val){
                  bOk = false;
                  if(log !=null) log.writeError("ERROR CheckCfgSubtree subnodes: %s: %s different %s ?? 5s ", this.tag, e.getKey(), val, cmp);
                }
              }
            }
          } // for attribs
          if(this.bCheckAttributeNode != nodeCmp.bCheckAttributeNode){
            bOk = false;
            if(log !=null) log.writeError("ERROR CheckCfgSubtree bCheckAttributeNode: %s:  %b ?? %b", this.tag, this.bList, nodeCmp.bList);
          }
          if(this.bCheckAttributeNode != nodeCmp.bCheckAttributeNode){
            bOk = false;
            if(log !=null) log.writeError("ERROR CheckCfgSubtree bCheckAttributeNode: %s:  %b ?? %b", this.tag, this.bList, nodeCmp.bList);
          }
          if(this.bStoreAttribsInNewContent != nodeCmp.bStoreAttribsInNewContent) {
            bOk = false;
            if(log !=null) log.writeError("ERROR CheckCfgSubtree bStoreAttribsInNewContent %s: %b ?? %b", this.tag, this.bStoreAttribsInNewContent, nodeCmp.bStoreAttribsInNewContent);
          }
          if(!this.cfgSubtreeName.equals(nodeCmp.cfgSubtreeName)) {
            bOk = false;
            if(log !=null) log.writeError("ERROR CheckCfgSubtree cfgSubtreeName %s: %s ?? %s", this.tag, this.cfgSubtreeName, nodeCmp.cfgSubtreeName);
          }
          if(this.dstClassName !=null && !this.dstClassName.equals(nodeCmp.dstClassName)) {
            bOk = false;
            if(log !=null) log.writeError("ERROR CheckCfgSubtree dstClassName %s: %s ?? %s", this.tag, this.dstClassName, nodeCmp.dstClassName);
          }
          bOk &= cmpDataAccess(this.tag, "contentStorePath", this.contentStorePath, nodeCmp.contentStorePath, log);
          bOk &= cmpDataAccess(this.tag, "elementStorePath", this.elementStorePath, nodeCmp.elementStorePath, log);
          bOk &= cmpDataAccess(this.tag, "elementFinishPath", this.elementFinishPath, nodeCmp.elementFinishPath, log);
          bOk &= cmpDataAccess(this.tag, "nameSpaceDef", this.nameSpaceDef, nodeCmp.nameSpaceDef, log);
          if(!this.tag.equals(nodeCmp.tag)) {
            bOk = false;
            if(log !=null) log.writeError("ERROR CheckCfgSubtree tag %s: %s ?? %s", this.tag, this.tag, nodeCmp.tag);
          }
        }
        if(!bOk) {
          Debugutil.stop();
        }
      } catch(Exception exc) {
        if(log !=null) log.writeError("ERROR CheckCfgSubtree unexpected exception", exc);
      }
      return bOk;
    }
    

    
    private boolean cmpDataAccess(CharSequence tag, String what, DataAccess.DatapathElement d1, DataAccess.DatapathElement d2, LogMessage log) {
      if(d1 == null ) return true;   // not given in current, but given in subtree node
      if(d2 == null ) {
        return true;                 // only given in current
      }
      if(!d1.toString().equals(d2.toString())) {
        if(log !=null) log.writeError("ERROR CheckCfgSubtree %s dataAccess.%s: %s ?? %s", tag, what, d1, d2);
        return false;
      } else {
        //TODO check the attributes
        return true;
      }
    }
    
  
    @Override public String toString(){ 
      if(this.subnodes !=null && this.subnodes.size() >=1) {
        XmlCfgNode subnode0 = this.subnodes.get("?");
        if(subnode0 == this) {                             // special case, if the node itself is also in the subtree.
          return this.tag + (this.attribs != null ? " attr:" + this.attribs.toString():"") + " nodes: ?: own";
        }                                                  // without the special handling an infinite recursively is forced.
      }
      return this.tag + (this.attribs != null ? " attr:" + this.attribs.toString():"") 
      + (this.subnodes !=null ? " nodes...:" /*+ this.subnodes.toString()*/ : ""); 
    }
    
  
  
  }

}
