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
 *
 ****************************************************************************/
package org.vishia.zbnf;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.text.ParseException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeMap;


import org.vishia.jbat.Zbnf2Text;
import org.vishia.mainCmd.MainCmd;
import org.vishia.mainCmd.MainCmd_ifc;
import org.vishia.mainCmd.Report;
import org.vishia.util.FileSystem;
import org.vishia.util.StringPart;
import org.vishia.util.StringPartFromFileLines;
import org.vishia.xmlSimple.SimpleXmlOutputter;
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

  /**Version, history and license.
   * <ul>
   * <li>2012-03-23 Hartmut new {@link #smain(String[], boolean)} as alternative to {@link #main(String[])}
   *   to call from other Java parts. The {@link #main(String[])} calls {@link java.lang.System#exit(int)}
   *   which terminates the JVM. Is it a good idea? Maybe main should only return, but how to deliver a errorlevel
   *   to a running shell script/batch without System.exit(errlevel). Maybe two methods are necessary.
   * <li>2013-03-20 Hartmut bugfix close() on written XML file. Without close the file was not able to access
   *   in a later step in the same Java program, whereby ending the Java process closes from operation system.
   * <li>2013-02-11 Hartmut chg: now does not use ZbnfXmlOutput but uses the intrinsic Xml tree returned with
   *   {@link ZbnfParser#getResultTree()}. 
   * <li>2012-11-01 Hartmut Some changes in structure of args, non-function-relevant.
   *   It is to provide common structures with {@link Zbnf2Text}.
   * <li>2006-05-00 Hartmut creation: 
   * </ul>
   * <br><br>
   * <b>Copyright/Copyleft</b>:
   * For this source the LGPL Lesser General Public License,
   * published by the Free Software Foundation is valid.
   * It means:
   * <ol>
   * <li> You can use this source without any restriction for any desired purpose.
   * <li> You can redistribute copies of this source to everybody.
   * <li> Every user of this source, also the user of redistribute copies
   *    with or without payment, must accept this license for further using.
   * <li> But the LPGL ist not appropriate for a whole software product,
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
  public static final int version = 20121101;

  
  public interface PrepareXmlNode
  {
    
    void prepareXmlNode(XmlNode xmlDst, String text) throws XmlException;
  }
  
  protected static class Args{
    
    /**Cmdline-argument, set on -i option. Inputfile to to something. :TODO: its a example.*/
    String sFileIn = null;
  
    /**Cmdline-argument, set on -y option. Inputfile to to something. :TODO: its a example.*/
    String sFileSyntax = null;
  
  
  
    /**Cmdline-argument, set on -o option. Outputfile to output something. :TODO: its a example.*/
    public String sFileOut = null;
    
    /**If true then executes parsing only if the output file does not exist or the input is newer. */
    public boolean checknew;
    
    /**Encoding given from cmdline argument -x, -y or -z
     */
    Charset encoding = Charset.defaultCharset();
    
    
    List<String> additionalSemantic;
  
    /** Type of the conversion, set in dependence of the -o or -x -option. */
    //private XslConvertMode mode = new XslConvertMode();
  }  
  
  /** Help reference to name the report output.*/
  protected final Report report;

  final Args arg;
  
  protected ZbnfParser parser = null;
  

  /*---------------------------------------------------------------------------------------------*/
  /** main started from java*/
  public static void main(String [] sArgs)
  { 
    smain(sArgs, true);
  }


  /**Invocation from another java program without exit the JVM
   * @param sArgs same like {@link #main(String[])}
   * @return "" or an error String
   */
  public static String smain(String[] sArgs){ return smain(sArgs, false); }

  
  private static String smain(String[] sArgs, boolean shouldExitVM){
    String sRet;
    Args args = new Args();
    CmdLine mainCmdLine = new CmdLine(args, sArgs); //the instance to parse arguments and others.
    try{
      mainCmdLine.addCmdLineProperties();
      boolean bOk = true;
      try{ mainCmdLine.parseArguments(); }
      catch(Exception exception)
      { mainCmdLine.report("Argument error:", exception);
        mainCmdLine.setExitErrorLevel(MainCmd_ifc.exitWithArgumentError);
        bOk = false;
      }
      Zbnf2Xml main = new Zbnf2Xml(args, mainCmdLine);     //the main instance
      if(bOk)
      { /** The execution class knows the SampleCmdLine Main class in form of the MainCmd super class
            to hold the contact to the command line execution.
        */
        try{ main.parseAndWriteXml(); }
        catch(Exception exception)
        { //catch the last level of error. No error is reported direct on command line!
          main.report.report("Uncatched Exception on main level:", exception);
          exception.printStackTrace(System.out);
          main.report.setExitErrorLevel(MainCmd_ifc.exitWithErrors);
        }
      }
      sRet = "";
    } catch(Exception exc){
      sRet = exc.getMessage();
    }
    if(shouldExitVM) { mainCmdLine.exit(); }
    return sRet;
  }



  /*---------------------------------------------------------------------------------------------*/
  /** Constructor of the main class.
      The command line arguments are parsed here. After them the execute class is created as composition of Zbnf2Xml.
  */
  public Zbnf2Xml(Args args, Report report)
  { this.arg = args;
    this.report = report;
  }


  public Zbnf2Xml(String input, String syntax, String output, Report report)
  {
    arg = new Args();
    
    this.arg.sFileIn = input;
    this.arg.sFileOut = output;
    this.arg.sFileSyntax = syntax;
    this.report = report;
  }

  /**The inner class CmdLine helps to evaluate the command line arguments
   * and show help messages on command line.
   */
  protected static class CmdLine extends MainCmd
  { 
  
    MainCmd.SetArgument setInput = new MainCmd.SetArgument(){ @Override public boolean setArgument(String val){ 
      argData.sFileIn = val; return true;
    }};

    
    MainCmd.SetArgument setSyntax = new MainCmd.SetArgument(){ @Override public boolean setArgument(String val){ 
      argData.sFileSyntax = val; return true;
    }};

    
    MainCmd.SetArgument setOutUtf8 = new MainCmd.SetArgument(){ @Override public boolean setArgument(String val){ 
      argData.sFileOut = val; argData.encoding = Charset.forName("UTF-8"); return true;
    }};

    
    MainCmd.SetArgument setOutAscii = new MainCmd.SetArgument(){ @Override public boolean setArgument(String val){ 
      argData.sFileOut = val; argData.encoding = Charset.forName("US-ASCII"); return true;
    }};

    
    MainCmd.SetArgument setOut = new MainCmd.SetArgument(){ @Override public boolean setArgument(String val){ 
      argData.sFileOut = val; return true;
    }};

    
    MainCmd.SetArgument setChecknew = new MainCmd.SetArgument(){ @Override public boolean setArgument(String val){ 
      argData.checknew = true; return true;
    }};

    
    
    MainCmd.SetArgument setOutEncoding = new MainCmd.SetArgument(){ @Override public boolean setArgument(String val){ 
      try{ argData.encoding = Charset.forName(val); return true;
      } catch(Exception exc){ return false; }
    }};

    
    MainCmd.SetArgument setXmlContent = new MainCmd.SetArgument(){ @Override public boolean setArgument(String val){ 
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
      if(argData.additionalSemantic == null)
      { argData.additionalSemantic = new LinkedList<String>();
      }
      argData.additionalSemantic.add(addSemantic);
      argData.additionalSemantic.add(addContent);
      return true;
    }};

    
    
    protected final MainCmd.Argument[] argList =
    { new MainCmd.Argument("-i", ":<INPUT>    inputfilepath, this file is parsing", setInput)
    , new MainCmd.Argument("-s", ":<SYNTAX>   syntax prescript in ZBNF format for parsing", setSyntax)
    , new MainCmd.Argument("-x", ":<OUTPUT>   output xml file written in UTF8-encoding", setOutUtf8)
    , new MainCmd.Argument("-y", ":<OUTPUT>   output xml file written in the standard encoding of system\n" 
                            + "               or the given -charset:encoding", setOut)
    , new MainCmd.Argument("-z", ":<OUTPUT>   output xml file written in US-ASCII-encoding", setOutAscii)
    , new MainCmd.Argument("-checknew", "     executes only if output not exists or input is newer", setChecknew)
    , new MainCmd.Argument("-charset", ":<CHARSET> use this encoding.", setOutEncoding)
    , new MainCmd.Argument("-a", ":<NAME>=<VALUE> set an additional xml information\n" 
                            + "   <NAME> of a additional XML infomation, typical @attribute" 
                            + "   <VALUE> its text, may be in \"\"", setXmlContent)
    };

    public final Args argData;
    
    /*---------------------------------------------------------------------------------------------*/
    /**Constructor of the cmdline handling class.
    The command line arguments are parsed here. After them the execute class is created as composition of SampleCmdLine.
*/
    protected CmdLine(Args argData, String[] sCmdlineArgs)
    { super(sCmdlineArgs);
      this.argData = argData;
    }
    

    void addCmdLineProperties(){
      super.addAboutInfo("Conversion text to XML via ZBNF");
      super.addAboutInfo("made by HSchorrig, 2006-03-20..2013-02-09");
      super.addHelpInfo("args: -i:<INPUT> -s:<SYNTAX> -[x|y|z]:<OUTPUT> [{-a:<NAME>=<VALUE>}]");  //[-w[+|-|0]]
      super.addArgument(argList);
      super.addStandardHelpInfo();
    }
    


    /** Invoked from parseArguments if no argument is given. In the default implementation a help info is written
     * and the application is terminated. The user should overwrite this method if the call without comand line arguments
     * is meaningfull.
     *
     */
    @Override
    protected void callWithoutArguments()
    { //overwrite with empty method - if the calling without arguments
      //having equal rights than the calling with arguments - no special action.
    }
  
  
  
  
    /*---------------------------------------------------------------------------------------------*/
    /**Checks the cmdline arguments relation together.
       If there is an inconsistents, a message should be written. It may be also a warning.
       @return true if successfull, false if failed.
    */
    @Override
    protected boolean checkArguments()
    { boolean bOk = true;
  
      if(argData.sFileIn == null)            { bOk = false; writeError("ERROR argument -iInputfile is obligat."); }
      else if(argData.sFileIn.length()==0)   { bOk = false; writeError("ERROR argument -iInputfile without content.");}
  
      if(argData.sFileSyntax == null)            { bOk = false; writeError("ERROR argument -sSyntaxfile is obligat."); }
      else if(argData.sFileSyntax.length()==0)   { bOk = false; writeError("ERROR argument -sSyntaxfile without content.");}
  
      if(argData.sFileOut == null)           { bOk = false; writeError("argument -y -x or -z: no outputfile is given");}
      else if(argData.sFileOut.length()==0)  { bOk = false; writeError("argument -y -x or -z without content"); }
      if(!bOk) setExitErrorLevel(exitWithArgumentError);
  
      return bOk;
  
   }
  }//class CmdLine


  
  /**Executes the parsing process and writes an XML file. 
   * @deprecated use {@link #parseAndWriteXml()}
   * @return true if successfull
   */
  @Deprecated
  public final boolean execute(){ return parseAndWriteXml(); }


  public boolean parseAndWriteXml()
  { boolean bOk = true;
    File fileOut = new File(arg.sFileOut);
    File fileIn = new File(arg.sFileIn);
    if(arg.checknew){
      if(fileOut.exists() && fileIn.exists() && fileOut.lastModified() > fileIn.lastModified()){
        report.writeInfo("Zbnf2Xml - is uptodate; " + fileOut.getAbsolutePath()); report.writeInfoln("");
        return true;
      }
    }
    { parser = new ZbnfParser(report);
      parser.setReportIdents(Report.error, Report.info, Report.debug, Report.fineDebug);
      try
      { parser.setSkippingComment("/*", "*/", true);
        parser.setSyntax(new File(arg.sFileSyntax));
      }
      catch (ParseException exception)
      { report.writeError("Parser Syntax reading error: " + exception.getMessage());
        //writeError("Stack:" + e.getStackTrace());
        exception.printStackTrace();
        bOk = false;
      } 
      catch (IllegalCharsetNameException e)
      {
        report.writeError("The " + arg.sFileSyntax + " contains an illegal charset-name");
        bOk = false;
      } 
      catch (UnsupportedCharsetException e)
      {
        report.writeError("The charset in " + arg.sFileSyntax + " is not supported");
        bOk = false;
      } 
      catch (FileNotFoundException e)
      {
        report.writeError("file not found:" + arg.sFileSyntax);
        bOk = false;
      } catch (IOException e)
      {
        report.writeError("file read error:" + arg.sFileSyntax);
        bOk = false;
      }
    }
    if(bOk)
    { parser.reportSyntax(report, Report.fineInfo);
    }
    StringPart spToParse = null;
    if(bOk)
    { Charset inputEncoding = parser.getInputEncoding();
      String sInputEncodingKeyword = parser.getInputEncodingKeyword();
      try
      { //spToParse = new StringPartFromFileLines(new File(sFileIn));
        spToParse = new StringPartFromFileLines(fileIn, -1, sInputEncodingKeyword, inputEncoding);
      }
      catch(FileNotFoundException exception)
      { report.writeError("file not found:" + fileIn.getAbsolutePath());
        bOk = false;
      }
      catch(IOException exception)
      { report.writeError("file read error:" + fileIn.getAbsolutePath());
        bOk = false;
      }
    }
    if(bOk)
    { report.writeInfoln("parsing " + arg.sFileIn);
      try{ bOk = parser.parse(spToParse, arg.additionalSemantic); }
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
      report.writeInfo(" XML: ");
      //XmlNodeSimple<ZbnfParseResultItem> xmlTop = parser.getResultTree();
      XmlNode xmlTop = parser.getResultTree();
      TreeMap<String, String> xmlnsList = parser.getXmlnsFromSyntaxPrescript();
      /**Adds the namespace declarations if exists: */
      { if(xmlnsList != null)
        { Iterator<String> iter = xmlnsList.keySet().iterator();
          while(iter.hasNext())
          { String nsKey = iter.next();
            String nsVal = xmlnsList.get(nsKey);
            //xmlTop.addNamespaceDeclaration(Namespace.getNamespace(nsKey, nsVal));
            xmlTop.addNamespaceDeclaration(nsKey, nsVal);
          }
        }      
      }
      if(arg.encoding == null) 
      { arg.encoding = Charset.forName("UTF-8");
      }
      try
      { 
        FileSystem.mkDirPath(fileOut);
        FileOutputStream streamOut = new FileOutputStream(fileOut);
        OutputStreamWriter out = new OutputStreamWriter(streamOut, arg.encoding);
        SimpleXmlOutputter xmlOutputter = new SimpleXmlOutputter();
        xmlOutputter.write(out, xmlTop);
        out.close();
        streamOut.close();
        report.writeInfo(" done "); report.writeInfoln("");
      }
      catch(IOException exception)
      { report.writeError("file not writeable:" + fileOut.getAbsolutePath());
        bOk = false;
      }
      
    }
    return bOk;
  }




}



                           