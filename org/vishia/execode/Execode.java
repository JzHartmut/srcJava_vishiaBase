package org.vishia.execode;

import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

/**
 * It is similar but not equivalent to {@linkplain https://en.wikipedia.org/wiki/Java_bytecode_instruction_listings}
 * The primary difference is: It stores string sources used for example for code generation.
 * @author hartmut
 *
 */
public class Execode {

  
  public enum EInstruction {
    set(0x10, -1)
    , add(0x60, 1), sub(0x64, 1), mult(0x68, 1), div(0x6c, 1)
    , pow(0,1), mod(0x70, 1), neg(0x74, 0), bitneg(0x78, 0)
    , shl(0x80, 1), shr(0x88, 1), ushr(0x80, 1)
    , and(0x70, 1), or(0x70, 1), xor(0x70, 1)
    , equ(0,1), nequ(0,1), lt(0,1), le(0,1), gt(0,1), ge(0,1)
    ;
    public int ix;
    
    /**<0: push >0 pop
     * 
     */
    public final int usedStack;
    
    EInstruction(int ix, int usedStack){
      this.ix = ix;
      this.usedStack = usedStack;
    }
  }
  
  
  public enum EOperand {
    var(0), constant(1), stack(2);
    
    int ix;
    EOperand(int ix){ this.ix = ix; }
  }
  
  
  private static EInstruction[] instrTable = new EInstruction[256];
  
  public enum EXXXInstruction {
    nop       ( 0x00, false, EOperand.stack,       EInstruction.set);
//    setVar    ( 0x10, true, EOperand.var,       EBaseInstruction.set),
//    setConst  ( 0x11, true, EOperand.constant,  EBaseInstruction.set),
//    push      ( 0x12, true, EOperand.stack,     EBaseInstruction.set),
//    addVar    ( 0x60, false, EOperand.var,      EBaseInstruction.add),
//    addConst  ( 0x61, false, EOperand.constant, EBaseInstruction.add),
//    addStack  ( 0x62, false, EOperand.stack,    EBaseInstruction.add),
//    subVar    ( 0x64, false, EOperand.var,      EBaseInstruction.sub),
//    subConst  ( 0x65, false, EOperand.constant, EBaseInstruction.sub),
//    subStack  ( 0x66, false, EOperand.stack,    EBaseInstruction.sub),
//    multVar   ( 0x68, false, EOperand.var,      EBaseInstruction.mult),
//    multConst ( 0x69, false, EOperand.constant, EBaseInstruction.mult),
//    multStack ( 0x6a, false, EOperand.stack,    EBaseInstruction.mult),
//    divVar    ( 0x6c, false, EOperand.var,      EBaseInstruction.div),
//    divConst  ( 0x6d, false, EOperand.constant, EBaseInstruction.div),
//    divStack  ( 0x6e, false, EOperand.stack,    EBaseInstruction.div);
    
    int ix;
    
    boolean bSet;
    
    EOperand XXXoperand;
    
    EInstruction XXXoperator;
    
    EXXXInstruction(int ix, boolean bSet, EOperand operand, EInstruction operator){
      this.ix = ix;
      this.bSet = bSet;
      this.XXXoperand = operand;
      this.XXXoperator = operator;
      assert(instrTable[ix] == null);
//      instrTable[ix] = this;
    }
  };
  
  
  public static class Constant {
    String sConst;
    float fConst;
    double dConst;
    long lConst;
    char type;
    
    public Constant(String val) { this.sConst = val; }
    
    @Override public String toString() { return sConst; }
    
  }
  
  
  
  
  
  
  
  public static class Instruction {
    final EInstruction instr;
    final int id;
    final Constant constant;
    final EOperand operand;
    
    /**It is especially for debugging. */
    final String text;
    
    public Instruction(EInstruction instr, String constant ) {
      this.instr = instr;
      this.constant = new Constant(constant);
      this.id = -1;
      this.text = constant;
      this.operand = EOperand.constant;
    }

    public Instruction(EInstruction instr) {
      this.instr = instr;
      this.text = "";
      this.constant = null;
      this.id = -1;
      this.operand = EOperand.stack;
    }
  
    public Instruction(EInstruction instr, int id, String name) {
      this.instr = instr;
      this.id = id;
      this.constant = null;
      this.text = name;
      this.operand = EOperand.var;
    }
  
    
    @Override public String toString() { return this.text + " " + this.instr;  }
    
  }
  
  
  
  List<Instruction> code = new LinkedList<Instruction>();
  
  
  public Execode() {
//    initInstrTable();
  }
  
  
  
  
//  private void initInstrTable() {
//    if(instrTable[EInstruction.nop.ix] ==null) {
//      setInstrTable(EInstruction.nop);
//    }
//  }
//  
//  
//  private void setInstrTable(EInstruction instr) {
//    assert(instrTable[instr.ix] == null); //not used till now.
//    instrTable[instr.ix] = instr;
//  }
//  
//  public static EInstruction getInstr(EBaseInstruction baseInst, EOperand oper) {
//    EInstruction instr = instrTable[baseInst.ix + oper.ix];
//    assert(instr !=null);
//    return instr;
//  }
  
  
  public void addInstr(EInstruction instr, String val) { this.code.add(new Instruction(instr, val)); }

  public void addInstr(EInstruction instr) { this.code.add(new Instruction(instr)); }
  
  public void addInstrVariable(EInstruction instr, int idVar, String name) { this.code.add(new Instruction(instr, idVar, name)); }
  
  
  public CharSequence convertToOtx() {
    StringBuilder u = null;
    Stack<StringBuilder> uStack = new Stack<StringBuilder>(); 
    for(Instruction instr: this.code) {
      CharSequence rightOp = null; 
      if(instr.instr.usedStack ==-1) {
        if(u !=null) {
          uStack.push(u);
        }
        u = new StringBuilder();
      } else if(instr.operand == EOperand.stack) {
        rightOp = u;  //it is the top stack level or the accu
        u = uStack.pop();
        u.insert(0,  '(').append(')');
      }
      switch(instr.instr) {
      case set: u.append(""); break;
      case add: u.append(" + "); break;
      case sub: u.append(" + "); break;
      case div: u.append(" / "); break;
      case mod: u.append(" % "); break;
      case mult:u.append(" * "); break;
      case neg: u.append(" -"); break;
      case bitneg: u.append(" ~"); break;
      case pow: u.append(" ** "); break;
      case shl: u.append(" << "); break;
      case shr: u.append(" >> "); break;
      case ushr: u.append(" :>> "); break;
      case equ: u.append(" == "); break;
      case nequ: u.append(" != "); break;
      case gt: u.append(" > "); break;
      case ge: u.append(" >= "); break;
      case lt: u.append(" < "); break;
      case le: u.append(" <= "); break;
      case and: u.append(" && "); break;
      case or: u.append(" || "); break;
      case xor: u.append(" XOR "); break;
//      default: u.append("??"); break;
      }
      switch(instr.operand) {
      case constant: u.append(instr.constant); break;
      case stack: 
        u.append("(").append(rightOp).append(")"); break;
      case var: u.append(" ").append(instr.text).append(" ");  break;
      }
    }
    assert(uStack.size()==0);
    return u;
  }
  
  
  @Override public String toString() { return code.toString(); }
  
  public static void main(String[] args) {
    Execode thiz = new Execode();
    thiz.addInstr(EInstruction.set, "5");
    thiz.addInstr(EInstruction.add, "3");
    thiz.addInstr(EInstruction.set, "7");
    thiz.addInstr(EInstruction.add, "2");
    thiz.addInstr(EInstruction.mult);
    CharSequence expr = thiz.convertToOtx();
    System.out.println(expr);
  }
}
