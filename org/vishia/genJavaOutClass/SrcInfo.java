package org.vishia.genJavaOutClass;

/**This class usable as super class for parse results contains
 * the information about the parsed source, file, line, column
 * in an unique form especially for comment and error report
 * to deduce to the source of generate.
 * @author Hartmut Schorrig
 * LPGL licence
 * @since 2022-03 but concept of info is older. 
 *
 */
public class SrcInfo {
  
  //TODO should be private, change in Type_Zbnf for generation.
  public int _srcColumn_, _srcLine_ = 0; public String _srcFile_ = "unknown";
  
  public void setSrcInfo( String file, int line, int column) {
    this._srcFile_ = file; this._srcLine_ = line; this._srcColumn_ = column; 
  }
  
  
  public void setSrcInfo(SrcInfo src) {
    this._srcFile_ = src._srcFile_;
    this._srcLine_ = src._srcLine_;
    this._srcColumn_ = src._srcColumn_;
  }
  
  
  public boolean containsInfo() {
    return this._srcFile_ !=null && this._srcLine_ >0; 
  }
  
  public String getSrcInfo ( int[] lineColumn) {
    if(lineColumn !=null) {
      if(lineColumn.length >=1) { lineColumn[0] = this._srcLine_; }
      if(lineColumn.length >=2) { lineColumn[1] = this._srcColumn_; }
    }
    return this._srcFile_;
  }
  
  public String getFilePath ( ) {
    if(this._srcFile_ == null) return ("unknown.file");
    else return this._srcFile_;
  }
  
  public String getFileName ( ) {
    if(this._srcFile_ == null) return ("unknown.file");
    else { 
      int posSlash = this._srcFile_.lastIndexOf('/'); 
      return this._srcFile_.substring(posSlash+1);
    }
  }
  
  
  public int getLine ( ) { return this._srcLine_; }
  
  @Override public String toString() {
    return " @" + getFileName() + ":" + this._srcLine_ + " ";
  }

  public String showSrcInfo() {
    return " @" + getFileName() + ":" + this._srcLine_ + " ";
  }

}
