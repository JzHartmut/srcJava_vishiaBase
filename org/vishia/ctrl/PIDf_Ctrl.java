package org.vishia.ctrl;

/**The sources are only quick copied and adapted from emC
 * @author hartmut
 *
 */
public class PIDf_Ctrl {

  
  
  
  public static class ParFactors {

    
    float kP;

    /**Smoothing time for D-Part.*/
    float fTsD;

    /**Factor for D-Part including kP and Transformation to int. */
    float fD;

    /**Factor for wxP for Integrator adding. */
    long fI;
    
    /**Factor from float-x to long and to float-y-scale. */
    float fIx, fIy;

    /**Maximal value for the y output. The integrator in the PID uses fix point 64 bit for high accuracy.
     * This value is used to build the correct factors from float to fix point. 
     */
    float yMax;
  }
  
  
  
  
  public static class Par
  {

    
    /**for debugging and check: The used step time for calcualation of the factors. */
    float Tctrl;

    /**Maximal value for the y output. The integrator in the PID uses fix point 64 bit for high accuracy.
     * This value is used to build the correct factors from float to fix point. 
     */
    float yMax;

    /**Primary and used parameter: P-gain. */
    float kP;
    
    /**Primary parameter: Nachstellzeit. used: fI. */
    float Tn;
    
    float Td;
    
    float T1d;

    /**Internal paramter depending factors. */
    ParFactors i = new ParFactors();   //the composite instance is here

    int dbgct_reparam;


    /**If set then changes from outside are disabled. For Inspector access. */
    boolean man;

    /**
     * @param Tstep not used here only for simulation systems to determine its step time.
     */
    public Par ( float Tstep)
    { 
      
    }


    
    public boolean init ( float Tctrl_param, float yMax_param
        , float kP, float Tn, float Td, float Tsd, ParFactors[] parFactors_y)
     { //check before cast:
       this.Tctrl = Tctrl_param;
       this.yMax = yMax_param;
       //setInitialized_ObjectJc(&this.base.obj);
       set(kP, Tn, Td, Tsd, parFactors_y);
       return true;
     }


    
    
    /**step of PID controller
    * @simulink Object-FB.
    */
    public void set(float kP, float Tn_param, float Td_param, float Tsd_param, ParFactors[] parFactors_y) {
      //TODO if(!isLocked_ObjectJc(&this.i.base.obj)) { //do not change the current parameter yet. 
        if(this.man == false) {
          this.kP = kP;
          this.Tn = Tn_param;
          this.Td = Td_param;
          this.T1d = Tsd_param;
          //this.limPbeforeD = 1;  //it is better to fast reach point with max controller output. 
        }
        this.i.kP = this.kP;
        this.i.yMax = this.yMax;
        this.i.fIy = this.yMax / (float)(0x40000000L);
        this.i.fIx = (float)(0x40000000L) / this.yMax;
        this.i.fI = this.Tn <=0 ? 0 : (long)(this.i.fIx * (this.Tctrl / this.Tn)); // * (float)(0x100000000LL));
        this.i.fTsD = this.T1d <= 0 ? 1.0f : 1.0f - (float)Math.exp(-this.Tctrl / this.T1d);
        this.i.fD = (this.Td / this.Tctrl);
        this.dbgct_reparam +=1;
        //TODO: lock_ObjectJc(&this.i.base.obj);
      //TODO }
      //Output the pointer anyway also in case of lock, it should be determined.
      if(parFactors_y !=null){ parFactors_y[0] = this.i; }  //use the reference to the prepared inner data as event data reference
        //if(man_y !=null) { *man_y = this.man ? 1 : 0; }
    }



    
  } 
  
  protected ParFactors parNew;
  
  /**Currently used factors. */
  protected ParFactors f;

  /**Current limitation of output. */
  protected float lim;


  protected float Tstep;

  /**Smoothed differential. */
  protected float dwxP;

  /**Stored for D-part, to view input. */
  protected float wxP;

  /**Stored for D-part, to view input. */
  protected float wxPD;

  /**To view output. It is the same value as y_y arg of [[step_PIDf_Ctrl_emC(...)]]. */
  protected float y;

  /**Value of the integrator. */
  protected long qI;

  /**Limited output from I fix point. To view. */
  protected int qIhi;

  /**Limited output from P and D fix point. To view. */
  protected int wxP32, wxPD32;
  
  /**Value of the differentiator. */
  protected float qD1;
  
  protected int dbgct_reparam;


  public PIDf_Ctrl(float Tstep)
  {
    //check before cast:
    //ASSERT_emC(CHECKstrict_ObjectJc(othiz, sizeof(PIDf_Ctrl_emC_s), refl_PIDf_Ctrl_emC, 0), "faulty ObjectJc",0,0 );
    //should be done outside! CTOR_ObjectJc(othiz, othiz, sizeof(PIDf_Ctrl_emC_s), refl_PIDf_Ctrl_emC, 0);
    //inner ObjectJc-based struct:
    //CTOR_ObjectJc(&this.f.base.obj, &this.f, sizeof(this.f), refl_ParFactors, 1);
    this.Tstep = Tstep;
    this.lim = 1.0f;
  }





  public boolean init(ParFactors par) {
    boolean bOk = par != null;
    if(bOk) {
      this.f = par;  //it is a memcpy
      //It cleans only a bit. The rest is done in another time slice.
      this.lim = par.yMax;
      //TODO: unlock_ObjectJc(&par->base.obj);
    //TODO: setInitialized_ObjectJc(&this.base.obj);
    }
    return bOk;
  }



  public void setLim(float yLim) {
    this.lim = yLim;
  }


  public void param ( ParFactors par) {
    //TODO: if(par !=null && memcmp(par, &this.f, sizeof(this.f)) !=0) { //both have same type!
      //only if there is a difference, set the event.
      this.parNew = par;
    //TODO: } else {
    //TODO:   unlock_ObjectJc(&par->base.obj); //should be unlocked, nobody does it elsewhere!
    //TODO: }
  }


  public float XXXXXXstep ( float wx, float[] y_y)
  {
    if(this.parNew !=null) { 
      //because of data consistence the new parameter factors
      //should be copied in the fast working thread.
      //It does not need a lot of time!
      this.f = this.parNew;  //it is a memcpy
      //It cleans only a bit. The rest is done in another time slice.
      //TODO: unlock_ObjectJc(&this.parNew->base.obj);
      this.parNew = null;  //remove this event information.
    }
    float wxP = wx * this.f.kP;
    //limit to max output.
    if (wxP > this.lim) { wxP = this.lim; }
    else if (wxP < -this.lim) { wxP = -this.lim; }
    else {} //reamin wxP

    
    float dwxP = wxP - this.wxP;  //on limitation the dwxP is 0. It is better for increasing the process signal on max output.
    this.dwxP += (this.f.fTsD * (dwxP - this.dwxP));
    this.wxP = wxP;  //store for differenzial and to inspect
    float wxPD = wxP + (this.f.fD * this.dwxP);  //+ D-Part.

    //limit P + D.
    if (wxPD > this.lim) { wxPD = this.lim; }
    else if (wxPD < -this.lim) { wxPD = -this.lim; }
    else {} //remain wxPD
    this.wxPD = wxPD;  //to inspect.

    int wxP32 = (int)(this.f.fIx * wxP);  //integer representation of wxP

    int wxPD32 = (int)(this.f.fIx * wxPD);     //has never an overflow because wxPD is limited.
    int yImin, yImax;
    //limit it to 24 bit
    if (wxP32 < 0) { yImin = -0x40000000 - wxP32; yImax =  0x40000000; }  //
    else {          yImax =  0x40000000 - wxP32; yImin = -0x40000000; }
    this.wxPD32 = wxPD32;  //to inspect
    long xdI = wxP32 * this.f.fI;
    long qI1 = this.qI + xdI;
    int qIhi = (int)(qI1 >> 32);
    if (qIhi > yImax) { qIhi = yImax; qI1 = ((long)qIhi) << 32; }
    else if (qIhi < yImin) { qIhi = yImin; qI1 = ((long)qIhi) << 32; }
    else {} //remain qIhi
    this.qI = qI1;
    this.qIhi = qIhi;
    this.y = this.f.fIy * (wxPD32 + qIhi);  //use hi part of integrator for output.
    if(y_y !=null) { y_y[0] = this.y; }
    return this.y;
  }


  public float y() { return this.y; }
  
  protected float yI() { return this.f.fIy * this.qIhi; }
  
  
  
  
  
  
  public float step ( float wx, float[] y_y)
  {
    if(this.parNew !=null) { 
      //because of data consistence the new parameter factors
      //should be copied in the fast working thread.
      //It does not need a lot of time!
      this.f = this.parNew;  //it is a memcpy
      //It cleans only a bit. The rest is done in another time slice.
      //TODO: unlock_ObjectJc(&this.parNew->base.obj);
      this.parNew = null;  //remove this event information.
    }
    float wxP = wx * this.f.kP;
    //limit to max output.
    if (wxP > this.lim) { wxP = this.lim; }
    else if (wxP < -this.lim) { wxP = -this.lim; }
    else {} //reamin wxP

    
    float dwxP = wxP - this.wxP;  //on limitation the dwxP is 0. It is better for increasing the process signal on max output.
    this.dwxP += (this.f.fTsD * (dwxP - this.dwxP));
    this.wxP = wxP;  //store for differenzial and to inspect
    float wxPD = wxP + (this.f.fD * this.dwxP);  //+ D-Part.

    //limit P + D.
    if (wxPD > this.lim) { wxPD = this.lim; }
    else if (wxPD < -this.lim) { wxPD = -this.lim; }
    else {} //remain wxPD
    this.wxPD = wxPD;  //to inspect.

    int wxP32 = (int)(this.f.fIx * wxP);  //integer representation of wxP

    int wxPD32 = (int)(this.f.fIx * wxPD);     //has never an overflow because wxPD is limited.
    int yImin, yImax;
    //limit it to 24 bit
    if (wxP32 < 0) { yImin = -0x40000000 - wxP32; yImax =  0x40000000; }  //
    else {          yImax =  0x40000000 - wxP32; yImin = -0x40000000; }
    this.wxPD32 = wxPD32;  //to inspect
    long xdI = wxP32 * this.f.fI;
    long qI1 = this.qI + xdI;
    int qIhi = (int)(qI1 >> 32);
    if (qIhi > yImax) { qIhi = yImax; qI1 = ((long)qIhi) << 32; }
    else if (qIhi < yImin) { qIhi = yImin; qI1 = ((long)qIhi) << 32; }
    else {} //remain qIhi
    this.qI = qI1;
    this.qIhi = qIhi;

    this.y = this.f.fIy * (wxPD32 + qIhi);  //use hi part of integrator for output.
    if(y_y!=null) { y_y[0] = this.y; }
    return this.y;
  }


  
  
  
}
