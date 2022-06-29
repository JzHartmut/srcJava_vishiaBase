package org.vishia.zbnf;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.vishia.genJavaOutClass.GenJavaOutClass;
import org.vishia.mainCmd.MainCmd;
import org.vishia.mainCmd.MainCmdLogging_ifc;
import org.vishia.util.Arguments;
import org.vishia.util.CheckVs;
import org.vishia.util.Debugutil;

/**This class is used to generate two Java source files as container for parsed data derived from the zbnf syntax script.
 *
 */
public class GenZbnfJavaData
{

  /**Version, history and license.
   * <ul>
   * <li>2022-06-06 Argument for typeNs prepared, due to {@link GenJavaOutClass} change. 
   * <li>2022-05-13 Hartmut new: regards {@link #bOnlyOneEach} ?& to prevent a container for parse result, only one element.    
   * <li>2022-04-30 Hartmut: <ode>{&lt;?*semantic>...</code> is a component, on calling {@link WrClassZbnf#wrVariable(SubClassZbnf, String, String, String, String, ZbnfSyntaxPrescript, boolean, boolean, List)}
   *   called in {@link WrClassZbnf#evaluateChildSyntax(List, SubClassZbnf, boolean, int)}.
   *   There also: regarded there: {@link ZbnfSyntaxPrescript.EType#kStoreSrc}
   * <li>2022-02-18 Hartmut: Improvements for basic types, now proper without manual corrections:
   *   The typeNs argument should be null for basic types. It is tested. The value of typeNs is yet not used,
   *   here some improvement opportunities TODO.  
   * <li>2022-02-11 Hartmut only adaptions to changed {@link GenJavaOutClass}
   * <li>2022-02-07 Hartmut chg {@link WrClassZbnf#evaluateChildSyntax(List, boolean, int)}: 
   *   <ul><li>If {@link ZbnfSyntaxPrescript#bAssignIntoNextComponent} is set, this element must not placed in the current class.
   *   <li>Instead, in the syntax.zbnf an element <...?+!...> should be defined, not for parsing, only for generating here. See kOnlyMarker
   *   <li>semantic = "@", this marker is used to set the semantic from {@link ZbnfSyntaxPrescript#sDefinitionIdent}, must regard here, not done in past...?
   *   <li>Now {@link ZbnfSyntaxPrescript.RepetitionSyntax#bOnlyOneEachOption} is regarded to prevent producing a List for the elements. 
   *   </ul>   
   * <li>2022-02-07 Hartmut chg {@link WrClassZbnf#wrVariable(String, String, ZbnfSyntaxPrescript, boolean, boolean, List): better error report.   
   * <li>2021-11-25 Hartmut chg {@link CmdLine}: Argument <code>-dirJava:$(ENV)/...</code> can contain now environment variables
   *   which are resolved using {@link Arguments#replaceEnv(String)}.
   * <li>2021-11-25 Hartmut chg {@link #evaluateSyntax(ZbnfSyntaxPrescript)} better error message, write what is faulty.
   * <li>2021-11-25 Hartmut chg #evaluateChildSyntax(...) argument <code>item</code> in the moment bad commented.
   * <li>2020-07-16 Hartmut chg using LinkedList instead ArrayList. Question of philosophy. 
   * <li>2020-07-16 Hartmut new regard &lt;...?""...> store as String and as parsed data.
   * <li>2019-12-08 new control of class name with component::=&lt;name>
   * <li>2019-12-08 Hartmut only formally gardening (prevent warnings)
   * <li>2019-08-17 Hartmut divided in 2 classes, the {@link GenJavaOutClass} has gotten most content from here,
   *   but that class is used from {@link org.vishia.xmlReader.GenXmlCfgJavaData} too with common approaches.  
   * <li>2019-05-14 Hartmut creation
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
   * <li> But the LPGL is not appropriate for a whole software product,
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
  public static final String sVersion = "2022-06-06";
  
  
  
  
  /**All parsed components from {@link ZbnfParser#listSubPrescript}. */
  protected TreeMap<String,ZbnfSyntaxPrescript> idxSubSyntax;
  
  
  
  
  
  /**Instances of this class describe a sub class in generated code.
   */
  static class SubClassZbnf extends GenJavaOutClass.SubClassJava{

    
    final ZbnfSyntaxPrescript subSyntax;
    
    
    SubClassZbnf(ZbnfSyntaxPrescript subSyntax, String semantic, String className)
    { super(semantic, className, subSyntax.sSuperItemType);
      this.subSyntax = subSyntax;
    }
  }
  
  
  
  /**Command line args */
  private final GenJavaOutClass.CmdArgs cmdArgs;

  private final MainCmdLogging_ifc log;
  

  
  private final GenJavaOutClass genClass;
  
  
  //private final Map<String, String> superTypes = new TreeMap<String, String>();
  
  
  public GenZbnfJavaData(GenJavaOutClass.CmdArgs cmdArgs, MainCmdLogging_ifc log)
  { this.cmdArgs = cmdArgs;
    this.log = log;
    this.genClass = new GenJavaOutClass(cmdArgs, log);
  }


  
  
  
  
  public void setAndEvaluateSyntax() {
    ZbnfParser parser = new ZbnfParser(this.log);
    try {
      parser.setSyntax(this.cmdArgs.fileInput);
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    this.idxSubSyntax = parser.listSubPrescript;
    ZbnfSyntaxPrescript mainScript = parser.mainScript();
    if(this.cmdArgs.fileOutStruct !=null) {
      try {
        Writer wOutStruct = new FileWriter(this.cmdArgs.fileOutStruct);
        parser.writeSyntaxStruct(wOutStruct);
        wOutStruct.close();
      }
      catch(Exception exc) {
        System.err.println("Error writing syntax struct: " + this.cmdArgs.fileOutStruct.getAbsolutePath());
      }
    }
    evaluateSyntax(mainScript);
    
  }
  
  
  
  
  
  
  private void evaluateSyntax(ZbnfSyntaxPrescript mainScript) {
    this.genClass.setupWriter();
    WrClassZbnf wrClass = this.new WrClassZbnf();  //the main class to write


    try {
      ZbnfSyntaxPrescript startScript = this.idxSubSyntax.get(mainScript.sDefinitionIdent);
      SubClassZbnf classStart = new SubClassZbnf(startScript, mainScript.sDefinitionIdent, mainScript.sDefinitionIdent);
      wrClass.evaluateChildSyntax(startScript.childSyntaxPrescripts, classStart, false, 1);
      wrClass.wrClassJava.writeOperations();
      //
      //
      //
      if(this.cmdArgs.bAll) {
        for(Map.Entry<String, ZbnfSyntaxPrescript> e : this.idxSubSyntax.entrySet()) {
          String sDefinitionIdent = e.getKey();
          if(!sDefinitionIdent.equals(mainScript.sDefinitionIdent)) {
            ZbnfSyntaxPrescript cmpnSyntax = e.getValue();
            wrClass.registerCmpn(cmpnSyntax);
//            SubClassZbnf classData = new SubClassZbnf(cmpnSyntax, sDefinitionIdent, GenJavaOutClass.firstUppercase(sDefinitionIdent));
//            classData.sDbgIdent = cmpnSyntax.sDefinitionIdent;
//            GenZbnfJavaData.this.genClass.idxRegisteredCmpn.put(sDefinitionIdent, classData);
//            GenZbnfJavaData.this.genClass.listCmpn.add(classData);
            //genCmpnClass(classData);
          }
        }
      }
//      else {
        int ixCmpn = 0;   //=============================  // enroll all components
        while(this.genClass.listCmpn.size() > ixCmpn) { //possible to add on end in loop
          SubClassZbnf classData = (SubClassZbnf)this.genClass.listCmpn.get(ixCmpn++);
          genCmpnClass(classData);
  
        }
//      }
      //
      this.genClass.finishClassWrite();
      //
    } catch (Exception e1) {
      // TODO Auto-generated catch block
      //System.err.println("ERROR: " + e1.getMessage());
      CharSequence info = CheckVs.exceptionInfo("ERROR: ", e1, 0, 10);
      System.err.append(info);
    }
    this.genClass.closeWrite();    
  }

  
  
  
  /**Generate the content of a dst class from a syntax component.
   * @param classData contains syntax
   * @throws Exception
   */
  private void genCmpnClass ( SubClassZbnf classData) throws Exception {
    String sSuperType = classData.subSyntax.sSuperItemType;
    WrClassZbnf wrClass = new WrClassZbnf();
    if(classData.subSyntax.sDefinitionIdent.equals("description"))
      Debugutil.stop();
    wrClass.wrClassJava.wrClassCmpn(classData, classData.subSyntax.sSuperItemType);
    //
    //
    if(classData.subSyntax.childSyntaxPrescripts !=null) {
      wrClass.evaluateChildSyntax(classData.subSyntax.childSyntaxPrescripts, classData, false, 0);
    }
    if(classData.subSyntax instanceof ZbnfSyntaxPrescript.RepetitionSyntax) {
      ZbnfSyntaxPrescript.RepetitionSyntax repSyntax = (ZbnfSyntaxPrescript.RepetitionSyntax)classData.subSyntax;
      ZbnfSyntaxPrescript repeatSyntax = repSyntax.getRepetitionBackwardPrescript();
      if(repeatSyntax !=null && repeatSyntax.childSyntaxPrescripts !=null) {
        wrClass.evaluateChildSyntax(repeatSyntax.childSyntaxPrescripts, classData, false, 0);
      }
    }
    //
    wrClass.wrClassJava.writeOperations();
    //
    this.genClass.finishCmpnWrite();
    
  }

  
  
  
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
    GenJavaOutClass.CmdArgs args = new GenJavaOutClass.CmdArgs();
    CmdLine mainCmdLine = new CmdLine(args, sArgs); //the instance to parse arguments and others.
    try{
      mainCmdLine.addCmdLineProperties();
      boolean bOk;
      try{ bOk = mainCmdLine.parseArguments(); }
      catch(Exception exception)
      { mainCmdLine.report("Argument error:", exception);
        mainCmdLine.setExitErrorLevel(MainCmdLogging_ifc.exitWithArgumentError);
        bOk = false;
      }
      if(bOk)
      { GenZbnfJavaData main = new GenZbnfJavaData(args, mainCmdLine);     //the main instance
        /* The execution class knows the SampleCmdLine Main class in form of the MainCmd super class
           to hold the contact to the command line execution.
        */
        try{ main.setAndEvaluateSyntax(); }
        catch(Exception exc)
        { //catch the last level of error. No error is reported direct on command line!
          System.err.println(exc.getMessage());
        }
      }
      sRet = "";
    } catch(Exception exc){
      exc.printStackTrace();
      sRet = exc.getMessage();
    }
    if(shouldExitVM) { mainCmdLine.exit(); }
    return sRet;
  }

  
  
  private class WrClassZbnf {

    
    final GenJavaOutClass.WrClassJava wrClassJava;
    
    
    WrClassZbnf() {
      this.wrClassJava = GenZbnfJavaData.this.genClass.new WrClassJava();
    }
    
    
    /**An syntax item can have an inner syntax tree. It is not a component. 
     * The result of the inner tree is stored in the same level.
     * If a called component is found, it is checked whether it has an own semantic
     * and the component's syntax is not contained in #idxCmpnWithoutSemantic ( name::=&lt;?> )
     * Then it is added to build a new class.
     * If it has not a semantic, the called component is expand here. 
     * @param childScript
     * @param bList
     * @param level
     * @throws IOException
     */
    void evaluateChildSyntax(List<ZbnfSyntaxPrescript> childScript, SubClassZbnf classData, boolean bList, int level) throws Exception {
      for(ZbnfSyntaxPrescript item: childScript) {
        if(item.sSemantic !=null && item.sSemantic.equals("argument"))
          Debugutil.stop();
        if(item.sDefinitionIdent !=null && item.sDefinitionIdent.equals("type"))
          Debugutil.stop();
        if(item.eType == ZbnfSyntaxPrescript.EType.kOnlyMarker)
          Debugutil.stop();
        if(item.bAssignIntoNextComponent) {      // ignore this items, not necessary here. 
          //                                     // It should be defined in the component in form <...?+!...>
        }
        else {
          ZbnfSyntaxPrescript item2 = null;  //any addtional item to convert
          String semantic = item.sSemantic == null ? "" : item.sSemantic;
          if(semantic.equals("@")) {             // marker for <syntax?+?> semantic == syntax
            semantic = item.sDefinitionIdent;
          }
          if(semantic.equals("ExprPart"))
            Debugutil.stop();
          //if(semantic.startsWith("@")) { semantic = semantic.substring(1); }
          if(semantic.length() >0 && semantic.charAt(0) != '@' && item.childsHasSemantic() ) {
            //It is an option etc with [<?semantic>...
            int posSep = semantic.indexOf('/');
            String subSemantic = posSep >0 ? semantic.substring(0, posSep) : semantic;
//            getRegisterSubclass(subSemantic, item);
  //          SubClassZbnf metaclass = idxMetaClass.get(subSemantic);
  //          if(metaclass == null) { metaclass = new SubClassZbnf(subSemantic); idxMetaClass.put(subSemantic, metaclass); }
  //          metaclass.subSyntax = item;  //evaluate it in an extra class destination.
            ////
          }
          boolean bRepetition = bList;                     // from calling level, whether it is inside a repetition. 
          boolean bListVar = bList && !item.bOnlyOneEach;  // for the current created variable
          boolean bEvaluateChildSyntax = true;
          if(item.eType !=null) {
            switch(item.eType) {
              
              case kRepetition: 
              case kRepetitionRepeat:  
                if(item instanceof ZbnfSyntaxPrescript.RepetitionSyntax) {     
                  ZbnfSyntaxPrescript.RepetitionSyntax itemRepeat = (ZbnfSyntaxPrescript.RepetitionSyntax)item;
                  bRepetition = !itemRepeat.bOnlyOneEachOption; // && ! itemRepeat.bEntryComponentContainer;
                  if(!bRepetition)     // not necessery to have Lists for all elements or own element for repetition. 
                    Debugutil.stop();
                } else {
                  Debugutil.stop();    //do not change type of bRepetition
                  //bRepetition = true; //store immediately result in list
                }
                ZbnfSyntaxPrescript.RepetitionSyntax repeatItem = (ZbnfSyntaxPrescript.RepetitionSyntax)item;
                item2 = repeatItem.backward;
                if(item.sSemantic !=null) {      // It is [<?semantic>...]: The parsed content in [...] should be stored as String
                  if(repeatItem.bEntryComponentContainer) {
                    bEvaluateChildSyntax = false;
                    String sType = Character.toUpperCase(semantic.charAt(0)) + semantic.substring(1);
                    String typeNs = GenZbnfJavaData.this.cmdArgs.sJavaClass + ".";
                    wrVariable(classData, typeNs, sType, null, semantic, item, bRepetition, true, null);  //it is as a component, but only inline
                    //evaluateSubCmpn(item, classData, false, level);
                    registerCmpn(item);                    // enroll content as component later
                    
                  } else {
                    wrVariable(classData, null, "String", null, semantic+"_Text", item, bRepetition, false, null); 
                  }
                }
                break;
              case kOnlySemantic:
              case kAlternative: 
              case kAlternativeOptionCheckEmptyFirst:
              case kSimpleOption:
              case kAlternativeOption:
                if(item.sSemantic !=null) {
                  //It is [<?semantic>...]: The parsed content in [...] should be stored as String
                  wrVariable(classData, null, "String", null, semantic, item, bListVar, false, null); 
                }
                break;
              
              case kExpectedVariant:
                break;
              
              case kFloatWithFactor:
              case kFloatNumber: wrVariable(classData, null, "float", null, semantic, item, bListVar, false, null);
              break;
              
              case kPositivNumber:
              case kIntegerNumber:
              case kHexNumber: wrVariable(classData, null, "int", null, semantic, item, bListVar, false, null);
              break;
              
              case kStoreSrc:
              case kStringUntilEndString:
              case kStringUntilEndStringInclusive:
              case kStringUntilEndStringTrim:
              case kStringUntilEndStringWithIndent:
              case kStringUntilEndchar:
              case kStringUntilEndcharInclusive:
              case kStringUntilEndcharOutsideQuotion:
              case kStringUntilEndcharWithIndent:
              case kStringUntilRightEndchar:
              case kStringUntilRightEndcharInclusive:
              case kQuotedString:
              case kRegularExpression:
              case kIdentifier: {
                if(item.sSubSyntax ==null) {
                  wrVariable(classData, null, "String", null, semantic, item, bListVar, false, null);
                }
              } break;
              
              case kNegativVariant:
              case kNotDefined:
                break;
                
              case kSkipSpaces:
                break;
                
              case kOnlyMarker:
              case kSyntaxComponent: 
                evaluateSubCmpnCall(item, classData, bListVar, level);
                break;
                
              case kSyntaxDefinition:
                break;
              case kTerminalSymbol:
                break;
              case kTerminalSymbolInComment:
                break;
              case kUnconditionalVariant:
                break;
              default:
                assert(false);
            }
          }
          if(item.sSubSyntax !=null) {
            String sType = Character.toUpperCase(item.sSubSyntax.charAt(0)) + item.sSubSyntax.substring(1);
            String typeNs = GenZbnfJavaData.this.cmdArgs.sJavaClass + ".";  //A type defined inside the dataClass
            String name = semantic !=null ? semantic : Character.toLowerCase(item.sSubSyntax.charAt(0)) + item.sSubSyntax.substring(1);
            wrVariable(classData, typeNs, sType, null, name, item, false, false, null);
          }
          //any item can contain an inner tree. Especially { ...inner syntax <cmpn>...}
          //in a repetition bList = true;
          if(bEvaluateChildSyntax) { 
            if(item.childSyntaxPrescripts !=null) {
              evaluateChildSyntax(item.childSyntaxPrescripts, classData, bRepetition, level+1);
            }
            if(item2 !=null && item2.childSyntaxPrescripts !=null) {
              evaluateChildSyntax(item2.childSyntaxPrescripts, classData, bRepetition, level+1);
            }
          }
        }
      }
    }

    private SubClassZbnf getRegisterSubclass(String name, ZbnfSyntaxPrescript syntaxItem) {
      SubClassZbnf classData = (SubClassZbnf)GenZbnfJavaData.this.genClass.idxRegisteredCmpn.get(name);
      if(classData == null) {
        classData = new SubClassZbnf(syntaxItem, name, GenJavaOutClass.firstUppercase(name));
        classData.sDbgIdent = syntaxItem.sDefinitionIdent;
        GenZbnfJavaData.this.genClass.idxRegisteredCmpn.put(name, classData);
        GenZbnfJavaData.this.genClass.listCmpn.add(classData);
        ////
      }
      return classData;
    }

    /**This routine is called for <code>&lt;cmpnSyntax...></code>.
     * <ul>
     * <li>The component is searched in {@link #idxSubSyntax}. It should be found, elsewhere it is an IllegalArgumentException
     * <li>The semantic is taken either from the component's definition if it is not given on the item.
     *   In this case it is written in source like <code>&lt;component></code> and item[@link #sSemantic} contains "@"-
     * <li>In that case the semantic is usual the syntax identifier.
     * <li>In that case it can be abbreviating because <code>component::=&lt;?semantic></code> is given.
     * <li>In that case the semantic can be null if <code>component::=&lt;?></code> is given. See next List.
     * <li>The semantic is taken from the item if <code>&lt;component?semantic></code> is given.
     * <li>The semantic is null if <code>&lt;component?></code> is given.
     * </ul>
     * Depending on semantic == null:
     * <ul>
     * <li>If the semantic is null, then the component's syntax definition is used
     * and the component's data are created in this class.  
     * <li>If the semantic is given then a container for the component's data is created in this class 
     *   via {@link #wrVariable(String, String, boolean, boolean)}
     *   and the component's name is {@link #registerCmpn(String)} to create a class for it later if not created already. 
     * </ul>  
     * @param item The calling item of the component
     * @param bList true if the syntax is part of a repetition
     * @param level
     * @throws IOException
     */
    private void evaluateSubCmpnCall(ZbnfSyntaxPrescript item, SubClassZbnf classData, boolean bList, int level) throws Exception {
      if(item.sDefinitionIdent.startsWith("Expression"))
        Debugutil.stop();
      ZbnfSyntaxPrescript prescript = GenZbnfJavaData.this.idxSubSyntax.get(item.sDefinitionIdent); 
      if(prescript == null) throw new IllegalArgumentException("error in syntax, component not found: " + item.sDefinitionIdent);
      //on semantic "@" in the item the semantic of the prescript should be used.
      //That is usually the same like item.sDefinitionIdent, but can be defined other via cpmpn::=<?semantic> 
      String semantic = item.sSemantic == null ? null : item.sSemantic.equals("@") ? prescript.sSemantic : item.sSemantic; 
      boolean bListVar = bList && !item.bOnlyOneEach;
      if(semantic == null) { //either the item is written with <...?> or the prescript with ::=<?> 
        //-----------------------------------------------  // the components content is expand here:
        evaluateChildSyntax(prescript.childSyntaxPrescripts, classData, bListVar, level);
      }
      else {                                               // creates a variable for the component. 
        if(item.bStoreAsString) {
          String name = item.bDonotStoreData ? semantic : semantic + "_string";
          wrVariable(classData, null, "String", null, name, null, bListVar, false, null); ////xx
        }
        if(!item.bDonotStoreData) {
        //create an own class for the component, write a container here.
          List<String> obligateAttribs = null;
          if(prescript.childSyntaxPrescripts !=null) for( ZbnfSyntaxPrescript subitem: prescript.childSyntaxPrescripts) {
            if(subitem.sSemantic !=null && subitem.sSemantic.length()>1 && subitem.sSemantic.charAt(0) == '@') {
              //an syntaxSymbol which is requested (because not in an option etc) and it is an attribute:
              if(obligateAttribs==null) { obligateAttribs = new LinkedList<String>(); }
              obligateAttribs.add(subitem.sSemantic.substring(1));
            }
          }
          String sTypeObj = prescript.sSemantic;
          if(sTypeObj == null || sTypeObj.equals("@")) {
            sTypeObj = item.sDefinitionIdent;
          }
          sTypeObj = GenJavaOutClass.firstUppercase(sTypeObj);
          String sTypeRef = prescript.sSuperItemType;
          if(sTypeRef ==null) {
            sTypeRef = sTypeObj;
          } else {
            Debugutil.stop();
          }
          
          if(sTypeObj.equals("Integer")) {
            Debugutil.stop();
          } else {
            ZbnfSyntaxPrescript cmpn = GenZbnfJavaData.this.idxSubSyntax.get(item.sDefinitionIdent);
            if(cmpn == null) {
              throw new IllegalArgumentException("syntax component not found: " + item.sDefinitionIdent);
            }
            
            registerCmpn(cmpn);                            // register the component to evaluate later.
          }
          String typeNs = GenZbnfJavaData.this.cmdArgs.sJavaClass + ".";
          //---------------------------------------------  // the variable for the component's reference
          wrVariable(classData, typeNs, sTypeRef, sTypeObj, semantic, item, bListVar, true, obligateAttribs);
        }
      }
      
    }

    protected void wrVariable(SubClassZbnf classData, String typeNs, String typeRef, String typeObj, String semantic, ZbnfSyntaxPrescript syntaxitem, boolean bListVar
          , boolean bCmpn, List<String> obligateAttribs
          ) throws Exception {
      final String sTypeObj1 = (typeObj == null) ? typeRef :  typeObj;  
      if(semantic !=null && semantic.length() >0) { //else: do not write, parsed without data
        if(semantic.startsWith("ST"))
          Debugutil.stop();
        if(sTypeObj1.startsWith("String"))
          Debugutil.stop();
        int posSep = semantic.indexOf('/');
        if(posSep>0) {
          String semantic1 = semantic.substring(0,  posSep);
          String semantic2 = semantic.substring(posSep+1);
          List<String> obligateAttribs1 = null;
          if(semantic2.startsWith("@")) {
            semantic2 = semantic2.substring(1);
            //The second part is an Attribute, it is a default one. 
            obligateAttribs1 = new LinkedList<String>();
            obligateAttribs1.add(semantic2);
          }
          SubClassZbnf metaClass = getRegisterSubclass(semantic1, syntaxitem); //idxMetaClass.get(semantic1);
//          if(metaClass == null) { 
//            metaClass = new SubClassZbnf(semantic1);
//            idxMetaClass.put(semantic1, metaClass);
//          }
          //Writes a new MetaClass, it is like a Component.
          wrVariable(classData, typeNs, semantic1, semantic1, semantic1, syntaxitem, bListVar, true, obligateAttribs1);  //create the parent
          GenJavaOutClass.SubClassField elems = new GenJavaOutClass.SubClassField(typeRef, GenJavaOutClass.firstLowercase(semantic2), semantic2);
          //elems.put("varName", firstLowercase(semantic2));
          //elems.put("semantic", semantic2);
          //elems.put("type", type);
          if(metaClass.fieldsFromSemanticAttr == null) { metaClass.fieldsFromSemanticAttr = new TreeMap<String, GenJavaOutClass.SubClassField>(); }
          metaClass.fieldsFromSemanticAttr.put(semantic2, elems);
        }
        else {
          if(semantic.equals("secondOperand"))
            Debugutil.stop();
          String sVariableExist = this.wrClassJava.variables.get(semantic);
          if(sVariableExist !=null) {                  // sVariableExist is stored with the type
            if(! sVariableExist.equals(typeRef)) {        // check the type, then ok, else error
              ZbnfSyntaxPrescript item1 = syntaxitem;  // error is: using the same semantic in syntax rule with different type.
              String sText = "\n  Semantic <...?" + semantic + "> with different types\n  ";
              while(item1 !=null) {                    // if the same semantic with same type is used but faultyness,
                sText += item1.toString() + "\n  ";    // ... it cannot be detected.
                item1 = item1.parent;
              }
              throw new IllegalArgumentException(sText);
            }
          } else {
            final String typeRef2;
            if(typeRef.equals("Integer")) { 
              typeRef2 = "int"; 
            } else {
              typeRef2 = typeRef;
            }
            if(semantic.equals("FBType")) {  //a required Attribute in XML
              Debugutil.stop();
            }
            if(semantic.indexOf("@")>=0) {  //a required Attribute in XML
              Debugutil.stop();
            }
            List<String> args = null;
            //String attribs = "";
            //String attribsAssign = "";
            if(obligateAttribs !=null) for(String attrib: obligateAttribs) {
              if(args == null) {args = new LinkedList<String>(); }
              args.add(GenJavaOutClass.firstLowercase(attrib));
            }
            //semantic = semantic.replace("@!", "");
            String semantic2 = semantic.replace("@", "");
            semantic2 = semantic2.replace("/", "_");
            if(semantic2.equals("value"))
              Debugutil.stop();
            
            this.wrClassJava.wrVariable(classData, semantic2, typeNs, typeRef2, sTypeObj1, !bCmpn, bListVar, bCmpn, args); 
            
          }  
        }
      }
      Debugutil.stop();
    }

    /**Registers a need Component. It is invoked either on usage of a component, or for all existing component definitions.
     * Invocation on usage only means unused components do not create classes.
     * If a component's start item is already registered, it is not done twice. 
     * @param cmpn The syntax definition head item of the components definition.
     * It is either a name::=... or also a {&lt;?*name>...}
     */
    private void registerCmpn(ZbnfSyntaxPrescript cmpn) {
      String name = cmpn.sSemantic;
      if(name ==null || name.equals("@")) {
        name = cmpn.sDefinitionIdent;
      }                                                    // only add a new component if it is not done already.
      if(GenZbnfJavaData.this.genClass.idxRegisteredCmpn.get(name) == null) {
        SubClassZbnf classData = new SubClassZbnf(cmpn, name, GenJavaOutClass.firstUppercase(name));
        classData.sDbgIdent = cmpn.sDefinitionIdent;
        GenZbnfJavaData.this.genClass.idxRegisteredCmpn.put(name, classData);
        GenZbnfJavaData.this.genClass.listCmpn.add(classData);
        ////
      }
    }
    
  }
  
  
  
  
  
  
  
  

    /**The inner class CmdLine helps to evaluate the command line arguments
     * and show help messages on command line.
     */
    private static class CmdLine extends MainCmd
    { 
    
      
      
      public final MainCmd.Argument[] defArguments =
      { new MainCmd.Argument("-s", "<SYNTAX>    syntax prescript in ZBNF format for parsing", new MainCmd.SetArgument(){ 
            @Override public boolean setArgument(String val){ 
              CmdLine.this.argData.fileInput = new File(val);  return true;
            }})
      , new MainCmd.Argument("-struct", ":<fileStruct.txt> out file to show the struct, optional", new MainCmd.SetArgument(){ 
          @Override public boolean setArgument(String val){ 
            argData.fileOutStruct = new File(Arguments.replaceEnv(val));  return true;
          }})
      , new MainCmd.Argument("-dirJava", ":<dirJava>    directory for Java output", new MainCmd.SetArgument(){ 
        @Override public boolean setArgument(String val){ 
          String val1 = Arguments.replaceEnv(val);
          CmdLine.this.argData.dirJava = new File(val1);  return true;
        }})    
      , new MainCmd.Argument("-pkg", ":<pkg.path>    directory for Java output", new MainCmd.SetArgument(){ 
        @Override public boolean setArgument(String val){ 
          CmdLine.this.argData.sJavaPkg = val;  return true;
        }})    
      , new MainCmd.Argument("-class", ":<class>.java    directory for Java output", new MainCmd.SetArgument(){ 
        @Override public boolean setArgument(String val){ 
          CmdLine.this.argData.sJavaClass = val;  return true;
        }})    
      , new MainCmd.Argument("-all", " generate Sub classes from all syntax components", new MainCmd.SetArgument(){ 
            @Override public boolean setArgument(String val){ 
              CmdLine.this.argData.bAll = true;  return true;
        }})    
      };
  
      public final GenJavaOutClass.CmdArgs argData;
      
      /*---------------------------------------------------------------------------------------------*/
      /**Constructor of the cmdline handling class.
      The command line arguments are parsed here. After them the execute class is created as composition of SampleCmdLine.
  */
      protected CmdLine(GenJavaOutClass.CmdArgs argData, String[] sCmdlineArgs)
      { super(sCmdlineArgs);
        this.argData = argData;
      }
      
  
      void addCmdLineProperties(){
        super.addAboutInfo("Generate Java source code as destination class from ZBNF syntax script");
        super.addAboutInfo("made by HSchorrig, 2019-05-10..2019-08-29");
        super.addArgument(this.defArguments);
        super.addHelpInfo("==Standard arguments of MainCmd==");
        super.addStandardHelpInfo();
      }
      
    
    
    
      /*---------------------------------------------------------------------------------------------*/
      /**Checks the cmdline arguments relation together.
         If there is an inconsistents, a message should be written. It may be also a warning.
         @return true if successfull, false if failed.
      */
      @Override
      protected boolean checkArguments()
      { boolean bOk = true;
    
        if(this.argData.fileInput == null)            { bOk = false; writeError("ERROR argument Syntaxfile is obligat."); }
        else if(this.argData.fileInput.length()==0)   { bOk = false; writeError("ERROR argument Syntaxfile without content.");}
    
        return bOk;
    
     }
    }//class CmdLine

  
  
}
