package org.vishia.zbnf;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.vishia.genJavaOutClass.GenJavaOutClass;
import org.vishia.genJavaOutClass.GenJavaOutClass.CmdArgs;
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
  public static final String sVersion = "2022-02-07";

  
  
  
  /**All parsed components from {@link ZbnfParser#listSubPrescript}. */
  protected TreeMap<String,ZbnfSyntaxPrescript> idxSubSyntax;
  
  

  
  
  /**Instances of this class describe a sub class in generated code.
   */
  static class SubClassZbnf extends GenJavaOutClass.SubClassJava{

    
    ZbnfSyntaxPrescript subSyntax;
    
    
    SubClassZbnf(String semantic, String className)
    { super(semantic, className);
    }
  
  }
  
  /**Command line args */
  private final CmdArgs cmdArgs;

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
    
    evaluateSyntax(mainScript);
    
  }
  
  
  
  
  
  
  private void evaluateSyntax(ZbnfSyntaxPrescript mainScript) {
    this.genClass.setupWriter();
    WrClassZbnf wrClass = this.new WrClassZbnf();  //the main class to write


    try {
      ZbnfSyntaxPrescript startScript = this.idxSubSyntax.get(mainScript.sDefinitionIdent);
      wrClass.evaluateChildSyntax(startScript.childSyntaxPrescripts, false, 1);
      wrClass.wrClassJava.writeOperations();
      //
      //
      //
      if(this.cmdArgs.bAll) {
        for(Map.Entry<String, ZbnfSyntaxPrescript> e : this.idxSubSyntax.entrySet()) {
          String sDefinitionIdent = e.getKey();
          if(!sDefinitionIdent.equals(mainScript.sDefinitionIdent)) {
            ZbnfSyntaxPrescript cmpnSyntax = e.getValue();
            SubClassZbnf classData = new SubClassZbnf(sDefinitionIdent, GenJavaOutClass.firstUppercase(sDefinitionIdent));
            classData.sDbgIdent = cmpnSyntax.sDefinitionIdent;
            classData.subSyntax = cmpnSyntax;
            genCmpnClass(classData);
          }
        }
      }
      else {
        int ixCmpn = 0;
        while(this.genClass.listCmpn.size() > ixCmpn) { //possible to add on end in loop
          SubClassZbnf classData = (SubClassZbnf)this.genClass.listCmpn.get(ixCmpn++);
          genCmpnClass(classData);
  
        }
      }
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
      wrClass.evaluateChildSyntax(classData.subSyntax.childSyntaxPrescripts, false, 0);
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
    CmdArgs args = new CmdArgs();
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
    void evaluateChildSyntax(List<ZbnfSyntaxPrescript> childScript, boolean bList, int level) throws Exception {
      
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
          //if(semantic.startsWith("@")) { semantic = semantic.substring(1); }
          if(semantic.length() >0 && semantic.charAt(0) != '@' && item.childsHasSemantic() ) {
            //It is an option etc with [<?semantic>...
            int posSep = semantic.indexOf('/');
            String subSemantic = posSep >0 ? semantic.substring(0, posSep) : semantic;
            getRegisterSubclass(subSemantic, item);
  //          SubClassZbnf metaclass = idxMetaClass.get(subSemantic);
  //          if(metaclass == null) { metaclass = new SubClassZbnf(subSemantic); idxMetaClass.put(subSemantic, metaclass); }
  //          metaclass.subSyntax = item;  //evaluate it in an extra class destination.
            ////
          }
          boolean bRepetition = bList;
          if(item.eType !=null) {
            switch(item.eType) {
              
              case kRepetition: 
              case kRepetitionRepeat:  
                if(item instanceof ZbnfSyntaxPrescript.RepetitionSyntax) {     
                  ZbnfSyntaxPrescript.RepetitionSyntax itemRepeat = (ZbnfSyntaxPrescript.RepetitionSyntax)item;
                  bRepetition = !itemRepeat.bOnlyOneEachOption && ! itemRepeat.bEntryComponentContainer;
                  if(!bRepetition)     // not necessery to have Lists for all elements or own element for repetition. 
                    Debugutil.stop();
                } else {
                  Debugutil.stop();    //do not change type of bRepetition
                  //bRepetition = true; //store immediately result in list
                }
                ZbnfSyntaxPrescript.RepetitionSyntax repeatItem = (ZbnfSyntaxPrescript.RepetitionSyntax)item;
                item2 = repeatItem.backward;
                if(item.sSemantic !=null) {      // It is [<?semantic>...]: The parsed content in [...] should be stored as String
                  wrVariable("String", null, semantic+"_Text", item, bRepetition, false, null); 
                }
                break;
              case kOnlySemantic:
              case kAlternative: 
              case kAlternativeOptionCheckEmptyFirst:
              case kSimpleOption:
              case kAlternativeOption:
                if(item.sSemantic !=null) {
                  //It is [<?semantic>...]: The parsed content in [...] should be stored as String
                  wrVariable("String", null, semantic, item, bList, false, null); 
                }
                break;
              
              case kExpectedVariant:
                break;
              
              case kFloatWithFactor:
              case kFloatNumber: wrVariable("float", null, semantic, item, bList, false, null); break;
              
              case kPositivNumber:
              case kIntegerNumber:
              case kHexNumber: wrVariable("int", null, semantic, item, bList, false, null); break;
              
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
                  wrVariable("String", null, semantic, item, bList, false, null);
                }
              } break;
              
              case kNegativVariant:
              case kNotDefined:
                break;
                
              case kSkipSpaces:
                break;
                
              case kOnlyMarker:
              case kSyntaxComponent: 
                evaluateSubCmpn(item, bList, level);
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
                Debugutil.todo();
            }
          }
          if(item.sSubSyntax !=null) {
            String sType = Character.toUpperCase(item.sSubSyntax.charAt(0)) + item.sSubSyntax.substring(1);
            String name = semantic !=null ? semantic : Character.toLowerCase(item.sSubSyntax.charAt(0)) + item.sSubSyntax.substring(1);
            wrVariable(sType, null, name, item, false, false, null);
          }
          //any item can contain an inner tree. Especially { ...inner syntax <cmpn>...}
          //in a repetition bList = true;
          if(item.childSyntaxPrescripts !=null) {
            evaluateChildSyntax(item.childSyntaxPrescripts, bRepetition, level+1);
          }
          if(item2 !=null && item2.childSyntaxPrescripts !=null) {
            evaluateChildSyntax(item2.childSyntaxPrescripts, bRepetition, level+1);
          }
        }
      }
    }

    private SubClassZbnf getRegisterSubclass(String name, ZbnfSyntaxPrescript syntaxItem) {
      SubClassZbnf classData = (SubClassZbnf)GenZbnfJavaData.this.genClass.idxRegisteredCmpn.get(name);
      if(classData == null) {
        classData = new SubClassZbnf(name, GenJavaOutClass.firstUppercase(name));
        classData.sDbgIdent = syntaxItem.sDefinitionIdent;
        classData.subSyntax = syntaxItem;
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
    private void evaluateSubCmpn(ZbnfSyntaxPrescript item, boolean bList, int level) throws Exception {
      if(item.sDefinitionIdent.startsWith("ST"))
        Debugutil.stop();
      ZbnfSyntaxPrescript prescript = GenZbnfJavaData.this.idxSubSyntax.get(item.sDefinitionIdent); 
      if(prescript == null) throw new IllegalArgumentException("error in syntax, component not found: " + item.sDefinitionIdent);
      //on semantic "@" in the item the semantic of the prescript should be used.
      //That is usually the same like item.sDefinitionIdent, but can be defined other via cpmpn::=<?semantic> 
      String semantic = item.sSemantic == null ? null : item.sSemantic.equals("@") ? prescript.sSemantic : item.sSemantic; 
      if(semantic == null) { //either the item is written with <...?> or the prescript with ::=<?> 
      //if(item.sSemantic == null || prescript.sSemantic == null) {
        //expand here
        evaluateChildSyntax(prescript.childSyntaxPrescripts, bList, level);
      }
      else {
        if(item.bStoreAsString) {
          String name = item.bDonotStoreData ? semantic : semantic + "_string";
          wrVariable("String", null, name, null, bList, false, null); ////xx
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
            registerCmpn(item);
          }
          wrVariable(sTypeRef, sTypeObj, semantic, item, bList, true, obligateAttribs);
        }
      }
      
    }

    protected void wrVariable(String typeRef, String typeObj, String semantic, ZbnfSyntaxPrescript syntaxitem, boolean bList
          , boolean bCmpn, List<String> obligateAttribs
          ) throws Exception {
      final String sTypeObj1 = (typeObj == null) ? typeRef :  typeObj;  
      if(semantic !=null && semantic.length() >0) { //else: do not write, parsed without data
            if(semantic.startsWith("ST"))
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
              wrVariable(semantic1, semantic1, semantic1, syntaxitem, bList, true, obligateAttribs1);  //create the parent
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
                
                this.wrClassJava.wrVariable(semantic2, typeRef2, sTypeObj1, !bCmpn, bList, bCmpn, args); 
                
              }  
            }
          }
        }

    /**Registers a need Component. It is invoked on usage of a component, not for existing component definitions.
     * It means unused components do not create classes.
     * @param item The item which uses the component
     */
    private void registerCmpn(ZbnfSyntaxPrescript item) {
      ZbnfSyntaxPrescript cmpn = GenZbnfJavaData.this.idxSubSyntax.get(item.sDefinitionIdent);
      if(cmpn == null) {
        throw new IllegalArgumentException("syntax component not found: " + item.sDefinitionIdent);
      }
      String name = cmpn.sSemantic;
      if(name ==null || name.equals("@")) {
        name = cmpn.sDefinitionIdent;
      }
      if(GenZbnfJavaData.this.genClass.idxRegisteredCmpn.get(name) == null) {
        SubClassZbnf classData = new SubClassZbnf(name, GenJavaOutClass.firstUppercase(name));
        classData.sDbgIdent = cmpn.sDefinitionIdent;
        classData.subSyntax = cmpn;
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
  
      public final CmdArgs argData;
      
      /*---------------------------------------------------------------------------------------------*/
      /**Constructor of the cmdline handling class.
      The command line arguments are parsed here. After them the execute class is created as composition of SampleCmdLine.
  */
      protected CmdLine(CmdArgs argData, String[] sCmdlineArgs)
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
