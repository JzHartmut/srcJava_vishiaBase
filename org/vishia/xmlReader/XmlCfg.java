package org.vishia.xmlReader;

import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.vishia.util.DataAccess;
import org.vishia.util.Debugutil;
import org.vishia.util.StringFunctions;
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
  public static final String version = "2022-06-06";


  /**Assignment between nameSpace-value and nameSpace-alias gotten from the xmlns:ns="value" declaration in the read cfg.XML file. 
   * If this table is null than the config file is read yet. */
  Map<String, String> xmlnsAssign;

  /**Configuration for subtrees which can occur anywhere in the XML file. A subtree have to be determined by a tag name of its root element.
   * With them it can be found here. */
  Map<String, XmlCfgNode> subtrees;
  
  /**entries of usage of subtree instances which are defined later in the text. */
  Map<String, List<XmlCfgNode>> subtreeForward;
  
  XmlCfgNode rootNode = new XmlCfgNode(null, this, "root");

  
  Map<String, DataAccess.IntegerIx> attribNameVale;
  
  
  /**Creates the configuration to read a config.xml file.
   * @return instance
   */
  static XmlCfg newCfgCfg()
  { XmlCfg cfgCfg = new XmlCfg();
    cfgCfg.rootNode = new XmlCfg.XmlCfgNode(null, cfgCfg, null);  //The rootnode of the cfg is only formalistic.
    
    XmlCfg.XmlCfgNode rootNode = new XmlCfg.XmlCfgNode(null, cfgCfg, "xmlinput:root");  //<xmlinput:cfg as node 2. level
    cfgCfg.rootNode.addSubnode(rootNode.tag.toString(), rootNode);        //The cfg file should start with a <xmlinput:root
    //because nodes.setNewElementPath(...) is not set, the same rootNode acts as cfg for the 2. level. 
    //It is possible to add <xmlinput:root...> as 2. level, but it is not do so
    
    XmlCfg.XmlCfgNode cfgNode = new XmlCfg.XmlCfgNode(null, cfgCfg, "xmlinput:cfg");  //<xmlinput:cfg as node 2. level
    rootNode.addSubnode(cfgNode.tag.toString(), cfgNode);    //2. level add to root node too.
    
    //The next XmlCfgNode is used for all subnodes of the <xmlinput.cfg...
    XmlCfg.XmlCfgNode nodes = new XmlCfg.XmlCfgNode(null, cfgCfg, null); //Any unspecific nodes.
    cfgNode.addSubnode("?", nodes);    //2. level add to root node too.
    nodes.addSubnode("?", nodes);
    
    
//    cfgNode.subNodeUnspec = nodes; //it is set to use for all nodes with 
//    nodes.subNodeUnspec = nodes;  //recursively, all children are unspec.
    
    try {
      nodes.setNewElementPath("!newElement(tag)");
      //if the attribute xmlinput:data is read in the input config.xml, then its values should be used to set the datapath for the element.
      //It is done via invocation of setNewElementPath(...) on the output config.
      nodes.addAttribStorePath("xmlinput:data", "!setNewElementPath(value)");  
      nodes.addAttribStorePath("xmlinput:finish", "!setFinishElementPath(value)");  
      nodes.addAttribStorePath("xmlinput:class", "!dstClassName");  //This attribute value should be used to store locally in name.
      nodes.addAttribStorePath("xmlinput:list", "!setList()");  
      nodes.setContentStorePath("!setContentStorePath(text)");
      StringPartScan spAttribStorePath = new StringPartScan("addAttribStorePath(name, value)");
      nodes.attribsUnspec = new DataAccess.DatapathElement(spAttribStorePath, nodes.allArgNames, null);  //use addAttributeStorePath in the dst node to add.
      
      //Nodes for the config-subtree
      XmlCfg.XmlCfgNode nodeSub = new XmlCfg.XmlCfgNode(null, cfgCfg, "xmlinput:subtree");
      rootNode.addSubnode(nodeSub.tag.toString(), nodeSub);
      //nodeSub.attribsForCheck = new IndexMultiTable<String, AttribDstCheck>(IndexMultiTable.providerString); 
      //AttribDstCheck checkName = new AttribDstCheck(true);
      //nodeSub.attribsForCheck.put("name", checkName);
      nodeSub.addAttribStorePath("xmlinput:name", "!@subtreename");  //This attribute value should be used to store locally in name.
      nodeSub.addAttribStorePath("xmlinput:class", "!dstClassName");  //This attribute value should be used to store locally in name.
      nodeSub.addAttribStorePath("xmlinput:list", "!setList()");  
      nodeSub.addAttribStorePath("xmlinput:data", "!setNewElementPath(value)");  
      nodeSub.addAttribStorePath("xmlinput:finish", "!setFinishElementPath(value)");  
      nodeSub.setNewElementPath("!addSubTree(subtreename)");
      spAttribStorePath.assign("addAttribStorePath(name, value)");
      nodeSub.attribsUnspec = new DataAccess.DatapathElement(spAttribStorePath, nodes.allArgNames, null);  //use addAttributeStorePath in the dst node to add.
      nodeSub.addSubnode("?", nodes);
  //    nodeSub.subNodeUnspec = nodes;  //recursively, all children are unspec.
      //nodeSub.addAttribStorePath("xmlinput:data", "!addSubTree(name)");  //This attribute should be used to set the datapath for this element.
    }
    catch(ParseException exc) {
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
    XmlCfgNode subtreeRoot = new XmlCfgNode(null, this, name); //The root for a subtree configuration structure.
    if(subtrees == null) { subtrees = new TreeMap/*IndexMultiTable*/<String, XmlCfgNode>(/*IndexMultiTable.providerString*/); }
    subtrees.put(sname, subtreeRoot);  //config-global types of subtrees
    return subtreeRoot;
  }
  


  void transferNamespaceAssignment(Map<String, String> src) {
    xmlnsAssign = new TreeMap/*IndexMultiTable*/<String, String>();
    for(Map.Entry<String, String > ens: src.entrySet()) {
      String nsKey = ens.getKey();
      String nsPath = ens.getValue();
      xmlnsAssign.put(nsPath, nsKey);  //translate the found path in the XML source to the nameSpace key used in the config.xml 
    }
    src.clear();
    
  }

  
  
  
  
  void finishReadCfg(Map<String, String> namespaces) {
    this.transferNamespaceAssignment(namespaces);
    
    if(this.subtrees !=null) for(Map.Entry<String, XmlCfgNode> e : this.subtrees.entrySet()) {
      XmlCfgNode subtree = e.getValue();
      subtree.dstClassName = subtree.tag.toString();
      //
      //check whether other subtrees has using this subtree as forward:
      List<XmlCfgNode> subtreeUsage = this.subtreeForward ==null ? null: this.subtreeForward.get(subtree.tag.toString());
      if(subtreeUsage !=null) {
        for(XmlCfgNode e1: subtreeUsage) {
          e1.addFromSubtree(subtree);
        }
      }
      //
    }
    this.subtreeForward = null; //processed, no more necessary.
    
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
    
  } //class
  
  
  
  /**This class describes one node as pattern how the content of a parsed xml file should be stored.
   * It is an output instance while the config.xml is read. 
   *
   */
  public static class XmlCfgNode
  {
    /**The whole config. Especially {@link XmlCfg#xmlnsAssign} is used to evaluate attributes. 
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
      if(!dstPath.startsWith("!")) throw new IllegalArgumentException("The store path in xmlInput:data= \"" + dstPath + "\" in config.xml should start with ! because it is a store path.");
      //this.allArgNames = new TreeMap<String, DataAccess.IntegerIx>();
      StringPartScan spPath = new StringPartScan(dstPath.substring(1));
      spPath.setIgnoreWhitespaces(true);
      this.elementStorePath = new DataAccess.DatapathElement(spPath, allArgNames, null);  //gathered necessary names.
      if(this.allArgNames.size() ==0) {
        this.allArgNames = null; //not necessary.
      }
    }
  
    
    /**Sets the path for the "set element" invocation see {@link XmlCfgNode#elementFinishPath}.
     * @param dstPath either a method or an access to a field.
     * @throws ParseException 
     */
    public void setFinishElementPath(String dstPath) throws ParseException {
      if(!dstPath.startsWith("!")) throw new IllegalArgumentException("The store path in xmlInput:data= \"" + dstPath + "\" in config.xml should start with ! because it is a store path.");
      //this.allArgNames = new TreeMap<String, DataAccess.IntegerIx>();
      StringPartScan spPath = new StringPartScan(dstPath.substring(1));
      spPath.setIgnoreWhitespaces(true);
      this.elementFinishPath = new DataAccess.DatapathElement(spPath, allArgNames, null);  //gathered necessary names.
      if(this.allArgNames.size() ==0) {
        this.allArgNames = null; //not necessary.
      }
    }
  
    
    public void setList() {
      bList = true;
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
        XmlCfgNode subtree = this.cfg.subtrees.get(sAttrValue);
        if(subtree == null) {
          //not found or later in file. 
          if(cfg.subtreeForward == null) { cfg.subtreeForward = new TreeMap<String, List<XmlCfgNode>>(); }
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
      else if(  attribsForCheck !=null     //The attribsForCheck was set because a primary config node with bCheckAttributeNode was found before
        && (attribForCheck = attribsForCheck.get(key))!=null
        && attribForCheck.bUseForCheck
        ) {
          //tag is a StringBuilder in that case.
        ((StringBuilder)tag).append('@').append(key).append("=\"").append(sAttrValue).append("\"");
        //Note: this instance is not added in the subnodes in the parent yet, because the key is completed here.
        //Because the subnode of cfg should have any content the setContentStorePath(...) is called.
        //There it is added to parent.
      }
      else if(sAttrValue.length() >0) {  //empty attribute value in cfg: ignore attribute.
        if(attribs == null) { attribs = new TreeMap<String, AttribDstCheck>(); } //IndexMultiTable<String, AttribDstCheck>(IndexMultiTable.providerString); }
        AttribDstCheck dPathAccess;
        if(!StringFunctions.startsWith(sAttrValue, "!")) throw new IllegalArgumentException("read config: store path should start with !");
        //
        String dstPath;
        if(StringFunctions.equals(sAttrValue, "!CHECK")) {
          //use the attribute value as key for select the config and output, it is the primary config node
          dstPath = null;
          dPathAccess = new AttribDstCheck(key, true);
          attribs.put(key,  dPathAccess); //create if not exists
          bCheckAttributeNode = true;       
        }
        else  {
          dPathAccess = new AttribDstCheck(key, false);
          attribs.put(key,  dPathAccess);
          dstPath = sAttrValue.substring(1);
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
            bStoreAttribsInNewContent = true;  
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
    
    
    void addFromSubtree(XmlCfgNode subtree) {
      //Use the same subtree information for all subtree references. 
      //If the data would be copied, some more memory is neccessary, but not too much. But it is not necessary to copy.
      this.subnodes = subtree.subnodes;
      if(subtree.attribs !=null) for(Map.Entry<String, AttribDstCheck> e : subtree.attribs.entrySet()) {
        String attrName = e.getKey();
        if(this.attribs == null) { this.attribs = new TreeMap/*IndexMultiTable*/<String, AttribDstCheck>(); }
        AttribDstCheck attr = this.attribs.get(attrName);
        if(attr == null) {
          this.attribs.put(attrName, e.getValue());  //store the attrib from the subtree instance.
        } //else: The attrib in the subtree call wins. (new since 2019-03, before: both are equal)
      }
      if(this.dstClassName == null) { this.dstClassName = subtree.dstClassName; }
    }
    
    
    
    void addSubnode(String key, XmlCfgNode node) {
      if(subnodes == null) { subnodes = new TreeMap/*IndexMultiTable*/<String, XmlCfgNode>(); }
      if(key.startsWith("Object@"))
        Debugutil.stop();
      if(key.startsWith("Array@"))
        Debugutil.stop();
      subnodes.put(key, node);
    }
  
  
    /**This method is invoked from the xml configuration reader to create a new subNode for a found elmeent.
     * @param name ns:tag of the found element in the config.xml 
     * @return the sub node
     */
    XmlCfgNode newElement(CharSequence name) {
      String sname = name.toString();
      XmlCfgNode subNode = null;
      if(subnodes !=null) {
        XmlCfgNode subNodeForCheck = subnodes.get(sname);  //check whether a subNode with this key is existing already,
        if(subNodeForCheck !=null) {
          if(!subNodeForCheck.bCheckAttributeNode) {
            throw new IllegalArgumentException("XmlReader-cfg: An element has more as one node with the same tag name. This is only admissible if the first node contains a \"!CHECK\" attribute.");
          } else {
            StringBuilder tagBuffer = new StringBuilder(64); tagBuffer.append(subNodeForCheck.tag); //append more @attrib="value" for the key in tag.
            subNode = new XmlCfgNode(this, cfg, tagBuffer);
            subNode.attribsForCheck = subNodeForCheck.attribs;
            //Note: this subnode is not added in the subnodes in the parent yet, because the key is completed here.
            //Because the subnode of cfg should have any content the setContentStorePath(...) is called.
            //There it is added to parent.
          }
        }
        //than it is the first subnode with some  <tag attr="!CHECK"/> entries. Use it to work with it.
      } 
      if(subNode == null) {
        subNode = new XmlCfgNode(this, cfg, sname);  //A sub node for the config.
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
    void setContentStorePath(String text) throws ParseException {
      if(this.tag instanceof StringBuilder) { //it is a second node with same tag, but with attributes for check.
        if(this.tag.toString().startsWith("Object@"))
          Debugutil.stop();
        if(this.tag.toString().startsWith("Array@"))
          Debugutil.stop();
        this.parent.subnodes.put(this.tag.toString(), this); //put this node in its parent, it is not done yet. 
      }
      if(!text.startsWith("!")) throw new IllegalArgumentException("Any content of a config.xml should start with ! because it is a store path.");
      StringPartScan sp = new StringPartScan(text.substring(1));
      
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
      //subtreenode.addAttribStorePath("xmlinput:name", name.toString());
      subtreenode.cfgSubtreeName = name.toString();
      return subtreenode;
    }
    
    
  
    @Override public String toString(){ 
      if(this.subnodes !=null && this.subnodes.size() >=1) {
        XmlCfgNode subnode0 = this.subnodes.get("?");
        if(subnode0 == this) {                             // special case, if the node itself is also in the subtree.
          return this.tag + (this.attribs != null ? " attr:" + this.attribs.toString():"") + " nodes: ?: own";
        }                                                  // without the special handling an infinite recursively is forced.
      }
      return this.tag + (this.attribs != null ? " attr:" + this.attribs.toString():"") 
      + (this.subnodes !=null ? " nodes:" + this.subnodes.toString() : ""); 
    }
    
  
  
  }

}
