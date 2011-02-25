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
 * 4) But the LPGL ist not appropriate for a whole software product,
 *    if this source is only a part of them. It means, the user 
 *    must publish this part of source,
 *    but don't need to publish the whole source of the own product.
 * 5) You can study and modify (improve) this source 
 *    for own using or for redistribution, but you have to license the
 *    modified sources likewise under this LGPL Lesser General Public License.
 *    You mustn't delete this Copyright/Copyleft inscription in this source file.    
 *
 * @author www.vishia.de/Java
 * @version 2006-06-15  (year-month-day)
 * list of changes: 
 * 2006-05-00 JcHartmut: creation
 *
 ****************************************************************************/
package org.vishia.xml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

//import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
//import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;

//import net.sf.saxon.Configuration;
//import net.sf.saxon.TransformerFactoryImpl;
//import net.sf.saxon.jdom.DocumentWrapper;
//import net.sf.saxon.om.NodeInfo;
//import net.sf.saxon.tinytree.TinyBuilder;
//import net.sf.saxon.tinytree.TinyTree;
//import net.sf.saxon.tree.TreeBuilder;



/**This class helps to transform XML documents. Internally it uses the saxon transformer.  
 * The separability to saxon is, the output will not write only to a file,
 * it may be postprocessed, therefor an alternativ output of the xml tree is used. 
 * <br>
 * TODO: here are some tryings to output to TinyTree, Jdom and others,
 * but there are no prosperity now. It is indefinite here now, how to evaluate
 * and change a tree with the saxon object model. 
 * Therefore only the simple output method is useable, see comments.
 */

public class XmlMTransformer
{

  /**The transformer containing the XSL stylesheet. It should be an instance
   * of SAXON. But the type is defined in javax.xml.transform.
   */
  final Transformer transformer;
  
  /**The Source for transformation.*/
  final Source xmlSource;
  
  /**Creates the instance with given Transformer and given Source.
   * The instance type of Source should be matches to the Transformer. 
   * @param source
   * @param transformer
   */
  XmlMTransformer(Source source, Transformer transformer)
  { this.transformer = transformer;
    this.xmlSource = source;
  }
  
  /**Transforms and writes the result XML directly to the given file.
   * 
   * @param fileOut
   * @throws FileNotFoundException
   * @throws TransformerException
   */
  void transformToFile(File fileOut) 
  throws FileNotFoundException, TransformerException
  {
    transformer.transform(xmlSource, new StreamResult(new FileOutputStream(fileOut)));
  }
  
  
  /**Transforms and writes the result in a SAXON TinyTree.
   * This is tested, it works good.
   * @return
   * @throws TransformerException
   */
/*
  NodeInfo transformToTinyTree() 
  throws TransformerException
  {
    TinyBuilder builder = new TinyBuilder();
    transformer.transform(xmlSource, builder);
    TinyTree treeOut = builder.getTree();
    NodeInfo xmlOut = treeOut.getNode(0); //it should be the root node (?)
    return xmlOut;
  }
*/  
  
  
  /**Transforms and writes the result in a SAXON Tree.
   * This is not tested, how to get the root node?
   * @return
   * @throws TransformerException
   */
/*
  NodeInfo transformToSaxonTree() 
  throws TransformerException
  {
    TreeBuilder builder = new TreeBuilder();
    transformer.transform(xmlSource, builder);
    //how get the output? TinyTree xmlOut = builder.getTree();
    return null;
  }
*/  
  
  
  /**Transforms and writes the result in a SAXON Tree.
   * This is tested, but doesn't work in this form.
   * How to transform or convert to a jdom tree?
   * @return
   * @throws TransformerException
   */
/*
  org.jdom.Element transformToJdom(TransformerFactory tfactory) 
  throws TransformerException
  { //org.jdom.transform.JDOMResult xmlResult = new org.jdom.transform.JDOMResult();
    Configuration config = ((TransformerFactoryImpl)tfactory).getConfiguration();

    org.jdom.Document outJdom = new org.jdom.Document();
    net.sf.saxon.jdom.DocumentWrapper docWrapper 
    = new DocumentWrapper(outJdom, "xx", config);
    transformer.transform(xmlSource, (Result)docWrapper);
    if(!outJdom.hasRootElement())
    { throw new TransformerException("xslTransformationXml: no root element produced");
    }
    //if success than return the detached root element from conversion document.
    //The document is further unnecessary and will be deleted by the garbage collector.
    org.jdom.Element xmlOut = outJdom.getRootElement();
    xmlOut.detach();
    return xmlOut;
  }
*/  
  
  
/*  
  void output(Document document, File fOut) 
  throws IOException, XMLStreamException
  { XMLOutputFactory factory = new XMLOutputFactoryImpl();
    Writer stream = new FileWriter(fOut); 
    XMLStreamWriter writer = factory.createXMLStreamWriter(stream);
    writer.writeStartDocument();
    Node node = document.getFirstChild();
    writer.writeEmptyElement(node.getLocalName());
    writer.writeEndDocument();
    writer.close();
  }
  
  
  String outputSimple(Document document, File fOut)
  { String sError = null;
    try{ output(document, fOut); }
    catch(IOException exception)
    { sError = "IOException: " + exception.getMessage();
    }
    catch(XMLStreamException exception)
    { sError = "XMLStreamException: " + exception.getMessage();
    }
    return sError;
  }
  
  
*/
}
