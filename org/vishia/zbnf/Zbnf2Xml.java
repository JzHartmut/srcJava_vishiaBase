/****************************************************************************/
/* Copyright/Copyleft: 
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
 * 2006-05-00: www.vishia.de creation
 *
 ****************************************************************************/
package org.vishia.zbnf;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;

import org.vishia.mainCmd.MainCmd;
import org.vishia.mainCmd.MainCmd_ifc;
import org.vishia.mainCmd.Report;
import org.vishia.util.StringPart;
import org.vishia.util.StringPartFromFileLines;
import org.vishia.xmlSimple.XmlException;
import org.vishia.xmlSimple.XmlNode;
import org.vishia.zbnf.ZbnfParser;



/** This class contains a public static main routine callable from command line
 * and a execute-routine callable form eclipse-ANT (TODO)
 * to convert Plain Syntactical Textfiles via ZBNF to xml-output, using the vishia-ZBNF-parser.
 * <br/>
 * To invoke the conversion use the commandline version with parameters see help output
 * getting by invoking without parameters.
 * <br/>
 * short description: invoke:<br>
 * <pre> >java cp $CLASSPATH vishia.strincScan.SBNF2Xml -iINPUT -sSBNF -yXMLOUTPUT</pre>
*/

public class Zbnf2Xml
{

  
  public interface PrepareXmlNode
  {
    
    void prepareXmlNode(XmlNode xmlDst, String text) throws XmlException;
  }
  
  
  
  /**Cmdline-argument, set on -i option. Inputfile to to something. :TODO: its a example.*/
  String sFileIn = null;

  /**Cmdline-argument, set on -y option. Inputfile to to something. :TODO: its a example.*/
  String sFileSyntax = null;



  /**Cmdline-argument, set on -o option. Outputfile to output something. :TODO: its a example.*/
  String sFileOut = null;
  
  /**Encoding given from cmdline argument -x, -y or -z
   */
  Charset encoding = Charset.defaultCharset();
  
  
  List<String> additionalSemantic;

  /** Type of the conversion, set in dependence of the -o or -x -option. */
  //private XslConvertMode mode = new XslConvertMode();
  
  
  /** Help reference to name the report output.*/
  Report report;

  

  /*---------------------------------------------------------------------------------------------*/
  /** main started from java*/
  public static void main(String [] args)
  { Zbnf2Xml main = new Zbnf2Xml();     //the main instance
    CmdLine mainCmdLine = main.new CmdLine(args); //the instance to parse arguments and others.
    main.report = mainCmdLine;  //it may be also another instance based on MainCmd_ifc
    boolean bOk = true;
    try{ mainCmdLine.parseArguments(); }
    catch(Exception exception)
    { main.report.report("Argument error:", exception);
      main.report.setExitErrorLevel(MainCmd_ifc.exitWithArgumentError);
      bOk = false;
    }
    if(bOk)
    { /** The execution class knows the SampleCmdLine Main class in form of the MainCmd super class
          to hold the contact to the command line execution.
      */
      try{ main.execute(); }
      catch(Exception exception)
      { //catch the last level of error. No error is reported direct on command line!
        main.report.report("Uncatched Exception on main level:", exception);
        exception.printStackTrace(System.out);
        main.report.setExitErrorLevel(MainCmd_ifc.exitWithErrors);
      }
    }
    mainCmdLine.exit();
  }






  /*---------------------------------------------------------------------------------------------*/
  /** Constructor of the main class.
      The command line arguments are parsed here. After them the execute class is created as composition of Zbnf2Xml.
  */
  public Zbnf2Xml()
  { 
  }


  public Zbnf2Xml(String input, String syntax, String output, Report report)
  {
    this.sFileIn = input;
    this.sFileOut = output;
    this.sFileSyntax = syntax;
    this.report = report;
  }

  /**The inner class CmdLine helps to evaluate the command line arguments
   * and show help messages on command line.
   */
  class CmdLine extends MainCmd
  { 
  
    /*---------------------------------------------------------------------------------------------*/
    /** Constructor of the main class.
        The command line arguments are parsed here. After them the execute class is created as composition of SampleCmdLine.
    */
    private CmdLine(String[] args)
    { super(args);
      report = this;
      super.addAboutInfo("Conversion text to XML via ZBNF");
      super.addAboutInfo("made by HSchorrig, 2006-03-20..2008-02-29");
      super.addHelpInfo("args: -i:<INPUT> -s:<SYNTAX> -y:<OUTPUT> [{-a:<NAME>=<VALUE>}]");  //[-w[+|-|0]]
      super.addStandardHelpInfo();
      super.addHelpInfo("-i:<INPUT>    inputfilepath, this file is parsing.");
      super.addHelpInfo("-s:<SYNTAX>   syntax prescript in SBNF format for parsing");
      super.addHelpInfo("-x:<OUTPUT>   output xml file written in UTF8-encoding");
      super.addHelpInfo("-y:<OUTPUT>   output xml file written in the standard encoding of VM");
      super.addHelpInfo("                or the given -charset:encoding");
      super.addHelpInfo("-z:<OUTPUT>   output xml file written in US-ASCII-encoding");
      super.addHelpInfo("-charset:<CHARSET> use this encoding.");
      super.addHelpInfo("-a:<NAME>     name of a additional XML infomation, typical @attribute");
      super.addHelpInfo("  =<VALUE>    value of the additional XML infomation, may be in \"\"");
      
      //super.addHelpInfo("-w+          Write with indentification and beatification");
      //super.addHelpInfo("-w-          Write without indentification, long lines");
      //super.addHelpInfo("-w0          Write without indentification, long lines");
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
    public boolean testArgument(String argc, int nArg)
    { boolean bOk = true;  //set to false if the argc is not passed
  
      if(argc.startsWith("--_"))      { /*ignore it. */ }
      else if(argc.startsWith("-i:")){ sFileIn   = getArgument(3); }
      else if(argc.startsWith("-i")) { sFileIn   = getArgument(2); }
      else if(argc.startsWith("-s:")){ sFileSyntax  = getArgument(3); }
      else if(argc.startsWith("-s")) { sFileSyntax  = getArgument(2); }
      else if(argc.startsWith("-x:")){ sFileOut = getArgument(3); encoding = Charset.forName("UTF-8"); }
      else if(argc.startsWith("-x")) { sFileOut = getArgument(2); encoding = Charset.forName("UTF-8"); }
      else if(argc.startsWith("-y:")){ sFileOut = getArgument(3); }
      else if(argc.startsWith("-y")) { sFileOut = getArgument(2); }
      else if(argc.startsWith("-z:")){ sFileOut = getArgument(3); encoding = Charset.forName("US-ASCII"); }
      else if(argc.startsWith("-z")) { sFileOut = getArgument(2); encoding = Charset.forName("US-ASCII");  }
      else if(argc.startsWith("-charset:")){ encoding = Charset.forName(getArgument(9));  }
      else if(argc.startsWith("-a:"))
      { //argument
        String sArg = getArgument(3);
        String addSemantic, addContent;
        int posAssign = sArg.indexOf('=');
        if(posAssign >=0)
        { addSemantic = sArg.substring(0, posAssign);
          if(sArg.length() > (posAssign +2) && sArg.charAt(posAssign+1)=='\"')
          { addContent = sArg.substring(posAssign +2, sArg.length()-1);  //without "", assumed the arg ends with "
          }
          else 
          { addContent = sArg.substring(posAssign +1);
          }
        }
        else
        { addSemantic = sArg;
          addContent = "";
        }
        if(additionalSemantic == null)
        { additionalSemantic = new LinkedList<String>();
        }
        additionalSemantic.add(addSemantic);
        additionalSemantic.add(addContent);
      }
  /*
      else if(argc.equals("-w+"))   { mode.setIndent("  "); }
      else if(argc.equals("-w-"))   { mode.setIndent(null); }
      else if(argc.equals("-w0"))   { mode.setIndent(""); }
  */
      else bOk=false;
  
      return bOk;
    }
  
    /** Invoked from parseArguments if no argument is given. In the default implementation a help info is written
     * and the application is terminated. The user should overwrite this method if the call without comand line arguments
     * is meaningfull.
     *
     */
    protected void callWithoutArguments()
    { //overwrite with empty method - if the calling without arguments
      //having equal rights than the calling with arguments - no special action.
    }
  
  
  
  
    /*---------------------------------------------------------------------------------------------*/
    /**Checks the cmdline arguments relation together.
       If there is an inconsistents, a message should be written. It may be also a warning.
       @return true if successfull, false if failed.
    */
    protected boolean checkArguments()
    { boolean bOk = true;
  
      if(sFileIn == null)            { bOk = false; writeError("ERROR argument -iInputfile is obligat."); }
      else if(sFileIn.length()==0)   { bOk = false; writeError("ERROR argument -iInputfile without content.");}
  
      if(sFileSyntax == null)            { bOk = false; writeError("ERROR argument -sSyntaxfile is obligat."); }
      else if(sFileSyntax.length()==0)   { bOk = false; writeError("ERROR argument -sSyntaxfile without content.");}
  
      if(sFileOut == null)           { bOk = false; writeError("argument -y -x or -z: no outputfile is given");}
      else if(sFileOut.length()==0)  { bOk = false; writeError("argument -y -x or -z without content"); }
      if(!bOk) setExitErrorLevel(exitWithArgumentError);
  
      return bOk;
  
   }
  }//class CmdLine




  public boolean execute()
  { boolean bOk = true;
    ZbnfParser parser = null;
    { parser = new ZbnfParser(report);
      parser.setReportIdents(Report.error, Report.info, Report.debug, Report.fineDebug);
      try
      { parser.setSkippingComment("/*", "*/", true);
        parser.setSyntax(new File(sFileSyntax));
      }
      catch (ParseException exception)
      { report.writeError("Parser Syntax reading error: " + exception.getMessage());
        //writeError("Stack:" + e.getStackTrace());
        exception.printStackTrace();
        bOk = false;
      } 
      catch (IllegalCharsetNameException e)
      {
        report.writeError("The " + sFileSyntax + " contains an illegal charset-name");
        bOk = false;
      } 
      catch (UnsupportedCharsetException e)
      {
        report.writeError("The charset in " + sFileSyntax + " is not supported");
        bOk = false;
      } 
      catch (FileNotFoundException e)
      {
        report.writeError("file not found:" + sFileSyntax);
        bOk = false;
      } catch (IOException e)
      {
        report.writeError("file read error:" + sFileSyntax);
        bOk = false;
      }
    }
    if(bOk)
    { parser.reportSyntax(report);
    }
    StringPart spToParse = null;
    if(bOk)
    { Charset inputEncoding = parser.getInputEncoding();
      String sInputEncodingKeyword = parser.getInputEncodingKeyword();
      try
      { //spToParse = new StringPartFromFileLines(new File(sFileIn));
        spToParse = new StringPartFromFileLines(new File(sFileIn), -1, sInputEncodingKeyword, inputEncoding);
      }
      catch(FileNotFoundException exception)
      { report.writeError("file not found:" + sFileIn);
        bOk = false;
      }
      catch(IOException exception)
      { report.writeError("file read error:" + sFileIn);
        bOk = false;
      }
    }
    if(bOk)
    { report.writeInfoln("parsing " + sFileIn);
      try{ bOk = parser.parse(spToParse, additionalSemantic); }
      catch(Exception exception)
      { report.writeError("any exception while parsing:" + exception.getMessage());
        
        report.report("any exception while parsing", exception);
        parser.reportStore(report);
        //evaluateStore(parser.getFirstParseResult());
        bOk = false;
      }
      if(!bOk)
      { report.writeError(parser.getSyntaxErrorReport());
        parser.reportStore(report);
        //evaluateStore(parser.getFirstParseResult());
      }
    }
    if(bOk)
    { parser.reportStore(report);
      //evaluateStore(parser.getFirstParseResult());
      ZbnfXmlOutput outputXml = new ZbnfXmlOutput();
      //outputXml.write(parser.getFirstParseResult(), sFileOut);
      report.writeInfo(" XML: ");
      TreeMap<String, String> xmlnsList = parser.getXmlnsFromSyntaxPrescript();
      if(encoding == null) 
      { encoding = Charset.forName("UTF-8");
      }
      String sEncoding = encoding.displayName();
      ZbnfParseResultItem zbnfTop = parser.getFirstParseResult();
      try
      { 
        FileOutputStream fileOut = new FileOutputStream(sFileOut);
        OutputStreamWriter out = new OutputStreamWriter(fileOut, encoding);
        outputXml.write(zbnfTop, xmlnsList, out);
        report.writeInfo(" done "); report.writeInfoln("");
      }
      catch(FileNotFoundException exception)
      { report.writeError("file not writeable:" + sFileOut);
        bOk = false;
      } catch (XmlException e)
      {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      
    }
    return bOk;
  }




}



                           