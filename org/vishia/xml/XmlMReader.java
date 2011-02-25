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
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;


import org.vishia.mainCmd.Report;
import org.vishia.xmlSimple.XmlException;

//import net.sf.saxon.Configuration;
//import net.sf.saxon.TransformerFactoryImpl;
//import net.sf.saxon.jdom.DocumentWrapper;


/**This class helps to read XML documents. 
 * The separability to other readers is, more as one XML input file may be read,
 * all input files are joined together in one Document, with an extra root element,
 * named 'root'. Therefore the letter 'M' (multiple) is included in the name of the class. 
 * <br>
 * Inside, partly JDOM is used {@linkplain http:\\www.jdom.org}, but most of the
 * inner working should be based on SAXON {@linkplain http:www.saxonica.com}.
 */
public abstract class XmlMReader
{

  public static final int mReplaceWhiteSpaceWith1Space = 0x0001;
  public static final int mExpandWikiFormat            = 0x0002;
  protected Report console;
  
  XmlMReader(Report console)
  { this.console = console;
  }
  
  
  public XmlMReader()
  { this.console = null;
  }
  
  public void setReport(Report console)
  { this.console = console;
  }
  
  
  protected static class FileTypeIn
  { protected final String sName;
    /** Mode of preprocessing inputfile, see m... */
    private final int mode; 

    private final File fileIn;;
    

    public FileTypeIn(String sFileIn, int mode)
    { sName = sFileIn;
      fileIn = new File(sName);
      this.mode = mode;
    }
    
    public File getFile(){ return fileIn; }  
    
    public int getMode(){ return mode; }  
  }

  /**Cmdline-argument, set on -i option. Inputfile to to something. :TODO: its a example.*/
  final List<FileTypeIn> listFileIn = new LinkedList<FileTypeIn>();

  void addInputFile(String sFileName)
  { addInputFile(sFileName, 0);
  }
  
  /**Adds the information about a input file. This methods adds a File instance to an internal list.
   * it doesn't read the content yet.
   * @param sFileName
   * @param mode Ones of mReplaceWhiteSpaceWith1Space or mExpandWikiFormat
   */
  public void addInputFile(String sFileName, int mode)
  { FileTypeIn fileIn = new FileTypeIn(sFileName, mode);
    listFileIn.add(fileIn);
  }
  


  
  public abstract Source readInputs(TransformerFactory tfactory);

  
  public Source readInputsHowDoesitWorks(TransformerFactory tfactory)
  {
  	String sFirstInputFile = listFileIn.get(0).getFile().getAbsolutePath();
  	Source xmlSource;
  	try{ 
    	xmlSource = new StreamSource(new FileReader(sFirstInputFile));
  	} catch(Exception exc){
      xmlSource = null;  		
  	}
  	return xmlSource;
  	
  	/*
  	XMLInputFactory inputFactory = XMLInputFactory.newFactory();
    XMLStreamReader xmlStreamReader = null;
    try{ 
    	xmlStreamReader = inputFactory.createXMLStreamReader(new FileReader(sFirstInputFile));
  	
  	} catch(Exception exc){
  		
  	}
  	xmlStreamReader.
  	*/
  }
  
  /*  
  private void readInput() throws TransformerConfigurationException, MalformedURLException, IOException, SAXException
  {
    TransformerFactory tfactory = TransformerFactory.newInstance();

    if (tfactory.getFeature(SAXSource.FEATURE)) {
        XMLReader reader =
            ((SAXTransformerFactory) tfactory)
                .newXMLFilter(new StreamSource(new File("xx")));

        reader.setContentHandler(new ExampleContentHandler());
        reader.parse(new InputSource(new File("xx").toURL().toString()));
    } else {
        System.out.println("tfactory does not support SAX features!");
    }
    
  }
  */
  
}
