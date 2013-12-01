package org.vishia.sclConversions;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


import org.vishia.byteData.PositionElementInStruct;
import org.vishia.mainCmd.MainCmd;
import org.vishia.mainCmd.MainCmd_ifc;
import org.vishia.mainCmd.Report;
import org.vishia.util.Assert;
import org.vishia.util.FileSystem;
import org.vishia.util.ShortenString;
import org.vishia.cmd.ZGenExecuter;
import org.vishia.cmd.ZGenScript;
import org.vishia.zbnf.ZbnfJavaOutput;
import org.vishia.zgen.ZGen;


/**Converts some SCL source files to one source file which gathers all variables and one configuration file
 * for an operation-and-monitoring application. The conversion is controlled by a script.
 * @author Hartmut Schorrig
 *
 */
public class SCLstruct2Lists
{
  
  /**Version, history and license.
   * <ul>
   * <li>2013-09-07 Hartmut chg: Adaption to Jbatch
   * <li>2013-02-00 Hartmut chg: Script controlled
   * <li>2011-00-00 Hartmut created: first version non-script controlled.
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
   */
  public final static int version = 20130908; 

  
  

  /** Aggregation to the Console implementation class.*/
  final MainCmd_ifc console;


  /**This class holds the got arguments from command line. 
   * It is possible that some more operations with arguments as a simple get process.
   * The arguments may got also from configuration files named in cmd line arguments or other.
   * The user of arguments should know (reference) only this instance, not all other things arround MainCmd,
   * it it access to arguments. 
   */
  private static class Args
  {
    /**Cmdline-argument, set on -srcpath= option. Inputfile for configuration translating. */
    String sSrcPath = null;
  
    /**Cmdline-argument, set on -cfg= option. Inputfile for configuration translating. */
    String sFileCfg = null;
  
    /**Cmdline-argument, set on -wincc= option. Outputfile for wincc-variable list. */
    String sFileWinccVarCsv = null;

    /**Cmdline-argument, set on -scl= option. Outputfile for Scl-source which gathers all variables. */
    String sFileScl = null;

    /**Cmdline-argument, set on -sclCtrl= option. Control file for SCL-source which gathers all variables. */
    String sFileSclCtrl = null;

    /**Cmdline-argument, set on -header= option. Outputfile for C-header-source which gathers all variables. */
    //String sFileHeader = null;

    /**Cmdline-argument, set on -oam= option. Outputfile for a config-file which describes the oam-Variable. */
    String sFileOamVariables = null;

    /**Cmdline-argument, set on -oamCtrl= option. Control file for Outputfile for a config-file which describes the oam-Variable. */
    String sFileOamVariablesCtrl = null;

    /**Cmdline-argument, set on -db= option. Outputfile for a config-file which describes the oam-Variable. */
    String sFileDbVariables = null;

    /**Cmdline-argument, set on -z: option. Zbnf-path for syntax files. */
    String sPathZbnf = null;
    
    /**True than generate for all found variables. */
    boolean bAllVariable = false;
  }

  final Args args;

  /**Data to generate either the wincc variable import or the structure of a record for all oam-data, see {@link Args#sFileScl}. */
  GenerateVariableImport generateVariableImport;
  
  /**This index assigns the S7-Types to the appropriate types for header-files, WinCC etc. */
  private final Map<String, TypeConversion> indexTypes = new TreeMap<String, TypeConversion>();
  
  
  ShortenString shortenScl24Names = new ShortenString(24);
  
  
  /*---------------------------------------------------------------------------------------------*/
  /** main started from java*/
  public static void main(String [] args)
  { SCLstruct2Lists.Args cmdlineArgs = new SCLstruct2Lists.Args();     //holds the command line arguments.
    CmdLine mainCmdLine = new CmdLine(args, cmdlineArgs); //the instance to parse arguments and others.
    boolean bOk = true;
    try{ mainCmdLine.parseArguments(); }
    catch(Exception exception)
    { mainCmdLine.setExitErrorLevel(MainCmd_ifc.exitWithArgumentError);
      bOk = false;
    }
    if(bOk)
    { /**Now instantiate the main class. 
       * It is possible to create some aggregates (final references) first outside depends on args.
       * Therefore the main class is created yet here.
       */
      SCLstruct2Lists main = new SCLstruct2Lists(mainCmdLine, cmdlineArgs);
      /** The execution class knows the Main class in form of the MainCmd super class
          to hold the contact to the command line execution.
      */
      try
      { main.execute(cmdlineArgs); 
      }
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
   * This class also implements the {@link MainCmd_ifc}, for some cmd-line-respective things
   * like error level, output of texts, exit handling.  
   */
  static class CmdLine extends MainCmd
  {
  
  
    final Args cmdlineArgs;
    
    /*---------------------------------------------------------------------------------------------*/
    /** Constructor of the main class.
        The command line arguments are parsed here. After them the execute class is created as composition of SampleCmdLine.
    */
    private CmdLine(String[] argsInput, Args cmdlineArgs)
    { super(argsInput);
      this.cmdlineArgs = cmdlineArgs;
      //:TODO: user, add your help info!
      //super.addHelpInfo(getAboutInfo());
      super.addAboutInfo("Conversion SCL-Files (Simatic) for oam");
      super.addAboutInfo("made by Hartmut Schorrig, 2010-08-01, 2010-10-15");
      super.addHelpInfo("made by Hartmut Schorrig, 2010-08-01, 2010-10-15");
      super.addHelpInfo("-srcpath=PATH Path base to sources, additional to config file. If not given,");
      super.addHelpInfo("              then the source path is the directory of the config file.");
      super.addHelpInfo("-z:ZBNFPATH syntaxpath, should contain winCCVarCfg.zbnf and sclStruct.zbnf.");
      super.addHelpInfo("param: TODO");
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
    @Override
    protected boolean testArgument(String arg, int nArg)
    { boolean bOk = true;  //set to false if the argc is not passed
  
      if(arg.startsWith("-cfg="))      cmdlineArgs.sFileCfg   = getArgument(5);
      else if(arg.startsWith("-srcpath=")) cmdlineArgs.sSrcPath  = getArgument(9);
      else if(arg.startsWith("-wincc=")) cmdlineArgs.sFileWinccVarCsv  = getArgument(7);
      else if(arg.startsWith("-scl=")) cmdlineArgs.sFileScl  = getArgument(5);
      else if(arg.startsWith("-sclCtrl=")) cmdlineArgs.sFileSclCtrl  = getArgument(9);
      //else if(arg.startsWith("-header=")) cmdlineArgs.sFileHeader  = getArgument(8);
      else if(arg.startsWith("-oam=")) cmdlineArgs.sFileOamVariables  = getArgument(5);
      else if(arg.startsWith("-oamCtrl=")) cmdlineArgs.sFileOamVariablesCtrl  = getArgument(9);
      else if(arg.startsWith("-db=")) cmdlineArgs.sFileDbVariables  = getArgument(4);
      else if(arg.startsWith("-z=")) cmdlineArgs.sPathZbnf  = getArgument(3);
      else if(arg.startsWith("-all")) cmdlineArgs.bAllVariable = true;
      else bOk=false;
  
      return bOk;
    }
  
    /** Invoked from parseArguments if no argument is given. In the default implementation a help info is written
     * and the application is terminated. The user should overwrite this method if the call without comand line arguments
     * is meaningfull.
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
       If there is an inconsistents, a message should be written. It may be also a warning.
       :TODO: the user only should determine the specific checks, this is a sample.
       @return true if successfull, false if failed.
    */
    @Override
    protected boolean checkArguments()
    { boolean bOk = true;
  
      if(cmdlineArgs.sFileCfg == null)            { bOk = false; writeError("ERROR argument -i is obligat."); }
      else if(cmdlineArgs.sFileCfg.length()==0)   { bOk = false; writeError("ERROR argument -i without content.");}
  
      if(!bOk) setExitErrorLevel(exitWithArgumentError);
    
      return bOk;
    
    }
  }//class CmdLine
  
  
  /**Variable which is used for oam data transfer. */
  static class OamVariable
  {
    /**The name for SCL variable name in OamVariables.scl. It is a shorten but unique string from {@link #sVariablePath} 
     * and block {@link ZbnfVariablenBlock#winCCfolder}, {@link ZbnfVariablenBlock#prefix} and {@link ZbnfVariablenBlock#suffix}. */
    final String name24;  
    
    /**The variable itself. */
    final Variable variable;   
    
    
    /**Path to the variable inside the DB which is named in the {@link #block}. 
     * It contains . if the variable is inside a sub structure. */
    final String structPath;
   
    /**Datapath used for unique access in Oam. */
    final String dataPath;
    
    /**Contains prefix etc. */
    final ZbnfVariablenBlock block;
    
    /**The position of this variable in the oamVariables.dbf respectively in the dataStream of all variables. */
    final int posByte;
    
    /**If it is a boolean, the mask refers the bit. Values 0x01...0x80. If it is not a boolean, the mask is 0. */
    final int maskBitInByte;
    
    OamVariable(String name24, Variable variable, String structPath, String sDataPath, int posByte, int maskBitInByte, ZbnfVariablenBlock block ){
      this.name24 = name24;
      this.variable = variable;
      this.structPath = structPath;
      this.dataPath = sDataPath;
      this.block = block;
      this.posByte = posByte;
      this.maskBitInByte = maskBitInByte;
    }
  }
  
  /**The list for all oam variables to synthesize. */
  List<OamVariable> oamVariables = new LinkedList<OamVariable>();
  
  static class PositionOfVariable extends PositionElementInStruct
  {
    /**Position of variable in the all-Variable-byte[] red from a file-image of DB or from IP-Telg. */
    int posVariable = 0;
    /**Bit mask for boolean variables, 0..8000 for Oam-Access */
    int bitMask = 0x1;
    
    
    
  }
  
  
  
  
  /**This class gets and stores the results from a parsed ZBNF-component <code>constant</code>.
   * 
   */
  public static class ZbnfVariable ///
  {
    /**From ZBNF: <$?name>. */
    public List<String> name;
    
    
    private List<ZbnfVariable> structZbnfVariables;
    
    /**From ZBNF: <$?type>. */
    public String type;
    
    /**From ZBNF: <#?stringlen>. */
    public int stringlen;
    
    /**From ZBNF: <#?@arrayStartIx>. */
    public int arrayStartIx;
    
    /**From ZBNF: <#?@arrayEndIx>. */
    public int arrayEndIx;
    
    /**From ZBNF: <""?@comment>. */
    public String assignment= "";  //it is optional.
    
    public String otherRepresentation = null;
    
    public String otherType = null;
    
    /**from ZBNF: set if '+' is written after '(WinCC:,'. 
     * It means, both, otherRepresentatio and the variable itself should be generated. */
    public boolean additional;
    
    /**from ZBNF: set if '(winCC' is found. Only if it is set, a variable should be generated. */
    private boolean winCC;
    
    /**from ZBNF: set if '(winCC' is found. Only if it is set, a variable should be generated. */
    public void set_winCC()
    { winCC = true;
    }
    
    /**from ZBNF: set if 'oam' is found. Only if it is set, a variable should be generated. */
    private boolean oam;
    
    /**from ZBNF: set if '<?oam>' is found. Only if it is set, a variable should be generated. */
    public void set_oam()
    { oam = true;
    }
    
    //public boolean noVariable;
    
    //public boolean union;
    
    public String comment, comment2;
    
    /**From Zbnf parse result, see {@link #add_structVariable(ZbnfVariable)}. */
    public ZbnfVariable new_structVariable(){ return new ZbnfVariable(); }
    
    /**From Zbnf parse result: <pre>
     * STRUCT { <variable?structVariable> } END_STRUCT
     * </pre>
     */
    public void add_structVariable(ZbnfVariable val){ 
      if(structZbnfVariables == null){ structZbnfVariables = new LinkedList<ZbnfVariable>(); }
      structZbnfVariables.add(val);
      /*
      for(String name: val.name){
        Variable variable = createVariable(name, val);
        structVariables.add(variable);
      }
      */
    }
    
    @Override public String toString(){ return name.toString(); }
    
  }
  
  /**This class saves parse results from the config file for each line. 
   * Syntax:
   * <pre>
variablenBlock::=
  [variable |struct <$?structName>] : [<$?winCCfolder>/][<$?prefix>]*[<$?suffix>]  
{ @ DB<#?nrDB>\.DBB<#?nrByte> 
| DBname = <*\ ,;?dbName>
| Type [:|=] <*\ ,;?pathUDT>
? , } ;.
   * </pre>
   */
  public static class ZbnfVariablenBlock
  {
    /**If set, than struct <$?structName> is read. It is a struct definition without a instantiated data block. 
     * It may be a UDT-definition, but also a function block which is used inside another function blocks. 
     * If this element is null, it is an instantiated data block, see {@link #nrDB}. */
    public String structName = null;
    
    /**From ZBNF [<$?winCCfolder>/]. It should be null if not present. 
     * For wincc variable import one folder can be used. For Inspector-GUI it is a part of the name. 
     */
    public String winCCfolder = null;

    /**From ZBNF: [<$?prefix>]. It should be empty if not present. Prefix for name for the oam-Variable.
     * The whole variable name is build with the variable name inside the function block, with this prefix
     * and the {@link #suffix}.
     * */
    public String prefix = "";
    
    /**From ZBNF: [<$?suffix>]. It should be empty if not present. */
    public String suffix = "";
    
    /**From ZBNF: Type: <*;?pathUDT> . always present. This is the scl-file.*/
    public String pathUDT;

    /**From ZBNF: Type: <* ,;?dbName> This string is used for access to the data in the generated oamVariables.scl FB. */
    public String dbName;
    
    /**From ZBNF: @ DB<#?nrDB>\.DBB<#?nrByte>. Data block number and byte number. */
    public int nrDB, nrByte;
    
    //private int lengthPreSuffix;
    
    @Override public String toString(){ return pathUDT + "/" + dbName; }
  }
  
  
  /**Parse result of an SCL file. Only the variable definitions are stored.
   * @author Hartmut Schorrig
   *
   */
  public static class ZbnfResultFileSCL
  {
    private final List<Variable> variable = new LinkedList<Variable>();
    
    public ZbnfVariable new_variable()
    { return new ZbnfVariable();
    }
    
    public void add_variable(ZbnfVariable value)
    { 
      for(String name: value.name){
        Variable var = createVariable(name, value);
        variable.add(var);
      }
    }
    
    public void set_comment(String text){} //ignore it.
  }
  
  
  public static class ZbnfResultStructVariable
  {
    private final List<ZbnfVariable> variable = new LinkedList<ZbnfVariable>();
    
    public ZbnfVariable new_variable()
    { return new ZbnfVariable();
    }
    
    public void add_variable(ZbnfVariable value)
    { 
      variable.add(value);
    }
    
  }
  
  
  public static class ZbnfResultDataCfg
  {
    private final List<ZbnfVariablenBlock> variablenBlock = new LinkedList<ZbnfVariablenBlock>();
    
    
    public ZbnfVariablenBlock new_variablenBlock()
    { return new ZbnfVariablenBlock();
    }
    
    public void add_variablenBlock(ZbnfVariablenBlock value)
    { 
      variablenBlock.add(value);
    }
    
  }
  

  
  /**A variable. An instance is build in {@link SCLstruct2Lists#createVariable(String, ZbnfVariable)}.
   * <br><br>
   * Tiny object model diagramm, see {@link org.vishia.util.Docu_UML_simpleNotation}:
   * <pre>
   * Variable
   *    |
   *    |-------zbnfVariable------->ZbnfVariable
   *    |                               |
   * </pre>
   */
  static class Variable
  { String sName;
    int pos;
    int boolMask;
    TypeConversion type;
    
    int nrofBytes;
    
    int nrofElements;
    
    
    /**If this variable is a struct variable, its members: */
    List<Variable> structVariables;
    

    
    /**Aggregation to the parsers result which contains information about array indices etc. */
    final ZbnfVariable zbnfVariable;
    
    Variable(ZbnfVariable zbnfVariable){ this.zbnfVariable = zbnfVariable; }
    
    
    @Override public String toString(){ return sName + "@" + pos + ":" + (type==null? "?" : type.typeCharS7); }
    
  }


  
  
  /**This inner class encapsulates the conversion of the parsed Data 
   * to save a data structure (UDT), which may be included in any other data structure.
   */
  private static class SaveDataStruct extends TypeConversion
  {
    final PositionOfVariable position = new PositionOfVariable();
    
    List<Variable> variables = new LinkedList<Variable>();
    
    public SaveDataStruct(String name) 
    { super(0, 'O', 'O', name, name, name);
      this.position.posVariable = 0;
      this.position.bitMask = 0x1;
    }


    void saveVariable(Variable variable) //, VariablenBlock block)
    {
      //final String sVariable = (block.winCCfolder !=null ? block.winCCfolder + "/" : "") 
      //                         + block.prefix + zbnfVariable.name + block.suffix;
        variables.add(variable);
    }
    
    
    
  }//class SaveDataStruct
  
  
  
  
  static Variable createVariable(String name, ZbnfVariable zbnfVariable) //, VariablenBlock block)
  {
    //final String sVariable = (block.winCCfolder !=null ? block.winCCfolder + "/" : "") 
    //                         + block.prefix + zbnfVariable.name + block.suffix;
    Variable variable = new Variable(zbnfVariable);
    variable.sName = name;
    int nrofElements;
    if(zbnfVariable.arrayStartIx >=0){
      nrofElements = zbnfVariable.arrayEndIx- zbnfVariable.arrayStartIx +1;
    } else {
      nrofElements = 1;
    }
    variable.nrofElements = nrofElements;
    if(zbnfVariable.structZbnfVariables !=null){
      variable.structVariables = new LinkedList<Variable>();  //copy the StructZbnfVariable to this.
      //Note that a ZbnfVariable can represent more as one variable writing name1, name2: <TYPE>
      for(ZbnfVariable structZbnfVariable : zbnfVariable.structZbnfVariables){ //loop over all struct variables
        for(String elementName: structZbnfVariable.name){ 
          //loop over all variables with same type: "name1, name2: TYPE", usual only one time.
          String structVariableName = name + "." + elementName; //name of the struct variable is "structname.elementname"
          Variable structVariable = createVariable(structVariableName, structZbnfVariable);
          variable.structVariables.add(structVariable);
        }
      }
    }
    return variable;
  }


  
  static void completeVariableWithType(Variable variable, TypeConversion type, PositionOfVariable position) //, VariablenBlock block)
  {
    if(type.typeCharJava == 'Z'){
      variable.boolMask = position.bitMask;
      variable.nrofBytes = 0;
      variable.pos = position.posVariable;
      while(--variable.nrofElements >=0){
        position.bitMask <<=1;
        if(position.bitMask >0x80){
          position.bitMask = 0x1;  //for the next boolean.
          position.posVariable += 1;
        }
      }
    } else {
      if(position.bitMask != 0x1){
        position.bitMask = 0x1;  //start next boolean with it.
        position.posVariable +=1; //rest of BOOL unused.
      }
      if(type.nrofBytes >1 && (position.posVariable & 1) !=0){
        //even address, use odd
        position.posVariable +=1;
      }
      variable.pos = position.posVariable;
      if(type.typeCharJava == 's'){
        position.posVariable += (variable.zbnfVariable.stringlen +2)* variable.nrofElements;
        variable.nrofBytes = variable.zbnfVariable.stringlen +2;
      } else {
        position.posVariable += type.nrofBytes * variable.nrofElements;
        variable.nrofBytes = type.nrofBytes;
      }  
    }
  }
  
  
  
  /**This inner class encapsulates the conversion of the parsed Data 
   * to generate the configuration-file for access to the data in a byte[]-array.
   * The byte[]-array may be red from a file or IP-Telegram, which maps one ore more DB-data.
   */
  private static class GenerateDbConfigAccess
  {
    /**Position of variable in the all-Variable-byte[] red from a file-image of DB or from IP-Telg. */
    int posVariable = 0;
    /**Bit mask for boolean variables, 0..8000 for Oam-Access */
    int bitMask = 0x1;
    /**Buffer to prepare the informations about oam variable, which are accessible in a byte[]-Buffer
     * red from a file or from IP-telegram-Communication.
     */
    final StringBuilder dbList;
   
    final String sFile;
    
    
    
    public GenerateDbConfigAccess(String sFile) 
    {
      this.posVariable = 0;
      this.bitMask = 0x1;
      this.dbList = new StringBuilder(8000);
      this.sFile = sFile;
    }

    /**Generates the config file for all variable. 
     * The config-file is able to use to access to the data of a DB, which are stored in a file.
     * More as one DB can be stored in file.
     * @param name Name of the variable in the DB
     * @param type conversion information of the type of the variable
     * @param block configuration of translation for this FB, DB or UDT. 
     *        Here pre- and suffixes and the position of the data in the file are found.
     */
    void generateDbVariableLine(String name, TypeConversion type, ZbnfVariablenBlock block)
    {
      final String sVariable = block.prefix + name + block.suffix;
      if(type.typeCharJava == 'Z'){
        String sMask = Integer.toHexString(bitMask);
        dbList.append("\n").append(block.winCCfolder).append('/').append(sVariable).append(": ").append(type.typeCharJava).append(" @")
          .append(posVariable).append(".0x").append(Integer.toHexString(bitMask)).append(";//DB").append(block.nrDB);
        bitMask <<=1;
        if(bitMask >0x80){
          bitMask = 0x1;  //for the next boolean.
          posVariable += 1;
        }
      } else {
        if(bitMask != 0x1){
          bitMask = 0x1;  //start next boolean with it.
          posVariable +=1; //rest of BOOL unused.
        }
        if(type.nrofBytes >1 && (posVariable & 1) !=0){
          //even address, use odd
          posVariable +=1;
        }
        dbList.append("\n").append(block.winCCfolder).append('/').append(sVariable).append(": ").append(type.typeCharJava).append(" @")
            .append(posVariable).append(";  //DB").append(block.nrDB);
        posVariable += type.nrofBytes;
      }
      final String srcDb;
      if(block.dbName != null){
        srcDb = block.dbName;
      } else {
        srcDb = "DB" + block.nrDB;
      }
    }

    /**Generates the config file for all variable. 
     * The config-file is able to use to access to the data of a DB, which are stored in a file.
     * More as one DB can be stored in file.
     * @param identArgJbat Name of the variable in the DB
     * @param type conversion information of the type of the variable
     * @param block configuration of translation for this FB, DB or UDT. 
     *        Here pre- and suffixes and the position of the data in the file are found.
     */
    void generateDbVariableLine(Variable src, String sStructName, ZbnfVariablenBlock block, int posBase)
    {
      final String sVariable = block.prefix + sStructName + src.sName + block.suffix;
      final String srcDb;
      if(block.dbName != null){
        srcDb = block.dbName;
      } else {
        srcDb = "DB" + block.nrDB;
      }
      if(src.type.typeCharJava == 'Z'){
        String sMask = Integer.toHexString(bitMask);
        dbList.append("\n").append(block.winCCfolder).append('/').append(sVariable).append(": ")
          .append(src.type.typeCharJava).append(" @")
          .append(src.pos+posBase).append(".0x").append(Integer.toHexString(src.boolMask)).append(";//DB").append(block.nrDB);
      } else {
        dbList.append("\n").append(block.winCCfolder).append('/').append(sVariable).append(": ").append(src.type.typeCharJava)
          .append(" @").append(src.pos+posBase).append(" +").append(src.nrofBytes);
        if(src.nrofElements >1){
          dbList.append(" *").append(src.nrofElements);
        }
        dbList.append(";  //").append(srcDb);
      }
    }

    void writeFile() throws IOException
    {
      Writer sclOut = new FileWriter(sFile);
      sclOut.write("==AllVariableFromDB==");
      sclOut.write(dbList.toString());
      sclOut.close();
    }
    
    
  }//class GenerateDbConfigAccess
  
  
  /**Saves all struct <$?name>, which may be included in other data definitions. */
  Map<String, SaveDataStruct> allDataStruct = new TreeMap<String, SaveDataStruct>();
  
  /**Set with the working class if the argument -db is given and this output-file should be generated.
   */
  GenerateDbConfigAccess genDbConfig;
  
  
  SCLstruct2Lists(MainCmd_ifc console, Args args)
  {
    this.console = console;
    this.args = args;
  }
  
  
  /**Main execution routine.
   * @param args The args, typical got from the command line invocation.
   * @throws IOException
   * @throws IllegalAccessException 
   */
  void execute(Args args) throws IOException, IllegalAccessException
  {
    boolean bOk;
    ZbnfResultDataCfg configData;
    if(args.sFileCfg !=null){
      configData = parseAndStoreCfg(args);    //parse the -cfg=config_file
    } else {
      configData = null;
    }
    if(configData != null){
      
      final String sFileVariableImport;
      final BufferedWriter output;
      if(args.sFileWinccVarCsv !=null){
        File fVariableImport = new File(args.sFileWinccVarCsv);
        Writer output1 = new FileWriter(fVariableImport);
        output = new BufferedWriter(output1);
        sFileVariableImport = fVariableImport.getName(); 
      } else {
        sFileVariableImport = null;
        output = null;
      }
      
      if(args.sFileDbVariables != null){
        genDbConfig = new GenerateDbConfigAccess(args.sFileDbVariables);
      }
      
      generateVariableImport = new GenerateVariableImport(sFileVariableImport, configData, output);
      generateVariableImport.generateOutFiles(args.sSrcPath);
      if(output !=null)
      { output.close();
      }
      if(genDbConfig != null){
        genDbConfig.writeFile();
      }
      console.writeInfoln("SUCCESS creation: " + sFileVariableImport); //fVariableImport.getAbsolutePath());
      console.writeInfoln("");
    }
  }
  
  
  
  /**This method reads the input VHDL-script, parses it with ZBNF, 
   * stores all results in the Java-class {@link ZbnfResultData} 
   */
  private ZbnfResultDataCfg parseAndStoreCfg(Args args) throws IOException
  {
    /**The instance to store the data of parsing result is created locally and private visible: */
    ZbnfResultDataCfg zbnfResultData = new ZbnfResultDataCfg();
    /**This call processes the whole parsing and storing action: */
    File fileIn = new File(args.sFileCfg);
    File fileSyntax = new File(args.sPathZbnf + "/winCCVarCfg.zbnf");
    String sError = ZbnfJavaOutput.parseFileAndFillJavaObject(zbnfResultData.getClass(), zbnfResultData, fileIn, fileSyntax, console, 1200);
    if(sError != null)
    { /**there is any problem while parsing, report it: */
      console.writeError("ERROR Parsing file: " + fileIn.getAbsolutePath() + "\n" + sError);
      return null;
    }
    else
    { console.writeInfoln("SUCCESS parsed: " + fileIn.getCanonicalPath());
      return zbnfResultData;
    }
  
  }
  
  private static class TypeConversion
  { int nrofBytes;
    /**The char to designate the DB-Byte/word B, W, D*/
    final char typeCharS7;
    /**The char to designate the type for the java-GUI B, I, F*/
    final char typeCharJava;
    /**The type in S7: BYTE, BOOL, WORD, REAL etc. */
    final String s7Type;
    /**The type in Wincc: */
    final String winccType;
    /**The type in C: int8, int16, float usw. */
    final String cType;
    private TypeConversion(int nrofBytes, char typeCharS7, char typeCharJava, String s7Type, String cType, String winccType) {
      super();
      this.nrofBytes = nrofBytes;
      this.typeCharS7 = typeCharS7;
      this.typeCharJava = typeCharJava;
      this.s7Type = s7Type;
      this.cType = cType;
      this.winccType = winccType;
    }
  
  }
  
  /**This inner class encapsulates the conversion of the parsed Data 
   * to generate the files for WinCC-Variable-import and for java-Access to bytes.
   */
  private class GenerateVariableImport
  {

    /**Bit mask for boolean variables, 0..8000 for Oam-Access */
    //int bitMask = 0x1;
    
    /**Position of variable in the oam-byte[]. */
    //int posOamVariable = 0;
    
    
    PositionOfVariable posOamVariable = new PositionOfVariable();
    
    //StringBuilder sclVariables = new StringBuilder(8000);
    //StringBuilder sclAssignment = new StringBuilder(8000);
    //StringBuilder oamList = new StringBuilder(8000);
    /**Buffer to prepare the informations about all variable, which are accessible in a byte[]-Buffer
     * red from a file or from IP-telegram-Communication.
     */
    //StringBuilder sclDbHeader = new StringBuilder(8000);
    StringBuilder sbOut = new StringBuilder(10000);
    
    final String sNameVariableImport; 
    
    /**The content of the config file. */
    final ZbnfResultDataCfg configData; 
    
    final GenerateOneSCLfile generateOneSclFile = new GenerateOneSCLfile();
    
    final Writer output;
    

    final static String sNewline = "\r\n";
    
    /**50 spaces to generate fix tab position. */
    final static String spaces = "                                                  ";
    
    /**
     * @param nameVariableImport
     * @param configData
     * @param output
     */
    public GenerateVariableImport(String nameVariableImport, ZbnfResultDataCfg configData, Writer output)
    {
      this.sNameVariableImport = nameVariableImport;
      this.configData = configData;
      this.output = output;
      indexTypes.put("BYTE",  new TypeConversion(1, 'B', 'B', "BYTE", "int8", "Byte"));
      indexTypes.put("INT",   new TypeConversion(2, 'W', 'S', "INT", "int16", "Int"));
      indexTypes.put("DINT",  new TypeConversion(4, 'D', 'I', "DINT", "int32", "Dint"));
      indexTypes.put("WORD",  new TypeConversion(2, 'W', 'S', "WORD", "uint16", "Int"));
      indexTypes.put("DWORD", new TypeConversion(4, 'D', 'I', "DWORD", "uint32", "Dint"));
      indexTypes.put("BOOL",  new TypeConversion(0, 'B', 'Z', "BOOL", "int8", "Bool"));
      indexTypes.put("REAL",  new TypeConversion(4, 'D', 'F', "REAL", "float", "Real"));
      indexTypes.put("STRING",  new TypeConversion(4, 'S', 's', "STRING", "string", "String"));
    }
    
    /**Generates the destination files from all input files. This routine is called 1 time. 
     * @reads this.{@link #configData}
     * @uses this
     * @writes this.output.*
     * @calls {@link #generateStructure()}
     * @return true if success
     * @throws IOException
     * @throws IllegalAccessException 
     */
    private boolean generateOutFiles(String sPathSrc) throws IOException, IllegalAccessException
    {
      ZGenExecuter textGen = new ZGenExecuter(console);
      
      if(output !=null){
        output.write("# WinCC flexible 2008 Variablen-Import\r\n");
        output.write("# Generated by SCLstruct2Lists, made by Hartmut Schorrig, Version 2010-03-26\r\n");
      }
      final String sPathUDTbase;
      File fileIn = new File(args.sFileCfg);
      File dirIn = FileSystem.getDirectory(fileIn);
      if(sPathSrc != null){
        sPathUDTbase = sPathSrc + "/";
      } else {
        sPathUDTbase = dirIn.getAbsolutePath() + "/";
      }
      
      for(ZbnfVariablenBlock block: configData.variablenBlock) {
        generateOneSclFile.generateOneSclFile(sPathUDTbase, block);
      }
      
      String sGenCtrlSclOamAssigment = 
        "<:file>"
      + "FUNCTION_BLOCK OamVariables\n"
      + "VAR\n"
      + "<:for:var:oamVariables>\n"
      + "  <:><*$var.name24>: <.>"
      + "  <:if:$var.variable.nrofElements ?gt 1>"  ///
      + "  <:>ARRAY[<*$var.variable.zbnfVariable.arrayStartIx>..<*$var.variable.zbnfVariable.arrayEndIx>] OF <.><.if>"
      + "  <:><*$var.variable.type.s7Type>;\n"
      + "<.>\n"
      + "<.for>"
      + "END_VAR\n"
      + "BEGIN\n"
      + "<:for:var:oamVariables>\n"
      + "  <:><*$var.name24> := <.>" 
      + "  <:if:$var.block.dbName><*$var.block.dbName><:else>DB<*$var.block.nrDB><.if>" 
      + "  <:>.<*$var.sDataPath><*$var.variable.sName>;\n"  ///
      + "<.>\n"
      + "<.for>"
      + "END_FUNCTION_BLOCK\n"
      + "<.file>"
      ;
      Writer sclOut;
      //ZGen.Args argsZ = new ZGen.Args();
      //ZGen zbatch = new ZGen(argsZ, console);
      ZGenScript genScript; // = new JbatchScript(textGen, console);
      if(args.sFileScl != null){
        sclOut = new FileWriter(args.sFileScl);
        try{
          if(args.sFileSclCtrl !=null){
            genScript = ZGen.translateAndSetGenCtrl(new File(args.sFileSclCtrl), new File(args.sFileSclCtrl + ".test.xml"), console);
          } else {
            genScript = ZGen.translateAndSetGenCtrl(sGenCtrlSclOamAssigment, console);
          }
        } catch(Exception exc){ throw new RuntimeException(exc); }
        textGen.setScriptVariable("scl", 'O', SCLstruct2Lists.this, true);
        textGen.execute(genScript, true, false, sclOut);
        
        /*
        sclOut.write("FUNCTION_BLOCK OamVariables");
        sclOut.write("\nVAR");
        sclOut.write(sclVariables.toString());
        sclOut.write("\nEND_VAR");
        sclOut.write("\nBEGIN");
        sclOut.write(sclAssignment.toString());
        sclOut.write("\nEND_FUNCTION_BLOCK");
        sclOut.write("\n");
        */
        sclOut.close();
      }
      if(args.sFileOamVariables != null){
        sclOut = new FileWriter(args.sFileOamVariables);
        try{
          genScript = ZGen.translateAndSetGenCtrl(new File(args.sFileOamVariablesCtrl), new File(args.sFileOamVariablesCtrl + ".test.xml"), console);
        } catch(Exception exc){ throw new RuntimeException(exc); }
        textGen.execute(genScript, true, false, sclOut);
        /*
        sclOut.write("==OamVariables==");
        sclOut.write(oamList.toString());
        */
        sclOut.close();
      }
      
      if(output != null){
        output.write(sbOut.toString());
      }
      return true;
    }
    
    
    
    
    
    
    
  }//class GenerateVariableImport

  
  
  class GenerateOneSCLfile
  {

    /**Result data of parsing the UDT-Files. */
    ZbnfResultFileSCL zbnfResultDataUDT; 
 
    final PositionOfVariable positionElementAll = new PositionOfVariable();
    
    GenerateOneSCLfile()
    {
      
    }
    
    
    /**This routine parses a SCL-File and returns the result.
     * @param sFileScl The file (absolute path)
     * @return The parsed content.
     * @throws IOException
     */
    ZbnfResultFileSCL parseSclFile(String sFileScl)
    throws IOException
    {
      /**The instance to store the data of parsing result is created locally and private visible: */
      ZbnfResultFileSCL zbnfResultData = new ZbnfResultFileSCL();
      /**This call processes the whole parsing and storing action: */
      File fileIn = new File(sFileScl);
      File fileSyntax = new File(args.sPathZbnf + "/sclStruct.zbnf");
      String sError = ZbnfJavaOutput.parseFileAndFillJavaObject(zbnfResultData.getClass(), zbnfResultData, fileIn, fileSyntax, console, 1200);
      if(sError != null)
      { /**there is any problem while parsing, report it: */
        console.writeError("ERROR Parsing file: " + fileIn.getAbsolutePath() + "\n" + sError);
        return null;
      }
      else
      { console.writeInfoln("SUCCESS parsed: " + fileIn.getCanonicalPath());
        return zbnfResultData;
      }
    
    }

    
    
    
    
    void generateOneSclFile(String sPathUDTbase, ZbnfVariablenBlock block) 
    throws IOException
    {
      //block.lengthPreSuffix = (block.prefix !=null ? block.prefix.length() : 0) + (block.suffix !=null ? block.suffix.length() : 0);
      zbnfResultDataUDT = parseSclFile(sPathUDTbase + block.pathUDT);
      if(zbnfResultDataUDT != null){
        final SaveDataStruct dataStruct;
        if(block.structName != null){
          dataStruct = new SaveDataStruct(block.structName);
          allDataStruct.put(block.structName, dataStruct);
          int nrofBytes = generateForVariables("", zbnfResultDataUDT.variable, dataStruct, block, dataStruct.position, 0);
          dataStruct.position.adjustNonBool(nrofBytes);
          dataStruct.nrofBytes = nrofBytes;
        } else {
          positionElementAll.setStartPos(block.nrByte);
          generateForVariables("", zbnfResultDataUDT.variable, null, block, positionElementAll,  0);
        }
      }
    }    
    
    
    
    /**Generates source for the block or stores information about it in dataStruct. 
     * @param nameStruct
     * @param variables
     * @param dataStruct if not null then save variable data in it. Else generate output.
     * @param block
     * @param recursiveCt
     * @throws IOException
     * @return nr of bytes of this struct
     */
    int generateForVariables(String nameStruct, List<Variable> variables, SaveDataStruct dataStruct
        , ZbnfVariablenBlock block, PositionOfVariable positionElement, int recursiveCt) 
    throws IOException
    { int bytePos = 0;
      if(block.dbName !=null && block.dbName.equals("SES_ctrl_MainD"))
        Assert.stop();
      for(Variable variable: variables){
        if(variable.sName.equals("cub2"))
          stop();
        if(variable.structVariables !=null){  //internal data struct inside the fb
          if(recursiveCt > 10) throw new IllegalArgumentException("too many recursions");
          //String nameStruct2 = nameStruct + variable.sName + ".";
          bytePos += generateForVariables(nameStruct, variable.structVariables, dataStruct, block, positionElement, recursiveCt+1);  
        }
        else {
          variable.type = indexTypes.get(variable.zbnfVariable.type);  //basicTypes
          if(variable.type == null){
            //not a basic type
            String structType = variable.zbnfVariable.type;
            if(structType.equals("SES_ctrl_CalcUcapbmax"))
              Assert.stop();
            if(structType.equals("SES_capb_ActValues_t"))
              Assert.stop();
            variable.type = allDataStruct.get(structType);
          }
          //either a simple variable or an external data struct, other FB etc.
          if(variable.type == null){
            console.writeError("unknown type: " + variable.zbnfVariable.type);
          } else {
            completeVariableWithType(variable, variable.type, positionElement);
            if(dataStruct !=null){
              dataStruct.saveVariable(variable); //, block);
            } 
            else {
              
              if(variable.type.typeCharS7 == 'O'){  //a complex structure
                //includes the variables from structure.
                SaveDataStruct dataStructIncl = (SaveDataStruct) variable.type;
                
                String nameStruct2 = nameStruct + variable.sName + ".";
                bytePos += generateForVariables(nameStruct2, dataStructIncl.variables, dataStruct, block, positionElement, recursiveCt+1);
                if(genDbConfig != null){
                  for(Variable variableIncl: dataStructIncl.variables){
                    String sName = variable.sName + "." + variableIncl.sName;
                    genDbConfig.generateDbVariableLine(variable, variable.sName + ".", block, genDbConfig.posVariable);
                    
                  }
                  //The next position.
                  genDbConfig.posVariable += dataStructIncl.position.posVariable;
                }
              } else {
                
                //Type of variable is given, simpe type
                generateForSimpleVariables(nameStruct, variable, dataStruct, block, positionElement);
                //
              }
            }
            bytePos += variable.type.nrofBytes;
          }
        }
      }
      return bytePos;
    }
    
    
    
    
    
    void generateForSimpleVariables(String nameStruct, Variable variable, SaveDataStruct dataStruct
        , ZbnfVariablenBlock block, PositionElementInStruct positionElement) 
    throws IOException
    { positionElement.adjustNonBool(variable.type.nrofBytes);
      console.reportln(Report.fineInfo, "DB" + positionElement.getIxByte() + " " + variable.sName + ":" + variable.type.winccType);
      if(dataStruct != null){
        //save only the struct, generate nothing:
        dataStruct.saveVariable(variable); //, block);
      } else { 
        //generate sources for Wincc-import and oam-DB with its access description.
        if(variable.zbnfVariable.otherRepresentation != null){
          TypeConversion otherType = indexTypes.get(variable.zbnfVariable.otherType);
          if(variable.type == null){
            console.writeError("unknown type: " + variable.zbnfVariable.otherType);
          } else {
            generatevariableEntry(variable.zbnfVariable.otherRepresentation, otherType, positionElement.getIxByte(), positionElement.getIxBit(), block);
          }  
        }
        //generate winCC-variable from name of entry
        if(variable.zbnfVariable.winCC && (variable.zbnfVariable.otherRepresentation == null || variable.zbnfVariable.additional)){
          generatevariableEntry(variable.sName, variable.type, positionElement.getIxByte(), positionElement.getIxBit(), block);
        }
        if(args.bAllVariable || variable.zbnfVariable.oam || variable.zbnfVariable.winCC){
          ////Generate the scl-FB for all oam-Variables and the access list for the Java-GUI
          generateVariableOam(nameStruct, variable, block);
        }
        if(genDbConfig != null){
          //TODO what is this, not used 10-09-30
          genDbConfig.generateDbVariableLine(variable.sName, variable.type, block);
        }
        //increment ix
        positionElement.incrPos(variable.zbnfVariable.arrayStartIx, variable.zbnfVariable.arrayEndIx, variable.type.nrofBytes);
        
      }//saveStruct else generate  
      
    }
    
    
    
    void generatevariableEntry(String name, TypeConversion type, int ixByte, int ixBit, ZbnfVariablenBlock block) 
    throws IOException
    { String sWinccStringLength = ""; //empty if not used.
      String sArrayElements = "1"; 
      int winccCapture = 2;  //kind of capturing, (1 = Auf Anforderung 2 = Zyklisch bei Verwendung (Standard) 3 = Zyklisch fortlaufend)
      String winccCycle = "500 ms";
      //create variable for WinCC
      if(block.winCCfolder != null){
        generateVariableImport.sbOut.append(block.winCCfolder + "/");
      }
      generateVariableImport.sbOut.append(block.prefix + name + block.suffix);
      generateVariableImport.sbOut.append("\tWinAC\tDB ").append(block.nrDB);
      generateVariableImport.sbOut.append(" DB").append(type.typeCharS7).append(" ").append(ixByte);
      if(type.typeCharJava == 'Z'){
        generateVariableImport.sbOut.append("." + ixBit);
      }
      generateVariableImport.sbOut.append("\t").append(type.winccType);
      generateVariableImport.sbOut.append("\t").append(sWinccStringLength);
      generateVariableImport.sbOut.append("\t").append(sArrayElements);
      generateVariableImport.sbOut.append("\t").append(winccCapture);
      generateVariableImport.sbOut.append("\t").append(winccCycle);
      generateVariableImport.sbOut.append("\t").append("\t\t\t\t\t0\t10\t0\t100\t0\t0\t");
      generateVariableImport.output.write(generateVariableImport.sbOut.append("\r\n").toString());
      generateVariableImport.sbOut.setLength(0);
    }
    
    
    
    
    /**Generates the information for one oam-variable in the container for all oam variables.
     * <ul>
     * <li>S7.scl code
     * <li>Headerfile
     * </ul>
     * @param identArgJbat
     * @param type
     * @param block
     */
    void generateVariableOam(String nameStruct, Variable variable, ZbnfVariablenBlock block)
    { String name = variable.sName;
      if(name.equals("modulVoltg"))
        stop();
      /*
      if(name.length() > 24 - block.lengthPreSuffix){
        name = name.substring(0, 24 - block.lengthPreSuffix);  //shorten the name, it should be unique else.
      }
      */
      //name in OAMvariables.scl
      //assume that names in sub-Structs don't differ.
      final StringBuilder uVariable = new StringBuilder(block.prefix + nameStruct.replace('.', '_') + name + block.suffix);
      
      final String sVariable = uVariable.toString();  //it is persistent.
      
      int pos;
      while((pos = uVariable.indexOf(".")) >=0){
        uVariable.replace(pos, pos+1, "");  //remove all "."
        uVariable.setCharAt(pos, Character.toUpperCase(uVariable.charAt(pos)));
      }
      
      final String sVariable24 = shortenScl24Names.adjustLength(uVariable.toString());
      
      int maskBitInByte;
      if(variable.type.typeCharJava == 'Z'){
        maskBitInByte = generateVariableImport.posOamVariable.getMaskBit();
      } else {
        generateVariableImport.posOamVariable.adjustNonBool(variable.type.nrofBytes);
        maskBitInByte = 0;
      }
      int posByte = generateVariableImport.posOamVariable.getIxByte();
      oamVariables.add(new OamVariable(sVariable24, variable, nameStruct, sVariable, posByte, maskBitInByte, block));

      generateVariableImport.posOamVariable.incrPos(variable.zbnfVariable.arrayStartIx, variable.zbnfVariable.arrayEndIx, variable.type.nrofBytes); 
      
      /*
      generateVariableImport.sclVariables.append("\n").append(sVariable24).append(": ");
      if(variable.zbnfVariable.arrayEndIx > variable.zbnfVariable.arrayStartIx || variable.zbnfVariable.arrayStartIx >0){
        generateVariableImport.sclVariables.append("ARRAY[").append(variable.zbnfVariable.arrayStartIx)
          .append("..").append(variable.zbnfVariable.arrayEndIx).append("] OF ");
      }
      generateVariableImport.sclVariables.append(variable.type.s7Type).append(";");
      if(variable.type.typeCharJava == 'Z'){
        if(generateVariableImport.posOamVariable.getMaskBit() == 0x1){
          generateVariableImport.sclDbHeader.append ("\n  ").append("uint16 bit_").append(sVariable).append(";");
        }
        String sMask = Integer.toHexString(generateVariableImport.posOamVariable.getMaskBit());
        generateVariableImport.sclDbHeader.append ("\n  #define ").append(sVariable).append(" ").append("0x").append(sMask);
        generateVariableImport.oamList.append("\n").append(block.winCCfolder).append('/')
        .append(sVariable).append(": ")
        .append(variable.type.typeCharJava).append(" @").append(generateVariableImport.posOamVariable.getIxByte())
        .append(".0x").append(Integer.toHexString(generateVariableImport.posOamVariable.getMaskBit()))
        .append(";//DB").append(block.nrDB);
        generateVariableImport.posOamVariable.incrPosInOriginalDb(variable.zbnfVariable.arrayStartIx, variable.zbnfVariable.arrayEndIx, 0);  //increment 1 bit.
      } else {
        generateVariableImport.posOamVariable.adjustNonBool(variable.type.nrofBytes);
        generateVariableImport.sclDbHeader.append ("\n  ").append(variable.type.cType).append(" ").append(sVariable).append(";");
        generateVariableImport.oamList.append("\n").append(block.winCCfolder).append('/').append(sVariable).append(": ")
        .append(variable.type.typeCharJava).append(" @").append(generateVariableImport.posOamVariable.getIxByte())
        .append(";  //DB").append(block.nrDB);
        generateVariableImport.posOamVariable.incrPosInOriginalDb(variable.zbnfVariable.arrayStartIx, variable.zbnfVariable.arrayEndIx, variable.type.nrofBytes); 
      }
      final String srcDb;
      if(block.dbName != null){
        srcDb = block.dbName;
      } else {
        srcDb = "DB" + block.nrDB;
      }
      generateVariableImport.sclAssignment.append("\n").append(sVariable24)
      .append(" := ").append(srcDb).append(".").append(nameStruct).append(name).append(";");
      */
    }
    
    
    
    
    
  }
  
  
  
  
  
  
  
  void stop(){} //debug
  
  
  
  
}