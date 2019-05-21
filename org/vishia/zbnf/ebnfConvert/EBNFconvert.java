package org.vishia.zbnf.ebnfConvert;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import org.vishia.util.Debugutil;
import org.vishia.util.StringPreparer;


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

  
  
  /**Some syntax components which are defined as name::= &lt;identifier&gt;.
   * They are written as &lt;$?name&gt; on usage.
   */
  Map<String, String> identifiers = new TreeMap<String, String>(); 
  
  
  /**Invoked from a JZtxtcmd script.
   * @param ebnf parsed data
   * @param wr FileWriter
   */
  public void convert(EBNFread ebnf, Appendable wr) {
    
    checkForIdentifier(ebnf);
    
    
    if(wr !=null) {
      try {
        for(EBNFread.EBNFdef ebnfCmpn: ebnf.list_cmpnDef) {
          if(identifiers.get(ebnfCmpn.cmpnName)!=null) {
            //it is an identifier.
          }
          else if(ebnfCmpn.cmpnDef.items ==null) {
            wr.append("## ").append(ebnfCmpn.cmpnName).append("::= ... ")
              .append(ebnfCmpn.cmpnDef.comment).append("\n\n");
          } else {
            wr.append(ebnfCmpn.cmpnName).append("::=");
            if(ebnfCmpn.nrCmpn <=1) {
              wr.append("<?>");  //component without own data because only one component in its syntax def
            }
            if(ebnfCmpn.cmpnName.equals("event_conn_source"))
              Debugutil.stop();
            convertExpr(wr, ebnfCmpn.cmpnDef, 1);
            wr.append(".\n\n");
          }
        }
      }catch(IOException exc) {}
    }
  }
  
  
  
  
  /**Checks for identifier definition:
   * If a EBNFdefinition is written as <pre>
   * syntaxdefXYZ ::= &lt;identifier> </pre>
   * then the syntaxdef is added to the {@link #identifiers}. On usage with <pre>
   *   ... syntaxdefXYZ ...</pre>
   * it is translated to <pre>
   *   ... &lt;$?syntaxdefXYZ></pre>  
   * 
   * @param ebnf
   */
  private void checkForIdentifier(EBNFread ebnf) {
    for(EBNFread.EBNFdef ebnfCmpn: ebnf.list_cmpnDef) {
      if(ebnfCmpn.cmpnName.equals("algorithm_name"))
        Debugutil.stop();

      EBNFread.EBNFitem item;
      if(  ebnfCmpn.cmpnDef.items !=null) {
        if( ebnfCmpn.cmpnDef.items.size() ==1
            && (item = ebnfCmpn.cmpnDef.items.get(0)).what == '<'
            && item.cmpn.equals("identifier")
            ) {
          identifiers.put(ebnfCmpn.cmpnName, ebnfCmpn.cmpnName);
        }
        else {
          for(EBNFread.EBNFitem itemcheck: ebnfCmpn.cmpnDef.items) {
            //if()
          }
        }
      }
    }
    
  }
  
  
  
  private void convertExpr(Appendable wr, EBNFread.EBNFexpr expr, int level) throws IOException {
    if(level >=2 && expr.hasAlternatives) {
      wr.append("[");
    }
    String sCmpnBeforeRepeat = null;
    boolean bOnlyOneCmpn = expr.cmpnDef.nrCmpn <=1;
    for(EBNFread.EBNFitem item: expr.items) {
      sCmpnBeforeRepeat = convertItem(wr, sCmpnBeforeRepeat, item, bOnlyOneCmpn, level);
    }
    wrCmpnCall(wr, sCmpnBeforeRepeat, bOnlyOneCmpn);
    if(level >=2 && expr.hasAlternatives) {
      wr.append("]");
    }
  }
  
  
  
  /**Converts one item from the read EBNF. An item is defined as <pre>
EBNFitem::=
  '<*'?literal>'               ## 'literal'
| <""?literal>                 ## "literal"
| \<<*\>?comment>\>            ## <comment in EBNF>
| <$?cmpn> [? ::= ]            ##ident::= breaks the loop, it is a new definition.
| \[ <EBNFalt?option> \]      ## [ simple option ]
| \{ <EBNFalt?repetition> \}  ## { repetition }  Note: can be empty. 
| ( <EBNFalt?parenthesis> )   ##sub syntax with own structure
.   * </pre>
   * <ul>
   * <li>Special handling of <code>  component { ...syntax... component }</code>:<br>
   *   It should be written as <code> { &lt;component> ? ...syntax...}</code><br>
   *   For that the <code>component</code> is not written but returned and checked for the next item if a repetition follows.
   *   If it is not so, the component is written with {@link #wrCmpnCall(Appendable, String)}-
   * <li><code>EBNFalt</code> is a sub syntax of the item. It is converted with calling {@link #convertExpr(Appendable, org.vishia.zbnf.ebnfConvert.EBNFread.EBNFexpr, int)}.
   * <li>Elsewhere the simple ZBNF representation is written.
   * </ul>
   * @param wr
   * @param sCmpnBeforeRepeat It is written as component like <code>&lt?component></code> using {@link #wrCmpnCall(Appendable, String)} 
   *   if this item is not a repetition. It is checked in {@link #checkRepetition(Appendable, String, org.vishia.zbnf.ebnfConvert.EBNFread.EBNFexpr, int)}.
   * @param item
   * @param level
   * @return null or a component name if a component call is detected.<code>
   * @throws IOException
   */
  private String convertItem(Appendable wr, String sCmpnBeforeRepeat, EBNFread.EBNFitem item, boolean bOnlyOneCmpn, int level) throws IOException {
    String sCmpnBeforeRepeatRet = null;
    switch(item.what) {
      case '[': 
        wrCmpnCall(wr, sCmpnBeforeRepeat, bOnlyOneCmpn);
        wr.append(" ["); convertExpr(wr, (EBNFread.EBNFexpr) item, level+1); wr.append(" ]"); 
        break;
      case '|':
        wrCmpnCall(wr, sCmpnBeforeRepeat, bOnlyOneCmpn);
        if(level <2) {
          wr.append("\n |");
        } else {
          wr.append(" |");
        }
        convertExpr(wr, (EBNFread.EBNFexpr) item, level+1);
        break;
      case '{': 
        checkRepetition(wr, sCmpnBeforeRepeat, (EBNFread.EBNFexpr) item, bOnlyOneCmpn, level+1);
        break;
      case '<': 
        wrCmpnCall(wr, sCmpnBeforeRepeat, bOnlyOneCmpn);
        sCmpnBeforeRepeatRet = item.cmpn;
        break;
      case '"': 
        wrCmpnCall(wr, sCmpnBeforeRepeat, bOnlyOneCmpn);
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
        wrCmpnCall(wr, sCmpnBeforeRepeat, bOnlyOneCmpn);
        wr.append("    ##").append(item.comment).append("\n    "); 
        break;
      case '(': 
        wrCmpnCall(wr, sCmpnBeforeRepeat, bOnlyOneCmpn);
        convertExpr(wr, (EBNFread.EBNFexpr) item, level+1); 
        break;
      default: throw new IllegalArgumentException("unexpected type of EBNFitem: " + item.what);
    }
    return sCmpnBeforeRepeatRet;
  }
  
  
  /**It writes a component call. It is deferred for repetition check.
   * @param wr
   * @param sCmpn
   * @throws IOException
   */
  private void wrCmpnCall(Appendable wr, String sCmpn, boolean bOnlyOneCmpn) throws IOException {
    if(sCmpn !=null) {
      wr.append(" <");
      if(identifiers.get(sCmpn)!=null) {
        wr.append("$?");
      }
      wr.append(sCmpn);
      wr.append('>');
    }
  }
  
  
  /**In EBNF a typical repetition is written like <pre>
   * COMPONENT { SEPARATOR COMPONENT }.</pre>
   * In Zbnf it is more simple, more clear and necessary to create data:<pre>
   * { &lt;COMPONENT&gt; ? SEPARATOR }.</pre>
   * After ? it is the backward branch.
   * <br>
   * This routine tests whether the last item of expr is a Component call 
   * with the same component like given in sCmpnBeforeRepeat. 
   * Then it is such a typical kind of repetition with at least one occurence and separator.
   * 
   * @param wr
   * @param sCmpnBeforeRepeat
   * @param expr
   * @param level
   * @throws IOException
   */
  private void checkRepetition(Appendable wr, String sCmpnBeforeRepeat, EBNFread.EBNFexpr expr, boolean bOnlyOneCmpn, int level) throws IOException {
    EBNFread.EBNFitem itemRepeat;
    int zItems = expr.items.size();
    if(  zItems >=1
      && (itemRepeat = expr.items.get(zItems -1)).what == '<'
      && itemRepeat.cmpn.equals(sCmpnBeforeRepeat) 
      ) {
      wr.append(" {"); 
      wrCmpnCall(wr, sCmpnBeforeRepeat, bOnlyOneCmpn);
      if(zItems >1) {
        wr.append(" ? "); 
        String sCmpnInRepeat = null;
        for(int ix=0; ix < zItems-1; ++ix) {
          EBNFread.EBNFitem item = expr.items.get(ix);
          sCmpnInRepeat = convertItem(wr, sCmpnInRepeat, item, bOnlyOneCmpn, level);
        }
        if(sCmpnInRepeat !=null) { wrCmpnCall(wr, sCmpnInRepeat, bOnlyOneCmpn); }
      }
      wr.append(" }"); 
    
    } else {
      wrCmpnCall(wr, sCmpnBeforeRepeat, bOnlyOneCmpn);
      wr.append(" [{"); convertExpr(wr, expr, level+1); wr.append(" }]"); 
    }
  
  }
}
