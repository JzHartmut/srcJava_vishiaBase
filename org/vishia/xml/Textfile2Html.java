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
 * 2009-12-31: Hartmut corr: Creation of a directory of output path. 
 * 2005 Hartmut created
 */
package org.vishia.xml;

import java.io.*;

import org.vishia.mainCmd.*;
import org.vishia.util.FileSystem;

/** Class for conversion of text files, at example XML Files, into a browser visible HTML-format.</br>
    The problem is: Some browsers shows files with xml content and with the extension txt
    yet in form of xml files with hardheaded interpretation. If the content of the file
    will be shown only in text format, all things of xml like scripting should be hided,
    especially the tag structur. The <-char is converted to &lt; A HTML frame is generated.
*/

public class Textfile2Html extends MainCmd
{

  /**Cmdline-argument, set on -i option. Inputpath.*/
  String sPathIn = null;

  /**Cmdline-argument, set on -o option. Outputpath.*/
  String sPathOut = null;

  /**Cmdline-argument, set on -f option. Filename.*/
  String sFile = "";

  /**Cmdline-argument, set on -linesize: option.*/
  int maxLineSize = 120;
  
  /** String of spaces to provide some spaces for indent*/
  static String sIndent = "                              ";

  /**Writer of the output used internally.*/
  private BufferedWriter writer;
    
  
  
  /*---------------------------------------------------------------------------------------------*/
  /** main started from java
   * The command line arguments follow this help text: <pre>
    super.addHelpInfo("param: -iINPUT -oOUTPUT -fFILE -linesize:size");
    super.addStandardHelpInfo();
    super.addHelpInfo("-iINPUT    inputfilepath, a xml like file.");
    super.addHelpInfo("-oOUTPUT   outputfilepath, this file is written.");
    super.addHelpInfo("-fFILE     if this argument is given, read from INPUT/FILE, write to OUTPUT/FILE.html.");
    super.addHelpInfo("-linesize: length of a line, a wrapping is generated after it.");
    </pre>
   * */
  public static void main(String [] args)
  { Textfile2Html main = new Textfile2Html(args);
    main.execute();
    main.exit();
  }


  /** Executes the cmd-line-application. The functionality application class is created inside this method
      independent of the command line invoke.
  */
  void execute()
  { boolean bOk = true;
    try{ super.parseArguments(); }
    catch(Exception exception)
    { setExitErrorLevel(MainCmd_ifc.exitWithArgumentError);
      bOk = false;
    }

    if(bOk)
    { /** The execution class knows the Textfile2Html Main class in form of the MainCmd super class
          to hold the contact to the command line execution.
      */

      try
      { convert(); 
      }
      catch(Exception exception)
      { //catch the last level of error. No error is reported direct on command line!
        report("Abort the routine:", exception);
        setExitErrorLevel(MainCmd_ifc.exitWithErrors);
      }
    }
    //note: exit the command line application in static main()
  }





  /*---------------------------------------------------------------------------------------------*/
  /** Constructor of the main class.
      The command line arguments are parsed here. After them the execute class is created as composition of Textfile2Html.
  */
  Textfile2Html(String[] args)
  { super(args);
    //super.addHelpInfo(getAboutInfo());
    super.addAboutInfo("Translator text files to a html readable represantiation");
    super.addAboutInfo("made by JcHartmut, 2006-02-25");
    super.addHelpInfo("param: -iINPUT -oOUTPUT -fFILE -linesize:size");
    super.addStandardHelpInfo();
    super.addHelpInfo("-iINPUT    inputfilepath, a xml like file.");
    super.addHelpInfo("-oOUTPUT   outputfilepath, this file is written.");
    super.addHelpInfo("-fFILE     if this argument is given, read from INPUT/FILE, write to OUTPUT/FILE.html.");
    super.addHelpInfo("-linesize: length of a line, a wrapping is generated after it.");

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
  public boolean testArgument(String arg, int nArg)
  { boolean bOk = true;  //set to false if the argc is not passed

    if(arg.startsWith("-i"))             { sPathIn    = getArgument(2);}
    else if(arg.startsWith("-o"))        { sPathOut   = getArgument(2);}
    else if(arg.startsWith("-f"))        { sFile      = getArgument(2);}
    else if(arg.startsWith("-linesize:")){ maxLineSize= Integer.parseInt(getArgument(10)); }
    else bOk=false;

    return bOk;
  }





  /*---------------------------------------------------------------------------------------------*/
  /**Checks the cmdline arguments relation together.
     If there is an inconsistents, a message should be written. It may be also a warning.
     @return true if successfull, false if failed.
  */
  protected boolean checkArguments()
  { boolean bOk = true;

    if(sPathIn == null)            { bOk = false; writeError("ERROR argument -i is obligat."); }
    else if(sPathIn.length()==0)   { bOk = false; writeError("ERROR argument -i without content.");}

    if(sPathOut == null)           { writeWarning("argument -o no outputfile is given, use default"); sPathOut = "out.txt";}
    else if(sPathOut.length()==0)  { bOk = false; writeError("argument -o without content"); }

    if(!bOk) setExitErrorLevel(exitWithArgumentError);

    return bOk;

 }

  
  void convert()
  throws Exception
  { 
    BufferedReader reader = null;
    if(sFile.length()>0 ){ sPathIn = sPathIn + "/" + sFile; }
    try
    { reader = new BufferedReader(new FileReader(sPathIn));
    }
    catch(FileNotFoundException exception)
    { writeError("input file not found", exception);
      setExitErrorLevel(exitWithFileProblems);
      throw exception;
    }
    
    if(sFile.length()>0 ){ sPathOut = sPathOut + "/" + sFile + ".html"; }
    try
    { FileSystem.mkDirPath(sPathOut);  //if the dst directory doesn't exists.
      writer = new BufferedWriter(new FileWriter(sPathOut));
    }
    catch(FileNotFoundException exception)
    { writeError("output file not found", exception);
      setExitErrorLevel(exitWithFileProblems);
      throw exception;
    }

    report(info, "translate " + sPathIn + " to " + sPathOut);
    
    writer.write("<html><head><title>" + sPathIn + "</title></head>\n<body><pre>\n");
    
    String sLine;
    while( (sLine = reader.readLine()) != null)
    { /**ascertain the indentitation of the block of this line*/
      int indent = 0;
      while(sLine.length() > indent && sLine.charAt(indent) == ' '){ indent +=1; }
      sLine = sLine.substring(indent);  //without indentitation.
      //don't exaggerate it. We haven't enaugh room.
      //note, that 2 addtional spaces are needed for wrapping a line
      if(indent > sIndent.length()-2){ indent = sIndent.length()-2; }
      
      /** Cut the line if it is to long*/
      int indentWrapped = indent;  //first line with same indent.
      int maxLength = maxLineSize - indentWrapped;
      while(sLine.length() > maxLength)
      { int posLast=sLine.lastIndexOf(' ', maxLength);
        if(posLast < 0) posLast = maxLength; //cutting it at pos 80
        printLine( sIndent.substring(0, indentWrapped) + sLine.substring(0, posLast));
        //continue AFTER space, otherwise an infinite loop occurs!!!
        sLine = sLine.substring(posLast +1);
        indentWrapped = indent +2;  //next wrapped lines with higher indent.
        maxLength = maxLineSize - indentWrapped;
      }
      printLine( sIndent.substring(0, indentWrapped) + sLine);  //print the rest.
      
    }
    writer.write("\n</pre>\n</body></html>\n");
    writer.close();
    writer = null;  //no longer used.
    reportln(info,"....successfull.");
  }
  
  void printLine(String sLine) 
  throws IOException
  { { //the main task: replace all < with the named periphrases.
      sLine = sLine.replaceAll("&", "&amp;");
      sLine = sLine.replaceAll("<", "&lt;");
      sLine = sLine.replaceAll(">", "&gt;");
      writer.write(sLine);
      writer.write("\n");
    }
    
  }
  
  
  
  
  
  
  
}



                           