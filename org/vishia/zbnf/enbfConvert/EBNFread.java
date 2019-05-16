
package org.vishia.zbnf.enbfConvert;

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

  public static class EBNFblock {

    public String cmpnName; //<$?cmpnName> 


    List<EBNFexpr> list_cmpnDef = new ArrayList<EBNFexpr>();

    /**From ZBNF syntax: &lt;...?cmpnDef> */
    public EBNFexpr new_cmpnDef() { return new EBNFexpr(':'); }
    public void add_cmpnDef(EBNFexpr val) { list_cmpnDef.add(val); }


    public String pdftext; //<*| ?pdftext> 

  }

  public static class EBNFdef {

    public String cmpnName; //<$?cmpnName> 


    public EBNFexpr cmpnDef;

    /**From ZBNF syntax: &lt;...?cmpnDef> */
    public EBNFexpr new_cmpnDef() { return new EBNFexpr(':'); }
    public void set_cmpnDef(EBNFexpr val) { cmpnDef = val; }

  }

  public static class EBNFexpr {

    public int page;  

    public String literal; //<"" ""?literal> 

    public String comment; //<*C >?comment> 

    public String cmpn; //<$?cmpn> 

    public boolean hasAlternatives = false;

    /**Contains more as one member of items. Else null. */
    public List<EBNFexpr> items;

    public final char what;  

    public EBNFexpr(char what){ this.what = what; }  


    /**From ZBNF syntax: &lt;...?option> */
    public EBNFexpr new_option() { return new EBNFexpr('['); }
    public void add_option(EBNFexpr val) { 
      if(items == null) { items = new ArrayList<EBNFexpr>(); } 
      items.add(val); 
    }





    /**From ZBNF syntax: &lt;...?repetition> */
    public EBNFexpr new_repetition() { return new EBNFexpr('{'); }
    public void add_repetition(EBNFexpr val) {
      if(items == null) { items = new ArrayList<EBNFexpr>(); } 
      items.add(val); 
    }




    /**From ZBNF syntax: &lt;...?alternative> */
    public EBNFexpr new_alternative() { 
      this.hasAlternatives = true; 
      return new EBNFexpr('|'); 
    }

    public void add_alternative(EBNFexpr val) {
      if(items == null) { items = new ArrayList<EBNFexpr>(); } 
      items.add(val); 
    }



    /**From ZBNF syntax: &lt;...?parenthesis> */
    public EBNFexpr new_parenthesis() { return new EBNFexpr('('); }
    public void add_parenthesis(EBNFexpr val) { 
      if(items == null) { items = new ArrayList<EBNFexpr>(); } 
      items.add(val); 
    }



    public void set_literal(String literal) { 
      EBNFexpr item = new EBNFexpr('\"'); 
      item.literal = literal; 
      if(items == null) { items = new ArrayList<EBNFexpr>(); } 
      items.add(item); 
    }

    public void set_cmpn(String ident){ 
      EBNFexpr item = new EBNFexpr('<'); 
      item.cmpn = ident; 
      if(items == null) { items = new ArrayList<EBNFexpr>(); } 
      items.add(item); 
    }

    public void set_comment(String comment){ 
      EBNFexpr item = new EBNFexpr('#'); 
      item.comment = comment; 
      if(items == null) { items = new ArrayList<EBNFexpr>(); } 
      items.add(item); 
    }


  }

  public static class EBNFmain {

    public String cmpnName; //<$?cmpnName> 


    List<EBNFexpr> list_cmpnDef = new ArrayList<EBNFexpr>();

    /**From ZBNF syntax: &lt;...?cmpnDef> */
    public EBNFexpr new_cmpnDef() { return new EBNFexpr(':'); }
    public void add_cmpnDef(EBNFexpr val) { list_cmpnDef.add(val); }


    public String pdftext; //<*| ?pdftext> 

  }

}
