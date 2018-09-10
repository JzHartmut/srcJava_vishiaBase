package org.vishia.test;

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
  
  
  

  public static void main(String[] args)
  {

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
