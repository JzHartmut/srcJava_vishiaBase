
package org.vishia.zbnf.ebnfConvert;

import java.util.ArrayList;
import java.util.List;

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


  public String cmpnName; //<$?cmpnName> 


  public List<EBNFdef> list_cmpnDef = new ArrayList<EBNFdef>();

  /**From ZBNF syntax: &lt;...?cmpnDef> */
  public EBNFdef new_EBNFdef() { return new EBNFdef(); }
  public void add_EBNFdef(EBNFdef val) { list_cmpnDef.add(val); }


  public String pdftext; //<*| ?pdftext> 

  public static class EBNFblock extends EBNFdef {


    public String pdftext; //<*| ?pdftext> 

  }

  public static class EBNFdef {

    public String cmpnName; //<$?cmpnName> 


    public EBNFalt cmpnDef;

    /**Number of called Components in this expression.
     * If it is only 1, the ZBNF call is writteh with <code>&lt;cmpn?></code>
     */
    public int nrCmpn;

    /**From ZBNF syntax: &lt;...?cmpnDef> */
    public EBNFalt new_cmpnDef() { return new EBNFalt(this, ':'); }
    public void set_cmpnDef(EBNFalt val) { cmpnDef = val; }

  }


  
  /**It is either the whole syntax of a component without toplevel alternatives or one alternative on toplevel.
   *
   */
  public static class EBNFalt extends EBNFexpr {
    
    //EBNFalt(char what){ expr = new EBNFexpr(this, what); }
    EBNFalt(EBNFdef cmpnDef, char what){ super(cmpnDef, what);  }
    
    /**From ZBNF syntax: &lt;...?alternative> */
    public EBNFexpr new_alternative() { 
      this.hasAlternatives = true;
      if(cmpnDef.nrCmpn <=1) {
        cmpnDef.nrCmpn = 0;    //if the last alternative has less then 1 component, count newly.
      } //else, it is >1, count forward, not meaningfull.
      
      return (new EBNFalt( super.cmpnDef, '|')); 
    }

    public void add_alternative(EBNFexpr val) {
      if(items == null) { items = new ArrayList<EBNFitem>(); } 
      items.add(val); 
    }


    
 
  }

  public static class EBNFexpr extends EBNFitem {

    final EBNFdef cmpnDef;
    
    public boolean hasAlternatives = false;

    /**Contains more as one member of items. Else null. */
    public List<EBNFitem> items;
    

    public EBNFexpr(EBNFdef cmpnAlt, char what) { 
      super(what); 
//      if(cmpnAlt == null) {
//        this.cmpndef = (EBNFalt)this; 
//      } else {
        this.cmpnDef = cmpnAlt;
//      }
    }  


    /**From ZBNF syntax: &lt;...?option> */
    public EBNFexpr new_option() { return new EBNFexpr(cmpnDef, '['); }
    
    public void add_option(EBNFexpr val) { 
      if(items == null) { items = new ArrayList<EBNFitem>(); } 
      items.add(val); 
    }





    /**From ZBNF syntax: &lt;...?repetition> */
    public EBNFexpr new_repetition() { return new EBNFexpr(cmpnDef, '{'); }
    
    public void add_repetition(EBNFexpr val) {
      if(items == null) { items = new ArrayList<EBNFitem>(); } 
      items.add(val); 
    }




    /**From ZBNF syntax: &lt;...?alternative> */
    public EBNFexpr new_alternative() { 
      this.hasAlternatives = true; 
      return (new EBNFexpr(cmpnDef, '|')); 
    }

    public void add_alternative(EBNFexpr val) {
      if(items == null) { items = new ArrayList<EBNFitem>(); } 
      items.add(val); 
    }



    /**From ZBNF syntax: &lt;...?parenthesis> */
    public EBNFexpr new_parenthesis() { return new EBNFexpr(cmpnDef, '('); }
    
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
      cmpnDef.nrCmpn +=1;
      EBNFitem item = new EBNFitem('<'); 
      item.cmpn = ident; 
      if(items == null) { items = new ArrayList<EBNFitem>(); } 
      items.add(item); 
    }

    public void set_comment(String comment){ 
      EBNFitem item = new EBNFitem('#'); 
      item.comment = comment; 
      if(items == null) { items = new ArrayList<EBNFitem>(); } 
      items.add(item); 
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

  }
  
  
  
  public static class EBNFmain {

    public String cmpnName; //<$?cmpnName> 


    List<EBNFexpr> list_cmpnDef = new ArrayList<EBNFexpr>();

    /**From ZBNF syntax: &lt;...?cmpnDef> */
//    public EBNFexpr new_cmpnDef() { return new EBNFexpr(':'); }
//    public void add_cmpnDef(EBNFexpr val) { list_cmpnDef.add(val); }


    public String pdftext; //<*| ?pdftext> 

  }

}
