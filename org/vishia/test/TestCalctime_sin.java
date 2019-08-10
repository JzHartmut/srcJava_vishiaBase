package org.vishia.test;

import org.vishia.util.Debugutil;

public class TestCalctime_sin
{
  public static class Double_ab { double a; double b;}

  
  private final Double_ab[] y;

  public TestCalctime_sin(){
    y = new Double_ab[5000];
    for(int i = 0; i < y.length; ++i){ y[i] = new Double_ab(); }
    
  }
  
  
  public static void fourier(int nHarmon, Double_ab y) {
    //Double_ab y = new Double_ab();
    double signal;
    int ii=0;
    double w = 0.0f;
    double dw = Math.PI / 6000.0f * nHarmon;
    do {
      if(ii < 6000){ signal = 0.0f; }
      else if(ii < 9000){ signal = 1.5f; }
      else { signal = 1.0f; }
      y.a += signal * Math.cos(w);
      y.b += signal * Math.sin(w);
      w += dw;
      if(w > Math.PI) { w -= 2* Math.PI; }
    }while(++ii < 12000);
    
    //return y;
  }


  public void fourier(int nHarmon){
    fourier(nHarmon, y[nHarmon]);
  }
  

  
  public static int testatan2() {
    double a = -2, b=-0.00001;
    double angle1 = Math.atan2(b,a);
    double angle2 = Math.atan2(b,a)/Math.PI;
    int angle = (int)(Math.atan2(b,a)/Math.PI * 0x40000000 * 2);
    String hex = Integer.toHexString(angle);
    //
    double magn = Math.sqrt(a*a + b*b);
    
    double an = Math.cos(angle1);
    double bn = Math.sin(angle1);
    
    double a2 = an * magn;
    double b2 = bn * magn;
    
    Debugutil.stop();
    return angle;
  }
  
  
  
  
  /**
   * @param currmax
   * @param value
   * @return
   */
  private static int retmax(int currmax, int value) {
    if(value > currmax) return value;
    else return currmax;
  }
  
  
  public static int searchMax() {
    int[] values = { 2,5,4,9,3,6 };
    final int max1 = retmax(retmax(retmax(retmax(retmax(retmax(0, values[0])
    , values[1]), values[2]), values[3]), values[4]), values[5]);
    
    int max2 = 0;
    for(int value: values) {
      max2 = retmax(max2, value);
    }
    return max2;
  }
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  

  public static void main(String[] args)
  {
    searchMax();
    
    testatan2();
    
    float clock = 0.001f;
    
    Double_ab[] y = new Double_ab[5000];
    for(int i = 0; i < y.length; ++i){ y[i] = new Double_ab(); }
    int i;
    float dtime1;
    System.out.printf("Start...\n");
    long timestart = System.nanoTime();
    for(i=1; i<5000; ++i){
      fourier(i, y[i]);
    }
    { long timeend = System.nanoTime();
      dtime1 = (timeend - timestart) * clock;
    }
    System.out.printf("Tclock = %f us, T1 = %f ms\n", clock, dtime1/1000);
  }
  
  
}
