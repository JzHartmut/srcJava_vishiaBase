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
import java.util.Map;
import java.util.TreeMap;

import org.vishia.util.Arguments;
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
    
    Arguments.SetArgument set_rlink = new Arguments.SetArgument(){ @Override public boolean setArgument(String val){ 
      assert(val.charAt(1) == ':');
      int ix = val.charAt(0) - '0';
      Args.this.rlink[ix] = val.substring(2);          // this is a link to replace instead "../../" etc.
      return true;
    }};

    
    int maxLineLength;
    
    Args() {
      super("Preparation of Asciidoc", "");
      addArg(this.sMaxLineLength);
      addArg(new Argument("-rlink", ":1:https// replaces for link:../link, digit is number of ../../", this.set_rlink));

    }
    
    String[] rlink = new String[5];
    
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
  
  
  private static String sChpAdd = "=====";
  
  Map<String, String> idxChpLabel = new TreeMap<String, String>(); 
  
  Map<String, String> idxChpNr = new TreeMap<String, String>(); 
  
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
    thiz.processChapterStruct(args);
    thiz.processFile(args);
  }


  void processChapterStruct ( Args args) throws IOException {
    for(File fIn: args.fIn) {
      BufferedReader frIn = new BufferedReader(new InputStreamReader(new FileInputStream(fIn), args.csIn));
      String sLine;
      String chpLabel = null;
      int[] chpNr = new int[5];
      while( (sLine = frIn.readLine()) !=null) {
        int nChp = 0;
        if(sLine.startsWith("[#")) {
          int posEnd = sLine.indexOf(']');
          chpLabel = sLine.substring(2, posEnd);
        } else if(sLine.startsWith("== ")) {
          chpNr[0] +=1; chpNr[1] = 0; chpNr[2] = 0; chpNr[3] = 0; chpNr[4] = 0; 
          nChp = 1;
        } else if(sLine.startsWith("=== ")) {
          chpNr[1] +=1; chpNr[2] = 0; chpNr[3] = 0; chpNr[4] = 0; 
          nChp = 2;
        } else if(sLine.startsWith("==== ")) {
          chpNr[2] +=1; chpNr[3] = 0; chpNr[4] = 0; 
          nChp = 3;
        } else if(sLine.startsWith("===== ")) {
          chpNr[3] +=1; chpNr[4] = 0; 
          nChp = 4;
        } else if(sLine.startsWith("====== ")) {
          chpNr[4] +=1;  
          nChp = 5;
        } else {
          nChp = 0;
          chpLabel = null;  //after any line not a chapter start
        }
        if(nChp >0) {
          String sChpNr = "";
          for(int ix = 0; ix < nChp; ++ix) {
            if(ix >0) { sChpNr += '.'; }
            sChpNr += Integer.toString(chpNr[ix]);
          }
          if(chpLabel == null) {
            System.err.println("No chapter label for " + sChpNr + sLine);
          } else {
            this.idxChpNr.put(chpLabel, sChpNr);
            this.idxChpLabel.put(sChpNr, chpLabel);
          }
        }
      }
      frIn.close();
    }
    System.out.println("all chapter label gathered ");
  }
  
  
  void processFile ( Args args) throws IOException {
    Writer fwOut = new OutputStreamWriter(new FileOutputStream(args.fOut), args.csOut);
    boolean bFirst = true;
    for(File fIn: args.fIn) {
      File dirIn = fIn.getParentFile();
      BufferedReader frIn = new BufferedReader(new InputStreamReader(new FileInputStream(fIn), args.csIn));
      String sLine;
      if(!bFirst) {
        while( (sLine = frIn.readLine()) !=null) {
          if(sLine.startsWith("[#") || sLine.startsWith("== ") || sLine.startsWith("<<<")) {
            break;
          }
        }        
      } else {
        sLine = frIn.readLine();
      }
      bFirst = false;
      do {
        processLine(sLine, frIn, fwOut, dirIn, args);
      } while( (sLine = frIn.readLine()) !=null);
      //
      frIn.close();
    }
    fwOut.close();
    System.out.println("successfull: " + args.fOut.getPath());
  }
  
  
  
  void processLine ( String sLine, BufferedReader frIn, Writer fwOut, File dirIn, Args args) throws IOException {
    if(sLine.equals("----")) {
      processPre(frIn, fwOut, dirIn, args);
    } else if(sLine.startsWith("include::")) {
      processIncludeAdoc(sLine, fwOut, dirIn, args);
    } else {
      String sLineNew = sLine;
      int posLink = sLine.indexOf("link:");
      if(posLink >=0) {
        sLineNew = replaceLink(sLine, posLink, args);
      }
      sLineNew = addChapterNrToChapterLink(sLine);
      fwOut.append(sLineNew).append('\n');
    }
    
  }
  
  
  
  
  private String replaceLink ( String sLine, int posLink, Args args) {
    String sLineNew = sLine;
    String sLink = sLine.substring(posLink + 5);
    if(!sLink.startsWith("https:") && !sLink.startsWith("http:") && !sLink.startsWith("www.")) {
      int ixBack = 0;
      while(sLink.substring(3*ixBack).startsWith("../")) {
        ixBack +=1;
      }
      //      before and  "...link:"       from arg: "https:..."   rest after the "../../    
      String sLineEnd = ixBack == 0 ? "/" + sLink : sLink.substring(3*ixBack-1);
      sLineNew = sLine.substring(0, posLink+5) + args.rlink[ixBack] + sLineEnd;
    }
    return sLineNew;
  }
  
  
  
  private String addChapterNrToChapterLink(String sLine) {
    String sLineNew = sLine;
    int posChapterLink, posChapterLinkEnd = 0;
    while( (posChapterLink = sLineNew.indexOf("<<#",posChapterLinkEnd)) >=0) {
      posChapterLinkEnd = sLineNew.indexOf(">>", posChapterLink);
      String sLabel = sLineNew.substring(posChapterLink+3, posChapterLinkEnd);
      String sNr = this.idxChpNr.get(sLabel);
      if(sNr ==null) { 
        System.err.println("chapter label not found: " + sLabel);
      } else {
        sLineNew = sLineNew.substring(0, posChapterLink) + sNr + ' ' + sLineNew.substring(posChapterLink);
      }
    }
    return sLineNew;
  }
  
  
  void processIncludeAdoc ( String sLineIncl, Writer fwOut, File dirIn, Args args) throws IOException {
    int posArgs = sLineIncl.indexOf('[');
    int posArgEnd = sLineIncl.indexOf(']', posArgs+1);
    String sChpLabel1=null, sChpLabel9=null;
    int levelOffs = 0;
    if(posArgs <0 || posArgEnd <0) {
      System.err.println("missing [...]: " + sLineIncl);
    } else {
      String[] sArgs = sLineIncl.substring(posArgs+1, posArgEnd).split(",");
      for(String sArg: sArgs) {
        sArg = sArg.trim();
        if(sArg.startsWith("leveloffset")) {
          int posp = sArg.indexOf("=");
          String snr = sArg.substring(posp+1).trim();
          levelOffs = Integer.parseInt(snr);
        }
        else if(sArg.startsWith("chapter=")) {
          int posSep = sArg.indexOf("..");
          if(posSep >0) {
            sChpLabel9 = "[#" + sArg.substring(posSep+2).trim() + "]";
            sChpLabel1 = "[#" + sArg.substring(8, posSep).trim() + "]";
          } else {
            sChpLabel1 = sChpLabel9 = "[#" + sArg.substring(8).trim() + "]";
          }
        }
      }
      String sfFile = sLineIncl.substring(9, posArgs);
      File fIncl = new File(dirIn, sfFile);
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
        boolean pgBreakBefore = false;
        int nLevelChp = 0, nLevelChpCurr = 0;;
        while( (sLine = frIncl.readLine()) !=null) {
          if(sLine.startsWith("<<<")) {
            pgBreakBefore = true;
          }
          else if(sChpLabel1 ==null && (sLine.startsWith("[#") || sLine.startsWith("== ") || sLine.startsWith("<<<"))) {
            break;
          }
          else if(sChpLabel1 !=null  && sLine.startsWith(sChpLabel1)) {
            break;
          }
          else if(sLine.trim().length()>0) {
            pgBreakBefore = false;
          }
        }
        if(pgBreakBefore) {
          fwOut.append("<<<\n\n");
        }
        boolean bLastChpLabel = false;
        while(sLine !=null) {
          if(sChpLabel9 !=null  && sLine.startsWith(sChpLabel9)) {
            bLastChpLabel = true;
            nLevelChp = 0;
          }
          if(sLine.startsWith("==")){
            nLevelChpCurr = 1;
            while(sLine.charAt(++nLevelChpCurr) == '=');   // counts "=== "
            if(bLastChpLabel) {
              if(nLevelChp == 0) {
                nLevelChp = nLevelChpCurr;
              } else if(nLevelChpCurr <= nLevelChp){
                break;
              }
            }
            if(levelOffs >0) {
              sLine = sChpAdd.substring(0, levelOffs) + sLine;
            }
          }
          processLine(sLine, frIncl, fwOut, dirIn, args);
          sLine = frIncl.readLine();
        } //while
        frIncl.close();
      }
    }
  }
  
  
  
  /**Replace something in line.
   * <ul><li>link:../link with given arguments in {@link Args#rlink} for the given number of ../
   * <li>chapter <<# with chapter NR <<# 
   * <ul> 
   * @param sLine
   * @param args
   * @return
   */
  private String processLine ( String sLine, Args args) {
    String sLineNew = sLine;
    int posLink = sLine.indexOf("link:");
    if(posLink >=0) {
      String sLink = sLine.substring(posLink + 5);
      if(!sLink.startsWith("https:") && !sLink.startsWith("http:") && !sLink.startsWith("www.")) {
        int ixBack = 0;
        while(sLink.substring(3*ixBack).startsWith("../")) {
          ixBack +=1;
        }
        //      before and  "...link:"       from arg: "https:..."   rest after the "../../    
        String sLineEnd = ixBack == 0 ? "/" + sLink : sLink.substring(3*ixBack-1);
        sLineNew = sLine.substring(0, posLink+5) + args.rlink[ixBack] + sLineEnd;
      }
    }
    int posChapterLink, posChapterLinkEnd = 0;
    while( (posChapterLink = sLineNew.indexOf("<<#",posChapterLinkEnd)) >=0) {
      posChapterLinkEnd = sLineNew.indexOf(">>", posChapterLink);
      String sLabel = sLineNew.substring(posChapterLink+3, posChapterLinkEnd);
      String sNr = this.idxChpNr.get(sLabel);
      if(sNr ==null) { 
        System.err.println("chapter label not found: " + sLabel);
      } else {
        sLineNew = sLineNew.substring(0, posChapterLink) + sNr + ' ' + sLineNew.substring(posChapterLink);
      }
    }
    return sLineNew;
  }
  
  
  void processPre(BufferedReader frIn, Writer fwOut, File dirIn, Args args) throws IOException {
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
        readTagFromFile(sLine.substring(9, posTag), sTag, fwOut, dirIn, args);
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

  
  void readTagFromFile(String sfFile, String sTag, Writer fwOut, File dirIn, Args args) throws IOException {
    File fIncl = new File(dirIn, sfFile);
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
