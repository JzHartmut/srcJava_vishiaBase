package org.vishia.xmlReader.test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.vishia.util.Debugutil;
import org.vishia.util.IndexMultiTable;
import org.vishia.util.StringFormatter;
import org.vishia.xmlReader.XmlJzCfgAnalyzer;
import org.vishia.xmlReader.XmlJzReader;

public class TestReadMsc15Cfg
{

 
  public static class MscOption{
    
    Map<String, MscOption> nodes = new IndexMultiTable<String, MscOption>(IndexMultiTable.providerString);

    final String name;
    
    String regname, pkgname;
    
    String content;
    
    MscOption(String name){ this.name = name; }
    
    public MscOption addOption(String name, String rname) {
      MscOption newNode = new MscOption(name);
      newNode.regname = rname;
      nodes.put(name, newNode);
      return newNode;
    }
    
    public MscOption addSubOption(String name, String rname, String pname) {
      MscOption newNode = new MscOption(name);
      newNode.regname = rname;
      newNode.pkgname = pname;
      nodes.put(name, newNode);
      return newNode;
    }
  
    
    void setContent(String text) {
      content = text;
    }
    
  }
  
  
  
  public static class KeyboardShortCuts {
    
    //Map<String, KeyboardShortcut> nodes = new IndexMultiTable<String, KeyboardShortcut>(IndexMultiTable.providerString);
    
    List<KeyboardShortcut> shortcuts = new ArrayList<KeyboardShortcut>();
    
    List<KeyboardShortcut> shortcutsUser = new ArrayList<KeyboardShortcut>();

    List<KeyboardShortcut> removedShortcuts = new ArrayList<KeyboardShortcut>();
    
    Map<String, KeyboardShortcut> sorted = new IndexMultiTable<String, KeyboardShortcut>(IndexMultiTable.providerString);

    
    KeyboardShortCuts addKeyboardShortcut() { return this; }

    
    KeyboardShortcut addDfltKeyboardShortcut(String cmd, String scope) { 
      KeyboardShortcut ret = new KeyboardShortcut(cmd, scope, true);
      shortcuts.add(ret);
      return ret;
    }
    
    KeyboardShortcut addUserKeyboardShortcut(String cmd, String scope) { 
      KeyboardShortcut ret = new KeyboardShortcut(cmd, scope, false);
      //shortcuts.add(ret);
      shortcutsUser.add(ret);
      return ret;
    }

  
    KeyboardShortcut removeKeyboardShortcut(String cmd, String scope) { 
      KeyboardShortcut ret = new KeyboardShortcut(cmd, scope, true);
      removedShortcuts.add(ret);
      return ret;
    }
    

  
  
  }
  
  
  
  
  public static class KeyboardShortcut{
    

    final String cmd, scope;
    
    final boolean dflt;
    
    String shortcut;
    
    KeyboardShortcut(String cmd, String scope, boolean dflt){ this.cmd = cmd; this.scope = scope; this.dflt = dflt; }
    
  
    
    void setShortcut(String text) {
      shortcut = text;
    }
    
    
    @Override public String toString() {
      return shortcut + ":" + cmd + "/" + scope;
    }
    
  }
  
  
  
  
  
  static void analyzeStruct(File fXmlIn) throws IOException {
    
    
    File fCfgOut = new File("T:/cfg.xml");
    XmlJzCfgAnalyzer wr = new XmlJzCfgAnalyzer();
    wr.readXmlStruct(fXmlIn);
    wr.writeCfgTemplate(fCfgOut);
    
  }
  
  
  static void readKeys(File fXmlIn) throws IOException {
    XmlJzReader xmlReader = new XmlJzReader();
    xmlReader.readCfg(new File("c:/Programs/MSC15_adaptSmlk/settings.cfg.xml"));
    KeyboardShortCuts data = new KeyboardShortCuts();
    xmlReader.readXml(fXmlIn, data);
    for(KeyboardShortcut sc: data.shortcutsUser) {
      if(sc.shortcut !=null) {
        data.sorted.put(sc.shortcut, sc);
      }
    }
    for(KeyboardShortcut sc: data.shortcuts) {
      if(sc.shortcut !=null) {
        data.sorted.put(sc.shortcut, sc);
      }
    }
    try {
      BufferedWriter wr = new BufferedWriter(new FileWriter("c:/Programs/MSC15_adaptSmlk/keyboardSettings.txt"));
      StringFormatter sf = new StringFormatter(100);
      for(Map.Entry<String, KeyboardShortcut> e: data.sorted.entrySet()) {
        KeyboardShortcut sc = e.getValue();
        sf.reset();
        sf.add(sc.shortcut).pos(25, 2).add(sc.dflt? "- ":"U ").add(sc.cmd).add(" scope: ").add(sc.scope).add("\n");
        wr.write(sf.toString());
      }
      for(KeyboardShortcut sc: data.removedShortcuts) {
        sf.reset();
        sf.pos(25, 2).add("X ").add(sc.cmd).add(" scope: ").add(sc.scope).add("\n");
        wr.write(sf.toString());
      }
      wr.close();
      sf.close();
    } catch (IOException exc) {
      System.err.println(exc);
    }
    Debugutil.stop();
  }
  
  
  
  
  public static void main(String args[]) {
    
    //File fXmlIn = new File("c:/Users/hartmut/Documents/Visual Studio 2015/Settings/CurrentSettings.vssettings");
    File fXmlIn = new File("D:/ML/SULtrcCurve/SULtrc.xml");
    try {
      analyzeStruct(fXmlIn);
    } catch (Exception e) {
      System.err.println("Unexpected: " + e.getMessage());
      e.printStackTrace(System.err);
    }

    //readKeys(fXmlIn);
    
    
    Debugutil.stop();
    
  }
  
  
  
}
