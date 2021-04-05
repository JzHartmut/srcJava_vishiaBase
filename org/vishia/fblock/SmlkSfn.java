package org.vishia.fblock;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.vishia.header2Reflection.CheaderParser;
import org.vishia.util.Debugutil;

/**This class supports generation of Simulink S-Functions from header files. 
 * 
 * @author Hartmut Schorrig
 *
 */
public class SmlkSfn {

  
  /**Version, License and History:
   * <ul>
   * <li>2021-02-19 Hartmut new: Created from jzTc algorithm, Java is more obviously  
   * </ul>
   * 
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
  public static final String version = "2021-02-20";


  /**Wrapper for Operations with additional information about Step time and characteristic. 
   * TODO: really need?
   */
  public static class ZbnfOpData {
    final CheaderParser.MethodDef zbnfOp;
    
    
    /**Step time designation. 
     * If "!" or "#" then ctor and init. 
     * Else name of the step time as "Tstep", "Tport1" etc.
     * If starts with "+" then it is the update routine. 
     */
    final String steptime;
    
    final Whatisit whatisit;


    public ZbnfOpData(CheaderParser.MethodDef zbnfOp, String steptime, Whatisit whatisit) {
      this.zbnfOp = zbnfOp;
      this.steptime = steptime;
      this.whatisit = whatisit;
    }
    
    
  }
  
  
  
  
  /**It describes the kind of Ports and Parameters. Contains the enum Definition 
   * for C:  <code>EPortType_Entry_DefPortType_emC</code>, see header file <code>emC/Base/DefPortTypes_emC.h</code> 
   */
  enum WhatPort {
    
    /**Input for this on Operation-FB, used as normal Input. */
    thisIn(false, "mInputStep_Entry_DefPortType_emC"),
    
    /**Dummy input for step order, not used for C-operations.*/
    stepIn(false, "mStepIn_Entry_DefPortType_emC"),
    
    /**Dummy output for step order, not set, remain 0. */
    stepOut(false, "mStepOut_Entry_DefPortType_emC"),
    
    /**Special output for Simulink connection to trigger input of a Triggered Sub system.
     * It is a special simulink feature. Commonly it is an event connection. */
    fnCallTrgOut(false, "mOutputFnCall_Entry_DefPortType_emC"),
    
    /**Output for thiz, als handle, with Tinit, to connect aggregations. */
    thisOutInit(false, "mOutputThizInit_Entry_DefPortType_emC"),

    /**Output for thiz, als handle, with Tstep, to connect in step order. */
    thisOutStep(false, "mOutputThizStep_Entry_DefPortType_emC"),
    
    /**Any input used in Tinit, for initialization. May be numbers, bus or handle (as aggregation). */ 
    inpInit(false, "mInputInit_Entry_DefPortType_emC"),
    
    /**Any input used in Tstep, may be numbers, bus or handle (as association). */
    inpStep(false, "mInputStep_Entry_DefPortType_emC"),
    
    /**Any input in another Tstep time then the Tstep. Numbers, bus or handle (as association).*/
    inpStep2(false, "mInputStep2_Entry_DefPortType_emC"),
    
    /**Any output for other initializations, maybe a handle (aggregation) or also calculated number from Tinit or parameter inputs. */
    outInit(false, "mOutputInit_Entry_DefPortType_emC"),
    
    /**Any output in the step time. */
    outStep(false, "mOutputStep_Entry_DefPortType_emC"),
    
    /**Any output from other than the main Tstep time. */
    outStep2(false, "mOutputStep2_Entry_DefPortType_emC"),
    
    /**Any parameter, for parameter dialog in Simulink. Tunable. Input in step operations. */
    tunableParam(true, "tunable"),
    
    /**Any parameter non tunable, used in Ctor and init. */
    fixParam(true, "non-tunable"),
    
    /**Parameter value for the Tstep time. Determines the step time if given (non INHERITED by wiring). 
     * It may be used or not for operations. */
    tstepParam(true, "Tstep, non tunable");
    
    final String sEnum_SetDefPortTypes;
    final boolean bParam;
    WhatPort(boolean bParam, String sEnum_SetDefPortTypes) {
      this.bParam = bParam;
      this.sEnum_SetDefPortTypes = sEnum_SetDefPortTypes;
    }
  }
  
  
  
  /**Description of one port of parameter. Values are used in JZtxtcmd.  */
  public static class ZbnfPort {
    
    /**The argument of operation from header parser. */
    final CheaderParser.AttributeOrTypedef zbnfArg;
    
    /**The name of the port, usual same as {@link CheaderParser.AttributeOrTypedef#name}*/ 
    final String name;
    
    /**A associated step time independent of the definition of a value. 
     * It is "Tinit" or "Tstep" or "Tstep1..." accordingly to the operation. */
    final String tstep;
    
    /**It is set from {@link #whatis}: {@link WhatPort#sEnum_SetDefPortTypes}. */
    final String sEnum_SetDefPortTypes;
    
    /**The number of port or parameter starting from 0 independent for in, out and param. */
    final int nr;
    
    /**Characteristic of the port or parameter. */
    final WhatPort whatis;
    
    public ZbnfPort(CheaderParser.AttributeOrTypedef zbnfArg, String name, String tstep, WhatPort whatis, int nr) {
      this.zbnfArg = zbnfArg;
      this.name = name;
      this.tstep = tstep;
      this.whatis = whatis;
      this.sEnum_SetDefPortTypes = whatis.sEnum_SetDefPortTypes;
      this.nr = nr;
    }
    
    @Override public String toString() { return this.name + " @" + this.nr + this.tstep; }

  }
  
  
  
  
  /**The content of this class is used in the JZtxtcmd script to generate SFunction wrapper.c code and tlc code.
   * The content is prepared accordingly to the necessities of the jzTc generation scripts. 
   *
   */
  public static class ZbnfFB {
    
    /**Name of the SFunction of the FBlock. */
    public String name;
    
    /**null if not ObjectJc-based, else the path from thiz to ObjectJc instance. */
    public String sBasedOnObject;
    
    /**Decides whether ObjectJc or the instance is used as first argument in ctor TODO used? maybe name othiz/thiz decides.*/
    public boolean bObject;
    
    /**maybe needless meanwhile, because {@link #thizAttr}*/
    public boolean bStatic = true;
    
    /**Type of thiz, remain null for static. */
    public CheaderParser.AttributeOrTypedef thizAttr;
    
    /**A FBlock with only one step time. Simulink: Block base sample times.
     * The step time may be explicitly by param Tstep or inherit from in/outputs. */
    public boolean isFBstep;
    
    
    /**All input ports in order. */
    final List<ZbnfPort> inPorts = new LinkedList<ZbnfPort>();
    
    /**All output ports in order. */
    final List<ZbnfPort> outPorts = new LinkedList<ZbnfPort>();
    
    /**All params in order. */
    final List<ZbnfPort> paramPorts = new LinkedList<ZbnfPort>();

    /**Sorted ports incl. parameter. */
    final Map<String, ZbnfPort> allArgsIx = new TreeMap<String, ZbnfPort>();
    
    

    
    
    
    
    
    public boolean busInputCheck;  //if any argument _bus, _ybus, _cbus, _ycbus are found, set it to generate check code.
    public boolean busInputGather;
    public boolean busOutputCheck;
    public boolean busOutputGather;
    public boolean bVarg = false;         //set on vaarg in init, step. Then return.ixPworkFBinfo should be >=0 

    public int ixParamTstep = -1;  //index of the Tstep parameter in ctor, -1: not given
    public int ixParamTstep2 = -1; //~same for Tstep2
    public int ixParamStep = 0;    //index of the first parameter for Tstep routine
    public int ixParamUpd = 0;             
    public int ixParamStep2 = 0;   //~
    public int ixParamInit = 0;
    public int ixParamCtor = 0;
    public int ixInputStep = 0;    //index of first input for step An step-in may be before. It is 0 or 1.
    public int ixInputUpd = 0;
    public int ixInputStep2 = 0; 
    public int ixInputInit = 0; 
    public int ixInputThiz = -1; 
    public int ixOutputStep = 0;
    public int ixOutputStep2 = 0;
    public int ixOutputInit = 0;
    public int ixOutputVarg = 0;
    
    public int ixOutputThizStep = -1;
    public int ixOutputThizInit = -1;
    public int nrofParamsNoTunable = 0;
    public int nrofParamsTunable = 0;
    public int nrofParams = 0;
    public int nrofInputs = 0;                                        
    public int nrofOutputs = 0;
    public int nrofPorts = 0;                      
    public int nrofPortsMax = 64;  //additional possible variable port number
    public StringBuilder paramBitsTunable = new StringBuilder("0");
    public int bitsParamTunable = 0;
    public Map<String, ZbnfPort> paramsNoTunable = new TreeMap<String, ZbnfPort>();   //all names in order of the ix using for ssGetSFcnParam(simstruct, <&ixParams>)

    public int ixDworkThiz = -1;
    public int ixDworkBus = -1;
    public int nrofDwork = 0;
    public int ixPworkFBinfo = -1;
    public int nrofPwork = 0;
    public int ixBusInfo = 0;

    /**Used to include the reflection info. */
    List<String> busTypes;
    
    /**Reference to the CHeaderParser operations. The arguments with types are stored there too. */
    ZbnfOpData dataCtor, dataDtor, dataInit, dataTlcParam, dataDPorts, dataUpd, dataOp;
    
    /**Reference to the CHeaderParser operations. The arguments with types are stored there too. */
    CheaderParser.MethodDef ctor, dtor, init, tlcParam, dPorts, upd, op;
    
    
    /**Reference to the CHeaderParser operations for additional step times. The arguments with types are stored there too. */
    final List<ZbnfOpData> portSteps = new LinkedList<ZbnfOpData>();

    
    public ZbnfFB() { }
    
    
    /**Check the arguments (from header parser) of an operation. 
     * <ul>
     * <li>thiz first occurrence used to set #thizArg. Detect wheter based on Object: {@link #sBasedOnObject}.
     *   If all operations have not a thiz (not expected) then the return type of the ctor determins {@link #thizAttr}.
     * <li>othiz First argument of the ctor. Set {@link #bObject}:
     * <li>TODO further document   
     * </ul>
     * @param zbnfOper The operation. if null, does nothing
     */
    public void checkArgs(ZbnfOpData zbnfOper) {
      if(zbnfOper == null) return;
      CheaderParser.MethodDef zbnfOp = zbnfOper.zbnfOp;
      for(CheaderParser.AttributeOrTypedef arg : zbnfOp.args) {
        String name = arg.name;
        if( arg.name.endsWith("_bus") || arg.name.endsWith("_ybus") ) {
          if(this.busTypes == null) { this.busTypes = new LinkedList<String>(); }
          this.busTypes.add(arg.type.basename);
        }
        if(name.equals("thiz")) {
          if(this.thizAttr == null) { 
            this.thizAttr = arg;
            this.bStatic = false;
            this.sBasedOnObject = arg.type.typeClass().sBasedOnObjectJc;
        } }
        else if(name.equals("othiz")) {
          this.bStatic = false; this.bObject = true;
        }
        else if(name.equals("Tstep")) {
          if(this.allArgsIx.get(name) ==null) {
            ZbnfPort zbnfPort = new ZbnfPort(arg, name, zbnfOper.steptime, WhatPort.tstepParam, this.nrofParams ++);
            this.paramPorts.add(zbnfPort);
            this.paramsNoTunable.put(name, zbnfPort);
            this.allArgsIx.put(name, zbnfPort);
            this.ixParamTstep = zbnfPort.nr;
            this.nrofParamsNoTunable +=1;
          }
        }
        else if( arg.name.startsWith("vport_")) {          // TODO docu
          this.bVarg = true;
          if(arg.arraysize !=null && arg.arraysize.value !=0) {
            this.nrofPortsMax = this.nrofPortsMax + arg.arraysize.value;
          } else { this.nrofPortsMax = this.nrofPortsMax + 1; }
        }
        else if( arg.name.startsWith("_zVaargs_")) {
          this.bVarg = true;
        }
        else if( arg.name.equals("_simtime")) {
          //do nothing.
        } 
        else if(name.endsWith("_y") || name.endsWith("_ybus")) {
          //name = name.substring(0, name.length()-2);
          if(this.allArgsIx.get(name) ==null) {
            WhatPort whatis = zbnfOper.whatisit.bInit ? WhatPort.outInit : WhatPort.outStep;
            ZbnfPort zbnfPort = new ZbnfPort(arg, name, zbnfOper.steptime, whatis, this.nrofOutputs ++);
            this.outPorts.add(zbnfPort);
            this.allArgsIx.put(name, zbnfPort);
          }
        }
        else if(name.endsWith("_param")) {
          //name = name.substring(0, name.length()-6);
          if(this.allArgsIx.get(name) ==null) {
            boolean bTunable = zbnfOper.whatisit.bParamIsTunable && !arg.type.name.equals("StringJc");
            WhatPort whatis = bTunable ? WhatPort.tunableParam : WhatPort.fixParam;
            ZbnfPort zbnfPort = new ZbnfPort(arg, name, zbnfOper.steptime, whatis, this.nrofParams ++);
            this.paramPorts.add(zbnfPort);
            this.allArgsIx.put(name, zbnfPort);
            if(bTunable) {
              this.bitsParamTunable |= 1 << zbnfPort.nr;
              this.nrofParamsTunable +=1;
            } else {
              this.nrofParamsNoTunable +=1;
              this.paramsNoTunable.put(name, zbnfPort);
            }
          }
        }
        else if( arg.type.basename.equals("DefPortTypes_emC")  || arg.type.basename.equals("EDefPortTypes_emC")) {
          if(this.ixPworkFBinfo == -1) {                   // it is a special argument TODO docu
            this.ixPworkFBinfo = this.nrofPwork; 
            this.nrofPwork = this.nrofPwork +1; 
          }  
        }  
        else if(zbnfOper.whatisit.bArgIsNonTunableParam) {
          if(this.allArgsIx.get(name) ==null) {
            ZbnfPort zbnfPort = new ZbnfPort(arg, name, zbnfOper.steptime, WhatPort.fixParam, this.nrofParams ++);
            this.paramPorts.add(zbnfPort);
            this.allArgsIx.put(name, zbnfPort);
            this.nrofParamsNoTunable +=1;
            this.paramsNoTunable.put(name, zbnfPort);
          }
        }
        else {
          if(this.allArgsIx.get(name) ==null) {
            WhatPort whatis = zbnfOper.whatisit.bInit ? WhatPort.inpInit : WhatPort.inpStep;
            ZbnfPort zbnfPort = new ZbnfPort(arg, name, zbnfOper.steptime, whatis, this.nrofInputs ++);
            this.inPorts.add(zbnfPort);
            this.allArgsIx.put(name, zbnfPort);
          }
        }
      }
    }


    /**Detects all ports which are determined by the ctor, init, step operations which are associated to this Object-FB.
     * It checks all arguments from this routines. 
     * Special arguments which are not associated immediately to the ports are detected too:
     * <ul>
     * <li>
     * </ul> 
     * The type of the Object-FB is detected by the first 'thiz' argument of the given {@link #op} operation 
     * (only as fallback from the return type of ctor). 
     */
    public void prepareObjectFB() {
      CheaderParser.AttributeOrTypedef thizArg = this.op.args.get(0);  //The first arg should be this. 
      if(thizArg.name.equals("thiz")) {                  //not thiz as first arg, then static.
        this.thizAttr = thizArg;
      } else {
        if(this.thizAttr == null && this.ctor.type !=null) { // return type of ctor is default the type of FB
          this.thizAttr = new CheaderParser.AttributeOrTypedef("return");
          this.thizAttr.type = this.ctor.type;
          this.thizAttr.name = "return";                         // elsewhere it needs an argument thiz for the type.
        }                                                        // or it is static, without thiz (Operation-FB) 
      }
      if(this.op.description.simulinkTag.contains("fnCallTrg")) {
        String name = "trg";
        ZbnfPort zbnfPort = new ZbnfPort(this.thizAttr, name, "Tstep", WhatPort.fnCallTrgOut, this.nrofOutputs ++);
        this.outPorts.add(zbnfPort);
        this.allArgsIx.put(name, zbnfPort);
      }
      if(this.op.description.simulinkTag.contains("step-in")) {
        String name = "step-in";
        ZbnfPort zbnfPort = new ZbnfPort(null, name, "Tstep", WhatPort.stepIn, this.nrofInputs ++);
        this.inPorts.add(zbnfPort);
        this.allArgsIx.put(name, zbnfPort);
      }
      if(this.op.description.simulinkTag.contains("step-out")) {
        String name = "step-out";
        ZbnfPort zbnfPort = new ZbnfPort(null, name, "Tstep", WhatPort.stepOut, this.nrofOutputs ++);
        this.outPorts.add(zbnfPort);
        this.allArgsIx.put(name, zbnfPort);
      }
      
      
      this.ixInputStep = this.nrofInputs;
      this.ixOutputStep = this.nrofOutputs;
      this.ixParamStep = this.nrofParams;
      checkArgs(this.dataOp);            // build ports & params, firstly from Object-FB
      
      this.ixInputUpd = this.nrofInputs;
      this.ixParamUpd = this.nrofParams;
      checkArgs(this.dataUpd);           // build ports & params, from update
      
      this.ixInputStep2 = this.nrofInputs;
      this.ixOutputStep2 = this.nrofOutputs;
      this.ixParamStep2 = this.nrofParams;
      for(ZbnfOpData portStep : this.portSteps) {
        checkArgs(portStep);               // build ports & params, from all other operations
      }
      
      this.ixInputInit = this.nrofInputs;
      this.ixOutputInit = this.nrofOutputs;
      this.ixParamInit = this.nrofParams;
      if(this.init !=null) {
        checkArgs(this.dataInit);            // build ports & params, from init
      }
      
      this.ixOutputVarg = this.nrofOutputs;
      this.ixParamCtor = this.nrofParams;
      if(this.ctor !=null) {
        checkArgs(this.dataCtor);            // build params, from ctor
      }
      this.ixDworkThiz = 0;
      this.nrofDwork = 1;
    
      if(this.ctor == null && !this.bStatic) {
        String name = "thiz";
        ZbnfPort zbnfPort = new ZbnfPort(null, name, "Tstep", WhatPort.thisIn, this.nrofInputs ++);
        this.ixInputThiz = zbnfPort.nr;
        this.inPorts.add(zbnfPort);
        this.allArgsIx.put(name, zbnfPort);
      }
      
      if(this.ctor != null && !this.op.description.simulinkTag.contains("no-thizStep")) {
        String name = "thizo";
        ZbnfPort zbnfPort = new ZbnfPort(this.thizAttr, name, "Tstep", WhatPort.thisOutStep, this.nrofOutputs ++);
        this.ixOutputThizStep = zbnfPort.nr;
        this.outPorts.add(zbnfPort);
        this.allArgsIx.put(name, zbnfPort);
      }
      if(this.ctor != null && !this.op.description.simulinkTag.contains("no-thizInit")) {
        String name = "ithizo";
        ZbnfPort zbnfPort = new ZbnfPort(this.thizAttr, name, "Tinit", WhatPort.thisOutInit, this.nrofOutputs ++);
        this.ixOutputThizInit = zbnfPort.nr;
        this.outPorts.add(zbnfPort);
        this.allArgsIx.put(name, zbnfPort);
      }
      this.nrofPorts = this.nrofInputs + this.nrofOutputs;
    
    }


    void prepareOperationsFB ( CheaderParser.MethodDef zbnfOp) {
      this.name = zbnfOp.name;
      this.dataOp = new ZbnfOpData(zbnfOp, "Tstep", Whatisit.oper);
      this.op = zbnfOp;
      if(this.name.equals("addObj_DataNode_Inspc"))
        Debugutil.stop();
      checkArgs(this.dataOp);          // build ports & params, firstly from Object-FB
      if(this.thizAttr !=null) {
        String name = "thiz";
        ZbnfPort zbnfPort = new ZbnfPort(this.thizAttr, name, "Tstep", WhatPort.thisIn, this.nrofInputs ++);
        this.inPorts.add(zbnfPort);
        this.allArgsIx.put(name, zbnfPort);
        this.ixInputThiz = zbnfPort.nr;
      }
      this.ixInputInit = this.ixInputUpd = this.ixInputStep2 = this.nrofInputs;
      this.ixOutputInit = this.ixOutputStep2 = this.ixOutputVarg = this.nrofOutputs;
      this.ixParamInit = this.ixParamUpd = this.ixParamStep2 = this.ixParamCtor = this.nrofParams;
      this.isFBstep = true;
      this.ixDworkThiz = -1;
      this.nrofDwork = 0;
      this.nrofPorts = this.nrofInputs + this.nrofOutputs;
      
    }
    
    @Override public String toString() { return this.name; }
  }
  
  
  
  
  /**It describes the kind of Operation.
   */
  enum Whatisit {
    ctor(true, true, false),
    dtor(false, false, false), 
    init(true, false, false),
    oper(false, false, true),
    defTcl(false, false, false),
    defPortTypes(false, false, false);
    final boolean bArgIsNonTunableParam;
    final boolean bParamIsTunable;
    final boolean bInit;
    
    Whatisit( boolean bInit, boolean bArgParam, boolean bParamTunable) {
      this.bInit = bInit;
      this.bArgIsNonTunableParam = bArgParam;
      this.bParamIsTunable = bParamTunable;
    }
  }
  
  
  
  /**This opeation is called from the JZtxtcmd generation script after parsing. 
   * 
   * @param parseResult all parsed files
   * @return list of prepared informations.
   */
  public static List<ZbnfFB> analyseOperations(CheaderParser.ZbnfResultData parseResult) {
    List<ZbnfFB> fblocks = new LinkedList<ZbnfFB>();
    
    ZbnfFB zfb = new ZbnfFB();
    
    int nStep = 0;
    
    for(CheaderParser.ZbnfResultFile headerfile: parseResult.files){
      for(CheaderParser.ClassC classC: headerfile.listClassC) {
        for(CheaderParser.HeaderBlockEntry entry: classC.entries) {
          if(entry instanceof CheaderParser.StructDefinition) { //  .whatisit == "structDefinition") {
            CheaderParser.StructDefinition zbnfStruct = (CheaderParser.StructDefinition)entry; 
            zfb = new ZbnfFB();                       // new instance ZbnfFB after a struct definition 
            zfb.sBasedOnObject = zbnfStruct.sBasedOnObjectJc;     //String to ObjectJc part
          }
          else if(entry instanceof CheaderParser.MethodDef) {
            CheaderParser.MethodDef zbnfOp = (CheaderParser.MethodDef)entry; 
            if(entry.description !=null && entry.description.simulinkTag !=null) {
              String simulinkTag = entry.description.simulinkTag;
              if(simulinkTag.contains("ctor")) {
                zfb.dataCtor = new ZbnfOpData(zbnfOp, "!", Whatisit.ctor);
                zfb.ctor = zbnfOp;
                zfb.dPorts = null;                         // all other operations get invalid on a new ctor
                zfb.tlcParam = null;
                zfb.init = null;
                zfb.upd = null;
                zfb.portSteps.clear();
              }
              else if(simulinkTag.contains("dtor")) {
                zfb.dataDtor = new ZbnfOpData(zbnfOp, "~", Whatisit.dtor);
                zfb.dtor = zbnfOp;
                }
              else if(simulinkTag.contains("init")) {
                zfb.dataInit = new ZbnfOpData(zbnfOp, "Tinit", Whatisit.init);
                zfb.init = zbnfOp;
                zfb.upd = null;                            // the other routines to determine an Object-FB should follow.
                zfb.dataUpd = null;                        // routines before are invalid. 
                zfb.portSteps.clear();
                //zfb.dPorts = null;                       //this routines should be valid furthermore
                //zfb.dataDPorts = null;
                //zfb.tlcParam = null;
                //zfb.dataTlcParam = null;
              } 
              else if(simulinkTag.contains("update")) {
                zfb.dataUpd = new ZbnfOpData(zbnfOp, "+Tstep", Whatisit.oper);
                zfb.upd = zbnfOp;
              } 
              else if(simulinkTag.contains("defTlcParam")) {
                zfb.dataTlcParam = new ZbnfOpData(zbnfOp, "%", Whatisit.defTcl);
                zfb.tlcParam = zbnfOp;
              } 
              else if(simulinkTag.contains("defPortTypes")) {
                zfb.dataDPorts = new ZbnfOpData(zbnfOp, "@", Whatisit.defPortTypes);
                zfb.dPorts = zbnfOp;
              } 
              else if(simulinkTag.contains("portStep") || simulinkTag.contains("step2")) {
                zfb.portSteps.add(new ZbnfOpData(zbnfOp, "Tstep" + Integer.toString(++nStep), Whatisit.oper));
              } 
              else if(simulinkTag.contains("Operation-FB")) {
                //                                           // yet a new FB is found as Operation-FB
                ZbnfFB zfbOp = new ZbnfFB();   // write it to a new ZbnfFB
                zfbOp.prepareOperationsFB(zbnfOp);
                fblocks.add(zfbOp);
                //                                           // the current zfb remain active
              } 
              else if(simulinkTag.contains("Object-FB")) {
                zfb.dataOp = new ZbnfOpData(zbnfOp, "Tstep", Whatisit.oper);
                zfb.op = zbnfOp;
                zfb.name = zbnfOp.name;                   // yet a new FB is found as Object-FB
                if(zfb.name.equals("param_PIDf_Ctrl_emC"))
                  Debugutil.stop();
                zfb.prepareObjectFB();
                fblocks.add(zfb);
                ZbnfFB zfbnew = new ZbnfFB();
                zfbnew.sBasedOnObject = zfb.sBasedOnObject;
                zfbnew.dataCtor = zfb.dataCtor;                    // ctor, init etc. remain valid for the next ObjectFB
                zfbnew.dataDtor = zfb.dataDtor;
                zfbnew.dataInit = zfb.dataInit;
                zfbnew.dataDPorts = zfb.dataDPorts;
                zfbnew.dataTlcParam = zfb.dataTlcParam;
                zfbnew.ctor = zfb.ctor;                    // ctor, init etc. remain valid for the next ObjectFB
                zfbnew.dtor = zfb.dtor;
                zfbnew.init = zfb.init;
                zfbnew.dPorts = zfb.dPorts;
                zfbnew.tlcParam = zfb.tlcParam;
                zfb = zfbnew;                              // further usage in new Operation
              } 
          } }
    } } } 
    
    return fblocks;
  }
  
}
