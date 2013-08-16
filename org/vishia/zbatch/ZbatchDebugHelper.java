package org.vishia.zbatch;

public class ZbatchDebugHelper
{


  public CharSequence info(Object obj){
    
    //Build an information string about the object:
    StringBuilder u = new StringBuilder();
    Class<?> clazz = obj.getClass();
    u.append("Type=");
    u.append(clazz.getCanonicalName());
    u.append("; toString=").append(obj.toString());
    u.append("; ");
    return u;

  }

  
  public CharSequence infoln(CharSequence start, Object obj){
    //Build an information string about the object:
    StringBuilder u = new StringBuilder();
    u.append(start);
    Class<?> clazz = obj.getClass();
    u.append("Type=");
    u.append(clazz.getCanonicalName());
    u.append("; toString=").append(obj.toString());
    u.append(";\n");
    return u;
    
  }


}
