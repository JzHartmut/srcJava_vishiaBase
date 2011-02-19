package org.vishia.xmlSimple;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
//import java.text.ParseException;
import java.util.TreeMap;

import org.vishia.util.StringPart;

import org.vishia.mainCmd.*;
//import vishia.stringScan.StringPart;

/**This class is the wrapper for the command line invokation of a XSLT preprocessor
 * for text output. The intension of preprocessing a XSLT script is shown in followed:<br>
 * 
 * For text output, it is convenient for software support to write the lines
 * in XSL script in the order of the output file. But if some informations from XML
 * are merged in them, the necessitative expression may be a little long and not clearly.
 * A short form will be better.
 * <br>
 * The class converts the given script line per line, it is a simple displacement
 * of textual parts, combined with the possibility of alias names especially for 
 * XPATH expressions.
 * <br>
 * The key strings of special expressions begin with the &lt;-character like XML,
 * but followed by a special character outside the XML possibilities. So it is
 * plain detectable by human in the script and non-ambiguous detectable by the conversion routine.
 * The followed expressions will be detected and converted:
 * <table border=1 width=100%>
 * <tr><td><code>(?=<i>ident</i>=<i>XPATH</i>?)</code>
 * </td><td>The XPATH expression is stored and assigned to the alias <code><i>ident</i></code>.
 * </td></tr>
 * <tr><td><code>(?!<i>path</i>?)</code>
 * </td><td>Value of the element or attribut given by expression. <code><i>path</i></code>
 *   is either a XPATH expression or, if starting with <code>=</code> an alias representing a XPATH.
 *   The resulting code in XSLT is:<br>
 *   <code>&lt;/xsl:text&gt;&lt;xsl:value-of select="<i>path</i>" /&gt;&lt;xsl:text&gt;</code><br>
 * </td></tr>
 * <tr><td><code>(?:<i>name</i>[:<i>path</i>](<i>parameter</i>=<i>value</i>, ...)?)</code>
 * </td><td>call of the named template <code><i>name</i></code> with selecting 
 *   the <code><i>path</i></code> before, with the named <code><i>parameter</i></code> 
 *   with theire given <code><i>value</i></code>.<br>
 *   <ul><li>The <code><i>path</i></code> should be written in "" if it is given directly. 
 *     The path may be given also as alias, if started with <code>=</code>.</li>
 *   <li>
	 *   The <code><i>value</i></code> of the parameters may be given as followed:
	 *   <table witdht=100% border=1>
	 *   <tr><td>*xpath
	 *   </td><td>A XPATH expression
	 *   </td></tr>
	 *   <tr><td>=alias
	 *   </td><td>A XPATH expression given with the alias
	 *   </td></tr>
	 *   <tr><td>'text'
	 *   </td><td>A constant text expression.
	 *   </td></tr>
	 *   </table>
	 * </li>  
 *   <li>Some expressions may be concated with " + ". See example.
 *   If no parameter are given, write <code>()</code>. Separate parameters with a colon <code>,</code>.</li>
 *   
 *   <li>The resulting code in XSLT is:<br>
 *   <code>&lt;/xsl:text&gt;&lt;xsl:for-each select="<i>path</i>"&gt;
 *   &lt;xsl:call-template name="<i>name</i>"&gt;
 *   &lt;xsl:with-param name="<i>parameter</i>"&gt;...(value)&lt;/xsl:with-param&gt;
 *   &lt;/xsl:call-template&gt;
 *   &lt;xsl:text&gt;</code><br>
 *   If no <code><i>path</i></code> is given by writing <code>&lt;:<i>path</i>:<i>name</i>(...</code>,
 *   no <code>&lt;xsl:for-each ...</code> wrapping is done.
 *   </li></ul> 
 * </td></tr>
 * </table>  
 * <br>Example:<br>
 * The code (TODO!!!)
 * <pre>(?=myPath=element1/element2/@value?)
 * ...&lt;xsl:text>
 * The text with info:(?@=myPath?) and info2:(?@attrib?) is written
 * with (?:callname()?) and with (?:callname2:"callpath"(par1='text' + xxx)?)&lt;/xsl:text>
 * </pre>
 * is converted to
 * <pre> ... TODO
 * </pre>
 * 
 */

public class Xsltpre
{
  /**Cmdline-argument, set on -i option. Inputfile to to something.*/
  String sFileIn = null;

  /**Cmdline-argument, set on -o option. Outputfile to output something.*/
  String sFileOut = null;

  /**encoding argument*/
  String sEncoding = "ISO-8859-1";

  /**The input xsl file with special entries. */
  File fIn;
  
  /**The generated xsl file in standard format.*/
  File fOut;

  /**If false than generate fOut always, if true than generate only if the fIn is newer. */
  boolean bGenerateOnlyifNecessary = true;
  

  final String[] sKeys;
  MainCmd_ifc console;
  
  
  public Xsltpre(File fIn, File fOut)
  { this();
    this.fIn = fIn;
    this.fOut = fOut;
  }
  
  
  private Xsltpre()
  { sKeys = new String[]
    { "<="  //alias
    , "<@"  //value-of
    , "<:"  //call-template
    , "<:call "  //call-template
    , "<?if "  //choose - when
    , "<?elif "  //choose - when
    , "<?else "  //choose - when
    , "</?if>"  //when - end
    , "</?else>"  //when - end
    , "(?="  //alias  #kk
    , "(?!"  //value-of path or alias 
    , "(?@"  //value-of attribute
    , "(?$"  //value-of variable 
    , "(?call "  //call-template
    , "(?if "  //choose - when
    , "(?elif "  //when
    , "(?else?)"  //choose - when
    , "(?ifNext?)"  //choose - when test="last() > postion()"
    , "(?/if?)"  //when - end
    , "(?/else?)"  //when - end
    };
  }

  TreeMap<String, String> aliases;
  
  
  
  
  /**Generate the output file, but only if it is older as the input, or it don't exists, 
   * or bGenerateAlways is set to true.
   *
   */
  public void execute()
  { boolean bOk = true;
  	
    if( !bGenerateOnlyifNecessary || !fOut.exists() || (fOut.lastModified() < fIn.lastModified()) )
    { 
      if(fOut.exists()) 
      { //not available in JEE5: fOut.setWritable(true); 
        fOut.delete(); 
      } 
      aliases = new TreeMap<String, String>();
      LineNumberReader reader = null;
      FileWriter writer = null;
      try{ reader = new LineNumberReader(new FileReader(fIn));}
      catch (FileNotFoundException exc)
      { console.writeError("file not found: " + sFileIn ); 
      	console.setExitErrorLevel(Report.exitWithFileProblems);
      	bOk = false;
      }
      if(bOk)
      { try{ writer = new FileWriter(fOut);}
        catch (IOException exc)
        { console.writeError("file create/open error: " + sFileOut ); 
        	console.setExitErrorLevel(Report.exitWithFileProblems);
        	bOk = false;
        }
      }
      if(bOk)
      {
      	try
        { String sLineIn;
          StringPart spLineIn = new StringPart();
          int[] idxKey = new int[1];
          StringBuffer sLineOut = new StringBuffer(12000);
          do
          { sLineIn = reader.readLine();
            sLineOut.setLength(0);
            if(sLineIn != null)
            { spLineIn.assign(sLineIn);
              do
              { if(spLineIn.startsWith("(?if"))
              	  stop();
              	spLineIn.seekAnyString(sKeys, idxKey);
                sLineOut.append(spLineIn.getLastPart());
                if(spLineIn.found())
                { switch(idxKey[0])
                  { case 0: setAliasOld(spLineIn); break;
                  case 1: setValueOfOld(sLineOut, spLineIn); break;
                  case 2: setCallTemplateOld(sLineOut, spLineIn); break;
                  case 3: setCallTemplateOld(sLineOut, spLineIn); break;
                  case 4: setIfOld(sLineOut, spLineIn); break;
                  case 5: setElifOld(sLineOut, spLineIn); break;
                  case 6: setElseOld(sLineOut, spLineIn); break;
                  case 7: setIfEndOld(sLineOut, spLineIn); break;
                  case 8: setElseEndOld(sLineOut, spLineIn); break;
                  case 9: setAlias(spLineIn); break;
                  case 10: setValueOf(sLineOut, spLineIn); break;
                  case 11: setValueOf(sLineOut, spLineIn); break;
                  case 12: setValueOf(sLineOut, spLineIn); break;
                  case 13: setCallTemplate(sLineOut, spLineIn); break;
                  case 14: setIf(sLineOut, spLineIn); break;
                  case 15: setElif(sLineOut, spLineIn); break;
                  case 16: setElse(sLineOut, spLineIn); break;
                  case 17: setIfNext(sLineOut, spLineIn); break;
                  case 18: setIfEnd(sLineOut, spLineIn); break;
                  case 19: setElseEnd(sLineOut, spLineIn); break;
                  default: throw new RuntimeException("unexpected choice:" + idxKey[0]);
                  }
                }
              }while(spLineIn.found());  
            }
            sLineOut.append('\n');
            writer.write(sLineOut.toString());
            sLineOut.setLength(0);
          }while(sLineIn != null);
          writer.close();
        }
        catch(IOException exc)
        {
          System.out.println("file exception" + exc.getMessage());
        }
      }
    }  
  }
  
  
  private void setAlias(StringPart spLineIn)
  { //input: (?=key=value?)
    spLineIn.seek(3).lento('=');
    String sKey = spLineIn.getCurrentPart();
    spLineIn.fromEnd().seek(1).lento("?)");
    aliases.put(sKey, spLineIn.getCurrentPart());
    spLineIn.fromEnd().seek(2);
  }
  
  private void setValueOf(StringBuffer sLineOut, StringPart spLineIn)
  { //input: (?!value?) or (?!=alias?) or (?$variable?) or (?@attribute?)
  	char cType = spLineIn.seek(2).getCurrentChar();
  	spLineIn.seek(1).lento("?)");
    String sValue;
    if(spLineIn.getCurrentChar() == '=')
    { String sKey = spLineIn.seek(1).getCurrentPart();
      sValue = (String)(aliases.get(sKey));
    }
    else
    { sValue = spLineIn.getCurrentPart();
    }
    sLineOut.append("</xsl:text><xsl:value-of select=\"");
    if(cType != '!'){ sLineOut.append(cType); }
    sLineOut.append(sValue);
    sLineOut.append("\" /><xsl:text>");
    spLineIn.fromEnd().seek(2);
  }
  
  
  private void setCallTemplate(StringBuffer sLineOut, StringPart spLineIn)
  { //input: (?call name[:"select"](pname=par, pname=par)?) 
    //par may be 'text' or @alias or $variable or *xpath concated bei " + "
    //example (?call callName:"element"(par=@alias + 'text' + *path, par2='text')?)
    spLineIn.seek(7).lentoAnyChar(":(?");
    String sCall = spLineIn.getCurrentPart();
    char cSep = spLineIn.fromEnd().getCurrentChar();
    String sSelect;
    
    sLineOut.append("</xsl:text>");
    if(cSep == ':')
    { spLineIn.seek(1).lentoQuotionEnd('\"', Integer.MAX_VALUE);
      sSelect = spLineIn.getCurrentPart();
      sLineOut.append("<xsl:for-each select=");
      sLineOut.append(sSelect);
      sLineOut.append(">");
      cSep = spLineIn.fromEnd().getCurrentChar();
    }
    else
    { sSelect = null;
    }
    
    sLineOut.append("<xsl:call-template name=\"");
    sLineOut.append(sCall);
    if(sCall.equals("ast"))
    	stop();
            
    if(cSep == '(')
    { sLineOut.append("\">");
        
      spLineIn.seek(1);
      while(spLineIn.getCurrentChar() != ')')
      {
        spLineIn.lentoAnyChar("=");
        String sParamName = spLineIn.getCurrentPart();
        spLineIn.fromEnd().seek(1).lentoAnyChar(",)");
        String sParamValue = spLineIn.getCurrentPart();

        sLineOut.append("<xsl:with-param name=\"");
        sLineOut.append(sParamName);
        sLineOut.append("\" ");
        if(sParamValue.indexOf(" + ") >= 0)
        { setParamValueMultiContent(sLineOut, sParamValue);
        }
        else
        { setParamValueSelect(sLineOut, sParamValue);        
        }
        if(spLineIn.fromEnd().getCurrentChar() == ',')
        { spLineIn.seek(2);  // the user has to write ", nextname"
        }
      }      
      sLineOut.append("</xsl:call-template>");
    }
    else //no (), without param
    { sLineOut.append("\" />");
    }
    if(sSelect != null)
    { sLineOut.append("</xsl:for-each>");
    }
    sLineOut.append("<xsl:text>");
    spLineIn.seek("?)", StringPart.seekEnd);
  }
  
  private void setParamValueSelect(StringBuffer sLineOut, String sParamValue)
  { if(sParamValue.startsWith("="))
    { String sKey = sParamValue.substring(1);
      sParamValue = "\"" + (String)(aliases.get(sKey)) + "\"";
    }
    sLineOut.append(" select=");
    sLineOut.append(sParamValue);
    sLineOut.append(" />");
  }
  
  
  private void setParamValueMultiContent(StringBuffer sLineOut, String sParamValue)
  { if(sParamValue.startsWith("*"))
    { sParamValue = sParamValue.substring(1);
    }
    sLineOut.append("\" select=\"");
    sLineOut.append(sParamValue);
    sLineOut.append("\" />");
  }
  
  private void setIf(StringBuffer sLineOut, StringPart spLineIn)
  { //input: (?if "condition"?)
    spLineIn.seek(5);
    sLineOut.append("</xsl:text><xsl:choose><xsl:when test=");
    sLineOut.append(ifcondition(spLineIn));
    sLineOut.append("><xsl:text>");
    spLineIn.fromEnd().seek(2);
  }
  
  
  private String ifcondition(StringPart spLineIn)
  {
    String sTest;
    if(spLineIn.getCurrentChar()=='\"')
    { spLineIn.lentoQuotionEnd('\"', Integer.MAX_VALUE);
      sTest = spLineIn.getCurrentPart();
    }
    else 
    { spLineIn.lento("?)", StringPart.seekNormal);
      sTest = "\"" + spLineIn.getCurrentPart() + "\""; 
    }
    return sTest;   	
  }
  
  private void setElif(StringBuffer sLineOut, StringPart spLineIn)
  { //input: (?elif "condition"?)
    spLineIn.seek(7);
    sLineOut.append("</xsl:text></xsl:when><xsl:when test=");
    sLineOut.append(ifcondition(spLineIn));
    sLineOut.append("><xsl:text>");
    spLineIn.fromEnd().seek(2);
  }
  
  
  private void setElse(StringBuffer sLineOut, StringPart spLineIn)
  { //input: (?else?)
    spLineIn.seek(8);
    sLineOut.append("</xsl:text></xsl:when><xsl:otherwise><xsl:text>");
  }
  
  
  private void setIfNext(StringBuffer sLineOut, StringPart spLineIn)
  { //input: (?ifNext?)
    spLineIn.seek(10);
    sLineOut.append("</xsl:text><xsl:choose><xsl:when test=\"last() > position()\"><xsl:text>");
  }
  
  
  private void setIfEnd(StringBuffer sLineOut, StringPart spLineIn)
  { //input: (?/if?)
    spLineIn.seek(7);
    sLineOut.append("</xsl:text></xsl:when></xsl:choose><xsl:text>");
  }
  
  
  private void setElseEnd(StringBuffer sLineOut, StringPart spLineIn)
  { //input: (?/else?)
    spLineIn.seek(9);
    sLineOut.append("</xsl:text></xsl:otherwise></xsl:choose><xsl:text>");
  }
  
  
  
  private void setAliasOld(StringPart spLineIn)
  { //input: <=key=value>
    spLineIn.seek(2).lento('=');
    String sKey = spLineIn.getCurrentPart();
    spLineIn.fromEnd().seek(1).lento('>');
    aliases.put(sKey, spLineIn.getCurrentPart());
    spLineIn.fromEnd().seek(1);
  }
  
  private void setValueOfOld(StringBuffer sLineOut, StringPart spLineIn)
  { //input: <@value> or <@=alias>
    spLineIn.seek(2).lento('>');
    String sValue;
    if(spLineIn.getCurrentChar() == '=')
    { String sKey = spLineIn.seek(1).getCurrentPart();
      sValue = (String)(aliases.get(sKey));
    }
    else
    { sValue = spLineIn.getCurrentPart();
    }
    sLineOut.append("</xsl:text><xsl:value-of select=\"");
    sLineOut.append(sValue);
    sLineOut.append("\" /><xsl:text>");
    spLineIn.fromEnd().seek(1);
  }
  
  
  private void setCallTemplateOld(StringBuffer sLineOut, StringPart spLineIn)
  { //input: <:name[:"select"](pname=par, pname=par)> 
    //par may be 'text' or @alias or $variable or *xpath concated bei " + "
    //example <:element/*:callName(par=@alias + 'text' + *path, par2='text')>
    spLineIn.seek(2).lentoAnyChar(":(>");
    String sCall = spLineIn.getCurrentPart();
    char cSep = spLineIn.fromEnd().getCurrentChar();
    String sSelect;
    
    if(cSep == ':')
    { spLineIn.seek(1).lentoQuotionEnd('\"', Integer.MAX_VALUE);
      sSelect = spLineIn.getCurrentPart();
      sLineOut.append("</xsl:text><xsl:for-each select=");
      sLineOut.append(sSelect);
      sLineOut.append(">");
      cSep = spLineIn.fromEnd().getCurrentChar();
    }
    else{ sSelect = null; }
    
    sLineOut.append("<xsl:call-template name=\"");
    sLineOut.append(sCall);
    if(sCall.equals("ast"))
    	stop();
            
    if(cSep == '(')
    { sLineOut.append("\">");
        
      spLineIn.seek(1);
      while(spLineIn.getCurrentChar() != ')')
      {
        spLineIn.lentoAnyChar("=");
        String sParamName = spLineIn.getCurrentPart();
        spLineIn.fromEnd().seek(1).lentoAnyChar(",)");
        String sParamValue = spLineIn.getCurrentPart();

        sLineOut.append("<xsl:with-param name=\"");
        sLineOut.append(sParamName);
        sLineOut.append("\" ");
        if(sParamValue.indexOf(" + ") >= 0)
        { setParamValueMultiContent(sLineOut, sParamValue);
        }
        else
        { setParamValueSelect(sLineOut, sParamValue);        
        }
        if(spLineIn.fromEnd().getCurrentChar() == ',')
        { spLineIn.seek(2);  // the user has to write ", nextname"
        }
      }      
      sLineOut.append("</xsl:call-template>");
    }
    else //no (), without param
    { sLineOut.append("\" />");
    }
    if(sSelect != null)
    { sLineOut.append("</xsl:for-each>");
    }
    sLineOut.append("<xsl:text>");
    spLineIn.seek(">", StringPart.seekNormal).seek(1);
  }
  /*
  private void setParamValueSelectOld(StringBuffer sLineOut, String sParamValue)
  { if(sParamValue.startsWith("="))
    { String sKey = sParamValue.substring(1);
      sParamValue = "\"" + (String)(aliases.get(sKey)) + "\"";
    }
    sLineOut.append(" select=");
    sLineOut.append(sParamValue);
    sLineOut.append(" />");
  }
  */
  /*
  private void setParamValueMultiContentOld(StringBuffer sLineOut, String sParamValue)
  { if(sParamValue.startsWith("*"))
    { sParamValue = sParamValue.substring(1);
    }
    sLineOut.append("\" select=\"");
    sLineOut.append(sParamValue);
    sLineOut.append("\" />");
  }
  */
  
  private void setIfOld(StringBuffer sLineOut, StringPart spLineIn)
  { //input: <?if "condition"?value>
    spLineIn.seek(5).lentoQuotionEnd('\"', Integer.MAX_VALUE);
    String sTest = spLineIn.getCurrentPart();
    sLineOut.append("</xsl:text><xsl:choose><xsl:when test=");
    sLineOut.append(sTest);
    sLineOut.append("><xsl:text>");
    spLineIn.fromEnd().seek(1);
  }
  
  
  private void setElifOld(StringBuffer sLineOut, StringPart spLineIn)
  { //input: <?else "condition"?value>
    spLineIn.seek(7).lentoQuotionEnd('\"', Integer.MAX_VALUE);
    String sTest = spLineIn.getCurrentPart();
    sLineOut.append("</xsl:text></xsl:choose><xsl:when test=");
    sLineOut.append(sTest);
    sLineOut.append("><xsl:text>");
    spLineIn.fromEnd().seek(1);
  }
  
  
  private void setElseOld(StringBuffer sLineOut, StringPart spLineIn)
  { //input: </?else>
    spLineIn.seek(8);
    sLineOut.append("</xsl:text></xsl:when><xsl:otherwise><xsl:text>");
  }
  
  
  private void setIfEndOld(StringBuffer sLineOut, StringPart spLineIn)
  { //input: </?if>
    spLineIn.seek(6);
    sLineOut.append("</xsl:text></xsl:when></xsl:choose><xsl:text>");
  }
  
  
  private void setElseEndOld(StringBuffer sLineOut, StringPart spLineIn)
  { //input: </?else>
    spLineIn.seek(8);
    sLineOut.append("</xsl:text></xsl:otherwise></xsl:choose><xsl:text>");
  }
  
  
  
  
  /*---------------------------------------------------------------------------------------------*/
  /** main started from java*/
  public static void main(String [] args)
  { Xsltpre main = new Xsltpre();
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
      main.fIn = new File(main.sFileIn);
      main.fOut = new File(main.sFileOut);

      try{ main.execute(); }
      catch(Exception exception)
      { //catch the last level of error. No error is reported direct on command line!
        mainCmdLine.report("Uncatched Exception on main level:", exception);
        mainCmdLine.setExitErrorLevel(MainCmd_ifc.exitWithErrors);
      }
    }
    mainCmdLine.exit();
  }

  void stop(){}
  

  /**Inner class for invocation from command line, created only in the static main routine.*/
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
      super.addAboutInfo("Text preprocessor for Xslt");
      super.addAboutInfo("made by JcSchorrig, 2007-01-18");
      super.addHelpInfo("Converts Xsl with macros in a standard xsl.");
      super.addHelpInfo("param: -iINPUT -oOUTPUT [-eEncoding] [-always]");
      super.addHelpInfo("-iINPUT    inputfilepath of a XSL file with macros.");
      super.addHelpInfo("-iOUTPUT   outputfilepath of the pure XSL file.");
      super.addHelpInfo("-eEncoding encoding of the output, default is ISO-8859-1.");
      super.addHelpInfo("-always generate always, otherwise not if the output is newer.");
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
  
      if(arg.startsWith("-i:"))      sFileIn   = getArgument(3);
      else if(arg.startsWith("-i"))      sFileIn   = getArgument(2);
      else if(arg.startsWith("-o:")) sFileOut  = getArgument(3);
      else if(arg.startsWith("-o")) sFileOut  = getArgument(2);
      else if(arg.startsWith("-e:")) sEncoding = getArgument(3);
      else if(arg.startsWith("-e")) sEncoding = getArgument(2);
      else if(arg.startsWith("-always")) bGenerateOnlyifNecessary = false;
      else bOk=false;
  
      return bOk;
    }
  
    /*---------------------------------------------------------------------------------------------*/
    /**Checks the cmdline arguments relation together.
       If there is an inconsistents, a message should be written. It may be also a warning.
       :TODO: the user only should determine the specific checks, this is a sample.
       @return true if successfull, false if failed.
    */
    protected boolean checkArguments()
    { boolean bOk = true;
  
      if(sFileIn == null)            { bOk = false; writeError("ERROR argument -i is obligat."); }
      else if(sFileIn.length()==0)   { bOk = false; writeError("ERROR argument -i without content.");}
  
      if(sFileOut == null)           { writeWarning("argument -o no outputfile is given, use default"); sFileOut = "out.txt";}
      else if(sFileOut.length()==0)  { bOk = false; writeError("argument -o without content"); }
  
      if(!bOk) setExitErrorLevel(exitWithArgumentError);
  
      return bOk;
  
    }
  }  
}
