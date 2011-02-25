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
 * @author Hartmut Schorrig, Germany, Pinzberg
 * @version 2009-03-31  (year-month-day)
 * list of changes: 
 * 2009-12-29 Hartmut new: The transformer class is now able to define via param -xslt:, so the SAXON isn't compiled fix.
 * 2009-12-29 Hartmut new: A Xslp-file is translated to the xsl-format, param -p:, the xsl is stored if param -t: is given.^
 * 2009-12-29 Hartmut new: params for translation may be defined, using name=value as param of invocation.
 * 2009-12-29 Hartmut new: The output format isn't fixed to XML, at may be text too. Depending on entry in XSL-File.
 * 2009-31-03 Hartmut: Cmd line argument separator : possible, outputs for help improved.
 * 2006-05-00 Hartmut: creation
 *
 ****************************************************************************/
package org.vishia.xml;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;


//import net.sf.saxon.om.NodeInfo;

//import org.xml.sax.SAXException;

//import com.sun.xml.internal.stream.XMLOutputFactoryImpl;

import org.vishia.mainCmd.MainCmd;
import org.vishia.mainCmd.Report;
import org.vishia.mainCmd.MainCmd_ifc;
import org.vishia.xmlSimple.XmlException;
import org.vishia.xmlSimple.Xsltpre;


/**vishia XSLT Translator. 
 * <br><br>
 * This translator calls the java-standard XSLT-engine defined with 
 * javax.xml.transform.TransformerFactory. The cmdLine-option <code>-xslt:TRANSFORMER</code>
 * determines, which Transformer is used. The default-value is <code>net.sf.saxon.TransformerFactoryImpl</code>,
 * therefore this Transformer is recommended to use. It should be provided by the adequate jar-File
 * specified in the java's-CLASSPATH.
 * <br><br>
 * The vishia-XSLT prepares some inputs before calling the outside given Transformer:
 * <br><br>
 * <b>Multiple inputs: </b>
 * <br>
 * More as one XML-Inputfile is able to use as input. All input file-contents are disposed after a 
 *   internal created <code>< root></code> root-Element. It means, the translation file have to be regard
 *   this additional <code>< root></code> root-Element. Write:
 *   <pre>
 *     < xsl:apply-templates match="/root" >
 *        ....
 *     </xsl:apply-templates >
 *   </pre>
 *   instead the often used
 *   <pre>
 *     < xsl:apply-templates match="/" >
 *        ....
 *     </xsl:apply-templates >
 *   </pre>
 *           
 * <br><br>
 * <b>Replacing of spaces of input:</b>
 * <br>
 * The XML file representation may contain any white spaces (more as one space, line feed, indentation)
 * between the XML-elements. It is a method of beautification of the appearance of the file content.
 * The white spaces may be included by any XML-writer independent of the meaning of the content.
 * But if the input should present a text with formatted inner elements (maybe XHTML), the white spaces
 * may be interpreted as really text characters. The class {@link org.vishia.xml.XmlExtensions} contains
 * a static method {@link org.vishia.xml.XmlExtensions.readXmlFileTrimWhiteSpace(File)} which is used here
 * if an input is designated with the cmdLine option <code>-j:FILE</code>.   
 * <br><br>
 * <b>Expansion of wikistyle: </b>
 * <br>
 * This feature is not implemented yet. The conversion of {@link org.vishia.xmlSimple.WikistyleTextToSimpleXml}
 * is implemented instead in the ZBNF2Xml-conversion (class {@link org.vishia.zbnf.ZbnfXmlOutput}
 * and in the class {@link org.vishia.xml.docuGen.CorrectHref} for documentation generation.
 * It was contained here in the past. It isn't supported yet, because the conversion routine 
 * is changed using the {@link org.vishia.xmlSimple.XmlNode}, which is not directly compatible with JDOM.
 * An adaption is existing in the {@link org.vishia.xml.XmlNodeJdom}, but it isn't used here yet.
 * <br><br>
 * <b>Beautification of output: </b>
 * <br>
 * This feature was implemented here in the past before using the SAXON-XSLT. But the SAXON writes the file
 * in an adequate style itself. 
 * <br><br>
 * A beautification should generate indentation automatically. But it should regard textual contents: 
 * If any element contains a textual content, its whole content should be written in one line 
 * to prevent additional white spaces which may be confused the textual content. 
 * Such an XML-writer is implemented in {@link {@link org.vishia.xml.XmlExtensions.writeXmlBeautificatedTextFile(Element, File, Charset)}}.
 * This writer uses JDOM as input. This is the older implementation.
 * A newer implementation of this feature is present in {@link org.vishia.xmlSimple.SimpleXmlOutputter}.
 */

public class Xslt
{

  /**Cmdline-argument, set on -y option. Outputfile to output something.*/
  protected String sFileOut = null;
  
  
  /**CmdLine-argument set on -t option: XSLT-File for Transformer. Either it is the primary file,
   * or, if {@link sFileXslp} is given, it is the name of the output file. 
   */
  protected String sFileXslt = null;

  /**CmdLine-argument set on -p option: XSLT-File pre-converted with {@link org.vishia.xmlSimple.Xsltpre}.
   * If it is null, the {@link sFileXslt is used as input. }. 
   * 
   * NOTE: If the XSLT-File given in {@link sFileXslt}-
   * element is newer as this given file, the first one will not overwritten by this given file. This feature
   * may be able to use for fine adjustments of content while debugging a script. 
   */
  protected String sFileXslp = null;
  
  
  protected class Parameter
  { String name; String value;
  
    public Parameter(String name, String value)
    { this.name = name; this.value = value;
    } 
  }
  
  protected List<Parameter> params = new LinkedList<Parameter>();
  
  
  /**Class which is used by the javax.xml.transform.TransformerFactory
   * It is set with System.setProperty("javax.xml.transform.TransformerFactory", ...);
   */
  protected String sTransformer = "net.sf.saxon.TransformerFactoryImpl";

  /**Set on command line option. If true, after xslt translation
   * the output tree is evaluate for elements with attribute expandWikistyle or WikiFormat.
   * This elements are expanded.
   */ 
  protected boolean bWikiFormat = false;
  
  /**Instance to process input files. This instance holds informations about input files with several reading conditions 
   * and contains a xml parser call.
   */ 
  protected XmlMReaderJdomSaxon xmlMReader;

  /*---------------------------------------------------------------------------------------------*/
  /** main started from java.
   * <br><br>
   * <b>Command line options:</b>
   * <br><br>
   * <pre>
      "invoke { -[i|j|k]:INPUT } [-t:XSLT] [-p:XSLP] -y:OUTPUT [-xslt:TRANSFORMER]");
      "-i:INPUT-XML-file");
      "-j:INPUT-XML-file, Whitespaces will replaced with 1 space");
      "-t:XSLT: xsl-script XML2-compatible, it is output if -p:XSLP is given");
      "-p:XSLP: Script pretranslated with Xsltpre, than -t:XSLT will be created if older.");
      "         If no option -t:XSLT is given, XSLT will be created parallel with .xsl as extension.");
      "-y:OUTPUT-file");
      "-xslt:TRANSFORMER Set the class for Transformer-Implementation. Default is " + sTransformer);
      </pre>
   * 
   * */
  public static void main(String [] args)
  { Xslt main = new Xslt();     //the main instance
    CmdLine mainCmdLine = main.new CmdLine(args); //the instance to parse arguments and others.
    main.console = mainCmdLine;  //it may be also another instance based on MainCmd_ifc
    boolean bOk = true;
    try{ 
    	@SuppressWarnings("unchecked")
    	Class<XmlMReaderJdomSaxon> classXmlReader = (Class<XmlMReaderJdomSaxon>)Class.forName("org.vishia.xml.XmlMReaderJdomSaxon");
      main.xmlMReader = classXmlReader.newInstance();
    } catch(Exception exc){
    	main.console.setExitErrorLevel(MainCmd_ifc.exitWithArgumentError);
      bOk = false;
    }
    if(bOk){
	    main.xmlMReader.setReport(main.console);
	    try{ mainCmdLine.parseArguments(); }
	    catch(Exception exception)
	    { main.console.setExitErrorLevel(MainCmd_ifc.exitWithArgumentError);
	      bOk = false;
	    }
    }
    if(bOk)
    { /** The execution class knows the Xslt Main class in form of the MainCmd super class
          to hold the contact to the command line execution.
      */
      try{ main.execute(); }
      catch(Exception exception)
      { //catch the last level of error. No error is reported direct on command line!
        main.console.report("Uncatched Exception on main level:", exception);
        main.console.setExitErrorLevel(MainCmd_ifc.exitWithErrors);
      }
    }
    mainCmdLine.exit();
  }

  
  
  /**The inner class CmdLine helps to evaluate the command line arguments
   * and show help messages on command line.
   */
  class CmdLine extends MainCmd
  {
  
  
    /*---------------------------------------------------------------------------------------------*/
    /** Constructor of the main class.
        The command line arguments are parsed here. After them the execute class is created as composition of Xslt.
    */
    private CmdLine(String[] args)
    { super(args);
      //:TODO: user, add your help info!
      //super.addHelpInfo(getAboutInfo());
      super.addAboutInfo("XSLT-Translator");
      super.addAboutInfo("made by Hartmut Schorrig, 2005..2009-03-31");
      super.addHelpInfo("* saxon9.jar, saxon9-jdom.jar, jdom.jar required internally.");
      super.addHelpInfo("* Multiple input files are able too, all XML-inputs are disposed as child of a <root>");
      super.addHelpInfo("* Enhancments of wiki-format texts to XHTML implicitly");
      super.addHelpInfo("* Prepared XSL-Script possible, see org.vishia.xmlSimple.Xsltpre");
      super.addHelpInfo("invoke { -[i|j|k]:INPUT } [-t:XSLT] [-p:XSLP] -y:OUTPUT [-xslt:TRANSFORMER] {PARAM}");
      super.addHelpInfo("-i:INPUT-XML-file");
      super.addHelpInfo("-j:INPUT-XML-file, Whitespaces will replaced with 1 space");
      //super.addHelpInfo("-k:INPUT-XML-file, before processing expand wiki-Format");
      super.addHelpInfo("-t:XSLT: xsl-script XML2-compatible, it is output if -p:XSLP is given");
      super.addHelpInfo("-p:XSLP: Script pretranslated with Xsltpre, than -t:XSLT will be created if older.");
      super.addHelpInfo("         If no option -t:XSLT is given, XSLT will be created parallel with .xsl as extension.");
      super.addHelpInfo("-y:OUTPUT-file");
      super.addHelpInfo("-xslt:TRANSFORMER Set the class for Transformer-Implementation. Default is " + sTransformer);
      super.addHelpInfo("PARAM: written in form NAME=VALE or {URI}NAME=VALUE. They are available in the XSLT-script as $NAME.");
      super.addStandardHelpInfo();
      
    }
  
  
  
  
  
  
    /*---------------------------------------------------------------------------------------------*/
    /** Tests one argument. This method is invoked from parseArgument. It is abstract in the superclass MainCmd
        and must be overwritten from the user.
        @param argc String of the actual parsed argument from cmd line
        @param nArg number of the argument in order of the command line, the first argument is number 1.
        @return true is okay,
                false if the argument doesn't match. The parseArgument method in MainCmd throws an exception,
                the application should be aborted.
    */
    protected boolean testArgument(String arg, int nArg)
    { boolean bOk = true;  //set to false if the argc is not passed
      int posArg = (arg.length()>=2 && arg.charAt(2)==':') ? 3 : 2; //with or without :
    
      if(     arg.startsWith("-i")) { xmlMReader.addInputFile(getArgument(posArg)); }
      else if(arg.startsWith("-j")) { xmlMReader.addInputFile(getArgument(posArg),XmlMReaderJdomSaxon.mReplaceWhiteSpaceWith1Space); }
      else if(arg.startsWith("-k")) { xmlMReader.addInputFile(getArgument(posArg),XmlMReaderJdomSaxon.mExpandWikiFormat); }
      else if(arg.startsWith("-t")) { sFileXslt = getArgument(posArg); }
      else if(arg.startsWith("-p")) { sFileXslp = getArgument(posArg); }
      else if(arg.startsWith("-y")) { sFileOut =  getArgument(posArg); }
      else if(arg.startsWith("-xslt:")) { sTransformer =  getArgument(6); }
      //else if(arg.startsWith("-wikiformat")){ bWikiFormat = true; }
      else if(arg.startsWith("-w+")) {} //TODO output beautification or not. It depends on the transformer.
      else if(arg.startsWith("-w-")) {} //TODO output beautification or not.
      else 
      { int posSep = arg.indexOf('=');
        if(posSep >0){
          String sName = arg.substring(0, posSep);
          String sValue = arg.substring(posSep+1);
          params.add(new Parameter(sName, sValue));
        } else {
          bOk=false;
        }
      }
  
      return bOk;
    }
  
    /** Invoked from parseArguments if no argument is given. In the default implementation a help info is written
     * and the application is terminated. The user should overwrite this method if the call without comand line arguments
     * is meaningfull.
     * @throws ParseException 
     *
     */
    protected void callWithoutArguments() throws ParseException
    { //:TODO: overwrite with empty method - if the calling without arguments
      //having equal rights than the calling with arguments - no special action.
      super.callWithoutArguments();  //it needn't be overwritten if it is unnecessary
    }
  
  
  
  
    /*---------------------------------------------------------------------------------------------*/
    /**Checks the cmdline arguments relation together.
       If there is an inconsistents, a message should be written. It may be also a warning.
       @return true if successfull, false if failed.
    */
    protected boolean checkArguments()
    { boolean bOk = true;
  
      //if(sFileIn.size()==0)   { bOk = false; writeError("ERROR argument -i without content.");}
  
      if(sFileOut == null)           { writeWarning("argument -y: no outputfile is given");}
      else if(sFileOut.length()==0)  { bOk = false; writeError("argument -y: without content"); }
  
      if(sFileXslt == null)           { writeWarning("argument -t: no XSLT-file is given"); }
      else if(sFileXslt.length()==0)  { bOk = false; writeError("argument -t: without content"); }
  
      if(!bOk) setExitErrorLevel(exitWithArgumentError);
    
      return bOk;
    
    }
  }//class CmdLine
  
  /** Aggregation to the Console implementation class.*/
  MainCmd_ifc console;


  /** Executes the task of this class.
   * @throws ParserConfigurationException 
   * @throws IOException 
   * @throws SAXException 
   * @throws FileNotFoundException 
   * @throws TransformerException 
   * @throws XmlException 
  */
  private void execute() 
  throws ParserConfigurationException, FileNotFoundException, IOException, TransformerException, XmlException
  { console.reportln(Report.fineInfo, "vishia-XSLT with " + sTransformer);
    System.setProperty("javax.xml.transform.TransformerFactory", sTransformer);
    //The SAXON TransformerFactory is instanciated because above System.setProperty()
    TransformerFactory     tfactory   = TransformerFactory.newInstance();

    //net.sf.saxon.om.DocumentInfo doc;
    final Source doc;

    File fileXsl;
    if(sFileXslp != null){
      /**Generate a adequate xsl-file: */
      fileXsl = genXslFromXslp();
    } else {
      fileXsl = new File(sFileXslt);
    }
      
    doc = xmlMReader.readInputs(tfactory);
    
    //outputSimple(docRoot, new File("test1.out"));
    
    Source xslt = new StreamSource(fileXsl);
    Transformer transformer = tfactory.newTransformer(xslt);
    Properties  oprops     = new Properties();

    for(Parameter param: params){
      transformer.setParameter(param.name, param.value);
    }
    
    
    //oprops.put("method", "xml");
    //transformer.setOutputProperties(oprops);

    XmlMTransformer transformerExec = new XmlMTransformer(doc, transformer); 

    if(true)
    { File fileOut = new File(sFileOut);
      { //this executes the saxon transformation.
        transformerExec.transformToFile(fileOut);
        console.writeInfoln("output written to:" + fileOut.getAbsolutePath());
      }
    }
    /*
    else if(false)
    {
      NodeInfo transformedTree = transformerExec.transformToTinyTree();
      WikiFormat wikiFormat = new WikiFormat();
      wikiFormat.process(transformedTree);
    }
    else if(false)
    { org.jdom.Element xmlAfterTransform = transformerExec.transformToJdom(tfactory);
      ConverterWikistyleTextToXml wikiFormat = new ConverterWikistyleTextToXml();
      wikiFormat.testXmlTreeAndConvert(xmlAfterTransform);
    }
    */
    //transformer.transform(doc, new StreamResult(new FileOutputStream(sFileOut)));
    //transformer.transform(new DOMSource(doc),
    //                     new StreamResult(System.out));

    console.writeInfoln("");  //new line after all.
  }

  
  
  
  
  /**Converts the Xslt-file using {@link org.vishia.xmlSimple.Xsltpre}
   * @return
   */
  private File genXslFromXslp()
  {
    if(sFileXslt == null){
      int posLastDot = sFileXslp.lastIndexOf('.');
      if(posLastDot < 0 || sFileXslp.endsWith(".xsl")){
        sFileXslt = sFileXslp + ".xsl";  //append the extension, 
      } else {
        sFileXslt = sFileXslp.substring(0, posLastDot) + ".xsl";
      }
    }
    File fileXsl = new File(sFileXslt); //without 'p'
    File fileXslp = new File(sFileXslp);
    Xsltpre xsltpre = new Xsltpre(fileXslp, fileXsl);
    xsltpre.execute();
    
    return fileXsl;
  }
  
  
  
  
  
}



                           