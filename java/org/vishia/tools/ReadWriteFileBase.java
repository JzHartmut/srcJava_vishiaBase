package org.vishia.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;

import org.vishia.util.Arguments;
import org.vishia.util.ExcUtil;
import org.vishia.util.FileFunctions;

/**This class can be extended by any application which reads one file and writes the output
 * (any conversion routine). 
 * @author hartmut Schorrig
 *
 */
public class ReadWriteFileBase {
  
  /**Version, history and copyright/copyleft.
   * <ul>
   * <li>2023-03-17 created. 
   * </ul>
   * 
   * <b>Copyright/Copyleft</b>:<br>
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
   *    but doesn't need to publish the whole source of the own product.
   * <li> You can study and modify (improve) this source
   *    for own using or for redistribution, but you have to license the
   *    modified sources likewise under this LGPL Lesser General Public License.
   *    You mustn't delete this Copyright/Copyleft inscription in this source file.
   * </ol>
   * If you intent to use this source without publishing its usage, you can get
   * a second license subscribing a special contract with the author. 
   * 
   * @author Hartmut Schorrig = hartmut.schorrig@vishia.de
   */
  @SuppressWarnings("hiding") public static final String sVersion = "2023-03-17";

  
  
  static class ArgsBase extends Arguments {

    /**Argument contains the timestamp.val to use as value, shorter form */ 
    public Argument sfOut = new Argument("-o", ":output file path (*.adoc), default: -in:...Y");
    public Argument encodingIn = new Argument("-inCharset", ":Encoding for -in, default UTF-8");
    public Argument encodingOut = new Argument("-outCharset", ":Encoding for -o, default UTF-8");

    File dirWrk, fOut;
    
    List<File> fIn = new LinkedList<File>();
    
    Arguments.SetArgument set_dirWrk = new Arguments.SetArgument(){ @Override public boolean setArgument(String val){ 
      ArgsBase.this.dirWrk = new File(FileFunctions.absolutePath(val, null).toString());
      return true;
    }};

    Arguments.SetArgument set_fIn = new Arguments.SetArgument(){ @Override public boolean setArgument(String val){ 
      ArgsBase.this.fIn.add(new File(FileFunctions.absolutePath(val, ArgsBase.this.dirWrk)));  //note: dirWrk may be null
      return true;
    }};

    
    Charset csIn, csOut;
    
    ArgsBase(String sAbout, String sHelp){
      super.aboutInfo = sAbout;
      super.helpInfo="obligate args: -in:...";
      addArg(new Argument("-wd", ":working dir path, default: system's current dir", this.set_dirWrk));
      addArg(new Argument("-in", ":input file path", this.set_fIn));
      addArg(this.sfOut);
      addArg(this.encodingIn);
      addArg(this.encodingOut);
    }

    @Override public boolean testConsistence ( Appendable msg ) throws IOException {
      if(this.dirWrk !=null) {
        if(!ArgsBase.this.dirWrk.exists() || !ArgsBase.this.dirWrk.isDirectory()) {
          msg.append("-wd:" + this.dirWrk.getAbsolutePath() + " : not existing or not a directory\n");
          return false;
        }
      }
      if(this.fIn.size() == 0) {
        msg.append("-in:path/to/input.adoc is obligate\n");
        return false;
      } else {
        for(File fIn : this.fIn) {
          if(!fIn.exists()) {
            msg.append("-in:" + fIn.getAbsolutePath() + " : file not found\n");
            return false;
          }
        }
      }
      //                                                   // -o: not given, use -i:...Y
      final String sfOut = this.sfOut.val != null ? this.sfOut.val : this.fIn.get(0).getAbsolutePath() + "Y";
      String sfOutAbs = FileFunctions.absolutePath(sfOut, this.dirWrk);
      if(sfOutAbs.endsWith("/")) {                         // -o:path/to/dirOut/ is given
        sfOutAbs += this.fIn.get(0).getName();
      }
      this.fOut = new File(sfOutAbs);  //note: dirWrk may be null
      if(!this.fOut.getParentFile().exists()) {
        msg.append("-o:" + this.fOut + " : directory does not exist, should be created manually\n");
      }
      if(this.encodingIn.val !=null) {
        this.csIn = Charset.forName(this.encodingIn.val);  //may except
      } else {
        this.csIn = Charset.forName("UTF-8");
      }
      if(this.encodingOut.val !=null) {
        this.csOut = Charset.forName(this.encodingOut.val);  //may except
      } else {
        this.csOut = Charset.forName("UTF-8");
      }
      return true;
    }
    
  }
  
  
  public static void main(String cmdArgs[]) {
    ArgsBase args = new ArgsBase("Example for read write file, reads and writes back", "");
    args.readArguments(cmdArgs);            // note: exit on error
    try {
      smain(args);                       // internal smain with prepared Args
    }
    catch(Throwable exc) {
      CharSequence sExc = ExcUtil.exceptionInfo("unexpected: ", exc, 0, 20);
      System.err.println(sExc);
      System.exit(5);
    }
    System.exit(0); 
  }


  public static void smain(ArgsBase args) throws IOException {
    Writer fwOut = new OutputStreamWriter(new FileOutputStream(args.fOut), args.csOut);
    for(File fIn: args.fIn) {
      BufferedReader frIn = new BufferedReader(new InputStreamReader(new FileInputStream(fIn), args.csIn));
      String sLine;
      while( (sLine = frIn.readLine()) !=null) {
        fwOut.append(sLine).append('\n');
      }
      frIn.close();
    }
    fwOut.close();
  }


}
