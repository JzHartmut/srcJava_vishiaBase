package org.vishia.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**This class helps to prepare a text with data. It is the small solution for {@link org.vishia.jztxtcmd.JZtxtcmd}
 * only for text preparation of one or a few lines.
 * <br>
 * The prescript to build the text is parsed from a given String: {@link #parse(String)}.
 * It is stored in an instance of this as a list of items. Any item is marked with a value of {@link ECmd}.
 * On one of the {@link #exec(StringFormatter, Object...)} routines a text output is produced with this prescript and given data.
 * <br>
 * <b>Basic example: </b>
 * ...
 * <br>
 * @author Hartmut Schorrig
 *
 */
public class StringPreparer
{
  
  /**Version, history and license.
   * <ul>
   * <li>2019-05-08: Creation
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
   */
  public static final String version = "2019-05-12";
  
  
  //StringFormatter fm;
  
  enum ECmd{
    addString('s', "str"),
    addVar('v', "var"),
    pos('p', "pos")
    ;
    ECmd(char cmd, String sCmd){
      this.cmd = cmd; this.sCmd = sCmd;
    }
    char cmd;
    String sCmd;
    @Override public String toString() { return sCmd; }
  }
  
  class Cmd {
    ECmd cmd;
    final String str;
    int ixVar = -1;
    final int pos0, pos1;
    //abstract void exec();
    
    public Cmd(ECmd what, String str)
    { this.cmd = what;
      this.str = str;
      this.pos0 = this.pos1 = 0;
    }
    
    public Cmd(ECmd what, String str, int pos0, int pos1)
    { this.cmd = what;
      this.str = str;
      this.pos0 = pos0;
      this.pos1 = pos1;
    }
    
    
    @Override public String toString() {
      return cmd + ":" + str;
    }
    
  }
  
//  class CmdString extends Cmd {
//    public CmdString(String str, int pos0, int pos1)
//    { super(str, pos0, pos1);
//    }
//
//    public void exec() {
//      fm.add(str.substring(pos0, pos1));
//    }
//  };
//  
//  
//  
//  class CmdVar extends Cmd {
//    String sVar;
//    public CmdVar(String str)
//    { super(str, 0,0);
//      sVar = str;
//    }
//
//    public void exec() {
//      
//      fm.add(str.substring(pos0, pos1));
//    }
//  };
  
  
  
  
  
  
  
  private List<Cmd> cmds = new ArrayList<Cmd>();
  
  
  
  
  /**Instantiates for a given prescript. 
   * @param prescript 
   */
  public StringPreparer(String prescript) {
    this.parse(prescript);
  }
  
  
  private Map<String, Integer> vars = new TreeMap<String, Integer>();
  
  private int ctVar = 0;
  
  public void parse(String src) {
    int pos0 = 0;
    int pos1 = 0;
    StringPartScan sp = new StringPartScan(src);
    sp.setIgnoreWhitespaces(true);
    while(sp.length() >0) {
      sp.lento('<');
      if(sp.found()) {
        pos1 = pos0 + (int)sp.length();
        sp.fromEnd().scan().scanStart();
        if(sp.scan("<&").scanIdentifier().scan(">").scanOk()){
          String sName = sp.getLastScannedString();
          Integer ixVar = vars.get(sName);
          if(ixVar == null) {
            vars.put(sName, ctVar);  //store the variable in order of occurence
            ctVar +=1; 
          }
          storeCmd(src, pos0, pos1, 0, new Cmd(ECmd.addVar, sName));
          pos0 = (int)sp.getCurrentPosition();  //after '>'
        }
        else { //No proper cmd found:
          
        }
      }
      else { //no more '<' found:
        sp.len0end();
        storeCmd(src, pos0, pos0 + sp.length(), 0, null);
        sp.fromEnd();  //length is null then.
      }
    } //while
    sp.close();
    for(Cmd cmd: cmds) {
      if(cmd.cmd == ECmd.addVar) {
        Integer ixVar = vars.get(cmd.str);
        cmd.ixVar = ixVar;  //store the order of occurrence.
        
      }
    }
  }





  public void exec( StringFormatter fm, Map<String, Object> values) {
    for(Cmd cmd : cmds) {
      switch(cmd.cmd) {
        case addString: fm.add(cmd.str); break;
        case addVar: {
          Object val = values.get(cmd.str);
          fm.add(val.toString());
        } break;
      }
    }
  }
  
  
  
  
  
  /**Executes preparation
   * @param fm
   * @param values
   */
  public void exec( StringFormatter fm, Object ... values ) {
    int ixVal = 0;
    for(Cmd cmd : cmds) {
      switch(cmd.cmd) {
        case addString: fm.add(cmd.str); break;
        case addVar: {
          Object val = values[ixVal];
          fm.add(val.toString());
        } break;
      }
    }
  }
  
  
  /**Executes preparation
   * @param fm
   * @param values in order of first occurrence in the prescript
   * @throws IOException 
   */
  public void exec( Appendable sb, Object ... values ) throws IOException {
    //int ixVal = 0;
    for(Cmd cmd : cmds) {
      switch(cmd.cmd) {
        case addString: sb.append(cmd.str); break;
        case addVar: {
          Integer ixVar = vars.get(cmd.str);
          Object val = values[ixVar];
          sb.append(val.toString());
        } break;
      }
    }
  }
  
  
  
  
  
  
  
  private void storeCmd(String src, int pos0, int pos1, int pos2, Cmd what) {
    if(pos1 > pos0) {
      cmds.add(new Cmd(ECmd.addString, src.substring(pos0, pos1)));
    }
    if(what !=null) {
      cmds.add(what);
    }
  }
  
}
