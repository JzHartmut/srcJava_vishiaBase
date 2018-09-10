/****************************************************************************
 * Copyright/Copyleft:
 *
 * For this source the LGPL Lesser General Public License,
 * published by the Free Software Foundation is valid.
 * It means:
 * 1) You can use this source without any restriction for any desired purpose.
 * 2) You can redistribute copies of this source to everybody.
 * 3) Every user of this source, also the user of redistribute copies
 *    with or without payment, must accept this license for further using.
 * 4) But the LPGL is not appropriate for a whole software product,
 *    if this source is only a part of them. It means, the user
 *    must publish this part of source,
 *    but don't need to publish the whole source of the own product.
 * 5) You can study and modify (improve) this source
 *    for own using or for redistribution, but you have to license the
 *    modified sources likewise under this LGPL Lesser General Public License.
 *    You mustn't delete this Copyright/Copyleft inscription in this source file.
 *
 * @author Hartmut Schorrig: hartmut.schorrig@vishia.de, www.vishia.org
 * @version 0.93 2011-01-05  (year-month-day)
 *******************************************************************************/ 
package org.vishia.util;

/**This class helps to measurement the cycle- and calculation times of any cyclically routine.
 * @author Hartmut Schorrig
 * @since 2010-07-14
 *
 */
public class MinMaxTime {
 
	int init = 0;
	
	double minCyclTime;

  double midCyclTime;

  double maxCyclTime;

  double minCalcTime;

  double midCalcTime;

  double maxCalcTime;

  long _lastTime;

  double _startTime;
 
  /**Factor to calc to millisec.  */
  double ms;
  
  /**Call it at start of the cyclically operation. */
  public void cyclTime(){
  	long time = System.nanoTime();       
    if(init == 0){ 
    	init = 1;  //only save time.
    } else {
	  	double cyclTime = ms * (double)(time - _lastTime);
	    if(init == 1){
	    	init = 2;
	    	minCyclTime = midCyclTime = maxCyclTime = cyclTime;
	    }
	    if(cyclTime > maxCyclTime) { maxCyclTime = cyclTime; }  
	    if(cyclTime < minCyclTime) { minCyclTime = cyclTime; }  
	    midCyclTime += (cyclTime - midCyclTime) * 0.01;            
    }
	  _lastTime = time;                  
  	
  }
	  
  /**Call it at end of the cyclically operation. */
  public void calcTime(){
  	long time = System.nanoTime();       
    double calcTime = ms * (double)(time - _lastTime);
    if(init!=2){
    	minCalcTime = midCalcTime = maxCyclTime = calcTime;
    }
    if(calcTime > maxCalcTime) { maxCalcTime = calcTime; }  
    if(calcTime < minCalcTime) { minCalcTime = calcTime; }  
    midCalcTime += (calcTime - midCalcTime) * 0.01;            
    _lastTime = time;                  
  	
  }
	  
  public void adjust(){
  	_lastTime = System.nanoTime();
  	try{ Thread.sleep(100); } catch(InterruptedException exc){
  		
  	}
  	long t100ms = System.nanoTime() - _lastTime;
  	ms = 100.0 / t100ms;
  }
  
  
  public void init(){
  	init = 0 ;
  }
  
  
}
