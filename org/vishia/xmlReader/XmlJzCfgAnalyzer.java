package org.vishia.xmlReader;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.vishia.util.Assert;
import org.vishia.util.Debugutil;
import org.vishia.util.IndexMultiTable;
import org.vishia.util.StringFunctions_B;
import org.vishia.xmlSimple.SimpleXmlOutputter;
import org.vishia.xmlSimple.XmlException;
import org.vishia.xmlSimple.XmlNode;
import org.vishia.xmlSimple.XmlNodeSimple;


/**This class reads any XML file and writes its structure to a XmlCfg format. 
 * With it the xmlCfg to read that file to data can be prepared.
 * @author Hartmut Schorrig
 *
 */
public class XmlJzCfgAnalyzer
{
  /**Version, License and History:
   * <ul>
   * <li>2022-06-06 Hartmut: produces the "xmlinput:finish" entry, see {@link XmlCfg}.
   * <li>2022-06-06 Hartmut: regards all namespace entries
   * <li>2019-08-18 improvements with namespace and usage for {@link GenXmlCfgJavaData}.
   * <li>2018-09-10 writes cfg attributes.
   * <li>2018-08-15 created.
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
  public static final String version = "2018-08-15";

  
  static class AttribRead {
    String namespace;
    String name;
    /**storage type or access routine */
    String value;
  }




  int debugStopLineXmlInp = -1;
  
  /**The common structure data of the read XML file. */
  final XmlStructureData xmlStructData = new XmlStructureData();
  
  /**The tree of the structure of the read XML file. It contains the same tag element only one time.
   * It is tested in {@link XmlStructureNode#addElement(String)}. */
  XmlStructureNode xmlStructTree = new XmlStructureNode(null, "root", xmlStructData);  //the root node for reading config

  /**The declared name spaces found in all nodes. */
  Map<String, String> nameSpacesAll;   //TODO use it.
  

  
  public XmlJzCfgAnalyzer(){
    
  }
  
  /**Only for internal debug. See implementation. There is a possibility to set a break point if the parser reaches the line.
   * @param line
   */
  public void setDebugStop(int line) {
    debugStopLineXmlInp = line;
  }
 
  
  
  
  public void writeCfgTemplate(File wrCfg) {
    FileWriter writer = null;
    try {
      //Output it as cfg.xml
      XmlNodeSimple<?> root = new XmlNodeSimple<>("xmlinput:root");
      root.addNamespaceDeclaration("xmlinput", "www.vishia.org/XmlReader-xmlinput");
      for(Map.Entry<String, String> en: this.nameSpacesAll.entrySet()) {
        root.addNamespaceDeclaration(en.getKey(), en.getValue());
      }
      for(XmlStructureNode structnode: this.xmlStructData.cfgSubtreeList) {
        //add one subtree node for each tag type in its context:
        assert(structnode.sSubtreenode !=null);  //it should be designated.
        XmlNode wrCfgsubtreenode = root.addNewNode("subtree", "xmlinput"); //second node "cfg"
        if(structnode.sSubtreenode.equals("ObjectList_A"))
          Debugutil.stop();
        wrCfgsubtreenode.setAttribute("name", "xmlinput", structnode.sSubtreenode);
        String sClass = StringFunctions_B.replaceNonIdentifierChars(structnode.sSubtreenode, '-').toString();
        wrCfgsubtreenode.setAttribute("class", "xmlinput", sClass);
        if(structnode.attribs !=null)for(Map.Entry<String, AttribRead> e_attrib: structnode.attribs.entrySet()) {
          AttribRead attrib = e_attrib.getValue();
//          if(attrib.name.equals("idref"))
//            Debugutil.stop();
          wrCfgsubtreenode.setAttribute(attrib.name, attrib.namespace, attrib.value);
        }
        wrSetAddContentAttrib(structnode, wrCfgsubtreenode);
        //
        if(structnode.nodes !=null) for(Map.Entry<String, XmlStructureNode> e_srcSubnode: structnode.nodes.entrySet()) {
          XmlStructureNode srcSubnode = e_srcSubnode.getValue();
          XmlNodeSimple<?> xmlNodeSub = new XmlNodeSimple<>(srcSubnode.tag);
          wrCfgsubtreenode.addContent(xmlNodeSub);
          addWrNode(xmlNodeSub, srcSubnode, 999);
        }
      }
      //add the root cfg node with all its subnodes:
      XmlNode cfgnode = root.addNewNode("cfg", "xmlinput"); //second node "cfg"
      //
      //add all nodes from data, recursively called.
      addWrNode(cfgnode, xmlStructTree, 999);  
      //
      SimpleXmlOutputter oXml = new SimpleXmlOutputter();
      writer = new FileWriter(wrCfg);
      oXml.write(writer, root);
      writer.close();
    } catch(XmlException | IOException exc) {
      CharSequence error = Assert.exceptionInfo("unexpected", exc, 0, 100);
      System.err.append(error);
    } finally {
      if(writer !=null) {
        try { writer.close(); } catch(IOException exc) { System.err.append("cannot close"+wrCfg.getAbsolutePath());}
      }
    }
    
    Debugutil.stop();
  }


  
  
  
  
  private void wrSetAddContentAttrib(XmlStructureNode structNode, XmlNode wrCfgXmlNode) throws XmlException {
    CharSequence sArg;
    if(structNode.attribs !=null) {
      StringBuilder uArg= new StringBuilder(100);
      sArg = uArg;
      char sep = '(';
      for(Map.Entry<String,AttribRead> e: structNode.attribs.entrySet()) {
        AttribRead attrib = e.getValue();
        wrCfgXmlNode.setAttribute(attrib.name, attrib.namespace, attrib.value);  //transfer to cfg too.
        if(attrib.value.startsWith("!@")) {  //use only attributes which should be used as arguments for the set/add operation
          uArg.append(sep).append(attrib.value.substring(2));
          sep = ',';
        }
      }
      uArg.append(')');
    } else {
      sArg = "()";
    }                                            // writes the xmlinput:data="!new_Tag(...)" to create data for the sub nodes and attribs
    wrCfgXmlNode.setAttribute("data", "xmlinput", "!new_" + structNode.tagIdent + sArg);
    final String setAdd;
    if(structNode.onlySingle) {                 // writes the xmlinput:finish="!set_Tag(data)" to set the data. 
      setAdd = "set_";
    } else {
      setAdd = "add_";
      wrCfgXmlNode.setAttribute("list", "xmlinput", "");
    }
    wrCfgXmlNode.setAttribute("finish", "xmlinput", setAdd + structNode.tagIdent + "(value)");
    
  }
  
  
  
  
  /**Adds the node and recursively all sub nodes from {@link XmlStructureNode}
   * It is invoked after a XML file war read with {@link XmlCfg#newCfgReadStruct()} 
   * @param wrCfgXmlNode The xml node for output to add.
   * @param structNode The node from the structure of the read XML file, 
   * @param recursion decremented, exception on <=0
   * @throws XmlException on XML error, IllegalArgumentException on recursion error.
   */
  private void addWrNode(XmlNode wrCfgXmlNode, XmlStructureNode structNode, int recursion) throws XmlException {
    if(recursion <0) throw new IllegalArgumentException();
    if(structNode.nodes !=null || structNode.attribs !=null) {
      
      wrSetAddContentAttrib(structNode, wrCfgXmlNode);  //sets the xmlinput:data="..." and xmlinput:list=""
      if(structNode.sSubtreenode !=null) {
        //The tag type occurs more as one time in different situations, but with the same meaning. Use subtree in cfg.
        String sSubtreeName = structNode.sSubtreenode;
        wrCfgXmlNode.setAttribute("subtree", "xmlinput", sSubtreeName);
      } else {
        //Only one time tag type: tree inside.
        if(structNode.nodes !=null || structNode.attribs !=null && (structNode.bText || structNode.attribs.size() >1)) {
          //sub nodes or more as one (attribute or text content) needs a sub class to store the content for this node. 
          String sClass = StringFunctions_B.replaceNonIdentifierChars(structNode.tagIdent, '-').toString();
          wrCfgXmlNode.setAttribute("class", "xmlinput", sClass  );
        }
        if(structNode.nameSpaces !=null) {
          for(Map.Entry<String, String> e: structNode.nameSpaces.entrySet()) {
            wrCfgXmlNode.addNamespaceDeclaration(e.getKey(), e.getValue());
          }
        }
        if(structNode.nodes !=null) { //has subnodes
          for(Map.Entry<String, XmlStructureNode> e: structNode.nodes.entrySet()) {
            XmlStructureNode subnode = e.getValue();
            String tag = e.getKey();
            XmlNodeSimple<?> xmlNodeSub = new XmlNodeSimple<>(tag);
            wrCfgXmlNode.addContent(xmlNodeSub);
            addWrNode(xmlNodeSub, subnode, recursion-1);
        } }
      }
      if(structNode.bText) {
        wrCfgXmlNode.addContent("!set_text(text)");
      }
    } else { //no attribs, no sub tree, only the text content should be stored.
      if(structNode.onlySingle) {
        wrCfgXmlNode.addContent("!set_" + structNode.tag + "(text)");  //set_TAG(String text)
      } else {
        wrCfgXmlNode.addContent("!add_" + structNode.tag + "(text)");
      }
    }
  }


  /**Reads any XML file and stores the structure in the {@link #xmlStructTree}.
   *  
   * @param fXmlIn
   * @throws IOException 
   */
  public void readXmlStruct(File fXmlIn) throws IOException {
    XmlJzReader xmlReader = new XmlJzReader();
    if(this.debugStopLineXmlInp >0) {
      xmlReader.setDebugStop(this.debugStopLineXmlInp);
    }
    XmlCfg cfg = newCfgReadStruct();   // use this special config file to read all data
    xmlReader.readXml(fXmlIn, this.xmlStructTree, cfg);
    this.xmlStructData.checkCfgSubtree();   //removeSingleEntries();
    this.nameSpacesAll = xmlReader.namespaces;
    Debugutil.stop();
  }


  
  /**Creates the configuration to read any xml file to store its structure especially in {@link XmlJzCfgAnalyzer.XmlStructureNode}.
   * @return instance of XmlCfg to do so.
   */
  public static XmlCfg newCfgReadStruct()
  { XmlCfg cfgCommon = new XmlCfg();
    cfgCommon.rootNode = new XmlCfg.XmlCfgNode(null, cfgCommon, null);  //The rootnode of the cfg is only formalistic.
    
    XmlCfg.XmlCfgNode rootNode = new XmlCfg.XmlCfgNode(null, cfgCommon, "?");  //<xmlinput:cfg as node 2. level
    cfgCommon.rootNode.addSubnode(rootNode.tag.toString(), rootNode);        //The cfg file should start with a <xmlinput:root
    rootNode.addSubnode(rootNode.tag.toString(), rootNode);        //The cfg file should start with a <xmlinput:root
    
    try {
      //On any element the 'addElement(tag)' is invoked via Reflection. 
      rootNode.setNewElementPath("!addElement(tag)");  //executed in the data destination instance.
      rootNode.addAttribStorePath("?", "!setAttribute(name)"); 
      rootNode.setNameSpaceStorePath("!addNamespace(name, value)");
      rootNode.setContentStorePath("!setTextOccurrence()");
    }
    catch(ParseException exc) {
      throw new RuntimeException(exc); //it is unexpected
    }
    return cfgCommon;
  }

  

  
  
  /**Reads a XML file and writes its structure as cfg.xml to help creating a config for the XmlJzReader
   * @param args pathfileIn.xml pathfileOutcfg.xml 
   */
  public static void main(String[] args) {
    XmlJzCfgAnalyzer main = new XmlJzCfgAnalyzer();
    if(args.length == 0) {
      System.out.println("java -cp .... org.vishia.xmlReader.XmlJzCfgAnalyzer INFILE OUTFILE\n"
          + "  INFILE: Any user xml file\n"
          + "  OUTFILE: A config.xml file as template to create the config file for XmlJzReader\n");
    } else {
      try {
        main.readXmlStruct(new File(args[0]));
        //JZtxtcmdTester.dataHtmlNoExc(main.data, new File("T:/datashow.html"), true);
        main.writeCfgTemplate(new File(args[1]));
      } catch (Exception e) {
        System.err.println("Unexpected: " + e.getMessage());
        e.printStackTrace(System.err);
      }
    }
  }
  
  
  
  static class XmlStructureData {
    
    /**Contains all elements with its {@link #occurrence}.
     *
     */
    static class CfgSubtreeType {
      
      List<XmlStructureNode> occurrence = new ArrayList<XmlStructureNode>();
      
      public CfgSubtreeType(){}
      
      @Override public String toString() { return "" + occurrence.size() + " * " + occurrence.get(0).toString(); }
    }
    
    /**Contains info about equals element types.
     */
    static class CfgSubtreeType2 {
      
      final String tag;
      //XmlStructureNodeBuilder node;
      
      /**Contains all nodes in all levels which are detect as same type.*/
      List<XmlStructureNode> occurrence = new ArrayList<XmlStructureNode>();
      
      /**The representative attributes and sub nodes of all associated nodes. */
      XmlStructureNode representative;
      
      List<CfgSubtreeType2> dependings = new ArrayList<CfgSubtreeType2>();
      
      boolean bSort;
      
      Map<String, String> attributeNames = new TreeMap<String, String>();
      Map<String, String> nodeNames = new TreeMap<String, String>();
      
      public CfgSubtreeType2(XmlStructureNode structNode){
        this.tag = structNode.tag;
        this.representative = structNode; //the first
        this.occurrence.add(structNode);
      }
      
      
      @Override public String toString() { return "" + occurrence.size() + " * " + occurrence.get(0).toString(); }
    }

    
    
    /**Stores all node types per tagName, with its occurence in the structure file. 
     * */
    TreeMap<String, CfgSubtreeType> allElementTypes = new TreeMap<String, CfgSubtreeType>();
   
    /**Stores all node types with occurrence more as one time, with tag name but with extra entry for any different content (really different type with same tag). */
    IndexMultiTable<String, CfgSubtreeType2> allElementTypes2 = new IndexMultiTable<String, CfgSubtreeType2>(IndexMultiTable.providerString);
   
    
    
    /**Stores all node types for cfg subtree with the subtree name as key. */
    Map<String, XmlStructureNode> cfgSubtreeByName = new TreeMap<String, XmlStructureNode>();
    
    /**Stores the cfg subtree in the usage order. */
    List<XmlStructureNode> cfgSubtreeList = new ArrayList<XmlStructureNode>();
    
    
    
    private void createCfgSubtree(XmlStructureNode node, char nameModif) {
      CfgSubtreeType2 cfgSubtreeType = new CfgSubtreeType2(node);
      if(nameModif < 'A') {
        node.sSubtreenode = node.tag;
      } else {
        node.sSubtreenode = node.tag + '_' + nameModif;
      }
      if(node.attribs !=null) for(Map.Entry<String, AttribRead> e: node.attribs.entrySet()) {
        cfgSubtreeType.attributeNames.put(e.getKey(), e.getKey());
      }
      if(node.nodes !=null) for(Map.Entry<String, XmlStructureNode> e: node.nodes.entrySet()) {
        cfgSubtreeType.nodeNames.put(e.getKey(), e.getKey());
      }
      allElementTypes2.add(node.tag, cfgSubtreeType);  //more as one with same tag name possible, store only for comparison (check)
      cfgSubtreeByName.put(node.sSubtreenode, node);   //unique cfg-subtree key, for usage.

    }
    
    
    /**Adds the occurrence of a sub node in the struct data. This is independent whether a cfg-subtree should be created. It is input for build a cfg-subtree.
     * It is possible that a config-subtree should be built though this node has no sub nodes.
     * @param node
     */
    void addStructureNodeOccurence(XmlStructureNode node) {
      CfgSubtreeType cfgSubtreeWithAllOccurences = allElementTypes.get(node.tag); 
      if(cfgSubtreeWithAllOccurences == null) {
        cfgSubtreeWithAllOccurences = new CfgSubtreeType();
        allElementTypes.put(node.tag, cfgSubtreeWithAllOccurences);
      }
      cfgSubtreeWithAllOccurences.occurrence.add(node);
    }
    
    
    
    /**Checks whether a element type is already existing with a score. 
     * 3/4 of all found attributes and sub elements are identically with another existing element
     * with the same tag name. 
     * Invoked for all element types found in the source XML tree.
     * If another node is found, the appropriated {@link CfgSubtreeType2} in {@link #allElementTypes2}
     * is supplemented ({@link CfgSubtreeType2#representative}.
     * If the node is a new one, {@link #createCfgSubtree(XmlStructureNode, char)} is called.
     * @param node
     */
    private void checkStructureNodeOccurence(XmlStructureNode node) {
      if(node.nodes == null && node.attribs == null) {
        return; //without sub nodes: does not need to store as subtree.
      }
      Iterator<CfgSubtreeType2> iterCfgSubtrees = allElementTypes2.iterator(node.tag);
      CfgSubtreeType2 cfgSubtreeType;
      char nameModif = 'A'-1;
      boolean found = false;
      if(node.tag.equals("AttributeList"))
        Debugutil.stop();
      if(!iterCfgSubtrees.hasNext() || !(cfgSubtreeType = iterCfgSubtrees.next()).tag.equals(node.tag)) {
        createCfgSubtree(node, '\0'); //same tag only one time.
      }
      else { //Same tag more as one.
        do {
          nameModif +=1;
          //check whether the found cfgSubtree seems to be the same type, because it has the same children:
          int nrfound=0, nrcount=0;
          if(node.attribs !=null) for(Map.Entry<String, AttribRead> e: node.attribs.entrySet()) {
            String key = e.getKey();
            if(cfgSubtreeType.attributeNames.get(key) !=null) {
              nrfound +=1;
            }
            nrcount +=1;
          }
          Map<String, String> foundNodeNames = new TreeMap<String, String>();
          if(node.nodes !=null) for(Map.Entry<String, XmlStructureNode> e: node.nodes.entrySet()) {
            String key = e.getKey();
            if(foundNodeNames.get(key)==null) { //only any occurrence of a subnode type only as one time type occurrence.
              foundNodeNames.put(key, key);
              if(cfgSubtreeType.nodeNames.get(e.getKey()) !=null) {
                nrfound +=1;
              }
              nrcount +=1;
            } else {
              assert(false);
            }
          }
          if(nrfound < (nrcount+3) /4) { //most (3/4) nodes or attributes are non-identical: It is another cfgSubtree type.
          } else {
            found = true; //>= 3/4 all attrib and nodes are identically:
            cfgSubtreeType.occurrence.add(node);
            node.sSubtreenode = cfgSubtreeType.representative.sSubtreenode;
            assert(node.sSubtreenode !=null);
            if(node.attribs !=null) for(Map.Entry<String, AttribRead> e: node.attribs.entrySet()) {
              String key = e.getKey();
              if(cfgSubtreeType.attributeNames.get(key) ==null) {
                cfgSubtreeType.attributeNames.put(key, key);     //an attribute non detected as yet, add it in representative.;
                cfgSubtreeType.representative.attribs.put(key, e.getValue());
              }
            }
            if(node.nodes !=null) for(Map.Entry<String, XmlStructureNode> e: node.nodes.entrySet()) {
              String key = e.getKey();
              foundNodeNames.put(key, key);
              if(cfgSubtreeType.nodeNames.get(e.getKey()) ==null) {
                cfgSubtreeType.nodeNames.put(key, key);    //a node non detected as yet, add it in representative.
                cfgSubtreeType.representative.nodes.put(key, e.getValue());
              }
            }
            break;
          }
        } while(!found && iterCfgSubtrees.hasNext() && (cfgSubtreeType = iterCfgSubtrees.next()).tag.equals(node.tag));
        if(!found) {
          createCfgSubtree(node, nameModif);
        }
      }
    }
    
    
    
    
    private void checkCfgSubtree() {
      for(Map.Entry<String, CfgSubtreeType> e: allElementTypes.entrySet()) {
        CfgSubtreeType cfgSubtreeOccurrences = e.getValue();
        if(cfgSubtreeOccurrences.occurrence.size() >1) { 
          //only if the element with this tag name occurs more as one time in the structure tree
          for(XmlStructureNode structNode: cfgSubtreeOccurrences.occurrence ) {
            checkStructureNodeOccurence(structNode);
          }            
        }
        //XmlStructureNode node= e.getValue().representative;
      }
      //detect all dependencies in cfg-subtree
      for(Map.Entry<String, CfgSubtreeType2> e: allElementTypes2.entrySet()) {
        CfgSubtreeType2 cfgSubtree = e.getValue();
        XmlStructureNode node= e.getValue().representative;
        if(!node.bDependencyChecked) {
          node.bDependencyChecked = true;
          checkUsageSubtreenode(cfgSubtree, node, 99);
        }
      }
      for(Map.Entry<String, CfgSubtreeType2> e: allElementTypes2.entrySet()) {
        CfgSubtreeType2 cfgSubtree = e.getValue();
        processDependingCfgSubtree(cfgSubtree, 99);
      }
    }
    
    
    
    
    private void processDependingCfgSubtree(CfgSubtreeType2 cfgSubtree, int recursionCt) {
      if(recursionCt < 50)
        Debugutil.stop();
      assert(recursionCt >=0);
      if(!cfgSubtree.bSort ) {
        if(cfgSubtree.dependings.size()==0) {
          cfgSubtreeList.add(cfgSubtree.representative);
          cfgSubtree.bSort = true;
        } else {
          cfgSubtree.bSort = true;  //prevent recursion on own depending 
          for(CfgSubtreeType2 dep: cfgSubtree.dependings) {
            processDependingCfgSubtree(dep, recursionCt-1);
          }
          //all dependencies processed.
          cfgSubtreeList.add(cfgSubtree.representative);
        }
      }
    }
    
    
    
    
    private void checkUsageSubtreenode(CfgSubtreeType2 cfgSubtreeNeeds, XmlStructureNode node, int recursiveCt) {
      assert(recursiveCt >= 0);
      if(node.nodes !=null) for(Map.Entry<String,XmlStructureNode> e_subnode: node.nodes.entrySet()) {
        XmlStructureNode subnode = e_subnode.getValue();
        if(subnode.sSubtreenode !=null) { //need as subtree
          if(!subnode.bDependencyChecked) {
            subnode.bDependencyChecked = true;
            //cfgSubtreeList.add(0, subnode);  //add before the evaluated node.
            CfgSubtreeType2 cfgSubtreeSub;
            Iterator<CfgSubtreeType2> iterSearch = allElementTypes2.iterator(subnode.tag);
            while(iterSearch.hasNext() && (cfgSubtreeSub = iterSearch.next()).tag.equals(subnode.tag)) {
              if(cfgSubtreeSub.representative.sSubtreenode.equals(subnode.sSubtreenode)) {
                cfgSubtreeNeeds.dependings.add(cfgSubtreeSub);
                checkUsageSubtreenode(cfgSubtreeSub, cfgSubtreeSub.representative, recursiveCt -1);  //firstly check this usage.
                break;
              }
            }
        } }
        else {
          checkUsageSubtreenode(cfgSubtreeNeeds, subnode, recursiveCt -1);  //not in cfg-subtree, process it.
        }
      }
    }
    
    
    
    
    
    
    
    
  }
  
  
  

  /**This class is one node for one element to store the structure of a XML file via {@link XmlJzReader}.
   * <ul>
   * <li>An instance of this class should be created as root node. The root node from the XML file is stored there as the first level.
   * It means, this root instance is not presented in the XML file, and the root instance has one member: The root node of the XML file.
   * <li>Instances are created for any found element with different tags in the XML tree. This instances are stored in the internal {@link #nodes} tree.
   * <li>If elements with the same tag name are found on one level of the XML file, only one instance of this is stored in {@link #nodes}.
   *   It means an instance presents not the content of the XML file but the structure. An instance of this contains the sum of all found attributes.
   * <li>If an element with a known tag is found in a deeper level of the XML file,    
   * </ul> 
   * The information of the built tree of this class can be used to 
   * <ul>
   * <li>Write a config file template, using {@link XmlJzCfgAnalyzer#writeCfgTemplate(File)}
   * </ul>
   * This class builds the root node and any child node too.
   * <pre>
   * XmlCfgNodeBuilder<*>--nodes-->XmlCfgNodeBuilder
   *                                  <*> 
   *                                   |
   *                                   +--attribs--->String
   * </pre>
   * @author Hartmut Schorrig
   *
   */
  static class XmlStructureNode
  {
    
    /**Tag name of the element. */
    final String tag;
    
    final String tagIdent;
    
    String sSubtreenode;
    
    /**Set if at least one of the occurrences has a text content.*/
    boolean bText = false;
    
    boolean XXXbDetermineWithParent = false;
    
    boolean bDependencyChecked;
    
    
    //boolean bUseSubtree;
    
    /**Set if the node is only one time in all found trees. 
     * It means that a variable is sufficient to store the content of all attributes and the text if no subNodes exists.
     */
    boolean onlySingle = true;
    
    
    final XmlStructureNode parent;
    
    /**Found sub nodes. The list is supplemented if new sub nodes are found on further occurrences of elements. 
     * It contains the same tag element only one time. that is tested in {@link #addElement(String)}.
     */
    Map<String, XmlStructureNode> nodes;
    
    Map<String, String> nodesLocal;
    
    /**Found attributes. The list is supplemented if new attribute names are found on further occurrences of elements. */
    Map<String, AttribRead> attribs;
    
    /**The declared name spaces for this node. */
    Map<String, String> nameSpaces;
    
    int nrofAttributes = 0;
    
    /**Set if new attributes are found. Then the 
     * 
     */
    boolean bNewAttributes = false; 
    
    
    final XmlStructureData xmlStructData;
    
    XmlStructureNode(XmlStructureNode parent, String tag, XmlStructureData xmlStructData){ 
      this.parent = parent; 
      this.tag = tag;
      this.tagIdent = StringFunctions_B.replaceNonIdentifierChars(tag, '-').toString();
      
      this.xmlStructData = xmlStructData; 
    }
    
    
    /**Returns an instance to store the occurrence of a XML-element.
     * If more XML-elements with the same tag name are found, only one occurrence of this element type is stored.
     * If an element with the same tag name is given, which is already registered, then the same instance is returned from the found element before.
     * With them maybe new occurring attributes or new occurring elements are stored. 
     * It is invoked via reflection from {@link XmlJzCfgAnalyzer#newCfgReadStruct()}
     * @param tag
     * @return
     */
    public XmlStructureNode addElement(String tag) { 
      if(tag.contains("   "))
        Debugutil.stop();
      if(tag.equals("Document")) {
        Debugutil.stop();
      }
      if(this.nodes == null) {
        this.nodes = new TreeMap<String, XmlStructureNode>();
      }
      if(this.nodesLocal == null) {
        this.nodesLocal = new TreeMap<String, String>();
      }
      XmlStructureNode subNode = this.nodes.get(tag); //use existent one with same tag to strore further content.
      if(subNode == null) {
        subNode = new XmlStructureNode(this, tag, this.xmlStructData); 
        this.nodes.put( tag, subNode);
        this.xmlStructData.addStructureNodeOccurence(subNode);
      }
      if(this.nodesLocal.get(tag)!=null) { 
        subNode.onlySingle = false; //at least twice in this tree.
      }
      this.nodesLocal.put(tag, tag); //to detect whether it occurs a second one
      subNode.nodesLocal = null;
      return subNode; 
    }
    
    
    
    public void addNamespace(String key, String value) {
      if(this.nameSpaces == null) { this.nameSpaces = new TreeMap<String, String>(); }
      this.nameSpaces.put(key, value);
//TODO      if(nameSpacesAll == null) { this.nameSpacesAll = new TreeMap<String, String>(); }
//      this.nameSpacesAll.put(key, value);
    }
    
    /**It is invoked via reflection from {@link XmlJzCfgAnalyzer#newCfgReadStruct()}
     * @param tag
     * @return
     */
    public void setAttribute(String namespacename) { 
      AttribRead attrib = new AttribRead();
      //replaces the ':' between namespace:name with _
      attrib.value = "!@" + StringFunctions_B.replaceNonIdentifierChars(namespacename, '-').toString();
      int posNamespace = namespacename.indexOf(':');
      if(posNamespace >=0) {
        attrib.namespace = namespacename.substring(0, posNamespace);
        attrib.name = namespacename.substring(posNamespace+1);
      } else {
        attrib.name = namespacename;
      }
      if(this.attribs == null) { 
        this.attribs = new TreeMap<String, AttribRead>(); 
        this.bNewAttributes = true;
        this.attribs.put(namespacename, attrib);  //the first attrin
      }
      else if(this.attribs.get(namespacename) ==null) {
        this.bNewAttributes = true;
        this.attribs.put(namespacename, attrib);  //a new attrib
      }
    }

    
    public void setTextOccurrence() { this.bText = true; }
   
    
    @Override public String toString(){ return this.tag + (this.attribs != null ? " attr:" + this.attribs.toString():"") + (this.nodes !=null ? " nodes:" + this.nodes.toString() : ""); }
    
  }

  
  
}
