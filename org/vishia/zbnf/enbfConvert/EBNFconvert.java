package org.vishia.zbnf.enbfConvert;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import org.vishia.util.Debugutil;
import org.vishia.util.StringPreparer;
import org.vishia.zbnf.ZbnfParser;
import org.vishia.zcmd.Zbnf2Text;


/**This class writes zbnf syntax from read EBNF.
 * @author hartmut Schorrig
 *
 */
public class EBNFconvert
{

  /**Version, history and license.
   * <ul>
   * <li>2019-05-14 Hartmut creation: 
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
  public static final String sVersion = "2019-05-16";

  
  
  StringPreparer zbnfCmpn = new StringPreparer("<&cmpnName>::=");
  
  StringPreparer zbnfExpr = new StringPreparer("");
  
  /**Some syntax components which are defined as name::= &lt;identifier&gt;.
   * They are written as &lt;$?name&gt; on usage.
   */
  Map<String, String> identifiers = new TreeMap<String, String>(); 
  
  
  /**Invoked from a JZtxtcmd script.
   * @param ebnf parsed data
   * @param wr FileWriter
   */
  public void convert(EBNFread ebnf, Appendable wr) {
    Debugutil.stop();
    
    for(EBNFread.EBNFdef ebnfCmpn: ebnf.list_cmpnDef) {
      if(ebnfCmpn.cmpnName.equals("algorithm_name"))

        Debugutil.stop();
      EBNFread.EBNFexpr item;
      if(  ebnfCmpn.cmpnDef.items !=null
        && ebnfCmpn.cmpnDef.items.size() ==1
        && (item = ebnfCmpn.cmpnDef.items.get(0)).what == '<'
        && item.cmpn.equals("identifier")
        ) {
        identifiers.put(ebnfCmpn.cmpnName, ebnfCmpn.cmpnName);
      }
    }
    
    
    if(wr !=null) {
      StringBuilder sb = new StringBuilder(200);
      try {
        for(EBNFread.EBNFdef ebnfCmpn: ebnf.list_cmpnDef) {
          if(identifiers.get(ebnfCmpn.cmpnName)!=null) {
            //it is an identifier.
          }
          else if(ebnfCmpn.cmpnDef.items ==null) {
            wr.append("## ").append(ebnfCmpn.cmpnName).append("::= ... ").append(ebnfCmpn.cmpnDef.comment).append("\n\n");
          } else {
            sb.setLength(0); //clear
            zbnfCmpn.exec(sb, ebnfCmpn.cmpnName, "...");
            wr.append(sb);
            if(ebnfCmpn.cmpnName.equals("event_conn_source"))
              Debugutil.stop();
            convert(wr, ebnfCmpn.cmpnDef, 1);
            wr.append(".\n\n");
          }
        }
//        wr.close();
      }catch(IOException exc) {}
    }
  }
  
  
  
  private void convert(Appendable wr, EBNFread.EBNFexpr expr, int level) throws IOException {
    if(level >=2 && expr.hasAlternatives) {
      wr.append("[");
    }
    
    for(EBNFread.EBNFexpr item: expr.items) {
      switch(item.what) {
        case '[': 
          wr.append(" ["); convert(wr, item, level+1); wr.append(" ]"); 
          break;
        case '|':
          if(level <2) {
            wr.append("\n |");
          } else {
            wr.append(" |");
          }
          convert(wr, item, level+1);
          break;
        case '{': 
          wr.append(" [{"); convert(wr, item, level+1); wr.append(" }]"); 
          break;
        case '<': 
          wr.append(" <");
          if(identifiers.get(item.cmpn)!=null) {
            wr.append("$?");
          }
          wr.append(item.cmpn).append(">"); 
          break;
        case '"': 
          int ix = 0;
          wr.append(' ');
          while(ix <  item.literal.length()) {
            char cc = item.literal.charAt(ix++);
            if("[]{}<>.?\\".indexOf(cc) >=0) {
              wr.append('\\');
            }
            wr.append(cc);
          }
          break;
        case '#': 
          wr.append("    ##").append(item.comment).append("\n    "); 
          break;
        case '(': 
          convert(wr, item, level+1); 
          break;
        default: throw new IllegalArgumentException("unexpected type of EBNFitem: " + item.what);
      }
    }

    if(level >=2 && expr.hasAlternatives) {
      wr.append("]");
    }
  }
}
