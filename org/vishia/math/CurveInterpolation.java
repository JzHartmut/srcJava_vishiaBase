package org.vishia.math;

import java.util.Arrays;

import org.vishia.util.Debugutil;


//Test with JZcmd:
//==JZcmd==
//JZcmd main(){ 
//JZcmd   Obj result = java org.vishia.math.CurveInterpolation.test2(); 
//JZcmd   Obj plotWindow = java org.vishia.gral.test.GralPlotWindow.create("Test CurveInterpolation");
//JZcmd   Obj plot = plotWindow.canvas();
//JZcmd   Obj color = java org.vishia.gral.ifc.GralColor.getColor("red");
//JZcmd   Obj scaling = plot.userUnitsPerGrid(-5, -5, 0.4, 0.4);
//JZcmd   plot.drawLine(color, scaling, result, 1);
//JZcmd   plot.drawLine(color, scaling, result, 2);
//JZcmd   plot.drawLine(color, scaling, result, 3);
//JZcmd   plot.drawLine(color, scaling, result, 4);
//JZcmd   plot.drawLine(color, scaling, result, 5);
//xxJZcmd   Obj color = java org.vishia.gral.ifc.GralColor.getColor("gn");
//xxJZcmd   plot.drawLine(color, scaling, java org.vishia.math.CurveInterpolation.pointsTest(),1);
//JZcmd   plotWindow.waitForClose();
//JZcmd 
//JZcmd 
//JZcmd 
//JZcmd 
//JZcmd }
//==endJZcmd==


/**This class contains some static methods for curve interpolation with given setting points.
 * 
 * @author Hartmut Schorrig
 *
 */
public class CurveInterpolation
{
  
  /**Version, history and license.
   * <ul>
   * <li>2015-09-26 Hartmut creation.   
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
   * @author Hartmut Schorrig = hartmut@vishia.org
   * 
   */
  public static final String sVersion = "2015-10-04";

  
  public static float linearInterpolation(float x, float[][] points, int ixstart) {
    int ix = search(x, points, 0, points.length, ixstart, getColumn);  //search x in points.
    //testIndex[++testIndex[0]] = ix;
    //float d0, d1, d2;
    float d;
    float x1 = points[ix][0], x2;
    float y1 = points[ix][1], y2;
    if (ix < points.length -1){
      x2 = points[ix+1][0];
      y2 = points[ix+1][1];
      d = (y2 - y1) / (x2 - x1);
    } else { //ix in last segment
      x2 = x1;
      y2 = y1;
      float x0 = points[ix-1][0];
      float y0 = points[ix-1][1];
      y2 = y1;
      d = (y1 - y0) / (x1 - x0);
    }
    float dx = x - x1;
    float y = y1 + d * dx;          //value with slope on left point.
    return y;
  }

  
  /**This spline interpolation has the following propertied:
   * <ul>
   * <li>Any y-value in points is exactly represented by the associated x input. It means any point in points
   *   is exactly represented in the result. 
   * <li>The middle point of a segment in points is exactly represented in the result.
   * <li>The slope (dx/dreturn) left and right from a point in points is identically, it is the middle value of slope of the both segments.
   * <li>The slope in the middle of a segment is exact the slope of the segment.   
   * </ul>
   * See the following plot. The red curve is a result curve, the green curve is the curve of points with 4 points.
   * <img src="../../../img/CurveInterpolation.png" />
   * 
   * @param x input
   * @param points array of ordered pair of numbers. points[...][0] is the x value. points[...][1] is the associated y-value.
   *   The x values have to be increasing.    
   * @param ixstart -1 if not used, possible index to enforce the search of x input in points. Result of {@link #search(float, float[][], int)}. 
   * @return The associated y value to x.
   */
  public static float splineInterpolation(float x, float[][] points, int ixstart) {
    int ix = search(x, points, 0, points.length, ixstart, getColumn);  //search x in points.
    //testIndex[++testIndex[0]] = ix;
    //float d0, d1, d2;
    float dl, d, dr;
    float x0, x1 = points[ix][0], x2, x3;
    float y0, y1 = points[ix][1], y2, y3;
    if(ix >=1){
      x0 = points[ix-1][0];
      y0 = points[ix-1][1];
    } else {
      x0 = x1; y0 = y1;
    }
    if(ix == 0){
      x2 = points[ix+1][0];
      y2 = points[ix+1][1];
      x3 = points[ix+2][0];
      y3 = points[ix+2][1];
      d = (y2 - y1) / (x2 - x1);
      dl = d;
      dr = (y3 - y1) / (x3 - x1);
    } else if (ix < points.length -2){
      x0 = points[ix-1][0];
      y0 = points[ix-1][1];
      x2 = points[ix+1][0];
      y2 = points[ix+1][1];
      x3 = points[ix+2][0];
      y3 = points[ix+2][1];
      dl = (y2 - y0) / (x2 - x0);
      d = (y2 - y1) / (x2 - x1);
      dr = (y3 - y1) / (x3 - x1);
    } else if (ix < points.length -1){
      x0 = points[ix-1][0];
      y0 = points[ix-1][1];
      x2 = points[ix+1][0];
      y2 = points[ix+1][1];
      dl = (y2 - y0) / (x2 - x0);
      d = (y2 - y1) / (x2 - x1);
      dr = d;
    } else { //ix in last segment
      x2 = x1;
      y2 = y1;
      dl = (y1 - y0) / (x1 - x0);
      d = dr = dl;
    }
    //float dl = (d0 + d1)/2;  //slope on left point is arithmetic mean between left and used segment.
    //float dr = (d1 + d2)/2;  //slope on right point is arithmetic mean between used and right segment.
    float xs = (x1 + x2)/2;  //half between x1..x2, half of segment.
    float ys = y1 + d * (xs - x1);  //exactly y in the middle of the used segment.
    float y;
    if(x == 7.25f){
      Debugutil.stop();
    }
  
    if(x < xs){         //left side in segment:
      float dx = x - x1;
      float fs = 1.0f - dx / (xs - x1);  //1.0 on left point, 0 in middle of segment.
      fs *= fs;                          //use 0.25 on quarter x of segment, more middle of segment.
      float y1y = y1 + dl * dx;          //value with slope on left point.
      float ysy = ys - d * (xs - x);    //value with slope of the segment from middle point of the segment.
      y = (fs) * y1y + (1-fs) * ysy;     //Use mean value weighted with position in segment.
    } else {            //right side in segment:
      float dx = x - xs;
      float fs = xs < x2 ? (x - xs) / (x2 - xs) : 1.0f;  //after right point, xs is x2
      fs *= fs;                          //use 0.25 on quarter x of segment, more middle of segment.
      float ysy = ys + d * (x - xs);    //value with slope of the segment from middle point of the segment.
      float y2y = y2 - dr * (x2 - x);    //value with slope on right point.
      y = (1-fs) * ysy + (fs) * y2y;     //Use mean value weighted with position in segment.
    }
    return y;
  }

  
  
  
    /**
     * <pre>
     *          xc1   xc     xc2
     *   xr1    y1    yc1    yc2    ------dc1
     *   
     *   xr           y
     *   
     *   xr2    yr2   ycr2    y2    ------dc2
     * 
     * </pre>
     * @param xr
     * @param xc
     * @param points
     * @param ixstart
     * @return
     */
    public static float linearInterpolation(float xr, float xc, float[][] points, int ixstart) {
    int ixr = search(xr, points, 1, points.length, ixstart, getColumn);  //search x in points.
    int ixc = search(xc, points[0], 1, points[0].length, ixstart, getSingle);  //search x in points.
    //testIndex[++testIndex[0]] = ix;
    //float d0, d1, d2;
    final float dr, dc1, dc2;
    final float xr1 = points[ixr][0], xr2;
    final float xc1 = points[0][ixc], xc2;
    final float y1 = points[ixr][ixc], yr2, yc2;
    final float y2;
    final int ixr2;
    if (ixr < points.length -1 && ixc < points[0].length-1){
      ixr2 = ixr +1;
      xr2 = points[ixr2][0];
      xc2 = points[0][ixc+1];
      yr2 = points[ixr2][ixc];
      yc2 = points[ixr] [ixc+1];
      y2 =  points[ixr2][ixc+1];
      dc1 = (yc2 - y1) / (xc2 - xc1);
      dc2 = (y2 - yr2) / (xc2 - xc1);
    } else if (ixr < points.length -1 ) { //ixc in last segment
      float xc0 = points[0][ixc-1];
      float yc0 = points[ixr][ixc-1];
      xc2 = xc1 + (xc1 - xc0);
      ixr2 = ixr +1;
      yr2 = points[ixr2][ixc];
      yc2 = y1 + (y1 - yc0) / (xc1 - xc0); //points[ixr] [ixc+1];
      y2 =  points[ixr2][ixc+1];
      xr2 = xr1;
      dc1 = (yc2 - y1) / (xc1 - xc0);
      dc2 = (y2 - yr2) / (xc1 - xc0);
    } else { //both in last segment
      float xc0 = points[0][ixc-1];
      float yc0 = points[ixr][ixc-1];
      float xr0 = points[ixr-1][0];
      float yr0 = points[ixr-1][ixc];
      xc2 = xc1 + (xc1 - xc0);
      //ixr2 = ixr +1;
      yr2 = 0; //TODO points[ixr2][ixc];
      yc2 = y1 + (y1 - yc0) / (xc1 - xc0); //points[ixr] [ixc+1];
      y2 = 0; // points[ixr2][ixc+1];
      xr2 = xr1;
      dc1 = dc2 = (yc2 - y1) / (xc1 - xc0);
    }
    /*
    if (ixc < points[0].length -1){
      xc2 = points[0][ixc+1];
      yc2 = points[ixr2][ixc+1];
      dc1 = (yc2 - y1) / (xc2 - xc1);
    } else { //ix in last segment
      xc2 = xc1;
      yc2 = y1;
      float xc0 = points[0][ixc-1];
      float yc0 = points[ixr][ixc-1];
      yr2 = y1;
      dc = (y1 - yc0) / (xr1 - xc0);
    }
    */
    float dxc = xc - xc1;
    float dxr = xr - xr1;
    float yc1 =  y1  + dc1 * dxc;
    float ycr2 = yr2 + dc2 * dxc;
    dr = (ycr2 - yc1) / (xr2 - xr1);
    float y = yc1 + dr * dxr;          //value with slope on left point.
    return y;
  }

  

  
  public static float splineInterpolation(float xr, float xc, float[][] points, int ixstart) {
    int ixr = search(xr, points[0], 1, points[0].length, ixstart, getSingle);  //search x in points.
    int ixc = search(xc, points, 0, points.length, ixstart, getColumn);  //search x in points.
    float dl, d, dr;
    float xr0, xr1 = points[ixr][0], x2, x3;
    float yr0, yr1 = points[ixr][ixc], y2, y3;
    return 0;
  }  
  
  
  
  private interface Get { float get(int ix, Object container); }
  
  private static Get getColumn = new Get() { 
    public float get(int ix, Object container){ return ((float[][])container)[ix][0]; }
  };
  
  private static Get getSingle = new Get() { 
    public float get(int ix, Object container){ return ((float[])container)[ix]; }
  };
  
  /**Binary search of x in points[...][0]
   * @param x
   * @param points
   * @param ixstart check whether x is in the correct segment, then fast return.
   * @return
   */
  private static int search(float x, Object points, int from, int to, int ixstart, Get get) {
    if(ixstart >=0 && get.get(ixstart, points) <= x && (ixstart >= to-1 || x <= get.get(ixstart+1, points)) ){ 
      return ixstart;
    } else {
      //binary search
      int low = from;
      int high = to;
      int mid = from;
      while (low < (high-1)) 
      {
        mid = (low + high) >> 1;
        float y = get.get(mid, points);
        if ( x >= y)
        { low = mid;
          //equal = false;
          //if(x < points[mid+1][0]) {
          //  high = mid;
          //}
        }
        else { // dont check : if(cmp >0) because search always to left to found the leftest position
          high = mid;   //search in left part also if key before mid is equal
          mid = high;  //if it is the end
        }
      }
      return low;
      //return low > mid ? low : mid;
    }
  }
  
  
  public static float[][] pointsTest = {{0, 1}, {5, 10}, {10, 1.0f}, { 20, 14f}, {30, 20.0f}};
  
  
  public static float[][] pointsTest(){ return pointsTest; }
  
  private static int[] testIndex = new int[200];
  
  public static float[][] test(){
    Arrays.fill(testIndex, 0);
    //float[] xtest = { 0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15};
    float[][] result = new float[140][2];
    for(int ix = 0; ix < 140; ++ix){
      float x = (0.25f * ix) -2;
      result[ix][0] = x;
      result[ix][1] = linearInterpolation(x, pointsTest, -1);
      //result[ix][1] = splineInterpolation(x, pointsTest, -1);
    }
    return result;
  }
  
  
  /**The xr or x1 values are given from -1, 2, 7 10 in the first row for columns.
   * The xc or x2 values are given from -1, 3, 8 12 in the first column for rows.
   * The y values are shown in the row and column of the x values.
   */
  public static float[][] points2Test = 
  { {  0, -1,  2,  7, 90 }
  , { -1,  0,  2,  7, 10 }
  , {  3,  4, 10, 11, 12 }
  , {  8,  5, 12, 15, 16 }
  , { 92,  6, 15, 19, 19 }
  };
  
  public static float[][] test2(){
    Arrays.fill(testIndex, 0);
    //float[] xtest = { 0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15};
    float[][] result = new float[140][10];
    for(int ixc = 1; ixc < 10; ++ixc  ) {
      float xc = -4 + 2 * ixc;
      result[0][ixc] = xc;
      for(int ix = 1; ix < 140; ++ix){
        float xr = -0.5f + (0.25f * ix) -2;
        result[ix][0] = xr;
        if(ixc == 2 && ix == 5)
          Debugutil.stop();
        result[ix][ixc] = linearInterpolation(xr, xc, points2Test, -1);
        //result[ix][1] = splineInterpolation(x, pointsTest, -1);
      }
    }
    return result;
  }
  
  
  
  
}
