package org.vishia.test;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;

public class TestAddJar
{
  
  public static void main(String[] args){
    TestAddJar main = new TestAddJar();
    try {
      main.execute();
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }
  
  
  
  @SuppressWarnings("unused")
  void execute() throws MalformedURLException, ClassNotFoundException, InstantiationException, IllegalAccessException{
    //it is false! 
    //System.loadLibrary("D:/Programme/Eclipse3_5/plugins/org.apache.ant_1.7.1.v20090120-1145/lib/ant-swing.jar");
    //The current classLoader or the system's classLoader:
    ClassLoader classLoader = this.getClass().getClassLoader();
    //classLoader.
    File jarFile = new File("D:/Programme/Eclipse3_5/plugins/org.apache.ant_1.7.1.v20090120-1145/lib/ant-swing.jar");
    URI uri = jarFile.toURI();
    URL[] urls = new URL[1];
    urls[0] = uri.toURL();
    URLClassLoader child = new URLClassLoader(urls, classLoader);
    //Load a class in that jar:
    Class<?> clazz = child.loadClass("org.apache.tools.ant.taskdefs.optional.splash.SplashScreen");
    //Load another class not in that library but in the parent.
    Class<?> clazz2 = child.loadClass("org.vishia.util.StringPart");
    System.out.println(clazz.getName());
    
    //does not work:  Object test = new org.apache.tools.ant.taskdefs.optional.splash.SplashScreen();
    try{ child.close();} catch(IOException exc) { /*close not handled*/ }
    Object test = clazz.newInstance();
    
  }
}
