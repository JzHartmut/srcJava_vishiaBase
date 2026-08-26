package org.vishia.util;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**This class supports management of condition expressions.
 * A given condition expression can be transformed to a true table.
 * Then with an algorithm similar as using in a Karnaugh map
 * (see https://en.wikipedia.org/wiki/Karnaugh_map)
 * the resulting expression can be read out back from the true table (which is a karnaugh map).
 * The Karnaugh map does not need to be as clear as a human representation,
 * as the algorithm has more options available to it. 
 * <br>
 * The true table is presented as result in one bit per line:
 * <pre>
 * boolean in bit
 * CBA
 * 000     0
 * 001     1
 * 010     2
 * 011     3
 * 100     4
 * 101     5
 * 110     6
 * 111     7 </pre>
 * Hence the A is presented with value true as:<pre>
 *       10101010 </pre>
 * And the others:<pre>
 * ~A    01010101
 *  A    10101010  
 * ~B    00110011
 *  B    11001100  
 * ~C    00001111 
 *  C    11110000 </pre>
 *  With this scheme, in the 64 bit long value up to 6 boolean values are able to process.
 *  To process more, it needs an array long[].
 *  For 20 booleans 4096 elements are necessary. But usual less booleans are used.
 *  <br>TODO The algorithm is yet written and tested only for the 6 boolean. 
 *  <br>  
 *       
 * 
 * Simple example: <code>B & A || B & ~A || ~B & ~A</code> is presented as:
 * <pre>
 * ~A     01010101
 *  A     10101010
 * ~B     00110011
 *  B     11001100
 *  
 *  A     10101010
 *  & B   11001100
 *        --------
 * B & A  10001000
 * ||    
 * B & ~A 01000010
 * || 
 *~B & ~A 00010001
 *        --------
 *    y   11011101 The result    
 * </pre>
 * <ul>
 * <li>The value C is not used, because bit 7..4 and 3..0 are equal.
 * <li>it remains <code>1101</code>
 * <li><code>y & B == B</code> because <code>1101 & 1100  == 1100</code>, B is set in or relation.
 * <li><code>y = y & ~B </code> because B is tested, remains <code>0001</code> 
 *   or better <code>0101</code> because the ~B result is duplicated in the bits of B         
 * <li><code>y & A == 0</code> because <code>0101 & 1010 == 0.0.</code>, A is not set in or relation.
 * <li><code>y &~A == ~A</code> because <code>.1.1. == .1.1</code>, ~A is set in or relation.
 * <li>Hence the result is <code>B || ~A</code>
 * 
 * @author Hartmut Schorrig, LPGL license
 * @since 2026-08-11
 *
 */
public class ConditionExpression {

  /**Version, history and license.
   * <ul>
   * <li>2026-08-11 Created, some refactoring after
   * </ul>
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
   */
  public static String version = "2026-08-25";

  /**Describes an instance which offers a simple condition, true or false or an enumeration. 
   * It is designated as "pin" of a function block. 
   *
   */
  public interface PinCond {
    
    /**For a boolean condition 0 is false, 1 is true.
     * For a enum condition it should return 0, 1, ... subsequently till {@link #sizeCond()} -1
     * @return
     */
    int ixCond ();
    
    /**Returns 2 for boolean, >2 for a enum condition
     * @return
     */
    int sizeCond ();
    
    /**The associated FBlock which contains the pin(s) more possible.
     * The FBlock is necessary to detect whether a PinCond with true and a next one with false
     * is associated to the same FBlock. 
     * @return
     */
    FBcond fbCond ();
    
    /**Name of the condition inside the FBcond for textual output.
     * @return readable "nameFBlock.namePin"
     */
    String nameFBcond ();
    
    
    
    /**A possible name for the condition for textual output (report)
     * @return readable name
     */
    String nameCond ();
  }
  
  
  /**Describes an instance which contains some condition pins. 
   * Typical boolean, then {@link #getCondition(int)} delivers 2.
   *
   */
  public interface FBcond {
    
    /**Add a pin
     * @param ix should be proper to the implementation instance. Typical 0 for false and 1 for true.
     * @param cond the condition as Pin
     * @return true if sucessfull.
     */
    boolean addCondition ( int ix, PinCond cond);
    
    /**Get the pin due to the ix
     * @param ix 0 or 1 for boolean
     * @return null if invalid ix
     */
    PinCond getCondition ( int ix); 
    
  }
  
  
  
  /**Inner class for instances to describe a simple or even nested complex conditions.
   * It contains
   * <ul><li>{@link #pinCond} if the condition is defined only by this one pin
   * <li>{@link #listCond} If the condition is OR or AND related with more as one pin. 
   * <li>{@link #bAnd} dedicates whether the items in {@link #listCond} are AND or OR related.
   * <li>{@link #hash} the hash to find out unique instances if the same content is given.
   * </ul>
   * Nested condiitons:
   * <ul>
   * <li>If {@link #listCond} is used, the list can contain either single pins, 
   *   means the contained {@link Cond} items have only set {@link #pinCond}. 
   *   Then this element is not nested.
   * <li>Or the {@link Cond} in the {@link #listCond} contains by itself again a {@link #listCond}.
   *   Then it is nested.
   * </ul>  
   * Usual AND and OR relations should be alternate, or better a canonical form with only two nested levels
   * should be used, first OR, second AND. But the algorithm works for all combinations.
   * But AND and OR can be given even in any order and kind. 
   * The operation {@link ConditionExpression#buildTrueTable(Cond, long[], long[])} regard all AND and OR in any kind.  
   *
   */
  public static class Cond implements ToStringAppend {
    
    
    public final static long kHashAddOR = 0x0200000200000000L;
    
    public final static long kHashAddAND = 0x0300000300000000L;
        

    
    /**One member of a superior ListCond or the only one Evout which determines the condition.
     */
    private PinCond pinCond;
    
    /**If not null, then contains more as one pinCond in the sub List or just a deeper sublist.
     * The members are AND or OR related due to {@link #bAnd}
     */
    private List<Cond> listCond;

    /**true then elements in 'listCond' builds an AND for events contributing,
     * used for a Join_OFB output and its following chain.
     * false then elements in 'listCond' builds an OR for events contributing,
     * used for more as one evout connected to one evin.  
     */
    public final boolean bAnd;
    
    /**This is the hash to use for a superior instance which contains this {@link Cond},
     * or it is even usable for unified instances of this. 
     * <ul>
     * <li>The hash should be build by the sum of all hashs of sub {@link #listCond}
     * which contains {@link #kHashAddAND} or {@link #kHashAddOR} proper to the {@link #bAnd}
     * plus all hashs of the contained {@link Cond} elements.
     * <li>or the hash is build by the {@link System#identityHashCode(Object)} of the {@link #pinCond} instance 
     * plus {@link #kHashAddAND} as initialize in the ctor {@link Cond#Cond(PinCond).
     * 
     */
    private long hash;
    
    /**Creates an instance with a {@link #listCond} for an expression condition.
     * @param bAnd true then the elements in {@link #listCond} are AND related, false then OR related.
     */
    public Cond (boolean bAnd) {
      this.bAnd = bAnd;
      this.listCond = new LinkedList<>();
      this.pinCond = null;
      this.hash =  bAnd ? kHashAddAND : kHashAddOR;  // differ AND or OR in hash as base value
    }
    
    /**Creates an instance usual as member of a superior {@link #listCond} with only this {@link PinCond} as condition. 
     * It is also possible that this is the first condition, and later some more will be added as AND related. 
     * Then it should be an AND list, because OR related events are added via an extra list, not as one event.
     * If {@link #addPin(PinCond)} is called for this list, then the {@link #pinCond} is transferred into the {@link #listCond},
     * not removed (it is final) but ignored because {@link #listCond} is not null and is prior used.
     * @param pinCond
     */
    public Cond (PinCond pinCond, boolean bAnd) {
      this.bAnd = bAnd;                              // a next added event is AND related
      this.listCond = null;                          // not used, maybe later defined in addPin(...)
      this.pinCond = pinCond;                        // the relevant info
      this.hash = System.identityHashCode(pinCond);   // hash is the simple of pin
    }
    
    
    /**Clones a {@link Cond} with the same hash, to add more entries 
     * <ul>
     * <li>A {@link #listCond} is created, left empty if src has no members.
     * <li>If the src contains {@link Cond#pinCond} then this is added in the {@link #listCond} as entry.
     * <li>All entries in a given src.{@link #listCond} are added to the {@link #listCond}.
     * <li>It's aware, it does not change referenced instances nor provides referenced instances to change.
     *   But this clone is given to change the own {@link #listCond}.
     * </ul>
     * @param src the src to clone.
     */
    public Cond (Cond src) {
      this.bAnd = src.bAnd;                              // a next added event is OR related
      this.listCond = new LinkedList<>();                          // not used, maybe later defined in addPin(...)
      this.pinCond = null;
      if( src.listCond !=null) {                     // the relevant info
        this.listCond.addAll(src.listCond);          // add all sub lists, let it unchanged
      } 
      else if( src.pinCond !=null) {      //---------vv listCondSrc.pinCond only given:
        Cond entry = new Cond(src.pinCond, true);  // add the pinCond as entry in this
        this.listCond.add(entry);
      }
      this.hash = src.hashCode();                        // hash is the same as we would have an OR-list
    }
    
    
    /**Returns a new Condition which does not contain the contribution of 'condAnd' 
     * in its given {@link #listCond}.
     * @param condAnd
     * @return this if nothing is reduced, else a new instance.
     */
    public Cond reduceCond (Cond condAnd) {
      List<Cond> listCondReduced = new LinkedList<>();
      boolean bReduce = false;
      long hash = this.bAnd ? kHashAddAND : kHashAddOR;
      for(Cond cond1 : this.listCond) {
        if(cond1.hash == condAnd.hash) { // same CondPin contained:
          bReduce = true;
        } else {
          listCondReduced.add(cond1);
          hash += cond1.hash;
        }
      }
      if(bReduce) {
        if(listCondReduced.size() ==0) {
          return null;             //<<------------------------ no condition necessary
        } else {  
          Cond ret = new Cond(this.bAnd);  // with an empty List
          ret.listCond = listCondReduced;
          ret.hash = hash;
          return ret;
        }
      } else {
        return this;               //<<--- unchanged, nothing reduced.
      }
    }
    
    
    public PinCond pinCond() { return this.pinCond; }
    
    
    public List<Cond> iterSublist () { return this.listCond; }
    
    public List<Cond> listCond () { return this.listCond; }
    
    public long hash () { return this.hash; }
    
    /**Add a pinAdd to the given list as a single contribution. 
     * See also {@link #addCond(Cond)} to add an expression contribution, a sub list.
     * This event is AND or OR related, depending on {@link #bAnd}, with the other entries in the own {@link #listCond}.
     * <br>If the instance are first created with {@link #ConditionExpression(PinCond, boolean)}
     * then the {@link #pinCond} is removed and a {@link #listCond()} is created with the primary given {@link #bAnd}.
     * @param pinAdd
     */
    public void addPin(PinCond pinAdd) {
      if(this.listCond == null && this.pinCond !=null) { //vv given: only pinCond is set as single entry
        this.listCond = new LinkedList<>();                  // this should be a part of the list.
        this.hash = (this.bAnd ? kHashAddAND : kHashAddOR);       // build hash new
        Cond entry1 = new Cond(this.pinCond, true);                // builds an entry with only this event, formal a ListCond
        this.pinCond = null;                                 // remove it because contained in listCond
        this.listCond.add(entry1);                           // the given this.pinCond is not removed, but no more used.
        this.hash += entry1.hash;                             // starts hash new, hash of the list entry1, containing OR information
      }
      Cond entry = new Cond(pinAdd, true);                         // builds an entry with only this event, formal a ListCond
      this.listCond.add(entry);
      this.hash += entry.hash;                               // adds the hash of the list entry, containing OR information
    }
 
    /**Add a condition to the given list as a expression contribution. 
     * See also {@link #addPin(PinCond)} to add a single contribution.
     * This sub list is AND or OR related, depending on {@link #bAnd}, with the other entries in the own {@link #listCond}.
     * @param cond null admissible, then nothing is added
     */
    public void addCond(Cond cond) {
      if(this.listCond == null) { this.listCond = new LinkedList<>(); }
      if(cond !=null) {
        this.listCond.add(cond);
        hash += cond.hash;
      }
    }
    
    public void addAll ( Cond src) {
      Debugutil.stopp();
    }
    
    
    @Override public String toString() {
      if(this.listCond == null && this.pinCond !=null) { return this.pinCond.nameFBcond(); }
      else {
        StringBuilder sb = new StringBuilder();
        try { toStringAppend(sb); } catch(IOException exc) {}
        return sb.toString();
      }
    }


    @Override public Appendable toStringAppend ( Appendable app, Object... cond ) throws IOException {
      if(this.listCond !=null) {
        String sep = "(";
        for(Cond list: this.listCond) {
          app.append(sep);
          list.toStringAppend(app);                          // recursively call
          sep = this.bAnd ? " && " : " || ";
        }
        app.append(")");
      } else if(this.pinCond !=null) { 
        app.append(this.pinCond.nameFBcond()); 
      }
      
      return app;
    }
    
  }

  
  
  
  

  
  /**This list contains all used conditions in their FBlocks.
   * The list is filled in {@link #addCondition() } with new incoming PinCond.
   * It is used in #get
   */
  private List<FBcond> listFBcond = new LinkedList<>();
  
  /**True, false or both, or multiple pins */
  private int[] valuesFBcond = new int[6];
  
  private List<FBcond> listFBcondRemoved = new LinkedList<>();



  public ConditionExpression () {
  }
  
  
  
  /**Copy constructor to enhance a condition in the first level.
   * @param src
   */
  public ConditionExpression (ConditionExpression src) {
    for(FBcond fbExpr : src.listFBcond) {
      this.listFBcond.add(fbExpr);
    }
  }
  
  
  
  
  /**Builds a table of bits, known as true table, with a given maybe complex (nested) condition.
   * <ul><li>The return value for a simple A & B returns 0x8888888888888888, only the 4 last bits are relevant.
   * <li>Return for A | B is 0xeeeeeeeeeeeeeeee (three bits are set in bit 3..0)
   * <li>Return for A & B || ~A & ~B is 0x9999999999999999
   * <li>More complex conditions are adequat processed.
   * <li>The {@link #listFBcond} is filled in order of seen {@link Cond#pinCond()},
   *   adequate {@link #valuesFBcond} is filled. 
   * <li>If {@link PinCond} are only given with one value true or false, then {@link #valuesFBcond} will contain
   *   only a single bit set in each index, and {@link #needsClean()} returns false.
   *   This detects, that {@link #getCondition(long[], Object)} will be return the same expression as given in 'cond',
   *   (maybe with changed nesting), and it may be not necessary to call it. 
   * </ul> 
   * @param cond the given condition
   * @return the true table of the condition, only with one element if the number of different {@link PinCond}
   * does not exceed 6 different true/false pins. 
   * If there are more, then an array is returned. TODO not implemented yet.
   */
  public long[] buildTrueTable (Cond cond) {
    return buildTrueTable(cond, null, null, 0);
  }  
  
  
  /**Builds a table of bits, known as true table, with a given maybe complex (nested) condition.
   * <ul>
   * <li>This operation is called recursively for the nested levels in {@link Cond#listCond}.
   * <li>If {@link Cond#listCond()} is null, then the only one condition contained in {@link Cond#pinCond}
   *   is combined to the given bits in tblSum.
   * <li>If {@link Cond#listCond} is given, but the entries in 
   * </ul>
   * @param cond The given condition, null if tblPrev is given
   * @param tblSum
   * @param tblPrev result from the deeper level
   * @param bAnd
   * @return
   */
  private long[] buildTrueTable (Cond cond, long[] tblSum, long[] tblPrev, int recursive) {
    if(recursive > 100) {
      throw new IllegalArgumentException("too many recursions");
    }
    long[] ret = tblSum;
    if(cond.listCond !=null) {  //===========================vv the cond contains not only one pin:
      // first add all sub condition lists
      for(Cond condSub : cond.iterSublist()) { 
        long[] tblSub = null;
        if(condSub.listCond !=null) {   //===================vv first go recursively in the deepest level
          tblSub = buildTrueTable(condSub, tblSub, tblPrev, recursive+1);
          ret = addCondition(null, ret, tblSub, cond.bAnd);
        }
      }
      //
      for(Cond listSub : cond.iterSublist()) {  //===========vv then add immediately given conditions of this level
        if(listSub.pinCond !=null) {
          ret = addCondition(listSub.pinCond, ret, null, cond.bAnd);
          assert(listSub.iterSublist() ==null);
        }
      }
    }                            //==========================^^ not only one pin 
    else if(cond.pinCond !=null) {  //=======================vv condition contains only one pin, this is anyway on the deepest level
      ret = addCondition(cond.pinCond, ret, tblPrev, cond.bAnd);  // add the true table bits of this pin
      assert(cond.iterSublist() ==null);
    }
    return ret;
  }
  
  
  
  
  public long[] addCondition (PinCond evCond, long[] condSum, long[] condPrev, boolean bAnd) {
    long[] condEv = buildMaskFromCond(evCond, condPrev);
    if(condSum == null && condEv !=null) { //--------vv initial: set the result of condition
      return condEv;
    } else if(condEv !=null ){
      int zSum = condSum.length;                 // increase the condSum to the length of mask
      int zix = condEv.length > condSum.length ? condEv.length : condSum.length;
      if(condEv.length > condSum.length) {
        condSum = Arrays.copyOf(condSum, zix);
        do {
          for(int ix = 0; ix < zSum; ++ix) {
            long bits = condSum[ix];
            condSum[zSum] = bits;
            zSum +=1;
          }
        } while(zSum < zix);
      }
      for(int ix = 0; ix < zSum; ++ix) {          //--------vv build condSum with new given condition in mask.
        int ixm = ix % condEv.length;           //<<-------- repeat access to lower elements if mask is shorter
        if(bAnd) {
          condSum[ix] &= condEv[ixm];                  // the AND of results
        } else {
          condSum[ix] |= condEv[ixm];                  // the OR of results
        }
      } // for
      return condSum;
    }
    else {
      return condSum;   // nothing new
    }
  }
  
  
  
  
  
  //ffd7 = cond4.true || (cond4.false && cond3.true && cond2.true && cond1.true || 
  /**
   * @param condSum
   * @param evTest
   * @return null if condSum==null, no condition given
   */
  public Cond getCondition (long[] condSum, Object evTest) {
    if(evTest instanceof PinCond && ((PinCond)evTest).nameFBcond().equals("cd_X.prep")) Debugutil.stopp();
    if(condSum == null) return null;
    long val = condSum[0];
    int nCond = this.listFBcond.size() -1;
    Cond retCond = null; 
    long valCurr = val;
    long valAnd = val;
    do {            //=======================================vv first process all values which are pure OR related.
      long mask = maskFalse(nCond);   // it is maskFalse but even maskTrue for shifted bitpos
      long maskTrue = maskTrue(nCond);
      int bitPos = bitpos(nCond);
      long valFalse = valCurr & mask;
      long valTrue = (valCurr>>bitPos) & mask;
      FBcond fbx = this.listFBcond.get(nCond);
      if(valTrue == valFalse) {          //------------------vv this condition is no more used.
        this.listFBcondRemoved.add(fbx);                     // this fbCond is no more relevant.
        // NOTE: do not change this.listFBcond because the position of the other fbCond in the list is relevant. 
      } else {                           //------------------vv condition is used, with AND or immediately
        long valResult = valCurr;
        if(valTrue == mask) {                     //---------vv this pin is completely 1 in all combinations:
            assert(valFalse != mask);  // because valTrue != valFalse
            PinCond pin = fbx.getCondition(1);
            if(retCond == null) { retCond = new Cond(false); } // a new OR table necessary
            retCond.addPin(pin);                      // add it as solitary
            valAnd &= ~maskTrue;                             // remove all these bits from the true table of possible combination bits
            valResult &= ~maskTrue;                          // maskTrue is more left in respect to mask
            valResult |= valResult << bitPos;                // replace maskTrue bits with maskFalse bits, copy the remaining false values to the maskTrue position for further compare.
        } 
        if(valFalse == mask) {
            assert(valTrue != mask);    // because valTrue != valFalse
            PinCond pin = fbx.getCondition(0);
            if(retCond == null) { retCond = new Cond(false); } // a new OR table necessary
            retCond.addPin(pin);
            valAnd &= ~mask;
            valResult &= ~mask;                 // copy the remaining true values to the maskFalse position for further compare.
            valResult |= valResult >>> bitPos;        // hint: use logic shift, left side a 00 should be inserted.
        }
        valCurr = valResult;
      }
    } while(--nCond >=0);
    //
    Debugutil.stop();
    valCurr = valAnd;
    valCurr = 0;
    
    
    retCond = buildCondAnd(valAnd, retCond);
    return retCond;
  }

  
  
  /**Checks whether a pin is currently acitve in the condition
   * after build the new {@link Cond} with {@link #getCondition(long[], Object)}.
   * @param pin
   * @return
   */
  public boolean containsPinCond (PinCond pin) {
    FBcond fb = pin.fbCond();
    if(this.listFBcondRemoved.contains(fb)) {
      return false;                     //<<------------------- return for removed pin
    } 
    else if(this.listFBcond.contains(fb)) {
      return true;                      //<<------------------- return for containing pin
    }
    else {
      return false;                     //<<------------------- return for unknown pin
    }
  }
  
  
  
  /**Returns false, if any FBcond is only contained with one PinCond as its contribution.
   * Then it means clean is not necessary.
   * @return true if at least one {@link FBcond} is presented with more as one {@link PinCond}.
   *   It means clean should be done.
   */
  public boolean needsClean () {
    for(int value: this.valuesFBcond) {
      if(value == 3) {     //TODO maybe enhanced, check whether more as one bit is set.
        return true;
      }
    }
    return false;
  }
  

  /**Simple check each bit and build condition with AND of all relevant conditions.
   * @param valArg remaining bits after grouping bits are processed.
   * @param nCond
   * @param listCondArg
   * @param recursive
   */
  private Cond buildCondAnd (long valArg, Cond condArg) {
    Cond retCond = condArg;   // maybe null
    int nCondMax = this.listFBcond.size()-1;
    int nCond = nCondMax;
    long valCurr = valArg;
    List<Integer> listnCond = new LinkedList<>();            // number of condition to exclude
    long maskVal = 0xffffffffffffffffL;
    do {            //=======================================vv first process all values which are pure OR related.
      long mask = maskFalse(nCond);   // it is maskFalse but even maskTrue for shifted bitpos
      long maskTrue = maskTrue(nCond);
      int bitPos = bitpos(nCond);
      long valFalse = valCurr & mask & maskVal;
      long valTrue = (valCurr>>bitPos) & mask & maskVal;
      if(valTrue == valFalse) {                  // valuetrue == valFalse, then this condition is not used.
        valCurr &= ~maskTrue;                    // remove the true part, not relevant to explore the bit.
//        valCurr |= 1L<<bitPos;
        maskVal &= ~maskTrue;
        listnCond.add(nCond);         // for this nCond same values for true and false
      } else {
        
      }
    } while(--nCond >=0);
    long mBit = 1L<<bitpos(nCondMax+1);
    while( (mBit>>>=1) !=0) {
      if( (valCurr & mBit) !=0) {
        Cond listCondAnd = null;
        Iterator<Integer> iternCond = listnCond.iterator();
        int nCondExclude = iternCond.hasNext() ? iternCond.next() : -1;
        for(nCond = nCondMax; nCond >=0; --nCond) {
          if(nCond == nCondExclude) {   // if before was detected, true and false are equal, then ignore this entry.
            nCondExclude = iternCond.hasNext() ? iternCond.next() : -1;
          } else {                      //===================vv one combination bit in the trueTable detected which is not double existing for false
            if(listCondAnd == null) {                        // then we need a AND condition term for this combination
              listCondAnd = new Cond(true); 
              if(retCond == null) { retCond = new Cond(false); } // a new OR table necessary
              retCond.addCond(listCondAnd);
            }
            long maskFalse = maskFalse(nCond);               // check with all possible combination which true/false is it. 
            long maskTrue = maskTrue(nCond);
            if( (mBit & maskFalse) !=0) {
              FBcond fbx = this.listFBcond.get(nCond);
              PinCond pin = fbx.getCondition(0);
              listCondAnd.addPin(pin);
            } else {
              if( (mBit & maskTrue) !=0) {
                FBcond fbx = this.listFBcond.get(nCond);
                PinCond pin = fbx.getCondition(1);
                listCondAnd.addPin(pin);
              }
            }
          }
        }
      }
    }
    return retCond;
  }
  
  
  
  
  
  
  
  private long maskFalse (int nrCond) {
    switch(nrCond) {
    case 0: return 0x5555555555555555L;
    case 1: return 0x3333333333333333L;
    case 2: return 0x0f0f0f0f0f0f0f0fL;
    case 3: return 0x00ff00ff00ff00ffL;
    case 4: return 0x0000ffff0000ffffL;
    case 5: return 0x00000000ffffffffL;
    default: return 0;
    }
  }
  
  
  private long maskTrue (int nrCond) {
    switch(nrCond) {
    case 0: return 0xaaaaaaaaaaaaaaaaL;
    case 1: return 0xccccccccccccccccL;
    case 2: return 0xf0f0f0f0f0f0f0f0L;
    case 3: return 0xff00ff00ff00ff00L;
    case 4: return 0xffff0000ffff0000L;
    case 5: return 0xffffffff00000000L;
    default: return 0;
    }
  }
  
  
  private int bitpos (int nrCond) {
    switch(nrCond) {
    case 0: return 1;
    case 1: return 2;
    case 2: return 4;
    case 3: return 8;
    case 4: return 16;
    case 5: return 32;
    default: return 0;
    }
  }
  
  
  /**Builds a mask from the given condition with the determined event pins in {@link #idxEventPosBit}.
   * <ul>
   * <li>If only one condition evout exists and this is the first call, 
   *   then the mask is set to 0xaaaaaaaaaaaaaaaa for a true bit (ExprEv_OFB.true) or 0x5555555555555555 for a false bit.
   * <li>If only one condition evout exists and this is the second used evout in {@link #idxEventPosBit},
   *   then the mask is set to 0xcccccccc for a true bit (ExprEv_OFB.true) or 0x33333333 for a false bit.
   * <li>If only one condition evout exists and this is the next used evout in {@link #idxEventPosBit},
   *   then the mask is set to:
   *   <ul>
   *   <li>0x5555555555555555 for a false, 0xaaaaaaaaaaaaaaaa for a true for 1th.
   *   <li>0x3333333333333333 for a false, 0xcccccccccccccccc for a true for 2th.
   *   <li>0x0f0f0f0f0f0f0f0f for a false, 0xf0f0f0f0f0f0f0f0 for a true for 3th.
   *   <li>0x00ff00ff00ff00ff for a false, 0xff00ff00ff00ff00 for a true for 4th.
   *   <li>0x0000ffff0000ffff for a false, 0xff00ff00ff00ff00 for a true for 5th.
   *   <li>0x00000000ffffffff for a false, 0xffffffff00000000 for a true for 6th.
   *   <li>{ 0x0, 0xffffffffffffffff} for false, { 0xffffffffffffffff, 0x0} for 7th.
   *   <li>{ 0,0,-1, -1} for false, { -1, -1, 0,0} for true for 8th (note: -1 is 0xffffffffffffffff)
   *   <li>{ 0,0,0,0,-1,-1,-1,-1} ... etc. for more bits.
   *   <li>It means for 16 conditions an array of 1024 elements is returned,
   *     for 24 conditions, 2^18 elements are returned (~260000)
   *   <li>usual 2..5 conditions are used for which one element as long is sufficient.  
   *   </ul>
   * <li>If the 'evPins' contains more driving condition bits, then the relation AND or OR with these bits
   *   are built. It means for example if the 1th, and 3th evout is found with each true,
   *   then the resulting mask is: <pre>
   *   0xaaaaaaaaaaaaaaaa
   *   0xf0f0f0f0f0f0f0f0
   *   ------------------
   * = 0xfafafafafafafafa</pre>
   *   The resulting mask is built by recursively call of this same operation for all found 'evPins'
   *   from the evout which have conditions, with its {@link Pin_FBcl#evpinsChain()}. 
   *   This presents a true table with the values  <pre>
   *   c b a 
   *   1 b 1
   *   c b a  OR
   *   0 b 0  b
   *   0 b 1  1
   *   0 b 0  b
   *   0 b 1  1  as mask, b is hold free in the mask
   *   1 b 0  1  it is 11111010 = 0xfa
   *   1 b 1  1  read from back, 
   *   1 b 0  1  ^
   *   1 b 1  1  | </pre> which is presented by this 8 lower bits 0xfa: <pre>
   * 
   * <li>Adequate example if the 1th, and 3th evout is found with each true and false (negated),
   *   but and related, then the resulting mask is: <pre>
   *   0xaaaaaaaaaaaaaaaa
   *   0x0f0f0f0f0f0f0f0f
   *   ------------------
   * = 0x0a0a0a0a0a0a0a0a</pre>
   *   The resulting mask is built by recursively call of this same operation for all found 'evPins'
   *   from the evout which have conditions, with its {@link Pin_FBcl#evpinsChain()}. 
   *   This presents a true table with the values  <pre>
   *   c b a 
   *   0 b 1</pre>
   *   which is presented by this 8 lower bits 0xfa: <pre>
   *  ~c b a  AND
   *   0 b 0  0
   *   0 b 1  b
   *   0 b 0  0
   *   0 b 1  b  as mask, b is hold free in the mask
   *   1 b 0  0  it is 00001010 = 0x0a
   *   1 b 1  0  read from back, 
   *   1 b 0  0  ^
   *   1 b 1  0  |
   *   </pre>  
   * </ul>   
   * @param evPins given condition as possible complex expression
   * @param maskPrev mask given from recursively call. It should be null for the first recursion
   * @param maskResult the Mask with bits from the condition
   * @param recursion only for catastrophical break, yet max, 24 recurions, typical 2..3
   * @return the resulting mask 
   */
  private long[] buildMaskFromCond(PinCond pinAdd, long[] maskPrev) {
    if(pinAdd == null || pinAdd.fbCond() == null) {
      return maskPrev;
    }
    final long[] mask;
    FBcond fbCond = pinAdd.fbCond();                  // ExprEv_OFB with true or false event:
    boolean bFalse = pinAdd.ixCond() ==0;
    assert(bFalse || pinAdd.ixCond() ==1);
    if(bFalse) { 
      fbCond.addCondition(0, pinAdd);
    } else { 
      fbCond.addCondition(1, pinAdd);
    }
    int ixFbx = ixFBcondAdd(pinAdd);
    int bitPos1 = bitpos(ixFbx);
    long[] maskC;
    if(bitPos1 <= 32 ) {
      maskC = new long[1];
      maskC[0] = (1L<< bitPos1) -1;                     // results in 0x1, 0x3, 0x0f, 0x00ff etc. 
      if(!bFalse) { maskC[0] <<= bitPos1; }             // for true:  0x2, 0xc, 0xf0, 0xff00 etc. 
      while( (bitPos1*=2)< 64) {                          // expands to 0xaaaaaaaa, 0xcccccccc, 0xf0f0f0f0 etc. 
        maskC[0] = maskC[0] | maskC[0] << bitPos1;
      }
    } else {
      int zMaskC = 1 << (bitPos1 >>5);                  // bit > 32 needs long[2] for 64 etc.
      if(zMaskC <= 256) { 
        maskC = new long[zMaskC];                       // max for 14 condition evout, more is not possible.
        int ix1 = bFalse ? bitPos1 >>6 : bitPos1>>5;    // for true:  0x2, 0xc, 0xf0, 0xff00 etc. 
        //for() TODO
      } else {
        // too much, yet exception
        maskC = null;
      }
    }
    if(maskPrev == null) {
      mask = maskC;
    } else {
      int zix = maskC.length > maskPrev.length ? maskC.length : maskPrev.length;
      mask = new long[zix];
      for(int ix = 0; ix < zix; ++ix) {
        int ixC = zix % maskC.length;
        int ixP = zix % maskPrev.length;
        mask[ix] = maskC[ixC] & maskPrev[ixP];                            // combine with the yet given mask from recursively call before
    } }
    return mask;
  }

  
  
  
  /**Register or get the index to the {@link FBcond} in {@link #listFBcond}
   * and register true and false in {@link #valuesFBcond}.
   * @param pinAdd the given pin.
   * @return
   */
  private int ixFBcondAdd (PinCond pinAdd) {
    int ixFBx = 0;
    for(FBcond fbx: this.listFBcond) {
      if(pinAdd.fbCond() == fbx) {
        break;
      }
      ixFBx +=1;
    }
    if(ixFBx >= this.listFBcond.size()) {
      this.listFBcond.add(pinAdd.fbCond());
    }
    if(this.valuesFBcond.length <= ixFBx) {
      this.valuesFBcond = Arrays.copyOf(this.valuesFBcond, ixFBx+1);
    }
    this.valuesFBcond[ixFBx] |= 1<< pinAdd.ixCond();
    return ixFBx;    
  }
  
  
}
