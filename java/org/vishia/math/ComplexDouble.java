package org.vishia.math;

public class ComplexDouble {

  public double re,im;
  
  public ComplexDouble(double re, double im) {
    this.re = re; this.im = im;
  }
  
  public void set(double re, double im) {
    this.re = re; this.im = im;
  }
  
  public double magn() { return Math.sqrt(this.re*this.re + this.im*this.im); }
  

}
