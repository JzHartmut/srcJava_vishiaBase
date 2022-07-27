package org.vishia.header2Reflection;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;

import org.vishia.mainCmd.MainCmd;
import org.vishia.mainCmd.MainCmd_ifc;
import org.vishia.mainCmd.Report;
import org.vishia.util.FileSystem;
import org.vishia.util.StringPartFromFileLines;
import org.vishia.util.StringPartScan;
import org.vishia.zbnf.ZbnfParseResultItem;
import org.vishia.zbnf.ZbnfParser;

/**This class is able to parse C-language files (header, implementation) and outputs all dependencies of variables. 
 * @author Hartmut Schorrig
 *
 */
public class C2DataDeps  extends MainCmd {
  
  
  
  /**Revision number
   * <ul>
   * <li>2012-06-08: Creation, adapted from Header2Reflection!
   * 
   * </ul>
   */
  static public final int revisionInfo = 20120608;
  
  
  
  /**Aggregation to the Console implementation class.*/
  final Report console;



  /**Input files. */
  private final List<FileSystem.FileAndBasePath> listFileIn = new LinkedList<FileSystem.FileAndBasePath>();

  /**The parser. */
  private ZbnfParser parser;

  private String sFileZbnf;

  
  private String sOutDir;


  protected List<ZbnfParseResultItem> listParsed = new LinkedList<ZbnfParseResultItem>();
  
  /*---------------------------------------------------------------------------------------------*/
  /** main started from java*/
  public static void main(String [] args)
  { C2DataDeps main = new C2DataDeps(args); //the instance to parse arguments and others.
    boolean bOk = true;
    try{ main.parseArguments(args); }
    catch(Exception exception)
    { main.setExitErrorLevel(MainCmd_ifc.exitWithArgumentError);
      bOk = false;
    }
    if(bOk)
    { /** The execution class knows the CmdHeader2Reflection Main class in form of the MainCmd super class
          to hold the contact to the command line execution.
      */
      try{ main.execute(); }
      catch(Exception exception)
      { //catch the last level of error. No error is reported direct on command line!
        main.console.report("Uncatched Exception on main level:", exception);
        exception.printStackTrace(System.err);
        main.console.setExitErrorLevel(MainCmd_ifc.exitWithErrors);
      }
    }
    main.exit();
  }

  
  
  /**The main instance to work, also settled outside calling per cmdLine. */
  //C2DataDeps main;

  /**Cmdline-argument, set on -i option. Inputfile to to something. :TODO: its a example.*/
  String sFileIn = null;

  /*---------------------------------------------------------------------------------------------*/
  /** Constructor of the main class.
      The command line arguments are parsed here. After them the execute class is created as composition of CmdHeader2Reflection.
  */
  private C2DataDeps(String[] args)
  { super(args);
    console = this;
    //:TODO: user, add your help info!
    //super.addHelpInfo(getAboutInfo());
    super.addAboutInfo("Generate Data dependency report from C-files");
    super.addAboutInfo("made by Hartmut Schorrig, 2012-06-07");
    super.addHelpInfo("param: {-i:INPUT} [-o:OUTDIR |-out.c:OUTFILE | -obin[l|b]:BINFILE -offs:OFFSETFILE ] -z:CHeader.zbnf -b:Types.cfg -c_only -ro:FileTypes.out");
    super.addStandardHelpInfo();
    super.addHelpInfo("-i:INPUT    inputfilepath, more as one -i is admissible and typical.");
    super.addHelpInfo("-o:OUTDIR For any INPUT one output file in this directory.");
    super.addHelpInfo("-out.c:OUTFILE The file path and name for OUTFILE.c and OUTFILE.h, this file is written.");
    super.addHelpInfo("-offs:OFFSETFILE An extra file.c for offset-constants for second CPU.");
    super.addHelpInfo("-obinl:BINFILE in little endian, contains all structure informations, only able to use with -offs:.");
    super.addHelpInfo("-obinb:BINFILE Same, but the BINFILE will written in big endian.");
    super.addHelpInfo("-obin[b|l]hex:HEXFILE The BINFILE will written in Intel-hex-Format.");
    super.addHelpInfo("-c_only Don't reagard C++-classes, don't generate C++-code especially static_cast<...> .");
    super.addHelpInfo("-b:Types.cfg Config-file for special handled types.");
    super.addHelpInfo("Syntax of the config-file: ------------------------------------------------------------");
    super.addHelpInfo(Header2Reflection.sSyntaxReflectionTypes);
    super.addHelpInfo("---------------------------------------------------------------------------------------");
  }


  /**The parseArguments is replaced by a variant with reference to the working class, 
   * using the super.parseArguments() indside. 
   */
  public void parseArguments(Header2Reflection main, String [] args)
  throws ParseException
  { //this.main = main;
    super.parseArguments(args);
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
   * @throws FileNotFoundException 
  */
  @Override
  protected boolean testArgument(String arg, int nArg)
  { boolean bOk = true;  //set to false if the argc is not passed

    if(arg.startsWith("-i:"))     {
      String sFile = getArgument(3);
      try{
        if(!FileSystem.addFilesWithBasePath(null, sFile, listFileIn)){
          writeError("file not found: " + sFile);
          bOk = false;
        }
      } catch(Exception exc){
        writeError("file not found: " + sFile);
        bOk = false;
      }
    }
    else if(arg.startsWith("-o:")) bOk = setOutDir(getArgument(3));
    else if(arg.startsWith("-z:")) this.sFileZbnf = getArgument(3);
    else bOk=false;

    return bOk;
  }

  /** Invoked from parseArguments if no argument is given. In the default implementation a help info is written
   * and the application is terminated. The user should overwrite this method if the call without comand line arguments
   * is meaningfully.
   * @throws ParseException 
   *
   */
  @Override
  protected void callWithoutArguments() throws ParseException
  { //:TODO: overwrite with empty method - if the calling without arguments
    //having equal rights than the calling with arguments - no special action.
    super.callWithoutArguments();  //it needn't be overwritten if it is unnecessary
  }




  /*---------------------------------------------------------------------------------------------*/
  /**Checks the cmdline arguments relation together.
     If there is an inconsistent, a message should be written. It may be also a warning.
     :TODO: the user only should determine the specific checks, this is a sample.
     @return true if successful, false if failed.
  */
  @Override
  protected boolean checkArguments()
  { return true;
  
  }
 

  
  boolean setOutDir(String sFile)
  { boolean bOk = true;
    this.sOutDir = sFile.replace('\\', '/');
    try { FileSystem.mkDirPath(sOutDir);} 
    catch (FileNotFoundException e) {
      bOk = false;
    }
    return bOk;
  }

  
  
  
  boolean init()
  throws IllegalCharsetNameException, UnsupportedCharsetException, FileNotFoundException, IOException
  { boolean bOk= true;
    if(sFileZbnf == null) {
      /**No -z: option, than standard file used: */
      String sZbnfHome = System.getenv("ZBNFJAX_HOME");
      if(sZbnfHome == null) {
        console.writeError("No -z:SYNTAX option, and the environment variable ZBNFJAX_HOME is unknown, abort.");
        bOk = false;
      } else {
        sFileZbnf = sZbnfHome + "/zbnf/Cheader.zbnf";
      }
    }
    if(bOk){
      File fileSyntax= new File(sFileZbnf);
      String sSyntax = FileSystem.readFile(fileSyntax);
      if(sSyntax == null){
        console.writeError("fault SYNTAX file, abort:" + sFileZbnf);
        bOk = false;
      } else {
        parser = new ZbnfParser(console, 10);
        try{ parser.setSyntax(sSyntax); }
        catch(ParseException exc)
        { console.writeError("Headersyntax fault in " + sFileZbnf, exc);
          parser.reportSyntax(console, Report.fineInfo);
          bOk = false;
        }
        parser.setReportIdents(Report.error, Report.info, Report.debug, Report.fineDebug);
      }  
    }
    return bOk;
  }



  
  
  
  void execute(){
    boolean bOk = true;
    try{ init();
    
    } catch(Exception exc){
      System.err.println("C2DataDeps.execute - any exception while init; " + exc.getMessage());
      bOk = false;
    }
    if(bOk){
      for(FileSystem.FileAndBasePath fileEntry: listFileIn){
        parseCfile(fileEntry.file);
      }
    }
  }

  
  
  
  
  boolean parseCfile(File headerFile){
    boolean bOk = true;
    StringPartScan spInput = null;
    try{ 
      spInput = new StringPartFromFileLines(headerFile,-1, null, null);
    } catch(FileNotFoundException exc){
      System.err.println("C2DataDeps.parseCfile - Unexpected file not found; " + exc.getMessage());
      bOk = false;
    } catch(Exception exc){
      System.err.println("C2DataDeps.parseCfile - Any exception; " + exc.getMessage());
      bOk = false;
    }
    bOk = parser.parse(spInput);
    if(!bOk)
    { String sError = parser.getFoundedInputOnError();
      //console.writeError(sError);
      sError = parser.getExpectedSyntaxOnError();
      //console.writeError(sError);
      sError = parser.getSyntaxErrorReport();
      console.writeError(sError);
      stop();

    }
    else {
      parser.reportStore(console, Report.fineInfo);
      ZbnfParseResultItem  resultItem = parser.getFirstParseResult();
      listParsed.add(resultItem);
    }
    return bOk;
  }
  
  /**Helper method for debugging. */
  void stop()
  { //debug
  }

}
