package org.vishia.zbnf.ebnfConvert;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import org.vishia.util.Debugutil;
import org.vishia.zbnf.ebnfConvert.EBNFread.EBNFdef;


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
  Map<String, EBNFread.EBNFdef> identifiers = new TreeMap<String, EBNFread.EBNFdef>(); 
  
  Map<String, EBNFdef> idx_cmpnDef;

  
  /**Invoked from a JZtxtcmd script.
   * @param ebnf parsed data
   * @param wr FileWriter
   */
  public void convert(EBNFread ebnf, Appendable wr) {
    
    idx_cmpnDef = ebnf.idx_cmpnDef;
    
    checkForIdentifier(ebnf);
    
    
    if(wr !=null) {
      try {
        for(EBNFread.EBNFdef ebnfCmpn: ebnf.list_cmpnDef) {
          if(ebnfCmpn.cmpnRepl !=null || ebnfCmpn.zbnfBasic !=null){
            //do nothing.
            Debugutil.stop();
          }
          else if(identifiers.get(ebnfCmpn.cmpnName)!=null) {
            //it is an identifier.
          }
          
          else if(ebnfCmpn.items ==null) {
            wr.append("## ").append(ebnfCmpn.cmpnName).append("::= ... ")
              .append(ebnfCmpn.comment).append("\n\n");
          } else {
            wr.append(ebnfCmpn.cmpnName).append("::=");
            if(ebnfCmpn.bOnlyText) {
              wr.append("<?>");  //component without own data because only one component in its syntax def
            }
//          if(ebnfCmpn.nrCmpn <=1) {
//          wr.append("<?>");  //component without own data because only one component in its syntax def
//        }
            if(ebnfCmpn.cmpnName.equals("event_conn_source"))
              Debugutil.stop();
            convertExpr(wr, ebnfCmpn, 1);
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
      if(!ebnfCmpn.bChecked) {
        checkCmpn(ebnfCmpn);
      }
    }
    
  }
  
  
  
  private void checkCmpn(EBNFread.EBNFdef ebnfCmpn) {
    if(ebnfCmpn.cmpnName.equals("subscript")) {
      Debugutil.stop();
    }
    EBNFread.EBNFitem item;
    ebnfCmpn.bChecked = true;  //prevent check again.
    if(ebnfCmpn.zbnfBasic ==null) { //do nothing for basics.
      
      ebnfCmpn.bChecking = true;
      ebnfCmpn.bOnlyText = true;  //set to false if other than a text is detected.
      ebnfCmpn.nrCmpn = 0;
      if(  ebnfCmpn.items !=null) {
        if( ebnfCmpn.items.size() ==1
            && (item = ebnfCmpn.items.get(0)).what == '<'
            //&& item.cmpn.equals("identifier")
            ) {
          EBNFread.EBNFdef cmpnRepl = idx_cmpnDef.get(item.cmpn);
          ebnfCmpn.cmpnRepl = cmpnRepl;
          identifiers.put(ebnfCmpn.cmpnName, cmpnRepl);
          ebnfCmpn.bOnlyText = false;
        }
        else {
          if(ebnfCmpn.cmpnName.equals("signed_integer_type_name"))
            Debugutil.stop();
          checkAlternative(ebnfCmpn, ebnfCmpn, 0);  //check the first expression, then alternatives
        }
      }
    }
    ebnfCmpn.bChecking = false;
  }
  
  
  
  
  void checkAlternative(EBNFread.EBNFexpr expr, EBNFread.EBNFdef cmpn, int recursive) {
    for(EBNFread.EBNFitem item: expr.items) {
      if(item.what == '|') { //another alternative
        //if an alternative follows after the items of an expr, only some more alternative expr are following.
        //The first alternative is finished now.
        if(recursive == 0 && cmpn.nrCmpn <=1) {  //only on first level:
          cmpn.nrCmpn = 0;  //count for the next alternativ
        }
        checkAlternative((EBNFread.EBNFexpr)item, cmpn, recursive +1);
      } else {
        checkitem(item, cmpn, recursive);
      }
    }
    
  }
  
  
  
  void checkExpr(EBNFread.EBNFexpr expr, EBNFread.EBNFdef cmpn, int recursive) {
    if(recursive > 100) throw new IllegalArgumentException("too many recursions");
    
    for(EBNFread.EBNFitem item: expr.items) {
      checkitem(item, cmpn, recursive);
    }
    
  }
  
  
  
  void checkitem(EBNFread.EBNFitem item, EBNFread.EBNFdef cmpn, int recursive) {
    if(item.what == '|') { //another alternative
      //if an alternative follows after the items of an expr, only some more alternative expr are following.
      //The first alternative is finished now.
      //checkAlternative((EBNFread.EBNFexpr)item, cmpn);
    } else {
      boolean bOnlyTextCmpn = false;
      if(item.what == '<') {
        cmpn.nrCmpn +=1;  //count called components to detect only one.
        if(item.cmpn.equals("resource_type_name"))
          Debugutil.stop();
        if(identifiers.get(item.cmpn) ==null) {
          EBNFread.EBNFdef cmpnCall = idx_cmpnDef.get(item.cmpn);
          if(cmpnCall !=null && !cmpnCall.bChecked) {
            checkCmpn(cmpnCall);
          }
          if(cmpnCall !=null && cmpnCall.bChecking && cmpnCall.bOnlyText) {
            Debugutil.stop();
            bOnlyTextCmpn = false;  //The component is in checking, not ready, it invokes this cmpn. No text!
          } else 
            bOnlyTextCmpn = cmpnCall !=null && cmpnCall.bOnlyText;
        }
      }
      if(item.what == '<' && !bOnlyTextCmpn ) {
        cmpn.bOnlyText = false;
      }
      if( "#".indexOf(item.what) >=0) {  //not a component or literal 
        cmpn.bOnlyText = false;
      }
    }
    //Handle all sub expr, | too:
    if(item instanceof EBNFread.EBNFexpr) {
      checkExpr((EBNFread.EBNFexpr)item, cmpn, recursive+1);
    }
    
  }
  
  private void convertExpr(Appendable wr, EBNFread.EBNFexpr expr, int level) throws IOException {
    if(level >=2 && expr.hasAlternatives) {
      wr.append("[");
    }
    String sCmpnBeforeRepeat = null;
    boolean bOnlyOneCmpn = expr.cmpnDef.nrCmpn <=1;
    for(EBNFread.EBNFitem item: expr.items) {
      sCmpnBeforeRepeat = convertItem(wr, sCmpnBeforeRepeat, item, expr.cmpnDef, bOnlyOneCmpn, level);
    }
    wrCmpnCall(wr, sCmpnBeforeRepeat, bOnlyOneCmpn, expr.cmpnDef);
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
  private String convertItem(Appendable wr, String sCmpnBeforeRepeat
      , EBNFread.EBNFitem item, EBNFread.EBNFdef inCmpn, boolean bOnlyOneCmpn, int level) throws IOException {
    String sCmpnBeforeRepeatRet = null;
    switch(item.what) {
      case '[': 
        EBNFread.EBNFexpr itemExpr = (EBNFread.EBNFexpr) item;
        wrCmpnCall(wr, sCmpnBeforeRepeat, bOnlyOneCmpn, inCmpn);
        wr.append(" [");
        if(itemExpr.bOnlyTextInExpr) {
          wr.append("<?").append("text").append('>');
        }
        convertExpr(wr, itemExpr, level+1); 
        wr.append(" ]"); 
        break;
      case '|':
        wrCmpnCall(wr, sCmpnBeforeRepeat, bOnlyOneCmpn, inCmpn);
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
        wrCmpnCall(wr, sCmpnBeforeRepeat, bOnlyOneCmpn, inCmpn);
        sCmpnBeforeRepeatRet = item.cmpn;
        break;
      case '"': 
        wrCmpnCall(wr, sCmpnBeforeRepeat, bOnlyOneCmpn, inCmpn);
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
        wrCmpnCall(wr, sCmpnBeforeRepeat, bOnlyOneCmpn, inCmpn);
        wr.append("    ##").append(item.comment).append("\n    "); 
        break;
      case '(': 
        wrCmpnCall(wr, sCmpnBeforeRepeat, bOnlyOneCmpn, inCmpn);
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
  private void wrCmpnCall(Appendable wr, String sCmpn, boolean bOnlyOneCmpn, EBNFread.EBNFdef inCmpn) throws IOException {
    if(sCmpn ==null) return;  //no sCmpn given, typical
    //String sCmpnRepl = identifiers.get(sCmpn);  
    boolean bCmpnText;
    EBNFread.EBNFdef cmpn = idx_cmpnDef.get(sCmpn);
    EBNFread.EBNFdef cmpnRepl = cmpn;
    while(cmpnRepl !=null && cmpnRepl.cmpnRepl !=null) {
      cmpnRepl = cmpnRepl.cmpnRepl;
    }
    if(cmpn == null || cmpn.cmpnRepl !=null) { 
      bCmpnText = false; 
    } else {
      if(inCmpn.bOnlyText && cmpn.bOnlyText) {
        bCmpnText = false;
      } else {
        bCmpnText = cmpn.bOnlyText;
      }
    }
    wr.append(" <");
    if(cmpnRepl !=null && cmpnRepl.zbnfBasic !=null) {
      wr.append(cmpnRepl.zbnfBasic).append("?");
    }
    else if(cmpnRepl != cmpn) {
      String sCmpnRepl = cmpnRepl.cmpnName;
      wr.append(sCmpnRepl).append("?");
      if(bCmpnText) {
        wr.append("\"!\"");
        bCmpnText = false;
      }
    }
    
    wr.append(sCmpn);  //it is the semantic after replacement or it is the syntax.
    if(bCmpnText) {
      wr.append("?\"!\"@");
    }
    if(cmpn !=null && cmpn.cmpnRepl ==null && cmpn.nrCmpn <=1 && !cmpn.bOnlyText) {
      wr.append("?");
    }
    wr.append('>');
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
      wrCmpnCall(wr, sCmpnBeforeRepeat, bOnlyOneCmpn, expr.cmpnDef);
      if(zItems >1) {
        wr.append(" ? "); 
        String sCmpnInRepeat = null;
        for(int ix=0; ix < zItems-1; ++ix) {
          EBNFread.EBNFitem item = expr.items.get(ix);
          sCmpnInRepeat = convertItem(wr, sCmpnInRepeat, item, expr.cmpnDef, bOnlyOneCmpn, level);
        }
        if(sCmpnInRepeat !=null) { wrCmpnCall(wr, sCmpnInRepeat, bOnlyOneCmpn, expr.cmpnDef); }
      }
      wr.append(" }"); 
    
    } else {
      wrCmpnCall(wr, sCmpnBeforeRepeat, bOnlyOneCmpn, expr.cmpnDef);
      wr.append(" [{"); convertExpr(wr, expr, level+1); wr.append(" }]"); 
    }
  
  }
}
