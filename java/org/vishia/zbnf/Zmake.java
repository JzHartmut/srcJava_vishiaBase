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
 * @author Hartmut Schorrig = hartmut.schorrig@vishia.de
 * @version 2009-06-15  (year-month-day)
 * list of changes:
 * 2010-01-05: Hartmut corr: now use 'ZBNFJAX_HOME' instead 'XML_TOOLBASE', it is more significating. 
 * 2007..2009: Hartmut: some changes
 * 2007 Hartmut created
 */
package org.vishia.zbnf;

import java.text.ParseException;
import java.util.Properties;
import java.io.File;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.vishia.xmlSimple.Xsltpre;
import org.vishia.zbnf.Zbnf2Xml;
import org.vishia.mainCmd.MainCmd;
import org.vishia.mainCmd.MainCmd_ifc;


/**This class supports a making using eclipse.ANT. But the user's makefile is considerably incomplex as a ant.xml file.
 * Using Zbnf and Xslt the user's make file is translated to the ant.xml-format. The original input can use an arbitrary
 * syntax. The syntax is determined by the Zbnf file given as argument. The called routines of the make process are determined
 * from the Xslt-file, which is used to generate the ant.xml-file. The ant.xml-file is temprorary, but it can be checked and saved.
 * <br>
 * This class is callable from ant itself (under construction, not ready yet), from another java-routine or from command line.
 * <ul><li>For the commandline invocation see {@link main(String[]}.</li>
 *     <li>For the calling from java use the public constuctor Zmake(...) (TODO) and the methode {@link execute()}. </li>
 *     <li>For ant the class extends org.apache.tools.ant.Task (TODO)</li>
 * </ul>
 * The execute routine calls first the zbnf-parser, than call SAXON-xslt with the given scripts.
 * At least ant is called automatically in the same process (TODO).         
 * 
 * @author JcHartmut
 *
 */
public class Zmake
{
  private String input = null;
  
  /** Aggregation to the Console implementation class.*/
  private MainCmd_ifc console;

  /**String path to the XML_TOOLBASE Directory. */
  private String zbnfjax_PATH;
  
  /**String path to the current dir from calling. */
  private String curDir = null;  //default: actual dir
  
  /**String path fromcurDir to a tmp dir. */
  private String tmp = "../tmp";  
  
  /**String path for the absolute tmp dir. */
  private String tmpAbs;  
  
  
  /**Path of ZBNF script to generate the ant.xml*/
  private String sZbnf4ant = "xsl/ZmakeStd.zbnf";
  
  /**Path of XSL script to generate the ant.xml*/
  private String sXslt4ant = "xsl/ZmakeStd.xslp";
  
  /**Path of the input.xml*/
  private String sInputXml = null;
  
  /**Path of the ant.xml*/
  private String sAntXml = null;
  
  
  /*---------------------------------------------------------------------------------------------*/
  /**Invocation from command line. Up to now the ant file will produced, but ant will not started here. 
   * <br>code in constructor to generate help info:<pre>
     Zmake organizer                                               
     made by JcHartmut, 2007-07-06 - 2007-10-18                    
     invoke>%JAX_EXE% org.vishia.zbnfXml.Zmake [INPUT] [{OPTIONS}]
     * pathes to files or dirs are absolute or relativ from cmd line invocation.               
     * TPATH means a path started from given -ZBNFJAX_HOME:PATH or ZBNFJAX_HOME in environment.
     * WPATH means a path started from given -tmp directory (WorkPATH).
     * INPUTFILE is the only filename without path and without extension dissolved from INPUT                        
     INPUT              The first argument without - is the input file with path and extension.
     -i:INPUT           path to the input file alternatively to INPUT.                         
     -curdir:PATH       sets the current dir alternatively to command line invocation path.     
     -ZBNFJAX_HOME:PATH path to the ZBNFJAX_HOME, default it is getted from environment.       
     -tmp:PATH          path of tmp dir, will be created if not exists, default=\"../tmp\".    
     -tmpinputxml:WPATH name of the temporary file parsed from input, default=INPUTFILE.xml   
     -tmpantxml:WPATH   ant.xml-file to generate, default=ant_INPUTFILE.xml                        
     -zbnf4ant:TPATH    zbnf-file to parse the input                                           
     -xslt4ant:TPATH    xslt-file to generate the ant.xml                                      
     </pre>
   *
   */
  public static void main(String [] args)
  { Zmake main = new Zmake();     //the main instance
    CmdLine mainCmdLine = main.new CmdLine(args); //the instance to parse arguments and others.
    main.console = mainCmdLine;  //it may be also another instance based on MainCmd_ifc
    boolean bOk = true;
    try{ mainCmdLine.parseArguments(); }
    catch(Exception exception)
    { main.console.setExitErrorLevel(MainCmd_ifc.exitWithArgumentError);
      bOk = false;
    }
    if(bOk)
    { /** The execution class knows the SampleCmdLine Main class in form of the MainCmd super class
          to hold the contact to the command line execution.
      */
      String sExecuteError = null;
      try{ sExecuteError = main.execute(); }
      catch(Exception exception)
      { //catch the last level of error. No error is reported direct on command line!
        main.console.report("Uncatched Exception on main level:", exception);
        exception.printStackTrace(System.err);
        main.console.setExitErrorLevel(MainCmd_ifc.exitWithErrors);
      }
      if(sExecuteError != null)
      { 
        main.console.reportln(0, sExecuteError);
        main.console.setExitErrorLevel(MainCmd_ifc.exitWithErrors);
      }
    }
    main.console.writeInfoln("* ");
    mainCmdLine.exit();
  }


  
  private class CmdLine extends MainCmd
  {
    /*---------------------------------------------------------------------------------------------*/
    /** Constructor of the CmdLine class.
        The command line arguments are parsed here. After them the execute class is created as composition of SampleCmdLine.
    */
    private CmdLine(String[] args)
    { super(args);
      //:TODO: user, add your help info!
      //super.addHelpInfo(getAboutInfo());
      super.addAboutInfo("Zmake organizer");
      super.addAboutInfo("made by JcHartmut, 2007-07-06 - 2007-10-18");
      super.addHelpInfo("invoke>%JAX_EXE% org.vishia.zbnfXml.Zmake [INPUT] [{OPTIONS}]");
      super.addHelpInfo("* pathes to files or dirs are absolute or relativ from cmd line invocation.");
      super.addHelpInfo("* TPATH means a path started from given -ZBNFJAX_HOME:PATH or ZBNFJAX_HOME in environment.");
      super.addHelpInfo("* WPATH means a path started from given -tmp directory (WorkPATH).");
      super.addHelpInfo("* INPUTFILE is the only filename without path and without extension dissolved from INPUT");
      super.addHelpInfo("INPUT              The first argument without - is the input file with path and extension.");
      super.addHelpInfo("-i:INPUT           path to the input file alternatively to INPUT.");
      super.addHelpInfo("-curdir:PATH       sets the current dir alternatively to command line invocation path.");
      super.addHelpInfo("-ZBNFJAX_HOME:PATH path to the ZBNFJAX_HOME, default it is getted from environment.");
      super.addHelpInfo("-tmp:PATH          path of tmp dir, will be created if not exists, default=\"../tmp\".");
      super.addHelpInfo("-tmpinputxml:WPATH name of the temporary file parsed from input, default=INPUTFILE.xml");
      super.addHelpInfo("-tmpantxml:WPATH   ant.xml-file to generate, default=ant_INPUTFILE.xml");
      super.addHelpInfo("-zbnf4ant:TPATH    zbnf-file to parse the input");
      super.addHelpInfo("-xslt4ant:TPATH    xslt-file to generate the ant.xml");
      super.addStandardHelpInfo();
      
    }
  
  
    /*---------------------------------------------------------------------------------------------*/
    /** Tests one argument. This method is invoked from parseArgument. It is abstract in the superclass MainCmd
        and must be overwritten from the user.
        :TODO: user, test and evaluate the content of the argument string
        or test the number of the argument and evaluate the content in dependence of the number.
  
        @param argc String of the actual parsed argument from cmd line
        @param nArg number of the argument in order of the command line, the first argument is number 1.
        @return true is okay,
                false if the argument doesn't match. The parseArgument method in MainCmd throws an exception,
                the application should be aborted.
    */
    protected boolean testArgument(String arg, int nArg)
    { boolean bOk = true;  //set to false if the argc is not passed
  
      if(nArg==0 && !arg.startsWith("-"))      { input = getArgument(0); }
      else if(arg.startsWith("-i:"))           { input = getArgument(3); }
      else if(arg.startsWith("-i"))            { input = getArgument(2); }
      else if(arg.startsWith("-curdir:"))      { curDir = getArgument(8) + "/"; }
      else if(arg.startsWith("-curdir="))      { curDir = getArgument(8) + "/"; }
      else if(arg.startsWith("-ZBNFJAX_HOME:")){ zbnfjax_PATH = getArgument(14); }
      else if(arg.startsWith("-XML_TOOLBASE:")){ zbnfjax_PATH = getArgument(14); }  //older version, compatibility
      else if(arg.startsWith("-XML_TOOLBASE=")){ zbnfjax_PATH = getArgument(14); }  //older version, compatibility
      else if(arg.startsWith("-tmp:"))         { tmp = getArgument(5); }
      else if(arg.startsWith("-tmp="))         { tmp = getArgument(5); } //older version, compatibility
      else if(arg.startsWith("-tmpinputxml:")) { sInputXml = getArgument(13); }
      else if(arg.startsWith("-tmpantxml:"))   { sAntXml = getArgument(11); }
      else if(arg.startsWith("-antxml="))      { sAntXml = getArgument(8); }  //older version, compatibility
      else if(arg.startsWith("-zbnf4ant:"))    { sZbnf4ant = getArgument(10); }
      else if(arg.startsWith("-zbnf4ant="))    { sZbnf4ant = getArgument(10); }  //older version, compatibility
      else if(arg.startsWith("-xslt4ant:"))    { sXslt4ant = getArgument(10); }
      else if(arg.startsWith("-xslt4ant="))    { sXslt4ant = getArgument(10); }  //older version, compatibility
      else bOk=false;
  
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
       :TODO: the user only should determine the specific checks, this is a sample.
       @return true if successfull, false if failed.
    */
    protected boolean checkArguments()
    { boolean bOk = true;
  
      if(input == null)              
      { bOk = false; 
        writeError("ERROR argument -i is obligat."); 
      }

      if(!bOk) setExitErrorLevel(exitWithArgumentError);
    
      return bOk;
    
    }
  }//class CmdLine

    
  
  /** Execute the task of the class. 
   * @throws ParseException */
  String execute() throws ParseException
  { boolean bOk = true;
    String sError = null;
    //the followed line maybe unnecessary because the java cmd line interpretation always cuts the quotion  marks,
    //Such quotion marks appeares if a double click from commandline is happened. 
    if(input.startsWith("\"") && input.length()>=2){ input = input.substring(1, input.length()-1); }

    /*Separate input path file ext.*/
    String inputFile, inputExt;
    { int pos1 = input.lastIndexOf(('/'));
      int pos2 = input.lastIndexOf(('\\'));
      int pos3 = input.lastIndexOf((':'));
      if(pos2 > pos1){ pos1 = pos2; }
      if(pos3 > pos1){ pos1 = pos3; }
      if(pos1 < 0){ pos1 = -1; }
      int pos9 = input.lastIndexOf('.');
      if(pos9 < pos1) { pos9 = input.length(); }
      inputFile = input.substring(pos1 + 1, pos9); //, pos9);
      inputExt =  input.substring(pos9);
      
      if(curDir == null) 
      { curDir = input.substring(0, pos1 +1);  //"" if no path before filename is given.
        input = inputFile + inputExt;
      } 
      else
      { //input is the full named file, but it is used relative to current dir.
        //curDir is given from command line.
      }
    }
    
    tmpAbs = curDir +tmp;
    
    if(zbnfjax_PATH==null) { zbnfjax_PATH = System.getenv("XML_TOOLBASE"); }
    if(zbnfjax_PATH==null) { zbnfjax_PATH = System.getenv("ZBNFJAX_HOME"); }
    if(zbnfjax_PATH == null)
    { throw new ParseException("ZBNFJAX_HOME is not set. Either you should set a environment variable with this name or you should use the -ZBNFJAX_HOME: cmdline-Argument.", 0);
    }
    if(zbnfjax_PATH.startsWith("\"") && zbnfjax_PATH.length()>=2){ zbnfjax_PATH = zbnfjax_PATH.substring(1, zbnfjax_PATH.length()-1); }
    zbnfjax_PATH += "/";
    
    console.writeInfoln("* Zmake: " + input);
    
    File tmpDir = new File(tmpAbs);
    if(!tmpDir.exists()) { tmpDir.mkdir(); }
    
    if(sInputXml == null)
    { sInputXml = inputFile + inputExt + ".xml"; 
    }
    File fileZbnfXml = new File(tmpAbs + "/" + sInputXml);
    //not available in JEE5: fileSbnfXml.setWritable(true); 
    fileZbnfXml.delete();

    if(sAntXml == null){ sAntXml = "ant_" + inputFile + inputExt + ".xml"; }
    File fileAnt = new File(tmpAbs + "/" + sAntXml);
    //not available in JEE5: fileAnt.setWritable(true); 
    fileAnt.delete();
    
    console.writeInfoln("* Zmake: parsing \"" + curDir + input + "\" with \"" + zbnfjax_PATH + sZbnf4ant + "\" to \""  + fileZbnfXml.getAbsolutePath() + "\"...");
    //call the parser from input, it produces a temporary xml file.
    String sInputAbs = curDir + input;
    String sZbnf =zbnfjax_PATH + sZbnf4ant;
    String sInputAbs_xml = tmpAbs + "/" + sInputXml;
    Zbnf2Xml sbnfParser = new Zbnf2Xml(sInputAbs, sZbnf, sInputAbs_xml, console); 
    
    bOk = sbnfParser.execute();
    if(!bOk) throw new ParseException("zbnf syntax error",0);
    console.writeInfo("done");
    //create the new xsl file from xslp if necessary
    File fileXsl4Ant;
    if(!sXslt4ant.endsWith(".xslp"))
    { fileXsl4Ant = new File(zbnfjax_PATH + sXslt4ant);
    }
    else  
    { int posSep = sXslt4ant.lastIndexOf('/');
      String sNameXsl = sXslt4ant.substring(posSep +1, sXslt4ant.length()-1);  //without last "p" from "xslp"
      String sPathXsl = sXslt4ant.substring(0, posSep+1);
    
      File dirGen= new File(zbnfjax_PATH + sPathXsl + "gen");
      if(!dirGen.exists())
      { dirGen.mkdir();
      }
      
      fileXsl4Ant= new File(zbnfjax_PATH + sPathXsl + "gen/" + sNameXsl);
      File fileXslp = new File(zbnfjax_PATH + sXslt4ant);

      if(!fileXslp.exists())
      { bOk = false; 
        console.writeError("* Zmake: " + fileXslp.getAbsolutePath() + " not exists.");
        sError = "Xsl File Ant.xml failed";
      }
        
      if(!fileXsl4Ant.exists() || fileXslp.lastModified() > fileXsl4Ant.lastModified())
      { console.writeInfoln("* Zmake: generate " + fileXsl4Ant.getAbsolutePath() + " from " + fileXslp.getAbsolutePath());
        Xsltpre xsltpre = new Xsltpre(fileXslp,fileXsl4Ant);
        xsltpre.execute();
      }
      else
      { console.writeInfoln("* Zmake: generated " + fileXsl4Ant.getAbsolutePath() + " is uptodate.");
      }
    }
    
    if(!fileXsl4Ant.exists())
    { bOk = false; 
      console.writeError("* Zmake: " + fileXsl4Ant.getAbsolutePath() + " not exists.");
      sError = "Xsl File Ant.xml failed";
    }
      
    if(bOk)
    { System.setProperty("javax.xml.transform.TransformerFactory", "net.sf.saxon.TransformerFactoryImpl");
      //The SAXON TransformerFactory is instanciated because above System.setProperty()
      TransformerFactory     tfactory   = TransformerFactory.newInstance();
      Transformer transformer = null;
      console.writeInfoln("* Zmake: preparing net.sf.saxon as XSLT-translator with " + fileXsl4Ant.getAbsolutePath() + " ...");
      try { transformer = tfactory.newTransformer(new StreamSource(fileXsl4Ant)); }
      catch (TransformerConfigurationException e)
      { bOk = false; 
        sError="The saxon transformer couldn't be find: net.sf.saxon.TransformerFactoryImpl.class. The .class saxon*.jar have to be part of the classpath.";
      }
      if(bOk)
      { console.writeInfo("done");
        console.writeInfoln("* Zmake: translating " + fileZbnfXml.getAbsolutePath() + " ...");
        Properties  oprops     = new Properties();
        oprops.put(javax.xml.transform.OutputKeys.INDENT, "yes");
        //oprops.put("method", "xml");
        transformer.setOutputProperties(oprops);
        transformer.setParameter("tmp", tmp);  //the argument -tmp: as String from current directory. 
        try { transformer.transform(new StreamSource(fileZbnfXml),new StreamResult(fileAnt)); }
        catch (TransformerException e)
        { bOk = false;
          sError = "xslt exception: " + e.getMessage();
        }
        if(bOk)
        { console.writeInfo("done");
        }
      }

    }
    
    return sError;
  }
  
  
}
