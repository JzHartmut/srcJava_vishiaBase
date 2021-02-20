package org.vishia.fblock;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.vishia.header2Reflection.CheaderParser;
import org.vishia.header2Reflection.CheaderParser.AttributeOrTypedef;
import org.vishia.header2Reflection.CheaderParser.ZbnfResultData;

/**This class supports generation of Simulink S-Functions from header files. 
 * 
 * @author hartmut Schorrig
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
  
  
  
  public static class ZbnfPort {
    final CheaderParser.AttributeOrTypedef zbnfArg;
    final String name;
    final String tstep;
    final int nr;
    
    public ZbnfPort(AttributeOrTypedef zbnfArg, String name, String tstep, int nr) {
      this.zbnfArg = zbnfArg;
      this.name = name;
      this.tstep = tstep;
      this.nr = nr;
    }
    
    @Override public String toString() { return this.name + " @" + this.nr + this.tstep; }

  }
  
  
  
  
  public static class ZbnfFB {
    
    public String name;
    
    public String sBasedOnObject;
    
    public boolean bObject;
    
    public boolean bStatic = true;
    
    /**A FBlock with only one step time. Simulink: Block bases.
     * The step time may be explicitly by param Tstep or inherit from in/outputs. */
    public boolean isFBstep;
    
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


    
    
    final List<ZbnfPort> inPorts = new LinkedList<ZbnfPort>();
    
    final Map<String, ZbnfPort> allArgsInIx = new TreeMap<String, ZbnfPort>();
    
    final List<ZbnfPort> outPorts = new LinkedList<ZbnfPort>();
    
    final Map<String, ZbnfPort> allArgsOutIx = new TreeMap<String, ZbnfPort>();
    
    final Map<String, ZbnfPort> allArgsIx = new TreeMap<String, ZbnfPort>();
    
    final List<ZbnfPort> paramPorts = new LinkedList<ZbnfPort>();
    
    final Map<String, ZbnfPort> paramPortIx = new TreeMap<String, ZbnfPort>();
    
    
    ZbnfOpData dataCtor, dataDtor, dataInit, dataTlcParam, dataDPorts, dataUpd, dataOp;
    
    CheaderParser.MethodDef ctor, dtor, init, tlcParam, dPorts, upd, op;
    
    final List<ZbnfOpData> operations = new LinkedList<ZbnfOpData>();

    
    public ZbnfFB() { }
    
    @Override public String toString() { return this.name; }
  }
  
  
  
  
  enum Whatisit {
    ctor(true, false),
    dtor(false, false), 
    init(false, false),
    oper(false, true),
    defTcl(false, false),
    defPortTypes(false, false);
    final boolean bArgIsNonTunableParam;
    final boolean bParamIsTunable;
    
    Whatisit( boolean bArgParam, boolean bParamTunable) {
      this.bArgIsNonTunableParam = bArgParam;
      this.bParamIsTunable = bParamTunable;
    }
  }
  
  
  
  
  /**
   * @param zbnfFB
   * @param zbnfOper if null, does nothing
   */
  public static void checkArgs(ZbnfFB zbnfFB, ZbnfOpData zbnfOper) {
    if(zbnfOper == null) return;
    CheaderParser.MethodDef zbnfOp = zbnfOper.zbnfOp;
    for(CheaderParser.AttributeOrTypedef arg : zbnfOp.args) {
      String name = arg.name;
      if(name.equals("thiz")) {
        zbnfFB.bStatic = false;
      }
      else if(name.equals("othiz")) {
        zbnfFB.bStatic = false; zbnfFB.bObject = true;
      }
      else if(name.equals("Tstep")) {
        if(zbnfFB.paramPortIx.get(name) ==null) {
          ZbnfPort zbnfPort = new ZbnfPort(arg, name, zbnfOper.steptime, zbnfFB.nrofParams ++);
          zbnfFB.paramPorts.add(zbnfPort);
          zbnfFB.paramPortIx.put(name, zbnfPort);
          zbnfFB.paramsNoTunable.put(name, zbnfPort);
          zbnfFB.allArgsIx.put(name, zbnfPort);
          zbnfFB.ixParamStep = zbnfPort.nr;
          zbnfFB.nrofParamsNoTunable +=1;
        }
      }
      else if(name.endsWith("_y")) {
        name = name.substring(0, name.length()-2);
        if(zbnfFB.allArgsOutIx.get(name) ==null) {
          ZbnfPort zbnfPort = new ZbnfPort(arg, name, zbnfOper.steptime, zbnfFB.nrofOutputs ++);
          zbnfFB.outPorts.add(zbnfPort);
          zbnfFB.allArgsOutIx.put(name, zbnfPort);
          zbnfFB.allArgsIx.put(name, zbnfPort);
        }
      }
      else if(name.endsWith("_param")) {
        name = name.substring(0, name.length()-6);
        if(zbnfFB.paramPortIx.get(name) ==null) {
          ZbnfPort zbnfPort = new ZbnfPort(arg, name, zbnfOper.steptime, zbnfFB.nrofParams ++);
          zbnfFB.paramPorts.add(zbnfPort);
          zbnfFB.paramPortIx.put(name, zbnfPort);
          zbnfFB.allArgsIx.put(name, zbnfPort);
          if(zbnfOper.whatisit.bParamIsTunable) {
            zbnfFB.bitsParamTunable |= 1 << zbnfPort.nr;
            zbnfFB.nrofParamsTunable +=1;
          } else {
            zbnfFB.nrofParamsNoTunable +=1;
          }
        }
      }
      else if(zbnfOper.whatisit.bArgIsNonTunableParam) {
        if(zbnfFB.paramPortIx.get(name) ==null) {
          ZbnfPort zbnfPort = new ZbnfPort(arg, name, zbnfOper.steptime, zbnfFB.nrofParams ++);
          zbnfFB.paramPorts.add(zbnfPort);
          zbnfFB.paramPortIx.put(name, zbnfPort);
          zbnfFB.allArgsIx.put(name, zbnfPort);
          zbnfFB.nrofParamsNoTunable +=1;
        }
      }
      else {
        if(zbnfFB.allArgsInIx.get(name) ==null) {
          ZbnfPort zbnfPort = new ZbnfPort(arg, name, zbnfOper.steptime, zbnfFB.nrofInputs ++);
          zbnfFB.inPorts.add(zbnfPort);
          zbnfFB.allArgsInIx.put(name, zbnfPort);
          zbnfFB.allArgsIx.put(name, zbnfPort);
        }
      }
    }
  }

  
  
  
  
  public static void checkArgsObjectFB(ZbnfFB zbnfFB) {
    if(zbnfFB.op.description.simulinkTag.contains("step-in")) {
      String name = "step-in";
      ZbnfPort zbnfPort = new ZbnfPort(null, name, "Tstep", zbnfFB.nrofInputs ++);
      zbnfFB.inPorts.add(zbnfPort);
      zbnfFB.allArgsInIx.put(name, zbnfPort);
    }
    if(zbnfFB.op.description.simulinkTag.contains("step-out")) {
      String name = "step-out";
      ZbnfPort zbnfPort = new ZbnfPort(null, name, "Tstep", zbnfFB.nrofOutputs ++);
      zbnfFB.outPorts.add(zbnfPort);
      zbnfFB.allArgsOutIx.put(name, zbnfPort);
    }
    
    
    zbnfFB.ixInputStep = zbnfFB.nrofInputs;
    zbnfFB.ixOutputStep = zbnfFB.nrofOutputs;
    zbnfFB.ixParamStep = zbnfFB.nrofParams;
    checkArgs(zbnfFB, zbnfFB.dataOp);            // build ports & params, firstly from Object-FB
    
    zbnfFB.ixInputUpd = zbnfFB.nrofInputs;
    zbnfFB.ixParamUpd = zbnfFB.nrofParams;
    checkArgs(zbnfFB, zbnfFB.dataUpd);           // build ports & params, from update
    
    zbnfFB.ixInputStep2 = zbnfFB.nrofInputs;
    zbnfFB.ixOutputStep2 = zbnfFB.nrofOutputs;
    zbnfFB.ixParamStep2 = zbnfFB.nrofParams;
    for(ZbnfOpData zbnfOper : zbnfFB.operations) {
      checkArgs(zbnfFB, zbnfOper);               // build ports & params, from all other operations
    }
    
    zbnfFB.ixInputInit = zbnfFB.nrofInputs;
    zbnfFB.ixOutputInit = zbnfFB.nrofOutputs;
    zbnfFB.ixParamInit = zbnfFB.nrofParams;
    if(zbnfFB.init !=null) {
      checkArgs(zbnfFB, zbnfFB.dataInit);            // build ports & params, from init
    }
    zbnfFB.ixParamCtor = zbnfFB.nrofParams;
    if(zbnfFB.ctor !=null) {
      checkArgs(zbnfFB, zbnfFB.dataCtor);            // build params, from ctor
    }
    zbnfFB.ixDworkThiz = 0;
    zbnfFB.nrofDwork = 1;

    if(zbnfFB.ctor == null && !zbnfFB.bStatic) {
      String name = "thiz";
      ZbnfPort zbnfPort = new ZbnfPort(null, name, "Tstep", zbnfFB.nrofInputs ++);
      zbnfFB.ixInputThiz = zbnfPort.nr;
      zbnfFB.inPorts.add(zbnfPort);
      zbnfFB.allArgsInIx.put(name, zbnfPort);
    }
    if(zbnfFB.ctor != null && !zbnfFB.op.description.simulinkTag.contains("no-thizStep")) {
      String name = "thizo";
      ZbnfPort zbnfPort = new ZbnfPort(null, name, "Tstep", zbnfFB.nrofOutputs ++);
      zbnfFB.ixOutputThizStep = zbnfPort.nr;
      zbnfFB.outPorts.add(zbnfPort);
      zbnfFB.allArgsOutIx.put(name, zbnfPort);
    }
    if(zbnfFB.ctor != null && !zbnfFB.op.description.simulinkTag.contains("no-thizInit")) {
      String name = "ithizo";
      ZbnfPort zbnfPort = new ZbnfPort(null, name, "Tinit", zbnfFB.nrofOutputs ++);
      zbnfFB.ixOutputThizInit = zbnfPort.nr;
      zbnfFB.outPorts.add(zbnfPort);
      zbnfFB.allArgsOutIx.put(name, zbnfPort);
    }

  
  }
  
  
  
  public static List<ZbnfFB> analyseOperations(ZbnfResultData parseResult) {
    List<ZbnfFB> fblocks = new LinkedList<ZbnfFB>();
    
    ZbnfFB zbnfFB = new ZbnfFB();
    
    int nStep = 0;
    
    for(CheaderParser.ZbnfResultFile headerfile: parseResult.files){
      for(CheaderParser.ClassC classC: headerfile.listClassC) {
        for(CheaderParser.HeaderBlockEntry entry: classC.entries) {
          if(entry instanceof CheaderParser.StructDefinition) { //  .whatisit == "structDefinition") {
            CheaderParser.StructDefinition zbnfStruct = (CheaderParser.StructDefinition)entry; 
            zbnfFB = new ZbnfFB();                       // new instance ZbnfFB after a struct definition 
            zbnfFB.sBasedOnObject = zbnfStruct.sBasedOnObjectJc;     //String to ObjectJc part
          }
          else if(entry instanceof CheaderParser.MethodDef) {
            CheaderParser.MethodDef zbnfOp = (CheaderParser.MethodDef)entry; 
            if(entry.description !=null && entry.description.simulinkTag !=null) {
              String simulinkTag = entry.description.simulinkTag;
              if(simulinkTag.contains("ctor")) {
                zbnfFB.dataCtor = new ZbnfOpData(zbnfOp, "!", Whatisit.ctor);
                zbnfFB.ctor = zbnfOp;
              }
              else if(simulinkTag.contains("dtor")) {
                zbnfFB.dataDtor = new ZbnfOpData(zbnfOp, "~", Whatisit.dtor);
                zbnfFB.dtor = zbnfOp;
                }
              else if(simulinkTag.contains("init")) {
                zbnfFB.dataInit = new ZbnfOpData(zbnfOp, "Tinit", Whatisit.init);
                zbnfFB.init = zbnfOp;
              } 
              else if(simulinkTag.contains("update")) {
                zbnfFB.dataUpd = new ZbnfOpData(zbnfOp, "+Tstep", Whatisit.oper);
                zbnfFB.upd = zbnfOp;
              } 
              else if(simulinkTag.contains("defTlcParam")) {
                zbnfFB.dataTlcParam = new ZbnfOpData(zbnfOp, "%", Whatisit.defTcl);
                zbnfFB.tlcParam = zbnfOp;
              } 
              else if(simulinkTag.contains("defPortTypes")) {
                zbnfFB.dataDPorts = new ZbnfOpData(zbnfOp, "@", Whatisit.defPortTypes);
                zbnfFB.dPorts = zbnfOp;
              } 
              else if(simulinkTag.contains("PortStep-FB") || simulinkTag.contains("step2")) {
                zbnfFB.operations.add(new ZbnfOpData(zbnfOp, "Tstep" + Integer.toString(++nStep), Whatisit.oper));
              } 
              else if(simulinkTag.contains("Operation-FB")) {
                //                                           // yet a new FB is found as Operation-FB
                ZbnfFB zbnfOperationFB = new ZbnfFB();   // write it to a new ZbnfFB
                zbnfOperationFB.name = zbnfOp.name;
                zbnfOperationFB.sBasedOnObject = zbnfFB.sBasedOnObject;
                zbnfOperationFB.dataOp = new ZbnfOpData(zbnfOp, "Tstep", Whatisit.oper);
                zbnfOperationFB.op = zbnfOp;
                checkArgs(zbnfOperationFB, zbnfOperationFB.dataOp);          // build ports & params, firstly from Object-FB
                zbnfFB.ixDworkThiz = -1;
                zbnfFB.nrofDwork = 0;
                fblocks.add(zbnfOperationFB);
                //                                           // the current zbnfFB remain active
              } 
              else if(simulinkTag.contains("Object-FB")) {
                zbnfFB.dataOp = new ZbnfOpData(zbnfOp, "Tstep", Whatisit.oper);
                zbnfFB.op = zbnfOp;
                zbnfFB.name = zbnfOp.name;                   // yet a new FB is found as Object-FB
                checkArgsObjectFB(zbnfFB);
                fblocks.add(zbnfFB);
                ZbnfFB zbnfFBnew = new ZbnfFB();
                zbnfFBnew.sBasedOnObject = zbnfFB.sBasedOnObject;
                zbnfFBnew.dataCtor = zbnfFB.dataCtor;                    // ctor, init etc. remain valid for the next ObjectFB
                zbnfFBnew.dataDtor = zbnfFB.dataDtor;
                zbnfFBnew.dataInit = zbnfFB.dataInit;
                zbnfFBnew.dataDPorts = zbnfFB.dataDPorts;
                zbnfFBnew.dataTlcParam = zbnfFB.dataTlcParam;
                zbnfFBnew.ctor = zbnfFB.ctor;                    // ctor, init etc. remain valid for the next ObjectFB
                zbnfFBnew.dtor = zbnfFB.dtor;
                zbnfFBnew.init = zbnfFB.init;
                zbnfFBnew.dPorts = zbnfFB.dPorts;
                zbnfFBnew.tlcParam = zbnfFB.tlcParam;
                zbnfFB = zbnfFBnew;                              // further usage in new Operation
              } 
          } }
    } } } 
    
    return fblocks;
  }
  
}
