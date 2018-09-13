package org.vishia.test;

import org.vishia.util.Debugutil;

/**In this class the numeric is tested (division by zero, mult 32 bit etc. to start in debug.
 * @author Hartmut Schorrig
 *
 */
public class TestArithmetik
{

  
  
  
  public static void main(String args[]) {
    short a=25000, b=4;
    int y = a*b;      //mult with 32 bit though inputs are 16 bit
    
    int c=25000000, d=4000;
    long y2=c*d;      //mult with32 bit too, result is faulty
    
    float f=-1.0f, g=0;
    float y3 = f/g;   //result is -infinity, no exception. It is ok
    
    float y31 = 0.1f*y3;  //y31 -inifinity too.

    int m=10000, n=0;
    int y5=m / n;    //abort because numeric exception, only float works with infinity.

    
    
    Debugutil.stop();
  }
}
