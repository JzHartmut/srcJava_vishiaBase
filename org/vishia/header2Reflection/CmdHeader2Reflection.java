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
 * 2010-01-05 Hartmut mkdir of outdir if doesn't exists
 * 2007..2009: Hartmut: some changes
 * 2007 Hartmut created
 */
package org.vishia.header2Reflection;

import java.io.FileNotFoundException;
import java.text.ParseException;

import org.vishia.mainCmd.MainCmd;
import org.vishia.mainCmd.MainCmd_ifc;
import org.vishia.mainCmd.Report;


/**Class for command line invokation. */
public class CmdHeader2Reflection extends MainCmd
{
  
  
  
  /**Revision number
   * <ul>
   * <li>2011-04-11: Dependency to the control file: regard it in maker. To header and footer too!
   * 
   * </ul>
   */
  static public final int revisionInfo = 20110411;
  
  
  
  /**Aggregation to the Console implementation class.*/
  Report console;




  /*---------------------------------------------------------------------------------------------*/
  /** main started from java*/
  public static void main(String [] args)
  { CmdHeader2Reflection mainCmdLine = new CmdHeader2Reflection(args); //the instance to parse arguments and others.
    Header2Reflection main = new Header2Reflection(mainCmdLine);     //the main instance
    boolean bOk = true;
    try{ mainCmdLine.parseArguments(main, args); }
    catch(Exception exception)
    { mainCmdLine.setExitErrorLevel(MainCmd_ifc.exitWithArgumentError);
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
    mainCmdLine.exit();
  }

  
  
  /**The main instance to work, also settled outside calling per cmdLine. */
  Header2Reflection main;

  /**Cmdline-argument, set on -i option. Inputfile to to something. :TODO: its a example.*/
  String sFileIn = null;

  /*---------------------------------------------------------------------------------------------*/
  /** Constructor of the main class.
      The command line arguments are parsed here. After them the execute class is created as composition of CmdHeader2Reflection.
  */
  private CmdHeader2Reflection(String[] args)
  { super(args);
    //:TODO: user, add your help info!
    //super.addHelpInfo(getAboutInfo());
    super.addAboutInfo("Generate Reflection.c and Reflection.h");
    super.addAboutInfo("made by Hartmut Schorrig, 2009-11-22");
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
  { this.main = main;
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

    if(arg.startsWith("-i:"))      bOk = main.addInputFilemask(getArgument(3));
    else if(arg.startsWith("-o:")) bOk = main.setOutDir(getArgument(3));
    else if(arg.startsWith("-z:")) main.setSyntax(getArgument(3));
    else if(arg.startsWith("-r:")) main.setReflectionTypes(getArgument(3));
    else if(arg.startsWith("-b:")) main.setReflectionBlockedTypes(getArgument(3));
    else if(arg.startsWith("-ro:")) main.setReflectionTypesOut(getArgument(4), true);
    else if(arg.startsWith("-ro+")) main.setReflectionTypesOut(getArgument(4), false);
    else if(arg.startsWith("-out.c:")) bOk = main.setOutC(getArgument(7));
    else if(arg.startsWith("-offs:")) bOk = main.setOutOffset(getArgument(6));
    else if(arg.startsWith("-obinl:")) bOk = main.setOutBin(getArgument(7), false,false);
    else if(arg.startsWith("-obinb:")) bOk = main.setOutBin(getArgument(7), true, false);
    else if(arg.startsWith("-obinlhex:")) bOk = main.setOutBin(getArgument(10), false,true);
    else if(arg.startsWith("-obinbhex:")) bOk = main.setOutBin(getArgument(10), true, true);
    else if(arg.startsWith("-c_only")) main.c_only = true;
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
 
  
}