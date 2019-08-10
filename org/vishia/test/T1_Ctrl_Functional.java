package org.vishia.test;

public class T1_Ctrl_Functional {

  final double kT1;

  T1_Ctrl_Functional(double T1, double Tstep){
    kT1 = T1 < Tstep ? 1.0 : Tstep/T1;
  }

  double calc(double x, double y_last) {
    return y_last +  kT1 * (x - y_last);
  }
  
  
  double calcRecursive(double x, double y_last) {
    return Math.abs(x-y_last) < 0.1 
         ? y_last
         : calcRecursive(x, calc(x, y_last));
  }
  
  
  
  public static void main(String[] args) {
    T1_Ctrl_Functional t1 = new T1_Ctrl_Functional(10, 1);
    double r = t1.calcRecursive(1, 0);
    double x = 1.0;
    double y_start = 0;
    double y_end = t1.calc(x, t1.calc(x, y_start));
    
  }
}

