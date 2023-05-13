package org.vishia.util;

public class StringArray implements CharSequence {

  final char[] array;
  final int from, to;
  
  
  public StringArray(char[] array, int from, int to) {
    this.array = array;
    this.from = from;
    this.to = to;
  }

  @Override public int length () {
    return this.to - this.from;
  }

  @Override public char charAt ( int index ) {
    return this.array[this.from + index];
  }

  @Override public CharSequence subSequence ( int start, int end ) {
    return new StringArray(this.array, this.from + start, this.from + end);
  }

}
