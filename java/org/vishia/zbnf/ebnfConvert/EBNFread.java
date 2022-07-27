
package org.vishia.zbnf.ebnfConvert;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.vishia.util.Debugutil;

/**This class is the destination to storing parse results with ebnf.zbnf, the interpretation of EBNF sources. 
 * The used syntax is:<pre>
<?ZBNF-www.vishia.de version="0.7" encoding="UTF-8" ?>
$endlineComment=##.



EBNFmain::= { <EBNFblock?> }\e.

EBNFblock::= [PRODUCTION RULE[S]: | SYNTAX:] { <EBNFdef> } [B\.| SEMANTICS:|\e] <*|PRODUCTION RULE:|PRODUCTION RULES:|SYNTAX:|\e?pdftext>.

EBNFdef::= <$?cmpnName> ::= <EBNFalt?cmpnDef>.


EBNFalt::=
{ [? SEMANTICS: | B\. ]        ##not SEMANTICS:, abort loop
  [  <EBNFitem?>
  | \| <EBNFexpr?alternative>    ## | alternative   Note: it is lower prior then concatenation.
  ]                
}.

EBNFexpr::=
{ [? SEMANTICS: | B\. ]        ##not SEMANTICS:, abort loop
  <EBNFitem?>
}.

EBNFitem::=
| '<*'?literal>'               ## 'literal'
| <""?literal>                 ## "literal"
| \<<*\>?comment>\>            ## <comment in EBNF>
| <$?cmpn> [? ::= ]            ##ident::= breaks the loop, it is a new definition.
| \[ <EBNFalt?option> \]      ## [ simple option ]
| \{ <EBNFalt?repetition> \}  ## { repetition }  Note: can be empty. 
| ( <EBNFalt?parenthesis> )   ##sub syntax with own structure
 * </pre>
 * Note: The EBNF syntax came from a copy of text in a pdf document. 
 * */

public class EBNFread {


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


  //public String cmpnName; //<$?cmpnName> 


  public List<EBNFdef> list_cmpnDef = new ArrayList<EBNFdef>();

  public Map<String, EBNFdef> idx_cmpnDef = new TreeMap<String, EBNFdef>();

  /**From ZBNF syntax: &lt;...?cmpnDef> */
  public EBNFdef new_EBNFdef() { return new EBNFdef(); }
  public void add_EBNFdef(EBNFdef val) { list_cmpnDef.add(val); idx_cmpnDef.put(val.cmpnName, val); }


  public String pdftext; //<*| ?pdftext> 


  /**An instance of this contains a EBNF component definition like name::=SYNTAX.
   * It based on {@link EBNFalt} because the SYNTAX is stored in the base immediately.
   * This class adds only the {@link #cmpnName} to complete the data.
   *
   */
  public static class EBNFdef extends EBNFalt {

    public boolean bOnlyText;
    
    public boolean bChecked, bChecking;
    
    public String cmpnName; //<$?cmpnName> 


    public String zbnfBasic;
    
    //public EBNFalt cmpnDef;
    
    /**Number of called Components in this expression.
     * If it is only 1, the ZBNF call is writteh with <code>&lt;cmpn?></code>
     */
    public int nrCmpn;
    
    
    /**If set this component definition contains only one item as component call.
     * 
     */
    public EBNFdef cmpnRepl;


    public EBNFdef() { super(null, ':'); }

    /**From ZBNF syntax: &lt;...?cmpnDef> */
//    public EBNFalt new_cmpnDef() { return new EBNFalt(this, ':'); }
//    public void set_cmpnDef(EBNFalt val) { cmpnDef = val; }

  }


  
  /**It is either the whole syntax of a component without toplevel alternatives or one alternative on toplevel.
   *
   */
  public static class EBNFalt extends EBNFexpr {
    
    //EBNFalt(char what){ expr = new EBNFexpr(this, what); }
    EBNFalt(EBNFdef cmpnDef, char what){ super(cmpnDef, null, what);  }
    
    /**From ZBNF syntax: &lt;...?alternative> */
    public EBNFexpr new_alternative() { 
      this.hasAlternatives = true;
//      if(cmpnDef.nrCmpn <=1) {
//        cmpnDef.nrCmpn = 0;    //if the last alternative has less then 1 component, count newly.
//      } //else, it is >1, count forward, not meaningfull.
      
      return (new EBNFalt( super.cmpnDef, '|')); 
    }

    public void add_alternative(EBNFexpr val) {
      if(items == null) { items = new ArrayList<EBNFitem>(); } 
      items.add(val); 
    }

    @Override public String toString() {
      return " |" + what + " " ;
    }

    
 
  }

  public static class EBNFexpr extends EBNFitem {

    /**The parent component definition for component global settings. */
    final EBNFdef cmpnDef;
    
    final EBNFexpr _parent;
    
    /**Set false on evaluation if other than literal are stored here.
     * 
     */
    boolean bOnlyTextInExpr = true;
    
    public boolean hasAlternatives = false;

    /**Contains more as one member of items. Else null. */
    public List<EBNFitem> items;
    

    public EBNFexpr(EBNFdef cmpnDef, EBNFexpr parent, char what) { 
      super(what); 
      this._parent = parent;
      if(cmpnDef == null) {           //null is given on the constructor of EBNFdef itself, 
        this.cmpnDef = (EBNFdef)this; //the use this itself. 
      } else {
        this.cmpnDef = cmpnDef;
      }
    }  


    /**From ZBNF syntax: &lt;...?option> */
    public EBNFexpr new_option() { return new EBNFexpr(cmpnDef, this, '['); }
    
    public void add_option(EBNFexpr val) { 
      if(items == null) { items = new ArrayList<EBNFitem>(); } 
      items.add(val); 
    }





    /**From ZBNF syntax: &lt;...?repetition> */
    public EBNFexpr new_repetition() { return new EBNFexpr(cmpnDef, this, '{'); }
    
    public void add_repetition(EBNFexpr val) {
      if(items == null) { items = new ArrayList<EBNFitem>(); } 
      items.add(val); 
    }




    /**From ZBNF syntax: &lt;...?alternative> */
    public EBNFexpr new_alternative() { 
      this.hasAlternatives = true; 
      return (new EBNFexpr(cmpnDef, this, '|')); 
    }

    public void add_alternative(EBNFexpr val) {
      if(items == null) { items = new ArrayList<EBNFitem>(); } 
      items.add(val); 
    }



    /**From ZBNF syntax: &lt;...?parenthesis> */
    public EBNFexpr new_parenthesis() { return new EBNFexpr(cmpnDef, this, '('); }
    
    public void add_parenthesis(EBNFexpr val) { 
      if(items == null) { items = new ArrayList<EBNFitem>(); } 
      items.add(val); 
    }



    public void set_literal(String literal) { 
      EBNFitem item = new EBNFitem('\"'); 
      item.literal = literal; 
      if(items == null) { items = new ArrayList<EBNFitem>(); } 
      items.add(item); 
    }

    public void set_cmpn(String ident){ 
      //EBNFalt cmpnAlt = this.cmpnAlt == null ? (EBNFalt)this : this.cmpnAlt;
      if(ident.equals("plug_name") && cmpnDef.cmpnName.equals("ec_expression_operand"))
        Debugutil.stop();
      resetOnlyTextItem();
//      cmpnDef.nrCmpn +=1;
      EBNFitem item = new EBNFitem('<'); 
      item.cmpn = ident; 
      if(items == null) { items = new ArrayList<EBNFitem>(); } 
      items.add(item); 
    }

    public void set_comment(String comment){ 
      resetOnlyTextItem();
      EBNFitem item = new EBNFitem('#'); 
      item.comment = comment; 
      if(items == null) { items = new ArrayList<EBNFitem>(); } 
      items.add(item); 
    }

    
    
    void resetOnlyTextItem() {
      EBNFexpr parent = this;
      int recursive = 0;
      do {
        parent.bOnlyTextInExpr = false;
        parent = parent._parent;
      } while(parent !=null && recursive < 20);
      if(parent !=null) {
        throw new RuntimeException("test me");
      }
    }
    
    
    @Override public String toString() {
      return what + "";
    }


  }

  
  /**A non derived item has no sub items. It is a end-item: String or component. 
   * A derived item is a EBNFexpr.
   *
   */
  public static class EBNFitem {

    public EBNFitem(char what){ this.what = what; }  

    /**Short designation for
     * <ul>
     * <li>: for a component (sub syntax) definition (ENMFexpr)
     * <li>{ [ ( for repetition, option, paranthesis (EBNFexpr)
     * <li>| for alternative (EBNFexpr)
     * <li>" # < for string, comment, component-call (sub syntax) in a simple EBNFitem
     * </ul>
     * 
     */
    public final char what;

    public int page;  

    public String literal; //<"" ""?literal> 

    public String comment; //<*C >?comment> 

    public String cmpn; //<$?cmpn> 
    
    @Override public String toString() {
      if(what == '<') return '<' + cmpn + '>';
      else return what + literal;
    }

  }
  
  
  
  public static class XXXXXEBNFmain {

    public String cmpnName; //<$?cmpnName> 


    List<EBNFexpr> list_cmpnDef = new ArrayList<EBNFexpr>();

    /**From ZBNF syntax: &lt;...?cmpnDef> */
//    public EBNFexpr new_cmpnDef() { return new EBNFexpr(':'); }
//    public void add_cmpnDef(EBNFexpr val) { list_cmpnDef.add(val); }


    public String pdftext; //<*| ?pdftext> 

  }

}
