package org.vishia.util;

import java.util.LinkedList;
import java.util.List;

/**This class enhances the StringPartScan with the capability to get a line and column
 * due to the current position.
 * <br>
 * On construction all '\n' are searched in the given text.
 * With them the internal {@link #posLineStart} is filled with the position after '\n'.
 * This takes a little while, iteration through the text.
 * Then all information for {@link #getline()} and {@link #getlineCol()} are given, see there.
 * 
 */
public class StringPartScanLineCol extends StringPartScan {

  
  final int[] posLineStart;
  
  public StringPartScanLineCol(CharSequence text) {
    super(text);
    int zText = text.length();
    List<Integer> listLineStartPos = new LinkedList<>();
    listLineStartPos.add(0);
    for(int ix = 0; ix < zText; ++ix) {
      if(text.charAt(ix) == '\n') { listLineStartPos.add(ix+1); }
    }
    this.posLineStart = new int[listLineStartPos.size()];
    int ix = -1;
    for(int pos: listLineStartPos) {
      this.posLineStart[++ix] = pos;
    }
  }
  
  private int binarySearch(int pos) {
    int low = 0;
    int high = this.posLineStart.length - 1;
    while (low +1 < high) 
    {
      int mid = (low + high) >> 1;
      int midVal = this.posLineStart[mid];
      if (midVal <= pos) { 
        low = mid;
      }
      else { 
        high = mid;  
      }
    }
    return low;  // pos not found.
  }
  
  /**returns the line and column of the current position.
   * Due to the internal {@link StringPartScan#begin}
   * this position is searched in {@link #posLineStart} using binary search. It is fast.
   * @return [0] line count from 1 as first, [1] column count from 1 as left.
   */
  public int[] getlineCol () {
    int pos = super.begin + super.absPos0;
    int ixPos = binarySearch(pos);
    int posline = this.posLineStart[ixPos];
    int column = pos - posline +1;
    return new int[] { ixPos+1, column};
  }

  public int getline () {
    int pos = super.begin + super.absPos0;
    int ixPos = binarySearch(pos);
    return ixPos+1;
  }
  
  
  /**Only for particular debug test
   * @param args not used
   */
  public static void main(String args[]) {
    StringPartScanLineCol thiz = new StringPartScanLineCol("\nline 1\nline2\n");
    for(int ix = 0; ix < thiz.endMax; ++ix) {
      int ixPos = thiz.binarySearch(ix);
      System.out.print(ixPos);
    }
    Debugutil.stop();
  }
}
