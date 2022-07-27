//D:/vishia/Java/srcJava_vishiaBase/org/vishia/fileRemote/test/TestFileCluster.java
//==JZcmd==
//JZcmd java org.vishia.fileRemote.test.TestFileCluster.test();
//Obj test = 
//==endJZcmd==
package org.vishia.fileRemote.test;

import org.vishia.fileRemote.FileCluster;
import org.vishia.fileRemote.FileRemote;
import org.vishia.util.Assert;

public class TestFileCluster
{
  
  FileCluster fileCluster = FileRemote.clusterOfApplication;
  
  public void execute(){
    
    FileRemote file1 = fileCluster.getDir("D:/vishia/Java/docuSrcJava_Zbnf_priv/org");
    FileRemote file2 = fileCluster.getDir("D:/vishia/Java/docuSrcJava_vishiaBase/org/vishia/zbnf");
    FileRemote file0 = fileCluster.getDir("D:/vishia/Java");
    FileRemote file2p = file2.getParentFile();  //searches whether the parent is registered in FileCluster.
    FileRemote file3 = fileCluster.getDir("D:/vishia/Java/docuSrcJava_vishiaBase");
    FileRemote file4 = file3.child("org/vishia");

    Assert.check(file2p == file4);

    FileRemote file5 = fileCluster.getDir("D:/vishia/Java/docuSrcJava_vishiaBase/org/vishia/fileRemote");

  }
  
  
  public void execute2(){
    
    FileRemote file1 = fileCluster.getDir("D:/vishia/Java/docuSrcJavaPriv_Zbnf/org/vishia/zbnf");
    FileRemote file3 = fileCluster.getDir("D:/vishia/Java/docuSrcJavaPriv_Zbnf/org/vishia/zcmd");
    FileRemote file2 = fileCluster.getDir("D:/vishia/Java/docuSrcJavaPriv_Zbnf");
    FileRemote file0 = file2.child("org/vishia");

    Assert.check(file0 == file1);

  }
  
  
  public static final void main(String[] args){ test();}
  
  public static final void test(){
    TestFileCluster main = new TestFileCluster();
    main.execute2();
  }
  
}
