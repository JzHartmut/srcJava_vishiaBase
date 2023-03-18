package org.vishia.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;

import org.vishia.util.ExcUtil;
import org.vishia.util.FileFunctions;

public class PrepareAsciidoc extends ReadWriteFileBase {

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

  static class Args extends ArgsBase {

    Argument sMaxLineLength = new Argument("-lmax", ":97 maximal length of a line in a pre section");
    
    int maxLineLength;
    
    Args() {
      super("Preparation of Asciidoc", "");
      addArg(this.sMaxLineLength);
    }
    
    @Override public boolean testConsistence ( Appendable msg) throws IOException {
      boolean bOk = super.testConsistence(msg);
      if(!bOk) { return false; }
      if(this.sMaxLineLength.val !=null) {
        this.maxLineLength = Integer.parseInt(this.sMaxLineLength.val);
      } else {
        this.maxLineLength = 97;
      }
      return bOk;
    }
  }
  
  
  
  public static void main(String cmdArgs[]) {
    Args args = new Args();
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


  public static void smain ( Args args) throws IOException {
    PrepareAsciidoc thiz = new PrepareAsciidoc();
    thiz.processFile(args);
  }


  void processFile ( Args args) throws IOException {
    BufferedReader frIn = new BufferedReader(new InputStreamReader(new FileInputStream(args.fIn), args.csIn));
    Writer fwOut = new OutputStreamWriter(new FileOutputStream(args.fOut), args.csOut);
    String sLine;
    while( (sLine = frIn.readLine()) !=null) {
      if(sLine.equals("----")) {
        processPre(frIn, fwOut, args);
      } else {
        fwOut.append(sLine).append('\n');
      }
    }
    fwOut.close();
    frIn.close();
    System.out.println("successfull: " + args.fOut.getPath());
  }
  
  void processPre(BufferedReader frIn, Writer fwOut, Args args) throws IOException {
    fwOut.append("----\n");
    String sLine;
    while( (sLine = frIn.readLine()) !=null) {
      if(sLine.startsWith("include::")) {
        int posTag = sLine.indexOf('[');
        int posTagEnd = sLine.indexOf(']', posTag+1);
        String sTag;
        if(posTag >0 && posTagEnd == posTag +1) {
          sTag = null;
        }
        else if(!sLine.substring(posTag).startsWith("[tag=") || posTagEnd <0) {
          System.err.println("faulty " + sLine);
          sTag = null;
        } else {
          sTag = sLine.substring(posTag + 5, posTagEnd);
        }
        readTagFromFile(sLine.substring(9, posTag), sTag, fwOut, args);
      } else if(sLine.startsWith("----")) {
        fwOut.append("----\n");
        break;
      } else {
        fwOut.append(shortenPreLine(sLine, args)).append('\n');
      }
    }
  }
  
  
  
  private String shortenPreLine ( String sLine, Args args) {
    if(sLine.length() > args.maxLineLength) {
      return sLine.substring(0, args.maxLineLength -3) + "...";
    }
    else return sLine;
  }

  
  void readTagFromFile(String sfFile, String sTag, Writer fwOut, Args args) throws IOException {
    File fIncl = new File(args.fIn.getParentFile(), sfFile);
    BufferedReader frIncl;
    try {
      frIncl = new BufferedReader(new InputStreamReader(new FileInputStream(fIncl), args.csIn));
    } catch(FileNotFoundException exc) {
      frIncl = null;
      fwOut.append("ERROR file not found: ").append(sfFile).append('\n');
      System.err.println("ERROR file not found: " + sfFile);
    }
    if(frIncl !=null) {
      String sLine;
      boolean bTag = sTag ==null;             //tag found
      boolean bEndTag = false;
      boolean bTagOff = false;
      while( !bEndTag && (sLine = frIncl.readLine()) !=null) {
        if(!bTag) {
          if(sLine.contains("tag::" + sTag + "[]")) {
            bTag = true;
          }
        } else if(sTag !=null && sLine.contains("end::" + sTag + "[]")) {
          bEndTag = true;                      // finish file
        } else if(sLine.contains("tagOffStart::")) {
          fwOut.append("    ......\n");
          bTagOff = true;
        } else if(sLine.contains("tagOffEnd::")) {  //
          bTagOff = false;
        } else if(sLine.contains("tagOff::")){
          fwOut.append("    ......\n");
        } else if(!bTagOff && !sLine.contains("tag::") && !sLine.contains("end::")){
          fwOut.append(shortenPreLine(sLine, args)).append('\n');
        }
      }
      if(sTag !=null && (!bTag || ! bEndTag)) {
        System.err.println("Problem with tag=" + sTag + " in " +sfFile);
      }
      frIncl.close();
    }
    //fwOut.append("read file" + sfFile + ":::" + sTag);
  }
  
}
